/*
 * LineContext.java
 * Copyright (C) 1999 mike dillon
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

package org.gjt.sp.jedit.syntax;

public class LineContext
{
	public LineContext parent;
	public ParserRule inRule;
	public ParserRuleSet rules;

	public LineContext(ParserRule r, ParserRuleSet rs)
	{
		inRule = r;
		rules = rs;
	}

	public LineContext(ParserRuleSet rs, LineContext lc)
	{
		rules = rs;
		try
		{
			parent = (lc == null) ? null : (LineContext) lc.clone();
		}
		catch (CloneNotSupportedException e)
		{
		}
	}

	public LineContext(ParserRule r)
	{
		this(r, null);
	}

	public LineContext()
	{
		this((ParserRule) null, (ParserRuleSet) null);
	}

	protected Object clone()
		throws CloneNotSupportedException
	{
		LineContext lc = new LineContext();
		lc.inRule = inRule;
		lc.rules = rules;

		try
		{
			lc.parent = (parent == null) ? null : (LineContext) parent.clone();
		}
		catch (CloneNotSupportedException e)
		{
		}

		return lc;
	}
}
