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

  public BoatReservationListDialog(Frame parent, UUID boatId, boolean allowNewReservations,
      boolean allowNewReservationsWeekly, boolean allowEditDeleteReservations) {
    super(parent, TITEL_BOOTRESERVIERUNGEN, Daten.project
        .getBoatReservations(false), 0, null);
    iniValues(boatId, allowNewReservations, allowNewReservationsWeekly,
        allowEditDeleteReservations);
  }

  private void iniValues(UUID boatId, boolean allowNewReservations,
      boolean allowNewReservationsWeekly, boolean allowEditDeleteReservations) {
    // Lieblingsbreite der Datumsspalten
    minColumnWidths = new int[] { 200, 200, 200, 200, 180, 0 }; // abf

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
      boat.setAutoCompleteData(
          new AutoCompleteList(Daten.project.getBoats(false).data(), now, now));
      if (SimpleInputDialog.showInputDialog(this, International.getString("Boot auswählen"),
          boat)) {
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

    if (newRecord) {
      if (!checkEinweisung(((BoatReservationRecord) record).getBoat())) {
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

  private boolean checkEinweisung(BoatRecord boatRecord) {
    if (boatRecord != null && boatRecord.getTypeSeats(0).equals(
            International.getString("Profi Boote Kontrollnummer"))) {
      int yesNoDialog = Dialog.yesNoDialog(
              International.getString("Titel Einweisung Profi Boote"),
              "  " + boatRecord.getQualifiedName() + "\n\r"
                      + International.getString("Frage Einweisung Profi Boote") + "\n\r  "
                      + Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS,
                      boatRecord.getTypeSeats(0)));
      if (yesNoDialog != Dialog.YES) {
        Dialog.infoDialog(Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS,
                        boatRecord.getTypeSeats(0)),
                International.getString("Termin Einweisung Profi Boote"));
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean deleteCallback(DataRecord[] records) {
    for (DataRecord dataRecord : records) {
      BoatReservationRecord boatReservationRecord = (BoatReservationRecord) dataRecord;
      if (boatReservationRecord.getType().equals(BoatReservationRecord.TYPE_WEEKLY)) {
        return nachfragenWeekly(); // leider ist ein WEEKLY dabei.
      }
    }
    return super.deleteCallback(records);
  }

  private boolean nachfragenWeekly() {
    int antwortAuswahlDialog = Dialog.auswahlDialog(
        International.getString("Wöchentliche Termine löschen"),
        International.getString("Möchtest du wöchentliche Termine wirklich löschen?\n"
            + "Diese festen Termine wurden von Fachwarten angelegt."),
        International.getString("nein, ich trau mich nicht"),
        International.getString("ja, ich darf das - bin Fachwart"));
    switch (antwortAuswahlDialog) {
      case 0: // nein // nein, nicht löschen
        return false; // nein, nicht löschen
      case 1: // ja, // ja, bitte Termine löschen
        return true; // ja, bitte Termine löschen
      default:
        return false; // "Abbruch"
    }
  }

  public String getFilterFieldDescription() {
    // für zweite Anzeige im Dialog - just to be sure
    return filterFieldDescription;
  }
}
