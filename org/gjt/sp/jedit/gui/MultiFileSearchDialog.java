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

import javax.swing.border.TitledBorder;
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

		this.view = view;

		ActionHandler actionListener = new ActionHandler();

		getContentPane().setLayout(new BorderLayout());

		JPanel panel = new JPanel(new GridLayout(5,1));

		JLabel caption = new JLabel(jEdit.getProperty("search.multifile.caption"));
		panel.add(caption);

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
		selected.getModel().setSelected(fileset instanceof BufferListSet
			&& !(fileset instanceof DirectoryListSet));
		selected.addActionListener(actionListener);

		panel.add(directory = new JRadioButton(jEdit.getProperty(
			"search.multifile.directory")));
		grp.add(directory);
		directory.getModel().setSelected(fileset instanceof DirectoryListSet);
		directory.addActionListener(actionListener);

		getContentPane().add(BorderLayout.NORTH,panel);

		options = new JPanel(new GridLayout(1,1));
		options.setBorder(new TitledBorder(jEdit.getProperty(
			"search.multifile.options")));

		getContentPane().add(BorderLayout.CENTER,options);

		updateOptions();

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

	// private members
	private View view;

	private boolean okClicked;
	private JRadioButton current;
	private JRadioButton all;
	private JRadioButton selected;
	private JRadioButton directory;

	private JPanel options;

	private JList bufferList;
	private JTextField directoryPath;
	private JButton directoryChoose;
	private JCheckBox directoryRecurse;
	private JButton ok;
	private JButton cancel;

	private void updateOptions()
	{
		for(int i = 0; i < options.getComponentCount(); i++)
		{
			options.remove(i);
		}

		if(selected.getModel().isSelected())
			options.add(new BufferListOptions());
		else if(directory.getModel().isSelected())
			options.add(new DirectoryListOptions());
		else
			options.add(new JLabel(jEdit.getProperty(
				"search.multifile.no-options")));

		if(isShowing())
		{
			pack();
			validate();
		}
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();

			if(source == current || source == all
				|| source == selected || source == directory)
			{
				updateOptions();
			}
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

	class BufferListOptions extends JPanel
	{
		BufferListOptions()
		{
			super(new GridLayout(1,1));

			DefaultListModel bufferListModel = new DefaultListModel();

			Buffer buffer = jEdit.getFirstBuffer();
			while(buffer != null)
			{
				bufferListModel.addElement(buffer.getPath());
				buffer = buffer.getNext();
			}

			bufferList = new JList(bufferListModel);

			SearchFileSet fileset = SearchAndReplace.getSearchFileSet();

			if(fileset instanceof BufferListSet
				&& !(fileset instanceof DirectoryListSet))
			{
				buffer = fileset.getFirstBuffer(view);
				do
				{
					for(int j = 0; j < bufferListModel.getSize(); j++)
					{
						if(bufferListModel.getElementAt(j) == buffer)
							bufferList.addSelectionInterval(j,j);
					}
				}
				while((buffer = fileset.getNextBuffer(view,buffer)) != null);
			}

			add(new JScrollPane(bufferList));
		}
	}

	class DirectoryListOptions extends JPanel {}
}
