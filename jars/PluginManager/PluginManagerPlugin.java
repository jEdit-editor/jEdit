/*
 * PluginManagerPlugin.java - Plugin manager
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

import org.gjt.sp.jedit.msg.EditorStarted;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Vector;

public class PluginManagerPlugin extends EBPlugin
{
	public void start()
	{
		jEdit.addAction(new OpenAction());
	}

	public void createMenuItems(View view, Vector menus, Vector menuItems)
	{
		menuItems.addElement(GUIUtilities.loadMenuItem(view,"plugin-manager"));
	}

	public void handleMessage(EBMessage msg)
	{
		if(msg instanceof EditorStarted)
		{
			String build = jEdit.getProperty("update-plugins.last-version");
			String myBuild = jEdit.getBuild();
			if(build == null)
			{
				GUIUtilities.hideSplashScreen();
				int result = JOptionPane.showConfirmDialog(null,
					jEdit.getProperty("first-time.message"),
					jEdit.getProperty("first-time.title"),
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE);
				if(result == JOptionPane.YES_OPTION)
					new PluginManager(null);
			}
			else if(myBuild.compareTo(build) > 0)
			{
				GUIUtilities.hideSplashScreen();
				String[] args = {
					MiscUtilities.buildToVersion(build),
					MiscUtilities.buildToVersion(myBuild)
				};
				int result = JOptionPane.showConfirmDialog(null,
					jEdit.getProperty("new-version.message",args),
					jEdit.getProperty("new-version.title"),
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE);
				if(result == JOptionPane.YES_OPTION)
					new PluginManager(null);
			}
			else if(myBuild.compareTo(build) < 0)
				return;

			jEdit.setProperty("update-plugins.last-version",myBuild);
		}
	}

	// returns an array of all installed plugins' path names
	// used by install and update functions
	public static String[] getPlugins()
	{
		Vector installed = new Vector();

		String sysDir = MiscUtilities.constructPath(
			jEdit.getJEditHome(),"jars");
		String[] sysDirList = new File(sysDir).list();
		if(sysDirList != null)
		{
			for(int i = 0; i < sysDirList.length; i++)
			{
				String file = sysDirList[i];
				if(!file.toLowerCase().endsWith(".jar"))
						continue;
				installed.addElement(file);
			}
		}

		String settings = jEdit.getSettingsDirectory();
		if(settings != null)
		{
			String userDir = MiscUtilities.constructPath(
				settings,"jars");
			String[] userDirList = new File(userDir).list();
			if(userDirList != null)
			{
				for(int i = 0; i < userDirList.length; i++)
				{
					String file = userDirList[i];
					if(!file.toLowerCase().endsWith(".jar"))
						continue;
					installed.addElement(file);
				}
			}
		}

		String[] retVal = new String[installed.size()];
		installed.copyInto(retVal);
		return retVal;
	}

	// returns loaded and not loaded plugin lists
	public static String[][] getPluginsEx()
	{
		String[][] retVal = new String[4][];

		Vector loaded = new Vector();
		Vector loadedJARs = new Vector();
		Vector notLoaded = new Vector();
		Vector notLoadedJARs = new Vector();

		EditPlugin.JAR[] jars = jEdit.getPluginJARs();
		for(int i = 0; i < jars.length; i++)
		{
			EditPlugin.JAR jar = jars[i];
			EditPlugin[] plugins = jar.getPlugins();
			if(plugins.length == 0)
			{
				// if there are no plugins in this JAR,
				// list it as loaded
				String name = MiscUtilities.getFileName(jar.getPath());
				loaded.addElement(name);
				loadedJARs.addElement(jar.getPath());
				continue;
			}

			for(int j = 0; j < plugins.length; j++)
			{
				EditPlugin plugin = plugins[j];
				if(plugin instanceof EditPlugin.Broken)
				{
					notLoaded.addElement(plugin.getClassName());
					notLoadedJARs.addElement(jar.getPath());
				}
				else
				{
					loaded.addElement(plugin.getClassName());
					loadedJARs.addElement(jar.getPath());
				}
			}
		}

		retVal[0] = new String[loaded.size()];
		loaded.copyInto(retVal[0]);
		retVal[1] = new String[loadedJARs.size()];
		loadedJARs.copyInto(retVal[1]);
		retVal[2] = new String[notLoaded.size()];
		notLoaded.copyInto(retVal[2]);
		retVal[3] = new String[notLoadedJARs.size()];
		notLoadedJARs.copyInto(retVal[3]);

		return retVal;
	}

	// returns plugins that we attemped to load at startup
	public static String[] getLoadedPlugins()
	{
		Vector retVal = new Vector();

		EditPlugin.JAR[] jars = jEdit.getPluginJARs();
		for(int i = 0; i < jars.length; i++)
		{
			retVal.addElement(MiscUtilities.getFileName(
				jars[i].getPath()));
		}

		String[] array = new String[retVal.size()];
		retVal.copyInto(array);
		return array;
	}

	public static boolean removePlugins(PluginManager dialog, String[] plugins)
	{
		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < plugins.length; i++)
		{
			buf.append(plugins[i]);
			buf.append('\n');
		}
		String[] args = { buf.toString() };

		int result = JOptionPane.showConfirmDialog(dialog,
			jEdit.getProperty("remove-plugins.message",args),
			jEdit.getProperty("remove-plugins.title"),
			JOptionPane.YES_NO_OPTION,
			JOptionPane.QUESTION_MESSAGE);
		if(result != JOptionPane.YES_OPTION)
			return false;

		// this becomes true if at least one plugin was
		// sucessfully removed (otherwise we don't bother
		// displaying the 'removal ok' dialog box)

		String jEditHome = jEdit.getJEditHome();
		boolean ok = false;
		for(int i = 0; i < plugins.length; i++)
		{
			String plugin = plugins[i];
			ok |= removePlugin(dialog,plugin);
		}

		if(ok)
			GUIUtilities.message(dialog,"remove-plugins.done",new String[0]);

		return ok;
	}

	public static boolean installPlugins(PluginManager dialog)
	{
		InstallPluginsDialog installPlugins = new InstallPluginsDialog(dialog);
		String[] urls = installPlugins.getPluginURLs();
		String dir = installPlugins.getInstallDirectory();
		if(urls != null && dir != null)
		{
			String[] dirs = new String[urls.length];
			for(int i = 0; i < dirs.length; i++)
			{
				dirs[i] = dir;
			}

			if(installPlugins(dialog,urls,dirs))
			{
				GUIUtilities.message(dialog,"install-plugins.done",
					new String[0]);
				return true;
			}
		}
		return false;
	}

	public static boolean updatePlugins(PluginManager dialog)
	{
		UpdatePluginsDialog updatePlugins = new UpdatePluginsDialog(dialog);
		PluginList.Plugin[] plugins = updatePlugins.getPlugins();
		if(plugins != null)
		{
			String sysPluginDir = MiscUtilities.constructPath(
				jEdit.getJEditHome(),"jars");

			String[] urls = new String[plugins.length];
			String[] dirs = new String[plugins.length];
			for(int i = 0; i < urls.length; i++)
			{
				String url = plugins[i].download;
				String path = plugins[i].path;

				File jarFile = new File(path);
				File srcFile = new File(path.substring(0,
					path.length() - 4));

				backup(jarFile);
				if(srcFile.exists())
					backup(srcFile);

				urls[i] = url;
				dirs[i] = jarFile.getParent();
			}

			if(installPlugins(dialog,urls,dirs))
			{
				GUIUtilities.message(dialog,"update-plugins.done",
					new String[0]);
				return true;
			}
		}

		return false;
	}

	// package-private members
	static String getDownloadDir()
	{
		if(downloadDir == null)
		{
			String settings = jEdit.getSettingsDirectory();
			if(settings == null)
				settings = System.getProperty("user.home");
			downloadDir = new File(MiscUtilities.constructPath(
				settings,"PluginManager.download"));
			downloadDir.mkdirs();
		}

		return downloadDir.getPath();
	}

	// private members
	private static File usrBackupDir;
	private static File sysBackupDir;
	private static File downloadDir;

	private static String getBackupDir(File plugin)
	{
		File pluginDir = new File(plugin.getParent());
		File backupDir = new File(pluginDir.getParent(),"PluginManager.backup");
		backupDir.mkdirs();

		return backupDir.getPath();
	}

	private static boolean removePlugin(PluginManager dialog, String plugin)
	{
		// move JAR first
		File jarFile = new File(plugin);
		File srcFile = new File(plugin.substring(0,plugin.length() - 4));

		boolean ok = true;
		ok &= backup(jarFile);
		if(srcFile.exists())
			ok &= backup(srcFile);

		if(!ok)
		{
			String[] args = { jarFile.getName() };
			GUIUtilities.error(dialog,"remove-plugins.error",args);
		}

		return ok;
	}

	private static boolean installPlugins(PluginManager dialog,
		String[] urls, String[] dirs)
	{
		PluginDownloadProgress progress = new PluginDownloadProgress(
			dialog,urls,dirs);

		return progress.isOK();
	}

	private static boolean backup(File file)
	{
		String name = file.getName();
		String directory = getBackupDir(file);

		int i = 0;
		File backupFile;
		// find non-existent <name>.backup.<i> file
		while((backupFile = new File(directory,name + ".backup." + i)).exists())
		{
			i++;
		}

		Log.log(Log.DEBUG,PluginManagerPlugin.class,"Moving "
			+ file + " to " + backupFile);

		return file.renameTo(backupFile);
	}

	static class OpenAction extends EditAction
	{
		OpenAction()
		{
			super("plugin-manager");
		}

		public void actionPerformed(ActionEvent evt)
		{
			new PluginManager(getView(evt));
		}
	}
}
