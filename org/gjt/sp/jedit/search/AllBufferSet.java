/*
 * AllBufferSet.java - All buffer matcher
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

import org.gjt.sp.jedit.*;

/**
 * A file set for searching all open buffers.
 * @author Slava Pestov
 * @version $Id$
 */
public class AllBufferSet implements SearchFileSet
{
	/**
	 * Returns the first buffer to search.
	 * @param view The view performing the search
	 */
	public Buffer getFirstBuffer(View view)
	{
		return jEdit.getFirstBuffer();
	}

	/**
	 * Returns the next buffer to search.
	 * @param view The view performing the search
	 * @param buffer The last buffer searched
	 */
	public Buffer getNextBuffer(View view, Buffer buffer)
	{
		if(buffer == null)
			return view.getBuffer();
		else
		{
			Buffer buf = jEdit.getFirstBuffer();
			do
			{
				if(buf == buffer)
				{
					if(buf.getNext() != null)
						return buf.getNext();
					else
						return null;
				}
			}
			while((buf = buf.getNext()) != null);
		}
		throw new InternalError("Huh? Buffer not on list?");
	}

	/**
	 * Called if the specified buffer didn't have any matches.
	 * @param buffer The buffer
	 */
	public void doneWithBuffer(Buffer buffer) {}
}
/*
 * ChangeLog:
 * $Log$
 * Revision 1.6  1999/10/10 06:38:45  sp
 * Bug fixes and quicksort routine
 *
 * Revision 1.5  1999/10/03 03:47:16  sp
 * Minor stupidity, IDL mode
 *
 * Revision 1.4  1999/10/02 01:12:36  sp
 * Search and replace updates (doesn't work yet), some actions moved to TextTools
 *
 * Revision 1.3  1999/06/09 07:28:10  sp
 * Multifile search and replace tweaks, removed console.html
 *
 * Revision 1.2  1999/06/09 05:22:11  sp
 * Find next now supports multi-file searching, minor Perl mode tweak
 *
 * Revision 1.1  1999/06/03 08:24:13  sp
 * Fixing broken CVS
 *
 */
