/*
 * HistoryModel.java - History list model
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

package org.gjt.sp.jedit.gui;

import javax.swing.*;
import java.io.*;
import java.util.*;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;

/**
 * A history list. One history list can be used by several history text
 * fields.
 * @author Slava Pestov
 * @version $Id$
 */
public class HistoryModel
{
	/**
	 * Creates a new history list. Calling this is normally not
	 * necessary.
	 */
	public HistoryModel(String name)
	{
		this.name = name;

		try
		{
			max = Integer.parseInt(jEdit.getProperty("history"));
		}
		catch(NumberFormatException nf)
		{
			max = 25;
		}

		data = new Vector(max);
	}

	/**
	 * Adds an item to the end of this history list, trimming the list
	 * to the maximum number of items if necessary.
	 * @param text The item
	 */
	public void addItem(String text)
	{
		if(text == null || text.length() == 0)
			return;

		int index = data.indexOf(text);
		if(index != -1)
			data.removeElementAt(index);

		data.insertElementAt(text,0);

		if(getSize() > max)
			data.removeElementAt(getSize() - 1);
	}

	/**
	 * Returns an item from the history list.
	 * @param index The index
	 */
	public String getItem(int index)
	{
		return (String)data.elementAt(index);
	}

	/**
	 * Returns the number of elements in this history list.
	 */
	public int getSize()
	{
		return data.size();
	}

	/**
	 * Returns the name of this history list. This can be passed
	 * to the HistoryTextField constructor.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Returns a named model. If the specified model does not
	 * already exist, it will be created.
	 * @param name The model name
	 */
	public static HistoryModel getModel(String name)
	{
		if(models == null)
			models = new Hashtable();

		HistoryModel model = (HistoryModel)models.get(name);
		if(model == null)
		{
			model = new HistoryModel(name);
			models.put(name,model);
		}

		return model;
	}

	/**
	 * Loads the history from the specified file. jEdit calls this
	 * on startup.
	 * @param path The file path
	 */
	public static void loadHistory(String path)
	{
		if(models == null)
			models = new Hashtable();

		try
		{
			BufferedReader in = new BufferedReader(new FileReader(path));
	
			HistoryModel currentModel = null;
			String line;
	
			while((line = in.readLine()) != null)
			{
				if(line.startsWith("[") && line.endsWith("]"))
				{
					if(currentModel != null)
					{
						models.put(currentModel.getName(),
							currentModel);
					}
					currentModel = new HistoryModel(line
						.substring(1,line.length() - 1));
				}
				else if(currentModel == null)
				{
					throw new IOException("History data starts"
						+ " before model name");
				}
				else
				{
					currentModel.addItemToEnd(
						escapesToChars(line));
				}
			}
	
			if(currentModel != null)
			{
				models.put(currentModel.getName(),currentModel);
			}

			in.close();
		}
		catch(FileNotFoundException fnf)
		{
			Log.log(Log.DEBUG,HistoryModel.class,fnf);
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,HistoryModel.class,io);
		}
	}

	/**
	 * Saves the history to the specified file. jEdit calls this when
	 * it is exiting.
	 * @param path The file path
	 */
	public static void saveHistory(String path)
	{
		try
		{
			BufferedWriter out = new BufferedWriter(
				new FileWriter(path));

			if(models == null)
			{
				out.close();
				return;
			}

			Enumeration modelEnum = models.elements();
			while(modelEnum.hasMoreElements())
			{
				HistoryModel model = (HistoryModel)modelEnum
					.nextElement();

				out.write('[');
				out.write(model.getName());
				out.write("]\r\n");

				for(int i = 0; i < model.getSize(); i++)
				{
					out.write(charsToEscapes(model.getItem(i)));
					out.write("\r\n");
				}
			}

			out.close();
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,HistoryModel.class,io);
		}
	}

	// private members
	private String name;
	private int max;
	private Vector data;
	private static Hashtable models;

	private void addItemToEnd(String item)
	{
		data.addElement(item);
	}

	private static String escapesToChars(String str)
	{
		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < str.length(); i++)
		{
			char c = str.charAt(i);
			switch(c)
			{
			case '\\':
				if(i == str.length() - 1)
				{
					buf.append('\\');
					break;
				}
				c = str.charAt(++i);
				switch(c)
				{
				case 'n':
					buf.append('\n');
					break;
				case 'r':
					buf.append('\r');
					break;
				case 't':
					buf.append('\t');
					break;
				default:
					buf.append(c);
					break;
				}
				break;
			default:
				buf.append(c);
			}
		}
		return buf.toString();
	}

	private static String charsToEscapes(String str)
	{
		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < str.length(); i++)
		{
			char c = str.charAt(i);
			switch(c)
			{
			case '\n':
				buf.append("\\n");
				break;
			case '\r':
				buf.append("\\r");
				break;
			case '\t':
				buf.append("\\t");
				break;
			case '[':
				buf.append("\\[");
				break;
			case ']':
				buf.append("\\]");
				break;
			case '\\':
				buf.append("\\\\");
				break;
			default:
				buf.append(c);
				break;
			}
		}
		return buf.toString();
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.9  1999/11/21 07:59:30  sp
 * JavaDoc updates
 *
 * Revision 1.8  1999/10/31 07:15:34  sp
 * New logging API, splash screen updates, bug fixes
 *
 * Revision 1.7  1999/10/26 07:43:59  sp
 * Session loading and saving, directory list search started
 *
 * Revision 1.6  1999/05/12 05:23:41  sp
 * Fixed compile % -vs- $ bug, also HistoryModel \ bug
 *
 * Revision 1.5  1999/05/09 03:50:17  sp
 * HistoryTextField is now a text field again
 *
 * Revision 1.4  1999/05/08 00:13:00  sp
 * Splash screen change, minor documentation update, toolbar API fix
 *
 */
