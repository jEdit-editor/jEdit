/*
 * BufferTabs.java - Buffer tabs that appear in views
 * Copyright (C) 1999 Jason Ginchereau
 * Portions copyright (C) 2000 Slava Pestov
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

import javax.swing.event.*;
import javax.swing.*;
import java.awt.*;
import java.util.Vector;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.jedit.*;

/**
 * This is basically Jason's BufferTabs.java, somewhat reformatted and
 * rearranged.
 */
public class BufferTabs extends JTabbedPane
{
	public BufferTabs(View view)
	{
		this.view = view;

		buffers = new Vector();

		Buffer buffer = jEdit.getFirstBuffer();
		while(buffer != null)
		{
			addBufferTab(buffer);
			buffer = buffer.getNext();
		}

		update();
		addChangeListener(new ChangeHandler());
	}

	public boolean isFocusTraversable()
	{
		return false;
	}

	public boolean isRequestFocusEnabled()
	{
		return false;
	}

	public void addBufferTab(Buffer buffer)
	{
		int index = buffer.getIndex();
		int selectedIndex = getSelectedIndex();

		buffers.insertElementAt(buffer,index);
		insertTab(buffer.getName(),getIcon(buffer),
			new Magic(buffer),buffer.getPath(),
			index);

		if(index <= selectedIndex)
		{
			selectedIndex++;
			setSelectedIndex(selectedIndex);
		}
	}

	public void removeBufferTab(Buffer buffer)
	{
		int index = buffers.indexOf(buffer);
		int selectedIndex = getSelectedIndex();

		buffers.removeElementAt(index);

		removeTabAt(index);

		if(index < selectedIndex)
		{
			if(selectedIndex != 0)
				selectedIndex--;

			setSelectedIndex(selectedIndex);
		}
		else if(index == selectedIndex)
		{
			Magic comp = (Magic)getSelectedComponent();
			if(comp != null)
				comp.update();
		}
	}

	public void updateBufferTab(Buffer buffer)
	{
		// if dirty is now true, we just update the tab's icon,
		// otherwise, the name might have changed (dirty = false
		// means just saved)
		if(buffer.isDirty())
		{
			int index = buffer.getIndex();
			setIconAt(index,getIcon(buffer));
		}
		else
		{
			updating = true;

			removeBufferTab(buffer);
			addBufferTab(buffer);
// 			selectBufferTab(buffer);

			updating = false;

			if(view.getBuffer() == buffer)
				selectBufferTab(buffer);
		}
	}

	public void selectBufferTab(Buffer buffer)
	{
		int index = buffer.getIndex();
		int selectedIndex = getSelectedIndex();
		if(index == selectedIndex)
			update();
		else
			setSelectedIndex(buffer.getIndex());
	}

	public void update()
	{
		((Magic)getSelectedComponent()).update();
	}

	// private members
	private View view;
	private Vector buffers;
	private boolean removing;
	private boolean updating;

	private static ImageIcon newDirtyIcon, newIcon, dirtyIcon, normalIcon;
	static
	{
		newDirtyIcon = new ImageIcon(BufferTabs.class.getResource(
			"/org/gjt/sp/jedit/new_dirty.gif"));
		newIcon = new ImageIcon(BufferTabs.class.getResource(
			"/org/gjt/sp/jedit/new.gif"));
		dirtyIcon = new ImageIcon(BufferTabs.class.getResource(
			"/org/gjt/sp/jedit/dirty.gif"));
		normalIcon = new ImageIcon(BufferTabs.class.getResource(
			"/org/gjt/sp/jedit/normal.gif"));
	}

	private ImageIcon getIcon(Buffer buffer)
	{
		if(buffer.isNewFile())
		{
			if(buffer.isDirty())
				return newDirtyIcon;
			else
				return newIcon;
		}
		else if(buffer.isDirty())
			return dirtyIcon;
		else
			return normalIcon;
	}

	class ChangeHandler implements ChangeListener
	{
		public void stateChanged(ChangeEvent evt)
		{
			if(updating)
				return;

			int index = getSelectedIndex();
			Buffer buffer = jEdit.getFirstBuffer();
			while(buffer != null)
			{
				if(index == 0)
					break;
				buffer = buffer.getNext();
				index--;
			}

			Magic comp = (Magic)getSelectedComponent();
			if(comp != null)
				comp.select();
		}
	}

	// each tab has an instance of this component, which swaps the
	// one text area instance in and out
	class Magic extends JPanel
	{
		Buffer buffer;

		Magic(Buffer buffer)
		{
			super(new BorderLayout());
			this.buffer = buffer;
		}

		void select()
		{
			view.setBuffer(buffer);
			update();
		}

		void update()
		{
			Component comp;
			if(view.getSplitPane() == null)
				comp = view.getTextArea();
			else
				comp = view.getSplitPane();
			this.add(BorderLayout.CENTER,comp);
			this.revalidate();

			view.focusOnTextArea();
		}
	}
}
