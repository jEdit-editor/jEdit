/*
 * HTMLActionButton.java - Button that invokes an action. For use by EditBuddy
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

package org.gjt.sp.jedit.gui;

import javax.swing.*;
import org.gjt.sp.jedit.*;

/**
 * Do not use this class. The only reason it is in the jEdit core, and not
 * the EditBuddy plugin is that the Swing HTML &lt;object&gt; tag can't
 * load classes with the non-system class loader.
 */
public class HTMLActionButton extends JButton
{
	public String getAction()
	{
		return action;
	}

	public void setAction(String action)
	{
		this.action = action;
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
	}

	public String getDependsOnPlugin()
	{
		return pluginName;
	}

	public void setDependsOnPlugin(String pluginName)
	{
		this.pluginName = pluginName;
		EditPlugin plugin = jEdit.getPlugin(pluginName);
		if(plugin instanceof EditPlugin.Broken || plugin == null)
			setEnabled(false);
		else
			setEnabled(true);
	}

	// so that null action commands are supported
	public String getActionCommand()
	{
		return getModel().getActionCommand();
	}

	// private members
	private String action, pluginName;
}
