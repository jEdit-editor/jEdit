/*
 * NullTokenMarker.java - Returns Token.NULL
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
package org.gjt.sp.jedit.syntax;

import javax.swing.text.*;
import java.util.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

/**
 * Set a document's token marker to this to disable syntax highlighting.
 *
 * @since 2.6pre1
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class NullTokenMarker extends TokenMarker
{
	public LineInfo markTokens(Buffer buffer, int lineIndex)
	{
		Element lineElement = buffer.getDefaultRootElement()
			.getElement(lineIndex);
		nullLineInfo.lastToken = null;
		addToken(nullLineInfo,lineElement.getEndOffset()
			- lineElement.getStartOffset() - 1,Token.NULL);
		addToken(nullLineInfo,0,Token.END);
		return nullLineInfo;
	}

	public LineInfo markTokens(Segment line, int lineIndex)
	{
		nullLineInfo.lastToken = null;
		addToken(nullLineInfo,line.count,Token.NULL);
		addToken(nullLineInfo,0,Token.END);
		return nullLineInfo;
	}

	/**
	 * Does nothing.
	 */
	public void insertLines(int index, int len) {}

	/**
	 * Does nothing.
	 */
	public void deleteLines(int index, int len) {}

	/**
	 * Does nothing.
	 */
	public void linesChanged(int index, int len) {}

	public Object clone()
	{
		return this;
	}

	/**
	 * Returns the global instance of the NullTokenMarker. Since
	 * it carries no state, all users can share this one instance.
	 */
	public static NullTokenMarker getSharedInstance()
	{
		return sharedInstance;
	}

	// private members
	private static NullTokenMarker sharedInstance = new NullTokenMarker();
	private LineInfo nullLineInfo = new LineInfo();

	private NullTokenMarker() {}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.3  2000/10/12 09:28:27  sp
 * debugging and polish
 *
 * Revision 1.2  2000/07/26 07:48:45  sp
 * stuff
 *
 * Revision 1.1  2000/07/14 06:00:45  sp
 * bracket matching now takes syntax info into account
 *
 */
