/*
 * View.java - jEdit view
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

import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.syntax.SyntaxStyle;
import org.gjt.sp.jedit.syntax.Token;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.util.Log;

/**
 * A window that edits buffers. There is no public constructor in the
 * View class. Views are created and destroyed by the <code>jEdit</code>
 * class.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class View extends JFrame implements EBComponent
{
	/**
	 * Displays the specified string in the status area of this view.
	 * @param str The string to display
	 */
	public void pushStatus(String str)
	{
		statusMsg.push(str);
		JEditTextArea[] textAreas = getTextAreas();
		for(int i = 0 ; i < textAreas.length; i++)
			textAreas[i].getStatus().repaint();
	}

	/**
	 * Displays the previous status bar message.
	 */
	public void popStatus()
	{
		statusMsg.pop();
		JEditTextArea[] textAreas = getTextAreas();
		for(int i = 0 ; i < textAreas.length; i++)
			textAreas[i].getStatus().repaint();
	}

	/**
	 * Returns the buffer being edited by this view.
	 */
	public final Buffer getBuffer()
	{
		return buffer;
	}

	/**
	 * Returns the most recently edited buffer.
	 */
	public final Buffer getRecentBuffer()
	{
		return recentBuffer;
	}

	/**
	 * Sets the buffer being edited by this view.
	 * @param buffer The buffer to edit.
	 */
	public void setBuffer(final Buffer buffer)
	{
		if(this.buffer == buffer)
			return;

		recentBuffer = this.buffer;
		if(recentBuffer != null)
			saveCaretInfo();
		this.buffer = buffer;

		unsplit();
		textArea.setDocument(buffer);
		((StatusBar)textArea.getStatus()).repaint();

		updateMarkerMenus();
		updateTitle();

		// Don't fire event for the initial buffer set
		if(recentBuffer != null)
			EditBus.send(new ViewUpdate(this,ViewUpdate.BUFFER_CHANGED));

		if(bufferTabs != null)
			bufferTabs.selectBufferTab(buffer);
		updateBuffersMenu();

		focusOnTextArea();

		// Only do this after all I/O requests are complete
		VFSManager.runInAWTThread(new Runnable()
		{
			public void run()
			{
				loadCaretInfo();
				buffer.checkModTime(View.this);
			}
		});
	}

	/**
	 * Sets the focus onto the text area.
	 */
	public final void focusOnTextArea()
	{
		textArea.requestFocus();
	}

	/**
	 * Returns the view's text area.
	 */
	public final JEditTextArea getTextArea()
	{
		return textArea;
	}

	/**
	 * Returns the search bar.
	 * @since jEdit 2.4pre4
	 */
	public final SearchBar getSearchBar()
	{
		return searchBar;
	}

	/**
	 * Returns the input handler.
	 */
	public final InputHandler getInputHandler()
	{
		return inputHandler;
	}

	/**
	 * Splits the view.
	 * @since jEdit 2.3pre2
	 */
	public void split(int orientation)
	{
		saveCaretInfo();
		JEditTextArea oldTextArea = textArea;
		textArea = createTextArea();
		initTextArea(textArea,oldTextArea);
		loadCaretInfo();

		JComponent oldParent = (JComponent)oldTextArea.getParent();

		if(oldParent instanceof JSplitPane)
		{
			JSplitPane oldSplitPane = (JSplitPane)oldParent;
			int dividerPos = oldSplitPane.getDividerLocation();

			Component left = oldSplitPane.getLeftComponent();
			final JSplitPane newSplitPane = new JSplitPane(orientation,oldTextArea,textArea);
			newSplitPane.setBorder(null);

			if(left == oldTextArea)
				oldSplitPane.setLeftComponent(newSplitPane);
			else
				oldSplitPane.setRightComponent(newSplitPane);

			oldSplitPane.setDividerLocation(dividerPos);

			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					newSplitPane.setDividerLocation(0.5);
				}
			});
		}
		else
		{
			JSplitPane newSplitPane = splitPane = new JSplitPane(orientation,
				oldTextArea,textArea);
			newSplitPane.setBorder(null);
			if(bufferTabs != null)
				bufferTabs.update();
			else
			{
				oldParent.add(splitPane);
				oldParent.revalidate();
			}

			newSplitPane.setDividerLocation(oldParent.getHeight() / 2);
		}

		textArea.requestFocus();
	}

	/**
	 * Unsplits the view.
	 * @since jEdit 2.3pre2
	 */
	public void unsplit()
	{
		if(splitPane != null)
		{
			JEditTextArea[] textAreas = getTextAreas();
			for(int i = 0; i < textAreas.length; i++)
			{
				JEditTextArea _textArea = textAreas[i];
				if(textArea != _textArea)
				{
					EditBus.send(new ViewUpdate(this,_textArea,
						ViewUpdate.TEXTAREA_DESTROYED));
				}
			}

			JComponent parent = (JComponent)splitPane.getParent();
			parent.remove(splitPane);
			splitPane = null;
			if(bufferTabs != null)
				bufferTabs.update();
			else
			{
				parent.add(textArea);
				parent.revalidate();
			}
		}

		focusOnTextArea();
	}

	/**
	 * Returns the top-level split pane, if any.
	 * @since jEdit 2.3pre2
	 */
	public JSplitPane getSplitPane()
	{
		return splitPane;
	}

	/**
	 * Returns all text areas.
	 * @since jEdit 2.3pre2
	 */
	public JEditTextArea[] getTextAreas()
	{
		if(splitPane == null)
		{
			JEditTextArea[] ta = { textArea };
			return ta;
		}
		else
		{
			Vector vec = new Vector();
			getTextAreas(vec,splitPane);
			JEditTextArea[] ta = new JEditTextArea[vec.size()];
			vec.copyInto(ta);
			return ta;
		}
	}

	/**
	 * Adds a tool bar to this view.
	 * @param toolBar The tool bar
	 */
	public void addToolBar(Component toolBar)
	{
		toolBars.add(toolBar);
		getRootPane().revalidate();
	}

	/**
	 * Removes a tool bar from this view.
	 * @param toolBar The tool bar
	 */
	public void removeToolBar(Component toolBar)
	{
		toolBars.remove(toolBar);
		getRootPane().revalidate();
	}

	/**
	 * Saves the caret information to the current buffer.
	 */
	public void saveCaretInfo()
	{
		buffer.putProperty(Buffer.SELECTION_START,new Integer(
			textArea.getSelectionStart()));
		buffer.putProperty(Buffer.SELECTION_END,new Integer(
			textArea.getSelectionEnd()));
		buffer.putProperty(Buffer.SELECTION_RECT,new Boolean(
			textArea.isSelectionRectangular()));
		buffer.putProperty(Buffer.SCROLL_VERT,new Integer(
			textArea.getFirstLine()));
		buffer.putProperty(Buffer.SCROLL_HORIZ,new Integer(
			textArea.getHorizontalOffset()));
		buffer.putProperty(Buffer.OVERWRITE,new Boolean(
			textArea.isOverwriteEnabled()));
	}

	/**
	 * Loads the caret information from the curret buffer.
	 */
	public void loadCaretInfo()
	{
		Integer start = (Integer)buffer.getProperty(Buffer.SELECTION_START);
		Integer end = (Integer)buffer.getProperty(Buffer.SELECTION_END);
		Boolean rectSel = (Boolean)buffer.getProperty(Buffer.SELECTION_RECT);
		Integer firstLine = (Integer)buffer.getProperty(Buffer.SCROLL_VERT);
		Integer horizontalOffset = (Integer)buffer.getProperty(Buffer.SCROLL_HORIZ);
		Boolean overwrite = (Boolean)buffer.getProperty(Buffer.OVERWRITE);

		if(start != null && end != null)
		{
			textArea.select(Math.min(start.intValue(),
				buffer.getLength()),
				Math.min(end.intValue(),
				buffer.getLength()));
		}

		if(firstLine != null && horizontalOffset != null)
		{
			textArea.setFirstLine(firstLine.intValue());
			textArea.setHorizontalOffset(horizontalOffset.intValue());
		}

		if(rectSel != null && overwrite != null)
		{
			textArea.setSelectionRectangular(rectSel.booleanValue());
			textArea.setOverwriteEnabled(overwrite.booleanValue());
		}
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
	 * Shows the wait cursor.
	 */
	public void showWaitCursor()
	{
		if(waitCount++ == 0)
		{
			Cursor cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
			setCursor(cursor);
			JEditTextArea[] textAreas = getTextAreas();
			for(int i = 0; i < textAreas.length; i++)
			{
				JEditTextArea textArea = textAreas[i];
				textArea.getPainter().setCursor(cursor);
			}
		}
	}

	/**
	 * Hides the wait cursor.
	 */
	public void hideWaitCursor()
	{
		if(waitCount > 0)
			waitCount--;

		if(waitCount == 0)
		{
			Cursor cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
			setCursor(cursor);
			cursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
			JEditTextArea[] textAreas = getTextAreas();
			for(int i = 0; i < textAreas.length; i++)
			{
				JEditTextArea textArea = textAreas[i];
				textArea.getPainter().setCursor(cursor);
			}
		}
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

	public void handleMessage(EBMessage msg)
	{
		if(msg instanceof PropertiesChanged)
			propertiesChanged();
		else if(msg instanceof MacrosChanged)
			updateMacrosMenu();
		else if(msg instanceof SearchSettingsChanged)
		{
			if(searchBar != null)
				searchBar.update();
		}
		else if(msg instanceof BufferUpdate)
			handleBufferUpdate((BufferUpdate)msg);
	}

	public JMenu getMenu(String name)
	{
		if(name.equals("buffers"))
			return buffers;
		else if(name.equals("open-from-menu"))
			return openFrom;
		else if(name.equals("save-to-menu"))
			return saveTo;
		else if(name.equals("recent-files"))
			return recent;
		else if(name.equals("current-directory"))
			return currentDirectory;
		else if(name.equals("clear-marker"))
			return clearMarker;
		else if(name.equals("goto-marker"))
			return gotoMarker;
		else if(name.equals("macros"))
			return macros;
		else if(name.equals("plugins"))
			return plugins;
		else if(name.equals("help-menu"))
			return help;
		else
			return null;
	}

	// package-private members
	View prev;
	View next;

	View(View view, Buffer buffer)
	{
		setIconImage(GUIUtilities.getEditorIcon());

		// Dynamic menus
		buffers = GUIUtilities.loadMenu(this,"buffers");
		openFrom = GUIUtilities.loadMenu(this,"open-from-menu");
		saveTo = GUIUtilities.loadMenu(this,"save-to-menu");
		recent = GUIUtilities.loadMenu(this,"recent-files");
		currentDirectory = new CurrentDirectoryMenu(this);
		clearMarker = GUIUtilities.loadMenu(this,"clear-marker");
		gotoMarker = GUIUtilities.loadMenu(this,"goto-marker");
		macros = GUIUtilities.loadMenu(this,"macros");
		help = GUIUtilities.loadMenu(this,"help-menu");
		plugins = GUIUtilities.loadMenu(this,"plugins");
		updateVFSMenus();
		updateMacrosMenu();
		updateHelpMenu();
		updatePluginsMenu();

		EditBus.addToBus(this);

		setJMenuBar(GUIUtilities.loadMenuBar(this,"view.mbar"));

		toolBars = new Box(BoxLayout.Y_AXIS);

		statusMsg = new Stack();

		inputHandler = new DefaultInputHandler(this,(DefaultInputHandler)
			jEdit.getInputHandler());

		textArea = createTextArea();

		if(view == null)
			propertiesChanged();
		else
			loadPropertiesFromView(view);

		if(buffer == null)
			setBuffer(jEdit.getFirstBuffer());
		else
			setBuffer(buffer);

		getContentPane().add(BorderLayout.NORTH,toolBars);

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowHandler());
	}

	void close()
	{
		closed = true;
		GUIUtilities.saveGeometry(this,"view");
		saveCaretInfo();
		EditBus.removeFromBus(this);
		dispose();

		JEditTextArea[] textAreas = getTextAreas();
		for(int i = 0; i < textAreas.length; i++)
		{
			EditBus.send(new ViewUpdate(this,textAreas[i],
				ViewUpdate.TEXTAREA_DESTROYED));
		}
	}

	// protected members

	/**
	 * Forwards key events directly to the input handler.
	 * This is slightly faster than using a KeyListener
	 * because some Swing overhead is avoided.
	 */
	protected void processKeyEvent(KeyEvent evt)
	{
		switch(evt.getID())
		{
		case KeyEvent.KEY_PRESSED:
			inputHandler.keyPressed(evt);
			break;
		case KeyEvent.KEY_RELEASED:
			inputHandler.keyReleased(evt);
			break;
		}
	}

	// private members
	private Buffer buffer;
	private Buffer recentBuffer;
	private boolean closed;

	private JMenu buffers;
	private JMenu openFrom;
	private JMenu saveTo;
	private JMenu recent;
	private JMenu currentDirectory;
	private JMenu clearMarker;
	private JMenu gotoMarker;
	private JMenu macros;
	private JMenu plugins;
	private JMenu help;

	private Box toolBars;
	private JToolBar toolBar;
	private SearchBar searchBar;

	private Stack statusMsg;

	private BufferTabs bufferTabs;
	private JSplitPane splitPane;
	private JEditTextArea textArea;

	private InputHandler inputHandler;

	private int waitCount;

	private boolean showFullPath;
	private boolean checkModStatus;

	/**
	 * Reloads various settings from the properties.
	 */
	private void getTextAreas(Vector vec, Component comp)
	{
		if(comp instanceof JEditTextArea)
			vec.addElement(comp);
		else if(comp instanceof JSplitPane)
		{
			JSplitPane split = (JSplitPane)comp;
			getTextAreas(vec,split.getLeftComponent());
			getTextAreas(vec,split.getRightComponent());
		}
	}

	private void propertiesChanged()
	{
		loadToolBars();
		initBufferTabs();

		showFullPath = jEdit.getBooleanProperty("view.showFullPath");
		checkModStatus = jEdit.getBooleanProperty("view.checkModStatus");
		if(buffer != null)
			updateTitle();

		JEditTextArea[] textAreas = getTextAreas();
		for(int i = 0; i < textAreas.length; i++)
		{
			initTextArea(textAreas[i]);
		}

		loadStyles();

		updateRecentMenu();
	}

	private void initTextArea(JEditTextArea textArea)
	{
		TextAreaPainter painter = textArea.getPainter();

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

		painter.setFont(font);
		painter.setBracketHighlightEnabled(jEdit.getBooleanProperty(
			"view.bracketHighlight"));
		painter.setBracketHighlightColor(GUIUtilities.parseColor(
			jEdit.getProperty("view.bracketHighlightColor")));
		painter.setEOLMarkersPainted(jEdit.getBooleanProperty(
			"view.eolMarkers"));
		painter.setInvalidLinesPainted(jEdit.getBooleanProperty(
			"view.paintInvalid"));
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
		painter.setBlockCaretEnabled(jEdit.getBooleanProperty(
			"view.blockCaret"));
		painter.setLineHighlightEnabled(jEdit.getBooleanProperty(
			"view.lineHighlight"));
		painter.setLineHighlightColor(GUIUtilities.parseColor(
			jEdit.getProperty("view.lineHighlightColor")));

		Gutter gutter = textArea.getGutter();
		try
		{
			int width = Integer.parseInt(jEdit.getProperty(
				"view.gutter.width"));
			gutter.setGutterWidth(width);
		}
		catch(NumberFormatException nf)
		{
			// retain the default gutter width
		}
		gutter.setCollapsed(jEdit.getBooleanProperty(
			"view.gutter.collapsed"));
		gutter.setLineNumberingEnabled(jEdit.getBooleanProperty(
			"view.gutter.lineNumbers"));
		try
		{
			int interval = Integer.parseInt(jEdit.getProperty(
				"view.gutter.highlightInterval"));
			gutter.setHighlightInterval(interval);
		}
		catch(NumberFormatException nf)
		{
			// retain the default highlight interval
		}
		gutter.setCurrentLineHighlightEnabled(jEdit.getBooleanProperty(
			"view.gutter.highlightCurrentLine"));
		gutter.setBackground(GUIUtilities.parseColor(
			jEdit.getProperty("view.gutter.bgColor")));
		gutter.setForeground(GUIUtilities.parseColor(
			jEdit.getProperty("view.gutter.fgColor")));
		gutter.setHighlightedForeground(GUIUtilities.parseColor(
			jEdit.getProperty("view.gutter.highlightColor")));
		gutter.setCurrentLineForeground(GUIUtilities.parseColor(
			jEdit.getProperty("view.gutter.currentLineColor")));
		String alignment = jEdit.getProperty(
			"view.gutter.numberAlignment");
		if ("right".equals(alignment))
		{
			gutter.setLineNumberAlignment(Gutter.RIGHT);
		}
		else if ("center".equals(alignment))
		{
			gutter.setLineNumberAlignment(Gutter.CENTER);
		}
		else // left == default case
		{
			gutter.setLineNumberAlignment(Gutter.LEFT);
		}
		try
		{
			int width = Integer.parseInt(jEdit.getProperty(
				"view.gutter.borderWidth"));
			gutter.setBorder(width, GUIUtilities.parseColor(
				jEdit.getProperty("view.gutter.borderColor")));
		}
		catch(NumberFormatException nf)
		{
			// retain the default border
		}
		try
		{
			String fontname = jEdit.getProperty("view.gutter.font");
			int fontsize = Integer.parseInt(jEdit.getProperty(
				"view.gutter.fontsize"));
			int fontstyle = Integer.parseInt(jEdit.getProperty(
				"view.gutter.fontstyle"));
			gutter.setFont(new Font(fontname,fontstyle,fontsize));
		}
		catch(NumberFormatException nf)
		{
			// retain the default font
		}

		textArea.setCaretBlinkEnabled(jEdit.getBooleanProperty(
			"view.caretBlink"));

		try
		{
			textArea.setElectricScroll(Integer.parseInt(jEdit
				.getProperty("view.electricBorders")));
		}
		catch(NumberFormatException nf)
		{
			textArea.setElectricScroll(0);
		}

		// Set up the right-click popup menu
		textArea.setRightClickPopup(GUIUtilities
			.loadPopupMenu(this,"view.context"));
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
			styles[Token.LITERAL1] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.literal1"));
			styles[Token.LITERAL2] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.literal2"));
			styles[Token.LABEL] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.label"));
			styles[Token.KEYWORD1] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.keyword1"));
			styles[Token.KEYWORD2] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.keyword2"));
			styles[Token.KEYWORD3] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.keyword3"));
			styles[Token.FUNCTION] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.function"));
			styles[Token.MARKUP] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.markup"));
			styles[Token.OPERATOR] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.operator"));
			styles[Token.DIGIT] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.digit"));
			styles[Token.INVALID] = GUIUtilities.parseStyle(
				jEdit.getProperty("view.style.invalid"));

			JEditTextArea[] textAreas = getTextAreas();
			for(int i = 0; i < textAreas.length; i++)
			{
				JEditTextArea textArea = textAreas[i];
				textArea.getPainter().setStyles(styles);
			}
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,this,e);
		}
	}

	private void loadPropertiesFromView(View view)
	{
		loadToolBars();
		initBufferTabs();

		showFullPath = view.showFullPath;
		checkModStatus = view.checkModStatus;

		JEditTextArea[] textAreas = getTextAreas();
		for(int i = 0; i < textAreas.length; i++)
		{
			initTextArea(textAreas[i],view.textArea);
		}

		updateRecentMenu();
	}

	private void initTextArea(JEditTextArea textArea, JEditTextArea copy)
	{
		TextAreaPainter painter = copy.getPainter();
		TextAreaPainter myPainter = textArea.getPainter();
		myPainter.setFont(painter.getFont());
		myPainter.setBracketHighlightEnabled(painter.isBracketHighlightEnabled());
		myPainter.setBracketHighlightColor(painter.getBracketHighlightColor());
		myPainter.setEOLMarkersPainted(painter.getEOLMarkersPainted());
		myPainter.setInvalidLinesPainted(painter.getInvalidLinesPainted());
		myPainter.setEOLMarkerColor(painter.getEOLMarkerColor());
		myPainter.setCaretColor(painter.getCaretColor());
		myPainter.setSelectionColor(painter.getSelectionColor());
		myPainter.setBackground(painter.getBackground());
		myPainter.setForeground(painter.getForeground());
		myPainter.setBlockCaretEnabled(painter.isBlockCaretEnabled());
		myPainter.setLineHighlightEnabled(painter.isLineHighlightEnabled());
		myPainter.setLineHighlightColor(painter.getLineHighlightColor());
		myPainter.setStyles(painter.getStyles());

		Gutter myGutter = textArea.getGutter();
		Gutter gutter = copy.getGutter();
		myGutter.setGutterWidth(gutter.getGutterWidth());
		myGutter.setCollapsed(gutter.isCollapsed());
		myGutter.setLineNumberingEnabled(gutter.isLineNumberingEnabled());
		myGutter.setHighlightInterval(gutter.getHighlightInterval());
		myGutter.setCurrentLineHighlightEnabled(gutter.isCurrentLineHighlightEnabled());
		myGutter.setLineNumberAlignment(gutter.getLineNumberAlignment());
		myGutter.setFont(gutter.getFont());
		myGutter.setBorder(gutter.getBorder());
		myGutter.setBackground(gutter.getBackground());
		myGutter.setForeground(gutter.getForeground());
		myGutter.setHighlightedForeground(gutter.getHighlightedForeground());
		myGutter.setCurrentLineForeground(gutter.getCurrentLineForeground());

		textArea.setCaretBlinkEnabled(copy.isCaretBlinkEnabled());
		textArea.setElectricScroll(copy.getElectricScroll());

		myPainter.setStyles(painter.getStyles());

		// Set up the right-click popup menu
		textArea.setRightClickPopup(GUIUtilities
			.loadPopupMenu(this,"view.context"));
	}

	private void loadToolBars()
	{
		if(jEdit.getBooleanProperty("view.showToolbar"))
		{
			if(toolBar == null)
			{
				toolBar = GUIUtilities.loadToolBar("view.toolbar");
				toolBar.add(Box.createGlue());
			}
			if(toolBar.getParent() == null)
				addToolBar(toolBar);
		}
		else if(toolBar != null)
		{
			removeToolBar(toolBar);
			toolBar = null;
		}

		if(jEdit.getBooleanProperty("view.showSearchbar"))
		{
			if(searchBar == null)
				searchBar = new SearchBar(this);
			if(searchBar.getParent() == null)
				addToolBar(searchBar);
		}
		else if(searchBar != null)
		{
			removeToolBar(searchBar);
			searchBar = null;
		}
	}

	private JEditTextArea createTextArea()
	{
		JEditTextArea textArea = new JEditTextArea();

		// Add the line number display
		textArea.add(JEditTextArea.LEFT_OF_SCROLLBAR,new StatusBar(textArea));

		textArea.getGutter().setContextMenu(GUIUtilities
			.loadPopupMenu(this,"gutter.context"));

		textArea.addCaretListener(new CaretHandler());
		textArea.addFocusListener(new FocusHandler());

		if(buffer != null)
		{
			textArea.setDocument(buffer);
			textArea.setEditable(!buffer.isReadOnly());
		}

		EditBus.send(new ViewUpdate(this,textArea,
			ViewUpdate.TEXTAREA_CREATED));

		return textArea;
	}

	private void initBufferTabs()
	{
		Container parent;

		if(bufferTabs != null)
			parent = bufferTabs.getParent();
		else if(splitPane != null)
			parent = splitPane.getParent();
		else
			parent = textArea.getParent();

		if(parent == null)
			parent = getContentPane();

		Container comp;
		if(splitPane == null)
			comp = textArea;
		else
			comp = splitPane;

		if(jEdit.getBooleanProperty("view.showBufferTabs"))
		{
			if(bufferTabs == null)
			{
				bufferTabs = new BufferTabs(this);
				parent.remove(comp);

				// BorderLayout adds to center by default,
				// but this will also work with other layouts.
				parent.add(bufferTabs);
			}

			bufferTabs.setTabPlacement(Integer.parseInt(
				jEdit.getProperty("view.bufferTabsPos")));
		}
		else
		{
			if(bufferTabs != null)
			{
				parent.remove(bufferTabs);
				bufferTabs = null;
				parent.add(comp);
			}
			else if(comp.getParent() == null)
			{
				parent.add(comp);
			}
		}

		getRootPane().revalidate();
		getRootPane().repaint();
	}

	/**
	 * Updates the title bar and read only status of the text
	 * area.
	 */
	private void updateTitle()
	{
		Object[] args = { ((showFullPath && !buffer.isNewFile())
			? buffer.getPath() : buffer.getName()),
			new Integer(buffer.isReadOnly() ? 1 : 0),
			new Integer(buffer.isDirty() ? 1: 0),
			new Integer(buffer.isNewFile() ? 1: 0)};
		setTitle(jEdit.getProperty("view.title",args));

		JEditTextArea[] textAreas = getTextAreas();
		for(int i = 0; i < textAreas.length; i++)
		{
			JEditTextArea textArea = textAreas[i];
			textArea.setEditable(!buffer.isReadOnly());
		}
	}

	/**
	 * Recreates the buffers menu.
	 */
	private void updateBuffersMenu()
	{
		if(jEdit.getBufferCount() == 0)
		{
			// if this becomes zero, it is guaranteed that
			// a new untitled buffer will open again. So
			// we just ignore this and update the menu
			// when the untitled buffer comes in
			return;
		}

		// Because the buffers menu contains normal items as
		// well as dynamically-generated stuff, we are careful
		// to only remove the dynamic crap here...
		for(int i = buffers.getMenuComponentCount() - 1; i >= 0; i--)
		{
			if(buffers.getMenuComponent(i) instanceof JSeparator)
				break;
			else
				buffers.remove(i);
		}

		Buffer[] bufferArray = jEdit.getBuffers();
		ButtonGroup grp = new ButtonGroup();
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
					"view.buffer-label",args));
			menuItem.addActionListener(jEdit.getAction("select-buffer"));
			grp.add(menuItem);
			menuItem.setActionCommand(name);
			if(buffer == b)
				menuItem.getModel().setSelected(true);
			buffers.add(menuItem);
		}
	}

	/**
	 * Recreates the VFS open-from and save-to menus.
	 */
	private void updateVFSMenus()
	{
		EditAction openFromAction = jEdit.getAction("open-from");
		EditAction saveToAction = jEdit.getAction("save-to");

		Enumeration enum = VFSManager.getFilesystems();

		Vector openFromItems = new Vector();
		Vector saveToItems = new Vector();
		while(enum.hasMoreElements())
		{
			VFS vfs = (VFS)enum.nextElement();
			String name = vfs.getName();
			String label = jEdit.getProperty("vfs." + name
				+ ".label");
			EnhancedMenuItem emi = new EnhancedMenuItem(
				label,null,openFromAction,name);
			openFromItems.addElement(emi);
			emi = new EnhancedMenuItem(
				label,null,saveToAction,name);
			saveToItems.addElement(emi);
		}

		MiscUtilities.quicksort(openFromItems,new MenuItemCompare());
		MiscUtilities.quicksort(saveToItems,new MenuItemCompare());
		for(int i = 0; i < openFromItems.size(); i++)
		{
			openFrom.add((JMenuItem)openFromItems.elementAt(i));
			saveTo.add((JMenuItem)saveToItems.elementAt(i));
		}
	}

	/**
	 * Recreates the recent menu.
	 */
	private void updateRecentMenu()
	{
		if(recent.getMenuComponentCount() != 0)
			recent.removeAll();
		EditAction action = jEdit.getAction("open-path");
		String[] recentArray = jEdit.getRecent();
		if(recentArray.length == 0)
		{
			recent.add(GUIUtilities.loadMenuItem(this,"no-recent"));
			return;
		}
		for(int i = 0; i < recentArray.length; i++)
		{
			String path = recentArray[i];
			EnhancedMenuItem menuItem = new EnhancedMenuItem(path,
				null,action,path);
			recent.add(menuItem);
		}
	}

	/**
	 * Recreates the goto marker and clear marker menus.
	 */
	private void updateMarkerMenus()
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
			EnhancedMenuItem menuItem = new EnhancedMenuItem(name,
				null,clearMarkerAction,name);
			clearMarker.add(menuItem);
			menuItem = new EnhancedMenuItem(name,null,
				gotoMarkerAction,name);
			gotoMarker.add(menuItem);
		}
	}

	/**
	 * Recreates the macros menu.
	 */
	private void updateMacrosMenu()
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

		int count = macros.getMenuComponentCount();

		Vector macroVector = Macros.getMacros();
		createMacrosMenu(macros,macroVector,0);

		if(count == macros.getMenuComponentCount())
			macros.add(GUIUtilities.loadMenuItem(this,"no-macros"));
	}

	private void createMacrosMenu(JMenu menu, Vector vector, int start)
	{
		EditAction action = jEdit.getAction("play-macro");

		for(int i = start; i < vector.size(); i++)
		{
			Object obj = vector.elementAt(i);
			if(obj instanceof Macros.Macro)
			{
				Macros.Macro macro = (Macros.Macro)obj;
				String label = macro.label;
				String path = macro.path;
				EnhancedMenuItem menuItem = new EnhancedMenuItem(
					label,jEdit.getProperty(macro.name + ".shortcut"),
					action,path);
				menu.add(menuItem);
			}
			else if(obj instanceof Vector)
			{
				Vector subvector = (Vector)obj;
				String name = (String)subvector.elementAt(0);
				JMenu submenu = new JMenu(name);
				createMacrosMenu(submenu,subvector,1);
				if(submenu.getMenuComponentCount() == 0)
				{
					submenu.add(GUIUtilities.loadMenuItem(
						this,"no-macros"));
				}
				menu.add(submenu);
			}
		}
	}

	/**
	 * Recreates the plugins menu.
	 */
	private void updatePluginsMenu()
	{
		if(plugins.getMenuComponentCount() != 0)
			plugins.removeAll();

		// Query plugins for menu items
		Vector pluginMenus = new Vector();
		Vector pluginMenuItems = new Vector();

		EditPlugin[] pluginArray = jEdit.getPlugins();
		for(int i = 0; i < pluginArray.length; i++)
		{
			try
			{
				pluginArray[i].createMenuItems(this,pluginMenus,
					pluginMenuItems);
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,this,"Error creating menu items"
					+ " for plugin");
				Log.log(Log.ERROR,this,t);
			}
		}

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

	private void updateHelpMenu()
	{
		EditAction action = jEdit.getAction("help");

		EditPlugin[] plugins = jEdit.getPlugins();
		for(int i = 0; i < plugins.length; i++)
		{
			EditPlugin plugin = plugins[i];
			String name = plugin.getClass().getName();

			String label = jEdit.getProperty("plugin." + name + ".name");
			String docs = jEdit.getProperty("plugin." + name + ".docs");
			if(docs != null)
			{
				java.net.URL docsURL = plugin.getClass().getResource(docs);
				if(label != null && docsURL != null)
				{
					help.add(new EnhancedMenuItem(label,
						null,action,docsURL.toString()));
				}
			}
		}
	}

	private static class MenuItemCompare implements MiscUtilities.Compare
	{
		public int compare(Object obj1, Object obj2)
		{
			return ((JMenuItem)obj1).getText().compareTo(
				((JMenuItem)obj2).getText());
		}
	}

	private void handleBufferUpdate(BufferUpdate msg)
	{
		Buffer _buffer = msg.getBuffer();
		if(msg.getWhat() == BufferUpdate.CREATED)
		{
			updateBuffersMenu();

			if(bufferTabs != null)
				bufferTabs.addBufferTab(_buffer);
		}
		else if(msg.getWhat() == BufferUpdate.CLOSED)
		{
			if(bufferTabs != null)
				bufferTabs.removeBufferTab(_buffer);

			if(_buffer == buffer)
			{
				Buffer newBuffer = (recentBuffer != null ?
					recentBuffer : _buffer.getPrev());
				if(newBuffer != null && !newBuffer.isClosed())
					setBuffer(newBuffer);
				else if(jEdit.getBufferCount() != 0)
					setBuffer(jEdit.getFirstBuffer());

				recentBuffer = null;
			}

			updateRecentMenu();
			updateBuffersMenu();
		}
		else if(msg.getWhat() == BufferUpdate.DIRTY_CHANGED
			|| msg.getWhat() == BufferUpdate.LOADED)
		{
			if(_buffer == buffer)
				updateTitle();
			updateBuffersMenu();

			if(bufferTabs != null)
				bufferTabs.updateBufferTab(_buffer);
		}
		else if(msg.getWhat() == BufferUpdate.MARKERS_CHANGED)
		{
			if(_buffer == buffer)
				updateMarkerMenus();
		}
		else if(msg.getWhat() == BufferUpdate.MODE_CHANGED)
		{
			if(_buffer == buffer)
			{
				JEditTextArea[] textAreas = getTextAreas();
				for(int i = 0; i < textAreas.length; i++)
				{
					JEditTextArea textArea = textAreas[i];
					textArea.getPainter().repaint();
				}
			}
		}
	}

	// event listeners
	class CaretHandler implements CaretListener
	{
		public void caretUpdate(CaretEvent evt)
		{
			JEditTextArea textArea = (JEditTextArea)evt.getSource();
			((StatusBar)textArea.getStatus()).repaint();
		}
	}

	class FocusHandler extends FocusAdapter
	{
		public void focusGained(FocusEvent evt)
		{
			textArea = (JEditTextArea)evt.getSource();
		}
	}

	class WindowHandler extends WindowAdapter
	{
		boolean gotFocus;

		public void windowActivated(WindowEvent evt)
		{
			if(!gotFocus)
			{
				textArea.requestFocus();
				gotFocus = true;
			}

			if(checkModStatus)
				buffer.checkModTime(View.this);
		}

		public void windowClosing(WindowEvent evt)
		{
			jEdit.closeView(View.this);
		}
	}

	class StatusBar extends JComponent
	{
		JEditTextArea textArea;

		StatusBar(JEditTextArea textArea)
		{
			StatusBar.this.textArea = textArea;
			StatusBar.this.setDoubleBuffered(true);
			StatusBar.this.setFont(UIManager.getFont("Label.font"));
			StatusBar.this.setForeground(UIManager.getColor("Label.foreground"));
			StatusBar.this.setBackground(UIManager.getColor("Label.background"));
		}

		public void paint(Graphics g)
		{
			FontMetrics fm = g.getFontMetrics();

			int dot = textArea.getCaretPosition();

			int currLine = StatusBar.this.textArea.getCaretLine();
			int start = StatusBar.this.textArea.getLineStartOffset(currLine);
			int numLines = StatusBar.this.textArea.getLineCount();

			String str;
			if(inputHandler.isRepeatEnabled())
			{
				int repeatCount = inputHandler.getRepeatCount();
				if(repeatCount == 1)
					str = "";
				else
					str = String.valueOf(repeatCount);
				Object[] args = { str };
				str = jEdit.getProperty("view.status.repeat",args);
			}
			else if(!statusMsg.isEmpty())
				str = (String)statusMsg.peek();
			else
			{
				str = ("col " + ((dot - start) + 1) + " line "
					+ (currLine + 1) + "/"
					+ numLines + " "
					+ (((currLine + 1) * 100) / numLines) + "%");
			}

			g.drawString(str,0,(StatusBar.this.getHeight()
				+ fm.getAscent()) / 2);
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
 * Revision 1.163  2000/04/29 09:17:07  sp
 * VFS updates, various fixes
 *
 * Revision 1.162  2000/04/28 09:29:11  sp
 * Key binding handling improved, VFS updates, some other stuff
 *
 * Revision 1.161  2000/04/25 03:32:40  sp
 * Even more VFS hacking
 *
 * Revision 1.160  2000/04/24 11:00:23  sp
 * More VFS hacking
 *
 * Revision 1.159  2000/04/21 05:32:20  sp
 * Focus tweak
 *
 * Revision 1.158  2000/04/18 08:27:52  sp
 * Context menu editor started
 *
 * Revision 1.157  2000/04/15 04:14:47  sp
 * XML files updated, jEdit.get/setBooleanProperty() method added
 *
 * Revision 1.156  2000/04/14 11:57:38  sp
 * Text area actions moved to org.gjt.sp.jedit.actions package
 *
 * Revision 1.155  2000/04/09 03:14:14  sp
 * Syntax token backgrounds can now be specified
 *
 * Revision 1.154  2000/04/08 09:34:58  sp
 * Documentation updates, minor syntax changes
 *
 * Revision 1.153  2000/04/08 06:10:51  sp
 * Digit highlighting, search bar bug fix
 *
 * Revision 1.152  2000/04/08 02:39:33  sp
 * New Token.MARKUP type, remove Token.{CONSTANT,VARIABLE,DATATYPE}
 *
 */
