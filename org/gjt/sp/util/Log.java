/*
 * Log.java - A class for logging events
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

package org.gjt.sp.util;

import javax.swing.text.*;
import java.awt.Color;
import java.io.*;
import java.util.StringTokenizer;

/**
 * This class provides methods for logging events. It has the same
 * purpose as System.out.println() and such, but more powerful.
 * All events are logged to a Swing document, and those with a
 * high urgency (warnings and errors) are also printed to the
 * standard error stream. This class can also optionally redirect
 * standard output and error to the log.
 * @author Slava Pestov
 * @version $Id$
 */
public class Log
{
	/**
	 * Debugging message urgency. Should be used for messages only
	 * useful when debugging a problem.
	 * @since jEdit 2.2pre2
	 */
	public static final int DEBUG = 1;

	/**
	 * Message urgency. Should be used for messages which give more
	 * detail than notices.
	 * @since jEdit 2.2pre2
	 */
	public static final int MESSAGE = 3;

	/**
	 * Notice urgency. Should be used for messages that directly
	 * affect the user.
	 * @since jEdit 2.2pre2
	 */
	public static final int NOTICE = 5;

	/**
	 * Warning urgency. Should be used for messages that warrant
	 * attention.
	 * @since jEdit 2.2pre2
	 */
	public static final int WARNING = 7;

	/**
	 * Error urgency. Should be used for messages that signal a
	 * failure.
	 * @since jEdit 2.2pre2
	 */
	public static final int ERROR = 9;

	/**
	 * Returns the document where messages are logged. The document
	 * of a Swing text area can be set to this to graphically view
	 * log messages.
	 * @since jEdit 2.2pre2
	 */
	public static Document getLogDocument()
	{
		return logDocument;
	}

	/**
	 * Redirects standard output and error to the log. This can
	 * only be called once.
	 * @since jEdit 2.2pre2
	 */
	public static void redirectStdio()
	{
		if(System.out == realOut && System.err == realErr)
		{
			System.setOut(createPrintStream(NOTICE,null));
			System.setErr(createPrintStream(ERROR,null));
		}
	}

	/**
	 * Saves the log to a file.
	 * @param path The file
	 * @since jEdit 2.2pre2
	 */
	public static void saveLog(String path)
		throws IOException
	{
		saveLog(new FileWriter(path));
	}

	/**
	 * Saves the log to the specified writer.
	 * @param out The writer
	 * @since jEdit 2.2pre2
	 */
	public static void saveLog(Writer out)
		throws IOException
	{
		if(!(out instanceof BufferedWriter))
		{
			saveLog(new BufferedWriter(out));
			return;
		}

		String lineSep = System.getProperty("line.separator");

		Element map = logDocument.getDefaultRootElement();
		for(int i = 0; i < map.getElementCount(); i++)
		{
			Element lineElement = map.getElement(i);
			try
			{
				String text = logDocument.getText(
					lineElement.getStartOffset(),
					lineElement.getEndOffset()
					- lineElement.getStartOffset() - 1);
				out.write(text);
				out.write(lineSep);
			}
			catch(BadLocationException bl)
			{
				log(ERROR,Log.class,bl);
			}
		}

		out.close();
	}

	/**
	 * Clears the log.
	 */
	public static void clearLog()
	{
		try
		{
			logDocument.remove(0,logDocument.getLength());

			log(NOTICE,Log.class,"To copy from the activity log, select"
				+ " the appropriate text and press C+c");

			// Log some stuff
			log(NOTICE,Log.class,"When reporting bugs, please"
				+ " include the following information:");
			String[] props = {
				"java.version", "java.vendor",
				"java.vendor.url",
				"java.compiler", "os.name", "os.version",
				"os.arch", "user.home", "user.dir"
				};
			for(int i = 0; i < props.length; i++)
			{
				log(NOTICE,Log.class,
					props[i] + "=" + System.getProperty(props[i]));
			}
		}
		catch(BadLocationException bl)
		{
			log(ERROR,Log.class,bl);
		}
	}

	/**
	 * Logs a message. This method is threadsafe.
	 * @param urgency The urgency
	 * @param source The object that logged this message.
	 * @param message The message. This can either be a string or
	 * an exception
	 *
	 * @since jEdit 2.2pre2
	 */
	public static void log(int urgency, Object source, Object message)
	{
		String _source;
		if(source == null)
		{
			_source = Thread.currentThread().getName();
			if(_source == null)
			{
				_source = Thread.currentThread().getClass().getName();
			}
		}
		else if(source instanceof Class)
			_source = ((Class)source).getName();
		else
			_source = source.getClass().getName();
		int index = _source.lastIndexOf('.');
		if(index != -1)
			_source = _source.substring(index+1);

		if(message instanceof Throwable)
		{
			_logException(urgency,source,(Throwable)message);
		}
		else
		{
			String _message = String.valueOf(message);
			// If multiple threads log stuff, we don't want
			// the output to get mixed up
			synchronized(LOCK)
			{
				StringTokenizer st = new StringTokenizer(
					_message,"\r\n");
				while(st.hasMoreTokens())
				{
					_log(urgency,_source,st.nextToken());
				}
			}
		}
	}

	// private members
	private static Object LOCK = new Object();
	private static Document logDocument;
	private static PrintStream realOut;
	private static PrintStream realErr;

	static
	{
		realOut = System.out;
		realErr = System.err;

		logDocument = new PlainDocument();
		clearLog();
	}

	private static PrintStream createPrintStream(final int urgency,
		final Object source)
	{
		return new PrintStream(new OutputStream() {
			public void write(int b)
			{
				byte[] barray = { (byte)b };
				write(barray,0,1);
			}

			public void write(byte[] b, int off, int len)
			{
				String str = new String(b,off,len);
				log(urgency,source,str);
			}
		});
	}

	private static void _logException(final int urgency,
		final Object source,
		final Throwable message)
	{
		PrintStream out = createPrintStream(urgency,source);

		synchronized(LOCK)
		{
			message.printStackTrace(out);
		}
	}

	private static void _log(int urgency, String source,
		String message)
	{
		message = source + ": " + message + '\n';
		_log(urgency,message);

		if(urgency >= WARNING)
			realErr.print(message);
	}

	private static void _log(int urgency, String message)
	{
		try
		{
			logDocument.insertString(logDocument.getLength(),
				"[" + urgencyToString(urgency) + "] " + message,
				null);
		}
		catch(BadLocationException bl)
		{
			bl.printStackTrace();
		}
	}

	private static String urgencyToString(int urgency)
	{
		switch(urgency)
		{
		case DEBUG:
			return "debug";
		case MESSAGE:
			return "message";
		case NOTICE:
			return "notice";
		case WARNING:
			return "warning";
		case ERROR:
			return "error";
		}

		throw new IllegalArgumentException("Invalid urgency: " + urgency);
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.7  2000/01/14 04:23:50  sp
 * 2.3pre2 stuff
 *
 * Revision 1.6  1999/12/13 03:40:30  sp
 * Bug fixes, syntax is now mostly GPL'd
 *
 * Revision 1.5  1999/11/21 07:59:30  sp
 * JavaDoc updates
 *
 * Revision 1.4  1999/11/21 01:20:31  sp
 * Bug fixes, EditBus updates, fixed some warnings generated by jikes +P
 *
 * Revision 1.3  1999/11/09 10:14:34  sp
 * Macro code cleanups, menu item and tool bar clicks are recorded now, delete
 * word commands, check box menu item support
 *
 * Revision 1.2  1999/11/06 02:06:50  sp
 * Logging updates, bug fixing, icons, various other stuff
 *
 * Revision 1.1  1999/10/31 07:15:34  sp
 * New logging API, splash screen updates, bug fixes
 *
 */
