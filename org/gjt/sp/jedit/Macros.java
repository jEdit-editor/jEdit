/*
 * Macros.java - Macro manager
 * Copyright (C) 1999 Slava Pestov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import org.gjt.sp.jedit.event.*;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.util.Log;

/**
 * This class records and plays macros. Thanks to Romain Guy for writing
 * the S<sup>3</sup> plugin, which provided the insparation for this
 * class.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class Macros
{
	public static void beginRecording(View view, String name, Buffer buffer)
	{
		lastMacro = name;

		view.getTextArea().getInputHandler().setMacroRecorder(
			new BufferRecorder(buffer));
	}

	public static void endRecording(View view)
	{
		InputHandler inputHandler = view.getTextArea().getInputHandler();
		BufferRecorder recorder = (BufferRecorder)inputHandler
			.getMacroRecorder();

		if(recorder != null)
		{
			if(lastMacro != null)
				view.setBuffer(recorder.buffer);
			inputHandler.setMacroRecorder(null);
		}
	}

	static
	{
		jEdit.addEditorListener(new EditorHandler());
	}

	static class EditorHandler extends EditorAdapter
	{
		public void bufferClosed(EditorEvent evt)
		{
			View view = evt.getView();

			InputHandler inputHandler = view.getTextArea()
				.getInputHandler();
			BufferRecorder recorder = (BufferRecorder)inputHandler
				.getMacroRecorder();

			if(recorder != null)
			{
				if(evt.getBuffer() == recorder.buffer)
					inputHandler.setMacroRecorder(null);
				view.showStatus(null);
			}
		}
	}

	public static void playMacro(View view, String name)
	{
		lastMacro = name;

		if(name == null)
		{
			Buffer buffer = jEdit.getBuffer(MiscUtilities.constructPath(
				null,"<< temp macro >>"));
			if(buffer == null)
			{
				view.getToolkit().beep();
				return;
			}

			playMacroFromBuffer(view,"<< temp macro >>",buffer);
			return;
		}

		String fileName = MiscUtilities.constructPath(
			jEdit.getSettingsDirectory(),"macros",name);

		// Check if it's open
		Buffer buffer = jEdit.getBuffer(fileName);

		if(buffer == null)
			playMacroFromFile(view,name,fileName);
		else
			playMacroFromBuffer(view,name,buffer);
	}

	public static boolean playMacroCommand(View view, String macro,
		int lineNo, String line)
	{
		if(line.length() == 0 || line.charAt(0) == '#')
			return true;

		String action;
		String actionCommand;
		int index = line.indexOf('@');
		if(index == -1)
		{
			action = line;
			actionCommand = null;
		}
		else
		{
			action = line.substring(0,index);
			actionCommand = line.substring(index + 1);
		}

		ActionListener _action = jEdit.getAction(action);
		if(_action == null)
		{
			// it's a text area action
			ActionListener[] actions = DefaultInputHandler.ACTIONS;
			String[] names = DefaultInputHandler.ACTION_NAMES;

			// Start from end because insert-char is the last
			// one, and it's probably the most frequently invoked
			// action
			for(int i = actions.length - 1; i >= 0; i--)
			{
				if(names[i].equals(action))
				{
					_action = actions[i];
					break;
				}
			}
		}

		if(_action /* still */ == null)
		{
			Object[] args = { macro, new Integer(lineNo), action };
			GUIUtilities.error(view,"macro-error",args);
			return false;
		}

		JEditTextArea textArea = view.getTextArea();
		textArea.getInputHandler().executeAction(_action,textArea,
			actionCommand);

		return true;
	}

	public static String getLastMacro()
	{
		return lastMacro;
	}

	// private members
	private static String lastMacro;

	private static void playMacroFromBuffer(View view, String macro,
		Buffer buffer)
	{
		try
		{
			Element map = buffer.getDefaultRootElement();
			for(int i = 0; i < map.getElementCount(); i++)
			{
				Element lineElement = map.getElement(i);
				if(!playMacroCommand(view,macro,i,
					buffer.getText(
					lineElement.getStartOffset(),
					lineElement.getEndOffset()
					- lineElement.getStartOffset() - 1)))
					break;
			}
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,Macros.class,bl);
		}
	}

	private static void playMacroFromFile(View view, String macro, String path)
	{
		try
		{
			BufferedReader in = new BufferedReader(new FileReader(path));

			String line;
			int lineNo = 1;
			while((line = in.readLine()) != null)
			{
				if(!playMacroCommand(view,macro,lineNo,line))
					break;
				lineNo++;
			}

			in.close();
		}
		catch(IOException io)
		{
			String[] args = { io.getMessage() };
			GUIUtilities.error(view,"ioerror",args);
		}
	}

	private static String getActionName(ActionListener listener)
	{
		if(listener instanceof EditAction)
			return ((EditAction)listener).getName();
		else
		{
			// it's a text area action
			ActionListener[] actions = DefaultInputHandler.ACTIONS;
			String[] names = DefaultInputHandler.ACTION_NAMES;

			// Start from end because insert-char is the last
			// one, and it's probably the most frequently invoked
			// action
			for(int i = actions.length - 1; i >= 0; i--)
			{
				if(actions[i] == listener)
					return names[i];
			}
		}

		throw new InternalError("Unknown action: " + listener);
	}

	static class BufferRecorder implements InputHandler.MacroRecorder
	{
		Buffer buffer;
		boolean lastWasInsert;

		BufferRecorder(Buffer buffer)
		{
			this.buffer = buffer;
		}

		public void actionPerformed(ActionListener listener,
			String actionCommand)
		{
			String name = getActionName(listener);

			// Collapse multiple insert-char's
			if(name.equals("insert-char"))
			{
				if(lastWasInsert)
				{
					append(actionCommand);
					return;
				}
				else
					lastWasInsert = true;
			}
			else
				lastWasInsert = false;

			if(actionCommand == null)
				append("\n" + name);
			else
				append("\n" + name + "@" + actionCommand);
		}

		private void append(String str)
		{
			try
			{
				buffer.insertString(buffer.getLength(),str,null);
			}
			catch(BadLocationException bl)
			{
				Log.log(Log.ERROR,this,bl);
			}
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.8  1999/10/31 07:15:34  sp
 * New logging API, splash screen updates, bug fixes
 *
 * Revision 1.7  1999/10/28 09:07:21  sp
 * Directory list search
 *
 * Revision 1.6  1999/10/19 09:10:13  sp
 * pre5 bug fixing
 *
 * Revision 1.5  1999/10/17 04:16:28  sp
 * Bug fixing
 *
 * Revision 1.4  1999/10/16 09:43:00  sp
 * Final tweaking and polishing for jEdit 2.1final
 *
 * Revision 1.3  1999/10/10 06:38:45  sp
 * Bug fixes and quicksort routine
 *
 */
