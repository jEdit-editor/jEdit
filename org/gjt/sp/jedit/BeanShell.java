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

import bsh.Interpreter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Segment;
import javax.swing.JFileChooser;
import java.lang.reflect.InvocationTargetException;
import java.io.*;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.util.Log;

public class BeanShell
{
	public static void evalSelection(View view, JEditTextArea textArea)
	{
		String command = textArea.getSelectedText();
		if(command == null)
		{
			view.getToolkit().beep();
			return;
		}
		Object returnValue = eval(view,command);
		if(returnValue != null)
			textArea.setSelectedText(returnValue.toString());
	}

	public static void eval(View view)
	{
		String command = GUIUtilities.input(view,"beanshell-eval-input",null);
		if(command != null)
		{
			Object returnValue = eval(view,command);
			if(returnValue != null)
			{
				String[] args = { returnValue.toString() };
				GUIUtilities.message(view,"beanshell-eval",args);
			}
		}
	}

	public static void showRunScriptDialog(View view)
	{
		String path = GUIUtilities.showFileDialog(view,
			null,JFileChooser.OPEN_DIALOG);
		if(path != null)
			runScript(view,path);
	}

	public static void runScript(View view, String path)
	{
		Reader in;
		Buffer buffer = jEdit.getBuffer(path);
		if(buffer != null)
		{
			Segment seg = new Segment();
			try
			{
				buffer.getText(0,buffer.getLength(),seg);
			}
			catch(BadLocationException e)
			{
				// XXX
				throw new InternalError();
			}

			in = new CharArrayReader(seg.array,seg.offset,seg.count);
		}
		else
		{
			try
			{
				in = new BufferedReader(new FileReader(path));
			}
			catch(FileNotFoundException e)
			{
				GUIUtilities.error(view,"beanshell-notfound",
					new String[] { path });
				return;
			}
		}

		if(view != null)
		{
			EditPane editPane = view.getEditPane();
			interp.setVariable("view",view);
			interp.setVariable("editPane",editPane);
			interp.setVariable("buffer",editPane.getBuffer());
			interp.setVariable("textArea",editPane.getTextArea());
		}

		try
		{
			running = true;

			interp.eval(in);
		}
		catch(Throwable e)
		{
			if(e instanceof InvocationTargetException)
				e = ((InvocationTargetException)e).getTargetException();

			Log.log(Log.ERROR,BeanShell.class,e);
			GUIUtilities.error(view,"beanshell-error",
				new String[] { path, e.getMessage() });
		}
		finally
		{
			running = false;
		}

		if(view != null)
		{
			interp.setVariable("view",null);
			interp.setVariable("editPane",null);
			interp.setVariable("buffer",null);
			interp.setVariable("textArea",null);
		}
	}

	public static Object eval(View view, String command)
	{
		if(view != null)
		{
			EditPane editPane = view.getEditPane();
			interp.setVariable("view",view);
			interp.setVariable("editPane",editPane);
			interp.setVariable("buffer",editPane.getBuffer());
			interp.setVariable("textArea",editPane.getTextArea());
		}

		Object returnValue;
		try
		{
			returnValue = interp.eval(command);
		}
		catch(Throwable e)
		{
			returnValue = null;
			if(e instanceof InvocationTargetException)
				e = ((InvocationTargetException)e).getTargetException();

			Log.log(Log.ERROR,BeanShell.class,e);
			GUIUtilities.error(view,"beanshell-error",
				new String[] { command, e.getMessage() });
		}

		if(view != null)
		{
			interp.setVariable("view",null);
			interp.setVariable("editPane",null);
			interp.setVariable("buffer",null);
			interp.setVariable("textArea",null);
		}

		return returnValue;
	}

	/**
	 * Returns if a BeanShell script or macro is currently running.
	 * @since jEdit 2.7pre2
	 */
	public static boolean isScriptRunning()
	{
		return running;
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
	private static boolean running;
}
