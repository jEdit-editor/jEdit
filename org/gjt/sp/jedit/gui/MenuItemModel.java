/*
 * MenuItemModel.java - A menu item template
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

package org.gjt.sp.jedit.gui;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.Insets;
import java.net.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class MenuItemModel
{
	public MenuItemModel(String name)
	{
		this.name = name;

		String actionName;
		int index = name.indexOf('@');
		if(index != -1)
		{
			arg = name.substring(index+1);
			actionName = name.substring(0,index);
		}
		else
		{
			arg = null;
			actionName = name;
			action = jEdit.getAction(name);
		}
		action = jEdit.getAction(actionName);

		if(actionName.equals("play-macro"))
		{
			index = arg.lastIndexOf('/');
			label = arg.substring(index + 1).replace('_',' ');
		}
		else
		{
			label = jEdit.getProperty(name.concat(".label"));
			if(label == null)
				label = name;
		}

		index = label.indexOf('$');
		if(index != -1 && label.length() - index > 1)
		{
                        mnemonic = Character.toLowerCase(label.charAt(index + 1));
			label = label.substring(0,index).concat(label.substring(++index));
		}
		else
			mnemonic = '\0';

		// stuff for createButton():
		String iconName = jEdit.getProperty(name + ".icon");
		if(iconName != null)
		{
			icon = GUIUtilities.loadIcon(iconName);
			toolTip = label;
			String shortcut = jEdit.getProperty(name + ".shortcut");
			if(shortcut != null)
				toolTip = toolTip + " (" + shortcut + ")";
		}
	}

	// note that the 'view' parameter is unused by MenuItemModel
	// and is only there so that subclassing by MenuModel will work.
	// It may go away in the future (and in fact the entire
	// Menu*Model API might change; plugins shouldn't call it
	// directly, and instead use the wrapper methods in the
	// GUIUtilities class).
	public JMenuItem create(View view)
	{
		return create(true);
	}

	public JMenuItem createForPopup()
	{
		return create(false);
	}

	public JMenuItem create(boolean setMnemonic)
	{
		JMenuItem mi;
		if(action != null && action.isToggle())
			mi = new EnhancedCheckBoxMenuItem(label,action,arg);
		else
			mi = new EnhancedMenuItem(label,action,arg);

		if(setMnemonic && mnemonic != '\0')
			mi.setMnemonic(mnemonic);

		return mi;
	}

	public EnhancedButton createButton()
	{
		return new EnhancedButton(icon,toolTip,action,arg);
	}

	/**
	 * Some menu item models, such as macros, come and go, and should
	 * not be cached.
	 * @since jEdit 2.6pre1
	 */
	public boolean isTransient()
	{
		if(action == null)
			return false;

		if(action.getName().equals("play-macro"))
			return true;
		else
			return false;
	}

	// protected members
	protected Icon icon;
	protected String name;
	protected String label;
	protected String toolTip;
	protected char mnemonic;
	protected EditAction action;
	protected String arg;
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.15  2000/09/23 03:01:10  sp
 * pre7 yayayay
 *
 * Revision 1.14  2000/08/10 08:30:40  sp
 * VFS browser work, options dialog work, more random tweaks
 *
 * Revision 1.13  2000/07/26 07:48:44  sp
 * stuff
 *
 * Revision 1.12  2000/07/12 09:11:38  sp
 * macros can be added to context menu and tool bar, menu bar layout improved
 *
 * Revision 1.11  2000/06/29 06:20:45  sp
 * Tool bar icon code bug fix
 *
 * Revision 1.10  2000/05/14 10:55:22  sp
 * Tool bar editor started, improved view registers dialog box
 *
 * Revision 1.9  2000/04/29 09:17:07  sp
 * VFS updates, various fixes
 *
 * Revision 1.8  2000/04/18 11:44:31  sp
 * Context menu editor finished
 *
 */
