/*
 * indent_on_tab.java - Action
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
import javax.swing.text.Element;
import java.awt.event.ActionEvent;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class indent_on_tab extends EditAction
{
        public void actionPerformed(ActionEvent evt)
        {
                View view = getView(evt);
                Buffer buffer = view.getBuffer();
                JEditTextArea textArea = view.getTextArea();

		// expand current word
		if(Abbrevs.expandOnUserInput())
			Abbrevs.expandAbbrev(buffer,textArea);

                Mode mode = buffer.getMode();
		int selStart = textArea.getSelectionStart();
		int selEnd = textArea.getSelectionEnd();

                if(selStart == selEnd
			&& "on".equals(buffer.getProperty("indentOnTab"))
			&& mode.indentLine(buffer,view,textArea
			.getSelectionStartLine(),false))
                {
				return;
                }
                if("yes".equals(buffer.getProperty("noTabs")))
                {
			Element map = buffer.getDefaultRootElement();
			Element lineElement = map.getElement(textArea.getSelectionStartLine());

			try
			{
				String line = buffer.getText(lineElement
					.getStartOffset(),selStart -
					lineElement.getStartOffset());

				textArea.setSelectedText(createSoftTab(line,
					buffer.getTabSize()));
			}
			catch(BadLocationException bl)
			{
				Log.log(Log.ERROR,this,bl);
			}
                }
                else
                        textArea.setSelectedText("\t");
        }

	private String createSoftTab(String line, int tabSize)
	{
		int pos = 0;

		for(int i = 0; i < line.length(); i++)
		{
			switch(line.charAt(pos))
			{
			case '\t':
				pos = 0;
				break;
			default:
				if(++pos >= tabSize)
					pos = 0;
				break;
			}
		}

		return MiscUtilities.createWhiteSpace(tabSize - pos,0);
	}
}
