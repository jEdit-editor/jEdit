/*
 * LatestVersionPlugin.java - Latest Version Check Plugin
 * Copyright (C) 1999, 2000 Slava Pestov
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

import java.util.Vector;
import org.gjt.sp.jedit.*;

public class LatestVersionPlugin extends EditPlugin
{
	public void start()
	{
		jEdit.addAction(new version_check());
	}

	public void createMenuItems(View view, Vector menus, Vector menuItems)
	{
		menuItems.addElement(GUIUtilities.loadMenuItem(view,"version-check"));
	}
}
