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
import javax.swing.border.*;
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

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,6,12,9));
		setContentPane(content);

		JLabel label = new JLabel(jEdit.getProperty(
			"login.caption",new String[] { host }));
		label.setBorder(new EmptyBorder(0,6,3,0));
		content.add(BorderLayout.NORTH,label);

		JPanel panel = createFieldPanel(user,password);

		content.add(BorderLayout.CENTER,panel);

		Box box = new Box(BoxLayout.X_AXIS);
		box.add(Box.createGlue());
		ok = new JButton(jEdit.getProperty("common.ok"));
		ok.addActionListener(this);
		getRootPane().setDefaultButton(ok);
		box.add(ok);
		box.add(Box.createHorizontalStrut(6));
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(this);
		box.add(cancel);
		box.add(Box.createGlue());

		content.add(box,BorderLayout.SOUTH);

		GUIUtilities.requestFocus(this,(user == null ? userField
			: passwordField));

		pack();
		setLocationRelativeTo(view);
		show();
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

	private JPanel createFieldPanel(String user, String password)
	{
		GridBagLayout layout = new GridBagLayout();
		JPanel panel = new JPanel(layout);
		panel.setBorder(new EmptyBorder(3,0,9,0));

		GridBagConstraints cons = new GridBagConstraints();
		cons.insets = new Insets(3,6,3,3);
		cons.gridwidth = cons.gridheight = 1;
		cons.gridx = cons.gridy = 0;
		cons.fill = GridBagConstraints.BOTH;
		JLabel label = new JLabel(jEdit.getProperty("login.user"),
			SwingConstants.RIGHT);
		layout.setConstraints(label,cons);
		panel.add(label);

		userField = new JTextField(user);
		cons.gridx = 1;
		cons.weightx = 1.0f;
		layout.setConstraints(userField,cons);
		panel.add(userField);

		label = new JLabel(jEdit.getProperty("login.password"),
			SwingConstants.RIGHT);
		cons.gridx = 0;
		cons.weightx = 0.0f;
		cons.gridy = 1;
		layout.setConstraints(label,cons);
		panel.add(label);

		passwordField = new JPasswordField(password);
		cons.gridx = 1;
		cons.weightx = 1.0f;
		layout.setConstraints(passwordField,cons);
		panel.add(passwordField);

		return panel;
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.4  2000/06/03 07:28:26  sp
 * User interface updates, bug fixes
 *
 */
