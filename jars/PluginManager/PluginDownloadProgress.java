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
	public PluginDownloadProgress(JDialog dialog, String[] urls,
		String[] dirs)
	{
		super(JOptionPane.getFrameForComponent(dialog),
			jEdit.getProperty("download-progress.title"),true);

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		count = urls.length;

		message = new JLabel("Hello World");
		message.setBorder(new EmptyBorder(0,0,12,0));
		content.add(BorderLayout.NORTH,message);

		progress = new JProgressBar();
		progress.setStringPainted(true);
		content.add(BorderLayout.CENTER,progress);

		stop = new JButton(jEdit.getProperty("download-progress.stop"));
		stop.addActionListener(new ActionHandler());
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
		panel.setBorder(new EmptyBorder(12,0,0,0));
		panel.add(Box.createGlue());
		panel.add(stop);
		panel.add(Box.createGlue());
		content.add(BorderLayout.SOUTH,panel);

		addWindowListener(new WindowHandler());

		pack();

		Dimension screen = getToolkit().getScreenSize();
		Dimension size = getSize();
		size.width = Math.max(size.width,500);
		setSize(size);
		setLocationRelativeTo(dialog);

		thread = new PluginDownloadThread(this,urls,dirs);

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
