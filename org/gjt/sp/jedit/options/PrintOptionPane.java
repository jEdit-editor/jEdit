/*
 * PrintOptionPane.java - Printing options panel
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
import java.awt.*;
import org.gjt.sp.jedit.gui.FontComboBox;
import org.gjt.sp.jedit.*;

public class PrintOptionPane extends AbstractOptionPane
{
	public PrintOptionPane()
	{
		super("print");
	}

	// protected members
	protected void _init()
	{
		/* Font */
		font = new FontComboBox();
		font.setSelectedItem(jEdit.getProperty("print.font"));
		addComponent(jEdit.getProperty("options.print.font"),font);

		/* Font style */
		String[] styles = { jEdit.getProperty("options.editor.plain"),
			jEdit.getProperty("options.editor.bold"),
			jEdit.getProperty("options.editor.italic"),
			jEdit.getProperty("options.editor.boldItalic") };
		style = new JComboBox(styles);
		try
		{
			style.setSelectedIndex(Integer.parseInt(jEdit
				.getProperty("print.fontstyle")));
		}
		catch(NumberFormatException nf)
		{
		}
		addComponent(jEdit.getProperty("options.print.fontstyle"),
			style);

		/* Font size */
		String[] sizes = { "9", "10", "12", "14", "18", "24" };
		size = new JComboBox(sizes);
		size.setEditable(true);
		size.setSelectedItem(jEdit.getProperty("print.fontsize"));
		addComponent(jEdit.getProperty("options.print.fontsize"),size);

		/* Margins */
		topMargin = new JTextField(jEdit.getProperty("print.margin.top"));
		addComponent(jEdit.getProperty("options.print.margin.top"),topMargin);
		leftMargin = new JTextField(jEdit.getProperty("print.margin.left"));
		addComponent(jEdit.getProperty("options.print.margin.left"),leftMargin);
		bottomMargin = new JTextField(jEdit.getProperty("print.margin.bottom"));
		addComponent(jEdit.getProperty("options.print.margin.bottom"),bottomMargin);
		rightMargin = new JTextField(jEdit.getProperty("print.margin.right"));
		addComponent(jEdit.getProperty("options.print.margin.right"),rightMargin);

		/* Header */
		printHeader = new JCheckBox(jEdit.getProperty("options.print"
			+ ".header"));
		printHeader.setSelected(jEdit.getBooleanProperty("print.header"));
		addComponent(printHeader);

		/* Footer */
		printFooter = new JCheckBox(jEdit.getProperty("options.print"
			+ ".footer"));
		printFooter.setSelected(jEdit.getBooleanProperty("print.footer"));
		addComponent(printFooter);

		/* Line numbering */
		printLineNumbers = new JCheckBox(jEdit.getProperty("options.print"
			+ ".lineNumbers"));
		printLineNumbers.setSelected(jEdit.getBooleanProperty("print.lineNumbers"));
		addComponent(printLineNumbers);

		/* Syntax highlighting */
		syntax = new JCheckBox(jEdit.getProperty("options.print"
			+ ".syntax"));
		syntax.setSelected(jEdit.getBooleanProperty("print.syntax"));
		addComponent(syntax);
	}

	protected void _save()
	{
		jEdit.setProperty("print.font",(String)font.getSelectedItem());
		jEdit.setProperty("print.fontsize",(String)size.getSelectedItem());
		jEdit.setProperty("print.fontstyle",String.valueOf(style
			.getSelectedIndex()));
		jEdit.setProperty("print.margin.top",topMargin.getText());
		jEdit.setProperty("print.margin.left",leftMargin.getText());
		jEdit.setProperty("print.margin.bottom",bottomMargin.getText());
		jEdit.setProperty("print.margin.right",rightMargin.getText());
		jEdit.setBooleanProperty("print.header",printHeader.isSelected());
		jEdit.setBooleanProperty("print.footer",printFooter.isSelected());
		jEdit.setBooleanProperty("print.lineNumbers",printLineNumbers.isSelected());
		jEdit.setBooleanProperty("print.syntax",syntax.isSelected());
	}

	// private members
	private JComboBox font;
	private JComboBox style;
	private JComboBox size;
	private JTextField topMargin;
	private JTextField leftMargin;
	private JTextField bottomMargin;
	private JTextField rightMargin;
	private JCheckBox printHeader;
	private JCheckBox printFooter;
	private JCheckBox printLineNumbers;
	private JCheckBox syntax;
}
