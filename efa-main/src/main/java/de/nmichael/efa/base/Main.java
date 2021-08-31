/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.base;

import de.nmichael.efa.Daten;
import de.nmichael.efa.Program;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.data.Project;
import de.nmichael.efa.gui.EfaBaseFrame;
import de.nmichael.efa.util.International;

// @i18n complete
public class Main extends Program {

  private String project = null;

  public Main(String[] args) {
    super(Daten.APPL_EFABASE, args);
    AdminRecord admin = getNewlyCreatedAdminRecord();

    if (project != null) {
      Project.openProject(project, true);
    }

    EfaBaseFrame frame = new EfaBaseFrame(EfaBaseFrame.MODE_BASE);
    if (admin != null) {
      frame.setAdmin(admin);
    }
    frame.showMe();
    Daten.iniSplashScreen(false);
  }

  @Override
  public void printUsage(String wrongArgument) {
    super.printUsage(wrongArgument);
    printOption("-open <project>", International.getString("Projekt <project> öffnen"));
    System.exit(0);
  }

  @Override
  public void checkArgs(String[] args) {
    super.checkArgs(args);
    for (int i = 0; i < args.length; i++) {
      if (args[i] == null) {
        continue; // argument already handled by super class
      }
      if (args[i].equals("-open") && i + 1 < args.length) {
        args[i] = null;
        project = args[++i];
        args[i] = null;
        continue;
      }
    }
    checkRemainingArgs(args);
  }

  public static void main(String[] args) {
    new Main(args);
  }

}
