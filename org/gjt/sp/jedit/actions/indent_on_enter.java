/*
 * indent_on_enter.java - Action
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
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.*;

public class indent_on_enter extends EditAction
{
        public indent_on_enter()
        {
                super("indent-on-enter");
	}

        public void actionPerformed(ActionEvent evt)
        {
                View view = getView(evt);
                Buffer buffer = view.getBuffer();
                JEditTextArea textArea = view.getTextArea();

		textArea.setSelectedText("\n");

                Mode mode = buffer.getMode();
		int selStart = textArea.getSelectionStart();
		int selEnd = textArea.getSelectionEnd();

                if(selStart == selEnd
			&& "on".equals(buffer.getProperty("indentOnEnter")))
		{
			mode.indentLine(buffer,view,textArea.getCaretLine(),false);
                }
        }
}
