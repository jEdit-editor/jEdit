/*
 * EditPlugin.java - Interface all plugins must implement
 * Copyright (C) 1999, 2000 Slava Pestov
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

import java.util.Vector;
import org.gjt.sp.jedit.gui.OptionsDialog;

/**
 * The interface between jEdit and a plugin. Plugins provide implementations
 * of this class to register any edit modes and actions they provide, and to
 * add their commands the jEdit's menu bar.<p>
 *
 * This class obsoletes the <code>Plugin</code> interface from jEdit 2.0
 * and earlier. Its main advantage over the old system is the more flexible
 * menu bar setup code, and the fact that it is a class, rather than an
 * interface, which means methods can be added without breaking existing
 * plugins.
 *
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 2.1pre1
 */
public abstract class EditPlugin
{
	/**
	 * Returns the plugin's class name.
	 *
	 * @since jEdit 2.5pre3
	 */
	public String getClassName()
	{
		return getClass().getName();
	}

	/**
	 * Method called by jEdit to initialize the plugin.
	 * Actions and edit modes should be registered here, along
	 * with any EditBus paraphinalia.
	 * @since jEdit 2.1pre1
	 */
	public void start() {}

	/**
	 * Method called by jEdit before exiting. Usually, nothing
	 * needs to be done here.
	 * @since jEdit 2.1pre1
	 */
	public void stop() {}

	/**
	 * Method called every time a view is created to set up the
	 * Plugins menu. Menus and menu items should be loaded using the
	 * methods in the GUIUtilities class, and added to the appropriate
	 * lists.
	 * @param view The view
	 * @param menus Add submenus here
	 * @param menuItems Add menuitems here
	 *
	 * @see GUIUtilities#loadMenu(View,String)
	 * @see GUIUtilities#loadMenuItem(View,String)
	 *
	 * @since jEdit 2.1pre1
	 */
	public void createMenuItems(View view, Vector menus, Vector menuItems) {}

	/**
	 * Method called every time the plugin options dialog box is
	 * displayed. Any option panes created by the plugin should be
	 * added here.
	 * @param optionsDialog The plugin options dialog box
	 *
	 * @see OptionPane
	 * @see OptionsDialog#addOptionPane(OptionPane)
	 *
	 * @since jEdit 2.1pre1
	 */
	public void createOptionPanes(OptionsDialog optionsDialog) {}

	/**
	 * A placeholder for a plugin that didn't load.
	 */
	public static class Broken extends EditPlugin
	{
		public String getClassName()
		{
			return clazz;
		}

		// package-private members
		Broken(String clazz)
		{
			this.clazz = clazz;
		}

		// private members
		private String clazz;
	}

	/**
	 * A JAR file.
	 */
	public static class JAR
	{
		public String getPath()
		{
			return path;
		}

		public JARClassLoader getClassLoader()
		{
			return classLoader;
		}

		public void addPlugin(EditPlugin plugin)
		{
			plugin.start();
			plugins.addElement(plugin);
		}

		public EditPlugin[] getPlugins()
		{
			EditPlugin[] array = new EditPlugin[plugins.size()];
			plugins.copyInto(array);
			return array;
		}

		public int getIndex()
		{
			return index;
		}

		// package-private members
		int index;

		JAR(String path, JARClassLoader classLoader)
		{
			this.path = path;
			this.classLoader = classLoader;
			plugins = new Vector();
		}

		public void getPlugins(Vector vector)
		{
			for(int i = 0; i < plugins.size(); i++)
			{
				vector.addElement(plugins.elementAt(i));
			}
		}

		// private members
		private String path;
		private JARClassLoader classLoader;
		private Vector plugins;
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.5  2000/05/14 10:55:21  sp
 * Tool bar editor started, improved view registers dialog box
 *
 * Revision 1.4  2000/02/20 03:14:13  sp
 * jEdit.getBrokenPlugins() method
 *
 * Revision 1.3  1999/12/11 06:34:39  sp
 * Bug fixes
 *
 * Revision 1.2  1999/10/10 06:38:45  sp
 * Bug fixes and quicksort routine
 *
 * Revision 1.1  1999/09/30 12:21:04  sp
 * No net access for a month... so here's one big jEdit 2.1pre1
 *
 */
