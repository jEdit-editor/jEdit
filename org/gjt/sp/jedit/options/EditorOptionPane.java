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
import java.awt.*;
import org.gjt.sp.jedit.gui.FontComboBox;
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
		/* Modes */
		modes = jEdit.getModes();
		String[] modeNames = new String[modes.length];
		for(int i = 0; i < modes.length; i++)
		{
			modeNames[i] = modes[i].getName();
		}
		defaultMode = new JComboBox(modeNames);
		defaultMode.setSelectedItem(jEdit.getProperty("buffer.defaultMode"));
		addComponent(jEdit.getProperty("options.editor.defaultMode"),
			defaultMode);

		/* Font */
		font = new FontComboBox();
		font.setSelectedItem(jEdit.getProperty("view.font"));
		addComponent(jEdit.getProperty("options.editor.font"),font);

		/* Font style */
		String[] styles = { jEdit.getProperty("options.editor.plain"),
			jEdit.getProperty("options.editor.bold"),
			jEdit.getProperty("options.editor.italic"),
			jEdit.getProperty("options.editor.boldItalic") };
		style = new JComboBox(styles);
		try
		{
			style.setSelectedIndex(Integer.parseInt(jEdit
				.getProperty("view.fontstyle")));
		}
		catch(NumberFormatException nf)
		{
		}
		addComponent(jEdit.getProperty("options.editor.fontstyle"),
			style);

		/* Font size */
		String[] sizes = { "9", "10", "12", "14", "18", "24" };
		size = new JComboBox(sizes);
		size.setEditable(true);
		size.setSelectedItem(jEdit.getProperty("view.fontsize"));
		addComponent(jEdit.getProperty("options.editor.fontsize"),size);

		/* Tab size */
		String[] tabSizes = { "2", "4", "8" };
		tabSize = new JComboBox(tabSizes);
		tabSize.setEditable(true);
		tabSize.setSelectedItem(jEdit.getProperty("buffer.tabSize"));
		addComponent(jEdit.getProperty("options.editor.tabSize"),tabSize);

		/* Line highlight */
		lineHighlight = new JCheckBox(jEdit.getProperty("options.editor"
			+ ".lineHighlight"));
		lineHighlight.getModel().setSelected(jEdit.getBooleanProperty(
			"view.lineHighlight"));
		addComponent(lineHighlight);

		/* Bracket highlight */
		bracketHighlight = new JCheckBox(jEdit.getProperty("options.editor"
			+ ".bracketHighlight"));
		bracketHighlight.getModel().setSelected(jEdit.getBooleanProperty(
			"view.bracketHighlight"));
		addComponent(bracketHighlight);

		/* EOL markers */
		eolMarkers = new JCheckBox(jEdit.getProperty("options.editor"
			+ ".eolMarkers"));
		eolMarkers.getModel().setSelected(jEdit.getBooleanProperty(
			"view.eolMarkers"));
		addComponent(eolMarkers);

		/* Paint invalid */
		paintInvalid = new JCheckBox(jEdit.getProperty("options.editor"
			+ ".paintInvalid"));
		paintInvalid.getModel().setSelected(jEdit.getBooleanProperty(
			"view.paintInvalid"));
		addComponent(paintInvalid);

		/* Syntax highlighting */
		syntax = new JCheckBox(jEdit.getProperty("options.editor"
			+ ".syntax"));
		syntax.getModel().setSelected(jEdit.getBooleanProperty(
			"buffer.syntax"));
		addComponent(syntax);

		/* Indent on tab */
		indentOnTab = new JCheckBox(jEdit.getProperty("options.editor"
			+ ".indentOnTab"));
		indentOnTab.getModel().setSelected(jEdit.getBooleanProperty(
			"buffer.indentOnTab"));
		addComponent(indentOnTab);

		/* Indent on enter */
		indentOnEnter = new JCheckBox(jEdit.getProperty("options.editor"
			+ ".indentOnEnter"));
		indentOnEnter.getModel().setSelected(jEdit.getBooleanProperty(
			"buffer.indentOnEnter"));
		addComponent(indentOnEnter);

		/* Soft tabs */
		noTabs = new JCheckBox(jEdit.getProperty("options.editor"
			+ ".noTabs"));
		noTabs.getModel().setSelected(jEdit.getBooleanProperty(
			"buffer.noTabs"));
		addComponent(noTabs);

		/* Blinking caret */
		blinkCaret = new JCheckBox(jEdit.getProperty("options.editor"
			+ ".blinkCaret"));
		blinkCaret.getModel().setSelected(jEdit.getBooleanProperty(
			"view.caretBlink"));
		addComponent(blinkCaret);

		/* Block caret */
		blockCaret = new JCheckBox(jEdit.getProperty("options.editor"
			+ ".blockCaret"));
		blockCaret.getModel().setSelected(jEdit.getBooleanProperty(
			"view.blockCaret"));
		addComponent(blockCaret);

		/* Electric borders */
		electricBorders = new JCheckBox(jEdit.getProperty("options.editor"
			+ ".electricBorders"));
		electricBorders.getModel().setSelected(!"0".equals(jEdit.getProperty(
			"view.electricBorders")));
		addComponent(electricBorders);

		/* Smart home/end */
		homeEnd = new JCheckBox(jEdit.getProperty("options.editor"
			+ ".homeEnd"));
		homeEnd.getModel().setSelected(jEdit.getBooleanProperty(
			"view.homeEnd"));
		addComponent(homeEnd);
	}

	protected void _save()
	{
		jEdit.setProperty("buffer.defaultMode",
			modes[defaultMode.getSelectedIndex()].getName());
		jEdit.setProperty("view.font",(String)font.getSelectedItem());
		jEdit.setProperty("view.fontsize",(String)size.getSelectedItem());
		jEdit.setProperty("view.fontstyle",String.valueOf(style
			.getSelectedIndex()));
		jEdit.setProperty("buffer.tabSize",(String)tabSize
			.getSelectedItem());
		jEdit.setBooleanProperty("view.lineHighlight",lineHighlight.getModel()
			.isSelected());
		jEdit.setBooleanProperty("view.bracketHighlight",bracketHighlight.getModel()
			.isSelected());
		jEdit.setBooleanProperty("view.eolMarkers",eolMarkers.getModel()
			.isSelected());
		jEdit.setBooleanProperty("view.paintInvalid",paintInvalid.getModel()
			.isSelected());
		jEdit.setBooleanProperty("buffer.syntax",syntax.getModel().isSelected());
		jEdit.setBooleanProperty("buffer.indentOnTab",indentOnTab.getModel()
			.isSelected());
		jEdit.setBooleanProperty("buffer.indentOnEnter",indentOnEnter.getModel()
			.isSelected());
		jEdit.setBooleanProperty("view.caretBlink",blinkCaret.getModel()
			.isSelected());
		jEdit.setBooleanProperty("view.blockCaret",blockCaret.getModel()
			.isSelected());
		jEdit.setProperty("view.electricBorders",electricBorders.getModel()
			.isSelected() ? "3" : "0");
		jEdit.setBooleanProperty("buffer.noTabs",noTabs.getModel()
			.isSelected());
		jEdit.setBooleanProperty("view.homeEnd",homeEnd.getModel()
			.isSelected());
	}

	// private members
	private Mode[] modes;
	private JComboBox defaultMode;
	private JComboBox font;
	private JComboBox style;
	private JComboBox size;
	private JComboBox tabSize;
	private JCheckBox lineHighlight;
	private JCheckBox bracketHighlight;
	private JCheckBox eolMarkers;
	private JCheckBox paintInvalid;
	private JCheckBox syntax;
	private JCheckBox indentOnTab;
	private JCheckBox indentOnEnter;
	private JCheckBox blinkCaret;
	private JCheckBox blockCaret;
	private JCheckBox electricBorders;
	private JCheckBox noTabs;
	private JCheckBox homeEnd;
}
