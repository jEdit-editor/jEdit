/*
 * MacroInputDialog.java - Macro input login dialog
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
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import org.gjt.sp.jedit.*;

public class MacroInputDialog extends EnhancedDialog implements ActionListener
{
	public MacroInputDialog(View view)
	{
		super(view,jEdit.getProperty("macro-input.title"),true);
		this.view = view;

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,0));
		setContentPane(content);

		JLabel label = new JLabel(jEdit.getProperty(
			"macro-input.caption"));
		label.setBorder(new EmptyBorder(0,0,6,12));
		content.add(BorderLayout.NORTH,label);

		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints cons = new GridBagConstraints();
		cons.insets = new Insets(0,0,6,12);
		cons.gridwidth = cons.gridheight = 1;
		cons.gridx = cons.gridy = 0;
		cons.fill = GridBagConstraints.BOTH;

		JPanel panel = new JPanel(layout);
		label = new JLabel(jEdit.getProperty("macro-input.prompt"),
			SwingConstants.RIGHT);
		layout.setConstraints(label,cons);
		panel.add(label);

		cons.gridx = 1;
		cons.weightx = 1.0f;
		promptField = new JTextField();
		layout.setConstraints(promptField,cons);
		panel.add(promptField);

		cons.gridx = 0;
		cons.gridy = 1;
		cons.weightx = 0.0f;
		label = new JLabel(jEdit.getProperty("macro-input.register"),
			SwingConstants.RIGHT);
		layout.setConstraints(label,cons);
		panel.add(label);

		cons.gridx = 1;
		cons.fill = GridBagConstraints.NONE;
		cons.anchor = GridBagConstraints.WEST;
		registerField = new JTextField(2);
		layout.setConstraints(registerField,cons);
		panel.add(registerField);

		content.add(BorderLayout.CENTER,panel);

		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
		panel.setBorder(new EmptyBorder(6,0,0,12));
		panel.add(Box.createGlue());
		ok = new JButton(jEdit.getProperty("common.ok"));
		ok.addActionListener(this);
		getRootPane().setDefaultButton(ok);
		panel.add(ok);
		panel.add(Box.createHorizontalStrut(6));
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(this);
		panel.add(cancel);
		panel.add(Box.createGlue());

		content.add(panel, BorderLayout.SOUTH);

		GUIUtilities.requestFocus(this,promptField);

		pack();
		setLocationRelativeTo(view);
		show();
	}

	// EnhancedDialog implementation
	public void ok()
	{
		prompt = promptField.getText();
		register = registerField.getText();
		if(prompt == null || register == null || register.length() != 1)
		{
			view.getToolkit().beep();
			return;
		}

		isOK = true;
		dispose();
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

	public String getPrompt()
	{
		return prompt;
	}

	public String getRegister()
	{
		return register;
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
	private JTextField promptField;
	private JTextField registerField;
	private String prompt;
	private String register;
	private boolean isOK;
	private JButton ok;
	private JButton cancel;
}
