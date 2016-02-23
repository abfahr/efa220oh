
package de.nmichael.efa.calendar;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.WeekDay;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.model.property.XProperty;
import de.nmichael.efa.Daten;
import de.nmichael.efa.data.BoatReservationRecord;
import de.nmichael.efa.data.BoatReservations;
import de.nmichael.efa.data.Clubwork;
import de.nmichael.efa.data.ClubworkRecord;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.ex.EfaException;

public class ICalendarExport {

  public static final String CRLF = net.fortuna.ical4j.util.Strings.LINE_SEPARATOR; // "\r\n"
  private static final String EFA = "efa";
  private static final String ABFX_DE = "@abfx.de";

  public void saveAllReservationToCalendarFile() {
    try {
      saveAllReservationToCalendarFileIntern();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ValidationException e) {
      e.printStackTrace();
    } catch (ParserException e) {
      e.printStackTrace();
    } catch (EfaException e) {
      e.printStackTrace();
    } catch (ParseException e) {
      e.printStackTrace();
    }
  }

  public void saveAllClubworkToCalendarFile() {
    try {
      saveAllClubworkToCalendarFileIntern();
    } catch (EfaException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ValidationException e) {
      e.printStackTrace();
    }
  }

  private void saveAllReservationToCalendarFileIntern() throws IOException, ValidationException,
      ParserException, EfaException, ParseException {

    // [x] Bootshaus (nur das vertragspflichtige Haus)
    // [x] Boote (alles ohne Bootshaus)
    // [x] Regeltermine (viele Boote an Mo/Do-Terminen)
    // [x] alle Boote eines Regeltermins zusammenfassen?
    // [ ] mit Klarnamen
    // [ ] mit Telefonnummer
    // [ ] mit Anlass/Infotext
    boolean saveBootshaus = true;
    boolean saveBoote = true;
    boolean saveWeekly = true;
    boolean saveWeeklyAsSingleEvent = true;
    boolean saveName = false;
    boolean savePhone = false;
    boolean saveInfo = false;

    // Creating a new calendar
    net.fortuna.ical4j.model.Calendar calendar = new net.fortuna.ical4j.model.Calendar();
    ArrayList<String> wochentermine = new ArrayList<String>();

    BoatReservations boatReservations = Daten.project.getBoatReservations(false);
    for (DataKey<?, ?, ?> dataKey : boatReservations.data().getAllKeys()) {
      DataRecord dataRecord = boatReservations.data().get(dataKey);
      BoatReservationRecord boatReservationRecord = (BoatReservationRecord) dataRecord;
      if (boatReservationRecord.getDeleted()) {
        continue;
      }
      String type = boatReservationRecord.getType();
      String dayOfWeek = boatReservationRecord.getDayOfWeek();
      String boatName = boatReservationRecord.getBoatName();
      boolean isBootshausReservierung = boatReservationRecord.isBootshausOH();
      DataTypeDate dateFrom = boatReservationRecord.getDateFrom();
      DataTypeTime timeFrom = boatReservationRecord.getTimeFrom();
      DataTypeDate dateTo = boatReservationRecord.getDateTo();
      DataTypeTime timeTo = boatReservationRecord.getTimeTo();
      String contactPhone = boatReservationRecord.getContact();
      String personAsName = boatReservationRecord.getPersonAsName();
      String reason = boatReservationRecord.getReason();
      String reservationTimeDescription = boatReservationRecord.getReservationTimeDescription();
      long lastModified = boatReservationRecord.getLastModified();
      String dateTimeLastModifiedStr = new DateTime(lastModified).toString().replace('T', '.');
      int reservationOrder = boatReservationRecord.getReservation();
      String efaId = EFA + reservationOrder + personAsName.substring(0, 1).toUpperCase();
      String uid = dateTimeLastModifiedStr + "." + efaId + ABFX_DE;
      String modif = "(" + efaId + " aktualisiert am " + dateTimeLastModifiedStr + ")";

      String description = reservationTimeDescription + CRLF;
      if (saveInfo && !"k.A.".equals(reason)) {
        description += "Anlass: " + reason + CRLF;
      }
      if (saveName) {
        description += "Mitglied: " + personAsName + CRLF;
      }
      if (savePhone && !"".equals(contactPhone)) {
        description += "Fon: " + contactPhone + CRLF;
      }
      description += modif;

      if (BoatReservationRecord.TYPE_WEEKLY.equals(type)) {
        if (!saveWeekly) {
          continue;
        }
        if (dateFrom == null) {
          dateFrom = new DataTypeDate(lastModified);
        }
        if (dateFrom.equals(DataTypeDate.today())) {
          dateFrom = new DataTypeDate(lastModified);
        }
        if (dateTo == null) {
          dateTo = dateFrom;
        }
      }
      DateTime startDateTime = new DateTime(dateFrom.getTimestamp(timeFrom));
      DateTime endDateTime = new DateTime(dateTo.getTimestamp(timeTo));

      // Creating an event
      String eventSummary = boatName + " - " + (saveName ? personAsName : efaId);
      VEvent termin = new VEvent(startDateTime, endDateTime, eventSummary);
      termin.getProperties().add(new Description(description));
      termin.getProperties().add(new Location("Isekai 10 Hamburg"));
      termin.getProperties().add(new Uid(uid));

      if (BoatReservationRecord.TYPE_WEEKLY.equals(type)) {
        if (saveWeeklyAsSingleEvent && !isBootshausReservierung) {
          if (wochentermine.contains(reservationTimeDescription)) {
            continue;
          }
          wochentermine.add(reservationTimeDescription);
          termin.getSummary().setValue("OH-Regeltermin");
          // TODO fahr 2016-02-01 Hier noch den Text erweitern:
          // keine Ausleihe möglich
          // nur nach Rücksprache mit den Fachwarten
          // Es gibt keine Boote in der Zeit

        }
        // String recur3 = "RRULE:FREQ=" + type + ";BYDAY=" + dayOfWeek.substring(0, 2);
        Recur recur = new Recur();
        recur.setFrequency(Recur.WEEKLY);
        recur.getDayList().add(new WeekDay(dayOfWeek.substring(0, 2)));
        termin.getProperties().add(new RRule(recur));
      }

      if (isBootshausReservierung) {
        if (saveBootshaus) {
          calendar.getComponents().add(termin);
        }
      } else {
        if (saveBoote) {
          calendar.getComponents().add(termin);
        }
      }
    }

    // Saving as iCalendar file
    saveCalendarToFile(calendar, "OH-Bootshaus");
  }

  private void saveAllClubworkToCalendarFileIntern() throws EfaException, IOException,
  ValidationException {
    // Creating a new calendar
    net.fortuna.ical4j.model.Calendar calendar = new net.fortuna.ical4j.model.Calendar();

    Clubwork clubwork = Daten.project.getCurrentClubwork();
    for (DataKey<?, ?, ?> dataKey : clubwork.data().getAllKeys()) {
      DataRecord dataRecord = clubwork.data().get(dataKey);
      ClubworkRecord clubworkRecord = (ClubworkRecord) dataRecord;

      String firstLastName = clubworkRecord.getFirstLastName();
      String description = clubworkRecord.getDescription();
      DataTypeDate workDate = clubworkRecord.getWorkDate();
      double hours = clubworkRecord.getHours();
      DateTime dateTimeLastModified = new DateTime(clubworkRecord.getLastModified());
      String dateTimeLastModifiedStr = dateTimeLastModified.toString().replace('T', '.');
      String efaId = EFA + firstLastName.substring(0, 1).toUpperCase();
      String uid = dateTimeLastModifiedStr + "." + efaId + ABFX_DE;

      String descriptionAlle = firstLastName + CRLF;
      descriptionAlle += description + CRLF;
      descriptionAlle += workDate + " ganze " + hours + " Stunden" + CRLF;
      descriptionAlle += "(eingetragen am " + dateTimeLastModifiedStr + ")" + CRLF;
      if (clubworkRecord.getDeleted()) {
        descriptionAlle += "Dieser Eintrag wurde gelöscht" + CRLF;
      }

      VEvent event = new VEvent(new Date(workDate.getTimestamp(null)),
          firstLastName + " " + hours + "h " + description);
      event.getProperties().add(new Description(descriptionAlle));
      event.getProperties().add(new Uid(uid));

      calendar.getComponents().add(event);
    }

    saveCalendarToFile(calendar, "OH-Vereinsarbeit");
  }

  private void saveCalendarToFile(net.fortuna.ical4j.model.Calendar iCalendar,
      String title) throws FileNotFoundException, IOException, ValidationException {

    String dateTimeStamp = new DateTime().toString().replace('T', '.');
    String extension = ".ics";
    String description = title + " vom " + dateTimeStamp;

    iCalendar.getProperties().add(new ProdId("-//Arndt Boris Fahr//iCal4j 1.0//EN"));
    iCalendar.getProperties().add(Version.VERSION_2_0);
    iCalendar.getProperties().add(CalScale.GREGORIAN);
    iCalendar.getProperties().add(new XProperty("X-WR-CALNAME", title));
    iCalendar.getProperties().add(new XProperty("X-WR-TIMEZONE", "Europe/Copenhagen"));
    iCalendar.getProperties().add(new XProperty("X-LIC-LOCATION", "Europe/Copenhagen"));
    iCalendar.getProperties().add(new XProperty("X-WR-CALDESC", description));

    // Saving as iCalendar file
    CalendarOutputter outputter = new CalendarOutputter();
    outputter.setValidating(true);

    String path = Daten.efaBaseConfig.efaUserDirectory + Daten.fileSep;
    FileOutputStream foutohne = new FileOutputStream(path + title + extension);
    outputter.output(iCalendar, foutohne);

    path += "backup" + Daten.fileSep;
    FileOutputStream foutdat = new FileOutputStream(path + dateTimeStamp + "." + title + extension);
    outputter.output(iCalendar, foutdat);
  }
}
