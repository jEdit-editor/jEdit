/*
 * TextUtilities.java - Utility functions used by the text area classes
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

package org.gjt.sp.jedit.textarea;

import javax.swing.text.*;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.jedit.Buffer;

/**
 * Class with several utility functions used by the text area component.
 * @author Slava Pestov
 * @version $Id$
 */
public class TextUtilities
{
	/**
	 * Returns the offset of the bracket matching the one at the
	 * specified offset of the buffer, or -1 if the bracket is
	 * unmatched (or if the character is not a bracket).
	 * @param buffer The buffer
	 * @param line The line
	 * @param offset The offset within that line
	 * @exception BadLocationException If an out-of-bounds access
	 * was attempted on the buffer's text
	 * @since jEdit 3.0pre1
	 */
	public static int findMatchingBracket(Buffer buffer, int line, int offset)
		throws BadLocationException
	{
		if(buffer.getLength() == 0)
			return -1;

		Element map = buffer.getDefaultRootElement();
		Element lineElement = map.getElement(line);
		Segment lineText = new Segment();
		int lineStart = lineElement.getStartOffset();
		buffer.getText(lineStart,lineElement.getEndOffset() - lineStart - 1,
			lineText);

		char c = lineText.array[lineText.offset + offset];
		char cprime; // c` - corresponding character
		boolean direction; // true = back, false = forward

		switch(c)
		{
		case '(': cprime = ')'; direction = false; break;
		case ')': cprime = '('; direction = true; break;
		case '[': cprime = ']'; direction = false; break;
		case ']': cprime = '['; direction = true; break;
		case '{': cprime = '}'; direction = false; break;
		case '}': cprime = '{'; direction = true; break;
		default: return -1;
		}

		int count;

		TokenMarker tokenMarker = buffer.getTokenMarker();

		// Get the syntax token at 'offset'
		// only tokens with the same type will be checked for
		// the corresponding bracket
		byte idOfBracket = Token.NULL;

		TokenMarker.LineInfo lineInfo = tokenMarker.markTokens(lineText,line);
		Token lineTokens = lineInfo.firstToken;

		int tokenListOffset = 0;
		for(;;)
		{
			if(lineTokens.id == Token.END)
				throw new InternalError("offset > line length");
	
			if(tokenListOffset + lineTokens.length > offset)
			{
				idOfBracket = lineTokens.id;
				break;
			}
			else
			{
				tokenListOffset += lineTokens.length;
				lineTokens = lineTokens.next;
			}
		}

		if(direction)
		{
			// scan backwards

			count = 0;

			for(int i = line; i >= 0; i--)
			{
				// get text
				lineElement = map.getElement(i);
				lineStart = lineElement.getStartOffset();
				int lineLength = lineElement.getEndOffset()
					- lineStart - 1;

				buffer.getText(lineStart,lineLength,lineText);

				int scanStartOffset;
				if(i != line)
				{
					lineTokens = tokenMarker.markTokens(lineText,i).lastToken;
					tokenListOffset = lineLength;
					scanStartOffset = lineLength - 1;
				}
				else
				{
 					if(tokenListOffset != lineLength)
 						tokenListOffset += lineTokens.length;
					//lineTokens = lineInfo.lastToken;
					scanStartOffset = offset;
					/* System.err.println("sso=" + scanStartOffset + ",tlo=" + tokenListOffset);

					Token __ = lineTokens;
					for(;;)
					{
						if(__ == null)
							break;
						System.err.println(__);
						__ = __.prev;
					} */
				}

				// only check tokens with id 'idOfBracket'
				while(lineTokens != null)
				{
					byte id = lineTokens.id;
					if(id == Token.END)
					{
						lineTokens = lineTokens.prev;
						continue;
					}

					int len = lineTokens.length;
					if(id == idOfBracket)
					{
						for(int j = scanStartOffset; j >= tokenListOffset  - len; j--)
						{
							/* if(j >= lineText.count)
								System.err.println("WARNING: " + j + " >= " + lineText.count);
							else if(j < 0)
							{
								System.err.println("sso=" + scanStartOffset + ", tlo=" + tokenListOffset + ",len=" + len);
								System.err.println("WARNING: " + j + " < 0");
							} */

							char ch = lineText.array[lineText.offset + j];
							if(ch == c)
								count++;
							else if(ch == cprime)
							{
								if(--count == 0)
									return lineStart + j;
							}
						}
					}

					scanStartOffset = tokenListOffset = tokenListOffset - len;
					lineTokens = lineTokens.prev;
				}
			}
		}
		else
		{
			// scan forwards

			count = 0;

			for(int i = line; i < map.getElementCount(); i++)
			{
				// get text
				lineElement = map.getElement(i);
				lineStart = lineElement.getStartOffset();
				buffer.getText(lineStart,lineElement.getEndOffset()
					- lineStart - 1,lineText);

				int scanStartOffset;
				if(i != line)
				{
					lineTokens = tokenMarker.markTokens(lineText,i).firstToken;
					tokenListOffset = 0;
					scanStartOffset = 0;
				}
				else
					scanStartOffset = offset + 1;

				// only check tokens with id 'idOfBracket'
				for(;;)
				{
					byte id = lineTokens.id;
					if(id == Token.END)
						break;

					int len = lineTokens.length;
					if(id == idOfBracket)
					{
						for(int j = scanStartOffset; j < tokenListOffset + len; j++)
						{
							char ch = lineText.array[lineText.offset + j];
							if(ch == c)
								count++;
							else if(ch == cprime)
							{
								if(count-- == 0)
									return lineStart + j;
							}
						}
					}

					scanStartOffset = tokenListOffset = tokenListOffset + len;
					lineTokens = lineTokens.next;
				}
			}
		}

		// Nothing found
		return -1;
	}

	/**
	 * Locates the start of the word at the specified position.
	 * @param line The text
	 * @param pos The position
	 * @param noWordSep Characters that are non-alphanumeric, but
	 * should be treated as word characters anyway
	 */
	public static int findWordStart(String line, int pos, String noWordSep)
	{
		char ch = line.charAt(pos);

		if(noWordSep == null)
			noWordSep = "";
		boolean selectNoLetter = (!Character.isLetterOrDigit(ch)
			&& noWordSep.indexOf(ch) == -1);

		int wordStart = 0;
		for(int i = pos; i >= 0; i--)
		{
			ch = line.charAt(i);
			if(selectNoLetter ^ (!Character.isLetterOrDigit(ch) &&
				noWordSep.indexOf(ch) == -1))
			{
				wordStart = i + 1;
				break;
			}
		}

		return wordStart;
	}

	/**
	 * Locates the end of the word at the specified position.
	 * @param line The text
	 * @param pos The position
	 * @param noWordSep Characters that are non-alphanumeric, but
	 * should be treated as word characters anyway
	 */
	public static int findWordEnd(String line, int pos, String noWordSep)
	{
		if(pos != 0)
			pos--;

		char ch = line.charAt(pos);

		if(noWordSep == null)
			noWordSep = "";
		boolean selectNoLetter = (!Character.isLetterOrDigit(ch)
			&& noWordSep.indexOf(ch) == -1);

		int wordEnd = line.length();
		for(int i = pos; i < line.length(); i++)
		{
			ch = line.charAt(i);
			if(selectNoLetter ^ (!Character.isLetterOrDigit(ch) &&
				noWordSep.indexOf(ch) == -1))
			{
				wordEnd = i;
				break;
			}
		}
		return wordEnd;
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.9  2000/07/22 03:27:04  sp
 * threaded I/O improved, autosave rewrite started
 *
 * Revision 1.8  2000/07/15 06:56:29  sp
 * bracket matching debugged
 *
 * Revision 1.7  2000/07/14 06:00:45  sp
 * bracket matching now takes syntax info into account
 *
 * Revision 1.6  2000/01/28 00:20:58  sp
 * Lots of stuff
 *
 * Revision 1.5  1999/12/19 11:14:29  sp
 * Static abbrev expansion started
 *
 * Revision 1.4  1999/12/13 03:40:30  sp
 * Bug fixes, syntax is now mostly GPL'd
 *
 * Revision 1.3  1999/11/21 03:40:18  sp
 * Parts of EditBus not used by core moved to EditBus.jar
 *
 * Revision 1.2  1999/07/16 23:45:49  sp
 * 1.7pre6 BugFree version
 *
 * Revision 1.1  1999/06/29 09:03:18  sp
 * oops, forgot to add TextUtilities.java
 *
 */
