/*
 * indent_lines.java
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
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.*;

public class indent_lines extends EditAction
{
        public void actionPerformed(ActionEvent evt)
        {
                View view = getView(evt);
		JEditTextArea textArea = view.getTextArea();
                Buffer buffer = view.getBuffer();
		Mode mode = buffer.getMode();

		String actionCommand = evt.getActionCommand();

		/* This action is invoked with an action command when
		* the user presses } or some 'indentCloseBrackets' key.
		* The idea is that when the user presses } in a Java file,
		* the line will be reindented properly.
		*
		* however, since key bindings act globally, we don't attempt
		* to reindent the line in modes which don't have this key in
		* 'indentCloseBrackets'.
		*/
		if(actionCommand != null)
		{
			textArea.overwriteSetSelectedText(actionCommand);

			String indentCloseBrackets = (String)buffer
				.getProperty("indentCloseBrackets");
			if(indentCloseBrackets == null)
				return;
			if(indentCloseBrackets.indexOf(actionCommand) == -1)
				return;
		}

		int start = textArea.getSelectionStartLine();
		int end = textArea.getSelectionEndLine();

		buffer.beginCompoundEdit();
		for(int i = start; i <= end; i++)
		{
			mode.indentLine(buffer,view,i,true);
		}
		buffer.endCompoundEdit();
	}
}
