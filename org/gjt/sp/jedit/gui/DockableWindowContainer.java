/*
 * DockableWindowContainer.java - holds dockable windows
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

package org.gjt.sp.jedit.gui;

import javax.swing.border.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.*;

/**
 * A container for dockable windows. This class should never be used
 * directly.
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 2.6pre3
 */
public interface DockableWindowContainer
{
	void addDockableWindow(DockableWindow win);
	void saveDockableWindow(DockableWindow win);
	void removeDockableWindow(DockableWindow win);
	void showDockableWindow(DockableWindow win);
	boolean isDockableWindowVisible(DockableWindow win);

	/**
	 * Tabbed pane container.
	 */
	public class TabbedPane extends JTabbedPane implements DockableWindowContainer
	{
		String position;
		int dimension;
		boolean collapsed;

		public TabbedPane(String position)
		{
			this.position = position;

			try
			{
				dimension = Integer.parseInt(jEdit.getProperty(
					"view.dock." + position + ".dimension"));
			}
			catch(NumberFormatException nf)
			{
				dimension = -1;
			}

			MouseHandler mouseHandler = new MouseHandler();
			addMouseListener(mouseHandler);
			addMouseMotionListener(mouseHandler);

			propertiesChanged();
		}

		public boolean isCollapsed()
		{
			return getComponentCount() == 0 || collapsed;
		}

		public void setCollapsed(boolean collapsed)
		{
			if(getComponentCount() == 0)
				return;

			this.collapsed = collapsed;
			revalidate();
		}

		public void toggleCollapsed()
		{
			setCollapsed(!collapsed);
		}

		public void saveDimension()
		{
			if(dimension <= 5)
				dimension = -1;

			jEdit.setProperty("view.dock." + position + ".dimension",
				String.valueOf(dimension));
			jEdit.setBooleanProperty("view.dock." + position
				+ ".collapsed",collapsed);
		}

		public void propertiesChanged()
		{
			int top = position.equals(DockableWindowManager.BOTTOM) ? 5 : 0;
			int left = position.equals(DockableWindowManager.RIGHT) ? 5 : 0;
			int bottom = position.equals(DockableWindowManager.TOP) ? 5 : 0;
			int right = position.equals(DockableWindowManager.LEFT) ? 5 : 0;

			setBorder(new MatteBorder(top,left,bottom,right,
				GUIUtilities.parseColor(jEdit.getProperty(
				"view.docking.borderColor"))));

			int tabsPos = Integer.parseInt(jEdit.getProperty(
				"view.docking.tabsPos"));
			if(tabsPos == 0)
				setTabPlacement(JTabbedPane.TOP);
			else if(tabsPos == 1)
				setTabPlacement(JTabbedPane.BOTTOM);
		}

		public Dimension getMinimumSize()
		{
			return new Dimension(0,0);
		}

		public Dimension getPreferredSize()
		{
			if(getComponentCount() == 0)
				return new Dimension(0,0);

			Dimension prefSize = super.getPreferredSize();
			if(collapsed)
			{
				if(position.equals(DockableWindowManager.LEFT)
					|| position.equals(DockableWindowManager.RIGHT))
					prefSize.width = 5;
				else if(position.equals(DockableWindowManager.TOP)
					|| position.equals(DockableWindowManager.BOTTOM))
					prefSize.height = 5;
			}
			else if(dimension == -1)
			{
				if(position.equals(DockableWindowManager.LEFT)
					|| position.equals(DockableWindowManager.RIGHT))
					dimension = prefSize.width;
				else if(position.equals(DockableWindowManager.TOP)
					|| position.equals(DockableWindowManager.BOTTOM))
					 dimension = prefSize.height;
			}
			else
			{
				if(position.equals(DockableWindowManager.LEFT)
					|| position.equals(DockableWindowManager.RIGHT))
					prefSize.width = dimension;
				else if(position.equals(DockableWindowManager.TOP)
					|| position.equals(DockableWindowManager.BOTTOM))
					prefSize.height = dimension;
			}

			return prefSize;
		}

		public void addDockableWindow(DockableWindow win)
		{
			addTab(jEdit.getProperty(win.getName()
				+ ".title"),win.getComponent());
			setSelectedComponent(win.getComponent());
			if(dimension == 0)
				dimension = -1;

			collapsed = false;

			revalidate();
		}

		public void saveDockableWindow(DockableWindow win) {}

		public void removeDockableWindow(DockableWindow win)
		{
			remove(win.getComponent());
			revalidate();
		}

		public void showDockableWindow(DockableWindow win)
		{
			setSelectedComponent(win.getComponent());
			if(collapsed)
			{
				collapsed = false;
				revalidate();
			}
			win.getComponent().requestFocus();
		}

		public boolean isDockableWindowVisible(DockableWindow win)
		{
			return !collapsed;
		}

		class MouseHandler extends MouseAdapter implements MouseMotionListener
		{
			boolean canDrag;
			Point dragStart;

			public void mousePressed(MouseEvent evt)
			{
				dragStart = evt.getPoint();
				dragStart.x = (getWidth() - dragStart.x);
				dragStart.y = (getHeight() - dragStart.y);
			}

			public void mouseClicked(MouseEvent evt)
			{
				if(evt.getClickCount() == 2)
					setCollapsed(!isCollapsed());
			}

			public void mouseMoved(MouseEvent evt)
			{
				Border border = getBorder();
				Insets insets = border.getBorderInsets(TabbedPane.this);
				int cursor = Cursor.DEFAULT_CURSOR;
				canDrag = false;
				if(position.equals(DockableWindowManager.TOP))
				{
					if(evt.getY() >= getHeight() - insets.bottom)
					{
						cursor = Cursor.N_RESIZE_CURSOR;
						canDrag = true;
					}
				}
				else if(position.equals(DockableWindowManager.LEFT))
				{
					if(evt.getX() >= getWidth() - insets.right)
					{
						cursor = Cursor.W_RESIZE_CURSOR;
						canDrag = true;
					}
				}
				else if(position.equals(DockableWindowManager.BOTTOM))
				{
					if(evt.getY() <= insets.top)
					{
						cursor = Cursor.S_RESIZE_CURSOR;
						canDrag = true;
					}
				}
				else if(position.equals(DockableWindowManager.RIGHT))
				{
					if(evt.getX() <= insets.left)
					{
						cursor = Cursor.E_RESIZE_CURSOR;
						canDrag = true;
					}
				}

				setCursor(Cursor.getPredefinedCursor(cursor));
			}

			public void mouseDragged(MouseEvent evt)
			{
				if(!canDrag)
					return;

				if(dragStart == null) // can't happen?
					return;

				if(position.equals(DockableWindowManager.TOP))
					dimension = evt.getY() + dragStart.y;
				else if(position.equals(DockableWindowManager.LEFT))
					dimension = evt.getX() + dragStart.x;
				else if(position.equals(DockableWindowManager.BOTTOM))
					dimension = getHeight() - evt.getY();
				else if(position.equals(DockableWindowManager.RIGHT))
					dimension = getWidth() - evt.getX();

				dimension = Math.max(5,dimension);
				if(dimension == 5)
				{
					dimension = -1;
					collapsed = true;
				}
				else
					collapsed = false;

				revalidate();
			}

			public void mouseExited(MouseEvent evt)
			{
				setCursor(Cursor.getPredefinedCursor(
					Cursor.DEFAULT_CURSOR));
			}
		}
	}

	/**
	 * Floating container.
	 */
	public class Floating extends JFrame implements DockableWindowContainer
	{
		public Floating(DockableWindowManager dockableWindowManager)
		{
			this.dockableWindowManager = dockableWindowManager;
			setIconImage(GUIUtilities.getPluginIcon());
			setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		}

		public void addDockableWindow(DockableWindow window)
		{
			this.window = window;
			name = window.getName();
			setTitle(jEdit.getProperty(name + ".title"));

			getContentPane().add(BorderLayout.CENTER,window.getComponent());

			pack();
			GUIUtilities.loadGeometry(this,name);
			show();
		}

		public void saveDockableWindow(DockableWindow window)
		{
			GUIUtilities.saveGeometry(this,name);
		}

		public void removeDockableWindow(DockableWindow window)
		{
			super.dispose();
		}

		public void showDockableWindow(DockableWindow window)
		{
			toFront();
			requestFocus();
		}

		public boolean isDockableWindowVisible(DockableWindow win)
		{
			return true;
		}

		public void dispose()
		{
			dockableWindowManager.removeDockableWindow(name);
		}

		// private members
		private DockableWindowManager dockableWindowManager;
		private DockableWindow window;
		private String name;
	}
}
