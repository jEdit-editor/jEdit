/*
 * PluginInfo.java - Information about a plugin, installed or not installed
 * Copyright (C) 2000, 2001 Slava Pestov
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

package org.gjt.sp.jedit.pluginmgr;

import org.gjt.sp.jedit.jEdit;

class PluginInfo
{
	public static final int LOADED = 0;
	public static final int LOADED_AND_BROKEN = 1;
	public static final int NOT_LOADED = 2;
	public static final int NOT_INSTALLED = 3;

	int mode;
	String path;
	String clazz;
	String name, version, author;
	String latestVersion;
	String description;

	// constructor for not installed plugins
	PluginInfo(int mode, String path, String name, String version,
		String author, String description)
	{
		this.mode = mode;
		this.path = path;
		this.name = name;
		this.version = version;
		this.author = author;
		this.description = description;
	}

	// constructor for installed plugins
	PluginInfo(int mode, String path, String clazz)
	{
		this.mode = mode;
		this.path = path;
		this.clazz = clazz;

		if(clazz == null)
			name = path;
		else
		{
			name = jEdit.getProperty("plugin." + clazz + ".name");
			if(name == null)
				name = clazz;

			version = jEdit.getProperty("plugin." + clazz
				+ ".version");

			author = jEdit.getProperty("plugin." + clazz
				+ ".author");
		}
	}

	public String toString()
	{
		return name;
	}
}
