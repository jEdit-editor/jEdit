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
	 * Returns if a prefix key has been pressed.
	 */
	public boolean isPrefixActive()
	{
		return repeat;
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
		{
			repeatCount = 0;
			view.getCommandLine().setState(CommandLine.NULL_STATE);
		}
	}

	/**
	 * Returns the number of times the next action will be repeated.
	 */
	public int getRepeatCount()
	{
		return (repeat ? repeatCount : 1);
	}

	/**
	 * Sets the number of times the next action will be repeated.
	 * @param repeatCount The repeat count
	 */
	public void setRepeatCount(int repeatCount)
	{
		repeat = true;
		this.repeatCount = repeatCount;
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
	 * Returns the last executed action.
	 * @since jEdit 2.5pre5
	 */
	public EditAction getLastAction()
	{
		return lastAction;
	}

	/**
	 * Returns the number of times the last action was executed.
	 * @since jEdit 2.5pre5
	 */
	public int getLastActionCount()
	{
		return lastActionCount;
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

		// remember the last executed action
		if(lastAction == action)
			lastActionCount++;
		else
		{
			lastAction = action;
			lastActionCount = 1;
		}

		// remember old values, in case action changes them
		CommandLine cli = view.getCommandLine();
		boolean _repeat = repeat;
		int _repeatCount = getRepeatCount();
		int _state = cli.getState();

		// execute the action
		if(action.isRepeatable())
		{
			View view = EditAction.getView((Component)source);
			Buffer buffer = view.getBuffer();

			try
			{
				buffer.beginCompoundEdit();

				for(int i = 0; i < _repeatCount; i++)
					action.actionPerformed(evt);
			}
			finally
			{
				buffer.endCompoundEdit();
			}
		}
		else
			action.actionPerformed(evt);

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
		{
			// first of all, if the state became TOPLEVEL,
			// PROMPT_LINE or PROMPT_ONE_CHAR, do *not*
			// reset the repeat count, or in fact do anything
			// at all.
			int state = cli.getState();
			if(state == CommandLine.TOPLEVEL_STATE
				|| state == CommandLine.PROMPT_ONE_CHAR_STATE
				|| state == CommandLine.PROMPT_LINE_STATE)
				return;

			// now; if the state used to be NULL_STATE and
			// became something, don't reset it. If the state
			// used to be something else, then we *do* reset
			// it to NULL_STATE
			if(_state != CommandLine.NULL_STATE)
				cli.setState(CommandLine.NULL_STATE);
			repeat = false;
			repeatCount = 0;
		}
	}

	// protected members
	protected View view;
	protected EditAction inputAction;
	protected boolean repeat;
	protected int repeatCount;
	protected InputHandler.MacroRecorder recorder;

	protected EditAction lastAction;
	protected int lastActionCount;

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
 * Revision 1.13  2000/09/23 03:01:10  sp
 * pre7 yayayay
 *
 * Revision 1.12  2000/09/07 04:46:08  sp
 * bug fixes
 *
 * Revision 1.11  2000/09/01 11:31:01  sp
 * Rudimentary 'command line', similar to emacs minibuf
 *
 * Revision 1.10  2000/08/10 11:55:58  sp
 * VFS browser toolbar improved a little bit, font selector tweaks
 *
 * Revision 1.9  2000/07/15 10:10:18  sp
 * improved printing
 *
 * Revision 1.8  2000/05/27 05:52:06  sp
 * Improved home/end actions
 *
 * Revision 1.7  2000/05/24 07:56:05  sp
 * bug fixes
 *
 * Revision 1.6  2000/05/23 04:04:52  sp
 * Marker highlight updates, next/prev-marker actions
 *
 * Revision 1.5  2000/05/14 10:55:21  sp
 * Tool bar editor started, improved view registers dialog box
 *
 * Revision 1.4  2000/05/09 10:51:52  sp
 * New status bar, a few other things
 *
 * Revision 1.3  2000/05/07 05:48:30  sp
 * You can now edit several buffers side-by-side in a split view
 *
 * Revision 1.2  2000/04/30 07:27:13  sp
 * Ftp VFS hacking, bug fixes
 *
 */
