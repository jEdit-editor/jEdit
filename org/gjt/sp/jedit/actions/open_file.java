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
import java.util.Hashtable;
import org.gjt.sp.jedit.browser.VFSBrowser;
import org.gjt.sp.jedit.io.VFSSession;
import org.gjt.sp.jedit.*;

public class open_file extends EditAction
{
	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		VFSSession[] vfsSession = new VFSSession[1];
		String[] files = GUIUtilities.showVFSFileDialog(view,null,
			VFSBrowser.OPEN_DIALOG,true,vfsSession);

		Buffer buffer = null;
		if(files != null)
		{
			Hashtable props = new Hashtable();
			props.put(Buffer.VFS_SESSION_HACK,vfsSession[0]);
			for(int i = 0; i < files.length; i++)
			{
				buffer = jEdit.openFile(null,null,files[i],
					false,false,props);
			}
		}

		if(buffer != null)
			view.setBuffer(buffer);
	}
}
