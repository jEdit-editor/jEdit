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

public class MarkerHighlight implements TextAreaHighlight
{
	public void init(JEditTextArea textArea, TextAreaHighlight next)
	{
		this.textArea = textArea;
		this.next = next;
	}

	public void paintHighlight(Graphics gfx, int line, int y)
	{
		if(highlightEnabled)
		{
			Color color = getHighlightColor(line);
			if(color != null)
			{
				int firstLine = textArea.getFirstLine();
				line -= firstLine;

				FontMetrics fm = textArea.getPainter().getFontMetrics();
				gfx.setColor(color);
				gfx.fillRect(0,line * fm.getHeight(),textArea.getGutter()
					.getWidth(),fm.getHeight());
			}
		}

		if(next != null)
			next.paintHighlight(gfx,line,y);
	}

	public String getToolTipText(MouseEvent evt)
	{
		if(highlightEnabled)
		{
			FontMetrics fm = textArea.getPainter().getFontMetrics();
			int line = textArea.getFirstLine() + evt.getY() / fm.getHeight();
			String tooltip = getLineToolTip(line);
			if(tooltip != null)
				return tooltip;
		}

		if(next != null)
			return next.getToolTipText(evt);
		else
			return null;
	}

	public Color getMarkerHighlightColor()
	{
		return markerHighlightColor;
	}

	public void setMarkerHighlightColor(Color markerHighlightColor)
	{
		this.markerHighlightColor = markerHighlightColor;
	}

	public Color getRegisterHighlightColor()
	{
		return registerHighlightColor;
	}

	public void setRegisterHighlightColor(Color registerHighlightColor)
	{
		this.registerHighlightColor = registerHighlightColor;
	}

	public boolean isHighlightEnabled()
	{
		return highlightEnabled;
	}

	public void setHighlightEnabled(boolean highlightEnabled)
	{
		this.highlightEnabled = highlightEnabled;
	}

	// private members
	private JEditTextArea textArea;
	private TextAreaHighlight next;
	private boolean highlightEnabled;

	private Color markerHighlightColor, registerHighlightColor;

	private Color getHighlightColor(int line)
	{
		Buffer buffer = textArea.getBuffer();
		Vector registers = Registers.getCaretRegisters();

		line = buffer.virtualToPhysical(line);

		for(int i = 0; i < registers.size(); i++)
		{
			Registers.CaretRegister reg = (Registers.CaretRegister)
				registers.elementAt(i);

			if(reg.getBuffer() == buffer)
			{
				if(line == textArea.getLineOfOffset(reg.getOffset()))
					return registerHighlightColor;
			}
		}

		Vector markers = buffer.getMarkers();
		for(int i = 0; i < markers.size(); i++)
		{
			Marker marker = (Marker)markers.elementAt(i);
			if(line == textArea.getLineOfOffset(marker.getStart()))
				return markerHighlightColor;
		}

		return null;
	}

	private String getLineToolTip(int line)
	{
		Buffer buffer = textArea.getBuffer();
		Registers.Register[] registers = Registers.getRegisters();

		line = buffer.virtualToPhysical(line);

		for(int i = 0; i < registers.length; i++)
		{
			Object obj = registers[i];
			if(!(obj instanceof Registers.CaretRegister))
				continue;

			Registers.CaretRegister reg = (Registers.CaretRegister)obj;

			if(reg.getBuffer() == buffer)
			{
				if(line == textArea.getLineOfOffset(reg.getOffset()))
				{
					String str;
					if(i == '\n')
						str = "\\n";
					else if(i == '\t')
						str = "\\t";
					else
						str = String.valueOf((char)i);

					String[] args = { str };
					return jEdit.getProperty("view.gutter.register",args);
				}
			}
		}

		Vector markers = buffer.getMarkers();
		for(int i = 0; i < markers.size(); i++)
		{
			Marker marker = (Marker)markers.elementAt(i);
			if(line == textArea.getLineOfOffset(marker.getStart()))
			{
				String[] args = { marker.getName() };
				return jEdit.getProperty("view.gutter.marker",args);
			}
		}

		return null;
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.7  2001/01/26 03:46:56  sp
 * Folding is now in a minimally useful state
 *
 * Revision 1.6  2001/01/23 09:23:48  sp
 * code cleanups, misc tweaks
 *
 * Revision 1.5  2000/07/22 03:27:03  sp
 * threaded I/O improved, autosave rewrite started
 *
 * Revision 1.4  2000/07/14 06:00:45  sp
 * bracket matching now takes syntax info into account
 *
 * Revision 1.3  2000/06/12 02:43:30  sp
 * pre6 almost ready
 *
 * Revision 1.2  2000/05/23 04:04:53  sp
 * Marker highlight updates, next/prev-marker actions
 *
 */
