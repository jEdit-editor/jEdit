/*
 * EditorListener.java - Editor event listener
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

package org.gjt.sp.jedit.event;

import java.util.EventListener;
import org.gjt.sp.jedit.*;

/**
 * The editor event listener interface.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public interface EditorListener extends EventListener
{
	/**
	 * Method invoked when a buffer has been created.
	 */
	public void bufferCreated(EditorEvent evt);

	/**
	 * Method invoked when a buffer has been closed.
	 */
	public void bufferClosed(EditorEvent evt);

	/**
	 * Method invoked when a view has been created.
	 */
	public void viewCreated(EditorEvent evt);

	/**
	 * Method invoked when a view has been closed.
	 */
	public void viewClosed(EditorEvent evt);

	/**
	 * Method invoked when the values of properties that
	 * might require settings to be reloaded are changed.
	 * This is invoked by the Options dialog box.
	 */
	public void propertiesChanged(EditorEvent evt);

	/**
	 * Method invoked when the list of available macros has
	 * changed.
	 */
	public void macrosChanged(EditorEvent evt);
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.7  1999/11/09 10:14:34  sp
 * Macro code cleanups, menu item and tool bar clicks are recorded now, delete
 * word commands, check box menu item support
 *
 * Revision 1.6  1999/03/16 04:34:46  sp
 * HistoryTextField updates, moved generate-text to a plugin, fixed spelling mistake in EditAction Javadocs
 *
 * Revision 1.5  1999/03/14 04:13:40  sp
 * Fixed ArrayIndexOutOfBounds in TokenMarker, minor Javadoc updates, minor documentation updates
 *
 * Revision 1.4  1999/03/14 02:22:13  sp
 * Syntax colorizing tweaks, server bug fix
 *
 * Revision 1.3  1999/03/12 23:51:00  sp
 * Console updates, uncomment removed cos it's too buggy, cvs log tags added
 *
 */
