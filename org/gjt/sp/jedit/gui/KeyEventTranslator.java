/*
 * KeyEventTranslator.java - Hides some warts of AWT event API
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2003, 2005 Slava Pestov
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

//{{{ Imports
import java.awt.event.*;
import javax.swing.KeyStroke;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.gjt.sp.jedit.Debug;
import org.gjt.sp.jedit.OperatingSystem;
import org.gjt.sp.util.Log;
//}}}

/** In conjunction with the <code>KeyEventWorkaround</code>, hides some
 * warts in the AWT key event API.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class KeyEventTranslator
{
	//{{{ addTranslation() method
	/**
	 * Adds a keyboard translation.
	 * @param key1 Translate this key
	 * @param key2 Into this key
	 * @since jEdit 4.2pre3
	 */
	public static void addTranslation(Key key1, Key key2)
	{
		transMap.put(key1,key2);
	} //}}}

	//{{{ translateKeyEvent() method

	protected static KeyEvent lastKeyPressEvent;

	protected static boolean lastKeyPressAccepted;

	/**
	 * Pass this an event from {@link
	 * KeyEventWorkaround#processKeyEvent(java.awt.event.KeyEvent)}.
	 * @param evt the KeyEvent to translate
	 * @since jEdit 4.2pre3
	 */
	public static Key translateKeyEvent(KeyEvent evt)
	{
		int modifiers = evt.getModifiersEx();
		Key returnValue;

		switch(evt.getID())
		{
		case KeyEvent.KEY_PRESSED:
			int keyCode = evt.getKeyCode();
			if((keyCode >= KeyEvent.VK_0
				&& keyCode <= KeyEvent.VK_9)
				|| (keyCode >= KeyEvent.VK_A
				&& keyCode <= KeyEvent.VK_Z))
			{
				returnValue = new Key(modifiersToString(modifiers), '\0', Character.toLowerCase((char)keyCode));			
			}
			else
			{
				if(keyCode == KeyEvent.VK_TAB)
				{
					evt.consume();
					returnValue = new Key(
						modifiersToString(modifiers),
						keyCode,'\0');
				}
				else if(keyCode == KeyEvent.VK_SPACE)
				{
					// for SPACE or S+SPACE we pass the
					// key typed since international
					// keyboards sometimes produce a
					// KEY_PRESSED SPACE but not a
					// KEY_TYPED SPACE, eg if you have to
					// do a "<space> to insert ".
					if((modifiers & ~InputEvent.SHIFT_DOWN_MASK) == 0)
						returnValue = null;
                    else 
						returnValue = new Key(modifiersToString(modifiers), 0, ' ');
				}
				else
				{
					returnValue = new Key(
						modifiersToString(modifiers),
						keyCode,'\0');
				}
			}
			break;
		case KeyEvent.KEY_TYPED:
			char ch = evt.getKeyChar();

			switch(ch)
			{
			case '\n':
			case '\t':
			case '\b':
				return null;
			case ' ':
                if ((modifiers & ~InputEvent.SHIFT_DOWN_MASK) != 0)
					return null;
			}

			int ignoreMods;
			if(Debug.ALT_KEY_PRESSED_DISABLED)
			{
				/* on macOS, A+ can be user input */
				ignoreMods = InputEvent.SHIFT_DOWN_MASK
					| InputEvent.ALT_GRAPH_DOWN_MASK
					| InputEvent.ALT_DOWN_MASK;
			}
			else
			{
				/* on macOS, A+ can be user input */
				ignoreMods = InputEvent.SHIFT_DOWN_MASK
					| InputEvent.ALT_GRAPH_DOWN_MASK;
			}

			if((modifiers & InputEvent.ALT_GRAPH_DOWN_MASK) == 0
				&& (modifiers & ~ignoreMods) != 0)
			{				
				return null;
			}
			else
			{
				if(ch == ' ')
				{
					returnValue = new Key(
						modifiersToString(modifiers),
						0,ch);
				}
				else
					returnValue = new Key(null,0,ch);
			}
			break;
		default:
			return null;
		}

		/* I guess translated events do not have the 'evt' field set
		so consuming won't work. I don't think this is a problem as
		nothing uses translation anyway */
		Key trans = transMap.get(returnValue);
		if(trans == null)
			return returnValue;
		else
			return trans;
	} //}}}

	//{{{ parseKey() method
	/**
	 * Converts a string to a keystroke. The string should be of the
	 * form <i>modifiers</i>+<i>shortcut</i> where <i>modifiers</i>
	 * is any combination of A for Alt, C for Control, S for Shift
	 * or M for Meta, and <i>shortcut</i> is either a single character,
	 * or a keycode name from the <code>KeyEvent</code> class, without
	 * the <code>VK_</code> prefix.
	 * @param keyStroke A string description of the key stroke
	 * @since jEdit 4.2pre3
	 */
	public static Key parseKey(String keyStroke)
	{
		if(keyStroke == null)
			return null;

		String key;
        	int modifiers = 0;

        	String[] pieces = keyStroke.split("\\+", 2);
        	if (pieces.length == 1)
        	{
        		key = pieces[0];
        	}
        	else
        	{
        		modifiers = parseModifiers(pieces[0]);
        		key = pieces[1];
        	}

		if (key.length() == 1)
		{
			return new Key(modifiersToString(modifiers), 0, key.charAt(0));
		}
		else if (key.length() == 0)
		{
			Log.log(Log.ERROR,KeyEventTranslator.class,
					"Invalid key stroke: " + keyStroke);
			return null;
		}
		else if (key.equals("SPACE"))
		{
			return new Key(modifiersToString(modifiers), 0, ' ');
		}

		int code = parseKeyCode(key);
		if (code == KeyEvent.VK_UNDEFINED)
		{
			return null;
		}
		else
		{
			return new Key(modifiersToString(modifiers), code, '\0');
		}
	} //}}}

	//{{{ parseKeyStroke() method
        /**
         * Converts a string to a Swing KeyStroke. The string should be of the
	 * form <i>modifiers</i>+<i>shortcut</i> where <i>modifiers</i>
	 * is any combination of A for Alt, C for Control, S for Shift
	 * or M for Meta, and <i>shortcut</i> is either a single character,
	 * or a keycode name from the <code>KeyEvent</code> class, without
	 * the <code>VK_</code> prefix. Returns null if the string corresponds
	 * to multiple KeyStrokes (e.g., "C+e C+COMMA").
	 * @param shortcut A string description of the key stroke
	 * @since jEdit 5.0
         */
        public static KeyStroke parseKeyStroke(String shortcut)
        {
        	if (shortcut == null || shortcut.indexOf(' ') != -1)
        		return null;

        	String key;
        	int modifiers = 0;

        	String[] pieces = shortcut.split("\\+", 2);
        	if (pieces.length == 1)
        	{
        		key = pieces[0];
        	}
        	else
        	{
        		modifiers = parseModifiers(pieces[0]);
        		key = pieces[1];
        	}

		if (key.length() == 1)
		{
			return KeyStroke.getKeyStroke(Character.valueOf(key.charAt(0)), modifiers);
		}
		else if (key.length() == 0)
		{
			Log.log(Log.ERROR,KeyEventTranslator.class,
					"Invalid key stroke: " + shortcut);
			return null;
		}

		int keyCode = parseKeyCode(key);
		if (keyCode == KeyEvent.VK_UNDEFINED)
		{
			return null;
		}
		else
		{
			return KeyStroke.getKeyStroke(keyCode, modifiers);
		}
	} //}}}

	//{{{ setModifierMapping() method
	/**
	 * Changes the mapping between symbolic modifier key names
	 * (<code>C</code>, <code>A</code>, <code>M</code>, <code>S</code>) and
	 * Java modifier flags.
	 *
	 * You can map more than one Java modifier to a symobolic modifier, for
	 * example :
	 * <p><code>
	 	setModifierMapping(
	 		InputEvent.CTRL_DOWN_MASK,
	 		InputEvent.ALT_DOWN_MASK | InputEvent.META_DOWN_MASK,
	 		0,
	 		InputEvent.SHIFT_MASK);
	   </code></p>
	 *
	 * You cannot map a Java modifer to more than one symbolic modifier.
	 *
	 * @param c The modifier(s) to map the <code>C</code> modifier to
	 * @param a The modifier(s) to map the <code>A</code> modifier to
	 * @param m The modifier(s) to map the <code>M</code> modifier to
	 * @param s The modifier(s) to map the <code>S</code> modifier to
	 *
	 * @since jEdit 4.2pre3
	 */
	public static void setModifierMapping(int c, int a, int m, int s)
	{

		int duplicateMapping =
			(c & a) | (c & m) | (c & s) | (a & m) | (a & s) | (m & s);

		if((duplicateMapping & InputEvent.CTRL_DOWN_MASK) != 0)
		{
			throw new IllegalArgumentException(
				"CTRL is mapped to more than one modifier");
		}
		if((duplicateMapping & InputEvent.ALT_DOWN_MASK) != 0)
		{
			throw new IllegalArgumentException(
				"ALT is mapped to more than one modifier");
		}
		if((duplicateMapping & InputEvent.META_DOWN_MASK) != 0)
		{
			throw new IllegalArgumentException(
				"META is mapped to more than one modifier");
		}
		if((duplicateMapping & InputEvent.SHIFT_DOWN_MASK) != 0)
		{
			throw new IllegalArgumentException(
				"SHIFT is mapped to more than one modifier");
		}

		KeyEventTranslator.c = c;
		KeyEventTranslator.a = a;
		KeyEventTranslator.m = m;
		KeyEventTranslator.s = s;
	} //}}}

	//{{{ getSymbolicModifierName() method
	/**
	 * Returns a the symbolic modifier name for the specified Java modifier
	 * flag.
	 *
	 * @param mod A modifier constant from <code>InputEvent</code>
	 *
	 * @since jEdit 4.2pre3
	 */
	public static char getSymbolicModifierName(int mod)
	{
		if((mod & c) != 0)
			return 'C';
		else if((mod & a) != 0)
			return 'A';
		else if((mod & m) != 0)
			return 'M';
		else if((mod & s) != 0)
			return 'S';
		else
			return '\0';
	} //}}}

	//{{{ modifiersToString() method
	private static final int[] MODS = {
		InputEvent.CTRL_DOWN_MASK,
		InputEvent.ALT_DOWN_MASK,
		InputEvent.META_DOWN_MASK,
		InputEvent.SHIFT_DOWN_MASK
	};

	public static String modifiersToString(int mods)
	{
		StringBuilder buf = null;

		for (int modifier : MODS)
		{
			if ((mods & modifier) != 0)
				buf = lazyAppend(buf, getSymbolicModifierName(modifier));
		}

		if(buf == null)
			return null;
		else
			return buf.toString();
	} //}}}

	//{{{ getModifierString() method
	/**
	 * Returns a string containing symbolic modifier names set in the
	 * specified event.
	 *
	 * @param evt The event
	 *
	 * @since jEdit 4.2pre3
	 */
	public static String getModifierString(InputEvent evt)
	{
		StringBuilder buf = new StringBuilder();
		if(evt.isControlDown())
			buf.append(getSymbolicModifierName(InputEvent.CTRL_DOWN_MASK));
		if(evt.isAltDown())
			buf.append(getSymbolicModifierName(InputEvent.ALT_DOWN_MASK));
		if(evt.isMetaDown())
			buf.append(getSymbolicModifierName(InputEvent.META_DOWN_MASK));
		if(evt.isShiftDown())
			buf.append(getSymbolicModifierName(InputEvent.SHIFT_DOWN_MASK));
		return buf.length() == 0 ? null : buf.toString();
	} //}}}

	static int c, a, m, s;

	//{{{ Private members
	/** This map is a pool of Key. */
	private static final Map<Key, Key> transMap = new HashMap<Key, Key>();

	private static StringBuilder lazyAppend(StringBuilder buf, char ch)
	{
		if(buf == null)
			buf = new StringBuilder();
		if(buf.indexOf(String.valueOf(ch)) == -1)
			buf.append(ch);
		return buf;
	}

	static
	{
		if(OperatingSystem.isMacOS())
		{
			setModifierMapping(
				InputEvent.META_DOWN_MASK,  /* == C+ */
				InputEvent.CTRL_DOWN_MASK,  /* == A+ */
				/* M+ discarded by key event workaround! */
				InputEvent.ALT_DOWN_MASK,   /* == M+ */
				InputEvent.SHIFT_DOWN_MASK  /* == S+ */);
		}
		else
		{
			setModifierMapping(
				InputEvent.CTRL_DOWN_MASK,
				InputEvent.ALT_DOWN_MASK,
				InputEvent.META_DOWN_MASK,
				InputEvent.SHIFT_DOWN_MASK);
		}
	}

	//{{{ parseModifiers() method
	private static int parseModifiers(String modifierString)
	{
		int modifiers = 0;

		for (char ch : modifierString.toCharArray())
		{
			switch (Character.toUpperCase(ch))
			{
			case 'A':
				modifiers |= a;
				break;
			case 'C':
				modifiers |= c;
				break;
			case 'M':
				modifiers |= m;
				break;
			case 'S':
				modifiers |= s;
				break;
			}
		}

		return modifiers;
	} //}}

	//{{{ parseKeyCode() method
	/**
	 * Parses the name of a keycode from the KeyEvent class to the the actual
	 * member of that class. (e.g., the string "VK_COMMA" becomes the integer
	 * KeyEvent.VK_COMMA). Returns KeyEvent.VK_UNDEFINED if no such member is found.
	 */
	private static int parseKeyCode(String code)
	{
		try
		{
			return KeyEvent.class.getField("VK_".concat(code)).getInt(null);
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,KeyEventTranslator.class,
				"Invalid key code: " + code);
			return KeyEvent.VK_UNDEFINED;
		}
	} //}}}

	//}}}

	//{{{ Key class
	public static class Key
	{
		public final String modifiers;
		public final int key;
		public final char input;

		private final int hashCode;
		/**
			Whether this Key event applies to all jEdit windows (and not only a specific jEdit GUI component).
		*/
		protected boolean isFromGlobalContext;

		public Key(String modifiers, int key, char input)
		{
			this.modifiers = modifiers;
			this.key = key;
			this.input = input;
			hashCode = key + input;
		}

		@Override
		public int hashCode()
		{
			return hashCode;
		}

		@Override
		public boolean equals(Object o)
		{
			if(o instanceof Key)
			{
				Key k = (Key)o;
				if(Objects.equals(modifiers, k.modifiers) && key == k.key
					&& input == k.input)
				{
					return true;
				}
			}

			return false;
		}

		@Override
		public String toString()
		{
			return (modifiers == null ? "" : modifiers)
				+ '<'
				+ Integer.toString(key,16)
				+ ','
				+ Integer.toString(input,16)
				+ '>';
		}

		public void setIsFromGlobalContext(boolean to)
		{
			isFromGlobalContext = to;
		}

		public boolean isFromGlobalContext()
		{
			return isFromGlobalContext;
		}
	} //}}}
}
