/*
 * open_file.java
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

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.io.File;
import org.gjt.sp.jedit.*;

public class open_file extends EditAction
{
	public open_file()
	{
		super("open-file");
	}

	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		String file = GUIUtilities.showFileDialog(view,view.getBuffer()
			.getFile().getParent(),JFileChooser.OPEN_DIALOG);

		if(file != null)
			jEdit.openFile(view,null,file,false,false);
	}
}
