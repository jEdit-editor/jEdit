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
public class HyperSearch extends EnhancedDialog implements EBComponent
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

		ActionHandler actionListener = new ActionHandler();

		multifile.addActionListener(actionListener);
		multifileBtn.addActionListener(actionListener);
		find.addActionListener(actionListener);
		findBtn.addActionListener(actionListener);
		close.addActionListener(actionListener);

		if(defaultFind != null)
		{
			find.setText(defaultFind);
			save();
		}

		EditBus.addToBus(this);

		pack();
		GUIUtilities.loadGeometry(this,"hypersearch");

		show();

		if(defaultFind != null)
			doHyperSearch();

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

	// EnhancedDialog implementation
	public void ok()
	{
		save();
		doHyperSearch();
	}

	public void cancel()
	{
		EditBus.removeFromBus(this);
		GUIUtilities.saveGeometry(this,"hypersearch");
		dispose();
	}
	// end EnhancedDialog implementation

	public void handleMessage(EBMessage msg)
	{
		if(msg instanceof BufferUpdate)
		{
			BufferUpdate bmsg = (BufferUpdate)msg;
			if(bmsg.getWhat() == BufferUpdate.CLOSED)
			{
				Buffer buffer = bmsg.getBuffer();
				for(int i = resultModel.getSize() - 1; i >= 0; i--)
				{
					SearchResult result = (SearchResult)resultModel
						.elementAt(i);
					if(result.buffer == buffer)
						resultModel.removeElementAt(i);
				}
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
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
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
				if(doHyperSearch(buffer,matcher))
					fileset.matchFound(buffer);
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
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	private boolean doHyperSearch(Buffer buffer, SearchMatcher matcher)
		throws Exception
	{
		boolean retVal = false;

		buffer.loadIfNecessary(null);

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
				cancel();
			else if(source == findBtn || source == find)
				ok();
			else if(source == multifileBtn)
				showMultiFileDialog();
			else if(source == multifile)
			{
				if(multifile.getModel().isSelected())
					showMultiFileDialog();
				else
					fileset = new CurrentBufferSet();
			}
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
 * Revision 1.47  1999/12/05 03:01:05  sp
 * Perl token marker bug fix, file loading is deferred, style option pane fix
 *
 * Revision 1.46  1999/11/28 00:33:07  sp
 * Faster directory search, actions slimmed down, faster exit/close-all
 *
 * Revision 1.45  1999/11/26 07:37:11  sp
 * Escape/enter handling code moved to common superclass, bug fixes
 *
 * Revision 1.44  1999/11/23 08:03:21  sp
 * Miscallaeneous stuff
 *
 * Revision 1.43  1999/11/21 01:20:31  sp
 * Bug fixes, EditBus updates, fixed some warnings generated by jikes +P
 *
 * Revision 1.42  1999/11/20 02:34:22  sp
 * more pre6 stuffs
 *
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
 * matchFound()
 *
 * Revision 1.37  1999/10/02 01:12:36  sp
 * Search and replace updates (doesn't work yet), some actions moved to TextTools
 *
 * Revision 1.36  1999/07/05 04:38:39  sp
 * Massive batch of changes... bug fixes, also new text component is in place.
 * Have fun
 *
 */
