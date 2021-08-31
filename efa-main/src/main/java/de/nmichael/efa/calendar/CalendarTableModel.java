
package de.nmichael.efa.calendar;

import javax.swing.table.DefaultTableModel;

public class CalendarTableModel extends DefaultTableModel {
  private static final long serialVersionUID = 5275055643548894506L;

  @Override
  public boolean isCellEditable(int rowIndex, int mColIndex) {
    return false;
  }
}
