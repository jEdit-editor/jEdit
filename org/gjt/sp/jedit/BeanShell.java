/*
 * BeanShell.java - BeanShell scripting support
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

package org.gjt.sp.jedit;

import bsh.*;
import java.io.*;
import org.gjt.sp.util.Log;

public class BeanShell
{
	public static Object eval(View view, String command)
	{
		if(view != null)
		{
			interp.setVariable("view",view);
			interp.setVariable("buffer",view.getBuffer());
			interp.setVariable("textArea",view.getTextArea());
		}

		try
		{
			return interp.eval(command);
		}
		catch(Exception e)
		{
			e.printStackTrace(); // XXX
		}

		if(view != null)
		{
			interp.setVariable("view",null);
			interp.setVariable("buffer",null);
			interp.setVariable("textArea",null);
		}

		return null;
	}

	static void init()
	{
		Log.log(Log.DEBUG,BeanShell.class,"Initializing BeanShell"
			+ " interpreter");
		interp = new Interpreter();
		try
		{
			interp.eval(new BufferedReader(new InputStreamReader(
				BeanShell.class.getResourceAsStream("jedit.bsh"))));
		}
		catch(Throwable t)
		{
			Log.log(Log.ERROR,BeanShell.class,"Error loading jedit.bsh from JAR file:");
			Log.log(Log.ERROR,BeanShell.class,t);
			System.exit(1);
		}
	}

	// private members
	private static Interpreter interp;
}
