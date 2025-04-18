/*
 * FileRootsVFS.java - Local root filesystems VFS
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2005 Slava Pestov
 * Portions copyright (C) 2002 Kris Kopicki
 * Portions copyright (C) 2002 Carmine Lucarelli
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

package org.gjt.sp.jedit.io;

//{{{ Imports
import javax.annotation.Nonnull;
import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;
import java.awt.Component;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.StringTokenizer;

import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.OperatingSystem;
import org.gjt.sp.jedit.jEdit;

import static org.gjt.sp.jedit.MiscUtilities.isUncPath;
//}}}

/**
 * A VFS that lists local root filesystems.
 * @author Slava Pestov
 * @version $Id$
 */
public class FileRootsVFS extends VFS
{
	public static final String PROTOCOL = "roots";

	//{{{ FileRootsVFS constructor
	public FileRootsVFS()
	{
		super("roots",LOW_LATENCY_CAP | BROWSE_CAP
			| NON_AWT_SESSION_CAP,
			new String[] { EA_TYPE });
	} //}}}

	//{{{ getParentOfPath() method
	@Override
	@Nonnull
	public String getParentOfPath(String path)
	{
		return PROTOCOL + ':';
	} //}}}

	//{{{ isRemotePath() method
	@Override
	public boolean isRemotePath(String path)
	{
		return false;
	} //}}}

	//{{{ _listFiles() method
	@Override
	public VFSFile[] _listFiles(Object session, String url, Component comp)
	{
		File[] roots = listRoots();

		if(roots == null)
			return null;

		VFSFile[] rootDE = new VFSFile[roots.length];
		for(int i = 0; i < roots.length; i++)
			rootDE[i] = new Root(roots[i]);

		return rootDE;
	} //}}}

	//{{{ _getFile() method
	@Override
	public VFSFile _getFile(Object session, String path, Component comp)
	{
		return new Root(new File(path));
	} //}}}

	//{{{ Private members
	private static final FileSystemView fsView = FileSystemView.getFileSystemView();

	//{{{ listRoots() method
	private static File[] listRoots()
	{
		if (OperatingSystem.isMacOS())
		{
			// Nasty hardcoded values
			File[] volumes = new File("/Volumes").listFiles();
			assert volumes != null : "Volumes cannot be null on macOS";
			LinkedList<File> roots = new LinkedList<>();

			roots.add(new File("/"));

			for (File volume : volumes)
			{
				// Make sure people don't do stupid things like putting files in /Volumes
				if (volume.isDirectory())
					roots.add(volume);
			}

			return roots.toArray(new File[0]);
		}
		else
		{
			File[] roots = File.listRoots();
			File[] fsViewRoots = fsView.getRoots();

			if(fsViewRoots == null)
				return roots;

			Set<File> rootsPlus = new HashSet<>(roots.length + fsViewRoots.length);
			rootsPlus.addAll(Arrays.asList(roots));
			rootsPlus.addAll(Arrays.asList(fsViewRoots));
			if(OperatingSystem.isWindows()) {
				String uncRoots = jEdit.getProperty("vfs.browser.additionalUncRoots", "\\\\wsl$");
				StringTokenizer st = new StringTokenizer(uncRoots);
				while(st.hasMoreTokens())
				{
					String uncRoot = st.nextToken();
					rootsPlus.add(new File(uncRoot));
				}
			}
			return rootsPlus.toArray(new File[0]);
		}
	} //}}}

	//}}}

	//{{{ Root class
	static class Root extends VFSFile
	{
		Root(File file)
		{
			// REMIND: calling isDirectory() on a floppy drive
			// displays stupid I/O error dialog box on Windows

			this.file = file;
			String path = file.getPath();
			if(OperatingSystem.isWindows()
					&& (path.length() == 3)
					&& (path.charAt(1) == ':')
					&& ((path.charAt(2) == File.separatorChar) || (path.charAt(2) == '/')))
				path = path.substring(0,2);
			setPath(path);
			setDeletePath(path);
			setSymlinkPath(path);

			if(fsView.isFloppyDrive(file))
			{
				setType(VFSFile.FILESYSTEM);
				setName(path);
			}
			else if(fsView.isDrive(file))
			{
				setType(VFSFile.FILESYSTEM);
				setName(path + ' '
					+ fsView.getSystemDisplayName(file));
			}
			else if(OperatingSystem.isWindows() && isUncPath(path))
			{
				setType(VFSFile.FILESYSTEM);
				setName(path);
			}
			else if(file.isDirectory())
			{
				if(fsView.isFileSystemRoot(file))
					setType(VFSFile.FILESYSTEM);
				else
					setType(VFSFile.DIRECTORY);

				if(OperatingSystem.isMacOS())
					setName(MiscUtilities.getFileName(path));
				else
					setName(path);
			}
			else
				setType(VFSFile.FILE);
		}

		@Override
		public Icon getIcon(boolean expanded, boolean openBuffer)
		{
			if(icon == null)
			{
				icon = isUncPath(file.getPath())
						? getDefaultIcon(expanded,openBuffer)
						: fsView.getSystemIcon(file);
			}
			return icon;
		}

		@Override
		public String getExtendedAttribute(String name)
		{
			if(name.equals(EA_TYPE))
				return super.getExtendedAttribute(name);
			else
			{
				// don't want it to show "0 bytes" for size,
				// etc.
				return null;
			}
		}

		private final File file;
		private transient Icon icon;
	} //}}}
}
