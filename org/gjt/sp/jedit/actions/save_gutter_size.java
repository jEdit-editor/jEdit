/*
 * save_gutter_size.java
 * Copyright (C) 2000 mike dillon
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
import org.gjt.sp.jedit.textarea.Gutter;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.EditAction;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.View;

public class save_gutter_size extends EditAction
{
	public void actionPerformed(ActionEvent evt)
	{
		jEdit.setProperty("view.gutter.width", Integer.toString(
			getView(evt).getTextArea().getGutter().getGutterWidth()));
	}
}
