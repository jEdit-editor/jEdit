/*
 *  gnu/regexp/REFilterReader.java
 *  Copyright (C) 2001 Lee Sau Dan
 *  Based on gnu.regexp.REFilterInputStream by Wes Biggs
 *
 *  This library is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published
 *  by the Free Software Foundation; either version 2.1 of the License, or
 *  (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package gnu.regexp;
import java.io.FilterReader;
import java.io.Reader;

/**
 * Replaces instances of a given RE with replacement text. 
 *
 * @author <A HREF="http://www.csis.hku.hk/~sdlee/">Lee Sau Dan</A>
 * @since gnu.regexp 1.1.0
 */

public class REFilterReader extends FilterReader {

  private RE m_expr;
  private String m_replace;
  private String m_buffer;
  private int m_bufpos;
  private int m_offset;
  private CharIndexedReader m_stream;

  /**
   * Creates an REFilterReader.  When reading from this stream,
   * occurrences of patterns matching the supplied regular expression
   * will be replaced with the supplied replacement text (the
   * metacharacters $0 through $9 may be used to refer to the full
   * match or subexpression matches.
   *
   * @param f_stream The Reader to be filtered.
   * @param f_expr The regular expression to search for.
   * @param f_replace The text pattern to replace matches with.  
   */
  public REFilterReader(Reader f_stream, RE f_expr, String f_replace) {
    super(f_stream);
    m_stream = new CharIndexedReader(f_stream,0);
    m_expr = f_expr;
    m_replace = f_replace;
  }

  /**
   * Reads the next byte from the stream per the general contract of
   * Reader.read().  Returns -1 on error or end of stream.
   */
  public int read() {
    // If we have buffered replace data, use it.
    if ((m_buffer != null) && (m_bufpos < m_buffer.length())) {
      return (int) m_buffer.charAt(m_bufpos++);
    }

    // check if input is at a valid position
    if (!m_stream.isValid()) return -1;

    REMatch mymatch = new REMatch(m_expr.getNumSubs(),m_offset,0);
    if (m_expr.match(m_stream,mymatch)) {
      mymatch.end[0] = mymatch.index;
      mymatch.finish(m_stream);
      m_stream.move(mymatch.toString().length());
      m_offset += mymatch.toString().length();
      m_buffer = mymatch.substituteInto(m_replace);
      m_bufpos = 1;

      if (m_buffer.length() > 0) {
	  return m_buffer.charAt(0);
      }
    }
    char ch = m_stream.charAt(0);
    if (ch == CharIndexed.OUT_OF_BOUNDS) return -1;
    m_stream.move(1);
    m_offset++;
    return ch;
  }

  /** 
   * Returns false.  REFilterReader does not support mark() and
   * reset() methods. 
   */
  public boolean markSupported() {
    return false;
  }

  /** Reads from the stream into the provided array. */
  public int read(byte[] b, int off, int len) {
    int i;
    int ok = 0;
    while (len-- > 0) {
      i = read();
      if (i == -1) return (ok == 0) ? -1 : ok;
      b[off++] = (byte) i;
      ok++;
    }
    return ok;
  }

  /** Reads from the stream into the provided array. */
  public int read(byte[] b) {
    return read(b,0,b.length);
  }
}
