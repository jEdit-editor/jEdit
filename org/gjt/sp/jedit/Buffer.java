/*
 * Buffer.java - jEdit buffer
 * Copyright (C) 1998, 1999, 2000, 2001 Slava Pestov
 * Portions copyright (C) 1999, 2000 mike dillon
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
import org.gjt.sp.jedit.gui.BufferOptions;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.search.RESearchMatcher;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.util.Log;

/**
 * An in-memory copy of an open file.<p>
 *
 * This is basically a Swing document with support for loading and
 * saving, and various other miscellanous things such as markers.<p>
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
		if(getBooleanProperty("syntax"))
			setTokenMarker(mode.getTokenMarker());
		else
			setTokenMarker(jEdit.getMode("text").getTokenMarker());

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
	 * Displays the buffer options dialog box.
	 * @since jEdit 2.7pre2
	 */
	public void showBufferOptionsDialog(View view)
	{
		new BufferOptions(view,this);
	}

	/**
	 * Displays the 'insert file' dialog box and inserts the selected file
	 * into the buffer.
	 * @param view The view
	 * @since jEdit 2.7pre2
	 */
	public void showInsertFileDialog(View view)
	{
		String[] files = GUIUtilities.showVFSFileDialog(view,null,
			VFSBrowser.OPEN_DIALOG,false);

		if(files != null)
			insert(view,files[0]);
	}

	/**
	 * Prints the buffer.
	 * @param view The view
	 * @since jEdit 2.7pre2
	 */
	public void print(View view)
	{
		PrintJob job = view.getToolkit().getPrintJob(view,name,null);
		if(job == null)
			return;

		view.showWaitCursor();

		int topMargin;
		int leftMargin;
		int bottomMargin;
		int rightMargin;
		int ppi = job.getPageResolution();

		try
		{
			topMargin = (int)(Float.valueOf(jEdit.getProperty(
				"print.margin.top")).floatValue() * ppi);
		}
		catch(NumberFormatException nf)
		{
			topMargin = ppi / 2;
		}
		try
		{
			leftMargin = (int)(Float.valueOf(jEdit.getProperty(
				"print.margin.left")).floatValue() * ppi);
		}
		catch(NumberFormatException nf)
		{
			leftMargin = ppi / 2;
		}
		try
		{
			bottomMargin = (int)(Float.valueOf(jEdit.getProperty(
				"print.margin.bottom")).floatValue() * ppi);
		}
		catch(NumberFormatException nf)
		{
			bottomMargin = topMargin;
		}
		try
		{
			rightMargin = (int)(Float.valueOf(jEdit.getProperty(
				"print.margin.right")).floatValue() * ppi);
		}
		catch(NumberFormatException nf)
		{
			rightMargin = leftMargin;
		}

		boolean printHeader = jEdit.getBooleanProperty("print.header");
		boolean printFooter = jEdit.getBooleanProperty("print.footer");
		boolean printLineNumbers = jEdit.getBooleanProperty("print.lineNumbers");
		boolean syntax = jEdit.getBooleanProperty("print.syntax");

		String header = path;
		String footer = new Date().toString();

		int lineCount = getDefaultRootElement().getElementCount();

		SyntaxStyle[] styles = view.getTextArea().getPainter().getStyles();
		TabExpander expander = null;

		Graphics gfx = null;

		String fontFamily = jEdit.getProperty("print.font");
		int fontSize;
		try
		{
			fontSize = Integer.parseInt(jEdit.getProperty(
				"print.fontsize"));
		}
		catch(NumberFormatException nf)
		{
			fontSize = 10;
		}
		int fontStyle;
		try
		{
			fontStyle = Integer.parseInt(jEdit.getProperty(
				"print.fontstyle"));
		}
		catch(NumberFormatException nf)
		{
			fontStyle = Font.PLAIN;
		}

		boolean style = jEdit.getBooleanProperty("print.style");
		boolean color = jEdit.getBooleanProperty("print.color");

		Font font = new Font(fontFamily,fontStyle,fontSize);

		FontMetrics fm = null;
		Dimension pageDimension = job.getPageDimension();
		int pageWidth = pageDimension.width;
		int pageHeight = pageDimension.height;
		int y = 0;
		int tabSize = 0;
		int lineHeight = 0;
		int page = 0;

		int lineNumberDigits = (int)Math.ceil(Math.log(
			lineCount) / Math.log(10));

		for(int i = 0; i < lineCount; i++)
		{
			if(gfx == null)
			{
				page++;

				gfx = job.getGraphics();
				gfx.setFont(font);
				fm = gfx.getFontMetrics();
				lineHeight = fm.getHeight();
				tabSize = getTabSize() * fm.charWidth(' ');
				expander = new PrintTabExpander(leftMargin,tabSize);

				y = topMargin + lineHeight - fm.getDescent()
					- fm.getLeading();

				if(printHeader)
				{
					gfx.setColor(Color.lightGray);
					gfx.fillRect(leftMargin,topMargin,pageWidth
						- leftMargin - rightMargin,lineHeight);
					gfx.setColor(Color.black);
					gfx.drawString(header,leftMargin,y);
					y += lineHeight;
				}
			}

			y += lineHeight;

			gfx.setColor(Color.black);
			gfx.setFont(font);

			int x = leftMargin;
			if(printLineNumbers)
			{
				int lineNumberWidth = fm.charWidth('0') * lineNumberDigits;
				String lineNumber = String.valueOf(i + 1);
				gfx.drawString(lineNumber,(leftMargin + lineNumberWidth)
					- fm.stringWidth(lineNumber),y);
				x += lineNumberWidth + fm.charWidth('0');
			}

			paintSyntaxLine(i,styles,expander,style,color,
				gfx,Color.white,x,y);

			int bottomOfPage = pageHeight - bottomMargin - lineHeight;
			if(printFooter)
				bottomOfPage -= lineHeight * 2;

			if(y >= bottomOfPage || i == lineCount - 1)
			{
				if(printFooter)
				{
					y = pageHeight - bottomMargin;

					gfx.setColor(Color.lightGray);
					gfx.setFont(font);
					gfx.fillRect(leftMargin,y - lineHeight,pageWidth
						- leftMargin - rightMargin,lineHeight);
					gfx.setColor(Color.black);
					y -= (lineHeight - fm.getAscent());
					gfx.drawString(footer,leftMargin,y);

					Integer[] args = { new Integer(page) };
					String pageStr = jEdit.getProperty("print.page",args);
					int width = fm.stringWidth(pageStr);
					gfx.drawString(pageStr,pageWidth - rightMargin
						- width,y);
				}

				gfx.dispose();
				gfx = null;
			}
		}

		job.end();

		view.hideWaitCursor();
	}

	/**
	 * Reloads the buffer from disk, asking for confirmation if the buffer
	 * is dirty.
	 * @param view The view
	 * @since jEdit 2.7pre2
	 */
	public void reload(View view)
	{
		if(getFlag(DIRTY))
		{
			String[] args = { name };
			int result = GUIUtilities.confirm(view,"changedreload",
				args,JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
			if(result != JOptionPane.YES_OPTION)
				return;
		}

		load(view,true);
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

				if(reload)
					clearProperties();
				else
				{
					// reload maxLineLen and tabSize
					// from the global/mode properties
					getDocumentProperties().remove(
						"tabSize");
					getDocumentProperties().remove(
						"indentSize");
					getDocumentProperties().remove(
						"maxLineLen");
					getDocumentProperties().remove(
						BufferIORequest.LOAD_DATA);
				}

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
				undo.addEdit(saveUndo = new SaveUndo());
				try
				{
					undo.setLimit(Integer.parseInt(
						jEdit.getProperty(
						"buffer.undoCount")));
				}
				catch(NumberFormatException nf)
				{
					undo.setLimit(100);
				}

				setMode();

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

				// send some EditBus messages
				if(!getFlag(TEMPORARY))
				{
					EditBus.send(new BufferUpdate(Buffer.this,
						BufferUpdate.LOADED));
					EditBus.send(new BufferUpdate(Buffer.this,
						BufferUpdate.MARKERS_CHANGED));
				}

				try
				{
					int collapseFolds = ((Integer)
						getProperty("collapseFolds"))
						.intValue();
					if(collapseFolds != 0)
						expandFolds(collapseFolds);
				}
				catch(Exception e)
				{
				}
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
			|| !getFlag(DIRTY)
			|| getFlag(LOADING)
			|| getFlag(IO))
			return;

		setFlag(AUTOSAVE_DIRTY,false);

		VFSManager.runInWorkThread(new BufferIORequest(
			BufferIORequest.AUTOSAVE,null,this,null,
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
				int result = GUIUtilities.confirm(view,
					"filechanged-save",args,
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
				// Saving a NEW_FILE will create a file on
				// disk, thus file system browsers must reload
				if(getFlag(NEW_FILE) || !getPath().equals(oldPath))
					VFSManager.sendVFSUpdate(getVFS(),getPath(),true);

				setFlag(IO,false);

				if(rename)
				{
					// we do a write lock so that the
					// autosave, which grabs a read lock,
					// is not executed between the
					// deletion of the autosave file
					// and clearing of the dirty flag
					try
					{
						Buffer.this.writeLock();

						if(autosaveFile != null)
							autosaveFile.delete();

						setFlag(AUTOSAVE_DIRTY,false);
						setFlag(READ_ONLY,false);
						setFlag(NEW_FILE,false);
						setFlag(UNTITLED,false);
						setFlag(DIRTY,false);

						// can only have one of these in the queue
						if(saveUndo != null)
							saveUndo.die();

						undo.addEdit(saveUndo = new SaveUndo());
					}
					finally
					{
						Buffer.this.writeUnlock();
					}

					if(!getPath().equals(oldPath))
					{
						jEdit.updatePosition(Buffer.this);
						setMode();
					}

					if(file != null)
						modTime = file.lastModified();

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

		boolean newReadOnly = !file.canWrite();
		if(newReadOnly != getFlag(READ_ONLY))
		{
			setFlag(READ_ONLY,newReadOnly);
			EditBus.send(new BufferUpdate(this,
				BufferUpdate.DIRTY_CHANGED));
		}

		if(!jEdit.getBooleanProperty("view.checkModStatus"))
			return;

		long oldModTime = modTime;
		long newModTime = file.lastModified();

		if(newModTime != oldModTime)
		{
			modTime = newModTime;

			if(!file.exists())
			{
				setFlag(NEW_FILE,true);
				EditBus.send(new BufferUpdate(this,
					BufferUpdate.DIRTY_CHANGED));
				Object[] args = { path };
				GUIUtilities.message(view,"filedeleted",args);
				return;
			}

			String prop = (isDirty() ? "filechanged-dirty"
				: "filechanged-focus");

			Object[] args = { path };
			int result = GUIUtilities.confirm(view,
				prop,args,JOptionPane.YES_NO_OPTION,
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
	 * Returns the file for this buffer. This may be null if the buffer
	 * is non-local.
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
			setFlag(DIRTY,false);
			setFlag(AUTOSAVE_DIRTY,false);

			// can only have one of these in the queue
			if(saveUndo != null)
				saveUndo.die();

			undo.addEdit(saveUndo = new SaveUndo());
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
	 * Undoes the most recent edit.
	 *
	 * @since jEdit 2.7pre2
	 */
	public void undo()
	{
		if(undo == null)
			return;

		if(!isEditable())
		{
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		try
		{
			setFlag(UNDO_IN_PROGRESS,true);
			undo.undo();
		}
		catch(CannotUndoException cu)
		{
			Log.log(Log.DEBUG,this,cu);
			Toolkit.getDefaultToolkit().beep();
			return;
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
	 * @since jEdit 2.7pre2
	 */
	public void redo()
	{
		if(undo == null)
			return;

		if(!isEditable())
		{
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		try
		{
			setFlag(UNDO_IN_PROGRESS,true);
			undo.redo();
		}
		catch(CannotRedoException cr)
		{
			Log.log(Log.DEBUG,this,cr);
			Toolkit.getDefaultToolkit().beep();
			return;
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
	 * Returns if a compound edit is currently active.
	 * @since jEdit 3.1pre1
	 */
	public boolean insideCompoundEdit()
	{
		return compoundEdit != null;
	}

	/**
	 * Removes trailing whitespace from all lines in the specified range.
	 * @param first The start line
	 * @param last The end line
	 * @since jEdit 2.7pre2
	 */
	public void removeTrailingWhiteSpace(int first, int last)
	{
		int line, pos, lineStart, lineEnd, tail;

		Element map = getDefaultRootElement();

		try
		{
			beginCompoundEdit();

			for (line = first; line <= last; line++)
			{
				Element lineElement = map.getElement(line);
				getText(lineElement.getStartOffset(),
					lineElement.getEndOffset()
					- lineElement.getStartOffset() - 1,seg);

				// blank line
				if (seg.count == 0) continue;

				lineStart = seg.offset;
				lineEnd = seg.offset + seg.count - 1;

				for (pos = lineEnd; pos >= lineStart; pos--)
				{
					if (!Character.isWhitespace(seg.array[pos]))
						break;
				}

				tail = lineEnd - pos;

				// no whitespace
				if (tail == 0) continue;

				remove(lineElement.getEndOffset() - 1 - tail,
					tail);
			}
		}
		catch (BadLocationException ble)
		{
			Log.log(Log.ERROR, this, ble);
		}
		finally
		{
			endCompoundEdit();
		}
	}

	/**
	 * Shifts the indent of each line in the specified range to the left.
	 * @param first The first line
	 * @param last The last line
	 */
	public void shiftIndentLeft(int first, int last)
	{
		try
		{
			beginCompoundEdit();

			int tabSize = getTabSize();
			int indentSize = getIndentSize();
			boolean noTabs = getBooleanProperty("noTabs");
			Element map = getDefaultRootElement();

			for(int i = first; i <= last; i++)
			{
				Element lineElement = map.getElement(i);
				int lineStart = lineElement.getStartOffset();
				String line = getText(lineStart,
					lineElement.getEndOffset() - lineStart - 1);
				int whiteSpace = MiscUtilities
					.getLeadingWhiteSpace(line);
				if(whiteSpace == 0)
					continue;
				int whiteSpaceWidth = Math.max(0,MiscUtilities
					.getLeadingWhiteSpaceWidth(line,tabSize)
					- indentSize);
				remove(lineStart,whiteSpace);
				insertString(lineStart,MiscUtilities
					.createWhiteSpace(whiteSpaceWidth,
					(noTabs ? 0 : tabSize)),null);
			}
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
		finally
		{
			endCompoundEdit();
		}
	}

	/**
	 * Shifts the indent of each line in the specified range to the right.
	 * @param first The first line
	 * @param last The last line
	 */
	public void shiftIndentRight(int first, int last)
	{
		try
		{
			beginCompoundEdit();

			int tabSize = getTabSize();
			int indentSize = getIndentSize();
			boolean noTabs = getBooleanProperty("noTabs");
			Element map = getDefaultRootElement();
			for(int i = first; i <= last; i++)
			{
				Element lineElement = map.getElement(i);
				int lineStart = lineElement.getStartOffset();
				String line = getText(lineStart,
					lineElement.getEndOffset() - lineStart - 1);
				int whiteSpace = MiscUtilities
					.getLeadingWhiteSpace(line);
				int whiteSpaceWidth = MiscUtilities
					.getLeadingWhiteSpaceWidth(
					line,tabSize) + indentSize;
				remove(lineStart,whiteSpace);
				insertString(lineStart,MiscUtilities
					.createWhiteSpace(whiteSpaceWidth,
					(noTabs ? 0 : tabSize)),null);
			}
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
		finally
		{
			endCompoundEdit();
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
		// don't do this while loading, otherwise we will
		// blow away caret location properties
		if(!getFlag(LOADING))
			clearProperties();
		parseBufferLocalProperties();

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
	}

	/**
	 * If auto indent is enabled, this method is called when the `Tab'
	 * or `Enter' key is pressed to perform mode-specific indentation
	 * and return true, or return false if a normal tab is to be inserted.
	 * @param line The line number to indent
	 * @param canIncreaseIndent If false, nothing will be done if the
	 * calculated indent is greater than the current
	 * @param canDecreaseIndent If false, nothing will be done if the
	 * calculated indent is less than the current
	 * @return true if the tab key event should be swallowed (ignored)
	 * false if a real tab should be inserted
	 */
	public boolean indentLine(int lineIndex, boolean canIncreaseIndent,
		boolean canDecreaseIndent)
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
					RE.REG_ICASE,RESearchMatcher.RE_SYNTAX_JEDIT);
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

		Element map = getDefaultRootElement();

		String prevLine = null;
		String line = null;

		Element lineElement = map.getElement(lineIndex);
		int start = lineElement.getStartOffset();

		// Get line text
		try
		{
			line = getText(start,lineElement.getEndOffset() - start - 1);

			for(int i = lineIndex - 1; i >= 0; i--)
			{
				lineElement = map.getElement(i);
				int lineStart = lineElement.getStartOffset();
				int len = lineElement.getEndOffset() - lineStart - 1;
				if(len != 0)
				{
					prevLine = getText(lineStart,len);
					break;
				}
			}
	
			if(prevLine == null)
				return false;
		}
		catch(BadLocationException e)
		{
			Log.log(Log.ERROR,this,e);
			return false;
		}

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
					lineElement = map.getElement(map.getElementIndex(
						offset));
					int startOffset = lineElement.getStartOffset();
					String closeLine = getText(startOffset,
						lineElement.getEndOffset() - startOffset - 1);
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
	 * Indents all specified lines.
	 * @param start The first line to indent
	 * @param end The last line to indent
	 * @since jEdit 3.1pre3
	 */
	public void indentLines(int start, int end)
	{
		beginCompoundEdit();
		for(int i = start; i <= end; i++)
			indentLine(i,true,true);
		endCompoundEdit();
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
		linesChanged(start,len);

		for(int i = 0; i < len; i++)
			markTokens(start + i);
	}

	/**
	 * Paints the specified line onto the graphics context.
	 * @param buffer The buffer
	 * @param lineIndex The line
	 * @param styles The syntax style list
	 * @param expander The tab expander used to determine tab stops. May
	 * be null
	 * @param style True if the output should be syntax styled
	 * @param color True if the output should be syntax colored
	 * @param gfx The graphics context
	 * @param x The x co-ordinate
	 * @param y The y co-ordinate
	 * @return The x co-ordinate, plus the width of the painted string
	 * @since jEdit 3.1pre1
	 */
	public int paintSyntaxLine(int lineIndex, SyntaxStyle[] styles,
		TabExpander expander, boolean style, boolean color,
		Graphics gfx, Color background, int x, int y)
	{
		LineInfo info = lineInfo[lineIndex];

		if(info.tokensValid)
		{
			// have to do this 'manually'
			Element lineElement = getDefaultRootElement()
				.getElement(lineIndex);
			int lineStart = lineElement.getStartOffset();
			try
			{
				getText(lineStart,lineElement.getEndOffset()
					- lineStart - 1,seg);
			}
			catch(BadLocationException e)
			{
				Log.log(Log.ERROR,this,e);
			}
		}
		else
			markTokens(lineIndex);

		Token tokens = info.firstToken;

		// the above should leave the text in the 'line' segment

		Font defaultFont = gfx.getFont();
		Color defaultColor = gfx.getColor();

		int originalOffset = seg.offset;
		int originalCount = seg.count;

		int offset = 0;
		for(;;)
		{
			byte id = tokens.id;
			if(id == Token.END)
				break;

			int length = tokens.length;
			seg.count = length;
			if(id == Token.NULL)
			{
				gfx.setColor(defaultColor);
				gfx.setFont(defaultFont);
			}
			else
			{
				Color bg = styles[id].getBackgroundColor();
				if(color && bg != null)
				{
					FontMetrics fm = (style ? styles[id].getFontMetrics(defaultFont)
						: gfx.getFontMetrics(defaultFont));
					int width   = Utilities.getTabbedTextWidth(seg, fm, x, expander, 0); 
					int height  = fm.getHeight();
					int descent = fm.getDescent();
					int leading = fm.getLeading();
					gfx.setColor(background);
					gfx.setXORMode(bg);
					gfx.fillRect(x, y - height + descent + leading, width, height);
					gfx.setPaintMode();
				}

				if(style)
					gfx.setFont(styles[id].getStyledFont(defaultFont));
				if(color)
					gfx.setColor(styles[id].getForegroundColor());
			}

			x = Utilities.drawTabbedText(seg,x,y,gfx,expander,0);
			seg.offset += length;
			offset += length;

			tokens = tokens.next;
		}

		seg.offset = originalOffset;
		seg.count = originalCount;

		return x;
	}

	/**
	 * Returns the syntax tokens for the specified line.
	 * @param lineIndex The line number
	 * @since jEdit 3.1pre1
	 */
	public LineInfo markTokens(int lineIndex)
	{
		LineInfo info = lineInfo[lineIndex];

		/* If cached tokens are valid, return 'em */
		if(info.tokensValid)
			return info;

		//long _start = System.currentTimeMillis();

		/*
		 * Else, go up to 100 lines back, looking for a line with
		 * cached tokens. Tokenize from that line to this line.
		 */
		int start = Math.max(0,lineIndex - 100) - 1;
		int end = Math.max(0,lineIndex - 100);

		for(int i = lineIndex - 1; i > end; i--)
		{
			if(lineInfo[i].tokensValid)
			{
				start = i;
				break;
			}
		}

		LineInfo prev;
		if(start == -1)
			prev = null;
		else
			prev = lineInfo[start];

		//System.err.println("i=" + lineIndex + ",start=" + start);
		Element map = getDefaultRootElement();

		for(int i = start + 1; i <= lineIndex; i++)
		{
			info = lineInfo[i];
			if(info.tokensValid)
			{
				prev = info;
				continue;
			}

			Element lineElement = map.getElement(i);
			int lineStart = lineElement.getStartOffset();
			try
			{
				getText(lineStart,lineElement.getEndOffset()
					- lineStart - 1,seg);
			}
			catch(BadLocationException e)
			{
				Log.log(Log.ERROR,this,e);
			}

			/* Prepare for tokenization */
			info.lastToken = null;

			ParserRule oldRule = info.context.inRule;
			TokenMarker.LineContext oldParent = info.context.parent;

			tokenMarker.markTokens(prev,info,seg);

			ParserRule newRule = info.context.inRule;
			TokenMarker.LineContext newParent = info.context.parent;

			info.tokensValid = true;

			if(i != lastTokenizedLine)
			{
				nextLineRequested = false;
				lastTokenizedLine = i;
			}

			nextLineRequested |= (oldRule != newRule || oldParent != newParent);

			info.addToken(0,Token.END);

			prev = info;
		}

		if(nextLineRequested && lineCount - lineIndex > 1)
		{
			linesChanged(lineIndex + 1,lineCount - lineIndex - 1);
		}

		//System.err.println(System.currentTimeMillis() - _start);

		return info;
	}

	/**
	 * Store the width of a line, in pixels.
	 * @param lineIndex The line number
	 * @param width The width
	 * @since jEdit 3.1pre1
	 */
	public boolean setLineWidth(int lineIndex, int width)
	{
		LineInfo info = lineInfo[lineIndex];
		int oldWidth = info.width;
		info.width = width;
		return width != oldWidth;
	}

	/**
	 * Returns the maximum line width in the specified line range.
	 * The strange mix of physical/virtual line numbers is due to
	 * the way the text area paints lines.
	 * @param start The first physical line
	 * @param len The number of virtual lines from the first line
	 * @since jEdit 3.1pre1
	 */
	public int getMaxLineWidth(int start, int len)
	{
		int retVal = 0;
		int lines = 0;
		for(int i = start; ; i++)
		{
			if(i >= lineCount || lines >= len)
				break;

			LineInfo info = lineInfo[i];
			if(info.visible)
			{
				retVal = Math.max(lineInfo[i].width,retVal);
				lines++;
			}
		}
		return retVal;
	}

	/*
	 * Returns true if the next line should be repainted. This
	 * will return true after a line has been tokenized that starts
	 * a multiline token that continues onto the next line.
	 */
	public boolean isNextLineRequested()
	{
		return nextLineRequested;
	}

	/**
	 * Returns the line info object for the specified line.
	 * @since jEdit 3.1pre1
	 */
	public LineInfo getLineInfo(int line)
	{
		return lineInfo[line];
	}

	/**
	 * Returns if the specified line is visible.
	 * @since jEdit 3.1pre1
	 */
	public boolean isLineVisible(int line)
	{
		return lineInfo[line].visible;
	}

	/**
	 * Returns if the specified line begins a fold.
	 * @since jEdit 3.1pre1
	 */
	public boolean isFoldStart(int line)
	{
		if(line == lineCount - 1)
			return false;

		// how it works:

		// - if a line has a greater fold level than the next,
		//   it is a fold

		// - if a line is invisible, it is also a fold, even
		//   if the fold level is the same (rationale: changing
		//   indent levels while folds are collapsed shouldn't
		//   create pernamently inaccessable sections)

		// - exception to the above: if the line is the last
		//   virtual line, don't report it as a fold if the
		//   fold levels are the same and the next is invisible,
		//   otherwise the last narrowed line will always be
		//   a fold start which is silly

		// note that the last two cases are temporarily disabled
		// in 3.1pre3 because expandFoldAt() doesn't handle them
		// properly.
		return getFoldLevel(line) < getFoldLevel(line + 1);
			/*|| (line != virtualLines[virtualLineCount - 1]
			&& !lineInfo[line + 1].visible);*/
	}

	/**
	 * Returns the fold level of the specified line.
	 * @since jEdit 3.1pre1
	 */
	public int getFoldLevel(int line)
	{
		LineInfo info = lineInfo[line];

		if(info.foldLevelValid)
			return info.foldLevel;
		else
		{
			boolean changed = false;

			// make this configurable!
			int tabSize = getTabSize();

			Element lineElement = getDefaultRootElement()
				.getElement(line);
			int start = lineElement.getStartOffset();
			try
			{
				getText(start,lineElement.getEndOffset() - start - 1,seg);
			}
			catch(BadLocationException bl)
			{
				Log.log(Log.ERROR,this,bl);
			}

			int offset = seg.offset;
			int count = seg.count;

			int whitespace = 0;

			if(count == 0)
			{
				// empty line. inherit previous line's fold level
				if(line != 0)
					whitespace = getFoldLevel(line - 1);
				else
					whitespace = 0;
			}
			else
			{
				// this is so that lines consisting of only
				// whitespace don't cause disruptions
				boolean seenNonWhitespace = false;
loop:				for(int i = 0; i < count; i++)
				{
					switch(seg.array[offset + i])
					{
					case ' ':
						whitespace++;
						break;
					case '\t':
						whitespace += (tabSize - whitespace % tabSize);
						break;
					default:
						seenNonWhitespace = true;
						break loop;
					}
				}

				if(!seenNonWhitespace)
				{
					if(line != 0)
						whitespace = getFoldLevel(line - 1);
					else
						whitespace = 0;
				}
			}

			if(info.foldLevel != whitespace)
			{
				info.foldLevel = whitespace;
				fireFoldLevelsChanged(line - 1,line - 1);
			}

			info.foldLevelValid = true;
			return whitespace;
		}
	}

	/**
	 * Returns the previous visible line before the specified index, or
	 * -1 if no previous lines are visible.
	 * @param lineNo The line
	 * @since jEdit 3.1pre1
	 */
	public int getPrevVisibleLine(int lineNo)
	{
		for(int i = lineNo - 1; i >= 0; i--)
		{
			if(lineInfo[i].visible)
				return i;
		}

		return -1;
	}

	/**
	 * Returns the next visible line after the specified index, or
	 * -1 if no subsequent lines are visible.
	 * @param lineNo The line
	 * @since jEdit 3.1pre1
	 */
	public int getNextVisibleLine(int lineNo)
	{
		for(int i = lineNo + 1; i < lineCount; i++)
		{
			if(lineInfo[i].visible)
				return i;
		}

		return -1;
	}

	/**
	 * Maps a virtual line number to a physical line number. To simplify
	 * matters for text area highlighters, this method maps out-of-bounds
	 * line numbers as well.
	 * @since jEdit 3.1pre1
	 */
	public int virtualToPhysical(int lineNo)
	{
		if(lineNo >= virtualLineCount)
			return lineCount + (lineNo - virtualLineCount);

		return virtualLines[lineNo];
	}

	/**
	 * Maps a physical line number to a virtual line number.
	 * @since jEdit 3.1pre1
	 */
	public int physicalToVirtual(int lineNo)
	{
		int start = 0;
		int end = virtualLineCount - 1;

		if(lineNo < virtualLines[start])
			return start;
		else if(lineNo > virtualLines[end])
			return end;

		// funky binary search
		for(;;)
		{
			switch(end - start)
			{
			case 0:
				if(virtualLines[start] < lineNo)
					return start + 1;
				else
					return start;
			case 1:
				if(virtualLines[start] < lineNo)
				{
					if(virtualLines[end] < lineNo)
						return end + 1;
					else
						return end;
				}
				else
					return start;
			default:
				int pivot = start + (end - start) / 2;
				int value = virtualLines[pivot];
				if(value == lineNo)
					return pivot;
				else if(value < lineNo)
					start = pivot + 1;
				else
					end = pivot - 1;
				break;
			}
		}
	}

	/**
	 * Collapse the fold that contains the specified line number.
	 * @param line The first line number of the fold
	 * @return False if there are no folds in the buffer
	 * @since jEdit 3.1pre1
	 */
	public boolean collapseFoldAt(int line)
	{
		int initialFoldLevel = getFoldLevel(line);

		int start = 0;
		int end = lineCount - 1;

		if(line != lineCount - 1
			&& getFoldLevel(line + 1) > initialFoldLevel)
		{
			// this line is the start of a fold
			start = line + 1;

			for(int i = line + 1; i < lineCount; i++)
			{
				if(getFoldLevel(i) <= initialFoldLevel)
				{
					end = i - 1;
					break;
				}
			}
		}
		else
		{
			boolean ok = false;

			// scan backwards looking for the start
			for(int i = line - 1; i >= 0; i--)
			{
				if(getFoldLevel(i) < initialFoldLevel)
				{
					start = i + 1;
					ok = true;
					break;
				}
			}

			if(!ok)
			{
				// no folds in buffer
				return false;
			}

			for(int i = line + 1; i < lineCount; i++)
			{
				if(getFoldLevel(i) < initialFoldLevel)
				{
					end = i - 1;
					break;
				}
			}
		}

		int delta = (end - start + 1);

		for(int i = start; i <= end; i++)
		{
			LineInfo info = lineInfo[i];
			if(info.visible)
				info.visible = false;
			else
				delta--;
		}

		if(delta == 0)
		{
			// user probably pressed A+BACK_SPACE twice
			return false;
		}

		//System.err.println("collapse from " + start + " to " + end);

		// I forgot to do this at first and it took me ages to
		// figure out
		start = physicalToVirtual(start);

		//System.err.println("virtualized start is " + start);

		// update virtual -> physical map
		virtualLineCount -= delta;

		//System.err.println("new virtual line count is " + virtualLineCount);

		System.arraycopy(virtualLines,start + delta,virtualLines,start,
			virtualLines.length - start - delta);

		//System.err.println("copy from " + (start + delta)
		//	+ " to " + start);

		fireFoldStructureChanged();

		return true;
	}

	/**
	 * Expand the fold that begins at the specified line number.
	 * @param line The first line number of the fold
	 * @param fully If true, fold will be expanded fully, otherwise
	 * only one level will be expanded
	 * @return False if there are no folds in the buffer
	 * @since jEdit 3.1pre2
	 */
	public boolean expandFoldAt(int line, boolean fully)
	{
		int initialFoldLevel = getFoldLevel(line);

		int start = 0;
		int end = lineCount - 1;

		if(line != lineCount - 1
			&& lineInfo[line].visible
			&& !lineInfo[line + 1].visible
			&& getFoldLevel(line + 1) > initialFoldLevel)
		{
			// this line is the start of a fold
			start = line + 1;

			for(int i = line + 1; i < lineCount; i++)
			{
				if(lineInfo[i].visible && getFoldLevel(i) <= initialFoldLevel)
				{
					end = i - 1;
					break;
				}
			}
		}
		else
		{
			/* if(lineInfo[line].visible)
			{
				// the user probably pressed A+ENTER twice
				return false;
			} */

			boolean ok = false;

			// scan backwards looking for the start
			for(int i = line - 1; i >= 0; i--)
			{
				if(lineInfo[i].visible && getFoldLevel(i) < initialFoldLevel)
				{
					start = i + 1;
					ok = true;
					break;
				}
			}

			if(!ok)
			{
				// no folds in buffer
				return false;
			}

			for(int i = line + 1; i < lineCount; i++)
			{
				if(lineInfo[i].visible && getFoldLevel(i) < initialFoldLevel)
				{
					end = i - 1;
					break;
				}
			}
		}

		int delta = 0;
		int tmpMapLen = 0;
		int[] tmpVirtualMap = new int[end - start + 1];

		// we need a different value of initialFoldLevel here!
		initialFoldLevel = getFoldLevel(start);
		//System.err.println("new initial fold level is " + initialFoldLevel);

		for(int i = start; i <= end; i++)
		{
			LineInfo info = lineInfo[i];
			if(info.visible)
			{
				// user will be confused if 'expand fold'
				// hides lines
				tmpVirtualMap[tmpMapLen++] = i;
			}
			else if(!fully && getFoldLevel(i) > initialFoldLevel)
			{
				// don't expand lines with higher fold
				// levels
			}
			else
			{
				//System.err.println("fuck you: " + i);
				tmpVirtualMap[tmpMapLen++] = i;
				delta++;
				info.visible = true;
			}
		}

		// I forgot to do this at first and it took me ages to
		// figure out
		int virtualLine;
		if(start > virtualLines[virtualLineCount - 1])
			virtualLine = virtualLineCount;
		else
			virtualLine = physicalToVirtual(start);

		//System.err.println("virtual start is " + virtualLine
		//	+ ", physical start is " + start);

		// update virtual -> physical map
		virtualLineCount += delta;

		//System.err.println("virtual line count is " + virtualLineCount);

		if(virtualLines.length <= virtualLineCount)
		{
			int[] virtualLinesN = new int[(virtualLineCount + 1) * 2];
			System.arraycopy(virtualLines,0,
				virtualLinesN,0,virtualLines.length);
			virtualLines = virtualLinesN;
		}

		//System.err.println("copy from " + (virtualLine)
		//	+ " to " + (virtualLine + delta));

		System.arraycopy(virtualLines,virtualLine,virtualLines,
			virtualLine + delta,virtualLines.length
			- virtualLine - delta);
		for(int j = 0; j < tmpMapLen; j++)
		{
			//System.err.println((virtualLine + j) + " maps to " + tmpVirtualMap[j]);
			virtualLines[virtualLine + j] = tmpVirtualMap[j];
		}

		fireFoldStructureChanged();

		return true;
	}

	/**
	 * This is intended to be called from actions.xml.
	 * @since jEdit 3.1pre1
	 */
	public void expandFolds(char digit)
	{
		if(digit < '1' || digit > '9')
		{
			Toolkit.getDefaultToolkit().beep();
			return;
		}
		else
			expandFolds((int)(digit - '1') + 1);
	}

	/**
	 * Expand all folds in the buffer up to a specified level.
	 * @param level All folds less than this level will be expanded,
	 * others will be collapsed. This is not the actual fold level;
	 * it is multiplied by the indent size first
	 * @since jEdit 3.1pre1
	 */
	public void expandFolds(int level)
	{
		if(virtualLines.length <= lineCount)
		{
			int[] virtualLinesN = new int[(lineCount + 1) * 2];
			System.arraycopy(virtualLines,0,
				virtualLinesN,0,virtualLines.length);
			virtualLines = virtualLinesN;
		}

		level = (level - 1) * getIndentSize() + 1;

		virtualLineCount = 0;

		for(int i = 0; i < lineCount; i++)
		{
			if(getFoldLevel(i) < level)
			{
				lineInfo[i].visible = true;
				virtualLines[virtualLineCount++] = i;
			}
			else
				lineInfo[i].visible = false;
		}

		fireFoldStructureChanged();
	}

	/**
	 * Expand all folds in the specified document.
	 * @since jEdit 3.1pre1
	 */
	public void expandAllFolds()
	{
		if(virtualLines.length <= lineCount)
		{
			int[] virtualLinesN = new int[(lineCount + 1) * 2];
			System.arraycopy(virtualLines,0,
				virtualLinesN,0,virtualLines.length);
			virtualLines = virtualLinesN;
		}

		virtualLineCount = lineCount;
		for(int i = 0; i < lineCount; i++)
		{
			virtualLines[i] = i;
			lineInfo[i].visible = true;
		}

		fireFoldStructureChanged();
	}

	/**
	 * Narrows the visible portion of the buffer to the specified
	 * line range.
	 * @param start The first line
	 * @param end The last line
	 * @since jEdit 3.1pre3
	 */
	public void narrow(int start, int end)
	{
		virtualLineCount = end - start + 1;
		virtualLines = new int[virtualLineCount];

		for(int i = 0; i < start; i++)
			lineInfo[i].visible = false;

		for(int i = start; i <= end; i++)
		{
			LineInfo info = lineInfo[i];
			info.visible = true;
			virtualLines[i - start] = i;
		}

		for(int i = end + 1; i < lineCount; i++)
			lineInfo[i].visible = false;

		fireFoldStructureChanged();
	}

	/**
	 * Adds a fold listener.
	 * @param listener The listener
	 * @since jEdit 3.1pre1
	 */
	public void addFoldListener(FoldListener l)
	{
		foldListeners.addElement(l);
	}

	/**
	 * Removes a fold listener.
	 * @param listener The listener
	 * @since jEdit 3.1pre1
	 */
	public void removeFoldListener(FoldListener l)
	{
		foldListeners.removeElement(l);
	}

	/**
	 * Returns the number of physical lines in the buffer.
	 * @since jEdit 3.1pre1
	 */
	public int getLineCount()
	{
		return lineCount;
	}

	/**
	 * Returns the number of virtual lines in the buffer.
	 * @since jEdit 3.1pre1
	 */
	public int getVirtualLineCount()
	{
		return virtualLineCount;
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
		if(!getFlag(READ_ONLY))
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
		lineCount = 1;
		lineInfo = new LineInfo[1];
		lineInfo[0] = new LineInfo();
		lineInfo[0].visible = true;

		virtualLineCount = 1;
		virtualLines = new int[1];
		foldListeners = new Vector();

		seg = new Segment();
		lastTokenizedLine = -1;

		setDocumentProperties(new BufferProps());
		clearProperties();

		setFlag(TEMPORARY,temp);
		setFlag(READ_ONLY,readOnly);

		markers = new Vector();

		addUndoableEditListener(new UndoHandler());

		Enumeration keys = props.keys();
		Enumeration values = props.elements();
		while(keys.hasMoreElements())
		{
			putProperty(keys.nextElement(),values.nextElement());
		}

		Mode defaultMode = jEdit.getMode(jEdit.getProperty("buffer.defaultMode"));
		if(defaultMode == null)
			defaultMode = jEdit.getMode("text");
		setMode(defaultMode);

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
	 * We overwrite this method to update the line info array
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
			addLinesToMap(ch.getIndex() + 1,len);
			linesChanged(index,lineCount - index);
			index += (len + 1);
		}
		else
		{
			linesChanged(getDefaultRootElement()
				.getElementIndex(evt.getOffset()),1);
		}

		setDirty(true);

		super.fireInsertUpdate(evt);
	}
	
	/**
	 * We overwrite this method to update the line info array
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
			removeLinesFromMap(index,len);
			linesChanged(index,lineCount - index);
		}
		else
		{
			linesChanged(getDefaultRootElement()
				.getElementIndex(evt.getOffset()),1);
		}

		setDirty(true);

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
	private static final int UNDO_IN_PROGRESS = 8;
	private static final int TEMPORARY = 9;

	private int flags;

	private long modTime;
	private File file;
	private VFS vfs;
	private File autosaveFile;
	private String path;
	private String name;
	private Mode mode;

	private MyUndoManager undo;
	private UndoableEdit saveUndo;
	private CompoundEdit compoundEdit;
	private boolean compoundEditNonEmpty;
	private int compoundEditCount;
	private Vector markers;
	private int savedSelStart;
	private int savedSelEnd;

	// Syntax highlighting
	private TokenMarker tokenMarker;
	private Segment seg;
	private LineInfo[] lineInfo;
	private int lineCount;
	private int lastTokenizedLine;
	private boolean nextLineRequested;

	// Folding
	private int[] virtualLines;
	private int virtualLineCount;
	private Vector foldListeners;

	private void setPath(String path)
	{
		this.path = path;

		vfs = VFSManager.getVFSForPath(path);
		name = vfs.getFileName(path);

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
		int result = GUIUtilities.confirm(view,"autosave-found",args,
			JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);

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
		String lineSeparator = (String)getProperty(LINESEP);
		((BufferProps)getDocumentProperties()).clear();
		putProperty("i18n",Boolean.FALSE);
		if(lineSeparator != null)
			putProperty(LINESEP,lineSeparator);
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

	private void setTokenMarker(TokenMarker tokenMarker)
	{
		this.tokenMarker = tokenMarker;

		ParserRuleSet mainSet = tokenMarker.getMainRuleSet();
		for(int i = 0; i < lineCount; i++)
		{
			LineInfo info = lineInfo[i];
			info.context = new TokenMarker.LineContext(null,mainSet);
			info.tokensValid = false;
		}
	}

	/**
	 * Inserts the specified line range into the virtual to physical
	 * mapping and line info array.
	 * @param index The first line number
	 * @param lines The number of lines 
	 */
	private void addLinesToMap(int index, int lines)
	{
		if(lines <= 0)
			return;

		LineInfo prev = lineInfo[index - 1];

		int virtualLine;
		// special case
		if(index == lineCount)
			virtualLine = virtualLineCount;
		else
			virtualLine = physicalToVirtual(index);

		int virtualLength;

		/* update the virtual -> physical mapping if the newly
		 * inserted lines are actually visible */
		if(prev.visible)
		{
			virtualLineCount += lines;

			if(virtualLines.length <= virtualLineCount)
			{
				int[] virtualLinesN = new int[
					(virtualLineCount + 1) * 2];
				System.arraycopy(virtualLines,0,
					virtualLinesN,0,
					virtualLines.length);
				virtualLines = virtualLinesN;
			}

			virtualLength = virtualLine + lines;

			System.arraycopy(virtualLines,virtualLine,
				virtualLines,virtualLength,
				virtualLines.length - virtualLength);

			for(int i = 0; i < lines; i++)
				virtualLines[virtualLine + i] = index + i;
		}
		else
			virtualLength = virtualLine + 1;

		for(int i = virtualLength; i < virtualLineCount; i++)
			virtualLines[i] += lines;

		lineCount += lines;

		if(lineInfo.length <= lineCount)
		{
			LineInfo[] lineInfoN = new LineInfo[(lineCount + 1) * 2];
			System.arraycopy(lineInfo,0,lineInfoN,0,
					 lineInfo.length);
			lineInfo = lineInfoN;
		}

		int length = index + lines;
		System.arraycopy(lineInfo,index,lineInfo,length,
			lineInfo.length - length);

		ParserRuleSet mainSet = tokenMarker.getMainRuleSet();
		for(int i = 0; i < lines; i++)
		{
			LineInfo info = new LineInfo();
			info.context = new TokenMarker.LineContext(null,mainSet);
			info.visible = prev.visible;
			lineInfo[index + i] = info;
		}
	}

	/**
	 * Deletes the specified line range from the virtual to physical
	 * mapping and line info array.
	 * @param index The first line number
	 * @param lines The number of lines
	 */
	private void removeLinesFromMap(int index, int lines)
	{
		if (lines <= 0)
			return;

		int length = index + lines;
		int virtualLine = physicalToVirtual(index);
		int virtualLength = physicalToVirtual(length);

		if(length <= virtualLines[virtualLineCount - 1])
		{
			System.arraycopy(virtualLines,virtualLength,
				virtualLines,virtualLine,
				virtualLines.length - virtualLength);

			for(int i = virtualLine;
				i < virtualLineCount
				- (virtualLength - virtualLine);
				i++)
				virtualLines[i] -= lines;
		}

		virtualLineCount -= (virtualLength - virtualLine);

		lineCount -= lines;
		System.arraycopy(lineInfo,length,lineInfo,
			index,lineInfo.length - length);
	}

	/**
	 * Called when the specified lines change. This invalidates
	 * cached syntax tokens and fold level.
	 * @param index The first line number
	 * @param lines The number of lines
	 */
	private void linesChanged(int index, int lines)
	{
		for(int i = 0; i < lines; i++)
		{
			LineInfo info = lineInfo[index + i];
			info.tokensValid = false;
			info.foldLevelValid = false;
		}
	}

	static class PrintTabExpander implements TabExpander
	{
		private int leftMargin;
		private int tabSize;

		public PrintTabExpander(int leftMargin, int tabSize)
		{
			this.leftMargin = leftMargin;
			this.tabSize = tabSize;
		}

		public float nextTabStop(float x, int tabOffset)
		{
			int ntabs = ((int)x - leftMargin) / tabSize;
			return (ntabs + 1) * tabSize + leftMargin;
		}
	}

	private void fireFoldLevelsChanged(int firstLine, int lastLine)
	{
		for(int i = 0; i < foldListeners.size(); i++)
		{
			((FoldListener)foldListeners.elementAt(i))
				.foldLevelsChanged(firstLine,lastLine);
		}
	}

	private void fireFoldStructureChanged()
	{
		for(int i = 0; i < foldListeners.size(); i++)
		{
			((FoldListener)foldListeners.elementAt(i))
				.foldStructureChanged();
		}
	}

	/**
	 * Only useful for the text area.
	 */
	public static interface FoldListener
	{
		void foldLevelsChanged(int firstLine, int lastLine);

		void foldStructureChanged();
	}

	/**
	 * Inner class for storing information about tokenized lines.
	 */
	public static class LineInfo
	{
		/**
		 * Do not use this variable. The only reason it is public
		 * is so that classes in the 'syntax' package can use it.
		 */
		public TokenMarker.LineContext context;

		/**
		 * Returns the first syntax token.
		 * @since jEdit 3.1pre1
		 */
		public Token getFirstToken()
		{
			return firstToken;
		}

		/**
		 * Returns the last syntax token.
		 * @since jEdit 3.1pre1
		 */
		public Token getLastToken()
		{
			return lastToken;
		}

		/**
		 * Do not call this method. The only reason it is public
		 * is so that classes in the 'syntax' package can call it.
		 */
		public void addToken(int length, byte id)
		{
			if(id >= Token.INTERNAL_FIRST && id <= Token.INTERNAL_LAST)
				throw new InternalError("Invalid id: " + id);

			if(length == 0 && id != Token.END)
				return;

			if(firstToken == null)
			{
				firstToken = new Token(length,id);
				lastToken = firstToken;
			}
			else if(lastToken == null)
			{
				lastToken = firstToken;
				firstToken.length = length;
				firstToken.id = id;
			}
			else if(lastToken.id == id)
			{
				lastToken.length += length;
			}
			else if(lastToken.next == null)
			{
				lastToken.next = new Token(length,id);
				lastToken.next.prev = lastToken;
				lastToken = lastToken.next;
			}
			else
			{
				lastToken = lastToken.next;
				lastToken.length = length;
				lastToken.id = id;
			}
		}

		// package-private members
		Token firstToken;
		Token lastToken;
		boolean tokensValid;
		int width;
		int foldLevel;
		boolean foldLevelValid;
		boolean visible;
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

	class SaveUndo implements UndoableEdit
	{
		boolean dead;

		public void undo()
		{
			//System.err.println("dirty false");
			//if(!dead)
			//	setDirty(false);
		}

		public boolean canUndo()
		{
			return true;
		}

		public void redo()
		{
			undo();
		}

		public boolean canRedo()
		{
			return true;
		}

		public boolean isSignificant()
		{
			return false;
		}

		public void die()
		{
			dead = true;
		}

		public boolean addEdit(UndoableEdit edit)
		{
			return false;
		}

		public boolean replaceEdit(UndoableEdit edit)
		{
			return false;
		}

		public String getPresentationName()
		{
			return null;
		}

		public String getUndoPresentationName()
		{
			return null;
		}

		public String getRedoPresentationName()
		{
			return null;
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
}
