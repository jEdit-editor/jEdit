/*
 * jEdit.java - Main class of the jEdit editor
 * Copyright (C) 1998, 1999 Slava Pestov
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

import javax.swing.event.EventListenerList;
import javax.swing.text.Element;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.MessageFormat;
import java.util.*;
import org.gjt.sp.jedit.event.*;
import org.gjt.sp.jedit.gui.HistoryModel;
import org.gjt.sp.jedit.search.SearchAndReplace;
import org.gjt.sp.jedit.textarea.DefaultInputHandler;
import org.gjt.sp.jedit.textarea.InputHandler;

/**
 * The main class of the jEdit text editor.
 * @author Slava Pestov
 * @version $Id$
 */
public class jEdit
{
	/**
	 * Returns the jEdit version as a human-readable string.
	 */
	public static String getVersion()
	{
		return MiscUtilities.buildToVersion(getBuild());
	}

	/**
	 * Returns the internal version. String.compareTo() can be used
	 * to compare different internal versions.
	 */
	public static String getBuild()
	{
		// (major) (minor) (<99 = preX, 99 = final) (bug fix)
		return "02.01.99.00";
	}

	/**
	 * The main method of the jEdit application.
	 * <p>
	 * This should never be invoked directly.
	 * @param args The command line arguments
	 */
	public static void main(String[] args)
	{
		// Parse command line
		boolean endOpts = false;
		boolean readOnly = false;
		boolean reuseView = false;
		settingsDirectory = System.getProperty("user.home") +
			File.separator + ".jedit";
		String portFile = "server";
		boolean desktop = true;
		boolean showSplash = true;

		for(int i = 0; i < args.length; i++)
		{
			String arg = args[i];
			if(arg.startsWith("-") && !endOpts)
			{
				if(arg.equals("--"))
					endOpts = true;
				else if(arg.equals("-usage"))
				{
					version();
					System.err.println();
					usage();
					System.exit(1);
				}
				else if(arg.equals("-version"))
				{
					version();
					System.exit(1);
				}
				else if(arg.equals("-nosettings"))
					settingsDirectory = null;
				else if(arg.startsWith("-settings="))
					settingsDirectory = arg.substring(10);
				else if(arg.startsWith("-noserver"))
					portFile = null;
				else if(arg.startsWith("-server="))
					portFile = arg.substring(8);
				else if(arg.equals("-nodesktop"))
					desktop = false;
				else if(arg.equals("-nosplash"))
					showSplash = false;
				else if(arg.equals("-readonly"))
					readOnly = true;
				else if(arg.equals("-reuseview"))
					reuseView = true;
				else
				{
					System.err.println("Unknown option: "
						+ arg);
					usage();
					System.exit(1);
				}
				args[i] = null;
			}
		}

		if(settingsDirectory != null && portFile != null)
			portFile = settingsDirectory + File.separator + portFile;
		else
			portFile = null;

		// Connect to server
		String userDir = System.getProperty("user.dir");

		if(portFile != null && new File(portFile).exists())
		{
			int port, key;
			try
			{
				BufferedReader in = new BufferedReader(new FileReader(portFile));
				port = Integer.parseInt(in.readLine());
				key = Integer.parseInt(in.readLine());
				in.close();

				Socket socket = new Socket(InetAddress.getLocalHost(),port);
				Writer out = new OutputStreamWriter(socket.getOutputStream());
				out.write(String.valueOf(key));
				out.write('\n');

				if(readOnly)
					out.write("readonly\n");
				if(reuseView)
					out.write("reuseview\n");
				out.write("parent=" + userDir + "\n");
				out.write("--\n");

				for(int i = 0; i < args.length; i++)
				{
					if(args[i] != null)
					{
						out.write(args[i]);
						out.write('\n');
					}
				}

				out.close();

				System.exit(0);
			}
			catch(Exception e)
			{
				System.err.println("Error connecting to server:");
				e.printStackTrace();
			}
		}

		// GUIUtilities.static() adds a listener to our list,
		// so it must be available before GUIUtilities is first used
		listenerList = new EventListenerList();

		// Show the kool splash screen
		if(showSplash)
			GUIUtilities.showSplashScreen();

		// Get things rolling
		initMisc();
		initSystemProperties();
		initModes();
		initPlugins();
		initUserProperties();
		initActions();
		initPLAF();
		initKeyBindings();
		propertiesChanged();
		initRecent();
		if(settingsDirectory != null)
		{
			HistoryModel.loadHistory(settingsDirectory + File.separator
				+ "history");
		}
		SearchAndReplace.load();

		// Start plugins
		JARClassLoader.initPlugins();

		// Start server
		if(portFile != null)
			server = new EditServer(portFile);

		// Load files specified on the command line
		Buffer buffer = null;
		for(int i = 0; i < args.length; i++)
		{
			if(args[i] == null)
				continue;
			buffer = openFile(null,userDir,args[i],readOnly,false);
		}
		if(buffer == null)
		{
			if("on".equals(getProperty("saveDesktop")) && desktop)
				buffer = loadDesktop();
		}
		if(buffer == null)
			buffer = newFile(null);

		// Create the view and hide the splash screen.
		// Paranoid thread safety courtesy of Sun.
		final Buffer _buffer = buffer;

		SwingUtilities.invokeLater(new Runnable() {
			public void run()
			{
				newView(null,_buffer);
				GUIUtilities.hideSplashScreen();
			}
		});
	}

	/**
	 * Loads the properties from the specified input stream. This
	 * calls the <code>load()</code> method of the properties object
	 * and closes the stream.
	 * @param in The input stream
	 * @exception IOException if an I/O error occured
	 */
	public static void loadProps(InputStream in)
		throws IOException
	{
		props.load(new BufferedInputStream(in));
		in.close();
	}

	/**
	 * Fetches a property, returning null if it's not defined.
	 * @param name The property
	 */
	public static final String getProperty(String name)
	{
		return props.getProperty(name);
	}

	/**
	 * Fetches a property, returning the default value if it's not
	 * defined.
	 * @param name The property
	 * @param def The default value
	 */
	public static final String getProperty(String name, String def)
	{
		return props.getProperty(name,def);
	}

	/**
	 * Returns the property with the specified name, formatting it with
	 * the <code>java.text.MessageFormat.format()</code> method.
	 * @param name The property
	 * @param args The positional parameters
	 */
	public static final String getProperty(String name, Object[] args)
	{
		if(name == null)
			return null;
		if(args == null)
			return props.getProperty(name,name);
		else
			return MessageFormat.format(props.getProperty(name,
				name),args);
	}

	/**
	 * Sets a property to a new value.
	 * @param name The property
	 * @param value The new value
	 */
	public static final void setProperty(String name, String value)
	{
		/* if value is null:
		 * - if default is null, unset user prop
		 * - else set user prop to ""
		 * else
		 * - if default equals value, ignore
		 * - if default doesn't equal value, set user
		 */
		if(value == null || value.length() == 0)
		{
			String prop = (String)defaultProps.get(name);
			if(prop == null || prop.length() == 0)
				props.remove(name);
			else
				props.put(name,"");
		}
		else
		{
			if(!value.equals(getProperty(name)))
				props.put(name,value);
		}
	}

	/**
	 * Sets a property to a new value. This method does not override
	 * properties set in the user property list, nor are the
	 * properties set with this method saved in the user properties.
	 * @param name The property
	 * @param value The new value
	 */
	public static final void setDefaultProperty(String name, String value)
	{
		defaultProps.put(name,value);
	}

	/**
	 * Unsets (clears) a property.
	 * @param name The property
	 */
	public static final void unsetProperty(String name)
	{
		if(defaultProps.get(name) != null)
			props.put(name,"");
		else
			props.remove(name);
	}

	/**
	 * Reloads various settings from the properties.
	 */
	public static void propertiesChanged()
	{
		// Auto save
		if(autosave != null)
			autosave.interrupt();

		autosave = new Autosave();

		// Recent files
		try
		{
			maxRecent = Integer.parseInt(getProperty("recent"));
		}
		catch(NumberFormatException nf)
		{
			maxRecent = 8;
		}

		fireEditorEvent(EditorEvent.PROPERTIES_CHANGED,null,null);
	}

	/**
	 * Loads all plugins in a directory.
	 * @param directory The directory
	 */
	public static void loadPlugins(String directory)
	{
		String[] args = { directory };
		System.out.println(jEdit.getProperty(
			"jar.scanningdir",args));

		File file = new File(directory);
		if(!(file.exists() || file.isDirectory()))
			return;
		String[] plugins = file.list();
		MiscUtilities.quicksort(plugins,new MiscUtilities.StringICaseCompare());

		if(plugins == null)
			return;
		for(int i = 0; i < plugins.length; i++)
		{
			String plugin = plugins[i];
			if(!plugin.toLowerCase().endsWith(".jar"))
				continue;
			try
			{
				new JARClassLoader(directory + File.separator
					+ plugin);
			}
			catch(IOException io)
			{
				String[] args2 = { plugin };
				System.err.println(jEdit.getProperty(
					"jar.error.load",args2));
				io.printStackTrace();
			}
		}
	}

	/**
	 * Adds a plugin to the editor.
	 * @param plugin The plugin
	 */
	public static void addPlugin(EditPlugin plugin)
	{
		plugin.start();
		plugins.addElement(plugin);
	}

	/**
	 * Returns a plugin by it's class name.
	 * @param name The plugin to return
	 */
	public static EditPlugin getPlugin(String name)
	{
		for(int i = 0; i < plugins.size(); i++)
		{
			EditPlugin p = (EditPlugin)plugins.elementAt(i);
			if(p.getClass().getName().equals(name))
				return p;
		}

		return null;
	}

	/**
	 * Returns an array of installed plugins.
	 */
	public static EditPlugin[] getPlugins()
	{
		EditPlugin[] pluginArray = new EditPlugin[plugins.size()];
		plugins.copyInto(pluginArray);
		return pluginArray;
	}

	/**
	 * Registers an action with the editor.
	 * @param action The action
	 */
	public static void addAction(EditAction action)
	{
		String name = action.getName();
		actionHash.put(name,action);

		// Register key binding
		String binding = getProperty(name + ".shortcut");
		if(binding != null)
			inputHandler.addKeyBinding(binding,action);
	}

	/**
	 * Returns a named action.
	 * @param action The action
	 */
	public static EditAction getAction(String action)
	{
		return (EditAction)actionHash.get(action);
	}

	/**
	 * Returns the list of actions registered with the editor.
	 */
	public static EditAction[] getActions()
	{
		EditAction[] actions = new EditAction[actionHash.size()];
		Enumeration enum = actionHash.elements();
		int i = 0;
		while(enum.hasMoreElements())
		{
			actions[i++] = (EditAction)enum.nextElement();
		}
		return actions;
	}

	/**
	 * Registers an edit mode with the editor.
	 * @param mode The edit mode
	 */
	public static void addMode(Mode mode)
	{
		modes.addElement(mode);
	}

	/**
	 * Returns the edit mode with the specified name.
	 * @param name The edit mode
	 */
	public static Mode getMode(String name)
	{
		for(int i = 0; i < modes.size(); i++)
		{
			Mode mode = (Mode)modes.elementAt(i);
			if(mode.getName().equals(name))
				return mode;
		}
		return null;
	}

	/**
	 * Returns the localised name of an edit mode.
	 * @param mode The edit mode
	 */
	public static String getModeName(Mode mode)
	{
		return jEdit.props.getProperty("mode." +
			mode.getName() + ".name");
	}

	/**
	 * Returns an array of installed edit modes.
	 */
	public static Mode[] getModes()
	{
		Mode[] array = new Mode[modes.size()];
		modes.copyInto(array);
		return array;
	}

	/**
	 * Opens a file.
	 * @param view The view to open the file in
	 * @param parent The parent directory of the file
	 * @param path The path name of the file
	 * @param readOnly True if the file should be read only
	 * @param newFile True if the file should not be loaded from disk
	 */
	public static Buffer openFile(View view, String parent, String path,
		boolean readOnly, boolean newFile)
	{
		if(view != null && parent == null)
			parent = view.getBuffer().getFile().getParent();

		int index = path.indexOf('#');
		String marker = null;
		if(index != -1)
		{
			marker = path.substring(index + 1);
			path = path.substring(0,index);
		}

		// Java doesn't currently support saving to file:// URLs,
		// hence the crude hack here
		if(path.startsWith("file:"))
			path = path.substring(5);

		URL url = null;
		try
		{
			url = new URL(path);
		}
		catch(MalformedURLException mu)
		{
			path = MiscUtilities.constructPath(parent,path);

		}
		Buffer buffer = buffersFirst;
		while(buffer != null)
		{
			if(buffer.getPath().equals(path))
			{
				if(view != null)
				{
					if(marker != null)
						gotoMarker(buffer,view,marker);
					view.setBuffer(buffer);
				}
				return buffer;
			}
			buffer = buffer.next;
		}

		// Show the wait cursor because there certainly is going
		// to be a file load
		if(view != null)
			GUIUtilities.showWaitCursor(view);

		buffer = new Buffer(view,url,path,readOnly,newFile);
		addBufferToList(buffer);

		if(marker != null)
			gotoMarker(buffer,null,marker);

		if(view != null)
		{
			view.setBuffer(buffer);

			// Hide wait cursor
			GUIUtilities.hideWaitCursor(view);
		}

		fireEditorEvent(EditorEvent.BUFFER_CREATED,view,buffer);

		return buffer;
	}

	/**
	 * Creates a new `untitled' file.
	 * @param view The view to create the file in
	 */
	public static Buffer newFile(View view)
	{
		// Find the highest Untitled-n file
		int untitledCount = 0;
		Buffer buffer = buffersFirst;
		while(buffer != null)
		{
			if(buffer.getName().startsWith("Untitled-"))
			{
				try
				{
					untitledCount = Math.max(untitledCount,
						Integer.parseInt(buffer.getName()
						.substring(9)));
				}
				catch(NumberFormatException nf)
				{
				}
			}
			buffer = buffer.next;
		}

		return openFile(view,null,"Untitled-" + (untitledCount+1),false,true);
	}

	/**
	 * Closes a buffer. If there are unsaved changes, the user is
	 * prompted if they should be saved first.
	 * @param view The view
	 * @param buffer The buffer
	 * @return True if the buffer was really closed, false otherwise
	 */
	public static boolean closeBuffer(View view, Buffer buffer)
	{
		if(_closeBuffer(view,buffer))
		{
			removeBufferFromList(buffer);
			buffer.getAutosaveFile().delete();
			buffer.close();
			fireEditorEvent(EditorEvent.BUFFER_CLOSED,view,buffer);

			// Create a new file when the last is closed
			if(buffersFirst == null && buffersLast == null)
				newFile(view);

			return true;
		}
		else
			return false;
	}

	/**
	 * Closes all open buffers.
	 */
	public static void closeAllBuffers(View view)
	{
		// Close all buffers
		Buffer buffer = buffersFirst;
		while(buffer != null)
		{
			if(!_closeBuffer(view,buffer))
				return;
			buffer.getAutosaveFile().delete();
			buffer = buffer.next;
		}

		GUIUtilities.showWaitCursor(view);

		// Once we are sure they're all going to be blown away,
		// fire BUFFER_CLOSED events
		buffer = buffersFirst;
		while(buffer != null)
		{
			fireEditorEvent(EditorEvent.BUFFER_CLOSED,view,buffer);
			buffer = buffer.next;
		}

		// Create a new untitled file
		buffersFirst = buffersLast = null;
		bufferCount = 0;
		newFile(view);

		// newFile() calls openFile(), which hides the wait cursor
		// XXX
	}

	/**
	 * Returns the buffer with the specified path name.
	 * @param path The path name
	 */
	public static Buffer getBuffer(String path)
	{
		Buffer buffer = buffersFirst;
		while(buffer != null)
		{
			if(buffer.getPath().equals(path))
				return buffer;
			buffer = buffer.next;
		}
		return null;
	}

	/**
	 * Returns an array of open buffers.
	 */
	public static Buffer[] getBuffers()
	{
		Buffer[] buffers = new Buffer[bufferCount];
		Buffer buffer = buffersFirst;
		for(int i = 0; i < bufferCount; i++)
		{
			buffers[i] = buffer;
			buffer = buffer.next;
		}
		return buffers;
	}

	/**
	 * Returns the first buffer.
	 */
	public static Buffer getFirstBuffer()
	{
		return buffersFirst;
	}

	/**
	 * Returns the last buffer.
	 */
	public static Buffer getLastBuffer()
	{
		return buffersLast;
	}

	/**
	 * Returns the current input handler (key binding to action mapping)
	 * @see org.gjt.sp.jedit.textarea.InputHandler
	 */
	public static InputHandler getInputHandler()
	{
		return inputHandler;
	}

	/**
	 * Creates a new view of a buffer.
	 * @param view The view from which to take the geometry, buffer and
	 * caret position from
	 * @param buffer The buffer
	 */
	public static View newView(View view, Buffer buffer)
	{
		if(view != null)
		{
			GUIUtilities.showWaitCursor(view);
			view.saveCaretInfo();
		}

		View newView = new View(view,buffer);
		addViewToList(newView);

		fireEditorEvent(EditorEvent.VIEW_CREATED,newView,newView.getBuffer());

		// Do this crap here so that the view is created
		// and added to the list before it is shown
		// (for the sake of plugins that add stuff to views)
		newView.pack();

		if(view != null)
		{
			newView.setSize(view.getSize());
			Point location = view.getLocation();
			location.x += 20;
			location.y += 20;
			newView.setLocation(location);

			GUIUtilities.hideWaitCursor(view);
		}
		else
		{
			GUIUtilities.loadGeometry(newView,"view");
		}

		newView.show();
		newView.focusOnTextArea();
		newView.addWindowListener(windowHandler);

		return newView;
	}

	/**
	 * Closes a view. jEdit will exit if this was the last open view.
	 */
	public static void closeView(View view)
	{
		if(viewsFirst == viewsLast)
			exit(view); /* exit does editor event & save */
		else
		{
			fireEditorEvent(EditorEvent.VIEW_CLOSED,view,view.getBuffer());

			view.close();
			removeViewFromList(view);
		}
	}

	/**
	 * Returns an array of all open views.
	 */
	public static View[] getViews()
	{
		View[] views = new View[viewCount];
		View view = viewsFirst;
		for(int i = 0; i < viewCount; i++)
		{
			views[i] = view;
			view = view.next;
		}
		return views;
	}

	/**
	 * Returns the first view.
	 */
	public static View getFirstView()
	{
		return viewsFirst;
	}

	/**
	 * Returns the last view.
	 */
	public static View getLastView()
	{
		return viewsLast;
	}

	/**
	 * Returns an array of recently opened files.
	 */
	public static String[] getRecent()
	{
		String[] array = new String[recent.size()];
		recent.copyInto(array);
		return array;
	}

	/**
	 * Returns the jEdit install directory.
	 */
	public static String getJEditHome()
	{
		return jEditHome;
	}

	/**
	 * Returns the user settings directory.
	 */
	public static String getSettingsDirectory()
	{
		return settingsDirectory;
	}

	/**
	 * Adds an editor event listener to the global editor listener
	 * list.
	 * @param listener The editor event listener
	 */
	public static final void addEditorListener(EditorListener listener)
	{
		listenerList.add(EditorListener.class,listener);
	}

	/**
	 * Removes an editor event listener from the global editor listener
	 * list.
	 * @param listener The editor event listener
	 */
	public static final void removeEditorListener(EditorListener listener)
	{
		listenerList.remove(EditorListener.class,listener);
	}

	/**
	 * Exits cleanly from jEdit, prompting the user if any unsaved files
	 * should be saved first.
	 * @param view The view from which this exit was called
	 */
	public static void exit(View view)
	{
		// Save the `desktop'
		if("on".equals(getProperty("saveDesktop")))
		{
			int bufNum = 0;
			view.saveCaretInfo();
			Buffer buffer = buffersFirst;
			while(buffer != null)
			{
				if(buffer.isNewFile())
				{
					buffer = buffer.next;
					continue;
				}
				setProperty("desktop." + bufNum + ".path",
					buffer.getPath());
				setProperty("desktop." + bufNum + ".mode",
					buffer.getMode().getName());
				setProperty("desktop." + bufNum + ".readOnly",
					buffer.isReadOnly() ? "yes" : "no");
				setProperty("desktop." + bufNum + ".rectSelect",
					buffer.isSelectionRectangular() ? "yes" : "no");
				setProperty("desktop." + bufNum + ".current",
					view.getBuffer() == buffer ? "yes" : "no");
				setProperty("desktop." + bufNum + ".selStart",
					String.valueOf(buffer.getSavedSelStart()));
				setProperty("desktop." + bufNum + ".selEnd",
					String.valueOf(buffer.getSavedSelEnd()));
				bufNum++;
				buffer = buffer.next;
			}
			unsetProperty("desktop." + bufNum + ".path");
		}

		// Close all buffers
		Buffer buffer = buffersFirst;
		while(buffer != null)
		{
			if(!_closeBuffer(view,buffer))
				return;
			buffer = buffer.next;
		}

		// Stop autosave thread
		autosave.stop();

		// Delete autosave files
		buffer = buffersFirst;
		while(buffer != null)
		{
			buffer.getAutosaveFile().delete();
			buffer = buffer.next;
		}

		// Stop server here
		if(server != null)
			server.stopServer();

		// Only save view properties here - save unregisters
		// listeners, and we would have problems if the user
		// closed a view but cancelled an unsaved buffer close
		view.close();

		// Stop all plugins
		for(int i = 0; i < plugins.size(); i++)
		{
			((EditPlugin)plugins.elementAt(i)).stop();
		}

		// Save the recent file list
		for(int i = 0; i < recent.size(); i++)
		{
			String file = (String)recent.elementAt(i);
			setProperty("recent." + i,file);
		}
		unsetProperty("recent." + maxRecent);

		// Save the history lists
		HistoryModel.saveHistory(settingsDirectory + File.separator
			+ "history");

		// Save search and replace state
		SearchAndReplace.save();

		// Write the user properties file
		if(settingsDirectory != null)
		{
			try
			{
				OutputStream out = new FileOutputStream(
					settingsDirectory + File.separator
					+ "properties");
				props.save(out,"Use the -nosettings switch"
					+ " if you want to edit this file in jEdit");
				out.close();
			}
			catch(IOException io)
			{
				io.printStackTrace();
			}
		}

		// Byebye...
		System.out.println("Thank you for using jEdit. Send an e-mail"
			+ " to Slava Pestov <sp@gjt.org>.");
		System.exit(0);
	}

	// package-private members

	// View class needs to fire VIEW_CREATED editor events
	static void fireEditorEvent(int id, View view, Buffer buffer)
	{
		EditorEvent evt = null;
		Object[] listeners = listenerList.getListenerList();
		for(int i = listeners.length - 2; i >= 0; i-= 2)
		{
			if(listeners[i] == EditorListener.class)
			{
				if(evt == null)
					evt = new EditorEvent(id,view,buffer);
				evt.fire((EditorListener)listeners[i+1]);
			}
		}
	}

	// private members
	private static String jEditHome;
	private static String settingsDirectory;
	private static Properties defaultProps;
	private static Properties props;
	private static Autosave autosave;
	private static EditServer server;
	private static Hashtable actionHash;
	private static Vector plugins;
	private static Vector modes;
	private static Vector recent;
	private static int maxRecent;
	private static InputHandler inputHandler;
	private static EventListenerList listenerList;
	private static WindowHandler windowHandler;

	// buffer link list
	private static int bufferCount;
	private static Buffer buffersFirst;
	private static Buffer buffersLast;

	// view link list
	private static int viewCount;
	private static View viewsFirst;
	private static View viewsLast;

	private jEdit() {}

	private static void usage()
	{
		System.err.println("Usage: jedit [<options>] [<files>]");

		System.err.println("Common options:");
		System.err.println("	<filename>#<marker>: Positions caret"
			+ " at marker <marker>");
		System.err.println("	<filename>#+<line>: Positions caret"
			+ " at line number <line>");
		System.err.println("	--: End of options");
		System.err.println("	-version: Print jEdit version and"
			+ " exit");
		System.err.println("	-usage: Print this message and exit");
		System.err.println("	-readonly: Open files read-only");
		System.err.println("	-noserver: Don't start editor server");
		System.err.println("	-server=<path>: Reads/writes server"
			+ " info to file <path>");

		System.err.println();
		System.err.println("Server-only options:");
		System.err.println("	-nosettings: Don't load user-specific"
			+ " settings");
		System.err.println("	-settings=<path>: Load user-specific"
			+ " settings from <path>");
		System.err.println("	-nodesktop: Ignore saved desktop");
		System.err.println("	-nosplash: Don't show splash screen");
		System.err.println();
		System.err.println("Client-only options:");
		System.err.println("	-reuseview: Don't open new view in"
			+ " server jEdit");

		System.err.println();
		System.err.println("Report bugs to Slava Pestov <sp@gjt.org>.");
	}

	private static void version()
	{
		System.err.println("jEdit " + getVersion());
	}

	/**
	 * Initialise various objects, register protocol handlers,
	 * register editor listener, and determine installation
	 * directory.
	 */
	private static void initMisc()
	{
		inputHandler = new DefaultInputHandler();
		windowHandler = new WindowHandler();

		// Add our protocols to java.net.URL's list
		System.getProperties().put("java.protocol.handler.pkgs",
			"org.gjt.sp.jedit.proto|" +
			System.getProperty("java.protocol.handler.pkgs",""));

		// Determine installation directory
		jEditHome = System.getProperty("jedit.home");
		if(jEditHome == null)
		{
			String classpath = System
				.getProperty("java.class.path");
			int index = classpath.toLowerCase()
				.indexOf("jedit.jar");
			int start = classpath.lastIndexOf(File
				.pathSeparator,index) + 1;
			if(index > start)
			{
				jEditHome = classpath.substring(start,
					index - 1);
			}
			else
				jEditHome = System.getProperty("user.dir");
		}
		jEditHome = jEditHome + File.separator;

		if(settingsDirectory != null)
		{
			File _settingsDirectory = new File(settingsDirectory);
			if(!_settingsDirectory.exists())
				_settingsDirectory.mkdir();
			File _macrosDirectory = new File(settingsDirectory,"macros");
			if(!_macrosDirectory.exists())
				_macrosDirectory.mkdir();
		}
	}

	/**
	 * Load system properties.
	 */
	private static void initSystemProperties()
	{
		defaultProps = props = new Properties();

		try
		{
			loadProps(jEdit.class.getResourceAsStream(
				"/org/gjt/sp/jedit/jedit.props"));
			loadProps(jEdit.class.getResourceAsStream(
				"/org/gjt/sp/jedit/jedit_gui.props"));
			loadProps(jEdit.class.getResourceAsStream(
				"/org/gjt/sp/jedit/jedit_keys.props"));
		}
		catch(Exception e)
		{
			System.err.println(">> ERROR LOADING SYSTEM PROPERTIES <<\n"
				+ "One of the following property files could not be loaded:\n"
				+ "- jedit.props\n"
				+ "- jedit_gui.props\n"
				+ "- jedit_keys.props\n"
				+ "Try reinstalling jEdit.");
			System.exit(1);
		}
	}

	/**
	 * Load edit modes.
	 */
	private static void initModes()
	{
		/* Try to guess the eventual size to avoid unnecessary
		 * copying */
		modes = new Vector(17 /* modes built into jEdit */
			+ 10 /* give plugins some space */);

		addMode(new org.gjt.sp.jedit.mode.text());
		addMode(new org.gjt.sp.jedit.mode.bat());
		addMode(new org.gjt.sp.jedit.mode.c());
		addMode(new org.gjt.sp.jedit.mode.cc());
		addMode(new org.gjt.sp.jedit.mode.eiffel());
		addMode(new org.gjt.sp.jedit.mode.html());
		addMode(new org.gjt.sp.jedit.mode.idl());
		addMode(new org.gjt.sp.jedit.mode.java_mode());
		addMode(new org.gjt.sp.jedit.mode.javascript());
		addMode(new org.gjt.sp.jedit.mode.makefile());
		addMode(new org.gjt.sp.jedit.mode.patch());
		addMode(new org.gjt.sp.jedit.mode.perl());
		addMode(new org.gjt.sp.jedit.mode.props());
		addMode(new org.gjt.sp.jedit.mode.python());
		addMode(new org.gjt.sp.jedit.mode.sh());
		addMode(new org.gjt.sp.jedit.mode.tex());
		addMode(new org.gjt.sp.jedit.mode.tsql());
		addMode(new org.gjt.sp.jedit.mode.xml());
	}

	/**
	 * Load actions.
	 */
	private static void initActions()
	{
		actionHash = new Hashtable();

		addAction(new org.gjt.sp.jedit.actions.about());
		addAction(new org.gjt.sp.jedit.actions.append_string_register());
		addAction(new org.gjt.sp.jedit.actions.block_comment());
		addAction(new org.gjt.sp.jedit.actions.box_comment());
		addAction(new org.gjt.sp.jedit.actions.buffer_options());
		addAction(new org.gjt.sp.jedit.actions.clear_marker());
		addAction(new org.gjt.sp.jedit.actions.close_all());
		addAction(new org.gjt.sp.jedit.actions.close_file());
		addAction(new org.gjt.sp.jedit.actions.close_view());
		addAction(new org.gjt.sp.jedit.actions.copy());
		addAction(new org.gjt.sp.jedit.actions.copy_string_register());
		addAction(new org.gjt.sp.jedit.actions.cut());
		addAction(new org.gjt.sp.jedit.actions.cut_string_register());
		addAction(new org.gjt.sp.jedit.actions.delete_end_line());
		addAction(new org.gjt.sp.jedit.actions.delete_line());
		addAction(new org.gjt.sp.jedit.actions.delete_paragraph());
		addAction(new org.gjt.sp.jedit.actions.delete_start_line());
		addAction(new org.gjt.sp.jedit.actions.edit_macro());
		addAction(new org.gjt.sp.jedit.actions.exchange_caret_register());
		addAction(new org.gjt.sp.jedit.actions.exit());
		addAction(new org.gjt.sp.jedit.actions.expand_abbrev());
		addAction(new org.gjt.sp.jedit.actions.find());
		addAction(new org.gjt.sp.jedit.actions.find_next());
		addAction(new org.gjt.sp.jedit.actions.find_selection());
		addAction(new org.gjt.sp.jedit.actions.global_options());
		addAction(new org.gjt.sp.jedit.actions.goto_end_indent());
		addAction(new org.gjt.sp.jedit.actions.goto_line());
		addAction(new org.gjt.sp.jedit.actions.goto_marker());
		addAction(new org.gjt.sp.jedit.actions.goto_register());
		addAction(new org.gjt.sp.jedit.actions.help());
		addAction(new org.gjt.sp.jedit.actions.hypersearch());
		addAction(new org.gjt.sp.jedit.actions.hypersearch_selection());
		addAction(new org.gjt.sp.jedit.actions.indent_on_enter());
		addAction(new org.gjt.sp.jedit.actions.indent_on_tab());
		addAction(new org.gjt.sp.jedit.actions.join_lines());
		addAction(new org.gjt.sp.jedit.actions.locate_bracket());
		addAction(new org.gjt.sp.jedit.actions.multifile_search());
		addAction(new org.gjt.sp.jedit.actions.new_file());
		addAction(new org.gjt.sp.jedit.actions.new_view());
		addAction(new org.gjt.sp.jedit.actions.next_bracket_exp());
		addAction(new org.gjt.sp.jedit.actions.next_buffer());
		addAction(new org.gjt.sp.jedit.actions.next_paragraph());
		addAction(new org.gjt.sp.jedit.actions.open_file());
		addAction(new org.gjt.sp.jedit.actions.open_path());
		addAction(new org.gjt.sp.jedit.actions.open_selection());
		addAction(new org.gjt.sp.jedit.actions.open_url());
		addAction(new org.gjt.sp.jedit.actions.paste());
		addAction(new org.gjt.sp.jedit.actions.paste_previous());
		addAction(new org.gjt.sp.jedit.actions.paste_string_register());
		addAction(new org.gjt.sp.jedit.actions.play_last_macro());
		addAction(new org.gjt.sp.jedit.actions.play_temp_macro());
		addAction(new org.gjt.sp.jedit.actions.play_macro());
		addAction(new org.gjt.sp.jedit.actions.plugin_options());
		addAction(new org.gjt.sp.jedit.actions.prev_bracket_exp());
		addAction(new org.gjt.sp.jedit.actions.prev_buffer());
		addAction(new org.gjt.sp.jedit.actions.prev_paragraph());
		addAction(new org.gjt.sp.jedit.actions.print());
		addAction(new org.gjt.sp.jedit.actions.record_macro());
		addAction(new org.gjt.sp.jedit.actions.record_temp_macro());
		addAction(new org.gjt.sp.jedit.actions.redo());
		addAction(new org.gjt.sp.jedit.actions.reload());
		addAction(new org.gjt.sp.jedit.actions.replace_all());
		addAction(new org.gjt.sp.jedit.actions.replace_in_selection());
		addAction(new org.gjt.sp.jedit.actions.rescan_macros());
		addAction(new org.gjt.sp.jedit.actions.save());
		addAction(new org.gjt.sp.jedit.actions.save_all());
		addAction(new org.gjt.sp.jedit.actions.save_as());
		addAction(new org.gjt.sp.jedit.actions.save_url());
		addAction(new org.gjt.sp.jedit.actions.scroll_line());
		addAction(new org.gjt.sp.jedit.actions.select_all());
		addAction(new org.gjt.sp.jedit.actions.select_block());
		addAction(new org.gjt.sp.jedit.actions.select_buffer());
		addAction(new org.gjt.sp.jedit.actions.select_caret_register());
		addAction(new org.gjt.sp.jedit.actions.select_line_range());
		addAction(new org.gjt.sp.jedit.actions.select_next_paragraph());
		addAction(new org.gjt.sp.jedit.actions.select_none());
		addAction(new org.gjt.sp.jedit.actions.select_prev_paragraph());
		addAction(new org.gjt.sp.jedit.actions.set_caret_register());
		addAction(new org.gjt.sp.jedit.actions.set_filename_register());
		addAction(new org.gjt.sp.jedit.actions.send());
		addAction(new org.gjt.sp.jedit.actions.set_marker());
		addAction(new org.gjt.sp.jedit.actions.shift_left());
		addAction(new org.gjt.sp.jedit.actions.shift_right());
		addAction(new org.gjt.sp.jedit.actions.stop_recording());
		addAction(new org.gjt.sp.jedit.actions.tab());
		addAction(new org.gjt.sp.jedit.actions.undo());
		addAction(new org.gjt.sp.jedit.actions.untab());
		addAction(new org.gjt.sp.jedit.actions.view_registers());
		addAction(new org.gjt.sp.jedit.actions.wing_comment());
		addAction(new org.gjt.sp.jedit.actions.word_count());
	}

	/**
	 * Loads plugins.
	 */
	private static void initPlugins()
	{
		plugins = new Vector();
		loadPlugins(jEditHome + "jars");
		if(settingsDirectory != null)
		{
			File jarsDirectory = new File(settingsDirectory,"jars");
			if(!jarsDirectory.exists())
				jarsDirectory.mkdir();
			loadPlugins(jarsDirectory.getPath());
		}
	}

	/**
	 * Loads user properties.
	 */
	private static void initUserProperties()
	{
		props = new Properties(defaultProps);

		if(settingsDirectory != null)
		{
			try
			{
				loadProps(new FileInputStream(settingsDirectory
					+ File.separator + "properties"));
			}
			catch(FileNotFoundException fnf)
			{
			}
			catch(IOException e)
			{
				System.err.println("Error while loading user"
					+ " properties:");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Sets the Swing look and feel.
	 */
	private static void initPLAF()
	{
		String lf = getProperty("lookAndFeel");
		try
		{
			if(lf != null && lf.length() != 0)
				UIManager.setLookAndFeel(lf);
		}
		catch(Exception e)
		{
			System.err.println("Error loading L&F!");
			e.printStackTrace();
		}
	}

	/**
	 * Loads the recent file list.
	 */
	private static void initRecent()
	{
		recent = new Vector(maxRecent);

		for(int i = 0; i < maxRecent; i++)
		{
			String recentFile = getProperty("recent." + i);
			if(recentFile != null)
				recent.addElement(recentFile);
		}
	}

	/**
	 * Loads the key bindings.
	 */
	private static void initKeyBindings()
	{
		// Register text area key bindings
		ActionListener[] textActions = DefaultInputHandler.ACTIONS;
		for(int i = 0; i < textActions.length; i++)
		{
			String binding = jEdit.getProperty(DefaultInputHandler
				.ACTION_NAMES[i] + ".shortcut");
			if(binding != null)
			{
				inputHandler.addKeyBinding(binding,textActions[i]);
			}
		}
	}

	private static Buffer loadDesktop()
	{
		Buffer b = null;
		try
		{
			int i = 0;
			String path;
			while((path = getProperty("desktop." + i + ".path")) != null)
			{
				String mode = getProperty("desktop." + i + ".mode");
				boolean readOnly = "yes".equals(getProperty(
					"desktop." + i + ".readOnly"));
				boolean current = "yes".equals(getProperty(
					"desktop." + i + ".current"));
				int selStart = Integer.parseInt(getProperty(
					"desktop." + i + ".selStart"));
				int selEnd = Integer.parseInt(getProperty(
					"desktop." + i + ".selEnd"));
				boolean rectSelect = "yes".equals(getProperty(
					"desktop." + i + ".rectSelect"));
				Buffer buffer = openFile(null,null,path,readOnly,
					false);
				buffer.setCaretInfo(selStart,selEnd,rectSelect);
				Mode m = getMode(mode);
				if(m != null)
					buffer.setMode(m);
				if(current)
					b = buffer;
				i++;
			}
		}
		catch(Exception e)
		{
			System.err.println("Error while loading desktop:");
			e.printStackTrace();
		}
		if(b == null && buffersLast != null)
			b = buffersLast;
		return b;
	}

	private static void gotoMarker(Buffer buffer, View view,
		String marker)
	{
		if(marker.length() == 0)
			return;

		int start, end;

		// Handle line number
		if(marker.charAt(0) == '+')
		{
			try
			{
				int line = Integer.parseInt(marker.substring(1));
				Element lineElement = buffer.getDefaultRootElement()
					.getElement(line + 1);
				start = end = lineElement.getStartOffset();
			}
			catch(Exception e)
			{
				return;
			}
		}
		// Handle marker
		else
		{
			Marker m = buffer.getMarker(marker);
			if(m == null)
				return;
			start = m.getStart();
			end = m.getEnd();
		}

		if(view == null || view.getBuffer() != buffer)
			buffer.setCaretInfo(start,end,false);
		else if(view != null)
			view.getTextArea().select(start,end);
	}

	private static void addBufferToList(Buffer buffer)
	{
		bufferCount++;

		if(buffersFirst == null)
			buffersFirst = buffersLast = buffer;
		else
		{
			buffer.prev = buffersLast;
			buffersLast.next = buffer;
			buffersLast = buffer;
		}
	}

	private static void removeBufferFromList(Buffer buffer)
	{
		bufferCount--;

		if(buffer == buffersFirst && buffer == buffersLast)
		{
			buffersFirst = buffersLast = null;
			return;
		}

		if(buffer == buffersFirst)
		{
			buffersFirst = buffer.next;
			buffer.next.prev = null;
		}
		else
		{
			buffer.prev.next = buffer.next;
		}

		if(buffer == buffersLast)
		{
			buffersLast = buffersLast.prev;
			buffer.prev.next = null;
		}
		else
		{
			buffer.next.prev = buffer.prev;
		}
	}

	private static void addViewToList(View view)
	{
		viewCount++;

		if(viewsFirst == null)
			viewsFirst = viewsLast = view;
		else
		{
			view.prev = viewsLast;
			viewsLast.next = view;
			viewsLast = view;
		}
	}

	private static void removeViewFromList(View view)
	{
		viewCount--;

		if(view == viewsFirst)
		{
			viewsFirst = view.next;
			view.next.prev = null;
		}
		else
		{
			view.prev.next = view.next;
		}

		if(view == viewsLast)
		{
			viewsLast = viewsLast.prev;
			view.prev.next = null;
		}
		else
		{
			view.next.prev = view.prev;
		}
	}

	private static boolean _closeBuffer(View view, Buffer buffer)
	{
		if(buffer.isDirty())
		{
			Object[] args = { buffer.getName() };
			int result = JOptionPane.showConfirmDialog(view,
				getProperty("notsaved.message",args),
				getProperty("notsaved.title"),
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.WARNING_MESSAGE);
			if(result == JOptionPane.YES_OPTION)
			{
				if(!buffer.save(view,null))
					return false;
			}
			else if(result == JOptionPane.NO_OPTION)
			{
				buffer.setDirty(false);
			}
			else
				return false;
		}
		if(!buffer.isNewFile())
		{
			String path = buffer.getPath();
			if(recent.contains(path))
				recent.removeElement(path);
			recent.insertElementAt(path,0);
			if(recent.size() > maxRecent)
				recent.removeElementAt(maxRecent);
		}
		return true;
	}

	// Since window closing is handled by the editor itself,
	// and is the same for all views, it is ok to do it here
	static class WindowHandler extends WindowAdapter
	{
		public void windowClosing(WindowEvent evt)
		{
			closeView((View)evt.getSource());
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.141  1999/10/22 07:05:37  sp
 * Version number changed to 2.1final
 *
 * Revision 1.140  1999/10/21 07:59:24  sp
 * Eiffel mode and better font list added
 *
 * Revision 1.139  1999/10/19 09:10:13  sp
 * pre5 bug fixing
 *
 * Revision 1.138  1999/10/17 04:16:28  sp
 * Bug fixing
 *
 * Revision 1.137  1999/10/16 09:43:00  sp
 * Final tweaking and polishing for jEdit 2.1final
 *
 * Revision 1.136  1999/10/10 06:38:45  sp
 * Bug fixes and quicksort routine
 *
 * Revision 1.135  1999/10/07 04:57:13  sp
 * Images updates, globs implemented, file filter bug fix, close all command
 *
 * Revision 1.134  1999/10/06 08:39:46  sp
 * Fixes to repeating and macro features
 *
 * Revision 1.133  1999/10/05 10:55:29  sp
 * File dialogs open faster, and experimental keyboard macros
 *
 * Revision 1.132  1999/10/05 04:43:58  sp
 * Minor bug fixes and updates
 *
 * Revision 1.131  1999/10/04 03:20:51  sp
 * Option pane change, minor tweaks and bug fixes
 *
 * Revision 1.130  1999/10/03 03:47:15  sp
 * Minor stupidity, IDL mode
 *
 * Revision 1.129  1999/10/02 01:12:36  sp
 * Search and replace updates (doesn't work yet), some actions moved to TextTools
 *
 * Revision 1.128  1999/10/01 07:31:39  sp
 * RMI server replaced with socket-based server, minor changes
 *
 * Revision 1.127  1999/09/30 12:21:04  sp
 * No net access for a month... so here's one big jEdit 2.1pre1
 */
