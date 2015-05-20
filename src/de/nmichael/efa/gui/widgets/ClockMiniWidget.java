/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui.widgets;

import javax.swing.JComponent;
import javax.swing.JLabel;

import de.nmichael.efa.util.EfaUtil;

public class ClockMiniWidget {

  private JLabel label = new JLabel();
  private ClockUpdater clockUpdater;

  public ClockMiniWidget() {
    label.setText("12:34");
    clockUpdater = new ClockUpdater();
    clockUpdater.start();
  }

  public void stopClock() {
    clockUpdater.stopClock();
  }

  public JComponent getGuiComponent() {
    return label;
  }

  class ClockUpdater extends Thread {

    volatile boolean keepRunning = true;

    @Override
    public void run() {
      while (keepRunning) {
        try {
          label.setText(EfaUtil.getCurrentTimeStampHHMM());
          Thread.sleep(60000);
        } catch (Exception e) {
          EfaUtil.foo();
        }
      }
    }

    public void stopClock() {
      keepRunning = false;
    }

  }

}
