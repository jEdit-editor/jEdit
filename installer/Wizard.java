/*
 * Wizard.java
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

package installer;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

/*
 * An abstract class that steps the user through a set of pages. Used by
 * SwingInstall.
 */
public abstract class Wizard extends JComponent
{
	public Wizard(Color highlight, Icon logo, String cancelButtonLabel,
		String prevButtonLabel, String nextButtonLabel,
		String finishButtonLabel, Component[] pages)
	{
		this.highlight = highlight;
		this.logo = logo;

		ActionHandler actionHandler = new ActionHandler();

		cancelButton = new JButton(cancelButtonLabel);
		cancelButton.setRequestFocusEnabled(false);
		cancelButton.addActionListener(actionHandler);
		prevButton = new JButton(prevButtonLabel);
		prevButton.setRequestFocusEnabled(false);
		prevButton.addActionListener(actionHandler);
		nextButton = new JButton();
		nextButton.setRequestFocusEnabled(false);
		nextButton.addActionListener(actionHandler);
		this.nextButtonLabel = nextButtonLabel;
		this.finishButtonLabel = finishButtonLabel;

		this.pages = pages;
		for(int i = 0; i < pages.length; i++)
		{
			add(pages[i]);
		}

		setLayout(new WizardLayout());
		add(cancelButton);
		add(prevButton);
		add(nextButton);

		pageChanged();
	}

	public void paintComponent(Graphics g)
	{
		int topBorder = logo.getIconHeight() + PADDING * 2;
		int sideBorder = PADDING * 2;
		int bottomBorder = cancelButton.getPreferredSize().height + PADDING * 2;

		g.setColor(highlight);
		g.fillRect(0,0,getWidth(),getHeight());

		logo.paintIcon(this,g,sideBorder + (getWidth() - PADDING * 4
			- logo.getIconWidth()) / 2,PADDING);

		int width = getWidth() - sideBorder * 2;
		int height = getHeight() - topBorder - bottomBorder;

		g.setColor(getBackground());
		g.fillRoundRect(sideBorder,topBorder,width,height,
			PADDING * 2,PADDING * 2);
	}

	// protected members
	protected abstract void cancelCallback();
	protected abstract void finishCallback();

	// private members
	private Color highlight;
	private Icon logo;
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
				cancelCallback();
			else if(evt.getSource() == prevButton)
			{
				currentPage--;
				pageChanged();
			}
			else if(evt.getSource() == nextButton)
			{
				if(currentPage == pages.length - 1)
					finishCallback();
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

			dim.width = Math.max(logo.getIconWidth()
				- PADDING * 2,dim.width);

			dim.width += PADDING * 6;
			dim.height += (logo.getIconHeight() + cancelButton
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

			dim.width = Math.max(logo.getIconWidth()
				- PADDING * 2,dim.width);

			dim.width += PADDING * 6;
			dim.height += (logo.getIconHeight() + cancelButton
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

			int topBorder = logo.getIconHeight() + PADDING * 2;
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
