/*
 * BufferOptions.java - Buffer-specific options dialog
 * Copyright (C) 1999 Slava Pestov
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
import org.gjt.sp.jedit.*;

/**
 * Buffer-specific options dialog.
 * @author Slava Pestov
 * @version $Id$
 */
public class BufferOptions extends EnhancedDialog
{
	public BufferOptions(View view)
	{
		super(view,jEdit.getProperty("buffer-options.title"),true);
		this.view = view;
		this.buffer = view.getBuffer();

		ActionHandler actionListener = new ActionHandler();
		JPanel panel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		panel.setLayout(layout);

		GridBagConstraints cons = new GridBagConstraints();
		cons.gridx = cons.gridy = 0;
		cons.gridwidth = 3;
		cons.gridheight = 1;
		cons.fill = GridBagConstraints.BOTH;
		cons.weightx = 1.0f;

		// Tab size
		JLabel label = new JLabel(jEdit.getProperty(
			"options.editor.tabSize"),SwingConstants.RIGHT);
		layout.setConstraints(label,cons);
		panel.add(label);

		cons.gridx = 3;
		cons.gridwidth = 1;
		String[] tabSizes = { "2", "4", "8" };
		tabSize = new JComboBox(tabSizes);
		tabSize.setEditable(true);
		tabSize.setSelectedItem(buffer.getProperty("tabSize"));
		tabSize.addActionListener(actionListener);
		layout.setConstraints(tabSize,cons);
		panel.add(tabSize);

		// Edit mode
		cons.gridx = 0;
		cons.gridy = 1;
		cons.gridwidth = 3;
		label = new JLabel(jEdit.getProperty(
			"buffer-options.mode"),SwingConstants.RIGHT);
		layout.setConstraints(label,cons);
		panel.add(label);

		cons.gridx = 3;
		cons.gridwidth = 1;
		modes = jEdit.getModes();
		String bufferMode = buffer.getMode().getName();
		int index = 0;
		String[] modeNames = new String[modes.length];
		for(int i = 0; i < modes.length; i++)
		{
			Mode mode = modes[i];
			modeNames[i] = mode.getName()
				+ " (" + mode.getProperty("label") + ")";
			if(bufferMode.equals(mode.getName()))
				index = i;
		}
		mode = new JComboBox(modeNames);
		mode.setSelectedIndex(index);
		mode.addActionListener(actionListener);
		layout.setConstraints(mode,cons);
		panel.add(mode);

		// Line separator
		cons.gridx = 0;
		cons.gridy = 2;
		cons.gridwidth = 3;
		label = new JLabel(jEdit.getProperty("buffer-options.lineSeparator"),
			SwingConstants.RIGHT);
		layout.setConstraints(label,cons);
		panel.add(label);

		cons.gridx = 3;
		cons.gridwidth = 1;
		String[] lineSeps = { jEdit.getProperty("lineSep.unix"),
			jEdit.getProperty("lineSep.windows"),
			jEdit.getProperty("lineSep.mac") };
		lineSeparator = new JComboBox(lineSeps);
		String lineSep = (String)buffer.getProperty(Buffer.LINESEP);
		if(lineSep == null)
			lineSep = System.getProperty("line.separator");
		if("\n".equals(lineSep))
			lineSeparator.setSelectedIndex(0);
		else if("\r\n".equals(lineSep))
			lineSeparator.setSelectedIndex(1);
		else if("\r".equals(lineSep))
			lineSeparator.setSelectedIndex(2);
		lineSeparator.addActionListener(actionListener);
		layout.setConstraints(lineSeparator,cons);
		panel.add(lineSeparator);

		// Syntax colorizing
		cons.gridx = 0;
		cons.gridy = 3;
		cons.gridwidth = cons.REMAINDER;
		cons.fill = GridBagConstraints.NONE;
		cons.anchor = GridBagConstraints.WEST;
		syntax = new JCheckBox(jEdit.getProperty(
			"options.editor.syntax"));
		syntax.getModel().setSelected(buffer.getBooleanProperty("syntax"));
		syntax.addActionListener(actionListener);
		layout.setConstraints(syntax,cons);
		panel.add(syntax);

		// Indent on tab
		cons.gridy = 4;
		indentOnTab = new JCheckBox(jEdit.getProperty(
			"options.editor.indentOnTab"));
		indentOnTab.getModel().setSelected(buffer
			.getBooleanProperty("indentOnTab"));
		indentOnTab.addActionListener(actionListener);
		layout.setConstraints(indentOnTab,cons);
		panel.add(indentOnTab);

		// Indent on enter
		cons.gridy = 5;
		indentOnEnter = new JCheckBox(jEdit.getProperty(
			"options.editor.indentOnEnter"));
		indentOnEnter.getModel().setSelected(buffer
			.getBooleanProperty("indentOnEnter"));
		indentOnEnter.addActionListener(actionListener);
		layout.setConstraints(indentOnEnter,cons);
		panel.add(indentOnEnter);

		// Soft tabs
		cons.gridy = 6;
		noTabs = new JCheckBox(jEdit.getProperty(
			"options.editor.noTabs"));
		noTabs.getModel().setSelected(buffer.getBooleanProperty("noTabs"));
		noTabs.addActionListener(actionListener);
		layout.setConstraints(noTabs,cons);
		panel.add(noTabs);

		// Props label
		cons.gridy = 7;
		label = new JLabel(jEdit.getProperty("buffer-options.props"));
		layout.setConstraints(label,cons);
		panel.add(label);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(BorderLayout.NORTH,panel);

		props = new JTextArea(4,30);
		props.setLineWrap(true);
		props.setWrapStyleWord(false);
		getContentPane().add(BorderLayout.CENTER,new JScrollPane(props));
		updatePropsField();

		panel = new JPanel();
		ok = new JButton(jEdit.getProperty("common.ok"));
		ok.addActionListener(actionListener);
		getRootPane().setDefaultButton(ok);
		panel.add(ok);
		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(actionListener);
		panel.add(cancel);
		getContentPane().add(BorderLayout.SOUTH,panel);

		pack();
		setLocationRelativeTo(view);
		show();
	}

	// EnhancedDialog implementation
	public void ok()
	{
		try
		{
			buffer.putProperty("tabSize",new Integer(
				tabSize.getSelectedItem().toString()));
		}
		catch(NumberFormatException nf)
		{
		}

		int index = mode.getSelectedIndex();
		buffer.setMode(modes[index]);

		index = lineSeparator.getSelectedIndex();
		String lineSep;
		if(index == 0)
			lineSep = "\n";
		else if(index == 1)
			lineSep = "\r\n";
		else if(index == 2)
			lineSep = "\r";
		else
			throw new InternalError();
		buffer.putProperty("lineSeparator",lineSep);

		buffer.putBooleanProperty("syntax",syntax.getModel()
			.isSelected());
		buffer.putBooleanProperty("indentOnTab",indentOnTab.getModel()
			.isSelected());
		buffer.putBooleanProperty("indentOnEnter",indentOnEnter.getModel()
			.isSelected());
		buffer.putBooleanProperty("noTabs",noTabs.getModel().isSelected());

		buffer.propertiesChanged();
		dispose();

		// Update text area
		view.getTextArea().getPainter().repaint();
	}

	public void cancel()
	{
		dispose();
	}
	// end EnhancedDialog implementation

        // private members
	private View view;
	private Buffer buffer;
	private JComboBox tabSize;
	private Mode[] modes;
	private JComboBox mode;
	private JComboBox lineSeparator;
	private JCheckBox indentOnTab;
	private JCheckBox indentOnEnter;
	private JCheckBox syntax;
	private JCheckBox noTabs;
	private JTextArea props;
	private JButton ok;
	private JButton cancel;

	private void updatePropsField()
	{
		props.setText(":tabSize=" + tabSize.getSelectedItem()
			+ ":noTabs=" + noTabs.getModel().isSelected()
			+ ":mode=" + modes[mode.getSelectedIndex()].getName()
			+ ":indentOnTab=" + indentOnTab.getModel().isSelected()
			+ ":indentOnEnter=" + indentOnEnter.getModel().isSelected()
			+ ":syntax=" + syntax.getModel().isSelected()
			+ ":");
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(source == ok)
				ok();
			else if(source == cancel)
				cancel();
			else if(source instanceof JComboBox
				|| source instanceof JCheckBox)
				updatePropsField();
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.15  2000/04/25 11:00:20  sp
 * FTP VFS hacking, some other stuff
 *
 * Revision 1.14  2000/04/15 04:14:47  sp
 * XML files updated, jEdit.get/setBooleanProperty() method added
 *
 * Revision 1.13  2000/04/07 06:57:26  sp
 * Buffer options dialog box updates, API docs updated a bit in syntax package
 *
 * Revision 1.12  1999/11/26 07:37:11  sp
 * Escape/enter handling code moved to common superclass, bug fixes
 *
 * Revision 1.11  1999/10/23 03:48:22  sp
 * Mode system overhaul, close all dialog box, misc other stuff
 *
 * Revision 1.10  1999/09/30 12:21:04  sp
 * No net access for a month... so here's one big jEdit 2.1pre1
 *
 * Revision 1.9  1999/08/21 01:48:18  sp
 * jEdit 2.0pre8
 *
 * Revision 1.8  1999/07/16 23:45:49  sp
 * 1.7pre6 BugFree version
 *
 * Revision 1.7  1999/04/19 05:44:34  sp
 * GUI updates
 *
 * Revision 1.6  1999/04/08 04:44:51  sp
 * New _setBuffer method in View class, new addTab method in Console class
 *
 * Revision 1.5  1999/04/07 05:22:46  sp
 * Buffer options bug fix, keyword map API change (get/setIgnoreCase() methods)
 *
 * Revision 1.4  1999/04/02 03:21:09  sp
 * Added manifest file, common strings such as OK, etc are no longer duplicated
 * many times in jedit_gui.props
 *
 * Revision 1.3  1999/04/02 02:39:46  sp
 * Updated docs, console fix, getDefaultSyntaxColors() method, hypersearch update
 *
 */
