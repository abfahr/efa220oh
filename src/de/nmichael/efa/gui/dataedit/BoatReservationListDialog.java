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
import de.nmichael.efa.calendar.ICalendarExport;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeDataRecordTable;
import de.nmichael.efa.core.items.ItemTypeStringAutoComplete;
import de.nmichael.efa.data.BoatRecord;
import de.nmichael.efa.data.BoatReservationRecord;
import de.nmichael.efa.data.BoatReservations;
import de.nmichael.efa.data.Boats;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.gui.SimpleInputDialog;
import de.nmichael.efa.gui.util.AutoCompleteList;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

// @i18n complete
public class BoatReservationListDialog extends DataListDialog {

  private static final String TITEL_BOOTRESERVIERUNGEN = International.getString("Reservierungen");
  private static final long serialVersionUID = 1L;
  boolean allowNewReservationsWeekly = true;

  public BoatReservationListDialog(Frame parent, AdminRecord admin) {
    super(parent, TITEL_BOOTRESERVIERUNGEN, Daten.project
        .getBoatReservations(false), 0, admin);
    iniValues(null, true, true, true);
  }

  public BoatReservationListDialog(JDialog parent, AdminRecord admin) {
    super(parent, TITEL_BOOTRESERVIERUNGEN, Daten.project
        .getBoatReservations(false), 0, admin);
    iniValues(null, true, true, true);
  }

  public BoatReservationListDialog(Frame parent, UUID boatId, AdminRecord admin) {
    super(parent, TITEL_BOOTRESERVIERUNGEN, Daten.project
        .getBoatReservations(false), 0, admin);
    iniValues(boatId, true, true, true);
  }

  public BoatReservationListDialog(JDialog parent, UUID boatId, AdminRecord admin) {
    super(parent, TITEL_BOOTRESERVIERUNGEN, Daten.project
        .getBoatReservations(false), 0, admin);
    iniValues(boatId, true, true, true);
  }

  public BoatReservationListDialog(Frame parent, UUID boatId, boolean allowNewReservations,
      boolean allowNewReservationsWeekly, boolean allowEditDeleteReservations) {
    super(parent, TITEL_BOOTRESERVIERUNGEN, Daten.project
        .getBoatReservations(false), 0, null);
    iniValues(boatId, allowNewReservations, allowNewReservationsWeekly, allowEditDeleteReservations);
  }

  public BoatReservationListDialog(JDialog parent, UUID boatId, boolean allowNewReservations,
      boolean allowNewReservationsWeekly, boolean allowEditDeleteReservations) {
    super(parent, TITEL_BOOTRESERVIERUNGEN, Daten.project
        .getBoatReservations(false), 0, null);
    iniValues(boatId, allowNewReservations, allowNewReservationsWeekly, allowEditDeleteReservations);
  }

  private void iniValues(UUID boatId, boolean allowNewReservations,
      boolean allowNewReservationsWeekly, boolean allowEditDeleteReservations) {
    // Lieblingsbreite der Datumsspalten
    minColumnWidths = new int[] { 200, 135, 135, 200, 135, 0 }; // abf

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
    if (Daten.efaConfig.isSaveAllReservationToCalendarFile()) {
      new ICalendarExport().saveAllReservationToCalendarFile();
    }
    return super.cancel();
  }

}
