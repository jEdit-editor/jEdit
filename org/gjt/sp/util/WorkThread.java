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
	}

	/**
	 * Adds a work request to the queue.
	 * @param run The runnable
	 * @param inAWT If true, will be executed in AWT thread. Otherwise,
	 * will be executed in work thread
	 */
	public void addWorkRequest(Runnable run, boolean inAWT)
	{
		Log.log(Log.DEBUG,this,"Adding request: " + run +
			(inAWT ? " in AWT thread" : ""));

		synchronized(lock)
		{
			// if inAWT is set and there are no requests
			// pending, execute it immediately
			if(requestCount == 0 && inAWT)
			{
				Log.log(Log.DEBUG,this,"AWT immediate: " + run);
				run.run();
				return;
			}

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
	public void waitForAll()
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
	 * Aborts the currently running operation.
	 */
	public void abort()
	{
		stop(new Abort());
	}

	public void run()
	{
		Log.log(Log.DEBUG,this,"Work request thread starting");

		for(;;)
		{
			try
			{
				doRequests();
			}
			catch(Abort a)
			{
			}
		}
	}

	// protected members

	/**
	 * Called when the thread is starting to process a set of requests.
	 */
	protected void beginRunning() {}

	/**
	 * Called when the thread is finished processing a set of requests.
	 */
	protected void endRunning() {}

	// private members
	private Object lock = new Object();
	private Request firstRequest;
	private Request lastRequest;
	private int requestCount;

	private void doRequests()
	{
		synchronized(lock)
		{
			Log.log(Log.DEBUG,this,"Waking up with "
				+ requestCount + " request(s) pending");

			if(firstRequest != null)
			{
				beginRunning();

				while(firstRequest != null)
				{
					doRequest(getNextRequest());
				}

				endRunning();
			}

			// notify a running waitForAll() method
			lock.notify();

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
			Log.log(Log.DEBUG,this,"Running in AWT thread: " + request.run);

			// this hack ensures that requestCount is zero only
			// when there are really no requests running
			Runnable r = new Runnable()
			{
				public void run()
				{
					request.run.run();
					synchronized(lock)
					{
						requestCount--;
					}
				}
			};
			SwingUtilities.invokeLater(r);
		}
		else
		{
			Log.log(Log.DEBUG,this,"Running in work thread: " + request.run);
			request.run.run();
			requestCount--;
		}
	}

	private Request getNextRequest()
	{
		// no need to sync on 'lock' since we're only called
		// from doRequests()

		Request request = firstRequest;
		firstRequest = firstRequest.next;
		if(firstRequest == null)
			lastRequest = null;
		return request;
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

	public class Abort extends Error
	{
		public Abort()
		{
			super("I/O abort");
		}
	}
}

/*
 * Change Log:
 * $Log$
 * Revision 1.2  2000/04/25 03:32:40  sp
 * Even more VFS hacking
 *
 * Revision 1.1  2000/04/24 11:00:23  sp
 * More VFS hacking
 *
 */
