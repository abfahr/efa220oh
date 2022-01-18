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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

// @i18n complete

public class ItemTypeRadioButtons extends ItemTypeLabelValue {

  private String value;
  private String[] valueList;
  private String[] displayList;
  private JRadioButton[] buttons;
  private volatile boolean ignoreItemStateChanges = false;

  public ItemTypeRadioButtons(String name, String value,
      String[] valueList, String[] displayList,
      int type, String category, String description) {
    this.name = name;
    this.value = value;
    this.valueList = valueList;
    this.displayList = displayList;
    this.type = type;
    this.category = category;
    this.description = description;
  }

  @Override
  public IItemType copyOf() {
    return new ItemTypeRadioButtons(name, value, valueList.clone(), displayList.clone(), type,
        category, description);
  }

  @Override
  protected JComponent initializeField() {
    JPanel groupPanel = new JPanel();
    groupPanel.setLayout(new GridBagLayout());
    ButtonGroup group = new ButtonGroup();
    if (displayList != null) {
      buttons = new JRadioButton[displayList.length];
    }
    for (int i = 0; displayList != null && i < displayList.length; i++) {
      JRadioButton b = new JRadioButton();
      b.setText(displayList[i]);
      group.add(b);
      groupPanel.add(b, new GridBagConstraints(i, 0, 1, 1, 0.0, 0.0,
          GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, (i > 0 ? 10 : 0), 0, 0),
          0, 0));
      b.addItemListener(new java.awt.event.ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
          actionEvent(e);
        }
      });
      buttons[i] = b;
    }
    showValue();
    return groupPanel;
  }

  @Override
  public String getValueFromField() {
    if (buttons != null) {
      for (int i = 0; i < buttons.length; i++) {
        if (buttons[i].isSelected()) {
          return valueList[i];
        }
      }
    }
    return toString(); // otherwise a hidden field in expert mode might return null
  }

  @Override
  public void showValue() {
    super.showValue();
    for (int i = 0; valueList != null && value != null && buttons != null && i < valueList.length; i++) {
      if (value.equals(valueList[i])) {
        ignoreItemStateChanges = true;
        try {
          buttons[i].setSelected(true);
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

}
