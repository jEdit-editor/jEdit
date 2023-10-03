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

package org.gjt.sp.jedit.textarea;

import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.Primitives;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(MockitoJUnitRunner.class)
public class LineCharacterBreakerTest {
    @Mock
    private TextArea textArea;
    @Mock
    private JEditBuffer buffer;

    @Before
    public void setUp() throws Exception {
        Mockito.reset(textArea, buffer);
    }

    @Test
    public void testNextOf() {
        var line = 10;
        var integerArgumentCaptor = ArgumentCaptor.forClass(Primitives.primitiveTypeOf(int.class));
        Mockito.when(textArea.getLineOfOffset(integerArgumentCaptor.capture())).thenReturn(line);
        Mockito.when(textArea.getBuffer()).thenReturn(buffer);
        Mockito.when(buffer.getLineSegment(line)).thenReturn("732K    /run");
        Mockito.when(textArea.getLineStartOffset(line)).thenReturn(136);
        var breaker = new TextArea.LineCharacterBreaker(textArea, 148);
        assertEquals(149, breaker.nextOf(148));
    }

    @Test
    public void testPreviousOf() {
        var line = 10;
        var integerArgumentCaptor = ArgumentCaptor.forClass(Primitives.primitiveTypeOf(int.class));
        Mockito.when(textArea.getLineOfOffset(integerArgumentCaptor.capture())).thenReturn(line);
        Mockito.when(textArea.getBuffer()).thenReturn(buffer);
        Mockito.when(buffer.getLineSegment(line)).thenReturn("732K    /run");
        Mockito.when(textArea.getLineStartOffset(line)).thenReturn(136);
        var breaker = new TextArea.LineCharacterBreaker(textArea, 148);
        assertEquals(136, breaker.previousOf(137));
    }
}