/*
 * InstallThread.java - Performs actual installation
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

package org.gjt.sp.sim;

import java.io.*;
import java.util.Vector;

public class InstallThread extends Thread
{
	public InstallThread(SIMInstaller installer, OperatingSystem os,
		Progress progress, String installDir, String binDir,
		int size, Vector components)
	{
		super("SIM install thread");

		this.installer = installer;
		this.os = os;
		this.progress = progress;
		this.installDir = installDir;
		this.binDir = binDir;
		this.size = size;
		this.components = components;

		buf = new byte[32768];
	}

	public void setProgress(Progress progress)
	{
		this.progress = progress;
	}

	public void run()
	{
		progress.setMaximum(size * 1024);

		String filesetDir = installer.getProperty("app.fileset.dir");
		String dataDir = installer.getProperty("app.data.dir");

		try
		{
			for(int i = 0; i < components.size(); i++)
			{
				String comp = (String)components.elementAt(i);
				installPackage(comp,filesetDir,dataDir);

				if(Thread.interrupted())
				{
					progress.aborted();
					return;
				}
			}

			// create it in case it doesn't already exist
			if(binDir != null)
				os.mkdirs(binDir);

			os.createScript(installer,this,installDir,binDir,
				installer.getProperty("app.name"));
		}
		catch(IOException io)
		{
			progress.error(io.toString());
			return;
		}

		progress.done();
	}

	public void copy(String infile, String outfile)
		throws IOException
	{
		InputStream _in = getClass().getResourceAsStream(infile);

		if(_in == null)
		{
			throw new FileNotFoundException("File not found"
				+ " in JAR: " + infile);
		}

		BufferedInputStream in = new BufferedInputStream(_in);

		File outFile = new File(outfile);

		// XXX: this will create problems when we allow directories
		// to be part of file sets
		os.mkdirs(outFile.getParent());

		BufferedOutputStream out = new BufferedOutputStream(
			new FileOutputStream(outFile));

		int count;

		for(;;)
		{
			count = in.read(buf,0,buf.length);
			if(count == -1)
				break;

			out.write(buf,0,count);
			progress.advance(count);

			if(Thread.interrupted())
			{
				in.close();
				out.close();

				progress.aborted();
				return;
			}
		}

		in.close();
		out.close();
	}

	// private members
	private SIMInstaller installer;
	private OperatingSystem os;
	private Progress progress;
	private String installDir;
	private String binDir;
	private int size;
	private Vector components;
	private byte[] buf;

	private void installPackage(String comp, String filesetDir, String dataDir)
		throws IOException
	{
		BufferedReader in = new BufferedReader(new InputStreamReader(
			getClass().getResourceAsStream(filesetDir + "/" + comp)));

		String path;
		while((path = in.readLine()) != null)
		{
			String infile = dataDir + "/" + path;
			String outfile = installDir + File.separatorChar
				+ path.replace('/',File.separatorChar);
			copy(infile,outfile);
		}

		in.close();
	}
}
