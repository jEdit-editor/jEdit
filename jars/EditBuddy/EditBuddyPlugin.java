/*
 * EditBuddyPlugin.java - EditBuddy plugin
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

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import org.gjt.sp.jedit.msg.ViewUpdate;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class EditBuddyPlugin extends EBPlugin
{
	public void start()
	{
		// use plugin manager's last-version property
		// for backwards compatibility
		lastBuild = jEdit.getProperty("update-plugins.last-version");

		// reset toolbar when upgrading from 2.6pre6 to
		// avoid confusion
		if(lastBuild != null && lastBuild.compareTo("02.06.06.00") <= 0)
			resetToolBar();
	}

	public void createMenuItems(Vector menuItems)
	{
		menuItems.addElement(GUIUtilities.loadMenu("edit-buddy"));
	}

	public void handleMessage(EBMessage msg)
	{
		if(msg instanceof ViewUpdate)
		{
			ViewUpdate vmsg = (ViewUpdate)msg;
			if(vmsg.getWhat() == ViewUpdate.CREATED)
			{
				final View view = vmsg.getView();
				view.addWindowListener(new WindowAdapter()
				{
					public void windowOpened(WindowEvent evt)
					{
						if(lastBuild == null)
							welcome(view);
						else if(jEdit.getBooleanProperty("tip.show"))
							tipOfTheDay(view);

						jEdit.setProperty("update-plugins.last-version",jEdit.getBuild());

						EditBus.removeFromBus(EditBuddyPlugin.this);
						view.removeWindowListener(this);
					}
				});
			}
		}
	}

	public static void welcome(View view)
	{
		new Welcome(view);
	}

	public static void tipOfTheDay(View view)
	{
		new TipOfTheDay(view);
	}

	// private members
	private static String lastBuild;

	private static void resetToolBar()
	{
		Log.log(Log.WARNING,EditBuddyPlugin.class,"Upgrading from jEdit"
			+ " 2.6pre6 or earlier; resetting toolbar to defaults");

		String toolbar = jEdit.getProperty("view.toolbar");
		StringTokenizer st = new StringTokenizer(toolbar);
		while(st.hasMoreTokens())
		{
			String action = st.nextToken();
			jEdit.resetProperty(action + ".icon");
		}
		jEdit.resetProperty("view.toolbar");
		GUIUtilities.invalidateMenuModels();
	}
}
