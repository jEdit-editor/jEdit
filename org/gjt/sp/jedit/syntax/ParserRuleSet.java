/*
 * ParserRuleSet.java - A set of parser rules
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

import java.util.Enumeration;
import java.util.Vector;
import javax.swing.text.Segment;

/**
 * A set of parser rules.
 * @author mike dillon
 * @version $Id$
 */
public class ParserRuleSet
{
	public ParserRuleSet(int sz)
	{
		rules = new Vector(sz);
	}

	public ParserRuleSet()
	{
		this(DEFAULT_RULE_VECTOR_SIZE);
	}

	public void addRule(ParserRule r)
	{
		rules.addElement(r);
	}

	public void removeRule(ParserRule r)
	{
		rules.removeElement(r);
	}

	public Vector getRules()
	{
		return rules;
	}

	public ParserRule[] getRuleArray()
	{
		ParserRule[] ruleArray = new ParserRule[rules.size()];
		rules.copyInto(ruleArray);
		return ruleArray;
	}

	public int getRuleCount()
	{
		return rules.size();
	}

	public int getTerminateChar()
	{
		return terminateChar;
	}

	public void setTerminateChar(int atChar)
	{
		terminateChar = (atChar >= 0) ? atChar : -1;
	}

	public boolean getIgnoreCase()
	{
		return ignoreCase;
	}

	public void setIgnoreCase(boolean b)
	{
		ignoreCase = b;
	}

	public KeywordMap getKeywords()
	{
		return keywords;
	}

	public void setKeywords(KeywordMap km)
	{
		keywords = km;
	}

	public ParserRule getEscapeRule()
	{
		return escapeRule;
	}

	public Segment getEscapePattern()
	{
		if (escapePattern == null && escapeRule != null)
		{
			escapePattern = new Segment(escapeRule.searchChars, 0,
				escapeRule.sequenceLengths[0]);
		}
		return escapePattern;
	}

	public void setEscape(String esc)
	{
		if (esc == null)
		{
			escapeRule = null;
		}
		else
		{
			escapeRule = ParserRuleFactory.createEscapeRule(esc,
				TokenMarker.AC_NULL);
		}
		escapePattern = null;
	}

	public int getDefault()
	{
		return defaultID;
	}

	public void setDefault(int def)
	{
		defaultID = def;
	}

	private static final int DEFAULT_RULE_VECTOR_SIZE = 48;
	private KeywordMap keywords;
	private Vector rules;
	private ParserRule escapeRule;
	private Segment escapePattern;
	private int terminateChar = -1;
	private boolean ignoreCase = true;
	private int defaultID;
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.3  2000/04/07 06:57:26  sp
 * Buffer options dialog box updates, API docs updated a bit in syntax package
 *
 * Revision 1.2  2000/04/01 08:40:55  sp
 * Streamlined syntax highlighting, Perl mode rewritten in XML
 *
 */
