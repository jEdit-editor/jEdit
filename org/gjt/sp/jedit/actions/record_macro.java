/*
 * record_macro.java
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

import javax.swing.text.BadLocationException;
import java.awt.event.ActionEvent;
import java.io.File;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class record_macro extends EditAction
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

		if(view.getTextArea().getInputHandler().getMacroRecorder() != null)
		{
			GUIUtilities.error(view,"already-recording",new String[0]);
			return;
		}

		String name = GUIUtilities.input(view,"record",null);
		if(name == null)
			return;

		name = name.replace(' ','_');

		Buffer buffer = jEdit.openFile(null,null,
			MiscUtilities.constructPath(settings,"macros",
			name + ".macro"),
			false,true);

		if(buffer == null)
			return;

		try
		{
			buffer.remove(0,buffer.getLength());
			buffer.insertString(0,jEdit.getProperty("macro.header"),null);
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}

		view.pushStatus(jEdit.getProperty("view.status.recording"));
		Macros.beginRecording(view,name,buffer);
	}

	public boolean isRecordable()
	{
		return false;
	}
}
