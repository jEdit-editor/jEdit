/*
 * OptionsDialog.java - Global options dialog
 * Copyright (C) 1998, 1999, 2000 Slava Pestov
 * Portions copyright (C) 1999 mike dillon
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
import javax.swing.border.*;
import javax.swing.event.*;
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
	implements ActionListener, ListSelectionListener
{
	public OptionsDialog(View view)
	{
		super(view,jEdit.getProperty("options.title"),true);

		view.showWaitCursor();

		getContentPane().setLayout(new BorderLayout());
		panes = new Vector();

		cardPanel = new JPanel(new CardLayout());
		cardPanel.setBorder(new CompoundBorder(new BevelBorder(
			BevelBorder.RAISED), new EmptyBorder(2, 2, 2, 2)));

		getContentPane().add(cardPanel, BorderLayout.CENTER);

		paneNames = new DefaultListModel();
		paneList = new JList(paneNames);
		paneList.addListSelectionListener(this);
		getContentPane().add(new JScrollPane(paneList),
			BorderLayout.WEST);

		JPanel buttons = new JPanel();
		ok = new JButton(jEdit.getProperty("common.ok"));
		ok.addActionListener(this);
		buttons.add(ok);
		getRootPane().setDefaultButton(ok);
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(this);
		buttons.add(cancel);
		getContentPane().add(BorderLayout.SOUTH,buttons);

		addOptionPane(new org.gjt.sp.jedit.options.GeneralOptionPane());
		addOptionPane(new org.gjt.sp.jedit.options.EditorOptionPane());
		addOptionPane(new org.gjt.sp.jedit.options.StyleOptionPane());
		addOptionPane(new org.gjt.sp.jedit.options.FileFilterOptionPane());
		addOptionPane(new org.gjt.sp.jedit.options.CommandShortcutsOptionPane());
		addOptionPane(new org.gjt.sp.jedit.options.MacroShortcutsOptionPane());
		addOptionPane(new org.gjt.sp.jedit.options.AbbrevsOptionPane());

		// Query plugins for option panes
		EditPlugin[] plugins = jEdit.getPlugins();
		for(int i = 0; i < plugins.length; i++)
		{
			try
			{
				plugins[i].createOptionPanes(this);
			}
			catch(Throwable t)
			{
				Log.log(Log.ERROR,this,"Error creating option pane");
				Log.log(Log.ERROR,this,t);
			}
		}

		view.hideWaitCursor();

		Dimension screen = getToolkit().getScreenSize();
		pack();
		setLocation((screen.width - getSize().width) / 2,
			(screen.height - getSize().height) / 2);
		show();
	}

	public void addOptionPane(OptionPane pane)
	{
		String label = jEdit.getProperty("options." + pane.getName()
			+ ".label");

		cardPanel.add(pane.getComponent(), pane.getName());
		panes.addElement(pane);
		paneNames.addElement(label);
		if (paneList.getSelectedIndex() == -1)
			paneList.setSelectedIndex(0);
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

	public void valueChanged(ListSelectionEvent evt)
	{
		Object source = evt.getSource();

		if (source != paneList) return;

		int idx = ((JList)source).getSelectedIndex();

		if (idx != -1)
		{
			String cardName = ((OptionPane)panes.elementAt(idx))
				.getName();
			((CardLayout)cardPanel.getLayout()).show(cardPanel,
				cardName);
		}
	}

	// private members
	private Vector panes;
	private JTabbedPane tabs;
	private JList paneList;
	private JPanel cardPanel;
	private DefaultListModel paneNames;
	private JButton ok;
	private JButton cancel;
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.11  2000/01/30 04:23:23  sp
 * New about box, minor bug fixes and updates here and there
 *
 * Revision 1.10  2000/01/16 06:09:27  sp
 * Bug fixes
 *
 * Revision 1.9  2000/01/14 22:11:24  sp
 * Enhanced options dialog box
 *
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
