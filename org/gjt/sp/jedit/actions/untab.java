/*
 * untab.java
 * Copyright (C) 1998 Slava Pestov
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
import javax.swing.text.Element;
import java.awt.event.ActionEvent;
import org.gjt.sp.jedit.syntax.SyntaxTextArea;
import org.gjt.sp.jedit.*;

public class untab extends EditAction
{
	public untab()
	{
		super("untab");
	}

	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		Buffer buffer = view.getBuffer();
		try
		{
			SyntaxTextArea textArea = view.getTextArea();
			if(!textArea.isEditable())
			{
				view.getToolkit().beep();
				return;
			}
			int start = textArea.getSelectionStart();
			int end = textArea.getSelectionEnd();
			Element map = buffer.getDefaultRootElement();
			Element lineElement = map.getElement(
				map.getElementIndex(textArea
				.getCaretPosition()));
			int lineStart = lineElement.getStartOffset();
			int lineEnd = lineElement.getEndOffset() - 1;
			if(start == end)
			{
				start = lineStart;
				end = lineEnd;
			}
			end -= start;
			int tabSize = buffer.getTabSize();
			String text = doUntab(buffer.getText(start,end),
				tabSize);
			buffer.remove(start,end);
			buffer.insertString(start,text,null);
		}
		catch(BadLocationException bl)
		{
		}
	}

	private String doUntab(String in, int tabSize)
	{
		StringBuffer buf = new StringBuffer();
		int width = 0;
		for(int i = 0; i < in.length(); i++)
		{
			switch(in.charAt(i))
			{
			case '\t':
				int count = tabSize - (width % tabSize);
				width += count;
				while(--count >= 0)
					buf.append(' ');
				break;
			case '\n':
				width = 0;
				buf.append(in.charAt(i));
				break;
			default:
				width++;
				buf.append(in.charAt(i));
				break;
                        }
                }
                return buf.toString();
	}
}
