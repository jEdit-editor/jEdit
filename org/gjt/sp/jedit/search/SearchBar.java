/*
 * SearchBar.java - Search & replace toolbar
 * Portions copyright (C) 2000 Slava Pestov
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

package org.gjt.sp.jedit.search;
import java.awt.event.*;
import java.awt.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.BadLocationException;
import javax.swing.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.HistoryTextField;
import org.gjt.sp.util.Log;

public class SearchBar extends JPanel
{
	public SearchBar(View view)
	{
		super(new BorderLayout());

		this.view = view;

		Font boldFont = new Font("Dialog",Font.BOLD,10);
		Font plainFont = new Font("Dialog",Font.PLAIN,10);

		JLabel label = new JLabel(jEdit.getProperty("view.search.find"));
		label.setFont(boldFont);
		label.setBorder(new EmptyBorder(0,2,0,12));
		add(label,BorderLayout.WEST);
		Box box = new Box(BoxLayout.Y_AXIS);
		box.add(Box.createGlue());
		box.add(find = new HistoryTextField("find"));
		find.setFont(plainFont);
		Dimension min = find.getMinimumSize();
		min.width = Integer.MAX_VALUE;
		find.setMaximumSize(min);
		ActionHandler actionHandler = new ActionHandler();
		find.addKeyListener(new KeyHandler());
		find.addActionListener(actionHandler);
		find.getDocument().addDocumentListener(new DocumentHandler());
		box.add(Box.createGlue());
		add(box,BorderLayout.CENTER);

		Insets margin = new Insets(1,1,1,1);

		Box buttons = new Box(BoxLayout.X_AXIS);
		buttons.add(Box.createHorizontalStrut(12));
		buttons.add(ignoreCase = new JCheckBox(jEdit.getProperty(
			"search.case")));
		ignoreCase.setFont(boldFont);
		ignoreCase.addActionListener(actionHandler);
		ignoreCase.setMargin(margin);
		buttons.add(Box.createHorizontalStrut(2));
		buttons.add(regexp = new JCheckBox(jEdit.getProperty(
			"search.regexp")));
		regexp.setFont(boldFont);
		regexp.addActionListener(actionHandler);
		regexp.setMargin(margin);
		buttons.add(Box.createHorizontalStrut(2));
		buttons.add(hyperSearch = new JCheckBox(jEdit.getProperty(
			"search.hypersearch")));
		hyperSearch.setFont(boldFont);
		hyperSearch.addActionListener(actionHandler);
		hyperSearch.setMargin(margin);

		update();

		add(buttons,BorderLayout.EAST);
	}

	public HistoryTextField getField()
	{
		return find;
	}

	public void update()
	{
		ignoreCase.setSelected(SearchAndReplace.getIgnoreCase());
		regexp.setSelected(SearchAndReplace.getRegexp());
		setHyperSearch(hyperSearch.isSelected());
	}

	public void setHyperSearch(boolean hyperSearch)
	{
		jEdit.setBooleanProperty("search.hypersearch.toggle",hyperSearch);
		this.hyperSearch.setSelected(hyperSearch);
		find.setModel(hyperSearch ? "find" : null);
	}

	// private members
	private View view;
	private HistoryTextField find;
	private JCheckBox ignoreCase, regexp, hyperSearch;

	private boolean incrementalSearch(int start)
	{
		/* For example, if the current fileset is a directory,
		 * C+g will find the next match within that fileset.
		 * This can be annoying if you have just done an
		 * incremental search and want the next occurrence
		 * in the current buffer. */
		SearchAndReplace.setSearchFileSet(new CurrentBufferSet());

		SearchAndReplace.setSearchString(find.getText());

		try
		{
			if(SearchAndReplace.find(view,view.getBuffer(),start))
				return true;
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
		catch(Exception ia)
		{
			// invalid regexp, ignore
			// return true to avoid annoying beeping while
			// typing a re
			return true;
		}

		return false;
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(evt.getSource() == find)
			{
				String text = find.getText();
				if(text.length() == 0)
				{
					jEdit.setBooleanProperty("search.hypersearch.toggle",
						hyperSearch.isSelected());
					SearchAndReplace.showSearchDialog(view,null);
				}
				else if(hyperSearch.isSelected())
				{
					find.setText(null);
					SearchAndReplace.setSearchString(text);
					SearchAndReplace.setSearchFileSet(new CurrentBufferSet());
					SearchAndReplace.hyperSearch(view);
				}
				else
				{
					// on enter, start search from end
					// of current match to find next one
					if(!incrementalSearch(view.getTextArea()
						.getSelectionEnd()))
					{
						// not found. start from
						// beginning
						if(!incrementalSearch(0))
						{
							// not found at all. beep.
							getToolkit().beep();
						}
					}
				}
			}
			else if(evt.getSource() == hyperSearch)
				update();
			else if(evt.getSource() == ignoreCase)
			{
				SearchAndReplace.setIgnoreCase(ignoreCase
					.isSelected());
			}
			else if(evt.getSource() == regexp)
			{
				SearchAndReplace.setRegexp(regexp
					.isSelected());
			}
		}
	}

	class DocumentHandler implements DocumentListener
	{
		public void insertUpdate(DocumentEvent evt)
		{
			// on insert, start search from beginning of
			// current match. This will continue to highlight
			// the current match until another match is found
			if(!hyperSearch.isSelected())
			{
				if(!incrementalSearch(view.getTextArea()
					.getSelectionStart()))
					getToolkit().beep();
			}
		}

		public void removeUpdate(DocumentEvent evt)
		{
			// on backspace, restart from beginning
			// when we write reverse search, implement real
			// backtracking
			if(!hyperSearch.isSelected())
			{
				String text = find.getText();
				if(text.length() != 0)
				{
					// don't beep if not found.
					// subsequent beeps are very
					// annoying when backspacing an
					// invalid search string.
					incrementalSearch(0);
				}
			}
		}

		public void changedUpdate(DocumentEvent evt)
		{
		}
	}

	class KeyHandler extends KeyAdapter
	{
		public void keyPressed(KeyEvent evt)
		{
			if(evt.getKeyCode() == KeyEvent.VK_ESCAPE)
			{
				evt.consume();
				view.getEditPane().focusOnTextArea();
			}
		}
	}
}
