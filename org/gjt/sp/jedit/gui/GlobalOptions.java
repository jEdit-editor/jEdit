/*
 * GlobalOptions.java - Global options dialog
 * Copyright (C) 1998, 1999 Slava Pestov
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

package org.gjt.sp.jedit.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import org.gjt.sp.jedit.event.*;
import org.gjt.sp.jedit.*;

/**
 * The global (editor-wide) settings dialog.
 * @author Slava Pestov
 * @version $Id$
 */
public class GlobalOptions extends OptionsDialog
{
	public GlobalOptions(View view)
	{
		super(view,jEdit.getProperty("options.title"));

		addOptionPane(new org.gjt.sp.jedit.options.GeneralOptionPane());
		addOptionPane(new org.gjt.sp.jedit.options.EditorOptionPane());
		addOptionPane(new org.gjt.sp.jedit.options.KeyTableOptionPane());
		addOptionPane(new org.gjt.sp.jedit.options.StyleOptionPane());
		addOptionPane(new org.gjt.sp.jedit.options.FileFilterOptionPane());

		view.hideWaitCursor();

		Dimension screen = getToolkit().getScreenSize();
		pack();
		setLocation((screen.width - getSize().width) / 2,
			(screen.height - getSize().height) / 2);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		show();
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.11  1999/10/23 03:48:22  sp
 * Mode system overhaul, close all dialog box, misc other stuff
 *
 * Revision 1.10  1999/09/30 12:21:04  sp
 * No net access for a month... so here's one big jEdit 2.1pre1
 *
 * Revision 1.9  1999/07/16 23:45:49  sp
 * 1.7pre6 BugFree version
 *
 * Revision 1.8  1999/06/12 02:30:27  sp
 * Find next can now perform multifile searches, multifile-search command added,
 * new style option pane
 *
 * Revision 1.7  1999/06/07 06:36:32  sp
 * Syntax `styling' (bold/italic tokens) added,
 * plugin options dialog for plugin option panes
 *
 * Revision 1.6  1999/04/21 07:39:19  sp
 * FAQ added, plugins can now add panels to the options dialog
 *
 * Revision 1.5  1999/04/02 03:21:09  sp
 * Added manifest file, common strings such as OK, etc are no longer duplicated
 * many times in jedit_gui.props
 *
 * Revision 1.4  1999/03/21 01:07:27  sp
 * Fixed stupid bug in global options
 *
 * Revision 1.3  1999/03/20 05:23:32  sp
 * Code cleanups
 *
 * Revision 1.2  1999/03/20 04:52:55  sp
 * Buffer-specific options panel finished, attempt at fixing OS/2 caret bug, code
 * cleanups
 *
 * Revision 1.1  1999/03/20 02:07:59  sp
 * Starting work on buffer-specific options panel
 *
 */
