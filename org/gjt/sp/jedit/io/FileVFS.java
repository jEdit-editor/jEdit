/*
 * FileVFS.java - Local filesystem VFS
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

package org.gjt.sp.jedit.io;

import javax.swing.filechooser.FileSystemView;
import java.awt.Component;
import java.io.*;
import java.util.Vector;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

/**
 * Local filesystem VFS.
 * @author Slava Pestov
 * @version $Id$
 */
public class FileVFS extends VFS
{
	public static final String BACKED_UP_PROPERTY = "FileVFS__backedUp";

	public FileVFS()
	{
		super("file");
		fsView = FileSystemView.getFileSystemView();
	}

	public int getCapabilities()
	{
		return READ_CAP | WRITE_CAP | BROWSE_CAP | DELETE_CAP
			| RENAME_CAP | MKDIR_CAP;
	}

	public String getParentOfPath(String path)
	{
		File[] roots = fsView.getRoots();
		for(int i = 0; i < roots.length; i++)
		{
			if(roots[i].getPath().equals(path))
				return FileRootsVFS.PROTOCOL + ":";
		}

		return MiscUtilities.getParentOfPath(path);
	}

	public String constructPath(String parent, String path)
	{
		return MiscUtilities.constructPath(parent,path);
	}

	public char getFileSeparator()
	{
		return File.separatorChar;
	}

	public boolean load(View view, Buffer buffer, String path)
	{
		File file = buffer.getFile();

		if(!file.exists())
		{
			buffer.setNewFile(true);
			buffer.setDirty(false);
			return false;
		}
		else
			buffer.setReadOnly(!file.canWrite());

		if(file.isDirectory())
		{
			String[] args = { file.getPath() };
			GUIUtilities.error(view,"open-directory",args);
			buffer.setNewFile(false);
			buffer.setDirty(false);
			return false;
		}

		if(!file.canRead())
		{
			String[] args = { file.getPath() };
			GUIUtilities.error(view,"no-read",args);
			buffer.setNewFile(false);
			buffer.setDirty(false);
			return false;
		}

		return super.load(view,buffer,path);
	}

	public boolean save(View view, Buffer buffer, String path)
	{
		// can't call buffer.getFile() here because this
		// method is called *before* setPath()
		File file = new File(path);

		// Apparently, certain broken OSes (like Micro$oft Windows)
		// can mess up directories if they are write()'n to
		if(file.isDirectory())
		{
			String[] args = { file.getPath() };
			GUIUtilities.error(view,"save-directory",args);
			return false;
		}

		// Check that we can actually write to the file
		if((file.exists() && !file.canWrite())
			|| (!file.exists() && !new File(file.getParent()).canWrite()))
		{
			String[] args = { path };
			GUIUtilities.error(view,"no-write",args);
			return false;
		}

		/* When doing a 'save as', the path to save to (path)
		 * will not be the same as the buffer's previous path
		 * (buffer.getPath()). In that case, we want to create
		 * a backup of the new path, even if the old path was
		 * backed up as well (BACKED_UP property set) */
		if(!path.equals(buffer.getPath()))
			buffer.getDocumentProperties().remove(BACKED_UP_PROPERTY);

		backup(buffer,file);
		return super.save(view,buffer,path);
	}

	public VFS.DirectoryEntry[] _listDirectory(VFSSession session, String path,
		Component comp)
	{
		File directory = new File(path);
		String[] list = directory.list();
		if(list == null)
			return null;

		Vector list2 = new Vector();
		for(int i = 0; i < list.length; i++)
		{
			String name = list[i];
			VFS.DirectoryEntry file = _getDirectoryEntry(session,
				MiscUtilities.constructPath(path,name),comp);
			if(file != null)
				list2.addElement(file);
		}

		VFS.DirectoryEntry[] retVal = new VFS.DirectoryEntry[list2.size()];
		list2.copyInto(retVal);
		return retVal;
	}

	public DirectoryEntry _getDirectoryEntry(VFSSession session, String path,
		Component comp)
	{
		File file = new File(path);
		if(!file.exists())
			return null;

		int type;
		if(file.isDirectory())
			type = VFS.DirectoryEntry.DIRECTORY;
		else
			type = VFS.DirectoryEntry.FILE;

		return new VFS.DirectoryEntry(file.getName(),
			path,path,type,file.length(),fsView.isHiddenFile(file));
	}

	public boolean _delete(VFSSession session, String path, Component comp)
	{
		boolean retVal = new File(path).delete();
		VFSManager.sendVFSUpdate(this,path,true);
		return retVal;
	}

	public boolean _rename(VFSSession session, String from, String to,
		Component comp)
	{
		boolean retVal = new File(from).renameTo(new File(to));
		VFSManager.sendVFSUpdate(this,from,true);
		return retVal;
	}


	public boolean _mkdir(VFSSession session, String directory, Component comp)
	{
		boolean retVal = new File(directory).mkdir();
		VFSManager.sendVFSUpdate(this,directory,true);
		return retVal;
	}

	public InputStream _createInputStream(VFSSession session, String path,
		boolean ignoreErrors, Component comp) throws IOException
	{
		try
		{
			return new FileInputStream(path);
		}
		catch(IOException io)
		{
			if(ignoreErrors)
				return null;
			else
				throw io;
		}
	}

	public OutputStream _createOutputStream(VFSSession session, String path,
		Component comp) throws IOException
	{
		OutputStream retVal = new FileOutputStream(path);
		VFSManager.sendVFSUpdate(this,path,true);
		return retVal;
	}

	// private members
	private FileSystemView fsView;

	// The BACKED_UP flag prevents more than one backup from being
	// written per session (I guess this should be made configurable
	// in the future)
	//
	// we don't fire a VFSUpdate in this message as _createOutputStream()
	// will do it anyway
	private void backup(Buffer buffer, File file)
	{
		if(buffer.getProperty(BACKED_UP_PROPERTY) != null)
			return;
		buffer.putProperty(BACKED_UP_PROPERTY,Boolean.TRUE);

		// Fetch properties
		int backups;
		try
		{
			backups = Integer.parseInt(jEdit.getProperty(
				"backups"));
		}
		catch(NumberFormatException nf)
		{
			Log.log(Log.ERROR,this,nf);
			backups = 1;
		}

		if(backups == 0)
			return;

		String backupPrefix = jEdit.getProperty("backup.prefix","");
		String backupSuffix = jEdit.getProperty("backup.suffix","~");

		// Check for backup.directory property, and create that
		// directory if it doesn't exist
		String backupDirectory = jEdit.getProperty("backup.directory");
		if(backupDirectory == null || backupDirectory.length() == 0)
			backupDirectory = file.getParent();
		else
		{
			backupDirectory = MiscUtilities.constructPath(
				System.getProperty("user.home"),backupDirectory);

			// Perhaps here we would want to guard with
			// a property for parallel backups or not.
			backupDirectory = MiscUtilities.concatPath(
				backupDirectory,file.getParent());

			File dir = new File(backupDirectory);

			if (!dir.exists())
				dir.mkdirs();
		}

		String name = file.getName();

		// If backups is 1, create ~ file
		if(backups == 1)
		{
			file.renameTo(new File(backupDirectory,
				backupPrefix + name + backupSuffix));
		}
		// If backups > 1, move old ~n~ files, create ~1~ file
		else
		{
			new File(backupDirectory,
				backupPrefix + name + backupSuffix
				+ backups + backupSuffix).delete();

			for(int i = backups - 1; i > 0; i--)
			{
				File backup = new File(backupDirectory,
					backupPrefix + name + backupSuffix
					+ i + backupSuffix);

				backup.renameTo(new File(backupDirectory,
					backupPrefix + name + backupSuffix
					+ (i+1) + backupSuffix));
			}

			file.renameTo(new File(backupDirectory,
				backupPrefix + name + backupSuffix
				+ "1" + backupSuffix));
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.21  2000/08/29 07:47:13  sp
 * Improved complete word, type-select in VFS browser, bug fixes
 *
 * Revision 1.20  2000/08/27 02:06:52  sp
 * Filter combo box changed to a text field in VFS browser, passive mode FTP toggle
 *
 * Revision 1.19  2000/08/23 09:51:48  sp
 * Documentation updates, abbrev updates, bug fixes
 *
 * Revision 1.18  2000/08/22 07:25:01  sp
 * Improved abbrevs, bug fixes
 *
 * Revision 1.17  2000/08/20 07:29:31  sp
 * I/O and VFS browser improvements
 *
 * Revision 1.16  2000/08/15 08:07:11  sp
 * A bunch of bug fixes
 *
 * Revision 1.15  2000/08/10 08:30:41  sp
 * VFS browser work, options dialog work, more random tweaks
 *
 * Revision 1.14  2000/08/06 09:44:27  sp
 * VFS browser now has a tree view, rename command
 *
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
 */
