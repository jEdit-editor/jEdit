/*
 * DefaultErrorSource.java - Default error source
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

import javax.swing.text.Element;
import javax.swing.text.Position;
import java.io.File;
import java.util.*;
import org.gjt.sp.jedit.msg.*;

/**
 * An abstract error source that uses positions to keep track of offsets.
 * @author Slava Pestov
 * @version $Id$
 *
 * @since jEdit 2.2pre6
 */
public class DefaultErrorSource implements ErrorSource, EBComponent
{
	/**
	 * Creates a new default error source.
	 */
	public DefaultErrorSource(String name)
	{
		errors = new Hashtable();
		this.name = name;
	}

	/**
	 * Returns a string description of this error source.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Returns the number of errors in this source.
	 */
	public int getErrorCount()
	{
		return errorCount;
	}

	/**
	 * Returns all errors.
	 */
	public ErrorSource.Error[] getAllErrors()
	{
		if(errors.size() == 0)
			return null;

		Vector errorList = new Vector(errorCount);

		Enumeration enum = errors.elements();
		while(enum.hasMoreElements())
		{
			ErrorList list = (ErrorList)enum.nextElement();

			DefaultError error = list.first;
			while(error != null)
			{
				errorList.addElement(error);
				error = error.next;
			}
		}

		ErrorSource.Error[] array = new ErrorSource.Error[errorList.size()];
		errorList.copyInto(array);
		return array;
	}

	/**
	 * Returns the number of errors in the specified file.
	 * @param path The full path name
	 */
	public int getFileErrorCount(String path)
	{
		ErrorList list = (ErrorList)errors.get(path);
		if(list == null)
			return 0;

		int count = 0;
		DefaultError error = list.first;
		while(error != null)
		{
			count++;
			error = error.next;
		}

		return count;
	}

	/**
	 * Returns all errors in the specified file.
	 * @param path The full path name
	 */
	public ErrorSource.Error[] getFileErrors(String path)
	{
		ErrorList list = (ErrorList)errors.get(path);
		if(list == null)
			return null;

		Vector vector = null;
		DefaultError error = list.first;

		while(error != null)
		{
			if(vector == null)
				vector = new Vector();
			vector.addElement(error);
			error = error.next;
		}

		if(vector == null)
			return null;

		ErrorSource.Error[] array = new ErrorSource.Error[vector.size()];
		vector.copyInto(array);
		return array;
	}

	/**
	 * Returns all errors on the specified line.
	 * @param lineIndex The line number
	 */
	public ErrorSource.Error[] getLineErrors(Buffer buffer, int lineIndex)
	{
		if(errors.size() == 0)
			return null;

		ErrorList list = (ErrorList)errors.get(buffer.getPath());
		if(list == null)
			return null;

		Vector errorList = null;

		DefaultError error = list.first;
		while(error != null)
		{
			if(error.getLineNumber() == lineIndex)
			{
				if(errorList == null)
					errorList = new Vector();
				errorList.addElement(error);
			}
			error = error.next;
		}

		if(errorList == null)
			return null;

		ErrorSource.Error[] array = new ErrorSource.Error[errorList.size()];
		errorList.copyInto(array);
		return array;
	}

	/**
	 * Removes all errors from this error source.
	 */
	public void clear()
	{
		errors.clear();
		errorCount = 0;

		ErrorSourceUpdate message = new ErrorSourceUpdate(this,
			ErrorSourceUpdate.ERRORS_CLEARED,this,null);
		EditBus.send(message);
	}

	/**
	 * Adds an error to this error source. This method is thread-safe.
	 * @param path The path name
	 * @param lineIndex The line number
	 * @param start The start offset
	 * @param end The end offset
	 * @param error The error message
	 */
	public synchronized void addError(String path, int lineIndex,
		int start, int end, String error)
	{
		DefaultError newError = new DefaultError(this,path,lineIndex,
			start,end,error);

		ErrorList list = (ErrorList)errors.get(newError.getFilePath());
		if(list == null)
		{
			list = new ErrorList();
			errors.put(newError.getFilePath(),list);
		}

		if(list.first == null)
		{
			list.first = list.last = newError;
		}
		else
		{
			list.last.next = newError;
			list.last = newError;
		}

		errorCount++;

		ErrorSourceUpdate message = new ErrorSourceUpdate(this,
			ErrorSourceUpdate.ERROR_ADDED,this,newError);
		EditBus.send(message);
	}

	public void handleMessage(EBMessage message)
	{
		if(message instanceof BufferUpdate)
			handleBufferMessage((BufferUpdate)message);
	}

	public String toString()
	{
		return getClass().getName() + "[" + name + "]";
	}

	// protected members
	protected String name;
	protected int errorCount;
	protected Hashtable errors;

	static class ErrorList
	{
		DefaultError first;
		DefaultError last;
	}

	// private members
	private void handleBufferMessage(BufferUpdate message)
	{
		Buffer buffer = message.getBuffer();

		if(message.getWhat() == BufferUpdate.CREATED)
		{
			ErrorList list = (ErrorList)errors.get(buffer.getPath());
			if(list != null)
			{
				DefaultError error = list.first;
				while(error != null)
				{
					error.openNotify(buffer);
					error = error.next;
				}
			}
		}
		else if(message.getWhat() == BufferUpdate.CLOSED)
		{
			ErrorList list = (ErrorList)errors.get(buffer.getPath());
			if(list != null)
			{
				DefaultError error = list.first;
				while(error != null)
				{
					error.closeNotify(buffer);
					error = error.next;
				}
			}
		}
	}

	/**
	 * An error.
	 */
	public static class DefaultError implements ErrorSource.Error
	{
		/**
		 * Creates a new default error.
		 * @param path The path
		 * @param start The start offset
		 * @param end The end offset
		 * @param error The error message
		 */
		public DefaultError(ErrorSource source, String path,
			int lineIndex, int start, int end, String error)
		{
			this.source = source;

			// Create absolute path
			this.path = MiscUtilities.constructPath(System
				.getProperty("user.dir"),path);

			this.lineIndex = lineIndex;
			this.start = start;
			this.end = end;
			this.error = error;

			// Shortened name used in display
			name = new File(this.path).getName();

			// If the buffer is open, this creates a floating position
			Buffer buffer = jEdit.getBuffer(this.path);
			if(buffer != null)
				openNotify(buffer);
		}

		/**
		 * Returns the error source.
		 */
		public ErrorSource getErrorSource()
		{
			return source;
		}

		/**
		 * Returns the buffer involved, or null if it is not open.
		 */
		public Buffer getBuffer()
		{
			return buffer;
		}

		/**
		 * Returns the file name involved.
		 */
		public String getFilePath()
		{
			return path;
		}

		/**
		 * Returns the name portion of the file involved.
		 */
		public String getFileName()
		{
			return name;
		}

		/**
		 * Returns the line number.
		 */
		public int getLineNumber()
		{
			if(startPos != null)
			{
				return buffer.getDefaultRootElement().getElementIndex(
					startPos.getOffset());
			}
			else
				return lineIndex;
		}

		/**
		 * Returns the start offset.
		 */
		public int getStartOffset()
		{
			if(startPos != null)
			{
				return startPos.getOffset()
					- buffer.getDefaultRootElement()
					.getElement(getLineNumber())
					.getStartOffset();
			}
			else
				return start;
		}

		/**
		 * Returns the end offset.
		 */
		public int getEndOffset()
		{
			if(endPos != null)
			{
				return endPos.getOffset()
					- buffer.getDefaultRootElement()
					.getElement(getLineNumber())
					.getStartOffset();
			}
			else
				return end;
		}

		/**
		 * Returns the error message.
		 */
		public String getErrorMessage()
		{
			return error;
		}

		/**
		 * Returns a string representation of this error.
		 */
		public String toString()
		{
			return "[" + getErrorSource().getName()
				+ "] " + getFileName() + ":" +
				getLineNumber() + ":" +
				getErrorMessage();
		}
			
		// package-private members
		DefaultError next;

		/*
		 * Notifies the compiler error that a buffer has been opened.
		 * This creates the floating position if necessary.
		 *
		 * I could make every CompilerError listen for buffer open
		 * events, but it's too much effort remembering to unregister
		 * the listeners, etc.
		 */
		void openNotify(Buffer buffer)
		{
			this.buffer = buffer;
			Element map = buffer.getDefaultRootElement();
			try
			{
				Element lineElement = map.getElement(lineIndex);
				if(lineElement != null)
				{
					linePos = buffer.createPosition(lineElement
						.getStartOffset());
				}

				if(start != 0)
					startPos = buffer.createPosition(lineElement.getStartOffset() + start);
				if(end != 0)
					endPos = buffer.createPosition(lineElement.getStartOffset() + end);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}

		/*
		 * Notifies the compiler error that a buffer has been closed.
		 * This clears the floating position if necessary.
		 *
		 * I could make every CompilerError listen for buffer closed
		 * events, but it's too much effort remembering to unregister
		 * the listeners, etc.
		 */

		void closeNotify(Buffer buffer)
		{
			this.buffer = null;
			linePos = null;
			startPos = null;
			endPos = null;
		}

		// private members
		private ErrorSource source;

		private String path;
		private String name;
		private Buffer buffer;

		private int lineIndex;
		private int start;
		private int end;

		private Position linePos;
		private Position startPos;
		private Position endPos;

		private String error;
	}
}
