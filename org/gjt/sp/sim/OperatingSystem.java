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

	public void createScript(SIMInstaller installer, String installDir,
		String binDir, String name) throws IOException {}

	public void mkdirs(String directory) throws IOException
	{
		File file = new File(directory);
		if(!file.exists())
			file.mkdirs();
	}

	public static OperatingSystem getOperatingSystem()
	{
		if(os != null)
			return os;

		String osName = System.getProperty("os.name");
		if(osName.indexOf("Windows") != -1)
			os = new Windows();
		else if(osName.indexOf("Mac") != -1)
			os = new MacOS();
		else if(osName.indexOf("OS/2") != -1)
			os = new HalfAnOS();
		else
			os = new Unix();

		return os;
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

		public void createScript(SIMInstaller installer,
			String installDir, String binDir, String name)
			throws IOException
		{
			// create app start script
			String script = binDir + File.separatorChar
				+ name.toLowerCase();

			// Delete existing copy
			new File(script).delete();

			// Write simple script
			FileWriter out = new FileWriter(script);
			out.write("#!/bin/sh\n");
			out.write("exec ${JAVA-java} ${" + name.toUpperCase()
				+ "} -classpath \"${CLASSPATH}:"
				+ installDir + File.separator
				+ name.toLowerCase() + ".jar\" "
				+ installer.getProperty("app.main.class")
				+ " $@\n");
			out.write("echo Cannot find ${JAVA-java}."
				+ " make sure its directory appears in"
				+ " your PATH environment variable.\n");
			out.close();

			// Make it executable
			String[] chmodArgs = { "chmod", "755", script };
			exec(chmodArgs);
		}

		public void mkdirs(String directory) throws IOException
		{
			File file = new File(directory);
			if(!file.exists())
			{
				String[] mkdirArgs = { "mkdir", "-m", "755",
					"-p", directory };
				exec(mkdirArgs);
			}
		}

		public void exec(String[] args) throws IOException
		{
			Process proc = Runtime.getRuntime().exec(args);
			proc.getInputStream().close();
			proc.getOutputStream().close();
			proc.getErrorStream().close();
			try
			{
				proc.waitFor();
			}
			catch(InterruptedException ie)
			{
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
			return "C:\\Windows";
		}

		public void createScript(SIMInstaller installer,
			String installDir, String binDir, String name)
			throws IOException
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
	}

	// private members
	private static OperatingSystem os;
}
