/*
 * SplashScreen.java - Splash screen
 * Copyright (C) 1998, 1999, 2000 Slava Pestov
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

import javax.swing.*;
import java.awt.*;

public class SplashScreen extends JFrame
{
	public SplashScreen()
	{
		super("jEdit is starting up");
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		JLabel label = new JLabel(new ImageIcon(getClass()
			.getResource("/org/gjt/sp/jedit/jedit_logo.jpg")));
		setContentPane(label);

		Dimension screen = getToolkit().getScreenSize();
		pack();
		setLocation((screen.width - getSize().width) / 2,
			(screen.height - getSize().height) / 2);
		show();

		label.paintImmediately(0,0,label.getWidth(),label.getHeight());
	}
}
