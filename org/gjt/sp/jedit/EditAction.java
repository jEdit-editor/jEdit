/*
 * EditAction.java - jEdit action listener
 * Copyright (C) 1998, 1999, 2000 Slava Pestov
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
import org.gjt.sp.util.Log;

/**
 * The class all jEdit actions must extend.<p>
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
public abstract class EditAction
// no longer implements ActionListener
{
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
	 * Invokes the action.
	 * @param view The view
	 * @since jEdit 2.7pre2
	 */
	public void invoke(View view)
	{
		// default implementation
		ActionEvent evt = new ActionEvent(view,
			ActionEvent.ACTION_PERFORMED,
			null);

		actionPerformed(evt);
	}

	/**
	 * @deprecated Extend invoke() instead, or better yet, write
	 * your actions in BeanShell
	 */
	public void actionPerformed(ActionEvent evt) {}

	/**
	 * @deprecated No longer necessary.
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
	 * @deprecated No longer necessary.
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

	/**
	 * Returns if this edit action should not be repeated. Returns false
	 * by default.
	 * @since jEdit 2.7pre2
	 */
	public boolean noRepeat()
	{
		return false;
	}

	/**
	 * Returns if this edit action should not be recorded. Returns false
	 * by default.
	 * @since jEdit 2.7pre2
	 */
	public boolean noRecord()
	{
		return false;
	}

	/**
	 * Returns the BeanShell code that will replay this action.
	 * @since jEdit 2.7pre2
	 */
	public String getCode()
	{
		return "view.getInputHandler().invokeAction("
			+ "jEdit.getAction(\"" + name + "\"))";
	}

	// private members
	private String name;

	/**
	 * 'Wrap' EditActions in this class to turn them into AWT
	 * ActionListeners, that can be attached to buttons, menu items, etc.
	 */
	public static class Wrapper implements ActionListener
	{
		public Wrapper(EditAction action)
		{
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
			// Let input handler do recording, repeating, etc
			EditAction.getView(evt).getInputHandler()
				.invokeAction(action);
		}

		// private members
		private EditAction action;
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.33  2000/11/21 02:58:03  sp
 * 2.7pre2 finished
 *
 * Revision 1.32  2000/11/17 11:15:59  sp
 * Actions removed, documentation updates, more BeanShell work
 *
 * Revision 1.31  2000/11/16 10:25:16  sp
 * More macro work
 *
 * Revision 1.30  2000/11/16 04:01:10  sp
 * BeanShell macros started
 *
 * Revision 1.29  2000/11/13 11:19:26  sp
 * Search bar reintroduced, more BeanShell stuff
 *
 * Revision 1.28  2000/09/03 03:16:52  sp
 * Search bar integrated with command line, enhancements throughout
 *
 * Revision 1.27  2000/09/01 11:31:00  sp
 * Rudimentary 'command line', similar to emacs minibuf
 *
 * Revision 1.26  2000/07/15 02:45:22  sp
 * Minor changes to core for EditBuddy plugin
 *
 * Revision 1.25  2000/04/28 09:29:11  sp
 * Key binding handling improved, VFS updates, some other stuff
 *
 * Revision 1.24  2000/04/14 11:57:38  sp
 * Text area actions moved to org.gjt.sp.jedit.actions package
 *
 * Revision 1.23  2000/04/03 10:22:24  sp
 * Search bar
 *
 * Revision 1.22  2000/02/15 07:44:30  sp
 * bug fixes, doc updates, etc
 *
 */
