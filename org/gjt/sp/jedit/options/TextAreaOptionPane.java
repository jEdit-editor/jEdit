/*
 * TextAreaOptionPane.java - Text area options panel
 * Copyright (C) 1998, 1999, 2000 Slava Pestov
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

package org.gjt.sp.jedit.options;

import javax.swing.*;
import java.awt.*;
import org.gjt.sp.jedit.gui.FontSelector;
import org.gjt.sp.jedit.*;

public class TextAreaOptionPane extends AbstractOptionPane
{
	public TextAreaOptionPane()
	{
		super("textarea");
	}

	public void _init()
	{
		/* Font */
		String _fontFamily = jEdit.getProperty("view.font");
		int _fontStyle;
		try
		{
			_fontStyle = Integer.parseInt(jEdit.getProperty("view.fontstyle"));
		}
		catch(NumberFormatException nf)
		{
			_fontStyle = Font.PLAIN;
		}
		int _fontSize;
		try
		{
			_fontSize = Integer.parseInt(jEdit.getProperty("view.fontsize"));
		}
		catch(NumberFormatException nf)
		{
			_fontSize = 14;
		}
		font = new FontSelector(new Font(_fontFamily,_fontStyle,_fontSize));

		addComponent(jEdit.getProperty("options.textarea.font"),font);

		/* Line highlight */
		lineHighlight = new JCheckBox(jEdit.getProperty("options.textarea"
			+ ".lineHighlight"));
		lineHighlight.setSelected(jEdit.getBooleanProperty("view.lineHighlight"));
		addComponent(lineHighlight);

		/* Bracket highlight */
		bracketHighlight = new JCheckBox(jEdit.getProperty("options.textarea"
			+ ".bracketHighlight"));
		bracketHighlight.setSelected(jEdit.getBooleanProperty(
			"view.bracketHighlight"));
		addComponent(bracketHighlight);

		/* EOL markers */
		eolMarkers = new JCheckBox(jEdit.getProperty("options.textarea"
			+ ".eolMarkers"));
		eolMarkers.setSelected(jEdit.getBooleanProperty("view.eolMarkers"));
		addComponent(eolMarkers);

		/* Wrap guide */
		wrapGuide = new JCheckBox(jEdit.getProperty("options.textarea"
			+ ".wrapGuide"));
		wrapGuide.setSelected(jEdit.getBooleanProperty("view.wrapGuide"));
		addComponent(wrapGuide);

		/* Blinking caret */
		blinkCaret = new JCheckBox(jEdit.getProperty("options.textarea"
			+ ".blinkCaret"));
		blinkCaret.setSelected(jEdit.getBooleanProperty("view.caretBlink"));
		addComponent(blinkCaret);

		/* Block caret */
		blockCaret = new JCheckBox(jEdit.getProperty("options.textarea"
			+ ".blockCaret"));
		blockCaret.setSelected(jEdit.getBooleanProperty("view.blockCaret"));
		addComponent(blockCaret);

		/* Electric borders */
		electricBorders = new JCheckBox(jEdit.getProperty("options.textarea"
			+ ".electricBorders"));
		electricBorders.setSelected(!"0".equals(jEdit.getProperty(
			"view.electricBorders")));
		addComponent(electricBorders);

		/* Smart home/end */
		homeEnd = new JCheckBox(jEdit.getProperty("options.textarea"
			+ ".homeEnd"));
		homeEnd.setSelected(jEdit.getBooleanProperty("view.homeEnd"));
		addComponent(homeEnd);

		
	}

	public void _save()
	{
		Font _font = font.getFont();
		jEdit.setProperty("view.font",_font.getFamily());
		jEdit.setProperty("view.fontsize",String.valueOf(_font.getSize()));
		jEdit.setProperty("view.fontstyle",String.valueOf(_font.getStyle()));

		jEdit.setBooleanProperty("view.lineHighlight",lineHighlight
			.isSelected());
		jEdit.setBooleanProperty("view.bracketHighlight",bracketHighlight
			.isSelected());
		jEdit.setBooleanProperty("view.eolMarkers",eolMarkers
			.isSelected());
		jEdit.setBooleanProperty("view.wrapGuide",wrapGuide
			.isSelected());
		jEdit.setBooleanProperty("view.caretBlink",blinkCaret.isSelected());
		jEdit.setBooleanProperty("view.blockCaret",blockCaret.isSelected());
		jEdit.setProperty("view.electricBorders",electricBorders
			.isSelected() ? "3" : "0");
		jEdit.setBooleanProperty("view.homeEnd",homeEnd.isSelected());
	}

	// private members
	private FontSelector font;
	private JCheckBox lineHighlight;
	private JCheckBox bracketHighlight;
	private JCheckBox eolMarkers;
	private JCheckBox wrapGuide;
	private JCheckBox blinkCaret;
	private JCheckBox blockCaret;
	private JCheckBox electricBorders;
	private JCheckBox homeEnd;
}

/*
 * Change Log:
 * $Log$
 * Revision 1.5  2000/11/07 10:08:32  sp
 * Options dialog improvements, documentation changes, bug fixes
 *
 * Revision 1.4  2000/11/05 05:25:46  sp
 * Word wrap, format and remove-trailing-ws commands from TextTools moved into core
 *
 * Revision 1.3  2000/09/26 10:19:47  sp
 * Bug fixes, spit and polish
 *
 * Revision 1.2  2000/08/11 09:06:52  sp
 * Browser option pane
 *
 * Revision 1.1  2000/08/10 08:30:41  sp
 * VFS browser work, options dialog work, more random tweaks
 *
 * Revision 1.1  2000/08/05 07:16:12  sp
 * Global options dialog box updated, VFS browser now supports right-click menus
 *
 */
