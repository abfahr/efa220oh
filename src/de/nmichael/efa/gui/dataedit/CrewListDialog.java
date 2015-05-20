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
import de.nmichael.efa.data.CrewRecord;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.util.International;

// @i18n complete
public class CrewListDialog extends DataListDialog {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public CrewListDialog(Frame parent, AdminRecord admin) {
    super(parent, International.getString("Mannschaften"), Daten.project.getCrews(false), 0, admin);
  }

  public CrewListDialog(JDialog parent, AdminRecord admin) {
    super(parent, International.getString("Mannschaften"), Daten.project.getCrews(false), 0, admin);
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
      record = Daten.project.getCrews(false).createCrewRecord(UUID.randomUUID());
    }
    return new CrewEditDialog(parent, (CrewRecord) record, newRecord, admin);
  }
}
