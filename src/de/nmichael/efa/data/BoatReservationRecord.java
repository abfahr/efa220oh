/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.data;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import net.fortuna.ical4j.model.DateTime;
import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeDate;
import de.nmichael.efa.core.items.ItemTypeLabel;
import de.nmichael.efa.core.items.ItemTypeRadioButtons;
import de.nmichael.efa.core.items.ItemTypeString;
import de.nmichael.efa.core.items.ItemTypeStringAutoComplete;
import de.nmichael.efa.core.items.ItemTypeStringList;
import de.nmichael.efa.core.items.ItemTypeTime;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.storage.MetaData;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.gui.util.TableItem;
import de.nmichael.efa.gui.util.TableItemHeader;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

// @i18n complete
public class BoatReservationRecord extends DataRecord {

  private static final long LONG_MILLI_SECONDS_PER_DAY = 24 * 60 * 60 * 1000;
  // =========================================================================
  // Value Constants
  // =========================================================================
  public static final String TYPE_ONETIME = "ONETIME";
  public static final String TYPE_WEEKLY = "WEEKLY";

  // =========================================================================
  // Field Names
  // =========================================================================
  public static final String VBOAT = "VirtualBoat";
  public static final String BOATID = "BoatId";
  public static final String RESERVATION = "Reservation";
  public static final String TYPE = "Type";
  public static final String VRESERVATIONDATE = "VirtualReservationDate";
  public static final String DATEFROM = "DateFrom";
  public static final String DATETO = "DateTo";
  public static final String DAYOFWEEK = "DayOfWeek";
  public static final String TIMEFROM = "TimeFrom";
  public static final String TIMETO = "TimeTo";
  public static final String VPERSON = "VirtualPerson";
  public static final String PERSONID = "PersonId";
  public static final String PERSONNAME = "PersonName";
  public static final String REASON = "Reason";
  public static final String CONTACT = "Contact";

  public static final String[] IDX_BOATID = new String[] { BOATID };

  public static void initialize() {
    Vector<String> f = new Vector<String>();
    Vector<Integer> t = new Vector<Integer>();

    f.add(VBOAT);
    t.add(IDataAccess.DATA_VIRTUAL);
    f.add(BOATID);
    t.add(IDataAccess.DATA_UUID);
    f.add(RESERVATION);
    t.add(IDataAccess.DATA_INTEGER);
    f.add(TYPE);
    t.add(IDataAccess.DATA_STRING);
    f.add(VRESERVATIONDATE);
    t.add(IDataAccess.DATA_VIRTUAL);
    f.add(DATEFROM);
    t.add(IDataAccess.DATA_DATE);
    f.add(DATETO);
    t.add(IDataAccess.DATA_DATE);
    f.add(DAYOFWEEK);
    t.add(IDataAccess.DATA_STRING);
    f.add(TIMEFROM);
    t.add(IDataAccess.DATA_TIME);
    f.add(TIMETO);
    t.add(IDataAccess.DATA_TIME);
    f.add(VPERSON);
    t.add(IDataAccess.DATA_VIRTUAL);
    f.add(PERSONID);
    t.add(IDataAccess.DATA_UUID);
    f.add(PERSONNAME);
    t.add(IDataAccess.DATA_STRING);
    f.add(REASON);
    t.add(IDataAccess.DATA_STRING);
    f.add(CONTACT);
    t.add(IDataAccess.DATA_STRING);
    MetaData metaData = constructMetaData(BoatReservations.DATATYPE, f, t, false);
    metaData.setKey(new String[] { BOATID, RESERVATION });
    metaData.addIndex(IDX_BOATID);
  }

  public BoatReservationRecord(BoatReservations boatReservation, MetaData metaData) {
    super(boatReservation, metaData);
  }

  @Override
  public DataRecord createDataRecord() { // used for cloning
    return getPersistence().createNewRecord();
  }

  @Override
  public DataKey<UUID, Integer, ?> getKey() {
    return new DataKey<UUID, Integer, String>(getBoatId(), getReservation(), null);
  }

  public static DataKey<UUID, Integer, ?> getKey(UUID id, int res) {
    return new DataKey<UUID, Integer, String>(id, res, null);
  }

  @Override
  public boolean isValidAt(long validAt) {
    return true;
    // Boat Reservation are always valid and should be shown even if the boat is invalid
    // return getPersistence().getProject().getBoats(false).isValidAt(getBoatId(), validAt);
  }

  @Override
  public boolean getDeleted() {
    return getPersistence().getProject().getBoats(false).isBoatDeleted(getBoatId());
  }

  public void setBoatId(UUID id) {
    setUUID(BOATID, id);
  }

  public UUID getBoatId() {
    return getUUID(BOATID);
  }

  public void setReservation(int no) {
    setInt(RESERVATION, no);
  }

  public int getReservation() {
    return getInt(RESERVATION);
  }

  public void setType(String type) {
    setString(TYPE, type);
  }

  public String getType() {
    return getString(TYPE);
  }

  public void setDateFrom(DataTypeDate date) {
    setDate(DATEFROM, date);
  }

  public DataTypeDate getDateFrom() {
    return getDate(DATEFROM);
  }

  public void setDateTo(DataTypeDate date) {
    setDate(DATETO, date);
  }

  public DataTypeDate getDateTo() {
    return getDate(DATETO);
  }

  public void setDayOfWeek(String dayOfWeek) {
    setString(DAYOFWEEK, dayOfWeek);
  }

  public String getDayOfWeek() {
    return getString(DAYOFWEEK);
  }

  public void setTimeFrom(DataTypeTime time) {
    setTime(TIMEFROM, time);
  }

  public DataTypeTime getTimeFrom() {
    return getTime(TIMEFROM);
  }

  public void setTimeTo(DataTypeTime time) {
    setTime(TIMETO, time);
  }

  public DataTypeTime getTimeTo() {
    return getTime(TIMETO);
  }

  public void setPersonId(UUID id) {
    setUUID(PERSONID, id);
  }

  public UUID getPersonId() {
    return getUUID(PERSONID);
  }

  public void setPersonName(String name) {
    setString(PERSONNAME, name);
  }

  public String getPersonName() {
    return getString(PERSONNAME);
  }

  public void setReason(String reason) {
    setString(REASON, reason);
  }

  public String getReason() {
    String s = getString(REASON);
    if (s == null || s.length() == 0) {
      if (isBootshausOH()) {
        s = International.getString("privat mit Vertrag");
      } else {
        s = International.getString("");
      }

    }
    return s;
  }

  public void setContact(String contact) {
    setString(CONTACT, contact);
  }

  public String getContact() {
    String s = getString(CONTACT);
    if (s == null || s.length() == 0) {
      return "";
    }
    return s;
  }

  private String getDateDescription(DataTypeDate date, String weekday, DataTypeTime time) {
    if (date == null && weekday == null) {
      return "";
    }
    return (date != null ? date.toString() : EfaTypes.getValueWeekday(weekday)) +
        (time != null ? " " + time.toString() : "");
  }

  public String getDateTimeFromDescription() {
    String type = getType();
    if (type != null && type.equals(TYPE_ONETIME)) {
      return getDateDescription(getDateFrom(), null, getTimeFrom());
    }
    if (type != null && type.equals(TYPE_WEEKLY)) {
      return getDateDescription(null, getDayOfWeek(), getTimeFrom());
    }
    return "";
  }

  public String getDateTimeToDescription() {
    String type = getType();
    if (type != null && type.equals(TYPE_ONETIME)) {
      return getDateDescription(getDateTo(), null, getTimeTo());
    }
    if (type != null && type.equals(TYPE_WEEKLY)) {
      return getDateDescription(null, getDayOfWeek(), getTimeTo());
    }
    return "";
  }

  public String getReservationTimeDescription() {
    return getDateTimeFromDescription() + " - " + getDateTimeToDescription();
  }

  private PersonRecord getPersonRecord() {
    UUID id = getPersonId();
    if (id == null) {
      return null;
    }
    try {
      Persons persons = getPersistence().getProject().getPersons(false);
      return persons.getPerson(id, System.currentTimeMillis());
    } catch (Exception e) {
      return null;
    }
  }

  public String getPersonAsName() {
    PersonRecord p = getPersonRecord();
    if (p != null) {
      return p.getQualifiedName();
    }
    return getPersonName();
  }

  public BoatRecord getBoat() {
    Boats boats = getPersistence().getProject().getBoats(false);
    if (boats != null) {
      BoatRecord r = boats.getBoat(getBoatId(), System.currentTimeMillis());
      if (r == null) {
        r = boats.getAnyBoatRecord(getBoatId());
      }
      return r;
    }
    return null;
  }

  public String getBoatName() {
    BoatRecord r = getBoat();
    String boatName = "?";
    if (r != null) {
      boatName = r.getQualifiedName();
    }
    return boatName;
  }

  @Override
  protected Object getVirtualColumn(int fieldIdx) {
    if (getFieldName(fieldIdx).equals(VBOAT)) {
      return getBoatName();
    }
    if (getFieldName(fieldIdx).equals(VRESERVATIONDATE)) {
      return getReservationTimeDescription();
    }
    if (getFieldName(fieldIdx).equals(VPERSON)) {
      return getPersonAsName();
    }
    return null;
  }

  /**
   * @param now
   * @param lookAheadMinutes
   * @return 0 if valid now; n>0 in valid in n minutes; <0 if not valid within specified interval
   */
  public long getReservationValidInMinutes(long now, long lookAheadMinutes) {
    try {
      DataTypeDate dateFrom = null;
      DataTypeDate dateTo = null;
      DataTypeTime timeFrom = null;
      DataTypeTime timeTo = null;
      if (this.getType().equals(TYPE_ONETIME)) {
        dateFrom = this.getDateFrom();
        dateTo = this.getDateTo();
        timeFrom = this.getTimeFrom();
        timeTo = this.getTimeTo();
      }
      if (this.getType().equals(TYPE_WEEKLY)) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(now);
        int weekday = cal.get(Calendar.DAY_OF_WEEK);
        String dayOfWeek = getDayOfWeek();
        // Note: lookAheadMinutes is not supported over midnight for weekly reservations
        switch (weekday) {
          case Calendar.MONDAY:
            if (!dayOfWeek.equals(EfaTypes.TYPE_WEEKDAY_MONDAY)) {
              return -1;
            }
            break;
          case Calendar.TUESDAY:
            if (!dayOfWeek.equals(EfaTypes.TYPE_WEEKDAY_TUESDAY)) {
              return -1;
            }
            break;
          case Calendar.WEDNESDAY:
            if (!dayOfWeek.equals(EfaTypes.TYPE_WEEKDAY_WEDNESDAY)) {
              return -1;
            }
            break;
          case Calendar.THURSDAY:
            if (!dayOfWeek.equals(EfaTypes.TYPE_WEEKDAY_THURSDAY)) {
              return -1;
            }
            break;
          case Calendar.FRIDAY:
            if (!dayOfWeek.equals(EfaTypes.TYPE_WEEKDAY_FRIDAY)) {
              return -1;
            }
            break;
          case Calendar.SATURDAY:
            if (!dayOfWeek.equals(EfaTypes.TYPE_WEEKDAY_SATURDAY)) {
              return -1;
            }
            break;
          case Calendar.SUNDAY:
            if (!dayOfWeek.equals(EfaTypes.TYPE_WEEKDAY_SUNDAY)) {
              return -1;
            }
            break;
        }
        // ok, this is our weekday!
        dateFrom = new DataTypeDate(now);
        dateTo = new DataTypeDate(now);
        timeFrom = this.getTimeFrom();
        timeTo = this.getTimeTo();
      }
      long resStart = dateFrom.getTimestamp(timeFrom);
      long resEnd = dateTo.getTimestamp(timeTo);

      // ist die vorliegende Reservierung jetzt gültig
      if (now >= resStart && now <= resEnd) {
        return 0;
      }

      // ist die vorliegende Reservierung innerhalb von minutesAhead gültig
      if (now < resStart && now + lookAheadMinutes * 60 * 1000 >= resStart) {
        return (resStart - now) / (60 * 1000);
      }

    } catch (Exception e) {
      Logger.logdebug(e);
    }
    return -1;
  }

  public boolean isObsolete(long now) {
    try {
      if (this.getType().equals(TYPE_WEEKLY)) {
        return false;
      }
      if (this.getType().equals(TYPE_ONETIME)) {
        DataTypeDate dateTo = this.getDateTo();
        DataTypeTime timeTo = this.getTimeTo();
        long resEnd = dateTo.getTimestamp(timeTo);
        if (isBootshausOH()) { // Bootshaus stehen lassen
          long tage = Daten.efaConfig.getAnzahlTageAbgelaufenesBootshausSichtbar();
          resEnd = resEnd + tage * LONG_MILLI_SECONDS_PER_DAY; // Reservierung ende x Tage später
        }
        return now > resEnd;
      }
    } catch (Exception e) {
      Logger.logdebug(e);
    }
    return false;
  }

  /**
   * @return true, falls diese Reservierung das Bootshaus betrifft
   */
  public boolean isBootshausOH() {
    return BoatRecord.BOOTSHAUS.equals(getBoatId());
  }

  @Override
  public String getAsText(String fieldName) {
    if (fieldName.equals(BOATID)) {
      return getBoatName();
    }
    if (fieldName.equals(PERSONID)) {
      if (get(PERSONID) != null) {
        return getPersonAsName();
      } else {
        return null;
      }
    }
    return super.getAsText(fieldName);
  }

  @Override
  public boolean setFromText(String fieldName, String value) {
    if (fieldName.equals(BOATID)) {
      Boats boats = getPersistence().getProject().getBoats(false);
      BoatRecord br = boats.getBoat(value, -1);
      if (br != null) {
        set(fieldName, br.getId());
      }
    } else if (fieldName.equals(PERSONID)) {
      Persons persons = getPersistence().getProject().getPersons(false);
      PersonRecord pr = persons.getPerson(value, -1);
      if (pr != null) {
        set(fieldName, pr.getId());
      }
    } else {
      return super.setFromText(fieldName, value);
    }
    return (value.equals(getAsText(fieldName)));
  }

  @Override
  public Vector<IItemType> getGuiItems(AdminRecord admin) {
    final String CAT_BASEDATA = "%01%" + International.getString("Reservierung");
    IItemType item;
    Vector<IItemType> v = new Vector<IItemType>();

    item = new ItemTypeLabel("GUI_BOAT_NAME",
        IItemType.TYPE_PUBLIC, CAT_BASEDATA,
        International.getMessage("Reservierung für {boat}", getBoatName()));
    item.setPadding(0, 0, 0, 10);
    v.add(item);

    item = new ItemTypeRadioButtons(BoatReservationRecord.TYPE,
        (getType() != null && getType().length() > 0 ? getType() : TYPE_ONETIME),
        new String[] { TYPE_ONETIME, TYPE_WEEKLY },
        new String[] {
            International.getString("einmalig"),
            International.getString("wöchentlich"),
        },
        IItemType.TYPE_PUBLIC, CAT_BASEDATA,
        International.getString("Art der Reservierung"));
    v.add(item);

    item = new ItemTypeStringList(BoatReservationRecord.DAYOFWEEK,
        getDayOfWeek(),
        EfaTypes.makeDayOfWeekArray(EfaTypes.ARRAY_STRINGLIST_VALUES),
        EfaTypes.makeDayOfWeekArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY),
        IItemType.TYPE_PUBLIC, CAT_BASEDATA,
        International.getString("Wochentag"));
    item.setNotNull(true);
    v.add(item);

    ItemTypeDate dateFrom = new ItemTypeDate(BoatReservationRecord.DATEFROM,
        getDateFrom(),
        IItemType.TYPE_PUBLIC, CAT_BASEDATA,
        International.getString("Von") + " (" +
            International.getString("Tag") + ")");
    dateFrom.setNotNull(true);
    v.add(dateFrom);

    ItemTypeTime timeFrom = new ItemTypeTime(BoatReservationRecord.TIMEFROM,
        getTimeFrom(),
        IItemType.TYPE_PUBLIC, CAT_BASEDATA,
        International.getString("Von") + " (" +
            International.getString("Zeit") + ")");
    timeFrom.setNotNull(true);
    timeFrom.enableSeconds(false);
    v.add(timeFrom);

    ItemTypeDate dateTo = new ItemTypeDate(BoatReservationRecord.DATETO,
        getDateTo(),
        IItemType.TYPE_PUBLIC, CAT_BASEDATA,
        International.getString("Bis") + " (" +
            International.getString("Tag") + ")");
    dateTo.setNotNull(true);
    dateTo.setMustBeAfter(dateFrom, true);
    v.add(dateTo);

    ItemTypeTime timeto = new ItemTypeTime(BoatReservationRecord.TIMETO,
        getTimeTo(),
        IItemType.TYPE_PUBLIC, CAT_BASEDATA,
        International.getString("Bis") + " (" +
            International.getString("Zeit") + ")");
    timeto.setNotNull(true);
    timeto.enableSeconds(false);
    timeto.setReferenceTime(DataTypeTime.time235959());
    timeto.setMustBeAfter(dateFrom, timeFrom, dateTo, false);
    v.add(timeto);

    ItemTypeStringAutoComplete personId = getGuiItemTypeStringAutoComplete(
        BoatReservationRecord.PERSONID, null,
        IItemType.TYPE_PUBLIC, CAT_BASEDATA,
        getPersistence().getProject().getPersons(false),
        System.currentTimeMillis(),
        System.currentTimeMillis(),
        International.getString("Reserviert für"));
    personId.setNotNull(true);
    personId.setAlternateFieldNameForPlainText(BoatReservationRecord.PERSONNAME);
    if (getPersonId() != null) {
      personId.setId(getPersonId());
    } else {
      personId.parseAndShowValue(getPersonName());
    }
    v.add(personId);

    ItemTypeString reason = new ItemTypeString(BoatReservationRecord.REASON,
        getReason(),
        IItemType.TYPE_PUBLIC, CAT_BASEDATA,
        International.getString("Reservierungsgrund"));
    reason.setMinCharacters(5);
    v.add(reason);

    item = new ItemTypeString(BoatReservationRecord.CONTACT,
        getContact(),
        IItemType.TYPE_PUBLIC, CAT_BASEDATA,
        International.getString("Telefon für Rückfragen"));
    item.setNotNull(true);
    v.add(item);

    // Virtual Fields hidden internal, only for list output and export/import
    item = new ItemTypeString(BoatReservationRecord.VBOAT,
        getBoatName(),
        IItemType.TYPE_INTERNAL, "",
        International.getString("Boot"));
    v.add(item);

    item = new ItemTypeString(BoatReservationRecord.VRESERVATIONDATE,
        getReservationTimeDescription(),
        IItemType.TYPE_INTERNAL, "",
        International.getString("Zeitraum"));
    v.add(item);

    item = new ItemTypeString(BoatReservationRecord.VPERSON,
        getPersonAsName(),
        IItemType.TYPE_INTERNAL, "",
        International.getString("Person"));
    v.add(item);
    return v;
  }

  @Override
  public void saveGuiItems(Vector<IItemType> items) {
    super.saveGuiItems(items);
  }

  @Override
  public TableItemHeader[] getGuiTableHeader() {
    TableItemHeader[] header = new TableItemHeader[6];
    header[0] = new TableItemHeader(International.getString("Boot"));
    header[1] = new TableItemHeader(International.getString("Von"));
    header[2] = new TableItemHeader(International.getString("Bis"));
    header[3] = new TableItemHeader(International.getString("Reserviert für"));
    header[4] = new TableItemHeader(International.getString("Telefon für Rückfragen"));
    header[5] = new TableItemHeader(International.getString("Grund"));
    return header;
  }

  @Override
  public TableItem[] getGuiTableItems() {
    TableItem[] items = new TableItem[6];
    items[0] = new TableItem(getBoatName());
    items[1] = new TableItem(getDateTimeFromDescription());
    items[2] = new TableItem(getDateTimeToDescription());
    items[3] = new TableItem(getPersonAsName());
    items[4] = new TableItem(getContact());
    items[5] = new TableItem(getReason());
    return items;
  }

  @Override
  public String getQualifiedName() {
    return International.getMessage("Reservierung für {boat}", getBoatName());
  }

  @Override
  public String[] getQualifiedNameFields() {
    return IDX_BOATID;
  }

  public String getFormattedEmailtextBootshausnutzungswart() {
    PersonRecord p = getPersonRecord();

    List<String> msg = new ArrayList<String>();
    msg.add("Hallo Bootshausnutzungswart Wolfgang!");
    msg.add("");
    msg.add("Hier die neueste Reservierung von EFA am Isekai");
    msg.add("Eingabe durch: " + getPersonAsName() + " "
        + (p != null ? p.getMembershipNo() + " " + p.getStatusName() : "(wer ist das?)"));
    msg.add("Eingabe am: " + new DateTime(getLastModified()).toString().replace('T', '.'));
    msg.add("");
    msg.add("Reservierung des " + getBoatName());
    msg.add("für die Zeit: " + getReservationTimeDescription());
    msg.add("Kontakt: " + getContact());
    msg.add("Telefon (aus Sewobe): " + (p != null ? p.getTelefonFestnetz() : "..."));
    msg.add("Handy (aus Sewobe): " + (p != null ? p.getTelefonHandy() : "..."));
    msg.add("Email (aus Sewobe): " + (p != null ? p.getEmail() : "..."));
    msg.add("Grund der Reservierung: " + getReason());
    msg.add("");
    msg.add("mit freundlichen Grüßen");
    msg.add("Efa");
    msg.add("");
    if (p != null) {
      msg.add("PS: Hi Wolle, der nachfolgende Text ist eine Vorlage für eine Antwort an das Mitglied "
          + p.getEmail());
      // msg.add("");
      if (p.getGender().equals(EfaTypes.TYPE_GENDER_FEMALE)) {
        msg.add("Liebe " + p.getFirstName() + ",");
      } else {
        msg.add("Lieber " + p.getFirstName() + ",");
      }
      msg.add("Dein Vertrag ist beim Nutzungswart eingegangen und wurde in die Datenbank eingegeben. Mit der nächsten Abbuchung wird das Nutzungsentgeld abgebucht. Bitte beachte, dass der Verein bis vier Wochen vor dem beantragten Veranstaltungstermin das Vortrittsrecht hat (siehe allgemeine Vertragsinhalte). In diesem Falle wird das Entgelt zurück überwiesen.");
      msg.add("Reservierung des " + getBoatName() + " für die Zeit: "
          + getReservationTimeDescription());
      msg.add("Viel Spaß und Gruß");
      msg.add("Wolfgang (Bootshausnutzungswart)");
    }
    return join(msg);
  }

  public String getFormattedEmailtextMitglied(PersonRecord p) {
    if (p == null) {
      return null;
    }
    List<String> msg = new ArrayList<String>();
    msg.add("Hallo " + p.getFirstName() + "!");
    msg.add("");
    msg.add("Hier eine Erinnerung an Deine Reservierung in EFA am Isekai. "
        + "(Eingabe am " + new DateTime(getLastModified()).toString().replace('T', '.') + ")");
    msg.add("");
    msg.add("Reservierung des " + getBoatName());
    msg.add("für die Zeit: " + getReservationTimeDescription() + " für " + getPersonAsName());
    msg.add("Grund der Reservierung: " + getReason());
    msg.add("");
    if (isBootshausOH()) {
      msg.add("Solltest Du (noch) keinen Bootshausnutzungsvertrag unterschrieben haben, dann fülle das Formular schnell aus (http://www.overfreunde.de/downloads.html) und gib es im Bootshaus rechtzeitig vor deiner Bootshausnutzung ab (ansonsten werden dir automatisch 75 Euro berechnet).");
    }
    msg.add("Solltest Du diese Reservierung (inzwischen) nicht (mehr) brauchen, dann trage Dich bitte im Bootshaus wieder aus.");
    if (isBootshausOH()) {
      msg.add("Bitte denke daran, das Bootshaus nach der Nutzung aufgeräumt und gereinigt zu hinterlassen.");
    }
    msg.add("Ansonsten viel Spaß im " + getBoatName());
    msg.add("");
    msg.add("mit freundlichen Grüßen");
    msg.add("Efa");
    // msg.add("");
    // msg.add(toString());
    return join(msg);
  }

  private String join(List<String> list) {
    StringBuilder sb = new StringBuilder();
    for (String single : list) {
      sb.append(single).append("\n");
    }
    return sb.toString();
  }
}
