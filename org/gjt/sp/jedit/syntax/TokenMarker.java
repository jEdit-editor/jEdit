/*
 * TokenMarker.java - Base token marker class
 * Copyright (C) 1998, 1999, 2000 Slava Pestov
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
import java.util.*;

/**
 * A token marker that splits lines of text into tokens. Each token carries
 * a length field and an indentification tag that can be mapped to a color
 * for painting that token.<p>
 *
 * For performance reasons, the linked list of tokens is reused after each
 * line is tokenized. Therefore, the return value of <code>markTokens</code>
 * should only be used for immediate painting. Notably, it cannot be
 * cached.
 *
 * @author Slava Pestov
 * @version $Id$
 *
 * @see org.gjt.sp.jedit.syntax.Token
 */
public abstract class TokenMarker
{
	/**
	 * A wrapper for the lower-level <code>markTokensImpl</code> method
	 * that is called to split a line up into tokens.
	 * @param line The line
	 * @param lineIndex The line number
	 */
	public Token markTokens(Segment line, int lineIndex)
	{
		if(lineIndex >= length)
		{
			throw new IllegalArgumentException("Tokenizing invalid line: "
				+ lineIndex);
		}

		LineInfo info = lineInfo[lineIndex];

		/* If cached tokens are valid, return 'em */
		if(info.tokensValid)
			return info.firstToken;

		/* Otherwise, prepare for tokenization */
		info.lastToken = null;

		LineInfo prev;
		if(lineIndex == 0)
			prev = null;
		else
			prev = lineInfo[lineIndex - 1];

		byte oldToken = info.token;
		byte token = markTokensImpl(prev == null ?
			Token.NULL : prev.token,line,lineIndex,info);

		info.token = token;
		info.tokensValid = true;

		nextLineRequested = (oldToken != token);
		if(nextLineRequested && length - lineIndex > 1)
		{
			lineInfo[lineIndex + 1].tokensValid = false;
		}

		addToken(info,0,Token.END);

		return info.firstToken;
	}

	/**
	 * An abstract method that splits a line up into tokens. It
	 * should parse the line, and call <code>addToken(info,)</code> to
	 * add syntax tokens to the token list. Then, it should return
	 * the initial token type for the next line.<p>
	 *
	 * For example if the current line contains the start of a 
	 * multiline comment that doesn't end on that line, this method
	 * should return the comment token type so that it continues on
	 * the next line.
	 *
	 * @param token The initial token type for this line
	 * @param line The line to be tokenized
	 * @param lineIndex The index of the line in the document,
	 * starting at 0
	 * @param info The LineInfo object for this line
	 * @return The initial token type for the next line
	 */
	protected abstract byte markTokensImpl(byte token, Segment line,
		int lineIndex, LineInfo info);

	/**
	 * Returns if the token marker supports tokens that span multiple
	 * lines. If this is true, the object using this token marker is
	 * required to pass all lines in the document to the
	 * <code>markTokens()</code> method (in turn).<p>
	 *
	 * The default implementation returns true; it should be overridden
	 * to return false on simpler token markers for increased speed.
	 */
	public boolean supportsMultilineTokens()
	{
		return true;
	}

	/**
	 * Informs the token marker that lines have been inserted into
	 * the document. This inserts a gap in the <code>lineInfo</code>
	 * array.
	 * @param index The first line number
	 * @param lines The number of lines 
	 */
	public void insertLines(int index, int lines)
	{
		if(lines <= 0)
			return;
		length += lines;
		ensureCapacity(length);
		int len = index + lines;
		System.arraycopy(lineInfo,index,lineInfo,len,
			lineInfo.length - len);

		for(int i = index + lines - 1; i >= index; i--)
		{
			lineInfo[i] = new LineInfo();
		}
	}
	
	/**
	 * Informs the token marker that line have been deleted from
	 * the document. This removes the lines in question from the
	 * <code>lineInfo</code> array.
	 * @param index The first line number
	 * @param lines The number of lines
	 */
	public void deleteLines(int index, int lines)
	{
		if (lines <= 0)
			return;
		int len = index + lines;
		length -= lines;
		System.arraycopy(lineInfo,len,lineInfo,
			index,lineInfo.length - len);
	}

	/**
	 * Informs the token marker that lines have changed. This will
	 * invalidate any cached tokens.
	 * @param index The first line number
	 * @param lines The number of lines
	 */
	public void linesChanged(int index, int lines)
	{
		for(int i = 0; i < lines; i++)
		{
			lineInfo[index + i].tokensValid = false;
		}
	}

	/**
	 * Returns the number of lines in this token marker.
	 */
	public int getLineCount()
	{
		return length;
	}

	/**
	 * Returns true if the next line should be repainted. This
	 * will return true after a line has been tokenized that starts
	 * a multiline token that continues onto the next line.
	 */
	public boolean isNextLineRequested()
	{
		return nextLineRequested;
	}

	// protected members

	/**
	 * An array for storing information about lines. It is enlarged and
	 * shrunk automatically by the <code>insertLines()</code> and
	 * <code>deleteLines()</code> methods.
	 */
	protected LineInfo[] lineInfo;

	/**
	 * The number of lines in the model being tokenized. This can be
	 * less than the length of the <code>lineInfo</code> array.
	 */
	protected int length;

	/**
	 * True if the next line should be painted.
	 */
	protected boolean nextLineRequested;

	/**
	 * Ensures that the <code>lineInfo</code> array can contain the
	 * specified index. This enlarges it if necessary. No action is
	 * taken if the array is large enough already.<p>
	 *
	 * It should be unnecessary to call this under normal
	 * circumstances; <code>insertLine()</code> should take care of
	 * enlarging the line info array automatically.
	 *
	 * @param index The array index
	 */
	protected void ensureCapacity(int index)
	{
		if(lineInfo == null)
			lineInfo = new LineInfo[index + 1];
		else if(lineInfo.length <= index)
		{
			LineInfo[] lineInfoN = new LineInfo[(index + 1) * 2];
			System.arraycopy(lineInfo,0,lineInfoN,0,
					 lineInfo.length);
			lineInfo = lineInfoN;
		}
	}

	/**
	 * Adds a token to the token list.
	 * @param info The line to add the token to
	 * @param length The length of the token
	 * @param id The id of the token
	 */
	protected void addToken(LineInfo info, int length, byte id)
	{
		if(id >= Token.INTERNAL_FIRST && id <= Token.INTERNAL_LAST)
			throw new InternalError("Invalid id: " + id);

		if(length == 0 && id != Token.END)
			return;

		if(info.firstToken == null)
		{
			info.firstToken = new Token(length,id);
			info.lastToken = info.firstToken;
		}
		else if(info.lastToken == null)
		{
			info.lastToken = info.firstToken;
			info.firstToken.length = length;
			info.firstToken.id = id;
		}
		else if(info.lastToken.id == id)
		{
			info.lastToken.length += length;
		}
		else if(info.lastToken.next == null)
		{
			info.lastToken.next = new Token(length,id);
			info.lastToken = info.lastToken.next;
		}
		else
		{
			info.lastToken = info.lastToken.next;
			info.lastToken.length = length;
			info.lastToken.id = id;
		}
	}

	/**
	 * Inner class for storing information about tokenized lines.
	 */
	public class LineInfo
	{
		/**
		 * Creates a new LineInfo object with token = Token.NULL
		 * and obj = null.
		 */
		public LineInfo()
		{
		}

		/**
		 * The id of the last token of the line.
		 */
		public byte token;

		/**
		 * The first token of this line.
		 */
		public Token firstToken;

		/**
		 * The last token of this line.
		 */
		public Token lastToken;

		/**
		 * True if the tokens can be used, false if markTokensImpl()
		 * needs to be called.
		 */
		public boolean tokensValid;

		/**
		 * This is for use by the token marker implementations
		 * themselves. It can be used to store anything that
		 * is an object and that needs to exist on a per-line
		 * basis.
		 */
		public Object obj;
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.33  2000/03/20 03:42:55  sp
 * Smoother syntax package, opening an already open file will ask if it should be
 * reloaded, maybe some other changes
 *
 * Revision 1.32  1999/12/13 03:40:30  sp
 * Bug fixes, syntax is now mostly GPL'd
 *
 * Revision 1.31  1999/12/10 03:22:47  sp
 * Bug fixes, old loading code is now used again
 *
 * Revision 1.30  1999/12/07 07:27:59  sp
 * TokenMarker.reset() fixed
 *
 * Revision 1.29  1999/12/07 07:19:36  sp
 * Buffer loading code cleaned up
 *
 * Revision 1.28  1999/07/29 08:50:21  sp
 * Misc stuff for 1.7pre7
 *
 * Revision 1.27  1999/07/16 23:45:49  sp
 * 1.7pre6 BugFree version
 *
 * Revision 1.26  1999/07/05 04:38:39  sp
 * Massive batch of changes... bug fixes, also new text component is in place.
 * Have fun
 *
 * Revision 1.25  1999/06/06 05:05:25  sp
 * Search and replace tweaks, Perl/Shell Script mode updates
 *
 * Revision 1.24  1999/06/05 00:22:58  sp
 * LGPL'd syntax package
 *
 * Revision 1.23  1999/05/03 08:28:14  sp
 * Documentation updates, key binding editor, syntax text area bug fix
 *
 */
