/*
 * LoginDialog.java - FTP login dialog
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

package org.gjt.sp.jedit.gui;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import org.gjt.sp.jedit.*;

public class LoginDialog extends EnhancedDialog implements ActionListener
{
	public LoginDialog(View view, String host, String user, String password)
	{
		super(view,jEdit.getProperty("login.title"),true);
		this.view = view;

		Container content = getContentPane();

		content.add(BorderLayout.NORTH,new JLabel(jEdit.getProperty(
			"login.caption",new String[] { host })));

		JPanel panel = new JPanel(new GridLayout(2,2));
		panel.add(new JLabel(jEdit.getProperty("login.user")));
		userField = new JTextField(user);
		panel.add(userField);

		panel.add(new JLabel(jEdit.getProperty("login.password")));
		passwordField = new JPasswordField(password);
		panel.add(passwordField);

		content.add(BorderLayout.CENTER,panel);

		panel = new JPanel();
		ok = new JButton(jEdit.getProperty("common.ok"));
		ok.addActionListener(this);
		getRootPane().setDefaultButton(ok);
		panel.add(ok);
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(this);
		panel.add(cancel);

		content.add(panel, BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(view);
		show();

		if(password == null)
			userField.requestFocus();
		else
			passwordField.requestFocus();
	}

	// EnhancedDialog implementation
	public void ok()
	{
		if(userField.hasFocus() && passwordField.getPassword().length == 0)
			passwordField.requestFocus();
		else
		{
			user = userField.getText();
			if(user == null || user.length() == 0)
			{
				getToolkit().beep();
				return;
			}
			password = new String(passwordField.getPassword());
			if(password == null)
				password = "";
			isOK = true;
			dispose();
		}
	}

	public void cancel()
	{
		dispose();
	}
	// end EnhancedDialog implementation

	public boolean isOK()
	{
		return isOK;
	}

	public String getUser()
	{
		return user;
	}

	public String getPassword()
	{
		return password;
	}

	public void actionPerformed(ActionEvent evt)
	{
		Object source = evt.getSource();
		if(source == ok)
			ok();
		else if(source == cancel)
			cancel();
	}

	// private members
	private View view;
	private JTextField userField;
	private JPasswordField passwordField;
	private String user;
	private String password;
	private boolean isOK;
	private JButton ok;
	private JButton cancel;
}
