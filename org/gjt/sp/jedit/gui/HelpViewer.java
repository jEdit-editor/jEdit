/*
 * HelpViewer.java - HTML Help viewer
 * Copyright (C) 1999 Slava Pestov
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
import javax.swing.text.html.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class HelpViewer extends JFrame
{
	public HelpViewer(URL url)
	{
		super(jEdit.getProperty("helpviewer.title"));
		
		history = new URL[25];

		ActionHandler actionListener = new ActionHandler();

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(BorderLayout.WEST,new JLabel(
			jEdit.getProperty("helpviewer.url")));
		urlField = new JTextField();
		urlField.addKeyListener(new KeyHandler());
		panel.add(urlField);
		
		getContentPane().add(BorderLayout.NORTH,panel);

		viewer = new JEditorPane();
		viewer.setEditable(false);
		viewer.addHyperlinkListener(new LinkHandler());
		getContentPane().add(BorderLayout.CENTER,
			new JScrollPane(viewer));

		panel = new JPanel();

		back = new JButton(jEdit.getProperty("helpviewer.back"));
		back.setIcon(new ImageIcon(getClass().getResource(
			"/org/gjt/sp/jedit/toolbar/Left.gif")));
		back.addActionListener(actionListener);
		panel.add(back);
		forward = new JButton(jEdit.getProperty("helpviewer.forward"));
		forward.setIcon(new ImageIcon(getClass().getResource(
			"/org/gjt/sp/jedit/toolbar/Right.gif")));
		forward.addActionListener(actionListener);
		panel.add(forward);

		home = new JButton(jEdit.getProperty("helpviewer.home"));
		home.setIcon(new ImageIcon(getClass().getResource(
			"/org/gjt/sp/jedit/toolbar/Help.gif")));
		home.addActionListener(actionListener);
		panel.add(home);
		
		getContentPane().add(BorderLayout.SOUTH,panel);

		gotoURL(url,true);
		
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		setSize(600,400);
		GUIUtilities.loadGeometry(this,"helpviewer");

		show();
	}

	public void dispose()
	{
		GUIUtilities.saveGeometry(this,"helpviewer");
		super.dispose();
	}

	// private members
	private JButton back;
	private JButton forward;
	private JButton home;
	private JEditorPane viewer;
	private JTextField urlField;
	private URL[] history;
	private int historyPos;

	private void gotoURL(URL url, boolean addToHistory)
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
			String[] args = { io.getMessage() };
			GUIUtilities.error(this,"ioerror",args);
		}
	}
	
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
			else if(source == home)
			{
				try
				{
					gotoURL(new URL("jeditdocs:"),true);
				}
				catch(MalformedURLException mu)
				{
					Log.log(Log.ERROR,this,mu);
				}
			}
		}
	}

	class KeyHandler extends KeyAdapter
	{
		public void keyPressed(KeyEvent evt)
		{
			if(evt.getKeyCode() == KeyEvent.VK_ENTER)
			{
				try
				{
					gotoURL(new URL(urlField.getText()),
						true);
				}
				catch(MalformedURLException mf)
				{
					String[] args = { urlField.getText() };
					GUIUtilities.error(HelpViewer.this,
						"badurl",args);
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
