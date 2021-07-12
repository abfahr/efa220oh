/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.data.importefa1;

import java.util.UUID;

import de.nmichael.efa.data.BoatRecord;
import de.nmichael.efa.data.Boats;
import de.nmichael.efa.data.Destinations;
import de.nmichael.efa.data.PersonRecord;
import de.nmichael.efa.data.Persons;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.util.International;

public abstract class ImportBase {

  protected ImportTask task;
  int cntWarning = 0;
  int cntError = 0;

  public ImportBase(ImportTask task) {
    this.task = task;
  }

  public abstract String getDescription();

  public abstract boolean runImport();

  protected void logInfo(String s) {
    task.logInfo("INFO    - " + getDescription() + " - " + s + "\n", true, true);
  }

  protected void logDetail(String s) {
    task.logInfo("DETAIL  - " + getDescription() + " - " + s + "\n", false, true);
  }

  protected void logWarning(String s) {
    task.logInfo("WARNING - " + getDescription() + " - " + s + "\n", true, true);
    cntWarning++;
  }

  protected void logError(String s) {
    task.logInfo("ERROR   - " + getDescription() + " - " + s + "\n", true, true);
    cntError++;
  }

  public int getWarningCount() {
    return cntWarning;
  }

  public int getErrorCount() {
    return cntError;
  }

  protected UUID findPerson(Persons persons, String[] IDX, String name, boolean warnIfNotFound,
      long validAt) {
    name = name.trim();
    if (name.length() == 0) {
      return null;
    }
    String[] qname = persons.staticPersonRecord.getQualifiedNameValues(name); // PersonRecord.tryGetNameAndAffix(name);
    return findPerson2(persons, IDX, qname[0], warnIfNotFound, validAt);
  }

  protected UUID findPerson2(Persons persons, String[] IDX, String name,
      boolean warnIfNotFound, long validAt) {
    try {
      DataKey[] keys = persons.data().getByFields(IDX,
          new String[] {
              (name != null && name.length() > 0 ? name : null),
              (null) });
      if (keys != null && keys.length > 0) {
        for (DataKey<?, ?, ?> key : keys) {
          PersonRecord r = (PersonRecord) persons.data().get(key);
          if (r != null && r.isValidAt(validAt)) {
            return (UUID) key.getKeyPart1();
          }
        }
        return null;
      }
    } catch (Exception e) {}
    if (warnIfNotFound) {
      logWarning(International.getMessage("{type_of_entry} {entry} nicht in {list} gefunden.",
          International.getString("Person"),
          name,
          International.getString("Mitglieder")));
    }
    return null;
  }

  protected UUID findBoat(Boats boats, String[] IDX, String name, boolean warnIfNotFound,
      long validAt) {
    name = name.trim();
    if (name.length() == 0) {
      return null;
    }
    String[] qname = BoatRecord.tryGetNameAndAffix(name);
    return findBoat(boats, IDX, qname[0], qname[1], warnIfNotFound, validAt);
  }

  protected UUID findBoat(Boats boats, String[] IDX, String boatName, String nameAffix,
      boolean warnIfNotFound, long validAt) {
    try {
      DataKey[] keys = boats.data().getByFields(IDX,
          new String[] {
              (boatName != null && boatName.length() > 0 ? boatName : null),
              (nameAffix != null && nameAffix.length() > 0 ? nameAffix : null) });
      if (keys != null && keys.length > 0) {
        for (DataKey<?, ?, ?> key : keys) {
          BoatRecord r = (BoatRecord) boats.data().get(key);
          if (r != null && r.isValidAt(validAt) &&
              (nameAffix != null || r.getNameAffix() == null || r.getNameAffix().length() == 0)) {
            return (UUID) key.getKeyPart1();
          }
        }
        return null;
      }
    } catch (Exception e) {}
    if (warnIfNotFound) {
      logWarning(International.getMessage("{type_of_entry} {entry} nicht in {list} gefunden.",
          International.getString("Boot"),
          boatName + (nameAffix != null && nameAffix.length() > 0 ? " (" + nameAffix + ")" : ""),
          International.getString("Bootsliste")));
    }
    return null;
  }

  protected UUID findDestination(Destinations destinations, String[] IDX, String name,
      boolean warnIfNotFound, long validAt) {
    name = name.trim();
    if (name.length() == 0) {
      return null;
    }
    try {
      DataKey[] keys = destinations.data().getByFields(IDX,
          new String[] { name });
      if (keys != null && keys.length > 0) {
        for (DataKey<?, ?, ?> key : keys) {
          DataRecord r = destinations.data().get(key);
          if (r != null && r.isValidAt(validAt)) {
            return (UUID) key.getKeyPart1();
          }
        }
        return null;
      }
    } catch (Exception e) {}
    if (warnIfNotFound) {
      logWarning(International.getMessage("{type_of_entry} {entry} nicht in {list} gefunden.",
          International.getString("Ziel"),
          name,
          International.getString("Zielliste")));
    }
    return null;
  }

}
