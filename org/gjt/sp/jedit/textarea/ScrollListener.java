/*
 * ScrollListener.java - text area scroll event listener
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

import java.util.EventListener;

/**
 * A listener for text area scrolled events.
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 2.5pre4
 */
public interface ScrollListener extends EventListener
{
	/**
	 * The vertical scroll bar position has changed.
	 * @param evt The event
	 */
	void verticalScrollUpdate(ScrollEvent evt);

	/**
	 * The horizontal scroll bar position has changed.
	 * @param evt The event
	 */
	void horizontalScrollUpdate(ScrollEvent evt);
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.1  2000/05/22 12:05:46  sp
 * Markers are highlighted in the gutter, bug fixes
 *
 */
