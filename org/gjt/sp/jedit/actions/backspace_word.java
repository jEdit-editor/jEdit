/*
 * backspace_word.java
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
import javax.swing.text.BadLocationException;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class backspace_word extends EditAction
{
	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		JEditTextArea textArea = view.getTextArea();
		if(!textArea.isEditable())
		{
			view.getToolkit().beep();
			return;
		}

		int start = textArea.getSelectionStart();
		if(start != textArea.getSelectionEnd())
		{
			textArea.setSelectedText("");
			return;
		}

		int line = textArea.getCaretLine();
		int lineStart = textArea.getLineStartOffset(line);
		int caret = start - lineStart;

		String lineText = textArea.getLineText(textArea
			.getCaretLine());

		if(caret == 0)
		{
			if(lineStart == 0)
			{
				view.getToolkit().beep();
				return;
			}
			caret--;
		}
		else
		{
			String noWordSep = (String)view.getBuffer()
				.getProperty("noWordSep");
			caret = TextUtilities.findWordStart(lineText,
				caret-1,noWordSep);
		}

		try
		{
			view.getBuffer().remove(caret + lineStart,
					start - (caret + lineStart));
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
	}
}
