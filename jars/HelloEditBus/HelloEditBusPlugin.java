/*
 * HelloEditBusPlugin.java - Sample EB plugin
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
import org.gjt.sp.jedit.msg.*;
import java.util.Vector;

/**
 * The 'Plugin' class is the interface between jEdit and the plugin.
 * Plugins can either extend EditPlugin or EBPlugin. EBPlugins have
 * the additional property that they receive EditBus messages.
 *
 * If your plugin only _sends_ EditBus messages (eg, if all it does
 * is maintain an ErrorSource) there is no need to extend EBPlugin.
 *
 * Only extend EBPlugin if your plugin needs to _receive_ messages
 * (eg, if your plugin needs to know when views or buffers are
 * being opened)
 */
public class HelloEditBusPlugin extends EBPlugin
{
	/**
	 * Method called by jEdit to initialize the plugin.
	 */
	public void start()
	{
		// initalize error source
		errorSource = new DefaultErrorSource("HelloEditBus");
		EditBus.addToBus(errorSource);
		EditBus.addToNamedList(ErrorSource.ERROR_SOURCES_LIST,errorSource);
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
	 * @param menuItems Add menuitems here
	 */
	//public void createMenuItems(Vector menuItems) {}

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
		if(message instanceof BufferUpdate)
		{
			BufferUpdate bu = (BufferUpdate)message;
			if(bu.getWhat() == BufferUpdate.CREATED)
			{
				// a buffer was created, so add some
				// sample errors on line 10 and 11.

				errorSource.addError(ErrorSource.WARNING,
					bu.getBuffer().getPath(),
					9,0,0,"A warning");

				errorSource.addError(ErrorSource.ERROR,
					bu.getBuffer().getPath(),
					10,0,0,"An error");
			}
		}
	}

	// private members
	private DefaultErrorSource errorSource;
}
