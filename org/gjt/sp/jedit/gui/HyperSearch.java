/*
 * HyperSearch.java - HyperSearch window
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
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.search.*;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.util.Log;

/**
 * HyperSearch dialog.
 * @author Slava Pestov
 * @version $Id$
 */
public class HyperSearch extends EnhancedFrame implements EBComponent
{
	public HyperSearch(View view, String defaultFind)
	{
		super(jEdit.getProperty("hypersearch.title"));
		this.view = view;

		fileset = SearchAndReplace.getSearchFileSet();

		resultModel = new DefaultListModel();
		Container content = getContentPane();
		content.setLayout(new BorderLayout());

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(new EmptyBorder(12,12,12,12));

		JPanel fieldPanel = new JPanel(new BorderLayout());
		fieldPanel.setBorder(new EmptyBorder(0,0,6,0));
		JLabel label = new JLabel(jEdit.getProperty("hypersearch.find"));
		label.setBorder(new EmptyBorder(0,0,2,0));
		fieldPanel.add(label,BorderLayout.NORTH);
		find = new HistoryTextField("find");
		fieldPanel.add(find, BorderLayout.CENTER);
		panel.add(fieldPanel, BorderLayout.NORTH);

		Box box = new Box(BoxLayout.X_AXIS);
		ignoreCase = new JCheckBox(jEdit.getProperty(
			"search.ignoreCase"),SearchAndReplace.getIgnoreCase());
		regexp = new JCheckBox(jEdit.getProperty(
			"search.regexp"),SearchAndReplace.getRegexp());
		multifile = new JCheckBox();
		multifile.setSelected(!(fileset instanceof CurrentBufferSet));
		multifileBtn = new JButton(jEdit.getProperty(
			"search.multifile"));
		box.add(ignoreCase);
		box.add(Box.createHorizontalStrut(3));
		box.add(regexp);
		box.add(Box.createHorizontalStrut(3));
		box.add(multifile);
		box.add(multifileBtn);
		box.add(Box.createHorizontalStrut(6));
		start = new JButton(jEdit.getProperty("hypersearch.start"));
		box.add(start);
		box.add(Box.createHorizontalStrut(6));
		stop = new JButton(jEdit.getProperty("hypersearch.stop"));
		box.add(stop);
		box.add(Box.createHorizontalStrut(6));
		status = new JLabel();
		box.add(status);
		getRootPane().setDefaultButton(start);
		updateEnabled();
		updateStatus();
		Dimension size = status.getPreferredSize();
		size.width = Math.max(size.width,100);
		status.setPreferredSize(size);
		status.setMinimumSize(size);
		panel.add(box, BorderLayout.CENTER);

		content.add(panel, BorderLayout.NORTH);

		results = new JList(resultModel);
		results.setVisibleRowCount(16);
		results.addMouseListener(new MouseHandler());
		content.add(new JScrollPane(results), BorderLayout.CENTER);

		ActionHandler actionListener = new ActionHandler();

		multifile.addActionListener(actionListener);
		multifileBtn.addActionListener(actionListener);
		find.addActionListener(actionListener);
		start.addActionListener(actionListener);
		stop.addActionListener(actionListener);

		if(defaultFind != null)
			find.setText(defaultFind);

		EditBus.addToBus(this);

		setIconImage(GUIUtilities.getEditorIcon());

		GUIUtilities.requestFocus(this,find);

		pack();
		GUIUtilities.loadGeometry(this,"hypersearch");
		show();

		if(defaultFind != null)
			ok();
	}

	public void save()
	{
		find.addCurrentToHistory();
		SearchAndReplace.setSearchString(find.getText());
		SearchAndReplace.setIgnoreCase(ignoreCase.isSelected());
		SearchAndReplace.setRegexp(regexp.isSelected());
		SearchAndReplace.setSearchFileSet(fileset);
	}

	public void dispose()
	{
		GUIUtilities.saveGeometry(this,"hypersearch");

		if(thread != null)
		{
			thread.stop();
			thread = null;
		}
		super.dispose();
	}

	// EnhancedDialog implementation
	public void ok()
	{
		if(thread != null)
			return;

		save();
		thread = new HyperSearchThread();
		updateEnabled();
	}

	public void cancel()
	{
		if(thread != null)
			thread.stop();

		EditBus.removeFromBus(this);
		dispose();
	}
	// end EnhancedDialog implementation

	public void handleMessage(EBMessage msg)
	{
		if(msg instanceof BufferUpdate)
		{
			BufferUpdate bmsg = (BufferUpdate)msg;
			Buffer buffer = bmsg.getBuffer();
			if(bmsg.getWhat() == BufferUpdate.LOADED)
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
		else if(msg instanceof ViewUpdate)
		{
			ViewUpdate vmsg = (ViewUpdate)msg;
			View view = vmsg.getView();
			if(vmsg.getWhat() == ViewUpdate.CLOSED)
			{
				if(this.view == view)
					this.view = jEdit.getFirstView();
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
	private JButton start;
	private JButton stop;
	private JLabel status;
	private JList results;
	private DefaultListModel resultModel;
	private HyperSearchThread thread;
	private int current;

	private void updateEnabled()
	{
		find.setEnabled(thread == null);
		ignoreCase.setEnabled(thread == null);
		regexp.setEnabled(thread == null);
		multifile.setEnabled(thread == null);
		multifileBtn.setEnabled(thread == null);
		start.setEnabled(thread == null);
		stop.setEnabled(thread != null);
	}

	private void updateStatus()
	{
		Object[] args = { new Integer(current), new Integer(fileset.getBufferCount()) };
		status.setText(jEdit.getProperty("hypersearch.status",args));
	}

	private void showMultiFileDialog()
	{
		SearchFileSet fs = new MultiFileSearchDialog(
			view,fileset).getSearchFileSet();
		if(fs != null)
			fileset = fs;
		multifile.setSelected(!(fileset instanceof CurrentBufferSet));
		updateStatus();
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
				buffer = jEdit.openFile(null,path);
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
			if(source == start || source == find)
				ok();
			else if(source == stop)
				thread.stop();
			else if(source == multifileBtn)
				showMultiFileDialog();
			else if(source == multifile)
			{
				if(multifile.isSelected())
					showMultiFileDialog();
				else
					fileset = new CurrentBufferSet();
			}
		}
	}

	class MouseHandler extends MouseAdapter
	{
		public void mouseClicked(MouseEvent evt)
		{
			int index = results.locationToIndex(evt.getPoint());
			if(index == -1)
				return;

			final SearchResult result = (SearchResult)resultModel
				.getElementAt(index);
			final Buffer buffer = result.getBuffer();

			if(buffer == null)
				return;

			VFSManager.runInAWTThread(new Runnable()
			{
				public void run()
				{
					int pos = result.linePos.getOffset();
					view.setBuffer(buffer);
					view.getTextArea().setCaretPosition(pos);
					view.toFront();
					view.requestFocus();
				}
			});
		}
	}

	class HyperSearchThread extends Thread
	{
		HyperSearchThread()
		{
			super("jEdit HyperSearch thread");
			setPriority(4);
			start();
		}

		public void run()
		{
			try
			{
				current = 0;

				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

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
					// Wait for buffer to finish loading
					if(!buffer.isLoaded())
						VFSManager.waitForRequests();

					doHyperSearch(buffer,matcher);

					current++;
					updateStatus();
				}
				while((buffer = fileset.getNextBuffer(view,buffer)) != null);

				if(resultModel.isEmpty())
					view.getToolkit().beep();
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,this,e);
				Object[] args = { e.getMessage() };
				if(args[0] == null)
					args[0] = e.toString();
				VFSManager.error(view,"searcherror",args);
			}
			finally
			{
				thread = null;
				updateEnabled();
				updateStatus();

				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		}

		private boolean doHyperSearch(Buffer buffer, SearchMatcher matcher)
			throws Exception
		{
			boolean retVal = false;

			try
			{
				buffer.readLock();
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
			}
			finally
			{
				buffer.readUnlock();
			}

			return retVal;
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.66  2000/08/16 08:47:19  sp
 * Stuff
 *
 * Revision 1.65  2000/08/11 12:13:14  sp
 * Preparing for 2.6pre2 release
 *
 * Revision 1.64  2000/06/16 10:11:06  sp
 * Bug fixes ahoy
 *
 * Revision 1.63  2000/06/03 07:28:26  sp
 * User interface updates, bug fixes
 *
 * Revision 1.62  2000/05/21 06:06:43  sp
 * Documentation updates, shell script mode bug fix, HyperSearch is now a frame
 *
 * Revision 1.61  2000/05/21 03:00:51  sp
 * Code cleanups and bug fixes
 *
 * Revision 1.60  2000/05/20 07:02:04  sp
 * Documentation updates, tool bar editor finished, a few other enhancements
 *
 * Revision 1.59  2000/05/14 10:55:21  sp
 * Tool bar editor started, improved view registers dialog box
 *
 * Revision 1.58  2000/05/06 05:53:46  sp
 * HyperSearch bug fix
 *
 * Revision 1.57  2000/04/29 09:17:07  sp
 * VFS updates, various fixes
 *
 * Revision 1.56  2000/04/28 09:29:12  sp
 * Key binding handling improved, VFS updates, some other stuff
 *
 * Revision 1.55  2000/04/27 08:32:57  sp
 * VFS fixes, read only fixes, macros can prompt user for input, improved
 * backup directory feature
 *
 * Revision 1.54  2000/04/25 03:32:40  sp
 * Even more VFS hacking
 *
 * Revision 1.53  2000/04/24 11:00:23  sp
 * More VFS hacking
 *
 * Revision 1.52  2000/04/03 10:22:24  sp
 * Search bar
 *
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
 */
