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
import java.awt.*;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.*;

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
		Dimension dim = statusMessage.getPreferredSize();
		dim.width = 0;
		statusMessage.setPreferredSize(dim);
		add(BorderLayout.CENTER,statusMessage);

		bufferStatus = new JLabel();
		bufferStatus.setBorder(border);
		bufferStatus.setFont(font);
		add(BorderLayout.EAST,bufferStatus);

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
				if(repeatCount == 1)
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
			return new Dimension(180,0);
		}
	}
}

/*
 * Change Log:
 * $Log$
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
