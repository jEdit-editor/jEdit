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
import java.io.File;
import java.net.*;
import java.util.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

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
		setLayout(new BorderLayout());

		JPanel panel = new JPanel(new GridLayout(3,1));

		/* Show toolbar */
		showToolbar = new JCheckBox(jEdit.getProperty(
			"options.general.showToolbar"));
		showToolbar.setSelected(jEdit.getBooleanProperty("view.showToolbar"));
		panel.add(showToolbar);

		/* Show search bar */
		showSearchbar = new JCheckBox(jEdit.getProperty(
			"options.general.showSearchbar"));
		showSearchbar.setSelected(jEdit.getBooleanProperty(
			"view.showSearchbar"));
		panel.add(showSearchbar);

		panel.add(new JLabel(jEdit.getProperty(
			"options.toolbar.caption")));

		add(BorderLayout.NORTH,panel);

		String toolbar = jEdit.getProperty("view.toolbar");
		StringTokenizer st = new StringTokenizer(toolbar);
		listModel = new DefaultListModel();
		while(st.hasMoreTokens())
		{
			String actionName = (String)st.nextToken();

			Icon icon;
			String label;
			String iconName;
			if(actionName.equals("-"))
			{
				label = "-";
				iconName = null;
				icon = null;
			}
			else
			{
				label = jEdit.getProperty(actionName + ".label");
				if(label == null)
					continue;

				iconName = jEdit.getProperty(actionName + ".icon");
				if(iconName == null)
					continue;

				icon = GUIUtilities.loadToolBarIcon(iconName);
			}
			listModel.addElement(new Button(actionName,iconName,icon,label));
		}

		list = new JList(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addListSelectionListener(new ListHandler());
		list.setCellRenderer(new ButtonCellRenderer());

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
			vector.addElement(new Button(actionName,null,null,label));
		}
		MiscUtilities.quicksort(vector,new ButtonCompare());

		actionsList = new DefaultListModel();
		actionsList.ensureCapacity(vector.size());
		for(int i = 0; i < vector.size(); i++)
		{
			actionsList.addElement(vector.elementAt(i));
		}

		// create icons list
		iconList = new DefaultComboBoxModel();
		st = new StringTokenizer(jEdit.getProperty("icons"));
		while(st.hasMoreElements())
		{
			String icon = st.nextToken();
			iconList.addElement(new IconListEntry(
				GUIUtilities.loadToolBarIcon(icon),icon));
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
		jEdit.setBooleanProperty("view.showToolbar",showToolbar
			.isSelected());
		jEdit.setBooleanProperty("view.showSearchbar",showSearchbar
			.isSelected());

		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < listModel.getSize(); i++)
		{
			if(i != 0)
				buf.append(' ');
			Button button = (Button)listModel.elementAt(i);
			buf.append(button.actionName);
			jEdit.setProperty(button.actionName + ".icon",button.iconName);
		}
		jEdit.setProperty("view.toolbar",buf.toString());

		GUIUtilities.invalidateMenuModels();
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
	private DefaultComboBoxModel iconList;

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
		String iconName;
		Icon icon;
		String label;

		Button(String actionName, String iconName, Icon icon, String label)
		{
			this.actionName = actionName;
			this.iconName = iconName;
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

	static class IconListEntry
	{
		Icon icon;
		String name;

		IconListEntry(Icon icon, String name)
		{
			this.icon = icon;
			this.name = name;
		}
	}

	static class ButtonCellRenderer extends DefaultListCellRenderer
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

	static class IconCellRenderer extends DefaultListCellRenderer
	{
		public Component getListCellRendererComponent(JList list,
			Object value, int index, boolean isSelected,
			boolean cellHasFocus)
		{
			super.getListCellRendererComponent(list,value,index,
				isSelected,cellHasFocus);

			IconListEntry icon = (IconListEntry)value;
			setText(icon.name);
			setIcon(icon.icon);

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
				ToolBarAddDialog dialog = new ToolBarAddDialog(
					ToolBarOptionPane.this,
					actionsList,iconList);
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

class ToolBarAddDialog extends EnhancedDialog
{
	public ToolBarAddDialog(Component comp, ListModel actionsList,
		ComboBoxModel iconList)
	{
		super(JOptionPane.getFrameForComponent(comp),
			jEdit.getProperty("options.toolbar.add.title"),true);
		getContentPane().add(BorderLayout.NORTH,new JLabel(
			jEdit.getProperty("options.toolbar.add.caption")));

		JPanel centerPanel = new JPanel(new BorderLayout());
		JPanel radioPanel = new JPanel(new GridLayout(2,1));

		ActionHandler actionHandler = new ActionHandler();
		ButtonGroup grp = new ButtonGroup();

		separator = new JRadioButton(jEdit.getProperty("options.toolbar"
			+ ".add.separator"));
		separator.setSelected(true);
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
		centerPanel.add(BorderLayout.CENTER,new JScrollPane(list));

		JPanel iconPanel = new JPanel(new BorderLayout());
		JPanel labelPanel = new JPanel(new GridLayout(2,1));
		JPanel compPanel = new JPanel(new GridLayout(2,1));
		grp = new ButtonGroup();
		labelPanel.add(builtin = new JRadioButton(jEdit.getProperty(
			"options.toolbar.add.builtin")));
		builtin.addActionListener(actionHandler);
		builtin.setSelected(true);
		grp.add(builtin);
		labelPanel.add(file = new JRadioButton(jEdit.getProperty(
			"options.toolbar.add.file")));
		grp.add(file);
		file.addActionListener(actionHandler);
		iconPanel.add(BorderLayout.WEST,labelPanel);
		builtinCombo = new JComboBox(iconList);
		builtinCombo.setRenderer(new ToolBarOptionPane.IconCellRenderer());
		compPanel.add(builtinCombo);

		fileButton = new JButton(jEdit.getProperty("options.toolbar.add.no-icon"));
		fileButton.setMargin(new Insets(1,1,1,1));
		fileButton.setIcon(new ImageIcon(getClass().getResource(
			"/org/gjt/sp/jedit/toolbar/blank-20.gif")));
		fileButton.setHorizontalAlignment(SwingConstants.LEFT);
		fileButton.addActionListener(actionHandler);
		compPanel.add(fileButton);
		iconPanel.add(BorderLayout.CENTER,compPanel);
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

		updateEnabled();

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

		if(separator.isSelected())
			return new ToolBarOptionPane.Button("-",null,null,"-");
		else if(action.isSelected())
		{
			Icon icon;
			String iconName;
			if(builtin.isSelected())
			{
				ToolBarOptionPane.IconListEntry selectedIcon =
					(ToolBarOptionPane.IconListEntry)
					builtinCombo.getSelectedItem();
				icon = selectedIcon.icon;
				iconName = selectedIcon.name;
			}
			else
			{
				icon = fileButton.getIcon();
				iconName = fileIcon;
				if(iconName == null)
					iconName = "blank-20.gif";
			}

			ToolBarOptionPane.Button button = (ToolBarOptionPane.Button)
				list.getSelectedValue();
			return new ToolBarOptionPane.Button(button.actionName,
				iconName,icon,button.label);
		}
		else
			throw new InternalError();
	}

	// private members
	private boolean isOK;
	private JRadioButton separator, action;
	private JList list;
	private JRadioButton builtin;
	private JComboBox builtinCombo;
	private JRadioButton file;
	private JButton fileButton;
	private String fileIcon;
	private JButton ok, cancel;

	private void updateEnabled()
	{
		boolean enabled = action.isSelected();
		builtin.setEnabled(enabled);
		file.setEnabled(enabled);
		builtinCombo.setEnabled(enabled && builtin.isSelected());
		fileButton.setEnabled(enabled && file.isSelected());
		list.setEnabled(enabled);
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(source == separator)
				updateEnabled();
			else if(source == action)
				updateEnabled();
			else if(source == ok)
				ok();
			else if(source == cancel)
				cancel();
			else if(source == builtin || source == file)
				updateEnabled();
			else if(source == fileButton)
			{
				String directory;
				if(fileIcon == null)
					directory = null;
				else
					directory = MiscUtilities.getFileParent(fileIcon);
				String path = GUIUtilities.showFileDialog(null,directory,
					JFileChooser.OPEN_DIALOG);
				if(path != null)
				{
					fileIcon = "file://" + path.replace(
						File.separatorChar,'/');
					try
					{
						fileButton.setIcon(new ImageIcon(new URL(
							fileIcon)));
					}
					catch(MalformedURLException mf)
					{
						Log.log(Log.ERROR,this,mf);
					}
					fileButton.setText(MiscUtilities.getFileName(fileIcon));
				}
			}
		}
	}
}

/*
 * Change Log:
 * $Log$
 * Revision 1.4  2000/05/21 03:00:51  sp
 * Code cleanups and bug fixes
 *
 * Revision 1.3  2000/05/20 07:02:04  sp
 * Documentation updates, tool bar editor finished, a few other enhancements
 *
 * Revision 1.2  2000/05/16 10:47:40  sp
 * More work on toolbar editor, -gui command line switch
 *
 * Revision 1.1  2000/05/14 10:55:22  sp
 * Tool bar editor started, improved view registers dialog box
 *
 */
