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
public class BrowserView extends JPanel
{
	public BrowserView(VFSBrowser browser)
	{
		this.browser = browser;

		currentlyLoadingTreeNode = rootNode = new DefaultMutableTreeNode(null,true);
		model = new DefaultTreeModel(rootNode,true);

		tree = new BrowserJTree(model);
		tree.setCellRenderer(new FileCellRenderer());
		tree.setEditable(false);
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

	public void selectNone()
	{
		tree.setSelectionPaths(new TreePath[0]);
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
			if(!MiscUtilities.isURL(path) || MiscUtilities.getProtocolOfURL(path)
				.equals("file"))
				reloadDirectory(rootNode,path);
		}
		else if(path.startsWith(browserDir))
			reloadDirectory(rootNode,path);
	}

	public Component getDefaultFocusComponent()
	{
		return tree;
	}

	// private members
	private VFSBrowser browser;

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

	private void showFilePopup(VFS.DirectoryEntry file, Point point)
	{
		BrowserPopupMenu popup = new BrowserPopupMenu(browser,file);
		popup.show(tree,point.x,point.y);
	}

	class BrowserJTree extends JTree
	{
		BrowserJTree(TreeModel model)
		{
			super(model);
			ToolTipManager.sharedInstance().registerComponent(this);
		}

		public final String getToolTipText(MouseEvent evt)
		{
			TreePath path = getPathForLocation(evt.getX(), evt.getY());
			if(path != null)
			{
				Rectangle cellRect = getPathBounds(path);
				if(cellRect != null && !cellRectIsVisible(cellRect))
					return path.getLastPathComponent().toString();
			}
			return null;
		}

		public final Point getToolTipLocation(MouseEvent evt)
		{
			TreePath path = getPathForLocation(evt.getX(), evt.getY());
			if(path != null)
			{
				Rectangle cellRect = getPathBounds(path);
				if(cellRect != null && !cellRectIsVisible(cellRect))
				{
					return new Point(cellRect.x + 20, cellRect.y + 2);
				}
			}
			return null;
		}

		protected void processKeyEvent(KeyEvent evt)
		{
			if(evt.getID() == KeyEvent.KEY_PRESSED)
			{
				switch(evt.getKeyCode())
				{
				case KeyEvent.VK_ENTER:
					browser.filesActivated();
					evt.consume();
					break;
				case KeyEvent.VK_LEFT:
					if(tree.getMinSelectionRow() == 0
						|| tree.getMinSelectionRow() == 1)
					{
						String directory = browser.getDirectory();
						browser.setDirectory(VFSManager.getVFSForPath(
							directory).getParentOfPath(directory));
						evt.consume();
					}
					break;
				}
			}

			if(!evt.isConsumed())
				super.processKeyEvent(evt);
		}

		protected void processMouseEvent(MouseEvent evt)
		{
			ToolTipManager ttm = ToolTipManager.sharedInstance();

			switch(evt.getID())
			{
			case MouseEvent.MOUSE_ENTERED:
				toolTipInitialDelay = ttm.getInitialDelay();
				toolTipReshowDelay = ttm.getReshowDelay();
				ttm.setInitialDelay(200);
				ttm.setReshowDelay(0);
				super.processMouseEvent(evt);
				break;
			case MouseEvent.MOUSE_EXITED:
				ttm.setInitialDelay(toolTipInitialDelay);
				ttm.setReshowDelay(toolTipReshowDelay);
				super.processMouseEvent(evt);
				break;
			case MouseEvent.MOUSE_PRESSED:
				if((evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)
				{
					TreePath path = tree.getPathForLocation(evt.getX(),evt.getY());
					if(path == null)
					{
						super.processMouseEvent(evt);
						break;
					}

					if(!tree.isPathSelected(path))
						tree.setSelectionPath(path);

					if(evt.getClickCount() == 1)
					{
						browser.filesSelected();
						super.processMouseEvent(evt);
					}
					if(evt.getClickCount() == 2)
					{
						// don't pass double-clicks to tree, otherwise
						// directory nodes will be expanded and we don't
						// want that
						browser.filesActivated();
						break;
					}
				}
				else if((evt.getModifiers() & MouseEvent.BUTTON3_MASK) != 0)
				{
					TreePath path = tree.getPathForLocation(evt.getX(),evt.getY());
					if(path == null)
						showFilePopup(null,evt.getPoint());
					else
					{
						if(!tree.isPathSelected(path))
							tree.setSelectionPath(path);
	
						Object userObject = ((DefaultMutableTreeNode)path
							.getLastPathComponent()).getUserObject();
						if(userObject instanceof VFS.DirectoryEntry)
						{
							VFS.DirectoryEntry file = (VFS.DirectoryEntry)
								userObject;
							showFilePopup(file,evt.getPoint());
						}
					}

					return;
				}

				super.processMouseEvent(evt);
				break;
			default:
				super.processMouseEvent(evt);
				break;
			}
		}

		// private members
		private int toolTipInitialDelay = -1;
		private int toolTipReshowDelay = -1;

		private final boolean cellRectIsVisible(Rectangle cellRect)
		{
			Rectangle vr = BrowserJTree.this.getVisibleRect();
			return vr.contains(cellRect.x,cellRect.y) &&
				vr.contains(cellRect.x + cellRect.width,
				cellRect.y + cellRect.height);
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
 * Revision 1.6  2000/10/30 07:14:04  sp
 * 2.7pre1 branched, GUI improvements
 *
 * Revision 1.12  2000/10/05 04:30:10  sp
 * *** empty log message ***
 *
 * Revision 1.11  2000/09/23 03:01:10  sp
 * pre7 yayayay
 *
 * Revision 1.10  2000/08/31 02:54:00  sp
 * Improved activity log, bug fixes
 *
 * Revision 1.9  2000/08/29 07:47:12  sp
 * Improved complete word, type-select in VFS browser, bug fixes
 *
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
 */
