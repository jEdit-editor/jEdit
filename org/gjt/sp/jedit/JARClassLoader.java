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
		index = classLoaders.size();
		classLoaders.addElement(this);

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
	}

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

	// private members

	/* Loading of plugin classes is deferred until all JARs
	 * are loaded - this is necessary because a plugin might
	 * depend on classes stored in other JARs. */
	private static Vector classLoaders = new Vector();
	private int index;
	private Vector pluginClasses = new Vector();
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
				Log.log(Log.ERROR,this,t);
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
			return;

		// JDK 1.1.8 throws a GPF when we do an isAssignableFrom()
		// on an unresolved class
		Class clazz = loadClass(name,true);
		int modifiers = clazz.getModifiers();
		if(!Modifier.isInterface(modifiers)
			&& !Modifier.isAbstract(modifiers)
			&& EditPlugin.class.isAssignableFrom(clazz))
		{
			Log.log(Log.NOTICE,this,"Starting plugin " + name);

			jEdit.addPlugin((EditPlugin)clazz.newInstance());
		}
	}

	private boolean checkDependencies(String name)
	{
		int i = 0;

		// For `failed dependencies' error message
		StringBuffer deps = new StringBuffer();

		String[] args = { name };
		deps.append(jEdit.getProperty("jar.error.deps",args));
		deps.append('\n');

		boolean ok = true;

		String dep;
		while((dep = jEdit.getProperty("plugin." + name + ".depend." + i++)) != null)
		{
			int index = dep.indexOf(' ');
			if(index == -1)
			{
				deps.append(dep);
				deps.append('\n');
				ok = false;
				continue;
			}

			String what = dep.substring(0,index);
			String arg = dep.substring(index + 1);

			String[] args2 = new String[1];
			if(what.equals("jedit"))
				args2[0] = MiscUtilities.buildToVersion(arg);
			else
				args2[0] = arg;

			deps.append(jEdit.getProperty("jar.what." + what,args2));
			deps.append('\n');

			if(what.equals("jdk"))
			{
				if(System.getProperty("java.version")
					.compareTo(arg) < 0)
					ok = false;
			}
			else if(what.equals("jedit"))
			{
				if(jEdit.getBuild().compareTo(arg) < 0)
					ok = false;
			}
			else if(what.equals("class"))
			{
				try
				{
					loadClass(arg,false);
				}
				catch(Exception e)
				{
					ok = false;
				}
			}
			else
				ok = false;
		}

		if(!ok)
			Log.log(Log.WARNING,this,deps);

		return ok;
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
 * Revision 1.19  1999/10/31 07:15:34  sp
 * New logging API, splash screen updates, bug fixes
 *
 * Revision 1.18  1999/10/10 06:38:45  sp
 * Bug fixes and quicksort routine
 *
 * Revision 1.17  1999/10/05 04:43:58  sp
 * Minor bug fixes and updates
 *
 * Revision 1.16  1999/10/04 03:20:51  sp
 * Option pane change, minor tweaks and bug fixes
 *
 * Revision 1.15  1999/09/30 12:21:04  sp
 * No net access for a month... so here's one big jEdit 2.1pre1
 *
 * Revision 1.13  1999/08/21 01:48:18  sp
 * jEdit 2.0pre8
 *
 * Revision 1.12  1999/06/07 09:02:40  sp
 * Minor JAR loader tweak
 *
 * Revision 1.11  1999/05/22 08:33:53  sp
 * FAQ updates, mode selection tweak, patch mode update, javadoc updates, JDK 1.1.8 fix
 *
 * Revision 1.10  1999/05/15 00:29:19  sp
 * Prev error bug fix, doc updates, tips updates
 */
