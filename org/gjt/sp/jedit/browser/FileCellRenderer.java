/*
 * FileCellRenderer.java - renders table cells for the VFS browser
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999 Jason Ginchereau
 * Portions copyright (C) 2001, 2003 Slava Pestov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit.browser;

//{{{ Imports
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.*;
//}}}

public class FileCellRenderer extends DefaultTableCellRenderer
{
	public static Icon fileIcon = GUIUtilities.loadIcon("File.png");
	public static Icon openFileIcon = GUIUtilities.loadIcon("OpenFile.png");
	public static Icon dirIcon = GUIUtilities.loadIcon("Folder.png");
	public static Icon openDirIcon = GUIUtilities.loadIcon("OpenFolder.png");
	public static Icon filesystemIcon = GUIUtilities.loadIcon("DriveSmall.png");
	public static Icon loadingIcon = GUIUtilities.loadIcon("ReloadSmall.png");

	//{{{ FileCellRenderer constructor
	public FileCellRenderer()
	{
		plainFont = UIManager.getFont("Tree.font");
		if(plainFont == null)
			plainFont = jEdit.getFontProperty("metal.secondary.font");
		boldFont = plainFont.deriveFont(Font.BOLD);
	} //}}}

	//{{{ getTableCellRendererComponent() method
	public Component getTableCellRendererComponent(JTable table,
		Object value, boolean isSelected, boolean hasFocus, 
		int row, int column)
	{
		super.getTableCellRendererComponent(table,value,isSelected,
			hasFocus,row,column);

		if(value instanceof VFSDirectoryEntryTableModel.Entry)
		{
			VFSDirectoryEntryTableModel.Entry entry =
				(VFSDirectoryEntryTableModel.Entry)value;
			VFS.DirectoryEntry file = entry.dirEntry;

			setFont(file.type == VFS.DirectoryEntry.FILE
				? plainFont : boldFont);

			colorDetermined = false;
			this.isSelected = isSelected;
			this.file = file;

			if(column == 0)
			{
				underlined = (jEdit.getBuffer(file.path) != null);

				setIcon(showIcons
					? getIconForFile(file,entry.expanded)
					: null);
				setText(file.name);

				int state;
				if(file.type == VFS.DirectoryEntry.FILE)
					state = ExpansionToggleBorder.STATE_NONE;
				else if(entry.expanded)
					state = ExpansionToggleBorder.STATE_EXPANDED;
				else
					state = ExpansionToggleBorder.STATE_COLLAPSED;

				setBorder(new ExpansionToggleBorder(
					state,entry.level));
			}
			else
			{
				VFSDirectoryEntryTableModel model = (VFSDirectoryEntryTableModel)table.getModel();
				String extAttr = model.getExtendedAttribute(column - 1);

				underlined = false;
				setIcon(null);
				setText(file.getExtendedAttribute(extAttr));
				setBorder(new EmptyBorder(1,1,1,1));
			}
		}

		return this;
	} //}}}

	//{{{ paintComponent() method
	public void paintComponent(Graphics g)
	{
		if(!colorDetermined)
		{
			if(!isSelected)
			{
				Color color = file.getColor();

				setForeground(color == null
					? UIManager.getColor("Tree.foreground")
					: color);
			}
		}

		super.paintComponent(g);

		if(underlined)
		{
			Font font = getFont();

			FontMetrics fm = getFontMetrics(font);
			int x, y;
			if(getIcon() == null)
			{
				x = 0;
				y = fm.getAscent() + 2;
			}
			else
			{
				x = getIcon().getIconWidth() + getIconTextGap();
				y = Math.max(fm.getAscent() + 2,16);
			}

			Insets border = getBorder().getBorderInsets(this);
			x += border.left;

			g.setColor(getForeground());
			g.drawLine(x,y,x + fm.stringWidth(getText()),y);
		}
	} //}}}

	//{{{ getIconForFile() method
	public static Icon getIconForFile(VFS.DirectoryEntry file, boolean expanded)
	{
		if(file.type == VFS.DirectoryEntry.DIRECTORY)
			return (expanded ? openDirIcon : dirIcon);
		else if(file.type == VFS.DirectoryEntry.FILESYSTEM)
			return filesystemIcon;
		else if(jEdit.getBuffer(file.path) != null)
			return openFileIcon;
		else
			return fileIcon;
	} //}}}

	//{{{ Package-private members
	Font plainFont;
	Font boldFont;
	boolean showIcons;

	//{{{ propertiesChanged() method
	void propertiesChanged()
	{
		showIcons = jEdit.getBooleanProperty("vfs.browser.showIcons");
	} //}}}

	//{{{ getEntryWidth() method
	int getEntryWidth(VFSDirectoryEntryTableModel.Entry entry,
		FontMetrics fm)
	{
		String name = entry.dirEntry.name;
		int width = fm.stringWidth(name);
		width += ExpansionToggleBorder.ICON_WIDTH
			+ entry.level * ExpansionToggleBorder.LEVEL_WIDTH
			+ 3;
		if(showIcons)
		{
			width += fileIcon.getIconWidth();
			width += getIconTextGap();
		}
		return width;
	} //}}}

	//}}}

	//{{{ Private members
	private boolean underlined;
	private boolean colorDetermined;
	private boolean isSelected;
	private VFS.DirectoryEntry file;
	//}}}

	//{{{ ExpansionToggleBorder class
	static class ExpansionToggleBorder implements Border
	{
		static final Icon COLLAPSED_ICON;
		static final Icon EXPANDED_ICON;
		static final int ICON_WIDTH;

		static final int LEVEL_WIDTH = 15;

		static final int STATE_NONE = 0;
		static final int STATE_COLLAPSED = 1;
		static final int STATE_EXPANDED = 2;

		//{{{ ExpansionToggleBorder constructor
		public ExpansionToggleBorder(int state, int level)
		{
			this.state = state;
			this.level = level;
		} //}}}

		//{{{ paintBorder() method
		public void paintBorder(Component c, Graphics g,
			int x, int y, int width, int height)
		{
			switch(state)
			{
			case STATE_COLLAPSED:
				COLLAPSED_ICON.paintIcon(c,g,
					x + level * LEVEL_WIDTH + 1,
					y + 1);
				break;
			case STATE_EXPANDED:
				EXPANDED_ICON.paintIcon(c,g,
					x + level * LEVEL_WIDTH + 1,
					y + 1);
				break;
			}
		} //}}}

		//{{{ getBorderInsets() method
		public Insets getBorderInsets(Component c)
		{
			return new Insets(1,level * LEVEL_WIDTH
				+ ICON_WIDTH + 2,1,1);
		} //}}}

		//{{{ isBorderOpaque() method
		public boolean isBorderOpaque()
		{
			return false;
		} //}}}

		//{{{ isExpansionToggle() method
		public static boolean isExpansionToggle(int level, int x)
		{
			return (x >= level * LEVEL_WIDTH)
				&& (x <= level * LEVEL_WIDTH + ICON_WIDTH);
		} //}}}

		//{{{ Private members
		private int state;
		private int level;

		static
		{
			COLLAPSED_ICON = UIManager.getIcon("Tree.collapsedIcon");
			EXPANDED_ICON = UIManager.getIcon("Tree.expandedIcon");
			ICON_WIDTH = Math.max(COLLAPSED_ICON.getIconWidth(),
				EXPANDED_ICON.getIconWidth());
		} //}}}
	} //}}}
}
