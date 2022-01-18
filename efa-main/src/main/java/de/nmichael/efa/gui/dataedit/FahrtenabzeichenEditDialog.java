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

import javax.swing.JDialog;

import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.data.Fahrtenabzeichen;
import de.nmichael.efa.data.FahrtenabzeichenRecord;
import de.nmichael.efa.data.efawett.DRVSignatur;
import de.nmichael.efa.util.International;

// @i18n complete
public class FahrtenabzeichenEditDialog extends UnversionizedDataEditDialog {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public FahrtenabzeichenEditDialog(Frame parent, FahrtenabzeichenRecord r, boolean newRecord,
      AdminRecord admin) {
    super(parent, International.onlyFor("Fahrtenabzeichen", "de"), r, newRecord, admin);
    initialize(r);
  }

  public FahrtenabzeichenEditDialog(JDialog parent, FahrtenabzeichenRecord r, boolean newRecord,
      AdminRecord admin) {
    super(parent, International.onlyFor("Fahrtenabzeichen", "de"), r, newRecord, admin);
    initialize(r);
  }

  @Override
  public void keyAction(ActionEvent evt) {
    _keyAction(evt);
  }

  private void initialize(FahrtenabzeichenRecord r) {
    DRVSignatur sig = (r != null ? r.getDRVSignatur() : null);
    if (sig != null) {
      sig.checkSignature();
      if (sig.getSignatureState() == DRVSignatur.SIG_UNKNOWN_KEY) {
        if (((Fahrtenabzeichen) r.getPersistence()).downloadKey(sig.getKeyName())) {
          r.updateGuiItems();
        }
      }
    }
  }

}
