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
			while(requestCount != 0)
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

	public void run()
	{
		Log.log(Log.DEBUG,this,"Work request thread starting");

		for(;;)
		{
			doRequests();
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

			if(requestCount != 0)
			{
				beginRunning();

				while(requestCount != 0)
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

	private void doRequest(Request request)
	{
		try
		{
			if(request.inAWT)
			{
				Log.log(Log.DEBUG,this,"Running in AWT thread: " + request.run);
				SwingUtilities.invokeLater(request.run);
			}
			else
			{
				Log.log(Log.DEBUG,this,"Running in work thread: " + request.run);
				request.run.run();
			}
		}
		catch(Throwable t)
		{
			Log.log(Log.ERROR,this,"Error executing " + request.run + ":");
			Log.log(Log.ERROR,this,t);
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
		requestCount--;
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
}

/*
 * Change Log:
 * $Log$
 * Revision 1.1  2000/04/24 11:00:23  sp
 * More VFS hacking
 *
 */
