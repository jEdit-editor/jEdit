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

			System.out.println("jEdit server started on port "
				+ socket.getLocalPort()
				+ " (port file: " + portFile + ")");
		}
		catch(IOException io)
		{
			System.err.println("Error starting server:");
			io.printStackTrace();
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
				System.out.println(client + ": connected");

				BufferedReader in = new BufferedReader(
					new InputStreamReader(client.getInputStream()));

				try
				{
					int key = Integer.parseInt(in.readLine());
					if(key != authKey)
					{
						System.err.println(client + ": wrong"
							+ " authorization key");
						in.close();
						client.close();
						continue;
					}
				}
				catch(Exception e)
				{
					System.err.println(client + ": invalid"
							+ " authorization key");
					in.close();
					client.close();
					continue;
				}

				System.out.println(client + ": authenticated successfully");
				handleClient(client,in);

				client.close();
			}
		}
		catch(IOException io)
		{
			System.err.println("Error with server:");
			io.printStackTrace();
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
				jEdit.newView(null,buffer);
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
			e.printStackTrace();
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
			e.printStackTrace();
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
			e.printStackTrace();
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
					System.err.println(client + ": unknown server"
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
			view = jEdit.getViews()[0];
			TSsetBuffer(view,buffer);
		}
		else
			TSnewView(buffer);
	}
}

/*
 * ChangeLog:
 * $Log$
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
