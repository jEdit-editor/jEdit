/*
 * VFSSession.java - Stores state between VFS calls
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

package org.gjt.sp.jedit.io;

import java.util.Hashtable;

/**
 * Some virtual filesystems, such as the FTP filesystem, need to store
 * certain state, such as login information, the socket connection to
 * the server, and so on, between VFS calls. This calls facilitates
 * storage of such information.
 *
 * @author Slava Pestov
 * @version $Id$
 *
 * @since jEdit 2.6pre2
 */
public class VFSSession implements Cloneable
{
	/**
	 * This key contains the path name of the buffer which created
	 * this session, if any. It should only be accessed by methods
	 * in the VFS class that directly deal with buffers (for
	 * example, load() and save()).<p>
	 *
	 * The VFS browser also stores the path of the current directory
	 * in this key.
	 */
	public static final String PATH_KEY = "VFSSession__path";

	/**
	 * Returns a stored value.
	 * @param key The key
	 */
	public Object get(Object key)
	{
		return hashtable.get(key);
	}

	/**
	 * Stores a value.
	 * @param key The key
	 * @param value The value
	 */
	public void put(Object key, Object value)
	{
		hashtable.put(key,value);
	}

	/**
	 * Removes a value.
	 * @param key The key
	 */
	public void remove(Object key)
	{
		hashtable.remove(key);
	}

	/**
	 * Returns the VFS that owns this session.
	 */
	public VFS getOwnerVFS()
	{
		return owner;
	}

	/**
	 * Sets the VFS that owns this session.
	 * @param vfs The VFS
	 */
	public void setOwnerVFS(VFS owner)
	{
		if(this.owner != null && owner != null)
		{
			throw new IllegalArgumentException("setOwnerVFS() called"
				+ " on an in-use session");
		}
		else
			this.owner = owner;
	}

	public Object clone()
	{
		VFSSession clone = new VFSSession();
		clone.hashtable = (Hashtable)hashtable.clone();
		return clone;
	}

	// private members
	private VFS owner;
	private Hashtable hashtable = new Hashtable();
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.2  2000/07/31 11:32:09  sp
 * VFS file chooser is now in a minimally usable state
 *
 * Revision 1.1  2000/07/29 12:24:08  sp
 * More VFS work, VFS browser started
 *
 */
