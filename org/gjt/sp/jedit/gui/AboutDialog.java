/*
 * AbooutDialog.java - About jEdit dialog box
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

import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.StringTokenizer;
import org.gjt.sp.jedit.*;

public class AboutDialog extends EnhancedDialog
{
	public AboutDialog(View view)
	{
		super(view,jEdit.getProperty("about.title"),true);

		JLabel label = new JLabel(
			new ImageIcon(getClass().getResource(
			"/org/gjt/sp/jedit/jedit_logo.jpg")));
		label.setBorder(new EmptyBorder(10,10,10,10));
		getContentPane().add(BorderLayout.NORTH,label);
		JPanel panel = new JPanel(new GridLayout(0,1));
		String[] args = { jEdit.getVersion() };
		StringTokenizer st = new StringTokenizer(
			jEdit.getProperty("about.message",args),"\n");
		while(st.hasMoreTokens())
		{
			panel.add(new JLabel(st.nextToken(),
				SwingConstants.CENTER));
		}
		getContentPane().add(BorderLayout.CENTER,panel);

		panel = new JPanel();
		ok = new JButton(jEdit.getProperty("common.ok"));
		ok.addActionListener(new ActionHandler());
		getRootPane().setDefaultButton(ok);
		panel.add(ok);
		getContentPane().add(BorderLayout.SOUTH,panel);

		Dimension screen = getToolkit().getScreenSize();
		pack();
		setLocation((screen.width - getSize().width) / 2,
			(screen.height - getSize().height) / 2);
		show();
	}

	public void ok()
	{
		dispose();
	}

	public void cancel()
	{
		dispose();
	}

	// private members
	private JButton ok;

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			dispose();
		}
	}
}
