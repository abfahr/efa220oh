/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.util;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Vector;

// @i18n complete

public class CaptureOutputPrintStream extends PrintStream {

  private Vector lines;

  public CaptureOutputPrintStream(OutputStream f) {
    super(f);
    lines = new Vector();
  }

  @Override
  public void print(Object o) {
    lines.add(o);
  }

  @Override
  public void print(String s) {
    lines.add(s);
  }

  public Vector getLines() {
    return lines;
  }

}
