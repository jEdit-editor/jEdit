/*
 * View.java - jEdit view
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

import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.util.Log;

/**
 * A window that edits buffers. There is no public constructor in the
 * View class. Views are created and destroyed by the <code>jEdit</code>
 * class.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class View extends JFrame implements EBComponent
{
	/**
	 * Displays the specified string in the status area of this view.
	 * @param str The string to display
	 */
	public void pushStatus(String str)
	{
		statusMsg.push(str);
		/*JEditTextArea[] textAreas = getTextAreas();
		for(int i = 0 ; i < textAreas.length; i++)
			textAreas[i].getStatus().repaint();*/
	}

	/**
	 * Displays the previous status bar message.
	 */
	public void popStatus()
	{
		if(!statusMsg.isEmpty())
			statusMsg.pop();

		/*JEditTextArea[] textAreas = getTextAreas();
		for(int i = 0 ; i < textAreas.length; i++)
			textAreas[i].getStatus().repaint();*/
	}

	/**
	 * Returns the search bar.
	 * @since jEdit 2.4pre4
	 */
	public final SearchBar getSearchBar()
	{
		return searchBar;
	}

	/**
	 * Returns the input handler.
	 */
	public final InputHandler getInputHandler()
	{
		return inputHandler;
	}

	/**
	 * Splits the view.
	 * @since jEdit 2.3pre2
	 */
	public void split(int orientation)
	{
		editPane.saveCaretInfo();
		EditPane oldEditPane = editPane;
		setEditPane(createEditPane(oldEditPane,null));
		editPane.loadCaretInfo();

		JComponent oldParent = (JComponent)oldEditPane.getParent();

		if(oldParent instanceof JSplitPane)
		{
			JSplitPane oldSplitPane = (JSplitPane)oldParent;
			int dividerPos = oldSplitPane.getDividerLocation();

			Component left = oldSplitPane.getLeftComponent();
			final JSplitPane newSplitPane = new JSplitPane(orientation,
				oldEditPane,editPane);
			newSplitPane.setBorder(null);

			if(left == oldEditPane)
				oldSplitPane.setLeftComponent(newSplitPane);
			else
				oldSplitPane.setRightComponent(newSplitPane);

			oldSplitPane.setDividerLocation(dividerPos);

			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					newSplitPane.setDividerLocation(0.5);
					editPane.focusOnTextArea();
				}
			});
		}
		else
		{
			JSplitPane newSplitPane = splitPane = new JSplitPane(orientation,
				oldEditPane,editPane);
			newSplitPane.setBorder(null);
			oldParent.add(splitPane);
			oldParent.revalidate();

			newSplitPane.setDividerLocation(oldParent.getHeight() / 2);
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					editPane.focusOnTextArea();
				}
			});
		}
	}

	/**
	 * Unsplits the view.
	 * @since jEdit 2.3pre2
	 */
	public void unsplit()
	{
		if(splitPane != null)
		{
			EditPane[] editPanes = getEditPanes();
			for(int i = 0; i < editPanes.length; i++)
			{
				EditPane _editPane = editPanes[i];
				if(editPane != _editPane)
					editPane.close();
			}

			JComponent parent = (JComponent)splitPane.getParent();
			parent.remove(splitPane);
			splitPane = null;
			parent.add(editPane);
			parent.revalidate();
		}

		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				editPane.focusOnTextArea();
			}
		});
	}

	/**
	 * Returns the top-level split pane, if any.
	 * @since jEdit 2.3pre2
	 */
	public JSplitPane getSplitPane()
	{
		return splitPane;
	}

	/**
	 * Returns the current edit pane's buffer.
	 */
	public Buffer getBuffer()
	{
		return editPane.getBuffer();
	}

	/**
	 * Sets the current edit pane's buffer.
	 */
	public void setBuffer(Buffer buffer)
	{
		editPane.setBuffer(buffer);
	}

	/**
	 * Returns the current edit pane's text area.
	 */
	public JEditTextArea getTextArea()
	{
		return editPane.getTextArea();
	}

	/**
	 * Returns the current edit pane.
	 * @since jEdit 2.5pre2
	 */
	public EditPane getEditPane()
	{
		return editPane;
	}

	/**
	 * Returns all edit panes.
	 * @since jEdit 2.5pre2
	 */
	public EditPane[] getEditPanes()
	{
		if(splitPane == null)
		{
			EditPane[] ep = { editPane };
			return ep;
		}
		else
		{
			Vector vec = new Vector();
			getEditPanes(vec,splitPane);
			EditPane[] ep = new EditPane[vec.size()];
			vec.copyInto(ep);
			return ep;
		}
	}

	/**
	 * Adds a tool bar to this view.
	 * @param toolBar The tool bar
	 */
	public void addToolBar(Component toolBar)
	{
		toolBars.add(toolBar);
		getRootPane().revalidate();
	}

	/**
	 * Removes a tool bar from this view.
	 * @param toolBar The tool bar
	 */
	public void removeToolBar(Component toolBar)
	{
		toolBars.remove(toolBar);
		getRootPane().revalidate();
	}

	/**
	 * Returns true if this view has been closed with
	 * <code>jEdit.closeView()</code>.
	 */
	public final boolean isClosed()
	{
		return closed;
	}

	/**
	 * Shows the wait cursor and glass pane.
	 */
	public synchronized void showWaitCursor()
	{
		if(waitCount++ == 0)
		{
			glassPane.setVisible(true);

			// still needed even though glass pane
			// has a wait cursor
			Cursor cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
			setCursor(cursor);
			EditPane[] editPanes = getEditPanes();
			for(int i = 0; i < editPanes.length; i++)
			{
				EditPane editPane = editPanes[i];
				editPane.getTextArea().getPainter()
					.setCursor(cursor);
			}
		}
	}

	/**
	 * Hides the wait cursor and glass pane.
	 */
	public synchronized void hideWaitCursor()
	{
		if(waitCount > 0)
			waitCount--;

		if(waitCount == 0)
		{
			glassPane.setVisible(false);

			// still needed even though glass pane
			// has a wait cursor
			Cursor cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
			setCursor(cursor);
			cursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
			EditPane[] editPanes = getEditPanes();
			for(int i = 0; i < editPanes.length; i++)
			{
				EditPane editPane = editPanes[i];
				editPane.getTextArea().getPainter()
					.setCursor(cursor);
			}
		}
	}

	/**
	 * Returns the next view in the list.
	 */
	public final View getNext()
	{
		return next;
	}

	/**
	 * Returns the previous view in the list.
	 */
	public final View getPrev()
	{
		return prev;
	}

	public void handleMessage(EBMessage msg)
	{
		if(msg instanceof PropertiesChanged)
			propertiesChanged();
		else if(msg instanceof MacrosChanged)
			updateMacrosMenu();
		else if(msg instanceof SearchSettingsChanged)
		{
			if(searchBar != null)
				searchBar.update();
		}
		else if(msg instanceof BufferUpdate)
			handleBufferUpdate((BufferUpdate)msg);
	}

	public JMenu getMenu(String name)
	{
		if(name.equals("buffers"))
			return buffers;
		else if(name.equals("open-from-menu"))
			return openFrom;
		else if(name.equals("save-to-menu"))
			return saveTo;
		else if(name.equals("recent-files"))
			return recent;
		else if(name.equals("current-directory"))
			return currentDirectory;
		else if(name.equals("clear-marker"))
			return clearMarker;
		else if(name.equals("goto-marker"))
			return gotoMarker;
		else if(name.equals("macros"))
			return macros;
		else if(name.equals("plugins"))
			return plugins;
		else if(name.equals("help-menu"))
			return help;
		else
			return null;
	}

	// package-private members
	View prev;
	View next;

	View(Buffer buffer)
	{
		setIconImage(GUIUtilities.getEditorIcon());

		editPane = createEditPane(null,buffer);

		// Dynamic menus
		buffers = GUIUtilities.loadMenu(this,"buffers");
		openFrom = GUIUtilities.loadMenu(this,"open-from-menu");
		saveTo = GUIUtilities.loadMenu(this,"save-to-menu");
		recent = GUIUtilities.loadMenu(this,"recent-files");
		currentDirectory = new CurrentDirectoryMenu(this);
		clearMarker = GUIUtilities.loadMenu(this,"clear-marker");
		gotoMarker = GUIUtilities.loadMenu(this,"goto-marker");
		macros = GUIUtilities.loadMenu(this,"macros");
		help = GUIUtilities.loadMenu(this,"help-menu");
		plugins = GUIUtilities.loadMenu(this,"plugins");
		updateVFSMenus();
		updateMacrosMenu();
		updateHelpMenu();
		updatePluginsMenu();

		EditBus.addToBus(this);

		setJMenuBar(GUIUtilities.loadMenuBar(this,"view.mbar"));

		toolBars = new Box(BoxLayout.Y_AXIS);

		statusMsg = new Stack();

		inputHandler = new DefaultInputHandler(this,(DefaultInputHandler)
			jEdit.getInputHandler());

		propertiesChanged();

		getContentPane().add(BorderLayout.NORTH,toolBars);
		getContentPane().add(BorderLayout.CENTER,editPane);

		glassPane = new GlassPane();
		getRootPane().setGlassPane(glassPane);

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowHandler());
	}

	void close()
	{
		closed = true;
		GUIUtilities.saveGeometry(this,"view");
		EditBus.removeFromBus(this);
		dispose();

		EditPane[] editPanes = getEditPanes();
		for(int i = 0; i < editPanes.length; i++)
			editPanes[i].close();

		// null some variables so that retaining references
		// to closed views won't hurt as much.
		buffers = openFrom = saveTo = recent = currentDirectory
			= clearMarker = gotoMarker = macros = plugins
			= help = null;
		toolBars = null;
		toolBar = null;
		searchBar = null;
		statusMsg = null;
		splitPane = null;
		inputHandler = null;
		glassPane = null;

		setContentPane(new JPanel());
	}

	/**
	 * Updates the title bar and read only status of the text
	 * area.
	 */
	void updateTitle()
	{
		Vector buffers = new Vector();
		EditPane[] editPanes = getEditPanes();
		for(int i = 0; i < editPanes.length; i++)
		{
			Buffer buffer = editPanes[i].getBuffer();
			if(buffers.indexOf(buffer) == -1)
				buffers.addElement(buffer);
		}

		StringBuffer title = new StringBuffer(jEdit.getProperty("view.title"));
		for(int i = 0; i < buffers.size(); i++)
		{
			if(i != 0)
				title.append(", ");

			Buffer buffer = (Buffer)buffers.elementAt(i);
			title.append((showFullPath && !buffer.isNewFile())
				? buffer.getPath() : buffer.getName());
		}
		setTitle(title.toString());
	}

	/**
	 * Recreates the buffers menu.
	 */
	void updateBuffersMenu()
	{
		if(jEdit.getBufferCount() == 0)
		{
			// if this becomes zero, it is guaranteed that
			// a new untitled buffer will open again. So
			// we just ignore this and update the menu
			// when the untitled buffer comes in
			return;
		}

		// Because the buffers menu contains normal items as
		// well as dynamically-generated stuff, we are careful
		// to only remove the dynamic crap here...
		for(int i = buffers.getMenuComponentCount() - 1; i >= 0; i--)
		{
			if(buffers.getMenuComponent(i) instanceof JSeparator)
				break;
			else
				buffers.remove(i);
		}

		Buffer[] bufferArray = jEdit.getBuffers();
		ButtonGroup grp = new ButtonGroup();
		for(int i = 0; i < bufferArray.length; i++)
		{
			Buffer b = bufferArray[i];
			String name = b.getPath();
			Object[] args = { name,
				new Integer(b.isReadOnly() ? 1: 0),
				new Integer(b.isDirty() ? 1 : 0),
				new Integer(b.isNewFile() ? 1 : 0) };
			JRadioButtonMenuItem menuItem =
				new JRadioButtonMenuItem(jEdit.getProperty(
					"view.buffer-label",args));
			menuItem.addActionListener(jEdit.getAction("select-buffer"));
			grp.add(menuItem);
			menuItem.setActionCommand(name);
			if(editPane.getBuffer() == b)
				menuItem.getModel().setSelected(true);
			buffers.add(menuItem);
		}
	}

	/**
	 * Recreates the goto marker and clear marker menus.
	 */
	void updateMarkerMenus()
	{
		if(clearMarker.getMenuComponentCount() != 0)
			clearMarker.removeAll();
		if(gotoMarker.getMenuComponentCount() != 0)
			gotoMarker.removeAll();
		EditAction clearMarkerAction = jEdit.getAction("clear-marker");
		EditAction gotoMarkerAction = jEdit.getAction("goto-marker");
		Enumeration enum = editPane.getBuffer().getMarkers();
		if(!enum.hasMoreElements())
		{
			clearMarker.add(GUIUtilities.loadMenuItem(this,"no-markers"));
			gotoMarker.add(GUIUtilities.loadMenuItem(this,"no-markers"));
			return;
		}
		while(enum.hasMoreElements())
		{
			String name = ((Marker)enum.nextElement())
				.getName();
			EnhancedMenuItem menuItem = new EnhancedMenuItem(name,
				null,clearMarkerAction,name);
			clearMarker.add(menuItem);
			menuItem = new EnhancedMenuItem(name,null,
				gotoMarkerAction,name);
			gotoMarker.add(menuItem);
		}
	}

	// protected members

	/**
	 * Forwards key events directly to the input handler.
	 * This is slightly faster than using a KeyListener
	 * because some Swing overhead is avoided.
	 */
	protected void processKeyEvent(KeyEvent evt)
	{
		if(glassPane.isVisible())
		{
			super.processKeyEvent(evt);
			return;
		}

		switch(evt.getID())
		{
		case KeyEvent.KEY_TYPED:
			if(inputHandler.isPrefixActive())
				inputHandler.keyTyped(evt);
			break;
		case KeyEvent.KEY_PRESSED:
			inputHandler.keyPressed(evt);
			break;
		case KeyEvent.KEY_RELEASED:
			inputHandler.keyReleased(evt);
			break;
		}

		if(!evt.isConsumed())
			super.processKeyEvent(evt);
	}

	// private members
	private boolean closed;

	private JMenu buffers;
	private JMenu openFrom;
	private JMenu saveTo;
	private JMenu recent;
	private JMenu currentDirectory;
	private JMenu clearMarker;
	private JMenu gotoMarker;
	private JMenu macros;
	private JMenu plugins;
	private JMenu help;

	private Box toolBars;
	private JToolBar toolBar;
	private SearchBar searchBar;

	private Stack statusMsg;

	private EditPane editPane;
	private JSplitPane splitPane;

	private InputHandler inputHandler;
	private GlassPane glassPane;

	private int waitCount;

	private boolean showFullPath;
	private boolean checkModStatus;

	private void getEditPanes(Vector vec, Component comp)
	{
		if(comp instanceof EditPane)
			vec.addElement(comp);
		else if(comp instanceof JSplitPane)
		{
			JSplitPane split = (JSplitPane)comp;
			getEditPanes(vec,split.getLeftComponent());
			getEditPanes(vec,split.getRightComponent());
		}
	}

	/**
	 * Reloads various settings from the properties.
	 */
	private void propertiesChanged()
	{
		loadToolBars();

		showFullPath = jEdit.getBooleanProperty("view.showFullPath");
		checkModStatus = jEdit.getBooleanProperty("view.checkModStatus");
		updateTitle();

		updateRecentMenu();
	}

	private void loadToolBars()
	{
		if(jEdit.getBooleanProperty("view.showToolbar"))
		{
			if(toolBar == null)
			{
				toolBar = GUIUtilities.loadToolBar("view.toolbar");
				toolBar.add(Box.createGlue());
			}
			if(toolBar.getParent() == null)
				addToolBar(toolBar);
		}
		else if(toolBar != null)
		{
			removeToolBar(toolBar);
			toolBar = null;
		}

		if(jEdit.getBooleanProperty("view.showSearchbar"))
		{
			if(searchBar == null)
				searchBar = new SearchBar(this);
			if(searchBar.getParent() == null)
				addToolBar(searchBar);
		}
		else if(searchBar != null)
		{
			removeToolBar(searchBar);
			searchBar = null;
		}
	}

	private EditPane createEditPane(EditPane pane, Buffer buffer)
	{
		EditPane editPane = new EditPane(this,pane,buffer);
		editPane.getTextArea().addFocusListener(new FocusHandler());
		return editPane;
	}

	private void setEditPane(EditPane editPane)
	{
		EditPane oldPane = this.editPane;
		this.editPane = editPane;
		if(oldPane.getBuffer() != editPane.getBuffer())
		{
			updateBuffersMenu();
			updateMarkerMenus();
		}
	}

	/**
	 * Returns true if at least one edit pane is editing this buffer.
	 */
	private boolean isOpen(Buffer buffer)
	{
		EditPane[] editPanes = getEditPanes();
		for(int i = 0; i < editPanes.length; i++)
		{
			if(editPanes[i].getBuffer() == buffer)
				return true;
		}
		return false;
	}

	/**
	 * Recreates the VFS open-from and save-to menus.
	 */
	private void updateVFSMenus()
	{
		EditAction openFromAction = jEdit.getAction("open-from");
		EditAction saveToAction = jEdit.getAction("save-to");

		Enumeration enum = VFSManager.getFilesystems();

		Vector openFromItems = new Vector();
		Vector saveToItems = new Vector();
		while(enum.hasMoreElements())
		{
			VFS vfs = (VFS)enum.nextElement();
			String name = vfs.getName();
			String label = jEdit.getProperty("vfs." + name
				+ ".label");
			EnhancedMenuItem emi = new EnhancedMenuItem(
				label,null,openFromAction,name);
			openFromItems.addElement(emi);
			emi = new EnhancedMenuItem(
				label,null,saveToAction,name);
			saveToItems.addElement(emi);
		}

		MiscUtilities.quicksort(openFromItems,new MenuItemCompare());
		MiscUtilities.quicksort(saveToItems,new MenuItemCompare());
		for(int i = 0; i < openFromItems.size(); i++)
		{
			openFrom.add((JMenuItem)openFromItems.elementAt(i));
			saveTo.add((JMenuItem)saveToItems.elementAt(i));
		}
	}

	/**
	 * Recreates the recent menu.
	 */
	private void updateRecentMenu()
	{
		if(recent.getMenuComponentCount() != 0)
			recent.removeAll();
		EditAction action = jEdit.getAction("open-path");
		String[] recentArray = jEdit.getRecent();
		if(recentArray.length == 0)
		{
			recent.add(GUIUtilities.loadMenuItem(this,"no-recent"));
			return;
		}
		for(int i = 0; i < recentArray.length; i++)
		{
			String path = recentArray[i];
			EnhancedMenuItem menuItem = new EnhancedMenuItem(path,
				null,action,path);
			recent.add(menuItem);
		}
	}

	/**
	 * Recreates the macros menu.
	 */
	private void updateMacrosMenu()
	{
		// Because the macros menu contains normal items as
		// well as dynamically-generated stuff, we are careful
		// to only remove the dynamic crap here...
		for(int i = macros.getMenuComponentCount() - 1; i >= 0; i--)
		{
			if(macros.getMenuComponent(i) instanceof JSeparator)
				break;
			else
				macros.remove(i);
		}

		int count = macros.getMenuComponentCount();

		Vector macroVector = Macros.getMacros();
		createMacrosMenu(macros,macroVector,0);

		if(count == macros.getMenuComponentCount())
			macros.add(GUIUtilities.loadMenuItem(this,"no-macros"));
	}

	private void createMacrosMenu(JMenu menu, Vector vector, int start)
	{
		EditAction action = jEdit.getAction("play-macro");

		for(int i = start; i < vector.size(); i++)
		{
			Object obj = vector.elementAt(i);
			if(obj instanceof Macros.Macro)
			{
				Macros.Macro macro = (Macros.Macro)obj;
				String label = macro.label;
				String path = macro.path;
				EnhancedMenuItem menuItem = new EnhancedMenuItem(
					label,jEdit.getProperty(macro.name + ".shortcut"),
					action,path);
				menu.add(menuItem);
			}
			else if(obj instanceof Vector)
			{
				Vector subvector = (Vector)obj;
				String name = (String)subvector.elementAt(0);
				JMenu submenu = new JMenu(name);
				createMacrosMenu(submenu,subvector,1);
				if(submenu.getMenuComponentCount() == 0)
				{
					submenu.add(GUIUtilities.loadMenuItem(
						this,"no-macros"));
				}
				menu.add(submenu);
			}
		}
	}

	/**
	 * Recreates the plugins menu.
	 */
	private void updatePluginsMenu()
	{
		if(plugins.getMenuComponentCount() != 0)
			plugins.removeAll();

		// Query plugins for menu items
		Vector pluginMenus = new Vector();
		Vector pluginMenuItems = new Vector();

		EditPlugin[] pluginArray = jEdit.getPlugins();
		for(int i = 0; i < pluginArray.length; i++)
		{
			try
			{
				pluginArray[i].createMenuItems(this,pluginMenus,
					pluginMenuItems);
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,this,"Error creating menu items"
					+ " for plugin");
				Log.log(Log.ERROR,this,t);
			}
		}

		if(pluginMenus.isEmpty() && pluginMenuItems.isEmpty())
		{
			plugins.add(GUIUtilities.loadMenuItem(this,"no-plugins"));
			return;
		}

		// Sort them
		MenuItemCompare comp = new MenuItemCompare();
		MiscUtilities.quicksort(pluginMenus,comp);
		MiscUtilities.quicksort(pluginMenuItems,comp);

		for(int i = 0; i < pluginMenus.size(); i++)
			plugins.add((JMenu)pluginMenus.elementAt(i));

		if(!pluginMenus.isEmpty() && !pluginMenuItems.isEmpty())
			plugins.addSeparator();

		for(int i = 0; i < pluginMenuItems.size(); i++)
			plugins.add((JMenuItem)pluginMenuItems.elementAt(i));
	}

	private void updateHelpMenu()
	{
		EditAction action = jEdit.getAction("help");

		EditPlugin[] plugins = jEdit.getPlugins();
		for(int i = 0; i < plugins.length; i++)
		{
			EditPlugin plugin = plugins[i];
			String name = plugin.getClass().getName();

			String label = jEdit.getProperty("plugin." + name + ".name");
			String docs = jEdit.getProperty("plugin." + name + ".docs");
			if(docs != null)
			{
				java.net.URL docsURL = plugin.getClass().getResource(docs);
				if(label != null && docsURL != null)
				{
					help.add(new EnhancedMenuItem(label,
						null,action,docsURL.toString()));
				}
			}
		}
	}

	private static class MenuItemCompare implements MiscUtilities.Compare
	{
		public int compare(Object obj1, Object obj2)
		{
			return ((JMenuItem)obj1).getText().compareTo(
				((JMenuItem)obj2).getText());
		}
	}

	private void handleBufferUpdate(BufferUpdate msg)
	{
		Buffer buffer = msg.getBuffer();
		if(msg.getWhat() == BufferUpdate.CREATED)
		{
			updateBuffersMenu();
		}
		else if(msg.getWhat() == BufferUpdate.CLOSED)
		{
			updateRecentMenu();
			updateBuffersMenu();
		}
		else if(msg.getWhat() == BufferUpdate.DIRTY_CHANGED
			|| msg.getWhat() == BufferUpdate.LOADED)
		{
			if(isOpen(buffer))
				updateTitle();
			updateBuffersMenu();
		}
		else if(msg.getWhat() == BufferUpdate.MARKERS_CHANGED)
		{
			if(isOpen(buffer))
				updateMarkerMenus();
		}
	}

	/*class CaretHandler implements CaretListener
	{
		public void caretUpdate(CaretEvent evt)
		{
			JEditTextArea textArea = (JEditTextArea)evt.getSource();
			((StatusBar)textArea.getStatus()).repaint();
		}
	}*/

	class FocusHandler extends FocusAdapter
	{
		public void focusGained(FocusEvent evt)
		{
			// walk up hierarchy, looking for an EditPane
			Component comp = (Component)evt.getSource();
			while(!(comp instanceof EditPane))
			{
				comp = comp.getParent();
			}

			setEditPane((EditPane)comp);
		}
	}

	class WindowHandler extends WindowAdapter
	{
		boolean gotFocus;

		public void windowActivated(WindowEvent evt)
		{
			if(!gotFocus)
			{
				editPane.focusOnTextArea();
				gotFocus = true;
			}

			if(checkModStatus)
				editPane.getBuffer().checkModTime(View.this);
		}

		public void windowClosing(WindowEvent evt)
		{
			jEdit.closeView(View.this);
		}
	}

	class GlassPane extends JComponent
	{
		GlassPane()
		{
			GlassPane.this.enableEvents(AWTEvent.KEY_EVENT_MASK);
			GlassPane.this.enableEvents(AWTEvent.MOUSE_EVENT_MASK);
			GlassPane.this.setOpaque(false);
			GlassPane.this.setCursor(Cursor.getPredefinedCursor(
				Cursor.WAIT_CURSOR));
		}
	}

	/*class StatusBar extends JComponent
	{
		JEditTextArea textArea;

		StatusBar(JEditTextArea textArea)
		{
			StatusBar.this.textArea = textArea;
			StatusBar.this.setDoubleBuffered(true);
			StatusBar.this.setFont(UIManager.getFont("Label.font"));
			StatusBar.this.setForeground(UIManager.getColor("Label.foreground"));
			StatusBar.this.setBackground(UIManager.getColor("Label.background"));
		}

		public void paint(Graphics g)
		{
			FontMetrics fm = g.getFontMetrics();

			int dot = textArea.getCaretPosition();

			int currLine = StatusBar.this.textArea.getCaretLine();
			int start = StatusBar.this.textArea.getLineStartOffset(currLine);
			int numLines = StatusBar.this.textArea.getLineCount();

			String str;
			if(inputHandler.isRepeatEnabled())
			{
				int repeatCount = inputHandler.getRepeatCount();
				if(repeatCount == 1)
					str = "";
				else
					str = String.valueOf(repeatCount);
				Object[] args = { str };
				str = jEdit.getProperty("view.status.repeat",args);
			}
			else if(!statusMsg.isEmpty())
				str = (String)statusMsg.peek();
			else
			{
				str = ("col " + ((dot - start) + 1) + " line "
					+ (currLine + 1) + "/"
					+ numLines + " "
					+ (((currLine + 1) * 100) / numLines) + "%");
			}

			g.drawString(str,0,(StatusBar.this.getHeight()
				+ fm.getAscent()) / 2);
		}

		public Dimension getPreferredSize()
		{
			return new Dimension(200,0);
		}
	}*/
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.168  2000/05/07 05:48:30  sp
 * You can now edit several buffers side-by-side in a split view
 *
 * Revision 1.167  2000/05/05 11:08:26  sp
 * Johnny Ryall
 *
 * Revision 1.166  2000/05/04 10:37:04  sp
 * Wasting time
 *
 * Revision 1.165  2000/05/01 11:53:23  sp
 * More icons added to toolbar, minor updates here and there
 *
 * Revision 1.164  2000/04/30 07:27:13  sp
 * Ftp VFS hacking, bug fixes
 *
 * Revision 1.163  2000/04/29 09:17:07  sp
 * VFS updates, various fixes
 *
 * Revision 1.162  2000/04/28 09:29:11  sp
 * Key binding handling improved, VFS updates, some other stuff
 *
 * Revision 1.161  2000/04/25 03:32:40  sp
 * Even more VFS hacking
 *
 * Revision 1.160  2000/04/24 11:00:23  sp
 * More VFS hacking
 *
 * Revision 1.159  2000/04/21 05:32:20  sp
 * Focus tweak
 *
 * Revision 1.158  2000/04/18 08:27:52  sp
 * Context menu editor started
 *
 * Revision 1.157  2000/04/15 04:14:47  sp
 * XML files updated, jEdit.get/setBooleanProperty() method added
 *
 */
