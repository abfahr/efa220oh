/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui.util;

public class TableItemHeader {

  private String txt;
  private int maxColumnWidth = -1;

  public TableItemHeader(String txt) {
    this.txt = txt;
    setMaxColumnWidth(txt.length());
  }

  @Override
  public String toString() {
    return txt;
  }

  public void updateColumnWidth(String cellContent) {
    if (cellContent != null && cellContent.length() > maxColumnWidth) {
      maxColumnWidth = cellContent.length();
    }
  }

  public void setMaxColumnWidth(int width) {
    maxColumnWidth = width;
  }

  public int getMaxColumnWidth() {
    return maxColumnWidth;
  }

}
