/*
 * Welcome.java - Welcome to jEdit dialog box
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

import javax.swing.border.EmptyBorder;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.IOException;
import org.gjt.sp.jedit.gui.EnhancedDialog;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class Welcome extends EnhancedDialog
{
	public Welcome(View view)
	{
		super(view,jEdit.getProperty("welcome.title"),false);
		setContentPane(new WelcomePanel());
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		pack();
		setLocationRelativeTo(view);
		show();
	}

	public void ok()
	{
		dispose();
	}

	public void cancel()
	{
		dispose();
	}

	class WelcomePanel extends JPanel
	{
		WelcomePanel()
		{
			super(new BorderLayout(12,12));
			setBorder(new EmptyBorder(12,12,12,12));

			JLabel label = new JLabel(jEdit.getProperty("welcome.caption"));
			label.setFont(new Font("SansSerif",Font.PLAIN,24));
			label.setForeground(UIManager.getColor("Button.foreground"));
			add(BorderLayout.NORTH,label);

			welcomeText = new JEditorPane();
			welcomeText.setEditable(false);
			welcomeText.setContentType("text/html");
			JScrollPane scroller = new JScrollPane(welcomeText);
			scroller.setPreferredSize(new Dimension(600,300));
			add(BorderLayout.CENTER,scroller);

			Box buttons = new Box(BoxLayout.X_AXIS);

			buttons.add(Box.createGlue());

			close = new JButton(jEdit.getProperty("common.close"));
			close.addActionListener(new ActionHandler());
			buttons.add(close);
			Welcome.this.getRootPane().setDefaultButton(close);

			add(BorderLayout.SOUTH,buttons);

			try
			{
				welcomeText.setPage(getClass().getResource("welcome.html"));
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,this,e);
			}
		}

		// private members
		private JButton close;
		private JEditorPane welcomeText;

		class ActionHandler implements ActionListener
		{
			public void actionPerformed(ActionEvent evt)
			{
				Object source = evt.getSource();
				if(source == close)
					dispose();
			}
		}
	}
}
