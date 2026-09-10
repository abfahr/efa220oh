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
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import de.nmichael.efa.Daten;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.MetaData;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.ex.EfaModifyException;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

// @i18n complete

public class BoatReservations extends StorageObject {

  public static final String DATATYPE = "efa2boatreservations";
  private static final ThreadLocal<Boolean> IGNORE_RESERVATION_CONFLICTS =
      ThreadLocal.withInitial(() -> false);

  public static void setIgnoreReservationConflictsForCurrentThread(boolean ignoreConflicts) {
    IGNORE_RESERVATION_CONFLICTS.set(ignoreConflicts);
  }

  public BoatReservations(int storageType,
      String storageLocation,
      String storageUsername,
      String storagePassword,
      String storageObjectName) {
    super(storageType, storageLocation, storageUsername, storagePassword, storageObjectName,
        DATATYPE, International.getString("Bootsreservierungen"));
    BoatReservationRecord.initialize();
    dataAccess.setMetaData(MetaData.getMetaData(DATATYPE));
  }

  @Override
  public DataRecord createNewRecord() {
    return new BoatReservationRecord(this, MetaData.getMetaData(DATATYPE));
  }

  public BoatReservationRecord createBoatReservationsRecordFromClone(UUID id,
      BoatReservationRecord original) {
    int reservationId = getNextReservation(id);
    if (reservationId > 0) {
      BoatReservationRecord r = (BoatReservationRecord) original.cloneRecord();
      r.setBoatId(id);
      r.setReservation(reservationId);
      r.resetHashId();
      return r;
    }
    return null;
  }

  public BoatReservationRecord createBoatReservationsRecord(UUID id) {
    int val = getNextReservation(id);
    if (val > 0) {
      return createBoatReservationsRecord(id, val);
    }
    return null;
  }

  private int getNextReservation(UUID id) {
    AutoIncrement autoIncrement = getProject().getAutoIncrement(false);

    int tries = 0;
    int val = 0;
    try {
      while (tries++ < 100) {
        // usually autoincrement should always give a unique new id.
        // but in case our id's got out of sync, we try up to 100 times to fine a
        // new unique reservation id.
        val = autoIncrement.nextAutoIncrementIntValue(data().getStorageObjectType());
        if (val <= 0) {
          break;
        }
        if (data().get(BoatReservationRecord.getKey(id, val)) == null) {
          break;
        }
      }
    } catch (Exception e) {
      Logger.logdebug(e);
    }
    return val;
  }

  public BoatReservationRecord createBoatReservationsRecord(UUID id, int reservation) {
    BoatReservationRecord r = new BoatReservationRecord(this, MetaData.getMetaData(DATATYPE));
    r.setBoatId(id);
    r.setReservation(reservation);
    return r;
  }

  public BoatReservationRecord[] getBoatReservationsByPerson(UUID personId) {
    try {
      DataKey<?, ?, ?>[] keys = data().getByFields(
          new String[] { BoatReservationRecord.PERSONID },
          new Object[] { personId });
      if (keys == null || keys.length == 0) {
        return null;
      }
      BoatReservationRecord[] recs = new BoatReservationRecord[keys.length];
      for (int i = 0; i < keys.length; i++) {
        recs[i] = (BoatReservationRecord) data().get(keys[i]);
      }
      return recs;
    } catch (Exception e) {
      Logger.logdebug(e);
      return null;
    }
  }

  public BoatReservationRecord[] getBoatReservations(UUID boatId) {
    try {
      DataKey<?, ?, ?>[] keys = data()
          .getByFields(BoatReservationRecord.IDX_BOATID, new Object[] { boatId });
      if (keys == null || keys.length == 0) {
        return null;
      }
      BoatReservationRecord[] recs = new BoatReservationRecord[keys.length];
      for (int i = 0; i < keys.length; i++) {
        recs[i] = (BoatReservationRecord) data().get(keys[i]);
      }
      return recs;
    } catch (Exception e) {
      Logger.logwarn(e);
      return null;
    }
  }

  public BoatReservationRecord findBoatReservationByNumber(int reservierungsnummer) {
    try {
      @SuppressWarnings("unchecked")
      DataKey<UUID, Integer, String>[] allKeys = data().getAllKeys();
      for (DataKey<UUID, Integer, String> dataKey : allKeys) {
        if (reservierungsnummer == dataKey.getKeyPart2()) {
          DataRecord dataRecord = data().get(dataKey);
          return (BoatReservationRecord) dataRecord;
        }
      }
    } catch (EfaException e) {
      Logger.log(Logger.ERROR, Logger.MSG_DATAADM_RECORDDELETED, e);
    }
    return null;
  }

  public BoatReservationRecord[] findBoatReservationsByHashId(String hashId) {
    try {
      DataKey<?, ?, ?>[] keys = data()
          .getByFields(new String[] { BoatReservationRecord.HASHID }, new Object[] { hashId });
      if (keys == null || keys.length == 0) {
        return null;
      }
      BoatReservationRecord[] recs = new BoatReservationRecord[keys.length];
      for (int i = 0; i < keys.length; i++) {
        recs[i] = (BoatReservationRecord) data().get(keys[i]);
      }
      return recs;
    } catch (Exception e) {
      Logger.logwarn(e);
      return null;
    }
  }

  public BoatReservationRecord[] getBoatReservations(UUID boatId, long now, long lookAheadMinutes) {
    BoatReservationRecord[] reservations = getBoatReservations(boatId);

    Vector<BoatReservationRecord> activeReservations = new Vector<>();
    for (int i = 0; reservations != null && i < reservations.length; i++) {
      BoatReservationRecord r = reservations[i];
      if (r.getReservationValidInMinutes(now, lookAheadMinutes) >= 0) {
        activeReservations.add(r);
      }
    }

    if (activeReservations.isEmpty()) {
      return null;
    }
    BoatReservationRecord[] a = new BoatReservationRecord[activeReservations.size()];
    for (int i = 0; i < a.length; i++) {
      a[i] = activeReservations.get(i);
    }
    return a;
  }

  public int purgeObsoleteReservations(UUID boatId, long now) {
    BoatReservationRecord[] reservations = getBoatReservations(boatId);
    int purged = 0;

    for (int i = 0; reservations != null && i < reservations.length; i++) {
      BoatReservationRecord r = reservations[i];
      if (r.isObsolete(now)) {
        try {
          data().delete(r.getKey());
          purged++;
        } catch (Exception e) {
          Logger.log(e);
        }
      }
    }
    return purged;
  }

  public List<BoatReservationRecord> findConflictingReservations(BoatReservationRecord r) {
    List<BoatReservationRecord> conflicts = new ArrayList<>();
    BoatReservationRecord[] br = this.getBoatReservations(r.getBoatId());
    for (int i = 0; br != null && i < br.length; i++) {
      if (br[i].getReservation() == r.getReservation()) {
        continue;
      }
      if (isReservationConflict(r, br[i])) {
        conflicts.add(br[i]);
      }
    }
    return conflicts;
  }

  public String getReservationConflictsDescription(BoatReservationRecord r) {
    return getReservationConflictsDescription(findConflictingReservations(r));
  }

  public String getReservationConflictsDescription(List<BoatReservationRecord> conflicts) {
    if (conflicts == null || conflicts.isEmpty()) {
      return "";
    }
    StringBuilder msg = new StringBuilder();
    msg.append(International.getString("Folgende Reservierungen verursachen Kollisionen:"));
    for (BoatReservationRecord conflict : conflicts) {
      msg.append("\n- ")
          .append(conflict.getReservationTimeDescription(BoatReservationRecord.KEEP_NUM_DATE))
          .append("\n  ")
          .append(conflict.getPersonAsName())
          .append(" ")
          .append(conflict.getContact());
      String reason = conflict.getReason();
      if (reason != null && !reason.trim().isEmpty()) {
        msg.append("\n  ").append(reason.trim());
      }
    }
    return msg.toString();
  }

  private boolean isReservationConflict(BoatReservationRecord r, BoatReservationRecord other) {
    if (other.isWeeklyReservationType()
        && r.getType().equals(BoatReservationRecord.TYPE_ONETIME)) {
      List<DataTypeDate> liste = getListOfDates(r.getDateFrom(), r.getDateTo());
      for (DataTypeDate day : liste) {
        if (other.isWeeklyReservationOnDate(day)) {
          if (DataTypeDate.isRangeOverlap(r.getDateFrom(),
              r.getTimeFrom(),
              r.getDateTo(),
              r.getTimeTo(),
              r.getDateFrom(),
              other.getTimeFrom(),
              r.getDateTo(),
              other.getTimeTo())) {
            double anzahlStunden = r.getDurationInHours();
            double minimumDauerFuerKulanz = Daten.efaConfig.getMinimumDauerFuerKulanz();
            return anzahlStunden < minimumDauerFuerKulanz;
          }
        }
      }
    }
    if (other.isWeeklyReservationType()
        && r.isWeeklyReservationType()) {
      return isTimeRangeOverlap(r.getTimeFrom(), r.getTimeTo(),
          other.getTimeFrom(), other.getTimeTo())
          && hasWeeklyDateOverlap(r, other);
    }
    if (other.getType().equals(BoatReservationRecord.TYPE_ONETIME)
        && r.isWeeklyReservationType()) {
      List<DataTypeDate> liste = getListOfDates(other.getDateFrom(), other.getDateTo());
      for (DataTypeDate day : liste) {
        if (r.isWeeklyReservationOnDate(day)) {
          if (DataTypeDate.isRangeOverlap(other.getDateFrom(),
              other.getTimeFrom(),
              other.getDateTo(),
              other.getTimeTo(),
              other.getDateFrom(),
              r.getTimeFrom(),
              other.getDateTo(),
              r.getTimeTo())) {
            return true;
          }
        }
      }
    }
    if (other.getType().equals(BoatReservationRecord.TYPE_ONETIME)
        && r.getType().equals(BoatReservationRecord.TYPE_ONETIME)) {
      return DataTypeDate.isRangeOverlap(r.getDateFrom(),
          r.getTimeFrom(),
          r.getDateTo(),
          r.getTimeTo(),
          other.getDateFrom(),
          other.getTimeFrom(),
          other.getDateTo(),
          other.getTimeTo());
    }
    return false;
  }

  private boolean isTimeRangeOverlap(DataTypeTime r1From, DataTypeTime r1To,
      DataTypeTime r2From, DataTypeTime r2To) {
    return r1From.isBefore(r2To) && r1To.isAfter(r2From);
  }

  @Override
  public void preModifyRecordCallback(DataRecord record, boolean add, boolean update,
      boolean delete)
      throws EfaModifyException {
    if (add || update) {
      assertFieldNotEmpty(record, BoatReservationRecord.BOATID);
      assertFieldNotEmpty(record, BoatReservationRecord.RESERVATION);
      assertFieldNotEmpty(record, BoatReservationRecord.TYPE);

      BoatReservationRecord r = ((BoatReservationRecord) record);

      String myMatch = Daten.efaConfig.getRegexForVorUndNachname();
      if (!r.getPersonAsName().matches(myMatch)) {
        throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
            International.getString("Bitte Vor- und Nachname eingeben"),
            Thread.currentThread().getStackTrace());
      }
      myMatch = Daten.efaConfig.getRegexForHandynummer();
      if (!r.getContact().matches(myMatch)) {
        throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
            International.getString("Telefonnummer bitte mit separater Vorwahl"),
            Thread.currentThread().getStackTrace());
      }

      if (r.getType().equals(BoatReservationRecord.TYPE_ONETIME)) {
        DataTypeDate today = DataTypeDate.today();

        if (r.getDateFrom().isSet() && r.getDateFrom().isBefore(today)) {
          throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
              International.getString("Das Startdatum muss in der Zukunft liegen")
                  + " " + r.getDateFrom(),
              Thread.currentThread().getStackTrace());
        }
        if (r.getDateTo().isSet() && r.getDateTo().isBefore(today)) {
          throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
              International.getString("Das Enddatum muss in der Zukunft liegen"),
              Thread.currentThread().getStackTrace());
        }
        DataTypeDate ferneZukunft = today;
        ferneZukunft.addDays(4 * 365); // vier Jahre vertippt
        if (r.getDateFrom().isSet() && r.getDateFrom().isAfter(ferneZukunft)) {
          throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
              International.getString("Das Startdatum liegt zu weit in der Zukunft."),
              Thread.currentThread().getStackTrace());
        }
        if (r.getDateTo().isSet() && r.getDateTo().isAfter(ferneZukunft)) {
          throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
              International.getString("Das Enddatum liegt zu weit in der Zukunft."),
              Thread.currentThread().getStackTrace());
        }
      }

      if (r.isBootshausOH() && r.getType().equals(BoatReservationRecord.TYPE_ONETIME)) {
        // String maximaleEndZeit = "11:00"; // Uhr
        String maximaleEndZeit = Daten.efaConfig
            .getMaximaleEndUhrzeitFolgetagBeiBootshausReservierung();
        if (r.isFolgeTagNachUhrzeit(maximaleEndZeit + ":00:00")) {
          throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
              International
                  .getString("Für das Bootshaus bitte täglich einzelne Reservierungen eintragen. " +
                      "Es entstehen separate Nutzungsentgelte bei Reservierung nach "
                      + maximaleEndZeit + " Uhr am Folgetag."),
              Thread.currentThread().getStackTrace());
        }
      }

      if (r.isWeeklyReservationType() && r.getDaysOfWeekWithFallback().length() == 0) {
        throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
            International.getString("Bitte Wochentag eingeben"),
            Thread.currentThread().getStackTrace());
      }
      assertFieldNotEmpty(record, BoatReservationRecord.DATEFROM);
      assertFieldNotEmpty(record, BoatReservationRecord.TIMEFROM);
      assertFieldNotEmpty(record, BoatReservationRecord.TIMETO);
      if (r.getType().equals(BoatReservationRecord.TYPE_ONETIME)) {
        assertFieldNotEmpty(record, BoatReservationRecord.DATETO);
      }
      if (!IGNORE_RESERVATION_CONFLICTS.get()) {
        List<BoatReservationRecord> conflicts = findConflictingReservations(r);
        if (!conflicts.isEmpty()) {
          throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
              getReservationConflictsDescription(conflicts),
              Thread.currentThread().getStackTrace());
        }
      }
      if (r.getType().equals(BoatReservationRecord.TYPE_ONETIME) &&
          r.getDayOfWeek() != null) {
        r.setDayOfWeek(null);
        r.setDaysOfWeek(null);
        r.setWeekInterval(1);
      }
    }
  }

  private List<DataTypeDate> getListOfDates(DataTypeDate dateFrom, DataTypeDate dateTo) {
    DataTypeDate myDateFrom = dateFrom;
    DataTypeDate myDateTo = dateTo;

    List<DataTypeDate> datumListe = new ArrayList<>();
    DataTypeDate myDate = new DataTypeDate(myDateFrom);
    while (myDate.isBeforeOrEqual(myDateTo)) {
      datumListe.add(new DataTypeDate(myDate));
      myDate.addDays(1);
    }
    return datumListe;
  }

  private boolean hasWeeklyDateOverlap(BoatReservationRecord a, BoatReservationRecord b) {
    DataTypeDate dateFrom = maxDate(getWeeklyDateFrom(a), getWeeklyDateFrom(b));
    DataTypeDate dateTo = minDate(getWeeklyDateTo(a, dateFrom), getWeeklyDateTo(b, dateFrom));
    for (DataTypeDate day = new DataTypeDate(dateFrom);
        day.isBeforeOrEqual(dateTo);
        day.addDays(1)) {
      if (a.isWeeklyReservationOnDate(day) && b.isWeeklyReservationOnDate(day)) {
        return true;
      }
    }
    return false;
  }

  private DataTypeDate getWeeklyDateFrom(BoatReservationRecord r) {
    DataTypeDate dateFrom = r.getDateFrom();
    if (dateFrom != null && dateFrom.isSet()) {
      return dateFrom;
    }
    dateFrom = DataTypeDate.today();
    dateFrom.addDays(-30);
    return dateFrom;
  }

  private DataTypeDate getWeeklyDateTo(BoatReservationRecord r, DataTypeDate dateFrom) {
    DataTypeDate dateTo = r.getDateTo();
    if (dateTo != null && dateTo.isSet()) {
      return dateTo;
    }
    dateTo = new DataTypeDate(dateFrom);
    dateTo.addDays(4 * 365);
    return dateTo;
  }

  private DataTypeDate maxDate(DataTypeDate a, DataTypeDate b) {
    return (a.isAfter(b) ? a : b);
  }

  private DataTypeDate minDate(DataTypeDate a, DataTypeDate b) {
    return (a.isBefore(b) ? a : b);
  }

}
