/*
 * goto_line.java
 * Copyright (C) 1998, 1999 Slava Pestov
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

import javax.swing.*;
import javax.swing.text.Element;
import java.awt.event.ActionEvent;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class goto_line extends EditAction
implements InputHandler.NonRepeatable
{
	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		Buffer buffer = view.getBuffer();
		JEditTextArea textArea = view.getTextArea();

		int line = 0;

		if(textArea.getInputHandler().isRepeatEnabled())
			line = textArea.getInputHandler().getRepeatCount();
		else
		{
			try
			{
				line = Integer.parseInt(GUIUtilities.input(view,"gotoline",null));
			}
			catch(NumberFormatException nf)
			{}
		}

		Element map = buffer.getDefaultRootElement();

		if(line < 1 || line > map.getElementCount())
		{
			view.getToolkit().beep();
			return;
		}

		Element element = map.getElement(line - 1);
		view.getTextArea().setCaretPosition(element.getStartOffset());
	}
}
