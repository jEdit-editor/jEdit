/*
 * next_marker.java
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

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Vector;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.*;

public class next_marker extends EditAction
{
	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		JEditTextArea textArea = view.getTextArea();
		int caret = textArea.getCaretPosition();

		Vector markers = view.getBuffer().getMarkers();
		Marker marker = null;
		for(int i = 0; i < markers.size(); i++)
		{
			Marker _marker = (Marker)markers.elementAt(i);
			if(_marker.getStart() > caret)
			{
				marker = _marker;
				break;
			}
		}

		if(marker != null)
			textArea.setCaretPosition(marker.getStart());
		else
			view.getToolkit().beep();
	}
}
