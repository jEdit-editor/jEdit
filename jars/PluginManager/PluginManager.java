/*
 * PluginManager.java - Plugin list
 * Copyright (C) 2000 Slava Pestov
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

import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.File;
import java.util.Vector;
import org.gjt.sp.jedit.gui.EnhancedDialog;
import org.gjt.sp.jedit.*;

public class PluginManager extends EnhancedDialog
{
	public PluginManager(View view)
	{
		super(view,jEdit.getProperty("plugin-manager.title"),true);

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		JPanel panel = new JPanel(new BorderLayout());
		tree = new JTree();
		tree.setCellRenderer(new Renderer());
		tree.setRootVisible(false);
		tree.setVisibleRowCount(16);
		tree.putClientProperty("JTree.lineStyle", "Angled");
		tree.getSelectionModel().addTreeSelectionListener(
			new TreeHandler());
		panel.add(BorderLayout.CENTER,new JScrollPane(tree));

		JPanel panel2 = new JPanel(new BorderLayout());
		panel2.setBorder(new EmptyBorder(6,0,12,0));
		JPanel labelBox = new JPanel(new GridLayout(4,1,0,3));
		labelBox.setBorder(new EmptyBorder(0,0,0,12));
		labelBox.add(new JLabel(jEdit.getProperty("plugin-manager"
			+ ".info.path"),SwingConstants.RIGHT));
		labelBox.add(new JLabel(jEdit.getProperty("plugin-manager"
			+ ".info.name"),SwingConstants.RIGHT));
		labelBox.add(new JLabel(jEdit.getProperty("plugin-manager"
			+ ".info.author"),SwingConstants.RIGHT));
		labelBox.add(new JLabel(jEdit.getProperty("plugin-manager"
			+ ".info.version"),SwingConstants.RIGHT));
		panel2.add(BorderLayout.WEST,labelBox);

		JPanel valueBox = new JPanel(new GridLayout(4,1,0,3));
		valueBox.add(path = new JLabel());
		valueBox.add(name = new JLabel());
		valueBox.add(author = new JLabel());
		valueBox.add(version = new JLabel());
		panel2.add(BorderLayout.CENTER,valueBox);

		panel.add(BorderLayout.SOUTH,panel2);
		content.add(BorderLayout.CENTER,panel);

		Box box = new Box(BoxLayout.X_AXIS);

		box.add(Box.createGlue());
		remove = new JButton(jEdit.getProperty("plugin-manager"
			+ ".remove"));
		remove.addActionListener(new ActionHandler());
		box.add(remove);
		box.add(Box.createHorizontalStrut(6));
		update = new JButton(jEdit.getProperty("plugin-manager"
			+ ".update"));
		update.addActionListener(new ActionHandler());
		box.add(update);
		box.add(Box.createHorizontalStrut(6));
		install = new JButton(jEdit.getProperty("plugin-manager"
			+ ".install"));
		install.addActionListener(new ActionHandler());
		box.add(install);
		box.add(Box.createHorizontalStrut(6));
		close = new JButton(jEdit.getProperty("common.close"));
		close.addActionListener(new ActionHandler());
		box.add(close);
		box.add(Box.createGlue());

		content.add(BorderLayout.SOUTH,box);

		updateTree();

		Dimension screen = getToolkit().getScreenSize();
		pack();
		setLocationRelativeTo(view);
		show();
	}

	public void ok()
	{
		dispose();
	}

	public void cancel()
	{
		dispose();
	}

	// private members
	private String[][] plugins;
	private JTree tree;
	private JLabel path;
	private JLabel name;
	private JLabel author;
	private JLabel version;
	private JButton remove;
	private JButton update;
	private JButton install;
	private JButton close;

	private void updateTree()
	{
		DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode();
		DefaultTreeModel treeModel = new DefaultTreeModel(treeRoot);

		plugins = PluginManagerPlugin.getPluginsEx();

		DefaultMutableTreeNode loadedTree = new DefaultMutableTreeNode(
			jEdit.getProperty("plugin-manager.loaded"),true);
		String[] loaded = plugins[0];
		String[] loadedJARs = plugins[1];

		for(int i = 0; i < loaded.length; i++)
		{
			String path = loadedJARs[i];

			// skip removed plugins
			if(!new File(path).exists())
				continue;
			loadedTree.insert(new DefaultMutableTreeNode(
				new Entry(path,loaded[i]),false),
				loadedTree.getChildCount());
		}

		DefaultMutableTreeNode notLoadedTree = new DefaultMutableTreeNode(
			jEdit.getProperty("plugin-manager.not-loaded"),true);
		String[] notLoaded = plugins[2];
		String[] notLoadedJARs = plugins[3];
		for(int i = 0; i < notLoaded.length; i++)
		{
			String path = notLoadedJARs[i];

			// skip removed plugins
			if(!new File(path).exists())
				continue;
			notLoadedTree.insert(new DefaultMutableTreeNode(
				new Entry(path,notLoaded[i]),false),
				notLoadedTree.getChildCount());
		}

		treeRoot.insert(loadedTree,0);
		treeRoot.insert(notLoadedTree,1);

		tree.setModel(treeModel);
		for(int i = 0; i < tree.getRowCount(); i++)
			tree.expandRow(i);

		remove.setEnabled(false);
		path.setText(null);
		name.setText(null);
		author.setText(null);
		version.setText(null);
	}

	class Entry
	{
		String path;
		String clazz;

		Entry(String path, String clazz)
		{
			Entry.this.path = path;
			Entry.this.clazz = clazz;
		}

		public String toString()
		{
			if(clazz == null)
				return MiscUtilities.getFileName(path);
			else
			{
				String name = jEdit.getProperty("plugin."
					+ clazz + ".name");
				if(name != null)
					return name;
				else
					return clazz;
			}
		}
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(source == close)
				ok();
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
			}
		}
	}

	class TreeHandler implements TreeSelectionListener
	{
		public void valueChanged(TreeSelectionEvent evt)
		{
			TreePath selection = evt.getPath();
			DefaultMutableTreeNode node;
			if(selection == null)
			{
				node = null;
			}
			else
			{
				node = (DefaultMutableTreeNode)
					selection.getLastPathComponent();
			}

			String pathStr;
			String nameStr;
			String authorStr;
			String versionStr;

			if(node != null && node.isLeaf()
				&& node.getUserObject() instanceof Entry)
			{
				remove.setEnabled(true);

				Entry entry = (Entry)node.getUserObject();
				pathStr = entry.path;
				String clazz = entry.clazz;
				if(clazz != null)
				{
					String unknown = jEdit.getProperty(
						"plugin-manager.info.unknown");
					nameStr = entry.toString();
					if(clazz == null)
						authorStr = versionStr = unknown;
					else
					{
						authorStr = jEdit.getProperty("plugin."
							+ clazz + ".author");
						if(authorStr == null)
							authorStr = unknown;
						versionStr = jEdit.getProperty("plugin."
							+ clazz + ".version");
						if(versionStr == null)
							versionStr = unknown;
					}
				}
				else
				{
					nameStr = authorStr = versionStr
						= jEdit.getProperty(
						"plugin-manager.info.not-loaded");
				}
			}
			else
			{
				remove.setEnabled(false);

				pathStr = nameStr = authorStr = versionStr = "";
			}

			path.setText(pathStr);
			name.setText(nameStr);
			author.setText(authorStr);
			version.setText(versionStr);
		}
	}

	class Renderer extends JLabel implements TreeCellRenderer
	{
		Renderer()
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

			setText(value.toString());

			setBorder(hasFocus ? focusBorder : noFocusBorder);

			return this;
		}

		private Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);
		private Border focusBorder = new LineBorder(UIManager.getColor(
			"Tree.selectionBorderColor"), 1);
	}
}
