/*
 * JEditTextArea.java - jEdit's text component
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

import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.Enumeration;
import java.util.Vector;
import org.gjt.sp.jedit.gui.InputHandler;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.jedit.EditAction;
import org.gjt.sp.jedit.View;
import org.gjt.sp.util.Log;

/**
 * jEdit's text area component. It is more suited for editing program
 * source code than JEditorPane, because it drops the unnecessary features
 * (images, variable-height lines, and so on) and adds a whole bunch of
 * useful goodies such as:
 * <ul>
 * <li>More flexible key binding scheme
 * <li>Supports macro recorders
 * <li>Rectangular selection
 * <li>Bracket highlighting
 * <li>Syntax highlighting
 * <li>Command repetition
 * <li>Block caret can be enabled
 * </ul>
 * It is also faster and doesn't have as many bugs.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class JEditTextArea extends JComponent
{
	/**
	 * Creates a new JEditTextArea.
	 */
	public JEditTextArea()
	{
		enableEvents(AWTEvent.FOCUS_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);

		// Initialize some misc. stuff
		painter = new TextAreaPainter(this);
		gutter = new Gutter(this);
		documentHandler = new DocumentHandler();
		listenerList = new EventListenerList();
		caretEvent = new MutableCaretEvent();
		bracketLine = bracketPosition = -1;
		blink = true;
		lineSegment = new Segment();

		// Initialize the GUI
		setLayout(new ScrollLayout());
		add(LEFT,gutter);
		add(CENTER,painter);
		add(RIGHT,vertical = new JScrollBar(JScrollBar.VERTICAL));
		add(BOTTOM,horizontal = new JScrollBar(JScrollBar.HORIZONTAL));

		// Add some event listeners
		vertical.addAdjustmentListener(new AdjustHandler());
		horizontal.addAdjustmentListener(new AdjustHandler());
		painter.addComponentListener(new ComponentHandler());
		painter.addMouseListener(new MouseHandler());
		painter.addMouseMotionListener(new DragHandler());
		addFocusListener(new FocusHandler());

		caretVisible = true;
		editable = true;
	}

	/**
	 * Returns the object responsible for painting this text area.
	 */
	public final TextAreaPainter getPainter()
	{
		return painter;
	}

 	/**
	 * Returns the gutter to the left of the text area or null if the gutter
	 * is disabled
	 */
	public final Gutter getGutter()
	{
		return gutter;
	}

	/**
	 * Returns true if the caret is blinking, false otherwise.
	 */
	public final boolean isCaretBlinkEnabled()
	{
		return caretBlinks;
	}

	/**
	 * Toggles caret blinking.
	 * @param caretBlinks True if the caret should blink, false otherwise
	 */
	public void setCaretBlinkEnabled(boolean caretBlinks)
	{
		this.caretBlinks = caretBlinks;
		if(!caretBlinks)
			blink = false;

		painter.invalidateSelectedLines();
	}

	/**
	 * Returns true if the caret is visible, false otherwise.
	 */
	public final boolean isCaretVisible()
	{
		return (!caretBlinks || blink) && caretVisible;
	}

	/**
	 * Sets if the caret should be visible.
	 * @param caretVisible True if the caret should be visible, false
	 * otherwise
	 */
	public void setCaretVisible(boolean caretVisible)
	{
		this.caretVisible = caretVisible;
		blink = true;

		painter.invalidateSelectedLines();
	}

	/**
	 * Blinks the caret.
	 */
	public final void blinkCaret()
	{
		if(caretBlinks)
		{
			blink = !blink;
			painter.invalidateSelectedLines();
		}
		else
			blink = true;
	}

	/**
	 * Returns the number of lines from the top and button of the
	 * text area that are always visible.
	 */
	public final int getElectricScroll()
	{
		return electricScroll;
	}

	/**
	 * Sets the number of lines from the top and bottom of the text
	 * area that are always visible
	 * @param electricScroll The number of lines always visible from
	 * the top or bottom
	 */
	public final void setElectricScroll(int electricScroll)
	{
		this.electricScroll = electricScroll;
	}

	/**
	 * Updates the state of the scroll bars. This should be called
	 * if the number of lines in the document changes, or when the
	 * size of the text are changes.
	 */
	public void updateScrollBars()
	{
		if(vertical != null && visibleLines != 0)
		{
			// don't display stuff past the end of the document if
			// we can help it
			int lineCount = getLineCount();
			if(firstLine < 0)
			{
				setFirstLine(0);
				return;
			}
			else if(lineCount < firstLine + visibleLines)
			{
				// this will call updateScrollBars(), so
				// just return...
				int newFirstLine = Math.max(0,lineCount - visibleLines);
				if(newFirstLine != firstLine)
				{
					setFirstLine(newFirstLine);
					return;
				}
			}

			vertical.setValues(firstLine,visibleLines,0,getLineCount());
			vertical.setUnitIncrement(2);
			vertical.setBlockIncrement(visibleLines);
		}

		int width = painter.getWidth();
		if(horizontal != null && width != 0)
		{
			horizontal.setValues(-horizontalOffset,width,0,width * 5);
			horizontal.setUnitIncrement(painter.getFontMetrics()
				.charWidth('w'));
			horizontal.setBlockIncrement(width / 2);
		}
	}

	/**
	 * Returns the line displayed at the text area's origin.
	 */
	public final int getFirstLine()
	{
		return firstLine;
	}

	/**
	 * Sets the line displayed at the text area's origin.
	 */
	public void setFirstLine(int firstLine)
	{
		if(firstLine == this.firstLine)
			return;

		this.firstLine = firstLine;

		if(firstLine != vertical.getValue())
			updateScrollBars();
		painter.repaint();
		gutter.repaint();
	}

	/**
	 * Returns the number of lines visible in this text area.
	 */
	public final int getVisibleLines()
	{
		return visibleLines;
	}

	/**
	 * Recalculates the number of visible lines. This should not
	 * be called directly.
	 */
	public final void recalculateVisibleLines()
	{
		if(painter == null)
			return;
		int height = painter.getHeight();
		int lineHeight = painter.getFontMetrics().getHeight();
		visibleLines = height / lineHeight;
		updateScrollBars();
	}

	/**
	 * Returns the horizontal offset of drawn lines.
	 */
	public final int getHorizontalOffset()
	{
		return horizontalOffset;
	}

	/**
	 * Sets the horizontal offset of drawn lines. This can be used to
	 * implement horizontal scrolling.
	 * @param horizontalOffset offset The new horizontal offset
	 */
	public void setHorizontalOffset(int horizontalOffset)
	{
		if(horizontalOffset == this.horizontalOffset)
			return;
		this.horizontalOffset = horizontalOffset;
		if(horizontalOffset != horizontal.getValue())
			updateScrollBars();
		painter.repaint();
	}

	/**
	 * A fast way of changing both the first line and horizontal
	 * offset.
	 * @param firstLine The new first line
	 * @param horizontalOffset The new horizontal offset
	 * @return True if any of the values were changed, false otherwise
	 */
	public boolean setOrigin(int firstLine, int horizontalOffset)
	{
		boolean vertChanged = false, horizChanged = false;
		int oldFirstLine = this.firstLine;

		if(firstLine != this.firstLine)
		{
			this.firstLine = firstLine;
			vertChanged = true;
		}

		if(horizontalOffset != this.horizontalOffset)
		{
			this.horizontalOffset = horizontalOffset;
			horizChanged = true;
		}

		if(vertChanged || horizChanged)
		{
			updateScrollBars();
			painter.repaint();
			gutter.repaint();
		}

		return vertChanged || horizChanged;
	}

	/**
	 * Ensures that the caret is visible by scrolling the text area if
	 * necessary.
	 * @return True if scrolling was actually performed, false if the
	 * caret was already visible
	 */
	public boolean scrollToCaret()
	{
		int line = getCaretLine();
		int lineStart = getLineStartOffset(line);
		int offset = Math.max(0,Math.min(getLineLength(line) - 1,
			getCaretPosition() - lineStart));

		return scrollTo(line,offset);
	}

	/**
	 * Ensures that the specified line and offset is visible by scrolling
	 * the text area if necessary.
	 * @param line The line to scroll to
	 * @param offset The offset in the line to scroll to
	 * @return True if scrolling was actually performed, false if the
	 * line and offset was already visible
	 */
	public boolean scrollTo(int line, int offset)
	{
		// visibleLines == 0 before the component is realized
		// we can't do any proper scrolling then, so we have
		// this hack...
		if(visibleLines == 0)
		{
			setFirstLine(Math.max(0,line - electricScroll));
			return true;
		}

		int newFirstLine = firstLine;
		int newHorizontalOffset = horizontalOffset;

		if(line < firstLine + electricScroll)
		{
			newFirstLine = Math.max(0,line - electricScroll);
		}
		else if(line + electricScroll >= firstLine + visibleLines)
		{
			newFirstLine = (line - visibleLines) + electricScroll + 1;
			if(newFirstLine + visibleLines >= getLineCount())
				newFirstLine = getLineCount() - visibleLines;
			if(newFirstLine < 0)
				newFirstLine = 0;
		}

		int x = offsetToX(line,offset);
		int width = painter.getFontMetrics().charWidth('w');

		if(x < 0)
		{
			newHorizontalOffset = Math.min(0,horizontalOffset
				- x + width + 5);
		}
		else if(x + width >= painter.getWidth())
		{
			newHorizontalOffset = horizontalOffset +
				(painter.getWidth() - x) - width - 5;
		}

		return setOrigin(newFirstLine,newHorizontalOffset);
	}

	/**
	 * Converts a line index to a y co-ordinate.
	 * @param line The line
	 */
	public int lineToY(int line)
	{
		FontMetrics fm = painter.getFontMetrics();
		return (line - firstLine) * fm.getHeight()
			- (fm.getLeading() + fm.getDescent());
	}

	/**
	 * Converts a y co-ordinate to a line index.
	 * @param y The y co-ordinate
	 */
	public int yToLine(int y)
	{
		FontMetrics fm = painter.getFontMetrics();
		int height = fm.getHeight();
		return Math.max(0,Math.min(getLineCount() - 1,
			y / height + firstLine));
	}

	/**
	 * Converts an offset in a line into an x co-ordinate.
	 * @param line The line
	 * @param offset The offset, from the start of the line
	 */
	public int offsetToX(int line, int offset)
	{
		TokenMarker tokenMarker = getTokenMarker();

		/* Use painter's cached info for speed */
		FontMetrics fm = painter.getFontMetrics();

		getLineText(line,lineSegment);

		int segmentOffset = lineSegment.offset;
		int x = horizontalOffset;

		/* If syntax coloring is disabled, do simple translation */
		if(tokenMarker == null)
		{
			lineSegment.count = offset;
			return x + Utilities.getTabbedTextWidth(lineSegment,
				fm,x,painter,0);
		}
		/* If syntax coloring is enabled, we have to do this because
		 * tokens can vary in width */
		else
		{
			Token tokens = tokenMarker.markTokens(lineSegment,line);

			Toolkit toolkit = painter.getToolkit();
			Font defaultFont = painter.getFont();
			SyntaxStyle[] styles = painter.getStyles();

			for(;;)
			{
				byte id = tokens.id;
				if(id == Token.END)
				{
					return x;
				}

				if(id == Token.NULL)
					fm = painter.getFontMetrics();
				else
					fm = styles[id].getFontMetrics(defaultFont);

				int length = tokens.length;

				if(offset + segmentOffset < lineSegment.offset + length)
				{
					lineSegment.count = offset - (lineSegment.offset - segmentOffset);
					return x + Utilities.getTabbedTextWidth(
						lineSegment,fm,x,painter,0);
				}
				else
				{
					lineSegment.count = length;
					x += Utilities.getTabbedTextWidth(
						lineSegment,fm,x,painter,0);
					lineSegment.offset += length;
				}
				tokens = tokens.next;
			}
		}
	}

	/**
	 * Converts an x co-ordinate to an offset within a line.
	 * @param line The line
	 * @param x The x co-ordinate
	 */
	public int xToOffset(int line, int x)
	{
		TokenMarker tokenMarker = getTokenMarker();

		/* Use painter's cached info for speed */
		FontMetrics fm = painter.getFontMetrics();

		getLineText(line,lineSegment);

		char[] segmentArray = lineSegment.array;
		int segmentOffset = lineSegment.offset;
		int segmentCount = lineSegment.count;

		int width = horizontalOffset;

		if(tokenMarker == null)
		{
			for(int i = 0; i < segmentCount; i++)
			{
				char c = segmentArray[i + segmentOffset];
				int charWidth;
				if(c == '\t')
					charWidth = (int)painter.nextTabStop(width,i)
						- width;
				else
					charWidth = fm.charWidth(c);

				if(painter.isBlockCaretEnabled())
				{
					if(x - charWidth <= width)
						return i;
				}
				else
				{
					if(x - charWidth / 2 <= width)
						return i;
				}

				width += charWidth;
			}

			return segmentCount;
		}
		else
		{
			Token tokens = tokenMarker.markTokens(lineSegment,line);

			int offset = 0;
			Toolkit toolkit = painter.getToolkit();
			Font defaultFont = painter.getFont();
			SyntaxStyle[] styles = painter.getStyles();

			for(;;)
			{
				byte id = tokens.id;
				if(id == Token.END)
					return offset;

				if(id == Token.NULL)
					fm = painter.getFontMetrics();
				else
					fm = styles[id].getFontMetrics(defaultFont);

				int length = tokens.length;

				for(int i = 0; i < length; i++)
				{
					char c = segmentArray[segmentOffset + offset + i];
					int charWidth;
					if(c == '\t')
						charWidth = (int)painter.nextTabStop(width,offset + i)
							- width;
					else
						charWidth = fm.charWidth(c);

					if(painter.isBlockCaretEnabled())
					{
						if(x - charWidth <= width)
							return offset + i;
					}
					else
					{
						if(x - charWidth / 2 <= width)
							return offset + i;
					}

					width += charWidth;
				}

				offset += length;
				tokens = tokens.next;
			}
		}
	}

	/**
	 * Converts a point to an offset, from the start of the text.
	 * @param x The x co-ordinate of the point
	 * @param y The y co-ordinate of the point
	 */
	public int xyToOffset(int x, int y)
	{
		int line = yToLine(y);
		int start = getLineStartOffset(line);
		return start + xToOffset(line,x);
	}

	/**
	 * Returns the document this text area is editing.
	 */
	public final SyntaxDocument getDocument()
	{
		return document;
	}

	/**
	 * Sets the document this text area is editing.
	 * @param document The document
	 */
	public void setDocument(SyntaxDocument document)
	{
		if(this.document == document)
			return;
		if(this.document != null)
			this.document.removeDocumentListener(documentHandler);
		this.document = document;

		document.addDocumentListener(documentHandler);
		documentHandlerInstalled = true;

		select(0,0);
		updateScrollBars();
		painter.repaint();
		gutter.repaint();
	}

	/**
	 * Returns the document's token marker. Equivalent to calling
	 * <code>getDocument().getTokenMarker()</code>.
	 */
	public final TokenMarker getTokenMarker()
	{
		return document.getTokenMarker();
	}

	/**
	 * Sets the document's token marker. Equivalent to caling
	 * <code>getDocument().setTokenMarker()</code>.
	 * @param tokenMarker The token marker
	 */
	public final void setTokenMarker(TokenMarker tokenMarker)
	{
		document.setTokenMarker(tokenMarker);
	}

	/**
	 * Returns the length of the document. Equivalent to calling
	 * <code>getDocument().getLength()</code>.
	 */
	public final int getDocumentLength()
	{
		return document.getLength();
	}

	/**
	 * Returns the number of lines in the document.
	 */
	public final int getLineCount()
	{
		return document.getDefaultRootElement().getElementCount();
	}

	/**
	 * Returns the line containing the specified offset.
	 * @param offset The offset
	 */
	public final int getLineOfOffset(int offset)
	{
		return document.getDefaultRootElement().getElementIndex(offset);
	}

	/**
	 * Returns the start offset of the specified line.
	 * @param line The line
	 * @return The start offset of the specified line, or -1 if the line is
	 * invalid
	 */
	public int getLineStartOffset(int line)
	{
		Element lineElement = document.getDefaultRootElement()
			.getElement(line);
		if(lineElement == null)
			return -1;
		else
			return lineElement.getStartOffset();
	}

	/**
	 * Returns the end offset of the specified line.
	 * @param line The line
	 * @return The end offset of the specified line, or -1 if the line is
	 * invalid.
	 */
	public int getLineEndOffset(int line)
	{
		Element lineElement = document.getDefaultRootElement()
			.getElement(line);
		if(lineElement == null)
			return -1;
		else
			return lineElement.getEndOffset();
	}

	/**
	 * Returns the length of the specified line.
	 * @param line The line
	 */
	public int getLineLength(int line)
	{
		Element lineElement = document.getDefaultRootElement()
			.getElement(line);
		if(lineElement == null)
			return -1;
		else
			return lineElement.getEndOffset()
				- lineElement.getStartOffset() - 1;
	}

	/**
	 * Returns the entire text of this text area.
	 */
	public String getText()
	{
		try
		{
			return document.getText(0,document.getLength());
		}
		catch(BadLocationException bl)
		{
			bl.printStackTrace();
			return null;
		}
	}

	/**
	 * Sets the entire text of this text area.
	 */
	public void setText(String text)
	{
		try
		{
			document.beginCompoundEdit();
			document.remove(0,document.getLength());
			document.insertString(0,text,null);
		}
		catch(BadLocationException bl)
		{
			bl.printStackTrace();
		}
		finally
		{
			document.endCompoundEdit();
		}
	}

	/**
	 * Returns the specified substring of the document.
	 * @param start The start offset
	 * @param len The length of the substring
	 * @return The substring, or null if the offsets are invalid
	 */
	public final String getText(int start, int len)
	{
		try
		{
			return document.getText(start,len);
		}
		catch(BadLocationException bl)
		{
			bl.printStackTrace();
			return null;
		}
	}

	/**
	 * Copies the specified substring of the document into a segment.
	 * If the offsets are invalid, the segment will contain a null string.
	 * @param start The start offset
	 * @param len The length of the substring
	 * @param segment The segment
	 */
	public final void getText(int start, int len, Segment segment)
	{
		try
		{
			document.getText(start,len,segment);
		}
		catch(BadLocationException bl)
		{
			bl.printStackTrace();
			segment.offset = segment.count = 0;
		}
	}

	/**
	 * Returns the text on the specified line.
	 * @param lineIndex The line
	 * @return The text, or null if the line is invalid
	 */
	public final String getLineText(int lineIndex)
	{
		int start = getLineStartOffset(lineIndex);
		return getText(start,getLineEndOffset(lineIndex) - start - 1);
	}

	/**
	 * Copies the text on the specified line into a segment. If the line
	 * is invalid, the segment will contain a null string.
	 * @param lineIndex The line
	 */
	public final void getLineText(int lineIndex, Segment segment)
	{
		int start = getLineStartOffset(lineIndex);
		getText(start,getLineEndOffset(lineIndex) - start - 1,segment);
	}

	/**
	 * Returns the selection start offset.
	 */
	public final int getSelectionStart()
	{
		return selectionStart;
	}

	/**
	 * Returns the offset where the selection starts on the specified
	 * line.
	 */
	public int getSelectionStart(int line)
	{
		if(line == selectionStartLine)
			return selectionStart;
		else if(rectSelect)
		{
			Element map = document.getDefaultRootElement();
			int start = selectionStart - map.getElement(selectionStartLine)
				.getStartOffset();

			Element lineElement = map.getElement(line);
			int lineStart = lineElement.getStartOffset();
			int lineEnd = lineElement.getEndOffset() - 1;
			return Math.min(lineEnd,lineStart + start);
		}
		else
			return getLineStartOffset(line);
	}

	/**
	 * Returns the selection start line.
	 */
	public final int getSelectionStartLine()
	{
		return selectionStartLine;
	}

	/**
	 * Sets the selection start. The new selection will be the new
	 * selection start and the old selection end.
	 * @param selectionStart The selection start
	 * @see #select(int,int)
	 */
	public final void setSelectionStart(int selectionStart)
	{
		select(selectionStart,selectionEnd);
	}

	/**
	 * Returns the selection end offset.
	 */
	public final int getSelectionEnd()
	{
		return selectionEnd;
	}

	/**
	 * Returns the offset where the selection ends on the specified
	 * line.
	 */
	public int getSelectionEnd(int line)
	{
		if(line == selectionEndLine)
			return selectionEnd;
		else if(rectSelect)
		{
			Element map = document.getDefaultRootElement();
			int end = selectionEnd - map.getElement(selectionEndLine)
				.getStartOffset();

			Element lineElement = map.getElement(line);
			int lineStart = lineElement.getStartOffset();
			int lineEnd = lineElement.getEndOffset() - 1;
			return Math.min(lineEnd,lineStart + end);
		}
		else
			return getLineEndOffset(line) - 1;
	}

	/**
	 * Returns the selection end line.
	 */
	public final int getSelectionEndLine()
	{
		return selectionEndLine;
	}

	/**
	 * Sets the selection end. The new selection will be the old
	 * selection start and the bew selection end.
	 * @param selectionEnd The selection end
	 * @see #select(int,int)
	 */
	public final void setSelectionEnd(int selectionEnd)
	{
		select(selectionStart,selectionEnd);
	}

	/**
	 * Returns the caret position. This will either be the selection
	 * start or the selection end, depending on which direction the
	 * selection was made in.
	 */
	public final int getCaretPosition()
	{
		return (biasLeft ? selectionStart : selectionEnd);
	}

	/**
	 * Returns the caret line.
	 */
	public final int getCaretLine()
	{
		return (biasLeft ? selectionStartLine : selectionEndLine);
	}

	/**
	 * Returns the mark position. This will be the opposite selection
	 * bound to the caret position.
	 * @see #getCaretPosition()
	 */
	public final int getMarkPosition()
	{
		return (biasLeft ? selectionEnd : selectionStart);
	}

	/**
	 * Returns the mark line.
	 */
	public final int getMarkLine()
	{
		return (biasLeft ? selectionEndLine : selectionStartLine);
	}

	/**
	 * Sets the caret position. The new selection will consist of the
	 * caret position only (hence no text will be selected)
	 * @param caret The caret position
	 * @see #select(int,int)
	 */
	public final void setCaretPosition(int caret)
	{
		select(caret,caret);
	}

	/**
	 * Selects all text in the document.
	 */
	public final void selectAll()
	{
		select(0,getDocumentLength());
	}

	/**
	 * Moves the mark to the caret position.
	 */
	public final void selectNone()
	{
		select(getCaretPosition(),getCaretPosition());
	}

	/**
	 * Selects from the start offset to the end offset. This is the
	 * general selection method used by all other selecting methods.
	 * The caret position will be start if start &lt; end, and end
	 * if end &gt; start.
	 * @param start The start offset
	 * @param end The end offset
	 */
	public void select(int start, int end)
	{
		int newStart, newEnd;
		boolean newBias;
		if(start <= end)
		{
			newStart = start;
			newEnd = end;
			newBias = false;
		}
		else
		{
			newStart = end;
			newEnd = start;
			newBias = true;
		}

		if(newStart < 0 || newEnd > getDocumentLength())
		{
			throw new IllegalArgumentException("Bounds out of"
				+ " range: " + newStart + "," +
				newEnd);
		}

		// If the new position is the same as the old, we don't
		// do all this crap, however we still do the stuff at
		// the end (clearing magic position, scrolling)
		if(newStart != selectionStart || newEnd != selectionEnd
			|| newBias != biasLeft)
		{
			updateBracketHighlight(end);

			int newStartLine = getLineOfOffset(newStart);
			int newEndLine = getLineOfOffset(newEnd);

			painter.invalidateLineRange(selectionStartLine,selectionEndLine);
			painter.invalidateLineRange(newStartLine,newEndLine);

			// repaint the gutter if the current line changes and current
			// line highlighting is enabled
			if ((newStartLine != selectionStartLine || newEndLine
				!= selectionEndLine || newBias != biasLeft)
				&& gutter.isCurrentLineHighlightEnabled())
			{
				gutter.invalidateLine(biasLeft ? selectionStartLine
					: selectionEndLine);
				gutter.invalidateLine(newBias ? newStartLine : newEndLine);
			}

			document.addUndoableEdit(new CaretUndo(
				selectionStart,selectionEnd,
				newStart,newEnd));

			selectionStart = newStart;
			selectionEnd = newEnd;
			selectionStartLine = newStartLine;
			selectionEndLine = newEndLine;
			biasLeft = newBias;

			fireCaretEvent();
		}

		// When the user is typing, etc, we don't want the caret
		// to blink
		blink = true;
		caretTimer.restart();

		// Disable rectangle select if selection start = selection end
		if(selectionStart == selectionEnd)
			rectSelect = false;

		// Clear the `magic' caret position used by up/down
		magicCaret = -1;

		scrollToCaret();
	}

	/**
	 * Returns the selected text, or null if no selection is active.
	 */
	public final String getSelectedText()
	{
		if(selectionStart == selectionEnd)
			return null;

		if(rectSelect)
		{
			// Return each row of the selection on a new line

			Element map = document.getDefaultRootElement();

			int start = selectionStart - map.getElement(selectionStartLine)
				.getStartOffset();
			int end = selectionEnd - map.getElement(selectionEndLine)
				.getStartOffset();

			// Certain rectangles satisfy this condition...
			if(end < start)
			{
				int tmp = end;
				end = start;
				start = tmp;
			}

			StringBuffer buf = new StringBuffer();
			Segment seg = new Segment();

			for(int i = selectionStartLine; i <= selectionEndLine; i++)
			{
				Element lineElement = map.getElement(i);
				int lineStart = lineElement.getStartOffset();
				int lineEnd = lineElement.getEndOffset() - 1;
				int lineLen = lineEnd - lineStart;

				lineStart = Math.min(lineStart + start,lineEnd);
				lineLen = Math.min(end - start,lineEnd - lineStart);

				getText(lineStart,lineLen,seg);
				buf.append(seg.array,seg.offset,seg.count);

				if(i != selectionEndLine)
					buf.append('\n');
			}

			return buf.toString();
		}
		else
		{
			return getText(selectionStart,
				selectionEnd - selectionStart);
		}
	}

	/**
	 * Replaces the selection with the specified text.
	 * @param selectedText The replacement text for the selection
	 */
	public void setSelectedText(String selectedText)
	{
		if(!editable)
		{
			throw new InternalError("Text component"
				+ " read only");
		}

		document.beginCompoundEdit();

		try
		{
			if(rectSelect)
			{
				Element map = document.getDefaultRootElement();

				int start = selectionStart - map.getElement(selectionStartLine)
					.getStartOffset();
				int end = selectionEnd - map.getElement(selectionEndLine)
					.getStartOffset();

				// Certain rectangles satisfy this condition...
				if(end < start)
				{
					int tmp = end;
					end = start;
					start = tmp;
				}

				int lastNewline = 0;
				int currNewline = 0;

				for(int i = selectionStartLine; i <= selectionEndLine; i++)
				{
					Element lineElement = map.getElement(i);
					int lineStart = lineElement.getStartOffset();
					int lineEnd = lineElement.getEndOffset() - 1;
					int rectStart = Math.min(lineEnd,lineStart + start);

					document.remove(rectStart,Math.min(lineEnd - rectStart,
						end - start));

					if(selectedText == null)
						continue;

					currNewline = selectedText.indexOf('\n',lastNewline);
					if(currNewline == -1)
						currNewline = selectedText.length();

					document.insertString(rectStart,selectedText
						.substring(lastNewline,currNewline),null);

					lastNewline = Math.min(selectedText.length(),
						currNewline + 1);
				}

				if(selectedText != null &&
					currNewline != selectedText.length())
				{
					int offset = map.getElement(selectionEndLine)
						.getEndOffset() - 1;
					document.insertString(offset,"\n",null);
					document.insertString(offset + 1,selectedText
						.substring(currNewline + 1),null);
				}
			}
			else
			{
				document.remove(selectionStart,
					selectionEnd - selectionStart);
				if(selectedText != null)
				{
					document.insertString(selectionStart,
						selectedText,null);
				}
			}
		}
		catch(BadLocationException bl)
		{
			bl.printStackTrace();
			throw new InternalError("Cannot replace"
				+ " selection");
		}
		// No matter what happends... stops us from leaving document
		// in a bad state
		finally
		{
			document.endCompoundEdit();
		}

		setCaretPosition(selectionEnd);
	}

	/**
	 * Returns true if this text area is editable, false otherwise.
	 */
	public final boolean isEditable()
	{
		return editable;
	}

	/**
	 * Sets if this component is editable.
	 * @param editable True if this text area should be editable,
	 * false otherwise
	 */
	public final void setEditable(boolean editable)
	{
		this.editable = editable;
	}

	/**
	 * Returns the right click popup menu.
	 */
	public final JPopupMenu getRightClickPopup()
	{
		return popup;
	}

	/**
	 * Sets the right click popup menu.
	 * @param popup The popup
	 */
	public final void setRightClickPopup(JPopupMenu popup)
	{
		this.popup = popup;
	}

	/**
	 * Returns the `magic' caret position. This can be used to preserve
	 * the column position when moving up and down lines.
	 */
	public final int getMagicCaretPosition()
	{
		return magicCaret;
	}

	/**
	 * Sets the `magic' caret position. This can be used to preserve
	 * the column position when moving up and down lines.
	 * @param magicCaret The magic caret position
	 */
	public final void setMagicCaretPosition(int magicCaret)
	{
		this.magicCaret = magicCaret;
	}

	/**
	 * Similar to <code>setSelectedText()</code>, but overstrikes the
	 * appropriate number of characters if overwrite mode is enabled.
	 * @param str The string
	 * @see #setSelectedText(String)
	 * @see #isOverwriteEnabled()
	 */
	public void overwriteSetSelectedText(String str)
	{
		// Don't overstrike if there is a selection
		if(!overwrite || selectionStart != selectionEnd)
		{
			setSelectedText(str);
			return;
		}

		// Don't overstrike if we're on the end of
		// the line
		int caret = getCaretPosition();
		int caretLineEnd = getLineEndOffset(getCaretLine());
		if(caretLineEnd - caret <= str.length())
		{
			setSelectedText(str);
			return;
		}

		document.beginCompoundEdit();

		try
		{
			document.remove(caret,str.length());
			document.insertString(caret,str,null);
		}
		catch(BadLocationException bl)
		{
			bl.printStackTrace();
		}
		finally
		{
			document.endCompoundEdit();
		}
	}

	/**
	 * Returns true if overwrite mode is enabled, false otherwise.
	 */
	public final boolean isOverwriteEnabled()
	{
		return overwrite;
	}

	/**
	 * Sets if overwrite mode should be enabled.
	 * @param overwrite True if overwrite mode should be enabled,
	 * false otherwise.
	 */
	public final void setOverwriteEnabled(boolean overwrite)
	{
		this.overwrite = overwrite;
		painter.invalidateSelectedLines();
	}

	/**
	 * Returns true if the selection is rectangular, false otherwise.
	 */
	public final boolean isSelectionRectangular()
	{
		return rectSelect;
	}

	/**
	 * Sets if the selection should be rectangular.
	 * @param overwrite True if the selection should be rectangular,
	 * false otherwise.
	 */
	public final void setSelectionRectangular(boolean rectSelect)
	{
		this.rectSelect = rectSelect;
		painter.invalidateSelectedLines();
	}

	/**
	 * Returns the position of the highlighted bracket (the bracket
	 * matching the one before the caret)
	 */
	public final int getBracketPosition()
	{
		return bracketPosition;
	}

	/**
	 * Returns the line of the highlighted bracket (the bracket
	 * matching the one before the caret)
	 */
	public final int getBracketLine()
	{
		return bracketLine;
	}

	/**
	 * Adds a caret change listener to this text area.
	 * @param listener The listener
	 */
	public final void addCaretListener(CaretListener listener)
	{
		listenerList.add(CaretListener.class,listener);
	}

	/**
	 * Removes a caret change listener from this text area.
	 * @param listener The listener
	 */
	public final void removeCaretListener(CaretListener listener)
	{
		listenerList.remove(CaretListener.class,listener);
	}

	/**
	 * Called by the AWT when this component is added to a parent.
	 * Adds document listener.
	 */
	public void addNotify()
	{
		super.addNotify();

		if(hasFocus())
		{
			setCaretVisible(true);
			focusedComponent = this;
		}

		ToolTipManager.sharedInstance().registerComponent(painter);
		ToolTipManager.sharedInstance().registerComponent(gutter);

		if(!documentHandlerInstalled)
		{
			documentHandlerInstalled = true;
			document.addDocumentListener(documentHandler);
		}

		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				recalculateVisibleLines();
			}
		});
	}

	/**
	 * Called by the AWT when this component is removed from it's parent.
	 * This clears the pointer to the currently focused component.
	 * Also removes document listener.
	 */
	public void removeNotify()
	{
		super.removeNotify();

		ToolTipManager.sharedInstance().unregisterComponent(painter);

		if(focusedComponent == this)
		{
			focusedComponent = null;
		}

		if(documentHandlerInstalled)
		{
			document.removeDocumentListener(documentHandler);
			documentHandlerInstalled = false;
		}
	}

	// package-private members

	Segment lineSegment;

	// protected members

	protected void processKeyEvent(KeyEvent evt)
	{
		View view = EditAction.getView(evt);

		// Ignore
		if(view.isClosed())
			return;

		if(view.getRootPane().getGlassPane().isVisible())
		{
			super.processKeyEvent(evt);
			return;
		}

		InputHandler inputHandler = view.getInputHandler();
		switch(evt.getID())
		{
		case KeyEvent.KEY_TYPED:
			inputHandler.keyTyped(evt);
			break;
		case KeyEvent.KEY_PRESSED:
			inputHandler.keyPressed(evt);
			break;
		case KeyEvent.KEY_RELEASED:
			inputHandler.keyReleased(evt);
			break;
		}

		if(!evt.isConsumed())
			super.processKeyEvent(evt);
	}

	// private members
	private static String CENTER = "center";
	private static String RIGHT = "right";
	private static String LEFT = "left";
	private static String BOTTOM = "bottom";

	private static JEditTextArea focusedComponent;
	private static Timer caretTimer;
	
	private TextAreaPainter painter;

	private Gutter gutter;

	private JPopupMenu popup;

	private EventListenerList listenerList;
	private MutableCaretEvent caretEvent;

	private boolean caretBlinks;
	private boolean caretVisible;
	private boolean blink;

	private boolean editable;

	private int firstLine;
	private int visibleLines;
	private int electricScroll;

	private int horizontalOffset;
	
	private JScrollBar vertical;
	private JScrollBar horizontal;
	private boolean scrollBarsInitialized;

	private SyntaxDocument document;
	private DocumentHandler documentHandler;
	private boolean documentHandlerInstalled;

	private int selectionStart;
	private int selectionStartLine;
	private int selectionEnd;
	private int selectionEndLine;
	private boolean biasLeft;

	private int bracketPosition;
	private int bracketLine;

	private int magicCaret;

	// Offset where drag was started; used by double-click drag (word
	// selection)
	private int dragStartLine;
	private int dragStartOffset;

	private boolean overwrite;
	private boolean rectSelect;

	private void fireCaretEvent()
	{
		Object[] listeners = listenerList.getListenerList();
		for(int i = listeners.length - 2; i >= 0; i--)
		{
			if(listeners[i] == CaretListener.class)
			{
				((CaretListener)listeners[i+1]).caretUpdate(caretEvent);
			}
		}
	}

	private void updateBracketHighlight(int newCaretPosition)
	{
		if(!painter.isBracketHighlightEnabled())
			return;

		if(bracketLine != -1)
			painter.invalidateLine(bracketLine);

		if(newCaretPosition == 0)
		{
			bracketPosition = bracketLine = -1;
			return;
		}

		try
		{
			int offset = TextUtilities.findMatchingBracket(
				document,newCaretPosition - 1);
			if(offset != -1)
			{
				bracketLine = getLineOfOffset(offset);
				bracketPosition = offset - getLineStartOffset(bracketLine);

				if(bracketLine != -1)
					painter.invalidateLine(bracketLine);
				return;
			}
		}
		catch(BadLocationException bl)
		{
			bl.printStackTrace();
		}

		bracketLine = bracketPosition = -1;
	}

	private void documentChanged(DocumentEvent evt)
	{
		DocumentEvent.ElementChange ch = evt.getChange(
			document.getDefaultRootElement());

		int count;
		if(ch == null)
			count = 0;
		else
			count = ch.getChildrenAdded().length -
				ch.getChildrenRemoved().length;

		int line = getLineOfOffset(evt.getOffset());
		if(count == 0)
		{
			painter.invalidateLine(line);
		}
		// do magic stuff
		else if(line < firstLine)
		{
			setFirstLine(firstLine + count);
		}
		// end of magic stuff
		else
		{
			painter.invalidateLineRange(line,firstLine + visibleLines);
			gutter.invalidateLineRange(line,firstLine + visibleLines);
			updateScrollBars();
		}
	}

	// private members

	// for event handlers only
	private int clickCount;

	class ScrollLayout implements LayoutManager
	{
		public void addLayoutComponent(String name, Component comp)
		{
			if(name.equals(CENTER))
				center = comp;
			else if(name.equals(RIGHT))
				right = comp;
			else if(name.equals(LEFT))
				left = comp;
			else if(name.equals(BOTTOM))
				bottom = comp;
		}

		public void removeLayoutComponent(Component comp)
		{
			if(center == comp)
				center = null;
			else if(right == comp)
				right = null;
			else if(left == comp)
				left = null;
			else if(bottom == comp)
				bottom = null;
		}

		public Dimension preferredLayoutSize(Container parent)
		{
			Dimension dim = new Dimension();
			Insets insets = getInsets();
			dim.width = insets.left + insets.right;
			dim.height = insets.top + insets.bottom;

			Dimension leftPref = left.getPreferredSize();
			dim.width += leftPref.width;
			Dimension centerPref = center.getPreferredSize();
			dim.width += centerPref.width;
			dim.height += centerPref.height;
			Dimension rightPref = right.getPreferredSize();
			dim.width += rightPref.width;
			Dimension bottomPref = bottom.getPreferredSize();
			dim.height += bottomPref.height;

			return dim;
		}

		public Dimension minimumLayoutSize(Container parent)
		{
			Dimension dim = new Dimension();
			Insets insets = getInsets();
			dim.width = insets.left + insets.right;
			dim.height = insets.top + insets.bottom;

			Dimension leftPref = left.getMinimumSize();
			dim.width += leftPref.width;
			Dimension centerPref = center.getMinimumSize();
			dim.width += centerPref.width; 
			dim.height += centerPref.height;
			Dimension rightPref = right.getMinimumSize();
			dim.width += rightPref.width;
			Dimension bottomPref = bottom.getMinimumSize();
			dim.height += bottomPref.height;

			return dim;
		}

		public void layoutContainer(Container parent)
		{
			Dimension size = parent.getSize();
			Insets insets = parent.getInsets();
			int itop = insets.top;
			int ileft = insets.left;
			int ibottom = insets.bottom;
			int iright = insets.right;

			int rightWidth = right.getPreferredSize().width;
			int leftWidth = left.getPreferredSize().width;
			int bottomHeight = bottom.getPreferredSize().height;
			int centerWidth = size.width - leftWidth - rightWidth -
				ileft - iright;
			int centerHeight = size.height - bottomHeight - itop -
				ibottom;

			left.setBounds(
				ileft,
				itop,
				leftWidth,
				centerHeight);

			center.setBounds(
				ileft + leftWidth,
				itop,
				centerWidth,
				centerHeight);

			right.setBounds(
				ileft + leftWidth + centerWidth,
				itop,
				rightWidth,
				centerHeight);

			bottom.setBounds(
				ileft,
				itop + centerHeight,
				size.width - rightWidth - ileft - iright,
				bottomHeight);
		}

		Component center;
		Component left;
		Component right;
		Component bottom;
	}

	static class CaretBlinker implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(focusedComponent != null
				&& focusedComponent.hasFocus())
				focusedComponent.blinkCaret();
		}
	}

	class MutableCaretEvent extends CaretEvent
	{
		MutableCaretEvent()
		{
			super(JEditTextArea.this);
		}

		public int getDot()
		{
			return getCaretPosition();
		}

		public int getMark()
		{
			return getMarkPosition();
		}
	}

	class AdjustHandler implements AdjustmentListener
	{
		public void adjustmentValueChanged(final AdjustmentEvent evt)
		{
			if(!scrollBarsInitialized)
				return;

			if(evt.getAdjustable() == vertical)
				setFirstLine(vertical.getValue());
			else
				setHorizontalOffset(-horizontal.getValue());
		}
	}

	class ComponentHandler extends ComponentAdapter
	{
		public void componentResized(ComponentEvent evt)
		{
			recalculateVisibleLines();
			scrollBarsInitialized = true;
		}
	}

	class DocumentHandler implements DocumentListener
	{
		public void insertUpdate(DocumentEvent evt)
		{
			documentChanged(evt);

			if(!document.isLoaded())
				return;

			int offset = evt.getOffset();
			int length = evt.getLength();

			int newStart;
			int newEnd;

			boolean change = false;

			if(selectionStart > offset || (selectionStart 
				== selectionEnd && selectionStart == offset))
			{
				change = true;
				newStart = selectionStart + length;
			}
			else
				newStart = selectionStart;

			if(selectionEnd >= offset)
			{
				change = true;
				newEnd = selectionEnd + length;
			}
			else
				newEnd = selectionEnd;

			if(change)
				select(newStart,newEnd);
			else
				updateBracketHighlight(getCaretPosition());
		}

		public void removeUpdate(DocumentEvent evt)
		{
			documentChanged(evt);

			if(!document.isLoaded())
				return;

			int offset = evt.getOffset();
			int length = evt.getLength();

			int newStart;
			int newEnd;

			boolean change = false;

			if(selectionStart > offset)
			{
				change = true;

				if(selectionStart > offset + length)
					newStart = selectionStart - length;
				else
					newStart = offset;
			}
			else
				newStart = selectionStart;

			if(selectionEnd > offset)
			{
				change = true;

				if(selectionEnd > offset + length)
					newEnd = selectionEnd - length;
				else
					newEnd = offset;
			}
			else
				newEnd = selectionEnd;

			if(change)
				select(newStart,newEnd);
			else
				updateBracketHighlight(getCaretPosition());
		}

		public void changedUpdate(DocumentEvent evt)
		{
		}
	}

	class DragHandler implements MouseMotionListener
	{
		public void mouseDragged(MouseEvent evt)
		{
			if(popup != null && popup.isVisible())
				return;

			setSelectionRectangular((evt.getModifiers()
				& InputEvent.CTRL_MASK) != 0);

			switch(clickCount)
			{
			case 1:
				doSingleDrag(evt);
				break;
			case 2:
				doDoubleDrag(evt);
				break;
			case 3:
				doTripleDrag(evt);
				break;
			}
		}

		public void mouseMoved(MouseEvent evt) {}

		private void doSingleDrag(MouseEvent evt)
		{
			select(getMarkPosition(),xyToOffset(
				evt.getX(),evt.getY()));
		}

		private void doDoubleDrag(MouseEvent evt)
		{
			int markLineStart = getLineStartOffset(dragStartLine);
			int markLineLength = getLineLength(dragStartLine);
			int mark = dragStartOffset;

			int line = yToLine(evt.getY());
			int lineStart = getLineStartOffset(line);
			int lineLength = getLineLength(line);
			int offset = xToOffset(line,evt.getX());

			String lineText = getLineText(line);
			String markLineText = getLineText(dragStartLine);
			String noWordSep = (String)document.getProperty("noWordSep");

			if(markLineStart + dragStartOffset > lineStart + offset)
			{
				if(offset != 0 && offset != lineLength)
				{
					offset = TextUtilities.findWordStart(
						lineText,offset,noWordSep);
				}

				if(markLineLength != 0)
				{
					mark = TextUtilities.findWordEnd(
						markLineText,mark,noWordSep);
				}
			}
			else
			{
				if(offset != 0 && lineLength != 0)
				{
					offset = TextUtilities.findWordEnd(
						lineText,offset,noWordSep);
				}

				if(mark != 0 && mark != markLineLength)
				{
					mark = TextUtilities.findWordStart(
						markLineText,mark,noWordSep);
				}
			}

			select(markLineStart + mark,lineStart + offset);
		}

		private void doTripleDrag(MouseEvent evt)
		{
			int mark = getMarkLine();
			int mouse = yToLine(evt.getY());
			int offset = xToOffset(mouse,evt.getX());
			if(mark > mouse)
			{
				mark = getLineEndOffset(mark) - 1;
				if(offset == getLineLength(mouse))
					mouse = getLineEndOffset(mouse) - 1;
				else
					mouse = getLineStartOffset(mouse);
			}
			else
			{
				mark = getLineStartOffset(mark);
				if(offset == 0)
					mouse = getLineStartOffset(mouse);
				else
					mouse = getLineEndOffset(mouse) - 1;
			}
			select(mark,mouse);
		}
	}

	class FocusHandler implements FocusListener
	{
		public void focusGained(FocusEvent evt)
		{
			setCaretVisible(true);
			focusedComponent = JEditTextArea.this;
		}

		public void focusLost(FocusEvent evt)
		{
			setCaretVisible(false);
			// so that caret undos use the most recently
			// focused text area
			//focusedComponent = null;
		}
	}

	class MouseHandler extends MouseAdapter
	{
		public void mousePressed(MouseEvent evt)
		{
			requestFocus();

			// This is not done properly sometimes
			focusedComponent = JEditTextArea.this;
			setCaretVisible(true);

			if((evt.getModifiers() & InputEvent.BUTTON3_MASK) != 0
				&& popup != null)
			{
				popup.show(painter,evt.getX(),evt.getY());
				return;
			}

			dragStartLine = yToLine(evt.getY());
			dragStartOffset = xToOffset(dragStartLine,evt.getX());
			int dot = getLineStartOffset(dragStartLine) + dragStartOffset;

			clickCount = evt.getClickCount();
			switch(clickCount)
			{
			case 1:
				doSingleClick(evt,dot);
				break;
			case 2:
				// It uses the bracket matching stuff, so
				// it can throw a BLE
				try
				{
					doDoubleClick(evt,dot);
				}
				catch(BadLocationException bl)
				{
					bl.printStackTrace();
				}
				break;
			case 3:
				doTripleClick(evt);
				break;
			}
		}

		private void doSingleClick(MouseEvent evt, int dot)
		{
			if((evt.getModifiers() & InputEvent.SHIFT_MASK) != 0)
			{
				rectSelect = (evt.getModifiers() & InputEvent.CTRL_MASK) != 0;
				select(getMarkPosition(),dot);
			}
			else
				setCaretPosition(dot);
		}

		private void doDoubleClick(MouseEvent evt, int dot) throws BadLocationException
		{
			// Ignore empty lines
			if(getLineLength(dragStartLine) == 0)
				return;

			try
			{
				int bracket = TextUtilities.findMatchingBracket(
					document,Math.max(0,dot - 1));
				if(bracket != -1)
				{
					int mark = getMarkPosition();
					// Hack
					if(bracket > mark)
					{
						bracket++;
						mark--;
					}
					select(mark,bracket);
					return;
				}
			}
			catch(BadLocationException bl)
			{
				bl.printStackTrace();
			}

			// Ok, it's not a bracket... select the word
			String lineText = getLineText(dragStartLine);
			String noWordSep = (String)document.getProperty("noWordSep");
			if(dragStartOffset == getLineLength(dragStartLine))
				dragStartOffset--;

			int wordStart = TextUtilities.findWordStart(lineText,
				dragStartOffset,noWordSep);
			int wordEnd = TextUtilities.findWordEnd(lineText,
				dragStartOffset+1,noWordSep);

			int lineStart = getLineStartOffset(dragStartLine);
			select(lineStart + wordStart,lineStart + wordEnd);
		}

		private void doTripleClick(MouseEvent evt)
		{
			select(getLineStartOffset(dragStartLine),
				getLineEndOffset(dragStartLine)-1);
		}
	}

	static class CaretUndo extends AbstractUndoableEdit
	{
		private int start;
		private int end;
		private int newStart;
		private int newEnd;

		CaretUndo(int start, int end, int newStart, int newEnd)
		{
			this.start = start;
			this.end = end;

			this.newStart = newStart;
			this.newEnd = newEnd;
		}

		public boolean isSignificant()
		{
			return false;
		}

		public String getPresentationName()
		{
			return "caret move";
		}

		public void undo() throws CannotUndoException
		{
			super.undo();

			if(focusedComponent != null)
				focusedComponent.select(start,end);
		}

		public boolean addEdit(UndoableEdit edit)
		{
			if(edit instanceof CaretUndo)
			{
//				CaretUndo cedit = (CaretUndo)edit;
//				start = cedit.start;
//				end = cedit.end;
//				newStart = cedit.start;
//				newEnd = cedit.end;
				edit.die();

				return true;
			}
			else
				return false;
		}

		public String toString()
		{
			return getPresentationName() + "[start="
				+ start + ",end=" + end + "]";
		}
	}

	static
	{
		caretTimer = new Timer(500,new CaretBlinker());
		caretTimer.setInitialDelay(500);
		caretTimer.start();
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.68  2000/06/02 02:21:06  sp
 * minor bug fixes
 *
 * Revision 1.67  2000/05/24 07:56:05  sp
 * bug fixes
 *
 * Revision 1.66  2000/05/23 04:04:52  sp
 * Marker highlight updates, next/prev-marker actions
 *
 * Revision 1.65  2000/05/22 12:05:45  sp
 * Markers are highlighted in the gutter, bug fixes
 *
 * Revision 1.64  2000/05/21 03:00:51  sp
 * Code cleanups and bug fixes
 *
 * Revision 1.63  2000/05/14 10:55:22  sp
 * Tool bar editor started, improved view registers dialog box
 *
 * Revision 1.62  2000/05/12 11:07:39  sp
 * Bug fixes, documentation updates
 *
 * Revision 1.61  2000/05/09 10:51:52  sp
 * New status bar, a few other things
 *
 * Revision 1.60  2000/05/07 05:48:30  sp
 * You can now edit several buffers side-by-side in a split view
 *
 * Revision 1.59  2000/05/06 05:53:46  sp
 * HyperSearch bug fix
 *
 * Revision 1.58  2000/05/04 10:37:04  sp
 * Wasting time
 *
 * Revision 1.57  2000/04/30 07:27:14  sp
 * Ftp VFS hacking, bug fixes
 *
 * Revision 1.56  2000/04/29 09:17:07  sp
 * VFS updates, various fixes
 *
 * Revision 1.55  2000/04/28 09:29:12  sp
 * Key binding handling improved, VFS updates, some other stuff
 *
 * Revision 1.54  2000/04/24 11:00:23  sp
 * More VFS hacking
 *
 * Revision 1.53  2000/04/18 05:56:26  sp
 * Documentation updates
 *
 * Revision 1.52  2000/04/17 06:34:24  sp
 * More focus debugging, linesChanged() tweaked
 *
 * Revision 1.51  2000/04/09 03:14:14  sp
 * Syntax token backgrounds can now be specified
 *
 * Revision 1.50  2000/04/08 02:39:33  sp
 * New Token.MARKUP type, remove Token.{CONSTANT,VARIABLE,DATATYPE}
 *
 * Revision 1.49  2000/03/27 07:31:23  sp
 * We now use Log.log() in some places instead of System.err.println, HTML mode
 * now supports <script> tags, external delegation bug fix
 *
 * Revision 1.48  2000/03/21 07:18:53  sp
 * bug fixes
 *
 * Revision 1.47  2000/03/20 03:42:55  sp
 * Smoother syntax package, opening an already open file will ask if it should be
 * reloaded, maybe some other changes
 *
 * Revision 1.46  2000/03/14 06:22:25  sp
 * Lots of new stuff
 *
 */
