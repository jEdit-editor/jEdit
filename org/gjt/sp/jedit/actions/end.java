/*
 * end.java
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

public class end extends EditAction
{
	private boolean select;

	public end()
	{
		this(false);
	}

	public end(boolean select)
	{
		this.select = select;
	}

	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		JEditTextArea textArea = view.getTextArea();

		int caret = textArea.getCaretPosition();

		int lastOfLine = textArea.getLineEndOffset(
			textArea.getCaretLine()) - 1;
		int lastVisibleLine = textArea.getFirstLine()
			+ textArea.getVisibleLines();
		if(lastVisibleLine >= textArea.getLineCount())
		{
			lastVisibleLine = Math.min(textArea.getLineCount() - 1,
				lastVisibleLine);
		}
		else
			lastVisibleLine -= (textArea.getElectricScroll() + 1);

		int lastVisible = textArea.getLineEndOffset(lastVisibleLine) - 1;
		int lastDocument = textArea.getDocumentLength();

		if(caret == lastDocument)
		{
			view.getToolkit().beep();
			return;
		}
		else if(!"yes".equals(jEdit.getProperty("view.homeEnd")))
			caret = lastOfLine;
		else if(caret == lastVisible)
			caret = lastDocument;
		else if(caret == lastOfLine)
			caret = lastVisible;
		else
			caret = lastOfLine;

		if(select)
			textArea.select(textArea.getMarkPosition(),caret);
		else
			textArea.setCaretPosition(caret);
	}
}
