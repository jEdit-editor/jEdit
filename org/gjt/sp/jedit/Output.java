/*
 * Output.java - EditBus output interface
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

/**
 * An interface for displaying text output, much like a terminal. Plugins
 * that implement some sort of shell or interpreter can use outputs to
 * display output to the user.
 * @author Slava Pestov
 * @version $Id$
 *
 * @since jEdit 2.2pre6
 */
public interface Output
{
	/**
	 * Appends some output to the display.
	 * @param color The color to display it in (null will use
	 * the default)
	 * @param text The text
	 */
	void addOutput(Color color, String text);

	/**
	 * This method must be called when the command
	 * finishes executing. The output may take some action, for
	 * example printing a "command done" message.
	 */
	void commandDone();

	/**
	 * Returns the default color for informational messages.
	 */
	Color getInfoColor();

	/**
	 * Returns the default color for error messages.
	 */
	Color getErrorColor();
}
