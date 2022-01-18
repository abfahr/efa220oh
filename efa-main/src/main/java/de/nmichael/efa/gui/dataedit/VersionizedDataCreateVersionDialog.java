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

import java.awt.AWTEvent;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.swing.JDialog;

import de.nmichael.efa.core.items.IItemListener;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeDateTime;
import de.nmichael.efa.core.items.ItemTypeLabel;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;

// @i18n complete
public class VersionizedDataCreateVersionDialog extends BaseDialog implements IItemListener {

  /**
   *
   */
  private static final long serialVersionUID = 1L;
  private DataRecord dataRecord;
  private int versionId;
  private ItemTypeDateTime validFrom;
  private long validFromResult = -1;

  public VersionizedDataCreateVersionDialog(Frame parent, DataRecord r, int versionId) {
    super(parent, International.getString("Neue Version erstellen"), International
        .getStringWithMnemonic("Version erstellen"));
    this.dataRecord = r;
    this.versionId = versionId;
  }

  public VersionizedDataCreateVersionDialog(JDialog parent, DataRecord r, int versionId) {
    super(parent, International.getString("Neue Version erstellen"), International
        .getStringWithMnemonic("Version erstellen"));
    this.dataRecord = r;
    this.versionId = versionId;
  }

  @Override
  public void keyAction(ActionEvent evt) {
    _keyAction(evt);
  }

  @Override
  protected void iniDialog() throws Exception {
    // create GUI items
    mainPanel.setLayout(new GridBagLayout());

    ItemTypeLabel label1 = new ItemTypeLabel("LABEL1", IItemType.TYPE_PUBLIC, "",
        International.getMessage("als Kopie von Version {version}", versionId) +
        " (" + International.getString("gültig") + " " + dataRecord.getValidRangeString() + ")");
    label1.setPadding(0, 0, 0, 10);
    label1.displayOnGui(this, mainPanel, 0, 0);

    long today = DataTypeDate.today().getTimestamp(new DataTypeTime(0, 0, 0));
    long validFromTs = (today > dataRecord.getValidFrom() && today < dataRecord.getInvalidFrom() ? today
        : dataRecord.getValidFrom());
    validFrom = new ItemTypeDateTime("VALID_FROM",
        (validFromTs == 0 ? null : new DataTypeDate(validFromTs)),
        (validFromTs == 0 ? null : new DataTypeTime(validFromTs)),
        IItemType.TYPE_PUBLIC, "", International.getString("Neue Version gültig ab"));
    DataTypeDate rDate = new DataTypeDate(validFromTs);
    rDate.setDay(1);
    rDate.setMonth(1);
    validFrom.setReferenceDate(rDate, new DataTypeTime(0, 0, 0));
    validFrom.registerItemListener(this);
    validFrom.displayOnGui(this, mainPanel, 0, 1);
    closeButton.setIcon(getIcon(BaseDialog.IMAGE_ACCEPT));
    closeButton.setIconTextGap(10);
    validFrom.requestFocus();
  }

  boolean checkValidFrom() {
    validFrom.getValueFromGui();
    long validFromTs = (validFrom.isSet() ? validFrom.getTimeStamp() : 0);
    boolean ok = validFromTs > dataRecord.getValidFrom()
        && validFromTs < dataRecord.getInvalidFrom();
    if (!ok) {
      if (validFromTs <= dataRecord.getValidFrom()) {
        Dialog.error(International.getMessage(
            "Der Beginn des Zeitraums muß nach {timestamp} liegen.",
            dataRecord.getValidFromTimeString()));
      } else {
        if (validFromTs >= dataRecord.getInvalidFrom()) {
          Dialog
          .error(International
              .getString("Die aktuelle Version ist zum gewählten Zeitraum bereits ungültig. "
                  +
                  "Bitte wähle eine andere Version, oder verlängere zuerst den Gültigkeitszeitraum der aktuellen Version."));
        } else {
          Dialog.error(International.getMessage(
              "Das Ende des Zeitraums darf nicht nach {timestamp} liegen.",
              dataRecord.getValidUntilTimeString()));
        }
      }
      validFrom.requestFocus();
    }
    return ok;
  }

  @Override
  public void itemListenerAction(IItemType itemType, AWTEvent event) {}

  @Override
  public void closeButton_actionPerformed(ActionEvent e) {
    if (!checkValidFrom()) {
      return;
    }
    this.validFromResult = (validFrom.isSet() ? validFrom.getTimeStamp() : 0);
    super.closeButton_actionPerformed(e);
  }

  public long getValidFromResult() {
    return validFromResult;
  }

}
