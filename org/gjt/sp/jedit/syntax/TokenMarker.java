/*
 * TokenMarker.java - Tokenizes lines of text
 * Copyright (C) 1998, 1999, 2000 Slava Pestov
 * Copyright (C) 1999, 2000 mike dillon
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

import javax.swing.text.*;
import java.awt.*;
import java.util.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;

/**
 * A token marker splits lines of text into tokens. Each token carries
 * a length field and an identification tag that can be mapped to a color
 * or font style for painting that token.
 *
 * @author Slava Pestov, mike dillon
 * @version $Id$
 *
 * @see org.gjt.sp.jedit.syntax.Token
 */
public class TokenMarker implements Cloneable
{
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

	public TokenMarker()
	{
		ruleSets = new Hashtable(64);
		lineInfo = new LineInfo[0];
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

		rules = (ParserRuleSet) ruleSets.get(setName);

		if (rules == null && !setName.startsWith(rulePfx))
		{
			int delim = setName.indexOf("::");

			String modeName = setName.substring(0, delim);

			Mode mode = jEdit.getMode(modeName);
			if(mode == null)
			{
				Log.log(Log.ERROR,TokenMarker.class,
					"Unknown edit mode: " + modeName);
				rules = null;
			}
			else
			{
				TokenMarker marker = mode.getTokenMarker();

				if (marker == null)
				{
					Log.log(Log.ERROR,TokenMarker.class,
					"Cannot delegate to plain text mode");
					rules = null;
				}
				else
				{
					rules = marker.getRuleSet(setName);
				}
			}

			// store external ParserRuleSet in the local hashtable for
			// faster lookups later
			ruleSets.put(setName, rules);
		}

		if (rules == null)
		{
			Log.log(Log.ERROR,this,"Unresolved delegate target: " + setName);
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

	/**
	 * Paints the specified line onto the graphics context.
	 * @param buffer The buffer
	 * @param lineIndex The line
	 * @param styles The syntax style list
	 * @param expander The tab expander used to determine tab stops. May
	 * be null
	 * @param gfx The graphics context
	 * @param x The x co-ordinate
	 * @param y The y co-ordinate
	 * @return The x co-ordinate, plus the width of the painted string
	 * @since jEdit 2.7pre1
	 */
	public int paintSyntaxLine(Buffer buffer, int lineIndex,
		SyntaxStyle[] styles, TabExpander expander, Graphics gfx,
		Color background, int x, int y)
	{
		LineInfo info = lineInfo[lineIndex];
		if(info.tokensValid)
		{
			// have to do this 'manually'
			Element lineElement = buffer.getDefaultRootElement()
				.getElement(lineIndex);
			int lineStart = lineElement.getStartOffset();
			try
			{
				buffer.getText(lineStart,lineElement.getEndOffset()
					- lineStart - 1,line);
			}
			catch(BadLocationException e)
			{
				Log.log(Log.ERROR,this,e);
			}
		}
		else
			markTokens(buffer,lineIndex);

		Token tokens = info.firstToken;

		// the above should leave the text in the 'line' segment

		Font defaultFont = gfx.getFont();
		Color defaultColor = gfx.getColor();

		int originalOffset = line.offset, originalCount = line.count;

		int offset = 0;
		for(;;)
		{
			byte id = tokens.id;
			if(id == Token.END)
				break;

			int length = tokens.length;
			line.count = length;
			if(id == Token.NULL)
			{
				if(!defaultColor.equals(gfx.getColor()))
					gfx.setColor(defaultColor);
				if(!defaultFont.equals(gfx.getFont()))
					gfx.setFont(defaultFont);
			}
			else
			{
				Color bg = styles[id].getBackgroundColor();
				if (bg != null)
				{
					FontMetrics fm = styles[id].getFontMetrics(defaultFont);
					int width   = Utilities.getTabbedTextWidth(line, fm, x, expander, 0); 
					int height  = fm.getHeight();
					int descent = fm.getDescent();
					int leading = fm.getLeading();
					gfx.setColor(background);
					gfx.setXORMode(bg);
					gfx.fillRect(x, y - height + descent + leading, width, height);
					gfx.setPaintMode();
				}

				styles[id].setGraphicsFlags(gfx,defaultFont);
			}

			x = Utilities.drawTabbedText(line,x,y,gfx,expander,0);
			line.offset += length;
			offset += length;

			tokens = tokens.next;
		}

		line.offset = originalOffset;
		line.count = originalCount;

		return x;
	}

	/**
	 * Returns the syntax tokens for the specified line.
	 * @param buffer The buffer
	 * @param lineIndex The line number
	 * @since jEdit 2.6pre9
	 */
	public LineInfo markTokens(Buffer buffer, int lineIndex)
	{
		LineInfo info = lineInfo[lineIndex];

		/* If cached tokens are valid, return 'em */
		if(info.tokensValid)
			return info;

		//long _start = System.currentTimeMillis();

		/*
		 * Else, go up to 100 lines back, looking for a line with
		 * cached tokens. Tokenize from that line to this line.
		 */
		int start = Math.max(0,lineIndex - 100) - 1;
		int end = Math.max(0,lineIndex - 100);

		for(int i = lineIndex - 1; i > end; i--)
		{
			if(lineInfo[i].tokensValid)
			{
				start = i;
				break;
			}
		}

		LineInfo prev;
		if(start == -1)
			prev = null;
		else
			prev = lineInfo[start];

		//System.err.println("i=" + lineIndex + ",start=" + start);
		Element map = buffer.getDefaultRootElement();

		for(int i = start + 1; i <= lineIndex; i++)
		{
			info = lineInfo[i];
			if(info.tokensValid)
			{
				prev = info;
				continue;
			}

			Element lineElement = map.getElement(i);
			int lineStart = lineElement.getStartOffset();
			try
			{
				buffer.getText(lineStart,lineElement.getEndOffset()
					- lineStart - 1,line);
			}
			catch(BadLocationException e)
			{
				Log.log(Log.ERROR,this,e);
			}

			/* Prepare for tokenization */
			info.lastToken = null;

			ParserRule oldRule = info.context.inRule;
			LineContext oldParent = info.context.parent;
			markTokensImpl(prev,info);
			ParserRule newRule = info.context.inRule;
			LineContext newParent = info.context.parent;

			info.tokensValid = true;

			if(i != lastTokenizedLine)
			{
				nextLineRequested = false;
				lastTokenizedLine = i;
			}

			nextLineRequested |= (oldRule != newRule || oldParent != newParent);

			addToken(info,0,Token.END);

			prev = info;
		}

		if(nextLineRequested && length - lineIndex > 1)
		{
			linesChanged(lineIndex + 1,length - lineIndex - 1);
		}

		//System.err.println(System.currentTimeMillis() - _start);

		return info;
	}

	/**
	 * Store the width of a line, in pixels.
	 * @param lineIndex The line number
	 * @param width The width
	 */
	public boolean setLineWidth(int lineIndex, int width)
	{
		LineInfo info = lineInfo[lineIndex];
		int oldWidth = info.width;
		info.width = width;
		return width != oldWidth;
	}

	/**
	 * Returns the maximum line width in the specified line range.
	 * @param start The first line
	 * @param len The number of lines
	 */
	public int getMaxLineWidth(int start, int len)
	{
		int retVal = 0;
		for(int i = start; i <= start + len; i++)
		{
			if(i >= length)
				break;
			retVal = Math.max(lineInfo[i].width,retVal);
		}
		return retVal;
	}

	/**
	 * Informs the token marker that lines have been inserted into
	 * the document. This inserts a gap in the <code>lineInfo</code>
	 * array.
	 * @param index The first line number
	 * @param lines The number of lines 
	 */
	public void insertLines(int index, int lines)
	{
		if(lines <= 0)
			return;
		length += lines;
		ensureCapacity(length);
		int len = index + lines;
		System.arraycopy(lineInfo,index,lineInfo,len,
			lineInfo.length - len);

		ParserRuleSet mainSet = getRuleSet(rulePfx.concat("MAIN"));

		for(int i = index + lines - 1; i >= index; i--)
		{
			lineInfo[i] = new LineInfo();
			lineInfo[i].context = new LineContext(null, mainSet);
		}
	}
	
	/**
	 * Informs the token marker that line have been deleted from
	 * the document. This removes the lines in question from the
	 * <code>lineInfo</code> array.
	 * @param index The first line number
	 * @param lines The number of lines
	 */
	public void deleteLines(int index, int lines)
	{
		if (lines <= 0)
			return;
		int len = index + lines;
		length -= lines;
		System.arraycopy(lineInfo,len,lineInfo,
			index,lineInfo.length - len);
	}

	/**
	 * Informs the token marker that lines have changed. This will
	 * invalidate any cached tokens.
	 * @param index The first line number
	 * @param lines The number of lines
	 */
	public void linesChanged(int index, int lines)
	{
		for(int i = 0; i < lines; i++)
		{
			lineInfo[index + i].tokensValid = false;
		}
	}

	/**
	 * Returns the number of lines in this token marker.
	 */
	public int getLineCount()
	{
		return length;
	}

	/*
	 * Returns true if the next line should be repainted. This
	 * will return true after a line has been tokenized that starts
	 * a multiline token that continues onto the next line.
	 */
	public boolean isNextLineRequested()
	{
		return nextLineRequested;
	}

	public Object clone()
	{
		return new TokenMarker(this);
	}

	// private members
	private static final int SOFT_SPAN = MARK_FOLLOWING | NO_WORD_BREAK;

	private String name;
	private String rulePfx;
	private Hashtable ruleSets;

	private Segment line = new Segment(new char[0],0,0);

	private LineInfo[] lineInfo;
	private int length;
	private int lastTokenizedLine = -1;
	private boolean nextLineRequested;

	private LineContext context;
	private Segment pattern = new Segment(new char[0],0,0);
	private int lastOffset;
	private int lastKeyword;
	private int lineLength;
	private int pos;
	private boolean escaped;

	private TokenMarker(TokenMarker copy)
	{
		name = copy.name;
		rulePfx = copy.rulePfx;
		ruleSets = copy.ruleSets;
		lineInfo = new LineInfo[0];	
	}

	private void markTokensImpl(LineInfo prevInfo, LineInfo info)
	{
		LineContext lastContext = (prevInfo == null ? null
			: prevInfo.context);
		if(lastContext == null) lastContext = new LineContext(null,
			getRuleSet(rulePfx.concat("MAIN")));

		context = info.context;

		context.parent = (lastContext.parent == null ? null
			: (LineContext)lastContext.parent.clone());
		context.inRule = lastContext.inRule;
		context.rules = lastContext.rules;

		lastOffset = lastKeyword = line.offset;
		lineLength = line.count + line.offset;

		int terminateChar = context.rules.getTerminateChar();
		int searchLimit = (terminateChar >= 0 && terminateChar < line.count)
			? line.offset + terminateChar : lineLength;

		escaped = false;

		boolean b;
		boolean tempEscaped;
		Segment tempPattern;
		ParserRule rule;
		LineContext tempContext;

		for(pos = line.offset; pos < searchLimit; pos++)
		{
			// if we are not in the top level context, we are delegated
			if (context.parent != null)
			{
				tempContext = context;

				context = context.parent;

				pattern.array = context.inRule.searchChars;
				pattern.count = context.inRule.sequenceLengths[1];
				pattern.offset = context.inRule.sequenceLengths[0];

				b = handleRule(info, line, context.inRule);

				context = tempContext;

				if (!b)
				{
					if (escaped)
					{
						escaped = false;
					}
					else
					{
						if (pos != lastOffset)
						{
							if (context.inRule == null)
							{
								markKeyword(info,line, lastKeyword, pos);

								addToken(info,pos - lastOffset,
									context.rules.getDefault());
							}
							else if ((context.inRule.action & (NO_LINE_BREAK | NO_WORD_BREAK)) == 0)
							{
								addToken(info,pos - lastOffset,
									context.inRule.token);
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
								context.rules.getDefault());
						}
						else
						{
							addToken(info,pattern.count,context.inRule.token);
						}

						context.inRule = null;

						lastKeyword = lastOffset = pos + pattern.count;
					}

					pos += (pattern.count - 1); // move pos to last character of match sequence

					continue;
				}
			}

			// check the escape rule for the current context, if there is one
			if ((rule = context.rules.getEscapeRule()) != null)
			{
				// assign tempPattern to mutable "buffer" pattern
				tempPattern = pattern;

				// swap in the escape pattern
				pattern = context.rules.getEscapePattern();

				tempEscaped = escaped;

				b = handleRule(info, line, rule);

				// swap back the buffer pattern
				pattern = tempPattern;

				if (!b)
				{
					if (tempEscaped) escaped = false;
					continue;
				}
			}

			// if we are inside a span, check for its end sequence
			rule = context.inRule;
			if(rule != null && (rule.action & SPAN) == SPAN)
			{
				pattern.array = rule.searchChars;
				pattern.count = rule.sequenceLengths[1];
				pattern.offset = rule.sequenceLengths[0];

				// if we match the end of the span, or if this is a "hard" span,
				// we continue to the next character; otherwise, we check all
				// applicable rules below
				if (!handleRule(info,line,rule)
					|| (rule.action & SOFT_SPAN) == 0)
				{
					escaped = false;
					continue;
				}
			}

			// now check every rule
			rule = context.rules.getRules(line.array[pos]);
			while(rule != null)
			{
				pattern.array = rule.searchChars;

				if (context.inRule == rule && (rule.action & SPAN) == SPAN)
				{
					pattern.count = rule.sequenceLengths[1];
					pattern.offset = rule.sequenceLengths[0];
				}
				else
				{
					pattern.count = rule.sequenceLengths[0];
					pattern.offset = 0;
				}

				// stop checking rules if there was a match and go to next pos
				if (!handleRule(info,line,rule))
					break;

				rule = rule.next;
			}

			escaped = false;
		}

		// check for keywords at the line's end
		if(context.inRule == null)
			markKeyword(info, line, lastKeyword, lineLength);

		// mark all remaining characters
		if(lastOffset != lineLength)
		{
			if (context.inRule == null)
			{
				addToken(info,lineLength - lastOffset,
					context.rules.getDefault());
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
				addToken(info,lineLength - lastOffset,context.inRule.token);

				if((context.inRule.action & MARK_FOLLOWING) == MARK_FOLLOWING)
				{
					context.inRule = null;
				}
			}
		}

		info.context = context;
	}

	/**
	 * Checks if the rule matches the line at the current position
	 * and handles the rule if it does match
	 * @param line Segment to check rule against
	 * @param checkRule ParserRule to check against line
	 * @return true,  keep checking other rules
	 *     <br>false, stop checking other rules
	 */
	private boolean handleRule(LineInfo info, Segment line, ParserRule checkRule)
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

		if (escaped)
		{
			pos += pattern.count - 1;
			return false;
		}
		else if ((checkRule.action & IS_ESCAPE) == IS_ESCAPE)
		{
			escaped = true;
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
				addToken(info,pos - lastOffset,context.inRule.token);
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

			markKeyword(info, line, lastKeyword, pos);

			if ((checkRule.action & MARK_PREVIOUS) != MARK_PREVIOUS)
			{
				lastKeyword = pos + pattern.count;

				if ((checkRule.action & WHITESPACE) == WHITESPACE)
				{
					return false; // break out of inner for loop to check next char
				}

				// mark previous sequence as NULL (plain text)
				if (lastOffset < pos)
				{
					addToken(info,pos - lastOffset,
						context.rules.getDefault());
				}
			}

			switch(checkRule.action & MAJOR_ACTIONS)
			{
			case 0:
				// this is a plain sequence rule
				addToken(info,pattern.count,checkRule.token);
				lastOffset = pos + pattern.count;

				break;
			case SPAN:
				context.inRule = checkRule;

				if ((checkRule.action & DELEGATE) != DELEGATE)
				{
					if ((checkRule.action & EXCLUDE_MATCH) == EXCLUDE_MATCH)
					{
						addToken(info,pattern.count,
							context.rules.getDefault());
						lastOffset = pos + pattern.count;
					}
					else
					{
						lastOffset = pos;
					}
				}
				else
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
								context.rules.getDefault());
						}
						else
						{
							addToken(info,pattern.count,checkRule.token);
						}
						lastOffset = pos + pattern.count;

						context = new LineContext(delegateSet, context);
					}
				}

				break;
			case EOL_SPAN:
				if ((checkRule.action & EXCLUDE_MATCH) == EXCLUDE_MATCH)
				{
					addToken(info,pattern.count,
						context.rules.getDefault());
					addToken(info,lineLength - (pos + pattern.count),
						checkRule.token);
				}
				else
				{
					addToken(info,lineLength - pos,
						checkRule.token);
				}
				lastOffset = lineLength;
				lastKeyword = lineLength;
				pos = lineLength;

				return false;
			case MARK_PREVIOUS:
				if (lastKeyword > lastOffset)
				{
					addToken(info, lastKeyword - lastOffset,
						context.rules.getDefault());
					lastOffset = lastKeyword;
				}

				if ((checkRule.action & EXCLUDE_MATCH) == EXCLUDE_MATCH)
				{
					addToken(info, pos - lastOffset, checkRule.token);
					addToken(info, pattern.count,
						context.rules.getDefault());
				}
				else
				{
					addToken(info, pos - lastOffset + pattern.count,
						checkRule.token);
				}
				lastOffset = pos + pattern.count;

				break;
			case MARK_FOLLOWING:
				context.inRule = checkRule;
				if ((checkRule.action & EXCLUDE_MATCH) == EXCLUDE_MATCH)
				{
					addToken(info,pattern.count,
						context.rules.getDefault());
					lastOffset = pos + pattern.count;
				}
				else
				{
					lastOffset = pos;
				}

				break;
			default:
				throw new InternalError("Unhandled major action");
			}

			lastKeyword = lastOffset;

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
					addToken(info,pos - lastOffset,checkRule.token);
					addToken(info,pattern.count,
						context.rules.getDefault());
				}
				else
				{
					addToken(info,(pos + pattern.count) - lastOffset,
						checkRule.token);
				}
				lastKeyword = lastOffset = pos + pattern.count;

				pos += (pattern.count - 1); // move pos to last character of match sequence
			}

			return false; // break out of inner for loop to check next char
		}
		return true;
	}

	private void markKeyword(LineInfo info, Segment line, int start, int end)
	{
		KeywordMap keywords = context.rules.getKeywords();

		int len = end - start;

		// do digits
		if(context.rules.getHighlightDigits())
		{
			boolean digit = true;
			char[] array = line.array;
			boolean octal = false;
			boolean hex = false;
			boolean seenSomeDigits = false;
loop:			for(int i = 0; i < len; i++)
			{
				char ch = array[start+i];
				switch(ch)
				{
				case '0':
					if(i == 0)
						octal = true;
					seenSomeDigits = true;
					continue loop;
				case '1': case '2': case '3':
				case '4': case '5': case '6':
				case '7': case '8': case '9':
					seenSomeDigits = true;
					continue loop;
				case 'x': case 'X':
					if(octal && i == 1)
					{
						hex = true;
						continue loop;
					}
					else
						break;
				case 'd': case 'D':
					if(hex)
						continue loop;
					else
						break;
				case 'f': case 'F':
					if(hex || seenSomeDigits)
						continue loop;
					else
						break;
				case 'l': case 'L':
					if(seenSomeDigits)
						continue loop;
					else
						break;
				case 'e': case 'E':
					if(seenSomeDigits)
						continue loop;
					else
						break;
				case 'a': case 'A': case 'b': case 'B':
				case 'c': case 'C':
					if(hex)
						continue loop;
					else
						break;
				case '.': case '-':
					// normally, this shouldn't be
					// necessary, because most modes
					// define '.' and '-' SEQs. However,
					// in props mode, we can't define
					// such a SEQ because it would
					// break the AT_LINE_START
					// MARK_PREVIOUS rule.
					continue loop;
				default:
					break;
				}

				// if we ended up here, then we have found a
				// non-digit character.
				digit = false;
				break loop;
			}

			// if we got this far with digit = true, then the keyword
			// consists of all digits. Add it as such.
			if(digit)
			{
				if(start != lastOffset)
				{
					addToken(info,start - lastOffset,
						context.rules.getDefault());
				}
				addToken(info,len,Token.DIGIT);
				lastKeyword = lastOffset = end;

				return;
			}
		}

		if(keywords != null)
		{
			byte id = keywords.lookup(line, start, len);

			if(id != Token.NULL)
			{
				if(start != lastOffset)
				{
					addToken(info,start - lastOffset,
						context.rules.getDefault());
				}
				addToken(info,len, id);
				lastKeyword = lastOffset = end;
			}
		}
	}

	private void ensureCapacity(int index)
	{
		if(lineInfo.length <= index)
		{
			LineInfo[] lineInfoN = new LineInfo[(index + 1) * 2];
			System.arraycopy(lineInfo,0,lineInfoN,0,
					 lineInfo.length);
			lineInfo = lineInfoN;
		}
	}

	private void addToken(LineInfo info, int length, byte id)
	{
		if(id >= Token.INTERNAL_FIRST && id <= Token.INTERNAL_LAST)
			throw new InternalError("Invalid id: " + id);

		if(length == 0 && id != Token.END)
			return;

		if(info.firstToken == null)
		{
			info.firstToken = new Token(length,id);
			info.lastToken = info.firstToken;
		}
		else if(info.lastToken == null)
		{
			info.lastToken = info.firstToken;
			info.firstToken.length = length;
			info.firstToken.id = id;
		}
		else if(info.lastToken.id == id)
		{
			info.lastToken.length += length;
		}
		else if(info.lastToken.next == null)
		{
			info.lastToken.next = new Token(length,id);
			info.lastToken.next.prev = info.lastToken;
			info.lastToken = info.lastToken.next;
		}
		else
		{
			info.lastToken = info.lastToken.next;
			info.lastToken.length = length;
			info.lastToken.id = id;
		}
	}

	/**
	 * Inner class for storing information about tokenized lines.
	 */
	public static class LineInfo
	{
		/**
		 * The first token of this line.
		 */
		public Token firstToken;

		/**
		 * The last token of this line.
		 */
		public Token lastToken;

		/**
		 * True if the tokens can be used, false if markTokensImpl()
		 * needs to be called.
		 */
		/* package-private */ boolean tokensValid;

		/**
		 * The line context.
		 */
		/* package-private */ LineContext context;

		/**
		 * The line width.
		 */
		/* package-private */ int width;
	}

	public static class LineContext
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
			parent = (lc == null ? null : (LineContext)lc.clone());
		}

		public LineContext(ParserRule r)
		{
			inRule = r;
		}

		public LineContext()
		{
		}

		public Object clone()
		{
			LineContext lc = new LineContext();
			lc.inRule = inRule;
			lc.rules = rules;
			lc.parent = (parent == null) ? null : (LineContext) parent.clone();

			return lc;
		}
	}
}
