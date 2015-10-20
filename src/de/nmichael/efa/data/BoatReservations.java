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

import java.util.UUID;
import java.util.Vector;

import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.MetaData;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.ex.EfaModifyException;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

// @i18n complete

public class BoatReservations extends StorageObject {

  public static final String DATATYPE = "efa2boatreservations";

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
    int reservation = getNextReservation(id);
    if (reservation > 0) {
      BoatReservationRecord r = (BoatReservationRecord) original.cloneRecord();
      r.setBoatId(id);
      r.setReservation(reservation);
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
      Logger.logdebug(e);
      return null;
    }
  }

  public BoatReservationRecord[] getBoatReservations(UUID boatId, long now, long lookAheadMinutes) {
    BoatReservationRecord[] reservations = getBoatReservations(boatId);

    Vector<BoatReservationRecord> activeReservations = new Vector<BoatReservationRecord>();
    for (int i = 0; reservations != null && i < reservations.length; i++) {
      BoatReservationRecord r = reservations[i];
      if (r.getReservationValidInMinutes(now, lookAheadMinutes) >= 0) {
        activeReservations.add(r);
      }
    }

    if (activeReservations.size() == 0) {
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

  @Override
  public void preModifyRecordCallback(DataRecord record, boolean add, boolean update, boolean delete)
      throws EfaModifyException {
    if (add || update) {
      assertFieldNotEmpty(record, BoatReservationRecord.BOATID);
      assertFieldNotEmpty(record, BoatReservationRecord.RESERVATION);
      assertFieldNotEmpty(record, BoatReservationRecord.TYPE);

      BoatReservationRecord r = ((BoatReservationRecord) record);
      if (r.getType().equals(BoatReservationRecord.TYPE_ONETIME)) {
        DataTypeDate today = DataTypeDate.today();

        if (r.getDateFrom().isSet() && r.getDateFrom().isBefore(today)) {
          throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
              International.getString("Das Startdatum muss in der Zukunft liegen."),
              Thread.currentThread().getStackTrace());
        }
        if (r.getDateTo().isSet() && r.getDateTo().isBefore(today)) {
          throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
              International.getString("Das Enddatum muss in der Zukunft liegen."),
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

      BoatReservationRecord[] br = this.getBoatReservations(r.getBoatId());
      for (int i = 0; br != null && i < br.length; i++) {
        if (br[i].getReservation() == r.getReservation()) {
          continue;
        }
        if (!r.getType().equals(br[i].getType())) {
          continue;
        }
        if (r.getType().equals(BoatReservationRecord.TYPE_WEEKLY)) {
          assertFieldNotEmpty(record, BoatReservationRecord.DAYOFWEEK);
          assertFieldNotEmpty(record, BoatReservationRecord.TIMEFROM);
          assertFieldNotEmpty(record, BoatReservationRecord.TIMETO);
          if (!r.getDayOfWeek().equals(br[i].getDayOfWeek())) {
            continue;
          }
          if (DataTypeTime.isRangeOverlap(r.getTimeFrom(), r.getTimeTo(),
              br[i].getTimeFrom(), br[i].getTimeTo())) {
            throw new EfaModifyException(
                Logger.MSG_DATA_MODIFYEXCEPTION,
                International
                    .getString("Die Reservierung überschneidet sich mit einer anderen Reservierung."),
                Thread.currentThread().getStackTrace());

          }
        }
        if (r.getType().equals(BoatReservationRecord.TYPE_ONETIME)) {
          assertFieldNotEmpty(record, BoatReservationRecord.DATEFROM);
          assertFieldNotEmpty(record, BoatReservationRecord.DATETO);
          assertFieldNotEmpty(record, BoatReservationRecord.TIMEFROM);
          assertFieldNotEmpty(record, BoatReservationRecord.TIMETO);
          if (DataTypeDate.isRangeOverlap(r.getDateFrom(),
              r.getTimeFrom(),
              r.getDateTo(),
              r.getTimeTo(),
              br[i].getDateFrom(),
              br[i].getTimeFrom(),
              br[i].getDateTo(),
              br[i].getTimeTo())) {
            throw new EfaModifyException(
                Logger.MSG_DATA_MODIFYEXCEPTION,
                International
                    .getString("Die Reservierung überschneidet sich mit einer anderen Reservierung."),
                Thread.currentThread().getStackTrace());

          }
        }
      }
      if (r.getType().equals(BoatReservationRecord.TYPE_ONETIME) &&
          r.getDayOfWeek() != null) {
        r.setDayOfWeek(null);
      }
    }
  }

}
