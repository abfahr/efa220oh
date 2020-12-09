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
import de.nmichael.efa.core.items.ItemTypeStringPhone;
import de.nmichael.efa.core.items.ItemTypeTime;
import de.nmichael.efa.data.BoatRecord;
import de.nmichael.efa.data.BoatReservationRecord;
import de.nmichael.efa.data.BoatReservations;
import de.nmichael.efa.data.PersonRecord;
import de.nmichael.efa.data.Persons;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.ex.InvalidValueException;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

// @i18n complete
public class BoatReservationEditDialog extends UnversionizedDataEditDialog
    implements IItemListener {

  private static final long serialVersionUID = 1L;

  public BoatReservationEditDialog(JDialog parent, BoatReservationRecord r,
      boolean newRecord, boolean allowWeeklyReservation, AdminRecord admin) throws Exception {
    super(parent, International.getString("Reservierung"), r, newRecord, admin);
    initListener();
    setAllowWeeklyReservation(allowWeeklyReservation);
    if (!r.isBootshausOH() && admin == null) {
      enableReason(false);
    }
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
      if (item.getName().equals(BoatReservationRecord.DATETO)) {
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

  private void enableReason(boolean enable) {
    for (IItemType item : allGuiItems) {
      if (item.getName().equals(BoatReservationRecord.REASON)) {
        item.setEditable(enable);
        item.setEnabled(enable);
      }
    }
  }

  @Override
  protected boolean saveRecord() throws InvalidValueException {
    if (newRecord) {
      String errorText = "";
      String reasonString = this.getItem("Reason").getValueFromField();
      String name = this.getItem("VirtualBoat").getValueFromField();
      if (name == BoatRecord.BOOTSHAUS_NAME && reasonString == null) {
        errorText = "kein Reservierungsgrund angegeben??";
      }
      if (name == BoatRecord.BOOTSHAUS_NAME && reasonString.trim().isEmpty()) {
        errorText = "Bitte Reservierungsgrund angegeben,\n"
            + "damit andere Beneidisch wissen!";
      }
      String templatePrivatMitVertrag = International.getString("Fehlermeldung PrivatMitVertrag");
      if (reasonString.contains(templatePrivatMitVertrag)) {
        errorText = "Bitte eigenen Reservierungsgrund angeben:\n" + templatePrivatMitVertrag;
      }
      String templatePrangerText = International.getString("Fehlermeldung bei langerAusleihe");
      if (reasonString.contains(templatePrangerText)) {
        errorText = "Bitte Absprache statt Frage angeben:\n" + templatePrangerText;
      }
      if (reasonString.equals(templatePrangerText)) {
        errorText = "Bitte Absprache angeben.\n" + templatePrangerText;
      }

      if (!errorText.isEmpty()) {
        enableReason(true);
        Dialog.error(errorText);
        return false;
      }
    }
    return super.saveRecord();
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
          it.setVisible(true);
        }
        if (it.getName().equals(BoatReservationRecord.DATETO)) {
          it.setVisible(type.equals(BoatReservationRecord.TYPE_ONETIME));
          it.setVisible(true);
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

    // Datum erst zwei Tage später?
    if (item != null && item.getName().equals(BoatReservationRecord.DATETO) &&
        ((event instanceof FocusEvent && event.getID() == FocusEvent.FOCUS_LOST)
            || (event instanceof KeyEvent && ((KeyEvent) event).getKeyChar() == '\n'))) {
      ItemTypeDate dateTo = (ItemTypeDate) item;
      double anzahlStunden = 0;
      for (IItemType it : allGuiItems) {
        if (it.getName().equals(BoatReservationRecord.DATEFROM)) {
          ItemTypeDate dateFrom = (ItemTypeDate) it;
          if (admin == null && !dateTo.isSet()) {
            dateTo.setValueDate(dateFrom.getDate());
            dateTo.showValue();
          }
          anzahlStunden = dateTo.getDate().getDifferenceDays(dateFrom.getDate()) * 24;
          break;
        }
      }
      double minimumDauerFuerKulanz = Daten.efaConfig.getMinimumDauerFuerKulanz();
      for (IItemType it : allGuiItems) {
        if (it.getName().equals(BoatReservationRecord.REASON)) {
          ItemTypeString reason = (ItemTypeString) it;
          String prangerText = International.getString("Fehlermeldung bei langerAusleihe");
          String reasonString = reason.getValue().replace(prangerText, "").trim();
          if (admin == null && anzahlStunden >= minimumDauerFuerKulanz) {
            reasonString = prangerText + " " + reasonString;
            enableReason(true);
          }
          reason.setValue(reasonString);
          reason.showValue();
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
          String prangerText = International.getString("Fehlermeldung bei BadMitgliedsname");
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
        Logger.log(Logger.WARNING, Logger.MSG_ERR_UNEXPECTED, e);
      }
    }

    // Telefonnummer (Handy)
    if (item != null && item.getName().equals(BoatReservationRecord.CONTACT) &&
        ((event instanceof FocusEvent && event.getID() == FocusEvent.FOCUS_LOST) ||
            (event instanceof KeyEvent && ((KeyEvent) event).getKeyChar() == '\n'))) {
      ItemTypeStringPhone eingegebeneHandynummer = (ItemTypeStringPhone) item;
      for (IItemType it : allGuiItems) {
        if (it.getName().equals(BoatReservationRecord.REASON)) {
          ItemTypeString reason = (ItemTypeString) it;
          String prangerText = International.getString("Fehlermeldung bei BadHandynummer");
          prangerText = "Vorwahl kenntlich machen!";
          String reasonString = reason.getValue().replace(prangerText, "").trim();
          String myMatch = Daten.efaConfig.getRegexForHandynummer();
          if (!eingegebeneHandynummer.getValue().matches(myMatch)) {
            reasonString = prangerText + " " + reasonString;
          }
          reason.setValue(reasonString);
          reason.showValue();
          break;
        }
      }
    }
  }

  private void findAnyPreviousReservation(ItemTypeStringAutoComplete item) {
    boolean isMostStattLatest = Daten.efaConfig
        .getValueEfaDirekt_FindenNachHaeufigsterStattNeuesterReservierung();

    UUID personId = (UUID) item.getAutoCompleteData().getId(item.getValueFromField());
    if (personId == null) {
      return;
    }
    Persons persons = Daten.project.getPersons(false);
    PersonRecord person = persons.getPerson(personId, System.currentTimeMillis());
    if (!person.isErlaubtTelefon()) {
      return;
    }
    String bestTelnum = "";
    String bestReason = "";
    String latestReason = "";
    bestTelnum = person.getHandy2();
    if (bestTelnum == null || bestTelnum.length() == 0) {
      bestTelnum = person.getFestnetz1();
    }
    if (bestTelnum == null || bestTelnum.length() == 0) {
      bestTelnum = "";
    }
    BoatReservations boatReservations = Daten.project.getBoatReservations(false);
    BoatReservationRecord[] oldReservations = boatReservations
        .getBoatReservationsByPerson(personId);
    if (oldReservations != null) {
      BoatReservationRecord latestReservation = oldReservations[0];
      List<String> listTelnums = new ArrayList<String>();
      List<String> listReasons = new ArrayList<String>();
      long latestModified = 0L;
      for (BoatReservationRecord boatReservationRecord : oldReservations) {
        listTelnums.add(boatReservationRecord.getContact());
        if (boatReservationRecord.getReason().trim().length() > 0) {
          listReasons.add(boatReservationRecord.getReason());
        }
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
      latestReason = latestReservation.getReason();
    }

    for (IItemType it : allGuiItems) {
      if (it.getName().equals(BoatReservationRecord.CONTACT)) {
        ItemTypeStringPhone phoneContactGuiField = (ItemTypeStringPhone) it;
        if (phoneContactGuiField.getValueFromField().isEmpty()) {
          phoneContactGuiField.setValue(bestTelnum);
        }
      }
      if (it.getName().equals(BoatReservationRecord.REASON)) {
        ItemTypeString reasonGuiField = (ItemTypeString) it;
        if (reasonGuiField.getValueFromField().isEmpty()
            && getDataRecord().isBootshausOH()) {
          reasonGuiField.setValue(bestReason);
          reasonGuiField.setValue(latestReason);
        }
      }
    }
  }

  private void setAllowWeeklyReservation(boolean allowWeeklyReservation) throws Exception {
    if (allowWeeklyReservation) {
      return;
    }
    if (!newRecord && dataRecord != null && BoatReservationRecord.TYPE_WEEKLY
        .equals(((BoatReservationRecord) dataRecord).getType())) {
      throw new Exception(
          International.getString("Diese Reservierung kann nicht bearbeitet werden.")
              + "\n" + ((BoatReservationRecord) dataRecord).getType());
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

  public BoatReservationRecord getDataRecord() {
    return (BoatReservationRecord) dataRecord;
  }

}
