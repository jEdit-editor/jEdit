/*
 * shift_right.java
 * Copyright (C) 1999 Slava Pestov
 *
 * This	free software; you can redistribute it and/or
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

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import java.awt.event.ActionEvent;
import org.gjt.sp.jedit.gui.SyntaxTextArea;
import org.gjt.sp.jedit.*;

public class shift_right extends EditAction
{
	public shift_right()
	{
		super("shift-right");
	}

	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		Buffer buffer = view.getBuffer();
		buffer.beginCompoundEdit();
		try
		{
			SyntaxTextArea textArea = view.getTextArea();
			if(!textArea.isEditable())
			{
				view.getToolkit().beep();
				return;
			}
			int tabSize = buffer.getTabSize();
			boolean noTabs = ("yes".equals(buffer.getProperty(
				"noTabs")));
			Element map = buffer.getDefaultRootElement();
			int start = map.getElementIndex(textArea
				.getSelectionStart());
			int end = map.getElementIndex(textArea
				.getSelectionEnd());
			for(int i = start; i <= end; i++)
			{
				Element lineElement = map.getElement(i);
				int lineStart = lineElement.getStartOffset();
				String line = buffer.getText(lineStart,
					lineElement.getEndOffset() - lineStart - 1);
				int whiteSpace = TextUtilities
					.getLeadingWhiteSpace(line);
				int whiteSpaceWidth = TextUtilities
					.getLeadingWhiteSpaceWidth(
					line,tabSize) + tabSize;
				buffer.remove(lineStart,whiteSpace);
				buffer.insertString(lineStart,TextUtilities
					.createWhiteSpace(whiteSpaceWidth,
					tabSize,noTabs),null);
			}
		}
		catch(BadLocationException bl)
		{
			bl.printStackTrace();
		}
		buffer.endCompoundEdit();
	}
}	
