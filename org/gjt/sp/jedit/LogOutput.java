/*
 * LogOutput.java - Log output
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

import java.awt.Color;
import org.gjt.sp.jedit.msg.GetOutput;
import org.gjt.sp.util.Log;

/**
 * Outputs to the jEdit activity log. This class is not public and should
 * not be created directly; use GetOutput(Log) instead
 * @author Slava Pestov
 * @version $Id$
 *
 * @since jEdit 2.2pre6
 */
/*public*/ class LogOutput implements Output
{
	/**
	 * Appends some output.
	 * @param color The color to display it in. This is ignored
	 * @param text The text
	 */
	public void addOutput(Color color, String text)
	{
		Log.log(Log.MESSAGE,this,text);
	}

	/**
	 * This method must be called when the command
	 * finishes executing. This is ignored.
	 */
	public void commandDone()
	{}

	/**
	 * Returns the default color for informational messages.
	 */
	public Color getInfoColor()
	{
		return null;
	}

	/**
	 * Returns the default color for error messages.
	 */
	public Color getErrorColor()
	{
		return null;
	}
}
