/*
 * Marker.java - Named location in a buffer
 * Copyright (C) 1998, 1999, 2000 Slava Pestov
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

import javax.swing.text.BadLocationException;
import javax.swing.text.Position;
import org.gjt.sp.util.Log;

/**
 * A marker is a name/position pair, that can be used to name locations
 * in Swing <code>Document</code> instances.<p>
 *
 * Markers are primarily used in buffers. They can be added with
 * <code>Buffer.addMarker()</code>, removed with
 * <code>Buffer.removeMarker()</code>, and a marker instance can be
 * obtained by calling <code>Buffer.getMarker()</code> with the marker's
 * name.
 *
 * @author Slava Pestov
 * @version $Id$
 *
 * @see Buffer#addMarker(String,int,int)
 * @see Buffer#getMarker(String)
 * @see Buffer#removeMarker(String)
 */
public class Marker
{
	/**
	 * Creates a new marker. This should not be called under
	 * normal circumstances - use <code>Buffer.addMarker()</code>
	 * instead.
	 */
	public Marker(Buffer buffer, String name, int start, int end)
	{
		this.buffer = buffer;
		this.name = name;
		this.start = start;
		this.end = end;
	}

	/**
	 * Creates the floating positions.
	 */
	public void createPositions()
	{
		try
		{
			startPosition = buffer.createPosition(start);
			endPosition = buffer.createPosition(end);
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
	}

	/**
	 * Returns the name of this marker.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Returns the start position of this marker.
	 */
	public int getStart()
	{
		return startPosition.getOffset();
	}

	/**
	 * Returns the end position of this marker.
	 */
	public int getEnd()
	{
		return endPosition.getOffset();
	}

	// private members
	private Buffer buffer;
	private String name;
	private int start, end;
	private Position startPosition, endPosition;
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.4  2000/08/17 08:04:09  sp
 * Marker loading bug fixed, docking option pane
 *
 * Revision 1.3  1999/03/12 07:54:47  sp
 * More Javadoc updates
 *
 */
