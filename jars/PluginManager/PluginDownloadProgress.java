/*
 * PluginDownloadProgress.java - Plugin download progress meter
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
import org.gjt.sp.jedit.*;

public class PluginDownloadProgress extends JDialog
{
	public PluginDownloadProgress(View view, String[] urls, String[] dirs)
	{
		super(view,jEdit.getProperty("download-progress.title"),true);

		count = urls.length;

		message = new JLabel("Hello World");
		message.setBorder(new EmptyBorder(10,10,10,10));
		getContentPane().add(BorderLayout.NORTH,message);

		progress = new JProgressBar();
		progress.setStringPainted(true);
		getContentPane().add(BorderLayout.CENTER,progress);

		stop = new JButton(jEdit.getProperty("download-progress.stop"));
		stop.addActionListener(new ActionHandler());
		JPanel panel = new JPanel();
		panel.setBorder(new EmptyBorder(10,10,10,10));
		panel.add(stop);
		getContentPane().add(BorderLayout.SOUTH,panel);

		thread = new PluginDownloadThread(view,this,urls,dirs);

		pack();

		Dimension screen = getToolkit().getScreenSize();
		Dimension size = getSize();
		size.width = Math.max(size.width,500);
		setBounds((screen.width - size.width) / 2,
			(screen.height - size.height) / 2,
			size.width, size.height);

		addWindowListener(new WindowHandler());

		show();
	}

	public void downloading(String plugin)
	{
		String[] args = { plugin };
		showMessage(jEdit.getProperty("download-progress.downloading",args));
		stop.setEnabled(true);
	}

	public void installing(String plugin)
	{
		String[] args = { plugin };
		showMessage(jEdit.getProperty("download-progress.installing",args));
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

	public void done(boolean ok)
	{
		this.ok |= ok;

		if(done == count)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					dispose();
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
	private PluginDownloadThread thread;

	private JLabel message;
	private JProgressBar progress;
	private JButton stop;
	private int count;
	private int done = 1;

	private boolean ok;

	private void showMessage(final String msg)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				message.setText(msg + " (" + done + "/" + count + ")");
			}
		});
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
		public void windowClosing(WindowEvent evt)
		{
			thread.interrupt();
			dispose();
		}
	}
}
