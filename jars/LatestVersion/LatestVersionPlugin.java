/*
 * LatestVersionPlugin.java - Latest Version Check Plugin
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

import javax.swing.JOptionPane;
import java.util.Vector;
import org.gjt.sp.jedit.event.*;
import org.gjt.sp.jedit.*;

public class LatestVersionPlugin extends EditPlugin
{
	public void start()
	{
		jEdit.addAction(new version_check_settings());
		jEdit.addAction(new version_check_now());
		jEdit.addEditorListener(new EditorHandler());
	}

	public void createMenuItems(View view, Vector menus, Vector menuItems)
	{
		menus.addElement(GUIUtilities.loadMenu(view,"version-check.menu"));
	}

	public static void doVersionCheckConfirm(View view)
	{
		GUIUtilities.hideSplashScreen();

		String[] args = { jEdit.getProperty("version-check.interval"),
			jEdit.getProperty("version-check.url") };

		int result = JOptionPane.showConfirmDialog(view,
			jEdit.getProperty("version-check.confirm.message",args),
			jEdit.getProperty("version-check.confirm.title"),
			JOptionPane.YES_NO_OPTION,
			JOptionPane.INFORMATION_MESSAGE);
		jEdit.setProperty("version-check.enabled",
			(result == JOptionPane.YES_OPTION) ? "yes" : "no");
	}

	public static void doVersionCheck(View view)
	{
		new VersionCheckThread(view).start();
	}

	class EditorHandler extends EditorAdapter
	{
		public void viewCreated(EditorEvent evt)
		{
			jEdit.removeEditorListener(this);

			if(jEdit.getProperty("version-check.enabled") == null)
			{
				doVersionCheckConfirm(evt.getView());
			}
			else if("yes".equals(jEdit.getProperty("version-check.enabled")))
			{
				String lastTimeStr = jEdit.getProperty(
					"version-check.last-time");
				long lastTime;
				if(lastTimeStr == null)
					lastTime = 0L;
				else
					lastTime = Long.parseLong(lastTimeStr);

				long interval = Long.parseLong(jEdit.getProperty(
					"version-check.interval"))
					* 1000 * 60 * 60 * 24;

				long currentTime = System.currentTimeMillis();

				if(lastTime + interval < currentTime)
					doVersionCheck(evt.getView());
			}
		}
	}
}
