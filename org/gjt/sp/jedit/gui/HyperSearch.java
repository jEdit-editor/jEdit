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
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.jedit.search.*;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.util.Log;

/**
 * HyperSearch dialog.
 * @author Slava Pestov
 * @version $Id$
 */
public class HyperSearch extends JDialog implements EBComponent
{
	public HyperSearch(View view, String defaultFind)
	{
		super(view,jEdit.getProperty("hypersearch.title"),false);
		this.view = view;

		fileset = SearchAndReplace.getSearchFileSet();

		resultModel = new DefaultListModel();
		Container content = getContentPane();
		content.setLayout(new BorderLayout());
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JLabel(jEdit.getProperty("hypersearch.find")),
			BorderLayout.WEST);
		find = new HistoryTextField("find");
		find.setText(defaultFind);
		panel.add(find, BorderLayout.CENTER);
		content.add(panel, BorderLayout.NORTH);

		JPanel stretchPanel = new JPanel(new BorderLayout());

		panel = new JPanel();
		ignoreCase = new JCheckBox(jEdit.getProperty(
			"search.ignoreCase"),SearchAndReplace.getIgnoreCase());
		regexp = new JCheckBox(jEdit.getProperty(
			"search.regexp"),SearchAndReplace.getRegexp());
		multifile = new JCheckBox();
		multifile.getModel().setSelected(!(fileset
			instanceof CurrentBufferSet));
		multifileBtn = new JButton(jEdit.getProperty(
			"search.multifile"));
		panel.add(ignoreCase);
		panel.add(regexp);
		panel.add(multifile);
		panel.add(multifileBtn);
		findBtn = new JButton(jEdit.getProperty("hypersearch.findBtn"));
		panel.add(findBtn);
		getRootPane().setDefaultButton(findBtn);
		close = new JButton(jEdit.getProperty("common.close"));
		panel.add(close);
		stretchPanel.add(panel,BorderLayout.NORTH);

		results = new JList();
		results.setVisibleRowCount(10);
		results.addListSelectionListener(new ListHandler());
		stretchPanel.add(new JScrollPane(results), BorderLayout.CENTER);

		content.add(stretchPanel, BorderLayout.CENTER);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		KeyHandler keyListener = new KeyHandler();
		ActionHandler actionListener = new ActionHandler();

		find.addKeyListener(keyListener);
		addKeyListener(keyListener);
		multifile.addActionListener(actionListener);
		multifileBtn.addActionListener(actionListener);
		find.addActionListener(actionListener);
		findBtn.addActionListener(actionListener);
		close.addActionListener(actionListener);

		EditBus.addToBus(this);

		pack();
		GUIUtilities.loadGeometry(this,"hypersearch");

		show();
		find.requestFocus();
	}
	
	public void save()
	{
		find.addCurrentToHistory();
		SearchAndReplace.setSearchString(find.getText());
		SearchAndReplace.setIgnoreCase(ignoreCase.getModel().isSelected());
		SearchAndReplace.setRegexp(regexp.getModel().isSelected());
		SearchAndReplace.setSearchFileSet(fileset);
	}
	
	public void dispose()
	{
		EditBus.removeFromBus(this);
		GUIUtilities.saveGeometry(this,"hypersearch");
		super.dispose();
	}

	public void handleMessage(EBMessage msg)
	{
		if(msg instanceof BufferUpdate)
		{
			BufferUpdate bmsg = (BufferUpdate)msg;
			Buffer buffer = bmsg.getBuffer();
			for(int i = 0; i < resultModel.getSize(); i++)
			{
				SearchResult result = (SearchResult)resultModel
					.elementAt(i);
				if(result.buffer == buffer)
					resultModel.removeElementAt(i);
			}
		}
	}

	// private members
	private View view;
	private SearchFileSet fileset;
	private HistoryTextField find;
	private JCheckBox ignoreCase;
	private JCheckBox regexp;
	private JCheckBox multifile;
	private JButton multifileBtn;
	private JButton findBtn;
	private JButton close;
	private JList results;
	private DefaultListModel resultModel;

	private void doHyperSearch()
	{
		try
		{
			resultModel.removeAllElements();
			SearchMatcher matcher = SearchAndReplace
				.getSearchMatcher();
			if(matcher == null)
			{
				view.getToolkit().beep();
				return;
			}

			SearchFileSet fileset = SearchAndReplace.getSearchFileSet();
			Buffer buffer = fileset.getFirstBuffer(view);

			do
			{
				if(!doHyperSearch(buffer,matcher))
					fileset.doneWithBuffer(buffer);
			}
			while((buffer = fileset.getNextBuffer(view,buffer)) != null);
	
			if(resultModel.isEmpty())
				view.getToolkit().beep();
			results.setModel(resultModel);
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,this,e);
			Object[] args = { e.getMessage() };
			if(args[0] == null)
				args[0] = e.toString();
			GUIUtilities.error(view,"searcherror",args);
		}
	}

	private boolean doHyperSearch(Buffer buffer, SearchMatcher matcher)
		throws Exception
	{
		boolean retVal = false;

		Element map = buffer.getDefaultRootElement();
		int lines = map.getElementCount();
		for(int i = 1; i <= lines; i++)
		{
			Element lineElement = map.getElement(i - 1);
			int start = lineElement.getStartOffset();
			String lineString = buffer.getText(start,
				lineElement.getEndOffset() - start - 1);
			int[] match = matcher.nextMatch(lineString);
			if(match != null)
			{
				resultModel.addElement(
					new SearchResult(buffer,
					buffer.createPosition(start + match[0]),
					buffer.createPosition(start + match[1])));
				retVal = true;
			}
		}

		return retVal;
	}

	private void showMultiFileDialog()
	{
		SearchFileSet fs = new MultiFileSearchDialog(
			view,fileset).getSearchFileSet();
		if(fs != null)
		{
			fileset = fs;
		}
		multifile.getModel().setSelected(!(
			fileset instanceof CurrentBufferSet));
	}

	class SearchResult
	{
		Buffer buffer;
		Position start;
		Position end;
		String str; // cached for speed

		SearchResult(Buffer buffer, Position start, Position end)
		{
			this.buffer = buffer;
			this.start = start;
			this.end = end;
			Element map = buffer.getDefaultRootElement();
			int line = map.getElementIndex(start.getOffset());
			str = buffer.getName() + ":" + (line + 1) + ":"
				+ getLine(map.getElement(line));
		}

		String getLine(Element elem)
		{
			if(elem == null)
				return "";
			try
			{
				return buffer.getText(elem.getStartOffset(),
					elem.getEndOffset() -
					elem.getStartOffset() - 1);
			}
			catch(BadLocationException bl)
			{
				Log.log(Log.ERROR,this,bl);
				return "";
			}
		}

		public String toString()
		{
			return str;
		}
	}

	class ActionHandler implements ActionListener
	{
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
			else if(source == multifileBtn)
			{
				showMultiFileDialog();
			}
			else if(source == multifile)
			{
				if(multifile.getModel().isSelected())
					showMultiFileDialog();
				else
					fileset = new CurrentBufferSet();
			}
		}
	}

	class KeyHandler extends KeyAdapter
	{
		public void keyPressed(KeyEvent evt)
		{
			if(evt.getKeyCode() == KeyEvent.VK_ESCAPE)
				dispose();
		}
	}

	class ListHandler implements ListSelectionListener
	{
		public void valueChanged(ListSelectionEvent evt)
		{
			if(results.isSelectionEmpty() || evt.getValueIsAdjusting())
				return;

			SearchResult result = (SearchResult)results.getSelectedValue();
			Buffer buffer = result.buffer;
			int start = result.start.getOffset();
			int end = result.end.getOffset();

			view.setBuffer(buffer);
			view.getTextArea().select(start,end);
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.41  1999/11/19 08:54:52  sp
 * EditBus integrated into the core, event system gone, bug fixes
 *
 * Revision 1.40  1999/11/07 06:51:43  sp
 * Check box menu items supported
 *
 * Revision 1.39  1999/10/31 07:15:34  sp
 * New logging API, splash screen updates, bug fixes
 *
 * Revision 1.38  1999/10/11 07:14:22  sp
 * doneWithBuffer()
 *
 * Revision 1.37  1999/10/02 01:12:36  sp
 * Search and replace updates (doesn't work yet), some actions moved to TextTools
 *
 * Revision 1.36  1999/07/05 04:38:39  sp
 * Massive batch of changes... bug fixes, also new text component is in place.
 * Have fun
 *
 * Revision 1.35  1999/06/09 07:28:10  sp
 * Multifile search and replace tweaks, removed console.html
 *
 * Revision 1.34  1999/06/05 07:17:08  sp
 * Cascading makefiles, HyperSearch tweak, doc updates
 *
 * Revision 1.33  1999/06/03 08:24:13  sp
 * Fixing broken CVS
 *
 * Revision 1.34  1999/05/31 08:11:10  sp
 * Syntax coloring updates, expand abbrev bug fix
 *
 * Revision 1.33  1999/05/31 04:38:51  sp
 * Syntax optimizations, HyperSearch for Selection added (Mike Dillon)
 *
 * Revision 1.32  1999/05/30 01:28:43  sp
 * Minor search and replace updates
 *
 * Revision 1.31  1999/05/29 08:06:56  sp
 * Search and replace overhaul
 *
 * Revision 1.30  1999/05/09 03:50:17  sp
 * HistoryTextField is now a text field again
 *
 * Revision 1.29  1999/04/23 07:35:11  sp
 * History engine reworking (shared history models, history saved to
 * .jedit-history)
 *
 * Revision 1.28  1999/04/19 05:44:34  sp
 * GUI updates
 *
 * Revision 1.27  1999/04/08 04:44:51  sp
 * New _setBuffer method in View class, new addTab method in Console class
 *
 * Revision 1.26  1999/04/02 03:21:09  sp
 * Added manifest file, common strings such as OK, etc are no longer duplicated
 * many times in jedit_gui.props
 *
 */
