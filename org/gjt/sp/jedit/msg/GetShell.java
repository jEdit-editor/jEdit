/*
 * GetShell.java - Get shell message
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
 * Message to obtain a named shell.
 * @author Slava Pestov
 * @version $Id$
 *
 * @since jEdit 2.2pre6
 */
public class GetShell extends EBMessage
{
	/**
	 * Creates a new get shell message.
	 * @param source The message source
	 * @param name The shell name
	 * @param view The view
	 */
	public GetShell(EBComponent source, String name, View view)
	{
		super(source);
		if(name == null)
			throw new NullPointerException("Name must be non-null");

		this.name = name;
		this.view = view;
	}

	/**
	 * Returns the requested shell name.
	 */
	public String getShellName()
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
	 * Sets the shell.
	 */
	public void setShell(Shell shell)
	{
		this.shell = shell;
		veto();
	}

	/**
	 * Returns the shell, or null if nobody responded to the message.
	 */
	public Shell getShell()
	{
		return shell;
	}

	public String paramString()
	{
		return super.paramString() + ",name=" + name;
	}

	// private members
	private String name;
	private View view;
	private Shell shell;
}
