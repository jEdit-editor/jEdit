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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.search.SearchBar;
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
	 * Sets if the 'macro recording' message should be displayed.
	 * @since jEdit 2.7pre1
	 */
	public void setRecordingStatus(boolean recording)
	{
		if(recording)
			this.recording.setText(jEdit.getProperty("view.status.recording"));
		else
			this.recording.setText(null);
	}

	/**
	 * Quick search.
	 * @since jEdit 2.7pre2
	 */
	public void quickIncrementalSearch()
	{
		if(searchBar == null)
		{
			getToolkit().beep();
			return;
		}

		searchBar.setBatch(false);
		searchBar.getField().setText(getTextArea().getSelectedText());
		searchBar.getField().requestFocus();
	}

	/**
	 * Quick batch search.
	 * @since jEdit 2.7pre2
	 */
	public void quickBatchSearch()
	{
		if(searchBar == null)
		{
			getToolkit().beep();
			return;
		}

		searchBar.setBatch(true);
		searchBar.getField().setText(getTextArea().getSelectedText());
		searchBar.getField().requestFocus();
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
	 * Returns the listener that will handle all key events in this
	 * view, if any.
	 */
	public KeyListener getKeyEventInterceptor()
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
	public InputHandler getInputHandler()
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
	 * Returns the macro recorder.
	 */
	public Macros.Recorder getMacroRecorder()
	{
		return recorder;
	}

	/**
	 * Sets the macro recorder.
	 * @param recorder The macro recorder
	 */
	public void setMacroRecorder(Macros.Recorder recorder)
	{
		this.recorder = recorder;
	}

	/**
	 * Splits the view horizontally.
	 * @since jEdit 2.7pre2
	 */
	public void splitHorizontally()
	{
		split(JSplitPane.VERTICAL_SPLIT);
	}

	/**
	 * Splits the view vertically.
	 * @since jEdit 2.7pre2
	 */
	public void splitVertically()
	{
		split(JSplitPane.HORIZONTAL_SPLIT);
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
			}
		});
	}

	/**
	 * Moves keyboard focus to the next text area.
	 * @since jEdit 2.7pre2
	 */
	public void nextSplit()
	{
		EditPane[] editPanes = getEditPanes();
		for(int i = 0; i < editPanes.length; i++)
		{
			if(editPane == editPanes[i])
			{
				if(i == editPanes.length - 1)
					editPanes[0].focusOnTextArea();
				else
					editPanes[i+1].focusOnTextArea();
				break;
			}
		}
	}

	/**
	 * Moves keyboard focus to the previous text area.
	 * @since jEdit 2.7pre2
	 */
	public void prevSplit()
	{
		EditPane[] editPanes = getEditPanes();
		for(int i = 0; i < editPanes.length; i++)
		{
			if(editPane == editPanes[i])
			{
				if(i == 0)
					editPanes[editPanes.length - 1].focusOnTextArea();
				else
					editPanes[i-1].focusOnTextArea();
				break;
			}
		}
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
	 * Updates the borders of all gutters in this view to reflect the
	 * currently focused text area.
	 * @since jEdit 2.6final
	 */
	public void updateGutterBorders()
	{
		EditPane[] editPanes = getEditPanes();
		for(int i = 0; i < editPanes.length; i++)
			editPanes[i].getTextArea().getGutter().updateBorder();
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
	public boolean isClosed()
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
	 * Returns if synchronized scrolling is enabled.
	 * @since jEdit 2.7pre1
	 */
	public boolean isSynchroScrollEnabled()
	{
		return synchroScroll;
	}

	/**
	 * Toggles synchronized scrolling.
	 * @since jEdit 2.7pre2
	 */
	public void toggleSynchroScrollEnabled()
	{
		setSynchroScrollEnabled(!synchroScroll);
	}

	/**
	 * Sets synchronized scrolling.
	 * @since jEdit 2.7pre1
	 */
	public void setSynchroScrollEnabled(boolean synchroScroll)
	{
		this.synchroScroll = synchroScroll;
		JEditTextArea textArea = getTextArea();
		int firstLine = textArea.getFirstLine();
		int horizontalOffset = textArea.getHorizontalOffset();
		synchroScrollVertical(textArea,firstLine);
		synchroScrollHorizontal(textArea,horizontalOffset);
	}

	/**
	 * Sets the first line of all text areas.
	 * @param textArea The text area that is propagating this change
	 * @param firstLine The first line
	 * @since jEdit 2.7pre1
	 */
	public void synchroScrollVertical(JEditTextArea textArea, int firstLine)
	{
		if(!synchroScroll)
			return;

		EditPane[] editPanes = getEditPanes();
		for(int i = 0; i < editPanes.length; i++)
		{
			if(editPanes[i].getTextArea() != textArea)
				editPanes[i].getTextArea()._setFirstLine(firstLine);
		}
	}

	/**
	 * Sets the horizontal offset of all text areas.
	 * @param textArea The text area that is propagating this change
	 * @param horizontalOffset The horizontal offset
	 * @since jEdit 2.7pre1
	 */
	public void synchroScrollHorizontal(JEditTextArea textArea, int horizontalOffset)
	{
		if(!synchroScroll)
			return;

		EditPane[] editPanes = getEditPanes();
		for(int i = 0; i < editPanes.length; i++)
		{
			if(editPanes[i].getTextArea() != textArea)
				editPanes[i].getTextArea()._setHorizontalOffset(horizontalOffset);
		}
	}

	/**
	 * Returns the next view in the list.
	 */
	public View getNext()
	{
		return next;
	}

	/**
	 * Returns the previous view in the list.
	 */
	public View getPrev()
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
		if(name.equals("recent-files"))
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
		if(isClosed())
			return;

		// JTextComponents don't consume events...
		if(getFocusOwner() instanceof JTextComponent)
		{
			// fix for the bug where key events in JTextComponents
			// inside views are also handled by the input handler
			if(evt.getID() == KeyEvent.KEY_PRESSED)
			{
				switch(evt.getKeyCode())
				{
				case KeyEvent.VK_BACK_SPACE:
				case KeyEvent.VK_TAB:
				case KeyEvent.VK_ENTER:
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
		recent = GUIUtilities.loadMenu(this,"recent-files");
		currentDirectory = new CurrentDirectoryMenu(this);
		clearMarker = GUIUtilities.loadMenu(this,"clear-marker");
		gotoMarker = GUIUtilities.loadMenu(this,"goto-marker");
		macros = GUIUtilities.loadMenu(this,"macros");
		help = GUIUtilities.loadMenu(this,"help-menu");
		plugins = GUIUtilities.loadMenu(this,"plugins");

		editPane = createEditPane(null,buffer);

		updateMarkerMenus();
		updateMacrosMenu();
		updatePluginsMenu();
		updateHelpMenu();

		EditBus.addToBus(this);

		JMenuBar menuBar = GUIUtilities.loadMenuBar(this,"view.mbar");
		menuBar.add(Box.createGlue());
		menuBar.add(new MiniIOProgress());
		menuBar.add(recording = new JLabel());
		setJMenuBar(menuBar);

		toolBars = new Box(BoxLayout.Y_AXIS);

		inputHandler = new DefaultInputHandler(this,(DefaultInputHandler)
			jEdit.getInputHandler());

		propertiesChanged();

		getContentPane().add(BorderLayout.NORTH,toolBars);
		getContentPane().add(BorderLayout.CENTER,dockableWindowManager);

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
		recent = currentDirectory = clearMarker
			= gotoMarker = macros = plugins = help = null;
		toolBars = null;
		toolBar = null;
		searchBar = null;
		splitPane = null;
		inputHandler = null;
		recorder = null;

		setContentPane(new JPanel());
	}

	/**
	 * Updates the title bar.
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
	 * Recreates the goto marker and clear marker menus.
	 */
	void updateMarkerMenus()
	{
		if(clearMarker.getMenuComponentCount() != 0)
			clearMarker.removeAll();
		if(gotoMarker.getMenuComponentCount() != 0)
			gotoMarker.removeAll();

		ActionListener clearMarkerAction = new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				Buffer buffer = getBuffer();
				if(buffer.isReadOnly())
					getToolkit().beep();
				buffer.removeMarker(evt.getActionCommand());
			}
		};

		ActionListener gotoMarkerAction = new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				JEditTextArea textArea = getTextArea();
				Marker marker = getBuffer().getMarker(evt.getActionCommand());
				if(marker != null)
					textArea.select(marker.getStart(),marker.getEnd());
			}
		};

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
			JMenuItem menuItem = new JMenuItem(name);
			menuItem.addActionListener(clearMarkerAction);
			clearMarker.add(menuItem);

			menuItem = new JMenuItem(name);
			menuItem.addActionListener(gotoMarkerAction);
			gotoMarker.add(menuItem);
		}
	}

	// private members
	private boolean closed;

	private DockableWindowManager dockableWindowManager;

	private JMenu recent;
	private JMenu currentDirectory;
	private JMenu clearMarker;
	private JMenu gotoMarker;
	private JMenu macros;
	private JMenu plugins;
	private JMenu help;

	private JLabel recording;

	private Box toolBars;
	private JToolBar toolBar;
	private SearchBar searchBar;

	private boolean synchroScroll;

	private EditPane editPane;
	private JSplitPane splitPane;

	private KeyListener keyEventInterceptor;
	private InputHandler inputHandler;
	private Macros.Recorder recorder;

	private int waitCount;

	private boolean showFullPath;

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
		updateTitle();

		updateRecentMenu();

		dockableWindowManager.propertiesChanged();
	}

	private void loadToolBars()
	{
		if(jEdit.getBooleanProperty("view.showToolbar"))
		{
			if(toolBar != null)
			{
				Component[] components = toolBars.getComponents();
				for(int i = 0; i < components.length; i++)
				{
					if(components[i] == toolBar)
					{
						toolBars.remove(toolBar);
						toolBar = GUIUtilities.loadToolBar("view.toolbar");
						toolBar.add(Box.createGlue());
						toolBars.add(toolBar,i);
						getRootPane().revalidate();
						return;
					}
				}
			}
			else
			{
				toolBar = GUIUtilities.loadToolBar("view.toolbar");
				toolBar.add(Box.createGlue());
				addToolBar(toolBar);
			}
		}
		else if(toolBar != null)
		{
			removeToolBar(toolBar);
			toolBar = null;
		}

		if(jEdit.getBooleanProperty("view.showSearchbar"))
		{
			if(searchBar == null)
			{
				searchBar = new SearchBar(this);
				addToolBar(searchBar);
			}
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
		JEditTextArea textArea = editPane.getTextArea();
		textArea.addFocusListener(new FocusHandler());
		EditBus.send(new EditPaneUpdate(editPane,EditPaneUpdate.CREATED));
		return editPane;
	}

	private void setEditPane(EditPane editPane)
	{
		EditPane oldPane = this.editPane;
		this.editPane = editPane;
		if(oldPane.getBuffer() != editPane.getBuffer())
			updateMarkerMenus();
	}

	/**
	 * Recreates the recent menu.
	 */
	private void updateRecentMenu()
	{
		if(recent.getMenuComponentCount() != 0)
			recent.removeAll();
		ActionListener listener = new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				jEdit.openFile(View.this,evt.getActionCommand());
			}
		};

		Vector recentVector = BufferHistory.getBufferHistory();
		if(recentVector.size() == 0)
		{
			recent.add(GUIUtilities.loadMenuItem("no-recent"));
			return;
		}

		for(int i = 0; i < recentVector.size(); i++)
		{
			String path = ((BufferHistory.Entry)recentVector
				.elementAt(i)).path;
			VFS vfs = VFSManager.getVFSForPath(path);
			JMenuItem menuItem = new JMenuItem(
				MiscUtilities.getFileName(path) + " ("
				+ vfs.getParentOfPath(path) + ")");
			menuItem.setActionCommand(path);
			menuItem.addActionListener(listener);
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
			if(obj instanceof Macros.Macro)
			{
				Macros.Macro macro = (Macros.Macro)obj;
				String label = macro.name;
				int index = label.lastIndexOf('/');
				label = label.substring(index + 1)
					.replace('_',' ');

				menu.add(new EnhancedMenuItem(label,
					macro.action,null));
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
		ActionListener listener = new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				try
				{
					HelpViewer.gotoURL(new URL(evt.getActionCommand()));
				}
				catch(MalformedURLException e)
				{
					Log.log(Log.ERROR,View.this,e);
				}
			}
		};

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

					JMenuItem menuItem = new JMenuItem(label);
					menuItem.setActionCommand(docsURL.toString());
					menuItem.addActionListener(listener);
					menu.add(menuItem);
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
		if(msg.getWhat() == BufferUpdate.CLOSED)
			updateRecentMenu();
		else if(msg.getWhat() == BufferUpdate.DIRTY_CHANGED
			|| msg.getWhat() == BufferUpdate.LOADED)
		{
			if(!buffer.isDirty())
			{
				// have to update title after each save
				// in case it was a 'save as'
				EditPane[] editPanes = getEditPanes();
				for(int i = 0; i < editPanes.length; i++)
				{
					if(editPanes[i].getBuffer() == buffer)
					{
						updateTitle();
						break;
					}
				}
			}
		}
		else if(msg.getWhat() == BufferUpdate.MARKERS_CHANGED)
		{
			if(buffer == getBuffer())
				updateMarkerMenus();
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

			editPane.getBuffer().checkModTime(View.this);
		}

		public void windowClosing(WindowEvent evt)
		{
			jEdit.closeView(View.this);
		}
	}
}
