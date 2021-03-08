/* Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.core.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Vector;

import de.nmichael.efa.Daten;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.storage.MetaData;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.ex.EfaModifyException;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

// @i18n complete

public class EfaTypes extends StorageObject {

  public static final String DATATYPE = "efa2types";

  public static final String CATEGORY_BOAT = "BOAT"; // BART
  public static final String CATEGORY_NUMSEATS = "NUMSEATS"; // BANZAHL
  public static final String CATEGORY_RIGGING = "RIGGING"; // BRIGGER
  public static final String CATEGORY_COXING = "COXING"; // BSTM
  public static final String CATEGORY_SESSION = "SESSION"; // FAHRT
  public static final String CATEGORY_STATUS = "STATUS"; // n/a

  public static final String TYPE_BOAT_RACING = "RACING"; // RENNBOOT
  public static final String TYPE_BOAT_WHERRY = "WHERRY"; // WHERRY
  public static final String TYPE_BOAT_TRIMMY = "TRIMMY"; // TRIMMY
  public static final String TYPE_BOAT_AGIG = "AGIG"; // AGIG
  public static final String TYPE_BOAT_BGIG = "BGIG"; // BGIG
  public static final String TYPE_BOAT_CGIG = "CGIG"; // CGIG
  public static final String TYPE_BOAT_DGIG = "DGIG"; // DGIG
  public static final String TYPE_BOAT_EGIG = "EGIG"; // EGIG
  public static final String TYPE_BOAT_INRIGGER = "INRIGGER"; // INRIGGER
  public static final String TYPE_BOAT_BARQUE = "BARQUE"; // BARKE
  public static final String TYPE_BOAT_CHURCHBOAT = "CHURCHBOAT"; // KIRCHBOOT
  public static final String TYPE_BOAT_MOTORBOAT = "MOTORBOAT"; // MOTORBOOT
  public static final String TYPE_BOAT_ERG = "ERG"; // ERGO
  public static final String TYPE_BOAT_SEAKAYAK = "SEAKAYAK"; // neu für Kanuten: Seekajak
  public static final String TYPE_BOAT_RACINGKAYAK = "RACINGKAYAK"; // neu für Kanuten: Rennkajak
  public static final String TYPE_BOAT_WHITEWATERKAYAK = "WHITEWATERKAYAK"; // neu für Kanuten:
  // Wildwasserkajak
  public static final String TYPE_BOAT_CANADIANTOURINGCANOE = "CANADIANTOURINGCANOE"; // neu für
  // Kanuten:
  // Tourenkanadier
  public static final String TYPE_BOAT_POLOBOAT = "POLOBOAT"; // neu für Kanuten: Tourenkanadier
  public static final String TYPE_BOAT_FOLDINGCANOE = "FOLDINGCANOE"; // neu für Kanuten:
  // Tourenkanadier
  public static final String TYPE_BOAT_CANADIANTEAMCANOE = "CANADIANTEAMCANOE"; // neu für Kanuten:
  // Mannschaftskanadier
  public static final String TYPE_BOAT_DRAGONBOAT = "DRAGONBOAT"; // neu für Kanuten: Drachenboot
  public static final String TYPE_BOAT_OTHER = "OTHER"; // other

  public static final String TYPE_NUMSEATS_1 = "1"; // 1
  public static final String TYPE_NUMSEATS_2 = "2"; // 2
  public static final String TYPE_NUMSEATS_2X = "2X"; // 2
  public static final String TYPE_NUMSEATS_3 = "3"; // 3
  public static final String TYPE_NUMSEATS_4 = "4"; // 4
  public static final String TYPE_NUMSEATS_4X = "4X"; // 4
  public static final String TYPE_NUMSEATS_5 = "5"; // 5
  public static final String TYPE_NUMSEATS_6 = "6"; // 6
  public static final String TYPE_NUMSEATS_6X = "6X"; // 6
  public static final String TYPE_NUMSEATS_8 = "8"; // 8
  public static final String TYPE_NUMSEATS_8X = "8X"; // 8
  public static final String TYPE_NUMSEATS_OTHER = "OTHER"; // other

  public static final String TYPE_RIGGING_SCULL = "SCULL"; // SKULL
  public static final String TYPE_RIGGING_SWEEP = "SWEEP"; // RIEMEN
  public static final String TYPE_RIGGING_PADDLE = "PADDLE"; // neu für Kanuten: Paddel
  public static final String TYPE_RIGGING_OTHER = "OTHER"; // other

  public static final String TYPE_COXING_COXED = "COXED"; // MIT
  public static final String TYPE_COXING_COXLESS = "COXLESS"; // OHNE
  public static final String TYPE_COXING_OTHER = "OTHER"; // other

  // Kanu-eFB

  public static final String TYPE_STATUS_GUEST = "GUEST"; // Gast
  public static final String TYPE_STATUS_OTHER = "OTHER"; // andere

  public static final String TYPE_WEEKDAY_MONDAY = "MONDAY";
  public static final String TYPE_WEEKDAY_TUESDAY = "TUESDAY";
  public static final String TYPE_WEEKDAY_WEDNESDAY = "WEDNESDAY";
  public static final String TYPE_WEEKDAY_THURSDAY = "THURSDAY";
  public static final String TYPE_WEEKDAY_FRIDAY = "FRIDAY";
  public static final String TYPE_WEEKDAY_SATURDAY = "SATURDAY";
  public static final String TYPE_WEEKDAY_SUNDAY = "SUNDAY";

  public static String TEXT_UNKNOWN = "unknown"; // overwritten in International.java

  public static final int SELECTION_ROWING = 1;
  public static final int SELECTION_CANOEING = 2;

  public static final int ARRAY_STRINGLIST_VALUES = 1;
  public static final int ARRAY_STRINGLIST_DISPLAY = 2;

  private Vector<String> categories;
  // private Hashtable<String,Vector<EfaTypeRecord>> values;
  private CustSettings custSettings;

  // Default Construktor
  public EfaTypes(CustSettings custSettings) {
    super(IDataAccess.TYPE_FILE_XML, Daten.efaCfgDirectory, null, null, "types", DATATYPE,
        International.getString("Bezeichnungen"));
    EfaTypeRecord.initialize();
    dataAccess.setMetaData(MetaData.getMetaData(DATATYPE));
    this.custSettings = custSettings;
    iniCategories();
  }

  public EfaTypes(int storageType,
      String storageLocation,
      String storageUsername,
      String storagePassword) {
    super(storageType, storageLocation, storageUsername, storagePassword, "types", DATATYPE,
        International.getString("Bezeichnungen"));
    EfaTypeRecord.initialize();
    dataAccess.setMetaData(MetaData.getMetaData(DATATYPE));
    iniCategories();
  }

  @Override
  public void open(boolean createNewIfNotExists) throws EfaException {
    super.open(createNewIfNotExists);
    try {
      if (createNewIfNotExists
          && (data().getAllKeys() == null || data().getAllKeys().length == 0)) {
        // empty EfaTypes newly created
        // make sure that this.custSettings != null when creating from scratch!
        setCustSettings(custSettings);
        setToLanguage(null);
      }
    } catch (Exception e) {
      Logger.log(e);
    }
  }

  @Override
  public DataRecord createNewRecord() {
    return new EfaTypeRecord(this, MetaData.getMetaData(DATATYPE));
  }

  public EfaTypeRecord createEfaTypeRecord(String category, String type, String value) {
    EfaTypeRecord r = new EfaTypeRecord(this, MetaData.getMetaData(DATATYPE));
    r.setCategory(category);
    r.setType(type);
    r.setPosition(getHighestPosition(category) + 1);
    r.setValue(value);
    return r;
  }

  public EfaTypeRecord getRecord(String category, String type) {
    try {
      return ((EfaTypeRecord) data().get(EfaTypeRecord.getKey(category, type)));
    } catch (Exception e) {
      return null;
    }
  }

  public void addRecord(EfaTypeRecord r) {
    try {
      data().add(r);
    } catch (Exception e) {
      Logger.log(e);
    }
  }

  public void updateRecord(String category, String type, String value) {
    try {
      DataKey k = EfaTypeRecord.getKey(category, type);
      EfaTypeRecord r = (EfaTypeRecord) data().get(k);
      r.setValue(value);
      data().update(r);
    } catch (Exception e) {
      Logger.log(e);
    }
  }

  public void deleteRecord(String category, String type) {
    try {
      DataKey k = EfaTypeRecord.getKey(category, type);
      data().delete(k);
    } catch (Exception e) {
      Logger.log(e);
    }
  }

  private void iniCategories() {
    categories = new Vector<String>();
    categories.add(CATEGORY_BOAT);
    categories.add(CATEGORY_NUMSEATS);
    categories.add(CATEGORY_RIGGING);
    categories.add(CATEGORY_COXING);
    categories.add(CATEGORY_SESSION);
    categories.add(CATEGORY_STATUS);
  }

  public void setValue(String cat, String typ, String val) {
    if (cat == null || typ == null || val == null ||
        cat.length() == 0 || typ.length() == 0 || val.length() == 0 ||
        !categories.contains(cat)) {
      return;
    }

    if (!isConfigured(cat, typ)) {
      addRecord(createEfaTypeRecord(cat, typ, val));
    } else {
      updateRecord(cat, typ, val);
    }
  }

  public void removeValue(String cat, String typ) {
    if (!isConfigured(cat, typ)) {
      return;
    }
    deleteRecord(cat, typ);
  }

  public void removeAllValues(String cat) {
    try {
      DataKey[] keys = data().getByFields(new String[] { EfaTypeRecord.CATEGORY },
          new String[] { cat });
      for (DataKey key : keys) {
        data().delete(key);
      }
    } catch (Exception e) {
      Logger.log(e);
    }
  }

  public boolean isConfigured(String cat, String typ) {
    if (cat == null || typ == null || cat.length() == 0 || typ.length() == 0) {
      return false;
    }
    return getRecord(cat, typ) != null;
  }

  public String getValue(String cat, String typ) {
    if (cat == null || typ == null || cat.length() == 0 || typ.length() == 0) {
      return International.getString("unbekannt");
    }
    EfaTypeRecord r = getRecord(cat, typ);
    if (r != null && r.getValue() != null) {
      return r.getValue();
    }
    return International.getString("unbekannt");
  }

  public static String getValueWeekday(String type) {
    if (type == null) {
      return "";
    }
    if (type.endsWith(TYPE_WEEKDAY_MONDAY)) {
      return International.getString("Montag");
    }
    if (type.endsWith(TYPE_WEEKDAY_TUESDAY)) {
      return International.getString("Dienstag");
    }
    if (type.endsWith(TYPE_WEEKDAY_WEDNESDAY)) {
      return International.getString("Mittwoch");
    }
    if (type.endsWith(TYPE_WEEKDAY_THURSDAY)) {
      return International.getString("Donnerstag");
    }
    if (type.endsWith(TYPE_WEEKDAY_FRIDAY)) {
      return International.getString("Freitag");
    }
    if (type.endsWith(TYPE_WEEKDAY_SATURDAY)) {
      return International.getString("Samstag");
    }
    if (type.endsWith(TYPE_WEEKDAY_SUNDAY)) {
      return International.getString("Sonntag");
    }
    return "";
  }

  public String getTypeForValue(String cat, String val) {
    if (cat == null || cat.length() == 0 || val == null || val.length() == 0) {
      return null;
    }
    try {
      DataKey[] keys = data().getByFields(new String[] { EfaTypeRecord.CATEGORY },
          new String[] { cat });
      for (DataKey key : keys) {
        EfaTypeRecord r = (EfaTypeRecord) data().get(key);
        if (r.getValue() != null && r.getValue().equals(val)) {
          return r.getType();
        }
      }
    } catch (Exception e) {
      Logger.log(e);
    }
    return null;
  }

  private EfaTypeRecord[] getItems(String cat) {
    if (cat == null || cat.length() == 0) {
      return null;
    }
    try {
      Vector<EfaTypeRecord> types = new Vector<EfaTypeRecord>();
      DataKey[] keys = data().getByFields(new String[] { EfaTypeRecord.CATEGORY },
          new String[] { cat });
      if (keys != null) {
        for (DataKey key : keys) {
          EfaTypeRecord r = (EfaTypeRecord) data().get(key);
          if (r.getCategory() != null && r.getCategory().equals(cat)) {
            types.add(r);
          }
        }
      }
      EfaTypeRecord[] ra = new EfaTypeRecord[types.size()];
      for (int i = 0; i < ra.length; i++) {
        ra[i] = types.get(i);
      }
      Arrays.sort(ra);
      return ra;
    } catch (Exception e) {
      Logger.log(e);
    }
    return null;
  }

  public int getHighestPosition(String cat) {
    int max = -1;
    EfaTypeRecord[] items = getItems(cat);
    for (int i = 0; items != null && i < items.length; i++) {
      if (items[i].getPosition() > max) {
        max = items[i].getPosition();
      }
    }
    return max;
  }

  public int size(String cat) {
    EfaTypeRecord[] types = getItems(cat);
    if (types == null) {
      return 0;
    }
    return types.length;
  }

  public String[] getTypesArray(String cat) {
    EfaTypeRecord[] types = getItems(cat);
    if (types == null) {
      return new String[0];
    }
    String[] a = new String[types.length];
    for (int i = 0; i < types.length; i++) {
      a[i] = types[i].getType();
    }
    return a;
  }

  public String[] getValueArray(String cat) {
    EfaTypeRecord[] types = getItems(cat);
    if (types == null) {
      return new String[0];
    }
    String[] a = new String[types.length];
    for (int i = 0; i < types.length; i++) {
      a[i] = types[i].getValue();
    }
    return a;
  }

  public static boolean isGigBoot(String key) {
    if (key == null || key.length() == 0) {
      return false;
    }
    int sep = key.indexOf("_");
    String type = key;
    if (sep > 0) {
      type = key.substring(sep + 1);
    }
    if (type.length() == 0) {
      return false;
    }

    return (type.equals(TYPE_BOAT_AGIG) ||
        type.equals(TYPE_BOAT_BGIG) ||
        type.equals(TYPE_BOAT_CGIG) ||
        type.equals(TYPE_BOAT_DGIG) ||
        type.equals(TYPE_BOAT_EGIG) ||
        type.equals(TYPE_BOAT_INRIGGER) ||
        type.equals(TYPE_BOAT_BARQUE) ||
        type.equals(TYPE_BOAT_CHURCHBOAT) ||
        type.equals(TYPE_BOAT_WHERRY) || type.equals(TYPE_BOAT_TRIMMY));
  }

  public ArrayList<String> getDefaultCanoeBoatTypes() {
    ArrayList<String> list = new ArrayList<String>();
    if (isConfigured(CATEGORY_BOAT, TYPE_BOAT_SEAKAYAK)) {
      list.add(TYPE_BOAT_SEAKAYAK);
    }
    if (isConfigured(CATEGORY_BOAT, TYPE_BOAT_RACINGKAYAK)) {
      list.add(TYPE_BOAT_RACINGKAYAK);
    }
    if (isConfigured(CATEGORY_BOAT, TYPE_BOAT_WHITEWATERKAYAK)) {
      list.add(TYPE_BOAT_WHITEWATERKAYAK);
    }
    if (isConfigured(CATEGORY_BOAT, TYPE_BOAT_CANADIANTOURINGCANOE)) {
      list.add(TYPE_BOAT_CANADIANTOURINGCANOE);
    }
    if (isConfigured(CATEGORY_BOAT, TYPE_BOAT_POLOBOAT)) {
      list.add(TYPE_BOAT_POLOBOAT);
    }
    if (isConfigured(CATEGORY_BOAT, TYPE_BOAT_FOLDINGCANOE)) {
      list.add(TYPE_BOAT_FOLDINGCANOE);
    }
    if (isConfigured(CATEGORY_BOAT, TYPE_BOAT_CANADIANTEAMCANOE)) {
      list.add(TYPE_BOAT_CANADIANTEAMCANOE);
    }
    if (isConfigured(CATEGORY_BOAT, TYPE_BOAT_DRAGONBOAT)) {
      list.add(TYPE_BOAT_DRAGONBOAT);
    }
    return list;
  }

  public static int getNumberOfRowers(String key) {
    if (key == null) {
      return LogbookRecord.CREW_MAX;
    }
    if (key.equals(EfaTypes.TYPE_NUMSEATS_1)) {
      return 1;
    }
    if (key.equals(EfaTypes.TYPE_NUMSEATS_2) ||
        key.equals(EfaTypes.TYPE_NUMSEATS_2X)) {
      return 2;
    }
    if (key.equals(EfaTypes.TYPE_NUMSEATS_3)) {
      return 3;
    }
    if (key.equals(EfaTypes.TYPE_NUMSEATS_4) ||
        key.equals(EfaTypes.TYPE_NUMSEATS_4X)) {
      return 4;
    }
    if (key.equals(EfaTypes.TYPE_NUMSEATS_5)) {
      return 5;
    }
    if (key.equals(EfaTypes.TYPE_NUMSEATS_6) ||
        key.equals(EfaTypes.TYPE_NUMSEATS_6X)) {
      return 6;
    }
    if (key.equals(EfaTypes.TYPE_NUMSEATS_8) ||
        key.equals(EfaTypes.TYPE_NUMSEATS_8X)) {
      return 8;
    }
    if (key.equals(EfaTypes.TYPE_NUMSEATS_OTHER)) {
      return LogbookRecord.CREW_MAX;
    }

    // ok, no key found. Now try to extract some numbers from the key itself (as in "6X")
    int num = EfaUtil.stringFindInt(key, 0);
    if (num > 0 && num <= LogbookRecord.CREW_MAX) {
      return num;
    }

    return LogbookRecord.CREW_MAX;
  }

  public static String getStringUnknown() {
    return International.getString("unbekannt");
  }

  public void setCustSettings(CustSettings custSettings) {
    if (custSettings != null) {
      this.custSettings = custSettings;
    } else {
      this.custSettings = new CustSettings();
    }
  }

  private int setToLanguage(String cat, String typ, String itxt, String otxt,
      ResourceBundle bundle, boolean createNewIfNotExists) {
    if ((!isConfigured(cat, typ) && createNewIfNotExists) ||
        (isConfigured(cat, typ) && getValue(cat, typ).equals(itxt))) {
      // value not yet configured or unchanged (has default value for current language)
      String key = International.makeKey(otxt);
      try {
        String val = bundle.getString(key);
        setValue(cat, typ, val);
      } catch (Exception e) {
        setValue(cat, typ, itxt); // use itxt as value if target language bundle does not contain
        // translation
      }
      return 1;
    }
    return 0;
  }

  public int setToLanguage_Boats(ResourceBundle bundle, int typeSelection, boolean createNew) {
    int count = 0;
    switch (typeSelection) {
      case SELECTION_ROWING:
        count += setToLanguage(CATEGORY_BOAT, TYPE_BOAT_RACING,
            International.getString("Rennboot"), "Rennboot", bundle, createNew);
        count += setToLanguage(CATEGORY_BOAT, TYPE_BOAT_WHERRY, International.getString("Wherry"),
            "Wherry", bundle, createNew);
        count += setToLanguage(CATEGORY_BOAT, TYPE_BOAT_TRIMMY, International.getString("Trimmy"),
            "Trimmy", bundle, createNew);
        count += setToLanguage(CATEGORY_BOAT, TYPE_BOAT_AGIG, International.getString("A-Gig"),
            "A-Gig", bundle, createNew);
        count += setToLanguage(CATEGORY_BOAT, TYPE_BOAT_BGIG, International.getString("B-Gig"),
            "B-Gig", bundle, createNew);
        count += setToLanguage(CATEGORY_BOAT, TYPE_BOAT_CGIG, International.getString("C-Gig"),
            "C-Gig", bundle, createNew);
        count += setToLanguage(CATEGORY_BOAT, TYPE_BOAT_DGIG, International.getString("D-Gig"),
            "D-Gig", bundle, createNew);
        count += setToLanguage(CATEGORY_BOAT, TYPE_BOAT_EGIG, International.getString("E-Gig"),
            "E-Gig", bundle, createNew);
        count += setToLanguage(CATEGORY_BOAT, TYPE_BOAT_INRIGGER,
            International.getString("Inrigger"), "Inrigger", bundle, createNew);
        count += setToLanguage(CATEGORY_BOAT, TYPE_BOAT_BARQUE, International.getString("Barke"),
            "Barke", bundle, createNew);
        count += setToLanguage(CATEGORY_BOAT, TYPE_BOAT_CHURCHBOAT,
            International.getString("Kirchboot"), "Kirchboot", bundle, createNew);
        count += setToLanguage(CATEGORY_BOAT, TYPE_BOAT_ERG, International.getString("Ergo"),
            "Ergo", bundle, createNew);

        count += setToLanguage(CATEGORY_NUMSEATS, TYPE_NUMSEATS_1,
            International.getString("Einer"), "Einer", bundle, createNew);
        count += setToLanguage(CATEGORY_NUMSEATS, TYPE_NUMSEATS_2,
            International.getString("Zweier"), "Zweier", bundle, createNew);
        count += setToLanguage(CATEGORY_NUMSEATS, TYPE_NUMSEATS_2X,
            International.getString("Doppelzweier"), "Doppelzweier", bundle, createNew);
        count += setToLanguage(CATEGORY_NUMSEATS, TYPE_NUMSEATS_3,
            International.getString("Dreier"), "Dreier", bundle, createNew);
        count += setToLanguage(CATEGORY_NUMSEATS, TYPE_NUMSEATS_4,
            International.getString("Vierer"), "Vierer", bundle, createNew);
        count += setToLanguage(CATEGORY_NUMSEATS, TYPE_NUMSEATS_4X,
            International.getString("Doppelvierer"), "Doppelvierer", bundle, createNew);
        count += setToLanguage(CATEGORY_NUMSEATS, TYPE_NUMSEATS_5,
            International.getString("Fünfer"), "Fünfer", bundle, createNew);
        count += setToLanguage(CATEGORY_NUMSEATS, TYPE_NUMSEATS_6,
            International.getString("Sechser"), "Sechser", bundle, createNew);
        count += setToLanguage(CATEGORY_NUMSEATS, TYPE_NUMSEATS_6X,
            International.getString("Doppelsechser"), "Doppelsechser", bundle, createNew);
        count += setToLanguage(CATEGORY_NUMSEATS, TYPE_NUMSEATS_8,
            International.getString("Achter"), "Achter", bundle, createNew);
        count += setToLanguage(CATEGORY_NUMSEATS, TYPE_NUMSEATS_8X,
            International.getString("Doppelachter"), "Doppelachter", bundle, createNew);

        count += setToLanguage(CATEGORY_RIGGING, TYPE_RIGGING_SCULL,
            International.getString("Skull"), "Skull", bundle, createNew);
        count += setToLanguage(CATEGORY_RIGGING, TYPE_RIGGING_SWEEP,
            International.getString("Riemen"), "Riemen", bundle, createNew);
        break;
      case SELECTION_CANOEING:
        count += setToLanguage(CATEGORY_BOAT, TYPE_BOAT_SEAKAYAK,
            International.getString("Seekajak"), "Seekajak", bundle, createNew);
        count += setToLanguage(CATEGORY_BOAT, TYPE_BOAT_RACINGKAYAK,
            International.getString("Rennkajak"), "Rennkajak", bundle, createNew);
        count += setToLanguage(CATEGORY_BOAT, TYPE_BOAT_WHITEWATERKAYAK,
            International.getString("Wildwasserkajak"), "Wildwasserkajak", bundle, createNew);
        count += setToLanguage(CATEGORY_BOAT, TYPE_BOAT_CANADIANTOURINGCANOE,
            International.getString("Tourenkanadier"), "Tourenkanadier", bundle, createNew);
        count += setToLanguage(CATEGORY_BOAT, TYPE_BOAT_POLOBOAT,
            International.getString("Poloboot"), "Poloboot", bundle, createNew);
        count += setToLanguage(CATEGORY_BOAT, TYPE_BOAT_FOLDINGCANOE,
            International.getString("Faltboot"), "Faltboot", bundle, createNew);
        count += setToLanguage(CATEGORY_BOAT, TYPE_BOAT_CANADIANTEAMCANOE,
            International.getString("Mannschaftskanadier"), "Mannschaftskanadier", bundle,
            createNew);
        count += setToLanguage(CATEGORY_BOAT, TYPE_BOAT_DRAGONBOAT,
            International.getString("Drachenboot"), "Drachenboot", bundle, createNew);

        count += setToLanguage(CATEGORY_NUMSEATS, TYPE_NUMSEATS_1,
            International.getString("Einer"), "Einer", bundle, createNew);
        count += setToLanguage(CATEGORY_NUMSEATS, TYPE_NUMSEATS_2,
            International.getString("Zweier"), "Zweier", bundle, createNew);
        count += setToLanguage(CATEGORY_NUMSEATS, TYPE_NUMSEATS_3,
            International.getString("Dreier"), "Dreier", bundle, createNew);
        count += setToLanguage(CATEGORY_NUMSEATS, TYPE_NUMSEATS_4,
            International.getString("Vierer"), "Vierer", bundle, createNew);

        count += setToLanguage(CATEGORY_RIGGING, TYPE_RIGGING_PADDLE,
            International.getString("Paddel"), "Paddel", bundle, createNew);
        break;
    }

    count += setToLanguage(CATEGORY_BOAT, TYPE_BOAT_MOTORBOAT,
        International.getString("Motorboot"), "Motorboot", bundle, createNew);
    count += setToLanguage(CATEGORY_BOAT, TYPE_BOAT_OTHER, International.getString("andere"),
        "andere", bundle, createNew);
    count += setToLanguage(CATEGORY_NUMSEATS, TYPE_NUMSEATS_OTHER,
        International.getString("andere"), "andere", bundle, createNew);
    count += setToLanguage(CATEGORY_RIGGING, TYPE_RIGGING_OTHER, International.getString("andere"),
        "andere", bundle, createNew);
    count += setToLanguage(CATEGORY_COXING, TYPE_COXING_COXED, International.getString("mit Stm."),
        "mit Stm.", bundle, createNew);
    count += setToLanguage(CATEGORY_COXING, TYPE_COXING_COXLESS,
        International.getString("ohne Stm."), "ohne Stm.", bundle, createNew);
    count += setToLanguage(CATEGORY_COXING, TYPE_COXING_OTHER, International.getString("andere"),
        "andere", bundle, createNew);

    return count;
  }

  public boolean setToLanguage(String lang) {
    ResourceBundle bundle = null;
    if (lang != null) {
      try {
        bundle = ResourceBundle.getBundle(International.BUNDLE_NAME, new Locale(lang));
      } catch (Exception e) {
        Logger.log(Logger.ERROR, Logger.MSG_CORE_EFATYPESFAILEDSETVALUES,
            "Failed to set EfaTypes values for language " + lang + ".");
        bundle = International.getResourceBundle();
        // return false;
      }
    } else {
      bundle = International.getResourceBundle();
    }

    boolean createNew = (custSettings != null ? true : false);

    setToLanguage(CATEGORY_STATUS, TYPE_STATUS_GUEST, International.getString("Gast"), "Gast",
        bundle, createNew);
    setToLanguage(CATEGORY_STATUS, TYPE_STATUS_OTHER, International.getString("andere"), "andere",
        bundle, createNew);

    setToLanguage_Boats(bundle, SELECTION_ROWING,
        (custSettings != null ? custSettings.activateRowingOptions : false));
    setToLanguage_Boats(bundle, SELECTION_CANOEING,
        (custSettings != null ? custSettings.activateCanoeingOptions : false));

    return true;
  }

  private static String[] makeTypeArray(int type, String cat) {
    if (Daten.efaTypes == null) {
      return null; // can happen during startup
    }
    if (type == ARRAY_STRINGLIST_VALUES) {
      return Daten.efaTypes.getTypesArray(cat);
    }
    if (type == ARRAY_STRINGLIST_DISPLAY) {
      return Daten.efaTypes.getValueArray(cat);
    }
    return null;
  }

  public static String[] makeSessionTypeArray(int type) {
    return makeSessionTypeArray(type, false);
  }

  public static String[] makeSessionTypeArray(int type, boolean withEmptyValue) {
    if (Daten.efaTypes == null) {
      // EfaTypes are not available when efa is started (will be initialized after EfaConfig).
      // This doesn't matter ... when EfaConfigFrame is opened, a new EfaConfig instance will be
      // created
      // (as a copy from the static instance). This will also initialize all lists, including this
      // one.
      return null;
    }
    String[] types = makeTypeArray(type, EfaTypes.CATEGORY_SESSION);
    if (withEmptyValue && types != null && types.length > 0) {
      String[] types2 = new String[types.length + 1];
      types2[0] = (type == ARRAY_STRINGLIST_VALUES ? ""
          : "<"
              + International.getString("keine Auswahl") + ">");
      for (int i = 0; i < types.length; i++) {
        types2[i + 1] = types[i];
      }
      types = types2;
    }
    return types;
  }

  public static String[] makeBoatTypeArray(int type) {
    return makeTypeArray(type, EfaTypes.CATEGORY_BOAT);
  }

  public static String[] makeBoatSeatsArray(int type) {
    return makeTypeArray(type, EfaTypes.CATEGORY_NUMSEATS);
  }

  public static String[] makeBoatRiggingArray(int type) {
    return makeTypeArray(type, EfaTypes.CATEGORY_RIGGING);
  }

  public static String[] makeBoatCoxingArray(int type) {
    return makeTypeArray(type, EfaTypes.CATEGORY_COXING);
  }

  public static String[] makeDayOfWeekArray(int type) {
    int shift = (International.getLanguageID().startsWith("de") ? 0 : 6);
    String[] list = new String[7];
    for (int i = 0; i < list.length; i++) {
      int nrOfDay = (i + shift) % 7;
      String day = null;
      switch (nrOfDay) {
        case 0:
          day = TYPE_WEEKDAY_MONDAY;
          break;
        case 1:
          day = TYPE_WEEKDAY_TUESDAY;
          break;
        case 2:
          day = TYPE_WEEKDAY_WEDNESDAY;
          break;
        case 3:
          day = TYPE_WEEKDAY_THURSDAY;
          break;
        case 4:
          day = TYPE_WEEKDAY_FRIDAY;
          break;
        case 5:
          day = TYPE_WEEKDAY_SATURDAY;
          break;
        case 6:
          day = TYPE_WEEKDAY_SUNDAY;
          break;
      }
      list[i] = (type == ARRAY_STRINGLIST_VALUES ? day : getValueWeekday(day));
    }
    return list;
  }

  @Override
  public void preModifyRecordCallback(DataRecord record, boolean add, boolean update,
      boolean delete)
      throws EfaModifyException {
    if (add || update) {
      assertFieldNotEmpty(record, EfaTypeRecord.CATEGORY);
      assertFieldNotEmpty(record, EfaTypeRecord.TYPE);
      assertFieldNotEmpty(record, EfaTypeRecord.VALUE);
      assertUnique(record, new String[] { EfaTypeRecord.CATEGORY, EfaTypeRecord.TYPE });
      assertUnique(record, new String[] { EfaTypeRecord.CATEGORY, EfaTypeRecord.VALUE });
    }
  }

}
