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
import javax.swing.text.Element;
import javax.swing.JOptionPane;
import java.awt.Component;
import org.gjt.sp.jedit.gui.InputHandler;
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
	 * @param comp The component to display dialog boxes for
	 * @return True if the operation was successful, false otherwise
	 */
	public static boolean find(View view, Component comp)
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
					// Wait for the buffer to load
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

				int result = JOptionPane.showConfirmDialog(comp,
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
			GUIUtilities.error(comp,"searcherror",args);
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

		String text = buffer.getText(start,
			buffer.getLength() - start);
		final int[] match = matcher.nextMatch(text);
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
	 * @param comp The component
	 * @return True if the operation was successful, false otherwise
	 */
	public static boolean replace(View view, Component comp)
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
			GUIUtilities.error(comp,"searcherror",args);
		}

		return false;
	}

	/**
	 * Replaces all occurances of the search string with the replacement
	 * string.
	 * @param comp The component
	 */
	public static boolean replaceAll(View view, Component comp)
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
				// Wait for buffer to finish loading
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
			GUIUtilities.error(comp,"searcherror",args);
		}
		finally
		{
			view.hideWaitCursor();
		}

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
	 * string.
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
	private static String search;
	private static String replace;
	private static boolean regexp;
	private static boolean ignoreCase;
	private static SearchMatcher matcher;
	private static SearchFileSet fileset;

	private static void record(View view, String action)
	{
		InputHandler.MacroRecorder recorder = view.getInputHandler()
			.getMacroRecorder();

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
 * Revision 1.29  2000/04/25 03:32:40  sp
 * Even more VFS hacking
 *
 * Revision 1.28  2000/04/15 04:14:47  sp
 * XML files updated, jEdit.get/setBooleanProperty() method added
 *
 * Revision 1.27  2000/04/06 02:22:12  sp
 * Incremental search, documentation updates
 *
 * Revision 1.26  2000/04/01 12:21:27  sp
 * mode cache implemented
 *
 * Revision 1.25  2000/01/31 05:04:48  sp
 * C+e C+x will ask to add abbrev if not found, other minor updates
 *
 * Revision 1.24  2000/01/14 07:50:51  sp
 * Documentation updates, faster literal search, GUI updates, bug fixes
 *
 * Revision 1.23  1999/12/20 06:05:27  sp
 * Search settings buttons on tool bar, static abbrevs
 *
 * Revision 1.22  1999/12/10 03:22:47  sp
 * Bug fixes, old loading code is now used again
 *
 */
