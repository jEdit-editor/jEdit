/*
 * expand_abbrev.java
 * Copyright (C) 1998, 1999 Slava Pestov
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
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class expand_abbrev extends EditAction
{
	public expand_abbrev()
	{
		super("expand-abbrev");
	}

	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		Buffer buffer = view.getBuffer();
		JEditTextArea textArea = view.getTextArea();

		String separators = (String)buffer.getProperty("noWordSep");
		int dot = textArea.getSelectionStart();
		int lineNo = textArea.getSelectionStartLine();	
		int start = textArea.getLineStartOffset(lineNo);
		int len = textArea.getLineEndOffset(lineNo) - start - 1;

		// Support finding the previous match
		if(view == lastView && buffer == lastBuffer
			&& dot == lastDot + lastMatchLen)
		{
			Log.log(Log.ERROR,this,"hey bob");
			dot = lastDot;
			lineNo = lastMatch;
		}
		else
		{
			Log.log(Log.ERROR,this,"d=" + dot + ",ld=" + lastDot
				+ ",lml=" + lastMatchLen);

			lastView = view;
			lastBuffer = buffer;
			lastDot = dot;
			lastMatchLen = 0;
		}

		String line;

		try
		{
			line = buffer.getText(start,len); // chop newline

			// scan backwards to find word
			int wordStart = start;
loop:			for(int i = dot - 1; i >= start; i--)
			{
				char c = line.charAt(i - start);
				switch(c)
				{
				case ' ':
				case '\t':
					wordStart = i + 1;
					break loop;
				default:
					if(!Character.isLetterOrDigit(c)
						&& (separators == null
						|| separators.indexOf(c) == -1))
					{
						wordStart = i + 1;
						break loop;
					}
				}
			}
			if(wordStart == dot)
			{
				view.getToolkit().beep();
				return;
			}
			String word = line.substring(wordStart - start,
						     dot - start);
			// loop through lines in file looking for previous
			// occurance of word
			for(int i = lineNo; i >= 0; i--)
			{
				int lineStart = textArea.getLineStartOffset(i);
				int lineLen;
				if(i == lineNo)
					lineLen = wordStart - lineStart;
				else
				{
					lineLen = textArea.getLineEndOffset(i)
						- lineStart - 1;
				}
				line = buffer.getText(lineStart, lineLen);
				int index = getIndexOfWord(line,word,separators);
				if(index != -1)
				{
					int wordEnd = lineLen;
loop2:					for(int j = index + 1; j < lineLen; j++)
					{
						char c = line.charAt(j);
						switch(c)
						{
						case ' ':
						case '\t':
							wordEnd = j;
							break loop2;
						default:
							if(!Character.isLetterOrDigit(c)
								&& (separators == null
								|| separators.indexOf(c) == -1))
							{
								wordEnd = j;
								break loop2;
							}
						}
					}

					buffer.remove(dot - lastMatchLen,lastMatchLen);

					lastMatch = i - 1;
					lastMatchLen = wordEnd - (index + word.length());

					textArea.setSelectedText(
						line.substring(index +
						word.length(),wordEnd));
					return;
				}
			}
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}

		lastMatchLen = 0;

		view.getToolkit().beep();
	}

	// private members
	private View lastView;
	private Buffer lastBuffer;
	private int lastDot = -1;
	private int lastMatchLen;
	private int lastMatch = -1;

	private int getIndexOfWord(String line, String word, String separators)
	{
		for(int i = 0; i < line.length(); i++)
		{
			if(i == 0)
			{
				if(line.regionMatches(0,word,0,word.length()))
					return 0;
			}

			char c = line.charAt(i);
			if(!Character.isLetterOrDigit(c)
				&& (separators == null ||
				separators.indexOf(c) == -1))
			{
				if(line.regionMatches(i + 1,word,
					0,word.length()))
					return i + 1;
			}
		}

		return -1;
	}
}
