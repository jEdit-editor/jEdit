/*
 * SwingProgress.java - Swing progress window
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

package org.gjt.sp.sim;

import javax.swing.border.EmptyBorder;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

public class SwingProgress extends JFrame
implements ActionListener, Progress
{
	public SwingProgress(String appName)
	{
		super("SIM - installing " + appName);

		this.appName = appName;

		message = new JLabel("Installing " + appName + "...");
		message.setBorder(new EmptyBorder(10,10,10,10));
		getContentPane().add(BorderLayout.NORTH,message);

		progress = new JProgressBar();
		progress.setStringPainted(true);
		getContentPane().add(BorderLayout.CENTER,progress);

		stop = new JButton("Stop");
		stop.addActionListener(this);
		JPanel panel = new JPanel();
		panel.setBorder(new EmptyBorder(10,10,10,10));
		panel.add(stop);
		getContentPane().add(BorderLayout.SOUTH,panel);

		Dimension screen = getToolkit().getScreenSize();
		pack();
		Dimension size = getSize();
		size.width = Math.max(500,size.width);
		setBounds((screen.width - size.width) / 2,
			(screen.height - size.height) / 2,
			size.width,size.height);
		show();
	}

	public void showMessage(final String msg)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				message.setText(msg);
			}
		});
	}

	public void setMaximum(final int max)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				progress.setMaximum(max);
			}
		});
	}

	public void advance(final int value)
	{
		try
		{
			SwingUtilities.invokeAndWait(new Runnable()
			{
				public void run()
				{
					progress.setValue(progress
						.getValue() + value);
				}
			});
			Thread.yield();
		}
		catch(Exception e)
		{
		}
	}

	public void done()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				dispose();
				JOptionPane.showMessageDialog(null,
					appName + " installed successfully.",
					"Installation complete",
					JOptionPane.INFORMATION_MESSAGE);
				System.exit(0);
			}
		});
	}

	public void aborted()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				dispose();
				JOptionPane.showMessageDialog(null,
					"Installation aborted.",
					"Installation aborted",
					JOptionPane.ERROR_MESSAGE);
				System.exit(1);
			}
		});
	}

	public void error(final String message)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				dispose();
				JOptionPane.showMessageDialog(null,
					message,
					"Installation aborted",
					JOptionPane.ERROR_MESSAGE);
				System.exit(1);
			}
		});
	}

	public void setThread(InstallThread thread)
	{
		this.thread = thread;
	}

	public void actionPerformed(ActionEvent evt)
	{
		if(evt.getSource() == stop)
		{
			stop.getModel().setEnabled(false);
			thread.interrupt();
		}
	}

	// private members
	private String appName;
	private JLabel message;
	private JProgressBar progress;
	private JButton stop;
	private InstallThread thread;
}
