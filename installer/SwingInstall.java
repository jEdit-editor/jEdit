/*
 * SwingInstall.java
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

package installer;

import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

/*
 * Graphical front-end to installer.
 */
public class SwingInstall extends JFrame
{
	public SwingInstall()
	{
		installer = new Install();

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
	Install installer;
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
				text.setText("Loading '" + readme + "'...");
				text.setPage(getClass().getResource(readme));
			}
			catch(IOException io)
			{
				text.setText("Error loading '" + readme + "'");
				io.printStackTrace();
			}

			text.setEditable(false);

			JScrollPane scrollPane = new JScrollPane(text);
			Dimension dim = new Dimension();
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
			super(new BorderLayout());

			JLabel caption = new JLabel("First, specify where "
				+ appName + " is to be installed:");
			Font font = caption.getFont();
			caption.setFont(new Font(font.getFamily(),font.getStyle(),18));

			add(BorderLayout.NORTH,caption);

			Box box = new Box(BoxLayout.Y_AXIS);

			String _binDir = OperatingSystem.getOperatingSystem()
				.getShortcutDirectory(appName,appVersion);

			JPanel directoryPanel = new JPanel();
			GridBagLayout layout = new GridBagLayout();
			directoryPanel.setLayout(layout);
			GridBagConstraints cons = new GridBagConstraints();
			cons.anchor = GridBagConstraints.WEST;
			cons.fill = GridBagConstraints.HORIZONTAL;
			cons.gridy = 1;
			cons.insets = new Insets(0,0,6,0);

			JLabel label = new JLabel("Install program in: ",SwingConstants.RIGHT);
			label.setBorder(new EmptyBorder(0,0,0,12));
			layout.setConstraints(label,cons);
			directoryPanel.add(label);

			cons.weightx = 1.0f;
			installDir = new JTextField();
			installDir.setText(OperatingSystem.getOperatingSystem()
				.getInstallDirectory(appName,appVersion));
			layout.setConstraints(installDir,cons);
			directoryPanel.add(installDir);

			if(_binDir != null)
			{
				cons.gridy = 2;
				cons.weightx = 0.0f;
				cons.insets = new Insets(0,0,0,0);
				label = new JLabel("Install shortcut in: ",SwingConstants.RIGHT);
				label.setBorder(new EmptyBorder(0,0,0,12));
				layout.setConstraints(label,cons);
				directoryPanel.add(label);

				cons.weightx = 1.0f;
				binDir = new JTextField(_binDir);
				layout.setConstraints(binDir,cons);
				directoryPanel.add(binDir);
			}

			box.add(directoryPanel);

			Box buttons = new Box(BoxLayout.X_AXIS);
			chooseInstall = new JButton("Choose Install Directory...");
			chooseInstall.setRequestFocusEnabled(false);
			chooseInstall.addActionListener(this);
			buttons.add(chooseInstall);
			if(_binDir != null)
			{
				buttons.add(Box.createHorizontalStrut(6));
				chooseBin = new JButton("Choose Shortcut Directory...");
				chooseBin.setRequestFocusEnabled(false);
				chooseBin.addActionListener(this);
				buttons.add(chooseBin);
			}
			box.add(buttons);

			box.add(Box.createGlue());

			add(BorderLayout.CENTER,box);
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
			super(new BorderLayout());

			JLabel caption = new JLabel("Now, specify program"
				+ " components to install:");
			Font font = caption.getFont();
			caption.setFont(new Font(font.getFamily(),font.getStyle(),18));

			add(BorderLayout.NORTH,caption);

			Box box = new Box(BoxLayout.Y_AXIS);
			box.add(Box.createGlue());
			box.add(comp = createCompPanel());
			box.add(Box.createGlue());

			add(BorderLayout.CENTER,box);

			sizeLabel = new JLabel("",SwingConstants.LEFT);
			add(BorderLayout.SOUTH,sizeLabel);

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

			Dimension dim = panel.getPreferredSize();
			dim.width = Integer.MAX_VALUE;
			panel.setMaximumSize(dim);

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
