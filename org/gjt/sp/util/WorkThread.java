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

import javax.swing.SwingUtilities;

/**
 * Services work requests in the background.
 * @author Slava Pestov
 * @version $Id$
 */
public class WorkThread extends Thread
{
	public WorkThread()
	{
		super("Work request thread");
		setDaemon(true);
		setPriority(4);
	}

	/**
	 * Adds a work request to the queue.
	 * @param run The runnable
	 * @param inAWT If true, will be executed in AWT thread. Otherwise,
	 * will be executed in work thread
	 */
	public void addWorkRequest(Runnable run, boolean inAWT)
	{
		// if inAWT is set and there are no requests
		// pending, execute it immediately
		if(requestCount == 0 && inAWT && SwingUtilities.isEventDispatchThread())
		{
			Log.log(Log.DEBUG,this,"AWT immediate: " + run);
			run.run();
			return;
		}

		synchronized(lock)
		{
			Request request = new Request(run,inAWT);
			if(firstRequest == null && lastRequest == null)
				firstRequest = lastRequest = request;
			else
			{
				lastRequest.next = request;
				lastRequest = request;
			}

			requestCount++;

			lock.notify();
		}
	}

	/**
	 * Waits until all requests are complete.
	 */
	public void waitForRequests()
	{
		synchronized(lock)
		{
			while(firstRequest != null)
			{
				try
				{
					lock.wait();
				}
				catch(InterruptedException ie)
				{
					Log.log(Log.ERROR,this,ie);
				}
			}
		}
	}

	/**
	 * Returns the number of pending requests.
	 */
	public int getRequestCount()
	{
		return requestCount;
	}

	public void run()
	{
		Log.log(Log.DEBUG,this,"Work request thread starting");

		for(;;)
		{
			doRequests();
		}
	}

	// private members
	private Object lock = new Object();
	private Request firstRequest;
	private Request lastRequest;
	private int requestCount;

	private void doRequests()
	{
		while(firstRequest != null)
		{
			doRequest(getNextRequest());
		}

		synchronized(lock)
		{
			// notify a running waitForRequests() method
			lock.notifyAll();

			// wait for more requests
			try
			{
				lock.wait();
			}
			catch(InterruptedException ie)
			{
				Log.log(Log.ERROR,this,ie);
			}
		}
	}

	private void doRequest(final Request request)
	{
		if(request.inAWT)
		{
			// this hack ensures that requestCount is zero only
			// when there are really no requests running
			Runnable r = new Runnable()
			{
				public void run()
				{
					Log.log(Log.DEBUG,WorkThread.class,
						"Running in AWT thread: "
						+ request.run);

					try
					{
						request.run.run();
					}
					catch(Throwable t)
					{
						Log.log(Log.ERROR,WorkThread.class,"Exception "
							+ "in AWT thread:");
						Log.log(Log.ERROR,WorkThread.class,t);
					}

					requestCount--;
				}
			};
			SwingUtilities.invokeLater(r);
		}
		else
		{
			Log.log(Log.DEBUG,WorkThread.class,"Running in work thread: "
				+ request.run);
			try
			{
				request.run.run();
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,WorkThread.class,"Exception "
					+ "in work thread:");
				Log.log(Log.ERROR,WorkThread.class,t);
			}
			requestCount--;
		}
	}

	private Request getNextRequest()
	{
		synchronized(lock)
		{
			Request request = firstRequest;
			firstRequest = firstRequest.next;
			if(firstRequest == null)
				lastRequest = null;
			return request;
		}
	}

	class Request
	{
		Runnable run;
		boolean inAWT;

		Request next;

		Request(Runnable run, boolean inAWT)
		{
			this.run = run;
			this.inAWT = inAWT;
		}
	}
}

/*
 * Change Log:
 * $Log$
 * Revision 1.6  2000/05/21 06:06:43  sp
 * Documentation updates, shell script mode bug fix, HyperSearch is now a frame
 *
 * Revision 1.5  2000/05/01 11:53:24  sp
 * More icons added to toolbar, minor updates here and there
 *
 * Revision 1.4  2000/04/29 09:17:07  sp
 * VFS updates, various fixes
 *
 * Revision 1.3  2000/04/27 08:32:58  sp
 * VFS fixes, read only fixes, macros can prompt user for input, improved
 * backup directory feature
 *
 * Revision 1.2  2000/04/25 03:32:40  sp
 * Even more VFS hacking
 *
 * Revision 1.1  2000/04/24 11:00:23  sp
 * More VFS hacking
 *
 */
