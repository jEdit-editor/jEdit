/*
 * TextRenderingManager.java - Abstract differences between AWT and Java 2D
 * Copyright (C) 2001 Slava Pestov
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

import javax.swing.text.TabExpander;
import java.awt.*;
import org.gjt.sp.util.Log;

public abstract class TextRenderingManager
{
	static final String JAVA2D_RENDER_CLASS = "org.gjt.sp.jedit.textarea.TextRenderingManager2D";

	public static TextRenderingManager createTextRenderingManager()
	{
		if(java2d)
		{
			try
			{
				ClassLoader loader = TextRenderingManager.class
					.getClassLoader();

				Class clazz;
				if(loader == null)
					clazz = Class.forName(JAVA2D_RENDER_CLASS);
				else
					clazz = loader.loadClass(JAVA2D_RENDER_CLASS);

				return (TextRenderingManager)clazz.newInstance();
			}
			catch(Exception e)
			{
				throw new NoClassDefFoundError(JAVA2D_RENDER_CLASS);
			}
		}
		else
			return new AWTTextRenderingManager();
	}

	public void setupGraphics(Graphics g) {}

	public void configure(boolean antiAlias, boolean fracFontMetrics) {}

	public float drawChars(char[] text, int off, int len, Graphics g,
		float x, float y, TabExpander e, Color background)
	{
		// this probably should be moved elsewhere
		if(background != null)
		{
			float width = charsWidth(text,off,len,g.getFont(),x,e);

			FontMetrics fm = g.getFontMetrics();
			float height = fm.getHeight();
			float descent = fm.getDescent();
			float leading = fm.getLeading();

			g.setColor(background);
			g.setXORMode(background);
			g.fillRect((int)x,(int)(y - height + descent + leading),
				(int)width,(int)height);

			g.setPaintMode();
		}

		int flushLen = 0;
		int flushIndex = off;

		int end = off + len;

		for(int i = off; i < end; i++)
		{
			if(text[i] == '\t')
			{
				if(flushLen > 0)
				{
					x += _drawCharsAndGetWidth(text,flushIndex,
						flushLen,g,x,y);
					flushLen = 0;
				}

				flushIndex = i + 1;

				x = e.nextTabStop(x,i - off);
			}
			else
				flushLen++;
		}

		if(flushLen > 0)
			x += _drawCharsAndGetWidth(text,flushIndex,flushLen,g,x,y);

		return x;
	}

	public float charsWidth(char[] text, int off, int len, Font font, float x,
		TabExpander e)
	{
		float newX = x;

		int flushLen = 0;
		int flushIndex = off;

		int end = off + len;

		for(int i = off; i < end; i++)
		{
			if(text[i] == '\t')
			{
				if(flushLen > 0)
				{
					newX += _getWidth(text,flushIndex,flushLen,font);
					flushLen = 0;
				}

				flushIndex = i + 1;

				newX = e.nextTabStop(newX,i - off);
			}
			else
				flushLen++;
		}

		if(flushLen > 0)
			newX += _getWidth(text,flushIndex,flushLen,font);

		return newX - x;
	}

	public int xToOffset(char[] text, int off, int len, Font font, float x0,
		TabExpander e, boolean round, float[] widthArray)
	{
		int flushLen = 0;
		int flushIndex = off;

		int end = off + len;

		float width = widthArray[0];

		for(int i = off; i < end; i++)
		{
			if(text[i] == '\t')
			{
				if(flushLen > 0)
				{
					float newWidth = _getWidth(text,flushIndex,
						flushLen,font);
					if(x <= width + newWidth)
					{
						return _xToOffset(text,flushIndex,
							flushLen,font,x - width,
							round) - off;
					}
					else
						width += newWidth;

					flushLen = 0;
				}

				flushIndex = i + 1;

				float newWidth = e.nextTabStop(width,i - off) - width;
				if(x <= width + newWidth)
				{
					if(round || (x - width) < (width + newWidth - x))
						return i;
					else
						return i + 1;
				}
				else
					width += newWidth;
			}
			else
				flushLen++;
		}

		if(flushLen > 0)
		{
			float newWidth = _getWidth(text,flushIndex,flushLen,font);
			if(x <= width + newWidth)
			{
				return _xToOffset(text,flushIndex,flushLen,font,
					x - width,round) - off;
			}
			else
				width += newWidth;
		}

		widthArray[0] = width;
		return -1;
	}

	abstract float _drawChars(char[] text, int start, int len, Graphics g,
		float x, float y);

	abstract float _getWidth(char[] text, int start, int len, Font font);

	abstract int _offsetToX(char[] text, int start, int len, Font font, float x,
		boolean round);

	static boolean java2d;

	static
	{
		try
		{
			ClassLoader loader = TextRenderingManager.class.getClassLoader();

			if(loader == null)
				Class.forName("java.awt.Graphics2D");
			else
				loader.loadClass("java.awt.Graphics2D");

			Log.log(Log.DEBUG,TextRenderingManager.class,
				"Java2D detected; will use new text rendering code");
			java2d = true;
		}
		catch(ClassNotFoundException cnf)
		{
			Log.log(Log.DEBUG,TextRenderingManager.class,
				"Java2D not detected; will use old text rendering code");
			java2d = false;
		}
	}
}
