/*
 * JCheckBoxList.java - A list, each item can be checked or unchecked
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

import javax.swing.table.*;
import javax.swing.*;
import java.util.Vector;

public class JCheckBoxList extends JTable
{
	public JCheckBoxList(Object[] items)
	{
		setModel(items);
		getSelectionModel().setSelectionMode(ListSelectionModel
			.SINGLE_SELECTION);
		setShowGrid(false);
	}

	public void setModel(Object[] items)
	{
		setModel(new CheckBoxListModel(items));
		init();
	}

	public void setModel(Vector items)
	{
		setModel(new CheckBoxListModel(items));
		init();
	}

	public Object[] getCheckedValues()
	{
		Vector values = new Vector();
		CheckBoxListModel model = (CheckBoxListModel)getModel();
		for(int i = 0; i < model.items.size(); i++)
		{
			CheckBoxListModel.Entry entry = (CheckBoxListModel.Entry)
				model.items.elementAt(i);
			if(entry.checked)
				values.addElement(entry.value);
		}

		Object[] retVal = new Object[values.size()];
		values.copyInto(retVal);
		return retVal;
	}

	public Object getSelectedValue()
	{
		int row = getSelectedRow();
		if(row == -1)
			return null;
		else
			return getModel().getValueAt(row,1);
	}

	// private members
	private void init()
	{
		setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		TableColumn column = getColumnModel().getColumn(0);
		int checkBoxWidth = new JCheckBox().getPreferredSize().width;
		column.setPreferredWidth(checkBoxWidth);
		column.setMinWidth(checkBoxWidth);
		column.setWidth(checkBoxWidth);
		column.setMaxWidth(checkBoxWidth);
		column.setResizable(false);
	}
}

class CheckBoxListModel extends AbstractTableModel
{
	Vector items;

	CheckBoxListModel(Object[] _items)
	{
		items = new Vector(_items.length);
		for(int i = 0; i < _items.length; i++)
		{
			items.addElement(new Entry(_items[i]));
		}
	}

	CheckBoxListModel(Vector _items)
	{
		items = new Vector(_items.size());
		for(int i = 0; i < _items.size(); i++)
		{
			items.addElement(new Entry(_items.elementAt(i)));
		}
	}

	public int getRowCount()
	{
		return items.size();
	}

	public int getColumnCount()
	{
		return 2;
	}

	public String getColumnName(int col)
	{
		return null;
	}

	public Object getValueAt(int row, int col)
	{
		Entry entry = (Entry)items.elementAt(row);
		switch(col)
		{
		case 0:
			return new Boolean(entry.checked);
		case 1:
			return entry.value;
		default:
			throw new InternalError();
		}
	}

	public Class getColumnClass(int col)
	{
		switch(col)
		{
		case 0:
			return Boolean.class;
		case 1:
			return String.class;
		default:
			throw new InternalError();
		}
	}

	public boolean isCellEditable(int row, int col)
	{
		return (col == 0 && !(((Entry)items.elementAt(row)).value
			instanceof String));
	}

	public void setValueAt(Object value, int row, int col)
	{
		if(col == 0)
		{
			((Entry)items.elementAt(row)).checked =
				(value.equals(Boolean.TRUE));
		}
	}

	class Entry
	{
		boolean checked;
		Object value;

		Entry(Object value)
		{
			this.value = value;
		}
	}
}
