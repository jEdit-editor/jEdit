/*
 * SwingInstall.java - Swing installer
 * Copyright (C) 1999, 2000 Slava Pestov
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
{
	public SwingInstall()
	{
		installer = new SIMInstaller();

		appName = installer.getProperty("app.name");
		appVersion = installer.getProperty("app.version");

		setTitle(appName + " " + appVersion + " installer");

		getContentPane().add(BorderLayout.CENTER,new InstallWizard(
			chooseDirectory = new ChooseDirectory(),
			selectComponents = new SelectComponents()));

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

	// package-private members
	SIMInstaller installer;
	String appName;
	String appVersion;

	ChooseDirectory chooseDirectory;
	SelectComponents selectComponents;

	void install()
	{
		Vector components = new Vector();
		int size = 0;

		JPanel comp = selectComponents.comp;

		for(int i = 0; i < comp.getComponentCount(); i++)
		{
			if(((JCheckBox)comp.getComponent(i))
				.getModel().isSelected())
			{
				size += installer.getIntProperty(
					"comp." + i + ".size");
				components.addElement(installer.getProperty(
					"comp." + i + ".fileset"));
			}
		}

		dispose();

		JTextField binDir = chooseDirectory.binDir;
		String installDir = chooseDirectory.installDir.getText();

		SwingProgress progress = new SwingProgress(appName);
		InstallThread thread = new InstallThread(
			installer,progress,
			(installDir == null ? null : installDir),
			(binDir == null ? null : binDir.getText()),
			size,components);
		progress.setThread(thread);
		thread.start();
	}

	class InstallWizard extends Wizard
	{
		InstallWizard(ChooseDirectory chooseDirectory,
			SelectComponents selectComponents)
		{
			super(new Color(0xccccff),
				new ImageIcon(InstallWizard.class.getResource(
				installer.getProperty("app.logo"))),
				"Cancel","Previous","Next","Install",
				new Component[] { new About(), chooseDirectory,
				selectComponents });
		}

		protected void cancelCallback()
		{
			System.exit(0);
		}

		protected void finishCallback()
		{
			install();
		}
	}
				
	class About extends JPanel
	{
		About()
		{
			super(new BorderLayout());

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

			JScrollPane scrollPane = new JScrollPane(text);
			Dimension dim = scrollPane.getPreferredSize();
			dim.height = 250;
			scrollPane.setPreferredSize(dim);
			add(BorderLayout.CENTER,scrollPane);
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

			JLabel caption = new JLabel("First, specify where "
				+ appName + " is to be installed:");
			Font font = caption.getFont();
			caption.setFont(new Font(font.getFamily(),font.getStyle(),18));

			add(BorderLayout.NORTH,caption);

			String _binDir = OperatingSystem.getOperatingSystem()
				.getShortcutDirectory();

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
			installDir.setText(OperatingSystem.getOperatingSystem()
				.getInstallDirectory(appName,appVersion));

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
		JPanel comp;
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

			JLabel caption = new JLabel("Now, specify program"
				+ " components to install:");
			Font font = caption.getFont();
			caption.setFont(new Font(font.getFamily(),font.getStyle(),18));

			layout.setConstraints(caption,cons);
			add(caption);

			comp = createCompPanel();

			cons.gridy++;
			layout.setConstraints(comp,cons);
			add(comp);

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

		private JPanel createCompPanel()
		{
			int count = installer.getIntProperty("comp.count");
			JPanel panel = new JPanel(new GridLayout(count,1));

			for(int i = 0; i < count; i++)
			{
				JCheckBox checkBox = new JCheckBox(
					installer.getProperty("comp." + i + ".name")
					+ " (" + installer.getProperty("comp." + i + ".size")
					+ "Kb)");
				checkBox.getModel().setSelected(true);
				checkBox.addActionListener(this);
				checkBox.setRequestFocusEnabled(false);
				panel.add(checkBox);
			}

			return panel;
		}

		private void updateSize()
		{
			int size = 0;

			for(int i = 0; i < comp.getComponentCount(); i++)
			{
				if(((JCheckBox)comp.getComponent(i))
					.getModel().isSelected())
				{
					size += installer.getIntProperty(
						"comp." + i + ".size");
				}
			}

			sizeLabel.setText("Estimated disk usage of selected"
				+ " components: " + size + "Kb");
		}
	}
}
