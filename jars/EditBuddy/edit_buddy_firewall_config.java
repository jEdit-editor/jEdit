/*
 * edit_buddy_firewall_config.java - Displays firewall configuration dialog
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

import java.awt.event.ActionEvent;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

public class edit_buddy_firewall_config extends EditAction
{
	public edit_buddy_firewall_config()
	{
		super("edit-buddy-firewall-config");
	}

	public void actionPerformed(ActionEvent evt)
	{
		OptionPane optPane;
		try
		{
			optPane = (OptionPane)getClass().getClassLoader()
				.loadClass("FirewallOptionPane").newInstance();
		}
		catch(Exception e)
		{
			Log.log(Log.ERROR,this,e);
			return;
		}

		new OptionPaneDialog(null,optPane,false);
	}
}
