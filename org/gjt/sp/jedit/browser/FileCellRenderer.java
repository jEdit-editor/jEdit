/*
 * FileCellRenderer.java - renders list and tree cells for the VFS browser
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
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit.browser;

import java.awt.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.border.*;

import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.jEdit;

public final class FileCellRenderer implements javax.swing.ListCellRenderer,
	javax.swing.tree.TreeCellRenderer
{
	public FileCellRenderer() {
		font = UIManager.getFont("Tree.font");
		font = new Font(font.getName(), Font.PLAIN, font.getSize());

		treeSelectionForeground = UIManager.getColor("Tree.selectionForeground");
		treeNoSelectionForeground = UIManager.getColor("Tree.textForeground");
		treeSelectionBackground = UIManager.getColor("Tree.selectionBackground");
		treeNoSelectionBackground = UIManager.getColor("Tree.textBackground");

		// use metal icons because not all looks and feels define these.
		// note that metal is guaranteed to exist, so this shouldn't
		// cause problems in the future.
		UIDefaults metalDefaults = new javax.swing.plaf.metal.MetalLookAndFeel()
			.getDefaults();
		fileIcon = metalDefaults.getIcon("FileView.fileIcon");
		dirIcon = metalDefaults.getIcon("FileView.directoryIcon");
		filesystemIcon = metalDefaults.getIcon("FileView.hardDriveIcon");
		loadingIcon = metalDefaults.getIcon("FileView.hardDriveIcon");
	}

	public Component getListCellRendererComponent(JList list, Object value,
		int index, boolean sel, boolean focus)
	{
		if(listCellRenderer == null)
		{
			listCellRenderer = new JLabel();
			listCellRenderer.setOpaque(true);
			listCellRenderer.setFont(font);
		}

		VFS.DirectoryEntry file = (VFS.DirectoryEntry)value;
		boolean opened = (jEdit.getBuffer(file.path) != null);
		listCellRenderer.setBorder(opened ? openBorder : closedBorder);

		if(sel)
		{
			listCellRenderer.setBackground(list.getSelectionBackground());
			listCellRenderer.setForeground(list.getSelectionForeground());
		}
		else
		{
			listCellRenderer.setBackground(list.getBackground());
			listCellRenderer.setForeground(list.getForeground());
		}

		listCellRenderer.setIcon(getIconForFile(file));
		listCellRenderer.setText(file.name);
		listCellRenderer.setEnabled(list.isEnabled());

		return listCellRenderer;
	}

	public Component getTreeCellRendererComponent(JTree tree, Object value,
		boolean sel, boolean expanded, boolean leaf, int row,
		boolean focus)
	{
		if(treeCellRenderer == null)
		{
			treeCellRenderer = new JLabel();
			treeCellRenderer.setOpaque(true);
			treeCellRenderer.setFont(font);
		}

		if(sel)
		{
			treeCellRenderer.setBackground(treeSelectionBackground);
			treeCellRenderer.setForeground(treeSelectionForeground);
		}
		else
		{
			treeCellRenderer.setBackground(treeNoSelectionBackground);
			treeCellRenderer.setForeground(treeNoSelectionForeground);
		}

		treeCellRenderer.setEnabled(tree.isEnabled());

		DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)value;
		Object userObject = treeNode.getUserObject();
		if(userObject instanceof VFS.DirectoryEntry)
		{
			VFS.DirectoryEntry file = (VFS.DirectoryEntry)userObject;

			boolean opened = (jEdit.getBuffer(file.path) != null);
			treeCellRenderer.setBorder(opened ? openBorder : closedBorder);

			treeCellRenderer.setIcon(getIconForFile(file));
			treeCellRenderer.setText(file.name);
		}
		else if(userObject instanceof BrowserTreeView.LoadingPlaceholder)
		{
			treeCellRenderer.setIcon(loadingIcon);
			treeCellRenderer.setText(jEdit.getProperty("vfs.browser.tree.loading"));
			treeCellRenderer.setBorder(closedBorder);
		}
		else if(userObject instanceof String)
		{
			treeCellRenderer.setIcon(dirIcon);
			treeCellRenderer.setText((String)userObject);
			treeCellRenderer.setBorder(closedBorder);
		}

		return treeCellRenderer;
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
	private JLabel listCellRenderer = null;
	private JLabel treeCellRenderer = null;

	private Font font;

	private Icon fileIcon;
	private Icon dirIcon;
	private Icon filesystemIcon;
	private Icon loadingIcon;

	private Border closedBorder = new EmptyBorder(0,3,1,0);
	private Border openBorder = new CompoundBorder(new EmptyBorder(0,1,1,0),
		new MatteBorder(0,2,0,0,Color.black));

	private Color treeSelectionForeground;
	private Color treeNoSelectionForeground;
	private Color treeSelectionBackground;
	private Color treeNoSelectionBackground;
}

/*
 * Change Log:
 * $Log$
 * Revision 1.5  2000/09/23 03:01:10  sp
 * pre7 yayayay
 *
 * Revision 1.4  2000/08/16 08:47:19  sp
 * Stuff
 *
 * Revision 1.3  2000/08/06 09:44:27  sp
 * VFS browser now has a tree view, rename command
 *
 * Revision 1.2  2000/07/31 11:32:09  sp
 * VFS file chooser is now in a minimally usable state
 *
 * Revision 1.1  2000/07/30 09:04:19  sp
 * More VFS browser hacking
 *
 */
