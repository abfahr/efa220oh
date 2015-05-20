/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.drv;

import java.awt.AWTEvent;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.JComponent;
import javax.swing.JDialog;

import de.nmichael.efa.util.ActionHandler;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;

// @i18n complete (needs no internationalization -- only relevant for Germany)

public class EmptyFrame extends JDialog implements ActionListener {
  /**
   *
   */
  private static final long serialVersionUID = 1L;
  Frame parent;

  public EmptyFrame(Frame parent) {
    super(parent);
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    Dialog.frameOpened(this);
    try {
      jbInit();
    } catch (Exception e) {
      e.printStackTrace();
    }
    EfaUtil.pack(this);
    this.parent = parent;
    // this.requestFocus();
  }

  // ActionHandler Events
  public void keyAction(ActionEvent evt) {
    if (evt == null || evt.getActionCommand() == null) {
      return;
    }
    if (evt.getActionCommand().equals("KEYSTROKE_ACTION_0")) { // Escape
      cancel();
    }
  }

  // Initialisierung des Frames
  private void jbInit() throws Exception {
    ActionHandler ah = new ActionHandler(this);
    try {
      ah.addKeyActions(getRootPane(), JComponent.WHEN_IN_FOCUSED_WINDOW,
          new String[] { "ESCAPE", "F1" }, new String[] { "keyAction", "keyAction" });
    } catch (NoSuchMethodException e) {
      System.err.println("Error setting up ActionHandler");
    }
  }

  /** Overridden so we can exit when window is closed */
  @Override
  protected void processWindowEvent(WindowEvent e) {
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      cancel();
    }
    super.processWindowEvent(e);
  }

  /** Close the dialog */
  void cancel() {
    Dialog.frameClosed(this);
    dispose();
  }

  /** Close the dialog on a button event */
  @Override
  public void actionPerformed(ActionEvent e) {}

}
