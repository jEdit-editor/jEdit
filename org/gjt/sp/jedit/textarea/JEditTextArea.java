/*
 * JEditTextArea.java - jEdit's text component
 * Copyright (C) 1999, 2000 Slava Pestov
 * Portions copyright (C) 2000 Ollie Rutherfurd
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

import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Segment;
import javax.swing.text.Utilities;
import javax.swing.undo.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.Enumeration;
import java.util.Vector;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.jedit.*;
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
	 * Adding components with this name to the text area will place
	 * them left of the horizontal scroll bar.
	 */
	public static String LEFT_OF_SCROLLBAR = "los";

	/**
	 * Creates a new JEditTextArea.
	 */
	public JEditTextArea(View view)
	{
		enableEvents(AWTEvent.FOCUS_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);

		this.view = view;

		// Initialize some misc. stuff
		painter = new TextAreaPainter(this);
		gutter = new Gutter(view,this);
		documentHandler = new DocumentHandler();
		foldHandler = new FoldHandler();
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

		horizontal.setValues(0,0,0,0);

		// this ensures that the text area's look is slightly
		// more consistent with the rest of the metal l&f.
		// while it depends on not-so-well-documented portions
		// of Swing, it only affects appearance, so future
		// breakage shouldn't matter
		if(UIManager.getLookAndFeel() instanceof MetalLookAndFeel)
		{
			setBorder(new TextAreaBorder());
			vertical.putClientProperty("JScrollBar.isFreeStanding",
				Boolean.FALSE);
			horizontal.putClientProperty("JScrollBar.isFreeStanding",
				Boolean.FALSE);
			//horizontal.setBorder(null);
		}

		// Add some event listeners
		vertical.addAdjustmentListener(new AdjustHandler());
		horizontal.addAdjustmentListener(new AdjustHandler());
		painter.addComponentListener(new ComponentHandler());

		mouseHandler = new MouseHandler();
		painter.addMouseListener(mouseHandler);
		painter.addMouseMotionListener(mouseHandler);

		addFocusListener(new FocusHandler());

		// This doesn't seem very correct, but it fixes a problem
		// when setting the initial caret position for a buffer
		// (eg, from the recent file list)
		focusedComponent = this;
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

		invalidateSelectedLines();
	}

	/**
	 * Blinks the caret.
	 */
	public final void blinkCaret()
	{
		if(caretBlinks)
		{
			blink = !blink;
			invalidateSelectedLines();
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
	 * if the number of lines in the buffer changes, or when the
	 * size of the text are changes.
	 */
	public void updateScrollBars()
	{
		if(vertical != null && visibleLines != 0)
		{
			// don't display stuff past the end of the buffer if
			// we can help it
			int lineCount = getVirtualLineCount();
			if(lineCount < firstLine + visibleLines)
			{
				// this will call updateScrollBars(), so
				// just return...
				int newFirstLine = lineCount - visibleLines;
				if(newFirstLine != firstLine
					&& newFirstLine >= 0)
				{
					setFirstLine(newFirstLine);
					return;
				}
			}

			vertical.setValues(firstLine,visibleLines,0,lineCount);
			vertical.setUnitIncrement(2);
			vertical.setBlockIncrement(visibleLines);
		}

		int width = painter.getWidth();
		if(horizontal != null && width != 0)
		{
			maxHorizontalScrollWidth = 0;
			painter.repaint();

			horizontal.setUnitIncrement(painter.getFontMetrics()
				.charWidth('w'));
			horizontal.setBlockIncrement(width / 2);
		}
	}

	/**
	 * Returns the line displayed at the text area's origin. This is
	 * a virtual, not a physical, line number.
	 */
	public final int getFirstLine()
	{
		return firstLine;
	}

	/**
	 * Sets the line displayed at the text area's origin. This is
	 * a virtual, not a physical, line number.
	 */
	public void setFirstLine(int firstLine)
	{
		if(firstLine == this.firstLine)
			return;

		_setFirstLine(firstLine);

		view.synchroScrollVertical(this,firstLine);
	}

	public void _setFirstLine(int firstLine)
	{
		this.firstLine = Math.max(0,firstLine);
		physFirstLine = buffer.virtualToPhysical(this.firstLine);

		maxHorizontalScrollWidth = 0;

		if(this.firstLine != vertical.getValue())
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
		_setHorizontalOffset(horizontalOffset);

		view.synchroScrollHorizontal(this,horizontalOffset);
	}

	public void _setHorizontalOffset(int horizontalOffset)
	{
		this.horizontalOffset = horizontalOffset;
		if(horizontalOffset != horizontal.getValue())
			updateScrollBars();
		painter.repaint();
	}

	/**
	 * @deprecated Use setFirstLine() and setHorizontalOffset() instead
	 */
	public boolean setOrigin(int firstLine, int horizontalOffset)
	{
		setFirstLine(firstLine);
		setHorizontalOffset(horizontalOffset);
		return true;
	}

	/**
	 * Centers the caret on the screen.
	 * @since jEdit 2.7pre2
	 */
	public void centerCaret()
	{
		Element map = buffer.getDefaultRootElement();

		int gotoLine = firstLine + visibleLines / 2;

		if(gotoLine < 0 || gotoLine >= map.getElementCount())
		{
			getToolkit().beep();
			return;
		}

		Element element = map.getElement(gotoLine);
		setCaretPosition(element.getStartOffset());
	}

	/**
	 * Scrolls up by one line.
	 * @since jEdit 2.7pre2
	 */
	public void scrollUpLine()
	{
		if(firstLine > 0)
			setFirstLine(firstLine-1);
		else
			getToolkit().beep();
	}

	/**
	 * Scrolls up by one page.
	 * @since jEdit 2.7pre2
	 */
	public void scrollUpPage()
	{
		if(firstLine > 0)
		{
			int newFirstLine = firstLine - visibleLines;
			setFirstLine(newFirstLine);
		}
		else
		{
			getToolkit().beep();
		}
	}

	/**
	 * Scrolls down by one line.
	 * @since jEdit 2.7pre2
	 */
	public void scrollDownLine()
	{
		int numLines = getVirtualLineCount();

		if(firstLine + visibleLines < numLines)
			setFirstLine(firstLine + 1);
		else
			getToolkit().beep();
	}

	/**
	 * Scrolls down by one page.
	 * @since jEdit 2.7pre2
	 */
	public void scrollDownPage()
	{
		int numLines = getVirtualLineCount();

		if(firstLine + visibleLines < numLines)
		{
			int newFirstLine = firstLine + visibleLines;
			setFirstLine(newFirstLine + visibleLines < numLines
				? newFirstLine : numLines - visibleLines);
		}
		else
		{
			getToolkit().beep();
		}
	}

	/**
	 * Ensures that the caret is visible by scrolling the text area if
	 * necessary.
	 * @param doElectricScroll If true, electric scrolling will be performed
	 */
	public void scrollToCaret(boolean doElectricScroll)
	{
		int caretLine = getCaretLine();
		int offset = getCaretPosition() - getLineStartOffset(caretLine);
		int virtualCaretLine = buffer.physicalToVirtual(caretLine);

		// visibleLines == 0 before the component is realized
		// we can't do any proper scrolling then, so we have
		// this hack...
		if(visibleLines == 0)
		{
			setFirstLine(caretLine - electricScroll);
			return;
		}

		int electricScroll = (doElectricScroll ? this.electricScroll : 0);

		boolean changed = false;

		int _firstLine = firstLine + electricScroll;
		int _lastLine = firstLine + visibleLines - electricScroll;
		if(virtualCaretLine > _firstLine && virtualCaretLine < _lastLine)
		{
			// vertical scroll position is correct already
		}
		else if(_firstLine - virtualCaretLine > visibleLines
			|| virtualCaretLine - _lastLine > visibleLines)
		{
			int virtualMarkLine = buffer.physicalToVirtual(getMarkLine());

			// center {markLine,caretLine} on screen
			firstLine = virtualMarkLine - (visibleLines
				- virtualCaretLine + virtualMarkLine) / 2;
 			firstLine = Math.max(virtualCaretLine - visibleLines
				+ electricScroll + 1,firstLine);
 			firstLine = Math.min(virtualCaretLine /* + visibleLines */
				- electricScroll,firstLine);

			changed = true;
		}
		else if(virtualCaretLine < _firstLine)
		{
			firstLine = Math.max(0,virtualCaretLine - electricScroll);

			changed = true;
		}
		else if(virtualCaretLine >= _lastLine)
		{
			firstLine = (virtualCaretLine - visibleLines)
				+ electricScroll + 1;
			if(firstLine >= getVirtualLineCount() - visibleLines)
				firstLine = getVirtualLineCount() - visibleLines;

			changed = true;
		}

		int x = offsetToX(caretLine,offset);
		int width = painter.getFontMetrics().charWidth('w');

		if(x < 0)
		{
			horizontalOffset = Math.min(0,horizontalOffset
				- x + width + 5);
			changed = true;
		}
		else if(x >= painter.getWidth() - width - 5)
		{
			horizontalOffset = horizontalOffset +
				(painter.getWidth() - x) - width - 5;
			changed = true;
		}

		if(changed)
		{
			if(firstLine < 0)
				firstLine = 0;

			physFirstLine = buffer.virtualToPhysical(firstLine);

			updateScrollBars();
			painter.repaint();
			gutter.repaint();

			view.synchroScrollVertical(this,firstLine);
			view.synchroScrollHorizontal(this,horizontalOffset);
		}
	}

	/**
	 * Converts a line index to a y co-ordinate. This must be a virtual,
	 * not a physical, line number.
	 * @param line The line
	 */
	public int lineToY(int line)
	{
		FontMetrics fm = painter.getFontMetrics();
		return (line - firstLine) * fm.getHeight()
			- (fm.getLeading() + fm.getDescent());
	}

	/**
	 * Converts a y co-ordinate to a virtual line index.
	 * @param y The y co-ordinate
	 */
	public int yToLine(int y)
	{
		FontMetrics fm = painter.getFontMetrics();
		int height = fm.getHeight();
		return Math.max(0,Math.min(getVirtualLineCount() - 1,
			y / height + firstLine));
	}

	/**
	 * Converts an offset in a line into an x co-ordinate.
	 * @param line The line
	 * @param offset The offset, from the start of the line
	 */
	public int offsetToX(int line, int offset)
	{
		Token tokens = buffer.markTokens(line).getFirstToken();

		FontMetrics fm = painter.getFontMetrics();

		getLineText(line,lineSegment);

		int segmentOffset = lineSegment.offset;
		int x = horizontalOffset;

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

	/**
	 * Converts an x co-ordinate to an offset within a line.
	 * @param line The line
	 * @param x The x co-ordinate
	 */
	public int xToOffset(int line, int x)
	{
		Token tokens = buffer.markTokens(line).getFirstToken();

		FontMetrics fm = painter.getFontMetrics();

		getLineText(line,lineSegment);

		char[] segmentArray = lineSegment.array;
		int segmentOffset = lineSegment.offset;
		int segmentCount = lineSegment.count;

		int width = horizontalOffset;

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

	/**
	 * Converts a point to an offset, from the start of the text.
	 * @param x The x co-ordinate of the point
	 * @param y The y co-ordinate of the point
	 */
	public int xyToOffset(int x, int y)
	{
		FontMetrics fm = painter.getFontMetrics();
		int height = fm.getHeight();
		int line = y / height + firstLine;

		if(line < 0)
			return 0;
		else if(line >= getVirtualLineCount())
			return getBufferLength();
		else
		{
			line = buffer.virtualToPhysical(line);
			return getLineStartOffset(line) + xToOffset(line,x);
		}
	}

	/**
	 * Marks a line as needing a repaint.
	 * @param line The line to invalidate
	 */
	public final void invalidateLine(int line)
	{
		line = buffer.physicalToVirtual(line);

		FontMetrics fm = painter.getFontMetrics();
		int y = lineToY(line) + fm.getDescent() + fm.getLeading();
		painter.repaint(0,y,painter.getWidth(),fm.getHeight());
		gutter.repaint(0,y,gutter.getWidth(),fm.getHeight());
	}

	/**
	 * Marks a range of lines as needing a repaint.
	 * @param firstLine The first line to invalidate
	 * @param lastLine The last line to invalidate
	 */
	public final void invalidateLineRange(int firstLine, int lastLine)
	{
		firstLine = buffer.physicalToVirtual(firstLine);
		lastLine = buffer.physicalToVirtual(lastLine);

		FontMetrics fm = painter.getFontMetrics();
		int y = lineToY(firstLine) + fm.getDescent() + fm.getLeading();
		int height = (lastLine - firstLine + 1) * fm.getHeight();
		painter.repaint(0,y,painter.getWidth(),height);
		gutter.repaint(0,y,gutter.getWidth(),height);
	}

	/**
	 * Repaints the lines containing the selection.
	 */
	public final void invalidateSelectedLines()
	{
		invalidateLineRange(selectionStartLine,selectionEndLine);
	}

	/**
	 * Returns the buffer this text area is editing.
	 */
	public final Buffer getBuffer()
	{
		return buffer;
	}

	/**
	 * Sets the buffer this text area is editing.
	 * @param buffer The buffer
	 */
	public void setBuffer(Buffer buffer)
	{
		if(this.buffer == buffer)
			return;
		if(this.buffer != null)
		{
			this.buffer.removeDocumentListener(documentHandler);
			this.buffer.removeFoldListener(foldHandler);
		}
		this.buffer = buffer;

		buffer.addDocumentListener(documentHandler);
		buffer.addFoldListener(foldHandler);
		documentHandlerInstalled = true;

		maxHorizontalScrollWidth = 0;

		painter.updateTabSize();

		select(0,0,false);
		updateScrollBars();
		painter.repaint();
		gutter.repaint();
	}

	/**
	 * Returns the length of the buffer. Equivalent to calling
	 * <code>getBuffer().getLength()</code>.
	 */
	public final int getBufferLength()
	{
		return buffer.getLength();
	}

	/**
	 * Returns the number of lines in the document.
	 */
	public final int getLineCount()
	{
		return buffer.getLineCount();
	}

	/**
	 * Returns the number of visible lines in the document (which may
	 * be less than the total due to folding).
	 * @since jEdit 3.1pre1
	 */
	public final int getVirtualLineCount()
	{
		return buffer.getVirtualLineCount();
	}

	/**
	 * Collapses the fold containing the caret.
	 * @since jEdit 3.1pre1
	 */
	public void collapseFold()
	{
		if(!buffer.collapseFoldAt(getCaretLine()))
			getToolkit().beep();
	}

	/**
	 * Expands the fold containing the caret.
	 * @since jEdit 3.1pre1
	 */
	public void expandFold()
	{
		if(!buffer.expandFoldAt(getCaretLine()))
			getToolkit().beep();
	}

	/**
	 * Returns the line containing the specified offset.
	 * @param offset The offset
	 */
	public final int getLineOfOffset(int offset)
	{
		return buffer.getDefaultRootElement().getElementIndex(offset);
	}

	/**
	 * Returns the start offset of the specified line.
	 * @param line The line
	 * @return The start offset of the specified line, or -1 if the line is
	 * invalid
	 */
	public int getLineStartOffset(int line)
	{
		Element lineElement = buffer.getDefaultRootElement()
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
		Element lineElement = buffer.getDefaultRootElement()
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
		Element lineElement = buffer.getDefaultRootElement()
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
			return buffer.getText(0,buffer.getLength());
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
			buffer.beginCompoundEdit();
			buffer.remove(0,buffer.getLength());
			buffer.insertString(0,text,null);
		}
		catch(BadLocationException bl)
		{
			bl.printStackTrace();
		}
		finally
		{
			buffer.endCompoundEdit();
		}
	}

	/**
	 * Returns the specified substring of the buffer.
	 * @param start The start offset
	 * @param len The length of the substring
	 * @return The substring, or null if the offsets are invalid
	 */
	public final String getText(int start, int len)
	{
		try
		{
			return buffer.getText(start,len);
		}
		catch(BadLocationException bl)
		{
			bl.printStackTrace();
			return null;
		}
	}

	/**
	 * Copies the specified substring of the buffer into a segment.
	 * If the offsets are invalid, the segment will contain a null string.
	 * @param start The start offset
	 * @param len The length of the substring
	 * @param segment The segment
	 */
	public final void getText(int start, int len, Segment segment)
	{
		try
		{
			buffer.getText(start,len,segment);
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
		Element lineElement = buffer.getDefaultRootElement()
			.getElement(lineIndex);
		int start = lineElement.getStartOffset();
		getText(start,lineElement.getEndOffset() - start - 1,segment);
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
			Element map = buffer.getDefaultRootElement();
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
		select(selectionStart,selectionEnd,true);
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
			Element map = buffer.getDefaultRootElement();
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
		select(selectionStart,selectionEnd,true);
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
	 * Moves the caret without moving the mark.
	 * @param mark The mark position
	 * @since jEdit 2.7pre2
	 */
	public final void moveCaretPosition(int caret)
	{
		select(getMarkPosition(),caret,true);
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
		select(caret,caret,true);
	}

	/**
	 * Selects all text in the buffer.
	 */
	public final void selectAll()
	{
		select(0,getBufferLength(),false);
	}

	/**
	 * Moves the mark to the caret position.
	 */
	public final void selectNone()
	{
		select(getCaretPosition(),getCaretPosition(),true);
	}

	/**
	 * Selects the current line.
	 * @since jEdit 2.7pre2
	 */
	public void selectLine()
	{
		int caretLine = getCaretLine();
		int start = getLineStartOffset(caretLine);
		int end = getLineEndOffset(caretLine) - 1;
		select(start,end);
	}

	/**
	 * Selects the paragraph at the caret position.
	 * @since jEdit 2.7pre2
	 */
	public void selectParagraph()
	{
		int caretLine = getCaretLine();

		if(getLineLength(caretLine) == 0)
		{
			view.getToolkit().beep();
			return;
		}

		int start = caretLine;
		int end = caretLine;

		while(start >= 0)
		{
			if(getLineLength(start) == 0)
				break;
			else
				start--;
		}

		while(end < getLineCount())
		{
			if(getLineLength(end) == 0)
				break;
			else
				end++;
		}

		select(getLineStartOffset(start + 1),
			getLineEndOffset(end - 1) - 1);
	}

	/**
	 * Selects the word at the caret position.
	 * @since jEdit 2.7pre2
	 */
	public void selectWord()
	{
		int line = getCaretLine();
		int lineStart = getLineStartOffset(line);
		int offset = getCaretPosition() - lineStart;

		if(getLineLength(line) == 0)
			return;

		String lineText = getLineText(line);
		String noWordSep = (String)buffer.getProperty("noWordSep");

		if(offset == getLineLength(line))
			offset--;

		int wordStart = TextUtilities.findWordStart(lineText,offset,noWordSep);
		int wordEnd = TextUtilities.findWordEnd(lineText,offset+1,noWordSep);

		select(lineStart + wordStart,lineStart + wordEnd);
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
		select(start,end,true);
	}

	/**
	 * Selects from the start offset to the end offset. This is the
	 * general selection method used by all other selecting methods.
	 * The caret position will be start if start &lt; end, and end
	 * if end &gt; start.
	 * @param start The start offset
	 * @param end The end offset
	 * @param doElectricScroll If true, electric scrolling will be
	 * performed
	 */
	public void select(int start, int end, boolean doElectricScroll)
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

		if(newStart < 0 || newEnd > getBufferLength())
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
			int newStartLine = getLineOfOffset(newStart);
			int newEndLine = getLineOfOffset(newEnd);

			updateBracketHighlight(newEndLine,newEnd
				- getLineStartOffset(newEndLine));

			invalidateLineRange(selectionStartLine,selectionEndLine);
			invalidateLineRange(newStartLine,newEndLine);

			buffer.addUndoableEdit(new CaretUndo(selectionStart,
				selectionEnd));

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

		if(focusedComponent == this)
			scrollToCaret(doElectricScroll);
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

			Element map = buffer.getDefaultRootElement();

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
		if(!isEditable())
		{
			throw new InternalError("Text component"
				+ " read only");
		}

		buffer.beginCompoundEdit();

		try
		{
			if(rectSelect)
			{
				Element map = buffer.getDefaultRootElement();

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

					buffer.remove(rectStart,Math.min(lineEnd - rectStart,
						end - start));

					if(selectedText == null)
						continue;

					currNewline = selectedText.indexOf('\n',lastNewline);
					if(currNewline == -1)
						currNewline = selectedText.length();

					buffer.insertString(rectStart,selectedText
						.substring(lastNewline,currNewline),null);

					lastNewline = Math.min(selectedText.length(),
						currNewline + 1);
				}

				if(selectedText != null &&
					currNewline != selectedText.length())
				{
					int offset = map.getElement(selectionEndLine)
						.getEndOffset() - 1;
					buffer.insertString(offset,"\n",null);
					buffer.insertString(offset + 1,selectedText
						.substring(currNewline + 1),null);
				}
			}
			else
			{
				buffer.remove(selectionStart,
					selectionEnd - selectionStart);
				if(selectedText != null)
				{
					buffer.insertString(selectionStart,
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
		// No matter what happends... stops us from leaving buffer
		// in a bad state
		finally
		{
			buffer.endCompoundEdit();
		}

		select(selectionEnd,selectionEnd,false);
	}

	/**
	 * Returns true if this text area is editable, false otherwise.
	 */
	public final boolean isEditable()
	{
		return buffer.isEditable();
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
	 * Indents all selected lines.
	 * @since jEdit 2.7pre2
	 */
	public void indentSelectedLines()
	{
		buffer.beginCompoundEdit();
		for(int i = selectionStartLine; i <= selectionEndLine; i++)
		{
			buffer.indentLine(i,true,true);
		}
		buffer.endCompoundEdit();
	}

	/**
	 * Handles the insertion of the specified character. Performs
	 * auto indent, expands abbreviations, does word wrap, etc.
	 * @param ch The character
	 * @see #setSelectedText(String)
	 * @see #isOverwriteEnabled()
	 * @since jEdit 2.7pre3
	 */
	public void userInput(char ch)
	{
		if(!isEditable())
		{
			getToolkit().beep();
			return;
		}

		boolean selection = (selectionStart != selectionEnd);
		int caretLine = getCaretLine();

		if(ch == ' ' && Abbrevs.getExpandOnInput()
			&& Abbrevs.expandAbbrev(view,false))
			return;
		else if(ch == '\t')
		{
			if(buffer.getBooleanProperty("indentOnTab")
				&& !selection
				&& buffer.indentLine(selectionStartLine,true,false))
				return;
			else if(buffer.getBooleanProperty("noTabs"))
			{
				int lineStart = getLineStartOffset(selectionStartLine);

				String line = getText(lineStart,selectionStart
					- lineStart);

				setSelectedText(createSoftTab(line,
					buffer.getTabSize()));
			}
			else
				setSelectedText("\t");
			return;
		}
		else if(ch == '\n')
		{
			try
			{
				buffer.beginCompoundEdit();
				setSelectedText("\n");
				if(buffer.getBooleanProperty("indentOnEnter"))
					buffer.indentLine(selectionStartLine,true,false);
			}
			finally
			{
				buffer.endCompoundEdit();
			}
			return;
		}
		else
		{
			String str = String.valueOf(ch);
			if(selection)
			{
				setSelectedText(str);
				return;
			}

			try
			{
				if(ch == ' ')
				{
					if(doWordWrap(caretLine,true))
						return;
				}
				else
					doWordWrap(caretLine,false);
			}
			catch(BadLocationException bl)
			{
				Log.log(Log.ERROR,this,bl);
			}

			try
			{
				buffer.beginCompoundEdit();

				// Don't overstrike if we're on the end of
				// the line
				int caret = getCaretPosition();
				if(overwrite)
				{
					int caretLineEnd = getLineEndOffset(caretLine);
					if(caretLineEnd - caret > 1)
						buffer.remove(caret,1);
				}

				buffer.insertString(caret,str,null);
			}
			catch(BadLocationException bl)
			{
				Log.log(Log.ERROR,this,bl);
			}
			finally
			{
				buffer.endCompoundEdit();
			}
		}

		String indentOpenBrackets = (String)buffer
			.getProperty("indentOpenBrackets");
		String indentCloseBrackets = (String)buffer
			.getProperty("indentCloseBrackets");
		if((indentCloseBrackets != null && indentCloseBrackets.indexOf(ch) != -1)
			|| (indentOpenBrackets != null && indentOpenBrackets.indexOf(ch) != -1))
		{
			buffer.indentLine(caretLine,false,true);
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
	 * Sets overwrite mode.
	 */
	public final void setOverwriteEnabled(boolean overwrite)
	{
		this.overwrite = overwrite;
		invalidateSelectedLines();
	}

	/**
	 * Toggles overwrite mode.
	 * @since jEdit 2.7pre2
	 */
	public final void toggleOverwriteEnabled()
	{
		overwrite = !overwrite;
		invalidateSelectedLines();
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
	 * @param rectSelect True if the selection should be rectangular,
	 * false otherwise.
	 */
	public final void setSelectionRectangular(boolean rectSelect)
	{
		this.rectSelect = rectSelect;
		invalidateSelectedLines();
	}

	/**
	 * Toggles rectangular selection.
	 * @since jEdit 2.7pre2
	 */
	public final void toggleSelectionRectangular()
	{
		setSelectionRectangular(!rectSelect);
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
	 * Deletes the character before the caret, or the selection, if one is
	 * active.
	 * @since jEdit 2.7pre2
	 */
	public void backspace()
	{
		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}

		if(selectionStart != selectionEnd)
		{
			setSelectedText("");
		}
		else
		{
			int caret = getCaretPosition();
			if(caret == 0)
			{
				getToolkit().beep();
				return;
			}
			try
			{
				buffer.remove(caret - 1,1);
			}
			catch(BadLocationException bl)
			{
				Log.log(Log.ERROR,this,bl);
			}
		}
	}

	/**
	 * Deletes the word before the caret.
	 * @since jEdit 2.7pre2
	 */
	public void backspaceWord()
	{
		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}

		if(selectionStart != selectionEnd)
		{
			setSelectedText("");
			return;
		}

		int line = getCaretLine();
		int lineStart = getLineStartOffset(line);
		int caret = selectionStart - lineStart;

		String lineText = getLineText(line);

		if(caret == 0)
		{
			if(lineStart == 0)
			{
				getToolkit().beep();
				return;
			}
			caret--;
		}
		else
		{
			String noWordSep = (String)buffer.getProperty("noWordSep");
			caret = TextUtilities.findWordStart(lineText,caret-1,noWordSep);
		}

		try
		{
			buffer.remove(caret + lineStart,
				selectionStart - (caret + lineStart));
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
	}

	/**
	 * Deletes the character after the caret.
	 * @since jEdit 2.7pre2
	 */
	public void delete()
	{
		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}

		if(selectionStart != selectionEnd)
		{
			setSelectedText("");
		}
		else
		{
			int caret = getCaretPosition();
			if(caret == buffer.getLength())
			{
				getToolkit().beep();
				return;
			}
			try
			{
				buffer.remove(caret,1);
			}
			catch(BadLocationException bl)
			{
				Log.log(Log.ERROR,this,bl);
			}
		}
	}

	/**
	 * Deletes from the caret to the end of the current line.
	 * @since jEdit 2.7pre2
	 */
	public void deleteToEndOfLine()
	{
		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}

		int caret = getCaretPosition();
		Element map = buffer.getDefaultRootElement();
		Element lineElement = map.getElement(
			view.getTextArea().getCaretLine());
		try
		{
			buffer.remove(caret,lineElement.getEndOffset()
				- caret - 1);
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
	}

	/**
	 * Deletes the line containing the caret.
	 * @since jEdit 2.7pre2
	 */
	public void deleteLine()
	{
		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}

		Element map = buffer.getDefaultRootElement();
		Element lineElement = map.getElement(view.getTextArea().getCaretLine());
		try
		{
			int start = lineElement.getStartOffset();
			int end = lineElement.getEndOffset();
			if(end > buffer.getLength())
			{
				if(start != 0)
					start--;
				end--;
			}
			buffer.remove(start,end - start);
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
	}

	/**
	 * Deletes the paragraph containing the caret.
	 * @since jEdit 2.7pre2
	 */
	public void deleteParagraph()
	{
		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}

		int lineNo = getCaretLine();

		int start = 0, end = buffer.getLength();

		for(int i = lineNo - 1; i >= 0; i--)
		{
			if(getLineLength(i) == 0)
			{
				start = getLineStartOffset(i);
				break;
			}
		}

		for(int i = lineNo + 1; i < getLineCount(); i++)
		{
			if(getLineLength(i) == 0)
			{
				end = getLineStartOffset(i);
				break;
			}
		}

		try
		{
			buffer.remove(start,end - start);
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
	}

	/**
	 * Deletes from the caret to the beginning of the current line.
	 * @since jEdit 2.7pre2
	 */
	public void deleteToStartOfLine()
	{
		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}

		int caret = getCaretPosition();
		Element map = buffer.getDefaultRootElement();
		Element lineElement = map.getElement(getCaretLine());

		try
		{
			buffer.remove(lineElement.getStartOffset(),
				caret - lineElement.getStartOffset());
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
	}

	/**
	 * Deletes the word in front of the caret.
	 * @since jEdit 2.7pre2
	 */
	public void deleteWord()
	{
		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}

		if(selectionStart != selectionEnd)
		{
			setSelectedText("");
			return;
		}

		int line = getCaretLine();
		int lineStart = getLineStartOffset(line);
		int caret = selectionStart - lineStart;

		String lineText = getLineText(getCaretLine());

		if(caret == lineText.length())
		{
			if(lineStart + caret == buffer.getLength())
			{
				getToolkit().beep();
				return;
			}
			caret++;
		}
		else
		{
			String noWordSep = (String)buffer.getProperty("noWordSep");
			caret = TextUtilities.findWordEnd(lineText,
				caret+1,noWordSep);
		}

		try
		{
			buffer.remove(selectionStart,(caret + lineStart) - selectionStart);
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
	}

	/**
	 * Moves the caret to the next closing bracket.
	 * @since jEdit 2.7pre2.
	 */
	public void goToNextBracket(boolean select)
	{
		int caret = getCaretPosition();

		String text = getText(caret,buffer.getLength() - caret - 1);

		boolean ok = false;

loop:		for(int i = 0; i < text.length(); i++)
		{
			switch(text.charAt(i))
			{
			case ')': case ']': case '}':
				ok = true;
				caret = caret + i + 1;
				break loop;
			}
		}

		if(!ok)
			getToolkit().beep();
		else
		{
			if(select)
				select(getMarkPosition(),caret);
			else
				setCaretPosition(caret);
		}
	}

	/**
	 * Moves the caret to the next character.
	 * @since jEdit 2.7pre2.
	 */
	public void goToNextCharacter(boolean select)
	{
		if(!select && selectionStart != selectionEnd)
		{
			setCaretPosition(selectionEnd);
			return;
		}

		int caret = getCaretPosition();
		if(caret == buffer.getLength())
			getToolkit().beep();
		else
		{
			caret++;

			if(select)
				select(getMarkPosition(),caret);
			else
				setCaretPosition(caret);
		}
	}

	/**
	 * Movse the caret to the next line.
	 * @since jEdit 2.7pre2
	 */
	public void goToNextLine(boolean select)
	{
		int caret = getCaretPosition();
		int line = getCaretLine();

		if(line == getLineCount() - 1)
		{
			getToolkit().beep();
			return;
		}

		int magic = getMagicCaretPosition();
		if(magic == -1)
		{
			magic = offsetToX(line,caret - getLineStartOffset(line));
		}

		caret = getLineStartOffset(line + 1)
			+ xToOffset(line + 1,magic + 1);
		if(select)
			select(getMarkPosition(),caret);
		else
			setCaretPosition(caret);
		setMagicCaretPosition(magic);
	}

	/**
	 * Moves the caret to the next marker.
	 * @since jEdit 2.7pre2
	 */
	public void goToNextMarker(boolean select)
	{
		int caret = getCaretPosition();
		Vector markers = buffer.getMarkers();
		Marker marker = null;

		for(int i = 0; i < markers.size(); i++)
		{
			Marker _marker = (Marker)markers.elementAt(i);
			if(_marker.getStart() > caret)
			{
				marker = _marker;
				break;
			}
		}

		if(marker == null)
			view.getToolkit().beep();
		else
		{
			caret = marker.getStart();

			if(select)
				select(getMarkPosition(),caret);
			else
				setCaretPosition(caret);
		}
	}

	/**
	 * Moves the caret to the next screenful.
	 * @since jEdit 2.7pre2.
	 */
	public void goToNextPage(boolean select)
	{
		int lineCount = getLineCount();
		int caret = getCaretPosition();
		int line = getCaretLine();

		int magic = getMagicCaretPosition();
		if(magic == -1)
		{
			magic = offsetToX(line,caret - getLineStartOffset(line));
		}

		if(firstLine + visibleLines * 2 >= lineCount - 1)
			setFirstLine(lineCount - visibleLines);
		else
			setFirstLine(firstLine + visibleLines);

		line = Math.min(lineCount - 1,line + visibleLines);
		caret = getLineStartOffset(line) + xToOffset(line,magic + 1);
		if(select)
			select(getMarkPosition(),caret);
		else
			setCaretPosition(caret);

		setMagicCaretPosition(magic);
	}

	/**
	 * Moves the caret to the start of the next paragraph.
	 * @since jEdit 2.7pre2
	 */
	public void goToNextParagraph(boolean select)
	{
		int lineNo = getCaretLine();

		int caret = getBufferLength();

		for(int i = lineNo + 1; i < getLineCount(); i++)
		{
			if(getLineLength(i) == 0)
			{
				caret = getLineStartOffset(i);
				break;
			}
		}

		if(select)
			select(getMarkPosition(),caret);
		else
			setCaretPosition(caret);
	}

	/**
	 * Moves the caret to the start of the next word.
	 * @since jEdit 2.7pre2
	 */
	public void goToNextWord(boolean select)
	{
		int caret = getCaretPosition();
		int line = getCaretLine();
		int lineStart = getLineStartOffset(line);
		caret -= lineStart;

		String lineText = getLineText(line);

		if(caret == lineText.length())
		{
			if(lineStart + caret == buffer.getLength())
			{
				getToolkit().beep();
				return;
			}

			caret++;
		}
		else
		{
			String noWordSep = (String)buffer.getProperty("noWordSep");
			caret = TextUtilities.findWordEnd(lineText,caret + 1,noWordSep);
		}

		caret += lineStart;
		if(select)
			select(getMarkPosition(),caret);
		else
			setCaretPosition(caret);
	}

	/**
	 * Moves the caret to the previous bracket.
	 * @since jEdit 2.7pre2
	 */
	public void goToPrevBracket(boolean select)
	{
		String text = getText(0,getCaretPosition());

		int caret = -1;

loop:		for(int i = getCaretPosition() - 1; i >= 0; i--)
		{
			switch(text.charAt(i))
			{
			case '(': case '[': case '{':
				caret = i;
				break loop;
			}
		}

		if(caret == -1)
			getToolkit().beep();
		else
		{
			if(select)
				select(getMarkPosition(),caret);
			else
				setCaretPosition(caret);
		}
	}

	/**
	 * Moves the caret to the previous character.
	 * @since jEdit 2.7pre2.
	 */
	public void goToPrevCharacter(boolean select)
	{
		if(!select && selectionStart != selectionEnd)
		{
			setCaretPosition(selectionStart);
			return;
		}

		int caret = getCaretPosition();
		if(caret == 0)
			getToolkit().beep();
		else
		{
			caret--;

			if(select)
				select(getMarkPosition(),caret);
			else
				setCaretPosition(caret);
		}
	}

	/**
	 * Moves the caret to the previous line.
	 * @since jEdit 2.7pre2
	 */
	public void goToPrevLine(boolean select)
	{
		int caret = getCaretPosition();
		int line = getCaretLine();

		if(line == 0)
		{
			getToolkit().beep();
			return;
		}

		int magic = getMagicCaretPosition();
		if(magic == -1)
		{
			magic = offsetToX(line,caret - getLineStartOffset(line));
		}

		caret = getLineStartOffset(line - 1) + xToOffset(line - 1,magic + 1);
		if(select)
			select(getMarkPosition(),caret);
		else
			setCaretPosition(caret);
		setMagicCaretPosition(magic);
	}

	/**
	 * Moves the caret to the previous marker.
	 * @since jEdit 2.7pre2
	 */
	public void goToPrevMarker(boolean select)
	{
		int caret = getCaretPosition();

		Vector markers = buffer.getMarkers();
		Marker marker = null;
		for(int i = markers.size() - 1; i >= 0; i--)
		{
			Marker _marker = (Marker)markers.elementAt(i);
			if(_marker.getStart() < caret)
			{
				marker = _marker;
				break;
			}
		}

		if(marker == null)
			getToolkit().beep();
		else
		{
			caret = marker.getStart();

			if(select)
				select(getMarkPosition(),caret);
			else
				setCaretPosition(caret);
		}
	}

	/**
	 * Moves the caret to the previous screenful.
	 * @since jEdit 2.7pre2
	 */
	public void goToPrevPage(boolean select)
	{
		int caret = getCaretPosition();
		int line = getCaretLine();

		if(firstLine < visibleLines)
			setFirstLine(0);
		else
			setFirstLine(firstLine - visibleLines);

		int magic = getMagicCaretPosition();
		if(magic == -1)
		{
			magic = offsetToX(line,caret - getLineStartOffset(line));
		}

		line = Math.max(0,line - visibleLines);
		caret = getLineStartOffset(line) + xToOffset(line,magic + 1);

		if(select)
			select(getMarkPosition(),caret);
		else
			setCaretPosition(caret);

		setMagicCaretPosition(magic);
	}

	/**
	 * Moves the caret to the start of the previous paragraph.
	 * @since jEdit 2.7pre2
	 */
	public void goToPrevParagraph(boolean select)
	{
		int lineNo = getCaretLine();

		int caret = 0;

		for(int i = lineNo - 1; i >= 0; i--)
		{
			if(getLineLength(i) == 0)
			{
				caret = getLineStartOffset(i);
				break;
			}
		}

		if(select)
			select(getMarkPosition(),caret);
		else
			setCaretPosition(caret);
	}

	/**
	 * Moves the caret to the start of the previous word.
	 * @since jEdit 2.7pre2
	 */
	public void goToPrevWord(boolean select)
	{
		int caret = getCaretPosition();
		int line = getCaretLine();
		int lineStart = getLineStartOffset(line);
		caret -= lineStart;

		String lineText = getLineText(line);

		if(caret == 0)
		{
			if(lineStart == 0)
			{
				view.getToolkit().beep();
				return;
			}
			caret--;
		}
		else
		{
			String noWordSep = (String)buffer.getProperty("noWordSep");
			caret = TextUtilities.findWordStart(lineText,caret - 1,noWordSep);
		}

		caret += lineStart;

		if(select)
			select(getMarkPosition(),caret);
		else
			setCaretPosition(caret);
	}

	/**
	 * On subsequent invocations, first moves the caret to the first
	 * non-whitespace character of the line, then the beginning of the
	 * line, then to the first visible line.
	 * @since jEdit 2.7pre2
	 */
	public void smartHome(boolean select)
	{
		if(!jEdit.getBooleanProperty("view.homeEnd"))
			goToStartOfLine(select);
		else
		{
			switch(view.getInputHandler().getLastActionCount())
			{
			case 1:
				goToStartOfWhiteSpace(select);
				break;
			case 2:
				goToStartOfLine(select);
				break;
			default: //case 3:
				goToFirstVisibleLine(select);
				break;
			}
		}
	}

	/**
	 * On subsequent invocations, first moves the caret to the last
	 * non-whitespace character of the line, then the end of the
	 * line, then to the last visible line.
	 * @since jEdit 2.7pre2
	 */
	public void smartEnd(boolean select)
	{
		if(!jEdit.getBooleanProperty("view.homeEnd"))
			goToEndOfLine(select);
		else
		{
			switch(view.getInputHandler().getLastActionCount())
			{
			case 1:
				goToEndOfWhiteSpace(select);
				break;
			case 2:
				goToEndOfLine(select);
				break;
			default: //case 3:
				goToLastVisibleLine(select);
				break;
			}
		}
	}

	/**
	 * Moves the caret to the beginning of the current line.
	 * @since jEdit 2.7pre2
	 */
	public void goToStartOfLine(boolean select)
	{
		// do this here, for weird reasons
		Macros.Recorder recorder = view.getMacroRecorder();
		if(recorder != null)
			recorder.record("textArea.goToStartOfLine(" + select + ");");

		if(select)
		{
			select(getMarkPosition(),getLineStartOffset(
				getCaretLine()));
		}
		else
			setCaretPosition(getLineStartOffset(getCaretLine()));
	}

	/**
	 * Moves the caret to the end of the current line.
	 * @since jEdit 2.7pre2
	 */
	public void goToEndOfLine(boolean select)
	{
		// do this here, for weird reasons
		Macros.Recorder recorder = view.getMacroRecorder();
		if(recorder != null)
			recorder.record("textArea.goToEndOfLine(" + select + ");");

		if(select)
		{
			select(getMarkPosition(),getLineEndOffset(
				getCaretLine()) - 1);
		}
		else
			setCaretPosition(getLineEndOffset(getCaretLine()) - 1);
	}

	/**
	 * Moves the caret to the first non-whitespace character of the current
	 * line.
	 * @since jEdit 2.7pre2
	 */
	public void goToStartOfWhiteSpace(boolean select)
	{
		// do this here, for weird reasons
		Macros.Recorder recorder = view.getMacroRecorder();
		if(recorder != null)
			recorder.record("textArea.goToStartOfWhiteSpace(" + select + ");");

		int line = getCaretLine();
		int firstIndent = MiscUtilities.getLeadingWhiteSpace(getLineText(line));
		int firstOfLine = getLineStartOffset(line);

		firstIndent = firstOfLine + firstIndent;
		if(firstIndent == getLineEndOffset(line) - 1)
			firstIndent = firstOfLine;

		if(select)
			select(getMarkPosition(),firstIndent);
		else
			setCaretPosition(firstIndent);
	}

	/**
	 * Moves the caret to the last non-whitespace character of the current
	 * line.
	 * @since jEdit 2.7pre2
	 */
	public void goToEndOfWhiteSpace(boolean select)
	{
		// do this here, for weird reasons
		Macros.Recorder recorder = view.getMacroRecorder();
		if(recorder != null)
			recorder.record("textArea.goToEndOfWhiteSpace(" + select + ");");

		int line = getCaretLine();
		int lastIndent = MiscUtilities.getTrailingWhiteSpace(getLineText(line));
		int lastOfLine = getLineEndOffset(line) - 1;

		lastIndent = lastOfLine - lastIndent;
		if(lastIndent == getLineStartOffset(line))
			lastIndent = lastOfLine;

		if(select)
			select(getMarkPosition(),lastIndent);
		else
			setCaretPosition(lastIndent);
	}

	/**
	 * Moves the caret to the first visible line.
	 * @since jEdit 2.7pre2
	 */
	public void goToFirstVisibleLine(boolean select)
	{
		// do this here, for weird reasons
		Macros.Recorder recorder = view.getMacroRecorder();
		if(recorder != null)
			recorder.record("textArea.goToFirstVisibleLine(" + select + ");");

		int firstVisibleLine = (firstLine <= electricScroll) ? 0 :
			firstLine + electricScroll;
		if(firstVisibleLine >= getLineCount())
			firstVisibleLine = getLineCount() - 1;

		int firstVisible = getLineEndOffset(firstVisibleLine) - 1;

		if(select)
			select(getMarkPosition(),firstVisible);
		else
			setCaretPosition(firstVisible);
	}

	/**
	 * Moves the caret to the last visible line.
	 * @since jEdit 2.7pre2
	 */
	public void goToLastVisibleLine(boolean select)
	{
		// do this here, for weird reasons
		Macros.Recorder recorder = view.getMacroRecorder();
		if(recorder != null)
			recorder.record("textArea.goToLastVisibleLine(" + select + ");");

		int lastVisibleLine = firstLine + visibleLines;

		if(lastVisibleLine >= getLineCount())
			lastVisibleLine = getLineCount() - 1;
		else if(lastVisibleLine <= electricScroll)
			lastVisibleLine = 0;
		else
			lastVisibleLine -= electricScroll;

		int lastVisible = getLineEndOffset(lastVisibleLine) - 1;

		if(select)
			select(getMarkPosition(),lastVisible);
		else
			setCaretPosition(lastVisible);
	}

	/**
	 * Prepends each line of the selection with the block comment string.
	 * @since jEdit 2.7pre2
	 */
	public void blockComment()
	{
		String comment = (String)buffer.getProperty("blockComment");
		if(!buffer.isEditable() || comment == null || comment.length() == 0)
		{
			getToolkit().beep();
			return;
		}

		comment = comment + ' ';

		buffer.beginCompoundEdit();

		try
		{
			for(int i = selectionStartLine; i <= selectionEndLine; i++)
			{
				buffer.insertString(getLineStartOffset(i),
					comment,null);
			}
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
		finally
		{
			buffer.endCompoundEdit();
		}

		selectNone();
	}

	/**
	 * Adds comment start and end strings to the beginning and end of the
	 * selection, and box comment strings to the beginning of each line of
	 * the selection.
	 * @since jEdit 2.7pre2
	 */
	public void boxComment()
	{
		String commentStart = (String)buffer.getProperty("commentStart");
		String commentEnd = (String)buffer.getProperty("commentEnd");
		String boxComment = (String)buffer.getProperty("boxComment");
		if(!buffer.isEditable() || commentStart == null
			|| commentEnd == null || boxComment == null
			|| commentStart.length() == 0 || commentEnd.length() == 0
			|| boxComment.length() == 0)
		{
			getToolkit().beep();
			return;
		}

		commentStart = commentStart + ' ';
		commentEnd = ' ' + commentEnd;
		boxComment = boxComment + ' ';

		Element map = buffer.getDefaultRootElement();

		buffer.beginCompoundEdit();

		try
		{
			Element lineElement = map.getElement(selectionStartLine);
			int start = lineElement.getStartOffset();

			buffer.insertString(start,commentStart,null);
			for(int i = selectionStartLine + 1; i <= selectionEndLine; i++)
			{
				lineElement = map.getElement(i);
				start = lineElement.getStartOffset();
				buffer.insertString(start,boxComment,null);
			}
			lineElement = map.getElement(selectionEndLine);
			int end = lineElement.getEndOffset() - 1;
			buffer.insertString(end,commentEnd,null);
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
		finally
		{
			buffer.endCompoundEdit();
		}

		selectNone();
	}

	/**
	 * Adds comment start and end strings to the beginning and end of the
	 * selection.
	 * @since jEdit 2.7pre2
	 */
	public void wingComment()
	{
		String commentStart = (String)buffer.getProperty("commentStart");
		String commentEnd = (String)buffer.getProperty("commentEnd");
		if(!buffer.isEditable() || commentStart == null || commentEnd == null
			|| commentStart.length() == 0 || commentEnd.length() == 0)
		{
			getToolkit().beep();
			return;
		}

		commentStart = commentStart + ' ';
		commentEnd = ' ' + commentEnd;

		buffer.beginCompoundEdit();
		try
		{
			buffer.insertString(selectionStart,commentStart,null);
			buffer.insertString(selectionEnd,commentEnd,null);
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
		finally
		{
			buffer.endCompoundEdit();
		}

		selectNone();
	}

	/**
	 * Formats the paragraph containing the caret.
	 * @since jEdit 2.7pre2
	 */
	public void formatParagraph()
	{
		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}
		int maxLineLength = ((Integer)buffer.getProperty("maxLineLen"))
			.intValue();
		if(maxLineLength <= 0)
		{
			getToolkit().beep();
			return;
		}

		String text = getSelectedText();
		if(text != null)
			setSelectedText(TextUtilities.format(text,maxLineLength));
		else
		{
			int lineNo = getCaretLine();

			int start = 0, end = buffer.getLength();

			for(int i = lineNo - 1; i >= 0; i--)
			{
				if(getLineLength(i) == 0)
				{
					start = getLineStartOffset(i);
					break;
				}
			}

			for(int i = lineNo + 1; i < getLineCount(); i++)
			{
				if(getLineLength(i) == 0)
				{
					end = getLineStartOffset(i);
					break;
				}
			}
			try
			{
				buffer.beginCompoundEdit();

				text = buffer.getText(start,end - start);
				buffer.remove(start,end - start);
				buffer.insertString(start,TextUtilities.format(
					text,maxLineLength),null);
			}
			catch(BadLocationException bl)
			{
				return;
			}
			finally
			{
				buffer.endCompoundEdit();
			}
		}
	}

	/**
	 * Converts spaces to tabs in the selection.
	 * @since jEdit 2.7pre2
	 */
	public void spacesToTabs()
	{
		if(!buffer.isEditable() || selectionStart == selectionEnd)
                {
                	getToolkit().beep();
                	return;
                }

		setSelectedText(TextUtilities.spacesToTabs(getSelectedText(),
			buffer.getTabSize()));
	}

	/**
	 * Converts tabs to spaces in the selection.
	 * @since jEdit 2.7pre2
	 */
	public void tabsToSpaces()
	{
		if(!buffer.isEditable() || selectionStart == selectionEnd)
                {
                	getToolkit().beep();
                	return;
                }

		setSelectedText(TextUtilities.tabsToSpaces(getSelectedText(),
			buffer.getTabSize()));
	}

	/**
	 * Converts the selected text to upper case.
	 * @since jEdit 2.7pre2
	 */
	public void toUpperCase()
	{
		if(!buffer.isEditable() || selectionStart == selectionEnd)
			getToolkit().beep();
		else
			setSelectedText(getSelectedText().toUpperCase());
	}

	/**
	 * Converts the selected text to lower case.
	 * @since jEdit 2.7pre2
	 */
	public void toLowerCase()
	{
		if(!buffer.isEditable() || selectionStart == selectionEnd)
			getToolkit().beep();
		else
			setSelectedText(getSelectedText().toLowerCase());
	}

	/**
	 * Removes trailing whitespace from all lines in the selection.
	 * @since jEdit 2.7pre2
	 */
	public void removeTrailingWhiteSpace()
	{
		if(!buffer.isEditable())
			getToolkit().beep();
		else
		{
			buffer.removeTrailingWhiteSpace(selectionStartLine,
				selectionEndLine);
		}
	}

	/**
	 * Shifts the indent to the left.
	 * @since jEdit 2.7pre2
	 */
	public void shiftIndentLeft()
	{
		if(!buffer.isEditable())
			getToolkit().beep();
		else
		{
			buffer.shiftIndentLeft(selectionStartLine,
				selectionEndLine);
		}
	}

	/**
	 * Shifts the indent to the right.
	 * @since jEdit 2.7pre2
	 */
	public void shiftIndentRight()
	{
		if(!buffer.isEditable())
			getToolkit().beep();
		else
		{
			buffer.shiftIndentRight(selectionStartLine,
				selectionEndLine);
		}
	}

	/**
	 * Joins the current and the next line.
	 * @since jEdit 2.7pre2
	 */
	public void joinLines()
	{
		Element map = buffer.getDefaultRootElement();
		int lineNo = getCaretLine();
		Element lineElement = map.getElement(lineNo);
		int start = lineElement.getStartOffset();
		int end = lineElement.getEndOffset();
		if(end > buffer.getLength())
		{
			getToolkit().beep();
			return;
		}
		Element nextLineElement = map.getElement(lineNo+1);
		int nextStart = nextLineElement.getStartOffset();
		int nextEnd = nextLineElement.getEndOffset();
		try
		{
			buffer.remove(end - 1,MiscUtilities.getLeadingWhiteSpace(
				buffer.getText(nextStart,nextEnd - nextStart)) + 1);
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
	}

	/**
	 * Moves the caret to the bracket matching the one before the caret.
	 * @since jEdit 2.7pre3
	 */
	public void goToMatchingBracket()
	{
		int line = getCaretLine();
		int dot = getCaretPosition() - getLineStartOffset(line);

		try
		{
			int bracket = TextUtilities.findMatchingBracket(
				buffer,line,Math.max(0,dot - 1));
			if(bracket != -1)
			{
				setCaretPosition(bracket + 1);
				return;
			}
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}

		getToolkit().beep();
	}

	// Eliminates lots of switch() statements
	private final String openBrackets = "([{";
	private final String closeBrackets = ")]}";

	/**
	 * Selects the code block surrounding the caret.
	 * @since jEdit 2.7pre2
	 */
	public void selectBlock()
	{
		int start = selectionStart;
		int end = selectionEnd;
		String text = getText(0,buffer.getLength());

		// Scan backwards, trying to find a bracket
		int count = 1;
		char openBracket = '\0';
		char closeBracket = '\0';

		// We can't do the backward scan if start == 0
		if(start == 0)
		{
			view.getToolkit().beep();
			return;
		}

backward_scan:	while(--start > 0)
		{
			char c = text.charAt(start);
			int index = openBrackets.indexOf(c);
			if(index != -1)
			{
				if(--count == 0)
				{
					openBracket = c;
					closeBracket = closeBrackets.charAt(index);
					break backward_scan;
				}
			}
			else if(closeBrackets.indexOf(c) != -1)
				count++;
		}

		// Reset count
		count = 1;

		// Scan forward, matching that bracket
		if(openBracket == '\0')
		{
			getToolkit().beep();
			return;
		}
		else
		{
forward_scan:		do
			{
				char c = text.charAt(end);
				if(c == closeBracket)
				{
					if(--count == 0)
					{
						end++;
						break forward_scan;
					}
				}
				else if(c == openBracket)
					count++;
			}
			while(++end < buffer.getLength());
		}

		select(start,end);
	}

	/**
	 * Displays the 'go to line' dialog box, and moves the caret to the
	 * specified line number.
	 * @since jEdit 2.7pre2
	 */
	public void showGoToLineDialog()
	{
		String line = GUIUtilities.input(view,"goto-line",null);
		if(line == null)
			return;

		try
		{
			int lineNumber = Integer.parseInt(line) - 1;
			setCaretPosition(getLineStartOffset(lineNumber));
		}
		catch(Exception e)
		{
			getToolkit().beep();
		}
	}

	/**
	 * Displays the 'select line range' dialog box, and selects the
	 * specified range of lines.
	 * @since jEdit 2.7pre2
	 */
	public void showSelectLineRangeDialog()
	{
		new SelectLineRange(view);
	}

	/**
	 * Displays the 'word count' dialog box.
	 * @since jEdit 2.7pre2
	 */
	public void showWordCountDialog()
	{
		String selection = getSelectedText();
		if(selection != null)
		{
			doWordCount(view,selection);
			return;
		}
		try
		{
			doWordCount(view,buffer.getText(0,buffer.getLength()));
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
	}

	/**
	 * Displays the 'set marker' dialog box.
	 * @since jEdit 2.7pre2
	 */
	public void showSetMarkerDialog()
	{
		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}

		String marker = GUIUtilities.input(view,"setmarker",
			getSelectedText());
		if(marker != null)
		{
			buffer.addMarker(marker,getSelectionStart(),
				getSelectionEnd());
		}
	}

	/**
	 * Attempts to complete the word at the caret position, by searching
	 * the buffer for words that start with the currently entered text. If
	 * only one completion is found, it is inserted immediately, otherwise
	 * a popup is shown will all possible completions.
	 * @since jEdit 2.7pre2
	 */
	public void completeWord()
	{
		String noWordSep = (String)buffer.getProperty("noWordSep");
		if(noWordSep == null)
			noWordSep = "";
		if(!buffer.isEditable())
		{
			getToolkit().beep();
			return;
		}

		// first, we get the word before the caret

		int lineIndex = getCaretLine();
		String line = getLineText(lineIndex);
		int dot = getCaretPosition() - getLineStartOffset(lineIndex);
		if(dot == 0)
		{
			getToolkit().beep();
			return;
		}

		int wordStart = TextUtilities.findWordStart(line,dot-1,noWordSep);
		String word = line.substring(wordStart,dot);
		if(word.length() == 0)
		{
			getToolkit().beep();
			return;
		}

		Vector completions = new Vector();
		int wordLen = word.length();

		// now loop through all lines of current buffer
		for(int i = 0; i < getLineCount(); i++)
		{
			line = getLineText(i);

			// check for match at start of line

			if(line.startsWith(word))
			{
				if(i == lineIndex && wordStart == 0)
					continue;

				String _word = completeWord(line,0,noWordSep);
				if(_word.length() != wordLen)
				{
					// remove duplicates
					if(completions.indexOf(_word) == -1)
						completions.addElement(_word);
				}
			}

			// check for match inside line
			int len = line.length() - word.length();
			for(int j = 0; j < len; j++)
			{
				char c = line.charAt(j);
				if(!Character.isLetterOrDigit(c) && noWordSep.indexOf(c) == -1)
				{
					if(i == lineIndex && wordStart == (j + 1))
						continue;

					if(line.regionMatches(j + 1,word,0,wordLen))
					{
						String _word = completeWord(line,j + 1,noWordSep);
						if(_word.length() != wordLen)
						{
							// remove duplicates
							if(completions.indexOf(_word) == -1)
								completions.addElement(_word);
						}
					}
				}
			}
		}

		// sort completion list

		MiscUtilities.quicksort(completions,new MiscUtilities.StringICaseCompare());

		if(completions.size() == 0)
			getToolkit().beep();
		// if there is only one competion, insert in buffer
		else if(completions.size() == 1)
		{
			// chop off 'wordLen' because that's what's already
			// in the buffer
			setSelectedText(((String)completions
				.elementAt(0)).substring(wordLen));
		}
		// show dialog box if > 1
		else
		{
			Point location = new Point(offsetToX(lineIndex,wordStart),
				painter.getFontMetrics().getHeight()
				* (lineIndex - firstLine + 1));
			SwingUtilities.convertPointToScreen(location,painter);
			new CompleteWord(view,word,completions,location);
		}
	}

	/**
	 * Called by the AWT when this component is added to a parent.
	 * Adds document listener.
	 */
	public void addNotify()
	{
		super.addNotify();

		ToolTipManager.sharedInstance().registerComponent(painter);
		ToolTipManager.sharedInstance().registerComponent(gutter);

		if(!documentHandlerInstalled)
		{
			documentHandlerInstalled = true;
			buffer.addDocumentListener(documentHandler);
			buffer.addFoldListener(foldHandler);
		}

		recalculateVisibleLines();
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
		ToolTipManager.sharedInstance().unregisterComponent(gutter);

		if(focusedComponent == this)
			focusedComponent = null;

		if(documentHandlerInstalled)
		{
			buffer.removeDocumentListener(documentHandler);
			buffer.removeFoldListener(foldHandler);
			documentHandlerInstalled = false;
		}
	}

	/**
	 * Bug workarounds.
	 * @since jEdit 2.7pre1
	 */
	public boolean hasFocus()
	{
		Component c = this;
		while(!(c instanceof Window))
		{
			if(c == null)
				return false;
			c = c.getParent();
		}

		Component focusOwner = ((Window)c).getFocusOwner();
		boolean hasFocus = (focusOwner == this);
		if(hasFocus && focusedComponent != this)
			focusedComponent = this;
		return hasFocus;
	}

	/**
	 * Bug workarounds.
	 * @since jEdit 2.7pre1
	 */
	public void grabFocus()
	{
		super.grabFocus();
		// ensure that focusedComponent is set correctly
		hasFocus();
	}

	// package-private members
	Segment lineSegment;
	MouseHandler mouseHandler;
	int maxHorizontalScrollWidth;

	/**
	 * Returns true if the caret is visible, false otherwise.
	 */
	final boolean isCaretVisible()
	{
		return blink && hasFocus();
	}

	/**
	 * Returns true if the line and bracket is visible, false otherwise.
	 */
	final boolean isHighlightVisible()
	{
		return hasFocus();
	}

	/**
	 * Recalculates the number of visible lines. This should not
	 * be called directly.
	 */
	void recalculateVisibleLines()
	{
		if(painter == null)
			return;
		int height = painter.getHeight();
		int lineHeight = painter.getFontMetrics().getHeight();
		visibleLines = height / lineHeight;
		updateScrollBars();
	}

	void updateMaxHorizontalScrollWidth()
	{
		int _maxHorizontalScrollWidth = buffer.getMaxLineWidth(
			firstLine,visibleLines);
		if(_maxHorizontalScrollWidth != maxHorizontalScrollWidth)
		{
			maxHorizontalScrollWidth = _maxHorizontalScrollWidth;
			horizontal.setValues(-horizontalOffset,painter.getWidth(),
				0,maxHorizontalScrollWidth
				+ painter.getFontMetrics().charWidth('w'));
		}
	}

	// protected members
	protected void processKeyEvent(KeyEvent evt)
	{
		evt = KeyEventWorkaround.processKeyEvent(evt);
		if(evt == null)
			return;

		// Ignore
		if(view.isClosed())
			return;

		InputHandler inputHandler = view.getInputHandler();
		KeyListener keyEventInterceptor = view.getKeyEventInterceptor();
		switch(evt.getID())
		{
		case KeyEvent.KEY_TYPED:
			if(keyEventInterceptor != null)
				keyEventInterceptor.keyTyped(evt);
			else
				inputHandler.keyTyped(evt);
			break;
		case KeyEvent.KEY_PRESSED:
			if(keyEventInterceptor != null)
				keyEventInterceptor.keyPressed(evt);
			else
				inputHandler.keyPressed(evt);
			break;
		case KeyEvent.KEY_RELEASED:
			if(keyEventInterceptor != null)
				keyEventInterceptor.keyReleased(evt);
			else
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

	private static Timer caretTimer;
	private static JEditTextArea focusedComponent;

	private View view;
	private Gutter gutter;
	private TextAreaPainter painter;

	private JPopupMenu popup;

	private EventListenerList listenerList;
	private MutableCaretEvent caretEvent;

	private boolean caretBlinks;
	private boolean blink;

	private int firstLine;
	private int physFirstLine; // only used when fold structure changes

	private int visibleLines;
	private int electricScroll;

	private int horizontalOffset;

	private JScrollBar vertical;
	private JScrollBar horizontal;
	private boolean scrollBarsInitialized;

	private Buffer buffer;
	private DocumentHandler documentHandler;
	private FoldHandler foldHandler;
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

	// for event handlers only
	private int clickCount;

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

	private String createSoftTab(String line, int tabSize)
	{
		int pos = 0;

		for(int i = 0; i < line.length(); i++)
		{
			switch(line.charAt(pos))
			{
			case '\t':
				pos = 0;
				break;
			default:
				if(++pos >= tabSize)
					pos = 0;
				break;
			}
		}

		return MiscUtilities.createWhiteSpace(tabSize - pos,0);
	}

	private boolean doWordWrap(int line, boolean spaceInserted)
		throws BadLocationException
	{
		int maxLineLen = ((Integer)buffer.getProperty("maxLineLen"))
			.intValue();

		if(maxLineLen <= 0)
			return false;

		Element lineElement = buffer.getDefaultRootElement()
			.getElement(line);
		int start = lineElement.getStartOffset();
		int end = lineElement.getEndOffset();
		int len = end - start - 1;

		// don't wrap unless we're at the end of the line
		if(getCaretPosition() != end - 1)
			return false;

		int tabSize = buffer.getTabSize();

		String wordBreakChars = (String)buffer.getProperty("wordBreakChars");

		buffer.getText(start,len,lineSegment);

		int lineStart = lineSegment.offset;
		int logicalLength = 0; // length with tabs expanded
		int lastWordOffset = -1;
		boolean lastWasSpace = true;
		boolean initialWhiteSpace = true;
		int initialWhiteSpaceLength = 0;
		for(int i = 0; i < len; i++)
		{
			char ch = lineSegment.array[lineStart + i];
			if(ch == '\t')
			{
				if(initialWhiteSpace)
					initialWhiteSpaceLength = i + 1;
				logicalLength += tabSize - (logicalLength % tabSize);
				if(!lastWasSpace && logicalLength <= maxLineLen)
				{
					lastWordOffset = i;
					lastWasSpace = true;
				}
			}
			else if(ch == ' ')
			{
				if(initialWhiteSpace)
					initialWhiteSpaceLength = i + 1;
				logicalLength++;
				if(!lastWasSpace && logicalLength <= maxLineLen)
				{
					lastWordOffset = i;
					lastWasSpace = true;
				}
			}
			else if(wordBreakChars != null && wordBreakChars.indexOf(ch) != -1)
			{
				initialWhiteSpace = false;
				logicalLength++;
				if(!lastWasSpace && logicalLength <= maxLineLen)
				{
					lastWordOffset = i;
					lastWasSpace = true;
				}
			}
			else
			{
				initialWhiteSpace = false;
				logicalLength++;
				lastWasSpace = false;
			}

			int insertNewLineAt;
			if(spaceInserted && logicalLength == maxLineLen)
				insertNewLineAt = end - 1;
			else if(logicalLength >= maxLineLen && lastWordOffset != -1)
				insertNewLineAt = lastWordOffset + start;
			else
				continue;

			// if the first non-whitespace string of the
			// line is the blockComment or boxComment
			// string, insert that string on the next
			// line as well
			String nextLineStart = null;
			String blockComment = (String)buffer.getProperty("blockComment");
			if(blockComment != null)
			{
				char[] blockCommentChars = blockComment.toCharArray();
				if(TextUtilities.regionMatches(true,lineSegment,
					    lineSegment.offset + initialWhiteSpaceLength,
					    blockCommentChars))
					    nextLineStart = blockComment;
			}

			String boxComment = (String)buffer.getProperty("boxComment");
			if(boxComment != null)
			{
				char[] boxCommentChars = boxComment.toCharArray();
				if(TextUtilities.regionMatches(true,lineSegment,
					    lineSegment.offset + initialWhiteSpaceLength,
					    boxCommentChars))
					    nextLineStart = boxComment;
			}

			try
			{
				buffer.beginCompoundEdit();
				buffer.insertString(insertNewLineAt,"\n",null);
				if(nextLineStart != null)
				{
					buffer.insertString(insertNewLineAt + 1,
						nextLineStart,null);
				}
				buffer.indentLine(line + 1,true,true);
			}
			finally
			{
				buffer.endCompoundEdit();
			}

			return true;
		}

		return false;
	}

	private void doWordCount(View view, String text)
	{
		char[] chars = text.toCharArray();
		int characters = chars.length;
		int words;
		if(characters == 0)
			words = 0;
		else
			words = 1;
		int lines = 1;
		boolean word = false;
		for(int i = 0; i < chars.length; i++)
		{
			switch(chars[i])
			{
			case '\r': case '\n':
				lines++;
			case ' ': case '\t':
				if(word)
				{
					words++;
					word = false;
				}
				break;
			default:
				word = true;
				break;
			}
		}
		Object[] args = { new Integer(characters), new Integer(words),
			new Integer(lines) };
		GUIUtilities.message(view,"wordcount",args);
	}

	// return word that starts at 'offset'
	private String completeWord(String line, int offset, String noWordSep)
	{
		// '+ 1' so that findWordEnd() doesn't pick up the space at the start
		int wordEnd = TextUtilities.findWordEnd(line,offset + 1,noWordSep);
		return line.substring(offset,wordEnd);
	}

	private void updateBracketHighlight(int line, int offset)
	{
		if(!painter.isBracketHighlightEnabled())
			return;

		if(bracketLine != -1)
			invalidateLine(bracketLine);

		if(offset == 0)
		{
			bracketPosition = bracketLine = -1;
			return;
		}

		try
		{
			int bracketOffset = TextUtilities.findMatchingBracket(
				buffer,line,offset - 1,
				physFirstLine,buffer.virtualToPhysical(
					Math.min(getVirtualLineCount(),
					firstLine + visibleLines)));
			if(bracketOffset != -1)
			{
				bracketLine = getLineOfOffset(bracketOffset);
				bracketPosition = bracketOffset
					- getLineStartOffset(bracketLine);
				invalidateLine(bracketLine);
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
			buffer.getDefaultRootElement());

		int count;
		if(ch == null)
			count = 0;
		else
			count = ch.getChildrenAdded().length -
				ch.getChildrenRemoved().length;

		int line = getLineOfOffset(evt.getOffset());
		if(count == 0)
			invalidateLine(line);
		// do magic stuff
		else if(line < firstLine)
		{
			setFirstLine(firstLine + count);
			// calls updateScrollBars()
		}
		// end of magic stuff
		else
		{
			updateScrollBars();
			invalidateLineRange(line,firstLine + visibleLines);
		}
	}

	static class TextAreaBorder extends AbstractBorder
	{
		private static final Insets insets = new Insets(1, 1, 2, 2);

		public void paintBorder(Component c, Graphics g, int x, int y,
			int width, int height)
		{
			g.translate(x,y);

			g.setColor(MetalLookAndFeel.getControlDarkShadow());
			g.drawRect(0,0,width-2,height-2);
			g.setColor(MetalLookAndFeel.getControlHighlight());

			g.drawLine(width-1,1,width-1,height-1);
			g.drawLine(1,height-1,width-1,height-1);

			g.setColor(MetalLookAndFeel.getControl());
			g.drawLine(width-2,2,width-2,2);
			g.drawLine(1,height-2,1,height-2);

			g.translate(-x,-y);
		}

		public Insets getBorderInsets(Component c)
		{
			return new Insets(1,1,2,2);
		}
	}

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
			else if(name.equals(LEFT_OF_SCROLLBAR))
				leftOfScrollBar = comp;
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
			else if(leftOfScrollBar == comp)
				leftOfScrollBar = null;
		}

		public Dimension preferredLayoutSize(Container parent)
		{
			Dimension dim = new Dimension();
			Border border = getBorder();
			Insets insets;
			if(border == null)
				insets = new Insets(0,0,0,0);
			else
			{
				insets = getBorder().getBorderInsets(
					JEditTextArea.this);
			}

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
			Border border = getBorder();
			Insets insets;
			if(border == null)
				insets = new Insets(0,0,0,0);
			else
			{
				insets = getBorder().getBorderInsets(
					JEditTextArea.this);
			}

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
			Border border = getBorder();
			Insets insets;
			if(border == null)
				insets = new Insets(0,0,0,0);
			else
			{
				insets = getBorder().getBorderInsets(
					JEditTextArea.this);
			}

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

			if(leftOfScrollBar != null)
			{
				Dimension dim = leftOfScrollBar.getPreferredSize();
				leftOfScrollBar.setBounds(ileft,
					itop + centerHeight,
					dim.width,
					bottomHeight);
				ileft += dim.width;
			}

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
		Component leftOfScrollBar;
	}

	static class CaretBlinker implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(focusedComponent != null && focusedComponent.hasFocus())
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

			if(!buffer.isLoaded())
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
				select(newStart,newEnd,true);
			else
			{
				int caretLine = getCaretLine();
				updateBracketHighlight(caretLine,getCaretPosition()
					- getLineStartOffset(caretLine));
			}
		}

		public void removeUpdate(DocumentEvent evt)
		{
			documentChanged(evt);

			if(!buffer.isLoaded())
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
				select(newStart,newEnd,false);
			else
			{
				int caretLine = getCaretLine();
				updateBracketHighlight(caretLine,getCaretPosition()
					- getLineStartOffset(caretLine));
			}
		}

		public void changedUpdate(DocumentEvent evt) {}
	}

	class FoldHandler implements Buffer.FoldListener
	{
		public void foldLevelsChanged(int firstLine, int lastLine)
		{
			invalidateLineRange(firstLine,lastLine);
		}

		public void foldStructureChanged()
		{
			// recalculate first line
			setFirstLine(buffer.physicalToVirtual(physFirstLine));

			// update scroll bars because the number of
			// virtual lines might have changed
			updateScrollBars();

			// repaint gutter and painter
			gutter.repaint();
			painter.repaint();
		}
	}

	class FocusHandler implements FocusListener
	{
		public void focusGained(FocusEvent evt)
		{
			invalidateSelectedLines();

			// repaint the gutter so that the border color
			// reflects the focus state
			view.updateGutterBorders();
		}

		public void focusLost(FocusEvent evt)
		{
			invalidateSelectedLines();
		}
	}

	class MouseHandler extends MouseAdapter implements MouseMotionListener
	{
		public void mousePressed(MouseEvent evt)
		{
			buffer.endCompoundEdit();

			grabFocus();

			blink = true;
			invalidateSelectedLines();

			if((evt.getModifiers() & InputEvent.BUTTON3_MASK) != 0
				&& popup != null)
			{
				if(popup.isVisible())
					popup.setVisible(false);
				else
					popup.show(painter,evt.getX()+1,evt.getY()+1);
				return;
			}

			int x = evt.getX();
			int y = evt.getY();

			dragStartLine = buffer.virtualToPhysical(yToLine(y));
			dragStartOffset = xToOffset(dragStartLine,x);
			int dot = xyToOffset(x,y);

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
			default: //case 3:
				doTripleClick(evt);
				break;
			}
		}

		private void doSingleClick(MouseEvent evt, int dot)
		{
			if((evt.getModifiers() & InputEvent.SHIFT_MASK) != 0)
			{
				rectSelect = (evt.getModifiers() & InputEvent.CTRL_MASK) != 0;
				select(getMarkPosition(),dot,false);
			}
			else
				select(dot,dot,false);
		}

		private void doDoubleClick(MouseEvent evt, int dot) throws BadLocationException
		{
			// Ignore empty lines
			if(getLineLength(dragStartLine) == 0)
				return;

			try
			{
				int bracket = TextUtilities.findMatchingBracket(
					buffer,dragStartLine,
					Math.max(0,dragStartOffset - 1));

				if(bracket != -1)
				{
					int mark = getMarkPosition();
					// Hack
					if(bracket < mark)
					{
						bracket++;
						mark--;
					}
					select(mark,bracket,false);
					return;
				}
			}
			catch(BadLocationException bl)
			{
				bl.printStackTrace();
			}

			// Ok, it's not a bracket... select the word
			String lineText = getLineText(dragStartLine);
			String noWordSep = (String)buffer.getProperty("noWordSep");
			if(dragStartOffset == getLineLength(dragStartLine))
				dragStartOffset--;

			int wordStart = TextUtilities.findWordStart(lineText,
				dragStartOffset,noWordSep);
			int wordEnd = TextUtilities.findWordEnd(lineText,
				dragStartOffset+1,noWordSep);

			int lineStart = getLineStartOffset(dragStartLine);
			select(lineStart + wordStart,lineStart + wordEnd,false);
		}

		private void doTripleClick(MouseEvent evt)
		{
			select(getLineStartOffset(dragStartLine),
				getLineEndOffset(dragStartLine),false);
		}

		public void mouseDragged(MouseEvent evt)
		{
			if((evt.getModifiers() & InputEvent.BUTTON3_MASK) != 0
				|| (popup != null && popup.isVisible()))
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
			default: //case 3:
				doTripleDrag(evt);
				break;
			}
		}

		public void mouseMoved(MouseEvent evt) {}

		private void doSingleDrag(MouseEvent evt)
		{
			select(getMarkPosition(),xyToOffset(
				evt.getX(),evt.getY()),false);
		}

		private void doDoubleDrag(MouseEvent evt)
		{
			int markLineStart = getLineStartOffset(dragStartLine);
			int markLineLength = getLineLength(dragStartLine);
			int mark = dragStartOffset;

			int line = buffer.virtualToPhysical(yToLine(evt.getY()));
			int lineStart = getLineStartOffset(line);
			int lineLength = getLineLength(line);
			int offset = xToOffset(line,evt.getX());

			String lineText = getLineText(line);
			String markLineText = getLineText(dragStartLine);
			String noWordSep = (String)buffer.getProperty("noWordSep");

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

			select(markLineStart + mark,lineStart + offset,false);
		}

		private void doTripleDrag(MouseEvent evt)
		{
			int mark = getMarkLine();
			int mouse = buffer.virtualToPhysical(yToLine(evt.getY()));
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
			select(mark,mouse,false);
		}
	}

	static class CaretUndo extends AbstractUndoableEdit
	{
		private int start;
		private int end;

		CaretUndo(int start, int end)
		{
			this.start = start;
			this.end = end;
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
			{
				int length = focusedComponent
					.getBuffer().getLength();
				if(start <= length && end <= length)
					focusedComponent.select(start,end,true);
				else
					Log.log(Log.WARNING,this,
						start + " or " + end
						+ " > " + length + "??!!");
			}
		}

		public boolean addEdit(UndoableEdit edit)
		{
			if(edit instanceof CaretUndo)
			{
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
