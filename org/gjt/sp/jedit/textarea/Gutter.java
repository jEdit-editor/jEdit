/*
 * Gutter.java
 * Copyright (C) 1999, 2000 mike dillon
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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import org.gjt.sp.jedit.*;

public class Gutter extends JComponent implements SwingConstants
{
	public Gutter(View view, JEditTextArea textArea)
	{
		this.view = view;
		this.textArea = textArea;

		setDoubleBuffered(true);

		MouseHandler ml = new MouseHandler();
		addMouseListener(ml);
		addMouseMotionListener(ml);
	}

	public void paintComponent(Graphics gfx)
	{
		if (expanded)
		{
			// fill the background
			Rectangle r = gfx.getClipBounds();
			gfx.setColor(getBackground());
			gfx.fillRect(r.x, r.y, r.width, r.height);

			// if buffer is loading, don't paint anything
			if (!textArea.getBuffer().isLoaded())
				return;

			// paint custom highlights, if there are any
			if (highlights != null) paintCustomHighlights(gfx);

			// paint line numbers
			paintLineNumbers(gfx);
		}
	}

	private void paintLineNumbers(Graphics gfx)
	{
		FontMetrics pfm = textArea.getPainter().getFontMetrics();
		int lineHeight = pfm.getHeight();

		Rectangle clip = gfx.getClipBounds();

		int baseline = (clip.y - clip.y % lineHeight) + (int) Math.round(
			(this.baseline + lineHeight - pfm.getDescent()) / 2.0);

		int firstLine = clip.y / lineHeight + textArea.getFirstLine() + 1;
		int lastLine = firstLine + clip.height / lineHeight;
		int caretLine = textArea.getCaretLine() + 1;

		int firstValidLine = firstLine > 1 ? firstLine : 1;
		int lastValidLine = (lastLine > textArea.getLineCount())
			? textArea.getLineCount() : lastLine;

		boolean highlightCurrentLine = currentLineHighlightEnabled
			&& (textArea.getSelectionStart() == textArea.getSelectionEnd());

		gfx.setFont(getFont());

		Color fg = getForeground();
		Color hfg = getHighlightedForeground();
		Color clfg = getCurrentLineForeground();

		String number;
		int offset;

		for (int line = firstLine; line <= lastLine;
			line++, baseline += lineHeight)
		{
			// only print numbers for valid lines
			if (line < firstValidLine || line > lastValidLine)
				continue;

			number = Integer.toString(line);

			switch (alignment)
			{
			case RIGHT:
				offset = gutterSize.width - collapsedSize.width
					- (fm.stringWidth(number) + 1);
				break;
			case CENTER:
				offset = ((gutterSize.width - collapsedSize.width)
					- fm.stringWidth(number)) / 2;
				break;
			case LEFT: default:
				offset = 1;
			}

			if (line == caretLine && highlightCurrentLine)
			{
				gfx.setColor(clfg);
			}
			else if (interval > 1 && line % interval == 0)
			{
				gfx.setColor(hfg);
			}
			else
			{
				gfx.setColor(fg);
			}

			gfx.drawString(number, ileft + offset, baseline);
		}
	}

	private void paintCustomHighlights(Graphics gfx)
	{
		int lineHeight = textArea.getPainter().getFontMetrics()
			.getHeight();

		int firstLine = textArea.getFirstLine();
		int lastLine = firstLine + (getHeight() / lineHeight);

		int y = 0;

		for (int line = firstLine; line < lastLine;
			line++, y += lineHeight)
		{
			highlights.paintHighlight(gfx, line, y);
		}
	}

	/**
	* Marks a line as needing a repaint.
	* @param line The line to invalidate
	*/
	public final void invalidateLine(int line)
	{
		if(!expanded)
			return;

		FontMetrics pfm = textArea.getPainter().getFontMetrics();
		repaint(0,textArea.lineToY(line) + pfm.getDescent() + pfm.getLeading(),
			getWidth(),pfm.getHeight());
	}

	/**
	* Marks a range of lines as needing a repaint.
	* @param firstLine The first line to invalidate
	* @param lastLine The last line to invalidate
	*/
	public final void invalidateLineRange(int firstLine, int lastLine)
	{
		if(!expanded)
			return;

		FontMetrics pfm = textArea.getPainter().getFontMetrics();
		repaint(0,textArea.lineToY(firstLine) + pfm.getDescent() + pfm.getLeading(),
			getWidth(),(lastLine - firstLine + 1) * pfm.getHeight());
	}

	/**
	 * Adds a custom highlight painter.
	 * @param highlight The highlight
	 */
	public void addCustomHighlight(TextAreaHighlight highlight)
	{
		highlight.init(textArea, highlights);
		highlights = highlight;
	}

	/**
	 * Convenience method for setting a default matte border on the right
	 * with the specified border width and color
	 * @param width The border width (in pixels)
	 * @param color1 The focused border color
	 * @param color2 The unfocused border color
	 * @param color3 The gutter/text area gap color
	 */
	public void setBorder(int width, Color color1, Color color2, Color color3)
	{
		this.borderWidth = width;

		focusBorder = new CompoundBorder(new MatteBorder(0,0,0,width,color3),
			new MatteBorder(0,0,0,width,color1));
		noFocusBorder = new CompoundBorder(new MatteBorder(0,0,0,width,color3),
			new MatteBorder(0,0,0,width,color2));
		updateBorder();
	}

	/**
	 * Sets the border differently if the text area has focus or not.
	 */
	public void updateBorder()
	{
		// because we are called from the text area's focus handler,
		// we do an invokeLater() so that the view's focus handler
		// has a chance to execute and set the edit pane properly
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				if(view.getEditPane() == null)
					return;

				if(view.getEditPane().getTextArea() == textArea)
					setBorder(focusBorder);
				else
					setBorder(noFocusBorder);
			}
		});
	}

	/*
	 * JComponent.setBorder(Border) is overridden here to cache the left
	 * inset of the border (if any) to avoid having to fetch it during every
	 * repaint.
	 */
	public void setBorder(Border border)
	{
		super.setBorder(border);

		if (border == null)
		{
			ileft = 0;
			collapsedSize.width = 0;
			collapsedSize.height = 0;
		}
		else
		{
			Insets insets = border.getBorderInsets(this);
			ileft = insets.left;
			collapsedSize.width = insets.left + insets.right;
			collapsedSize.height = gutterSize.height
				= insets.top + insets.bottom;
			gutterSize.width = insets.left + insets.right
				+ fm.stringWidth("12345");
		}
	}

	/*
	 * JComponent.setFont(Font) is overridden here to cache the baseline for
	 * the font. This avoids having to get the font metrics during every
	 * repaint.
	 */
	public void setFont(Font font)
	{
		super.setFont(font);

		fm = getFontMetrics(font);

		baseline = fm.getAscent();
	}

	/**
	 * Get the foreground color for highlighted line numbers
	 * @return The highlight color
	 */
	public Color getHighlightedForeground()
	{
		return intervalHighlight;
	}

	public void setHighlightedForeground(Color highlight)
	{
		intervalHighlight = highlight;
	}

	public Color getCurrentLineForeground()
 	{
		return currentLineHighlight;
	}

	public void setCurrentLineForeground(Color highlight)
	{
		currentLineHighlight = highlight;
 	}

	/*
	 * Component.getPreferredSize() is overridden here to support the
	 * collapsing behavior.
	 */
	public Dimension getPreferredSize()
	{
		if (expanded)
			return gutterSize;
		else
			return collapsedSize;
	}

	public Dimension getMinimumSize()
	{
		return getPreferredSize();
	}

	public String getToolTipText(MouseEvent evt)
	{
		return (highlights == null) ? null :
			highlights.getToolTipText(evt);
	}

	/**
	 * Identifies whether the horizontal alignment of the line numbers.
	 * @return Gutter.RIGHT, Gutter.CENTER, Gutter.LEFT
	 */
	public int getLineNumberAlignment()
	{
		return alignment;
	}

	/**
	 * Sets the horizontal alignment of the line numbers.
	 * @param alignment Gutter.RIGHT, Gutter.CENTER, Gutter.LEFT
	 */
	public void setLineNumberAlignment(int alignment)
	{
		if (this.alignment == alignment) return;

		this.alignment = alignment;

		repaint();
	}

	/**
	 * Identifies whether the gutter is collapsed or expanded.
	 * @return true if the gutter is expanded, false if it is collapsed
	 */
	public boolean isExpanded()
	{
		return expanded;
	}

	/**
	 * Sets whether the gutter is collapsed or expanded and force the text
	 * area to update its layout if there is a change.
	 * @param collapsed true if the gutter is expanded,
	 *                   false if it is collapsed
	 */
	public void setExpanded(boolean expanded)
	{
		if (this.expanded == expanded) return;

		this.expanded = expanded;

		textArea.revalidate();
	}

	/**
	 * Toggles whether the gutter is collapsed or expanded.
	 */
	public void toggleExpanded()
	{
		setExpanded(!expanded);
	}

	/**
	 * Sets the number of lines between highlighted line numbers.
	 * @return The number of lines between highlighted line numbers or
	 *          zero if highlighting is disabled
	 */
	public int getHighlightInterval()
	{
		return interval;
	}

	/**
	 * Sets the number of lines between highlighted line numbers. Any value
	 * less than or equal to one will result in highlighting being disabled.
	 * @param interval The number of lines between highlighted line numbers
	 */
	public void setHighlightInterval(int interval)
	{
		if (interval <= 1) interval = 0;
		this.interval = interval;
		repaint();
	}

	public boolean isCurrentLineHighlightEnabled()
	{
		return currentLineHighlightEnabled;
	}

	public void setCurrentLineHighlightEnabled(boolean enabled)
	{
		if (currentLineHighlightEnabled == enabled) return;

		currentLineHighlightEnabled = enabled;

		repaint();
	}

	// private members
	private View view;
	private JEditTextArea textArea;

	private TextAreaHighlight highlights;

	private int baseline;
	private int ileft;

	private Dimension gutterSize = new Dimension(0,0);
	private Dimension collapsedSize = new Dimension(0,0);

	private Color intervalHighlight;
	private Color currentLineHighlight;

	private FontMetrics fm;

	private int alignment;

	private int interval;
	private boolean currentLineHighlightEnabled;
	private boolean expanded;

	private int borderWidth;
	private Border focusBorder, noFocusBorder;

	class MouseHandler extends MouseAdapter implements MouseMotionListener
	{
		public void mousePressed(MouseEvent e)
		{
			if(e.getClickCount() == 2)
				toggleExpanded();
			else
			{
				e.translatePoint(-getWidth(),0);
				textArea.mouseHandler.mousePressed(e);
			}
		}

		public void mouseDragged(MouseEvent e)
		{
			e.translatePoint(-getWidth(),0);
			textArea.mouseHandler.mouseDragged(e);
		}

		public void mouseMoved(MouseEvent e) {}

		public void mouseReleased(MouseEvent e)
		{
			e.translatePoint(-getWidth(),0);
			textArea.mouseHandler.mouseReleased(e);
		}
	}
}
