/*
 * OptionsDialog.java - Abstract tabbed options dialog
 * Copyright (C) 1998, 1999 Slava Pestov
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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

/**
 * An abstract tabbed options dialog box.
 * @author Slava Pestov
 * @version $Id$
 */
public class OptionsDialog extends EnhancedDialog
implements ActionListener
{
	public OptionsDialog(View view, String title)
	{
		super(view,title,true);

		view.showWaitCursor();

		getContentPane().setLayout(new BorderLayout());
		panes = new Vector();
		tabs = new JTabbedPane();
		getContentPane().add(BorderLayout.CENTER,tabs);
		JPanel buttons = new JPanel();
		ok = new JButton(jEdit.getProperty("common.ok"));
		ok.addActionListener(this);
		buttons.add(ok);
		getRootPane().setDefaultButton(ok);
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(this);
		buttons.add(cancel);
		getContentPane().add(BorderLayout.SOUTH,buttons);
	}

	public void addOptionPane(OptionPane pane)
	{
		tabs.addTab(jEdit.getProperty("options." + pane.getName()
			+ ".label"),pane.getComponent());
		panes.addElement(pane);
	}

	// EnhancedDialog implementation
	public void ok()
	{
		Enumeration enum = panes.elements();
		while(enum.hasMoreElements())
		{
			try
			{
				((OptionPane)enum.nextElement()).save();
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,this,"Error saving option pane");
				Log.log(Log.ERROR,this,t);
			}
		}

		/* This will fire the PROPERTIES_CHANGED event */
		jEdit.propertiesChanged();
		dispose();
	}

	public void cancel()
	{
		dispose();
	}
	// end EnhancedDialog implementation

	public void actionPerformed(ActionEvent evt)
	{
		Object source = evt.getSource();
		if(source == ok)
			ok();
		else if(source == cancel)
			cancel();
	}

	// protected members
	protected Vector panes;
	protected JTabbedPane tabs;

	// private members
	private JButton ok;
	private JButton cancel;
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.8  1999/11/26 07:37:11  sp
 * Escape/enter handling code moved to common superclass, bug fixes
 *
 * Revision 1.7  1999/11/19 08:54:52  sp
 * EditBus integrated into the core, event system gone, bug fixes
 *
 * Revision 1.6  1999/10/23 03:48:22  sp
 * Mode system overhaul, close all dialog box, misc other stuff
 *
 * Revision 1.5  1999/10/04 03:20:51  sp
 * Option pane change, minor tweaks and bug fixes
 *
 * Revision 1.4  1999/09/30 12:21:04  sp
 * No net access for a month... so here's one big jEdit 2.1pre1
 *
 * Revision 1.3  1999/07/16 23:45:49  sp
 * 1.7pre6 BugFree version
 *
 * Revision 1.2  1999/07/05 04:38:39  sp
 * Massive batch of changes... bug fixes, also new text component is in place.
 * Have fun
 *
 * Revision 1.1  1999/06/07 06:36:32  sp
 * Syntax `styling' (bold/italic tokens) added,
 * plugin options dialog for plugin option panes
 *
 */
