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

import java.awt.AWTEvent;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;

import javax.swing.JDialog;

import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.IItemListener;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeDate;
import de.nmichael.efa.core.items.ItemTypeRadioButtons;
import de.nmichael.efa.data.BoatReservationRecord;
import de.nmichael.efa.util.International;

// @i18n complete
public class BoatReservationEditDialog extends UnversionizedDataEditDialog implements IItemListener {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public BoatReservationEditDialog(Frame parent, BoatReservationRecord r,
      boolean newRecord, boolean allowWeeklyReservation, AdminRecord admin) throws Exception {
    super(parent, International.getString("Reservierung"), r, newRecord, admin);
    initListener();
    setAllowWeeklyReservation(allowWeeklyReservation);
  }

  public BoatReservationEditDialog(JDialog parent, BoatReservationRecord r,
      boolean newRecord, boolean allowWeeklyReservation, AdminRecord admin) throws Exception {
    super(parent, International.getString("Reservierung"), r, newRecord, admin);
    initListener();
    setAllowWeeklyReservation(allowWeeklyReservation);
  }

  @Override
  public void keyAction(ActionEvent evt) {
    _keyAction(evt);
  }

  private void initListener() {
    IItemType itemType = null;
    for (IItemType item : allGuiItems) {
      if (item.getName().equals(BoatReservationRecord.TYPE)) {
        ((ItemTypeRadioButtons) item).registerItemListener(this);
        itemType = item;
      }
      if (item.getName().equals(BoatReservationRecord.DATEFROM)) {
        item.registerItemListener(this);
      }
    }
    itemListenerAction(itemType, null);
  }

  @Override
  public void itemListenerAction(IItemType item, AWTEvent event) {
    if (item != null && item.getName().equals(BoatReservationRecord.TYPE)) {
      String type = item.getValueFromField();
      if (type == null) {
        return;
      }
      for (IItemType it : allGuiItems) {
        if (it.getName().equals(BoatReservationRecord.DAYOFWEEK)) {
          it.setVisible(type.equals(BoatReservationRecord.TYPE_WEEKLY));
        }
        if (it.getName().equals(BoatReservationRecord.DATEFROM)) {
          it.setVisible(type.equals(BoatReservationRecord.TYPE_ONETIME));
        }
        if (it.getName().equals(BoatReservationRecord.DATETO)) {
          it.setVisible(type.equals(BoatReservationRecord.TYPE_ONETIME));
        }
      }
    }

    if (item != null && item.getName().equals(BoatReservationRecord.DATEFROM) &&
        ((event instanceof FocusEvent && event.getID() == FocusEvent.FOCUS_LOST) ||
        (event instanceof KeyEvent && ((KeyEvent) event).getKeyChar() == '\n'))) {
      ItemTypeDate dateFrom = (ItemTypeDate) item;
      for (IItemType it : allGuiItems) {
        if (it.getName().equals(BoatReservationRecord.DATETO)) {
          ItemTypeDate dateTo = (ItemTypeDate) it;
          if (dateTo.getDate().isBefore(dateFrom.getDate())) {
            dateTo.setValueDate(dateFrom.getDate());
          }
          dateTo.showValue();
          break;
        }
      }
    }

  }

  private void setAllowWeeklyReservation(boolean allowWeeklyReservation) throws Exception {
    if (!allowWeeklyReservation) {
      if (!newRecord && dataRecord != null &&
          BoatReservationRecord.TYPE_WEEKLY.equals(((BoatReservationRecord) dataRecord).getType())) {
        throw new Exception(
            International.getString("Diese Reservierung kann nicht bearbeitet werden."));
      }
      for (IItemType it : allGuiItems) {
        if (it.getName().equals(BoatReservationRecord.TYPE)) {
          it.parseAndShowValue(BoatReservationRecord.TYPE_ONETIME);
          it.setVisible(false);
          it.setEditable(false);
          itemListenerAction(it, null);
          continue;
        }
        if (it.getName().equals(BoatReservationRecord.DAYOFWEEK)) {
          // sonst verhindert ein Dirty das Abbrechen:
          it.parseValue("SUNDAY");
          it.setUnchanged();
        }

      }
    }
  }

  public BoatReservationRecord getDataRecord() {
    return (BoatReservationRecord) dataRecord;
  }
}
