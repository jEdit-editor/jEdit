/*
 * FirstTimeWizard.java - First time installation wizard
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

import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.StringTokenizer;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;

public class FirstTimeWizard extends Wizard
{
	public FirstTimeWizard()
	{
		super(jEdit.getProperty("edit-buddy.first-time.caption"),
			createPages());
	}

	// private members
	private static Component[] createPages()
	{
		Component[] pages = {
			createHTMLScrollPane(createHTMLView(FirstTimeWizard.class
				.getResource("/first-time.html"))),
			new SettingsDirPanel(),
			createHTMLScrollPane(createHTMLView(FirstTimeWizard.class
				.getResource("/firewall.html"))),
			createHTMLScrollPane(createHTMLView(FirstTimeWizard.class
				.getResource("/install-plugins.html"))),
			createHTMLScrollPane(createHTMLView(FirstTimeWizard.class
				.getResource("/first-time-finished.html")))
		};
		return pages;
	}

	static class SettingsDirPanel extends JPanel
	{
		SettingsDirPanel()
		{
			super(new BorderLayout());

			String[] args = { jEdit.getSettingsDirectory() };
			add(BorderLayout.NORTH,createHTMLScrollPane(
				createHTMLView(jEdit.getProperty("edit-buddy.first-time"
				+ ".settings-dir.text",args))));

			DefaultListModel listModel = new DefaultListModel();
			StringTokenizer st = new StringTokenizer(jEdit.getProperty(
				"edit-buddy.first-time.settings-dir.files"));
			while(st.hasMoreTokens())
			{
				listModel.addElement(st.nextToken());
			}

			list = new JList(listModel);
			list.setCellRenderer(new Renderer());
			list.addListSelectionListener(new ListHandler());
			JScrollPane scroller = new JScrollPane(list);
			scroller.setVerticalScrollBarPolicy(JScrollPane
				.VERTICAL_SCROLLBAR_ALWAYS);
			scroller.setBorder(new MatteBorder(1,1,1,1,Color.black));
			add(BorderLayout.WEST,scroller);

			description = createHTMLView(getClass().getResource("/settings-dir.html"));
			add(BorderLayout.CENTER,createHTMLScrollPane(description));
		}

		// private members
		private JList list;
		private JEditorPane description;

		static class Renderer extends DefaultListCellRenderer
		{
			public Component getListCellRendererComponent(
				JList list, Object value, int index,
				boolean isSelected, boolean cellHasFocus)
			{
				super.getListCellRendererComponent(list,value,
					index,isSelected,cellHasFocus);

				String svalue = (String)value;
				if(svalue.endsWith("/"))
				{
					setIcon(UIManager.getIcon("FileView.directoryIcon"));
					svalue = svalue.substring(0,svalue.length() - 1);
				}
				else
					setIcon(UIManager.getIcon("FileView.fileIcon"));

				setText(svalue);

				return this;
			}
		}

		class ListHandler implements ListSelectionListener
		{
			public void valueChanged(ListSelectionEvent evt)
			{
				if(evt.getValueIsAdjusting())
					return;

				String value = (String)list.getSelectedValue();
				if(value == null)
					return;

				if(value.endsWith("/"))
					value = value.substring(0,value.length() - 1);
				try
				{
					description.setPage(FirstTimeWizard.class
						.getResource("settings-dir-"
						+ value + ".html"));
				}
				catch(IOException io)
				{
					Log.log(Log.ERROR,this,io);
				}
			}
		}
	}
}
