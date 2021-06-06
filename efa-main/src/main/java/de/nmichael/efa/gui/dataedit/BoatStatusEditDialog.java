/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui.dataedit;

import java.awt.Frame;
import java.awt.event.ActionEvent;

import javax.swing.JDialog;

import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.data.BoatStatusRecord;
import de.nmichael.efa.util.International;

// @i18n complete
public class BoatStatusEditDialog extends UnversionizedDataEditDialog {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public BoatStatusEditDialog(Frame parent, BoatStatusRecord r, boolean newRecord, AdminRecord admin) {
    super(parent, International.getString("Bootsstatus"), r, newRecord, admin);
  }

  public BoatStatusEditDialog(JDialog parent, BoatStatusRecord r, boolean newRecord,
      AdminRecord admin) {
    super(parent, International.getString("Bootsstatus"), r, newRecord, admin);
  }

  @Override
  public void keyAction(ActionEvent evt) {
    _keyAction(evt);
  }

}
