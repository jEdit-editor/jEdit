/*
 * TextAreaAndGutterOptionPane.java - User interface options panel
 * Copyright (C) 1998, 2000, 2000 Slava Pestov
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
import org.gjt.sp.jedit.gui.FontComboBox;
import org.gjt.sp.jedit.*;

public class TextAreaAndGutterOptionPane extends AbstractOptionPane
{
	public TextAreaAndGutterOptionPane()
	{
		super("textarea-gutter");
	}

	public void _init()
	{
		addSeparator("options.textarea-gutter.textarea");

		/* Font */
		font = new FontComboBox();
		font.setSelectedItem(jEdit.getProperty("view.font"));
		addComponent(jEdit.getProperty("options.textarea-gutter.font"),font);

		/* Font style */
		String[] styles = { jEdit.getProperty("options.textarea-gutter.plain"),
			jEdit.getProperty("options.textarea-gutter.bold"),
			jEdit.getProperty("options.textarea-gutter.italic"),
			jEdit.getProperty("options.textarea-gutter.boldItalic") };
		style = new JComboBox(styles);
		try
		{
			style.setSelectedIndex(Integer.parseInt(jEdit
				.getProperty("view.fontstyle")));
		}
		catch(NumberFormatException nf)
		{
		}
		addComponent(jEdit.getProperty("options.textarea-gutter.fontstyle"),
			style);

		/* Font size */
		String[] sizes = { "9", "10", "12", "14", "18", "24" };
		size = new JComboBox(sizes);
		size.setEditable(true);
		size.setSelectedItem(jEdit.getProperty("view.fontsize"));
		addComponent(jEdit.getProperty("options.textarea-gutter.fontsize"),size);

		/* Line highlight */
		lineHighlight = new JCheckBox(jEdit.getProperty("options.textarea-gutter"
			+ ".lineHighlight"));
		lineHighlight.setSelected(jEdit.getBooleanProperty("view.lineHighlight"));
		addComponent(lineHighlight);

		/* Bracket highlight */
		bracketHighlight = new JCheckBox(jEdit.getProperty("options.textarea-gutter"
			+ ".bracketHighlight"));
		bracketHighlight.setSelected(jEdit.getBooleanProperty(
			"view.bracketHighlight"));
		addComponent(bracketHighlight);

		/* EOL markers */
		eolMarkers = new JCheckBox(jEdit.getProperty("options.textarea-gutter"
			+ ".eolMarkers"));
		eolMarkers.setSelected(jEdit.getBooleanProperty("view.eolMarkers"));
		addComponent(eolMarkers);

		/* Paint invalid */
		paintInvalid = new JCheckBox(jEdit.getProperty("options.textarea-gutter"
			+ ".paintInvalid"));
		paintInvalid.setSelected(jEdit.getBooleanProperty("view.paintInvalid"));
		addComponent(paintInvalid);

		/* Blinking caret */
		blinkCaret = new JCheckBox(jEdit.getProperty("options.textarea-gutter"
			+ ".blinkCaret"));
		blinkCaret.setSelected(jEdit.getBooleanProperty("view.caretBlink"));
		addComponent(blinkCaret);

		/* Block caret */
		blockCaret = new JCheckBox(jEdit.getProperty("options.textarea-gutter"
			+ ".blockCaret"));
		blockCaret.setSelected(jEdit.getBooleanProperty("view.blockCaret"));
		addComponent(blockCaret);

		/* Electric borders */
		electricBorders = new JCheckBox(jEdit.getProperty("options.textarea-gutter"
			+ ".electricBorders"));
		electricBorders.setSelected(!"0".equals(jEdit.getProperty(
			"view.electricBorders")));
		addComponent(electricBorders);

		/* Smart home/end */
		homeEnd = new JCheckBox(jEdit.getProperty("options.textarea-gutter"
			+ ".homeEnd"));
		homeEnd.setSelected(jEdit.getBooleanProperty("view.homeEnd"));
		addComponent(homeEnd);

		addSeparator("options.textarea-gutter.gutter");
		gutterExpanded = new JCheckBox(jEdit.getProperty(
			"options.textarea-gutter.gutter.expanded"));
		gutterExpanded.setSelected(!jEdit.getBooleanProperty(
			"view.gutter.collapsed"));
		addComponent(gutterExpanded);

		lineNumbersEnabled = new JCheckBox(jEdit.getProperty(
			"options.textarea-gutter.gutter.lineNumbers"));
		lineNumbersEnabled.setSelected(jEdit.getBooleanProperty(
			"view.gutter.lineNumbers"));
		addComponent(lineNumbersEnabled);

		gutterCurrentLineHighlightEnabled = new JCheckBox(jEdit.getProperty(
			"options.textarea-gutter.gutter.currentLineHighlight"));
		gutterCurrentLineHighlightEnabled.setSelected(jEdit.getBooleanProperty(
			"view.gutter.highlightCurrentLine"));
		addComponent(gutterCurrentLineHighlightEnabled);

		gutterWidth = new JTextField(jEdit.getProperty(
			"view.gutter.width"));
		addComponent(jEdit.getProperty("options.textarea-gutter.gutter.width"),
			gutterWidth);

		gutterBorderWidth = new JTextField(jEdit.getProperty(
			"view.gutter.borderWidth"));
		addComponent(jEdit.getProperty("options.textarea-gutter.gutter.borderWidth"),
			gutterBorderWidth);

		gutterHighlightInterval = new JTextField(jEdit.getProperty(
			"view.gutter.highlightInterval"));
		addComponent(jEdit.getProperty("options.textarea-gutter.gutter.interval"),
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
		addComponent(jEdit.getProperty("options.textarea-gutter.gutter.numberAlignment"),
			gutterNumberAlignment);

		gutterFont = new FontComboBox();
		gutterFont.setSelectedItem(jEdit.getProperty("view.gutter.font"));
		addComponent(jEdit.getProperty("options.textarea-gutter.gutter.font"),gutterFont);

		gutterStyle = new JComboBox(styles);
		try
		{
			gutterStyle.setSelectedIndex(Integer.parseInt(jEdit
				.getProperty("view.gutter.fontstyle")));
		}
		catch(NumberFormatException nf)
		{
		}
		addComponent(jEdit.getProperty("options.textarea-gutter.gutter.fontstyle"),
			gutterStyle);

		gutterSize = new JComboBox(sizes);
		gutterSize.setEditable(true);
		gutterSize.setSelectedItem(jEdit.getProperty("view.gutter.fontsize"));
		addComponent(jEdit.getProperty("options.textarea-gutter.gutter.fontsize"),gutterSize);
	}

	public void _save()
	{
		jEdit.setProperty("view.font",(String)font.getSelectedItem());
		jEdit.setProperty("view.fontsize",(String)size.getSelectedItem());
		jEdit.setProperty("view.fontstyle",String.valueOf(style
			.getSelectedIndex()));
		jEdit.setBooleanProperty("view.lineHighlight",lineHighlight
			.isSelected());
		jEdit.setBooleanProperty("view.bracketHighlight",bracketHighlight
			.isSelected());
		jEdit.setBooleanProperty("view.eolMarkers",eolMarkers
			.isSelected());
		jEdit.setBooleanProperty("view.paintInvalid",paintInvalid
			.isSelected());
		jEdit.setBooleanProperty("view.caretBlink",blinkCaret.isSelected());
		jEdit.setBooleanProperty("view.blockCaret",blockCaret.isSelected());
		jEdit.setProperty("view.electricBorders",electricBorders
			.isSelected() ? "3" : "0");
		jEdit.setBooleanProperty("view.homeEnd",homeEnd.isSelected());

		jEdit.setBooleanProperty("view.gutter.collapsed",
			!gutterExpanded.isSelected());
		jEdit.setBooleanProperty("view.gutter.lineNumbers", lineNumbersEnabled
			.isSelected());
		jEdit.setBooleanProperty("view.gutter.highlightCurrentLine",
			gutterCurrentLineHighlightEnabled.isSelected());
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
		jEdit.setProperty("view.gutter.font",
			gutterFont.getSelectedItem().toString());
		jEdit.setProperty("view.gutter.fontsize",
			gutterSize.getSelectedItem().toString());
		jEdit.setProperty("view.gutter.fontstyle",
			Integer.toString(gutterStyle.getSelectedIndex()));
	}

	// private members
	private JComboBox font;
	private JComboBox style;
	private JComboBox size;
	private JCheckBox lineHighlight;
	private JCheckBox bracketHighlight;
	private JCheckBox eolMarkers;
	private JCheckBox paintInvalid;
	private JCheckBox blinkCaret;
	private JCheckBox blockCaret;
	private JCheckBox electricBorders;
	private JCheckBox homeEnd;

	private JCheckBox gutterExpanded;
	private JCheckBox lineNumbersEnabled;
	private JCheckBox gutterCurrentLineHighlightEnabled;
	private JTextField gutterWidth;
	private JTextField gutterBorderWidth;
	private JTextField gutterHighlightInterval;
	private JComboBox gutterNumberAlignment;
	private JComboBox gutterFont;
	private JComboBox gutterStyle;
	private JComboBox gutterSize;
}

/*
 * Change Log:
 * $Log$
 * Revision 1.1  2000/08/05 07:16:12  sp
 * Global options dialog box updated, VFS browser now supports right-click menus
 *
 */
