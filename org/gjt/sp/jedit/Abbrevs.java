/*
 * Abbrevs.java - Abbreviation manager
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

import javax.swing.text.BadLocationException;
import javax.swing.*;
import java.io.*;
import java.util.*;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.util.Log;

/**
 * Abbreviation manager.
 * @author Slava Pestov
 * @version $Id$
 */
public class Abbrevs
{
	/**
	 * Returns if abbreviations should be expanded after the
	 * user finishes typing a word.
	 */
	public static boolean getExpandOnInput()
	{
		return expandOnInput;
	}

	/**
	 * Sets if abbreviations should be expanded after the
	 * user finishes typing a word.
	 * @param true If true, typing a non-alphanumeric characater will
	 * automatically attempt to expand the current abbrev
	 */
	public static void setExpandOnInput(boolean expandOnInput)
	{
		Abbrevs.expandOnInput = expandOnInput;
	}

	/**
	 * Expands the abbrev at the caret position in the specified
	 * view.
	 * @param view The view
	 * @param add If true and abbrev not found, will ask user if
	 * it should be added
	 * @since jEdit 2.3pre3
	 */
	public static void expandAbbrev(View view, boolean add)
	{
		JEditTextArea textArea = view.getTextArea();
		Buffer buffer = view.getBuffer();

		int line = textArea.getCaretLine();
		int lineStart = textArea.getLineStartOffset(line);
		int caret = textArea.getCaretPosition();

		String lineText = textArea.getLineText(line);
		if(lineText.length() == 0)
			return;

		int pos = caret - lineStart;
		if(pos == 0)
			return;

		int wordStart = TextUtilities.findWordStart(lineText,pos - 1,
			(String)buffer.getProperty("noWordSep"));

		String abbrev = lineText.substring(wordStart,pos);
		String expand = Abbrevs.expandAbbrev(buffer.getMode().getName(),abbrev);

		if(expand == null)
		{
			if(add)
				addAbbrev(view,abbrev);
			return;
		}
		else
		{
			buffer.beginCompoundEdit();
			try
			{
				buffer.remove(lineStart + wordStart,pos - wordStart);
				buffer.insertString(lineStart + wordStart,expand,null);
			}
			catch(BadLocationException bl)
			{
				Log.log(Log.ERROR,Abbrevs.class,bl);
			}
			buffer.endCompoundEdit();

		}
	}

	/**
	 * Locates a completion for the specified abbrev.
	 * @param mode The edit mode
	 * @param abbrev The abbrev
	 * @since jEdit 2.3pre1
	 */
	public static String expandAbbrev(String mode, String abbrev)
	{
		// try mode-specific abbrevs first
		Hashtable modeAbbrevs = (Hashtable)modes.get(mode);
		if(modeAbbrevs != null)
		{
			String expand = (String)modeAbbrevs.get(abbrev);
			if(expand != null)
				return expand;
		}

		return (String)globalAbbrevs.get(abbrev);
	}

	/**
	 * Returns the global abbreviation set.
	 * @since jEdit 2.3pre1
	 */
	public static Hashtable getGlobalAbbrevs()
	{
		return globalAbbrevs;
	}

	/**
	 * Sets the global abbreviation set.
	 * @param globalAbbrevs The new global abbrev set
	 * @since jEdit 2.3pre1
	 */
	public static void setGlobalAbbrevs(Hashtable globalAbbrevs)
	{
		Abbrevs.globalAbbrevs = globalAbbrevs;
	}

	/**
	 * Returns the mode-specific abbreviation set.
	 * @since jEdit 2.3pre1
	 */
	public static Hashtable getModeAbbrevs()
	{
		return modes;
	}

	/**
	 * Sets the mode-specific abbreviation set.
	 * @param globalAbbrevs The new global abbrev set
	 * @since jEdit 2.3pre1
	 */
	public static void setModeAbbrevs(Hashtable modes)
	{
		Abbrevs.modes = modes;
	}

	// package-private members
	static void load()
	{
		expandOnInput = "yes".equals(jEdit.getProperty("view.expandOnInput"));

		globalAbbrevs = new Hashtable();
		modes = new Hashtable();

		boolean loaded = false;

		String settings = jEdit.getSettingsDirectory();
		if(settings != null)
		{
			String path = MiscUtilities.constructPath(settings,"abbrevs");

			try
			{
				Log.log(Log.MESSAGE,Abbrevs.class,"Loading " + path);
				loadAbbrevs(new FileReader(path));
				loaded = true;
			}
			catch(FileNotFoundException fnf)
			{
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,Abbrevs.class,"Error while loading " + path);
				Log.log(Log.ERROR,Abbrevs.class,e);
			}
		}

		// only load global abbrevs if user abbrevs file could not be loaded
		if(!loaded)
		{
			try
			{
				Log.log(Log.MESSAGE,Abbrevs.class,"Loading default.abbrevs");
				loadAbbrevs(new InputStreamReader(Abbrevs.class
					.getResourceAsStream("default.abbrevs")));
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,Abbrevs.class,"Error while loading default.abbrevs");
				Log.log(Log.ERROR,Abbrevs.class,e);
			}
		}
	}

	static void save()
	{
		jEdit.setProperty("view.expandOnInput",expandOnInput
			? "yes" : "no");

		String settings = jEdit.getSettingsDirectory();
		if(settings != null)
		{
			String path = MiscUtilities.constructPath(settings,"abbrevs");

			try
			{
				Log.log(Log.MESSAGE,Abbrevs.class,"Saving " + path);
				saveAbbrevs(new FileWriter(path));
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,Abbrevs.class,"Error while saving " + path);
				Log.log(Log.ERROR,Abbrevs.class,e);
			}
		}
	}

	// private members
	private static boolean expandOnInput;
	private static Hashtable globalAbbrevs;
	private static Hashtable modes;

	private Abbrevs() {}

	private static void loadAbbrevs(Reader _in) throws Exception
	{
		BufferedReader in = new BufferedReader(_in);

		Hashtable currentAbbrevs = null;

		String line;
		while((line = in.readLine()) != null)
		{
			if(line.length() == 0)
				continue;
			else if(line.startsWith("["))
			{
				if(line.equals("[global]"))
					currentAbbrevs = globalAbbrevs;
				else
				{
					String mode = line.substring(1,
						line.length() - 1);
					currentAbbrevs = (Hashtable)modes.get(mode);
					if(currentAbbrevs == null)
					{
						currentAbbrevs = new Hashtable();
						modes.put(mode,currentAbbrevs);
					}
				}
			}
			else
			{
				int index = line.indexOf('|');
				currentAbbrevs.put(line.substring(0,index),
					MiscUtilities.escapesToChars(
					line.substring(index + 1)));
			}
		}

		in.close();
	}

	private static void saveAbbrevs(Writer _out) throws Exception
	{
		BufferedWriter out = new BufferedWriter(_out);
		String lineSep = System.getProperty("line.separator");

		// write global abbrevs
		out.write("[global]");
		out.write(lineSep);

		saveAbbrevs(out,globalAbbrevs);

		// write mode abbrevs
		Enumeration keys = modes.keys();
		Enumeration values = modes.elements();
		while(keys.hasMoreElements())
		{
			out.write('[');
			out.write((String)keys.nextElement());
			out.write(']');
			out.write(lineSep);
			saveAbbrevs(out,(Hashtable)values.nextElement());
		}

		out.close();
	}

	private static void saveAbbrevs(Writer out, Hashtable abbrevs)
		throws Exception
	{
		String lineSep = System.getProperty("line.separator");

		Enumeration keys = abbrevs.keys();
		Enumeration values = abbrevs.elements();
		while(keys.hasMoreElements())
		{
			out.write((String)keys.nextElement());
			out.write('|');
			out.write(MiscUtilities.charsToEscapes((String)values.nextElement()));
			out.write(lineSep);
		}
	}

	private static void addAbbrev(View view, String abbrev)
	{
		String[] args = { abbrev };
		JTextField textField = new JTextField();
		Object[] message = {
			jEdit.getProperty("add-abbrev.message",args),
			textField };
		Object[] options = { jEdit.getProperty("add-abbrev.global"),
			jEdit.getProperty("add-abbrev.mode"),
			jEdit.getProperty("common.cancel") };

		int retVal = JOptionPane.showOptionDialog(view,message,
			jEdit.getProperty("add-abbrev.title"),
			JOptionPane.YES_NO_OPTION,
			JOptionPane.QUESTION_MESSAGE,
			null,options,options[0]);

		String expand = textField.getText();
		if(expand == null || retVal == 2)
			return;

		if(retVal == 1)
		{
			String mode = view.getBuffer().getMode().getName();
			Hashtable modeAbbrevs = (Hashtable)modes.get(mode);
			if(modeAbbrevs == null)
			{
				modeAbbrevs = new Hashtable();
				modes.put(mode,modeAbbrevs);
			}
			modeAbbrevs.put(abbrev,expand);
		}
		else
			globalAbbrevs.put(abbrev,expand);

		expandAbbrev(view,false);
	}
}
