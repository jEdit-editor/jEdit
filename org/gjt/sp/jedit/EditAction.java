/*
 * EditAction.java - jEdit action listener
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
 * shortcut of the action
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
		if(name == null)
		{
			String clazz = getClass().getName();
			clazz = clazz.substring("org.gjt.sp.jedit.actions.".length());
			clazz = clazz.replace('_','-');
			name = clazz;
		}

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
			else if(comp instanceof JPopupMenu)
				comp = ((JPopupMenu)comp).getInvoker();
			else if(comp != null)
				comp = comp.getParent();
			else
				break;
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

	/**
	 * jEdit wraps all EditActions in this wrapper so that they can
	 * be recorded to macros and repeated. The wrapper also handles
	 * autoloading of built-in actions.<p>
	 *
	 * This class should never be used directly. The
	 * <code>jEdit.addAction()</code> method creates instances of
	 * this class automatically.
	 */
	public static class Wrapper extends EditAction
	{
		/**
		 * Creates a new wrapper that will autoload the built-in
		 * action with the specified name.
		 * @param name
		 */
		public Wrapper(String name)
		{
			super(name);
		}

		/**
		 * Creates a new wrapper that will autoload the specified
		 * plugin action.
		 * @param action The plugin action
		 */
		public Wrapper(EditAction action)
		{
			super(action.name);
			this.action = action;
		}

		/**
		 * Called when the user selects this action from a menu.
		 * It passes the action through the
		 * <code>InputHandler.executeAction()</code> method,
		 * which performs any recording or repeating. It also
		 * loads the action if necessary.
		 *
		 * @param evt The action event
		 */
		public void actionPerformed(ActionEvent evt)
		{
			loadIfNecessary();

			View view = EditAction.getView(evt);
			JEditTextArea textArea = view.getTextArea();

			textArea.getInputHandler().executeAction(action,
				textArea,evt.getActionCommand());
		}

		/**
		 * Delegates to the underlying action. Note that
		 * built-in/autoloaded actions cannot be toggles.
		 */
		public boolean isToggle()
		{
			if(action == null)
				return false;
			else
				return action.isToggle();
		}

		/**
		 * Delegates to the underlying action. Note that
		 * built-in/autoloaded actions cannot be toggles.
		 */
		public boolean isSelected(Component comp)
		{
			if(action == null)
				return false;
			else
				return action.isSelected(comp);
		}

		/**
		 * Loads the action if necessary.
		 */
		public void loadIfNecessary()
		{
			if(action != null)
				return;

			String className = "org.gjt.sp.jedit.actions."
				+ Wrapper.this.getName().replace('-','_');

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

		// private members
		private EditAction action;
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.23  2000/04/03 10:22:24  sp
 * Search bar
 *
 * Revision 1.22  2000/02/15 07:44:30  sp
 * bug fixes, doc updates, etc
 *
 * Revision 1.21  2000/01/14 04:23:50  sp
 * 2.3pre2 stuff
 *
 * Revision 1.20  1999/12/13 03:40:29  sp
 * Bug fixes, syntax is now mostly GPL'd
 *
 * Revision 1.19  1999/12/10 03:22:46  sp
 * Bug fixes, old loading code is now used again
 *
 * Revision 1.18  1999/12/07 06:30:48  sp
 * Compile errors fixed, new 'new view' icon
 *
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
 */
