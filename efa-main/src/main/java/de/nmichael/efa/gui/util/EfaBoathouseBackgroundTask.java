/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui.util;

import java.awt.Window;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

import de.nmichael.efa.Daten;
import de.nmichael.efa.data.BoatDamageRecord;
import de.nmichael.efa.data.BoatDamages;
import de.nmichael.efa.data.BoatRecord;
import de.nmichael.efa.data.BoatReservationRecord;
import de.nmichael.efa.data.BoatReservations;
import de.nmichael.efa.data.BoatStatus;
import de.nmichael.efa.data.BoatStatusRecord;
import de.nmichael.efa.data.Boats;
import de.nmichael.efa.data.DestinationRecord;
import de.nmichael.efa.data.Destinations;
import de.nmichael.efa.data.Logbook;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.data.MessageRecord;
import de.nmichael.efa.data.Messages;
import de.nmichael.efa.data.PersonRecord;
import de.nmichael.efa.data.Persons;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataKeyIterator;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeIntString;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.gui.EfaBaseFrame;
import de.nmichael.efa.gui.EfaBoathouseFrame;
import de.nmichael.efa.gui.EfaExitFrame;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;

public class EfaBoathouseBackgroundTask extends Thread {

  private static final int CHECK_INTERVAL = 60;
  private static final int REMOTE_SCN_CHECK_INTERVAL = 5;
  private static final int ONCE_AN_HOUR = 3600 / CHECK_INTERVAL;
  private static final long BOAT_DAMAGE_REMINDER_INTERVAL = 7 * 24 * 60 * 60 * 1000;
  private static final long RESERVATION_REMINDER_DAY = 24 * 60 * 60 * 1000;
  private EfaBoathouseFrame efaBoathouseFrame;
  private boolean isProjectOpen = false;
  private boolean isLocalProject = true;
  private int onceAnHour;
  private Date date;
  private Calendar cal;
  private Calendar lockEfa;
  private long lastEfaConfigScn = -1;
  private long lastBoatStatusScn = -1;
  private long newBoatStatusScn = -1;

  public EfaBoathouseBackgroundTask(EfaBoathouseFrame efaBoathouseFrame) {
    this.efaBoathouseFrame = efaBoathouseFrame;
    onceAnHour = 5; // initial nach 5 Schleifendurchläufen zum ersten Mal hier reingehen
    cal = new GregorianCalendar();
    lockEfa = null;
    date = new Date();
  }

  public void setEfaLockBegin(DataTypeDate datum, DataTypeTime zeit) {
    if (Daten.efaConfig.getValueEfaDirekt_locked()) {
      lockEfa = null; // don't lock twice
      return;
    }
    if (datum == null || !datum.isSet()) {
      lockEfa = null;
    } else {
      if (zeit != null && zeit.isSet()) {
        lockEfa = new GregorianCalendar(datum.getYear(), datum.getMonth() - 1, datum.getDay(),
            zeit.getHour(), zeit.getMinute());
      } else {
        lockEfa = new GregorianCalendar(datum.getYear(), datum.getMonth() - 1, datum.getDay());
      }
    }
  }

  private void mailWarnings() {
    try {
      BufferedReader f = new BufferedReader(new FileReader(Daten.efaLogfile));
      String s;
      Vector<String> warnings = new Vector<String>();
      while ((s = f.readLine()) != null) {
        if (Logger.isWarningLine(s)
            && Logger.getLineTimestamp(s) > Daten.efaConfig
                .getValueEfaDirekt_bnrWarning_lasttime()) {
          warnings.add(s);
        }
      }
      f.close();
      if (warnings.size() == 0) {
        Logger.log(Logger.INFO, Logger.MSG_EVT_CHECKFORWARNINGS,
            International.getMessage(
                "Seit {date} sind keinerlei Warnungen in efa verzeichnet worden.",
                EfaUtil.getTimeStamp(Daten.efaConfig.getValueEfaDirekt_bnrWarning_lasttime())));
      } else {
        Logger.log(Logger.INFO, Logger.MSG_EVT_CHECKFORWARNINGS,
            International.getMessage("Seit {date} sind {n} Warnungen in efa verzeichnet worden.",
                EfaUtil.getTimeStamp(Daten.efaConfig.getValueEfaDirekt_bnrWarning_lasttime()),
                warnings.size()));
        String txt = International.getMessage(
            "Folgende Warnungen sind seit {date} in efa verzeichnet worden:",
            EfaUtil.getTimeStamp(Daten.efaConfig.getValueEfaDirekt_bnrWarning_lasttime())) + "\n"
            + International.getMessage("{n} Warnungen", warnings.size()) + "\n\n";
        for (int i = 0; i < warnings.size(); i++) {
          txt += warnings.get(i) + "\n";
        }
        if (Daten.project != null && Daten.efaConfig != null) {
          Messages messages = Daten.project.getMessages(false);
          if (messages != null && Daten.efaConfig.getValueEfaDirekt_bnrWarning_admin()) {
            messages.createAndSaveMessageRecord(MessageRecord.TO_ADMIN,
                International.getString("Warnungen"), txt);
          }
          if (messages != null && Daten.efaConfig.getValueEfaDirekt_bnrWarning_bootswart()) {
            messages.createAndSaveMessageRecord(MessageRecord.TO_BOATMAINTENANCE,
                International.getString("Warnungen"), txt);
          }
        }
      }
      if (Daten.efaConfig != null) {
        Daten.efaConfig.setValueEfaDirekt_bnrWarning_lasttime(System.currentTimeMillis());
        // @efaconfig Daten.efaConfig.writeFile();
      }

    } catch (Exception e) {
      Logger.log(Logger.ERROR, Logger.MSG_ERR_CHECKFORWARNINGS,
          "Checking Logfile for Warnings and mailing them to Admin failed: " + e.toString());
    }
  }

  @Override
  public void run() {
    // Diese Schleife läuft i.d.R. einmal pro Minute
    setName("EfaBoathouseBackgroundTask");
    while (true) {
      try {
        if (Logger.isTraceOn(Logger.TT_BACKGROUND, 5)) {
          long idleMillis = System.currentTimeMillis() - efaBoathouseFrame.getLastUserInteraction();
          Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
              "EfaBoathouseBackgroundTask: after sleepForAWhile() alive! (idle since "
                  + idleMillis / 1000 / 60 + "min " + idleMillis / 1000 % 60 + "sec)");
        }

        // find out whether a project is open, and whether it's local or remote
        updateProjectInfo();

        // abf
        checkBoatEndtime();

        // Update GUI on Config Changes
        checkUpdateGui();

        // Reservierungs-Checker
        checkBoatStatus();

        // Nach ungelesenen Nachrichten für den Admin suchen
        checkForUnreadMessages();

        // Nach Dateien mit Löschungen für Reservierungen suchen
        checkForFilesWithReservationRequests();

        // automatisches Beenden von efa
        checkForExitOrRestart();

        // efa zeitgesteuert sperren
        checkForLockEfa();

        // automatisches Beginnen eines neuen Fahrtenbuchs (z.B. zum Jahreswechsel)
        checkForAutoCreateNewLogbook();

        // immer im Vordergrund
        checkAlwaysInFront();

        // Fokus-Kontrolle
        checkFocus();

        // Speicher-Überwachung
        checkMemory();

        // Aktivitäten einmal pro Stunde
        if (--onceAnHour <= 0) {
          System.gc();
          // Damit Speicherüberwachung funktioniert
          // (anderenfalls wird CollectionUsage nicht aktualisiert; Java-Bug)
          onceAnHour = ONCE_AN_HOUR;
          if (Logger.isTraceOn(Logger.TT_BACKGROUND)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
                "EfaBoathouseBackgroundTask: onceAnHour alive!");
          }

          checkWarnings();

          checkUnfixedBoatDamages();

          remindAdminOfLogbookSwitch();
        }

        sleepForAWhile();

      } catch (Exception eglobal) {
        Logger.log(eglobal);
      }
    } // end: while(true)
  } // end: run

  private void updateProjectInfo() {
    try {
      if (Daten.project != null) {
        isProjectOpen = true;
        if (Daten.project.getProjectStorageType() == IDataAccess.TYPE_FILE_XML) {
          isLocalProject = true;
        } else {
          isLocalProject = false;
        }
      } else {
        isProjectOpen = false;
      }
    } catch (Exception e) {
      Logger.logdebug(e);
    }
  }

  private void sleepForAWhile() {
    if (Logger.isTraceOn(Logger.TT_BACKGROUND, 8)) {
      long idleMillis = System.currentTimeMillis() - efaBoathouseFrame.getLastUserInteraction();
      Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
          "EfaBoathouseBackgroundTask: sleepForAWhile() (idle since "
              + idleMillis / 1000 / 60 + "min " + idleMillis / 1000 % 60 + "sec)");
    }
    if (!isProjectOpen) {
      // sleep 60 seconds
      if (Logger.isTraceOn(Logger.TT_BACKGROUND, 9)) {
        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
            "EfaBoathouseBackgroundTask: sleep for " + CHECK_INTERVAL + " seconds....");
      }
      try {
        Thread.sleep(CHECK_INTERVAL * 1000);
      } catch (Exception e) {
        // wenn unterbrochen, dann versuch nochmal, kurz zu schlafen, und arbeite dann weiter!! ;-)
        try {
          Thread.sleep(100);
        } catch (Exception ee) {
          EfaUtil.foo();
        }
      }
    } else {
      // sleep at most 60 seconds, but wake up earlier if boat status has changed
      int cnt = CHECK_INTERVAL / REMOTE_SCN_CHECK_INTERVAL;
      if (Logger.isTraceOn(Logger.TT_BACKGROUND, 9)) {
        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
            "EfaBoathouseBackgroundTask: sleep for " + cnt + " times " + REMOTE_SCN_CHECK_INTERVAL
                + " seconds.");
      }
      BoatStatus boatStatus = null;
      try {
        boatStatus = (Daten.project != null ? Daten.project.getBoatStatus(false) : null);
      } catch (Exception e) {
        Logger.logdebug(e);
      }
      for (int i = 0; i < cnt; i++) {
        if (Logger.isTraceOn(Logger.TT_BACKGROUND, 9)) {
          Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
              "EfaBoathouseBackgroundTask: sleep" + i + " for "
                  + REMOTE_SCN_CHECK_INTERVAL + " seconds...");
        }
        try {
          Thread.sleep(REMOTE_SCN_CHECK_INTERVAL * 1000);
        } catch (Exception e) {
          // wenn unterbrochen, versuche nochmals kurz zu schlafen, und arbeite dann weiter!! ;-)
          try {
            Thread.sleep(100);
          } catch (Exception ee) {
            EfaUtil.foo();
          }
        }
        try {
          newBoatStatusScn = (boatStatus != null ? boatStatus.data().getSCN() : -1);
          if (newBoatStatusScn != -1 && newBoatStatusScn != lastBoatStatusScn) {
            // do NOT set lastBoatStatusScn = scn here!
            // This will be done when boat status is updated.
            if (Logger.isTraceOn(Logger.TT_BACKGROUND, 9)) {
              Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
                  "EfaBoathouseBackgroundTask: BoatStatus scn is " + newBoatStatusScn
                      + " (previously " + lastBoatStatusScn + ")");
            }
            break;
          }
          int seconds = Calendar.getInstance().get(Calendar.SECOND);
          if (seconds < REMOTE_SCN_CHECK_INTERVAL) {
            // Screen-Update zeitlich verschieben - auf die Zeit mit :00-:05 Sekunden verschieben.
            if (Logger.isTraceOn(Logger.TT_BACKGROUND, 8)) {
              Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
                  "EfaBoathouseBackgroundTask: seconds is :0" + seconds
                      + " (break for-loop to sync gui-clock)");
            }
            if (seconds > 0) {
              Thread.sleep((REMOTE_SCN_CHECK_INTERVAL - seconds) * 1000);
            }
            break;
          }
        } catch (Exception e) {
          Logger.logdebug(e);
        }
      }
    }
  }

  private void checkUpdateGui() {
    if (Logger.isTraceOn(Logger.TT_BACKGROUND, 8)) {
      Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
          "EfaBoathouseBackgroundTask: checkUpdateGui()");
    }
    if (Daten.efaConfig != null) {
      try {
        long scn = Daten.efaConfig.data().getSCN();
        if (scn != lastEfaConfigScn) {
          efaBoathouseFrame.updateGuiElements();
          lastEfaConfigScn = scn;
        }
      } catch (Exception e) {
        Logger.logdebug(e);
      }
    }
  }

  /**
   * Logbook-Fahrten mit Endtime werden automatisch beendet.
   */
  private void checkBoatEndtime() {
    long now = System.currentTimeMillis();

    if (Daten.project == null) return;
    // get List of Boats "on the water" = boatsOnTheWaterList;
    BoatStatus boatStatus = Daten.project.getBoatStatus(false);
    Vector<BoatStatusRecord> boats = new Vector<BoatStatusRecord>();
    if (Daten.efaConfig.isAutomaticEndLogbookOnTheWater()) {
      boats = boatStatus.getBoats(BoatStatusRecord.STATUS_ONTHEWATER, true);
    }
    if (Daten.efaConfig.isAutomaticEndLogbookNotAvailable()) {
      boats.addAll(boatStatus.getBoats(BoatStatusRecord.STATUS_NOTAVAILABLE, true));
    }

    Logbook currentLogbook = Daten.project.getCurrentLogbook();

    if (boats != null) {
      for (BoatStatusRecord boatStatusRecord : boats) {
        DataTypeIntString entryNo = boatStatusRecord.getEntryNo();
        if (entryNo == null) {
          continue;
        }
        LogbookRecord logbookRecord = currentLogbook.getLogbookRecord(entryNo);
        if (logbookRecord == null) {
          continue;
        }
        // check Endtime is set,valid
        // check Endtime is past
        if (logbookRecord.isEndtimeSetAndAlreadyPast(now)) {
          // do "Fahrt Beenden" with this boats
          logbookRecord.addComments("(efa: Fahrt nach Ablauf automatisch ausgetragen)");
          logbookRecord.setSessionIsOpen(false);
          // updateBoatStatus(true, EfaBaseFrame.MODE_BOATHOUSE_FINISH);
          EfaBaseFrame.logBoathouseEvent(Logger.INFO, Logger.MSG_EVT_TRIPEND,
                  International.getString("Fahrtende") + " (autom)", logbookRecord);
          boatStatusRecord.setCurrentStatus(BoatStatusRecord.STATUS_AVAILABLE);
          boatStatusRecord.setShowInList(null);
          boatStatusRecord.setComment("");
          boatStatusRecord.setEntryNo(null);
          boatStatusRecord.setLogbook(null);
          boatStatusRecord.setBoatText(logbookRecord.getBoatAsName());
          try {
            currentLogbook.data().update(logbookRecord); // saveEntry();
            boatStatus.data().update(boatStatusRecord);
          } catch (EfaException e) {
            Logger.log(Logger.ERROR, Logger.MSG_ERROR_EXCEPTION, e);
          }
        }
      }
    }
  }

  private void checkBoatStatus() {
    if (Logger.isTraceOn(Logger.TT_BACKGROUND, 8)) {
      Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
          "EfaBoathouseBackgroundTask: checkBoatStatus()");
    }

    boolean listChanged = false;
    if (newBoatStatusScn != -1 &&
        newBoatStatusScn != lastBoatStatusScn) {
      // Falls Datenbank BoatStatus sich geändert hat (abf)
      listChanged = true;
    }
    lastBoatStatusScn = newBoatStatusScn;

    if (isProjectOpen && !isLocalProject) {
      efaBoathouseFrame.updateBoatLists(listChanged);
      if (Logger.isTraceOn(Logger.TT_BACKGROUND, 8)) {
        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
            "EfaBoathouseBackgroundTask: checkBoatStatus() - done for remote project");
      }
      return;
    }

    BoatStatus boatStatus = (Daten.project != null ? Daten.project.getBoatStatus(false) : null);
    BoatReservations boatReservations = (Daten.project != null ? Daten.project
        .getBoatReservations(false) : null);
    BoatDamages boatDamages = (Daten.project != null ? Daten.project.getBoatDamages(false) : null);
    if (boatStatus == null || boatReservations == null || boatDamages == null) {
      return;
    }

    long now = System.currentTimeMillis();
    long aktuelleMinute = (now / 1000 / 60);
    try {
      DataKeyIterator it = boatStatus.data().getStaticIterator();
      for (DataKey<?, ?, ?> k = it.getFirst(); k != null; k = it.getNext()) {
        BoatStatusRecord boatStatusRecord = (BoatStatusRecord) boatStatus.data().get(k);
        if (boatStatusRecord == null) {
          continue;
        }
        try {
          String oldCurrentStatus = boatStatusRecord.getCurrentStatus();
          String oldShowInList = boatStatusRecord.getShowInList();
          String oldComment = boatStatusRecord.getComment();

          // set CurrentStatus correctly
          if (!boatStatusRecord.getCurrentStatus().equals(BoatStatusRecord.STATUS_ONTHEWATER)
              && !boatStatusRecord.getCurrentStatus().equals(boatStatusRecord.getBaseStatus())) {
            boatStatusRecord.setCurrentStatus(boatStatusRecord.getBaseStatus());
          }

          if (boatStatusRecord.getCurrentStatus().equals(BoatStatusRecord.STATUS_HIDE)) {
            if (boatStatusRecord.getShowInList() != null
                && !boatStatusRecord.getShowInList().equals(BoatStatusRecord.STATUS_HIDE)) {
              boatStatusRecord.setShowInList(null);
              boatStatus.data().update(boatStatusRecord);
              listChanged = true;
            }
            continue;
          }
          if (boatStatusRecord.getUnknownBoat()) {
            if (!boatStatusRecord.getCurrentStatus().equals(BoatStatusRecord.STATUS_ONTHEWATER)) {
              boatStatus.data().delete(boatStatusRecord.getKey());
              listChanged = true;
            }
            continue;
          }

          // höchstens alle 10 Minuten, nicht jede Minute
          if (aktuelleMinute % 10 == 0) {
            long boatReservationReminderTime = RESERVATION_REMINDER_DAY;
            if (boatStatusRecord.isBootshausOH()) {
              boatReservationReminderTime *= Daten.efaConfig.getAnzahlTageErinnerungBootshaus();
            } else {
              boatReservationReminderTime *= Daten.efaConfig.getAnzahlTageErinnerungBoote();
            }
            if (boatReservationReminderTime > 0) {
              BoatReservationRecord[] br = boatReservations.getBoatReservations(
                  boatStatusRecord.getBoatId(), now + boatReservationReminderTime, 0);
              sendeEmailAlsErinnerungWennZeitpunktErreicht(br, boatReservationReminderTime);
            }
          }

          // delete any obsolete reservations
          int purgedRes = boatReservations.purgeObsoleteReservations(
              boatStatusRecord.getBoatId(), now);

          // find all currently valid reservations
          BoatReservationRecord[] reservations = boatReservations.getBoatReservations(
              boatStatusRecord.getBoatId(), now, 0);
          if (reservations == null || reservations.length == 0) {
            // no reservations at the moment - nothing to do
            if (!boatStatusRecord.getCurrentStatus().equals(BoatStatusRecord.STATUS_ONTHEWATER)
                && !boatStatusRecord.getShowInList().equals(boatStatusRecord.getCurrentStatus())) {
              boatStatusRecord.setShowInList(null);
            }

            if (purgedRes > 0) {
              boatStatusRecord.setComment(null);
            } else {
              // wow, now this is a hack!
              // If there is a comment for this boat that *looks* as if it was a
              // reservation comment, remove it! This might not work for all languages,
              // but for some...
              // Reason for such reservation strings could be reservations that were
              // explicitly deleted by the Admin while a boat was reserved, and have
              // never been purged by the background task itself.
              String resstr = International.getMessage(
                  "reserviert für {name} ({reason}) {from_to}", "", "", "");
              if (resstr.length() > 10) {
                resstr = resstr.substring(0, 10);
              }
              if (oldComment != null && oldComment.startsWith(resstr)) {
                boatStatusRecord.setComment(null);
              }
            }
          } else {
            // reservations found
            if (!boatStatusRecord.getCurrentStatus().equals(BoatStatusRecord.STATUS_ONTHEWATER)) {
              if (Daten.efaConfig.isAutomaticStartLogbookFromReservation()) {
                LogbookRecord newLogbookRecord = starteFahrtMitEndtimeLautReservation(reservations);
                if (newLogbookRecord != null) {
                  updateBoatstatusRecord(boatStatusRecord, newLogbookRecord);
                }
              }
              if (Daten.efaConfig.getValueEfaDirekt_resBooteNichtVerfuegbar()) {
                if (!boatStatusRecord.getShowInList()
                    .equals(BoatStatusRecord.STATUS_NOTAVAILABLE)) {
                  boatStatusRecord.setShowInList(BoatStatusRecord.STATUS_NOTAVAILABLE);
                }
              } else {
                if (!boatStatusRecord.getShowInList().equals(boatStatusRecord.getBaseStatus())) {
                  boatStatusRecord.setShowInList(boatStatusRecord.getBaseStatus());
                }
              }
            }
            // letzte Reservierung, nicht erste
            String s = International.getMessage("reserviert für {name} ({reason}) {from_to}",
                reservations[reservations.length - 1].getPersonAsName(),
                reservations[reservations.length - 1].getReason(),
                reservations[reservations.length - 1].getReservationTimeDescription(
                    BoatReservationRecord.KEEP_NUM_DATE));
            boatStatusRecord.setComment(s);
          }

          // find all current damages
          boolean damaged = false;
          BoatDamageRecord[] damages = boatDamages.getBoatDamages(boatStatusRecord.getBoatId());
          for (int i = 0; damages != null && i < damages.length; i++) {
            if (!damages[i].getFixed() && damages[i].getSeverity() != null
                && damages[i].getSeverity().equals(BoatDamageRecord.SEVERITY_NOTUSEABLE)) {
              boatStatusRecord.setComment(damages[i].getShortDamageInfo());
              damaged = true;
              if (!boatStatusRecord.getShowInList().equals(BoatStatusRecord.STATUS_NOTAVAILABLE)) {
                boatStatusRecord.setShowInList(BoatStatusRecord.STATUS_NOTAVAILABLE);
              }
              break; // stop after first severe damage
            }
          }
          if (!damaged && boatStatusRecord.getComment() != null &&
              BoatDamageRecord.isCommentBoatDamage(boatStatusRecord.getComment())) {
            boatStatusRecord.setComment(null);
          }

          // make sure that if the boat is on the water,
          // this status overrides any other list settings
          if (boatStatusRecord.getCurrentStatus().equals(BoatStatusRecord.STATUS_ONTHEWATER)) {
            if (boatStatusRecord.isOnTheWaterShowNotAvailable()) {
              boatStatusRecord.setShowInList(BoatStatusRecord.STATUS_NOTAVAILABLE);
            } else {
              boatStatusRecord.setShowInList(BoatStatusRecord.STATUS_ONTHEWATER);
            }
          }

          boolean statusRecordChanged = false;
          if (oldCurrentStatus == null
              || !oldCurrentStatus.equals(boatStatusRecord.getCurrentStatus())) {
            statusRecordChanged = true;
          }
          if (oldShowInList == null
              || !oldShowInList.equals(boatStatusRecord.getShowInList())) {
            statusRecordChanged = true;
          }
          if ((oldComment == null &&
              boatStatusRecord.getComment() != null &&
              boatStatusRecord.getComment().length() > 0)
              || (oldComment != null &&
                  !oldComment.equals(boatStatusRecord.getComment()))) {
            statusRecordChanged = true;
          }

          if (statusRecordChanged) {
            boatStatus.data().update(boatStatusRecord);
            listChanged = true;
          }
          if (statusRecordChanged && Logger.isTraceOn(Logger.TT_BACKGROUND, 2)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
                "BoatStatus changed for Boat " + boatStatusRecord.getBoatNameAsString(now)
                    + ", new Status: " + boatStatusRecord.toString());
          }
        } catch (Exception ee) {
          Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, ee);
        }
      }
      if (Logger.isTraceOn(Logger.TT_BACKGROUND, 9)) {
        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
            "EfaBoathouseBackgroundTask: checkBoatStatus() -"
                + (listChanged ? "" : " not")
                + " calling updateBoatLists("
                + listChanged + ")");
      }
      if (listChanged) {
        efaBoathouseFrame.updateBoatLists(listChanged); // must have
      }
      // Prüfung der letzten User-Interaction Zeitpunkt
      checkBoatStatusWhileIdle();

      if (Logger.isTraceOn(Logger.TT_BACKGROUND, 9)) {
        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
            "EfaBoathouseBackgroundTask: checkBoatStatus() - done");
      }
    } catch (Exception e) {
      Logger.logwarn(e);
    }
  }

  private void checkBoatStatusWhileIdle() {
    long now = System.currentTimeMillis();
    long idleSec = (now - efaBoathouseFrame.getLastUserInteraction()) / 1000;
    long waitSec = Daten.AUTO_EXIT_MIN_LAST_USED * 60;

    if (idleSec > 30) {
      if (Logger.isTraceOn(Logger.TT_BACKGROUND, 6)) {
        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
            "EfaBoathouseBackgroundTask: checkBoatStatus() - clearAllPopups() "
                + "(idle since " + idleSec / 60 + "min " + idleSec % 60 + "sec)");
      }
      efaBoathouseFrame.clearAllPopups(); // nach 30 Sekunden
    }
    if (idleSec < (waitSec + 0)) {
      if (Logger.isTraceOn(Logger.TT_BACKGROUND, 6)) {
        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
            "EfaBoathouseBackgroundTask: checkBoatStatus() - nix reset. zu kurz "
                + "(idle since " + idleSec / 60 + "min " + idleSec % 60 + "sec)");
      }
      return;
    }
    if (idleSec > (waitSec + CHECK_INTERVAL)) {
      if (Logger.isTraceOn(Logger.TT_BACKGROUND, 6)) {
        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
            "EfaBoathouseBackgroundTask: checkBoatStatus() - nix reset. zu lang "
                + "(idle since " + idleSec / 60 + "min " + idleSec % 60 + "sec)");
      }
      return;
    }
    if (Logger.isTraceOn(Logger.TT_BACKGROUND, 6)) {
      Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
          "EfaBoathouseBackgroundTask: checkBoatStatus() -> resetSorting() "
              + "(idle since " + idleSec / 60 + "min " + idleSec % 60 + "sec)");
    }
    if (!efaBoathouseFrame.resetSorting()) { // nach 5 Minuten
      // includes efaBoathouseFrame.updateBoatLists(listChanged);
      // true means reset has already done updateBoatLists() and alive()

      // efaBoathouseFrame.boatListRequestFocus(0);
      efaBoathouseFrame.updateBoatLists(false);
    }
  }

  /**
   * Eine Woche vor der Reservierung wird eine Email verschickt
   *
   * @param reservations
   */
  private void sendeEmailAlsErinnerungWennZeitpunktErreicht(BoatReservationRecord[] reservations,
      long remindertime) {
    if (reservations == null) {
      return;
    }
    if (reservations.length != 1) {
      // TODO abf
      return;
    }
    String aktion = "REMINDER";
    for (BoatReservationRecord boatReservationRecord : reservations) {
      long lastModified = boatReservationRecord.getLastModified();
      long realStart = boatReservationRecord.getDateFrom()
          .getTimestamp(boatReservationRecord.getTimeFrom());
      if (lastModified + remindertime > realStart) {
        // Email wurde offenbar schon einmal verschickt
        continue;
      }

      boatReservationRecord.sendEmailReminder(aktion);

      // update von LastModified, um keine erneuten Erinnerungsmails zu schicken
      try {
        Daten.project.getBoatReservations(false).data().update(boatReservationRecord);
      } catch (EfaException e) {
        Logger.logwarn(e);
        e.printStackTrace();
      }
    }
  }

  /**
   * Eine Reservierung startet automatisch eine Fahrt
   *
   * @param boatReservations
   * @return
   */
  private LogbookRecord starteFahrtMitEndtimeLautReservation(
      BoatReservationRecord[] boatReservations) {
    if (boatReservations == null) {
      return null;
    }
    for (BoatReservationRecord boatReservationRecord : boatReservations) {
      if (boatReservationRecord.getType().equals(BoatReservationRecord.TYPE_WEEKLY)) {
        continue; // skip weekly
      }
      if (boatReservationRecord.getInvisible()) {
        continue; // skip invisible
      }
      if (boatReservationRecord.isModifiedAfterStartAndChangedOften()) {
        continue; // skip autom.Start
      }
      LogbookRecord newLogbookRecord = createAndPersistNewLogbookRecord(boatReservationRecord);
      if (newLogbookRecord != null) {
        EfaBaseFrame.logBoathouseEvent(Logger.INFO, Logger.MSG_EVT_TRIPEND,
            International.getString("Fahrtbeginn") + "(reserv)", newLogbookRecord);
        boatReservationRecord.setInvisible(true); // TODO unnötig - kann raus!
        updateReservation(boatReservationRecord);
        return newLogbookRecord;
      }
    }
    return null;
  }

  private void updateReservation(BoatReservationRecord boatReservationRecord) {
    try {
      boatReservationRecord.getPersistence().data().update(boatReservationRecord);
    } catch (EfaException e) {
      Logger.log(Logger.WARNING, Logger.MSG_ERROR_EXCEPTION, e);
    }
  }

  private LogbookRecord createAndPersistNewLogbookRecord(
      BoatReservationRecord boatReservationRecord) {
    Logbook currentLogbook = Daten.project.getCurrentLogbook();

    DataTypeIntString newEntryNo = currentLogbook.getNextEntryNo();
    LogbookRecord newLogbookRecord = currentLogbook.createLogbookRecord(newEntryNo);
    newLogbookRecord.setEntryId(newEntryNo); // braucht man das?
    newLogbookRecord.setBoatId(boatReservationRecord.getBoatId());
    // newLogbookRecord.setCrewId(1, boatReservationRecord.getPersonId());
    newLogbookRecord.setCoxId(boatReservationRecord.getPersonId());
    if (boatReservationRecord.getPersonId() == null) {
      // newLogbookRecord.setCrewName(1, boatReservationRecord.getPersonName());
      newLogbookRecord.setCoxName(boatReservationRecord.getPersonName());
    }
    newLogbookRecord.setContact(boatReservationRecord.getContact());
    newLogbookRecord.setDate(boatReservationRecord.getDateFrom());
    newLogbookRecord.setStartTime(boatReservationRecord.getTimeFrom());
    if (!boatReservationRecord.getDateFrom().equals(boatReservationRecord.getDateTo())) {
      newLogbookRecord.setEndDate(boatReservationRecord.getDateTo());
    }
    newLogbookRecord.setEndTime(boatReservationRecord.getTimeTo());
    String reason = boatReservationRecord.getReason();
    reason = reason.isEmpty() ? "anhand Reservierung" : reason;
    Destinations destinations = Daten.project.getDestinations(false);
    DestinationRecord dr = destinations.getDestination(reason, System.currentTimeMillis());
    if (dr != null && dr.getId() != null) {
      newLogbookRecord.setDestinationId(dr.getId());
    } else {
      newLogbookRecord.setDestinationName(reason);
    }
    newLogbookRecord.setComments("(efa: Fahrt gestartet aufgrund einer Reservierung)");
    newLogbookRecord.setSessionIsOpen(true);
    try {
      currentLogbook.data().add(newLogbookRecord); // save
      return newLogbookRecord;
    } catch (EfaException e) {
      // Auto-generated catch block
      e.printStackTrace();
      return null;
    }
  }

  private void updateBoatstatusRecord(BoatStatusRecord boatStatusRecord,
      LogbookRecord newLogbookRecord) {
    boatStatusRecord.setCurrentStatus(BoatStatusRecord.STATUS_ONTHEWATER);
    if (efaBoathouseFrame.getLogbook() == null) return;
    boatStatusRecord.setLogbook(efaBoathouseFrame.getLogbook().getName());
    boatStatusRecord.setEntryNo(newLogbookRecord.getEntryId());
  }

  private void checkForFilesWithReservationRequests() {
    String resultText = null;

    // Ordner efa2/todoo öffnen
    String folderTodo = Daten.efaBaseConfig.efaUserDirectory + "todo" + Daten.fileSep;
    Map<String, String> strMap = getStringMap(folderTodo);
    if (strMap.isEmpty()) {
      return;
    }

    Persons persons = Daten.project.getPersons(false);
    PersonRecord person = getValidPersonBehindRequest(persons, strMap);
    String oldEmailTo = person == null ? null : person.getEmail();

    String aktion = strMap.get("action");
    if (aktion != null) {
      switch (aktion) {
        case "DELETE":
          resultText = performDeleteReservationRequest(strMap);
          break; // return; // to avoid two Mails
        case "INSERT":
          resultText = performInsertReservationRequest(person, strMap);
          break; // return; // to avoid two Mails

        case "UNSUBSCRIBE":
        case "SUBSCRIBE":
          resultText = performSubscribeReservationRequest(persons, person, strMap);
          break;

        case "CHANGE_NAME":
        case "SETMAIL":
        case "SETPHONENR":
        case "SETKÜRZEL":
          resultText = performSetPersonMitgliedRequest(persons, person, strMap);
          break;

        default:
          break;
      }
    }
    if (person != null) {
      String emailToAdresse = person.getEmail();
      if (oldEmailTo != null && !oldEmailTo.equals(emailToAdresse)) {
        person.sendEmailConfirmation(oldEmailTo, "CONFIRM_" + aktion, resultText);
      }
      switch (aktion) {
        case "DELETE":
        case "INSERT":
          break; // return; // to avoid two Mails
        default:
          person.sendEmailConfirmation(emailToAdresse, "CONFIRM_" + aktion, resultText);
      }
    }
  }

  private PersonRecord getValidPersonBehindRequest(Persons persons, Map<String, String> strMap) {
    if (persons == null) {
      return null;
    }
    long now = System.currentTimeMillis();
    PersonRecord person = persons.getPersonByMembership(strMap.get("mitgliedNr"), now);
    if (person == null) {
      person = persons.getPerson(strMap.get("ForName"), now);
    }
    return person;
  }

  private String performDeleteReservationRequest(Map<String, String> strMap) {
    String strEfaId = strMap.get("efaId");
    if (strEfaId == null) {
      return null; // kein File im Folder
    }
    int reservierungsnummer = Integer.parseInt(strEfaId.replaceAll("\\D+", ""));
    if (reservierungsnummer == 0) {
      String error = "Storno-Link: keine Reservierungsnummer " + reservierungsnummer;
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }

    BoatReservations boatReservations = Daten.project.getBoatReservations(false);
    String strHashId = strMap.get("hashId");
    String strCodeOld = strMap.get("code");
    if (strHashId == null) {
      // alte Schreibweise
      strHashId = strCodeOld;
    }
    if (strHashId == null) {
      String error = "Storno-Link: kein Code HashId " + strHashId;
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }
    strHashId = strHashId.replace("'", "");
    if (strHashId.isEmpty()) {
      // alte Schreibweise
      strHashId = strCodeOld;
      strHashId = strHashId.replace("'", "");
    }
    if (strHashId.isEmpty()) {
      String error = "Storno-Link: leerer Code HashId " + strHashId;
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }

    // hashId = Reservierung suchen // hashId = code prüfen
    BoatReservationRecord[] brrArray = boatReservations.findBoatReservationsByHashId(strHashId);
    if (brrArray == null || brrArray.length == 0) {
      String error = "Storno-Link: Reservierung mit hashId " + strHashId
          + " nicht (mehr) gefunden. " + brrArray;
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }
    BoatReservationRecord brrHashId = brrArray[0];
    if (brrHashId == null) {
      String error = "Storno-Link: unbekannte Reservierung1 " + strHashId;
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }

    // -----------------------------

    // reservierungsnummer prüfenq1
    if (reservierungsnummer != brrHashId.getReservation()) {
      String error = "Storno-Link: falsche efaId " + reservierungsnummer + " in Reservierung1 "
          + brrHashId.getReservation();
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }
    // efaId prüfen2
    if (!(strEfaId.equals(brrHashId.getEfaId()))) {
      String error = "Storno-Link: falsche efaId " + strEfaId + " in Reservierung2 "
          + brrHashId.getEfaId();
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }

    // -----------------------------

    // efaId = Reservierung suchen
    BoatReservationRecord brr = boatReservations.findBoatReservationByNumber(reservierungsnummer);
    if (brr == null) {
      String error = "Storno-Link: unbekannte Reservierung2 " + reservierungsnummer;
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }

    // efaId prüfen3
    if (!(strEfaId.equals(brr.getEfaId()))) {
      String error = "Storno-Link: falsche efaId " + strEfaId + " in Reservierung3 "
          + brr.getEfaId();
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }

    // hashId = code prüfen
    if (!(strHashId.equals(brr.getHashId()))) {
      String error = "Storno-Link: falscher Code HashId " + strHashId;
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }

    // Bestätigungsmail verschicken
    String aktion = strMap.get("action"); // "DELETE";
    brr.sendEmailBeiReservierung(aktion);

    try {
      // Reservierung löschen
      boatReservations.data().delete(brr.getKey());
    } catch (EfaException e) {
      Logger.log(Logger.ERROR, Logger.MSG_ABF_ERROR, e);
      return e.getLocalizedMessage();
    }
    String info = brr.getPersistence().getDescription() + ": "
        + International.getMessage(
            "{name} hat Datensatz '{record}' gelöscht.",
            "Storno-Link", brr.getQualifiedName() +
                " von " + brr.getPersonAsName());
    Logger.log(Logger.INFO, Logger.MSG_ABF_INFO, info);
    return info;
  }

  private String performInsertReservationRequest(PersonRecord personRecord,
      Map<String, String> strMap) {
    long now = System.currentTimeMillis();
    String strBoatName = strMap.get("efaBootName");
    if (strBoatName == null) {
      String error = "Add-Link: kein Boot angegeben " + strBoatName;
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }
    UUID boatId = null;
    try {
      Boats boats = Daten.project.getBoats(false);
      DataKey<?, ?, ?>[] byNameField = boats.data().getByFields(
          new String[] { BoatRecord.NAME }, new String[] { strBoatName.trim() }, now);
      boatId = byNameField != null ? (UUID) byNameField[0].getKeyPart1() : null;
    } catch (Exception e1) {
      Logger.log(Logger.ERROR, Logger.MSG_ABF_ERROR, "Add-Link: e1 " + e1.getLocalizedMessage());
    }
    if (boatId == null) {
      String error = "Add-Link: unbekanntes Boot " + strBoatName;
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }

    BoatReservations boatReservations = Daten.project.getBoatReservations(false);
    BoatReservationRecord brr = boatReservations.createBoatReservationsRecord(boatId);
    if (brr == null) {
      String error = "Add-Link: cannot createBoatReservationsRecord " + strBoatName;
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }

    try {
      UUID personId = personRecord != null ? personRecord.getId() : null;
      if (personId != null) {
        brr.setPersonId(personId);
      } else {
        brr.setPersonName(strMap.get("ForName"));
      }
      brr.setType(BoatReservationRecord.TYPE_ONETIME);
      brr.setDateFrom(DataTypeDate.parseDate(strMap.get("DateFrom")));
      brr.setTimeFrom(DataTypeTime.parseTime(strMap.get("TimeFrom")));
      brr.setDateTo(DataTypeDate.parseDate(strMap.get("DateTo")));
      brr.setTimeTo(DataTypeTime.parseTime(strMap.get("TimeTo")));
      brr.setContact(strMap.get("Telefon"));
      brr.setReason(strMap.get("Reason"));
      if (!brr.getDateTo().isSet()) {
        brr.setDateTo(DataTypeDate.parseDate(strMap.get("DateFrom")));
      }
      if (brr.getReason().isEmpty()) {
        brr.setReason("anhand Add-Link");
      }
    } catch (Exception e3) {
      Logger.log(Logger.ERROR, Logger.MSG_ABF_ERROR, e3);
      return "Add-Link: e3 " + e3.getLocalizedMessage();
    }
    try {
      brr.resetHashId(); // TODO 2020.11.01 abf Muss anders: Erstmalig eine HashId setzen?
      boatReservations.data().add(brr);

      // Bestätigungsmail verschicken
      String aktion = strMap.get("action"); // "INSERT";
      brr.sendEmailBeiReservierung(aktion);

      String info = brr.getPersistence().getDescription() + ": "
          + International.getMessage("{name} hat neuen Datensatz '{record}' erstellt.",
              "Add-Link von " + brr.getPersonAsName(), brr.getQualifiedName() + " "
                  + brr.getReservationTimeDescription(BoatReservationRecord.REPLACE_HEUTE));
      Logger.log(Logger.INFO, Logger.MSG_DATAADM_RECORDADDED, info);
      return info;
    } catch (Exception e2) {
      String error = "Add-Link: e2 " + e2.getLocalizedMessage();
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }
  }

  private String performSubscribeReservationRequest(Persons persons, PersonRecord person,
      Map<String, String> strMap) {
    String aktion = strMap.get("action"); // "UNSUBSCRIBE";
    if (aktion == null || aktion.isBlank()) {
      String error = "Newsletter-Link: keine Aktion angegeben " + aktion;
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }

    String strPersonMitgliedNrOH = strMap.get("mitgliedNr");
    if (strPersonMitgliedNrOH == null || strPersonMitgliedNrOH.isBlank()) {
      String error = "Newsletter-" + aktion + ": keine OH-MitgliedsNr angegeben "
          + strPersonMitgliedNrOH;
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }
    if (person == null) {
      String error = "Newsletter-" + aktion + ": unbekanntes Mitglied " + strPersonMitgliedNrOH;
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }
    if (!person.getMembershipNo().equals(strPersonMitgliedNrOH)) {
      String error = "Newsletter-" + aktion + ": falsche Mitgliedsnummer " + strPersonMitgliedNrOH
          + " bei " + person.getFirstLastName() + " " + person.getMembershipNo();
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }

    if (aktion.equalsIgnoreCase("SUBSCRIBE") && person.isErlaubtEmail()) {
      String error = "Newsletter-" + aktion + ": " + person.getFirstLastName()
          + " ist bereits angemeldet. "
          + person.isErlaubtEmail();
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }
    if (aktion.equalsIgnoreCase("UNSUBSCRIBE") && !person.isErlaubtEmail()) {
      String error = "Newsletter-" + aktion + ": " + person.getFirstLastName()
          + " ist bereits abgemeldet. "
          + person.isErlaubtEmail();
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }
    if (aktion.equalsIgnoreCase("SUBSCRIBE")) {
      person.setErlaubnisEmail(true);
    }
    if (aktion.equalsIgnoreCase("UNSUBSCRIBE")) {
      person.setErlaubnisEmail(false);
    }
    if (persons == null) {
      String error = aktion + ": keine Mitglieder gefunden. Daten.project.getPersons() ";
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }
    try {
      persons.data().update(person);
      String info = "Newsletter-" + aktion + ": " + person.getFirstLastName()
          + " hat nun Email-Erlaubnis " + person.isErlaubtEmail();
      Logger.log(Logger.INFO, Logger.MSG_ABF_INFO, info);
      return info;
    } catch (EfaException e2) {
      String error = "Newsletter-" + aktion + ": e2 " + e2.getLocalizedMessage();
      Logger.log(Logger.ERROR, Logger.MSG_ABF_ERROR, error);
      return error;
    }
  }

  private String performSetPersonMitgliedRequest(Persons persons, PersonRecord person,
      Map<String, String> strMap) {
    String aktion = strMap.get("action"); // "SETKÜRZEL" "SETPHONENR" "SETMAIL" "CHANGE_NAME"
    if (aktion == null || aktion.isBlank()) {
      String error = "Person-Profil-Link: keine Aktion angegeben " + aktion;
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }

    String strPersonMitgliedNrOH = strMap.get("mitgliedNr");
    if (strPersonMitgliedNrOH == null || strPersonMitgliedNrOH.isBlank()) {
      String error = "Person-Profil-" + aktion + ": keine OH-MitgliedsNr angegeben "
          + strPersonMitgliedNrOH;
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }

    if (person == null) {
      String error = "Person-Profil-" + aktion + ": unbekanntes Mitglied " + strPersonMitgliedNrOH;
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }
    if (!person.getMembershipNo().equals(strPersonMitgliedNrOH)) {
      String error = "Person-Profil-" + aktion + ": falsche Mitgliedsnummer "
          + strPersonMitgliedNrOH
          + " bei " + person.getFirstLastName() + " " + person.getMembershipNo();
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }

    String errorHashId = checkHashId(strMap.get("hashId"));
    if (errorHashId != null) {
      errorHashId = "Person-Profil-" + errorHashId;
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, errorHashId);
      return errorHashId;
    }

    if (aktion.equalsIgnoreCase("CHANGE_NAME")) {
      return performChangePersonNameRequest(strMap, persons, person);
    }

    String name = strMap.get("ForName");
    if (name == null) {
      // alte Schreibweise
      name = strMap.get("name");
    }
    if (!person.getFirstLastName().equals(name)) {
      String error = "Person-Profil-" + aktion + ": falscher Name angegeben " + name
          + ", bei EFA: " + person.getFirstLastName();
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }

    if (aktion.equalsIgnoreCase("SETMAIL")) {
      return performChangePersonEmailRequest(strMap, persons, person);
    }
    if (aktion.equalsIgnoreCase("SETPHONENR")) {
      return performChangePersonPhoneNrRequest(strMap, persons, person);
    }
    if (aktion.equalsIgnoreCase("SETKÜRZEL")) {
      return performChangePersonShortcutRequest(strMap, persons, person);
    }
    return null;
  }

  private String performChangePersonNameRequest(Map<String, String> strMap,
      Persons persons, PersonRecord person) {
    String aktion = strMap.get("action"); // "CHANGE_NAME"
    if (!aktion.equalsIgnoreCase("CHANGE_NAME")) {
      String error = "Person-Profil-Link: keine gültige Aktion angegeben " + aktion;
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }

    String neuerName = strMap.get("ForName");
    if (neuerName == null) {
      neuerName = "";
    } else {
      neuerName = neuerName.trim();
    }

    if (person.hatSchreibweiseNameGeaendert()) {
      if (neuerName.equals(person.getFirstLastName())) {
        String error = "Person-Profil-" + aktion + ": " + person.getFirstLastName()
            + " heißt bereits '" + neuerName
            + "' und hat bereits: " + person.hatSchreibweiseNameGeaendert();
        Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
        return error;
      }
    }

    if (neuerName.isBlank()) {
      String error = "Person-Profil-" + aktion + ": " + person.getFirstLastName()
          + " muss einen Namen haben, der nicht leer ist: '" + neuerName + "' ";
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }

    if (persons == null) {
      String error = "Person-Profil-" + aktion
          + ": keine Mitglieder gefunden. Daten.project.getPersons() ";
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }
    long now = System.currentTimeMillis();
    PersonRecord otherPerson = persons.getPerson(neuerName, now);
    if (otherPerson != null) {
      String error = "Person-Profil-" + aktion + ": Den Namen '" + neuerName
          + "' hat " + otherPerson.getFirstLastName() + " bereits.";
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }

    String nameParts[] = neuerName.split(" ", 2);
    String neuerVorname = nameParts[0];
    if (neuerVorname == null) {
      String error = "Person-Profil-" + aktion + ": Der Name '" + neuerName
          + "' hat keinen Vornamen " + neuerVorname;
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }
    neuerVorname = neuerVorname.trim();
    if (neuerVorname.isBlank()) {
      String error = "Person-Profil-" + aktion + ": Der Name '" + neuerName
          + "' hat leeren Vornamen " + neuerVorname;
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }
    String neuerNachname = nameParts[1].trim();
    if (neuerNachname == null) {
      String error = "Person-Profil-" + aktion + ": Der Name '" + neuerName
          + "' hat keinen Vornamen " + neuerNachname;
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }
    neuerNachname = neuerNachname.trim();
    if (neuerNachname.isBlank()) {
      String error = "Person-Profil-" + aktion + ": Der Name '" + neuerName
          + "' hat leeren Vornamen " + neuerNachname;
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }

    String myMatch = Daten.efaConfig.getRegexForVorUndNachname();
    if (!neuerName.matches(myMatch)) {
      String error = "Person-Profil-" + aktion + ": "
          + International.getString("Bitte Vor- und Nachname eingeben") + " " + neuerName;
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }

    String oldName = person.getFirstLastName();
    person.setSchreibweiseGeaendert(!neuerName.isBlank());
    person.setFirstName(neuerVorname);
    person.setLastName(neuerNachname);

    try {
      persons.data().update(person);
      String info = "Person-Profil-" + aktion + ": " + oldName
          + " hat seinen Namen in '" + person.getFirstLastName()
          + "' geändert und hat nun: " + person.hatSchreibweiseNameGeaendert();
      Logger.log(Logger.INFO, Logger.MSG_ABF_INFO, info);
      return info;
    } catch (EfaException e2) {
      String error = "Person-Profil-" + aktion + ": e2 " + e2.getLocalizedMessage();
      Logger.log(Logger.ERROR, Logger.MSG_ABF_ERROR, error);
      return error;
    }
  }

  private String performChangePersonEmailRequest(Map<String, String> strMap,
      Persons persons, PersonRecord person) {
    String aktion = strMap.get("action"); // "SETMAIL"
    if (!aktion.equalsIgnoreCase("SETMAIL")) {
      String error = "Person-Profil-Link: keine gültige Aktion angegeben " + aktion;
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }

    String neueEmail = strMap.get("email");
    if (neueEmail == null) {
      neueEmail = "";
    } else {
      neueEmail = neueEmail.trim();
    }

    if (person.isErlaubtEmail()) {
      if (neueEmail.equals(person.getEmail())) {
        String error = "Person-Profil-" + aktion + ": " + person.getFirstLastName()
            + " hat bereits Email '" + person.getEmail()
            + "' und Erlaubnis: " + person.isErlaubtEmail();
        Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
        return error;
      }
    } else {
      if (neueEmail.isBlank() && person.getEmail() == null) {
        String error = "Person-Profil-" + aktion + ": " + person.getFirstLastName()
            + " hat keine Email zum Entfernen. "
            + person.isErlaubtEmail() + " " + person.getEmail();
        Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
        return error;
      }
    }

    // not blank, check spelling
    if (!neueEmail.isBlank()) {
      // String myMatch = "^[-_.\\w]+@([0-9a-zA-Z][-\\w]*[0-9a-zA-Z]\\.){1,300}[a-zA-Z]{2,9})$";
      String myMatch = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$"; // zwei

      if (!neueEmail.matches(myMatch)) {
        String error = "Person-Profil-" + aktion + ": "
            + "Illegale Emailadresse sieht komisch aus: '" + neueEmail + "'";
        Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
        return error;
      }

      if (persons == null) {
        String error = "Person-Profil-" + aktion
            + ": keine Mitglieder gefunden. Daten.project.getPersons() ";
        Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
        return error;
      }
      long now = System.currentTimeMillis();
      PersonRecord otherPerson = persons.getPersonWithEMail(neueEmail, now);
      if (otherPerson != null &&
          !otherPerson.getId().equals(person.getId()) &&
          !otherPerson.getMembershipNo().equals(person.getMembershipNo())) {
        String error = "Person-Profil-" + aktion + ": Die Email '" + neueEmail
            + "' ist bereits vergeben: an " + otherPerson.getFirstLastName();
        Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
        return error;
      }
    }

    person.setErlaubnisEmail(!neueEmail.isBlank());
    person.setEmail(neueEmail);

    if (persons == null) {
      String error = "Person-Profil-" + aktion
          + ": keine Mitglieder gefunden. Daten.project.getPersons() ";
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }
    try {
      persons.data().update(person);
      String info = "Person-Profil-" + aktion + ": " + person.getFirstLastName()
          + " hat nun die Email '" + person.getEmail()
          + "' und die Erlaubnis: " + person.isErlaubtEmail();
      Logger.log(Logger.INFO, Logger.MSG_ABF_INFO, info);
      return info;
    } catch (EfaException e2) {
      String error = "Person-Profil-" + aktion + ": e2 " + e2.getLocalizedMessage();
      Logger.log(Logger.ERROR, Logger.MSG_ABF_ERROR, error);
      return error;
    }
  }

  private String performChangePersonPhoneNrRequest(Map<String, String> strMap,
      Persons persons, PersonRecord person) {
    String aktion = strMap.get("action"); // "SETPHONENR"
    if (!aktion.equalsIgnoreCase("SETPHONENR") &&
        !aktion.equalsIgnoreCase("phoneNr")) {
      String error = "Person-Profil-Link: keine gültige Aktion angegeben " + aktion;
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }

    String neuesTelefon = strMap.get("telefon");
    if (neuesTelefon == null) {
      neuesTelefon = "";
    } else {
      neuesTelefon = neuesTelefon.trim();
    }

    if (person.isErlaubtTelefon()) {
      if (neuesTelefon.equals(person.getFestnetz1())) {
        String error = "Person-Profil-" + aktion + ": " + person.getFirstLastName()
            + " hat bereits Telefon '" + person.getFestnetz1()
            + "' und Erlaubnis: " + person.isErlaubtTelefon();
        Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
        return error;
      }
      if (neuesTelefon.equals(person.getHandy2())) {
        String error = "Person-Profil-" + aktion + ": " + person.getFirstLastName()
            + " hat bereits Telefon '" + person.getHandy2()
            + "' und Erlaubnis: " + person.isErlaubtTelefon();
        Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
        return error;
      }
    } else {
      if (neuesTelefon.isBlank() && person.getFestnetz1() == null && person.getHandy2() == null) {
        String error = "Person-Profil-" + aktion + ": "
            + person.getFirstLastName() + " hat kein Telefon zum Entfernen. "
            + person.isErlaubtTelefon() + " "
            + person.getFestnetz1() + " " + person.getHandy2();
        Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
        return error;
      }
    }

    if (!neuesTelefon.isBlank()) {
      if (persons == null) {
        String error = "Person-Profil-" + aktion
            + ": keine Mitglieder gefunden. Daten.project.getPersons() ";
        Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
        return error;
      }
      long now = System.currentTimeMillis();
      PersonRecord otherPerson = persons.getPersonWithTelefon(neuesTelefon, now);
      if (otherPerson != null &&
          !otherPerson.getId().equals(person.getId()) &&
          !otherPerson.getMembershipNo().equals(person.getMembershipNo())) {
        String error = "Person-Profil-" + aktion + ": Die Telefonnummer '" + neuesTelefon
            + "' ist bereits vergeben: an " + otherPerson.getFirstLastName();
        Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
        return error;
      }
    }

    String myMatch = Daten.efaConfig.getRegexForHandynummer();
    if (!neuesTelefon.matches(myMatch)) {
      String error = "Person-Profil-" + aktion + ": "
          + International.getString("Telefonnummer bitte mit separater Vorwahl") + " "
          + neuesTelefon;
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }

    person.setErlaubnisTelefon(!neuesTelefon.isBlank());
    person.setFestnetz1(null);
    person.setHandy2(neuesTelefon);

    if (persons == null) {
      String error = "Person-Profil-" + aktion
          + ": keine Mitglieder gefunden. Daten.project.getPersons() ";
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }
    try {
      persons.data().update(person);
      String info = "Person-Profil-" + aktion + ": " + person.getFirstLastName()
          + " hat nun die TelefonNr '" + person.getHandy2()
          + "' und die Erlaubnis: " + person.isErlaubtTelefon();
      Logger.log(Logger.INFO, Logger.MSG_ABF_INFO, info);
      return info;
    } catch (EfaException e2) {
      String error = "Person-Profil-" + aktion + ": e2 " + e2.getLocalizedMessage();
      Logger.log(Logger.ERROR, Logger.MSG_ABF_ERROR, error);
      return error;
    }
  }

  private String performChangePersonShortcutRequest(Map<String, String> strMap,
      Persons persons, PersonRecord person) {
    String aktion = strMap.get("action"); // "SETKÜRZEL"
    if (!aktion.equalsIgnoreCase("SETKÜRZEL")) {
      String error = "Person-Profil-Link: keine gültige Aktion angegeben " + aktion;
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }

    String neuesKürzel = strMap.get("kürzel");
    if (neuesKürzel == null) {
      neuesKürzel = "";
    } else {
      neuesKürzel = neuesKürzel.trim().toLowerCase();
    }

    if (person.isErlaubtKuerzel()) {
      if (neuesKürzel.equals(person.getInputShortcut())) {
        String error = "Person-Profil-" + aktion + ": " + person.getFirstLastName()
            + " hat bereits Kürzel '" + person.getInputShortcut()
            + "' und Erlaubnis: " + person.isErlaubtKuerzel();
        Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
        return error;
      }
    } else {
      if (neuesKürzel.isBlank() && person.getInputShortcut() == null) {
        String error = "Person-Profil-" + aktion + ": " + person.getFirstLastName()
            + " hat kein Kürzel zum Entfernen. "
            + person.isErlaubtKuerzel() + " " + person.getInputShortcut();
        Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
        return error;
      }
    }

    // not blank, check spelling
    if (!neuesKürzel.isBlank()) {
      String myMatch = "\\w+\\.?"; // "^[\"\\\\w+\\\\.?\"]+";
      // String myMatch = "^[A-Za-z0-9]+";
      if (!neuesKürzel.matches(myMatch)) {
        String error = "Person-Profil-" + aktion + ": "
            + "Illegales Kürzel sieht komisch aus: '" + neuesKürzel + "'";
        Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
        return error;
      }

      if (persons == null) {
        String error = "Person-Profil-" + aktion
            + ": keine Mitglieder gefunden. Daten.project.getPersons() ";
        Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
        return error;
      }
      long now = System.currentTimeMillis();
      PersonRecord otherPerson = persons.getPersonWithInputShortcut(neuesKürzel, now);
      if (otherPerson != null &&
          !otherPerson.getId().equals(person.getId()) &&
          !otherPerson.getMembershipNo().equals(person.getMembershipNo())) {
        String error = "Person-Profil-" + aktion + ": Das Kürzel '" + neuesKürzel
            + "' ist bereits vergeben: an " + otherPerson.getFirstLastName();
        Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
        return error;
      }
    }

    person.setErlaubnisKuerzel(!neuesKürzel.isBlank());
    person.setInputShortcut(neuesKürzel);

    if (persons == null) {
      String error = "Person-Profil-" + aktion
          + ": keine Mitglieder gefunden. Daten.project.getPersons() ";
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING, error);
      return error;
    }
    try {
      persons.data().update(person);
      String info = "Person-Profil-" + aktion + ": " + person.getFirstLastName()
          + " hat nun das Kürzel '" + person.getInputShortcut()
          + "' und die Erlaubnis: " + person.isErlaubtKuerzel();
      Logger.log(Logger.INFO, Logger.MSG_ABF_INFO, info);
      return info;
    } catch (EfaException e2) {
      String error = "Person-Profil-" + aktion + ": e2 " + e2.getLocalizedMessage();
      Logger.log(Logger.ERROR, Logger.MSG_ABF_ERROR, error);
      return error;
    }
  }

  private String checkHashId(String strHashId) {
    if (strHashId == null) {
      return "checkHashId: kein Code HashId " + strHashId;
    }
    strHashId = strHashId.replace("'", "");
    if (strHashId.isEmpty()) {
      return "checkHashId: leerer Code HashId " + strHashId;
    }

    // hashId = Reservierung suchen // hashId = code prüfen
    BoatReservations boatReservations = Daten.project.getBoatReservations(false);
    BoatReservationRecord[] brrArray = boatReservations.findBoatReservationsByHashId(strHashId);
    if (brrArray == null || brrArray.length == 0) {
      return "checkHashId: keine Reservierung mit hashId " + strHashId + " gefunden. " + brrArray;
    }
    BoatReservationRecord brr = brrArray[0];
    if (brr == null) {
      return "checkHashId: unbekannte Reservierung1 " + strHashId;
    }

    return null;
  }

  private Map<String, String> getStringMap(String folderTodo) {
    Map<String, String> stringMap = new HashMap<String, String>();
    try {
      List<Path> listTxtFiles = new ArrayList<Path>();
      DirectoryStream<Path> stream = Files.newDirectoryStream(Path.of(folderTodo), "*.{txt,json}");
      stream.forEach(listTxtFiles::add);
      listTxtFiles.sort(Comparator.comparing(Path::toString));
      if (listTxtFiles.isEmpty()) {
        return stringMap;
      }

      // erste Datei rausnehmen (älteste) (nur eine)
      Path firstFilename = listTxtFiles.get(0);
      Charset charset = Charset.defaultCharset();
      List<String> stringList = Files.readAllLines(firstFilename, charset);

      if (!stringList.isEmpty()) {
        for (String string : stringList) {
          String[] array = string.split("=");
          if (array.length > 1) {
            stringMap.put(array[0], array[1]);
          }
        }
      }

      // Datei nach done=backup verschieben into backup
      Path targetPath = Path
          .of(Daten.efaBakDirectory + "efa.linkedReservations" + Daten.fileSep);
      Path targetFilename = targetPath.resolve(firstFilename.getFileName());
      Files.move(firstFilename, targetFilename, StandardCopyOption.REPLACE_EXISTING);

    } catch (IOException e) {
      Logger.log(Logger.ERROR, Logger.MSG_DATAADM_RECORDDELETED, e);
    }
    return stringMap;
  }

  private void checkForUnreadMessages() {
    if (Logger.isTraceOn(Logger.TT_BACKGROUND, 8)) {
      Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
          "EfaBoathouseBackgroundTask: checkForUnreadMessages()");
    }
    boolean admin = false;
    boolean boatmaintenance = false;

    Messages messages = (Daten.project != null ? Daten.project.getMessages(false) : null);

    if (messages != null) {
      // durchsuche die letzten 50 Nachrichten nach ungelesenen (aus Performancegründen immer nur
      // die letzen 50)
      int i = 0;
      try {
        DataKeyIterator it = messages.data().getStaticIterator();
        DataKey<?, ?, ?> k = it.getLast();
        while (k != null) {
          MessageRecord msg = (MessageRecord) messages.data().get(k);
          if (msg != null && !msg.getRead()) {
            if (msg.getTo().equals(MessageRecord.TO_ADMIN)) {
              admin = true;
            }
            if (msg.getTo().equals(MessageRecord.TO_BOATMAINTENANCE)) {
              boatmaintenance = true;
            }
          }
          if (++i == 50 || (admin && boatmaintenance)) {
            break;
          }
          k = it.getPrev();
        }
      } catch (Exception e) {
        Logger.logdebug(e);
      }
    }
    efaBoathouseFrame.setUnreadMessages(admin, boatmaintenance);
  }

  private void checkForExitOrRestart() {
    if (Logger.isTraceOn(Logger.TT_BACKGROUND, 8)) {
      Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
          "EfaBoathouseBackgroundTask: checkForExitOrRestart()");
    }
    // automatisches, zeitgesteuertes Beenden von efa ?
    if (Daten.efaConfig.getValueEfaDirekt_exitTime().isSet()
        && System.currentTimeMillis() > Daten.efaStartTime + (Daten.AUTO_EXIT_MIN_RUNTIME + 1) * 60
            * 1000) {
      date.setTime(System.currentTimeMillis());
      cal.setTime(date);
      int now = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
      int exitTime = Daten.efaConfig.getValueEfaDirekt_exitTime().getHour() * 60
          + Daten.efaConfig.getValueEfaDirekt_exitTime().getMinute();
      if ((now >= exitTime && now < exitTime + Daten.AUTO_EXIT_MIN_RUNTIME) ||
          (now + (24 * 60) >= exitTime
              && now + (24 * 60) < exitTime + Daten.AUTO_EXIT_MIN_RUNTIME)) {
        Logger.log(Logger.INFO, Logger.MSG_EVT_TIMEBASEDEXIT,
            International.getString("Eingestellte Uhrzeit zum Beenden von efa erreicht"));
        if (System.currentTimeMillis() - efaBoathouseFrame
            .getLastUserInteraction() < Daten.AUTO_EXIT_MIN_LAST_USED * 60 * 1000) {
          String loggertxt = International.getMessage(
              "Beenden von efa wird verzögert, da efa innerhalb der letzten {n} Minuten noch benutzt wurde ...",
              Daten.AUTO_EXIT_MIN_LAST_USED);
          Logger
              .log(
                  Logger.INFO,
                  Logger.MSG_EVT_TIMEBASEDEXITDELAY,
                  loggertxt);
        } else {
          EfaExitFrame.exitEfa(International.getString("Zeitgesteuertes Beenden von efa"), false,
              EfaBoathouseFrame.EFA_EXIT_REASON_TIME);
        }
      }
    }

    // automatisches Beenden nach Inaktivität ?
    if (Daten.efaConfig.getValueEfaDirekt_exitIdleTime() > 0
        && System.currentTimeMillis() - efaBoathouseFrame.getLastUserInteraction() > Daten.efaConfig
            .getValueEfaDirekt_exitIdleTime() * 60 * 1000) {
      Logger.log(Logger.INFO, Logger.MSG_EVT_INACTIVITYBASEDEXIT,
          International.getString("Eingestellte Inaktivitätsdauer zum Beenden von efa erreicht"));
      EfaExitFrame.exitEfa(International.getString("Zeitgesteuertes Beenden von efa"), false,
          EfaBoathouseFrame.EFA_EXIT_REASON_IDLE);
    }

    // automatischer, zeitgesteuerter Neustart von efa ?
    if (Daten.efaConfig.getValueEfaDirekt_restartTime().isSet()
        && System.currentTimeMillis() > Daten.efaStartTime + (Daten.AUTO_EXIT_MIN_RUNTIME + 1) * 60
            * 1000) {
      date.setTime(System.currentTimeMillis());
      cal.setTime(date);
      int now = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
      int restartTime = Daten.efaConfig.getValueEfaDirekt_restartTime().getHour() * 60
          + Daten.efaConfig.getValueEfaDirekt_restartTime().getMinute();
      if ((now >= restartTime && now < restartTime + Daten.AUTO_EXIT_MIN_RUNTIME)
          || (now + (24 * 60) >= restartTime && now + (24 * 60) < restartTime
              + Daten.AUTO_EXIT_MIN_RUNTIME)) {
        Logger.log(Logger.INFO, Logger.MSG_EVT_TIMEBASEDRESTART,
            "Automatischer Neustart von efa (einmal täglich).");
        if (System.currentTimeMillis() - efaBoathouseFrame
            .getLastUserInteraction() < Daten.AUTO_EXIT_MIN_LAST_USED * 60 * 1000) {
          Logger.log(Logger.INFO, Logger.MSG_EVT_TIMEBASEDRESTARTDELAY,
              "Neustart von efa wird verzögert, da efa innerhalb der letzten "
                  + Daten.AUTO_EXIT_MIN_LAST_USED + " Minuten noch benutzt wurde ...");
        } else {
          EfaExitFrame.exitEfa("Automatischer Neustart von efa", true,
              EfaBoathouseFrame.EFA_EXIT_REASON_AUTORESTART);
        }
      }
    }

    // Neustart wegen Datei im File-System: pleaseReboot.txt, efa.new.jar, auswertung.csv
    String gefundeneDatei = getDateiImFileSystem();
    if (!gefundeneDatei.isEmpty()) {
      Logger.log(Logger.INFO, Logger.MSG_EVT_FILEFOUND,
          International.getString("Neustart forciert wegen: ") + gefundeneDatei);
      EfaExitFrame.exitEfa(International.getString("Forcierter Neustart von efa"), true,
          EfaBoathouseFrame.EFA_EXIT_REASON_FILEFOUND);
    }

  }

  private String getDateiImFileSystem() {
    // if (pleaseReboot.txt) {
    // if (efa.new.jar) {
    // if (auswertung.csv) {
    String filenameRestartEfaCmd = Daten.PLEASE_RESTART_EFA;
    String[] filenames = {
        Daten.efaBaseConfig.efaUserDirectory + filenameRestartEfaCmd, // forcedRestartInEFA2
        Daten.userHomeDir + filenameRestartEfaCmd, // forcedRestartInHome
        Daten.efaProgramDirectory + Daten.EFA_NEW_JAR, // newEfaVersionToInstall
        Daten.userHomeDir + "Downloads/Auswertung.Sewobe/" + "auswertung.csv", // newPersonsFromSewobe
    };
    for (String filename : filenames) {
      if (filename.startsWith(Daten.efaProgramDirectory) &&
          filename.contains("efa220oh")) {
        continue; // Entwicklungs-PC
      }
      if (!(new java.io.File(filename)).isFile()) {
        continue;
      }
      if (filename.endsWith(filenameRestartEfaCmd)) {
        new java.io.File(filename).delete();
      }
      return filename;
    }
    return ""; // nothing found
  }

  private void checkForLockEfa() {
    if (Logger.isTraceOn(Logger.TT_BACKGROUND, 8)) {
      Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
          "EfaBoathouseBackgroundTask: checkForLockEfa()");
    }
    if (Daten.efaConfig != null) {
      if (Daten.efaConfig.getValueEfaDirekt_locked()) {
        efaBoathouseFrame.lockEfa();
        return;
      }
      setEfaLockBegin(Daten.efaConfig.getValueEfaDirekt_lockEfaFromDatum(),
          Daten.efaConfig.getValueEfaDirekt_lockEfaFromZeit());
    }

    if (lockEfa != null) {
      date.setTime(System.currentTimeMillis());
      cal.setTime(date);
      if (cal.after(lockEfa) && efaBoathouseFrame != null) {
        efaBoathouseFrame.lockEfa();
        lockEfa = null;
      }
    }
  }

  private void checkForAutoCreateNewLogbook() {
    if (Logger.isTraceOn(Logger.TT_BACKGROUND, 8)) {
      Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
          "EfaBoathouseBackgroundTask: checkForAutoCreateNewLogbook()");
    }
    try {
      if (Daten.project != null && Daten.project.isOpen()) {
        DataTypeDate date = Daten.project.getAutoNewLogbookDate();
        if (date != null && date.isSet()) {
          DataTypeDate today = DataTypeDate.today();
          if (today.isAfterOrEqual(date)) {
            autoOpenNewLogbook();
            if (today.getDifferenceDays(date) >= 7) {
              // we only delete the logswitch data after 7 days to also give
              // all remote clients a chance to change the logbook; otherwise,
              // they wouldn't be able to see the configured date any more and
              // would never change the logbook
              Daten.project.setAutoNewLogbookDate(null);
              Daten.project.setAutoNewLogbookName(null);

            }
          }
        }
      }
    } catch (Exception e) {
      // can crash when project is currently being opened
      Logger.logdebug(e);
    }
  }

  private void checkAlwaysInFront() {
    if (Logger.isTraceOn(Logger.TT_BACKGROUND, 8)) {
      Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
          "EfaBoathouseBackgroundTask: checkAlwaysInFront()");
    }
    if (Daten.efaConfig.getValueEfaDirekt_immerImVordergrund() &&
        efaBoathouseFrame != null
        && Dialog.frameCurrent() == efaBoathouseFrame) {
      Window[] windows = efaBoathouseFrame.getOwnedWindows();
      boolean topWindow = true;
      if (windows != null) {
        for (Window window : windows) {
          if (window != null && window.isVisible()) {
            topWindow = false;
          }
        }
      }
      if (topWindow && Daten.efaConfig.getValueEfaDirekt_immerImVordergrundBringToFront()) {
        efaBoathouseFrame.bringFrameToFront();
      }
    }
  }

  private void checkFocus() {
    if (Logger.isTraceOn(Logger.TT_BACKGROUND, 8)) {
      Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
          "EfaBoathouseBackgroundTask: checkFocus()");
    }
    if (this.efaBoathouseFrame != null
        && this.efaBoathouseFrame.getFocusOwner() == this.efaBoathouseFrame) {
      // das Frame selbst hat den Fokus: Das soll nicht sein! Gib einer Liste den Fokus!
      efaBoathouseFrame.boatListRequestFocus(0);
    }
  }

  private void checkMemory() {
    if (Logger.isTraceOn(Logger.TT_BACKGROUND, 8)) {
      Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
          "EfaBoathouseBackgroundTask: checkMemory()");
    }
    try {
      // System.gc(); // !!! ONLY ENABLE FOR DEBUGGING PURPOSES !!!
      if (de.nmichael.efa.java15.Java15.isMemoryLow(Daten.MIN_FREEMEM_PERCENTAGE,
          Daten.WARN_FREEMEM_PERCENTAGE)) {
        efaBoathouseFrame.exitOnLowMemory("EfaBoathouseBackgroundTask: MemoryLow", false);
      }
    } catch (UnsupportedClassVersionError e) {
      EfaUtil.foo();
    } catch (NoClassDefFoundError e) {
      EfaUtil.foo();
    }
  }

  private void checkWarnings() {
    if (Logger.isTraceOn(Logger.TT_BACKGROUND, 8)) {
      Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
          "EfaBoathouseBackgroundTask: checkWarnings()");
    }
    // WARNINGs aus Logfile an Admins verschicken
    if (System.currentTimeMillis() >= Daten.efaConfig.getValueEfaDirekt_bnrWarning_lasttime() + 7l
        * 24l * 60l * 60l * 1000l
        && (Daten.efaConfig.getValueEfaDirekt_bnrWarning_admin() || Daten.efaConfig
            .getValueEfaDirekt_bnrWarning_bootswart())
        && Daten.efaLogfile != null) {
      mailWarnings();
    }
  }

  private void checkUnfixedBoatDamages() {
    if (!isProjectOpen || !isLocalProject) {
      return;
    }
    if (Logger.isTraceOn(Logger.TT_BACKGROUND, 8)) {
      Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
          "EfaBoathouseBackgroundTask: checkUnfixedBoatDamages()");
    }
    BoatDamages boatDamages = (Daten.project != null ? Daten.project.getBoatDamages(false) : null);
    Messages messages = (Daten.project != null ? Daten.project.getMessages(false) : null);
    if (boatDamages == null || messages == null) {
      return;
    }

    long now = System.currentTimeMillis();
    long last = (Daten.efaConfig != null ? Daten.efaConfig.getValueLastBoatDamageReminder() : -1);
    if (last == -1 || now - BOAT_DAMAGE_REMINDER_INTERVAL > last) {
      boolean damagesOlderThanAWeek = false;
      Vector<DataKey<?, ?, ?>> openDamages = new Vector<DataKey<?, ?, ?>>();
      try {
        DataKeyIterator it = boatDamages.data().getStaticIterator();
        for (DataKey<?, ?, ?> k = it.getFirst(); k != null; k = it.getNext()) {
          if (boatDamages == null) {
            continue;
          }
          BoatDamageRecord damage = (BoatDamageRecord) boatDamages.data().get(k);
          if (!damage.getFixed()) {
            BoatRecord r = damage.getBoatRecord();
            if (r != null && r.isValidAt(System.currentTimeMillis())) {
              openDamages.add(k);
              if (damage.getReportDate() != null
                  && damage.getReportTime() != null
                  && damage.getReportDate().isSet()
                  && damage.getReportTime().isSet()
                  && damage.getReportDate().getTimestamp(damage.getReportTime()) < now
                      - BOAT_DAMAGE_REMINDER_INTERVAL) {
                damagesOlderThanAWeek = true;
              }
            }
          }

        }

        if (damagesOlderThanAWeek) {
          StringBuilder s = new StringBuilder();
          s.append(International.getMessage("Es liegen {count} offene Bootsschäden vor:",
              openDamages.size()) + "\n\n");
          for (DataKey<?, ?, ?> k : openDamages) {
            if (boatDamages == null) {
              continue;
            }
            BoatDamageRecord damage = (BoatDamageRecord) boatDamages.data().get(k);
            s.append(damage.getCompleteDamageInfo() + "\n");
          }
          s.append(International
              .getString("Sollten die Schäden bereits behoben sein, so markiere sie bitte "
                  + "in efa als behoben."));
          messages.createAndSaveMessageRecord(MessageRecord.TO_BOATMAINTENANCE,
              International.getString("Offene Bootsschäden"),
              s.toString());
          Daten.efaConfig.setValueLastBoatDamageReminder(now);
        }
      } catch (Exception e) {
        Logger.logdebug(e);
      }
    }
  }

  private void remindAdminOfLogbookSwitch() {
    try {
      if (Daten.project != null && Daten.project.isOpen()) {
        DataTypeDate date = Daten.project.getAutoNewLogbookDate();
        if (date == null || !date.isSet()) {
          Logbook currentLogbook = efaBoathouseFrame.getLogbook();
          if (currentLogbook != null && currentLogbook.getEndDate() != null &&
              currentLogbook.getEndDate().isSet()) {
            DataTypeDate today = DataTypeDate.today();
            if (today.isBefore(currentLogbook.getEndDate()) &&
                today.getDifferenceDays(currentLogbook.getEndDate()) < 31) {
              String lastReminderForLogbook = Daten.efaConfig
                  .getValueEfaBoathouseChangeLogbookReminder();
              if (!currentLogbook.getName().equals(lastReminderForLogbook)) {
                // ok, it's due for a reminder
                Daten.project
                    .getMessages(false)
                    .createAndSaveMessageRecord(
                        MessageRecord.TO_ADMIN,
                        International.getString("Erinnerung an Fahrtenbuchwechsel"),
                        International
                            .getMessage(
                                "Der Gültigkeitszeitraum des aktuellen Fahrtenbuchs {name} endet am {datum}.",
                                currentLogbook.getName(), currentLogbook.getEndDate().toString())
                            + "\n"
                            +
                            International
                                .getString(
                                    "Um anschließend automatisch ein neues Fahrtenbuch zu öffnen, erstelle bitte "
                                        +
                                        "im Admin-Modus ein neues Fahrtenbuch und aktiviere den automatischen Fahrtenbuchwechsel."));
                Daten.efaConfig.setValueEfaBoathouseChangeLogbookReminder(currentLogbook.getName());
              }
            }
          }
        }
      }
    } catch (Exception e) {
      // can crash when project is currently being opened
      Logger.logdebug(e);
    }
  }

  private void autoOpenNewLogbook() {
    if (Daten.project == null || !Daten.project.isOpen()) {
      return;
    }
    String newLogbookName = Daten.project.getAutoNewLogbookName();
    if (newLogbookName == null || newLogbookName.length() == 0) {
      return;
    }
    Logbook currentLogbook = efaBoathouseFrame.getLogbook();
    if (currentLogbook == null) {
      return;
    }
    if (newLogbookName.equals(currentLogbook.getName())) {
      // we have already changed the logbook --> nothing to do
      return;
    }

    // Logswitch Key (to identify whether we already switched logbooks)
    String date = (Daten.project.getAutoNewLogbookDate() != null &&
        Daten.project.getAutoNewLogbookDate().isSet()
            ? Daten.project.getAutoNewLogbookDate().toString()
            : "");
    String key = newLogbookName + "~" + date;
    if (key.equals(Daten.project.getLastLogbookSwitch())) {
      // it seems the admin has explicitly opened another (maybe the previous) logbook
      // again, but we have already completed the switch into the configured logbook
      // in this efa instance.
      return;
    }

    Logger.log(Logger.INFO, Logger.MSG_EVT_AUTOSTARTNEWLOGBOOK,
        International.getString("Fahrtenbuchwechsel wird begonnen ..."));

    BoatStatus boatStatus = Daten.project.getBoatStatus(false);
    long lockLogbook = -1;
    long lockStatus = -1;
    try {
      Logbook newLogbook = Daten.project.getLogbook(newLogbookName, false);

      // Step 1: Try to find and open new Logbook
      if (newLogbook == null) {
        Logger.log(Logger.ERROR, Logger.MSG_ERR_AUTOSTARTNEWLOGBOOK,
            LogString.fileNotFound(newLogbookName, International.getString("Fahrtenbuch")));
        throw new Exception("New Logbook not found");
      }

      // Step 2: Abort open Sessions (only for local project)
      boolean sessionsAborted = false;
      if (Daten.project.getProjectStorageType() != IDataAccess.TYPE_EFA_REMOTE) {
        Vector<BoatStatusRecord> boatsOnTheWater = boatStatus
            .getBoats(BoatStatusRecord.STATUS_ONTHEWATER);
        if (boatsOnTheWater.size() > 0) {
          lockStatus = boatStatus.data().acquireGlobalLock();
          lockLogbook = currentLogbook.data().acquireGlobalLock();
          sessionsAborted = true;
          Logger.log(Logger.INFO, Logger.MSG_EVT_AUTOSTARTNEWLBSTEP,
              International.getString("Offene Fahrten werden abgebrochen ..."));
          for (int i = 0; i < boatsOnTheWater.size(); i++) {
            BoatStatusRecord sr = boatsOnTheWater.get(i);
            LogbookRecord r = null;
            if (sr.getEntryNo() != null && sr.getEntryNo().isSet()) {
              r = currentLogbook.getLogbookRecord(sr.getEntryNo());
              r.setSessionIsOpen(false);
              currentLogbook.data().update(r, lockLogbook);
            }
            sr.setEntryNo(null);
            sr.setCurrentStatus(sr.getBaseStatus());
            boatStatus.data().update(sr, lockStatus);
            EfaBaseFrame.logBoathouseEvent(Logger.INFO, Logger.MSG_EVT_TRIPABORT,
                International.getString("Fahrtabbruch"), r);
            boatStatus.data().releaseGlobalLock(lockStatus);
            lockStatus = -1;
            currentLogbook.data().releaseGlobalLock(lockLogbook);
            lockLogbook = -1;
          }
        }
      }

      // Step 3: Activate the new Logbook
      if (efaBoathouseFrame.openLogbook(newLogbook.getName())) {
        Logger
            .log(Logger.INFO, Logger.MSG_EVT_AUTOSTARTNEWLBDONE,
                LogString.operationSuccessfullyCompleted(International
                    .getString("Fahrtenbuchwechsel")));
        Daten.project.setLastLogbookSwitch(key);
      } else {
        throw new Exception("Failed to open new Logbook");
      }

      Messages messages = Daten.project.getMessages(false);
      messages
          .createAndSaveMessageRecord(
              MessageRecord.TO_ADMIN,
              International.getString("Fahrtenbuchwechsel"),
              International
                  .getString("efa hat soeben wie konfiguriert ein neues Fahrtenbuch geöffnet.")
                  + "\n"
                  + International
                      .getMessage(
                          "Das neue Fahrtenbuch heißt {name} und ist gültig vom {fromdate} bis {todate}.",
                          newLogbook.getName(), newLogbook.getStartDate().toString(), newLogbook
                              .getEndDate().toString())
                  + "\n"
                  + LogString.operationSuccessfullyCompleted(International
                      .getString("Fahrtenbuchwechsel"))
                  + "\n\n"
                  + (sessionsAborted ? International
                      .getString(
                          "Zum Zeitpunkt des Fahrtenbuchwechsels befanden sich noch einige Boote "
                              + "auf dem Wasser. Diese Fahrten wurden ABGEBROCHEN. Die abgebrochenen "
                              + "Fahrten sind in der Logdatei verzeichnet.")
                      : ""));
      EfaUtil.sleep(500);
      efaBoathouseFrame.updateBoatLists(true);
      EfaUtil.sleep(500);
      interrupt();
    } catch (Exception e) {
      Logger.logdebug(e);
      Logger.log(Logger.ERROR, Logger.MSG_ERR_AUTOSTARTNEWLOGBOOK,
          LogString.operationAborted(International.getString("Fahrtenbuchwechsel")));
      Messages messages = Daten.project.getMessages(false);
      messages
          .createAndSaveMessageRecord(
              MessageRecord.TO_ADMIN,
              International.getString("Fahrtenbuchwechsel"),
              International
                  .getString(
                      "efa hat soeben versucht, wie konfiguriert ein neues Fahrtenbuch anzulegen.")
                  + "\n"
                  + International.getString("Bei diesem Vorgang traten jedoch FEHLER auf.")
                  + "\n\n"
                  + International
                      .getString(
                          "Ein Protokoll ist in der Logdatei (Admin-Modus: Logdatei anzeigen) zu finden."));
    } finally {
      if (boatStatus != null && lockStatus >= 0) {
        boatStatus.data().releaseGlobalLock(lockStatus);
      }
      if (currentLogbook != null && lockLogbook >= 0) {
        currentLogbook.data().releaseGlobalLock(lockLogbook);
      }
    }

  }
}
