/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;

import de.nmichael.efa.Daten;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;

public class NotificationDialog extends BaseDialog {

  private static final long serialVersionUID = 1L;
  private String text;
  private String image;
  private String textcolor;
  private String bkcolor;
  private volatile int closeTimeout;
  private JLabel closeInfoLabel;
  private boolean _canceled = false;

  public NotificationDialog(Frame parent, String text, String image, String textcolor,
      String bgcolor, int closeTimeout) {
    super(parent, "Notification", International.getStringWithMnemonic("Schließen"));
    this.text = text;
    this.image = image;
    this.textcolor = textcolor;
    this.bkcolor = bgcolor;
    this.closeTimeout = closeTimeout;
    this._closeButtonText = null;
  }

  @Override
  protected void iniDialog() throws Exception {
    this.setUndecorated(true);
    mainPanel.setLayout(new BorderLayout());

    JTextPane t = new JTextPane();
    t.setContentType("text/html");
    t.setEditable(false);
    t.setText("<html><body bgcolor=\"#"
        + bkcolor
        + "\">"
        +
        "<table cellpadding=\"20\" align=\"center\"><tr>"
        +
        "<td><img src=\""
        + EfaUtil.saveImage(image, "png", Daten.efaTmpDirectory,
            true, false, true)
        + "\"></td>"
        +
        "<td align=\"center\" valign=\"middle\" style=\"font-family:sans-serif; font-size:24pt; font-weight:bold; color:#ffffff\">"
        + text + "</td>" +
        "</tr></table>" +
        "</html");
    t.addMouseListener(new java.awt.event.MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        cancel();
      }
    });

    t.setPreferredSize(new Dimension(600, 175));
    mainPanel.add(t, BorderLayout.CENTER);
    closeInfoLabel = new JLabel();
    closeInfoLabel.setHorizontalAlignment(SwingConstants.CENTER);
    closeInfoLabel.setHorizontalTextPosition(SwingConstants.CENTER);
    mainPanel.add(closeInfoLabel, BorderLayout.SOUTH);
    if (closeTimeout > 0) {
      (new CloseTimeoutThread()).start();
    }
  }

  @Override
  public void keyAction(ActionEvent evt) {
    _keyAction(evt);
  }

  @Override
  public boolean cancel() {
    if (!_canceled) {
      _canceled = true;
      closeTimeout = 0;
      return super.cancel();
    }
    return true;
  }

  class CloseTimeoutThread extends Thread {
    @Override
    public void run() {
      for (int i = 0; i < closeTimeout; i++) {
        closeInfoLabel.setText(
            International.getMessage("Dieses Fenster schließt automatisch in {sec} Sekunden ...",
                Math.max(closeTimeout - i, 0)));
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {}
      }
      cancel();
    }
  }

}
