/*
 * EditServer.java - jEdit server
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2003 Slava Pestov
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

//{{{ Imports
import org.gjt.sp.jedit.bsh.NameSpace;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.Random;
import org.gjt.sp.jedit.io.FileVFS;
import org.gjt.sp.util.Log;
//}}}

/**
 * Inter-process communication.<p>
 *
 * The edit server protocol is very simple.
 * <code><i>jEditSettingsDir</i>/server</code>
 * is an ASCII file containing two lines, the first being the port number,
 * the second being the authorization key.<p>
 *
 * You connect to that port on the local machine, sending the authorization
 * key as four bytes in network byte order, followed by the length of the
 * BeanShell script as two bytes in network byte order, followed by the
 * script in UTF8 encoding. After the socked is closed, the BeanShell script
 * will be executed by jEdit.<p>
 *
 * The snippet is executed in the AWT thread. None of the usual BeanShell
 * variables (view, buffer, textArea, editPane) are set so the script has to
 * figure things out by itself.<p>
 *
 * In most cases, the script will call the static
 * {@link #handleClient(boolean,String,String[])} method, but of course more
 * complicated stuff can be done too.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class EditServer extends Thread
{
	//{{{ EditServer constructor
	EditServer(String portFile)
	{
		super("jEdit server daemon [" + portFile + "]");
		setDaemon(true);
		this.portFile = portFile;
		try
		{
			// On Unix, set permissions of port file to rw-------,
			// so that on broken Unices which give everyone read
			// access to user home dirs, people can't see your
			// port file (and hence send arbitriary BeanShell code
			// your way. Nasty.)
			if(OperatingSystem.isUnix())
			{
				new File(portFile).createNewFile();
				FileVFS.setPermissions(portFile,0600);
			}

			// Bind to any port on localhost; accept 2 simultaneous
			// connection attempts before rejecting connections
			socket = new ServerSocket(0, 2,
				InetAddress.getLoopbackAddress());
			authKey = new Random().nextInt(Integer.MAX_VALUE);
			int port = socket.getLocalPort();

			FileWriter out = new FileWriter(portFile);

			try
			{
				out.write("c\n");
				out.write(String.valueOf(port));
				out.write("\n");
				out.write(String.valueOf(authKey));
				out.write("\n");
			}
			finally
			{
				out.close();
			}

			ok = true;

			Log.log(Log.DEBUG,this,"jEdit server started on port "
				+ socket.getLocalPort());
			Log.log(Log.DEBUG,this,"Authorization key is "
				+ authKey);
		}
		catch(IOException io)
		{
			/* on some Windows versions, connections to localhost
			 * fail if the network is not running. To avoid
			 * confusing newbies with weird error messages, log
			 * errors that occur while starting the server
			 * as NOTICE, not ERROR */
			Log.log(Log.NOTICE,this,io);
		}
	} //}}}

	//{{{ run() method
	@Override
	public void run()
	{
		while (true)
		{
			if(abort)
			{
				stopServer();
				return;
			}

			try
			{
				Socket client = socket.accept();

				// Stop script kiddies from opening the edit
				// server port and just leaving it open, as a
				// DoS
				client.setSoTimeout(1000);

				Log.log(Log.MESSAGE,this,client + ": connected");

				DataOutputStream out = new DataOutputStream(
						client.getOutputStream());
				out.writeInt(authKey);

				DataInputStream in = new DataInputStream(
					client.getInputStream());

				handleClient(client, out, in);
			}
			catch(SocketTimeoutException e)
			{
				if(!abort)
					Log.log(Log.NOTICE,this,e);
			}
			catch(Exception e)
			{
				if(!abort)
					Log.log(Log.ERROR,this,e);
			}
		}
	} //}}}

	//{{{ handleClient() method
	/**
	 * @param restore Ignored unless no views are open
	 * @param parent The client's parent directory
	 * @param args A list of files. Null entries are ignored, for convinience
	 * @since jEdit 3.2pre7
	 */
	public static void handleClient(boolean restore, String parent,
		String[] args)
	{
		handleClient(restore,false,false,parent,args);
	} //}}}

	//{{{ handleClient() method
	/**
	 * @param restore Ignored unless no views are open
	 * @param newView Open a new view?
	 * @param newPlainView Open a new plain view?
	 * @param parent The client's parent directory
	 * @param args A list of files. Null entries are ignored, for convenience
	 * @return the buffer
	 * @since jEdit 4.2pre1
	 */
	public static Buffer handleClient(boolean restore,
		boolean newView, boolean newPlainView, String parent,
		String[] args)
	{
		// we have to deal with a huge range of possible border cases here.
		if(jEdit.getFirstView() == null)
		{
			// coming out of background mode.
			// no views open.
			// no buffers open if args empty.

			boolean hasBufferArgs = false;

			for (String arg : args)
			{
				if (arg != null)
				{
					hasBufferArgs = true;
					break;
				}
			}


			boolean restoreFiles = restore
				&& jEdit.getBooleanProperty("restore")
				&& (!hasBufferArgs
				|| jEdit.getBooleanProperty("restore.cli"));

			View view = PerspectiveManager.loadPerspective(
				restoreFiles);

			Buffer buffer = jEdit.openFiles(view,parent,args);

			if(view == null)
			{
				if(buffer == null)
					buffer = jEdit.getFirstBuffer();
				jEdit.newView(null,buffer);
			}
			else if(buffer != null)
				view.setBuffer(buffer);

			return buffer;
		}
		else if(newPlainView)
		{
			// no background mode, and opening a new view
			Buffer buffer = jEdit.openFiles(null,parent,args);
			if(buffer == null)
				buffer = jEdit.getFirstBuffer();
			jEdit.newView(null,buffer,true);
			return buffer;
		}
		else if(newView)
		{
			// no background mode, and opening a new view
			Buffer buffer = jEdit.openFiles(null,parent,args);
			if(buffer == null)
				buffer = jEdit.getFirstBuffer();
			jEdit.newView(jEdit.getActiveView(),buffer,false);
			return buffer;
		}
		else
		{
			// no background mode, and reusing existing view
			View view = jEdit.getActiveView();
			Buffer buffer = jEdit.openFiles(view,parent,args);
			view.toFront();
			return buffer;
		}
	} //}}}

	//{{{ isOK() method
	boolean isOK()
	{
		return ok;
	} //}}}

	//{{{ getPort method
	public int getPort()
	{
		return socket.getLocalPort();
	} //}}}

	//{{{ stopServer() method
	void stopServer()
	{
		abort = true;
		try
		{
			socket.close();
		}
		catch(IOException io)
		{
		}

		new File(portFile).delete();
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private final String portFile;
	private ServerSocket socket;
	private int authKey;
	private boolean ok;
	private boolean abort;
	//}}}

	//{{{ handleClient() method
	private void handleClient(final Socket client, DataOutputStream out, DataInputStream in)
		throws Exception
	{
		int key = in.readInt();
		if(key != authKey)
		{
			Log.log(Log.ERROR,this,client + ": wrong"
				+ " authorization key (got " + key
				+ ", expected " + authKey + ")");
			in.close();
			out.close();
			client.close();
		}
		else
		{
			// Reset the timeout
			client.setSoTimeout(0);

			Log.log(Log.DEBUG,this,client + ": authenticated"
				+ " successfully");

			final String script = in.readUTF();
			Log.log(Log.DEBUG,this,script);

			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						NameSpace ns = new NameSpace(
							BeanShell.getNameSpace(),
							"EditServer namespace");
						ns.setVariable("socket",client);
						BeanShell.eval(null,ns,script);
					}
					catch(org.gjt.sp.jedit.bsh.UtilEvalError e)
					{
						Log.log(Log.ERROR,this,e);
					}
					finally
					{
						try
						{
							BeanShell.getNameSpace().setVariable("socket",null);
						}
						catch(org.gjt.sp.jedit.bsh.UtilEvalError e)
						{
							Log.log(Log.ERROR,this,e);
						}
					}
				}
			});
		}
	} //}}}

	//}}}
}
