/*
 * Mode.java - jEdit editing mode
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

package org.gjt.sp.jedit;

import gnu.regexp.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import org.gjt.sp.jedit.syntax.TokenMarker;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.jedit.*;

/**
 * An edit mode defines specific settings for editing some type of file.
 * One instance of this class is created for each supported edit mode.
 * In most cases, instances of this class can be created directly, however
 * if the edit mode needs to define custom indentation behaviour,
 * subclassing is required.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class Mode
{
	/**
	 * Creates a new edit mode.
	 *
	 * @param name The name used in mode listings and to query mode
	 * properties
	 * @see #getProperty(String)
	 */
	public Mode(String name)
	{
		this.name = name;

		// Bind indentCloseBrackets to indent-line
		String indentCloseBrackets = (String)getProperty("indentCloseBrackets");
		if(indentCloseBrackets != null)
		{
			EditAction action = jEdit.getAction("indent-line");

			for(int i = 0; i < indentCloseBrackets.length(); i++)
			{
				jEdit.getInputHandler().addKeyBinding(
					indentCloseBrackets.substring(i,i+1),
					action);
			}
		}

		try
		{
			String filenameGlob = (String)getProperty("filenameGlob");
			if(filenameGlob != null)
			{
				filenameRE = new RE(MiscUtilities.globToRE(
					filenameGlob),RE.REG_ICASE);
			}

			String firstlineGlob = (String)getProperty("firstlineGlob");
			if(firstlineGlob != null)
			{
				firstlineRE = new RE(MiscUtilities.globToRE(
					firstlineGlob),RE.REG_ICASE);
			}
		}
		catch(REException re)
		{
			System.err.println("Invalid filename/firstline globs"
				+ " in mode " + name + ":");
			re.printStackTrace();
		}
	}

	/**
	 * Called when a buffer enters this mode.
	 * @param buffer The buffer that entered this mode
	 */
	public void enter(Buffer buffer) {}

	/**
	 * Called when a view enters this mode.
	 * @param view The view that entered this mode
	 */
	public void enterView(View view) {}

	/**
	 * Called when a buffer leaves this mode.
	 * @param buffer The buffer that left this mode
	 */
	public void leave(Buffer buffer) {}
	
	/**
	 * Called when a view leaves this mode.
	 * @param view The view that left this mode
	 */
	public void leaveView(View view) {}

	/**
	 * If auto indent is enabled, this method is called when the `Tab'
	 * or `Enter' key is pressed to perform mode-specific indentation
	 * and return true, or return false if a normal tab is to be inserted.
	 * @param buffer The buffer where the tab key was pressed
	 * @param view The view where the tab key was pressed
	 * @param line The line number to indent
	 * @param force If true, the line will be indented even if it already
	 * has the right amount of indent
	 * @return true if the tab key event should be swallowed (ignored)
	 * false if a real tab should be inserted
	 */
	public boolean indentLine(Buffer buffer, View view,
		int lineIndex, boolean force)
	{
		// Use JEditTextArea's line access methods
		JEditTextArea textArea = view.getTextArea();

		// Get properties
		String openBrackets = (String)getProperty("indentOpenBrackets");
		String closeBrackets = (String)getProperty("indentCloseBrackets");
		if(openBrackets == null)
			openBrackets = "";
		if(closeBrackets == null)
			closeBrackets = "";
		int tabSize = buffer.getTabSize();
		boolean noTabs = "yes".equals(buffer.getProperty("noTabs"));

		if(lineIndex == 0)
			return false;

		// Get line text
		String line = textArea.getLineText(lineIndex);
		int start = textArea.getLineStartOffset(lineIndex);
		String prevLine = null;
		for(int i = lineIndex - 1; i >= 0; i--)
		{
			if(textArea.getLineLength(i) != 0)
			{
				prevLine = textArea.getLineText(i);
				break;
			}
		}

		if(prevLine == null)
			return false;

		/*
		 * On the previous line,
		 * if(bob) { --> +1
		 * if(bob) { } --> 0
		 * } else if(bob) { --> +1
		 */
		boolean prevLineStart = true; // False after initial indent
		int prevLineIndent = 0; // Indent width (tab expanded)
		int prevLineBrackets = 0; // Additional bracket indent
		for(int i = 0; i < prevLine.length(); i++)
		{
			char c = prevLine.charAt(i);
			switch(c)
			{
			case ' ':
				if(prevLineStart)
					prevLineIndent++;
				break;
			case '\t':
				if(prevLineStart)
				{
					prevLineIndent += (tabSize
						- (prevLineIndent
						% tabSize));
				}
				break;
			default:
				prevLineStart = false;
				if(closeBrackets.indexOf(c) != -1)
					prevLineBrackets = Math.max(
						prevLineBrackets-1,0);
				else if(openBrackets.indexOf(c) != -1)
					prevLineBrackets++;
				break;
			}
		}

		/*
		 * On the current line,
		 * } --> -1
		 * } else if(bob) { --> -1
		 * if(bob) { } --> 0
		 */
		boolean lineStart = true; // False after initial indent
		int lineIndent = 0; // Indent width (tab expanded)
		int lineWidth = 0; // White space count
		int lineBrackets = 0; // Additional bracket indent
		int closeBracketIndex = -1; // For lining up closing
			// and opening brackets
		for(int i = 0; i < line.length(); i++)
		{
			char c = line.charAt(i);
			switch(c)
			{
			case ' ':
				if(lineStart)
				{
					lineIndent++;
					lineWidth++;
				}
				break;
			case '\t':
				if(lineStart)
				{
					lineIndent += (tabSize
						- (lineIndent
						% tabSize));
					lineWidth++;
				}
				break;
			default:
				lineStart = false;
				if(closeBrackets.indexOf(c) != -1)
				{
					if(lineBrackets == 0)
						closeBracketIndex = i;
					else
						lineBrackets--;
				}
				else if(openBrackets.indexOf(c) != -1)
					lineBrackets++;
				break;
			}
		}

		try
		{
			if(closeBracketIndex != -1)
			{
				int offset = TextUtilities.findMatchingBracket(
					buffer,start + closeBracketIndex);
				if(offset != -1)
				{
					String closeLine = textArea.getLineText(
						textArea.getLineOfOffset(offset));
					prevLineIndent = MiscUtilities
						.getLeadingWhiteSpaceWidth(
						closeLine,tabSize);
				}
				else
					return false;
			}
			else
			{
				prevLineIndent += (prevLineBrackets * tabSize);
			}

			// Insert a tab if line already has correct indent
			// and force is not set
			if(!force && lineIndent >= prevLineIndent)
				return false;

			// Do it
			buffer.remove(start,lineWidth);
			buffer.insertString(start,MiscUtilities.createWhiteSpace(
				prevLineIndent,(noTabs ? 0 : tabSize)),null);
			return true;
		}
		catch(BadLocationException bl)
		{
			bl.printStackTrace();
		}

		return false;
	}

	/**
	 * Returns a <code>TokenMarker</code> for this mode. Can return null
	 * if this mode doesn's support syntax highlighting. The default
	 * implementation creates a new instance of the class specified
	 * by the "tokenMarker" mode property.
	 *
	 * @see #getProperty(String)
	 */
	public TokenMarker createTokenMarker()
	{
		String clazz = (String)getProperty("tokenMarker");
		if(clazz == null)
			return null;

		try
		{
			Class cls;
			ClassLoader loader = getClass().getClassLoader();
			if(loader == null)
				cls = Class.forName(clazz);
			else
				cls = loader.loadClass(clazz);

			return (TokenMarker)cls.newInstance();
		}
		catch(Exception e)
		{
			System.err.println("Cannot create token marker " + clazz + ":");
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Returns a mode property. The default implementation obtains the
	 * property from the jEdit property named
	 * "mode.<i>mode name</i>.<i>key</i>".
	 * @param key The property name
	 *
	 * @since jEdit 2.2pre1
	 */
	public Object getProperty(String key)
	{
		String value = jEdit.getProperty("mode." + name + "." + key);
		try
		{
			return new Integer(value);
		}
		catch(NumberFormatException nf)
		{
			return value;
		}
	}

	/**
	 * Returns if the edit mode is suitable for editing the specified
	 * file. The buffer name and first line is checked against the
	 * file name and first line globs, respectively.
	 * @param buffer The buffer
	 * @param fileName The buffer's name
	 * @param firstLine The first line of the buffer
	 *
	 * @since jEdit 2.2pre1
	 */
	public boolean accept(Buffer buffer, String fileName, String firstLine)
	{
		if(filenameRE != null && filenameRE.isMatch(fileName))
			return true;

		if(firstlineRE != null && firstlineRE.isMatch(firstLine))
			return true;

		return false;
	}

	/**
	 * Returns the internal name of this edit mode.
	 */
	public String getName()
	{
		return name;
	}

	// protected members

	/**
	 * @since jEdit 2.2pre1
	 */
	protected RE filenameRE;

	/**
	 * @since jEdit 2.2pre1
	 */
	protected RE firstlineRE;

	// private members
	private String name;
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.15  1999/10/24 06:04:00  sp
 * QuickSearch in tool bar, auto indent updates, macro recorder updates
 *
 * Revision 1.14  1999/10/24 02:06:41  sp
 * Miscallaneous pre1 stuff
 *
 * Revision 1.13  1999/10/23 03:48:22  sp
 * Mode system overhaul, close all dialog box, misc other stuff
 *
 * Revision 1.12  1999/09/30 12:21:04  sp
 * No net access for a month... so here's one big jEdit 2.1pre1
 *
 * Revision 1.11  1999/05/26 04:46:03  sp
 * Minor API change, soft tabs fixed ,1.7pre1
 *
 * Revision 1.10  1999/05/22 08:33:53  sp
 * FAQ updates, mode selection tweak, patch mode update, javadoc updates, JDK 1.1.8 fix
 *
 * Revision 1.9  1999/04/19 05:47:35  sp
 * ladies and gentlemen, 1.6pre1
 *
 * Revision 1.8  1999/03/21 07:53:14  sp
 * Plugin doc updates, action API change, new method in MiscUtilities, new class
 * loader, new plugin interface
 *
 * Revision 1.7  1999/03/12 07:23:19  sp
 * Fixed serious view bug, Javadoc updates
 *
 */
