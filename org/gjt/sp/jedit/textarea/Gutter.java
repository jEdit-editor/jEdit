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
import javax.swing.border.Border;

public class Gutter extends JComponent implements SwingConstants
{
	public Gutter(JEditTextArea textArea)
	{
		this.textArea = textArea;

		setDoubleBuffered(true);

		GutterMouseListener ml = new GutterMouseListener();
		addMouseListener(ml);
		addMouseMotionListener(ml);
	}

	public void paintComponent(Graphics gfx)
	{
		if (!collapsed)
		{
			// fill the background
			Rectangle r = gfx.getClipBounds();
			gfx.setColor(getBackground());
			gfx.fillRect(r.x, r.y, r.width, r.height);

			// if document is loading, don't paint anything
			if (!textArea.getDocument().isLoaded()) return;

			// paint custom highlights, if there are any
			if (highlights != null) paintCustomHighlights(gfx);

			// paint line numbers, if they are enabled
			if (lineNumberingEnabled) paintLineNumbers(gfx);
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
	 * @param color The border color
	 */
	public void setBorder(int width, Color color)
	{
		setBorder(BorderFactory.createMatteBorder(0,0,0,width,color));
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
			collapsedSize.height = insets.top + insets.bottom;
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

	/**
	 * Set the width of the expanded gutter
	 * @param width The gutter width
	 */
	public void setGutterWidth(int width)
	{
		if (width < collapsedSize.width) width = collapsedSize.width;

		gutterSize.width = width;

		// if the gutter is expanded, ask the text area to revalidate
		// the layout to resize the gutter
		if (!collapsed) textArea.revalidate();
	}

	/**
	 * Get the width of the expanded gutter
	 * @return The gutter width
	 */
	public int getGutterWidth()
	{
		return gutterSize.width;
	}

	/*
	 * Component.getPreferredSize() is overridden here to support the
	 * collapsing behavior.
	 */
	public Dimension getPreferredSize()
	{
		if (collapsed)
		{
			return collapsedSize;
		}
		else
		{
			return gutterSize;
		}
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
	 * Identifies whether or not the line numbers are drawn in the gutter
	 * @return true if the line numbers are drawn, false otherwise
	 */
	public boolean isLineNumberingEnabled()
	{
		return lineNumberingEnabled;
	}

	/**
	 * Turns the line numbering on or off and causes the gutter to be
	 * repainted.
	 * @param enabled true if line numbers are drawn, false otherwise
	 */
	public void setLineNumberingEnabled(boolean enabled)
	{
		if (lineNumberingEnabled == enabled) return;

		lineNumberingEnabled = enabled;

		repaint();
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
	 * @return true if the gutter is collapsed, false if it is expanded
	 */
	public boolean isCollapsed()
	{
		return collapsed;
	}

	/**
	 * Sets whether the gutter is collapsed or expanded and force the text
	 * area to update its layout if there is a change.
	 * @param collapsed true if the gutter is collapsed,
	 *                   false if it is expanded
	 */
	public void setCollapsed(boolean collapsed)
	{
		if (this.collapsed == collapsed) return;

		this.collapsed = collapsed;

		textArea.revalidate();
	}

	/**
	 * Toggles whether the gutter is collapsed or expanded.
	 */
	public void toggleCollapsed()
	{
		setCollapsed(!collapsed);
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

	public JPopupMenu getContextMenu()
	{
		return context;
	}

	public void setContextMenu(JPopupMenu context)
	{
		this.context = context;
	}

	// private members
	// the JEditTextArea this gutter is attached to
	private JEditTextArea textArea;

	private JPopupMenu context;

	private TextAreaHighlight highlights;

	private int baseline = 0;
	private int ileft = 0;

	private Dimension gutterSize = new Dimension(0,0);
	private Dimension collapsedSize = new Dimension(0,0);

	private Color intervalHighlight;
	private Color currentLineHighlight;

	private FontMetrics fm;

	private int alignment;

	private int interval = 0;
	private boolean lineNumberingEnabled = true;
	private boolean currentLineHighlightEnabled = false;
	private boolean collapsed = false;

	class GutterMouseListener extends MouseAdapter
		implements MouseMotionListener
	{
		public void mouseClicked(MouseEvent e)
		{
			int count = e.getClickCount();
			if (count == 1)
			{
				if (context == null || context.isVisible())
					return;

				if ((e.getModifiers() & InputEvent.BUTTON3_MASK)
					!= 0)
				{
					//XXX this is a hack to make sure the
					//XXX actions get the right text area
					textArea.requestFocus();

					context.show(Gutter.this,
						e.getX(), e.getY());
				}
			}
			else if (count >= 2)
			{
				toggleCollapsed();
			}
		}

		public void mousePressed(MouseEvent e)
		{
			dragStart = e.getPoint();

			startWidth = gutterSize.width;
		}

		public void mouseDragged(MouseEvent e)
		{
			if (dragStart == null) return;

			if (isCollapsed()) setCollapsed(false);

			Point p = e.getPoint();

			gutterSize.width = startWidth + p.x - dragStart.x;

			if (gutterSize.width < collapsedSize.width)
			{
				gutterSize.width = startWidth;
				setCollapsed(true);
			}

			SwingUtilities.invokeLater(new Runnable() {
				public void run()
				{
					textArea.revalidate();
				}
			});
		}

		public void mouseExited(MouseEvent e)
		{
			if (dragStart != null && dragStart.x > e.getPoint().x)
			{
				setCollapsed(true);
				gutterSize.width = startWidth;

				SwingUtilities.invokeLater(new Runnable() {
					public void run()
					{
						textArea.revalidate();
					}
				});
			}

			//dragStart = null;
		}

		public void mouseMoved(MouseEvent e) {}

		public void mouseReleased(MouseEvent e)
		{
			dragStart = null;
		}

		private Point dragStart = null;
		private int startWidth = 0;
	}
}
