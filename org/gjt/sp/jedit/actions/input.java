/*
 * input.java
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

package org.gjt.sp.jedit.actions;

import java.awt.event.ActionEvent;
import javax.swing.JOptionPane;
import org.gjt.sp.jedit.gui.MacroInputDialog;
import org.gjt.sp.jedit.gui.InputHandler;
import org.gjt.sp.jedit.*;

public class input extends EditAction
{
	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		String actionCommand = evt.getActionCommand();

		if(actionCommand == null)
		{
			InputHandler.MacroRecorder recorder = view.getInputHandler()
				.getMacroRecorder();
			if(recorder == null)
			{
				GUIUtilities.error(view,"macro-input.no-record",null);
				return;
			}

			MacroInputDialog dialog = new MacroInputDialog(view);
			if(dialog.isOK())
			{
				String prompt = dialog.getPrompt();
				String register = dialog.getRegister();
				recorder.actionPerformed(this,register + '@' + prompt);
			}
		}
		else
		{
			char register;
			int index = actionCommand.indexOf('@');
			if(index != 1)
				register = '$';
			else
			{
				register = actionCommand.charAt(0);
				actionCommand = actionCommand.substring(2);
			}

			String retVal = (String)JOptionPane.showInputDialog(view,
				actionCommand,jEdit.getProperty("macro-input.title"),
				JOptionPane.QUESTION_MESSAGE);
			if(retVal == null)
				Registers.clearRegister(register);
			else
			{
				Registers.setRegister(register,new
					Registers.StringRegister(retVal));
			}
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
