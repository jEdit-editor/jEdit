/*
 * SplashScreen.java - Splash screen
 * Copyright (C) 1998 Slava Pestov
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
import java.net.URL;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;

public class SplashScreen extends JWindow
{
	public SplashScreen()
	{
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		JPanel splash = new JPanel(new BorderLayout());
		URL url = getClass().getResource("/org/gjt/sp/jedit/jedit_logo.gif");
		if(url != null)
		{
			splash.add(new JLabel(new ImageIcon(url)),
				BorderLayout.CENTER);
		}

		splash.add(BorderLayout.NORTH,new JLabel("jEdit "
			+ jEdit.getVersion(),JLabel.CENTER));

		progress = new JProgressBar(0,6);
		progress.setStringPainted(true);
		progress.setString("jEdit is starting up...");
		splash.add(BorderLayout.SOUTH,progress);

		splash.setBorder(new SoftBevelBorder(SoftBevelBorder.RAISED));

		setContentPane(splash);

		Dimension screen = getToolkit().getScreenSize();
		pack();
		setLocation((screen.width - getSize().width) / 2,
			(screen.height - getSize().height) / 2);
		show();
	}

	public void advance(final String text)
	{
		try
		{
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run()
				{
					progress.setValue(progress.getValue() + 1);
					progress.setString(text);
				}
			});
			Thread.yield();
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,this,e);
		}
	}

	// private members
	private JProgressBar progress;
}
