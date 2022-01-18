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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import de.nmichael.efa.Daten;
import de.nmichael.efa.data.Logbook;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.data.PersonRecord;
import de.nmichael.efa.data.Persons;
import de.nmichael.efa.gui.ProgressDialog;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;
import de.nmichael.efa.util.ProgressTask;
import de.nmichael.efa.util.XmlHandler;

public class DataImport extends ProgressTask {

  public static final String IMPORTMODE_ADD = "ADD"; // import as new record; fail for duplicates
  // (also for duplicate versionized records with different validity)
  public static final String IMPORTMODE_UPD = "UPDATE"; // update existing record; fail if record
  // doesn't exist (for versionized: if no version exists)
  public static final String IMPORTMODE_ADDUPD = "ADD_OR_UPDATE"; // add, or if duplicate, update

  // Import Options for Logbook Import
  public static final String ENTRYNO_DUPLICATE_SKIP = "DUPLICATE_SKIP"; // if duplicate EntryId,
  // skip entry
  public static final String ENTRYNO_DUPLICATE_ADDEND = "DUPLICATE_ADDEND"; // if duplicate EntryId,
  // add entry with new EntryId at end
  public static final String ENTRYNO_ALWAYS_ADDEND = "ALWAYS_ADDEND"; // add all entries with new
  // EntryId at end

  // only relevant for versionized storage objects
  public static final String UPDMODE_UPDATEVALIDVERSION = "UPDVERSION"; // update version which is
  // valid at specified timestamp; fail if no version is valid
  public static final String UPPMODE_CREATENEWVERSION = "NEWVERSION"; // always create a version at
  // specified timestamp; fail if version for exact same timestamp exists

  private StorageObject storageObject;
  private IDataAccess dataAccess;
  private String[] fields;
  private String[] keyFields;
  private String overrideKeyField;
  private boolean versionized;
  private String filename;
  private String encoding;
  private char csvSeparator;
  private char csvQuotes;
  private String importMode;
  private String logbookEntryNoHandling;
  private long validAt;
  private String updMode;
  private int importCount = 0;
  private int changeCount = 0;
  private int newCount = 0;
  private int errorCount = 0;
  private int warningCount = 0;
  private boolean isLogbook = false;

  public DataImport(StorageObject storageObject,
      String filename, String encoding,
      char csvSeparator, char csvQuotes,
      String importMode, String updMode,
      String logbookEntryNoHandling,
      long validAt) {
    super();
    this.storageObject = storageObject;
    this.dataAccess = storageObject.data();
    this.versionized = storageObject.data().getMetaData().isVersionized();
    this.fields = dataAccess.getFieldNames();
    this.keyFields = dataAccess.getKeyFieldNames();
    this.filename = filename;
    this.encoding = encoding;
    this.csvSeparator = csvSeparator;
    this.csvQuotes = csvQuotes;
    this.importMode = importMode;
    this.logbookEntryNoHandling = logbookEntryNoHandling;
    this.validAt = validAt;
    this.updMode = updMode;
    this.isLogbook = storageObject.data().getStorageObjectType().equals(Logbook.DATATYPE);
  }

  public static boolean isXmlFile(String filename) {
    try {
      BufferedReader f = new BufferedReader(new FileReader(filename));
      String s = f.readLine();
      boolean xml = (s != null && s.toLowerCase().startsWith("<?xml"));
      f.close();
      return xml;
    } catch (Exception eignore) {
      return false;
    }
  }

  private Vector<String> splitFields(String s) {
    Vector<String> fields = new Vector<String>();
    boolean inQuote = false;
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < s.length(); i++) {
      if (!inQuote && s.charAt(i) == csvQuotes) {
        inQuote = true;
        continue;
      }
      if (inQuote && s.charAt(i) == csvQuotes) {
        inQuote = false;
        continue;
      }
      if (!inQuote && s.charAt(i) == csvSeparator) {
        fields.add(buf.toString());
        buf = new StringBuffer();
        continue;
      }
      buf.append(s.charAt(i));
    }
    fields.add(buf.toString());
    return fields;
  }

  private void logImportFailed(DataRecord r, String msg, Exception e) {
    if (e != null) {
      Logger.logdebug(e);
    }
    logInfo("\nERROR: " + LogString.operationFailed(
        International.getMessage("Import von Datensatz {record}", r.toString()), msg));
    errorCount++;
  }

  private void logImportWarning(DataRecord r, String msg) {
    logInfo("\nWARNING: " + msg + ": " + r.toString());
    warningCount++;
  }

  private long getValidFrom(DataRecord r) {
    long rv = r.getValidFrom();
    return (rv > 0 ? rv : validAt);
  }

  private long getInvalidFrom(DataRecord r) {
    long rv = r.getInvalidFrom();
    return (rv > 0 && rv < Long.MAX_VALUE ? rv : -1);
  }

  private void addRecord(DataRecord r) {
    try {
      if (r.getDeleted()) {
        return;
      }
      if (versionized) {
        long myValidAt = getValidFrom(r);
        dataAccess.addValidAt(r, myValidAt);
        newCount++;
        setCurrentWorkDone(++importCount);
      } else {
        dataAccess.add(r);
        newCount++;
        setCurrentWorkDone(++importCount);
      }
    } catch (Exception e) {
      logImportFailed(r, e.toString(), e);
    }
  }

  private void updateRecord(DataRecord r, ArrayList<String> fieldsInImport) {
    try {
      DataRecord rOrig = (versionized
          ? dataAccess.getValidAt(r.getKey(), validAt)
          : dataAccess.get(r.getKey()));
      if (rOrig == null) {
        logImportFailed(r, International.getString(
            "Keine gültige Version des Datensatzes gefunden."), null);
        return;
      }

      // has the import record an InvalidFrom field?
      long invalidFrom = (versionized ? getInvalidFrom(r) : -1);
      if (invalidFrom <= rOrig.getValidFrom()) {
        invalidFrom = -1;
      }
      boolean changed = false;

      boolean isPerson = (rOrig instanceof PersonRecord);
      for (int i = 0; i < fields.length; i++) {
        String fieldName = fields[i];
        Object objValue = r.get(fieldName);
        if ((objValue != null || fieldsInImport.contains(fieldName))
            && !r.isKeyField(fieldName)
            && !fieldName.equals(DataRecord.LASTMODIFIED)
            && !fieldName.equals(DataRecord.VALIDFROM)
            && !fieldName.equals(DataRecord.INVALIDFROM)
            && !fieldName.equals(DataRecord.INVISIBLE)
            && !fieldName.equals(DataRecord.DELETED)) {

          Object oBefore = rOrig.get(fieldName);
          if (isPerson) {
            PersonRecord personBisher = (PersonRecord) rOrig;
            switch (fieldName) {
              case PersonRecord.EMAIL:
              case PersonRecord.ISALLOWEDEMAIL:
                // kein Update von Sewobe, falls Mitglied Nutzung verboten hat
                // es sei denn, Person hat nun erstmalig Email TODO 2021-02-08 abf
                if (!personBisher.isErlaubtEmail()) {
                  continue;
                }
                break;
              case PersonRecord.FESTNETZ1:
              case PersonRecord.HANDY2:
                // kein Update von Sewobe, falls Mitglied bisherige Nummer freigegeben hat.
                if (personBisher.isErlaubtTelefon()) {
                  continue;
                }
                break;
              case PersonRecord.LASTNAME:
              case PersonRecord.FIRSTNAME:
              case PersonRecord.FIRSTLASTNAME:
                // kein Update von Sewobe, falls Mitglied bereits Namen geändert hat.
                if (personBisher.hatSchreibweiseNameGeaendert()) {
                  continue;
                }
              default:
            }
          }

          rOrig.set(fieldName, objValue);
          if ((objValue != null && !objValue.equals(oBefore)) ||
              (objValue == null && oBefore != null)) {
            changed = true;
          }
        }
      }

      if (invalidFrom <= 0) {
        long myValidAt = getValidFrom(r);
        if (!versionized || updMode.equals(UPDMODE_UPDATEVALIDVERSION)
            || rOrig.getValidFrom() == myValidAt) {
          if (changed) {
            dataAccess.update(rOrig);
            changeCount++;
          }
          setCurrentWorkDone(++importCount);
        }
        if (versionized && updMode.equals(UPPMODE_CREATENEWVERSION)
            && rOrig.getValidFrom() != myValidAt) {
          if (changed) {
            dataAccess.addValidAt(rOrig, myValidAt);
            changeCount++;
          }
          setCurrentWorkDone(++importCount);
        }
      } else {
        dataAccess.changeValidity(rOrig, rOrig.getValidFrom(), invalidFrom);
        changeCount++;
        setCurrentWorkDone(++importCount);
      }
    } catch (Exception e) {
      logImportFailed(r, e.toString(), e);
    }
  }

  private boolean importRecord(DataRecord r, ArrayList<String> fieldsInImport) {
    try {
      if (Logger.isDebugLogging()) {
        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_DATA,
            "importing " + r.toString());
      }
      DataRecord[] otherVersions = null;

      if (importMode.equals(IMPORTMODE_ADD) &&
          logbookEntryNoHandling != null &&
          logbookEntryNoHandling.equals(ENTRYNO_ALWAYS_ADDEND)) {
        // determine new EntryId for logbook
        r.set(keyFields[0], ((Logbook) storageObject).getNextEntryNo());
      }

      if (isLogbook
          && (importMode.equals(IMPORTMODE_ADD) || importMode.equals(IMPORTMODE_ADDUPD))) {
        LogbookRecord lr = ((LogbookRecord) r);
        if (lr.getEntryId() == null || !lr.getEntryId().isSet()
            || lr.getEntryId().toString().length() == 0) {
          r.set(keyFields[0], ((Logbook) storageObject).getNextEntryNo());
        }
      }

      DataKey<?, ?, ?> key = r.getKey();
      if (key.getKeyPart1() == null || overrideKeyField != null) {
        // first key field is *not* set, or we're overriding the default key field
        DataKey<?, ?, ?>[] keys = null;
        if (overrideKeyField == null) {
          // -> search for record by QualifiedName
          keys = dataAccess.getByFields(r.getQualifiedNameFields(),
              r.getQualifiedNameValues(r.getQualifiedName()),
              (versionized ? validAt : -1));
        } else {
          // -> search for record by user-specified key field
          keys = dataAccess.getByFields(new String[] { overrideKeyField },
              new String[] { r.getAsString(overrideKeyField) },
              (versionized ? validAt : -1));
        }
        if (keys != null && keys.length > 0) {
          for (int i = 0; i < keyFields.length; i++) {
            if (!keyFields[i].equals(DataRecord.VALIDFROM)) {
              r.set(keyFields[i], keys[0].getKeyPart(i));
            }
          }
        } else {
          for (int i = 0; i < keyFields.length; i++) {
            if (!keyFields[i].equals(DataRecord.VALIDFROM)
                && r.get(keyFields[i]) == null) {
              if (dataAccess.getMetaData().getFieldType(keyFields[i]) == IDataAccess.DATA_UUID) {
                r.set(keyFields[i], UUID.randomUUID());
              } else {
                logImportFailed(r, "KeyField(s) not set", null);
                return false;
              }
            }
          }
        }
      }
      key = r.getKey();

      if (versionized) {
        otherVersions = dataAccess.getValidAny(key);
      } else {
        DataRecord r1 = dataAccess.get(key);
        otherVersions = (r1 != null ? new DataRecord[] { r1 } : null);
      }

      if (importMode.equals(IMPORTMODE_ADD) &&
          otherVersions != null && otherVersions.length > 0 &&
          logbookEntryNoHandling != null &&
          logbookEntryNoHandling.equals(ENTRYNO_DUPLICATE_ADDEND)) {
        r.set(keyFields[0], ((Logbook) storageObject).getNextEntryNo());
        otherVersions = null;
      }

      if (importMode.equals(IMPORTMODE_ADD)) {
        if (otherVersions != null && otherVersions.length > 0) {
          logImportFailed(r, International.getString("Datensatz existiert bereits"), null);
          return false;
        } else {
          addRecord(r);
          return true;
        }
      }
      if (importMode.equals(IMPORTMODE_UPD)) {
        if (otherVersions == null || otherVersions.length == 0) {
          logImportFailed(r, International.getString("Datensatz nicht gefunden"), null);
          return false;
        } else {
          updateRecord(r, fieldsInImport);
          return true;
        }
      }
      if (importMode.equals(IMPORTMODE_ADDUPD)) {
        if (otherVersions != null && otherVersions.length > 0) {
          updateRecord(r, fieldsInImport);
        } else {
          addRecord(r);
        }
        return true;
      }
    } catch (Exception e) {
      logImportFailed(r, e.getMessage(), e);
    }
    return false;
  }

  public int runXmlImport() {
    DataImportXmlParser responseHandler = null;
    try {
      XMLReader parser = EfaUtil.getXMLReader();
      responseHandler = new DataImportXmlParser(this, dataAccess);
      parser.setContentHandler(responseHandler);
      parser.parse(new InputSource(new FileInputStream(filename)));
    } catch (Exception e) {
      logInfo(e.toString());
      errorCount++;
      Logger.log(e);
      if (Daten.isGuiAppl()) {
        Dialog.error(e.toString());
      }
    }
    return (responseHandler != null ? responseHandler.getImportedRecordsCount() : 0);
  }

  public int runCsvImport() {
    int count = 0;
    try {
      int linecnt = 0;
      String[] header = null;
      ArrayList<String> fieldsInImport = new ArrayList<String>();
      BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(filename),
          encoding));
      String s;
      DataRecord dummyRecord = storageObject.createNewRecord();
      boolean isPersons = (storageObject instanceof Persons);
      if (isPersons) {
        overrideKeyField = PersonRecord.MEMBERSHIPNO;
      }
      while ((s = f.readLine()) != null) {
        s = s.trim();
        if (s.length() == 0) {
          continue;
        }
        Vector<String> fields = splitFields(s);
        if (fields.size() > 0) {
          if (linecnt == 0) {
            // header
            header = new String[fields.size()];
            for (int i = 0; i < fields.size(); i++) {
              header[i] = fields.get(i);
              if (header[i].startsWith("#") && header[i].endsWith("#") && header.length > 2) {
                header[i] = header[i].substring(1, header[i].length() - 1).trim();
                overrideKeyField = header[i];
              }
              String[] equivFields = dummyRecord.getEquivalentFields(header[i]);
              for (String ef : equivFields) {
                fieldsInImport.add(ef);
              }
            }
          } else {
            // fields
            DataRecord r = storageObject.createNewRecord();
            for (int i = 0; i < header.length; i++) {
              String value = (fields.size() > i ? fields.get(i) : null);
              if (value != null && value.length() > 0) {
                String headerField = header[i];
                try {
                  value = value.trim();
                  boolean isSameValue = r.setFromText(headerField, value);
                  if (!isSameValue) {
                    String asText = r.getAsText(headerField);
                    if (!(asText == null && value.equals("null"))) {
                      logImportWarning(r, "Value '" + value + "' for Field '" + headerField
                          + "' corrected to '" + asText + "'");
                    }
                  }
                } catch (Exception esetvalue) {
                  logImportWarning(r, "Cannot set value '" + value + "' for Field '" + headerField
                      + "': " + esetvalue.toString());
                }
              }
            }
            if (isPersons && !((PersonRecord) r).isValidMemberOH()) {
              continue;
            }
            if (importRecord(r, fieldsInImport)) {
              count++;
            }
          }
        }
        linecnt++;
      }
      f.close();
    } catch (Exception e) {
      logInfo(e.toString());
      errorCount++;
      Logger.log(e);
      if (Daten.isGuiAppl()) {
        Dialog.error(e.toString());
      }
    }
    return count;
  }

  @Override
  public void run() {
    setRunning(true);
    this.logInfo(International.getMessage("Importiere {type}-Datensätze ...",
        storageObject.getDescription())); // Personen-
    if (isXmlFile(filename)) {
      runXmlImport();
    } else {
      runCsvImport();
    }
    this.logInfo("\n\n"
        + International.getMessage("{count} Datensätze erfolgreich importiert.", importCount));
    this.logInfo("\n" + International.getMessage("{count} Änderungen.", changeCount));
    this.logInfo("\n" + International.getMessage("{count} neue Mitglieder.", newCount));
    this.logInfo("\n" + International.getMessage("{count} Fehler.", errorCount));
    this.logInfo("\n" + International.getMessage("{count} Warnungen.", warningCount));

    // Start the Audit in the background to find any eventual inconsistencies
    (new Audit(Daten.project)).start();

    setDone();
  }

  @Override
  public int getAbsoluteWork() {
    return 100; // just a guess
  }

  @Override
  public String getSuccessfullyDoneMessage() {
    return International.getMessage("{count} Datensätze erfolgreich importiert.",
        "" + changeCount + "+" + newCount + "=" + importCount + " "
            + storageObject.getDescription());
  }

  public void runImport(ProgressDialog progressDialog) {
    this.start();
    if (progressDialog != null) {
      progressDialog.showDialog();
    }
  }

  public class DataImportXmlParser extends XmlHandler {

    private DataImport dataImport;
    private IDataAccess dataAccess;
    private DataRecord record;
    private ArrayList<String> fieldsInImport;
    private boolean textImport;
    private int count = 0;

    public DataImportXmlParser(DataImport dataImport, IDataAccess dataAccess) {
      super(DataExport.FIELD_EXPORT);
      this.dataImport = dataImport;
      this.dataAccess = dataAccess;
    }

    @Override
    public void startElement(String uri, String localName, String qname, Attributes atts) {
      super.startElement(uri, localName, qname, atts);

      if (localName.equals(DataExport.FIELD_EXPORT)) {
        String type = atts.getValue(DataExport.EXPORT_TYPE);
        textImport = (type != null && type.equals(DataExport.EXPORT_TYPE_TEXT));
      } else if (localName.equals(DataRecord.ENCODING_RECORD)) {
        // begin of record
        record = dataAccess.getPersistence().createNewRecord();
        fieldsInImport = new ArrayList<String>();
        return;
      } else {
        if (atts.getValue("key") != null && atts.getValue("key").equalsIgnoreCase("true")) {
          overrideKeyField = localName;
        }
      }

    }

    @Override
    public void endElement(String uri, String localName, String qname) {
      super.endElement(uri, localName, qname);

      if (record != null && localName.equals(DataRecord.ENCODING_RECORD)) {
        // end of record
        if (dataImport.importRecord(record, fieldsInImport)) {
          count++;
        }
        record = null;
        fieldsInImport = null;
      }
      String fieldValue = getFieldValue();
      if (record != null && fieldValue != null) {
        // end of field
        try {
          if (textImport) {
            if (!record.setFromText(fieldName, fieldValue.trim())) {
              dataImport.logImportWarning(record, "Value '" + fieldValue + "' for Field '"
                  + fieldName + "' corrected to '" + record.getAsText(fieldName) + "'");
            }
          } else {
            record.set(fieldName, fieldValue.trim());
          }
          String[] equivFields = record.getEquivalentFields(fieldName);
          for (String f : equivFields) {
            fieldsInImport.add(f);
          }
        } catch (Exception esetvalue) {
          dataImport.logImportWarning(record, "Cannot set value '" + fieldValue + "' for Field '"
              + fieldName + "': " + esetvalue.toString());
        }
      }
    }

    public int getImportedRecordsCount() {
      return count;
    }

  }
}
