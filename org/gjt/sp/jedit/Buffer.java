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
import org.gjt.sp.jedit.browser.VFSBrowser;
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
public class Buffer extends PlainDocument implements EBComponent
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
		if(getFlag(SYNTAX))
			setTokenMarker(mode.createTokenMarker());
		else
			setTokenMarker(new NullTokenMarker());

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

		// cache these for improved performance
		putProperty("tabSize",getProperty("tabSize"));
		putProperty("maxLineLen",getProperty("maxLineLen"));
	}

	/**
	 * Loads the buffer from disk, even if it is loaded already.
	 * @param view The view
	 * @param reload If true, user will not be asked to recover autosave
	 * file, if any
	 *
	 * @since 2.5pre1
	 */
	public boolean load(final View view, final boolean reload)
	{
		if(isPerformingIO())
		{
			GUIUtilities.error(view,"buffer-multiple-io",null);
			return false;
		}

		setFlag(LOADING,true);

		// view text areas temporarily blank out while a buffer is
		// being loaded, to indicate to the user that there is no
		// data available yet.
		EditBus.send(new BufferUpdate(this,BufferUpdate.LOAD_STARTED));

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
		Runnable runnable = new Runnable()
		{
			public void run()
			{
				StringBuffer sbuf = (StringBuffer)getProperty(
					BufferIORequest.LOAD_DATA);
				clearProperties();
				if(sbuf != null)
				{
					try
					{
						// For `reload' command
						remove(0,getLength());
						insertString(0,sbuf.toString(),null);
					}
					catch(BadLocationException bl)
					{
						bl.printStackTrace();
					}
				}

				undo = new MyUndoManager();
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

				setMode();

				// send some EditBus messages
				EditBus.send(new BufferUpdate(Buffer.this,
					BufferUpdate.LOADED));
				EditBus.send(new BufferUpdate(Buffer.this,
					BufferUpdate.MARKERS_CHANGED));
			}
		};

		if(getFlag(TEMPORARY))
			runnable.run();
		else
			VFSManager.runInAWTThread(runnable);

		return true;
	}

	/**
	 * Loads a file from disk, and inserts it into this buffer.
	 * @param view The view
	 *
	 * @since 2.7pre1
	 */
	public boolean insert(final View view, String path)
	{
		if(isPerformingIO())
		{
			GUIUtilities.error(view,"buffer-multiple-io",null);
			return false;
		}

		if(!MiscUtilities.isURL(path))
			path = MiscUtilities.constructPath(this.path,path);

		Buffer buffer = jEdit.getBuffer(path);
		if(buffer != null)
		{
			try
			{
				view.getTextArea().setSelectedText(
					buffer.getText(0,buffer.getLength()));
			}
			catch(BadLocationException bl)
			{
				bl.printStackTrace();
			}
			return true;
		}

		VFS vfs = VFSManager.getVFSForPath(path);

		setFlag(IO,true);

		// this returns false if initial sanity
		// checks (if the file is a directory, etc)
		// fail
		if(!vfs.insert(view,this,path))
		{
			setFlag(IO,false);
			return false;
		}

		// Do some stuff once loading is finished
		VFSManager.runInAWTThread(new Runnable()
		{
			public void run()
			{
				setFlag(IO,false);

				StringBuffer sbuf = (StringBuffer)getProperty(
					BufferIORequest.LOAD_DATA);
				if(sbuf != null)
				{
					getDocumentProperties().remove(
						BufferIORequest.LOAD_DATA);

					view.getTextArea().setSelectedText(sbuf.toString());
				}
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
			|| getFlag(LOADING) || getFlag(IO))
			return;

		setFlag(AUTOSAVE_DIRTY,false);

		VFSManager.runInWorkThread(new BufferIORequest(
			BufferIORequest.AUTOSAVE,null,this,new VFSSession(),
			VFSManager.getFileVFS(),autosaveFile.getPath()));
	}

	/**
	 * Prompts the user for a file to save this buffer to.
	 * @param view The view
	 * @param rename True if the buffer's path should be changed, false
	 * if only a copy should be saved to the specified filename
	 * @since jEdit 2.6pre5
	 */
	public boolean saveAs(View view, boolean rename)
	{
		String[] files = GUIUtilities.showVFSFileDialog(view,path,
			VFSBrowser.SAVE_DIALOG,false);

		// files[] should have length 1, since the dialog type is
		// SAVE_DIALOG
		if(files == null)
			return false;

		return save(view,files[0],rename);
	}

	/**
	 * Saves this buffer to the specified path name, or the current path
	 * name if it's null.
	 * @param view The view
	 * @param path The path name to save the buffer to, or null to use
	 * the existing path
	 */
	public boolean save(View view, String path)
	{
		return save(view,path,true);
	}

	/**
	 * Saves this buffer to the specified path name, or the current path
	 * name if it's null.
	 * @param view The view
	 * @param path The path name to save the buffer to, or null to use
	 * the existing path
	 * @param rename True if the buffer's path should be changed, false
	 * if only a copy should be saved to the specified filename
	 * @since jEdit 2.6pre5
	 */
	public boolean save(final View view, String path, final boolean rename)
	{
		if(isPerformingIO())
		{
			GUIUtilities.error(view,"buffer-multiple-io",null);
			return false;
		}

		if(path == null && getFlag(NEW_FILE))
			return saveAs(view,rename);

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

		setFlag(IO,true);
		EditBus.send(new BufferUpdate(this,BufferUpdate.SAVING));

		if(path == null)
			path = this.path;

		// can't call setPath() here because we don't want a failed
		// 'save as' to change the buffer's path, so obtain the VFS
		// instance 'manually'
		VFS vfs = VFSManager.getVFSForPath(path);

		if(!vfs.save(view,this,path))
		{
			setFlag(IO,false);
			return false;
		}

		final String oldPath = this.path;
		if(rename)
			setPath(path);

		// Once save is complete, do a few other things
		VFSManager.runInAWTThread(new Runnable()
		{
			public void run()
			{
				if(rename)
				{
					if(autosaveFile != null)
						autosaveFile.delete();

					saveUndo = undo.editToBeUndone();

					setFlag(AUTOSAVE_DIRTY,false);
					setFlag(READ_ONLY,false);
					setFlag(NEW_FILE,false);
					setFlag(UNTITLED,false);
					setFlag(DIRTY,false);

					saveUndo = undo.editToBeUndone();

					if(!getPath().equals(oldPath))
						setMode();

					if(file != null)
						modTime = file.lastModified();

					if(!getPath().equals(oldPath))
					{
						jEdit.updatePosition(Buffer.this);
						setMode();
					}

					if(file != null)
						modTime = file.lastModified();
				}

				setFlag(IO,false);

				if(!getPath().equals(oldPath))
					VFSManager.sendVFSUpdate(getVFS(),getPath(),true);

				if(rename)
				{
					EditBus.send(new BufferUpdate(Buffer.this,
						BufferUpdate.DIRTY_CHANGED));
				}
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
		if(file == null || getFlag(NEW_FILE) || getFlag(IO))
			return;

		long oldModTime = modTime;
		long newModTime = file.lastModified();

		if(newModTime != oldModTime)
		{
			modTime = newModTime;

			if(!file.exists())
			{
				Object[] args = { path };
				GUIUtilities.message(view,"filedeleted",args);
				return;
			}

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
	 * Returns true if the buffer is currently performing I/O.
	 * @since jEdit 2.7pre1
	 */
	public final boolean isPerformingIO()
	{
		return getFlag(LOADING) || getFlag(IO);
	}

	/**
	 * @deprecated Call isPerformingIO() instead
	 */
	public final boolean isSaving()
	{
		return getFlag(IO);
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
	 * Returns true if this file is editable, false otherwise.
	 * @since jEdit 2.7pre1
	 */
	public final boolean isEditable()
	{
		return !(getFlag(READ_ONLY) || getFlag(IO) || getFlag(LOADING));
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
		{
			// remember the undo associated with a clean buffer,
			// so that if we ever return to that undo we can
			// clear the dirty flag
			saveUndo = undo.editToBeUndone();
			setFlag(DIRTY,false);
		}

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
	 * @since jEdit 2.6pre6
	 */
	public Icon getIcon()
	{
		if(getFlag(DIRTY))
			return GUIUtilities.DIRTY_BUFFER_ICON;
		else if(getFlag(READ_ONLY))
			return GUIUtilities.READ_ONLY_BUFFER_ICON;
		else if(getFlag(NEW_FILE))
			return GUIUtilities.NEW_BUFFER_ICON;
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

		UndoableEdit toUndo = undo.editToBeUndone();
		if(toUndo == saveUndo)
			setDirty(false);

		return true;
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

		UndoableEdit toUndo = undo.editToBeUndone();
		if(toUndo == saveUndo)
			setDirty(false);

		return true;
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
		{
			compoundEditNonEmpty = true;
			compoundEdit.addEdit(edit);
		}
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
		{
			compoundEditNonEmpty = false;
			compoundEdit = new CompoundEdit();
		}
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
		if(compoundEditCount == 0)
		{
			compoundEdit.end();
			if(compoundEditNonEmpty && compoundEdit.canUndo())
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
	 * Returns the indent size used in this buffer. This is equivalent
	 * to calling getProperty("indentSize").
	 * @since jEdit 2.7pre1
	 */
	public int getIndentSize()
	{
		return ((Integer)getProperty("indentSize")).intValue();
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
			return;

		Mode oldMode = this.mode;

		this.mode = mode;

		clearProperties();
		parseBufferLocalProperties();

		propertiesChanged(); // sets up token marker

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
	 * @param canIncreaseIndent If false, nothing will be done if the
	 * calculated indent is greater than the current
	 * @param canDecreaseIndent If false, nothing will be done if the
	 * calculated indent is less than the current
	 * @return true if the tab key event should be swallowed (ignored)
	 * false if a real tab should be inserted
	 */
	// XXX: having to pass a textArea around sucks! Should update this
	// method to use Elements one day
	public boolean indentLine(JEditTextArea textArea, int lineIndex,
		boolean canIncreaseIndent, boolean canDecreaseIndent)
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
		int indentSize = getIndentSize();
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
				prevLineIndent += (prevLineBrackets * indentSize);
			}

			if(prevLineMatches)
				prevLineIndent += indentSize;

			if(!canDecreaseIndent && prevLineIndent <= lineIndent)
				return false;

			if(!canIncreaseIndent && prevLineIndent >= lineIndent)
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
		return tokenMarker;
	}

	/**
	 * Sets the token marker that is to be used to split lines of
	 * this document up into tokens.
	 * @param tm The new token marker
	 */
	public void setTokenMarker(TokenMarker tm)
	{
		if(tm == null)
			throw new NullPointerException("token marker cannot be null");

		tokenMarker = tm;
		tokenMarker.insertLines(0,getDefaultRootElement().getElementCount());
	}

	/**
	 * @deprecated Don't call this method.
	 */
	public void tokenizeLines() {}

	/**
	 * Reparses the document, by passing the specified lines to the
	 * token marker. This should be called after a large quantity of
	 * text is first inserted.
	 * @param start The first line to parse
	 * @param len The number of lines, after the first one to parse
	 */
	public void tokenizeLines(int start, int len)
	{
		if(tokenMarker instanceof NullTokenMarker)
			return;

		tokenMarker.linesChanged(start,len);

		Segment lineSegment = new Segment();
		Element map = getDefaultRootElement();

		len += start;

		for(int i = start; i < len; i++)
			tokenMarker.markTokens(this,i);
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
		Marker markerN = new Marker(this,name,start,end);
		boolean added = false;

		// don't sort markers while buffer is being loaded
		if(!getFlag(LOADING))
		{
			markerN.createPositions();

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
	 * @since jEdit 2.6pre1
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
		return name + " (" + vfs.getParentOfPath(path) + ")";
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
		setDocumentProperties(new BufferProps());
		clearProperties();

		setFlag(TEMPORARY,temp);
		setFlag(READ_ONLY,readOnly);

		markers = new Vector();

		addDocumentListener(new DocumentHandler());
		addUndoableEditListener(new UndoHandler());

		if(props != null)
		{
			Enumeration keys = props.keys();
			Enumeration values = props.elements();
			while(keys.hasMoreElements())
			{
				putProperty(keys.nextElement(),values.nextElement());
			}
		}

		setMode(jEdit.getMode("text"));
		setPath(path);

		/* Magic: UNTITLED is only set if newFile param to
		 * constructor is set, NEW_FILE is also set if file
		 * doesn't exist on disk.
		 *
		 * This is so that we can tell apart files created
		 * with jEdit.newFile(), and those that just don't
		 * exist on disk.
		 *
		 * Why do we need to tell the difference between the
		 * two? jEdit.addBufferToList() checks if the only
		 * opened buffer is an untitled buffer, and if so,
		 * replaces it with the buffer to add. We don't want
		 * this behavior to occur with files that don't
		 * exist on disk; only untitled ones.
		 */
		setFlag(UNTITLED,newFile);

		if(file != null)
			newFile |= !file.exists();

		if(!temp)
			EditBus.addToBus(Buffer.this);

		setFlag(NEW_FILE,newFile);
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

	// protected members
	
	/**
	 * We overwrite this method to update the token marker
	 * state immediately so that any event listeners get a
	 * consistent token marker.
	 */
	protected void fireInsertUpdate(DocumentEvent evt)
	{
		DocumentEvent.ElementChange ch = evt.getChange(
			getDefaultRootElement());
		if(ch != null)
		{
			int index = ch.getIndex();
			int len = ch.getChildrenAdded().length -
				ch.getChildrenRemoved().length;
			tokenMarker.linesChanged(index,
				tokenMarker.getLineCount() - index);
			tokenMarker.insertLines(ch.getIndex() + 1,len);
			index += (len + 1);
		}
		else
		{
			tokenMarker.linesChanged(getDefaultRootElement()
				.getElementIndex(evt.getOffset()),1);
		}

		super.fireInsertUpdate(evt);
	}
	
	/**
	 * We overwrite this method to update the token marker
	 * state immediately so that any event listeners get a
	 * consistent token marker.
	 */
	protected void fireRemoveUpdate(DocumentEvent evt)
	{
		DocumentEvent.ElementChange ch = evt.getChange(
			getDefaultRootElement());
		if(ch != null)
		{
			int index = ch.getIndex();
			int len = ch.getChildrenRemoved().length -
				ch.getChildrenAdded().length;
			tokenMarker.linesChanged(index,
				tokenMarker.getLineCount() - index);
			tokenMarker.deleteLines(index + 1,len);
		}
		else
		{
			tokenMarker.linesChanged(getDefaultRootElement()
				.getElementIndex(evt.getOffset()),1);
		}

		super.fireRemoveUpdate(evt);
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
	private static final int IO = 2;
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
	private String name;
	private Mode mode;
	private TokenMarker tokenMarker;
	private MyUndoManager undo;
	private UndoableEdit saveUndo;
	private CompoundEdit compoundEdit;
	private boolean compoundEditNonEmpty;
	private int compoundEditCount;
	private Vector markers;
	private int savedSelStart;
	private int savedSelEnd;

	private void setPath(String path)
	{
		this.path = path;
		name = MiscUtilities.getFileName(path);

		vfs = VFSManager.getVFSForPath(path);
		if(vfs instanceof FileVFS)
		{
			file = new File(path);

			// if we don't do this, the autosave file won't be
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

	private void clearProperties()
	{
		((BufferProps)getDocumentProperties()).clear();
		putProperty("i18n",Boolean.FALSE);
	}

	private void parseBufferLocalProperties()
	{
		try
		{
			Element map = getDefaultRootElement();
			for(int i = 0; i < Math.min(10,map.getElementCount()); i++)
			{
				Element line = map.getElement(i);
				String text = getText(line.getStartOffset(),
					line.getEndOffset() - line.getStartOffset() - 1);
				parseBufferLocalProperty(text);
			}
	
			// Create marker positions
			for(int i = 0; i < markers.size(); i++)
			{
				((Marker)markers.elementAt(i))
					.createPositions();
			}
		}
		catch(BadLocationException bl)
		{
			bl.printStackTrace();
		}
	}

	private void parseBufferLocalProperty(String prop)
	{
		StringBuffer buf = new StringBuffer();
		String name = null;
		boolean escape = false;
		for(int i = 0; i < prop.length(); i++)
		{
			char c = prop.charAt(i);
			switch(c)
			{
			case ':':
				if(escape)
				{
					escape = false;
					buf.append(':');
					break;
				}
				if(name != null)
				{
					String value = buf.toString();
					try
					{
						putProperty(name,new Integer(value));
					}
					catch(NumberFormatException nf)
					{
						putProperty(name,value);
					}
				}
				buf.setLength(0);
				break;
			case '=':
				if(escape)
				{
					escape = false;
					buf.append('=');
					break;
				}
				name = buf.toString();
				buf.setLength(0);
				break;
			case '\\':
				if(escape)
					buf.append('\\');
				escape = !escape;
				break;
			case 'n':
				if(escape)
				{	buf.append('\n');
					escape = false;
					break;
				}
			case 't':
				if(escape)
				{
					buf.append('\t');
					escape = false;
					break;
				}
			default:
				buf.append(c);
				break;
			}
		}
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

	// we need to call some protected methods, so override this class
	// to make them public
	class MyUndoManager extends UndoManager
	{
		public UndoableEdit editToBeUndone()
		{
			return super.editToBeUndone();
		}

		public UndoableEdit editToBeRedone()
		{
			return super.editToBeRedone();
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
 * Revision 1.185  2000/11/07 10:08:30  sp
 * Options dialog improvements, documentation changes, bug fixes
 *
 * Revision 1.184  2000/11/05 00:44:13  sp
 * Improved HyperSearch, improved horizontal scroll, other stuff
 *
 * Revision 1.183  2000/11/02 09:19:31  sp
 * more features
 *
 * Revision 1.182  2000/10/30 07:14:03  sp
 * 2.7pre1 branched, GUI improvements
 *
 * Revision 1.181  2000/10/12 09:28:26  sp
 * debugging and polish
 *
 * Revision 1.180  2000/09/26 10:19:45  sp
 * Bug fixes, spit and polish
 *
 * Revision 1.179  2000/09/23 03:01:09  sp
 * pre7 yayayay
 *
 * Revision 1.178  2000/09/09 04:00:33  sp
 * 2.6pre6
 *
 * Revision 1.177  2000/09/03 03:16:52  sp
 * Search bar integrated with command line, enhancements throughout
 *
 * Revision 1.176  2000/08/31 02:54:00  sp
 * Improved activity log, bug fixes
 *
 * Revision 1.175  2000/08/29 07:47:10  sp
 * Improved complete word, type-select in VFS browser, bug fixes
 */
