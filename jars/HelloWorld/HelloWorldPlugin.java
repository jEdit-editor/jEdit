/*
 * HelloWorldPlugin.java - Sample plugin
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

import org.gjt.sp.jedit.*;
import java.util.Vector;

public class HelloWorldPlugin extends EditPlugin
{
	/**
	 * Method called by jEdit to initialize the plugin.
	 */
	//public void start() {}

	/**
	 * Method called by jEdit before exiting. Usually, nothing
	 * needs to be done here.
	 */
	//public void stop() {}

	/**
	 * Method called every time a view is created to set up the
	 * Plugins menu. Menus and menu items should be loaded using the
	 * methods in the GUIUtilities class, and added to the list.
	 * @param menuItems Add menuitems here
	 */
	public void createMenuItems(Vector menuItems)
	{
		menuItems.addElement(GUIUtilities.loadMenuItem("hello-world"));
	}

	/**
	 * Method called every time the plugin options dialog box is
	 * displayed. Any option panes created by the plugin should be
	 * added here.
	 * @param optionsDialog The plugin options dialog box
	 *
	 * @see OptionPane
	 * @see OptionsDialog#addOptionPane(OptionPane)
	 */
	//public void createOptionPanes(OptionsDialog optionsDialog) {}

	/**
	 * Displays the 'hello world' dialog box. This method is called
	 * by the hello-world action, defined in actions.xml.
	 */
	public static void showHelloWorldDialog(View view)
	{
		GUIUtilities.message(view,"hello-world",null);
	}
}
