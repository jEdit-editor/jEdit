/*
 * HTMLTokenMarker.java - HTML token marker
 * Copyright (C) 1998, 1999 Slava Pestov
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA.
 */
package org.gjt.sp.jedit.syntax;

import javax.swing.text.Segment;

/**
 * HTML token marker.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class HTMLTokenMarker extends TokenMarker
{
	public static final byte JAVASCRIPT = Token.INTERNAL_FIRST;

	public HTMLTokenMarker()
	{
		this(true);
	}

	public HTMLTokenMarker(boolean js)
	{
		this.js = js;
		keywords = JavaScriptTokenMarker.getKeywords();
	}

	public byte markTokensImpl(byte token, Segment line, int lineIndex)
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
			case Token.NULL: // HTML text
				backslash = false;
				switch(c)
				{
				case '<':
					addToken(i - lastOffset,token);
					lastOffset = lastKeyword = i;
					if(SyntaxUtilities.regionMatches(false,
						line,i1,"!--"))
					{
						i += 3;
						token = Token.COMMENT1;
					}
					else if(js && SyntaxUtilities.regionMatches(
						true,line,i1,"script>"))
					{
						addToken(8,Token.KEYWORD1);
						lastOffset = lastKeyword = (i += 8);
						token = JAVASCRIPT;
					}
					else
					{
						token = Token.KEYWORD1;
					}
					break;
				case '&':
					addToken(i - lastOffset,token);
					lastOffset = lastKeyword = i;
					token = Token.KEYWORD2;
					break;
				}
				break;
			case Token.KEYWORD1: // Inside a tag
				backslash = false;
				if(c == '>')
				{
					addToken(i1 - lastOffset,token);
					lastOffset = lastKeyword = i1;
					token = Token.NULL;
				}
				break;
			case Token.KEYWORD2: // Inside an entity
				backslash = false;
				if(c == ';')
				{
					addToken(i1 - lastOffset,token);
					lastOffset = lastKeyword = i1;
					token = Token.NULL;
					break;
				}
				break;
			case Token.COMMENT1: // Inside a comment
				backslash = false;
				if(SyntaxUtilities.regionMatches(false,line,i,"-->"))
				{
					addToken((i += 3) - lastOffset,token);
					lastOffset = lastKeyword = i;
					token = Token.NULL;
				}
				break;
			case JAVASCRIPT: // Inside a JavaScript
				switch(c)
				{
				case '<':
					backslash = false;
					doKeyword(line,i,c);
					if(SyntaxUtilities.regionMatches(true,
						line,i1,"/script>"))
					{
						addToken(i - lastOffset,
							Token.NULL);
						addToken(9,Token.KEYWORD1);
						lastOffset = lastKeyword = (i += 9);
						token = Token.NULL;
					}
					break;
				case '"':
					if(backslash)
						backslash = false;
					else
					{
						doKeyword(line,i,c);
						addToken(i - lastOffset,Token.NULL);
						lastOffset = lastKeyword = i;
						token = Token.LITERAL1;
					}
					break;
				case '\'':
					if(backslash)
						backslash = false;
					else
					{
						doKeyword(line,i,c);
						addToken(i - lastOffset,Token.NULL);
						lastOffset = lastKeyword = i;
						token = Token.LITERAL2;
					}
					break;
				case '/':
					backslash = false;
					doKeyword(line,i,c);
					if(length - i > 1)
					{
						addToken(i - lastOffset,Token.NULL);
						lastOffset = lastKeyword = i;
						if(array[i1] == '/')
						{
							addToken(length - i,Token.COMMENT2);
							lastOffset = lastKeyword = length;
							break loop;
						}
						else if(array[i1] == '*')
						{
							token = Token.COMMENT2;
						}
					}
					break;
				default:					backslash = false;
					if(!Character.isLetterOrDigit(c)
						&& c != '_')
						doKeyword(line,i,c);
					break;
				}
				break;
			case Token.LITERAL1: // JavaScript "..."
				if(backslash)
					backslash = false;
				else if(c == '"')
				{
					addToken(i1 - lastOffset,Token.LITERAL1);
					lastOffset = lastKeyword = i1;
					token = JAVASCRIPT;
				}
				break;
			case Token.LITERAL2: // JavaScript '...'
				if(backslash)
					backslash = false;
				else if(c == '\'')
				{
					addToken(i1 - lastOffset,Token.LITERAL1);
					lastOffset = lastKeyword = i1;
					token = JAVASCRIPT;
				}
				break;
			case Token.COMMENT2: // Inside a JavaScript comment
				backslash = false;
				if(c == '*' && length - i > 1 && array[i1] == '/')
				{
					addToken((i+=2) - lastOffset,Token.COMMENT2);
					lastOffset = lastKeyword = i;
					token = JAVASCRIPT;
				}
				break;
			default:
				throw new InternalError("Invalid state: "
					+ token);
			}
		}

		switch(token)
		{
		case Token.LITERAL1:
		case Token.LITERAL2:
			addToken(length - lastOffset,Token.INVALID);
			token = JAVASCRIPT;
			break;
		case Token.KEYWORD2:
			addToken(length - lastOffset,Token.INVALID);
			token = Token.NULL;
			break;
		case JAVASCRIPT:
			doKeyword(line,length,'\0');
			addToken(length - lastOffset,Token.NULL);
			break;
		default:
			addToken(length - lastOffset,token);
			break;
		}

		return token;
	}

	// private members
	private KeywordMap keywords;
	private boolean js;
	private int lastOffset;
	private int lastKeyword;

	private boolean doKeyword(Segment line, int i, char c)
	{
		int i1 = i+1;

		int len = i - lastKeyword;
		byte id = keywords.lookup(line,lastKeyword,len);
		if(id != Token.NULL)
		{
			if(lastKeyword != lastOffset)
				addToken(lastKeyword - lastOffset,Token.NULL);
			addToken(len,id);
			lastOffset = i;
		}
		lastKeyword = i1;
		return false;
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.31  1999/10/23 03:48:22  sp
 * Mode system overhaul, close all dialog box, misc other stuff
 *
 * Revision 1.30  1999/09/30 12:21:05  sp
 * No net access for a month... so here's one big jEdit 2.1pre1
 *
 * Revision 1.29  1999/07/16 23:45:49  sp
 * 1.7pre6 BugFree version
 *
 * Revision 1.28  1999/06/05 00:22:58  sp
 * LGPL'd syntax package
 *
 * Revision 1.27  1999/06/03 08:24:13  sp
 * Fixing broken CVS
 *
 * Revision 1.28  1999/05/31 08:11:10  sp
 * Syntax coloring updates, expand abbrev bug fix
 *
 * Revision 1.27  1999/05/31 04:38:51  sp
 * Syntax optimizations, HyperSearch for Selection added (Mike Dillon)
 *
 * Revision 1.26  1999/05/14 04:56:15  sp
 * Docs updated, default: fix in C/C++/Java mode, full path in title bar toggle
 *
 * Revision 1.25  1999/05/11 09:05:10  sp
 * New version1.6.html file, some other stuff perhaps
 *
 * Revision 1.24  1999/05/03 04:28:01  sp
 * Syntax colorizing bug fixing, console bug fix for Swing 1.1.1
 *
 * Revision 1.23  1999/04/22 06:03:26  sp
 * Syntax colorizing change
 *
 * Revision 1.22  1999/04/19 05:38:20  sp
 * Syntax API changes
 *
 * Revision 1.21  1999/03/13 08:50:39  sp
 * Syntax colorizing updates and cleanups, general code reorganizations
 *
 * Revision 1.20  1999/03/12 23:51:00  sp
 * Console updates, uncomment removed cos it's too buggy, cvs log tags added
 *
 */
