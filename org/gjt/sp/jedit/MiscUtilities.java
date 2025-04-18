/*
 * MiscUtilities.java - Various miscellaneous utility functions
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright \<copyright> 1999-2013 Slava Pestov, Richard S. Hall, Dirk Moebius,
 *    jgellene, ezust, vanza, kpouer, Vampire0, Jarekczek, k_satoda, voituk,
 *    Thomas Meyer, Martin Raspe
 *   And possibly other members of the All Volunteer Developer Team (tm)
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

//{{{ Imports
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.MalformedInputException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gjt.sp.jedit.io.*;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.IOUtilities;

import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.util.StandardUtilities;
import org.gjt.sp.util.StringList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
//}}}

/**
 * Path, URL name manipulation, string manipulation, and more.<p>
 *
 * The most frequently used members of this class are:<p>
 *
 * <b>Some path name methods:</b><p>
 * <ul>
 * <li>{@link #getFileName(String)}</li>
 * <li>{@link #getParentOfPath(String)}</li>
 * <li>{@link #constructPath(String,String)}</li>
 * </ul>
 *
 * @version $Id$
 */
public class MiscUtilities
{
	//{{{ Path name methods

	//{{{ canonPath() method
	/**
	 * @return the canonical form of the specified path name. Currently
	 * only expands a leading <code>~</code>. <b>For local path names
	 * only.</b>
	 * @param path The path name
	 * @since jEdit 4.0pre2
	 */
	@Nonnull
	public static String canonPath(@Nonnull String path)
	{
		if(path.isEmpty())
			return path;

		if(path.startsWith("file://"))
			path = path.substring("file://".length());
		else if(path.startsWith("file:"))
			path = path.substring("file:".length());
		else if(isURL(path))
			return path;

		if(File.separatorChar == '\\')
		{
			// get rid of mixed paths on Windows
			path = path.replace('/','\\');
			// also get rid of trailing spaces on Windows
			int trim = path.length();
			while(path.charAt(trim - 1) == ' ')
				trim--;

			if (path.charAt(trim - 1) == '\\')
				while (trim > 1 && path.charAt(trim - 2) == '\\')
				{
					trim--;
				}
			path = path.substring(0,trim);
		}

		if(path.startsWith('~' + File.separator))
		{
			path = path.substring(2);
			String home = System.getProperty("user.home");

			if(home.endsWith(File.separator))
				return home + path;
			else
				return home + File.separator + path;
		}
		else if("~".equals(path))
			return System.getProperty("user.home");
		else if ("-".equals(path))
			return getParentOfPath(jEdit.getActiveView().getBuffer().getPath());
		else
			return path;
	} //}}}

	//{{{ expandVariables() method
	static final String varPatternString = "(\\$([a-zA-Z0-9_]+))";
	static final String varPatternString2 = "(\\$\\{([^}]+)\\})";
	static final String winPatternString  = "(%([^%]+)%)";
	static final Pattern varPattern = Pattern.compile(varPatternString);
	static final Pattern varPattern2 = Pattern.compile(varPatternString2);
	static final Pattern winPattern = Pattern.compile(winPatternString);


	/** A helper function for expandVariables when handling Windows paths on non-windows systems.
	*/
	private static String win2unix(String winPath)
	{
		String unixPath = winPath.replace('\\', '/');
		Matcher m = winPattern.matcher(unixPath);
		if (m.find())
		{
			String varName = m.group(2);
			String expansion = jEdit.systemManager.getenv(varName);
			if (expansion != null)
			{
				expansion = Matcher.quoteReplacement(expansion);
				return m.replaceFirst(expansion);
			}
		}
		return unixPath;
	}

	/** Accepts a string from the user (or a settings file) which may contain a variable-prefix path of various syntaxes. Performs the reverse of abbreviate() from any platform.
	 *  The function supports the following prefix syntaxes:
	 *     ~/ or ~\   expand to user.home
	 *     $varname (all platforms)
	 *     %varname% (all platforms)
	 *     ${varname} (non-Windows platforms)
	 *     And expand each of these by looking at the system environment variables for possible expansions.
	 *     @return a string which is either the unchanged input string, or one with expanded variables.
	 *     @since jEdit 4.3
	 *     @see #abbreviate
	 *     @author ezust
	 */
	public static String expandVariables(String arg)
	{
		if (arg.startsWith("~/") || arg.startsWith("~\\"))
			return System.getProperty("user.home") + arg.substring(1);

		Matcher m = winPattern.matcher(arg);
		if (!OperatingSystem.isWindows() && m.find() )
			return win2unix(arg);
		Pattern p = varPattern;
		m = p.matcher(arg);
		if (!m.find())
		{
			if (OperatingSystem.isWindows())
				p = winPattern;
			else p = varPattern2;
			m = p.matcher(arg);
			if (!m.find()) // no variables to substitute
				return arg;
		}
		String varName = m.group(2);
		String expansion = jEdit.systemManager.getenv(varName);
		if (expansion == null) {
			if (varName.equalsIgnoreCase("jedit_settings") && jEdit.getSettingsDirectory() != null) {
				expansion = jEdit.getSettingsDirectory();
			}
			else {
				// try everything uppercase?
				varName = varName.toUpperCase();
				String uparg = arg.toUpperCase();
				m = p.matcher(uparg);
				expansion = System.getenv(varName);
			}
		}
		if (expansion != null) {
			expansion = expansion.replace("\\", "\\\\");
			return m.replaceFirst(expansion);
		}
		return arg;
	} //}}}

	//{{{ abbreviate() methods
	/** The reverse of expandVariables(), returns a shortened path if possible.
	 *
	 *  Uses platform convention (%varname% on windows, $varname on other platforms)
	 *
	 *	@return an abbreviated path, replacing values with variables, if a prefix exists.
	 *  @see #expandVariables
	 *  @since jEdit 4.3
	 */
	public static String abbreviate(String path)
	{
		if (svc == null)
			svc = new VarCompressor();
		return svc.compress(path);
	}

	/** Same as abbreviate() but checks a view option which can
	 *  disable the feature for jEdit UI components.
	 *  @since jEdit 5.1
	 */
	public static String abbreviateView(String path)
	{
		if (!jEdit.getBooleanProperty("view.abbreviatePaths")) return path;
		return abbreviate(path);
	} //}}}

	//{{{ resolveSymlinks() method
	/**
	 * Resolves any symbolic links in the path name specified
	 * using <code>File.getCanonicalPath()</code>. <b>For local path
	 * names only.</b>
	 * @since jEdit 4.2pre1
	 */
	public static String resolveSymlinks(@Nonnull String path)
	{
		if(isURL(path))
			return path;

		// 2 aug 2003: OS/2 Java has a broken getCanonicalPath()
		if(OperatingSystem.isOS2())
			return path;
		// 18 nov 2003: calling this on a drive letter on Windows causes
		// drive access
		if(OperatingSystem.isWindows())
		{
			if(path.length() == 2 || path.length() == 3)
			{
				if(path.charAt(1) == ':')
					return path;
			}
		}
		try
		{
			return new File(path).getCanonicalPath();
		}
		catch(IOException io)
		{
			return path;
		}
	} //}}}

	//{{{ isAbsolutePath() method
	/**
	 * Returns if the specified path name is an absolute path or URL.
	 * @since jEdit 4.1pre11
	 */
	public static boolean isAbsolutePath(@Nonnull String path)
	{
		if(isURL(path))
			return true;
		else if(path.startsWith("~/") || path.startsWith('~' + File.separator) || "~".equals(path))
			return true;
		else if ("-".equals(path))
			return true;
		else if(OperatingSystem.isWindows())
		{
			if(path.length() == 2 && path.charAt(1) == ':')
			{
				//  c:
				return true;
			}
			if(path.length() > 2 &&
				path.charAt(1) == ':' &&
				(path.charAt(2) == '\\' || path.charAt(2) == '/'))
				return true;

			if(path.startsWith("\\\\")
				|| path.startsWith("//"))
				return true;
		}
		// not sure if this is correct for OpenVMS.
		else if(OperatingSystem.isUnix()
				|| OperatingSystem.isVMS())
		{
			// nice and simple
			if(!path.isEmpty() && path.charAt(0) == '/')
				return true;
		}

		return false;
	} //}}}

	//{{{ constructPath() methods
	/**
	 * Constructs an absolute path name from a directory and another
	 * path name. This method is VFS-aware.
	 * @param parent The directory
	 * @param path The path name
	 */
	public static String constructPath(@Nullable String parent, @Nonnull String path)
	{
		if(isAbsolutePath(path))
			return canonPath(path);

		if (parent == null)
			parent = System.getProperty("user.dir");

		if (path.isEmpty())
			return parent;

		// have to handle this case specially on windows.
		// insert \ between, eg A: and myfile.txt.
		if(OperatingSystem.isWindows())
		{
			if(path.length() == 2 && path.charAt(1) == ':')
			{
				// c: -> c:
				return path;
			}
			if  (path.length() > 2 && path.charAt(1) == ':' && path.charAt(2) != '\\')
			{
				// c:blabla.txt -> c:\blabla.txt
				path = path.substring(0,2) + '\\'
					+ path.substring(2);
				return canonPath(path);
			}
		}

		String dd = ".." + File.separator;
		String d = '.' + File.separator;

		for(;;)
		{
			if(".".equals(path))
				return parent;
			else if("..".equals(path))
				return getParentOfPath(parent);
			else if(path.startsWith(dd) || path.startsWith("../"))
			{
				parent = getParentOfPath(parent);
				path = path.substring(3);
			}
			else if(path.startsWith(d) || path.startsWith("./"))
				path = path.substring(2);
			else
				break;
		}
		if(path.isEmpty())
			return parent;

		if(OperatingSystem.isWindows() && !isURL(parent) && path.charAt(0) == '\\')
			parent = parent.substring(0,2);

		VFS vfs = VFSManager.getVFSForPath(parent);

		return canonPath(vfs.constructPath(parent,path));
	}

	/**
	 * Constructs an absolute path name from three path components.
	 * This method is VFS-aware.
	 * @param parent The parent directory
	 * @param path1 The first path
	 * @param path2 The second path
	 */
	public static String constructPath(String parent,
				    String path1, String path2)
	{
		return constructPath(constructPath(parent,path1),path2);
	} //}}}

	//{{{ concatPath() method
	/**
	 * Like {@link #constructPath}, except <code>path</code> will be
	 * appended to <code>parent</code> even if it is absolute.
	 * <b>For local path names only.</b>.
	 *
	 * @param parent the parent path
	 * @param path the path to append to the parent
	 */
	public static String concatPath(@Nonnull String parent, @Nonnull String path)
	{
		parent = canonPath(parent);
		path = canonPath(path);

		// Make all child paths relative.
		if (path.startsWith(File.separator))
			path = path.substring(1);
		else if (path.length() >= 3 && path.charAt(1) == ':')
			path = path.replace(':', File.separatorChar);

		if (parent.endsWith(File.separator))
			return parent + path;
		else
			return parent + File.separator + path;
	} //}}}

	//{{{ getFirstSeparatorIndex() method
	/**
	 * Return the first index of either / or the OS-specific file
	 * separator.
	 * @param path The path
	 * @return the index of the first separator or -1 if not found
	 * @since jEdit 4.3pre3
	 */
	public static int getFirstSeparatorIndex(String path)
	{
		int sepIndex = path.indexOf(File.separatorChar);
		int slashIndex = path.indexOf('/');
		if((sepIndex == -1) && (slashIndex == -1))
			return -1;
		if(sepIndex == -1)
			return slashIndex;
		if(slashIndex == -1)
			return sepIndex;
		return Math.min(sepIndex,slashIndex);
	} //}}}

	//{{{ getLastSeparatorIndex() method
	/**
	 * Return the last index of either / or the OS-specific file
	 * separator.
	 * @param path The path
	 * @since jEdit 4.3pre3
	 */
	public static int getLastSeparatorIndex(@Nonnull String path)
	{
		return getLastSeparatorIndex(path, false);
	}

	/**
	 * Return the last index of either / or the OS-specific file
	 * separator while optionally ignoring trailing separators.
	 * @param path The path
	 * @since jEdit 5.8pre1
	 */
	public static int getLastSeparatorIndex(@Nonnull String path, boolean ignoreTrailingSeparators)
	{
		int lastIndex = path.length() - 1;
		if(ignoreTrailingSeparators)
		{
			while(lastIndex > 0
					&& (path.charAt(lastIndex) == File.separatorChar
					|| path.charAt(lastIndex) == '/'))
			{
				lastIndex--;
			}
		}

		return Math.max(path.lastIndexOf(File.separatorChar,lastIndex),
				path.lastIndexOf('/',lastIndex));
	} //}}}

	//{{{ getFileExtension() method
	/**
	 * Returns the extension of the specified filename, starting with the last dot.
	 * @param path The path
	 * @return the last dot (.) and the text that follows, or an empty
	 * string if no dots are found.
	 * i.e. if your filename is
	 *    IP-192.168.1.1-data.tar.gz
	 * it will return
	 *    .gz
	 * @see #getCompleteBaseName(String)
	 */
	@Nonnull
	public static String getFileExtension(String path)
	{
		int fsIndex = getLastSeparatorIndex(path);
		int index = path.lastIndexOf('.');
		// there could be a dot in the path and no file extension
		if(index == -1 || index < fsIndex )
			return "";
		else
			return path.substring(index);
	} //}}}

	//{{{ getFileName() method
	/**
	 * Returns the last component of the specified path.
	 * This method is VFS-aware.
	 * @param path The path name
	 */
	public static String getFileName(String path)
	{
		return VFSManager.getVFSForPath(path).getFileName(path);
	} //}}}

	//{{{ getCompleteBaseName() method
	/**
	 * @return the complete basename of a fileName (before the last period).
	 * i.e. if your filename is
	 *    /net/log/IP-192.168.1.1-data.tar.gz
	 * it will return
	 *    IP-192.168.1.1-data.tar
	 * @param path the path name
	 * @see #getBaseName(String) getBaseName
	 * @see #getFileExtension(String) getFileExtension
	 * @since jEdit 5.0
	 */
	public static String getCompleteBaseName(String path)
	{
		String name = getFileName(path);
		int index = name.lastIndexOf('.');
		if (index == -1)
			return name;
		return name.substring(0, index);
	} //}}}

	//{{{ getBaseName() method
	/**
	 * @return the base name of a fileName (before the first period).
	 * i.e. If your filename is
	 *     /net/log/IP-192.168.1.1-data.tar.gz
	 * it will return
	 *     IP-192
	 * @param path The path name
	 * @since jEdit 5.0
	 * @see #getCompleteBaseName(String)
	*/
	public static String getBaseName(String path)
	{
		String name = getFileName(path);
		int index = name.indexOf('.');
		if(index == -1)
			return name;
		else
			return name.substring(0,index);
	} //}}}

	//{{{ getParentOfPath() method
	/**
	 * Returns the parent of the specified path. This method is VFS-aware.
	 * @param path The path name
	 * @since jEdit 2.6pre5
	 */
	public static String getParentOfPath(String path)
	{
		return VFSManager.getVFSForPath(path).getParentOfPath(path);
	} //}}}

	//{{{ getProtocolOfURL() method
	/**
	 * Returns the protocol specified by a URL.
	 * @param url The URL
	 * @since jEdit 2.6pre5
	 */
	public static String getProtocolOfURL(String url)
	{
		return url.substring(0,url.indexOf(':'));
	} //}}}

	//{{{ isURL() method
	/**
	 * Checks if the specified string is a URL.
	 * @param str The string to check
	 * @return True if the string is a URL, false otherwise
	 */
	public static boolean isURL(@Nonnull String str)
	{
		int fsIndex = getFirstSeparatorIndex(str);
		if(fsIndex == 0) // /etc/passwd
			return false;
		else if(fsIndex == 2) // C:\AUTOEXEC.BAT
			return false;

		int cIndex = str.indexOf(':');
		if(cIndex <= 1) // D:, or doesn't contain : at all
			return false;

		String protocol = str.substring(0,cIndex);
		VFS vfs = VFSManager.getVFSForProtocol(protocol);
		if(!(vfs instanceof UrlVFS))
			return true;

		try
		{
			new URL(str);
			return true;
		}
		catch(MalformedURLException mf)
		{
			return false;
		}
	} //}}}

	// {{{ isUncPath() method
	/**
	 * Checks if the specified string is a UNC path.
	 * @param str The string to check
	 * @return {@code true} if the string is a UNC path, {@code false} otherwise
	 */
	public static boolean isUncPath(@Nonnull String str)
	{
		return (str.length() >= 2)
				&& (((str.charAt(0) == File.separatorChar) && (str.charAt(1) == File.separatorChar))
				|| ((str.charAt(0) == '/') && (str.charAt(1) == '/')));
	} //}}}

	//{{{ getNthBackupFile method
	/**
	 * Gets the file to store the Nth backup of the given file.
	 * @param name The last part of the filename of the file being
	 *             backed up.
	 * @param backup The number of the current backup.
	 * @param backups Total number of backup copies.
	 * @since 5.0pre1
	 */
	public static File getNthBackupFile(String name, int backup,
			int backups, String backupPrefix,
			String backupSuffix, String backupDirectory)
	{
		File backupFile;
		if(backupPrefix == null)
			backupPrefix = "";
		if(backupSuffix == null)
			backupSuffix = "";
		if(backups <= 1)
		{
			backupFile = new File(backupDirectory,
				backupPrefix + name + backupSuffix);
		}
		else
		{
			backupFile = new File(backupDirectory,
				backupPrefix + name + backupSuffix
				+ backup + backupSuffix);
		}
		return backupFile;
	} //}}}

	//{{{ openInDesktop() method
	/** Opens a file or URI using the desktop file associations.
		<p>
		Uses native desktop commands for each platform, which ask the user to choose an
		association for files that do not already have one, using the desktop's
		dialog, in contrast to Desktop.open() which just throws an IOException
		for unknown types.

		If a URI is supplied, use desktop browser.

		@param path or URI of thing to open/browse
		@author Alan Ezust
		@since jEdit 5.0
	*/
	public static void openInDesktop(String path)
	{
		StringList sl = new StringList();
		if (OperatingSystem.isWindows())
		{
			if (MiscUtilities.isURL(path)) try {
				URI uri = new URI(path);
				java.awt.Desktop.getDesktop().browse(uri);
				return;
			}
			catch (IOException ioe) {
				Log.log(Log.ERROR, path, "Can't open URI", ioe);
			} catch (URISyntaxException use) {
				Log.log(Log.ERROR, path, "Bad URI syntax:", use);
			}
			else {
				sl.add("rundll32");
				sl.add("SHELL32.DLL,ShellExec_RunDLL");
			}
		}
		else if (OperatingSystem.isMacOS())
			sl.add("open");
		else if (OperatingSystem.isX11())
		{
			/* For gnome, use gnome-open. Need a way of testing that gnome is actually
			   running though. Otherwise it is not the correct program to use.
			File f = new File("/usr/bin/gnome-open");
			if (f.exists()) sl.add("gnome-open");
			else */
			sl.add("xdg-open");
		}
		try
		{
			if (sl.isEmpty()) // I don't know what platform it is
				java.awt.Desktop.getDesktop().open(new File(path));
			else
			{
				sl.add(path);
				Log.log(Log.DEBUG, MiscUtilities.class, "openInDesktop: " + sl.join(" "));
				Runtime.getRuntime().exec(sl.toArray());
			}
		}
		catch (IOException ioe)
		{
			Log.log(Log.ERROR, MiscUtilities.class, "openInDesktop failed: " + path, ioe);
		}
	}// }}}

	//{{{ prepareAutosaveDirectory method
	/**
	 * Prepares the directory to autosave the specified file.
	 * A jEdit property is used to determine the directory.
	 * If there is none specified by props,
	 * then the current directory is used, but only for local files.
	 * The directory is created if it does not exist.
	 * @param path path to the buffer
	 * @return Autosave directory. <code>null</code> is returned for
	 * non-local files if no backup directory is specified in properties.
	 * @since jEdit 5.5
	 */
	public static File prepareAutosaveDirectory(String path)
	{
		boolean isLocal = VFSManager.getVFSForPath(path) instanceof FileVFS;
		File file;
		if (isLocal)
			file = new File(path);
		else
			file = new File(replaceNonPathChars(path, "_"));
		File dir = file;
		if (!dir.isDirectory())
			dir=dir.getParentFile();

		// Check for autosave.directory
		String autosaveDirectory = jEdit.getProperty("autosave.directory");
		if(autosaveDirectory != null)
		{
			autosaveDirectory = MiscUtilities.expandVariables(autosaveDirectory);
			if (path.startsWith(autosaveDirectory))
				return dir;
			// Perhaps here we would want to guard with
			// a property for parallel backups or not.
			autosaveDirectory = MiscUtilities.concatPath(
				autosaveDirectory, dir.getAbsolutePath());
			dir = new File(autosaveDirectory);
			if (!dir.exists())
				dir.mkdirs();
		}
		else {
			if (!isLocal)
				return null;
		}
		return dir;

	} //}}}


	//{{{ getBackupDirectory method
	/**
	 * Get backup.directory property, or null.
	 * @return backup.directory property, or null
	 * @since jEdit 5.5pre1
	 */
	public static String getBackupDirectory()
	{
		String backupDirectory = jEdit.getProperty("backup.directory");
		if(backupDirectory == null || backupDirectory.isEmpty())
		{
			return null;
		} else {
			return MiscUtilities.expandVariables(backupDirectory);
		}
	}// }}}

	//{{{ prepareBackupDirectory method
	/**
	 * Prepares the directory to backup the specified file.
	 * A jEdit property is used to determine the directory.
	 * If there is no dedicated backup directory specified by props,
	 * then the current directory is used, but only for local files.
	 * The directory is created if it does not exist.
	 * @param path path to the buffer
	 * @return Backup directory. <code>null</code> is returned for
	 * non-local files if no backup directory is specified in properties.
	 * @since 5.0pre1
	 */
	public static File prepareBackupDirectory(String path)
	{
		boolean isLocal = VFSManager.getVFSForPath(path) instanceof FileVFS;
		File file;
		if (isLocal)
			file = new File(path);
		else
			file = new File(replaceNonPathChars(path, "_"));
		File dir = file;
		if (!dir.isDirectory())
			dir=dir.getParentFile();

		// Check for backup.directory
		String backupDirectory = getBackupDirectory();
		if(backupDirectory == null)
		{
			if (!isLocal)
				return null;
		}
		else
		{
			if (path.startsWith(backupDirectory))
				return dir;
			// Perhaps here we would want to guard with
			// a property for parallel backups or not.
			backupDirectory = MiscUtilities.concatPath(
				backupDirectory, dir.getAbsolutePath());
			dir = new File(backupDirectory);
			if (!dir.exists())
				dir.mkdirs();
		}
		return dir;

	} //}}}

	//{{{ prepareBackupFile methods
	/**
	 * Prepares the filename for performing backup of the given file.
	 * In case of multiple backups does necessary backup renumbering.
	 * Checks whether the last backup was not earlier than
	 * <code>backup.minTime</code> (property) ms ago.
	 * Uses jedit properties to determine backup parameters,
	 * like prefix, suffix.
	 * @param path The file to back up.
	 * @param backupDir The directory, usually obtained from
	 *                  <code>prepareBackupDirectory</code>.
	 * @return File suitable for backup of <code>file</code>,
	           or <code>null</code> if the last backup was
	           less than <code>backup.minTime</code> ms ago.
	 * @since 5.0pre1
	 */
	public static File prepareBackupFile(String path, File backupDir)
	{
		// read properties
		int backups = jEdit.getIntegerProperty("backups",1);
		String backupPrefix = jEdit.getProperty("backup.prefix");
		String backupSuffix = jEdit.getProperty("backup.suffix");
		int backupTimeDistance = jEdit.getIntegerProperty("backup.minTime",0);

		return prepareBackupFile(path, backups, backupPrefix,
				backupSuffix, backupDir.getPath(),
				backupTimeDistance);
	}

	/**
	 * Prepares the filename for performing backup of the given file.
	 * In case of multiple backups does necessary backup renumbering.
	 * Checks whether the last backup was not earlier than
	 * <code>backupTimeDistance</code> ms ago.
	 * @param path The file to back up.
	 * @param backups The number of backups. Must be &gt;= 1. If &gt; 1, backup
	 * files will be numbered.
	 * @param backupDirectory The directory determined externally or
	 * obtained from <code>prepareBackupDirectory</code>.
	 * @return File suitable for backup of <code>file</code>,
	           or <code>null</code> if the last backup was
	           less than <code>backupTimeDistance</code> ms ago.
	 * @since 5.0pre1
	 */
	public static File prepareBackupFile(String path, int backups,
		 String backupPrefix, String backupSuffix,
		 String backupDirectory, int backupTimeDistance)
	{
		File file;
		boolean isLocal = VFSManager.getVFSForPath(path) instanceof FileVFS;
		if (isLocal)
			file = new File(path);
		else
			file = new File(replaceNonPathChars(path, "_"));

		String name = file.getName();
		File backupFile = getNthBackupFile(name, 1, backups,
				backupPrefix, backupSuffix,
				backupDirectory);
		if (backupFile.equals(file))
		{
			Log.log(Log.WARNING, MiscUtilities.class,
				jEdit.getProperty("ioerror.backup-same-name")
				+ " " + jEdit.getProperty("ioerror.backup-failed"));
			return null;
		}

		long modTime = backupFile.lastModified();
		/* if backup file was created less than
		* 'backupTimeDistance' ago, we do not
		* create the backup */
		if(System.currentTimeMillis() - modTime
			< backupTimeDistance)
		{
			Log.log(Log.DEBUG,MiscUtilities.class,
				"Backup not done because of backup.minTime");
			return null;
		}

		File lastBackup = getNthBackupFile(name, backups, backups,
				backupPrefix, backupSuffix,
				backupDirectory);
		// Under unfortunate circumstances the calculated backup
		// file name may be equal to the source file name. Be careful
		// not to delete the original file.
		if (!lastBackup.equals(file))
			lastBackup.delete();

		if(backups > 1)
		{
			for(int i = backups - 1; i > 0; i--)
			{
				File backup1 = getNthBackupFile(name, i,
					backups, backupPrefix,
					backupSuffix, backupDirectory);
				File backup2 = getNthBackupFile(name, i+1,
					backups, backupPrefix,
					backupSuffix, backupDirectory);

				backup1.renameTo(backup2);
			}

		}
		return backupFile;
	} //}}}

	//{{{ saveBackup() methods
	/**
	 * Saves a backup (optionally numbered) of a file. Reads
	 * jedit properties to determine backup parameters, like
	 * prefix, suffix, directory.
	 * <p>This version calls
	 * <code>prepareBackupDirectory</code>.
	 * @param file A local file
	 * @since jEdit 5.0pre1
	 */
	public static void saveBackup(File file)
	{
		File backupDir = prepareBackupDirectory(file.toString());
		File backupFile = prepareBackupFile(file.toString(), backupDir);
		if (backupFile != null)
			saveBackup(file, backupFile);
	}

	/**
	 * Saves a backup (optionally numbered) of a file.
	 * @param file A local file
	 * @param backups The number of backups. Must be &gt;= 1. If &gt; 1, backup
	 * files will be numbered.
	 * @param backupPrefix The backup file name prefix
	 * @param backupSuffix The backup file name suffix
	 * @param backupDirectory The directory where to save backups; if null,
	 * they will be saved in the same directory as the file itself.
	 * @since jEdit 4.0pre1
	 */
	 public static void saveBackup(File file, int backups,
		 String backupPrefix, String backupSuffix,
		 String backupDirectory)
	{
		saveBackup(file,backups,backupPrefix,backupSuffix,backupDirectory,0);
	}

	/**
	 * Saves a backup (optionally numbered) of a file. Requires
	 * specifying the backup directory and generates the backup filename.
	 * @param file A local file
	 * @param backups The number of backups. Must be &gt;= 1. If &gt; 1, backup
	 * files will be numbered.
	 * @param backupPrefix The backup file name prefix
	 * @param backupSuffix The backup file name suffix
	 * @param backupDirectory The directory where to save backups; if null,
	 * they will be saved in the same directory as the file itself.
	 * @param backupTimeDistance The minimum time in minutes when a backup
	 * version 1 shall be moved into version 2; if 0, backups are always
	 * moved.
	 * @since jEdit 4.2pre5
	 */
	public static void saveBackup(File file, int backups,
			       String backupPrefix, String backupSuffix,
			       String backupDirectory, int backupTimeDistance)
	{
		File backupFile = prepareBackupFile(file.toString(), backups,
				backupPrefix, backupSuffix,
				backupDirectory, backupTimeDistance);
		if (backupFile == null)
			return;

		saveBackup(file, backupFile);
	}

	/**
	 * Saves a backup of a local file. Requires
	 * specifying source and destination files.
	 * @param file A local file
	 * @param backupFile A local backup file.
	 * @since jEdit 5.0pre1
	 */
	public static void saveBackup(File file, File backupFile)
	{
		Log.log(Log.DEBUG,MiscUtilities.class,
			"Saving backup of file \"" +
			file.getAbsolutePath() + "\" to \"" +
			backupFile.getAbsolutePath() + '"');
		if (!file.renameTo(backupFile))
			IOUtilities.moveFile(file, backupFile);
	}
	//}}}

	//{{{ isBinary() methods
	/**
	 * Check if an InputStream is binary.
	 * First this tries encoding auto detection. If an encoding is
	 * detected, the stream should be a text stream. Otherwise, this
	 * will check the first characters 100
	 * (jEdit property vfs.binaryCheck.length) in the system default
	 * encoding. If more than 1 (jEdit property vfs.binaryCheck.count)
	 * NUL(\u0000) was found, the stream is declared binary.
	 *
	 * This is not 100% because sometimes the autodetection could fail.
	 *
	 * This method will not close the stream. You have to do it yourself
	 *
	 * @param in the stream
	 * @return <code>true</code> if the stream was detected as binary
	 * @throws IOException IOException If an I/O error occurs
	 * @since jEdit 4.3pre10
	 */
	public static boolean isBinary(InputStream in) throws IOException
	{
		AutoDetection.Result detection = new AutoDetection.Result(in);
		// If an encoding is detected, this is a text stream
		if (detection.getDetectedEncoding() != null)
		{
			return false;
		}
		// Read the stream in system default encoding. The encoding
		// might be wrong. But enough for binary detection.
		try
		{
			return containsNullCharacter(
				new InputStreamReader(detection.getRewindedStream()));
		}
		catch (MalformedInputException mie)
		{
			// This error probably means the input is binary.
			return true;
		}
	} //}}}

	//{{{ isBackup() method
	/**
	 * Check if the filename is a backup file.
	 * @param filename the filename to check
	 * @return true if this is a backup file.
	 * @since jEdit 4.3pre5
	 */
	public static boolean isBackup(String filename)
	{
		if (filename == null)
		{
			return false;
		}
		
		if (jEdit.getIntegerProperty("backups") <= 0)
		{
				return false;
		}

		// drop the path for the remaining checks
		filename = getFileName(filename);
		
		// check for Untitled-XX
		if (filename.matches("Untitled-\\d+"))
		{
			return false;
		}

		// check for #Untitled-X# and #filename#save#
		if (filename.matches("[#]Untitled-\\d+[#]") || filename.matches("[#].*?[#]save[#]"))
		{
			return true;
		}

		// check for user supplied prefix and suffix
		String backupPrefix = jEdit.getProperty("backup.prefix");
		String backupSuffix = jEdit.getProperty("backup.suffix");
		if (backupPrefix != null && !backupPrefix.isEmpty() && backupSuffix != null && !backupSuffix.isEmpty())
		{
			return filename.startsWith(backupPrefix) && filename.endsWith(backupSuffix);
		}

		if (backupPrefix != null && !backupPrefix.isEmpty() && filename.startsWith(backupPrefix))
		{
			return true;
		}

		if (backupSuffix != null && !backupSuffix.isEmpty() && filename.endsWith(backupSuffix))
		{
			return true;
		}
		
		// if the user sets an empty prefix and suffix, then check if the filename 
		// ends with a number, but isn't a known mode suffix
		if ((backupPrefix == null || backupPrefix.isEmpty()) && (backupSuffix == null || backupSuffix.isEmpty()))
		{
			if (filename.matches(".*?\\d+")) 
			{
				Mode[] modes = org.gjt.sp.jedit.syntax.ModeProvider.instance.getModes();
				for (Mode mode : modes) 
				{
					if (mode.acceptFile(null, filename)) 
					{
						return false;	
					}
				}
				return true;
			}
		}
		return false;
	} //}}}

	//{{{ autodetect() method
	/**
	 * Tries to detect if the stream is gzipped, and if it has an encoding
	 * specified with an XML PI.
	 *
	 * @param in the input stream reader that must be autodetected
	 * @param buffer a buffer. It can be null if you only want to autodetect the encoding of a file
	 * @return a Reader using the detected encoding
	 * @throws IOException io exception during read
	 * @since jEdit 4.3pre5
	 */
	public static Reader autodetect(InputStream in, Buffer buffer) throws IOException
	{
		String encoding;
		if (buffer == null)
			encoding = System.getProperty("file.encoding");
		else
			encoding = buffer.getStringProperty(JEditBuffer.ENCODING);
		boolean gzipped = false;

		if (buffer == null || buffer.getBooleanProperty(Buffer.ENCODING_AUTODETECT))
		{
			AutoDetection.Result detection = new AutoDetection.Result(in);
			gzipped = detection.streamIsGzipped();
			if (gzipped)
			{
				Log.log(Log.DEBUG, MiscUtilities.class
					, "Stream is Gzipped");
			}
			String detected = detection.getDetectedEncoding();
			if (detected != null)
			{
				encoding = detected;
				Log.log(Log.DEBUG, MiscUtilities.class
					, "Stream encoding detected is " + detected);
			}
			in = detection.getRewindedStream();
		}
		else
		{
			// Make the stream buffered in the same way.
			in = AutoDetection.getMarkedStream(in);
		}

		Reader result = EncodingServer.getTextReader(in, encoding);
		if (buffer != null)
		{
			// Store the successful properties.
			if (gzipped)
			{
				buffer.setBooleanProperty(Buffer.GZIPPED,true);
			}
			buffer.setProperty(JEditBuffer.ENCODING, encoding);
		}
		return result;
	} //}}}

	//{{{ fileToClass() method
	/**
	 * Converts a file name to a class name. All slash characters are
	 * replaced with periods and the trailing '.class' is removed.
	 * @param name The file name
	 */
	public static String fileToClass(String name)
	{
		char[] clsName = name.toCharArray();
		for(int i = clsName.length - 6; i >= 0; i--)
			if(clsName[i] == '/')
				clsName[i] = '.';
		return new String(clsName,0,clsName.length - 6);
	} //}}}

	//{{{ classToFile() method
	/**
	 * Converts a class name to a file name. All periods are replaced
	 * with slashes and the '.class' extension is added.
	 * @param name The class name
	 */
	public static String classToFile(String name)
	{
		return name.replace('.','/').concat(".class");
	} //}}}

	//{{{ pathsEqual() method
	/**
	 * @param p1 A path name
	 * @param p2 A path name
	 * @return True if both paths are equal, ignoring trailing slashes, as
	 * well as case insensitivity on Windows.
	 * @since jEdit 4.3pre2
	 */
	public static boolean pathsEqual(String p1, String p2)
	{
		VFS v1 = VFSManager.getVFSForPath(p1);
		VFS v2 = VFSManager.getVFSForPath(p2);

		if(v1 != v2)
			return false;

		if(p1.endsWith("/") || p1.endsWith(File.separator))
			p1 = p1.substring(0,p1.length() - 1);

		if(p2.endsWith("/") || p2.endsWith(File.separator))
			p2 = p2.substring(0,p2.length() - 1);

		if((v1.getCapabilities() & VFS.CASE_INSENSITIVE_CAP) != 0)
			return p1.equalsIgnoreCase(p2);
		else
			return p1.equals(p2);
	} //}}}

	//}}}

	//{{{ Text methods

	//{{{ escapesToChars() method
	/**
	 * Converts "\n" and "\t" escapes in the specified string to
	 * newlines and tabs.
	 * @param str The string
	 * @since jEdit 2.3pre1
	 */
	public static String escapesToChars(String str)
	{
		StringBuilder buf = new StringBuilder();
		for(int i = 0; i < str.length(); i++)
		{
			char c = str.charAt(i);
			switch(c)
			{
			case '\\':
				if(i == str.length() - 1)
				{
					buf.append('\\');
					break;
				}
				c = str.charAt(++i);
				switch(c)
				{
				case 'n':
					buf.append('\n');
					break;
				case 't':
					buf.append('\t');
					break;
				default:
					buf.append(c);
					break;
				}
				break;
			default:
				buf.append(c);
			}
		}
		return buf.toString();
	} //}}}

	//{{{ getLongestPrefix() methods
	/**
	 * Returns the longest common prefix in the given set of strings.
	 * @param str The strings
	 * @param ignoreCase If true, case insensitive
	 * @since jEdit 4.2pre2
	 */
	public static String getLongestPrefix(List<String> str, boolean ignoreCase)
	{
		if(str.isEmpty())
			return "";

		int prefixLength = 0;

loop:		for(;;)
		{
			String s = str.get(0);
			if(prefixLength >= s.length())
				break loop;
			char ch = s.charAt(prefixLength);
			for(int i = 1; i < str.size(); i++)
			{
				s = str.get(i);
				if(prefixLength >= s.length())
					break loop;
				if(!compareChars(s.charAt(prefixLength),ch,ignoreCase))
					break loop;
			}
			prefixLength++;
		}

		return str.get(0).substring(0,prefixLength);
	}

	/**
	 * Returns the longest common prefix in the given set of strings.
	 * @param str The strings
	 * @param ignoreCase If true, case insensitive
	 * @since jEdit 4.2pre2
	 */
	public static String getLongestPrefix(String[] str, boolean ignoreCase)
	{
		return getLongestPrefix((Object[])str,ignoreCase);
	}

	/**
	 * Returns the longest common prefix in the given set of strings.
	 * @param str The strings (calls <code>toString()</code> on each object)
	 * @param ignoreCase If true, case insensitive
	 * @since jEdit 4.2pre6
	 */
	public static String getLongestPrefix(Object[] str, boolean ignoreCase)
	{
		if(str.length == 0)
			return "";

		int prefixLength = 0;

		String first = str[0].toString();

loop:		for(;;)
		{
			if(prefixLength >= first.length())
				break loop;
			char ch = first.charAt(prefixLength);
			for(int i = 1; i < str.length; i++)
			{
				String s = str[i].toString();
				if(prefixLength >= s.length())
					break loop;
				if(!compareChars(s.charAt(prefixLength),ch,ignoreCase))
					break loop;
			}
			prefixLength++;
		}

		return first.substring(0,prefixLength);
	} //}}}

	//}}}

	//{{{ buildToVersion() method
	/**
	 * Converts an internal version number (build) into a
	 * `human-readable' form.
	 * @param build The build
	 */
	public static String buildToVersion(String build)
	{
		if(build.length() != 11)
			return "<unknown version: " + build + '>';
		// First 2 chars are the major version number
		int major = Integer.parseInt(build.substring(0,2));
		// Second 2 are the minor number
		int minor = Integer.parseInt(build.substring(3,5));
		// Then the pre-release status
		int beta = Integer.parseInt(build.substring(6,8));
		// Finally the micro version number
		int micro = Integer.parseInt(build.substring(9,11));

		return major + "." + minor
			+ (beta != 99 ? "pre" + beta : "." + micro);
	} //}}}

	//{{{ isToolsJarAvailable() method
	/**
	 * If on JDK 1.2 or higher, make sure that tools.jar is available.
	 * This method should be called by plugins requiring the classes
	 * in this library.
	 * <p>
	 * tools.jar is searched for in the following places:
	 * <ol>
	 *   <li>the classpath that was used when jEdit was started,
	 *   <li>jEdit's jars folder in the user's home,
	 *   <li>jEdit's system jars folder,
	 *   <li><i>java.home</i>/lib/. In this case, tools.jar is added to
	 *       jEdit's list of known jars using jEdit.addPluginJAR(),
	 *       so that it gets loaded through JARClassLoader.
	 * </ol><p>
	 *
	 * On older JDK's this method does not perform any checks, and returns
	 * <code>true</code> (even though there is no tools.jar).
	 *
	 * @return <code>false</code> if and only if on JDK 1.2 and tools.jar
	 *    could not be found. In this case it prints some warnings on Log,
	 *    too, about the places where it was searched for.
	 * @since jEdit 3.2.2
	 */
	public static boolean isToolsJarAvailable()
	{
		Log.log(Log.DEBUG, MiscUtilities.class,"Searching for tools.jar...");

		Collection<String> paths = new LinkedList<>();

		//{{{ 1. Check whether tools.jar is in the system classpath:
		paths.add("System classpath: "
			+ System.getProperty("java.class.path"));

		try
		{
			// Either class sun.tools.javac.Main or
			// com.sun.tools.javac.Main must be there:
			try
			{
				Class.forName("sun.tools.javac.Main");
			}
			catch(ClassNotFoundException e1)
			{
				Class.forName("com.sun.tools.javac.Main");
			}
			Log.log(Log.DEBUG, MiscUtilities.class,
				"- is in classpath. Fine.");
			return true;
		}
		catch(ClassNotFoundException e)
		{
			//Log.log(Log.DEBUG, MiscUtilities.class,
			//	"- is not in system classpath.");
		} //}}}

		//{{{ 2. Check whether it is in the jEdit user settings jars folder:
		String settingsDir = jEdit.getSettingsDirectory();
		if(settingsDir != null)
		{
			String toolsPath = constructPath(settingsDir, "jars",
				"tools.jar");
			paths.add(toolsPath);
			if(new File(toolsPath).exists())
			{
				Log.log(Log.DEBUG, MiscUtilities.class,
					"- is in the user's jars folder. Fine.");
				// jEdit will load it automatically
				return true;
			}
		} //}}}

		//{{{ 3. Check whether it is in jEdit's system jars folder:
		String jEditDir = jEdit.getJEditHome();
		if(jEditDir != null)
		{
			String toolsPath = constructPath(jEditDir, "jars", "tools.jar");
			paths.add(toolsPath);
			if(new File(toolsPath).exists())
			{
				Log.log(Log.DEBUG, MiscUtilities.class,
					"- is in jEdit's system jars folder. Fine.");
				// jEdit will load it automatically
				return true;
			}
		} //}}}

		//{{{ 4. Check whether it is in <java.home>/lib:
		String toolsPath = System.getProperty("java.home");
		if(toolsPath.toLowerCase().endsWith(File.separator + "jre"))
			toolsPath = toolsPath.substring(0, toolsPath.length() - 4);
		toolsPath = constructPath(toolsPath, "lib", "tools.jar");
		paths.add(toolsPath);

		if(!new File(toolsPath).exists())
		{
			Log.log(Log.WARNING, MiscUtilities.class,
				"Could not find tools.jar.\n"
				+ "I checked the following locations:\n"
				+ paths.toString());
			return false;
		} //}}}

		//{{{ Load it, if not yet done:
		PluginJAR jar = jEdit.getPluginJAR(toolsPath);
		if(jar == null)
		{
			Log.log(Log.DEBUG, MiscUtilities.class,
				"- adding " + toolsPath + " to jEdit plugins.");
			jEdit.addPluginJAR(toolsPath);
		}
		else
			Log.log(Log.DEBUG, MiscUtilities.class,
				"- has been loaded before.");
		//}}}

		return true;
	} //}}}

	//{{{ parsePermissions() method
	/**
	 * Parse a Unix-style permission string (rwxrwxrwx).
	 * @param s The string (must be 9 characters long).
	 * @since jEdit 4.1pre8
	 */
	public static int parsePermissions(String s)
	{
		int permissions = 0;

		if(s.length() == 9)
		{
			if(s.charAt(0) == 'r')
				permissions += 0400;
			if(s.charAt(1) == 'w')
				permissions += 0200;
			if(s.charAt(2) == 'x')
				permissions += 0100;
			else if(s.charAt(2) == 's')
				permissions += 04100;
			else if(s.charAt(2) == 'S')
				permissions += 04000;
			if(s.charAt(3) == 'r')
				permissions += 040;
			if(s.charAt(4) == 'w')
				permissions += 020;
			if(s.charAt(5) == 'x')
				permissions += 010;
			else if(s.charAt(5) == 's')
				permissions += 02010;
			else if(s.charAt(5) == 'S')
				permissions += 02000;
			if(s.charAt(6) == 'r')
				permissions += 04;
			if(s.charAt(7) == 'w')
				permissions += 02;
			if(s.charAt(8) == 'x')
				permissions += 01;
			else if(s.charAt(8) == 't')
				permissions += 01001;
			else if(s.charAt(8) == 'T')
				permissions += 01000;
		}

		return permissions;
	} //}}}

	//{{{ getEncodings() methods
	/**
	 * Returns a list of supported character encodings.
	 * @since jEdit 4.3
	 * @param getSelected Whether to return just the selected encodings or all.
	 */
	public static String[] getEncodings(boolean getSelected)
	{
		Set<String> set;
		if (getSelected)
		{
			set = EncodingServer.getSelectedNames();
		}
		else
		{
			set = EncodingServer.getAvailableNames();
		}
		return set.toArray(StandardUtilities.EMPTY_STRING_ARRAY);
	} //}}}

	//{{{ throwableToString() method
	/**
	 * Returns a string containing the stack trace of the given throwable.
	 * @since jEdit 4.2pre6
	 */
	public static String throwableToString(Throwable t)
	{
		StringWriter s = new StringWriter();
		t.printStackTrace(new PrintWriter(s));
		return s.toString();
	} //}}}

	//{{{ Private members
	private MiscUtilities() {}

	//{{{ compareChars() method
	/**
	 * Compares two chars.
	 * should this be public?
	 * @param ch1 the first char
	 * @param ch2 the second char
	 * @param ignoreCase true if you want to ignore case
	 */
	private static boolean compareChars(char ch1, char ch2, boolean ignoreCase)
	{
		if(ignoreCase)
			return Character.toUpperCase(ch1) == Character.toUpperCase(ch2);
		else
			return ch1 == ch2;
	} //}}}

	//{{{ containsNullCharacter() method
	private static boolean containsNullCharacter(Reader reader)
		throws IOException
	{
		int nbChars = jEdit.getIntegerProperty("vfs.binaryCheck.length",100);
		int authorized = jEdit.getIntegerProperty("vfs.binaryCheck.count",1);
		for (long i = 0L;i < nbChars;i++)
		{
			int c = reader.read();
			if (c == -1)
				return false;
			if (c == 0)
			{
				authorized--;
				if (authorized == 0)
					return true;
			}
		}
		return false;
	} //}}}

	//{{{ replaceNonPathChars
	/**
	 * Replaces the characters which are usually invalid as part of pathname.
	 * Used by backup routines to convert remote filenames to local paths.
	 * @param replaceWith The string to replace illegal chars with,
	 * for example <code>_</code>.
	 */
	private static String replaceNonPathChars(String path, String replaceWith)
	{
		if (path == null)
			return null;
		String sForeignChars = ":*?\"<>|";
		// Construct a regex from sForeignChars
		StringBuilder sbForeignCharsEsc = new StringBuilder(20);
		for (int i = 0; i < sForeignChars.length(); i++)
		{
			sbForeignCharsEsc.append("\\");
			sbForeignCharsEsc.append(sForeignChars.charAt(i));
		}

		return path.replaceAll("[" + sbForeignCharsEsc + "]", replaceWith);
	} //}}}

	//}}}

	//{{{ storeProperties() method
	/**
	 * Stores properties with sorted keys.
	 * @param props  Given properties.
	 * @param out  Output stream.
	 * @param comments  Description of the property list.
	 * @since jEdit 5.3
	 */
	public static void storeProperties(Properties props, OutputStream out, String comments)
        throws IOException
	{
		Properties sorted = new Properties()
		{
			@Override
			public synchronized Enumeration<Object> keys()
			{
				return Collections.enumeration(new TreeSet<>(props.keySet()));
			}

			@Override
			public synchronized Set<Map.Entry<Object,Object>> entrySet()
			{
				return (new TreeMap<>(props)).entrySet();
			}

		};
		sorted.putAll(props);
		sorted.store(out, comments);
	} //}}}

	@Nullable
	static VarCompressor svc;

	//{{{ VarCompressor class
	/**
	 * Singleton class for quickly "compressing" paths into variable-prefixed values.
	 * @author alan ezust
	 */
	static class VarCompressor
	{
		/** a reverse mapping of values to environment variable names */
		final Map<String, String> prefixMap = new HashMap<>();
		/** previously compressed strings saved for quick access later */
		final Map<String, String> previous = new HashMap<>();

		//{{{ VarCompressor constructor
		VarCompressor()
		{
			ProcessBuilder pb = new ProcessBuilder();
			Map<String, String> env = pb.environment();
			if (OperatingSystem.isUnix())
				prefixMap.put(System.getProperty("user.home"), "~");
			if (jEdit.getSettingsDirectory() != null)
				prefixMap.put(jEdit.getSettingsDirectory(), "JEDIT_SETTINGS");
			for (Map.Entry<String, String> entry: env.entrySet())
			{
				String k = entry.getKey();
				if (k.equalsIgnoreCase("pwd") || k.equalsIgnoreCase("oldpwd")) continue;
				if (!Character.isLetter(k.charAt(0))) continue;
				String v = entry.getValue();
				// only add possible candidates to the prefix map
				if (!canBePathPrefix(v)) continue;
				// no need for trailing file separator
				if (v.endsWith(File.separator))
					v = v.substring(0, v.length()-1);
				// check if it is actually shorter
				if (OperatingSystem.isWindows())
					if (k.length()+2 > v.length()) continue; // gets replaced by %FOO%
				else
					if (k.length()+1 > v.length()) continue; // gets replaced by $FOO
				if (OperatingSystem.isWindows())
				{
					// no case sensitivity, might as well convert to lower case
					v = v.toLowerCase();
					k = k.toLowerCase();
				}
				if (prefixMap.containsKey(v))
				{
					String otherKey = prefixMap.get(v);
					if (otherKey.length() < k.length()) continue;
				}
				prefixMap.put(v, k);
			}

		} //}}}

		//{{{ compress() method
		String compress(String path)
		{
			String original = path;
			if (previous.containsKey(path))
			{
				return previous.get(path);
			}
			String bestPrefix = "/";
			String verifiedPrefix = bestPrefix;
			for (String tryPrefix : prefixMap.keySet())
			{
				if (tryPrefix.length() < bestPrefix.length()) continue;
				if (OperatingSystem.isWindows() &&
				    path.toLowerCase().startsWith(tryPrefix))
					bestPrefix = tryPrefix;
				else if (path.startsWith(tryPrefix))
				{
					bestPrefix = tryPrefix;
				}
				// Only use prefix if it is a directory-prefix of the path
				if (!bestPrefix.equals(verifiedPrefix))
				{
					String remainder = original.substring(bestPrefix.length());
					if (remainder.length() < 1 || remainder.startsWith(File.separator))
						verifiedPrefix = bestPrefix;
					else bestPrefix = verifiedPrefix;
				}
			}
			if (bestPrefix.length() > 1)
			{
				String remainder = original.substring(bestPrefix.length());
				String envvar = prefixMap.get(bestPrefix);
				if (envvar.equals("~"))
					path = envvar + remainder;
				else if (OperatingSystem.isWindows())
					path = '%' + envvar.toUpperCase() + '%' + remainder;
				else
					path = '$' + envvar + remainder;
			}
			previous.put(original, path);
			return path;
		} //}}}

		//{{{ canBePathPrefix() method
		// Returns true if the argument may absolutely point a directory.
		// For speed, no access to file system or network should happen.
		private static boolean canBePathPrefix(String s)
		{
			// Do not use File#isDirectory() since it causes
			// access to file system or network to check if
			// the directory is actually exists.
			return !s.contains(File.pathSeparator)
				&& new File(s).isAbsolute();
		} //}}}
	} //}}}

}
