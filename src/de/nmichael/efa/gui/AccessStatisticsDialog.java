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
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.Hashtable;

import javax.swing.JDialog;

import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeTable;
import de.nmichael.efa.data.storage.AccessStatistics;
import de.nmichael.efa.gui.util.TableItem;
import de.nmichael.efa.util.International;

public class AccessStatisticsDialog extends BaseDialog {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public AccessStatisticsDialog(Frame parent) {
    super(parent, International.getStringWithMnemonic("Zugriffsstatisik"),
        International.getStringWithMnemonic("Schließen"));
  }

  public AccessStatisticsDialog(JDialog parent) {
    super(parent, International.getStringWithMnemonic("Zugriffsstatisik"),
        International.getStringWithMnemonic("Schließen"));
  }

  @Override
  protected void iniDialog() throws Exception {
    mainPanel.setLayout(new BorderLayout());
    /*
     * JScrollPane scroll = new JScrollPane(); scroll.setPreferredSize(new Dimension(600,400));
     * JTextArea out = new JTextArea(); scroll.getViewport().add(out, null); mainPanel.add(scroll,
     * BorderLayout.NORTH); out.setFont(new Font( "Monospaced", Font.PLAIN, 12 ));
     * out.setText(AccessStatistics.getStatisticsAsString());
     */
    String[] header = new String[] {
        "Object", "Operation", "Counter", "Value"
    };
    Hashtable<String, TableItem[]> items = new Hashtable<String, TableItem[]>();
    String[][] data = AccessStatistics.getStatisticsAsArray();
    for (String[] fields : data) {
      String key = AccessStatistics.getKey(fields);
      TableItem[] ti = new TableItem[fields.length];
      for (int j = 0; j < fields.length; j++) {
        ti[j] = new TableItem(fields[j]);
      }
      items.put(key, ti);
    }
    ItemTypeTable table = new ItemTypeTable("", header, items,
        "", IItemType.TYPE_PUBLIC, "", "Access Statistics");
    table.displayOnGui(this, mainPanel, BorderLayout.CENTER);
  }

  @Override
  public void keyAction(ActionEvent evt) {
    _keyAction(evt);
  }

}
