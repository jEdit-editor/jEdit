/*
 * join_lines.java
 * Copyright (C) 1998 Slava Pestov
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
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class join_lines extends EditAction
{
	public join_lines()
	{
		super("join-lines");
	}

	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		if(!view.getTextArea().isEditable())
		{
			view.getToolkit().beep();
			return;
		}
		Buffer buffer = view.getBuffer();
		Element map = buffer.getDefaultRootElement();
		int lineNo = view.getTextArea().getCaretLine();
		Element lineElement = map.getElement(lineNo);
		int start = lineElement.getStartOffset();
		int end = lineElement.getEndOffset();
		if(end >= buffer.getLength())
		{
			view.getToolkit().beep();
			return;
		}
		Element nextLineElement = map.getElement(lineNo+1);
		int nextStart = nextLineElement.getStartOffset();
		int nextEnd = nextLineElement.getEndOffset();
		try
		{
			buffer.remove(end - 1,MiscUtilities.getLeadingWhiteSpace(
				buffer.getText(nextStart,nextEnd - nextStart)) + 1);
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
	}
}
