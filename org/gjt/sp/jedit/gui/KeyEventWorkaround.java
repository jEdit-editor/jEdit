/*
 * KeyEventWorkaround.java - Works around bugs in Java event handling
 * Copyright (C) 2000 Slava Pestov
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

import java.awt.event.*;
import java.awt.*;

public class KeyEventWorkaround
{
	// from JDK 1.2 InputEvent.java
	public static final int ALT_GRAPH_MASK = 1 << 5;

	public static KeyEvent processKeyEvent(KeyEvent evt)
	{
		int keyCode = evt.getKeyCode();
		int modifiers = evt.getModifiers();
		char ch = evt.getKeyChar();

		switch(evt.getID())
		{
		case KeyEvent.KEY_PRESSED:
			// get rid of keys we never need to handle
			if(keyCode == KeyEvent.VK_CONTROL ||
				keyCode == KeyEvent.VK_SHIFT ||
				keyCode == KeyEvent.VK_ALT ||
				keyCode == KeyEvent.VK_META)
				return null;

			// get rid of undefined keys
			if(keyCode == '\0')
				return null;

			handleBrokenKeys(modifiers,keyCode);

			return evt;
		case KeyEvent.KEY_TYPED:
			if((modifiers & (~ (ALT_GRAPH_MASK | KeyEvent.SHIFT_MASK))) != 0)
				return null;

			if(ch == KeyEvent.CHAR_UNDEFINED)
				return null;

			if(ch != '\b' && (ch < 0x20 || ch == 0x7f))
				return null;

			// if the last key was a broken key, filter
			// out all except 'a'-'z' that occur 750 ms after.
			if(last == LAST_BROKEN && System.currentTimeMillis()
				- lastKeyTime < 750 && !Character.isLetter(ch))
			{
				last = LAST_NOTHING;
				return null;
			}
			// otherwise, if it was ALT, filter out everything.
			else if(last == LAST_ALT && System.currentTimeMillis()
				- lastKeyTime < 750)
			{
				last = LAST_NOTHING;
				return null;
			}

			return evt;
		default:
			return evt;
		}
	}

	// private members
	private static long lastKeyTime;

	private static int last;
	private static final int LAST_NOTHING = 0;
	private static final int LAST_ALTGR = 1;
	private static final int LAST_ALT = 2;
	private static final int LAST_BROKEN = 3;

	private static void handleBrokenKeys(int modifiers, int keyCode)
	{
		// If you have any keys you would like to add to this list,
		// e-mail me

		if(modifiers == (KeyEvent.ALT_MASK | KeyEvent.CTRL_MASK)
			|| modifiers == (KeyEvent.ALT_MASK | KeyEvent.CTRL_MASK
			| KeyEvent.SHIFT_MASK))
		{
			last = LAST_ALTGR;
			return;
		}
		else if((modifiers & (~ (ALT_GRAPH_MASK | KeyEvent.SHIFT_MASK))) == 0)
		{
			last = LAST_NOTHING;
			return;
		}

		if((modifiers & KeyEvent.ALT_MASK) != 0)
			last = LAST_ALT;
		else if((keyCode < KeyEvent.VK_A || keyCode > KeyEvent.VK_Z)
			&& keyCode != KeyEvent.VK_LEFT && keyCode != KeyEvent.VK_RIGHT
			&& keyCode != KeyEvent.VK_UP && keyCode != KeyEvent.VK_DOWN
			&& keyCode != KeyEvent.VK_DELETE && keyCode != KeyEvent.VK_BACK_SPACE)
			last = LAST_BROKEN;
		else
			last = LAST_NOTHING;

		lastKeyTime = System.currentTimeMillis();
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.6  2000/10/28 00:36:58  sp
 * ML mode, Haskell mode
 *
 * Revision 1.5  2000/10/05 04:30:10  sp
 * *** empty log message ***
 *
 * Revision 1.4  2000/09/26 10:19:47  sp
 * Bug fixes, spit and polish
 *
 * Revision 1.3  2000/09/23 03:01:10  sp
 * pre7 yayayay
 *
 * Revision 1.2  2000/09/09 04:00:34  sp
 * 2.6pre6
 *
 * Revision 1.1  2000/09/07 04:46:08  sp
 * bug fixes
 *
 */
