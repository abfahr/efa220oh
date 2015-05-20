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

import de.nmichael.efa.data.Persons;
import de.nmichael.efa.data.Project;

public class MenuPersons extends MenuData {

  public MenuPersons(CLI cli) {
    super(cli);
    this.storageObject = cli.getPersistence(Persons.class,
        Project.STORAGEOBJECT_PERSONS, Persons.DATATYPE);
    this.storageObjectDescription = "persons";
  }

  @Override
  public int runCommand(Stack<String> menuStack, String cmd, String args) {
    int ret = super.runCommand(menuStack, cmd, args);
    if (ret < 0) {
      return CLI.RC_UNKNOWN_COMMAND;
    } else {
      return ret;
    }
  }

}
