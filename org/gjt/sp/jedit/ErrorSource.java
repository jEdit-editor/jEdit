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
 * An error source. Error sources generate errors which other plugins can
 * present in some fashion, for example the ErrorList plugin displays
 * an error list.
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
	String getName();

	/**
	 * Returns the number of errors in this source.
	 */
	int getErrorCount();

	/**
	 * Returns an array of all errors in this error source.
	 */
	Error[] getAllErrors();

	/**
	 * Returns the number of errors in the specified file.
	 * @param path Full path name
	 */
	int getFileErrorCount(String path);

	/**
	 * Returns all errors in the specified file.
	 * @param path Full path name
	 */
	Error[] getFileErrors(String path);

	/**
	 * Returns all errors on the specified line.
	 * @param lineIndex The line number
	 */
	Error[] getLineErrors(Buffer buffer, int lineIndex);

	/**
	 * An error.
	 */
	public interface Error
	{
		/**
		 * Returns the source of this error.
		 */
		ErrorSource getErrorSource();

		/**
		 * Returns the buffer involved, or null if it is not open.
		 */
		Buffer getBuffer();

		/**
		 * Returns the file path name involved.
		 */
		String getFilePath();

		/**
		 * Returns just the name portion of the file involved.
		 */
		String getFileName();

		/**
		 * Returns the line number.
		 */
		int getLineNumber();

		/**
		 * Returns the start offset.
		 */
		int getStartOffset();

		/**
		 * Returns the end offset.
		 */
		int getEndOffset();

		/**
		 * Returns the error message.
		 */
		String getErrorMessage();
	}
}
