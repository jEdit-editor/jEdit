/*
 * ModeOptionPane.java - Mode options panel
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

package org.gjt.sp.jedit.options;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.*;

public class ModeOptionPane extends AbstractOptionPane
{
	public ModeOptionPane()
	{
		super("modes");
	}

	// protected members
	protected void _init()
	{
		Mode[] modes = jEdit.getModes();
		modeProps = new ModeProperties[modes.length];
		String[] modeNames = new String[modes.length];
		for(int i = 0; i < modes.length; i++)
		{
			Mode mode = modes[i];
			modeNames[i] = mode.getName();
			modeProps[i] = new ModeProperties(mode);
		}
		mode = new JComboBox(modeNames);
		mode.addActionListener(new ActionHandler());

		addComponent(jEdit.getProperty("options.modes.mode"),mode);

		useDefaults = new JCheckBox(jEdit.getProperty("options.modes.useDefaults"));
		useDefaults.addActionListener(new ActionHandler());
		addComponent(useDefaults);

		addComponent(jEdit.getProperty("options.modes.filenameGlob"),
			filenameGlob = new JTextField());

		addComponent(jEdit.getProperty("options.modes.firstlineGlob"),
			firstlineGlob = new JTextField());

		String[] tabSizes = { "2", "4", "8" };
		addComponent(jEdit.getProperty("options.editor.tabSize"),
			tabSize = new JComboBox(tabSizes));
		tabSize.setEditable(true);

		addComponent(jEdit.getProperty("options.modes.commentStart"),
			commentStart = new JTextField());

		addComponent(jEdit.getProperty("options.modes.commentEnd"),
			commentEnd = new JTextField());

		addComponent(jEdit.getProperty("options.modes.boxComment"),
			boxComment = new JTextField());

		addComponent(jEdit.getProperty("options.modes.blockComment"),
			blockComment = new JTextField());

		addComponent(jEdit.getProperty("options.modes.noWordSep"),
			noWordSep = new JTextField());

		addComponent(noTabs = new JCheckBox(jEdit.getProperty(
			"options.editor.noTabs")));

		addComponent(indentOnTab = new JCheckBox(jEdit.getProperty(
			"options.editor.indentOnTab")));

		addComponent(indentOnEnter = new JCheckBox(jEdit.getProperty(
			"options.editor.indentOnEnter")));

		addComponent(syntax = new JCheckBox(jEdit.getProperty(
			"options.editor.syntax")));

		selectMode();
	}

	protected void _save()
	{
		for(int i = 0; i < modeProps.length; i++)
		{
			modeProps[i].save();
		}
	}

	// private members
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
		current.mode.loadIfNecessary();

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
 * Revision 1.2  2000/05/21 03:00:51  sp
 * Code cleanups and bug fixes
 *
 * Revision 1.1  2000/05/13 05:13:31  sp
 * Mode option pane
 *
 */
