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

package org.gjt.sp.jedit.search;

import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.BadLocationException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import org.gjt.sp.jedit.gui.EnhancedDialog;
import org.gjt.sp.jedit.gui.HistoryTextField;
import org.gjt.sp.jedit.io.FileVFS;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

/**
 * Search and replace dialog.
 * @author Slava Pestov
 * @version $Id$
 */
public class SearchDialog extends EnhancedDialog
{
	public SearchDialog(View view)
	{
		super(view,jEdit.getProperty("search.title"),false);

		this.view = view;

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(0,12,12,12));
		setContentPane(content);

		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.add(BorderLayout.NORTH,createFieldPanel());
		centerPanel.add(BorderLayout.CENTER,createSearchSettingsPanel());
		content.add(BorderLayout.CENTER,centerPanel);
		content.add(BorderLayout.SOUTH,createMultiFilePanel());

		content.add(BorderLayout.EAST,createButtonsPanel());

		setResizable(false);

		pack();
		jEdit.unsetProperty("search.width");
		jEdit.unsetProperty("search.height");
		GUIUtilities.loadGeometry(this,"search");
	}

	public void setSearchString(String searchString)
	{
		find.setText(searchString);
		replace.setText(null);

		if(!isVisible())
		{
			keepDialog.setSelected(jEdit.getBooleanProperty(
				"search.keepDialog.toggle"));
			ignoreCase.setSelected(SearchAndReplace.getIgnoreCase());
			regexp.setSelected(SearchAndReplace.getRegexp());

			hyperSearch.setSelected(jEdit.getBooleanProperty("search.hypersearch.toggle"));

			fileset = SearchAndReplace.getSearchFileSet();
			if(fileset instanceof CurrentBufferSet)
				searchCurrentBuffer.setSelected(true);
			else if(fileset instanceof AllBufferSet)
				searchAllBuffers.setSelected(true);
			else if(fileset instanceof DirectoryListSet)
				searchDirectory.setSelected(true);

			if(fileset instanceof DirectoryListSet)
			{
				filter.setText(((DirectoryListSet)fileset)
					.getFileFilter());
				directory.setText(((DirectoryListSet)fileset)
					.getDirectory());
				searchSubDirectories.setSelected(((DirectoryListSet)fileset)
					.isRecursive());
			}
			else
			{
				String path;
				if(view.getBuffer().getVFS() instanceof FileVFS)
				{
					path = MiscUtilities.getParentOfPath(
						view.getBuffer().getPath());
				}
				else
					path = System.getProperty("user.dir");
				directory.setText(path);

				if(fileset instanceof AllBufferSet)
				{
					filter.setText(((AllBufferSet)fileset)
						.getFileFilter());
				}
				else
				{
					filter.setText("*" + MiscUtilities
						.getFileExtension(view.getBuffer()
						.getName()));
				}

				searchSubDirectories.setSelected(true);
			}

			updateEnabled();
			setVisible(true);
		}

		toFront();
		requestFocus();

		GUIUtilities.requestFocus(this,find);
	}

	public void ok()
	{
		try
		{
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

			if(!save())
				return;

			if(hyperSearch.isSelected())
			{
				if(SearchAndReplace.hyperSearch(view));
					closeOrKeepDialog();
			}
			else
			{
				if(SearchAndReplace.find(view))
					closeOrKeepDialog();
			}
		}
		finally
		{
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}

	public void cancel()
	{
		save();
		GUIUtilities.saveGeometry(this,"search");
		setVisible(false);
	}

	// private members
	private View view;
	private SearchFileSet fileset;

	// fields
	private HistoryTextField find, replace;

	// search settings
	private JCheckBox keepDialog, ignoreCase, regexp, hyperSearch;
	private JRadioButton searchCurrentBuffer, searchAllBuffers,
		searchDirectory;

	// multifile settings
	private HistoryTextField filter, directory;
	private JCheckBox searchSubDirectories;
	private JButton choose;

	// buttons
	private JButton findBtn, replaceBtn, replaceAndFindBtn, replaceAllBtn,
		closeBtn;

	private JPanel createFieldPanel()
	{
		ButtonActionHandler actionHandler = new ButtonActionHandler();

		JPanel fieldPanel = new JPanel(new GridLayout(2,1));
		fieldPanel.setBorder(new EmptyBorder(0,0,12,12));

		JPanel panel2 = new JPanel(new BorderLayout());
		JLabel label = new JLabel(jEdit.getProperty("search.find"));
		label.setDisplayedMnemonic(jEdit.getProperty("search.find.mnemonic")
			.charAt(0));
		find = new HistoryTextField("find");
		find.addActionListener(actionHandler);
		label.setLabelFor(find);
		label.setBorder(new EmptyBorder(12,0,2,0));
		panel2.add(BorderLayout.NORTH,label);
		panel2.add(BorderLayout.CENTER,find);
		fieldPanel.add(panel2);

		panel2 = new JPanel(new BorderLayout());
		label = new JLabel(jEdit.getProperty("search.replace"));
		label.setDisplayedMnemonic(jEdit.getProperty("search.replace.mnemonic")
			.charAt(0));
		replace = new HistoryTextField("replace");
		replace.addActionListener(actionHandler);
		label.setLabelFor(replace);
		label.setBorder(new EmptyBorder(12,0,2,0));
		panel2.add(BorderLayout.NORTH,label);
		panel2.add(BorderLayout.CENTER,replace);
		fieldPanel.add(panel2);

		return fieldPanel;
	}

	private JPanel createSearchSettingsPanel()
	{
		JPanel searchSettings = new JPanel(new GridLayout(4,2));
		searchSettings.setBorder(new EmptyBorder(0,0,12,12));

		SettingsActionHandler actionHandler = new SettingsActionHandler();
		ButtonGroup fileset = new ButtonGroup();

		searchSettings.add(new JLabel(jEdit.getProperty("search.fileset")));

		keepDialog = new JCheckBox(jEdit.getProperty("search.keep"));
		keepDialog.setMnemonic(jEdit.getProperty("search.keep.mnemonic")
			.charAt(0));
		searchSettings.add(keepDialog);

		searchCurrentBuffer = new JRadioButton(jEdit.getProperty("search.current"));
		searchCurrentBuffer.setMnemonic(jEdit.getProperty("search.current.mnemonic")
			.charAt(0));
		fileset.add(searchCurrentBuffer);
		searchSettings.add(searchCurrentBuffer);
		searchCurrentBuffer.addActionListener(actionHandler);

		ignoreCase = new JCheckBox(jEdit.getProperty("search.case"));
		ignoreCase.setMnemonic(jEdit.getProperty("search.case.mnemonic")
			.charAt(0));
		searchSettings.add(ignoreCase);
		ignoreCase.addActionListener(actionHandler);

		searchAllBuffers = new JRadioButton(jEdit.getProperty("search.all"));
		searchAllBuffers.setMnemonic(jEdit.getProperty("search.all.mnemonic")
			.charAt(0));
		fileset.add(searchAllBuffers);
		searchSettings.add(searchAllBuffers);
		searchAllBuffers.addActionListener(actionHandler);

		regexp = new JCheckBox(jEdit.getProperty("search.regexp"));
		regexp.setMnemonic(jEdit.getProperty("search.regexp.mnemonic")
			.charAt(0));
		searchSettings.add(regexp);
		regexp.addActionListener(actionHandler);

		searchDirectory = new JRadioButton(jEdit.getProperty("search.directory"));
		searchDirectory.setMnemonic(jEdit.getProperty("search.directory.mnemonic")
			.charAt(0));
		fileset.add(searchDirectory);
		searchSettings.add(searchDirectory);
		searchDirectory.addActionListener(actionHandler);

		hyperSearch = new JCheckBox(jEdit.getProperty("search.hypersearch"));
		hyperSearch.setMnemonic(jEdit.getProperty("search.hypersearch.mnemonic")
			.charAt(0));
		searchSettings.add(hyperSearch);
		hyperSearch.addActionListener(actionHandler);

		return searchSettings;
	}

	private JPanel createMultiFilePanel()
	{
		JPanel multifile = new JPanel();

		GridBagLayout layout = new GridBagLayout();
		multifile.setLayout(layout);

		GridBagConstraints cons = new GridBagConstraints();
		cons.gridy = cons.gridwidth = cons.gridheight = 1;
		cons.anchor = GridBagConstraints.WEST;
		cons.fill = GridBagConstraints.HORIZONTAL;

		filter = new HistoryTextField("search.filter");

		cons.insets = new Insets(0,0,3,0);

		JLabel label = new JLabel(jEdit.getProperty("search.filterField"),
			SwingConstants.RIGHT);
		label.setBorder(new EmptyBorder(0,0,0,12));
		label.setDisplayedMnemonic(jEdit.getProperty("search.filterField.mnemonic")
			.charAt(0));
		label.setLabelFor(filter);
		cons.weightx = 0.0f;
		layout.setConstraints(label,cons);
		multifile.add(label);

		cons.insets = new Insets(0,0,3,6);
		cons.weightx = 1.0f;
		layout.setConstraints(filter,cons);
		multifile.add(filter);

		cons.gridy++;

		directory = new HistoryTextField("search.directory");

		label = new JLabel(jEdit.getProperty("search.directoryField"),
			SwingConstants.RIGHT);
		label.setBorder(new EmptyBorder(0,0,0,12));

		label.setDisplayedMnemonic(jEdit.getProperty("search.directoryField.mnemonic")
			.charAt(0));
		label.setLabelFor(directory);
		cons.insets = new Insets(0,0,3,0);
		cons.weightx = 0.0f;
		layout.setConstraints(label,cons);
		multifile.add(label);

		cons.insets = new Insets(0,0,3,6);
		cons.weightx = 1.0f;
		cons.gridwidth = 2;
		layout.setConstraints(directory,cons);
		multifile.add(directory);

		choose = new JButton(jEdit.getProperty("search.choose"));
		choose.setMnemonic(jEdit.getProperty("search.choose.mnemonic")
			.charAt(0));
		cons.insets = new Insets(0,0,3,0);
		cons.weightx = 0.0f;
		cons.gridwidth = 1;
		layout.setConstraints(choose,cons);
		multifile.add(choose);
		choose.addActionListener(new MultiFileActionHandler());

		cons.insets = new Insets(0,0,0,0);
		cons.gridy++;
		cons.gridwidth = 4;

		searchSubDirectories = new JCheckBox(jEdit.getProperty(
			"search.subdirs"));
		searchSubDirectories.setMnemonic(jEdit.getProperty("search.subdirs.mnemonic")
			.charAt(0));
		layout.setConstraints(searchSubDirectories,cons);
		multifile.add(searchSubDirectories);

		return multifile;
	}

	private Box createButtonsPanel()
	{
		Box box = new Box(BoxLayout.Y_AXIS);

		ButtonActionHandler actionHandler = new ButtonActionHandler();

		box.add(Box.createVerticalStrut(12));

		JPanel grid = new JPanel(new GridLayout(5,1,0,12));

		findBtn = new JButton(jEdit.getProperty("search.findBtn"));
		getRootPane().setDefaultButton(findBtn);
		grid.add(findBtn);
		findBtn.addActionListener(actionHandler);

		replaceBtn = new JButton(jEdit.getProperty("search.replaceBtn"));
		replaceBtn.setMnemonic(jEdit.getProperty("search.replaceBtn.mnemonic")
			.charAt(0));
		grid.add(replaceBtn);
		replaceBtn.addActionListener(actionHandler);

		replaceAndFindBtn = new JButton(jEdit.getProperty("search.replaceAndFindBtn"));
		replaceAndFindBtn.setMnemonic(jEdit.getProperty("search.replaceAndFindBtn.mnemonic")
			.charAt(0));
		grid.add(replaceAndFindBtn);
		replaceAndFindBtn.addActionListener(actionHandler);

		replaceAllBtn = new JButton(jEdit.getProperty("search.replaceAllBtn"));
		replaceAllBtn.setMnemonic(jEdit.getProperty("search.replaceAllBtn.mnemonic")
			.charAt(0));
		grid.add(replaceAllBtn);
		replaceAllBtn.addActionListener(actionHandler);

		closeBtn = new JButton(jEdit.getProperty("common.close"));
		grid.add(closeBtn);
		closeBtn.addActionListener(actionHandler);

		grid.setMaximumSize(grid.getPreferredSize());

		box.add(grid);
		box.add(Box.createGlue());

		return box;
	}

	private void updateEnabled()
	{
		boolean replaceEnabled = !hyperSearch.isSelected();

		replace.setEnabled(replaceEnabled);
		replaceBtn.setEnabled(replaceEnabled);
		replaceAndFindBtn.setEnabled(replaceEnabled);
		replaceAllBtn.setEnabled(replaceEnabled);

		filter.setEnabled(searchAllBuffers.isSelected()
			|| searchDirectory.isSelected());

		boolean directoryEnabled = searchDirectory.isSelected();

		directory.setEnabled(directoryEnabled);
		choose.setEnabled(directoryEnabled);
		searchSubDirectories.setEnabled(directoryEnabled);
	}

	private boolean save()
	{
		String filter = this.filter.getText();
		if(filter.length() == 0)
			filter = "*";

		if(searchCurrentBuffer.isSelected())
			fileset = new CurrentBufferSet();
		else if(searchAllBuffers.isSelected())
			fileset = new AllBufferSet(filter);
		else if(searchDirectory.isSelected())
		{
			String directory = this.directory.getText();
			boolean recurse = searchSubDirectories.isSelected();

			if(fileset instanceof DirectoryListSet)
			{
				DirectoryListSet dset = (DirectoryListSet)fileset;
				if(!dset.getDirectory().equals(directory)
					|| !dset.getFileFilter().equals(filter)
					|| !dset.isRecursive() == recurse)
					fileset = new DirectoryListSet(directory,filter,recurse);
			}
			else
				fileset = new DirectoryListSet(directory,filter,recurse);
		}

		if(fileset.getBufferCount() == 0)
		{
			// oops
			GUIUtilities.error(this,"empty-fileset",null);
			return false;
		}

		SearchAndReplace.setSearchFileSet(fileset);

		if(find.getText().length() != 0)
		{
			find.addCurrentToHistory();
			SearchAndReplace.setSearchString(find.getText());
			replace.addCurrentToHistory();
			SearchAndReplace.setReplaceString(replace.getText());
		}

		jEdit.setBooleanProperty("search.keepDialog.toggle",
			keepDialog.isSelected());

		jEdit.setBooleanProperty("search.hypersearch.toggle",
			hyperSearch.isSelected());

		return true;
	}

	private void closeOrKeepDialog()
	{
		if(keepDialog.isSelected())
			return;
		else
		{
			GUIUtilities.saveGeometry(this,"search");
			setVisible(false);
		}
	}

	class SettingsActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();

			if(source == ignoreCase)
				SearchAndReplace.setIgnoreCase(ignoreCase.isSelected());
			else if(source == regexp)
				SearchAndReplace.setRegexp(regexp.isSelected());
			else if(source == hyperSearch
				|| source == searchCurrentBuffer
				|| source == searchAllBuffers
				|| source == searchDirectory)
				updateEnabled();
		}
	}

	class MultiFileActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			File dir = new File(directory.getText());
			JFileChooser chooser = new JFileChooser(dir.getParent());
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setSelectedFile(dir);

			if(chooser.showOpenDialog(SearchDialog.this)
				== JFileChooser.APPROVE_OPTION)
				directory.setText(chooser.getSelectedFile().getPath());
		}
	}

	class ButtonActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();

			if(source == closeBtn)
				cancel();
			else if(source == findBtn || source == find
				|| source == replace)
			{
				ok();
			}
			else if(source == replaceBtn)
			{
				save();
				if(SearchAndReplace.replace(view))
					closeOrKeepDialog();
				else
					getToolkit().beep();
			}
			else if(source == replaceAndFindBtn)
			{
				save();
				if(SearchAndReplace.replace(view))
					ok();
				else
					getToolkit().beep();
			}
			else if(source == replaceAllBtn)
			{
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

				save();
				if(SearchAndReplace.replaceAll(view))
					closeOrKeepDialog();
				else
					getToolkit().beep();

				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		}
	}
}
