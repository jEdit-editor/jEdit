/*
 * REFileFilter.java - Regular expression file filter
 * Copyright (C) 1999 Slava Pestov
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

import gnu.regexp.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
 * Regular expression filename filter.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class REFileFilter extends FileFilter
{
	public REFileFilter(String name, String re)
		throws REException
	{
		this.name = name;
		this.re = new RE(re,RE.REG_ICASE);
	}

	public boolean accept(File file)
	{
		return file.isDirectory() || re.isMatch(file.getName());
	}

	public String getDescription()
	{
		return name;
	}

	// private members
	private String name;
	private RE re;
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.2  1999/10/07 04:57:13  sp
 * Images updates, globs implemented, file filter bug fix, close all command
 *
 * Revision 1.1  1999/10/03 04:13:25  sp
 * Forgot to add some files
 *
 */
