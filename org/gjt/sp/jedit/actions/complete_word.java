/*
 * complete_word.java
 * Copyright (C) 1998, 1999 Slava Pestov
 * Copyright (C) 1999 Valery Kondakoff
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
import java.util.*;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class complete_word extends EditAction
{
	private boolean createSet = true, forward = true;
	private Vector wordSet = new Vector();
	private View view;
	private Buffer buffer;
	private JEditTextArea textArea;
	private String separators, line, word;
	private int dot, dotdot, lineNo, start, len, wordStart, lineCount;
	private int count = 1;
	private int myLength;
	private int[] array;

	public void actionPerformed(ActionEvent evt)
	{

		view = getView(evt);
		buffer = view.getBuffer();
		textArea = view.getTextArea();

		separators = (String)buffer.getProperty("noWordSep");

		dot = textArea.getSelectionStart();
		lineNo = textArea.getSelectionStartLine();
		start = textArea.getLineStartOffset(lineNo);
		len = textArea.getLineEndOffset(lineNo) - start - 1;
		lineCount = textArea.getLineCount();

		if (createSet)
		{
			word = getWord();
			firstTurn(word);
		}
		else
		{
			String newString = getWord();
			if (newString == null)
			{
				createSet = true;
				return;
			}
			int wordLength = word.length();
			if (newString.length() < wordLength ||
						!wordSet.contains(newString.substring(word.length()))
						|| (dotdot + myLength) < dot)
			{
				createSet = true;
				count = 1;
				firstTurn(newString);
				return;
			}
			int size = wordSet.size() - 1;
			if (size > 0)
			{
				if (count < size & forward)
				{
					textArea.setSelectionStart(dotdot);
					textArea.setSelectedText((String)wordSet.elementAt(count));
					myLength = ((String)wordSet.elementAt(count)).length();
					count++;
				}
				else
				{
					textArea.setSelectionStart(dotdot);
					textArea.setSelectedText((String)wordSet.elementAt(count));
					myLength = ((String)wordSet.elementAt(count)).length();
					count = 0;
				}
			}
			else
			{
				createSet = true;
				return;
			}
		}
	}

	private void firstTurn(String string)
	{
		word = string;
		if (string == null)
		{
			return;
		}
		wordSet.removeAllElements();
		array = null;
		fillWordSet(wordSet, string);
		if (wordSet.isEmpty())
		{
			return;
		}
		if (wordSet.size() == 1)
		{
			dotdot = textArea.getSelectionStart();
			textArea.setSelectedText((String)wordSet.elementAt(0));
			myLength = ((String)wordSet.elementAt(0)).length();
			return;
		}
		else
		{
			dotdot = textArea.getSelectionStart();
			textArea.setSelectedText((String)wordSet.elementAt(0));
			myLength = ((String)wordSet.elementAt(0)).length();
			createSet = false;
		}
	}

	private void addString(Vector vector, String string)
	{
		if (!vector.contains(string))
		{
			vector.addElement(string);
		}
	}

	private void fillWordSet(Vector vector, String string)
	{
		try
		{
			for(int i = lineCount - 1; i >= 0; i--)
			{
				int lineStart = textArea.getLineStartOffset(i);
				int lineLen = textArea.getLineEndOffset(i) - lineStart - 1;
				line = buffer.getText(lineStart, lineLen);
				int[] array = getIndexOfWord(line,string,separators);

				for (int y = 0; y < array.length; y++)
				{
					int index = array[y];
					if(index != -1)
					{
						int wordEnd = lineLen;
loop2:						for(int j = index + 1; j < lineLen; j++)
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
						String temp = line.substring(index + string.length(),wordEnd);
						if (temp.length() != 0)
						{
							addString(vector, temp);
						}
					}
				}
			}
			array = null;
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
	}

	private String getWord()
	{
		try
		{
			line = buffer.getText(start,len); // chop newline
			// scan backwards to find word
			wordStart = start;
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
			if (wordStart == dot)
			{
				view.getToolkit().beep();
				return null;
			}

		}
		catch (BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
		return line.substring(wordStart - start, dot - start);
	}

	private int[] getIndexOfWord(String line, String word, String separators)
	{
		array = new int[line.length()];
		for(int i = 0; i < line.length(); i++)
		{
			if(i == 0)
			{
				if(line.regionMatches(0,word,0,word.length()))
				{
					array[i] = 0;
					continue;
				}
			}
			char c = line.charAt(i);
			if(!Character.isLetterOrDigit(c)
				&& (separators == null ||
				separators.indexOf(c) == -1))
			{
				array[i] = -1;
				if(line.regionMatches(i + 1,word,
					0,word.length()))
					array[i] = i + 1;
			}
			else
			{
				array[i] = -1;
			}
		}
		return array;
	}
}
