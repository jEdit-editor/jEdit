/*
 * CommandLine.java - Command line
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

import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.text.BadLocationException;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.Vector;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.*;

public class CommandLine extends JPanel
{
	public static final int NULL_STATE = 0;
	public static final int TOPLEVEL_STATE = 1;
	public static final int REPEAT_STATE = 2;
	public static final int PROMPT_ONE_CHAR_STATE = 3;
	public static final int PROMPT_LINE_STATE = 4;

	public CommandLine(View view)
	{
		super(new BorderLayout());

		this.view = view;

		label = new JLabel();
		label.setFont(new Font("Dialog",Font.BOLD,10));
		label.setForeground(UIManager.getColor("Button.foreground"));
		label.setBorder(new EmptyBorder(0,0,0,6));

		add(BorderLayout.CENTER,textField = new CLITextField());
		textField.setFont(new Font("Dialog",Font.PLAIN,10));

		completionTimer = new Timer(0,new UpdateCompletions());
		completionTimer.setRepeats(false);
	}

	public void setState(int state)
	{
		if(this.state == state)
			return;

		this.state = state;

		if(state == NULL_STATE)
		{
			view.showStatus(null);
			textField.setModel("cli");
			// must return focus to text area after a repeat
			view.getEditPane().focusOnTextArea();
			reset();
		}
		else if(state == TOPLEVEL_STATE)
		{
			view.showStatus(jEdit.getProperty(
				"view.command-line.top-level"));
			textField.setModel("cli");
			reset();
		}
		else if(state == REPEAT_STATE)
		{
			view.showStatus(jEdit.getProperty(
				"view.command-line.repeat"));
			textField.setModel(null);
			reset();
		}
		else if(state == PROMPT_ONE_CHAR_STATE)
			reset();
		else if(state == PROMPT_LINE_STATE)
		{
			textField.setModel("cli.prompt");
			reset();
		}
	}

	public JTextField getTextField()
	{
		return textField;
	}

	/**
	 * Prompts the user to enter one character at the command line.
	 * @param prompt The prompt string
	 * @param promptAction This action will be executed with the
	 * character as the action command when the user enters it
	 * @since jEdit 2.6pre5
	 */
	public void promptOneChar(String prompt, EditAction promptAction)
	{
		view.showStatus(prompt);
		this.promptAction = promptAction;
		setState(PROMPT_ONE_CHAR_STATE);
		textField.requestFocus();
	}

	/**
	 * Prompts the user to enter one line of text at the command line.
	 * @param prompt The prompt string
	 * @param promptAction This action will be executed with the
	 * text as the action command when the user presses ENTER
	 * @since jEdit 2.6pre5
	 */
	public void promptLine(String prompt, EditAction promptAction)
	{
		view.showStatus(prompt);
		this.promptAction = promptAction;
		setState(PROMPT_LINE_STATE);
		textField.requestFocus();
	}

	// private members
	private View view;
	private JLabel label;
	private CLITextField textField;
	private CompletionWindow window;
	private EditAction[] actions;
	private Vector completions;
	private Timer completionTimer;

	private int state;

	private EditAction promptAction;

	private void reset()
	{
		remove(label);
		textField.setText(null);
		completionTimer.stop();
		hideCompletionWindow();
	}

	private void updateCompletions()
	{
		if(state != TOPLEVEL_STATE)
			return;

		if(window != null)
		{
			// if window is already visible, update them
			// immediately
			completionTimer.stop();
			completionTimer.setInitialDelay(0);
		}
		else
		{
			// don't show window if user is typing
			completionTimer.stop();
			completionTimer.setInitialDelay(300);
		}

		completionTimer.start();
	}

	private void updateCompletionsSafely()
	{
		try
		{
			final String text = textField.getDocument().getText(0,
				textField.getSelectionStart());
			if(text.length() == 0)
			{
				hideCompletionWindow();
				return;
			}

			if(actions == null)
			{
				actions = jEdit.getActions();
				MiscUtilities.quicksort(actions,new ActionCompare());
				completions = new Vector(actions.length);
			}
			else
				completions.removeAllElements();

			for(int i = 0; i < actions.length; i++)
			{
				EditAction action = actions[i];
				String name = action.getName();
				if(!action.needsActionCommand() && name.startsWith(text))
					completions.addElement(name);
			}

			if(completions.size() == 0)
				hideCompletionWindow();
			else
				showCompletionWindow(completions);
		}
		catch(BadLocationException bl)
		{
			Log.log(Log.ERROR,this,bl);
		}
	}

	class ActionCompare implements MiscUtilities.Compare
	{
		public int compare(Object obj1, Object obj2)
		{
			EditAction a1 = (EditAction)obj1;
			EditAction a2 = (EditAction)obj2;
			return a1.getName().compareTo(a2.getName());
		}
	}

	class UpdateCompletions implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			updateCompletionsSafely();
		}
	}

	private void hideCompletionWindow()
	{
		if(window != null)
		{
			window.dispose();
			window = null;
		}
	}

	private void showCompletionWindow(Vector completions)
	{
		if(window != null)
		{
			window.setListData(completions);
			window.requestFocus();
			window.toFront();
		}
		else
			window = new CompletionWindow(completions);
	}

	private void executeAction(String actionName)
	{
		textField.addCurrentToHistory();

		EditAction action = jEdit.getAction(actionName);
		if(action == null)
		{
			String[] args = { actionName };
			GUIUtilities.error(view,"unknown-action",args);
			view.getEditPane().focusOnTextArea();
		}
		else
		{
			view.getEditPane().focusOnTextArea();
			view.getInputHandler().executeAction(action,this,null);
		}
	}

	class CLITextField extends HistoryTextField
	{
		CLITextField()
		{
			super("cli");
			CLITextField.this.addFocusListener(new FocusHandler());
			getDocument().addDocumentListener(new DocumentHandler());
		}

		protected void processKeyEvent(KeyEvent evt)
		{
			if(state == NULL_STATE)
				return;

			int modifiers = evt.getModifiers();

			switch(evt.getID())
			{
			case KeyEvent.KEY_TYPED:
				char ch = evt.getKeyChar();
				if(ch == KeyEvent.CHAR_UNDEFINED ||
					ch < 0x20 || ch == 0x7f
					|| (modifiers & KeyEvent.ALT_MASK) != 0)
					return;

				if(state == TOPLEVEL_STATE)
				{
					if(getText().length() == 0 && Character.isDigit(ch))
						handleDigit(evt);
					else if(ch == ' ')
						handleTopLevelEnter(evt);
					else
						super.processKeyEvent(evt);
				}
				else if(state == REPEAT_STATE)
				{
					if(Character.isDigit(ch))
						handleDigit(evt);
					else
						view.processKeyEvent(evt);
				}
				else if(state == PROMPT_ONE_CHAR_STATE)
				{
					evt.consume();
					handlePromptOneChar(evt);
				}
				else if(state == PROMPT_LINE_STATE)
					super.processKeyEvent(evt);
				break;
			case KeyEvent.KEY_PRESSED:
				int keyCode = evt.getKeyCode();
				if(modifiers == 0 && keyCode == KeyEvent.VK_ESCAPE)
				{
					setState(NULL_STATE);
					evt.consume();
					break;
				}
				else if(state == TOPLEVEL_STATE)
				{
					if(modifiers == 0 && keyCode == KeyEvent.VK_UP)
						handleTopLevelUp(evt);
					else if(modifiers == 0 && keyCode == KeyEvent.VK_DOWN)
						handleTopLevelDown(evt);
					else if(modifiers == 0 && keyCode == KeyEvent.VK_ENTER)
						handleTopLevelEnter(evt);
					else
						super.processKeyEvent(evt);
					break;
				}
				else if(state == PROMPT_ONE_CHAR_STATE)
				{
					if(modifiers == 0 &&
						(keyCode == KeyEvent.VK_ENTER
						|| keyCode == KeyEvent.VK_TAB))
					{
						evt.consume();
						handlePromptOneChar(evt);
					}
					break;
				}
				else if(state == PROMPT_LINE_STATE)
				{
					if(modifiers == 0 &&
						keyCode == KeyEvent.VK_ENTER)
					{
						evt.consume();
						handlePromptLine();
					}
					else
						super.processKeyEvent(evt);
					break;
				}
				// fall through if state is REPEAT_STATE
			case KeyEvent.KEY_RELEASED:
				if(state == REPEAT_STATE)
					view.processKeyEvent(evt);
				break;
			}
		}

		void handleDigit(KeyEvent evt)
		{
			InputHandler input = view.getInputHandler();
			input.setRepeatEnabled(true);
			int repeatCount = input.getRepeatCount();
			repeatCount *= 10;
			repeatCount += (evt.getKeyChar() - '0');
			input.setRepeatCount(repeatCount);

			// in case we're in TOPLEVEL
			setState(REPEAT_STATE);

			// insert number into text field
			super.processKeyEvent(evt);
		}

		void handleTopLevelUp(KeyEvent evt)
		{
			if(window.isShowing())
			{
				int selected = window.list.getSelectedIndex();
				if(selected != 1)
				{
					window.list.setSelectedIndex(selected - 1);
					window.list.ensureIndexIsVisible(selected - 1);
				}
				evt.consume();
			}
			else
				super.processKeyEvent(evt);
		}

		void handleTopLevelDown(KeyEvent evt)
		{
			if(window.isShowing())
			{
				int total = window.list.getModel().getSize();
				int selected = window.list.getSelectedIndex();
				if(selected != total - 1)
				{
					window.list.setSelectedIndex(selected + 1);
					window.list.ensureIndexIsVisible(selected + 1);
				}
				evt.consume();
			}
			else
				super.processKeyEvent(evt);
		}

		void handleTopLevelEnter(KeyEvent evt)
		{
			String action;
			if(window != null && window.isShowing())
				action = (String)window.list.getSelectedValue();
			else
				action = getText();
			if(action.length() != 0)
				executeAction(action);
			evt.consume();
		}

		void handlePromptOneChar(KeyEvent evt)
		{
			char ch = evt.getKeyChar();
			String arg = String.valueOf(ch);
			EditAction _promptAction = promptAction;
			setState(NULL_STATE);

			view.getInputHandler().executeAction(_promptAction,
				this,arg);
		}

		void handlePromptLine()
		{
			EditAction _promptAction = promptAction;
			String text = textField.getText();

			textField.addCurrentToHistory();
			setState(NULL_STATE);

			view.getInputHandler().executeAction(_promptAction,
				this,text);
		}

		class DocumentHandler implements DocumentListener
		{
			public void changedUpdate(DocumentEvent evt) {}

			public void insertUpdate(DocumentEvent evt)
			{
				updateCompletions();
			}

			public void removeUpdate(DocumentEvent evt)
			{
				updateCompletions();
			}
		}

		class FocusHandler implements FocusListener
		{
			public void focusGained(FocusEvent evt)
			{
				if(state != REPEAT_STATE
					&& state != PROMPT_ONE_CHAR_STATE
					&& state != PROMPT_LINE_STATE)
					setState(TOPLEVEL_STATE);
			}

			public void focusLost(FocusEvent evt)
			{
				if(state != REPEAT_STATE
					&& state != PROMPT_ONE_CHAR_STATE
					&& state != PROMPT_LINE_STATE)
					setState(NULL_STATE);
			}
		}
	}

	class CompletionWindow extends JWindow
	{
		JList list;

		CompletionWindow(Vector items)
		{
			super(view);

			list = new JList();
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			getContentPane().add(BorderLayout.CENTER,new JScrollPane(list));

			setListData(items);

			CompletionWindow.this.show();
		}

		void setListData(Vector items)
		{
			list.setListData(items);
			list.setVisibleRowCount(Math.min(8,items.size()));
			list.setSelectedIndex(0);

			pack();
			Point loc = new Point(0,-CompletionWindow.this.getSize().height);
			SwingUtilities.convertPointToScreen(loc,textField);
			CompletionWindow.this.setLocation(loc);
		}
	}
}

/*
 * Change Log:
 * $Log$
 * Revision 1.1  2000/09/01 11:31:00  sp
 * Rudimentary 'command line', similar to emacs minibuf
 *
 */
