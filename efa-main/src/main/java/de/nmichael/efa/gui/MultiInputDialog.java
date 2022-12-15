/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui;

import java.awt.*;
import java.awt.event.ActionEvent;

import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;

// @i18n complete
public class MultiInputDialog extends BaseDialog {

  /**
   *
   */
  private static final long serialVersionUID = 1L;
  private String KEYACTION_ENTER;
  protected IItemType[] items;

  public MultiInputDialog(Window parent, String title, IItemType[] items) {
    super(parent, title, International.getStringWithMnemonic("OK"));
    this.items = items;
  }

  @Override
  public void _keyAction(ActionEvent evt) {
    if (evt.getActionCommand().equals(KEYACTION_ENTER)) {
      closeButton_actionPerformed(evt);
    }
    super._keyAction(evt);
  }

  @Override
  public void keyAction(ActionEvent evt) {
    _keyAction(evt);
  }

  @Override
  protected void iniDialog() throws Exception {
    KEYACTION_ENTER = addKeyAction("ENTER");

    // create GUI items
    mainPanel.setLayout(new GridBagLayout());

    int y = 0;
    for (IItemType item : items) {
      y += item.displayOnGui(this, mainPanel, 0, y);
    }
    this.setRequestFocus(items[0]);
    items[0].requestFocus();

    if (closeButton != null) {
      closeButton.setIcon(getIcon("button_accept.png"));
    }
  }

  @Override
  public void closeButton_actionPerformed(ActionEvent e) {
    for (int i = 0; i < items.length; i++) {
      items[i].getValueFromGui();
      if (!items[i].isValidInput()) {
        Dialog.error(International.getMessage("Ungültige Eingabe im Feld '{field}'",
            items[i].getDescription()));
        items[i].requestFocus();
        return;
      }
    }
    setDialogResult(true);
    super.closeButton_actionPerformed(e);
  }

  public static boolean showInputDialog(Window parent, String title, IItemType[] items) {
    MultiInputDialog dlg = new MultiInputDialog(parent, title, items);
    dlg.showDialog();
    return dlg.resultSuccess;
  }

}
