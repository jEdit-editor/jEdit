/*
 * SwingInstall.java - Swing installer
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

package org.gjt.sp.sim;

import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

public class SwingInstall extends JFrame
implements ActionListener, ChangeListener
{
	public SwingInstall()
	{
		installer = new SIMInstaller();

		os = OperatingSystem.getOperatingSystem();

		appName = installer.getProperty("app.name");
		appVersion = installer.getProperty("app.version");

		setTitle("SIM - installing " + appName);

		JLabel imgLabel = new JLabel(createIcon(installer.getProperty(
			"app.logo")));
		imgLabel.setBorder(new SoftBevelBorder(SoftBevelBorder.LOWERED));
		getContentPane().add(BorderLayout.WEST,imgLabel);

		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons,BoxLayout.X_AXIS));
		exit = new JButton("Exit",createIcon("Exit.gif"));
		exit.addActionListener(this);
		exit.setRequestFocusEnabled(false);
		prev = new JButton("Previous",createIcon("Left.gif"));
		prev.addActionListener(this);
		prev.setRequestFocusEnabled(false);
		next = new JButton();
		nextIcon = createIcon("Right.gif");
		installIcon = createIcon("SaveDB.gif");
		next.addActionListener(this);
		next.setRequestFocusEnabled(false);

		exit.setPreferredSize(prev.getPreferredSize());
		next.setPreferredSize(prev.getPreferredSize());

		buttons.add(exit);
		buttons.add(Box.createGlue());
		buttons.add(prev);
		buttons.add(Box.createHorizontalStrut(10));
		buttons.add(next);
		buttons.setBorder(new EmptyBorder(10,10,10,10));

		tabs = new JTabbedPane();
		tabs.addTab("About " + appName,createIcon("Help.gif"),new About());
		tabs.addTab("Install Directory",createIcon("FolderIn.gif"),
			chooseDirTab = new ChooseDirectory());
		tabs.addTab("Components to Install",createIcon("SaveAll.gif"),
			selectCompTab = new SelectComponents());
		getContentPane().add(BorderLayout.CENTER,tabs);

		// update button enabled/disabled state
		tabs.addChangeListener(this);
		updatePrevNext();

		getContentPane().add(BorderLayout.SOUTH,buttons);

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowHandler());

		Dimension screen = getToolkit().getScreenSize();
		pack();
		setLocation((screen.width - getSize().width) / 2,
			(screen.height - getSize().height) / 2);
		show();
	}

	class WindowHandler extends WindowAdapter
	{
		public void windowClosing(WindowEvent evt)
		{
			System.exit(0);
		}
	}

	public void actionPerformed(ActionEvent evt)
	{
		if(evt.getSource() == exit)
			System.exit(0);
		else if(evt.getSource() == prev)
			tabs.setSelectedIndex(tabs.getSelectedIndex() - 1);
		else if(evt.getSource() == next)
		{
			if(tabs.getSelectedIndex() == tabs.getComponentCount() - 1)
				install();
			else
				tabs.setSelectedIndex(tabs.getSelectedIndex() + 1);
		}
	}

	public void stateChanged(ChangeEvent evt)
	{
		updatePrevNext();
	}

	// package-private members
	SIMInstaller installer;
	OperatingSystem os;
	String appName;
	String appVersion;

	JTabbedPane tabs;
	ChooseDirectory chooseDirTab;
	SelectComponents selectCompTab;

	JButton exit;
	JButton prev;
	JButton next;

	ImageIcon nextIcon;
	ImageIcon installIcon;

	ImageIcon createIcon(String name)
	{
		return new ImageIcon(getClass().getResource(name));
	}

	void updatePrevNext()
	{
		int index = tabs.getSelectedIndex();
		prev.setEnabled(index != 0);
		next.setText(index == tabs.getComponentCount() - 1
			? "Install" : "Next");
		next.setIcon(index == tabs.getComponentCount() - 1
			? installIcon : nextIcon);
	}

	void install()
	{
		Vector components = new Vector();
		int size = 0;

		JPanel userComp = selectCompTab.userComp;

		for(int i = 0; i < userComp.getComponentCount(); i++)
		{
			if(((JCheckBox)userComp.getComponent(i))
				.getModel().isSelected())
			{
				size += installer.getIntProperty(
					"comp.user." + i + ".size");
				components.addElement(installer.getProperty(
					"comp.user." + i + ".fileset"));
			}
		}

		JPanel develComp = selectCompTab.develComp;

		for(int i = 0; i < develComp.getComponentCount(); i++)
		{
			if(((JCheckBox)develComp.getComponent(i))
				.getModel().isSelected())
			{
				size += installer.getIntProperty(
					"comp.devel." + i + ".size");
				components.addElement(installer.getProperty(
					"comp.devel." + i + ".fileset"));
			}
		}

		dispose();

		JTextField binDir = chooseDirTab.binDir;
		String installDir = chooseDirTab.installDir.getText();

		SwingProgress progress = new SwingProgress(appName);
		InstallThread thread = new InstallThread(
			installer,os,progress,
			(installDir == null ? null : installDir),
			(binDir == null ? null : binDir.getText()),
			size,components);
		progress.setThread(thread);
		thread.start();
	}

	class About extends JPanel
	{
		About()
		{
			setLayout(new BorderLayout());

			JLabel caption = new JLabel(appName + " "
				+ appVersion + " installer");
			Font font = caption.getFont();
			caption.setFont(new Font(font.getFamily(),font.getStyle(),24));

			add(BorderLayout.NORTH,caption);

			JEditorPane text = new JEditorPane();
			String readme = installer.getProperty("app.readme");

			try
			{
				text.setPage(getClass().getResource(readme));
				text.setEditable(false);
			}
			catch(IOException io)
			{
				text.setText("Error loading '" + readme + "'");
				io.printStackTrace();
			}

			add(BorderLayout.CENTER,new JScrollPane(text));
		}
	}

	class ChooseDirectory extends JPanel
	implements ActionListener
	{
		JTextField installDir;
		JButton chooseInstall;
		JTextField binDir;
		JButton chooseBin;

		ChooseDirectory()
		{
			setLayout(new BorderLayout());

			JLabel caption = new JLabel("Step 1 - specify where "
				+ appName + " is to be installed");
			Font font = caption.getFont();
			caption.setFont(new Font(font.getFamily(),font.getStyle(),24));

			add(BorderLayout.NORTH,caption);

			String _binDir = os.getShortcutDirectory();

			Box box = new Box(BoxLayout.Y_AXIS);

			Box fieldBox = new Box(BoxLayout.X_AXIS);

			Box labels = new Box(BoxLayout.Y_AXIS);
			labels.add(new JLabel("Install program in: ",SwingConstants.RIGHT));
			if(_binDir != null)
			{
				labels.add(Box.createVerticalStrut(5));
				labels.add(new JLabel("Install shortcut in: ",
					SwingConstants.RIGHT));
			}

			fieldBox.add(labels);

			Box fields = new Box(BoxLayout.Y_AXIS);
			fields.add(installDir = new JTextField());
			installDir.setText(os.getInstallDirectory(appName,appVersion));

			fields.add(Box.createVerticalStrut(5));
			if(_binDir != null)
				fields.add(binDir = new JTextField(_binDir));

			fieldBox.add(fields);

			box.add(fieldBox);

			JPanel buttons = new JPanel();
			chooseInstall = new JButton("Choose Install Directory...");
			chooseInstall.setRequestFocusEnabled(false);
			chooseInstall.addActionListener(this);
			buttons.add(chooseInstall);

			if(_binDir != null)
			{
				chooseBin = new JButton("Choose Shortcut Directory...");
				chooseBin.setRequestFocusEnabled(false);
				chooseBin.addActionListener(this);
				buttons.add(chooseBin);
			}

			box.add(buttons);

			JPanel panel = new JPanel();
			panel.add(box);
			add(BorderLayout.CENTER,panel);
		}

		public void actionPerformed(ActionEvent evt)
		{
			JTextField field = (evt.getSource() == chooseInstall
				? installDir : binDir);

			File directory = new File(field.getText());
			JFileChooser chooser = new JFileChooser(directory.getParent());
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setSelectedFile(directory);

			if(chooser.showOpenDialog(SwingInstall.this)
				== JFileChooser.APPROVE_OPTION)
				field.setText(chooser.getSelectedFile().getPath());
		}
	}

	class SelectComponents extends JPanel
	implements ActionListener
	{
		JPanel userComp;
		JPanel develComp;
		JLabel sizeLabel;

		SelectComponents()
		{
			GridBagLayout layout = new GridBagLayout();
			setLayout(layout);
			GridBagConstraints cons = new GridBagConstraints();
			cons.anchor = GridBagConstraints.NORTHWEST;
			cons.fill = GridBagConstraints.HORIZONTAL;
			cons.weightx = 1.0f;
			cons.weighty = 1.0f;
			cons.gridy = 1;

			JLabel caption = new JLabel("Step 2 - specify"
				+ " components to install");
			Font font = caption.getFont();
			caption.setFont(new Font(font.getFamily(),font.getStyle(),24));

			layout.setConstraints(caption,cons);
			add(caption);

			userComp = createUserPanel();
			userComp.setBorder(new TitledBorder("Components for users"));

			cons.gridy++;
			layout.setConstraints(userComp,cons);
			add(userComp);

			develComp = createDevelPanel();
			develComp.setBorder(new TitledBorder("Components for developers"));

			if(develComp.getComponentCount() != 0)
			{
				cons.gridy++;
				layout.setConstraints(develComp,cons);
				add(develComp);
			}

			sizeLabel = new JLabel("",SwingConstants.LEFT);
			cons.gridy++;
			layout.setConstraints(sizeLabel,cons);
			add(sizeLabel);

			updateSize();
		}

		public void actionPerformed(ActionEvent evt)
		{
			updateSize();
		}

		private JPanel createUserPanel()
		{
			int count = installer.getIntProperty("comp.user.count");
			JPanel panel = new JPanel(new GridLayout(count,1));

			for(int i = 0; i < count; i++)
			{
				JCheckBox checkBox = new JCheckBox(
					installer.getProperty("comp.user." + i + ".name")
					+ " (" + installer.getProperty("comp.user." + i + ".size")
					+ "Kb)");
				checkBox.getModel().setSelected(true);
				checkBox.addActionListener(this);
				panel.add(checkBox);
			}

			return panel;
		}

		private JPanel createDevelPanel()
		{
			int count = installer.getIntProperty("comp.devel.count");
			JPanel panel = new JPanel(new GridLayout(count,1));

			for(int i = 0; i < count; i++)
			{
				JCheckBox checkBox = new JCheckBox(
					installer.getProperty("comp.devel." + i + ".name")
					+ " (" + installer.getProperty("comp.devel." + i + ".size")
					+ "Kb)");
				checkBox.getModel().setSelected(true);
				checkBox.addActionListener(this);
				panel.add(checkBox);
			}

			return panel;
		}

		private void updateSize()
		{
			int size = 0;

			for(int i = 0; i < userComp.getComponentCount(); i++)
			{
				if(((JCheckBox)userComp.getComponent(i))
					.getModel().isSelected())
				{
					size += installer.getIntProperty(
						"comp.user." + i + ".size");
				}
			}

			for(int i = 0; i < develComp.getComponentCount(); i++)
			{
				if(((JCheckBox)develComp.getComponent(i))
					.getModel().isSelected())
				{
					size += installer.getIntProperty(
						"comp.devel." + i + ".size");
				}
			}

			sizeLabel.setText("Estimated disk usage of selected"
				+ " components: " + size + "Kb");
		}
	}
}
