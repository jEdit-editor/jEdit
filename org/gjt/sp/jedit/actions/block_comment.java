/*
 * block_comment.java
 * Copyright (C) 1999, 2000 Slava Pestov
 *
 * This	free software; you can redistribute it and/or
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

public class block_comment extends EditAction
{
	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		JEditTextArea textArea = view.getTextArea();
		Buffer buffer = view.getBuffer();
		String comment = (String)buffer.getProperty("blockComment");
		if(!textArea.isEditable() || comment == null)
		{
			view.getToolkit().beep();
			return;
		}

		comment = comment + ' ';

		int startLine = textArea.getSelectionStartLine();
		int endLine = textArea.getSelectionEndLine();

		buffer.beginCompoundEdit();

		try
		{
			for(int i = startLine; i <= endLine; i++)
			{
				buffer.insertString(textArea.getLineStartOffset(i),
					comment,null);
			}
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
		finally
		{
			buffer.endCompoundEdit();
		}

		textArea.select(textArea.getCaretPosition(),
			textArea.getCaretPosition());
	}
}
