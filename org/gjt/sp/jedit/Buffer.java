/*
 * Buffer.java - jEdit buffer
 * Copyright (C) 1998, 1999 Slava Pestov
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
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.syntax.*;
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
 * Various other properties are also used by jEdit and plugin actions.<p>
 *
 * To improve jEdit's perceived speed, buffers are not loaded from disk
 * until the user first interacts with them. Because of this, plugins and
 * other jEdit modules must call the <code>loadIfNecessary()</code> method
 * of a buffer before doing anything with it.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class Buffer extends SyntaxDocument implements EBComponent
{
	/**
	 * Size of I/O buffers.
	 */
	public static final int IOBUFSIZE = 32768;

	/**
	 * Line separator property.
	 */
	public static final String LINESEP = "lineSeparator";

	// caret info properties

	/**
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
		setFlag(SYNTAX,"on".equals(getProperty("syntax")));
	}

	/**
	 * Loads a buffer from disk if it is not loaded already.
	 * To make jEdit seem faster, files are not actually loaded
	 * disk when opened. They are only loaded when they are used
	 * for the first time.<p>
	 *
	 * This method must be called before doing anything with a buffer.
	 * @param view The view
	 *
	 * @since 2.2pre7
	 */
	public void loadIfNecessary(View view)
	{
		if(!getFlag(LOADED))
			load(view);
	}

	/**
	 * Loads the buffer from disk, even if it is loaded already.
	 * @param view The view
	 *
	 * @since 2.2pre7
	 */
	public void load(View view)
	{
		if(view != null)
			view.showWaitCursor();

		if(!getFlag(NEW_FILE))
		{
			// Only on initial load
			if(autosaveFile.exists() && !getFlag(LOADED))
			{
				Object[] args = { autosaveFile.getPath() };
				GUIUtilities.message(view,"autosaveexists",args);
			}
		}

		setFlag(LOADED,false);

		if(!getFlag(NEW_FILE))
		{
			read(view);
			readMarkers();
		}

		if(!getFlag(TEMPORARY))
		{
			setMode();
			propertiesChanged();
			EditBus.addToBus(this);
		}

		if(view != null)
			view.hideWaitCursor();

		setFlag(LOADED,true);
	}

	/**
	 * Autosaves this buffer.
	 */
	public void autosave()
	{
		if(getFlag(AUTOSAVE_DIRTY))
		{
			try
			{
				File autosaveTmp = new File(autosaveFile
					.getPath().concat("+tmp+#"));
				save(new FileOutputStream(autosaveTmp));
				/* workaround for JDK 1.2 bug */
				autosaveFile.delete();
				autosaveTmp.renameTo(autosaveFile);
				/* XXX race alert if setDirty() runs here */
				setFlag(AUTOSAVE_DIRTY,false);
			}
			catch(FileNotFoundException fnf)
			{
				/* this could happen if eg, the directory
				 * containing this file was renamed.
				 * we ignore the error then so the user
				 * isn't flooded with exceptions. */
				Log.log(Log.NOTICE,this,fnf);
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,this,e);
			}
		}
	}
	
	/**
	 * Prompts the user for a file to save this buffer to.
	 * @param view The view
	 */
	public boolean saveAs(View view)
	{
		String file = GUIUtilities.showFileDialog(view,getPath(),
			JFileChooser.SAVE_DIALOG);
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
	public boolean save(View view, String path)
	{
		// Do nothing if not loaded
		if(!getFlag(LOADED))
			return true;

		if(path == null && getFlag(NEW_FILE))
			return saveAs(view);

		boolean returnValue;
		URL saveUrl;
		File saveFile;
		if(path == null)
		{
			saveUrl = url;
			path = this.path;
			saveFile = file;
			// only do this check if saving to original file
			// if user is doing a `Save As' they should know
			// what they're doing anyway
			long newModTime = saveFile.lastModified();
			if(newModTime > modTime)
			{
				Object[] args = { path };
				int result = JOptionPane.showConfirmDialog(view,
					jEdit.getProperty("filechanged.message",
					args),jEdit.getProperty("filechanged.title"),
					JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE);
				if(result != JOptionPane.YES_OPTION)
					return false;
			}
		}
		else
		{
			try
			{
				saveUrl = new URL(path);
			}
			catch(MalformedURLException mu)
			{
				saveUrl = null;
			}
			saveFile = new File(path);
		}

		// A save is definately going to occur; show the wait cursor
		// and fire BUFFER_SAVING event
		if(view != null)
			view.showWaitCursor();

		EditBus.send(new BufferUpdate(this,BufferUpdate.SAVING));

		backup(view,saveFile);

		try
		{
			OutputStream out;
			if(saveUrl != null)
			{
				URLConnection connection = saveUrl
					.openConnection();
				out = connection.getOutputStream();
			}
			else
				out = new FileOutputStream(saveFile);
			if(path.endsWith(".gz"))
				out = new GZIPOutputStream(out);
			save(out);
			url = saveUrl;
			this.path = path;
			file = saveFile;
			setPath();
			saveMarkers();
			if(getFlag(NEW_FILE) || mode.getName().equals("text"))
				setMode();
			setFlag(AUTOSAVE_DIRTY,false);
			setFlag(READ_ONLY,false);
			setFlag(NEW_FILE,false);
			setFlag(DIRTY,false);
			EditBus.send(new BufferUpdate(this,BufferUpdate.DIRTY_CHANGED));

			autosaveFile.delete();
			modTime = file.lastModified();

			returnValue = true;
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,this,io);
			Object[] args = { io.toString() };
			GUIUtilities.error(view,"ioerror",args);
			returnValue = false;
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
			returnValue = false;
		}

		// Hide wait cursor
		if(view != null)
			view.hideWaitCursor();

		return returnValue;
	}

	/**
	 * Returns the URL this buffer is editing.
	 */
	public final URL getURL()
	{
		return url;
	}
	
	/**
	 * Returns the file this buffer is editing.
	 */
	public final File getFile()
	{
		return file;
	}

	/**
	 * Returns the autosave file for this buffer.
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
	 * Returns true if this is an untitled file, false otherwise.
	 */
	public final boolean isNewFile()
	{
		return getFlag(NEW_FILE);
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
	 * Sets the `dirty' (changed since last save) flag of this buffer.
	 */
	public void setDirty(boolean d)
	{
		boolean old_d = getFlag(DIRTY);

		if(d)
		{
			if(!getFlag(LOADED) || getFlag(READ_ONLY))
				return;
			if(getFlag(DIRTY) && getFlag(AUTOSAVE_DIRTY))
				return;
			setFlag(DIRTY,true);
			setFlag(AUTOSAVE_DIRTY,true);
		}
		else
			setFlag(DIRTY,false);

		if(d != old_d)
		{
			EditBus.send(new BufferUpdate(this,BufferUpdate.DIRTY_CHANGED));
			if(name.toLowerCase().endsWith(".macro"))
				Macros.loadMacros();
		}
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
	 * Undoes the most recent edit. Returns true if the undo was
	 * successful.
	 *
	 * @since jEdit 2.2pre1
	 */
	public boolean undo()
	{
		try
		{
			setFlag(UNDO_IN_PROGRESS,true);
			undo.undo();
			setFlag(UNDO_IN_PROGRESS,false);
			return true;
		}
		catch(CannotUndoException cu)
		{
			Log.log(Log.DEBUG,this,cu);
			return false;
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
		try
		{
			setFlag(UNDO_IN_PROGRESS,true);
			undo.redo();
			setFlag(UNDO_IN_PROGRESS,false);
			return true;
		}
		catch(CannotRedoException cr)
		{
			Log.log(Log.DEBUG,this,cr);
			return false;
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
		if(getFlag(UNDO_IN_PROGRESS))
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

		View[] views = jEdit.getViews();

		Mode oldMode = this.mode;
		if(oldMode != null)
		{
			for(int i = 0; i < views.length; i++)
			{
				View view = views[i];
				if(view.getBuffer() == this)
					oldMode.leaveView(view);
			}
			oldMode.leave(this);
		}

		this.mode = mode;

		mode.enter(this);

		setTokenMarker(mode.createTokenMarker());

		for(int i = 0; i < views.length; i++)
		{
			View view = views[i];
			if(view.getBuffer() == this)
				mode.enterView(view);
		}

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

			// Plugin modes will appear last in the list
			// (initPlugins() is called after initModes())
			// so we start from the end
			for(int i = modes.length - 1; i >= 0; i--)
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
	 * Returns the token marker for this buffer.
	 */
	public final TokenMarker getTokenMarker()
	{
		if(getFlag(SYNTAX))
			return super.getTokenMarker();
		else
			return null;
	}

	/**
	 * Returns an enumeration of set markers.
	 */
	public final Enumeration getMarkers()
	{
		return markers.elements();
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
		if(getFlag(LOADED))
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

		EditBus.send(new BufferUpdate(this,BufferUpdate.MARKERS_CHANGED));
	}

	/**
	 * Removes the marker with the specified name.
	 * @param name The name of the marker to remove
	 */
	public void removeMarker(String name)
	{
		setDirty(true);

loop:		for(int i = 0; i < markers.size(); i++)
		{
			Marker marker = (Marker)markers.elementAt(i);
			if(marker.getName().equals(name))
				markers.removeElementAt(i);
		}
		EditBus.send(new BufferUpdate(this,BufferUpdate.MARKERS_CHANGED));
	}
	
	/**
	 * Returns the marker with the specified name.
	 * @param name The marker name
	 */
	public Marker getMarker(String name)
	{
		Enumeration enum = getMarkers();
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

	Buffer(URL url, String path, boolean readOnly,
		boolean newFile, boolean temp)
	{
		this.url = url;
		this.path = path;
		setFlag(TEMPORARY,temp);
		setFlag(NEW_FILE,newFile);
		setFlag(READ_ONLY,readOnly);

		markers = new Vector();

		undo = new UndoManager();
		addDocumentListener(new DocumentHandler());
		addUndoableEditListener(new UndoHandler());

		setDocumentProperties(new BufferProps());
		putProperty("i18n",Boolean.FALSE);

		setPath();
	}

	void commitTemporary()
	{
		setFlag(TEMPORARY,false);
		if(getFlag(LOADED))
			EditBus.addToBus(this);
	}

	void close()
	{
		setFlag(CLOSED,true);
		autosaveFile.delete();

		// Unloaded buffers are not on the bus
		if(getFlag(LOADED))
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
	private static final int LOADED = 1;
	private static final int NEW_FILE = 2;
	private static final int AUTOSAVE_DIRTY = 3;
	private static final int DIRTY = 4;
	private static final int READ_ONLY = 5;
	private static final int BACKED_UP = 6;
	private static final int SYNTAX = 7;
	private static final int UNDO_IN_PROGRESS = 8;
	private static final int TEMPORARY = 9;

	private int flags;

	private File file;
	private long modTime;
	private File autosaveFile;
	private File markersFile;
	private URL url;
	private URL markersUrl;
	private String name;
	private String path;
	private Mode mode;
	private UndoManager undo;
	private CompoundEdit compoundEdit;
	private int compoundEditCount;
	private Vector markers;
	private int savedSelStart;
	private int savedSelEnd;

	private void setPath()
	{
		if(url == null)
		{
			file = new File(path);
			name = file.getName();
			markersFile = new File(file.getParent(),'.' + name
				+ ".marks");
		}
		else
		{
			name = url.getFile();
			if(name.length() == 0)
				name = url.getHost();
			file = new File(name);
			name = file.getName();
			try
			{
				markersUrl = new URL(url,'.' + new File(name)
					.getName() + ".marks");
			}
			catch(MalformedURLException mu)
			{
				markersUrl = null;
			}
		}
		// if we don't do this, the autosave of a file won't be
		// deleted after a save as
		if(autosaveFile != null)
			autosaveFile.delete();
		autosaveFile = new File(file.getParent(),'#' + name + '#');
	}

	/*
	 * The most complicated method in this class :-) Read and understand
	 * all these notes if you want to snarf this code for your own app;
	 * it has a number of subtle behaviours which are not entirely
	 * obvious.
	 *
	 * Some notes that will help future hackers:
	 * - We use a StringBuffer because there is no way to pre-allocate
	 *   in the GapContent - and adding text each time to the GapContent
	 *   would be slow because it would require array enlarging, etc.
	 *   Better to do as few gap inserts as possible.
	 *
	 * - The StringBuffer is pre-allocated to Math.max(fileSize,
	 *   IOBUFSIZE * 4) because when loading from URLs, fileSize is 0
	 *   and we don't want to StringBuffer to enlarge 1000000 times for
	 *   large URLs
	 *
	 * - We read the stream in IOBUFSIZE (= 32k) blocks, and loop over
	 *   the read characters looking for line breaks.
	 *   - a \r or \n causes a line to be added to the model, and appended
	 *     to the string buffer
	 *   - a \n immediately following an \r is ignored; so that Windows
	 *     line endings are handled
	 *
	 * - This method remembers the line separator used in the file, and
	 *   stores it in the lineSeparator buffer-local property. However,
	 *   if the file contains, say, hello\rworld\n, lineSeparator will
	 *   be set to \n, and the file will be saved as hello\nworld\n.
	 *   Hence jEdit is not really appropriate for editing binary files.
	 *
	 * - To make reloading a bit easier, this method automatically
	 *   removes all data from the model before inserting it. This
	 *   shouldn't cause any problems, as most documents will be
	 *   empty before being loaded into anyway.
	 *
	 * - If the last character read from the file is a line separator,
	 *   it is not added to the model! There are two reasons:
	 *   - On Unix, all text files have a line separator at the end,
	 *     there is no point wasting an empty screen line on that
	 *   - Because save() appends a line separator after *every* line,
	 *     it prevents the blank line count at the end from growing
	 */
	private void read(View view)
	{
		if(file.exists())
			setFlag(READ_ONLY,!file.canWrite());

		InputStream _in;
		URLConnection connection = null;
		StringBuffer sbuf = new StringBuffer(Math.max(
			(int)file.length(),IOBUFSIZE * 4));

		try
		{
			if(url != null)
				_in = url.openStream();
			else
				_in = new FileInputStream(file);
			if(name.endsWith(".gz"))
				_in = new GZIPInputStream(_in);
			InputStreamReader in = new InputStreamReader(_in,
				jEdit.getProperty("buffer.encoding",
				System.getProperty("file.encoding")));
			char[] buf = new char[IOBUFSIZE];
			// Number of characters in 'buf' array.
			// InputStream.read() doesn't always fill the
			// array (eg, the file size is not a multiple of
			// IOBUFSIZE, or it is a GZipped file, etc)
			int len;

			// True if a \n was read after a \r. Usually
			// means this is a DOS/Windows file
			boolean CRLF = false;

			// A \r was read, hence a MacOS file
			boolean CROnly = false;

			// Was the previous read character a \r?
			// If we read a \n and this is true, we assume
			// we have a DOS/Windows file
			boolean lastWasCR = false;
			
			while((len = in.read(buf,0,buf.length)) != -1)
			{
				// Offset of previous line, relative to
				// the start of the I/O buffer (NOT
				// relative to the start of the document)
				int lastLine = 0;

				for(int i = 0; i < len; i++)
				{
					// Look for line endings.
					switch(buf[i])
					{
					case '\r':
						// If we read a \r and
						// lastWasCR is also true,
						// it is probably a Mac file
						// (\r\r in stream)
						if(lastWasCR)
						{
							CROnly = true;
							CRLF = false;
						}
						// Otherwise set a flag,
						// so that \n knows that last
						// was a \r
						else
						{
							lastWasCR = true;
						}

						// Insert a line
						sbuf.append(buf,lastLine,i -
							lastLine);
						sbuf.append('\n');

						// This is i+1 to take the
						// trailing \n into account
						lastLine = i + 1;
						break;
					case '\n':
						// If lastWasCR is true,
						// we just read a \r followed
						// by a \n. We specify that
						// this is a Windows file,
						// but take no further
						// action and just ignore
						// the \r.
						if(lastWasCR)
						{
							CROnly = false;
							CRLF = true;
							lastWasCR = false;
							// Bump lastLine so
							// that the next line
							// doesn't erronously
							// pick up the \r
							lastLine = i + 1;
						}
						// Otherwise, we found a \n
						// that follows some other
						// character, hence we have
						// a Unix file
						else
						{
							CROnly = false;
							CRLF = false;
							sbuf.append(buf,lastLine,
								i - lastLine);
							sbuf.append('\n');
							lastLine = i + 1;
						}
						break;
					default:
						// If we find some other
						// character that follows
						// a \r, so it is not a
						// Windows file, and probably
						// a Mac file
						if(lastWasCR)
						{
							CROnly = true;
							CRLF = false;
							lastWasCR = false;
						}
						break;
					}
				}
				// Add remaining stuff from buffer
				sbuf.append(buf,lastLine,len - lastLine);
			}
			if(CRLF)
				putProperty(LINESEP,"\r\n");
			else if(CROnly)
				putProperty(LINESEP,"\r");
			else
				putProperty(LINESEP,"\n");
			in.close();

			// For `reload' command
			remove(0,getLength());

			// Chop trailing newline (if any)
			int length = sbuf.length();
			if(length != 0 && sbuf.charAt(length - 1) == '\n')
				sbuf.setLength(length - 1);

			insertString(0,sbuf.toString(),null);

			// Process buffer-local properties
			Element map = getDefaultRootElement();
			for(int i = 0; i < Math.min(10,map.getElementCount()); i++)
			{
				Element line = map.getElement(i);
				String text = getText(line.getStartOffset(),
					line.getEndOffset() - line.getStartOffset() - 1);
				processProperty(text);
			}

			setFlag(NEW_FILE,false);
			setFlag(READ_ONLY,false);
			setDirty(false);

			modTime = file.lastModified();

			// fire LOADING event
			EditBus.send(new BufferUpdate(this,BufferUpdate.LOADING));
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
		catch(FileNotFoundException fnf)
		{
			setFlag(NEW_FILE,true);
			Log.log(Log.NOTICE,this,fnf);
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,this,io);
			Object[] args = { io.toString() };
			GUIUtilities.error(view,"ioerror",args);
		}
	}

	private void processProperty(String prop)
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
						putProperty(name,new Integer(
							value));
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

	private void readMarkers()
	{
		// For `reload' command
		markers.removeAllElements();

		try
		{
			InputStream in;
			if(url != null)
				in = markersUrl.openStream();
			else
				in = new FileInputStream(markersFile);
			StringBuffer buf = new StringBuffer();
			int c;
			boolean eof = false;
			String name = null;
			int start = -1;
			int end = -1;
			for(;;)
			{
				if(eof)
					break;
				switch(c = in.read())
				{
				case -1:
					eof = true;
				case ';': case '\n': case '\r':
					if(buf.length() == 0)
						continue;
					String str = buf.toString();
					buf.setLength(0);
					if(name == null)
						name = str;
					else if(start == -1)
					{
						try
						{
							start = Integer
								.parseInt(str);
						}
						catch(NumberFormatException nf)
						{
							Log.log(Log.ERROR,this,nf);
							start = 0;
						}
					}
					else if(end == -1)
					{
						try
						{
							end = Integer
								.parseInt(str);
						}
						catch(NumberFormatException nf)
						{
							Log.log(Log.ERROR,this,nf);
							end = 0;
						}
						addMarker(name,start,end);
						name = null;
						start = -1;
						end = -1;
					}
					break;
				default:
					buf.append((char)c);
					break;
				}
			}
			in.close();
		}
		catch(Exception e)
		{
			//Log.log(Log.ERROR,this,e);
		}
	}

	// Saving is much simpler than loading :-)
	private void save(OutputStream _out)
		throws IOException, BadLocationException
	{
		BufferedWriter out = new BufferedWriter(
			new OutputStreamWriter(_out,
				jEdit.getProperty("buffer.encoding",
				System.getProperty("file.encoding"))),
				IOBUFSIZE);
		Segment lineSegment = new Segment();
		String newline = (String)getProperty(LINESEP);
		if(newline == null)
			newline = System.getProperty("line.separator");
		Element map = getDefaultRootElement();
		for(int i = 0; i < map.getElementCount(); i++)
		{
			Element line = map.getElement(i);
			int start = line.getStartOffset();
			getText(start,line.getEndOffset() - start - 1,
				lineSegment);
			out.write(lineSegment.array,lineSegment.offset,
				lineSegment.count);
			out.write(newline);
		}
		out.close();
	}
	
	private void saveMarkers()
		throws IOException
	{
		if(markers.isEmpty())
		{
			markersFile.delete();
			return;
		}
		OutputStream out;
		if(url != null)
		{
			URLConnection connection = markersUrl.openConnection();
			out = connection.getOutputStream();
		}
		else
			out = new FileOutputStream(markersFile);
		Writer o = new OutputStreamWriter(out);
		Enumeration enum = getMarkers();
		while(enum.hasMoreElements())
		{
			Marker marker = (Marker)enum.nextElement();
			o.write(marker.getName());
			o.write(';');
			o.write(String.valueOf(marker.getStart()));
			o.write(';');
			o.write(String.valueOf(marker.getEnd()));
			o.write('\n');
		}
		o.close();
	}

	// The BACKED_UP flag prevents more than one backup from being
	// written per session (I guess this should be made configurable
	// in the future)
	private void backup(View view, File file)
	{
		if(getFlag(BACKED_UP))
			return;
		setFlag(BACKED_UP,true);
		
		// Fetch properties
		int backups;
		try
		{
			backups = Integer.parseInt(jEdit.getProperty(
				"backups"));
		}
		catch(NumberFormatException nf)
		{
			Log.log(Log.ERROR,this,nf);
			backups = 1;
		}

		if(backups == 0)
			return;

		String backupPrefix = jEdit.getProperty("backup.prefix","");
		String backupSuffix = jEdit.getProperty("backup.suffix","~");

		// Check for backup.directory property, and create that
		// directory if it doesn't exist
		String backupDirectory = jEdit.getProperty("backup.directory");
		if(backupDirectory == null || backupDirectory.length() == 0)
			backupDirectory = file.getParent();
		else
		{
			backupDirectory = MiscUtilities.constructPath(
				System.getProperty("user.home"),backupDirectory);
			new File(backupDirectory).mkdirs();
		}
		
		String name = file.getName();

		boolean ok = true;

		// If backups is 1, create ~ file
		if(backups == 1)
		{
			ok &= file.renameTo(new File(backupDirectory,
				backupPrefix + name + backupSuffix));
		}
		// If backups > 1, move old ~n~ files, create ~1~ file
		else
		{
			new File(backupDirectory,
				backupPrefix + name + backupSuffix
				+ backups + backupSuffix).delete();

			for(int i = backups - 1; i > 0; i--)
			{
				File backup = new File(backupDirectory,
					backupPrefix + name + backupSuffix
					+ i + backupSuffix);

				ok &= backup.renameTo(new File(backupDirectory,
					backupPrefix + name + backupSuffix
					+ (i+1) + backupSuffix));
			}

			ok &= file.renameTo(new File(backupDirectory,
				backupPrefix + name + backupSuffix
				+ "1" + backupSuffix));
		}

		/*if(!ok)
		{
			String[] args = { file.getPath() };
			GUIUtilities.error(view,"backup-failed",args);
		}*/
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
			{
				o = mode.getProperty((String)key);
				if(o != null)
					return o;
			}

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
 * Revision 1.115  1999/12/11 06:34:39  sp
 * Bug fixes
 *
 * Revision 1.114  1999/12/10 03:22:46  sp
 * Bug fixes, old loading code is now used again
 *
 * Revision 1.113  1999/12/07 08:16:55  sp
 * Reload bug nailed to the wall
 *
 * Revision 1.112  1999/12/07 07:19:36  sp
 * Buffer loading code cleaned up
 *
 * Revision 1.111  1999/12/07 06:30:48  sp
 * Compile errors fixed, new 'new view' icon
 *
 * Revision 1.110  1999/12/05 03:01:05  sp
 * Perl token marker bug fix, file loading is deferred, style option pane fix
 *
 * Revision 1.109  1999/12/03 23:48:10  sp
 * C+END/C+HOME, LOADING BufferUpdate message, misc stuff
 *
 * Revision 1.108  1999/11/29 02:45:50  sp
 * Scroll bar position saved when switching buffers
 *
 * Revision 1.107  1999/11/28 00:33:06  sp
 * Faster directory search, actions slimmed down, faster exit/close-all
 *
 * Revision 1.106  1999/11/27 06:01:20  sp
 * Faster file loading, geometry fix
 *
 * Revision 1.105  1999/11/19 08:54:51  sp
 * EditBus integrated into the core, event system gone, bug fixes
 *
 */
