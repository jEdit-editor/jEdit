/*
 * BrowserTreeView.java
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
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit.browser;

import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.Enumeration;
import java.util.Vector;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.MiscUtilities;

/**
 * VFS browser tree view.
 * @author Slava Pestov
 * @version $Id$
 */
public class BrowserTreeView extends BrowserView
{
	public BrowserTreeView(VFSBrowser browser)
	{
		super(browser);

		currentlyLoadingTreeNode = rootNode = new DefaultMutableTreeNode(null,true);
		model = new DefaultTreeModel(rootNode,true);

		tree = new BrowserJTree(model);
		tree.setCellRenderer(new FileCellRenderer());
		tree.setEditable(false);
		tree.addMouseListener(new MouseHandler());
		tree.addTreeExpansionListener(new TreeHandler());
		tree.putClientProperty("JTree.lineStyle", "Angled");
		tree.setRowHeight(22);
		tree.setVisibleRowCount(10);

		if(browser.isMultipleSelectionEnabled())
			tree.getSelectionModel().setSelectionMode(
				TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		else
			tree.getSelectionModel().setSelectionMode(
				TreeSelectionModel.SINGLE_TREE_SELECTION);

		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, scroller = new JScrollPane(tree));
	}

	public VFS.DirectoryEntry[] getSelectedFiles()
	{
		Vector selected = new Vector(tree.getSelectionCount());
		TreePath[] paths = tree.getSelectionPaths();
		if(paths == null)
			return new VFS.DirectoryEntry[0];

		for(int i = 0; i < paths.length; i++)
		{
			DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)
				paths[i].getLastPathComponent();
			Object obj = treeNode.getUserObject();
			if(obj instanceof VFS.DirectoryEntry)
				selected.addElement(obj);
		}

		VFS.DirectoryEntry[] retVal = new VFS.DirectoryEntry[selected.size()];
		selected.copyInto(retVal);
		return retVal;
	}

	public void directoryLoaded(Vector directory)
	{
		if(currentlyLoadingTreeNode == rootNode)
		{
			rootNode.setUserObject(browser.getDirectory());
			scroller.getViewport().setViewPosition(new Point(0,0));
		}

		currentlyLoadingTreeNode.removeAllChildren();

		if(directory != null)
		{
			for(int i = 0; i < directory.size(); i++)
			{
				VFS.DirectoryEntry file = (VFS.DirectoryEntry)
					directory.elementAt(i);
				boolean allowsChildren = (file.type != VFS.DirectoryEntry.FILE);
				currentlyLoadingTreeNode.add(new DefaultMutableTreeNode(file,allowsChildren));
			}
		}

		// fire events
		model.reload(currentlyLoadingTreeNode);

		tree.expandPath(new TreePath(currentlyLoadingTreeNode.getPath()));

		/* If the user expands a tree node manually, the tree
		 * listener sets currentlyLoadingTreeNode to that.
		 * But if VFSBrowser.setDirectory() is called, we want
		 * the root node to be updated.
		 *
		 * Since the browser view receives no prior notification
		 * to a setDirectory(), we set the currentlyLoadingTreeNode
		 * to null here. */
		currentlyLoadingTreeNode = rootNode;
	}

	public void updateFileView()
	{
		tree.repaint();
	}

	public void reloadDirectory(String path)
	{
		// because this method is called for *every* VFS update,
		// we don't want to scan the tree all the time. So we
		// use the following algorithm to determine if the path
		// might be part of the tree:
		// - if the path starts with the browser's current directory,
		//   we do the tree scan
		// - if the browser's directory is 'favorites:' -- we have to
		//   do the tree scan, as every path can appear under the
		//   favorites list
		// - if the browser's directory is 'roots:' and path is on
		//   the local filesystem, do a tree scan
		String browserDir = browser.getDirectory();
		if(browserDir.startsWith(FavoritesVFS.PROTOCOL))
			reloadDirectory(rootNode,path);
		else if(browserDir.startsWith(FileRootsVFS.PROTOCOL))
		{
			if(!MiscUtilities.isURL(path) || MiscUtilities.getFileProtocol(path)
				.equals("file"))
				reloadDirectory(rootNode,path);
		}
		else if(path.startsWith(browserDir))
			reloadDirectory(rootNode,path);
	}

	// private members
	private JTree tree;
	private JScrollPane scroller;
	private DefaultTreeModel model;
	private DefaultMutableTreeNode rootNode;
	private DefaultMutableTreeNode currentlyLoadingTreeNode;

	private static FileCellRenderer renderer = new FileCellRenderer();

	private boolean reloadDirectory(DefaultMutableTreeNode node, String path)
	{
		// nodes which are not expanded need not be checked
		if(!tree.isExpanded(new TreePath(node.getPath())))
			return false;

		Object userObject = node.getUserObject();
		if(userObject instanceof String)
		{
			if(path.equals(userObject))
			{
				loadDirectoryNode(node,path,false);
				return true;
			}
		}
		else if(userObject instanceof VFS.DirectoryEntry)
		{
			VFS.DirectoryEntry file = (VFS.DirectoryEntry)userObject;

			// we don't need to do anything with files!
			if(file.type == VFS.DirectoryEntry.FILE)
				return false;

			if(path.equals(file.path))
			{
				loadDirectoryNode(node,path,false);
				return true;
			}
		}

		if(node.getChildCount() != 0)
		{
			Enumeration children = node.children();
			while(children.hasMoreElements())
			{
				DefaultMutableTreeNode child = (DefaultMutableTreeNode)
					children.nextElement();
				if(reloadDirectory(child,path))
					return true;
			}
		}

		return false;
	}

	private void loadDirectoryNode(DefaultMutableTreeNode node, String path,
		boolean showLoading)
	{
		currentlyLoadingTreeNode = node;

		if(showLoading)
		{
			node.removeAllChildren();
			node.add(new DefaultMutableTreeNode(new LoadingPlaceholder(),false));
		}

		// fire events
		model.reload(currentlyLoadingTreeNode);

		browser.loadDirectory(path);
	}

	class BrowserJTree extends HelpfulJTree
	{
		BrowserJTree(TreeModel model)
		{
			super(model);
		}

		protected void processMouseEvent(MouseEvent evt)
		{
			// don't pass double-clicks to tree, otherwise
			// directory nodes will be expanded and we don't
			// want that
			if(evt.getID() == MouseEvent.MOUSE_PRESSED
				&& evt.getClickCount() == 2)
			{
				evt.consume();
				return;
			}
			else
				super.processMouseEvent(evt);
		}
	}

	class MouseHandler extends MouseAdapter
	{
		public void mouseClicked(MouseEvent evt)
		{
			if((evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)
			{
				if(tree.getSelectionCount() != 0)
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
				TreePath path = tree.getPathForLocation(evt.getX(),evt.getY());
				if(path != null)
				{
					if(!tree.isPathSelected(path))
						tree.setSelectionPath(path);
				}

				Object userObject = ((DefaultMutableTreeNode)path
					.getLastPathComponent()).getUserObject();
				if(userObject instanceof VFS.DirectoryEntry)
				{
					VFS.DirectoryEntry file = (VFS.DirectoryEntry)
						userObject;
					showFilePopup(file,tree,evt.getPoint());
				}
			}
		}
	}

	class TreeHandler implements TreeExpansionListener
	{
		public void treeExpanded(TreeExpansionEvent evt)
		{
			TreePath path = evt.getPath();
			DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)
				path.getLastPathComponent();
			Object userObject = treeNode.getUserObject();
			if(userObject instanceof VFS.DirectoryEntry)
			{
				loadDirectoryNode(treeNode,((VFS.DirectoryEntry)
					userObject).path,true);
			}
		}

		public void treeCollapsed(TreeExpansionEvent evt)
		{
			TreePath path = evt.getPath();
			DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)
				path.getLastPathComponent();
			if(treeNode.getUserObject() instanceof VFS.DirectoryEntry)
			{
				// we add the placeholder so that the node has
				// 1 child (otherwise the user won't be able to
				// expand it again)
				treeNode.removeAllChildren();
				treeNode.add(new DefaultMutableTreeNode(new LoadingPlaceholder(),false));
				model.reload(treeNode);
			}
		}
	}

	class LoadingPlaceholder {}
}

/*
 * Change Log:
 * $Log$
 * Revision 1.8  2000/08/27 02:06:52  sp
 * Filter combo box changed to a text field in VFS browser, passive mode FTP toggle
 *
 * Revision 1.7  2000/08/20 07:29:30  sp
 * I/O and VFS browser improvements
 *
 * Revision 1.6  2000/08/15 08:07:10  sp
 * A bunch of bug fixes
 *
 * Revision 1.5  2000/08/13 07:35:23  sp
 * Dockable window API
 *
 * Revision 1.4  2000/08/11 12:13:14  sp
 * Preparing for 2.6pre2 release
 *
 * Revision 1.3  2000/08/10 11:55:58  sp
 * VFS browser toolbar improved a little bit, font selector tweaks
 *
 * Revision 1.2  2000/08/10 08:30:40  sp
 * VFS browser work, options dialog work, more random tweaks
 *
 * Revision 1.1  2000/08/06 09:44:27  sp
 * VFS browser now has a tree view, rename command
 *
 */
