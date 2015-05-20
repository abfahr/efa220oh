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
import de.nmichael.efa.data.PersonRecord;
import de.nmichael.efa.data.Persons;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.ProgressTask;

// @i18n complete
public class PersonListDialog extends DataListDialog {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public PersonListDialog(Frame parent, long validAt, AdminRecord admin) {
    super(parent, International.getString("Personen"),
        Daten.project.getPersons(false), validAt, admin);
    if (admin != null && admin.isAllowedEditPersons()) {
      addMergeAction();
    }
  }

  public PersonListDialog(JDialog parent, long validAt, AdminRecord admin) {
    super(parent, International.getString("Personen"),
        Daten.project.getPersons(false), validAt, admin);
    if (admin != null && admin.isAllowedEditPersons()) {
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
      record = Daten.project.getPersons(false).createPersonRecord(UUID.randomUUID());
    }
    return new PersonEditDialog(parent, (PersonRecord) record, newRecord, admin);
  }

  @Override
  protected ProgressTask getMergeProgressTask(DataKey mainKey, DataKey[] mergeKeys) {
    Persons persons = (Persons) persistence;
    return persons.getMergePersonsProgressTask(mainKey, mergeKeys);
  }

}
