/*
 * append_string_register.java
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

package org.gjt.sp.jedit.actions;

import java.awt.event.ActionEvent;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.jedit.*;

public class append_string_register extends EditAction
implements InputHandler.NonRecordable, InputHandler.NonRepeatable
{
	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		JEditTextArea textArea = view.getTextArea();
		String selection = textArea.getSelectedText();

		if(selection == null)
			return;

		String actionCommand = evt.getActionCommand();
		if(actionCommand == null || actionCommand.length() != 1)
		{
			view.showStatus(jEdit.getProperty("view.status.append-string-register"));
			textArea.getInputHandler().grabNextKeyStroke(this);
		}
		else
		{
			view.showStatus(null);

			char ch = actionCommand.charAt(0);
			if(ch == '\0')
			{
				view.getToolkit().beep();
				return;
			}

			InputHandler inputHandler = textArea.getInputHandler();
			InputHandler.MacroRecorder recorder = inputHandler.getMacroRecorder();

			if(recorder != null)
				recorder.actionPerformed(this,actionCommand);

			int repeatCount = inputHandler.getRepeatCount();
			StringBuffer buf = new StringBuffer();
			for(int i = 0; i < repeatCount; i++)
				buf.append(selection);
			selection = buf.toString();

			Registers.Register register = Registers.getRegister(ch);

			if(register == null || register.toString() == null)
				Registers.setRegister(ch,new Registers.StringRegister(selection));
			else
			{
				Registers.setRegister(ch,new Registers.StringRegister(register.toString()
					+ selection));
			}
		}
	}
}
