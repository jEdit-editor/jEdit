/*
 * StatusBar.java - The status bar displayed at the bottom of views
 * Copyright (C) 2001 Slava Pestov
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
import java.awt.*;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.*;

/**
 * The status bar, used for the following:
 * <ul>
 * <li>Displaying caret position information
 * <li>Displaying readNextChar() prompts
 * <li>Displaying the 'macro recording' message
 * <li>Displaying the status of the overwrite, multi select flags
 * <li>I/O progress
 * <li>And so on
 * </ul>
 *
 * @version $Id$
 * @author Slava Pestov
 * @since jEdit 3.2pre2
 */
public class StatusBar extends JPanel
{
	public StatusBar(View view)
	{
		super(new BorderLayout());

		this.view = view;

		caretStatus = new CaretStatus();
		add(BorderLayout.WEST,caretStatus);

		message = new JLabel();
		message.setForeground(UIManager.getColor("Button.foreground"));
		message.setFont(new Font("Dialog",Font.BOLD,10));
		add(BorderLayout.CENTER,message);

		ioProgress = new MiniIOProgress();
		add(BorderLayout.EAST,ioProgress);
	}

	public void setMessage(String message)
	{
		if(message == null)
		{
			if(view.getMacroRecorder() != null)
				this.message.setText(jEdit.getProperty("view.status.recording"));
			else
				this.message.setText(null);
		}
		else
			this.message.setText(message);
	}

	public void repaintCaretStatus()
	{
		caretStatus.repaint();
	}

	// private members
	private View view;
	private CaretStatus caretStatus;
	private JLabel message;
	private MiniIOProgress ioProgress;

	class CaretStatus extends JComponent
	{
		public CaretStatus()
		{
			CaretStatus.this.setDoubleBuffered(true);
			CaretStatus.this.setForeground(UIManager.getColor("Button.foreground"));
			CaretStatus.this.setBackground(UIManager.getColor("Label.background"));
			CaretStatus.this.setFont(new Font("Dialog",Font.BOLD,10));
		}

		public void paintComponent(Graphics g)
		{
			FontMetrics fm = g.getFontMetrics();

			JEditTextArea textArea = view.getTextArea();
			int dot = textArea.getCaretPosition();

			int currLine = textArea.getCaretLine();
			int start = textArea.getLineStartOffset(currLine);
			int numLines = textArea.getLineCount();

			String str = "col " + ((dot - start) + 1) + " : line "
				+ (currLine + 1) + " / " + numLines;

			g.drawString(str,2,(CaretStatus.this.getHeight()
				+ fm.getAscent()) / 2 - 1);
		}

		public Dimension getPreferredSize()
		{
			FontMetrics fm = CaretStatus.this.getToolkit()
				.getFontMetrics(CaretStatus.this.getFont());

			return new Dimension(fm.stringWidth("col 999 : line 9999 / 9999") + 4,
				fm.getHeight());
		}

		public Dimension getMaximumSize()
		{
			return CaretStatus.this.getPreferredSize();
		}
	}

	class MiniIOProgress extends JComponent
		implements WorkThreadProgressListener
	{
		public MiniIOProgress()
		{
			MiniIOProgress.this.setDoubleBuffered(true);
			MiniIOProgress.this.setForeground(UIManager.getColor("Button.foreground"));
			MiniIOProgress.this.setBackground(UIManager.getColor("Button.background"));

			icon = GUIUtilities.loadIcon("io.gif");
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
			MiniIOProgress.this.repaint();
		}

		public void paintComponent(Graphics g)
		{
			WorkThreadPool ioThreadPool = VFSManager.getIOThreadPool();
			if(ioThreadPool.getThreadCount() == 0)
				return;

			FontMetrics fm = g.getFontMetrics();

			if(ioThreadPool.getRequestCount() == 0)
				return;
			else
			{
				icon.paintIcon(this,g,MiniIOProgress.this.getWidth()
					- icon.getIconWidth() - 2,
					(MiniIOProgress.this.getHeight()
					- icon.getIconHeight()) / 2);
			}

			int progressHeight = MiniIOProgress.this.getHeight()
				/ ioThreadPool.getThreadCount();
			int progressWidth = MiniIOProgress.this.getWidth()
				- icon.getIconWidth() - 8;

			for(int i = 0; i < ioThreadPool.getThreadCount(); i++)
			{
				WorkThread thread = ioThreadPool.getThread(i);
				int max = thread.getProgressMaximum();
				if(!thread.isRequestRunning() || max == 0)
					continue;

				int value = thread.getProgressValue();
				double progressRatio = ((double)value / max);

				// when loading gzip files, for example,
				// progressValue (data read) can be larger
				// than progressMaximum (file size)
				progressRatio = Math.min(progressRatio,1.0);

				g.fillRect(0,progressHeight / 2 + i * progressHeight,
					(int)(progressRatio * progressWidth),1);
			}
		}

		public Dimension getPreferredSize()
		{
			return new Dimension(40,0);
		}

		public Dimension getMaximumSize()
		{
			return new Dimension(40,Integer.MAX_VALUE);
		}

		// private members
		private Icon icon;
	}
}
