/*
 * Registers.java - Register manager
 * Copyright (C) 1999, 2000 Slava Pestov
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
import java.io.*;
import java.util.Vector;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.jedit.msg.RegistersChanged;
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

		EditBus.send(new RegistersChanged(null));
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

		EditBus.send(new RegistersChanged(null));
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
	 * Returns a vector of caret registers. For internal use only.
	 */
	public static Vector getCaretRegisters()
	{
		return caretRegisters;
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
	public static class CaretRegister implements Register
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

			caretRegisters.addElement(this);
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
			caretRegisters.removeElement(this);
		}

		/**
		 * Returns the buffer involved, or null if it is not open.
		 */
		public Buffer getBuffer()
		{
			return buffer;
		}

		/**
		 * Returns the buffer involved, opening it if necessary.
		 */
		public Buffer openFile()
		{
			if(buffer == null)
				return jEdit.openFile(null,path);
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

		void openNotify(Buffer _buffer)
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

		void closeNotify(Buffer _buffer)
		{
			if(_buffer == buffer)
			{
				buffer = null;
				offset = pos.getOffset();
				pos = null;
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

				// Some Java versions return the clipboard
				// contents using the native line separator,
				// so have to convert it here
				BufferedReader in = new BufferedReader(
					new StringReader(selection));
				StringBuffer buf = new StringBuffer();
				String line;
				while((line = in.readLine()) != null)
				{
					buf.append(line);
					buf.append('\n');
				}
				// remove trailing \n
				buf.setLength(buf.length() - 1);
				return buf.toString();
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
	private static Vector caretRegisters = new Vector();

	private Registers() {}

	static
	{
		EditBus.addToBus(new CaretRegisterHelper());
		setRegister('$',new ClipboardRegister());
	}

	static class CaretRegisterHelper implements EBComponent
	{
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
				for(int i = 0; i < caretRegisters.size(); i++)
				{
					((CaretRegister)caretRegisters.elementAt(i))
						.openNotify(_buffer);
				}
			}
			else if(msg.getWhat() == BufferUpdate.CLOSED)
			{
				for(int i = 0; i < caretRegisters.size(); i++)
				{
					((CaretRegister)caretRegisters.elementAt(i))
						.closeNotify(_buffer);
				}
			}
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.11  2000/11/11 02:59:29  sp
 * FTP support moved out of the core into a plugin
 *
 * Revision 1.10  2000/06/12 02:43:29  sp
 * pre6 almost ready
 *
 * Revision 1.9  2000/05/22 12:05:45  sp
 * Markers are highlighted in the gutter, bug fixes
 *
 * Revision 1.8  2000/03/20 03:42:55  sp
 * Smoother syntax package, opening an already open file will ask if it should be
 * reloaded, maybe some other changes
 *
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
