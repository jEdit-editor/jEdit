/*
 * SyntaxStyle.java - A simple text style class
 * Copyright (C) 1999, 2001 Slava Pestov
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

import java.lang.reflect.Method;
import java.awt.*;
import java.util.StringTokenizer;
import org.gjt.sp.util.Log;

/**
 * A simple text style class. It can specify the color, italic flag,
 * and bold flag of a run of text.
 * @author Slava Pestov
 * @version $Id$
 */
public class SyntaxStyle
{
	/**
	 * Creates a new SyntaxStyle.
	 * @param fgColor The text color
	 * @param bgColor The background color
	 * @param italic True if the text should be italics
	 * @param bold True if the text should be bold
	 */
	public SyntaxStyle(Color fgColor, Color bgColor, boolean italic, boolean bold)
	{
		this.fgColor = fgColor;
		this.bgColor = bgColor;
		this.italic = italic;
		this.bold = bold;
	}

	/**
	 * Returns the text color.
	 */
	public Color getForegroundColor()
	{
		return fgColor;
	}

	/**
	 * Returns the background color.
	 */
	public Color getBackgroundColor()
	{
		return bgColor;
	}

	/**
	 * Returns true if no font styles are enabled.
	 */
	public boolean isPlain()
	{
		return !(bold || italic);
	}

	/**
	 * Returns true if italics is enabled for this style.
	 */
	public boolean isItalic()
	{
		return italic;
	}

	/**
	 * Returns true if boldface is enabled for this style.
	 */
	public boolean isBold()
	{
		return bold;
	}

	/**
	 * Returns the specified font, but with the style's bold and
	 * italic flags applied.
	 */
	public Font getStyledFont(Font font)
	{
		if(font == null)
			throw new NullPointerException("font param must not"
				+ " be null");

		if(font == lastFont)
			return lastStyledFont;
		lastFont = font;

		int style = (bold ? Font.BOLD : 0)
			| (italic ? Font.ITALIC : 0);

		if(method == null)
		{
			lastStyledFont = new Font(font.getFamily(),style,
				font.getSize());
		}
		else
		{
			Object[] args = { new Integer(style) };
			try
			{
				lastStyledFont = (Font)method.invoke(font,args);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				return null;
			}
		}

		return lastStyledFont;
	}

	/**
	 * Returns a string representation of this object.
	 */
	public String toString()
	{
		return getClass().getName() + "[fgColor=" + fgColor +
			((bgColor == null) ? "" : ",bgColor=" + bgColor) +
			(italic ? ",italic" : "") +
			(bold ? ",bold" : "") + "]";
	}

	// private members
	private Color fgColor;
	private Color bgColor;
	private boolean italic;
	private boolean bold;
	private Font lastFont;
	private Font lastStyledFont;
	private static Method method;

	static
	{
		try
		{
			// try Java 1.2 code
			method = Font.class.getMethod("deriveFont",
				new Class[] { int.class });
			Log.log(Log.DEBUG,SyntaxStyle.class,"deriveFont() available");
		}
		catch(Exception e)
		{
			// use Java 1.1 code
		}
	}
}
