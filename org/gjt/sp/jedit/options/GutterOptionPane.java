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
import org.gjt.sp.jedit.gui.FontSelector;
import org.gjt.sp.jedit.*;

public class GutterOptionPane extends AbstractOptionPane
{
	public GutterOptionPane()
	{
		super("gutter");
	}

	public void _init()
	{
		/* Font */
		String _fontFamily = jEdit.getProperty("view.gutter.font");

		int _fontStyle;
		try
		{
			_fontStyle = Integer.parseInt(jEdit.getProperty("view.gutter.fontstyle"));
		}
		catch(NumberFormatException nf)
		{
			_fontStyle = Font.PLAIN;
		}

		int _fontSize;
		try
		{
			_fontSize = Integer.parseInt(jEdit.getProperty("view.gutter.fontsize"));
		}
		catch(NumberFormatException nf)
		{
			_fontSize = 14;
		}
		gutterFont = new FontSelector(new Font(_fontFamily,_fontStyle,_fontSize));

		addComponent(jEdit.getProperty("options.gutter.font"),gutterFont);

		gutterWidth = new JTextField(jEdit.getProperty(
			"view.gutter.width"));
		addComponent(jEdit.getProperty("options.gutter.width"),
			gutterWidth);

		gutterBorderWidth = new JTextField(jEdit.getProperty(
			"view.gutter.borderWidth"));
		addComponent(jEdit.getProperty("options.gutter.borderWidth"),
			gutterBorderWidth);

		gutterHighlightInterval = new JTextField(jEdit.getProperty(
			"view.gutter.highlightInterval"));
		addComponent(jEdit.getProperty("options.gutter.interval"),
			gutterHighlightInterval);

		String[] alignments = new String[] {
			"Left", "Center", "Right"
		};
		gutterNumberAlignment = new JComboBox(alignments);
		String alignment = jEdit.getProperty("view.gutter.numberAlignment");
		if("right".equals(alignment))
			gutterNumberAlignment.setSelectedIndex(2);
		else if("center".equals(alignment))
			gutterNumberAlignment.setSelectedIndex(1);
		else
			gutterNumberAlignment.setSelectedIndex(0);
		addComponent(jEdit.getProperty("options.gutter.numberAlignment"),
			gutterNumberAlignment);

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

		gutterCurrentLineHighlightEnabled = new JCheckBox(jEdit.getProperty(
			"options.gutter.currentLineHighlight"));
		gutterCurrentLineHighlightEnabled.setSelected(jEdit.getBooleanProperty(
			"view.gutter.highlightCurrentLine"));
		addComponent(gutterCurrentLineHighlightEnabled);
	}

	public void _save()
	{
		Font _font = gutterFont.getFont();
		jEdit.setProperty("view.gutter.font",_font.getFamily());
		jEdit.setProperty("view.gutter.fontsize",String.valueOf(_font.getSize()));
		jEdit.setProperty("view.gutter.fontstyle",String.valueOf(_font.getStyle()));

		jEdit.setProperty("view.gutter.width", gutterWidth.getText());
		jEdit.setProperty("view.gutter.borderWidth",
			gutterBorderWidth.getText());
		jEdit.setProperty("view.gutter.highlightInterval",
			gutterHighlightInterval.getText());
		String alignment = null;
		switch(gutterNumberAlignment.getSelectedIndex())
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
		jEdit.setBooleanProperty("view.gutter.collapsed",
			!gutterExpanded.isSelected());
		jEdit.setBooleanProperty("view.gutter.lineNumbers", lineNumbersEnabled
			.isSelected());
		jEdit.setBooleanProperty("view.gutter.highlightCurrentLine",
			gutterCurrentLineHighlightEnabled.isSelected());
	}

	// private members
	private FontSelector gutterFont;
	private JTextField gutterWidth;
	private JTextField gutterBorderWidth;
	private JTextField gutterHighlightInterval;
	private JComboBox gutterNumberAlignment;
	private JCheckBox gutterExpanded;
	private JCheckBox lineNumbersEnabled;
	private JCheckBox gutterCurrentLineHighlightEnabled;
}

/*
 * Change Log:
 * $Log$
 * Revision 1.7  2000/11/07 10:08:32  sp
 * Options dialog improvements, documentation changes, bug fixes
 *
 */
