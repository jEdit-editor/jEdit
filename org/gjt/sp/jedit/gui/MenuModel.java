/*
 * MenuModel.java - A menu template
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

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.util.StringTokenizer;
import java.util.Vector;
import org.gjt.sp.jedit.*;

public class MenuModel extends MenuItemModel
{
	public static final Object SEPARATOR = "-";

	public MenuModel(String name)
	{
		super(name);

		children = new Vector();

		String menuItems = jEdit.getProperty(name);
		if(menuItems != null)
		{
			StringTokenizer st = new StringTokenizer(menuItems);
			while(st.hasMoreTokens())
			{
				String menuItemName = st.nextToken();
				if(menuItemName.equals("-"))
					children.addElement(SEPARATOR);
				else
				{
					if(menuItemName.startsWith("%"))
					{
						children.addElement(GUIUtilities.loadMenuModel(
							menuItemName.substring(1)));
					}
					else
					{
						children.addElement(GUIUtilities.loadMenuItemModel(
							menuItemName));
					}
				}
			}
		}
	}

	public JMenuItem create(View view)
	{
		JMenu menu = view.getMenu(name);
		if(menu != null)
			return menu;

		menu = new JMenu(label);
		menu.setMnemonic(mnemonic);

		for(int i = 0; i < children.size(); i++)
		{
			Object obj = children.elementAt(i);
			if(obj == SEPARATOR)
				menu.addSeparator();
			else
			{
				MenuItemModel menuItem = (MenuItemModel)obj;
				menu.add(menuItem.create(view));
			}
		}

		return menu;
	}

	public JPopupMenu createPopup(View view)
	{
		JPopupMenu menu = new JPopupMenu();
		menu.setInvoker(view);

		for(int i = 0; i < children.size(); i++)
		{
			Object obj = children.elementAt(i);
			if(obj == SEPARATOR)
				menu.addSeparator();
			else
			{
				MenuItemModel menuItem = (MenuItemModel)obj;
				menu.add(menuItem.create(view));
			}
		}

		return menu;
	}

	// protected members
	protected Vector children;
}
