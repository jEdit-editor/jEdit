/*
 * GUIUtilities.java - Various GUI utility functions
 * Copyright (C) 1999, 2000 Slava Pestov
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

import gnu.regexp.REException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.*;
import java.util.Hashtable;
import java.util.StringTokenizer;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.syntax.SyntaxStyle;
import org.gjt.sp.util.Log;

/**
 * Class with several useful GUI functions.<p>
 *
 * It provides methods for:
 * <ul>
 * <li>Loading menu bars, menus and menu items from the properties
 * <li>Loading popup menus from the properties
 * <li>Loading tool bars and tool bar buttons from the properties
 * <li>Displaying various common dialog boxes
 * <li>Converting string representations of colors to color objects
 * <li>Loading and saving window geometry from the properties
 * <li>Displaying file open and save dialog boxes
 * <li>Loading images and caching them
 * </ul>
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class GUIUtilities
{
	/**
	 * Instructs jEdit to invalidate all menu models.
	 * @param name The menu name
	 */
	public static void invalidateMenuModels()
	{
		menus.clear();
	}

	/**
	 * Loads a menubar model.
	 * @param name The menu bar name
	 */
	public static MenuBarModel loadMenuBarModel(String name)
	{
		MenuBarModel mbar = (MenuBarModel)menus.get(name);
		if(mbar == null)
		{
			mbar = new MenuBarModel(name);
			menus.put(name,mbar);
		}
		return mbar;
	}

	/**
	 * Creates a menubar.
	 * @param view The view to load the menubar for
	 * @param name The menu bar name
	 */
	public static JMenuBar loadMenuBar(View view, String name)
	{
		return loadMenuBarModel(name).create(view);
	}

	/**
	 * Loads a menu model.
	 * @param view The view to load the menu for
	 * @param name The menu name
	 */
	public static MenuModel loadMenuModel(String name)
	{
		MenuModel menu = (MenuModel)menus.get(name);
		if(menu == null)
		{
			menu = new MenuModel(name);
			menus.put(name,menu);
		}
		return menu;
	}

	/**
	 * Creates a menu.
	 * @param view The view to load the menu for
	 * @param name The menu name
	 */
	public static JMenu loadMenu(View view, String name)
	{
		return (JMenu)loadMenuModel(name).create(view);
	}

	/**
	 * Creates a popup menu.
	 * @param view The view to load the popup menu for
	 * @param name The menu name
	 */
	public static JPopupMenu loadPopupMenu(View view, String name)
	{
		return loadMenuModel(name).createPopup(view);
	}

	/**
	 * Loads a menu item model.
	 * @param name The menu item name
	 */
	public static MenuItemModel loadMenuItemModel(String name)
	{
		MenuItemModel menuitem = (MenuItemModel)menus.get(name);
		if(menuitem == null)
		{
			menuitem = new MenuItemModel(name);
			if(!menuitem.isTransient())
				menus.put(name,menuitem);
		}
		return menuitem;
	}

	/**
	 * Creates a menu item.
	 * @param name The menu item name
	 * @since jEdit 3.0pre1
	 */
	public static JMenuItem loadMenuItem(String name)
	{
		// 'view' parameter is 'null' because MenuItemModel
		// doesn't use it.

		// ... but create() still needs a 'view' parameter
		// because MenuModel (which subclasses MenuItemModel)
		// needs it.
		return loadMenuItemModel(name).create(null);
	}

	/**
	 * @deprecated If you are writing a plugin that specifically
	 * targets jEdit 3.0 or later, you should use the
	 * <code>loadMenuItem()</code> method that doesn't take
	 * the <code>view</code> parameter.
	 * @param view Unused
	 * @param name The menu item name
	 */
	public static JMenuItem loadMenuItem(View view, String name)
	{
		return loadMenuItem(name);
	}

	/**
	 * Loads a tool bar model.
	 * @param name The tool bar name
	 */
	public static ToolBarModel loadToolBarModel(String name)
	{
		ToolBarModel toolbar = (ToolBarModel)menus.get(name);
		if(toolbar == null)
		{
			toolbar = new ToolBarModel(name);
			menus.put(name,toolbar);
		}
		return toolbar;
	}

	/**
	 * Creates a toolbar.
	 * @param name The toolbar name
	 */
	public static JToolBar loadToolBar(String name)
	{
		return loadToolBarModel(name).create();
	}

	/**
	 * Loads a tool bar button. The tooltip is constructed from
	 * the <code><i>name</i>.label</code> and
	 * <code><i>name</i>.shortcut</code> properties and the icon is loaded
	 * from the resource named '/org/gjt/sp/jedit/toolbar/' suffixed
	 * with the value of the <code><i>name</i>.icon</code> property.
	 * @param name The name of the button
	 */
	public static EnhancedButton loadToolButton(String name)
	{
		return loadMenuItemModel(name).createButton();
	}

	/**
	 * Loads a tool bar icon.
	 * @param iconName The icon name
	 */
	public static Icon loadToolBarIcon(String iconName)
	{
		// check if there is a cached version first
		Icon icon = (Icon)icons.get(iconName);
		if(icon != null)
			return icon;

		// get the icon
		if(iconName.startsWith("file:"))
		{
			icon = new ImageIcon(iconName.substring(5));
		}
		else
		{
			icon = new ImageIcon(GUIUtilities.class.getResource(
				"/org/gjt/sp/jedit/toolbar/" + iconName));
		}

		icons.put(iconName,icon);
		return icon;
	}

	/**
	 * `Prettifies' a menu item label by removing the `$' sign and the
	 * training ellipisis, if any. This can be used to process the
	 * contents of an <i>action</i>.label property.
	 */
	public static String prettifyMenuLabel(String label)
	{
		int index = label.indexOf('$');
		if(index != -1)
		{
			label = label.substring(0,index)
				.concat(label.substring(index + 1));
		}
		if(label.endsWith("..."))
			label = label.substring(0,label.length() - 3);
		return label;
	}

	/**
	 * Displays a dialog box.
	 * The title of the dialog is fetched from
	 * the <code><i>name</i>.title</code> property. The message is fetched
	 * from the <code><i>name</i>.message</code> property. The message
	 * is formatted by the property manager with <code>args</code> as
	 * positional parameters.
	 * @param comp The component to display the dialog for
	 * @param name The name of the dialog
	 * @param args Positional parameters to be substituted into the
	 * message text
	 */
	public static void message(Component comp, String name, Object[] args)
	{
		hideSplashScreen();

		JOptionPane.showMessageDialog(comp,
			jEdit.getProperty(name.concat(".message"),args),
			jEdit.getProperty(name.concat(".title"),args),
			JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * Displays an error dialog box.
	 * The title of the dialog is fetched from
	 * the <code><i>name</i>.title</code> property. The message is fetched
	 * from the <code><i>name</i>.message</code> property. The message
	 * is formatted by the property manager with <code>args</code> as
	 * positional parameters.
	 * @param comp The component to display the dialog for
	 * @param name The name of the dialog
	 * @param args Positional parameters to be substituted into the
	 * message text
	 */
	public static void error(Component comp, String name, Object[] args)
	{
		hideSplashScreen();

		JOptionPane.showMessageDialog(comp,
			jEdit.getProperty(name.concat(".message"),args),
			jEdit.getProperty(name.concat(".title"),args),
			JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Displays an input dialog box and returns any text the user entered.
	 * The title of the dialog is fetched from
	 * the <code><i>name</i>.title</code> property. The message is fetched
	 * from the <code><i>name</i>.message</code> property.
	 * @param comp The component to display the dialog for
	 * @param name The name of the dialog
	 * @param def The text to display by default in the input field
	 */
	public static String input(Component comp, String name, Object def)
	{
		hideSplashScreen();

		String retVal = (String)JOptionPane.showInputDialog(comp,
			jEdit.getProperty(name.concat(".message")),
			jEdit.getProperty(name.concat(".title")),
			JOptionPane.QUESTION_MESSAGE,null,null,def);
		return retVal;
	}

	/**
	 * Displays an input dialog box and returns any text the user entered.
	 * The title of the dialog is fetched from
	 * the <code><i>name</i>.title</code> property. The message is fetched
	 * from the <code><i>name</i>.message</code> property.
	 * @param comp The component to display the dialog for
	 * @param name The name of the dialog
	 * @param def The property whose text to display in the input field
	 */
	public static String inputProperty(Component comp, String name, String def)
	{
		hideSplashScreen();

		String retVal = (String)JOptionPane.showInputDialog(comp,
			jEdit.getProperty(name.concat(".message")),
			jEdit.getProperty(name.concat(".title")),
			JOptionPane.QUESTION_MESSAGE,
			null,null,jEdit.getProperty(def));
		if(retVal != null)
			jEdit.setProperty(def,retVal);
		return retVal;
	}

	/**
	 * Displays a file selection dialog box. This is better than creating
	 * your own <code>JFileChooser</code> because it supports file filters,
	 * and reuses existing file choosers for maximum efficency.
	 * @param view The view
	 * @param file The file to select by default
	 * @param type The dialog type
	 * @return The selected file
	 */
	public static String showFileDialog(View view, String file, int type)
	{
		if(file == null)
			file = System.getProperty("user.dir");
		File _file = new File(file);
		JFileChooser chooser = getFileChooser(view);

		chooser.setCurrentDirectory(_file);
		chooser.rescanCurrentDirectory();
		if(_file.isDirectory())
			chooser.setSelectedFile(null);
		else
			chooser.setSelectedFile(_file);

		chooser.setDialogType(type);
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

		int retVal = chooser.showDialog(view,null);
		if(retVal == JFileChooser.APPROVE_OPTION)
		{
			File selectedFile = chooser.getSelectedFile();
			if(selectedFile != null)
				return selectedFile.getAbsolutePath();
		}

		return null;
	}

	/**
	 * Converts a color name to a color object. The name must either be
	 * a known string, such as `red', `green', etc (complete list is in
	 * the <code>java.awt.Color</code> class) or a hex color value
	 * prefixed with `#', for example `#ff0088'.
	 * @param name The color name
	 */
	public static Color parseColor(String name)
	{
		return parseColor(name, Color.black);
	}

	public static Color parseColor(String name, Color defaultColor)
	{
		if(name == null)
			return defaultColor;
		else if(name.startsWith("#"))
		{
			try
			{
				return Color.decode(name);
			}
			catch(NumberFormatException nf)
			{
				return defaultColor;
			}
		}
		else if("red".equals(name))
			return Color.red;
		else if("green".equals(name))
			return Color.green;
		else if("blue".equals(name))
			return Color.blue;
		else if("yellow".equals(name))
			return Color.yellow;
		else if("orange".equals(name))
			return Color.orange;
		else if("white".equals(name))
			return Color.white;
		else if("lightGray".equals(name))
			return Color.lightGray;
		else if("gray".equals(name))
			return Color.gray;
		else if("darkGray".equals(name))
			return Color.darkGray;
		else if("black".equals(name))
			return Color.black;
		else if("cyan".equals(name))
			return Color.cyan;
		else if("magenta".equals(name))
			return Color.magenta;
		else if("pink".equals(name))
			return Color.pink;
		else
			return defaultColor;
	}

	/**
	 * Converts a color object to its hex value. The hex value
	 * prefixed is with `#', for example `#ff0088'.
	 * @param c The color object
	 */
	public static String getColorHexString(Color c)
	{
		String colString = Integer.toHexString(c.getRGB() & 0xffffff);
		return "#000000".substring(0,7 - colString.length()).concat(colString);
	}

	/**
	 * Converts a style string to a style object.
	 * @param str The style string
	 * @exception IllegalArgumentException if the style is invalid
	 */
	public static SyntaxStyle parseStyle(String str)
		throws IllegalArgumentException
	{
		Color fgColor = Color.black;
		Color bgColor = null;
		boolean italic = false;
		boolean bold = false;
		StringTokenizer st = new StringTokenizer(str);
		while(st.hasMoreTokens())
		{
			String s = st.nextToken();
			if(s.startsWith("color:"))
			{
				fgColor = GUIUtilities.parseColor(s.substring(6), Color.black);
			}
			else if(s.startsWith("bgColor:"))
			{
				bgColor = GUIUtilities.parseColor(s.substring(8), null);
			}
			else if(s.startsWith("style:"))
			{
				for(int i = 6; i < s.length(); i++)
				{
					if(s.charAt(i) == 'i')
						italic = true;
					else if(s.charAt(i) == 'b')
						bold = true;
					else
						throw new IllegalArgumentException(
							"Invalid style: " + s);
				}
			}
			else
				throw new IllegalArgumentException(
					"Invalid directive: " + s);
		}
		return new SyntaxStyle(fgColor,bgColor,italic,bold);
	}

	/**
	 * Converts a style into it's string representation.
	 * @param style The style
	 */
	public static String getStyleString(SyntaxStyle style)
	{
		StringBuffer buf = new StringBuffer();

		buf.append("color:" + getColorHexString(style.getForegroundColor()));
		if(style.getBackgroundColor() != null) 
		{
			buf.append(" bgColor:" + getColorHexString(style.getBackgroundColor()));
		}
		if(!style.isPlain())
		{
			buf.append(" style:" + (style.isItalic() ? "i" : "")
				+ (style.isBold() ? "b" : ""));
		}

		return buf.toString();
	}

	/**
	 * Loads a windows's geometry from the properties.
	 * The geometry is loaded from the <code><i>name</i>.x</code>,
	 * <code><i>name</i>.y</code>, <code><i>name</i>.width</code> and
	 * <code><i>name</i>.height</code> properties.
	 *
	 * @param win The window
	 * @param name The window name
	 */
	public static void loadGeometry(Window win, String name)
	{
		int x, y, width, height;

		try
		{
			width = Integer.parseInt(jEdit.getProperty(name + ".width"));
			height = Integer.parseInt(jEdit.getProperty(name + ".height"));
		}
		catch(NumberFormatException nf)
		{
			Dimension size = win.getSize();
			width = size.width;
			height = size.height;
		}

		try
		{
			x = Integer.parseInt(jEdit.getProperty(name + ".x"));
			y = Integer.parseInt(jEdit.getProperty(name + ".y"));
		}
		catch(NumberFormatException nf)
		{
			Component parent = win.getParent();
			if(parent == null)
			{
				Dimension screen = win.getToolkit().getScreenSize();
				x = (screen.width - width) / 2;
				y = (screen.height - height) / 2;
			}
			else
			{
				Rectangle bounds = parent.getBounds();
				x = bounds.x + (bounds.width - width) / 2;
				y = bounds.y + (bounds.height - height) / 2;
			}
		}

		win.setLocation(x,y);
		win.setSize(width,height);

		// workaround for broken Linux JDK
		if(System.getProperty("os.name").indexOf("Linux") != -1)
			win.addWindowListener(new LinuxWorkaround(win,x,y,width,height));
	}

	static class LinuxWorkaround extends WindowAdapter
	{
		Window win;
		int x, y, width, height;

		LinuxWorkaround(Window win, int x, int y, int width, int height)
		{
			this.win = win;
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}

		public void windowOpened(WindowEvent evt)
		{
			win.setLocation(x,y);
			win.setSize(width,height);

			win.removeWindowListener(this);
		}
	}

	/**
	 * Saves a window's geometry to the properties.
	 * The geometry is saved to the <code><i>name</i>.x</code>,
	 * <code><i>name</i>.y</code>, <code><i>name</i>.width</code> and
	 * <code><i>name</i>.height</code> properties.
	 * @param win The window
	 * @param name The window name
	 */
	public static void saveGeometry(Window win, String name)
	{
		Point location = win.getLocation();
		Dimension size = win.getSize();
		jEdit.setProperty(name + ".x",String.valueOf(location.x));
		jEdit.setProperty(name + ".y",String.valueOf(location.y));
		jEdit.setProperty(name + ".width",String.valueOf(size.width));
		jEdit.setProperty(name + ".height",String.valueOf(size.height));

	}

	/**
	 * Ensures that the splash screen is not visible. This should be
	 * called before displaying any dialog boxes or windows at
	 * startup.
	 */
	public static void hideSplashScreen()
	{
		if(splash != null)
		{
			splash.dispose();
			splash = null;
		}
	}

	/**
	 * Returns the default editor window image.
	 */
	public static Image getEditorIcon()
	{
		return new ImageIcon(GUIUtilities.class.getResource(
			"jedit_icon1.gif")).getImage();
	}

	/**
	 * Returns the default plugin window image.
	 */
	public static Image getPluginIcon()
	{
		return new ImageIcon(GUIUtilities.class.getResource(
			"jedit_icon2.gif")).getImage();
	}

	/**
	 * Focuses on the specified component as soon as the window becomes
	 * active.
	 * @param win The window
	 * @param comp The component
	 */
	public static void requestFocus(final Window win, final Component comp)
	{
		win.addWindowListener(new WindowAdapter()
		{
			public void windowActivated(WindowEvent evt)
			{
				comp.requestFocus();
				win.removeWindowListener(this);
			}
		});
	}

	// package-private members
	static JFileChooser chooser;

	static void startLoadThread()
	{
		thread = new LoadThread();
	}

	static class LoadThread extends Thread
	{
		LoadThread()
		{
			super("jEdit file chooser load thread");
			setPriority(Thread.MIN_PRIORITY);
			start();
		}

		public void run()
		{
			Log.log(Log.DEBUG,LoadThread.class,"Loading file"
				+ " dialog...");
			getFileChooser(null);
			Log.log(Log.DEBUG,LoadThread.class,"File dialog"
				+ " loaded");
		}
	}

	static void showSplashScreen()
	{
		splash = new SplashScreen();
	}

	// advances progress bar
	static void setProgressText(String message)
	{
		if(splash != null)
			splash.advance(message);
	}

	// private members
	private static LoadThread thread;
	private static SplashScreen splash;
	// since the names of menu items, menus, tool bars, etc are
	// unique because of properties, we can store all in one
	// hashtable.
	private static Hashtable menus = new Hashtable();
	private static Hashtable icons = new Hashtable();

	private GUIUtilities() {}

	// Since only one file chooser is every visible at any one
	// time, we can reuse 'em
	private static synchronized JFileChooser getFileChooser(View view)
	{
		if(chooser == null)
		{
			if(view != null)
				view.showWaitCursor();

			chooser = new JFileChooser();
			chooser.setAccessory(new FindAccessory(chooser));

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
					chooser.addChoosableFileFilter(
						new REFileFilter(label,
						MiscUtilities.globToRE(glob)));
				}
				catch(REException re)
				{
					Log.log(Log.ERROR,GUIUtilities.class,
						"Invalid file filter: " + glob);
					Log.log(Log.ERROR,GUIUtilities.class,
						re);
				}
			}

			// Load file filters
			int i = 0;
			String name;

			while((name = jEdit.getProperty("filefilter." + i + ".name")) != null
				&& name.length() != 0)
			{
				try
				{
					chooser.addChoosableFileFilter(new REFileFilter(name,
						MiscUtilities.globToRE(jEdit.getProperty(
						"filefilter." + i + ".re"))));
				}
				catch(REException re)
				{
					Log.log(Log.ERROR,GUIUtilities.class,
						"Invalid file filter: " + i);
					Log.log(Log.ERROR,GUIUtilities.class,
						re);
				}

				i++;
			}

			chooser.setFileFilter(chooser.getAcceptAllFileFilter());

			// increase preferred size a little bit
			Dimension size = chooser.getPreferredSize();
			size.width *= 1.1;
			chooser.setPreferredSize(size);

			if(view != null)
				view.hideWaitCursor();
		}

		return chooser;
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.68  2000/07/12 09:11:37  sp
 * macros can be added to context menu and tool bar, menu bar layout improved
 *
 * Revision 1.67  2000/06/29 06:20:45  sp
 * Tool bar icon code bug fix
 *
 * Revision 1.66  2000/06/12 02:43:29  sp
 * pre6 almost ready
 *
 * Revision 1.65  2000/06/05 08:22:25  sp
 * bug fixes
 *
 * Revision 1.64  2000/05/24 07:56:04  sp
 * bug fixes
 *
 * Revision 1.63  2000/05/23 04:04:52  sp
 * Marker highlight updates, next/prev-marker actions
 *
 * Revision 1.62  2000/05/21 06:06:43  sp
 * Documentation updates, shell script mode bug fix, HyperSearch is now a frame
 *
 * Revision 1.61  2000/05/20 07:02:04  sp
 * Documentation updates, tool bar editor finished, a few other enhancements
 *
 * Revision 1.60  2000/05/16 10:47:40  sp
 * More work on toolbar editor, -gui command line switch
 *
 * Revision 1.59  2000/05/14 10:55:21  sp
 * Tool bar editor started, improved view registers dialog box
 *
 * Revision 1.58  2000/05/08 11:20:07  sp
 * New file finder in open dialog box
 *
 * Revision 1.57  2000/05/04 10:37:04  sp
 * Wasting time
 *
 */
