/*
 * InstallPluginsDialog.java - Plugin install dialog box
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

import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.Vector;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.*;

public class InstallPluginsDialog extends EnhancedDialog
{
	public InstallPluginsDialog(JDialog dialog)
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

		String[] listItems = { jEdit.getProperty("install-plugins.loading") };
		plugins = new JCheckBoxList(listItems);
		//plugins.setVisibleRowCount(8);
		plugins.getSelectionModel().addListSelectionListener(new ListHandler());
		JScrollPane scroller = new JScrollPane(plugins);
		Dimension dim = scroller.getPreferredSize();
		dim.height = 120;
		scroller.setPreferredSize(dim);
		panel.add(BorderLayout.CENTER,scroller);

		JPanel panel2 = new JPanel(new BorderLayout());
		panel2.setBorder(new EmptyBorder(6,0,0,0));
		JPanel labelBox = new JPanel(new GridLayout(6,1,0,3));
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
			+ ".info.requires"),SwingConstants.RIGHT));
		labelBox.add(new JLabel(jEdit.getProperty("install-plugins"
			+ ".info.description"),SwingConstants.RIGHT));
		panel2.add(BorderLayout.WEST,labelBox);

		JPanel valueBox = new JPanel(new GridLayout(6,1,0,3));
		valueBox.setBorder(new EmptyBorder(0,0,3,0));
		valueBox.add(name = new JLabel());
		valueBox.add(author = new JLabel());
		valueBox.add(version = new JLabel());
		valueBox.add(updated = new JLabel());
		valueBox.add(requires = new JLabel());
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
			settings = jEdit.getProperty("install-plugins.info.none");
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

		args[0] = MiscUtilities.constructPath(jEdit.getJEditHome(),"jars");
		installSystem = new JRadioButton(jEdit.getProperty("install-plugins"
			+ ".system",args));
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
		thread = new LoadThread();
		show();
	}

	public void ok()
	{
		if(thread != null)
		{
			thread.stop();
			thread = null;
		}

		dispose();
	}

	public void cancel()
	{
		if(thread != null)
		{
			thread.stop();
			thread = null;
		}

		cancelled = true;

		dispose();
	}

	public String getInstallDirectory()
	{
		if(installUser.isSelected())
			return MiscUtilities.constructPath(jEdit.getSettingsDirectory(),"jars");
		else
			return MiscUtilities.constructPath(jEdit.getJEditHome(),"jars");
	}

	public String[] getPluginURLs()
	{
		if(cancelled)
			return null;

		Vector vector = new Vector();
		Object[] selected = plugins.getCheckedValues();
		for(int i = 0; i < selected.length; i++)
		{
			Object object = selected[i];
			if(object instanceof PluginList.Plugin)
			{
				vector.addElement(((PluginList.Plugin)object).download);
			}
		}

		if(vector.size() == 0)
			return null;

		String[] retVal = new String[vector.size()];
		vector.copyInto(retVal);
		return retVal;
	}

	// private members
	private JCheckBoxList plugins;
	private JLabel name;
	private JLabel author;
	private JLabel version;
	private JLabel updated;
	private JLabel requires;
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
				name.setText(plugin.name);
				author.setText(plugin.author);
				version.setText(plugin.latestVersion);
				updated.setText(plugin.updated);
				requires.setText(plugin.requires);
				description.setText(plugin.description);
			}
			else
			{
				install.setEnabled(false);

				name.setText(null);
				author.setText(null);
				version.setText(null);
				updated.setText(null);
				requires.setText(null);
				description.setText(null);
			}
		}
	}

	class LoadThread extends Thread
	{
		LoadThread()
		{
			super("Plugin list load");
			start();
		}

		public void run()
		{
			PluginList.Plugin[] pluginList = new PluginList(
				InstallPluginsDialog.this).getPlugins();
			if(pluginList == null)
				return;

			// skip plugins that are already installed
			String[] installed = PluginManagerPlugin.getPlugins(false);
			final Vector model = new Vector();
loop:			for(int i = 0; i < pluginList.length; i++)
			{
				String jar = pluginList[i].jar;
				for(int j = 0; j < installed.length; j++)
				{
					if(jar.equals(installed[j]))
						continue loop;
				}
				model.addElement(pluginList[i]);
			}

			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					plugins.setModel(model);
				}
			});

			thread = null;
		}
	}
}
