/*
 * docking_top_expanded.java - Action
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

package org.gjt.sp.jedit.actions;

import java.awt.event.ActionEvent;
import java.awt.Component;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.*;

public class docking_top_expanded extends EditAction
{
	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		DockableWindowContainer.TabbedPane c
			= view.getDockableWindowManager().getDockingArea(
			DockableWindowManager.TOP);
		c.setCollapsed(!c.isCollapsed());
	}

	public boolean isToggle()
	{
		return true;
	}

	public boolean isSelected(Component comp)
	{
		View view = getView(comp);
		DockableWindowContainer.TabbedPane c
			= view.getDockableWindowManager().getDockingArea(
			DockableWindowManager.TOP);
		return !c.isCollapsed();
	}
}
