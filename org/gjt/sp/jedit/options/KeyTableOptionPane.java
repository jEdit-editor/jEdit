/*
 * KeyTableOptionPane.java - Key table options panel
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
import java.util.*;
import org.gjt.sp.jedit.textarea.InputHandler;
import org.gjt.sp.jedit.*;

/**
 * Key binding editor option pane.
 * @author Slava Pestov
 * @version $Id$
 */
public class KeyTableOptionPane extends AbstractOptionPane
{
	public KeyTableOptionPane()
	{
		super("keys");
		setLayout(new BorderLayout());
		add(BorderLayout.NORTH,new JLabel(jEdit.getProperty("options"
			+ ".keys.note")));
		add(BorderLayout.CENTER,createKeyTableScroller());
	}

	public void save()
	{
		keyModel.save();
	}

	// private members
	private JTable keyTable;
	private KeyTableModel keyModel;

	private JScrollPane createKeyTableScroller()
	{
		keyModel = createKeyTableModel();
		keyTable = new JTable(keyModel);
		keyTable.getTableHeader().setReorderingAllowed(false);
		Dimension d = keyTable.getPreferredSize();
		d.height = Math.min(d.height,200);
		JScrollPane scroller = new JScrollPane(keyTable);
		scroller.setPreferredSize(d);
		return scroller;
	}

	private KeyTableModel createKeyTableModel()
	{
		return new KeyTableModel();
	}
}

class KeyTableModel extends AbstractTableModel
{
	private Vector bindings;

	KeyTableModel()
	{
		Enumeration textActions = InputHandler.getActions();
		EditAction[] actions = jEdit.getActions();

		bindings = new Vector(actions.length + 30);

		// Add text area key bindings
		while(textActions.hasMoreElements())
		{
			String name = (String)textActions.nextElement();
			String label = jEdit.getProperty(name + ".label");
			// Skip certain actions this way (ENTER, TAB)
			if(label == null)
				continue;
			String shortcut = jEdit.getProperty(name + ".shortcut");
			bindings.addElement(new KeyBinding(name,label,shortcut));
		}

		// Add menu item key bindings
		for(int i = 0; i < actions.length; i++)
		{
			String name = actions[i].getName();
			String label = jEdit.getProperty(name + ".label");
			// Skip certain actions this way (ENTER, TAB)
			if(label == null)
				continue;
			label = GUIUtilities.prettifyMenuLabel(label);
			String shortcut = jEdit.getProperty(name + ".shortcut");
			bindings.addElement(new KeyBinding(name,label,shortcut));
		}

		// Add macros
		addMacroBindings(Macros.getMacros());

		// Sort
		MiscUtilities.quicksort(bindings,new KeyCompare());
	}

	public int getColumnCount()
	{
		return 2;
	}

	public int getRowCount()
	{
		return bindings.size();
	}

	public Object getValueAt(int row, int col)
	{
		KeyBinding binding = (KeyBinding)bindings.elementAt(row);
		switch(col)
		{
		case 0:
			return binding.label;
		case 1:
			return binding.shortcut;
		default:
			return null;
		}
	}

	public boolean isCellEditable(int row, int col)
	{
		return (col == 1);
	}

	public void setValueAt(Object value, int row, int col)
	{
		if(col != 1)
			return;
		((KeyBinding)bindings.elementAt(row)).shortcut = (String)value;
		fireTableRowsUpdated(row,row);
	}

	public String getColumnName(int index)
	{
		switch(index)
		{
		case 0:
			return jEdit.getProperty("options.keys.name");
		case 1:
			return jEdit.getProperty("options.keys.binding");
		default:
			return null;
		}
	}

	public void save()
	{
		for(int i = 0; i < bindings.size(); i++)
		{
			KeyBinding binding = (KeyBinding)bindings.elementAt(i);
			jEdit.setProperty(binding.name + ".shortcut",binding.shortcut);
		}
	}

	// private members
	private void addMacroBindings(Vector macros)
	{
		for(int i = 0; i < macros.size(); i++)
		{
			Object obj = macros.elementAt(i);
			if(obj instanceof Macros.Macro)
			{
				Macros.Macro macro = (Macros.Macro)obj;
				bindings.addElement(new KeyBinding(macro.name,
					"Macro: " + macro.name,
					jEdit.getProperty(macro.name + ".shortcut")));
			}
			else if(obj instanceof Vector)
			{
				addMacroBindings((Vector)obj);
			}
		}
	}
		
	class KeyCompare implements MiscUtilities.Compare
	{
		public int compare(Object obj1, Object obj2)
		{
			return ((KeyBinding)obj1).label.compareTo(
				((KeyBinding)obj2).label);
		}
	}

	class KeyBinding
	{
		KeyBinding(String name, String label, String shortcut)
		{
			this.name = name;
			this.label = label;
			this.shortcut = shortcut;
		}

		String name;
		String label;
		String shortcut;
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.8  1999/11/10 10:43:01  sp
 * Macros can now have shortcuts, various miscallaneous updates
 *
 * Revision 1.7  1999/11/09 10:14:34  sp
 * Macro code cleanups, menu item and tool bar clicks are recorded now, delete
 * word commands, check box menu item support
 *
 * Revision 1.6  1999/10/11 07:14:22  sp
 * doneWithBuffer()
 *
 * Revision 1.5  1999/10/04 03:20:51  sp
 * Option pane change, minor tweaks and bug fixes
 *
 * Revision 1.4  1999/09/30 12:21:04  sp
 * No net access for a month... so here's one big jEdit 2.1pre1
 *
 * Revision 1.3  1999/07/16 23:45:49  sp
 * 1.7pre6 BugFree version
 *
 * Revision 1.2  1999/05/04 07:45:22  sp
 * Event mutlicaster is now re-entrant, key binding editor updates
 *
 * Revision 1.1  1999/05/03 08:28:14  sp
 * Documentation updates, key binding editor, syntax text area bug fix
 *
 */
