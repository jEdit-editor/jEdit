/*
 * HyperSearch.java - HyperSearch dialog
 * Copyright (C) 1998, 1999, 2000 Slava Pestov
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
			Buffer buffer = bmsg.getBuffer();
			if(bmsg.getWhat() == BufferUpdate.CREATED)
			{
				for(int i = resultModel.getSize() - 1; i >= 0; i--)
				{
					SearchResult result = (SearchResult)resultModel
						.elementAt(i);
					if(buffer.getPath().equals(result.path))
						result.bufferOpened(buffer);
				}
			}
			else if(bmsg.getWhat() == BufferUpdate.CLOSED)
			{
				for(int i = resultModel.getSize() - 1; i >= 0; i--)
				{
					SearchResult result = (SearchResult)resultModel
						.elementAt(i);
					if(buffer == result.buffer)
						result.bufferClosed();
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
				doHyperSearch(buffer,matcher);
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

		Element map = buffer.getDefaultRootElement();
		int lines = map.getElementCount();
		for(int i = 0; i < lines; i++)
		{
			Element lineElement = map.getElement(i);
			int start = lineElement.getStartOffset();
			String lineString = buffer.getText(start,
				lineElement.getEndOffset() - start - 1);
			int[] match = matcher.nextMatch(lineString);
			if(match != null)
			{
				resultModel.addElement(new SearchResult(buffer,i));
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
		String path;
		Buffer buffer;
		int line;
		Position linePos;
		String str; // cached for speed

		SearchResult(Buffer buffer, int line)
		{
			path = buffer.getPath();
			this.line = line;

			if(!buffer.isTemporary())
				bufferOpened(buffer);

			str = buffer.getName() + ":" + (line + 1) + ":"
				+ getLine(buffer,buffer.getDefaultRootElement()
				.getElement(line));
		}

		String getLine(Buffer buffer, Element elem)
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

		public void bufferOpened(Buffer buffer)
		{
			this.buffer = buffer;
			Element map = buffer.getDefaultRootElement();
			Element elem = map.getElement(line);
			if(elem == null)
				elem = map.getElement(map.getElementCount()-1);
			try
			{
				linePos = buffer.createPosition(elem.getStartOffset());
			}
			catch(BadLocationException bl)
			{
				Log.log(Log.ERROR,this,bl);
			}
		}

		public void bufferClosed()
		{
			buffer = null;
			linePos = null;
		}

		public Buffer getBuffer()
		{
			if(buffer == null)
				jEdit.openFile(null,path);
			return buffer;
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
			Buffer buffer = result.getBuffer();
			int pos = result.linePos.getOffset();

			view.setBuffer(buffer);
			view.getTextArea().setCaretPosition(pos);
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.51  2000/04/01 12:21:27  sp
 * mode cache implemented
 *
 * Revision 1.50  2000/03/20 03:42:55  sp
 * Smoother syntax package, opening an already open file will ask if it should be
 * reloaded, maybe some other changes
 *
 * Revision 1.49  2000/02/01 06:12:33  sp
 * Gutter added (still not fully functional)
 *
 * Revision 1.48  2000/01/29 10:12:43  sp
 * BeanShell edit mode, bug fixes
 *
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
 */
