/*
 * FavoritesVFS.java - Stores frequently-visited directory locations
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

import java.awt.Component;
import java.util.Vector;

/**
 * A VFS used for remembering frequently-visited directories. Listing it
 * returns the favorites list. The deletePath of each entry is the
 * directory prefixed with "favorites:" so that right-clicking on a
 * favorite and clicking 'delete' in the browser just deletes the
 * favorite, and not the directory itself.
 * @author Slava Pestov
 * @version $Id$
 */
public class FavoritesVFS extends VFS
{
	public static final String PROTOCOL = "favorites";

	public FavoritesVFS()
	{
		super("favorites");
		lock = new Object();
		favorites = new Vector();
	}

	public int getCapabilities()
	{
		// BROWSE_CAP not set because we don't want the VFS browser
		// to create the default 'favorites' button on the tool bar
		return /* BROWSE_CAP | */ DELETE_CAP;
	}

	public VFS.DirectoryEntry[] _listDirectory(VFSSession session, String url,
		Component comp)
	{
		synchronized(lock)
		{
			VFS.DirectoryEntry[] retVal = new VFS.DirectoryEntry[favorites.size()];
			for(int i = 0; i < retVal.length; i++)
			{
				String favorite = (String)favorites.elementAt(i);
				retVal[i] = new VFS.DirectoryEntry(favorite,
					favorite,"favorites:" + favorite,
					VFS.DirectoryEntry.DIRECTORY,
					0L,false);
			}
			return retVal;
		}
	}

	public void _delete(VFSSession session, String path, Component comp)
	{
		synchronized(lock)
		{
			path = path.substring("favorites:".length());
			favorites.removeElement(path);
		}
	}

	public static void loadFavorites()
	{
		String favorite;
		int i = 0;
		while((favorite = jEdit.getProperty("vfs.favorite." + i)) != null)
		{
			favorites.addElement(favorite);
			i++;
		}
	}

	public static void addToFavorites(String path)
	{
		synchronized(lock)
		{
			favorites.addElement(path);
		}
	}

	public static void saveFavorites()
	{
		for(int i = 0; i < favorites.size(); i++)
		{
			jEdit.setProperty("vfs.favorite." + i,favorites.elementAt(i));
		}
	}

	// private members
	private static Object lock;
	private static Vector favorites;
}

/*
 * Change Log:
 * $Log$
 * Revision 1.1  2000/08/03 07:43:42  sp
 * Favorites added to browser, lots of other stuff too
 *
 */
