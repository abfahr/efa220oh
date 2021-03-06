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
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.ex.EfaException;
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
      String reasonString = getItem("Reason").getValueFromField();
      String name = getItem("VirtualBoat").getValueFromField();
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
      if (!checkUndAktualisiereHandyNrInPersonProfil()) {
        return false;
      }
    }
    return super.saveRecord();
  }

  private boolean checkUndAktualisiereHandyNrInPersonProfil() {
    // Nutzer fragen, ob Handy-Nummer gespeichert werden soll
    if (!newRecord) {
      return true;
    }
    ItemTypeStringPhone phoneNr = (ItemTypeStringPhone) getItem(BoatReservationRecord.CONTACT);
    if (phoneNr == null || phoneNr.getValue().isBlank()) {
      return true;
    }
    String action = getTitle(); // "Reservierung"
    boolean booleanAlleMenschenZumVormerkenDerHandyNummerAuffordern = Daten.efaConfig
        .getValueEfaDirekt_AlleMenschenZumVormerkenDerHandyNummerAuffordern();
    ItemTypeStringAutoComplete cox = (ItemTypeStringAutoComplete) getItem(
        BoatReservationRecord.PERSONID);
    if (cox == null || !cox.isKnown()) {
      if (booleanAlleMenschenZumVormerkenDerHandyNummerAuffordern) {
        fragenUndLoggen(action, cox, phoneNr);
      }
      return true;
    }
    UUID personId = (UUID) cox.getId(cox.getValue());
    Persons persons = Daten.project.getPersons(false);
    PersonRecord person = persons.getPerson(personId, System.currentTimeMillis());
    if (person == null) {
      if (booleanAlleMenschenZumVormerkenDerHandyNummerAuffordern) {
        fragenUndLoggen(action, cox, phoneNr);
      }
      return true;
    }
    String antwort = person.checkUndAktualisiereHandyNr(phoneNr.getValue(),
        booleanAlleMenschenZumVormerkenDerHandyNummerAuffordern);
    if (antwort.contentEquals("noQuestion")) {
      return true; // Frage nicht mögich, also weiter
    }
    if (antwort.contentEquals("abbrechen")) { // ESC
      return false; // User wollte abbrechen, also STOP: stop saving loobook
    }
    if (!antwort.contains("saved")) {
      return false; // wrong answer!?! and change Phone in Dialog
    }

    // save entry
    String info = action + ": " + person.getFirstLastName();
    if (antwort.contentEquals("savedNew")) {
      info += " hat nun als TelefonNr '" + person.getHandy2() + "'";
    }
    if (antwort.contentEquals("savedOld")) {
      info += " hat weiterhin die TelefonNr '" + person.getHandy2() + "'";
    }
    if (antwort.contentEquals("savedEmpty")) {
      info += " hat nun kein Telefon " + person.getHandy2() + " " + person.getFestnetz1() + ",";
    }
    info += " und die Erlaubnis '" + person.isErlaubtTelefon() + "'";
    try {
      Logger.log(Logger.INFO, Logger.MSG_ABF_INFO, info);
      person.sendEmailConfirmation(person.getEmail(), "CONFIRM_SETPHONENR", info);
      persons.data().update(person);
      return true; // TelefonNr wurde aktualisiert, weiter mit Reservierung speichern
    } catch (EfaException e3) {
      String error = action + ": e3 " + e3.getLocalizedMessage();
      Logger.log(Logger.ERROR, Logger.MSG_ABF_ERROR, error);
      return true; // TelefonNr wurde aktualisiert, weiter mit Reservierung speichern
    }
  }

  private void fragenUndLoggen(String action,
      ItemTypeStringAutoComplete cox,
      ItemTypeStringPhone phoneNr) {
    String info = checkUndAktualisiereHandyNr(action, phoneNr.getValue());
    String coxName = (cox != null) ? cox.getValue() + " (unbekannt)" : "Ein unbekanntes Mitglied";
    info = coxName + " hätte vielleicht gerne " + phoneNr + " gespeichert: " + info;
    Logger.log(Logger.INFO, Logger.MSG_ABF_INFO, info);
  }

  public String checkUndAktualisiereHandyNr(String action, String newPhone) {
    // true = nur zugesagte Leute werden korrigiert.
    // false = alle Leute werden gefragt, Ausnahme zugesagte Nummer stimmt noch
    String telnumAusProfil = International.getString("keine Nummer bzw nix"); // keine bzw. nix

    // weder noch
    String frage = "Bevor es losgeht... eine Frage zu Deinen Benutzereinstellungen:\n";
    frage += "- Heutige Telefonnummer ist: " + newPhone + ",\n";
    frage += "- sonst übliche TelefonNr lautete: " + telnumAusProfil + ".\n";
    frage += "Falls Du Dich nur vertippt hast, drücke bitte die Taste ESC auf der Tastatur oben links.\n";
    frage += "\n";
    frage += "Darf sich EFA die neue Nummer merken? ";
    frage += "Soll EFA in Zukunft die neue Nummer vorschlagen?\n";
    frage += "alte Nummer                                   Drücke ESC für zurück                        neue Nummer\n";
    int antwort = Dialog.auswahlDialog("Zukünftige Vorbelegung der Telefonnummer", frage,
        newPhone + " vorschlagen", // 0 ja neue Nummer übernehmen
        "nix mehr vorschlagen", // 1 Erlaubnis entziehen
        telnumAusProfil + " vorschlagen"); // 2 = alte bisherige Nummer
    switch (antwort) {
      case 0: // neue Nummer zukünftig merken (rechts, default, selektiert)
        return "savedNew"; // muss noch gespeichert werden / persistiert
      case 1: // gar nix mehr vorschlagen
        return "savedEmpty"; // muss noch gespeichert werden / persistiert
      case 2: // alten Vorschlag beibehalten (links)
        return "savedEmpty"; // muss noch gespeichert werden / persistiert
      case 3: // hier könnte ein Button "abbrechen" rein...
        return "abbrechen"; // = nix tun
      case -1: // abbrechen = cancel = ESC = x // zurück, nochmal die Nummer ändern
        return "abbrechen"; // = nix tun
      default: // unbekannt
        return "abbrechen"; // = nix tun
    }
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
          dateTo.setSelection(0, 10);
        }
        if (it.getName().equals(BoatReservationRecord.TIMEFROM)) {
          if (dateFrom.getDate().equals(DataTypeDate.today())) {
            ItemTypeTime timeFrom = (ItemTypeTime) it;
            DataTypeTime now = DataTypeTime.now();
            now.setSecond(0);
            now.add((5 - now.getMinute() % 5) * 60);
            timeFrom.parseValue(now.toString());
            timeFrom.showValue();
            timeFrom.setSelection(0, 5);
          }
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
          timeTo.setSelection(0, 5);
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
            dateTo.setSelection(0, 10);
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
          reason.setSelection(0, 999);
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
          reason.setSelection(0, 999);
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
          reason.setSelection(0, 999);
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
    } else {
      Logger.log(Logger.INFO, Logger.MSG_DEBUG_AUTOCOMPLETE,
          "Formular: TelNum für " + item.getValueFromField() + " automatisch eingetragen. "
              + person.isErlaubtTelefon());
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
          phoneContactGuiField.showValue();
          phoneContactGuiField.setSelection(0, 30);
        }
      }
      if (it.getName().equals(BoatReservationRecord.REASON)) {
        ItemTypeString reasonGuiField = (ItemTypeString) it;
        if (reasonGuiField.getValueFromField().isEmpty()
            && getDataRecord().isBootshausOH()) {
          reasonGuiField.setValue(bestReason);
          reasonGuiField.setValue(latestReason);
          reasonGuiField.showValue();
          reasonGuiField.setSelection(0, 999);
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
