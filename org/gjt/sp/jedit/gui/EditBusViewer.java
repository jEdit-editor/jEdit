/*
 * EditBusViewer.java - Tree view of the EditBus
 * Copyright (C) 1999 Slava Pestov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit.gui;

import javax.swing.tree.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.Enumeration;
import org.gjt.sp.jedit.*;

public class EditBusViewer extends JFrame
{
	public EditBusViewer()
	{
		super(jEdit.getProperty("view-editbus.title"));

		JPanel panel = new JPanel();
		updateButton = new JButton(jEdit.getProperty("view-editbus.update"));
		updateButton.addActionListener(new ActionHandler());
		panel.add(updateButton);
		getContentPane().add(BorderLayout.NORTH,panel);

		tree = new JTree();
		JScrollPane scroller = new JScrollPane(tree);
		scroller.setPreferredSize(new Dimension(300,300));
		getContentPane().add(BorderLayout.CENTER,scroller);

		updateTree();

		pack();
		GUIUtilities.loadGeometry(this,"view-editbus");
		show();
	}

	public void updateTree()
	{
		treeRoot = new DefaultMutableTreeNode(jEdit.getProperty(
			"view-editbus.editbus"),true);
		treeModel = new DefaultTreeModel(treeRoot);

		DefaultMutableTreeNode compTree = new DefaultMutableTreeNode(
			jEdit.getProperty("view-editbus.components"),true);
		EBComponent[] components = EditBus.getComponents();
		for(int i = 0; i < components.length; i++)
		{
			compTree.insert(new DefaultMutableTreeNode(
				components[i].toString(),false),i);
		}
		treeRoot.insert(compTree,0);

		DefaultMutableTreeNode listsTree = new DefaultMutableTreeNode(
			jEdit.getProperty("view-editbus.lists"),true);
		Enumeration lists = EditBus.getNamedLists();
		while(lists.hasMoreElements())
		{
			Object tag = lists.nextElement();
			DefaultMutableTreeNode listTree = new DefaultMutableTreeNode(tag,true);
			Object[] values = EditBus.getNamedList(tag);
			for(int i = 0; i < values.length; i++)
			{
				listTree.insert(new DefaultMutableTreeNode(
					values[i].toString(),false),i);
			}
			listsTree.insert(listTree,0);
		}
		treeRoot.insert(listsTree,1);

		tree.setModel(treeModel);
	}

	// private members
	private JButton updateButton;
	private JTree tree;
	private DefaultTreeModel treeModel;
	private DefaultMutableTreeNode treeRoot;

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(evt.getSource() == updateButton)
				updateTree();
		}
	}
}
