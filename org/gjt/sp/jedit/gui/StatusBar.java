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
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.*;

public class StatusBar extends JPanel
{
	public StatusBar(EditPane editPane)
	{
		super(new BorderLayout(2,0));
		setBorder(new EmptyBorder(0,1,1,2));

		this.editPane = editPane;

		Font font = new Font("Dialog",Font.BOLD,10);

		buffers = new JComboBox();
		buffers.setRenderer(new BufferCellRenderer());
		buffers.setMaximumRowCount(10);
		buffers.setFont(font);
		buffers.addActionListener(new ActionHandler());
		add(BorderLayout.CENTER,buffers);

		caret = new CaretStatus();
		caret.setFont(font);
		add(BorderLayout.EAST,caret);

		updateBuffers();
	}

	public void selectBuffer(Buffer buffer)
	{
		buffers.setSelectedItem(buffer);
	}

	public void updateBuffers()
	{
		updating = true;
		buffers.setModel(new DefaultComboBoxModel(jEdit.getBuffers()));
		buffers.setSelectedItem(editPane.getBuffer());
		updating = false;
	}

	public void updateBufferStatus()
	{
		buffers.repaint();
	}

	public void updateCaretStatus()
	{
		caret.repaint();
	}

	// private members
	private EditPane editPane;
	private JComboBox buffers;
	private CaretStatus caret;
	private boolean updating;

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(!updating)
			{
				Buffer buffer = (Buffer)buffers.getSelectedItem();
				if(buffer != null)
					editPane.setBuffer(buffer);
			}
		}
	}

	class BufferCellRenderer extends DefaultListCellRenderer
	{
		public Component getListCellRendererComponent(
			JList list, Object value, int index,
			boolean isSelected, boolean cellHasFocus)
		{
			super.getListCellRendererComponent(list,null,index,
				isSelected,cellHasFocus);
			Buffer buffer = (Buffer)value;
			if(buffer == null)
			{
				setIcon(null);
				setText(null);
			}
			else
			{
				setIcon(buffer.getIcon());
				setText(buffer.getName() + " ("
					+ buffer.getVFS().getParentOfPath(
					buffer.getPath()) + ")");
				return this;
			}
			return this;
		}
	}

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
			if(!editPane.getBuffer().isLoaded())
				return;

			FontMetrics fm = g.getFontMetrics();

			JEditTextArea textArea = editPane.getTextArea();
			int dot = textArea.getCaretPosition();

			int currLine = textArea.getCaretLine();
			int start = textArea.getLineStartOffset(currLine);
			int numLines = textArea.getLineCount();

			String str = ((dot - start) + 1) + " : " + (currLine + 1)
				+ " / " + numLines;

			g.drawString(str,CaretStatus.this.getWidth()
				- fm.stringWidth(str),(CaretStatus.this.getHeight() 
				+ fm.getAscent()) / 2 - 1);
		}

		public Dimension getPreferredSize()
		{
			FontMetrics fm = CaretStatus.this.getToolkit()
				.getFontMetrics(CaretStatus.this.getFont());

			return new Dimension(fm.stringWidth("99 : 9999 / 9999"),
				fm.getHeight());
		}

		public Dimension getMaximumSize()
		{
			return getPreferredSize();
		}
	}
}

/*
 * Change Log:
 * $Log$
 * Revision 1.10  2000/10/30 07:14:04  sp
 * 2.7pre1 branched, GUI improvements
 *
 * Revision 1.9  2000/10/15 04:10:34  sp
 * bug fixes
 *
 * Revision 1.8  2000/09/01 11:31:01  sp
 * Rudimentary 'command line', similar to emacs minibuf
 *
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
