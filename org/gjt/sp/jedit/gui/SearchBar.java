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

package org.gjt.sp.jedit.gui;
import java.awt.event.*;
import java.awt.*;
import javax.swing.event.*;
import javax.swing.text.BadLocationException;
import javax.swing.*;
import org.gjt.sp.jedit.search.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class SearchBar extends JPanel
{
	public SearchBar(View view)
	{
		this.view = view;

		setLayout(new BorderLayout());
		add(new JLabel(jEdit.getProperty("view.search.find")),
			BorderLayout.WEST);
		Box box = new Box(BoxLayout.Y_AXIS);
		box.add(Box.createGlue());
		box.add(find = new HistoryTextField("find"));
		Dimension min = find.getMinimumSize();
		min.width = Integer.MAX_VALUE;
		find.setMaximumSize(min);
		ActionHandler handler = new ActionHandler();
		find.addKeyListener(new KeyHandler());
		find.addActionListener(new ActionHandler());
		find.getDocument().addDocumentListener(new DocumentHandler());
		box.add(Box.createGlue());
		add(box,BorderLayout.CENTER);

		JPanel panel = new JPanel();
		panel.add(Box.createHorizontalStrut(5));
		panel.add(incremental = new JCheckBox(jEdit.getProperty(
			"view.search.incremental")));
		incremental.addActionListener(new ActionHandler());
		panel.add(ignoreCase = new JCheckBox(jEdit.getProperty(
			"search.ignoreCase")));
		ignoreCase.addActionListener(new ActionHandler());
		panel.add(regexp = new JCheckBox(jEdit.getProperty(
			"search.regexp")));
		regexp.addActionListener(new ActionHandler());
		panel.add(multifile = new JCheckBox());
		multifile.addActionListener(new ActionHandler());
		panel.add(multifileBtn = new JButton(jEdit.getProperty(
			"search.multifile")));
		multifileBtn.addActionListener(new ActionHandler());
		multifileBtn.setMargin(new Insets(1,1,1,1));

		update();

		add(panel,BorderLayout.EAST);
	}

	public HistoryTextField getField()
	{
		return find;
	}

	public void setIncremental(boolean incremental)
	{
		this.incremental.getModel().setSelected(incremental);
		update();
	}

	public void update()
	{
		if(incremental.getModel().isSelected())
		{
			multifile.setSelected(false);
			multifile.setEnabled(false);
			multifileBtn.setEnabled(false);
		}
		else
		{
			ignoreCase.getModel().setSelected(SearchAndReplace.getIgnoreCase());
			regexp.getModel().setSelected(SearchAndReplace.getRegexp());
			multifile.getModel().setSelected(!(SearchAndReplace.getSearchFileSet()
				instanceof CurrentBufferSet));
			multifile.setEnabled(true);
			multifileBtn.setEnabled(true);
		}
	}

	// private members
	private View view;
	private HistoryTextField find;
	private JCheckBox incremental, ignoreCase, regexp, multifile;
	private JButton multifileBtn;

	private void showMultiFileDialog()
	{
		SearchFileSet fs = new MultiFileSearchDialog(
			view,SearchAndReplace.getSearchFileSet())
			.getSearchFileSet();
		if(fs != null)
			SearchAndReplace.setSearchFileSet(fs);
		multifile.getModel().setSelected(!(
			SearchAndReplace.getSearchFileSet()
			instanceof CurrentBufferSet));
	}

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
					new SearchDialog(view,null);
				}
				else if(incremental.getModel().isSelected())
				{
					// on enter, start search from end
					// of current match to find next one
					find.addCurrentToHistory();
					incrementalSearch(view.getTextArea()
						.getSelectionEnd());
				}
				else
				{
					find.addCurrentToHistory();
					find.setText(null);
					SearchAndReplace.setSearchString(text);
					SearchAndReplace.find(view);
					view.focusOnTextArea();
				}
			}
			else if(evt.getSource() == incremental)
			{
				update();
			}
			else if(evt.getSource() == ignoreCase)
			{
				SearchAndReplace.setIgnoreCase(ignoreCase
					.getModel().isSelected());
			}
			else if(evt.getSource() == regexp)
			{
				SearchAndReplace.setRegexp(regexp
					.getModel().isSelected());
			}
			else if(source == multifileBtn)
			{
				showMultiFileDialog();
			}
			else if(source == multifile)
			{
				if(multifile.getModel().isSelected())
					showMultiFileDialog();
				else
					SearchAndReplace.setSearchFileSet(
						new CurrentBufferSet());
			}
		}
	}

	class KeyHandler extends KeyAdapter
	{
		public void keyPressed(KeyEvent evt)
		{
			if(evt.getKeyCode() == KeyEvent.VK_ESCAPE)
			{
				view.focusOnTextArea();
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
			if(incremental.getModel().isSelected())
				incrementalSearch(view.getTextArea()
					.getSelectionStart());
		}

		public void removeUpdate(DocumentEvent evt)
		{
			// on backspace, restart from beginning
			// when we write reverse search, implement real
			// backtracking
			if(incremental.getModel().isSelected())
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
}

/*
 * ActionLog:
 * $Log$
 * Revision 1.5  2000/04/25 03:32:40  sp
 * Even more VFS hacking
 *
 * Revision 1.4  2000/04/08 06:10:51  sp
 * Digit highlighting, search bar bug fix
 *
 * Revision 1.3  2000/04/06 09:28:08  sp
 * Better plugin error reporting, search bar updates
 *
 * Revision 1.2  2000/04/06 02:22:12  sp
 * Incremental search, documentation updates
 *
 * Revision 1.1  2000/04/04 04:53:26  sp
 * added SearchBar.java
 *
 */
