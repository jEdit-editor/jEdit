/*
 * Copyright (C) 2000, 2001 Slava Pestov
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
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

package org.gjt.sp.jedit.options;

//{{{ Imports
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.StringTokenizer;
import javax.swing.AbstractListModel;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.gui.RolloverButton;
import org.gjt.sp.jedit.jEdit;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static javax.swing.Box.createGlue;
import static javax.swing.Box.createHorizontalStrut;
import static javax.swing.BoxLayout.X_AXIS;
import static javax.swing.ListSelectionModel.SINGLE_SELECTION;
import static org.gjt.sp.jedit.GUIUtilities.error;
import static org.gjt.sp.jedit.GUIUtilities.loadIcon;
import static org.gjt.sp.jedit.MiscUtilities.getLastSeparatorIndex;
import static org.gjt.sp.jedit.MiscUtilities.isUncPath;
// }}}

/**
 * Additional UNC roots editor.
 *
 * @author Björn Kautler
 * @since jEdit 5.8pre1
 */
public class BrowserAdditionalUncRootsOptionPane extends AbstractOptionPane
{
	//{{{ BrowserAdditionalUncRootsOptionPane constructor
	public BrowserAdditionalUncRootsOptionPane()
	{
		super("browser.additional-unc-roots");
		caption = new JLabel(jEdit.getProperty("options.browser.additional-unc-roots.caption"));
	} // }}}

	//{{{ _init method
	@Override
	protected void _init()
	{
		setLayout(new BorderLayout());

		add(NORTH, caption);

		listData = new ArrayList<>();
		listModel = new SortedListModel();
		reloadUncRootsList(jEdit.getProperty("vfs.browser.additionalUncRoots"));

		list = new JList<>(listModel);
		list.setSelectionMode(SINGLE_SELECTION);
		list.addListSelectionListener(__ -> updateButtons());

		add(CENTER, new JScrollPane(list));

		JPanel buttons = new JPanel();
		buttons.setBorder(new EmptyBorder(3, 0, 0, 0));
		buttons.setLayout(new BoxLayout(buttons, X_AXIS));
		ActionHandler actionHandler = new ActionHandler();
		add = new RolloverButton(loadIcon(jEdit.getProperty("common.add.icon")));
		add.setToolTipText(jEdit.getProperty("common.add"));
		add.addActionListener(actionHandler);
		buttons.add(add);
		buttons.add(createHorizontalStrut(6));
		remove = new RolloverButton(loadIcon(jEdit.getProperty("common.remove.icon")));
		remove.setToolTipText(jEdit.getProperty("common.remove"));
		remove.addActionListener(actionHandler);
		buttons.add(remove);
		buttons.add(createGlue());

		// add "reset to defaults" button
		reset = new RolloverButton(loadIcon(jEdit.getProperty("common.clearAll.icon")));
		reset.setToolTipText(jEdit.getProperty("common.reset"));
		reset.addActionListener(actionHandler);
		buttons.add(reset);

		updateButtons();
		add(BorderLayout.SOUTH, buttons);
	} // }}}

	//{{{ _save method
	@Override
	protected void _save()
	{
		StringJoiner joiner = new StringJoiner(" ");
		for(String uncRoot : listData)
			joiner.add(uncRoot);
		jEdit.setProperty("vfs.browser.additionalUncRoots", joiner.toString());
	} // }}}

	//{{{ Private members
	private List<String> listData;
	private SortedListModel listModel;
	private JList<String> list;
	private JButton add;
	private JButton remove;
	private JButton reset;
	private final JLabel caption;
	// }}}

	//{{{ updateButtons method
	private void updateButtons()
	{
		int index = list.getSelectedIndex();
		remove.setEnabled((index != -1) && !listData.isEmpty());
	} // }}}

	//{{{ reloadUncRootsList method
	private void reloadUncRootsList(String uncRoots)
	{
		listData.clear();
		StringTokenizer st = new StringTokenizer(uncRoots);
		while(st.hasMoreTokens())
		{
			String uncRoot = st.nextToken();
			if(!listData.contains(uncRoot))
				listData.add(uncRoot);
		}
		Collections.sort(listData);
		listModel.fireContentsChanged(listModel, 0, listData.size() - 1);
	} // }}}

	//{{{ SortedListModel class
	private class SortedListModel extends AbstractListModel<String>
	{
		@Override
		public int getSize()
		{
			return listData.size();
		}

		@Override
		public String getElementAt(int index)
		{
			return listData.get(index);
		}

		@Override
		protected void fireContentsChanged(Object source, int index0, int index1)
		{
			super.fireContentsChanged(source, index0, index1);
		}
	} // }}}

	//{{{ ActionHandler class
	private class ActionHandler implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();

			if(source == add)
			{
				String userInput = GUIUtilities.input(BrowserAdditionalUncRootsOptionPane.this, "options.browser.additional-unc-roots.add.dialog", null);
				while(userInput != null)
				{
					int lastSeparatorIndex = getLastSeparatorIndex(userInput);
					if(((isUncPath(userInput) && (lastSeparatorIndex == 1))
								|| (!userInput.isBlank() && (lastSeparatorIndex == -1)))
							&& (!userInput.contains(" ")))
					{
						String uncRoot = "\\\\" + userInput.substring(lastSeparatorIndex + 1);
						if(!listData.contains(uncRoot))
						{
							String selectedValue = list.getSelectedValue();
							listData.add(uncRoot);
							Collections.sort(listData);
							listModel.fireContentsChanged(listModel, 0, listData.size() - 1);
							list.setSelectedValue(selectedValue, true);
						}
						break;
					}
					else
					{
						error(BrowserAdditionalUncRootsOptionPane.this, "options.browser.additional-unc-roots.format.error", null);
						userInput = GUIUtilities.input(BrowserAdditionalUncRootsOptionPane.this, "options.browser.additional-unc-roots.add.dialog", userInput);
					}
				}
			}
			else if(source == remove)
			{
				int index = list.getSelectedIndex();
				listData.remove(index);
				listModel.fireContentsChanged(listModel, 0, listData.size() - 1);
				if(!listData.isEmpty())
				{
					list.setSelectedIndex(
						Math.min(listData.size() - 1,
							index));
				}
				updateButtons();
			}
			else if(source == reset)
			{
				String dialogType = "options.browser.additional-unc-roots.reset.dialog";
				int result = GUIUtilities.confirm(list, dialogType, null,
					JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE);

				if(result == JOptionPane.YES_OPTION)
				{
					// the user should be able to cancel the options dialog
					// so we need to modify the list, not the actual property
					// since the default value is not available,
					// we reset, fetch default value and re-set to original
					String orgUncRoots = jEdit.getProperty("vfs.browser.additionalUncRoots");
					jEdit.resetProperty("vfs.browser.additionalUncRoots");
					String defaultUncRoots = jEdit.getProperty("vfs.browser.additionalUncRoots");
					jEdit.setProperty("vfs.browser.additionalUncRoots", orgUncRoots);
					reloadUncRootsList(defaultUncRoots);

					// reset selection if user had more buttons than default
					list.setSelectedIndex(0);
					list.ensureIndexIsVisible(0);
					updateButtons();
				}
			}
		}
	} // }}}
}
