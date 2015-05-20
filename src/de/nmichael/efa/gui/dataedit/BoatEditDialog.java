/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui.dataedit;

import java.awt.Frame;
import java.awt.event.ActionEvent;

import javax.swing.JDialog;

import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.data.BoatRecord;
import de.nmichael.efa.data.BoatStatus;
import de.nmichael.efa.ex.InvalidValueException;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

// @i18n complete
public class BoatEditDialog extends VersionizedDataEditDialog {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public BoatEditDialog(Frame parent, BoatRecord r, boolean newRecord, AdminRecord admin) {
    super(parent, International.getString("Boot"), r, newRecord, admin);
  }

  public BoatEditDialog(JDialog parent, BoatRecord r, boolean newRecord, AdminRecord admin) {
    super(parent, International.getString("Boot"), r, newRecord, admin);
  }

  @Override
  public void keyAction(ActionEvent evt) {
    _keyAction(evt);
  }

  @Override
  protected boolean saveRecord() throws InvalidValueException {
    boolean success = super.saveRecord();
    if (success) {
      if (newRecord && dataRecord != null) {
        BoatStatus boatStatus = dataRecord.getPersistence().getProject().getBoatStatus(false);
        if (boatStatus != null) {
          try {
            boatStatus.data().add(
                boatStatus.createBoatStatusRecord(((BoatRecord) dataRecord).getId(), null));
          } catch (Exception e) {
            Logger.log(e);
          }
        }
      }
    }
    return success;
  }
}
