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

			lastWasAltGR = (modifiers == (KeyEvent.ALT_MASK | KeyEvent.CTRL_MASK)
				|| modifiers == (KeyEvent.ALT_MASK | KeyEvent.CTRL_MASK
				| KeyEvent.SHIFT_MASK));

			if((modifiers & (~ (ALT_GRAPH_MASK | KeyEvent.SHIFT_MASK))) != 0
				&& isBrokenKey(modifiers,keyCode))
				lastKeyTime = System.currentTimeMillis();
			else
				lastKeyTime = 0L;

			return evt;
		case KeyEvent.KEY_TYPED:
			if((modifiers & (~ (ALT_GRAPH_MASK | KeyEvent.SHIFT_MASK))) != 0)
				return null;

			if(ch == KeyEvent.CHAR_UNDEFINED)
				return null;

			if(ch != '\b' && (ch < 0x20 || ch == 0x7f))
				return null;

			// some Java versions send a Control+Alt KEY_PRESSED
			// before an AltGR KEY_TYPED...
			if(lastWasAltGR && (modifiers & ALT_GRAPH_MASK) != 0)
			{
				lastWasAltGR = false;
				lastKeyTime = 0L;
				return evt;
			}

			// with some Java versions, the modifiers are
			// lost in the KEY_TYPED event. As a very crude
			// workaround, we get rid of KEY_TYPED events
			// that occur 500ms or less after KEY_RELEASED
			if(System.currentTimeMillis() - lastKeyTime < 500)
			{
				lastWasAltGR = false;
				lastKeyTime = 0L;
				return null;
			}

			return evt;
		default:
			return evt;
		}
	}

	// private members
	private static long lastKeyTime;
	private static boolean lastWasAltGR;

	private static boolean isBrokenKey(int modifiers, int keyCode)
	{
		// If you have any keys you would like to add to this list,
		// e-mail me
		if((modifiers & KeyEvent.ALT_MASK) != 0)
			return true;

		if(keyCode < KeyEvent.VK_A || keyCode > KeyEvent.VK_Z)
			return true;
		else
			return false;
	}
}

/*
 * ChangeLog:
 * $Log$
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
