/*
 * ActionListHandler.java - XML handler for action files
 * Copyright (C) 2000 Slava Pestov
 * Portions copyright (C) 1999 mike dillon
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

package org.gjt.sp.jedit;

import com.microstar.xml.*;
import java.io.*;
import java.util.Stack;
import org.gjt.sp.util.Log;

class ActionListHandler extends HandlerBase
{
	ActionListHandler(String path)
	{
		this.path = path;
	}

	public Object resolveEntity(String publicId, String systemId)
	{
		if("actions.dtd".equals(systemId))
		{
			try
			{
				return new BufferedReader(new InputStreamReader(
					getClass().getResourceAsStream("actions.dtd")));
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,this,"Error while opening"
					+ " actions.dtd:");
				Log.log(Log.ERROR,this,e);
				System.exit(1);
			}
		}

		return null;
	}

	public void attribute(String aname, String value, boolean isSpecified)
	{
		aname = (aname == null) ? null : aname.intern();
		value = (value == null) ? null : value.intern();

		if(aname == "NAME")
			actionName = value;
		else if(aname == "NO_REPEAT")
			noRepeat = (value == "TRUE");
		else if(aname == "NO_RECORD")
			noRecord = (value == "TRUE");
	}

	public void doctypeDecl(String name, String publicId,
		String systemId) throws Exception
	{
		if("ACTIONS".equalsIgnoreCase(name))
			return;

		Log.log(Log.ERROR,this,path + ": DOCTYPE must be ACTIONS");
		System.exit(1);
	}

	public void charData(char[] c, int off, int len)
	{
		String tag = peekElement();
		String text = new String(c, off, len);

		if (tag == "ACTION_PERFORMED")
		{
			actionPerformed = text;
		}
		else if (tag == "IS_SELECTED")
		{
			isSelected = text;
		}
	}

	public void startElement(String tag)
	{
		tag = pushElement(tag);

		if (tag == "ACTION")
		{
			actionPerformed = null;
			isSelected = null;
		}
	}

	public void endElement(String name)
	{
		if(name == null)
			return;

		String tag = peekElement();

		if(name.equalsIgnoreCase(tag))
		{
			if(tag == "ACTION")
			{
				jEdit.addAction(new BeanShellAction(actionName,
					actionPerformed,isSelected,noRepeat,
					noRecord));
			}

			popElement();
		}
		else
		{
			// can't happen
			throw new InternalError();
		}
	}

	public void startDocument()
	{
		try
		{
			pushElement(null);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	// end HandlerBase implementation

	// private members
	private String path;

	private String actionName;
	private String actionPerformed;
	private String isSelected;

	private boolean noRepeat;
	private boolean noRecord;

	private Stack stateStack;

	private String pushElement(String name)
	{
		if (stateStack == null) stateStack = new Stack();

		name = (name == null) ? null : name.intern();

		stateStack.push(name);

		return name;
	}

	private String peekElement()
	{
		if (stateStack == null) stateStack = new Stack();

		return (String) stateStack.peek();
	}

	private String popElement()
	{
		if (stateStack == null) stateStack = new Stack();

		return (String) stateStack.pop();
	}
}
