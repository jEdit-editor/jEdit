/*
 * SelectLineRange.java - Selects a range of lines
 * Copyright (C) 1999 Slava Pestov
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

import java.awt.*;
import java.awt.event.*;
import javax.swing.text.Element;
import javax.swing.*;
import org.gjt.sp.jedit.*;

/**
 * Select line range dialog.
 * @author Slava Pestov
 * @version $Id$
 */
public class SelectLineRange extends EnhancedDialog
implements ActionListener
{
	public SelectLineRange(View view)
	{
		super(view,jEdit.getProperty("selectlinerange.title"),true);

		buffer = view.getBuffer();

		getContentPane().add(BorderLayout.NORTH,new JLabel(
			jEdit.getProperty("selectlinerange.caption")));

		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(2,2));		
		panel.add(new JLabel(jEdit.getProperty("selectlinerange.start"),
			SwingConstants.RIGHT));
		panel.add(start = new JTextField());

		panel.add(new JLabel(jEdit.getProperty("selectlinerange.end"),
			SwingConstants.RIGHT));
		panel.add(end = new JTextField());
		getContentPane().add(BorderLayout.CENTER,panel);

		panel = new JPanel();
		panel.add(ok = new JButton(jEdit.getProperty("common.ok")));
		ok.addActionListener(this);
		panel.add(cancel = new JButton(jEdit.getProperty(
			"common.cancel")));
		cancel.addActionListener(this);
		getContentPane().add(BorderLayout.SOUTH,panel);

		getRootPane().setDefaultButton(ok);

		pack();
		setLocationRelativeTo(view);
		show();

		start.requestFocus();
	}

	public void actionPerformed(ActionEvent evt)
	{
		Object source = evt.getSource();

		if(source == ok)
			ok();
		else if(source == cancel)
			cancel();
	}

	// EnhancedDialog implementation
	public void ok()
	{
		int startLine;
		int endLine;

		try
		{
			startLine = Integer.parseInt(start.getText()) - 1;
			endLine = Integer.parseInt(end.getText()) - 1;
		}
		catch(NumberFormatException nf)
		{
			getToolkit().beep();
			return;
		}

		Element startElement = buffer.getDefaultRootElement()
			.getElement(startLine);
		Element endElement = buffer.getDefaultRootElement()
			.getElement(endLine);
		
		if(startElement == null || endElement == null)
		{
			getToolkit().beep();
			return;
		}

		int startOffset = startElement.getStartOffset();
		int endOffset = endElement.getEndOffset() - 1;

		((View)getParent()).getTextArea().select(startOffset,endOffset);

		dispose();
	}

	public void cancel()
	{
		dispose();
	}
	// end EnhancedDialog implementation

	// private members
	private Buffer buffer;
	private JTextField start;
	private JTextField end;
	private JButton ok;
	private JButton cancel;
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.11  2000/04/25 11:00:20  sp
 * FTP VFS hacking, some other stuff
 *
 * Revision 1.10  2000/02/15 07:44:30  sp
 * bug fixes, doc updates, etc
 *
 * Revision 1.9  1999/11/26 07:37:11  sp
 * Escape/enter handling code moved to common superclass, bug fixes
 *
 * Revision 1.8  1999/05/04 04:51:25  sp
 * Fixed HistoryTextField for Swing 1.1.1
 *
 * Revision 1.7  1999/04/24 07:34:46  sp
 * Documentation updates
 *
 * Revision 1.6  1999/04/23 07:35:11  sp
 * History engine reworking (shared history models, history saved to
 * .jedit-history)
 *
 * Revision 1.5  1999/04/19 05:44:34  sp
 * GUI updates
 *
 * Revision 1.4  1999/04/02 03:21:09  sp
 * Added manifest file, common strings such as OK, etc are no longer duplicated
 * many times in jedit_gui.props
 *
 * Revision 1.3  1999/03/20 05:23:32  sp
 * Code cleanups
 *
 * Revision 1.2  1999/03/19 08:32:22  sp
 * Added a status bar to views, Escape key now works in dialog boxes
 *
 */
