/*
 * play_macro.java
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

package org.gjt.sp.jedit.actions;

import java.awt.event.ActionEvent;
import org.gjt.sp.jedit.gui.InputHandler;
import org.gjt.sp.jedit.*;

public class play_macro extends EditAction
{
	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		String macroName = evt.getActionCommand();

		Macros.Macro macro = Macros.getMacro(macroName);
		if(macro == null)
		{
			String[] args = { macroName };
			GUIUtilities.error(view,"macro-not-found",args);
			return;
		}

		// This hackery is necessary to prevent actions inside the
		// macro from picking up the repeat count
		InputHandler inputHandler = view.getInputHandler();
		int repeatCount = inputHandler.getRepeatCount();
		inputHandler.setRepeatEnabled(false);
		inputHandler.setRepeatCount(1);

		for(int i = repeatCount - 1; i >= 0; i--)
		{
			Macros.playMacro(view,macro.path);
		}
	}

	public boolean isRepeatable()
	{
		return false;
	}

	public boolean isRecordable()
	{
		return false;
	}
}
