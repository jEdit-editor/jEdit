/*
 * SearchDialog.java - Search and replace dialog
 * Copyright (C) 1998, 1999, 2000, 2001 Slava Pestov
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
import org.gjt.sp.jedit.gui.*;
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

		pack();
		jEdit.unsetProperty("search.width");
		jEdit.unsetProperty("search.d-width");
		jEdit.unsetProperty("search.height");
		jEdit.unsetProperty("search.d-height");
		GUIUtilities.loadGeometry(this,"search");
	}

	public void setSearchString(String searchString)
	{
		find.setText(searchString);
		find.selectAll();

		replace.setText(null);
		replaceScript.setText(null);

		ignoreCase.setSelected(SearchAndReplace.getIgnoreCase());
		regexp.setSelected(SearchAndReplace.getRegexp());
		wrap.setSelected(SearchAndReplace.getAutoWrapAround());

		if(SearchAndReplace.getReverseSearch())
			searchBack.setSelected(true);
		else
			searchForward.setSelected(true);

		if(SearchAndReplace.getBeanShellReplace())
			beanShellReplace.setSelected(true);
		else
			stringReplace.setSelected(true);

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

		if(!isVisible())
		{
			keepDialog.setSelected(jEdit.getBooleanProperty(
				"search.keepDialog.toggle"));

			hyperSearch.setSelected(jEdit.getBooleanProperty(
				"search.hypersearch.toggle"));

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
	private HistoryTextField find, replace, replaceScript;

	private JRadioButton stringReplace, beanShellReplace;

	// search settings
	private JCheckBox keepDialog, ignoreCase, regexp, hyperSearch,
		wrap;
	private JRadioButton searchBack, searchForward;
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

		JPanel fieldPanel = new JPanel(new VariableGridLayout(
			VariableGridLayout.FIXED_NUM_COLUMNS,1));
		fieldPanel.setBorder(new EmptyBorder(0,0,12,12));

		JLabel label = new JLabel(jEdit.getProperty("search.find"));
		label.setDisplayedMnemonic(jEdit.getProperty("search.find.mnemonic")
			.charAt(0));
		find = new HistoryTextField("find");
		find.addActionListener(actionHandler);
		label.setLabelFor(find);
		label.setBorder(new EmptyBorder(12,0,2,0));
		fieldPanel.add(label);
		fieldPanel.add(find);

		label = new JLabel(jEdit.getProperty("search.replace"));
		label.setDisplayedMnemonic(jEdit.getProperty("search.replace.mnemonic")
			.charAt(0));
		label.setBorder(new EmptyBorder(12,0,0,0));
		fieldPanel.add(label);

		ButtonGroup grp = new ButtonGroup();
		ReplaceActionHandler replaceActionHandler = new ReplaceActionHandler();

		stringReplace = new JRadioButton(jEdit.getProperty(
			"search.string-replace-btn"));
		stringReplace.setBorder(new EmptyBorder(3,0,3,0));
		stringReplace.addActionListener(replaceActionHandler);
		grp.add(stringReplace);
		fieldPanel.add(stringReplace);

		replace = new HistoryTextField("replace");
		replace.addActionListener(actionHandler);
		label.setLabelFor(replace);
		fieldPanel.add(replace);

		beanShellReplace = new JRadioButton(jEdit.getProperty(
			"search.beanshell-replace-btn"));
		beanShellReplace.setBorder(new EmptyBorder(6,0,3,0));
		beanShellReplace.addActionListener(replaceActionHandler);
		grp.add(beanShellReplace);
		fieldPanel.add(beanShellReplace);

		if(jEdit.getBooleanProperty("search.beanshell.value"))
			beanShellReplace.setSelected(true);
		else
			stringReplace.setSelected(true);

		replaceScript = new HistoryTextField("replace.script");
		replaceScript.addActionListener(actionHandler);
		fieldPanel.add(replaceScript);

		return fieldPanel;
	}

	private JPanel createSearchSettingsPanel()
	{
		JPanel searchSettings = new JPanel(new VariableGridLayout(
			VariableGridLayout.FIXED_NUM_COLUMNS,3));
		searchSettings.setBorder(new EmptyBorder(0,0,12,12));

		SettingsActionHandler actionHandler = new SettingsActionHandler();
		ButtonGroup fileset = new ButtonGroup();

		searchSettings.add(new JLabel(jEdit.getProperty("search.fileset")));

		keepDialog = new JCheckBox(jEdit.getProperty("search.keep"));
		keepDialog.setMnemonic(jEdit.getProperty("search.keep.mnemonic")
			.charAt(0));
		searchSettings.add(keepDialog);

		searchSettings.add(new JLabel(jEdit.getProperty("search.direction")));

		ButtonGroup direction = new ButtonGroup();

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

		searchBack = new JRadioButton(jEdit.getProperty("search.back"));
		searchBack.setMnemonic(jEdit.getProperty("search.back.mnemonic")
			.charAt(0));
		direction.add(searchBack);
		searchSettings.add(searchBack);
		searchBack.addActionListener(actionHandler);

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

		searchForward = new JRadioButton(jEdit.getProperty("search.forward"));
		searchForward.setMnemonic(jEdit.getProperty("search.forward.mnemonic")
			.charAt(0));
		direction.add(searchForward);
		searchSettings.add(searchForward);
		searchForward.addActionListener(actionHandler);

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

		wrap= new JCheckBox(jEdit.getProperty("search.wrap"));
		wrap.setMnemonic(jEdit.getProperty("search.wrap.mnemonic")
			.charAt(0));
		searchSettings.add(wrap);
		wrap.addActionListener(actionHandler);

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

		replace.setEnabled(replaceEnabled
			&& stringReplace.isSelected());
		replaceScript.setEnabled(replaceEnabled
			&& beanShellReplace.isSelected());
		stringReplace.setEnabled(replaceEnabled);
		beanShellReplace.setEnabled(replaceEnabled);
		replaceBtn.setEnabled(replaceEnabled);
		replaceAndFindBtn.setEnabled(replaceEnabled);
		replaceAllBtn.setEnabled(replaceEnabled);
		wrap.setEnabled(replaceEnabled);

		boolean reverseEnabled = replaceEnabled
			&& searchCurrentBuffer.isSelected();
		searchBack.setEnabled(reverseEnabled);
		searchForward.setEnabled(reverseEnabled);

		boolean regexpEnabled = (hyperSearch.isSelected()
			|| !(searchBack.isEnabled() && searchBack.isSelected()));
		regexp.setEnabled(regexpEnabled);

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
		this.filter.addCurrentToHistory();
		if(filter.length() == 0)
			filter = "*";

		if(searchCurrentBuffer.isSelected())
			fileset = new CurrentBufferSet();
		else if(searchAllBuffers.isSelected())
			fileset = new AllBufferSet(filter);
		else if(searchDirectory.isSelected())
		{
			String directory = this.directory.getText();
			this.directory.addCurrentToHistory();
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

		jEdit.setBooleanProperty("search.keepDialog.toggle",
			keepDialog.isSelected());

		jEdit.setBooleanProperty("search.hypersearch.toggle",
			hyperSearch.isSelected());

		boolean ok = true;

		if(fileset.getBufferCount() == 0)
		{
			// oops
			GUIUtilities.error(this,"empty-fileset",null);
			ok = false;
		}
		else
			SearchAndReplace.setSearchFileSet(fileset);

		if(find.getText().length() != 0)
		{
			find.addCurrentToHistory();
			SearchAndReplace.setSearchString(find.getText());
			replace.addCurrentToHistory();
			replaceScript.addCurrentToHistory();

			SearchAndReplace.setReplaceString(
				beanShellReplace.isSelected()
				? replaceScript.getText()
				: replace.getText());
		}
		else
			ok = false;

		return ok;
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

	class ReplaceActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			updateEnabled();
			SearchAndReplace.setBeanShellReplace(
				beanShellReplace.isSelected());
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
			else if(source == searchBack || source == searchForward)
				SearchAndReplace.setReverseSearch(searchBack.isSelected());
			else if(source == wrap)
				SearchAndReplace.setAutoWrapAround(wrap.isSelected());

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
