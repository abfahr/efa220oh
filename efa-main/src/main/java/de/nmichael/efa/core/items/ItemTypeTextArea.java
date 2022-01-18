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
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.FocusEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Mnemonics;

// @i18n complete

public class ItemTypeTextArea extends ItemType {

  protected String value;
  protected JLabel label;
  protected JScrollPane scrollPane;
  protected int labelGridWidth = 1;
  protected int labelGridAnchor = GridBagConstraints.WEST;
  protected int labelGridFill = GridBagConstraints.NONE;
  protected Font labelFont;
  protected Font fieldFont;
  protected boolean wrap;
  protected int caretPosition = 0;

  public ItemTypeTextArea(String name, String value, int type,
      String category, String description) {
    this.name = name;
    this.value = value;
    this.type = type;
    this.category = category;
    this.description = description;
    this.fieldWidth = 600;
    this.fieldHeight = 300;
    this.fieldGridWidth = 2;
  }

  @Override
  public IItemType copyOf() {
    return new ItemTypeTextArea(name, value, type, category, description);
  }

  @Override
  protected void iniDisplay() {
    if (getDescription() != null) {
      label = new JLabel();
      Mnemonics.setLabel(dlg, label, getDescription() + ": ");
      label.setLabelFor(field);
      if (type == IItemType.TYPE_EXPERT) {
        label.setForeground(Color.red);
      }
      if (color != null) {
        label.setForeground(color);
      }
      labelFont = label.getFont();
    } else {
      labelGridWidth = 0;
    }
    scrollPane = new JScrollPane();
    Dialog.setPreferredSize(scrollPane, fieldWidth, fieldHeight);
    field = new JTextArea();
    ((JTextArea) field).setEditable(isEditable);
    ((JTextArea) field).setDisabledTextColor(Color.black);
    if (wrap) {
      ((JTextArea) field).setWrapStyleWord(true);
      ((JTextArea) field).setLineWrap(true);
    }
    field.setEnabled(isEnabled && isEditable);
    scrollPane.getViewport().add(field, null);
    if (backgroundColor != null) {
      field.setBackground(backgroundColor);
    }
    fieldFont = field.getFont();
    field.addFocusListener(new java.awt.event.FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        field_focusGained(e);
      }

      @Override
      public void focusLost(FocusEvent e) {
        field_focusLost(e);
      }
    });
    showValue();
  }

  @Override
  public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
    this.dlg = dlg;
    iniDisplay();
    if (label != null) {
      panel.add(label, new GridBagConstraints(x, y, labelGridWidth, fieldGridHeight, 0.0, 0.0,
          labelGridAnchor, labelGridFill, new Insets(padYbefore, padXbefore, 0, padXafter), 0, 0));
    }
    panel.add(scrollPane, new GridBagConstraints(x, y + 1, fieldGridWidth, fieldGridHeight, 0.0,
        0.0,
        fieldGridAnchor, fieldGridFill, new Insets(0, padXbefore, padYafter, padXafter), 0, 0));
    if (!isEnabled) {
      setEnabled(isEnabled);
    }
    return 2;
  }

  @Override
  public void parseValue(String value) {
    if (value != null) {
      value = value.trim();
    }
    this.value = value;
  }

  @Override
  public void getValueFromGui() {
    if (field != null) {
      String s = getValueFromField();
      if (s != null) {
        parseValue(s);
      }
    }
  }

  @Override
  protected void field_focusLost(FocusEvent e) {
    getValueFromGui();
    showValue();
    super.field_focusLost(e);
  }

  @Override
  public String toString() {
    return (value != null ? value : "");
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
    showValue();
  }

  public void setLabelGrid(int gridWidth, int gridAnchor, int gridFill) {
    labelGridWidth = gridWidth;
    labelGridAnchor = (gridAnchor != -1 ? gridAnchor : labelGridAnchor);
    labelGridFill = (gridFill != -1 ? gridFill : labelGridFill);
  }

  public Font getLabelFont() {
    return (label != null ? label.getFont() : null);
  }

  public void setLabelFont(Font font) {
    if (label != null) {
      label.setFont(font);
    }
  }

  public void restoreLabelFont() {
    if (label != null) {
      label.setFont(labelFont);
    }
  }

  @Override
  public void setDescription(String s) {
    super.setDescription(s);
    Mnemonics.setLabel(dlg, label, getDescription() + ": ");
  }

  public Font getFieldFont() {
    return field.getFont();
  }

  public void setFieldFont(Font font) {
    field.setFont(font);
  }

  public void restoreFieldFont() {
    field.setFont(fieldFont);
  }

  @Override
  public String getValueFromField() {
    if (field != null) {
      return ((JTextArea) field).getText();
    } else {
      return toString(); // otherwise a hidden field in expert mode might return null
    }
  }

  @Override
  public void showValue() {
    if (field != null) {
      String text = toString();
      ((JTextArea) field).setText(text);
      ((JTextArea) field).setCaretPosition((caretPosition <= text.length() ? caretPosition : text
          .length()));
    }
  }

  public void setCaretPosition(int caretPosition) {
    this.caretPosition = caretPosition;
  }

  @Override
  public boolean isValidInput() {
    if (isNotNullSet()) {
      if (value == null || value.length() == 0) {
        lastInvalidErrorText = International.getString("Feld darf nicht leer sein");
        return false;
      }
    }
    return true;
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    if (label != null) {
      label.setForeground((enabled ? (new JLabel()).getForeground() : Color.gray));
    }
    if (field != null) {
      field.setEnabled(enabled);
    }
  }

  public void setWrap(boolean wrap) {
    this.wrap = wrap;
  }

}
