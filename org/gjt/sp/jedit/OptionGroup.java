/*
 * OptionGroup.java - Option pane group
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

package org.gjt.sp.jedit;

import java.util.Enumeration;
import java.util.Vector;

public class OptionGroup
{
	public OptionGroup(String name)
	{
		this.name = name;
		members = new Vector();
	}

	public String getName()
	{
		return name;
	}

	public void addOptionGroup(OptionGroup group)
	{
		members.addElement(group);
	}

	public void addOptionPane(OptionPane pane)
	{
		members.addElement(pane);
	}

	public Enumeration getMembers()
	{
		return members.elements();
	}

	private String name;
	private Vector members;
}
