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
import org.gjt.sp.jedit.io.*;

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

		list.addKeyListener(new KeyHandler());
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
		// preserve selected files so user won't be annoyed by
		// automatic updates
		VFS.DirectoryEntry[] selected = getSelectedFiles();
		String[] selectedNames = new String[selected.length];
		for(int i = 0; i < selected.length; i++)
		{
			selectedNames[i] = selected[i].name;
		}

		if(directory == null)
			list.setListData(new Object[0]);
		else
			list.setListData(directory);

		// restore selection
		if(selectedNames.length != 0)
		{
			for(int i = 0; i < directory.size(); i++)
			{
				VFS.DirectoryEntry file = (VFS.DirectoryEntry)directory
					.elementAt(i);
				String name = file.name;
				for(int j = 0; j < selectedNames.length; j++)
				{
					if(selectedNames[j].equals(name))
					{
						list.addSelectionInterval(i,i);
						break;
					}
				}
			}
		}
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

	public Component getDefaultFocusComponent()
	{
		return list;
	}

	// private members
	private JList list;
	private JScrollPane scroller;

	private static FileCellRenderer renderer = new FileCellRenderer();

	class KeyHandler extends KeyAdapter
	{
		StringBuffer typeSelectBuffer = new StringBuffer();
		Timer timer = new Timer(0,new ClearTypeSelect());

		public void keyTyped(KeyEvent evt)
		{
			char ch = evt.getKeyChar();
			typeSelectBuffer.append(ch);
			doTypeSelect(typeSelectBuffer.toString());

			timer.stop();
			timer.setInitialDelay(500);
			timer.setRepeats(false);
			timer.start();
		}

		public void keyPressed(KeyEvent evt)
		{
			if(evt.getKeyCode() == KeyEvent.VK_ENTER)
			{
				browser.filesActivated();
				evt.consume();
			}
			else if(evt.getKeyCode() == KeyEvent.VK_LEFT)
			{
				VFS vfs = VFSManager.getVFSForPath(browser
					.getDirectory());
				browser.setDirectory(vfs.getParentOfPath(
					browser.getDirectory()));
			}
		}

		private void doTypeSelect(String str)
		{
			ListModel model = list.getModel();
			for(int i = 0; i < model.getSize(); i++)
			{
				VFS.DirectoryEntry file = (VFS.DirectoryEntry)
					model.getElementAt(i);
				if(file.name.regionMatches(true,0,str,0,str.length()))
				{
					list.setSelectedIndex(i);
					list.ensureIndexIsVisible(i);
					browser.filesSelected();
					return;
				}
			}
		}

		class ClearTypeSelect implements ActionListener
		{
			public void actionPerformed(ActionEvent evt)
			{
				typeSelectBuffer.setLength(0);
			}
		}
	}

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
 * Revision 1.8  2000/08/29 07:47:12  sp
 * Improved complete word, type-select in VFS browser, bug fixes
 *
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
