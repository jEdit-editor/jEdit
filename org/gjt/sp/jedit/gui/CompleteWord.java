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
import javax.swing.border.EmptyBorder;
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

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		words = new JList(completions);

		words.setVisibleRowCount(Math.min(completions.size(),16));
		words.setFont(view.getTextArea().getPainter().getFont());

		words.addMouseListener(this);
		words.setSelectedIndex(0);
		words.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		insert = new JButton(jEdit.getProperty("complete-word.insert"));
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		content.setLayout(new BorderLayout());
		String[] args = { word };
		JLabel label = new JLabel(jEdit.getProperty("complete-word.caption",args));
		label.setBorder(new EmptyBorder(0,0,6,0));
		content.add(label,BorderLayout.NORTH);

		JScrollPane scroller = new JScrollPane(words);
		Dimension dim = scroller.getPreferredSize();

		content.add(scroller, BorderLayout.CENTER);
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
		panel.setBorder(new EmptyBorder(12,0,0,0));
		panel.add(Box.createGlue());
		panel.add(insert);
		panel.add(Box.createHorizontalStrut(6));
		panel.add(cancel);
		panel.add(Box.createGlue());
		content.add(panel, BorderLayout.SOUTH);

		getRootPane().setDefaultButton(insert);
		insert.addActionListener(this);
		cancel.addActionListener(this);

		GUIUtilities.requestFocus(this,words);

		pack();
		setLocationRelativeTo(view);
		show();
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
