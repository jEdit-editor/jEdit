/*
 * JavaScriptTokenMarker.java - JavaScript token marker
 * Copyright (C) 1999 Slava Pestov
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
 * JavaScript token marker.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class JavaScriptTokenMarker extends CTokenMarker
{
	public JavaScriptTokenMarker()
	{
		super(false,getKeywords());
	}

	public static KeywordMap getKeywords()
	{
		if(javaScriptKeywords == null)
		{
			javaScriptKeywords = new KeywordMap(false);
			javaScriptKeywords.add("function",Token.KEYWORD3);
			javaScriptKeywords.add("var",Token.KEYWORD3);
			javaScriptKeywords.add("else",Token.KEYWORD1);
			javaScriptKeywords.add("for",Token.KEYWORD1);
			javaScriptKeywords.add("if",Token.KEYWORD1);
			javaScriptKeywords.add("in",Token.KEYWORD1);
			javaScriptKeywords.add("new",Token.KEYWORD1);
			javaScriptKeywords.add("return",Token.KEYWORD1);
			javaScriptKeywords.add("while",Token.KEYWORD1);
			javaScriptKeywords.add("with",Token.KEYWORD1);
			javaScriptKeywords.add("break",Token.KEYWORD1);
			javaScriptKeywords.add("case",Token.KEYWORD1);
			javaScriptKeywords.add("continue",Token.KEYWORD1);
			javaScriptKeywords.add("default",Token.KEYWORD1);
			javaScriptKeywords.add("false",Token.LABEL);
			javaScriptKeywords.add("this",Token.LABEL);
			javaScriptKeywords.add("true",Token.LABEL);
		}
		return javaScriptKeywords;
	}

	// private members
	private static KeywordMap javaScriptKeywords;
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.3  1999/12/13 03:40:29  sp
 * Bug fixes, syntax is now mostly GPL'd
 *
 * Revision 1.2  1999/06/05 00:22:58  sp
 * LGPL'd syntax package
 *
 * Revision 1.1  1999/03/13 09:11:46  sp
 * Syntax code updates, code cleanups
 *
 */
