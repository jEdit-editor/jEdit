/*
 * edit_user_macros.java
 * Copyright (C) 1999, 2000 Slava Pestov
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

package org.gjt.sp.jedit.actions;

import javax.swing.JFileChooser;
import java.awt.event.ActionEvent;
import java.io.File;
import org.gjt.sp.jedit.browser.*;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.jedit.*;

public class edit_user_macros extends EditAction
{
	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);

		String settings = jEdit.getSettingsDirectory();

		if(settings == null)
		{
			GUIUtilities.error(view,"no-settings",new String[0]);
			return;
		}

		DockableWindowManager dockableWindowManager
			= view.getDockableWindowManager();

		dockableWindowManager.showDockableWindow(VFSBrowserDockable.NAME);
		VFSBrowser browser = (VFSBrowser)dockableWindowManager
			.getDockableWindow(VFSBrowserDockable.NAME)
			.getComponent();

		browser.setDirectory(MiscUtilities.constructPath(settings,"macros"));
	}
}
