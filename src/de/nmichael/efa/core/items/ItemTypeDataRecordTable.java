/**
 * Title: efa - elektronisches Fahrtenbuch für Ruderer Copyright: Copyright (c)
 * 2001-2011 by Nicolas Michael Website: http://efa.nmichael.de/ License: GNU
 * General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.core.items;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import de.nmichael.efa.Daten;
import de.nmichael.efa.calendar.CalendarString;
import de.nmichael.efa.calendar.CalendarTableModel;
import de.nmichael.efa.calendar.TblCalendarRenderer;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.data.BoatRecord;
import de.nmichael.efa.data.BoatReservationRecord;
import de.nmichael.efa.data.BoatReservations;
import de.nmichael.efa.data.Boats;
import de.nmichael.efa.data.ClubworkRecord;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataKeyIterator;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.ex.EfaModifyException;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.gui.MultiInputDialog;
import de.nmichael.efa.gui.dataedit.BoatReservationEditDialog;
import de.nmichael.efa.gui.dataedit.DataEditDialog;
import de.nmichael.efa.gui.dataedit.VersionizedDataDeleteDialog;
import de.nmichael.efa.gui.util.EfaMouseListener;
import de.nmichael.efa.gui.util.TableItem;
import de.nmichael.efa.gui.util.TableItemHeader;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

// @i18n complete
public class ItemTypeDataRecordTable extends ItemTypeTable implements IItemListener {

  public static final String CRLF = net.fortuna.ical4j.util.Strings.LINE_SEPARATOR; // "\r\n"
  public static final int ACTION_NEW = 0;
  public static final int ACTION_EDIT = 1;
  public static final int ACTION_DELETE = 2;
  public static final int ACTION_OTHER = -1;
  public static final String ACTIONTEXT_NEW = International.getString("Neu");
  public static final String ACTIONTEXT_EDIT = International.getString("Bearbeiten");
  public static final String ACTIONTEXT_DELETE = International.getString("Löschen");
  public static final String BUTTON_IMAGE_CENTERED_PREFIX = "%";
  private static final String[] DEFAULT_ACTIONS = new String[] {
    ACTIONTEXT_NEW,
    ACTIONTEXT_EDIT,
    ACTIONTEXT_DELETE
  };
  protected StorageObject persistence;
  protected long validAt = -1; // configured validAt
  protected long myValidAt = -1; // actually used validAt in updateData(); if validAt == -1, then
  // myValidAt is "now" each time the data is updated
  protected AdminRecord admin;
  protected boolean showAll = false;
  protected boolean showDeleted = false;
  protected String filterFieldName;
  protected String filterFieldValue;
  protected String buttonPanelPosition = BorderLayout.EAST;
  protected Vector<DataRecord> data;
  protected Hashtable<String, DataRecord> mappingKeyToRecord;
  protected Hashtable<DataTypeDate, String> mappingDateToReservations;
  protected IItemListenerDataRecordTable itemListenerActionTable;
  protected ItemTypeString searchField;
  protected String wochentagFilter;
  protected DataTypeDate selectedDateFilter;
  protected ItemTypeBoolean filterBySearch;
  protected JTable aggregationTable = null;
  protected JPanel myPanel;
  protected JPanel tablePanel;
  protected JPanel rightSidePanel;
  protected JPanel pnlCalendarPanel;
  protected JPanel buttonPanel;
  protected JPanel searchPanel;
  protected Hashtable<ItemTypeButton, String> actionButtons;
  protected static final String ACTION_BUTTON = "ACTION_BUTTON";
  protected String[] actionText;
  protected int[] actionTypes;
  protected String[] actionIcons;
  protected int defaultActionForDoubleclick = ACTION_EDIT;
  protected Color markedCellColor = Color.red;
  protected boolean markedCellBold = false;

  int realYear, realMonth, realDay, currentYear, currentMonth;
  JButton lblMonth;
  JButton btnPrev, btnNext;
  DefaultTableModel mtblCalendar; // Table model
  JTable tblCalendar;
  int xr320 = 400;
  int yu335 = 410;

  public ItemTypeDataRecordTable(String name,
      TableItemHeader[] tableHeader,
      StorageObject persistence,
      long validAt,
      AdminRecord admin,
      String filterFieldName, String filterFieldValue,
      String[] actions, int[] actionTypes, String[] actionIcons,
      IItemListenerDataRecordTable itemListenerActionTable,
      int type, String category, String description) {
    super(name, tableHeader, null, null, type, category, description);
    setData(persistence, validAt, admin, filterFieldName, filterFieldValue);
    setActions(actions, actionTypes, actionIcons);
    this.itemListenerActionTable = itemListenerActionTable;
    renderer = new de.nmichael.efa.gui.util.TableCellRenderer();
    renderer.setMarkedBold(false);
    renderer.setMarkedForegroundColor(markedCellColor);
    renderer.setMarkedBold(markedCellBold);
    renderer.setMarkedBackgroundColor(null);
  }

  protected void setData(StorageObject persistence, long validAt, AdminRecord admin,
      String filterFieldName, String filterFieldValue) {
    this.persistence = persistence;
    this.validAt = validAt;
    this.admin = admin;
    this.filterFieldName = filterFieldName;
    this.filterFieldValue = filterFieldValue;
  }

  public void setAndUpdateData(long validAt, boolean showAll, boolean showDeleted) {
    this.validAt = validAt;
    this.showAll = showAll;
    this.showDeleted = showDeleted;
    updateData();
  }

  public void setActions(String[] actions, int[] actionTypes, String[] actionIcons) {
    if (actions == null || actionTypes == null) {
      super.setPopupActions(DEFAULT_ACTIONS);
      this.actionText = DEFAULT_ACTIONS;
      this.actionTypes = new int[] { ACTION_NEW, ACTION_EDIT, ACTION_DELETE };
      this.actionIcons = new String[] {
          "button_add.png", "button_edit.png", "button_delete.png"
      };
    } else {
      int popupActionCnt = 0;
      for (int actionType : actionTypes) {
        if (actionType >= 0) {
          popupActionCnt++;
        } else {
          break; // first action with type < 0 (and all others after this) won't be shows as popup
          // actions
        }
      }
      String[] myPopupActions = new String[popupActionCnt];
      for (int i = 0; i < myPopupActions.length; i++) {
        if (actionTypes[i] >= 0) {
          myPopupActions[i] = actions[i];
        }
      }
      super.setPopupActions(myPopupActions);
      this.actionText = actions;
      this.actionTypes = actionTypes;
      if (actionIcons != null) {
        this.actionIcons = actionIcons;
      } else {
        this.actionIcons = new String[actionTypes.length];
        for (int i = 0; i < this.actionIcons.length; i++) {
          switch (actionTypes[i]) {
            case ACTION_NEW:
              this.actionIcons[i] = BaseDialog.IMAGE_ADD;
              break;
            case ACTION_EDIT:
              this.actionIcons[i] = BaseDialog.IMAGE_EDIT;
              break;
            case ACTION_DELETE:
              this.actionIcons[i] = BaseDialog.IMAGE_DELETE;
              break;
          }
        }
      }
    }
  }

  public void setDefaultActionForDoubleclick(int defaultAction) {
    this.defaultActionForDoubleclick = defaultAction;
  }

  protected void iniDisplayActionTable(Window dlg) {
    this.dlg = dlg;
    myPanel = new JPanel();
    myPanel.setLayout(new BorderLayout());
    tablePanel = new JPanel();
    tablePanel.setLayout(new GridBagLayout());
    buttonPanel = new JPanel();
    buttonPanel.setLayout(new GridBagLayout());
    buttonPanel.setAlignmentY(Component.TOP_ALIGNMENT);
    rightSidePanel = new JPanel(new BorderLayout());
    rightSidePanel.setBorder(new EmptyBorder(40, 0, 15, 10));
    // rightSidePanel.setAlignmentY(Component.TOP_ALIGNMENT);
    searchPanel = new JPanel();
    searchPanel.setLayout(new GridBagLayout());
    myPanel.add(tablePanel, BorderLayout.CENTER);
    myPanel.add(rightSidePanel, buttonPanelPosition);
    tablePanel.add(searchPanel, new GridBagConstraints(0, 10, 0, 0, 0.0, 0.0,
        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    actionButtons = new Hashtable<ItemTypeButton, String>();

    if (persistence.getName().equals("boatreservations")) {
      rightSidePanel.add(buttonPanel, BorderLayout.SOUTH);
      drawCalendar();
    } else {
      rightSidePanel.add(buttonPanel, BorderLayout.CENTER);
    }

    JPanel smallButtonPanel = null;
    for (int i = 0; actionText != null && i < actionText.length; i++) {
      if (actionTypes[i] >= 2000) {
        continue; // actions >= 2000 not shown as buttons
      }
      String action = ACTION_BUTTON + "_" + actionTypes[i];
      ItemTypeButton button = new ItemTypeButton(action, IItemType.TYPE_PUBLIC, "BUTTON_CAT",
          (actionTypes[i] < 1000 ? actionText[i] : null)); // >= 2000 just as small buttons without
      // text
      button.registerItemListener(this);
      if (actionTypes[i] < 1000) {
        button.setPadding(20, 20,
            (i > 0 && actionTypes[i] < 0 && actionTypes[i - 1] >= 0 ? 20 : 0), 5);
        button.setFieldSize(200, -1);
      } else {
        button.setPadding(5, 5, 5, 5);
        button.setFieldSize(50, -1);
        if (smallButtonPanel == null) {
          smallButtonPanel = new JPanel();
          smallButtonPanel.setLayout(new GridBagLayout());
          buttonPanel.add(smallButtonPanel, new GridBagConstraints(0, i, 1, 1, 0.0, 0.0,
              GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(20, 0, 20, 0),
              0, 0));
        }
      }
      if (actionIcons != null && i < actionIcons.length && actionIcons[i] != null) {
        String iconName = actionIcons[i];
        if (iconName.startsWith(BUTTON_IMAGE_CENTERED_PREFIX)) {
          iconName = iconName.substring(1);
        } else {
          button.setHorizontalAlignment(SwingConstants.LEFT);
        }
        if (iconName != null && iconName.length() > 0) {
          button.setIcon(BaseDialog.getIcon(iconName));
        }
      }
      if (actionTypes[i] < 1000) {
        button.displayOnGui(dlg, buttonPanel, 0, i);
      } else {
        button.displayOnGui(dlg, smallButtonPanel, i, 0);
      }
      actionButtons.put(button, action);
    }

    searchField = new ItemTypeString("SEARCH_FIELD", "", IItemType.TYPE_PUBLIC, "SEARCH_CAT",
        International.getString("Suche"));
    searchField.setFieldSize(300, -1);
    searchField.registerItemListener(this);
    searchField.displayOnGui(dlg, searchPanel, 0, 0);
    filterBySearch = new ItemTypeBoolean("FILTERBYSEARCH", true, IItemType.TYPE_PUBLIC,
        "SEARCH_CAT", International.getString("filtern"));
    filterBySearch.registerItemListener(this);
    filterBySearch.displayOnGui(dlg, searchPanel, 10, 0);
  }

  @Override
  public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
    iniDisplayActionTable(dlg);
    panel.add(myPanel, new GridBagConstraints(x, y, fieldGridWidth, fieldGridHeight, 0.0, 0.0,
        fieldGridAnchor, fieldGridFill, new Insets(padYbefore, padXbefore, padYafter, padXafter),
        0, 0));
    super.displayOnGui(dlg, tablePanel, 0, 0);
    return 1;
  }

  @Override
  public int displayOnGui(Window dlg, JPanel panel, String borderLayoutPosition) {
    iniDisplayActionTable(dlg);
    panel.add(myPanel, borderLayoutPosition);
    super.displayOnGui(dlg, tablePanel, 0, 0);
    return 1;
  }

  public void setVisibleButtonPanel(boolean visible) {
    rightSidePanel.setVisible(visible);
  }

  public void setVisibleSearchPanel(boolean visible) {
    searchPanel.setVisible(visible);
  }

  @Override
  public void showValue() {
    items = new Hashtable<String, TableItem[]>();
    mappingKeyToRecord = new Hashtable<String, DataRecord>();
    if (data == null && persistence != null) {
      updateData();
    }
    boolean isVersionized = persistence.data().getMetaData().isVersionized();
    for (int i = 0; data != null && i < data.size(); i++) {
      DataRecord r = data.get(i);
      TableItem[] content = r.getGuiTableItems();

      // mark deleted records
      if (r.getDeleted()) {
        for (TableItem it : content) {
          it.setMarked(true);
        }
      }

      // mark invalid and invisible records
      if (isVersionized && (!r.isValidAt(myValidAt) || r.getInvisible())) {
        for (TableItem it : content) {
          it.setDisabled(true);
        }
      }

      items.put(r.getKey().toString(), content);
      mappingKeyToRecord.put(r.getKey().toString(), r);
    }
    keys = items.keySet().toArray(new String[0]);
    Arrays.sort(keys);
    super.showValue();
  }

  @Override
  public void itemListenerAction(IItemType itemType, AWTEvent event) {
    if (event != null && event instanceof ActionEvent
        && event.getID() == ActionEvent.ACTION_PERFORMED
        && !(itemType instanceof ItemTypeBoolean)) {
      ActionEvent e = (ActionEvent) event;
      String cmd = e.getActionCommand();
      int actionId = -1;
      if (cmd != null && cmd.startsWith(EfaMouseListener.EVENT_POPUP_CLICKED)) {
        try {
          actionId = actionTypes[EfaUtil.stringFindInt(cmd, -1)];
        } catch (Exception eignore) {}
      }
      if (cmd != null && cmd.startsWith(EfaMouseListener.EVENT_MOUSECLICKED_2x)) {
        actionId = defaultActionForDoubleclick;
      }
      if (itemType != null && itemType instanceof ItemTypeButton) {
        actionId = EfaUtil.stringFindInt(actionButtons.get(itemType), -1);
      }
      if (actionId == -1) {
        return;
      }
      int[] rows = table.getSelectedRows();
      DataRecord[] records = null;
      if (rows != null && rows.length > 0) {
        records = new DataRecord[rows.length];
        for (int i = 0; i < rows.length; i++) {
          records[i] = mappingKeyToRecord.get(keys[rows[i]]);
        }
      }
      if (!Daten.isWriteModeMitSchluessel()) {
        Dialog.meldung("Nur für Vereinsmitglieder",
            "Bitte erst Bootshausschlüssel nach rechts drehen!");
      } else if (persistence != null && itemListenerActionTable != null) {
        DataEditDialog dlg;
        switch (actionId) {
          case ACTION_NEW:
            dlg = itemListenerActionTable.createNewDataEditDialog(getParentDialog(), persistence,
                null);
            if (dlg == null) {
              return;
            }
            dlg.showDialog();
            if (admin != null && dlg instanceof BoatReservationEditDialog) {
              try {
                uebertragenAufAndereBoote(((BoatReservationEditDialog) dlg).getDataRecord());
              } catch (EfaException e1) {
                Logger.logdebug(e1);
              }
            }
            if (dlg instanceof BoatReservationEditDialog) {
              BoatReservationRecord reservation = ((BoatReservationEditDialog) dlg).getDataRecord();
              if (reservation.isBootshausOH()) {
                sendEmail("wolle", "INSERT", reservation);
              }
            }
            break;
          case ACTION_EDIT:
            for (int i = 0; records != null && i < records.length; i++) {
              if (records[i] != null) {
                if (records[i].getDeleted()) {
                  switch (Dialog
                      .yesNoCancelDialog(
                          International.getString("Datensatz wiederherstellen"),
                          International
                              .getMessage(
                                  "Der Datensatz '{record}' wurde gelöscht. Möchtest Du ihn wiederherstellen?",
                                  records[i].getQualifiedName()))) {
                    case Dialog.YES:
                      try {
                        DataRecord[] rall = persistence.data().getValidAny(records[i].getKey());
                        for (int j = 0; rall != null && j < rall.length; j++) {
                          rall[j].setDeleted(false);
                          persistence.data().update(rall[j]);
                        }
                      } catch (Exception exr) {
                        Dialog.error(exr.toString());
                        return;
                      }
                      break;
                    case Dialog.NO:
                      continue;
                    case Dialog.CANCEL:
                      return;
                  }
                }
                dlg = itemListenerActionTable.createNewDataEditDialog(getParentDialog(),
                    persistence, records[i]);
                if (dlg == null) {
                  return;
                }
                dlg.showDialog();
                if (!dlg.getDialogResult()) {
                  break;
                }
                if (dlg instanceof BoatReservationEditDialog) {
                  BoatReservationRecord reservation = ((BoatReservationEditDialog) dlg)
                      .getDataRecord();
                  if (reservation.isBootshausOH()) {
                    sendEmail("wolle", "UPDATE", reservation);
                  }
                }
              }
            }
            break;
          case ACTION_DELETE:
            if (records == null || records.length == 0) {
              return;
            }
            // löschen alter Termine verhindern
            if (itemListenerActionTable != null) {
              if (!itemListenerActionTable.deleteCallback(records)) {
                updateData();
                showValue();
                return;
              }
            }
            int res = -1;
            if (records.length == 1) {
              res = Dialog.yesNoDialog(International.getString("Wirklich löschen?"),
                  International.getMessage(
                      "Möchtest Du den Datensatz '{record}' wirklich löschen?",
                      records[0].getQualifiedName()));
            } else {
              res = Dialog.yesNoDialog(International.getString("Wirklich löschen?"),
                  International.getMessage(
                      "Möchtest Du {count} ausgewählte Datensätze wirklich löschen?",
                      records.length));
            }
            if (res != Dialog.YES) {
              return;
            }
            long deleteAt = Long.MAX_VALUE;
            if (persistence.data().getMetaData().isVersionized()) {
              VersionizedDataDeleteDialog ddlg =
                  new VersionizedDataDeleteDialog(getParentDialog(),
                      (records.length == 1 ? records[0].getQualifiedName()
                          : International.getMessage("{count} Datensätze", records.length)));
              ddlg.showDialog();
              deleteAt = ddlg.getDeleteAtResult();
              if (deleteAt == Long.MAX_VALUE || deleteAt < -1) {
                return;
              }
            }
            try {
              for (int i = 0; records != null && i < records.length; i++) {
                if (records[i] != null) {
                  if (persistence.data().getMetaData().isVersionized()) {
                    if (records[i] instanceof BoatReservationRecord) {
                      BoatReservationRecord reservation = (BoatReservationRecord) records[i];
                      if (reservation.isBootshausOH()) {
                        sendEmail("wolle", "DELETE", reservation);
                      }
                    }
                    persistence.data().deleteVersionizedAll(records[i].getKey(), deleteAt);
                    if (deleteAt >= 0) {
                      Logger.log(
                          Logger.INFO,
                          Logger.MSG_DATAADM_RECORDDELETEDAT,
                          records[i].getPersistence().getDescription()
                              + ": "
                              + International.getMessage(
                                  "{name} hat Datensatz '{record}' ab {date} gelöscht.",
                                  (admin != null ? International.getString("Admin") + " '"
                                      + admin.getName() + "'"
                                      : International.getString("Normaler Benutzer")),
                                  records[i].getQualifiedName(),
                                  EfaUtil.getTimeStampDDMMYYYY(deleteAt)));
                    } else {
                      Logger
                          .log(
                              Logger.INFO,
                              Logger.MSG_DATAADM_RECORDDELETED,
                              records[i].getPersistence().getDescription()
                                  + ": "
                                  + International
                                      .getMessage(
                                          "{name} hat Datensatz '{record}' zur vollständigen Löschung markiert.",
                                          (admin != null ? International.getString("Admin") + " '"
                                              + admin.getName() + "'"
                                              : International.getString("Normaler Benutzer")),
                                          records[i].getQualifiedName()));
                    }
                  } else {
                    if (records[i] instanceof BoatReservationRecord) {
                      BoatReservationRecord reservation = (BoatReservationRecord) records[i];
                      if (reservation.isBootshausOH()) {
                        sendEmail("wolle", "DELETE", reservation);
                      }
                    }
                    persistence.data().delete(records[i].getKey());
                    Logger.log(
                        Logger.INFO,
                        Logger.MSG_DATAADM_RECORDDELETED,
                        records[i].getPersistence().getDescription()
                            + ": "
                            + International.getMessage(
                                "{name} hat Datensatz '{record}' gelöscht.",
                                (admin != null ? International.getString("Admin") + " '"
                                    + admin.getName() + "'"
                                    : International.getString("Normaler Benutzer")),
                                records[i].getQualifiedName()));
                  }
                }
              }
            } catch (EfaModifyException exmodify) {
              exmodify.displayMessage();
            } catch (Exception ex) {
              Logger.logdebug(ex);
              Dialog.error(ex.toString());
            }
            break;
        }
      }
      if (itemListenerActionTable != null) {
        itemListenerActionTable.itemListenerActionTable(actionId, records);
      }
      updateData();
      showValue();
    }
    if (event != null && event instanceof KeyEvent && event.getID() == KeyEvent.KEY_RELEASED
        && itemType == searchField) {
      String s = searchField.getValueFromField();
      if (s != null && s.length() > 0 && keys != null && items != null) {
        s = s.toLowerCase();
        Vector<String> sv = null;
        boolean[] sb = null;
        if (s.indexOf(" ") > 0) {
          sv = EfaUtil.split(s, ' ');
          if (sv != null && sv.size() == 0) {
            sv = null;
          } else {
            sb = new boolean[sv.size()];
          }
        }
        int rowFound = -1;
        for (int i = 0; rowFound < 0 && i < keys.length; i++) {
          // search in row i
          for (int j = 0; sb != null && j < sb.length; j++) {
            sb[j] = false; // matched parts of substring
          }

          TableItem[] row = items.get(keys[i]);
          for (int j = 0; row != null && rowFound < 0 && j < row.length; j++) {
            // search in row i, column j
            String t = (row[j] != null ? row[j].toString() : null);
            t = (t != null ? t.toLowerCase() : null);
            if (t == null) {
              continue;
            }

            // match entire search string against column
            if (t.indexOf(s) >= 0) {
              rowFound = i;
            }

            if (sv != null && rowFound < 0) {
              // match column against substrings
              for (int k = 0; k < sv.size(); k++) {
                if (t.indexOf(sv.get(k)) >= 0) {
                  sb[k] = true;
                }
              }
            }
          }
          if (sb != null && rowFound < 0) {
            rowFound = i;
            for (int j = 0; j < sb.length; j++) {
              if (!sb[j]) {
                rowFound = -1;
              }
            }
          }
        }
        if (rowFound >= 0) {
          int currentIdx = table.getCurrentRowIndex(rowFound);
          if (currentIdx >= 0) {
            scrollToRow(currentIdx);
          }
        }
      }
    }
    if (event != null
        && (event instanceof KeyEvent && event.getID() == KeyEvent.KEY_RELEASED && itemType == searchField)
        || (event instanceof ActionEvent && event.getID() == ActionEvent.ACTION_PERFORMED && itemType == filterBySearch)) {
      updateFilter();
    }
  }

  /**
   * Bitte diese Reservierung übertragen auf alle Boote dieser Gruppen
   *
   * @param dataRecord
   * @throws EfaException
   */
  private void uebertragenAufAndereBoote(BoatReservationRecord dataRecord) throws EfaException {
    String[] boatSeatsValuesArray = EfaTypes
        .makeBoatSeatsArray(EfaTypes.ARRAY_STRINGLIST_VALUES);
    String[] boatSeatsDisplayArray = EfaTypes
        .makeBoatSeatsArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY);

    int header = 2;
    IItemType[] items = new IItemType[header + boatSeatsValuesArray.length];
    BoatRecord originalBoat = dataRecord.getBoat();
    String originalTypeSeats = originalBoat.getTypeSeats(0);

    String originalBoatType = "";
    for (int i = 0; i < boatSeatsValuesArray.length; i++) {
      boolean checkMe = originalTypeSeats.equals(boatSeatsValuesArray[i]);
      items[header + i] = new ItemTypeBoolean(boatSeatsValuesArray[i], checkMe,
          IItemType.TYPE_INTERNAL, "", International.getString(boatSeatsDisplayArray[i]));
      if (checkMe) {
        originalBoatType = boatSeatsDisplayArray[i];
      }
    }
    items[0] = new ItemTypeLabel("L0", IItemType.TYPE_INTERNAL, "",
        "Sollen andere Boote genauso reserviert werden?");
    items[1] = new ItemTypeLabel("L1", IItemType.TYPE_INTERNAL, "",
        "Welche Bootstypen? (" + originalBoat.getName() + " = " + originalBoatType + ")");
    boolean success = MultiInputDialog.showInputDialog(getParentDialog(),
        International.getString("Übertragen auf ganze Gruppen"), items);
    if (success) {

      // join items
      Map<Integer, String> mapBootsTypen = new HashMap<Integer, String>();
      for (int i = 0; i < boatSeatsValuesArray.length; i++) {
        boolean selectedGui = ((ItemTypeBoolean) items[header + i]).getValue();
        if (selectedGui) {
          mapBootsTypen.put(i, boatSeatsValuesArray[i]);
        }
      }

      // persistence.
      BoatReservations reservations = Daten.project.getBoatReservations(true);
      // reservations = persistence.data().getAllKeys();
      // reservations = (BoatReservations) dataRecord.getPersistence(); // TODO

      // boats.
      Boats boats = Daten.project.getBoats(false);
      IDataAccess data2 = boats.data();
      // for-schleife
      for (DataKey<?, ?, ?> dataKey : data2.getAllKeys()) {
        BoatRecord boatRecord = (BoatRecord) data2.get(dataKey);
        if (originalBoat.getId().equals(boatRecord.getId())) {
          // aber nicht das eine Boot selber
          // dieses Boot haben wir schon reserviert
          continue;
        }

        String typeSeats = boatRecord.getTypeSeats(0);
        // get alle Boote von dieser Kategorie (Seats)
        if (mapBootsTypen.containsValue(typeSeats)) {
          // dieser typeSeat war gewünscht

          // alle parameter einzeln eintragen
          BoatReservationRecord newReservationsRecord = reservations
              .createBoatReservationsRecordFromClone(boatRecord.getId(), dataRecord);

          // check for conflicts TODO
          // newReservationsRecord.checkValidValues();
          if (!versionizedRecordOfThatNameAlreadyExists(newReservationsRecord)) {

            // newReservationsRecord.saveRecord();
            reservations.data().add(newReservationsRecord);
            Logger.log(
                Logger.INFO,
                Logger.MSG_DATAADM_RECORDADDED,
                newReservationsRecord.getPersistence().getDescription()
                    + ": "
                    +
                    International.getMessage("{name} hat neuen Datensatz '{record}' erstellt.",
                        (admin != null ? International.getString("Admin") + " '" + admin.getName()
                            + "'" :
                            International.getString("Normaler Benutzer")),
                        newReservationsRecord.getQualifiedName()));
          }
        }
      }
    }
  }

  private void sendEmail(String empfaenger, String aktion, BoatReservationRecord brr) {
    String emailAdresse = empfaenger.trim().toLowerCase() + "@abfx.de";
    String emailSubject = "OH Reservierung " + aktion;
    String emailMessage = "Hallo " + capitalize(empfaenger) + "!" + "\n\n"
        + brr.getFormattedEmailtext();
    ;

        Daten.project.getMessages(false).createAndSaveMessageRecord(
            emailAdresse, emailSubject, emailMessage);
  }

  private String capitalize(final String line) {
    return Character.toUpperCase(line.charAt(0)) + line.substring(1).toLowerCase();
  }

  private boolean versionizedRecordOfThatNameAlreadyExists(BoatReservationRecord dataRecord) {
    boolean allowConflicts = true; // TODO
    String conflict = null;
    boolean identical = true;
    try {
      if (!dataRecord.getPersistence().data().getMetaData().isVersionized()) {
        return false;
      }
      DataKey<?, ?, ?>[] keys = dataRecord.getPersistence().data()
          .getByFields(dataRecord.getQualifiedNameFields(),
              dataRecord.getQualifiedNameValues(dataRecord.getQualifiedName()));
      for (int i = 0; keys != null && i < keys.length; i++) {
        DataRecord r = dataRecord.getPersistence().data().get(keys[i]);
        if (!r.getDeleted()) {
          conflict = r.getQualifiedName() + " (" + r.getValidRangeString() + ")";
        }
      }
      if (conflict == null) {
        // TODO conflict = findSimilarRecordsOfThisName();
        identical = false;
      }
    } catch (Exception e) {
      Logger.logdebug(e);
    }
    if (conflict != null) {
      if (allowConflicts) {
        String warn = (identical ?
            International.getString("Es existiert bereits ein gleichnamiger Datensatz!") :
              International.getString("Es existiert bereits ein ähnlicher Datensatz!"));
        if (Dialog.yesNoDialog(International.getString("Warnung"),
            warn + "\n"
                + conflict + "\n"
                + International.getString("Möchtest Du diesen Datensatz trotzdem erstellen?")) != Dialog.YES) {
          return true;
        }
      } else {
        Dialog.error(International.getString("Es existiert bereits ein gleichnamiger Datensatz!"));
        return true;
      }
    }
    return false;
  }

  private void updateFilterWithDate(int day) {
    DataTypeDate date = new DataTypeDate(day, currentMonth + 1, currentYear);
    searchField.setValue(date.toString().replace('/', '.'));
    if (day != 0) {
      wochentagFilter = date.getWeekdayAsString().toLowerCase(); // "donnerstag";
      selectedDateFilter = date;
    } else {
      selectedDateFilter = null;
    }
    updateFilter();
  }

  public DataTypeDate getSelectedDateFilter() {
    return selectedDateFilter;
  }

  protected void updateFilter() {
    searchField.getValueFromGui();
    filterBySearch.getValueFromGui();
    if (filterBySearch.isChanged() || (filterBySearch.getValue() && searchField.isChanged())) {
      updateDataMitRightSideRefresh(false);
      updateAggregations(filterBySearch.getValue());
      showValue();
    }
    filterBySearch.setUnchanged();
    searchField.setUnchanged();
    wochentagFilter = null;
  }

  protected void updateAggregations(boolean create) {
    if (aggregationTable != null) {
      tablePanel.remove(aggregationTable);
    }

    if (create && data != null) {
      int size = data.size();
      if (size > 0) {
        String[] aggregationStrings = new String[header.length];
        for (int i = 0; i < header.length; i++) {
          aggregationStrings[i] = "";
        }

        HashMap<String, Object> overallInfo = new HashMap<String, Object>();
        for (int i = 0; i < size && aggregationStrings != null; i++) {
          DataRecord r = data.get(i);
          aggregationStrings = r.getGuiTableAggregations(aggregationStrings, i, size, overallInfo);
        }

        if (aggregationStrings != null) {
          int length = 0;
          for (int i = 0; i < header.length; i++) {
            if (!aggregationStrings[i].equals("")) {
              length++;
            }
          }

          // create table
          TableModel dataModel = new DefaultTableModel(1, length);
          for (int i = 0, j = 0; i < header.length; i++) {
            if (!aggregationStrings[i].equals("")) {
              dataModel.setValueAt(aggregationStrings[i], 0, j++);
            }
          }
          aggregationTable = new JTable(dataModel);

          tablePanel.add(aggregationTable,
              new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                  GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1),
                  0, 0));
          tablePanel.setComponentZOrder(aggregationTable, 0);
        }
      }
    }

    tablePanel.revalidate();
    tablePanel.repaint();
  }

  protected void updateData() {
    updateDataMitRightSideRefresh(true);
  }

  private void updateDataMitRightSideRefresh(boolean updateDataRightSideCalendar) {
    if (persistence == null) {
      return;
    }
    try {
      String filterByAnyText = null;
      if (filterBySearch != null && searchField != null) {
        filterBySearch.getValueFromField();
        searchField.getValueFromGui();
        if (filterBySearch.getValue() && searchField.getValue() != null
            && searchField.getValue().length() > 0) {
          filterByAnyText = searchField.getValue().toLowerCase();
        }
      }
      myValidAt = (validAt >= 0 ? validAt : System.currentTimeMillis());
      data = new Vector<DataRecord>();
      IDataAccess dataAccess = persistence.data();
      boolean isVersionized = dataAccess.getMetaData().isVersionized();
      DataKeyIterator it = dataAccess.getStaticIterator();
      DataKey<?, ?, ?> key = it.getFirst();
      Hashtable<DataKey<?, ?, ?>, String> uniqueHash = new Hashtable<DataKey<?, ?, ?>, String>();
      if (updateDataRightSideCalendar) {
        mappingDateToReservations = new Hashtable<DataTypeDate, String>();
      }
      while (key != null) {
        // avoid duplicate versionized keys for the same record
        if (isVersionized) {
          DataKey<?, ?, ?> ukey = dataAccess.getUnversionizedKey(key);
          if (uniqueHash.get(ukey) != null) {
            key = it.getNext();
            continue;
          }
          uniqueHash.put(ukey, "");
        }

        DataRecord r;
        if (isVersionized) {
          r = dataAccess.getValidAt(key, myValidAt);
          if (r == null && showAll) {
            r = dataAccess.getValidLatest(key);
          }
        } else {
          r = dataAccess.get(key);
          if (!showAll && !r.isValidAt(myValidAt)) {
            r = null;
          }
        }
        if (r == null && showDeleted) {
          DataRecord[] any = dataAccess.getValidAny(key);
          if (any != null && any.length > 0 && any[0].getDeleted()) {
            r = any[0];
          }
        }
        if (r != null && (!r.getDeleted() || showDeleted)) {
          if (filterFieldName == null || filterFieldValue == null
              || filterFieldValue.equals(r.getAsString(filterFieldName))) {
            String allFieldsAsLowerText = r.getAllFieldsAsSeparatedText().toLowerCase();
            if (filterByAnyText == null
                || (filterByAnyText != null && allFieldsAsLowerText.indexOf(filterByAnyText) >= 0)
                || (wochentagFilter != null && allFieldsAsLowerText.indexOf(wochentagFilter) >= 0)) {
              if (!(r instanceof ClubworkRecord) || Daten.isAdminMode()
                  || isToday(r.getLastModified())) {
                data.add(r);
              }
            }
          }
          if (updateDataRightSideCalendar) {
            mappingDateToName(r);
          }
        }
        key = it.getNext();
      }
      filterBySearch.setDescription("gefiltert " + data.size());
    } catch (Exception e) {
      Logger.logdebug(e);
    }
  }

  private boolean isToday(long timeInMillis) {
    long todayInMillis = DataTypeDate.today().toCalendar().getTimeInMillis();
    return timeInMillis > todayInMillis;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    itemListenerAction(this, e);
  }

  public void setButtonPanelPosition(String borderLayoutPosition) {
    this.buttonPanelPosition = borderLayoutPosition;
  }

  public void setMarkedCellColor(Color color) {
    this.markedCellColor = color;
    if (renderer != null) {
      renderer.setMarkedForegroundColor(color);
    }
  }

  public void setMarkedCellBold(boolean bold) {
    this.markedCellBold = bold;
    if (renderer != null) {
      renderer.setMarkedBold(bold);
    }
  }

  public Vector<DataRecord> getDisplayedData() {
    Vector<DataRecord> sortedData = new Vector<DataRecord>();
    for (int i = 0; i < data.size(); i++) {
      sortedData.add(mappingKeyToRecord.get(keys[table.getOriginalIndex(i)]));
    }
    return sortedData;
  }

  public Vector<DataRecord> getSelectedData() {
    String[] keys = getSelectedKeys();
    Vector<DataRecord> selectedData = new Vector<DataRecord>();
    for (int i = 0; keys != null && i < keys.length; i++) {
      selectedData.add(mappingKeyToRecord.get(keys[i]));
    }
    return selectedData;
  }

  public boolean isFilterSet() {
    filterBySearch.getValueFromGui();
    return filterBySearch.getValue();
  }

  private void drawCalendar() {
    // Container pane;

    lblMonth = new JButton("heute");
    btnPrev = new JButton("<<");
    btnNext = new JButton(">>");
    mtblCalendar = new CalendarTableModel();
    tblCalendar = new JTable(mtblCalendar);
    JScrollPane stblCalendar = new JScrollPane(tblCalendar);
    // JPanel stblCalendarNeu = new JPanel();
    // stblCalendarNeu.add(tblCalendar);

    // Register action listeners
    btnPrev.addActionListener(new btnPrev_Action());
    btnNext.addActionListener(new btnNext_Action());
    lblMonth.addActionListener(new lblMonth_Action());

    pnlCalendarPanel = new JPanel(new BorderLayout()); // BorderLayout
    // pnlCalendarPanel.setSize(xr320 + 10, yu335 + 40); // Set size to 400x400 pixels
    pnlCalendarPanel.add(btnPrev);
    pnlCalendarPanel.add(lblMonth);
    pnlCalendarPanel.add(btnNext);
    pnlCalendarPanel.add(stblCalendar);

    // Set bounds
    // pnlCalendarPanel.setBounds(0, 0, xr320, yu335);
    btnPrev.setBounds(50, yu335 - 25, 70, 25);
    lblMonth.setBounds((xr320 - 170) / 2, yu335 - 25, 220, 25);
    btnNext.setBounds(xr320 - 70, yu335 - 25, 70, 25);
    // stblCalendar.setBounds(10, 50, xr320 - 20, yu335 - 85);
    // tblCalendar.setBounds(10, 50, xr320 - 10, yu335 - 85);

    rightSidePanel.add(pnlCalendarPanel, BorderLayout.NORTH);

    // Add headers
    String[] headers = { "Mon", "Die", "Mit", "Don", "Fre", "Sam", "Son" }; // All headers
    for (int i = 0; i < 7; i++) {
      mtblCalendar.addColumn(headers[i]);
    }

    // tblCalendar.getParent().setBackground(tblCalendar.getBackground()); // Set background

    // No resize/reorder
    tblCalendar.getTableHeader().setResizingAllowed(false);
    tblCalendar.getTableHeader().setReorderingAllowed(false);

    // Single cell selection
    tblCalendar.setColumnSelectionAllowed(true);
    tblCalendar.setRowSelectionAllowed(true);
    tblCalendar.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    tblCalendar.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        int day = getDay(e.getPoint());
        refreshCalendar(day, currentMonth, currentYear);
        updateFilterWithDate(day); // after clicking
        repaintCalendarButtons();
      }
    });

    // Set row/column count
    tblCalendar.setRowHeight(58);
    mtblCalendar.setColumnCount(7);
    mtblCalendar.setRowCount(6);

    // Get real month/year
    GregorianCalendar cal = new GregorianCalendar(); // Create calendar
    realDay = cal.get(GregorianCalendar.DAY_OF_MONTH); // Get day
    realMonth = cal.get(GregorianCalendar.MONTH); // Get month
    realYear = cal.get(GregorianCalendar.YEAR); // Get year
    currentMonth = realMonth; // Match month and year
    currentYear = realYear;

    // Refresh calendar
    refreshCalendar(0, realMonth, realYear); // Refresh calendar
    // don't filter at startup
  }

  private void repaintCalendarButtons() {
    btnPrev.repaint();
    lblMonth.repaint();
    btnNext.repaint();
  }

  protected int getDay(Point point) {
    int row = tblCalendar.rowAtPoint(point);
    int col = tblCalendar.columnAtPoint(point);

    Object valueAt = mtblCalendar.getValueAt(row, col);
    if (valueAt instanceof CalendarString) {
      CalendarString value = (CalendarString) valueAt;
      return value.getDay();
    }
    return 0;
  }

  public void refreshCalendar(int day, int month, int year) {
    // Variables
    String[] months = { "Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August",
        "September", "Oktober", "November", "Dezember" };

    // Allow/disallow buttons
    btnPrev.setEnabled(true);
    btnNext.setEnabled(true);
    if (month == 0 && year <= realYear - 10) {
      btnPrev.setEnabled(false);
    } // Too early
    if (month == 11 && year >= realYear + 10) {
      btnNext.setEnabled(false);
    } // Too late
    lblMonth.setText(months[month] + " " + year); // Refresh the month label (at the top)
    // Re-align label with calendar
    // lblMonth.setBounds((xr320 + 40 - lblMonth.getPreferredSize().width) / 2, yu335 - 25, 190,
    // 25);

    // Clear table
    for (int i = 0; i < 6; i++) {
      for (int j = 0; j < 7; j++) {
        mtblCalendar.setValueAt(null, i, j);
      }
    }

    // Get first day of month and number of days
    GregorianCalendar cal = new GregorianCalendar(year, month, 1);
    // cal.setFirstDayOfWeek(GregorianCalendar.MONDAY);
    int numberOfDays = cal.getActualMaximum(GregorianCalendar.DAY_OF_MONTH);
    int startOfMonth = cal.get(GregorianCalendar.DAY_OF_WEEK);
    if (startOfMonth < 2) {
      startOfMonth += 7;
    }

    // Draw calendar
    for (int i = 1; i <= numberOfDays; i++) {
      int aktuell = i + startOfMonth - 3;
      int row = new Integer(aktuell / 7);
      int column = aktuell % 7;
      String buchungStand = getBuchungString(new DataTypeDate(i, month + 1, year));
      CalendarString aValue = new CalendarString(i, buchungStand);
      mtblCalendar.setValueAt(aValue, row, column);
    }

    // Apply renderer
    TblCalendarRenderer tblCalendarRenderer = new TblCalendarRenderer(realYear, realMonth, realDay,
        currentYear, currentMonth, day);
    tblCalendar.setDefaultRenderer(tblCalendar.getColumnClass(0), tblCalendarRenderer);
  }

  public String getBuchungString(DataTypeDate date) {
    if (mappingDateToReservations == null) {
      // updateData();
      if (mappingDateToReservations == null) {
        return "";
      }
    }
    String myValue = mappingDateToReservations.get(date);
    if (myValue == null) {
      return "";
    }
    return myValue;
  }

  class btnPrev_Action implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      if (currentMonth == 0) { // Back one year
        currentMonth = 11;
        currentYear -= 1;
      }
      else { // Back one month
        currentMonth -= 1;
      }
      refreshCalendar(0, currentMonth, currentYear);
      updateFilterWithDate(0); // button Previous
      repaintCalendarButtons();
    }
  }

  class btnNext_Action implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      if (currentMonth == 11) { // Foward one year
        currentMonth = 0;
        currentYear += 1;
      }
      else { // Foward one month
        currentMonth += 1;
      }
      refreshCalendar(0, currentMonth, currentYear);
      updateFilterWithDate(0); // button Next
      repaintCalendarButtons();
    }
  }

  class lblMonth_Action implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      currentMonth = realMonth;
      currentYear = realYear;
      refreshCalendar(0, currentMonth, currentYear);
      updateFilterWithDate(0); // button Month
      repaintCalendarButtons();
    }
  }

  private void mappingDateToName(DataRecord r) {
    if (r instanceof BoatReservationRecord) {
      BoatReservationRecord brr = (BoatReservationRecord) r;
      boolean isBootshausReservierung = brr.isBootshausOH();
      String bootshausKuerzel = "BH";
      String trennzeichen = "'";
      List<DataTypeDate> dates = getListOfDates(brr.getDateFrom(), brr.getDateTo(),
          brr.getDayOfWeek());
      for (DataTypeDate dataTypeDate : dates) {
        // alten Wert auslesen
        String myValue = mappingDateToReservations.get(dataTypeDate);
        if (myValue == null) {
          myValue = "";
        }
        boolean containsBootshaus = false;
        if (myValue.contains(bootshausKuerzel)) {
          containsBootshaus = true;
          myValue = myValue.replace(bootshausKuerzel, "");
        }
        myValue = myValue.replace(trennzeichen, "");
        int myCount = Integer.parseInt("0" + myValue);

        if (isBootshausReservierung) {
          containsBootshaus = true;
        } else {
          myCount++;
        }
        myValue = "";
        if (containsBootshaus) {
          myValue += bootshausKuerzel;
        }
        if (myCount > 0) {
          myValue += trennzeichen + myCount;
        }
        mappingDateToReservations.put(dataTypeDate, myValue);
      }
    }
  }

  private List<DataTypeDate> getListOfDates(DataTypeDate dateFrom, DataTypeDate dateTo,
      String dayOfWeek) {
    DataTypeDate myDateFrom = dateFrom;
    DataTypeDate myDateTo = dateTo;

    int wochentag = -1;
    if (dayOfWeek != null) {
      wochentag = getWochentag(dayOfWeek);
      myDateFrom = DataTypeDate.today();
      int fromWochentag = myDateFrom.toCalendar().get(Calendar.DAY_OF_WEEK);
      myDateFrom.addDays(wochentag - fromWochentag);
      myDateTo = new DataTypeDate(myDateFrom);
      myDateTo.addDays(365);
    }

    List<DataTypeDate> datumListe = new ArrayList<DataTypeDate>();
    DataTypeDate myDate = new DataTypeDate(myDateFrom);
    while (myDate.isBeforeOrEqual(myDateTo)) {
      datumListe.add(new DataTypeDate(myDate));
      if (wochentag == myDate.toCalendar().get(Calendar.DAY_OF_WEEK)) {
        myDate.addDays(7);
      } else {
        myDate.addDays(1);
      }
    }
    return datumListe;
  }

  private int getWochentag(String dayName) {
    SimpleDateFormat dayFormat = new SimpleDateFormat("E", Locale.US);
    Date date;
    try {
      date = dayFormat.parse(dayName);
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return 0;
    }
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
    return dayOfWeek;

  }

}
