/*
 * Registers.java - Register manager
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

package org.gjt.sp.jedit;

import javax.swing.text.*;
import java.awt.datatransfer.*;
import java.awt.Toolkit;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.util.Log;

/**
 * jEdit's registers are an extension of the clipboard metaphor. There
 * can be an unlimited number of registers, each one holding a string,
 * caret position, or file name.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class Registers
{
	/**
	 * Returns the named register.
	 * @param name The name
	 */
	public static Register getRegister(char name)
	{
		if(registers == null || name >= registers.length)
			return null;
		else
			return registers[name];
	}

	/**
	 * Sets a register.
	 * @param name The name
	 * @param value The new value
	 */
	public static void setRegister(char name, Register newRegister)
	{
		if(registers == null)
		{
			registers = new Register[name + 1];
			registers[name] = newRegister;
		}
		else if(name >= registers.length)
		{
			Register[] newRegisters = new Register[
				Math.min(1<<16,name * 2)];
			System.arraycopy(registers,0,newRegisters,0,
				registers.length);
			registers = newRegisters;
			registers[name] = newRegister;
		}
		else
		{
			Register register = registers[name];

			if(register instanceof ClipboardRegister)
			{
				((ClipboardRegister)register).setValue(
					newRegister.toString());
			}
			else
			{
				if(register != null)
					register.dispose();
				registers[name] = newRegister;
			}
		}
	}

	/**
	 * Clears (sets it's value to null) the specified register.
	 * @param name The register name
	 */
	public static void clearRegister(char name)
	{
		if(name >= registers.length)
			return;

		Register register = registers[name];
		if(register instanceof ClipboardRegister)
			((ClipboardRegister)register).setValue("");
		else
		{
			if(register != null)
				register.dispose();
			registers[name] = null;
		}
	}

	/**
	 * Returns an array of all available registers.
	 */
	public static Register[] getRegisters()
	{
		return registers;
	}

	public static interface Register
	{
		public String toString();
		public void dispose();
	}

	public static class CaretRegister implements Register, EBComponent
	{
		private String path;
		private int offset;
		private Buffer buffer;
		private Position pos;

		public CaretRegister(Buffer buffer, int offset)
		{
			path = buffer.getPath();
			this.offset = offset;
			this.buffer = buffer;
			try
			{
				pos = buffer.createPosition(offset);
			}
			catch(BadLocationException bl)
			{
				Log.log(Log.ERROR,this,bl);
			}

			EditBus.addToBus(this);
		}

		public String toString()
		{
			if(buffer == null)
				return path + ":" + offset;
			else
				return buffer.getPath() + ":" + pos.getOffset();
		}

		public void dispose()
		{
			EditBus.removeFromBus(this);
		}

		public Buffer getBuffer()
		{
			if(buffer == null)
				return jEdit.openFile(null,null,path,false,false);
			else
				return buffer;
		}

		public int getOffset()
		{
			if(pos == null)
				return offset;
			else
				return pos.getOffset();
		}

		public void handleMessage(EBMessage msg)
		{
			if(msg instanceof BufferUpdate)
				handleBufferUpdate((BufferUpdate)msg);
		}

		private void handleBufferUpdate(BufferUpdate msg)
		{
			Buffer _buffer = msg.getBuffer();
			if(msg.getWhat() == BufferUpdate.CREATED)
			{
				if(buffer == null && _buffer.getPath().equals(path))
				{
					buffer = _buffer;
					try
					{
						pos = buffer.createPosition(offset);
					}
					catch(BadLocationException bl)
					{
						Log.log(Log.ERROR,this,bl);
					}
				}
			}
			else if(msg.getWhat() == BufferUpdate.CLOSED)
			{
				if(_buffer == buffer)
				{
					buffer = null;
					offset = pos.getOffset();
					pos = null;
				}
			}
		}
	}

	public static class ClipboardRegister implements Register
	{
		public void setValue(String value)
		{
			Clipboard clipboard = Toolkit.getDefaultToolkit()
				.getSystemClipboard();
			StringSelection selection = new StringSelection(value);
			clipboard.setContents(selection,null);
		}

		public String toString()
		{
			Clipboard clipboard = Toolkit.getDefaultToolkit()
				.getSystemClipboard();
			try
			{
				String selection = (String)(clipboard
					.getContents(this).getTransferData(
					DataFlavor.stringFlavor));

				// The MacOS MRJ doesn't convert \r to \n,
				// so do it here
				return selection.replace('\r','\n');
			}
			catch(Exception e)
			{
				Log.log(Log.NOTICE,this,e);
				return null;
			}
		}

		public void dispose() {}
	}

	public static class StringRegister implements Register
	{
		private String value;

		public StringRegister(String value)
		{
			this.value = value;
		}

		public String toString()
		{
			return value;
		}

		public void dispose() {}
	}

	// private members
	private static Register[] registers;

	private Registers() {}

	static
	{
		setRegister('$',new ClipboardRegister());
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.5  1999/11/19 08:54:51  sp
 * EditBus integrated into the core, event system gone, bug fixes
 *
 * Revision 1.4  1999/11/07 06:51:43  sp
 * Check box menu items supported
 *
 * Revision 1.3  1999/10/31 07:15:34  sp
 * New logging API, splash screen updates, bug fixes
 *
 * Revision 1.2  1999/10/23 03:48:22  sp
 * Mode system overhaul, close all dialog box, misc other stuff
 *
 * Revision 1.1  1999/10/03 04:13:26  sp
 * Forgot to add some files
 *
 */
