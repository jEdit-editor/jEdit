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

public class SearchBar extends JToolBar
{
	public SearchBar(View view)
	{
		this.view = view;

		setFloatable(false);

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

		Insets margin = new Insets(1,1,1,1);

		Box buttons = new Box(BoxLayout.X_AXIS);
		buttons.add(Box.createHorizontalStrut(5));
		buttons.add(incremental = new JCheckBox(jEdit.getProperty(
			"view.search.incremental")));
		incremental.addActionListener(new ActionHandler());
		incremental.setMargin(margin);
		buttons.add(Box.createHorizontalStrut(5));
		buttons.add(ignoreCase = new JCheckBox(jEdit.getProperty(
			"search.ignoreCase")));
		ignoreCase.addActionListener(new ActionHandler());
		ignoreCase.setMargin(margin);
		buttons.add(Box.createHorizontalStrut(5));
		buttons.add(regexp = new JCheckBox(jEdit.getProperty(
			"search.regexp")));
		regexp.addActionListener(new ActionHandler());
		regexp.setMargin(margin);
		buttons.add(Box.createHorizontalStrut(5));
		buttons.add(multifile = new JCheckBox());
		multifile.addActionListener(new ActionHandler());
		multifile.setMargin(margin);
		buttons.add(multifileBtn = new JButton(jEdit.getProperty(
			"search.multifile")));
		multifileBtn.addActionListener(new ActionHandler());
		multifileBtn.setMargin(margin);

		update();

		add(buttons,BorderLayout.EAST);
	}

	public HistoryTextField getField()
	{
		return find;
	}

	public void setIncremental(boolean incremental)
	{
		this.incremental.setSelected(incremental);
		update();
	}

	public void update()
	{
		if(incremental.isSelected())
		{
			multifile.setSelected(false);
			multifile.setEnabled(false);
			multifileBtn.setEnabled(false);
		}
		else
		{
			ignoreCase.setSelected(SearchAndReplace.getIgnoreCase());
			regexp.setSelected(SearchAndReplace.getRegexp());
			multifile.setSelected(!(SearchAndReplace.getSearchFileSet()
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
		multifile.setSelected(!(SearchAndReplace.getSearchFileSet()
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
				else if(incremental.isSelected())
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
					SearchAndReplace.find(view,view);
					view.getEditPane().focusOnTextArea();
				}
			}
			else if(evt.getSource() == incremental)
			{
				update();
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
			else if(source == multifileBtn)
			{
				showMultiFileDialog();
			}
			else if(source == multifile)
			{
				if(multifile.isSelected())
					showMultiFileDialog();
				else
					SearchAndReplace.setSearchFileSet(
						new CurrentBufferSet());
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
			if(incremental.isSelected())
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
			if(incremental.isSelected())
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

/*
 * ActionLog:
 * $Log$
 * Revision 1.13  2000/05/21 03:00:51  sp
 * Code cleanups and bug fixes
 *
 * Revision 1.12  2000/05/13 05:13:31  sp
 * Mode option pane
 *
 * Revision 1.11  2000/05/12 11:07:39  sp
 * Bug fixes, documentation updates
 *
 * Revision 1.10  2000/05/09 10:51:52  sp
 * New status bar, a few other things
 *
 * Revision 1.9  2000/05/07 07:29:02  sp
 * Splitting fixes
 *
 * Revision 1.8  2000/05/07 05:48:30  sp
 * You can now edit several buffers side-by-side in a split view
 *
 * Revision 1.7  2000/05/05 11:08:26  sp
 * Johnny Ryall
 *
 * Revision 1.6  2000/05/04 10:37:04  sp
 * Wasting time
 *
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
