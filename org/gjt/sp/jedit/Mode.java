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
import org.gjt.sp.jedit.syntax.TokenMarker;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

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
			Log.log(Log.ERROR,this,"Invalid filename/firstline"
				+ " globs in mode " + name);
			Log.log(Log.ERROR,this,re);
		}

		initKeyBindings();
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
			Log.log(Log.ERROR,this,e);
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
		if(value == null)
			return null;

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

	/**
	 * Returns a string representation of this edit mode.
	 */
	public String toString()
	{
		return getClass().getName() + "[" + getName() + "]";
	}

	// package-private members

	// called by jEdit.reloadKeyBindings()
	void initKeyBindings()
	{
		// Bind indentCloseBrackets to indent-line
		String indentCloseBrackets = (String)getProperty("indentCloseBrackets");
		if(indentCloseBrackets != null)
		{
			EditAction action = jEdit.getAction("indent-lines");

			for(int i = 0; i < indentCloseBrackets.length(); i++)
			{
				jEdit.getInputHandler().addKeyBinding(
					indentCloseBrackets.substring(i,i+1),
					action);
			}
		}
	}

	// private members
	private String name;
	private RE firstlineRE;
	private RE filenameRE;
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.21  2000/03/20 06:06:36  sp
 * Mode internals cleaned up
 *
 * Revision 1.20  2000/01/29 08:18:08  sp
 * bug fixes, misc updates
 *
 * Revision 1.19  1999/12/19 08:12:34  sp
 * 2.3 started. Key binding changes  don't require restart, expand-abbrev renamed to complete-word, new splash screen
 *
 * Revision 1.18  1999/12/11 06:34:39  sp
 * Bug fixes
 *
 * Revision 1.17  1999/10/31 07:15:34  sp
 * New logging API, splash screen updates, bug fixes
 *
 * Revision 1.16  1999/10/30 02:44:18  sp
 * Miscallaneous stuffs
 *
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
 */
