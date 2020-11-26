/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.data.sync;

// @i18n complete

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.JDialog;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.data.BoatRecord;
import de.nmichael.efa.data.Boats;
import de.nmichael.efa.data.Destinations;
import de.nmichael.efa.data.Logbook;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.data.PersonRecord;
import de.nmichael.efa.data.Persons;
import de.nmichael.efa.data.ProjectRecord;
import de.nmichael.efa.data.Waters;
import de.nmichael.efa.data.WatersRecord;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataKeyIterator;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.gui.BaseTabbedDialog;
import de.nmichael.efa.gui.EfaConfigDialog;
import de.nmichael.efa.gui.ProgressDialog;
import de.nmichael.efa.gui.dataedit.ProjectEditDialog;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;
import de.nmichael.efa.util.ProgressTask;

public class KanuEfbSyncTask extends ProgressTask {

  private static final int DEBUG_MARK_SIZE = 10 * 1024 * 1024;

  private AdminRecord admin;
  private Logbook logbook;
  private String loginurl;
  private String cmdurl;
  private String username;
  private String password;
  private HttpCookie sessionCookie;
  private long lastSync;
  private long thisSync;
  private boolean loggedIn = false;
  private boolean successfulCompleted = false;
  private int countSyncUsers = 0;
  private int countSyncBoats = 0;
  private int countSyncWaters = 0;
  private int countSyncTrips = 0;
  private int countWarnings = 0;
  private int countErrors = 0;

  TrustManager[] trustAllCerts = new TrustManager[] {
      new X509TrustManager() {

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
          return null;
        }

        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] certs,
            String authType) {
          // No need to implement.
        }

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] certs,
            String authType) {
          // No need to implement.
        }
      }
  };

  public KanuEfbSyncTask(Logbook logbook, AdminRecord admin) {
    super();
    this.admin = admin;
    getConfigValues();
    this.logbook = logbook;
  }

  private void getConfigValues() {
    this.loginurl = Daten.efaConfig.getValueKanuEfb_urlLogin();
    this.cmdurl = Daten.efaConfig.getValueKanuEfb_urlRequest();
    this.username = Daten.project.getClubKanuEfbUsername();
    this.password = Daten.project.getClubKanuEfbPassword();
    this.lastSync = Daten.project.getClubKanuEfbLastSync();
    if (this.lastSync == IDataAccess.UNDEFINED_LONG) {
      this.lastSync = 0;
    }
  }

  private void buildRequestHeader(StringBuilder s, String requestName) {
    s.append("<?xml version='1.0' encoding='UTF-8' ?>\n");
    s.append("<xml>\n");
    s.append("<request command=\"" + requestName + "\">\n");
  }

  private void buildRequestFooter(StringBuilder s) {
    s.append("</request>\n");
    s.append("</xml>\n");
  }

  private KanuEfbXmlResponse sendRequest(String request, boolean expectResponse) throws Exception {
    if (Logger.isTraceOn(Logger.TT_SYNC)) {
      logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCDEBUG, "Sende Synchronisierungs-Anfrage an "
          + cmdurl + ":\n" + request);
    }
    URL url = new URL(this.cmdurl);
    URLConnection connection = url.openConnection();
    connection.setDoOutput(true);
    connection.setDoInput(true);
    connection.setUseCaches(false);
    connection.setAllowUserInteraction(true);
    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    connection.setRequestProperty("Cookie", (sessionCookie != null ? sessionCookie.toString()
        : "null"));
    OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
    out.write("xmlCode=" + URLEncoder.encode(request, "UTF-8"));
    out.flush();
    out.close();

    if (expectResponse) {
      try {
        return getResponse(connection, new BufferedInputStream(connection.getInputStream()));
      } catch (Exception e) {
        logInfo(Logger.ERROR, Logger.MSG_SYNC_SYNCDEBUG,
            "Fehler bei Kommunikation mit " + cmdurl + ": " + e.getMessage());
        return null;
      }
    } else {
      return null;
    }
  }

  private KanuEfbXmlResponse getResponse(URLConnection connection, BufferedInputStream in) {
    if (Logger.isTraceOn(Logger.TT_SYNC)) {
      try {
        in.mark(DEBUG_MARK_SIZE);
        logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCDEBUG, "Antwort von Kanu-eFB:");
        logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCDEBUG, "    -- HEADER START --");
        Map<String, List<String>> m = connection.getHeaderFields();
        for (String header : m.keySet()) {
          logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCDEBUG,
              "    " + header + "=" + connection.getHeaderField(header));
        }
        logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCDEBUG, "    -- HEADER END --");
        BufferedReader buf = new BufferedReader(new InputStreamReader(in));
        String s;
        logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCDEBUG, "    -- RESPONSE START --");
        while ((s = buf.readLine()) != null) {
          logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCDEBUG, "   " + s);
        }
        logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCDEBUG, "    -- RESPONSE END --");
        in.reset();
      } catch (Exception e) {
        Logger.log(e);
      }
    }

    KanuEfbXmlResponse response = null;
    try {
      XMLReader parser = EfaUtil.getXMLReader();
      response = new KanuEfbXmlResponse(this);
      parser.setContentHandler(response);
      parser.parse(new InputSource(in));
    } catch (Exception e) {
      Logger.log(e);
      if (Logger.isTraceOn(Logger.TT_SYNC)) {
        logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCDEBUG, "Exception:" + e.toString());
      }
      response = null;
    }

    if (Logger.isTraceOn(Logger.TT_SYNC) && response != null) {
      response.printAll();
    }

    return response;
  }

  private boolean login() {
    try {
      loggedIn = false;
      logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCDEBUG, "Login auf " + this.loginurl
          + " mit Benutzername " + this.username + " ...");
      CookieManager manager = new CookieManager();
      manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
      CookieHandler.setDefault(manager);
      URL url = new URL(this.loginurl);
      URLConnection connection = url.openConnection();

      connection.setDoOutput(true);
      connection.setDoInput(true);
      connection.setUseCaches(false);
      connection.setAllowUserInteraction(true);
      connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
      UUID projectId;
      try {
        projectId = Daten.project.getProjectId();
      } catch (Exception e) {
        logInfo(Logger.ERROR, Logger.MSG_SYNC_ERRORCONFIG, "No project ID found: " + e.toString());
        projectId = null;
      }
      String projectName = EfaUtil.escapeHtmlGetString(Daten.project.getProjectName());
      String clubName = EfaUtil.escapeHtmlGetString(Daten.project.getClubName());
      out.write("username=" + username +
          "&password=" + password +
          (projectId != null ? "&project=" + projectId.toString() : "") +
          (projectName != null && projectName.length() > 0 ? "&projectname=" + projectName : "") +
          (clubName != null && clubName.length() > 0 ? "&clubname=" + clubName : ""));
      out.flush();
      out.close();

      KanuEfbXmlResponse response = getResponse(connection,
          new BufferedInputStream(connection.getInputStream()));
      CookieStore cookieJar = manager.getCookieStore();
      List<HttpCookie> cookies = cookieJar.getCookies();
      for (HttpCookie cookie : cookies) {
        if (Logger.isTraceOn(Logger.TT_SYNC)) {
          logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCDEBUG, "Session Cookie: " + cookie);
        }
        sessionCookie = cookie;
      }

      int retCode = (response == null ? -1
          : EfaUtil
              .stringFindInt(response.getValue(0, "code"), -1));
      if (retCode != 1) {
        String msg = (response == null ? "unbekannt" : response.getValue(0, "message"));
        logInfo(Logger.ERROR, Logger.MSG_SYNC_ERRORLOGIN, "Login fehlgeschlagen: Code " + retCode
            + " (" + msg + ")");
        return false;
      }

    } catch (Exception e) {
      Logger.logdebug(e);
      logInfo(Logger.ERROR, Logger.MSG_SYNC_ERRORLOGIN, "Login fehlgeschlagen: " + e.toString());
      return false;
    }
    loggedIn = true;
    if (Logger.isTraceOn(Logger.TT_SYNC)) {
      logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCDEBUG, "Login erfolgreich.");
    }
    return true;
  }

  private boolean handleSyncUserResponse(KanuEfbXmlResponse response) throws Exception {
    Persons persons = Daten.project.getPersons(false);
    if (response != null && response.isResponseOk("SyncUsers")) {
      logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Synchronisierungs-Antwort erhalten für "
          + response.getNumberOfRecords() + " Personen ...");
      for (int i = 0; i < response.getNumberOfRecords(); i++) {
        Hashtable<String, String> fields = response.getFields(i);
        boolean ok = false;
        String personName = "<unknown>";
        String firstName = fields.get("firstname");
        String lastName = fields.get("lastname");
        String dateOfBirth = fields.get("dateofbirth");
        String efbId = fields.get("id");
        if (firstName != null && lastName != null) {
          firstName = firstName.trim();
          lastName = lastName.trim();
          personName = PersonRecord.getFullName(firstName, lastName, "", false);
          PersonRecord[] plist = persons.getPersons(personName, thisSync);
          PersonRecord p = (plist != null && plist.length == 1 ? plist[0] : null);

          // try to match person on date of birth
          for (int pi = 0; p == null && plist != null && pi < plist.length; pi++) {
            if (plist[pi].getBirthday() != null && dateOfBirth != null
                && plist[pi].getBirthday().isSet()) {
              DataTypeDate bday = DataTypeDate.parseDate(dateOfBirth);
              if (bday != null && bday.isSet()) {
                if (bday.equals(plist[pi].getBirthday())) {
                  p = plist[pi];
                } else {
                  if (plist[i].getBirthday().getDay() < 1
                      && plist[i].getBirthday().getMonth() < 1
                      && plist[i].getBirthday().getYear() == bday.getYear()) {
                    p = plist[pi];
                  }
                }
              }
            }
          }
          if (efbId != null && p != null) {
            efbId = efbId.trim();
            ok = true;
          }
        }
        if (Logger.isTraceOn(Logger.TT_SYNC)) {
          if (ok) {
            logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCINFO,
                "  Synchronisierungs-Antwort für Person: " + personName + " (EfbId=" + efbId + ")");
          } else {
            logInfo(Logger.DEBUG, Logger.MSG_SYNC_WARNINCORRECTRESPONSE,
                "  Synchronisierungs-Antwort für unbekannte Person: " + personName);
          }
        }
      }
      logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, countSyncUsers
          + " neue Personen synchronisiert.");
      return true;
    } else {
      logInfo(Logger.ERROR, Logger.MSG_SYNC_ERRORINVALIDRESPONSE,
          "Ungültige Synchronisierungs-Antwort.");
      return false;
    }
  }

  private boolean syncUsers() {
    try {
      logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Synchronisiere Personen ...");

      // ask eFB to sync all users from eFB -> efa
      // this is deprecated
      StringBuilder request = new StringBuilder();
      buildRequestHeader(request, "SyncUsers");
      buildRequestFooter(request);
      logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO,
          "Sende Synchronisierungs-Anfrage für alle Personen ...");
      KanuEfbXmlResponse response = sendRequest(request.toString(), true);
      if (!handleSyncUserResponse(response)) {
        return false;
      }

      // transmit all efa users without eFB ID's to eFB
      /*
       * StringBuilder request = new StringBuilder(); buildRequestHeader(request, "SyncUsers");
       * Persons persons = Daten.project.getPersons(false); int reqCnt = 0; DataKeyIterator it =
       * persons.data().getStaticIterator(); DataKey k = it.getFirst(); while (k != null) {
       * PersonRecord r = (PersonRecord)persons.data().get(k); if (r != null &&
       * r.isValidAt(thisSync) && r.isStatusMember() && (r.getLastModified() > lastSync ||
       * r.getEfbId() == null || r.getEfbId().length() == 0)) { if
       * (Logger.isTraceOn(Logger.TT_SYNC)) { logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCINFO,
       * "  erstelle Synchronisierungs-Anfrage für Person: " + r.getQualifiedName()); }
       * request.append("<person>"); request.append("<firstName>"+r.getFirstName()+"</firstName>");
       * request.append("<lastName>"+r.getLastName()+"</lastName>"); if (r.getBirthday() != null &&
       * r.getBirthday().isSet()) {
       * request.append("<dateOfBirth>"+r.getBirthday().toString()+"</dateOfBirth>"); }
       * request.append("</person>\n"); reqCnt++; } k = it.getNext(); } buildRequestFooter(request);
       * logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Sende Synchronisierungs-Anfrage für " +
       * reqCnt + " Personen ..."); KanuEfbXmlResponse response = sendRequest(request.toString(),
       * true); if (!handleSyncUserResponse(response)) { return false; }
       */
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  private boolean syncBoats() {
    try {
      logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Synchronisiere Boote ...");
      StringBuilder request = new StringBuilder();
      buildRequestHeader(request, "SyncBoats");
      Boats boats = Daten.project.getBoats(false);
      DataKeyIterator it = boats.data().getStaticIterator();
      DataKey k = it.getFirst();
      int reqCnt = 0;
      Hashtable<String, UUID> efaIds = new Hashtable<String, UUID>();
      while (k != null) {
        BoatRecord r = (BoatRecord) boats.data().get(k);
        if (r != null && r.isValidAt(thisSync) && Daten.efaConfig.isCanoeBoatType(r) &&
            (r.getLastModified() > lastSync || r.getEfbId() == null
                || r.getEfbId().length() == 0)) {
          if (Logger.isTraceOn(Logger.TT_SYNC)) {
            logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCINFO,
                "  erstelle Synchronisierungs-Anfrage für Boot: " + r.getQualifiedName());
          }
          request.append("<boat><name>" + r.getQualifiedName() + "</name></boat>\n");
          efaIds.put(r.getQualifiedName(), r.getId());
          reqCnt++;
        }
        k = it.getNext();
      }
      buildRequestFooter(request);

      logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Sende Synchronisierungs-Anfrage für "
          + reqCnt + " Boote ...");
      KanuEfbXmlResponse response = sendRequest(request.toString(), true);
      if (response != null && response.isResponseOk("SyncBoats")) {
        logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Synchronisierungs-Antwort erhalten für "
            + response.getNumberOfRecords() + " Boote ...");
        for (int i = 0; i < response.getNumberOfRecords(); i++) {
          Hashtable<String, String> fields = response.getFields(i);
          boolean ok = false;
          String boatName = fields.get("label");
          String efbId = fields.get("id");
          if (boatName != null) {
            boatName = boatName.trim();
            UUID efaId = efaIds.get(boatName);
            if (efaId != null && efbId != null) {
              BoatRecord b = boats.getBoat(efaId, thisSync);
              efbId = efbId.trim();
              if (b != null) {
                if (!efbId.equals(b.getEfbId())) {
                  b.setEfbId(efbId);
                  boats.data().update(b);
                  countSyncBoats++;
                }
                ok = true;
              }
            }
          }
          if (ok) {
            if (Logger.isTraceOn(Logger.TT_SYNC)) {
              logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCINFO,
                  "  Synchronisierungs-Antwort für Boot: " + boatName + " (EfbId=" + efbId + ")");
            }
          } else {
            logInfo(Logger.WARNING, Logger.MSG_SYNC_WARNINCORRECTRESPONSE,
                "Ungültige Synchronisierungs-Antwort für Boot: " + boatName);
          }
        }
        logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, countSyncBoats
            + " neue Boote synchronisiert.");
      } else {
        logInfo(Logger.ERROR, Logger.MSG_SYNC_ERRORINVALIDRESPONSE,
            "Ungültige Synchronisierungs-Antwort.");
        return false;
      }
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  private boolean syncWaters() {
    try {
      logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Synchronisiere Gewässer ...");
      StringBuilder request = new StringBuilder();
      buildRequestHeader(request, "SyncWaters");
      Waters waters = Daten.project.getWaters(false);
      DataKeyIterator it = waters.data().getStaticIterator();
      DataKey k = it.getFirst();
      int reqCnt = 0;
      Hashtable<String, UUID> efaIds = new Hashtable<String, UUID>();
      while (k != null) {
        WatersRecord r = (WatersRecord) waters.data().get(k);
        if (r != null && r.isValidAt(thisSync) &&
            (r.getLastModified() > lastSync || r.getEfbId() == null
                || r.getEfbId().length() == 0)) {
          if (Logger.isTraceOn(Logger.TT_SYNC)) {
            logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCINFO,
                "  erstelle Synchronisierungs-Anfrage für Gewässer: " + r.getQualifiedName());
          }
          request.append("<water><name>" + r.getQualifiedName() + "</name></water>\n");
          efaIds.put(r.getQualifiedName(), r.getId());
          reqCnt++;
        }
        k = it.getNext();
      }
      buildRequestFooter(request);

      logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Sende Synchronisierungs-Anfrage für "
          + reqCnt + " Gewässer ...");
      KanuEfbXmlResponse response = sendRequest(request.toString(), true);
      if (response != null && response.isResponseOk("SyncWaters")) {
        logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Synchronisierungs-Antwort erhalten für "
            + response.getNumberOfRecords() + " Gewässer ...");
        for (int i = 0; i < response.getNumberOfRecords(); i++) {
          Hashtable<String, String> fields = response.getFields(i);
          boolean ok = false;
          String watersName = fields.get("label");
          String efbId = fields.get("id");
          if (watersName != null) {
            watersName = watersName.trim();
            UUID efaId = efaIds.get(watersName);
            if (efaId != null && efbId != null) {
              WatersRecord w = waters.getWaters(efaId);
              efbId = efbId.trim();
              if (w != null) {
                if (!efbId.equals(w.getEfbId())) {
                  w.setEfbId(efbId);
                  waters.data().update(w);
                  countSyncWaters++;
                }
                ok = true;
              }
            }
          }
          if (ok) {
            if (Logger.isTraceOn(Logger.TT_SYNC)) {
              logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCINFO,
                  "  Synchronisierungs-Antwort für Gewässer: " + watersName + " (EfbId=" + efbId
                      + ")");
            }
          } else {
            logInfo(Logger.WARNING, Logger.MSG_SYNC_WARNINCORRECTRESPONSE,
                "Ungültige Synchronisierungs-Antwort für Gewässer: " + watersName);
          }
        }
        logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, countSyncWaters
            + " neue Gewässer synchronisiert.");
      } else {
        logInfo(Logger.ERROR, Logger.MSG_SYNC_ERRORINVALIDRESPONSE,
            "Ungültige Synchronisierungs-Antwort.");
        return false;
      }
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  private boolean syncTrips() {
    try {
      logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Synchronisiere Fahrten für " +
          0 + " Personen mit Efb-ID's ...");
      StringBuilder request = new StringBuilder();
      buildRequestHeader(request, "SyncTrips");

      Boats boats = Daten.project.getBoats(false);
      Persons persons = Daten.project.getPersons(false);
      Destinations destinations = Daten.project.getDestinations(false);
      Waters waters = Daten.project.getWaters(false);

      DataKeyIterator it = logbook.data().getStaticIterator();
      DataKey k = it.getFirst();
      int reqCnt = 0;
      Hashtable<String, LogbookRecord> efaEntryIds = new Hashtable<String, LogbookRecord>();
      while (k != null) {
        LogbookRecord r = (LogbookRecord) logbook.data().get(k);
        if (r != null &&
            (r.getLastModified() > r.getSyncTime() || r.getSyncTime() <= 0) &&
            r.isRowingOrCanoeingSession() &&
            Daten.efaConfig.isCanoeBoatType(r.getBoatRecord(r.getValidAtTimestamp()))) {} else {
          if (r != null) {
            if (Logger.isTraceOn(Logger.TT_SYNC)) {
              logInfo(
                  Logger.DEBUG,
                  Logger.MSG_SYNC_SYNCINFO,
                  "  keine Synchronisierungs-Anfrage für unveränderte Fahrt: "
                      + r.getQualifiedName());
            }
          }
        }
        k = it.getNext();
      }
      buildRequestFooter(request);

      logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Sende Synchronisierungs-Anfrage für "
          + reqCnt + " Fahrten ...");
      KanuEfbXmlResponse response = sendRequest(request.toString(), true);
      if (response != null && response.isResponseOk("SyncTrips")) {
        logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Synchronisierungs-Antwort erhalten für "
            + response.getNumberOfRecords() + " Fahrten ...");
        for (int i = 0; i < response.getNumberOfRecords(); i++) {
          Hashtable<String, String> fields = response.getFields(i);
          boolean ok = false;
          String tripId = fields.get("tripid");
          int result = EfaUtil.string2int(fields.get("result"), -1);
          LogbookRecord r = null;
          if (tripId != null) {
            tripId = tripId.trim();
            r = efaEntryIds.get(tripId);
          }
          String resultText = fields.get("resulttext");
          if (r != null) {
            if (result == 0 || // 0 - ok - new trip accepted
                result == 1 || // 1 - ok - existing trip updated
                result == 2) { // 2 - ok - existing trip deleted
              r.setSyncTime(thisSync);
              logbook.data().update(r);
              ok = true;
            }
          } else {
            logInfo(Logger.WARNING, Logger.MSG_SYNC_WARNINCORRECTRESPONSE,
                "Fehler beim Synchronisieren von Fahrt: Trip ID " + tripId + " unbekannt (Code "
                    + result + " - " + resultText + ")");
          }
          if (ok) {
            countSyncTrips++;
            if (Logger.isTraceOn(Logger.TT_SYNC)) {
              logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCINFO,
                  "  Fahrt erfolgreich synchronisiert: " + r.toString());
            }
          } else {
            logInfo(Logger.WARNING, Logger.MSG_SYNC_WARNINCORRECTRESPONSE,
                "Fehler beim Synchronisieren von Fahrt: " + tripId + " (Code " + result + " - "
                    + resultText + ")");
          }
        }
        logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, countSyncTrips + " Fahrten synchronisiert.");
      } else {
        logInfo(Logger.ERROR, Logger.MSG_SYNC_ERRORINVALIDRESPONSE,
            "Ungültige Synchronisierungs-Antwort.");
        return false;
      }
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  private boolean syncDone() {
    try {
      if (loggedIn) {
        logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Logout ...");

        StringBuilder request = new StringBuilder();
        buildRequestHeader(request, "SyncDone");
        buildRequestFooter(request);

        KanuEfbXmlResponse response = sendRequest(request.toString(), false);
      }

    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  @Override
  public void run() {
    setRunning(true);
    try {
      Thread.sleep(1000);
    } catch (Exception e) {}
    int i = 0;
    thisSync = System.currentTimeMillis();
    logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Beginne Synchronisierung mit Kanu-eFB ...");
    logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Startzeit der Synchronisierung: " +
        EfaUtil.getTimeStamp(thisSync) + " (" + thisSync + ")");
    logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Letzte Synchronisierung: " +
        (lastSync == 0 ? "noch nie" : EfaUtil.getTimeStamp(lastSync)) + " (" + lastSync + ")");
    while (true) {
      if (!login()) {
        break;
      }
      setCurrentWorkDone(++i);
      if (!syncUsers()) {
        break;
      }
      setCurrentWorkDone(++i);
      if (!syncBoats()) {
        break;
      }
      setCurrentWorkDone(++i);
      if (!syncWaters()) {
        break;
      }
      setCurrentWorkDone(++i);
      if (!syncTrips()) {
        break;
      }
      setCurrentWorkDone(++i);
      break;
    }
    syncDone();
    setCurrentWorkDone(++i);
    if (i == getAbsoluteWork()) {
      Daten.project.setClubKanuEfbLastSync(thisSync);
      StringBuilder msg = new StringBuilder();
      if (countErrors == 0) {
        if (countWarnings == 0) {
          msg.append("Synchronisierung mit Kanu-eFB erfolgreich beendet.");
        } else {
          msg.append("Synchronisierung mit Kanu-eFB mit Warnungen beendet.");
        }
      } else {
        msg.append("Synchronisierung mit Kanu-eFB mit Fehlern beendet.");
      }
      msg.append(" [");
      msg.append(countSyncUsers + " Personen, ");
      msg.append(countSyncBoats + " Boote, ");
      msg.append(countSyncWaters + " Gewässer, ");
      msg.append(countSyncTrips + " Fahrten synchronisiert] [");
      msg.append(countWarnings + " Warnungen, ");
      msg.append(countErrors + " Fehler");
      msg.append("]");
      logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, msg.toString());
      successfulCompleted = true;
    } else {
      logInfo(Logger.ERROR, Logger.MSG_SYNC_ERRORABORTSYNC,
          "Synchronisierung mit Kanu-eFB wegen Fehlern abgebrochen.");
      successfulCompleted = false;
    }
    setDone();
  }

  @Override
  public int getAbsoluteWork() {
    return 6;
  }

  @Override
  public String getSuccessfullyDoneMessage() {
    if (successfulCompleted) {
      return LogString.operationSuccessfullyCompleted(International.getString("Synchronisation")) +
          "\n" + countSyncTrips + " Fahrten synchronisiert." +
          "\n" + countSyncUsers + " Personen synchronisiert." +
          "\n" + countSyncBoats + " Boote synchronisiert." +
          "\n" + countSyncWaters + " Gewässer synchronisiert." +
          "\n\n" + countWarnings + " Warnungen" +
          "\n" + countErrors + " Fehler";
    } else {
      return LogString.operationFailed(International.getString("Synchronisation"));
    }
  }

  private void logInfo(String type, String key, String msg) {
    Logger.log(type, key, msg);
    // if (!type.equals(Logger.DEBUG)) {
    logInfo(msg + "\n");
    // }
    if (Logger.WARNING.equals(type)) {
      countWarnings++;
    }
    if (Logger.ERROR.equals(type)) {
      countErrors++;
    }
  }

  public void startSynchronization(ProgressDialog progressDialog) {
    if (Daten.isGuiAppl()) {
      if (Dialog
          .yesNoDialog(
              International.onlyFor("Mit Kanu-eFB synchronisieren", "de"),
              International
                  .onlyFor(
                      "Es werden alle Fahrten aus dem aktuellen Fahrtenbuch mit dem Kanu-eFB synchronisiert.",
                      "de")
                  + "\n" +
                  International.getString("Bitte stelle eine Verbindung zum Internet her.") + "\n" +
                  International.getString("Möchtest Du fortfahren?")) != Dialog.YES) {
        return;
      }
    }
    while (loginurl == null || loginurl.length() == 0 ||
        cmdurl == null || cmdurl.length() == 0) {
      String msg = International.getString("Fehlende Konfigurationseinstellungen");
      if (!Daten.isGuiAppl()) {
        Logger.log(Logger.ERROR, Logger.MSG_SYNC_ERRORABORTSYNC, msg);
        Daten.haltProgram(Daten.HALT_MISCONFIG);
      }
      Dialog.infoDialog(msg,
          International.getString("Bitte vervollständige die Konfigurationseinstellungen!"));
      EfaConfigDialog dlg = new EfaConfigDialog((JDialog) null, Daten.efaConfig,
          BaseTabbedDialog.makeCategory(Daten.efaConfig.CATEGORY_SYNC,
              Daten.efaConfig.CATEGORY_KANUEFB));
      dlg.showDialog();
      if (!dlg.getDialogResult()) {
        return;
      }
      getConfigValues();
    }
    while (username == null || username.length() == 0 ||
        password == null || password.length() == 0) {
      String msg = International.getString("Fehlende Konfigurationseinstellungen");
      if (!Daten.isGuiAppl()) {
        Logger.log(Logger.ERROR, Logger.MSG_SYNC_ERRORABORTSYNC, msg);
        Daten.haltProgram(Daten.HALT_MISCONFIG);
      }
      ProjectEditDialog dlg = new ProjectEditDialog((JDialog) null, Daten.project, null,
          ProjectRecord.GUIITEMS_SUBTYPE_KANUEFB, admin);
      dlg.showDialog();
      if (!dlg.getDialogResult()) {
        return;
      }
      getConfigValues();
    }

    // Install the all-trusting trust manager
    try {
      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
      java.lang.System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true");
    } catch (Exception e) {
      System.out.println(e);
    }

    this.start();
    if (progressDialog != null) {
      progressDialog.showDialog();
    }
  }

  public boolean isSuccessfullyCompleted() {
    return successfulCompleted;
  }

}
