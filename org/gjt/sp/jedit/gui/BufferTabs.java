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
	public BufferTabs(View view, JEditTextArea textArea)
	{
		this.view = view;
		this.textArea = textArea;

		buffers = new Vector();

		Buffer buffer = jEdit.getFirstBuffer();
		while(buffer != null)
		{
			addBufferTab(buffer);
			buffer = buffer.getNext();
		}

		((Magic)getSelectedComponent()).update();
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
		insertTab(getTabLabel(buffer),null,new Magic(buffer),null,index);

		if(index <= selectedIndex)
		{
			selectedIndex++;
			setSelectedIndex(selectedIndex);
		}

		view.focusOnTextArea();
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
		// if dirty is now true, we just update the tab's label,
		// otherwise, the name might have changed (dirty = false
		// means just saved)
		if(buffer.isDirty())
		{
			int index = buffer.getIndex();
			setTitleAt(index,getTabLabel(buffer));
		}
		else
		{
			updating = true;

			removeBufferTab(buffer);
			addBufferTab(buffer);
			selectBufferTab(buffer);

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
			((Magic)getSelectedComponent()).update();
		else
			setSelectedIndex(buffer.getIndex());
	}

	// private members
	private View view;
	private JEditTextArea textArea;
	private Vector buffers;
	private boolean removing;
	private boolean updating;

	private String getTabLabel(Buffer buffer)
	{
		Object[] args = { buffer.getName(),
			new Integer(buffer.isReadOnly() ? 1 : 0),
			new Integer(buffer.isDirty() ? 1: 0),
			new Integer(buffer.isNewFile() ? 1: 0)};

		return jEdit.getProperty("view.title",args);
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
			this.add(BorderLayout.CENTER,textArea);
			this.revalidate();
		}
	}
}
