/*
 * FtpBrowser.java - Ftp open/save dialog box
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

package org.gjt.sp.jedit.io;

import javax.swing.event.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.*;

public class FtpBrowser extends JDialog
{
	public static final int OPEN = 0;
	public static final int SAVE = 1;

	public FtpBrowser(View view, Buffer buffer, int type)
	{
		super(view,jEdit.getProperty("vfs.ftp.browser."
			+ (type == OPEN ? "open" : "save") + "-title"),true);

		this.type = type;

		// Get default values for fields from buffer's current
		// settings
		String _host, _username, _password, _path = null;

		if(buffer.getVFS() instanceof FtpVFS)
		{
			FtpAddress address = new FtpAddress(buffer.getPath());
			_host = address.host + ":" + address.port;
			_username = address.user;
			if(_username == null)
				_username = (String)buffer.getProperty(FtpVFS.USERNAME_PROPERTY);
			_password = address.password;
			if(_password == null)
				_password = (String)buffer.getProperty(FtpVFS.PASSWORD_PROPERTY);
			_path = MiscUtilities.getFileParent(address.path);
		}
		else
		{
			_host = null;
			_username = null;
			_password = null;
			_path = null;
		}

		JPanel topPanel = new JPanel(new BorderLayout());

		JPanel topLabels = new JPanel(new GridLayout(4,1));
		topLabels.add(new JLabel(jEdit.getProperty("vfs.ftp.browser.host"),
			SwingConstants.RIGHT));
		topLabels.add(new JLabel(jEdit.getProperty("vfs.ftp.browser.username"),
			SwingConstants.RIGHT));
		topLabels.add(new JLabel(jEdit.getProperty("vfs.ftp.browser.password"),
			SwingConstants.RIGHT));
		topLabels.add(new JLabel(jEdit.getProperty("vfs.ftp.browser.path"),
			SwingConstants.RIGHT));
		topPanel.add(BorderLayout.WEST,topLabels);

		JPanel topFields = new JPanel(new GridLayout(4,1));
		topFields.add(host = new JTextField(_host));
		topFields.add(username = new JTextField(_username));
		topFields.add(password = new JTextField(_password));
		topFields.add(path = new JTextField(_path));
		topPanel.add(BorderLayout.CENTER,topFields);

		getContentPane().add(BorderLayout.NORTH,topPanel);

		JPanel buttons = new JPanel();
		select = new JButton(jEdit.getProperty("vfs.ftp.browser."
			+ (type == OPEN ? "open" : "save")));
		buttons.add(select);

		// the below locks the preferred size so that the buttons
		// don't jump around when toggling between connected and
		// disconnected
		connect = new JButton(jEdit.getProperty("vfs.ftp.browser.disconnect"));
		connect.setPreferredSize(connect.getPreferredSize());
		buttons.add(connect);

		home = new JButton(jEdit.getProperty("vfs.ftp.browser.home"));
		buttons.add(home);
		refresh = new JButton(jEdit.getProperty("vfs.ftp.browser.refresh"));
		buttons.add(refresh);
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		buttons.add(cancel);

		listModel = new DefaultListModel();
		list = new JList(listModel);
		list.addListSelectionListener(new ListHandler());
		getContentPane().add(BorderLayout.CENTER,new JScrollPane(list));

		getContentPane().add(BorderLayout.SOUTH,buttons);
		updateButtons();

		pack();
		setLocationRelativeTo(view);
		show();
	}

	// private members
	private int type;
	private JTextField host, username, password, path;
	private DefaultListModel listModel;
	private JList list;
	private JButton select, connect, home, refresh, cancel;

	private boolean connected;

	private void updateButtons()
	{
		host.setEnabled(!connected);
		username.setEnabled(!connected);
		password.setEnabled(!connected);

		connect.setText(jEdit.getProperty("vfs.ftp.browser."
			+ (connected ? "connect" : "disconnect")));
		home.setEnabled(connected);
		refresh.setEnabled(connected);

		int index = list.getSelectedIndex();
		select.setEnabled(connected && index != -1);
	}

	class ListHandler implements ListSelectionListener
	{
		public void valueChanged(ListSelectionEvent evt)
		{
			if(evt.getValueIsAdjusting())
				return;
			updateButtons();
		}
	}
}

/*
 * Change Log:
 * $Log$
 * Revision 1.1  2000/04/25 11:00:20  sp
 * FTP VFS hacking, some other stuff
 *
 */
