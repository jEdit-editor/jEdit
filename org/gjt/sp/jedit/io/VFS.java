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

import java.awt.Component;
import java.io.*;
import org.gjt.sp.jedit.*;

/**
 * A virtual filesystem implementation. Note tha methods whose names are
 * prefixed with "_" are called from the I/O thread.
 * @param author Slava Pestov
 * @author $Id$
 */
public abstract class VFS
{
	/**
	 * Read capability.
	 * @since jEdit 2.6pre2
	 */
	public static final int READ_CAP = 1 << 0;

	/**
	 * Write capability.
	 * @since jEdit 2.6pre2
	 */
	public static final int WRITE_CAP = 1 << 1;

	/**
	 * VFS browser capability.
	 * @since jEdit 2.6pre2
	 */
	public static final int BROWSE_CAP = 1 << 2;

	/**
	 * Delete file capability.
	 * @since jEdit 2.6pre2
	 */
	public static final int DELETE_CAP = 1 << 3;

	/**
	 * Rename file capability.
	 * @since jEdit 2.6pre2
	 */
	public static final int RENAME_CAP = 1 << 4;

	/**
	 * Make directory file capability.
	 * @since jEdit 2.6pre2
	 */
	public static final int MKDIR_CAP = 1 << 5;

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
	 * label stored in the <code>vfs.<i>name</i>.label</code>
	 * property, and the icon from the <code>vfs.<i>name</i>.icon</code>
	 * property.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Returns the capabilities of this VFS.
	 * @since jEdit 2.6pre2
	 */
	public abstract int getCapabilities();

	/**
	 * Displays a dialog box that should set up a session and return
	 * the initial URL to browse.
	 * @param session The VFS session
	 * @param comp The component that will parent error dialog boxes
	 * @return The URL
	 * @since jEdit 2.6pre2
	 */
	public String showBrowseDialog(VFSSession session, Component comp)
	{
		return null;
	}

	/**
	 * Returns the parent of the specified URL. By default,
	 * same as MiscUtilities.getFileParent().
	 * @param url The URL
	 * @since jEdit 2.6pre2
	 */
	public String getFileParent(String url)
	{
		return MiscUtilities.getFileParent(url);
	}

	/**
	 * Constructs a path from the specified directory and
	 * file name component. By default, same as
	 * MiscUtilities.constructPath().
	 * @param parent The parent directory
	 * @param path The path
	 * @since jEdit 2.6pre2
	 */
	public String constructPath(String parent, String path)
	{
		return MiscUtilities.constructPath(parent,path);
	}

	/**
	 * Starts a VFS session. This method is called from the AWT thread,
	 * so it should not do any I/O. It could, however, prompt for
	 * a login name and password, for example.
	 * @param session The VFS session
	 * @param comp The component that will parent error dialog boxes
	 * @return True if everything is okay, false if the user cancelled
	 * the operation
	 * @since jEdit 2.6pre2
	 */
	public boolean setupVFSSession(VFSSession session, Component comp)
	{
		session.setOwnerVFS(this);
		return true;
	}

	/**
	 * Loads the specified buffer. The default implementation posts
	 * an I/O request to the I/O thread.
	 * @param view The view
	 * @param buffer The buffer
	 * @param path The path
	 */
	public boolean load(View view, Buffer buffer, String path)
	{
		if(!setupVFSSession(buffer.getVFSSession(),view))
			return false;

		VFSManager.runInWorkThread(new IORequest(IORequest.LOAD,
			view,buffer,path,this));
		return true;
	}

	/**
	 * Saves the specifies buffer. The default implementation posts
	 * an I/O request to the I/O thread.
	 * @param view The view
	 * @param buffer The buffer
	 * @param path The path
	 */
	public boolean save(View view, Buffer buffer, String path)
	{
		if(!setupVFSSession(buffer.getVFSSession(),view))
			return false;

		VFSManager.runInWorkThread(new IORequest(IORequest.SAVE,
			view,buffer,path,this));
		return true;
	}

	// the remaining methods are only called from the I/O thread

	/**
	 * Lists the specified directory. Note that this must be a full
	 * URL, including the host name, path name, and so on. The
	 * username and password is obtained from the session.
	 * @param session The session
	 * @param directory The directory
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException if an I/O error occurred
	 * @since jEdit 2.6pre2
	 */
	public DirectoryEntry[] _listDirectory(VFSSession session, String directory,
		Component comp)
		throws IOException
	{
		return null;
	}

	/**
	 * Returns the specified directory entry.
	 * @param session The session
	 * @param url The URL
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException if an I/O error occurred
	 * @since jEdit 2.6pre2
	 */
	public DirectoryEntry _getDirectoryEntry(VFSSession session, String url,
		Component comp)
		throws IOException
	{
		return null;
	}

	/**
	 * A directory entry.
	 * @since jEdit 2.6pre2
	 */
	public static class DirectoryEntry implements Serializable
	{
		public static final int FILE = 0;
		public static final int DIRECTORY = 1;
		public static final int FILESYSTEM = 2;

		public String name;
		public String path;
		public String deletePath;
		public int type;
		public long length;
		public boolean hidden;

		public DirectoryEntry(String name, String path, String deletePath,
			int type, long length, boolean hidden)
		{
			this.name = name;
			this.path = path;
			this.deletePath = deletePath;
			this.type = type;
			this.length = length;
			this.hidden = hidden;
		}

		public String toString()
		{
			return name;
		}
	}

	/**
	 * Deletes the specified URL.
	 * @param session The VFS session
	 * @param url The URL
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException if an I/O error occurs
	 * @since jEdit 2.6pre2
	 */
	public boolean _delete(VFSSession session, String url, Component comp)
		throws IOException
	{
		return false;
	}

	/**
	 * Creates a new directory with the specified URL.
	 * @param session The VFS session
	 * @param directory The directory
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException if an I/O error occurs
	 * @since jEdit 2.6pre2
	 */
	public boolean _mkdir(VFSSession session, String directory, Component comp)
		throws IOException
	{
		return false;
	}

	/**
	 * A buffer has been loaded. This method is called from the I/O
	 * thread.
	 * @param buffer The buffer
	 * @exception IOException If an I/O error occurs
	 */
	public void _loadComplete(Buffer buffer) throws IOException {}

	/**
	 * A buffer has been saved. This method is called from the I/O
	 * thread.
	 * @param buffer The buffer
	 * @exception IOException If an I/O error occurs
	 */
	public void _saveComplete(Buffer buffer) throws IOException {}

	/**
	 * Creates an input stream. This method is called from the I/O
	 * thread.
	 * @param session the VFS session
	 * @param path The path
	 * @param ignoreErrors If true, file not found errors should be
	 * ignored
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException If an I/O error occurs
	 * @since jEdit 2.6pre2
	 */
	public InputStream _createInputStream(VFSSession session,
		String path, boolean ignoreErrors, Component comp)
		throws IOException
	{
		return null;
	}

	/**
	 * Creates an output stream. This method is called from the I/O
	 * thread.
	 * @param session the VFS session
	 * @param path The path
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException If an I/O error occurs
	 * @since jEdit 2.6pre2
	 */
	public OutputStream _createOutputStream(VFSSession session,
		String path, Component comp)
		throws IOException
	{
		return null;
	}

	/**
	 * Finishes the specified VFS session. This must be called
	 * after all I/O with this VFS is complete, to avoid leaving
	 * stale network connections and such.
	 * @param session The VFS session
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException if an I/O error occurred
	 * @since jEdit 2.6pre2
	 */
	public void _endVFSSession(VFSSession session, Component comp)
		throws IOException
	{
		if(session.getOwnerVFS() == this)
			session.setOwnerVFS(null);
		else if(session.getOwnerVFS() == null)
		{
			throw new IllegalArgumentException("_endSession()"
				+ " called on an unowned session");
		}
		else
		{
			throw new IllegalArgumentException("_endSession()"
				+ " called on a session that is not mine");
		}
	}

	// private members
	private String name;
}

/*
 * Change Log:
 * $Log$
 * Revision 1.13  2000/08/05 07:16:12  sp
 * Global options dialog box updated, VFS browser now supports right-click menus
 *
 * Revision 1.12  2000/08/03 07:43:42  sp
 * Favorites added to browser, lots of other stuff too
 *
 * Revision 1.11  2000/07/31 11:32:09  sp
 * VFS file chooser is now in a minimally usable state
 *
 * Revision 1.10  2000/07/30 09:04:19  sp
 * More VFS browser hacking
 *
 * Revision 1.9  2000/07/29 12:24:08  sp
 * More VFS work, VFS browser started
 *
 * Revision 1.8  2000/07/26 07:48:45  sp
 * stuff
 *
 * Revision 1.7  2000/07/19 08:35:59  sp
 * plugin devel docs updated, minor other changes
 *
 * Revision 1.6  2000/04/29 09:17:07  sp
 * VFS updates, various fixes
 *
 * Revision 1.5  2000/04/28 09:29:12  sp
 * Key binding handling improved, VFS updates, some other stuff
 *
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
