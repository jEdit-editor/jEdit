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

import javax.swing.border.*;
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

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(0,12,12,12));
		setContentPane(content);

		fileset = SearchAndReplace.getSearchFileSet();

		find = new HistoryTextField("find");
		find.setText(defaultFind);
		replace = new HistoryTextField("replace");
		keepDialog = new JCheckBox(jEdit.getProperty(
			"search.keepDialog"));
		keepDialog.setMnemonic(jEdit.getProperty("search.keepDialog"
			+ ".mnemonic").charAt(0));
		ignoreCase = new JCheckBox(jEdit.getProperty(
			"search.ignoreCase"));
		ignoreCase.setMnemonic(jEdit.getProperty("search.ignoreCase"
			+ ".mnemonic").charAt(0));
		regexp = new JCheckBox(jEdit.getProperty(
			"search.regexp"));
		regexp.setMnemonic(jEdit.getProperty("search.regexp.mnemonic")
			.charAt(0));
		multifile = new JCheckBox();
		multifileBtn = new JButton(jEdit.getProperty(
			"search.multifile"));
		multifileBtn.setMnemonic(jEdit.getProperty("search.multifile"
			+ ".mnemonic").charAt(0));
		findBtn = new JButton(jEdit.getProperty("search.findBtn"));
		replaceBtn = new JButton(jEdit.getProperty("search.replaceBtn"));
		replaceBtn.setMnemonic(jEdit.getProperty("search.replaceBtn"
			+ ".mnemonic").charAt(0));
		replaceFind = new JButton(jEdit.getProperty("search.replaceFind"));
		replaceFind.setMnemonic(jEdit.getProperty("search.replaceFind"
			+ ".mnemonic").charAt(0));
		replaceAll = new JButton(jEdit.getProperty("search.replaceAll"));
		replaceAll.setMnemonic(jEdit.getProperty("search.replaceAll"
			+ ".mnemonic").charAt(0));
		close = new JButton(jEdit.getProperty("common.close"));

		JPanel panel = new JPanel(new GridLayout(2,1));
		JPanel panel2 = new JPanel(new BorderLayout());
		JLabel label = new JLabel(jEdit.getProperty("search.find"));
		label.setDisplayedMnemonic(jEdit.getProperty("search.find.mnemonic")
			.charAt(0));
		label.setLabelFor(find);
		label.setBorder(new EmptyBorder(12,0,2,0));
		panel2.add(BorderLayout.NORTH,label);
		panel2.add(BorderLayout.CENTER,find);
		panel.add(panel2);
		panel2 = new JPanel(new BorderLayout());
		label = new JLabel(jEdit.getProperty("search.replace"));
		label.setDisplayedMnemonic(jEdit.getProperty("search.replace.mnemonic")
			.charAt(0));
		label.setLabelFor(replace);
		label.setBorder(new EmptyBorder(12,0,2,0));
		panel2.add(BorderLayout.NORTH,label);
		panel2.add(BorderLayout.CENTER,replace);
		panel.add(panel2);
		content.add(BorderLayout.NORTH,panel);

		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
		panel.setBorder(new EmptyBorder(6,0,12,0));
		panel.add(Box.createGlue());
		panel.add(keepDialog);
		panel.add(Box.createHorizontalStrut(2));
		panel.add(ignoreCase);
		panel.add(Box.createHorizontalStrut(2));
		panel.add(regexp);
		panel.add(Box.createHorizontalStrut(2));
		panel.add(multifile);
		panel.add(multifileBtn);
		panel.add(Box.createGlue());
		content.add(BorderLayout.CENTER,panel);

		Box box = new Box(BoxLayout.X_AXIS);
		box.add(Box.createGlue());
		box.add(findBtn);
		box.add(Box.createHorizontalStrut(6));
		box.add(replaceBtn);
		box.add(Box.createHorizontalStrut(6));
		box.add(replaceFind);
		box.add(Box.createHorizontalStrut(6));
		box.add(replaceAll);
		box.add(Box.createHorizontalStrut(6));
		box.add(close);
		box.add(Box.createGlue());
		getRootPane().setDefaultButton(findBtn);
		content.add(BorderLayout.SOUTH,box);

		ActionHandler actionListener = new ActionHandler();
		find.addActionListener(actionListener);
		replace.addActionListener(actionListener);
		multifile.addActionListener(actionListener);
		multifileBtn.addActionListener(actionListener);
		findBtn.addActionListener(actionListener);
		replaceBtn.addActionListener(actionListener);
		replaceFind.addActionListener(actionListener);
		replaceAll.addActionListener(actionListener);
		close.addActionListener(actionListener);

		GUIUtilities.requestFocus(this,find);

		pack();

		// don't remember the width and height of the dialog box,
		// so that people upgrading from jEdit 2.3 don't get a
		// dialog box that is way too small (since its size and
		// layout has changed over the versions, a size that used
		// to make sense might no longer)
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
			closeOrKeepDialog();

		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	public void cancel()
	{
		save();
		GUIUtilities.saveGeometry(this,"search");
		setVisible(false);
	}
	// end EnhancedDialog implementation

	public void setSearchString(String search)
	{
		find.setText(search);
		replace.setText(null);

		if(!isVisible())
		{
			keepDialog.setSelected(jEdit.getBooleanProperty("search.keepDialog.toggle"));
			ignoreCase.setSelected(SearchAndReplace.getIgnoreCase());
			regexp.setSelected(SearchAndReplace.getRegexp());
			multifile.setSelected(!(fileset instanceof CurrentBufferSet));

			setVisible(true);
		}

		toFront();
		requestFocus();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				find.requestFocus();
			}
		});
	}

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
	private JButton replaceBtn;
	private JButton replaceFind;
	private JButton replaceAll;
	private JButton close;
	
	private void save()
	{
		// so that opening and closing the dialog box with a blank
		// search string doesn't stop find-next from working
		if(find.getText().length() != 0)
		{
			find.addCurrentToHistory();
			SearchAndReplace.setSearchString(find.getText());
			replace.addCurrentToHistory();
			SearchAndReplace.setReplaceString(replace.getText());
		}

		jEdit.setBooleanProperty("search.keepDialog.toggle",keepDialog
			.isSelected());
		SearchAndReplace.setIgnoreCase(ignoreCase.isSelected());
		SearchAndReplace.setRegexp(regexp.isSelected());
		SearchAndReplace.setSearchFileSet(fileset);
	}

	private void closeOrKeepDialog()
	{
		if(keepDialog.isSelected())
			return;
		GUIUtilities.saveGeometry(this,"search");
		setVisible(false);
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
			if(source == close)
				cancel();
			else if(source == findBtn || source == find
				|| source == replace)
			{
				ok();
			}
			else if(source == replaceBtn)
			{
				save();
				if(SearchAndReplace.replace(view,SearchDialog.this))
					closeOrKeepDialog();
				else
					getToolkit().beep();
			}
			else if(source == replaceFind)
			{
				save();
				if(SearchAndReplace.replace(view,SearchDialog.this))
					ok();
				else
					getToolkit().beep();
			}
			else if(source == replaceAll)
			{
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

				save();
				if(SearchAndReplace.replaceAll(view,SearchDialog.this))
					closeOrKeepDialog();
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
 * Revision 1.20  2000/11/05 00:44:14  sp
 * Improved HyperSearch, improved horizontal scroll, other stuff
 *
 * Revision 1.19  2000/11/02 09:19:33  sp
 * more features
 *
 * Revision 1.18  2000/10/13 06:57:20  sp
 * Edit User/System Macros command, gutter mouse handling improved
 *
 * Revision 1.17  2000/08/31 02:54:00  sp
 * Improved activity log, bug fixes
 *
 * Revision 1.16  2000/08/29 07:47:12  sp
 * Improved complete word, type-select in VFS browser, bug fixes
 *
 * Revision 1.15  2000/08/05 07:16:12  sp
 * Global options dialog box updated, VFS browser now supports right-click menus
 *
 * Revision 1.14  2000/06/16 10:11:06  sp
 * Bug fixes ahoy
 *
 * Revision 1.13  2000/06/03 07:28:26  sp
 * User interface updates, bug fixes
 *
 * Revision 1.12  2000/05/21 06:06:43  sp
 * Documentation updates, shell script mode bug fix, HyperSearch is now a frame
 *
 * Revision 1.11  2000/05/21 03:00:51  sp
 * Code cleanups and bug fixes
 *
 * Revision 1.10  2000/05/04 10:37:04  sp
 * Wasting time
 *
 */
