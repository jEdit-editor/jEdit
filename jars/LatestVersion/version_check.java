/*
 * version_check.java
 * Copyright (C) 1999, 2000 Slava Pestov
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

import java.awt.event.ActionEvent;
import java.io.*;
import java.net.*;
import javax.swing.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class version_check extends EditAction
{
	public version_check()
	{
		super("version-check");
	}

	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		view.showWaitCursor();

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
					newVersionAvailable(view,version,url);
				}
				else
				{
					GUIUtilities.message(view,"version-check.up-to-date",
						new String[0]);
				}
			}
		}
		catch(IOException e)
		{
			String[] args = { e.getMessage() };
			GUIUtilities.error(view,"ioerror",args);
		}

		view.hideWaitCursor();
	}

	public void newVersionAvailable(View view, String version, URL url)
	{
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
