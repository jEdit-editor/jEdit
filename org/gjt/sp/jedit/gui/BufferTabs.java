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
	public BufferTabs(EditPane editPane)
	{
		this.editPane = editPane;

		Font myFont = getFont();
		Font textFont = editPane.getTextArea().getPainter().getFont();
		Font newFont = new Font(myFont.getFamily(),Font.PLAIN,textFont.getSize());
		setFont(newFont);

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

		if(getComponentCount() != 1 && index <= selectedIndex)
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

		boolean oldUpdating = updating;
		updating = true;
		removeTabAt(index);
		updating = oldUpdating;

		if(index < selectedIndex)
		{
			if(selectedIndex != 0)
				selectedIndex--;

			setSelectedIndex(selectedIndex);
		}
	}

	public void updateBufferTab(Buffer buffer)
	{
		int index = buffer.getIndex();

		// if dirty is now true, we just update the tab's icon,
		// otherwise, the name might have changed (dirty = false
		// means just saved)
		if(buffer.isDirty())
		{
			setIconAt(index,getIcon(buffer));
		}
		else
		{
			int oldIndex = buffers.indexOf(buffer);
			/* this can happen if the file is a new file and
			 * the Buffer.load() cleanup runnable runs before
			 * jEdit.openFile() returns.
			 *
			 * If this is the case, then we just ignore the
			 * update request and wait until we get the
			 * BufferUpdate.CREATED message.
			 */
			if(oldIndex == -1)
				return;

			updating = true;

			removeBufferTab(buffer);
			addBufferTab(buffer);

			updating = false;

			if(editPane.getBuffer() == buffer)
				selectBufferTab(buffer);
		}
	}

	public void selectBufferTab(Buffer buffer)
	{
		int index = buffer.getIndex();
		int selectedIndex = getSelectedIndex();

		if(index == selectedIndex)
			update();
		else if(index < getComponentCount())
			setSelectedIndex(index);
	}

	public void update()
	{
		((Magic)getSelectedComponent()).update();
	}

	// private members
	private EditPane editPane;
	private Vector buffers;
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
			if(updating || getComponentCount() == 0)
				return;

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
			editPane.setBuffer(buffer);
			update();
		}

		void update()
		{
			this.add(BorderLayout.CENTER,editPane.getTextArea());
			this.revalidate();

			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					editPane.focusOnTextArea();
				}
			});
		}

		public boolean isValidateRoot()
		{
			return true;
		}
	}
}
