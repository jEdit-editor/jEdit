/*
 * BrowserIORequest.java - VFS browser I/O request
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

package org.gjt.sp.jedit.browser;

import java.io.*;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.util.WorkRequest;
import org.gjt.sp.util.WorkThread;

/**
 * A browser I/O request.
 * @author Slava Pestov
 * @version $Id$
 */
public class BrowserIORequest extends WorkRequest
{
	/**
	 * Directory listing I/O request.
	 */
	public static final int LIST_DIRECTORY = 0;

	/**
	 * Delete file I/O request.
	 */
	public static final int DELETE = 1;

	/**
	 * Make directory I/O request.
	 */
	public static final int MKDIR = 2;

	/**
	 * Creates a new browser I/O request.
	 * @param type The request type
	 * @param browser The VFS browser instance
	 * @param path The path name to operate on
	 */
	public BrowserIORequest(int type, VFSBrowser browser,
		VFSSession session, VFS vfs, String path)
	{
		this.type = type;
		this.browser = browser;
		this.session = session;
		this.vfs = vfs;
		this.path = path;
	}

	public void run()
	{
		switch(type)
		{
		case LIST_DIRECTORY:
			listDirectory();
			break;
		case DELETE:
			delete();
			break;
		case MKDIR:
			mkdir();
			break;
		}
	}

	// private members
	private int type;
	private VFSBrowser browser;
	private VFSSession session;
	private VFS vfs;
	private String path;

	private void listDirectory()
	{
		VFS.DirectoryEntry[] directory = null;

		try
		{
			setAbortable(true);
			String[] args = { path };
			setStatus(jEdit.getProperty("vfs.status.listing-directory",args));

			try
			{
				directory = vfs._listDirectory(session,path,browser);
			}
			catch(IOException io)
			{
				args[0] = io.getMessage();
				VFSManager.error(browser,"ioerror",args);
			}
		}
		catch(WorkThread.Abort a)
		{
		}
		finally
		{
			try
			{
				vfs._endVFSSession(session,browser);
			}
			catch(IOException io)
			{
				String[] args = { io.getMessage() };
				VFSManager.error(browser,"ioerror",args);
			}
		}

		setAbortable(false);
		browser.directoryLoaded(directory);
	}

	private void delete()
	{
		try
		{
			setAbortable(true);
			String[] args = { path };
			setStatus(jEdit.getProperty("vfs.status.deleting",args));

			try
			{
				if(!vfs._delete(session,path,browser))
					VFSManager.error(browser,"vfs.browser.delete-error",args);
			}
			catch(IOException io)
			{
				args[0] = io.getMessage();
				VFSManager.error(browser,"ioerror",args);
			}
		}
		catch(WorkThread.Abort a)
		{
		}
		finally
		{
			try
			{
				vfs._endVFSSession(session,browser);
			}
			catch(IOException io)
			{
				String[] args = { io.getMessage() };
				VFSManager.error(browser,"ioerror",args);
			}
		}
	}

	private void mkdir()
	{
		try
		{
			setAbortable(true);
			String[] args = { path };
			setStatus(jEdit.getProperty("vfs.status.mkdir",args));

			try
			{
				if(!vfs._mkdir(session,path,browser))
					VFSManager.error(browser,"vfs.browser.mkdir-error",args);
			}
			catch(IOException io)
			{
				args[0] = io.getMessage();
				VFSManager.error(browser,"ioerror",args);
			}
		}
		catch(WorkThread.Abort a)
		{
		}
		finally
		{
			try
			{
				vfs._endVFSSession(session,browser);
			}
			catch(IOException io)
			{
				String[] args = { io.getMessage() };
				VFSManager.error(browser,"ioerror",args);
			}
		}
	}
}

/*
 * Change Log:
 * $Log$
 * Revision 1.2  2000/08/05 07:16:12  sp
 * Global options dialog box updated, VFS browser now supports right-click menus
 *
 * Revision 1.1  2000/07/29 12:24:08  sp
 * More VFS work, VFS browser started
 *
 */
