/*
 * goto_end_indent.java
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
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class goto_end_indent extends EditAction
{
	public goto_end_indent()
	{
		super("goto-end-indent");
	}

	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		Buffer buffer = view.getBuffer();
		Element map = buffer.getDefaultRootElement();
		Element lineElement = map.getElement(view.getTextArea().getCaretLine());
		int start = lineElement.getStartOffset();
		int end = lineElement.getEndOffset();
		try
		{
			String str = buffer.getText(start,end - start);
			int whitespace = start + MiscUtilities
				.getLeadingWhiteSpace(str);
			view.getTextArea().setCaretPosition(whitespace);
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
	}
}
