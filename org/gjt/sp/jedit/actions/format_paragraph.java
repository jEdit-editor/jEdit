/*
 * format_paragraph.java
 * Copyright (C) 1998, 2000 Slava Pestov
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

import javax.swing.text.BadLocationException;
import java.awt.event.ActionEvent;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.*;

public class format_paragraph extends EditAction
{
	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		JEditTextArea textArea = view.getTextArea();
		Buffer buffer = view.getBuffer();
		if(!buffer.isEditable())
		{
			view.getToolkit().beep();
			return;
		}
		int maxLineLength = ((Integer)buffer.getProperty("maxLineLen"))
			.intValue();
		String text = textArea.getSelectedText();
		if(text != null)
			textArea.setSelectedText(doFormat(text,maxLineLength));
		else
		{
			int lineNo = textArea.getCaretLine();

			int start = 0, end = textArea.getBufferLength();

			for(int i = lineNo - 1; i >= 0; i--)
			{
				if(textArea.getLineLength(i) == 0)
				{
					start = textArea.getLineStartOffset(i);
					break;
				}
			}

			for(int i = lineNo + 1; i < textArea.getLineCount(); i++)
			{
				if(textArea.getLineLength(i) == 0)
				{
					end = textArea.getLineStartOffset(i);
					break;
				}
			}
			try
			{
				text = buffer.getText(start,end - start);
				buffer.remove(start,end - start);
				buffer.insertString(start,doFormat(text,
					maxLineLength),null);
			}
			catch(BadLocationException bl)
			{
				return;
			}
		}
	}

	private String doFormat(String text, int maxLineLength)
	{
		StringBuffer buf = new StringBuffer();
		StringBuffer word = new StringBuffer();
		int lineLength = 0;
		boolean newline = true;
		boolean space = false;
		char[] chars = text.toCharArray();
		for(int i = 0; i < chars.length; i++)
		{
			char c = chars[i];
			switch(c)
			{
			case '\n':
				if(i == 0 || chars.length - i <= 2)
				{
					if(lineLength + word.length() >= maxLineLength)
						buf.append('\n');
					else if(space && word.length() != 0)
						buf.append(' ');
					buf.append(word);
					word.setLength(0);
					buf.append('\n');
					newline = true;
					space = false;
					break;
				}
				else if(newline)
				{
					if(lineLength + word.length() >= maxLineLength)
						buf.append('\n');
					else if(space && word.length() != 0)
						buf.append(' ');
					buf.append(word);
					word.setLength(0);
					buf.append("\n\n");
					newline = space = false;
					lineLength = 0;
					break;
				}
				else
					newline = true;
			case ' ':
				if(lineLength + word.length() >= maxLineLength)
				{
					buf.append('\n');
					lineLength = 0;
					newline = true;
				}
				else if(space && lineLength != 0 && word.length() != 0)
				{
					buf.append(' ');
					lineLength++;
					space = false;
				}
				else
					space = true;
				buf.append(word);
				lineLength += word.length();
				word.setLength(0);
				break;
			default:
				newline = false;
				// without this test, we would have spaces
				// at the start of lines
				if(lineLength != 0)
					space = true;
				word.append(c);
				break;
			}
		}
		if(lineLength + word.length() >= maxLineLength)
			buf.append('\n');
		else if(space && word.length() != 0)
			buf.append(' ');
		buf.append(word);
		return buf.toString();
	}
}
