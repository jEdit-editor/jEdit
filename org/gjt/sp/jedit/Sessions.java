/*
 * Sessions.java - Session manager
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

		StringTokenizer st = new StringTokenizer(line,"\t");
		while(st.hasMoreTokens())
		{
			String token = st.nextToken();

			if(token.startsWith("path:"))
				path = token.substring(5);
			else if(token.startsWith("selStart:"))
				selStart = new Integer(token.substring(9));
			else if(token.startsWith("selEnd:"))
				selEnd = new Integer(token.substring(7));
			else if(token.startsWith("firstLine:"))
				firstLine = new Integer(token.substring(10));
			else if(token.startsWith("horizontalOffset:"))
				horizontalOffset = new Integer(token.substring(17));
			else if(token.equals("current"))
				current = true;
		}

		if(path == null)
			return null;

		Buffer buffer = jEdit.openFile(null,path);
		if(buffer == null)
			return null;

		if(selStart != null && selEnd != null
			&& firstLine != null && horizontalOffset != null)
		{
			buffer.putProperty(Buffer.SELECTION_START,selStart);
			buffer.putProperty(Buffer.SELECTION_END,selEnd);
			buffer.putProperty(Buffer.SCROLL_VERT,firstLine);
			buffer.putProperty(Buffer.SCROLL_HORIZ,horizontalOffset);
		}

		return (current ? buffer : null);
	}

	/**
	 * Writes one line to a session file.
	 */
	private static void writeSessionCommand(View view, Buffer buffer,
		Writer out) throws IOException
	{
		out.write("path:");
		out.write(buffer.getPath());

		Integer start = (Integer)buffer.getProperty(Buffer.SELECTION_START);
		Integer end = (Integer)buffer.getProperty(Buffer.SELECTION_END);
		Integer firstLine = (Integer)buffer.getProperty(Buffer.SCROLL_VERT);
		Integer horizontalOffset = (Integer)buffer.getProperty(
			Buffer.SCROLL_HORIZ);

		if(start != null && end != null
			&& firstLine != null && horizontalOffset != null)
		{
			out.write("\tselStart:");
			out.write(start.toString());

			out.write("\tselEnd:");
			out.write(end.toString());

			out.write("\tfirstLine:");
			out.write(firstLine.toString());

			out.write("\thorizontalOffset:");
			out.write(horizontalOffset.toString());
		}

		if(view.getBuffer() == buffer)
			out.write("\tcurrent");
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.18  2000/07/19 08:35:59  sp
 * plugin devel docs updated, minor other changes
 *
 * Revision 1.17  2000/05/07 05:48:30  sp
 * You can now edit several buffers side-by-side in a split view
 *
 * Revision 1.16  2000/04/27 08:32:57  sp
 * VFS fixes, read only fixes, macros can prompt user for input, improved
 * backup directory feature
 *
 * Revision 1.15  2000/04/24 04:45:36  sp
 * New I/O system started, and a few minor updates
 *
 * Revision 1.14  2000/04/23 03:36:39  sp
 * Minor fixes
 *
 * Revision 1.13  2000/03/20 03:42:55  sp
 * Smoother syntax package, opening an already open file will ask if it should be
 * reloaded, maybe some other changes
 *
 * Revision 1.12  2000/02/20 03:14:13  sp
 * jEdit.getBrokenPlugins() method
 *
 * Revision 1.11  1999/12/21 06:50:51  sp
 * Documentation updates, abbrevs option pane finished, bug fixes
 *
 * Revision 1.10  1999/11/30 01:37:35  sp
 * New view icon, shortcut pane updates, session bug fix
 *
 * Revision 1.9  1999/11/29 02:45:50  sp
 * Scroll bar position saved when switching buffers
 *
 * Revision 1.8  1999/11/21 01:20:30  sp
 * Bug fixes, EditBus updates, fixed some warnings generated by jikes +P
 *
 * Revision 1.7  1999/11/19 08:54:51  sp
 * EditBus integrated into the core, event system gone, bug fixes
 *
 * Revision 1.6  1999/11/09 10:14:34  sp
 * Macro code cleanups, menu item and tool bar clicks are recorded now, delete
 * word commands, check box menu item support
 *
 * Revision 1.5  1999/11/07 06:51:43  sp
 * Check box menu items supported
 *
 * Revision 1.4  1999/11/06 02:06:50  sp
 * Logging updates, bug fixing, icons, various other stuff
 *
 * Revision 1.3  1999/10/31 07:15:34  sp
 * New logging API, splash screen updates, bug fixes
 *
 * Revision 1.2  1999/10/28 09:07:21  sp
 * Directory list search
 *
 */
