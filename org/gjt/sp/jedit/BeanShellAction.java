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
	public BeanShellAction(String name, String code,
		String isSelected, boolean noRepeat, boolean noRecord)
	{
		super(name);

		this.code = code;
		this.noRepeat = noRepeat;
		this.noRecord = noRecord;

		if(isSelected != null)
		{
			cachedIsSelected = "_action" + counter++ + "()";
			BeanShell.eval(null,cachedIsSelected + "{"
				+ isSelected + "}");
		}
	}

	public void invoke(View view)
	{
		if(cachedCode == null)
		{
			cachedCode = "_action" + counter++ + "()";
			BeanShell.eval(null,cachedCode + "{"
				+ code + "}");
		}
		BeanShell.eval(view,cachedCode);
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

	public boolean noRepeat()
	{
		return noRepeat;
	}

	public boolean noRecord()
	{
		return noRecord;
	}

	public String getCode()
	{
		return code.trim();
	}

	// private members
	private static int counter;

	private boolean noRepeat;
	private boolean noRecord;
	private String code;
	private String cachedCode;
	private String cachedIsSelected;
}
