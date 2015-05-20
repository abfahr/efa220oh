/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.ex;

import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.util.Dialog;

public class InvalidValueException extends Exception {

  /**
   *
   */
  private static final long serialVersionUID = 1L;
  IItemType item;
  String msg;

  public InvalidValueException(IItemType item, String msg) {
    this.item = item;
    this.msg = msg;
  }

  @Override
  public String toString() {
    return msg;
  }

  @Override
  public String getMessage() {
    return msg;
  }

  public void displayMessage() {
    if (msg != null) {
      Dialog.error(msg);
    }
    if (item != null) {
      item.requestFocus();
    }
  }

}
