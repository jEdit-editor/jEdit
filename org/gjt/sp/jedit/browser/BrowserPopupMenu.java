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
					FavoritesVFS.addToFavorites(browser.getDirectory());
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

/*
 * Change Log:
 * $Log$
 * Revision 1.8  2000/11/11 02:59:30  sp
 * FTP support moved out of the core into a plugin
 *
 * Revision 1.7  2000/10/30 07:14:04  sp
 * 2.7pre1 branched, GUI improvements
 *
 * Revision 1.6  2000/10/05 04:30:10  sp
 * *** empty log message ***
 *
 * Revision 1.5  2000/08/27 02:06:52  sp
 * Filter combo box changed to a text field in VFS browser, passive mode FTP toggle
 *
 * Revision 1.4  2000/08/16 12:14:29  sp
 * Passwords are now saved, bug fixes, documentation updates
 *
 * Revision 1.3  2000/08/13 07:35:23  sp
 * Dockable window API
 *
 * Revision 1.2  2000/08/06 09:44:27  sp
 * VFS browser now has a tree view, rename command
 *
 * Revision 1.1  2000/08/05 07:16:12  sp
 * Global options dialog box updated, VFS browser now supports right-click menus
 *
 */
