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
import java.awt.Component;
import org.gjt.sp.jedit.gui.LoginDialog;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.WorkThreadPool;

/**
 * jEdit's virtual filesystem allows it to transparently edit files
 * stored elsewhere than the local filesystem, for example on an FTP
 * site.
 * @author Slava Pestov
 * @version $Id$
 */
public class VFSManager
{
	/**
	 * Do not call.
	 */
	public static void start()
	{
		ioThreadPool.start();
	}

	/**
	 * Returns the I/O thread pool.
	 */
	public static WorkThreadPool getIOThreadPool()
	{
		return ioThreadPool;
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
	 * Returns the VFS for the specified name.
	 * @param name The VFS name
	 * @since jEdit 2.6pre2
	 */
	public static VFS getVFSForName(String name)
	{
		return (VFS)vfsHash.get(name);
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
			VFS vfs = (VFS)protocolHash.get(protocol);
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
		vfsHash.put(vfs.getName(),vfs);
		protocolHash.put(protocol,vfs);
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
		ioThreadPool.waitForRequests();
	}

	/**
	 * Returns if the last request caused an error.
	 */
	public static boolean errorOccurred()
	{
		return error;
	}

	/**
	 * Returns the number of pending I/O requests.
	 */
	public static int getRequestCount()
	{
		return ioThreadPool.getRequestCount();
	}

	/**
	 * Executes the specified runnable in the AWT thread once all
	 * pending I/O requests are complete.
	 * @since jEdit 2.5pre1
	 */
	public static void runInAWTThread(Runnable run)
	{
		ioThreadPool.addWorkRequest(run,true);
	}

	/**
	 * Executes the specified runnable in one of the I/O threads.
	 * @since jEdit 2.6pre2
	 */
	public static void runInWorkThread(Runnable run)
	{
		ioThreadPool.addWorkRequest(run,false);
	}

	/**
	 * For use by VFS implementations and IO requests. Displays the
	 * specified error in the AWT thread.
	 * @since jEdit 2.6pre1
	 */
	public static void error(final Component comp, final String error, final Object[] args)
	{
		// if we are already in the AWT thread, take a shortcut
		if(SwingUtilities.isEventDispatchThread())
		{
			GUIUtilities.error(comp,error,args);
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

				if(comp == null || !comp.isShowing())
					GUIUtilities.error(null,error,args);
				else
					GUIUtilities.error(comp,error,args);
			}
		});
	}

	/**
	 * If a password has been saved for the host and user name in the
	 * session, it sets the value of the session's PASSWORD_KEY value
	 * accordingly. Otherwise, a login dialog box is displayed.
	 * @param session The VFS session
	 * @param comp The component that will parent the login dialog box
	 * @return True if everything is ok, false if the user cancelled the
	 * operation
	 * @since jEdit 2.6pre3
	 */
	public static boolean showLoginDialog(VFSSession session, Component comp)
	{
		String host = (String)session.get(VFSSession.HOSTNAME_KEY);
		String user = (String)session.get(VFSSession.USERNAME_KEY);
		String password = (String)session.get(VFSSession.PASSWORD_KEY);

		if(host != null && user != null)
		{
			if(password != null)
				return true;

			password = (String)passwordHash.get(user + "@" + host);
			if(password != null)
			{
				session.put(VFSSession.PASSWORD_KEY,password);
				return true;
			}
		}

		/* since this can be called at startup time,
		 * we need to hide the splash screen. */
		GUIUtilities.hideSplashScreen();

		LoginDialog dialog = new LoginDialog(comp,host,user,password);
		if(!dialog.isOK())
			return false;

		host = dialog.getHost();
		user = dialog.getUser();
		password = dialog.getPassword();

		session.put(VFSSession.HOSTNAME_KEY,host);
		session.put(VFSSession.USERNAME_KEY,user);
		session.put(VFSSession.PASSWORD_KEY,password);

		passwordHash.put(user + "@" + host,password);

		return true;
	}

	/**
	 * Forgets all saved passwords.
	 * @since jEdit 2.6pre3
	 */
	public static void forgetPasswords()
	{
		passwordHash.clear();
	}

	// private members
	private static WorkThreadPool ioThreadPool;
	private static VFS fileVFS = new FileVFS();
	private static VFS urlVFS = new UrlVFS();
	private static Hashtable vfsHash;
	private static Hashtable protocolHash;
	private static Hashtable passwordHash;
	private static boolean error;

	static
	{
		int count;
		try
		{
			count = Integer.parseInt(jEdit.getProperty("ioThreadCount"));
		}
		catch(NumberFormatException nf)
		{
			count = 4;
		}
		ioThreadPool = new WorkThreadPool("jEdit I/O",count);
		vfsHash = new Hashtable();
		protocolHash = new Hashtable();
		passwordHash = new Hashtable();
		registerVFS(FavoritesVFS.PROTOCOL,new FavoritesVFS());
		registerVFS(FileRootsVFS.PROTOCOL,new FileRootsVFS());
		registerVFS(FtpVFS.PROTOCOL,new FtpVFS());
	}

	private VFSManager() {}
}

/*
 * Change Log:
 * $Log$
 * Revision 1.17  2000/08/16 12:14:29  sp
 * Passwords are now saved, bug fixes, documentation updates
 *
 * Revision 1.16  2000/08/03 07:43:42  sp
 * Favorites added to browser, lots of other stuff too
 *
 * Revision 1.15  2000/07/29 12:24:08  sp
 * More VFS work, VFS browser started
 *
 * Revision 1.14  2000/07/26 07:48:45  sp
 * stuff
 *
 * Revision 1.13  2000/07/22 03:27:03  sp
 * threaded I/O improved, autosave rewrite started
 *
 * Revision 1.12  2000/07/21 10:23:49  sp
 * Multiple work threads
 *
 * Revision 1.11  2000/07/19 11:45:18  sp
 * I/O requests can be aborted now
 *
 * Revision 1.10  2000/07/19 08:35:59  sp
 * plugin devel docs updated, minor other changes
 *
 * Revision 1.9  2000/06/12 02:43:29  sp
 * pre6 almost ready
 *
 * Revision 1.8  2000/05/21 06:06:43  sp
 * Documentation updates, shell script mode bug fix, HyperSearch is now a frame
 *
 * Revision 1.7  2000/05/01 11:53:24  sp
 * More icons added to toolbar, minor updates here and there
 *
 * Revision 1.6  2000/04/29 09:17:07  sp
 * VFS updates, various fixes
 *
 * Revision 1.5  2000/04/27 08:32:57  sp
 * VFS fixes, read only fixes, macros can prompt user for input, improved
 * backup directory feature
 *
 */
