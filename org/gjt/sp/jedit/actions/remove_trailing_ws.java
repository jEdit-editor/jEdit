/*
 * remove_trailing_ws.java - jEdit action to remove trailing whitespace
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
import javax.swing.text.Element;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditAction;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.util.Log;

public class remove_trailing_ws extends EditAction
{
	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		JEditTextArea textArea = view.getTextArea();
		Buffer buffer = view.getBuffer();

		Element map = buffer.getDefaultRootElement();

		int selectionStart = textArea.getSelectionStart();
		int selectionEnd = textArea.getSelectionEnd();

		int start, end;
		if(selectionStart == selectionEnd)
		{
			start = 0;
			end = textArea.getLineCount();
		}
		else
		{
			start = textArea.getSelectionStartLine();
			end = textArea.getSelectionEndLine() + 1;
		}

		try
		{
			buffer.beginCompoundEdit();

			for(int i = start; i < end; i++)
			{
				Element lineElement = map.getElement(i);
				int lineStart = lineElement.getStartOffset();
				int lineEnd = lineElement.getEndOffset() - 1;

				String text = buffer.getText(lineStart,
					lineEnd - lineStart);
				buffer.remove(lineStart,lineEnd - lineStart);
				text = removeTrailingWS(text);
				buffer.insertString(lineStart,text,null);
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

	protected String removeTrailingWS(String text)
	{
		char c;
		for (int i = text.length(); i > 0; i--)
		{
			c = text.charAt(i - 1);

			if (!(c == ' ' || c == '\t'))
				return text.substring(0, i);
		}

		return new String();
	}
}
