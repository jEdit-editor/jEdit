/*
 * insert_literal.java - Action
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
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class insert_literal extends EditAction
{
	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		Buffer buffer = view.getBuffer();
		JEditTextArea textArea = view.getTextArea();
		if(!textArea.isEditable())
		{
			view.getToolkit().beep();
			return;
		}

		String str = evt.getActionCommand();

		if(str == null)
		{
			view.pushStatus(jEdit.getProperty("view.status.insert-literal"));
			view.getInputHandler().grabNextKeyStroke(this);
			return;
		}
		else
		{
			view.popStatus();

			if(!str.equals("\0"))
			{
				int repeatCount = view.getInputHandler().getRepeatCount();

				StringBuffer buf = new StringBuffer();
				for(int i = 0; i < repeatCount; i++)
					buf.append(str);
				textArea.overwriteSetSelectedText(buf.toString());
			}
		}

		view.getToolkit().beep();
	}

	public boolean isRepeatable()
	{
		return false;
	}
}
