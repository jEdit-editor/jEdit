/*
 * FindAccessory.java - a powerful find accessory for open dialogs
 * (C) 2000 Ken Klinner, portions copyright (C) 2000 Romain Guy
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA, 02111-1307, USA.
 */

package org.gjt.sp.jedit.gui;

import gnu.regexp.*;

import java.io.*;
import java.util.*;
import java.text.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.*;

import java.beans.*;

import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.util.Log;

/**
 * A threaded file search accessory for JFileChooser.
 * <P>
 * Presents JFileChooser users with a tabbed panel interface for
 * specifying file search criteria (search by name and search by date of
 * modification).
 * Finds are performed "in the background" with found files displayed
 * dynamically as they are found. Only one search can be active at
 * a time. FindResults are displayed in a scrolling list within a results
 * tab panel.
 * <P>
 * Findes are performed asynchronously so the user can continue
 * browsing the file system. The user may stop the search at any time.
 * Accepting or cancelling the file chooser or closing the dialog window
 * will automatically stop a search in progress.
 * <P>
 * The starting folder of the search (the search base) is displayed
 * at the top of the accessory panel. The search base dsiplay will
 * not change while a search is running. Thes search base display
 * will change to reflect the current directory of JFileChooser
 * when a search is not running.
 * <P>
 * Changing the search options does not affect a search in progress.
 *
 * @version 1.0, 2000/01/19
 * @author Ken Klinner, kklinner@opiom.com
 */

public class FindAccessory extends JPanel implements Runnable, PropertyChangeListener,
																										 ActionListener, FindProgressCallback
{
	/**
	 Label for this accessory.
	 */
	static public final String ACCESSORY_NAME = jEdit.getProperty("findAccessory.find");

	/**
	Default max number of found items. Prevents overloading results list.
	*/
	static public final int DEFAULT_MAX_SEARCH_HITS = 500;

	/**
	Find start action name
	*/
	static public final String ACTION_START = jEdit.getProperty("findAccessory.start");

	/**
	Find stop action name
	*/
	static public final String ACTION_STOP = jEdit.getProperty("findAccessory.stop");

	/**
	Parent JFileChooser component
	*/
	protected JFileChooser chooser = null;

	protected FindAction actionStart = null;
	protected FindAction actionStop = null;

	/**
	This version of FindAccesory supports only one active search thread
	*/
	protected Thread searchThread = null;

	/**
	Set to true to stop current search
	*/
	protected boolean killFind = false;

	/**
	Find options with results list
	*/
	protected FindTabs searchTabs = null;

	/**
	Find controls with progress display
	*/
	protected FindControls controlPanel = null;

	/**
	Number of items inspected by current/last search
	*/
	protected int total = 0;

	/**
	Number of items found by current/last search
	*/
	protected int matches = 0;

	/**
	Max number of found items to prevent overloading
	the results list.
	*/
	protected int maxMatches = DEFAULT_MAX_SEARCH_HITS;

	/**
	 Construct a search panel with start and stop actions, option panes and a
	 results list pane that can display up to DEFAULT_MAX_SEARCH_HITS items.
	 */
	public FindAccessory()
	{
		super();

		setBorder(new TitledBorder(ACCESSORY_NAME));
		setLayout(new BorderLayout());

		actionStart = new FindAction(ACTION_START, null);
		actionStop = new FindAction(ACTION_STOP, null);

		add(searchTabs = new FindTabs(), BorderLayout.CENTER);
		add(controlPanel = new FindControls(actionStart, actionStop, true),
			BorderLayout.SOUTH);

		setMinimumSize(getPreferredSize());

		updateFindDirectory();
	}

	/**
	 Construct a search panel with start and stop actions and "attach" it to
	 the specified JFileChooser component. Calls register() to establish
	 FindAccessory as a PropertyChangeListener of JFileChooser.

	 @param parent JFileChooser containing this accessory
	 */
	public FindAccessory(JFileChooser parent)
	{
		this();
		chooser = parent;
		register(chooser);
	}

	/**
	 Construct a search panel with start and stop actions and "attach" it to
	 the specified JFileChooser component. Calls register() to establish
	 FindAccessory as a PropertyChangeListener of JFileChooser. Sets maximum
	 number of found items to limit the load in the results list.

	 @param parent JFileChooser containing this accessory
	 @param max Max number of items for results list. Find stops when max
	 number of items found.
	*/
	public FindAccessory(JFileChooser c, int max)
	{
		this(c);
		setMaxFindHits(max);
	}

	/**
	 Sets maximum capacity of the results list.
	 Find stops when max number of items found.

	 @param max Max capacity of results list.
	*/
	public void setMaxFindHits(int max)
	{
		maxMatches = max;
	}

	/**
	 Returns maximum capacity of results list.

	 @return Max capacity of results list.
	*/
	public int getMaxFindHits ()
	{
		return maxMatches;
	}

	/**
	 Called by JFileChooser when a property changes. FindAccessory listens
	 for DIRECTORY_CHANGED_PROPERTY and updates the path component to display
	 the full path of the current JFileChooser directory. When a search is
	 in progress the path component is <b>not</b> updated - the path component
	 will display the starting point of the current search.

	 @param e PropertyChangeEvent from parent JFileChooser.
	*/
	public void propertyChange(PropertyChangeEvent e)
	{
		String prop = e.getPropertyName();
		if (prop.equals(JFileChooser.DIRECTORY_CHANGED_PROPERTY))
		{
			updateFindDirectory();
		}
	}

	/**
	 Called by JFileChooser when the user provokes an action like "cancel"
	 or "open". Listens for APPROVE_SELECTION and CANCEL_SELECTION action
	 and stops the current search, if there is one.

	 @param e ActionEvent from parent JFileChooser.
	*/
	public void actionPerformed(ActionEvent e)
	{
		String command = e.getActionCommand();
		if (command == null)
			return; // Can this happen? Probably not. Call me paranoid.
		if (command.equals(JFileChooser.APPROVE_SELECTION))
			quit();
		else if (command.equals(JFileChooser.CANCEL_SELECTION))
			quit();
	}


	/**
	 Displays the absolute path to the parent's current directory if and only
	 if there is no active search.
	*/
	public void updateFindDirectory()
	{
		if (isRunning())
			return;
		if (chooser == null)
			return;
		File f = chooser.getCurrentDirectory();
	}

	/**
	 Set parent's current directory to the parent folder of the specified
	 file and select the specified file. This method is invoked when the
	 user double clicks on an item in the results list.

	 @param f File to select in parent JFileChooser
	*/
	public void goTo(File f)
	{
		if (f == null)
			return;
		if (!f.exists())
			return;
		if (chooser == null)
			return;

		// Make sure that files and directories can be displayed
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

		// Make sure that parent file chooser will show the type of file
		// specified
		javax.swing.filechooser.FileFilter filter = chooser.getFileFilter();
		if (filter != null)
		{
			if (!filter.accept(f))
			{
				// The current filter will not display the specified file.
				// Set the file filter to the built-in accept-all filter (*.*)
				javax.swing.filechooser.FileFilter all =
					chooser.getAcceptAllFileFilter();
				chooser.setFileFilter(all);
			}
		}

		// Tell parent file chooser to display contents of parentFolder.
		// Prior to Java 1.2.2 setSelectedFile() did not set the current
		// directory the folder containing the file to be selected.
		File parentFolder = new File(f.getParent());
		if (parentFolder != null)
			chooser.setCurrentDirectory(parentFolder);

		// Nullify the current selection, if any.
		// Why is this necessary?
		// Emperical evidence suggests that JFileChooser gets "sticky" (i.e. it
		// does not always relinquish the current selection). Nullifying the
		// current selection seems to yield better results.
		chooser.setSelectedFile(null);

		// Select the file
		chooser.setSelectedFile(f);

		// Refresh file chooser display.
		// Is this really necessary? Testing on a variety of systems with
		// Java 1.2.2 suggests that this helps. Sometimes it doesn't work,
		// but it doesn't do any harm.
		chooser.invalidate();
		chooser.repaint();
	}

	/**
	 Start a search. The path display will show the starting folder of the
	 search.
	 Finds are recursive and will span the entire folder hierarchy below the
	 base folder. The user may continue to browse with JFileChooser.
	*/
	public synchronized void start()
	{
		if (searchTabs != null)
			searchTabs.showFindResults();
		updateFindDirectory();
		killFind = false;
		if (searchThread == null)
		{
			searchThread = new Thread(this);
		}
		if (searchThread != null)
			searchThread.start();
	}

	/**
	 Stop the active search.
	*/
	public synchronized void stop()
	{
		killFind = true;
	}

	/**
	 @return true if a search is currently running
	*/
	public boolean isRunning()
	{
		if (searchThread == null)
			return false;
		return searchThread.isAlive();
	}

	/**
	 Find thread
	*/
	public void run()
	{
		if (searchThread == null)
			return;
		if (Thread.currentThread() != searchThread)
			return;
		try
		{
			actionStart.setEnabled(false);
			actionStop.setEnabled(true);
			runFind(chooser.getCurrentDirectory(), newFind());
		}
		catch (InterruptedException e)
		{
		}
		finally
		{
			actionStart.setEnabled(true);
			actionStop.setEnabled(false);
			searchThread = null;
		}
	}

	/**
	 Recursive search beginning at folder <b>base</b> for files and folders
	 matching each filter in the <b>filters</b> array. To interrupt set
	 <b>killFind</b> to true. Also stops when number of search hits (matches)
	 equals <b>maxMatches</b>.
	 <P>
	 <b>Note:</b> Convert this to a nonrecursive search algorithm on systems
	 where stack space might be limited and/or the search hierarchy might be
	 very deep.

	 @param base starting folder of search
	 @param filters matches must pass each filters in array
	 @exception InterruptedException if thread is interrupted
	*/
	protected void runFind(File base, FindFilter[] filters)
		throws InterruptedException
	{
		if (base == null)
			return;
		if (!base.exists())
			return; // Not likely to happen
		if (filters == null)
			return;

		if (killFind)
			return;
		File folder = null;
		if (base.isDirectory())
			folder = base;
		else
			folder = new File(base.getParent());

		String[] files = folder.list();
		if(files == null)
			return;

		for (int i = 0; i < files.length; i++)
		{
			File file = new File(folder,files[i]);
			total++;
			if (accept(file, filters))
			{
				matches++;
				searchTabs.addFoundFile(file);
			}
			updateProgress();
			if (killFind)
				return;
			Thread.currentThread().sleep(0);

			if (file.isDirectory())
				runFind(file, filters);
			if ((maxMatches > 0) && (matches >= maxMatches))
			{
				return;// stopgap measure so that we don't overload
			}
		}
	}

	/**
	 @param file file to pass to each filter's accept method
	 @param filters array of selection criteria

	 @return true if specified file matches each filter's selection criteria
	*/
	protected boolean accept(File file, FindFilter[] filters)
	{
		if (file == null)
			return false;
		if (filters == null)
			return false;

		for (int i = 0; i < filters.length; i++)
		{
			if (!filters[i].accept(file, this))
				return false;
		}
		return true;
	}

	/**
	 Called by FindFilter to report progress of a search. Purely
	 a voluntary report. This really should be implemented as a
	 property change listener.
	 Percentage completion = (current/total)*100.

	 @param filter FindFilter reporting progress
	 @param file file being searched
	 @param current current "location" of search
	 @param total expected maximum value of current

	 @return true to continue search, false to abort
	*/
	public boolean reportProgress(FindFilter filter, File file,
		long current, long total)
	{
		return !killFind;
	}

	/**
	 Begins a new search by resetting the <b>total</b> and <b>matches</b>
	 progress variables and retrieves the search filter array from the
	 options panel.
	 Each tab in the options panel is responsible for generating a
	 FindFilter based on its current settings.

	 @return Array of search filters from the options panel.
	*/
	protected FindFilter[] newFind()
	{

		total = matches = 0;
		updateProgress();

		if (searchTabs != null)
			return searchTabs.newFind();
		return null;
	}

	/**
	 Display progress of running search.
	*/
	protected void updateProgress()
	{
		controlPanel.showProgress(matches, total);
	}


	/**
	 Add this component to the specified JFileChooser's list of property
	 change listeners and action listeners.

	 @param c parent JFileChooser
	*/
	protected void register(JFileChooser c)
	{
		if (c == null)
			return;
		c.addPropertyChangeListener(this);
		c.addActionListener(this);
	}

	/**
	 Remove this component from the specified JFileChooser's list of property
	 change listeners and action listeners.

	 @param c parent JFileChooser
	*/
	protected void unregister(JFileChooser c)
	{
		if (c == null)
			return;
		c.removeActionListener(this);
		c.removePropertyChangeListener(this);
	}

	/**
	 Stop the current search and unregister in preparation for parent
	 shutdown.
	*/
	public void quit()
	{
		stop();
	}


	/**
	 Invoked by FindAction objects to start and stop searches.
	*/
	public void action(String command)
	{
		if (command == null)
			return;
		if (command.equals(ACTION_START))
			start();
		else if (command.equals(ACTION_STOP))
			stop();
	}

	/**
	 Convenience class for adding action objects to the control panel.
	 */
	class FindAction extends AbstractAction
	{

		/**
		 Construct a search control action currently implements
		 FindAccesory.ACTION_START and FindAccessory.ACTION_STOP.

		 @param text command
		 @param icon button icon
		 */
		FindAction(String text, Icon icon)
		{
			super(text, icon);
		}

		/**
		 Invoke FindAction's action() method.

		 @param e action event
		*/
		public void actionPerformed(ActionEvent e)
		{
			action(e.getActionCommand());
		}
	}

	/**
	 Find controls panel displays default action components for starting
	 and stopping a search. Also displays the search progress in the form of
	 a text display indicating the number of items found and the total number
	 of items encountered in the search.
	 */
	class FindControls extends JPanel
	{
		protected JLabel progress = null;

		/**
		 Construct a simple search control panel with buttons for
		 starting and stopping a search and a simple display for
		 search progress.
		*/
		FindControls(FindAction find, FindAction stop, boolean recurse)
		{
			super();
			setLayout(new BorderLayout());

			JToolBar tools = new JToolBar();
			tools.setFloatable(false);
			tools.add(actionStart = new FindAction(ACTION_START, null));
			tools.add(actionStop = new FindAction(ACTION_STOP, null));
			add(tools, BorderLayout.WEST);

			progress = new JLabel("",SwingConstants.RIGHT);

			// So that frequent updates will appear smooth
			progress.setDoubleBuffered(true);

			add(progress, BorderLayout.EAST);
		}

		/**
		 Display search progress as a text field
		 "no. of matches / total searched".
		 @param matches number of items found
		 @param total number of items investigated
		 */
		public void showProgress(int matches, int total)
		{
			if (progress == null)
				return;
			progress.setText(String.valueOf(matches) + "/" +
				String.valueOf(total));
		}

	}

	/**
	 Contains a collecton of search options displayed as tabbed panes and
	 at least one pane for displaying the search results. Each options tab
	 pane is a user interface for sprecifying the search criteria and a
	 factory for a FindFilter to implement the acceptance function. By making
	 the search option pane responsible for generating a FindFilter object,
	 the programmer can easily extend the search capabilities without
	 modifying the controlling search engine.
	 */
	class FindTabs extends JTabbedPane
	{
		protected String TAB_NAME = jEdit.getProperty("findAccessory.name"); //"Name";
		protected String TAB_DATE = jEdit.getProperty("findAccessory.date"); //"Date";
		protected String TAB_RESULTS = jEdit.getProperty("findAccessory.found"); //"Found";

		protected FindResults resultsPanel = null;
		protected JScrollPane resultsScroller = null;


		/**
		 Construct a search tabbed pane with tab panels for seach by
		 filename, search by date, and search results.
		*/
		FindTabs()
		{
			super();

			// Add search-by-name panel
			addTab(TAB_NAME, new FindByName());

			// Add search-by-date panel
			addTab(TAB_DATE, new FindByDate());

			// Add results panel
			resultsScroller = new JScrollPane(resultsPanel = new FindResults());

			// so that updates will be smooth
			resultsPanel.setDoubleBuffered(true);
			resultsScroller.setDoubleBuffered(true);

			addTab(TAB_RESULTS, resultsScroller);
		}

		/**
		 Adds the specified file to the results list.

		 @param f file to add to results list
		 */
		public void addFoundFile(File f)
		{
			if (resultsPanel != null)
				resultsPanel.append(f);
		}

		/**
		 Bring the search results tab panel to the front.
		 */
		public void showFindResults()
		{
			if (resultsScroller != null)
				setSelectedComponent(resultsScroller);
		}

		/**
		 Prepares the panel for a new search by clearing the results list,
		 bringing the results tab panel to the front and generating an
		 array of search filters for each search options pane that
		 implements the FindFilterFactory interface.

		 @return array of FindFilters to be used by the controlling
		 search engine
		 */
		public FindFilter[] newFind()
		{
			// Clear the results display
			if (resultsPanel != null)
				resultsPanel.clear();

			// Fix the width of the scrolling results panel so the layout
			// managers don't try to make it too wide for JFileChooser
			Dimension dim = resultsScroller.getSize();
			//resultsScroller.setMaximumSize(dim);
			resultsScroller.setPreferredSize(dim);

			// Return an array of FindFilters
			Vector filters = new Vector();
			for (int i = 0; i < getTabCount(); i++)
			{
				try
				{
					FindFilterFactory fac = (FindFilterFactory) getComponentAt(i);
					FindFilter f = fac.createFindFilter();
					if (f != null)
						filters.addElement(f);
				}
				catch (Throwable e)
				{
					// The FindResults pane does not implement FindFilterFactory
				}
			}
			if (filters.size() == 0)
				return null;
			FindFilter[] filterArray = new FindFilter[filters.size()];
			for (int i = 0; i < filterArray.length; i++)
			{
				filterArray[i] = (FindFilter) filters.elementAt(i);
			}
			return filterArray;
		}
	}

	/**
	 Appears as a special pane within the FindOptions tabbed panel.
	 The only one that does not generate a FindFilter.
	 */
	class FindResults extends JPanel
	{
		protected DefaultListModel model = null;
		protected JList fileList = null;


		/**
		Construct a search results pane with a scrollable list of files.
		When an item is double-clicked the FindAccessory controller will
		be instructed to select the file in the parent JFileChooser's item
		display.
		*/
		FindResults()
		{
			super();
			setLayout(new BorderLayout());

			model = new DefaultListModel();
			fileList = new JList(model);
			fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			fileList.setCellRenderer(new FindResultsCellRenderer());
			add(fileList, BorderLayout.CENTER);

			// Double click listener
			MouseListener mouseListener = new MouseAdapter()
			{
				public void mouseClicked(MouseEvent e)
				{
					if (e.getClickCount() == 2)
					{
						try
						{
							int index =fileList.locationToIndex(
								e.getPoint());
							File f = (File) model.elementAt(index);
							goTo(f);
						}
						catch (Throwable err)
						{
						}
					}
				}
			};
			fileList.addMouseListener(mouseListener);
		}

		/**
		 Add a file to the results list.

		 @param f file found
		 */
		public void append(File f)
		{
			if (f == null)
				return;
			model.addElement(f);
		}

		/**
		 Clear all items from the results list.
		 */
		public void clear()
		{
			if (model != null)
			{
				model.removeAllElements();
				invalidate();
				repaint();
			}
		}

		/**
		 Convenience class for rendering cells in the results list.
		 */
		class FindResultsCellRenderer extends DefaultListCellRenderer
		{
			public Component getListCellRendererComponent(JList list,
				Object value, int index, boolean isSelected,
				boolean cellHasFocus)
			{
				super.getListCellRendererComponent(list,value,
					index,isSelected,cellHasFocus);

				if (index == -1)
				{
					// This shouldn't happen since we won't be using this
					// renderer in a combo box
					int selected = list.getSelectedIndex();
					if (selected == -1)
						return this;
					else
						index = selected;
				}

				// show absolute path of file
				File file = (File) model.elementAt(index);
				setText(file.getAbsolutePath());

				return this;
			}

		}
	}
}


/**
 Each search option tab that implements FindFilterFactory defines an
 inner class that implements FindFilter. When a search is started
 the search panel invokes createFindFilter() on each panel that
 implements FindFilterFactory, thus causing the panel to create
 a FindFilter object that implements its search settings.
 */
interface FindFilter
{
	//public boolean accept (File f);
	boolean accept(File f, FindProgressCallback monitor);
}

interface FindProgressCallback
{
	/**
	 Should be called by all time-consuming search filters at a reasonable
	 interval. Allows the search controller to report progress and to
	 abort the search in a clean and timely way.
	 @param filter FindFilter reporting the progress
	 @param file the file being searched
	 @param current current "location" of search
	 @param total maximum value
	 @return true if search should continue, false to abort
	 */
	boolean reportProgress(FindFilter filter, File file,
					long current, long total);
}

/**
 Implemented by each search option panel. Each panel is responsible for
 creating a FindFilter object that implements the search criteria
 specified by its user interface.
 */
interface FindFilterFactory
{
	FindFilter createFindFilter();
}

/**
 Implements a user interface and generates FindFilter for selecting
 files by date.
 */
class FindByDate extends JPanel implements FindFilterFactory
{
	public static String THE_BIG_BANG = jEdit.getProperty("findAccessory.bang"); //"The Big Bang";
	public static String THE_BIG_CRUNCH = jEdit.getProperty("findAccessory.crunch"); //"The Big Crunch";
	public static String YESTERDAY = jEdit.getProperty("findAccessory.yesterday"); //"Yesterday";
	public static String TODAY = jEdit.getProperty("findAccessory.today"); //"Today";
	public static String NOW = jEdit.getProperty("findAccessory.now"); //"Now";

	public static String MODIFIED_LABEL = jEdit.getProperty("findAccessory.modified"); //"Modified";
	public static String FORMAT_LABEL = jEdit.getProperty("findAccessory.format"); //"mm/dd/yyyy";
	public static String FROM_DATE_LABEL = jEdit.getProperty("findAccessory.from"); //"between start of";
	public static String TO_DATE_LABEL = jEdit.getProperty("findAccessory.to"); //"and end of";

	protected JComboBox fromDateField = null;
	protected JComboBox toDateField = null;

	protected String[] fromDateItems = {THE_BIG_BANG, YESTERDAY, TODAY};
	protected String[] toDateItems = {THE_BIG_CRUNCH, TODAY, NOW, YESTERDAY};


	FindByDate()
	{
		super();
		setLayout(new BorderLayout());

		// Grid Layout
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());

		JPanel labels = new JPanel(new GridLayout(3,1));
		JPanel combos = new JPanel(new GridLayout(3,1));

		// Date selection criteria
		JLabel modified = new JLabel(MODIFIED_LABEL, SwingConstants.LEFT);
		labels.add(modified);

		// format note
		JLabel format = new JLabel(FORMAT_LABEL, SwingConstants.LEFT);
		combos.add(format);

		// between
		JLabel betweenLabel = new JLabel(FROM_DATE_LABEL, SwingConstants.RIGHT);
		labels.add(betweenLabel);

		// from date
		//fromDateField = new JTextField(8);
		fromDateField = new JComboBox(fromDateItems);
		fromDateField.setEditable(true);
		combos.add(fromDateField);

		// and
		JLabel andLabel = new JLabel(TO_DATE_LABEL, SwingConstants.RIGHT);
		labels.add(andLabel);

		//toDateField = new JTextField(8);
		toDateField = new JComboBox(toDateItems);
		toDateField.setEditable(true);
		combos.add(toDateField);

		p.add(labels, BorderLayout.WEST);
		p.add(combos, BorderLayout.CENTER);
		add(p, BorderLayout.NORTH);
	}

	/**
	 Generate a search filter object based on the setting of this UI
	 component.

	 @return a FindFilter object that implements the selection criteria
	 */
	public FindFilter createFindFilter()
	{
		long from = -1;
		long to = -1;

		from = startDateToTime((String) fromDateField.getSelectedItem());
		to = endDateToTime((String) toDateField.getSelectedItem());

		return new DateFilter(from, to);
	}

	/**
	 Convenience method for converting the start date text to milliseconds
	 since January 1, 1970.

	 @return milliseconds since January 1, 1970
	 */
	protected long startDateToTime(String s)
	{
		if (s == null)
			return -1;
		SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
		Date d = formatter.parse(s, new ParsePosition(0));
		if (d == null)
		{
			if (s.equalsIgnoreCase(TODAY))
			{
				String today = formatter.format(new Date());
				d = formatter.parse(today, new ParsePosition(0));
			}
			else if (s.equalsIgnoreCase(YESTERDAY))
			{
				String yesterday = formatter.format(
								new Date(new Date().getTime() - 24 * 60 * 60 * 1000));
				d = formatter.parse(yesterday, new ParsePosition(0));
			}
			else if (s.equalsIgnoreCase(THE_BIG_BANG))
			{
				return 0; // Not exactly the beginning of time, but
				//close enough for computer work
			}
		}
		if (d != null)
			return d.getTime();
		return -1;
	}

	/**
	 Convenience method for converting the end date text to milliseconds
	 since January 1, 1970. The end time is the end of the specified day.

	 @return milliseconds since January 1, 1970
	 */
	protected long endDateToTime(String s)
	{
		if (s == null)
			return -1;
		SimpleDateFormat dateFormatter = new SimpleDateFormat("MM/dd/yyyy");

		long time = -1;
		Date d = dateFormatter.parse(s, new ParsePosition(0));
		if (d == null)
		{
			if (s.equalsIgnoreCase(TODAY))
			{
				String today = dateFormatter.format(new Date());
				d = dateFormatter.parse(today, new ParsePosition(0));
				if (d != null)
					time = d.getTime() + (24L * 3600L * 1000L);
			}
			else if (s.equalsIgnoreCase(YESTERDAY))
			{
				String yesterday = dateFormatter.format(
								new Date(new Date().getTime() - 24 * 60 * 60 * 1000));
				d = dateFormatter.parse(yesterday, new ParsePosition(0));
				if (d != null)
					time = d.getTime() + (24L * 3600L * 1000L);
			}
			else if (s.equalsIgnoreCase(NOW))
			{
				d = new Date();
				if (d != null)
					time = d.getTime();
			}
			else if (s.equalsIgnoreCase(THE_BIG_CRUNCH))
			{
				time = Long.MAX_VALUE;
			}
		}
		else
		{
			// Valid date. Now add 24 hours to make sure that the
			// date is inclusive
			time = d.getTime() + (24L * 3600L * 1000L);
		}

		return time;
	}

	/**
	 Filter object for selecting files by the date range specified by the UI.
	 */
	class DateFilter implements FindFilter
	{
		protected long startTime = -1;
		protected long endTime = -1;


		DateFilter(long from, long to)
		{
			startTime = from;
			endTime = to;
		}

		public boolean accept(File f, FindProgressCallback callback)
		{
			if (f == null)
				return false;

			long t = f.lastModified();

			if (startTime >= 0)
			{
				if (t < startTime)
					return false;
			}
			if (endTime >= 0)
			{
				if (t > endTime)
					return false;
			}

			return true;
		}
	}

}


/**
 Implements user interface and generates FindFilter for selecting
 files by name.
 */
class FindByName extends JPanel implements FindFilterFactory
{
	protected String NAME_PATTERN = jEdit.getProperty("findAccessory.pattern"); //"pattern";
	protected String NAME_CONTAINS = jEdit.getProperty("findAccessory.contains"); //"contains";
	protected String NAME_IS = jEdit.getProperty("findAccessory.is"); //"is";
	protected String NAME_STARTS_WITH = jEdit.getProperty("findAccessory.starts"); //"starts with";
	protected String NAME_ENDS_WITH = jEdit.getProperty("findAccessory.ends"); //"ends with";
	protected int NAME_PATTERN_INDEX = 0;
	protected int NAME_CONTAINS_INDEX = 1;
	protected int NAME_IS_INDEX = 2;
	protected int NAME_STARTS_WITH_INDEX = 3;
	protected int NAME_ENDS_WITH_INDEX = 4;
	protected String[] criteria = {NAME_PATTERN, NAME_CONTAINS, NAME_IS,
	NAME_STARTS_WITH, NAME_ENDS_WITH};

	protected JTextField nameField = null;
	protected JComboBox combo = null;
	protected JCheckBox ignoreCaseCheck = null;

	FindByName()
	{
		super();
		setLayout(new BorderLayout());

		// Grid Layout
		JPanel p = new JPanel();
		p.setLayout(new GridLayout(2,2));

		// Name
		combo = new JComboBox(criteria);
		//combo.setPreferredSize(combo.getPreferredSize());
		p.add(combo);

		nameField = new JTextField();
		Box box = new Box(BoxLayout.Y_AXIS);
		box.add(Box.createGlue());
		box.add(nameField);
		box.add(Box.createGlue());
		p.add(box);

		// ignore case
		p.add(new JLabel("",SwingConstants.RIGHT));

		ignoreCaseCheck = new JCheckBox(jEdit.getProperty("findAccessory.ignorecase"),true);
		p.add(ignoreCaseCheck);

		add(p, BorderLayout.NORTH);
	}

	public FindFilter createFindFilter()
	{
		return new NameFilter(nameField.getText(),
			combo.getSelectedIndex(), ignoreCaseCheck.isSelected());
	}

	/**
	 	Filter object for selecting files by name.
	 */
	class NameFilter implements FindFilter
	{
		protected String match = null;
		protected RE matchRE = null;
		protected int howToMatch = -1;
		protected boolean ignoreCase = true;

		NameFilter(String name, int how, boolean ignore)
		{
			match = name;
			if(how == NAME_PATTERN_INDEX)
			{
				try
				{
					matchRE = new RE(MiscUtilities.globToRE(name));
				}
				catch(REException re)
				{
					Log.log(Log.ERROR,this,re);
				}
			}
			howToMatch = how;
			ignoreCase = ignore;
		}

		public boolean accept(File f, FindProgressCallback callback)
		{
			if (f == null)
				return false;

			if ((match == null) || (match.length() == 0))
				return true;
			if (howToMatch < 0)
				return true;

			String filename = f.getName();

			if (howToMatch == NAME_PATTERN_INDEX)
			{
				return matchRE.isMatch(filename);
			}
			else if (howToMatch == NAME_CONTAINS_INDEX)
			{
				if (ignoreCase)
				{
					if (filename.toLowerCase().indexOf(match.toLowerCase()) >= 0)
						return true;
					else
						return false;
				}
				else
				{
					if (filename.indexOf(match) >= 0)
						return true;
					else
						return false;
				}
			}
			else if (howToMatch == NAME_IS_INDEX)
			{
				if (ignoreCase)
				{
					if (filename.equalsIgnoreCase(match))
						return true;
					else
						return false;
				}
				else
				{
					if (filename.equals(match))
						return true;
					else
						return false;
				}
			}
			else if (howToMatch == NAME_STARTS_WITH_INDEX)
			{
				if (ignoreCase)
				{
					if (filename.toLowerCase().startsWith(match.toLowerCase()))
						return true;
					else
						return false;
				}
				else
				{
					if (filename.startsWith(match))
						return true;
					else
						return false;
				}
			}
			else if (howToMatch == NAME_ENDS_WITH_INDEX)
			{
				if (ignoreCase)
				{
					if (filename.toLowerCase().endsWith(match.toLowerCase()))
						return true;
					else
						return false;
				}
				else
				{
					if (filename.endsWith(match))
						return true;
					else
						return false;
				}
			}

			return true;
		}
	}

}

/*
 * Change Log:
 * $Log$
 * Revision 1.2  2000/05/09 10:51:52  sp
 * New status bar, a few other things
 *
 * Revision 1.1  2000/05/08 11:20:08  sp
 * New file finder in open dialog box
 *
 */
