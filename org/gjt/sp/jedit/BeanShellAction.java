/*
 * BeanShellAction.java - BeanShell action
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

package org.gjt.sp.jedit;

import java.awt.event.ActionEvent;
import java.awt.*;

public class BeanShellAction extends EditAction
{
	public BeanShellAction(String name, String actionPerformed,
		String isSelected)
	{
		super(name);

		this.actionPerformed = actionPerformed;

		if(isSelected != null)
		{
			cachedIsSelected = "_action" + counter++ + "()";
			BeanShell.eval(null,cachedIsSelected + "{"
				+ isSelected + "}");
		}
	}

	public void actionPerformed(ActionEvent evt)
	{
		if(cachedActionPerformed == null)
		{
			cachedActionPerformed = "_action" + counter++ + "()";
			BeanShell.eval(null,cachedActionPerformed + "{"
				+ actionPerformed + "}");
		}
		BeanShell.eval(getView(evt),cachedActionPerformed);
	}

	public boolean isToggle()
	{
		return cachedIsSelected != null;
	}

	public boolean isSelected(Component comp)
	{
		if(cachedIsSelected == null)
			return false;

		return(Boolean.TRUE.equals(BeanShell.eval(getView(comp),
			cachedIsSelected)));
	}

	// private members
	private static int counter;

	private String actionPerformed;
	private String cachedActionPerformed;
	private String cachedIsSelected;
}
