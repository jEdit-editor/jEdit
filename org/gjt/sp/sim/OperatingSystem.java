/*
 * OperatingSystem.java - OS specific stuff
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

public abstract class OperatingSystem
{
	public abstract String getInstallDirectory(String name, String version);

	public abstract String getShortcutDirectory();

	public abstract void createScript(SIMInstaller installer,
		InstallThread installThread, String installDir,
		String binDir, String name) throws IOException;

	// On Unix, we call chmod on the resulting directory...
	public void mkdirs(String directory) throws IOException
	{
		new File(directory).mkdirs();
	}

	public static OperatingSystem getOperatingSystem()
	{
		String osName = System.getProperty("os.name");
		if(osName.indexOf("Windows") != -1)
			return new Windows();
		else if(osName.indexOf("Mac") != -1)
			return new MacOS();
		else if(osName.indexOf("OS/2") != -1)
			return new HalfAnOS();
		else
			return new Unix();
	}

	public static class Unix extends OperatingSystem
	{
		public String getInstallDirectory(String name, String version)
		{
			return "/usr/local/share/" + name.toLowerCase()
				+ "-" + version;
		}

		public String getShortcutDirectory()
		{
			return "/usr/local/bin";
		}

		public void mkdirs(String directory) throws IOException
		{
			// make it executable
			String[] mkdirArgs = { "mkdir", "-m", "755",
				"-p", directory };
			try
			{
				Runtime.getRuntime().exec(mkdirArgs).waitFor();
			}
			catch(InterruptedException e)
			{
			}
		}

		public void createScript(SIMInstaller installer,
			InstallThread installThread, String installDir,
			String binDir, String name) throws IOException
		{
			/**
			 * This method copies the simlaunch script to
			 * binDir, then creates a simple script that
			 * starts the app using simlaunch in installDir.
			 * Finally, it symlinks installDir/app to
			 * binDir/app.
			 */

			// copy simlaunch to /usr/local/bin.
			// XXX: need to check if a newer version of simlaunch
			// exists
			String simlaunch = binDir + File.separatorChar
				+ "simlaunch";
			installThread.copy("simlaunch",simlaunch);

			// make it executable
			String[] chmodArgs = { "chmod", "755", simlaunch };
			Runtime.getRuntime().exec(chmodArgs);

			// create app start script
			String script = installDir + File.separatorChar
				+ name.toLowerCase();

			// Delete existing copy
			new File(script).delete();

			// Write simple script
			FileWriter out = new FileWriter(script);
			out.write("#!/bin/sh\n");
			out.write("exec simlaunch ");
			out.write(name.toLowerCase());
			out.write(' ');
			out.write(installer.getProperty("app.main.class"));
			out.write(' ');
			out.write("$@\n");
			out.write("echo Cannot find the simlaunch script."
				+ " make sure its directory is listed in"
				+ " your PATH.\n");
			out.close();

			// Make it executable
			chmodArgs[2] = script;
			Runtime.getRuntime().exec(chmodArgs);

			// Symlink installDir/app to binDir/app
			String linkToScript = binDir + File.separatorChar
				+ name.toLowerCase();

			// Don't symlink if binDir == installDir
			if(!script.equals(linkToScript))
			{
				String[] lnArgs = { "ln", "-sf", script,
					linkToScript };
				Runtime.getRuntime().exec(lnArgs);
			}
		}
	}

	public static class Windows extends OperatingSystem
	{
		public String getInstallDirectory(String name, String version)
		{
			return "C:\\Program Files\\" + name + " " + version;
		}

		public String getShortcutDirectory()
		{
			return "C:\\Windows\\Desktop";
		}

		public void createScript(SIMInstaller installer,
			InstallThread installThread, String installDir,
			String binDir, String name) throws IOException
		{
			/**
			 * This method creates a batch file in binDir
			 * to start the app.
			 */
			FileWriter out = new FileWriter(binDir
				+ File.separatorChar + name + ".bat");
			out.write("@echo off\r\n\"");
			out.write(System.getProperty("java.home"));
			out.write("\\bin\\java\" -classpath \"%CLASSPATH%;");
			out.write(installDir);
			out.write(File.separatorChar);
			out.write(name.toLowerCase());
			out.write(".jar\" ");
			out.write(installer.getProperty("app.main.class"));
			out.write(" %1 %2 %3 %4 %5 %6 %7 %8 %9\r\n");
			out.close();
		}
	}

	public static class MacOS extends OperatingSystem
	{
		public String getInstallDirectory(String name, String version)
		{
			return name + " " + version;
		}

		public String getShortcutDirectory()
		{
			return null;
		}

		public void createScript(SIMInstaller installer,
			InstallThread installThread, String installDir,
			String binDir, String name) throws IOException
		{
		}
	}

	public static class HalfAnOS extends OperatingSystem
	{
		public String getInstallDirectory(String name, String version)
		{
			return "C:\\" + name + " " + version;
		}

		public String getShortcutDirectory()
		{
			return null;
		}

		public void createScript(SIMInstaller installer,
			InstallThread installThread, String installDir,
			String binDir, String name) throws IOException
		{
		}
	}
}
