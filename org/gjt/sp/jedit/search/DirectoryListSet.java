/*
 * DirectoryListSet.java - Directory list matcher
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

package org.gjt.sp.jedit.search;

import gnu.regexp.REException;
import java.io.*;
import java.util.Vector;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

/**
 * Recursive directory search.
 * @author Slava Pestov
 * @version $Id$
 */
public class DirectoryListSet extends BufferListSet
{
	public DirectoryListSet(String directory, String glob, boolean recurse)
	{
		super(listFiles(directory,glob,recurse));
	}

	// private members
	/**
	 * One day this might become public and move to MiscUtilities...
	 */
	private static Vector listFiles(String directory,
		String glob, boolean recurse)
	{
		Log.log(Log.DEBUG,DirectoryListSet.class,"Searching in "
			+ directory);
		Vector files = new Vector(50);

		REFileFilter filter;
		try
		{
			filter = new REFileFilter(MiscUtilities.globToRE(glob));
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,DirectoryListSet.class,e);
			filter = null;
		}

		listFiles(new Vector(),files,new File(directory),filter,recurse);

		return files;
	}

	private static void listFiles(Vector stack, Vector files,
		File directory, REFileFilter filter, boolean recurse)
	{
		if(stack.contains(directory))
		{
			Log.log(Log.ERROR,DirectoryListSet.class,
				"Recursion in DirectoryListSet: "
				+ directory.getPath());
			return;
		}
		else
			stack.addElement(directory);
		
		String[] _files = directory.list(filter);
		if(_files == null)
			return;

		for(int i = 0; i < _files.length; i++)
		{
			File file = new File(directory,_files[i]);
			if(file.isDirectory())
			{
				if(recurse)
					listFiles(stack,files,file,filter,recurse);
			}
			else
			{
				Log.log(Log.DEBUG,DirectoryListSet.class,file.getPath());
				files.addElement(MiscUtilities.constructPath(
					null,file.getPath()));
			}
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.2  1999/11/07 06:51:43  sp
 * Check box menu items supported
 *
 * Revision 1.1  1999/11/06 02:49:53  sp
 * Added missing files
 *
 */
