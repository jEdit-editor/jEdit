/*
 * BufferHistory.java - Remembers caret positions 
 * Copyright (C) 2000, 2001 Slava Pestov
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
import java.util.*;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.util.Log;

public class BufferHistory
{
	public static int getCaretPosition(String path)
	{
		Entry entry = getEntry(path);
		return (entry == null ? 0 : entry.caret);
	}

	public static Selection[] getSelection(String path)
	{
		Entry entry = getEntry(path);
		return (entry == null ? null : stringToSelection(entry.selection));
	}

	public static void setEntry(String path, int caret, Selection[] selection)
	{
		removeEntry(path);
		addEntry(new Entry(path,caret,selectionToString(selection)));
	}

	public static Vector getBufferHistory()
	{
		return history;
	}

	public static void load(File file)
	{
		try
		{
			max = Integer.parseInt(jEdit.getProperty("recentFiles"));
		}
		catch(NumberFormatException e)
		{
			max = 50;
		}

		try
		{
			BufferedReader in = new BufferedReader(
				new FileReader(file));

			String line;
			while((line = in.readLine()) != null)
			{
				int index1 = line.indexOf('\t');

				String path = line.substring(0,index1);

				int index2 = line.indexOf('\t',index1 + 1);
				int caret;
				String selection;
				if(index2 == -1)
				{
					caret = Integer.parseInt(line.substring(
							index1 + 1));
					selection = null;
				}
				else
				{
					caret = Integer.parseInt(line.substring(
							index1 + 1,index2));
					selection = line.substring(index2);
				}

				addEntry(new Entry(path,caret,selection));
			}

			in.close();
		}
		catch(FileNotFoundException e)
		{
			Log.log(Log.NOTICE,BufferHistory.class,e);
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,BufferHistory.class,e);
		}
	}

	public static void save(File file)
	{
		String lineSep = System.getProperty("line.separator");

		try
		{
			BufferedWriter out = new BufferedWriter(
				new FileWriter(file));

			Enumeration enum = history.elements();
			while(enum.hasMoreElements())
			{
				Entry entry = (Entry)enum.nextElement();
				out.write(entry.path);
				out.write('\t');
				out.write(String.valueOf(entry.caret));
				out.write('\t');
				if(entry.selection != null)
					out.write(String.valueOf(entry.selection));
				out.write(lineSep);
			}

			out.close();
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,BufferHistory.class,e);
		}
	}

	// private members
	private static Vector history;
	private static boolean pathsCaseInsensitive;
	private static int max;

	static
	{
		history = new Vector();
		pathsCaseInsensitive = (File.separatorChar == '\\'
			|| File.separatorChar == ':');
	}

	private static Entry getEntry(String path)
	{
		Enumeration enum = history.elements();
		while(enum.hasMoreElements())
		{
			Entry entry = (Entry)enum.nextElement();
			if(pathsCaseInsensitive)
			{
				if(entry.path.equalsIgnoreCase(path))
					return entry;
			}
			else
			{
				if(entry.path.equals(path))
					return entry;
			}
		}

		return null;
	}

	private static void addEntry(Entry entry)
	{
		history.insertElementAt(entry,0);
		while(history.size() > max)
			history.removeElementAt(history.size() - 1);
	}

	private static void removeEntry(String path)
	{
		Enumeration enum = history.elements();
		for(int i = 0; i < history.size(); i++)
		{
			Entry entry = (Entry)history.elementAt(i);
			if(entry.path.equals(path))
			{
				history.removeElementAt(i);
				return;
			}
		}
	}

	private static String selectionToString(Selection[] s)
	{
		if(s == null)
			return null;

		StringBuffer buf = new StringBuffer();

		for(int i = 0; i < s.length; i++)
		{
			if(i != 0)
				buf.append(' ');

			Selection sel = s[i];
			if(sel instanceof Selection.Range)
				buf.append("range ");
			else //if(sel instanceof Selection.Rect)
				buf.append("rect ");
			buf.append(sel.getStart());
			buf.append(' ');
			buf.append(sel.getEnd());
		}

		return buf.toString();
	}

	private static Selection[] stringToSelection(String s)
	{
		if(s == null)
			return null;

		Vector selection = new Vector();
		StringTokenizer st = new StringTokenizer(s);

		while(st.hasMoreTokens())
		{
			String type = st.nextToken();
			int start = Integer.parseInt(st.nextToken());
			int end = Integer.parseInt(st.nextToken());
			Selection sel;
			if(type.equals("range"))
				sel = new Selection.Range(start,end);
			else //if(type.equals("rect"))
				sel = new Selection.Rect(start,end);

			selection.addElement(sel);
		}

		Selection[] returnValue = new Selection[selection.size()];
		selection.copyInto(returnValue);
		return returnValue;
	}

	public static class Entry
	{
		public String path;
		public int caret;
		public String selection;

		public Entry(String path, int caret, String selection)
		{
			this.path = path;
			this.caret = caret;
			this.selection = selection;
		}
	}
}
