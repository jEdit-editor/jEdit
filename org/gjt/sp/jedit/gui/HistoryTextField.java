/*
 * HistoryTextField.java - Text field with a combo box history
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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import org.gjt.sp.jedit.*;

/**
 * Text field with a combo box component listing previously entered
 * strings.
 * @author Slava Pestov
 * @version $Id$
 */
public class HistoryTextField extends JComboBox
{
	public HistoryTextField(String name)
	{
		this.name = name;

		try
		{
			max = Integer.parseInt(jEdit.getProperty("history"));
		}
		catch(NumberFormatException nf)
		{
			max = 25;
		}

		String line;
		int i = 0;
		while((line = jEdit.getProperty("history." + name + "." + i)) != null)
		{
			if(i >= max)
				break;
			if(line.length() != 0)
				addItem(line);
			i++;
		}

		setEditable(true);
		setMaximumRowCount(20);
		setSelectedItem(null);

		getEditor().getEditorComponent()
			.addKeyListener(new HistoryKeyListener());
	}

	public void save()
	{
		String text = (String)getEditor().getItem();
		if(text == null)
			text = (String)getSelectedItem();
		if(text == null || text.length() == 0)
			text = "";
		else
		{
			insertItemAt(text,0);
			if(getItemCount() > max)
				removeItemAt(getItemCount() - 1);
		}
		for(int i = 0; i < getItemCount(); i++)
		{
			jEdit.setProperty("history." + name + "." +
				i,(String)getItemAt(i));
		}
	}

	public void addCurrentToHistory()
	{
		String text = (String)getEditor().getItem();
		if(text == null)
			text = (String)getSelectedItem();
		if(text == null || text.length() == 0)
			return;
		insertItemAt(text,0);
		if(getItemCount() > max)
			removeItemAt(getItemCount() - 1);
	}

	public void actionPerformed(ActionEvent evt)
	{
		Object current = getEditor().getItem();
		setSelectedItem(current);

		// Don't fire actionEvent here
	}

	public void selectedItemChanged()
	{
		if(selectedItemReminder != null)
		{
			fireItemStateChanged(new ItemEvent(this,
				ItemEvent.ITEM_STATE_CHANGED,
				selectedItemReminder,ItemEvent.DESELECTED));
		}

		selectedItemReminder = getModel().getSelectedItem();

		if(selectedItemReminder != null)
		{
			fireItemStateChanged(new ItemEvent(this,
				ItemEvent.ITEM_STATE_CHANGED,
				selectedItemReminder,ItemEvent.SELECTED));
		}
		
		// Don't fire actionEvent here
	}

	/**
	 * Making it public so that an inner class can use it.
	 */
	public void fireActionEvent()
	{
		super.fireActionEvent();
	}
	
	// private members
	private String name;
	private int max;

	class HistoryKeyListener extends KeyAdapter
	{
		public void keyPressed(KeyEvent evt)
		{
			if(evt.getKeyCode() == KeyEvent.VK_ENTER)
			{
				Object current = getEditor().getItem();
				setSelectedItem(current);
				fireActionEvent();
			}
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.20  1999/03/28 01:36:24  sp
 * Backup system overhauled, HistoryTextField updates
 *
 * Revision 1.19  1999/03/27 03:22:16  sp
 * Number of items in a history list can now be set
 *
 * Revision 1.18  1999/03/27 03:08:55  sp
 * Changed max number of items in history to 25 from 100
 *
 * Revision 1.17  1999/03/21 08:37:16  sp
 * Slimmer action system, history text field update
 *
 * Revision 1.16  1999/03/20 01:55:42  sp
 * New color option pane, fixed search & replace bug
 *
 * Revision 1.15  1999/03/20 00:26:48  sp
 * Console fix, backed out new JOptionPane code, updated tips
 *
 * Revision 1.14  1999/03/19 21:50:16  sp
 * Made HistoryTextField compile
 *
 * Revision 1.13  1999/03/19 06:08:45  sp
 * Fixed conflicts from incomplete commit, removed obsolete files
 *
 */
