/*
 * FtpVFS.java - Ftp VFS
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

import com.fooware.net.*;
import java.awt.Component;
import java.io.*;
import java.net.*;
import java.util.*;
import org.gjt.sp.jedit.gui.LoginDialog;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

/**
 * FTP VFS.
 * @author Slava Pestov
 * @version $Id$
 */
public class FtpVFS extends VFS
{
	public static final String PROTOCOL = "ftp";

	public static final String CLIENT_KEY = "FtpVFS__client";
	public static final String HOSTNAME_KEY = "FtpVFS__hostname";
	public static final String USERNAME_KEY = "FtpVFS__username";
	public static final String PASSWORD_KEY = "FtpVFS__password";

	public FtpVFS()
	{
		super("ftp");
	}

	public int getCapabilities()
	{
		return READ_CAP | WRITE_CAP | BROWSE_CAP | DELETE_CAP
			| RENAME_CAP | MKDIR_CAP;
	}

	public String showBrowseDialog(VFSSession session, Component comp)
	{
		LoginDialog dialog = new LoginDialog(comp,null,null,null);
		if(!dialog.isOK())
			return null;

		session.put(HOSTNAME_KEY,dialog.getHost());
		//session.put(USERNAME_KEY,dialog.getUser());
		session.put(PASSWORD_KEY,dialog.getPassword());

		return PROTOCOL + "://" + dialog.getUser() + "@"
			+ dialog.getHost() + "/";
	}

	public String getFileParent(String path)
	{
		FtpAddress address = new FtpAddress(path);
		address.path = MiscUtilities.getFileParent(address.path);
		return address.toString();
	}

	public String constructPath(String parent, String path)
	{
		if(path.startsWith("/"))
		{
			FtpAddress address = new FtpAddress(parent);
			address.path = path;
			return address.toString();
		}
		else if(parent.endsWith("/"))
			return parent + path;
		else
			return parent + '/' + path;
	}

	public boolean setupVFSSession(VFSSession session, Component comp)
	{
		super.setupVFSSession(session,comp);

		String path = (String)session.get(VFSSession.PATH_KEY);
		String savedHost = (String)session.get(HOSTNAME_KEY);
		String savedUser = (String)session.get(USERNAME_KEY);
		String savedPassword = (String)session.get(PASSWORD_KEY);

		try
		{
			FtpAddress address = new FtpAddress(path);

			if(!address.host.equals(savedHost))
			{
				// erase saved username and password if
				// the user switches to a different host
				savedUser = savedPassword = null;
			}

			if(address.user == null)
				address.user = savedUser;
			if(address.password == null)
				address.password = savedPassword;

			if(address.user == null || address.password == null)
			{
				/* since this can be called at startup time,
				 * we need to hide the splash screen. */
				GUIUtilities.hideSplashScreen();

				LoginDialog dialog = new LoginDialog(comp,address.host,
					address.user,address.password);
				if(!dialog.isOK())
					return false;

				address.user = dialog.getUser();
				address.password = dialog.getPassword();
			}

			session.put(HOSTNAME_KEY,address.host);
			session.put(USERNAME_KEY,address.user);
			session.put(PASSWORD_KEY,address.password);

			return true;
		}
		catch(IllegalArgumentException ia)
		{
			// FtpAddress.<init> can throw this
			return false;
		}
	}

	public VFS.DirectoryEntry[] _listDirectory(VFSSession session, String url,
		Component comp) throws IOException
	{
		// add trailing '/' if necessary, so that foo and foo/ aren't
		// represented differently in cache
		if(!url.endsWith("/"))
			url = url + "/";

		VFS.DirectoryEntry[] directory = DirectoryCache.getCachedDirectory(url);
		if(directory != null)
			return directory;

		FtpAddress address = new FtpAddress(url);
		FtpClient client = _getFtpClient(session,address,false,comp);
		if(client == null)
			return null;

		BufferedReader in = null;
		Vector directoryVector = new Vector();
		try
		{
			client.dataPort();
			Reader _in = client.list(address.path);
			if(_in == null)
			{
				String[] args = { url, client.getResponse().toString() };
				VFSManager.error(comp,"vfs.ftp.list-error",args);
				return null;
			}

			in = new BufferedReader(_in);
			String line;
			while((line = in.readLine()) != null)
			{
				if(line.startsWith("total"))
					continue;

				VFS.DirectoryEntry entry = lineToDirectoryEntry(line);
				if(entry.name.equals(".") || entry.name.equals(".."))
					continue;

				directoryVector.addElement(entry);
			}

			in.close();

			for(int i = 0; i < directoryVector.size(); i++)
			{
				VFS.DirectoryEntry entry = (VFS.DirectoryEntry)
					directoryVector.elementAt(i);
				if(entry.type == __LINK)
					resolveSymlink(session,url,entry,comp);
				else
				{
					// prepend directory to create full path
					entry.path = constructPath(url,entry.name);
					entry.deletePath = entry.path;
				}
			}

			directory = new VFS.DirectoryEntry[directoryVector.size()];
			directoryVector.copyInto(directory);
			DirectoryCache.setCachedDirectory(url,directory);
			return directory;
		}
		finally
		{
			if(in != null)
			{
				try
				{
					in.close();
				}
				catch(Exception e)
				{
					Log.log(Log.ERROR,this,e);
				}
			}
		}
	}

	public boolean _delete(VFSSession session, String url, Component comp)
		throws IOException
	{
		FtpAddress address = new FtpAddress(url);
		FtpClient client = _getFtpClient(session,address,true,comp);
		if(client == null)
			return false;

		VFS.DirectoryEntry directoryEntry = _getDirectoryEntry(
			session,url,comp);
		if(directoryEntry.type == VFS.DirectoryEntry.FILE)
			client.delete(address.path);
		else if(directoryEntry.type == VFS.DirectoryEntry.DIRECTORY)
		{
			client.removeDirectory(address.path);
			DirectoryCache.flushCachedDirectory(url);
		}

		DirectoryCache.flushCachedDirectory(getFileParent(url));

		return client.getResponse().isPositiveCompletion();
	}

	public boolean _rename(VFSSession session, String from, String to,
		Component comp) throws IOException
	{
		FtpAddress address = new FtpAddress(from);
		FtpClient client = _getFtpClient(session,address,true,comp);
		if(client == null)
			return false;

		VFS.DirectoryEntry directoryEntry = _getDirectoryEntry(
			session,from,comp);

		client.renameFrom(address.path);
		client.renameTo(new FtpAddress(to).path);

		if(directoryEntry.type == VFS.DirectoryEntry.DIRECTORY)
			DirectoryCache.flushCachedDirectory(from);

		DirectoryCache.flushCachedDirectory(getFileParent(from));

		return client.getResponse().isPositiveCompletion();
	}

	public boolean _mkdir(VFSSession session, String directory, Component comp)
		throws IOException
	{
		FtpAddress address = new FtpAddress(directory);
		FtpClient client = _getFtpClient(session,address,true,comp);
		if(client == null)
			return false;

		client.makeDirectory(address.path);

		DirectoryCache.flushCachedDirectory(getFileParent(directory));

		return client.getResponse().isPositiveCompletion();
	}

	// this method is severely broken, and in many cases, most fields
	// of the returned directory entry will not be filled in.
	public VFS.DirectoryEntry _getDirectoryEntry(VFSSession session, String path,
		Component comp)
		throws IOException
	{
		FtpAddress address = new FtpAddress(path);
		FtpClient client = _getFtpClient(session,address,true,comp);
		if(client == null)
			return null;

		client.dataPort();
		Reader _reader = client.list(address.path);
		if(_reader == null)
		{
			// eg, file not found
			return null;
		}

		BufferedReader reader = new BufferedReader(_reader);
		String line = reader.readLine();
		reader.close();
		if(line != null)
		{
			if(line.startsWith("total"))
			{
				// ok, this really sucks.
				// we were asked to get the directory
				// entry for a directory. This stupid
				// implementation will only work for
				// the resolveSymlink() method. A proper
				// version will be written some other time.
				return new VFS.DirectoryEntry(null,null,null,
					VFS.DirectoryEntry.DIRECTORY,0L,false);
			}
			else
			{
				VFS.DirectoryEntry dirEntry = lineToDirectoryEntry(line);
				if(dirEntry.type == __LINK)
					resolveSymlink(session,getFileParent(path),
						dirEntry,comp);
				return dirEntry;
			}
		}
		else
			return null;
	}

	public InputStream _createInputStream(VFSSession session, String path,
		boolean ignoreErrors, Component comp) throws IOException
	{
		FtpAddress address = new FtpAddress(path);
		FtpClient client = _getFtpClient(session,address,ignoreErrors,comp);
		if(client == null)
			return null;

		client.dataPort();
		InputStream in = client.retrieveStream(address.path);

		if(in == null)
		{
			if(!ignoreErrors)
			{
				String[] args = { address.host, address.port, address.path,
					client.getResponse().toString() };
				VFSManager.error(comp,"vfs.ftp.download-error",args);
			}
		}

		return in;
	}

	public OutputStream _createOutputStream(VFSSession session, String path,
		Component comp) throws IOException
	{
		FtpAddress address = new FtpAddress(path);
		FtpClient client = _getFtpClient(session,address,false,comp);
		if(client == null)
			return null;

		client.dataPort();
		OutputStream out = client.storeStream(address.path);

		if(out == null)
		{
			String[] args = { address.host, address.port, address.path,
				client.getResponse().toString() };
			VFSManager.error(comp,"vfs.ftp.upload-error",args);
		}

		DirectoryCache.flushCachedDirectory(getFileParent(path));

		return out;
	}

	public void _endVFSSession(VFSSession session, Component comp)
		throws IOException
	{
		try
		{
			FtpClient client = (FtpClient)session.get(CLIENT_KEY);
			if(client != null)
				client.logout();
		}
		finally
		{
			// even if we are aborted...
			session.remove(CLIENT_KEY);
		}

		super._endVFSSession(session,comp);
	}

	// private members
	private static final int __LINK = 10;

	private FtpClient _getFtpClient(VFSSession session, FtpAddress address,
		boolean ignoreErrors, Component comp)
	{
		FtpClient client = (FtpClient)session.get(CLIENT_KEY);
		if(client == null)
		{
			if(address.user == null)
				address.user = (String)session.get(USERNAME_KEY);
			if(address.password == null)
				address.password = (String)session.get(PASSWORD_KEY);

			client = _createFtpClient(address.host,address.port,
				address.user,address.password,ignoreErrors,comp);
			if(client != null)
				session.put(CLIENT_KEY,client);
		}

		return client;
	}

	private static FtpClient _createFtpClient(String host, String port,
		String user, String password, boolean ignoreErrors,
		Component comp)
	{
		FtpClient client = new FtpClient();

		try
		{
			Log.log(Log.DEBUG,FtpVFS.class,"Connecting to " + host + ":" + port);
			client.connect(host,Integer.parseInt(port));
			if(!client.getResponse().isPositiveCompletion())
			{
				if(!ignoreErrors)
				{
					String[] args = { host,port, client.getResponse().toString() };
					VFSManager.error(comp,"vfs.ftp.connect-error",args);
				}
				return null;
			}

			client.userName(user);
			if(!client.getResponse().isPositiveIntermediary())
			{
				if(!ignoreErrors)
				{
					String[] args = { host, port, user,
						client.getResponse().toString() };
					VFSManager.error(comp,"vfs.ftp.login-error",args);
				}
				client.logout();
				return null;
			}

			client.password(password);
			if(!client.getResponse().isPositiveCompletion())
			{
				if(!ignoreErrors)
				{
					String[] args = { host, port, user,
						client.getResponse().toString() };
					VFSManager.error(comp,"vfs.ftp.login-error",args);
				}
				client.logout();
				return null;
			}

			client.representationType(FtpClient.IMAGE_TYPE);

			return client;
		}
		catch(SocketException se)
		{
			if(ignoreErrors)
				return null;

			String response;
			if(client != null)
				response = client.getResponse().toString();
			else
				response = "";
			String[] args = { host, port, response };
			VFSManager.error(comp,"vfs.ftp.connect-error",args);
			return null;
		}
		catch(IOException io)
		{
			if(ignoreErrors)
				return null;

			String[] args = { io.getMessage() };
			VFSManager.error(comp,"ioerror",args);
			return null;
		}
	}

	// Convert a line of LIST output to a VFS.DirectoryEntry
	private VFS.DirectoryEntry lineToDirectoryEntry(String line)
	{
		int type;
		switch(line.charAt(0))
		{
		case 'd':
			type = VFS.DirectoryEntry.DIRECTORY;
			break;
		case 'l':
			type = __LINK;
			break;
		default:
			type = VFS.DirectoryEntry.FILE;
			break;
		}

		// first, extract the fifth field, which is the file size
		int i;
		int j = 0;
		boolean lastWasSpace = false;
		int fieldCount = 0;

		long length = 0L;
		String name = null;

		for(i = 0; i < line.length(); i++)
		{
			if(line.charAt(i) == ' ')
			{
				lastWasSpace = true;
			}
			else
			{
				if(lastWasSpace)
				{
					fieldCount++;

					if(fieldCount == 4)
						j = i;
					else if(fieldCount == 5)
					{
						length = Long.parseLong(
							line.substring(j,i).trim());
					}
					else if(fieldCount == 8)
					{
						name = line.substring(i);
						break;
					}
				}

				lastWasSpace = false;
			}
		}

		// path is null; it will be created later, by _listDirectory()
		return new VFS.DirectoryEntry(name,null,null,type,length,
			name.charAt(0) == '.' /* isHidden */);
	}

	private void resolveSymlink(VFSSession session, String dir,
		VFS.DirectoryEntry entry, Component comp) throws IOException
	{
		String name = entry.name;
		int index = name.indexOf(" -> ");
		String link = name.substring(index + " -> ".length());
		link = constructPath(dir,link);
		VFS.DirectoryEntry linkDirEntry = _getDirectoryEntry(
			session,link,comp);
		if(linkDirEntry == null)
			entry.type = VFS.DirectoryEntry.FILE;
		else
			entry.type = linkDirEntry.type;

		if(entry.type == __LINK)
		{
			// this link links to a link. Don't bother
			// resolving any futher, otherwise we will
			// have to handle symlinks loops, etc, and it
			// will just complicate the code.
			entry.type = VFS.DirectoryEntry.FILE;
		}

		entry.name = name.substring(0,index);
		entry.path = link;
		entry.deletePath = constructPath(dir,entry.name);
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.19  2000/08/16 08:47:19  sp
 * Stuff
 *
 * Revision 1.18  2000/08/10 08:30:41  sp
 * VFS browser work, options dialog work, more random tweaks
 *
 * Revision 1.17  2000/08/06 09:44:27  sp
 * VFS browser now has a tree view, rename command
 *
 * Revision 1.16  2000/08/05 07:16:12  sp
 * Global options dialog box updated, VFS browser now supports right-click menus
 *
 * Revision 1.15  2000/08/03 07:43:42  sp
 * Favorites added to browser, lots of other stuff too
 *
 * Revision 1.14  2000/08/01 11:44:15  sp
 * More VFS browser work
 *
 * Revision 1.13  2000/07/31 11:32:09  sp
 * VFS file chooser is now in a minimally usable state
 *
 * Revision 1.12  2000/07/30 09:04:19  sp
 * More VFS browser hacking
 *
 * Revision 1.11  2000/07/29 12:24:08  sp
 * More VFS work, VFS browser started
 *
 */
