/*
 * EnhancedMenuItem.java - Menu item with user-specified accelerator string
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
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.EditAction;
import org.gjt.sp.jedit.GUIUtilities;

/**
 * jEdit's custom menu item. It adds support for multi-key shortcuts.
 */
public class EnhancedMenuItem extends JMenuItem
{
	/**
	 * Creates a new menu item. Most plugins should call
	 * GUIUtilities.loadMenuItem() instead.
	 * @param label The menu item label
	 * @param keyBinding The key binding
	 * @param action The edit action
	 * @param actionCommand The action command
	 */
	public EnhancedMenuItem(String label, String keyBinding,
		EditAction action, String actionCommand)
	{
		super(label);
		this.keyBinding = keyBinding;

		if(action != null)
		{
			setEnabled(true);
			addActionListener(action);
		}
		else
			setEnabled(false);

		setActionCommand(actionCommand);

		acceleratorFont = UIManager
			.getFont("MenuItem.acceleratorFont");
		acceleratorFont = new Font("Monospaced",
			acceleratorFont.getStyle(),
			acceleratorFont.getSize());
		acceleratorForeground = UIManager
			.getColor("MenuItem.acceleratorForeground");
		acceleratorSelectionForeground = UIManager
			.getColor("MenuItem.acceleratorSelectionForeground");
	}

	public Dimension getPreferredSize()
	{
		Dimension d = super.getPreferredSize();
		if(keyBinding != null)
		{
			d.width += (getToolkit().getFontMetrics(acceleratorFont)
				.stringWidth(keyBinding) + 30);
		}
		return d;
	}

	public void paint(Graphics g)
	{
		super.paint(g);
		if(keyBinding != null)
		{
			g.setFont(acceleratorFont);
			g.setColor(getModel().isArmed() ?
				acceleratorSelectionForeground :
				acceleratorForeground);
			FontMetrics fm = g.getFontMetrics();
			Insets insets = getInsets();
			g.drawString(keyBinding,getWidth() - (fm.stringWidth(
				keyBinding) + insets.right + insets.left),
				getFont().getSize() + (insets.top - 1)
				/* XXX magic number */);
		}
	}

	/**
	 * We override this so that null action commands are supported.
	 */
	public String getActionCommand()
	{
		return getModel().getActionCommand();
	}

	// private members
	private String keyBinding;
	private Font acceleratorFont;
	private Color acceleratorForeground;
	private Color acceleratorSelectionForeground;
}
