/*
 * InputHandler.java - Manages key bindings and executes actions
 * Copyright (C) 1999 Slava Pestov
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

import javax.swing.JPopupMenu;
import java.awt.event.*;
import java.awt.Component;
import java.util.*;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

/**
 * An input handler converts the user's key strokes into concrete actions.
 * It also takes care of macro recording and action repetition.<p>
 *
 * This class provides all the necessary support code for an input
 * handler, but doesn't actually do any key binding logic. It is up
 * to the implementations of this class to do so.
 *
 * @author Slava Pestov
 * @version $Id$
 * @see org.gjt.sp.jedit.gui.DefaultInputHandler
 */
public abstract class InputHandler extends KeyAdapter
{
	/**
	 * Creates a new input handler.
	 * @param view The view
	 */
	public InputHandler(View view)
	{
		this.view = view;
	}

	/**
	 * Adds a key binding to this input handler.
	 * @param keyBinding The key binding (the format of this is
	 * input-handler specific)
	 * @param action The action
	 */
	public abstract void addKeyBinding(String keyBinding, EditAction action);

	/**
	 * Removes a key binding from this input handler.
	 * @param keyBinding The key binding
	 */
	public abstract void removeKeyBinding(String keyBinding);

	/**
	 * Removes all key bindings from this input handler.
	 */
	public abstract void removeAllKeyBindings();

	/**
	 * Grabs the next key typed event and invokes the specified
	 * action with the key as a the action command.
	 * @param action The action
	 */
	public void grabNextKeyStroke(EditAction action)
	{
		grabAction = action;
	}

	/**
	 * Returns if repeating is enabled. When repeating is enabled,
	 * actions will be executed multiple times. This is usually
	 * invoked with a special key stroke in the input handler.
	 */
	public boolean isRepeatEnabled()
	{
		return repeat;
	}

	/**
	 * Enables repeating. When repeating is enabled, actions will be
	 * executed multiple times. Once repeating is enabled, the input
	 * handler should read a number from the keyboard.
	 */
	public void setRepeatEnabled(boolean repeat)
	{
		this.repeat = repeat;
		if(!repeat)
			repeatCount = 0;

		if(view != null)
		{
			JEditTextArea[] textAreas = view.getTextAreas();
			for(int i = 0; i < textAreas.length; i++)
				textAreas[i].getStatus().repaint();
		}
	}

	/**
	 * Returns the number of times the next action will be repeated.
	 */
	public int getRepeatCount()
	{
		return (repeat ? Math.max(1,repeatCount) : 1);
	}

	/**
	 * Sets the number of times the next action will be repeated.
	 * @param repeatCount The repeat count
	 */
	public void setRepeatCount(int repeatCount)
	{
		this.repeatCount = repeatCount;

		if(view != null)
		{
			JEditTextArea[] textAreas = view.getTextAreas();
			for(int i = 0; i < textAreas.length; i++)
				textAreas[i].getStatus().repaint();
		}
	}

	/**
	 * Returns the action used to handle text input.
	 */
	public EditAction getInputAction()
	{
		return inputAction;
	}

	/**
	 * Sets the action used to handle text input.
	 * @param inputAction The new input action
	 */
	public void setInputAction(EditAction inputAction)
	{
		this.inputAction = inputAction;
	}
	
	/**
	 * Returns the macro recorder. If this is non-null, all executed
	 * actions should be forwarded to the recorder.
	 */
	public InputHandler.MacroRecorder getMacroRecorder()
	{
		return recorder;
	}

	/**
	 * Sets the macro recorder. If this is non-null, all executed
	 * actions should be forwarded to the recorder.
	 * @param recorder The macro recorder
	 */
	public void setMacroRecorder(InputHandler.MacroRecorder recorder)
	{
		this.recorder = recorder;
	}

	/**
	 * Executes the specified action, repeating and recording it as
	 * necessary.
	 * @param action The action
	 * @param source The event source
	 * @param actionCommand The action command
	 */
	public void executeAction(EditAction action, Object source,
		String actionCommand)
	{
		// create event
		ActionEvent evt = new ActionEvent(source,
			ActionEvent.ACTION_PERFORMED,
			actionCommand);

		// don't do anything if the action is a wrapper
		if(action.isWrapper())
		{
			action.actionPerformed(evt);
			return;
		}

		// remember old values, in case action changes them
		boolean _repeat = repeat;
		int _repeatCount = getRepeatCount();

		// execute the action
		if(action.isRepeatable())
		{
			for(int i = 0; i < _repeatCount; i++)
				action.actionPerformed(evt);
		}
		else
			action.actionPerformed(evt);

		// do recording. Notice that we do no recording whatsoever
		// for actions that grab keys
		if(grabAction == null)
		{
			if(recorder != null)
			{
				if(action.isRecordable())
				{
					if(_repeatCount != 1)
					{
						recorder.actionPerformed(
							jEdit.getAction("repeat"),
							String.valueOf(_repeatCount));
					}

					recorder.actionPerformed(action,actionCommand);
				}
			}

			// If repeat was true originally, clear it
			// Otherwise it might have been set by the action, etc
			if(_repeat)
				setRepeatEnabled(false);
		}
	}

	// protected members
	protected View view;
	protected EditAction inputAction;
	protected EditAction grabAction;
	protected boolean repeat;
	protected int repeatCount;
	protected InputHandler.MacroRecorder recorder;

	/**
	 * If a key is being grabbed, this method should be called with
	 * the appropriate key event. It executes the grab action with
	 * the typed character as the parameter.
	 */
	protected void handleGrabAction(KeyEvent evt)
	{
		// Clear it *before* it is executed so that executeAction()
		// resets the repeat count
		EditAction _grabAction = grabAction;
		grabAction = null;

		char keyChar = evt.getKeyChar();
		int keyCode = evt.getKeyCode();

		String arg;

		if(keyChar != KeyEvent.VK_UNDEFINED)
			arg = String.valueOf(keyChar);
		else if(keyCode == KeyEvent.VK_TAB)
			arg = "\t";
		else if(keyCode == KeyEvent.VK_ENTER)
			arg = "\n";
		else
			arg = "\0";

		executeAction(_grabAction,evt.getSource(),arg);
	}

	/**
	 * Macro recorder.
	 */
	public interface MacroRecorder
	{
		void actionPerformed(EditAction action, String actionCommand);
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.1  2000/04/28 09:29:12  sp
 * Key binding handling improved, VFS updates, some other stuff
 *
 */
