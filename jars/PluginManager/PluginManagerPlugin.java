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
					installPlugins(null);
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
					updatePlugins(null);
			}
			else if(myBuild.compareTo(build) < 0)
				return;

			jEdit.setProperty("update-plugins.last-version",myBuild);
		}
	}

	public static String getLastPathComponent(String path)
	{
		int index = path.lastIndexOf(File.separatorChar);
		if(index == -1)
			index = path.lastIndexOf('/');

		return path.substring(index + 1);
	}

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

	public static String[][] getPluginsEx()
	{
		String[][] retVal = new String[3][];

		Vector loadedPlugins = new Vector();
		Vector loaded = new Vector();
		Vector notLoaded = new Vector();
		EditPlugin[] plugins = jEdit.getPlugins();

		for(int i = 0; i < plugins.length; i++)
		{
			EditPlugin plugin = plugins[i];
			String name = plugin.getClass().getName();
			if(!name.endsWith("Plugin"))
				continue;

			JARClassLoader loader = (JARClassLoader)plugin
				.getClass().getClassLoader();
			String path = loader.getPath();
			if(loaded.indexOf(path) == -1)
			{
				loaded.addElement(path);
				loadedPlugins.addElement(name);
			}
		}

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
				file = MiscUtilities.constructPath(sysDir,file);
				if(loaded.indexOf(file) == -1)
					notLoaded.addElement(file);
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
					file = MiscUtilities.constructPath(userDir,file);
					if(loaded.indexOf(file) == -1)
						notLoaded.addElement(file);
				}
			}
		}

		retVal[0] = new String[loaded.size()];
		loaded.copyInto(retVal[0]);
		retVal[1] = new String[loadedPlugins.size()];
		loadedPlugins.copyInto(retVal[1]);
		retVal[2] = new String[notLoaded.size()];
		notLoaded.copyInto(retVal[2]);

		return retVal;
	}

	public static boolean removePlugins(View view, String[] plugins)
	{
		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < plugins.length; i++)
		{
			buf.append(plugins[i]);
			buf.append('\n');
		}
		String[] args = { buf.toString() };

		int result = JOptionPane.showConfirmDialog(view,
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
			ok |= removePlugin(view,plugin);
		}

		if(ok)
			GUIUtilities.message(view,"remove-plugins.done",new String[0]);

		return ok;
	}

	public static boolean installPlugins(View view)
	{
		InstallPluginsDialog dialog = new InstallPluginsDialog(view);
		String[] urls = dialog.getPluginURLs();
		String dir = dialog.getInstallDirectory();
		if(urls != null && dir != null)
		{
			String[] dirs = new String[urls.length];
			for(int i = 0; i < dirs.length; i++)
			{
				dirs[i] = dir;
			}

			if(installPlugins(view,urls,dirs))
			{
				GUIUtilities.message(view,"install-plugins.done",
					new String[0]);
				return true;
			}
		}
		return false;
	}

	public static boolean updatePlugins(View view)
	{
		UpdatePluginsDialog dialog = new UpdatePluginsDialog(view);
		PluginList.Plugin[] plugins = dialog.getPlugins();
		if(plugins != null)
		{
			String sysPluginDir = MiscUtilities.constructPath(
				jEdit.getJEditHome(),"jars");

			String[] urls = new String[plugins.length];
			String[] dirs = new String[plugins.length];
			for(int i = 0; i < urls.length; i++)
			{
				String url = plugins[i].download;
				EditPlugin plugin = jEdit.getPlugin(plugins[i].clazz);
				JARClassLoader loader = (JARClassLoader)plugin
					.getClass().getClassLoader();

				String path = loader.getPath();
				File jarFile = new File(path);
				File srcFile = new File(path.substring(0,
					path.length() - 4));

				backup(jarFile);
				if(srcFile.exists())
					backup(srcFile);

				urls[i] = url;
				dirs[i] = jarFile.getParent();
			}

			if(installPlugins(view,urls,dirs))
			{
				GUIUtilities.message(view,"update-plugins.done",
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

	private static boolean removePlugin(View view, String plugin)
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
			GUIUtilities.error(view,"remove-plugins.error",args);
		}

		return ok;
	}

	private static boolean installPlugins(View view, String[] urls, String[] dirs)
	{
		PluginDownloadProgress progress = new PluginDownloadProgress(view,
			urls,dirs);

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
