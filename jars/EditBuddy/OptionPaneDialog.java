/*
 * OptionPaneDialog.java - Option pane wrapper dialog
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

import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.gui.EnhancedDialog;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class OptionPaneDialog extends EnhancedDialog
{
	public OptionPaneDialog(OptionPane optPane)
	{
		super(null,jEdit.getProperty("options." + optPane.getName()
			+ ".label"),true);

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		this.optPane = optPane;
		optPane.init();

		content.add(BorderLayout.CENTER,optPane.getComponent());

		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons,BoxLayout.X_AXIS));
		buttons.setBorder(new EmptyBorder(12,0,0,0));
		buttons.add(Box.createGlue());
		ok = new JButton(jEdit.getProperty("common.ok"));
		ok.addActionListener(new ActionHandler());
		buttons.add(ok);
		getRootPane().setDefaultButton(ok);
		buttons.add(Box.createHorizontalStrut(6));
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(new ActionHandler());
		buttons.add(cancel);
		buttons.add(Box.createGlue());
		content.add(BorderLayout.SOUTH,buttons);

		pack();
		Dimension screen = getToolkit().getScreenSize();
		setLocation((screen.width - getSize().width) / 2,
			(screen.height - getSize().height) / 2);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		show();
	}

	public void ok()
	{
		optPane.save();
		dispose();
	}

	public void cancel()
	{
		dispose();
	}

	// private members
	private OptionPane optPane;
	private JButton ok, cancel;

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(evt.getSource() == ok)
				ok();
			else
				cancel();
		}
	}
}
