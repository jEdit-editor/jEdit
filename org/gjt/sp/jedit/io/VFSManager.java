/*
 * VFSManager.java - VFS manager
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

package org.gjt.sp.jedit.io;

public class VFSManager
{
	/**
	 * In jEdit 2.5, this method will block the current thread until
	 * all I/O is complete. In jEdit 2.4, since I/O is synchronous,
	 * this method will always return immediately.
	 */
	public static void waitForRequests()
	{
	}

	/**
	 * In jEdit 2.5, this method will execute the specified runnable
	 * in the AWT thread after all I/O requests are complete.
	 * In jEdit 2.4, since I/O is synchronous, this method will
	 * invoke the runnable immediatley.
	 * @param r The runnable
	 */
	public static void runInAWTThread(Runnable r)
	{
		r.run();
	}

	// private members
	private VFSManager() {}
}
