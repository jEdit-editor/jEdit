/*
 * PluginResURLConnection.java - jEdit plugin resource URL connection
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

package org.gjt.sp.jedit.proto.jeditresource;

import java.io.*;
import java.net.*;
import java.text.MessageFormat;
import org.gjt.sp.jedit.*;

public class PluginResURLConnection extends URLConnection
{
	public PluginResURLConnection(URL url)
		throws IOException
	{
		super(url);
		
		String file = url.getFile();
		int index = file.indexOf('/',1);
		if(index == -1)
			throw new IOException("Invalid plugin resource URL");
		int start;
		if(file.charAt(0) == '/')
			start = 1;
		else
			start = 0;
		int pluginIndex = Integer.parseInt(file.substring(start,index));
		in = JARClassLoader.getClassLoader(pluginIndex)
			.getResourceAsStream(file.substring(index + 1));
	}

	public void connect() {}

	public InputStream getInputStream()
		throws IOException
	{
		return in;
	}

	// private members
	private InputStream in;
}
