/*
 * MultiFileSearchDialog.java - Multifile search and replace dialog
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

package org.gjt.sp.jedit.gui;

import javax.swing.border.EmptyBorder;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.File;
import java.util.Vector;
import org.gjt.sp.jedit.io.FileVFS;
import org.gjt.sp.jedit.search.*;
import org.gjt.sp.jedit.*;

/**
 * The dialog for selecting the file set to search in.
 * @author Slava Pestov
 * @version $Id$
 */
public class MultiFileSearchDialog extends EnhancedDialog
{
	public MultiFileSearchDialog(View view, SearchFileSet fileset)
	{
		super(view,jEdit.getProperty("multifile.title"),true);

		this.view = view;
		this.fileset = fileset;

		GridBagLayout layout = new GridBagLayout();
		JPanel content = new JPanel(layout);
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		GridBagConstraints cons = new GridBagConstraints();
		cons.anchor = GridBagConstraints.WEST;
		cons.fill = GridBagConstraints.BOTH;
		cons.weightx = 1.0f;
		cons.insets = new Insets(0,0,3,0);

		JLabel caption = new JLabel(jEdit.getProperty("multifile.caption"));
		cons.gridy++;
		layout.setConstraints(caption,cons);
		content.add(caption);

		ActionHandler actionHandler = new ActionHandler();
		ButtonGroup grp = new ButtonGroup();

		current = new JRadioButton(jEdit.getProperty("multifile.current"));
		if(fileset instanceof CurrentBufferSet)
			current.setSelected(true);
		current.addActionListener(actionHandler);
		grp.add(current);
		cons.gridy++;
		layout.setConstraints(current,cons);
		content.add(current);

		all = new JRadioButton(jEdit.getProperty("multifile.all"));
		if(fileset instanceof AllBufferSet)
			all.setSelected(true);
		all.addActionListener(actionHandler);
		grp.add(all);
		cons.gridy++;
		layout.setConstraints(all,cons);
		content.add(all);

		selected = new JRadioButton(jEdit.getProperty("multifile.selected"));
		if(fileset instanceof BufferListSet
			&& !(fileset instanceof DirectoryListSet))
			selected.setSelected(true);
		selected.addActionListener(actionHandler);
		grp.add(selected);
		cons.gridy++;
		layout.setConstraints(selected,cons);
		content.add(selected);

		JScrollPane list = createBufferList();
		cons.gridy++;
		cons.weighty = 1.0f;
		layout.setConstraints(list,cons);
		content.add(list);
		cons.weighty = 0.0f;

		directory = new JRadioButton(jEdit.getProperty("multifile.directory"));
		if(fileset instanceof DirectoryListSet)
			directory.setSelected(true);
		directory.addActionListener(actionHandler);
		grp.add(directory);
		cons.gridy++;
		layout.setConstraints(directory,cons);
		content.add(directory);

		JPanel options = createDirectoryOptions();
		cons.gridy++;
		layout.setConstraints(options,cons);
		content.add(options);

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
		panel.setBorder(new EmptyBorder(9,0,0,0));

		panel.add(Box.createGlue());
		ok = new JButton(jEdit.getProperty("common.ok"));
		ok.addActionListener(actionHandler);
		getRootPane().setDefaultButton(ok);
		panel.add(ok);
		panel.add(Box.createHorizontalStrut(6));

		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(actionHandler);
		panel.add(cancel);
		panel.add(Box.createGlue());

		cons.gridy++;
		layout.setConstraints(panel,cons);
		content.add(panel);

		updateEnabled();

		pack();
		setLocationRelativeTo(view);
		show();
	}

	public SearchFileSet getSearchFileSet()
	{
		if(isOK)
			return fileset;
		else
			return null;
	}

	// EnhancedDialog implementation
	public void ok()
	{
		if(current.isSelected())
			fileset = new CurrentBufferSet();
		else if(all.isSelected())
			fileset = new AllBufferSet();
		else if(selected.isSelected())
		{
			Object[] values = bufferList.getSelectedValues();
			if(values == null || values.length == 0)
			{
				GUIUtilities.error(view,"invalid-fileset",null);
				return;
			}
			else
				fileset = new BufferListSet(values);
		}
		else if(directory.isSelected())
		{
			String _directory = directoryPath.getText();
			String _glob = (String)directoryGlob.getSelectedItem();
			boolean _recurse = directoryRecurse.isSelected();

			jEdit.setBooleanProperty("multifile.directory.recurse.value",_recurse);

			fileset = new DirectoryListSet(_directory,
				_glob,_recurse);
			if(!((BufferListSet)fileset).isValid())
			{
				GUIUtilities.error(view,"invalid-fileset",null);
				return;
			}
		}
		else
			return;

		isOK = true;
		dispose();
	}

	public void cancel()
	{
		dispose();
	}
	// end EnhancedDialog implementation

	// private members
	private View view;
	private SearchFileSet fileset;

	private JRadioButton current;
	private JRadioButton all;
	private JRadioButton selected;
	private JRadioButton directory;

	private JList bufferList;

	private JTextField directoryPath;
	private JButton directoryChoose;
	private JComboBox directoryGlob;
	private JCheckBox directoryRecurse;

	private JButton ok;
	private JButton cancel;

	private boolean isOK;

	private void updateEnabled()
	{
		bufferList.setEnabled(selected.isSelected());
		if(directory.isSelected())
		{
			directoryPath.setEnabled(true);
			directoryChoose.setEnabled(true);
			directoryGlob.setEnabled(true);
			directoryRecurse.setEnabled(true);
		}
		else
		{
			directoryPath.setEnabled(false);
			directoryChoose.setEnabled(false);
			directoryGlob.setEnabled(false);
			directoryRecurse.setEnabled(false);
		}
	}

	private JScrollPane createBufferList()
	{
		Buffer[] buffers = jEdit.getBuffers();
		bufferList = new JList(buffers);

		if(fileset instanceof BufferListSet &&
			!(fileset instanceof DirectoryListSet))
		{
			Buffer buffer = fileset.getFirstBuffer(view);
			do
			{
				for(int i = 0; i < buffers.length; i++)
				{
					if(buffers[i] == buffer)
						bufferList.addSelectionInterval(i,i);
				}
				buffer = fileset.getNextBuffer(view,buffer);
			}
			while(buffer != null);
		}

		return new JScrollPane(bufferList);
	}

	private JPanel createDirectoryOptions()
	{
		JPanel box = new JPanel(new BorderLayout());

		// First component houses the labels.
		JPanel labels = new JPanel(new GridLayout(2,1,0,3));
		JLabel label = new JLabel(jEdit.getProperty("multifile.directory.path"),
			SwingConstants.RIGHT);
		label.setBorder(new EmptyBorder(0,0,0,12));
		labels.add(label);
		label = new JLabel(jEdit.getProperty("multifile.directory.glob"),
			SwingConstants.RIGHT);
		label.setBorder(new EmptyBorder(0,0,0,12));
		labels.add(label);

		box.add(BorderLayout.WEST,labels);

		// Second components houses the text fields and buttons.
		JPanel fields = new JPanel(new GridLayout(2,1,0,3));

		// Box holding path field and choose button
		Box box1 = new Box(BoxLayout.X_AXIS);

		Box box2 = new Box(BoxLayout.Y_AXIS);
		box2.add(Box.createGlue());

		String path;
		if(view.getBuffer().getVFS() instanceof FileVFS)
		{
			path = MiscUtilities.getFileParent(
				view.getBuffer().getPath());
		}
		else
			path = System.getProperty("user.dir");

		directoryPath = new JTextField(path);
		Dimension dim = directoryPath.getPreferredSize();
		dim.width = Integer.MAX_VALUE;
		directoryPath.setMaximumSize(dim);
		box2.add(directoryPath);
		box2.add(Box.createGlue());
		box1.add(box2);
		box1.add(Box.createHorizontalStrut(6));

		directoryChoose = new JButton(jEdit.getProperty("multifile"
			+ ".directory.choose"));
		directoryChoose.addActionListener(new ActionHandler());
		box1.add(directoryChoose);

		fields.add(box1);

		// Box holding glob field and recurse check box
		Box box3 = new Box(BoxLayout.X_AXIS);

		Box box4 = new Box(BoxLayout.Y_AXIS);
		box4.add(Box.createGlue());

		Mode[] modes = jEdit.getModes();
		Vector globs = new Vector(modes.length);
		globs.addElement("*");
		for(int i = 0; i < modes.length; i++)
		{
			String glob = (String)modes[i].getProperty("filenameGlob");
			if(glob != null)
				globs.addElement(glob);
		}

		int i = 0;
		String glob;
		while((glob = jEdit.getProperty("filefilter." + i)) != null)
		{
			globs.addElement(glob);
			i++;
		}

		directoryGlob = new JComboBox(globs);
		directoryGlob.setEditable(true);
		directoryGlob.setSelectedItem("*" + MiscUtilities.getFileExtension(
			view.getBuffer().getName()));
		dim = directoryGlob.getPreferredSize();
		dim.width = Integer.MAX_VALUE;
		directoryGlob.setMaximumSize(dim);
		box4.add(directoryGlob);
		box4.add(Box.createGlue());
		box3.add(box4);
		box3.add(Box.createHorizontalStrut(6));

		directoryRecurse = new JCheckBox(jEdit.getProperty("multifile"
			+ ".directory.recurse"));
		directoryRecurse.setSelected(jEdit.getBooleanProperty(
			"multifile.directory.recurse.value"));
		directoryRecurse.addActionListener(new ActionHandler());
		directoryRecurse.setMargin(new Insets(0,0,0,0));
		box3.add(directoryRecurse);

		fields.add(box3);

		box.add(BorderLayout.CENTER,fields);

		return box;
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();

			if(source instanceof JRadioButton)
				updateEnabled();
			if(source == directoryChoose)
			{
				File directory = new File(directoryPath.getText());
				JFileChooser chooser = new JFileChooser(directory.getParent());
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				chooser.setSelectedFile(directory);

				if(chooser.showOpenDialog(MultiFileSearchDialog.this)
					== JFileChooser.APPROVE_OPTION)
					directoryPath.setText(chooser.getSelectedFile().getPath());
			}
			else if(source == ok)
				ok();
			else if(source == cancel)
				cancel();
		}
	}
}

/*
 * ChangeLog:
 * $Log$
 * Revision 1.15  2000/06/04 08:57:35  sp
 * GUI updates, bug fixes
 *
 * Revision 1.14  2000/05/21 03:00:51  sp
 * Code cleanups and bug fixes
 *
 * Revision 1.13  2000/04/27 08:32:57  sp
 * VFS fixes, read only fixes, macros can prompt user for input, improved
 * backup directory feature
 *
 * Revision 1.12  2000/04/25 11:00:20  sp
 * FTP VFS hacking, some other stuff
 *
 * Revision 1.11  2000/04/15 04:14:47  sp
 * XML files updated, jEdit.get/setBooleanProperty() method added
 *
 * Revision 1.10  2000/03/14 06:22:25  sp
 * Lots of new stuff
 *
 * Revision 1.9  2000/02/08 10:04:05  sp
 * Bug fixes, documentation updates
 *
 * Revision 1.8  1999/11/26 07:37:11  sp
 * Escape/enter handling code moved to common superclass, bug fixes
 *
 * Revision 1.7  1999/10/28 09:07:21  sp
 * Directory list search
 *
 */
