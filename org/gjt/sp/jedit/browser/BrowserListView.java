/*
 * BrowserListView.java - Browser list view
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

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.event.*;
import javax.swing.*;
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.jEdit;

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

		if(browser.getMode() == VFSBrowser.OPEN_DIALOG)
			list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		else // if SAVE_DIALOG or BROWSER mode
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		list.addMouseListener(new MouseHandler());
		list.addListSelectionListener(new ListHandler());
		list.setMinimumSize(new Dimension(0,0));
		list.setVisibleRowCount(10);

		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, new JScrollPane(list));
	}

	public VFS.DirectoryEntry[] getSelectedFiles()
	{
		Object[] selected = list.getSelectedValues();
		VFS.DirectoryEntry[] retVal = new VFS.DirectoryEntry[selected.length];
		System.arraycopy(selected,0,retVal,0,selected.length);
		return retVal;
	}

	public void directoryLoaded(String path, VFS.DirectoryEntry[] directory)
	{
		// XXX: filtering and sorting
		if(directory == null)
			list.setListData(new Object[0]);
		else
			list.setListData(directory);
	}

	public void updateFileView()
	{
		list.repaint();
	}

	// private members
	private JList list;

	private static FileCellRenderer renderer = new FileCellRenderer();

	class ListHandler implements ListSelectionListener
	{
		public void valueChanged(ListSelectionEvent evt)
		{
			browser.filesSelected();
		}
	}

	class MouseHandler extends MouseAdapter
	{
		public void mouseClicked(MouseEvent evt)
		{
			if((evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0 &&
				evt.getClickCount() == 2)
			{
				if(list.getSelectedIndex() != -1);
					browser.filesActivated();
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
 * Revision 1.1  2000/07/30 09:04:19  sp
 * More VFS browser hacking
 *
 */
