/*
 * WorkThread.java - Background thread that does stuff
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

package org.gjt.sp.util;

/**
 * Services work requests in the background.
 * @author Slava Pestov
 * @version $Id$
 */
public class WorkThread extends Thread
{
	public WorkThread(WorkThreadPool pool, ThreadGroup group, String name)
	{
		super(group, name);
		setDaemon(true);
		setPriority(4);

		this.pool = pool;
	}

	/**
	 * Sets if the current request can be aborted.
	 * @since jEdit 3.0pre1
	 */
	public void setAbortable(boolean abortable)
	{
		synchronized(abortLock)
		{
			this.abortable = abortable;
			if(aborted)
				stop(new Abort());
		}
	}

	/**
	 * Aborts the currently running request, if allowed.
	 * @since jEdit 3.0pre1
	 */
	public void abortCurrentRequest()
	{
		synchronized(abortLock)
		{
			if(abortable && !aborted)
				stop(new Abort());
			aborted = true;
		}
	}

	public void run()
	{
		Log.log(Log.DEBUG,this,"Work request thread starting [" + getName() + "]");

		for(;;)
		{
			doRequests();
		}
	}

	// private members
	private WorkThreadPool pool;
	private Object abortLock = new Object();
	private boolean abortable;
	private boolean aborted;

	private void doRequests()
	{
		WorkThreadPool.Request request;
		for(;;)
		{
			request = pool.getNextRequest();
			if(request == null)
				break;
			else
				doRequest(request);
		}

		synchronized(pool.waitForAllLock)
		{
			// notify a running waitForRequests() method
			pool.waitForAllLock.notifyAll();
		}

		synchronized(pool.lock)
		{
			// wait for more requests
			try
			{
				pool.lock.wait();
			}
			catch(InterruptedException ie)
			{
				Log.log(Log.ERROR,this,ie);
			}
		}
	}

	private void doRequest(WorkThreadPool.Request request)
	{
		Log.log(Log.DEBUG,WorkThread.class,"Running in work thread: " + request);

		try
		{
			request.run.run();
		}
		catch(Abort a)
		{
			Log.log(Log.ERROR,WorkThread.class,"Unhandled abort");
		}
		catch(Throwable t)
		{
			Log.log(Log.ERROR,WorkThread.class,"Exception "
				+ "in work thread:");
			Log.log(Log.ERROR,WorkThread.class,t);
		}
		finally
		{
			synchronized(abortLock)
			{
				aborted = abortable = false;
			}
			pool.requestDone();
		}
	}

	public static class Abort extends Error
	{
		public Abort()
		{
			super("Work request aborted");
		}
	}
}

/*
 * Change Log:
 * $Log$
 * Revision 1.14  2000/07/21 10:23:49  sp
 * Multiple work threads
 *
 * Revision 1.13  2000/07/19 11:45:18  sp
 * I/O requests can be aborted now
 *
 * Revision 1.12  2000/07/03 03:32:16  sp
 * *** empty log message ***
 *
 * Revision 1.11  2000/06/24 06:24:56  sp
 * work thread bug fixes
 *
 * Revision 1.10  2000/06/24 03:46:48  sp
 * VHDL mode, bug fixing
 *
 * Revision 1.9  2000/06/16 10:11:06  sp
 * Bug fixes ahoy
 *
 * Revision 1.8  2000/06/12 02:43:30  sp
 * pre6 almost ready
 *
 * Revision 1.7  2000/06/06 04:38:09  sp
 * WorkThread's AWT request stuff reworked
 *
 * Revision 1.6  2000/05/21 06:06:43  sp
 * Documentation updates, shell script mode bug fix, HyperSearch is now a frame
 *
 * Revision 1.5  2000/05/01 11:53:24  sp
 * More icons added to toolbar, minor updates here and there
 *
 * Revision 1.4  2000/04/29 09:17:07  sp
 * VFS updates, various fixes
 *
 */
