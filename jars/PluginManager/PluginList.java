/*
 * PluginList.java - Plugin list downloader
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

import java.awt.Component;
import java.io.*;
import java.net.*;
import java.util.Vector;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class PluginList
{
	public PluginList(Component comp)
	{
		Vector pluginVector = new Vector();

		try
		{
			URL url = new URL(jEdit.getProperty("plugin-manager.url"));
			BufferedReader in = new BufferedReader(
				new InputStreamReader(url.openStream()));

			Plugin plugin = null;
			String line;
			while((line = in.readLine()) != null)
			{
				if(line.startsWith("plugin "))
				{
					plugin = new Plugin(line.substring(7));
					pluginVector.addElement(plugin);
				}
				else if(line.startsWith("jar "))
				{
					plugin.jar = line.substring(4);
					plugin.setClassAndVersion();
				}
				else if(line.startsWith("author "))
					plugin.author = line.substring(7);
				else if(line.startsWith("version "))
					plugin.latestVersion = line.substring(8);
				else if(line.startsWith("updated "))
					plugin.updated = line.substring(8);
				else if(line.startsWith("requires "))
					plugin.requires = line.substring(9);
				else if(line.startsWith("description "))
				{
					String desc = plugin.description;
					line = line.substring(12);
					if(desc != null)
						desc = desc + "\n" + line;
					else
						desc = line;
					plugin.description = desc;
				}
				else if(line.startsWith("download "))
					plugin.download = line.substring(9);
			}

			in.close();

			plugins = new Plugin[pluginVector.size()];
			pluginVector.copyInto(plugins);
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,this,io);
			String[] args = { io.getMessage() };
			GUIUtilities.error(comp,"plugin-list.ioerror",args);
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,this,e);
		}
	}

	public Plugin[] getPlugins()
	{
		return plugins;
	}

	// private members
	private Plugin[] plugins;

	public class Plugin
	{
		public String name;
		public String jar;
		public String path;
		public String clazz;
		public String author;
		public String currVersion;
		public String latestVersion;
		public String updated;
		public String requires;
		public String description;
		public String download;

		Plugin(String name)
		{
			this.name = name;
		}

		void setClassAndVersion()
		{
			EditPlugin.JAR[] jars = jEdit.getPluginJARs();
			for(int i = 0; i < jars.length; i++)
			{
				EditPlugin.JAR checkJar = jars[i];
				EditPlugin[] plugins = checkJar.getPlugins();
				String clazz;
				if(plugins.length != 0)
					clazz = plugins[0].getClassName();
				else
					clazz = null;
				String path = checkJar.getPath();
				if(MiscUtilities.getFileName(path).equals(jar))
				{
					currVersion = jEdit.getProperty(
						"plugin." + clazz + ".version");
					this.path = path;
					this.clazz = clazz;
					break;
				}
			}
		}

		public String toString()
		{
			return name;
		}
	}
}
