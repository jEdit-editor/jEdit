/*
 * SendDialog.java - Send Dialog
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

package org.gjt.sp.jedit.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.util.Log;

public class SendDialog extends EnhancedDialog implements ActionListener
{
	public static final String CRLF = "\r\n";

	public SendDialog(View view)
	{
		super(view,jEdit.getProperty("send.title"),true);
		this.view = view;

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,0));
		setContentPane(content);

		JPanel panel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		panel.setLayout(layout);

		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridwidth = constraints.gridheight = 1;
		constraints.fill = constraints.BOTH;
		constraints.insets = new Insets(0,0,6,12);
		JLabel label = new JLabel(jEdit.getProperty("send.smtp"),
			SwingConstants.RIGHT);
		layout.setConstraints(label,constraints);
		panel.add(label);

		constraints.gridx = 1;
		constraints.weightx = 1.0f;
		smtp = new JTextField(jEdit.getProperty("send.smtp.value"),30);
		layout.setConstraints(smtp,constraints);
		panel.add(smtp);

		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.weightx = 0.0f;
		label = new JLabel(jEdit.getProperty("send.from"),
			SwingConstants.RIGHT);
		layout.setConstraints(label,constraints);
		panel.add(label);

		constraints.gridx = 1;
		constraints.weightx = 1.0f;
		from = new JTextField(jEdit.getProperty("send.from.value"),30);
		layout.setConstraints(from,constraints);
		panel.add(from);

		constraints.gridx = 0;
		constraints.gridy = 2;
		constraints.weightx = 0.0f;
		label = new JLabel(jEdit.getProperty("send.to"),
			SwingConstants.RIGHT);
		layout.setConstraints(label,constraints);
		panel.add(label);

		constraints.gridx = 1;
		constraints.weightx = 1.0f;
		to = new JTextField(jEdit.getProperty("send.to.value"),30);
		layout.setConstraints(to,constraints);
		panel.add(to);

		constraints.gridx = 0;
		constraints.gridy = 3;
		constraints.weightx = 0.0f;
		label = new JLabel(jEdit.getProperty("send.subject"),
			SwingConstants.RIGHT);
		layout.setConstraints(label,constraints);
		panel.add(label);

		constraints.gridx = 1;
		constraints.weightx = 1.0f;
		subject = new JTextField(view.getBuffer().getPath(),30);
		layout.setConstraints(subject,constraints);
		panel.add(subject);

		constraints.gridx = 0;
		constraints.gridy = 4;
		constraints.gridwidth = 2;
		constraints.fill = constraints.VERTICAL;
		constraints.anchor = constraints.WEST;
		selectionOnly = new JCheckBox(jEdit.getProperty(
			"send.selectionOnly"));
		selectionOnly.setSelected(jEdit.getBooleanProperty(
			"send.selectionOnly.value"));
		layout.setConstraints(selectionOnly,constraints);
		panel.add(selectionOnly);

		content.add(BorderLayout.NORTH,panel);

		buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons,BoxLayout.X_AXIS));
		buttons.setBorder(new EmptyBorder(6,0,0,12));
		buttons.add(Box.createGlue());
		send = new JButton(jEdit.getProperty("send.send"));
		buttons.add(send);
		buttons.add(Box.createHorizontalStrut(6));
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		buttons.add(cancel);
		buttons.add(Box.createGlue());

		content.add(BorderLayout.SOUTH,buttons);

		getRootPane().setDefaultButton(send);
		send.addActionListener(this);
		cancel.addActionListener(this);

		pack();
		setLocationRelativeTo(view);
		show();
	}

	public void actionPerformed(ActionEvent evt)
	{
		Object source = evt.getSource();
		if(source == send)
			ok();
		else if(source == cancel)
			cancel();
	}

	// EnhancedDialog implementation
	public void ok()
	{
		JEditTextArea textArea = view.getTextArea();

		String smtp = this.smtp.getText();
		String from = this.from.getText();
		String to = this.to.getText();
		String subject = this.subject.getText();
		boolean selectionOnly = this.selectionOnly.isSelected();

		if(smtp.length() == 0 || from.length() == 0
			|| to.length() == 0)
		{
			error("sendempty");
			return;
		}

		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		Object[] args = { smtp };
		Log.log(Log.DEBUG,this,jEdit.getProperty("send.connect",args));

		try
		{
			int index = smtp.indexOf(':');
			int port = 25;
			if(index != -1)
			{
				port = Integer.parseInt(smtp.substring(index + 1));
				smtp = smtp.substring(0,index);
			}

			Socket socket = new Socket(smtp,port);
			BufferedReader in = new BufferedReader(new
				InputStreamReader(socket.getInputStream()));
			Writer out = new OutputStreamWriter(socket
				.getOutputStream());

			String response = in.readLine();
			Log.log(Log.DEBUG,this,response);
			if(!response.startsWith("220"))
			{
				error("serverdown");
				return;
			}

			String command = "HELO " + socket.getLocalAddress()
				.getHostName() + CRLF;
			Log.log(Log.DEBUG,this,command);
			out.write(command);
			out.flush();

			response = in.readLine();
			Log.log(Log.DEBUG,this,response);
			if(!response.startsWith("250"))
			{
				error("badhost");
				return;
			}
			command = "MAIL FROM: <" + from + ">" + CRLF;
			Log.log(Log.DEBUG,this,command);
			out.write(command);
			out.flush();

			response = in.readLine();
			Log.log(Log.DEBUG,this,response);
			if(!response.startsWith("250"))
			{
				error("badsender");
				return;
			}
			command = "RCPT TO: <" + to + ">" + CRLF;
			Log.log(Log.DEBUG,this,command);
			out.write(command);
			out.flush();

			response = in.readLine();
			Log.log(Log.DEBUG,this,response);
			if(!response.startsWith("250"))
			{
				error("badrecepient");
				return;
			}
			command = "DATA" + CRLF;
			Log.log(Log.DEBUG,this,command);
			out.write(command);
			out.flush();

			response = in.readLine();
			Log.log(Log.DEBUG,this,response);
			if(!response.startsWith("354"))
			{
				error("badmsg");
				return;
			}

			out.write("Subject: " + subject);
			out.write(CRLF);
			out.write("X-Mailer: jEdit " + jEdit.getVersion());
			out.write(CRLF);
			out.write(CRLF);

			Buffer buffer = view.getBuffer();

			int startLine, endLine;
			if(selectionOnly)
			{
				startLine = textArea.getSelectionStartLine();
				endLine = textArea.getSelectionEndLine();
			}
			else
			{
				startLine = 0;
				endLine = textArea.getLineCount() - 1;
			}

			for(int i = startLine; i <= endLine; i++)
			{
				String line;

				if(selectionOnly)
				{
					int start = textArea.getSelectionStart(i);
					int end = textArea.getSelectionEnd(i);

					line = textArea.getText(start,end - start);
				}
				else
				{
					line = textArea.getLineText(i);
				}

				if(line.equals("."))
					line = "!"; // XXX use =nn code
				out.write(line);
				out.write(CRLF);
			}

			out.write("." + CRLF);
			out.flush();

			response = in.readLine();
			Log.log(Log.DEBUG,this,response);
			if(!response.startsWith("250"))
			{
				error("badmsg");
				return;
			}
			command = "QUIT" + CRLF;
			Log.log(Log.DEBUG,this,command);
			out.write(command);
			out.flush();

			response = in.readLine();
			Log.log(Log.DEBUG,this,response);
			in.close();
			out.close();
			socket.close();

			save();
			dispose();
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,this,io);
			args[0] = io.toString();
			GUIUtilities.error(view,"ioerror",args);
		}
		catch(NumberFormatException nf)
		{
			Log.log(Log.ERROR,this,nf);
			GUIUtilities.error(view,"badport",null);
		}

		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	public void cancel()
	{
		dispose();
	}
	// end EnhancedDialog implementation

	// private members
	private View view;
	private Thread thread;
	private JTextField smtp;
	private JTextField from;
	private JTextField to;
	private JTextField subject;
	private JCheckBox selectionOnly;
	private JPanel buttons;
	private JButton send;
	private JButton cancel;

	private void error(String msg)
	{
		GUIUtilities.error(view,msg,null);
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	private void save()
	{
		jEdit.setProperty("send.smtp.value",smtp.getText());
		jEdit.setProperty("send.from.value",from.getText());
		jEdit.setProperty("send.to.value",to.getText());
		jEdit.setProperty("send.subject.value",subject.getText());
		jEdit.setBooleanProperty("send.selectionOnly.value",
			selectionOnly.isSelected());
	}
}
