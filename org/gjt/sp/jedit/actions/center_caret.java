/*
 * center_caret.java
 * Copyright (C) 2000 Ollie Rutherfurd
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

import javax.swing.*;
import javax.swing.text.Element;
import java.awt.event.ActionEvent;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import java.awt.Rectangle;
import org.gjt.sp.jedit.*;

public class center_caret extends EditAction
{
	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		JEditTextArea textArea = view.getTextArea();
		Buffer buffer = view.getBuffer();
		Element map = buffer.getDefaultRootElement();

		int firstLine = textArea.getFirstLine();
		int gotoLine = firstLine + (textArea.getVisibleLines() / 2);

		if(gotoLine < 0 || gotoLine >= map.getElementCount())
		{
			view.getToolkit().beep();
			return;
		}

		Element element = map.getElement(gotoLine);
		view.getTextArea().setCaretPosition(element.getStartOffset());
	}
}
