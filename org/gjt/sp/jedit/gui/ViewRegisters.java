/*
 * ViewRegisters.java - View registers dialog
 * Copyright (C) 1999 Slava Pestov
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

public class ViewRegisters extends JDialog
{
	public ViewRegisters(View view)
	{
		super(view,jEdit.getProperty("view-registers.title"),true);

		Container content = getContentPane();

		Registers.Register[] registers = Registers.getRegisters();
		Vector strings = new Vector();

		for(int i = 0; i < registers.length; i++)
		{
			Registers.Register reg = registers[i];
			if(reg == null)
				continue;

			String value = reg.toString();
			if(value == null)
				continue;

			strings.addElement((char)i + ": " + value);
		}

		JList registerList = new JList(strings);
		registerList.setVisibleRowCount(10);
		registerList.setFont(view.getTextArea().getPainter().getFont());

		close = new JButton(jEdit.getProperty("common.close"));

		content.setLayout(new BorderLayout());

		content.add(new JLabel(jEdit.getProperty("view-registers.caption")),
			BorderLayout.NORTH);

		JScrollPane scroller = new JScrollPane(registerList);
		Dimension dim = scroller.getPreferredSize();
		scroller.setPreferredSize(new Dimension(640,dim.height));

		content.add(scroller, BorderLayout.CENTER);

		JPanel panel = new JPanel();
		panel.add(close);
		content.add(panel, BorderLayout.SOUTH);

		addKeyListener(new KeyHandler());
		getRootPane().setDefaultButton(close);

		ActionHandler actionListener = new ActionHandler();
		close.addActionListener(actionListener);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		Dimension screen = getToolkit().getScreenSize();
		pack();
		setLocation((screen.width - getSize().width) / 2,
			(screen.height - getSize().height) / 2);
		show();
		registerList.requestFocus();
	}

	// private members
	private JButton close;

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(evt.getSource() == close)
				dispose();
		}
	}

	class KeyHandler extends KeyAdapter
	{
		public void keyPressed(KeyEvent evt)
		{
			switch(evt.getKeyCode())
			{
			case KeyEvent.VK_ENTER:
			case KeyEvent.VK_ESCAPE:
				dispose();
				break;
			}
		}
	}
}
