/*
 * HyperSearch.java - HyperSearch dialog
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

package org.gjt.sp.jedit.gui;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import gnu.regexp.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import org.gjt.sp.jedit.gui.SyntaxTextArea;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;

/**
 * HyperSearch dialog.
 * @author Slava Pestov
 * @version $Id$
 */
public class HyperSearch extends JDialog
implements ActionListener, KeyListener, ListSelectionListener
{
	public HyperSearch(View view)
	{
		super(view,jEdit.getProperty("hypersearch.title"),false);
		this.view = view;
		positions = new Vector();
		Container content = getContentPane();
		content.setLayout(new BorderLayout());
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JLabel(jEdit.getProperty("hypersearch.find")),
			BorderLayout.WEST);
		find = new HistoryTextField("find");
		panel.add(find, BorderLayout.CENTER);
		content.add(panel, BorderLayout.NORTH);

		JPanel stretchPanel = new JPanel(new BorderLayout());

		panel = new JPanel();
		ignoreCase = new JCheckBox(jEdit.getProperty(
			"search.ignoreCase"),
			"on".equals(jEdit.getProperty("search.ignoreCase.toggle")));
		panel.add(ignoreCase);
		panel.add(new JLabel(jEdit.getProperty("search.regexp")));
		regexpSyntax = new JComboBox(jEdit.SYNTAX_LIST);
		regexpSyntax.setSelectedItem(jEdit.getProperty("search"
			+ ".regexp.value"));
		panel.add(regexpSyntax);
		findBtn = new JButton(jEdit.getProperty("hypersearch.findBtn"));
		panel.add(findBtn);
		getRootPane().setDefaultButton(findBtn);
		close = new JButton(jEdit.getProperty("hypersearch.close"));
		panel.add(close);
		stretchPanel.add(panel,BorderLayout.NORTH);

		results = new JList();
		results.setVisibleRowCount(10);
		results.addListSelectionListener(this);
		stretchPanel.add(new JScrollPane(results), BorderLayout.CENTER);

		content.add(stretchPanel, BorderLayout.CENTER);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		find.getEditor().getEditorComponent().addKeyListener(this);
		find.addActionListener(this);
		addKeyListener(this);
		findBtn.addActionListener(this);
		close.addActionListener(this);

		pack();
		GUIUtilities.loadGeometry(this,"hypersearch");

		show();
		find.requestFocus();
	}
	
	public void save()
	{
		find.save();
		jEdit.setProperty("search.ignoreCase.toggle",ignoreCase
			.getModel().isSelected() ? "on" : "off");
		jEdit.setProperty("search.regexp.value",(String)regexpSyntax
			.getSelectedItem());
		GUIUtilities.saveGeometry(this,"hypersearch");
	}
	
	public void actionPerformed(ActionEvent evt)
	{
		Object source = evt.getSource();
		if(source == close)
			dispose();
		else if(source == findBtn || source == find)
		{
			save();
			doHyperSearch();
		}
	}

	public void keyPressed(KeyEvent evt)
	{
		if(evt.getKeyCode() == KeyEvent.VK_ESCAPE)	
			dispose();
	}

	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}

	public void valueChanged(ListSelectionEvent evt)
	{
		if(results.isSelectionEmpty())
			return;
		Position pos = (Position)positions.elementAt(results
			.getSelectedIndex());
		SyntaxTextArea textArea = view.getTextArea();
		Element map = view.getBuffer().getDefaultRootElement();
		Element lineElement = map.getElement(map.getElementIndex(pos
			.getOffset()));
		if(lineElement == null)
			return;
		textArea.select(lineElement.getStartOffset(),
			lineElement.getEndOffset() - 1);
	}

	// private members
	private View view;
	private HistoryTextField find;
	private JCheckBox ignoreCase;
	private JComboBox regexpSyntax;
	private JButton findBtn;
	private JButton close;
	private JList results;
	private Vector positions;

	private void doHyperSearch()
	{
		try
		{
			positions.removeAllElements();
			Buffer buffer = view.getBuffer();
			int tabSize = buffer.getTabSize();
			Vector data = new Vector();
			RE regexp = jEdit.getRE();
			Element map = buffer.getDefaultRootElement();
			int lines = map.getElementCount();
			for(int i = 1; i <= lines; i++)
			{
				Element lineElement = map.getElement(i - 1);
				int start = lineElement.getStartOffset();
				String lineString = buffer.getText(start,
					lineElement.getEndOffset() - start
					- 1);
				REMatch match = regexp.getMatch(lineString);
				if(match != null)
				{
					data.addElement(i + ":"
						+ lineString);
					positions.addElement(buffer
						.createPosition(start + match
						.getStartIndex()));
				}
			}
			if(data.isEmpty())
				view.getToolkit().beep();
			results.setListData(data);
		}
		catch(Exception e)
		{
			Object[] args = { e.getMessage() };
			if(args[0] == null)
				args[0] = e.toString();
			GUIUtilities.error(view,"reerror",args);
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.22  1999/03/20 04:52:55  sp
 * Buffer-specific options panel finished, attempt at fixing OS/2 caret bug, code
 * cleanups
 *
 * Revision 1.21  1999/03/19 08:32:22  sp
 * Added a status bar to views, Escape key now works in dialog boxes
 *
 * Revision 1.20  1999/03/19 07:12:11  sp
 * JOptionPane changes, did a fromdos of the source
 *
 */
