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
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JTextField;

import de.nmichael.efa.Daten;
import de.nmichael.efa.util.EfaUtil;

// @i18n complete

public abstract class ItemTypeLabelTextfield extends ItemTypeLabelValue {

  @Override
  protected JComponent initializeField() {
    JTextField f = new JTextField();
    return f;
  }

  @Override
  public String getValueFromField() {
    if (field != null) {
      return ((JTextField) field).getText();
    } else {
      return toString(); // otherwise a hidden field in expert mode might return null
    }
  }

  @Override
  public void showValue() {
    super.showValue();
    if (field != null) {
      ((JTextField) field).setText(toString());
      ((JTextField) field).setCaretPosition(0); // hopefully setting the caret to 0 doesn't break
      // anything?!
      // ((JTextField)field).selectAll(); @todo - change Velten (replace setCaretPosition(0) by
      // selectAll() - why?
    }
  }

  @Override
  protected void iniDisplay() {
    super.iniDisplay();
    JTextField f = (JTextField) field;
    f.setEditable(isEditable);
    f.setDisabledTextColor(Color.black);
    f.setEnabled(isEnabled && isEditable);
    if (fieldColor != null) {
      f.setForeground(fieldColor);
      if (!(isEnabled && isEditable)) {
        f.setDisabledTextColor(fieldColor);
      }
    }
    f.addKeyListener(new java.awt.event.KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        actionEvent(e);
      }

      @Override
      public void keyReleased(KeyEvent e) {
        actionEvent(e);
      }
    });
  }

  public void setSelection(int beginIndex, int endIndex) {
    if (field == null) {
      return;
    }
    JTextField f = (JTextField) field;
    if (endIndex > f.getText().length()) {
      endIndex = f.getText().length();
    }
    if (beginIndex < 0 || beginIndex > endIndex) {
      f.select(0, 0);
    } else {
      f.select(beginIndex, endIndex);
    }
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    try {
      if (field != null && Daten.lookAndFeel.equals("Metal")) {
        ((JTextField) field).setDisabledTextColor(Color.darkGray);
        ((JTextField) field).setBackground((enabled ? (new JTextField()).getBackground()
            : new Color(234, 234, 234)));
      }
    } catch (Exception e) {
      EfaUtil.foo();
    }
  }

  @Override
  public void setEditable(boolean editable) {
    super.setEditable(editable);
    if (field != null) {
      ((JTextField) field).setEditable(isEditable);
      ((JTextField) field).setEnabled(isEnabled && isEditable);
    }
  }
}
