/*
 * PastePrevious.java - Paste previous dialog
 * Copyright (C) 1998, 1999 Slava Pestov
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

public class PastePrevious extends JDialog
implements ActionListener, KeyListener, ListSelectionListener, MouseListener
{
	public PastePrevious(View view)
	{
		super(view,jEdit.getProperty("pasteprev.title"),true);
		this.view = view;
		Container content = getContentPane();
		clipHistory = HistoryModel.getModel("clipboard");

		clips = new JList(new AbstractListModel() {
			public int getSize()
			{
				return clipHistory.getSize();
			}

			public Object getElementAt(int index)
			{
				return clipHistory.getItem(index);
			}
		});

		clips.setVisibleRowCount(16);
		clips.setFont(view.getTextArea().getPainter().getFont());

		clips.addMouseListener(this);
		clips.addListSelectionListener(this);
		insert = new JButton(jEdit.getProperty("pasteprev.insert"));
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		content.setLayout(new BorderLayout());
		content.add(new JLabel(jEdit.getProperty("pasteprev.caption")),
			BorderLayout.NORTH);

		JScrollPane scroller = new JScrollPane(clips);
		Dimension dim = scroller.getPreferredSize();
		scroller.setPreferredSize(new Dimension(640,dim.height));

		content.add(scroller, BorderLayout.CENTER);
		JPanel panel = new JPanel();
		panel.add(insert);
		panel.add(cancel);
		content.add(panel, BorderLayout.SOUTH);
		updateButtons();

		addKeyListener(this);
		getRootPane().setDefaultButton(insert);
		insert.addActionListener(this);
		cancel.addActionListener(this);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		Dimension screen = getToolkit().getScreenSize();
		pack();
		setLocation((screen.width - getSize().width) / 2,
			(screen.height - getSize().height) / 2);
		show();
		clips.requestFocus();
	}

	public void actionPerformed(ActionEvent evt)
	{
		Object source = evt.getSource();
		if(source == insert)
			doInsert();
		else if(source == cancel)
			dispose();
	}

	public void keyPressed(KeyEvent evt)
	{
		switch(evt.getKeyCode())
		{
		case KeyEvent.VK_ENTER:
			doInsert();
			break;
		case KeyEvent.VK_ESCAPE:
			dispose();
			break;
		}
	}

	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}

	public void mouseClicked(MouseEvent evt)
	{
		if (evt.getClickCount() == 2)
		{
			doInsert();
		}
	}

	public void mouseEntered(MouseEvent evt) {}
	public void mouseExited(MouseEvent evt) {}
	public void mousePressed(MouseEvent evt) {}
	public void mouseReleased(MouseEvent evt) {}

	public void valueChanged(ListSelectionEvent evt)
	{
		updateButtons();
	}

	// private members
	private View view;
	private JList clips;
	private HistoryModel clipHistory;
	private JButton insert;
	private JButton cancel;

	private void updateButtons()
	{
		int selected = clips.getSelectedIndex();
		insert.setEnabled(selected != -1);
	}

	private void doInsert()
	{
		int selected = clips.getSelectedIndex();

		if(selected == -1)
		{
			view.getToolkit().beep();
			return;
		}

		String clip = clipHistory.getItem(selected);

		int repeatCount = view.getTextArea().getInputHandler()
			.getRepeatCount();
		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < repeatCount; i++)
			buf.append(clip);

		view.getTextArea().setSelectedText(buf.toString());

		dispose();
	}
}
