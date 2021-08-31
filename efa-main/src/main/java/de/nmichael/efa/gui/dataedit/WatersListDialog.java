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
import java.util.UUID;

import javax.swing.JDialog;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.data.Waters;
import de.nmichael.efa.data.WatersRecord;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;

// @i18n complete
public class WatersListDialog extends DataListDialog {

  /**
   *
   */
  private static final long serialVersionUID = 1L;
  public static final int ACTION_CREATEFROMTEMPLATE = 901; // negative actions will not be shown as
  // popup actions

  public WatersListDialog(Frame parent, AdminRecord admin) {
    super(parent, International.getString("Gewässer"), Daten.project.getWaters(false), 0, admin);
    addCreateWatersButton();
    intelligentColumnWidth = false;
  }

  public WatersListDialog(JDialog parent, AdminRecord admin) {
    super(parent, International.getString("Gewässer"), Daten.project.getWaters(false), 0, admin);
    addCreateWatersButton();
    intelligentColumnWidth = false;
  }

  private void addCreateWatersButton() {
    try {
      Daten.project.getWaters(false);
      if (Waters.getResourceTemplate(International.getLanguageID()) != null) {
        addAction(International.getString("Gewässerliste erstellen"),
            ACTION_CREATEFROMTEMPLATE,
            BaseDialog.IMAGE_SPECIAL);
      }
    } catch (Exception eignore) {}
  }

  @Override
  public void keyAction(ActionEvent evt) {
    _keyAction(evt);
  }

  @Override
  public void itemListenerActionTable(int actionId, DataRecord[] records) {
    super.itemListenerActionTable(actionId, records);
    switch (actionId) {
      case ACTION_CREATEFROMTEMPLATE:
        int count = Daten.project.getWaters(false).addAllWatersFromTemplate(
            International.getLanguageID());
        if (count > 0) {
          Dialog.infoDialog(International.getMessage(
              "{count} Gewässer aus Gewässerkatalog erfolgreich hinzugefügt.",
              count));
        } else {
          Dialog
          .infoDialog(International
              .getString("Alle Gewässer aus dem Gewässerkatalog sind bereits vorhanden (keine neuen hinzugefügt)."));
        }
        break;
    }
  }

  @Override
  public DataEditDialog createNewDataEditDialog(JDialog parent, StorageObject persistence,
      DataRecord record) {
    boolean newRecord = (record == null);
    if (record == null) {
      record = Daten.project.getWaters(false).createWatersRecord(UUID.randomUUID());
    }
    return new WatersEditDialog(parent, (WatersRecord) record, newRecord, admin);
  }
}
