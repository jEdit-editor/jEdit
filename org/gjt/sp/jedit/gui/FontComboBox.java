/*
 * FontComboBox.java - Font selector
 * Copyright (C) 1999 Jason Ginchereau
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

import java.awt.*;
import javax.swing.*;

/**
 * A ComboBox for selecting a font family name.
 * The selected item is rendered in the selected font.
 * When possible (in JDK 1.2 or later), all system font
 * names are listed in addition to the 5 default
 * Java symbolic font names.
 * <p>
 * Though it takes advantage of font features only
 * available in JDK 1.2 or later, this class still runs
 * properly in JDK 1.1.
 *
 * @author Jason Ginchereau
 * @version 10/20/99
 */
public class FontComboBox extends JComboBox {

   /**
    * Creates a new FontComboBox with the set of all available
    * font family names.
    */
   public FontComboBox() {
      this(getAvailableFontFamilyNames());

   }
   
   /**
    * Creates a new FontComboBox with a specific set of
    * font family names.
    */
   public FontComboBox(String[] fontFamilyNames) {
      super(fontFamilyNames);
      setRenderer(new FontRenderer());
   }
   
   /**
    * For some reason the default Java fonts show up in the
    * list with .bold, .bolditalic, and .italic extensions.
    */
   private static final String[] HIDEFONTS = {
      ".bold",
      ".italic"
   };
   
   /**
    * Gets a list of all available font family names.
    * When possible (in JDK 1.2 or later), this will
    * return GraphicsEnvironment.getAvailableFontFamilyNames().
    * However this method gracefully degrades to returning
    * Toolkit.getFontList() in JDK 1.1.
    */
   public static String[] getAvailableFontFamilyNames() {
      try {
         /* return GraphicsEnvironment.getLocalGraphicsEnvironment()
                                   .getAvailableFontFamilyNames(); */
         Class GEClass = Class.forName("java.awt.GraphicsEnvironment");
         Object GEInstance =
               GEClass.getMethod("getLocalGraphicsEnvironment", null)
               .invoke(null, null);
         String[] nameArray = (String[])
               GEClass.getMethod("getAvailableFontFamilyNames", null)
               .invoke(GEInstance, null);
         java.util.Vector nameVector = new java.util.Vector(nameArray.length);
         for(int i = 0, j; i < nameArray.length; i++) {
            for(j = 0; j < HIDEFONTS.length; j++)
               if(nameArray[i].indexOf(HIDEFONTS[j]) >= 0) break;
            if(j == HIDEFONTS.length)
               nameVector.addElement(nameArray[i]);
         }
	 String[] _array = new String[nameVector.size()];
	 nameVector.copyInto(_array);
	 return _array;
      } catch(Exception ex) {
         return Toolkit.getDefaultToolkit().getFontList();
      }
   }

   /**
    * Renders the selected item in the selected font, but all other items
    * in the list's default font.
    */
   private class FontRenderer extends JLabel implements ListCellRenderer {
      private ListCellRenderer defaultRenderer = new DefaultListCellRenderer();
      
      public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
         if(index == -1) {
            String fontFamily = (String) value;
            setText(fontFamily);
            Font renderFont = new Font(fontFamily,
                  FontComboBox.this.getFont().getStyle(),
                  FontComboBox.this.getFont().getSize());        
            super.setFont(renderFont);
            return this;
         }
         else {
            return defaultRenderer.getListCellRendererComponent(
                  list, value, index, isSelected, cellHasFocus);
         }
      }
      
      public void setFont(Font font) {
         // Prevent the ComboBoxUI from setting the font to the ComboBox's font.
      }
   }
}
