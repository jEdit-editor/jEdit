/*
 * set_search_parameters.java
 * Copyright (C) 1999 Slava Pestov
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

package org.gjt.sp.jedit.actions;

import java.awt.event.ActionEvent;
import java.util.StringTokenizer;
import org.gjt.sp.jedit.search.*;
import org.gjt.sp.jedit.*;

public class set_search_parameters extends EditAction
{
	public set_search_parameters()
	{
		super("set-search-parameters");
	}

	public void actionPerformed(ActionEvent evt)
	{
		String actionCommand = evt.getActionCommand();
		StringTokenizer st = new StringTokenizer(actionCommand);

		while(st.hasMoreTokens())
		{
			String token = st.nextToken();

			if(token.equals("regexp"))
				SearchAndReplace.setRegexp(true);
			else if(token.equals("literal"))
				SearchAndReplace.setRegexp(false);
			else if(token.equals("icase"))
				SearchAndReplace.setIgnoreCase(true);
			else if(token.equals("case"))
				SearchAndReplace.setIgnoreCase(false);
			else if(token.equals("current"))
				SearchAndReplace.setSearchFileSet(
					new CurrentBufferSet());
			else if(token.equals("all"))
				SearchAndReplace.setSearchFileSet(
					new AllBufferSet());
		}
	}
}
