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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.types.DataTypeList;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.Mnemonics;

public class ItemTypeMultiSelectCheckboxes<T> extends ItemType implements ActionListener {

  DataTypeList<T> value;
  JPanel mypanel;
  JPanel checkboxPanel;
  JLabel label;
  JCheckBox[] checkboxes;
  T[] keyData;
  String[] displayData;
  int xoffset = 0;
  int yoffset = 0;

  public ItemTypeMultiSelectCheckboxes(String name, DataTypeList<T> value, T[] keyData,
      String[] displayData,
      int type, String category, String description) {
    this.name = name;
    this.value = value;
    this.type = type;
    this.category = category;
    this.description = description;
    this.keyData = keyData;
    this.displayData = displayData;
    fieldWidth = 300;
    fieldHeight = 120;
  }

  @Override
  public ItemTypeMultiSelectCheckboxes copyOf() {
    DataTypeList<T> valueCopy = (value != null ? new DataTypeList<T>(value) : null);
    return new ItemTypeMultiSelectCheckboxes(name, valueCopy,
        keyData.clone(), displayData.clone(), type, category, description);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (listener != null) {
      listener.itemListenerAction(this, e);
    }
  }

  @Override
  protected void iniDisplay() {
    // not used, everything done in displayOnGui(...)
  }

  @Override
  public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
    panel.add(setupPanel(dlg), new GridBagConstraints(x + xoffset, y + yoffset, fieldGridWidth,
        fieldGridHeight, 0.0, 0.0,
        fieldGridAnchor, fieldGridFill, new Insets(padYbefore, 0, padYafter, padXafter), 0, 0));
    showValue();
    return 1;
  }

  private JPanel setupPanel(Window dlg) {
    this.dlg = dlg;

    mypanel = new JPanel(new BorderLayout());
    checkboxPanel = new JPanel(new GridLayout(0, 2));
    checkboxes = new JCheckBox[keyData.length];

    if (getDescription() != null) {
      label = new JLabel();
      Mnemonics.setLabel(dlg, label, getDescription() + ": ");
      label.setHorizontalAlignment(SwingConstants.LEFT);
      if (type == IItemType.TYPE_EXPERT) {
        label.setForeground(Color.red);
      }
      if (color != null) {
        label.setForeground(color);
      }
      Dialog.setPreferredSize(label, fieldWidth, 20);
      mypanel.add(label, BorderLayout.NORTH);
    }

    for (int i = 0; i < keyData.length; i++) {
      JCheckBox checkbox = new JCheckBox();
      Mnemonics.setButton(dlg, checkbox, displayData[i]);
      checkbox.setEnabled(isEnabled);
      checkbox.setVisible(isVisible);
      checkbox.addActionListener(this);
      checkboxes[i] = checkbox;
      checkboxPanel.add(checkbox);
    }
    mypanel.add(checkboxPanel, BorderLayout.CENTER);
    field = mypanel;
    setEnabled(isEnabled);
    setVisible(isVisible);

    return mypanel;
  }

  public Object[] getSelectedKeys() {
    Vector<Object> keys = new Vector<Object>();
    for (int i = 0; checkboxes != null && i < checkboxes.length; i++) {
      if (checkboxes[i].isSelected()) {
        keys.add(keyData[i]);
      }
    }
    return keys.toArray(new Object[0]);
  }

  @Override
  public boolean isValidInput() {
    return !isNotNullSet() || getValueFromField().length() > 0;
  }

  @Override
  public String getValueFromField() {
    DataTypeList<T> fieldValue = new DataTypeList<T>();
    Object[] values = getSelectedKeys();
    for (int i = 0; values != null && i < values.length; i++) {
      fieldValue.add((T) values[i]);
    }
    return fieldValue.toString();
  }

  @Override
  public void showValue() {
    if (checkboxes == null) {
      return;
    }
    for (int i = 0; i < checkboxes.length; i++) {
      checkboxes[i].setSelected(false);
      for (int j = 0; value != null && j < value.length(); j++) {
        T key = value.get(j);
        if (keyData[i].equals(key)) {
          checkboxes[i].setSelected(true);
          break;
        }
      }
    }
  }

  @Override
  public void getValueFromGui() {
    value = new DataTypeList<T>();
    Object[] values = getSelectedKeys();
    for (int i = 0; values != null && i < values.length; i++) {
      value.add((T) values[i]);
    }
  }

  @Override
  public String toString() {
    if (checkboxes != null) {
      return getValueFromField();
    }
    return (value != null ? value.toString() : "");
  }

  public void setValue(DataTypeList<T> v) {
    this.value = v;
    showValue();
  }

  @Override
  public void parseValue(String value) {
    this.value = DataTypeList.parseList(value, IDataAccess.DATA_STRING);
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
    if (checkboxes != null) {
      for (JCheckBox checkbox : checkboxes) {
        checkbox.setEnabled(enabled);
      }
    }
  }
}
