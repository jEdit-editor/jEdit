/*
 * VersionCheckThread.java - Latest Version Check Thread
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

import javax.swing.*;
import java.io.*;
import java.net.URL;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class VersionCheckThread extends Thread
{
	public VersionCheckThread(View view)
	{
		this.view = view;
	}

	public void run()
	{
		try
		{
			URL url = new URL(jEdit.getProperty("version-check.url"));
			InputStream in = url.openStream();
			BufferedReader bin = new BufferedReader(
				new InputStreamReader(in));

			String line;
			String version = null;
			String build = null;
			while((line = bin.readLine()) != null)
			{
				if(line.startsWith(".version"))
					version = line.substring(8).trim();
				else if(line.startsWith(".build"))
					build = line.substring(6).trim();
			}

			bin.close();

			if(version != null && build != null)
			{
				Log.log(Log.NOTICE,this,"Latest version is "
					+ version);
				String lastVersion = jEdit.getProperty(
					"version-check.last-version");
				if(jEdit.getBuild().compareTo(build) < 0 &&
					(lastVersion == null ||
					lastVersion.compareTo(build) < 0))
				{
					jEdit.setProperty("version-check.last-version",
						build);
					SwingUtilities.invokeLater(
						new NewVersionAvailable(version,url));
				}
			}

			jEdit.setProperty("version-check.last-time",String.valueOf(
				System.currentTimeMillis()));
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,this,e);
		}
	}

	// private members
	private View view;

	class NewVersionAvailable implements Runnable
	{
		String version;
		URL url;

		NewVersionAvailable(String version, URL url)
		{
			this.version = version;
			this.url = url;
		}

		public void run()
		{
			GUIUtilities.hideSplashScreen();

			String[] args = { version };

			int result = JOptionPane.showConfirmDialog(view,
				jEdit.getProperty("version-check.new-version.message",args),
				jEdit.getProperty("version-check.new-version.title"),
				JOptionPane.YES_NO_OPTION,
				JOptionPane.INFORMATION_MESSAGE);

			if(result == JOptionPane.YES_OPTION)
				jEdit.openFile(view,null,url.toString(),true,false);
		}
	}
}
