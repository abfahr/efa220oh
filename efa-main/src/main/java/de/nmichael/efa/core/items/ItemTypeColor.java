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

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JPanel;

import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;

// @i18n complete

public class ItemTypeColor extends ItemTypeLabelValue {

  private Color origButtonColor;
  private JButton butdel;

  private String color;

  public ItemTypeColor(String name, String color,
      int type, String category, String description) {
    this.name = name;
    this.color = color;
    this.type = type;
    this.category = category;
    this.description = description;
  }

  @Override
  public IItemType copyOf() {
    return new ItemTypeColor(name, color, type, category, description);
  }

  @Override
  public void parseValue(String value) {
    this.color = value;
  }

  @Override
  public String toString() {
    return color;
  }

  @Override
  protected JComponent initializeField() {
    JButton f = new JButton();
    return f;
  }

  @Override
  protected void iniDisplay() {
    super.iniDisplay();
    JButton f = (JButton) field;
    origButtonColor = f.getBackground();
    f.setEnabled(isEnabled);
    Dialog.setPreferredSize(f, fieldWidth, fieldHeight);
    f.setText(International.getMessage("{item} auswählen",
        International.getString("Farbe")));
    f.setBackground(EfaUtil.getColorOrGray(color));
    f.addActionListener(new java.awt.event.ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        buttonHit(e);
      }
    });

    butdel = new JButton();
    butdel.setEnabled(isEnabled);
    Dialog.setPreferredSize(butdel, fieldHeight, fieldHeight);
    butdel.setIcon(BaseDialog.getIcon(BaseDialog.IMAGE_DELETE));
    butdel.addActionListener(new java.awt.event.ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        buttonDel(e);
      }
    });
  }

  @Override
  public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
    int count = super.displayOnGui(dlg, panel, x, y);
    panel.add(butdel, new GridBagConstraints(x + fieldGridWidth + 1, y, 1, fieldGridHeight, 0.0,
        0.0,
        fieldGridAnchor, fieldGridFill,
        new Insets((itemOnNewRow ? 0 : padYbefore), (itemOnNewRow ? padXbefore : 0), padYafter,
            padXafter), 0, 0));
    return count;
  }

  @Override
  public void getValueFromGui() {
    if (field != null) {
      color = EfaUtil.getColor(field.getBackground());
    }
  }

  public Color getColor() {
    return EfaUtil.getColor(color);
  }

  @Override
  public String getValueFromField() {
    if (field != null) {
      return EfaUtil.getColor(field.getBackground());
    }
    return color;
  }

  @Override
  public void showValue() {}

  private void buttonHit(ActionEvent e) {
    Color color = JColorChooser.showDialog(dlg,
        International.getMessage("{item} auswählen",
            International.getString("Farbe")),
            field.getBackground());
    if (color != null) {
      field.setBackground(color);
    }
  }

  private void buttonDel(ActionEvent e) {
    this.color = null;
    field.setBackground(origButtonColor);
  }

  @Override
  public boolean isValidInput() {
    return true;
  }

}
