/*
 * MiniIOProgress.java - Mini I/O progress monitor in menu bar
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
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.*;

public class MiniIOProgress extends JComponent implements WorkThreadProgressListener
{
	public MiniIOProgress()
	{
		setDoubleBuffered(true);
		setForeground(UIManager.getColor("Button.foreground"));
		setBackground(UIManager.getColor("Button.background"));

		icon = GUIUtilities.loadIcon("io.gif");
	}

	public void addNotify()
	{
		super.addNotify();
		VFSManager.getIOThreadPool().addProgressListener(this);
	}

	public void removeNotify()
	{
		super.removeNotify();
		VFSManager.getIOThreadPool().removeProgressListener(this);
	}

	public void progressUpdate(WorkThreadPool threadPool, int threadIndex)
	{
		repaint();
	}

	public void paintComponent(Graphics g)
	{
		WorkThreadPool ioThreadPool = VFSManager.getIOThreadPool();
		if(ioThreadPool.getThreadCount() == 0)
			return;

		FontMetrics fm = g.getFontMetrics();

		if(ioThreadPool.getRequestCount() == 0)
			return;
		else
		{
			icon.paintIcon(this,g,getWidth() - icon.getIconWidth() - 2,
				(getHeight() - icon.getIconHeight()) / 2);
		}

		int progressHeight = getHeight() / ioThreadPool.getThreadCount();
		int progressWidth = getWidth() - icon.getIconWidth() - 8;

		for(int i = 0; i < ioThreadPool.getThreadCount(); i++)
		{
			WorkThread thread = ioThreadPool.getThread(i);
			int max = thread.getProgressMaximum();
			if(!thread.isRequestRunning() || max == 0)
				continue;

			int value = thread.getProgressValue();
			double progressRatio = ((double)value / max);

			// when loading gzip files, for example,
			// progressValue (data read) can be larger
			// than progressMaximum (file size)
			progressRatio = Math.min(progressRatio,1.0);

			g.fillRect(0,progressHeight / 2 + i * progressHeight,
				(int)(progressRatio * progressWidth),1);
		}
	}

	public Dimension getPreferredSize()
	{
		return new Dimension(40,0);
	}

	public Dimension getMaximumSize()
	{
		return new Dimension(40,Integer.MAX_VALUE);
	}

	// private members
	private Icon icon;
}

/*
 * Change Log:
 * $Log$
 * Revision 1.2  2000/11/08 09:31:37  sp
 * Junk
 *
 * Revision 1.1  2000/10/30 07:14:04  sp
 * 2.7pre1 branched, GUI improvements
 *
 */
