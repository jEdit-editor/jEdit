/*
 * complete_word.java
 * Copyright (C) 2000 Slava Pestov
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
import java.util.Vector;
import org.gjt.sp.jedit.gui.CompleteWord;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.jedit.*;

public class complete_word extends EditAction
{
	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		Buffer buffer = view.getBuffer();
		String noWordSep = (String)buffer.getProperty("noWordSep");
		if(noWordSep == null)
			noWordSep = "";
		JEditTextArea textArea = view.getTextArea();

		// first, we get the word before the caret

		String line = textArea.getLineText(textArea.getCaretLine());
		int dot = textArea.getCaretPosition() - textArea.getLineStartOffset(
			textArea.getCaretLine());
		if(dot == 0)
		{
			view.getToolkit().beep();
			return;
		}

		int wordStart = TextUtilities.findWordStart(line,dot-1,noWordSep);
		String word = line.substring(wordStart,dot);
		if(word.length() == 0)
		{
			view.getToolkit().beep();
			return;
		}

		view.showWaitCursor();

		Vector completions = new Vector();
		int wordLen = word.length();

		// now loop through all lines of current buffer

		for(int i = 0; i < textArea.getLineCount(); i++)
		{
			line = textArea.getLineText(i);

			// check for match at start of line

			if(line.startsWith(word))
			{
				String _word = getWord(line,0,noWordSep);
				if(_word.length() != wordLen)
				{
					// remove duplicates
					if(completions.indexOf(_word) == -1)
						completions.addElement(_word);
				}
			}

			// check for match inside line

			int len = line.length() - word.length();
			for(int j = 0; j < len; j++)
			{
				char c = line.charAt(j);
				if(!Character.isLetterOrDigit(c) && noWordSep.indexOf(c) == -1)
				{
					if(line.regionMatches(j + 1,word,0,wordLen))
					{
						String _word = getWord(line,j + 1,noWordSep);
						if(_word.length() != wordLen)
						{
							// remove duplicates
							if(completions.indexOf(_word) == -1)
								completions.addElement(_word);
						}
					}
				}
			}
		}

		// sort completion list

		MiscUtilities.quicksort(completions,new MiscUtilities.StringICaseCompare());
		view.hideWaitCursor();

		if(completions.size() == 0)
			view.getToolkit().beep();
		// if there is only one competion, insert in buffer
		else if(completions.size() == 1)
		{
			// chop off 'wordLen' because that's what's already
			// in the buffer
			textArea.setSelectedText(((String)completions
				.elementAt(0)).substring(wordLen));
		}
		// show dialog box if > 1
		else
		{
			new CompleteWord(view,word,completions);
		}
	}

	// return word that starts at 'offset'
	private String getWord(String line, int offset, String noWordSep)
	{
		// '+ 1' so that findWordEnd() doesn't pick up the space at the start
		int wordEnd = TextUtilities.findWordEnd(line,offset + 1,noWordSep);
		return line.substring(offset,wordEnd);
	}
}
