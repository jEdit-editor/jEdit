/*
 * Macros.java - Macro manager
 * Copyright (C) 1999, 2000 Slava Pestov
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
import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.util.*;
import org.gjt.sp.jedit.gui.InputHandler;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.msg.*;
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
		macroList = new Vector();
		macroHierarchy = new Vector();
		macrosHash = new Hashtable();

		systemMacroPath = MiscUtilities.constructPath(
			jEdit.getJEditHome(),"macros");
		loadMacros(macroHierarchy,"",new File(systemMacroPath));

		String settings = jEdit.getSettingsDirectory();

		if(settings != null)
		{
			userMacroPath = MiscUtilities.constructPath(
				settings,"macros");
			loadMacros(macroHierarchy,"",new File(userMacroPath));
		}

		// sort macro list
		MiscUtilities.quicksort(macroList,new MiscUtilities.StringICaseCompare());

		EditBus.send(new MacrosChanged(null));
	}

	/**
	 * Returns a vector hierarchy with all known macros in it.
	 * Each element of this vector is either a macro name string,
	 * or another vector. If it is a vector, the first element is a
	 * string label, the rest are again, either macro name strings
	 * or vectors.
	 * @since jEdit 2.6pre1
	 */
	public static Vector getMacroHierarchy()
	{
		return macroHierarchy;
	}

	/**
	 * Returns a single vector with all known macros in it. Each
	 * element of this vector is a macro name string.
	 * @since jEdit 2.6pre1
	 */
	public static Vector getMacroList()
	{
		return macroList;
	}

	/**
	 * Returns the macro with the specified name.
	 * @param macro The macro's name
	 * @since jEdit 2.6pre1
	 */
	public static Macro getMacro(String macro)
	{
		return (Macro)macrosHash.get(macro);
	}

	/**
	 * Encapsulates the macro's label, name and path.
	 * @since jEdit 2.2pre4
	 */
	public static class Macro
	{
		public String name;
		public String path;

		public Macro(final String name, String path)
		{
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
							evt.getID(),name));
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
			return name + ":" + path;
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
			recorder.dispose();
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

		// This hackery is necessary to prevent actions inside the
		// macro from picking up the repeat count
		InputHandler inputHandler = view.getInputHandler();
		int repeatCount = inputHandler.getRepeatCount();
		inputHandler.setRepeatEnabled(false);

		for(int i = repeatCount; i > 0; i--)
		{
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
			}
			else
			{
				String fileName = MiscUtilities.constructPath(
					jEdit.getSettingsDirectory(),"macros",name);

				// Check if it's open
				Buffer buffer = jEdit.getBuffer(fileName);

				if(buffer == null)
					playMacroFromFile(view,name,fileName);
				else
					playMacroFromBuffer(view,name,buffer);
			}
		}
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
			actionCommand = substituteRegisters(line.substring(
				index + 1));
		}

		if(action.equals("input"))
		{
			char register;
			index = actionCommand.indexOf('@');
			if(index != 1)
				register = '$';
			else
			{
				register = actionCommand.charAt(0);
				actionCommand = actionCommand.substring(2);
			}

			String retVal = (String)JOptionPane.showInputDialog(view,
				actionCommand,jEdit.getProperty("macro-input.title"),
				JOptionPane.QUESTION_MESSAGE);
			if(retVal == null)
				return false;
			else
			{
				Registers.setRegister(register,new
					Registers.StringRegister(retVal));
				return true;
			}
		}
		else
		{
			EditAction _action = jEdit.getAction(action);

			if(_action == null)
			{
				Object[] args = { macro, new Integer(lineNo), action };
				GUIUtilities.error(view,"macro-error",args);
				return false;
			}

			view.getInputHandler().executeAction(_action,view.getTextArea(),
				actionCommand);

			// wait for all I/O to complete before going on to the
			// next action
			VFSManager.waitForRequests();

			return true;
		}
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
	private static String systemMacroPath;
	private static String userMacroPath;

	private static Vector macroList;
	private static Vector macroHierarchy;
	private static Hashtable macrosHash;
	private static String lastMacro;

	static
	{
		EditBus.addToBus(new MacrosEBComponent());
	}

	private static void loadMacros(Vector vector, String path, File directory)
	{
		String[] macroFiles = directory.list();
		if(macroFiles == null)
			return;

		MiscUtilities.quicksort(macroFiles,new MiscUtilities.StringICaseCompare());

		for(int i = 0; i < macroFiles.length; i++)
		{
			String fileName = macroFiles[i];
			File file = new File(directory,fileName);
			if(fileName.toLowerCase().endsWith(".macro"))
			{
				String label = fileName.substring(0,fileName.length() - 6);
				String name = path + label;
				Macro newMacro = new Macro(name,file.getPath());
				macrosHash.put(name,newMacro);
				vector.addElement(name);
				macroList.addElement(name);
			}
			else if(file.isDirectory())
			{
				Vector submenu = new Vector();
				submenu.addElement(fileName.replace('_',' '));
				loadMacros(submenu,path + fileName + '/',file);
				vector.addElement(submenu);
			}
		}
	}

	private static void playMacroFromBuffer(View view, String macro,
		Buffer buffer)
	{
		Buffer _viewBuffer = view.getBuffer();

		try
		{
			_viewBuffer.beginCompoundEdit();

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
		finally
		{
			_viewBuffer.endCompoundEdit();
		}
	}

	private static void playMacroFromFile(View view, String macro, String path)
	{
		Buffer _viewBuffer = view.getBuffer();

		try
		{
			_viewBuffer.beginCompoundEdit();

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
		finally
		{
			_viewBuffer.endCompoundEdit();
		}
	}

	private static String substituteRegisters(String actionCommand)
	{
		StringBuffer buf = new StringBuffer();
		boolean backslash = false;
		for(int i = 0; i < actionCommand.length(); i++)
		{
			char ch = actionCommand.charAt(i);
			if(ch == '\\')
			{
				if(backslash)
				{
					backslash = false;
					buf.append('\\');
				}
				else
					backslash = true;
			}
			else if(ch == '$')
			{
				if(backslash || i == actionCommand.length() - 1)
				{
					buf.append(ch);
					backslash = false;
				}
				else
				{
					ch = actionCommand.charAt(++i);
					Registers.Register reg = Registers.getRegister(ch);
					if(reg != null)
					{
						String str = reg.toString();
						if(str != null)
							buf.append(str);
					}
				}
			}
			else
			{
				if(backslash)
				{
					buf.append('\\');
					backslash = false;
				}
				buf.append(ch);
			}
		}

		return buf.toString();
	}

	static class MacrosEBComponent implements EBComponent
	{
		public void handleMessage(EBMessage msg)
		{
			if(msg instanceof BufferUpdate)
			{
				BufferUpdate bmsg = (BufferUpdate)msg;
				if(bmsg.getWhat() == BufferUpdate.DIRTY_CHANGED
					&& !bmsg.getBuffer().isDirty())
					maybeReloadMacros(bmsg.getBuffer().getPath());
			}
			else if(msg instanceof VFSUpdate)
			{
				maybeReloadMacros(((VFSUpdate)msg).getPath());
			}
		}

		private void maybeReloadMacros(String path)
		{
			// On Windows and MacOS, path names are case insensitive
			if(File.separatorChar == '\\' || File.separatorChar == ':')
			{
				path = path.toLowerCase();
				if(path.startsWith(systemMacroPath.toLowerCase()))
					loadMacros();

				if(userMacroPath != null && path.startsWith(
					userMacroPath.toLowerCase()))
					loadMacros();
			}
			else
			{
				if(path.startsWith(systemMacroPath))
					loadMacros();

				if(userMacroPath != null && path.startsWith(userMacroPath))
					loadMacros();
			}
		}
	}

	static class BufferRecorder implements InputHandler.MacroRecorder,
		EBComponent
	{
		View view;
		Buffer buffer;
		boolean lastWasInsert;

		BufferRecorder(View view, Buffer buffer)
		{
			this.view = view;
			this.buffer = buffer;
			EditBus.addToBus(this);
		}

		public void actionPerformed(EditAction action, String actionCommand)
		{
			// Escape $ characters in action command
			if(actionCommand != null)
			{
				StringBuffer buf = new StringBuffer();
				for(int i = 0; i < actionCommand.length(); i++)
				{
					char ch = actionCommand.charAt(i);
					if(ch == '$')
						buf.append("\\$");
					else
						buf.append(ch);
				}
				actionCommand = buf.toString();
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

		public void handleMessage(EBMessage msg)
		{
			if(msg instanceof BufferUpdate)
			{
				BufferUpdate bmsg = (BufferUpdate)msg;
				if(bmsg.getWhat() == BufferUpdate.CLOSED)
				{
					if(bmsg.getBuffer() == buffer)
						dispose();
				}
			}
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

		private void dispose()
		{
			view.getInputHandler().setMacroRecorder(null);
			view.showStatus(null);
			EditBus.removeFromBus(this);
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.38  2000/10/12 09:28:26  sp
 * debugging and polish
 *
 * Revision 1.37  2000/10/05 04:30:09  sp
 * *** empty log message ***
 *
 * Revision 1.36  2000/09/26 10:19:46  sp
 * Bug fixes, spit and polish
 *
 * Revision 1.35  2000/09/03 03:16:52  sp
 * Search bar integrated with command line, enhancements throughout
 *
 * Revision 1.34  2000/08/11 09:06:51  sp
 * Browser option pane
 *
 * Revision 1.33  2000/08/10 08:30:40  sp
 * VFS browser work, options dialog work, more random tweaks
 *
 * Revision 1.32  2000/07/26 07:48:44  sp
 * stuff
 *
 * Revision 1.31  2000/07/19 08:35:59  sp
 * plugin devel docs updated, minor other changes
 *
 * Revision 1.30  2000/07/14 06:00:44  sp
 * bracket matching now takes syntax info into account
 *
 * Revision 1.29  2000/07/12 09:11:38  sp
 * macros can be added to context menu and tool bar, menu bar layout improved
 *
 * Revision 1.28  2000/06/04 08:57:35  sp
 * GUI updates, bug fixes
 *
 * Revision 1.27  2000/05/13 05:13:31  sp
 * Mode option pane
 *
 * Revision 1.26  2000/05/09 10:51:51  sp
 * New status bar, a few other things
 *
 */
