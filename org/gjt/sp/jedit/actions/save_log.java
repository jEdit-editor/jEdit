/*
 * save_log.java
 * Copyright (C) 1999 Slava Pestov
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
import java.io.IOException;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class save_log extends EditAction
{
	public save_log()
	{
		super("save-log");
	}

	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);

		String path = GUIUtilities.showFileDialog(
			view,MiscUtilities.constructPath(null,"jedit.log"),
			JFileChooser.SAVE_DIALOG);

		if(path != null)
		{
			try
			{
				Log.saveLog(path);
			}
			catch(IOException io)
			{
				Log.log(Log.ERROR,this,io);
				String[] args = { io.getMessage() };
				GUIUtilities.error(view,"ioerror",args);
			}
		}
	}
}
