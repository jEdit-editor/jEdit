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

import bsh.BshMethod;
import bsh.Interpreter;
import bsh.NameSpace;
import bsh.TargetError;
import javax.swing.text.BadLocationException;
import javax.swing.text.Segment;
import javax.swing.JFileChooser;
import java.lang.reflect.InvocationTargetException;
import java.io.*;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.util.Log;

public class BeanShell
{
	/**
	 * Evaluates the text selected in the specified text area.
	 * @since jEdit 2.7pre2
	 */
	public static void evalSelection(View view, JEditTextArea textArea)
	{
		String command = textArea.getSelectedText();
		if(command == null)
		{
			view.getToolkit().beep();
			return;
		}
		Object returnValue = eval(view,command,false);
		if(returnValue != null)
			textArea.setSelectedText(returnValue.toString());
	}

	/**
	 * Prompts for a BeanShell expression to evaluate;
	 * @since jEdit 2.7pre2
	 */
	public static void showEvaluateDialog(View view)
	{
		String command = GUIUtilities.input(view,"beanshell-eval-input",null);
		if(command != null)
		{
			if(!command.endsWith(";"))
				command = command + ";";

			int repeat = view.getInputHandler().getRepeatCount();

			if(view.getMacroRecorder() != null)
			{
				view.getMacroRecorder().record(repeat,command);
			}

			Object returnValue = null;
			try
			{
				for(int i = 0; i < repeat; i++)
				{
					returnValue = eval(view,command,true);
				}
			}
			catch(Error t)
			{
				// BeanShell error occured, abort execution
			}

			if(returnValue != null)
			{
				String[] args = { returnValue.toString() };
				GUIUtilities.message(view,"beanshell-eval",args);
			}
		}
	}

	/**
	 * Prompts for a BeanShell script to run.
	 * @since jEdit 2.7pre2
	 */
	public static void showRunScriptDialog(View view)
	{
		String path = GUIUtilities.showFileDialog(view,
			null,JFileChooser.OPEN_DIALOG);
		if(path != null)
		{
			Buffer buffer = view.getBuffer();
			try
			{
				buffer.beginCompoundEdit();
				runScript(view,path,true,false);
			}
			finally
			{
				buffer.endCompoundEdit();
			}
		}
	}

	/**
	 * Runs a BeanShell script.
	 * @since jEdit 2.7pre3
	 */
	public static void runScript(View view, String path,
		boolean ownNamespace, boolean rethrowBshErrors)
	{
		Reader in;
		Buffer buffer = jEdit.getBuffer(path);
		if(buffer != null && buffer.isLoaded())
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

		NameSpace namespace = interp.getNameSpace();
		if(ownNamespace)
			namespace = new NameSpace(namespace,"macro namespace");

		try
		{
			if(view != null)
			{
				EditPane editPane = view.getEditPane();
				namespace.setVariable("view",view);
				namespace.setVariable("editPane",editPane);
				namespace.setVariable("buffer",editPane.getBuffer());
				namespace.setVariable("textArea",editPane.getTextArea());
			}

			running = true;

			/* if(ownNamespace)
			{
				// terrible hack
				interp.eval(new InputStreamReader(
					BeanShell.class.getResourceAsStream(
					"/org/gjt/sp/jedit/jedit.bsh")),
					namespace,"jedit.bsh");
			} */

			interp.eval(in,namespace,path);
		}
		catch(Throwable e)
		{
			if(e instanceof TargetError)
				e = ((TargetError)e).getTarget();

			if(e instanceof InvocationTargetException)
				e = ((InvocationTargetException)e).getTargetException();

			Log.log(Log.ERROR,BeanShell.class,e);
			GUIUtilities.error(view,"beanshell-error",
				new String[] { path, e.toString() });

			if(e instanceof Error && rethrowBshErrors)
				throw (Error)e;
		}
		finally
		{
			running = false;
		}
	}

	/**
	 * Evaluates the specified BeanShell expression.
	 * @param view The view (may be null)
	 * @param command The expression
	 * @param rethrowBshErrors If true, BeanShell errors will
	 * be re-thrown to the caller
	 * @since jEdit 2.7pre3
	 */
	public static Object eval(View view, String command,
		boolean rethrowBshErrors)
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

			if(e instanceof TargetError)
				e = ((TargetError)e).getTarget();

			if(e instanceof InvocationTargetException)
				e = ((InvocationTargetException)e).getTargetException();

			Log.log(Log.ERROR,BeanShell.class,e);
			GUIUtilities.error(view,"beanshell-error",
				new String[] { command, e.toString() });

			if(e instanceof Error && rethrowBshErrors)
				throw (Error)e;
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
	 * Returns the specified method reference.
	 * @param method The method name
	 * @since jEdit 2.7pre2
	 */
	public static BshMethod getMethod(String name)
	{
		return interp.getNameSpace().getMethod(name);
	}

	/**
	 * Invokes the specified method reference.
	 * @param view The view
	 * @param method The method reference
	 * @param args Arguments to pass the method
	 * @since jEdit 2.7pre2
	 */
	public static Object invokeMethod(View view, BshMethod method, Object[] args)
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
			returnValue = method.invokeDeclaredMethod(args,interp);
		}
		catch(Throwable e)
		{
			returnValue = null;

			if(e instanceof TargetError)
				e = ((TargetError)e).getTarget();

			if(e instanceof InvocationTargetException)
				e = ((InvocationTargetException)e).getTargetException();

			Log.log(Log.ERROR,BeanShell.class,e);
			GUIUtilities.error(view,"beanshell-error",
				new String[] { String.valueOf(method), e.toString() });
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

	/**
	 * Returns the BeanShell interpreter instance.
	 * @since jEdit 3.0pre5
	 */
	public static Interpreter getInterpreter()
	{
		return interp;
	}

	static void init()
	{
		Log.log(Log.DEBUG,BeanShell.class,"Initializing BeanShell"
			+ " interpreter");

		try
		{
			interp = new Interpreter();
			interp.setVariable("classLoader",new JARClassLoader());
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
