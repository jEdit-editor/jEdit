/*
 * EnhancedButton.java - Check box button
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
 * A button that becomes selected when EditAction.isSelected() returns true.
 */
public class EnhancedButton extends JToggleButton
{
	public EnhancedButton(Icon icon, String toolTip, EditAction action,
		String actionCommand)
	{
		super(icon);

		this.action = action;

		if(action != null)
		{
			setEnabled(true);
			addActionListener(action);
		}
		else
			setEnabled(false);

		setActionCommand(actionCommand);

		setToolTipText(toolTip);
		setModel(new Model());

		Insets zeroInsets = new Insets(0,0,0,0);
		setMargin(zeroInsets);
		setRequestFocusEnabled(false);
	}

	public String getActionCommand()
	{
		return getModel().getActionCommand();
	}

	// private members
	private EditAction action;

	class Model extends JToggleButton.ToggleButtonModel
	{
		public boolean isSelected()
		{
			return action.isSelected(EnhancedButton.this);
		}
	}
}
