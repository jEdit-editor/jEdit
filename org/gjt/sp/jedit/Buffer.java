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
import org.gjt.sp.jedit.event.*;
import org.gjt.sp.jedit.syntax.*;

/**
 * An in-memory copy of an open file.
 * @author Slava Pestov
 * @version $Id$
 */
public class Buffer extends SyntaxDocument
{
	/**
	 * Size of I/O buffers.
	 */
	public static final int IOBUFSIZE = 32768;

	/**
	 * Line separator property.
	 */
	public static final String LINESEP = "lineSeparator";

	/**
	 * Reloads settings from the properties. This should be called
	 * after the <code>syntax</code> buffer-local property is
	 * changed.
	 */
	public void propertiesChanged()
	{
		setFlag(SYNTAX,"on".equals(getProperty("syntax")));
		Integer tabSize = (Integer)mode.getProperty("tabSize");
		if(tabSize != null)
			putProperty("tabSize",tabSize);
		else
			putProperty("tabSize",new Integer(jEdit.getProperty(
				"buffer.tabSize")));
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
			}
			catch(Exception e)
			{
				System.err.println("Error during autosave:");
				e.printStackTrace();
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

		fireBufferEvent(BufferEvent.BUFFER_SAVING);

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
			setFlag(DIRTY,false);
			setFlag(READ_ONLY,false);
			setFlag(NEW_FILE,false);

			autosaveFile.delete();
			modTime = file.lastModified();

			fireBufferEvent(BufferEvent.DIRTY_CHANGED);

			returnValue = true;
		}
		catch(IOException io)
		{
			Object[] args = { io.toString() };
			GUIUtilities.error(view,"ioerror",args);
			returnValue = false;
		}
		catch(BadLocationException bl)
		{
			bl.printStackTrace();
			returnValue = false;
		}

		// Hide wait cursor
		if(view != null)
			view.hideWaitCursor();

		return returnValue;
	}
	
	/**
	 * Reloads the buffer from disk.
	 * @param view The view that will be used to display error dialogs, etc
	 */
	public void reload(View view)
	{
		// Show the wait cursor
		if(view != null)
			view.showWaitCursor();

		// Delete the autosave
		autosaveFile.delete();

		// This is so that `dirty' isn't set
		setFlag(INIT,true);

		load(view);
		loadMarkers();

		// Maybe incorrect? Anyway people won't notice
		setMode();

		tokenizeLines();

		// Clear dirty flag
		setFlag(DIRTY,false);

		// Hide the wait cursor
		if(view != null)
			view.hideWaitCursor();

		fireBufferEvent(BufferEvent.DIRTY_CHANGED);

		setFlag(INIT,false);
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
		if(d)
		{
			if(getFlag(INIT) || getFlag(READ_ONLY))
				return;
			if(getFlag(DIRTY) && getFlag(AUTOSAVE_DIRTY))
				return;
			setFlag(DIRTY,true);
			setFlag(AUTOSAVE_DIRTY,true);
		}
		else
			setFlag(DIRTY,false);
		fireBufferEvent(BufferEvent.DIRTY_CHANGED);
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
			return;

		View[] views = jEdit.getViews();

		if(this.mode != null)
		{
			for(int i = 0; i < views.length; i++)
			{
				View view = views[i];
				if(view.getBuffer() == this)
					this.mode.leaveView(view);
			}
			this.mode.leave(this);
		}

		this.mode = mode;

		mode.enter(this);

		setTokenMarker(mode.createTokenMarker());

		for(int i = 0; i < views.length; i++)
		{
			View view = views[i];
			if(view.getBuffer() == this)
				this.mode.enterView(view);
		}

		fireBufferEvent(BufferEvent.MODE_CHANGED);
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
		int start = lineElement.getStartOffset();
		try
		{
			String line = getText(start,lineElement.getEndOffset()
				- start - 1);

			Mode[] modes = jEdit.getModes();

			// Plugin modes will appear first in the list
			// (initPlugins() is called before initModes())
			// so we start at 0
			for(int i = 0; i < modes.length; i++)
			{
				if(modes[i].accept(this,nogzName,line))
				{
					setMode(modes[i]);
					break;
				}
			}
		}
		catch(BadLocationException bl)
		{
			bl.printStackTrace();
		}
	}

	/**
	 * Returns the token marker for this buffer.
	 */
	public final TokenMarker getTokenMarker()
	{
		if(getFlag(SYNTAX))
			return tokenMarker;
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
			return;
		}
		boolean added = false;
		if(!getFlag(INIT))
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

		fireBufferEvent(BufferEvent.MARKERS_CHANGED);
		fireBufferEvent(BufferEvent.DIRTY_CHANGED);
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
		fireBufferEvent(BufferEvent.MARKERS_CHANGED);
		fireBufferEvent(BufferEvent.DIRTY_CHANGED);
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
	 * Returns the saved selection start.
	 */
	public final int getSavedSelStart()
	{
		return savedSelStart;
	}

	/**
	 * Returns the saved selection end.
	 */
	public final int getSavedSelEnd()
	{
		return savedSelEnd;
	}

	/**
	 * Returns the saved rectangular selection flag.
	 */
	public final boolean isSelectionRectangular()
	{
		return getFlag(RECT_SELECT);
	}
	/**
	 * Adds a buffer event listener to this buffer.
	 * @param listener The event listener
	 */
	public final void addBufferListener(BufferListener listener)
	{
		listenerList.add(BufferListener.class,listener);
	}

	/**	
	 * Removes a buffer event listener from this buffer.
	 * @param listener The event listener
	 */
	public final void removeBufferListener(BufferListener listener)
	{
		listenerList.remove(BufferListener.class,listener);
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

	// package-private members
	Buffer prev;
	Buffer next;

	Buffer(View view, URL url, String path, boolean readOnly, boolean newFile)
	{
		setFlag(INIT,true);

		this.url = url;
		this.path = path;
		setFlag(NEW_FILE,newFile);
		setFlag(READ_ONLY,readOnly);

		setDocumentProperties(new BufferProps());
		putProperty("i18n",Boolean.FALSE);
		
		listenerList = new EventListenerList();
		undo = new UndoManager();
		markers = new Vector();
		addDocumentListener(new DocumentHandler());

		setPath();

		// Set default mode
		setMode(jEdit.getMode(jEdit.getProperty("buffer.defaultMode")));

		// Load syntax property
		propertiesChanged();

		if(!newFile)
		{
			if(file.exists())
				setFlag(READ_ONLY,!file.canWrite());
			if(autosaveFile.exists())
			{
				Object[] args = { autosaveFile.getPath() };
				GUIUtilities.message(view,"autosaveexists",args);
			}
			load(view);
			loadMarkers();
		}

		setMode();

		addUndoableEditListener(new UndoHandler());
		jEdit.addEditorListener(editorHandler = new EditorHandler());
		
		setFlag(INIT,false);
	}

	void close()
	{
		setFlag(CLOSED,true);
		autosaveFile.delete();
		jEdit.removeEditorListener(editorHandler);
	}

	void setCaretInfo(int savedSelStart, int savedSelEnd,
		boolean rectSelect)
	{
		this.savedSelStart = savedSelStart;
		this.savedSelEnd = savedSelEnd;
		setFlag(RECT_SELECT,rectSelect);
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
	private static final int INIT = 1;
	private static final int NEW_FILE = 2;
	private static final int AUTOSAVE_DIRTY = 3;
	private static final int DIRTY = 4;
	private static final int READ_ONLY = 5;
	private static final int BACKED_UP = 6;
	private static final int SYNTAX = 7;
	private static final int UNDO_IN_PROGRESS = 8;
	private static final int RECT_SELECT = 9;

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
	private EditorHandler editorHandler;
	private EventListenerList listenerList;

	private void fireBufferEvent(int id)
	{
		BufferEvent evt = null;
		Object[] listeners = listenerList.getListenerList();
		for(int i = listeners.length - 2; i >= 0; i-= 2)
		{
			if(listeners[i] == BufferListener.class)
			{
				if(evt == null)
					evt = new BufferEvent(id,this);
				evt.fire((BufferListener)listeners[i+1]);
			}
		}
	}

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
	
	private void load(View view)
	{
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
			InputStreamReader in = new InputStreamReader(_in);
			char[] buf = new char[IOBUFSIZE];
			int len; // Number of characters in buffer
			int lineCount = 0;
			boolean CRLF = false; // Windows line endings
			boolean CROnly = false; // MacOS line endings
			boolean lastWasCR = false; // Was the previous character CR?
			
			while((len = in.read(buf,0,buf.length)) != -1)
			{
				int lastLine = 0; // Offset of last line
				for(int i = 0; i < len; i++)
				{
					switch(buf[i])
					{
					case '\r':
						if(lastWasCR) // \r\r, probably Mac
						{
							CROnly = true;
							CRLF = false;
						}
						else
						{
							lastWasCR = true;
						}
						sbuf.append(buf,lastLine,i -
							lastLine);
						sbuf.append('\n');
						lastLine = i + 1;
						break;
					case '\n':
						if(lastWasCR) // \r\n, probably DOS
						{
							CROnly = false;
							CRLF = true;
							lastWasCR = false;
							lastLine = i + 1;
						}
						else // Unix
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
						if(lastWasCR)
						{
							CROnly = true;
							CRLF = false;
							lastWasCR = false;
						}
						break;
					}
				}
				sbuf.append(buf,lastLine,len - lastLine);
			}
			if(CRLF)
				putProperty(LINESEP,"\r\n");
			else if(CROnly)
				putProperty(LINESEP,"\r");
			else
				putProperty(LINESEP,"\n");
			in.close();
                        if(sbuf.length() != 0 && sbuf.charAt(sbuf.length() - 1) == '\n')
				sbuf.setLength(sbuf.length() - 1);

			// For `reload' command
			remove(0,getLength());

			insertString(0,sbuf.toString(),null);
			setFlag(NEW_FILE,false);
			modTime = file.lastModified();

			// One day, we should interleave this code into
			// the above loop for top hat efficency. But for
			// now, this will suffice.
			Element map = getDefaultRootElement();
			for(int i = 0; i < 10; i++)
			{
				Element lineElement = map.getElement(i);
				if(lineElement == null)
					break;
				String line = getText(lineElement.getStartOffset(),
					lineElement.getEndOffset()
					- lineElement.getStartOffset());
				processProperty(line);
			}
		}
		catch(BadLocationException bl)
		{
			bl.printStackTrace();
		}
		catch(FileNotFoundException fnf)
		{
			setFlag(NEW_FILE,true);
		}
		catch(IOException io)
		{
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

	private void loadMarkers()
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
		}
	}

	private void save(OutputStream _out)
		throws IOException, BadLocationException
	{
		BufferedWriter out = new BufferedWriter(
			new OutputStreamWriter(_out),IOBUFSIZE);
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

		if(!ok)
		{
			String[] args = { file.getPath() };
			GUIUtilities.error(view,"backup-failed",args);
		}
	}

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
			o = mode.getProperty((String)key);
			if(o != null)
				return o;

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
	class EditorHandler
	extends EditorAdapter
	{
		public void propertiesChanged(EditorEvent evt)
		{
			Buffer.this.propertiesChanged();
		}
	}

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
 * Revision 1.99  1999/10/30 02:44:18  sp
 * Miscallaneous stuffs
 *
 * Revision 1.98  1999/10/24 02:06:40  sp
 * Miscallaneous pre1 stuff
 *
 * Revision 1.97  1999/10/23 03:48:22  sp
 * Mode system overhaul, close all dialog box, misc other stuff
 *
 * Revision 1.96  1999/10/16 09:43:00  sp
 * Final tweaking and polishing for jEdit 2.1final
 *
 * Revision 1.95  1999/10/03 03:47:15  sp
 * Minor stupidity, IDL mode
 *
 * Revision 1.94  1999/10/01 07:31:39  sp
 * RMI server replaced with socket-based server, minor changes
 *
 * Revision 1.93  1999/09/30 12:21:04  sp
 * No net access for a month... so here's one big jEdit 2.1pre1
 *
 * Revision 1.92  1999/08/21 01:48:18  sp
 * jEdit 2.0pre8
 *
 * Revision 1.91  1999/07/16 23:45:48  sp
 * 1.7pre6 BugFree version
 *
 * Revision 1.90  1999/07/08 06:06:04  sp
 * Bug fixes and miscallaneous updates
 *
 * Revision 1.89  1999/07/05 04:38:39  sp
 * Massive batch of changes... bug fixes, also new text component is in place.
 * Have fun
 *
 * Revision 1.88  1999/06/22 06:14:39  sp
 * RMI updates, text area updates, flag to disable geometry saving
 */
