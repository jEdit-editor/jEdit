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
import org.gjt.sp.jedit.gui.*;
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
			String actionName = (String)st.nextToken();
			String label;
			if(actionName.equals("-"))
				label = "-";
			else
			{
				label = jEdit.getProperty(actionName + ".label");
				if(label == null)
					continue;
			}
			listModel.addElement(new MenuItem(actionName,label));
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

		// create actions list
		EditAction[] actions = jEdit.getActions();
		Vector vector = new Vector(actions.length);
		for(int i = 0; i < actions.length; i++)
		{
			String actionName = actions[i].getName();
			String label = jEdit.getProperty(actionName + ".label");
			if(label == null)
				continue;
			vector.addElement(new MenuItem(actionName,label));
		}
		MiscUtilities.quicksort(vector,new MenuItemCompare());

		actionsList = new DefaultListModel();
		actionsList.ensureCapacity(vector.size());
		for(int i = 0; i < vector.size(); i++)
		{
			actionsList.addElement(vector.elementAt(i));
		}
	}

	class MenuItemCompare implements MiscUtilities.Compare
	{
		public int compare(Object obj1, Object obj2)
		{
			return ((MenuItem)obj1).label.toLowerCase().compareTo(
				((MenuItem)obj2).label.toLowerCase());
		}
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

	private DefaultListModel actionsList;

	private void updateButtons()
	{
		int index = list.getSelectedIndex();
		remove.setEnabled(index != -1 && listModel.getSize() != 0);
		moveUp.setEnabled(index > 0);
		moveDown.setEnabled(index != -1 && index != listModel.getSize() - 1);
	}

	static class MenuItem
	{
		String actionName;
		String label;

		MenuItem(String actionName, String label)
		{
			this.actionName = actionName;
			if(label.equals("-"))
				this.label = label;
			else
				this.label = GUIUtilities.prettifyMenuLabel(label);
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

			if(source == add)
			{
				AddDialog dialog = new AddDialog(
					ContextOptionPane.this,
					actionsList);
				MenuItem selection = dialog.getSelection();
				if(selection == null)
					return;

				int index = list.getSelectedIndex();
				if(index == -1)
					index = listModel.getSize();
				else
					index++;

				listModel.insertElementAt(selection,index);
				list.setSelectedIndex(index);
			}
			else if(source == remove)
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

class AddDialog extends EnhancedDialog
{
	public AddDialog(Component comp, ListModel actionsList)
	{
		super(JOptionPane.getFrameForComponent(comp),
			jEdit.getProperty("options.context.add.title"),
			true);
		getContentPane().add(BorderLayout.NORTH,new JLabel(
			jEdit.getProperty("options.context.add.caption")));

		JPanel centerPanel = new JPanel(new BorderLayout());
		JPanel radioPanel = new JPanel(new GridLayout(2,1));

		ActionHandler actionHandler = new ActionHandler();
		ButtonGroup grp = new ButtonGroup();

		separator = new JRadioButton(jEdit.getProperty("options.context"
			+ ".add.separator"));
		separator.getModel().setSelected(true);
		separator.addActionListener(actionHandler);
		grp.add(separator);
		radioPanel.add(separator);

		action = new JRadioButton(jEdit.getProperty("options.context"
			+ ".add.action"));
		action.addActionListener(actionHandler);
		grp.add(action);
		radioPanel.add(action);

		centerPanel.add(BorderLayout.NORTH,radioPanel);

		list = new JList(actionsList);
		list.setVisibleRowCount(16);
		list.setEnabled(false);
		centerPanel.add(BorderLayout.CENTER,new JScrollPane(list));
		getContentPane().add(BorderLayout.CENTER,centerPanel);

		JPanel southPanel = new JPanel();
		ok = new JButton(jEdit.getProperty("common.ok"));
		ok.addActionListener(actionHandler);
		getRootPane().setDefaultButton(ok);
		southPanel.add(ok);
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(actionHandler);
		southPanel.add(cancel);

		getContentPane().add(BorderLayout.SOUTH,southPanel);

		pack();
		Dimension screen = getToolkit().getScreenSize();
		setLocation((screen.width - getSize().width) / 2,
			(screen.height - getSize().height) / 2);
		show();
	}

	public void ok()
	{
		isOK = true;
		dispose();
	}

	public void cancel()
	{
		dispose();
	}

	public ContextOptionPane.MenuItem getSelection()
	{
		if(!isOK)
			return null;

		if(separator.getModel().isSelected())
			return new ContextOptionPane.MenuItem("-","-");
		else if(action.getModel().isSelected())
		{
			return (ContextOptionPane.MenuItem)list.getSelectedValue();
		}
		else
			throw new InternalError();
	}

	// private members
	private boolean isOK;
	private JRadioButton separator, action;
	private JList list;
	private JButton ok, cancel;

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(source == separator)
				list.setEnabled(false);
			else if(source == action)
				list.setEnabled(true);
			else if(source == ok)
				ok();
			else if(source == cancel)
				cancel();
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.4  2000/04/23 03:58:00  sp
 * ContextOptionPane didn't compile, hack to let JBrowse and QuickFile work
 *
 * Revision 1.3  2000/04/23 03:36:39  sp
 * Minor fixes
 *
 * Revision 1.2  2000/04/18 11:44:31  sp
 * Context menu editor finished
 *
 * Revision 1.1  2000/04/18 08:27:52  sp
 * Context menu editor started
 *
 */
