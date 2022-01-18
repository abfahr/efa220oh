/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui.statistics;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;

import de.nmichael.efa.data.StatisticsRecord;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.gui.util.Table;
import de.nmichael.efa.gui.util.TableItem;
import de.nmichael.efa.gui.util.TableItemHeader;
import de.nmichael.efa.gui.util.TableSorter;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

public class StatisticsTableDialog extends BaseDialog {

  /**
   *
   */
  private static final long serialVersionUID = 1L;
  private StatisticsRecord sr;
  private Table table;

  public StatisticsTableDialog(JDialog parent, StatisticsRecord sr) {
    super(parent, sr.pStatTitle, International.getStringWithMnemonic("Schließen"));
    this.sr = sr;
  }

  @Override
  protected void iniDialog() throws Exception {
    mainPanel.setLayout(new BorderLayout());
    TableItemHeader[] header = null;
    TableItem[][] data = null;
    try {
      BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(
          sr.sOutputFile),
          sr.sOutputEncoding));
      char sep = sr.sOutputCsvSeparator.charAt(0);
      ArrayList<Vector<String>> sdata = new ArrayList<Vector<String>>();

      String s;
      int c = 0;
      while ((s = f.readLine()) != null) {
        if (c++ == 0) {
          // header
          Vector<String> sheader = EfaUtil.split(s, sep);
          header = new TableItemHeader[sheader.size()];
          for (int i = 0; i < sheader.size(); i++) {
            header[i] = new TableItemHeader(sheader.get(i));
          }
        } else {
          // data
          sdata.add(EfaUtil.split(s, sep));
        }
      }
      f.close();

      data = new TableItem[sdata.size()][];
      for (int i = 0; i < sdata.size(); i++) {
        Vector<String> v = sdata.get(i);
        data[i] = new TableItem[v.size()];
        for (int j = 0; j < v.size(); j++) {
          data[i][j] = new TableItem(v.get(j));
          header[j].updateColumnWidth(v.get(j));
        }
      }
    } catch (Exception e) {
      Logger.log(e);
      Dialog.error(e.toString());
    }
    TableSorter sorter = new TableSorter(new DefaultTableModel(data, header));
    table = new Table(this, sorter, null, header, data, true);

    JScrollPane scroll = new JScrollPane();
    scroll.setPreferredSize(new Dimension(Dialog.screenSize.width - 400,
        Dialog.screenSize.height - 200));
    scroll.getViewport().add(table, null);
    mainPanel.add(scroll, BorderLayout.CENTER);
  }

  @Override
  public void keyAction(ActionEvent evt) {
    _keyAction(evt);
  }

}
