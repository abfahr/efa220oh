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

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import de.nmichael.efa.Daten;

// @i18n complete

public class EfaErrorPrintStream extends PrintStream {

  private static final int MAX_STACK_DEPTH_FOR_EFA_ERROR = 5;

  public static volatile boolean ignoreExceptions = false;
  private Object lastErrorObject = null;

  public EfaErrorPrintStream(FileOutputStream f) {
    super(f);
  }

  @Override
  public void print(Object o) {
    errorPrint(o);
    super.print(o);
  }

  @Override
  public void println(Object o) {
    errorPrint(o);
    super.println(o);
  }

  private void errorPrint(Object o) {
    if (!ignoreExceptions &&
        (o.getClass().toString().indexOf("Exception") > 0 ||
            o.getClass().toString().indexOf("java.lang.NoSuchMethodError") > 0 ||
            o.getClass().toString().indexOf("java.lang.NoClassDefFoundError") > 0) &&
            o.toString().indexOf("java.lang.Exception: Stack trace") != 0) { // für Stack-Inkonsistenzen
      // keine Fehlermeldung auf
      // dem Bildschirm anzeigen

      if (o == lastErrorObject) {
        return;
      }
      lastErrorObject = o;

      StringBuilder stacktrace = new StringBuilder();
      // set to true if this exception occurred within efa code (first n stack elements)
      boolean efaError = false;

      // get the stack trace
      try {
        StackTraceElement[] stack = null;
        try {
          stack = ((Exception) o).getStackTrace();
        } catch (Exception e1) {
          try {
            stack = ((NoSuchMethodError) o).getStackTrace();
          } catch (Exception e2) {
            try {
              stack = ((NoClassDefFoundError) o).getStackTrace();
            } catch (Exception e3) {}
          }
        }
        if (stack != null) {
          for (int i = 0; i < stack.length; i++) {
            String s = stack[i].toString();
            if (i < MAX_STACK_DEPTH_FOR_EFA_ERROR && s.contains("de.nmichael.efa")) {
              efaError = true;
            }
            stacktrace.append(s).append("\n");
          }
        }
      } catch (NoSuchMethodError j13) {
        EfaUtil.foo(); // StackTraceElement erst ab Java 1.4
      }

      // if the stack trace concerns classes from efa, ask for bug reports
      // (some other purely java (especially awt/swing) related bugs do not necessarily need to be reported)
      StringBuilder text = new StringBuilder(International.getString("Unerwarteter Programmfehler") + ": " + o);
      if (stacktrace.isEmpty()) {
        // assume this is an efa error (e.g. java.lang.ExceptionInInitializerError don't have a stack trace...)
        efaError = true;
      }
      if (efaError) {
        if (!stacktrace.isEmpty()) {
          text.append("\nStack Trace:\n").append(stacktrace);
        }
        text.append("\n").append(International.getMessage("Bitte melde diesen Fehler an: {efaemail}", Daten.EMAILSUPPORT));
        Logger.log(Logger.ERROR, Logger.MSG_ERROR_EXCEPTION, text.toString());
        if (Daten.isGuiAppl()) {
          new ErrorThread(o.toString(), stacktrace.toString()).start();
        }
      } else {
        text.append("\n").append(International
                .getString("Dieser Fehler ist möglicherweise ein Fehler in Java, der durch ein Java-Update behoben werden kann. "
                        + "Meistens führt diese Aart von Fehlern nur zu vorübergehenden Darstellungsproblemen und hat keine Auswirkung auf efa und die Daten. "
                        + "Sofern dieser Fehler nur selten auftritt und keine erkennbaren Folgen hat, kann er ignoriert werden."));

        String[] lines = stacktrace.toString().split("\n");
        boolean hasEfaLines = false;
        for (String line : lines) {
          if (line.contains("de.nmichael.efa")) {
            if (!hasEfaLines) {
              text.append("\nStack+Trace:");
              hasEfaLines = true;
            }
            text.append("\n").append(line);
          }
        }
        Logger.log(Logger.INFO, Logger.MSG_ERROR_EXCEPTION, text.toString());
      }
    }
  }

  static class ErrorThread extends Thread {

    String message;
    String stacktrace;

    ErrorThread(String message, String stacktrace) {
      this.message = message;
      this.stacktrace = stacktrace;
    }

    @Override
    public void run() {
      Dialog.exceptionError(message, stacktrace);
    }
  }
}
