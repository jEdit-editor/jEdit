/*
 * ModeCatalogHandler.java - XML handler for mode catalog files
 * Copyright (C) 2000, 2001 Slava Pestov
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

class ModeCatalogHandler extends HandlerBase
{
	ModeCatalogHandler(String directory)
	{
		this.directory = directory;
	}

	public Object resolveEntity(String publicId, String systemId)
	{
		if("catalog.dtd".equals(systemId))
		{
			try
			{
				return new BufferedReader(new InputStreamReader(
					getClass().getResourceAsStream("catalog.dtd")));
			}
			catch(Exception e)
			{
				Log.log(Log.ERROR,this,"Error while opening"
					+ " catalog.dtd:");
				Log.log(Log.ERROR,this,e);
			}
		}

		return null;
	}

	public void attribute(String aname, String value, boolean isSpecified)
	{
		aname = (aname == null) ? null : aname.intern();

		if(aname == "NAME")
			modeName = value;
		else if(aname == "FILE")
		{
			if(value == null)
			{
				Log.log(Log.ERROR,this,directory + "catalog:"
					+ " mode " + modeName + " doesn't have"
					+ " a FILE attribute");
			}
			else
				file = value;
		}
		else if(aname == "FILE_NAME_GLOB")
			filenameGlob = value;
		else if(aname == "FIRST_LINE_GLOB")
			firstlineGlob = value;
	}

	public void doctypeDecl(String name, String publicId,
		String systemId) throws Exception
	{
		if("CATALOG".equals(name))
			return;

		Log.log(Log.ERROR,this,directory + "catalog: DOCTYPE must be CATALOG");
	}

	public void endElement(String name)
	{
		if(name.equals("MODE"))
		{
			Mode mode = new Mode(modeName);
			mode.setProperty("file",MiscUtilities.constructPath(
				directory,file));

			if(filenameGlob != null)
				mode.setProperty("filenameGlob",filenameGlob);

			if(firstlineGlob != null)
				mode.setProperty("firstlineGlob",firstlineGlob);

			jEdit.addMode(mode);

			modeName = file = filenameGlob = firstlineGlob = null;
		}
	}

	// end HandlerBase implementation

	// private members
	private String directory;

	private String modeName;
	private String file;
	private String filenameGlob;
	private String firstlineGlob;
}
