/*
 * LogViewer.java
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
import java.awt.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class LogViewer extends JFrame
{
	public LogViewer(View view)
	{
		super(jEdit.getProperty("log-viewer.title"));

		setIconImage(GUIUtilities.getEditorIcon());

		JTextArea textArea = new JTextArea(24,80);
		textArea.setDocument(Log.getLogDocument());
		//textArea.setEditable(false);

		Font font = view.getTextArea().getPainter().getFont();
		textArea.setFont(font);
		getContentPane().add(BorderLayout.CENTER,new JScrollPane(textArea));

		pack();
		GUIUtilities.loadGeometry(this,"log-viewer");
		show();
	}

	public void dispose()
	{
		GUIUtilities.saveGeometry(this,"log-viewer");
		super.dispose();
	}
}
