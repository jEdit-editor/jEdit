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
import javax.swing.tree.*;
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
	public HyperSearch(View view)
	{
		super(jEdit.getProperty("hypersearch.title"));
		this.view = view;

		Container content = getContentPane();
		content.setLayout(new BorderLayout());

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(new EmptyBorder(12,12,12,12));

		find = new HistoryTextField("find");

		JPanel fieldPanel = new JPanel(new BorderLayout());
		fieldPanel.setBorder(new EmptyBorder(0,0,6,0));
		JLabel label = new JLabel(jEdit.getProperty("hypersearch.find"));
		label.setBorder(new EmptyBorder(0,0,2,0));
		label.setDisplayedMnemonic(jEdit.getProperty("search.find.mnemonic")
			.charAt(0));
		label.setLabelFor(find);
		fieldPanel.add(label,BorderLayout.NORTH);
		fieldPanel.add(find, BorderLayout.CENTER);
		panel.add(fieldPanel, BorderLayout.NORTH);

		Box box = new Box(BoxLayout.X_AXIS);
		ignoreCase = new JCheckBox(jEdit.getProperty(
			"search.ignoreCase"),SearchAndReplace.getIgnoreCase());
		ignoreCase.setMnemonic(jEdit.getProperty("search.ignoreCase"
			+ ".mnemonic").charAt(0));
		regexp = new JCheckBox(jEdit.getProperty(
			"search.regexp"),SearchAndReplace.getRegexp());
		regexp.setMnemonic(jEdit.getProperty("search.regexp.mnemonic")
			.charAt(0));
		multifile = new JCheckBox();
		multifile.setSelected(!(fileset instanceof CurrentBufferSet));
		multifileBtn = new JButton(jEdit.getProperty(
			"search.multifile"));
		multifileBtn.setMnemonic(jEdit.getProperty("search.multifile"
			+ ".mnemonic").charAt(0));
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
		Dimension size = status.getPreferredSize();
		size.width = Math.max(size.width,100);
		status.setPreferredSize(size);
		status.setMinimumSize(size);
		panel.add(box, BorderLayout.CENTER);

		content.add(panel, BorderLayout.NORTH);

		resultTreeRoot = new DefaultMutableTreeNode();
		resultTreeModel = new DefaultTreeModel(resultTreeRoot);
		resultTree = new JTree(resultTreeModel);
		resultTree.setCellRenderer(new ResultCellRenderer());
		resultTree.setVisibleRowCount(16);
		resultTree.setRootVisible(false);
		resultTree.setShowsRootHandles(true);
		resultTree.putClientProperty("JTree.lineStyle", "Angled");
		resultTree.setEditable(false);

		resultTree.addMouseListener(new MouseHandler());
		content.add(new JScrollPane(resultTree), BorderLayout.CENTER);

		ActionHandler actionListener = new ActionHandler();

		multifile.addActionListener(actionListener);
		multifileBtn.addActionListener(actionListener);
		find.addActionListener(actionListener);
		start.addActionListener(actionListener);
		stop.addActionListener(actionListener);

		EditBus.addToBus(this);

		setIconImage(GUIUtilities.getEditorIcon());

		GUIUtilities.requestFocus(this,find);

		pack();
		GUIUtilities.loadGeometry(this,"hypersearch");
	}

	public void save()
	{
		find.addCurrentToHistory();
		SearchAndReplace.setSearchString(find.getText());
		SearchAndReplace.setIgnoreCase(ignoreCase.isSelected());
		SearchAndReplace.setRegexp(regexp.isSelected());
		SearchAndReplace.setSearchFileSet(fileset);
	}

	public void setSearchString(final String search)
	{
		find.setText(search);
		resultTreeRoot.removeAllChildren();
		resultTreeModel.nodeChanged(resultTreeRoot);

		if(!isVisible())
		{
			fileset = SearchAndReplace.getSearchFileSet();

			ignoreCase.setSelected(SearchAndReplace.getIgnoreCase());
			regexp.setSelected(SearchAndReplace.getRegexp());
			multifile.setSelected(!(fileset instanceof CurrentBufferSet));

			setVisible(true);
		}

		System.err.println(fileset);
		updateStatus();

		toFront();
		requestFocus();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				find.requestFocus();
				if(search != null)
					ok();
			}
		});
	}

	// EnhancedDialog implementation
	public void ok()
	{
		if(thread != null)
			return;

		save();

		SearchMatcher matcher = SearchAndReplace.getSearchMatcher();
		if(matcher == null)
		{
			view.getToolkit().beep();
			return;
		}

		resultTreeRoot.removeAllChildren();
		resultTreeModel.reload(resultTreeRoot);

		thread = new HyperSearchThread(matcher);
		updateEnabled();
		thread.start();
	}

	public void cancel()
	{
		GUIUtilities.saveGeometry(this,"hypersearch");
		save();

		if(thread != null)
		{
			thread.stop();
			thread = null;
		}
		setVisible(false);
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
				for(int i = resultTreeRoot.getChildCount() - 1; i >= 0; i--)
				{
					DefaultMutableTreeNode bufferNode = (DefaultMutableTreeNode)
						resultTreeRoot.getChildAt(i);

					for(int j = bufferNode.getChildCount() - 1;
						j >= 0; j--)
					{
						SearchResult result = (SearchResult)
							((DefaultMutableTreeNode)bufferNode
							.getChildAt(j)).getUserObject();
						if(buffer.getPath().equals(result.path))
							result.bufferOpened(buffer);
					}
				}
			}
			else if(bmsg.getWhat() == BufferUpdate.CLOSED)
			{
				for(int i = resultTreeRoot.getChildCount() - 1; i >= 0; i--)
				{
					DefaultMutableTreeNode bufferNode = (DefaultMutableTreeNode)
						resultTreeRoot.getChildAt(i);

					for(int j = bufferNode.getChildCount() - 1;
						j >= 0; j--)
					{
						SearchResult result = (SearchResult)
							((DefaultMutableTreeNode)bufferNode
							.getChildAt(j)).getUserObject();
						if(buffer.getPath().equals(result.path))
							result.bufferClosed();
					}
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
				{
					EditBus.removeFromBus(this);
					dispose();
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
	private JButton start;
	private JButton stop;
	private JLabel status;
	private JTree resultTree;
	private DefaultMutableTreeNode resultTreeRoot;
	private DefaultTreeModel resultTreeModel;

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
		status.setText(current + " / " + fileset.getBufferCount());
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

			str = (line + 1) + ": " + getLine(buffer,
				buffer.getDefaultRootElement()
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
			TreePath path = resultTree.getPathForLocation(
				evt.getX(),evt.getY());
			if(path == null)
				return;

			Object value = ((DefaultMutableTreeNode)path
				.getLastPathComponent()).getUserObject();

			if(value instanceof String)
			{
				Buffer buffer = jEdit.openFile(view,(String)value);
				if(buffer == null)
					return;

				view.setBuffer(buffer);
				view.toFront();
				view.requestFocus();
			}
			else
			{
				final SearchResult result = (SearchResult)value;
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
	}

	class ResultCellRenderer extends DefaultTreeCellRenderer
	{
		public Component getTreeCellRendererComponent(JTree tree,
			Object value, boolean sel, boolean expanded,
			boolean leaf, int row, boolean hasFocus)
		{
			super.getTreeCellRendererComponent(tree,value,sel,
				expanded,leaf,row,hasFocus);

			setIcon(null);
			Font font = UIManager.getFont("Label.font");
			if(value instanceof String)
			{
				font = new Font(font.getFamily(),Font.BOLD,
					font.getSize());
			}
			else
			{
				font = new Font(font.getFamily(),Font.PLAIN,
					font.getSize());
			}
			ResultCellRenderer.this.setFont(font);

			return this;
		}
	}

	class HyperSearchThread extends Thread
	{
		SearchMatcher matcher;

		HyperSearchThread(SearchMatcher matcher)
		{
			super("jEdit HyperSearch thread");
			this.matcher = matcher;
		}

		public void run()
		{
			try
			{
				current = 0;

				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

				SearchFileSet fileset = SearchAndReplace.getSearchFileSet();
				Buffer buffer = fileset.getFirstBuffer(view);

				boolean retVal = false;

				do
				{
					// Wait for buffer to finish loading
					if(buffer.isPerformingIO())
						VFSManager.waitForRequests();

					retVal |= doHyperSearch(buffer,matcher);

					current++;
					updateStatus();
				}
				while((buffer = fileset.getNextBuffer(view,buffer)) != null);

				if(!retVal)
				{
					view.getToolkit().beep();
					return;
				}

				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						if(resultTreeRoot.getChildCount() == 1)
						{
							resultTree.expandPath(new TreePath(
								((DefaultMutableTreeNode)
								resultTreeRoot.getChildAt(0))
								.getPath()));
						}
					}
				});
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
			final DefaultMutableTreeNode bufferNode = new DefaultMutableTreeNode(
				buffer.getPath());

			try
			{
				buffer.readLock();

				Element map = buffer.getDefaultRootElement();
				Segment text = new Segment();
				int offset = 0;
				int line = -1;
loop:				for(;;)
				{
					buffer.getText(offset,buffer.getLength()
						- offset,text);
					int[] match = matcher.nextMatch(text);
					if(match == null)
						break loop;

					offset += match[1];

					int newLine = map.getElementIndex(offset);
					if(line == newLine)
					{
						// already had a result on this
						// line, skip
						continue loop;
					}

					line = newLine;

					bufferNode.insert(new DefaultMutableTreeNode(
						new SearchResult(buffer,line),false),
						bufferNode.getChildCount());
				}
			}
			finally
			{
				buffer.readUnlock();
			}

			if(bufferNode.getChildCount() == 0)
				return false;

			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					resultTreeRoot.insert(bufferNode,
						resultTreeRoot.getChildCount());
					resultTreeModel.reload(resultTreeRoot);
				}
			});

			return true;
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.72  2000/11/19 07:51:25  sp
 * Documentation updates, bug fixes
 *
 * Revision 1.71  2000/11/07 10:08:32  sp
 * Options dialog improvements, documentation changes, bug fixes
 *
 * Revision 1.70  2000/11/05 00:44:14  sp
 * Improved HyperSearch, improved horizontal scroll, other stuff
 *
 * Revision 1.69  2000/11/02 09:19:33  sp
 * more features
 *
 * Revision 1.68  2000/09/23 03:01:10  sp
 * pre7 yayayay
 *
 * Revision 1.67  2000/08/29 07:47:12  sp
 * Improved complete word, type-select in VFS browser, bug fixes
 *
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
 */
