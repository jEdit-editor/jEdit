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

		addChangeListener(new ChangeHandler());
	}

	public Component getComponentAt(int index)
	{
		if(!removing && index >= 0 && index < getTabCount())
			return textArea;
		else
			return super.getComponentAt(index);
	}

	public int indexOfComponent(Component component)
	{
		return super.indexOfComponent(textArea);
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
		if(getTabCount() == 0)
			addTab(getTabLabel(buffer),null,textArea);
		else
			insertTab(getTabLabel(buffer),null,null,null,index);

		if(index <= selectedIndex)
		{
			selectedIndex++;
			setSelectedIndex(selectedIndex);
		}

		if(index == 0)
			textArea.setVisible(true);

		view.focusOnTextArea();
	}

	public void removeBufferTab(Buffer buffer)
	{
		int index = buffers.indexOf(buffer);
		int selectedIndex = getSelectedIndex();

		try
		{
			removing = true;
			buffers.removeElementAt(index);

			removeTabAt(index);

			if(index < selectedIndex)
			{
				if(selectedIndex != 0)
					selectedIndex--;

				setSelectedIndex(selectedIndex);
			}

			if(getTabCount() != 0)
			{
				setComponentAt(0,textArea);
				textArea.setVisible(true);
			}
		}
		finally
		{
			removing = false;
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
			try
			{
				updating = true;
				removeBufferTab(buffer);
				addBufferTab(buffer);
				selectBufferTab(buffer);
			}
			finally
			{
				updating = false;
			}
			if(view.getBuffer() == buffer)
				selectBufferTab(buffer);
		}
	}

	public void selectBufferTab(Buffer buffer)
	{
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

			if(buffer != null)
				view.setBuffer(buffer);
		}
	}
}
