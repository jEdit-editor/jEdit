/*
 * JARClassLoader.java - Loads classes from JAR files
 * Copyright (C) 1999, 2000 Slava Pestov
 * Portions copyright (C) 1999 mike dillon
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

package org.gjt.sp.jedit;

import java.io.*;
import java.lang.reflect.Modifier;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import org.gjt.sp.util.Log;

/**
 * A class loader implementation that loads classes from JAR files.
 * @author Slava Pestov
 * @version $Id$
 */
public class JARClassLoader extends ClassLoader
{
	// no-args constructor is for loading classes from all plugins
	// eg BeanShell uses one of these so that scripts can use
	// plugin classes
	public JARClassLoader()
	{
	}

	public JARClassLoader(String path)
		throws IOException
	{
		zipFile = new ZipFile(path);

		Enumeration entires = zipFile.entries();
		while(entires.hasMoreElements())
		{
			ZipEntry entry = (ZipEntry)entires.nextElement();
			String name = entry.getName();
			if(name.toLowerCase().endsWith(".props"))
			{
				jEdit.loadProps(zipFile.getInputStream(entry));
			}
			else if(name.toLowerCase().endsWith(".class"))
			{
				if(name.endsWith("Plugin.class"))
					pluginClasses.addElement(name);
			}
		}

		jar = new EditPlugin.JAR(path,this);
		jEdit.addPluginJAR(jar);
	}

	/**
	 * @exception ClassNotFoundException if the class could not be found
	 */
	public Class loadClass(String clazz, boolean resolveIt)
		throws ClassNotFoundException
	{
		return loadClass(clazz,resolveIt,true);
	}

	public InputStream getResourceAsStream(String name)
	{
		if(zipFile == null)
			return null;

		try
		{
			ZipEntry entry = zipFile.getEntry(name);
			if(entry == null)
				return getSystemResourceAsStream(name);
			else
				return zipFile.getInputStream(entry);
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,this,io);

			return null;
		}
	}

	public URL getResource(String name)
	{
		if(zipFile == null)
			return null;

		ZipEntry entry = zipFile.getEntry(name);
		if(entry == null)
			return getSystemResource(name);

		try
		{
			return new URL(getResourceAsPath(name));
		}
		catch(MalformedURLException mu)
		{
			Log.log(Log.ERROR,this,mu);
			return null;
		}
	}

	public String getResourceAsPath(String name)
	{
		if(zipFile == null)
			return null;

		return "jeditresource:" + jar.getIndex() + "/" + name;
	}

	/**
	 * Closes the ZIP file. This plugin will no longer be usable
	 * after this.
	 * @since jEdit 2.6pre1
	 */
	public void closeZipFile()
	{
		if(zipFile == null)
			return;

		closed = true;
		try
		{
			zipFile.close();
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,this,io);
		}
	}

	// package-private members
	void loadAllPlugins()
	{
		for(int i = 0; i < pluginClasses.size(); i++)
		{
			String name = (String)pluginClasses.elementAt(i);
			name = MiscUtilities.fileToClass(name);

			try
			{
				loadPluginClass(name);
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,this,"Error while starting plugin " + name);
				Log.log(Log.ERROR,this,t);

				jar.addPlugin(new EditPlugin.Broken(name));
				String[] args = { name, t.toString() };
				GUIUtilities.error(null,"plugin.start-error",args);
			}
		}
	}

	// private members
	private EditPlugin.JAR jar;
	private Vector pluginClasses = new Vector();
	private ZipFile zipFile;
	private boolean closed;

	private void loadPluginClass(String name)
		throws Exception
	{
		// Check if a plugin with the same name is already loaded
		EditPlugin[] plugins = jEdit.getPlugins();

		for(int i = 0; i < plugins.length; i++)
		{
			if(plugins[i].getClass().getName().equals(name))
			{
				String[] args = { name };
				GUIUtilities.error(null,"plugin.already-loaded",args);
				return;
			}
		}

		// Check dependencies
		if(!checkDependencies(name))
		{
			jar.addPlugin(new EditPlugin.Broken(name));
			return;
		}

		// JDK 1.1.8 throws a GPF when we do an isAssignableFrom()
		// on an unresolved class
		Class clazz = loadClass(name,true);
		int modifiers = clazz.getModifiers();
		if(!Modifier.isInterface(modifiers)
			&& !Modifier.isAbstract(modifiers)
			&& EditPlugin.class.isAssignableFrom(clazz))
		{
			String version = jEdit.getProperty("plugin."
				+ name + ".version");

			if(version == null)
			{
				Log.log(Log.WARNING,this,"Plugin " +
					name + " doesn't"
					+ " have a 'version' property.");
				version = "";
			}
			else
				version = " (version " + version + ")";

			Log.log(Log.NOTICE,this,"Starting plugin " + name
					+ version);

			jar.addPlugin((EditPlugin)clazz.newInstance());
		}
	}

	private boolean checkDependencies(String name)
	{
		int i = 0;

		String dep;
		while((dep = jEdit.getProperty("plugin." + name + ".depend." + i++)) != null)
		{
			int index = dep.indexOf(' ');
			if(index == -1)
			{
				Log.log(Log.ERROR,this,name + " has an invalid"
					+ " dependency: " + dep);
				return false;
			}

			String what = dep.substring(0,index);
			String arg = dep.substring(index + 1);

			if(what.equals("jdk"))
			{
				if(System.getProperty("java.version")
					.compareTo(arg) < 0)
				{
					String[] args = { name, arg,
						System.getProperty("java.version") };
					GUIUtilities.error(null,"plugin.dep-jdk",args);
					return false;
				}
			}
			else if(what.equals("jedit"))
			{
				if(jEdit.getBuild().compareTo(arg) < 0)
				{
					String needs = MiscUtilities.buildToVersion(arg);
					String[] args = { name, needs,
						jEdit.getVersion() };
					GUIUtilities.error(null,"plugin.dep-jedit",args);
					return false;
				}
			}
			else if(what.equals("plugin"))
			{
				int index2 = arg.indexOf(' ');
				if(index2 == -1)
				{
					Log.log(Log.ERROR,this,name 
						+ " has an invalid dependency: "
						+ dep + " (version is missing)");
					return false;
				}
				
				String plugin = arg.substring(0,index2);
				String needVersion = arg.substring(index2 + 1);
				String currVersion = jEdit.getProperty("plugin." 
					+ plugin + ".version");

				if(currVersion == null)
				{
					String[] args = { name, needVersion, plugin };
					GUIUtilities.error(null,"plugin.dep-plugin.no-version",args);
					return false;
				}
				
				if(MiscUtilities.compareVersions(currVersion,
					needVersion) < 0)
				{
					String[] args = { name, needVersion, plugin, currVersion };
					GUIUtilities.error(null,"plugin.dep-plugin",args);
					return false;
				}
			}
			else if(what.equals("class"))
			{
				try
				{
					loadClass(arg,false);
				}
				catch(Exception e)
				{
					String[] args = { name, arg };
					GUIUtilities.error(null,"plugin.dep-class",args);
					return false;
				}
			}
			else
			{
				Log.log(Log.ERROR,this,name + " has unknown"
					+ " dependency: " + dep);
				return false;
			}
		}

		return true;
	}

	private Class findOtherClass(String clazz, boolean resolveIt)
		throws ClassNotFoundException
	{
		EditPlugin.JAR[] jars = jEdit.getPluginJARs();
		for(int i = 0; i < jars.length; i++)
		{
			JARClassLoader loader = jars[i].getClassLoader();
			Class cls = loader.loadClass(clazz,resolveIt,
				false);
			if(cls != null)
				return cls;
		}

		/* Defer to whoever loaded us (such as JShell, Echidna, etc) */
                ClassLoader loader = getClass().getClassLoader();
		if (loader != null)
			return loader.loadClass(clazz);

		/* Doesn't exist in any other plugin, look in system classes */
		return findSystemClass(clazz);
	}

	private Class loadClass(String clazz, boolean resolveIt, boolean doDepencies)
		throws ClassNotFoundException
	{
		Class cls = findLoadedClass(clazz);
		if(cls != null)
		{
			if(resolveIt)
				resolveClass(cls);
			return cls;
		}

		if(zipFile == null)
		{
			if(doDepencies)
				return findOtherClass(clazz,resolveIt);
			else
				return null;
		}

		String name = MiscUtilities.classToFile(clazz);

		try
		{
			ZipEntry entry;
			if(closed)
				entry = null;
			else
				entry = zipFile.getEntry(name);

			if(entry == null)
			{
				if(doDepencies)
					return findOtherClass(clazz,resolveIt);
				else
					return null;
			}

			InputStream in = zipFile.getInputStream(entry);

			int len = (int)entry.getSize();
			byte[] data = new byte[len];
			int success = 0;
			int offset = 0;
			while(success < len)
			{
				len -= success;
				offset += success;
				success = in.read(data,offset,len);
				if(success == -1)
				{
					Log.log(Log.ERROR,this,"Failed to load class "
						+ clazz + " from " + zipFile.getName());
					throw new ClassNotFoundException(clazz);
				}
			}

			cls = defineClass(clazz,data,0,data.length);

			if(resolveIt)
				resolveClass(cls);

			return cls;
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,this,io);

			throw new ClassNotFoundException(clazz);
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.38  2000/11/19 07:51:25  sp
 * Documentation updates, bug fixes
 *
 * Revision 1.37  2000/07/29 12:24:07  sp
 * More VFS work, VFS browser started
 *
 * Revision 1.36  2000/05/14 10:55:21  sp
 * Tool bar editor started, improved view registers dialog box
 *
 * Revision 1.35  2000/04/06 09:28:08  sp
 * Better plugin error reporting, search bar updates
 *
 * Revision 1.34  2000/04/06 00:55:39  sp
 * didn't compile
 *
 * Revision 1.33  2000/04/06 00:28:14  sp
 * Resource handling bugs fixed, minor token marker tweaks
 *
 * Revision 1.32  2000/03/18 05:45:25  sp
 * Complete word overhaul, various other changes
 *
 * Revision 1.31  2000/03/14 06:22:24  sp
 * Lots of new stuff
 *
 * Revision 1.30  2000/02/27 00:39:50  sp
 * Misc changes
 *
 * Revision 1.29  2000/02/20 03:14:13  sp
 * jEdit.getBrokenPlugins() method
 *
 * Revision 1.28  2000/02/16 05:51:20  sp
 * Misc updates, dirk's changes integrated
 *
 */
