/*
 * ExecuteCommand.java - Execute command message
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
 * Message to execute a command. Upon receiving this message, plugins should
 * check if they can execute this particular command. If it can, the command
 * should be executed, and the message vetoed.
 * @author Slava Pestov
 * @version $Id$
 *
 * @since jEdit 2.2pre6
 */
public class ExecuteCommand extends EBMessage
{
	/**
	 * Creates a new execute command message.
	 * @param source The message source
	 * @param name The name of the shell that should execute the command
	 * @param command The command
	 * @param output An interface for text output
	 */
	public ExecuteCommand(EBComponent source, String name,
		String command, View view, Output output)
	{
		super(source);

		if(name == null || command == null)
			throw new NullPointerException("Name and command must be non-null");

		this.name = name;
		this.command = command;
		this.view = view;
		this.output = output;

		tag = new Tag();
	}

	/**
	 * Returns the shell that should execute the command.
	 */
	public String getShellName()
	{
		return name;
	}

	/**
	 * Returns the command.
	 */
	public String getCommand()
	{
		return command;
	}

	/**
	 * Returns the interface for text output.
	 */
	public Output getOutput()
	{
		return output;
	}

	/**
	 * Returns the current view. This can be null.
	 */
	public View getView()
	{
		return view;
	}

	/**
	 * Returns the process tag. This can be passed to the
	 * StopCommand message to stop this command.
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
		return super.paramString() + ",name=" + name + ",command="
			+ command + ",tag=" + tag;
	}

	// private members
	private String name;
	private String command;
	private View view;
	private Output output;
	private Object tag;
}
