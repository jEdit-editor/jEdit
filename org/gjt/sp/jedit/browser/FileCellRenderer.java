/*
 * FileCellRenderer.java - renders list and tree cells for the VFS browser
 * Copyright (C) 1999 Jason Ginchereau
 * Portions copyright (C) 2001 Slava Pestov
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
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.border.*;

import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.jedit.*;

public final class FileCellRenderer extends DefaultTreeCellRenderer
{
	public FileCellRenderer()
	{
		// use metal icons because not all looks and feels define these.
		// note that metal is guaranteed to exist, so this shouldn't
		// cause problems in the future.
		UIDefaults metalDefaults = new javax.swing.plaf.metal.MetalLookAndFeel()
			.getDefaults();
		fileIcon = metalDefaults.getIcon("FileView.fileIcon");
		dirIcon = metalDefaults.getIcon("FileView.directoryIcon");
		filesystemIcon = metalDefaults.getIcon("FileView.hardDriveIcon");
		loadingIcon = metalDefaults.getIcon("FileView.hardDriveIcon");

		propertiesChanged();

		/* pure genius. since only one instance of this class is ever
		 * created, we can add a 'hard-coded' component to the editbus
		 * to handle property changes.
		 */
		EditBus.addToBus(new EBComponent()
		{
			public void handleMessage(EBMessage msg)
			{
				if(msg instanceof PropertiesChanged)
					propertiesChanged();
			}
		});
	}

	public Component getTreeCellRendererComponent(JTree tree, Object value,
		boolean sel, boolean expanded, boolean leaf, int row,
		boolean focus)
	{
		super.getTreeCellRendererComponent(tree,value,sel,expanded,
			leaf,row,focus);

		DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)value;
		Object userObject = treeNode.getUserObject();
		if(userObject instanceof VFS.DirectoryEntry)
		{
			VFS.DirectoryEntry file = (VFS.DirectoryEntry)userObject;

			boolean opened = (jEdit.getBuffer(file.path) != null);
			setBorder(opened ? openBorder : closedBorder);

			if(showIcons)
			{
				setIcon(getIconForFile(file));
				setText(file.name);
			}
			else
			{
				setIcon(null);
				setText(file.type == VFS.DirectoryEntry.DIRECTORY
					? file.name + "/" : file.name);
			}
		}
		else if(userObject instanceof BrowserView.LoadingPlaceholder)
		{
			if(showIcons)
				setIcon(loadingIcon);
			else
				setIcon(null);
			setText(jEdit.getProperty("vfs.browser.tree.loading"));
			setBorder(closedBorder);
		}
		else if(userObject instanceof String)
		{
			if(showIcons)
			{
				setIcon(dirIcon);
				setText((String)userObject);
			}
			else
			{
				setIcon(null);
				setText(userObject + "/");
			}

			setBorder(closedBorder);
		}
		else
		{
			// userObject is null?
			setIcon(null);
			setText(null);
		}

		return this;
	}

	// protected members
	protected Icon getIconForFile(VFS.DirectoryEntry file)
	{
		if(file.type == VFS.DirectoryEntry.DIRECTORY)
			return dirIcon;
		else if(file.type == VFS.DirectoryEntry.FILESYSTEM)
			return filesystemIcon;
		else
			return fileIcon;
	}

	// private members
	private boolean showIcons;

	private Icon fileIcon;
	private Icon dirIcon;
	private Icon filesystemIcon;
	private Icon loadingIcon;

	private Border closedBorder;
	private Border openBorder;

	private void propertiesChanged()
	{
		// bug in DefaultTreeCellRenderer?
		setBackground(UIManager.getColor("Tree.textBackground"));

		showIcons = jEdit.getBooleanProperty("vfs.browser.showIcons");
		if(showIcons)
		{
			closedBorder = new EmptyBorder(0,3,0,0);
			openBorder = new CompoundBorder(new MatteBorder(0,2,0,0,
				UIManager.getColor("Tree.textForeground")),
				new EmptyBorder(0,1,0,0));
		}
		else
		{
			closedBorder = new EmptyBorder(1,4,1,1);
			openBorder = new CompoundBorder(new MatteBorder(0,2,0,0,
				UIManager.getColor("Tree.textForeground")),
				new EmptyBorder(1,2,1,1));
		}
	}
}
