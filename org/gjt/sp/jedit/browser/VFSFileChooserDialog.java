/*
 * VFSFileChooserDialog.java - VFS file chooser
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

package org.gjt.sp.jedit.browser;

import javax.swing.border.EmptyBorder;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.File;
import java.util.Vector;
import org.gjt.sp.jedit.gui.EnhancedDialog;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.*;

/**
 * Wraps the VFS browser in a modal dialog.
 * @author Slava Pestov
 * @version $Id$
 */
public class VFSFileChooserDialog extends EnhancedDialog
{
	public VFSFileChooserDialog(View view, String path,
		int mode, boolean multipleSelection)
	{
		super(view,jEdit.getProperty("vfs.browser.title"),true);

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		String name;
		if(path == null || path.endsWith(File.separator) || path.endsWith("/"))
			name = null;
		else
		{
			name = MiscUtilities.getFileName(path);
			path = MiscUtilities.getFileParent(path);
		}

		browser = new VFSBrowser(view,path,mode,multipleSelection);
		browser.addBrowserListener(new BrowserHandler());
		content.add(BorderLayout.CENTER,browser);

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
		panel.setBorder(new EmptyBorder(12,0,0,0));

		if(mode == VFSBrowser.SAVE_DIALOG)
		{
			panel.add(new JLabel(jEdit.getProperty("vfs.browser.dialog.filename")));
			panel.add(Box.createHorizontalStrut(12));

			filenameField = new JTextField(20);
			filenameField.setText(name);
			Dimension dim = filenameField.getPreferredSize();
			dim.width = Integer.MAX_VALUE;
			filenameField.setMaximumSize(dim);
			Box box = new Box(BoxLayout.Y_AXIS);
			box.add(Box.createGlue());
			box.add(filenameField);
			box.add(Box.createGlue());
			panel.add(box);

			GUIUtilities.requestFocus(this,filenameField);

			panel.add(Box.createHorizontalStrut(12));
		}
		else
			panel.add(Box.createGlue());

		ok = new JButton(jEdit.getProperty("vfs.browser.dialog."
			+ (mode == VFSBrowser.OPEN_DIALOG ? "open" : "save")));
		ok.addActionListener(new ActionHandler());
		getRootPane().setDefaultButton(ok);
		panel.add(ok);
		panel.add(Box.createHorizontalStrut(6));
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(new ActionHandler());
		panel.add(cancel);

		if(mode != VFSBrowser.SAVE_DIALOG)
			panel.add(Box.createGlue());

		content.add(BorderLayout.SOUTH,panel);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		pack();
		GUIUtilities.loadGeometry(this,"vfs.browser.dialog");
		show();
	}

	public void dispose()
	{
		GUIUtilities.saveGeometry(this,"vfs.browser.dialog");
		super.dispose();
	}

	public void ok()
	{
		if(filenameField != null)
		{
			if(filenameField.getText() == null)
			{
				getToolkit().beep();
				return;
			}
		}
		else
		{
			VFS.DirectoryEntry[] files = browser.getSelectedFiles();
			if(files.length == 0)
				return;

			for(int i = 0; i < files.length; i++)
			{
				VFS.DirectoryEntry file = files[i];
				if(file.type == VFS.DirectoryEntry.FILESYSTEM
					|| file.type == VFS.DirectoryEntry.DIRECTORY)
				{
					// the browser will list the directory
					// in question, so just return
					return;
				}
			}
		}

		isOK = true;
		dispose();
	}

	public void cancel()
	{
		dispose();
	}

	public String[] getSelectedFiles()
	{
		if(!isOK)
			return null;

		if(filenameField != null)
		{
			String directory = browser.getDirectory();
			String[] retVal = { browser.getVFS().constructPath(
				directory,filenameField.getText()) };
			return retVal;
		}
		else
		{
			Vector vector = new Vector();
			VFS.DirectoryEntry[] selectedFiles = browser.getSelectedFiles();
			for(int i = 0; i < selectedFiles.length; i++)
			{
				VFS.DirectoryEntry file =  selectedFiles[i];
				if(file.type == VFS.DirectoryEntry.FILE)
					vector.addElement(file.path);
			}
			String[] retVal = new String[vector.size()];
			vector.copyInto(retVal);
			return retVal;
		}
	}

	public VFSSession getVFSSession()
	{
		return browser.getVFSSession();
	}

	// private members
	private VFSBrowser browser;
	private JTextField filenameField;
	private JButton ok;
	private JButton cancel;
	private boolean isOK;

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(evt.getSource() == ok)
				ok();
			else if(evt.getSource() == cancel)
				cancel();
		}
	}

	class BrowserHandler implements BrowserListener
	{
		public void filesSelected(VFSBrowser browser, VFS.DirectoryEntry[] files)
		{
			if(files.length == 0)
				return;

			if(filenameField != null)
			{
				VFS.DirectoryEntry file = files[0];
				if(file.type != VFS.DirectoryEntry.FILE)
					filenameField.setText(null);
				else
				{
					String path = file.path;
					if(path.startsWith(browser.getDirectory()))
						path = file.name;

					filenameField.setText(path);
				}
			}
		}

		public void filesActivated(VFSBrowser browser, VFS.DirectoryEntry[] files)
		{
			ok();
		}
	}
}

/*
 * Change Log:
 * $Log$
 * Revision 1.5  2000/08/13 07:35:23  sp
 * Dockable window API
 *
 * Revision 1.4  2000/08/10 11:55:58  sp
 * VFS browser toolbar improved a little bit, font selector tweaks
 *
 * Revision 1.3  2000/08/01 11:44:15  sp
 * More VFS browser work
 *
 * Revision 1.2  2000/07/31 11:32:09  sp
 * VFS file chooser is now in a minimally usable state
 *
 * Revision 1.1  2000/07/30 09:04:19  sp
 * More VFS browser hacking
 *
 */
