/*
 * SIMInstaller.java - Main installer class
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

package org.gjt.sp.sim;

import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

public class SIMInstaller
{
	public static final String VERSION = "0.2.1";

	public SIMInstaller()
	{
		props = new Properties();
		try
		{
			InputStream in = getClass().getResourceAsStream("/install.props");
			props.load(in);
			in.close();
		}
		catch(IOException io)
		{
			System.err.println("Error loading '/install.props':");
			io.printStackTrace();
		}
	}

	public String getProperty(String name)
	{
		return props.getProperty(name);
	}

	public int getIntProperty(String name)
	{
		try
		{
			return Integer.parseInt(props.getProperty(name));
		}
		catch(Exception e)
		{
			return -1;
		}
	}

	// private members
	private Properties props;
}
