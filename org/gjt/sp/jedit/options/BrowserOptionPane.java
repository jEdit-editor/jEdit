/*
 * BrowserOptionPane.java - Browser options panel
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

import javax.swing.table.*;
import javax.swing.*;
import java.awt.*;
import java.util.Vector;
import org.gjt.sp.jedit.*;

public class BrowserOptionPane extends AbstractOptionPane
{
	public BrowserOptionPane()
	{
		super("browser");
	}

	public void _init()
	{
		/* Default directory */
		String[] dirs = {
			jEdit.getProperty("options.browser.defaultPath.buffer"),
			jEdit.getProperty("options.browser.defaultPath.home"),
			jEdit.getProperty("options.browser.defaultPath.last")
		};

		defaultDirectory = new JComboBox(dirs);
		String defaultDir = jEdit.getProperty("vfs.browser.defaultPath");
		if("buffer".equals(defaultDir))
			defaultDirectory.setSelectedIndex(0);
		else if("home".equals(defaultDir))
			defaultDirectory.setSelectedIndex(1);
		else if("last".equals(defaultDir))
			defaultDirectory.setSelectedIndex(2);
		addComponent(jEdit.getProperty("options.browser.defaultPath"),
			defaultDirectory);

		/* Default view */
		String[] views = {
			jEdit.getProperty("options.browser.defaultView.list"),
			jEdit.getProperty("options.browser.defaultView.tree")
		};

		defaultView = new JComboBox(views);
		String _defaultView = jEdit.getProperty("vfs.browser.defaultView");
		if("list".equals(_defaultView))
			defaultView.setSelectedIndex(0);
		else if("tree".equals(_defaultView))
			defaultView.setSelectedIndex(1);
		addComponent(jEdit.getProperty("options.browser.defaultView"),
			defaultView);

		/* Show hidden files */
		showHiddenFiles = new JCheckBox(jEdit.getProperty("options.browser"
			+ ".showHiddenFiles"));
		showHiddenFiles.setSelected(jEdit.getBooleanProperty("vfs.browser"
			+ ".showHiddenFiles"));
		addComponent(showHiddenFiles);

		/* Sort file list */
		sortFiles = new JCheckBox(jEdit.getProperty("options.browser"
			+ ".sortFiles"));
		sortFiles.setSelected(jEdit.getBooleanProperty("vfs.browser"
			+ ".sortFiles"));
		addComponent(sortFiles);

		/* Ignore case when sorting */
		sortIgnoreCase = new JCheckBox(jEdit.getProperty("options.browser"
			+ ".sortIgnoreCase"));
		sortIgnoreCase.setSelected(jEdit.getBooleanProperty("vfs.browser"
			+ ".sortIgnoreCase"));
		addComponent(sortIgnoreCase);

		/* Mix files and directories */
		sortMixFilesAndDirs = new JCheckBox(jEdit.getProperty("options.browser"
			+ ".sortMixFilesAndDirs"));
		sortMixFilesAndDirs.setSelected(jEdit.getBooleanProperty("vfs.browser"
			+ ".sortMixFilesAndDirs"));
		addComponent(sortMixFilesAndDirs);

		addSeparator("options.browser.filters");

		GridBagConstraints cons = new GridBagConstraints();
		cons.gridy = 8;
		cons.gridwidth = cons.gridheight = GridBagConstraints.REMAINDER;
		cons.fill = GridBagConstraints.BOTH;
		cons.weightx = cons.weighty = 1.0f;

		JScrollPane filterScroller = createFileFilterScroller();
		gridBag.setConstraints(filterScroller,cons);
		add(filterScroller);
	}

	public void _save()
	{
		String[] dirs = { "buffer", "home", "last" };
		jEdit.setProperty("vfs.browser.defaultPath",dirs[defaultDirectory
			.getSelectedIndex()]);
		String[] views = { "list", "tree" };
		jEdit.setProperty("vfs.browser.defaultView",views[defaultView
			.getSelectedIndex()]);
		jEdit.setBooleanProperty("vfs.browser.showHiddenFiles",
			showHiddenFiles.isSelected());
		jEdit.setBooleanProperty("vfs.browser.sortFiles",
			sortFiles.isSelected());
		jEdit.setBooleanProperty("vfs.browser.sortIgnoreCase",
			sortIgnoreCase.isSelected());
		jEdit.setBooleanProperty("vfs.browser.sortMixFilesAndDirs",
			sortMixFilesAndDirs.isSelected());

		filterModel.save();
	}

	// private members
	private JComboBox defaultDirectory;
	private JComboBox defaultView;
	private JCheckBox showHiddenFiles;
	private JCheckBox sortFiles;
	private JCheckBox sortIgnoreCase;
	private JCheckBox sortMixFilesAndDirs;

	private JTable filterTable;
	private FileFilterTableModel filterModel;

	private JScrollPane createFileFilterScroller()
	{
		filterModel = new FileFilterTableModel();
		filterTable = new JTable(filterModel);
		filterTable.getTableHeader().setReorderingAllowed(false);
		Dimension d = filterTable.getPreferredSize();
		d.height = Math.min(d.height,200);
		JScrollPane scroller = new JScrollPane(filterTable);
		scroller.setPreferredSize(d);
		return scroller;
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

		// better names for these properties would be:
		// vfs.browser.filter.X.name
		// vfs.browser.filter.X.glob
		// but I still use the old names for historical reasons/
		// backwards compatibility
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
			return filter.glob;
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
			String glob = (col == 1 ? (String)value : null);
			filters.addElement(new FilterEntry(name,glob));

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
				filter.glob = (String)value;
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
			return jEdit.getProperty("options.browser.filters.name");
		case 1:
			return jEdit.getProperty("options.browser.filters.glob");
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
				|| filter.glob == null || filter.glob.length() == 0)
				continue;

			jEdit.setProperty("filefilter." + j + ".name",filter.name);
			jEdit.setProperty("filefilter." + j + ".re",filter.glob);
			j++;
		}

		jEdit.unsetProperty("filefilter." + j + ".name");
		jEdit.unsetProperty("filefilter." + j + ".re");
	}

	class FilterEntry
	{
		FilterEntry(String name, String glob)
		{
			this.name = name;
			this.glob = glob;
		}

		String name;
		String glob;
	}
}

/*
 * Change Log:
 * $Log$
 * Revision 1.3  2000/08/20 07:29:31  sp
 * I/O and VFS browser improvements
 *
 * Revision 1.2  2000/08/19 08:26:27  sp
 * More docking API tweaks
 *
 * Revision 1.1  2000/08/11 09:06:52  sp
 * Browser option pane
 *
 */
