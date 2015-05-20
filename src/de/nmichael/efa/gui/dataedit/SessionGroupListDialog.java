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
import javax.swing.ListSelectionModel;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.ItemTypeDataRecordTable;
import de.nmichael.efa.data.SessionGroupRecord;
import de.nmichael.efa.data.SessionGroups;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.util.International;

// @i18n complete
public class SessionGroupListDialog extends DataListDialog {

  /**
   *
   */
  private static final long serialVersionUID = 1L;
  public static final int ACTION_SELECT = 300;
  public static final int ACTION_UNSELECT = 301;

  private String logbook;
  private UUID selectedSessionGroupId;
  private boolean modeSelectSessionGroup;
  private SessionGroupRecord selectedRecord;

  public SessionGroupListDialog(Frame parent, String logbook, AdminRecord admin) {
    super(parent, International.getString("Fahrtgruppen"),
        Daten.project.getSessionGroups(false), 0, admin);
    iniValues(logbook, null, false);
  }

  public SessionGroupListDialog(JDialog parent, String logbook, AdminRecord admin) {
    super(parent, International.getString("Fahrtgruppen"),
        Daten.project.getSessionGroups(false), 0, admin);
    iniValues(logbook, null, false);
  }

  public SessionGroupListDialog(Frame parent, String logbook, UUID selectedSessionGroupId,
      AdminRecord admin) {
    super(parent, International.getString("Fahrtgruppen"),
        Daten.project.getSessionGroups(false), 0, admin);
    iniValues(logbook, selectedSessionGroupId, true);
  }

  public SessionGroupListDialog(JDialog parent, String logbook, UUID selectedSessionGroupId,
      AdminRecord admin) {
    super(parent, International.getString("Fahrtgruppen"),
        Daten.project.getSessionGroups(false), 0, admin);
    iniValues(logbook, selectedSessionGroupId, true);
  }

  private void iniValues(String logbook, UUID selectedSessionGroupId, boolean modeSelectSessionGroup) {
    this.logbook = logbook;
    this.selectedSessionGroupId = selectedSessionGroupId;
    this.modeSelectSessionGroup = modeSelectSessionGroup;
    if (logbook != null) {
      this.filterFieldName = SessionGroupRecord.LOGBOOK;
      this.filterFieldValue = logbook;
    }
    if (modeSelectSessionGroup) {
      actionText = new String[] {
          ItemTypeDataRecordTable.ACTIONTEXT_NEW,
          ItemTypeDataRecordTable.ACTIONTEXT_EDIT,
          ItemTypeDataRecordTable.ACTIONTEXT_DELETE,
          International.getString("Fahrtgruppe auswählen"),
          International.getString("Auswahl aufheben")
      };
      actionType = new int[] {
          ItemTypeDataRecordTable.ACTION_NEW,
          ItemTypeDataRecordTable.ACTION_EDIT,
          ItemTypeDataRecordTable.ACTION_DELETE,
          ACTION_SELECT,
          ACTION_UNSELECT
      };
    }
    intelligentColumnWidth = false;
  }

  @Override
  protected void iniDialog() throws Exception {
    super.iniDialog();
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setDefaultActionForDoubleclick(ACTION_SELECT);
    if (selectedSessionGroupId != null) {
      table.selectValue(selectedSessionGroupId.toString());
    }
  }

  @Override
  public void keyAction(ActionEvent evt) {
    _keyAction(evt);
  }

  @Override
  public void itemListenerActionTable(int actionId, DataRecord[] records) {
    super.itemListenerActionTable(actionId, records);
    switch (actionId) {
      case ACTION_SELECT:
        if (records != null && records.length == 1 && records[0] != null) {
          selectedRecord = (SessionGroupRecord) records[0];
          setDialogResult(true);
          cancel();
        }
        break;
      case ACTION_UNSELECT:
        selectedRecord = null;
        setDialogResult(true);
        cancel();
        break;
    }
  }

  @Override
  public DataEditDialog createNewDataEditDialog(JDialog parent, StorageObject persistence,
      DataRecord record) {
    boolean newRecord = (record == null);
    if (record == null && persistence != null && filterFieldValue != null) {
      record = ((SessionGroups) persistence).createSessionGroupRecord(UUID.randomUUID(),
          filterFieldValue);
    }
    if (record == null) {
      return null;
    }
    if (logbook != null && logbook.length() > 0) {
      ((SessionGroupRecord) record).setLogbook(logbook);
    }
    return new SessionGroupEditDialog(parent, (SessionGroupRecord) record, newRecord, admin);
  }

  public SessionGroupRecord getSelectedSessionGroupRecord() {
    return selectedRecord;
  }

}
