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
	 * Returns the dockable window manager associated with this view.
	 * @since jEdit 2.6pre3
	 */
	public DockableWindowManager getDockableWindowManager()
	{
		return dockableWindowManager;
	}

	/**
	 * Displays the specified string in the status area of this view.
	 * @param str The string to display
	 * @since jEdit 2.5pre2
	 */
	public void showStatus(String str)
	{
		status.showStatus(str);
	}

	/**
	 * Updates the status bar.
	 * @since jEdit 2.5pre3
	 */
	public void updateBufferStatus()
	{
		status.updateBufferStatus();
	}

	/**
	 * Updates the caret status.
	 * @since jEdit 2.5pre2
	 */
	public void updateCaretStatus()
	{
		status.updateCaretStatus();
	}

	/**
	 * Returns the command line prompt.
	 * @since jEdit 2.6pre5
	 */
	public final CommandLine getCommandLine()
	{
		return commandLine;
	}

	/**
	 * Returns the listener that will handle all key events in this
	 * view, if any.
	 */
	public final KeyListener getKeyEventInterceptor()
	{
		return keyEventInterceptor;
	}

	/**
	 * Sets the listener that will handle all key events in this
	 * view. For example, the complete word command uses this so
	 * that all key events are passed to the word list popup while
	 * it is visible.
	 * @param comp The component
	 */
	public void setKeyEventInterceptor(KeyListener listener)
	{
		this.keyEventInterceptor = listener;
	}

	/**
	 * Returns the input handler.
	 */
	public final InputHandler getInputHandler()
	{
		return inputHandler;
	}

	/**
	 * Sets the input handler.
	 * @param inputHandler The new input handler
	 */
	public void setInputHandler(InputHandler inputHandler)
	{
		this.inputHandler = inputHandler;
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

			Dimension size;
			if(oldParent instanceof JSplitPane)
				size = oldParent.getSize();
			else
				size = oldEditPane.getSize();
			newSplitPane.setDividerLocation(((orientation
				== JSplitPane.VERTICAL_SPLIT) ? size.height
				: size.width) / 2);
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					editPane.focusOnTextArea();
					editPane.getTextArea().getGutter()
						.updateBorder();
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
					_editPane.close();
			}

			JComponent parent = (JComponent)splitPane.getParent();

			parent.remove(splitPane);
			parent.add(editPane);
			parent.revalidate();

			splitPane = null;
			updateTitle();
		}

		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				editPane.focusOnTextArea();
				editPane.getTextArea().getGutter()
					.updateBorder();
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
			commandLine.updateSearchSettings();
		else if(msg instanceof BufferUpdate)
			handleBufferUpdate((BufferUpdate)msg);
	}

	public JMenu getMenu(String name)
	{
		if(name.equals("buffers"))
			return buffers;
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

	/**
	 * Forwards key events directly to the input handler.
	 * This is slightly faster than using a KeyListener
	 * because some Swing overhead is avoided.
	 */
	public void processKeyEvent(KeyEvent evt)
	{
		// JTextComponents don't consume events...
		if(getFocusOwner() instanceof JTextComponent)
		{
			// fix for the bug where key events in JTextComponents
			// inside views are also handled by the input handler
			if(evt.getID() == KeyEvent.KEY_PRESSED)
			{
				switch(evt.getKeyCode())
				{
				// have to do this because jEdit handles these
				// keys on KEY_PRESSED, but text components only
				// register them in their keymaps as KEY_TYPED
				case KeyEvent.VK_BACK_SPACE:
				case KeyEvent.VK_ENTER:
				case KeyEvent.VK_TAB:
					return;
				}
			}

			Keymap keymap = ((JTextComponent)getFocusOwner())
				.getKeymap();
			if(keymap.getAction(KeyStroke.getKeyStrokeForEvent(evt)) != null)
				return;
		}

		if(evt.isConsumed())
			return;

		evt = KeyEventWorkaround.processKeyEvent(evt);
		if(evt == null)
			return;

		switch(evt.getID())
		{
		case KeyEvent.KEY_TYPED:
			if(keyEventInterceptor != null)
				keyEventInterceptor.keyTyped(evt);
			else if(inputHandler.isPrefixActive())
				inputHandler.keyTyped(evt);
			break;
		case KeyEvent.KEY_PRESSED:
			if(keyEventInterceptor != null)
				keyEventInterceptor.keyPressed(evt);
			else
				inputHandler.keyPressed(evt);
			break;
		case KeyEvent.KEY_RELEASED:
			if(keyEventInterceptor != null)
				keyEventInterceptor.keyReleased(evt);
			else
				inputHandler.keyReleased(evt);
			break;
		}

		if(!evt.isConsumed())
			super.processKeyEvent(evt);
	}

	// package-private members
	View prev;
	View next;

	View(Buffer buffer)
	{
		setIconImage(GUIUtilities.getEditorIcon());

		dockableWindowManager = new DockableWindowManager(this);

		// Dynamic menus
		buffers = GUIUtilities.loadMenu(this,"buffers");
		recent = GUIUtilities.loadMenu(this,"recent-files");
		currentDirectory = new CurrentDirectoryMenu(this);
		clearMarker = GUIUtilities.loadMenu(this,"clear-marker");
		gotoMarker = GUIUtilities.loadMenu(this,"goto-marker");
		macros = GUIUtilities.loadMenu(this,"macros");
		help = GUIUtilities.loadMenu(this,"help-menu");
		plugins = GUIUtilities.loadMenu(this,"plugins");

		editPane = createEditPane(null,buffer);

		updateBuffersMenu();
		updateMacrosMenu();
		updatePluginsMenu();
		updateHelpMenu();

		EditBus.addToBus(this);

		setJMenuBar(GUIUtilities.loadMenuBar(this,"view.mbar"));

		toolBars = new Box(BoxLayout.Y_AXIS);

		inputHandler = new DefaultInputHandler(this,(DefaultInputHandler)
			jEdit.getInputHandler());

		propertiesChanged();

		getContentPane().add(BorderLayout.NORTH,toolBars);
		getContentPane().add(BorderLayout.CENTER,dockableWindowManager);

		Box statusBox = new Box(BoxLayout.Y_AXIS);
		status = new StatusBar(this);
		statusBox.add(status);
		commandLine = new CommandLine(this);
		statusBox.add(commandLine);

		getContentPane().add(BorderLayout.SOUTH,statusBox);

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowHandler());

		dockableWindowManager.init();
	}

	void close()
	{
		closed = true;

		// save dockable window geometry, and close 'em
		dockableWindowManager.close();

		GUIUtilities.saveGeometry(this,"view");
		EditBus.removeFromBus(this);
		dispose();

		EditPane[] editPanes = getEditPanes();
		for(int i = 0; i < editPanes.length; i++)
			editPanes[i].close();

		// null some variables so that retaining references
		// to closed views won't hurt as much.
		buffers = recent = currentDirectory = clearMarker
			= gotoMarker = macros = plugins = help = null;
		toolBars = null;
		toolBar = null;
		commandLine = null;
		status = null;
		splitPane = null;
		inputHandler = null;

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

/*
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
*/
		buffers.removeAll();

		Buffer[] bufferArray = jEdit.getBuffers();
		ButtonGroup grp = new ButtonGroup();
		for(int i = 0; i < bufferArray.length; i++)
		{
			Buffer b = bufferArray[i];
			Object[] args = { b.getName(),
				new Integer(b.isReadOnly() ? 1: 0),
				new Integer(b.isDirty() ? 1 : 0),
				new Integer(b.isNewFile() ? 1 : 0) };
			JRadioButtonMenuItem menuItem =
				new JRadioButtonMenuItem(jEdit.getProperty(
					"view.buffer-label",args),b.getIcon());
			menuItem.addActionListener(jEdit.getAction("select-buffer"));
			grp.add(menuItem);
			menuItem.setActionCommand(b.getPath());
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
		Vector markers = editPane.getBuffer().getMarkers();
		if(markers.size() == 0)
		{
			clearMarker.add(GUIUtilities.loadMenuItem("no-markers"));
			gotoMarker.add(GUIUtilities.loadMenuItem("no-markers"));
			return;
		}
		for(int i = 0; i < markers.size(); i++)
		{
			String name = ((Marker)markers.elementAt(i)).getName();
			EnhancedMenuItem menuItem = new EnhancedMenuItem(name,
				clearMarkerAction,name);
			clearMarker.add(menuItem);
			menuItem = new EnhancedMenuItem(name,gotoMarkerAction,name);
			gotoMarker.add(menuItem);
		}
	}

	// private members
	private boolean closed;

	private DockableWindowManager dockableWindowManager;

	private JMenu buffers;
	private JMenu recent;
	private JMenu currentDirectory;
	private JMenu clearMarker;
	private JMenu gotoMarker;
	private JMenu macros;
	private JMenu plugins;
	private JMenu help;

	private Box toolBars;
	private JToolBar toolBar;

	private EditPane editPane;
	private JSplitPane splitPane;

	private StatusBar status;
	private CommandLine commandLine;

	private KeyListener keyEventInterceptor;
	private InputHandler inputHandler;

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
		loadToolBar();

		showFullPath = jEdit.getBooleanProperty("view.showFullPath");
		checkModStatus = jEdit.getBooleanProperty("view.checkModStatus");
		updateTitle();

		updateRecentMenu();

		dockableWindowManager.propertiesChanged();
	}

	private void loadToolBar()
	{
		if(jEdit.getBooleanProperty("view.showToolbar"))
		{
			if(toolBar != null)
				removeToolBar(toolBar);

			toolBar = GUIUtilities.loadToolBar("view.toolbar");
			toolBar.add(Box.createGlue());
			addToolBar(toolBar);
		}
		else if(toolBar != null)
		{
			removeToolBar(toolBar);
			toolBar = null;
		}
	}

	private EditPane createEditPane(EditPane pane, Buffer buffer)
	{
		EditPane editPane = new EditPane(this,pane,buffer);
		JEditTextArea textArea = editPane.getTextArea();
		textArea.addCaretListener(new CaretHandler());
		textArea.addFocusListener(new FocusHandler());
		EditBus.send(new EditPaneUpdate(editPane,EditPaneUpdate.CREATED));
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
			status.updateBufferStatus();
		}
		else
			status.updateCaretStatus();
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
	 * Recreates the recent menu.
	 */
	private void updateRecentMenu()
	{
		if(recent.getMenuComponentCount() != 0)
			recent.removeAll();
		EditAction action = jEdit.getAction("open-file");
		String[] recentArray = jEdit.getRecent();
		if(recentArray.length == 0)
		{
			recent.add(GUIUtilities.loadMenuItem("no-recent"));
			return;
		}
		for(int i = 0; i < recentArray.length; i++)
		{
			String path = recentArray[i];
			EnhancedMenuItem menuItem = new EnhancedMenuItem(path,
				action,path);
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

		Vector macroVector = Macros.getMacroHierarchy();
		createMacrosMenu(macros,macroVector,0);

		if(count == macros.getMenuComponentCount())
			macros.add(GUIUtilities.loadMenuItem("no-macros"));
	}

	private void createMacrosMenu(JMenu menu, Vector vector, int start)
	{
		for(int i = start; i < vector.size(); i++)
		{
			Object obj = vector.elementAt(i);
			if(obj instanceof String)
			{
				menu.add(GUIUtilities.loadMenuItem(
					"play-macro@" + obj));
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
						"no-macros"));
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
		Vector pluginMenuItems = new Vector();

		EditPlugin[] pluginArray = jEdit.getPlugins();
		for(int i = 0; i < pluginArray.length; i++)
		{
			try
			{
				EditPlugin plugin = pluginArray[i];

				// major hack to put 'Plugin Manager' at the
				// top of the 'Plugins' menu
				if(plugin.getClassName().equals("PluginManagerPlugin"))
				{
					Vector vector = new Vector();
					plugin.createMenuItems(vector);
					plugins.add((JMenuItem)vector.elementAt(0));
					plugins.addSeparator();
				}
				else
				{
					// call old API
					plugin.createMenuItems(this,pluginMenuItems,
						pluginMenuItems);

					// call new API
					plugin.createMenuItems(pluginMenuItems);
				}
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,this,"Error creating menu items"
					+ " for plugin");
				Log.log(Log.ERROR,this,t);
			}
		}

		if(pluginMenuItems.isEmpty())
		{
			plugins.add(GUIUtilities.loadMenuItem("no-plugins"));
			return;
		}

		// Sort them
		MiscUtilities.quicksort(pluginMenuItems,new MenuItemCompare());

		JMenu menu = plugins;
		for(int i = 0; i < pluginMenuItems.size(); i++)
		{
			if(menu.getItemCount() >= 20)
			{
				menu.addSeparator();
				JMenu newMenu = new JMenu(jEdit.getProperty(
					"common.more"));
				menu.add(newMenu);
				menu = newMenu;
			}

			menu.add((JMenuItem)pluginMenuItems.elementAt(i));
		}
	}

	private void updateHelpMenu()
	{
		EditAction action = jEdit.getAction("help");

		JMenu menu = help;

		EditPlugin[] plugins = jEdit.getPlugins();
		for(int i = 0; i < plugins.length; i++)
		{
			EditPlugin plugin = plugins[i];
			// don't include broken plugins in list
			// ... why?
			//if(plugin instanceof EditPlugin.Broken)
			//	continue;

			String name = plugin.getClassName();

			String label = jEdit.getProperty("plugin." + name + ".name");
			String docs = jEdit.getProperty("plugin." + name + ".docs");
			if(docs != null)
			{
				java.net.URL docsURL = plugin.getClass().getResource(docs);
				if(label != null && docsURL != null)
				{
					if(menu.getItemCount() >= 20)
					{
						menu.addSeparator();
						JMenu newMenu = new JMenu(
							jEdit.getProperty(
							"common.more"));
						menu.add(newMenu);
						menu = newMenu;
					}

					menu.add(new EnhancedMenuItem(label,
						action,docsURL.toString()));
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
			{
				updateTitle();
				status.updateBufferStatus();
			}
			updateBuffersMenu();
		}
		else if(msg.getWhat() == BufferUpdate.MARKERS_CHANGED)
		{
			if(isOpen(buffer))
				updateMarkerMenus();
		}
	}

	class CaretHandler implements CaretListener
	{
		public void caretUpdate(CaretEvent evt)
		{
			status.repaint();
		}
	}

	class FocusHandler extends FocusAdapter
	{
		public void focusGained(FocusEvent evt)
		{
			// walk up hierarchy, looking for an EditPane
			Component comp = (Component)evt.getSource();
			while(!(comp instanceof EditPane))
			{
				if(comp == null)
					return;

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
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.198  2000/09/23 03:01:09  sp
 * pre7 yayayay
 *
 * Revision 1.197  2000/09/07 04:46:08  sp
 * bug fixes
 *
 * Revision 1.196  2000/09/03 03:16:53  sp
 * Search bar integrated with command line, enhancements throughout
 *
 * Revision 1.195  2000/09/01 11:31:00  sp
 * Rudimentary 'command line', similar to emacs minibuf
 *
 * Revision 1.194  2000/08/31 02:54:00  sp
 * Improved activity log, bug fixes
 *
 * Revision 1.193  2000/08/29 07:47:11  sp
 * Improved complete word, type-select in VFS browser, bug fixes
 *
 * Revision 1.192  2000/08/19 08:26:26  sp
 * More docking API tweaks
 *
 * Revision 1.191  2000/08/17 08:04:09  sp
 * Marker loading bug fixed, docking option pane
 *
 * Revision 1.190  2000/08/13 07:35:22  sp
 * Dockable window API
 *
 * Revision 1.189  2000/07/30 09:04:18  sp
 * More VFS browser hacking
 *
 * Revision 1.188  2000/07/26 07:48:44  sp
 * stuff
 *
 * Revision 1.187  2000/07/22 12:37:38  sp
 * WorkThreadPool bug fix, IORequest.load() bug fix, version wound back to 2.6
 *
 * Revision 1.186  2000/07/21 10:23:49  sp
 * Multiple work threads
 *
 * Revision 1.185  2000/07/19 11:45:18  sp
 * I/O requests can be aborted now
 *
 * Revision 1.184  2000/07/19 08:35:59  sp
 * plugin devel docs updated, minor other changes
 *
 * Revision 1.183  2000/07/14 06:00:44  sp
 * bracket matching now takes syntax info into account
 *
 */
