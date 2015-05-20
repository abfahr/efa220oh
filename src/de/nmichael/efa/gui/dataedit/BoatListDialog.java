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
import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.data.BoatRecord;
import de.nmichael.efa.data.Boats;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.ProgressTask;

// @i18n complete
public class BoatListDialog extends DataListDialog {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public BoatListDialog(Frame parent, long validAt, AdminRecord admin) {
    super(parent, International.getString("Boote"), Daten.project.getBoats(false), validAt, admin);
    if (admin != null && admin.isAllowedEditBoats()) {
      addMergeAction();
    }
  }

  public BoatListDialog(JDialog parent, long validAt, AdminRecord admin) {
    super(parent, International.getString("Boote"), Daten.project.getBoats(false), validAt, admin);
    if (admin != null && admin.isAllowedEditBoats()) {
      addMergeAction();
    }
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
      record = Daten.project.getBoats(false).createBoatRecord(UUID.randomUUID());
      ((BoatRecord) record).addTypeVariant("", EfaTypes.TYPE_BOAT_OTHER,
          EfaTypes.TYPE_NUMSEATS_OTHER,
          EfaTypes.TYPE_RIGGING_OTHER, EfaTypes.TYPE_COXING_OTHER, Boolean.toString(true));
    }
    return new BoatEditDialog(parent, (BoatRecord) record, newRecord, admin);
  }

  @Override
  protected ProgressTask getMergeProgressTask(DataKey mainKey, DataKey[] mergeKeys) {
    Boats boats = (Boats) persistence;
    return boats.getMergeBoatsProgressTask(mainKey, mergeKeys);
  }

}
