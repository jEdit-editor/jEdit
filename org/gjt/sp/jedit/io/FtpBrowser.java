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
import javax.swing.border.*;
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
			path = address.path;
			if(type == OPEN)
				path = MiscUtilities.getFileParent(path);
		}
		else
		{
			host = jEdit.getProperty("vfs.ftp.browser.host.value");
			user = jEdit.getProperty("vfs.ftp.browser.user.value");
			password = null;
			path = null;
		}

		JPanel topPanel = new JPanel(new BorderLayout());

		JPanel topLabels = new JPanel(new GridLayout(3,1));
		topLabels.add(new JLabel(jEdit.getProperty("vfs.ftp.browser.host"),
			SwingConstants.RIGHT));
		topLabels.add(new JLabel(jEdit.getProperty("vfs.ftp.browser.username"),
			SwingConstants.RIGHT));
		topLabels.add(new JLabel(jEdit.getProperty("vfs.ftp.browser.password"),
			SwingConstants.RIGHT));
		topPanel.add(BorderLayout.WEST,topLabels);

		JPanel topFields = new JPanel(new GridLayout(3,1));
		topFields.add(hostField = new JTextField(host));
		topFields.add(userField = new JTextField(user));
		topFields.add(passwordField = new JPasswordField(password));
		topFields.setBorder(new EmptyBorder(0,0,5,0));
		topPanel.add(BorderLayout.CENTER,topFields);

		getContentPane().add(BorderLayout.NORTH,topPanel);

		JPanel centerPanel = new JPanel(new BorderLayout());
		list = new JList();
		list.setVisibleRowCount(10);
		list.addListSelectionListener(new ListHandler());
		list.addMouseListener(new MouseHandler());
		list.setCellRenderer(new FileCellRenderer());
		centerPanel.add(BorderLayout.CENTER,new JScrollPane(list));

		JPanel pathPanel = new JPanel(new BorderLayout());
		pathPanel.add(BorderLayout.WEST,new JLabel(jEdit.getProperty(
			"vfs.ftp.browser.path"),SwingConstants.RIGHT));
		pathPanel.add(BorderLayout.CENTER,pathField = new JTextField(path));
		pathPanel.setBorder(new EmptyBorder(5,0,0,0));
		centerPanel.add(BorderLayout.SOUTH,pathPanel);

		getContentPane().add(BorderLayout.CENTER,centerPanel);

		JPanel buttons = new JPanel();
		ActionHandler actionHandler = new ActionHandler();

		// the below locks the preferred size so that the buttons
		// don't jump around
		select = new JButton(jEdit.getProperty("vfs.ftp.browser.connect"));
		select.setPreferredSize(select.getPreferredSize());
		select.addActionListener(actionHandler);
		buttons.add(select);

		up = new JButton(jEdit.getProperty("vfs.ftp.browser.up"));
		up.addActionListener(actionHandler);
		buttons.add(up);
		refresh = new JButton(jEdit.getProperty("vfs.ftp.browser.refresh"));
		refresh.addActionListener(actionHandler);
		buttons.add(refresh);
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(actionHandler);
		buttons.add(cancel);

		getContentPane().add(BorderLayout.SOUTH,buttons);

		addWindowListener(new WindowHandler());
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

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
	private JButton select, up, refresh, cancel;

	private boolean connected;
	private String path;
	private boolean isOK;

	private FtpClient client;

	private void update()
	{
		hostField.setEnabled(!connected);
		userField.setEnabled(!connected);
		passwordField.setEnabled(!connected);

		if(connected)
		{
			select.setText(jEdit.getProperty("vfs.ftp.browser."
				+ (type == OPEN ? "open" : "save")));
		}

		up.setEnabled(connected);
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

		client = FtpVFS.createFtpClient(view,host,port,user,password,false);
		if(client != null)
		{
			connected = true;
			String path = pathField.getText();
			if(path != null && path.length() != 0)
			{
				if(path.endsWith("/"))
					changeDirectory(path);
				else
				{
					changeDirectory(MiscUtilities
						.getFileParent(path));
					list.setSelectedValue(MiscUtilities
						.getFileName(path),true);
				}
			}
			else
				refresh();
		}

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

	private void changeDirectory(String directory)
	{
		try
		{
			client.changeWorkingDirectory(directory);
			if(!client.getResponse().isPositiveCompletion())
			{
				String[] args = { directory, client.getResponse().toString() };
				GUIUtilities.error(view,"vfs.ftp.cd-error",args);
			}
			else
				refresh();
		}
		catch(IOException io)
		{
			String[] args = { io.getMessage() };
			GUIUtilities.error(view,"ioerror",args);
		}
	}
	private void refresh()
	{
		list.setListData(new Object[0]);

		BufferedReader in = null;
		try
		{
			// get current directory
			client.printWorkingDirectory();
			path = extractPath(client.getResponse().getMessage());
			if(!path.endsWith("/"))
				path = path + '/';
			pathField.setText(path);

			client.dataPort();
			Reader _in = client.list();
			if(_in == null)
			{
				String[] args = { path, client.getResponse().toString() };
				GUIUtilities.error(view,"vfs.ftp.list-error",args);
				return;
			}

			in = new BufferedReader(_in);
			Vector fileList = new Vector();
			String line;
			while((line = in.readLine()) != null)
			{
				if(line.startsWith("total"))
					continue;

				String shortName = getShortName(line);
				if(shortName.equals("./"))
					continue;

				fileList.addElement(shortName);
			}
			MiscUtilities.quicksort(fileList,new FileCompare());
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
	}

	class FileCompare implements MiscUtilities.Compare
	{
		public int compare(Object obj1, Object obj2)
		{
			String str1 = (String)obj1;
			String str2 = (String)obj2;
			if(str1.endsWith("/"))
			{
				if(str2.endsWith("/"))
					return str1.compareTo(str2);
				else
					return -1;
			}
			else
			{
				if(str2.endsWith("/"))
					return 1;
				else
					return str1.compareTo(str2);
			}
		}
	}

	// PWD returns a string of the form '250 "foo" is current directory'
	private String extractPath(String line)
	{
		int start = line.indexOf('"') + 1;
		int end = line.lastIndexOf('"');
		return line.substring(start,end);
	}

	// LIST returns a file listing where the last field is the
	// file name
	private String getShortName(String line)
	{
		boolean directory = (line.charAt(0) == 'd');
		int fieldCount = 0;
		boolean lastWasSpace = false;
		int i;
		for(i = 0; i < line.length(); i++)
		{
			if(line.charAt(i) == ' ')
			{
				if(lastWasSpace)
					continue;
				else
				{
					lastWasSpace = true;
					fieldCount++;
				}
			}
			else
			{
				lastWasSpace = false;
				if(fieldCount == 8)
					break;
			}
		}

		line = line.substring(i);
		if(directory)
			line = line + '/';
		return line;
	}

	private void up()
	{
		try
		{
			client.changeToParentDirectory();
			if(!client.getResponse().isPositiveCompletion())
			{
				String[] args = { path, client.getResponse().toString() };
				GUIUtilities.error(view,"vfs.ftp.cd-error",args);
			}
			else
				refresh();
		}
		catch(IOException io)
		{
			String[] args = { io.getMessage() };
			GUIUtilities.error(view,"ioerror",args);
		}
	}

	private void selectPath()
	{
		String path = pathField.getText();

		if(path == null)
		{
			view.getToolkit().beep();
			return;
		}

		if(path.endsWith("/"))
		{
			changeDirectory(path);
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

		this.path = buf.toString();
		isOK = true;

		disconnect();
		dispose();
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();

			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

			if(source == select)
			{
				if(!connected)
				{
					connect();
					return;
				}

				selectPath();
			}
			else if(source == refresh)
			{
				refresh();
			}
			else if(source == up)
			{
				up();
			}
			else if(source == cancel)
			{
				disconnect();
				dispose();
			}

			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}

	class FileCellRenderer extends DefaultListCellRenderer
	{
		Icon directoryIcon = UIManager.getIcon("FileView.directoryIcon");
		Icon fileIcon = UIManager.getIcon("FileView.fileIcon");

		public Component getListCellRendererComponent(JList list,
			Object value, int index, boolean isSelected,
			boolean cellHasFocus)
		{
			super.getListCellRendererComponent(list,value,index,
				isSelected,cellHasFocus);

			String str = (String)value;
			Icon icon;
			if(str.endsWith("/"))
			{
				icon = directoryIcon;
				setText(str.substring(0,str.length() - 1));
			}
			else
				icon = fileIcon;
			setIcon(icon);

			return this;
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
				pathField.setText(path + selected);
		}
	}

	class MouseHandler extends MouseAdapter
	{
		public void mousePressed(MouseEvent evt)
		{
			if(evt.getClickCount() == 2)
			{
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				selectPath();
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		}
	}

	class WindowHandler extends WindowAdapter
	{
		public void windowClosed(WindowEvent evt)
		{
			String host = hostField.getText();
			if(host != null)
				jEdit.setProperty("vfs.ftp.browser.host.value",host);
			String user = userField.getText();
			if(user != null)
				jEdit.setProperty("vfs.ftp.browser.user.value",user);
		}
	}
}

/*
 * Change Log:
 * $Log$
 * Revision 1.6  2000/05/09 10:51:52  sp
 * New status bar, a few other things
 *
 * Revision 1.5  2000/05/01 11:53:24  sp
 * More icons added to toolbar, minor updates here and there
 *
 * Revision 1.4  2000/04/30 07:27:13  sp
 * Ftp VFS hacking, bug fixes
 *
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
