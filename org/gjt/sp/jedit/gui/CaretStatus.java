/*
 * CaretStatus.java - Caret status display
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

import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.*;
import java.awt.*;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.*;

public class CaretStatus extends JComponent
{
	public CaretStatus(EditPane editPane)
	{
		setDoubleBuffered(true);
		setForeground(UIManager.getColor("Label.foreground"));
		setBackground(UIManager.getColor("Label.background"));
		setFont(new Font("Dialog",Font.BOLD,10));

		if(UIManager.getLookAndFeel() instanceof MetalLookAndFeel)
		{
			Color c = MetalLookAndFeel.getControlDarkShadow();
			setBorder(new javax.swing.border.MatteBorder(1,0,0,1,c));
		}

		this.editPane = editPane;
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

		String str = "col " + ((dot - start) + 1) + " : line "
			+ (currLine + 1) + " / " + numLines;

		g.drawString(str,2,(getHeight() + fm.getAscent()) / 2 - 1);
	}

	public Dimension getPreferredSize()
	{
		FontMetrics fm = getToolkit().getFontMetrics(getFont());

		return new Dimension(fm.stringWidth("col 999 : line 9999 / 9999") + 4,
			fm.getHeight());
	}

	public Dimension getMaximumSize()
	{
		return getPreferredSize();
	}

	// private members
	private EditPane editPane;
}
