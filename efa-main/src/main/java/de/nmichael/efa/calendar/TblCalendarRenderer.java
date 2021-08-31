
package de.nmichael.efa.calendar;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;

public class TblCalendarRenderer extends DefaultTableCellRenderer {
  private static final long serialVersionUID = 8256072858414595273L;

  int realYear, realMonth, realDay;
  int currentYear, currentMonth, currentDay;

  public TblCalendarRenderer(int realYear, int realMonth, int realDay,
      int currentYear, int currentMonth, int currentDay) {
    super();
    this.realYear = realYear;
    this.realMonth = realMonth;
    this.realDay = realDay;
    this.currentYear = currentYear;
    this.currentMonth = currentMonth;
    this.currentDay = currentDay;
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
      boolean focused, int row, int column) {
    super.getTableCellRendererComponent(table, value, selected, focused, row, column);
    setBorder(null);
    setForeground(Color.black);
    if (column == 5 || column == 6) { // Week-end
      setBackground(new Color(255, 220, 220));
      setBackground(Color.yellow);
    }
    else { // Week
      setBackground(Color.white);
    }
    if (value != null) {
      if (currentYear == realYear
          && currentMonth == realMonth
          && ((CalendarString) value).getDay() == realDay) { // Today
        setBackground(new Color(220, 220, 255));
        setBackground(Color.blue);
      }
      if (((CalendarString) value).getDay() == currentDay) { // Selected Day
        setBorder(new LineBorder(Color.black));
      }
    }
    return this;
  }
}
