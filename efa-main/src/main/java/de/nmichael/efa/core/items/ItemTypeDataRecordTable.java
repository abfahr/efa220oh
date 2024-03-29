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
import java.awt.event.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import de.nmichael.efa.Daten;
import de.nmichael.efa.calendar.CalendarString;
import de.nmichael.efa.calendar.CalendarTableModel;
import de.nmichael.efa.calendar.TblCalendarRenderer;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.data.*;
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
import de.nmichael.efa.gui.ReserveAdditionalsDialog;
import de.nmichael.efa.gui.dataedit.BoatReservationEditDialog;
import de.nmichael.efa.gui.dataedit.BoatReservationListDialog;
import de.nmichael.efa.gui.dataedit.DataEditDialog;
import de.nmichael.efa.gui.dataedit.DataListDialog;
import de.nmichael.efa.gui.dataedit.StatisticsListDialog;
import de.nmichael.efa.gui.dataedit.VersionizedDataDeleteDialog;
import de.nmichael.efa.gui.util.EfaMouseListener;
import de.nmichael.efa.gui.util.TableCellRenderer;
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
  public static final String BOAT_SELECT_BTN = "boat_select_btn";
  public static final String BOAT_ALL_SELECT_BTN = "boat_select_all_btn";

  public static final String BOAT_REMOVE_BTN = "boat_remove_btn";
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
  protected Hashtable<String, DataRecord> mappingKeyToRecord = new Hashtable<>();
  protected Hashtable<DataTypeDate, Integer> mappingDateToReservations = new Hashtable<>();
  protected Hashtable<DataTypeDate, String> mappingBootshausDateToReservations = new Hashtable<>();
  protected Hashtable<Integer, String> mappingWeekdayToReservations = new Hashtable<>();
  protected Hashtable<Integer, DataTypeDate> mappingMinWeekdayToReservations = new Hashtable<>();
  protected Hashtable<Integer, DataTypeDate> mappingMaxWeekdayToReservations = new Hashtable<>();
  protected IItemListenerDataRecordTable itemListenerActionTable;
  protected ItemTypeString searchField;
  protected String wochentagFilter;
  protected DataTypeDate selectedDateFilter;
  protected ItemTypeBoolean filterBySearch;
  protected JTable aggregationTable = null;
  protected JPanel myPanel;
  protected JPanel centerTableListPanel;
  protected JPanel northSideCalenderPanel;
  protected JPanel rightSouthSideButtonPanel;
  protected JPanel pnlCalendarPanel;
  protected JPanel searchPanel;
  protected Hashtable<ItemTypeButton, String> actionButtons;
  protected static final String ACTION_BUTTON = "ACTION_BUTTON";
  protected String[] actionText;
  protected int[] actionTypes;
  protected String[] actionIcons;
  protected int defaultActionForDoubleclick = ACTION_EDIT;
  protected Color markedCellColor = Color.red;
  protected boolean markedCellBold = false;

  int realYear, realMonth, realDay;
  int currentYear, currentMonth;
  JButton lblMonth;
  JButton btnPrev, btnNext;
  DefaultTableModel mtblCalendar; // Table model
  JTable tblCalendar;

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
    renderer = new TableCellRenderer();
    renderer.setMarkedBold(false);
    renderer.setMarkedForegroundColor(markedCellColor);
    renderer.setMarkedBold(markedCellBold);
    renderer.setMarkedBackgroundColor(null);
    // TODO abf 2019-07-23 hier noch bei this.xxxx die Breite einstellen
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
            default:
              Logger.log(Logger.ERROR, Logger.MSG_ABF_ERROR,
                  "setActions(): unreachable switch: "
                      + "setActions() actionTypes = " + actionTypes[i]);
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

    northSideCalenderPanel = new JPanel(new BorderLayout());
    if (persistence.getName().equals("boatreservations")
        && category.equals("BASE_CAT")) {
      northSideCalenderPanel.setBorder(new EmptyBorder(11, 31, 0, 31));
      drawCalendar();
      // refreshCalendar(0, currentMonth, currentYear); // funktioniert nicht
      northSideCalenderPanel.add(pnlCalendarPanel, BorderLayout.NORTH);

      if (filterFieldValue != null
          && dlg instanceof BoatReservationListDialog) {
        String filterFieldDescription = ((BoatReservationListDialog) dlg)
            .getFilterFieldDescription();
        JLabel filterName = new JLabel();
        filterName.setText(filterFieldDescription);
        filterName.setBorder(new EmptyBorder(10, 0, 0, 0));
        filterName.setHorizontalAlignment(SwingConstants.CENTER);
        northSideCalenderPanel.add(filterName, BorderLayout.SOUTH);
      }
    }

    searchPanel = new JPanel();
    searchPanel.setLayout(new GridBagLayout());
    GridBagConstraints gridBagConstraints = new GridBagConstraints(
        0, 10,
        0, 0,
        0.0, 0.0,
        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0),
        0, 0);
    centerTableListPanel = new JPanel();
    centerTableListPanel.setLayout(new GridBagLayout());
    centerTableListPanel.add(searchPanel, gridBagConstraints);
    searchField = new ItemTypeString("SEARCH_FIELD", "", IItemType.TYPE_PUBLIC,
        "SEARCH_CAT", International.getString("Suche"));
    searchField.registerItemListener(this);
    searchField.displayOnGui(dlg, searchPanel, 0, 0);
    filterBySearch = new ItemTypeBoolean("FILTERBYSEARCH", true, IItemType.TYPE_PUBLIC,
        "SEARCH_CAT", International.getString("filtern"));
    filterBySearch.registerItemListener(this);
    filterBySearch.displayOnGui(dlg, searchPanel, 10, 0);

    JPanel buttonPanelWestMain;
    buttonPanelWestMain = new JPanel();
    buttonPanelWestMain.setLayout(new GridBagLayout());
    buttonPanelWestMain.setAlignmentY(Component.TOP_ALIGNMENT);
    JPanel buttonPanelEastAdditional;
    buttonPanelEastAdditional = new JPanel();
    buttonPanelEastAdditional.setLayout(new GridBagLayout());
    buttonPanelEastAdditional.setAlignmentY(Component.TOP_ALIGNMENT);
    rightSouthSideButtonPanel = new JPanel(new BorderLayout());
    rightSouthSideButtonPanel.setBorder(new EmptyBorder(15, 100, 15, 100));
    rightSouthSideButtonPanel.add(buttonPanelWestMain, BorderLayout.CENTER); // CENTER besser als
                                                                             // WEST
    rightSouthSideButtonPanel.add(buttonPanelEastAdditional, BorderLayout.EAST);

    myPanel = new JPanel();
    myPanel.setLayout(new BorderLayout());
    myPanel.add(northSideCalenderPanel, BorderLayout.NORTH);
    myPanel.add(centerTableListPanel, BorderLayout.CENTER);
    myPanel.add(rightSouthSideButtonPanel, buttonPanelPosition);

    actionButtons = new Hashtable<>();
    JPanel smallButtonPanel = null;
    boolean isMainButtonsWestGroup = true;
    for (int i = 0; actionText != null && i < actionText.length; i++) {
      if (actionTypes[i] >= 2000) {
        continue; // actions >= 2000 not shown as buttons
      }
      String action = ACTION_BUTTON + "_" + actionTypes[i];
      // >= 2000 just as small buttons without text
      String descriptionAction = actionTypes[i] < 1000 ? actionText[i] : null;
      ItemTypeButton button = new ItemTypeButton(action,
          IItemType.TYPE_PUBLIC, "BUTTON_CAT", descriptionAction);
      button.registerItemListener(this);
      if (actionTypes[i] < 1000) {
        int padYbefore2 = 0;
        if (i > 0 && actionTypes[i] < 0 && actionTypes[i - 1] >= 0) {
          padYbefore2 = 36;
          isMainButtonsWestGroup = false;
        }
        button.setPadding(20, 20, padYbefore2, 5);
        button.setFieldSize(200, -1);
      } else {
        button.setPadding(5, 5, 5, 5);
        button.setFieldSize(50, -1);
        if (smallButtonPanel == null) {
          smallButtonPanel = new JPanel();
          smallButtonPanel.setLayout(new GridBagLayout());
          GridBagConstraints gridBagConstraintsButtons = new GridBagConstraints(
              0, i, 1, 1, 0.0, 0.0,
              GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
              new Insets(20, 0, 20, 0),
              0, 0);
          buttonPanelWestMain.add(smallButtonPanel, gridBagConstraintsButtons);
        }
      }
      if (actionIcons != null && i < actionIcons.length && actionIcons[i] != null) {
        String iconName = actionIcons[i];
        if (iconName.startsWith(BUTTON_IMAGE_CENTERED_PREFIX)) {
          iconName = iconName.substring(1);
        } else {
          button.setHorizontalAlignment(SwingConstants.LEFT);
        }
        if (iconName.length() > 0) {
          button.setIcon(BaseDialog.getIcon(iconName));
        }
      }
      if (actionTypes[i] < 1000) {
        if (isMainButtonsWestGroup) {
          button.displayOnGui(dlg, buttonPanelWestMain, 0, i);
        } else {
          button.displayOnGui(dlg, buttonPanelEastAdditional, 0, i);
        }
      } else {
        button.displayOnGui(dlg, smallButtonPanel, i, 0);
      }
      actionButtons.put(button, action);
    }
  }

  @Override
  public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
    iniDisplayActionTable(dlg);
    GridBagConstraints gridBagConstraints = new GridBagConstraints(
        x, y,
        fieldGridWidth, fieldGridHeight,
        0.0, 0.0,
        fieldGridAnchor, fieldGridFill,
        new Insets(padYbefore, padXbefore, padYafter, padXafter),
        0, 0);
    panel.add(myPanel, gridBagConstraints);
    super.displayOnGui(dlg, centerTableListPanel, 0, 0);
    return 1;
  }

  @Override
  public int displayOnGui(Window dlg, JPanel panel, String borderLayoutPosition) {
    iniDisplayActionTable(dlg);
    panel.add(myPanel, borderLayoutPosition);
    super.displayOnGui(dlg, centerTableListPanel, 0, 0);
    return 1;
  }

  public void setVisibleButtonPanel(boolean visible) {
    rightSouthSideButtonPanel.setVisible(visible);
  }

  public void setVisibleSearchPanel(boolean visible) {
    searchPanel.setVisible(visible);
  }

  @Override
  public void showValue() {
    items = new Hashtable<>();
    mappingKeyToRecord = new Hashtable<>();
    if (data == null && persistence != null) {
      updateData();
    }
    boolean isVersionized = false;
    if (persistence != null) {
      isVersionized = persistence.data().getMetaData().isVersionized();
    }
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
    if (event instanceof ActionEvent
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
      if (itemType instanceof ItemTypeButton) {
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
      if (Daten.isNotWriteModeMitSchluessel()) {
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
            if (dlg instanceof BoatReservationEditDialog) {
              BoatReservationRecord reservation = ((BoatReservationEditDialog) dlg).getDataRecord();
              // reservation saved? persisted? valid?;
              if (!reservation.getContact().isEmpty() // validRecord?
                  && reservation.getLastModified() > 0) {
                String aktion = "INSERT";
                reservation.sendEmailBeiReservierung(aktion);
                String fehlermeldung = reservation.checkAndDisplayWarning();
                if (!fehlermeldung.isEmpty()) {
                  String warnungTitel = "Hinweis: neue Reservierung in der Vergangenheit ";
                  Dialog.infoDialog(warnungTitel, fehlermeldung);
                  Logger.log(Logger.INFO, Logger.MSG_EVT_TRIPSTART, warnungTitel
                      + reservation.getBoatName() + " "
                      + reservation.getDateTimeFromDescription(
                          BoatReservationRecord.REPLACE_HEUTE));
                  // TODO 2021-03-10 abf JETZT den Background-Job unterbrechen
                  // efaBoathouseBackgroundTask.interrupt();
                } else {
                  try {
                    // allowed for identified Persons with Id
                    // if (reservation.getPersonId() != null) { // validRecord?

                    if (Daten.efaConfig.getValueEfaDirekt_showAdvancedReserveAdditionalsDialog()){
                        reserveAdditionalItems(reservation, admin != null);
                    }else{
                        if (admin != null) {
                          uebertragenAufAndereBoote(reservation);
                        } else {
                          uebertragenAufAndereBooteDieserGruppe(reservation);
                        }
                    }
                  } catch (EfaException e1) {
                    Logger.log(Logger.ERROR, Logger.MSG_ERR_PANIC, e1);
                  }
                }
              }
            }
            break;
          case ACTION_EDIT:
            for (int i = 0; records != null && i < records.length; i++) {
              if (records[i] != null) {
                if (records[i].getDeleted()) {
                  int auswahlYesNoCancelDialog = Dialog.yesNoCancelDialog(
                      International.getString("Datensatz wiederherstellen"),
                      International.getMessage(
                          "Der Datensatz '{record}' wurde gelöscht. Möchtest Du ihn wiederherstellen?",
                          records[i].getQualifiedName()));
                  switch (auswahlYesNoCancelDialog) {
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
                    default:
                      Logger.log(Logger.ERROR, Logger.MSG_ABF_ERROR,
                          "itemListenerAction()1: unreachable switch: "
                              + "Datensatz wiederherstellen auswahlYesNoCancelDialog = "
                              + auswahlYesNoCancelDialog);
                      break;
                  }
                }
                dlg = itemListenerActionTable.createNewDataEditDialog(getParentDialog(),
                    persistence, records[i]);
                if (dlg == null) {
                  return;
                }
                String beforeEdit = records[i].toString();
                dlg.showDialog();
                if (!dlg.getDialogResult()) {
                  break;
                }
                if (dlg instanceof BoatReservationEditDialog) {
                  BoatReservationRecord reservation = ((BoatReservationEditDialog) dlg)
                      .getDataRecord();
                  String afterEdit = reservation.toString();
                  boolean reservationHasBeenChanged = !beforeEdit.equals(afterEdit);
                  if (reservationHasBeenChanged) {
                    String aktion = "UPDATE";
                    reservation.sendEmailBeiReservierung(aktion);
                  }
                  // checkAndDisplayErrors(reservation);
                }
              }
            }
            break;
          case ACTION_DELETE:
            if (records == null) {
              return;
            }
            // löschen alter Termine verhindern
            if (!itemListenerActionTable.deleteCallback(records)) {
              updateData();
              showValue();
              return;
            }
            int res;
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
              VersionizedDataDeleteDialog ddlg = new VersionizedDataDeleteDialog(getParentDialog(),
                  (records.length == 1 ? records[0].getQualifiedName()
                      : International.getMessage("{count} Datensätze", records.length)));
              ddlg.showDialog();
              deleteAt = ddlg.getDeleteAtResult();
              if (deleteAt == Long.MAX_VALUE || deleteAt < -1) {
                return;
              }
            }
            try {
              for (int i = 0; i < records.length; i++) {
                if (records[i] != null) {
                  if (persistence.data().getMetaData().isVersionized()) {
                    if (records[i] instanceof BoatReservationRecord) {
                      BoatReservationRecord reservation = (BoatReservationRecord) records[i];
                      String aktion = "DELETE";
                      reservation.sendEmailBeiReservierung(aktion);
                    }
                    persistence.data().deleteVersionizedAll(records[i].getKey(), deleteAt);
                    String whoUser = admin != null
                        ? International.getString("Admin") + " '" + admin.getName() + "'"
                        : International.getString("Normaler Benutzer");
                    if (deleteAt >= 0) {
                      Logger.log(Logger.INFO, Logger.MSG_DATAADM_RECORDDELETEDAT,
                          records[i].getPersistence().getDescription() + ": "
                              + International.getMessage(
                                  "{name} hat Datensatz '{record}' ab {date} gelöscht.",
                                  whoUser, records[i].getQualifiedName(),
                                  EfaUtil.getTimeStampDDMMYYYY(deleteAt)));
                    } else {
                      Logger.log(Logger.INFO, Logger.MSG_DATAADM_RECORDDELETED,
                          records[i].getPersistence().getDescription() + ": "
                              + International.getMessage(
                                  "{name} hat Datensatz '{record}' zur vollständigen Löschung markiert.",
                                  whoUser, records[i].getQualifiedName()));
                    }
                  } else {
                    String name = "";
                    if (records[i] instanceof BoatReservationRecord) {
                      BoatReservationRecord reservation = (BoatReservationRecord) records[i];
                      String aktion = "DELETE";
                      reservation.sendEmailBeiReservierung(aktion);
                      name += " ab " + reservation.getDateTimeFromDescription(
                          BoatReservationRecord.REPLACE_HEUTE);
                      name += " für " + reservation.getPersonAsName();
                    }
                    String whoUser;
                    if (admin != null) {
                      whoUser = International.getString("Admin") + " '" + admin.getName() + "'";
                    } else {
                      whoUser = International.getString("Normaler Benutzer");
                    }
                    persistence.data().delete(records[i].getKey());
                    Logger.log(Logger.INFO, Logger.MSG_DATAADM_RECORDDELETED,
                        records[i].getPersistence().getDescription() + ": "
                            + International.getMessage(
                                "{name} hat Datensatz '{record}' gelöscht.",
                                whoUser, records[i].getQualifiedName() + name));
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
          case DataListDialog.ACTION_IMPORT:
          case DataListDialog.ACTION_EXPORT:
          case DataListDialog.ACTION_MERGE:
          case DataListDialog.ACTION_PRINTLIST:
          case DataListDialog.ACTION_EDITASSISTENT:
          case StatisticsListDialog.ACTION_CREATESTATISTICS: // Lasse
          case StatisticsListDialog.ACTION_ONETIMESTATISTIC: // Boris selber
          case StatisticsListDialog.ACTION_MOVEUP: // Lasse
          case StatisticsListDialog.ACTION_MOVEDOWN: // Lasse noch nicht
            break;
          default:
            Logger.log(Logger.WARNING, Logger.MSG_ABF_ERROR,
                "itemListenerAction()2: unreachable switch: "
                    + "itemListenerAction actionId = " + actionId);
            break;
        }
      }
      if (itemListenerActionTable != null) {
        itemListenerActionTable.itemListenerActionTable(actionId, records);
      }
      updateData();
      showValue();
    }
    if (event instanceof KeyEvent && event.getID() == KeyEvent.KEY_RELEASED
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
            if (t.contains(s)) {
              rowFound = i;
            }

            if (sv != null && rowFound < 0) {
              // match column against substrings
              for (int k = 0; k < sv.size(); k++) {
                if (t.contains(sv.get(k))) {
                  sb[k] = true;
                }
              }
            }
          }
          if (sb != null && rowFound < 0) {
            rowFound = i;
            for (boolean b : sb) {
              if (!b) {
                rowFound = -1;
                break;
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
        && (event instanceof KeyEvent && event.getID() == KeyEvent.KEY_RELEASED
            && itemType == searchField)
        || (event instanceof ActionEvent && event.getID() == ActionEvent.ACTION_PERFORMED
            && itemType == filterBySearch)) {
      updateFilter();
    }
  }

  private void reserveSelectedItems(List<IItemType> selectedItems, BoatReservationRecord reservation) throws EfaException {
    ArrayList<String> fehlerListe = new ArrayList<>();
    String lastException = "";

    BoatRecord originalBoat = reservation.getBoat();

    BoatReservations reservations = Daten.project.getBoatReservations(true);
    for (IItemType iItemType : selectedItems) {
      if (!(iItemType instanceof ItemTypeBoolean)) {
        // erste Zeilen überspringen
        continue;
      }
      boolean selectedInGui = ((ItemTypeBoolean) iItemType).getValue();
      if (!selectedInGui) {
        // diese Boote wurden nicht ausgewählt
        continue;
      }
      @SuppressWarnings("unchecked")
      DataKey<UUID, Long, String> dataKey = iItemType.getDataKey();
      UUID selectedBoatId = dataKey.getKeyPart1();
      if (originalBoat.getId().equals(selectedBoatId)) {
        // das eigene Boot haben wir schon reserviert
        continue;
      }

      // neue Reservierung: alle Parameter einzeln eintragen
      BoatReservationRecord newReservationsRecord = reservations
              .createBoatReservationsRecordFromClone(selectedBoatId, reservation);

      if (versionizedRecordOfThatNameAlreadyExists(newReservationsRecord)) {
        // seems to have conflicts TODO
        // keep track of failures in a List
        fehlerListe.add("- leider kein " + newReservationsRecord.getBoatName());
        fehlerListe.add("--versionizedRecordOfThatNameAlreadyExists");
        continue;
      }

      // check Conflicts with same time
      try {
        reservations.preModifyRecordCallback(newReservationsRecord, true, false, false);
      } catch (EfaModifyException e) {
        // Logger.log(Logger.INFO, Logger.MSG_DATA_UPDATECONFLICT, e); // MSG_DATA_CREATEFAILED
        fehlerListe.add("- leider kein " + newReservationsRecord.getBoatName());
        lastException = e.getLocalizedMessage();
        continue;
      }

      reservations.data().add(newReservationsRecord);
      String aktion = "INSERT";
      newReservationsRecord.sendEmailBeiReservierung(aktion);
      Logger.log(Logger.INFO, Logger.MSG_DATAADM_RECORDADDED,
              newReservationsRecord.getPersistence().getDescription() + ": " +
                      International.getMessage("{name} hat neuen Datensatz '{record}' erstellt.",
                              (admin != null
                                      ? International.getString("Admin") + " '" + admin.getName() + "!'"
                                      : newReservationsRecord.getPersonAsName()),
                              newReservationsRecord.getQualifiedName() + " "
                                      + newReservationsRecord.getReservationTimeDescription(
                                      BoatReservationRecord.REPLACE_HEUTE)));
    } // for loop
    if (!fehlerListe.isEmpty()) {
      // display the failures at end
      String s = "";
      s += "Für die Zeit " + reservation.getReservationTimeDescription(
              BoatReservationRecord.REPLACE_HEUTE) + "\n";
      s += "konnten nicht alle Boote automatisch mitreserviert werden.\n";
      for (String string : fehlerListe) {
        s += string + "\n";
      }
      s += lastException;
      Dialog.infoDialog("Fehlerprotokoll", s);
    }
  }

  private void reserveAdditionalItems(BoatReservationRecord reservation, boolean adminMode) throws EfaException {

    List<IItemType> selectedItems = ReserveAdditionalsDialog.showInputDialog(getParentDialog(), reservation, items, adminMode);

    if (!selectedItems.isEmpty()){
      reserveSelectedItems(selectedItems, reservation);
    }
  }


  /**
   * Bitte diese Reservierung übertragen auf alle Boote dieser Gruppen
   *
   */
  private void uebertragenAufAndereBoote(BoatReservationRecord dataRecord) throws EfaException {
    String[] boatSeatsValuesArray = EfaTypes
        .makeBoatSeatsArray(EfaTypes.ARRAY_STRINGLIST_VALUES);
    String[] boatSeatsDisplayArray = EfaTypes
        .makeBoatSeatsArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY);

    int header = 3;
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
    items[2] = new ItemTypeLabel("L1", IItemType.TYPE_INTERNAL, "",
        "Achtung: Schon 1 Häckchen reserviert viele Boote, evtl. Mailflut");
    boolean success = MultiInputDialog.showInputDialog(getParentDialog(),
        International.getString("Übertragen auf ganze Gruppen"), items);
    if (success) {

      // join items
      Map<Integer, String> mapBootsTypen = new HashMap<>();
      for (int i = 0; i < boatSeatsValuesArray.length; i++) {
        boolean selectedGui = ((ItemTypeBoolean) items[header + i]).getValue();
        if (selectedGui) {
          mapBootsTypen.put(i, boatSeatsValuesArray[i]);
        }
      }

      BoatReservations reservations = Daten.project.getBoatReservations(true);
      Boats boats = Daten.project.getBoats(false);
      IDataAccess data2 = boats.data();
      long now = System.currentTimeMillis();
      for (DataKey<?, ?, ?> dataKey : data2.getAllKeys()) {
        BoatRecord boatRecord = (BoatRecord) data2.get(dataKey);
        if (!boatRecord.isValidAt(now)) {
          // Boot nicht mehr gültig, abgelaufen
          continue;
        }
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

          if (!versionizedRecordOfThatNameAlreadyExists(newReservationsRecord)) {
            reservations.data().add(newReservationsRecord);
            String aktion = "INSERT";
            newReservationsRecord.sendEmailBeiReservierung(aktion);
            Logger.log(Logger.INFO, Logger.MSG_DATAADM_RECORDADDED,
                newReservationsRecord.getPersistence().getDescription() + ": "
                    + International.getMessage("{name} hat neuen Datensatz '{record}' erstellt.",
                        (admin != null
                            ? International.getString("Admin") + " '" + admin.getName() + "!'"
                            : newReservationsRecord.getPersonAsName()),
                        newReservationsRecord.getQualifiedName() + " "
                            + newReservationsRecord.getReservationTimeDescription(
                                BoatReservationRecord.REPLACE_HEUTE)));
          }
        }
      }
    }
  }

  /**
   * Bitte diese Reservierung übertragen auf alle Boote dieser Gruppe
   *
   */
  private void uebertragenAufAndereBooteDieserGruppe(BoatReservationRecord reservationRecord)
      throws EfaException {
    BoatRecord originalBoat = reservationRecord.getBoat();

    ArrayList<IItemType> liste = createListOfSelectableItemsSimilarTo(originalBoat);
    liste.add(new ItemTypeLabel("*A-L0", IItemType.TYPE_INTERNAL, "",
        "Anstatt mehrere Ausleihen zu machen,"));
    liste.add(new ItemTypeLabel("*A-L1", IItemType.TYPE_INTERNAL, "",
        "kannst Du direkt weitere \"Boote\" anklicken."));
    liste.add(new ItemTypeLabel("*A-L2", IItemType.TYPE_INTERNAL, "",
        "Zeit: " + reservationRecord.getReservationTimeDescription(
            BoatReservationRecord.REPLACE_HEUTE)));
    liste.add(new ItemTypeLabel("*A-L3", IItemType.TYPE_INTERNAL, "",
        "Sollen andere Boote genauso reserviert werden?"));
    liste.add(new ItemTypeLabel("*A-L4", IItemType.TYPE_INTERNAL, "",
        "-> Bitte auswählen: (bitte nicht alle, evtl. Mailflut)"));

    IItemType[] items = createArraySortedByName(liste);

    boolean pressedOKAY = MultiInputDialog.showInputDialog(getParentDialog(),
        International.getString("Übertragen auf andere Boote dieser Gruppe"),
        items);
    if (!pressedOKAY) {
      return;
    }

    reserveSelectedItems(liste, reservationRecord);
  }


  private ArrayList<IItemType> createListOfItemsByTypeSeats(String typeSeats)
          throws  EfaException{
    IDataAccess allBoats = Daten.project.getBoats(false).data();
    ArrayList<IItemType> liste = new ArrayList<>();
    long now = System.currentTimeMillis();
    for (DataKey<?, ?, ?> dataKey : allBoats.getAllKeys()) {
      BoatRecord boatRecord = (BoatRecord) allBoats.get(dataKey);
      if (!boatRecord.isValidAt(now)) {
        // Boot nicht mehr gültig, abgelaufen
        continue;
      }
      if (!typeSeats.equals(boatRecord.getTypeSeats(0))) {
        // aber nicht fremde Bootstypen - hier als Sitzplätze
        // für andere Bootstypen, bitte neue Reservierung dort aufmachen.
        continue;
      }
      ItemTypeBoolean item = new ItemTypeBoolean(boatRecord.getName(), false,
              IItemType.TYPE_INTERNAL, "", boatRecord.getQualifiedName());
      item.setDataKey(boatRecord.getKey());
      liste.add(item);
    }
    return liste;
  }


  private ArrayList<IItemType> createListOfSelectableItemsSimilarTo(BoatRecord originalBoat)
      throws EfaException {
    ArrayList<IItemType> liste = new ArrayList<>();
    IDataAccess boats = Daten.project.getBoats(false).data();
    long now = System.currentTimeMillis();
    for (DataKey<?, ?, ?> dataKey : boats.getAllKeys()) {
      BoatRecord boatRecord = (BoatRecord) boats.get(dataKey);
      if (!boatRecord.isValidAt(now)) {
        // Boot nicht mehr gültig, abgelaufen
        continue;
      }
      if (!originalBoat.getTypeSeats(0).equals(boatRecord.getTypeSeats(0))) {
        // aber nicht fremde Bootstypen - hier als Sitzplätze
        // für andere Bootstypen, bitte neue Reservierung dort aufmachen.
        continue;
      }

      ItemTypeBoolean item = new ItemTypeBoolean(boatRecord.getName(), false,
          IItemType.TYPE_INTERNAL, "", boatRecord.getQualifiedName());
      item.setDataKey(boatRecord.getKey());
      if (originalBoat.getId().equals(boatRecord.getId())) {
        // das eigene Boot haben wir schon reserviert
        // item.setName("*A-L9 " + item.getName());
        item.setEnabled(false);
        item.setValue(true);
      }
      liste.add(item);
    }
    return liste;
  }

  private IItemType[] createArraySortedByName(ArrayList<IItemType> liste) {
    liste.sort(Comparator.comparing(IItemType::getName));
    IItemType[] items = new IItemType[liste.size()];
    return liste.toArray(items);
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
      String warn = International.getString("Es existiert bereits ein gleichnamiger Datensatz!");
      if (Dialog.yesNoDialog(International.getString("Warnung"),
          warn + "\n"
              + conflict + "\n"
              + International
                  .getString("Möchtest Du diesen Datensatz trotzdem erstellen?")) != Dialog.YES) {
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
      // wochentagFilter = null; // TODO
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
      centerTableListPanel.remove(aggregationTable);
    }

    if (create && data != null) {
      int size = data.size();
      if (size > 0) {
        String[] aggregationStrings = new String[header.length];
        for (int i = 0; i < header.length; i++) {
          aggregationStrings[i] = "";
        }

        HashMap<String, Object> overallInfo = new HashMap<>();
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
          GridBagConstraints gridBagConstraints = new GridBagConstraints(
              0, 1,
              1, 1,
              0.0, 0.0,
              GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
              new Insets(1, 1, 1, 1),
              0, 0);
          aggregationTable = new JTable(dataModel);
          centerTableListPanel.add(aggregationTable, gridBagConstraints);
          centerTableListPanel.setComponentZOrder(aggregationTable, 0);
        }
      }
    }

    centerTableListPanel.revalidate();
    centerTableListPanel.repaint();
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
      data = new Vector<>();
      IDataAccess dataAccess = persistence.data();
      boolean isVersionized = dataAccess.getMetaData().isVersionized();
      DataKeyIterator it = dataAccess.getStaticIterator();
      DataKey<?, ?, ?> key = it.getFirst();
      Hashtable<DataKey<?, ?, ?>, String> uniqueHash = new Hashtable<>();
      if (updateDataRightSideCalendar) {
        mappingDateToReservations = new Hashtable<>();
        mappingWeekdayToReservations = new Hashtable<>();
        mappingMinWeekdayToReservations = new Hashtable<>();
        mappingMaxWeekdayToReservations = new Hashtable<>();
        mappingBootshausDateToReservations = new Hashtable<>();
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
                || allFieldsAsLowerText.contains(filterByAnyText)
                || wochentagFilter != null && allFieldsAsLowerText.contains(wochentagFilter)
                    && r instanceof BoatReservationRecord
                    && ((BoatReservationRecord) r).getDateTo() == null) {
              if (!(r instanceof ClubworkRecord) || Daten.isAdminMode()
                  || isToday(r.getLastModified())) {
                data.add(r);
              }
            }
          }
          if (updateDataRightSideCalendar) {
            if (r instanceof BoatReservationRecord) {
              mappingDateToName((BoatReservationRecord) r);
            }
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
    Vector<DataRecord> sortedData = new Vector<>();
    for (int i = 0; i < data.size(); i++) {
      sortedData.add(mappingKeyToRecord.get(keys[table.getOriginalIndex(i)]));
    }
    return sortedData;
  }

  public Vector<DataRecord> getSelectedData() {
    String[] keys = getSelectedKeys();
    Vector<DataRecord> selectedData = new Vector<>();
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

    mtblCalendar = new CalendarTableModel(); // egal nur Model 6x7tage
    mtblCalendar.setRowCount(6);
    // mtblCalendar.setColumnCount(7);

    // Add headers
    String[] headers = { " Montag", " Dienstag", " Mittwoch", " Donnerstag", " Freitag", " Samstag",
        " Sonntag" }; // All headers
    for (int i = 0; i < 7; i++) {
      mtblCalendar.addColumn(headers[i]);
    }

    tblCalendar = new JTable(mtblCalendar); // kennt seine Höhe
    tblCalendar.setRowHeight(58); // 58 // Set row/column count
    tblCalendar.getTableHeader().setResizingAllowed(false);
    tblCalendar.getTableHeader().setReorderingAllowed(false);
    tblCalendar.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    tblCalendar.setColumnSelectionAllowed(true);
    tblCalendar.setRowSelectionAllowed(true);
    tblCalendar.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        int day = getDay(e.getPoint());
        refreshCalendar(day, currentMonth, currentYear);
        updateFilterWithDate(day); // after clicking
        repaintCalendarButtons();
      }
    });

    btnPrev = new JButton("     <<-     ");
    lblMonth = new JButton("heute");
    btnNext = new JButton("     ->>     ");

    // Register action listeners
    btnPrev.addActionListener(new btnPrev_Action());
    lblMonth.addActionListener(new lblMonth_Action());
    btnNext.addActionListener(new btnNext_Action());

    JPanel pnlCalendarButtonPanel = new JPanel(new BorderLayout());
    pnlCalendarButtonPanel.add(btnPrev, BorderLayout.WEST);
    pnlCalendarButtonPanel.add(lblMonth, BorderLayout.CENTER);
    pnlCalendarButtonPanel.add(btnNext, BorderLayout.EAST);

    pnlCalendarPanel = new JPanel(new BorderLayout());
    pnlCalendarPanel.add(tblCalendar.getTableHeader(), BorderLayout.NORTH);
    pnlCalendarPanel.add(tblCalendar, BorderLayout.CENTER);
    pnlCalendarPanel.add(pnlCalendarButtonPanel, BorderLayout.SOUTH);

    // Get real month/year
    GregorianCalendar cal = new GregorianCalendar(); // Create calendar
    realDay = cal.get(GregorianCalendar.DAY_OF_MONTH); // Get day
    realMonth = cal.get(GregorianCalendar.MONTH); // Get month
    realYear = cal.get(GregorianCalendar.YEAR); // Get year
    currentMonth = realMonth; // Match month and year
    currentYear = realYear;

    // Refresh calendar
    refreshCalendar(realDay, currentMonth, currentYear); // Refresh calendar
    // don't filter at startup
    // updateFilterWithDate(0); // button Month
  }

  private void repaintCalendarButtons() {
    btnPrev.repaint();
    lblMonth.repaint();
    btnNext.repaint();
  }

  protected int getDay(Point point) {
    int row = tblCalendar.rowAtPoint(point);
    int col = tblCalendar.columnAtPoint(point);

    // row oder col dürfen nicht < 0 sein
    if (row < 0) {
      return 0;
    }
    if (col < 0) {
      return 0;
    }
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
      int row = aktuell / 7;
      int column = aktuell % 7;
      String buchungStand = getBuchungString(new DataTypeDate(i, month + 1, year));
      CalendarString aValue = new CalendarString(i, buchungStand);
      mtblCalendar.setValueAt(aValue, row, column);
    }

    // Apply renderer
    TblCalendarRenderer tblCalendarRenderer = new TblCalendarRenderer(
        realYear, realMonth, realDay,
        currentYear, currentMonth, day);
    tblCalendar.setDefaultRenderer(tblCalendar.getColumnClass(0), tblCalendarRenderer);
  }

  public String getBuchungString(DataTypeDate date) {
    String myBuchungtext = "";
    Integer weekday = date.toCalendar().get(Calendar.DAY_OF_WEEK);

    String recurringEvent = mappingWeekdayToReservations.get(weekday);
    if (recurringEvent != null) {
      DataTypeDate minDate = mappingMinWeekdayToReservations.get(weekday);
      if (minDate == null) {
        minDate = date;
      }
      DataTypeDate maxDate = mappingMaxWeekdayToReservations.get(weekday);
      if (maxDate == null) {
        maxDate = date;
      }
      if (date.isAfterOrEqual(minDate) && maxDate.isAfterOrEqual(date)) {
        myBuchungtext += recurringEvent;
      }
    }
    String singleBootshausEvent = mappingBootshausDateToReservations.get(date);
    if (singleBootshausEvent != null) {
      myBuchungtext += singleBootshausEvent;
    }
    Integer singleEventCount = mappingDateToReservations.get(date);
    if (singleEventCount != null) {
      String trennzeichen = "'";
      myBuchungtext += trennzeichen + singleEventCount;
    }
    return myBuchungtext;
  }

  class btnPrev_Action implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      if (currentMonth == 0) { // Back one year
        currentMonth = 11;
        currentYear -= 1;
      } else { // Back one month
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
      } else { // Foward one month
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

  private void mappingDateToName(BoatReservationRecord brr) {
    String strDayOfWeek = brr.getDayOfWeek();
    Integer wochentag = getWochentag(strDayOfWeek);
    if (wochentag != null) {
      String regelterminKuerzel = "r";
      mappingWeekdayToReservations.put(wochentag, regelterminKuerzel);

      // Das "r" muss wissen, wann es anfangen soll!
      DataTypeDate neuesMinDate = brr.getDateFrom();
      if (neuesMinDate == null) {
        neuesMinDate = DataTypeDate.today();
        neuesMinDate.addDays(-30);
      }
      DataTypeDate bisherigesMinDate = mappingMinWeekdayToReservations.get(wochentag);
      if (bisherigesMinDate == null) {
        bisherigesMinDate = neuesMinDate;
      }
      if (bisherigesMinDate.isAfterOrEqual(neuesMinDate)) {
        mappingMinWeekdayToReservations.put(wochentag, neuesMinDate);
      }

      // Das "r" muss wissen, wann es aufhören soll!
      DataTypeDate neuesMaxDate = brr.getDateTo();
      if (neuesMaxDate == null) {
        neuesMaxDate = DataTypeDate.today();
        neuesMaxDate.addDays(366);
      }
      DataTypeDate bisherigesMaxDate = mappingMaxWeekdayToReservations.get(wochentag);
      if (bisherigesMaxDate == null) {
        bisherigesMaxDate = neuesMaxDate;
      }
      if (neuesMaxDate.isAfterOrEqual(bisherigesMaxDate)) {
        mappingMaxWeekdayToReservations.put(wochentag, neuesMaxDate);
      }
      return; // Regeltermine nicht zusammenzählen
    }

    boolean isBootshausReservierung = brr.isBootshausOH();
    String bootshausKuerzel = "BH";
    List<DataTypeDate> dates = getListOfDates(brr.getDateFrom(), brr.getDateTo());
    for (DataTypeDate dataTypeDate : dates) {
      if (isBootshausReservierung) {
        mappingBootshausDateToReservations.put(dataTypeDate, bootshausKuerzel);
      } else {
        Integer oldValue = mappingDateToReservations.get(dataTypeDate);
        oldValue = (oldValue == null) ? 0 : oldValue;
        mappingDateToReservations.put(dataTypeDate, oldValue + 1);
      }
    }
  }

  private List<DataTypeDate> getListOfDates(DataTypeDate dateFrom, DataTypeDate dateTo) {
    List<DataTypeDate> datumListe = new ArrayList<>();
    if (dateFrom != null && dateTo != null) {
      DataTypeDate myDate = new DataTypeDate(dateFrom);
      for (; myDate.isBeforeOrEqual(dateTo); myDate.addDays(1)) {
        datumListe.add(new DataTypeDate(myDate));
      }
    }
    return datumListe;
  }

  private Integer getWochentag(String dayName) {
    if (dayName == null) {
      return null;
    }
    SimpleDateFormat dayFormat = new SimpleDateFormat("E", Locale.US);
    Calendar calendar = Calendar.getInstance();
    try {
      Date date = dayFormat.parse(dayName);
      calendar.setTime(date);
      return calendar.get(Calendar.DAY_OF_WEEK);
    } catch (ParseException e) {
      e.printStackTrace();
      return null;
    }
  }
}
