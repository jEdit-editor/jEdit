/*
 * home.java
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

public class home extends EditAction
{
	private boolean select;

	public home()
	{
		this(false);
	}

	public home(boolean select)
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

		int firstIndent = MiscUtilities.getLeadingWhiteSpace(textArea
			.getLineText(line));

		int firstOfLine = textArea.getLineStartOffset(line);

		firstIndent = firstOfLine + firstIndent;
		if(firstIndent == textArea.getLineEndOffset(line) - 1)
			firstIndent = firstOfLine;

		int firstLine = textArea.getFirstLine();
		int electricScroll = textArea.getElectricScroll();

		int firstVisibleLine = (firstLine <= electricScroll) ? 0 :
			firstLine + electricScroll;
		if(firstVisibleLine >= textArea.getLineCount())
			firstVisibleLine = textArea.getLineCount() - 1;

		int firstVisible = textArea.getLineStartOffset(firstVisibleLine);

		int[] positions = { firstIndent, firstOfLine, firstVisible };
		int count;
		if(!jEdit.getBooleanProperty("view.homeEnd"))
			count = 1;
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
