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
import java.awt.*;
import org.gjt.sp.jedit.msg.EditorStarted;
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
	}

	public void handleMessage(EBMessage msg)
	{
		if(msg instanceof EditorStarted)
		{
			// use plugin manager's last-version property
			// for backwards compatibility
			String build = jEdit.getProperty("update-plugins.last-version");
			String myBuild = jEdit.getBuild();
			if(build == null)
			{
				doFirstTimeWizard();
			}
			else if(myBuild.compareTo(build) > 0)
			{
				doNewVersionWizard();
			}
			else if(myBuild.compareTo(build) < 0)
			{
				Log.log(Log.WARNING,EditBuddyPlugin.class,
					"You downgraded from jEdit " + build
					+ " to " + myBuild + "!");
			}

			jEdit.setProperty("update-plugins.last-version",myBuild);
		}
	}

	public static void doFirstTimeWizard()
	{
		if(jEdit.getSettingsDirectory() == null)
		{
			Log.log(Log.WARNING,EditBuddyPlugin.class,
				"Cannot run first-time wizard if -nosettings"
				+ " switch is specified");
			return;
		}

		FirstTimeWizard wizard = new FirstTimeWizard();
		doWizard("first-time",wizard);
	}

	public static void doNewVersionWizard()
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
		doWizard("new-version",wizard);
	}

	// private members
	private static void doWizard(String title, Wizard wizard)
	{
		JDialog dialog = new JDialog(null,jEdit.getProperty(
			"edit-buddy." + title + ".title"),true);
		wizard.setPreferredSize(new Dimension(640,480));
		dialog.setContentPane(wizard);
		dialog.pack();
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		dialog.setLocation((screen.width - dialog.getSize().width) / 2,
			(screen.height - dialog.getSize().height) / 2);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		GUIUtilities.hideSplashScreen();
		dialog.show();
	}
}
