/*
 * open_file.java
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

import java.awt.event.ActionEvent;
import org.gjt.sp.jedit.browser.VFSBrowser;
import org.gjt.sp.jedit.gui.CommandLine;
import org.gjt.sp.jedit.*;

public class open_file extends EditAction
{
	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		String path = evt.getActionCommand();

		if(path == null)
		{
			CommandLine commandLine = getCommandLine(evt);
			if(commandLine != null)
			{
				commandLine.promptLine(jEdit.getProperty(
					"view.status.open-file"),this);
			}
			else
				showOpenDialog(view);
		}
		else if(path.length() == 0)
			showOpenDialog(view);
		else
			jEdit.openFile(view,path);

	}

	private void showOpenDialog(View view)
	{
		String[] files = GUIUtilities.showVFSFileDialog(view,null,
			VFSBrowser.OPEN_DIALOG,true);

		Buffer buffer = null;
		if(files != null)
		{
			for(int i = 0; i < files.length; i++)
			{
				Buffer newBuffer = jEdit.openFile(null,files[i]);
				if(newBuffer != null)
					buffer = newBuffer;
			}
		}

		if(buffer != null)
			view.setBuffer(buffer);
	}
}
