/*
 * AbooutDialog.java - About jEdit dialog box
 * Copyright (C) 2000 Slava Pestov
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
import java.io.File;
import java.util.*;
import org.gjt.sp.jedit.*;

public class AboutDialog extends EnhancedDialog
{
	public AboutDialog(View view)
	{
		super(view,jEdit.getProperty("about.title"),true);

		view.showWaitCursor();

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab(jEdit.getProperty("about.about"),new AboutPane());
		tabs.addTab(jEdit.getProperty("about.plugins"),new PluginsPane());
		getContentPane().add(BorderLayout.CENTER,tabs);

		JPanel panel = new JPanel();
		ok = new JButton(jEdit.getProperty("common.ok"));
		ok.addActionListener(new ActionHandler());
		getRootPane().setDefaultButton(ok);
		panel.add(ok);
		getContentPane().add(BorderLayout.SOUTH,panel);

		Dimension screen = getToolkit().getScreenSize();
		pack();
		setLocation((screen.width - getSize().width) / 2,
			(screen.height - getSize().height) / 2);
		show();

		view.hideWaitCursor();
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
	private JButton ok;

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			dispose();
		}
	}

	class AboutPane extends JPanel
	{
		AboutPane()
		{
			super(new BorderLayout());
			AboutPane.this.add(BorderLayout.NORTH,new JLabel(
				new ImageIcon(AboutPane.class.getResource(
				"/org/gjt/sp/jedit/jedit_logo.jpg"))));
			JPanel panel = new JPanel(new GridLayout(0,1));
			String[] args = { jEdit.getVersion() };
			StringTokenizer st = new StringTokenizer(
				jEdit.getProperty("about.message",args),"\n");
			while(st.hasMoreTokens())
			{
				panel.add(new JLabel(st.nextToken(),
					SwingConstants.CENTER));
			}
			AboutPane.this.add(BorderLayout.CENTER,panel);
		}
	}

	class PluginsPane extends JPanel
	{
		PluginsPane()
		{
			super(new BorderLayout());

			JTree tree = new JTree();

			DefaultMutableTreeNode treeRoot =
				new DefaultMutableTreeNode(jEdit.getProperty(
					"about.plugins"),true);
			DefaultTreeModel treeModel = new DefaultTreeModel(treeRoot);

			String systemDir = MiscUtilities.constructPath(
				jEdit.getJEditHome(),"jars");
			DefaultMutableTreeNode sysTree = new DefaultMutableTreeNode(
				systemDir,true);

			EditPlugin[] plugins = jEdit.getPlugins();
			Vector sysPlugins = new Vector();
			Vector userPlugins = new Vector();

			for(int i = 0; i < plugins.length; i++)
			{
				EditPlugin plugin = plugins[i];
				JARClassLoader classLoader = (JARClassLoader)plugin
					.getClass().getClassLoader();
				String path = classLoader.getPath();
				String[] args = { plugin.getClass().getName(),
					new File(path).getName() };
				String label = jEdit.getProperty(
						"about.plugins.label",args);
				if(path.startsWith(systemDir))
					sysPlugins.addElement(label);
				else
					userPlugins.addElement(label);
			}

			for(int i = 0; i < sysPlugins.size(); i++)
			{
				sysTree.insert(new DefaultMutableTreeNode(
					sysPlugins.elementAt(i),false),i);
			}

			if(sysPlugins.size() == 0)
			{
				sysTree.insert(new DefaultMutableTreeNode(
					jEdit.getProperty("about.plugins.none"),
					false),0);
			}

			treeRoot.insert(sysTree,0);

			String settingsDir = jEdit.getSettingsDirectory();
			if(settingsDir != null)
			{
				settingsDir = MiscUtilities.constructPath(
					settingsDir,"jars");
				DefaultMutableTreeNode userTree = new DefaultMutableTreeNode(
					settingsDir,true);
				for(int i = 0; i < userPlugins.size(); i++)
				{
					userTree.insert(new DefaultMutableTreeNode(
						userPlugins.elementAt(i),false),i);
				}

				if(userPlugins.size() == 0)
				{
					userTree.insert(new DefaultMutableTreeNode(
						jEdit.getProperty("about.plugins.none"),
						false),0);
				}

				treeRoot.insert(userTree,1);
			}

			tree.setModel(treeModel);
			for(int i = 0; i < tree.getRowCount(); i++)
				tree.expandRow(i);

			PluginsPane.this.add(BorderLayout.CENTER,new JScrollPane(tree));
			PluginsPane.this.setPreferredSize(new Dimension(0,0));
		}
	}
}
