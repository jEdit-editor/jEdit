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

import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.*;

/**
 *
 * @author Jason Ginchereau
 */
public class BrowserPopupMenu extends JPopupMenu
{
	public BrowserPopupMenu(VFSBrowser browser, VFS.DirectoryEntry file)
	{
		this.browser = browser;
		this.file = file;
		this.vfs = browser.getVFS();

		boolean delete = (vfs.getCapabilities() & VFS.DELETE_CAP) != 0;
		boolean rename = (vfs.getCapabilities() & VFS.RENAME_CAP) != 0;

		if(jEdit.getBuffer(file.path) != null)
		{
			// file is currently opened in jEdit
			boolean dcclose = jEdit.getBooleanProperty("vfs.browser"
				+ ".doubleClickClose");
			add(createMenuItem("close",dcclose));
			add(createMenuItem("open",!dcclose));
			add(createMenuItem("open-view",false));
		}
		else
		{
			if(file.type == VFS.DirectoryEntry.DIRECTORY
				|| file.type == VFS.DirectoryEntry.FILESYSTEM)
			{
				add(createMenuItem("goto",true));
				add(createMenuItem("new-browser",false));
			}
			else if(browser.getMode() != VFSBrowser.BROWSER)
			{
				add(createMenuItem("select",true));
			}
			// else if in browser mode
			else
			{
				add(createMenuItem("open",true));
				add(createMenuItem("open-view",false));
			}

			if(rename)
				add(createMenuItem("rename",false));
			if(delete)
				add(createMenuItem("delete",false));
		}
	}

	// private members
	private VFSBrowser browser;
	private VFS.DirectoryEntry file;
	private VFS vfs;

	private JMenuItem createMenuItem(String name, boolean bold)
	{
		String label = jEdit.getProperty("vfs.browser.menu." + name + ".label");
		JMenuItem mi = new JMenuItem(label);
		Font f = mi.getFont();
		f = new Font(f.getName(), (bold ? Font.BOLD : Font.PLAIN), f.getSize());
		mi.setFont(f);
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
			{
				Hashtable props = new Hashtable();
				props.put(Buffer.VFS_SESSION_HACK,browser.getVFSSession());
				jEdit.openFile(view,null,file.path,false,false,props);
			}
			else if(actionCommand.equals("open-view"))
			{
				Hashtable props = new Hashtable();
				props.put(Buffer.VFS_SESSION_HACK,browser.getVFSSession());
				Buffer buffer = jEdit.openFile(null,null,file.path,false,false,props);
				if(buffer != null)
					jEdit.newView(view,buffer);
			}
			else if(actionCommand.equals("select"))
			{
				browser.filesActivated();
			}
			else if(actionCommand.equals("close"))
			{
				Buffer buffer = jEdit.getBuffer(file.path);
				if(buffer != null)
					jEdit.closeBuffer(view,buffer);
			}
			else if(actionCommand.equals("goto"))
			{
				browser.setDirectory(file.path);
			}
			else if(actionCommand.equals("new-browser"))
			{
				new VFSBrowserFrame(browser.getView(),file.path);
			}
			else if(evt.getActionCommand().equals("rename"))
			{
				//
			}
			else if(evt.getActionCommand().equals("delete"))
			{
				browser.delete(file.deletePath);
			}
		}
	}
}

/*
 * Change Log:
 * $Log$
 * Revision 1.1  2000/08/05 07:16:12  sp
 * Global options dialog box updated, VFS browser now supports right-click menus
 *
 */
