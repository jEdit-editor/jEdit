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
 * caret position, or file name. By default, register "$" contains the
 * clipboard, and all other registers are empty. Actions invoked by
 * the user and the methods in this class can change register contents.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class Registers
{
	/**
	 * Returns the specified register.
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
	 * Sets the specified register.
	 * @param name The name
	 * @param newRegister The new value
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
				if(newRegister instanceof StringRegister)
				{
					((ClipboardRegister)register).setValue(
						newRegister.toString());
				}
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
	 * Clears (i.e. it's value to null) the specified register.
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
	 * Returns an array of all available registers. Some of the elements
	 * of this array might be null.
	 */
	public static Register[] getRegisters()
	{
		return registers;
	}

	/**
	 * A register.
	 */
	public interface Register
	{
		/**
		 * Converts to a string.
		 */
		String toString();

		/**
		 * Called when this register is no longer available. This
		 * could remove listeners, free resources, etc.
		 */
		void dispose();
	}

	/**
	 * Register that points to a location in a file.
	 */
	public static class CaretRegister implements Register, EBComponent
	{
		private String path;
		private int offset;
		private Buffer buffer;
		private Position pos;

		/**
		 * Creates a new caret register.
		 * @param buffer The buffer
		 * @param offset The offset
		 */
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

		/**
		 * Converts to a string.
		 */
		public String toString()
		{
			if(buffer == null)
				return path + ":" + offset;
			else
				return buffer.getPath() + ":" + pos.getOffset();
		}

		/**
		 * Called when this register is no longer available. This
		 * implementation removes the register from the EditBus,
		 * so that it will no longer receive buffer notifications.
		 */
		public void dispose()
		{
			EditBus.removeFromBus(this);
		}

		/**
		 * Returns the buffer involved, opening it if necessary.
		 */
		public Buffer getBuffer()
		{
			if(buffer == null)
				return jEdit.openFile(null,null,path,false,false);
			else
				return buffer;
		}

		/**
		 * Returns the offset in the buffer.
		 */
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

	/**
	 * A clipboard register. Register "$" should always be an
	 * instance of this.
	 */
	public static class ClipboardRegister implements Register
	{
		/**
		 * Sets the clipboard contents.
		 */
		public void setValue(String value)
		{
			Clipboard clipboard = Toolkit.getDefaultToolkit()
				.getSystemClipboard();
			StringSelection selection = new StringSelection(value);
			clipboard.setContents(selection,null);
		}

		/**
		 * Returns the clipboard contents.
		 */
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

		/**
		 * Called when this register is no longer available. This
		 * implementation does nothing.
		 */
		public void dispose() {}
	}

	/**
	 * Register that stores a string.
	 */
	public static class StringRegister implements Register
	{
		private String value;

		/**
		 * Creates a new string register.
		 * @param value The contents
		 */
		public StringRegister(String value)
		{
			this.value = value;
		}

		/**
		 * Converts to a string.
		 */
		public String toString()
		{
			return value;
		}

		/**
		 * Called when this register is no longer available. This
		 * implementation does nothing.
		 */
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
 * Revision 1.7  2000/02/15 07:44:30  sp
 * bug fixes, doc updates, etc
 *
 * Revision 1.6  1999/11/21 01:20:30  sp
 * Bug fixes, EditBus updates, fixed some warnings generated by jikes +P
 *
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
