/*
 * ErrorSource.java - An error source
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

/**
 * Error source interface.
 * @author Slava Pestov
 * @version $Id$
 *
 * @since jEdit 2.2pre6
 */
public interface ErrorSource
{
	/**
	 * Returns a string description of this error source.
	 */
	public String getName();

	/**
	 * Returns the number of errors in this source.
	 */
	public int getErrorCount();

	/**
	 * Returns all errors.
	 */
	public Error[] getAllErrors();

	/**
	 * Returns the number of errors in a specified file.
	 * @param path Full path name
	 */
	public int getFileErrorCount(String path);

	/**
	 * Returns all errors in the specified file.
	 * @param path Full path name
	 */
	public Error[] getFileErrors(String path);

	/**
	 * Returns all errors on the specified line.
	 * @param lineIndex The line number
	 */
	public Error[] getLineErrors(Buffer buffer, int lineIndex);

	/**
	 * Removes all errors from this error source (if supported).
	 */
	public void clear();

	/**
	 * An error.
	 */
	public static interface Error
	{
		/**
		 * Returns the source of this error.
		 */
		public ErrorSource getErrorSource();

		/**
		 * Returns the buffer involved, or null if it is not open.
		 */
		public Buffer getBuffer();

		/**
		 * Returns the file path name involved.
		 */
		public String getFilePath();

		/**
		 * Returns just the name portion of the file involved.
		 */
		public String getFileName();

		/**
		 * Returns the line number.
		 */
		public int getLineNumber();

		/**
		 * Returns the start offset.
		 */
		public int getStartOffset();

		/**
		 * Returns the end offset.
		 */
		public int getEndOffset();

		/**
		 * Returns the error message.
		 */
		public String getErrorMessage();
	}
}
