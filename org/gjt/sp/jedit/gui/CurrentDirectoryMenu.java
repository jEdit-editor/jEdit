/*
 * CurrentDirectoryMenu.java - File list menu
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
import java.io.File;
import org.gjt.sp.jedit.*;

public class CurrentDirectoryMenu extends JMenu
{
	public CurrentDirectoryMenu(View view)
	{
		String label = jEdit.getProperty("current-directory.label");
		int index = label.indexOf('$');
		char mnemonic = '\0';
		if(index != -1)
		{
			mnemonic = Character.toUpperCase(label.charAt(index+1));
			label = label.substring(0,index) + label.substring(index+1);
		}
		setText(label);
		setMnemonic(mnemonic);

		this.view = view;
	}

	public void setPopupMenuVisible(boolean b)
	{
		if(b)
		{
			File dir = new File(view.getBuffer().getFile().getParent());

			JMenuItem mi = new JMenuItem(dir.getPath());
			mi.setEnabled(false);
			add(mi);
			addSeparator();

			JMenu current = this;
			int count = 0;
			EditAction action = jEdit.getAction("open-path");
			String[] list = dir.list();
			if(list != null)
			{
				MiscUtilities.quicksort(list,
					new MiscUtilities.StringICaseCompare());
				for(int i = 0; i < list.length; i++)
				{
					String name = list[i];

					File file = new File(dir,name);
					if(file.isDirectory())
						continue;

					mi = new EnhancedMenuItem(name,null,action,
						file.getPath());

					if(count++ > 20)
					{
						current.addSeparator();
						JMenu newCurrent = new JMenu(jEdit.getProperty(
							"current-directory.more.label"));
						current.add(newCurrent);
						current = newCurrent;
						count = 0;
					}
					current.add(mi);
				}
			}

			super.setPopupMenuVisible(b);
		}
		else
		{
			super.setPopupMenuVisible(b);

			if(getMenuComponentCount() != 0)
				removeAll();
		}
	}

	// private members
	private View view;
}
