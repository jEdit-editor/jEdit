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
import org.gjt.sp.jedit.browser.*;
import org.gjt.sp.jedit.gui.*;
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
	 * Opens the system macro directory in a VFS browser.
	 * @param view The view
	 * @since jEdit 2.7pre2
	 */
	public static void browseSystemMacros(View view)
	{
		DockableWindowManager dockableWindowManager
			= view.getDockableWindowManager();

		dockableWindowManager.addDockableWindow(VFSBrowserDockable.NAME);
		VFSBrowser browser = (VFSBrowser)dockableWindowManager
			.getDockableWindow(VFSBrowserDockable.NAME)
			.getComponent();

		browser.setDirectory(MiscUtilities.constructPath(
			jEdit.getJEditHome(),"macros"));
	}

	/**
	 * Opens the user macro directory in a VFS browser.
	 * @param view The view
	 * @since jEdit 2.7pre2
	 */
	public static void browseUserMacros(View view)
	{
		String settings = jEdit.getSettingsDirectory();

		if(settings == null)
		{
			GUIUtilities.error(view,"no-settings",null);
			return;
		}

		DockableWindowManager dockableWindowManager
			= view.getDockableWindowManager();

		dockableWindowManager.addDockableWindow(VFSBrowserDockable.NAME);
		VFSBrowser browser = (VFSBrowser)dockableWindowManager
			.getDockableWindow(VFSBrowserDockable.NAME)
			.getComponent();

		browser.setDirectory(MiscUtilities.constructPath(settings,"macros"));
	}

	/**
	 * Rebuilds the macros list, and sends a MacrosChanged message
	 * (views update their Macros menu upon receiving it)
	 * @since jEdit 2.2pre4
	 */
	public static void loadMacros()
	{
		macroList = new Vector();
		macroHierarchy = new Vector();

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
	 * Encapsulates the macro's label, name and path.
	 * @since jEdit 2.2pre4
	 */
	public static class Macro
	{
		public String name;
		public String path;
		public EditAction action;

		public Macro(String name, final String path)
		{
			this.name = name;
			this.path = path;

			action = new EditAction()
			{
				public void actionPerformed(ActionEvent evt)
				{
					BeanShell.runScript(getView(evt),path);
				}

				public boolean isWrapper()
				{
					return true;
				}
			};

			String binding = jEdit.getProperty(name + ".shortcut");
			if(binding != null)
				jEdit.getInputHandler().addKeyBinding(binding,action);
		}

		// for debugging
		public String toString()
		{
			return name + ":" + path;
		}
	}

	/**
	 * Starts recording a temporary macro.
	 * @param view The view
	 * @since jEdit 2.7pre2
	 */
	public static void recordTemporaryMacro(View view)
	{
		String settings = jEdit.getSettingsDirectory();

		if(settings == null)
		{
			GUIUtilities.error(view,"no-settings",new String[0]);
			return;
		}
		if(view.getMacroRecorder() != null)
		{
			GUIUtilities.error(view,"already-recording",new String[0]);
			return;
		}

		Buffer buffer = jEdit.openFile(null,settings + File.separator
			+ "macros","Temporary_Macro.bsh",false,true);

		if(buffer == null)
			return;

		try
		{
			buffer.remove(0,buffer.getLength());
			buffer.insertString(0,jEdit.getProperty("macro.temp.header"),null);
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,Macros.class,bl);
		}

		recordMacro(view,null,buffer);
	}

	/**
	 * Starts recording a macro.
	 * @param view The view
	 * @since jEdit 2.7pre2
	 */
	public static void recordMacro(View view)
	{
		String settings = jEdit.getSettingsDirectory();

		if(settings == null)
		{
			GUIUtilities.error(view,"no-settings",new String[0]);
			return;
		}

		if(view.getMacroRecorder() != null)
		{
			GUIUtilities.error(view,"already-recording",new String[0]);
			return;
		}

		String name = GUIUtilities.input(view,"record",null);
		if(name == null)
			return;

		name = name.replace(' ','_');

		Buffer buffer = jEdit.openFile(null,null,
			MiscUtilities.constructPath(settings,"macros",
			name + ".macro"),
			false,true);

		if(buffer == null)
			return;

		try
		{
			buffer.remove(0,buffer.getLength());
			buffer.insertString(0,jEdit.getProperty("macro.header"),null);
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,Macros.class,bl);
		}

		recordMacro(view,name,buffer);
	}

	/**
	 * Stops a recording currently in progress.
	 * @param view The view
	 * @since jEdit 2.7pre2
	 */
	public static void stopRecording(View view)
	{
		InputHandler inputHandler = view.getInputHandler();
		Recorder recorder = view.getMacroRecorder();

		if(recorder != null)
		{
			view.setMacroRecorder(null);
			if(lastMacro != null)
				view.setBuffer(recorder.buffer);
			recorder.dispose();
		}
	}

	/**
	 * Runs the temporary macro.
	 * @param view The view
	 * @since jEdit 2.7pre2
	 */
	public static void runTemporaryMacro(View view)
	{
		String settings = jEdit.getSettingsDirectory();

		if(settings == null)
		{
			GUIUtilities.error(view,"no-settings",new String[0]);
			return;
		}

		lastMacro = MiscUtilities.constructPath(
			jEdit.getSettingsDirectory(),"macros",
			"Temporary_Macro.bsh");

		BeanShell.runScript(view,lastMacro);
	}

	/**
	 * Runs the most recently run or recorded macro.
	 * @param view The view
	 * @since jEdit 2.7pre2
	 */
	public static void runLastMacro(View view)
	{
		if(lastMacro == null)
			view.getToolkit().beep();
		else
			BeanShell.runScript(view,lastMacro);
	}

	// private members
	private static String systemMacroPath;
	private static String userMacroPath;

	private static Vector macroList;
	private static Vector macroHierarchy;
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
			if(fileName.toLowerCase().endsWith(".bsh"))
			{
				String label = fileName.substring(0,fileName.length() - 4);
				String name = path + label;
				Macro newMacro = new Macro(name,file.getPath());
				vector.addElement(newMacro);
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

	/**
	 * Starts recording a macro.
	 * @param view The view
	 * @param name The macro name
	 * @param buffer The buffer to record to
	 */
	private static void recordMacro(View view, String name, Buffer buffer)
	{
		lastMacro = name;

		view.setRecordingStatus(true);
		view.setMacroRecorder(new Recorder(view,buffer));
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

	public static class Recorder implements EBComponent
	{
		View view;
		Buffer buffer;

		public Recorder(View view, Buffer buffer)
		{
			this.view = view;
			this.buffer = buffer;
			EditBus.addToBus(this);
		}

		public void record(String code)
		{
			append("\n");
			append(code);
		}

		public void record(int repeat, String code)
		{
			if(repeat == 1)
				record(code);
			else
			{
				record("for(int i = 1; i <= " + repeat + "; i++)\n"
					+ "{\n"
					+ code + ";\n"
					+ "}");
			}
		}

		public void record(int repeat, char ch)
		{
			String charStr = MiscUtilities.charsToEscapes(String.valueOf(ch));

			record(repeat,"textArea.userInput(\"" + charStr + "\");");
		}

		public void handleMessage(EBMessage msg)
		{
			if(msg instanceof BufferUpdate)
			{
				BufferUpdate bmsg = (BufferUpdate)msg;
				if(bmsg.getWhat() == BufferUpdate.CLOSED)
				{
					if(bmsg.getBuffer() == buffer)
						stopRecording(view);
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
			view.setRecordingStatus(false);
			EditBus.removeFromBus(this);
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.44  2000/11/16 10:25:16  sp
 * More macro work
 *
 * Revision 1.43  2000/11/16 04:01:10  sp
 * BeanShell macros started
 *
 * Revision 1.42  2000/11/12 05:36:48  sp
 * BeanShell integration started
 *
 * Revision 1.41  2000/11/02 09:19:31  sp
 * more features
 *
 * Revision 1.40  2000/10/30 07:14:03  sp
 * 2.7pre1 branched, GUI improvements
 *
 * Revision 1.39  2000/10/13 06:57:19  sp
 * Edit User/System Macros command, gutter mouse handling improved
 *
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
 */
