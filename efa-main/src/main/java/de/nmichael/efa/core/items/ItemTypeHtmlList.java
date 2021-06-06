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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.util.Hashtable;

import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;

import de.nmichael.efa.gui.util.EfaMouseListener;

// @i18n complete

public class ItemTypeHtmlList extends ItemType implements ActionListener {

  protected String value;

  protected JList list;
  protected JScrollPane scrollPane;
  protected EfaMouseListener mouseListener;
  protected JPopupMenu popup;
  protected String[] keys;
  protected Hashtable<String, String> items;
  protected String[] popupActions;

  public ItemTypeHtmlList(String name, String[] keys, Hashtable<String, String> items,
      String value,
      int type, String category, String description) {
    this.name = name;
    this.keys = keys;
    this.items = items;
    this.value = value;
    this.type = type;
    this.category = category;
    this.description = description;
    fieldWidth = 600;
    fieldHeight = 300;
    fieldGridAnchor = GridBagConstraints.CENTER;
    fieldGridFill = GridBagConstraints.NONE;
  }

  @Override
  public IItemType copyOf() {
    return new ItemTypeHtmlList(name, keys.clone(), (Hashtable<String, String>) items.clone(),
        value, type, category, description);
  }

  @Override
  public void showValue() {
    if (list != null) {
      if (keys != null && items != null) {
        String[] elements = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
          StringBuffer s = new StringBuffer();
          s.append(items.get(keys[i]));
          elements[i] = s.toString();
        }
        list.setListData(elements);
      } else {
        list.setListData(new Object[0]);
      }
      for (int i = 0; keys != null && value != null && i < keys.length; i++) {
        if (value.equals(keys[i])) {
          list.setSelectedIndex(i);
          list.scrollRectToVisible(list.getCellBounds(i, i));
          break;
        }
      }
    }
  }

  @Override
  protected void iniDisplay() {
    list = new JList();
    list.setCellRenderer(new MyCellRenderer());
    scrollPane = new JScrollPane();
    scrollPane.setPreferredSize(new Dimension(fieldWidth, fieldHeight));
    scrollPane.setMinimumSize(new Dimension(fieldWidth, fieldHeight));
    scrollPane.getViewport().add(list, null);

    if (popupActions != null) {
      popup = new JPopupMenu();
      for (int i = 0; i < popupActions.length; i++) {
        JMenuItem menuItem = new JMenuItem(popupActions[i]);
        menuItem.setActionCommand(EfaMouseListener.EVENT_POPUP_CLICKED + "_" + i);
        menuItem.addActionListener(this);
        popup.add(menuItem);
      }
    } else {
      popup = null;
    }
    list.addMouseListener(mouseListener = new EfaMouseListener(list, popup, this, false));
    list.addFocusListener(new java.awt.event.FocusAdapter() {
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
    this.field = list;
  }

  @Override
  public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
    this.dlg = dlg;
    iniDisplay();
    panel.add(scrollPane, new GridBagConstraints(x, y, fieldGridWidth, fieldGridHeight, 0.0, 0.0,
        fieldGridAnchor, fieldGridFill, new Insets(padYbefore, padXbefore, padYafter, 0), 0, 0));
    return 1;
  }

  public int displayOnGui(Window dlg, JPanel panel, String borderLayoutPosition) {
    this.dlg = dlg;
    iniDisplay();
    panel.add(field, borderLayoutPosition);
    return 1;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    actionEvent(e);
  }

  public void setValues(String[] keys, Hashtable<String, String> items) {
    this.keys = keys;
    this.items = items;
    showValue();
  }

  @Override
  public void parseValue(String value) {
    if (value != null) {
      value = value.trim();
    }
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  @Override
  public void getValueFromGui() {
    if (list != null && keys != null && list.getSelectedIndex() >= 0) {
      value = keys[list.getSelectedIndex()];
    }
  }

  @Override
  public String getValueFromField() {
    if (list != null && keys != null && list.getSelectedIndex() >= 0) {
      return keys[list.getSelectedIndex()];
    }
    return toString(); // otherwise a hidden field in expert mode might return null
  }

  @Override
  public boolean isValidInput() {
    return true;
  }

  public void setPopupActions(String[] actions) {
    this.popupActions = actions;
  }

  class MyCellRenderer extends JEditorPane implements ListCellRenderer {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Component getListCellRendererComponent(
        JList list, // the list
        Object value, // value to display
        int index, // cell index
        boolean isSelected, // is the cell selected
        boolean cellHasFocus) // does the cell have focus
    {
      String s = value.toString();
      setContentType("text/html");
      setText(s);
      if (isSelected) {
        setBackground(list.getSelectionBackground());
        setForeground(list.getSelectionForeground());
      } else {
        setBackground(list.getBackground());
        setForeground(list.getForeground());
      }
      setEnabled(list.isEnabled());
      setFont(list.getFont());
      setOpaque(true);
      return this;
    }
  }

  @Override
  public void setVisible(boolean visible) {
    list.setVisible(visible);
    scrollPane.setVisible(visible);
    super.setVisible(visible);
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    list.setEnabled(enabled);
    scrollPane.setEnabled(enabled);
  }

}
