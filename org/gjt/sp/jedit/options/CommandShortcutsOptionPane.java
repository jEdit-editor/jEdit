/*
 * CommandShortcutsOptionPane.java - Command shortcuts options panel
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
 * Option pane for editing command shortcuts.
 * @author Slava Pestov
 * @version $Id$
 */
public class CommandShortcutsOptionPane extends ShortcutsOptionPane
{
	public CommandShortcutsOptionPane()
	{
		super("command-keys");
	}

	// protected members
	protected void _save()
	{
		super._save();
		jEdit.reloadKeyBindings();
	}

	protected Vector createBindings()
	{
		EditAction[] actions = jEdit.getActions();

		Vector bindings = new Vector(actions.length);

		for(int i = 0; i < actions.length; i++)
		{
			EditAction action = actions[i];
			if(action.isPluginAction())
				continue;

			String name = action.getName();
			String label = jEdit.getProperty(name + ".label");
			// Skip certain actions this way (ENTER, TAB)
			if(label == null)
				continue;

			label = GUIUtilities.prettifyMenuLabel(label);
			String shortcut = jEdit.getProperty(name + ".shortcut");
			bindings.addElement(new KeyBinding(name,label,shortcut));
		}

		return bindings;
	}
}
