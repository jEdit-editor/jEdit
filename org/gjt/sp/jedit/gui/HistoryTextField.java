/*
 * HistoryTextField.java - Text field with a history
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

/**
 * Text field with an arrow-key accessable history.
 * @author Slava Pestov
 * @version $Id$
 */
public class HistoryTextField extends JTextField
{
	public HistoryTextField(String name)
	{
		this(name,false);
	}

	public HistoryTextField(String name, boolean instantPopups)
	{
		historyModel = HistoryModel.getModel(name);
		addKeyListener(new KeyHandler());
		addMouseListener(new MouseHandler());

		this.instantPopups = instantPopups;

		index = -1;
	}

	public void addCurrentToHistory()
	{
		historyModel.addItem(getText());
		index = 0;
	}

	public void setText(String text)
	{
		super.setText(text);
		index = -1;
	}

	public HistoryModel getModel()
	{
		return historyModel;
	}

	public void fireActionPerformed()
	{
		super.fireActionPerformed();
	}

	// private members
	private HistoryModel historyModel;
	private boolean instantPopups;
	private String current;
	private int index;

	private void doBackwardSearch()
	{
		if(getSelectionEnd() != getDocument().getLength())
		{
			setCaretPosition(getDocument().getLength());
		}

		String text = getText().substring(0,getSelectionStart());
		if(text == null)
		{
			historyPrevious();
			return;
		}

		for(int i = index + 1; i < historyModel.getSize(); i++)
		{
			String item = historyModel.getItem(i);
			if(item.startsWith(text))
			{
				replaceSelection(item.substring(text.length()));
				select(text.length(),getDocument().getLength());
				index = i;
				return;
			}
		}

		getToolkit().beep();
	}

	private void doForwardSearch()
	{
		if(getSelectionEnd() != getDocument().getLength())
		{
			setCaretPosition(getDocument().getLength());
		}

		String text = getText().substring(0,getSelectionStart());
		if(text == null)
		{
			historyNext();
			return;
		}

		for(int i = index - 1; i >= 0; i--)
		{
			String item = historyModel.getItem(i);
			if(item.startsWith(text))
			{
				replaceSelection(item.substring(text.length()));
				select(text.length(),getDocument().getLength());
				index = i;
				return;
			}
		}

		getToolkit().beep();
	}

	private void historyPrevious()
	{
		if(index == historyModel.getSize() - 1)
			getToolkit().beep();
		else if(index == -1)
		{
			current = getText();
			setText(historyModel.getItem(0));
			index = 0;
		}
		else
		{
			// have to do this because setText() sets index to -1
			int newIndex = index + 1;
			setText(historyModel.getItem(newIndex));
			index = newIndex;
		}
	}

	private void historyNext()
	{
		if(index == -1)
			getToolkit().beep();
		else if(index == 0)
			setText(current);
		else
		{
			// have to do this because setText() sets index to -1
			int newIndex = index - 1;
			setText(historyModel.getItem(newIndex));
			index = newIndex;
		}
	}

	private void showFullMenu(int x, int y)
	{
		JPopupMenu popup = new JPopupMenu();
		JMenuItem caption = new JMenuItem(historyModel.getName());
		caption.getModel().setEnabled(false);
		popup.add(caption);
		popup.addSeparator();

		ButtonGroup grp = new ButtonGroup();

		ActionHandler actionListener = new ActionHandler();

		if(index == -1)
			current = getText();

		if(current != null && current.length() != 0)
		{
			JRadioButtonMenuItem menuItem =
				new JRadioButtonMenuItem(current);
			menuItem.setActionCommand("-1");
			menuItem.addActionListener(actionListener);
			grp.add(menuItem);
			popup.add(menuItem);
			menuItem.getModel().setSelected(true);
		}

		for(int i = 0; i < historyModel.getSize(); i++)
		{
			JRadioButtonMenuItem menuItem =
				new JRadioButtonMenuItem(historyModel.getItem(i));
			menuItem.setActionCommand(String.valueOf(i));
			menuItem.addActionListener(actionListener);
			grp.add(menuItem);
			popup.add(menuItem);
			if(i == index)
				menuItem.getModel().setSelected(true);
		}

		popup.show(this,x,y);
	}

	private void showPartialMenu(int x, int y)
	{
		String text = getText().substring(0,getSelectionStart());
		if(text == null || text.length() == 0)
		{
			showFullMenu(x,y);
			return;
		}

		ActionHandler actionListener = new ActionHandler();

		JPopupMenu popup = new JPopupMenu();
		JMenuItem caption = new JMenuItem(historyModel.getName()
			+ "/" + text);
		caption.getModel().setEnabled(false);
		popup.add(caption);
		popup.addSeparator();

		for(int i = 0; i < historyModel.getSize(); i++)
		{
			String item = historyModel.getItem(i);
			if(item.startsWith(text))
			{
				JMenuItem menuItem = new JMenuItem(item);
				menuItem.setActionCommand(String.valueOf(i));
				menuItem.addActionListener(actionListener);
				popup.add(menuItem);
			}
		}

		popup.show(this,x,y);
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			int ind = Integer.parseInt(evt.getActionCommand());
			if(ind == -1)
			{
				if(index != -1)
					setText(current);
			}
			else
			{
				setText(historyModel.getItem(ind));
				index = ind;
			}
			if(instantPopups)
			{
				addCurrentToHistory();
				fireActionPerformed();
			}
		}
	}

	class KeyHandler extends KeyAdapter
	{
		public void keyPressed(KeyEvent evt)
		{
			if(evt.getKeyCode() == KeyEvent.VK_ENTER)
			{
				addCurrentToHistory();
			}
			else if(evt.getKeyCode() == KeyEvent.VK_UP)
			{
				if((evt.getModifiers() & InputEvent.CTRL_MASK) != 0)
					doBackwardSearch();
				else
					historyPrevious();
				evt.consume();
			}
			else if(evt.getKeyCode() == KeyEvent.VK_DOWN)
			{
				if((evt.getModifiers() & InputEvent.CTRL_MASK) != 0)
					doForwardSearch();
				else
					historyNext();
				evt.consume();
			}
			else if(evt.getKeyCode() == KeyEvent.VK_TAB
				&& (evt.getModifiers() & InputEvent.CTRL_MASK) != 0)
			{
				doBackwardSearch();
				evt.consume();
			}
		}
	}

	class MouseHandler extends MouseAdapter
	{
		public void mousePressed(MouseEvent evt)
		{
			if((evt.getModifiers() & InputEvent.CTRL_MASK) != 0)
				showPartialMenu(0,getHeight());
			else if((evt.getModifiers() & InputEvent.BUTTON3_MASK) != 0)
				showFullMenu(0,getHeight());
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.30  1999/11/16 08:21:20  sp
 * Various fixes, attempt at beefing up expand-abbrev
 *
 * Revision 1.29  1999/09/30 12:21:04  sp
 * No net access for a month... so here's one big jEdit 2.1pre1
 *
 * Revision 1.28  1999/05/09 04:48:47  sp
 * Funky history menus
 *
 * Revision 1.27  1999/05/09 03:50:17  sp
 * HistoryTextField is now a text field again
 *
 * Revision 1.26  1999/05/08 00:13:00  sp
 * Splash screen change, minor documentation update, toolbar API fix
 *
 * Revision 1.25  1999/05/05 07:20:45  sp
 * jEdit 1.6pre5
 *
 * Revision 1.24  1999/05/04 04:51:25  sp
 * Fixed HistoryTextField for Swing 1.1.1
 *
 * Revision 1.23  1999/04/25 03:39:37  sp
 * Documentation updates, console updates, history text field updates
 *
 * Revision 1.22  1999/04/23 07:35:11  sp
 * History engine reworking (shared history models, history saved to
 * .jedit-history)
 *
 * Revision 1.21  1999/04/19 05:44:34  sp
 * GUI updates
 *
 * Revision 1.20  1999/03/28 01:36:24  sp
 * Backup system overhauled, HistoryTextField updates
 *
 */
