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

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.io.*;
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

	/**
	 * Creates a new local filesystem.
	 */
	public FileVFS()
	{
		super("file");
	}

	/**
	 * Displays an open dialog box and returns the selected pathname.
	 * @param view The view
	 * @param buffer The buffer
	 */
	public Buffer showOpenDialog(View view, Buffer buffer)
	{
		String parent;
		File file = buffer.getFile();
		if(file == null)
			parent = System.getProperty("user.dir");
		else
			parent = file.getParent();

		String path = GUIUtilities.showFileDialog(view,parent,
			JFileChooser.OPEN_DIALOG);
		if(path != null)
			return jEdit.openFile(view,path);
		else
			return null;
	}

	/**
	 * Displays a save dialog box and returns the selected pathname.
	 * @param view The view
	 * @param buffer The buffer
	 */
	public String showSaveDialog(View view, Buffer buffer)
	{
		String path;
		if(buffer.getFile() == null)
			path = null;
		else
			path = buffer.getPath();

		String file = GUIUtilities.showFileDialog(view,path,
			JFileChooser.SAVE_DIALOG);

		if(file != null)
		{
			File _file = new File(file);
			if(_file.exists())
			{
				Object[] args = { _file.getName() };
				int result = JOptionPane.showConfirmDialog(view,
					jEdit.getProperty("fileexists.message",args),
					jEdit.getProperty("fileexists.title"),
					JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE);
				if(result != JOptionPane.YES_OPTION)
					return null;
			}

			return file;
		}
		else
			return null;
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
		File file = new File(path);

		if(!checkFile(view,buffer,file))
			return false;

		return super.load(view,buffer,path);
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
		File file = new File(path);

		// Check that we can actually write to the file
		if((file.exists() && !file.canWrite())
			|| (!file.exists() && !new File(file.getParent()).canWrite()))
		{
			String[] args = { path };
			GUIUtilities.error(view,"no-write",args);
			return false;
		}

		backup(buffer,file);
		return super.save(view,buffer,path);
	}

	/**
	 * Returns true if this VFS supports file deletion. This is required
	 * for marker saving to work.
	 */
	public boolean _canDelete()
	{
		return true;
	}

	/**
	 * Deletes the specified file.
	 */
	public void _delete(Buffer buffer, String path)
	{
		new File(path).delete();
	}

	/**
	 * Returns the length of the specified file.
	 */
	public long _getFileLength(Buffer buffer, String path)
	{
		return new File(path).length();
	}

	/**
	 * Creates an input stream. This method is called from the I/O
	 * thread.
	 * @param view The view
	 * @param buffer The buffer
	 * @param path The path
	 * @param ignoreErrors If true, file not found errors should be
	 * ignored
	 * @exception IOException If an I/O error occurs
	 */
	public InputStream _createInputStream(View view, Buffer buffer,
		String path, boolean ignoreErrors) throws IOException
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

	/**
	 * Creates an output stream. This method is called from the I/O
	 * thread.
	 * @param view The view
	 * @param buffer The buffer
	 * @param path The path
	 * @exception IOException If an I/O error occurs
	 */
	public OutputStream _createOutputStream(View view, Buffer buffer, String path)
		throws IOException
	{
		return new FileOutputStream(path);
	}

	// private members
	// The BACKED_UP flag prevents more than one backup from being
	// written per session (I guess this should be made configurable
	// in the future)
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

	private boolean checkFile(View view, Buffer buffer, File file)
	{
		if(!file.exists())
		{
			if(buffer != null)
			{
				buffer.setNewFile(true);
				buffer.setDirty(false);
			}
			return false;
		}
		else
			buffer.setReadOnly(!file.canWrite());

		if(file.isDirectory())
		{
			String[] args = { file.getPath() };
			GUIUtilities.error(view,"open-directory",args);
			if(buffer != null)
			{
				buffer.setNewFile(false);
				buffer.setDirty(false);
			}
			return false;
		}

		if(!file.canRead())
		{
			String[] args = { file.getPath() };
			GUIUtilities.error(view,"no-read",args);
			if(buffer != null)
			{
				buffer.setNewFile(false);
				buffer.setDirty(false);
			}
			return false;
		}

		return true;
	}
}
