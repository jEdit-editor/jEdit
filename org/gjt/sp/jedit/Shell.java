/*
 * Shell.java - EditBus shell interface
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
 * An abstract interface to a shell that executes commands.
 * @author Slava Pestov
 * @version $Id$
 *
 * @since jEdit 2.2pre6
 */
public interface Shell
{
	/**
	 * Executes a command.
	 * @param view The view
	 * @param command The command. The format of this is left entirely
	 * up to the implementation
	 * @param output The output
	 */
	void execute(View view, String command, Output output);

	/**
	 * Stops the currently executing command, if any.
	 */
	void stop();
}
