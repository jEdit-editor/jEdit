/*
 * PluginList.java - Plugin list
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

import com.microstar.xml.*;
import java.io.*;
import java.net.URL;
import java.util.Vector;
import org.gjt.sp.util.Log;
import org.gjt.sp.jedit.*;

/**
 * Plugin list downloaded from server.
 * @since jEdit 3.2pre2
 */
class PluginList
{
	PluginList()
	{
		plugins = new Vector();
		pluginSets = new Vector();

		String path = jEdit.getProperty("plugin-manager.url");
		PluginListHandler handler = new PluginListHandler(this,path);
		XmlParser parser = new XmlParser();
		parser.setHandler(handler);

		try
		{
			parser.parse(null,null,new BufferedReader(new InputStreamReader(
				new URL(path).openStream())));
		}
		catch(XmlException xe)
		{
			int line = xe.getLine();
			String message = xe.getMessage();
			Log.log(Log.ERROR,this,path + ":" + line
				+ ": " + message);
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,this,e);
		}
	}

	// package-private members
	void addPlugin(Plugin plugin)
	{
		plugin.checkIfInstalled();
		Log.log(Log.ERROR,this,plugin);
		plugins.addElement(plugin);
	}

	void addPluginSet(PluginSet set)
	{
		Log.log(Log.ERROR,this,set);
		pluginSets.addElement(set);
	}

	// private members
	private Vector plugins;
	private Vector pluginSets;

	static class PluginSet
	{
		String name;
		String description;
		Vector plugins = new Vector();

		void install(Roster roster, String installDirectory)
		{
			for(int i = 0; i < plugins.size(); i++)
			{
				Plugin plugin = (Plugin)plugins.elementAt(i);
				if(plugin.canBeInstalled())
					plugin.install(roster,installDirectory);
			}
		}

		public String toString()
		{
			return plugins.toString();
		}
	}

	static class Plugin
	{
		String jar;
		String name;
		String description;
		String author;
		Vector branches = new Vector();
		String installed;
		String installedVersion;

		void checkIfInstalled()
		{
			// check if the plugin is already installed.
			// this is a bit of hack
			EditPlugin.JAR[] jars = jEdit.getPluginJARs();
			for(int i = 0; i < jars.length; i++)
			{
				String path = jars[i].getPath();
				if(!new File(path).exists())
					continue;

				System.err.println(MiscUtilities.getFileName(path)
					+ ":" + jar);
				if(MiscUtilities.getFileName(path).equals(jar))
				{
					installed = path;

					EditPlugin[] plugins = jars[i].getPlugins();
					if(plugins.length >= 1)
					{
						installedVersion = jEdit.getProperty(
							"plugin." + plugins[0].getClassName()
							+ ".version");
					}
					break;
				}
			}

			String[] notLoaded = jEdit.getNotLoadedPluginJARs();
			for(int i = 0; i < notLoaded.length; i++)
			{
				String path = notLoaded[i];

				if(MiscUtilities.getFileName(path).equals(jar))
				{
					installed = path;
					break;
				}
			}
		}

		/**
		 * Find the first branch compatible with the running jEdit release.
		 */
		Branch getCompatibleBranch()
		{
			for(int i = 0; i < branches.size(); i++)
			{
				Branch branch = (Branch)branches.elementAt(i);
				if(branch.canSatisfyDependencies())
					return branch;
			}

			return null;
		}

		boolean canBeInstalled()
		{
			return getCompatibleBranch() != null;
		}

		void install(Roster roster, String installDirectory)
		{
			
		}

		public String toString()
		{
			return "[jar=" + jar + ",name=" + name + ",description="
				+ description + ",author=" + author + ",branches="
				+ branches + ",installed=" + installed
				+ ",installedVersion=" + installedVersion + "]";
		}
	}

	static class Branch
	{
		String version;
		String download;
		boolean obsolete;
		Vector deps = new Vector();

		boolean canSatisfyDependencies()
		{
			return false;
		}

		void satisfyDependencies(Roster roster)
		{
		}

		public String toString()
		{
			return "[version=" + version + ",download=" + download
				+ ",obsolete=" + obsolete + ",deps=" + deps + "]";
		}
	}

	static class Dependency
	{
		String what;
		String from;
		String to;
		// only used if what is "plugin"
		String plugin;

		Dependency(String what, String from, String to, String plugin)
		{
			this.what = what;
			this.from = from;
			this.to = to;
			this.plugin = plugin;
		}

		public String toString()
		{
			return "[what=" + what + ",from=" + from
				+ ",to=" + to + ",plugin=" + plugin + "]";
		}
	}
}
