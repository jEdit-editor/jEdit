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
		centerPanel.add(BorderLayout.SOUTH,createMultiFilePanel());
		content.add(BorderLayout.CENTER,centerPanel);

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

			String searchMode = jEdit.getProperty("search.mode.value");
			if("incremental".equals(searchMode))
				incrementalSearch.setSelected(true);
			else if("batch".equals(searchMode))
				batchSearch.setSelected(true);
			else
				normalSearch.setSelected(true);

			String batchMode = jEdit.getProperty("search.batch-results.value");
			if("buffer".equals(batchMode))
				batchBuffer.setSelected(true);
			else
				batchWindow.setSelected(true);

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
		if(incrementalSearch.isSelected())
		{
			// on enter, start search from end
			// of current match to find next one
			find.addCurrentToHistory();
			incrementalSearch(view.getTextArea()
				.getSelectionEnd());
		}
		else if(batchSearch.isSelected())
		{
			save();
			if(SearchAndReplace.batchSearch(view));
				closeOrKeepDialog();
		}
		else
		{
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

			save();
			if(SearchAndReplace.find(view))
				closeOrKeepDialog();

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
	private JRadioButton normalSearch, incrementalSearch, batchSearch;
	private JRadioButton batchWindow, batchBuffer;
	private JCheckBox keepDialog, ignoreCase, regexp;
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
		find.getDocument().addDocumentListener(new DocumentHandler());
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
		JPanel searchSettings = new JPanel(new GridLayout(7,2));
		searchSettings.setBorder(new EmptyBorder(0,0,12,12));

		SettingsActionHandler actionHandler = new SettingsActionHandler();

		ButtonGroup searchMode = new ButtonGroup();
		ButtonGroup fileset = new ButtonGroup();
		ButtonGroup showBatchResultsIn = new ButtonGroup();

		keepDialog = new JCheckBox(jEdit.getProperty("search.keep"));
		keepDialog.setMnemonic(jEdit.getProperty("search.keep.mnemonic")
			.charAt(0));
		searchSettings.add(keepDialog);

		searchSettings.add(new JLabel(jEdit.getProperty("search.mode")));

		ignoreCase = new JCheckBox(jEdit.getProperty("search.case"));
		ignoreCase.setMnemonic(jEdit.getProperty("search.case.mnemonic")
			.charAt(0));
		searchSettings.add(ignoreCase);
		ignoreCase.addActionListener(actionHandler);

		normalSearch = new JRadioButton(jEdit.getProperty("search.normal"));
		normalSearch.setMnemonic(jEdit.getProperty("search.normal.mnemonic")
			.charAt(0));
		searchMode.add(normalSearch);
		searchSettings.add(normalSearch);
		normalSearch.addActionListener(actionHandler);

		regexp = new JCheckBox(jEdit.getProperty("search.regexp"));
		regexp.setMnemonic(jEdit.getProperty("search.regexp.mnemonic")
			.charAt(0));
		searchSettings.add(regexp);
		regexp.addActionListener(actionHandler);

		incrementalSearch = new JRadioButton(jEdit.getProperty("search.incremental"));
		incrementalSearch.setMnemonic(jEdit.getProperty("search.incremental.mnemonic")
			.charAt(0));
		searchMode.add(incrementalSearch);
		searchSettings.add(incrementalSearch);
		incrementalSearch.addActionListener(actionHandler);

		searchSettings.add(new JLabel(jEdit.getProperty("search.fileset")));

		batchSearch = new JRadioButton(jEdit.getProperty("search.batch"));
		batchSearch.setMnemonic(jEdit.getProperty("search.batch.mnemonic")
			.charAt(0));
		searchMode.add(batchSearch);
		searchSettings.add(batchSearch);
		batchSearch.addActionListener(actionHandler);

		searchCurrentBuffer = new JRadioButton(jEdit.getProperty("search.current"));
		searchCurrentBuffer.setMnemonic(jEdit.getProperty("search.current.mnemonic")
			.charAt(0));
		fileset.add(searchCurrentBuffer);
		searchSettings.add(searchCurrentBuffer);
		searchCurrentBuffer.addActionListener(actionHandler);

		searchSettings.add(new JLabel(jEdit.getProperty("search.batch-results")));

		searchAllBuffers = new JRadioButton(jEdit.getProperty("search.all"));
		searchAllBuffers.setMnemonic(jEdit.getProperty("search.all.mnemonic")
			.charAt(0));
		fileset.add(searchAllBuffers);
		searchSettings.add(searchAllBuffers);
		searchAllBuffers.addActionListener(actionHandler);

		batchWindow = new JRadioButton(jEdit.getProperty("search.window"));
		batchWindow.setMnemonic(jEdit.getProperty("search.window.mnemonic")
			.charAt(0));
		fileset.add(batchWindow);
		showBatchResultsIn.add(batchWindow);
		searchSettings.add(batchWindow);

		searchDirectory = new JRadioButton(jEdit.getProperty("search.directory"));
		searchDirectory.setMnemonic(jEdit.getProperty("search.directory.mnemonic")
			.charAt(0));
		fileset.add(searchDirectory);
		searchSettings.add(searchDirectory);
		searchDirectory.addActionListener(actionHandler);

		batchBuffer = new JRadioButton(jEdit.getProperty("search.buffer"));
		batchBuffer.setMnemonic(jEdit.getProperty("search.buffer.mnemonic")
			.charAt(0));
		fileset.add(batchBuffer);
		showBatchResultsIn.add(batchBuffer);
		searchSettings.add(batchBuffer);

		return searchSettings;
	}

	private JPanel createMultiFilePanel()
	{
		JPanel multifile = new JPanel();
		multifile.setBorder(new EmptyBorder(0,0,0,12));

		GridBagLayout layout = new GridBagLayout();
		multifile.setLayout(layout);

		GridBagConstraints cons = new GridBagConstraints();
		cons.gridy = 1;
		cons.gridwidth = cons.gridheight = 1;
		cons.anchor = GridBagConstraints.WEST;
		cons.fill = GridBagConstraints.HORIZONTAL;

		filter = new HistoryTextField("filter");

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

		cons.weightx = 1.0f;
		layout.setConstraints(filter,cons);
		multifile.add(filter);

		cons.gridy++;

		directory = new HistoryTextField("directory");

		label = new JLabel(jEdit.getProperty("search.directoryField"),
			SwingConstants.RIGHT);
		label.setBorder(new EmptyBorder(0,0,0,12));

		label.setDisplayedMnemonic(jEdit.getProperty("search.directoryField.mnemonic")
			.charAt(0));
		label.setLabelFor(directory);
		cons.weightx = 0.0f;
		layout.setConstraints(label,cons);
		multifile.add(label);

		cons.insets = new Insets(0,0,3,6);
		cons.weightx = 1.0f;
		layout.setConstraints(directory,cons);
		multifile.add(directory);

		choose = new JButton(jEdit.getProperty("search.choose"));
		choose.setMnemonic(jEdit.getProperty("search.choose.mnemonic")
			.charAt(0));
		cons.weightx = 0.0f;
		layout.setConstraints(choose,cons);
		multifile.add(choose);
		choose.addActionListener(new MultiFileActionHandler());

		cons.insets = new Insets(0,0,0,0);
		cons.gridy++;
		cons.gridwidth = 3;

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
		boolean replaceEnabled = !(incrementalSearch.isSelected()
			|| batchSearch.isSelected());

		replace.setEnabled(replaceEnabled);
		replaceBtn.setEnabled(replaceEnabled);
		replaceAndFindBtn.setEnabled(replaceEnabled);
		replaceAllBtn.setEnabled(replaceEnabled);

		boolean batchAndMultiFileEnabled = (!incrementalSearch.isSelected());

		searchCurrentBuffer.setEnabled(batchAndMultiFileEnabled);
		searchAllBuffers.setEnabled(batchAndMultiFileEnabled);
		searchDirectory.setEnabled(batchAndMultiFileEnabled);

		boolean batchEnabled = batchSearch.isSelected();
		batchWindow.setEnabled(batchEnabled);
		batchBuffer.setEnabled(batchEnabled);

		filter.setEnabled(batchAndMultiFileEnabled
			&& (searchAllBuffers.isSelected()
			|| searchDirectory.isSelected()));

		boolean directoryEnabled = batchAndMultiFileEnabled
			&& searchDirectory.isSelected();

		directory.setEnabled(directoryEnabled);
		choose.setEnabled(directoryEnabled);
		searchSubDirectories.setEnabled(directoryEnabled);
	}

	private void incrementalSearch(int start)
	{
		SearchAndReplace.setSearchString(find.getText());

		try
		{
			if(SearchAndReplace.find(view,view.getBuffer(),start))
				return;
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
		catch(Exception ia)
		{
			// invalid regexp, ignore
		}

		view.getToolkit().beep();
	}

	private void save()
	{
		if(searchCurrentBuffer.isSelected())
			fileset = new CurrentBufferSet();
		else if(searchAllBuffers.isSelected())
			fileset = new AllBufferSet(filter.getText());
		else if(searchDirectory.isSelected())
		{
			String directory = this.directory.getText();
			String filter = this.filter.getText();
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
		String searchMode;
		if(incrementalSearch.isSelected())
			searchMode = "incremental";
		else if(batchSearch.isSelected())
			searchMode = "batch";
		else
			searchMode = "normal";

		jEdit.setProperty("search.mode.value",searchMode);

		jEdit.setProperty("search.batch-results.value",
			batchBuffer.isSelected() ? "buffer" : "window");
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
			else if(source == normalSearch
				|| source == incrementalSearch
				|| source == batchSearch
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

	class DocumentHandler implements DocumentListener
	{
		public void insertUpdate(DocumentEvent evt)
		{
			// on insert, start search from beginning of
			// current match. This will continue to highlight
			// the current match until another match is found
			if(incrementalSearch.isSelected())
			{
				incrementalSearch(view.getTextArea()
					.getSelectionStart());
			}
		}

		public void removeUpdate(DocumentEvent evt)
		{
			// on backspace, restart from beginning
			// when we write reverse search, implement real
			// backtracking
			if(incrementalSearch.isSelected())
			{
				String text = find.getText();
				if(text != null && text.length() != 0)
					incrementalSearch(0);
			}
		}

		public void changedUpdate(DocumentEvent evt)
		{
		}
	}
}
