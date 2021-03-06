/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.core.items;

import java.awt.AWTEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;

import de.nmichael.efa.data.types.DataTypeHours;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.Logger;

// @i18n complete

public class ItemTypeHours extends ItemTypeLabelTextfield {

  protected DataTypeHours value;
  protected boolean withSeconds = true;
  protected ItemTypeHours mustBeBeforeTime;
  protected ItemTypeHours mustBeAfterTime;
  protected ItemTypeDate beforeDate;
  protected ItemTypeDate myDate;
  protected boolean mustBeCanBeEqual = false;
  private DataTypeHours referenceTime = null;

  public ItemTypeHours(String name, DataTypeHours value, int type,
      String category, String description) {
    this.name = name;
    this.value = (value != null ? value : new DataTypeHours());
    this.type = type;
    this.category = category;
    this.description = description;
    this.referenceTime = (isSet() ? new DataTypeHours(value) : DataTypeHours.time000000());
  }

  @Override
  public IItemType copyOf() {
    return new ItemTypeHours(name, new DataTypeHours(value), type, category, description);
  }

  public void enableSeconds(boolean withSeconds) {
    this.withSeconds = withSeconds;
    if (value != null) {
      value.enableSeconds(withSeconds);
    }
  }

  public void setReferenceTime(DataTypeHours time) {
    this.referenceTime = (time != null ? time : DataTypeHours.time000000());
  }

  @Override
  public void parseValue(String value) {
    try {
      if (value != null && value.trim().length() > 0) {
        value = EfaUtil.correctHours(value,
            referenceTime.getHour(), referenceTime.getMinute(), referenceTime.getSecond(),
            true, true);

      }
      this.value = DataTypeHours.parseTime(value);
      this.value.enableSeconds(withSeconds);
    } catch (Exception e) {
      if (dlg == null) {
        Logger.log(Logger.ERROR, Logger.MSG_CORE_UNSUPPORTEDDATATYPE,
            "Invalid value for parameter " + name + ": " + value);
      }
    }
  }

  @Override
  public String toString() {
    return (value != null ? value.toString() : "");
  }

  public boolean isSet() {
    return value != null;
  }

  @Override
  protected void field_focusLost(FocusEvent e) {
    super.field_focusLost(e);
    if (!isSet() && isNotNullSet()) {
      if (referenceTime.isSet()) {
        parseValue(referenceTime.toString());
      } else {
        parseValue("00:00:00");
      }

      showValue();
    }
    if (isSet()) {
      referenceTime.setTime(value);
    }
  }

  public DataTypeHours getValue() {
    return value;
  }

  public int getValueHour() {
    return value.getHour();
  }

  public int getValueMinute() {
    return value.getMinute();
  }

  public int getValueSecond() {
    return value.getSecond();
  }

  public DataTypeHours getHours() {
    return new DataTypeHours(value);
  }

  public void setValueHour(int hour) {
    value.setHour(hour);
  }

  public void setValueMinute(int minute) {
    value.setMinute(minute);
  }

  public void setValueSecond(int second) {
    value.setSecond(second);
  }

  public void unset() {
    value.unset();
  }

  @Override
  public boolean isValidInput() {
    if (mustBeBeforeTime != null && isSet() && value.isSet()
        && !value.isBefore(mustBeBeforeTime.value)) {
      return mustBeCanBeEqual && value.equals(mustBeBeforeTime.value);
    }
    if (beforeDate != null && beforeDate.isSet() && myDate != null && myDate.isSet()) {
      if (beforeDate.getDate().isBefore(myDate.getDate()) ||
          (mustBeCanBeEqual && beforeDate.getDate().isBeforeOrEqual(myDate.getDate()))) {
        return true;
      }
    }
    if (mustBeAfterTime != null && isSet() && value.isSet()
        && !value.isAfter(mustBeAfterTime.value)) {
      return mustBeCanBeEqual && value.equals(mustBeAfterTime.value);
    }
    if (isNotNullSet()) {
      return isSet();
    }
    return true;
  }

  public void setMustBeBefore(ItemTypeHours item, boolean mayAlsoBeEqual) {
    mustBeBeforeTime = item;
    mustBeCanBeEqual = mayAlsoBeEqual;
  }

  public void setMustBeAfter(ItemTypeHours item, boolean mayAlsoBeEqual) {
    mustBeAfterTime = item;
    mustBeCanBeEqual = mayAlsoBeEqual;
  }

  public void setMustBeAfter(ItemTypeDate fromDate, ItemTypeHours fromTime,
      ItemTypeDate toDate, boolean mayAlsoBeEqual) {
    mustBeAfterTime = fromTime;
    beforeDate = fromDate;
    myDate = toDate;
    mustBeCanBeEqual = mayAlsoBeEqual;
  }

  // @override
  @Override
  public void actionEvent(AWTEvent e) {
    if (e != null && e instanceof KeyEvent && e.getID() == KeyEvent.KEY_PRESSED) {
      if (!isSet()) {
        value.setTime(referenceTime);
      }
      switch (((KeyEvent) e).getKeyCode()) {
        case KeyEvent.VK_PLUS:
        case KeyEvent.VK_ADD:
        case KeyEvent.VK_UP:
        case KeyEvent.VK_KP_UP:
          value.add((withSeconds ? 1 : 60));
          showValue();
          break;
        case KeyEvent.VK_MINUS:
        case KeyEvent.VK_SUBTRACT:
        case KeyEvent.VK_DOWN:
        case KeyEvent.VK_KP_DOWN:
          value.add((withSeconds ? -1 : -60));
          showValue();
          break;
      }
    }
    super.actionEvent(e);
  }

}
