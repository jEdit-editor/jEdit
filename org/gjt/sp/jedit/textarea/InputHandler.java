/*
 * InputHandler.java - Manages key bindings and executes actions
 * Copyright (C) 1999 Slava Pestov
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA.
 */

package org.gjt.sp.jedit.textarea;

import javax.swing.text.*;
import javax.swing.JPopupMenu;
import java.awt.event.*;
import java.awt.Component;
import java.util.EventObject;

/**
 * An abstract class for a key event handler. Concrete implementations
 * provide specific keystroke to action mappings.
 * @author Slava Pestov
 * @version $Id$
 * @see org.gjt.sp.jedit.textarea.DefaultInputHandler
 */
public abstract class InputHandler extends KeyAdapter
{
	public static final ActionListener BACKSPACE = new backspace();
	public static final ActionListener DELETE = new delete();
	public static final ActionListener END = new end(false);
	public static final ActionListener SELECT_END = new end(true);
	public static final ActionListener INSERT_BREAK = new insert_break();
	public static final ActionListener INSERT_TAB = new insert_tab();
	public static final ActionListener HOME = new home(false);
	public static final ActionListener SELECT_HOME = new home(true);
	public static final ActionListener NEXT_CHAR = new next_char(false);
	public static final ActionListener NEXT_LINE = new next_line(false);
	public static final ActionListener NEXT_PAGE = new next_page(false);
	public static final ActionListener NEXT_WORD = new next_word(false);
	public static final ActionListener SELECT_NEXT_CHAR = new next_char(true);
	public static final ActionListener SELECT_NEXT_LINE = new next_line(true);
	public static final ActionListener SELECT_NEXT_PAGE = new next_page(true);
	public static final ActionListener SELECT_NEXT_WORD = new next_word(true);
	public static final ActionListener OVERWRITE = new overwrite();
	public static final ActionListener PREV_CHAR = new prev_char(false);
	public static final ActionListener PREV_LINE = new prev_line(false);
	public static final ActionListener PREV_PAGE = new prev_page(false);
	public static final ActionListener PREV_WORD = new prev_word(false);
	public static final ActionListener SELECT_PREV_CHAR = new prev_char(true);
	public static final ActionListener SELECT_PREV_LINE = new prev_line(true);
	public static final ActionListener SELECT_PREV_PAGE = new prev_page(true);
	public static final ActionListener SELECT_PREV_WORD = new prev_word(true);
	public static final ActionListener REPEAT = new repeat();
	public static final ActionListener TOGGLE_RECT = new toggle_rect();

	// Default action
	public static final ActionListener INSERT_CHAR = new insert_char();

	public static final ActionListener[] ACTIONS = {
		BACKSPACE, DELETE, END, SELECT_END, INSERT_BREAK,
		INSERT_TAB, HOME, SELECT_HOME, NEXT_CHAR, NEXT_LINE,
		NEXT_PAGE, NEXT_WORD, SELECT_NEXT_CHAR, SELECT_NEXT_LINE,
		SELECT_NEXT_PAGE, SELECT_NEXT_WORD, OVERWRITE, PREV_CHAR,
		PREV_LINE, PREV_PAGE, PREV_WORD, SELECT_PREV_CHAR,
		SELECT_PREV_LINE, SELECT_PREV_PAGE, SELECT_PREV_WORD,
		REPEAT, TOGGLE_RECT, INSERT_CHAR };

	public static final String[] ACTION_NAMES = {
		"backspace", "delete", "end", "select-end", "insert-break",
		"insert-tab", "home", "select-home", "next-char", "next-line",
		"next-page", "next-word", "select-next-char", "select-next-line",
		"select-next-page", "select-next-word", "overwrite", "prev-char",
		"prev-line", "prev-page", "prev-word", "select-prev-char",
		"select-prev-line", "select-prev-page", "select-prev-word",
		"repeat", "toggle-rect", "insert-char" };

	/**
	 * Adds the default key bindings to this input handler.
	 */
	public abstract void addDefaultKeyBindings();

	/**
	 * Adds a key binding to this input handler.
	 * @param keyBinding The key binding (the format of this is
	 * input-handler specific)
	 * @param action The action
	 */
	public abstract void addKeyBinding(String keyBinding, ActionListener action);

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
	public void grabNextKeyStroke(ActionListener listener)
	{
		grabAction = listener;
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
	 * Returns a copy of this input handler that shares the same
	 * key bindings. Setting key bindings in the copy will also
	 * set them in the original.
	 */
	public abstract InputHandler copy();

	/**
	 * Executes the specified action.
	 * @param listener The action listener
	 * @param source The event source
	 * @param actionCommand The action command
	 */
	public void executeAction(ActionListener listener, Object source,
		String actionCommand)
	{
		/**
		 * We have to hardcode the recording of 'repeat' because
		 * when it is first invoked, its action command is null,
		 * and then input is received from the keyboard.
		 */
		if(recorder != null)
		{
			int repeatCount = getRepeatCount();
			if(repeatCount != 1)
			{
				recorder.actionPerformed(REPEAT,String.valueOf(repeatCount));
			}

			if(!(listener instanceof InputHandler.NonRecordable))
				recorder.actionPerformed(listener,actionCommand);
		}

		ActionEvent evt = new ActionEvent(source,
			ActionEvent.ACTION_PERFORMED,
			actionCommand);

		boolean _repeat = repeat;

		if(listener instanceof InputHandler.NonRepeatable)
			listener.actionPerformed(evt);
		else
		{
			int _repeatCount = repeatCount;
			repeatCount = 0;
			for(int i = 0; i < Math.max(1,_repeatCount); i++)
				listener.actionPerformed(evt);
		}

		// If repeat was true originally, clear it
		// Otherwise it might have been set by the action, etc

		// Check for grabAction being not null so that
		// repeat@5 copy-string-register copy-string-register@a
		// will work properly
		if(_repeat && grabAction == null)
		{
			repeat = false;
			repeatCount = 0;
		}
	}

	/**
	 * Returns the text area that fired the specified event.
	 * @param evt The event
	 */
	public static JEditTextArea getTextArea(EventObject evt)
	{
		if(evt != null)
		{
			Object o = evt.getSource();
			if(o instanceof Component)
			{
				// find the parent text area
				Component c = (Component)o;
				for(;;)
				{
					if(c instanceof JEditTextArea)
						return (JEditTextArea)c;
					else if(c == null)
						break;
					if(c instanceof JPopupMenu)
						c = ((JPopupMenu)c)
							.getInvoker();
					else
						c = c.getParent();
				}
			}
		}

		// this shouldn't happen
		System.err.println("BUG: getTextArea() returning null");
		System.err.println("Report this to Slava Pestov <sp@gjt.org>");
		return null;
	}

	// protected members

	/**
	 * If a key is being grabbed, this method should be called with
	 * the appropriate key event. It executes the grab action with
	 * the typed character as the parameter.
	 */
	protected void handleGrabAction(KeyEvent evt)
	{
		// Clear it *before* it is executed so that executeAction()
		// resets the repeat count
		ActionListener _grabAction = grabAction;
		grabAction = null;
		executeAction(_grabAction,evt.getSource(),
			String.valueOf(evt.getKeyChar()));
	}

	// protected members
	protected ActionListener grabAction;
	protected boolean repeat;
	protected int repeatCount;
	protected InputHandler.MacroRecorder recorder;

	/**
	 * If an action implements this interface, it should not be repeated.
	 * Instead, it will handle the repetition itself.
	 */
	public static interface NonRepeatable {}

	/**
	 * If an action implements this interface, it should not be recorded
	 * by the macro recorder. Instead, it will do its own recording.
	 */
	public static interface NonRecordable {}

	/**
	 * Macro recorder.
	 */
	public static interface MacroRecorder
	{
		public void actionPerformed(ActionListener listener,
			String actionCommand);
	}

	public static class backspace implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);

			if(!textArea.isEditable())
			{
				textArea.getToolkit().beep();
				return;
			}

			if(textArea.getSelectionStart()
			   != textArea.getSelectionEnd())
			{
				textArea.setSelectedText("");
			}
			else
			{
				int caret = textArea.getCaretPosition();
				if(caret == 0)
				{
					textArea.getToolkit().beep();
					return;
				}
				try
				{
					textArea.getDocument().remove(caret - 1,1);
				}
				catch(BadLocationException bl)
				{
					bl.printStackTrace();
				}
			}
		}
	}

	public static class delete implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);

			if(!textArea.isEditable())
			{
				textArea.getToolkit().beep();
				return;
			}

			if(textArea.getSelectionStart()
			   != textArea.getSelectionEnd())
			{
				textArea.setSelectedText("");
			}
			else
			{
				int caret = textArea.getCaretPosition();
				if(caret == textArea.getDocumentLength())
				{
					textArea.getToolkit().beep();
					return;
				}
				try
				{
					textArea.getDocument().remove(caret,1);
				}
				catch(BadLocationException bl)
				{
					bl.printStackTrace();
				}
			}
		}
	}

	public static class end implements ActionListener
	{
		private boolean select;

		public end(boolean select)
		{
			this.select = select;
		}

		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);

			int caret = textArea.getCaretPosition();

			int lastOfLine = textArea.getLineEndOffset(
				textArea.getCaretLine()) - 1;
			int lastVisibleLine = textArea.getFirstLine()
				+ textArea.getVisibleLines();
			if(lastVisibleLine >= textArea.getLineCount())
			{
				lastVisibleLine = Math.min(textArea.getLineCount() - 1,
					lastVisibleLine);
			}
			else
				lastVisibleLine -= (textArea.getElectricScroll() + 1);

			int lastVisible = textArea.getLineEndOffset(lastVisibleLine) - 1;
			int lastDocument = textArea.getDocumentLength();

			if(caret == lastDocument)
			{
				textArea.getToolkit().beep();
				return;
			}
			else if(caret == lastVisible)
				caret = lastDocument;
			else if(caret == lastOfLine)
				caret = lastVisible;
			else
				caret = lastOfLine;

			if(select)
				textArea.select(textArea.getMarkPosition(),caret);
			else
				textArea.setCaretPosition(caret);
		}
	}

	public static class home implements ActionListener
	{
		private boolean select;

		public home(boolean select)
		{
			this.select = select;
		}

		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);

			int caret = textArea.getCaretPosition();

			int firstLine = textArea.getFirstLine();

			int firstOfLine = textArea.getLineStartOffset(
				textArea.getCaretLine());
			int firstVisibleLine = (firstLine == 0 ? 0 :
				firstLine + textArea.getElectricScroll());
			int firstVisible = textArea.getLineStartOffset(
				firstVisibleLine);

			if(caret == 0)
			{
				textArea.getToolkit().beep();
				return;
			}
			else if(caret == firstVisible)
				caret = 0;
			else if(caret == firstOfLine)
				caret = firstVisible;
			else
				caret = firstOfLine;

			if(select)
				textArea.select(textArea.getMarkPosition(),caret);
			else
				textArea.setCaretPosition(caret);
		}
	}

	public static class insert_break implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);

			if(!textArea.isEditable())
			{
				textArea.getToolkit().beep();
				return;
			}

			textArea.setSelectedText("\n");
		}
	}

	public static class insert_tab implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);

			if(!textArea.isEditable())
			{
				textArea.getToolkit().beep();
				return;
			}

			textArea.overwriteSetSelectedText("\t");
		}
	}

	public static class next_char implements ActionListener
	{
		private boolean select;

		public next_char(boolean select)
		{
			this.select = select;
		}

		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			int caret = textArea.getCaretPosition();
			if(caret == textArea.getDocumentLength())
			{
				textArea.getToolkit().beep();
				return;
			}

			if(select)
				textArea.select(textArea.getMarkPosition(),
					caret + 1);
			else
				textArea.setCaretPosition(caret + 1);
		}
	}

	public static class next_line implements ActionListener
	{
		private boolean select;

		public next_line(boolean select)
		{
			this.select = select;
		}

		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			int caret = textArea.getCaretPosition();
			int line = textArea.getCaretLine();

			if(line == textArea.getLineCount() - 1)
			{
				textArea.getToolkit().beep();
				return;
			}

			int magic = textArea.getMagicCaretPosition();
			if(magic == -1)
			{
				magic = textArea.offsetToX(line,
					caret - textArea.getLineStartOffset(line));
			}

			caret = textArea.getLineStartOffset(line + 1)
				+ textArea.xToOffset(line + 1,magic);
			if(select)
				textArea.select(textArea.getMarkPosition(),caret);
			else
				textArea.setCaretPosition(caret);
			textArea.setMagicCaretPosition(magic);
		}
	}

	public static class next_page implements ActionListener
	{
		private boolean select;

		public next_page(boolean select)
		{
			this.select = select;
		}

		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			int lineCount = textArea.getLineCount();
			int firstLine = textArea.getFirstLine();
			int visibleLines = textArea.getVisibleLines();
			int line = textArea.getCaretLine();

			firstLine += visibleLines;

			if(firstLine + visibleLines >= lineCount - 1)
				firstLine = lineCount - visibleLines;

			textArea.setFirstLine(firstLine);

			int caret = textArea.getLineStartOffset(
				Math.min(textArea.getLineCount() - 1,
				line + visibleLines));
			if(select)
				textArea.select(textArea.getMarkPosition(),caret);
			else
				textArea.setCaretPosition(caret);
		}
	}

	public static class next_word implements ActionListener
	{
		private boolean select;

		public next_word(boolean select)
		{
			this.select = select;
		}

		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			int caret = textArea.getCaretPosition();
			int line = textArea.getCaretLine();
			int lineStart = textArea.getLineStartOffset(line);
			caret -= lineStart;

			String lineText = textArea.getLineText(textArea
				.getCaretLine());

			if(caret == lineText.length())
			{
				if(lineStart + caret == textArea.getDocumentLength())
				{
					textArea.getToolkit().beep();
					return;
				}
				caret++;
			}
			else
			{

				char ch = lineText.charAt(caret);

				String noWordSep = (String)textArea.getDocument()
					.getProperty("noWordSep");
				if(noWordSep == null)
					noWordSep = "";
				boolean selectNoLetter = (!Character
					.isLetterOrDigit(ch)
					&& noWordSep.indexOf(ch) == -1);

				int wordEnd = lineText.length();
				for(int i = caret; i < lineText.length(); i++)
				{
					ch = lineText.charAt(i);
					if(selectNoLetter ^ (!Character
						.isLetterOrDigit(ch) &&
						noWordSep.indexOf(ch) == -1))
					{
						wordEnd = i;
						break;
					}
				}
				caret = wordEnd;
			}

			if(select)
				textArea.select(textArea.getMarkPosition(),
					lineStart + caret);
			else
				textArea.setCaretPosition(lineStart + caret);
		}
	}

	public static class overwrite implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			textArea.setOverwriteEnabled(
				!textArea.isOverwriteEnabled());
		}
	}

	public static class prev_char implements ActionListener
	{
		private boolean select;

		public prev_char(boolean select)
		{
			this.select = select;
		}

		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			int caret = textArea.getCaretPosition();
			if(caret == 0)
			{
				textArea.getToolkit().beep();
				return;
			}

			if(select)
				textArea.select(textArea.getMarkPosition(),
					caret - 1);
			else
				textArea.setCaretPosition(caret - 1);
		}
	}

	public static class prev_line implements ActionListener
	{
		private boolean select;

		public prev_line(boolean select)
		{
			this.select = select;
		}

		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			int caret = textArea.getCaretPosition();
			int line = textArea.getCaretLine();

			if(line == 0)
			{
				textArea.getToolkit().beep();
				return;
			}

			int magic = textArea.getMagicCaretPosition();
			if(magic == -1)
			{
				magic = textArea.offsetToX(line,
					caret - textArea.getLineStartOffset(line));
			}

			caret = textArea.getLineStartOffset(line - 1)
				+ textArea.xToOffset(line - 1,magic);
			if(select)
				textArea.select(textArea.getMarkPosition(),caret);
			else
				textArea.setCaretPosition(caret);
			textArea.setMagicCaretPosition(magic);
		}
	}

	public static class prev_page implements ActionListener
	{
		private boolean select;

		public prev_page(boolean select)
		{
			this.select = select;
		}

		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			int firstLine = textArea.getFirstLine();
			int visibleLines = textArea.getVisibleLines();
			int line = textArea.getCaretLine();

			if(firstLine < visibleLines)
				firstLine = visibleLines;

			textArea.setFirstLine(firstLine - visibleLines);

			int caret = textArea.getLineStartOffset(
				Math.max(0,line - visibleLines));
			if(select)
				textArea.select(textArea.getMarkPosition(),caret);
			else
				textArea.setCaretPosition(caret);
		}
	}

	public static class prev_word implements ActionListener
	{
		private boolean select;

		public prev_word(boolean select)
		{
			this.select = select;
		}

		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			int caret = textArea.getCaretPosition();
			int line = textArea.getCaretLine();
			int lineStart = textArea.getLineStartOffset(line);
			caret -= lineStart;

			String lineText = textArea.getLineText(textArea
				.getCaretLine());

			if(caret == 0)
			{
				if(lineStart == 0)
				{
					textArea.getToolkit().beep();
					return;
				}
				caret--;
			}
			else
			{
				char ch = lineText.charAt(caret - 1);

				String noWordSep = (String)textArea.getDocument()
					.getProperty("noWordSep");
				if(noWordSep == null)
					noWordSep = "";
				boolean selectNoLetter = (!Character
					.isLetterOrDigit(ch)
					&& noWordSep.indexOf(ch) == -1);

				int wordStart = 0;
				for(int i = caret - 1; i >= 0; i--)
				{
					ch = lineText.charAt(i);
					if(selectNoLetter ^ (!Character
						.isLetterOrDigit(ch) &&
						noWordSep.indexOf(ch) == -1))
					{
						wordStart = i + 1;
						break;
					}
				}
				caret = wordStart;
			}

			if(select)
				textArea.select(textArea.getMarkPosition(),
					lineStart + caret);
			else
				textArea.setCaretPosition(lineStart + caret);
		}
	}

	public static class repeat implements ActionListener,
		InputHandler.NonRecordable
	{
		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			textArea.getInputHandler().setRepeatEnabled(true);
			String actionCommand = evt.getActionCommand();
			if(actionCommand != null)
			{
				textArea.getInputHandler().setRepeatCount(
					Integer.parseInt(actionCommand));
			}
		}
	}

	public static class toggle_rect implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			textArea.setSelectionRectangular(
				!textArea.isSelectionRectangular());
		}
	}

	public static class insert_char implements ActionListener,
		InputHandler.NonRepeatable
	{
		public void actionPerformed(ActionEvent evt)
		{
			JEditTextArea textArea = getTextArea(evt);
			String str = evt.getActionCommand();
			int repeatCount = textArea.getInputHandler().getRepeatCount();

			if(textArea.isEditable())
			{
				StringBuffer buf = new StringBuffer();
				for(int i = 0; i < repeatCount; i++)
					buf.append(str);
				textArea.overwriteSetSelectedText(buf.toString());
			}
			else
			{
				textArea.getToolkit().beep();
			}
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.6  1999/10/24 02:06:41  sp
 * Miscallaneous pre1 stuff
 *
 * Revision 1.5  1999/10/05 10:55:29  sp
 * File dialogs open faster, and experimental keyboard macros
 *
 */
