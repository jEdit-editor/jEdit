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

		/* Show full path */
		showFullPath = new JCheckBox(jEdit.getProperty(
			"options.general.showFullPath"));
		showFullPath.setSelected(jEdit.getBooleanProperty(
			"view.showFullPath"));
		addComponent(showFullPath);

		/* Passive-mode FTP */
		passiveFTP = new JCheckBox(jEdit.getProperty(
			"options.general.passiveFTP"));
		passiveFTP.setSelected(jEdit.getBooleanProperty(
			"vfs.ftp.passive"));
		addComponent(passiveFTP);
	}

	protected void _save()
	{
		String lf = lfs[lookAndFeel.getSelectedIndex()].getClassName();
		jEdit.setProperty("lookAndFeel",lf);
		jEdit.setProperty("recent",recent.getText());
		jEdit.setProperty("history",history.getText());
		jEdit.setBooleanProperty("saveDesktop",saveDesktop.isSelected());
		jEdit.setBooleanProperty("confirmExit",confirmExit.isSelected());
		jEdit.setBooleanProperty("sortBuffers",sortBuffers.isSelected());
		jEdit.setBooleanProperty("sortByName",sortByName.isSelected());
		jEdit.setBooleanProperty("view.checkModStatus",checkModStatus
			.isSelected());
		jEdit.setBooleanProperty("view.showFullPath",showFullPath
			.isSelected());
		jEdit.setBooleanProperty("vfs.ftp.passive",passiveFTP
			.isSelected());
	}

	// private members
	private UIManager.LookAndFeelInfo[] lfs;
	private JComboBox lookAndFeel;
	private JTextField recent;
	private JTextField history;
	private JCheckBox saveDesktop;
	private JCheckBox confirmExit;
	private JCheckBox sortBuffers;
	private JCheckBox sortByName;
	private JCheckBox checkModStatus;
	private JCheckBox showFullPath;
	private JCheckBox passiveFTP;
}

/*
 * Change Log:
 * $Log$
 * Revision 1.47  2000/11/07 10:08:32  sp
 * Options dialog improvements, documentation changes, bug fixes
 *
 * Revision 1.46  2000/10/30 07:14:04  sp
 * 2.7pre1 branched, GUI improvements
 *
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
