/*
 * SearchAndReplace.java - Search and replace
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

package org.gjt.sp.jedit.search;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.JOptionPane;
import org.gjt.sp.jedit.textarea.*;
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
	 * Sets the current search string.
	 * @param search The new search string
	 */
	public static void setSearchString(String search)
	{
		SearchAndReplace.search = search;
		matcher = null;
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
		SearchAndReplace.replace = replace;
		matcher = null;
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
		SearchAndReplace.ignoreCase = ignoreCase;
		matcher = null;
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
		SearchAndReplace.regexp = regexp;
		matcher = null;
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
			return new RESearchMatcher(search,replace,ignoreCase);
		else
			return new LiteralSearchMatcher(search,replace,ignoreCase);
	}

	/**
	 * Sets the current search file set.
	 * @param fileset The file set to perform searches in
	 */
	public static void setSearchFileSet(SearchFileSet fileset)
	{
		SearchAndReplace.fileset = fileset;
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

		record(view,"find-next");

		view.showWaitCursor();

		try
		{
loop:			for(;;)
			{
				while(buffer != null)
				{
					int start;
					if(view.getBuffer() == buffer && !repeat)
						start = view.getTextArea()
							.getSelectionEnd();
					else
						start = 0;
					if(find(view,buffer,start))
					{
						fileset.matchFound(buffer);
						view.hideWaitCursor();
						return true;
					}

					buffer = fileset.getNextBuffer(view,buffer);
				}

				if(repeat)
				{
					// no point showing this dialog box twice
					view.getToolkit().beep();
					return false;
				}

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

		view.hideWaitCursor();
		return false;
	}

	/**
	 * Finds the next instance of the search string in the specified
	 * buffer. This calls <code>loadIfNecessary()</code> on the buffer.
	 * @param view The view
	 * @param buffer The buffer
	 * @param start Location where to start the search
	 * @exception BadLocationException if `start' is invalid
	 * @exception IllegalArgumentException if regular expression search
	 * is enabled, the search string or replacement string is invalid
	 */
	public static boolean find(View view, Buffer buffer, int start)
		throws BadLocationException, IllegalArgumentException
	{
		SearchMatcher matcher = getSearchMatcher();

		// Load buffer if necessary
		buffer.loadIfNecessary(view);

		String text = buffer.getText(start,
			buffer.getLength() - start);
		int[] match = matcher.nextMatch(text);
		if(match != null)
		{
			view.setBuffer(buffer);
			view.getTextArea().select(start + match[0],
					start + match[1]);
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
		// setSelectedText() clears these values, so save them
		int selStart = textArea.getSelectionStart();
		boolean rect = textArea.isSelectionRectangular();

		if(selStart == textArea.getSelectionEnd())
		{
			view.getToolkit().beep();
			return false;
		}

		record(view,"replace-in-selection");

		try
		{
			SearchMatcher matcher = getSearchMatcher();
			if(matcher == null)
			{
				view.getToolkit().beep();
				return false;
			}

			String replacement = matcher.substitute(textArea.getSelectedText());
			if(replacement == null)
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
	 */
	public static boolean replaceAll(View view)
	{
		int lineCount = 0;
		int fileCount = 0;

		record(view,"replace-all");

		view.showWaitCursor();

		try
		{
			Buffer buffer = fileset.getFirstBuffer(view);
			do
			{
				// Leave buffer in a consistent state if
				// an error occurs
				try
				{
					buffer.beginCompoundEdit();
					int retVal = replaceAll(view,buffer);
					if(retVal != 0)
					{
						fileCount++;
						lineCount += retVal;
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

		// Hide the wait cursor
		view.hideWaitCursor();

		if(fileCount == 0)
			view.getToolkit().beep();
		else
		{
			Object[] args = { new Integer(lineCount),
				new Integer(fileCount) };
			GUIUtilities.message(view,"replace-results",args);
		}

		return (fileCount != 0);
	}

	/**
	 * Replaces all occurances of the search string with the replacement
	 * string. This calls <code>loadIfNecessary()</code> on the buffer.
	 * @param view The view
	 * @param buffer The buffer
	 * @return True if the replace operation was successful, false
	 * if no matches were found
	 */
	public static int replaceAll(View view, Buffer buffer)
		throws BadLocationException
	{
		if(!view.getTextArea().isEditable())
			return 0;

		SearchMatcher matcher = getSearchMatcher();
		if(matcher == null)
			return 0;

		// Load buffer if necessary
		buffer.loadIfNecessary(view);

		int lineCount = 0;
		Element map = buffer.getDefaultRootElement();

		for(int i = 0; i < map.getElementCount(); i++)
		{
			Element lineElement = map.getElement(i);
			int lineStart = lineElement.getStartOffset();
			int lineEnd = lineElement.getEndOffset()
				- lineStart - 1;

			String line = buffer.getText(lineStart,lineEnd);
			String newLine = matcher.substitute(line);
			if(newLine == null)
				continue;
			buffer.remove(lineStart,lineEnd);
			buffer.insertString(lineStart,newLine,null);

			lineCount++;
		}

		return lineCount;
	}

	/**
	 * Loads search and replace state from the properties.
	 */
	public static void load()
	{
		fileset = new CurrentBufferSet();
		search = jEdit.getProperty("search.find.value");
		replace = jEdit.getProperty("search.replace.value");
		regexp = "on".equals(jEdit.getProperty("search.regexp.toggle"));
		ignoreCase = "on".equals(jEdit.getProperty("search.ignoreCase.toggle"));
	}

	/**
	 * Saves search and replace state to the properties.
	 */
	public static void save()
	{
		jEdit.setProperty("search.find.value",search);
		jEdit.setProperty("search.replace.value",replace);
		jEdit.setProperty("search.ignoreCase.toggle",
			ignoreCase ? "on" : "off");
		jEdit.setProperty("search.regexp.toggle",
			regexp ? "on" : "off");
	}

	// private members
	private static String search;
	private static String replace;
	private static boolean regexp;
	private static boolean ignoreCase;
	private static SearchMatcher matcher;
	private static SearchFileSet fileset;

	private static void record(View view, String action)
	{
		InputHandler.MacroRecorder recorder = view.getTextArea()
			.getInputHandler().getMacroRecorder();

		if(recorder != null)
		{
			recorder.actionPerformed(jEdit.getAction("set-search-string"),
				search);
			recorder.actionPerformed(jEdit.getAction("set-replace-string"),
				replace);

			StringBuffer buf = new StringBuffer();
			if(regexp)
				buf.append("regexp ");
			else
				buf.append("literal ");
			if(ignoreCase)
				buf.append("icase");
			else
				buf.append("case");
			if(fileset instanceof CurrentBufferSet)
				buf.append(" current");
			else if(fileset instanceof AllBufferSet)
				buf.append(" all");

			recorder.actionPerformed(jEdit.getAction("set-search-parameters"),
				buf.toString());

			recorder.actionPerformed(jEdit.getAction(action),null);
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.21  1999/12/05 03:01:05  sp
 * Perl token marker bug fix, file loading is deferred, style option pane fix
 *
 * Revision 1.20  1999/11/29 02:45:50  sp
 * Scroll bar position saved when switching buffers
 *
 * Revision 1.19  1999/11/28 00:33:07  sp
 * Faster directory search, actions slimmed down, faster exit/close-all
 *
 * Revision 1.18  1999/10/31 07:15:34  sp
 * New logging API, splash screen updates, bug fixes
 *
 * Revision 1.17  1999/10/24 06:04:00  sp
 * QuickSearch in tool bar, auto indent updates, macro recorder updates
 *
 * Revision 1.16  1999/10/23 03:48:22  sp
 * Mode system overhaul, close all dialog box, misc other stuff
 *
 * Revision 1.15  1999/10/11 07:14:22  sp
 * matchFound()
 *
 * Revision 1.14  1999/10/06 05:52:34  sp
 * Macros were being played twice, dialog box shows how many replacements
 * 'replace all' made, tab command bug fix, documentation updates
 *
 * Revision 1.13  1999/10/02 01:12:36  sp
 * Search and replace updates (doesn't work yet), some actions moved to TextTools
 *
 * Revision 1.12  1999/09/30 12:21:04  sp
 * No net access for a month... so here's one big jEdit 2.1pre1
 *
 * Revision 1.11  1999/07/16 23:45:49  sp
 * 1.7pre6 BugFree version
 *
 * Revision 1.10  1999/07/08 06:06:04  sp
 * Bug fixes and miscallaneous updates
 *
 * Revision 1.9  1999/07/05 04:38:39  sp
 * Massive batch of changes... bug fixes, also new text component is in place.
 * Have fun
 *
 * Revision 1.8  1999/06/12 02:30:27  sp
 * Find next can now perform multifile searches, multifile-search command added,
 * new style option pane
 *
 * Revision 1.7  1999/06/09 07:28:10  sp
 * Multifile search and replace tweaks, removed console.html
 *
 * Revision 1.6  1999/06/09 05:22:11  sp
 * Find next now supports multi-file searching, minor Perl mode tweak
 *
 * Revision 1.5  1999/06/06 05:05:25  sp
 * Search and replace tweaks, Perl/Shell Script mode updates
 *
 * Revision 1.4  1999/06/03 08:24:13  sp
 * Fixing broken CVS
 *
 * Revision 1.4  1999/05/31 04:38:51  sp
 * Syntax optimizations, HyperSearch for Selection added (Mike Dillon)
 *
 * Revision 1.3  1999/05/30 01:28:43  sp
 * Minor search and replace updates
 *
 * Revision 1.2  1999/05/29 08:06:56  sp
 * Search and replace overhaul
 *
 */
