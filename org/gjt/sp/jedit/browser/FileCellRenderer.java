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
		Font f = UIManager.getFont("Tree.font");
		normalFont = new Font(f.getName(), Font.PLAIN, f.getSize());
		openedFont = new Font(f.getName(), Font.BOLD , f.getSize());

		treeSelectionForeground = UIManager.getColor("Tree.selectionForeground");
		treeNoSelectionForeground = UIManager.getColor("Tree.textForeground");
		treeSelectionBackground = UIManager.getColor("Tree.selectionBackground");
		treeNoSelectionBackground = UIManager.getColor("Tree.textBackground");

		fileIcon = GUIUtilities.loadToolBarIcon("Document.gif");
		dirIcon = GUIUtilities.loadToolBarIcon("Folder.gif");
		filesystemIcon = GUIUtilities.loadToolBarIcon("CD.gif");
		loadingIcon = GUIUtilities.loadToolBarIcon("Reload.gif");
	}

	public Component getListCellRendererComponent(JList list, Object value,
		int index, boolean sel, boolean focus)
	{
		if(listCellRenderer == null)
		{
			listCellRenderer = new JLabel();
			listCellRenderer.setBorder(border);
			listCellRenderer.setOpaque(true);
		}

		VFS.DirectoryEntry file = (VFS.DirectoryEntry)value;
		boolean opened = (jEdit.getBuffer(file.path) != null);

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

		listCellRenderer.setFont(getFontForFile(opened));
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
			treeCellRenderer.setBorder(border);
			treeCellRenderer.setOpaque(true);
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

			treeCellRenderer.setFont(getFontForFile(opened));
			treeCellRenderer.setIcon(getIconForFile(file));
			treeCellRenderer.setText(file.name);
		}
		else if(userObject instanceof BrowserTreeView.LoadingPlaceholder)
		{
			treeCellRenderer.setFont(getFontForFile(false));
			treeCellRenderer.setIcon(loadingIcon);
			treeCellRenderer.setText(jEdit.getProperty("vfs.browser.tree.loading"));
		}
		else if(userObject instanceof String)
		{
			treeCellRenderer.setFont(getFontForFile(false));
			treeCellRenderer.setIcon(dirIcon);
			treeCellRenderer.setText((String)userObject);
		}

		return treeCellRenderer;
	}

	// protected members
	protected Font getFontForFile(boolean opened)
	{
		return opened ? openedFont : normalFont;
	}

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

	private Font normalFont;
	private Font openedFont;

	private Icon fileIcon;
	private Icon dirIcon;
	private Icon filesystemIcon;
	private Icon loadingIcon;

	private Border border = new EmptyBorder(1,0,1,0);
	private Color treeSelectionForeground;
	private Color treeNoSelectionForeground;
	private Color treeSelectionBackground;
	private Color treeNoSelectionBackground;
}

/*
 * Change Log:
 * $Log$
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
