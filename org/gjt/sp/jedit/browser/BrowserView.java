/*
 * BrowserView.java - A VFS browser file view
 * Copyright (C) 2000 Slava Pestov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit.browser;

import javax.swing.JPanel;
import java.awt.*;
import org.gjt.sp.jedit.io.VFS;

/**
 * A browser view.
 * @author Slava Pestov
 * @version $Id$
 */
public abstract class BrowserView extends JPanel
{
	public BrowserView(VFSBrowser browser)
	{
		this.browser = browser;
	}

	/**
	 * Returns (0,0) to make it easier to use browser views in split
	 * panes.
	 */
	public Dimension getMinimumSize()
	{
		return new Dimension(0,0);
	}

	/**
	 * Returns the currently selected files.
	 */
	public abstract VFS.DirectoryEntry[] getSelectedFiles();

	/**
	 * A directory has been loaded.
	 * @param path The directry path
	 * @param directory The directory listing
	 */
	public abstract void directoryLoaded(String path, VFS.DirectoryEntry[] directory);

	/**
	 * Called when a buffer is opened or closed. Views that reflect
	 * this should update themselves accordingly in this method.
	 */
	public abstract void updateFileView();

	// protected members
	protected VFSBrowser browser;

	protected void showFilePopup(VFS.DirectoryEntry file, Component comp, Point p)
	{
	}
}

/*
 * Change Log:
 * $Log$
 * Revision 1.1  2000/07/30 09:04:19  sp
 * More VFS browser hacking
 *
 */
