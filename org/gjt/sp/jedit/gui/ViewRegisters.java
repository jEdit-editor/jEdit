/*
 * ViewRegisters.java - View registers dialog
 * Copyright (C) 1999, 2000 Slava Pestov
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
import java.util.Vector;
import org.gjt.sp.jedit.*;

public class ViewRegisters extends EnhancedDialog
{
	public ViewRegisters(View view)
	{
		super(view,jEdit.getProperty("view-registers.title"),true);

		Container content = getContentPane();

		contents = new Vector();
		registerCombo = new JComboBox();
		Registers.Register[] registers = Registers.getRegisters();

		int index = 0;
		for(int i = 0; i < registers.length; i++)
		{
			Registers.Register reg = registers[i];
			if(reg == null)
				continue;

			String value = reg.toString();
			if(value == null)
				continue;

			String name;
			if(i == '\n')
				name = "\n";
			else if(i == '\t')
				name = "\t";
			else if(i == '$')
			{
				index = registerCombo.getItemCount();
				name = jEdit.getProperty("view-registers.clipboard");
			}
			else
				name = String.valueOf((char)i);

			registerCombo.addItem(name);
			contents.addElement(reg);
		}

		if(registerCombo.getItemCount() == 0)
		{
			registerCombo.addItem(jEdit.getProperty("view-registers.none"));
			contents.addElement(null);
		}

		registerCombo.setMaximumSize(registerCombo.getPreferredSize());

		Box box = new Box(BoxLayout.X_AXIS);
		box.add(new JLabel(jEdit.getProperty("view-registers.caption")));
		box.add(registerCombo);
		box.add(Box.createGlue());
		box.add(new JLabel(jEdit.getProperty("view-registers.type")));
		type = new JLabel();
		box.add(type);
		box.add(Box.createHorizontalStrut(5));
		content.add(BorderLayout.NORTH,box);

		contentTextArea = new JTextArea(10,80);
		contentTextArea.setFont(view.getTextArea().getPainter().getFont());
		contentTextArea.setEditable(false);
		content.add(BorderLayout.CENTER,new JScrollPane(contentTextArea));

		JPanel panel = new JPanel();
		close = new JButton(jEdit.getProperty("common.close"));
		close.addActionListener(new ActionHandler());
		panel.add(close);
		getRootPane().setDefaultButton(close);
		content.add(BorderLayout.SOUTH,panel);

		registerCombo.addActionListener(new ActionHandler());
		registerCombo.setSelectedIndex(index);

		pack();
		setLocationRelativeTo(view);
		show();
	}

	// EnhancedDialog implementation
	public void ok()
	{
		dispose();
	}

	public void cancel()
	{
		dispose();
	}
	// end EnhancedDialog implementation

	// private members
	private JComboBox registerCombo;
	private JLabel type;
	private Vector contents;
	private JTextArea contentTextArea;
	private JButton close;

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(evt.getSource() == registerCombo)
			{
				Registers.Register reg = (Registers.Register)
					contents.elementAt(registerCombo
					.getSelectedIndex());

				if(reg == null)
					return;

				String typeString;
				if(reg instanceof Registers.StringRegister
					|| reg instanceof Registers.ClipboardRegister)
					typeString = jEdit.getProperty("view-registers.text");
				else if(reg instanceof Registers.CaretRegister)
					typeString = jEdit.getProperty("view-registers.position");
				else
					typeString = jEdit.getProperty("view-registers.unknown");

				type.setText(typeString);
				contentTextArea.setText(reg.toString());
			}
			else if(evt.getSource() == close)
				cancel();
		}
	}
}
