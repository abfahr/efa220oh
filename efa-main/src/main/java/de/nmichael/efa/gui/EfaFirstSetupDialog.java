/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.SwingConstants;

import de.nmichael.efa.core.config.*;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.data.storage.MetaData;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.gui.util.EfaMenuButton;
import org.apache.batik.ext.swing.GridBagConstants;

import de.nmichael.efa.Daten;
import de.nmichael.efa.data.storage.DataFile;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;

public class EfaFirstSetupDialog extends StepwiseDialog {

  private static final long serialVersionUID = 1L;
  static final String ADMIN_NAME = "ADMIN_NAME";
  static final String ADMIN_PASSWORD = "ADMIN_PASSWORD";

  static final String CUST_ROWING = "CUST_ROWING";
  static final String CUST_ROWINGGERMANY = "CUST_ROWINGGERMANY";
  static final String CUST_ROWINGBERLIN = "CUST_ROWINGBERLIN";
  static final String CUST_CANOEING = "CUST_CANOEING";
  static final String CUST_CANOEINGGERMANY = "CUST_CANOEINGGERMANY";

  static final String EFALIVE_CREATEADMIN = "EFALIVE_CREATEADMIN";

  private final boolean restoreFromBackup;
  private final boolean createSuperAdmin;
  private final boolean efaCustomization;
  private final boolean efaLiveAdmin;
  private CustSettings custSettings = null;
  private AdminRecord newSuperAdmin = null;

  public EfaFirstSetupDialog(boolean restoreFromBackup, boolean createSuperAdmin, boolean efaCustomization) {
    super((JFrame) null, Daten.EFA_LONGNAME);
    this.restoreFromBackup = restoreFromBackup;
    this.createSuperAdmin = createSuperAdmin;
    this.efaCustomization = efaCustomization;
    this.efaLiveAdmin = Daten.EFALIVE_VERSION != null && !Daten.admins.isEfaLiveAdminOk();
  }

  @Override
  public void keyAction(ActionEvent evt) {
    _keyAction(evt);
  }

  private int getNumberOfSteps() {
    int stepCnt = 1;
    if (restoreFromBackup) {
      stepCnt++;
    }
    if (createSuperAdmin) {
      stepCnt++;
    }
    if (efaCustomization) {
      stepCnt++;
    }
    if (efaLiveAdmin) {
      stepCnt++;
    }
    return stepCnt;
  }

  private int getRestoreFromBackupStep() {
    return (restoreFromBackup ? 1 : 99);
  }
  private int getCreateSuperAdminStep() {
    return (createSuperAdmin ? (restoreFromBackup ? 2 : 1) : 99);
  }
  private int getEfaCustomizationStep() {
    return (efaCustomization ? (createSuperAdmin ? (restoreFromBackup ? 3 : 2) : 1) : 99);
  }
  private int getEfaLiveAdminStep() {
    return (efaLiveAdmin ? (efaCustomization ? (createSuperAdmin ? (restoreFromBackup ? 4 : 3) : 2) : 1) : 99);
  }

  @Override
  String[] getSteps() {
    int i = 0;
    String[] steps = new String[getNumberOfSteps()];
    steps[i++] = International.getString("Willkommen!");
    if (restoreFromBackup) {
      steps[i++] = International.getString("Backup einspielen?");
    }
    if (createSuperAdmin) {
      steps[i++] = International.getString("Hauptadministrator anlegen");
    }
    if (efaCustomization) {
      steps[i++] = International.getString("Einstellungen");
    }
    if (efaLiveAdmin) {
      steps[i++] = Daten.EFA_LIVE;
    }
    return steps;
  }

  @Override
  String getDescription(int step) {
    if (step == 0) {
      return International.getString("Willkommen bei efa, dem elektronischen Fahrtenbuch!")
          + "\n"
          + International.getString(
              "Dieser Dialog führt Dich durch die ersten Schritte, um efa einzurichten.");
    }
    if (step == getRestoreFromBackupStep()) {
      return International.getString("Alte oder aktuelle Backup-Datei aus efa importieren.")
              + "\n"
              + International.getString("Bitte passende Datei im Filesystem aussuchen.");
    }
    if (step == getCreateSuperAdminStep()) {
      return International
          .getString("Alle Administrationsaufgaben in efa erfordern Administratorrechte.")
          + "\n"
          + International.getString(
              "Bitte lege ein Paßwort (mindestens 6 Zeichen) für den Hauptadministrator 'admin' fest.");
    }
    if (step == getEfaCustomizationStep()) {
      return International.getString("Welche Funktionen von efa möchtest Du verwenden?")
          + "\n"
          + International.getString(
              "Du kannst diese Einstellungen jederzeit in der efa-Konfiguration ändern.");
    }
    if (step == getEfaLiveAdminStep()) {
      return International.getString(
          "Bestimmte Funktionen von efaLive (Erstellen oder Einspielen eines Backups) erfordern, daß efaLive Administrator-Zugriff auf efa hat.")
          + "\n"
          + International.getString("Möchtest Du jetzt einen Administrator für efaLive anlegen?")
          + " "
          + International.getString(
              "Du kannst diesen Administrator jederzeit in der Verwaltung der Administratoren wieder löschen.");
    }
    return "";
  }

  @Override
  void initializeItems() {
    items = new ArrayList<>();
    IItemType item;

    // Items for Step 0
    items.add(item = new ItemTypeLabel("LOGO", IItemType.TYPE_PUBLIC, "0", ""));
    ((ItemTypeLabel) item).setImage(getIcon(Daten.getEfaImage(3)));
    item.setFieldGrid(-1, GridBagConstants.CENTER, GridBagConstants.HORIZONTAL);
    item.setPadding(10, 10, 10, 10);
    items.add(
        item = new ItemTypeLabel(Daten.EFA_GROSS, IItemType.TYPE_PUBLIC, "0", Daten.EFA_LONGNAME));
    ((ItemTypeLabel) item).setHorizontalAlignment(SwingConstants.CENTER);
    item.setFieldGrid(-1, GridBagConstants.CENTER, GridBagConstants.HORIZONTAL);
    items.add(item = new ItemTypeLabel("VERSION", IItemType.TYPE_PUBLIC, "0", International
        .getString("Version") + " " + Daten.VERSION));
    ((ItemTypeLabel) item).setHorizontalAlignment(SwingConstants.CENTER);
    item.setFieldGrid(-1, GridBagConstants.CENTER, GridBagConstants.HORIZONTAL);

    // Items for Step 1 (RestoreBackup)
    items.add(item = new ItemTypeButton("ADMIN_LABEL", IItemType.TYPE_PUBLIC,
            Integer.toString(getRestoreFromBackupStep()), International
            .getString("1. Aktuelles Backup herunterladen")));
    item.registerItemListener((itemType, event) -> showBrowser(event));
    items.add(item = new ItemTypeButton("ADMIN_LABEL", IItemType.TYPE_PUBLIC,
            Integer.toString(getRestoreFromBackupStep()), International
            .getString("2. jetzt Backup-Datei einspielen")));
    item.registerItemListener((itemType, event) -> performBackupRestoreDialog(event));
    items.add(item = new ItemTypeButton("ADMIN_LABEL", IItemType.TYPE_PUBLIC,
            Integer.toString(getRestoreFromBackupStep()), International
            .getString("3. schließlich EFA neustarten")));
    item.registerItemListener((itemType, event) -> restartEFA(event));

    // Items for Step 2 (CreateSuperAdmin)
    items.add(item = new ItemTypeLabel("ADMIN_LABEL", IItemType.TYPE_PUBLIC,
            Integer.toString(getCreateSuperAdminStep()), International
            .getString("Neuer Hauptadministrator")));
    items.add(item = new ItemTypeString(ADMIN_NAME, Admins.SUPERADMIN, IItemType.TYPE_PUBLIC,
        Integer.toString(getCreateSuperAdminStep()), International.getString("Name")));
    item.setEditable(false);
    items.add(item = new ItemTypePassword(ADMIN_PASSWORD, "", IItemType.TYPE_PUBLIC,
        Integer.toString(getCreateSuperAdminStep()), International.getString("Paßwort")));
    item.setNotNull(true);
    ((ItemTypePassword) item).setMinCharacters(6);
    items.add(item = new ItemTypePassword(ADMIN_PASSWORD + "_REPEAT", "", IItemType.TYPE_PUBLIC,
        Integer.toString(getCreateSuperAdminStep()), International.getString("Paßwort") +
            " (" + International.getString("Wiederholung") + ")"));
    item.setNotNull(true);
    ((ItemTypePassword) item).setMinCharacters(6);

    // Items for Step 3 (EfaCustomization)
    items.add(item = new ItemTypeLabel("CUST_LABEL", IItemType.TYPE_PUBLIC,
        Integer.toString(getEfaCustomizationStep()),
        International.getString("Welche Funktionen von efa möchtest Du verwenden?")));
    items.add(item = new ItemTypeBoolean(CUST_ROWING, false, IItemType.TYPE_PUBLIC,
        Integer.toString(getEfaCustomizationStep()), International.getString("Rudern")));
    items.add(item = new ItemTypeBoolean(CUST_ROWINGGERMANY, false
            && International.getLanguageID().startsWith("de"), IItemType.TYPE_PUBLIC,
        Integer.toString(getEfaCustomizationStep()),
        International.getString("Rudern") + " " +
            International.getMessage("in {region}",
                International.getString("Deutschland"))));
    items.add(item = new ItemTypeBoolean(CUST_ROWINGBERLIN, false, IItemType.TYPE_PUBLIC,
        Integer.toString(getEfaCustomizationStep()),
        International.getString("Rudern") + " " +
            International.getMessage("in {region}",
                International.getString("Berlin"))));
    items.add(item = new ItemTypeBoolean(CUST_CANOEING, true, IItemType.TYPE_PUBLIC,
        Integer.toString(getEfaCustomizationStep()),
        International.getString("Kanufahren")));
    items.add(item = new ItemTypeBoolean(CUST_CANOEINGGERMANY, false, IItemType.TYPE_PUBLIC,
        Integer.toString(getEfaCustomizationStep()),
        International.getString("Kanufahren") + " " +
            International.getMessage("in {region}",
                International.getString("Deutschland"))));

    // Items for Step 4 (EfaLiveAdmin)
    items.add(item = new ItemTypeBoolean(EFALIVE_CREATEADMIN, true, IItemType.TYPE_PUBLIC,
        Integer.toString(getEfaLiveAdminStep()),
        International.getMessage("Admin '{name}' erstellen", Admins.EFALIVEADMIN)));
  }

  @Override
  boolean checkInput(int direction) {
    boolean ok = super.checkInput(direction);
    if (ok && step == getCreateSuperAdminStep()) {
      String pass1 = getItemByName(ADMIN_PASSWORD).toString();
      String pass2 = getItemByName(ADMIN_PASSWORD + "_REPEAT").toString();
      if (!pass1.equals(pass2)) {
        Dialog.error(International.getMessage("Paßwort in Feld '{field}' nicht identisch.",
            getItemByName(ADMIN_PASSWORD + "_REPEAT").getDescription()));
        return false;
      }
    }
    return ok;
  }

  @Override
  boolean finishButton_actionPerformed(ActionEvent e) {
    if (!checkInput(0)) {
      return false;
    }
    //if (restoreFromBackup && backupMode != BackupMode.ignore) {
      //restoreFromBackup();
    //}
    if (createSuperAdmin) {
      createNewSuperAdmin(((ItemTypePassword) getItemByName(ADMIN_PASSWORD)).getValue());
    }
    if (efaCustomization) {
      custSettings = new CustSettings();
      custSettings.activateRowingOptions = ((ItemTypeBoolean) getItemByName(CUST_ROWING)).getValue();
      custSettings.activateGermanRowingOptions = ((ItemTypeBoolean) getItemByName(CUST_ROWINGGERMANY)).getValue();
      custSettings.activateBerlinRowingOptions = ((ItemTypeBoolean) getItemByName(CUST_ROWINGBERLIN)).getValue();
      custSettings.activateCanoeingOptions = ((ItemTypeBoolean) getItemByName(CUST_CANOEING)).getValue();
      custSettings.activateGermanCanoeingOptions = ((ItemTypeBoolean) getItemByName(CUST_CANOEINGGERMANY)).getValue();
    }
    if (efaLiveAdmin) {
      if (((ItemTypeBoolean) getItemByName(EFALIVE_CREATEADMIN)).getValue()) {
        Daten.admins.createOrFixEfaLiveAdmin();
      }
    }
    setDialogResult(true);
    cancel();
    return true;
  }

  private void showBrowser(AWTEvent event) {
    if (event.getID() == FocusEvent.FOCUS_GAINED) return;
    if (event.getID() == FocusEvent.FOCUS_LOST) return;

    // showBrowser with URL https://overfreunde.abfx.de/efa2/backup/
    Desktop desktop = Desktop.getDesktop();
    if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE)) {
      try {
        URI uri = new URI("https://overfreunde.abfx.de/efa2/backup/");
        desktop.browse(uri);
      } catch (URISyntaxException eURI) {
        System.out.println("URISyntaxException = " + eURI + "");
      } catch (IOException eIO) {
        System.out.println("IOException = " + eIO + "");
      }
    }
  }

  private void performBackupRestoreDialog(AWTEvent event) {
    if (event.getID() == FocusEvent.FOCUS_GAINED) return;
    if (event.getID() == FocusEvent.FOCUS_LOST) return;

    // now check permissions and perform the menu action

    try {
      if (Daten.efaConfig == null) {
        Daten.efaConfig = new EfaConfig();
        Daten.efaConfig.open(true);
      }
      if (Daten.efaTypes == null) {
        Daten.efaTypes = new EfaTypes(null);
        Daten.efaTypes.open(true);
      }
      if (Daten.admins == null) {
        Daten.admins = new Admins();
        Daten.admins.open(true);
      }
    } catch (EfaException e) {
      Logger.log(Logger.ERROR, Logger.MSG_ABF_ERROR, e);
    }

    // now check permissions and perform the menu action
    Admins admins = Daten.admins;
    AdminRecord adminRecord = new AdminRecord(admins, MetaData.getMetaData(Admins.DATATYPE));
    adminRecord.setAllowedRestoreBackup(true);

    EfaMenuButton.menuAction(this, EfaMenuButton.BUTTON_BACKUP, adminRecord, null);
  }

  private void restartEFA(AWTEvent event) {
    if (event.getID() == FocusEvent.FOCUS_GAINED) return;
    if (event.getID() == FocusEvent.FOCUS_LOST) return;

    Dialog.infoDialog("Bitte EFA neustarten");
    //setDialogResult(true);
    cancel();
  }

  void createNewSuperAdmin(String password) {
    if (password == null || password.length() == 0) {
      return;
    }
    try {
      Daten.admins.open(true);
      // ok, new admin file created (or existing, empty one opened). Now add admin
      AdminRecord r = Daten.admins.createAdminRecord(Admins.SUPERADMIN, password);
      Daten.admins.data().add(r);
      // Now delete sec file
      Daten.efaSec.delete(true);
      newSuperAdmin = r;
    } catch (Exception ee) {
      String msg = LogString.fileCreationFailed(((DataFile) Daten.admins.data()).getFilename(),
          International.getString("Administratoren"));
      Logger.log(Logger.ERROR, Logger.MSG_CORE_ADMINSFAILEDCREATE, msg);
      if (Daten.isGuiAppl()) {
        Dialog.error(msg);
      }
      Daten.haltProgram(Daten.HALT_ADMIN);
    }
    String msg = LogString.fileNewCreated(((DataFile) Daten.admins.data()).getFilename(),
        International.getString("Administratoren"));
    Logger.log(Logger.WARNING, Logger.MSG_CORE_ADMINSCREATEDNEW, msg);
    Dialog
        .infoDialog(
            International.getString("Neuer Hauptadministrator"),
            International.getString(
                "Ein neuer Administrator mit Namen 'admin' wurde angelegt. Bitte notiere Dir Name und Paßwort an einem sicheren Ort."));
  }

  public CustSettings getCustSettings() {
    return custSettings;
  }

  public AdminRecord getNewSuperAdmin() {
    return newSuperAdmin;
  }
}
