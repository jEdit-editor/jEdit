/*
 * PluginListDownloadProgress.java - Plugin list download progress dialog
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

package org.gjt.sp.jedit.pluginmgr;

import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.*;

class PluginListDownloadProgress extends JDialog
{
	PluginListDownloadProgress(PluginManager window)
	{
		super(JOptionPane.getFrameForComponent(window),
			jEdit.getProperty("plugin-manager.progress.title"),true);

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		JLabel caption = new JLabel(jEdit.getProperty("plugin-manager.progress.caption"));
		caption.setBorder(new EmptyBorder(0,0,12,0));
		content.add(BorderLayout.NORTH,caption);

		Box box = new Box(BoxLayout.X_AXIS);
		box.add(Box.createGlue());
		JButton stop = new JButton(jEdit.getProperty("plugin-manager.progress.stop"));
		stop.addActionListener(new ActionHandler());
		stop.setMaximumSize(stop.getPreferredSize());
		box.add(stop);
		box.add(Box.createGlue());
		content.add(BorderLayout.CENTER,box);

		addWindowListener(new WindowHandler());
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		pack();
		setLocationRelativeTo(window);
		setResizable(false);
		show();
	}

	PluginList getPluginList()
	{
		return list;
	}

	// private members
	private PluginList list;
	private DownloadThread thread;

	class DownloadThread extends Thread
	{
		public void run()
		{
			list = new PluginList();
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					PluginListDownloadProgress.this.dispose();
				}
			});
		}
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			thread.stop();
			dispose();
		}
	}

	class WindowHandler extends WindowAdapter
	{
		boolean done;

		public void windowActivated(WindowEvent evt)
		{
			if(done)
				return;

			done = true;
			thread = new DownloadThread();
			thread.start();
		}
	}
}
