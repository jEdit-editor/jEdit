/*
 * BoyerMooreSearchMatcher.java - Literal pattern String matcher utilizing the
 *         Boyer-Moore algorithm
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

package org.gjt.sp.jedit.search;

public class BoyerMooreSearchMatcher implements SearchMatcher
{
	/**
	 * Creates a new string literal matcher.
	 * @param pattern The search string
	 * @param replace The replacement string
	 * @param ignoreCase True if the matcher should be case insensitive,
	 * false otherwise
	 */
	public BoyerMooreSearchMatcher(String pattern, String replace,
		boolean ignoreCase)
	{
		this.pattern = ((ignoreCase) ? pattern.toUpperCase() : pattern)
			.toCharArray();
		this.replace = replace;
		this.ignoreCase = ignoreCase;

		generateSkipArray();
		generateSuffixArray();
	}

	/**
	 * Returns the offset of the first match of the specified text
	 * within this matcher.
	 * @param text The text to search in
	 * @return an array where the first element is the start offset
	 * of the match, and the second element is the end offset of
	 * the match
	 */
	public int[] nextMatch(String text)
	{
		int pos = match(text.toCharArray(), 0);

		if (pos == -1)
		{
			return null;
		}
		else
		{
			int[] match = { pos, pos + pattern.length };
			return match;
		}
	}

	/**
	 * Returns the specified text, with any substitution specified
	 * within this matcher performed.
	 * @param text The text
	 */
	public String substitute(String text)
	{
		StringBuffer buf = null;

		char[] chars = text.toCharArray();

		int pos, lastMatch = 0;

		while ((pos = match(chars, lastMatch)) != -1)
		{
			if (buf == null)
				buf = new StringBuffer(chars.length);

			if (pos != lastMatch)
			{
				buf.append(chars, lastMatch, pos - lastMatch);
			}
			buf.append(replace);

			lastMatch = pos + pattern.length;
		}

		if(buf == null)
		{
			return null;
		}
		else
		{
			if(lastMatch < chars.length)
			{
				buf.append(chars, lastMatch, chars.length -
					lastMatch);
			}
			return buf.toString();
		}
	}

	public int match(char[] text, int offset)
	{
		int i = (offset >= 0) ? offset : 0;
		int last_i = text.length - pattern.length;
		int first_j = pattern.length - 1;
		int j;

		char ch = 0;

		while (i <= last_i)
		{
			for (j = first_j; j >= 0; --j)
			{
				ch = text[i + j];
				if (ignoreCase) ch = Character.toUpperCase(ch);
				if (ch != pattern[j]) break;
			}

			if (j >= 0)
			{
				int bc = j + 1 - skip[getSkipIndex(ch)];
				int gs = suffix[j];
				i += (bc > gs) ? bc : gs;
			}
			else
			{
				return i;
			}
		}

		return -1;
	}

	// private members
	private char[] pattern;
	private String replace;
	private boolean ignoreCase;

	// Boyer-Moore member fields
	private int[] skip;
	private int[] suffix;

	// Boyer-Moore helper methods
	private void generateSkipArray()
	{
		skip = new int[256];

		char ch;
		int pos = 0;

		while (pos < pattern.length)
		{
			ch = pattern[pos];
			skip[getSkipIndex(ch)] = ++pos;
		}
	}

	private static final int getSkipIndex(char ch)
	{
		return ((int) ch) & 0x000000FF;
	}

	private void generateSuffixArray()
	{
		int m = pattern.length;

		int j = m + 1;

		suffix = new int[j];
		int[] tmp = new int[j];
		tmp[m] = j;

		for (int i = m; i > 0; i--)
		{
			while (j <= m && pattern[i - 1] != pattern[j - 1])
			{
				if (suffix[j] == 0)
				{
					suffix[j] = j - i;
				}

				j = tmp[j];
			}

			tmp[i - 1] = --j;
		}

		int k = tmp[0];

		for (j = 0; j <= m; j++)
		{
			if (suffix[j] == 0)
				suffix[j] = k;
			if (j == k)
				k = tmp[k];
		}

		System.arraycopy(suffix, 1, suffix, 0, m);
	}
}
