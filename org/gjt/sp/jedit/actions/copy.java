/*
 * copy.java
 * Copyright (C) 1998, 2000 Slava Pestov
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

import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.jedit.*;

public class copy extends EditAction
{
	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		JEditTextArea textArea = view.getTextArea();

		String selection = textArea.getSelectedText();
		if(selection == null)
			return;

		Clipboard clipboard = view.getToolkit().getSystemClipboard();

		int repeatCount = view.getInputHandler().getRepeatCount();
		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < repeatCount; i++)
			buf.append(selection);

		clipboard.setContents(new StringSelection(buf.toString()),null);
	}

	public boolean isRepeatable()
	{
		return false;
	}
}
