/*
 * GrabKeyDialog.java - Grabs keys from the keyboard
 * Copyright (C) 2001 Slava Pestov
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

import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.lang.reflect.*;
import java.util.Hashtable;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class GrabKeyDialog extends JDialog
{
	public GrabKeyDialog(Component comp, String[] shortcutData)
	{
		super(JOptionPane.getFrameForComponent(comp),
			jEdit.getProperty("grab-key.title"),true);
		this.command = shortcutData[0];
		this.oldShortcut = shortcutData[1];
		this.altShortcut = shortcutData[2];

		JPanel content = new JPanel(new BorderLayout())
		{
			/**
			 * Returns if this component can be traversed by pressing the
			 * Tab key. This returns false.
			 */
			public boolean isManagingFocus()
			{
				return false;
			}

			/**
			 * Makes the tab key work in Java 1.4.
			 * @since jEdit 3.2pre4
			 */
			public boolean getFocusTraversalKeysEnabled()
			{
				return false;
			}
		};

		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		JLabel label = new JLabel(jEdit.getProperty(
			"grab-key.caption",new String[] { command }));
		label.setBorder(new EmptyBorder(0,0,6,0));

		content.add(BorderLayout.NORTH,label);

		shortcut = new JTextField();
		shortcut.setEnabled(false);
		content.add(BorderLayout.CENTER,shortcut);

		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons,BoxLayout.X_AXIS));
		buttons.setBorder(new EmptyBorder(12,0,0,0));
		buttons.add(Box.createGlue());

		ok = new JButton(jEdit.getProperty("common.ok"));
		ok.addActionListener(new ActionHandler());
		buttons.add(ok);

		buttons.add(Box.createHorizontalStrut(12));

		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(new ActionHandler());
		buttons.add(cancel);

		buttons.add(Box.createGlue());
		content.add(BorderLayout.SOUTH,buttons);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		pack();
		setLocationRelativeTo(comp);
		setResizable(false);
		show();
	}

	public String getShortcut()
	{
		if(isOK)
			return shortcut.getText();
		else
			return null;
	}

	protected void processKeyEvent(KeyEvent _evt)
	{
		if(_evt.getID() != KeyEvent.KEY_PRESSED)
			return;

		KeyEvent evt = KeyEventWorkaround.processKeyEvent(_evt);
		if(evt == null)
		{
			Log.log(Log.DEBUG,this,"Event " + _evt + " filtered");
			return;
		}
		else
			Log.log(Log.DEBUG,this,"Event " + _evt + " passed");

		StringBuffer keyString = new StringBuffer(
			shortcut.getText());

		if(shortcut.getDocument().getLength() != 0)
			keyString.append(' ');

		boolean appendPlus = false;

		if(evt.isControlDown())
		{
			keyString.append('C');
			appendPlus = true;
		}

		if(evt.isAltDown())
		{
			keyString.append('A');
			appendPlus = true;
		}

		if(evt.isMetaDown())
		{
			keyString.append('M');
			appendPlus = true;
		}

		// don't want Shift+'d' recorded as S+D
		if(evt.getID() != KeyEvent.KEY_TYPED && evt.isShiftDown())
		{
			keyString.append('S');
			appendPlus = true;
		}

		if(appendPlus)
			keyString.append('+');

		int keyCode = evt.getKeyCode();

		String symbolicName = getSymbolicName(keyCode);

		if(symbolicName == null)
			return;

		keyString.append(symbolicName);

		shortcut.setText(keyString.toString());
	}

	/**
	 * Returns if this component can be traversed by pressing the
	 * Tab key. This returns false.
	 */
	public boolean isManagingFocus()
	{
		return false;
	}

	/**
	 * Makes the tab key work in Java 1.4.
	 * @since jEdit 3.2pre4
	 */
	public boolean getFocusTraversalKeysEnabled()
	{
		return false;
	}

	// private members

	// this is a bad hack
	private JTextField shortcut;
	private JButton ok;
	private JButton cancel;
	private boolean isOK;
	private String command;
	private String oldShortcut;
	private String altShortcut;

	private String getSymbolicName(int keyCode)
	{
		if(keyCode == KeyEvent.VK_UNDEFINED)
			return null;
		/* else if(keyCode == KeyEvent.VK_OPEN_BRACKET)
			return "[";
		else if(keyCode == KeyEvent.VK_CLOSE_BRACKET)
			return "]"; */

		if(keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z)
			return String.valueOf(Character.toLowerCase((char)keyCode));

		try
		{
			Field[] fields = KeyEvent.class.getFields();
			for(int i = 0; i < fields.length; i++)
			{
				Field field = fields[i];
				String name = field.getName();
				if(name.startsWith("VK_")
					&& field.getInt(null) == keyCode)
				{
					return name.substring(3);
				}
			}
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,this,e);
		}

		return null;
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(evt.getSource() == ok)
			{
				Log.log(Log.DEBUG, this, "Command = " + command);
				String shortcutString = shortcut.getText();
				// removing the shortcut is always OK
				if(shortcutString.length() == 0)
				{
					isOK = true;
				}
				// we don't need two identical shortcuts
				else if(shortcutString.equals(altShortcut))
				{
					GUIUtilities.error(GrabKeyDialog.this,
						"duplicate-alt-shortcut",null);
				}
				// a new shortcut is tested against existing shortcuts
				else if(!shortcutString.equals(oldShortcut))
				{
					DefaultInputHandler handler =
						(DefaultInputHandler)jEdit.getFirstView().getInputHandler();
					Object binding = handler.getKeyBinding(shortcutString);
					if(binding != null)
					{
						String[] pp = { binding instanceof Hashtable
							? jEdit.getProperty("duplicate-shortcut.prefix")
							: binding.toString()
						};
						GUIUtilities.error(GrabKeyDialog.this,
							"duplicate-shortcut",pp);
					}
					else
						isOK = true;
				}
			}
			dispose();
		}
	}
}
