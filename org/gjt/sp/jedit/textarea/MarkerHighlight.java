/*
 * MarkerHighlight.java - Paints marker highlights in the gutter
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

package org.gjt.sp.jedit.textarea;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Vector;
import org.gjt.sp.jedit.*;

public class MarkerHighlight implements TextAreaHighlight, ScrollListener
{
	public void init(JEditTextArea textArea, TextAreaHighlight next)
	{
		this.textArea = textArea;
		this.next = next;

		textArea.addScrollListener(this);
	}

	public void paintHighlight(Graphics gfx, int line, int y)
	{
		int firstLine = textArea.getFirstLine();
		line -= firstLine;

		if(line >= highlights.length)
			return;

		FontMetrics fm = textArea.getPainter().getFontMetrics();
		String str = highlights[line];
		if(str != null)
		{
			gfx.setColor(highlightColor);
			gfx.fillRect(0,line * fm.getHeight(),textArea.getGutter()
				.getWidth(),fm.getHeight());
		}

		if(next != null)
			next.paintHighlight(gfx,line,y);
	}

	public String getToolTipText(MouseEvent evt)
	{
		FontMetrics fm = textArea.getPainter().getFontMetrics();
		int line = evt.getY() / fm.getHeight();
		if(line < highlights.length)
		{
			String str = highlights[line];
			if(str != null)
				return str;
		}

		if(next != null)
			return next.getToolTipText(evt);
		else
			return null;
	}

	public void verticalScrollUpdate(ScrollEvent evt)
	{
		updateHighlight();
	}

	public void horizontalScrollUpdate(ScrollEvent evt) {}

	public void updateHighlight()
	{
		Buffer buffer = (Buffer)textArea.getDocument();
		int firstLine = textArea.getFirstLine();
		int visibleLines = textArea.getVisibleLines();

		highlights = new String[visibleLines];
		Registers.Register[] registers = Registers.getRegisters();

		for(int i = 0; i < registers.length; i++)
		{
			Object obj = registers[i];
			if(!(obj instanceof Registers.CaretRegister))
				continue;

			Registers.CaretRegister reg = (Registers.CaretRegister)obj;
			if(reg.getBuffer() == buffer)
			{
				int line = textArea.getLineOfOffset(reg.getOffset());
				if(line > firstLine && line < firstLine + visibleLines)
				{
					String str;
					if(i == '\n')
						str = "\\n";
					else if(i == '\t')
						str = "\\t";
					else
						str = String.valueOf((char)i);

					String[] args = { str };
					highlights[line - firstLine] = jEdit.getProperty(
						"view.gutter.register",args);
				}
			}
		}

		Vector markers = buffer.getMarkers();
		for(int i = 0; i < markers.size(); i++)
		{
			Marker marker = (Marker)markers.elementAt(i);
			int line = textArea.getLineOfOffset(marker.getStart());
			if(line > firstLine && line < firstLine + visibleLines)
			{
				String[] args = { marker.getName() };
				highlights[line - firstLine] = jEdit.getProperty(
					"view.gutter.marker",args);
			}
		}

		textArea.getGutter().repaint();
	}

	public Color getHighlightColor()
	{
		return highlightColor;
	}

	public void setHighlightColor(Color highlightColor)
	{
		this.highlightColor = highlightColor;
	}

	// private members
	private JEditTextArea textArea;
	private TextAreaHighlight next;

	private Color highlightColor;
	private String[] highlights;
}
