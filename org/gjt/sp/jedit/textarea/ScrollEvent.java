/*
 * ScrollEvent.java - text area scrolled
 * Copyright (C) 2000 Slava Pestov
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

package org.gjt.sp.jedit.textarea;

import java.util.EventObject;

/**
 * Event that is fired when the text area is scrolled.
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 2.5pre4
 */
public class ScrollEvent extends EventObject
{
	/**
	 * The vertical scroll bar position changed.
	 */
	public static final int VERTICAL = 0;

	/**
	 * The horizontal scroll bar position changed.
	 */
	public static final int HORIZONTAL = 1;

	/**
	 * Creates a new scroll event.
	 * @param textArea the text area
	 */
	public ScrollEvent(JEditTextArea textArea, int id)
	{
		super(textArea);
		this.id = id;
	}

	/**
	 * Returns the event type.
	 */
	public int getType()
	{
		return id;
	}

	// private members
	private int id;
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.1  2000/05/22 12:05:46  sp
 * Markers are highlighted in the gutter, bug fixes
 *
 */
