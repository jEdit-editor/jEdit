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
import javax.swing.*;
import org.gjt.sp.jedit.search.*;
import org.gjt.sp.jedit.*;

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
	}

	public void update()
	{
		if(incremental.getModel().isSelected())
		{
			ignoreCase.setSelected(false);
			regexp.setSelected(false);
			regexp.setEnabled(false);
			multifile.setSelected(false);
			multifile.setEnabled(false);
			multifileBtn.setEnabled(false);
		}
		else
		{
			ignoreCase.getModel().setSelected(SearchAndReplace.getIgnoreCase());
			regexp.getModel().setSelected(SearchAndReplace.getRegexp());
			regexp.setEnabled(true);
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

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(evt.getSource() == find)
			{
				if(incremental.getModel().isSelected())
				{
					System.err.println("Incremental search not implemented yet");
				}
				else
				{
					String text = find.getText();
					if(text == null || text.length() == 0)
					{
						new SearchDialog(view,null);
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
}

/*
 * ActionLog:
 * $Log$
 * Revision 1.1  2000/04/04 04:53:26  sp
 * added SearchBar.java
 *
 */
