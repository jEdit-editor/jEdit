/*
 * FileFilterOptionPane.java - Option pane for changing file filters
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

package org.gjt.sp.jedit.options;

import javax.swing.table.*;
import javax.swing.*;
import java.awt.*;
import java.util.Vector;
import org.gjt.sp.jedit.*;

/**
 * File filter editor option pane.
 * @author Slava Pestov
 * @version $Id$
 */
public class FileFilterOptionPane extends AbstractOptionPane
{
	public FileFilterOptionPane()
	{
		super("filters");
	}

	// protected members
	protected void _init()
	{
		setLayout(new BorderLayout());
		add(BorderLayout.CENTER,createFileFilterScroller());
	}

	protected void _save()
	{
		filterModel.save();
	}

	// private members
	private JTable filterTable;
	private FileFilterTableModel filterModel;

	private JScrollPane createFileFilterScroller()
	{
		filterModel = createFileFilterModel();
		filterTable = new JTable(filterModel);
		filterTable.getTableHeader().setReorderingAllowed(false);
		Dimension d = filterTable.getPreferredSize();
		d.height = Math.min(d.height,200);
		JScrollPane scroller = new JScrollPane(filterTable);
		scroller.setPreferredSize(d);
		return scroller;
	}

	private FileFilterTableModel createFileFilterModel()
	{
		return new FileFilterTableModel();
	}
}

class FileFilterTableModel extends AbstractTableModel
{
	private Vector filters;

	FileFilterTableModel()
	{
		filters = new Vector();

		String name;
		int i = 0;
		while((name = jEdit.getProperty("filefilter." + i + ".name"))
			!= null && name.length() != 0)
		{
			filters.addElement(new FilterEntry(name,
				jEdit.getProperty("filefilter." + i + ".re")));
			i++;
		}
	}

	public int getColumnCount()
	{
		return 2;
	}

	public int getRowCount()
	{
		return filters.size() + 1;
	}

	public Object getValueAt(int row, int col)
	{
		if(row == filters.size())
			return "";

		FilterEntry filter = (FilterEntry)filters.elementAt(row);
		switch(col)
		{
		case 0:
			return filter.name;
		case 1:
			return filter.re;
		default:
			return null;
		}
	}

	public boolean isCellEditable(int row, int col)
	{
		return true;
	}

	public void setValueAt(Object value, int row, int col)
	{
		// When we change the last row, another one is added...
		if(row == filters.size())
		{
			String name = (col == 0 ? (String)value : null);
			String re = (col == 1 ? (String)value : null);
			filters.addElement(new FilterEntry(name,re));

			fireTableRowsUpdated(row,row + 1);
		}
		else
		{
			FilterEntry filter = (FilterEntry)filters.elementAt(row);
			switch(col)
			{
			case 0:
				filter.name = (String)value;
				break;
			case 1:
				filter.re = (String)value;
				break;
			}

			fireTableRowsUpdated(row,row);
		}
	}

	public String getColumnName(int index)
	{
		switch(index)
		{
		case 0:
			return jEdit.getProperty("options.filters.name");
		case 1:
			return jEdit.getProperty("options.filters.re");
		default:
			return null;
		}
	}

	public void save()
	{
		// keep a seprate count for the filter entry property names,
		// because some filters might be blank
		int j = 0;

		for(int i = 0; i < filters.size(); i++)
		{
			FilterEntry filter = (FilterEntry)filters.elementAt(i);
			if(filter.name == null || filter.name.length() == 0
				|| filter.re == null || filter.re.length() == 0)
				continue;

			jEdit.setProperty("filefilter." + j + ".name",filter.name);
			jEdit.setProperty("filefilter." + j + ".re",filter.re);
			j++;
		}

		jEdit.unsetProperty("filefilter." + j + ".name");
		jEdit.unsetProperty("filefilter." + j + ".re");
	}

	class FilterEntry
	{
		FilterEntry(String name, String re)
		{
			this.name = name;
			this.re = re;
		}

		String name;
		String re;
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.4  2000/04/16 08:56:24  sp
 * Option pane updates
 *
 * Revision 1.3  1999/10/05 10:55:29  sp
 * File dialogs open faster, and experimental keyboard macros
 *
 * Revision 1.2  1999/10/04 03:20:51  sp
 * Option pane change, minor tweaks and bug fixes
 *
 * Revision 1.1  1999/10/03 04:13:26  sp
 * Forgot to add some files
 *
 */
