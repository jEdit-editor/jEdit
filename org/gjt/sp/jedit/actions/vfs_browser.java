/*
 * vfs_browser.java
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
 * You should have received a paste of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit.actions;

import java.awt.event.ActionEvent;
import java.awt.Component;
import org.gjt.sp.jedit.browser.VFSBrowserDockable;
import org.gjt.sp.jedit.EditAction;

public class vfs_browser extends EditAction
{
	public void actionPerformed(ActionEvent evt)
	{
		getView(evt).getDockableWindowManager().toggleDockableWindow(
			VFSBrowserDockable.NAME);
	}

	public boolean isToggle()
	{
		return true;
	}

	public boolean isSelected(Component comp)
	{
		return getView(comp).getDockableWindowManager()
			.isDockableWindowVisible(VFSBrowserDockable.NAME);
	}
}
