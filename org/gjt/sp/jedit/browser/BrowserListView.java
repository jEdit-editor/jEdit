/*
 * BrowserListView.java
 * Copyright (C) 1999 Jason Ginchereau
 * Portions copyright (C) 2000 Slava Pestov
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
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit.browser;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import org.gjt.sp.jedit.io.VFS;

/**
 * Originally taken from QuickFile, adapted for VFS browser and file chooser.
 * @author Jason Ginchereau
 * @version $Id$
 */
public class BrowserListView extends BrowserView
{
	public BrowserListView(VFSBrowser browser)
	{
		super(browser);

		list = new HelpfulJList();
		list.setCellRenderer(renderer);

		if(browser.isMultipleSelectionEnabled())
			list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		else
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		list.addMouseListener(new MouseHandler());
		list.setVisibleRowCount(10);

		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, scroller = new JScrollPane(list));
	}

	public VFS.DirectoryEntry[] getSelectedFiles()
	{
		Object[] selected = list.getSelectedValues();
		VFS.DirectoryEntry[] retVal = new VFS.DirectoryEntry[selected.length];
		System.arraycopy(selected,0,retVal,0,selected.length);
		return retVal;
	}

	public void directoryLoaded(Vector directory)
	{
		if(directory == null)
			list.setListData(new Object[0]);
		else
			list.setListData(directory);

		//scroller.getViewport().setViewPosition(new Point(0,0));
	}

	public void updateFileView()
	{
		list.repaint();
	}

	public void reloadDirectory(String path)
	{
		if(path.equals(browser.getDirectory()))
			browser.reloadDirectory(true);
	}

	// private members
	private JList list;
	private JScrollPane scroller;

	private static FileCellRenderer renderer = new FileCellRenderer();

	class MouseHandler extends MouseAdapter
	{
		public void mouseClicked(MouseEvent evt)
		{
			if((evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)
			{
				if(list.getSelectedIndex() != -1)
				{
					if(evt.getClickCount() == 1)
						browser.filesSelected();
					else if(evt.getClickCount() == 2)
						browser.filesActivated();
				}
			}
		}

		public void mousePressed(MouseEvent evt)
		{
			if((evt.getModifiers() & MouseEvent.BUTTON3_MASK) != 0)
			{
				int index = list.locationToIndex(evt.getPoint());
				if(index >= 0)
				{
					if(list.getSelectedIndex() != index)
						list.setSelectedIndex(index);
				}

				VFS.DirectoryEntry file = (VFS.DirectoryEntry)
					list.getSelectedValue();
				if(file != null)
					showFilePopup(file,list,evt.getPoint());
			}
		}
	}
}

/*
 * Change Log:
 * $Log$
 * Revision 1.7  2000/08/20 07:29:30  sp
 * I/O and VFS browser improvements
 *
 * Revision 1.6  2000/08/13 07:35:23  sp
 * Dockable window API
 *
 * Revision 1.5  2000/08/06 09:44:27  sp
 * VFS browser now has a tree view, rename command
 *
 * Revision 1.4  2000/08/05 07:16:12  sp
 * Global options dialog box updated, VFS browser now supports right-click menus
 *
 * Revision 1.3  2000/08/01 11:44:15  sp
 * More VFS browser work
 *
 * Revision 1.2  2000/07/31 11:32:09  sp
 * VFS file chooser is now in a minimally usable state
 *
 * Revision 1.1  2000/07/30 09:04:19  sp
 * More VFS browser hacking
 *
 */
