
package de.nmichael.efa.calendar;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.UUID;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.WeekDay;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.util.UidGenerator;
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

  public static final String CRLF = " " + net.fortuna.ical4j.util.Strings.LINE_SEPARATOR; // "\r\n"
  private static final UUID BOOTSHAUS = new UUID(-7033734156567033637L, -8676639372818108974L);

  public void saveAllReservationToCalendarFile() throws IOException, ValidationException,
      ParserException, EfaException, ParseException {

    BoatReservations boatReservations = Daten.project.getBoatReservations(false);

    String ordner = Daten.efaBaseConfig.efaUserDirectory + Daten.fileSep + "backup" + Daten.fileSep;
    String dateTimeStamp = new DateTime().toString().replace('T', '.');
    String allCalFile = "Reservierungen";
    String bhnutzungCalFile = "Bootshaus";

    // Creating a new calendar
    net.fortuna.ical4j.model.Calendar alleCalendar = new net.fortuna.ical4j.model.Calendar();
    alleCalendar.getProperties().add(new ProdId("-//Arndt Boris Fahr//EFA am OH//EN"));
    alleCalendar.getProperties().add(Version.VERSION_2_0);
    alleCalendar.getProperties().add(CalScale.GREGORIAN);
    // Creating a new calendar
    net.fortuna.ical4j.model.Calendar bhnutzungCalendar = new net.fortuna.ical4j.model.Calendar();
    bhnutzungCalendar.getProperties().add(new ProdId("-//Arndt Boris Fahr//EFA am OH//EN"));
    bhnutzungCalendar.getProperties().add(Version.VERSION_2_0);
    bhnutzungCalendar.getProperties().add(CalScale.GREGORIAN);

    Date bisSilvester = new Date();
    bisSilvester.setMonth(12 - 1);
    bisSilvester.setDate(31);

    for (DataKey<?, ?, ?> oneKey : boatReservations.data().getAllKeys()) {
      DataRecord dataRecord = boatReservations.data().get(oneKey);
      BoatReservationRecord reservation = (BoatReservationRecord) dataRecord;

      if (reservation.getDeleted()) {
        continue;
      }

      String type = reservation.getType(); // weekly? ONETIME?
      String dayOfWeek = reservation.getDayOfWeek();
      String boatName = reservation.getBoatName();
      UUID boatId = reservation.getBoatId();
      DataTypeDate dateFrom = reservation.getDateFrom();
      DataTypeTime timeFrom = reservation.getTimeFrom();
      DataTypeDate dateTo = reservation.getDateTo();
      DataTypeTime timeTo = reservation.getTimeTo();
      String contactPhone = reservation.getContact();
      String personAsName = reservation.getPersonAsName();
      String reason = reservation.getReason();
      String reservationTimeDescription = reservation.getReservationTimeDescription();
      DateTime dateTimeLastModified = new DateTime(reservation.getLastModified());
      int reservationOrder = reservation.getReservation();

      String descriptionAlle = reservationTimeDescription + CRLF;
      if (!"k.A.".equals(reason)) {
        descriptionAlle += "Anlass: " + reason + CRLF;
      }
      descriptionAlle += "Mitglied: " + personAsName + CRLF;
      if (!"".equals(contactPhone)) {
        descriptionAlle += "Fon: " + contactPhone + CRLF;
      }
      descriptionAlle += "(#" + reservationOrder;
      descriptionAlle += " aktualisiert am " + dateTimeLastModified + ")";

      String descriptionNurBhnutzung = reservationTimeDescription + CRLF;
      descriptionNurBhnutzung += "(#" + reservationOrder;
      descriptionNurBhnutzung += " aktualisiert am " + dateTimeLastModified
          + ")";

      // Creating an event
      if (BoatReservationRecord.TYPE_WEEKLY.equals(type)) {
        // wiederholender Termin
        dateFrom = DataTypeDate.today();
        dateFrom.addDays(-1);
        dateTo = dateFrom;
      }
      DateTime startDateTime = new DateTime(dateFrom.getTimestamp(timeFrom));
      DateTime endDateTime = new DateTime(dateTo.getTimestamp(timeTo));
      VEvent buchungAlle = new VEvent(startDateTime, endDateTime, boatName + " - " + personAsName);
      VEvent nurBootshaus = new VEvent(startDateTime, endDateTime,
          boatName + " - #" + reservationOrder + personAsName.substring(0, 1));

      if (BoatReservationRecord.TYPE_WEEKLY.equals(type)) {
        // wiederholender Termin
        Recur recur = new Recur(Recur.WEEKLY, bisSilvester);
        recur.getDayList().add(new WeekDay(dayOfWeek.substring(0, 2)));
        RRule rRule = new RRule(recur);
        buchungAlle.getProperties().add(rRule);
        nurBootshaus.getProperties().add(rRule);
      }
      buchungAlle.getProperties().add(new Description(descriptionAlle));
      nurBootshaus.getProperties().add(new Description(descriptionNurBhnutzung));
      buchungAlle.getProperties().add(new Location("Isekai 10 Hamburg"));
      nurBootshaus.getProperties().add(new Location("Isekai 10 Hamburg"));
      buchungAlle.getProperties().add(new UidGenerator("oh-id-" + reservationOrder).generateUid());
      nurBootshaus.getProperties().add(new UidGenerator("oh-id-" + reservationOrder).generateUid());
      alleCalendar.getComponents().add(buchungAlle);

      if (boatId.equals(BOOTSHAUS)) {
        bhnutzungCalendar.getComponents().add(nurBootshaus);
      }
    }

    // Saving an iCalendar file
    FileOutputStream fout = new FileOutputStream(ordner + dateTimeStamp + "." + allCalFile + ".ics");
    CalendarOutputter outputter = new CalendarOutputter();
    outputter.setValidating(false);
    outputter.output(alleCalendar, fout);
    // Saving an iCalendar file
    FileOutputStream bhnfout = new FileOutputStream(ordner + dateTimeStamp + "." + bhnutzungCalFile
        + ".ics");
    CalendarOutputter bhnOutputter = new CalendarOutputter();
    bhnOutputter.setValidating(false);
    bhnOutputter.output(bhnutzungCalendar, bhnfout);
  }

  public void saveAllClubworkToCalendarFile() throws EfaException, IOException, ValidationException {
    Clubwork vereinsarbeiten = Daten.project.getCurrentClubwork();

    String ordner = Daten.efaBaseConfig.efaUserDirectory + Daten.fileSep + "backup" + Daten.fileSep;
    String dateTimeStamp = new DateTime().toString().replace('T', '.');
    String allVereinsarbetFile = "Vereinsarbeit";

    // Creating a new calendar
    net.fortuna.ical4j.model.Calendar alleVereinsarbeitCalendar = new net.fortuna.ical4j.model.Calendar();
    alleVereinsarbeitCalendar.getProperties().add(new ProdId("-//Arndt Boris Fahr//EFA am OH//EN"));
    alleVereinsarbeitCalendar.getProperties().add(Version.VERSION_2_0);
    alleVereinsarbeitCalendar.getProperties().add(CalScale.GREGORIAN);

    for (DataKey<?, ?, ?> oneKey : vereinsarbeiten.data().getAllKeys()) {
      DataRecord dataRecord = vereinsarbeiten.data().get(oneKey);
      ClubworkRecord vereinsarbeit = (ClubworkRecord) dataRecord;
      if (vereinsarbeit.getDeleted()) {
        continue;
      }

      DataTypeDate workDate = vereinsarbeit.getWorkDate();
      double hours = vereinsarbeit.getHours();
      String description = vereinsarbeit.getDescription();
      String firstLastName = vereinsarbeit.getFirstLastName();
      DateTime dateTimeLastModified = new DateTime(vereinsarbeit.getLastModified());

      Date workCalDate = new Date(workDate.getTimestamp(null));// .toCalendar().getTimeInMillis());
      VEvent vereinsarbeitEvent = new VEvent(workCalDate, firstLastName + " " + hours + "h "
          + description);
      vereinsarbeitEvent.getProperties().getProperty(Property.DTSTART).getParameters()
      .add(Value.DATE);

      String descriptionAlle = firstLastName + CRLF;
      descriptionAlle += description + CRLF;
      descriptionAlle += workDate + " ganze " + hours + " Stunden" + CRLF;
      descriptionAlle += "(eingetragen am " + dateTimeLastModified + ")";

      vereinsarbeitEvent.getProperties().add(new Description(descriptionAlle));
      vereinsarbeitEvent.getProperties().add(new UidGenerator("oh-work").generateUid());

      alleVereinsarbeitCalendar.getComponents().add(vereinsarbeitEvent);

    }
    // Saving an iCalendar file
    FileOutputStream fout = new FileOutputStream(ordner + dateTimeStamp + "." + allVereinsarbetFile
        + ".ics");
    CalendarOutputter outputter = new CalendarOutputter();
    outputter.setValidating(false);
    outputter.output(alleVereinsarbeitCalendar, fout);

  }
}
