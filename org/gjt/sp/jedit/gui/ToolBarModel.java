/*
 * ToolBarModel.java - A tool bar template
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
import java.util.StringTokenizer;
import java.util.Vector;
import org.gjt.sp.jedit.*;

public class ToolBarModel extends MenuItemModel
{
	public static final Object SEPARATOR = "-";

	public ToolBarModel(String name)
	{
		super(name);

		children = new Vector();

		String buttons = jEdit.getProperty(name);
		if(buttons != null)
		{
			StringTokenizer st = new StringTokenizer(buttons);
			while(st.hasMoreTokens())
			{
				String button = st.nextToken();
				if(button.equals("-"))
					children.addElement(SEPARATOR);
				else
					children.addElement(GUIUtilities
						.loadMenuItemModel(button));
			}
		}
	}

	public JToolBar create()
	{
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		toolBar.putClientProperty("JToolBar.isRollover",Boolean.TRUE);

		for(int i = 0; i < children.size(); i++)
		{
			Object obj = children.elementAt(i);
			if(obj == SEPARATOR)
				toolBar.addSeparator();
			else
			{
				MenuItemModel menuItem = (MenuItemModel)obj;
				toolBar.add(menuItem.createButton());
			}
		}

		return toolBar;
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
