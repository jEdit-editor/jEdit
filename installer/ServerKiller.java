/*
 * ServerKiller.java - Utility class for the installer
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2009 Eric Le Lay
 *
 * this code is freely adapted from org/gjt/sp/jedit/jEdit.java
 * Copyright (C) 1998, 2005 Slava Pestov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package installer;

import java.io.*;
import java.net.*;

import static installer.OperatingSystem.getOperatingSystem;

/**
 * Utility class to check for a running jEdit server,
 * and stop it.
 * Useful on windows platform, where the jedit.jar archive
 * is locked and can't be overwritten by the installer.
 *
 * NB: The server file must be in the standard location (i.e. $HOME/.jedit/server)
 * for the server to be found.
 * @version	$Id$
 */
public class ServerKiller
{

	/**
	 * try to contact a running instance of jEdit Server
	 * and ask it to close.
	 * @return	true	either if no server was detected, or the server was shut-down,
	 *		false otherwise
	 */
	public static boolean quitjEditServer()
	{

		/* {{{ default server file location */
		File settingsDirectory = getOperatingSystem().getSettingsDirectory();
		File portFile = new File(settingsDirectory,"server");
		/* }}} */

		if(portFile.exists())
		{
			BufferedReader portFileIn = null;
			DataInputStream in = null;
			DataOutputStream out = null;
			try
			{
				portFileIn = new BufferedReader(new FileReader(portFile));
				String protocolVersion = portFileIn.readLine();
				if(!("b".equals(protocolVersion) || "c".equals(protocolVersion)))
				{
					System.out.println("Wrong port file format");
					return false;
				}

				int port = Integer.parseInt(portFileIn.readLine());
				int keyFromPortFile = Integer.parseInt(portFileIn.readLine());

				Socket socket = new Socket(InetAddress.getLoopbackAddress(),port);

				in = new DataInputStream(socket.getInputStream());

				boolean correctService;
				if("b".equals(protocolVersion))
				{
					// with protocol "b" just assume we are talking to a jEdit server
					// later protocols should answer with the auth key to make sure
					// we are connected to a proper jEdit server and not some
					// service that happens to listen on the port in a stale port file
					correctService = true;
				}
				else
				{
					// if the service does not answer at all, timeout after a second
					socket.setSoTimeout(1000);
					int keyByServer = in.readInt();
					if(keyByServer != keyFromPortFile)
					{
						System.out.println("The service" +
								" answering on the server port did not follow" +
								" the right protocol");
						correctService = false;
					}
					else
					{
						correctService = true;
					}
					socket.setSoTimeout(0);
				}

				// the listening service did not answer with the expected response
				// so do not send it further bytes but start a new jEdit instance
				if(correctService)
				{
					out = new DataOutputStream(socket.getOutputStream());
					out.writeInt(keyFromPortFile);

					// we can't close the socket cleanly, because we want
					// to wait for complete exit, and then it's too late.
					// so the socket is closed when the JVM is shut down.
					String script;
					script = "jEdit.exit(null,true);\n";

					out.writeUTF(script);

					// block until its closed
					try
					{
						in.read();
					}
					catch(Exception e)
					{
						//should get an exception !
					}
				}
			}
			catch(FileNotFoundException fnfe)
			{
				//it exists : we checked that earlier !
				throw new AssertionError(fnfe);
			}
			catch(UnknownHostException uhe)
			{
				//localhost doesn't exist ?
				throw new AssertionError(uhe);
			}
			catch(IOException ioe)
			{
				System.out.println("Exception while trying to connect to existing server:");
				ioe.printStackTrace(System.out);
				System.out.println("Don't worry too much !");
				return false; //warn the user
			}
			finally
			{
				closeQuietly(portFileIn);
				closeQuietly(in);
				closeQuietly(out);
			}
		}
		return true;
	}

	private static void closeQuietly(Closeable closeable)
	{
		if(closeable != null)
		{
			try
			{
				if (closeable instanceof Flushable)
				{
					((Flushable)closeable).flush();
				}
			}
			catch (IOException e)
			{
				// ignore
			}
			try
			{
				closeable.close();
			}
			catch (IOException e)
			{
				//ignore
			}
		}
	}

	/**
	 * try to connect to any running server instance and close it.
	 * exit with an error code on failure, but not if no server was found.
	 */
	public static void main(String[] args)
	{
		boolean success = quitjEditServer();
		if(!success)
		{
			System.exit(-1);
		}
	}
}
