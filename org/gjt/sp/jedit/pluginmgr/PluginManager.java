/*
 * PluginManager.java - Plugin manager window
 * Copyright (C) 2000, 2001 Slava Pestov
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

package org.gjt.sp.jedit.pluginmgr;

import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.File;
import java.util.Vector;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.*;

public class PluginManager extends JFrame
{
	public PluginManager()
	{
		super(jEdit.getProperty("plugin-manager.title"));

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		JLabel caption = new JLabel(jEdit.getProperty(
			"plugin-manager.caption"));
		caption.setBorder(new EmptyBorder(0,0,6,0));
		content.add(BorderLayout.NORTH,caption);

		tree = new JTree();
		tree.setCellRenderer(new Renderer());
		tree.setRootVisible(false);
		tree.setVisibleRowCount(16);

		content.add(BorderLayout.CENTER,new JScrollPane(tree));

		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons,BoxLayout.X_AXIS));
		buttons.setBorder(new EmptyBorder(12,0,0,0));

		buttons.add(Box.createGlue());
		remove = new JButton(jEdit.getProperty("plugin-manager"
			+ ".remove"));
		remove.addActionListener(new ActionHandler());
		buttons.add(remove);
		buttons.add(Box.createHorizontalStrut(6));
		update = new JButton(jEdit.getProperty("plugin-manager"
			+ ".update"));
		update.addActionListener(new ActionHandler());
		buttons.add(update);
		buttons.add(Box.createHorizontalStrut(6));
		install = new JButton(jEdit.getProperty("plugin-manager"
			+ ".install"));
		install.addActionListener(new ActionHandler());
		buttons.add(install);
		buttons.add(Box.createHorizontalStrut(6));
		close = new JButton(jEdit.getProperty("common.close"));
		close.addActionListener(new ActionHandler());
		buttons.add(close);
		buttons.add(Box.createGlue());

		content.add(BorderLayout.SOUTH,buttons);

		updateTree();

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		pack();
		GUIUtilities.loadGeometry(this,"plugin-manager");
		show();
	}

	public void dispose()
	{
		GUIUtilities.saveGeometry(this,"plugin-manager");
		super.dispose();
	}

	// private members
	private JTree tree;
	private JButton remove;
	private JButton update;
	private JButton install;
	private JButton close;

	private void updateTree()
	{
		DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode();
		DefaultTreeModel treeModel = new DefaultTreeModel(treeRoot);

		DefaultMutableTreeNode loadedTree = new DefaultMutableTreeNode(
			jEdit.getProperty("plugin-manager.loaded"),true);
		DefaultMutableTreeNode notLoadedTree = new DefaultMutableTreeNode(
			jEdit.getProperty("plugin-manager.not-loaded"),true);
		DefaultMutableTreeNode newTree = new DefaultMutableTreeNode(
			jEdit.getProperty("plugin-manager.new"),true);

		EditPlugin[] plugins = jEdit.getPlugins();
		for(int i = 0; i < plugins.length; i++)
		{
			EditPlugin plugin = plugins[i];
			String path = plugin.getJAR().getPath();
			if(!new File(path).exists())
			{
				// plugin was deleted
				continue;
			}

			Entry entry = new Entry(path,plugin.getClassName());
			if(plugin instanceof EditPlugin.Broken)
			{
				entry.broken = true;
				notLoadedTree.add(new DefaultMutableTreeNode(entry));
			}
			else
				loadedTree.add(new DefaultMutableTreeNode(entry));
		}

		if(notLoadedTree.getChildCount() != 0)
			treeRoot.add(notLoadedTree);

		if(loadedTree.getChildCount() != 0)
			treeRoot.add(loadedTree);

		String[] newPlugins = jEdit.getNotLoadedPluginJARs();
		for(int i = 0; i < newPlugins.length; i++)
		{
			Entry entry = new Entry(newPlugins[i],null);
			newTree.add(new DefaultMutableTreeNode(entry));
		}

		if(newTree.getChildCount() != 0)
			treeRoot.add(newTree);

		tree.setModel(treeModel);
		for(int i = 0; i < tree.getRowCount(); i++)
			tree.expandRow(i);

		remove.setEnabled(false);
	}

	class Entry
	{
		String path;
		String clazz;
		String name, version, author;
		boolean broken;
		String breakReason;

		Entry(String path, String clazz)
		{
			Entry.this.path = path;
			Entry.this.clazz = clazz;
			Entry.this.broken = broken;

			Entry.this.name = jEdit.getProperty("plugin."
				+ clazz + ".name");
			if(Entry.this.name == null)
				Entry.this.name = clazz;

			Entry.this.version = jEdit.getProperty("plugin."
				+ clazz + ".version");

			Entry.this.author = jEdit.getProperty("plugin."
				+ clazz + ".author");
		}
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			/* Object source = evt.getSource();
			if(source == close)
				dispose();
			else if(source == remove)
			{
				TreePath[] selected = tree.getSelectionModel()
					.getSelectionPaths();
				Vector plugins = new Vector();
				for(int i = 0; i < selected.length; i++)
				{
					Object last = ((DefaultMutableTreeNode)
						selected[i].getLastPathComponent())
						.getUserObject();
					if(last instanceof Entry)
						plugins.addElement(((Entry)last).path);
				}

				if(plugins.size() == 0)
				{
					getToolkit().beep();
					return;
				}

				String[] array = new String[plugins.size()];
				plugins.copyInto(array);
				setCursor(Cursor.getPredefinedCursor(
					Cursor.WAIT_CURSOR));

				if(PluginManagerPlugin.removePlugins(PluginManager.this,array))
					updateTree();

				setCursor(Cursor.getPredefinedCursor(
					Cursor.DEFAULT_CURSOR));
			}
			else if(source == update)
			{
				if(PluginManagerPlugin.updatePlugins(PluginManager.this))
					updateTree();
			}
			else if(source == install)
			{
				if(PluginManagerPlugin.installPlugins(PluginManager.this))
					updateTree();
			} */
		}
	}

	static class Renderer extends JPanel implements TreeCellRenderer
	{
		JLabel name;
		JLabel version;
		JLabel author;
		JLabel breakReason;

		Renderer()
		{
			Renderer.this.setLayout(new BoxLayout(this,
				BoxLayout.Y_AXIS));

			setOpaque(true);

			Font font = UIManager.getFont("Tree.font");

			name = new JLabel();
			name.setFont(new Font("SansSerif",Font.BOLD,14));
			name.setForeground(Color.black);
			version = new JLabel();
			version.setForeground(Color.black);
			version.setFont(font);

			author = new JLabel();
			author.setForeground(Color.black);
			author.setFont(font);

			breakReason = new JLabel();
			breakReason.setForeground(Color.black);
			breakReason.setFont(font);

			Box box = new Box(BoxLayout.X_AXIS);
			box.add(name);
			box.add(Box.createHorizontalStrut(6));
			box.add(version);
			Renderer.this.add(box);

			Renderer.this.add(author);
			Renderer.this.add(breakReason);
		}

		public Component getTreeCellRendererComponent(JTree tree,
			Object value, boolean selected, boolean expanded,
			boolean leaf, int row, boolean hasFocus)
		{
			if (selected)
			{
				Renderer.this.setBackground(UIManager.getColor(
					"Tree.selectionBackground"));
				Renderer.this.setForeground(UIManager.getColor(
					"Tree.selectionForeground"));
			}
			else
			{
				Renderer.this.setBackground(tree.getBackground());
				Renderer.this.setForeground(tree.getForeground());
			}

			Object _value = ((DefaultMutableTreeNode)value)
				.getUserObject();

			if(_value instanceof Entry)
			{
				Entry entry = (Entry)_value;
				name.setText(entry.name);
				version.setText(entry.version);

				// BAD
				author.setText("by " + entry.author);
			}
			else if(_value != null)
			{
				name.setText(_value.toString());
				version.setText(null);
				author.setText(null);
				breakReason.setText(null);
			}

			return this;
		}

		public boolean isShowing()
		{
			return true;
		}
	}
}
