/*
 * EditorEvent.java - Event fired when views and buffers are created and closed
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
 * The event fired when a view or buffer is created or closed.
 * Each editor event carries the buffer or view involved, along with
 * the event type and timestamp.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class EditorEvent extends AbstractEditorEvent
{
	/**
	 * The first event id that denotes an editor event.
	 */
	public static final int EDITOR_FIRST = ID_FIRST;

	/**
	 * The return value of the <code>getID()</code> function when
	 * a buffer has been created. This event is fired after the
	 * buffer has been added to the buffer list.
	 */
	public static final int BUFFER_CREATED = EDITOR_FIRST;

	/**
	 * The return value of the <code>getID()</code> function when
	 * a buffer has been closed. This event is fired after the
	 * buffer is removed from the buffer list.
	 */
	public static final int BUFFER_CLOSED = EDITOR_FIRST + 1;

	/**
	 * The return value of the <code>getID()</code> function when
	 * a view has been created. This event is fired after the view
	 * frame is shown.
	 */
	public static final int VIEW_CREATED = EDITOR_FIRST + 2;

	/**
	 * The return value of the <code>getID()</code> function when
	 * a view has been closed. This event is fired after the view
	 * frame has been destroyed.
	 */
	public static final int VIEW_CLOSED = EDITOR_FIRST + 3;

	/**
	 * The return value of the <code>getID()</code> function when
	 * value of properties that might require settings to be
	 * reloaded have changed. At the moment, this is fired by
	 * the Options dialog box only.
	 */
	public static final int PROPERTIES_CHANGED = EDITOR_FIRST + 4;

	/**
	 * The return value of the <code>getID()</code> function when
	 * the list of available macros has changed.
	 *
	 * @since jEdit 2.2pre4
	 */
	public static final int MACROS_CHANGED = EDITOR_FIRST + 5;

	/**
	 * The last event id that denotes an editor event.
	 */
	public static final int EDITOR_LAST = EDITOR_FIRST + 5;

	/**
	 * Creates a new editor event.
	 * @param id The event type
	 * @param view The view involved
	 * @param buffer The buffer involved
	 */
	public EditorEvent(int id, View view, Buffer buffer)
	{
		super(id,view,buffer);

		// Check the id value
		if(id < EDITOR_FIRST || id > EDITOR_LAST)
			throw new IllegalArgumentException("id out of range");
	}

	/**
	 * Fires the event to the specified listener. This invokes
	 * the appropriate method of the listener, depending on the
	 * event type.
	 * @param listener The event listener
	 */
	public void fire(EventListener listener)
	{
		if(!(listener instanceof EditorListener))
			return;

		EditorListener l = (EditorListener)listener;

		switch(id)
		{
		case BUFFER_CREATED:
			l.bufferCreated(this);
			break;
		case BUFFER_CLOSED:
			l.bufferClosed(this);
			break;
		case VIEW_CREATED:
			l.viewCreated(this);
			break;
		case VIEW_CLOSED:
			l.viewClosed(this);
			break;
		case PROPERTIES_CHANGED:
			l.propertiesChanged(this);
			break;
		case MACROS_CHANGED:
			l.macrosChanged(this);
			break;
		default:
			// shouldn't happen
			throw new InternalError();
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.8  1999/11/09 10:14:34  sp
 * Macro code cleanups, menu item and tool bar clicks are recorded now, delete
 * word commands, check box menu item support
 *
 * Revision 1.7  1999/07/08 06:06:04  sp
 * Bug fixes and miscallaneous updates
 *
 * Revision 1.6  1999/03/16 04:34:46  sp
 * HistoryTextField updates, moved generate-text to a plugin, fixed spelling mistake in EditAction Javadocs
 *
 * Revision 1.5  1999/03/14 02:22:13  sp
 * Syntax colorizing tweaks, server bug fix
 *
 * Revision 1.4  1999/03/12 23:51:00  sp
 * Console updates, uncomment removed cos it's too buggy, cvs log tags added
 *
 */
