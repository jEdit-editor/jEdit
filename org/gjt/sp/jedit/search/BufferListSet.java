/*
 * BufferListSet.java - Buffer list matcher
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

import java.util.Vector;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.*;

/**
 * A file set for searching a user-specified list of buffers.
 * @author Slava Pestov
 * @version $Id$
 */
public class BufferListSet implements SearchFileSet
{
	/**
	 * Creates a new buffer list search set. This constructor is
	 * only used by the multifile settings dialog box.
	 * @param files The path names to search
	 */
	public BufferListSet(Object[] files)
	{
		this.files = new Vector(files.length);
		for(int i = 0; i < files.length; i++)
		{
			this.files.addElement(((Buffer)files[i]).getPath());
		}
	}

	/**
	 * Creates a new buffer list search set.
	 * @param files The path names to search
	 */
	public BufferListSet(Vector files)
	{
		this.files = files;
	}

	/**
	 * Returns the first buffer to search.
	 * @param view The view performing the search
	 */
	public Buffer getFirstBuffer(View view)
	{
		return getBuffer((String)files.elementAt(0));
	}

	/**
	 * Returns the next buffer to search.
	 * @param view The view performing the search
	 * @param buffer The last buffer searched
	 */
	public Buffer getNextBuffer(View view, Buffer buffer)
	{
		if(buffer == null)
		{
			buffer = view.getBuffer();

			for(int i = 0; i < files.size(); i++)
			{
				if(files.elementAt(i).equals(buffer.getPath()))
					return buffer;
			}

			return getFirstBuffer(view);
		}
		else
		{
			// -1 so that the last isn't checked
			for(int i = 0; i < files.size() - 1; i++)
			{
				if(files.elementAt(i).equals(buffer.getPath()))
					return getBuffer((String)files.elementAt(i+1));
			}

			return null;
		}
	}

	/**
	 * Called if the specified buffer was found to have a match.
	 * @param buffer The buffer
	 */
	public void matchFound(Buffer buffer)
	{
		jEdit.commitTemporary(buffer);
	}

	// private members
	private Vector files;

	private Buffer getBuffer(String path)
	{
		Buffer buffer = jEdit.openTemporary(null,null,path,false,false);
		VFSManager.waitForRequests();
		return buffer;
	}
}
/*
 * ChangeLog:
 * $Log$
 * Revision 1.12  2000/04/24 11:00:23  sp
 * More VFS hacking
 *
 * Revision 1.11  1999/11/28 00:33:07  sp
 * Faster directory search, actions slimmed down, faster exit/close-all
 *
 * Revision 1.10  1999/10/30 02:44:18  sp
 * Miscallaneous stuffs
 *
 * Revision 1.9  1999/10/28 09:07:21  sp
 * Directory list search
 *
 * Revision 1.8  1999/10/26 07:43:59  sp
 * Session loading and saving, directory list search started
 *
 * Revision 1.7  1999/10/10 06:38:45  sp
 * Bug fixes and quicksort routine
 *
 * Revision 1.6  1999/10/02 01:12:36  sp
 * Search and replace updates (doesn't work yet), some actions moved to TextTools
 *
 * Revision 1.5  1999/07/16 23:45:49  sp
 * 1.7pre6 BugFree version
 *
 * Revision 1.4  1999/06/15 05:03:54  sp
 * RMI interface complete, save all hack, views & buffers are stored as a link
 * list now
 *
 * Revision 1.3  1999/06/09 07:28:10  sp
 * Multifile search and replace tweaks, removed console.html
 *
 */
