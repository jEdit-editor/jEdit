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

import javax.swing.border.EmptyBorder;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public abstract class Wizard extends JComponent
{
	public Wizard(String title, Component[] pages)
	{
		this.title = title;

		ActionHandler actionHandler = new ActionHandler();

		cancelButton = new JButton(jEdit.getProperty("common.cancel"));
		cancelButton.setRequestFocusEnabled(false);
		cancelButton.addActionListener(actionHandler);
		prevButton = new JButton(jEdit.getProperty("edit-buddy.prev"));
		prevButton.setRequestFocusEnabled(false);
		prevButton.addActionListener(actionHandler);
		nextButton = new JButton();
		nextButton.setRequestFocusEnabled(false);
		nextButton.addActionListener(actionHandler);
		nextButtonLabel = jEdit.getProperty("edit-buddy.next");
		finishButtonLabel = jEdit.getProperty("edit-buddy.finish");

		this.pages = pages;
		for(int i = 0; i < pages.length; i++)
		{
			add(pages[i]);
		}

		setLayout(new WizardLayout());
		add(cancelButton);
		add(prevButton);
		add(nextButton);

		// title font
		setFont(new Font("SansSerif",Font.PLAIN,36));
		metrics = getToolkit().getFontMetrics(getFont());

		pageChanged();
	}

	public void paintComponent(Graphics g)
	{
		int topBorder = metrics.getHeight() + PADDING * 2;
		int sideBorder = PADDING * 2;
		int bottomBorder = cancelButton.getPreferredSize().height + PADDING * 2;

		g.setColor(new Color(204,204,204));
		g.fillRect(0,0,getWidth(),getHeight());

		g.setColor(Color.black);
		g.drawString(title,sideBorder,PADDING + metrics.getAscent());
		int width = getWidth() - sideBorder * 2;
		int height = getHeight() - topBorder - bottomBorder;

		g.setColor(Color.white);
		g.fillRoundRect(sideBorder,topBorder,width,height,
			PADDING * 2,PADDING * 2);
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
		scroller.setBorder(null);

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(BorderLayout.CENTER,scroller);
		if(buttons != null)
		{
			JPanel buttonPanel = new JPanel();
			buttonPanel.setBackground(Color.white);
			buttonPanel.setLayout(new BoxLayout(buttonPanel,BoxLayout.X_AXIS));
			buttonPanel.setBorder(new EmptyBorder(12,12,12,12));
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
	private String title;
	private FontMetrics metrics;
	private JButton cancelButton;
	private JButton prevButton;
	private JButton nextButton;
	private String nextButtonLabel, finishButtonLabel;
	private Component[] pages;
	private int currentPage;

	private static int PADDING = 12;

	private void pageChanged()
	{
		prevButton.setEnabled(currentPage != 0);
		if(currentPage == pages.length - 1)
			nextButton.setText(finishButtonLabel);
		else
			nextButton.setText(nextButtonLabel);

		revalidate();
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

			dim.width = Math.max(metrics.stringWidth(title)
				- PADDING * 2,dim.width);

			dim.width += PADDING * 6;
			dim.height += (metrics.getHeight() + cancelButton
				.getPreferredSize().height + PADDING * 6);
			return dim;
		}

		public Dimension minimumLayoutSize(Container parent)
		{
			Dimension dim = new Dimension();

			for(int i = 0; i < pages.length; i++)
			{
				Dimension _dim = pages[i].getMinimumSize();
				dim.width = Math.max(_dim.width,dim.width);
				dim.height = Math.max(_dim.height,dim.height);
			}

			dim.width = Math.max(metrics.stringWidth(title)
				- PADDING * 2,dim.width);

			dim.width += PADDING * 6;
			dim.height += (metrics.getHeight() + cancelButton
				.getPreferredSize().height + PADDING * 6);
			return dim;
		}

		public void layoutContainer(Container parent)
		{
			Dimension size = getSize();

			// make all buttons the same size
			Dimension buttonSize = cancelButton.getPreferredSize();
			buttonSize.width = Math.max(buttonSize.width,prevButton.getPreferredSize().width);
			buttonSize.width = Math.max(buttonSize.width,nextButton.getPreferredSize().width);

			int topBorder = metrics.getHeight() + PADDING * 2;
			int sideBorder = PADDING * 2;
			int bottomBorder = buttonSize.height + PADDING * 2;

			// cancel button goes on far left
			cancelButton.setBounds(sideBorder,size.height - buttonSize.height
				- PADDING,buttonSize.width,buttonSize.height);

			// prev and next buttons are on the right
			prevButton.setBounds(size.width - buttonSize.width * 2 - 6 - sideBorder,
				size.height - buttonSize.height - PADDING,
				buttonSize.width,buttonSize.height);

			nextButton.setBounds(size.width - buttonSize.width - sideBorder,
				size.height - buttonSize.height - PADDING,
				buttonSize.width,buttonSize.height);

			// calculate size for current page
			Rectangle currentPageBounds = new Rectangle();
			currentPageBounds.x = PADDING * 3;
			currentPageBounds.y = topBorder + PADDING;
			currentPageBounds.width = size.width - PADDING * 6;
			currentPageBounds.height = size.height - topBorder
				- bottomBorder - PADDING * 2;

			for(int i = 0; i < pages.length; i++)
			{
				Component page = pages[i];
				page.setBounds(currentPageBounds);
				page.setVisible(i == currentPage);
			}
		}
	}
}
