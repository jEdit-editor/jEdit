/*
 * SearchDialog.java - Search and replace dialog
 * Copyright (C) 1998, 1999, 2000 Slava Pestov
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
import org.gjt.sp.jedit.search.*;
import org.gjt.sp.jedit.*;

/**
 * Search and replace dialog.
 * @author Slava Pestov
 * @version $Id$
 */
public class SearchDialog extends EnhancedDialog
{
	public SearchDialog(View view, String defaultFind)
	{
		super(view,jEdit.getProperty("search.title"),false);
		this.view = view;

		fileset = SearchAndReplace.getSearchFileSet();

		find = new HistoryTextField("find");
		find.setText(defaultFind);

		replace = new HistoryTextField("replace");
		keepDialog = new JCheckBox(jEdit.getProperty(
			"search.keepDialog"),jEdit.getBooleanProperty(
			"search.keepDialog.toggle"));
		ignoreCase = new JCheckBox(jEdit.getProperty(
			"search.ignoreCase"),SearchAndReplace.getIgnoreCase());
		regexp = new JCheckBox(jEdit.getProperty(
			"search.regexp"),SearchAndReplace.getRegexp());
		multifile = new JCheckBox();
		multifile.setSelected(!(fileset instanceof CurrentBufferSet));
		multifileBtn = new JButton(jEdit.getProperty(
			"search.multifile"));
		findBtn = new JButton(jEdit.getProperty("search.findBtn"));
		replaceSelection = new JButton(jEdit.getProperty("search"
			+ ".replaceSelection"));
		replaceSelection.setMnemonic(jEdit.getProperty("search"
			+ ".replaceSelection.mnemonic").charAt(0));
		replaceAll = new JButton(jEdit.getProperty("search.replaceAll"));
		replaceAll.setMnemonic(jEdit.getProperty("search.replaceAll"
			+ ".mnemonic").charAt(0));
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		getContentPane().setLayout(new BorderLayout());
		JPanel panel = new JPanel(new GridLayout(4,1));
		panel.add(new JLabel(jEdit.getProperty("search.find")));
		panel.add(find);
		panel.add(new JLabel(jEdit.getProperty("search.replace")));
		panel.add(replace);
		getContentPane().add(BorderLayout.NORTH,panel);
		panel = new JPanel();
		panel.add(keepDialog);
		panel.add(ignoreCase);
		panel.add(regexp);
		panel.add(multifile);
		panel.add(multifileBtn);
		getContentPane().add(BorderLayout.CENTER,panel);
		panel = new JPanel();
		panel.add(findBtn);
		panel.add(replaceSelection);
		panel.add(replaceAll);
		panel.add(cancel);
		getRootPane().setDefaultButton(findBtn);
		getContentPane().add(BorderLayout.SOUTH,panel);

		ActionHandler actionListener = new ActionHandler();
		multifile.addActionListener(actionListener);
		multifileBtn.addActionListener(actionListener);
		findBtn.addActionListener(actionListener);
		replaceSelection.addActionListener(actionListener);
		replaceAll.addActionListener(actionListener);
		cancel.addActionListener(actionListener);

		GUIUtilities.requestFocus(this,find);

		pack();

		// hack so that people upgrading from older jEdit don't
		// get a wrong size dialog box
		jEdit.unsetProperty("search.width");
		jEdit.unsetProperty("search.height");
		GUIUtilities.loadGeometry(this,"search");
		show();
	}

	// EnhancedDialog implementation
	public void ok()
	{
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		save();
		if(SearchAndReplace.find(view,SearchDialog.this))
			disposeOrKeepDialog();

		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	public void cancel()
	{
		GUIUtilities.saveGeometry(this,"search");
		dispose();
	}
	// end EnhancedDialog implementation

        // private members
	private View view;
	private SearchFileSet fileset;
	private HistoryTextField find;
	private HistoryTextField replace;
	private JCheckBox keepDialog;
	private JCheckBox ignoreCase;
	private JCheckBox regexp;
	private JCheckBox multifile;
	private JButton multifileBtn;
	private JButton findBtn;
	private JButton replaceSelection;
	private JButton replaceAll;
	private JButton cancel;
	
	private void save()
	{
		find.addCurrentToHistory();
		SearchAndReplace.setSearchString(find.getText());
		replace.addCurrentToHistory();
		SearchAndReplace.setReplaceString(replace.getText());
		jEdit.setBooleanProperty("search.keepDialog.toggle",keepDialog
			.isSelected());
		SearchAndReplace.setIgnoreCase(ignoreCase.isSelected());
		SearchAndReplace.setRegexp(regexp.isSelected());
		SearchAndReplace.setSearchFileSet(fileset);
	}

	private void disposeOrKeepDialog()
	{
		if(keepDialog.isSelected())
			return;
		dispose();
	}

	private void showMultiFileDialog()
	{
		SearchFileSet fs = new MultiFileSearchDialog(
			view,fileset).getSearchFileSet();
		if(fs != null)
		{
			fileset = fs;
		}
		multifile.setSelected(!(fileset instanceof CurrentBufferSet));
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			Buffer buffer = view.getBuffer();
			if(source == cancel)
				dispose();
			else if(source == findBtn)
			{
				ok();
			}
			else if(source == replaceSelection)
			{
				save();
				if(SearchAndReplace.replace(view,SearchDialog.this))
					disposeOrKeepDialog();
				else
					getToolkit().beep();
			}
			else if(source == replaceAll)
			{
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

				save();
				if(SearchAndReplace.replaceAll(view,SearchDialog.this))
					disposeOrKeepDialog();
				else
					getToolkit().beep();

				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
			else if(source == multifileBtn)
			{
				showMultiFileDialog();
			}
			else if(source == multifile)
			{
				if(multifile.isSelected())
					showMultiFileDialog();
				else
					fileset = new CurrentBufferSet();
			}
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.11  2000/05/21 03:00:51  sp
 * Code cleanups and bug fixes
 *
 * Revision 1.10  2000/05/04 10:37:04  sp
 * Wasting time
 *
 * Revision 1.9  2000/04/28 09:29:12  sp
 * Key binding handling improved, VFS updates, some other stuff
 *
 * Revision 1.8  2000/04/15 04:14:47  sp
 * XML files updated, jEdit.get/setBooleanProperty() method added
 *
 * Revision 1.7  2000/04/03 10:22:24  sp
 * Search bar
 *
 * Revision 1.6  1999/12/24 01:20:20  sp
 * Bug fixing and other stuff for 2.3pre1
 *
 * Revision 1.5  1999/11/26 07:37:11  sp
 * Escape/enter handling code moved to common superclass, bug fixes
 *
 * Revision 1.4  1999/09/30 12:21:04  sp
 * No net access for a month... so here's one big jEdit 2.1pre1
 *
 * Revision 1.3  1999/06/09 07:28:10  sp
 * Multifile search and replace tweaks, removed console.html
 *
 * Revision 1.2  1999/06/09 05:22:11  sp
 * Find next now supports multi-file searching, minor Perl mode tweak
 *
 * Revision 1.1  1999/06/03 08:40:03  sp
 * More cvs fixing
 *
 * Revision 1.1  1999/05/31 04:42:38  sp
 * oops #2
 *
 */
