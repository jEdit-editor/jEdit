/*
 * jEdit.java - Main class of the jEdit editor
 * Copyright (C) 1998, 1999, 2000 Slava Pestov
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

import com.microstar.xml.*;
import javax.swing.text.Element;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.MessageFormat;
import java.util.*;
import org.gjt.sp.jedit.browser.VFSBrowser;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.search.SearchAndReplace;
import org.gjt.sp.jedit.syntax.XModeHandler;
import org.gjt.sp.util.Log;

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
		return "02.07.02.00";
	}

	/**
	 * The main method of the jEdit application.
	 * <p>
	 * This should never be invoked directly.
	 * @param args The command line arguments
	 */
	public static void main(String[] args)
	{
		// for developers: run 'jedit 0' to get extensive logging
		int level = Log.WARNING;
		if(args.length >= 1)
		{
			String levelStr = args[0];
			if(levelStr.length() == 1 && Character.isDigit(
				levelStr.charAt(0)))
			{
				level = Integer.parseInt(levelStr);
				args[0] = null;
			}
		}

		// Parse command line
		boolean endOpts = false;
		boolean reuseView = false;
		settingsDirectory = MiscUtilities.constructPath(
			System.getProperty("user.home"),".jedit");
		String portFile = "server";
		boolean defaultSession = true;
		session = "default";
		boolean showSplash = true;
		// don't freak because 'false' is the default. This flag
		// is only checked when running in background mode
		boolean showGUI = false;

		for(int i = 0; i < args.length; i++)
		{
			String arg = args[i];
			if(arg == null)
				continue;
			else if(arg.length() == 0)
				args[i] = null;
			else if(arg.startsWith("-") && !endOpts)
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
				else if(arg.startsWith("-background"))
					background = true;
				else if(arg.startsWith("-gui"))
					showGUI = true;
				else if(arg.equals("-nosession"))
					session = null;
				else if(arg.startsWith("-session="))
				{
					session = arg.substring(9);
					defaultSession = false;
				}
				else if(arg.equals("-nosplash"))
					showSplash = false;
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
			portFile = MiscUtilities.constructPath(settingsDirectory,portFile);
		else
			portFile = null;

		if(session != null)
			session = Sessions.createSessionFileName(session);

		// Try connecting to another running jEdit instance
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

				if(!defaultSession)
				{
					if(session != null)
						out.write("session=" + session + "\n");
					else
						out.write("nosession\n");
				}
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
				// ok, this one seems to confuse newbies
				// endlessly, so log it as NOTICE, not
				// ERROR
				Log.log(Log.NOTICE,jEdit.class,"An error occurred"
					+ " while connecting to the jEdit server instance.");
				Log.log(Log.NOTICE,jEdit.class,"This probably means that"
					+ " jEdit crashed and/or exited abnormally");
				Log.log(Log.NOTICE,jEdit.class,"the last time it was run.");
				Log.log(Log.NOTICE,jEdit.class,"If you don't"
					+ " know what this means, don't worry.");
				Log.log(Log.NOTICE,jEdit.class,e);
			}
		}

		// Show the kool splash screen
		if(showSplash)
			GUIUtilities.showSplashScreen();

		// Initialize activity log and settings directory
		Writer stream;
		if(settingsDirectory != null)
		{
			File _settingsDirectory = new File(settingsDirectory);
			if(!_settingsDirectory.exists())
				_settingsDirectory.mkdirs();
			File _macrosDirectory = new File(settingsDirectory,"macros");
			if(!_macrosDirectory.exists())
				_macrosDirectory.mkdir();
			File _sessionsDirectory = new File(settingsDirectory,"sessions");
			if(!_sessionsDirectory.exists())
				_sessionsDirectory.mkdir();

			String logPath = MiscUtilities.constructPath(
				settingsDirectory,"activity.log");

			try
			{
				stream = new BufferedWriter(new FileWriter(logPath));
			}
			catch(Exception e)
			{
				e.printStackTrace();
				stream = null;
			}
		}
		else
		{
			stream = null;
		}

		Log.init(true,level,stream);
		Log.log(Log.NOTICE,jEdit.class,"jEdit version " + getVersion());
		Log.log(Log.MESSAGE,jEdit.class,"Settings directory is "
			+ settingsDirectory);

		// Initialize server
		if(portFile != null)
		{
			server = new EditServer(portFile);
			if(!server.isOK())
				server = null;
		}
		else
		{
			if(background)
			{
				background = false;
				System.err.println("You cannot specify both the"
					+ " -background and -noserver switches");
			}
		}

		// Get things rolling
		initMisc();
		initSystemProperties();
		BeanShell.init();
		GUIUtilities.advanceSplashProgress();
		initPlugins();
		GUIUtilities.advanceSplashProgress();
		initSiteProperties();
		GUIUtilities.advanceSplashProgress();
		initUserProperties();
		if(settingsDirectory != null)
		{
			File history = new File(MiscUtilities.constructPath(
				settingsDirectory,"history"));
			if(history.exists())
				historyModTime = history.lastModified();
			HistoryModel.loadHistory(history);
		}

		Abbrevs.load();

		GUIUtilities.advanceSplashProgress();

		// Buffer sort
		sortBuffers = getBooleanProperty("sortBuffers");
		sortByName = getBooleanProperty("sortByName");

		propertiesChanged();
		initPLAF();
		initRecent();
		SearchAndReplace.load();

		initActions();
		initModes();
		Macros.loadMacros();

		FavoritesVFS.loadFavorites();

		GUIUtilities.advanceSplashProgress();

		// Start plugins
		for(int i = 0; i < jars.size(); i++)
		{
			((EditPlugin.JAR)jars.elementAt(i)).getClassLoader()
				.loadAllPlugins();
		}

		GUIUtilities.advanceSplashProgress();

		// Preload menu and tool bar models
		GUIUtilities.loadMenuBarModel("view.mbar");
		GUIUtilities.loadMenuModel("view.context");
		GUIUtilities.loadMenuModel("gutter.context");
		if(getBooleanProperty("view.showToolbar"))
			GUIUtilities.loadToolBarModel("view.toolbar");

		GUIUtilities.advanceSplashProgress();

		Buffer buffer = openFiles(userDir,args);

		if(bufferCount == 0 && settingsDirectory != null)
		{
			// don't load default session when in background mode
			if(defaultSession)
			{
				if(!background && session != null
					&& getBooleanProperty("saveDesktop"))
					buffer = Sessions.loadSession(session,true);
			}
			else
				buffer = Sessions.loadSession(session,true);
		}

		// Create the view and hide the splash screen.
		final boolean _showGUI = showGUI;
		final Buffer _buffer = buffer;

		GUIUtilities.advanceSplashProgress();

		SwingUtilities.invokeLater(new Runnable() {
			public void run()
			{
				EditBus.send(new EditorStarted(null));

				// If no files to open were specified in
				// background mode, don't create a view.
				if(background && !_showGUI)
				{
					if(bufferCount != 0)
						newView(null,_buffer);
				}
				else
				{
					if(bufferCount == 0)
						newFile(null);
					newView(null,_buffer);
				}

				GUIUtilities.hideSplashScreen();
				Log.log(Log.MESSAGE,jEdit.class,"Startup "
					+ "complete");

				// Start I/O threads
				VFSManager.start();

				// Start edit server
				if(server != null)
					server.start();
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
	 * Returns the value of a boolean property.
	 * @param name The property
	 */
	public static final boolean getBooleanProperty(String name)
	{
		return getBooleanProperty(name,false);
	}

	/**
	 * Returns the value of a boolean property.
	 * @param name The property
	 * @param def The default value
	 */
	public static final boolean getBooleanProperty(String name, boolean def)
	{
		String value = getProperty(name);
		if(value == null)
			return def;
		else if(value.equals("true") || value.equals("yes")
			|| value.equals("on"))
			return true;
		else if(value.equals("false") || value.equals("no")
			|| value.equals("off"))
			return false;
		else
			return def;
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
		if(value == null)
		{
			String prop = (String)defaultProps.get(name);
			if(prop == null || prop.length() == 0)
				props.remove(name);
			else
				props.put(name,"");
		}
		else
		{
			String prop = (String)defaultProps.get(name);
			if(value.equals(prop))
				props.remove(name);
			else
				props.put(name,value);
		}
	}

	/**
	 * Sets a property to a new value. Properties set using this
	 * method are not saved to the user properties list.
	 * @param name The property
	 * @param value The new value
	 * @since jEdit 2.3final
	 */
	public static final void setTemporaryProperty(String name, String value)
	{
		props.remove(name);
		defaultProps.put(name,value);
	}

	/**
	 * @deprecated As of jEdit 2.3final. Use setTemporaryProperty()
	 * instead.
	 */
	public static final void setDefaultProperty(String name, String value)
	{
		setTemporaryProperty(name,value);
	}

	/**
	 * Sets a boolean property.
	 * @param name The property
	 * @param value The value
	 */
	public static final void setBooleanProperty(String name, boolean value)
	{
		setProperty(name,value ? "true" : "false");
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
	 * Resets a property to its default value.
	 * @param name The property
	 *
	 * @since jEdit 2.5pre3
	 */
	public static final void resetProperty(String name)
	{
		props.remove(name);
	}

	/**
	 * Reloads various settings from the properties.
	 */
	public static void propertiesChanged()
	{
		int interval;
		try
		{
			interval = Integer.parseInt(getProperty("autosave"));
		}
		catch(NumberFormatException nf)
		{
			Log.log(Log.ERROR,jEdit.class,nf);
			interval = 30;
		}
		Autosave.setInterval(interval);

		// Recent files
		try
		{
			maxRecent = Integer.parseInt(getProperty("recent"));
		}
		catch(NumberFormatException nf)
		{
			Log.log(Log.ERROR,jEdit.class,nf);
			maxRecent = 8;
		}

		EditBus.send(new PropertiesChanged(null));
	}

	/**
	 * Loads all plugins in a directory.
	 * @param directory The directory
	 */
	public static void loadPlugins(String directory)
	{
		Log.log(Log.NOTICE,jEdit.class,"Loading plugins from "
			+ directory);

		File file = new File(directory);
		if(!(file.exists() && file.isDirectory()))
			return;
		String[] plugins = file.list();
		if(plugins == null)
			return;

		MiscUtilities.quicksort(plugins,new MiscUtilities.StringICaseCompare());
		for(int i = 0; i < plugins.length; i++)
		{
			String plugin = plugins[i];
			if(!plugin.toLowerCase().endsWith(".jar"))
				continue;
			try
			{
				String path = MiscUtilities.constructPath(
					directory,plugin);
				Log.log(Log.DEBUG,jEdit.class,
					"Scanning JAR file: " + path);
				new JARClassLoader(path);
			}
			catch(IOException io)
			{
				Log.log(Log.ERROR,jEdit.class,"Cannot load"
					+ " plugin " + plugin);
				Log.log(Log.ERROR,jEdit.class,io);

				String[] args = { plugin, io.toString() };
				GUIUtilities.error(null,"plugin.load-error",args);
			}
		}
	}

	/**
	 * Adds a plugin to the editor.
	 * @param plugin The plugin
	 */
	public static void addPlugin(EditPlugin plugin)
	{
		plugins.addPlugin(plugin);
	}

	/**
	 * Adds a plugin to the editor.
	 * @param plugin The plugin
	 */
	public static void addPluginJAR(EditPlugin.JAR plugin)
	{
		plugin.index = jars.size();
		jars.addElement(plugin);
	}

	/**
	 * Returns the plugin with the specified class name.
	 */
	public static EditPlugin getPlugin(String name)
	{
		EditPlugin[] plugins = getPlugins();
		for(int i = 0; i < plugins.length; i++)
		{
			if(plugins[i].getClassName().equals(name))
				return plugins[i];
		}

		return null;
	}

	/**
	 * Returns an array of installed plugins.
	 */
	public static EditPlugin[] getPlugins()
	{
		Vector vector = new Vector();
		for(int i = 0; i < jars.size(); i++)
		{
			((EditPlugin.JAR)jars.elementAt(i)).getPlugins(vector);
		}
		plugins.getPlugins(vector);

		EditPlugin[] array = new EditPlugin[vector.size()];
		vector.copyInto(array);
		return array;
	}

	/**
	 * Returns an array of installed plugins.
	 * @since jEdit 2.5pre3
	 */
	public static EditPlugin.JAR[] getPluginJARs()
	{
		EditPlugin.JAR[] array = new EditPlugin.JAR[jars.size()];
		jars.copyInto(array);
		return array;
	}

	/**
	 * Returns the JAR with the specified path name.
	 * @param path The path name
	 * @since jEdit 2.6pre1
	 */
	public static EditPlugin.JAR getPluginJAR(String path)
	{
		for(int i = 0; i < jars.size(); i++)
		{
			EditPlugin.JAR jar = (EditPlugin.JAR)jars.elementAt(i);
			if(jar.getPath().equals(path))
				return jar;
		}

		return null;
	}

	/**
	 * Returns the JAR at the specified index.
	 * @since jEdit 2.5pre3
	 */
	public static EditPlugin.JAR getPluginJAR(int index)
	{
		return (EditPlugin.JAR)jars.elementAt(index);
	}

	/**
	 * Registers an action with the editor.
	 * @param action The action
	 */
	public static void addAction(EditAction action)
	{
		String name = action.getName();
		actionHash.put(name,new EditAction.Wrapper(action));

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
		Log.log(Log.DEBUG,jEdit.class,"Adding edit mode " + mode.getName());
		modes.addElement(mode);
	}

	/**
	 * Loads a mode cache file.
	 * @param path The mode cache file
	 * @return true if the cache file was loaded, false if not
	 */
	public static boolean loadModeCache(String path)
	{
		Log.log(Log.MESSAGE,jEdit.class,"Loading mode cache file " + path);

		File file = new File(path);
		if(!file.exists())
			return false;

		BufferedReader in = null;
		try
		{
			in = new BufferedReader(new FileReader(path));
			String line;
			while((line = in.readLine()) != null)
			{
				if(line.startsWith("version "))
				{
					String version = line.substring(8);
					if(!version.equals(getBuild()))
						return false;
				}
				else if(line.startsWith("mode "))
				{
					StringTokenizer st = new StringTokenizer(
						line.substring(5),"\t");
					String name = st.nextToken();
					Mode mode = new Mode(name);
					String[] props = { "label", "filenameGlob",
						"firstlineGlob", "grammar" };
					for(int i = 0; i < props.length; i++)
					{
						String value = st.nextToken();
						if(!value.equals("\0"))
							mode.setProperty(props[i],value);
					}
					mode.init();
					addMode(mode);
				}
			}

			return true;
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,jEdit.class,"Error loading mode cache:");
			Log.log(Log.ERROR,jEdit.class,e);
		}
		finally
		{
			try
			{
				if(in != null)
					in.close();
			}
			catch(IOException io)
			{
			}
		}

		return false;
	}

	/**
	 * Reloads all edit modes.
	 * @param view The view
	 */
	public static void reloadModes(View view)
	{
		view.showWaitCursor();

		String path;
		String settingsDirectory = jEdit.getSettingsDirectory();

		if(settingsDirectory == null)
			path = null;
		else
			path = MiscUtilities.constructPath(settingsDirectory,
				"mode-cache");

		jEdit.createModeCache(path);

		Buffer[] buffers = jEdit.getBuffers();
		for(int i = 0; i < buffers.length; i++)
			buffers[i].setMode();

		View[] views = jEdit.getViews();
		for(int i = 0; i < views.length; i++)
		{
			EditPane[] editPanes = views[i].getEditPanes();
			for(int j = 0; j < editPanes.length; j++)
				editPanes[j].getTextArea().repaint();
		}

		view.hideWaitCursor();
	}

	/**
	 * Recreates the mode cache file.
	 * @param path The mode cache file path
	 */
	public static void createModeCache(String path)
	{
		// remove existing modes
		modes = new Vector();

		// 'manually' create text mode
		Mode text = new Mode("text");
		text.setProperty("label",getProperty("mode.text.label"));
		text.setProperty("filenameGlob",getProperty("mode.text.filenameGlob"));
		text.init();
		addMode(text);

		loadModes(MiscUtilities.constructPath(getJEditHome(),"modes"));

		if(settingsDirectory != null)
		{
			String directory = MiscUtilities.constructPath(
				settingsDirectory,"modes");
			new File(directory).mkdirs();
			loadModes(directory);
		}

		/* Sort mode list */
		MiscUtilities.quicksort(modes,new ModeCompare());

		if(path == null)
			return;

		Log.log(Log.MESSAGE,jEdit.class,"Creating mode cache file: " + path);

		BufferedWriter out = null;
		try
		{
			out = new BufferedWriter(new FileWriter(path));
			out.write("version " + getBuild() + '\n');
			for(int i = 0; i < modes.size(); i++)
			{
				Mode mode = (Mode)modes.elementAt(i);
				out.write("mode ");
				out.write(mode.getName());
				String[] props = { "label", "filenameGlob",
					"firstlineGlob", "grammar" };
				for(int j = 0; j < props.length; j++)
				{
					out.write('\t');
					Object prop = mode.getProperty(props[j]);
					if(prop != null && prop.toString().length() > 0)
						out.write(prop.toString());
					else
						out.write('\0');
				}
				out.write('\n');
			}
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,jEdit.class,"Error saving mode cache:");
			Log.log(Log.ERROR,jEdit.class,e);
		}
		finally
		{
			try
			{
				if(out != null)
					out.close();
			}
			catch(IOException io)
			{
			}
		}
	}

	static class ModeCompare implements MiscUtilities.Compare
	{
		public int compare(Object obj1, Object obj2)
		{
			return ((Mode)obj1).getName().compareTo(
				((Mode)obj2).getName());
		}
	}

	/**
	 * Loads XML edit modes from the specified directory.
	 * @param directory The directory
	 */
	public static void loadModes(String directory)
	{
		Log.log(Log.NOTICE,jEdit.class,"Loading edit modes from " + directory);

		File file = new File(directory);
		if(!(file.exists() && file.isDirectory()))
			return;

		String[] grammars = file.list();
		if(grammars == null)
			return;

		for(int i = 0; i < grammars.length; i++)
		{
			String grammar = grammars[i];
			if(!grammar.toLowerCase().endsWith(".xml"))
				continue;

			loadMode(directory + File.separator + grammar);
		}
	}

	/**
	 * Loads an XML-defined edit mode from the specified reader.
	 * @param fileName The file name
	 */
	public static void loadMode(String fileName)
	{
		Log.log(Log.NOTICE,jEdit.class,"Loading edit mode " + fileName);

		XModeHandler xmh = new XModeHandler(fileName);
		XmlParser parser = new XmlParser();
		parser.setHandler(xmh);
		try
		{
			Reader grammar = new BufferedReader(new FileReader(fileName));
			parser.parse(null, null, grammar);
		}
		catch (Exception e)
		{
			Log.log(Log.ERROR, jEdit.class, e);

			if (e instanceof XmlException)
			{
				XmlException xe = (XmlException) e;
				int line = xe.getLine();
				String message = xe.getMessage();

				Object[] args = { fileName, new Integer(line), message };
				GUIUtilities.error(null,"xmode-parse",args);
			}
		}
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
	 * Displays the 'about jEdit' dialog box.
	 * @since jEdit 2.7pre2
	 */
	public static void showAboutDialog(View view)
	{
		new AboutDialog(view);
	}

	/**
	 * Displays the global options dialog box.
	 * @since jEdit 2.7pre2
	 */
	public static void showGlobalOptionsDialog(View view)
	{
		new OptionsDialog(view);
	}

	/**
	 * Displays the I/O progress monitor dialog box.
	 * @since jEdit 2.7pre2
	 */
	public static void showIOProgressMonitorDialog()
	{
		new IOProgressMonitor();
	}

	/**
	 * Displays the activity log window.
	 * @since jEdit 2.7pre2
	 */
	public static void showActivityLog()
	{
		new LogViewer();
	}

	/**
	 * Displays the open file dialog box, and opens any selected files.
	 * @param view The view
	 * @since jEdit 2.7pre2
	 */
	public static void showOpenFileDialog(View view)
	{
		String[] files = GUIUtilities.showVFSFileDialog(view,null,
			VFSBrowser.OPEN_DIALOG,true);

		Buffer buffer = null;
		if(files != null)
		{
			for(int i = 0; i < files.length; i++)
			{
				Buffer newBuffer = openFile(null,files[i]);
				if(newBuffer != null)
					buffer = newBuffer;
			}
		}

		if(buffer != null)
			view.setBuffer(buffer);
	}

	/**
	 * Opens the file names specified in the argument array. This
	 * handles +line and +marker arguments just like the command
	 * line parser.
	 * @param parent The parent directory
	 * @param args The file names to open
	 * @since jEdit 2.6pre4
	 */
	public static Buffer openFiles(String parent, String[] args)
	{
		Buffer retVal = null;
		Buffer lastBuffer = null;

		for(int i = 0; i < args.length; i++)
		{
			String arg = args[i];
			if(arg == null)
				continue;
			else if(arg.startsWith("+line:") || arg.startsWith("+marker:"))
			{
				if(lastBuffer != null)
					gotoMarker(lastBuffer,arg);
				continue;
			}

			lastBuffer = openFile(null,parent,arg,false,false);

			if(retVal == null && lastBuffer != null)
				retVal = lastBuffer;
		}

		return retVal;
	}

	/**
	 * Opens a file. Note that as of jEdit 2.5pre1, this may return
	 * null if the buffer could not be opened.
	 * @param view The view to open the file in
	 * @param path The file path
	 *
	 * @since jEdit 2.4pre1
	 */
	public static Buffer openFile(View view, String path)
	{
		return openFile(view,null,path,false,false,null);
	}

	/**
	 * Opens a file. Note that as of jEdit 2.5pre1, this may return
	 * null if the buffer could not be opened.
	 * @param view The view to open the file in
	 * @param parent The parent directory of the file
	 * @param path The path name of the file
	 * @param readOnly True if the file should be read only
	 * @param newFile True if the file should not be loaded from disk
	 * be prompted if it should be reloaded
	 */
	public static Buffer openFile(View view, String parent,
		String path, boolean readOnly, boolean newFile)
	{
		return openFile(view,parent,path,readOnly,newFile,null);
	}

	/**
	 * Opens a file. Note that as of jEdit 2.5pre1, this may return
	 * null if the buffer could not be opened.
	 * @param view The view to open the file in
	 * @param parent The parent directory of the file
	 * @param path The path name of the file
	 * @param readOnly True if the file should be read only
	 * @param newFile True if the file should not be loaded from disk
	 * be prompted if it should be reloaded
	 * @param props Buffer-local properties to set in the buffer
	 *
	 * @since JEdit 2.5pre1
	 */
	public static Buffer openFile(final View view, String parent,
		String path, boolean readOnly, boolean newFile,
		Hashtable props)
	{
		if(view != null && parent == null)
		{
			File file = view.getBuffer().getFile();
			if(file != null)
				parent = file.getParent();
		}

		String protocol;
		if(MiscUtilities.isURL(path))
		{
			protocol = MiscUtilities.getProtocolOfURL(path);
			if(protocol.equals("file"))
				path = path.substring(5);
		}
		else
			protocol = "file";
			
		if(protocol.equals("file"))
			path = MiscUtilities.constructPath(parent,path);

		Buffer buffer = getBuffer(path);
		if(buffer != null)
		{
			if(view != null)
				view.setBuffer(buffer);
			return buffer;
		}

		final Buffer newBuffer = new Buffer(view,path,readOnly,
			newFile,false,props);

		if(!newBuffer.load(view,false))
			return null;

		addBufferToList(newBuffer);

		EditBus.send(new BufferUpdate(newBuffer,BufferUpdate.CREATED));

		if(view != null)
			view.setBuffer(newBuffer);

		return newBuffer;
	}

	/**
	 * Opens a temporary buffer. A temporary buffer is like a normal
	 * buffer, except that an event is not fired, the the buffer is
	 * not added to the buffers list.
	 *
	 * @param view The view to open the file in
	 * @param parent The parent directory of the file
	 * @param path The path name of the file
	 * @param readOnly True if the file should be read only
	 * @param newFile True if the file should not be loaded from disk
	 */
	public static Buffer openTemporary(View view, String parent,
		String path, boolean readOnly, boolean newFile)
	{
		if(view != null && parent == null)
		{
			File file = view.getBuffer().getFile();
			if(file != null)
				parent = file.getParent();
		}

		String protocol;
		if(MiscUtilities.isURL(path))
		{
			protocol = MiscUtilities.getProtocolOfURL(path);
			if(protocol.equals("file"))
				path = path.substring(5);
		}
		else
			protocol = "file";
			
		if(protocol.equals("file"))
			path = MiscUtilities.constructPath(parent,path);

		Buffer buffer = getBuffer(path);
		if(buffer != null)
			return buffer;

		buffer = new Buffer(null,path,readOnly,newFile,true,null);
		if(!buffer.load(view,false))
			return null;
		else
			return buffer;
	}

	/**
	 * Adds a temporary buffer to the buffer list. This must be done
	 * before allowing the user to interact with the buffer in any
	 * way.
	 * @param buffer The buffer
	 */
	public static void commitTemporary(Buffer buffer)
	{
		if(!buffer.isTemporary())
			return;

		buffer.setMode();
		buffer.propertiesChanged();

		addBufferToList(buffer);
		buffer.commitTemporary();

		EditBus.send(new BufferUpdate(buffer,BufferUpdate.CREATED));
	}

	/**
	 * Creates a new `untitled' file.
	 * @param view The view to create the file in
	 */
	public static Buffer newFile(View view)
	{
		// If only one new file is open which is clean, just close
		// it, which will create an 'Untitled-1'
		if(buffersFirst != null && buffersFirst == buffersLast
			&& buffersFirst.isUntitled()
			&& !buffersFirst.isDirty())
		{
			closeBuffer(view,buffersFirst);
			// return the newly created 'untitled-1'
			return buffersFirst;
		}

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

		return openFile(view,null,"Untitled-" + (untitledCount+1),
			false,true);
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
		// Wait for pending I/O requests
		if(buffer.isPerformingIO())
		{
			VFSManager.waitForRequests();
			if(VFSManager.errorOccurred())
				return false;
		}

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
				if(!buffer.save(view,null,true))
					return false;
			}
			else if(result != JOptionPane.NO_OPTION)
				return false;
		}

		_closeBuffer(view,buffer);

		return true;
	}

	/**
	 * Closes the buffer, even if it has unsaved changes.
	 * @param view The view
	 * @param buffer The buffer
	 *
	 * @since jEdit 2.2pre1
	 */
	public static void _closeBuffer(View view, Buffer buffer)
	{
		removeBufferFromList(buffer);
		buffer.close();

		if(!buffer.isNewFile())
			addRecent(buffer.getPath());

		EditBus.send(new BufferUpdate(buffer,BufferUpdate.CLOSED));

		// Create a new file when the last is closed
		if(buffersFirst == null && buffersLast == null)
			newFile(view);
	}

	/**
	 * Closes all open buffers.
	 * @param view The view
	 */
	public static boolean closeAllBuffers(View view)
	{
		return closeAllBuffers(view,false);
	}

	/**
	 * Closes all open buffers.
	 * @param view The view
	 * @param isExiting This must be false unless this method is
	 * being called by the exit() method
	 */
	public static boolean closeAllBuffers(View view, boolean isExiting)
	{
		boolean dirty = false;

		Buffer buffer = buffersFirst;
		while(buffer != null)
		{
			if(buffer.isDirty())
			{
				dirty = true;
				break;
			}
			buffer = buffer.next;
		}

		if(dirty)
		{
			boolean ok = new CloseDialog(view).isOK();
			if(!ok)
				return false;
		}

		// Wait for pending I/O requests
		VFSManager.waitForRequests();
		if(VFSManager.errorOccurred())
			return false;

		// close remaining buffers (the close dialog only deals with
		// dirty ones)

		buffer = buffersFirst;

		// zero it here so that BufferTabs doesn't have any problems
		buffersFirst = buffersLast = null;
		bufferCount = 0;

		while(buffer != null)
		{
			if(!buffer.isNewFile())
				addRecent(buffer.getPath());
			buffer.close();
			if(!isExiting)
				EditBus.send(new BufferUpdate(buffer,BufferUpdate.CLOSED));
			buffer = buffer.next;
		}

		if(!isExiting)
			newFile(view);

		return true;
	}

	/**
	 * Saves all open buffers.
	 * @param view The view
	 * @param confirm If true, a confirmation dialog will be shown first
	 * @since jEdit 2.7pre2
	 */
	public static void saveAllBuffers(View view, boolean confirm)
	{
		if(confirm)
		{
			int result = JOptionPane.showConfirmDialog(view,
				jEdit.getProperty("saveall.message"),
				jEdit.getProperty("saveall.title"),
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE);
			if(result != JOptionPane.YES_OPTION)
				return;
		}

		Buffer buffer = buffersFirst;
		while(buffer != null)
		{
			if(buffer.isDirty())
				buffer.save(view,null,true);
			buffer = buffer.next;
		}
	}

	/**
	 * Reloads all open buffers.
	 * @param view The view
	 * @param confirm If true, a confirmation dialog will be shown first
	 * @since jEdit 2.7pre2
	 */
	public static void reloadAllBuffers(View view, boolean confirm)
	{
		if(confirm)
		{
			int result = JOptionPane.showConfirmDialog(view,
				jEdit.getProperty("reload-all.message"),
				jEdit.getProperty("reload-all.title"),
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE);
			if(result != JOptionPane.YES_OPTION)
				return;
		}

		Buffer[] buffers = jEdit.getBuffers();
		for(int i = 0; i < buffers.length; i++)
		{
			Buffer buffer = buffers[i];
			buffer.load(view,true);
		}
	}

	/**
	 * Returns the buffer with the specified path name. The path name
	 * must be an absolute, canonical, path.
	 * @param path The path name
	 * @see MiscUtilities#constructPath(String,String)
	 */
	public static Buffer getBuffer(String path)
	{
		boolean caseInsensitiveFilesystem = (File.separatorChar == '\\'
			|| File.separatorChar == ':' /* Windows or MacOS */);

		Buffer buffer = buffersFirst;
		while(buffer != null)
		{
			String _path = buffer.getPath();
			if(caseInsensitiveFilesystem)
			{
				if(_path.equalsIgnoreCase(path))
					return buffer;
			}
			else
			{
				if(_path.equals(path))
					return buffer;
			}
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
	 * Returns the number of open buffers.
	 */
	public static int getBufferCount()
	{
		return bufferCount;
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
	 * @see org.gjt.sp.jedit.gui.InputHandler
	 */
	public static InputHandler getInputHandler()
	{
		return inputHandler;
	}

	/**
	 * Reloads all key bindings from the properties.
	 * @since 2.3pre1
	 */
	public static void reloadKeyBindings()
	{
		inputHandler.removeAllKeyBindings();

		EditAction[] actions = getActions();
		for(int i = 0; i < actions.length; i++)
		{
			EditAction action = actions[i];
			String shortcut = jEdit.getProperty(action.getName()
				+ ".shortcut");
			if(shortcut != null)
				inputHandler.addKeyBinding(shortcut,action);
		}
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
			view.showWaitCursor();
			view.getEditPane().saveCaretInfo();
		}

		View newView = new View(buffer);

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

			view.hideWaitCursor();
		}
		else
		{
			GUIUtilities.loadGeometry(newView,"view");
		}

		addViewToList(newView);
		EditBus.send(new ViewUpdate(newView,ViewUpdate.CREATED));

		newView.show();

		return newView;
	}

	/**
	 * Closes a view. jEdit will exit if this was the last open view.
	 */
	public static void closeView(View view)
	{
		closeView(view,true);
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
	 * Returns the number of open views.
	 */
	public static int getViewCount()
	{
		return viewCount;
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
	 * Saves all user preferences to disk.
	 */
	public static void saveSettings()
	{
		if(settingsDirectory != null)
		{
			// Save the recent file list
			for(int i = 0; i < recent.size(); i++)
			{
				String file = (String)recent.elementAt(i);
				setProperty("recent." + i,file);
			}
			unsetProperty("recent." + maxRecent);

			File file = new File(MiscUtilities.constructPath(
				settingsDirectory, "history"));
			if(file.lastModified() != historyModTime)
			{
				Log.log(Log.WARNING,jEdit.class,file + " changed"
					+ " on disk; will not save history");
			}
			else
			{
				HistoryModel.saveHistory(file);
			}
			historyModTime = file.lastModified();

			SearchAndReplace.save();
			Abbrevs.save();
			FavoritesVFS.saveFavorites();

			file = new File(MiscUtilities.constructPath(
				settingsDirectory,"properties"));
			if(file.lastModified() != propsModTime)
			{
				Log.log(Log.WARNING,jEdit.class,file + " changed"
					+ " on disk; will not save user properties");
			}
			else
			{
				try
				{
					OutputStream out = new FileOutputStream(file);
					props.save(out,"jEdit properties");
					out.close();
				}
				catch(IOException io)
				{
					Log.log(Log.ERROR,jEdit.class,io);
				}

				propsModTime = file.lastModified();
			}
		}
	}

	/**
	 * Exits cleanly from jEdit, prompting the user if any unsaved files
	 * should be saved first.
	 * @param view The view from which this exit was called
	 * @param reallyExit If background mode is enabled and this parameter
	 * is true, then jEdit will close all open views instead of exiting
	 * entirely.
	 */
	public static void exit(View view, boolean reallyExit)
	{
		// Wait for pending I/O requests
		VFSManager.waitForRequests();

		// Even if reallyExit is false, we still exit properly
		// if background mode is off
		reallyExit |= !background;

		if(settingsDirectory != null && session != null)
			Sessions.saveSession(view,session);

		// Close all buffers
		if(!closeAllBuffers(view,reallyExit))
			return;

		// If we are running in background mode and
		// reallyExit was not specified, then return here.
		if(!reallyExit)
		{
			// in this case, we can't directly call
			// view.close(); we have to call closeView()
			// for all open views
			view = viewsFirst;
			while(view != null)
			{
				closeView(view,false);
				view = view.next;
			}

			// Save settings in case user kills the backgrounded
			// jEdit process
			saveSettings();

			return;
		}

		// Save view properties here - it unregisters
		// listeners, and we would have problems if the user
		// closed a view but cancelled an unsaved buffer close
		view.close();

		// Stop autosave timer
		Autosave.stop();

		// Stop server
		if(server != null)
			server.stopServer();

		// Stop all plugins
		EditPlugin[] plugins = getPlugins();
		for(int i = 0; i < plugins.length; i++)
		{
			plugins[i].stop();
		}

		// Send EditorExiting
		EditBus.send(new EditorExiting(null));

		// Save settings
		saveSettings();

		// Close activity log stream
		Log.closeStream();

		// Byebye...
		System.exit(0);
	}

	// package-private members

	/**
	 * If buffer sorting is enabled, this repositions the buffer.
	 */
	static void updatePosition(Buffer buffer)
	{
		if(sortBuffers)
		{
			removeBufferFromList(buffer);
			addBufferToList(buffer);
		}
	}

	// private members
	private static String jEditHome;
	private static String settingsDirectory;
	private static long propsModTime, historyModTime;
	private static String session;
	private static Properties defaultProps;
	private static Properties props;
	private static EditServer server;
	private static boolean background;
	private static Hashtable actionHash;
	private static Vector jars;
	private static EditPlugin.JAR plugins; /* plugins without a JAR */
	private static Vector modes;
	private static Vector recent;
	private static int maxRecent;
	private static InputHandler inputHandler;

	// buffer link list
	private static boolean sortBuffers;
	private static boolean sortByName;
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
		System.out.println("Usage: jedit [<options>] [<files>]");

		System.out.println("Common options:");
		System.out.println("	+marker:<marker>: Positions caret"
			+ " at marker <marker>");
		System.out.println("	+line:<line>: Positions caret"
			+ " at line number <line>");
		System.out.println("	--: End of options");
		System.out.println("	-version: Print jEdit version and"
			+ " exit");
		System.out.println("	-usage: Print this message and exit");
		System.out.println("	-nosession: Don't load default session");
		System.out.println("	-session=<name>: Load session from"
			+ " $HOME/.jedit/sessions/<name>");
		System.out.println("	-noserver: Don't start editor server");
		System.out.println("	-server=<name>: Reads/writes server"
			+ " info to $HOME/.jedit/<name>");

		System.out.println();
		System.out.println("Server-only options:");
		System.out.println("	-nosettings: Don't load user-specific"
			+ " settings");
		System.out.println("	-settings=<path>: Load user-specific"
			+ " settings from <path>");
		System.out.println("	-nosplash: Don't show splash screen");
		System.out.println("	-background: Run in background mode");
		System.out.println("	-gui: Create initial view in background mode");
		System.out.println();
		System.out.println("Client-only options:");
		System.out.println("	-reuseview: Don't open new view");

		System.out.println();
		System.out.println("To set minimum activity log level,"
			+ " specify a number as the first");
		System.out.println("command line parameter"
			+ " (1-9, 1 = debug, 9 = error)");
		System.out.println();
		System.out.println("Report bugs to Slava Pestov <sp@gjt.org>.");
	}

	private static void version()
	{
		System.out.println("jEdit " + getVersion());
	}

	/**
	 * Initialise various objects, register protocol handlers.
	 */
	private static void initMisc()
	{
		// Add our protocols to java.net.URL's list
		System.getProperties().put("java.protocol.handler.pkgs",
			"org.gjt.sp.jedit.proto|" +
			System.getProperty("java.protocol.handler.pkgs",""));

		inputHandler = new DefaultInputHandler(null);

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
			Log.log(Log.ERROR,jEdit.class,
				"Error while loading system properties!");
			Log.log(Log.ERROR,jEdit.class,
				"One of the following property files could not be loaded:\n"
				+ "- jedit.props\n"
				+ "- jedit_gui.props\n"
				+ "- jedit_keys.props\n"
				+ "jedit.jar is probably corrupt.");
			Log.log(Log.ERROR,jEdit.class,e);
			System.exit(1);
		}
	}

	/**
	 * Load site properties.
	 */
	private static void initSiteProperties()
	{
		// site properties are loaded as default properties, overwriting
		// jEdit's system properties

		String siteSettingsDirectory = MiscUtilities.constructPath(
			jEditHome, "site-props");
		File siteSettings = new File(siteSettingsDirectory);

		if (!(siteSettings.exists() && siteSettings.isDirectory()))
			return;

		String[] snippets = siteSettings.list();
		if (snippets == null)
			return;

		MiscUtilities.quicksort(snippets,
			new MiscUtilities.StringICaseCompare());

		for (int i = 0; i < snippets.length; ++i)
		{
			String snippet = snippets[i];
			if(!snippet.toLowerCase().endsWith(".props"))
				continue;

			try
			{
				String path = MiscUtilities.constructPath(
					siteSettingsDirectory,snippet);
				Log.log(Log.DEBUG,jEdit.class,
					"Loading site snippet: " + path);

				loadProps(new FileInputStream(new File(path)));
			}
			catch(FileNotFoundException fnf)
			{
				Log.log(Log.DEBUG,jEdit.class,fnf);
			}
			catch(IOException e)
			{
				Log.log(Log.ERROR,jEdit.class,"Cannot load site snippet "
					+ snippet);
				Log.log(Log.ERROR,jEdit.class,e);
			}
		}
	}

	/**
	 * Load edit modes.
	 */
	private static void initModes()
	{
		/* Try to guess the eventual size to avoid unnecessary
		 * copying */
		modes = new Vector(50);

		// try loading cache file
		String path;
		if(settingsDirectory != null)
		{
			path = MiscUtilities.constructPath(
				settingsDirectory,"mode-cache");
			if(loadModeCache(path))
				return;
		}
		else
			path = null;

		createModeCache(path);
	}

	/**
	 * Registers an action with the editor. This is for internal
	 * use only.
	 * @param action The action
	 */
	private static void addAction(String name)
	{
		EditAction.Wrapper action = new EditAction.Wrapper(name);
		actionHash.put(name,action);

		// Register key binding
		String binding = getProperty(name + ".shortcut");
		if(binding != null)
			inputHandler.addKeyBinding(binding,action);
	}

	/**
	 * Load actions.
	 */
	private static void initActions()
	{
		actionHash = new Hashtable();

		long start = System.currentTimeMillis();

		Reader in = new BufferedReader(new InputStreamReader(
			jEdit.class.getResourceAsStream("actions.xml")));
		loadActions("actions.xml",in);

		System.err.println(System.currentTimeMillis() - start);

		addAction("end");
		addAction("home");
		addAction("input");
		addAction("next-line");
		addAction("next-page");
		addAction("prev-line");
		addAction("prev-page");
		addAction("select-block");
		addAction("select-end");
		addAction("select-home");
		addAction("select-next-line");
		addAction("select-next-page");
		addAction("select-prev-line");
		addAction("select-prev-page");

		// Preload these actions so that needsActionCommand()
		// will return the correct value (for command line autocompletion)
		addAction(new org.gjt.sp.jedit.actions.play_macro());
	}

	/**
	 * Loads the specified action list.
	 * @since jEdit 2.7pre2
	 */
	private static void loadActions(String path, Reader in)
	{
		Log.log(Log.DEBUG,jEdit.class,"Loading actions from " + path);

		ActionListHandler ah = new ActionListHandler(path);
		XmlParser parser = new XmlParser();
		parser.setHandler(ah);
		try
		{
			parser.parse(null, null, in);
		}
		catch(XmlException xe)
		{
			int line = xe.getLine();
			String message = xe.getMessage();
			Log.log(Log.ERROR,jEdit.class,path + ":" + line
				+ ": " + message);
			System.exit(1);
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,jEdit.class,e);
			System.exit(1);
		}
	}

	/**
	 * Loads plugins.
	 */
	private static void initPlugins()
	{
		plugins = new EditPlugin.JAR(null,null);
		jars = new Vector();
		loadPlugins(MiscUtilities.constructPath(jEditHome,"jars"));
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
			File file = new File(MiscUtilities.constructPath(
				settingsDirectory,"properties"));
			propsModTime = file.lastModified();

			try
			{
				loadProps(new FileInputStream(file));
			}
			catch(FileNotFoundException fnf)
			{
				Log.log(Log.DEBUG,jEdit.class,fnf);
			}
			catch(IOException e)
			{
				Log.log(Log.ERROR,jEdit.class,e);
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
			Log.log(Log.ERROR,jEdit.class,e);
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

	private static void gotoMarker(Buffer buffer, String marker)
	{
		VFSManager.runInAWTThread(new GotoMarkerSafely(buffer,marker));
	}

	static class GotoMarkerSafely implements Runnable
	{
		Buffer buffer;
		String marker;

		GotoMarkerSafely(Buffer buffer, String marker)
		{
			this.buffer = buffer;
			this.marker = marker;
		}

		public void run()
		{
			int start, end;

			// Handle line number
			if(marker.startsWith("+line:"))
			{
				try
				{
					int line = Integer.parseInt(marker.substring(6));
					Element lineElement = buffer.getDefaultRootElement()
						.getElement(line - 1);
					start = end = lineElement.getStartOffset();
				}
				catch(Exception e)
				{
					return;
				}
			}
			// Handle marker
			else if(marker.startsWith("+marker:"))
			{
				Marker m = buffer.getMarker(marker.substring(8));
				if(m == null)
					return;
				start = m.getStart();
				end = m.getEnd();
			}
			// Can't happen
			else
				throw new InternalError();

			buffer.putProperty(Buffer.SELECTION_START,new Integer(start));
			buffer.putProperty(Buffer.SELECTION_END,new Integer(end));
			buffer.getDocumentProperties().remove(Buffer.SCROLL_HORIZ);
			buffer.getDocumentProperties().remove(Buffer.SCROLL_VERT);
		}
	}

	private static void addBufferToList(Buffer buffer)
	{
		// if only one, clean, 'untitled' buffer is open, we
		// replace it
		if(viewCount <= 1 && buffersFirst != null
			&& buffersFirst == buffersLast
			&& buffersFirst.isUntitled()
			&& !buffersFirst.isDirty())
		{
			Buffer oldBuffersFirst = buffersFirst;
			buffersFirst = buffersLast = buffer;
			EditBus.send(new BufferUpdate(oldBuffersFirst,
				BufferUpdate.CLOSED));
			return;
		}

		bufferCount++;

		if(buffersFirst == null)
		{
			buffersFirst = buffersLast = buffer;
			return;
		}
		else if(sortBuffers)
		{
			String name1 = (sortByName ? buffer.getName()
				: buffer.getPath()).toLowerCase();

			Buffer _buffer = buffersFirst;
			while(_buffer != null)
			{
				String name2 = (sortByName ? _buffer.getName()
					: _buffer.getPath()).toLowerCase();
				if(name1.compareTo(name2) <= 0)
				{
					buffer.next = _buffer;
					buffer.prev = _buffer.prev;
					_buffer.prev = buffer;
					if(_buffer != buffersFirst)
						buffer.prev.next = buffer;
					else
						buffersFirst = buffer;
					return;
				}

				_buffer = _buffer.next;
			}
		}

		buffer.prev = buffersLast;
		buffersLast.next = buffer;
		buffersLast = buffer;
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

		// fixes the hang that can occur if we 'save as' to a new
		// filename which requires re-sorting
		buffer.next = buffer.prev = null;
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

		if(viewsFirst == viewsLast)
		{
			viewsFirst = viewsLast = null;
			return;
		}

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

	private static void addRecent(String path)
	{
		if(recent.contains(path))
			recent.removeElement(path);
		recent.insertElementAt(path,0);
		if(recent.size() > maxRecent)
			recent.removeElementAt(maxRecent);
	}

	/**
	 * closeView() used by exit().
	 */
	private static void closeView(View view, boolean callExit)
	{
		if(viewsFirst == viewsLast && callExit)
			exit(view,false); /* exit does editor event & save */
		else
		{
			EditBus.send(new ViewUpdate(view,ViewUpdate.CLOSED));

			view.close();
			removeViewFromList(view);
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.293  2000/11/16 04:01:11  sp
 * BeanShell macros started
 *
 * Revision 1.292  2000/11/13 11:19:26  sp
 * Search bar reintroduced, more BeanShell stuff
 *
 * Revision 1.291  2000/11/12 05:36:48  sp
 * BeanShell integration started
 *
 * Revision 1.290  2000/11/11 02:59:29  sp
 * FTP support moved out of the core into a plugin
 *
 * Revision 1.289  2000/11/08 09:31:36  sp
 * Junk
 *
 * Revision 1.288  2000/11/07 10:08:31  sp
 * Options dialog improvements, documentation changes, bug fixes
 *
 * Revision 1.287  2000/11/05 05:25:45  sp
 * Word wrap, format and remove-trailing-ws commands from TextTools moved into core
 *
 * Revision 1.286  2000/11/05 00:44:14  sp
 * Improved HyperSearch, improved horizontal scroll, other stuff
 *
 * Revision 1.285  2000/11/02 09:19:31  sp
 * more features
 *
 * Revision 1.284  2000/10/30 07:14:03  sp
 * 2.7pre1 branched, GUI improvements
 *
 * Revision 1.283  2000/10/28 00:36:58  sp
 * ML mode, Haskell mode
 *
 * Revision 1.282  2000/10/13 06:57:20  sp
 * Edit User/System Macros command, gutter mouse handling improved
 *
 * Revision 1.281  2000/10/12 09:28:27  sp
 * debugging and polish
 *
 * Revision 1.280  2000/09/26 10:19:46  sp
 * Bug fixes, spit and polish
 *
 * Revision 1.279  2000/09/23 03:01:09  sp
 * pre7 yayayay
 *
 * Revision 1.278  2000/09/09 04:00:34  sp
 * 2.6pre6
 *
 * Revision 1.277  2000/09/06 04:39:47  sp
 * bug fixes
 *
 * Revision 1.276  2000/09/04 06:34:53  sp
 * bug fixes
 *
 * Revision 1.275  2000/09/03 03:16:53  sp
 * Search bar integrated with command line, enhancements throughout
 *
 * Revision 1.274  2000/09/01 11:31:00  sp
 * Rudimentary 'command line', similar to emacs minibuf
 *
 * Revision 1.273  2000/08/31 02:54:00  sp
 * Improved activity log, bug fixes
 *
 */
