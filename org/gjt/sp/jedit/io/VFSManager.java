/*
 * VFSManager.java - Main class of virtual filesystem
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

import java.util.Enumeration;
import java.util.Hashtable;
import javax.swing.SwingUtilities;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.WorkThread;

/**
 * jEdit's virtual filesystem allows it to transparently edit files
 * stored elsewhere than the local filesystem, for example on an FTP
 * site.
 * @author Slava Pestov
 * @version $Id$
 */
public class VFSManager
{
	public static void start()
	{
		ioThread.start();
	}

	/**
	 * Returns the local filesystem VFS.
	 * @since jEdit 2.5pre1
	 */
	public static VFS getFileVFS()
	{
		return fileVFS;
	}

	/**
	 * Returns the URL VFS.
	 * @since jEdit 2.5pre1
	 */
	public static VFS getUrlVFS()
	{
		return urlVFS;
	}

	/**
	 * Returns the VFS for the specified protocol.
	 * @param protocol The protocol
	 * @since jEdit 2.5pre1
	 */
	public static VFS getVFSForProtocol(String protocol)
	{
		if(protocol.equals("file"))
			return fileVFS;
		else
		{
			VFS vfs = (VFS)vfsHash.get(protocol);
			if(vfs != null)
				return vfs;
			else
				return urlVFS;
		}
	}

	/**
	 * Registers a virtual filesystem.
	 * @param protocol The protocol
	 * @param vfs The VFS
	 * @since jEdit 2.5pre1
	 */
	public static void registerVFS(String protocol, VFS vfs)
	{
		Log.log(Log.DEBUG,VFSManager.class,"Registered "
			+ vfs.getName() + " filesystem for "
			+ protocol + " protocol");
		vfsHash.put(protocol,vfs);
	}

	/**
	 * Unregisters a virtual filesystem.
	 * @param protocol The protocol
	 * @since jEdit 2.5pre1
	 */
	public static void unregisterVFS(String protocol)
	{
		vfsHash.remove(protocol);
	}

	/**
	 * Returns an enumeration of all registered filesystems.
	 * @since jEdit 2.5pre1
	 */
	public static Enumeration getFilesystems()
	{
		return vfsHash.elements();
	}

	/**
	 * Returns when all pending requests are complete.
	 * @since jEdit 2.5pre1
	 */
	public static void waitForRequests()
	{
		ioThread.waitForRequests();
	}

	/**
	 * Returns if the last request caused an error.
	 */
	public static boolean errorOccurred()
	{
		return error;
	}

	/**
	 * Aborts the currently running I/O operation.
	 * @since jEdit 2.5pre1
	 */
	public static void abortIO()
	{
		ioThread.abort();
	}

	/**
	 * Adds an I/O request to the work thread.
	 * @since jEdit 2.5pre1
	 */
	public static void addIORequest(int type, View view, Buffer buffer,
		String path, VFS vfs)
	{
		ioThread.addWorkRequest(new IORequest(type,view,buffer,path,vfs),false);
	}

	/**
	 * Executes the specified runnable in the AWT thread once all
	 * pending I/O requests are complete.
	 * @since jEdit 2.5pre1
	 */
	public static void runInAWTThread(Runnable run)
	{
		ioThread.addWorkRequest(run,true);
	}

	/**
	 * For use by VFS implementations and IO requests. Displays the
	 * specified error in the AWT thread.
	 * @since jEdit 2.5pre1
	 */
	public static void error(final View view, final String error, final Object[] args)
	{
		// if we are already in the AWT thread, take a shortcut
		if(SwingUtilities.isEventDispatchThread())
		{
			GUIUtilities.error(view,error,args);
			return;
		}

		// the 'error' chicanery ensures that stuff like:
		// VFSManager.waitForRequests()
		// if(VFSManager.errorOccurred())
		//         ...
		// will work (because the below runnable will only be
		// executed in the next event)
		VFSManager.error = true;

		runInAWTThread(new Runnable()
		{
			public void run()
			{
				VFSManager.error = false;

				if(view != null && !view.isShowing())
					view = null;
				GUIUtilities.error(view,error,args);
			}
		});
	}

	// private members
	private static WorkThread ioThread = new WorkThread();
	private static VFS fileVFS = new FileVFS();
	private static VFS urlVFS = new UrlVFS();
	private static Hashtable vfsHash;
	private static boolean error;

	static
	{
		vfsHash = new Hashtable();
		registerVFS("ftp",new FtpVFS());
	}

	private VFSManager() {}
}

/*
 * Change Log:
 * $Log$
 * Revision 1.6  2000/04/29 09:17:07  sp
 * VFS updates, various fixes
 *
 * Revision 1.5  2000/04/27 08:32:57  sp
 * VFS fixes, read only fixes, macros can prompt user for input, improved
 * backup directory feature
 *
 * Revision 1.4  2000/04/25 11:00:20  sp
 * FTP VFS hacking, some other stuff
 *
 * Revision 1.3  2000/04/25 03:32:40  sp
 * Even more VFS hacking
 *
 * Revision 1.2  2000/04/24 11:00:23  sp
 * More VFS hacking
 *
 * Revision 1.1  2000/04/24 04:45:37  sp
 * New I/O system started, and a few minor updates
 *
 */
