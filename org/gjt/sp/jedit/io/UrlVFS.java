/*
 * UrlVFS.java - Url VFS
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

import javax.swing.JOptionPane;
import java.io.*;
import java.net.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

/**
 * URL VFS.
 * @author Slava Pestov
 * @version $Id$
 */
public class UrlVFS extends VFS
{
	/**
	 * Creates a new URL VFS.
	 */
	public UrlVFS()
	{
		super("url");
	}

	/**
	 * Displays an open dialog box and returns the selected pathname.
	 * @param view The view
	 * @param buffer The buffer
	 */
	public Buffer showOpenDialog(View view, Buffer buffer)
	{
		String path = GUIUtilities.input(view,"openurl",null);
		if(path != null)
			return jEdit.openFile(view,path);
		else
			return null;
	}

	/**
	 * Displays a save dialog box and returns the selected pathname.
	 * @param view The view
	 * @param buffer The buffer
	 */
	public String showSaveDialog(View view, Buffer buffer)
	{
		return GUIUtilities.input(view,"saveurl",null);
	}

	/**
	 * Creates an input stream. This method is called from the I/O
	 * thread.
	 * @param view The view
	 * @param buffer The buffer
	 * @param path The path
	 * @param ignoreErrors If true, file not found errors should be
	 * ignored
	 * @exception IOException If an I/O error occurs
	 */
	public InputStream _createInputStream(View view, Buffer buffer,
		String path, boolean ignoreErrors) throws IOException
	{
		try
		{
			return new URL(path).openStream();
		}
		catch(MalformedURLException mu)
		{
			Log.log(Log.ERROR,this,mu);
			String[] args = { path };
			VFSManager.error(view,"badurl",args);
			return null;
		}
	}

	/**
	 * Creates an output stream. This method is called from the I/O
	 * thread.
	 * @param view The view
	 * @param buffer The buffer
	 * @param path The path
	 * @exception IOException If an I/O error occurs
	 */
	public OutputStream _createOutputStream(View view, Buffer buffer,
		String path) throws IOException
	{
		try
		{
			return new URL(path).openConnection()
				.getOutputStream();
		}
		catch(MalformedURLException mu)
		{
			Log.log(Log.ERROR,this,mu);
			String[] args = { path };
			VFSManager.error(view,"badurl",args);
			return null;
		}
	}
}
