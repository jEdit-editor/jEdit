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

public class CompleteWord extends JWindow
{
	public CompleteWord(View view, String word, Vector completions,
		Point location)
	{
		super(view);

		this.view = view;
		this.word = word;

		words = new JList(completions);

		words.setVisibleRowCount(Math.min(completions.size(),8));

		words.addMouseListener(new MouseHandler());
		words.setSelectedIndex(0);
		words.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JScrollPane scroller = new JScrollPane(words);

		getContentPane().add(scroller, BorderLayout.CENTER);

		// possible workaround for a bug some people have been seeing?
		Dimension dim = words.getPreferredSize();
		dim.width += scroller.getVerticalScrollBar().getPreferredSize()
			.width;
		words.setPreferredSize(dim);
		GUIUtilities.requestFocus(this,words);

		pack();
		setLocation(location);
		show();

		view.setKeyEventInterceptor(new KeyHandler());
	}

	public void dispose()
	{
		view.setKeyEventInterceptor(null);
		super.dispose();
	}

	// private members
	private View view;
	private String word;
	private JList words;

	private void insertSelected()
	{
		view.getTextArea().setSelectedText(((String)words
			.getSelectedValue()).substring(word.length()));
		dispose();
	}

	class KeyHandler extends KeyAdapter
	{
		public void keyPressed(KeyEvent evt)
		{
			switch(evt.getKeyCode())
			{
			case KeyEvent.VK_ENTER:
				insertSelected();
				evt.consume();
				break;
			case KeyEvent.VK_ESCAPE:
				dispose();
				evt.consume();
				break;
			case KeyEvent.VK_UP:
				int selected = words.getSelectedIndex();
				if(selected == 0)
					selected = words.getModel().getSize() - 1;
				else
					selected = selected - 1;
	
				words.setSelectedIndex(selected);
				words.ensureIndexIsVisible(selected);

				evt.consume();
				break;
			case KeyEvent.VK_DOWN:
				selected = words.getSelectedIndex();
				if(selected == words.getModel().getSize() - 1)
					selected = 0;
				else
					selected = selected - 1;

				words.setSelectedIndex(selected);
				words.ensureIndexIsVisible(selected);

				evt.consume();
				break;
			default:
				dispose();
				view.processKeyEvent(evt);
				break;
			}
		}

		public void keyTyped(KeyEvent evt)
		{
			char ch = evt.getKeyChar();
			if(ch == KeyEvent.CHAR_UNDEFINED ||
				ch < 0x20 || ch == 0x7f
				|| (evt.getModifiers() & KeyEvent.ALT_MASK) != 0)
				return;

			dispose();
			view.processKeyEvent(evt);
		}
	}

	class MouseHandler extends MouseAdapter
	{
		public void mouseClicked(MouseEvent evt)
		{
			insertSelected();
		}
	}
}
