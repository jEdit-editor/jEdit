/*
 * InstallPluginsDialog.java - Plugin install dialog box
 * Copyright (C) 2000, 2001 Slava Pestov
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

import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.Vector;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.*;

class InstallPluginsDialog extends EnhancedDialog
{
	InstallPluginsDialog(JDialog dialog, PluginList pluginList)
	{
		super(JOptionPane.getFrameForComponent(dialog),
			jEdit.getProperty("install-plugins.title"),true);

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		JLabel label = new JLabel(jEdit.getProperty("install-plugins.caption"));
		label.setBorder(new EmptyBorder(0,0,6,0));
		content.add(BorderLayout.NORTH,label);

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(new EmptyBorder(0,0,12,0));

		Vector model = new Vector();
		for(int i = 0; i < pluginList.plugins.size(); i++)
		{
			PluginList.Plugin plugin = (PluginList.Plugin)pluginList
				.plugins.elementAt(i);
			if(plugin.installed == null
				&& plugin.canBeInstalled())
				model.addElement(plugin);
		}
		plugins = new JCheckBoxList(model);
		//plugins.setVisibleRowCount(8);
		plugins.getSelectionModel().addListSelectionListener(new ListHandler());
		JScrollPane scroller = new JScrollPane(plugins);
		Dimension dim = scroller.getPreferredSize();
		dim.height = 120;
		scroller.setPreferredSize(dim);
		panel.add(BorderLayout.CENTER,scroller);

		JPanel panel2 = new JPanel(new BorderLayout());
		panel2.setBorder(new EmptyBorder(6,0,0,0));
		JPanel labelBox = new JPanel(new GridLayout(5,1,0,3));
		labelBox.setBorder(new EmptyBorder(0,0,3,12));
		labelBox.add(new JLabel(jEdit.getProperty("install-plugins"
			+ ".info.name"),SwingConstants.RIGHT));
		labelBox.add(new JLabel(jEdit.getProperty("install-plugins"
			+ ".info.author"),SwingConstants.RIGHT));
		labelBox.add(new JLabel(jEdit.getProperty("install-plugins"
			+ ".info.version"),SwingConstants.RIGHT));
		labelBox.add(new JLabel(jEdit.getProperty("install-plugins"
			+ ".info.updated"),SwingConstants.RIGHT));
		labelBox.add(new JLabel(jEdit.getProperty("install-plugins"
			+ ".info.description"),SwingConstants.RIGHT));
		panel2.add(BorderLayout.WEST,labelBox);

		JPanel valueBox = new JPanel(new GridLayout(5,1,0,3));
		valueBox.setBorder(new EmptyBorder(0,0,3,0));
		valueBox.add(name = new JLabel());
		valueBox.add(author = new JLabel());
		valueBox.add(version = new JLabel());
		valueBox.add(updated = new JLabel());
		valueBox.add(Box.createGlue());
		panel2.add(BorderLayout.CENTER,valueBox);

		JPanel panel3 = new JPanel(new BorderLayout(0,3));
		description = new JTextArea(6,30);
		description.setEditable(false);
		description.setLineWrap(true);
		description.setWrapStyleWord(true);
		JPanel panel4 = new JPanel(new BorderLayout());
		panel3.add(BorderLayout.NORTH,new JScrollPane(description));

		ButtonGroup grp = new ButtonGroup();
		installUser = new JRadioButton();
		String settings = jEdit.getSettingsDirectory();
		if(settings == null)
		{
			settings = jEdit.getProperty("install-plugins.none");
			installUser.setEnabled(false);
		}
		else
		{
			settings = MiscUtilities.constructPath(settings,"jars");
			installUser.setEnabled(true);
		}
		String[] args = { settings };
		installUser.setText(jEdit.getProperty("install-plugins.user",args));
		grp.add(installUser);
		panel3.add(BorderLayout.CENTER,installUser);

		installSystem = new JRadioButton();
		String jEditHome = jEdit.getJEditHome();
		if(settings == null)
		{
			jEditHome = jEdit.getProperty("install-plugins.none");
			installSystem.setEnabled(false);
		}
		else
		{
			jEditHome = MiscUtilities.constructPath(jEditHome,"jars");
			installSystem.setEnabled(true);
		}
		installSystem.setText(jEdit.getProperty("install-plugins.system",args));
		grp.add(installSystem);
		panel3.add(BorderLayout.SOUTH,installSystem);

		if(installUser.isEnabled())
			installUser.setSelected(true);
		else
			installSystem.setSelected(true);

		panel2.add(BorderLayout.SOUTH,panel3);

		panel.add(BorderLayout.SOUTH,panel2);

		content.add(BorderLayout.CENTER,panel);

		Box box = new Box(BoxLayout.X_AXIS);

		box.add(Box.createGlue());
		install = new JButton(jEdit.getProperty("install-plugins.install"));
		install.setEnabled(false);
		getRootPane().setDefaultButton(install);
		install.addActionListener(new ActionHandler());
		box.add(install);
		box.add(Box.createHorizontalStrut(6));

		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(new ActionHandler());
		box.add(cancel);
		box.add(Box.createHorizontalStrut(6));
		box.add(Box.createGlue());

		content.add(BorderLayout.SOUTH,box);

		pack();
		setLocationRelativeTo(dialog);
		show();
	}

	public void ok()
	{
		dispose();
	}

	public void cancel()
	{
		cancelled = true;

		dispose();
	}

	void installPlugins(Roster roster)
	{
		if(cancelled)
			return;

		String installDirectory;
		if(installUser.isSelected())
		{
			installDirectory = MiscUtilities.constructPath(
				jEdit.getSettingsDirectory(),"jars");
		}
		else
		{
			installDirectory = MiscUtilities.constructPath(
				jEdit.getJEditHome(),"jars");
		}

		Object[] selected = plugins.getCheckedValues();
		for(int i = 0; i < selected.length; i++)
		{
			PluginList.Plugin plugin = (PluginList.Plugin)selected[i];
			plugin.install(roster,installDirectory);
		}
	}

	// private members
	private JCheckBoxList plugins;
	private JLabel name;
	private JLabel author;
	private JLabel version;
	private JLabel updated;
	private JTextArea description;
	private JRadioButton installUser;
	private JRadioButton installSystem;

	private JButton install;
	private JButton cancel;

	private boolean cancelled;
	private Thread thread;

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(evt.getSource() == install)
				ok();
			else
				cancel();
		}
	}

	class ListHandler implements ListSelectionListener
	{
		public void valueChanged(ListSelectionEvent evt)
		{
			Object selected = plugins.getSelectedValue();
			if(selected instanceof PluginList.Plugin)
			{
				install.setEnabled(true);

				PluginList.Plugin plugin = (PluginList.Plugin)selected;
				PluginList.Branch branch = plugin.getCompatibleBranch();
				name.setText(plugin.name);
				author.setText(plugin.author);
				version.setText(branch.version);
				updated.setText(branch.date);
				description.setText(plugin.description);
			}
			else
			{
				install.setEnabled(false);

				name.setText(null);
				author.setText(null);
				version.setText(null);
				updated.setText(null);
				description.setText(null);
			}
		}
	}
}
