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
import java.awt.event.MouseEvent;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import de.nmichael.efa.util.EfaUtil;

public class ItemTypeLabel extends ItemType {

  private JLabel[] labels;
  private ImageIcon icon;
  private boolean mouseClickListener = false;

  public ItemTypeLabel(String name, int type,
      String category, String description) {
    this.name = name;
    this.type = type;
    this.category = category;
    this.description = description;
    this.fieldGridWidth = 2;
  }

  @Override
  public IItemType copyOf() {
    return new ItemTypeLabel(name, type, category, description);
  }

  @Override
  public void parseValue(String value) {}

  @Override
  public String toString() {
    return description;
  }

  @Override
  public void setDescription(String s) {
    super.setDescription(s);
    Vector<String> v = EfaUtil.split(description, '\n');
    if (v.size() == 0) {
      v.add("");
    }
    for (int i = 0; i < v.size() && i < labels.length; i++) {
      labels[i].setText(v.get(i));
    }
  }

  @Override
  protected void iniDisplay() {
    Vector<String> v = EfaUtil.split(description, '\n');
    if (v.size() == 0) {
      v.add("");
    }
    labels = new JLabel[v.size()];
    for (int i = 0; i < v.size(); i++) {
      JLabel l = new JLabel();
      l.setText(v.get(i));
      if (i == 0 && icon != null) {
        l.setHorizontalAlignment(SwingConstants.CENTER);
        l.setHorizontalTextPosition(SwingConstants.CENTER);
        l.setIcon(icon);
      }
      if (hAlignment != -1) {
        l.setHorizontalAlignment(hAlignment);
        l.setHorizontalTextPosition(hAlignment);
      }
      if (color != null) {
        l.setForeground(color);
      }
      l.setVisible(isVisible);
      labels[i] = l;
    }
    if (mouseClickListener) {
      addMouseClickListener();
    }
  }

  @Override
  public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
    this.dlg = dlg;
    iniDisplay();
    for (int i = 0; i < labels.length; i++) {
      panel.add(labels[i], new GridBagConstraints(x, y + i, fieldGridWidth, fieldGridHeight, 0.0,
          0.0,
          fieldGridAnchor, fieldGridFill, new Insets((i == 0 ? padYbefore : 0), padXbefore,
              (i + 1 == labels.length ? padYafter : 0), padXafter), 0, 0));
    }
    return labels.length;
  }

  @Override
  public void getValueFromGui() {}

  @Override
  public void requestFocus() {}

  @Override
  public String getValueFromField() {
    return null;
  }

  @Override
  public void showValue() {}

  @Override
  public boolean isValidInput() {
    return true;
  }

  @Override
  public void setVisible(boolean visible) {
    for (int i = 0; labels != null && i < labels.length; i++) {
      labels[i].setVisible(visible);
    }
    isVisible = visible;
  }

  @Override
  public boolean isVisible() {
    return isVisible;
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    for (JLabel label : labels) {
      label.setForeground((enabled ? (new JLabel()).getForeground() : Color.gray));
    }
  }

  @Override
  public boolean isEditable() {
    return false;
  }

  public void setImage(ImageIcon icon) {
    this.icon = icon;
  }

  public void activateMouseClickListener() {
    mouseClickListener = true;
  }

  private void addMouseClickListener() {
    for (JLabel label : labels) {
      label.addMouseListener(new java.awt.event.MouseAdapter() {

        @Override
        public void mouseClicked(MouseEvent e) {
          labelMouseClicked(e);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
          labelMouseEntered(e);
        }

        @Override
        public void mouseExited(MouseEvent e) {
          labelMouseExited(e);
        }
      });

    }
  }

  private void labelMouseClicked(MouseEvent e) {
    actionEvent(e);
  }

  private void labelMouseEntered(MouseEvent e) {
    try {
      JLabel label = (JLabel) e.getSource();
      label.setForeground(Color.red);
    } catch (Exception eignore) {}
  }

  private void labelMouseExited(MouseEvent e) {
    try {
      JLabel label = (JLabel) e.getSource();
      label.setForeground(Color.blue);
    } catch (Exception eignore) {}
  }

}
