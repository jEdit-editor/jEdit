/*
 * VFS.java - Virtual filesystem implementation
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2003 Slava Pestov
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
import java.awt.Color;
import java.awt.Component;
import java.io.*;
import java.io.Closeable;
import java.util.*;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.bufferio.BufferLoadRequest;
import org.gjt.sp.jedit.bufferio.BufferSaveRequest;
import org.gjt.sp.jedit.bufferio.BufferInsertRequest;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.ProgressObserver;
import org.gjt.sp.util.IOUtilities;
import org.gjt.sp.util.StandardUtilities;
import org.gjt.sp.util.Task;
import org.gjt.sp.util.ThreadUtilities;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
//}}}

/**
 * A virtual filesystem implementation.<p>
 *
 * Plugins can provide virtual file systems by defining entries in their
 * <code>services.xml</code> files like so:
 *
 * <pre>&lt;SERVICE CLASS="org.gjt.sp.jedit.io.VFS" NAME="<i>name</i>"&gt;
 *    new <i>MyVFS</i>();
 *&lt;/SERVICE&gt;</pre>
 *
 * URLs of the form <code><i>name</i>:<i>path</i></code> will then be handled
 * by the VFS named <code><i>name</i></code>.<p>
 *
 * See {@link org.gjt.sp.jedit.ServiceManager} for details.<p>
 *
 * <h2>Session objects:</h2>
 *
 * A session is used to persist things like login information, any network
 * sockets, etc. File system implementations that do not need this kind of
 * persistence return a dummy object as a session.<p>
 *
 * Methods whose names are prefixed with "_" expect to be given a
 * previously-obtained session object. A session must be obtained with
 * this method:
 *
 * <ul>
 * <li>{@link #createVFSSession(String,Component)}</li>
 * </ul>
 *
 * That method should be called from the AWT (EDT) thread, unless
 * the filesystem has <code>NON_AWT_SESSION_CAP</code> capability.<p>
 *
 * When done, the session must be disposed of using
 * {@link #_endVFSSession(Object,Component)}.<p>
 *
 * <h2>Thread safety:</h2>
 *
 * The following methods cannot be called from an I/O thread:
 *
 * <ul>
 * <li>{@link #createVFSSession(String,Component)} - unless
 *     <code>NON_AWT_SESSION_CAP</code> capability is set</li>
 * <li>{@link #insert(View,Buffer,String)}</li>
 * <li>{@link #load(View,Buffer,String,boolean)}</li>
 * <li>{@link #save(View,Buffer,String)}</li>
 * </ul>
 *
 * All remaining methods are required to be thread-safe in subclasses.
 *
 * <h2>Implementing a VFS</h2>
 *
 * You can override as many or as few methods as you want. Make sure
 * {@link #getCapabilities()} returns a value reflecting the functionality
 * implemented by your VFS.
 *
 * @see VFSManager#getVFSForPath(String)
 * @see VFSManager#getVFSForProtocol(String)
 *
 * @author Slava Pestov
 * @author $Id$
 */
public abstract class VFS
{
	//{{{ Capabilities
	/**
	 * Read capability.
	 * @since jEdit 2.6pre2
	 */
	public static final int READ_CAP = 1 << 0;

	/**
	 * Write capability.
	 * @since jEdit 2.6pre2
	 */
	public static final int WRITE_CAP = 1 << 1;

	/**
	 * Browse capability
	 * @since jEdit 4.3pre11
	 *
	 * This was the official API for adding items to a file
	 * system browser's <b>Plugins</b> menu in jEdit 4.1 and earlier. In
	 * jEdit 4.2, there is a different way of doing this, you must provide
	 * a <code>browser.actions.xml</code> file in your plugin JAR, and
	 * define <code>plugin.<i>class</i>.browser-menu-item</code>
	 * or <code>plugin.<i>class</i>.browser-menu</code> properties.
	 * See {@link org.gjt.sp.jedit.EditPlugin} for details.
	 */
	public static final int BROWSE_CAP = 1 << 2;

	/**
	 * Delete file capability.
	 * @since jEdit 2.6pre2
	 */
	public static final int DELETE_CAP = 1 << 3;

	/**
	 * Rename file capability.
	 * @since jEdit 2.6pre2
	 */
	public static final int RENAME_CAP = 1 << 4;

	/**
	 * Make directory capability.
	 * @since jEdit 2.6pre2
	 */
	public static final int MKDIR_CAP = 1 << 5;

	/**
	 * Low latency capability. If this is not set, then a confirm dialog
	 * will be shown before doing a directory search in this VFS.
	 * @since jEdit 4.1pre1
	 */
	public static final int LOW_LATENCY_CAP = 1 << 6;

	/**
	 * Case insensitive file system capability.
	 * @since jEdit 4.1pre1
	 */
	public static final int CASE_INSENSITIVE_CAP = 1 << 7;

	/**
	 * Sessions created outside Event Dispatching Thread -
	 * file system capability. Set for the file system that does not
	 * require that <code>createVFSSession</code> is called on edt.
	 * All systems that do not implement <code>createVFSSession</code>
	 * should set it, but others may too.
	 * @since jEdit 5.0pre1
	 */
	public static final int NON_AWT_SESSION_CAP = 1 << 8;
	//}}}

	//{{{ Extended attributes
	/**
	 * File type.
	 * @since jEdit 4.2pre1
	 */
	public static final String EA_TYPE = "type";

	/**
	 * File status (read only, read write, etc).
	 * @since jEdit 4.2pre1
	 */
	public static final String EA_STATUS = "status";

	/**
	 * File size.
	 * @since jEdit 4.2pre1
	 */
	public static final String EA_SIZE = "size";

	/**
	 * File last modified date.
	 * @since jEdit 4.2pre1
	 */
	public static final String EA_MODIFIED = "modified";
	//}}}

	public static final int IOBUFSIZE = 32678;

	//{{{ VFS constructors
	/**
	 * Creates a new virtual filesystem.
	 * @param name The name
	 * @param caps The capabilities
	 */
	protected VFS(String name, int caps)
	{
		this.name = name;
		this.caps = caps;
		// reasonable defaults (?)
		extAttrs = new String[] { EA_SIZE, EA_TYPE };
	}

	/**
	 * Creates a new virtual filesystem.
	 * @param name The name
	 * @param caps The capabilities
	 * @param extAttrs The extended attributes
	 * @since jEdit 4.2pre1
	 */
	protected VFS(String name, int caps, String[] extAttrs)
	{
		this.name = name;
		this.caps = caps;
		this.extAttrs = extAttrs;
	} //}}}

	//{{{ getName() method
	/**
	 * Returns this VFS's name. The name is used to obtain the
	 * label stored in the <code>vfs.<i>name</i>.label</code>
	 * property.
	 */
	public String getName()
	{
		return name;
	} //}}}

	//{{{ getCapabilities() method
	/**
	 * Returns the capabilities of this VFS.
	 * @since jEdit 2.6pre2
	 */
	public int getCapabilities()
	{
		return caps;
	} //}}}

	//{{{ isMarkersFileSupported() method
	/**
	 * Returns if an additional markers file can be saved by this VFS.
	 * Default is {@code true}.
	 *
	 * @since jEdit 4.3pre10
	 */
	public boolean isMarkersFileSupported()
	{
		return true;
	} //}}}

	//{{{ getExtendedAttributes() method
	/**
	 * Returns the extended attributes supported by this VFS.
	 * @since jEdit 4.2pre1
	 */
	public String[] getExtendedAttributes()
	{
		return extAttrs;
	} //}}}

	//{{{ getFileName() method
	/**
	 * Returns the file name component of the specified path.
	 * @param path The path
	 * @since jEdit 3.1pre4
	 */
	public String getFileName(String path)
	{
		if(path.equals("/"))
			return path;

		while(path.endsWith("/") || path.endsWith(File.separator))
			path = path.substring(0,path.length() - 1);

		int index = Math.max(path.lastIndexOf('/'),
			path.lastIndexOf(File.separatorChar));
		if(index == -1)
			index = path.indexOf(':');

		// don't want getFileName("roots:") to return ""
		if(index == -1 || index == path.length() - 1)
			return path;

		return path.substring(index + 1);
	} //}}}

	//{{{ getFilePath() method
	/**
	 * Returns the path component of the specified VFS path.
	 * The standard implementation cuts off the protocol
	 * and the protocol separator character and then delegates
	 * to eventually present sub-VFS-paths present in the VFS path
	 * like "jode:archive:/test.zip!/test.txt".
	 * <p>
	 * If a VFS implementation can have additional
	 * information in the VFS path like username / password / host / port
	 * for FTP VFS or archive filename for archive VFS, this
	 * method should be overridden to remove those information also.
	 * The easiest would be to remove those additional information
	 * and then delegate to {@code super.getFilePath()}.
	 *
	 * @param vfsPath The VFS path
	 * @since jEdit 4.5pre1
	 */
	public String getFilePath(String vfsPath)
	{
		if (!MiscUtilities.isURL(vfsPath))
			return vfsPath;

		String filePath = vfsPath.substring(MiscUtilities.getProtocolOfURL(vfsPath).length() + 1);

		return VFSManager.getVFSForPath(filePath).getFilePath(filePath);
	} //}}}

	//{{{ getParentOfPath() method
	/**
	 * Returns the parent of the specified path. This must be
	 * overridden to return a non-null value for browsing of this
	 * filesystem to work.
	 * @param path The path
	 * @since jEdit 2.6pre5
	 */
	@Nonnull
	public String getParentOfPath(String path)
	{
		// ignore last character(s) of path to properly handle
		// paths like /foo/bar/
		int index = MiscUtilities.getLastSeparatorIndex(path, true);
		if(index == -1)
			// this ensures that getFileParent("protocol://"), for
			// example, is "protocol://", otherwise we end up with
			// protocol:
			//   protocol:
			//     file
			return path;

		return path.substring(0,index + 1);
	} //}}}

	//{{{ isRemotePath() method
	/**
	 * Returns whether the specified path should be treated as remote path.
	 * This can have effects like not restoring the file on reopening jEdit eventually
	 * or loading the contents lazily on first view and similar things.
	 * <p>
	 * The default implementation always returns {@code true}.
	 * Subclasses that do not exclusively serve remote files should
	 * overwrite this method and return an appropriate result.
	 * VFS that wrap other VFS paths like for example {@code ArchiveVFS}
	 * should often delegate to the wrapped VFS to determine the result.
	 * <p>
	 * Whether a path should be treated as remote path, depends on multiple factors.
	 * If the path could for example temporarily be inaccessible like an FTP path
	 * when you are offline or an SQL table when the SQL server could be offline,
	 * it should probably be treated as remote path. But a "local" path can also be
	 * inaccessible if a USB stick is detached or a mapped network drive is not available,
	 * but that does not mean also all local paths should be marked as remote files.
	 * So you have to use your common sense to decide when to mark a file as remote.
	 * Another indicator is how long it needs to load a file or fail loading a file.
	 * If the user has ten files open and restarts jEdit with restoring remote files,
	 * and each file takes one minute to load or two minutes if it times out,
	 * then the user waits ten to twenty minutes for jEdit to start up, so the file should
	 * be marked as remote file, so that it is not restored or is loaded lazily.
	 * <p>
	 * Overrides of this method can safely assume that a call to
	 * {@link VFSManager#getVFSForPath} with the specified path would return an instance
	 * of the respective subclass where the method is implemented. Supplying any
	 * other VFS' path as argument here is illegal and leads to undefined behavior.
	 *
	 * @param path The path
	 * @since jEdit 5.8pre1
	 */
	public boolean isRemotePath(String path)
	{
		return true;
	} //}}}

	//{{{ constructPath() method
	/**
	 * Constructs a path from the specified directory and
	 * file name component. This must be overridden to return a
	 * non-null value, otherwise browsing this filesystem will
	 * not work.<p>
	 *
	 * Unless you are writing a VFS, this method should not be called
	 * directly. To ensure correct behavior, you <b>must</b> call
	 * {@link org.gjt.sp.jedit.MiscUtilities#constructPath(String,String)}
	 * instead.
	 *
	 * @param parent The parent directory
	 * @param path The path
	 * @since jEdit 2.6pre2
	 */
	public String constructPath(String parent, String path)
	{
		return parent + path;
	} //}}}

	//{{{ getFileSeparator() method
	/**
	 * Returns the file separator used by this VFS.
	 * @since jEdit 2.6pre9
	 */
	public char getFileSeparator()
	{
		return '/';
	} //}}}

	//{{{ getTwoStageSaveName() method
	/**
	 * Returns a temporary file name based on the given path.
	 *
	 * By default jEdit first saves a file to <code>#<i>name</i>#save#</code>
	 * and then renames it to the original file. However some virtual file
	 * systems might not support the <code>#</code> character in filenames,
	 * so this method permits the VFS to override this behavior.
	 *
	 * If this method returns <code>null</code>, two stage save will not
	 * be used for that particular file (introduced in jEdit 4.3pre1).
	 *
	 * @param path The path name
	 * @since jEdit 4.1pre7
	 */
	@Nullable
	public String getTwoStageSaveName(String path)
	{
		return MiscUtilities.constructPath(getParentOfPath(path),
			'#' + getFileName(path) + "#save#");
	} //}}}

	//{{{ reloadDirectory() method
	/**
	 * Called before a directory is reloaded by the file system browser.
	 * Can be used to flush a cache, etc.
	 * @since jEdit 4.0pre3
	 */
	public void reloadDirectory(String path) {} //}}}

	//{{{ createVFSSession() methods
	/**
	 * Creates a VFS session. This method is called from the AWT thread,
	 * so it should not do any I/O. It could, however, prompt for
	 * a login name and password, for example. A simpler filesystem
	 * may set the <code>NON_AWT_SESSION_CAP</code> capability. When set,
	 * sessions may be obtained from any thread.
	 * @param path The path in question
	 * @param comp The component that will parent any dialog boxes shown
	 * @return The session. The session can be null if there were errors
	 * @since jEdit 2.6pre3
	 */
	public Object createVFSSession(String path, Component comp)
	{
		return new Object();
	}

	/**
	* Same as {@link #createVFSSession}, but may be called from any
	* thread. It first checks the <code>NON_AWT_SESSION_CAP</code>
	* capability and enters EDT thread if necessary.
	*/
	public Object createVFSSessionSafe(final String path,
	                                   final Component comp)
	{
		Object session;
		if ((getCapabilities() & NON_AWT_SESSION_CAP) != 0)
		{
			session = createVFSSession(path, comp);
		}
		else
		{
			SessionGetter getter = new SessionGetter(path, comp);
			ThreadUtilities.runInDispatchThreadAndWait(getter);
			session = getter.get();
		}
		return session;
	}
	//}}}

	//{{{ load() method
	/**
	 * Loads the specified buffer. The default implementation posts
	 * an I/O request to the I/O thread.
	 * @param view The view
	 * @param buffer The buffer
	 * @param path The path
	 * @param untitled is the buffer untitled
	 */
	public boolean load(View view, Buffer buffer, String path, boolean untitled)
	{
		if((getCapabilities() & READ_CAP) == 0)
		{
			VFSManager.error(view,path,"vfs.not-supported.load",new String[] { name });
			return false;
		}

		Object session = createVFSSession(path,view);
		if(session == null)
			return false;

		try
		{
			VFSFile vfsFile = _getFile(session, path, view);
			if ((vfsFile != null) && (vfsFile.getLength() > Integer.MAX_VALUE))
			{
				VFSManager.error(view, path, "ioerror.file-too-big", null);
				return false;
			}
		}
		catch (IOException e)
		{
			// just log the exception here as it was only for the file size check
			// maybe the actual loading succeeds
			Log.log(Log.DEBUG, VFS.class, "Error during file size verification", e);
		}

		if((getCapabilities() & WRITE_CAP) == 0)
			buffer.setReadOnly(true);

		Task request = new BufferLoadRequest(view, buffer, session, this, path, untitled);
		if(buffer.isTemporary())
			// this makes HyperSearch much faster
			request.run();
		else
			// BufferLoadRequest can cause UI interations (for example FTP connection dialog),
			// so it should be runned in Dispatch thread
			//ThreadUtilities.runInDispatchThread(request);
			ThreadUtilities.runInBackground(request);

		return true;
	} //}}}

	//{{{ save() method
	/**
	 * Saves the specifies buffer. The default implementation posts
	 * an I/O request to the I/O thread.
	 * @param view The view
	 * @param buffer The buffer
	 * @param path The path
	 */
	public boolean save(View view, Buffer buffer, String path)
	{
		if((getCapabilities() & WRITE_CAP) == 0)
		{
			VFSManager.error(view,path,"vfs.not-supported.save",new String[] { name });
			return false;
		}

		Object session = createVFSSession(path,view);
		if(session == null)
			return false;

		/* When doing a 'save as', the path to save to (path)
		 * will not be the same as the buffer's previous path
		 * (buffer.getPath()). In that case, we want to create
		 * a backup of the new path, even if the old path was
		 * backed up as well (BACKED_UP property set) */
		if(!path.equals(buffer.getPath()))
			buffer.unsetProperty(Buffer.BACKED_UP);

		ThreadUtilities.runInBackground(new BufferSaveRequest(
			view,buffer,session,this,path));
		return true;
	} //}}}

	//{{{ copy() methods
	/**
	 * Copy a file to another using VFS.
	 *
	 * @param progress the progress observer. It could be null if you don't want to monitor progress. If not null
	 *                  you should probably launch this command in a WorkThread
	 * @param sourceVFS the source VFS
	 * @param sourceSession the VFS session
	 * @param sourcePath the source path. It must be a file and must exists
	 * @param targetVFS the target VFS
	 * @param targetSession the target session
	 * @param targetPath the target path.
	 * If it is a path, it must exists, if it is a file, the parent must
	 * exists
	 * @param comp The component that will parent error dialog boxes
	 * @param canStop could this copy be stopped ?
	 * @return true if the copy was successful
	 * @throws IOException  IOException If an I/O error occurs
	 * @since jEdit 4.3pre3
	 */
	public static boolean copy(ProgressObserver progress, VFS sourceVFS, Object sourceSession,String sourcePath,
				   VFS targetVFS, Object targetSession,String targetPath, Component comp, boolean canStop)
		throws IOException
	{
		return copy(progress, sourceVFS, sourceSession, sourcePath, targetVFS, targetSession, targetPath,
			    comp, canStop, true);
	}

	/**
	 * Copy a file to another using VFS.
	 *
	 * @param progress the progress observer. It could be null if you don't want to monitor progress. If not null
	 *                  you should probably launch this command in a WorkThread
	 * @param sourceVFS the source VFS
	 * @param sourceSession the VFS session
	 * @param sourcePath the source path. It must be a file and must exists
	 * @param targetVFS the target VFS
	 * @param targetSession the target session
	 * @param targetPath the target path.
	 * If it is a path, it must exists, if it is a file, the parent must
	 * exists
	 * @param comp The component that will parent error dialog boxes
	 * @param canStop could this copy be stopped ?
	 * @param sendVFSUpdate true if you want to send a VFS update after the copy. False otherwise (if you do a lot
	 *                      of copy)
	 * @return true if the copy was successful
	 * @throws IOException  IOException If an I/O error occurs
	 * @since jEdit 5.0
	 */
	public static boolean copy(ProgressObserver progress, VFS sourceVFS, Object sourceSession,String sourcePath,
		VFS targetVFS, Object targetSession,String targetPath, Component comp, boolean canStop,
		boolean sendVFSUpdate)
	throws IOException
	{
		if (sourcePath.equals(targetPath))
		{
			Log.log(Log.WARNING, VFS.class,
				jEdit.getProperty("ioerror.copy-self",
					new Object[] { sourcePath }));
			return false;
		}
		if (progress != null)
			progress.setStatus("Initializing");

		InputStream in = null;
		OutputStream out = null;
		try
		{
			VFSFile sourceVFSFile = sourceVFS._getFile(sourceSession, sourcePath, comp);
			if (sourceVFSFile == null)
				throw new FileNotFoundException("source path " + sourcePath + " doesn't exists");
			if (progress != null)
			{
				progress.setMaximum(sourceVFSFile.getLength());
			}
			VFSFile targetVFSFile = targetVFS._getFile(targetSession, targetPath, comp);
			if (targetVFSFile == null)
			{
				String parentTargetPath = MiscUtilities.getParentOfPath(targetPath);
				VFSFile parentTargetVFSFile = targetVFS._getFile(targetSession, parentTargetPath, comp);
				if (parentTargetVFSFile == null)
					throw new FileNotFoundException("target path " + parentTargetPath +
						" doesn't exists");
				if (parentTargetVFSFile.getType() == VFSFile.DIRECTORY)
				{
					String targetFilename = MiscUtilities.getFileName(targetPath);
					targetPath = MiscUtilities.constructPath(parentTargetPath, targetFilename);
				}
				else
				{
					throw new IOException("The parent of target path is a file");
				}
			}
			else if (targetVFSFile.getType() == VFSFile.DIRECTORY)
			{
				if (targetVFSFile.getPath().equals(sourceVFSFile.getPath()))
					return false;
				targetPath = MiscUtilities.constructPath(targetPath, sourceVFSFile.getName());
			}
			in = new BufferedInputStream(sourceVFS._createInputStream(sourceSession, sourcePath, false, comp));
			out = new BufferedOutputStream(targetVFS._createOutputStream(targetSession, targetPath, comp));
			boolean copyResult = IOUtilities.copyStream(IOBUFSIZE, progress, in, out, canStop);
			if (sendVFSUpdate)
				VFSManager.sendVFSUpdate(targetVFS, targetPath, true);
			return copyResult;
		}
		finally
		{
			IOUtilities.closeQuietly((Closeable)in);
			IOUtilities.closeQuietly((Closeable)out);
		}
	}

	/**
	 * Copy a file to another using VFS.
	 *
	 * @param progress the progress observer. It could be null if you don't want to monitor progress. If not null
	 *                  you should probably launch this command in a WorkThread
	 * @param sourcePath the source path
	 * @param targetPath the target path
	 * @param comp The component that will parent error dialog boxes
	 * @param canStop if true the copy can be stopped
	 * @param sendVFSUpdate true if you want to send a VFS update after the copy. False otherwise (if you do a lot
	 *                      of copy)
	 * @return true if the copy was successful
	 * @throws IOException IOException If an I/O error occurs
	 * @since jEdit 5.0
	 */
	public static boolean copy(ProgressObserver progress, String sourcePath,String targetPath, Component comp,
				   boolean canStop, boolean sendVFSUpdate)
		throws IOException
	{
		VFS sourceVFS = VFSManager.getVFSForPath(sourcePath);
		VFS targetVFS = VFSManager.getVFSForPath(targetPath);
		Object sourceSession = null;
		Object targetSession = null;
		try
		{
			sourceSession = sourceVFS.createVFSSession(sourcePath, comp);
			if (sourceSession == null)
			{
				Log.log(Log.WARNING, VFS.class, "Unable to get a valid session from " + sourceVFS +
												" for path " + sourcePath);
				return false;
			}
			targetSession = targetVFS.createVFSSession(targetPath, comp);
			if (targetSession == null)
			{
				Log.log(Log.WARNING, VFS.class, "Unable to get a valid session from " + targetVFS +
												" for path " + targetPath);
				return false;
			}
			return copy(progress, sourceVFS, sourceSession, sourcePath, targetVFS, targetSession, targetPath,
						comp,canStop, sendVFSUpdate);
		}
		finally
		{
			if (sourceSession != null)
				sourceVFS._endVFSSession(sourceSession, comp);
			if (targetSession != null)
				targetVFS._endVFSSession(targetSession, comp);
		}
	}

	/**
	 * Copy a file to another using VFS.
	 *
	 * @param progress the progress observer. It could be null if you don't want to monitor progress. If not null
	 *                  you should probably launch this command in a WorkThread
	 * @param sourcePath the source path
	 * @param targetPath the target path
	 * @param comp The component that will parent error dialog boxes
	 * @param canStop if true the copy can be stopped
	 * @return true if the copy was successful
	 * @throws IOException IOException If an I/O error occurs
	 * @since jEdit 4.3pre3
	 */
	public static boolean copy(ProgressObserver progress, String sourcePath,String targetPath, Component comp, boolean canStop)
	throws IOException
	{
		return copy(progress, sourcePath, targetPath, comp, canStop, true);
	} //}}}

	//{{{ insert() method
	/**
	 * Inserts a file into the specified buffer. The default implementation
	 * posts an I/O request to the I/O thread.
	 * @param view The view
	 * @param buffer The buffer
	 * @param path The path
	 */
	public boolean insert(View view, Buffer buffer, String path)
	{
		if((getCapabilities() & READ_CAP) == 0)
		{
			VFSManager.error(view,path,"vfs.not-supported.load",new String[] { name });
			return false;
		}

		Object session = createVFSSession(path,view);
		if(session == null)
			return false;

		ThreadUtilities.runInBackground(new BufferInsertRequest(
			view,buffer,session,this,path));
		return true;
	} //}}}

	// A method name that starts with _ requires a session object

	//{{{ _canonPath() method
	/**
	 * Returns the canonical form of the specified path name. For example,
	 * <code>~</code> might be expanded to the user's home directory.
	 * @param session The session
	 * @param path The path
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException if an I/O error occurred
	 * @since jEdit 4.0pre2
	 */
	public String _canonPath(Object session, String path, Component comp)
		throws IOException
	{
		return path;
	} //}}}

	//{{{ _listDirectory() method
	/**
	 * A convenience method that matches file names against globs, and can
	 * optionally list the directory recursively.
	 * @param session The session
	 * @param directory The directory. Note that this must be a full
	 * URL, including the host name, path name, and so on. The
	 * username and password (if needed by the VFS) is obtained from the
	 * session instance.
	 * @param glob Only file names matching this glob will be returned
	 * @param recursive If true, subdirectories will also be listed.
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException if an I/O error occurred
	 * @since jEdit 4.1pre1
	 */
	public String[] _listDirectory(Object session, String directory,
		String glob, boolean recursive, Component comp )
		throws IOException
	{
		String[] retval = _listDirectory(session, directory, glob, recursive, comp, true, false);
		return retval;
	} //}}}


	//{{{ _listDirectory() method
	/**
	 * A convenience method that matches file names against globs, and can
	 * optionally list the directory recursively.
	 * @param session The session
	 * @param directory The directory. Note that this must be a full
	 * URL, including the host name, path name, and so on. The
	 * username and password (if needed by the VFS) is obtained from the
	 * session instance.
	 * @param glob Only file names matching this glob will be returned
	 * @param recursive If true, subdirectories will also be listed.
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException if an I/O error occurred
	 * @param skipBinary ignore binary files (do not return them).
	 *    This will slow down the process since it will open the files
	 * @param skipHidden skips hidden files, directories, and
	 *        backup files. Ignores any file beginning with . or #, or ending with ~
	 *        or .bak
	 *
	 *
	 * @since jEdit 4.3pre5
	 */
	public String[] _listDirectory(Object session, String directory,
		String glob, boolean recursive, Component comp,
		boolean skipBinary, boolean skipHidden)
		throws IOException
	{
		VFSFileFilter filter = new GlobVFSFileFilter(glob);
		return _listDirectory(session, directory, filter,
				      recursive, comp, skipBinary,
				      skipHidden);
	} //}}}

	//{{{ _listDirectory() method
	/**
	 * A convenience method that filters the directory listing
	 * according to a filter, and can optionally list the directory
	 * recursively.
	 * @param session The session
	 * @param directory The directory. Note that this must be a full
	 * URL, including the host name, path name, and so on. The
	 * username and password (if needed by the VFS) is obtained from the
	 * session instance.
	 * @param filter The {@link VFSFileFilter} to use for filtering.
	 * @param recursive If true, subdirectories will also be listed.
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException if an I/O error occurred
	 * @param skipBinary ignore binary files (do not return them).
	 *    This will slow down the process since it will open the files
	 * @param skipHidden skips hidden files, directories, and
	 *        backup files. Ignores any file beginning with . or #, or ending with ~
	 *        or .bak
	 *
	 * @since jEdit 4.3pre7
	 */
	public String[] _listDirectory(Object session, String directory,
		VFSFileFilter filter, boolean recursive, Component comp,
		boolean skipBinary, boolean skipHidden)
		throws IOException
	{
		List<String> files = new ArrayList<>(100);

		listFiles(session,new HashSet<>(), files,directory,filter,
			recursive, comp, skipBinary, skipHidden);

		String[] retVal = files.toArray(StandardUtilities.EMPTY_STRING_ARRAY);

		Arrays.sort(retVal,new StandardUtilities.StringCompare<>(true));

		return retVal;
	} //}}}

	//{{{ _listFiles() method
	/**
	 * Lists the specified directory.
	 * @param session The session
	 * @param directory The directory. Note that this must be a full
	 * URL, including the host name, path name, and so on. The
	 * username and password (if needed by the VFS) is obtained from the
	 * session instance.
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException if an I/O error occurred
	 * @since jEdit 4.3pre2
	 */
	public VFSFile[] _listFiles(Object session, String directory,
		Component comp)
		throws IOException
	{
		VFSManager.error(comp,directory,"vfs.not-supported.list",new String[] { name });
		return null;
	} //}}}

	//{{{ _getFile() method
	/**
	 * Returns the specified directory entry.
	 * @param session The session get it with {@link VFS#createVFSSession(String, Component)}
	 * @param path The path
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException if an I/O error occurred
	 * @return The specified directory entry, or null if it doesn't exist.
	 * @since jEdit 4.3pre2
	 */
	@Nullable
	public VFSFile _getFile(Object session, String path, Component comp) throws IOException
	{
		return null;
	} //}}}

	//{{{ _delete() method
	/**
	 * Deletes the specified URL.
	 * @param session The VFS session
	 * @param path The path
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException if an I/O error occurs
	 * @since jEdit 2.7pre1
	 */
	public boolean _delete(Object session, String path, Component comp)
		throws IOException
	{
		return false;
	} //}}}

	//{{{ _rename() method
	/**
	 * Renames the specified URL. Some filesystems might support moving
	 * URLs between directories, however others may not. Do not rely on
	 * this behavior.
	 * @param session The VFS session
	 * @param from The old path
	 * @param to The new path
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException if an I/O error occurs
	 * @since jEdit 2.7pre1
	 */
	public boolean _rename(Object session, String from, String to,
		Component comp) throws IOException
	{
		return false;
	} //}}}

	//{{{ _mkdir() method
	/**
	 * Creates a new directory with the specified URL.
	 * @param session The VFS session
	 * @param directory The directory
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException if an I/O error occurs
	 * @since jEdit 2.7pre1
	 */
	public boolean _mkdir(Object session, String directory, Component comp)
		throws IOException
	{
		return false;
	} //}}}

	//{{{ _backup() method
	/**
	 * Backs up the specified file. Default implementation in 5.0pre1
	 * copies the file to the backup directory. Before 5.0pre1 it was
	 * empty.
	 * @param session The VFS session
	 * @param path The path
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException if an I/O error occurs
	 * @since jEdit 3.2pre2
	 */
	public void _backup(Object session, String path, Component comp)
		throws IOException
	{
		VFS vfsSrc = VFSManager.getVFSForPath(path);
		if (null == vfsSrc._getFile(session, path, comp))
			// file to backup does not exist
			return;

		// maybe the file is not applicable to local filesystem
		// but don't worry - for backup purposes it may be out
		// of a real filesystem
		File backupDir = MiscUtilities.prepareBackupDirectory(path);
		if (backupDir == null)
		{
			Log.log(Log.WARNING, VFS.class, "Backup of remote file "
				+ path + " failed, because there is no backup directory.");
			return;
		}
		if (!backupDir.exists())
		{
			// Usually that means there is no specified backup
			// directory.
			Log.log(Log.WARNING, VFS.class, "Backup of file " +
				path + " failed. Directory " + backupDir +
				" does not exist.");
			return;
		}

		File backupFile = MiscUtilities.prepareBackupFile(path, backupDir);
		if (backupFile == null)
		{
			return;
		}

		// do copy using VFS.copy
		VFS vfsDst = VFSManager.getVFSForPath(backupFile.getPath());
		Object sessionDst = vfsDst.createVFSSessionSafe(
				backupFile.getPath(), comp);
		if (sessionDst == null)
			{
			return;
			}
		try
		{
			if (!copy(null, vfsSrc, session, path,
				vfsDst, sessionDst, backupFile.getPath(),
				comp, true))
			{
				Log.log(Log.WARNING, VFS.class, "Backup of file " +
					path + " failed. Copy to " + backupFile +
					" failed.");
			}
		}
		finally
		{
			vfsDst._endVFSSession(sessionDst, comp);
		}

	} //}}}

	//{{{ _createInputStream() method
	/**
	 * Creates an input stream. This method is called from the I/O
	 * thread.
	 * @param session the VFS session
	 * @param path The path
	 * @param ignoreErrors If true, file not found errors should be
	 * ignored
	 * @param comp The component that will parent error dialog boxes
	 * @return an inputstream or <code>null</code> if there was a problem
	 * @exception IOException If an I/O error occurs
	 * @since jEdit 2.7pre1
	 */
	public InputStream _createInputStream(Object session,
		String path, boolean ignoreErrors, Component comp)
		throws IOException
	{
		VFSManager.error(comp,path,"vfs.not-supported.load",new String[] { name });
		return null;
	} //}}}

	//{{{ _createOutputStream() method
	/**
	 * Creates an output stream. This method is called from the I/O
	 * thread.
	 * @param session the VFS session
	 * @param path The path
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException If an I/O error occurs
	 * @since jEdit 2.7pre1
	 */
	public OutputStream _createOutputStream(Object session,
		String path, Component comp)
		throws IOException
	{
		VFSManager.error(comp,path,"vfs.not-supported.save",new String[] { name });
		return null;
	} //}}}

	//{{{ _saveComplete() method
	/**
	 * Called after a file has been saved.
	 * @param session The VFS session
	 * @param buffer The buffer
	 * @param path The path the buffer was saved to (can be different from
	 * {@link org.gjt.sp.jedit.Buffer#getPath()} if the user invoked the
	 * <b>Save a Copy As</b> command, for example).
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException If an I/O error occurs
	 * @since jEdit 4.1pre9
	 */
	public void _saveComplete(Object session, Buffer buffer, String path,
		Component comp) throws IOException {} //}}}

	//{{{ _finishTwoStageSave() method
	/**
	 * Called after a file has been saved and we use twoStageSave (first saving to
	 * another file). This should re-apply permissions for example.

	 * @param session The VFS session
	 * @param buffer The buffer
	 * @param path The path the buffer was saved to (can be different from
	 * {@link org.gjt.sp.jedit.Buffer#getPath()} if the user invoked the
	 * <b>Save a Copy As</b> command, for example).
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException If an I/O error occurs
	 * @since jEdit 4.3pre4
	 */
	public void _finishTwoStageSave(Object session, Buffer buffer, String path,
		Component comp) throws IOException
	{
	} //}}}

	//{{{ _endVFSSession() method
	/**
	 * Finishes the specified VFS session. This must be called
	 * after all I/O with this VFS is complete, to avoid leaving
	 * stale network connections and such.
	 * @param session The VFS session
	 * @param comp The component that will parent error dialog boxes
	 * @exception IOException if an I/O error occurred
	 * @since jEdit 2.7pre1
	 */
	public void _endVFSSession(Object session, Component comp)
		throws IOException
	{
	} //}}}

	//{{{ getDefaultColorFor() method
	/**
	 * Returns color of the specified file name, by matching it against
	 * user-specified regular expressions.
	 * @since jEdit 4.0pre1
	 */
	public static Color getDefaultColorFor(String name)
	{
		synchronized(lock)
		{
			if(colors == null)
				loadColors();

			for (ColorEntry entry : colors)
			{
				if (entry.getRe().matcher(name).matches())
					return entry.getColor();
			}

			return null;
		}
	} //}}}

	//{{{ DirectoryEntryCompare class
	/**
	 * Implementation of {@link Comparator}
	 * interface that compares {@link VFSFile} instances.
	 * @since jEdit 4.2pre1
	 */
	public static class DirectoryEntryCompare implements Comparator<VFSFile>
	{
		private final boolean sortIgnoreCase;
		private final boolean sortMixFilesAndDirs;

		/**
		 * Creates a new <code>DirectoryEntryCompare</code>.
		 * @param sortMixFilesAndDirs If false, directories are
		 * put at the top of the listing.
		 * @param sortIgnoreCase If false, upper case comes before
		 * lower case.
		 */
		public DirectoryEntryCompare(boolean sortMixFilesAndDirs,
			boolean sortIgnoreCase)
		{
			this.sortMixFilesAndDirs = sortMixFilesAndDirs;
			this.sortIgnoreCase = sortIgnoreCase;
		}

		@Override
		public int compare(VFSFile file1, VFSFile file2)
		{
			if(!sortMixFilesAndDirs)
			{
				if(file1.getType() != file2.getType())
					return file2.getType() - file1.getType();
			}

			return StandardUtilities.compareStrings(file1.getName(),
				file2.getName(),sortIgnoreCase);
		}
	} //}}}

	//{{{ Private members
	private final String name;
	private final int caps;
	private final String[] extAttrs;
	private static List<ColorEntry> colors;
	private static final Object lock = new Object();

	//{{{ Class initializer
	static
	{
		EditBus.addToBus(new EBComponent()
		{
			@Override
			public void handleMessage(EBMessage msg)
			{
				if(msg instanceof PropertiesChanged)
				{
					synchronized(lock)
					{
						colors = null;
					}
				}
			}
		});
	} //}}}

	//{{{ recursive listFiles() method
	private void listFiles(Object session, Collection<String> stack,
		List<String> files, String directory, VFSFileFilter filter, boolean recursive,
		Component comp, boolean skipBinary, boolean skipHidden) throws IOException
	{
		if (recursive && !MiscUtilities.isURL(directory))
		{
			String resolvedPath = MiscUtilities.resolveSymlinks(directory);
			/*
			 * If looking at a symlink, do not traverse the
			 * resolved path more than once.
			 */
			if (!directory.equals(resolvedPath))
			{
				if (stack.contains(resolvedPath))
				{
					Log.log(Log.ERROR,this,
						"Recursion in listFiles(): "
						+ directory);
					return;
				}
				stack.add(resolvedPath);
			}
		}

		VFSFile[] _files = _listFiles(session,directory,
			comp);
		if(_files == null || _files.length == 0)
			return;

		for(int i = 0; i < _files.length; i++)
		{
			if (Thread.currentThread().isInterrupted())
				break;
			VFSFile file = _files[i];
			if (skipHidden && (file.isHidden() || MiscUtilities.isBackup(file.getName())))
				continue;
			if(!filter.accept(file))
				continue;
			if(file.getType() == VFSFile.DIRECTORY
				|| file.getType() == VFSFile.FILESYSTEM)
			{
				if(recursive)
				{
					String canonPath = _canonPath(session,
						file.getPath(),comp);
					listFiles(session,stack,files,
						canonPath,filter,recursive,
						comp, skipBinary, skipHidden);
				}
			}
			else // It's a regular file
			{
				if (skipBinary)
				{
					try
					{
						if (file.isBinary(session))
						{
							Log.log(Log.NOTICE,this
								,file.getPath() + ": skipped as a binary file");
							continue;
						}
					}
					catch(IOException e)
					{
						Log.log(Log.ERROR,this,e);
						// may be not binary...
					}
				}
				files.add(file.getPath());
			}
		}
	} //}}}

	//{{{ loadColors() method
	private static void loadColors()
	{
		synchronized(lock)
		{
			colors = new ArrayList<>();

			if(!jEdit.getBooleanProperty("vfs.browser.colorize"))
				return;

			String glob;
			int i = 0;
			while((glob = jEdit.getProperty("vfs.browser.colors." + i + ".glob")) != null)
			{
				try
				{
					colors.add(new ColorEntry(
						Pattern.compile(StandardUtilities.globToRE(glob)),
						jEdit.getColorProperty(
						"vfs.browser.colors." + i + ".color",
						Color.black)));
				}
				catch(PatternSyntaxException e)
				{
					Log.log(Log.ERROR,VFS.class,"Invalid regular expression: " + glob);
					Log.log(Log.ERROR,VFS.class,e);
				}

				i++;
			}
		}
	} //}}}

	//{{{ ColorEntry class
	private static class ColorEntry
	{
		private final Pattern re;
		private final Color color;

		ColorEntry(Pattern re, Color color)
		{
			this.re = re;
			this.color = color;
		}

		public Pattern getRe()
		{
			return re;
		}

		public Color getColor()
		{
			return color;
		}
	} //}}}

	//{{{ SessionGetter class
	private class SessionGetter implements Runnable
	{
		SessionGetter(String path, Component comp)
		{
			this.path = path;
			this.comp = comp;
		}

		private Object session;
		private final String path;
		private final Component comp;

		@Override
		public void run()
		{
			session = createVFSSession(path, comp);
		}

		public Object get() { return session; }
	} //}}}

	//}}}
}
