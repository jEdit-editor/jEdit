/*
 * MultiFileSearchDialog.java - Multifile earch and replace dialog
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
import java.awt.event.*;
import java.awt.*;
import java.util.Vector;
import org.gjt.sp.jedit.search.*;
import org.gjt.sp.jedit.*;

/**
 * The dialog for selecting the file set to search in.
 * @author Slava Pestov
 * @version $Id$
 */
public class MultiFileSearchDialog extends JDialog
{
	public MultiFileSearchDialog(View view, SearchFileSet fileset)
	{
		super(view,jEdit.getProperty("search.multifile.title"),true);

		ActionHandler actionListener = new ActionHandler();

		getContentPane().setLayout(new BorderLayout());

		JPanel panel = new JPanel(new GridLayout(4,1));

		panel.add(new JLabel(jEdit.getProperty("search.multifile.caption")));

		ButtonGroup grp = new ButtonGroup();
		panel.add(current = new JRadioButton(jEdit.getProperty(
			"search.multifile.current")));
		grp.add(current);
		current.getModel().setSelected(fileset instanceof CurrentBufferSet);
		current.addActionListener(actionListener);

		panel.add(all = new JRadioButton(jEdit.getProperty(
			"search.multifile.all")));
		grp.add(all);
		all.getModel().setSelected(fileset instanceof AllBufferSet);
		all.addActionListener(actionListener);

		panel.add(selected = new JRadioButton(jEdit.getProperty(
			"search.multifile.selected")));
		grp.add(selected);
		selected.getModel().setSelected(fileset instanceof BufferListSet);
		selected.addActionListener(actionListener);

		getContentPane().add(BorderLayout.NORTH,panel);

		DefaultListModel bufferListModel = new DefaultListModel();

		Buffer buffer = jEdit.getFirstBuffer();
		while(buffer != null)
		{
			bufferListModel.addElement(buffer.getPath());
			buffer = buffer.getNext();
		}

		bufferList = new JList(bufferListModel);
		bufferList.setSelectionMode(ListSelectionModel
			.MULTIPLE_INTERVAL_SELECTION);

		if(fileset instanceof BufferListSet)
		{
			bufferList.setEnabled(true);
			buffer = fileset.getFirstBuffer(view);
			do
			{
				for(int j = 0; j < bufferListModel.getSize(); j++)
				{
					if(bufferListModel.getElementAt(j) == buffer)
						bufferList.addSelectionInterval(j,j);
				}
			}
			while((buffer = buffer.getNext()) != null);
		}
		else
			bufferList.setEnabled(false);

		getContentPane().add(BorderLayout.CENTER,
			new JScrollPane(bufferList));

		panel = new JPanel();
		panel.add(ok = new JButton(jEdit.getProperty("common.ok")));
		ok.addActionListener(actionListener);
		getRootPane().setDefaultButton(ok);
		panel.add(cancel = new JButton(jEdit.getProperty("common.cancel")));
		cancel.addActionListener(actionListener);
		getContentPane().add(BorderLayout.SOUTH,panel);

		addKeyListener(new KeyHandler());

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		Dimension screen = getToolkit().getScreenSize();
		pack();
		setLocation((screen.width - getSize().width) / 2,
			(screen.height - getSize().height) / 2);
		show();
	}

	// private members
	private boolean okClicked;
	private JRadioButton current;
	private JRadioButton all;
	private JRadioButton selected;
	private JList bufferList;
	private JButton ok;
	private JButton cancel;

	public SearchFileSet getSearchFileSet()
	{
		if(!okClicked)
			return null;

		if(current.getModel().isSelected())
			return new CurrentBufferSet();
		else if(all.getModel().isSelected())
			return new AllBufferSet();
		else if(selected.getModel().isSelected())
		{
			Object[] buffers = bufferList.getSelectedValues();
			if(buffers == null || buffers.length == 0)
				return new CurrentBufferSet();
			else
				return new BufferListSet(buffers);
		}
		return null;
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();

			if(source == current || source == all)
				bufferList.setEnabled(false);
			else if(source == selected)
				bufferList.setEnabled(true);
			else if(source == ok)
			{
				okClicked = true;
				dispose();
			}
			else if(source == cancel)
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
				okClicked = true;
				dispose();
				evt.consume();
				break;
			case KeyEvent.VK_ESCAPE:
				dispose();
				evt.consume();
				break;
			}
		}
	}
}
