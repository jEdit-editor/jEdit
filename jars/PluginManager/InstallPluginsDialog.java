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

import javax.swing.event.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.Vector;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.*;

public class InstallPluginsDialog extends EnhancedDialog
{
	public InstallPluginsDialog(View view)
	{
		super(view,jEdit.getProperty("install-plugins.title"),true);

		this.view = view;

		getContentPane().add(BorderLayout.NORTH,new JLabel(
			jEdit.getProperty("install-plugins.caption")));

		JPanel panel = new JPanel(new BorderLayout());

		String[] listItems = { jEdit.getProperty("install-plugins.loading") };
		plugins = new JList(listItems);
		plugins.setVisibleRowCount(8);
		plugins.addListSelectionListener(new ListHandler());
		panel.add(BorderLayout.CENTER,new JScrollPane(plugins));

		JPanel panel2 = new JPanel(new BorderLayout());
		JPanel labelBox = new JPanel(new GridLayout(6,1));
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

		JPanel valueBox = new JPanel(new GridLayout(6,1));
		valueBox.add(name = new JLabel());
		valueBox.add(author = new JLabel());
		valueBox.add(version = new JLabel());
		valueBox.add(updated = new JLabel());
		valueBox.add(requires = new JLabel());
		valueBox.add(Box.createGlue());
		panel2.add(BorderLayout.CENTER,valueBox);

		JPanel panel3 = new JPanel(new BorderLayout());
		description = new JTextArea(6,30);
		description.setEditable(false);
		description.setLineWrap(true);
		description.setWrapStyleWord(true);
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
			installUser.getModel().setSelected(true);
		else
			installSystem.getModel().setSelected(true);

		panel2.add(BorderLayout.SOUTH,panel3);

		panel.add(BorderLayout.SOUTH,panel2);

		getContentPane().add(BorderLayout.CENTER,panel);

		JPanel buttons = new JPanel();

		install = new JButton(jEdit.getProperty("install-plugins.install"));
		install.setEnabled(false);
		getRootPane().setDefaultButton(install);
		install.addActionListener(new ActionHandler());
		buttons.add(install);

		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(new ActionHandler());
		buttons.add(cancel);

		getContentPane().add(BorderLayout.SOUTH,buttons);

		thread = new LoadThread();

		pack();
		Dimension screen = getToolkit().getScreenSize();
		pack();
		setLocation((screen.width - getSize().width) / 2,
			(screen.height - getSize().height) / 2);
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
		if(installUser.getModel().isSelected())
			return MiscUtilities.constructPath(jEdit.getSettingsDirectory(),"jars");
		else
			return MiscUtilities.constructPath(jEdit.getJEditHome(),"jars");
	}

	public String[] getPluginURLs()
	{
		if(cancelled)
			return null;

		Vector vector = new Vector();
		Object[] selected = plugins.getSelectedValues();
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
	private View view;

	private JList plugins;
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
			PluginList.Plugin[] pluginList = new PluginList(view,true)
				.getPlugins();

			// skip plugins that are already installed
			String[] installed = PluginManagerPlugin.getPlugins();
			final DefaultListModel model = new DefaultListModel();
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
