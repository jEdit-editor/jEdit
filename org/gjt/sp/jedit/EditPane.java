/*
 * EditPane.java - Text area and buffer switcher
 * Copyright (C) 2000, 2001 Slava Pestov
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

import javax.swing.event.*;
import javax.swing.*;
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

		buffer.endCompoundEdit();

		recentBuffer = this.buffer;
		if(recentBuffer != null)
			saveCaretInfo();
		this.buffer = buffer;

		textArea.setBuffer(buffer);

		if(!init)
		{
			view.updateTitle();
			view.updateMarkerMenus();

			if(bufferSwitcher != null)
			{
				if(bufferSwitcher.getSelectedItem() != buffer)
					bufferSwitcher.setSelectedItem(buffer);
			}
			caretStatus.repaint();

			EditBus.send(new EditPaneUpdate(this,EditPaneUpdate
				.BUFFER_CHANGED));
		}

		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				// only do this if we are the current edit pane
				if(view.getEditPane() == EditPane.this
					&& (bufferSwitcher == null
					|| !bufferSwitcher.isPopupVisible()))
				{
					focusOnTextArea();
				}
			}
		});

		// Only do this after all I/O requests are complete
		Runnable runnable = new Runnable()
		{
			public void run()
			{
				loadCaretInfo();
				buffer.checkModTime(view);
			}
		};

		if(buffer.isPerformingIO())
			VFSManager.runInAWTThread(runnable);
		else
			runnable.run();
	}

	/**
	 * Selects the previous buffer.
	 * @since jEdit 2.7pre2
	 */
	public void prevBuffer()
	{
		Buffer buffer = this.buffer.getPrev();
		if(buffer == null)
			setBuffer(jEdit.getLastBuffer());
		else
			setBuffer(buffer);
	}

	/**
	 * Selects the next buffer.
	 * @since jEdit 2.7pre2
	 */
	public void nextBuffer()
	{
		Buffer buffer = this.buffer.getNext();
		if(buffer == null)
			setBuffer(jEdit.getFirstBuffer());
		else
			setBuffer(buffer);
	}

	/**
	 * Selects the most recently edited buffer.
	 * @since jEdit 2.7pre2
	 */
	public void recentBuffer()
	{
		if(recentBuffer != null)
			setBuffer(recentBuffer);
		else
			getToolkit().beep();
	}

	/**
	 * Sets the focus onto the text area.
	 * @since jEdit 2.5pre2
	 */
	public void focusOnTextArea()
	{
		textArea.grabFocus();
		// trying to work around buggy focus handling in some
		// Java versions
//		if(!textArea.hasFocus())
//		{
//			textArea.processFocusEvent(new FocusEvent(textArea,
//				FocusEvent.FOCUS_GAINED));
//		}
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
		{
			propertiesChanged();
			loadBufferSwitcher();
		}
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
	EditPane(View view, Buffer buffer)
	{
		super(new BorderLayout());

		init = true;

		this.view = view;

		EditBus.addToBus(this);

		textArea = new JEditTextArea(view);

		textArea.add(JEditTextArea.LEFT_OF_SCROLLBAR,
			caretStatus = new CaretStatus(this));
		textArea.addCaretListener(new CaretHandler());
		add(BorderLayout.CENTER,textArea);
		markerHighlight = new MarkerHighlight();
		textArea.getGutter().addCustomHighlight(markerHighlight);

		if(buffer == null)
			setBuffer(jEdit.getFirstBuffer());
		else
			setBuffer(buffer);

		propertiesChanged();

		loadBufferSwitcher();

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
	private BufferSwitcher bufferSwitcher;
	private CaretStatus caretStatus;
	private JEditTextArea textArea;
	private MarkerHighlight markerHighlight;

	// buttons that control folding
	private JButton level1, level2, level3, expandAll;

	private void propertiesChanged()
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
		painter.setEOLMarkerColor(GUIUtilities.parseColor(
			jEdit.getProperty("view.eolMarkerColor")));
		painter.setWrapGuidePainted(jEdit.getBooleanProperty(
			"view.wrapGuide"));
		painter.setWrapGuideColor(GUIUtilities.parseColor(
			jEdit.getProperty("view.wrapGuideColor")));
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
		gutter.setExpanded(jEdit.getBooleanProperty(
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
		gutter.setFoldColor(GUIUtilities.parseColor(
			jEdit.getProperty("view.gutter.foldColor")));
		markerHighlight.setMarkerHighlightColor(GUIUtilities.parseColor(
			jEdit.getProperty("view.gutter.markerColor")));
		markerHighlight.setRegisterHighlightColor(GUIUtilities.parseColor(
			jEdit.getProperty("view.gutter.registerColor")));
		markerHighlight.setHighlightEnabled(jEdit.getBooleanProperty(
			"view.gutter.markerHighlight"));
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
			.loadPopupMenu("view.context"));
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

	private void loadBufferSwitcher()
	{
		if(jEdit.getBooleanProperty("view.showBufferSwitcher"))
		{
			if(bufferSwitcher == null)
			{
				bufferSwitcher = new BufferSwitcher(this);
				add(BorderLayout.NORTH,bufferSwitcher);
				bufferSwitcher.updateBufferList();
				revalidate();
			}
		}
		else if(bufferSwitcher != null)
		{
			remove(bufferSwitcher);
			revalidate();
			bufferSwitcher = null;
		}
	}

	private void handleBufferUpdate(BufferUpdate msg)
	{
		Buffer _buffer = msg.getBuffer();
		if(msg.getWhat() == BufferUpdate.CREATED)
		{
			if(bufferSwitcher != null)
				bufferSwitcher.updateBufferList();

			/* When closing the last buffer, the BufferUpdate.CLOSED
			 * handler doesn't call setBuffer(), because null buffers
			 * are not supported. Instead, it waits for the subsequent
			 * 'Untitled' file creation. */
			if(buffer.isClosed())
				setBuffer(jEdit.getFirstBuffer());
		}
		else if(msg.getWhat() == BufferUpdate.CLOSED)
		{
			if(bufferSwitcher != null)
				bufferSwitcher.updateBufferList();

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
		else if(msg.getWhat() == BufferUpdate.LOAD_STARTED)
		{
			if(_buffer == buffer)
			{
				caretStatus.repaint();
				textArea.setCaretPosition(0);
				textArea.getPainter().repaint();
			}
		}
		else if(msg.getWhat() == BufferUpdate.DIRTY_CHANGED)
		{
			if(_buffer == buffer)
			{
				caretStatus.repaint();
				if(bufferSwitcher != null)
				{
					if(buffer.isDirty())
						bufferSwitcher.repaint();
					else
						bufferSwitcher.updateBufferList();
				}
			}
		}
		else if(msg.getWhat() == BufferUpdate.LOADED)
		{
			if(_buffer == buffer)
			{
				textArea.repaint();
				textArea.updateScrollBars();
				if(bufferSwitcher != null)
					bufferSwitcher.updateBufferList();
				caretStatus.repaint();
			}
		}
		else if(msg.getWhat() == BufferUpdate.MARKERS_CHANGED)
		{
			if(_buffer == buffer)
				textArea.getGutter().repaint();
		}
		else if(msg.getWhat() == BufferUpdate.MODE_CHANGED)
		{
			if(_buffer == buffer)
			{
				textArea.getPainter().repaint();
				if(bufferSwitcher != null)
					bufferSwitcher.repaint();
			}
		}
	}

	class CaretHandler implements CaretListener
	{
		public void caretUpdate(CaretEvent evt)
		{
			caretStatus.repaint();
		}
	}
}
