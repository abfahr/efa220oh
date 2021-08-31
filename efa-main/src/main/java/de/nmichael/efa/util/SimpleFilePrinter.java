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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import de.nmichael.efa.Daten;

// @i18n complete

public class SimpleFilePrinter implements Printable {

  private static final int DPI = 72; // Referenzauflösung des Drucksystems
  private static final double CM = DPI / 2.54; // Dots pro Zentimeter
  private static final double MM = DPI / 25.4; // Dots pro Millimeter
  private static final double MARGIN_TOP = 0.0 * CM; // zusätzlicher Seitenrand oben

  // folgende Werte werden im Konstruktor gesetzt (hier nur Beispielwerte)
  private static double OVERLAP = 5 * MM; // Überlappung bei mehrseitigen Dokumenten
  private static double PAGE_WIDTH = 595.0; // Seitenbreite (total)
  private static double PAGE_HEIGHT = 842.0; // Seitenhöhe (total)
  private static double PAGE_X = 17.0; // linker Rand
  private static double PAGE_Y = 22.0; // oberer Rand
  private static double PAGE_W = 560.0; // Seitenbreite (nutzbar)
  private static double PAGE_H = 797.0; // Seitenhöhe (nutzbar)

  private PrinterJob pjob; // Printjob
  private PageFormat pageformat; // Seitenformat
  public static PageFormat pageSetup = null; // Seitenformat-Setup
  private JEditorPane out; // HTML-Ausgabe

  public SimpleFilePrinter(JEditorPane out) {
    this.pjob = PrinterJob.getPrinterJob();
    this.out = out;

    // Seitenlayout
    if (Daten.efaConfig != null) {
      SimpleFilePrinter.PAGE_WIDTH = (Daten.efaConfig.getValuePrintPageWidth()) * MM;
      SimpleFilePrinter.PAGE_HEIGHT = (Daten.efaConfig.getValuePrintPageHeight()) * MM;
      SimpleFilePrinter.PAGE_X = (Daten.efaConfig.getValuePrintLeftMargin()) * MM;
      SimpleFilePrinter.PAGE_Y = (Daten.efaConfig.getValuePrintTopMargin()) * MM;
      SimpleFilePrinter.PAGE_W = PAGE_WIDTH - 2 * PAGE_X;
      SimpleFilePrinter.PAGE_H = PAGE_HEIGHT - 2 * PAGE_Y;
      SimpleFilePrinter.OVERLAP = (Daten.efaConfig.getValuePrintPageOverlap()) * MM;
    } else {
      SimpleFilePrinter.PAGE_WIDTH = 210f * MM;
      SimpleFilePrinter.PAGE_HEIGHT = 297f * MM;
      SimpleFilePrinter.PAGE_X = 15f * MM;
      SimpleFilePrinter.PAGE_Y = 15f * MM;
      SimpleFilePrinter.PAGE_W = PAGE_WIDTH - 2 * PAGE_X;
      SimpleFilePrinter.PAGE_H = PAGE_HEIGHT - 2 * PAGE_Y;
      SimpleFilePrinter.OVERLAP = 5f * MM;
    }
  }

  public boolean setupPageFormat() {
    try {
      PageFormat defaultPF = pjob.defaultPage();
      if (pageSetup == null) {
        this.pageformat = (PageFormat) defaultPF.clone();
        Paper p = new Paper();
        p.setSize(PAGE_WIDTH, PAGE_HEIGHT);
        p.setImageableArea(PAGE_X, PAGE_Y, PAGE_W, PAGE_H);
        this.pageformat.setPaper(p);
      } else {
        this.pageformat = pageSetup;
      }
      pjob.setPrintable(this, this.pageformat);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public boolean setupJobOptions() {
    try {
      return pjob.printDialog();
    } catch (Exception e) {
      return false;
    }
  }

  public void printFile() throws PrinterException {
    pjob.print();
  }

  // ---Implementierung von Printable-------------------
  @Override
  public int print(Graphics g, PageFormat pf, int page) throws PrinterException {
    double scale = out.getWidth() / pf.getImageableWidth();

    int offset = (int) ((pf.getImageableHeight() - MARGIN_TOP - OVERLAP) * scale * page);
    if (offset >= out.getHeight()) {
      return NO_SUCH_PAGE;
    }

    if (Logger.isTraceOn(Logger.TT_PRINT_FTP)) {
      Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_SIMPLEFILEPRINTER, "Printing page " + (page + 1)
          + "...");
      Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_SIMPLEFILEPRINTER,
          "PageMargin: " + pf.getImageableX() + " ; " + pf.getImageableY());
      Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_SIMPLEFILEPRINTER,
          "PageUsable: " + pf.getImageableWidth() + " x " + pf.getImageableHeight());
      Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_SIMPLEFILEPRINTER, "PageSize  : " + pf.getWidth()
          + " x " + pf.getHeight());
      Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_SIMPLEFILEPRINTER, "Document  : " + out.getWidth()
          + " x " + out.getHeight());
    }

    Graphics2D g2 = (Graphics2D) g;
    g2.scale(1.0 / scale, 1.0 / scale);
    g.translate((int) (pf.getImageableX() * scale),
        (int) ((pf.getImageableY() + MARGIN_TOP) * scale) - offset);
    out.paint(g);

    return PAGE_EXISTS;
  }

  public static void sizeJEditorPane(JEditorPane out) {
    JScrollPane pane = new JScrollPane();
    pane.setSize(new Dimension(1000, 1000));
    pane.getViewport().add(out, null);
    pane.doLayout();
    out.doLayout();
    try {
      Thread.sleep(250);
    } catch (Exception eqwe) {}
    /*
     * System.out.println(out.getText()); System.out.println(out.getSize());
     * System.out.println(out.getMinimumSize()); System.out.println(out.getPreferredSize());
     */
    double height = out.getPreferredSize().getHeight() + 50;
    out.setSize(new Dimension(1000, (int) height));
  }

}
