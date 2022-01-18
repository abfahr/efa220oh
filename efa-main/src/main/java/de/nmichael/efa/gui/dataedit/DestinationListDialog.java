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
import de.nmichael.efa.data.DestinationRecord;
import de.nmichael.efa.data.Destinations;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.ProgressTask;

// @i18n complete
public class DestinationListDialog extends DataListDialog {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public DestinationListDialog(Frame parent, long validAt, AdminRecord admin) {
    super(parent,
        International.getString("Ziele") + " / " +
            International.getString("Strecken"),
            Daten.project.getDestinations(false), validAt, admin);
    if (admin != null && admin.isAllowedEditDestinations()) {
      addMergeAction();
    }
  }

  public DestinationListDialog(JDialog parent, long validAt, AdminRecord admin) {
    super(parent,
        International.getString("Ziele") + " / " +
            International.getString("Strecken"),
            Daten.project.getDestinations(false), validAt, admin);
    if (admin != null && admin.isAllowedEditDestinations()) {
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
      record = Daten.project.getDestinations(false).createDestinationRecord(UUID.randomUUID());
    }
    return new DestinationEditDialog(parent, (DestinationRecord) record, newRecord, admin);
  }

  @Override
  protected ProgressTask getMergeProgressTask(DataKey mainKey, DataKey[] mergeKeys) {
    Destinations destinations = (Destinations) persistence;
    return destinations.getMergeDestinationsProgressTask(mainKey, mergeKeys);
  }

}
