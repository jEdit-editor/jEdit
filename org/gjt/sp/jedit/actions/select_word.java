/*
 * select_word.java
 * Copyright (C) 1999 mike dillon
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
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditAction;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.textarea.JEditTextArea;

public class select_word extends EditAction
{
	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		JEditTextArea textArea = view.getTextArea();
		Buffer buffer = view.getBuffer();

		int line = textArea.getCaretLine();
		int lineStart = textArea.getLineStartOffset(line);
		int offset = textArea.getCaretPosition() - lineStart;

		String lineText = textArea.getLineText(line);
		char ch = lineText.charAt(Math.max(0,offset - 1));

		String noWordSep = (String)buffer.getProperty("noWordSep");
		if(noWordSep == null)
			noWordSep = "";

		// If the user clicked on a non-letter char,
		// we select the surrounding non-letters
		boolean selectNoLetter = (!Character
			.isLetterOrDigit(ch)
			&& noWordSep.indexOf(ch) == -1);

		int wordStart = 0;
		for(int i = offset - 1; i >= 0; i--)
		{
			ch = lineText.charAt(i);
			if(selectNoLetter ^ (!Character
				.isLetterOrDigit(ch) &&
				noWordSep.indexOf(ch) == -1))
			{
				wordStart = i + 1;
				break;
			}
		}

		int wordEnd = lineText.length();
		for(int i = offset; i < lineText.length(); i++)
		{
			ch = lineText.charAt(i);
			if(selectNoLetter ^ (!Character
				.isLetterOrDigit(ch) &&
				noWordSep.indexOf(ch) == -1))
			{
				wordEnd = i;
				break;
			}
		}

		textArea.select(lineStart + wordStart,lineStart + wordEnd);
	}
}
