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

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.File;
import java.util.Vector;
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
		getContentPane().setLayout(layout);
		GridBagConstraints cons = new GridBagConstraints();
		cons.anchor = GridBagConstraints.WEST;
		cons.fill = GridBagConstraints.BOTH;
		cons.weightx = 1.0f;

		JLabel caption = new JLabel(jEdit.getProperty("multifile.caption"));
		cons.gridy++;
		layout.setConstraints(caption,cons);
		getContentPane().add(caption);

		ActionHandler actionHandler = new ActionHandler();
		ButtonGroup grp = new ButtonGroup();

		current = new JRadioButton(jEdit.getProperty("multifile.current"));
		if(fileset instanceof CurrentBufferSet)
			current.getModel().setSelected(true);
		current.addActionListener(actionHandler);
		grp.add(current);
		cons.gridy++;
		layout.setConstraints(current,cons);
		getContentPane().add(current);

		all = new JRadioButton(jEdit.getProperty("multifile.all"));
		if(fileset instanceof AllBufferSet)
			all.getModel().setSelected(true);
		all.addActionListener(actionHandler);
		grp.add(all);
		cons.gridy++;
		layout.setConstraints(all,cons);
		getContentPane().add(all);

		selected = new JRadioButton(jEdit.getProperty("multifile.selected"));
		if(fileset instanceof BufferListSet
			&& !(fileset instanceof DirectoryListSet))
			selected.getModel().setSelected(true);
		selected.addActionListener(actionHandler);
		grp.add(selected);
		cons.gridy++;
		layout.setConstraints(selected,cons);
		getContentPane().add(selected);

		JScrollPane list = createBufferList();
		cons.gridy++;
		cons.weighty = 1.0f;
		layout.setConstraints(list,cons);
		cons.weighty = 0.0f;
		getContentPane().add(list);

		directory = new JRadioButton(jEdit.getProperty("multifile.directory"));
		if(fileset instanceof DirectoryListSet)
			directory.getModel().setSelected(true);
		directory.addActionListener(actionHandler);
		grp.add(directory);
		cons.gridy++;
		layout.setConstraints(directory,cons);
		getContentPane().add(directory);

		JPanel options = createDirectoryOptions();
		cons.gridy++;
		layout.setConstraints(options,cons);
		getContentPane().add(options);

		JPanel panel = new JPanel();

		ok = new JButton(jEdit.getProperty("common.ok"));
		ok.addActionListener(actionHandler);
		getRootPane().setDefaultButton(ok);
		panel.add(ok);

		cancel = new JButton(jEdit.getProperty("common.cancel"));
		cancel.addActionListener(actionHandler);
		panel.add(cancel);

		cons.gridy++;
		layout.setConstraints(panel,cons);
		getContentPane().add(panel);

		updateEnabled();

		pack();
		setLocationRelativeTo(view);
		show();
	}

	public SearchFileSet getSearchFileSet()
	{
		if(!isOK)
			return null;

		if(current.getModel().isSelected())
			return new CurrentBufferSet();
		else if(all.getModel().isSelected())
			return new AllBufferSet();
		else if(selected.getModel().isSelected())
		{
			Object[] values = bufferList.getSelectedValues();
			if(values == null || values.length == 0)
				return new CurrentBufferSet();
			else
				return new BufferListSet(values);
		}
		else if(directory.getModel().isSelected())
		{
			String _directory = directoryPath.getText();
			String _glob = (String)directoryGlob.getSelectedItem();
			boolean _recurse = directoryRecurse.getModel()
				.isSelected();

			jEdit.setProperty("multifile.directory.path.value",_directory);
			jEdit.setProperty("multifile.directory.glob.value",_glob);
			jEdit.setBooleanProperty("multifile.directory.recurse.value",_recurse);

			return new DirectoryListSet(_directory,_glob,_recurse);
		}
		else
			return null;
	}

	// EnhancedDialog implementation
	public void ok()
	{
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
		bufferList.setEnabled(selected.getModel().isSelected());
		if(directory.getModel().isSelected())
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
		JPanel labels = new JPanel(new GridLayout(2,1));
		labels.add(new JLabel(jEdit.getProperty("multifile.directory.path"),
			SwingConstants.RIGHT));
		labels.add(new JLabel(jEdit.getProperty("multifile.directory.glob"),
			SwingConstants.RIGHT));

		box.add(BorderLayout.WEST,labels);

		// Second components houses the text fields and buttons.
		JPanel fields = new JPanel(new GridLayout(2,1));

		// Box holding path field and choose button
		JPanel box1 = new JPanel(new BorderLayout());

		Box box2 = new Box(BoxLayout.Y_AXIS);
		box2.add(Box.createGlue());
		directoryPath = new JTextField(MiscUtilities.getFileParent(
			view.getBuffer().getPath()));
		Dimension dim = directoryPath.getPreferredSize();
		dim.width = Integer.MAX_VALUE;
		directoryPath.setMaximumSize(dim);
		box2.add(directoryPath);
		box2.add(Box.createGlue());
		box1.add(BorderLayout.CENTER,box2);

		directoryChoose = new JButton(jEdit.getProperty("multifile"
			+ ".directory.choose"));
		directoryChoose.addActionListener(new ActionHandler());
		box1.add(BorderLayout.EAST,directoryChoose);

		fields.add(box1);

		// Box holding glob field and recurse check box
		JPanel box3 = new JPanel(new BorderLayout());

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
		box3.add(BorderLayout.CENTER,box4);

		directoryRecurse = new JCheckBox(jEdit.getProperty("multifile"
			+ ".directory.recurse"));
		directoryRecurse.getModel().setSelected(jEdit.getBooleanProperty(
			"multifile.directory.recurse.value"));
		directoryRecurse.addActionListener(new ActionHandler());
		box3.add(BorderLayout.EAST,directoryRecurse);

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
