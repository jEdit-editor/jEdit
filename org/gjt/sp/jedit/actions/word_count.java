/*
 * word_count.java - Action
 * Copyright (C) 1998 Slava Pestov
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

import javax.swing.text.BadLocationException;
import java.awt.event.ActionEvent;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class word_count extends EditAction
{
	public word_count()
	{
		super("word-count");
	}

	public void actionPerformed(ActionEvent evt)
	{
		View view = getView(evt);
		Buffer buffer = view.getBuffer();
		String selection = view.getTextArea().getSelectedText();
		if(selection != null)
		{
			doWordCount(view,selection);
			return;
		}
		try
		{
			doWordCount(view,buffer.getText(0,buffer.getLength()));
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
	}

	private void doWordCount(View view, String text)
	{
		char[] chars = text.toCharArray();
		int characters = chars.length;
		int words;
		if(characters == 0)
			words = 0;
		else
			words = 1;
		int lines = 1;
		boolean word = false;
		for(int i = 0; i < chars.length; i++)
		{
			switch(chars[i])
			{
			case '\r': case '\n':
				lines++;
			case ' ': case '\t':
				if(word)
				{
					words++;
					word = false;
				}
				break;
			default:
				word = true;
				break;
			}
		}
		Object[] args = { new Integer(characters), new Integer(words),
			new Integer(lines) };
		GUIUtilities.message(view,"wordcount",args);
	}
}
