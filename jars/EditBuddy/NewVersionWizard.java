/*
 * NewVersionWizard.java - New version installation wizard
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

import java.awt.Component;
import org.gjt.sp.jedit.jEdit;

public class NewVersionWizard extends Wizard
{
	public NewVersionWizard(String oldVersion, String newVersion)
	{
		super(jEdit.getProperty("edit-buddy.new-version.caption"),
			createPages(oldVersion, newVersion));
	}

	// private members
	private static Component[] createPages(String oldVersion,
		String newVersion)
	{
		String[] args = { oldVersion, newVersion };
		Component[] pages = {
			createHTMLScrollPane(createHTMLView(jEdit.getProperty(
				"edit-buddy.new-version.text",args)),null),
			createHTMLScrollPane(createHTMLView(NewVersionWizard.class
				.getResource("/update-plugins.html")),
				new String[] { "update-plugins" }),
			createHTMLScrollPane(createHTMLView(NewVersionWizard.class
				.getResource("/new-version-finished.html")),
				new String[] { "view-changes" , "view-news" }),
		};
		return pages;
	}
}
