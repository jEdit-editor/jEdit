/*
 * KeywordMap.java - Fast keyword->id map
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
package org.gjt.sp.jedit.syntax;

import javax.swing.text.Segment;
import org.gjt.sp.jedit.jEdit;

/**
 * A <code>KeywordMap</code> is similar to a hashtable in that it maps keys
 * to values. However, the `keys' are Swing segments. This allows lookups of
 * text substrings without the overhead of creating a new string object.
 * <p>
 * This class is used by <code>CTokenMarker</code> to map keywords to ids.
 * @see org.gjt.sp.jedit.syntax.CTokenMarker
 */
public class KeywordMap
{
	// public members

	/**
	 * Creates a new <code>KeywordMap</code>.
	 * @param ignoreCase True if keys are case insensitive
	 */
	public KeywordMap(boolean ignoreCase)
	{
		map = new Keyword[26];
		this.ignoreCase = ignoreCase;
	}

	/**
	 * Looks up a key.
	 * @param text The text segment
	 * @param offset The offset of the substring within the text segment
	 * @param length The length of the substring
	 */
	public String lookup(Segment text, int offset, int length)
	{
		if(length == 0)
			return null;
		char key = text.array[offset];
		Keyword k = map[Character.toUpperCase(key) % 26];
		while(k != null)
		{
			String keyword = k.keyword;
			if(length != keyword.length())
			{
				k = k.next;
				continue;
			}
			if(jEdit.regionMatches(ignoreCase,text,offset,keyword))
				return k.id;
			k = k.next;
		}
		return null;
	}

	/**
	 * Adds a key-value mapping.
	 * @param keyword The key
	 * @Param id The value
	 */
	public void add(String keyword, String id)
	{
		int key = Character.toUpperCase(keyword.charAt(0)) % 26;
		map[key] = new Keyword(keyword,id,map[key]);
	}

	// private members
	private class Keyword
	{
		public Keyword(String keyword, String id, Keyword next)
		{
			this.keyword = keyword;
			this.id = id;
			this.next = next;
		}

		public String keyword;
		public String id;
		public Keyword next;
	}

	private Keyword[] map;
	private boolean ignoreCase;
}
