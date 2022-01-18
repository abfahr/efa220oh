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
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.awt.event.FocusEvent;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.Border;

import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

// @i18n complete

public abstract class ItemType implements IItemType {

  protected String name;
  protected int type;
  protected String category;
  protected String description;

  protected Window dlg;
  protected JComponent field;
  protected IItemListener listener;
  protected String lastValue;
  protected String lastInvalidErrorText = "";
  protected DataKey dataKey; // no purpose other than storing it inside an ItemType, if needed by
  // external class
  protected Object referenceObject; // just a reference to any user-defined object

  protected Color color = null;
  protected Color savedFgColor = null;
  protected Color backgroundColor = null;
  protected Color savedBkgColor = null;
  protected Color backgroundColorWhenFocused = null;
  protected Border border = null;
  protected int padXbefore = 0;
  protected int padXafter = 0;
  protected int padYbefore = 0;
  protected int padYafter = 0;
  protected boolean notNull = false;
  protected int fieldWidth = 300;
  protected int fieldHeight = 19;
  protected int fieldGridWidth = 1;
  protected int fieldGridHeight = 1;
  protected int fieldGridAnchor = GridBagConstraints.WEST;
  protected int fieldGridFill = GridBagConstraints.NONE;
  protected int hAlignment = -1;
  protected boolean isVisible = true;
  protected boolean isEnabled = true;
  protected boolean isEditable = true;

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public int getType() {
    return type;
  }

  @Override
  public String getCategory() {
    return category;
  }

  @Override
  public void setCategory(String s) {
    category = s;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public void setDescription(String s) {
    description = s;
  }

  @Override
  public void setColor(Color c) {
    this.color = c;
    if (field != null) {
      field.setForeground(c);
    }
  }

  public void saveColor() {
    if (field != null) {
      savedFgColor = field.getForeground();
    } else {
      savedFgColor = Color.black;
    }
  }

  public void restoreColor() {
    if (field != null && savedFgColor != null) {
      field.setForeground(savedFgColor);
    }
  }

  @Override
  public void setBackgroundColor(Color c) {
    this.backgroundColor = c;
    if (field != null) {
      field.setBackground(c);
    }
  }

  @Override
  public void saveBackgroundColor(boolean force) {
    if (field != null && (savedBkgColor == null || force)) {
      savedBkgColor = field.getBackground();
    }
  }

  @Override
  public void restoreBackgroundColor() {
    if (field != null && savedBkgColor != null) {
      field.setBackground(savedBkgColor);
    }
  }

  public void setBackgroundColorWhenFocused(Color color) {
    backgroundColorWhenFocused = color;
  }

  @Override
  public void requestFocus() {
    if (field != null) {
      field.requestFocus();
    }
  }

  @Override
  public boolean hasFocus() {
    return (field != null && field.hasFocus());
  }

  @Override
  public void setPadding(int padXbefore, int padXafter, int padYbefore, int padYafter) {
    this.padXbefore = padXbefore;
    this.padXafter = padXafter;
    this.padYbefore = padYbefore;
    this.padYafter = padYafter;
  }

  @Override
  public void setFieldSize(int width, int height) {
    fieldWidth = (width > 0 ? width : fieldWidth);
    fieldHeight = (height > 0 ? height : fieldHeight);
  }

  @Override
  public void setFieldGrid(int gridWidth, int gridAnchor, int gridFill) {
    if (gridWidth >= 0) {
      fieldGridWidth = gridWidth;
    }
    if (gridAnchor >= 0) {
      fieldGridAnchor = gridAnchor;
    }
    if (gridFill >= 0) {
      fieldGridFill = gridFill;
    }
  }

  public void setBorder(Border border) {
    this.border = border;
    if (field != null) {
      field.setBorder(border);
    }
  }

  @Override
  public void setFieldGrid(int gridWidth, int gridHeight, int gridAnchor, int gridFill) {
    if (gridWidth >= 0) {
      fieldGridWidth = gridWidth;
    }
    if (gridHeight >= 0) {
      fieldGridHeight = gridHeight;
    }
    if (gridAnchor >= 0) {
      fieldGridAnchor = gridAnchor;
    }
    if (gridFill >= 0) {
      fieldGridFill = gridFill;
    }
  }

  public void setHorizontalAlignment(int hAlignment) {
    this.hAlignment = hAlignment;
  }

  protected abstract void iniDisplay();

  @Override
  public int displayOnGui(Window dlg, JPanel panel, int y) {
    return displayOnGui(dlg, panel, 0, y);
  }

  @Override
  public void parseAndShowValue(String value) {
    if (value != null) {
      parseValue(value);
    } else {
      parseValue("");
    }
    showValue();
  }

  @Override
  public void setNotNull(boolean notNull) {
    this.notNull = notNull;
  }

  @Override
  public boolean isNotNullSet() {
    return notNull;
  }

  protected void field_focusGained(FocusEvent e) {
    if (backgroundColorWhenFocused != null) {
      saveBackgroundColor(false);
      setBackgroundColor(backgroundColorWhenFocused);
    }
    actionEvent(e);
  }

  protected void field_focusLost(FocusEvent e) {
    if (backgroundColorWhenFocused != null) {
      restoreBackgroundColor();
    }
    actionEvent(e);
  }

  @Override
  public void setChanged() {
    lastValue = null;
  }

  @Override
  public void setUnchanged() {
    lastValue = toString();
  }

  @Override
  public boolean isChanged() {
    String s = toString();
    if ((s == null || s.length() == 0) && (lastValue == null || lastValue.length() == 0)) {
      return false;
    }
    if (Logger.isTraceOn(Logger.TT_GUI, 9)) {
      if (s != null && !s.equals(lastValue)) {
        Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI,
            getName() + ": old=" + lastValue + "; new=" + s);
      }
    }
    return s != null && !s.equals(lastValue);
  }

  @Override
  public void registerItemListener(IItemListener listener) {
    this.listener = listener;
  }

  @Override
  public void actionEvent(AWTEvent e) {
    if (listener != null && e != null) {
      listener.itemListenerAction(this, e);
    }
  }

  public JDialog getParentDialog() {
    if (dlg != null && dlg instanceof JDialog) {
      return (JDialog) dlg;
    }
    return null;
  }

  public JFrame getParentFrame() {
    if (dlg != null && dlg instanceof JFrame) {
      return (JFrame) dlg;
    }
    return null;
  }

  @Override
  public void setVisible(boolean visible) {
    isVisible = visible;
  }

  @Override
  public boolean isVisible() {
    return isVisible;
  }

  @Override
  public void setEnabled(boolean enabled) {
    isEnabled = enabled;
  }

  @Override
  public boolean isEnabled() {
    return isEnabled;
  }

  @Override
  public void setEditable(boolean editable) {
    isEditable = editable;
  }

  @Override
  public boolean isEditable() {
    return isEditable;
  }

  // methods which allow to store a DataKey inside an IItemType (only for special purposes)
  @Override
  public void setDataKey(DataKey k) {
    this.dataKey = k;
  }

  @Override
  public DataKey getDataKey() {
    return this.dataKey;
  }

  @Override
  public JComponent getComponent() {
    return field;
  }

  @Override
  public void setReferenceObject(Object o) {
    this.referenceObject = o;
  }

  @Override
  public Object getReferenceObject() {
    return referenceObject;
  }

  @Override
  public String getInvalidErrorText() {
    return International.getMessage("Ungültige Eingabe im Feld '{field}'",
        getDescription()) + ": " + lastInvalidErrorText;
  }

}
