/*
 * GenericTokenMarker.java - Generic token marker
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
import java.util.Hashtable;
import javax.swing.text.Segment;

/**
 * Generic token marker.
 *
 * @author mike dillon
 */
public class GenericTokenMarker extends TokenMarker
{
	// public members
	// major actions (total: 8)
	public static final int MAJOR_ACTIONS = 0x000000FF;
	public static final int WHITESPACE = 1 << 0;
	public static final int SPAN = 1 << 1;
	public static final int MARK_PREVIOUS = 1 << 2;
	public static final int MARK_FOLLOWING = 1 << 3;
	public static final int EOL_SPAN = 1 << 4;
//	public static final int MAJOR_ACTION_5 = 1 << 5;
//	public static final int MAJOR_ACTION_6 = 1 << 6;
//	public static final int MAJOR_ACTION_7 = 1 << 7;

	// action hints (total: 8)
	public static final int ACTION_HINTS = 0x0000FF00;
	public static final int EXCLUDE_MATCH = 1 << 8;
	public static final int AT_LINE_START = 1 << 9;
	public static final int NO_LINE_BREAK = 1 << 10;
	public static final int NO_WORD_BREAK = 1 << 11;
	public static final int IS_ESCAPE = 1 << 12;
	public static final int DELEGATE = 1 << 13;
//	public static final int ACTION_HINT_14 = 1 << 14;
//	public static final int ACTION_HINT_15 = 1 << 15;

	// action tokens (total: 16)
	public static final int ACTION_TOKENS = 0xFFFF0000;
	public static final int NULL = 1 << 16;
	public static final int KEYWORD1 = 1 << 17;
	public static final int KEYWORD2 = 1 << 18;
	public static final int KEYWORD3 = 1 << 19;
	public static final int COMMENT1 = 1 << 20;
	public static final int COMMENT2 = 1 << 21;
	public static final int LITERAL1 = 1 << 22;
	public static final int LITERAL2 = 1 << 23;
	public static final int LABEL = 1 << 24;
	public static final int OPERATOR = 1 << 25;
	public static final int INVALID = 1 << 26;
//	public static final int ACTION_TOKEN_27 = 1 << 27;
//	public static final int ACTION_TOKEN_28 = 1 << 28;
//	public static final int ACTION_TOKEN_29 = 1 << 29;
//	public static final int ACTION_TOKEN_30 = 1 << 30;
//	public static final int ACTION_TOKEN_31 = 1 << 31;

	public GenericTokenMarker()
	{
		ruleSets = new Hashtable();
	}

	public void addRuleSet(String setName, ParserRuleSet rules)
	{
		if (rules == null) return;

		if (setName == null) setName = "MAIN";

		ruleSets.put(rulePfx.concat(setName), rules);
	}

	public ParserRuleSet getRuleSet(String setName)
	{
		ParserRuleSet rules;

		int delim = setName.indexOf("::");

		if (delim == -1)
		{
			rules = (ParserRuleSet) ruleSets.get(rulePfx.concat(setName));
		}
		else
		{
			rules = (ParserRuleSet) ruleSets.get(setName);

			if (rules == null && !setName.startsWith(rulePfx))
			{
				String modeName = setName.substring(0, delim);

				rules = getExternalRuleSet(modeName,
					setName.substring(delim + 2));

				// store external ParserRuleSet in the local hashtable for
				// faster lookups later
				if (rules != null) ruleSets.put(setName, rules);
			}
		}

		if (rules == null)
		{
			System.err.println("Unresolved delegate target: " + setName);
		}

		return rules;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		if (name == null) throw new NullPointerException();

		this.name = name;
		rulePfx = name.concat("::");
	}

	public static void addTokenMarker(String name, TokenMarker tokenMarker)
	{
		tokenMarkers.put(name,tokenMarker);
	}

	public Object clone() throws CloneNotSupportedException
	{
		return new GenericTokenMarker(this);
	}

	// protected members
	protected static Hashtable tokenMarkers = new Hashtable();

	protected static ParserRuleSet getExternalRuleSet(String modeName, String setName)
	{
		GenericTokenMarker marker = (GenericTokenMarker)
			tokenMarkers.get(modeName);

		if (marker == null)
		{
			System.err.println("Unresolved token marker: "
				+ modeName);
			return null;
		}

		return marker.getRuleSet(setName);
	}

	protected byte markTokensImpl(byte token, Segment line, int idx,
		LineInfo info)
	{
		LineContext lastContext =
			(idx == 0 || lineInfo[idx - 1].obj == null)
			? new LineContext(null, getRuleSet("MAIN"))
			: ((LineContext) lineInfo[idx - 1].obj);

		try
		{
			context = (LineContext) lastContext.clone();
		}
		catch (CloneNotSupportedException cnse)
		{
			context = new LineContext();
		}

		lastOffset = lastKeyword = line.offset;
		lineLength = line.count + line.offset;

		int terminateChar = context.rules.getTerminateChar();
		int searchLimit = (terminateChar >= 0 && terminateChar < line.count)
			? line.offset + terminateChar : lineLength;

		escaped = false;

		boolean b;
		ParserRule tempRule;
		Enumeration rule_enum;

		for(pos = line.offset; pos < searchLimit; pos++)
		{
			if (context.parent != null)
			{
				LineContext tempContext = context;

				context = context.parent;

				pattern.array = context.inRule.searchChars;
				pattern.count = context.inRule.sequenceLengths[1];
				pattern.offset = context.inRule.sequenceLengths[0];

				b = handleRule(info,line, context.inRule);

				context = tempContext;

				if (!b)
				{
					if (pos != lastOffset)
					{
						if (context.inRule == null)
						{
							if(context.rules.getKeywords() != null)
							{
								markKeyword(info,line, lastKeyword, pos);
							}

							addToken(info,pos - lastOffset,
								getActionToken(context.rules.getDefault()));
						}
						else if ((context.inRule.action & (NO_LINE_BREAK | NO_WORD_BREAK)) == 0)
						{
							addToken(info,pos - lastOffset,
								getActionToken(context.inRule.action));
						}
						else
						{
							addToken(info,pos - lastOffset, Token.INVALID);
						}
					}

					context = (LineContext) context.parent;

					if ((context.inRule.action & EXCLUDE_MATCH) == EXCLUDE_MATCH)
					{
						addToken(info,pattern.count,
							getActionToken(context.rules.getDefault()));
					}
					else
					{
						addToken(info,pattern.count, getActionToken(context.inRule.action));
					}

					context.inRule = null;

					lastKeyword = lastOffset = pos + pattern.count;

					pos += (pattern.count - 1); // move pos to last character of match sequence

					continue;
				}
			}

			if (context.rules.getEscapeRule() != null)
			{
				// assign tempPattern to mutable "buffer" pattern
				Segment tempPattern = pattern;
				// swap in the escape pattern
				pattern = context.rules.getEscapePattern();

				b = handleRule(info,line, context.rules.getEscapeRule());

				// swap back the buffer pattern
				pattern = tempPattern;

				if (!b) continue;
			}

			if (context.inRule != null && (context.inRule.action & SOFT_SPAN) == 0)
			{
				// we are in a hard span rule, so see if context.inRule matches
				pattern.array = context.inRule.searchChars;
				pattern.count = context.inRule.sequenceLengths[1];
				pattern.offset = context.inRule.sequenceLengths[0];

				if (handleRule(info,line, context.inRule) && escaped)
					escaped = false;
			}
			else
			{
				// otherwise, if this isn't a hard span, check every rule
				rule_enum = context.rules.getRules();

				while (rule_enum.hasMoreElements())
				{
					tempRule = (ParserRule) rule_enum.nextElement();

					pattern.array = tempRule.searchChars;

					if (context.inRule == tempRule && (tempRule.action & SPAN) == SPAN)
					{
						pattern.count = tempRule.sequenceLengths[1];
						pattern.offset = tempRule.sequenceLengths[0];
					}
					else
					{
						pattern.count = tempRule.sequenceLengths[0];
						pattern.offset = 0;
					}

					// stop checking rules if there was a match and go to next pos
					if (!handleRule(info,line, tempRule)) break;
				}
			}
		}

		// check for keywords at the line's end
		if(context.rules.getKeywords() != null && context.inRule == null)
		{
			markKeyword(info,line, lastKeyword, lineLength);
		}

		// mark all remaining characters
		if(lastOffset != lineLength)
		{
			if (context.inRule == null)
			{
				addToken(info,lineLength - lastOffset,
					getActionToken(context.rules.getDefault()));
			}
			else if (
				(context.inRule.action & SPAN) == SPAN &&
				(context.inRule.action & (NO_LINE_BREAK | NO_WORD_BREAK)) != 0
			)
			{
				addToken(info,lineLength - lastOffset,Token.INVALID);
				context.inRule = null;
			}
			else
			{
				addToken(info,lineLength - lastOffset, getActionToken(context.inRule.action));

				if((context.inRule.action & MARK_FOLLOWING) == MARK_FOLLOWING)
				{
					context.inRule = null;
				}
			}
		}

		lineInfo[idx].obj = context;

		return getActionToken((context.inRule == null) ? context.rules.getDefault() :
			context.inRule.action);
	}

	/**
	 * Checks if the rule matches the line at the current position
	 * and handles the rule if it does match
	 * @param line Segment to check rule against
	 * @param checkRule ParserRule to check against line
	 * @return true,  keep checking other rules
	 *     <br>false, stop checking other rules
	 */
	protected boolean handleRule(LineInfo info, Segment line, ParserRule checkRule)
	{
		if (pattern.count == 0) return true;

		if (lineLength - pos < pattern.count) return true;

		char a, b;
		for (int k = 0; k < pattern.count; k++)
		{
			a = pattern.array[pattern.offset + k];
			b = line.array[pos + k];

			// break out and check the next rule if there is a mismatch
			if (
				!(
					a == b ||
					context.rules.getIgnoreCase() &&
					(
						Character.toLowerCase(a) == b ||
						a == Character.toLowerCase(b)
					)
				)
			) return true;
		}

		// check for escape sequences
		if (escaped || (checkRule.action & IS_ESCAPE) == IS_ESCAPE)
		{
			escaped = !escaped;
			pos += pattern.count - 1;
			return false;
		}

		// handle soft spans
		if (context.inRule != checkRule && context.inRule != null
			&& (context.inRule.action & SOFT_SPAN) != 0)
		{
			if ((context.inRule.action & NO_WORD_BREAK) == NO_WORD_BREAK)
			{
				addToken(info,pos - lastOffset, Token.INVALID);
			}
			else
			{
				addToken(info,pos - lastOffset,getActionToken(context.inRule.action));
			}
			lastOffset = lastKeyword = pos;
			context.inRule = null;
		}

		if (context.inRule == null)
		{
			if ((checkRule.action & AT_LINE_START) == AT_LINE_START)
			{
				if (
					(((checkRule.action & MARK_PREVIOUS) != 0) ?
					lastKeyword :
					pos) != line.offset
				)
				{
					return true;
				}
			}

			/* check to see if previous sequence is a keyword.
			 * skip this if the matching rule colorizes the previous
			 * sequence. */
			if ((checkRule.action & MARK_PREVIOUS) != MARK_PREVIOUS)
			{
				if (context.rules.getKeywords() != null)
				{
					markKeyword(info,line, lastKeyword, pos);
				}

				lastKeyword = pos + pattern.count;

				if ((checkRule.action & WHITESPACE) == WHITESPACE)
				{
					return false; // break out of inner for loop to check next char
				}

				// mark previous sequence as NULL (plain text)
				if (lastOffset < pos)
				{
					addToken(info,pos - lastOffset,
						getActionToken(context.rules.getDefault()));
				}
			}

			if ((checkRule.action & MAJOR_ACTIONS) == 0)
			{
				// this is a plain sequence rule
				addToken(info,pattern.count, getActionToken(checkRule.action));
				lastOffset = pos + pattern.count;
			}
			else if ((checkRule.action & SPAN) == SPAN)
			{
				context.inRule = checkRule;
				if ((checkRule.action & EXCLUDE_MATCH) == EXCLUDE_MATCH)
				{
					addToken(info,pattern.count,
						getActionToken(context.rules.getDefault()));
					lastOffset = pos + pattern.count;
				}
				else
				{
					lastOffset = pos;
				}

				if ((checkRule.action & DELEGATE) == DELEGATE)
				{
					String setName = new String(checkRule.searchChars,
						checkRule.sequenceLengths[0] + checkRule.sequenceLengths[1],
						checkRule.sequenceLengths[2]);

					ParserRuleSet delegateSet = getRuleSet(setName);

					if (delegateSet != null)
					{
						if ((checkRule.action & EXCLUDE_MATCH) == EXCLUDE_MATCH)
						{
							addToken(info,pattern.count,
								getActionToken(context.rules.getDefault()));
						}
						else
						{
							addToken(info,pattern.count, getActionToken(checkRule.action));
						}
						lastKeyword = lastOffset = pos + pattern.count;

						context = new LineContext(delegateSet, context);
					}
				}
			}
			else if ((checkRule.action & EOL_SPAN) == EOL_SPAN)
			{
				if ((checkRule.action & EXCLUDE_MATCH) == EXCLUDE_MATCH)
				{
					addToken(info,pattern.count,
						getActionToken(context.rules.getDefault()));
					addToken(info,lineLength - (pos + pattern.count),
						getActionToken(checkRule.action));
				}
				else
				{
					addToken(info,lineLength - pos,
						getActionToken(checkRule.action));
				}
				lastOffset = lineLength;
				lastKeyword = lineLength;
				pos = lineLength;
				return false;
			}
			else if ((checkRule.action & MARK_PREVIOUS) == MARK_PREVIOUS)
			{
				if ((checkRule.action & EXCLUDE_MATCH) == EXCLUDE_MATCH)
				{
					addToken(info,pos - lastOffset, getActionToken(checkRule.action));
					addToken(info,pattern.count,
						getActionToken(context.rules.getDefault()));
				}
				else
				{
					addToken(info,(pos + pattern.count) - lastOffset,
						getActionToken(checkRule.action));
				}
				lastOffset = pos + pattern.count;
			}
			else if ((checkRule.action & MARK_FOLLOWING) == MARK_FOLLOWING)
			{
				context.inRule = checkRule;
				if ((checkRule.action & EXCLUDE_MATCH) == EXCLUDE_MATCH)
				{
					addToken(info,pattern.count,
						getActionToken(context.rules.getDefault()));
					lastOffset = pos + pattern.count;
				}
				else
				{
					lastOffset = pos;
				}
			}
			else
			{
				// UNHANDLED MAJOR ACTION!!! SHOULD NOT HAPPEN!!!
			}
			pos += (pattern.count - 1); // move pos to last character of match sequence
			return false; // break out of inner for loop to check next char
		}
		else if ((checkRule.action & SPAN) == SPAN)
		{
			if ((checkRule.action & DELEGATE) != DELEGATE)
			{
				context.inRule = null;
				if ((checkRule.action & EXCLUDE_MATCH) == EXCLUDE_MATCH)
				{
					addToken(info,pos - lastOffset, getActionToken(checkRule.action));
					addToken(info,pattern.count,
						getActionToken(context.rules.getDefault()));
				}
				else
				{
					addToken(info,(pos + pattern.count) - lastOffset,
						getActionToken(checkRule.action));
				}
				lastKeyword = lastOffset = pos + pattern.count;

				pos += (pattern.count - 1); // move pos to last character of match sequence
			}

			return false; // break out of inner for loop to check next char
		}
		return true;
	}

	protected void markKeyword(LineInfo info, Segment line, int start, int end)
	{
		int len = end - start;

		byte id = context.rules.getKeywords().lookup(line, start, len);

		if(id != Token.NULL)
		{
			if(start != lastOffset)
			{
				addToken(info,start - lastOffset, getActionToken(
					context.rules.getDefault()));
			}
			addToken(info,len, id);
			lastOffset = end;
		}
	}

	protected static final byte getActionToken(int action)
	{
		// switch on the masked action token
		switch (action & ACTION_TOKENS)
		{
			case KEYWORD1:
				return Token.KEYWORD1;
			case KEYWORD2:
				return Token.KEYWORD2;
			case KEYWORD3:
				return Token.KEYWORD3;
			case LITERAL1:
				return Token.LITERAL1;
			case LITERAL2:
				return Token.LITERAL2;
			case COMMENT1:
				return Token.COMMENT1;
			case COMMENT2:
				return Token.COMMENT2;
			case LABEL:
				return Token.LABEL;
			case OPERATOR:
				return Token.OPERATOR;
			case INVALID:
				return Token.INVALID;
			case NULL: default:
				return Token.NULL;
		}
	}

	// private members
	private static final int SOFT_SPAN = MARK_FOLLOWING | NO_WORD_BREAK;

	private String name;
	private String rulePfx;
	private Hashtable ruleSets;

	private LineContext context;
	private Segment pattern = new Segment(new char[0],0,0);
	private int lastOffset;
	private int lastKeyword;
	private int lineLength;
	private int pos;
	private boolean escaped;

	// private constructor for cloning (baaa)
	private GenericTokenMarker(GenericTokenMarker copy)
	{
		name = copy.name;
		rulePfx = copy.rulePfx;
		ruleSets = copy.ruleSets;
	}
}
