/*
 * GeneralOptionPane.java - General options panel
 * Copyright (C) 1998, 1999, 2000 Slava Pestov
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

package org.gjt.sp.jedit.options;

import javax.swing.*;
import java.awt.*;
import org.gjt.sp.jedit.*;

public class GeneralOptionPane extends AbstractOptionPane
{
	public GeneralOptionPane()
	{
		super("general");
	}

	// protected members
	protected void _init()
	{
		addSeparator("options.general.misc");

		/* Recent file count */
		recent = new JTextField(jEdit.getProperty("recent"));
		addComponent(jEdit.getProperty("options.general.recent"),recent);

		/* History count */
		history = new JTextField(jEdit.getProperty("history"));
		addComponent(jEdit.getProperty("options.general.history"),history);

		/* Session management */
		saveDesktop = new JCheckBox(jEdit.getProperty(
			"options.general.saveDesktop"));
		saveDesktop.setSelected(jEdit.getBooleanProperty("saveDesktop"));
		addComponent(saveDesktop);

		/* Exit confirmation */
		confirmExit = new JCheckBox(jEdit.getProperty(
			"options.general.confirmExit"));
		confirmExit.setSelected(jEdit.getBooleanProperty("confirmExit"));
		addComponent(confirmExit);

		/* Sort buffers */
		sortBuffers = new JCheckBox(jEdit.getProperty(
			"options.general.sortBuffers"));
		sortBuffers.setSelected(jEdit.getBooleanProperty("sortBuffers"));
		addComponent(sortBuffers);

		/* Sort buffers by names */
		sortByName = new JCheckBox(jEdit.getProperty(
			"options.general.sortByName"));
		sortByName.setSelected(jEdit.getBooleanProperty("sortByName"));
		addComponent(sortByName);

		/* Check mod status on focus */
		checkModStatus = new JCheckBox(jEdit.getProperty(
			"options.general.checkModStatus"));
		checkModStatus.setSelected(jEdit.getBooleanProperty(
			"view.checkModStatus"));
		addComponent(checkModStatus);

		/* Passive-mode FTP */
		passiveFTP = new JCheckBox(jEdit.getProperty(
			"options.general.passiveFTP"));
		passiveFTP.setSelected(jEdit.getBooleanProperty(
			"vfs.ftp.passive"));
		addComponent(passiveFTP);

		addSeparator("options.general.loadsave");

		/* Default file encoding */
		String[] encodings = {
			"ASCII", "8859_1", "UTF8", "Cp850", "Cp1252",
			"MacRoman", "KOI8_R", "Unicode"
		};

		encoding = new JComboBox(encodings);
		encoding.setEditable(true);
		encoding.setSelectedItem(jEdit.getProperty("buffer.encoding",
			System.getProperty("file.encoding")));
		addComponent(jEdit.getProperty("options.general.encoding"),encoding);

		/* Autosave interval */
		autosave = new JTextField(jEdit.getProperty("autosave"));
		addComponent(jEdit.getProperty("options.general.autosave"),autosave);

		/* Backup count */
		backups = new JTextField(jEdit.getProperty("backups"));
		addComponent(jEdit.getProperty("options.general.backups"),backups);

		/* Backup directory */
		backupDirectory = new JTextField(jEdit.getProperty(
			"backup.directory"));
		addComponent(jEdit.getProperty("options.general.backupDirectory"),
			backupDirectory);

		/* Backup filename prefix */
		backupPrefix = new JTextField(jEdit.getProperty("backup.prefix"));
		addComponent(jEdit.getProperty("options.general.backupPrefix"),
			backupPrefix);

		/* Backup suffix */
		backupSuffix = new JTextField(jEdit.getProperty(
			"backup.suffix"));
		addComponent(jEdit.getProperty("options.general.backupSuffix"),
			backupSuffix);

		/* Line separator */
		String[] lineSeps = { jEdit.getProperty("lineSep.unix"),
			jEdit.getProperty("lineSep.windows"),
			jEdit.getProperty("lineSep.mac") };
		lineSeparator = new JComboBox(lineSeps);
		String lineSep = jEdit.getProperty("buffer.lineSeparator",
			System.getProperty("line.separator"));
		if("\n".equals(lineSep))
			lineSeparator.setSelectedIndex(0);
		else if("\r\n".equals(lineSep))
			lineSeparator.setSelectedIndex(1);
		else if("\r".equals(lineSep))
			lineSeparator.setSelectedIndex(2);
		addComponent(jEdit.getProperty("options.general.lineSeparator"),
			lineSeparator);

		/* Number of I/O threads to start */
		ioThreadCount = new JTextField(jEdit.getProperty("ioThreadCount"));
		addComponent(jEdit.getProperty("options.general.ioThreadCount"),
			ioThreadCount);

		addSeparator("options.general.ui");

		/* Look and feel */
		addComponent(new JLabel(jEdit.getProperty("options.general.lf.note")));

		lfs = UIManager.getInstalledLookAndFeels();
		String[] names = new String[lfs.length];
		String lf = UIManager.getLookAndFeel().getClass().getName();
		int index = 0;
		for(int i = 0; i < names.length; i++)
		{
			names[i] = lfs[i].getName();
			if(lf.equals(lfs[i].getClassName()))
				index = i;
		}

		lookAndFeel = new JComboBox(names);
		lookAndFeel.setSelectedIndex(index);

		addComponent(jEdit.getProperty("options.general.lf"),
			lookAndFeel);

		/* Buffer tabs position */
		String[] positions = {
			jEdit.getProperty("options.general.bufferTabsPos.off"),
			jEdit.getProperty("options.general.bufferTabsPos.top"),
			jEdit.getProperty("options.general.bufferTabsPos.left"),
			jEdit.getProperty("options.general.bufferTabsPos.bottom"),
			jEdit.getProperty("options.general.bufferTabsPos.right")
		};

		bufferTabsPos = new JComboBox(positions);
		if(!jEdit.getBooleanProperty("view.showBufferTabs"))
			bufferTabsPos.setSelectedIndex(0);
		else
		{
			bufferTabsPos.setSelectedIndex(Integer.parseInt(jEdit.getProperty(
				"view.bufferTabsPos")));
		}

		addComponent(jEdit.getProperty("options.general.bufferTabsPos"),
			bufferTabsPos);

		/* Show full path */
		showFullPath = new JCheckBox(jEdit.getProperty(
			"options.general.showFullPath"));
		showFullPath.setSelected(jEdit.getBooleanProperty(
			"view.showFullPath"));
		addComponent(showFullPath);
	}

	protected void _save()
	{
		jEdit.setProperty("recent",recent.getText());
		jEdit.setProperty("history",history.getText());
		jEdit.setBooleanProperty("saveDesktop",saveDesktop.isSelected());
		jEdit.setBooleanProperty("confirmExit",confirmExit.isSelected());
		jEdit.setBooleanProperty("sortBuffers",sortBuffers.isSelected());
		jEdit.setBooleanProperty("sortByName",sortByName.isSelected());
		jEdit.setBooleanProperty("view.checkModStatus",checkModStatus
			.isSelected());
		jEdit.setBooleanProperty("vfs.ftp.passive",passiveFTP
			.isSelected());

		jEdit.setProperty("buffer.encoding",(String)
			encoding.getSelectedItem());
		jEdit.setProperty("autosave",autosave.getText());
		jEdit.setProperty("backups",backups.getText());
		jEdit.setProperty("backup.directory",backupDirectory.getText());
		jEdit.setProperty("backup.prefix",backupPrefix.getText());
		jEdit.setProperty("backup.suffix",backupSuffix.getText());
		String lineSep = null;
		switch(lineSeparator.getSelectedIndex())
		{
		case 0:
			lineSep = "\n";
			break;
		case 1:
			lineSep = "\r\n";
			break;
		case 2:
			lineSep = "\r";
			break;
		}
		jEdit.setProperty("buffer.lineSeparator",lineSep);
		jEdit.setProperty("ioThreadCount",ioThreadCount.getText());

		String lf = lfs[lookAndFeel.getSelectedIndex()].getClassName();
		jEdit.setProperty("lookAndFeel",lf);
		int index = bufferTabsPos.getSelectedIndex();
		jEdit.setBooleanProperty("view.showBufferTabs",index != 0);
		if(index != 0)
			jEdit.setProperty("view.bufferTabsPos",String.valueOf(index));
		jEdit.setBooleanProperty("view.showFullPath",showFullPath
			.isSelected());
	}

	// private members
	private JTextField recent;
	private JTextField history;
	private JCheckBox saveDesktop;
	private JCheckBox confirmExit;
	private JCheckBox sortBuffers;
	private JCheckBox sortByName;
	private JCheckBox checkModStatus;
	private JCheckBox passiveFTP;

	private JComboBox encoding;
	private JTextField autosave;
	private JTextField backups;
	private JTextField backupDirectory;
	private JTextField backupPrefix;
	private JTextField backupSuffix;
	private JComboBox lineSeparator;
	private JTextField ioThreadCount;

	private UIManager.LookAndFeelInfo[] lfs;
	private JComboBox lookAndFeel;
	private JComboBox bufferTabsPos;
	private JCheckBox showFullPath;
}

/*
 * Change Log:
 * $Log$
 * Revision 1.45  2000/10/28 00:36:58  sp
 * ML mode, Haskell mode
 *
 * Revision 1.44  2000/10/12 09:28:27  sp
 * debugging and polish
 *
 * Revision 1.43  2000/09/26 10:19:47  sp
 * Bug fixes, spit and polish
 *
 * Revision 1.42  2000/08/27 02:06:52  sp
 * Filter combo box changed to a text field in VFS browser, passive mode FTP toggle
 *
 * Revision 1.41  2000/08/10 11:55:58  sp
 * VFS browser toolbar improved a little bit, font selector tweaks
 *
 * Revision 1.40  2000/08/10 08:30:41  sp
 * VFS browser work, options dialog work, more random tweaks
 *
 * Revision 1.39  2000/08/05 11:41:03  sp
 * More VFS browser work
 *
 * Revision 1.38  2000/08/05 07:16:12  sp
 * Global options dialog box updated, VFS browser now supports right-click menus
 *
 */
