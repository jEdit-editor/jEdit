/*
 * print.java
 * Copyright (C) 1998, 1999 Slava Pestov
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

package org.gjt.sp.jedit.actions;

import javax.swing.text.*;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.util.Date;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.EditAction;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;

public class print extends EditAction
{
	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		Buffer buffer = view.getBuffer();

		PrintJob job = view.getToolkit().getPrintJob(view,buffer.getName(),null);
		if(job == null)
			return;

		view.showWaitCursor();

		int topMargin;
		int leftMargin;
		int bottomMargin;
		int rightMargin;
		int ppi = job.getPageResolution();

		try
		{
			topMargin = (int)(Float.valueOf(jEdit.getProperty(
				"buffer.margin.top")).floatValue() * ppi);
		}
		catch(NumberFormatException nf)
		{
			topMargin = ppi / 2;
		}
		try
		{
			leftMargin = (int)(Float.valueOf(jEdit.getProperty(
				"buffer.margin.left")).floatValue() * ppi);
		}
		catch(NumberFormatException nf)
		{
			leftMargin = ppi / 2;
		}
		try
		{
			bottomMargin = (int)(Float.valueOf(jEdit.getProperty(
				"buffer.margin.bottom")).floatValue() * ppi);
		}
		catch(NumberFormatException nf)
		{
			bottomMargin = topMargin;
		}
		try
		{
			rightMargin = (int)(Float.valueOf(jEdit.getProperty(
				"buffer.margin.right")).floatValue() * ppi);
		}
		catch(NumberFormatException nf)
		{
			rightMargin = leftMargin;
		}

		String header = buffer.getPath();
		String footer = new Date().toString();

		JEditTextArea textArea = view.getTextArea();

		Segment lineSegment = new Segment();
		TokenMarker tokenMarker = textArea.getTokenMarker();
		SyntaxStyle[] styles = textArea.getPainter().getStyles();
		TabExpander expander = null;

		Graphics gfx = null;
		Font font = textArea.getPainter().getFont();
		FontMetrics fm = null;
		Dimension pageDimension = job.getPageDimension();
		int pageWidth = pageDimension.width;
		int pageHeight = pageDimension.height;
		int y = 0;
		int tabSize = 0;
		int lineHeight = 0;
		int page = 0;

		for(int i = 0; i < textArea.getLineCount(); i++)
		{
			if(gfx == null)
			{
				page++;

				gfx = job.getGraphics();
				gfx.setFont(font);
				fm = gfx.getFontMetrics();
				lineHeight = fm.getHeight();
				tabSize = buffer.getTabSize() * fm.charWidth(' ');
				expander = new PrintTabExpander(leftMargin,tabSize);
				gfx.setColor(Color.lightGray);
				gfx.fillRect(leftMargin,topMargin,pageWidth
					- leftMargin - rightMargin,lineHeight);
				gfx.setColor(Color.black);
				y = topMargin + lineHeight - fm.getDescent()
					- fm.getLeading();
				gfx.drawString(header,leftMargin,y);
				y += lineHeight;
			}

			y += lineHeight;
			textArea.getLineText(i,lineSegment);

			if(tokenMarker == null)
			{
				gfx.setColor(Color.black);
				gfx.setFont(font);
				Utilities.drawTabbedText(lineSegment,leftMargin,
					y,gfx,expander,0);
			}
			else
			{
				gfx.setColor(Color.black);
				gfx.setFont(font);
				Token tokens = tokenMarker.markTokens(lineSegment,i);
				SyntaxUtilities.paintSyntaxLine(lineSegment,
					tokens,styles,expander,gfx,Color.white,
					leftMargin,y);
			}

			if((y >= pageHeight - bottomMargin - lineHeight * 3) ||
				(i == textArea.getLineCount() - 1))
			{
				y = pageHeight - bottomMargin;

				gfx.setColor(Color.lightGray);
				gfx.setFont(font);
				gfx.fillRect(leftMargin,y - lineHeight,pageWidth
					- leftMargin - rightMargin,lineHeight);
				gfx.setColor(Color.black);
				y -= (lineHeight - fm.getAscent());
				gfx.drawString(footer,leftMargin,y);

				Integer[] args = { new Integer(page) };
				String pageStr = jEdit.getProperty("print.page",args);
				int width = fm.stringWidth(pageStr);
				gfx.drawString(pageStr,pageWidth - rightMargin
					- width,y);

				gfx.dispose();
				gfx = null;
			}
		}

		job.end();

		view.hideWaitCursor();
	}

	class PrintTabExpander implements TabExpander
	{
		private int leftMargin;
		private int tabSize;

		public PrintTabExpander(int leftMargin, int tabSize)
		{
			this.leftMargin = leftMargin;
			this.tabSize = tabSize;
		}

		public float nextTabStop(float x, int tabOffset)
		{
			int ntabs = ((int)x - leftMargin) / tabSize;
			return (ntabs + 1) * tabSize + leftMargin;
		}
	}
}
