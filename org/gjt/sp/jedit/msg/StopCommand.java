/*
 * StopCommand.java - Stop command message
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
 * Message to stop an executing command. Upon receiving this message, plugins
 * should check if the specified tag is for a command they're executing, and
 * stop it if necessary,
 * @author Slava Pestov
 * @version $Id$
 *
 * @since jEdit 2.2pre6
 */
public class StopCommand extends EBMessage
{
	/**
	 * Creates a new stop command message.
	 * @param source The message source
	 * @param tag Value of getTag() method of ExecuteCommand message
	 */
	public StopCommand(EBComponent source, Object tag)
	{
		super(source);
		if(tag == null)
			throw new NullPointerException("Tag must be non-null");

		this.tag = tag;
	}

	/**
	 * Returns the process tag. This should be obtained from
	 * the ExecuteCommand message.
	 */
	public Object getTag()
	{
		return tag;
	}

	/**
	 * Returns a string representation of this message's parameters.
	 */
	public String paramString()
	{
		return super.paramString() + ",tag=" + tag;
	}

	// private members
	private Object tag;
}
