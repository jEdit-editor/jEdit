/*
 * VFSBrowserFrame.java - VFS browser frame
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

import javax.swing.*;
import java.awt.BorderLayout;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;

/**
 * Wraps the VFS browser in a frame.
 * @author Slava Pestov
 * @version $Id$
 */
public class VFSBrowserFrame extends JFrame
{
	public VFSBrowserFrame(View view, String path)
	{
		super(jEdit.getProperty("vfs.browser.title"));

		getContentPane().add(new VFSBrowser(view,path,VFSBrowser.BROWSER));

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		pack();
		GUIUtilities.loadGeometry(this,"vfs.browser");
		show();
	}

	public void dispose()
	{
		GUIUtilities.saveGeometry(this,"vfs.browser");
		super.dispose();
	}
}

/*
 * Change Log:
 * $Log$
 * Revision 1.2  2000/07/30 09:04:19  sp
 * More VFS browser hacking
 *
 * Revision 1.1  2000/07/29 12:24:08  sp
 * More VFS work, VFS browser started
 *
 */
