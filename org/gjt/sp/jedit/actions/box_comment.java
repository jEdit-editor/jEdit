/*
 * box_comment.java
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

public class box_comment extends EditAction
{
	public box_comment()
	{
		super("box-comment");
	}

	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		JEditTextArea textArea = view.getTextArea();
		Buffer buffer = view.getBuffer();
		String commentStart = (String)buffer.getProperty("commentStart");
		String commentEnd = (String)buffer.getProperty("commentEnd");
		String boxComment = (String)buffer.getProperty("boxComment");
		if(commentStart == null || commentEnd == null
			|| boxComment == null)
		{
			view.getToolkit().beep();
			return;
		}
		commentStart = commentStart + ' ';
		commentEnd = ' ' + commentEnd;
		boxComment = boxComment + ' ';
		int selectionStart = textArea.getSelectionStart();
		int selectionEnd = textArea.getSelectionEnd();
		int startLine = textArea.getSelectionStartLine();
		int endLine = textArea.getSelectionEndLine();
		Element map = buffer.getDefaultRootElement();

		buffer.beginCompoundEdit();

		try
		{
			Element lineElement = map.getElement(startLine);
			int start = lineElement.getStartOffset();
			int indent = MiscUtilities.getLeadingWhiteSpace(
				buffer.getText(start,lineElement.getEndOffset()
				- start));
			buffer.insertString(Math.max(start + indent,selectionStart),
				commentStart,null);
			for(int i = startLine + 1; i <= endLine; i++)
			{
				lineElement = map.getElement(i);
				start = lineElement.getStartOffset();
				indent = MiscUtilities.getLeadingWhiteSpace(
					buffer.getText(start,lineElement
					.getEndOffset() - start));
				buffer.insertString(start + indent,boxComment,null);
			}
			lineElement = map.getElement(endLine);
			start = lineElement.getStartOffset();
			indent = MiscUtilities.getLeadingWhiteSpace(buffer
				.getText(start,lineElement.getEndOffset()
				- start));
			buffer.insertString(Math.max(start + indent,textArea
				.getSelectionEnd()),commentEnd,null);
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
		finally
		{
			buffer.endCompoundEdit();
		}

		textArea.select(textArea.getCaretPosition(),
			textArea.getCaretPosition());
	}
}
