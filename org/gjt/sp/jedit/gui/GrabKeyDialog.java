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
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class GrabKeyDialog extends JDialog
{
	public GrabKeyDialog(Component comp, String command)
	{
		super(JOptionPane.getFrameForComponent(comp),
			jEdit.getProperty("grab-key.title"),true);

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		JLabel label = new JLabel(jEdit.getProperty(
			"grab-key.caption",new String[] { command }));
		label.setBorder(new EmptyBorder(0,0,6,0));

		content.add(BorderLayout.NORTH,label);

		shortcut = new JTextField(40);
		shortcut.setEditable(false);
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

	protected void processKeyEvent(KeyEvent evt)
	{
		evt = KeyEventWorkaround.processKeyEvent(evt);
		if(evt == null)
			return;

		if(evt.getID() == KeyEvent.KEY_RELEASED)
			return;

		StringBuffer keyString = new StringBuffer(
			shortcut.getText());

		if(shortcut.getDocument().getLength() != 0)
			keyString.append(' ');

		int modifiers = evt.getModifiers();

		if((modifiers & InputEvent.CTRL_MASK) != 0)
			keyString.append('C');

		if((modifiers & InputEvent.ALT_MASK) != 0)
			keyString.append('A');

		if((modifiers & InputEvent.META_MASK) != 0)
			keyString.append('M');

		// don't want Shift+'d' recorded as S+D
		if(evt.getID() != KeyEvent.KEY_TYPED
			&& (modifiers & InputEvent.SHIFT_MASK) != 0)
			keyString.append('S');

		if(modifiers != 0)
			keyString.append('+');

		if(evt.getID() == KeyEvent.KEY_TYPED)
		{
			char keyChar = evt.getKeyChar();
			if(keyChar != ' ' && keyChar != '\n' && keyChar != '\t')
				keyString.append(keyChar);
		}
		else
		{
			String symbolicName;
			int keyCode = evt.getKeyCode();

			if((keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z)
				|| (keyCode >= KeyEvent.VK_0 && keyCode <= KeyEvent.VK_9))
			{
				if(modifiers == 0)
					return;
				else
				{
					symbolicName = String.valueOf(Character
						.toLowerCase((char)keyCode));
				}
			}
			else
			{
				symbolicName = getSymbolicName(keyCode);

				if(symbolicName == null)
					return;
			}

			keyString.append(symbolicName);
		}

		shortcut.setText(keyString.toString());
	}

	/**
	 * Returns if this component can be traversed by pressing the
	 * Tab key. This returns false.
	 */
	public final boolean isManagingFocus()
	{
		return false;
	}

	private JTextField shortcut;
	private JButton ok;
	private JButton cancel;
	private boolean isOK;

	private String getSymbolicName(int keyCode)
	{
		if(keyCode == KeyEvent.VK_UNDEFINED)
			return null;

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
				isOK = true;

			dispose();
		}
	}
}
