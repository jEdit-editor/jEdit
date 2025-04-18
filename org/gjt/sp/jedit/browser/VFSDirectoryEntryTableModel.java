/*
 * VFSDirectoryEntryTableModel.java - VFS directory entry table model
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2003, 2005 Slava Pestov
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

package org.gjt.sp.jedit.browser;

//{{{ Imports
import javax.swing.table.*;
import java.util.*;

import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.io.VFSFile;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.StandardUtilities;

import static org.gjt.sp.jedit.MiscUtilities.isUncPath;
//}}}

/**
 * @author Slava Pestov
 * @version $Id$
 * @since jEdit 4.2pre1
 */
public class VFSDirectoryEntryTableModel extends AbstractTableModel
{
	//{{{ VFSDirectoryEntryTableModel constructor
	public VFSDirectoryEntryTableModel()
	{
		extAttrs = new ArrayList<>();
		sortColumnIndex = 0;
		ascending = true;
	} //}}}

	//{{{ setRoot() method
	public void setRoot(VFS vfs, List<VFSFile> list)
	{
		extAttrs.clear();
		addExtendedAttributes(vfs);

		/* if(files != null && files.length != 0)
			fireTableRowsDeleted(0,files.length - 1); */

		files = new Entry[list.size()];
		for(int i = 0; i < files.length; i++)
		{
			files[i] = new Entry(list.get(i),0);
		}

		/* if(files.length != 0)
			fireTableRowsInserted(0,files.length - 1); */

		Arrays.sort(files, new EntryCompare(getSortAttribute(sortColumnIndex), ascending));
		fireTableStructureChanged();
	} //}}}

	//{{{ expand() method
	public int expand(VFS vfs, Entry entry, List<VFSFile> list)
	{
		int startIndex = -1;
		for(int i = 0; i < files.length; i++)
		{
			if(files[i] == entry)
				startIndex = i;
		}
		if (startIndex != -1)
			collapse(vfs,startIndex);

		addExtendedAttributes(vfs);
		entry.expanded = true;

		if(list != null)
		{
			// make a large enough destination array
			Entry[] newFiles = new Entry[files.length + list.size()];
			Entry[] subdirFiles = new Entry[list.size()];

			for(int i = 0; i < list.size(); i++)
			{
				subdirFiles[i] = new Entry(
					list.get(i),entry.level + 1,entry);
			}

			// sort expanded entries according to current sort params
			Arrays.sort(subdirFiles, new EntryCompare(
				getSortAttribute(sortColumnIndex), ascending));
			
			// make room after expanded entry for subdir files
			int nextIndex = startIndex + 1;
			System.arraycopy(files,0,newFiles,0,nextIndex);
			System.arraycopy(subdirFiles,0,newFiles,nextIndex,list.size());
			System.arraycopy(files,nextIndex,newFiles,nextIndex + list.size(),
				files.length - nextIndex);

			this.files = newFiles;

			/* fireTableRowsInserted(startIndex + 1,
				startIndex + list.size() + 1); */
		}

		/* fireTableRowsUpdated(startIndex,startIndex); */

		fireTableStructureChanged();

		return startIndex;
	} //}}}

	//{{{ collapse() method
	public void collapse(VFS vfs, int index)
	{
		Entry entry = files[index];
		if(!entry.expanded)
			return;

		entry.expanded = false;

		int lastIndex = index + 1;
		while(lastIndex < files.length)
		{
			Entry e = files[lastIndex];

			if(e.level <= entry.level)
				break;

			lastIndex++;

			if(e.expanded)
			{
				removeExtendedAttributes(VFSManager.getVFSForPath(
					e.dirEntry.getPath()));
			}
		}

		removeExtendedAttributes(vfs);

		Entry[] newFiles = new Entry[files.length - lastIndex + index + 1];
		System.arraycopy(files,0,newFiles,0,index + 1);
		System.arraycopy(files,lastIndex,newFiles,index + 1,
			files.length - lastIndex);

		files = newFiles;

		/* fireTableRowsUpdated(index,index);
		fireTableRowsDeleted(index + 1,lastIndex); */

		fireTableStructureChanged();
	} //}}}

	//{{{ getColumnCount() method
	@Override
	public int getColumnCount()
	{
		return 1 + extAttrs.size();
	} //}}}

	//{{{ getRowCount() method
	@Override
	public int getRowCount()
	{
		if(files == null)
			return 0;
		else
			return files.length;
	} //}}}

	//{{{ getColumnName() method
	@Override
	public String getColumnName(int col)
	{
		if(col == 0)
			return jEdit.getProperty("vfs.browser.name");
		else
			return jEdit.getProperty("vfs.browser." + getExtendedAttribute(col));
	} //}}}

	//{{{ getColumnClass() method
	@Override
	public Class<Entry> getColumnClass(int col)
	{
		return Entry.class;
	} //}}}

	//{{{ getValueAt() method
	@Override
	public Object getValueAt(int row, int col)
	{
		if(files == null)
			return null;
		else
			return files[row];
	} //}}}

	//{{{ getAscending() method
	public boolean getAscending()
	{
		return ascending;
	} //}}}

	//{{{ getSortColumnIndex() method
	public int getSortColumnIndex()
	{
		return sortColumnIndex;
	} //}}}

	//{{{ getSortAttribute() method
	public String getSortAttribute(int column)
	{
		return column == 0 ? "name" : getExtendedAttribute(column);
	} //}}}

	//{{{ sortByColumn() method
	public boolean sortByColumn(int columnIndex)
	{
		// toggle ascending/descending if column was clicked again
		ascending = sortColumnIndex != columnIndex || !ascending;

		// we don't sort by some attributes
		String sortBy = getSortAttribute(columnIndex);
		if(sortBy == VFS.EA_STATUS)
			return false;

		Arrays.sort(files, new EntryCompare(sortBy, ascending));

		// remember column
		sortColumnIndex = columnIndex;
		fireTableStructureChanged();

		return true;
	} //}}}

	//{{{ getExtendedAttribute() method
	public String getExtendedAttribute(int index)
	{
		return extAttrs.get(index - 1).name;
	} //}}}

	//{{{ getColumnWidth() method
	/**
	 * @param index The column index
	 * @return A saved column width
	 * @since jEdit 4.3pre2
	 */
	public int getColumnWidth(int index)
	{
		String extAttr = getExtendedAttribute(index);
		return jEdit.getIntegerProperty("vfs.browser."
			+ extAttr + ".width",100);
	} //}}}
	
	//{{{ saveColumnWidth() method
	/**
	 * @param index The column index
	 * @param w The column width
	 * @since jEdit 4.3pre2
	 */
	public void saveColumnWidth(int index, int w)
	{
		String extAttr = getExtendedAttribute(index);
		jEdit.setIntegerProperty("vfs.browser."
			+ extAttr + ".width",w);
	} //}}}
	
	//{{{ columnMoved() method
	protected void columnMoved(int fromIndex, int toIndex) {
		if (fromIndex == toIndex)
			return;
		if (fromIndex < 1 || fromIndex > getColumnCount())
			return;
		if (toIndex < 1 || toIndex > getColumnCount())
			return;
		ExtendedAttribute ea = extAttrs.remove(fromIndex - 1);
		extAttrs.add(toIndex - 1, ea);


		// Also adjust sort column index if touched

		if (sortColumnIndex == fromIndex) sortColumnIndex = toIndex;
		else if (sortColumnIndex == toIndex) sortColumnIndex = fromIndex;
	} //}}}
	
	//{{{ getFiles() method
	public VFSFile[] getFiles()
	{
		VFSFile[] f = new VFSFile[files.length];
		for(int i = 0; i < f.length; i++)
			f[i] = files[i].dirEntry;
		return f;
	} //}}}
	
	//{{{ Package-private members
	Entry[] files;
	//}}}

	//{{{ Private members
	private final List<ExtendedAttribute> extAttrs;
	private int sortColumnIndex;
	private boolean ascending;

	//{{{ addExtendedAttributes() method
	private void addExtendedAttributes(VFS vfs)
	{
		String[] attrs = vfs.getExtendedAttributes();
vfs_attr_loop:	for(int i = 0; i < attrs.length; i++)
		{
			for (ExtendedAttribute attr : extAttrs)
			{
				if (attrs[i].equals(attr.name))
				{
					attr.ref++;
					continue vfs_attr_loop;
				}
			}

			// this vfs has an extended attribute which is not
			// in the list. add it to the end with a ref count
			// of 1
			extAttrs.add(new ExtendedAttribute(attrs[i]));
		}
	} //}}}

	//{{{ removeExtendedAttributes() method
	private void removeExtendedAttributes(VFS vfs)
	{
		String[] attrs = vfs.getExtendedAttributes();
vfs_attr_loop:	for(int i = 0; i < attrs.length; i++)
		{
			Iterator<ExtendedAttribute> iter = extAttrs.iterator();
			while(iter.hasNext())
			{
				ExtendedAttribute attr = iter.next();
				if(attrs[i].equals(attr.name))
				{
					if(--attr.ref == 0)
					{
						// we no longer have any
						// dirs using this extended
						// attribute
						iter.remove();
					}

					continue vfs_attr_loop;
				}
			}

			// this vfs has an extended attribute which is not
			// in the list ???
			Log.log(Log.WARNING,this,"We forgot about " + attrs[i]);
		}
	} //}}}

	//}}}

	//{{{ Entry class
	static class Entry
	{
		VFSFile dirEntry;
		// is this branch an expanded dir?
		boolean expanded;
		// how deeply we are nested
		int level;
		// parent entry
		Entry parent;
		// file extension
		String extension;

		Entry(VFSFile dirEntry, int level, Entry parent)
		{
			this(dirEntry,level);
			this.parent = parent;
		}
		
		Entry(VFSFile dirEntry, int level)
		{
			this.dirEntry = dirEntry;
			this.level = level;
			if (dirEntry.getType() == VFSFile.FILE)
				this.extension = MiscUtilities.getFileExtension(dirEntry.getName());
		}
		
		
	} //}}}

	//{{{ ExtendedAttribute class
	static class ExtendedAttribute
	{
		/* reference counter allows us to remove a column from
		 * the table when no directory using this column is
		 * visible */
		int ref;

		String name;

		ExtendedAttribute(String name)
		{
			this.name = name;
			ref = 1;
		}
	} //}}}

	//{{{ EntryCompare class
	/**
	 * Implementation of {@link Comparator}
	 * interface that compares {@link VFSDirectoryEntryTableModel.Entry} instances.
	 * For sorting columns in the VFS Browser.
	 * @since jEdit 4.3pre7
	 */
	static class EntryCompare implements Comparator<Entry>
	{
		private final boolean sortIgnoreCase;
		private final boolean sortMixFilesAndDirs;
		private final boolean sortAscending;
		private final String sortAttribute;
		/**
		 * Creates a new <code>EntryCompare</code>
		 * Expanded branches are sorted, too, but keep with their parent entries
		 * @param sortBy The extended attribute by which to sort the entries.
		 * @param ascending If false, sort order is reversed.
		 */
		EntryCompare(String sortBy, boolean ascending)
		{
			this.sortMixFilesAndDirs = jEdit.getBooleanProperty(
				"vfs.browser.sortMixFilesAndDirs");
			this.sortIgnoreCase = jEdit.getBooleanProperty(
				"vfs.browser.sortIgnoreCase");
			this.sortAscending = ascending;
			this.sortAttribute = sortBy;
		}

		@Override
		public int compare(Entry entry1, Entry entry2)
		{
			// we want to compare sibling ancestors of the entries
			if(entry1.level < entry2.level) 
				return compare(entry1, entry2.parent);
			if(entry1.level > entry2.level)
				return compare(entry1.parent, entry2);

			// here we have entries of the same level
			if(entry1.parent != entry2.parent)
				return compare(entry1.parent, entry2.parent);

			// here we have siblings with the same parents
			// let's do the real comparison

			VFSFile file1 = entry1.dirEntry;
			VFSFile file2 = entry2.dirEntry;

			if(!sortMixFilesAndDirs && (sortAttribute != VFS.EA_TYPE))
			{
				if(((file1.getType() == VFSFile.FILE) || (file2.getType() == VFSFile.FILE))
						&& (file1.getType() != file2.getType()))
					return file2.getType() - file1.getType();
			}

			int result;

			// if the modified attribute is present, then we have a LocalFile
			if(sortAttribute == VFS.EA_MODIFIED)
				result = Long.compare(file1.getModified(), file2.getModified());
			// sort by size
			else if(sortAttribute == VFS.EA_SIZE)
				result = Long.compare(file1.getLength(), file2.getLength());
			// sort by type and extension
			else if(sortAttribute == VFS.EA_TYPE)
				if((file1.getType() != file2.getType()))
					result = file2.getType() - file1.getType();
				else
					result = StandardUtilities.compareStrings(
						entry1.extension,
						entry2.extension,
						sortIgnoreCase);
			// default: sort by name
			else
			{
				if(OperatingSystem.isWindows())
				{
					boolean file1Unc = isUncPath(file1.getPath());
					boolean file2Unc = isUncPath(file2.getPath());
					if(file1Unc && !file2Unc)
						result = 1;
					else if(!file1Unc && file2Unc)
						result = -1;
					else
						result = StandardUtilities.compareStrings(
							file1.getName(),
							file2.getName(),
							sortIgnoreCase);
				}
				else
				{
					result = StandardUtilities.compareStrings(
							file1.getName(),
							file2.getName(),
							sortIgnoreCase);
				}
			}

			return sortAscending ? result : -result;
		}
	} //}}}
}
