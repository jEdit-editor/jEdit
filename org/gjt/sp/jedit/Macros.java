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
import java.util.Vector;
import org.gjt.sp.jedit.gui.InputHandler;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.jedit.msg.MacrosChanged;
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
	/**
	 * Rebuilds the macros list, and sends a MacrosChanged message
	 * (views update their Macros menu upon receiving it)
	 * @since jEdit 2.2pre4
	 */
	public static void loadMacros()
	{
		macros = new Vector();

		loadMacros(macros,"",new File(MiscUtilities.constructPath(
			jEdit.getJEditHome(),"macros")));

		String settings = jEdit.getSettingsDirectory();

		if(settings != null)
		{
			loadMacros(macros,"",new File(MiscUtilities.constructPath(
				settings,"macros")));
		}

		EditBus.send(new MacrosChanged(null));
	}

	/**
	 * Returns a vector with all known macros in it. Each element of
	 * this vector is either a Macro instance, or another vector.
	 * If it is a vector, the first element is a string label, the
	 * rest are again, either Macro instances or vectors.
	 * @since jEdit 2.2pre4
	 */
	public static Vector getMacros()
	{
		return macros;
	}

	/**
	 * Encapsulates the macro's label, name and path.
	 * @since jEdit 2.2pre4
	 */
	public static class Macro
	{
		public String label;
		public String name;
		public String path;

		public Macro(String label, String name, final String path)
		{
			this.label = label;
			this.name = name;
			this.path = path;

			String binding = jEdit.getProperty(name + ".shortcut");
			if(binding != null)
			{
				final EditAction action = jEdit.getAction("play-macro");
				jEdit.getInputHandler().addKeyBinding(binding,new EditAction()
				{
					public void actionPerformed(ActionEvent evt)
					{
						action.actionPerformed(
							new ActionEvent(
							evt.getSource(),
							evt.getID(),path));
					}

					public boolean isWrapper()
					{
						return true;
					}
				});
			}
		}

		// for debugging
		public String toString()
		{
			return label + ":" + name + ":" + path;
		}
	}

	/**
	 * Starts recording a macro.
	 * @param view The view
	 * @param name The macro name
	 * @param buffer The buffer to record to
	 */
	public static void beginRecording(View view, String name, Buffer buffer)
	{
		lastMacro = name;

		view.getInputHandler().setMacroRecorder(
			new BufferRecorder(view,buffer));
	}

	/**
	 * Stops a recording currently in progress.
	 * @param view The view
	 */
	public static void endRecording(View view)
	{
		InputHandler inputHandler = view.getInputHandler();
		BufferRecorder recorder = (BufferRecorder)inputHandler
			.getMacroRecorder();

		if(recorder != null)
		{
			if(lastMacro != null)
				view.setBuffer(recorder.buffer);
			inputHandler.setMacroRecorder(null);
		}
	}

	/**
	 * Plays a macro.
	 * @param view The view
	 * @param name The macro name
	 */
	public static void playMacro(View view, String name)
	{
		lastMacro = name;

		if(name == null)
		{
			Buffer buffer = jEdit.getBuffer(MiscUtilities.constructPath(
				jEdit.getSettingsDirectory(),"macros","__temporary__.macro"));
			if(buffer == null)
			{
				view.getToolkit().beep();
				return;
			}

			playMacroFromBuffer(view,"__temporary__.macro",buffer);
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

	/**
	 * Executes a macro command. This is intended for the XInsert
	 * plugins and friends.
	 * @param view The view
	 * @param macro The macro name for error reporting
	 * @param lineNo The line number for error reporting
	 * @param line The macro text
	 */
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

		EditAction _action = jEdit.getAction(action);

		if(_action == null)
		{
			Object[] args = { macro, new Integer(lineNo), action };
			GUIUtilities.error(view,"macro-error",args);
			return false;
		}

		view.getInputHandler().executeAction(_action,view.getTextArea(),
			actionCommand);

		return true;
	}

	/**
	 * Returns the last executed macro. This can be passed to
	 * <code>playMacro()</code>, etc.
	 */
	public static String getLastMacro()
	{
		return lastMacro;
	}

	// private members
	private static Vector macros;
	private static String lastMacro;

	private static void loadMacros(Vector vector, String path, File directory)
	{
		String[] macroFiles = directory.list();
		if(macroFiles == null)
			return;

		MiscUtilities.quicksort(macroFiles,new MiscUtilities.StringICaseCompare());

		for(int i = 0; i < macroFiles.length; i++)
		{
			String name = macroFiles[i];
			File file = new File(directory,name);
			if(name.toLowerCase().endsWith(".macro"))
			{
				String label = name.substring(0,name.length() - 6);
				vector.addElement(new Macro(label,path + label,file.getPath()));
			}
			else if(file.isDirectory())
			{
				Vector submenu = new Vector();
				submenu.addElement(name);
				loadMacros(submenu,path + name + '/',file);
				vector.addElement(submenu);
			}
		}
	}

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
			Log.log(Log.ERROR,Macros.class,io);
			String[] args = { io.getMessage() };
			GUIUtilities.error(view,"ioerror",args);
		}
	}

	static class BufferRecorder implements InputHandler.MacroRecorder
	{
		View view;
		Buffer buffer;
		boolean lastWasInsert;

		BufferRecorder(View view, Buffer buffer)
		{
			this.view = view;
			this.buffer = buffer;
		}

		public void actionPerformed(EditAction action, String actionCommand)
		{
			if(buffer.isClosed())
			{
				view.getInputHandler().setMacroRecorder(null);
				view.popStatus();
				return;
			}

			String name = action.getName();

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
 * Revision 1.22  2000/04/28 09:29:11  sp
 * Key binding handling improved, VFS updates, some other stuff
 *
 * Revision 1.21  2000/04/14 11:57:38  sp
 * Text area actions moved to org.gjt.sp.jedit.actions package
 *
 * Revision 1.20  2000/04/01 12:21:27  sp
 * mode cache implemented
 *
 * Revision 1.19  1999/12/19 11:14:28  sp
 * Static abbrev expansion started
 *
 * Revision 1.18  1999/12/13 03:40:29  sp
 * Bug fixes, syntax is now mostly GPL'd
 *
 * Revision 1.17  1999/12/05 03:01:05  sp
 * Perl token marker bug fix, file loading is deferred, style option pane fix
 *
 * Revision 1.16  1999/11/27 06:01:20  sp
 * Faster file loading, geometry fix
 *
 * Revision 1.15  1999/11/23 08:03:21  sp
 * Miscallaeneous stuff
 *
 * Revision 1.14  1999/11/21 01:20:30  sp
 * Bug fixes, EditBus updates, fixed some warnings generated by jikes +P
 *
 * Revision 1.13  1999/11/19 08:54:51  sp
 * EditBus integrated into the core, event system gone, bug fixes
 *
 * Revision 1.12  1999/11/16 08:21:20  sp
 * Various fixes, attempt at beefing up expand-abbrev
 *
 * Revision 1.11  1999/11/10 10:43:01  sp
 * Macros can now have shortcuts, various miscallaneous updates
 *
 */
