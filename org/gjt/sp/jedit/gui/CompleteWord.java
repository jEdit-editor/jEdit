/*
 * CompleteWord.java - Complete word dialog
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
import java.util.Vector;
import org.gjt.sp.jedit.*;

public class CompleteWord extends EnhancedDialog
implements ActionListener, MouseListener
{
	public CompleteWord(View view, String word, Vector completions)
	{
		super(view,jEdit.getProperty("complete-word.title"),true);
		this.view = view;
		this.word = word;
		Container content = getContentPane();

		words = new JList(completions);

		words.setVisibleRowCount(Math.min(completions.size(),16));
		words.setFont(view.getTextArea().getPainter().getFont());

		words.addMouseListener(this);
		words.setSelectedIndex(0);
		insert = new JButton(jEdit.getProperty("complete-word.insert"));
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		content.setLayout(new BorderLayout());
		String[] args = { word };
		content.add(new JLabel(jEdit.getProperty("complete-word.caption",args)),
			BorderLayout.NORTH);

		JScrollPane scroller = new JScrollPane(words);
		Dimension dim = scroller.getPreferredSize();

		content.add(scroller, BorderLayout.CENTER);
		JPanel panel = new JPanel();
		panel.add(insert);
		panel.add(cancel);
		content.add(panel, BorderLayout.SOUTH);

		getRootPane().setDefaultButton(insert);
		insert.addActionListener(this);
		cancel.addActionListener(this);

		Dimension screen = getToolkit().getScreenSize();
		pack();
		setLocation((screen.width - getSize().width) / 2,
			(screen.height - getSize().height) / 2);
		show();
		words.requestFocus();
	}

	// EnhancedDialog implementation
	public void ok()
	{
		int selected = words.getSelectedIndex();

		if(selected == -1)
		{
			view.getToolkit().beep();
			return;
		}

		view.getTextArea().setSelectedText(((String)words
			.getSelectedValue()).substring(word.length()));

		dispose();
	}

	public void cancel()
	{
		dispose();
	}
	// end EnhancedDialog implementation

	public void actionPerformed(ActionEvent evt)
	{
		Object source = evt.getSource();
		if(source == insert)
			ok();
		else if(source == cancel)
			cancel();
	}

	public void mouseClicked(MouseEvent evt)
	{
		if(evt.getClickCount() == 2)
			ok();
	}

	public void mouseEntered(MouseEvent evt) {}
	public void mouseExited(MouseEvent evt) {}
	public void mousePressed(MouseEvent evt) {}
	public void mouseReleased(MouseEvent evt) {}

	// private members
	private View view;
	private String word;
	private JList words;
	private JButton insert;
	private JButton cancel;
}
