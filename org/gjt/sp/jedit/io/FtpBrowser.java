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

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,0));
		setContentPane(content);

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
				user = (String)buffer.getVFSSession().get(FtpVFS.USERNAME_KEY);
			password = address.password;
			if(password == null)
				password = (String)buffer.getVFSSession().get(FtpVFS.PASSWORD_KEY);
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

		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints cons = new GridBagConstraints();
		cons.gridwidth = cons.gridheight = 1;
		cons.gridx = cons.gridy = 0;
		cons.fill = GridBagConstraints.BOTH;
		cons.insets = new Insets(0,0,6,12);

		JPanel centerPanel = new JPanel(layout);

		JLabel label = new JLabel(jEdit.getProperty("vfs.ftp.browser.host"),
			SwingConstants.RIGHT);
		layout.setConstraints(label,cons);
		centerPanel.add(label);

		cons.gridy++;
		label = new JLabel(jEdit.getProperty("vfs.ftp.browser.username"),
			SwingConstants.RIGHT);
		layout.setConstraints(label,cons);
		centerPanel.add(label);

		cons.gridy++;
		label = new JLabel(jEdit.getProperty("vfs.ftp.browser.password"),
			SwingConstants.RIGHT);
		layout.setConstraints(label,cons);
		centerPanel.add(label);

		cons.gridy = 0;
		cons.gridx = 1;
		cons.weightx = 1.0f;

		hostField = new JTextField(host);
		layout.setConstraints(hostField,cons);
		centerPanel.add(hostField);
		cons.gridy++;
		userField = new JTextField(user);
		layout.setConstraints(userField,cons);
		centerPanel.add(userField);
		cons.gridy++;
		passwordField = new JPasswordField(password);
		layout.setConstraints(passwordField,cons);
		centerPanel.add(passwordField);

		cons.gridy++;
		cons.gridx = 0;
		cons.gridwidth = 2;
		cons.weighty = 1.0f;
		list = new JList();
		list.setVisibleRowCount(10);
		list.addListSelectionListener(new ListHandler());
		list.addMouseListener(new MouseHandler());
		list.setCellRenderer(new FileCellRenderer());
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane listScroller = new JScrollPane(list);
		layout.setConstraints(listScroller,cons);
		centerPanel.add(listScroller);

		cons.gridy++;
		cons.gridwidth = 1;
		cons.weightx = cons.weighty = 0.0f;
		label = new JLabel(jEdit.getProperty("vfs.ftp.browser.path"),
			SwingConstants.RIGHT);
		layout.setConstraints(label,cons);
		centerPanel.add(label);

		cons.gridx = 1;
		cons.weightx = 1.0f;
		pathField = new JTextField(path);
		layout.setConstraints(pathField,cons);
		centerPanel.add(pathField);

		content.add(BorderLayout.CENTER,centerPanel);

		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons,BoxLayout.X_AXIS));
		buttons.setBorder(new EmptyBorder(6,0,0,12));
		buttons.add(Box.createGlue());

		ActionHandler actionHandler = new ActionHandler();

		// the below locks the preferred size so that the buttons
		// don't jump around
		select = new JButton(jEdit.getProperty("vfs.ftp.browser.connect"));
		select.setPreferredSize(select.getPreferredSize());
		select.addActionListener(actionHandler);
		buttons.add(select);
		buttons.add(Box.createHorizontalStrut(6));

		up = new JButton(jEdit.getProperty("vfs.ftp.browser.up"));
		up.addActionListener(actionHandler);
		buttons.add(up);
		buttons.add(Box.createHorizontalStrut(6));
		refresh = new JButton(jEdit.getProperty("vfs.ftp.browser.refresh"));
		refresh.addActionListener(actionHandler);
		buttons.add(refresh);
		buttons.add(Box.createHorizontalStrut(6));
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(actionHandler);
		buttons.add(cancel);
		buttons.add(Box.createGlue());

		content.add(BorderLayout.SOUTH,buttons);

		addWindowListener(new WindowHandler());
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		update();

		ftpThread = new FtpThread();
		ftpThread.start();
		ftpThreadNotify = new Object();

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
	private int type;
	private JTextField hostField, userField, pathField;
	private JPasswordField passwordField;
	private JList list;
	private JButton select, up, refresh, cancel;

	private boolean connected;
	private String path;
	private boolean isOK;

	private FtpThread ftpThread;
	private Object ftpThreadNotify;
	private int ftpThreadRequest;
	private FtpClient client;

	// FTP thread requests
	private static final int FTP_NONE = 0;
	private static final int FTP_CONNECT = 1;
	private static final int FTP_DISCONNECT = 2;
	private static final int FTP_REFRESH = 3;
	private static final int FTP_UP = 4;
	private static final int FTP_SELECT = 5;

	private void doFtpRequest(int ftpThreadRequest)
	{
		synchronized(ftpThreadNotify)
		{
			this.ftpThreadRequest = ftpThreadRequest;
			ftpThreadNotify.notify();
		}
	}

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

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();

			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

			if(source == select)
			{
				if(!connected)
					doFtpRequest(FTP_CONNECT);
				else
					doFtpRequest(FTP_SELECT);
			}
			else if(source == refresh)
			{
				doFtpRequest(FTP_REFRESH);
			}
			else if(source == up)
			{
				doFtpRequest(FTP_UP);
			}
			else if(source == cancel)
			{
				doFtpRequest(FTP_DISCONNECT);
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
				doFtpRequest(FTP_SELECT);
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

			doFtpRequest(FTP_DISCONNECT);
		}
	}

	class FtpThread extends Thread
	{
		public void run()
		{
			for(;;)
			{
				synchronized(ftpThreadNotify)
				{
					try
					{
						ftpThreadNotify.wait();
					}
					catch(InterruptedException i)
					{
					}

					setCursor(Cursor.getPredefinedCursor(
						Cursor.WAIT_CURSOR));

					switch(ftpThreadRequest)
					{
					case FTP_CONNECT:
						connect();
						break;
					case FTP_DISCONNECT:
						disconnect();
						break;
					case FTP_REFRESH:
						refresh();
						break;
					case FTP_UP:
						up();
						break;
					case FTP_SELECT:
						selectPath();
						break;
					default:
						throw new InternalError("Invalid FTP request: "
							+ ftpThreadRequest);
					}

					setCursor(Cursor.getPredefinedCursor(
						Cursor.DEFAULT_CURSOR));

				}
			}
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

			client = FtpVFS.createFtpClient(FtpBrowser.this,host,
				port,user,password,false);

			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
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
				}
			});

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

			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					dispose();
				}
			});

			stop();
		}

		private void changeDirectory(String directory)
		{
			try
			{
				client.changeWorkingDirectory(directory);
				if(!client.getResponse().isPositiveCompletion())
				{
					String[] args = { directory, client.getResponse().toString() };
					VFSManager.error(FtpBrowser.this,"vfs.ftp.cd-error",args);
				}
				else
					refresh();
			}
			catch(IOException io)
			{
				String[] args = { io.getMessage() };
				VFSManager.error(FtpBrowser.this,"ioerror",args);
			}
		}

		private void refresh()
		{
			BufferedReader in = null;
			final Vector fileList = new Vector();
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
					VFSManager.error(FtpBrowser.this,"vfs.ftp.list-error",args);
					return;
				}

				in = new BufferedReader(_in);
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
			}
			catch(IOException io)
			{
				Log.log(Log.ERROR,this,io);
				String[] args = { io.toString() };
				VFSManager.error(FtpBrowser.this,"ioerror",args);
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

			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					list.setListData(fileList);
				}
			});
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
			boolean symlink = (line.charAt(0) == 'l');
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
			if(symlink)
			{
				int index = line.lastIndexOf(" -> ");
				if(index != 0)
					line = line.substring(0,index);
			}
			else if(directory)
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
					VFSManager.error(FtpBrowser.this,"vfs.ftp.cd-error",args);
				}
				else
					refresh();
			}
			catch(IOException io)
			{
				String[] args = { io.getMessage() };
				VFSManager.error(FtpBrowser.this,"ioerror",args);
			}
		}

		private void selectPath()
		{
			String path = pathField.getText();

			if(path == null)
			{
				getToolkit().beep();
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

			FtpBrowser.this.path = buf.toString();
			isOK = true;

			disconnect();
		}
	}
}

/*
 * Change Log:
 * $Log$
 * Revision 1.12  2000/07/29 12:24:08  sp
 * More VFS work, VFS browser started
 *
 * Revision 1.11  2000/07/26 07:48:44  sp
 * stuff
 *
 * Revision 1.10  2000/07/22 12:37:39  sp
 * WorkThreadPool bug fix, IORequest.load() bug fix, version wound back to 2.6
 *
 * Revision 1.9  2000/06/04 08:57:35  sp
 * GUI updates, bug fixes
 *
 * Revision 1.8  2000/06/02 08:43:03  sp
 * Printing fixes and enhancements, other bug fixes
 *
 * Revision 1.7  2000/05/12 11:07:39  sp
 * Bug fixes, documentation updates
 *
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
