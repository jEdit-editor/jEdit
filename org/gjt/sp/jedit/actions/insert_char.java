/*
 * insert_char.java - Action
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
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class insert_char extends EditAction
{
	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		Buffer buffer = view.getBuffer();
		JEditTextArea textArea = view.getTextArea();
		if(!textArea.isEditable())
		{
			view.getToolkit().beep();
			return;
		}

		String str = evt.getActionCommand();

		char ch = str.charAt(0);
		if(Abbrevs.getExpandOnInput() && ch == ' '
			&& Abbrevs.expandAbbrev(view,false))
			; // do nothing
		else
		{
			int repeatCount = view.getInputHandler().getRepeatCount();

			StringBuffer buf = new StringBuffer();
			for(int i = 0; i < repeatCount; i++)
				buf.append(str);
			textArea.overwriteSetSelectedText(buf.toString());
		}

		// do word wrap
		try
		{
			doWordWrap(buffer,textArea,textArea.getCaretLine());
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
	}

	public boolean isRepeatable()
	{
		return false;
	}

	public boolean needsActionCommand()
	{
		return true;
	}

	// private members
	private void doWordWrap(Buffer buffer, JEditTextArea textArea, int line)
		throws BadLocationException
	{
		int maxLineLen = ((Integer)buffer.getProperty("maxLineLen"))
			.intValue();

		if(maxLineLen <= 0)
			return;

		Element lineElement = buffer.getDefaultRootElement()
			.getElement(line);
		int start = lineElement.getStartOffset();
		int end = lineElement.getEndOffset();

		// don't wrap unless we're at the end of the line
		if(textArea.getCaretPosition() != end - 1)
			return;

		int tabSize = buffer.getTabSize();

		String wordBreakChars = (String)buffer.getProperty("wordBreakChars");

		String text = buffer.getText(start,end - start - 1);

		int logicalLength = 0; // length with tabs expanded
		int lastWordOffset = -1;
		boolean lastWasSpace = true;
		for(int i = 0; i < text.length(); i++)
		{
			char ch = text.charAt(i);
			if(ch == '\t')
			{
				logicalLength += tabSize - (logicalLength % tabSize);
				if(!lastWasSpace)
				{
					lastWordOffset = i;
					lastWasSpace = true;
				}
			}
			else if(ch == ' ')
			{
				logicalLength++;
				if(!lastWasSpace)
				{
					lastWordOffset = i;
					lastWasSpace = true;
				}
			}
			else if(wordBreakChars != null && wordBreakChars.indexOf(ch) != -1)
			{
				logicalLength++;
				if(!lastWasSpace)
				{
					lastWordOffset = i;
					lastWasSpace = true;
				}
			}
			else
			{
				logicalLength++;
				lastWasSpace = false;
			}

			if(logicalLength > maxLineLen && lastWordOffset != -1)
			{
				// break line at lastWordOffset
				try
				{
					buffer.beginCompoundEdit();
					buffer.insertString(lastWordOffset + start,"\n",null);
					buffer.indentLine(textArea,line + 1,true,true);
				}
				finally
				{
					buffer.endCompoundEdit();
				}

				break;
			}
		}
	}
}
