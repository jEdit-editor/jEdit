/*
 * EditServer.java - jEdit server
 * Copyright (C) 1999 Slava Pestov
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

package org.gjt.sp.jedit;

import javax.swing.SwingUtilities;
import java.io.*;
import java.net.*;
import java.util.Random;
import org.gjt.sp.util.Log;

/**
 * @author Slava Pestov
 * @version $Id$
 */
class EditServer extends Thread
{
	EditServer(String portFile)
	{
		super("jEdit server [" + portFile + "]");
		this.portFile = portFile;

		try
		{
			socket = new ServerSocket(0); // Bind to any port
			authKey = Math.abs(new Random().nextInt());

			FileWriter out = new FileWriter(portFile);
			out.write(String.valueOf(socket.getLocalPort()));
			out.write("\n");
			out.write(String.valueOf(authKey));
			out.write("\n");
			out.close();

			Log.log(Log.DEBUG,this,"jEdit server started on port "
				+ socket.getLocalPort());
			Log.log(Log.DEBUG,this,"Authorization key is "
				+ authKey);
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,this,io);
			return;
		}

		start();
	}

	public void run()
	{
		try
		{
			for(;;)
			{
				Socket client = socket.accept();
				Log.log(Log.MESSAGE,this,client + ": connected");

				BufferedReader in = new BufferedReader(
					new InputStreamReader(client.getInputStream()));

				try
				{
					int key = Integer.parseInt(in.readLine());
					if(key != authKey)
					{
						Log.log(Log.ERROR,this,
							client + ": wrong"
							+ " authorization key");
						in.close();
						client.close();
						continue;
					}
				}
				catch(Exception e)
				{
					Log.log(Log.ERROR,this,
							client + ": invalid"
							+ " authorization key");
					in.close();
					client.close();
					continue;
				}

				Log.log(Log.DEBUG,this,client + ": authenticated"
					+ " successfully");
				handleClient(client,in);

				client.close();
			}
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,this,io);
		}
	}

	void stopServer()
	{
		stop();
		new File(portFile).delete();
	}

	// private members
	private String portFile;
	private ServerSocket socket;
	private int authKey;

	// Thread-safe wrapper for jEdit.newView()
	private void TSnewView(final Buffer buffer)
	{
		SwingUtilities.invokeLater(new Runnable() {
			public void run()
			{
				View view = jEdit.newView(jEdit.getFirstView(),
					buffer);
				view.requestFocus();
				view.toFront();
			}
		});
	}

	// Thread-safe wrapper for jEdit.newFile()
	private Buffer TSnewFile()
	{
		final Buffer[] retVal = new Buffer[1];
		try
		{
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run()
				{
					retVal[0] = jEdit.newFile(null);
				}
			});
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,this,e);
		}
		return retVal[0];
	}

	// Thread-safe wrapper for jEdit.openFile()
	private Buffer TSopenFile(final String parent, final String path,
		final boolean readOnly)
	{
		final Buffer[] retVal = new Buffer[1];
		try
		{
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run()
				{
					retVal[0] = jEdit.openFile(null,
						parent,path,readOnly,false);
				}
			});
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,this,e);
		}
		return retVal[0];
	}

	// Thread-safe wrapper for Sessions.loadSession()
	private Buffer TSloadSession(final String session)
	{
		final Buffer[] retVal = new Buffer[1];
		try
		{
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run()
				{
					retVal[0] = Sessions.loadSession(session,false);
				}
			});
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,this,e);
		}
		return retVal[0];
	}

	// Thread-safe wrapper for View.setBuffer()
	private void TSsetBuffer(final View view, final Buffer buffer)
	{
		SwingUtilities.invokeLater(new Runnable() {
			public void run()
			{
				view.setBuffer(buffer);
			}
		});
	}

	private void handleClient(Socket client, BufferedReader in)
		throws IOException
	{
		boolean readOnly = false;
		boolean reuseView = false;
		String parent = null;
		String session = null;
		boolean endOpts = false;

		View view = null;
		Buffer buffer = null;

		String command;
		while((command = in.readLine()) != null)
		{
			if(endOpts)
				buffer = TSopenFile(parent,command,readOnly);
			else
			{
				if(command.equals("--"))
					endOpts = true;
				else if(command.equals("readonly"))
					readOnly = true;
				else if(command.equals("reuseview"))
					reuseView = true;
				else if(command.startsWith("parent="))
					parent = command.substring(7);
				else if(command.startsWith("session="))
					session = command.substring(8);
				else
					Log.log(Log.ERROR,this,client
						+ ": unknown server"
						+ " command: " + command);
			}
		}

		// If nothing was opened, try loading session, then new file
		if(buffer == null && session != null)
			buffer = TSloadSession(session);

		if(buffer == null)
			buffer = TSnewFile();

		// Create new view
		if(reuseView)
		{
			view = jEdit.getFirstView();
			TSsetBuffer(view,buffer);
			view.requestFocus();
			view.toFront();
		}
		else
			TSnewView(buffer);
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.11  2000/04/25 11:00:20  sp
 * FTP VFS hacking, some other stuff
 *
 * Revision 1.10  2000/04/21 05:32:20  sp
 * Focus tweak
 *
 * Revision 1.9  2000/03/20 03:42:55  sp
 * Smoother syntax package, opening an already open file will ask if it should be
 * reloaded, maybe some other changes
 *
 * Revision 1.8  2000/02/27 00:39:50  sp
 * Misc changes
 *
 * Revision 1.7  2000/02/20 03:14:13  sp
 * jEdit.getBrokenPlugins() method
 *
 * Revision 1.6  2000/02/03 04:53:48  sp
 * Bug fixes and small updates
 *
 * Revision 1.5  2000/01/21 00:35:29  sp
 * Various updates
 *
 * Revision 1.4  1999/10/31 07:15:34  sp
 * New logging API, splash screen updates, bug fixes
 *
 * Revision 1.3  1999/10/30 02:44:18  sp
 * Miscallaneous stuffs
 *
 * Revision 1.2  1999/10/04 06:13:52  sp
 * Repeat counts now supported
 *
 * Revision 1.1  1999/10/01 07:31:39  sp
 * RMI server replaced with socket-based server, minor changes
 *
 */
