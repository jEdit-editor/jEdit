/*
 * VFSFilter.java - VFS filename filter
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

package org.gjt.sp.jedit.browser;

import gnu.regexp.*;
import org.gjt.sp.jedit.MiscUtilities;

/**
 * A file name filter used by the VFS browser.
 * @author Slava Pestov
 * @version $Id$
 */
public class VFSFilter
{
	public VFSFilter(String label, String glob) throws REException
	{
		this.label = label + " (" + glob + ")";
		re = new RE(MiscUtilities.globToRE(glob),RE.REG_ICASE);
	}

	public String toString()
	{
		return label;
	}

	public boolean accept(String name)
	{
		return re.isMatch(name);
	}

	// private members
	private String label;
	private RE re;
}

/*
 * Change Log:
 * $Log$
 * Revision 1.1  2000/08/01 11:44:15  sp
 * More VFS browser work
 *
 */
