/*
 * SyntaxDocument.java - Document that can be tokenized
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
package org.gjt.sp.jedit.syntax;

import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.UndoableEdit;

/**
 * A document implementation that can be tokenized by the syntax highlighting
 * system.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class SyntaxDocument extends PlainDocument
{
	/**
	 * Returns the token marker that is to be used to split lines
	 * of this document up into tokens. May return null.
	 */
	public TokenMarker getTokenMarker()
	{
		return tokenMarker;
	}

	/**
	 * Sets the token marker that is to be used to split lines of
	 * this document up into tokens.
	 * @param tm The new token marker
	 */
	public void setTokenMarker(TokenMarker tm)
	{
		tokenMarker = tm;
		if(tm == null)
			return;
		tokenMarker.insertLines(0,getDefaultRootElement()
			.getElementCount());
		tokenizeLines();
	}

	/**
	 * Reparses the document, by passing all lines to the token
	 * marker. This should be called after the document is first
	 * loaded.
	 */
	public void tokenizeLines()
	{
		long start = System.currentTimeMillis();
		tokenizeLines(0,getDefaultRootElement().getElementCount());
	}

	/**
	 * Reparses the document, by passing the specified lines to the
	 * token marker. This should be called after a large quantity of
	 * text is first inserted.
	 * @param start The first line to parse
	 * @param len The number of lines, after the first one to parse
	 */
	public void tokenizeLines(int start, int len)
	{
		if(tokenMarker == null)
			return;

		tokenMarker.linesChanged(start,len);

		Segment lineSegment = new Segment();
		Element map = getDefaultRootElement();

		len += start;

		try
		{
			for(int i = start; i < len; i++)
			{
				Element lineElement = map.getElement(i);
				int lineStart = lineElement.getStartOffset();
				getText(lineStart,lineElement.getEndOffset()
					- lineStart - 1,lineSegment);
				tokenMarker.markTokens(lineSegment,i);
			}
		}
		catch(BadLocationException bl)
		{
			bl.printStackTrace();
		}
	}

	/**
	 * Starts a compound edit that can be undone in one operation.
	 * Subclasses that implement undo should override this method;
	 * this class has no undo functionality so this method is
	 * empty.
	 */
	public void beginCompoundEdit() {}

	/**
	 * Ends a compound edit that can be undone in one operation.
	 * Subclasses that implement undo should override this method;
	 * this class has no undo functionality so this method is
	 * empty.
	 */
	public void endCompoundEdit() {}

	/**
	 * Adds an undoable edit to this document's undo list. The edit
	 * should be ignored if something is currently being undone.
	 * @param edit The undoable edit
	 *
	 * @since jEdit 2.2pre1
	 */
	public void addUndoableEdit(UndoableEdit edit) {}

	// protected members
	protected TokenMarker tokenMarker;

	/**
	 * We overwrite this method to update the token marker
	 * state immediately so that any event listeners get a
	 * consistent token marker.
	 */
	protected void fireInsertUpdate(DocumentEvent evt)
	{
		if(tokenMarker != null)
		{
			DocumentEvent.ElementChange ch = evt.getChange(
				getDefaultRootElement());
			if(ch != null)
			{
				int index = ch.getIndex();
				int len = ch.getChildrenAdded().length -
					ch.getChildrenRemoved().length;
				tokenMarker.linesChanged(index,
					tokenMarker.getLineCount() - index);
				tokenMarker.insertLines(ch.getIndex() + 1,len);
				index += (len + 1);
			}
			else
			{
				tokenMarker.linesChanged(getDefaultRootElement()
					.getElementIndex(evt.getOffset()),1);
			}
		}

		super.fireInsertUpdate(evt);
	}
	
	/**
	 * We overwrite this method to update the token marker
	 * state immediately so that any event listeners get a
	 * consistent token marker.
	 */
	protected void fireRemoveUpdate(DocumentEvent evt)
	{
		if(tokenMarker != null)
		{
			DocumentEvent.ElementChange ch = evt.getChange(
				getDefaultRootElement());
			if(ch != null)
			{
				int index = ch.getIndex();
				int len = ch.getChildrenRemoved().length -
					ch.getChildrenAdded().length;
				tokenMarker.linesChanged(index,
					tokenMarker.getLineCount() - index);
				tokenMarker.deleteLines(index + 1,len);
			}
			else
			{
				tokenMarker.linesChanged(getDefaultRootElement()
					.getElementIndex(evt.getOffset()),1);
			}
		}

		super.fireRemoveUpdate(evt);
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.24  2000/04/17 06:34:24  sp
 * More focus debugging, linesChanged() tweaked
 *
 * Revision 1.23  2000/04/16 08:56:24  sp
 * Option pane updates
 *
 * Revision 1.22  2000/04/10 08:46:16  sp
 * Autosave recovery support, documentation updates
 *
 * Revision 1.21  2000/04/08 06:57:14  sp
 * Parser rules are now hashed; this dramatically speeds up tokenization
 *
 * Revision 1.20  2000/04/07 06:57:26  sp
 * Buffer options dialog box updates, API docs updated a bit in syntax package
 *
 * Revision 1.19  2000/04/02 06:38:28  sp
 * Bug fixes
 *
 * Revision 1.18  2000/04/01 08:40:55  sp
 * Streamlined syntax highlighting, Perl mode rewritten in XML
 *
 * Revision 1.17  2000/03/24 04:52:17  sp
 * bug fixing
 *
 * Revision 1.16  2000/03/21 07:18:53  sp
 * bug fixes
 *
 * Revision 1.15  2000/03/20 03:42:55  sp
 * Smoother syntax package, opening an already open file will ask if it should be
 * reloaded, maybe some other changes
 *
 * Revision 1.14  1999/12/13 03:40:30  sp
 * Bug fixes, syntax is now mostly GPL'd
 *
 */
