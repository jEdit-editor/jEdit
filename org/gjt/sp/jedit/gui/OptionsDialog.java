/*
 * OptionsDialog.java - Global options dialog
 * Copyright (C) 1998, 1999, 2000 Slava Pestov
 * Portions copyright (C) 1999 mike dillon
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

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.options.*;
import org.gjt.sp.util.Log;

/**
 * An abstract tabbed options dialog box.
 * @author Slava Pestov
 * @version $Id$
 */
public class OptionsDialog extends EnhancedDialog
	implements ActionListener, TreeSelectionListener
{
	public OptionsDialog(View view)
	{
		super(view,jEdit.getProperty("options.title"),true);

		view.showWaitCursor();

		getContentPane().setLayout(new BorderLayout());
		panes = new Hashtable();

		cardPanel = new JPanel(new CardLayout());
		cardPanel.setBorder(new CompoundBorder(new BevelBorder(
			BevelBorder.RAISED), new EmptyBorder(2, 2, 2, 2)));

		getContentPane().add(cardPanel, BorderLayout.CENTER);

		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
		DefaultTreeModel paneTreeModel = new DefaultTreeModel(rootNode);

		jEditNode = new DefaultMutableTreeNode("jedit");
		rootNode.add(jEditNode);

		pluginsNode = new DefaultMutableTreeNode("plugins");
		rootNode.add(pluginsNode);

		paneTree = new JTree(paneTreeModel);
		paneTree.setCellRenderer(new PaneNameRenderer());
		paneTree.putClientProperty("JTree.lineStyle", "Angled");
		paneTree.setRootVisible(false);
		getContentPane().add(new JScrollPane(paneTree),
			BorderLayout.WEST);

		JPanel buttons = new JPanel();
		ok = new JButton(jEdit.getProperty("common.ok"));
		ok.addActionListener(this);
		buttons.add(ok);
		getRootPane().setDefaultButton(ok);
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(this);
		buttons.add(cancel);
		getContentPane().add(buttons, BorderLayout.SOUTH);

		addOptionPane(new GeneralOptionPane(), jEditNode);
		addOptionPane(new EditorOptionPane(), jEditNode);
		addOptionPane(new StyleOptionPane(), jEditNode);
		addOptionPane(new GutterOptionPane(), jEditNode);
		addOptionPane(new FileFilterOptionPane(), jEditNode);

		OptionGroup shortcutsGroup = new OptionGroup("shortcuts");
		shortcutsGroup.addOptionPane(new CommandShortcutsOptionPane());
		shortcutsGroup.addOptionPane(new MacroShortcutsOptionPane());
		addOptionGroup(shortcutsGroup, jEditNode);

		addOptionPane(new AbbrevsOptionPane(), jEditNode);

		// Query plugins for option panes
		EditPlugin[] plugins = jEdit.getPlugins();
		for(int i = 0; i < plugins.length; i++)
		{
			try
			{
				plugins[i].createOptionPanes(this);
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR, this,
					"Error creating option pane");
				Log.log(Log.ERROR, this, t);
			}
		}

		paneTree.expandPath(new TreePath(paneTreeModel.getPathToRoot(
			jEditNode)));
		paneTree.expandPath(new TreePath(paneTreeModel.getPathToRoot(
			pluginsNode)));

		paneTree.getSelectionModel().addTreeSelectionListener(this);

		view.hideWaitCursor();

		Dimension screen = getToolkit().getScreenSize();
		pack();
		setLocation((screen.width - getSize().width) / 2,
			(screen.height - getSize().height) / 2);
		show();
	}

	public void addOptionGroup(OptionGroup group)
	{
		addOptionGroup(group, pluginsNode);
	}

	public void addOptionPane(OptionPane pane)
	{
		addOptionPane(pane, pluginsNode);
	}

	// EnhancedDialog implementation
	public void ok()
	{
		Enumeration enum = panes.elements();
		while(enum.hasMoreElements())
		{
			try
			{
				((OptionPane)enum.nextElement()).save();
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,this,"Error saving option pane");
				Log.log(Log.ERROR,this,t);
			}
		}

		/* This will fire the PROPERTIES_CHANGED event */
		jEdit.propertiesChanged();
		dispose();
	}

	public void cancel()
	{
		dispose();
	}
	// end EnhancedDialog implementation

	public void actionPerformed(ActionEvent evt)
	{
		Object source = evt.getSource();
		if(source == ok)
			ok();
		else if(source == cancel)
			cancel();
	}

	public void valueChanged(TreeSelectionEvent evt)
	{
		TreePath selection = evt.getPath();
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)
			selection.getLastPathComponent();

		if (node.isLeaf())
		{
			String cardName = (String) node.getUserObject();

			currentPane = cardName;

			((CardLayout)cardPanel.getLayout()).show(cardPanel,
				cardName);
		}
		else
		{
			paneTree.expandPath(selection);
		}
	}

	// private members
	private Hashtable panes;
	private JTree paneTree;
	private JPanel cardPanel;
	private JButton ok;
	private JButton cancel;
	private DefaultMutableTreeNode jEditNode;
	private DefaultMutableTreeNode pluginsNode;
	private String currentPane;

	private void addOptionGroup(OptionGroup group, DefaultMutableTreeNode node)
	{
		DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(
			group.getName());

		Enumeration enum = group.getMembers();

		while (enum.hasMoreElements())
		{
			Object elem = enum.nextElement();

			if (elem instanceof OptionPane)
			{
				addOptionPane((OptionPane) elem, groupNode);
			}
			else if (elem instanceof OptionGroup)
			{
				addOptionGroup((OptionGroup) elem, groupNode);
			}
		}

		node.add(groupNode);
	}

	private void addOptionPane(OptionPane pane, DefaultMutableTreeNode node)
	{
		String name = pane.getName();

		node.add(new DefaultMutableTreeNode(name, false));

		cardPanel.add(pane.getComponent(), name);

		panes.put(name, pane);
	}

	class PaneNameRenderer extends JLabel implements TreeCellRenderer
	{
		public PaneNameRenderer()
		{
			setOpaque(true);
			setFont(UIManager.getFont("Tree.font"));
		}

		public Component getTreeCellRendererComponent(JTree tree,
			Object value, boolean selected, boolean expanded,
			boolean leaf, int row, boolean hasFocus)
		{
			if (selected)
			{
				setBackground(UIManager.getColor(
					"Tree.selectionBackground"));
				setForeground(UIManager.getColor(
					"Tree.selectionForeground"));
			}
			else
			{
				setBackground(tree.getBackground());
				setForeground(tree.getForeground());
			}

			String name = value.toString();

			String label = jEdit.getProperty(
				"options." + name + ".label");

			if (label == null) label = name;

			setText(label);

			setBorder(hasFocus ? focusBorder : noFocusBorder);

			return this;
		}

		private Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);
		private Border focusBorder = new LineBorder(UIManager.getColor(
			"Tree.selectionBorderColor"), 1);
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.13  2000/02/02 10:03:31  sp
 * Option groups added
 *
 * Revision 1.11  2000/01/30 04:23:23  sp
 * New about box, minor bug fixes and updates here and there
 *
 * Revision 1.10  2000/01/16 06:09:27  sp
 * Bug fixes
 *
 * Revision 1.9  2000/01/14 22:11:24  sp
 * Enhanced options dialog box
 *
 * Revision 1.8  1999/11/26 07:37:11  sp
 * Escape/enter handling code moved to common superclass, bug fixes
 *
 * Revision 1.7  1999/11/19 08:54:52  sp
 * EditBus integrated into the core, event system gone, bug fixes
 *
 * Revision 1.6  1999/10/23 03:48:22  sp
 * Mode system overhaul, close all dialog box, misc other stuff
 *
 * Revision 1.5  1999/10/04 03:20:51  sp
 * Option pane change, minor tweaks and bug fixes
 *
 * Revision 1.4  1999/09/30 12:21:04  sp
 * No net access for a month... so here's one big jEdit 2.1pre1
 *
 * Revision 1.3  1999/07/16 23:45:49  sp
 * 1.7pre6 BugFree version
 *
 * Revision 1.2  1999/07/05 04:38:39  sp
 * Massive batch of changes... bug fixes, also new text component is in place.
 * Have fun
 *
 * Revision 1.1  1999/06/07 06:36:32  sp
 * Syntax `styling' (bold/italic tokens) added,
 * plugin options dialog for plugin option panes
 *
 */
