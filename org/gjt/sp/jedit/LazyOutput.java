/*
 * LazyOutput.java - Lazy output
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

/**
 * Creates a real output only when some text is printed.
 * @author Slava Pestov
 * @version $Id$
 *
 * @since jEdit 2.2pre6
 */
public class LazyOutput implements Output
{
	/**
	 * Creates a new lazy output.
	 * @param name The name of the underlying output.
	 */
	public LazyOutput(View view, String outputName)
	{
		this.view = view;
		this.outputName = outputName;
	}

	/**
	 * Appends some output.
	 * @param color The color to display it in (null will use
	 * the default)
	 * @param text The text
	 */
	public void addOutput(Color color, String text)
	{
		ensureOutputExists();
		output.addOutput(color,text);
	}

	/**
	 * This method must be called when the command
	 * finishes executing.
	 */
	public void commandDone()
	{
		if(output == null)
			return;
		output.commandDone();
	}

	/**
	 * Returns the default color for informational messages.
	 */
	public Color getInfoColor()
	{
		ensureOutputExists();
		return output.getInfoColor();
	}

	/**
	 * Returns the default color for error messages.
	 */
	public Color getErrorColor()
	{
		ensureOutputExists();
		return output.getErrorColor();
	}

	// private members
	private View view;
	private String outputName;
	private Output output;

	private void ensureOutputExists()
	{
		if(output == null)
		{
			GetOutput msg = new GetOutput(null,outputName,view);
			EditBus.send(msg);
			output = msg.getOutput();
			if(output == null)
				output = new LogOutput();
		}
	}
}
