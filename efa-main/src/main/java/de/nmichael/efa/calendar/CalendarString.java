
package de.nmichael.efa.calendar;

public class CalendarString {

  public static final String CRLF = net.fortuna.ical4j.util.Strings.LINE_SEPARATOR; // "\r\n"

  int day;
  String extraInfo;

  public CalendarString(int day, String extraInfo) {
    this.day = day;
    this.extraInfo = extraInfo;
  }

  public int getDay() {
    return day;
  }

  public String toDisplay() {
    return day + extraInfo;
  }

  @Override
  public String toString() {
    return String.format("%02d", day) + ". " + extraInfo;
  }

}
