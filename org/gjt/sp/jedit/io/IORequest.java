/*
 * IORequest.java - I/O request
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

import javax.swing.text.BadLocationException;
import java.io.*;
import java.util.zip.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

/**
 * An I/O request.
 * @author Slava Pestov
 * @version $Id$
 */
public class IORequest implements Runnable
{
	/**
	 * A file load request.
	 */
	public static final int LOAD = 0;

	/**
	 * A file save request.
	 */
	public static final int SAVE = 1;

	/**
	 * Creates a new I/O request.
	 * @param type The request type
	 * @param view The view
	 * @param buffer The buffer
	 * @param path The path
	 * @param vfs The VFS
	 */
	public IORequest(int type, View view, Buffer buffer, String path, VFS vfs)
	{
		this.type = type;
		this.view = view;
		this.buffer = buffer;
		this.path = path;
		this.vfs = vfs;

		markersPath = MiscUtilities.getFileParent(path)
			+ '.' + MiscUtilities.getFileName(path)
			+ ".marks";
	}

	/**
	 * If type is LOAD, calls buffer._read(vfs._createInputStream()).
	 * If type is SAVE, calls buffer._write(vfs._createOutputStream());
	 */
	public void run()
	{
		View[] views = jEdit.getViews();
		String status = (type == LOAD ? "loading" : "saving");
		String[] args = { MiscUtilities.getFileName(path) };
		String message = jEdit.getProperty("view.status." + status,args);
		for(int i = 0; i < views.length; i++)
		{
			View view = views[i];
			view.setEnabled(false);
			view.showWaitCursor();
			view.pushStatus(message);
		}

		try
		{
			switch(type)
			{
			case LOAD:
				load();
				break;
			case SAVE:
				save();
				break;
			}
		}
		catch(Throwable t)
		{
			Log.log(Log.ERROR,this,t);
		}

		for(int i = 0; i < views.length; i++)
		{
			View view = views[i];
			view.setEnabled(true);
			view.hideWaitCursor();
			view.popStatus();
		}
	}

	public String toString()
	{
		return getClass().getName() + "[type=" + (type == LOAD
			? "LOAD" : "SAVE") + ",view=" + view + ",buffer="
			+ buffer + ",vfs=" + vfs + "]";
	}

	// private members
	private int type;
	private View view;
	private Buffer buffer;
	private String path;
	private String markersPath;
	private VFS vfs;

	private void load()
	{
		try
		{
			InputStream in = vfs._createInputStream(view,path);
			if(in == null)
				return;
			if(path.endsWith(".gz"))
				in = new GZIPInputStream(in);

			buffer._read(in);
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,this,io);
			Object[] args = { io.toString() };
			VFSManager.error(view,"ioerror",args);
		}

		try
		{
			System.err.println("Loading markers from " + markersPath);
			InputStream in = vfs._createInputStream(view,markersPath);
			if(in == null)
				return;
			buffer._readMarkers(in);
		}
		catch(IOException io)
		{
			// ignore
		}
	}

	private void save()
	{
		try
		{
			OutputStream out = vfs._createOutputStream(view,path);
			if(out == null)
				return;
			if(path.endsWith(".gz"))
				out = new GZIPOutputStream(out);

			buffer._write(out);

			// We only save markers to VFS's that support deletion.
			// Otherwise, we will accumilate stale marks files.
			if(vfs.canDelete() && buffer.getMarkerCount() != 0)
			{
				System.err.println("Saving markers to " + markersPath);
				OutputStream output = vfs._createOutputStream(view,markersPath);
				if(out == null)
					return;
				buffer._writeMarkers(out);
			}
			else
				vfs.delete(markersPath);
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,this,io);
			Object[] args = { io.toString() };
			VFSManager.error(view,"ioerror",args);
		}
	}
}

/*
 * Change Log:
 * $Log$
 * Revision 1.1  2000/04/24 04:45:37  sp
 * New I/O system started, and a few minor updates
 *
 */
