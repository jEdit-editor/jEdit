/*
 * ToolBarOptionPane.java - Tool bar options panel
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
import java.net.*;
import java.util.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.*;

/**
 * Tool bar editor.
 * @author Slava Pestov
 * @version $Id$
 */
public class ToolBarOptionPane extends AbstractOptionPane
{
	public ToolBarOptionPane()
	{
		super("toolbar");
	}

	// protected members
	protected void _init()
	{
		/* Show toolbar */
		showToolbar = new JCheckBox(jEdit.getProperty(
			"options.general.showToolbar"));
		showToolbar.getModel().setSelected(jEdit.getBooleanProperty(
			"view.showToolbar"));
		addComponent(showToolbar);

		/* Show search bar */
		showSearchbar = new JCheckBox(jEdit.getProperty(
			"options.general.showSearchbar"));
		showSearchbar.getModel().setSelected(jEdit.getBooleanProperty(
			"view.showSearchbar"));
		addComponent(showSearchbar);

		addComponent(new JLabel(jEdit.getProperty(
			"options.toolbar.caption")));

		String toolbar = jEdit.getProperty("view.toolbar");
		StringTokenizer st = new StringTokenizer(toolbar);
		listModel = new DefaultListModel();
		while(st.hasMoreTokens())
		{
			String actionName = (String)st.nextToken();

			Icon icon;
			String label;
			if(actionName.equals("-"))
			{
				label = "-";
				icon = null;
			}
			else
			{
				label = jEdit.getProperty(actionName + ".label");
				if(label == null)
					continue;

				// get the icon
				URL url;
				String iconName = jEdit.getProperty(actionName + ".icon");
				if(iconName == null)
					continue;

				if(iconName.startsWith("file:"))
				{
					try
					{
						url = new URL(iconName);
					}
					catch(MalformedURLException mf)
					{
						url = null;
					}
				}
				else
				{
					url = getClass().getResource("/org/gjt/sp/jedit/toolbar/"
						+ iconName);
				}
				icon = new ImageIcon(url);
			}
			listModel.addElement(new Button(actionName,icon,label));
		}

		list = new JList(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addListSelectionListener(new ListHandler());
		list.setCellRenderer(new ButtonCellRenderer());

		// Make the list take up the rest of the option pane
		GridBagConstraints cons = new GridBagConstraints();
		cons.gridy = y++;
		cons.gridheight = cons.REMAINDER;
		cons.gridwidth = cons.REMAINDER;
		cons.fill = GridBagConstraints.BOTH;
		cons.weightx = 1.0f;

		gridBag.setConstraints(list,cons);
		add(list);

		add(BorderLayout.CENTER,new JScrollPane(list));

		JPanel buttons = new JPanel();
		ActionHandler actionHandler = new ActionHandler();
		add = new JButton(jEdit.getProperty("options.toolbar.add"));
		add.addActionListener(actionHandler);
		buttons.add(add);
		remove = new JButton(jEdit.getProperty("options.toolbar.remove"));
		remove.addActionListener(actionHandler);
		buttons.add(remove);
		moveUp = new JButton(jEdit.getProperty("options.toolbar.moveUp"));
		moveUp.addActionListener(actionHandler);
		buttons.add(moveUp);
		moveDown = new JButton(jEdit.getProperty("options.toolbar.moveDown"));
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
			vector.addElement(new Button(actionName,null,label));
		}
		MiscUtilities.quicksort(vector,new ButtonCompare());

		actionsList = new DefaultListModel();
		actionsList.ensureCapacity(vector.size());
		for(int i = 0; i < vector.size(); i++)
		{
			actionsList.addElement(vector.elementAt(i));
		}
	}

	class ButtonCompare implements MiscUtilities.Compare
	{
		public int compare(Object obj1, Object obj2)
		{
			return ((Button)obj1).label.toLowerCase().compareTo(
				((Button)obj2).label.toLowerCase());
		}
	}

	protected void _save()
	{
		jEdit.setBooleanProperty("view.showToolbar",showToolbar.getModel()
			.isSelected());
		jEdit.setBooleanProperty("view.showSearchbar",showSearchbar.getModel()
			.isSelected());

		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < listModel.getSize(); i++)
		{
			if(i != 0)
				buf.append(' ');
			buf.append(((Button)listModel.elementAt(i)).actionName);
		}
		jEdit.setProperty("view.toolbar",buf.toString());

		GUIUtilities.invalidateToolBarModel("view.toolbar");
	}

	// private members
	private JCheckBox showToolbar;
	private JCheckBox showSearchbar;
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

	static class Button
	{
		String actionName;
		Icon icon;
		String label;

		Button(String actionName, Icon icon, String label)
		{
			this.actionName = actionName;
			this.icon = icon;
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

	class ButtonCellRenderer extends DefaultListCellRenderer
	{
		public Component getListCellRendererComponent(JList list,
			Object value, int index, boolean isSelected,
			boolean cellHasFocus)
		{
			super.getListCellRendererComponent(list,value,index,
				isSelected,cellHasFocus);

			Button button = (Button)value;
			setText(button.label);
			setIcon(button.icon);

			return this;
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
					ToolBarOptionPane.this,
					actionsList);
				Button selection = dialog.getSelection();
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
			jEdit.getProperty("options.toolbar.add.title"),
			true);
		getContentPane().add(BorderLayout.NORTH,new JLabel(
			jEdit.getProperty("options.toolbar.add.caption")));

		JPanel centerPanel = new JPanel(new BorderLayout());
		JPanel radioPanel = new JPanel(new GridLayout(2,1));

		ActionHandler actionHandler = new ActionHandler();
		ButtonGroup grp = new ButtonGroup();

		separator = new JRadioButton(jEdit.getProperty("options.toolbar"
			+ ".add.separator"));
		separator.getModel().setSelected(true);
		separator.addActionListener(actionHandler);
		grp.add(separator);
		radioPanel.add(separator);

		action = new JRadioButton(jEdit.getProperty("options.toolbar"
			+ ".add.action"));
		action.addActionListener(actionHandler);
		grp.add(action);
		radioPanel.add(action);

		centerPanel.add(BorderLayout.NORTH,radioPanel);

		list = new JList(actionsList);
		list.setVisibleRowCount(16);
		list.setEnabled(false);
		centerPanel.add(BorderLayout.CENTER,new JScrollPane(list));

		JPanel iconPanel = new JPanel();
		// XXX
		centerPanel.add(BorderLayout.SOUTH,iconPanel);

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

	public ToolBarOptionPane.Button getSelection()
	{
		if(!isOK)
			return null;

		if(separator.getModel().isSelected())
			return new ToolBarOptionPane.Button("-",null,"-");
		else if(action.getModel().isSelected())
		{
			return (ToolBarOptionPane.Button)list.getSelectedValue();
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
 * Change Log:
 * $Log$
 * Revision 1.1  2000/05/14 10:55:22  sp
 * Tool bar editor started, improved view registers dialog box
 *
 */
