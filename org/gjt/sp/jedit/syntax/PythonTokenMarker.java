/*
 * PythonTokenMarker.java - Python token marker
 * Copyright (C) 1999 Jonathan Revusky
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
package org.gjt.sp.jedit.syntax;

import javax.swing.text.Segment;

/**
 * Python token marker.
 *
 * @author Jonathan Revusky
 * @version $Id$
 */
public class PythonTokenMarker extends TokenMarker
{
	private static final byte TRIPLEQUOTE1 = Token.INTERNAL_FIRST;
	private static final byte TRIPLEQUOTE2 = Token.INTERNAL_LAST;

	public PythonTokenMarker()
	{
		this.keywords = getKeywords();
	}

	public byte markTokensImpl(byte token, Segment line, int lineIndex, LineInfo info)
	{
		char[] array = line.array;
		int offset = line.offset;
		lastOffset = offset;
		lastKeyword = offset;
		int length = line.count + offset;
		boolean backslash = false;

loop:		for(int i = offset; i < length; i++)
		{
			int i1 = (i+1);

			char c = array[i];
			if(c == '\\')
			{
				backslash = !backslash;
				continue;
			}

			switch(token)
			{
			case Token.NULL:
				switch(c)
				{
				case '#':
					if(backslash)
						backslash = false;
					else
					{
						doKeyword(info,line,i,c);
						addToken(info,i - lastOffset,token);
						addToken(info,length - i,Token.COMMENT1);
						lastOffset = lastKeyword = length;
						break loop;
					}
					break;
				case '"':
					doKeyword(info,line,i,c);
					if(backslash)
						backslash = false;
					else
					{
						addToken(info,i - lastOffset,token);
						if(SyntaxUtilities.regionMatches(false,
							line,i1,"\"\""))
						{
							token = TRIPLEQUOTE1;
						}
						else
						{
							token = Token.LITERAL1;
						}
						lastOffset = lastKeyword = i;
					}
					break;
				case '\'':
					doKeyword(info,line,i,c);
					if(backslash)
						backslash = false;
					else
					{
						addToken(info,i - lastOffset,token);
						if(SyntaxUtilities.regionMatches(false,
							line,i1,"''"))
						{
							token = TRIPLEQUOTE2;
						}
						else
						{
							token = Token.LITERAL2;
						}
						lastOffset = lastKeyword = i;
					}
					break;
				default:
					backslash = false;
					if(!Character.isLetterOrDigit(c)
						&& c != '_')
						doKeyword(info,line,i,c);
					break;
				}
				break;
			case Token.LITERAL1:
				if(backslash)
					backslash = false;
				else if(c == '"')
				{
					addToken(info,i1 - lastOffset,token);
					token = Token.NULL;
					lastOffset = lastKeyword = i1;
				}
				break;
			case Token.LITERAL2:
				if(backslash)
					backslash = false;
				else if(c == '\'')
				{
					addToken(info,i1 - lastOffset,Token.LITERAL1);
					token = Token.NULL;
					lastOffset = lastKeyword = i1;
				}
				break;
			case TRIPLEQUOTE1:
				if(backslash)
					backslash = false;
				else if(SyntaxUtilities.regionMatches(false,
					line,i,"\"\"\""))
				{
					addToken(info,(i+=4) - lastOffset,
						Token.LITERAL1);
					token = Token.NULL;
					lastOffset = lastKeyword = i;
				}
				break;
			case TRIPLEQUOTE2:
				if(backslash)
					backslash = false;
				else if(SyntaxUtilities.regionMatches(false,
					line,i,"'''"))
				{
					addToken(info,(i+=4) - lastOffset,
						Token.LITERAL1);
					token = Token.NULL;
					lastOffset = lastKeyword = i;
				}
				break;
			default:
				throw new InternalError("Invalid state: "
					+ token);
			}
		}

		switch(token)
		{
			case TRIPLEQUOTE1:
			case TRIPLEQUOTE2:
				addToken(info,length - lastOffset,Token.LITERAL1);
				break;
			case Token.NULL:
				doKeyword(info,line,length,'\0');
			default:
				addToken(info,length - lastOffset,token);
				break;
		}

		return token;
	}

	public static KeywordMap getKeywords()
	{
		if (pyKeywords == null)
		{
			pyKeywords = new KeywordMap(false);
			pyKeywords.add("and",Token.KEYWORD3);
			pyKeywords.add("not",Token.KEYWORD3);
			pyKeywords.add("or",Token.KEYWORD3);
			pyKeywords.add("if",Token.KEYWORD1);
			pyKeywords.add("for",Token.KEYWORD1);
			pyKeywords.add("assert",Token.KEYWORD1);
			pyKeywords.add("break",Token.KEYWORD1);
			pyKeywords.add("continue",Token.KEYWORD1);
			pyKeywords.add("elif",Token.KEYWORD1);
			pyKeywords.add("else",Token.KEYWORD1);
			pyKeywords.add("except",Token.KEYWORD1);
			pyKeywords.add("exec",Token.KEYWORD1);
			pyKeywords.add("finally",Token.KEYWORD1);
			pyKeywords.add("raise",Token.KEYWORD1);
			pyKeywords.add("return",Token.KEYWORD1);
			pyKeywords.add("try",Token.KEYWORD1);
			pyKeywords.add("while",Token.KEYWORD1);
			pyKeywords.add("def",Token.KEYWORD2);
			pyKeywords.add("class",Token.KEYWORD2);
			pyKeywords.add("del",Token.KEYWORD2);
			pyKeywords.add("from",Token.KEYWORD2);
			pyKeywords.add("global",Token.KEYWORD2);
			pyKeywords.add("import",Token.KEYWORD2);
			pyKeywords.add("in",Token.KEYWORD2);
			pyKeywords.add("is",Token.KEYWORD2);
			pyKeywords.add("lambda",Token.KEYWORD2);
			pyKeywords.add("pass",Token.KEYWORD2);
			pyKeywords.add("print",Token.KEYWORD2);
		}
		return pyKeywords;
	}

	// private members
	private static KeywordMap pyKeywords;

	private KeywordMap keywords;
	private int lastOffset;
	private int lastKeyword;

	private boolean doKeyword(LineInfo info, Segment line, int i, char c)
	{
		int i1 = i+1;

		int len = i - lastKeyword;
		byte id = keywords.lookup(line,lastKeyword,len);
		if(id != Token.NULL)
		{
			if(lastKeyword != lastOffset)
				addToken(info,lastKeyword - lastOffset,Token.NULL);
			addToken(info,len,id);
			lastOffset = i;
		}
		lastKeyword = i1;
		return false;
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.4  2000/03/20 03:42:55  sp
 * Smoother syntax package, opening an already open file will ask if it should be
 * reloaded, maybe some other changes
 *
 * Revision 1.3  1999/12/14 04:20:35  sp
 * Various updates, PHP3 mode added
 *
 * Revision 1.2  1999/10/31 07:15:34  sp
 * New logging API, splash screen updates, bug fixes
 *
 * Revision 1.1  1999/09/30 12:21:05  sp
 * No net access for a month... so here's one big jEdit 2.1pre1
 *
 *
 */
