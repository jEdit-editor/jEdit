/*
 * PostScriptTokenMarker.java - PostScript token marker
 * Copyright (C) 2000 Ralf Engels engels@arcormail.de
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
 * PostScript token marker.
 *
 * @author Ralf Engels
 * @version $Id$
 */
public class PostScriptTokenMarker extends TokenMarker
{
	public PostScriptTokenMarker()
	{
		this(getKeywords());
	}

        public PostScriptTokenMarker( KeywordMap keywords)
	{
	    this.keywords = keywords;
	}

	public byte markTokensImpl(byte token, Segment line, int lineIndex)
	{
		char[] array = line.array;
		int offset   = line.offset;
		lastOffset   = offset;
		lastKeyword  = offset;
		int length   = line.count + offset;
		boolean backslash = false;

loop:		for(int i = offset; i < length; i++)
		{
			int  i1 = (i+1);
			char c  = array[i];
			char c1 = ' ';
			if( i1<array.length )
			    c1 = array[i1];

			if(c == '\\')
			    {
                                backslash = !backslash;
                                continue;
			    }
			else 
			    backslash = false;


			switch(token)
			{
			case Token.NULL:
				switch(c)
				{
				case '(':  // string
				    doKeyword(line,i,c);
				    {
					addToken(i - lastOffset,token);
					token = Token.LITERAL1;
					lastOffset = lastKeyword = i;
					braceCount = 1;
				    }
				    break;
				case '<':
				    doKeyword(line,i,c);

				    addToken(i - lastOffset,token);
				    token = Token.LITERAL2;
				    lastOffset = lastKeyword = i;
				    break;
				case '/':
				    doKeyword(line,i,c);

				    addToken(i - lastOffset, token );
				    token = Token.LABEL;
				    lastOffset = lastKeyword = i;
				    break;
				case '%': // read comment 
				    // (there are only one line comments in postscript, so there are no problems)
				    doKeyword(line,i,c);
				    if(length - i > 1)
					{
					    switch(array[i1])
						{
						case '%':
						case '?':
						case '!':
						    addToken(i - lastOffset,token);
						    addToken(length - i,Token.COMMENT2);
						    lastOffset = lastKeyword = length;
						    break loop;
						default:
						    addToken(i - lastOffset,token);
						    addToken(length - i,Token.COMMENT1);
						    lastOffset = lastKeyword = length;
						    break loop;
						}
					}
				    break;
				default:
				    if( Character.isWhitespace(c) || 
					c1=='[' || c1==']' || c1=='{' || c1=='}' )
					doKeyword(line,i,c);
				    break;
				}
				break;
			case Token.LITERAL1: // in string
			    switch( c )
				{
				case ')':
				    if( !backslash )
					{
					    braceCount--;
					    if( braceCount <= 0 )
						{
						    addToken(i1 - lastOffset,token);
						    token = Token.NULL;
						    lastOffset = lastKeyword = i1;
						}
					}
				    break;
				case '(':
				    if( !backslash )
					braceCount++;
				    break;
				default:
				}
			    backslash = false;
			    break;

			case Token.LITERAL2:
			    if( c == '>' )
				{
				    addToken(i1 - lastOffset,Token.LITERAL1);
				    token = Token.NULL;
				    lastOffset = lastKeyword = i1;
				}
			    break;
			case Token.LABEL:
			    if( Character.isWhitespace(c1) || 
				c1=='[' || c1==']' || c1=='{' || c1=='}' )
				{
				    addToken(i1 - lastOffset,token);
				    // doKeyword(line,i,c);
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

		addToken(length - lastOffset,token);
		
		return token;
	}

	public static KeywordMap getKeywords()
	{
		if(psKeywords == null)
		{
			psKeywords = new KeywordMap(false);

			psKeywords.add("pop",Token.KEYWORD1);
			psKeywords.add("exch",Token.KEYWORD1);
			psKeywords.add("dup",Token.KEYWORD1);
			psKeywords.add("copy",Token.KEYWORD1);
			psKeywords.add("roll",Token.KEYWORD1);
			psKeywords.add("clear",Token.KEYWORD1);
			psKeywords.add("count",Token.KEYWORD1);
			psKeywords.add("mark",Token.KEYWORD1);
			psKeywords.add("cleartomark",Token.KEYWORD1);
			psKeywords.add("counttomark",Token.KEYWORD1);

			psKeywords.add("exec",Token.KEYWORD1);
			psKeywords.add("if",Token.KEYWORD1);
			psKeywords.add("ifelse",Token.KEYWORD1);
			psKeywords.add("for",Token.KEYWORD1);
			psKeywords.add("repeat",Token.KEYWORD1);
			psKeywords.add("loop",Token.KEYWORD1);
			psKeywords.add("exit",Token.KEYWORD1);
			psKeywords.add("stop",Token.KEYWORD1);
			psKeywords.add("stopped",Token.KEYWORD1);
			psKeywords.add("countexecstack",Token.KEYWORD1);
			psKeywords.add("execstack",Token.KEYWORD1);
			psKeywords.add("quit",Token.KEYWORD1);
			psKeywords.add("start",Token.KEYWORD1);

			psKeywords.add("add",Token.OPERATOR);
			psKeywords.add("div",Token.OPERATOR);
			psKeywords.add("idiv",Token.OPERATOR);
			psKeywords.add("mod",Token.OPERATOR);
			psKeywords.add("mul",Token.OPERATOR);
			psKeywords.add("sub",Token.OPERATOR);
			psKeywords.add("abs",Token.OPERATOR);
			psKeywords.add("ned",Token.OPERATOR);
			psKeywords.add("ceiling",Token.OPERATOR);
			psKeywords.add("floor",Token.OPERATOR);
			psKeywords.add("round",Token.OPERATOR);
			psKeywords.add("truncate",Token.OPERATOR);
			psKeywords.add("sqrt",Token.OPERATOR);
			psKeywords.add("atan",Token.OPERATOR);
			psKeywords.add("cos",Token.OPERATOR);
			psKeywords.add("sin",Token.OPERATOR);
			psKeywords.add("exp",Token.OPERATOR);
			psKeywords.add("ln",Token.OPERATOR);
			psKeywords.add("log",Token.OPERATOR);
			psKeywords.add("rand",Token.OPERATOR);
			psKeywords.add("srand",Token.OPERATOR);
			psKeywords.add("rrand",Token.OPERATOR);

			psKeywords.add("true",Token.LITERAL2);
			psKeywords.add("false",Token.LITERAL2);
			psKeywords.add("NULL",Token.LITERAL2);
		}
		return psKeywords;
	}

	// private members
	private static KeywordMap psKeywords;

	private KeywordMap keywords;
	private int lastOffset;
	private int lastKeyword;

	/** number of open braces (for string) */
	private int braceCount; 

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
 * Revision 1.1  2000/01/22 22:25:08  sp
 * PostScript edit mode, other misc updates
 *
 */
