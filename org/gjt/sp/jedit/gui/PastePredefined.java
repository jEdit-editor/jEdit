/*
 * PastePredefined.java - Paste predefined dialog
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
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import org.gjt.sp.jedit.*;

public class PastePredefined extends JDialog
implements ActionListener, KeyListener, MouseListener
{
	public PastePredefined(View view)
	{
		super(view,jEdit.getProperty("pastepredef.title"),true);
		this.view = view;
		Container content = getContentPane();
		content.setLayout(new BorderLayout());

		content.add(new JLabel(jEdit.getProperty(
			"pastepredef.caption")), BorderLayout.NORTH);

		clipModel = new DefaultListModel();

		int i = 0;
		for(;;)
		{
			String clip = jEdit.getProperty("clipPredefined." + i);
			if(clip == null)
				break;
			clipModel.addElement(clip);
			i++;
		}

		clips = new JList(clipModel);
		clips.setVisibleRowCount(16);

		clips.setFont(view.getTextArea().getPainter().getFont());
		clips.addMouseListener(this);

		JScrollPane scroller = new JScrollPane(clips);
		Dimension dim = scroller.getPreferredSize();
		scroller.setPreferredSize(new Dimension(640,dim.height));

		content.add(scroller, BorderLayout.CENTER);

		JPanel buttons = new JPanel();
		insert = new JButton(jEdit.getProperty("common.insert"));
		insert.addActionListener(this);
		buttons.add(insert);
		add = new JButton(jEdit.getProperty("common.add"));
		add.addActionListener(this);
		buttons.add(add);
		delete = new JButton(jEdit.getProperty("common.delete"));
		delete.addActionListener(this);
		buttons.add(delete);
		edit = new JButton(jEdit.getProperty("common.edit"));
		edit.addActionListener(this);
		buttons.add(edit);
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(this);
		buttons.add(cancel);
		content.add(buttons, BorderLayout.SOUTH);

		getRootPane().setDefaultButton(insert);
		addKeyListener(this);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		Dimension screen = getToolkit().getScreenSize();
		pack();
		setLocation((screen.width - getSize().width) / 2,
			(screen.height - getSize().height) / 2);
		show();
		clips.requestFocus();
	}

	public void dispose()
	{
		int i = 0;
		while(i < clipModel.getSize())
		{
			jEdit.setProperty("clipPredefined." + i,
				(String)clipModel.getElementAt(i));
			i++;
		}
		jEdit.unsetProperty("clipPredefined." + i);
		super.dispose();
	}

	public void actionPerformed(ActionEvent evt)
	{
		Object source = evt.getSource();
		
		if(source == insert)
		{
			doInsert();
		}
		else if(source == add)
		{
			String clip = new ClippingEditor(view,null,false).getText();
			if(clip != null)
				clipModel.addElement(clip);
		}
		else if(source == delete)
		{
			int index = clips.getSelectedIndex();
			if(index != -1)
				clipModel.removeElementAt(index);
		}
		else if(source == edit)
		{
			int index = clips.getSelectedIndex();
			if(index != -1)
			{
				String clip = new ClippingEditor(view,(String)clipModel
					.getElementAt(index),false).getText();
				if(clip != null)
				{
					clipModel.setElementAt(clip,index);
				}
			}
		}
		else if(source == cancel)
			dispose();
	}

	public void keyPressed(KeyEvent evt)
	{
		switch(evt.getKeyCode())
		{
		case KeyEvent.VK_ENTER:
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

	// private members
	private View view;
	private JList clips;
	private DefaultListModel clipModel;
	private JButton insert;
	private JButton add;
	private JButton delete;
	private JButton edit;
	private JButton cancel;

	private void doInsert()
	{
		int index = clips.getSelectedIndex();
		if(index != -1)
		{
			String clip = (String)clipModel.getElementAt(index);

			int repeatCount = view.getTextArea().getInputHandler()
				.getRepeatCount();
			StringBuffer buf = new StringBuffer();
			for(int i = 0; i < repeatCount; i++)
				buf.append(clip);

			view.getTextArea().setSelectedText(buf.toString());
		}
		dispose();
	}
}
