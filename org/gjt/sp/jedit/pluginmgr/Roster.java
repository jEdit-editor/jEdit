/*
 * Roster.java - A list of things to do, used in various places
 * Copyright (C) 2001 Slava Pestov
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

import javax.swing.JOptionPane;
import java.awt.Component;
import java.io.File;
import java.util.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

class Roster
{
	Roster()
	{
		operations = new Vector();
	}

	void addOperation(Operation op)
	{
		for(int i = 0; i < operations.size(); i++)
		{
			if(operations.elementAt(i).equals(op))
				return;
		}

		operations.addElement(op);
	}

	boolean isEmpty()
	{
		return operations.size() == 0;
	}

	boolean confirm(Component comp)
	{
		String[] args = { toString() };

		int result = GUIUtilities.confirm(comp,"plugin-manager.roster-confirm",
			args,JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
		return (result == JOptionPane.YES_OPTION);
	}

	boolean performOperations()
	{
		for(int i = 0; i < operations.size(); i++)
		{
			Operation op = (Operation)operations.elementAt(i);
			if(!op.perform())
				return false;
		}

		return true;
	}

	public String toString()
	{
		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < operations.size(); i++)
		{
			buf.append(operations.elementAt(i));
			buf.append('\n');
		}
		return buf.toString();
	}

	// private members
	private Vector operations;

	static interface Operation
	{
		boolean inAWTThread();
		boolean perform();
		String toString();
		boolean equals(Object o);
	}

	static class Remove implements Operation
	{
		Remove(String plugin)
		{
			this.plugin = plugin;
		}

		public boolean inAWTThread()
		{
			return true;
		}

		public boolean perform()
		{
			// close JAR file
			EditPlugin.JAR jar = jEdit.getPluginJAR(plugin);
			if(jar != null)
				jar.getClassLoader().closeZipFile();

			// move JAR first
			File jarFile = new File(plugin);
			File srcFile = new File(plugin.substring(0,plugin.length() - 4));

			boolean ok = true;
			ok &= deleteRecursively(jarFile);

			if(srcFile.exists())
				ok &= deleteRecursively(srcFile);

			return ok;
		}

		public String toString()
		{
			String[] args = { plugin };
			return jEdit.getProperty("plugin-manager.roster.remove",args);
		}

		public boolean equals(Object o)
		{
			if(o instanceof Remove
				&& ((Remove)o).plugin.equals(plugin))
				return true;
			else
				return false;
		}

		// private members
		private String plugin;

		private boolean deleteRecursively(File file)
		{
			Log.log(Log.NOTICE,this,"Deleting " + file + " recursively");

			boolean ok = true;

			if(file.isDirectory())
			{
				String path = file.getPath();
				String[] children = file.list();
				for(int i = 0; i < children.length; i++)
				{
					ok &= deleteRecursively(new File(path,children[i]));
				}
			}

			ok &= file.delete();

			return ok;
		}
	}

	static class Install implements Operation
	{
		Install(String url, String installDirectory)
		{
			// catch those hooligans passing null urls
			if(url == null)
				throw new NullPointerException();

			this.url = url;
			this.installDirectory = installDirectory;
		}

		public boolean inAWTThread()
		{
			return false;
		}

		public boolean perform()
		{
			Log.log(Log.ERROR,this,"foo");
			return false;
		}

		public String toString()
		{
			String[] args = { url };
			return jEdit.getProperty("plugin-manager.roster.install",args);
		}

		public boolean equals(Object o)
		{
			if(o instanceof Install
				&& ((Install)o).url.equals(url))
			{
				/* even if installDirectory is different */
				return true;
			}
			else
				return false;
		}

		// private members
		private String url;
		private String installDirectory;
	}
}
