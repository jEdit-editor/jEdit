/*
 * VFS.java - Virtual filesystem implementation
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

import java.io.*;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.View;

/**
 * A virtual filesystem implementation.
 * @param author Slava Pestov
 * @author $Id$
 */
public abstract class VFS
{
	/**
	 * Creates a new virtual filesystem.
	 * @param name The name
	 */
	public VFS(String name)
	{
		this.name = name;
	}

	/**
	 * Returns this VFS's name. The name is used to obtain the
	 * menu item label stored in the <code>vfs.<i>name</i>.label</code>
	 * by the 'Open From' and 'Save To' menus.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Displays an open dialog box and returns the selected pathname.
	 * @param view The view
	 * @param buffer The buffer
	 */
	public abstract Buffer showOpenDialog(View view, Buffer buffer);

	/**
	 * Displays a save dialog box and returns the selected pathname.
	 * @param view The view
	 * @param buffer The buffer
	 */
	public abstract String showSaveDialog(View view, Buffer buffer);

	/**
	 * Loads the specified buffer. The default implementation posts
	 * an I/O request to the I/O thread.
	 * @param view The view
	 * @param buffer The buffer
	 * @param path The path
	 */
	public boolean load(View view, Buffer buffer, String path)
	{
		VFSManager.addIORequest(IORequest.LOAD,view,buffer,path,this);
		return true;
	}

	/**
	 * Called when the I/O thread has finished loading a buffer.
	 */
	public void loadCompleted(View view, Buffer buffer, String path) {}

	/**
	 * Saves the specifies buffer. The default implementation posts
	 * an I/O request to the I/O thread.
	 * @param view The view
	 * @param buffer The buffer
	 * @param path The path
	 */
	public boolean save(View view, Buffer buffer, String path)
	{
		VFSManager.addIORequest(IORequest.SAVE,view,buffer,path,this);
		return true;
	}

	/**
	 * Called when the I/O thread has finished saving a buffer.
	 */
	public void saveCompleted(View view, Buffer buffer, String path) {}

	/**
	 * Returns the last time the specified path has been modified on disk
	 * (or other storage medium). The default implementation returns 0
	 * (meaning this VFS doesn't support last modification times)
	 */
	public long getLastModified(String path)
	{
		return 0L;
	}

	/**
	 * Returns true if this VFS supports file deletion. This is required
	 * for marker saving to work. By default, this returns false.
	 */
	public boolean canDelete()
	{
		return false;
	}

	/**
	 * Deletes the specified file. By default, this does nothing.
	 */
	public void delete(String path)
	{
	}

	/**
	 * Creates an input stream. This method is called from the I/O
	 * thread.
	 * @param view The view
	 * @param path The path
	 * @param ignoreErrors If true, file not found errors should be
	 * ignored
	 * @exception IOException If an I/O error occurs
	 */
	public abstract  InputStream _createInputStream(View view, String path,
		boolean ignoreErrors) throws IOException;

	/**
	 * Creates an output stream. This method is called from the I/O
	 * thread.
	 * @param view The view
	 * @param path The path
	 * @exception IOException If an I/O error occurs
	 */
	public abstract OutputStream _createOutputStream(View view, String path)
		throws IOException;

	// private members
	private String name;
}

/*
 * Change Log:
 * $Log$
 * Revision 1.4  2000/04/27 08:32:57  sp
 * VFS fixes, read only fixes, macros can prompt user for input, improved
 * backup directory feature
 *
 * Revision 1.3  2000/04/25 11:00:20  sp
 * FTP VFS hacking, some other stuff
 *
 * Revision 1.2  2000/04/24 11:00:23  sp
 * More VFS hacking
 *
 * Revision 1.1  2000/04/24 04:45:37  sp
 * New I/O system started, and a few minor updates
 *
 */
