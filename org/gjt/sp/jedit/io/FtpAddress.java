/*
 * FtpAddress.java - Ftp addressing encapsulator
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

import org.gjt.sp.jedit.jEdit;

public class FtpAddress
{
	public String host;
	public String port;
	public String user;
	public String password;
	public String path;

	public FtpAddress(String url)
	{
		if(!url.startsWith("ftp:"))
			throw new IllegalArgumentException();

		// remove any leading slashes, and ftp: from URL
		int trimAt = 4;
		for(int i = 4; i < url.length(); i++)
		{
			if(url.charAt(i) != '/')
			{
				trimAt = i;
				break;
			}
		}
		url = url.substring(trimAt);

		// get username/password
		int index = url.indexOf('@');
		if(index == -1)
			user = password = null;
		else
		{
			user = url.substring(0,index);
			url = url.substring(index + 1);

			index = user.indexOf(':');
			if(index != -1)
			{
				password = user.substring(index + 1);
				user = user.substring(0,index);
			}
			else
				password = null;
		}

		// get host name and path
		index = url.indexOf('/');
		if(index == -1)
			throw new IllegalArgumentException();
		else
		{
			host = url.substring(0,index);
			path = url.substring(index);

			index = host.indexOf(':');
			if(index == -1)
				port = "21";
			else
			{
				port = host.substring(index + 1);
				host = host.substring(0,index);
			}
		}
	}

	public FtpAddress(String host, String port, String user,
		String password, String path)
	{
		this.host = host;
		this.port = port;
		this.user = user;
		this.password = password;
		this.path = path;
	}

	public String toString()
	{
		StringBuffer buf = new StringBuffer();
		buf.append("ftp://");
		if(user != null)
		{
			buf.append(user);
			if(password != null && password.length() != 0)
			{
				buf.append(':');
				buf.append(password);
			}
			buf.append('@');
		}
		buf.append(host);
		if(!port.equals("21"))
		{
			buf.append(':');
			buf.append(port);
		}
		buf.append(path);

		return buf.toString();
	}
}

/*
 * Change Log:
 * $Log$
 * Revision 1.2  2000/04/29 09:17:07  sp
 * VFS updates, various fixes
 *
 * Revision 1.1  2000/04/25 11:00:20  sp
 * FTP VFS hacking, some other stuff
 *
 */
