/*
 * Sessions.java - Session manager
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

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import java.io.*;
import java.util.StringTokenizer;
import org.gjt.sp.util.Log;

/**
 * Loads and saves sessions. A session is a file with a list of path names
 * and attributes. It can be used to save and restore a working environment.
 * @author Slava Pestov
 * @version $Id$
 */
public class Sessions
{
	/**
	 * Displays the 'load session' dialog box, and loads the selected
	 * session.
	 * @param view The view
	 * @since jEdit 2.7pre2
	 */
	public static void showLoadSessionDialog(View view)
	{
		String settingsDirectory = jEdit.getSettingsDirectory();
		if(settingsDirectory == null)
		{
			GUIUtilities.error(view,"no-settings",null);
			return;
		}

		String path = GUIUtilities.showFileDialog(
			view,MiscUtilities.constructPath(settingsDirectory,
			"sessions"),JFileChooser.OPEN_DIALOG);

		if(path != null)
		{
			Buffer buffer = Sessions.loadSession(path,false);
			if(buffer != null)
				view.setBuffer(buffer);
		}
	}

	/**
	 * Loads a session.
	 * @param session The file name, relative to $HOME/.jedit/sessions
	 * (.session suffix not required)
	 * @param ignoreNotFound If false, an exception will be printed if
	 * the session doesn't exist. If true, it will silently fail
	 * @since jEdit 2.2pre1
	 */
	public static Buffer loadSession(String session, boolean ignoreNotFound)
	{
		String filename = createSessionFileName(session);

		Buffer buffer = null;

		try
		{
			BufferedReader in = new BufferedReader(new FileReader(filename));

			String line;
			while((line = in.readLine()) != null)
			{
				Buffer _buffer = readSessionCommand(line);
				if(_buffer != null)
					buffer = _buffer;
			}

			in.close();
		}
		catch(FileNotFoundException fnf)
		{
			Log.log(Log.NOTICE,Sessions.class,fnf);
			if(ignoreNotFound)
				return null;
			String[] args = { filename };
			GUIUtilities.error(null,"filenotfound",args);
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,Sessions.class,io);
			String[] args = { io.getMessage() };
			GUIUtilities.error(null,"ioerror",args);
		}

		return buffer;
	}

	/**
	 * Displays the 'save session' dialog box, and saves the selected
	 * session.
	 * @param view The view
	 * @since jEdit 2.7pre2
	 */
	public static void showSaveSessionDialog(View view)
	{
		String settingsDirectory = jEdit.getSettingsDirectory();
		if(settingsDirectory == null)
		{
			GUIUtilities.error(view,"no-settings",null);
			return;
		}

		String path = GUIUtilities.showFileDialog(
			view,MiscUtilities.constructPath(settingsDirectory,
			"sessions"),JFileChooser.SAVE_DIALOG);

		if(path != null)
		{
			Sessions.saveSession(view,path);
		}
	}

	/**
	 * Saves the session
	 * @param view The view this is being saved from. The saved caret
	 * information and current buffer is taken from this view
	 * @param session The file name, relative to $HOME/.jedit/sessions
	 * (.session suffix not required)
	 * @since jEdit 2.2pre1
	 */
	public static void saveSession(View view, String session)
	{
		view.getEditPane().saveCaretInfo();

		String lineSep = System.getProperty("line.separator");
		String filename = createSessionFileName(session);

		Buffer buffer = jEdit.getFirstBuffer();

		try
		{
			BufferedWriter out = new BufferedWriter(new FileWriter(filename));

			while(buffer != null)
			{
				if(!buffer.isUntitled())
				{
					writeSessionCommand(view,buffer,out);
					out.write(lineSep);
				}
				buffer = buffer.getNext();
			}

			out.close();
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,Sessions.class,io);
			String[] args = { io.getMessage() };
			GUIUtilities.error(null,"ioerror",args);
		}
	}

	/**
	 * Converts a session name (eg, default) to a full path name
	 * (eg, /home/slava/.jedit/sessions/default.session)
	 * @since jEdit 2.2pre1
	 */
	public static String createSessionFileName(String session)
	{
		String filename = MiscUtilities.constructPath(
			jEdit.getSettingsDirectory(),"sessions",session);

		if(!filename.toLowerCase().endsWith(".session"))
			filename = filename + ".session";

		return filename;
	}

	// private members

	/**
	 * Parse one line from a session file.
	 */
	private static Buffer readSessionCommand(String line)
	{
		String path = null;
		Integer selStart = null;
		Integer selEnd = null;
		Integer firstLine = null;
		Integer horizontalOffset = null;
		boolean current = false;

		// handle path:XXX for backwards compatibility
		// with jEdit 2.2 sessions
		if(line.startsWith("path:"))
			line = line.substring(5);

		StringTokenizer st = new StringTokenizer(line,"\t");
		path = st.nextToken();

		// ignore all tokens except for 'current' to maintain
		// compatibility with jEdit 2.2 sessions
		while(st.hasMoreTokens())
		{
			String token = st.nextToken();

			if(token.equals("current"))
				current = true;
		}

		if(path == null)
			return null;

		Buffer buffer = jEdit.openFile(null,path);
		if(buffer == null)
			return null;

		return (current ? buffer : null);
	}

	/**
	 * Writes one line to a session file.
	 */
	private static void writeSessionCommand(View view, Buffer buffer,
		Writer out) throws IOException
	{
		out.write(buffer.getPath());

		if(view.getBuffer() == buffer)
			out.write("\tcurrent");
	}
}
