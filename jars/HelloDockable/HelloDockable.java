/*
 * HelloDockable.java - Sample dockable window
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

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.*;
import javax.swing.*;
import java.awt.*;

public class HelloDockable extends JPanel implements DockableWindow
{
	public HelloDockable()
	{
		setLayout(new BorderLayout());
		add(BorderLayout.CENTER,new JScrollPane(
			textArea = new JTextArea(10,20)));
	}

	// begin DockableWindow implementation
	public String getName()
	{
		return HelloDockablePlugin.NAME;
	}

	public Component getComponent()
	{
		return this;
	}
	// end DockableWindow implementation

	/**
	 * This method is called when the dockable window is added to
	 * the view, or closed if it is floating.
	 */
	public void addNotify()
	{
		super.addNotify();
		textArea.setText(jEdit.getProperty("hello-dockable.text"));
	}

	/**
	 * This method is called when the dockable window is removed from
	 * the view, or closed if it is floating.
	 */
	public void removeNotify()
	{
		super.removeNotify();
		jEdit.setProperty("hello-dockable.text",textArea.getText());
	}

	// private members
	private JTextArea textArea;
}
