/*
 * ErrorSourceUpdate.java - Message that an error source has changed
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

package org.gjt.sp.jedit.msg;

import org.gjt.sp.jedit.*;

/**
 * Message that an error source has changed.
 * @author Slava Pestov
 * @version $Id$
 *
 * @since jEdit 2.2pre6
 */
public class ErrorSourceUpdate extends EBMessage
{
	/**
	 * An error has been added.
	 */
	public static final Object ERROR_ADDED = "ERROR_ADDED";

	/**
	 * An error has been removed.
	 */
	public static final Object ERROR_REMOVED = "ERROR_REMOVED";

	/**
	 * All errors have been removed from this source.
	 */
	public static final Object ERRORS_CLEARED = "ERRORS_CLEARED";

	/**
	 * Creates a new error source update message.
	 * @param source The message source
	 * @param what What changed
	 * @param errorSource The error source
	 * @param error The error. Null if what is ERRORS_CLEARED
	 */
	public ErrorSourceUpdate(EBComponent source, Object what,
		ErrorSource errorSource, ErrorSource.Error error)
	{
		super(source);
		if(what == null || errorSource == null)
			throw new NullPointerException("What and error source must be non-null");

		this.what = what;
		this.errorSource = errorSource;
		this.error = error;
	}

	/**
	 * Returns what changed.
	 */
	public Object getWhat()
	{
		return what;
	}

	/**
	 * Returns the error source.
	 */
	public ErrorSource getErrorSource()
	{
		return errorSource;
	}

	/**
	 * Returns the error involved. Null if what is ERRORS_CLEARED.
	 */
	public ErrorSource.Error getError()
	{
		return error;
	}

	public String paramString()
	{
		return super.paramString() + ",what=" + what
			+ ",errorSource=" + errorSource
			+ ",error=" + error;
	}

	// private members
	private Object what;
	private ErrorSource errorSource;
	private ErrorSource.Error error;
}
