/*
 * next_split.java
 * Copyright (C) 2000 Slava Pestov
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
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.jedit.*;

public class next_split extends EditAction
{
	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		JEditTextArea[] textAreas = view.getTextAreas();
		JEditTextArea textArea = view.getTextArea();
		for(int i = 0; i < textAreas.length; i++)
		{
			if(textArea == textAreas[i])
			{
				if(i == textAreas.length - 1)
					textArea = textAreas[0];
				else
					textArea = textAreas[i+1];
				textArea.requestFocus();
				break;
			}
		}
	}
}
