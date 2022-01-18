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
import java.util.Hashtable;
import java.util.UUID;

import de.nmichael.efa.Daten;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataKeyIterator;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.MetaData;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.ex.EfaModifyException;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;
import de.nmichael.efa.util.ProgressTask;

// @i18n complete

public class Destinations extends StorageObject {

  public static final String DATATYPE = "efa2destinations";
  public DestinationRecord staticDestinationRecord;

  public Destinations(int storageType,
      String storageLocation,
      String storageUsername,
      String storagePassword,
      String storageObjectName) {
    super(storageType, storageLocation, storageUsername, storagePassword, storageObjectName,
        DATATYPE,
        International.getString("Ziele") + " / " +
            International.getString("Strecken"));
    DestinationRecord.initialize();
    staticDestinationRecord = (DestinationRecord) createNewRecord();
    dataAccess.setMetaData(MetaData.getMetaData(DATATYPE));
  }

  @Override
  public DataRecord createNewRecord() {
    return new DestinationRecord(this, MetaData.getMetaData(DATATYPE));
  }

  public DestinationRecord createDestinationRecord(UUID id) {
    DestinationRecord r = new DestinationRecord(this, MetaData.getMetaData(DATATYPE));
    r.setId(id);
    return r;
  }

  public DestinationRecord getDestination(UUID id, long validAt) {
    try {
      return (DestinationRecord) data().getValidAt(DestinationRecord.getKey(id, validAt), validAt);
    } catch (Exception e) {
      Logger.logdebug(e);
      return null;
    }
  }

  // find a record being valid at the specified time
  public DestinationRecord getDestination(String destinationName, long validAt) {
    return getDestination(destinationName, null, validAt);
  }

  public DestinationRecord getDestination(String destinationName, String onlyForBoathouseName,
      long validAt) {
    try {
      DataKey[] keys = data().getByFields(
          staticDestinationRecord.getQualifiedNameFields(),
          staticDestinationRecord.getQualifiedNameValues(destinationName), validAt);
      if (keys == null || keys.length < 1) {
        return null;
      }
      for (DataKey key : keys) {
        DestinationRecord r = (DestinationRecord) data().get(key);
        if (r.isValidAt(validAt)) {
          if (onlyForBoathouseName != null
              && !onlyForBoathouseName.equals(r.getOnlyInBoathouseName())) {
            continue;
          }
          return r;
        }
      }
      return null;
    } catch (Exception e) {
      Logger.logdebug(e);
      return null;
    }
  }

  // find any record being valid at least partially in the specified range
  public DestinationRecord getDestination(String destinationName, long validFrom, long validUntil,
      long preferredValidAt) {
    try {
      DataKey[] keys = data().getByFields(
          staticDestinationRecord.getQualifiedNameFields(),
          staticDestinationRecord.getQualifiedNameValues(destinationName));
      if (keys == null || keys.length < 1) {
        return null;
      }
      DestinationRecord candidate = null;
      for (DataKey key : keys) {
        DestinationRecord r = (DestinationRecord) data().get(key);
        if (r != null) {
          if (r.isInValidityRange(validFrom, validUntil)) {
            candidate = r;
            if (preferredValidAt >= r.getValidFrom() && preferredValidAt < r.getInvalidFrom()) {
              return r;
            }
          }
        }
      }
      return candidate;
    } catch (Exception e) {
      Logger.logdebug(e);
      return null;
    }
  }

  public boolean isDestinationDeleted(UUID destinationId) {
    try {
      DataRecord[] records = data().getValidAny(DestinationRecord.getKey(destinationId, -1));
      if (records != null && records.length > 0) {
        return records[0].getDeleted();
      }
    } catch (Exception e) {
      Logger.logdebug(e);
    }
    return false;
  }

  @Override
  public void preModifyRecordCallback(DataRecord record,
      boolean add, boolean update, boolean delete)
      throws EfaModifyException {
    if (add || update) {
      assertFieldNotEmpty(record, DestinationRecord.ID);
      assertFieldNotEmpty(record, DestinationRecord.NAME);
      if (Daten.efaConfig.getValueUseFunctionalityRowingBerlin() &&
          getProject().getBoathouseAreaID() > 0) {
        DestinationRecord dr = ((DestinationRecord) record);
        if (dr.getStartIsBoathouse() && dr.getDestinationAreas() != null &&
            dr.getDestinationAreas().findZielbereich(getProject().getBoathouseAreaID()) >= 0) {
          throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
              "Eigener Zielbereich " + getProject().getBoathouseAreaID()
                  + " bei Fahrten ab eigenem Bootshaus nicht erlaubt.",
              Thread.currentThread().getStackTrace());

        }
      }
    }
    if (delete) {
      String[] logbooks = getProject().getAllLogbookNames();
      for (int i = 0; logbooks != null && i < logbooks.length; i++) {
        assertNotReferenced(record, getProject().getLogbook(logbooks[i], false),
            new String[] { LogbookRecord.DESTINATIONID });
      }
    }
  }

  public ProgressTask getMergeDestinationsProgressTask(DataKey mainKey, DataKey[] mergeKeys) {
    return new Destinations.MergeDestinationsProgressTask(this, mainKey, mergeKeys);
  }

  class MergeDestinationsProgressTask extends ProgressTask {

    private Destinations destinations;
    private UUID mainID;
    private UUID[] mergeIDs;
    private int absoluteWork = 100;
    private int errorCount = 0;
    private int warningCount = 0;
    private int updateCount = 0;

    public MergeDestinationsProgressTask(Destinations destinations, DataKey mainKey,
        DataKey[] mergeKeys) {
      this.destinations = destinations;
      mainID = (UUID) mainKey.getKeyPart1();
      mergeIDs = new UUID[mergeKeys.length];
      for (int i = 0; i < mergeIDs.length; i++) {
        mergeIDs[i] = (UUID) mergeKeys[i].getKeyPart1();
      }
    }

    @Override
    public int getAbsoluteWork() {
      return absoluteWork;
    }

    @Override
    public String getSuccessfullyDoneMessage() {
      return International.getString("Datensätze erfolgreich zusammengefügt.") +
          (errorCount > 0 || warningCount > 0
              ? "\n[" + errorCount + " ERRORS, "
                  + warningCount + " WARNINGS]"
              : "");
    }

    private boolean isIdToBeMerged(UUID id) {
      if (id == null) {
        return false;
      }
      boolean found = false;
      for (UUID mergeID : mergeIDs) {
        if (id.equals(mergeID)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public void run() {
      setRunning(true);
      logInfo(International.getString("Datensätze zusammenfügen") + " ...\n");
      try {
        Project p = destinations.getProject();
        super.resultSuccess = false;

        // Search Logbooks
        String[] logbookNames = p.getAllLogbookNames();
        absoluteWork = (logbookNames != null ? logbookNames.length : 0) + 2;
        int workDone = 0;
        if (logbookNames != null) {
          for (String logbookName : logbookNames) {
            logInfo("Searching logbook " + logbookName + " ...\n");
            Logbook logbook = p.getLogbook(logbookName, false);
            DataKeyIterator it = logbook.data().getStaticIterator();
            for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
              LogbookRecord r = (LogbookRecord) logbook.data().get(k);
              if (r != null) {
                boolean changed = false;
                if (isIdToBeMerged(r.getDestinationId())) {
                  r.setDestinationId(mainID);
                  changed = true;
                }
                if (changed) {
                  logInfo("Updating record " + r.getQualifiedName() + " ...\n");
                  logbook.data().update(r);
                  updateCount++;
                }
              }
            }
            setCurrentWorkDone(++workDone);
          }
        }

        // Search Boats
        logInfo("Searching Boats ...\n");
        setCurrentWorkDone(++workDone);

        // Merge Destination and delete old Destinations
        logInfo("Merging Records ...\n");
        DataRecord[] mainDestination = destinations.data().getValidAny(
            DestinationRecord.getKey(mainID, 0));
        ArrayList<DataRecord> mergedDestinations = new ArrayList<DataRecord>();
        for (UUID mergeID : mergeIDs) {
          DataRecord[] mergeDestination = destinations.data().getValidAny(
              DestinationRecord.getKey(mergeID, 0));
          for (int j = 0; mergeDestination != null && j < mergeDestination.length; j++) {
            mergedDestinations.add(mergeDestination[j]);
          }
        }
        long validFrom = Long.MAX_VALUE;
        long invalidFrom = Long.MIN_VALUE;
        for (int i = 0; mainDestination != null && i < mainDestination.length; i++) {
          if (mainDestination[i].getValidFrom() < validFrom) {
            validFrom = mainDestination[i].getValidFrom();
          }
          if (mainDestination[i].getInvalidFrom() > invalidFrom) {
            invalidFrom = mainDestination[i].getInvalidFrom();
          }
        }
        Hashtable<Long, DataRecord> validStart = new Hashtable<Long, DataRecord>();
        for (int i = 0; i < mergedDestinations.size(); i++) {
          DestinationRecord r = (DestinationRecord) mergedDestinations.get(i).cloneRecord();
          if (r.getValidFrom() < validFrom && validStart.get(r.getValidFrom()) == null) {
            r.setId(mainID);
            logInfo("Merging " + r.getQualifiedName() + " (" + r.getValidRangeString() + ") ...\n");
            destinations.data().addValidAt(r, Long.MIN_VALUE);
            validStart.put(r.getValidFrom(), r);
            validFrom = r.getValidFrom();
          } else if (r.getInvalidFrom() > invalidFrom && validStart.get(invalidFrom) == null) {
            r.setId(mainID);
            logInfo("Merging " + r.getQualifiedName() + " (" + r.getValidRangeString() + ") ...\n");
            destinations.data().addValidAt(r, Long.MAX_VALUE);
            validStart.put(invalidFrom, r);
            invalidFrom = r.getInvalidFrom();
          }
        }
        // Deleting merged Records
        logInfo("Deleting merged Records ...\n");
        for (int i = 0; i < mergedDestinations.size(); i++) {
          DestinationRecord r = (DestinationRecord) mergedDestinations.get(i);
          logInfo("Deleting " + r.getQualifiedName() + " (" + r.getValidRangeString() + ") ...\n");
          destinations.data().delete(r.getKey());
        }
        setCurrentWorkDone(++workDone);
        super.resultSuccess = true;

      } catch (Exception e) {
        errorCount++;
        this.logInfo("\n" + International.getString("Fehler") + ": " + e.getMessage());
        this.logInfo("\n" + e.toString());
        Logger.logdebug(e);
      }
      this.logInfo("\n"
          + International.getMessage("{count} Datensätze wurden aktualisiert.", updateCount));
      this.logInfo("\n\n" + International.getMessage("{count} Fehler.", errorCount));
      this.logInfo("\n" + International.getMessage("{count} Warnungen.", warningCount));
      setDone();
    }
  }

}
