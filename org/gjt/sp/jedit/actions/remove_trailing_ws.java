/*
 * remove_trailing_ws.java - jEdit action to trim trailing whitespace
 * Copyright (C) 1999, 2000 mike dillon
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

package org.gjt.sp.jedit.actions;

import java.awt.event.ActionEvent;

import javax.swing.text.Segment;
import javax.swing.text.BadLocationException;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditAction;
import org.gjt.sp.jedit.View;

import org.gjt.sp.jedit.textarea.JEditTextArea;

import org.gjt.sp.util.Log;

public class remove_trailing_ws extends EditAction
{
	public void actionPerformed(ActionEvent evt)
	{
		View v = getView(evt);
		Buffer b = v.getBuffer();
		JEditTextArea ta = v.getTextArea();

		int first, last;

		if (ta.getSelectionStart() != ta.getSelectionEnd())
		{
			first = ta.getSelectionStartLine();
			last = ta.getSelectionEndLine();
		}
		else
		{
			first = 0;
			last = ta.getLineCount() - 1;
		}

		// lazy initiation of compound edit
		boolean editStarted = false;

		Segment seg = new Segment();

		int line, pos, lineStart, lineEnd, tail;

		for (line = first; line <= last; line++)
		{
			ta.getLineText(line, seg);

			// blank line
			if (seg.count == 0) continue;

			lineStart = seg.offset;
			lineEnd = seg.offset + seg.count - 1;

			for (pos = lineEnd; pos >= lineStart; pos--)
			{
				if (!Character.isWhitespace(seg.array[pos]))
				{
					break;
				}
			}

			tail = lineEnd - pos;

			// no whitespace
			if (tail == 0) continue;

			try
			{
				if (!editStarted)
				{
					b.beginCompoundEdit();
					editStarted = true;
				}

				b.remove(ta.getLineEndOffset(line) - 1 - tail,
					tail);
			}
			catch (BadLocationException ble)
			{
				Log.log(Log.ERROR, this, ble);
			}
		}

		if (editStarted) b.endCompoundEdit();
	}
}
