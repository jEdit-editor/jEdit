/*
 * GetOutput.java - Get output message
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
 * Message to obtain a named output. The <code>GetOutputs</code> message
 * can be used to obtain a list.
 * @author Slava Pestov
 * @version $Id$
 *
 * @since jEdit 2.2pre6
 */
public class GetOutput extends EBMessage
{
	/**
	 * Creates a new get output message.
	 * @param source The message source
	 * @param name The output name
	 * @param view The view
	 */
	public GetOutput(EBComponent source, String name, View view)
	{
		super(source);
		if(name == null)
			throw new NullPointerException("Name must be non-null");

		this.name = name;
		this.view = view;
	}

	/**
	 * Returns the requested output name.
	 */
	public String getOutputName()
	{
		return name;
	}

	/**
	 * Returns the view.
	 */
	public View getView()
	{
		return view;
	}

	/**
	 * Sets the output.
	 */
	public void setOutput(Output output)
	{
		this.output = output;
		veto();
	}

	/**
	 * Returns the output, or null if nobody responded to the message.
	 */
	public Output getOutput()
	{
		return output;
	}

	public String paramString()
	{
		return super.paramString() + ",name=" + name;
	}

	// private members
	private String name;
	private View view;
	private Output output;
}
