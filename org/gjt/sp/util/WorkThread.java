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
	public WorkThread(String name)
	{
		super(name);
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
		if(inAWT && requestCount == 0 && awtRequestCount == 0
			&& SwingUtilities.isEventDispatchThread())
		{
			Log.log(Log.DEBUG,this,"AWT immediate: " + run);
			run.run();
			return;
		}

		Request request = new Request(run,inAWT);

		synchronized(lock)
		{
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

		// FIXME: when called from a non-AWT thread,
		// waitForRequests() will return before all
		// AWT runnables have completed
		if(SwingUtilities.isEventDispatchThread())
		{
			Log.log(Log.DEBUG,this,"waitForRequests() running"
				+ " remaining AWT requests");
			// do any queued AWT runnables
			doAWTRequests();
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

	// AWT thread magic
	private boolean awtRunnerQueued;
	private Request firstAWTRequest;
	private Request lastAWTRequest;
	private int awtRequestCount;

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

	private void doAWTRequests()
	{
		Log.log(Log.DEBUG,this,"Running requests in AWT thread");

		while(firstAWTRequest != null)
		{
			doAWTRequest(getNextAWTRequest());
		}

		Log.log(Log.DEBUG,this,"Finished running requests in AWT thread");
	}

	private void doRequest(final Request request)
	{
		if(request.inAWT)
		{
			Log.log(Log.DEBUG,this,"Adding request to AWT queue: "
				+ request);

			synchronized(lock)
			{
				request.next = null;
				request.alreadyRun = false;

				if(firstAWTRequest == null && lastAWTRequest == null)
					firstAWTRequest = lastAWTRequest = request;
				else
				{
					lastAWTRequest.next = request;
					lastAWTRequest = request;
				}

				awtRequestCount++;

				queueAWTRunner();
			}
		}
		else
		{
			Log.log(Log.DEBUG,WorkThread.class,"Running in work thread: "
				+ request);
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

	public void doAWTRequest(Request request)
	{
		Log.log(Log.DEBUG,this,"Running in AWT thread: " + request);

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

		awtRequestCount--;

		// since this is also counted as a normal request...
		requestCount--;
	}

	private void queueAWTRunner()
	{
		if(!awtRunnerQueued)
		{
			awtRunnerQueued = true;
			SwingUtilities.invokeLater(new RunRequestsInAWTThread());
			Log.log(Log.DEBUG,this,"AWT runner queued");
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

			if(request.alreadyRun)
				throw new InternalError("AIEE!!! Request run twice!!! " + request.run);
			request.alreadyRun = true;

			Log.log(Log.DEBUG,this,"getNextRequest() returning " + request);

			StringBuffer buf = new StringBuffer("request queue is now: ");
			Request _request = request.next;
			while(_request != null)
			{
				buf.append(_request.id);
				if(_request.next != null)
					buf.append(",");
				_request = _request.next;
			}
			Log.log(Log.DEBUG,this,buf.toString());

			return request;
		}
	}

	private Request getNextAWTRequest()
	{
		synchronized(lock)
		{
			Request request = firstAWTRequest;
			firstAWTRequest = firstAWTRequest.next;
			if(firstAWTRequest == null)
				lastAWTRequest = null;

			if(request.alreadyRun)
				throw new InternalError("AIEE!!! Request run twice!!! " + request.run);
			request.alreadyRun = true;

			Log.log(Log.DEBUG,this,"getNextAWTRequest() returning " + request);

			StringBuffer buf = new StringBuffer("AWT request queue is now: ");
			Request _request = request.next;
			while(_request != null)
			{
				buf.append(_request.id);
				if(_request.next != null)
					buf.append(",");
				_request = _request.next;
			}
			Log.log(Log.DEBUG,this,buf.toString());

			return request;
		}
	}

	static int ID;

	static class Request
	{
		int id = ++ID;

		Runnable run;
		boolean inAWT;

		boolean alreadyRun;

		Request next;

		Request(Runnable run, boolean inAWT)
		{
			this.run = run;
			this.inAWT = inAWT;
		}

		public String toString()
		{
			return "[id=" + id + ",run=" + run + "]";
		}
	}

	class RunRequestsInAWTThread implements Runnable
	{
		public void run()
		{
			awtRunnerQueued = false;
			doAWTRequests();
		}
	}
}

/*
 * Change Log:
 * $Log$
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
