/*
 * MenuItemModel.java - A menu item template
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

package org.gjt.sp.jedit.gui;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.Insets;
import java.net.URL;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class MenuItemModel
{
	public MenuItemModel(String name)
	{
		this.name = name;

		int index = name.indexOf('@');
		if(index != -1)
		{
			arg = name.substring(index+1);
			action = jEdit.getAction(name.substring(0,index));
		}
		else
		{
			arg = null;
			action = jEdit.getAction(name);
		}

		label = jEdit.getProperty(name.concat(".label"));
		shortcut = jEdit.getProperty(name.concat(".shortcut"));
		if(label == null)
			label = name;

		index = label.indexOf('$');
		if(index != -1 && label.length() - index > 1)
		{
                        mnemonic = Character.toLowerCase(label.charAt(index + 1));
			label = label.substring(0,index).concat(label.substring(++index));
		}
		else
			mnemonic = '\0';

		// stuff for createButton();
		String iconName = jEdit.getProperty(name + ".icon");
		if(iconName != null)
		{
			URL url = getClass().getResource("/org/gjt/sp/jedit/toolbar/" + iconName);
			if(url != null)
			{
				icon = new ImageIcon(url);
				toolTip = GUIUtilities.prettifyMenuLabel(label);
				if(shortcut != null)
					toolTip = toolTip + " (" + shortcut + ")";
			}
		}
	}

	public JMenuItem create(View view)
	{
		JMenuItem mi;
		if(action != null && action.isToggle())
			mi = new EnhancedCheckBoxMenuItem(label,shortcut,
				action,arg);
		else
			mi = new EnhancedMenuItem(label,shortcut,action,arg);

		mi.setMnemonic(mnemonic);

		return mi;
	}

	public JButton createButton()
	{
		return new EnhancedButton(icon,toolTip,action,arg);
	}

	// protected members
	protected ImageIcon icon;
	protected String name;
	protected String label;
	protected String toolTip;
	protected String shortcut;
	protected char mnemonic;
	protected EditAction action;
	protected String arg;
}
