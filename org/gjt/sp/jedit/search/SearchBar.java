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
		label.setBorder(new EmptyBorder(0,0,0,12));
		add(label,BorderLayout.WEST);
		Box box = new Box(BoxLayout.Y_AXIS);
		box.add(Box.createGlue());
		box.add(find = new HistoryTextField("find"));
		find.setFont(plainFont);
		Dimension min = find.getMinimumSize();
		min.width = Integer.MAX_VALUE;
		find.setMaximumSize(min);
		ActionHandler handler = new ActionHandler();
		find.addKeyListener(new KeyHandler());
		find.addActionListener(new ActionHandler());
		find.getDocument().addDocumentListener(new DocumentHandler());
		box.add(Box.createGlue());
		add(box,BorderLayout.CENTER);

		Insets margin = new Insets(1,1,1,1);

		Box buttons = new Box(BoxLayout.X_AXIS);
		buttons.add(Box.createHorizontalStrut(12));
		buttons.add(ignoreCase = new JCheckBox(jEdit.getProperty(
			"search.case")));
		ignoreCase.setFont(boldFont);
		ignoreCase.addActionListener(new ActionHandler());
		ignoreCase.setMargin(margin);
		buttons.add(Box.createHorizontalStrut(2));
		buttons.add(regexp = new JCheckBox(jEdit.getProperty(
			"search.regexp")));
		regexp.setFont(boldFont);
		regexp.addActionListener(new ActionHandler());
		regexp.setMargin(margin);
		buttons.add(Box.createHorizontalStrut(2));
		buttons.add(batch = new JCheckBox(jEdit.getProperty(
			"search.batch")));
		batch.setFont(boldFont);
		batch.addActionListener(new ActionHandler());
		batch.setMargin(margin);

		update();

		add(buttons,BorderLayout.EAST);
	}

	public HistoryTextField getField()
	{
		return find;
	}

	public void setBatch(boolean batch)
	{
		this.batch.setSelected(batch);
	}

	public void update()
	{
		ignoreCase.setSelected(SearchAndReplace.getIgnoreCase());
		regexp.setSelected(SearchAndReplace.getRegexp());
	}

	// private members
	private View view;
	private HistoryTextField find;
	private JCheckBox ignoreCase, regexp, batch;

	private void incrementalSearch(int start)
	{
		SearchAndReplace.setSearchString(find.getText());

		try
		{
			if(SearchAndReplace.find(view,view.getBuffer(),start))
				return;
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
		catch(Exception ia)
		{
			// invalid regexp, ignore
		}

		view.getToolkit().beep();
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(evt.getSource() == find)
			{
				String text = find.getText();
				if(text == null && text.length() == 0)
				{
					SearchAndReplace.showSearchDialog(view,null);
				}
				else if(batch.isSelected())
				{
					find.addCurrentToHistory();
					find.setText(null);
					SearchAndReplace.setSearchString(text);
					SearchAndReplace.batchSearch(view);
				}
				else
				{
					// on enter, start search from end
					// of current match to find next one
					find.addCurrentToHistory();
					incrementalSearch(view.getTextArea()
						.getSelectionEnd());
				}
			}
			else if(evt.getSource() == batch)
			{
				if(batch.isSelected())
				{
					jEdit.setProperty("search.mode.value",
						"batch");
				}
			}
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
			if(!batch.isSelected())
			{
				incrementalSearch(view.getTextArea()
					.getSelectionStart());
			}
		}

		public void removeUpdate(DocumentEvent evt)
		{
			// on backspace, restart from beginning
			// when we write reverse search, implement real
			// backtracking
			if(!batch.isSelected())
			{
				String text = find.getText();
				if(text != null && text.length() != 0)
					incrementalSearch(0);
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
				view.getEditPane().focusOnTextArea();
			}
		}
	}
}
