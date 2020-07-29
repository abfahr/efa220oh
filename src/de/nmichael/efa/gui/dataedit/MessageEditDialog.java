/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui.dataedit;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.UUID;

import javax.swing.JDialog;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeBoolean;
import de.nmichael.efa.core.items.ItemTypeStringAutoComplete;
import de.nmichael.efa.data.MessageRecord;
import de.nmichael.efa.data.PersonRecord;
import de.nmichael.efa.data.Persons;
import de.nmichael.efa.ex.InvalidValueException;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

// @i18n complete
public class MessageEditDialog extends UnversionizedDataEditDialog {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public MessageEditDialog(Frame parent, MessageRecord r, boolean newRecord, AdminRecord admin) {
    super(parent, International.getString("Nachricht"), r, newRecord, admin);
    ini(admin);
  }

  public MessageEditDialog(JDialog parent, MessageRecord r, boolean newRecord, AdminRecord admin) {
    super(parent, International.getString("Nachricht"), r, newRecord, admin);
    ini(admin);
  }

  @Override
  public void keyAction(ActionEvent evt) {
    _keyAction(evt);
  }

  private void ini(AdminRecord admin) {
    ItemTypeBoolean msgRead = (ItemTypeBoolean) getItem(MessageRecord.READ);
    boolean setMsgRead = false;
    MessageRecord r = null;
    if (msgRead != null && !msgRead.getValue()) {
      r = (MessageRecord) dataRecord;
      if (r.getTo() == null || r.getTo().equals(MessageRecord.TO_ADMIN)) {
        msgRead.setEnabled(admin != null && admin.isAllowedMsgMarkReadAdmin());
        if (admin != null && admin.isAllowedMsgAutoMarkReadAdmin()) {
          setMsgRead = true;
        }
      } else {
        msgRead.setEnabled(admin != null && admin.isAllowedMsgMarkReadBoatMaintenance());
        if (admin != null && admin.isAllowedMsgAutoMarkReadBoatMaintenance()) {
          setMsgRead = true;
        }
      }
    }
    if (setMsgRead && r != null) {
      msgRead.setValue(true);
      msgRead.showValue();
      try {
        r.setRead(true);
        r.getPersistence().data().update(r);
        msgRead.setUnchanged();
      } catch (Exception eignore) {
        Logger.logdebug(eignore);
      }

    }
  }

  @Override
  public void updateGui() {
    super.updateGui();
    if (newRecord && getItem(MessageRecord.FROM) != null) {
      this.setRequestFocus(getItem(MessageRecord.FROM));
    }
  }

  @Override
  public void showDialog() {
    if (newRecord && getItem(MessageRecord.FROM) != null) {
      this.setRequestFocus(getItem(MessageRecord.FROM));
    }
    super.showDialog();
  }

  @Override
  protected boolean saveRecord() throws InvalidValueException {
    if (newRecord && dataRecord != null) {
      IItemType item = getItem(MessageRecord.TO);
      if (item != null) {
        String to = item.getValueFromField();
        MessageRecord msgRecord = (MessageRecord) dataRecord;
        if (Daten.efaConfig.getValueNotificationMarkReadAdmin()
            && MessageRecord.TO_ADMIN.equals(to)) {
          msgRecord.setRead(true);
        }
        if (Daten.efaConfig.getValueNotificationMarkReadBoatMaintenance()
            && MessageRecord.TO_BOATMAINTENANCE.equals(to)) {
          msgRecord.setRead(true);
        }

        try {
          ItemTypeStringAutoComplete from = (ItemTypeStringAutoComplete) getItem(
              MessageRecord.FROM);
          Persons persons = Daten.project.getPersons(false);
          PersonRecord p = persons.getPerson(
              (UUID) from.getId(from.getValueFromField()), System.currentTimeMillis());
          if (p != null && p.getEmail() != null) {
            String personEmail = p.getEmail().trim();
            if (personEmail.length() > 0) {
              msgRecord.setReplyTo(personEmail);
              msgRecord.setTo(personEmail);
            }
          }
        } catch (Exception e) {
          Logger.logdebug(e);
        }

      }
    }
    return super.saveRecord();
  }

}
