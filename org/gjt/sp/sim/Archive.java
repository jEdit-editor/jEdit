/*
 * Archive.java - SIM archive
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

package org.gjt.sp.sim;

import java.io.*;

public class Archive
{
	public static final int BUFSIZ = 32 * 1024;

	public static void main(String[] args)
	{
		if(args.length < 2)
		{
			usage();
			return;
		}

		String mode = args[0];
		if(mode.equals("c"))
			createArchive(args);
		else if(mode.equals("x"))
			extractArchive(args);
		else if(mode.equals("t"))
			listArchive(args);
		else
			usage();
	}

	public Archive(String filename) throws IOException
	{
		this(new FileInputStream(filename));
	}

	public Archive(InputStream in) throws IOException
	{
		this.in = new DataInputStream(in);

		String name = nextEntry();
		if(!name.equals("SIM_BEGIN"))
			throw new IOException("First entry is not SIM_BEGIN");
	}

	public String nextEntry() throws IOException
	{
		int namelen = in.readInt();
		byte[] namebuf = new byte[namelen];
		in.readFully(namebuf,0,namelen);
		length = in.readLong();
		String name = new String(namebuf);
		if(name.equals("SIM_END"))
		{
			in.close();
			return null;
		}
		else
			return name;
	}

	public void skipEntry() throws IOException
	{
		in.skip(length);
	}

	public InputStream readEntry() throws IOException 
	{
		return new EntryInputStream();
	}

	// private members
	private DataInputStream in;
	private long length;

	private static void usage()
	{
		System.err.println("Usage: java org.gjt.sp.sim.Archive c <archive name> <files>");
		System.err.println("Usage: java org.gjt.sp.sim.Archive x <archive name>");
		System.err.println("Usage: java org.gjt.sp.sim.Archive t <archive name>");
	}

	private static void createArchive(String[] args)
	{
		if(args.length < 3)
		{
			usage();
			return;
		}

		String filename = args[1];
		try
		{
			DataOutputStream out = new DataOutputStream(
				new FileOutputStream(filename));
			writeEntry(out,"SIM_BEGIN",0L,null);
			for(int i = 2; i < args.length; i++)
			{
				File file = new File(args[i]);
				long length = file.length();
				InputStream in;
				if(file.exists())
					in = new FileInputStream(file);
				else
					in = null;
				writeEntry(out,args[i],length,in);
			}
			writeEntry(out,"SIM_END",0L,null);
			out.close();
		}
		catch(IOException io)
		{
			io.printStackTrace();
		}
	}

	private static void writeEntry(DataOutputStream out, String name,
		long length, InputStream in) throws IOException
	{
		System.out.println(name);

		out.writeInt(name.length());
		out.writeBytes(name); // XXX: deprecated
		out.writeLong(length);

		if(in == null)
			return;

		byte[] buf = new byte[BUFSIZ];
		int count;
		while((count = in.read(buf,0,BUFSIZ)) != -1)
		{
			out.write(buf,0,count);
		}
		in.close();
	}

	private static void extractArchive(String[] args)
	{
		if(args.length != 2)
		{
			usage();
			return;
		}

		try
		{
			Archive archive = new Archive(args[1]);
			String name = null;
			while((name = archive.nextEntry()) != null)
			{
				System.out.println(name);
				InputStream in = archive.readEntry();
				File file = new File(new File(name).getParent());
				if(!file.exists())
					file.mkdirs();
				FileOutputStream out = new FileOutputStream(name);
				byte[] buf = new byte[BUFSIZ];
				int count;
				while((count = in.read(buf,0,BUFSIZ)) != -1)
				{
					out.write(buf,0,count);
				}
				in.close();
				out.close();
			}
		}
		catch(IOException io)
		{
			io.printStackTrace();
		}
	}

	private static void listArchive(String[] args)
	{
		if(args.length != 2)
		{
			usage();
			return;
		}

		try
		{
			Archive archive = new Archive(args[1]);
			String name = null;
			while((name = archive.nextEntry()) != null)
			{
				System.out.println(name);
				archive.skipEntry();
			}
		}
		catch(IOException io)
		{
			io.printStackTrace();
		}
	}

	class EntryInputStream extends InputStream
	{
		int count;

		public int read() throws IOException
		{
			if(count == length)
				return -1;
			else
			{
				count++;
				return in.read();
			}
		}

		public int read(byte[] buf, int off, int len)
			throws IOException
		{
			if(count == length)
				return -1;
			if(count + len > length)
				len = (int)(length - count);

			count += len;

			in.readFully(buf,off,len);
			return len;
		}

		public long skip(long n) throws IOException
		{
			return in.skip(n);
		}
	}
}
