/*
 * StyleOptionPane.java - Color/style option pane
 * Copyright (C) 1999 Slava Pestov
 * Portions copyright (C) 1999 mike dillon
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

import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.Vector;
import org.gjt.sp.jedit.syntax.SyntaxStyle;
import org.gjt.sp.jedit.*;

/**
 * Style/color option pane.
 * @author Slava Pestov
 * @version $Id$
 */
public class StyleOptionPane extends AbstractOptionPane
{
	public static final EmptyBorder noFocusBorder = new EmptyBorder(1,1,1,1);

	public StyleOptionPane()
	{
		super("styles");

		setLayout(new GridLayout(2,1));

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(BorderLayout.NORTH,new JLabel(jEdit.getProperty(
			"options.styles.colors")));
		panel.add(BorderLayout.CENTER,createColorTableScroller());
		add(panel);

		setLayout(new GridLayout(2,1));

		panel = new JPanel(new BorderLayout());
		panel.add(BorderLayout.NORTH,new JLabel(jEdit.getProperty(
			"options.styles.styles")));
		panel.add(BorderLayout.CENTER,createStyleTableScroller());
		add(panel);
	}

	public void save()
	{
		colorModel.save();
		styleModel.save();
	}

	// private members
	private ColorTableModel colorModel;
	private JTable colorTable;
	private StyleTableModel styleModel;
	private JTable styleTable;

	private JScrollPane createColorTableScroller()
	{
		colorModel = createColorTableModel();
		colorTable = new JTable(colorModel);
		colorTable.getTableHeader().setReorderingAllowed(false);
		colorTable.getSelectionModel().addListSelectionListener(
			new ListHandler(colorTable));
		TableColumnModel tcm = colorTable.getColumnModel();
 		TableColumn colorColumn = tcm.getColumn(1);
		colorColumn.setCellRenderer(new ColorTableModel.ColorRenderer());
		Dimension d = colorTable.getPreferredSize();
		d.height = Math.min(d.height,100);
		JScrollPane scroller = new JScrollPane(colorTable);
		scroller.setPreferredSize(d);
		return scroller;
	}

	private ColorTableModel createColorTableModel()
	{
		return new ColorTableModel();
	}

	private JScrollPane createStyleTableScroller()
	{
		styleModel = createStyleTableModel();
		styleTable = new JTable(styleModel);
		styleTable.getTableHeader().setReorderingAllowed(false);
		styleTable.getSelectionModel().addListSelectionListener(
			new ListHandler(styleTable));
		TableColumnModel tcm = styleTable.getColumnModel();
 		TableColumn styleColumn = tcm.getColumn(1);
		styleColumn.setCellRenderer(new StyleTableModel.StyleRenderer());
		Dimension d = styleTable.getPreferredSize();
		d.height = Math.min(d.height,100);
		JScrollPane scroller = new JScrollPane(styleTable);
		scroller.setPreferredSize(d);
		return scroller;
	}

	private StyleTableModel createStyleTableModel()
	{
		return new StyleTableModel();
	}

	class ListHandler implements ListSelectionListener
	{
		JTable table;

		ListHandler(JTable table)
		{
			this.table = table;
		}

		public void valueChanged(ListSelectionEvent evt)
		{
			if(evt.getValueIsAdjusting())
				return;
			if(table == colorTable)
			{
				Color color = JColorChooser.showDialog(
					StyleOptionPane.this,
					jEdit.getProperty("colorChooser.title"),
					(Color)colorModel.getValueAt(
					table.getSelectedRow(),1));
				if(color != null)
					colorModel.setValueAt(color,table
						.getSelectedRow(),1);
			}
			else if(table == styleTable)
			{
				SyntaxStyle style = new StyleEditor(
					StyleOptionPane.this,
					(SyntaxStyle)styleModel.getValueAt(
					table.getSelectedRow(),1)).getStyle();
				if(style != null)
					styleModel.setValueAt(style,table
						.getSelectedRow(),1);
			}
		}
	}
}

class ColorTableModel extends AbstractTableModel
{
	private Vector colorChoices;

	ColorTableModel()
	{
		colorChoices = new Vector(12);
		addColorChoice("options.styles.bgColor","view.bgColor");
		addColorChoice("options.styles.fgColor","view.fgColor");
		addColorChoice("options.styles.caretColor","view.caretColor");
		addColorChoice("options.styles.selectionColor",
			"view.selectionColor");
		addColorChoice("options.styles.lineHighlightColor",
			"view.lineHighlightColor");
		addColorChoice("options.styles.bracketHighlightColor",
			"view.bracketHighlightColor");
		addColorChoice("options.styles.eolMarkerColor",
			"view.eolMarkerColor");
		addColorChoice("options.styles.gutterBgColor",
			"view.gutter.bgColor");
		addColorChoice("options.styles.gutterFgColor",
			"view.gutter.fgColor");
		addColorChoice("options.styles.gutterHighlightColor",
			"view.gutter.highlightColor");
		addColorChoice("options.styles.gutterCurrentLineColor",
			"view.gutter.currentLineColor");
		addColorChoice("options.styles.gutterBorderColor",
			"view.gutter.borderColor");
	}

	public int getColumnCount()
	{
		return 2;
	}

	public int getRowCount()
	{
		return colorChoices.size();
	}

	public Object getValueAt(int row, int col)
	{
		ColorChoice ch = (ColorChoice)colorChoices.elementAt(row);
		switch(col)
		{
		case 0:
			return ch.label;
		case 1:
			return ch.color;
		default:
			return null;
		}
	}

	public void setValueAt(Object value, int row, int col)
	{
		ColorChoice ch = (ColorChoice)colorChoices.elementAt(row);
		if(col == 1)
			ch.color = (Color)value;
		fireTableRowsUpdated(row,row);
	}

	public String getColumnName(int index)
	{
		switch(index)
		{
		case 0:
			return jEdit.getProperty("options.styles.object");
		case 1:
			return jEdit.getProperty("options.styles.color");
		default:
			return null;
		}
	}

	public void save()
	{
		for(int i = 0; i < colorChoices.size(); i++)
		{
			ColorChoice ch = (ColorChoice)colorChoices
				.elementAt(i);
			jEdit.setProperty(ch.property,
				GUIUtilities.getColorHexString(ch.color));
		}
	}

	private void addColorChoice(String label, String property)
	{
		colorChoices.addElement(new ColorChoice(jEdit.getProperty(label),
			property,GUIUtilities.parseColor(jEdit.getProperty(property))));
	}

	static class ColorChoice
	{
		String label;
		String property;
		Color color;

		ColorChoice(String label, String property, Color color)
		{
			this.label = label;
			this.property = property;
			this.color = color;
		}
	}

	static class ColorRenderer extends JLabel
		implements TableCellRenderer
	{
		public ColorRenderer()
		{
			setOpaque(true);
			setBorder(StyleOptionPane.noFocusBorder);
		}
	
		// TableCellRenderer implementation
		public Component getTableCellRendererComponent(
			JTable table,
			Object value,
			boolean isSelected,
			boolean cellHasFocus,
			int row,
			int col)
		{
			if (isSelected)
			{
				setBackground(table.getSelectionBackground());
				setForeground(table.getSelectionForeground());
			}
			else
			{
				setBackground(table.getBackground());
				setForeground(table.getForeground());
			}
	
			if (value != null)
				setBackground((Color)value);
	
			setBorder((cellHasFocus) ? UIManager.getBorder(
				"Table.focusCellHighlightBorder")
				: StyleOptionPane.noFocusBorder);
			return this;
		}
		// end TableCellRenderer implementation
	}
}

class StyleTableModel extends AbstractTableModel
{
	private Vector styleChoices;

	StyleTableModel()
	{
		styleChoices = new Vector(10);
		addStyleChoice("options.styles.comment1Style","view.style.comment1");
		addStyleChoice("options.styles.comment2Style","view.style.comment2");
		addStyleChoice("options.styles.literal1Style","view.style.literal1");
		addStyleChoice("options.styles.literal2Style","view.style.literal2");
		addStyleChoice("options.styles.labelStyle","view.style.label");
		addStyleChoice("options.styles.keyword1Style","view.style.keyword1");
		addStyleChoice("options.styles.keyword2Style","view.style.keyword2");
		addStyleChoice("options.styles.keyword3Style","view.style.keyword3");
		addStyleChoice("options.styles.functionStyle","view.style.function");
		addStyleChoice("options.styles.markupStyle","view.style.markup");
		addStyleChoice("options.styles.operatorStyle","view.style.operator");
		addStyleChoice("options.styles.digitStyle","view.style.digit");
		addStyleChoice("options.styles.invalidStyle","view.style.invalid");
	}

	public int getColumnCount()
	{
		return 2;
	}

	public int getRowCount()
	{
		return styleChoices.size();
	}

	public Object getValueAt(int row, int col)
	{
		StyleChoice ch = (StyleChoice)styleChoices.elementAt(row);
		switch(col)
		{
		case 0:
			return ch.label;
		case 1:
			return ch.style;
		default:
			return null;
		}
	}

	public void setValueAt(Object value, int row, int col)
	{
		StyleChoice ch = (StyleChoice)styleChoices.elementAt(row);
		if(col == 1)
			ch.style = (SyntaxStyle)value;
		fireTableRowsUpdated(row,row);
	}

	public String getColumnName(int index)
	{
		switch(index)
		{
		case 0:
			return jEdit.getProperty("options.styles.object");
		case 1:
			return jEdit.getProperty("options.styles.style");
		default:
			return null;
		}
	}

	public void save()
	{
		for(int i = 0; i < styleChoices.size(); i++)
		{
			StyleChoice ch = (StyleChoice)styleChoices
				.elementAt(i);
			jEdit.setProperty(ch.property,
				GUIUtilities.getStyleString(ch.style));
		}
	}

	private void addStyleChoice(String label, String property)
	{
		styleChoices.addElement(new StyleChoice(jEdit.getProperty(label),
			property,
			GUIUtilities.parseStyle(jEdit.getProperty(property))));
	}

	static class StyleChoice
	{
		String label;
		String property;
		SyntaxStyle style;

		StyleChoice(String label, String property, SyntaxStyle style)
		{
			this.label = label;
			this.property = property;
			this.style = style;
		}
	}

	static class StyleRenderer extends JLabel
		implements TableCellRenderer
	{
		public StyleRenderer()
		{
			setOpaque(true);
			setBorder(StyleOptionPane.noFocusBorder);
			setText("Hello World");
		}
	
		// TableCellRenderer implementation
		public Component getTableCellRendererComponent(
			JTable table,
			Object value,
			boolean isSelected,
			boolean cellHasFocus,
			int row,
			int col)
		{
			if (isSelected)
			{
				setBackground(table.getSelectionBackground());
				setForeground(table.getSelectionForeground());
			}
			else
			{
				setBackground(table.getBackground());
				setForeground(table.getForeground());
			}
	
			if (value != null)
			{
				SyntaxStyle style = (SyntaxStyle)value;
				setForeground(style.getColor());
				setFont(style.getStyledFont(getFont()));
			}
	
			setBorder((cellHasFocus) ? UIManager.getBorder(
				"Table.focusCellHighlightBorder")
				: StyleOptionPane.noFocusBorder);
			return this;
		}
		// end TableCellRenderer implementation
	}
}

class StyleEditor extends JDialog
implements ActionListener, KeyListener
{
	StyleEditor(Component comp, SyntaxStyle style)
	{
		super(JOptionPane.getFrameForComponent(comp),
			jEdit.getProperty("styleEditor.title"),true);

		getContentPane().setLayout(new BorderLayout());

		JPanel panel = new JPanel();
		panel.add(italics = new JCheckBox(
			jEdit.getProperty("styleEditor.italics")));
		italics.getModel().setSelected(style.isItalic());
		panel.add(bold = new JCheckBox(
			jEdit.getProperty("styleEditor.bold")));
		bold.getModel().setSelected(style.isBold());
		panel.add(new JLabel(
			jEdit.getProperty("styleEditor.color")));
		panel.add(color = new JButton("    "));
		color.setBackground(style.getColor());
		color.setRequestFocusEnabled(false);
		color.addActionListener(this);

		getContentPane().add(BorderLayout.CENTER,panel);

		panel = new JPanel();
		panel.add(ok = new JButton(jEdit.getProperty("common.ok")));
		getRootPane().setDefaultButton(ok);
		ok.addActionListener(this);
		panel.add(cancel = new JButton(jEdit.getProperty("common.cancel")));
		cancel.addActionListener(this);

		getContentPane().add(BorderLayout.SOUTH,panel);

		addKeyListener(this);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		Dimension screen = getToolkit().getScreenSize();
		pack();
		setLocation((screen.width - getSize().width) / 2,
			(screen.height - getSize().height) / 2);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		show();
	}

	public void actionPerformed(ActionEvent evt)
	{
		Object source = evt.getSource();
		if(source == ok)
		{
			okClicked = true;
			dispose();
		}
		else if(source == cancel)
			dispose();
		else if(source == color)
		{
			Color c = JColorChooser.showDialog(this,
				jEdit.getProperty("colorChooser.title"),
				color.getBackground());
			if(c != null)
				color.setBackground(c);
		}
	}

	public void keyPressed(KeyEvent evt)
	{
		switch(evt.getKeyCode())
		{
		case KeyEvent.VK_ENTER:
			okClicked = true;
			dispose();
			evt.consume();
			break;
		case KeyEvent.VK_ESCAPE:
			dispose();
			evt.consume();
			break;
		}
	}

	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}

	public SyntaxStyle getStyle()
	{
		if(!okClicked)
			return null;
		return new SyntaxStyle(color.getBackground(),
				italics.getModel().isSelected(),
				bold.getModel().isSelected());
	}

	// private members
	private JCheckBox italics;
	private JCheckBox bold;
	private JButton color;
	private JButton ok;
	private JButton cancel;
	private boolean okClicked;
}
/**
 * ChangeLog:
 * $Log$
 * Revision 1.12  2000/04/08 02:39:33  sp
 * New Token.MARKUP type, remove Token.{CONSTANT,VARIABLE,DATATYPE}
 *
 * Revision 1.11  2000/04/06 13:09:46  sp
 * More token types added
 *
 * Revision 1.10  2000/03/21 07:18:53  sp
 * bug fixes
 *
 * Revision 1.9  2000/02/04 05:50:27  sp
 * More gutter updates from mike
 *
 * Revision 1.8  1999/12/05 03:01:05  sp
 * Perl token marker bug fix, file loading is deferred, style option pane fix
 *
 * Revision 1.7  1999/10/04 03:20:51  sp
 * Option pane change, minor tweaks and bug fixes
 *
 * Revision 1.6  1999/09/30 12:21:04  sp
 * No net access for a month... so here's one big jEdit 2.1pre1
 *
 * Revision 1.5  1999/07/16 23:45:49  sp
 * 1.7pre6 BugFree version
 *
 * Revision 1.4  1999/07/08 06:06:04  sp
 * Bug fixes and miscallaneous updates
 *
 * Revision 1.3  1999/07/05 04:38:39  sp
 * Massive batch of changes... bug fixes, also new text component is in place.
 * Have fun
 *
 * Revision 1.2  1999/06/16 03:29:59  sp
 * Added <title> tags to docs, configuration data is now stored in a
 * ~/.jedit directory, style option pane finished
 *
 * Revision 1.1  1999/06/12 02:30:27  sp
 * Find next can now perform multifile searches, multifile-search command added,
 * new style option pane
 *
 */
