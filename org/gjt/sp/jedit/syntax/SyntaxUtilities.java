/*
 * SyntaxUtilities.java - Utility functions used by syntax highlighting
 * Copyright (C) 1999 Slava Pestov
 * Portions copyright (C) 1999 Andre Kaplan
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

import javax.swing.text.*;
import java.awt.*;

import org.gjt.sp.jedit.*;

/**
 * Class with several utility functions used by jEdit's syntax highlighting
 * subsystem.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class SyntaxUtilities
{
	/**
	 * Checks if a subregion of a <code>Segment</code> is equal to a
	 * string.
	 * @param ignoreCase True if case should be ignored, false otherwise
	 * @param text The segment
	 * @param offset The offset into the segment
	 * @param match The string to match
	 */
	public static boolean regionMatches(boolean ignoreCase, Segment text,
					    int offset, String match)
	{
		int length = offset + match.length();
		char[] textArray = text.array;
		if(length > text.offset + text.count)
			return false;
		for(int i = offset, j = 0; i < length; i++, j++)
		{
			char c1 = textArray[i];
			char c2 = match.charAt(j);
			if(ignoreCase)
			{
				c1 = Character.toUpperCase(c1);
				c2 = Character.toUpperCase(c2);
			}
			if(c1 != c2)
				return false;
		}
		return true;
	}
	
	/**
	 * Checks if a subregion of a <code>Segment</code> is equal to a
	 * character array.
	 * @param ignoreCase True if case should be ignored, false otherwise
	 * @param text The segment
	 * @param offset The offset into the segment
	 * @param match The character array to match
	 */
	public static boolean regionMatches(boolean ignoreCase, Segment text,
					    int offset, char[] match)
	{
		int length = offset + match.length;
		char[] textArray = text.array;
		if(length > text.offset + text.count)
			return false;
		for(int i = offset, j = 0; i < length; i++, j++)
		{
			char c1 = textArray[i];
			char c2 = match[j];
			if(ignoreCase)
			{
				c1 = Character.toUpperCase(c1);
				c2 = Character.toUpperCase(c2);
			}
			if(c1 != c2)
				return false;
		}
		return true;
	}

	/**
	 * Paints the specified line onto the graphics context. Note that this
	 * method munges the offset and count values of the segment.
	 * @param line The line segment
	 * @param tokens The token list for the line
	 * @param styles The syntax style list
	 * @param expander The tab expander used to determine tab stops. May
	 * be null
	 * @param gfx The graphics context
	 * @param x The x co-ordinate
	 * @param y The y co-ordinate
	 * @return The x co-ordinate, plus the width of the painted string
	 */
	public static int paintSyntaxLine(Segment line, Token tokens,
		SyntaxStyle[] styles, TabExpander expander, Graphics gfx,
		Color background, int x, int y)
	{
		Font defaultFont = gfx.getFont();
		Color defaultColor = gfx.getColor();

		int offset = 0;
		for(;;)
		{
			byte id = tokens.id;
			if(id == Token.END)
				break;

			int length = tokens.length;
			line.count = length;
			if(id == Token.NULL)
			{
				if(!defaultColor.equals(gfx.getColor()))
					gfx.setColor(defaultColor);
				if(!defaultFont.equals(gfx.getFont()))
					gfx.setFont(defaultFont);
			}
			else
			{
				Color bg = styles[id].getBackgroundColor();
				if (bg != null)
				{
					FontMetrics fm = styles[id].getFontMetrics(defaultFont);
					int width   = Utilities.getTabbedTextWidth(line, fm, x, expander, 0); 
					int height  = fm.getHeight();
					int descent = fm.getDescent();
					int leading = fm.getLeading();
					gfx.setColor(background);
					gfx.setXORMode(bg);
					gfx.fillRect(x, y - height + descent + leading, width, height);
					gfx.setPaintMode();
				}

				styles[id].setGraphicsFlags(gfx,defaultFont);
			}

			x = Utilities.drawTabbedText(line,x,y,gfx,expander,0);
			line.offset += length;
			offset += length;

			tokens = tokens.next;
		}

		return x;
	}

	// private members
	private SyntaxUtilities() {}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.13  2000/04/09 04:40:00  sp
 * Documentation updates
 *
 * Revision 1.12  2000/04/09 03:14:14  sp
 * Syntax token backgrounds can now be specified
 *
 * Revision 1.11  2000/04/07 06:57:26  sp
 * Buffer options dialog box updates, API docs updated a bit in syntax package
 *
 * Revision 1.10  2000/04/06 13:09:46  sp
 * More token types added
 *
 * Revision 1.9  1999/12/13 03:40:30  sp
 * Bug fixes, syntax is now mostly GPL'd
 *
 * Revision 1.8  1999/07/08 06:06:04  sp
 * Bug fixes and miscallaneous updates
 *
 * Revision 1.7  1999/07/05 04:38:39  sp
 * Massive batch of changes... bug fixes, also new text component is in place.
 * Have fun
 *
 * Revision 1.6  1999/06/07 06:36:32  sp
 * Syntax `styling' (bold/italic tokens) added,
 * plugin options dialog for plugin option panes
 *
 * Revision 1.5  1999/06/05 00:22:58  sp
 * LGPL'd syntax package
 *
 * Revision 1.4  1999/04/19 05:38:20  sp
 * Syntax API changes
 *
 * Revision 1.3  1999/04/02 02:39:46  sp
 * Updated docs, console fix, getDefaultSyntaxColors() method, hypersearch update
 *
 * Revision 1.2  1999/03/27 02:46:17  sp
 * SyntaxTextArea is now modular
 *
 */
