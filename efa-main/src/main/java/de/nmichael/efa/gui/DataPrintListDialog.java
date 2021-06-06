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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.gui.util.TableItemHeader;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.HtmlFactory;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;
import de.nmichael.efa.util.Mnemonics;

public class DataPrintListDialog extends BaseDialog {

  /**
   *
   */
  private static final long serialVersionUID = 1L;
  private JScrollPane selectedFieldsScrollPane;
  private JList selectedFields;

  private StorageObject persistence;
  private long validAt;
  private AdminRecord admin;
  private Vector<DataRecord> data;
  private String[] fields;
  private String[] fieldDescription;
  private int[] selectedIndices;

  public DataPrintListDialog(Frame parent, StorageObject persistence, long validAt,
      AdminRecord admin,
      Vector<DataRecord> data) {
    super(parent, International.getMessage("{data} ausgeben", persistence.getDescription()),
        International.getStringWithMnemonic("Liste ausgeben"));
    setPersistence(persistence, validAt, admin, data);
  }

  public DataPrintListDialog(JDialog parent, StorageObject persistence, long validAt,
      AdminRecord admin,
      Vector<DataRecord> data) {
    super(parent, International.getMessage("{data} ausgeben", persistence.getDescription()),
        International.getStringWithMnemonic("Liste ausgeben"));
    setPersistence(persistence, validAt, admin, data);
  }

  public void setPersistence(StorageObject persistence, long validAt, AdminRecord admin,
      Vector<DataRecord> data) {
    this.persistence = persistence;
    this.validAt = validAt;
    this.admin = admin;
    this.data = data;
    this.fields = persistence.createNewRecord().getFieldNamesForTextExport(true);
    this.fieldDescription = new String[fields.length];
    Vector<Integer> indices = new Vector<Integer>();
    Vector<IItemType> items = persistence.createNewRecord().getGuiItems(admin);
    TableItemHeader[] defaultSelection = persistence.createNewRecord().getGuiTableHeader();
    for (int i = 0; i < fields.length; i++) {
      IItemType item = null;
      for (int j = 0; items != null && j < items.size(); j++) {
        if (items.get(j).getName().equals(fields[i])) {
          item = items.get(j);
          break;
        }
      }
      fieldDescription[i] = (item != null ? item.getDescription() : fields[i]);
      for (TableItemHeader element : defaultSelection) {
        if (element.toString().equals(fieldDescription[i])) {
          indices.add(i);
          break;
        }
      }
    }
    selectedIndices = new int[indices.size()];
    for (int i = 0; i < selectedIndices.length; i++) {
      selectedIndices[i] = indices.get(i);
    }
  }

  @Override
  protected void iniDialog() throws Exception {
    mainPanel.setLayout(new BorderLayout());

    JPanel mainControlPanel = new JPanel();
    mainControlPanel.setLayout(new GridBagLayout());
    JLabel fieldsLabel = new JLabel();
    Mnemonics.setLabel(this, fieldsLabel, International.getString("ausgewählte Felder") + ":");
    mainControlPanel.add(fieldsLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
        GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(10, 0, 0, 0), 0, 0));
    mainPanel.add(mainControlPanel, BorderLayout.NORTH);

    selectedFields = new JList();
    selectedFields.setListData(fieldDescription);
    selectedFields.setSelectedIndices(selectedIndices);
    selectedFieldsScrollPane = new JScrollPane();
    selectedFieldsScrollPane.setPreferredSize(new Dimension(300, 200));
    selectedFieldsScrollPane.getViewport().add(selectedFields);
    mainPanel.add(selectedFieldsScrollPane, BorderLayout.CENTER);

    closeButton.setIcon(getIcon(IMAGE_RUNEXPORT));

  }

  @Override
  public void keyAction(ActionEvent evt) {
    _keyAction(evt);
  }

  @Override
  public void closeButton_actionPerformed(ActionEvent e) {
    int[] indices = selectedFields.getSelectedIndices();
    String[] selectedFieldNames = new String[indices.length];
    String[] selectedFieldDescriptions = new String[indices.length];
    for (int i = 0; i < indices.length; i++) {
      selectedFieldNames[i] = fields[indices[i]];
      selectedFieldDescriptions[i] = fieldDescription[indices[i]];
    }

    if (selectedFieldNames.length == 0) {
      Dialog.error(International.getString("Keine Felder selektiert."));
      return;
    }

    String filename = Daten.efaTmpDirectory
        + persistence.data().getStorageObjectType() + "_"
        + persistence.data().getStorageObjectName()
        + ".html";
    if (printItems(filename, selectedFieldNames, selectedFieldDescriptions)) {
      BrowserDialog.openInternalBrowser(this, "file:" + filename);
      EfaUtil.deleteFile(filename);
    }

  }

  private boolean printItems(String filename, String[] fields, String[] descriptions) {
    try {
      BufferedWriter fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename,
          false), Daten.ENCODING_UTF));
      HtmlFactory.writeHeader(fw, persistence.getDescription(), true);

      fw.write("<table border align=\"center\">");

      fw.write("<tr>");
      for (String header : descriptions) {
        fw.write("<th>" + EfaUtil.escapeXml(header) + "</th>");
      }
      fw.write("</tr>");

      for (DataRecord r : data) {
        fw.write("<tr>");
        for (String field : fields) {
          String value = r.getAsText(field);
          fw.write("<td>" +
              (value != null && value.length() > 0 ? EfaUtil.escapeXml(value) : "&nbsp;") +
              "</td>");
        }
        fw.write("</tr>\n");
      }
      fw.write("</table>");
      HtmlFactory.writeFooter(fw);
      fw.close();
    } catch (Exception e) {
      Logger.logdebug(e);
      Dialog.error(e.toString());
      return false;
    }
    return true;
  }

}
