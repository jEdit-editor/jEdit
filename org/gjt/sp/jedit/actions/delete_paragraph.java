/*
 * delete_paragraph.java
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
import java.awt.event.ActionEvent;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.util.Log;

public class delete_paragraph extends EditAction
{
	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		JEditTextArea textArea = view.getTextArea();

		if(!textArea.isEditable())
		{
			view.getToolkit().beep();
			return;
		}

		Buffer buffer = view.getBuffer();
		int lineNo = textArea.getCaretLine();

		int start = 0, end = textArea.getDocumentLength();

		for(int i = lineNo - 1; i >= 0; i--)
		{
			if(textArea.getLineLength(i) == 0)
			{
				start = textArea.getLineStartOffset(i);
				break;
			}
		}

		for(int i = lineNo + 1; i < textArea.getLineCount(); i++)
		{
			if(textArea.getLineLength(i) == 0)
			{
				end = textArea.getLineStartOffset(i);
				break;
			}
		}

		try
		{
			buffer.remove(start,end - start);
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
	}
}
