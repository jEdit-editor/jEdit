/*
 * MenuBarModel.java - A menu bar template
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
import javax.swing.JMenuBar;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import org.gjt.sp.jedit.*;

public class MenuBarModel
{
	public MenuBarModel(String name)
	{
		children = new Vector();

		String menus = jEdit.getProperty(name);
		if(menus != null)
		{
			StringTokenizer st = new StringTokenizer(menus);
			while(st.hasMoreTokens())
			{
				MenuModel menu = GUIUtilities.loadMenuModel(st.nextToken());
				children.addElement(menu);
			}
		}
	}

	public JMenuBar create(View view)
	{
		JMenuBar mbar = new JMenuBar();

		Enumeration enum = children.elements();
		while(enum.hasMoreElements())
		{
			MenuModel menu = (MenuModel)enum.nextElement();
			mbar.add((JMenu)menu.create(view));
		}
		return mbar;
	}

	// protected members
	protected Vector children;
}
