/*
 * CTokenMarker.java - C token marker
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
 * C token marker.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class CTokenMarker extends TokenMarker
{
	public CTokenMarker()
	{
		this(true,getKeywords());
	}

	public CTokenMarker(boolean cpp, KeywordMap keywords)
	{
		this.cpp = cpp;
		this.keywords = keywords;
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
			case Token.NULL:
				switch(c)
				{
				case '#':
					if(backslash)
						backslash = false;
					else if(cpp)
					{
						if(doKeyword(line,i,c))
							break;
						addToken(i - lastOffset,token);
						addToken(length - i,Token.KEYWORD2);
						lastOffset = lastKeyword = length;
						break loop;
					}
					break;
				case '"':
					doKeyword(line,i,c);
					if(backslash)
						backslash = false;
					else
					{
						addToken(i - lastOffset,token);
						token = Token.LITERAL1;
						lastOffset = lastKeyword = i;
					}
					break;
				case '\'':
					doKeyword(line,i,c);
					if(backslash)
						backslash = false;
					else
					{
						addToken(i - lastOffset,token);
						token = Token.LITERAL2;
						lastOffset = lastKeyword = i;
					}
					break;
				case ':':
					if(lastKeyword == offset)
					{
						if(doKeyword(line,i,c))
							break;
						backslash = false;
						addToken(i1 - lastOffset,Token.LABEL);
						lastOffset = lastKeyword = i1;
					}
					else if(doKeyword(line,i,c))
						break;
					break;
				case '/':
					backslash = false;
					doKeyword(line,i,c);
					if(length - i > 1)
					{
						switch(array[i1])
						{
						case '*':
							addToken(i - lastOffset,token);
							lastOffset = lastKeyword = i;
							if(length - i > 2 && array[i+2] == '*')
								token = Token.COMMENT2;
							else
								token = Token.COMMENT1;
							break;
						case '/':
							addToken(i - lastOffset,token);
							addToken(length - i,Token.COMMENT1);
							lastOffset = lastKeyword = length;
							break loop;
						}
					}
					break;
				default:
					backslash = false;
					if(!Character.isLetterOrDigit(c)
						&& c != '_')
						doKeyword(line,i,c);
					break;
				}
				break;
			case Token.COMMENT1:
			case Token.COMMENT2:
				backslash = false;
				if(c == '*' && length - i > 1)
				{
					if(array[i1] == '/')
					{
						i++;
						addToken((i+1) - lastOffset,token);
						token = Token.NULL;
						lastOffset = lastKeyword = i+1;
					}
				}
				break;
			case Token.LITERAL1:
				if(backslash)
					backslash = false;
				else if(c == '"')
				{
					addToken(i1 - lastOffset,token);
					token = Token.NULL;
					lastOffset = lastKeyword = i1;
				}
				break;
			case Token.LITERAL2:
				if(backslash)
					backslash = false;
				else if(c == '\'')
				{
					addToken(i1 - lastOffset,Token.LITERAL1);
					token = Token.NULL;
					lastOffset = lastKeyword = i1;
				}
				break;
			default:
				throw new InternalError("Invalid state: "
					+ token);
			}
		}

		if(token == Token.NULL)
			doKeyword(line,length,'\0');

		switch(token)
		{
		case Token.LITERAL1:
		case Token.LITERAL2:
			addToken(length - lastOffset,Token.INVALID);
			token = Token.NULL;
			break;
		case Token.KEYWORD2:
			addToken(length - lastOffset,token);
			if(!backslash)
				token = Token.NULL;
		default:
			addToken(length - lastOffset,token);
			break;
		}

		return token;
	}

	public static KeywordMap getKeywords()
	{
		if(cKeywords == null)
		{
			cKeywords = new KeywordMap(false);
			cKeywords.add("char",Token.KEYWORD3);
			cKeywords.add("double",Token.KEYWORD3);
			cKeywords.add("enum",Token.KEYWORD3);
			cKeywords.add("float",Token.KEYWORD3);
			cKeywords.add("int",Token.KEYWORD3);
			cKeywords.add("long",Token.KEYWORD3);
			cKeywords.add("short",Token.KEYWORD3);
			cKeywords.add("signed",Token.KEYWORD3);
			cKeywords.add("struct",Token.KEYWORD3);
			cKeywords.add("typedef",Token.KEYWORD3);
			cKeywords.add("union",Token.KEYWORD3);
			cKeywords.add("unsigned",Token.KEYWORD3);
			cKeywords.add("void",Token.KEYWORD3);
			cKeywords.add("auto",Token.KEYWORD1);
			cKeywords.add("const",Token.KEYWORD1);
			cKeywords.add("extern",Token.KEYWORD1);
			cKeywords.add("register",Token.KEYWORD1);
			cKeywords.add("static",Token.KEYWORD1);
			cKeywords.add("volatile",Token.KEYWORD1);
			cKeywords.add("break",Token.KEYWORD1);
			cKeywords.add("case",Token.KEYWORD1);
			cKeywords.add("continue",Token.KEYWORD1);
			cKeywords.add("default",Token.KEYWORD1);
			cKeywords.add("do",Token.KEYWORD1);
			cKeywords.add("else",Token.KEYWORD1);
			cKeywords.add("for",Token.KEYWORD1);
			cKeywords.add("goto",Token.KEYWORD1);
			cKeywords.add("if",Token.KEYWORD1);
			cKeywords.add("return",Token.KEYWORD1);
			cKeywords.add("sizeof",Token.KEYWORD1);
			cKeywords.add("switch",Token.KEYWORD1);
			cKeywords.add("while",Token.KEYWORD1);
			cKeywords.add("asm",Token.KEYWORD2);
			cKeywords.add("asmlinkage",Token.KEYWORD2);
			cKeywords.add("far",Token.KEYWORD2);
			cKeywords.add("huge",Token.KEYWORD2);
			cKeywords.add("inline",Token.KEYWORD2);
			cKeywords.add("near",Token.KEYWORD2);
			cKeywords.add("pascal",Token.KEYWORD2);
			cKeywords.add("true",Token.LITERAL2);
			cKeywords.add("false",Token.LITERAL2);
			cKeywords.add("NULL",Token.LITERAL2);
		}
		return cKeywords;
	}

	// private members
	private static KeywordMap cKeywords;

	private boolean cpp;
	private KeywordMap keywords;
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
 * Revision 1.33  1999/10/31 07:15:34  sp
 * New logging API, splash screen updates, bug fixes
 *
 * Revision 1.32  1999/09/30 12:21:04  sp
 * No net access for a month... so here's one big jEdit 2.1pre1
 *
 * Revision 1.31  1999/08/21 01:48:18  sp
 * jEdit 2.0pre8
 *
 * Revision 1.30  1999/06/05 00:22:58  sp
 * LGPL'd syntax package
 *
 * Revision 1.29  1999/06/03 08:24:13  sp
 * Fixing broken CVS
 *
 * Revision 1.30  1999/05/31 08:11:10  sp
 * Syntax coloring updates, expand abbrev bug fix
 *
 * Revision 1.29  1999/05/31 04:38:51  sp
 * Syntax optimizations, HyperSearch for Selection added (Mike Dillon)
 *
 * Revision 1.28  1999/05/29 03:46:53  sp
 * CTokenMarker bug fix, new splash screen
 *
 * Revision 1.27  1999/05/14 04:56:15  sp
 * Docs updated, default: fix in C/C++/Java mode, full path in title bar toggle
 *
 * Revision 1.26  1999/05/11 09:05:10  sp
 * New version1.6.html file, some other stuff perhaps
 *
 * Revision 1.25  1999/04/22 06:03:26  sp
 * Syntax colorizing change
 *
 * Revision 1.24  1999/04/19 05:38:20  sp
 * Syntax API changes
 *
 * Revision 1.23  1999/03/13 08:50:39  sp
 * Syntax colorizing updates and cleanups, general code reorganizations
 *
 * Revision 1.22  1999/03/13 00:09:07  sp
 * Console updates, uncomment removed cos it's too buggy, cvs log tags added
 *
 * Revision 1.21  1999/03/12 23:51:00  sp
 * Console updates, uncomment removed cos it's too buggy, cvs log tags added
 *
 */
