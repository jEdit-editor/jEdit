/*
 * JARClassLoader.java - Loads classes from JAR files
 * Copyright (C) 1999 Slava Pestov
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
	public JARClassLoader(String path)
		throws IOException
	{
		fileName = new File(path).getName();
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

		// If this is done before the above while() statement
		// and an exception is thrown while the ZIP file is
		// being loaded, weird things happen...
		index = classLoaders.size();
		classLoaders.addElement(this);
	}

	/**
	 * @exception ClassNotFoundException if the class could not be found
	 */
	public Class loadClass(String clazz, boolean resolveIt)
		throws ClassNotFoundException
	{
		return loadClassFromZip(clazz,resolveIt,true);
	}

	public InputStream getResourceAsStream(String name)
	{
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
		return "jeditresource:" + index + "/" + name;
	}

	public String getPath()
	{
		return zipFile.getName();
	}

	public static void initPlugins()
	{
		for(int i = 0; i < classLoaders.size(); i++)
		{
			JARClassLoader classLoader = (JARClassLoader)
				classLoaders.elementAt(i);
			classLoader.loadAllPlugins();
		}
	}

	public static JARClassLoader getClassLoader(int index)
	{
		return (JARClassLoader)classLoaders.elementAt(index);
	}

	public static int getClassLoaderCount()
	{
		return classLoaders.size();
	}

	// private members

	/* Loading of plugin classes is deferred until all JARs
	 * are loaded - this is necessary because a plugin might
	 * depend on classes stored in other JARs. */
	private static Vector classLoaders = new Vector();
	private int index;
	private Vector pluginClasses = new Vector();
	private String fileName;
	private ZipFile zipFile;

	private void loadAllPlugins()
	{
		for(int i = 0; i < pluginClasses.size(); i++)
		{
			String name = (String)pluginClasses.elementAt(i);

			try
			{
				loadPluginClass(name);
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,this,"Error while starting plugin " + name);
				Log.log(Log.ERROR,this,t);

				jEdit.addBrokenPlugin(fileName,name);
			}
		}
	}

	private void loadPluginClass(String name)
		throws Exception
	{
		name = MiscUtilities.fileToClass(name);

		// Check if a plugin with the same name is already loaded
		EditPlugin[] plugins = jEdit.getPlugins();

		for(int i = 0; i < plugins.length; i++)
		{
			if(plugins[i].getClass().getName().equals(name))
			{
				Log.log(Log.WARNING,this,"A plugin named "
					+ name + " is already loaded");
				return;
			}
		}

		// Check dependencies
		if(!checkDependencies(name))
		{
			jEdit.addBrokenPlugin(fileName,name);
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
				Log.log(Log.ERROR,this,"Plugin " +
					name + " doesn't"
					+ " specify a 'version' property.");
				Log.log(Log.ERROR,this,"This property"
					+ " must be defined for the plugin manager"
					+ " to work.");
				jEdit.addBrokenPlugin(fileName,name);
				return;
			}
			else
			{
				Log.log(Log.NOTICE,this,"Starting plugin " + name
					+ " (version " + version + ")");
			}

			jEdit.addPlugin((EditPlugin)clazz.newInstance());
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
					Log.log(Log.ERROR,this,name + " requires"
						+ " Java " + arg);
					return false;
				}
			}
			else if(what.equals("jedit"))
			{
				if(jEdit.getBuild().compareTo(arg) < 0)
				{
					Log.log(Log.ERROR,this,name + " requires"
						+ " jEdit " + MiscUtilities
						.buildToVersion(arg));
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
					Log.log(Log.ERROR,this,"No version info"
						+ " found in plugin " + plugin
						+ ". Plugin " + name + " needs"
						+ " version " + needVersion);
					return false;
				}
				
				if(MiscUtilities.compareVersions(currVersion,
					needVersion) < 0)
				{
					Log.log(Log.ERROR,this,name + " requires"
						+ " plugin " + plugin + " version "
						+ needVersion);
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
					Log.log(Log.ERROR,this,name +
						" requires class "
						+ arg);
					return false;
				}
			}
			else if(what.equals("jext"))
			{
				Log.log(Log.ERROR,this,name + " is not a jEdit"
					+ " plugin");
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
		for(int i = 0; i < classLoaders.size(); i++)
		{
			JARClassLoader loader = (JARClassLoader)
				classLoaders.elementAt(i);
			Class cls = loader.loadClassFromZip(clazz,resolveIt,
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

	private Class loadClassFromZip(String clazz, boolean resolveIt,
		boolean doDepencies)
		throws ClassNotFoundException
	{
		Class cls = findLoadedClass(clazz);
		if(cls != null)
		{
			if(resolveIt)
				resolveClass(cls);
			return cls;
		}

		String name = MiscUtilities.classToFile(clazz);

		try
		{
			ZipEntry entry = zipFile.getEntry(name);

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
 * Revision 1.29  2000/02/20 03:14:13  sp
 * jEdit.getBrokenPlugins() method
 *
 * Revision 1.28  2000/02/16 05:51:20  sp
 * Misc updates, dirk's changes integrated
 *
 * Revision 1.27  2000/02/07 06:35:52  sp
 * Options dialog box updates
 *
 * Revision 1.26  2000/01/30 04:23:23  sp
 * New about box, minor bug fixes and updates here and there
 *
 * Revision 1.25  2000/01/28 00:54:26  sp
 * Blacklisting removed
 *
 * Revision 1.24  2000/01/28 00:20:58  sp
 * Lots of stuff
 *
 * Revision 1.23  2000/01/16 06:09:27  sp
 * Bug fixes
 *
 * Revision 1.22  1999/11/27 06:01:20  sp
 * Faster file loading, geometry fix
 *
 * Revision 1.21  1999/11/21 01:20:30  sp
 * Bug fixes, EditBus updates, fixed some warnings generated by jikes +P
 *
 * Revision 1.20  1999/11/09 10:14:34  sp
 * Macro code cleanups, menu item and tool bar clicks are recorded now, delete
 * word commands, check box menu item support
 *
 * Revision 1.19  1999/10/31 07:15:34  sp
 * New logging API, splash screen updates, bug fixes
 *
 */
