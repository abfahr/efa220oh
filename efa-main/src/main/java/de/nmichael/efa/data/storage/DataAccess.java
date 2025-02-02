/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.data.storage;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.BackupMetaDataItem;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;

// @i18n complete

public abstract class DataAccess implements IDataAccess {

  protected StorageObject persistence;
  protected String storageLocation;
  protected String storageObjectName;
  protected String storageObjectType;
  protected String storageObjectDescription;
  protected String storageUsername;
  protected String storagePassword;
  protected String storageObjectVersion;

  protected final LinkedHashMap<String, Integer> fieldTypes = new LinkedHashMap<String, Integer>();
  protected String[] keyFields;
  protected MetaData meta;
  protected DataRecord referenceRecord;
  protected boolean inOpeningStorageObject = false;
  protected boolean isPreModifyRecordCallbackEnabled = true;

  public static IDataAccess createDataAccess(StorageObject persistence,
      int type,
      String storageLocation,
      String storageUsername,
      String storagePassword,
      String storageObjectName,
      String storageObjectType,
      String storageObjectDescription) {
    IDataAccess dataAccess = null;
    switch (type) {
      case IDataAccess.TYPE_FILE_XML:
        dataAccess = new XMLFile(storageLocation, storageObjectName,
            storageObjectType, storageObjectDescription);
        dataAccess.setPersistence(persistence);
        return dataAccess;
      case IDataAccess.TYPE_DB_SQL:
        break; // (P6) TYPE_DB_SQL not yet implemented
      case IDataAccess.TYPE_EFA_REMOTE:
        dataAccess = new RemoteEfaClient(storageLocation, storageUsername,
            storagePassword, storageObjectName, storageObjectType, storageObjectDescription);
        dataAccess.setPersistence(persistence);
        return dataAccess;
    }
    Logger.log(Logger.ERROR, Logger.MSG_DATA_DATAACCESS,
        "DataAccess for " + storageObjectName + "." + storageObjectType + " (type " + type +
        ") is null");
    return null;
  }

  @Override
  public void setPersistence(StorageObject persistence) {
    this.persistence = persistence;
  }

  @Override
  public StorageObject getPersistence() {
    return persistence;
  }

  @Override
  public void setStorageLocation(String location) {
    this.storageLocation = location;
  }

  @Override
  public String getStorageLocation() {
    return this.storageLocation;
  }

  @Override
  public void setStorageObjectName(String name) {
    this.storageObjectName = name;
  }

  @Override
  public String getStorageObjectName() {
    return this.storageObjectName;
  }

  @Override
  public void setStorageObjectType(String type) {
    this.storageObjectType = type;
  }

  @Override
  public String getStorageObjectType() {
    return this.storageObjectType;
  }

  @Override
  public void setStorageObjectDescription(String description) {
    this.storageObjectDescription = description;
  }

  @Override
  public String getStorageObjectDescription() {
    return this.storageObjectDescription;
  }

  @Override
  public void setStorageUsername(String username) {
    this.storageUsername = username;
  }

  @Override
  public void setStoragePassword(String password) {
    this.storagePassword = password;
  }

  @Override
  public String getStorageUsername() {
    return this.storageUsername;
  }

  @Override
  public String getStoragePassword() {
    return this.storagePassword;
  }

  @Override
  public String getStorageObjectVersion() {
    return this.storageObjectVersion;
  }

  @Override
  public void setStorageObjectVersion(String version) {
    this.storageObjectVersion = version;
  }

  @Override
  public void registerDataField(String fieldName, int dataType) throws EfaException {
    if (fieldTypes.containsKey(fieldName)) {
      throw new EfaException(Logger.MSG_DATA_GENERICEXCEPTION, getUID()
          + ": Field Name is already in use: " + fieldName, Thread.currentThread().getStackTrace());
    }
    synchronized (fieldTypes) { // fieldTypes used for synchronization of fieldTypes and keyFields
      // as well
      fieldTypes.put(fieldName, dataType);
    }
  }

  @Override
  public void setKey(String[] fieldNames) throws EfaException {
    synchronized (fieldTypes) { // fieldTypes used for synchronization of fieldTypes and keyFields
      // as well
      for (String fieldName : fieldNames) {
        getFieldType(fieldName); // just to check for existence
      }
      this.keyFields = fieldNames;
    }
  }

  @Override
  public void setMetaData(MetaData meta) {
    this.meta = meta;
    try {
      for (int i = 0; i < meta.getNumberOfFields(); i++) {
        registerDataField(meta.getFieldName(i), meta.getFieldType(i));
      }
      setKey(meta.getKeyFields());
      String[][] indexFields = meta.getIndices();
      for (String[] indexField : indexFields) {
        createIndex(indexField);
      }
      referenceRecord = persistence.createNewRecord();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public MetaData getMetaData() {
    return meta;
  }

  @Override
  public String[] getKeyFieldNames() {
    String[] names = null;
    synchronized (fieldTypes) { // fieldTypes used for synchronization of fieldTypes and keyFields
      // as well
      names = new String[this.keyFields.length];
      for (int i = 0; i < names.length; i++) {
        names[i] = this.keyFields[i];
      }
    }
    return names;
  }

  @Override
  public String[] getFieldNames() {
    return getFieldNames(true);
  }

  @Override
  public String[] getFieldNames(boolean includingVirtual) {
    synchronized (fieldTypes) { // fieldTypes used for synchronization of fieldTypes and keyFields
      // as well
      String[] keys = new String[fieldTypes.size()];
      fieldTypes.keySet().toArray(keys);
      if (includingVirtual) {
        return keys;
      } else {
        Vector<String> v = new Vector<String>();
        for (String key : keys) {
          if (getMetaData().getFieldType(key) != IDataAccess.DATA_VIRTUAL) {
            v.add(key);
          }
        }
        if (keys.length != v.size()) {
          keys = new String[v.size()];
          for (int i = 0; i < v.size(); i++) {
            keys[i] = v.get(i);
          }
        }
        return keys;
      }
    }
  }

  @Override
  public int getFieldType(String fieldName) throws EfaException {
    Integer i = null;
    synchronized (fieldTypes) { // fieldTypes used for synchronization of fieldTypes and keyFields
      // as well
      i = fieldTypes.get(fieldName);
    }
    if (i == null) {
      throw new EfaException(Logger.MSG_DATA_FIELDDOESNOTEXIST, getUID()
          + ": Field Name does not exist: " + fieldName, Thread.currentThread().getStackTrace());
    }
    return i.intValue();
  }

  @Override
  public DataKey constructKey(DataRecord record) throws EfaException {
    Object v1 = null;
    Object v2 = null;
    Object v3 = null;

    if (keyFields.length >= 1) {
      v1 = (record != null ? record.get(keyFields[0]) : null);
    }
    if (keyFields.length >= 2) {
      v2 = (record != null ? record.get(keyFields[1]) : null);
    }
    if (keyFields.length >= 3) {
      v3 = (record != null ? record.get(keyFields[2]) : null);
    }

    return new DataKey(v1, v2, v3);
  }

  @Override
  public DataKey getUnversionizedKey(DataKey key) {
    boolean[] bUnversionized = new boolean[keyFields.length];
    for (int i = 0; i < keyFields.length; i++) {
      bUnversionized[i] = !keyFields[i].equals(DataRecord.VALIDFROM);
    }
    return new DataKey(key, bUnversionized); // this is the corresponding "unversionized" key (i.e.
    // key with only unversionized fields)
  }

  @Override
  public String getTypeName(int type) {
    switch (type) {
      case DATA_STRING:
        return "STRING";
      case DATA_INTEGER:
        return "INTEGER";
      case DATA_LONGINT:
        return "LONGINT";
      case DATA_DOUBLE:
        return "DOUBLE";
      case DATA_DECIMAL:
        return "DECIMAL";
      case DATA_DISTANCE:
        return "DISTANCE";
      case DATA_BOOLEAN:
        return "BOOLEAN";
      case DATA_DATE:
        return "DATE";
      case DATA_TIME:
        return "TIME";
      case DATA_UUID:
        return "UUID";
      case DATA_INTSTRING:
        return "INTSTRING";
      case DATA_PASSWORDH:
        return "PASSWORDH";
      case DATA_PASSWORDC:
        return "PASSWORDC";
      case DATA_LIST_STRING:
        return "LIST_STRING";
      case DATA_LIST_INTEGER:
        return "LIST_INTEGER";
      case DATA_LIST_UUID:
        return "LIST_UUID";
      case DATA_VIRTUAL:
        return "VIRTUAL";
      default:
        return "UNKNOWN";
    }
  }

  @Override
  public boolean inOpeningStorageObject() {
    return this.inOpeningStorageObject;
  }

  @Override
  public void setInOpeningStorageObject(boolean inOpening) {
    inOpeningStorageObject = inOpening;
  }

  @Override
  public void setPreModifyRecordCallbackEnabled(boolean enabled) {
    this.isPreModifyRecordCallbackEnabled = enabled;
  }

  @Override
  public boolean isPreModifyRecordCallbackEnabled() {
    return this.isPreModifyRecordCallbackEnabled
        && (Daten.efaConfig == null || Daten.efaConfig.getValueDataPreModifyRecordCallbackEnabled());
  }

  @Override
  public void saveToXmlFile(String filename) throws EfaException {
    if (!isStorageObjectOpen()) {
      throw new EfaException(Logger.MSG_DATA_SAVEFAILED, LogString.fileWritingFailed(filename,
          storageLocation, "Storage Object is not open"), Thread.currentThread().getStackTrace());
    }
    try {
      FileOutputStream out = new FileOutputStream(filename, false);
      XMLFile.writeFile(this, out);
      out.close();
    } catch (Exception e) {
      throw new EfaException(Logger.MSG_DATA_SAVEFAILED, LogString.fileWritingFailed(filename,
          storageLocation, e.toString()), Thread.currentThread().getStackTrace());
    }
  }

  @Override
  public BackupMetaDataItem saveToZipFile(String dir, ZipOutputStream zipOut) throws EfaException {
    if (!isStorageObjectOpen()) {
      throw new EfaException(Logger.MSG_DATA_SAVEFAILED, LogString.fileWritingFailed("ZIP Buffer",
          storageLocation, "Storage Object is not open"), Thread.currentThread().getStackTrace());
    }
    if (dir.length() > 0 && !dir.endsWith(Daten.fileSep)) {
      dir += Daten.fileSep;
    }
    String zipFileEntry = dir + getStorageObjectName() + "." + getStorageObjectType();
    long lock = -1;
    BackupMetaDataItem metaData = null;
    try {
      ZipEntry entry = new ZipEntry(zipFileEntry);
      zipOut.putNextEntry(entry);
      lock = acquireGlobalLock();
      metaData = new BackupMetaDataItem(getStorageObjectName(),
          getStorageObjectType(),
          zipFileEntry,
          getStorageObjectDescription(),
          getNumberOfRecords(),
          getSCN());
      XMLFile.writeFile(this, zipOut);
    } catch (Exception e) {
      throw new EfaException(Logger.MSG_DATA_SAVEFAILED,
          LogString.fileWritingFailed("ZIP Buffer", storageLocation, e.toString()), Thread
          .currentThread().getStackTrace());
    } finally {
      if (lock >= 0) {
        releaseGlobalLock(lock);
      }
    }
    return metaData;
  }

  // this method does NOT set the SCN to the value of the archive.
  // the SCN is always increasing!!
  @Override
  public synchronized void copyFromDataAccess(IDataAccess source) throws EfaException {
    truncateAllData();
    try {
      DataKeyIterator it = source.getStaticIterator();
      ArrayList<DataRecord> recordList = new ArrayList<DataRecord>();
      DataKey k = it.getFirst();
      while (k != null) {
        recordList.add(source.get(k));
        k = it.getNext();
      }

      setInOpeningStorageObject(true); // don't update LastModified Timestamps, don't increment SCN,
      // don't check assertions!
      if (recordList.size() > 0) {
        addAll(recordList.toArray(new DataRecord[0]), -1);
      }
    } catch (Exception e) {
      throw new EfaException(Logger.MSG_DATA_COPYFROMDATAACCESSFAILED, getUID() +
          ": Restore from DataAccess failed", Thread.currentThread().getStackTrace());
    } finally {
      setInOpeningStorageObject(false);
    }
  }
}
