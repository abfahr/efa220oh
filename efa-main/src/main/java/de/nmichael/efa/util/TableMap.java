/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

/**
 * In a chain of data manipulators some behaviour is common. TableMap
 * provides most of this behaviour and can be subclassed by filters
 * that only need to override a handful of specific methods. TableMap
 * implements TableModel by routing all requests to its model, and
 * TableModelListener by routing all events to its listeners. Inserting
 * a TableMap which has not been subclassed into a chain of table filters
 * should have no effect.
 *
 * @version 1.4 12/17/97
 * @author Philip Milne */

package de.nmichael.efa.util;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

// @i18n complete

public class TableMap extends AbstractTableModel
implements TableModelListener {
  /**
   *
   */
  private static final long serialVersionUID = 1L;
  protected TableModel model;

  public TableModel getModel() {
    return model;
  }

  public void setModel(TableModel model) {
    this.model = model;
    model.addTableModelListener(this);
  }

  // By default, implement TableModel by forwarding all messages
  // to the model.

  @Override
  public Object getValueAt(int aRow, int aColumn) {
    return model.getValueAt(aRow, aColumn);
  }

  @Override
  public void setValueAt(Object aValue, int aRow, int aColumn) {
    model.setValueAt(aValue, aRow, aColumn);
  }

  @Override
  public int getRowCount() {
    return (model == null) ? 0 : model.getRowCount();
  }

  @Override
  public int getColumnCount() {
    return (model == null) ? 0 : model.getColumnCount();
  }

  @Override
  public String getColumnName(int aColumn) {
    return model.getColumnName(aColumn);
  }

  @Override
  public Class getColumnClass(int aColumn) {
    return model.getColumnClass(aColumn);
  }

  @Override
  public boolean isCellEditable(int row, int column) {
    return model.isCellEditable(row, column);
  }

  //
  // Implementation of the TableModelListener interface,
  //
  // By default forward all events to all the listeners.
  @Override
  public void tableChanged(TableModelEvent e) {
    fireTableChanged(e);
  }
}
