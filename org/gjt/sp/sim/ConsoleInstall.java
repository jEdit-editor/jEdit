/*
 * ConsoleInstall.java - Text-only installer
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

public class ConsoleInstall
{
	public ConsoleInstall()
	{
		installer = new SIMInstaller();

		OperatingSystem os = OperatingSystem.getOperatingSystem();

		String appName = installer.getProperty("app.name");
		String appVersion = installer.getProperty("app.version");

		BufferedReader in = new BufferedReader(new InputStreamReader(
			System.in));

		System.out.println("*** SIM - installing " + appName);

		String installDir = os.getInstallDirectory(appName,appVersion);

		System.out.print("Installation directory [" + installDir + "]: ");
		System.out.flush();

		String _installDir = readLine(in);
		if(_installDir.length() != 0)
			installDir = _installDir;

		String binDir = os.getShortcutDirectory();

		if(binDir != null)
		{
			System.out.print("Shortcut directory [" + binDir + "]: ");
			System.out.flush();

			String _binDir = readLine(in);
			if(_binDir.length() != 0)
				binDir = _binDir;
		}

		int userCompCount = installer.getIntProperty("comp.user.count");
		int develCompCount = installer.getIntProperty("comp.devel.count");
		Vector components = new Vector(userCompCount + develCompCount);

		System.out.println("*** User components");
		for(int i = 0; i < userCompCount; i++)
		{
			String fileset = installer.getProperty("comp.user." + i + ".fileset");

			System.out.print("Install "
				+ installer.getProperty("comp.user." + i + ".name")
				+ " ("
				+ installer.getProperty("comp.user." + i + ".size")
				+ "Kb) [Y/n]? ");

			String line = readLine(in);
			if(line.length() == 0 || line.charAt(0) == 'y'
				|| line.charAt(0) == 'Y')
				components.addElement(fileset);
		}

		if(develCompCount != 0)
			System.out.println("*** Developer components");

		for(int i = 0; i < develCompCount; i++)
		{
			String fileset = installer.getProperty("comp.devel." + i + ".fileset");

			System.out.print("Install "
				+ installer.getProperty("comp.devel." + i + ".name")
				+ " ("
				+ installer.getProperty("comp.devel." + i + ".size")
				+ "Kb) [Y/n]? ");

			String line = readLine(in);
			if(line.length() != 0 && (line.charAt(0) != 'n'
				&& line.charAt(0) != 'N'))
				components.addElement(fileset);
		}

		System.out.println("*** Starting installation...");
		ConsoleProgress progress = new ConsoleProgress();
		InstallThread thread = new InstallThread(
			installer,os,progress,
			installDir,binDir,
			0 /* XXX */,components);
		thread.start();
	}

	// private members
	private SIMInstaller installer;

	private String readLine(BufferedReader in)
	{
		try
		{
			String line = in.readLine();
			if(line == null)
			{
				System.err.println("\nEOF in input!");
				System.exit(1);
				// can't happen
				throw new InternalError();
			}
			return line;
		}
		catch(IOException io)
		{
			System.err.println("\nI/O error: " + io);
			System.exit(1);
			// can't happen
			throw new InternalError();
		}
	}
}
