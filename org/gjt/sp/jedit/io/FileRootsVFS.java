/*
 * FileRootsVFS.java - Local root filesystems VFS
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

import javax.swing.filechooser.FileSystemView;
import java.awt.Component;
import java.io.File;

/**
 * A VFS that lists local root filesystems.
 * @author Slava Pestov
 * @version $Id$
 */
public class FileRootsVFS extends VFS
{
	public static final String PROTOCOL = "roots";

	public FileRootsVFS()
	{
		super("roots");
		fsView = FileSystemView.getFileSystemView();
	}

	public int getCapabilities()
	{
		// BROWSE_CAP not set because we don't want the VFS browser
		// to create the default 'favorites' button on the tool bar
		return 0 /* BROWSE_CAP | */;
	}

	public VFS.DirectoryEntry[] _listDirectory(VFSSession session, String url,
		Component comp)
	{
		File[] roots = fsView.getRoots();

		if(roots == null)
			return null;

		VFS.DirectoryEntry[] rootDE = new VFS.DirectoryEntry[roots.length];
		for(int i = 0; i < roots.length; i++)
		{
			String name = roots[i].getPath();
			rootDE[i] = new VFS.DirectoryEntry(name,name,name,
				VFS.DirectoryEntry.FILESYSTEM,0L,false);
		}

		return rootDE;
	}

	// private members
	private FileSystemView fsView;
}

/*
 * Change Log:
 * $Log$
 * Revision 1.1  2000/08/03 07:43:42  sp
 * Favorites added to browser, lots of other stuff too
 *
 */
