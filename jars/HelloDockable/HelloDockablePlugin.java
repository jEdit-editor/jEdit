/*
 * HelloDockablePlugin.java - Sample dockable plugin
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

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.msg.*;
import java.awt.event.ActionEvent;
import java.awt.Component;
import java.util.Vector;

/**
 * The 'Plugin' class is the interface between jEdit and the plugin.
 * Plugins can either extend EditPlugin or EBPlugin. EBPlugins have
 * the additional property that they receive EditBus messages.
 */
public class HelloDockablePlugin extends EBPlugin
{
	/**
	 * The 'name' of our dockable window.
	 */
	public static final String NAME = "hello-dockable";

	/**
	 * Method called by jEdit to initialize the plugin.
	 */
	public void start()
	{
		// add our dockable to the dockables 'named list'
		EditBus.addToNamedList(DockableWindow.DOCKABLE_WINDOW_LIST,NAME);
	}

	/**
	 * Method called by jEdit before exiting. Usually, nothing
	 * needs to be done here.
	 */
	//public void stop() {}

	/**
	 * Method called every time a view is created to set up the
	 * Plugins menu. Menus and menu items should be loaded using the
	 * methods in the GUIUtilities class, and added to the list.
	 * @param menuItems Add the menu item here
	 */
	public void createMenuItems(Vector menuItems)
	{
		menuItems.addElement(GUIUtilities.loadMenuItem("hello-dockable"));
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
	 * Handles a message sent on the EditBus. The default
	 * implementation ignores the message.
	 */
	public void handleMessage(EBMessage message)
	{
		/* upon receiving a CreateDockableWindow, we check if
		 * the name of the requested window is 'hello-dockable',
		 * and create it if it is so.
		 */
		if(message instanceof CreateDockableWindow)
		{
			CreateDockableWindow cmsg = (CreateDockableWindow)message;
			if(cmsg.getDockableWindowName().equals(NAME))
				cmsg.setDockableWindow(new HelloDockable());
		}
	}
}
