/*
 * BrowserPopupMenu.java - provides popup actions for rename, del, etc.
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
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA	02111-1307, USA.
 */

package org.gjt.sp.jedit.browser;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.*;

/**
 * @version $Id$
 * @author Slava Pestov and Jason Ginchereau
 */
public class BrowserPopupMenu extends JPopupMenu
{
	public BrowserPopupMenu(VFSBrowser browser, VFS.DirectoryEntry file)
	{
		this.browser = browser;

		if(file != null)
		{
			this.file = file;
			this.vfs = VFSManager.getVFSForPath(browser.getDirectory());

			boolean delete = (vfs.getCapabilities() & VFS.DELETE_CAP) != 0;
			boolean rename = (vfs.getCapabilities() & VFS.RENAME_CAP) != 0;

			if(jEdit.getBuffer(file.path) != null)
			{
				add(createMenuItem("open"));
				add(createMenuItem("open-view"));
				add(createMenuItem("insert"));
				add(createMenuItem("close"));
			}
			else
			{
				if(file.type == VFS.DirectoryEntry.DIRECTORY
					|| file.type == VFS.DirectoryEntry.FILESYSTEM)
				{
					add(createMenuItem("goto"));
				}
				else if(browser.getMode() != VFSBrowser.BROWSER)
				{
					add(createMenuItem("choose"));
				}
				// else if in browser mode
				else
				{
					add(createMenuItem("open"));
					add(createMenuItem("open-view"));
					add(createMenuItem("insert"));
				}
	
				if(rename)
					add(createMenuItem("rename"));
				if(delete)
					add(createMenuItem("delete"));
			}

			addSeparator();
		}

		JCheckBoxMenuItem showHiddenFiles = new JCheckBoxMenuItem(
			jEdit.getProperty("vfs.browser.menu.show-hidden-files.label"));
		showHiddenFiles.setActionCommand("show-hidden-files");
		showHiddenFiles.setSelected(browser.getShowHiddenFiles());
		showHiddenFiles.addActionListener(new ActionHandler());
		add(showHiddenFiles);

		addSeparator();
		add(createMenuItem("new-file"));
		add(createMenuItem("new-directory"));

		addSeparator();

		add(createMenuItem("add-to-favorites"));
		add(createMenuItem("go-to-favorites"));

		Enumeration enum = VFSManager.getFilesystems();
		boolean addedSeparator = false;

		while(enum.hasMoreElements())
		{
			VFS vfs = (VFS)enum.nextElement();
			if((vfs.getCapabilities() & VFS.BROWSE_CAP) == 0)
				continue;

			if(!addedSeparator)
			{
				addSeparator();
				addedSeparator = true;
			}

			JMenuItem menuItem = new JMenuItem(jEdit.getProperty(
				"vfs." + vfs.getName() + ".label"));
			menuItem.setActionCommand("vfs." + vfs.getName());
			menuItem.addActionListener(new ActionHandler());
			add(menuItem);
		}
	}

	// private members
	private VFSBrowser browser;
	private VFS.DirectoryEntry file;
	private VFS vfs;

	private JMenuItem createMenuItem(String name)
	{
		String label = jEdit.getProperty("vfs.browser.menu." + name + ".label");
		JMenuItem mi = new JMenuItem(label);
		mi.setActionCommand(name);
		mi.addActionListener(new ActionHandler());
		return mi;
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			View view = browser.getView();
			String actionCommand = evt.getActionCommand();

			if(actionCommand.equals("open"))
				jEdit.openFile(view,file.path);
			else if(actionCommand.equals("open-view"))
			{
				Buffer buffer = jEdit.openFile(null,file.path);
				if(buffer != null)
					jEdit.newView(view,buffer);
			}
			else if(actionCommand.equals("insert"))
				view.getBuffer().insert(view,file.path);
			else if(actionCommand.equals("choose"))
				browser.filesActivated();
			else if(actionCommand.equals("close"))
			{
				Buffer buffer = jEdit.getBuffer(file.path);
				if(buffer != null)
					jEdit.closeBuffer(view,buffer);
			}
			else if(actionCommand.equals("goto"))
				browser.setDirectory(file.path);
			else if(evt.getActionCommand().equals("rename"))
				browser.rename(file.path);
			else if(evt.getActionCommand().equals("delete"))
				browser.delete(file.deletePath);
			else if(actionCommand.equals("show-hidden-files"))
			{
				browser.setShowHiddenFiles(!browser.getShowHiddenFiles());
				browser.reloadDirectory();
			}
			else if(actionCommand.equals("new-file"))
			{
				VFS.DirectoryEntry[] selected = browser.getSelectedFiles();
				if(selected.length >= 1)
				{
					VFS.DirectoryEntry file = selected[0];
					if(file.type == VFS.DirectoryEntry.DIRECTORY)
						jEdit.newFile(view,file.path);
					else
					{
						VFS vfs = VFSManager.getVFSForPath(file.path);
						jEdit.newFile(view,vfs.getParentOfPath(file.path));
					}
				}
				else
					jEdit.newFile(view,null);
			}
			else if(actionCommand.equals("new-directory"))
				browser.mkdir();
			else if(actionCommand.equals("add-to-favorites"))
			{
				// if any directories are selected, add
				// them, otherwise add current directory
				Vector toAdd = new Vector();
				VFS.DirectoryEntry[] selected = browser.getSelectedFiles();
				for(int i = 0; i < selected.length; i++)
				{
					VFS.DirectoryEntry file = selected[i];
					if(file.type == VFS.DirectoryEntry.FILE)
					{
						GUIUtilities.error(browser,
							"vfs.browser.files-favorites",
							null);
						return;
					}
					else
						toAdd.addElement(file.path);
				}
	
				if(toAdd.size() != 0)
				{
					for(int i = 0; i < toAdd.size(); i++)
					{
						FavoritesVFS.addToFavorites((String)toAdd.elementAt(i));
					}
				}
				else
				{
					String directory = browser.getDirectory();
					if(directory.equals(FavoritesVFS.PROTOCOL + ":"))
					{
						GUIUtilities.error(browser,
							"vfs.browser.recurse-favorites",
							null);
					}
					else
					{
						FavoritesVFS.addToFavorites(directory);
					}
				}
			}
			else if(actionCommand.equals("go-to-favorites"))
				browser.setDirectory(FavoritesVFS.PROTOCOL + ":");
			else if(actionCommand.startsWith("vfs."))
			{
				String vfsName = actionCommand.substring(4);
				VFS vfs = VFSManager.getVFSByName(vfsName);
				String directory = vfs.showBrowseDialog(null,browser);
				if(directory != null)
					browser.setDirectory(directory);
			}
		}
	}
}
