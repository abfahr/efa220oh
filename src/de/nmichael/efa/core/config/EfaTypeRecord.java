/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.core.config;

import java.util.Vector;

import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.storage.MetaData;
import de.nmichael.efa.gui.util.TableItem;
import de.nmichael.efa.gui.util.TableItemHeader;
import de.nmichael.efa.util.Logger;

// @i18n complete

public class EfaTypeRecord extends DataRecord implements Comparable {

  // =========================================================================
  // Field Names
  // =========================================================================

  public static final String CATEGORY = "Category";
  public static final String TYPE = "Type";
  public static final String POSITION = "Position";
  public static final String VALUE = "Value";

  public static void initialize() {
    Vector<String> f = new Vector<String>();
    Vector<Integer> t = new Vector<Integer>();

    f.add(CATEGORY);
    t.add(IDataAccess.DATA_STRING);
    f.add(TYPE);
    t.add(IDataAccess.DATA_STRING);
    f.add(POSITION);
    t.add(IDataAccess.DATA_INTEGER);
    f.add(VALUE);
    t.add(IDataAccess.DATA_STRING);
    MetaData metaData = constructMetaData(EfaTypes.DATATYPE, f, t, false);
    metaData.setKey(new String[] { CATEGORY, TYPE });
  }

  public EfaTypeRecord(EfaTypes efaTypes, MetaData metaData) {
    super(efaTypes, metaData);
  }

  @Override
  public DataRecord createDataRecord() { // used for cloning
    return getPersistence().createNewRecord();
  }

  @Override
  public DataKey getKey() {
    return new DataKey<String, String, String>(getCategory(), getType(), null);
  }

  public static DataKey getKey(String category, String type) {
    return new DataKey<String, String, String>(category, type, null);
  }

  public void setCategory(String category) {
    setString(CATEGORY, category);
  }

  public String getCategory() {
    return getString(CATEGORY);
  }

  public void setType(String type) {
    setString(TYPE, type);
  }

  public String getType() {
    return getString(TYPE);
  }

  public void setPosition(int position) {
    setInt(POSITION, position);
  }

  public int getPosition() {
    return getInt(POSITION);
  }

  public void setValue(String value) {
    setString(VALUE, value);
  }

  public String getValue() {
    String s = getString(VALUE);
    return (s == null ? "" : s);
  }

  @Override
  public String[] getQualifiedNameFields() {
    return new String[] { CATEGORY, TYPE };
  }

  @Override
  public String getQualifiedName() {
    String category = getCategory();
    String type = getType();
    return (category != null && type != null ? category + "_" + type : "");
  }

  @Override
  public Vector<IItemType> getGuiItems(AdminRecord admin) {
    return null;
  }

  @Override
  public TableItemHeader[] getGuiTableHeader() {
    return null;
  }

  @Override
  public TableItem[] getGuiTableItems() {
    return null;
  }

  @Override
  public int compareTo(Object o) throws ClassCastException {
    if (o == null) {
      return -1;
    }
    try {
      EfaTypeRecord or = (EfaTypeRecord) o;
      if (this.getCategory().equals(or.getCategory())) {
        return (this.getPosition() < or.getPosition() ? -1 :
          (this.getPosition() > or.getPosition() ? 1 :
            0));
      } else {
        return this.getCategory().compareTo(or.getCategory());
      }
    } catch (Exception e) {
      Logger.logdebug(e);
    }
    return -1;
  }

}
