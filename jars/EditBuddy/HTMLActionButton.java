/*
 * HTMLActionButton.java - Button that invokes an action
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
import org.gjt.sp.jedit.*;

public class HTMLActionButton extends JButton
{
	public HTMLActionButton(String name)
	{
		setRequestFocusEnabled(false);

		setText(jEdit.getProperty("edit-buddy." + name + ".label"));

		String action = jEdit.getProperty("edit-buddy." + name + ".action");
		int index = action.indexOf('@');
		if(index == -1)
			addActionListener(jEdit.getAction(action));
		else
		{
			String arg = action.substring(index + 1);
			action = action.substring(0,index);
			addActionListener(jEdit.getAction(action));
			setActionCommand(arg);
		}

		String pluginName = jEdit.getProperty("edit-buddy." + name + ".dependsOnPlugin");
		if(pluginName != null)
		{
			EditPlugin plugin = jEdit.getPlugin(pluginName);
			if(plugin instanceof EditPlugin.Broken || plugin == null)
				setEnabled(false);
			else
				setEnabled(true);
		}
	}

	// so that null action commands are supported
	public String getActionCommand()
	{
		return getModel().getActionCommand();
	}
}
