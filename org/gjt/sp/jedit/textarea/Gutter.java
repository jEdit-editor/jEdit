/*
 * Gutter.java
 * Copyright (C) 1999 mike dillon
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.UIManager;

import javax.swing.border.Border;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

public class Gutter extends JComponent
{
	public Gutter(JEditTextArea textArea, int width)
	{
		this.textArea = textArea;
		setGutterWidth(width);
		setBorder(defaultBorder);
		setForeground(UIManager.getColor("Label.foreground"));
		Font labelFont = UIManager.getFont("Label.font");
		setFont(new Font(labelFont.getName(), labelFont.getStyle(), 8));
		addMouseListener(new GutterMouseListener());
	}

	public void paintComponent(Graphics gfx)
	{
		if (!collapsed)
		{
			Font defaultFont = getFont();
			gfx.setFont(defaultFont);

			// paint custom highlights, if there are any
			if (highlights != null) paintCustomHighlights(gfx);

			// paint line numbers, if they are enabled
			if (lineNumberingEnabled) paintLineNumbers(gfx);
		}
	}

	protected void paintLineNumbers(Graphics gfx)
	{
		int lineHeight = textArea.getPainter().getFontMetrics()
			.getHeight();
		int baseline = (fontHeight + lineHeight) / 2 -
			(fontHeight + lineHeight) % 2;

		int firstLine = textArea.getFirstLine() + 1;
		int lastLine = firstLine + textArea.getVisibleLines();

		int firstValidLine = (int) Math.max(1, firstLine);
		int lastValidLine = (int) Math.min(textArea.getLineCount(),
			lastLine);

		gfx.setColor(normal);

		String number;

		for (int line = firstLine; line < lastLine;
			line++, baseline += lineHeight)
		{
			// only print numbers for valid lines
			if (line < firstValidLine || line > lastValidLine)
				continue;

			number = Integer.toString(line);

			if (interval > 1 && line % interval == 0)
			{
				gfx.setColor(bright);
				gfx.drawString(number, ileft, baseline);
				gfx.setColor(normal);
			}
			else
			{
				gfx.drawString(number, ileft, baseline);
			}
		}
	}

	protected void paintCustomHighlights(Graphics gfx)
	{
		int lineHeight = textArea.getPainter().getFontMetrics()
			.getHeight();

		int firstLine = textArea.getFirstLine();
		int lastLine = firstLine + textArea.getVisibleLines();

		int y = 0;

		for (int line = firstLine; line < lastLine;
			line++, y += lineHeight)
		{
			highlights.paintHighlight(gfx, line, y);
		}
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
	 * JComponent.setFont(Font) is overridden here to cache the height of
	 * the font. This avoids having to get the font metrics during every
	 * repaint.
	 */
	public void setFont(Font font)
	{
		super.setFont(font);

		FontMetrics fm = getFontMetrics(font);
		fontHeight = fm.getHeight();
	}

	/*
	 * Component.setForeground(Color) is overridden here to cache both the
	 * foreground color and the Color obtained by calling brighter() on the
	 * supplied color.
	 */
	public void setForeground(Color fore)
	{
		super.setForeground(fore);

		normal = fore;
		bright = normal.brighter();
	}

	public void setGutterWidth(int width)
	{
		if (width < 0) width = 0;

		gutterSize.width = width;

		// if the gutter is expanded, ask the text area to revalidate
		// the layout to resize the gutter
		if (!collapsed) textArea.revalidate();
	}

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

	// private members
	private static final Border defaultBorder =
		new CompoundBorder(new EmptyBorder(0,1,0,3),
		new BevelBorder(BevelBorder.LOWERED));

	// the JEditTextArea this gutter is attached to
	private JEditTextArea textArea;

	private TextAreaHighlight highlights;

	private int fontHeight = 0;
	private int ileft = 0;

	private Dimension gutterSize = new Dimension(0,0);
	private Dimension collapsedSize = new Dimension(0,0);

	private Color normal;
	private Color bright;

	private int interval = 0;
	private boolean lineNumberingEnabled = true;
	private boolean collapsed = false;

	class GutterMouseListener extends MouseAdapter
	{
		public void mouseClicked(MouseEvent e)
		{
			if (e.getClickCount() >= 2)
			{
				toggleCollapsed();
			}
		}
	}
}
