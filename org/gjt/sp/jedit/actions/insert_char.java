/*
 * insert_char.java - Action
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

import java.awt.event.ActionEvent;
import org.gjt.sp.jedit.textarea.InputHandler;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class insert_char extends EditAction
implements InputHandler.NonRepeatable
{
	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);

		String noWordSep = (String)view.getBuffer().getProperty("noWordSep");

		String str = evt.getActionCommand();

		char ch = str.charAt(0);
		if(Abbrevs.getExpandOnInput() && !Character.isLetterOrDigit(ch)
			&& (noWordSep == null || noWordSep.indexOf(ch) == -1))
		{
			// non-word char entered
			// try to expand the abbrev first
			Abbrevs.expandAbbrev(view,false);
		}

		JEditTextArea textArea = view.getTextArea();
		int repeatCount = textArea.getInputHandler().getRepeatCount();

		if(textArea.isEditable())
		{
			StringBuffer buf = new StringBuffer();
			for(int i = 0; i < repeatCount; i++)
				buf.append(str);
			textArea.overwriteSetSelectedText(buf.toString());
		}
		else
		{
			view.getToolkit().beep();
		}
	}
}
