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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import javax.swing.JDialog;

import de.nmichael.efa.Daten;
import de.nmichael.efa.calendar.ICalendarExport;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeDataRecordTable;
import de.nmichael.efa.core.items.ItemTypeStringAutoComplete;
import de.nmichael.efa.data.BoatRecord;
import de.nmichael.efa.data.BoatReservationRecord;
import de.nmichael.efa.data.BoatReservations;
import de.nmichael.efa.data.Boats;
import de.nmichael.efa.data.storage.DataExport;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.gui.SimpleInputDialog;
import de.nmichael.efa.gui.util.AutoCompleteList;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

// @i18n complete
public class BoatReservationListDialog extends DataListDialog {

  private static final long serialVersionUID = 1L;
  boolean allowNewReservationsWeekly = true;

  public BoatReservationListDialog(Frame parent, AdminRecord admin) {
    super(parent, International.getString("Bootsreservierungen"), Daten.project
        .getBoatReservations(false), 0, admin);
    iniValues(null, true, true, true);
  }

  public BoatReservationListDialog(JDialog parent, AdminRecord admin) {
    super(parent, International.getString("Bootsreservierungen"), Daten.project
        .getBoatReservations(false), 0, admin);
    iniValues(null, true, true, true);
  }

  public BoatReservationListDialog(Frame parent, UUID boatId, AdminRecord admin) {
    super(parent, International.getString("Bootsreservierungen"), Daten.project
        .getBoatReservations(false), 0, admin);
    iniValues(boatId, true, true, true);
  }

  public BoatReservationListDialog(JDialog parent, UUID boatId, AdminRecord admin) {
    super(parent, International.getString("Bootsreservierungen"), Daten.project
        .getBoatReservations(false), 0, admin);
    iniValues(boatId, true, true, true);
  }

  public BoatReservationListDialog(Frame parent, UUID boatId, boolean allowNewReservations,
      boolean allowNewReservationsWeekly, boolean allowEditDeleteReservations) {
    super(parent, International.getString("Bootsreservierungen"), Daten.project
        .getBoatReservations(false), 0, null);
    iniValues(boatId, allowNewReservations, allowNewReservationsWeekly, allowEditDeleteReservations);
  }

  public BoatReservationListDialog(JDialog parent, UUID boatId, boolean allowNewReservations,
      boolean allowNewReservationsWeekly, boolean allowEditDeleteReservations) {
    super(parent, International.getString("Bootsreservierungen"), Daten.project
        .getBoatReservations(false), 0, null);
    iniValues(boatId, allowNewReservations, allowNewReservationsWeekly, allowEditDeleteReservations);
  }

  private void iniValues(UUID boatId, boolean allowNewReservations,
      boolean allowNewReservationsWeekly, boolean allowEditDeleteReservations) {
    // Lieblingsbreite der Datumsspalten
    minColumnWidths = new int[] { 0, 115, 115, 0, 0, 0 };

    if (boatId != null) {
      this.filterFieldName = BoatReservationRecord.BOATID;
      this.filterFieldValue = boatId.toString();
      if (Daten.project != null) {
        Boats boats = Daten.project.getBoats(false);
        if (boats != null) {
          BoatRecord r = boats.getBoat(boatId, System.currentTimeMillis());
          if (r != null) {
            this.filterFieldDescription = International.getString("Boot") + ": " +
                r.getQualifiedName();
          }
        }
      }
    }
    if (allowNewReservations && allowEditDeleteReservations) {
      if (admin != null) {
        // default: ADD, EDIT, DELETE, IMPORT, EXPORT
      } else {
        actionText = new String[] {
            ItemTypeDataRecordTable.ACTIONTEXT_NEW,
            ItemTypeDataRecordTable.ACTIONTEXT_EDIT,
            ItemTypeDataRecordTable.ACTIONTEXT_DELETE
        };
        actionType = new int[] {
            ItemTypeDataRecordTable.ACTION_NEW,
            ItemTypeDataRecordTable.ACTION_EDIT,
            ItemTypeDataRecordTable.ACTION_DELETE
        };
      }
    } else if (allowNewReservations) {
      actionText = new String[] { ItemTypeDataRecordTable.ACTIONTEXT_NEW };
      actionType = new int[] { ItemTypeDataRecordTable.ACTION_NEW };
    } else if (allowEditDeleteReservations) {
      actionText = new String[] { ItemTypeDataRecordTable.ACTIONTEXT_EDIT,
          ItemTypeDataRecordTable.ACTIONTEXT_DELETE };
      actionType = new int[] { ItemTypeDataRecordTable.ACTION_EDIT,
          ItemTypeDataRecordTable.ACTION_DELETE };
    } else {
      actionText = new String[] {};
      actionType = new int[] {};
    }
    this.allowNewReservationsWeekly = allowNewReservationsWeekly;
  }

  @Override
  public void keyAction(ActionEvent evt) {
    _keyAction(evt);
  }

  @Override
  protected void iniDialog() throws Exception {
    sortByColumn = 1;
    super.iniDialog();
  }

  @Override
  public DataEditDialog createNewDataEditDialog(JDialog parent, StorageObject persistence,
      DataRecord record) {
    boolean newRecord = (record == null);
    if (record == null && persistence != null && filterFieldValue != null) {
      record = ((BoatReservations) persistence).createBoatReservationsRecord(UUID
          .fromString(filterFieldValue));
    }
    if (record == null) {
      long now = System.currentTimeMillis();
      ItemTypeStringAutoComplete boat = new ItemTypeStringAutoComplete("BOAT", "",
          IItemType.TYPE_PUBLIC,
          "", International.getString("Boot"), false);
      boat.setAutoCompleteData(new AutoCompleteList(Daten.project.getBoats(false).data(), now, now));
      if (SimpleInputDialog.showInputDialog(this, International.getString("Boot auswählen"), boat)) {
        String s = boat.toString();
        try {
          if (s != null && s.length() > 0) {
            Boats boats = Daten.project.getBoats(false);
            record = ((BoatReservations) persistence).createBoatReservationsRecord(boats.getBoat(s,
                now).getId());
          }
        } catch (Exception e) {
          Logger.logdebug(e);
        }
      }
    }
    if (record == null) {
      return null;
    }
    if (newRecord) {
      DataTypeDate dateFilter = table.getSelectedDateFilter();
      if (dateFilter == null) {
        dateFilter = DataTypeDate.today();
      }
      ((BoatReservationRecord) record).setDateFrom(dateFilter);
    }
    if (admin == null) {
      try {
        Boats boats = Daten.project.getBoats(false);
        BoatRecord b = boats.getBoat(((BoatReservationRecord) record).getBoatId(),
            System.currentTimeMillis());
        if (b.getOwner() != null && b.getOwner().length() > 0 &&
            !Daten.efaConfig.getValueMembersMayReservePrivateBoats()) {
          Dialog.error(International.getString("Privatboote dürfen nicht reserviert werden!"));
          return null;
        }
      } catch (Exception e) {
        Logger.logdebug(e);
        return null;
      }
    }

    try {
      return new BoatReservationEditDialog(parent, (BoatReservationRecord) record,
          newRecord, allowNewReservationsWeekly, admin);
    } catch (Exception e) {
      Dialog.error(e.getMessage());
      return null;
    }
  }

  @Override
  public boolean cancel() {
    new ICalendarExport().saveAllReservationToCalendarFile();
    saveBootshausReservierungenToCsvFile();

    return super.cancel();
  }

  private void saveBootshausReservierungenToCsvFile() {
    Vector<DataRecord> selection = getAlleBoothausReservierungen();
    DataExport export = new DataExport(persistence, -1 /* validAt */,
        selection, getWollesFieldNames(),
        DataExport.Format.csv, Daten.ENCODING_ISO,
        getFilenameCSV(), DataExport.EXPORT_TYPE_TEXT);
    export.runExport();
  }

  private String getFilenameCSV() {
    String dir = Daten.userHomeDir;
    String fname = dir
        + (Daten.fileSep != null && !dir.endsWith(Daten.fileSep) ? Daten.fileSep : "")
        + persistence.data().getStorageObjectName() + ".csv";
    return fname;
  }

  private String[] getWollesFieldNames() {
    List<String> retVal = new ArrayList<String>();
    String[] fields = persistence.createNewRecord().getFieldNamesForTextExport(false);
    for (String field : fields) {
      if (!field.equals(DataRecord.LASTMODIFIED) &&
          !field.equals(DataRecord.CHANGECOUNT) &&
          !field.equals(DataRecord.VALIDFROM) &&
          !field.equals(DataRecord.INVALIDFROM) &&
          !field.equals(DataRecord.INVISIBLE) &&
          !field.equals(DataRecord.DELETED) &&
          !field.equals("Id")) {
        retVal.add("" + field);
      }
    }
    return retVal.toArray(new String[retVal.size()]);
  }

  private Vector<DataRecord> getAlleBoothausReservierungen() {
    Vector<DataRecord> retVal = new Vector<DataRecord>();
    try {
      for (DataKey<?, ?, ?> k : persistence.data().getAllKeys()) {
        BoatReservationRecord r = (BoatReservationRecord) persistence.data().get(k);
        if (r.isBootshausOH()) {
          retVal.add(r);
        }
      }
    } catch (EfaException e) {
      // TODO Auto-generated catch block
      retVal = null;
    }
    return retVal;
  }
}
