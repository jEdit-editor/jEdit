/*
 * SyntaxDocument.java - Document that can be tokenized
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
	 * of this document up into tokens. May return null if this
	 * document is not to be colorized.
	 */
	public TokenMarker getTokenMarker()
	{
		return tokenMarker;
	}

	/**
	 * Sets the token marker that is to be used to split lines of
	 * this document up into tokens. May throw an exception if
	 * this is not supported for this type of document.
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
		if(tokenMarker == null || !tokenMarker.supportsMultilineTokens())
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
				tokenMarker.insertLines(ch.getIndex() + 1,
					ch.getChildrenAdded().length -
					ch.getChildrenRemoved().length);
				tokenMarker.linesChanged(ch.getIndex(),2);
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
				tokenMarker.linesChanged(ch.getIndex(),2);
				tokenMarker.deleteLines(ch.getIndex() + 1,
					ch.getChildrenRemoved().length -
					ch.getChildrenAdded().length);
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
 * Revision 1.13  1999/12/10 03:22:47  sp
 * Bug fixes, old loading code is now used again
 *
 * Revision 1.12  1999/12/07 08:16:55  sp
 * Reload bug nailed to the wall
 *
 * Revision 1.11  1999/11/07 06:51:43  sp
 * Check box menu items supported
 *
 * Revision 1.10  1999/10/24 02:06:41  sp
 * Miscallaneous pre1 stuff
 *
 * Revision 1.9  1999/09/30 12:21:05  sp
 * No net access for a month... so here's one big jEdit 2.1pre1
 *
 * Revision 1.8  1999/07/05 04:38:39  sp
 * Massive batch of changes... bug fixes, also new text component is in place.
 * Have fun
 *
 * Revision 1.7  1999/06/22 06:14:39  sp
 * RMI updates, text area updates, flag to disable geometry saving
 *
 * Revision 1.6  1999/06/07 06:36:32  sp
 * Syntax `styling' (bold/italic tokens) added,
 * plugin options dialog for plugin option panes
 *
 * Revision 1.5  1999/06/05 00:22:58  sp
 * LGPL'd syntax package
 *
 */
