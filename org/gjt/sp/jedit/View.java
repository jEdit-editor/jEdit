/*
 * View.java - jEdit view
 * Copyright (C) 1998, 1999, 2000, 2001 Slava Pestov
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

		String text = getTextArea().getSelectedText();
		if(text != null && text.indexOf('\n') != -1)
			text = null;

		searchBar.setHyperSearch(false);
		searchBar.getField().setText(text);
		searchBar.getField().selectAll();
		searchBar.getField().requestFocus();
	}

	/**
	 * Quick HyperSearch.
	 * @since jEdit 2.7pre2
	 */
	public void quickHyperSearch()
	{
		if(searchBar == null)
		{
			getToolkit().beep();
			return;
		}

		String text = getTextArea().getSelectedText();
		if(text != null && text.indexOf('\n') != -1)
			text = null;

		searchBar.setHyperSearch(true);
		searchBar.getField().setText(text);
		searchBar.getField().selectAll();
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
	 * Returns the status bar.
	 * @since jEdit 3.2pre2
	 */
	public StatusBar getStatus()
	{
		return status;
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
		setEditPane(createEditPane(oldEditPane.getBuffer()));
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
	 * @since jEdit 2.7pre4
	 */
	public void nextTextArea()
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
	 * @since jEdit 2.7pre4
	 */
	public void prevTextArea()
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
		else if(msg instanceof EditPaneUpdate)
			handleEditPaneUpdate((EditPaneUpdate)msg);
	}

	public JMenu getMenu(String name)
	{
		if(name.equals("recent-files"))
			return recent;
		else if(name.equals("current-directory"))
			return currentDirectory;
		else if(name.equals("markers"))
			return markers;
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
		markers = new MarkersMenu(this);
		macros = GUIUtilities.loadMenu(this,"macros");
		help = GUIUtilities.loadMenu(this,"help-menu");
		plugins = GUIUtilities.loadMenu(this,"plugins");

		// finish persistent splits later
		Component comp = restoreSplitConfig(buffer,"+");
			/* jEdit.getProperty("view.splits") */;
		dockableWindowManager.add(comp);

		updateMacrosMenu();
		updatePluginsMenu();

		EditBus.addToBus(this);

		setJMenuBar(GUIUtilities.loadMenuBar(this,"view.mbar"));

		toolBars = new Box(BoxLayout.Y_AXIS);

		inputHandler = new DefaultInputHandler(this,(DefaultInputHandler)
			jEdit.getInputHandler());

		propertiesChanged();

		getContentPane().add(BorderLayout.NORTH,toolBars);
		getContentPane().add(BorderLayout.CENTER,dockableWindowManager);
		getContentPane().add(BorderLayout.SOUTH,status = new StatusBar(this));

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowHandler());

		dockableWindowManager.init();
	}

	void saveSplitConfig()
	{
		StringBuffer splitConfig = new StringBuffer();
		if(splitPane != null)
			saveSplitConfig(splitPane,splitConfig);
		else
			splitConfig.append('+');
		jEdit.setProperty("view.splits",splitConfig.reverse().toString());
	}

	void close()
	{
		closed = true;

		// save dockable window geometry, and close 'em
		dockableWindowManager.close();

		saveSplitConfig();

		GUIUtilities.saveGeometry(this,"view");
		EditBus.removeFromBus(this);
		dispose();

		EditPane[] editPanes = getEditPanes();
		for(int i = 0; i < editPanes.length; i++)
			editPanes[i].close();

		// null some variables so that retaining references
		// to closed views won't hurt as much.
		recent = currentDirectory = markers
			= macros = plugins = help = null;
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

	// private members
	private boolean closed;

	private DockableWindowManager dockableWindowManager;

	private JMenu recent;
	private JMenu currentDirectory;
	private JMenu markers;
	private JMenu macros;
	private JMenu plugins;
	private JMenu help;

	private Box toolBars;
	private JToolBar toolBar;
	private SearchBar searchBar;

	private boolean synchroScroll;

	private EditPane editPane;
	private JSplitPane splitPane;

	private StatusBar status;

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

	/*
	 * The split config is recorded in a simple RPN "language":
	 * + pushes a new edit pane onto the stack
	 * - pops the two topmost elements off the stack, creates a
	 * vertical split
	 * | pops the two topmost elements off the stack, creates a
	 * horizontal split
	 *
	 * Note that after saveSplitConfig() is called, we have to
	 * reverse the RPN "program" because this method appends
	 * stuff to the end, so the bottom-most nodes end up at the
	 * end
	 */
	private void saveSplitConfig(JSplitPane splitPane,
		StringBuffer splitConfig)
	{
		splitConfig.append(splitPane.getOrientation()
			== JSplitPane.VERTICAL_SPLIT ? '-' : '|');

		Component left = splitPane.getLeftComponent();
		if(left instanceof JSplitPane)
			saveSplitConfig((JSplitPane)left,splitConfig);
		else
			splitConfig.append('+');

		Component right = splitPane.getLeftComponent();
		if(right instanceof JSplitPane)
			saveSplitConfig((JSplitPane)right,splitConfig);
		else
			splitConfig.append('+');
	}

	private Component restoreSplitConfig(Buffer buffer, String splitConfig)
	{
		Stack stack = new Stack();

		for(int i = 0; i < splitConfig.length(); i++)
		{
			switch(splitConfig.charAt(i))
			{
			case '+':
				stack.push(editPane = createEditPane(buffer));
				break;
			case '-':
				stack.push(splitPane = new JSplitPane(
					JSplitPane.VERTICAL_SPLIT,
					(Component)stack.pop(),
					(Component)stack.pop()));
				splitPane.setBorder(null);
				break;
			case '|':
				stack.push(splitPane = new JSplitPane(
					JSplitPane.HORIZONTAL_SPLIT,
					(Component)stack.pop(),
					(Component)stack.pop()));
				splitPane.setBorder(null);
				break;
			}
		}

		return (Component)stack.peek();
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
				toolBars.remove(toolBar);

			toolBar = GUIUtilities.loadToolBar("view.toolbar");
			toolBar.add(Box.createGlue());

			toolBars.add(toolBar,0);
			getRootPane().revalidate();
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

	private EditPane createEditPane(Buffer buffer)
	{
		EditPane editPane = new EditPane(this,buffer);
		JEditTextArea textArea = editPane.getTextArea();
		textArea.addFocusListener(new FocusHandler());
		textArea.addCaretListener(new CaretHandler());
		textArea.addScrollListener(new ScrollHandler());
		EditBus.send(new EditPaneUpdate(editPane,EditPaneUpdate.CREATED));
		return editPane;
	}

	private void setEditPane(EditPane editPane)
	{
		this.editPane = editPane;
		status.repaintCaretStatus();
		status.updateMiscStatus();
	}

	/**
	 * Recreates the recent menu.
	 */
	private void updateRecentMenu()
	{
		if(recent.getMenuComponentCount() != 0)
			recent.removeAll();
		ActionListener actionListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				jEdit.openFile(View.this,evt.getActionCommand());
			}
		};

		MouseListener mouseListener = new MouseAdapter()
		{
			public void mouseEntered(MouseEvent evt)
			{
				status.setMessage(((JMenuItem)evt.getSource())
					.getActionCommand());
			}

			public void mouseExited(MouseEvent evt)
			{
				status.setMessage(null);
			}

			public void mouseClicked(MouseEvent evt)
			{
				status.setMessage(null);
			}
		};

		Vector recentVector = BufferHistory.getBufferHistory();

		if(recentVector.size() == 0)
		{
			recent.add(GUIUtilities.loadMenuItem("no-recent"));
			return;
		}

		/*
		 * While recentVector has 50 entries or so, we only display
		 * a few of those in the menu (otherwise it will be way too
		 * long)
		 */
		int recentFileCount = Math.min(recentVector.size(),
			Integer.parseInt(jEdit.getProperty("history")));

		for(int i = 0; i < recentFileCount; i++)
		{
			String path = ((BufferHistory.Entry)recentVector
				.elementAt(i)).path;
			VFS vfs = VFSManager.getVFSForPath(path);
			JMenuItem menuItem = new JMenuItem(vfs.getFileName(path));
			menuItem.setActionCommand(path);
			menuItem.addActionListener(actionListener);
			menuItem.addMouseListener(mouseListener);
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
					macro.action));
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
		// Query plugins for menu items
		Vector pluginMenuItems = new Vector();

		EditPlugin[] pluginArray = jEdit.getPlugins();
		for(int i = 0; i < pluginArray.length; i++)
		{
			try
			{
				EditPlugin plugin = pluginArray[i];

				// call old API
				int count = pluginMenuItems.size();
				plugin.createMenuItems(this,pluginMenuItems,
					pluginMenuItems);
				if(count != pluginMenuItems.size())
				{
					Log.log(Log.WARNING,this,plugin.getClassName()
						+ " is using the obsolete"
						+ " createMenuItems() API.");
				}

				// call new API
				plugin.createMenuItems(pluginMenuItems);
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
		MiscUtilities.quicksort(pluginMenuItems,
			new MiscUtilities.MenuItemCompare());

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

	private void handleBufferUpdate(BufferUpdate msg)
	{
		Buffer buffer = msg.getBuffer();
		if(msg.getWhat() == BufferUpdate.CLOSED)
			updateRecentMenu();
		else if(msg.getWhat() == BufferUpdate.DIRTY_CHANGED
			|| msg.getWhat() == BufferUpdate.LOADED)
		{
			status.repaintCaretStatus();
			status.updateMiscStatus();

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
	}

	private void handleEditPaneUpdate(EditPaneUpdate msg)
	{
		if(msg.getEditPane().getView() == this
			&& msg.getWhat() == EditPaneUpdate.BUFFER_CHANGED)
		{
			status.repaintCaretStatus();
			status.updateMiscStatus();
		}
	}

	class CaretHandler implements CaretListener
	{
		public void caretUpdate(CaretEvent evt)
		{
			status.repaintCaretStatus();
			status.updateMiscStatus();
			if(evt.getDot() != evt.getMark())
				Registers.setRegister('%',getTextArea().getSelectedText());
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

	class ScrollHandler implements ScrollListener
	{
		public void scrolledVertically(JEditTextArea textArea)
		{
			if(getTextArea() == textArea)
				status.repaintCaretStatus();
		}

		public void scrolledHorizontally(JEditTextArea textArea) {}
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

			Vector buffers = new Vector();
			EditPane[] editPanes = getEditPanes();
			for(int i = 0; i < editPanes.length; i++)
			{
				Buffer buffer = ((EditPane)editPanes[i])
					.getBuffer();
				if(buffers.contains(buffer))
					continue;
				else
					buffers.addElement(buffer);
			}

			for(int i = 0; i < buffers.size(); i++)
				((Buffer)buffers.elementAt(i)).checkModTime(View.this);
		}

		public void windowClosing(WindowEvent evt)
		{
			jEdit.closeView(View.this);
		}
	}
}
