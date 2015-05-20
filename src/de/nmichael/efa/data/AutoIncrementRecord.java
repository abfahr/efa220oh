/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.data;

import java.util.Vector;

import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.storage.MetaData;
import de.nmichael.efa.gui.util.TableItem;
import de.nmichael.efa.gui.util.TableItemHeader;

// @i18n complete

public class AutoIncrementRecord extends DataRecord {

  // =========================================================================
  // Field Names
  // =========================================================================

  public static final String SEQUENCE = "Sequence";
  public static final String INTVALUE = "IntValue";
  public static final String LONGVALUE = "LongValue";

  public static void initialize() {
    Vector<String> f = new Vector<String>();
    Vector<Integer> t = new Vector<Integer>();

    f.add(SEQUENCE);
    t.add(IDataAccess.DATA_STRING);
    f.add(INTVALUE);
    t.add(IDataAccess.DATA_INTEGER);
    f.add(LONGVALUE);
    t.add(IDataAccess.DATA_LONGINT);
    MetaData metaData = constructMetaData(AutoIncrement.DATATYPE, f, t, false);
    metaData.setKey(new String[] { SEQUENCE });
  }

  public AutoIncrementRecord(AutoIncrement autoIncrement, MetaData metaData) {
    super(autoIncrement, metaData);
  }

  @Override
  public DataRecord createDataRecord() { // used for cloning
    return getPersistence().createNewRecord();
  }

  @Override
  public DataKey getKey() {
    return new DataKey<String, String, String>(getSequence(), null, null);
  }

  public static DataKey getKey(String sequence) {
    return new DataKey<String, String, String>(sequence, null, null);
  }

  protected void setSequence(String sequence) {
    setString(SEQUENCE, sequence);
  }

  public String getSequence() {
    return getString(SEQUENCE);
  }

  protected void setIntValue(int value) {
    setInt(INTVALUE, value);
  }

  public int getIntValue() {
    return getInt(INTVALUE);
  }

  protected void setLongValue(long value) {
    setLong(LONGVALUE, value);
  }

  public long getLongValue() {
    return getLong(LONGVALUE);
  }

  @Override
  public Vector<IItemType> getGuiItems(AdminRecord admin) {
    return null; // not supported
  }

  @Override
  public TableItemHeader[] getGuiTableHeader() {
    return null; // not supported
  }

  @Override
  public TableItem[] getGuiTableItems() {
    return null; // not supported
  }

}
