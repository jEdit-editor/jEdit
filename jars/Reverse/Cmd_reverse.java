/*
 * Cmd_reverse.java - Simple plugin
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

import java.util.Hashtable;

public class Cmd_reverse implements Command
{
	public void exec(Buffer buffer, View view, String arg, Hashtable args)
	{
		SyntaxTextArea textArea = view.getTextArea();
		String selection = textArea.getSelectedText();
		if(selection != null)
			textArea.replaceSelection(new StringBuffer(selection)
				.reverse().toString());
		else
			view.getToolkit().beep();
	}
}
