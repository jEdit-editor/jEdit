/*
 * PluginManagerProgress.java - Plugin download progress meter
 * Copyright (C) 2000, 2001 Slava Pestov
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

package org.gjt.sp.jedit.pluginmgr;

import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import org.gjt.sp.jedit.*;

public class PluginManagerProgress extends JDialog
{
	public PluginManagerProgress(JDialog dialog, Roster roster)
	{
		super(JOptionPane.getFrameForComponent(dialog),
			jEdit.getProperty("plugin-manager.progress.title"),true);

		this.roster = roster;

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		message = new JLabel("...");
		message.setBorder(new EmptyBorder(0,0,12,0));
		content.add(BorderLayout.NORTH,message);

		progress = new JProgressBar();
		progress.setStringPainted(true);
		content.add(BorderLayout.CENTER,progress);

		stop = new JButton(jEdit.getProperty("plugin-manager.progress.stop"));
		stop.addActionListener(new ActionHandler());
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
		panel.setBorder(new EmptyBorder(12,0,0,0));
		panel.add(Box.createGlue());
		panel.add(stop);
		panel.add(Box.createGlue());
		content.add(BorderLayout.SOUTH,panel);

		count = roster.getOperationCount();

		addWindowListener(new WindowHandler());

		pack();

		Dimension screen = getToolkit().getScreenSize();
		Dimension size = getSize();
		size.width = Math.max(size.width,500);
		setSize(size);
		setLocationRelativeTo(dialog);

		show();
	}

	public void removing(String plugin)
	{
		String[] args = { plugin };
		showMessage(jEdit.getProperty("plugin-manager.progress.removing",args));
		stop.setEnabled(true);
	}

	public void downloading(String plugin)
	{
		String[] args = { plugin };
		showMessage(jEdit.getProperty("plugin-manager.progress.downloading",args));
		stop.setEnabled(true);
	}

	public void installing(String plugin)
	{
		String[] args = { plugin };
		showMessage(jEdit.getProperty("plugin-manager.progress.installing",args));
		stop.setEnabled(false);
	}

	public void setMaximum(final int total)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				progress.setMaximum(total);
			}
		});
	}

	public void setValue(final int value)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				progress.setValue(value);
			}
		});
	}

	public void done(final boolean ok)
	{
		this.ok |= ok;

		if(!ok || done == count)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					dispose();
					if(ok)
					{
						GUIUtilities.message(PluginManagerProgress.this,
							"plugin-manager.done",null);
					}
					else
					{
						GUIUtilities.message(PluginManagerProgress.this,
							"plugin-manager.failed",null);
					}
				}
			});
		}
		else
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					progress.setValue(0);
				}
			});
			done++;
		}
	}

	public boolean isOK()
	{
		return ok;
	}

	// private members
	private Thread thread;

	private JLabel message;
	private JProgressBar progress;
	private JButton stop;
	private int count;
	private int done = 1;

	private boolean ok;

	private Roster roster;

	private void showMessage(final String msg)
	{
		try
		{
			SwingUtilities.invokeAndWait(new Runnable()
			{
				public void run()
				{
					message.setText(msg + " (" + done + "/" + count + ")");
				}
			});
		}
		catch(Exception e)
		{
		}

		Thread.yield();
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			if(evt.getSource() == stop)
			{
				thread.interrupt();
				dispose();
			}
		}
	}

	class WindowHandler extends WindowAdapter
	{
		boolean done;

		public void windowActivated(WindowEvent evt)
		{
			if(done)
				return;

			done = true;
			thread = new RosterThread();
			thread.start();
		}

		public void windowClosing(WindowEvent evt)
		{
			thread.interrupt();
			dispose();
		}
	}

	class RosterThread extends Thread
	{
		RosterThread()
		{
			super("Plugin manager thread");
		}

		public void run()
		{
			roster.performOperations(PluginManagerProgress.this);
		}
	}
}
