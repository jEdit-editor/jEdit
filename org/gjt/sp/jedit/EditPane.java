/*
 * EditPane.java - Text area and buffer
 * Copyright (C) 2000 Slava Pestov
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

import javax.swing.JPanel;
import java.awt.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.syntax.*;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.util.Log;

/**
 * A panel containing a text area. Each edit pane can edit one buffer at
 * a time.
 * @author Slava Pestov
 * @version $Id$
 */
public class EditPane extends JPanel implements EBComponent
{
	/**
	 * Returns the view containing this edit pane.
	 * @since jEdit 2.5pre2
	 */
	public View getView()
	{
		return view;
	}

	/**
	 * Returns the current buffer.
	 * @since jEdit 2.5pre2
	 */
	public Buffer getBuffer()
	{
		return buffer;
	}

	/**
	 * Returns the most recently edited buffer.
	 * @since jEdit 2.5pre2
	 */
	public Buffer getRecentBuffer()
	{
		return recentBuffer;
	}

	/**
	 * Sets the current buffer.
	 * @param buffer The buffer to edit.
	 * @since jEdit 2.5pre2
	 */
	public void setBuffer(final Buffer buffer)
	{
		if(this.buffer == buffer)
			return;

		if(buffer.isClosed())
			throw new InternalError(buffer + " has been closed");

		recentBuffer = this.buffer;
		if(recentBuffer != null)
			saveCaretInfo();
		this.buffer = buffer;

		textArea.setBuffer(buffer);

		if(bufferTabs != null)
			bufferTabs.selectBufferTab(buffer);

		updateTextArea();
		focusOnTextArea();

		if(!init)
		{
			view.updateTitle();
			view.updateBufferStatus();
			view.updateBuffersMenu();
			view.updateMarkerMenus();

			EditBus.send(new EditPaneUpdate(this,EditPaneUpdate
				.BUFFER_CHANGED));
		}

		// Only do this after all I/O requests are complete
		VFSManager.runInAWTThread(new Runnable()
		{
			public void run()
			{
				loadCaretInfo();
				buffer.checkModTime(view);
			}
		});
	}

	/**
	 * Sets the focus onto the text area.
	 * @since jEdit 2.5pre2
	 */
	public void focusOnTextArea()
	{
		textArea.requestFocus();
	}

	/**
	 * Returns the view's text area.
	 * @since jEdit 2.5pre2
	 */
	public JEditTextArea getTextArea()
	{
		return textArea;
	}

	/**
	 * Saves the caret information to the current buffer.
	 * @since jEdit 2.5pre2
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

	public void handleMessage(EBMessage msg)
	{
		if(msg instanceof PropertiesChanged)
			propertiesChanged();
		else if(msg instanceof RegistersChanged)
			textArea.getGutter().repaint();
		else if(msg instanceof BufferUpdate)
			handleBufferUpdate((BufferUpdate)msg);
	}

	/**
	 * Loads the caret information from the curret buffer.
	 * @since jEdit 2.5pre2
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
	 * Returns 0,0 for split pane compatibility.
	 */
	public final Dimension getMinimumSize()
	{
		return new Dimension(0,0);
	}

	// package-private members
	EditPane(View view, EditPane editPane, Buffer buffer)
	{
		super(new BorderLayout());

		init = true;

		this.view = view;

		EditBus.addToBus(this);

		textArea = new JEditTextArea();
		markerHighlight = new MarkerHighlight();
		textArea.getGutter().addCustomHighlight(markerHighlight);
		textArea.getGutter().setContextMenu(GUIUtilities
			.loadPopupMenu(view,"view.gutter.context"));

		if(editPane != null)
			initTextArea(editPane.textArea);
		else
			initTextArea();

		initBufferTabs();

		if(buffer == null)
		{
			if(editPane != null)
				setBuffer(editPane.getBuffer());
			else
				setBuffer(jEdit.getFirstBuffer());
		}
		else
			setBuffer(buffer);

		init = false;
	}

	void close()
	{
		saveCaretInfo();
		EditBus.send(new EditPaneUpdate(this,EditPaneUpdate.DESTROYED));
		EditBus.removeFromBus(this);
	}

	// private members
	private boolean init;
	private View view;
	private Buffer buffer;
	private Buffer recentBuffer;
	private BufferTabs bufferTabs;
	private JEditTextArea textArea;
	private MarkerHighlight markerHighlight;

	private void propertiesChanged()
	{
		initTextArea();
		initBufferTabs();
	}

	private void initTextArea()
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
		markerHighlight.setMarkerHighlightColor(GUIUtilities.parseColor(
			jEdit.getProperty("view.gutter.markerColor")));
		markerHighlight.setRegisterHighlightColor(GUIUtilities.parseColor(
			jEdit.getProperty("view.gutter.registerColor")));
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
				jEdit.getProperty("view.gutter.focusBorderColor")),
				GUIUtilities.parseColor(jEdit.getProperty(
				"view.gutter.noFocusBorderColor")),
				textArea.getPainter().getBackground());
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

		loadStyles();

		// Set up the right-click popup menu
		textArea.setRightClickPopup(GUIUtilities
			.loadPopupMenu(view,"view.context"));
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

			textArea.getPainter().setStyles(styles);
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,this,e);
		}
	}

	private void initTextArea(JEditTextArea copy)
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

		try
		{
			int width = Integer.parseInt(jEdit.getProperty(
				"view.gutter.borderWidth"));
			myGutter.setBorder(width, GUIUtilities.parseColor(
				jEdit.getProperty("view.gutter.focusBorderColor")),
				GUIUtilities.parseColor(jEdit.getProperty(
				"view.gutter.noFocusBorderColor")),
				gutter.getBackground());
		}
		catch(NumberFormatException nf)
		{
			// retain the default border
		}

		myGutter.setBackground(gutter.getBackground());
		myGutter.setForeground(gutter.getForeground());
		myGutter.setHighlightedForeground(gutter.getHighlightedForeground());
		myGutter.setCurrentLineForeground(gutter.getCurrentLineForeground());

		textArea.setCaretBlinkEnabled(copy.isCaretBlinkEnabled());
		textArea.setElectricScroll(copy.getElectricScroll());

		myPainter.setStyles(painter.getStyles());

		// Set up the right-click popup menu
		textArea.setRightClickPopup(GUIUtilities
			.loadPopupMenu(view,"view.context"));
	}

	private void initBufferTabs()
	{
		if(jEdit.getBooleanProperty("view.showBufferTabs"))
		{
			if(bufferTabs == null)
			{
				bufferTabs = new BufferTabs(this);
				remove(textArea);
				add(bufferTabs);
			}

			bufferTabs.setTabPlacement(Integer.parseInt(
				jEdit.getProperty("view.bufferTabsPos")));
		}
		else
		{
			if(bufferTabs != null)
			{
				remove(bufferTabs);
				bufferTabs = null;
				add(textArea);
			}
			else if(textArea.getParent() == null)
			{
				add(textArea);
			}
		}

		revalidate();
		repaint();
	}

	private void updateTextArea()
	{
		textArea.setEditable(!buffer.isReadOnly());
	}

	private void handleBufferUpdate(BufferUpdate msg)
	{
		Buffer _buffer = msg.getBuffer();
		if(msg.getWhat() == BufferUpdate.CREATED)
		{
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
			else if(_buffer == recentBuffer)
				recentBuffer = null;
		}
		else if(msg.getWhat() == BufferUpdate.DIRTY_CHANGED
			|| msg.getWhat() == BufferUpdate.LOADED)
		{
			if(_buffer == buffer)
				updateTextArea();

			if(bufferTabs != null)
				bufferTabs.updateBufferTab(_buffer);
		}
		else if(msg.getWhat() == BufferUpdate.MARKERS_CHANGED)
		{
			if(_buffer == buffer)
				textArea.getGutter().repaint();
		}
		else if(msg.getWhat() == BufferUpdate.MODE_CHANGED)
		{
			if(_buffer == buffer)
				textArea.getPainter().repaint();
		}
	}
}

/*
 * Change Log:
 * $Log$
 * Revision 1.10  2000/07/22 03:27:03  sp
 * threaded I/O improved, autosave rewrite started
 *
 * Revision 1.9  2000/07/14 06:00:44  sp
 * bracket matching now takes syntax info into account
 *
 * Revision 1.8  2000/07/12 09:11:37  sp
 * macros can be added to context menu and tool bar, menu bar layout improved
 *
 * Revision 1.7  2000/06/24 03:46:48  sp
 * VHDL mode, bug fixing
 *
 * Revision 1.6  2000/05/23 04:04:52  sp
 * Marker highlight updates, next/prev-marker actions
 *
 * Revision 1.5  2000/05/22 12:05:45  sp
 * Markers are highlighted in the gutter, bug fixes
 *
 * Revision 1.4  2000/05/14 10:55:21  sp
 * Tool bar editor started, improved view registers dialog box
 *
 * Revision 1.3  2000/05/12 11:07:38  sp
 * Bug fixes, documentation updates
 *
 * Revision 1.2  2000/05/09 10:51:51  sp
 * New status bar, a few other things
 *
 * Revision 1.1  2000/05/07 05:48:30  sp
 * You can now edit several buffers side-by-side in a split view
 *
 */
