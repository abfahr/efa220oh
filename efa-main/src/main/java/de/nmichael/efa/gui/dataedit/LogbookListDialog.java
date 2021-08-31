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
import de.nmichael.efa.core.items.ItemTypeDataRecordTable;
import de.nmichael.efa.data.Logbook;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.gui.util.EfaMenuButton;
import de.nmichael.efa.util.International;

// @i18n complete
public class LogbookListDialog extends DataListDialog {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public static final int ACTION_CORRECTIONASSISTENT = -201; // negative actions will not be shown
  // as popup actions

  private Logbook logbook;

  public LogbookListDialog(Frame parent, AdminRecord admin, Logbook logbook) {
    super(parent, International.getString("Fahrtenbuch") + " " + logbook.getName(),
        logbook, -1, admin);
    iniSettings();
  }

  public LogbookListDialog(JDialog parent, AdminRecord admin, Logbook logbook) {
    super(parent, International.getString("Fahrtenbuch") + " " + logbook.getName(),
        logbook, -1, admin);
    iniSettings();
  }

  private void iniSettings() {
    removeAction(ItemTypeDataRecordTable.ACTION_NEW);
    removeAction(ItemTypeDataRecordTable.ACTION_EDIT);
    removeAction(ItemTypeDataRecordTable.ACTION_DELETE);
    addAction(International.getString("Korrekturassistent"),
        ACTION_CORRECTIONASSISTENT,
        BaseDialog.IMAGE_CORRECTION);
    minColumnWidths = new int[] { 80, 150, 150, 200, 150, 80 };
  }

  @Override
  public void keyAction(ActionEvent evt) {
    _keyAction(evt);
  }

  @Override
  public void itemListenerActionTable(int actionId, DataRecord[] records) {
    super.itemListenerActionTable(actionId, records);
    switch (actionId) {
      case ACTION_CORRECTIONASSISTENT:
        if (admin == null || !admin.isAllowedAdvancedEdit()) {
          EfaMenuButton.insufficientRights(admin, International.getString("Korrekturassistent"));
          break;
        }
        try {
          FixLogbookDialog dlg = new FixLogbookDialog(this, (Logbook) persistence, admin);
          dlg.showDialog();
        } catch (Exception e) {
          e.printStackTrace();
        }
        break;
    }
  }

  @Override
  public DataEditDialog createNewDataEditDialog(JDialog parent, StorageObject persistence,
      DataRecord record) {
    if (record == null) {
      return null;
    }
    return null;
  }
}
