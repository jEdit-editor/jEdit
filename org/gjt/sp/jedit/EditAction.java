/*
 * EditAction.java - Swing action subclass
 * Copyright (C) 1998, 1999 Slava Pestov
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

package org.gjt.sp.jedit;

import javax.swing.JPopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Component;
import java.util.EventObject;
import org.gjt.sp.jedit.textarea.InputHandler;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.util.Log;

/**
 * The class all jEdit actions must extend. It is an
 * <code>ActionListener</code> implementation with support for finding out
 * the view and buffer that invoked the action.<p>
 *
 * The <i>internal</i> name of an action is the string passed to the
 * EditAction constructor. An action instance can be obtained from it's
 * internal name with the <code>jEdit.getAction()</code> method. An
 * action's internal name can be obtained with the <code>getName()</code>
 * method.<p>
 *
 * Actions can be added at run-time with the <code>jEdit.addAction()</code>
 * method.
 *
 * An array of available actions can be obtained with the
 * <code>jEdit.getActions()</code> method.<p>
 *
 * The following properties relate to actions:
 * <ul>
 * <li><code><i>internal name</i>.label</code> - the label of the
 * action appearing in the menu bar or tooltip of a tool bar button
 * <li><code><i>internal name</i>.shortcut</code> - the keyboard
 * shortcut of the action. The action must be in a menu for this
 * to work; you can't have keyboard-only actions. Format is described
 * in documentation for the <code>GUIUtilities</code> class.
 * </ul>
 *
 * @author Slava Pestov
 * @version $Id$
 *
 * @see jEdit#getProperty(String)
 * @see jEdit#getProperty(String,String)
 * @see jEdit#getAction(String)
 * @see jEdit#getActions()
 * @see jEdit#addAction(org.gjt.sp.jedit.EditAction)
 * @see GUIUtilities#loadMenuItem(org.gjt.sp.jedit.View,String)
 */
public abstract class EditAction implements ActionListener 
{
	/**
	 * Creates a new <code>EditAction</code>. This constructor
	 * should be used by jEdit's own actions only.
	 */
	public EditAction()
	{
	}

	/**
	 * Creates a new <code>EditAction</code>.
	 * @param name The name of the action
	 */
	public EditAction(String name)
	{
		this.name = name;
	}

	/**
	 * Returns the internal name of this action.
	 */
	public final String getName()
	{
		return name;
	}

	/**
	 * Determines the view to use for the action.
	 */
	public static View getView(EventObject evt)
	{
		if(evt != null)
		{
			Object o = evt.getSource();
			if(o instanceof Component)
				return getView((Component)o);
		}
		// this shouldn't happen
		return null;
	}

	/**
	 * Determines the buffer to use for the action.
	 */
	public static Buffer getBuffer(EventObject evt)
	{
		View view = getView(evt);
		if(view != null)
			return view.getBuffer();
		return null;
	}

	/**
	 * Finds the view parent of the specified component.
	 * @since jEdit 2.2pre4
	 */
	public static View getView(Component comp)
	{
		for(;;)
		{
			if(comp instanceof View)
				return (View)comp;
			else if(comp == null)
				break;
			else if(comp instanceof JPopupMenu)
				comp = ((JPopupMenu)comp).getInvoker();
			else
				comp = comp.getParent();
		}
		return null;
	}

	/**
	 * Returns if this edit action should be displayed as a check box
	 * in menus.
	 * @since jEdit 2.2pre4
	 */
	public boolean isToggle()
	{
		return false;
	}

	/**
	 * If this edit action is a toggle, returns if it is selected or not.
	 * @param comp The component
	 * @since jEdit 2.2pre4
	 */
	public boolean isSelected(Component comp)
	{
		return false;
	}

	// private members
	private String name;

	public static class Wrapper extends EditAction
		implements InputHandler.NonRepeatable,
		InputHandler.NonRecordable
	{
		public Wrapper(String name)
		{
			super(name);
		}

		public Wrapper(EditAction action)
		{
			super(action.name);
			this.action = action;
		}

		public void actionPerformed(ActionEvent evt)
		{
			View view = EditAction.getView(evt);
			JEditTextArea textArea = view.getTextArea();

			if(action == null)
				loadAction();

			textArea.getInputHandler().executeAction(action,
				textArea,evt.getActionCommand());
		}

		public boolean isToggle()
		{
			if(action == null)
				return false;
			else
				return action.isToggle();
		}

		public boolean isSelected(Component comp)
		{
			if(action == null)
				return false;
			else
				return action.isSelected(comp);
		}

		// private members
		private EditAction action;

		private void loadAction()
		{
			String className = "org.gjt.sp.jedit.actions."
				+ getName().replace('-','_');

			Log.log(Log.DEBUG,this,"Loading action " + name +
				" (class=" + className + ")");

			try
			{
				Class clazz;
				ClassLoader loader = getClass().getClassLoader();
				if(loader == null)
					clazz = Class.forName(className);
				else
					clazz = loader.loadClass(className);

				action = (EditAction)clazz.newInstance();
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,this,"Cannot load action " + className);
				Log.log(Log.ERROR,this,e);
			}
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.17  1999/12/06 00:06:14  sp
 * Bug fixes
 *
 * Revision 1.16  1999/11/28 00:33:06  sp
 * Faster directory search, actions slimmed down, faster exit/close-all
 *
 * Revision 1.15  1999/11/27 06:01:20  sp
 * Faster file loading, geometry fix
 *
 * Revision 1.14  1999/11/07 06:51:43  sp
 * Check box menu items supported
 *
 * Revision 1.13  1999/10/31 07:15:34  sp
 * New logging API, splash screen updates, bug fixes
 *
 * Revision 1.12  1999/10/02 01:12:36  sp
 * Search and replace updates (doesn't work yet), some actions moved to TextTools
 *
 * Revision 1.11  1999/09/30 12:21:04  sp
 * No net access for a month... so here's one big jEdit 2.1pre1
 *
 * Revision 1.10  1999/05/22 08:33:53  sp
 * FAQ updates, mode selection tweak, patch mode update, javadoc updates, JDK 1.1.8 fix
 *
 * Revision 1.9  1999/03/24 05:45:27  sp
 * Juha Lidfors' backup directory patch, removed debugging messages from various locations, documentation updates
 *
 * Revision 1.8  1999/03/21 08:37:15  sp
 * Slimmer action system, history text field update
 *
 * Revision 1.7  1999/03/21 07:53:14  sp
 * Plugin doc updates, action API change, new method in MiscUtilities, new class
 * loader, new plugin interface
 *
 * Revision 1.6  1999/03/16 04:34:45  sp
 * HistoryTextField updates, moved generate-text to a plugin, fixed spelling mistake in EditAction Javadocs
 *
 */
