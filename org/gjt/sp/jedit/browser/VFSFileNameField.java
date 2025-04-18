/*
 * VFSFileNameField.java - File name field with completion
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2003, 2005 Slava Pestov
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

package org.gjt.sp.jedit.browser;

//{{{ Imports
import java.util.HashSet;
import java.awt.event.*;
import java.awt.*;
import java.util.Set;

import org.gjt.sp.jedit.OperatingSystem;
import org.gjt.sp.jedit.gui.HistoryTextField;
import org.gjt.sp.jedit.io.*;
import org.gjt.sp.jedit.MiscUtilities;

import org.gjt.sp.util.Log;

import static org.gjt.sp.jedit.MiscUtilities.isUncPath;
//}}}

/**
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.2pre1 (public since 4.5pre1)
 */
public class VFSFileNameField extends HistoryTextField
{
	//{{{ VFSFileNameField constructor
	public VFSFileNameField(VFSBrowser browser, String model)
	{
		super(model);
		setEnterAddsToHistory(false);

		this.browser = browser;

		Dimension dim = getPreferredSize();
		dim.width = Integer.MAX_VALUE;
		setMaximumSize(dim);

		// Enable TAB pressed for completion instead of
		// focas traversal.
		final int FORWARD = KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS;
		Set<AWTKeyStroke> keys = new HashSet<>(getFocusTraversalKeys(FORWARD));
		keys.remove(AWTKeyStroke.getAWTKeyStroke("pressed TAB"));
		setFocusTraversalKeys(FORWARD, keys);
	} //}}}

	//{{{ processKeyEvent() method
	@Override
	public void processKeyEvent(KeyEvent evt)
	{
		if(evt.getID() == KeyEvent.KEY_PRESSED)
		{
			String path = getText();

			switch(evt.getKeyCode())
			{
			case KeyEvent.VK_TAB:
				doComplete(path);
				break;
			case KeyEvent.VK_LEFT:
				if ((evt.getModifiersEx() & InputEvent.ALT_DOWN_MASK) == InputEvent.ALT_DOWN_MASK)
				{
					browser.previousDirectory();
					evt.consume();
				}
				else
				{
					// 				browser.getBrowserView().getTable().processKeyEvent(evt);
					super.processKeyEvent(evt);
				}
				break;
			case KeyEvent.VK_UP:
				if ((evt.getModifiersEx() & InputEvent.ALT_DOWN_MASK) == InputEvent.ALT_DOWN_MASK)
				{
					String p = browser.getDirectory();
					browser.setDirectory(MiscUtilities.getParentOfPath(p));
					evt.consume();
				}
				else
				{
					browser.getBrowserView().getTable()
					.processKeyEvent(evt);
				}
				break;
			case KeyEvent.VK_RIGHT:
				if ((evt.getModifiersEx() & InputEvent.ALT_DOWN_MASK) == InputEvent.ALT_DOWN_MASK)
				{
					evt.consume();
					browser.nextDirectory();
				}
				else
					super.processKeyEvent(evt);
				break;
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_PAGE_UP:
			case KeyEvent.VK_PAGE_DOWN:
				browser.getBrowserView().getTable()
					.processKeyEvent(evt);
				break;
			case KeyEvent.VK_ENTER:
				browser.filesActivated(
					(evt.isShiftDown()
					? VFSBrowser.M_OPEN_NEW_VIEW
					: VFSBrowser.M_OPEN),false);
				setText(null);
				evt.consume();
				break;
			default:
				super.processKeyEvent(evt);
				break;
			}
		}
		else if(evt.getID() == KeyEvent.KEY_TYPED)
		{
			char ch = evt.getKeyChar();
			super.processKeyEvent(evt);
			if((ch > 0x20 && ch != 0xff) || ch == 0x08)
			{
				String path = getText();
				BrowserView view = browser.getBrowserView();

				if(((MiscUtilities.getLastSeparatorIndex(path) == -1)
						&& (!(OperatingSystem.isWindows()
								&& (path.length() == 2)
								&& (path.charAt(1) == ':'))
							|| (browser.getDirectory().equals("roots:"))))
					|| (OperatingSystem.isWindows() && isUncPath(path) && (MiscUtilities.getLastSeparatorIndex(path) <= 1))
				)
				{
					int mode = browser.getMode();
					// fix for bug #765507
					// we don't type complete in save dialog
					// boxes. Press TAB to do an explicit
					// complete
					view.getTable().doTypeSelect(path,
						mode == VFSBrowser
						.CHOOSE_DIRECTORY_DIALOG
						||
						mode == VFSBrowser
						.SAVE_DIALOG);
				}
				else
					view.selectNone();
			}
		}
	} //}}}

	//{{{ Private members
	private final VFSBrowser browser;

	//{{{ doComplete() method
	public String doComplete(String path, String complete, boolean dirsOnly)
	{
		Log.log(Log.DEBUG,VFSFileNameField.class,
			"doComplete(" + path + "," + complete
			+ "," + dirsOnly);

		for(;;)
		{
			if(complete.isEmpty())
				return path;
			if(OperatingSystem.isWindows() && isUncPath(complete))
			{
				int index = MiscUtilities.getFirstSeparatorIndex(complete.substring(2));
				if(index == -1)
					return path;

				String newPath = VFSFile.findCompletion(path,
						complete.substring(0,index + 2),browser,dirsOnly);
				if(newPath == null)
					return null;
				path = newPath;
				complete = complete.substring(index + 3);
			}
			else
			{
				int index = MiscUtilities.getFirstSeparatorIndex(complete);
				if(index == -1)
					return path;

				String newPath = VFSFile.findCompletion(path,
					complete.substring(0,index),browser,dirsOnly);
				if(newPath == null)
					return null;
				path = newPath;
				complete = complete.substring(index + 1);
			}
		}
	} //}}}

	//{{{ doComplete() method
	private void doComplete(String currentText)
	{
		int index = MiscUtilities.getLastSeparatorIndex(currentText);
		String dir;
		if(index != -1)
			dir = currentText.substring(0,index + 1);
		else
			dir = "";

		SecondaryLoop secondaryLoop = Toolkit
				.getDefaultToolkit()
				.getSystemEventQueue()
				.createSecondaryLoop();

		if(MiscUtilities.isAbsolutePath(currentText))
		{
			boolean uncWithoutShare = OperatingSystem.isWindows() && isUncPath(currentText) && (index == 1);
			if(uncWithoutShare)
				dir = currentText;
			else if(dir.startsWith("/"))
				dir = dir.substring(1);

			dir = doComplete(VFSBrowser.getRootDirectory(),dir,true);
			if(dir == null)
				return;

			browser.setDirectory(dir, secondaryLoop::exit);
			secondaryLoop.enter();

			if(index == -1)
			{
				if(currentText.startsWith("/"))
					currentText = currentText.substring(1);
			}
			else if(!uncWithoutShare)
				currentText = currentText.substring(index + 1);
		}
		else
		{
			if(!dir.isEmpty())
			{
				dir = doComplete(browser.getDirectory(),dir,true);
				if(dir == null)
					return;

				browser.setDirectory(dir, secondaryLoop::exit);
				secondaryLoop.enter();

				currentText = currentText.substring(index + 1);
			}
		}

		BrowserView view = browser.getBrowserView();
		view.getTable().doTypeSelect(currentText,
			browser.getMode() == VFSBrowser
			.CHOOSE_DIRECTORY_DIALOG);

		String newText;

		VFSFile[] files = view.getSelectedFiles();
		if(files.length == 0)
			newText = currentText;
		else
		{
			String path = files[0].getPath();
			String name = files[0].getName();
			String parent = MiscUtilities.getParentOfPath(path);

			if(MiscUtilities.isAbsolutePath(currentText)
				&& !currentText.startsWith(browser.getDirectory()))
			{
				newText = path;
			}
			else
			{
				if(MiscUtilities.pathsEqual(parent,browser.getDirectory()))
					newText = name;
				else
					newText = path;
			}
		}

		setText(newText);
	} //}}}

	//}}}
}
