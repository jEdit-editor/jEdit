/*
 * reload_modes.java
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

package org.gjt.sp.jedit.actions;

import java.awt.event.ActionEvent;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.*;

public class reload_modes extends EditAction
{
	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		view.showWaitCursor();

		String path;
		String settingsDirectory = jEdit.getSettingsDirectory();

		if(settingsDirectory == null)
			path = null;
		else
			path = MiscUtilities.constructPath(settingsDirectory,
				"mode-cache");

		jEdit.createModeCache(path);

		Buffer[] buffers = jEdit.getBuffers();
		for(int i = 0; i < buffers.length; i++)
			buffers[i].setMode();

		View[] views = jEdit.getViews();
		for(int i = 0; i < views.length; i++)
		{
			JEditTextArea[] textAreas = views[i].getTextAreas();
			for(int j = 0; j < textAreas.length; j++)
				textAreas[j].repaint();
		}

		view.hideWaitCursor();
	}
}
