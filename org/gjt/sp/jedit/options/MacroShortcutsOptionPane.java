/*
 * MacroShortcutsOptionPane.java - Macro shortcuts options panel
 * Copyright (C) 1999, 2000 Slava Pestov
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
 * Option pane for editing macro key bindings.
 * @author Slava Pestov
 * @version $Id$
 */
public class MacroShortcutsOptionPane extends ShortcutsOptionPane
{
	public MacroShortcutsOptionPane()
	{
		super("macro-keys");
	}

	// protected members
	protected void _save()
	{
		super._save();
		Macros.loadMacros();
	}

	protected Vector createBindings()
	{
		Vector bindings = new Vector();
		Vector macroList = Macros.getMacroList();

		for(int i = 0; i < macroList.size(); i++)
		{
			String name = macroList.elementAt(i).toString();
			bindings.addElement(new KeyBinding(name,name,
				jEdit.getProperty(name + ".shortcut"),
				jEdit.getProperty(name + ".shortcut2")));
		}

		return bindings;
	}
}
