/*
 * VFSBrowser.java - VFS browser
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

package org.gjt.sp.jedit.browser;

import gnu.regexp.REException;
import javax.swing.border.EmptyBorder;
import javax.swing.event.EventListenerList;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

/**
 * The main class of the VFS browser.
 * @author Slava Pestov
 * @version $Id$
 */
public class VFSBrowser extends JPanel implements EBComponent
{
	/**
	 * Open file dialog mode. Equals JFileChooser.OPEN_DIALOG for
	 * backwards compatibility.
	 */
	public static final int OPEN_DIALOG = 0;

	/**
	 * Save file dialog mode. Equals JFileChooser.SAVE_DIALOG for
	 * backwards compatibility.
	 */
	public static final int SAVE_DIALOG = 1;

	/**
	 * Stand-alone browser mode.
	 */
	public static final int BROWSER = 2;

	/**
	 * Creates a new VFS browser.
	 * @param view The view to open buffers in by default
	 * @param path The path to display
	 * @param mode The browser mode
	 * @param multipleSelection True if multiple selection should be allowed
	 */
	public VFSBrowser(View view, String path, int mode, boolean multipleSelection)
	{
		super(new BorderLayout());

		listenerList = new EventListenerList();

		this.mode = mode;
		this.multipleSelection = multipleSelection;
		this.view = view;

		Box topBox = new Box(BoxLayout.Y_AXIS);
		topBox.add(createToolBar());

		GridBagLayout layout = new GridBagLayout();
		JPanel pathAndFilterPanel = new JPanel(layout);

		GridBagConstraints cons = new GridBagConstraints();
		cons.gridwidth = cons.gridheight = 1;
		cons.gridx = cons.gridy = 0;
		cons.fill = GridBagConstraints.BOTH;
		JLabel label = new JLabel(jEdit.getProperty("vfs.browser.directory"),
			SwingConstants.RIGHT);
		label.setBorder(new EmptyBorder(0,0,0,12));
		layout.setConstraints(label,cons);
		pathAndFilterPanel.add(label);

		pathField = new HistoryTextField("vfs.browser.path",true);
		pathField.addActionListener(new ActionHandler());
		cons.gridx = 1;
		cons.weightx = 1.0f;
		layout.setConstraints(pathField,cons);
		pathAndFilterPanel.add(pathField);

		label = new JLabel(jEdit.getProperty("vfs.browser.filter"),
			SwingConstants.RIGHT);
		label.setBorder(new EmptyBorder(0,0,0,12));
		cons.gridx = 0;
		cons.weightx = 0.0f;
		cons.gridy = 1;
		layout.setConstraints(label,cons);
		pathAndFilterPanel.add(label);

		filterCombo = new JComboBox();
		filterCombo.addActionListener(new ActionHandler());
		cons.gridx = 1;
		cons.weightx = 1.0f;
		layout.setConstraints(filterCombo,cons);
		pathAndFilterPanel.add(filterCombo);

		topBox.add(pathAndFilterPanel);
		add(BorderLayout.NORTH,topBox);

		browserView = new BrowserListView(this);
		add(BorderLayout.CENTER,browserView);

		propertiesChanged();

		vfsSession = new VFSSession();

		if(path == null)
		{
			String defaultPath = jEdit.getProperty("vfs.browser.defaultPath");
			String userHome = System.getProperty("user.home");
			if(defaultPath.equals("home"))
				path = userHome;
			else if(defaultPath.equals("buffer"))
			{
				if(view != null)
				{
					Buffer buffer = view.getBuffer();
					path = buffer.getVFS().getFileParent(
						buffer.getPath());
				}
				else
					path = userHome;
			}
			else if(defaultPath.equals("last"))
			{
				HistoryModel model = HistoryModel.getModel("vfs.browser.path");
				if(model.getSize() == 0)
					path = userHome;
				else
					path = model.getItem(0);
			}
			else
			{
				// unknown value??!!!
				path = userHome;
			}
		}

		setDirectory(path);
	}

	public void addNotify()
	{
		super.addNotify();
		EditBus.addToBus(this);
	}

	public void removeNotify()
	{
		super.removeNotify();
		EditBus.removeFromBus(this);
	}

	public void handleMessage(EBMessage msg)
	{
		if(msg instanceof ViewUpdate)
			handleViewUpdate((ViewUpdate)msg);
		else if(msg instanceof BufferUpdate)
			handleBufferUpdate((BufferUpdate)msg);
		else if(msg instanceof PropertiesChanged)
			propertiesChanged();
	}

	public String getDirectory()
	{
		return path;
	}

	public void setDirectory(String path)
	{
		this.path = path;
		if(MiscUtilities.isURL(path))
		{
			vfs = VFSManager.getVFSForProtocol(MiscUtilities
				.getFileProtocol(path));
		}
		else
			vfs = VFSManager.getFileVFS();

		vfsSession.put(VFSSession.PATH_KEY,path);

		pathField.setText(path);
		pathField.addCurrentToHistory();

		reloadDirectory(false);
	}

	public void reloadDirectory(boolean flushCache)
	{
		if(flushCache)
			DirectoryCache.flushCachedDirectory(path);

		if(!vfs.setupVFSSession(vfsSession,this))
			return;

		VFSManager.runInWorkThread(new BrowserIORequest(
			BrowserIORequest.LIST_DIRECTORY,this,
			vfsSession,vfs,path));
	}

	public VFS getVFS()
	{
		return vfs;
	}

	public VFSSession getVFSSession()
	{
		return vfsSession;
	}

	public int getMode()
	{
		return mode;
	}

	public boolean isMultipleSelectionEnabled()
	{
		return multipleSelection;
	}

	public boolean getShowHiddenFiles()
	{
		return showHiddenFiles;
	}

	public void setShowHiddenFiles(boolean showHiddenFiles)
	{
		this.showHiddenFiles = showHiddenFiles;
	}

	public VFSFilter getFilenameFilter()
	{
		return filenameFilter;
	}

	public void setFilenameFilter(VFSFilter filenameFilter)
	{
		this.filenameFilter = filenameFilter;
	}

	public VFS.DirectoryEntry[] getSelectedFiles()
	{
		return browserView.getSelectedFiles();
	}

	public void addBrowserListener(BrowserListener l)
	{
		listenerList.add(BrowserListener.class,l);
	}

	public void removeBrowserListener(BrowserListener l)
	{
		listenerList.remove(BrowserListener.class,l);
	}

	// package-private members
	void directoryLoaded(final VFS.DirectoryEntry[] list)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				if(list == null)
					return;

				Vector directoryVector = new Vector();
				for(int i = 0; i < list.length; i++)
				{
					VFS.DirectoryEntry file = list[i];
					if(file.hidden && !showHiddenFiles)
						continue;
					if(file.type == VFS.DirectoryEntry.FILE
						&& !filenameFilter.accept(file.name))
						continue;
					directoryVector.addElement(file);
				}

				if(sortFiles)
				{
					MiscUtilities.quicksort(directoryVector,
						new FileCompare());
				}

				browserView.directoryLoaded(path,directoryVector);
			}
		});
	}

	class FileCompare implements MiscUtilities.Compare
	{
		public int compare(Object obj1, Object obj2)
		{
			VFS.DirectoryEntry file1 = (VFS.DirectoryEntry)obj1;
			VFS.DirectoryEntry file2 = (VFS.DirectoryEntry)obj2;

			if(!sortMixFilesAndDirs)
			{
				if(file1.type != file2.type)
					return file2.type - file1.type;
			}

			if(sortIgnoreCase)
			{
				return file1.name.toLowerCase().compareTo(
					file2.name.toLowerCase());
			}
			else
			{
				return file1.name.compareTo(file2.name);
			}
		}
	}

	void filesSelected()
	{
		VFS.DirectoryEntry[] selectedFiles = browserView.getSelectedFiles();

		if(mode == BROWSER)
		{
			for(int i = 0; i < selectedFiles.length; i++)
			{
				VFS.DirectoryEntry file = selectedFiles[i];
				Buffer buffer = jEdit.getBuffer(file.path);
				if(buffer != null && view != null)
					view.setBuffer(buffer);
			}
		}

		Object[] listeners = listenerList.getListenerList();
		for(int i = 0; i < listeners.length; i++)
		{
			if(listeners[i] == BrowserListener.class)
			{
				BrowserListener l = (BrowserListener)listeners[i+1];
				l.filesSelected(this,selectedFiles);
			}
		}

	}

	void filesActivated()
	{
		VFS.DirectoryEntry[] selectedFiles = browserView.getSelectedFiles();

		for(int i = 0; i < selectedFiles.length; i++)
		{
			VFS.DirectoryEntry file = selectedFiles[i];

			if(file.type == VFS.DirectoryEntry.DIRECTORY
				|| file.type == VFS.DirectoryEntry.FILESYSTEM)
				setDirectory(file.path);
			else if(mode == BROWSER)
			{
				Buffer buffer = jEdit.getBuffer(file.path);
				if(buffer != null)
				{
					if(jEdit.getBooleanProperty("vfs.browser.doubleClickClose"))
					{
						jEdit.closeBuffer(view,buffer);
						continue;
					}
				}
				else
				{
					Hashtable props = new Hashtable();
					props.put(Buffer.VFS_SESSION_HACK,vfsSession);
					buffer = jEdit.openFile(null,null,file.path,
						false,false,props);
				}

				if(buffer != null)
				{
					if(view == null)
						view = jEdit.newView(null,buffer);
					else
						view.setBuffer(buffer);
				}
			}
			else
			{
				// if a file is selected in OPEN_DIALOG or
				// SAVE_DIALOG mode, just let the listener(s)
				// handle it
			}
		}

		Object[] listeners = listenerList.getListenerList();
		for(int i = 0; i < listeners.length; i++)
		{
			if(listeners[i] == BrowserListener.class)
			{
				BrowserListener l = (BrowserListener)listeners[i+1];
				l.filesActivated(this,selectedFiles);
			}
		}

	}

	// private members
	private static VFSFilter allFilter;
	private static Vector filters = new Vector();

	private EventListenerList listenerList;
	private View view;
	private String path;
	private VFS vfs;
	private VFSSession vfsSession;
	private HistoryTextField pathField;
	private JComboBox filterCombo;
	private JButton up, reload, roots, home, synchronize,
		addToFavorites, gotoFavorites;
	private BrowserView browserView;
	private VFSFilter filenameFilter;
	private int mode;
	private boolean multipleSelection;

	private boolean showHiddenFiles;
	private boolean sortFiles;
	private boolean sortMixFilesAndDirs;
	private boolean sortIgnoreCase;

	static
	{
		try
		{
			allFilter = new VFSFilter(jEdit.getProperty(
				"vfs.browser.filter.all"),"*");
		}
		catch(REException re)
		{
			throw new InternalError("* isn't a valid glob!!??");
		}

		EditBus.addToBus(new EBComponent()
		{
			public void handleMessage(EBMessage msg)
			{
				if(msg instanceof PropertiesChanged)
					updateFilters();
			}
		});

		updateFilters();
	}

	private static void updateFilters()
	{
		// recreate file filters
		filters.removeAllElements();

		filters.addElement(allFilter);

		// Create mode filename filters
		Mode[] modes = jEdit.getModes();
		for(int i = 0; i < modes.length; i++)
		{
			Mode mode = modes[i];
			String label = (String)mode.getProperty("label");
			String glob = (String)mode.getProperty("filenameGlob");
			if(label == null || glob == null)
				continue;

			try
			{
				filters.addElement(new VFSFilter(label,glob));
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,VFSBrowser.class,
					"Invalid file filter: " + glob);
				Log.log(Log.ERROR,VFSBrowser.class,e);
			}
		}

		// Load custom file filters
		int i = 0;
		String name;

		while((name = jEdit.getProperty("filefilter." + i + ".name")) != null
			&& name.length() != 0)
		{
			String glob = jEdit.getProperty("filefilter." + i + ".re");
			try
			{
				filters.addElement(new VFSFilter(name,glob));
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,VFSBrowser.class,
					"Invalid file filter: " + glob);
				Log.log(Log.ERROR,VFSBrowser.class,e);
			}

			i++;
		}
	}

	private JToolBar createToolBar()
	{
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);

		toolBar.add(up = createToolButton("up",false));
		toolBar.add(reload = createToolButton("reload",false));
		toolBar.addSeparator();
		toolBar.add(roots = createToolButton("roots",false));
		toolBar.add(home = createToolButton("home",false));
		toolBar.add(synchronize = createToolButton("synchronize",false));
		toolBar.addSeparator();
		toolBar.add(addToFavorites = createToolButton("addToFavorites",false));
		toolBar.add(gotoFavorites = createToolButton("gotoFavorites",false));
		toolBar.addSeparator();

		Enumeration enum = VFSManager.getFilesystems();
		while(enum.hasMoreElements())
		{
			VFS vfs = (VFS)enum.nextElement();

			if((vfs.getCapabilities() & VFS.BROWSE_CAP) == 0)
				continue;

			toolBar.add(createToolButton(vfs.getName(),true));
		}

		toolBar.add(Box.createGlue());
		return toolBar;
	}

	private JButton createToolButton(String name, boolean vfs)
	{
		JButton button = new JButton();
		String prefix = (vfs ? "vfs." : "vfs.browser.");

		button.setIcon(GUIUtilities.loadToolBarIcon(jEdit.getProperty(
			prefix + name + ".icon")));
		String label = jEdit.getProperty(prefix + name + ".label");
		if(vfs)
			button.setText(label);
		else
			button.setToolTipText(label);
		button.setRequestFocusEnabled(false);
		button.setMargin(new Insets(0,0,0,0));
		button.setActionCommand(name); // so that VFS buttons will work
		button.addActionListener(new ActionHandler());

		return button;
	}

	private void handleViewUpdate(ViewUpdate vmsg)
	{
		if(vmsg.getWhat() == ViewUpdate.CLOSED
			&& vmsg.getView() == view)
			view = null;
	}

	private void handleBufferUpdate(BufferUpdate bmsg)
	{
		if(bmsg.getWhat() == BufferUpdate.CREATED
			|| bmsg.getWhat() == BufferUpdate.CLOSED)
			browserView.updateFileView();
	}

	private void propertiesChanged()
	{
		showHiddenFiles = jEdit.getBooleanProperty("vfs.browser.showHiddenFiles");
		sortFiles = jEdit.getBooleanProperty("vfs.browser.sortFiles");
		sortMixFilesAndDirs = jEdit.getBooleanProperty("vfs.browser.sortMixFilesAndDirs");
		sortIgnoreCase = jEdit.getBooleanProperty("vfs.browser.sortIgnoreCase");

		filterCombo.setModel(new DefaultComboBoxModel(filters));

		// unless we're being called from the constructor, reload
		// directory so that new sorting settings take effect.
		if(path != null)
			reloadDirectory(false);
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(source == pathField)
			{
				String path = pathField.getText();
				if(path != null)
					setDirectory(path);
			}
			else if(source == filterCombo)
			{
				VFSFilter filter = (VFSFilter)filterCombo.getSelectedItem();
				if(filter != null)
				{
					VFSBrowser.this.filenameFilter = filter;

					// don't do anything during initialization...
					if(path != null)
						reloadDirectory(false);
				}
			}
			else if(source == up)
				setDirectory(vfs.getFileParent(path));
			else if(source == reload)
				reloadDirectory(true);
			else if(source == roots)
				setDirectory(FileRootsVFS.PROTOCOL + ":");
			else if(source == home)
				setDirectory(System.getProperty("user.home"));
			else if(source == synchronize)
			{
				if(view != null)
				{
					Buffer buffer = view.getBuffer();
					setDirectory(buffer.getVFS().getFileParent(
						buffer.getPath()));
				}
				else
					getToolkit().beep();
			}
			else if(source == addToFavorites)
			{
				FavoritesVFS.addToFavorites(path);
				reloadDirectory(true);
			}
			else if(source == gotoFavorites)
				setDirectory(FavoritesVFS.PROTOCOL + ":");
			else // it's a VFS button
			{
				String vfsName = ((JButton)source).getActionCommand();
				VFS vfs = VFSManager.getVFSForName(vfsName);
				String directory = vfs.showBrowseDialog(vfsSession,
					VFSBrowser.this);
				if(directory != null)
					setDirectory(directory);
			}
		}
	}
}

/*
 * Change Log:
 * $Log$
 * Revision 1.5  2000/08/03 07:43:42  sp
 * Favorites added to browser, lots of other stuff too
 *
 * Revision 1.4  2000/08/01 11:44:15  sp
 * More VFS browser work
 *
 * Revision 1.3  2000/07/31 11:32:09  sp
 * VFS file chooser is now in a minimally usable state
 *
 */
