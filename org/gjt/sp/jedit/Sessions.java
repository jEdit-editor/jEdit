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

import java.io.*;
import java.util.StringTokenizer;

/**
 * Loads and saves sessions.
 * @author Slava Pestov
 * @version $Id$
 */
public class Sessions
{
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
			if(ignoreNotFound)
				return null;
			String[] args = { filename };
			GUIUtilities.error(null,"filenotfound",args);
		}
		catch(IOException io)
		{
			String[] args = { io.getMessage() };
			GUIUtilities.error(null,"ioerror",args);
		}

		return buffer;
	}

	public static void saveSession(View view, String session)
	{
		view.saveCaretInfo();

		String filename = createSessionFileName(session);

		Buffer buffer = jEdit.getFirstBuffer();

		try
		{
			BufferedWriter out = new BufferedWriter(new FileWriter(filename));

			while(buffer != null)
			{
				writeSessionCommand(view,buffer,out);
				out.write("\r\n");
				buffer = buffer.getNext();
			}

			out.close();
		}
		catch(IOException io)
		{
			String[] args = { io.getMessage() };
			GUIUtilities.error(null,"ioerror",args);
		}
	}

	public static String createSessionFileName(String session)
	{
		String filename = MiscUtilities.constructPath(
			jEdit.getSettingsDirectory(),"sessions",session);

		if(!filename.toLowerCase().endsWith(".session"))
			filename = filename + ".session";

		return filename;
	}

	// private members
	private static Buffer readSessionCommand(String line)
	{
		String path = null;
		boolean newFile = false;
		int selStart = 0;
		int selEnd = 0;
		boolean rectSel = false;
		boolean current = false;

		StringTokenizer st = new StringTokenizer(line,"\t");
		while(st.hasMoreTokens())
		{
			String token = st.nextToken();

			if(token.startsWith("path:"))
				path = token.substring(5);
			else if(token.equals("new"))
				newFile = true;
			else if(token.startsWith("selStart:"))
				selStart = Integer.parseInt(token.substring(9));
			else if(token.startsWith("selEnd:"))
				selEnd = Integer.parseInt(token.substring(7));
			else if(token.equals("rectSel"))
				rectSel = true;
			else if(token.equals("current"))
				current = true;
		}

		if(path == null)
			return null;

		Buffer buffer = jEdit.openFile(null,null,path,false,newFile);
		buffer.setCaretInfo(selStart,selEnd,rectSel);
		return (current ? buffer : null);
	}

	private static void writeSessionCommand(View view, Buffer buffer,
		Writer out) throws IOException
	{
		out.write("path:");
		out.write(buffer.getPath());

		if(buffer.isNewFile())
			out.write("\tnew");

		out.write("\tselStart:");
		out.write(String.valueOf(buffer.getSavedSelStart()));

		out.write("\tselEnd:");
		out.write(String.valueOf(buffer.getSavedSelEnd()));

		if(buffer.isSelectionRectangular())
			out.write("\trectSel");

		if(view.getBuffer() == buffer)
			out.write("\tcurrent");
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.2  1999/10/28 09:07:21  sp
 * Directory list search
 *
 * Revision 1.1  1999/10/26 07:43:59  sp
 * Session loading and saving, directory list search started
 *
 */
