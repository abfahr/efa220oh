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
import java.util.UUID;

import javax.swing.JDialog;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.data.StatusRecord;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.util.International;

// @i18n complete
public class StatusListDialog extends DataListDialog {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public StatusListDialog(Frame parent, AdminRecord admin) {
    super(parent, International.getString("Status"),
        Daten.project.getStatus(false), 0, admin);
  }

  public StatusListDialog(JDialog parent, AdminRecord admin) {
    super(parent, International.getString("Status"),
        Daten.project.getStatus(false), 0, admin);
  }

  @Override
  public void keyAction(ActionEvent evt) {
    _keyAction(evt);
  }

  @Override
  public DataEditDialog createNewDataEditDialog(JDialog parent, StorageObject persistence,
      DataRecord record) {
    boolean newRecord = (record == null);
    if (record == null) {
      record = Daten.project.getStatus(false).createStatusRecord(UUID.randomUUID());
      ((StatusRecord) record).setType(StatusRecord.TYPE_USER);
    }
    return new StatusEditDialog(parent, (StatusRecord) record, newRecord, admin);
  }

}
