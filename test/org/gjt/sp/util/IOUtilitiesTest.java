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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class IOUtilitiesTest {
    public static final String CONTENT = "Hello World";

    @Test
    public void moveFile() throws IOException {
        var source = buildTmpSourceFile();
        var target = new File(source.getParentFile(), "destination.txt");
        IOUtilities.moveFile(source, target);
        assertFalse("Source file still exists", source.exists());
        assertEquals("The destination file do not exist or has a different content", CONTENT, Files.readString(target.toPath()));
    }

    @Test
    public void testFileLength() throws IOException {
        var source = buildTmpSourceFile();
        assertEquals(CONTENT.length(), IOUtilities.fileLength(source));
    }

    private static File buildTmpSourceFile() throws IOException {
        var source = File.createTempFile("source", ".txt");
        source.deleteOnExit();
        Files.writeString(source.toPath(), CONTENT);
        return source;
    }
}