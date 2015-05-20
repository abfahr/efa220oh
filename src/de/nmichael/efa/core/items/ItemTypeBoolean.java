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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;

import de.nmichael.efa.util.Logger;
import de.nmichael.efa.util.Mnemonics;

// @i18n complete

public class ItemTypeBoolean extends ItemType {

  private boolean value;
  private boolean useRadioButton = false;
  private ButtonGroup buttonGroup;

  public ItemTypeBoolean(String name, boolean value, int type,
      String category, String description) {
    this.name = name;
    this.value = value;
    this.type = type;
    this.category = category;
    this.description = description;
    this.fieldGridWidth = 3;
  }

  @Override
  public IItemType copyOf() {
    return new ItemTypeBoolean(name, value, type, category, description);
  }

  @Override
  public void parseValue(String value) {
    if (value != null) {
      value = value.trim();
    }
    try {
      this.value = Boolean.parseBoolean(value);
    } catch (Exception e) {
      Logger.log(Logger.ERROR, Logger.MSG_CORE_UNSUPPORTEDDATATYPE,
          "Invalid value for parameter " + name + ": " + value);
    }
  }

  @Override
  public String toString() {
    return Boolean.toString(value);
  }

  @Override
  protected void iniDisplay() {
    JToggleButton checkbox = (!useRadioButton ? new JCheckBox() : new JRadioButton());
    if (useRadioButton && buttonGroup != null) {
      buttonGroup.add(checkbox);
    }
    Mnemonics.setButton(dlg, checkbox, getDescription());
    checkbox.setSelected(value);
    if (type == IItemType.TYPE_EXPERT) {
      checkbox.setForeground(Color.red);
    }
    if (color != null) {
      checkbox.setForeground(color);
    }
    checkbox.setEnabled(isEnabled);
    checkbox.setVisible(isVisible);
    this.field = checkbox;
    checkbox.addActionListener(new java.awt.event.ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        actionEvent(e);
      }
    });
  }

  @Override
  public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
    this.dlg = dlg;
    iniDisplay();
    panel.add(field, new GridBagConstraints(x, y, fieldGridWidth, fieldGridHeight, 0.0, 0.0,
        fieldGridAnchor, fieldGridFill, new Insets(padYbefore, padXbefore, padYafter, padXafter),
        0, 0));
    return 1;
  }

  @Override
  public void getValueFromGui() {
    if (field != null) {
      value = ((JToggleButton) field).isSelected();
    }
  }

  @Override
  public String getValueFromField() {
    if (field != null) {
      return Boolean.toString(((JToggleButton) field).isSelected());
    } else {
      return toString(); // otherwise a hidden field in expert mode might return null
    }
  }

  @Override
  public void showValue() {
    if (field != null) {
      ((JToggleButton) field).setSelected(value);
    }
  }

  public boolean getValue() {
    return value;
  }

  public void setValue(boolean value) {
    this.value = value;
    showValue();
  }

  @Override
  public boolean isValidInput() {
    return true;
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (field != null) {
      field.setVisible(isVisible);
    }
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    if (field != null) {
      field.setEnabled(enabled);
    }
  }

  public void setUseRadioButton(boolean useRadioButton, ButtonGroup buttonGroup) {
    this.useRadioButton = useRadioButton;
    this.buttonGroup = buttonGroup;
  }

  @Override
  public void setDescription(String s) {
    super.setDescription(s);
    if (field != null) {
      Mnemonics.setButton(dlg, (JCheckBox) field, getDescription());
    }
  }
}
