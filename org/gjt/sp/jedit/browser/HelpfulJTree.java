/*
 * HelpfulJTree.java
 * Copyright (C) 1999 Jason Ginchereau
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA	02111-1307, USA.
 */

package org.gjt.sp.jedit.browser;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;

/**
 * Adds tool-tip helpers for obscured tree items.
 *
 * @author Jason Ginchereau
 */
public class HelpfulJTree extends JTree implements MouseListener
{
	public HelpfulJTree(TreeModel model)
	{
		super(model);
		ToolTipManager.sharedInstance().registerComponent(this);
		addMouseListener(this);
	}

	public final String getToolTipText(MouseEvent evt)
	{
		TreePath path = getPathForLocation(evt.getX(), evt.getY());
		if(path != null)
		{
			Rectangle cellRect = getPathBounds(path);
			if(cellRect != null && !cellRectIsVisible(cellRect))
				return path.getLastPathComponent().toString();
		}
		return null;
	}

	public final Point getToolTipLocation(MouseEvent evt)
	{
		TreePath path = getPathForLocation(evt.getX(), evt.getY());
		if(path != null)
		{
			Rectangle cellRect = getPathBounds(path);
			if(cellRect != null && !cellRectIsVisible(cellRect))
			{
				return new Point(cellRect.x + 20, cellRect.y);
			}
		}
		return null;
	}

	private final boolean cellRectIsVisible(Rectangle cellRect)
	{
		Rectangle vr = getVisibleRect();
		return vr.contains(cellRect.x,cellRect.y) &&
			vr.contains(cellRect.x + cellRect.width,
			cellRect.y + cellRect.height);
	}

	public void mouseClicked(MouseEvent evt) {}
	public void mousePressed(MouseEvent evt) {}
	public void mouseReleased(MouseEvent evt) {}

	public void mouseEntered(MouseEvent evt)
	{
		ToolTipManager ttm = ToolTipManager.sharedInstance();
		toolTipInitialDelay = ttm.getInitialDelay();
		toolTipReshowDelay = ttm.getReshowDelay();
		ttm.setInitialDelay(200);
		ttm.setReshowDelay(0);
	}

	public void mouseExited(MouseEvent evt)
	{
		ToolTipManager ttm = ToolTipManager.sharedInstance();
		ttm.setInitialDelay(toolTipInitialDelay);
		ttm.setReshowDelay(toolTipReshowDelay);
	}

	// private members
	private int toolTipInitialDelay = -1;
	private int toolTipReshowDelay = -1;
}

/*
 * Change Log:
 * $Log$
 * Revision 1.4  2000/09/23 03:01:10  sp
 * pre7 yayayay
 *
 * Revision 1.3  2000/08/16 12:14:29  sp
 * Passwords are now saved, bug fixes, documentation updates
 *
 * Revision 1.2  2000/08/16 08:47:19  sp
 * Stuff
 *
 * Revision 1.1  2000/08/06 09:44:27  sp
 * VFS browser now has a tree view, rename command
 *
 */
