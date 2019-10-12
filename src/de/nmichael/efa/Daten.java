/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa;

import java.awt.Color;
import java.awt.Frame;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Vector;
import java.util.jar.JarFile;

import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

import org.apache.commons.io.FileUtils;

import de.nmichael.efa.calendar.ICalendarExport;
import de.nmichael.efa.core.CrontabThread;
import de.nmichael.efa.core.EfaKeyStore;
import de.nmichael.efa.core.EfaRunning;
import de.nmichael.efa.core.EfaSec;
import de.nmichael.efa.core.EmailSenderThread;
import de.nmichael.efa.core.Plugins;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.config.Admins;
import de.nmichael.efa.core.config.CustSettings;
import de.nmichael.efa.core.config.EfaBaseConfig;
import de.nmichael.efa.core.config.EfaConfig;
import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeFile;
import de.nmichael.efa.data.Project;
import de.nmichael.efa.data.efawett.WettDefs;
import de.nmichael.efa.data.storage.DataFile;
import de.nmichael.efa.data.storage.RemoteEfaServer;
import de.nmichael.efa.gui.BrowserDialog;
import de.nmichael.efa.gui.EfaFirstSetupDialog;
import de.nmichael.efa.gui.SimpleInputDialog;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.HtmlFactory;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;

// @i18n complete
public class Daten {

  // Version für die Ausgabe (z.B. 2.1.0, kann aber
  // auch Zusätze wie "alpha" o.ä. enthalten)
  public final static String VERSION = "2.2.0"; 

  // VersionsID: Format: "X.Y.Z_MM";
  // final-Version z.B. 1.4.0_00; beta-Version z.B. 1.4.0_#1
  public final static String VERSIONID = "2.2.0_50";
  public final static String VERSIONRELEASEDATE = "12.10.2019"; // Release Date: TT.MM.JJJJ
  public final static String MAJORVERSION = "2";
  public final static String PROGRAMMID = "EFA.220"; // Versions-ID für Wettbewerbsmeldungen
  public final static String PROGRAMMID_DRV = "EFADRV.220"; // Versions-ID für Wettbewerbsmeldungen
  public final static String COPYRIGHTYEAR = "14"; // aktuelles Jahr (Copyright (c)
  // 2001-COPYRIGHTYEAR)

  // enable/disable development functions for next version
  public static final boolean NEW_FEATURES = false;

  public final static String EFA = "efa"; // efa program name/ID
  public static String EFA_SHORTNAME = "efa"; // dummy, will be set in International.ininitalize()
  public static String EFA_LONGNAME = "efa - elektronisches Fahrtenbuch"; // dummy, will be set in
  // International.ininitalize()
  public static String EFA_ONLINE = "efaOnline"; // dummy, will be set in
  // International.ininitalize()
  public static String EFA_BASE = "efaBasis"; // dummy, will be set in International.ininitalize()
  public static String EFA_BOATHOUSE = "efaBootshaus"; // dummy, will be set in
  // International.ininitalize()
  public static String EFA_CLI = "efaCLI"; // dummy, will be set in International.ininitalize()
  public static String EFA_LIVE = "efaLive"; // dummy, will be set in International.ininitalize()
  public static String EFA_WETT = "efaWett"; // dummy, will be set in International.ininitalize()
  public static String EFA_REMOTE = "efaRemote"; // dummy, will be set in
  // International.ininitalize()
  public final static String EFA_JAVA_ARGUMENTS = "EFA_JAVA_ARGUMENTS"; // Environment Variable Name
  // containing all arguments
  // passed to the "java"
  // command
  public static String efa_java_arguments = null; // Environment Variable Contents containing all
  // arguments passed to the "java" command
  public final static String EFADIREKT_MAINCLASS = de.nmichael.efa.boathouse.Main.class
      .getCanonicalName();
  public final static String EFAURL = "http://efa.nmichael.de";
  public final static String EFASUPPORTURL = "http://efa.nmichael.de/help.html";
  public final static String EFADEVURL = "http://kenai.com/projects/efa";
  public final static String EFATRANSLATEWIKI = "http://kenai.com/projects/efa/pages/TranslatingEfa";
  public final static String EFAWETTURL = "http://efa.rudern.de";
  public final static String NICOLASURL = "http://www.nmichael.de";
  public final static String EFAEMAILNAME = "efa";
  public final static String EMAILINFO = "info.efa" + ICalendarExport.ABFX_DE;
  public final static String EMAILBUGS = "bugs.efa" + ICalendarExport.ABFX_DE;
  public final static String EMAILHELP = "help.efa" + ICalendarExport.ABFX_DE;
  public final static String EMAILDEV = "dev.efa" + ICalendarExport.ABFX_DE;
  public static final String EFA_USERDATA_DIR = "efa2"; // <efauser> = ~/efa2/ Directory for efauser
  // data (if not efa program directory)
  public static final String EFA_RUNNING = "efa.run"; // <efauser>/efa.run Indiz, daß efaDirekt
  // läuft (enthält Port#)

  public final static String CONFIGFILE = "efa.cfg"; // <efauser>/cfg/efa.cfg Konfigurationsdatei
  public final static String DRVCONFIGFILE = "drv.cfg"; // <efauser>/cfg/drv.cfg
  // DRV-Konfigurationsdatei
  public static final String EFATYPESFILE = "types.cfg"; // <efauser>/cfg/types.cfg Konfiguration
  // für EfaTypes (Bezeichnungen)
  public static final String WETTFILE = "wett.cfg"; // <efauser>/cfg/wett.cfg Konfiguration für
  // Wettbewerbe
  public static final String WETTDEFS = "wettdefs.cfg"; // <efauser>/cfg/wettdefs.cfg
  // Wettbewerbs-Definitionen

  private static final String EFALIVE_VERSIONFILE = "/etc/efalive_version"; // file containing
  // efaLive version
  public static String EFALIVE_VERSION = null; // efaLive Version Number
  private static final String EFACREDENVVAR = "EFA_CRED"; // Environment Variable specifying the
  // Credentials File Name
  public static String EFACREDFILE = ".efacred"; // <userHomeDir>.efacred Credentials for CLI Remote
  // Access

  public static final String EFA_LICENSE = "license.html"; // <efa>/doc/license.html
  public static final String EFA_JAR = "efa.jar"; // <efa>/program/efa.jar
  public static final String EFA_SECFILE = "efa.sec"; // <efa>/program/efa.sec Hash von efa.jar: für
  // Erstellen des Admins
  public static final String EFA_HELPSET = "help/efaHelp";

  // efa exit codes
  // Note: Codes 99 and higher will cause the shell script to restart efa!
  // Therefore, any errors MUST use smaller codes than 99
  public static final int HALT_BASICCONFIG = 1;
  public static final int HALT_DIRECTORIES = 2;
  public static final int HALT_EFACONFIG = 3;
  public static final int HALT_EFATYPES = 4;
  public static final int HALT_EFASEC = 5;
  public static final int HALT_EFARUNNING = 6;
  public static final int HALT_FILEOPEN = 7;
  public static final int HALT_EFASECADMIN = 8;
  public static final int HALT_FILEERROR = 9;
  public static final int HALT_ERROR = 10;
  public static final int HALT_INSTALLATION = 11;
  public static final int HALT_ADMIN = 12;
  public static final int HALT_MISCONFIG = 12;
  public static final int HALT_FIRSTSETUP = 13;
  public static final int HALT_PANIC = 14;
  public static final int HALT_ADMINLOGIN = 15;
  public static final int HALT_DATALOCK = 16;
  public static final int HALT_JAVARESTART = 98;
  public static final int HALT_SHELLRESTART = 99;

  private static final String efaSubdirBACKUP = "backup";
  public static final String efaSubdirCFG = "cfg";
  public static final String efaSubdirDATA = "data";
  private static final String efaSubdirLOG = "log";
  private static final String efaSubdirTMP = "tmp";

  public static Program program = null; // this Program
  public static String userHomeDir = null; // User Home Directory (NOT the efa userdata directoy!!
  // see EfaBaseConfig!)
  public static String userName = null; // User Name
  public static String efaLogfile = null; // Logdatei für efa-Konsole
  public static String efaMainDirectory = null; // Efa-Hauptverzeichnis, immer mit "/" am Ende
  public static String efaProgramDirectory = null; // Programmverzeichnis, immer mit "/" am Ende
  // ("./program/")
  public static String efaPluginDirectory = null; // Programmverzeichnis, immer mit "/" am Ende
  // ("./program/plugins")
  public static String efaDataDirectory = null; // Efa-Datenverzeichnis, immer mit "/" am Ende
  // ("./data/")
  public static String efaLogDirectory = null; // Efa-Log-Verzeichnis, immer mit "/" am Ende
  // ("./log/")
  public static String efaCfgDirectory = null; // Efa-Configverzeichnis, immer mit "/" am Ende
  // ("./cfg/")
  public static String efaImagesDirectory = null; // Efa-Doku-Verzeichnis, immer mit "/" am Ende
  // ("./images/")
  public static String efaFormattingDirectory = null; // Efa-Ausgabe-Verzeichnis, immer mit "/" am
  // Ende ("./fmt/")
  public static String efaBakDirectory = null; // Efa-Backupverzeichnis, immer mit "/" am Ende
  // ("./backup/")
  public static String efaTmpDirectory = null; // Efa-Tempverzeichnis, immer mit "/" am Ende
  // ("./tmp/")
  // public static String efaStyleDirectory = null; // Efa-Stylesheetverzeichnis, mit "/" am Ende
  // ("./fmt/layout/")
  public static String fileSep = "/"; // Verzeichnis-Separator (wird in ini() ermittelt)

  public static String javaVersion = "";
  public static String jvmVersion = "";
  public static String osName = "";
  public static String osVersion = "";
  public static String lookAndFeel = "";

  public final static String PLUGIN_INFO_FILE = "plugins.xml";
  public static String pluginWebpage = "http://efa.nmichael.de/plugins.html"; // wird automatisch
  // auf das in der o.g. Datei stehende gesetzt

  public final static String ONLINEUPDATE_INFO = "http://efa.nmichael.de/eou/eou.xml";
  public final static String ONLINEUPDATE_INFO_DRV = "http://efa.nmichael.de/eou/eoudrv.xml";
  public final static String EFW_UPDATE_DATA = "http://efa.nmichael.de/efw.data";
  public final static String INTERNET_EFAMAIL = "http://cgi.snafu.de/nmichael/user-cgi-bin/efamail.pl";
  public final static String IMAGEPATH = "/de/nmichael/efa/img/";
  public final static String FILEPATH = "/de/nmichael/efa/files/";
  public final static String DATATEMPLATEPATH = "/de/nmichael/efa/data/templates/";

  public final static int AUTO_EXIT_MIN_RUNTIME = 60; // Minuten, die efa mindestens gelaufen sein
  // muß, damit es zu einem automatischen Beenden/Restart kommt (60)
  public final static int AUTO_EXIT_MIN_LAST_USED = 5; // Minuten, die efa mindestens nicht benutzt
  // wurde, damit Beenden/Neustart nicht verzögert wird (muß kleiner als
  // AUTO_EXIT_MIN_RUNTIME sein!!!) (5)
  public final static int WINDOWCLOSINGTIMEOUT = 600; // Timeout in Sekunden, nach denen im
  // Direkt-Modus manche Fenster automatisch geschlossen werden
  public final static int MIN_FREEMEM_PERCENTAGE = 90;
  public final static int WARN_FREEMEM_PERCENTAGE = 70;
  public final static int MIN_FREEMEM_COLLECTION_THRESHOLD = 99;
  public static boolean DONT_SAVE_ANY_FILES_DUE_TO_OOME = false;
  public static boolean javaRestart = false;

  public static EfaBaseConfig efaBaseConfig; // efa Base Config
  public static EfaConfig efaConfig; // Konfigurationsdatei
  public static EfaTypes efaTypes; // EfaTypes (Bezeichnungen)
  public static Admins admins; // Admins
  public static Project project; // Efa Project
  public static WettDefs wettDefs; // WettDefs
  public static EfaKeyStore keyStore; // KeyStore
  public static final String PUBKEYSTORE = "keystore_pub.dat"; // <efauser>/data/keystore_pub.dat
  public static final String DRVKEYSTORE = "keystore.dat"; // <efauser>/data/keystore.dat
  public static final String EFAMASTERKEY = "k)fx,R4{Qb:lhTg";

  public static EfaSec efaSec; // efa Security File
  public static EfaRunning efaRunning; // efa Running (Doppelstarts verhindern)
  public static EmailSenderThread emailSenderThread;

  private static StartLogo splashScreen; // Efa Splash Screen
  private static boolean firstEfaStart = false; // true wenn efa das erste Mal gestartet wurde und
  // EfaBaseConfig neu erzeugt wurde

  public static Color colorGreen = new Color(0, 150, 0);
  public static Color colorOrange = new Color(255, 100, 0);
  public static String defaultWriteProtectPw = null;
  public static long efaStartTime;
  public static boolean exceptionTest = false; // Exceptions beim Drücken von F1 produzieren (für
  // Exception-Test)
  public static boolean watchWindowStack = false; // Window-Stack überwachen

  // Encoding zum Lesen und Schreiben von Dateien
  public static final String ENCODING_ISO = "ISO-8859-1";
  public static final String ENCODING_UTF = "UTF-8";

  // Applikations-IDs
  public static int applID = -1;
  public static String applName = "Unknown"; // will be set in iniBase(...)
  public static final int APPL_EFABASE = 1;
  public static final int APPL_EFABH = 2;
  public static final int APPL_CLI = 3;
  public static final int APPL_DRV = 4;
  public static final int APPL_EMIL = 5;
  public static final int APPL_ELWIZ = 6;
  public static final int APPL_EDDI = 7;
  public static final String APPLNAME_EFA = "efaBase";
  public static final String APPLNAME_EFADIREKT = "efaBths";
  public static final String APPLNAME_CLI = "efaCLI";
  public static final String APPLNAME_DRV = "efaDRV";
  public static final String APPLNAME_EMIL = "emil";
  public static final String APPLNAME_ELWIZ = "elwiz";
  public static final String APPLNAME_EDDI = "eddi";

  // Applikations-Mode
  public static final int APPL_MODE_NORMAL = 1;
  public static final int APPL_MODE_ADMIN = 2;
  public static int applMode = APPL_MODE_NORMAL;

  // Applikations- PID
  public static String applPID = "XXXXX"; // will be set in iniBase(...)

  public static AdminRecord initialize() {
    if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
      Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "initialize()");
      printEfaInfos(false, false, true, false, false);
    }
    iniScreenSize();
    iniMainDirectory();
    iniEfaBaseConfig();
    iniLanguageSupport();
    iniUserDirectory();
    iniLogging();
    iniDirectories();
    iniSplashScreen(true);
    iniEnvironmentSettings();
    iniEfaSec();
    boolean createNewAdmin = iniAdmins();
    Object[] efaFirstSetup = iniEfaFirstSetup(createNewAdmin);
    CustSettings cust = (efaFirstSetup != null ? (CustSettings) efaFirstSetup[0] : null);
    iniEfaConfig(cust);
    iniEfaRunning();
    iniEfaTypes(cust);
    iniCopiedFiles();
    iniAllDataFiles();
    iniRemoteEfaServer();
    iniEmailSenderThread();
    iniGUI();
    iniChecks();
    if (createNewAdmin && efaFirstSetup != null) {
      return (AdminRecord) efaFirstSetup[1];
    }
    return null;
  }

  public static String getCurrentStack() {
    try {
      StackTraceElement[] stack = Thread.currentThread().getStackTrace();
      String trace = "";
      for (int i = stack.length - 1; i >= 0; i--) {
        trace = trace + " -> " + stack[i].toString();
        if (stack[i].toString().startsWith(International.class.getCanonicalName())) {
          break;
        }
      }
      return trace;
    } catch (Exception e) {
      return "";
    }
  }

  public static void haltProgram(int exitCode) {
    if (exitCode == 0 || exitCode == HALT_SHELLRESTART || exitCode == HALT_JAVARESTART) {
      if (project != null && project.isOpen()) {
        try {
          project.closeAllStorageObjects();
        } catch (Exception e) {
          Logger
              .log(
                  Logger.ERROR,
                  Logger.MSG_DATA_CLOSEFAILED,
                  LogString.fileCloseFailed(project.toString(), project.getDescription(),
                      e.toString()));
        }
      }
      if (admins != null && admins.isOpen()) {
        try {
          admins.close();
        } catch (Exception e) {
          Logger.log(Logger.ERROR, Logger.MSG_DATA_CLOSEFAILED,
              LogString.fileCloseFailed(admins.toString(), admins.getDescription(), e.toString()));
        }
      }
      if (efaConfig != null && efaConfig.isOpen()) {
        try {
          efaConfig.close();
        } catch (Exception e) {
          Logger.log(
              Logger.ERROR,
              Logger.MSG_DATA_CLOSEFAILED,
              LogString.fileCloseFailed(efaConfig.toString(), efaConfig.getDescription(),
                  e.toString()));
        }
      }
      if (efaTypes != null && efaTypes.isOpen()) {
        try {
          efaTypes.close();
        } catch (Exception e) {
          Logger.log(
              Logger.ERROR,
              Logger.MSG_DATA_CLOSEFAILED,
              LogString.fileCloseFailed(efaTypes.toString(), efaTypes.getDescription(),
                  e.toString()));
        }
      }
    }

    if (exitCode != 0) {
      if (exitCode == HALT_SHELLRESTART || exitCode == HALT_JAVARESTART) {
        Logger.log(Logger.INFO, Logger.MSG_CORE_HALT,
            International.getString("PROGRAMMENDE") + "  (Ver:" + VERSIONID + ")" + " (Exit Code " + exitCode + ")");
      } else {
        if (applID != APPL_CLI) {
          Logger.log(Logger.INFO, Logger.MSG_CORE_HALT, getCurrentStack());
        }
        Logger.log(Logger.ERROR, Logger.MSG_CORE_HALT,
            International.getString("PROGRAMMENDE") + "  (Ver:" + VERSIONID + ")" + " (Error Code " + exitCode + ")");
      }
    } else {
      Logger.log(Logger.INFO, Logger.MSG_CORE_HALT,
          International.getString("PROGRAMMENDE") + "  (Ver:" + VERSIONID + " vom " + VERSIONRELEASEDATE + ")");
    }
    if (program != null) {
      program.exit(exitCode);
    } else {
      System.exit(exitCode);
    }
  }

  public static void iniBase(int _applID) {
    if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
      Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniBase(" + _applID + ")");
    }
    project = null;
    fileSep = System.getProperty("file.separator");
    javaVersion = System.getProperty("java.version");
    jvmVersion = System.getProperty("java.vm.version");
    osName = System.getProperty("os.name");
    osVersion = System.getProperty("os.version");
    userHomeDir = System.getProperty("user.home");
    if (userHomeDir == null) {
      userHomeDir = "";
    }
    if (!userHomeDir.endsWith(fileSep)) {
      userHomeDir += fileSep;
    }
    userName = System.getProperty("user.name");
    applID = _applID;
    switch (applID) {
      case APPL_EFABASE:
        applName = APPLNAME_EFA;
        break;
      case APPL_EFABH:
        applName = APPLNAME_EFADIREKT;
        break;
      case APPL_CLI:
        applName = APPLNAME_CLI;
        break;
      case APPL_DRV:
        applName = APPLNAME_DRV;
        break;
      case APPL_EMIL:
        applName = APPLNAME_EMIL;
        break;
      case APPL_ELWIZ:
        applName = APPLNAME_ELWIZ;
        break;
      case APPL_EDDI:
        applName = APPLNAME_EDDI;
        break;
    }
    efaStartTime = System.currentTimeMillis();

    try {
      // ManagementFactory.getRuntimeMXBean().getName() == "12345@localhost" or similar (not
      // guaranteed by VM Spec!)
      applPID = EfaUtil.int2String(
          EfaUtil.stringFindInt(ManagementFactory.getRuntimeMXBean().getName(), 0), 5);
    } catch (Exception e) {
      applPID = "00000";
    }
  }

  private static void iniMainDirectory() {
    if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
      Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniMainDirectory()");
    }
    efaMainDirectory = System.getProperty("user.dir");
    if (!efaMainDirectory.endsWith(fileSep)) {
      efaMainDirectory += fileSep;
    }
    if (efaMainDirectory.endsWith("/program/")
        && !new File(efaMainDirectory + "program/").isDirectory()) {
      efaMainDirectory = efaMainDirectory.substring(0,
          efaMainDirectory.length() - 8);
    }
    if (efaMainDirectory.endsWith("/classes/")
        && !new File(efaMainDirectory + "program/").isDirectory()) {
      efaMainDirectory = efaMainDirectory.substring(0,
          efaMainDirectory.length() - 8);
    }
    efaProgramDirectory = efaMainDirectory + "program" + fileSep; // just
    // temporary, will be overwritten by iniDirectories()
    if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
      printEfaInfos(true, false, false, false, false);
    }
  }

  private static void iniEfaBaseConfig() {
    if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
      Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniEfaBaseConfig()");
    }
    String efaBaseConfigFile = userHomeDir
        + (fileSep != null && !userHomeDir.endsWith(fileSep) ? fileSep : "");
    efaBaseConfig = new EfaBaseConfig(efaBaseConfigFile);
    if (!EfaUtil.canOpenFile(efaBaseConfig.getFileName())) {
      if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
        Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION,
            "iniEfaBaseConfig(): cannot open: " + efaBaseConfig.getFileName());
      }
      if (!efaBaseConfig.writeFile()) {
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
          Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION,
              "iniEfaBaseConfig(): cannot write: " + efaBaseConfig.getFileName());
        }
        String msg = International.getString("efa can't start")
            + ": "
            + LogString.fileCreationFailed(International.getString("Basic Configuration File"),
                efaBaseConfig.getFileName());
        Logger.log(Logger.ERROR, Logger.MSG_CORE_BASICCONFIGFAILEDCREATE, msg);
        if (isGuiAppl()) {
          Dialog.error(msg);
        }
        haltProgram(HALT_BASICCONFIG);
      }
      firstEfaStart = true;
    }
    if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
      Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION,
          "iniEfaBaseConfig(): firstEfaStart=" + firstEfaStart);
    }
    if (!efaBaseConfig.readFile()) {
      if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
        Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION,
            "iniEfaBaseConfig(): cannot read: " + efaBaseConfig.getFileName());
      }
      String msg = International.getString("efa can't start")
          + ": "
          + LogString.fileOpenFailed(International.getString("Basic Configuration File"),
              efaBaseConfig.getFileName());
      Logger.log(Logger.ERROR, Logger.MSG_CORE_BASICCONFIGFAILEDOPEN, msg);
      if (isGuiAppl()) {
        Dialog.error(msg);
      }
      haltProgram(HALT_BASICCONFIG);
    }
    if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
      printEfaInfos(true, false, false, false, false);
    }
  }

  private static void iniLanguageSupport() {
    if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
      Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniLanguageSupport()");
    }
    International.initialize();
  }

  private static void iniUserDirectory() {
    if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
      Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniUserDirectory()");
    }
    if (firstEfaStart && isGuiAppl()) {
      while (true) {
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
          Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION,
              "iniUserDirectory(): prompting user for input...");
        }
        ItemTypeFile dir = new ItemTypeFile("USERDIR", efaBaseConfig.efaUserDirectory,
            International.getString("Verzeichnis für Nutzerdaten"),
            International.getString("Verzeichnisse"),
            null, ItemTypeFile.MODE_OPEN, ItemTypeFile.TYPE_DIR,
            IItemType.TYPE_PUBLIC, "",
            International
                .getString("In welchem Verzeichnis soll efa sämtliche Benutzerdaten ablegen?"));
        dir.setFieldSize(600, 19);
        if (SimpleInputDialog.showInputDialog((Frame) null,
            International.getString("Verzeichnis für Nutzerdaten"), dir)) {
          dir.getValueFromGui();
          if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION,
                "iniUserDirectory(): input=" + dir.getValue());
          }
          if (!efaBaseConfig.trySetUserDir(dir.getValue(), javaRestart)) {
            if (Logger.isTraceOn(Logger.TT_CORE, 9)
                || Logger.isDebugLoggingActivatedByCommandLine()) {
              Logger.log(
                  Logger.DEBUG,
                  Logger.MSG_CORE_STARTUPINITIALIZATION,
                  "iniUserDirectory(): "
                      +
                      LogString.directoryNoWritePermission(dir.getValue(),
                          International.getString("Verzeichnis")));
            }
            Dialog.error(LogString.directoryNoWritePermission(dir.getValue(),
                International.getString("Verzeichnis")));
          } else {
            efaBaseConfig.writeFile();
            if (Logger.isTraceOn(Logger.TT_CORE, 9)
                || Logger.isDebugLoggingActivatedByCommandLine()) {
              Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION,
                  "iniUserDirectory(): " + efaBaseConfig.getFileName() + " written.");
            }
            break;
          }
        } else {
          if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION,
                "iniUserDirectory(): input aborted.");
          }
          haltProgram(HALT_BASICCONFIG);
        }
      }
    }
    if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
      printEfaInfos(true, false, false, false, false);
    }
  }

  private static void iniLogging() {
    if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
      Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniLogging()");
    }
    efaLogDirectory = efaBaseConfig.efaUserDirectory + efaSubdirLOG + fileSep;
    if (!checkAndCreateDirectory(efaLogDirectory)) {
      haltProgram(HALT_DIRECTORIES);
    }
    String lastLogEntry = null;
    if (applID == APPL_EFABH) {
      lastLogEntry = Logger.getLastLogEntry("efa.log");
    }
    String baklog = null; // backup'ed logfile
    switch (applID) {
      case APPL_EFABASE:
      case APPL_EFABH:
      case APPL_DRV:
        baklog = Logger.ini("efa.log", true, false);
        break;
      case APPL_CLI:
        baklog = Logger.ini("efa.log", true, true);
        break;
      default:
        baklog = Logger.ini(null, true, false);
        break;
    }

    Logger.log(Logger.INFO, Logger.MSG_EVT_EFASTART,
        International.getString("PROGRAMMSTART") + " (Ver:" + VERSIONID + " vom " + VERSIONRELEASEDATE + ")");
    Logger.log(Logger.INFO, Logger.MSG_INFO_VERSION,
        "Java " + javaVersion + " (JVM " + jvmVersion + ")"
            + " -- OS: " + osName + " " + osVersion);
    if (Logger.isDebugLogging()) {
      Logger.log(Logger.INFO, Logger.MSG_LOGGER_DEBUGACTIVATED,
          "Debug Logging activated."); // do not internationalize!
    }
    if (baklog != null) {
      Logger.log(Logger.INFO, Logger.MSG_EVT_LOGFILEARCHIVED,
          International.getMessage("Alte Logdatei wurde nach '{filename}' verschoben.", baklog));
    }
    if (lastLogEntry != null && lastLogEntry.length() > 0 &&
        !lastLogEntry.contains(International.getString("PROGRAMMENDE"))) {
      Logger.log(Logger.WARNING, Logger.MSG_WARN_PREVIOUSEXITIRREGULAR,
          International.getMessage(
              "efa wurde zuvor nicht korrekt beendet. Letzer Eintrag in Logdatei: {msg}",
              lastLogEntry));
    }
  }

  private static void iniEnvironmentSettings() {
    if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
      Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniEnvironmentSettings()");
    }
    String s;

    try {
      if (applID == APPL_EFABH) {
        efa_java_arguments = System.getenv(EFA_JAVA_ARGUMENTS);
        if (Logger.isTraceOn(Logger.TT_CORE)) {
          Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_GENERIC,
              EFA_JAVA_ARGUMENTS + "=" + efa_java_arguments);
        }
      }
    } catch (Error e) {
      Logger.log(Logger.WARNING, Logger.MSG_WARN_CANTGETEFAJAVAARGS,
          "Cannot get Environment Variable " + EFA_JAVA_ARGUMENTS + ": " + e.toString());
    }

    try {
      s = System.getenv(EFACREDENVVAR);
      if (s != null && s.length() > 0) {
        EFACREDFILE = s;
      } else {
        EFACREDFILE = userHomeDir + EFACREDFILE;
      }
    } catch (Exception e) {
      Logger.logdebug(e);
    }

    try {
      if ((new File(EFALIVE_VERSIONFILE).exists())) {
        EFALIVE_VERSION = "";
        BufferedReader f = new BufferedReader(new FileReader(EFALIVE_VERSIONFILE));
        s = f.readLine();
        if (s != null) {
          EFALIVE_VERSION = s.trim();
        }
        f.close();
      }
    } catch (Exception e) {
      Logger.logdebug(e);
    }
  }

  private static void iniDirectories() {
    if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
      Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniDirectories()");
    }
    // ./program
    efaProgramDirectory = efaMainDirectory + "program" + fileSep;
    if (!checkAndCreateDirectory(efaProgramDirectory)) {
      haltProgram(HALT_DIRECTORIES);
    }

    // ./program/plugins
    efaPluginDirectory = efaProgramDirectory + "plugins" + fileSep;
    if (!checkAndCreateDirectory(efaPluginDirectory)) {
      haltProgram(HALT_DIRECTORIES);
    }

    // ./data
    if (applID != APPL_DRV) {
      efaDataDirectory = efaBaseConfig.efaUserDirectory + efaSubdirDATA + fileSep;
    } else {
      efaDataDirectory = efaBaseConfig.efaUserDirectory + "daten" + fileSep;
    }
    if (!checkAndCreateDirectory(efaDataDirectory)) {
      haltProgram(HALT_DIRECTORIES);
    }

    // ./cfg
    efaCfgDirectory = efaBaseConfig.efaUserDirectory + efaSubdirCFG + fileSep;
    if (!checkAndCreateDirectory(efaCfgDirectory)) {
      haltProgram(HALT_DIRECTORIES);
    }

    // ./images
    efaImagesDirectory = efaBaseConfig.efaUserDirectory + "images" + fileSep;
    if (!checkAndCreateDirectory(efaImagesDirectory)) {
      haltProgram(HALT_DIRECTORIES);
    }

    // ./bak
    if (!trySetEfaBackupDirectory(null)) {
      haltProgram(HALT_DIRECTORIES);
    }

    // ./tmp
    efaTmpDirectory = efaBaseConfig.efaUserDirectory + efaSubdirTMP + fileSep;
    if (!checkAndCreateDirectory(efaTmpDirectory)) {
      haltProgram(HALT_DIRECTORIES);
    }

    if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
      printEfaInfos(true, false, false, false, false);
    }
  }

  public static void iniSplashScreen(boolean show) {
    if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
      Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, 
          "iniSplashScreen(" + show + ")");
    }
    if (!isGuiAppl()) {
      return;
    }
    if (show) {
      try {
        splashScreen = new StartLogo(efaCfgDirectory + "efa.intro.png");
        splashScreen.show();
      } catch (Exception e) {
        splashScreen = new StartLogo(IMAGEPATH + "efaIntro.png");
        splashScreen.show();
      }
      try {
        Thread.sleep(1000); // Damit nach automatischem Restart genügend Zeit vergeht
      } catch (InterruptedException e) {}
    } else {
      if (splashScreen != null) {
        splashScreen.remove();
        splashScreen = null;
      }
    }
  }

  public static void iniEfaSec() {
    if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
      Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniEfaSec()");
    }
    if (firstEfaStart) {
      EfaSec.createNewSecFile(efaBaseConfig.efaUserDirectory + EFA_SECFILE,
          efaProgramDirectory + EFA_JAR);
    }
    efaSec = new EfaSec(efaBaseConfig.efaUserDirectory + EFA_SECFILE);
  }

  // returns true if we need to create a new super admin (and are allowed to do so)
  // returns false if we have a super admin and don't need to create one
  // halts efa if there is no super admin, but we're not allowed to create one either
  public static boolean iniAdmins() {
    if (applID == APPL_DRV) {
      return false;
    }
    admins = new Admins();
    try {
      // try to open admin file
      admins.open(false);
    } catch (Exception e) {
      if (!isGuiAppl()) {
        // if this is not a GUI appl, then stop here!
        Logger.log(Logger.ERROR, Logger.MSG_CORE_ADMINSFAILEDOPEN,
            LogString.fileOpenFailed(((DataFile) admins.data()).getFilename(),
                International.getString("Administratoren")));
        haltProgram(HALT_ADMIN);
      }
      // check whether admin file exists, and only could not be opened
      boolean exists = true;
      try {
        exists = admins.data().existsStorageObject();
      } catch (Exception ee) {
        Logger.logdebug(ee);
      }
      if (exists) {
        // admin file exists, but could not be opened. we exit here.
        String msg = LogString.fileOpenFailed(((DataFile) admins.data()).getFilename(),
            International.getString("Administratoren"));
        Logger.log(Logger.ERROR, Logger.MSG_CORE_ADMINSFAILEDOPEN, msg);
        if (isGuiAppl()) {
          Dialog.error(msg);
        }
        haltProgram(HALT_ADMIN);
      }
      // no admin file there, we need to create a new one
      if (efaSec.secFileExists() && efaSec.secValueValid()) {
        // ok, sec file is there: we're allowed to create a new one
        return true;
      } else {
        // no sec file there: exit and don't create new admin
        String msg = International.getString("Kein Admin gefunden.") + "\n"
            + International.getString("Aus Gründen der Sicherheit verweigert efa den Dienst. "
                + "Hilfe zum Reaktivieren von efa erhälst Du im Support-Forum.");
        Logger.log(Logger.ERROR, Logger.MSG_CORE_ADMINSFAILEDNOSEC, msg);
        if (isGuiAppl()) {
          Dialog.error(msg);
        }
        haltProgram(HALT_EFASEC);
      }
      return false; // we never reach here, but just to be sure... ;-)
    }
    // we do have a admin file already that we can open. now check whether there's a super admin
    // configured as well
    if (admins.getAdmin(Admins.SUPERADMIN) == null) {
      // we don't have a super admin yet
      if (efaSec.secFileExists() && efaSec.secValueValid()) {
        // ok, sec file is there: we're allowed to create a new one
        return true;
      }
      // no sec file there: exit and don't create new admin
      String msg = International.getString("Kein Admin gefunden.") + "\n"
          + International.getString("Aus Gründen der Sicherheit verweigert efa den Dienst. "
              + "Hilfe zum Reaktivieren von efa erhälst Du im Support-Forum.");
      Logger.log(Logger.ERROR, Logger.MSG_CORE_ADMINSFAILEDNOSEC, msg);
      if (isGuiAppl()) {
        Dialog.error(msg);
      }
      haltProgram(HALT_EFASEC);
      return false; // we never reach here, but just to be sure... ;-)
    } else {
      // ok, we do have a super admin already
      return false;
    }
  }

  /**
   * @return [0] == CustSettins; [1] == new AdminRecord
   */
  public static Object[] iniEfaFirstSetup(boolean createNewAdmin) {
    if (applID == APPL_DRV) {
      return null;
    }
    if (firstEfaStart || createNewAdmin) {
      if (!isGuiAppl()) {
        Logger.log(Logger.ERROR, Logger.MSG_CORE_BASICCONFIG,
            "efa is not yet fully set up. Please launch GUI program first.");
        haltProgram(HALT_BASICCONFIG);
      }
      iniSplashScreen(false);
      EfaFirstSetupDialog dlg = new EfaFirstSetupDialog(createNewAdmin, firstEfaStart);
      dlg.showDialog();
      if (!dlg.getDialogResult()) {
        haltProgram(HALT_FIRSTSETUP);
      }
      Object[] result = new Object[2];
      result[0] = dlg.getCustSettings();
      result[1] = dlg.getNewSuperAdmin();
      return result;
    }
    return null;
  }

  public static void iniEfaConfig(CustSettings custSettings) {
    if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
      Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniEfaConfig()");
    }
    if (applID != APPL_DRV) {
      efaConfig = new EfaConfig(custSettings);
      try {
        efaConfig.open(false);
      } catch (Exception eopen) {
        try {
          efaConfig.open(true);
          String msg = LogString.fileNewCreated(((DataFile) efaConfig.data()).getFilename(),
              International.getString("Konfigurationsdatei"));
          Logger.log(Logger.WARNING, Logger.MSG_CORE_EFACONFIGCREATEDNEW, msg);
        } catch (Exception ecreate) {
          String msg = LogString.fileCreationFailed(((DataFile) efaConfig.data()).getFilename(),
              International.getString("Konfigurationsdatei"));
          Logger.log(Logger.ERROR, Logger.MSG_CORE_EFACONFIGFAILEDCREATE, msg);
          if (isGuiAppl()) {
            Dialog.error(msg);
          }
          haltProgram(HALT_EFACONFIG);
        }
      }
      efaConfig.setExternalParameters(false);
    }
  }

  public static void iniEfaTypes(CustSettings custSettings) {
    if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
      Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniEfaTypes()");
    }
    if (applID == APPL_DRV) {
      return;
    }
    efaTypes = new EfaTypes(custSettings);
    try {
      efaTypes.open(false);
    } catch (Exception eopen) {
      try {
        efaTypes.open(true);
        String msg = LogString.fileNewCreated(((DataFile) efaTypes.data()).getFilename(),
            International.getString("Bezeichnungen"));
        Logger.log(Logger.WARNING, Logger.MSG_CORE_EFATYPESCREATEDNEW, msg);
      } catch (Exception ecreate) {
        String msg = LogString.fileCreationFailed(((DataFile) efaTypes.data()).getFilename(),
            International.getString("Bezeichnungen"));
        Logger.log(Logger.ERROR, Logger.MSG_CORE_EFATYPESFAILEDCREATE, msg);
        if (isGuiAppl()) {
          Dialog.error(msg);
        }
        haltProgram(HALT_EFATYPES);
      }
    }
    efaConfig.buildTypes();
  }

  public static void iniEfaRunning() {
    if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
      Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniEfaRunning()");
    }
    if (applID == APPL_CLI) {
      return;
    }
    efaRunning = new EfaRunning();
    if (efaRunning.isRunning()) {
      String msg = International
          .getString("efa läuft bereits und kann nicht zeitgleich zweimal gestartet werden!");
      Logger.log(Logger.ERROR, Logger.MSG_CORE_EFAALREADYRUNNING, msg);
      if (isGuiAppl()) {
        Dialog.error(msg);
      }
      haltProgram(HALT_EFARUNNING);
    }
    efaRunning.run();
    efaRunning.runDataLockThread();
  }

  public static void iniCopiedFiles() {
    if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
      Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniCopiedFiles()");
    }
    String distribCfgDirectory = efaMainDirectory + efaSubdirCFG + fileSep;
    tryCopy(distribCfgDirectory + WETTFILE, efaCfgDirectory + WETTFILE, true);
    tryCopy(distribCfgDirectory + WETTDEFS, efaCfgDirectory + WETTDEFS, true);
  }

  public static void iniAllDataFiles() {
    if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
      Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniAllDataFiles()");
    }
    wettDefs = new WettDefs(efaCfgDirectory + WETTDEFS);
    iniDataFile(wettDefs, true, International.onlyFor("Wettbewerbskonfiguration", "de"));
    keyStore = (applID != APPL_DRV ?
        new EfaKeyStore(efaDataDirectory + PUBKEYSTORE, "efa".toCharArray()) :
        new EfaKeyStore(efaDataDirectory + DRVKEYSTORE, "efa".toCharArray()));
  }

  public static void iniRemoteEfaServer() {
    if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
      Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniRemoteEfaServer()");
    }
    if (applID != APPL_EFABH) {
      return;
    }
    new RemoteEfaServer(efaConfig.getValueDataataRemoteEfaServerPort(),
        efaConfig.getValueDataRemoteEfaServerEnabled());
  }

  public static void iniEmailSenderThread() {
    if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
      Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniEmailSenderThread()");
    }
    if (applID == APPL_EFABASE || applID == APPL_EFABH) {
      try {
        emailSenderThread = new EmailSenderThread();
        emailSenderThread.start();
      } catch (NoClassDefFoundError e) {
        Logger.log(
            Logger.WARNING,
            Logger.MSG_CORE_MISSINGPLUGIN,
            International.getString("Fehlendes Plugin")
                + ": "
                + Plugins.PLUGIN_MAIL
                + " - "
                + International.getString("Kein email-Versand möglich!")
                + " "
                + International.getMessage(
                    "Bitte lade das fehlende Plugin unter der Adresse {url} herunter.",
                    pluginWebpage));
      }
    }
  }

  public static void iniScreenSize() {
    if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
      Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniScreenSize()");
    }
    if (isGuiAppl()) {
      Dialog.initializeScreenSize();
    }
  }

  public static void iniGUI() {
    if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
      Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniGUI()");
    }
    if (!isGuiAppl()) {
      return;
    }
    iniScreenSize();

    // Look&Feel
    if (efaConfig != null) { // is null for applDRV
      try {
        if (efaConfig.getValueLookAndFeel().length() == 0) {
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } else {
          UIManager.setLookAndFeel(efaConfig.getValueLookAndFeel());
        }
      } catch (Exception e) {
        Logger.log(Logger.WARNING, Logger.MSG_WARN_CANTSETLOOKANDFEEL,
            International.getString("Konnte Look&Feel nicht setzen") + ": " + e.toString());
      }
    }

    // Look&Feel specific Work-Arounds
    try {
      lookAndFeel = UIManager.getLookAndFeel().getClass().toString();
      if (!lookAndFeel.endsWith("MetalLookAndFeel")) {
        // to make PopupMenu's work properly and not swallow the next MousePressed Event, see:
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6753637
        Dialog.getUiDefaults().put("PopupMenu.consumeEventOnClose", false);
      }
      Color buttonFocusColor = (efaConfig != null ?
          efaConfig.getLafButtonFocusColor() : null);
      if (buttonFocusColor != null) {
        // colored square around text of selected button
        Dialog.getUiDefaults().put("Button.focus", new ColorUIResource(buttonFocusColor));
      }
      // allow users to press buttons by hitting ENTER (and not just SPACE)
      Dialog.getUiDefaults().put("Button.focusInputMap",
          new javax.swing.UIDefaults.LazyInputMap(new Object[] { "ENTER", "pressed",
              "released ENTER", "released",
              "SPACE", "pressed",
              "released SPACE", "released"
          }));
    } catch (Exception e) {
      Logger.log(Logger.WARNING, Logger.MSG_WARN_CANTSETLOOKANDFEEL,
          "Failed to apply LookAndFeel Workarounds: " + e.toString());
    }

    // Font Size
    if (applID == APPL_EFABH) {
      try {
        Dialog.setGlobalFontSize(efaConfig.getValueEfaDirekt_fontSize(),
            efaConfig.getValueEfaDirekt_fontStyle());
      } catch (Exception e) {
        Logger.log(
            Logger.WARNING,
            Logger.MSG_WARN_CANTSETFONTSIZE,
            International.getString("Schriftgröße konnte nicht geändert werden") + ": "
                + e.toString());
      }
    }
  }

  public static void iniChecks() {
    if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
      Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniChecks()");
    }
    checkEfaVersion(true);
    checkJavaVersion(true);
  }

  public static void iniDataFile(de.nmichael.efa.efa1.DatenListe f, boolean autoNewIfDoesntExist,
      String s) {
    if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
      Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION,
          "iniDataFile(" + f.getFileName() + "," + autoNewIfDoesntExist + "," + s + ")");
    }
    if (autoNewIfDoesntExist) {
      f.createNewIfDoesntExist();
    } else {
      if (!EfaUtil.canOpenFile(f.getFileName())) {
        if (f.writeFile()) {
          LogString.logInfo_fileNewCreated(f.getFileName(), s);
        } else {
          LogString.logError_fileCreationFailed(f.getFileName(), s);
        }
      }
    }
    if (!f.readFile()) {
      LogString.logError_fileOpenFailed(f.getFileName(), s);
    }

  }

  public static boolean isGuiAppl() {
    return (applID == APPL_EFABASE
        || applID == APPL_EFABH
        || applID == APPL_EMIL
        || applID == APPL_ELWIZ
        || applID == APPL_EDDI
        || applID == APPL_DRV) &&
        !CrontabThread.CRONJOB_THREAD_NAME.equals(Thread.currentThread().getName());
  }

  public static boolean isApplEfaBase() {
    return (applID == APPL_EFABASE);
  }

  public static boolean isApplEfaBoathouse() {
    return (applID == APPL_EFABH);
  }

  public static boolean isAdminMode() {
    return applID != APPL_EFABH || applMode == APPL_MODE_ADMIN;
  }

  public static boolean isWriteModeMitSchluessel() {
    return applID != APPL_EFABH || istSchluesselGedrehtIntern();
  }

  private static boolean istSchluesselGedrehtIntern() {
    String gpio = efaBaseConfig.efaUserDirectory;
    File fileGpio = new File(gpio + "value");
    File fileGut = new File(gpio + "value.gut.txt");
    try {
      String contentsGpio = FileUtils.readFileToString(fileGpio);
      String contentsGut = FileUtils.readFileToString(fileGut);
      return contentsGut.equals(contentsGpio);
    } catch (IOException e) {
      Logger.log(e);
      Dialog.exceptionError(e.getMessage(), e.fillInStackTrace().toString());
      return false;
    }
  }

  public static boolean isOsLinux() {
    return "Linux".equals(osName);
  }

  public static boolean isOsWindows() {
    return (osName != null && osName.startsWith("Windows"));
  }

  private static boolean checkAndCreateDirectory(String dir) {
    File f = new File(dir);
    if (!f.isDirectory()) {
      boolean result = f.mkdirs();
      if (result == true) {
        Logger.log(Logger.WARNING, Logger.MSG_CORE_SETUPDIRS,
            International.getMessage(
                "Verzeichnis '{directory}' konnte nicht gefunden werden und wurde neu erstellt.",
                dir));
      } else {
        Logger.log(Logger.ERROR, Logger.MSG_CORE_SETUPDIRS,
            International.getMessage(
                "Verzeichnis '{directory}' konnte weder gefunden, noch neu erstellt werden.", dir));
      }
      return result;
    }
    return true;
  }

  private static boolean tryCopy(String source, String dest, boolean alwaysCopyWhenNewer) {
    if (source.equals(dest)) {
      return true;
    }
    boolean copy = !(new File(dest)).exists();
    if (!copy) {
      File src = new File(source);
      File dst = new File(dest);
      if (src.exists() && dst.exists() && src.lastModified() > dst.lastModified()
          && alwaysCopyWhenNewer) {
        copy = true;
      }
    }
    if (copy) {
      if (EfaUtil.copyFile(source, dest)) {
        Logger.log(Logger.INFO, Logger.MSG_CORE_SETUPFILES,
            International.getMessage(
                "Datei '{file}' wurde aus der Vorlage {template} neu erstellt.", dest, source));
        return true;
      } else {
        Logger.log(Logger.ERROR, Logger.MSG_CORE_SETUPFILES,
            International.getMessage(
                "Datei '{file}' konnte nicht aus der Vorlage {template} neu erstellt werden.",
                dest, source));
        return false;
      }
    }
    return true; // nothing to do
  }

  public static boolean trySetEfaBackupDirectory(String dir) {
    if (dir == null || dir.length() == 0) {
      dir = efaBaseConfig.efaUserDirectory + efaSubdirBACKUP + fileSep;
    }
    if (!dir.endsWith(fileSep)) {
      dir = dir + fileSep;
    }
    if (checkAndCreateDirectory(dir)) {
      efaBakDirectory = dir;
      return true;
    }
    return false;
  }

  public static Vector getEfaInfos() {
    return getEfaInfos(true, true, true, true, false);
  }

  public static Vector getEfaInfos(boolean efaInfos,
      boolean pluginInfos,
      boolean javaInfos,
      boolean hostInfos,
      boolean jarInfos) {
    Vector infos = new Vector();

    // efa-Infos
    if (efaInfos) {
      infos.add("efa.version=" + VERSIONID);
      infos.add("efa.release.date=" + VERSIONRELEASEDATE);
      if (EFALIVE_VERSION != null && EFALIVE_VERSION.length() > 0) {
        infos.add("efalive.version=" + EFALIVE_VERSION);
      }
      if (applID != APPL_EFABH || applMode == APPL_MODE_ADMIN) {
        if (efaMainDirectory != null) {
          infos.add("efa.dir.main=" + efaMainDirectory);
        }
        if (efaBaseConfig != null && efaBaseConfig.efaUserDirectory != null) {
          infos.add("efa.dir.user=" + efaBaseConfig.efaUserDirectory);
        }
        if (efaProgramDirectory != null) {
          infos.add("efa.dir.program=" + efaProgramDirectory);
        }
        if (efaPluginDirectory != null) {
          infos.add("efa.dir.plugin=" + efaPluginDirectory);
        }
        if (efaImagesDirectory != null) {
          infos.add("efa.dir.images=" + efaImagesDirectory);
        }
        if (efaDataDirectory != null) {
          infos.add("efa.dir.data=" + efaDataDirectory);
        }
        if (efaCfgDirectory != null) {
          infos.add("efa.dir.cfg=" + efaCfgDirectory);
        }
        if (efaBakDirectory != null) {
          infos.add("efa.dir.bak=" + efaBakDirectory);
        }
        if (efaTmpDirectory != null) {
          infos.add("efa.dir.tmp=" + efaTmpDirectory);
        }
      }
    }

    // efa Plugin-Infos
    if (pluginInfos) {
      try {
        File dir = new File(efaPluginDirectory);
        if ((applID != APPL_EFABH || applMode == APPL_MODE_ADMIN) && Logger.isDebugLogging()) {
          File[] files = dir.listFiles();
          for (File file : files) {
            if (file.isFile()) {
              infos.add("efa.plugin.file=" + file.getName() + ":" + file.length());
            }
          }
        }

        Plugins plugins = Plugins.getPluginInfoFromLocalFile();
        String[] names = plugins.getAllPluginNames();
        for (String name : names) {
          infos.add("efa.plugin." + name + "=" +
              (Plugins.isPluginInstalled(name) ? "installed" : "not installed"));
        }
      } catch (Exception e) {
        Logger.log(Logger.ERROR, Logger.MSG_CORE_INFOFAILED,
            International.getString("Programminformationen konnten nicht ermittelt werden") + ": "
                + e.toString());
        return null;
      }
    }

    // Java Infos
    if (javaInfos) {
      infos.add("java.version=" + System.getProperty("java.version"));
      infos.add("java.vendor=" + System.getProperty("java.vendor"));
      infos.add("java.home=" + System.getProperty("java.home"));
      infos.add("java.vm.version=" + System.getProperty("java.vm.version"));
      infos.add("java.vm.vendor=" + System.getProperty("java.vm.vendor"));
      infos.add("java.vm.name=" + System.getProperty("java.vm.name"));
      infos.add("os.name=" + System.getProperty("os.name"));
      infos.add("os.arch=" + System.getProperty("os.arch"));
      infos.add("os.version=" + System.getProperty("os.version"));
      if (applID != APPL_EFABH || applMode == APPL_MODE_ADMIN) {
        infos.add("user.home=" + System.getProperty("user.home"));
        infos.add("user.name=" + System.getProperty("user.name"));
        infos.add("user.dir=" + System.getProperty("user.dir"));
        infos.add("java.class.path=" + System.getProperty("java.class.path"));
      }
    }

    // Host Infos
    if (hostInfos) {
      if (applID != APPL_EFABH || applMode == APPL_MODE_ADMIN) {
        try {
          infos.add("host.name=" + InetAddress.getLocalHost().getCanonicalHostName());
          infos.add("host.ip=" + InetAddress.getLocalHost().getHostAddress());
          infos.add("host.interface="
              + EfaUtil.getInterfaceInfo(NetworkInterface.getByInetAddress(InetAddress
                  .getLocalHost())));
        } catch (Exception eingore) {}
      }
    }

    // JAR methods
    if (jarInfos && Logger.isDebugLogging()) {
      try {
        String cp = System.getProperty("java.class.path");
        while (cp != null && cp.length() > 0) {
          int pos = cp.indexOf(";");
          if (pos < 0) {
            pos = cp.indexOf(":");
          }
          String jarfile;
          if (pos >= 0) {
            jarfile = cp.substring(0, pos);
            cp = cp.substring(pos + 1);
          } else {
            jarfile = cp;
            cp = null;
          }
          if (jarfile != null && jarfile.length() > 0 && new File(jarfile).isFile()) {
            try {
              infos.add("java.jar.filename=" + jarfile);
              JarFile jar = new JarFile(jarfile);
              Enumeration _enum = jar.entries();
              Object o;
              while (_enum.hasMoreElements() && (o = _enum.nextElement()) != null) {
                infos.add("java.jar.content="
                    + o + ":"
                    + (jar.getEntry(o.toString()) == null ? "null" : Long.toString(jar.getEntry(
                        o.toString()).getSize())));
              }
              jar.close();
            } catch (Exception e) {
              Logger.log(Logger.ERROR, Logger.MSG_CORE_INFOFAILED, e.toString());
              return null;
            }
          }
        }
      } catch (Exception e) {
        Logger.log(Logger.ERROR, Logger.MSG_CORE_INFOFAILED,
            International.getString("Programminformationen konnten nicht ermittelt werden") + ": "
                + e.toString());
        return null;
      }
    }
    return infos;
  }

  public static void printEfaInfos(boolean efaInfos, boolean pluginInfos, boolean javaInfos,
      boolean hostInfos, boolean jarInfos) {
    Vector infos = getEfaInfos(efaInfos, pluginInfos, javaInfos, hostInfos, jarInfos);
    for (int i = 0; infos != null && i < infos.size(); i++) {
      Logger.log(Logger.INFO, Logger.MSG_INFO_CONFIGURATION, (String) infos.get(i));
    }
  }

  public static String getEfaImage(int size) {
    switch (size) {
      case 1:  return IMAGEPATH + "efa_small.png";
      case 2:  return IMAGEPATH + "efa_logo.png";
      case 3:  return IMAGEPATH + "efa_large.png";
      default: return IMAGEPATH + "efa_logo.png";
    }
  }

  public static void checkEfaVersion(boolean interactive) {
    // @todo (P7) check for outdated efa version
  }

  public static void checkJavaVersion(boolean interactive) {
    // @todo (P7) check for outdated java version
  }

  public static void checkRegister() {
    if (PROGRAMMID.equals(efaConfig.getValueRegisteredProgramID())) {
      return; // already registered
    }
    efaConfig.setValueRegistrationChecks(efaConfig.getValueRegistrationChecks() + 1);

    boolean promptForRegistration = false;
    if (efaConfig.getValueRegisteredProgramID().length() == 0) {
      // never before registered
      if (efaConfig.getValueRegistrationChecks() <= 30
          && efaConfig.getValueRegistrationChecks() % 10 == 0) {
        promptForRegistration = true;
      }
    } else {
      // previous version already registered
      if (efaConfig.getValueRegistrationChecks() <= 10
          && efaConfig.getValueRegistrationChecks() % 10 == 0) {
        promptForRegistration = true;
      }
    }

    if (promptForRegistration) {
      if (BrowserDialog.openInternalBrowser(null, EFA_SHORTNAME,
          "file:" + HtmlFactory.createRegister(),
          850, 750).endsWith(".pl")) {
        // registration complete
        efaConfig.setValueRegisteredProgramID(PROGRAMMID);
        efaConfig.setValueRegistrationChecks(0);
      }

    }
  }
}
