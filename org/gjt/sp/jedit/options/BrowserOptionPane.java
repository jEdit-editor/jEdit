/*
 * BrowserOptionPane.java - Browser options panel
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

package org.gjt.sp.jedit.options;

import javax.swing.table.*;
import javax.swing.*;
import java.awt.*;
import java.util.Vector;
import org.gjt.sp.jedit.*;

public class BrowserOptionPane extends AbstractOptionPane
{
	public BrowserOptionPane()
	{
		super("browser");
	}

	public void _init()
	{
		/* Default directory */
		String[] dirs = {
			jEdit.getProperty("options.browser.defaultPath.buffer"),
			jEdit.getProperty("options.browser.defaultPath.home"),
			jEdit.getProperty("options.browser.defaultPath.last")
		};

		defaultDirectory = new JComboBox(dirs);
		String defaultDir = jEdit.getProperty("vfs.browser.defaultPath");
		if("buffer".equals(defaultDir))
			defaultDirectory.setSelectedIndex(0);
		else if("home".equals(defaultDir))
			defaultDirectory.setSelectedIndex(1);
		else if("last".equals(defaultDir))
			defaultDirectory.setSelectedIndex(2);
		addComponent(jEdit.getProperty("options.browser.defaultPath"),
			defaultDirectory);

		/* Show hidden files */
		showHiddenFiles = new JCheckBox(jEdit.getProperty("options.browser"
			+ ".showHiddenFiles"));
		showHiddenFiles.setSelected(jEdit.getBooleanProperty("vfs.browser"
			+ ".showHiddenFiles"));
		addComponent(showHiddenFiles);

		/* Sort file list */
		sortFiles = new JCheckBox(jEdit.getProperty("options.browser"
			+ ".sortFiles"));
		sortFiles.setSelected(jEdit.getBooleanProperty("vfs.browser"
			+ ".sortFiles"));
		addComponent(sortFiles);

		/* Ignore case when sorting */
		sortIgnoreCase = new JCheckBox(jEdit.getProperty("options.browser"
			+ ".sortIgnoreCase"));
		sortIgnoreCase.setSelected(jEdit.getBooleanProperty("vfs.browser"
			+ ".sortIgnoreCase"));
		addComponent(sortIgnoreCase);

		/* Mix files and directories */
		sortMixFilesAndDirs = new JCheckBox(jEdit.getProperty("options.browser"
			+ ".sortMixFilesAndDirs"));
		sortMixFilesAndDirs.setSelected(jEdit.getBooleanProperty("vfs.browser"
			+ ".sortMixFilesAndDirs"));
		addComponent(sortMixFilesAndDirs);

		/* Double-click close */
		doubleClickClose = new JCheckBox(jEdit.getProperty("options.browser"
			+ ".doubleClickClose"));
		doubleClickClose.setSelected(jEdit.getBooleanProperty("vfs.browser"
			+ ".doubleClickClose"));
		addComponent(doubleClickClose);
	}

	public void _save()
	{
		String[] dirs = { "buffer", "home", "last" };
		jEdit.setProperty("vfs.browser.defaultPath",dirs[defaultDirectory
			.getSelectedIndex()]);
		jEdit.setBooleanProperty("vfs.browser.showHiddenFiles",
			showHiddenFiles.isSelected());
		jEdit.setBooleanProperty("vfs.browser.sortFiles",
			sortFiles.isSelected());
		jEdit.setBooleanProperty("vfs.browser.sortIgnoreCase",
			sortIgnoreCase.isSelected());
		jEdit.setBooleanProperty("vfs.browser.sortMixFilesAndDirs",
			sortMixFilesAndDirs.isSelected());
		jEdit.setBooleanProperty("vfs.browser.doubleClickClose",
			doubleClickClose.isSelected());
	}

	// private members
	private JComboBox defaultDirectory;
	private JCheckBox showHiddenFiles;
	private JCheckBox sortFiles;
	private JCheckBox sortIgnoreCase;
	private JCheckBox sortMixFilesAndDirs;
	private JCheckBox doubleClickClose;
}

/*
 * Change Log:
 * $Log$
 * Revision 1.6  2000/10/30 07:14:04  sp
 * 2.7pre1 branched, GUI improvements
 *
 * Revision 1.5  2000/09/26 10:19:47  sp
 * Bug fixes, spit and polish
 *
 * Revision 1.4  2000/08/27 02:06:52  sp
 * Filter combo box changed to a text field in VFS browser, passive mode FTP toggle
 *
 * Revision 1.3  2000/08/20 07:29:31  sp
 * I/O and VFS browser improvements
 *
 * Revision 1.2  2000/08/19 08:26:27  sp
 * More docking API tweaks
 *
 * Revision 1.1  2000/08/11 09:06:52  sp
 * Browser option pane
 *
 */
