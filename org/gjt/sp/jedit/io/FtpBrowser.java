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

import com.fooware.net.*;
import javax.swing.event.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.*;
import java.util.Vector;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class FtpBrowser extends JDialog
{
	public static final int OPEN = 0;
	public static final int SAVE = 1;

	public FtpBrowser(View view, Buffer buffer, int type)
	{
		super(view,jEdit.getProperty("vfs.ftp.browser."
			+ (type == OPEN ? "open" : "save") + "-title"),true);

		this.view = view;
		this.type = type;

		// Get default values for fields from buffer's current
		// settings
		String host, user, password, path = null;

		if(buffer.getVFS() instanceof FtpVFS)
		{
			FtpAddress address = new FtpAddress(buffer.getPath());
			host = address.host + ":" + address.port;
			user = address.user;
			if(user == null)
				user = (String)buffer.getProperty(FtpVFS.USERNAME_PROPERTY);
			password = address.password;
			if(password == null)
				password = (String)buffer.getProperty(FtpVFS.PASSWORD_PROPERTY);
			path = MiscUtilities.getFileParent(address.path);
		}
		else
		{
			host = null;
			user = null;
			password = null;
			path = null;
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
		topFields.add(hostField = new JTextField(host));
		topFields.add(userField = new JTextField(user));
		topFields.add(passwordField = new JPasswordField(password));
		topFields.add(pathField = new JTextField(path));
		topPanel.add(BorderLayout.CENTER,topFields);

		getContentPane().add(BorderLayout.NORTH,topPanel);

		JPanel buttons = new JPanel();
		ActionHandler actionHandler = new ActionHandler();

		// the below locks the preferred size so that the buttons
		// don't jump around
		select = new JButton(jEdit.getProperty("vfs.ftp.browser.connect"));
		select.setPreferredSize(select.getPreferredSize());
		select.addActionListener(actionHandler);
		buttons.add(select);

		home = new JButton(jEdit.getProperty("vfs.ftp.browser.home"));
		home.addActionListener(actionHandler);
		buttons.add(home);
		refresh = new JButton(jEdit.getProperty("vfs.ftp.browser.refresh"));
		refresh.addActionListener(actionHandler);
		buttons.add(refresh);
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(actionHandler);
		buttons.add(cancel);

		list = new JList();
		list.setVisibleRowCount(16);
		list.addListSelectionListener(new ListHandler());
		getContentPane().add(BorderLayout.CENTER,new JScrollPane(list));

		getContentPane().add(BorderLayout.SOUTH,buttons);

		update();

		pack();
		setLocationRelativeTo(view);
		show();
	}

	public boolean isOK()
	{
		return isOK;
	}

	public String getPath()
	{
		return path;
	}

	public String getPassword()
	{
		return new String(passwordField.getPassword());
	}

	// private members
	private View view;

	private int type;
	private JTextField hostField, userField, pathField;
	private JPasswordField passwordField;
	private JList list;
	private JButton select, home, refresh, cancel;

	private boolean connected;
	private String path;
	private boolean isOK;

	private FtpClient client;

	private void update()
	{
		hostField.setEnabled(!connected);
		userField.setEnabled(!connected);
		passwordField.setEnabled(!connected);
		pathField.setEnabled(connected);

		if(connected)
		{
			select.setText(jEdit.getProperty("vfs.ftp.browser."
				+ (type == OPEN ? "open" : "save")));
			select.setEnabled(pathField.getText() != null);
		}

		home.setEnabled(connected);
		refresh.setEnabled(connected);
	}

	private void connect()
	{
		String host = hostField.getText();
		String user = userField.getText();

		if(host == null || user == null)
		{
			getToolkit().beep();
			return;
		}
		String port;
		int index = host.indexOf(':');
		if(index != -1)
		{
			port = host.substring(index + 1);
			host = host.substring(0,index);
		}
		else
			port = "21";

		String password = new String(passwordField.getPassword());

		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		client = FtpVFS.createFtpClient(view,host,port,user,password,false);
		if(client != null)
		{
			connected = true;
			refresh();
		}
		else
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

		update();
	}

	private void disconnect()
	{
		if(client == null)
			return;

		try
		{
			client.logout();
		}
		catch(IOException io)
		{
			// can't do much about it...
			Log.log(Log.ERROR,this,io);
		}

		connected = false;
	}

	private void refresh()
	{
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		list.setListData(new Object[0]);

		BufferedReader in = null;
		try
		{
			Reader _in = client.list();
			if(_in == null)
			{
				String[] args = { client.getResponse().toString() };
				GUIUtilities.error(view,"vfs.ftp.list-error",args);
				return;
			}

			in = new BufferedReader(_in);
			Vector fileList = new Vector();
			String line;
			while((line = in.readLine()) != null)
			{
				boolean directory = (line.charAt(0) == 'd');
				// XXX: what about filenames with spaces?
				int index = line.lastIndexOf(' ');
				line = line.substring(index + 1);
				if(directory)
					line = line.concat("/");
				fileList.addElement(line);
			}
			MiscUtilities.quicksort(fileList,new MiscUtilities
				.StringICaseCompare());
			list.setListData(fileList);
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,this,io);
			String[] args = { io.toString() };
			GUIUtilities.error(view,"ioerror",args);
		}
		finally
		{
			if(in != null)
			{
				try
				{
					in.close();
				}
				catch(Exception e)
				{
					Log.log(Log.ERROR,this,e);
				}
			}
		}

		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	private void select()
	{
		String path = pathField.getText();
		if(path.endsWith("/"))
		{
			// cd ...
			return;
		}

		StringBuffer buf = new StringBuffer("ftp://");
		String user = userField.getText();
		if(user != null)
		{
			buf.append(user);
			buf.append('@');
		}
		String host = hostField.getText();
		if(host == null || path == null)
		{
			getToolkit().beep();
			return;
		}
		buf.append(host);
		buf.append(path);

		path = buf.toString();
		isOK = true;

		disconnect();
		dispose();
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(source == select)
			{
				if(!connected)
				{
					connect();
					return;
				}

				select();
			}
			else if(source == refresh)
			{
				refresh();
			}
			else if(source == cancel)
			{
				disconnect();
				dispose();
			}
		}
	}

	class ListHandler implements ListSelectionListener
	{
		public void valueChanged(ListSelectionEvent evt)
		{
			if(evt.getValueIsAdjusting())
				return;

			Object selected = list.getSelectedValue();
			if(selected != null)
				pathField.setText(selected.toString());
		}
	}
}

/*
 * Change Log:
 * $Log$
 * Revision 1.3  2000/04/29 09:17:07  sp
 * VFS updates, various fixes
 *
 * Revision 1.2  2000/04/28 09:29:12  sp
 * Key binding handling improved, VFS updates, some other stuff
 *
 * Revision 1.1  2000/04/25 11:00:20  sp
 * FTP VFS hacking, some other stuff
 *
 */
