/*
 * PluginDownloadThread.java - Plugin download and install thread
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

import java.io.*;
import java.net.*;
import java.util.Enumeration;
import java.util.zip.*;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class PluginDownloadThread extends Thread
{
	public PluginDownloadThread(PluginDownloadProgress progress,
		String[] urls, String[] dirs)
	{
		super("Plugin downloader");

		this.progress = progress;
		this.urls = urls;
		this.dirs = dirs;

		start();
	}

	public void run()
	{
		for(int i = 0; i < urls.length; i++)
		{
			try
			{
				String url = urls[i];
				String fileName = MiscUtilities.getFileName(url);
				progress.downloading(fileName);
				String path = download(fileName,url);

				progress.installing(fileName);
				install(path,dirs[i]);

				progress.done(true);
			}
			catch(IOException io)
			{
				Log.log(Log.ERROR,this,io);

				String[] args = { io.getMessage() };
				VFSManager.error(progress,"ioerror",args);

				progress.done(false);
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,this,e);

				progress.done(false);
			}
		}
	}

	// private members
	private PluginDownloadProgress progress;
	private String[] urls;
	private String[] dirs;

	private String download(String fileName, String url) throws Exception
	{
		URLConnection conn = new URL(url).openConnection();
		progress.setMaximum(Math.max(0,conn.getContentLength()));

		String path = MiscUtilities.constructPath(PluginManagerPlugin
			.getDownloadDir(),fileName);

		copy(conn.getInputStream(),new FileOutputStream(path),true,true);

		return path;
	}

	private void install(String path, String dir) throws Exception
	{
		progress.setMaximum(1);

		ZipFile zipFile = new ZipFile(path);
		Enumeration enum = zipFile.entries();
		while(enum.hasMoreElements())
		{
			ZipEntry entry = (ZipEntry)enum.nextElement();
			String name = entry.getName().replace('/',File.separatorChar);
			File file = new File(dir,name);
			if(entry.isDirectory())
				file.mkdirs();
			else
			{
				new File(file.getParent()).mkdirs();
				copy(zipFile.getInputStream(entry),
					new FileOutputStream(file),false,false);
			}
		}

		new File(path).delete();

		progress.setValue(1);
	}

	private void copy(InputStream in, OutputStream out,
		boolean canStop, boolean doProgress) throws Exception
	{
		in = new BufferedInputStream(in);
		out = new BufferedOutputStream(out);

		byte[] buf = new byte[4096];
		int copied = 0;
		int count;
		while((count = in.read(buf,0,buf.length)) != -1)
		{
			out.write(buf,0,count);
			if(doProgress)
			{
				copied += count;
				progress.setValue(copied);
			}

			if(canStop && isInterrupted())
			{
				in.close();
				out.close();
				progress.done(false);
				stop();
			}
		}

		in.close();
		out.close();
	}
}
