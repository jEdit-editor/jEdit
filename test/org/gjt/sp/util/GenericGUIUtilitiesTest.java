/*
 * jEdit - Programmer's Text Editor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2023 jEdit contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.util;

import org.junit.Test;

import javax.swing.*;

import static org.junit.Assert.assertEquals;

public class GenericGUIUtilitiesTest {

    @Test
    public void prettifyMenuLabel() {
        assertEquals("File", GenericGUIUtilities.prettifyMenuLabel("File"));
        assertEquals("File", GenericGUIUtilities.prettifyMenuLabel("$File"));
        assertEquals("New in Mode", GenericGUIUtilities.prettifyMenuLabel("New in $Mode"));
    }

    @Test
    public void testSetAutoMnemonic() {
        var button = new JButton("$Plugins");
        GenericGUIUtilities.setAutoMnemonic(button);
        // see AbstractButton.setMnemonic()
        assertEquals('p' - ('a' - 'A'), button.getMnemonic());
        assertEquals("Plugins", button.getText());
    }
}