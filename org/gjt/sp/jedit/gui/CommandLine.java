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
import org.gjt.sp.jedit.search.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class CommandLine extends JPanel
{
	public static final int NULL_STATE = 0;
	public static final int TOPLEVEL_STATE = 1;
	public static final int REPEAT_STATE = 2;
	public static final int PROMPT_ONE_CHAR_STATE = 3;
	public static final int PROMPT_LINE_STATE = 4;
	public static final int QUICK_SEARCH_STATE = 5;
	public static final int INCREMENTAL_SEARCH_STATE = 6;

	public CommandLine(View view)
	{
		super(new BorderLayout());

		this.view = view;

		add(BorderLayout.CENTER,textField = new CLITextField());
		textField.setFont(new Font("Dialog",Font.PLAIN,10));

		completionTimer = new Timer(0,new UpdateCompletions());
		completionTimer.setRepeats(false);

		Font font = new Font("Dialog",Font.BOLD,10);
		Insets margin = new Insets(0,0,0,0);
		ActionHandler actionHandler = new ActionHandler();
		ignoreCase = new JCheckBox(jEdit.getProperty("search.ignoreCase"));
		ignoreCase.setMnemonic(jEdit.getProperty("search.ignoreCase.mnemonic")
			.charAt(0));
		ignoreCase.setFont(font);
		ignoreCase.setMargin(margin);
		ignoreCase.setRequestFocusEnabled(false);
		ignoreCase.addActionListener(actionHandler);
		regexp = new JCheckBox(jEdit.getProperty("search.regexp"));
		regexp.setMnemonic(jEdit.getProperty("search.regexp.mnemonic")
			.charAt(0));
		regexp.setFont(font);
		regexp.setMargin(margin);
		regexp.setRequestFocusEnabled(false);
		regexp.addActionListener(actionHandler);

		searchSettings = new JPanel();
		searchSettings.setLayout(new BoxLayout(searchSettings,
			BoxLayout.X_AXIS));
		searchSettings.add(Box.createHorizontalStrut(6));
		searchSettings.add(ignoreCase);
		searchSettings.add(Box.createHorizontalStrut(6));
		searchSettings.add(regexp);

		updateSearchSettings();
	}

	public int getState()
	{
		return state;
	}

	public void setState(int state)
	{
		if(this.state == state)
			return;

		this.state = state;

		textField.setText(null);
		completionTimer.stop();
		hideCompletionWindow();
		remove(searchSettings);

		if(state == NULL_STATE)
		{
			view.showStatus(null);
			textField.setModel("cli");
		}
		else if(state == TOPLEVEL_STATE)
		{
			view.showStatus(jEdit.getProperty(
				"view.command-line.top-level"));
			textField.setModel("cli");
		}
		else if(state == REPEAT_STATE)
		{
			view.showStatus(jEdit.getProperty(
				"view.command-line.repeat"));
			textField.setModel(null);
		}
		else if(state == PROMPT_LINE_STATE)
			textField.setModel("cli.prompt");
		else if(state == QUICK_SEARCH_STATE)
		{
			view.showStatus(jEdit.getProperty(
				"view.status.quick-search"));
			textField.setModel("find");

			add(BorderLayout.EAST,searchSettings);

			Dimension dim = searchSettings.getPreferredSize();
			dim.height = textField.getHeight();
			searchSettings.setPreferredSize(dim);

			revalidate();

			textField.requestFocus();
		}
		else if(state == INCREMENTAL_SEARCH_STATE)
		{
			view.showStatus(jEdit.getProperty(
				"view.status.incremental-search"));
			textField.setModel("find");

			add(BorderLayout.EAST,searchSettings);

			Dimension dim = searchSettings.getPreferredSize();
			dim.height = textField.getHeight();
			searchSettings.setPreferredSize(dim);

			revalidate();

			textField.requestFocus();
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

	public void updateSearchSettings()
	{
		ignoreCase.setSelected(SearchAndReplace.getIgnoreCase());
		regexp.setSelected(SearchAndReplace.getRegexp());
	}

	// private members
	private View view;
	private CLITextField textField;
	private CompletionWindow window;
	private EditAction[] actions;
	private Vector completions;
	private Timer completionTimer;

	private int state;

	private EditAction promptAction;

	private JPanel searchSettings;
	private JCheckBox ignoreCase, regexp;

	private int savedRepeatCount;

	private void getCompletions(String text)
	{
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
	}

	private void updateCompletions()
	{
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
		if(state != TOPLEVEL_STATE)
			return;

		try
		{
			final String text = textField.getDocument().getText(0,
				textField.getSelectionStart());
			if(text.length() == 0)
			{
				hideCompletionWindow();
				return;
			}

			getCompletions(text);

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

		getCompletions(actionName);
		EditAction action;
		if(completions.size() != 0)
		{
			actionName = (String)completions.elementAt(0);
			action = jEdit.getAction(actionName);
		}
		else
			action = null;

		setState(NULL_STATE);
		view.getEditPane().focusOnTextArea();

		if(action == null)
		{
			String[] args = { actionName };
			GUIUtilities.error(view,"unknown-action",args);
		}
		else
			view.getInputHandler().executeAction(action,this,null);
	}

	private void doQuickSearch()
	{
		String text = textField.getText();
		if(text.length() != 0)
		{
			textField.addCurrentToHistory();
			textField.setText(null);
			SearchAndReplace.setSearchFileSet(new CurrentBufferSet());
			SearchAndReplace.setSearchString(text);
			SearchAndReplace.find(view,view);
		}

		view.getEditPane().focusOnTextArea();
	}

	private void doIncrementalSearch(int start)
	{
		String text = textField.getText();
		if(text.length() != 0)
		{
			textField.addCurrentToHistory();
			SearchAndReplace.setSearchString(text);

			try
			{
				if(SearchAndReplace.find(view,view.getBuffer(),start))
					return;
			}
			catch(BadLocationException bl)
			{
				Log.log(Log.ERROR,this,bl);
			}
			catch(Exception ia)
			{
				// invalid regexp, ignore
			}

			getToolkit().beep();
		}
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(evt.getSource() == ignoreCase)
			{
				SearchAndReplace.setIgnoreCase(ignoreCase
					.isSelected());
			}
			else if(evt.getSource() == regexp)
			{
				SearchAndReplace.setRegexp(regexp
					.isSelected());
			}
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

		public void processKeyEvent(KeyEvent evt)
		{
			if(state == NULL_STATE)
				return;

			evt = KeyEventWorkaround.processKeyEvent(evt);
			if(evt == null)
				return;

			int modifiers = evt.getModifiers();

			switch(evt.getID())
			{
			case KeyEvent.KEY_TYPED:
				char ch = evt.getKeyChar();

				if(state == TOPLEVEL_STATE)
				{
					if(getText().length() == 0 && Character.isDigit(ch))
						handleDigit(evt);
					else
						super.processKeyEvent(evt);
				}
				else if(state == REPEAT_STATE)
				{
					if(Character.isDigit(ch))
						handleDigit(evt);
					else
					{
						setState(NULL_STATE);
						view.getEditPane().focusOnTextArea();
						view.getInputHandler().keyTyped(evt);
					}
				}
				else if(state == PROMPT_ONE_CHAR_STATE)
				{
					evt.consume();
					handlePromptOneChar(evt);
				}
				else if(state == PROMPT_LINE_STATE
					|| state == QUICK_SEARCH_STATE
					|| state == INCREMENTAL_SEARCH_STATE)
					super.processKeyEvent(evt);
				break;
			case KeyEvent.KEY_PRESSED:
				int keyCode = evt.getKeyCode();

				if((modifiers & (~ (InputEvent.SHIFT_MASK
					| KeyEventWorkaround.ALT_GRAPH_MASK))) == 0
					&& !evt.isActionKey()
					&& keyCode != KeyEvent.VK_BACK_SPACE
					&& keyCode != KeyEvent.VK_DELETE
					&& keyCode != KeyEvent.VK_ENTER
					&& keyCode != KeyEvent.VK_TAB
					&& keyCode != KeyEvent.VK_ESCAPE)
					return;

				if(modifiers == 0 && keyCode == KeyEvent.VK_ESCAPE)
				{
					view.getInputHandler().setRepeatCount(1);
					setState(NULL_STATE);
					view.getEditPane().focusOnTextArea();
					evt.consume();
					break;
				}
				else if(state == TOPLEVEL_STATE)
				{
					if(modifiers == 0 && keyCode == KeyEvent.VK_UP)
						handleTopLevelUp(evt);
					else if(modifiers == 0 && keyCode == KeyEvent.VK_DOWN)
						handleTopLevelDown(evt);
					else if(modifiers == 0
						&& (keyCode == KeyEvent.VK_ENTER
						|| keyCode == KeyEvent.VK_TAB))
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
				else if(state == QUICK_SEARCH_STATE)
				{
					if(modifiers == 0 &&
						keyCode == KeyEvent.VK_ENTER)
					{
						evt.consume();
						doQuickSearch();
					}
					else
						super.processKeyEvent(evt);
					break;
				}
				else if(state == INCREMENTAL_SEARCH_STATE)
				{
					if(modifiers == 0 &&
						keyCode == KeyEvent.VK_ENTER)
					{
						evt.consume();
						doIncrementalSearch(view.getTextArea()
							.getSelectionEnd());
					}
					else
						super.processKeyEvent(evt);
					break;
				}
				else if(state == REPEAT_STATE)
				{
					setState(NULL_STATE);
					view.getEditPane().focusOnTextArea();
					view.getInputHandler().keyPressed(evt);
				}
			}
		}

		void handleDigit(KeyEvent evt)
		{
			// in case we're in TOPLEVEL
			setState(REPEAT_STATE);

			int repeatCount;
			InputHandler inputHandler = view.getInputHandler();
			if(inputHandler.isRepeatEnabled())
				repeatCount = inputHandler.getRepeatCount();
			else
				repeatCount = 0;

			repeatCount *= 10;
			repeatCount += (evt.getKeyChar() - '0');

			inputHandler.setRepeatEnabled(true);
			inputHandler.setRepeatCount(repeatCount);

			// insert number into text field
			super.processKeyEvent(evt);
		}

		void handleTopLevelUp(KeyEvent evt)
		{
			if(window.isShowing())
			{
				int selected = window.list.getSelectedIndex();
				if(selected == 0)
					selected = window.list.getModel().getSize() - 1;
				else
					selected = selected - 1;

				window.list.setSelectedIndex(selected);
				window.list.ensureIndexIsVisible(selected);
	
				evt.consume();
			}
			else
				super.processKeyEvent(evt);
		}

		void handleTopLevelDown(KeyEvent evt)
		{
			if(window.isShowing())
			{
				int selected = window.list.getSelectedIndex();
				if(selected == window.list.getModel().getSize() - 1)
					selected = 0;
				else
					selected = selected + 1;

				window.list.setSelectedIndex(selected);
				window.list.ensureIndexIsVisible(selected);

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
			view.getEditPane().focusOnTextArea();

			view.getInputHandler().executeAction(_promptAction,
				this,arg);
		}

		void handlePromptLine()
		{
			EditAction _promptAction = promptAction;
			String text = textField.getText();

			textField.addCurrentToHistory();
			setState(NULL_STATE);
			view.getEditPane().focusOnTextArea();

			view.getInputHandler().executeAction(_promptAction,
				this,text);
		}

		class DocumentHandler implements DocumentListener
		{
			public void changedUpdate(DocumentEvent evt) {}

			public void insertUpdate(DocumentEvent evt)
			{
				if(state == TOPLEVEL_STATE)
					updateCompletions();
				else if(state == INCREMENTAL_SEARCH_STATE)
				{
					doIncrementalSearch(view.getTextArea()
						.getSelectionStart());
				}
			}

			public void removeUpdate(DocumentEvent evt)
			{
				if(state == TOPLEVEL_STATE)
					updateCompletions();
				else if(state == INCREMENTAL_SEARCH_STATE)
				{
					String text = textField.getText();
					if(text.length() != 0)
						doIncrementalSearch(0);
				}
			}
		}

		class FocusHandler implements FocusListener
		{
			public void focusGained(FocusEvent evt)
			{
				if(state == NULL_STATE)
					setState(TOPLEVEL_STATE);
			}

			public void focusLost(FocusEvent evt)
			{
				if(state == TOPLEVEL_STATE && window == null)
					setState(NULL_STATE);
				else if(state == INCREMENTAL_SEARCH_STATE
					|| state == QUICK_SEARCH_STATE)
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
			list.addKeyListener(new KeyHandler());
			view.setKeyEventInterceptor(new KeyHandler());
			list.addMouseListener(new MouseHandler());

			// stupid scrollbar policy is an attempt to work around
			// bugs people have been seeing with IBM's JDK -- 7 Sep 2000
			JScrollPane scroller = new JScrollPane(list,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			getContentPane().add(BorderLayout.CENTER,scroller);

			setListData(items);

			CompletionWindow.this.show();
		}

		public void dispose()
		{
			view.setKeyEventInterceptor(null);
			super.dispose();
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

		class KeyHandler extends KeyAdapter
		{
			public void keyTyped(KeyEvent evt)
			{
				textField.requestFocus();
				textField.processKeyEvent(evt);
			}

			public void keyPressed(KeyEvent evt)
			{
				textField.requestFocus();

				if(evt.getKeyCode() == KeyEvent.VK_ESCAPE)
					hideCompletionWindow();
				else if(evt.getKeyCode() == KeyEvent.VK_ENTER
					|| evt.getKeyCode() == KeyEvent.VK_TAB)
					executeAction((String)list.getSelectedValue());
				else
					textField.processKeyEvent(evt);
			}
		}

		class MouseHandler extends MouseAdapter
		{
			public void mouseClicked(MouseEvent evt)
			{
				executeAction((String)list.getSelectedValue());
			}
		}
	}
}

/*
 * Change Log:
 * $Log$
 * Revision 1.10  2000/10/28 00:36:58  sp
 * ML mode, Haskell mode
 *
 * Revision 1.9  2000/10/15 04:10:34  sp
 * bug fixes
 *
 * Revision 1.8  2000/10/05 04:30:10  sp
 * *** empty log message ***
 *
 * Revision 1.7  2000/09/30 01:17:00  sp
 * *** empty log message ***
 *
 * Revision 1.6  2000/09/26 10:19:46  sp
 * Bug fixes, spit and polish
 *
 * Revision 1.5  2000/09/23 03:01:10  sp
 * pre7 yayayay
 *
 * Revision 1.4  2000/09/07 04:46:08  sp
 * bug fixes
 *
 * Revision 1.3  2000/09/06 04:39:47  sp
 * bug fixes
 *
 * Revision 1.2  2000/09/03 03:16:53  sp
 * Search bar integrated with command line, enhancements throughout
 *
 * Revision 1.1  2000/09/01 11:31:00  sp
 * Rudimentary 'command line', similar to emacs minibuf
 *
 */
