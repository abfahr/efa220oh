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

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.ItemTypeDataRecordTable;
import de.nmichael.efa.data.BoatStatusRecord;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.util.International;

// @i18n complete
public class BoatStatusListDialog extends DataListDialog {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public BoatStatusListDialog(Frame parent, AdminRecord admin) {
    super(parent, International.getString("Bootsstatus"),
        Daten.project.getBoatStatus(false), -1, admin);
    actionText = new String[] { ItemTypeDataRecordTable.ACTIONTEXT_EDIT };
    actionType = new int[] { ItemTypeDataRecordTable.ACTION_EDIT };
    actionImage = new String[] { BaseDialog.IMAGE_EDIT };
    intelligentColumnWidth = false;
  }

  public BoatStatusListDialog(JDialog parent, AdminRecord admin) {
    super(parent, International.getString("Bootsstatus"),
        Daten.project.getBoatStatus(false), -1, admin);
    actionText = new String[] { ItemTypeDataRecordTable.ACTIONTEXT_EDIT };
    actionType = new int[] { ItemTypeDataRecordTable.ACTION_EDIT };
    actionImage = new String[] { BaseDialog.IMAGE_EDIT };
    intelligentColumnWidth = false;
  }

  @Override
  public void keyAction(ActionEvent evt) {
    _keyAction(evt);
  }

  @Override
  public DataEditDialog createNewDataEditDialog(JDialog parent, StorageObject persistence,
      DataRecord record) {
    if (record == null) {
      return null;
    }
    return new BoatStatusEditDialog(parent, (BoatStatusRecord) record, false, admin);
  }
}
