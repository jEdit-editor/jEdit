/*
 * FirstTimeWizard.java - First time installation wizard
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
import javax.swing.event.*;
import javax.swing.*;
import java.awt.*;
import org.gjt.sp.jedit.jEdit;

public class FirstTimeWizard extends Wizard
{
	public FirstTimeWizard()
	{
		super(jEdit.getProperty("edit-buddy.first-time.caption"),
			createPages());
	}

	// private members
	private static Component[] createPages()
	{
		Component[] pages = {
			createHTMLScrollPane(createHTMLView(FirstTimeWizard.class
				.getResource("/first-time.html")),null),
			createHTMLScrollPane(createHTMLView(FirstTimeWizard.class
				.getResource("/firewall.html")),
				new String[] { "firewall-config" }),
			createHTMLScrollPane(createHTMLView(FirstTimeWizard.class
				.getResource("/install-plugins.html")),
				new String[] { "install-plugins" }),
			createHTMLScrollPane(createHTMLView(FirstTimeWizard.class
				.getResource("/first-time-finished.html")),null)
		};
		return pages;
	}
}
