/*
 * Autosave.java - Autosave thread
 * Copyright (C) 1998, 1999 Slava Pestov
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

/**
 * @author Slava Pestov
 * @version $Id$
 */
class Autosave extends Thread
{
	Autosave()
	{
		super("jEdit autosave daemon");
		setDaemon(true);
		start();
	}

	public void run()
	{
		int interval;
		try
		{
			interval = Integer.parseInt(jEdit.getProperty(
				"autosave"));
		}
		catch(NumberFormatException nf)
		{
			interval = 15;
		}
		if(interval == 0)
			return;
		interval *= 1000;
		for(;;)
		{
			try
			{
				Thread.sleep(interval);
			}
			catch(InterruptedException i)
			{
				return;
			}
			if(interrupted())
				return;
			Buffer[] bufferArray = jEdit.getBuffers();
			for(int i = 0; i < bufferArray.length; i++)
				bufferArray[i].autosave();
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.6  2000/06/12 02:43:29  sp
 * pre6 almost ready
 *
 * Revision 1.5  1999/10/01 07:31:39  sp
 * RMI server replaced with socket-based server, minor changes
 *
 */
