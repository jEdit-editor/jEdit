/*
 * IORequest.java - I/O request
 * Copyright (C) 2000 Slava Pestov
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

package org.gjt.sp.jedit.io;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Segment;
import java.io.*;
import java.util.zip.*;
import java.util.Vector;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.*;

/**
 * An I/O request.
 * @author Slava Pestov
 * @version $Id$
 */
public class IORequest extends WorkRequest
{
	/**
	 * Size of I/O buffers.
	 */
	public static final int IOBUFSIZE = 32768;

	/**
	 * A file load request.
	 */
	public static final int LOAD = 0;

	/**
	 * A file save request.
	 */
	public static final int SAVE = 1;

	/**
	 * Creates a new I/O request.
	 * @param type The request type
	 * @param view The view
	 * @param buffer The buffer
	 * @param path The path
	 * @param vfs The VFS
	 */
	public IORequest(int type, View view, Buffer buffer, String path, VFS vfs)
	{
		this.type = type;
		this.view = view;
		this.buffer = buffer;
		this.path = path;
		this.vfs = vfs;

		markersPath = MiscUtilities.getFileParent(path)
			+ '.' + MiscUtilities.getFileName(path)
			+ ".marks";
	}

	/**
	 * If type is LOAD, calls buffer._read(vfs._createInputStream()).
	 * If type is SAVE, calls buffer._write(vfs._createOutputStream());
	 */
	public void run()
	{
		View[] views = null;

		try
		{
			views = jEdit.getViews();
			String status = (type == LOAD ? "loading" : "saving");
			String[] args = { MiscUtilities.getFileName(path) };
			String message = jEdit.getProperty("view.status." + status,args);
			for(int i = 0; i < views.length; i++)
			{
				views[i].showStatus(message);
			}

			switch(type)
			{
			case LOAD:
				load();
				break;
			case SAVE:
				save();
				break;
			}
		}
		finally
		{
			if(views != null)
			{
				for(int i = 0; i < views.length; i++)
				{
					views[i].showStatus(null);
				}
			}
		}
	}

	public String toString()
	{
		return getClass().getName() + "[type=" + (type == LOAD
			? "LOAD" : "SAVE") + ",view=" + view + ",buffer="
			+ buffer + ",vfs=" + vfs + "]";
	}

	// private members
	private int type;
	private View view;
	private Buffer buffer;
	private String path;
	private String markersPath;
	private VFS vfs;

	private void load()
	{
		InputStream in = null;

		try
		{
			try
			{
				setAbortable(true);

				in = vfs._createInputStream(view,buffer,path,false);
				if(in == null)
					return;

				if(path.endsWith(".gz"))
					in = new GZIPInputStream(in);

				read(buffer,in);
			}
			catch(BadLocationException bl)
			{
				Log.log(Log.ERROR,this,bl);
			}
			catch(IOException io)
			{
				Log.log(Log.ERROR,this,io);
				Object[] args = { io.toString() };
				VFSManager.error(view,"ioerror",args);
			}

			try
			{
				// just before inserting the text into the
				// buffer, read() calls setAbortable(false)
				setAbortable(true);

				in = vfs._createInputStream(view,buffer,markersPath,true);
				if(in != null)
					readMarkers(buffer,in);
			}
			catch(IOException io)
			{
				// ignore
			}
		}
		catch(WorkThread.Abort a)
		{
			if(in != null)
			{
				try
				{
					in.close();
				}
				catch(IOException io)
				{
				}
			}
		}

		try
		{
			vfs._loadComplete(buffer);
		}
		catch(IOException io)
		{
		}
		catch(WorkThread.Abort a)
		{
		}
	}

	/**
	 * Reads the buffer from the specified input stream. Read and
	 * understand all these notes if you want to snarf this code for
	 * your own app; it has a number of subtle behaviours which are
	 * not entirely obvious.<p>
	 *
	 * Some notes that will help future hackers:
	 * <ul>
	 * <li>
	 * We use a StringBuffer because there is no way to pre-allocate
	 * in the GapContent - and adding text each time to the GapContent
	 * would be slow because it would require array enlarging, etc.
	 * Better to do as few gap inserts as possible.
	 *
	 * <li>The StringBuffer is pre-allocated to Math.max(fileSize,
	 * IOBUFSIZE * 4) because when loading from URLs, fileSize is 0
	 * and we don't want to StringBuffer to enlarge 1000000 times for
	 * large URLs
	 *
	 * <li>We read the stream in IOBUFSIZE (= 32k) blocks, and loop over
	 * the read characters looking for line breaks.
	 * <ul>
	 * <li>a \r or \n causes a line to be added to the model, and appended
	 * to the string buffer
	 * <li>a \n immediately following an \r is ignored; so that Windows
	 * line endings are handled
	 * </ul>
	 *
	 * <li>This method remembers the line separator used in the file, and
	 * stores it in the lineSeparator buffer-local property. However,
	 * if the file contains, say, hello\rworld\n, lineSeparator will
	 * be set to \n, and the file will be saved as hello\nworld\n.
	 * Hence jEdit is not really appropriate for editing binary files.
	 *
	 * <li>To make reloading a bit easier, this method automatically
	 * removes all data from the model before inserting it. This
	 * shouldn't cause any problems, as most documents will be
	 * empty before being loaded into anyway.
	 *
	 * <li>If the last character read from the file is a line separator,
	 * it is not added to the model! There are two reasons:
	 * <ul>
	 * <li>On Unix, all text files have a line separator at the end,
	 * there is no point wasting an empty screen line on that
	 * <li>Because save() appends a line separator after *every* line,
	 * it prevents the blank line count at the end from growing
	 * </ul>
	 * 
	 * </ul>
	 */
	public void read(Buffer buffer, InputStream _in)
		throws IOException, BadLocationException
	{
		int bufLength;
		File file = buffer.getFile();
		if(file != null)
			bufLength = (int)file.length();
		else
			bufLength = IOBUFSIZE * 4;
		StringBuffer sbuf = new StringBuffer(bufLength);

		InputStreamReader in = new InputStreamReader(_in,
			jEdit.getProperty("buffer.encoding",
			System.getProperty("file.encoding")));
		char[] buf = new char[IOBUFSIZE];
		// Number of characters in 'buf' array.
		// InputStream.read() doesn't always fill the
		// array (eg, the file size is not a multiple of
		// IOBUFSIZE, or it is a GZipped file, etc)
		int len;

		// True if a \n was read after a \r. Usually
		// means this is a DOS/Windows file
		boolean CRLF = false;

		// A \r was read, hence a MacOS file
		boolean CROnly = false;

		// Was the previous read character a \r?
		// If we read a \n and this is true, we assume
		// we have a DOS/Windows file
		boolean lastWasCR = false;
		
		while((len = in.read(buf,0,buf.length)) != -1)
		{
			// Offset of previous line, relative to
			// the start of the I/O buffer (NOT
			// relative to the start of the document)
			int lastLine = 0;

			for(int i = 0; i < len; i++)
			{
				// Look for line endings.
				switch(buf[i])
				{
				case '\r':
					// If we read a \r and
					// lastWasCR is also true,
					// it is probably a Mac file
					// (\r\r in stream)
					if(lastWasCR)
					{
						CROnly = true;
						CRLF = false;
					}
					// Otherwise set a flag,
					// so that \n knows that last
					// was a \r
					else
					{
						lastWasCR = true;
					}

					// Insert a line
					sbuf.append(buf,lastLine,i -
						lastLine);
					sbuf.append('\n');

					// This is i+1 to take the
					// trailing \n into account
					lastLine = i + 1;
					break;
				case '\n':
					// If lastWasCR is true,
					// we just read a \r followed
					// by a \n. We specify that
					// this is a Windows file,
					// but take no further
					// action and just ignore
					// the \r.
					if(lastWasCR)
					{
						CROnly = false;
						CRLF = true;
						lastWasCR = false;
						// Bump lastLine so
						// that the next line
						// doesn't erronously
						// pick up the \r
						lastLine = i + 1;
					}
					// Otherwise, we found a \n
					// that follows some other
					// character, hence we have
					// a Unix file
					else
					{
						CROnly = false;
						CRLF = false;
						sbuf.append(buf,lastLine,
							i - lastLine);
						sbuf.append('\n');
						lastLine = i + 1;
					}
					break;
				default:
					// If we find some other
					// character that follows
					// a \r, so it is not a
					// Windows file, and probably
					// a Mac file
					if(lastWasCR)
					{
						CROnly = true;
						CRLF = false;
						lastWasCR = false;
					}
					break;
				}
			}
			// Add remaining stuff from buffer
			sbuf.append(buf,lastLine,len - lastLine);
		}

		// Can't abort the rest, otherwise the buffer may be
		// left in an inconsistent state
		setAbortable(false);

		if(CRLF)
			buffer.putProperty(Buffer.LINESEP,"\r\n");
		else if(CROnly)
			buffer.putProperty(Buffer.LINESEP,"\r");
		else
			buffer.putProperty(Buffer.LINESEP,"\n");
		in.close();

		// For `reload' command
		buffer.remove(0,buffer.getLength());

		// Chop trailing newline and/or ^Z (if any)
		int length = sbuf.length();
		if(length != 0)
		{
			char ch = sbuf.charAt(length - 1);
			if(length >= 2 && ch == 0x1a /* DOS ^Z */
				&& sbuf.charAt(length - 2) == '\n')
				sbuf.setLength(length - 2);
			else if(ch == '\n')
				sbuf.setLength(length - 1);
		}

		buffer.insertString(0,sbuf.toString(),null);

		// Process buffer-local properties
		Element map = buffer.getDefaultRootElement();
		for(int i = 0; i < Math.min(10,map.getElementCount()); i++)
		{
			Element line = map.getElement(i);
			String text = buffer.getText(line.getStartOffset(),
				line.getEndOffset() - line.getStartOffset() - 1);
			processProperty(buffer,text);
		}

		buffer.setNewFile(false);
	}
			
	private void processProperty(Buffer buffer, String prop)
	{
		StringBuffer buf = new StringBuffer();
		String name = null;
		boolean escape = false;
		for(int i = 0; i < prop.length(); i++)
		{
			char c = prop.charAt(i);
			switch(c)
			{
			case ':':
				if(escape)
				{
					escape = false;
					buf.append(':');
					break;
				}
				if(name != null)
				{
					String value = buf.toString();
					try
					{
						buffer.putProperty(name,new Integer(
							value));
					}
					catch(NumberFormatException nf)
					{
						buffer.putProperty(name,value);
					}
				}
				buf.setLength(0);
				break;
			case '=':
				if(escape)
				{
					escape = false;
					buf.append('=');
					break;
				}
				name = buf.toString();
				buf.setLength(0);
				break;
			case '\\':
				if(escape)
					buf.append('\\');
				escape = !escape;
				break;
			case 'n':
				if(escape)
				{	buf.append('\n');
					escape = false;
					break;
				}
			case 't':
				if(escape)
				{
					buf.append('\t');
					escape = false;
					break;
				}
			default:
				buf.append(c);
				break;
			}
		}
	}

	public void readMarkers(Buffer buffer, InputStream in)
		throws IOException
	{
		// For `reload' command
		buffer.removeAllMarkers();

		StringBuffer buf = new StringBuffer();
		int c;
		boolean eof = false;
		String name = null;
		int start = -1;
		int end = -1;
		for(;;)
		{
			if(eof)
				break;
			switch(c = in.read())
			{
			case -1:
				eof = true;
			case ';': case '\n': case '\r':
				if(buf.length() == 0)
					continue;
				String str = buf.toString();
				buf.setLength(0);
				if(name == null)
					name = str;
				else if(start == -1)
				{
					try
					{
						start = Integer.parseInt(str);
					}
					catch(NumberFormatException nf)
					{
						//Log.log(Log.ERROR,this,nf);
						start = 0;
					}
				}
				else if(end == -1)
				{
					try
					{
						end = Integer.parseInt(str);
					}
					catch(NumberFormatException nf)
					{
						//Log.log(Log.ERROR,this,nf);
						end = 0;
					}
					buffer.addMarker(name,start,end);
					name = null;
					start = -1;
					end = -1;
				}
				break;
			default:
				buf.append((char)c);
				break;
			}
		}
		in.close();
	}

	private void save()
	{
		OutputStream out = null;

		try
		{
			// the entire save operation can be aborted...
			setAbortable(true);

			try
			{
				out = vfs._createOutputStream(view,buffer,path);
				if(out == null)
					return;

				if(path.endsWith(".gz"))
					out = new GZIPOutputStream(out);

				write(buffer,out);

				// We only save markers to VFS's that support deletion.
				// Otherwise, we will accumilate stale marks files.
				if(vfs.canDelete() && buffer.getMarkerCount() != 0)
				{
					out = vfs._createOutputStream(view,buffer,markersPath);
					if(out != null)
						writeMarkers(buffer,out);
				}
				else
					vfs.delete(markersPath);
			}
			catch(BadLocationException bl)
			{
				Log.log(Log.ERROR,this,bl);
			}
			catch(IOException io)
			{
				Log.log(Log.ERROR,this,io);
				Object[] args = { io.toString() };
				VFSManager.error(view,"ioerror",args);
			}
		}
		catch(WorkThread.Abort a)
		{
			if(out != null)
			{
				try
				{
					out.close();
				}
				catch(IOException io)
				{
				}
			}
		}

		try
		{
			vfs._saveComplete(buffer);
		}
		catch(IOException io)
		{
		}
		catch(WorkThread.Abort a)
		{
		}
	}

	private void write(Buffer buffer, OutputStream _out)
		throws IOException, BadLocationException
	{
		BufferedWriter out = new BufferedWriter(
			new OutputStreamWriter(_out,
				jEdit.getProperty("buffer.encoding",
				System.getProperty("file.encoding"))),
				IOBUFSIZE);
		Segment lineSegment = new Segment();
		String newline = (String)buffer.getProperty(Buffer.LINESEP);
		if(newline == null)
			newline = System.getProperty("line.separator");
		Element map = buffer.getDefaultRootElement();
		for(int i = 0; i < map.getElementCount(); i++)
		{
			Element line = map.getElement(i);
			int start = line.getStartOffset();
			buffer.getText(start,line.getEndOffset() - start - 1,
				lineSegment);
			out.write(lineSegment.array,lineSegment.offset,
				lineSegment.count);
			out.write(newline);
		}
		out.close();

		File autosaveFile = buffer.getAutosaveFile();
		if(autosaveFile != null)
			autosaveFile.delete();
	}

	private void writeMarkers(Buffer buffer, OutputStream out)
		throws IOException
	{
		Writer o = new BufferedWriter(new OutputStreamWriter(out));
		Vector markers = buffer.getMarkers();
		for(int i = 0; i < markers.size(); i++)
		{
			Marker marker = (Marker)markers.elementAt(i);
			o.write(marker.getName());
			o.write(';');
			o.write(String.valueOf(marker.getStart()));
			o.write(';');
			o.write(String.valueOf(marker.getEnd()));
			o.write('\n');
		}
		o.close();
	}
}

/*
 * Change Log:
 * $Log$
 * Revision 1.10  2000/07/19 11:45:18  sp
 * I/O requests can be aborted now
 *
 * Revision 1.9  2000/05/23 04:04:52  sp
 * Marker highlight updates, next/prev-marker actions
 *
 * Revision 1.8  2000/05/14 10:55:22  sp
 * Tool bar editor started, improved view registers dialog box
 *
 * Revision 1.7  2000/05/09 10:51:52  sp
 * New status bar, a few other things
 *
 * Revision 1.6  2000/04/29 09:17:07  sp
 * VFS updates, various fixes
 *
 * Revision 1.5  2000/04/28 09:29:12  sp
 * Key binding handling improved, VFS updates, some other stuff
 *
 * Revision 1.4  2000/04/25 11:00:20  sp
 * FTP VFS hacking, some other stuff
 *
 * Revision 1.3  2000/04/25 03:32:40  sp
 * Even more VFS hacking
 *
 * Revision 1.2  2000/04/24 11:00:23  sp
 * More VFS hacking
 *
 * Revision 1.1  2000/04/24 04:45:37  sp
 * New I/O system started, and a few minor updates
 *
 */
