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

import java.awt.AWTEvent;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.swing.JDialog;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.IItemListener;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeDate;
import de.nmichael.efa.core.items.ItemTypeRadioButtons;
import de.nmichael.efa.core.items.ItemTypeString;
import de.nmichael.efa.core.items.ItemTypeStringAutoComplete;
import de.nmichael.efa.core.items.ItemTypeTime;
import de.nmichael.efa.data.BoatRecord;
import de.nmichael.efa.data.BoatReservationRecord;
import de.nmichael.efa.data.BoatReservations;
import de.nmichael.efa.data.PersonRecord;
import de.nmichael.efa.data.Persons;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;

// @i18n complete
public class BoatReservationEditDialog extends UnversionizedDataEditDialog implements IItemListener {

  private static final long serialVersionUID = 1L;

  public BoatReservationEditDialog(Frame parent, BoatReservationRecord r,
      boolean newRecord, boolean allowWeeklyReservation, AdminRecord admin) throws Exception {
    super(parent, International.getString("Reservierung"), r, newRecord, admin);
    initListener();
    setAllowWeeklyReservation(allowWeeklyReservation);
  }

  public BoatReservationEditDialog(JDialog parent, BoatReservationRecord r,
      boolean newRecord, boolean allowWeeklyReservation, AdminRecord admin) throws Exception {
    super(parent, International.getString("Reservierung"), r, newRecord, admin);
    initListener();
    setAllowWeeklyReservation(allowWeeklyReservation);
  }

  @Override
  public void keyAction(ActionEvent evt) {
    _keyAction(evt);
  }

  private void initListener() {
    IItemType itemType = null;
    for (IItemType item : allGuiItems) {
      if (item.getName().equals(BoatReservationRecord.TYPE)) {
        ((ItemTypeRadioButtons) item).registerItemListener(this);
        itemType = item;
      }
      if (item.getName().equals(BoatReservationRecord.DATEFROM)) {
        item.registerItemListener(this);
      }
      if (item.getName().equals(BoatReservationRecord.TIMEFROM)) {
        item.registerItemListener(this);
      }
      if (item.getName().equals(BoatReservationRecord.PERSONID)) {
        item.registerItemListener(this);
      }
      if (item.getName().equals(BoatReservationRecord.CONTACT)) {
        item.registerItemListener(this);
      }
    }
    itemListenerAction(itemType, null);
  }

  @Override
  public void itemListenerAction(IItemType item, AWTEvent event) {
    if (item != null && item.getName().equals(BoatReservationRecord.TYPE)) {
      String type = item.getValueFromField();
      if (type == null) {
        return;
      }
      for (IItemType it : allGuiItems) {
        if (it.getName().equals(BoatReservationRecord.DAYOFWEEK)) {
          it.setVisible(type.equals(BoatReservationRecord.TYPE_WEEKLY));
        }
        if (it.getName().equals(BoatReservationRecord.DATEFROM)) {
          it.setVisible(type.equals(BoatReservationRecord.TYPE_ONETIME));
        }
        if (it.getName().equals(BoatReservationRecord.DATETO)) {
          it.setVisible(type.equals(BoatReservationRecord.TYPE_ONETIME));
        }
      }
    }

    // Das Datum übernehmen
    if (item != null && item.getName().equals(BoatReservationRecord.DATEFROM) &&
        ((event instanceof FocusEvent && event.getID() == FocusEvent.FOCUS_LOST) ||
            (event instanceof KeyEvent && ((KeyEvent) event).getKeyChar() == '\n'))) {
      ItemTypeDate dateFrom = (ItemTypeDate) item;
      for (IItemType it : allGuiItems) {
        if (it.getName().equals(BoatReservationRecord.DATETO)) {
          ItemTypeDate dateTo = (ItemTypeDate) it;
          if (dateTo.getDate().isBefore(dateFrom.getDate())) {
            dateTo.setValueDate(dateFrom.getDate());
          }
          dateTo.showValue();
          break;
        }
      }
    }

    // Die Uhrzeit übernehmen und 2 Stunden dazuzählen
    if (item != null && item.getName().equals(BoatReservationRecord.TIMEFROM) &&
        ((event instanceof FocusEvent && event.getID() == FocusEvent.FOCUS_LOST)
            || (event instanceof KeyEvent && ((KeyEvent) event).getKeyChar() == '\n'))) {
      ItemTypeTime timeFrom = (ItemTypeTime) item;
      for (IItemType it : allGuiItems) {
        if (it.getName().equals(BoatReservationRecord.TIMETO)) {
          ItemTypeTime timeTo = (ItemTypeTime) it;
          if (!timeTo.isValidInput() || timeTo.getTime().isBefore(timeFrom.getTime())) {
            DataTypeTime newEndtime = timeFrom.getTime();
            newEndtime.add(120 * 60); // plus 2h = 119 Minuten
            timeTo.parseValue(newEndtime.toString(false)); // ohne Sekunden
          }
          timeTo.showValue();
          break;
        }
      }
    }

    // Name des Mitglieds
    if (item != null && item.getName().equals(BoatReservationRecord.PERSONID) &&
        ((event instanceof FocusEvent && event.getID() == FocusEvent.FOCUS_LOST) ||
            (event instanceof KeyEvent && ((KeyEvent) event).getKeyChar() == '\n'))) {

      // Prüfung Name zu kurz?
      ItemTypeString eingegebenerName = (ItemTypeString) item;
      for (IItemType it : allGuiItems) {
        if (it.getName().equals(BoatReservationRecord.REASON)) {
          ItemTypeString reason = (ItemTypeString) it;
          String prangerText = Daten.efaConfig.getTextBadMitgliedsname();
          String reasonString = reason.getValue().replace(prangerText, "").trim();
          String myMatch = Daten.efaConfig.getRegexForVorUndNachname();
          if (!eingegebenerName.getValue().matches(myMatch)) {
            reasonString = prangerText + " " + reasonString;
          }
          reason.setValue(reasonString);
          reason.showValue();
          break;
        }
      }

      try {
        if (Daten.efaConfig.getValueEfaDirekt_AlteReservierungDurchsuchen()) {
          // Hier Telefonnummer aus alter Reservierung übernehmen
          findAnyPreviousReservation((ItemTypeStringAutoComplete) item);
        }
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    // Telefonnummer (Handy)
    if (item != null && item.getName().equals(BoatReservationRecord.CONTACT) &&
        ((event instanceof FocusEvent && event.getID() == FocusEvent.FOCUS_LOST) ||
            (event instanceof KeyEvent && ((KeyEvent) event).getKeyChar() == '\n'))) {
      ItemTypeString eingegebeneHandynummer = (ItemTypeString) item;
      for (IItemType it : allGuiItems) {
        if (it.getName().equals(BoatReservationRecord.REASON)) {
          ItemTypeString reason = (ItemTypeString) it;
          String prangerText = Daten.efaConfig.getTextBadHandynummer();
          prangerText = "Vorwahl kenntlich machen!";
          String reasonString = reason.getValue().replace(prangerText, "").trim();
          String myMatch = Daten.efaConfig.getRegexForHandynummer();
          // myMatch = "0\\d{2,5}-\\d{3,}";
          // myMatch = "0\\d*[-\\.\\s]\\d*";
          // myMatch = "0[0-9]{1,5}.[0-9]{4,14}(?:x.+)?";
          // myMatch = ".*"; // alles erlaubt
          // myMatch = "0[1-9][0-9]*[\\.-/ ][0-9]*"; // geht best original - /)+
          // myMatch = "\\(?0[1-9][0-9]*[\\.\\-\\+\\_\\)/ ] *[0-9 ]*"; // standard
          if (!eingegebeneHandynummer.getValue().matches(myMatch)) {
            reasonString = prangerText + " " + reasonString;
          }
          reason.setValue(reasonString);
          reason.showValue();
          break;
        }
      }

      // TODO Hier prüfen, ob Termin-Überlapp
    }
  }

  private void findAnyPreviousReservation(ItemTypeStringAutoComplete item) {
    boolean isMostStattLatest = Daten.efaConfig.getValueEfaDirekt_FindenNachHaeufigsterStattNeuesterReservierung();

    UUID personId = (UUID) item.getAutoCompleteData().getId(item.getValueFromField());
    if (personId == null) {
      return;
    }
    Persons persons = Daten.project.getPersons(false);
    PersonRecord person = persons.getPerson(personId, System.currentTimeMillis());
    if (person.getInputShortcut().isEmpty()) {
      return;
    }
    BoatReservations boatReservations = Daten.project.getBoatReservations(false);
    BoatReservationRecord[] oldReservations = boatReservations.getBoatReservationsByPerson(personId);
    if (oldReservations == null) {
      return;
    }
    BoatReservationRecord latestReservation = oldReservations[0];
    List<String> listTelnums = new ArrayList<String>();
    List<String> listReasons = new ArrayList<String>();
    long latestModified = 0L;
    String bestTelnum = "";
    String bestReason = "";
    for (BoatReservationRecord boatReservationRecord : oldReservations) {
      listTelnums.add(boatReservationRecord.getContact());
      listReasons.add(boatReservationRecord.getReason());
      if (boatReservationRecord.getLastModified() > latestModified) {
        latestModified = boatReservationRecord.getLastModified();
        latestReservation = boatReservationRecord;
        bestTelnum = boatReservationRecord.getContact();
        bestReason = boatReservationRecord.getReason();
      }
    }
    if (isMostStattLatest) {      
      Map<String, Long> occurrencesTelnums = listTelnums.stream().collect(
          Collectors.groupingBy(w -> w, Collectors.counting()));
      Map<String, Long> occurrencesReasons = listReasons.stream().collect(
          Collectors.groupingBy(w -> w, Collectors.counting()));
      
      Long previous = 0L;
      for (String telnum : occurrencesTelnums.keySet()) {
        if (occurrencesTelnums.get(telnum) > previous) {
          previous = occurrencesTelnums.get(telnum);
          bestTelnum = telnum;
        }
      }
      previous = 0L;
      for (String reason : occurrencesReasons.keySet()) {
        if (occurrencesReasons.get(reason) > previous) {
          previous = occurrencesReasons.get(reason);
          bestReason = reason;
        }
      }
    }

    for (IItemType it : allGuiItems) {
      if (it.getName().equals(BoatReservationRecord.CONTACT)) {
        ItemTypeString phoneContactGuiField = (ItemTypeString) it;
        if (phoneContactGuiField.getValueFromField().isEmpty()) {
          phoneContactGuiField.setValue(bestTelnum);
        }
      }
      if (it.getName().equals(BoatReservationRecord.REASON)) {
        ItemTypeString reasonGuiField = (ItemTypeString) it;
        if (reasonGuiField.getValueFromField().isEmpty()) {
          reasonGuiField.setValue(bestReason);
          reasonGuiField.setValue(latestReservation.getReason());
        }
      }
    }
  }

  boolean isReservationForBoatDuringFreetime() {
    getValuesFromGui();

    String boatName = ""; // getItem("BoatName").getValueFromField();
    UUID boatId = BoatRecord.BOOTSHAUS;// getItem("BoatId").getValueFromField();
    long startZeit = System.currentTimeMillis(); // abf
    int dauerMinuten = Daten.efaConfig.getValueEfaDirekt_resLookAheadTime();

    BoatReservationRecord[] reservations = null;
    if (boatId != null) {
      BoatReservations boatReservations = Daten.project.getBoatReservations(false);
      reservations = boatReservations.getBoatReservations(boatId, startZeit, dauerMinuten);
    }

    if (reservations != null && reservations.length > 0) {
      long validInMinutes = reservations[0].getReservationValidInMinutes(startZeit, dauerMinuten);
      if (Dialog
          .yesNoCancelDialog(
              International.getString("Boot reserviert"),
              International.getMessage(
                  "Das Boot {boat} ist {currently_or_in_x_minutes} für {name} reserviert.",
                  boatName,
                  (validInMinutes == 0
                  ? International.getString("zur Zeit")
                      : International.getMessage("in {x} Minuten", (int) validInMinutes)),
                      reservations[0].getPersonAsName())
                      + "\n"
                      + (reservations[0].getReason() != null
                      && reservations[0].getReason().length() > 0 ?
                          International.getString("Grund") + ": " + reservations[0].getReason() + "\n"
                          : "")
                          + (reservations[0].getContact() != null
                          && reservations[0].getContact().length() > 0 ?
                              International.getString("Telefon für Rückfragen") + ": "
                              + reservations[0].getContact() + "\n" : "")
                              + "\n"
                              + International.getMessage("Die Reservierung liegt {from_time_to_time} vor.",
                                  reservations[0].getReservationTimeDescription()) + "\n"
                                  + International.getString("Möchtest Du trotzdem reservieren?"))
                                  != Dialog.YES) {
        return false;
      }
    }
    return true;
  }

  private void setAllowWeeklyReservation(boolean allowWeeklyReservation) throws Exception {
    if (!allowWeeklyReservation) {
      if (!newRecord && dataRecord != null &&
          BoatReservationRecord.TYPE_WEEKLY.equals(((BoatReservationRecord) dataRecord).getType())) {
        throw new Exception(
            International.getString("Diese Reservierung kann nicht bearbeitet werden."));
      }
      for (IItemType it : allGuiItems) {
        if (it.getName().equals(BoatReservationRecord.TYPE)) {
          it.parseAndShowValue(BoatReservationRecord.TYPE_ONETIME);
          it.setVisible(false);
          it.setEditable(false);
          itemListenerAction(it, null);
          continue;
        }
        if (it.getName().equals(BoatReservationRecord.DAYOFWEEK)) {
          // sonst verhindert ein Dirty das Abbrechen:
          it.parseValue("SUNDAY");
          it.setUnchanged();
        }

      }
    }
  }

  public BoatReservationRecord getDataRecord() {
    return (BoatReservationRecord) dataRecord;
  }

}
