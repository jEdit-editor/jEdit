/*
 * Buffer.java - jEdit buffer
 * Copyright (C) 1998, 1999, 2000 Slava Pestov
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

import gnu.regexp.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.util.Log;

/**
 * An in-memory copy of an open file.<p>
 *
 * This is basically a Swing document with support for loading and
 * saving, and various other miscallaenous things such as markers.<p>
 *
 * Buffers extend Swing document properties to obtain the default values
 * from jEdit's global properties.<p>
 *
 * The following properties are always defined:
 * <ul>
 * <li>tabSize: the tab size
 * <li>lineSeparator: default line separator. This is rarely useful,
 * because all buffers use "\n" as a separator in memory anyway. Only
 * use this property when reading/writing to the disk
 * </ul>
 *
 * Various other properties are also used by jEdit and plugin actions.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class Buffer extends SyntaxDocument implements EBComponent
{
	/**
	 * Line separator property.
	 */
	public static final String LINESEP = "lineSeparator";

	/**
	 * Caret info properties.
	 * @since 2.2pre7
	 */
	public static final String SELECTION_START = "Buffer__selStart";
	public static final String SELECTION_END = "Buffer__selEnd";
	public static final String SELECTION_RECT = "Buffer__rect";
	public static final String SCROLL_VERT = "Buffer__scrollVert";
	public static final String SCROLL_HORIZ = "Buffer__scrollHoriz";
	public static final String OVERWRITE = "Buffer__overwrite";

	/**
	 * Reloads settings from the properties. This should be called
	 * after the <code>syntax</code> buffer-local property is
	 * changed.
	 */
	public void propertiesChanged()
	{
		setFlag(SYNTAX,getBooleanProperty("syntax"));
		if(undo != null)
		{
			try
			{
				undo.setLimit(Integer.parseInt(jEdit.getProperty(
					"buffer.undoCount")));
			}
			catch(NumberFormatException nf)
			{
				undo.setLimit(100);
			}
		}

		// cache it for improved performance
		putProperty("tabSize",getProperty("tabSize"));
	}

	/**
	 * Loads the buffer from disk, even if it is loaded already.
	 * @param view The view
	 * @param reload If true, we automatically delete the autosave file,
	 * otherwise we warn user
	 *
	 * @since 2.5pre1
	 */
	public boolean load(final View view, final boolean reload)
	{
		setFlag(LOADING,true);

		undo = null;
		final boolean loadAutosave;

		if(!getFlag(NEW_FILE))
		{
			if(file != null)
				modTime = file.lastModified();

			// Only on initial load
			if(!reload && autosaveFile != null && autosaveFile.exists())
				loadAutosave = recoverAutosave(view);
			else
			{
				if(autosaveFile != null)
					autosaveFile.delete();
				loadAutosave = false;
			}

			if(!loadAutosave)
			{
				// this returns false if initial sanity
				// checks (if the file is a directory, etc)
				// fail
				if(!vfs.load(view,this,path))
				{
					setFlag(LOADING,false);
					return false;
				}
			}
		}
		else
			loadAutosave = false;

		// Do some stuff once loading is finished
		VFSManager.runInAWTThread(new Runnable()
		{
			public void run()
			{
				undo = new UndoManager();
				setFlag(LOADING,false);

				// if reloading a file, clear dirty flag
				if(reload)
					setDirty(false);

				// if loadAutosave is false, we loaded an
				// autosave file, so we set 'dirty' to true

				// note that we don't use setDirty(),
				// because a) that would send an unnecessary
				// message, b) it would also set the
				// AUTOSAVE_DIRTY flag, which will make
				// the autosave thread write out a
				// redundant autosave file
				if(loadAutosave)
					setFlag(DIRTY,true);

				if(getFlag(TEMPORARY))
					return;

				View _view = jEdit.getFirstView();
				while(_view != null)
				{
					EditPane[] editPanes = _view
						.getEditPanes();
					for(int i = 0; i < editPanes.length; i++)
					{
						EditPane editPane = editPanes[i];
						if(editPane.getBuffer() == Buffer.this)
						{
							editPane.getTextArea().setCaretPosition(0);
							editPane.getTextArea().repaint();
						}
					}
	
					_view = _view.getNext();
				}

				setMode();
				propertiesChanged();
				EditBus.addToBus(Buffer.this);

				// send some EditBus messages
				EditBus.send(new BufferUpdate(Buffer.this,
					BufferUpdate.LOADED));
				EditBus.send(new BufferUpdate(Buffer.this,
					BufferUpdate.MARKERS_CHANGED));
			}
		});

		return true;
	}

	/**
	 * Autosaves this buffer.
	 */
	public void autosave()
	{
		if(autosaveFile == null || !getFlag(AUTOSAVE_DIRTY)
			|| getFlag(LOADING) || getFlag(SAVING))
			return;

		setFlag(AUTOSAVE_DIRTY,false);

		/* try
		{
			File tmpAutosaveFile = new File(MiscUtilities
				.getFileParent(path),'#' + name + "#tmp#");
			_write(new FileOutputStream(tmpAutosaveFile));
			autosaveFile.delete();
			tmpAutosaveFile.renameTo(autosaveFile);
		}
		catch(FileNotFoundException fnf)
		{
			// this could happen if eg, the directory
			// containing this file was renamed.
			// we ignore the error then so the user
			/ isn't flooded with exceptions.
			Log.log(Log.NOTICE,this,fnf);
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,this,e);
		} */
	}

	/**
	 * Prompts the user for a file to save this buffer to.
	 * @param view The view
	 */
	public boolean saveAs(View view)
	{
		String file = VFSManager.getFileVFS().showSaveDialog(view,this);
		if(file != null)
			return save(view,file);
		else
			return false;
	}

	/**
	 * Saves this buffer to the specified path name, or the current path
	 * name if it's null.
	 * @param view The view
	 * @param path The path name to save the buffer to
	 */
	public boolean save(final View view, String path)
	{
		if(path == null && getFlag(NEW_FILE))
			return saveAs(view);

		if(path == null && file != null)
		{
			long newModTime = file.lastModified();

			if(newModTime != modTime)
			{
				Object[] args = { this.path };
				int result = JOptionPane.showConfirmDialog(view,
					jEdit.getProperty("filechanged-save.message",args),
					jEdit.getProperty("filechanged.title"),
					JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE);
				if(result != JOptionPane.YES_OPTION)
					return false;
			}
		}

		setFlag(SAVING,true);
		EditBus.send(new BufferUpdate(this,BufferUpdate.SAVING));

		if(path == null)
			path = this.path;

		final String oldPath = this.path;
		setPath(path);

		if(!vfs.save(view,this,path))
		{
			setFlag(SAVING,false);
			return false;
		}

		// Once save is complete, do a few other things
		VFSManager.runInAWTThread(new Runnable()
		{
			public void run()
			{
				setFlag(AUTOSAVE_DIRTY,false);
				setFlag(READ_ONLY,false);
				setFlag(NEW_FILE,false);
				setFlag(UNTITLED,false);
				setFlag(DIRTY,false);
				setFlag(SAVING,false);

				if(getFlag(NEW_FILE) || mode.getName().equals("text"))
					setMode();

				if(file != null)
					modTime = file.lastModified();

				if(!getPath().equals(oldPath))
					jEdit.updatePosition(Buffer.this);

				// getPath() used since 'path' in containing
				// method isn't final
				if(getPath().toLowerCase().endsWith(".macro"))
					Macros.loadMacros();

				EditBus.send(new BufferUpdate(Buffer.this,
					BufferUpdate.DIRTY_CHANGED));
			}
		});

		return true;
	}

	/**
	 * Returns the last time jEdit modified the file on disk.
	 */
	public long getLastModified()
	{
		return modTime;
	}

	/**
	 * Sets the last time jEdit modified the file on disk.
	 * @param modTime The new modification time
	 */
	public void setLastModified(long modTime)
	{
		this.modTime = modTime;
	}

	/**
	 * Check if the buffer has changed on disk.
	 */
	public void checkModTime(View view)
	{
		// don't do these checks while a save is in progress,
		// because for a moment newModTime will be greater than
		// oldModTime, due to the multithreading
		if(file == null || getFlag(NEW_FILE) || getFlag(SAVING))
			return;

		long oldModTime = modTime;
		long newModTime = file.lastModified();

		if(newModTime != oldModTime)
		{
			modTime = newModTime;

			String prop = (isDirty() ? "filechanged-dirty.message"
				: "filechanged-focus.message");

			Object[] args = { path };
			int result = JOptionPane.showConfirmDialog(view,
				jEdit.getProperty(prop,args),
				jEdit.getProperty("filechanged.title"),
				JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
			if(result == JOptionPane.YES_OPTION)
			{
				load(view,true);
			}
		}
	}

	/**
	 * Returns the virtual filesystem responsible for loading and
	 * saving this buffer.
	 */
	public VFS getVFS()
	{
		return vfs;
	}

	/**
	 * Returns the file this buffer is editing. This may be null if
	 * the file is non-local.
	 */
	public final File getFile()
	{
		return file;
	}

	/**
	 * Returns the autosave file for this buffer. This may be null if
	 * the file is non-local.
	 */
	public final File getAutosaveFile()
	{
		return autosaveFile;
	}

	/**
	 * Returns the name of this buffer.
	 */
	public final String getName()
	{
		return name;
	}

	/**
	 * Returns the path name of this buffer.
	 */
	public final String getPath()
	{
		return path;
	}

	/**
	 * Returns true if this buffer has been closed with
	 * <code>jEdit.closeBuffer()</code>.
	 */
	public final boolean isClosed()
	{
		return getFlag(CLOSED);
	}

	/**
	 * Returns true if the buffer is loaded.
	 */
	public final boolean isLoaded()
	{
		return !getFlag(LOADING);
	}

	/**
	 * Returns true if the buffer is currently being saved.
	 */
	public final boolean isSaving()
	{
		return getFlag(SAVING);
	}

	/**
	 * Returns true if this file doesn't exist on disk.
	 */
	public final boolean isNewFile()
	{
		return getFlag(NEW_FILE);
	}

	/**
	 * Sets the new file flag.
	 * @param newFile The new file flag
	 */
	public final void setNewFile(boolean newFile)
	{
		setFlag(NEW_FILE,newFile);
	}

	/**
	 * Returns true if this file is 'untitled'.
	 */
	public final boolean isUntitled()
	{
		return getFlag(UNTITLED);
	}

	/**
	 * Returns true if this file has changed since last save, false
	 * otherwise.
	 */
	public final boolean isDirty()
	{
		return getFlag(DIRTY);
	}
	
	/**
	 * Returns true if this file is read only, false otherwise.
	 */
	public final boolean isReadOnly()
	{
		return getFlag(READ_ONLY);
	}

	/**
	 * Sets the read only flag.
	 * @param readOnly The read only flag
	 */
	public final void setReadOnly(boolean readOnly)
	{
		setFlag(READ_ONLY,readOnly);
	}

	/**
	 * Sets the `dirty' (changed since last save) flag of this buffer.
	 */
	public void setDirty(boolean d)
	{
		boolean old_d = getFlag(DIRTY);

		if(d)
		{
			if(getFlag(LOADING) || getFlag(READ_ONLY))
				return;
			if(getFlag(DIRTY) && getFlag(AUTOSAVE_DIRTY))
				return;
			setFlag(DIRTY,true);
			setFlag(AUTOSAVE_DIRTY,true);
		}
		else
			setFlag(DIRTY,false);

		if(d != old_d)
			EditBus.send(new BufferUpdate(this,BufferUpdate.DIRTY_CHANGED));
	}

	/**
	 * Returns if this is a temporary buffer.
	 * @see jEdit#openTemporary(View,String,String,boolean,boolean)
	 * @see jEdit#commitTemporary(Buffer)
	 * @since jEdit 2.2pre7
	 */
	public boolean isTemporary()
	{
		return getFlag(TEMPORARY);
	}

	/**
	 * Returns this buffer's icon.
	 * @since jEdit 3.0pre1
	 */
	public ImageIcon getIcon()
	{
		if(getFlag(NEW_FILE))
		{
			if(getFlag(DIRTY))
				return GUIUtilities.NEW_DIRTY_BUFFER_ICON;
			else
				return GUIUtilities.NEW_BUFFER_ICON;
		}
		else if(getFlag(DIRTY))
			return GUIUtilities.DIRTY_BUFFER_ICON;
		else
			return GUIUtilities.NORMAL_BUFFER_ICON;
	}

	/**
	 * Undoes the most recent edit. Returns true if the undo was
	 * successful.
	 *
	 * @since jEdit 2.2pre1
	 */
	public boolean undo()
	{
		if(undo == null)
			return false;

		try
		{
			setFlag(UNDO_IN_PROGRESS,true);
			undo.undo();
			return true;
		}
		catch(CannotUndoException cu)
		{
			Log.log(Log.DEBUG,this,cu);
			return false;
		}
		finally
		{
			setFlag(UNDO_IN_PROGRESS,false);
		}
	}

	/**
	 * Redoes the most recently undone edit. Returns true if the redo was
	 * successful.
	 *
	 * @since jEdit 2.2pre1
	 */
	public boolean redo()
	{
		if(undo == null)
			return false;

		try
		{
			setFlag(UNDO_IN_PROGRESS,true);
			undo.redo();
			return true;
		}
		catch(CannotRedoException cr)
		{
			Log.log(Log.DEBUG,this,cr);
			return false;
		}
		finally
		{
			setFlag(UNDO_IN_PROGRESS,false);
		}
	}

	/**
	 * Adds an undoable edit to this document. This is non-trivial
	 * mainly because the text area adds undoable edits every time
	 * the caret is moved. First of all, undos are ignored while
	 * an undo is already in progress. This is no problem with Swing
	 * Document undos, but caret undos are fired all the time and
	 * this needs to be done. Also, insignificant undos are ignored
	 * if the redo queue is non-empty to stop something like a caret
	 * move from flushing all redos.
	 * @param edit The undoable edit
	 *
	 * @since jEdit 2.2pre1
	 */
	public void addUndoableEdit(UndoableEdit edit)
	{
		if(undo == null || getFlag(UNDO_IN_PROGRESS) || getFlag(LOADING))
			return;

		// Ignore insificant edits if the redo queue is non-empty.
		// This stops caret movement from killing redos.
		if(undo.canRedo() && !edit.isSignificant())
			return;

		if(compoundEdit != null)
			compoundEdit.addEdit(edit);
		else
			undo.addEdit(edit);
	}

	/**
	 * Starts a compound edit. All edits from now on until
	 * <code>endCompoundEdit()</code> are called will be merged
	 * into one. This can be used to make a complex operation
	 * undoable in one step. Nested calls to
	 * <code>beginCompoundEdit()</code> behave as expected,
	 * requiring the same number of <code>endCompoundEdit()</code>
	 * calls to end the edit.
	 * @see #endCompoundEdit()
	 * @see #undo()
	 */
	public void beginCompoundEdit()
	{
		if(getFlag(TEMPORARY))
			return;

		compoundEditCount++;
		if(compoundEdit == null)
			compoundEdit = new CompoundEdit();
	}

	/**
	 * Ends a compound edit. All edits performed since
	 * <code>beginCompoundEdit()</code> was called can now
	 * be undone in one step by calling <code>undo()</code>.
	 * @see #beginCompoundEdit()
	 * @see #undo()
	 */
	public void endCompoundEdit()
	{
		if(getFlag(TEMPORARY))
			return;

		if(compoundEditCount == 0)
			return;
		compoundEditCount--;
		if(compoundEdit != null)
		{
			compoundEdit.end();
			if(compoundEdit.canUndo())
				undo.addEdit(compoundEdit);
			compoundEdit = null;
		}
	}

	/**
	 * Returns the tab size used in this buffer. This is equivalent
	 * to calling getProperty("tabSize").
	 */
	public int getTabSize()
	{
		return ((Integer)getProperty("tabSize")).intValue();
	}

	/**
	 * Returns the value of a boolean property.
	 * @param name The property name
	 */
	public boolean getBooleanProperty(String name)
	{
		Object obj = getProperty(name);
		if(obj instanceof Boolean)
			return ((Boolean)obj).booleanValue();
		else if("true".equals(obj) || "on".equals(obj) || "yes".equals(obj))
			return true;
		else
			return false;
	}

	/**
	 * Sets a boolean property.
	 * @param name The property name
	 * @param value The value
	 */
	public void putBooleanProperty(String name, boolean value)
	{
		putProperty(name,value ? Boolean.TRUE : Boolean.FALSE);
	}

	/**
	 * Returns this buffer's edit mode.
	 */
	public final Mode getMode()
	{
		return mode;
	}
	
	/**
	 * Sets this buffer's edit mode.
	 * @param mode The mode
	 */
	public void setMode(Mode mode)
	{
		/* This protects against stupid people (like me)
		 * doing stuff like buffer.setMode(jEdit.getMode(...)); */
		if(mode == null)
			throw new NullPointerException("Mode must be non-null");

		if(this.mode == mode)
		{
			// this helps out with reloading and so on
			tokenizeLines();
			return;
		}

		Mode oldMode = this.mode;

		this.mode = mode;

		setTokenMarker(mode.createTokenMarker());

		// don't fire it for initial mode set
		if(oldMode != null)
			EditBus.send(new BufferUpdate(this,BufferUpdate.MODE_CHANGED));
	}

	/**
	 * Sets this buffer's edit mode by calling the accept() method
	 * of each registered edit mode.
	 */
	public void setMode()
	{
		String userMode = (String)getProperty("mode");
		if(userMode != null)
		{
			Mode m = jEdit.getMode(userMode);
			if(m != null)
			{
				setMode(m);
				return;
			}
		}

		String nogzName = name.substring(0,name.length() -
			(name.endsWith(".gz") ? 3 : 0));
		Element lineElement = getDefaultRootElement().getElement(0);
		try
		{
			String line = getText(0,(lineElement == null
				? 0 : lineElement.getEndOffset()-1));

			Mode[] modes = jEdit.getModes();

			for(int i = 0; i < modes.length; i++)
			{
				if(modes[i].accept(this,nogzName,line))
				{
					setMode(modes[i]);
					return;
				}
			}
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}

		// if we are being run on startup, we must ensure that
		// a valid mode exists after we're done!
		if(mode == null)
		{
			Mode defaultMode = jEdit.getMode(jEdit.getProperty("buffer.defaultMode"));
			if(defaultMode == null)
				defaultMode = jEdit.getMode("text");
			setMode(defaultMode);
		}
	}

	/**
	 * If auto indent is enabled, this method is called when the `Tab'
	 * or `Enter' key is pressed to perform mode-specific indentation
	 * and return true, or return false if a normal tab is to be inserted.
	 * @param textArea The text area
	 * @param line The line number to indent
	 * @param force If true, the line will be indented even if it already
	 * has the right amount of indent
	 * @return true if the tab key event should be swallowed (ignored)
	 * false if a real tab should be inserted
	 */
	public boolean indentLine(JEditTextArea textArea, int lineIndex, boolean force)
	{
		if(lineIndex == 0)
			return false;

		// Get properties
		String openBrackets = (String)getProperty("indentOpenBrackets");
		String closeBrackets = (String)getProperty("indentCloseBrackets");
		String _indentPrevLine = (String)getProperty("indentPrevLine");
		boolean doubleBracketIndent = getBooleanProperty("doubleBracketIndent");
		RE indentPrevLineRE = null;
		if(openBrackets == null)
			openBrackets = "";
		if(closeBrackets == null)
			closeBrackets = "";
		if(_indentPrevLine != null)
		{
			try
			{
				indentPrevLineRE = new RE(_indentPrevLine,
					RE.REG_ICASE,RESyntax.RE_SYNTAX_PERL5);
			}
			catch(REException re)
			{
				Log.log(Log.ERROR,this,"Invalid 'indentPrevLine'"
					+ " regexp: " + _indentPrevLine);
				Log.log(Log.ERROR,this,re);
			}
		}

		int tabSize = getTabSize();
		boolean noTabs = getBooleanProperty("noTabs");

		// Get line text
		String line = textArea.getLineText(lineIndex);
		int start = textArea.getLineStartOffset(lineIndex);
		String prevLine = null;
		for(int i = lineIndex - 1; i >= 0; i--)
		{
			if(textArea.getLineLength(i) != 0)
			{
				prevLine = textArea.getLineText(i);
				break;
			}
		}

		if(prevLine == null)
			return false;

		/*
		 * If 'prevLineIndent' matches a line --> +1
		 */
		boolean prevLineMatches = (indentPrevLineRE == null ? false
			: indentPrevLineRE.isMatch(prevLine));

		/*
		 * On the previous line,
		 * if(bob) { --> +1
		 * if(bob) { } --> 0
		 * } else if(bob) { --> +1
		 */
		boolean prevLineStart = true; // False after initial indent
		int prevLineIndent = 0; // Indent width (tab expanded)
		int prevLineBrackets = 0; // Additional bracket indent
		for(int i = 0; i < prevLine.length(); i++)
		{
			char c = prevLine.charAt(i);
			switch(c)
			{
			case ' ':
				if(prevLineStart)
					prevLineIndent++;
				break;
			case '\t':
				if(prevLineStart)
				{
					prevLineIndent += (tabSize
						- (prevLineIndent
						% tabSize));
				}
				break;
			default:
				prevLineStart = false;
				if(closeBrackets.indexOf(c) != -1)
					prevLineBrackets = Math.max(
						prevLineBrackets-1,0);
				else if(openBrackets.indexOf(c) != -1)
				{
					/*
					 * If supressBracketAfterIndent is true
					 * and we have something that looks like:
					 * if(bob)
					 * {
					 * then the 'if' will not shift the indent,
					 * because of the {.
					 *
					 * If supressBracketAfterIndent is false,
					 * the above would be indented like:
					 * if(bob)
					 *         {
					 */
					if(!doubleBracketIndent)
						prevLineMatches = false;
					prevLineBrackets++;
				}
				break;
			}
		}

		/*
		 * On the current line,
		 * } --> -1
		 * } else if(bob) { --> -1
		 * if(bob) { } --> 0
		 */
		boolean lineStart = true; // False after initial indent
		int lineIndent = 0; // Indent width (tab expanded)
		int lineWidth = 0; // White space count
		int lineBrackets = 0; // Additional bracket indent
		int closeBracketIndex = -1; // For lining up closing
			// and opening brackets
		for(int i = 0; i < line.length(); i++)
		{
			char c = line.charAt(i);
			switch(c)
			{
			case ' ':
				if(lineStart)
				{
					lineIndent++;
					lineWidth++;
				}
				break;
			case '\t':
				if(lineStart)
				{
					lineIndent += (tabSize
						- (lineIndent
						% tabSize));
					lineWidth++;
				}
				break;
			default:
				lineStart = false;
				if(closeBrackets.indexOf(c) != -1)
				{
					if(lineBrackets == 0)
						closeBracketIndex = i;
					else
						lineBrackets--;
				}
				else if(openBrackets.indexOf(c) != -1)
				{
					if(!doubleBracketIndent)
						prevLineMatches = false;
					lineBrackets++;
				}

				break;
			}
		}

		try
		{
			if(closeBracketIndex != -1)
			{
				int offset = TextUtilities.findMatchingBracket(
					this,lineIndex,closeBracketIndex);
				if(offset != -1)
				{
					String closeLine = textArea.getLineText(
						textArea.getLineOfOffset(offset));
					prevLineIndent = MiscUtilities
						.getLeadingWhiteSpaceWidth(
						closeLine,tabSize);
				}
				else
					return false;
			}
			else
			{
				prevLineIndent += (prevLineBrackets * tabSize);
			}

			if(prevLineMatches)
				prevLineIndent += tabSize;

			// Insert a tab if line already has correct indent
			// and force is not set
			if(!force && lineIndent >= prevLineIndent)
				return false;

			// Do it
			remove(start,lineWidth);
			insertString(start,MiscUtilities.createWhiteSpace(
				prevLineIndent,(noTabs ? 0 : tabSize)),null);
			return true;
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}

		return false;
	}

	/**
	 * Returns the token marker for this buffer.
	 */
	public final TokenMarker getTokenMarker()
	{
		if(getFlag(SYNTAX))
			return super.getTokenMarker();
		else
			return NullTokenMarker.getSharedInstance();
	}

	/**
	 * Doesn't do anything if tokenization is disabled.
	 */
	public void tokenizeLines()
	{
		if(!jEdit.getBooleanProperty("buffer.tokenize"))
			return;

		super.tokenizeLines();
	}

	/**
	 * Returns a vector of markers.
	 * @since jEdit 2.5pre4
	 */
	public final Vector getMarkers()
	{
		return markers;
	}

	/**
	 * Returns the number of markers in this buffer.
	 * @since jEdit 2.5pre1
	 */
	public final int getMarkerCount()
	{
		return markers.size();
	}

	/**
	 * Adds a marker to this buffer.
	 * @param name The name of the marker
	 * @param start The start offset of the marker
	 * @param end The end offset of this marker
	 */
	public void addMarker(String name, int start, int end)
	{
		setDirty(true);

		name = name.replace(';',' ');
		Marker markerN;
		try
		{
			markerN = new Marker(name,createPosition(start),
				createPosition(end));
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
			return;
		}
		boolean added = false;

		// don't sort markers while buffer is being loaded
		if(!getFlag(LOADING))
		{
			for(int i = 0; i < markers.size(); i++)
			{
				Marker marker = (Marker)markers.elementAt(i);
				if(marker.getName().equals(name))
				{
					markers.removeElementAt(i);
				}
				if(marker.getStart() > start)
				{
					markers.insertElementAt(markerN,i);
					added = true;
					break;
				}
			}
		}

		if(!added)
			markers.addElement(markerN);

		if(!getFlag(LOADING))
			EditBus.send(new BufferUpdate(this,BufferUpdate.MARKERS_CHANGED));
	}

	/**
	 * Removes the marker with the specified name.
	 * @param name The name of the marker to remove
	 */
	public void removeMarker(String name)
	{
		setDirty(true);

		for(int i = 0; i < markers.size(); i++)
		{
			Marker marker = (Marker)markers.elementAt(i);
			if(marker.getName().equals(name))
				markers.removeElementAt(i);
		}

		if(!getFlag(LOADING))
			EditBus.send(new BufferUpdate(this,BufferUpdate.MARKERS_CHANGED));
	}

	/**
	 * Removes all defined markers.
	 * @since jEdit 3.0pre1
	 */
	public void removeAllMarkers()
	{
		setDirty(true);

		markers.removeAllElements();

		if(!getFlag(LOADING))
			EditBus.send(new BufferUpdate(this,BufferUpdate.MARKERS_CHANGED));
	}

	/**
	 * Returns the marker with the specified name.
	 * @param name The marker name
	 */
	public Marker getMarker(String name)
	{
		Enumeration enum = markers.elements();
		while(enum.hasMoreElements())
		{
			Marker marker = (Marker)enum.nextElement();
			if(marker.getName().equals(name))
				return marker;
		}
		return null;
	}

	/**
	 * Returns the next buffer in the list.
	 */
	public final Buffer getNext()
	{
		return next;
	}

	/**
	 * Returns the previous buffer in the list.
	 */
	public final Buffer getPrev()
	{
		return prev;
	}

	/**
	 * Returns the position of this buffer in the buffer list.
	 */
	public final int getIndex()
	{
		int count = 0;
		Buffer buffer = prev;
		for(;;)
		{
			if(buffer == null)
				break;
			count++;
			buffer = buffer.prev;
		}
		return count;
	}

	/**
	 * Returns a string representation of this buffer.
	 * This simply returns the path name.
	 */
	public String toString()
	{
		return path;
	}

	public void handleMessage(EBMessage msg)
	{
		if(msg instanceof PropertiesChanged)
			propertiesChanged();
	}

	// package-private members
	Buffer prev;
	Buffer next;

	Buffer(View view, String path, boolean readOnly,
		boolean newFile, boolean temp, Hashtable props)
	{
		setFlag(TEMPORARY,temp);
		setFlag(READ_ONLY,readOnly);

		markers = new Vector();

		addDocumentListener(new DocumentHandler());
		addUndoableEditListener(new UndoHandler());

		setDocumentProperties(new BufferProps());
		putProperty("i18n",Boolean.FALSE);

		setPath(path);

		/* Magic: UNTITLED is only set of newFile param to
		 * constructor is set, NEW_FILE is also set if file
		 * doesn't exist on disk.
		 *
		 * This is so that we can tell apart files created
		 * with jEdit.newFile(), and those that just don't
		 * exist on disk.
		 */
		setFlag(UNTITLED,newFile);

		if(file != null)
			newFile |= !file.exists();

		setFlag(NEW_FILE,newFile);

		if(props != null)
		{
			Enumeration keys = props.keys();
			Enumeration values = props.elements();
			while(keys.hasMoreElements())
			{
				putProperty(keys.nextElement(),values.nextElement());
			}
		}
	}

	void commitTemporary()
	{
		setFlag(TEMPORARY,false);
		EditBus.addToBus(this);
	}

	void close()
	{
		setFlag(CLOSED,true);

		if(autosaveFile != null)
			autosaveFile.delete();

		EditBus.removeFromBus(this);
	}

	// private members
	private void setFlag(int flag, boolean value)
	{
		if(value)
			flags |= (1 << flag);
		else
			flags &= ~(1 << flag);
	}

	private boolean getFlag(int flag)
	{
		int mask = (1 << flag);
		return (flags & mask) == mask;
	}

	private static final int CLOSED = 0;
	private static final int LOADING = 1;
	private static final int SAVING = 2;
	private static final int NEW_FILE = 3;
	private static final int UNTITLED = 4;
	private static final int AUTOSAVE_DIRTY = 5;
	private static final int DIRTY = 6;
	private static final int READ_ONLY = 7;
	private static final int SYNTAX = 8;
	private static final int UNDO_IN_PROGRESS = 9;
	private static final int TEMPORARY = 10;

	private int flags;

	private long modTime;
	private File file;
	private VFS vfs;
	private File autosaveFile;
	private String path;
	private String protocol;
	private String name;
	private Mode mode;
	private UndoManager undo;
	private CompoundEdit compoundEdit;
	private int compoundEditCount;
	private Vector markers;
	private int savedSelStart;
	private int savedSelEnd;

	private void setPath(String path)
	{
		protocol = MiscUtilities.getFileProtocol(path);
		if(protocol == null)
			protocol = "file";

		if(path.startsWith("file:"))
			path = path.substring(5);

		this.path = path;
		name = MiscUtilities.getFileName(path);

		vfs = VFSManager.getVFSForProtocol(protocol);
		if(vfs instanceof FileVFS)
		{
			file = new File(path);

			// if we don't do this, the autosave of a file won't be
			// deleted after a save as
			if(autosaveFile != null)
				autosaveFile.delete();
			autosaveFile = new File(file.getParent(),'#' + name + '#');
		}
	}

	private boolean recoverAutosave(final View view)
	{
		// this method might get called at startup
		GUIUtilities.hideSplashScreen();

		final Object[] args = { autosaveFile.getPath() };
		int result = JOptionPane.showConfirmDialog(view,
			jEdit.getProperty("autosave-found.message",args),
			jEdit.getProperty("autosave-found.title"),
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE);

		if(result == JOptionPane.YES_OPTION)
		{
			vfs.load(view,this,autosaveFile.getPath());

			// show this message when all I/O requests are
			// complete
			VFSManager.runInAWTThread(new Runnable()
			{
				public void run()
				{
					GUIUtilities.message(view,"autosave-loaded",args);
				}
			});

			return true;
		}
		else
			return false;
	}

	// A dictionary that looks in the mode and editor properties
	// for default values
	class BufferProps extends Hashtable
	{
		public Object get(Object key)
		{
			// First try the buffer-local properties
			Object o = super.get(key);
			if(o != null)
				return o;

			// JDK 1.3 likes to use non-string objects
			// as keys
			if(!(key instanceof String))
				return null;

			// Now try mode.<mode>.<property>
			if(mode != null)
				return mode.getProperty((String)key);
			else
			{
				// Now try buffer.<property>
				String value = jEdit.getProperty("buffer." + key);
				if(value == null)
					return null;
	
				// Try returning it as an integer first
				try
				{
					return new Integer(value);
				}
				catch(NumberFormatException nf)
				{
					return value;
				}
			}
		}
	}

	// event handlers
	class UndoHandler
	implements UndoableEditListener
	{
		public void undoableEditHappened(UndoableEditEvent evt)
		{
			addUndoableEdit(evt.getEdit());
		}
	}

	class DocumentHandler
	implements DocumentListener
	{
		public void insertUpdate(DocumentEvent evt)
		{
			setDirty(true);
		}

		public void removeUpdate(DocumentEvent evt)
		{
			setDirty(true);
		}

		public void changedUpdate(DocumentEvent evt)
		{
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.160  2000/07/21 10:23:49  sp
 * Multiple work threads
 *
 * Revision 1.159  2000/07/19 11:45:18  sp
 * I/O requests can be aborted now
 *
 * Revision 1.158  2000/07/14 06:00:44  sp
 * bracket matching now takes syntax info into account
 *
 * Revision 1.157  2000/07/03 03:32:15  sp
 * *** empty log message ***
 *
 * Revision 1.156  2000/06/12 02:43:29  sp
 * pre6 almost ready
 *
 * Revision 1.155  2000/06/06 04:38:08  sp
 * WorkThread's AWT request stuff reworked
 *
 * Revision 1.154  2000/06/05 08:22:25  sp
 * bug fixes
 *
 * Revision 1.153  2000/06/04 08:57:35  sp
 * GUI updates, bug fixes
 *
 * Revision 1.152  2000/05/24 07:56:04  sp
 * bug fixes
 *
 * Revision 1.151  2000/05/22 12:05:45  sp
 * Markers are highlighted in the gutter, bug fixes
 *
 * Revision 1.150  2000/05/13 05:13:31  sp
 * Mode option pane
 *
 */
