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
import java.io.*;
import java.net.*;
import java.util.Hashtable;
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
	public static final String CLIENT_PROPERTY = "FtpVFS__client";
	public static final String USERNAME_PROPERTY = "FtpVFS__username";
	public static final String PASSWORD_PROPERTY = "FtpVFS__password";

	/**
	 * Creates a new FTP virtual filesystem.
	 */
	public FtpVFS()
	{
		super("ftp");
	}

	/**
	 * Displays an open dialog box and returns the selected pathname.
	 * @param view The view
	 * @param buffer The buffer
	 */
	public Buffer showOpenDialog(View view, Buffer buffer)
	{
		FtpBrowser browser = new FtpBrowser(view,buffer,FtpBrowser.OPEN);
		if(!browser.isOK())
			return null;

		Hashtable props = new Hashtable();
		props.put(PASSWORD_PROPERTY,browser.getPassword());
		return jEdit.openFile(view,null,browser.getPath(),false,false,props);
	}

	/**
	 * Displays a save dialog box and returns the selected pathname.
	 * @param view The view
	 * @param buffer The buffer
	 */
	public String showSaveDialog(View view, Buffer buffer)
	{
		FtpBrowser browser = new FtpBrowser(view,buffer,FtpBrowser.SAVE);
		if(!browser.isOK())
			return null;

		buffer.putProperty(PASSWORD_PROPERTY,browser.getPassword());
		return browser.getPath();
	}

	/**
	 * Loads the specified buffer.
	 * @param view The view
	 * @param buffer The buffer
	 * @param path The path
	 */
	public boolean load(View view, Buffer buffer, String path)
	{
		// since files may be loaded at startup, try to hide
		// the splash screen first
		GUIUtilities.hideSplashScreen();

		if(doLogin(view,buffer,path))
			return super.load(view,buffer,path);
		else
			return false;
	}

	/**
	 * Saves the specifies buffer.
	 * @param view The view
	 * @param buffer The buffer
	 * @param path The path
	 */
	public boolean save(View view, Buffer buffer, String path)
	{
		if(doLogin(view,buffer,path))
		{
			VFSManager.addIORequest(IORequest.SAVE,view,buffer,path,this);
			return true;
		}
		else
			return false;
	}

	/**
	 * Returns true if this VFS supports file deletion. This is required
	 * for marker saving to work.
	 */
	public boolean canDelete()
	{
		return true;
	}

	/**
	 * Deletes the specified file.
	 */
	public void delete(Buffer buffer, String path)
	{
		FtpAddress address = new FtpAddress(path);
		FtpClient client = getFtpClient(null,buffer,address,true);
		if(client == null)
			return;

		try
		{
			client.delete(address.path);
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,this,io);
		}
	}

	/**
	 * A buffer has been loaded. This method is called from the I/O
	 * thread.
	 * @param buffer The buffer
	 * @exception IOException If an I/O error occurs
	 */
	public void _loadComplete(Buffer buffer) throws IOException
	{
		FtpClient client = (FtpClient)buffer.getProperty(CLIENT_PROPERTY);
		if(client != null)
			client.logout();

		buffer.getDocumentProperties().remove(CLIENT_PROPERTY);
	}

	/**
	 * A buffer has been saved. This method is called from the I/O
	 * thread.
	 * @param buffer The buffer
	 * @exception IOException If an I/O error occurs
	 */
	public void _saveComplete(Buffer buffer) throws IOException
	{
		FtpClient client = (FtpClient)buffer.getProperty(CLIENT_PROPERTY);
		if(client != null)
			client.logout();

		buffer.getDocumentProperties().remove(CLIENT_PROPERTY);
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
		FtpAddress address = new FtpAddress(path);
		FtpClient client = getFtpClient(view,buffer,address,ignoreErrors);
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
				VFSManager.error(view,"vfs.ftp.download-error",args);
			}
			client.logout();
			buffer.getDocumentProperties().remove(CLIENT_PROPERTY);
			return null;
		}
		else
			return in;
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
		FtpAddress address = new FtpAddress(path);
		FtpClient client = getFtpClient(view,buffer,address,false);
		if(client == null)
			return null;

		client.dataPort();
		OutputStream out = client.storeStream(address.path);
		if(out == null)
		{
			String[] args = { address.host, address.port, address.path,
				client.getResponse().toString() };
			VFSManager.error(view,"vfs.ftp.upload-error",args);
			client.logout();
			buffer.getDocumentProperties().remove(CLIENT_PROPERTY);
			return null;
		}
		else
			return out;
	}

	// package-private members
	static FtpClient createFtpClient(View view, String host, String port,
		String user, String password, boolean ignoreErrors)
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
					VFSManager.error(view,"vfs.ftp.connect-error",args);
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
					VFSManager.error(view,"vfs.ftp.login-error",args);
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
					VFSManager.error(view,"vfs.ftp.login-error",args);
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

			String[] args = { host,port, client.getResponse().toString() };
			VFSManager.error(view,"vfs.ftp.connect-error",args);
			return null;
		}
		catch(IOException io)
		{
			if(ignoreErrors)
				return null;

			String[] args = { io.getMessage() };
			VFSManager.error(view,"ioerror",args);
			return null;
		}
	}

	boolean doLogin(View view, Buffer buffer, String path)
	{
		String savedUser = (String)buffer.getProperty(USERNAME_PROPERTY);
		String savedPassword = (String)buffer.getProperty(PASSWORD_PROPERTY);

		FtpAddress address = new FtpAddress(path);

		if(address.user == null)
			address.user = savedUser;
		if(address.password == null)
			address.password = savedPassword;

		if(address.user == null || address.password == null)
		{
			LoginDialog dialog = new LoginDialog(view,address.host,
				address.user,address.password);
			if(!dialog.isOK())
				return false;

			address.user = dialog.getUser();
			address.password = dialog.getPassword();
		}

		buffer.putProperty(USERNAME_PROPERTY,address.user);
		buffer.putProperty(PASSWORD_PROPERTY,address.password);

		return true;
	}

	// private members
	private FtpClient getFtpClient(View view, Buffer buffer,
		FtpAddress address, boolean ignoreErrors)
	{
		FtpClient client = (FtpClient)buffer.getProperty(CLIENT_PROPERTY);
		if(client == null)
		{
			if(address.user == null)
				address.user = (String)buffer.getProperty(USERNAME_PROPERTY);
			if(address.password == null)
				address.password = (String)buffer.getProperty(PASSWORD_PROPERTY);

			client = createFtpClient(view,address.host,address.port,
				address.user,address.password,ignoreErrors);
			buffer.putProperty(CLIENT_PROPERTY,client);
		}

		return client;
	}
}
