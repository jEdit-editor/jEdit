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
import java.awt.BorderLayout;
import java.util.Vector;
import org.gjt.sp.jedit.io.VFS;

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

		if(browser.isMultipleSelectionEnabled())
			tree.getSelectionModel().setSelectionMode(
				TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		else
			tree.getSelectionModel().setSelectionMode(
				TreeSelectionModel.SINGLE_TREE_SELECTION);

		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, new JScrollPane(tree));
	}

	public VFS.DirectoryEntry[] getSelectedFiles()
	{
		Vector selected = new Vector(tree.getSelectionCount());
		TreePath[] paths = tree.getSelectionPaths();
		if(paths == null)
			return null;

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
			rootNode.setUserObject(browser.getDirectory());

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

		System.err.println(currentlyLoadingTreeNode.getChildCount());

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
		// XXX: todo
		browser.reloadDirectory(true);
	}

	// private members
	private JTree tree;
	private DefaultTreeModel model;
	private DefaultMutableTreeNode rootNode;
	private DefaultMutableTreeNode currentlyLoadingTreeNode;

	private static FileCellRenderer renderer = new FileCellRenderer();

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
				System.err.println(userObject);
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
				System.err.println("expanded: " + treeNode.getUserObject());
				treeNode.add(new DefaultMutableTreeNode(new LoadingPlaceholder(),false));
				currentlyLoadingTreeNode = treeNode;
				browser.loadDirectory(((VFS.DirectoryEntry)userObject).path);
			}
		}

		public void treeCollapsed(TreeExpansionEvent evt)
		{
			TreePath path = evt.getPath();
			DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)
				path.getLastPathComponent();
			if(treeNode.getUserObject() instanceof VFS.DirectoryEntry)
			{
				System.err.println("collapsed: " + treeNode.getUserObject());
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
 * Revision 1.2  2000/08/10 08:30:40  sp
 * VFS browser work, options dialog work, more random tweaks
 *
 * Revision 1.1  2000/08/06 09:44:27  sp
 * VFS browser now has a tree view, rename command
 *
 */
