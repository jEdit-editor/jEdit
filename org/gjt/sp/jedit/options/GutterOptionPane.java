/*
 * GutterOptionPane.java - Gutter options panel
 * Copyright (C) 2000 mike dillon
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
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.FontComboBox;

public class GutterOptionPane extends AbstractOptionPane
{
	public GutterOptionPane()
	{
		super("gutter");
	}

	// protected members
	protected void _init()
	{
		gutterExpanded = new JCheckBox(jEdit.getProperty(
			"options.gutter.expanded"));
		gutterExpanded.setSelected(!jEdit.getBooleanProperty(
			"view.gutter.collapsed"));
		addComponent(gutterExpanded);

		lineNumbersEnabled = new JCheckBox(jEdit.getProperty(
			"options.gutter.lineNumbers"));
		lineNumbersEnabled.setSelected(jEdit.getBooleanProperty(
			"view.gutter.lineNumbers"));
		addComponent(lineNumbersEnabled);

		currentLineHighlightEnabled = new JCheckBox(jEdit.getProperty(
			"options.gutter.currentLineHighlight"));
		currentLineHighlightEnabled.setSelected(jEdit.getBooleanProperty(
			"view.gutter.highlightCurrentLine"));
		addComponent(currentLineHighlightEnabled);

		gutterWidth = new JTextField(jEdit.getProperty(
			"view.gutter.width"));
		addComponent(jEdit.getProperty("options.gutter.width"),
			gutterWidth);

		gutterBorderWidth = new JTextField(jEdit.getProperty(
			"view.gutter.borderWidth"));
		addComponent(jEdit.getProperty("options.gutter.borderWidth"),
			gutterBorderWidth);

		highlightInterval = new JTextField(jEdit.getProperty(
			"view.gutter.highlightInterval"));
		addComponent(jEdit.getProperty("options.gutter.interval"),
			highlightInterval);

		String[] alignments = new String[] {
			"Left", "Center", "Right"
		};
		numberAlignment = new JComboBox(alignments);
		String alignment = jEdit.getProperty("view.gutter.numberAlignment");
		if("right".equals(alignment))
			numberAlignment.setSelectedIndex(2);
		else if("center".equals(alignment))
			numberAlignment.setSelectedIndex(1);
		else
			numberAlignment.setSelectedIndex(0);
		addComponent(jEdit.getProperty("options.gutter.numberAlignment"), numberAlignment);

		font = new FontComboBox();
		font.setSelectedItem(jEdit.getProperty("view.gutter.font"));
		addComponent(jEdit.getProperty("options.gutter.font"),font);

		String[] styles = { jEdit.getProperty("options.editor.plain"),
			jEdit.getProperty("options.editor.bold"),
			jEdit.getProperty("options.editor.italic"),
			jEdit.getProperty("options.editor.boldItalic") };
		style = new JComboBox(styles);
		try
		{
			style.setSelectedIndex(Integer.parseInt(jEdit
				.getProperty("view.gutter.fontstyle")));
		}
		catch(NumberFormatException nf)
		{
		}
		addComponent(jEdit.getProperty("options.gutter.fontstyle"),
			style);

		String[] sizes = { "9", "10", "12", "14", "18", "24" };
		size = new JComboBox(sizes);
		size.setEditable(true);
		size.setSelectedItem(jEdit.getProperty("view.gutter.fontsize"));
		addComponent(jEdit.getProperty("options.gutter.fontsize"),size);
	}

	protected void _save()
	{
		jEdit.setBooleanProperty("view.gutter.collapsed",
			!gutterExpanded.isSelected());
		jEdit.setBooleanProperty("view.gutter.lineNumbers", lineNumbersEnabled
			.isSelected());
		jEdit.setBooleanProperty("view.gutter.highlightCurrentLine",
			currentLineHighlightEnabled.isSelected());
		jEdit.setProperty("view.gutter.width", gutterWidth.getText());
		jEdit.setProperty("view.gutter.borderWidth",
			gutterBorderWidth.getText());
		jEdit.setProperty("view.gutter.highlightInterval",
			highlightInterval.getText());
		String alignment = null;
		switch(numberAlignment.getSelectedIndex())
		{
		case 2:
			alignment = "right";
			break;
		case 1:
			alignment = "center";
			break;
		case 0: default:
			alignment = "left";
		}
		jEdit.setProperty("view.gutter.numberAlignment", alignment);
		jEdit.setProperty("view.gutter.font",
			font.getSelectedItem().toString());
		jEdit.setProperty("view.gutter.fontsize",
			size.getSelectedItem().toString());
		jEdit.setProperty("view.gutter.fontstyle",
			Integer.toString(style.getSelectedIndex()));
	}

	private JCheckBox gutterExpanded;
	private JCheckBox lineNumbersEnabled;
	private JCheckBox currentLineHighlightEnabled;
	private JTextField gutterWidth;
	private JTextField gutterBorderWidth;
	private JTextField highlightInterval;
	private JComboBox numberAlignment;
	private JComboBox font;
	private JComboBox style;
	private JComboBox size;
}
