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
import java.awt.event.ItemEvent;

import javax.swing.JComboBox;
import javax.swing.JComponent;

// @i18n complete

public class ItemTypeStringList extends ItemTypeLabelValue {

  private String value;
  private String[] valueList; //indices 1,2,...
  private String[] displayList; //Kinderboote, SUP,...
  private volatile boolean ignoreItemStateChanges = false;

  public ItemTypeStringList(String name, String value,
      String[] valueList, String[] displayList,
      int type, String category, String description) {
    this.name = name;
    this.value = value;
    this.valueList = valueList;
    this.displayList = displayList;
    this.type = type;
    this.category = category;
    this.description = description;
    this.lastValue = (value != null ? value.toString() : null);
  }

  @Override
  public IItemType copyOf() {
    return new ItemTypeStringList(name, value, (valueList != null ? valueList.clone() : null),
        (displayList != null ? displayList.clone() : null), type, category, description);
  }

  @Override
  protected JComponent initializeField() {
    JComboBox<ItemLabelValue> f = new JComboBox();
    for (int i = 0; displayList != null && i < displayList.length; i++) {
      ItemLabelValue item = new ItemLabelValue(valueList[i], displayList[i]);
      f.addItem(item);
    }
    f.addItemListener(new java.awt.event.ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        actionEvent(e);
      }
    });
    f.setVisible(isVisible);
    f.setEnabled(isEnabled);
    showValue();
    return f;
  }

  @Override
  public String getValueFromField() {
    if (field != null) {
      JComboBox c = (JComboBox) field;
      int idx = c.getSelectedIndex();
      if (idx >= 0 && idx < valueList.length) {
        return valueList[idx];
      }
    }
    return toString(); // otherwise a hidden field in expert mode might return null
  }

  @Override
  public void showValue() {
    super.showValue();
    for (int i = 0; valueList != null && value != null && field != null && i < valueList.length; i++) {
      if (value.equals(valueList[i])) {
        ignoreItemStateChanges = true;
        try {
          ((JComboBox) field).setSelectedIndex(i);
        } catch (Exception e) {}
        ignoreItemStateChanges = false;
        return;
      }
    }
  }

  @Override
  public void parseValue(String value) {
    if (value != null) {
      value = value.trim();
    }
    for (int i = 0; valueList != null && i < valueList.length; i++) {
      if (valueList[i].equals(value)) {
        this.value = value;
        return;
      }
    }
  }

  public void setListData(String[] valueList, String[] displayList) {
    this.valueList = valueList;
    this.displayList = displayList;
    ignoreItemStateChanges = true;
    try {
      ((JComboBox) field).removeAllItems();
      for (int i = 0; displayList != null && i < displayList.length; i++) {
        ((JComboBox) field).addItem(displayList[i]);
      }
    } catch (Exception e) {}
    ignoreItemStateChanges = false;
    showValue();
  }

  @Override
  public String toString() {
    return (value != null ? value : "");
  }

  public String getValue() {
    return value;
  }

  @Override
  protected void field_focusLost(FocusEvent e) {
    getValueFromGui();
    showValue();
    super.field_focusLost(e);
  }

  @Override
  public boolean isValidInput() {
    if (isNotNullSet()) {
      if (value == null || value.length() == 0) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void actionEvent(AWTEvent e) {
    if (!ignoreItemStateChanges) {
      super.actionEvent(e);
    }
  }

  public String[] getValueList() {
    return valueList;
  }

  public String[] getDisplayList() {
    return displayList;
  }

  public class ItemLabelValue
  {
    private String value;

    private String label;

    public ItemLabelValue(String value, String label) {
      this.value = value;
      this.label = label;
    }

    public String getLabel() {
      return label;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return label;
    }
  }
}
