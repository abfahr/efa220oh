/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.cli;

import java.util.Stack;

public class MenuSyncEfb extends MenuBase {

  public static final String CMD_RUN = "run";

  public MenuSyncEfb(CLI cli) {
    super(cli);
  }

  @Override
  public void printHelpContext() {
    printUsage(CMD_RUN, "[logbook]", "run synchronization with Kanu-eFB");
  }

  private int syncEfb(String args) {
    cli.logerr("You don't have permission to access this function.");
    return CLI.RC_NO_PERMISSION;
  }

  @Override
  public int runCommand(Stack<String> menuStack, String cmd, String args) {
    int ret = super.runCommand(menuStack, cmd, args);
    if (ret < 0) {
      if (cmd.equalsIgnoreCase(CMD_RUN)) {
        return syncEfb(args);
      }
      return CLI.RC_UNKNOWN_COMMAND;
    } else {
      return ret;
    }
  }
}
