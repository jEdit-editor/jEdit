/*
 * BufferHistory.java - Remembers caret positions 
 * Copyright (C) 2000 Slava Pestov
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
import java.util.Enumeration;
import java.util.Vector;
import org.gjt.sp.util.Log;

public class BufferHistory
{
	public static int getCaretPosition(String path)
	{
		Entry entry = getEntry(path);
		return (entry == null ? 0 : entry.caret);
	}

	public static void setCaretPosition(String path, int caret)
	{
		removeEntry(path);
		addEntry(new Entry(path,caret));
	}

	public static Vector getBufferHistory()
	{
		return history;
	}

	public static void load(File file)
	{
		try
		{
			max = Integer.parseInt(jEdit.getProperty("history"));
		}
		catch(NumberFormatException e)
		{
			max = 25;
		}

		try
		{
			BufferedReader in = new BufferedReader(
				new FileReader(file));

			String line;
			while((line = in.readLine()) != null)
			{
				int index = line.indexOf('\t');
				String path = line.substring(0,index);
				int caret = Integer.parseInt(line.substring(index+1));
				addEntry(new Entry(path,caret));
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

	public static class Entry
	{
		public String path;
		public int caret;

		public Entry(String path, int caret)
		{
			this.path = path;
			this.caret = caret;
		}
	}
}
