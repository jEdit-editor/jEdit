/*
 * VFSBrowser.java - VFS browser
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
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.jedit.msg.ViewUpdate;
import org.gjt.sp.jedit.*;

/**
 * The main class of the VFS browser.
 * @author Slava Pestov
 * @version $Id$
 */
public class VFSBrowser extends JPanel implements EBComponent
{
	/**
	 * Creates a new VFS browser.
	 * @param view The view to open buffers in by default
	 * @param path The path to display
	 */
	public VFSBrowser(View view, String path)
	{
		super(new BorderLayout());

		this.view = view;

		JPanel pathPanel = new JPanel(new BorderLayout());
		JLabel pathLabel = new JLabel(jEdit.getProperty("vfs.browser.directory"));
		pathLabel.setBorder(new EmptyBorder(0,0,0,12));
		pathPanel.add(BorderLayout.WEST,pathLabel);
		pathField = new HistoryTextField("vfs.browser.path",true);
		pathField.addActionListener(new ActionHandler());
		pathPanel.add(BorderLayout.CENTER,pathField);
		add(BorderLayout.NORTH,pathPanel);

		fileList = new JList();
		fileList.setVisibleRowCount(8);
		add(BorderLayout.CENTER,new JScrollPane(fileList));

		gotoDirectory(path);
	}

	public void addNotify()
	{
		super.addNotify();
		EditBus.addToBus(this);
	}

	public void removeNotify()
	{
		super.removeNotify();
		EditBus.removeFromBus(this);
	}

	public void handleMessage(EBMessage msg)
	{
		if(msg instanceof ViewUpdate)
			handleViewUpdate((ViewUpdate)msg);
		else if(msg instanceof BufferUpdate)
			handleBufferUpdate((BufferUpdate)msg);
	}

	public void gotoDirectory(String path)
	{
		if(path.equals(this.path))
			return;

		this.path = path;
		if(MiscUtilities.isURL(path))
		{
			vfs = VFSManager.getVFSForProtocol(MiscUtilities
				.getFileProtocol(path));
		}
		else
			vfs = VFSManager.getFileVFS();

		VFSSession vfsSession = new VFSSession();
		vfsSession.put(VFSSession.PATH_KEY,path);
		if(!vfs.setupVFSSession(vfsSession,this))
			return;

		pathField.setText(path);
		VFSManager.runInWorkThread(new BrowserIORequest(
			BrowserIORequest.LIST_DIRECTORY,this,
			vfsSession,vfs,path));
	}

	// package-private members
	void directoryLoaded(final VFS.DirectoryEntry[] list)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				fileList.setListData(list);
			}
		});
	}

	// private members
	private View view;
	private String path;
	private VFS vfs;
	private HistoryTextField pathField;
	private JList fileList;

	private void handleViewUpdate(ViewUpdate vmsg)
	{
		if(vmsg.getWhat() == ViewUpdate.CLOSED
			&& vmsg.getView() == view)
			view = null;
	}

	private void handleBufferUpdate(BufferUpdate bmsg)
	{
		//
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(evt.getSource() == pathField)
			{
				pathField.addCurrentToHistory();
				String path = pathField.getText();
				if(path != null)
					gotoDirectory(path);
			}
		}
	}
}
