/*
 * AddAbbrevDialog.java - Displayed when expanding unknown abbrev
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
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.*;

public class AddAbbrevDialog extends EnhancedDialog
{
	public AddAbbrevDialog(View view, String abbrev)
	{
		super(view,jEdit.getProperty("add-abbrev.title"),true);

		this.view = view;
		this.abbrev = abbrev;

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		content.add(BorderLayout.NORTH,new JLabel(jEdit.getProperty(
			"add-abbrev.caption")));
		editor = new AbbrevEditor();
		editor.setBorder(new EmptyBorder(0,0,12,0));
		content.add(BorderLayout.CENTER,editor);

		Box box = new Box(BoxLayout.X_AXIS);
		box.add(Box.createGlue());
		global = new JButton(jEdit.getProperty("add-abbrev.global"));
		global.addActionListener(new ActionHandler());
		box.add(global);
		box.add(Box.createHorizontalStrut(6));
		modeSpecific = new JButton(jEdit.getProperty("add-abbrev.mode"));
		modeSpecific.addActionListener(new ActionHandler());
		box.add(modeSpecific);
		box.add(Box.createHorizontalStrut(6));
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(new ActionHandler());
		box.add(cancel);
		box.add(Box.createGlue());
		content.add(BorderLayout.SOUTH,box);

		GUIUtilities.requestFocus(this,editor.getBeforeCaretTextArea());
		pack();
		setResizable(false);
		setLocationRelativeTo(view);
		show();
	}

	public void ok()
	{
		// do nothing when Enter pressed
	}

	public void cancel()
	{
		dispose();
	}

	// private members
	private View view;
	private String abbrev;
	private AbbrevEditor editor;
	private JButton global;
	private JButton modeSpecific;
	private JButton cancel;

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(source == global)
			{
				Abbrevs.addGlobalAbbrev(abbrev,editor.getExpansion());
				Abbrevs.expandAbbrev(view,false);
			}
			else if(source == modeSpecific)
			{
				Abbrevs.addModeAbbrev(view.getBuffer().getMode().getName(),
					abbrev,editor.getExpansion());
				Abbrevs.expandAbbrev(view,false);
			}

			dispose();
		}
	}
}
