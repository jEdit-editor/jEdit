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
import java.util.StringTokenizer;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.jedit.msg.ViewUpdate;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class EditBuddyPlugin extends EBPlugin
{
	public void start()
	{
		// these actions are only used by wizards.
		// they do not have defined '.label' properties,
		// therefore they do not appear in the 'Command Shortcuts'
		// option pane, nor can they be added to menus.
		jEdit.addAction(new edit_buddy_firewall_config());
		jEdit.addAction(new edit_buddy_plugin_install());
		jEdit.addAction(new edit_buddy_plugin_update());

		String build = jEdit.getProperty("update-plugins.last-version");

		// reset toolbar when upgrading from 2.6pre6 to
		// avoid confusion
		if(build != null && build.compareTo("02.06.06.00") <= 0)
			resetToolBar();
	}

	public void handleMessage(EBMessage msg)
	{
		if(msg instanceof ViewUpdate)
		{
			ViewUpdate vmsg = (ViewUpdate)msg;
			if(vmsg.getWhat() == ViewUpdate.CREATED)
			{
				View view = vmsg.getView();

				// use plugin manager's last-version property
				// for backwards compatibility
				String build = jEdit.getProperty("update-plugins.last-version");

				String myBuild = jEdit.getBuild();
				if(build == null)
				{
					doFirstTimeWizard(view);
				}
				else if(myBuild.compareTo(build) > 0)
				{
					doNewVersionWizard(view);
				}
				else if(myBuild.compareTo(build) < 0)
				{
					Log.log(Log.WARNING,EditBuddyPlugin.class,
						"You downgraded from jEdit " + build
						+ " to " + myBuild + "!");
				}

				jEdit.setProperty("update-plugins.last-version",myBuild);

				EditBus.removeFromBus(this);
			}
		}
	}

	public static void doFirstTimeWizard(View view)
	{
		if(jEdit.getSettingsDirectory() == null)
		{
			Log.log(Log.WARNING,EditBuddyPlugin.class,
				"Cannot run first-time wizard if -nosettings"
				+ " switch is specified");
			return;
		}

		FirstTimeWizard wizard = new FirstTimeWizard();
		doWizard(view,"first-time",wizard);
	}

	public static void doNewVersionWizard(View view)
	{
		if(jEdit.getSettingsDirectory() == null)
		{
			Log.log(Log.WARNING,EditBuddyPlugin.class,
				"Cannot run new version wizard if -nosettings"
				+ " switch is specified");
			return;
		}

		NewVersionWizard wizard = new NewVersionWizard(
			MiscUtilities.buildToVersion(jEdit.getProperty(
			"update-plugins.last-version")),
			MiscUtilities.buildToVersion(jEdit.getBuild()));;
		doWizard(view,"new-version",wizard);
	}

	// private members
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

	private static void doWizard(final View view, String title, Wizard wizard)
	{
		final JDialog dialog = new JDialog(view,jEdit.getProperty(
			"edit-buddy." + title + ".title"),false);
		wizard.setPreferredSize(new Dimension(640,480));
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setContentPane(wizard);
		dialog.pack();

		view.addWindowListener(new WindowAdapter()
		{
			public void windowOpened(WindowEvent evt)
			{
				dialog.setLocationRelativeTo(view);
				dialog.show();
			}
		});
	}
}
