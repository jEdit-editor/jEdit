/*
 * SearchAndReplace.java - Search and replace
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

package org.gjt.sp.jedit.search;

import javax.swing.text.BadLocationException;
import javax.swing.text.Segment;
import javax.swing.JOptionPane;
import java.awt.Component;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.msg.SearchSettingsChanged;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

/**
 * Class that implements regular expression and literal search within
 * jEdit buffers.
 * @author Slava Pestov
 * @version $Id$
 */
public class SearchAndReplace
{
	/**
	 * Displays the search & replace dialog box for the specified view.
	 * @param view The view
	 * @param defaultFind The initial search string
	 * @since jEdit 2.7pre1
	 */
	public static void showSearchDialog(View view, String defaultFind)
	{
		SearchDialog dialog = (SearchDialog)view.getRootPane()
			.getClientProperty(SEARCH_DIALOG_KEY);
		if(dialog == null)
		{
			dialog = new SearchDialog(view);
			view.getRootPane().putClientProperty(SEARCH_DIALOG_KEY,dialog);
		}

		dialog.setSearchString(defaultFind);
	}

	/**
	 * Displays the HyperSearch dialog box for the specified view.
	 * @param view The view
	 * @param defaultFind The initial search string
	 * @since jEdit 2.7pre1
	 */
	public static void showHyperSearchDialog(View view, String defaultFind)
	{
		HyperSearch dialog = (HyperSearch)view.getRootPane()
			.getClientProperty(HYPERSEARCH_DIALOG_KEY);
		if(dialog == null)
		{
			dialog = new HyperSearch(view);
			view.getRootPane().putClientProperty(HYPERSEARCH_DIALOG_KEY,dialog);
		}

		dialog.setSearchString(defaultFind);
	}

	/**
	 * Sets the current search string.
	 * @param search The new search string
	 */
	public static void setSearchString(String search)
	{
		if(search.equals(SearchAndReplace.search))
			return;

		SearchAndReplace.search = search;
		matcher = null;

		EditBus.send(new SearchSettingsChanged(null));
	}

	/**
	 * Returns the current search string.
	 */
	public static String getSearchString()
	{
		return search;
	}

	/**
	 * Sets the current replacement string.
	 * @param search The new replacement string
	 */
	public static void setReplaceString(String replace)
	{
		if(replace.equals(SearchAndReplace.replace))
			return;

		SearchAndReplace.replace = replace;
		matcher = null;

		EditBus.send(new SearchSettingsChanged(null));
	}

	/**
	 * Returns the current replacement string.
	 */
	public static String getReplaceString()
	{
		return replace;
	}

	/**
	 * Sets the ignore case flag.
	 * @param ignoreCase True if searches should be case insensitive,
	 * false otherwise
	 */
	public static void setIgnoreCase(boolean ignoreCase)
	{
		if(ignoreCase == SearchAndReplace.ignoreCase)
			return;

		SearchAndReplace.ignoreCase = ignoreCase;
		matcher = null;

		EditBus.send(new SearchSettingsChanged(null));
	}

	/**
	 * Returns the state of the ignore case flag.
	 * @return True if searches should be case insensitive,
	 * false otherwise
	 */
	public static boolean getIgnoreCase()
	{
		return ignoreCase;
	}

	/**
	 * Sets the state of the regular expression flag.
	 * @param regexp True if regular expression searches should be
	 * performed
	 */
	public static void setRegexp(boolean regexp)
	{
		if(regexp == SearchAndReplace.regexp)
			return;

		SearchAndReplace.regexp = regexp;
		matcher = null;

		EditBus.send(new SearchSettingsChanged(null));
	}

	/**
	 * Returns the state of the regular expression flag.
	 * @return True if regular expression searches should be performed
	 */
	public static boolean getRegexp()
	{
		return regexp;
	}

	/**
	 * Sets the current search string matcher. Note that calling
	 * <code>setSearchString</code>, <code>setReplaceString</code>,
	 * <code>setIgnoreCase</code> or <code>setRegExp</code> will
	 * reset the matcher to the default.
	 */
	public static void setSearchMatcher(SearchMatcher matcher)
	{
		SearchAndReplace.matcher = matcher;

		EditBus.send(new SearchSettingsChanged(null));
	}

	/**
	 * Returns the current search string matcher.
	 * @exception IllegalArgumentException if regular expression search
	 * is enabled, the search string or replacement string is invalid
	 */
	public static SearchMatcher getSearchMatcher()
		throws IllegalArgumentException
	{
		if(matcher != null)
			return matcher;

		if(search == null || "".equals(search))
			return null;

		if(regexp)
			matcher = new RESearchMatcher(search,replace,ignoreCase);
		else
			matcher = new BoyerMooreSearchMatcher(search,replace,ignoreCase);

		return matcher;
	}

	/**
	 * Sets the current search file set.
	 * @param fileset The file set to perform searches in
	 */
	public static void setSearchFileSet(SearchFileSet fileset)
	{
		SearchAndReplace.fileset = fileset;

		EditBus.send(new SearchSettingsChanged(null));
	}

	/**
	 * Returns the current search file set.
	 */
	public static SearchFileSet getSearchFileSet()
	{
		return fileset;
	}

	/**
	 * Finds the next occurance of the search string.
	 * @param view The view
	 * @return True if the operation was successful, false otherwise
	 */
	public static boolean find(View view)
	{
		boolean repeat = false;
		Buffer buffer = fileset.getNextBuffer(view,null);

		SearchMatcher matcher = getSearchMatcher();
		if(matcher == null)
		{
			view.getToolkit().beep();
			return false;
		}

		record(view,"SearchAndReplace.find(view);",false,true);

		view.showWaitCursor();

		try
		{
loop:			for(;;)
			{
				while(buffer != null)
				{
					// Wait for the buffer to load
					if(!buffer.isLoaded())
						VFSManager.waitForRequests();

					int start;
					if(view.getBuffer() == buffer && !repeat)
						start = view.getTextArea()
							.getSelectionEnd();
					else
						start = 0;
					if(find(view,buffer,start))
						return true;

					buffer = fileset.getNextBuffer(view,buffer);
				}

				if(repeat)
				{
					// no point showing this dialog box twice
					view.getToolkit().beep();
					return false;
				}

				/* Don't do this when playing a macro */
				if(BeanShell.isScriptRunning())
					break loop;

				int result = JOptionPane.showConfirmDialog(view,
					jEdit.getProperty("keepsearching.message"),
					jEdit.getProperty("keepsearching.title"),
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE);
				if(result == JOptionPane.YES_OPTION)
				{
					// start search from beginning
					buffer = fileset.getFirstBuffer(view);
					repeat = true;
				}
				else
					break loop;
			}
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,SearchAndReplace.class,e);
			Object[] args = { e.getMessage() };
			if(args[0] == null)
				args[0] = e.toString();
			GUIUtilities.error(view,"searcherror",args);
		}
		finally
		{
			view.hideWaitCursor();
		}

		return false;
	}

	/**
	 * Finds the next instance of the search string in the specified
	 * buffer.
	 * @param view The view
	 * @param buffer The buffer
	 * @param start Location where to start the search
	 * @exception BadLocationException if `start' is invalid
	 * @exception IllegalArgumentException if regular expression search
	 * is enabled, the search string or replacement string is invalid
	 */
	public static boolean find(final View view, final Buffer buffer, final int start)
		throws BadLocationException, IllegalArgumentException
	{
		SearchMatcher matcher = getSearchMatcher();

		Segment text = new Segment();
		buffer.getText(start,buffer.getLength() - start,text);

		int[] match = matcher.nextMatch(text);
		if(match != null)
		{
			fileset.matchFound(buffer);
			view.setBuffer(buffer);
			view.getTextArea().select(start + match[0],start + match[1]);
			return true;
		}
		else
			return false;
	}

	/**
	 * Replaces the current selection with the replacement string.
	 * @param view The view
	 * @return True if the operation was successful, false otherwise
	 */
	public static boolean replace(View view)
	{
		JEditTextArea textArea = view.getTextArea();

		if(!textArea.isEditable())
		{
			view.getToolkit().beep();
			return false;
		}

		// setSelectedText() clears these values, so save them
		int selStart = textArea.getSelectionStart();
		boolean rect = textArea.isSelectionRectangular();

		if(selStart == textArea.getSelectionEnd())
		{
			view.getToolkit().beep();
			return false;
		}

		record(view,"SearchAndReplace.replace(view);",true,false);

		try
		{
			SearchMatcher matcher = getSearchMatcher();
			if(matcher == null)
			{
				view.getToolkit().beep();
				return false;
			}

			String text = textArea.getSelectedText();
			String replacement = matcher.substitute(text);
			if(replacement == null || replacement.equals(text))
				return false;

			textArea.setSelectedText(replacement);
			textArea.setSelectionStart(selStart);
			textArea.setSelectionRectangular(rect);
			return true;
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,SearchAndReplace.class,e);
			Object[] args = { e.getMessage() };
			if(args[0] == null)
				args[0] = e.toString();
			GUIUtilities.error(view,"searcherror",args);
		}

		return false;
	}

	/**
	 * Replaces all occurances of the search string with the replacement
	 * string.
	 * @param view The view
	 */
	public static boolean replaceAll(View view)
	{
		int fileCount = 0;
		int occurCount = 0;

		record(view,"SearchAndReplace.replaceAll(view);",true,true);

		view.showWaitCursor();

		try
		{
			Buffer buffer = fileset.getFirstBuffer(view);
			do
			{
				// Wait for buffer to finish loading
				if(buffer.isPerformingIO())
					VFSManager.waitForRequests();

				// Leave buffer in a consistent state if
				// an error occurs
				try
				{
					buffer.beginCompoundEdit();
					int retVal = replaceAll(view,buffer);
					if(retVal != 0)
					{
						fileCount++;
						occurCount += retVal;
						fileset.matchFound(buffer);
					}
				}
				finally
				{
					buffer.endCompoundEdit();
				}
			}
			while((buffer = fileset.getNextBuffer(view,buffer)) != null);
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,SearchAndReplace.class,e);
			Object[] args = { e.getMessage() };
			if(args[0] == null)
				args[0] = e.toString();
			GUIUtilities.error(view,"searcherror",args);
		}
		finally
		{
			view.hideWaitCursor();
		}

		/* Don't do this when playing a macro, cos it's annoying */
		if(!BeanShell.isScriptRunning())
		{
			if(fileCount == 0)
				view.getToolkit().beep();
			else
			{
				Object[] args = { new Integer(occurCount),
					new Integer(fileCount) };
				GUIUtilities.message(view,"replace-results",args);
			}
		}

		return (fileCount != 0);
	}

	/**
	 * Replaces all occurances of the search string with the replacement
	 * string.
	 * @param view The view
	 * @param buffer The buffer
	 * @return True if the replace operation was successful, false
	 * if no matches were found
	 */
	public static int replaceAll(View view, Buffer buffer)
		throws BadLocationException
	{
		if(!buffer.isEditable())
			return 0;

		SearchMatcher matcher = getSearchMatcher();
		if(matcher == null)
			return 0;

		int occurCount = 0;

		Segment text = new Segment();
		int offset = 0;
loop:		for(;;)
		{
			buffer.getText(offset,buffer.getLength() - offset,text);
			int[] occur = matcher.nextMatch(text);
			if(occur == null)
				break loop;
			int start = occur[0] + offset;
			int end = occur[1] - occur[0];
			String found = buffer.getText(start,end);
			found = matcher.substitute(found);
			if(found != null)
			{
				buffer.remove(start,end);
				buffer.insertString(start,found,null);
				occurCount++;
				offset = start + found.length();
			}
			else
				offset += end;
		}

		return occurCount;
	}

	/**
	 * Loads search and replace state from the properties.
	 */
	public static void load()
	{
		fileset = new CurrentBufferSet();
		search = jEdit.getProperty("search.find.value");
		replace = jEdit.getProperty("search.replace.value");
		regexp = jEdit.getBooleanProperty("search.regexp.toggle");
		ignoreCase = jEdit.getBooleanProperty("search.ignoreCase.toggle");
	}

	/**
	 * Saves search and replace state to the properties.
	 */
	public static void save()
	{
		jEdit.setProperty("search.find.value",search);
		jEdit.setProperty("search.replace.value",replace);
		jEdit.setBooleanProperty("search.ignoreCase.toggle",ignoreCase);
		jEdit.setBooleanProperty("search.regexp.toggle",regexp);
	}

	// private members
	private static final String SEARCH_DIALOG_KEY = "SearchDialog";
	private static final String HYPERSEARCH_DIALOG_KEY = "HyperSearch";

	private static String search;
	private static String replace;
	private static boolean regexp;
	private static boolean ignoreCase;
	private static SearchMatcher matcher;
	private static SearchFileSet fileset;

	private static void record(View view, String action,
		boolean recordReplaceString, boolean recordFileSet)
	{
		Macros.Recorder recorder = view.getMacroRecorder();

		if(recorder != null)
		{
			recorder.record("SearchAndReplace.setSearchString(\""
				+ MiscUtilities.charsToEscapes(search) + "\");");

			if(recordReplaceString)
			{
				recorder.record("SearchAndReplace.setReplaceString(\""
					+ MiscUtilities.charsToEscapes(replace) + "\");");
			}

			recorder.record("SearchAndReplace.setIgnoreCase("
				+ ignoreCase + ");");
			recorder.record("SearchAndReplace.setRegexp("
				+ regexp + ");");

			if(recordFileSet)
			{
				if(fileset instanceof CurrentBufferSet)
				{
					recorder.record("SearchAndReplace.setSearchFileSet("
					+ "new CurrentBufferSet());");
				}
				else if(fileset instanceof AllBufferSet)
				{
					recorder.record("SearchAndReplace.setSearchFileSet("
						+ "new AllBufferSet());");
				}
			}

			recorder.record(action);
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.42  2000/11/19 07:51:25  sp
 * Documentation updates, bug fixes
 *
 * Revision 1.41  2000/11/16 10:25:18  sp
 * More macro work
 *
 * Revision 1.40  2000/11/16 04:01:12  sp
 * BeanShell macros started
 *
 * Revision 1.39  2000/11/07 10:08:32  sp
 * Options dialog improvements, documentation changes, bug fixes
 *
 * Revision 1.38  2000/11/05 00:44:15  sp
 * Improved HyperSearch, improved horizontal scroll, other stuff
 *
 * Revision 1.37  2000/11/02 09:19:34  sp
 * more features
 *
 * Revision 1.36  2000/10/13 06:57:20  sp
 * Edit User/System Macros command, gutter mouse handling improved
 *
 * Revision 1.35  2000/08/16 08:47:19  sp
 * Stuff
 *
 * Revision 1.34  2000/06/12 02:43:30  sp
 * pre6 almost ready
 *
 * Revision 1.33  2000/06/06 04:38:09  sp
 * WorkThread's AWT request stuff reworked
 *
 * Revision 1.32  2000/05/04 10:37:04  sp
 * Wasting time
 *
 * Revision 1.31  2000/04/28 09:29:12  sp
 * Key binding handling improved, VFS updates, some other stuff
 *
 * Revision 1.30  2000/04/27 08:32:57  sp
 * VFS fixes, read only fixes, macros can prompt user for input, improved
 * backup directory feature
 *
 */
