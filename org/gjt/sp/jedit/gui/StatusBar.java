/*
 * StatusBar.java - Status bar
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

package org.gjt.sp.jedit.gui;

import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.*;

public class StatusBar extends JPanel
{
	public StatusBar(View view)
	{
		super(new BorderLayout());
		setBorder(new EmptyBorder(0,1,0,1));

		this.view = view;
		Border border = new CompoundBorder(new EmptyBorder(2,1,2,1),
			new LineBorder(UIManager.getColor("Label.foreground")));
		border = new CompoundBorder(border,new EmptyBorder(2,1,2,1));
		Font font = new Font("Dialog",Font.BOLD,10);

		caret = new CaretStatus();
		caret.setBorder(border);
		caret.setFont(font);
		add(BorderLayout.WEST,caret);

		statusMessage = new JLabel();
		statusMessage.setBorder(border);
		statusMessage.setFont(font);
		add(BorderLayout.CENTER,statusMessage);

		Box box = new Box(BoxLayout.X_AXIS);

		bufferStatus = new JLabel();
		bufferStatus.setBorder(border);
		bufferStatus.setFont(font);
		box.add(bufferStatus);

		ioProgress = new IOProgress();
		ioProgress.setBorder(border);
		ioProgress.setFont(font);
		box.add(ioProgress);

		add(BorderLayout.EAST,box);

		updateBufferStatus();
	}

	public void showStatus(String message)
	{
		if(message == null)
			message = view.getBuffer().getPath();

		statusMessage.setText(message);
	}

	public void updateBufferStatus()
	{
		Buffer buffer = view.getBuffer();
		StringBuffer buf = new StringBuffer();
		if(buffer.isDirty())
			buf.append(jEdit.getProperty("view.status.modified"));
		if(buffer.isNewFile())
			buf.append(jEdit.getProperty("view.status.new"));
		if(buffer.isReadOnly())
			buf.append(jEdit.getProperty("view.status.read-only"));
		if(view.getInputHandler().getMacroRecorder() != null)
			buf.append(jEdit.getProperty("view.status.recording"));
		if(buf.length() == 0)
			buf.append(jEdit.getProperty("view.status.ok"));

		bufferStatus.setText(buf.toString());
		statusMessage.setText(buffer.getPath());
	}

	public void updateCaretStatus()
	{
		caret.repaint();
	}

	// private members
	private View view;
	private CaretStatus caret;
	private JLabel statusMessage;
	private JLabel bufferStatus;
	private IOProgress ioProgress;

	class CaretStatus extends JComponent
	{
		CaretStatus()
		{
			CaretStatus.this.setDoubleBuffered(true);
			CaretStatus.this.setForeground(UIManager.getColor("Label.foreground"));
			CaretStatus.this.setBackground(UIManager.getColor("Label.background"));
		}

		public void paintComponent(Graphics g)
		{
			FontMetrics fm = g.getFontMetrics();

			JEditTextArea textArea = view.getTextArea();
			int dot = textArea.getCaretPosition();

			int currLine = textArea.getCaretLine();
			int start = textArea.getLineStartOffset(currLine);
			int numLines = textArea.getLineCount();

			String str;
			if(view.getInputHandler().isRepeatEnabled())
			{
				int repeatCount = view.getInputHandler()
					.getRepeatCount();
				if(repeatCount == 0)
					str = "";
				else
					str = String.valueOf(repeatCount);
				Object[] args = { str };
				str = jEdit.getProperty("view.status.repeat",args);
			}
			else
			{
				str = ("col " + ((dot - start) + 1) + " line "
					+ (currLine + 1) + "/"
					+ numLines + " "
					+ (((currLine + 1) * 100) / numLines) + "%");
			}

			Insets insets = CaretStatus.this.getBorder()
				.getBorderInsets(this);
			int offx = insets.left;
			g.drawString(str,insets.left,(CaretStatus.this.getHeight()
				+ fm.getAscent()) / 2 - 1);
		}

		public Dimension getPreferredSize()
		{
			FontMetrics fm = CaretStatus.this.getToolkit()
				.getFontMetrics(CaretStatus.this.getFont());
			Insets insets = CaretStatus.this.getBorder()
				.getBorderInsets(this);

			return new Dimension(fm.stringWidth("col 123 line 1234"
				+ " / 1234 100%"),fm.getHeight() + insets.top
				+ insets.bottom);
		}

		public Dimension getMaximumSize()
		{
			return getPreferredSize();
		}
	}

	class IOProgress extends JComponent implements WorkThreadProgressListener
	{
		IOProgress()
		{
			IOProgress.this.setDoubleBuffered(true);
			IOProgress.this.setForeground(UIManager.getColor("Label.foreground"));
			IOProgress.this.setBackground(UIManager.getColor("Label.background"));
			IOProgress.this.addMouseListener(new MouseHandler());
		}

		public void addNotify()
		{
			super.addNotify();
			VFSManager.getIOThreadPool().addProgressListener(this);
		}

		public void removeNotify()
		{
			super.removeNotify();
			VFSManager.getIOThreadPool().removeProgressListener(this);
		}

		public void progressUpdate(WorkThreadPool threadPool, int threadIndex)
		{
			IOProgress.this.repaint();
		}

		public void paintComponent(Graphics g)
		{
			FontMetrics fm = g.getFontMetrics();

			JEditTextArea textArea = view.getTextArea();
			int dot = textArea.getCaretPosition();

			int currLine = textArea.getCaretLine();
			int start = textArea.getLineStartOffset(currLine);
			int numLines = textArea.getLineCount();

			WorkThreadPool ioThreadPool = VFSManager.getIOThreadPool();
			String str = String.valueOf(ioThreadPool.getRequestCount());

			Insets insets = IOProgress.this.getBorder()
				.getBorderInsets(this);
			int offx = IOProgress.this.getWidth() - insets.right
				- fm.stringWidth(str);
			g.drawString(str,offx,(IOProgress.this.getHeight()
				+ fm.getAscent()) / 2 - 1);

			int progressHeight = (IOProgress.this.getHeight()
				- insets.top - insets.bottom)
				/ ioThreadPool.getThreadCount();
			int progressWidth = IOProgress.this.getWidth() - insets.left
				- insets.right - fm.stringWidth(str);

			for(int i = 0; i < ioThreadPool.getThreadCount(); i++)
			{
				WorkThread thread = ioThreadPool.getThread(i);
				int max = thread.getProgressMaximum();
				if(!thread.isRequestRunning() || max == 0)
					continue;

				double progressRatio = ((double)thread
					.getProgressValue() / max);

				// when loading gzip files, for example,
				// progressValue (data read) can be larger
				// than progressMaximum (file size)
				progressRatio = Math.min(progressRatio,1.0);

				g.fillRect(insets.left,insets.top
					+ i * progressHeight,
					(int)(progressRatio * progressWidth),
					progressHeight);
			}
		}

		public Dimension getPreferredSize()
		{
			FontMetrics fm = IOProgress.this.getToolkit()
				.getFontMetrics(IOProgress.this.getFont());
			Insets insets = IOProgress.this.getBorder()
				.getBorderInsets(this);

			return new Dimension(40,fm.getHeight() + insets.top
				+ insets.bottom);
		}

		public Dimension getMaximumSize()
		{
			return getPreferredSize();
		}

		class MouseHandler extends MouseAdapter
		{
			public void mouseClicked(MouseEvent evt)
			{
				new IOProgressMonitor();
			}
		}
	}
}

/*
 * Change Log:
 * $Log$
 * Revision 1.7  2000/07/26 07:48:44  sp
 * stuff
 *
 * Revision 1.6  2000/07/22 06:22:27  sp
 * I/O progress monitor done
 *
 * Revision 1.5  2000/07/22 03:27:03  sp
 * threaded I/O improved, autosave rewrite started
 *
 * Revision 1.4  2000/07/15 10:10:18  sp
 * improved printing
 *
 * Revision 1.3  2000/06/03 07:28:26  sp
 * User interface updates, bug fixes
 *
 * Revision 1.2  2000/05/14 10:55:22  sp
 * Tool bar editor started, improved view registers dialog box
 *
 * Revision 1.1  2000/05/09 10:51:52  sp
 * New status bar, a few other things
 *
 */
