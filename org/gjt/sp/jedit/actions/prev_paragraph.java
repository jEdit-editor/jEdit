/*
 * prev_paragraph.java
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

import java.awt.event.ActionEvent;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.textarea.JEditTextArea;

public class prev_paragraph extends EditAction
{
	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		Buffer buffer = view.getBuffer();
		JEditTextArea textArea = view.getTextArea();

		int lineNo = textArea.getCaretLine();

		for(int i = lineNo - 1; i >= 0; i--)
		{
			if(textArea.getLineLength(i) == 0)
			{
				textArea.setCaretPosition(textArea
					.getLineStartOffset(i));
				return;
			}
		}

		textArea.setCaretPosition(0);
	}
}
