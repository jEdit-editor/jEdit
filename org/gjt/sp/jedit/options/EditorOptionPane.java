/*
 * EditorOptionPane.java - Editor options panel
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

package org.gjt.sp.jedit.options;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.*;

public class EditorOptionPane extends AbstractOptionPane
{
	public EditorOptionPane()
	{
		super("editor");
	}

	// protected members
	protected void _init()
	{
		addSeparator("options.editor.global");

		/* Modes */
		Mode[] modes = jEdit.getModes();
		String defaultModeString = jEdit.getProperty("buffer.defaultMode");
		String[] modeNames = new String[modes.length];
		int index = 0;
		for(int i = 0; i < modes.length; i++)
		{
			Mode _mode = modes[i];
			modeNames[i] = _mode.getName();
			if(defaultModeString.equals(_mode.getName()))
				index = i;
		}
		defaultMode = new JComboBox(modeNames);
		defaultMode.setSelectedIndex(index);
		addComponent(jEdit.getProperty("options.editor.defaultMode"),
			defaultMode);

		/* Tab size */
		String[] tabSizes = { "2", "4", "8" };
		defaultTabSize = new JComboBox(tabSizes);
		defaultTabSize.setEditable(true);
		defaultTabSize.setSelectedItem(jEdit.getProperty("buffer.tabSize"));
		addComponent(jEdit.getProperty("options.editor.tabSize"),defaultTabSize);

		/* Undo queue size */
		undoCount = new JTextField(jEdit.getProperty("buffer.undoCount"));
		addComponent(jEdit.getProperty("options.editor.undoCount"),undoCount);

		/* Syntax highlighting */
		defaultSyntax = new JCheckBox(jEdit.getProperty("options.editor"
			+ ".syntax"));
		defaultSyntax.setSelected(jEdit.getBooleanProperty("buffer.syntax"));
		addComponent(defaultSyntax);

		/* Indent on tab */
		defaultIndentOnTab = new JCheckBox(jEdit.getProperty("options.editor"
			+ ".indentOnTab"));
		defaultIndentOnTab.setSelected(jEdit.getBooleanProperty("buffer.indentOnTab"));
		addComponent(defaultIndentOnTab);

		/* Indent on enter */
		defaultIndentOnEnter = new JCheckBox(jEdit.getProperty("options.editor"
			+ ".indentOnEnter"));
		defaultIndentOnEnter.setSelected(jEdit.getBooleanProperty("buffer.indentOnEnter"));
		addComponent(defaultIndentOnEnter);

		/* Soft tabs */
		defaultNoTabs = new JCheckBox(jEdit.getProperty("options.editor"
			+ ".noTabs"));
		defaultNoTabs.setSelected(jEdit.getBooleanProperty("buffer.noTabs"));
		addComponent(defaultNoTabs);

		addSeparator("options.editor.mode-specific");

		modeProps = new ModeProperties[modes.length];
		for(int i = 0; i < modes.length; i++)
		{
			modeProps[i] = new ModeProperties(modes[i]);
		}
		mode = new JComboBox(modeNames);
		mode.addActionListener(new ActionHandler());

		addComponent(jEdit.getProperty("options.editor.mode"),mode);

		useDefaults = new JCheckBox(jEdit.getProperty("options.editor.useDefaults"));
		useDefaults.addActionListener(new ActionHandler());
		addComponent(useDefaults);

		addComponent(jEdit.getProperty("options.editor.filenameGlob"),
			filenameGlob = new JTextField());

		addComponent(jEdit.getProperty("options.editor.firstlineGlob"),
			firstlineGlob = new JTextField());

		addComponent(jEdit.getProperty("options.editor.tabSize"),
			tabSize = new JComboBox(tabSizes));
		tabSize.setEditable(true);

		addComponent(jEdit.getProperty("options.editor.commentStart"),
			commentStart = new JTextField());

		addComponent(jEdit.getProperty("options.editor.commentEnd"),
			commentEnd = new JTextField());

		addComponent(jEdit.getProperty("options.editor.boxComment"),
			boxComment = new JTextField());

		addComponent(jEdit.getProperty("options.editor.blockComment"),
			blockComment = new JTextField());

		addComponent(jEdit.getProperty("options.editor.noWordSep"),
			noWordSep = new JTextField());

		addComponent(syntax = new JCheckBox(jEdit.getProperty(
			"options.editor.syntax")));

		addComponent(indentOnTab = new JCheckBox(jEdit.getProperty(
			"options.editor.indentOnTab")));

		addComponent(indentOnEnter = new JCheckBox(jEdit.getProperty(
			"options.editor.indentOnEnter")));

		addComponent(noTabs = new JCheckBox(jEdit.getProperty(
			"options.editor.noTabs")));

		selectMode();
	}

	protected void _save()
	{
		jEdit.setProperty("buffer.defaultMode",
			modeProps[defaultMode.getSelectedIndex()].mode.getName());
		jEdit.setProperty("buffer.tabSize",(String)defaultTabSize
			.getSelectedItem());
		jEdit.setProperty("buffer.undoCount",undoCount.getText());
		jEdit.setBooleanProperty("buffer.syntax",defaultSyntax.isSelected());
		jEdit.setBooleanProperty("buffer.indentOnTab",defaultIndentOnTab
			.isSelected());
		jEdit.setBooleanProperty("buffer.indentOnEnter",defaultIndentOnEnter
			.isSelected());
		jEdit.setBooleanProperty("buffer.noTabs",defaultNoTabs.isSelected());

		saveMode();

		for(int i = 0; i < modeProps.length; i++)
		{
			modeProps[i].save();
		}
	}

	// private members
	private JComboBox defaultMode;
	private JComboBox defaultTabSize;
	private JTextField undoCount;
	private JCheckBox defaultSyntax;
	private JCheckBox defaultIndentOnTab;
	private JCheckBox defaultIndentOnEnter;
	private JCheckBox defaultNoTabs;

	private ModeProperties[] modeProps;
	private ModeProperties current;
	private JComboBox mode;
	private JCheckBox useDefaults;
	private JTextField filenameGlob;
	private JTextField firstlineGlob;
	private JComboBox tabSize;
	private JTextField commentStart;
	private JTextField commentEnd;
	private JTextField boxComment;
	private JTextField blockComment;
	private JTextField noWordSep;
	private JCheckBox noTabs;
	private JCheckBox indentOnTab;
	private JCheckBox indentOnEnter;
	private JCheckBox syntax;

	private void saveMode()
	{
		current.useDefaults = useDefaults.isSelected();
		current.filenameGlob = filenameGlob.getText();
		current.firstlineGlob = firstlineGlob.getText();
		current.tabSize = (String)tabSize.getSelectedItem();
		current.commentStart = commentStart.getText();
		current.commentEnd = commentEnd.getText();
		current.boxComment = boxComment.getText();
		current.blockComment = blockComment.getText();
		current.noWordSep = noWordSep.getText();
		current.noTabs = noTabs.isSelected();
		current.indentOnEnter = indentOnEnter.isSelected();
		current.indentOnTab = indentOnTab.isSelected();
		current.syntax = syntax.isSelected();
	}

	private void selectMode()
	{
		current = modeProps[mode.getSelectedIndex()];
		current.edited = true;
		current.load();

		useDefaults.setSelected(current.useDefaults);
		filenameGlob.setText(current.filenameGlob);
		firstlineGlob.setText(current.firstlineGlob);
		tabSize.setSelectedItem(current.tabSize);
		commentStart.setText(current.commentStart);
		commentEnd.setText(current.commentEnd);
		boxComment.setText(current.boxComment);
		blockComment.setText(current.blockComment);
		noWordSep.setText(current.noWordSep);
		noTabs.setSelected(current.noTabs);
		indentOnTab.setSelected(current.indentOnTab);
		indentOnEnter.setSelected(current.indentOnEnter);
		syntax.setSelected(current.syntax);

		updateEnabled();
	}

	private void updateEnabled()
	{
		boolean enabled = !modeProps[mode.getSelectedIndex()].useDefaults;
		filenameGlob.setEnabled(enabled);
		firstlineGlob.setEnabled(enabled);
		tabSize.setEnabled(enabled);
		commentStart.setEnabled(enabled);
		commentEnd.setEnabled(enabled);
		boxComment.setEnabled(enabled);
		blockComment.setEnabled(enabled);
		noWordSep.setEnabled(enabled);
		noTabs.setEnabled(enabled);
		indentOnTab.setEnabled(enabled);
		indentOnEnter.setEnabled(enabled);
		syntax.setEnabled(enabled);
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(evt.getSource() == mode)
			{
				saveMode();
				selectMode();
			}
			else if(evt.getSource() == useDefaults)
			{
				modeProps[mode.getSelectedIndex()].useDefaults =
					useDefaults.isSelected();
				updateEnabled();
			}
		}
	}

	class ModeProperties
	{
		Mode mode;
		boolean edited;
		boolean loaded;

		boolean useDefaults;
		String filenameGlob;
		String firstlineGlob;
		String tabSize;
		String commentStart;
		String commentEnd;
		String boxComment;
		String blockComment;
		String noWordSep;
		boolean noTabs;
		boolean indentOnTab;
		boolean indentOnEnter;
		boolean syntax;

		ModeProperties(Mode mode)
		{
			this.mode = mode;
		}

		void load()
		{
			if(loaded)
				return;

			loaded = true;

			mode.loadIfNecessary();

			useDefaults = !jEdit.getBooleanProperty("mode."
				+ mode.getName() + ".customSettings");
			filenameGlob = (String)mode.getProperty("filenameGlob");
			firstlineGlob = (String)mode.getProperty("firstlineGlob");
			tabSize = mode.getProperty("tabSize").toString();
			commentStart = (String)mode.getProperty("commentStart");
			commentEnd = (String)mode.getProperty("commentEnd");
			boxComment = (String)mode.getProperty("boxComment");
			blockComment = (String)mode.getProperty("blockComment");
			noWordSep = (String)mode.getProperty("noWordSep");
			noTabs = mode.getBooleanProperty("noTabs");
			indentOnTab = mode.getBooleanProperty("indentOnTab");
			indentOnEnter = mode.getBooleanProperty("indentOnEnter");
			syntax = mode.getBooleanProperty("syntax");
		}

		void save()
		{
			// don't do anything if the user didn't change
			// any settings
			if(!edited)
				return;

			String prefix = "mode." + mode.getName() + ".";
			jEdit.setBooleanProperty(prefix + "customSettings",!useDefaults);

			if(useDefaults)
			{
				jEdit.resetProperty(prefix + "filenameGlob");
				jEdit.resetProperty(prefix + "firstlineGlob");
				jEdit.resetProperty(prefix + "tabSize");
				jEdit.resetProperty(prefix + "commentStart");
				jEdit.resetProperty(prefix + "commentEnd");
				jEdit.resetProperty(prefix + "boxComment");
				jEdit.resetProperty(prefix + "blockComment");
				jEdit.resetProperty(prefix + "noWordSep");
				jEdit.resetProperty(prefix + "noTabs");
				jEdit.resetProperty(prefix + "indentOnTab");
				jEdit.resetProperty(prefix + "indentOnEnter");
				jEdit.resetProperty(prefix + "syntax");
			}
			else
			{
				jEdit.setProperty(prefix + "filenameGlob",filenameGlob);
				jEdit.setProperty(prefix + "firstlineGlob",firstlineGlob);
				jEdit.setProperty(prefix + "tabSize",tabSize);
				jEdit.setProperty(prefix + "commentStart",commentStart);
				jEdit.setProperty(prefix + "commentEnd",commentEnd);
				jEdit.setProperty(prefix + "boxComment",boxComment);
				jEdit.setProperty(prefix + "blockComment",blockComment);
				jEdit.setProperty(prefix + "noWordSep",noWordSep);
				jEdit.setBooleanProperty(prefix + "noTabs",noTabs);
				jEdit.setBooleanProperty(prefix + "indentOnTab",indentOnTab);
				jEdit.setBooleanProperty(prefix + "indentOnEnter",indentOnEnter);
				jEdit.setBooleanProperty(prefix + "syntax",syntax);
			}
		}
	}
}

/*
 * Change Log:
 * $Log$
 * Revision 1.27  2000/09/26 10:19:47  sp
 * Bug fixes, spit and polish
 *
 * Revision 1.26  2000/08/05 07:16:12  sp
 * Global options dialog box updated, VFS browser now supports right-click menus
 *
 */
