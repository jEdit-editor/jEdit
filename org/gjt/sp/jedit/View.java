/*
 * View.java - jEdit view
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

import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import org.gjt.sp.jedit.event.*;
import org.gjt.sp.jedit.syntax.SyntaxStyle;
import org.gjt.sp.jedit.syntax.Token;
import org.gjt.sp.jedit.textarea.*;

/**
 * A window that edits buffers. There is no public constructor in the
 * View class. Views are created and destroyed by the <code>jEdit</code>
 * class.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class View extends JFrame
{
	/**
	 * Reloads various settings from the properties.
	 */
	public void propertiesChanged()
	{
		if("on".equals(jEdit.getProperty("view.showToolbar")))
		{
			if(toolBar == null)
				toolBar = GUIUtilities.loadToolBar("view.toolbar");
			if(toolBar.getParent() == null)
				addToolBar(toolBar);
		}
		else if(toolBar != null)
		{
			removeToolBar(toolBar);
			toolBar = null;
			validate();
		}

		showFullPath = "on".equals(jEdit.getProperty("view.showFullPath"));

		String family = jEdit.getProperty("view.font");
		int size;
		try
		{
			size = Integer.parseInt(jEdit.getProperty(
				"view.fontsize"));
		}
		catch(NumberFormatException nf)
		{
			size = 14;
		}
		int style;
		try
		{
			style = Integer.parseInt(jEdit.getProperty(
				"view.fontstyle"));
		}
		catch(NumberFormatException nf)
		{
			style = Font.PLAIN;
		}
		Font font = new Font(family,style,size);

		TextAreaPainter painter = textArea.getPainter();

		painter.setFont(font);
		painter.setLineHighlightEnabled("on".equals(jEdit.getProperty(
			"view.lineHighlight")));
		painter.setLineHighlightColor(GUIUtilities.parseColor(
			jEdit.getProperty("view.lineHighlightColor")));
		painter.setBracketHighlightEnabled("on".equals(jEdit.getProperty(
			"view.bracketHighlight")));
		painter.setBracketHighlightColor(GUIUtilities.parseColor(
			jEdit.getProperty("view.bracketHighlightColor")));
		painter.setEOLMarkerEnabled("on".equals(jEdit.getProperty(
			"view.eolMarkers")));
		painter.setEOLMarkerColor(GUIUtilities.parseColor(
			jEdit.getProperty("view.eolMarkerColor")));
		painter.setCaretColor(GUIUtilities.parseColor(
			jEdit.getProperty("view.caretColor")));
		painter.setSelectionColor(GUIUtilities.parseColor(
			jEdit.getProperty("view.selectionColor")));
		painter.setBackground(GUIUtilities.parseColor(
			jEdit.getProperty("view.bgColor")));
		painter.setForeground(GUIUtilities.parseColor(
			jEdit.getProperty("view.fgColor")));
		painter.setBlockCaretEnabled("on".equals(jEdit.getProperty(
			"view.blockCaret")));

		textArea.setCaretBlinkEnabled("on".equals(jEdit.getProperty(
			"view.caretBlink")));

		try
		{
			textArea.setElectricScroll(Integer.parseInt(jEdit
				.getProperty("view.electricBorders")));
		}
		catch(NumberFormatException nf)
		{
			textArea.setElectricScroll(0);
		}

		loadStyles();

		updateOpenRecentMenu();
	}
	
	/**
	 * Recreates the buffers menu.
	 */
	public void updateBuffersMenu()
	{
		if(buffers.getMenuComponentCount() != 0)
			buffers.removeAll();

		buffers.add(GUIUtilities.loadMenuItem(this,"prev-buffer"));
		buffers.add(GUIUtilities.loadMenuItem(this,"next-buffer"));
		buffers.addSeparator();

		ButtonGroup grp = new ButtonGroup();
		Buffer[] bufferArray = jEdit.getBuffers();
		for(int i = 0; i < bufferArray.length; i++)
		{
			Buffer b = bufferArray[i];
			String name = b.getPath();
			Object[] args = { name,
				new Integer(b.isReadOnly() ? 1: 0),
				new Integer(b.isDirty() ? 1 : 0),
				new Integer(b.isNewFile() ? 1 : 0) };
			JRadioButtonMenuItem menuItem =
				new JRadioButtonMenuItem(jEdit.getProperty(
					"view.title",args));
			menuItem.addActionListener(jEdit.getAction("select-buffer"));
			grp.add(menuItem);
			menuItem.setActionCommand(name);
			if(buffer == b)
				menuItem.getModel().setSelected(true);
			buffers.add(menuItem);
		}
	}
	
	/**
	 * Recreates the open recent menu.
	 */
	public void updateOpenRecentMenu()
	{
		if(openRecent.getMenuComponentCount() != 0)
			openRecent.removeAll();
		EditAction action = jEdit.getAction("open-path");
		String[] recentArray = jEdit.getRecent();
		if(recentArray.length == 0)
		{
			openRecent.add(GUIUtilities.loadMenuItem(this,"no-recent"));
			return;
		}
		for(int i = 0; i < recentArray.length; i++)
		{
			String path = recentArray[i];
			JMenuItem menuItem = new JMenuItem(path);
			menuItem.setActionCommand(path);
			menuItem.addActionListener(action);
			openRecent.add(menuItem);
		}
	}
	
	/**
	 * Recreates the goto marker and clear marker menus.
	 */
	public void updateMarkerMenus()
	{
		if(clearMarker.getMenuComponentCount() != 0)
			clearMarker.removeAll();
		if(gotoMarker.getMenuComponentCount() != 0)
			gotoMarker.removeAll();
		EditAction clearMarkerAction = jEdit.getAction("clear-marker");
		EditAction gotoMarkerAction = jEdit.getAction("goto-marker");
		Enumeration enum = buffer.getMarkers();
		if(!enum.hasMoreElements())
		{
			clearMarker.add(GUIUtilities.loadMenuItem(this,"no-markers"));
			gotoMarker.add(GUIUtilities.loadMenuItem(this,"no-markers"));
			return;
		}
		while(enum.hasMoreElements())
		{
			String name = ((Marker)enum.nextElement())
				.getName();
			JMenuItem menuItem = new JMenuItem(name);
			menuItem.setActionCommand(name);
			menuItem.addActionListener(clearMarkerAction);
			clearMarker.add(menuItem);
			menuItem = new JMenuItem(name);
			menuItem.setActionCommand(name);
			menuItem.addActionListener(gotoMarkerAction);
			gotoMarker.add(menuItem);
		}
	}

	/**
	 * Recreates the macros menu.
	 */
	public void updateMacrosMenu()
	{
		// Because the macros menu contains normal items as
		// well as dynamically-generated stuff, we are careful
		// to only remove the dynamic crap here...
		for(int i = macros.getMenuComponentCount() - 1; i >= 0; i--)
		{
			if(macros.getMenuComponent(i) instanceof JSeparator)
				break;
			else
				macros.remove(i);
		}

		String settings = jEdit.getSettingsDirectory();

		boolean noMacros = true;

		if(settings != null)
		{
			EditAction action = jEdit.getAction("play-macro");

			String[] macroFiles = new File(settings +
				File.separator + "macros").list();
			for(int i = 0; i < macroFiles.length; i++)
			{
				String name = macroFiles[i];
				if(!name.toLowerCase().endsWith(".macro"))
					continue;

				noMacros = false;

				name = name.substring(0,name.length() - 6);
				JMenuItem menuItem = new JMenuItem(name);
				menuItem.addActionListener(action);
				menuItem.setActionCommand(name);
				macros.add(menuItem);
			}
		}

		if(noMacros)
			macros.add(GUIUtilities.loadMenuItem(this,"no-macros"));
	}

	/**
	 * Recreates the plugins menu.
	 */
	public void updatePluginsMenu()
	{
		if(plugins.getMenuComponentCount() != 0)
			plugins.removeAll();

		// Query plugins for menu items
		Vector pluginMenus = new Vector();
		Vector pluginMenuItems = new Vector();

		EditPlugin[] pluginArray = jEdit.getPlugins();
		for(int i = 0; i < pluginArray.length; i++)
			pluginArray[i].createMenuItems(this,pluginMenus,pluginMenuItems);

		if(pluginMenus.isEmpty() && pluginMenuItems.isEmpty())
		{
			plugins.add(GUIUtilities.loadMenuItem(this,"no-plugins"));
			return;
		}

		// Sort them
		MenuItemCompare comp = new MenuItemCompare();
		MiscUtilities.quicksort(pluginMenus,comp);
		MiscUtilities.quicksort(pluginMenuItems,comp);

		for(int i = 0; i < pluginMenus.size(); i++)
			plugins.add((JMenu)pluginMenus.elementAt(i));

		if(!pluginMenus.isEmpty() && !pluginMenuItems.isEmpty())
			plugins.addSeparator();

		for(int i = 0; i < pluginMenuItems.size(); i++)
			plugins.add((JMenuItem)pluginMenuItems.elementAt(i));
	}

	private static class MenuItemCompare implements MiscUtilities.Compare
	{
		public int compare(Object obj1, Object obj2)
		{
			return ((JMenuItem)obj1).getText().compareTo(
				((JMenuItem)obj2).getText());
		}
	}

	/**
	 * Displays the specified string in the status area of this view.
	 * @param str The string to display
	 */
	public void showStatus(String str)
	{
		status.showStatus(str);
	}

	/**
	 * Updates the title bar and read only status of the text
	 * area.
	 */
	public void updateTitle()
	{
		Object[] args = { ((showFullPath && !buffer.isNewFile())
			? buffer.getPath() : buffer.getName()),
			new Integer(buffer.isReadOnly() ? 1 : 0),
			new Integer(buffer.isDirty() ? 1: 0),
			new Integer(buffer.isNewFile() ? 1: 0)};
		setTitle(jEdit.getProperty("view.title",args));
		textArea.setEditable(!buffer.isReadOnly());
	}

	/**
	 * Returns the buffer being edited by this view.
	 */
	public final Buffer getBuffer()
	{
		return buffer;
	}

	/**
	 * Sets the buffer being edited by this view, without updating
	 * the `Buffers' menu. This is for internal use by the
	 * select-buffer action only, don't call this method in other
	 * situations.
	 * @param buffer The buffer to edit.
	 */
	public void _setBuffer(Buffer buffer)
	{
		if(this.buffer == buffer)
			return;

		if(buffer == null)
			throw new NullPointerException("Not supported yet");

		Buffer oldBuffer = this.buffer;
		if(oldBuffer != null)
		{
			saveCaretInfo();
			Mode mode = oldBuffer.getMode();
			if(mode != null)
				mode.leaveView(this);
		}
		this.buffer = buffer;

		textArea.setDocument(buffer);

		int start = Math.min(buffer.getLength(),buffer.getSavedSelStart());
		int end = Math.min(buffer.getLength(),buffer.getSavedSelEnd());
		textArea.select(start,end);
		textArea.setSelectionRectangular(buffer.isSelectionRectangular());

		updateMarkerMenus();
		updateTitle();
		status.repaint();

		Mode mode = buffer.getMode();
		if(mode != null)
			mode.enterView(this);

		focusOnTextArea();

		// Fire event
		fireViewEvent(ViewEvent.BUFFER_CHANGED,oldBuffer);
	}

	/**
	 * Sets the buffer being edited by this view.
	 * @param buffer The buffer to edit.
	 */
	public final void setBuffer(Buffer buffer)
	{
		_setBuffer(buffer);
		updateBuffersMenu();
	}

	/**
	 * Sets the focus onto the text area.
	 */
	public void focusOnTextArea()
	{
		textArea.requestFocus();
	}

	/**
	 * Returns this view's text area.
	 */
	public final JEditTextArea getTextArea()
	{
		return textArea;
	}

	/**
	 * Adds a tool bar to this view.
	 * @param toolBar The tool bar
	 */
	public void addToolBar(Component toolBar)
	{
		toolBars.add(toolBar);
		invalidate();
		validate();
	}

	/**
	 * Removes a tool bar from this view.
	 * @param toolBar The tool bar
	 */
	public void removeToolBar(Component toolBar)
	{
		toolBars.remove(toolBar);
	}

	/**
	 * Saves the caret information to the current buffer.
	 */
	public void saveCaretInfo()
	{
		buffer.setCaretInfo(
			textArea.getSelectionStart(),
			textArea.getSelectionEnd(),
			textArea.isSelectionRectangular());
	}

	/**
	 * Returns true if this view has been closed with
	 * <code>jEdit.closeView()</code>.
	 */
	public final boolean isClosed()
	{
		return closed;
	}

	/**
	 * Adds a view event listener to this view.
	 * @param listener The event listener
	 */
	public final void addViewListener(ViewListener listener)
	{
		listenerList.add(ViewListener.class,listener);
	}

	/**	
	 * Removes a view event listener from this view.
	 * @param listener The event listener
	 */
	public final void removeViewListener(ViewListener listener)
	{
		listenerList.remove(ViewListener.class,listener);
	}

	/**
	 * Returns the next view in the list.
	 */
	public final View getNext()
	{
		return next;
	}

	/**
	 * Returns the previous view in the list.
	 */
	public final View getPrev()
	{
		return prev;
	}

	// package-private members
	View prev;
	View next;

	View(View view, Buffer buffer)
	{
		listenerList = new EventListenerList();

		// Dynamic menus
		buffers = GUIUtilities.loadMenu(this,"buffers");
		openRecent = GUIUtilities.loadMenu(this,"open-recent");
		clearMarker = GUIUtilities.loadMenu(this,"clear-marker");
		gotoMarker = GUIUtilities.loadMenu(this,"goto-marker");
		macros = GUIUtilities.loadMenu(this,"macros");
		updateMacrosMenu();
		plugins = GUIUtilities.loadMenu(this,"plugins");
		updatePluginsMenu();

		jEdit.addEditorListener(editorListener = new EditorHandler());
		bufferListener = new BufferHandler();

		Buffer[] bufferArray = jEdit.getBuffers();
		for(int i = 0; i < bufferArray.length; i++)
		{
			bufferArray[i].addBufferListener(bufferListener);
		}

		setJMenuBar(GUIUtilities.loadMenubar(this,"view.mbar"));

		toolBars = new Box(BoxLayout.Y_AXIS);

		textArea = new JEditTextArea();

		// Add the line number display
		textArea.add(JEditTextArea.LEFT_OF_SCROLLBAR,status = new StatusBar());

		// Set up the right-click popup menu
		textArea.setRightClickPopup(GUIUtilities
			.loadPopupMenu(this,"view.context"));

		textArea.setInputHandler(jEdit.getInputHandler().copy());
		textArea.addCaretListener(new CaretHandler());

		propertiesChanged();

		if(buffer == null)
			setBuffer(bufferArray[bufferArray.length - 1]);
		else
			setBuffer(buffer);

		getContentPane().add(BorderLayout.NORTH,toolBars);
		getContentPane().add(BorderLayout.CENTER,textArea);

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
	}

	JMenu getMenu(String name)
	{
		if(name.equals("buffers"))
			return buffers;
		else if(name.equals("open-recent"))
			return openRecent;
		else if(name.equals("clear-marker"))
			return clearMarker;
		else if(name.equals("goto-marker"))
			return gotoMarker;
		else if(name.equals("macros"))
			return macros;
		else if(name.equals("plugins"))
			return plugins;
		else
			return null;
	}
	
	void close()
	{
		closed = true;
		GUIUtilities.saveGeometry(this,"view");
		saveCaretInfo();
		jEdit.removeEditorListener(editorListener);
		Buffer[] bufferArray = jEdit.getBuffers();
		for(int i = 0; i < bufferArray.length; i++)
			bufferArray[i].removeBufferListener(bufferListener);
		dispose();
	}

	// private members
	private Buffer buffer;
	private boolean closed;

	private JMenu buffers;
	private JMenu openRecent;
	private JMenu clearMarker;
	private JMenu gotoMarker;
	private JMenu macros;
	private JMenu plugins;

	private Box toolBars;
	private Component toolBar;
	private JEditTextArea textArea;
	private StatusBar status;

	private boolean showFullPath;

	private EventListenerList listenerList;
	private BufferListener bufferListener;
	private EditorListener editorListener;

	private void fireViewEvent(int id, Buffer buffer)
	{
		ViewEvent evt = null;
		Object[] listeners = listenerList.getListenerList();
		for(int i = listeners.length - 2; i >= 0; i-= 2)
		{
			if(listeners[i] == ViewListener.class)
			{
				if(evt == null)
					evt = new ViewEvent(id,this,buffer);
				evt.fire((ViewListener)listeners[i+1]);
			}
		}
	}

	private void loadStyles()
	{
		try
		{
			SyntaxStyle[] styles = new SyntaxStyle[Token.ID_COUNT];

			styles[Token.COMMENT1] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.comment1"));
			styles[Token.COMMENT2] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.comment2"));
			styles[Token.KEYWORD1] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.keyword1"));
			styles[Token.KEYWORD2] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.keyword2"));
			styles[Token.KEYWORD3] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.keyword3"));
			styles[Token.LABEL] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.label"));
			styles[Token.LITERAL1] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.literal1"));
			styles[Token.LITERAL2] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.literal2"));
			styles[Token.OPERATOR] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.operator"));
			styles[Token.INVALID] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.invalid"));

			textArea.getPainter().setStyles(styles);
		}
		catch(Exception e)
		{
			System.out.println("Error loading syntax styles:");
			e.printStackTrace();
		}
	}

	// event listeners
	class BufferHandler extends BufferAdapter
	{
		public void bufferDirtyChanged(BufferEvent evt)
		{
			Buffer _buffer = evt.getBuffer();
			if(_buffer == buffer)
				updateTitle();

			// Check if it's a macro that's just been saved
			if(_buffer.getName().toLowerCase().endsWith(".macro")
				&& !_buffer.isDirty())
			{
				// Then update the macros menu
				updateMacrosMenu();
			}

			updateBuffersMenu();
		}

		public void bufferMarkersChanged(BufferEvent evt)
		{
			if(evt.getBuffer() == buffer)
				updateMarkerMenus();
		}

		public void bufferModeChanged(BufferEvent evt)
		{
			if(evt.getBuffer() == buffer)
			{
				textArea.getPainter().repaint();
			}
		}
	}

	class CaretHandler implements CaretListener
	{
		public void caretUpdate(CaretEvent evt)
		{
			status.repaint();
		}
	}

	class EditorHandler extends EditorAdapter
	{
		public void bufferCreated(EditorEvent evt)
		{
			updateBuffersMenu();
			evt.getBuffer().addBufferListener(bufferListener);
		}

		public void bufferClosed(EditorEvent evt)
		{
			Buffer buf = evt.getBuffer();

			buf.removeBufferListener(bufferListener);

			if(buf == buffer)
			{
				Buffer[] bufferArray = jEdit.getBuffers();
				if(bufferArray.length != 0)
					_setBuffer(bufferArray[bufferArray.length - 1]);
			}

			updateOpenRecentMenu();
			updateBuffersMenu();
		}

		public void propertiesChanged(EditorEvent evt)
		{
			View.this.propertiesChanged();
		}
	}

	class StatusBar extends JComponent
	{
		String status;

		StatusBar()
		{
			setDoubleBuffered(true);
			setFont(UIManager.getFont("Label.font"));
			setForeground(UIManager.getColor("Label.foreground"));
			setBackground(UIManager.getColor("Label.background"));
		}

		void showStatus(String status)
		{
			this.status = status;
			repaint();
		}

		public void paint(Graphics g)
		{
			FontMetrics fm = g.getFontMetrics();

			int dot = textArea.getCaretPosition();

			int currLine = textArea.getCaretLine();
			int start = textArea.getLineStartOffset(currLine);
			int numLines = textArea.getLineCount();

			String status = (this.status != null ? this.status
				: "col " + ((dot - start) + 1) + " line "
				+ (currLine + 1) + "/"
				+ numLines + " "
				+ (((currLine + 1) * 100) / numLines) + "%");

			g.drawString(status,0,(getHeight() + fm.getAscent()) / 2);
		}

		public Dimension getPreferredSize()
		{
			return new Dimension(200,0);
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.92  1999/10/10 06:38:45  sp
 * Bug fixes and quicksort routine
 *
 * Revision 1.91  1999/10/05 10:55:29  sp
 * File dialogs open faster, and experimental keyboard macros
 *
 * Revision 1.90  1999/10/03 03:47:15  sp
 * Minor stupidity, IDL mode
 *
 * Revision 1.89  1999/10/01 07:31:39  sp
 * RMI server replaced with socket-based server, minor changes
 *
 * Revision 1.88  1999/09/30 12:21:04  sp
 * No net access for a month... so here's one big jEdit 2.1pre1
 *
 * Revision 1.86  1999/07/16 23:45:49  sp
 * 1.7pre6 BugFree version
 *
 * Revision 1.85  1999/07/08 06:06:04  sp
 * Bug fixes and miscallaneous updates
 *
 * Revision 1.84  1999/07/05 04:38:39  sp
 * Massive batch of changes... bug fixes, also new text component is in place.
 * Have fun
 *
 * Revision 1.83  1999/06/27 04:53:16  sp
 * Text selection implemented in text area, assorted bug fixes
 *
 * Revision 1.82  1999/06/25 06:54:08  sp
 * Text area updates
 *
 * Revision 1.81  1999/06/22 06:14:39  sp
 * RMI updates, text area updates, flag to disable geometry saving
 *
 * Revision 1.80  1999/06/20 07:00:59  sp
 * Text component rewrite started
 *
 * Revision 1.79  1999/06/20 02:15:45  sp
 * Syntax coloring optimizations
 *
 * Revision 1.78  1999/06/16 03:29:59  sp
 * Added <title> tags to docs, configuration data is now stored in a
 * ~/.jedit directory, style option pane finished
 *
 * Revision 1.77  1999/06/15 05:03:54  sp
 * RMI interface complete, save all hack, views & buffers are stored as a link
 * list now
 *
 */
