/*
 * shift_left.java
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
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class shift_left extends EditAction
{
	public shift_left()
	{
		super("shift-left");
	}

	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		Buffer buffer = view.getBuffer();
		buffer.beginCompoundEdit();
		try
		{
			JEditTextArea textArea = view.getTextArea();
			if(!textArea.isEditable())
			{
				view.getToolkit().beep();
				return;
			}
			int tabSize = buffer.getTabSize();
			boolean noTabs = ("yes".equals(buffer.getProperty(
				"noTabs")));
			Element map = buffer.getDefaultRootElement();
			int start = textArea.getSelectionStartLine();
			int end = textArea.getSelectionEndLine();
			for(int i = start; i <= end; i++)
			{
				Element lineElement = map.getElement(i);
				int lineStart = lineElement.getStartOffset();
				String line = buffer.getText(lineStart,
					lineElement.getEndOffset() - lineStart - 1);
				int whiteSpace = MiscUtilities
					.getLeadingWhiteSpace(line);
				if(whiteSpace == 0)
					continue;
				int whiteSpaceWidth = Math.max(0,MiscUtilities
					.getLeadingWhiteSpaceWidth(line,tabSize)
					- tabSize);
				buffer.remove(lineStart,whiteSpace);
				buffer.insertString(lineStart,MiscUtilities
					.createWhiteSpace(whiteSpaceWidth,
					(noTabs ? 0 : tabSize)),null);
			}
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
		finally
		{
			buffer.endCompoundEdit();
		}
	}
}
