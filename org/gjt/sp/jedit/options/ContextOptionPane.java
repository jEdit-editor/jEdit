/*
 * ContextOptionPane.java - Abbrevs options panel
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

package org.gjt.sp.jedit.options;

import javax.swing.event.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import org.gjt.sp.jedit.*;

/**
 * Right-click context menu editor.
 * @author Slava Pestov
 * @version $Id$
 */
public class ContextOptionPane extends AbstractOptionPane
{
	public ContextOptionPane()
	{
		super("context");
	}

	// protected members
	protected void _init()
	{
		setLayout(new BorderLayout());

		add(BorderLayout.NORTH,new JLabel(jEdit.getProperty(
			"options.context.caption")));

		String contextMenu = jEdit.getProperty("view.context");
		StringTokenizer st = new StringTokenizer(contextMenu);
		listModel = new DefaultListModel();
		while(st.hasMoreTokens())
		{
			listModel.addElement(new MenuItem(
				(String)st.nextToken()));
		}
		list = new JList(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addListSelectionListener(new ListHandler());

		add(BorderLayout.CENTER,new JScrollPane(list));

		JPanel buttons = new JPanel();
		ActionHandler actionHandler = new ActionHandler();
		add = new JButton(jEdit.getProperty("options.context.add"));
		add.addActionListener(actionHandler);
		buttons.add(add);
		remove = new JButton(jEdit.getProperty("options.context.remove"));
		remove.addActionListener(actionHandler);
		buttons.add(remove);
		moveUp = new JButton(jEdit.getProperty("options.context.moveUp"));
		moveUp.addActionListener(actionHandler);
		buttons.add(moveUp);
		moveDown = new JButton(jEdit.getProperty("options.context.moveDown"));
		moveDown.addActionListener(actionHandler);
		buttons.add(moveDown);

		updateButtons();
		add(BorderLayout.SOUTH,buttons);
	}

	protected void _save()
	{
		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < listModel.getSize(); i++)
		{
			if(i != 0)
				buf.append(' ');
			buf.append(((MenuItem)listModel.elementAt(i)).actionName);
		}
		jEdit.setProperty("view.context",buf.toString());

		GUIUtilities.invalidateMenuModel("view.context");
	}

	// private members
	private DefaultListModel listModel;
	private JList list;
	private JButton add;
	private JButton remove;
	private JButton moveUp, moveDown;

	private void updateButtons()
	{
		int index = list.getSelectedIndex();
		remove.setEnabled(index != -1);
		moveUp.setEnabled(index > 0);
		moveDown.setEnabled(index != -1 && index != listModel.getSize() - 1);
	}

	class MenuItem
	{
		String actionName;
		String label;

		MenuItem(String actionName)
		{
			this.actionName = actionName;
			if(actionName.equals("-"))
				label = actionName;
			else
			{
				label = GUIUtilities.prettifyMenuLabel(
					jEdit.getProperty(actionName + ".label"));
			}
		}

		public String toString()
		{
			return label;
		}
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();

			if(source == remove)
			{
				int index = list.getSelectedIndex();
				listModel.removeElementAt(index);
				updateButtons();
			}
			else if(source == moveUp)
			{
				int index = list.getSelectedIndex();
				Object selected = list.getSelectedValue();
				listModel.removeElementAt(index);
				listModel.insertElementAt(selected,index-1);
				list.setSelectedIndex(index-1);
			}
			else if(source == moveDown)
			{
				int index = list.getSelectedIndex();
				Object selected = list.getSelectedValue();
				listModel.removeElementAt(index);
				listModel.insertElementAt(selected,index+1);
				list.setSelectedIndex(index+1);
			}
		}
	}

	class ListHandler implements ListSelectionListener
	{
		public void valueChanged(ListSelectionEvent evt)
		{
			updateButtons();
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.1  2000/04/18 08:27:52  sp
 * Context menu editor started
 *
 */
