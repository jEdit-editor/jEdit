/*
 * tab.java
 * Copyright (C) 1998, 1999 Slava Pestov
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
import javax.swing.text.Element;
import java.awt.event.ActionEvent;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.*;

public class tab extends EditAction
{
	public void actionPerformed(ActionEvent evt)
	{
                View view = getView(evt);
                JEditTextArea textArea = view.getTextArea();

                if(!textArea.isEditable() || textArea.getSelectionStart()
                	== textArea.getSelectionEnd())
                {
                	view.getToolkit().beep();
                	return;
                }

		textArea.setSelectedText(doTab(textArea.getSelectedText(),
			view.getBuffer().getTabSize()));
	}

	private String doTab(String in, int tabSize)
	{
		StringBuffer buf = new StringBuffer();
		int width = 0;
		int whitespace = 0;
		for(int i = 0; i < in.length(); i++)
		{
			switch(in.charAt(i))
			{
			case ' ':
				whitespace++;
				width++;
				break;
			case '\t':
				int tab = tabSize - (width % tabSize);
				width += tab;
				whitespace += tab;
				break;
			case '\n':
				if(whitespace != 0)
				{
					buf.append(MiscUtilities
						.createWhiteSpace(whitespace,tabSize));
				}
				whitespace = 0;
				width = 0;
				buf.append('\n');
				break;
			default:
				if(whitespace != 0)
				{
					buf.append(MiscUtilities
						.createWhiteSpace(whitespace,tabSize));
				}
				buf.append(in.charAt(i));
				width++;
				break;
			}
		}

		if(whitespace != 0)
		{
			buf.append(MiscUtilities.createWhiteSpace(whitespace,tabSize));
		}

                return buf.toString();
	}
}
