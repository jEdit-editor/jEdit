/*
 * VFSBrowserDockable.java - Dockable VFS browser
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

package org.gjt.sp.jedit.browser;

import java.awt.Component;
import org.gjt.sp.jedit.gui.DockableWindow;
import org.gjt.sp.jedit.View;

/**
 * Wraps the VFS browser in a dockable window.
 * @author Slava Pestov
 * @version $Id$
 */
public class VFSBrowserDockable implements DockableWindow
{
	public static final String NAME = "vfs.browser";

	public VFSBrowserDockable(View view, String path)
	{
		comp = new VFSBrowser(view,path,VFSBrowser.BROWSER,false);
	}

	public String getName()
	{
		return NAME;
	}

	public Component getComponent()
	{
		return comp;
	}

	// private members
	private Component comp;
}

/*
 * Change Log:
 * $Log$
 * Revision 1.1  2000/08/13 07:35:23  sp
 * Dockable window API
 *
 */
