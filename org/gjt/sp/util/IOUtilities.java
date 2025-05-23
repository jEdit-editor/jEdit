/*
 * IOUtilities.java - IO related functions
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2006 Matthieu Casanova
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

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;

/**
 * IO tools that depend on JDK only.
 *
 * @author Matthieu Casanova
 * @version $Id$
 * @since 4.3pre5
 */
public class IOUtilities
{
	//{{{ moveFile() method
	/**
	 * Moves the source file to the destination.
	 *
	 * If the destination cannot be created or is a read-only file, the
	 * method returns <code>false</code>. Otherwise, the contents of the
	 * source are copied to the destination, the source is deleted,
	 * and <code>true</code> is returned.
	 *
	 * @param source The source file to move.
	 * @param dest   The destination where to move the file.
	 * @return true on success, false otherwise.
	 *
	 * @since jEdit 4.3pre9
	 */
	public static boolean moveFile(File source, File dest)
	{
		boolean ok = false;

		if ((dest.exists() && dest.canWrite())
			|| (!dest.exists() && dest.getParentFile().canWrite()))
			{
				OutputStream fos = null;
				InputStream fis = null;
				try
				{
					fos = new FileOutputStream(dest);
					fis = new FileInputStream(source);
					ok = copyStream(32768,null,fis,fos,false);
				}
				catch (IOException ioe)
				{
					Log.log(Log.WARNING, IOUtilities.class,
							"Error moving file: " + ioe + " : " + ioe.getMessage());
				}
				finally
				{
					closeQuietly(fos);
					closeQuietly(fis);
				}

				if(ok)
					source.delete();
			}
		return ok;
	} //}}}

	//{{{ copyStream() methods
	/**
	 * Copy an input stream to an output stream.
	 *
	 * @param bufferSize the size of the buffer
	 * @param progress the progress observer it could be null
	 * @param progressPrefix the progress prefix, it could be null
	 * @param in the input stream
	 * @param out the output stream
	 * @param canStop if true, the copy can be stopped by interrupting the thread
	 * @return <code>true</code> if the copy was done, <code>false</code> if it was interrupted
	 * @throws IOException  IOException If an I/O error occurs
	 */
	public static boolean copyStream(int bufferSize, @Nullable ProgressObserver progress,
					 String progressPrefix, InputStream in, OutputStream out,
					 boolean canStop)
		throws IOException
	{
		byte[] buffer = new byte[bufferSize];
		int n;
		long copied = 0L;
		while (-1 != (n = in.read(buffer)))
		{
			out.write(buffer, 0, n);
			copied += n;
			if(progress != null)
			{
				String progressMessage = StandardUtilities.formatFileSize(copied);
				if (progressPrefix != null)
				{
					progressMessage = String.format("%s (%s)", progressPrefix, progressMessage);
				}
				progress.setStatus(progressMessage);
				progress.setValue(copied);
			}
			if(canStop && Thread.interrupted())
				return false;
		}
		return true;
	}

	/**
	 * Copy an input stream to an output stream.
	 *
	 * @param bufferSize the size of the buffer
	 * @param progress the progress observer it could be null
	 * @param in the input stream
	 * @param out the output stream
	 * @param canStop if true, the copy can be stopped by interrupting the thread
	 * @return <code>true</code> if the copy was done, <code>false</code> if it was interrupted
	 * @throws IOException  IOException If an I/O error occurs
	 */
	public static boolean copyStream(int bufferSize, @Nullable ProgressObserver progress,
					InputStream in, OutputStream out, boolean canStop)
		throws IOException
	{
		return copyStream(bufferSize, progress, null, in, out, canStop);
	}

	/**
	 * Copy an input stream to an output stream with a buffer of 4096 bytes.
	 *
	 * @param progress the progress observer it could be null
	 * @param in the input stream
	 * @param out the output stream
	 * @param canStop if true, the copy can be stopped by interrupting the thread
	 * @return <code>true</code> if the copy was done, <code>false</code> if it was interrupted
	 * @throws IOException  IOException If an I/O error occurs
	 */
	public static boolean copyStream(@Nullable ProgressObserver progress,
					 InputStream in, OutputStream out, boolean canStop)
		throws IOException
	{
		return copyStream(4096, progress, null, in, out, canStop);
	}

	/**
	 * Copy an input stream to an output stream with a buffer of 4096 bytes.
	 *
	 * @param progress the progress observer it could be null
	 * @param progressPrefix the progress prefix, it could be null
	 * @param in the input stream
	 * @param out the output stream
	 * @param canStop if true, the copy can be stopped by interrupting the thread
	 * @return <code>true</code> if the copy was done, <code>false</code> if it was interrupted
	 * @throws IOException  IOException If an I/O error occurs
	 */
	public static boolean copyStream(@Nullable ProgressObserver progress, String progressPrefix,
					 InputStream in, OutputStream out, boolean canStop)
		throws IOException
	{
		return copyStream(4096, progress, progressPrefix, in, out, canStop);
	} //}}}

	//{{{ toByteArray() method
	/**
	 * Convert an InputStream into a byte array
	 *
	 * @param in the input stream
	 * @throws IOException  IOException If an I/O error occurs
	 * @since jEdit 5.6pre1
	 */
	public static byte[] toByteArray(InputStream in) throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream(500000);
		copyStream(500000, null, null, in, out, false);
		return out.toByteArray();
	} //}}}

	//{{{ toString() methods
	/**
	 * Convert an InputStream into a String with UTF-8 encoding
	 *
	 * @param in the input stream
	 * @throws IOException  IOException If an I/O error occurs
	 * @since jEdit 5.6pre1
	 */
	public static String toString(InputStream in) throws IOException
	{
		return toString(in, StandardCharsets.UTF_8);
	} //}}}

	//{{{ toString() methods
	/**
	 * Convert an InputStream into a String
	 *
	 * @param in the input stream
	 * @param charset the choosend charset
	 * @throws IOException  IOException If an I/O error occurs
	 * @since jEdit 5.6pre1
	 */
	public static String toString(InputStream in, Charset charset) throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream(500000);
		copyStream(500000, null, null, in, out, false);
		return out.toString(charset);
	} //}}}

	//{{{ fileLength() method
	/**
	 * Returns the length of a file. If it is a directory it will calculate recursively the length.
	 *
	 * @param file the file or directory
	 * @return the length of the file or directory. If the file doesn't exist it will return 0
	 * @since 4.3pre10
	 */
	public static long fileLength(File file)
	{
		long length = 0L;
		if (file.isFile())
			length = file.length();
		else if (file.isDirectory())
		{
			File[] files = file.listFiles();
			if (files != null)
			{
				for (File f : files)
					length += fileLength(f);
			}
		}
		return length;
	} // }}}

	//{{{ closeQuietly() methods
	/**
	 * Method that will close an {@link AutoCloseable} ignoring it if it is null and ignoring exceptions.
	 *
	 * @param closeable the closeable to close.
	 * @since jEdit 5.8pre1
	 */
	public static void closeQuietly(@Nullable AutoCloseable closeable)
	{
		if(closeable != null)
		{
			try
			{
				if (closeable instanceof Flushable)
				{
					((Flushable)closeable).flush();
				}
			}
			catch (IOException e)
			{
				// ignore
			}
			try
			{
				closeable.close();
			}
			catch (Exception e)
			{
				//ignore
			}
		}
	}

	/**
	 * Method that will close a {@link Closeable} ignoring it if it is null and ignoring exceptions.
	 *
	 * @param closeable the closeable to close.
	 * @since jEdit 4.3pre8
	 * @deprecated As of jEdit 5.8pre1, replaced by {@link #closeQuietly(AutoCloseable)}
	 */
	@Deprecated
	public static void closeQuietly(@Nullable Closeable closeable)
	{
		closeQuietly((AutoCloseable)closeable);
	}

	/**
	 * Method that will close an {@link ObjectInput} ignoring it if it is null and ignoring exceptions.
	 *
	 * @param in the closeable to close.
	 * @since jEdit 5.1pre1
	 * @deprecated As of jEdit 5.8pre1, replaced by {@link #closeQuietly(AutoCloseable)}
	 */
	@Deprecated
	public void closeQuietly(@Nullable ObjectInput in)
	{
		closeQuietly((AutoCloseable)in);
	}

	/**
	 * Method that will close an {@link ObjectOutput} ignoring it if it is null and ignoring exceptions.
	 * @param out the closeable to close.
	 * @since jEdit 5.1pre1
	 * @deprecated As of jEdit 5.8pre1, replaced by {@link #closeQuietly(AutoCloseable)}
	 */
	@Deprecated
	public void closeQuietly(@Nullable ObjectOutput out)
	{
		closeQuietly((AutoCloseable)out);
	} //}}}

	//{{{ IOUtilities() constructor
	private IOUtilities()
	{
	} //}}}
}
