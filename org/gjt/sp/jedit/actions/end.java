/*
 * end.java
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
		InputHandler inputHandler = view.getInputHandler();

		int caret = textArea.getCaretPosition();
		int line = textArea.getCaretLine();

		int lastIndent = textArea.getLineEndOffset(line)
			- MiscUtilities.getTrailingWhiteSpace(textArea
			.getLineText(line)) - 1;

		int lastOfLine = textArea.getLineEndOffset(
			textArea.getCaretLine()) - 1;

		int lastVisibleLine = textArea.getFirstLine()
			+ textArea.getVisibleLines();

		if(lastVisibleLine >= textArea.getLineCount())
		{
			lastVisibleLine = Math.min(textArea.getLineCount(),
				lastVisibleLine);
		}
		else if(lastVisibleLine <= textArea.getElectricScroll())
			lastVisibleLine = 0;
		else
			lastVisibleLine -= (textArea.getElectricScroll() + 1);

		int lastVisible = textArea.getLineEndOffset(lastVisibleLine) - 1;

		int[] positions = { lastIndent, lastOfLine, lastVisible };
		int count;
		if(!jEdit.getBooleanProperty("view.homeEnd"))
			count = 2;
		else if(inputHandler.getLastAction() == this)
		{
			count = Math.min(positions.length,inputHandler
				.getLastActionCount()) - 1;
		}
		else
			count = 0;

		caret = positions[count];

		if(select)
			textArea.select(textArea.getMarkPosition(),caret);
		else
			textArea.setCaretPosition(caret);
	}
}
