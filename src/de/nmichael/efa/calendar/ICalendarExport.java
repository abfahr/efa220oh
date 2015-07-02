
package de.nmichael.efa.calendar;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.UUID;

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
  private static final UUID BOOTSHAUS = new UUID(-7033734156567033637L, -8676639372818108974L);
  private static final String EFA = "efa";
  private static final String ABFX_DE = "@abfx.de";

  public void saveAllReservationToCalendarFile() throws IOException, ValidationException,
  ParserException, EfaException, ParseException {

    // Creating a new calendar
    net.fortuna.ical4j.model.Calendar alleCalendar = new net.fortuna.ical4j.model.Calendar();
    net.fortuna.ical4j.model.Calendar bootshausCalendar = new net.fortuna.ical4j.model.Calendar();

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
      UUID boatId = boatReservationRecord.getBoatId();
      DataTypeDate dateFrom = boatReservationRecord.getDateFrom();
      DataTypeTime timeFrom = boatReservationRecord.getTimeFrom();
      DataTypeDate dateTo = boatReservationRecord.getDateTo();
      DataTypeTime timeTo = boatReservationRecord.getTimeTo();
      String contactPhone = boatReservationRecord.getContact();
      String personAsName = boatReservationRecord.getPersonAsName();
      String reason = boatReservationRecord.getReason();
      String reservationTimeDescription = boatReservationRecord.getReservationTimeDescription();
      DateTime dateTimeLastModified = new DateTime(boatReservationRecord.getLastModified());
      String dateTimeLastModifiedStr = dateTimeLastModified.toString().replace('T', '.');
      int reservationOrder = boatReservationRecord.getReservation();
      String efaId = EFA + reservationOrder + personAsName.substring(0, 1).toUpperCase();
      String uid = dateTimeLastModifiedStr + "." + efaId + ABFX_DE;

      String descriptionAlle = reservationTimeDescription + CRLF;
      if (!"k.A.".equals(reason)) {
        descriptionAlle += "Anlass: " + reason + CRLF;
      }
      descriptionAlle += "Mitglied: " + personAsName + CRLF;
      if (!"".equals(contactPhone)) {
        descriptionAlle += "Fon: " + contactPhone + CRLF;
      }
      String modif = "(" + efaId + " aktualisiert am " + dateTimeLastModifiedStr + ")";
      descriptionAlle += modif;
      String descriptionBhnutzung = reservationTimeDescription + CRLF + modif;

      if (BoatReservationRecord.TYPE_WEEKLY.equals(type)) {
        dateFrom = DataTypeDate.today();
        dateFrom.addDays(-7); // TODO next THursday
        dateTo = dateFrom;
      }
      DateTime startDateTime = new DateTime(dateFrom.getTimestamp(timeFrom));
      DateTime endDateTime = new DateTime(dateTo.getTimestamp(timeTo));

      // Creating an event
      VEvent buchungAlle = new VEvent(startDateTime, endDateTime, boatName + " - " + personAsName);
      VEvent nurBootshaus = new VEvent(startDateTime, endDateTime, boatName + " - " + efaId);

      if (BoatReservationRecord.TYPE_WEEKLY.equals(type)) {
        // String recur3 = "RRULE:FREQ=" + type + ";BYDAY=" + dayOfWeek.substring(0, 2);
        Recur recur = new Recur();
        recur.setFrequency(Recur.WEEKLY);
        recur.getDayList().add(new WeekDay(dayOfWeek.substring(0, 2)));
        buchungAlle.getProperties().add(new RRule(recur));
        nurBootshaus.getProperties().add(new RRule(recur));
      }
      buchungAlle.getProperties().add(new Description(descriptionAlle));
      buchungAlle.getProperties().add(new Location("Isekai 10 Hamburg"));
      buchungAlle.getProperties().add(new Uid(uid));
      nurBootshaus.getProperties().add(new Description(descriptionBhnutzung));
      nurBootshaus.getProperties().add(new Location("Isekai 10 Hamburg"));
      nurBootshaus.getProperties().add(new Uid(uid));

      alleCalendar.getComponents().add(buchungAlle);
      if (boatId.equals(BOOTSHAUS)) {
        bootshausCalendar.getComponents().add(nurBootshaus);
      }
    }

    // Saving as iCalendar file
    saveCalendarToFile(alleCalendar, "OH-Reservierungen");
    saveCalendarToFile(bootshausCalendar, "OH-Bootshaus");
  }

  public void saveAllClubworkToCalendarFile() throws EfaException, IOException, ValidationException {
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

    String path = Daten.efaBaseConfig.efaUserDirectory + Daten.fileSep + "backup" + Daten.fileSep;
    String dateTimeStamp = new DateTime().toString().replace('T', '.');
    String extension = ".ics";
    String description = title + " vom " + dateTimeStamp;

    iCalendar.getProperties().add(new ProdId("-//Arndt Boris Fahr//iCal4j 1.0//EN"));
    iCalendar.getProperties().add(Version.VERSION_2_0);
    iCalendar.getProperties().add(CalScale.GREGORIAN);
    iCalendar.getProperties().add(new XProperty("X-WR-CALNAME", title));
    iCalendar.getProperties().add(new XProperty("X-WR-TIMEZONE", "Europe/Berlin"));
    iCalendar.getProperties().add(new XProperty("X-LIC-LOCATION", "Europe/Berlin"));
    iCalendar.getProperties().add(new XProperty("X-WR-CALDESC", description));

    // Saving as iCalendar file
    CalendarOutputter outputter = new CalendarOutputter();
    outputter.setValidating(true);
    FileOutputStream foutohne = new FileOutputStream(path + title + extension);
    FileOutputStream foutdat = new FileOutputStream(path + dateTimeStamp + "." + title + extension);
    outputter.output(iCalendar, foutohne);
    outputter.output(iCalendar, foutdat);
  }
}
