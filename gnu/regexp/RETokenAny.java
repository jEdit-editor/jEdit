/*
 *  gnu/regexp/RETokenAny.java
 *  Copyright (C) 1998-2001 Wes Biggs
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

class RETokenAny extends REToken {
  /** True if '.' can match a newline (RE_DOT_NEWLINE) */
  private boolean m_newline; 

  /** True if '.' can't match a null (RE_DOT_NOT_NULL) */
  private boolean m_null;    
  
  RETokenAny(int f_subIndex, boolean f_newline, boolean f_null) { 
    super(f_subIndex);
    m_newline = f_newline;
    m_null = f_null;
  }

  int getMinimumLength() {
    return 1;
  }

    boolean match(CharIndexed input, REMatch mymatch) {
    char ch = input.charAt(mymatch.index);
    if ((ch == CharIndexed.OUT_OF_BOUNDS)
	|| (!m_newline && (ch == '\n'))
	|| (m_null && (ch == 0)))
      return false;
    
    ++mymatch.index;
    return next(input, mymatch);
  }

  void dump(StringBuffer os) {
    os.append('.');
  }
}

