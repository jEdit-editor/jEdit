/*
 * DocIndeURLConnection.java - jEdit documentation index URL connection
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

package org.gjt.sp.jedit.proto.jeditdocs;

import java.io.*;
import java.net.*;
import java.text.MessageFormat;
import org.gjt.sp.jedit.*;

public class DocIndexURLConnection extends URLConnection
{
	public DocIndexURLConnection(URL url)
	{
		super(url);

		StringBuffer buf = new StringBuffer();
		buf.append(jEdit.getProperty("docindex.header"));

		File docDir = new File(MiscUtilities.constructPath(
			jEdit.getJEditHome(),"doc"));

		// Text files
		buf.append(jEdit.getProperty("docindex.general"));

		String textFileEntry = jEdit.getProperty("docindex.general.entry");
		String[] textFiles = docDir.list();
		String[] args = new String[2];

		if(textFiles != null)
		{
			MiscUtilities.quicksort(textFiles,new MiscUtilities
				.StringCompare());

			for(int i = 0; i < textFiles.length; i++)
			{
				if(textFiles[i].toUpperCase().endsWith(".TXT"))
				{
					args[0] = "\"file:" + docDir.getPath()
						+ File.separator
						+ textFiles[i] + "\"";
					args[1] = textFiles[i];
					buf.append(MessageFormat.format(
						textFileEntry,args));
				}
			}
		}

		// Books
		args = new String[1];

		buf.append(jEdit.getProperty("docindex.books"));

		String path = docDir.getPath() + File.separator
			+ "jeditdocs" + File.separator + "index.html";
		if(new File(path).exists())
		{
			args[0] = "\"file:" + path + "\"";
			buf.append(jEdit.getProperty("docindex.books.jeditdocs",args));
		}

		path = docDir.getPath() + File.separator
			+ "api" + File.separator + "packages.html";
		if(new File(path).exists())
		{
			args[0] = "\"file:" + path + "\"";
			buf.append(jEdit.getProperty("docindex.books.api",args));
		}

		// Plugins
		buf.append(jEdit.getProperty("docindex.plugins"));

		String pluginEntry = jEdit.getProperty("docindex.plugins.entry");

		EditPlugin[] plugins = jEdit.getPlugins();
		args = new String[4];
		for(int i = 0; i < plugins.length; i++)
		{
			EditPlugin plugin = plugins[i];
			String clazz = plugin.getClass().getName();

			args[0] = jEdit.getProperty("plugin." + clazz
				+ ".name");
			if(args[0] == null)
				continue;

			args[1] = jEdit.getProperty("plugin." + clazz
				+ ".author");
			if(args[1] == null)
				continue;

			args[2] = jEdit.getProperty("plugin." + clazz
				+ ".version");
			if(args[2] == null)
				continue;

			String docs = jEdit.getProperty("plugin."
				+ clazz + ".docs");
			if(docs == null)
				continue;
			JARClassLoader classLoader = (JARClassLoader)
				plugin.getClass().getClassLoader();
			args[3] = classLoader.getResourceAsPath(docs);
			buf.append(MessageFormat.format(
				pluginEntry,args));
		}

		buf.append(jEdit.getProperty("docindex.footer"));

		in = new StringBufferInputStream(buf.toString());
	}

	public void connect() {}

	public InputStream getInputStream()
	{
		return in;
	}

	public String getHeaderField(String name)
	{
		if(name.equals("content-type"))
			return "text/html";
		else
			return null;
	}
	
	// private members
	private StringBufferInputStream in;
}
