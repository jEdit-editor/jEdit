/*
 * LogViewer.java
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
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class LogViewer extends JFrame
{
	public LogViewer(View view)
	{
		super(jEdit.getProperty("log-viewer.title"));

		setIconImage(GUIUtilities.getEditorIcon());

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
		panel.setBorder(new EmptyBorder(0,0,12,0));
		panel.add(Box.createGlue());

		clear = new JButton(jEdit.getProperty("log-viewer.clear"));
		clear.addActionListener(new ActionHandler());
		panel.add(clear);
		panel.add(Box.createHorizontalStrut(6));

		save = new JButton(jEdit.getProperty("log-viewer.save"));
		save.addActionListener(new ActionHandler());
		panel.add(save);
		panel.add(Box.createGlue());

		content.add(BorderLayout.NORTH,panel);

		JTextArea textArea = new JTextArea(24,80);
		textArea.setDocument(Log.getLogDocument());
		//textArea.setEditable(false);

		Font font = view.getTextArea().getPainter().getFont();
		textArea.setFont(font);
		content.add(BorderLayout.CENTER,new JScrollPane(textArea));

		pack();
		GUIUtilities.loadGeometry(this,"log-viewer");
		show();
	}

	public void dispose()
	{
		GUIUtilities.saveGeometry(this,"log-viewer");
		super.dispose();
	}

	// private members
	private JButton clear, save;

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(evt.getSource() == clear)
			{
				Log.clearLog();
			}
			else
			{
				String path = GUIUtilities.showFileDialog(null,
					MiscUtilities.constructPath(null,"jedit.log"),
					JFileChooser.SAVE_DIALOG);

				if(path != null)
				{
					try
					{
						Log.saveLog(path);
					}
					catch(IOException io)
					{
						Log.log(Log.ERROR,this,io);
						String[] args = { io.getMessage() };
						GUIUtilities.error(LogViewer.this,"ioerror",args);
					}
				}
			}
		}
	}
}
