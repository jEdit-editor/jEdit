/*
 * exit.java
 * Copyright (C) 1998, 2000 Slava Pestov
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

import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import org.gjt.sp.jedit.*;

public class exit extends EditAction
{
	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);

		boolean ok;
		if(jEdit.getBooleanProperty("confirmExit"))
		{
			int result = JOptionPane.showConfirmDialog(view,
				jEdit.getProperty("confirm-exit.message"),
				jEdit.getProperty("confirm-exit.title"),
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE);

			ok = (result == JOptionPane.YES_OPTION);
		}
		else
			ok = true;

		if(ok)
			jEdit.exit(view,true);
	}
}
