/*
 * jEdit - Programmer's Text Editor
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2011 Matthieu Casanova
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
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

import org.gjt.sp.jedit.gui.AbstractContextOptionPane;
import org.gjt.sp.jedit.jEdit;

/**
 * Right-click context menu editor.
 *
 * @author Matthieu Casanova
 */
public class BrowserContextOptionPane extends AbstractContextOptionPane
{
	public BrowserContextOptionPane()
	{
		super("browser.custom.context", jEdit.getProperty("options.browser.custom.context.caption"), "browser.custom.context");
	}
}
