/*
 * HelpViewer.java - HTML Help viewer
 * Copyright (C) 1999, 2000 Slava Pestov
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
import javax.swing.event.*;
import javax.swing.text.html.*;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.gui.EnhancedFrame;
import org.gjt.sp.util.Log;

/**
 * jEdit's HTML viewer. It uses a Swing JEditorPane to display the HTML,
 * and implements a URL history.
 * @author Slava Pestov
 * @version $Id$
 */
public class HelpViewer extends EnhancedFrame
{
	/**
	 * Goes to the specified URL, creating a new help viewer or
	 * reusing an existing one as necessary.
	 * @param url The URL
	 * @since jEdit 2.2final
	 */
	public static void gotoURL(URL url)
	{
		if(helpViewer == null)
			helpViewer = new HelpViewer(url);
		else
		{
			helpViewer.toFront();
			helpViewer.requestFocus();
			helpViewer.gotoURL(url,true);
		}
	}

	/**
	 * Creates a new help viewer for the specified URL.
	 * @param url The URL
	 * @deprecated Use the static gotoURL() method instead
	 */
	public HelpViewer(URL url)
	{
		super(jEdit.getProperty("helpviewer.title"));

		setIconImage(GUIUtilities.getEditorIcon());

		history = new URL[25];

		ActionHandler actionListener = new ActionHandler();

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		toolBar.putClientProperty("JToolBar.isRollover",Boolean.TRUE);

		JLabel label = new JLabel(jEdit.getProperty("helpviewer.url"));
		label.setBorder(new EmptyBorder(0,0,0,12));
		toolBar.add(label);
		Box box = new Box(BoxLayout.Y_AXIS);
		box.add(Box.createGlue());
		urlField = new JTextField();
		Dimension dim = urlField.getPreferredSize();
		dim.width = Integer.MAX_VALUE;
		urlField.setMaximumSize(dim);
		box.add(urlField);
		box.add(Box.createGlue());
		toolBar.add(box);

		toolBar.add(Box.createHorizontalStrut(6));

		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons,BoxLayout.X_AXIS));
		buttons.setBorder(new EmptyBorder(0,12,0,0));
		back = new JButton(GUIUtilities.loadIcon("Back24.gif"));
		back.setToolTipText(jEdit.getProperty("helpviewer.back"));
		back.addActionListener(actionListener);
		back.setRequestFocusEnabled(false);
		toolBar.add(back);
		forward = new JButton(GUIUtilities.loadIcon("Forward24.gif"));
		forward.addActionListener(actionListener);
		forward.setToolTipText(jEdit.getProperty("helpviewer.forward"));
		forward.setRequestFocusEnabled(false);
		toolBar.add(forward);
		back.setPreferredSize(forward.getPreferredSize());

		getContentPane().add(BorderLayout.NORTH,toolBar);

		viewer = new JEditorPane();
		viewer.setEditable(false);
		viewer.setFont(new Font("Monospaced", Font.PLAIN, 12));
		viewer.addHyperlinkListener(new LinkHandler());
		getContentPane().add(BorderLayout.CENTER,new JScrollPane(viewer));

		gotoURL(url,true);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		setSize(600,400);
		GUIUtilities.loadGeometry(this,"helpviewer");

		show();
	}

	/**
	 * Displays the specified URL in the HTML component.
	 * @param url The URL
	 * @param addToHistory Should the URL be added to the back/forward
	 * history?
	 */
	public void gotoURL(URL url, boolean addToHistory)
	{
		// reset default cursor so that the hand cursor doesn't
		// stick around
		viewer.setCursor(Cursor.getDefaultCursor());

		try
		{
			urlField.setText(url.toString());
			viewer.setPage(url);
			if(addToHistory)
			{
				history[historyPos++] = url;
				if(history.length == historyPos)
					System.arraycopy(history,1,history,
						0,history.length);
				history[historyPos] = null;
			}
		}
		catch(IOException io)
		{
			Log.log(Log.ERROR,this,io);
			String[] args = { io.getMessage() };
			GUIUtilities.error(this,"ioerror",args);
		}
	}

	public void ok()
	{
		try
		{
			gotoURL(new URL(urlField.getText()),
				true);
		}
		catch(MalformedURLException mu)
		{
			Log.log(Log.ERROR,this,mu);
			String[] args = { urlField.getText() };
			GUIUtilities.error(HelpViewer.this,
				"badurl",args);
		}
	}

	public void cancel()
	{
		GUIUtilities.saveGeometry(this,"helpviewer");
		dispose();
		helpViewer = null;
	}

	// private members
	private static HelpViewer helpViewer;

	private JButton back;
	private JButton forward;
	private JEditorPane viewer;
	private JTextField urlField;
	private URL[] history;
	private int historyPos;

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(source == back)
			{
				if(historyPos <= 1)
					getToolkit().beep();
				else
				{
					URL url = history[--historyPos - 1];
					gotoURL(url,false);
				}
			}
			else if(source == forward)
			{
				if(history.length - historyPos <= 1)
					getToolkit().beep();
				else
				{
					URL url = history[historyPos];
					if(url == null)
						getToolkit().beep();
					else
					{
						historyPos++;
						gotoURL(url,false);
					}
				}
			}
		}
	}

	class LinkHandler implements HyperlinkListener
	{
		public void hyperlinkUpdate(HyperlinkEvent evt)
		{
			if(evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
			{
				if(evt instanceof HTMLFrameHyperlinkEvent)
				{
					((HTMLDocument)viewer.getDocument())
						.processHTMLFrameHyperlinkEvent(
						(HTMLFrameHyperlinkEvent)evt);
				}
				else
				{
					URL url = evt.getURL();
					if(url != null)
						gotoURL(url,true);
				}
			}
			else if (evt.getEventType() == HyperlinkEvent.EventType.ENTERED) {
				viewer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			}
			else if (evt.getEventType() == HyperlinkEvent.EventType.EXITED) {
				viewer.setCursor(Cursor.getDefaultCursor());
			}
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.28  2000/09/26 10:19:47  sp
 * Bug fixes, spit and polish
 *
 * Revision 1.27  2000/09/23 03:01:10  sp
 * pre7 yayayay
 *
 * Revision 1.26  2000/07/03 03:32:16  sp
 * *** empty log message ***
 *
 * Revision 1.25  2000/06/24 06:24:55  sp
 * work thread bug fixes
 *
 * Revision 1.24  2000/06/24 03:46:48  sp
 * VHDL mode, bug fixing
 *
 * Revision 1.23  2000/06/16 10:11:06  sp
 * Bug fixes ahoy
 *
 */
