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
 * the server, and so on, between VFS calls. This class facilitates
 * storage of such information.
 *
 * @author Slava Pestov
 * @version $Id$
 *
 * @since jEdit 2.6pre2
 */
public class VFSSession
{
	/**
	 * This key contains the host name for the path in question, if any.
	 * @since jEdit 2.6pre3
	 */
	public static final String HOSTNAME_KEY = "VFSSession.hostname";

	/**
	 * This key contains the user name for the path in question, if any.
	 * @since jEdit 2.6pre3
	 */
	public static final String USERNAME_KEY = "VFSSession.username";

	/**
	 * This key contains the password for the path in question, if any.
	 * @since jEdit 2.6pre3
	 */
	public static final String PASSWORD_KEY = "VFSSession.password";

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

	public String toString()
	{
		return getClass().getName() + ":" + hashtable;
	}

	// private members
	private Hashtable hashtable = new Hashtable();
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.4  2000/08/16 12:14:29  sp
 * Passwords are now saved, bug fixes, documentation updates
 *
 * Revision 1.3  2000/08/15 08:07:11  sp
 * A bunch of bug fixes
 *
 * Revision 1.2  2000/07/31 11:32:09  sp
 * VFS file chooser is now in a minimally usable state
 *
 * Revision 1.1  2000/07/29 12:24:08  sp
 * More VFS work, VFS browser started
 *
 */
