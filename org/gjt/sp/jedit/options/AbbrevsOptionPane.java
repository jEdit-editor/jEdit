/*
 * AbbrevsOptionPane.java - Abbrevs options panel
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
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import org.gjt.sp.jedit.*;

/**
 * Abbrev editor.
 * @author Slava Pestov
 * @version $Id$
 */
public class AbbrevsOptionPane extends AbstractOptionPane
{
	public AbbrevsOptionPane()
	{
		super("abbrevs");
		setLayout(new BorderLayout());

		JPanel panel = new JPanel();

		expandOnInput = new JCheckBox(jEdit.getProperty("options.abbrevs"
			+ ".expandOnInput"),Abbrevs.getExpandOnInput());
		panel.add(expandOnInput);

		panel.add(new JLabel(jEdit.getProperty("options.abbrevs.set")));

		Hashtable _modeAbbrevs = Abbrevs.getModeAbbrevs();
		modeAbbrevs = new Hashtable();
		Mode[] modes = jEdit.getModes();
		String[] sets = new String[modes.length + 1];
		sets[0] = "global";
		for(int i = 0; i < modes.length; i++)
		{
			String name = modes[i].getName();
			sets[i+1] = name;
			modeAbbrevs.put(name,new AbbrevsModel((Hashtable)_modeAbbrevs.get(name)));
		}
		setsComboBox = new JComboBox(sets);
		setsComboBox.addActionListener(new ActionHandler());
		panel.add(setsComboBox);

		add(BorderLayout.NORTH,panel);

		add(BorderLayout.CENTER,createAbbrevsScroller());

		panel = new JPanel();
		abbrev = new JButton(jEdit.getProperty("options.abbrevs.sort.abbrev"));
		abbrev.addActionListener(new ActionHandler());
		panel.add(abbrev);
		expand = new JButton(jEdit.getProperty("options.abbrevs.sort.expand"));
		expand.addActionListener(new ActionHandler());
		panel.add(expand);
		add(BorderLayout.SOUTH,panel);
	}

	public void save()
	{
		Abbrevs.setExpandOnInput(expandOnInput.getModel().isSelected());
		//abbrevsModel.save();
	}

	// private members
	private JComboBox setsComboBox;
	private JCheckBox expandOnInput;
	private JTable abbrevsTable;
	private JButton abbrev;
	private JButton expand;
	private AbbrevsModel globalAbbrevs;
	private Hashtable modeAbbrevs;

	private JScrollPane createAbbrevsScroller()
	{
		globalAbbrevs = new AbbrevsModel(Abbrevs.getGlobalAbbrevs());
		abbrevsTable = new JTable(globalAbbrevs);
		abbrevsTable.getTableHeader().setReorderingAllowed(false);
		Dimension d = abbrevsTable.getPreferredSize();
		d.height = Math.min(d.height,200);
		JScrollPane scroller = new JScrollPane(abbrevsTable);
		scroller.setPreferredSize(d);
		return scroller;
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(source == setsComboBox)
			{
				String selected = (String)setsComboBox.getSelectedItem();
				if(selected.equals("global"))
				{
					abbrevsTable.setModel(globalAbbrevs);
				}
				else
				{
					abbrevsTable.setModel((AbbrevsModel)
						modeAbbrevs.get(selected));
				}
			}
			else
			{
				((AbbrevsModel)abbrevsTable.getModel())
					.sort(source == abbrev ? 0 : 1);
			}
		}
	}
}

class AbbrevsModel extends AbstractTableModel
{
	Vector abbrevs;

	AbbrevsModel()
	{
		abbrevs = new Vector();
	}

	AbbrevsModel(Hashtable abbrevHash)
	{
		this();

		if(abbrevHash != null)
		{
			Enumeration abbrevEnum = abbrevHash.keys();
			Enumeration expandEnum = abbrevHash.elements();

			while(abbrevEnum.hasMoreElements())
			{
				abbrevs.addElement(new Abbrev((String)abbrevEnum.nextElement(),
					(String)expandEnum.nextElement()));
			}

			sort(0);
		}
	}

	public void sort(int col)
	{
		MiscUtilities.quicksort(abbrevs,new AbbrevCompare(col));
		fireTableDataChanged();
	}

	public int getColumnCount()
	{
		return 2;
	}

	public int getRowCount()
	{
		return abbrevs.size() + 1;
	}

	public Object getValueAt(int row, int col)
	{
		if(row == abbrevs.size())
			return null;

		Abbrev abbrev = (Abbrev)abbrevs.elementAt(row);
		switch(col)
		{
		case 0:
			return abbrev.abbrev;
		case 1:
			return abbrev.expand;
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
		if(value == null)
			value = "";

		Abbrev abbrev;
		if(row == abbrevs.size())
		{
			abbrev = new Abbrev();
			abbrevs.addElement(abbrev);
		}
		else
			abbrev = (Abbrev)abbrevs.elementAt(row);

		if(col == 0)
			abbrev.abbrev = (String)value;
		else
			abbrev.expand = (String)value;

		fireTableRowsUpdated(row,row + 1);
	}

	public String getColumnName(int index)
	{
		switch(index)
		{
		case 0:
			return jEdit.getProperty("options.abbrevs.abbrev");
		case 1:
			return jEdit.getProperty("options.abbrevs.expand");
		default:
			return null;
		}
	}

	/* public void save()
	{
		for(int i = 0; i < bindings.size(); i++)
		{
			KeyBinding binding = (KeyBinding)bindings.elementAt(i);
			jEdit.setProperty(binding.name + ".shortcut",binding.shortcut);
		}
	} */

	class AbbrevCompare implements MiscUtilities.Compare
	{
		int col;

		AbbrevCompare(int col)
		{
			this.col = col;
		}

		public int compare(Object obj1, Object obj2)
		{
			Abbrev a1 = (Abbrev)obj1;
			Abbrev a2 = (Abbrev)obj2;

			if(col == 0)
			{
				String abbrev1 = a1.abbrev;
				String abbrev2 = a2.abbrev;

				return abbrev1.compareTo(abbrev2);
			}
			else
			{
				String expand1 = a1.expand;
				String expand2 = a2.expand;

				return expand1.compareTo(expand2);
			}
		}
	}
}

class Abbrev
{
	Abbrev() {}

	Abbrev(String abbrev, String expand)
	{
		this.abbrev = abbrev;
		this.expand = expand;
	}

	String abbrev;
	String expand;
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.1  1999/12/20 08:38:43  sp
 * Abbrevs option pane
 *
 */
