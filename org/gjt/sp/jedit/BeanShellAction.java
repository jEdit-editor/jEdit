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

import bsh.BshMethod;
import bsh.Primitive;
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
			String cachedIsSelectedName = "_action" + counter++;
			BeanShell.eval(null,cachedIsSelectedName + "(){"
				+ isSelected + "}",false);
			cachedIsSelected = BeanShell.getMethod(cachedIsSelectedName);
		}
	}

	public void invoke(View view)
	{
		if(cachedCode == null)
		{
			String cachedCodeName = "_action" + counter++;
			BeanShell.eval(null,cachedCodeName + "(){"
				+ code + "}",false);
			cachedCode = BeanShell.getMethod(cachedCodeName);
		}
		BeanShell.invokeMethod(view,cachedCode,EMPTY_ARGS);
	}

	public boolean isToggle()
	{
		return cachedIsSelected != null;
	}

	public boolean isSelected(Component comp)
	{
		if(cachedIsSelected == null)
			return false;

		Primitive returnValue = (Primitive)BeanShell.invokeMethod(
			getView(comp),cachedIsSelected,EMPTY_ARGS);
		return returnValue.getValue().equals(Boolean.TRUE);
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
	private static final Object[] EMPTY_ARGS = new Object[0];

	private static int counter;

	private boolean noRepeat;
	private boolean noRecord;
	private String code;
	private BshMethod cachedCode;
	private BshMethod cachedIsSelected;
}
