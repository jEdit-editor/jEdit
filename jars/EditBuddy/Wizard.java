/*
 * Wizard.java - A 'wizard'
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
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public abstract class Wizard extends JPanel
{
	public Wizard(String title, Component[] pages)
	{
		super(new BorderLayout(12,12));
		setBorder(new EmptyBorder(12,12,12,12));

		JLabel label = new JLabel(title);
		label.setFont(new Font("SansSerif",Font.PLAIN,36));
		label.setForeground(UIManager.getColor("Button.foreground"));
		add(BorderLayout.NORTH,label);

		ActionHandler actionHandler = new ActionHandler();

		Box buttons = new Box(BoxLayout.X_AXIS);

		cancelButton = new JButton(jEdit.getProperty("common.cancel"));
		cancelButton.addActionListener(actionHandler);
		buttons.add(cancelButton);
		buttons.add(Box.createHorizontalStrut(6));
		buttons.add(Box.createGlue());

		prevButton = new JButton(jEdit.getProperty("edit-buddy.prev"));
		prevButton.addActionListener(actionHandler);
		buttons.add(prevButton);
		buttons.add(Box.createHorizontalStrut(6));

		nextButton = new JButton();
		nextButton.addActionListener(actionHandler);
		buttons.add(nextButton);

		// give all the buttons the same width
		Dimension dim = cancelButton.getPreferredSize();
		dim.width = Math.max(dim.width,prevButton.getPreferredSize().width);
		dim.width = Math.max(dim.width,nextButton.getPreferredSize().width);
		cancelButton.setPreferredSize(dim);
		prevButton.setPreferredSize(dim);
		nextButton.setPreferredSize(dim);

		add(BorderLayout.SOUTH,buttons);

		nextButtonLabel = jEdit.getProperty("edit-buddy.next");
		finishButtonLabel = jEdit.getProperty("edit-buddy.finish");

		pagesPanel = new JPanel(new WizardLayout());
		//pagesPanel.setBorder(UIManager.getBorder("TextField.border"));
		//pagesPanel.setBackground(Color.white);

		this.pages = pages;
		for(int i = 0; i < pages.length; i++)
		{
			pagesPanel.add(pages[i]);
		}

		add(BorderLayout.CENTER,pagesPanel);

		pageChanged();
	}

	// protected members
	protected static JEditorPane createHTMLView(Object page)
	{
		JEditorPane editorPane = new JEditorPane();
		editorPane.setEditable(false);
		editorPane.setContentType("text/html");

		if(page instanceof URL)
		{
			try
			{
				editorPane.setPage((URL)page);
			}
			catch(IOException io)
			{
				Log.log(Log.ERROR,FirstTimeWizard.class,io);
			}
		}
		else if(page instanceof String)
		{
			editorPane.setText((String)page);
		}

		return editorPane;
	}

	protected static JComponent createHTMLScrollPane(JComponent comp, String[] buttons)
	{
		JScrollPane scroller = new JScrollPane(comp);
		//scroller.setBorder(null);

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(BorderLayout.CENTER,scroller);
		if(buttons != null)
		{
			JPanel buttonPanel = new JPanel();
			buttonPanel.setLayout(new BoxLayout(buttonPanel,BoxLayout.X_AXIS));
			buttonPanel.setBorder(new EmptyBorder(12,0,0,0));
			//buttonPanel.setBackground(Color.white);
			buttonPanel.add(Box.createGlue());
			for(int i = 0; i < buttons.length; i++)
			{
				if(i != 0)
					buttonPanel.add(Box.createHorizontalStrut(6));
				buttonPanel.add(new BeanShellButton(buttons[i]));
			}
			buttonPanel.add(Box.createGlue());
			panel.add(BorderLayout.SOUTH,buttonPanel);
		}

		return panel;
	}

	// private members
	private JButton cancelButton;
	private JButton prevButton;
	private JButton nextButton;
	private String nextButtonLabel, finishButtonLabel;
	private Component[] pages;
	private int currentPage;
	private JPanel pagesPanel;

	private void pageChanged()
	{
		prevButton.setEnabled(currentPage != 0);
		if(currentPage == pages.length - 1)
			nextButton.setText(finishButtonLabel);
		else
			nextButton.setText(nextButtonLabel);

		pagesPanel.revalidate();
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(evt.getSource() == cancelButton)
				((JDialog)getRootPane().getParent()).dispose();
			else if(evt.getSource() == prevButton)
			{
				currentPage--;
				pageChanged();
			}
			else if(evt.getSource() == nextButton)
			{
				if(currentPage == pages.length - 1)
					((JDialog)getRootPane().getParent()).dispose();
				else
				{
					currentPage++;
					pageChanged();
				}
			}
		}
	}

	class WizardLayout implements LayoutManager
	{
		public void addLayoutComponent(String name, Component comp)
		{
		}

		public void removeLayoutComponent(Component comp)
		{
		}

		public Dimension preferredLayoutSize(Container parent)
		{
			Dimension dim = new Dimension();

			for(int i = 0; i < pages.length; i++)
			{
				Dimension _dim = pages[i].getPreferredSize();
				dim.width = Math.max(_dim.width,dim.width);
				dim.height = Math.max(_dim.height,dim.height);
			}

			return dim;
		}

		public Dimension minimumLayoutSize(Container parent)
		{
			return preferredLayoutSize(parent);
		}

		public void layoutContainer(Container parent)
		{
			Dimension size = parent.getSize();

			Rectangle currentPageBounds = new Rectangle(
				0,0,size.width,size.height);

			for(int i = 0; i < pages.length; i++)
			{
				Component page = pages[i];
				page.setBounds(currentPageBounds);
				page.setVisible(i == currentPage);
			}
		}
	}
}
