/*
 * HelpfulJList.java - Displays tooltips for obscured items
 * Copyright (C) 2000 Slava Pestov
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
import org.gjt.sp.jedit.jEdit;

/**
 * Adds tool-tip helpers for obscured list items.
 *
 * @author Jason Ginchereau
 */
public class HelpfulJList extends JList implements MouseListener
{
	public HelpfulJList()
	{
		ToolTipManager.sharedInstance().registerComponent(this);
		addMouseListener(this);
	}

	public final String getToolTipText(MouseEvent evt)
	{
		int index = locationToIndex(evt.getPoint());
		if(index >= 0)
		{
			Object item = getModel().getElementAt(index);
			Component renderer = getCellRenderer()
				.getListCellRendererComponent(this,item,
				index,isSelectedIndex(index),false);

			Dimension cellSize = renderer.getPreferredSize();
			Rectangle cellBounds = getCellBounds(index, index);
			if(cellBounds != null)
			{
				Rectangle cellRect = new Rectangle(0,cellBounds.y,
					cellSize.width,cellBounds.height);
				if(!cellRectIsVisible(cellRect))
					return item.toString();
			}
		}

		return null;
	}

	public final Point getToolTipLocation(MouseEvent evt)
	{
		int index = locationToIndex(evt.getPoint());
		if(index >= 0)
		{
			Object item = getModel().getElementAt(index);
			Component renderer = getCellRenderer().getListCellRendererComponent(
				this,item,index,isSelectedIndex(index),false);
			Dimension cellSize = renderer.getPreferredSize();
			Rectangle cellBounds = getCellBounds(index, index);
			if(cellBounds != null)
			{
				Rectangle cellRect = new Rectangle(cellBounds.x,cellBounds.y,
					 cellSize.width,cellBounds.height);

				if(!cellRectIsVisible(cellRect))
					return new Point(cellRect.x + 20, cellRect.y);
			}
		}
		return null;
	}

	private final boolean cellRectIsVisible(Rectangle cellRect)
	{
		Rectangle vr = getVisibleRect();
		return vr.contains(cellRect.x + 22, cellRect.y + 2) &&
			vr.contains(cellRect.x + 22 + cellRect.width - 31,
			cellRect.y + 2 + cellRect.height - 4);
	}

	public void mouseClicked(MouseEvent evt) {}
	public void mousePressed(MouseEvent evt) {}
	public void mouseReleased(MouseEvent evt) {}

	public void mouseEntered(MouseEvent evt)
	{
		ToolTipManager ttm = ToolTipManager.sharedInstance();
		toolTipInitialDelay = ttm.getInitialDelay();
		toolTipReshowDelay = ttm.getReshowDelay();
		ttm.setInitialDelay(0);
		ttm.setReshowDelay(0);
	}

	public void mouseExited(MouseEvent evt)
	{
		ToolTipManager ttm = ToolTipManager.sharedInstance();
		if(toolTipInitialDelay >= 0)
			ttm.setInitialDelay(toolTipInitialDelay);
		if(toolTipReshowDelay >= 0)
			ttm.setReshowDelay(toolTipReshowDelay);
	}

	// private members
	private int toolTipInitialDelay = -1;
	private int toolTipReshowDelay = -1;
}

/*
 * Change Log:
 * $Log$
 * Revision 1.1  2000/07/30 09:04:19  sp
 * More VFS browser hacking
 *
 */
