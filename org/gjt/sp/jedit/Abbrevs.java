/*
 * Abbrevs.java - Abbreviation manager
 * Copyright (C) 1999, 2000 Slava Pestov
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
	 * @since jEdit 2.6pre4
	 */
	public static boolean expandAbbrev(View view, boolean add)
	{
		JEditTextArea textArea = view.getTextArea();
		if(!textArea.isEditable())
		{
			view.getToolkit().beep();
			return false;
		}

		Buffer buffer = view.getBuffer();

		int line = textArea.getCaretLine();
		int lineStart = textArea.getLineStartOffset(line);
		int caret = textArea.getCaretPosition();

		String lineText = textArea.getLineText(line);
		if(lineText.length() == 0)
		{
			if(add)
				view.getToolkit().beep();
			return false;
		}

		int pos = caret - lineStart;
		if(pos == 0)
		{
			if(add)
				view.getToolkit().beep();
			return false;
		}

		int wordStart = TextUtilities.findWordStart(lineText,pos - 1,
			(String)buffer.getProperty("noWordSep"));

		String abbrev = lineText.substring(wordStart,pos);
		Expansion expand = Abbrevs.expandAbbrev(buffer.getMode().getName(),abbrev);

		if(expand == null)
		{
			if(add)
				addAbbrev(view,abbrev);
			return false;
		}
		else
		{
			buffer.beginCompoundEdit();
			try
			{
				buffer.remove(lineStart + wordStart,pos - wordStart);
				buffer.insertString(lineStart + wordStart,expand.text,null);
				if(expand.caretPosition != -1)
				{
					textArea.setCaretPosition(lineStart + wordStart
						+ expand.caretPosition);
				}
			}
			catch(BadLocationException bl)
			{
				Log.log(Log.ERROR,Abbrevs.class,bl);
			}
			buffer.endCompoundEdit();

			return true;
		}
	}

	/**
	 * Locates a completion for the specified abbrev.
	 * @param mode The edit mode
	 * @param abbrev The abbrev
	 * @since jEdit 2.6pre4
	 */
	public static Expansion expandAbbrev(String mode, String abbrev)
	{
		// try mode-specific abbrevs first
		Expansion expand = null;
		Hashtable modeAbbrevs = (Hashtable)modes.get(mode);
		if(modeAbbrevs != null)
			expand = (Expansion)modeAbbrevs.get(abbrev);

		if(expand == null)
			expand = (Expansion)globalAbbrevs.get(abbrev);

		return expand;
	}

	/**
	 * An abbreviation expansion.
	 * @since jEdit 2.6pre4
	 */
	public static class Expansion
	{
		public String text;
		public int caretPosition = -1;
		public int lineCount;

		public Expansion(String text)
		{
			StringBuffer buf = new StringBuffer();
			boolean backslash = false;

			for(int i = 0; i < text.length(); i++)
			{
				char ch = text.charAt(i);
				if(ch == '\\')
					backslash = true;
				else if(backslash)
				{
					if(ch == '|')
						caretPosition = buf.length();
					else if(ch == 'n')
					{
						buf.append('\n');
						lineCount++;
					}
					else
						buf.append(ch);
				}
				else
					buf.append(ch);
			}

			this.text = buf.toString();
		}

		public String toString()
		{
			StringBuffer buf = new StringBuffer();
			for(int i = 0; i < text.length(); i++)
			{
				if(i == caretPosition)
					buf.append("\\|");

				char ch = text.charAt(i);
				if(ch == '\n')
					buf.append("\\n");
				else if(ch == '\\')
					buf.append("\\\\");
				else
					buf.append(ch);
			}
			return buf.toString();
		}
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
		abbrevsChanged = true;
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
		abbrevsChanged = true;
		Abbrevs.modes = modes;
	}

	// package-private members
	static void load()
	{
		expandOnInput = jEdit.getBooleanProperty("view.expandOnInput");

		globalAbbrevs = new Hashtable();
		modes = new Hashtable();

		boolean loaded = false;

		String settings = jEdit.getSettingsDirectory();
		if(settings != null)
		{
			File file = new File(MiscUtilities.constructPath(settings,"abbrevs"));
			abbrevsModTime = file.lastModified();

			try
			{
				loadAbbrevs(new FileReader(file));
				loaded = true;
			}
			catch(FileNotFoundException fnf)
			{
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,Abbrevs.class,"Error while loading " + file);
				Log.log(Log.ERROR,Abbrevs.class,e);
			}
		}

		// only load global abbrevs if user abbrevs file could not be loaded
		if(!loaded)
		{
			try
			{
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
		jEdit.setBooleanProperty("view.expandOnInput",expandOnInput);

		String settings = jEdit.getSettingsDirectory();
		if(abbrevsChanged && settings != null)
		{
			File file = new File(MiscUtilities.constructPath(settings,"abbrevs"));
			if(file.exists() && file.lastModified() != abbrevsModTime)
			{
				Log.log(Log.WARNING,Abbrevs.class,file + " changed on disk;"
					+ " will not save abbrevs");
			}
			else
			{
				try
				{
					saveAbbrevs(new FileWriter(file));
				}
				catch(Exception e)
				{
					Log.log(Log.ERROR,Abbrevs.class,"Error while saving " + file);
					Log.log(Log.ERROR,Abbrevs.class,e);
				}
				abbrevsModTime = file.lastModified();
			}
		}
	}

	// private members
	private static boolean abbrevsChanged;
	private static long abbrevsModTime;
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
			else if(line.startsWith("[") && line.indexOf('|') == -1)
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
					new Expansion(line.substring(index + 1)));
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
			String abbrev = (String)keys.nextElement();
			out.write(abbrev);
			out.write('|');
			out.write(values.nextElement().toString());
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
		if(expand == null || (retVal != 0 && retVal != 1))
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
			modeAbbrevs.put(abbrev,new Expansion(expand));
		}
		else
			globalAbbrevs.put(abbrev,new Expansion(expand));

		abbrevsChanged = true;

		expandAbbrev(view,false);
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.13  2000/08/22 07:25:00  sp
 * Improved abbrevs, bug fixes
 *
 * Revision 1.12  2000/07/19 08:35:58  sp
 * plugin devel docs updated, minor other changes
 *
 * Revision 1.11  2000/04/27 08:32:56  sp
 * VFS fixes, read only fixes, macros can prompt user for input, improved
 * backup directory feature
 *
 * Revision 1.10  2000/04/15 04:14:46  sp
 * XML files updated, jEdit.get/setBooleanProperty() method added
 *
 * Revision 1.9  2000/03/21 07:18:53  sp
 * bug fixes
 *
 * Revision 1.8  2000/03/11 03:02:15  sp
 * 2.3final
 *
 * Revision 1.7  2000/03/04 03:39:54  sp
 * *** empty log message ***
 *
 * Revision 1.6  2000/02/15 07:44:30  sp
 * bug fixes, doc updates, etc
 *
 */
