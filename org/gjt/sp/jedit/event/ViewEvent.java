/*
 * ViewEvent.java - Event fired when a view's state changes
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
 * The event fired when a view's state changes.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class ViewEvent extends AbstractEditorEvent
{
	/**
	 * The first event id that denotes an view event.
	 */
	public static final int VIEW_FIRST = BufferEvent.BUFFER_LAST + 1;

	/**
	 * The return value of the <code>getID()</code> function when
	 * a view's current buffer has changed.
	 */
	public static final int BUFFER_CHANGED = VIEW_FIRST;

	/**
	 * The last event id that denotes a view event.
	 */
	public static final int VIEW_LAST = VIEW_FIRST;

	/**
	 * Creates a new view event.
	 * @param id The event type
	 * @param view The view involved
	 * @param buffer The original buffer
	 */
	public ViewEvent(int id, View view, Buffer buffer)
	{
		super(id,view,buffer);

		// Check the id value
		if(id < VIEW_FIRST || id > VIEW_LAST)
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
		if(!(listener instanceof ViewListener))
			return;

		ViewListener l = (ViewListener)listener;

		switch(id)
		{
		case BUFFER_CHANGED:
			l.viewBufferChanged(this);
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
 * Revision 1.7  1999/11/10 10:43:01  sp
 * Macros can now have shortcuts, various miscallaneous updates
 *
 * Revision 1.6  1999/07/08 06:06:04  sp
 * Bug fixes and miscallaneous updates
 *
 * Revision 1.5  1999/05/27 03:09:22  sp
 * Console unbundled
 *
 * Revision 1.4  1999/03/17 05:32:52  sp
 * Event system bug fix, history text field updates (but it still doesn't work), code cleanups, lots of banging head against wall
 *
 * Revision 1.3  1999/03/16 04:34:46  sp
 * HistoryTextField updates, moved generate-text to a plugin, fixed spelling mistake in EditAction Javadocs
 *
 * Revision 1.2  1999/03/12 23:51:00  sp
 * Console updates, uncomment removed cos it's too buggy, cvs log tags added
 *
 */
