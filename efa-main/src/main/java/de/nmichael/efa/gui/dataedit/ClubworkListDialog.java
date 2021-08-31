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

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.UUID;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import de.nmichael.efa.Daten;
import de.nmichael.efa.calendar.ICalendarExport;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeDataRecordTable;
import de.nmichael.efa.data.Clubwork;
import de.nmichael.efa.data.ClubworkRecord;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.util.International;

// @i18n complete
public class ClubworkListDialog extends DataListDialog {

  /**
   *
   */
  private static final long serialVersionUID = 1L;
  public static final int ACTION_CARRYOVER = 4;

  public ClubworkListDialog(Frame parent, AdminRecord admin) {
    super(parent, International.getString("Vereinsarbeit"), Daten.project.getCurrentClubwork(), 0,
        admin);
    iniValues();
  }

  public ClubworkListDialog(JDialog parent, AdminRecord admin) {
    super(parent, International.getString("Vereinsarbeit"), Daten.project.getCurrentClubwork(), 0,
        admin);
    iniValues();
  }

  private void iniValues() {
    super.sortByColumn = 2;
    super.sortAscending = false;
    super.filterFieldName = "Flag";
    super.filterFieldValue = "" + ClubworkRecord.Flags.Normal.ordinal();
  }

  @Override
  public void keyAction(ActionEvent evt) {
    _keyAction(evt);
  }

  @Override
  protected void iniActions() {
    if (admin == null) {
      actionText = new String[] {
          International.getString("Erfassen")
      };

      actionType = new int[] {
          ItemTypeDataRecordTable.ACTION_NEW
      };

      actionImage = new String[] {
          BaseDialog.IMAGE_ADD
      };
    }
    else {
      actionText = new String[] {
          ItemTypeDataRecordTable.ACTIONTEXT_NEW,
          ItemTypeDataRecordTable.ACTIONTEXT_EDIT,
          ItemTypeDataRecordTable.ACTIONTEXT_DELETE,
          International.getString("Liste ausgeben"),
          International.getString("Übertrag berechnen")
      };

      actionType = new int[] {
          ItemTypeDataRecordTable.ACTION_NEW,
          ItemTypeDataRecordTable.ACTION_EDIT,
          ItemTypeDataRecordTable.ACTION_DELETE,
          ACTION_PRINTLIST,
          ACTION_CARRYOVER
      };

      actionImage = new String[] {
          BaseDialog.IMAGE_ADD,
          BaseDialog.IMAGE_EDIT,
          BaseDialog.IMAGE_DELETE,
          BaseDialog.IMAGE_LIST,
          BaseDialog.IMAGE_MERGE
      };
    }
  }

  @Override
  protected void iniDialog() throws Exception {
    mainPanel.setLayout(new BorderLayout());

    JPanel mainTablePanel = new JPanel();
    mainTablePanel.setLayout(new BorderLayout());

    if (filterFieldDescription != null) {
      JLabel filterName = new JLabel();
      filterName.setText(filterFieldDescription);
      filterName.setHorizontalAlignment(SwingConstants.CENTER);
      mainTablePanel.add(filterName, BorderLayout.NORTH);
      mainTablePanel.setBorder(new EmptyBorder(10, 0, 0, 0));
    }

    table = new ClubworkItemTypeDataRecordTable("TABLE",
        persistence.createNewRecord().getGuiTableHeader(),
        persistence, validAt, admin,
        filterFieldName, filterFieldValue, // defaults are null
        actionText, actionType, actionImage, // default actions: new, edit, delete
        this,
        IItemType.TYPE_PUBLIC, "BASE_CAT", getTitle());
    table.setSorting(sortByColumn, sortAscending);
    table.setFontSize(tableFontSize);
    table.setMarkedCellColor(markedCellColor);
    table.setMarkedCellBold(markedCellBold);
    table.disableIntelligentColumnWidth(!intelligentColumnWidth);
    if (minColumnWidth > 0) {
      table.setMinColumnWidth(minColumnWidth);
    }
    if (minColumnWidths != null) {
      table.setMinColumnWidths(minColumnWidths);
    }
    table.setButtonPanelPosition(BorderLayout.SOUTH);
    table.setFieldSize(600, 500);
    table.setPadding(0, 0, 10, 0);
    table.displayOnGui(this, mainTablePanel, BorderLayout.CENTER);

    boolean hasEditAction = false;
    for (int i = 0; actionType != null && i < actionType.length; i++) {
      if (actionType[i] == ItemTypeDataRecordTable.ACTION_EDIT) {
        hasEditAction = true;
      }
    }
    if (!hasEditAction) {
      table.setDefaultActionForDoubleclick(-1);
    }

    super.iniControlPanel();
    mainPanel.add(mainTablePanel, BorderLayout.CENTER);

    setRequestFocus(table);
    this.validate();
  }

  @Override
  public DataEditDialog createNewDataEditDialog(JDialog parent, StorageObject persistence,
      DataRecord record) {
    boolean newRecord = (record == null);
    if (record == null) {
      record = Daten.project.getClubwork(Daten.project.getCurrentClubwork().getName(), false)
          .createClubworkRecord(UUID.randomUUID());
    }
    return new ClubworkEditDialog(parent, (ClubworkRecord) record, newRecord, admin);
  }

  @Override
  public void itemListenerActionTable(int actionId, DataRecord[] records) {
    if (actionId == ACTION_CARRYOVER) {
      Clubwork clubwork = Daten.project.getCurrentClubwork();
      clubwork.doCarryOver(this);
    }
    else {
      super.itemListenerActionTable(actionId, records);
    }
  }

  @Override
  public void closeButton_actionPerformed(ActionEvent e) {
    ICalendarExport cal = new ICalendarExport();
    cal.saveAllClubworkToCalendarFile();
    super.closeButton_actionPerformed(e);
  }
}
