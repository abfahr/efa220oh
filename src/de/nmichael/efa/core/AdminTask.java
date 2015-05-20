/**
 * Title: efa - elektronisches Fahrtenbuch für Ruderer Copyright: Copyright (c)
 * 2001-2011 by Nicolas Michael Website: http://efa.nmichael.de/ License: GNU
 * General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.core;

import javax.swing.JDialog;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.data.Clubwork;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.gui.AdminDialog;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

/**
 * Task that is executed whenever admin logs in. This task runs in the background whenever an admin
 * logs in interavtively (that is, in GUI mode). After being started, it sleeps for 1 second, then
 * waits for a maximum of 10 seconds until a project has successfully been opened. Once that is the
 * case, it runs the configured actions. Once all actions have completed, this task completes. It
 * does NOT run in the background indefinitely. In situations where opening of a project fails or
 * takes very long, this task may not execute any actions at all.
 */
public class AdminTask extends Thread {

  private static AdminTask task;
  private AdminRecord admin;
  private JDialog parent;

  public AdminTask(AdminRecord admin, JDialog parent) {
    this.admin = admin;
    this.parent = parent;
  }

  private void runActions() {
    Logger.log(Logger.DEBUG, Logger.MSG_CORE_ADMINTASK, "running AdminTask ...");
    // Actions to be implemented here!
    // For each action, check whether admin has necessary permissions.
    if (admin.isAllowedAdministerProjectClubwork()) {
      checkForClubworkCarryOver();
    }
  }

  @Override
  public void run() {
    try {
      boolean ready = false;
      for (int tries = 0; tries < 11; tries++) {
        // always start task with 1000 ms delay
        try {
          Thread.sleep(1000);
        } catch (Exception eignore) {}
        if (Daten.project != null && Daten.project.isOpen()
            && !Daten.project.isInOpeningProject()) {
          ready = true;
          break;
        }
      }
      if (ready) {
        runActions();
      }
    } catch (Exception e) {
      Logger.logdebug(e);
    }
    task = null;
  }

  public static void startAdminTask(AdminRecord admin, JDialog dlg) {
    if (!Daten.isGuiAppl()) {
      return;
    }
    if (admin == null) {
      return;
    }
    if (task != null && task.isAlive()) {
      return;
    }
    task = new AdminTask(admin, dlg);
    task.start();
  }

  private void checkForClubworkCarryOver() {
    String[] clubworkNames = Daten.project.getAllClubworkNames();
    if (clubworkNames != null && clubworkNames.length > 0) {
      Clubwork clubwork = Daten.project.getCurrentClubwork();
      if (clubwork != null && clubwork.isOpen()) {
        DataTypeDate date = clubwork.getEndDate();
        if (date != null && DataTypeDate.today().isAfterOrEqual(date)) {
          int res = Dialog
              .yesNoDialog(
                  International.getString("Übertrag berechnen"),
                  International
                  .getMessage(
                      "Möchtest Du den Übertrag für das Vereinsarbeitsbuch '{record}' wirklich berechnen?",
                      clubwork.getName()));
          if (res == Dialog.YES) {
            clubwork.doCarryOver(1, parent);
            ((AdminDialog) parent).updateInfos();
          }
        }
      } else {
        Dialog.error(International.getString("Kein Vereinsarbeitsbuch geöffnet.")
            + International.getMessage("Berechnen des {verb}s nicht möglich.",
                International.getString("Übertrag")));
      }
    }
  }
}
