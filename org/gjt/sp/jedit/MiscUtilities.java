/*
 * MiscUtilities.java - Various miscallaneous utility functions
 * Copyright (C) 1999 Slava Pestov
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

import java.io.*;
import java.util.Vector;

/**
 * Class with several useful miscellaneous functions.<p>
 *
 * It provides methods for converting file names to class names, for
 * constructing path names, and for various indentation calculations.
 * A quicksort implementation is also available.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class MiscUtilities
{
	/**
	 * Converts a file name to a class name. All slash characters are
	 * replaced with periods and the trailing '.class' is removed.
	 * @param name The file name
	 */
	public static String fileToClass(String name)
	{
		char[] clsName = name.toCharArray();
		for(int i = clsName.length - 6; i >= 0; i--)
			if(clsName[i] == '/')
				clsName[i] = '.';
		return new String(clsName,0,clsName.length - 6);
	}

	/**
	 * Converts a class name to a file name. All periods are replaced
	 * with slashes and the '.class' extension is added.
	 * @param name The class name
	 */
	public static String classToFile(String name)
	{
		return name.replace('.','/').concat(".class");
	}

	/**
	 * Constructs an absolute path name from a directory and another
	 * path name.
	 * @param parent The directory
	 * @param path The path name
	 */
	public static String constructPath(String parent, String path)
	{
		// absolute pathnames
		if(path.startsWith(File.separator))
			return canonPath(path);
		// windows pathnames, eg C:\document
		else if(path.length() >= 3 && path.charAt(1) == ':')
			return canonPath(path);
		// relative pathnames
		else if(parent == null)
			parent = System.getProperty("user.dir");
		// do it!
		if(parent.endsWith(File.separator))
			return canonPath(parent + path);
		else
			return canonPath(parent + File.separator + path);
	}

	/**
	 * Constructs an absolute path name from three path components.
	 * @param parent The parent directory
	 * @param path1 The first path
	 * @param path2 The second path
	 */
	public static String constructPath(String parent,
		String path1, String path2)
	{
		return constructPath(constructPath(parent,path1),path2);
	}

	/**
	 * Returns the number of leading white space characters in the
	 * specified string.
	 * @param str The string
	 */
	public static int getLeadingWhiteSpace(String str)
	{
		int whitespace = 0;
loop:		for(;whitespace < str.length();)
		{
			switch(str.charAt(whitespace))
			{
			case ' ': case '\t':
				whitespace++;
				break;
			default:
				break loop;
			}
		}
		return whitespace;
	}

	/**
	 * Returns the width of the leading white space in the specified
	 * string.
	 * @param str The string
	 * @param tabSize The tab size
	 */
	public static int getLeadingWhiteSpaceWidth(String str, int tabSize)
	{
		int whitespace = 0;
loop:		for(int i = 0; i < str.length(); i++)
		{
			switch(str.charAt(i))
			{
			case ' ':
				whitespace++;
				break;
			case '\t':
				whitespace += (tabSize - whitespace % tabSize);
				break;
			default:
				break loop;
			}
		}
		return whitespace;
	}

	/**
	 * Creates a string of white space with the specified length.
	 * @param len The length
	 * @param tabSize The tab size, or 0 if tabs are not to be used
	 */
	public static String createWhiteSpace(int len, int tabSize)
	{
		StringBuffer buf = new StringBuffer();
		if(tabSize == 0)
		{
			while(len-- > 0)
				buf.append(' ');
		}
		else		
		{
			int count = len / tabSize;
			while(count-- > 0)
				buf.append('\t');
			count = len % tabSize;
			while(count-- > 0)
				buf.append(' ');
		}
		return buf.toString();
	}

	/**
	 * Checks if the specified string is a URL.
	 * @param str The string to check
	 * @return True if the string is a URL, false otherwise
	 */
	public static boolean isURL(String str)
	{
		int colonIndex = str.indexOf(':');
		int spaceIndex = str.indexOf(' ');
		if(spaceIndex == -1)
			spaceIndex = str.length();

		if(colonIndex > 1 /* fails for C:\... */
			&& colonIndex < spaceIndex)
			return true;
		else
			return false;
	}

	/**
	 * Converts a Unix-style glob to a regular expression.
	 * ? becomes ., * becomes .*, {aa,bb} becomes (aa|bb).
	 * @param glob The glob pattern
	 */
	public static String globToRE(String glob)
	{
		StringBuffer buf = new StringBuffer();
		boolean backslash = false;
		boolean insideGroup = false;

		for(int i = 0; i < glob.length(); i++)
		{
			char c = glob.charAt(i);
			if(backslash)
			{
				buf.append('\\');
				buf.append(c);
				backslash = false;
				continue;
			}

			switch(c)
			{
			case '\\':
				backslash = true;
				break;
			case '?':
				buf.append('.');
				break;
			case '.':
				buf.append("\\.");
				break;
			case '*':
				buf.append(".*");
				break;
			case '{':
				buf.append('(');
				insideGroup = true;
				break;
			case ',':
				if(insideGroup)
					buf.append('|');
				else
					buf.append(',');
				break;
			case '}':
				buf.append(')');
				insideGroup = false;
				break;
			default:
				buf.append(c);
			}
		}

		return buf.toString();
	}

	/**
	 * Sorts the specified array.
	 * @param obj The array
	 * @param compare Compares the objects
	 */
	public static void quicksort(Object[] obj, Compare compare)
	{
		if(obj.length == 0)
			return;

		quicksort(obj,0,obj.length - 1,compare);
	}

	/**
	 * Sorts the specified vector.
	 * @param vector The vector
	 * @param compare Compares the objects
	 */
	public static void quicksort(Vector vector, Compare compare)
	{
		if(vector.size() == 0)
			return;

		quicksort(vector,0,vector.size() - 1,compare);
	}

	/**
	 * An interface for comparing objects.
	 */
	public interface Compare
	{
		int compare(Object obj1, Object obj2);
	}

	/**
	 * Compares strings.
	 */
	public static class StringCompare implements Compare
	{
		public int compare(Object obj1, Object obj2)
		{
			return obj1.toString().compareTo(obj2.toString());
		}
	}

	/**
	 * Compares strings ignoring case.
	 */
	public static class StringICaseCompare implements Compare
	{
		public int compare(Object obj1, Object obj2)
		{
			return obj1.toString().toLowerCase()
				.compareTo(obj2.toString()
				.toLowerCase());
		}
	}

	/**
	 * Converts an internal version number (build) into a
	 * `human-readable' form.
	 * @param build The build
	 */
	public static String buildToVersion(String build)
	{
		if(build.length() != 11)
			return "<unknown version: " + build + ">";
		// First 2 chars are the major version number
		int major = Integer.parseInt(build.substring(0,2));
		// Second 2 are the minor number
		int minor = Integer.parseInt(build.substring(3,5));
		// Then the pre-release status
		int beta = Integer.parseInt(build.substring(6,8));
		// Finally the bug fix release
		int bugfix = Integer.parseInt(build.substring(9,11));

		return "" + major + "." + minor +
			(beta == 99 && bugfix == 0 ? "final" :
			(beta == 99 ? "." + bugfix : "pre" + beta));
	}

	// private members
	private MiscUtilities() {}

	private static String canonPath(String path)
	{
		try
		{
			return new File(path).getCanonicalPath();
		}
		catch(IOException io)
		{
			return path;
		}
	}

	private static void quicksort(Object[] obj, int _start, int _end,
		Compare compare)
	{
		int start = _start;
		int end = _end;

		Object mid = obj[(_start + _end) / 2];

		if(_start > _end)
			return;

		while(start <= end)
		{
			while((start < _end) && (compare.compare(obj[start],mid) < 0))
				start++;

			while((end > _start) && (compare.compare(obj[end],mid) > 0))
				end--;

			if(start <= end)
			{
				Object o = obj[start];
				obj[start] = obj[end];
				obj[end] = o;

				start++;
				end--;
			}
		}

		if(_start < end)
			quicksort(obj,_start,end,compare);

		if(start < _end)
			quicksort(obj,start,_end,compare);
	}

	private static void quicksort(Vector obj, int _start, int _end,
		Compare compare)
	{
		int start = _start;
		int end = _end;

		Object mid = obj.elementAt((_start + _end) / 2);

		if(_start > _end)
			return;

		while(start <= end)
		{
			while((start < _end) && (compare.compare(obj.elementAt(start),mid) < 0))
				start++;

			while((end > _start) && (compare.compare(obj.elementAt(end),mid) > 0))
				end--;

			if(start <= end)
			{
				Object o = obj.elementAt(start);
				obj.setElementAt(obj.elementAt(end),start);
				obj.setElementAt(o,end);

				start++;
				end--;
			}
		}

		if(_start < end)
			quicksort(obj,_start,end,compare);

		if(start < _end)
			quicksort(obj,start,_end,compare);
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.24  1999/11/28 00:33:06  sp
 * Faster directory search, actions slimmed down, faster exit/close-all
 *
 * Revision 1.23  1999/11/21 01:20:30  sp
 * Bug fixes, EditBus updates, fixed some warnings generated by jikes +P
 *
 * Revision 1.22  1999/10/31 07:15:34  sp
 * New logging API, splash screen updates, bug fixes
 *
 * Revision 1.21  1999/10/30 02:44:18  sp
 * Miscallaneous stuffs
 *
 * Revision 1.20  1999/10/28 09:07:21  sp
 * Directory list search
 *
 * Revision 1.19  1999/10/19 09:10:13  sp
 * pre5 bug fixing
 *
 * Revision 1.18  1999/10/10 06:38:45  sp
 * Bug fixes and quicksort routine
 *
 * Revision 1.17  1999/10/07 04:57:13  sp
 * Images updates, globs implemented, file filter bug fix, close all command
 *
 * Revision 1.16  1999/09/30 12:21:04  sp
 * No net access for a month... so here's one big jEdit 2.1pre1
 *
 * Revision 1.14  1999/07/16 23:45:49  sp
 * 1.7pre6 BugFree version
 *
 * Revision 1.13  1999/07/08 06:06:04  sp
 * Bug fixes and miscallaneous updates
 *
 * Revision 1.12  1999/05/29 08:06:56  sp
 * Search and replace overhaul
 *
 * Revision 1.11  1999/05/28 02:00:25  sp
 * SyntaxView bug fix, faq update, MiscUtilities.isURL() method added
 *
 * Revision 1.10  1999/05/26 04:46:03  sp
 * Minor API change, soft tabs fixed ,1.7pre1
 *
 * Revision 1.9  1999/04/24 01:55:28  sp
 * MiscUtilities.constructPath() bug fixed, event system bug(s) fix
 *
 * Revision 1.8  1999/04/23 07:35:10  sp
 * History engine reworking (shared history models, history saved to
 * .jedit-history)
 *
 */
