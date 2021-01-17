/**
 * Title: efa - elektronisches Fahrtenbuch für Ruderer Copyright: Copyright (c)
 * 2001-2011 by Nicolas Michael Website: http://efa.nmichael.de/ License: GNU
 * General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.data;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.nmichael.efa.Daten;
import de.nmichael.efa.calendar.ICalendarExport;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.core.items.IItemFactory;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeBoolean;
import de.nmichael.efa.core.items.ItemTypeItemList;
import de.nmichael.efa.core.items.ItemTypeString;
import de.nmichael.efa.core.items.ItemTypeStringAutoComplete;
import de.nmichael.efa.core.items.ItemTypeStringList;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.storage.MetaData;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.gui.util.TableItem;
import de.nmichael.efa.gui.util.TableItemHeader;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;
import net.fortuna.ical4j.model.DateTime;

// @i18n complete

public class PersonRecord extends DataRecord implements IItemFactory {

  // =========================================================================
  // Field Names
  // =========================================================================
  public static final String ID = "Id";
  public static final String FIRSTNAME = "FirstName";
  public static final String LASTNAME = "LastName";
  public static final String FIRSTLASTNAME = "FirstLastName";
  public static final String NAMEAFFIX = "NameAffix";
  public static final String TITLE = "Title";
  public static final String GENDER = "Gender";
  public static final String ASSOCIATION = "Association";
  public static final String STATUSID = "StatusId";
  public static final String EMAIL = "Email";
  public static final String ISALLOWEDEMAIL = "erlaubtEmail";
  public static final String ISALLOWEDPHONE = "erlaubtTelefon";
  public static final String ISALLOWEDSHORT = "erlaubtKürzel";
  public static final String ISALLOWEDSPELL = "erlaubtSchreibweise";
  public static final String MEMBERSHIPNO = "MembershipNo";
  public static final String EXCLUDEFROMSTATISTIC = "ExcludeFromStatistics";
  public static final String BOATUSAGEBAN = "BoatUsageBan";
  public static final String INPUTSHORTCUT = "InputShortcut";
  public static final String FESTNETZ1 = "FreeUse1";
  public static final String HANDY2 = "FreeUse2";
  public static final String[] IDX_NAME_NAMEAFFIX = new String[] { FIRSTLASTNAME, NAMEAFFIX };
  private static String GUIITEM_GROUPS = "GUIITEM_GROUPS";
  private static String CAT_BASEDATA = "%01%" + International.getString("Basisdaten");
  private static String CAT_MOREDATA = "%02%" + International.getString("Weitere Daten");
  private static String CAT_GROUPS = "%04%" + International.getString("Gruppen");
  private static String CAT_FREEUSE = "%05%" + International.getString("Freie Verwendung");
  private static Pattern qnamePattern = Pattern.compile("(.+) \\(([^\\(\\)]+)\\)");

  public static void initialize() {
    Vector<String> f = new Vector<String>();
    Vector<Integer> t = new Vector<Integer>();

    f.add(ID);
    t.add(IDataAccess.DATA_UUID);
    f.add(FIRSTNAME);
    t.add(IDataAccess.DATA_STRING);
    f.add(LASTNAME);
    t.add(IDataAccess.DATA_STRING);
    f.add(FIRSTLASTNAME);
    t.add(IDataAccess.DATA_VIRTUAL);
    f.add(NAMEAFFIX);
    t.add(IDataAccess.DATA_STRING);
    f.add(TITLE);
    t.add(IDataAccess.DATA_STRING);
    f.add(GENDER);
    t.add(IDataAccess.DATA_STRING);
    f.add(ASSOCIATION);
    t.add(IDataAccess.DATA_STRING);
    f.add(STATUSID);
    t.add(IDataAccess.DATA_UUID);
    f.add(EMAIL);
    t.add(IDataAccess.DATA_STRING);
    f.add(ISALLOWEDEMAIL);
    t.add(IDataAccess.DATA_BOOLEAN);
    f.add(ISALLOWEDPHONE);
    t.add(IDataAccess.DATA_BOOLEAN);
    f.add(ISALLOWEDSHORT);
    t.add(IDataAccess.DATA_BOOLEAN);
    f.add(ISALLOWEDSPELL);
    t.add(IDataAccess.DATA_BOOLEAN);
    f.add(MEMBERSHIPNO);
    t.add(IDataAccess.DATA_STRING);
    f.add(EXCLUDEFROMSTATISTIC);
    t.add(IDataAccess.DATA_BOOLEAN);
    f.add(BOATUSAGEBAN);
    t.add(IDataAccess.DATA_BOOLEAN);
    f.add(INPUTSHORTCUT);
    t.add(IDataAccess.DATA_STRING);
    f.add(FESTNETZ1);
    t.add(IDataAccess.DATA_STRING);
    f.add(HANDY2);
    t.add(IDataAccess.DATA_STRING);
    MetaData metaData = constructMetaData(Persons.DATATYPE, f, t, true);
    metaData.setKey(new String[] { ID }); // plus VALID_FROM
    metaData.addIndex(IDX_NAME_NAMEAFFIX);
  }

  public PersonRecord(Persons persons, MetaData metaData) {
    super(persons, metaData);
  }

  @Override
  public DataRecord createDataRecord() { // used for cloning
    return getPersistence().createNewRecord();
  }

  @Override
  public DataKey<UUID, Long, ?> getKey() {
    return new DataKey<UUID, Long, String>(getId(), getValidFrom(), null);
  }

  public static DataKey<UUID, Long, ?> getKey(UUID id, long validFrom) {
    return new DataKey<UUID, Long, String>(id, validFrom, null);
  }

  public void setId(UUID id) {
    setUUID(ID, id);
  }

  public UUID getId() {
    return getUUID(ID);
  }

  public void setFirstName(String name) {
    setString(FIRSTNAME, name);
  }

  public String getFirstName() {
    return getString(FIRSTNAME);
  }

  public void setLastName(String name) {
    setString(LASTNAME, name);
  }

  public String getLastName() {
    return getString(LASTNAME);
  }

  public String getFirstLastName() {
    return getFirstLastName(true);
  }

  public String getFirstLastName(boolean alwaysFirstFirst) {
    return getFullName(getString(FIRSTNAME), getString(LASTNAME), null,
        (alwaysFirstFirst ? true : Daten.efaConfig.getValueNameFormatIsFirstNameFirst()));
  }

  public void setNameAffix(String affix) {
    setString(NAMEAFFIX, affix);
  }

  public String getNameAffix() {
    return getString(NAMEAFFIX);
  }

  public void setTitle(String title) {
    setString(TITLE, title);
  }

  public String getTitle() {
    return getString(TITLE);
  }

  public void setGender(String gender) {
    setString(GENDER, gender);
  }

  public String getGender() {
    return getString(GENDER);
  }

  public String getGenderAsString() {
    String s = getGender();
    return (s != null ? Daten.efaTypes.getValue(EfaTypes.CATEGORY_GENDER, s) : null);
  }

  public DataTypeDate getBirthday() {
    return null;
  }

  public String getAssocitation() {
    return getString(ASSOCIATION);
  }

  public void setStatusId(UUID id) {
    setUUID(STATUSID, id);
  }

  public UUID getStatusId() {
    return getUUID(STATUSID);
  }

  public String getStatusName() {
    UUID id = getStatusId();
    if (id == null) {
      return null;
    }
    StatusRecord r = getPersistence().getProject().getStatus(false).getStatus(id);
    if (r != null) {
      return r.getStatusName();
    }
    return null;
  }

  public boolean isStatusMember() {
    UUID id = getStatusId();
    if (id != null) {
      StatusRecord r = getPersistence().getProject().getStatus(false).getStatus(id);
      return (r != null && r.isMember());
    }
    return false;
  }

  public void setEmail(String email) {
    setString(EMAIL, email);
  }

  public String getEmail() {
    return getString(EMAIL);
  }

  public boolean isErlaubtEmail() {
    return getBool(ISALLOWEDEMAIL);
  }

  public void setErlaubnisEmail(boolean isErlaubtEmail) {
    setBool(ISALLOWEDEMAIL, isErlaubtEmail);
  }

  public boolean isErlaubtTelefon() {
    return getBool(ISALLOWEDPHONE);
  }

  public void setErlaubnisTelefon(boolean isErlaubtTelefon) {
    setBool(ISALLOWEDPHONE, isErlaubtTelefon);
  }

  public boolean isErlaubtKuerzel() {
    return getBool(ISALLOWEDSHORT);
  }

  public void setErlaubnisKuerzel(boolean isErlaubtKuerzel) {
    setBool(ISALLOWEDSHORT, isErlaubtKuerzel);
  }

  public boolean isErlaubtSchreibweise() {
    return getBool(ISALLOWEDSPELL);
  }

  public void setErlaubnisSchreibweise(boolean isErlaubtSchreibweise) {
    setBool(ISALLOWEDSPELL, isErlaubtSchreibweise);
  }

  public void setMembershipNo(String no) {
    setString(MEMBERSHIPNO, no);
  }

  public String getMembershipNo() {
    return getString(MEMBERSHIPNO);
  }

  public boolean getExcludeFromPublicStatistics() {
    return getBool(EXCLUDEFROMSTATISTIC);
  }

  public void setBoatUsageBan(boolean banned) {
    setBool(BOATUSAGEBAN, banned);
  }

  public boolean getBoatUsageBan() {
    return getBool(BOATUSAGEBAN);
  }

  public void setInputShortcut(String shortcut) {
    setString(INPUTSHORTCUT, shortcut);
  }

  public String getInputShortcut() {
    return getString(INPUTSHORTCUT);
  }

  public GroupRecord[] getGroupList() {
    Groups groups = getPersistence().getProject().getGroups(false);
    return groups.getGroupsForPerson(getId(), getValidFrom(), getInvalidFrom() - 1);
  }

  public void setFestnetz1(String s) {
    setString(FESTNETZ1, s);
  }

  public String getFestnetz1() {
    return getString(FESTNETZ1);
  }

  public void setHandy2(String s) {
    setString(HANDY2, s);
  }

  public String getHandy2() {
    return getString(HANDY2);
  }

  @Override
  protected Object getVirtualColumn(int fieldIdx) {
    if (getFieldName(fieldIdx).equals(FIRSTLASTNAME)) {
      return getFirstLastName();
    }
    return null;
  }

  public static String getFullName(String first, String last, String affix, boolean firstFirst) {
    String s = "";
    if (firstFirst) {
      if (first != null && first.length() > 0) {
        s = first.trim();
      }
      if (last != null && last.length() > 0) {
        s = s + (s.length() > 0 ? " " : "") + last.trim();
      }
    } else {
      if (last != null && last.length() > 0) {
        s = last.trim();
      }
      if (first != null && first.length() > 0) {
        s = s + (s.length() > 0 ? ", " : "") + first.trim();
      }
    }
    if (affix != null && affix.length() > 0) {
      s = s + " (" + affix + ")";
    }
    return s;
  }

  public static String getFirstLastName(String name) {
    if (name == null || name.length() == 0) {
      return "";
    }
    int pos = name.indexOf(", ");
    if (pos < 0) {
      return name;
    }
    return (name.substring(pos + 2) + " " + name.substring(0, pos)).trim();
  }

  public String getQualifiedName(boolean firstFirst) {
    return getFullName(getFirstName(), getLastName(), getNameAffix(), firstFirst);
  }

  @Override
  public String getQualifiedName() {
    return getQualifiedName(Daten.efaConfig.getValueNameFormatIsFirstNameFirst());
  }

  @Override
  public String[] getQualifiedNameFields() {
    return IDX_NAME_NAMEAFFIX;
  }

  @Override
  public String[] getQualifiedNameFieldsTranslateVirtualToReal() {
    return new String[] { FIRSTNAME, LASTNAME, NAMEAFFIX };
  }

  @Override
  public String[] getQualifiedNameValues(String qname) {
    Matcher m = qnamePattern.matcher(qname);
    if (m.matches()) {
      return new String[] {
          getFirstLastName(m.group(1).trim()),
          m.group(2).trim()
      };
    } else {
      return new String[] {
          getFirstLastName(qname.trim()),
          null
      };
    }
  }

  public static String[] tryGetFirstLastNameAndAffix(String s) {
    Matcher m = qnamePattern.matcher(s);
    String name = s.trim();
    String affix = null;
    if (m.matches()) {
      name = m.group(1).trim();
      affix = m.group(2).trim();
    }

    boolean firstFirst = Daten.efaConfig.getValueNameFormatIsFirstNameFirst();
    String firstName = (firstFirst ? name : null); // if first and last name cannot be found, ...
    String lastName = (firstFirst ? null : name); // ... use full name as either first or last
    int pos = name.indexOf(", ");
    if (pos < 0) {
      pos = name.indexOf(" ");
      if (pos >= 0) {
        firstName = name.substring(0, pos).trim();
        lastName = name.substring(pos + 1).trim();
      }
    } else {
      firstName = name.substring(pos + 2);
      lastName = name.substring(0, pos).trim();
    }
    return new String[] { firstName, lastName, affix };
  }

  public static String[] tryGetNameAndAffix(String s) {
    Matcher m = qnamePattern.matcher(s);
    String name = s.trim();
    String affix = null;
    if (m.matches()) {
      name = m.group(1).trim();
      affix = m.group(2).trim();
    }
    return new String[] { name, affix };
  }

  @Override
  public Object getUniqueIdForRecord() {
    return getId();
  }

  @Override
  public String getAsText(String fieldName) {
    if (fieldName.equals(FIRSTLASTNAME)) {
      return getFirstLastName(false);
    }
    if (fieldName.equals(GENDER)) {
      String s = getAsString(fieldName);
      if (s != null) {
        return Daten.efaTypes.getValue(EfaTypes.CATEGORY_GENDER, s);
      }
      return null;
    }
    if (fieldName.equals(STATUSID)) {
      return getStatusName();
    }
    return super.getAsText(fieldName);
  }

  @Override
  public boolean setFromText(String fieldName, String value) {
    if (fieldName.equals(GENDER)) {
      String s = Daten.efaTypes.getTypeForValue(EfaTypes.CATEGORY_GENDER, value);
      if (s != null) {
        set(fieldName, s);
      }
    } else if (fieldName.equals(STATUSID)) {
      Status status = getPersistence().getProject().getStatus(false);
      StatusRecord sr = status.findStatusByName(value);
      if (sr != null) {
        set(fieldName, sr.getId());
      }
    } else {
      return super.setFromText(fieldName, value);
    }
    return (value.equals(getAsText(fieldName)));
  }

  public Integer getPersonMemberMonth(DataTypeDate startDate, DataTypeDate endDate) {
    long fromLong = this.getValidFrom();
    if (fromLong == 0) {
      return 0;
    }
    DataTypeDate from = new DataTypeDate(fromLong);

    long toLong = this.getInvalidFrom();
    DataTypeDate to;
    if (toLong == Long.MAX_VALUE) {
      to = endDate;
    } else {
      to = new DataTypeDate(toLong);
    }

    return DataTypeDate.getMonthIntersect(from, to, startDate, endDate);
  }

  public static String getAssociationPostfix(PersonRecord p) {
    if (p != null && p.getAssocitation() != null && p.getAssocitation().length() > 0) {
      return " (" + p.getAssocitation() + ")";
    }
    return "";
  }

  public String getAssociationPostfix() {
    return getAssociationPostfix(this);
  }

  public static String trimAssociationPostfix(String s) {
    int pos = s.lastIndexOf(" (");
    if (pos > 0) {
      return s.substring(0, pos);
    }
    return s;
  }

  @Override
  public IItemType[] getDefaultItems(String itemName) {
    if (itemName.equals(PersonRecord.GUIITEM_GROUPS)) {
      IItemType[] items = new IItemType[1];
      items[0] = getGuiItemTypeStringAutoComplete(PersonRecord.GUIITEM_GROUPS, null,
          IItemType.TYPE_PUBLIC, CAT_GROUPS,
          getPersistence().getProject().getGroups(false), getValidFrom(), getInvalidFrom() - 1,
          International.getString("Gruppe"));
      items[0].setFieldSize(300, -1);
      return items;
    }
    return null;

  }

  @Override
  public Vector<IItemType> getGuiItems(AdminRecord admin) {
    Status status = getPersistence().getProject().getStatus(false);
    IItemType item;
    Vector<IItemType> v = new Vector<IItemType>();
    v.add(item = new ItemTypeString(PersonRecord.FIRSTNAME, getFirstName(),
        IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Vorname")));
    ((ItemTypeString) item).setNotAllowedCharacters(",");
    v.add(item = new ItemTypeString(PersonRecord.LASTNAME, getLastName(),
        IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Nachname")));
    ((ItemTypeString) item).setNotAllowedCharacters(",");

    if (admin != null && admin.isAllowedEditPersons()) {
      v.add(item = new ItemTypeStringList(PersonRecord.STATUSID,
          (getStatusId() != null ? getStatusId().toString()
              : status.getStatusOther().getId().toString()),
          status.makeStatusArray(Status.ARRAY_STRINGLIST_VALUES), status
              .makeStatusArray(Status.ARRAY_STRINGLIST_DISPLAY),
          IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Status")));

      v.add(item = new ItemTypeString(PersonRecord.EMAIL, getEmail(),
          IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("email")));
      v.add(item = new ItemTypeString(PersonRecord.HANDY2, getHandy2(),
          IItemType.TYPE_PUBLIC, CAT_BASEDATA,
          "H" + International.getString("Telefon/Handy") + "H"));
      v.add(item = new ItemTypeString(PersonRecord.FESTNETZ1, getFestnetz1(),
          IItemType.TYPE_PUBLIC, CAT_BASEDATA,
          "T" + International.getString("Telefon/Handy") + "T"));
      v.add(item = new ItemTypeString(PersonRecord.INPUTSHORTCUT, getInputShortcut(),
          IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Eingabekürzel")));

      v.add(item = new ItemTypeString(PersonRecord.MEMBERSHIPNO, getMembershipNo(),
          IItemType.TYPE_PUBLIC, CAT_MOREDATA, International.getString("Mitgliedsnummer")));
      v.add(item = new ItemTypeBoolean(PersonRecord.ISALLOWEDEMAIL, isErlaubtEmail(),
          IItemType.TYPE_PUBLIC, CAT_MOREDATA, International.getString("ist Erlaubt Email")));
      v.add(item = new ItemTypeBoolean(PersonRecord.ISALLOWEDPHONE, isErlaubtTelefon(),
          IItemType.TYPE_PUBLIC, CAT_MOREDATA, International.getString("ist Erlaubt Telefon")));
      v.add(item = new ItemTypeBoolean(PersonRecord.ISALLOWEDSHORT, isErlaubtKuerzel(),
          IItemType.TYPE_PUBLIC, CAT_MOREDATA, International.getString("ist Erlaubt Kürzel")));
      v.add(item = new ItemTypeBoolean(PersonRecord.ISALLOWEDSPELL, isErlaubtSchreibweise(),
          IItemType.TYPE_PUBLIC, CAT_MOREDATA,
          International.getString("ist Erlaubt Schreibweise")));
      v.add(item = new ItemTypeBoolean(PersonRecord.BOATUSAGEBAN, getBoatUsageBan(),
          IItemType.TYPE_PUBLIC, CAT_MOREDATA, International.getString("Bootsbenutzungs-Sperre")));
      item.setFieldSize(300, -1);
      v.add(item = new ItemTypeBoolean(PersonRecord.EXCLUDEFROMSTATISTIC,
          getExcludeFromPublicStatistics(),
          IItemType.TYPE_PUBLIC, CAT_MOREDATA, International
              .getString("von allgemein verfügbaren Statistiken ausnehmen")));

    } // admin visible

    v.add(item = new ItemTypeString(PersonRecord.ASSOCIATION, getAssocitation(),
        IItemType.TYPE_EXPERT, CAT_FREEUSE, International.getString("Verein")));
    v.add(item = new ItemTypeString(PersonRecord.TITLE, getTitle(),
        IItemType.TYPE_EXPERT, CAT_FREEUSE, International.getString("Titel")));
    ((ItemTypeString) item).setNotAllowedCharacters(",");
    v.add(item = new ItemTypeString(PersonRecord.NAMEAFFIX, getNameAffix(),
        IItemType.TYPE_EXPERT, CAT_FREEUSE, International.getString("Namenszusatz")));
    ((ItemTypeString) item).setNotAllowedCharacters(",");
    v.add(item = new ItemTypeStringList(PersonRecord.GENDER, getGender(),
        EfaTypes.makeGenderArray(EfaTypes.ARRAY_STRINGLIST_VALUES), EfaTypes
            .makeGenderArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY),
        IItemType.TYPE_EXPERT, CAT_FREEUSE, International.getString("Geschlecht")));

    // hidden parameter, just for BatchEditDialog
    v.add(item = getGuiItemTypeStringAutoComplete(PersonRecord.FIRSTLASTNAME, null,
        IItemType.TYPE_INTERNAL, "",
        getPersistence(), getValidFrom(), getInvalidFrom() - 1,
        International.getString("Name")));

    return v;
  }

  @Override
  public void saveGuiItems(Vector<IItemType> items) {
    for (IItemType item : items) {
      String name = item.getName();
      if (name.equals(GUIITEM_GROUPS) && item.isChanged()) {
        ItemTypeItemList list = (ItemTypeItemList) item;
        Groups groups = getPersistence().getProject().getGroups(false);
        Hashtable<UUID, String> groupIds = new Hashtable<UUID, String>();
        for (int i = 0; i < list.size(); i++) {
          ItemTypeStringAutoComplete l = (ItemTypeStringAutoComplete) list.getItems(i)[0];
          UUID id = (UUID) l.getId(l.getValue());
          if (id != null) {
            groupIds.put(id, "foo");
          }
        }
        groups.setGroupsForPerson(getId(),
            groupIds.keySet().toArray(new UUID[0]),
            getInvalidFrom() - 1);
      }
    }
    super.saveGuiItems(items);
  }

  @Override
  public TableItemHeader[] getGuiTableHeader() {
    TableItemHeader[] header = new TableItemHeader[7];
    if (Daten.efaConfig.getValueNameFormatIsFirstNameFirst()) {
      header[0] = new TableItemHeader(International.getString("Vorname"));
      header[1] = new TableItemHeader(International.getString("Nachname"));
    } else {
      header[0] = new TableItemHeader(International.getString("Nachname"));
      header[1] = new TableItemHeader(International.getString("Vorname"));
    }
    header[2] = new TableItemHeader(International.getString("EingabekürzelKurz"));
    header[3] = new TableItemHeader(International.getString("Status"));
    header[4] = new TableItemHeader(International.getString("okKürzel"));
    header[5] = new TableItemHeader(International.getString("okEmail"));
    header[6] = new TableItemHeader(International.getString("okTelefon"));
    return header;
  }

  @Override
  public TableItem[] getGuiTableItems() {
    TableItem[] items = new TableItem[7];
    if (Daten.efaConfig.getValueNameFormatIsFirstNameFirst()) {
      items[0] = new TableItem(getFirstName());
      items[1] = new TableItem(getLastName());
    } else {
      items[0] = new TableItem(getLastName());
      items[1] = new TableItem(getFirstName());
    }
    items[2] = new TableItem(getInputShortcut());
    items[3] = new TableItem(getStatusName());
    items[4] = new TableItem(isErlaubtKuerzel());
    items[5] = new TableItem(isErlaubtEmail());
    items[6] = new TableItem(isErlaubtTelefon());
    return items;
  }

  public boolean isValidMemberOH() {
    if (getMembershipNo() == null) {
      return false; // Mitgliedsnummer
    }
    if (getMembershipNo().isEmpty()) {
      return false; // Mitgliedsnummer
    }
    if ("Externe Adressen".equals(getStatusName())) {
      return false; // Hauptkategorie
    }
    if (getFirstName() == null) {
      return false; // Vorname fehlt
    }
    return true;
  }

  public boolean cleanPerson() {
    boolean retVal = false;
    // 1. bestimmte Felder leeren
    setBoatUsageBan(false); // kommt nicht mehr
    setNameAffix(null); // immer leer gewesen

    if (!isDyingMember()) {
      return retVal;
    }
    // 2. nur aktive Mitglieder behalten
    long oneMinute = 60 * 1000;
    long oneWeek = 7 * 24 * 60 * oneMinute;
    long now = System.currentTimeMillis();
    long invalidMillisAgo = now - getInvalidFrom();
    if (invalidMillisAgo < oneWeek) {
      return retVal; // nicht alt genug
    }
    if (getInvalidFrom() == Long.MAX_VALUE) {
      return retVal; // ewig gültig
    }
    setDeleted(true); // doof - Datensatz fehlt dann
    retVal = true;
    return retVal;
  }

  public boolean isDyingMember() {
    String statusDesMitglieds = getStatusName();
    if (statusDesMitglieds == null) {
      return false;
    }
    switch (statusDesMitglieds) {
      case "Aktives Mitglied":
      case "Mitglieder gekündigt":
        return false;
      case "Mitglieder ausgetreten":
      case "Mitglieder verstorben":
      case "Mitglieder ausgeschlossen":
        return true;
      case "Passives Mitglied":
      case "Externe Adressen":
      case "andere":
      case "Gast":
        return true;
      default:
        return false;
    }
  }

  public void sendEmailConfirmation(String emailToAdresse, String aktion, String errorText) {
    String emailSubject = "";
    boolean kombinierteEmailErlaubnis = false;
    kombinierteEmailErlaubnis = isErlaubtEmail();
    if (!isValidEmail(emailToAdresse)) {
      emailToAdresse = "efa+no.invalidEmailMitglied" + ICalendarExport.ABFX_DE;
      emailSubject = "Error efa.invalidEmail " + getFirstLastName() + " ";
      kombinierteEmailErlaubnis = false;
    }
    if (getLastModified() == IDataAccess.UNDEFINED_LONG) {
      emailToAdresse = "efa.error.LastModified";
      emailSubject = "Error LastModified ";
      kombinierteEmailErlaubnis = false;
    }
    emailSubject += "OH Änderung Bestätigung " + aktion;
    if (!kombinierteEmailErlaubnis) {
      emailToAdresse = emailToAdresse.replaceAll("@", ".").trim();
      emailToAdresse = "efa+no." + emailToAdresse + ICalendarExport.ABFX_DE;
      emailSubject += " " + getFirstLastName();
    }
    String anrede = getFirstName();
    String emailMessage = getFormattedEmailtextMitglied(anrede, aktion, errorText);
    if ((new File(Daten.efaBaseConfig.efaUserDirectory + Daten.DEBUG_MODE_SPECIAL).exists())) {
      System.out.println(emailSubject);
      System.out.println(emailMessage);
    }

    Messages messages = Daten.project.getMessages(false);
    messages.createAndSaveMessageRecord(emailToAdresse, emailSubject, emailMessage);
    Logger.log(Logger.INFO, Logger.MSG_DEBUG_GUI_ICONS,
        "Mail " + aktion + " verschickt an " + anrede + " " + emailToAdresse);
  }

  private String getFormattedEmailtextMitglied(String anrede, String aktion, String errorText) {
    List<String> msg = new ArrayList<String>();
    msg.add("Hallo " + anrede + "!");
    msg.add("");
    if (errorText != null && !errorText.isBlank()) {
      msg.add("Das Ergebnis Deiner Anfrage lautet: " + aktion);
      msg.add("-->  \"" + errorText + "\"  <--");
      msg.add("");
      switch (aktion) {
        case "CONFIRM_CHANGE_NAME":
        case "CONFIRM_SETMAIL":
        case "CONFIRM_SETPHONENR":
          msg.add("Bitte teile dem Schriftwart des Vereins Deine Änderungen auch mit.");
          msg.add("Du könntest zB. diese Mail an schriftwart@overfreunde.de weiterleiten.");
          msg.add("");
          break;
        default:
          break;
      }
    }
    if (aktion.contains("CONFIRM")) {
      msg.add("Hier ein Auszug Deiner persönlichen Daten bei EFa am Isekai. ");
      msg.add(" Vor- und Nachname: " + getFirstLastName());
      msg.add(" OH-MitgliedNr: " + getMembershipNo());
      msg.add(" Telefon: " + getHandy2() + " " + suppressNull(getFestnetz1()));
      msg.add(" Telefon als Eingabehilfe in EFa "
          + (isErlaubtTelefon() ? "" : "nicht ") + "freigegeben.");
      msg.add(" Email: " + suppressNull(getEmail()));
      msg.add(" Emails versenden ist EFa " + (isErlaubtEmail() ? "" : "nicht ") + "erlaubt.");
      msg.add(" Kürzel: \"" + suppressNull(getInputShortcut()) + "\" (Spitzname)");
      msg.add(" Kürzel ist bei Fahrtbeginn "
          + (isErlaubtKuerzel() ? "benutzbar." : "nicht erwünscht."));
      msg.add(" (" + getStringEingabeAm(getLastModified()) + ")");
      msg.add("");
    }
    msg.add("Solltest Du Deine Daten ändern wollen, dann bitte jemand vom Vorstand, dies zu tun.");
    msg.add("Alternativ kannst Du das Benutzerprofil auch per Klick anpassen: ");
    msg.add(" " + getEfaURL("efa/"));
    msg.add("Allerdings brauchst Du dort zur Identifizierung eine aktuelle Bootsreservierung.");
    msg.add("");
    msg.add("mit freundlichen Grüßen");
    msg.add("EFa-Touchscreen im Bootshaus");
    msg.add("");
    msg.add(International.getMessage("Newsletter abmelden {url}", getEfaURL("abmelden/")));
    return join(msg);
  }

  private String suppressNull(String text) {
    if (text == null) {
      return "";
    }
    return text;
  }

  private boolean isValidEmail(String emailCandidate) {
    if (emailCandidate == null) {
      return false;
    }
    if (emailCandidate.isEmpty()) {
      return false;
    }
    if (!emailCandidate.contains("@")) {
      return false;
    }
    return true;
  }

  private String getStringEingabeAm(long lastModifiziert) {
    if (lastModifiziert == IDataAccess.UNDEFINED_LONG) {
      return "Letzter Änderungszeitpunkt unbekannt";
    } else {
      return "Letzte Änderung am " + new DateTime(lastModifiziert).toString().replace('T', '.');
    }
  }

  private String getEfaURL(String folder) {
    String url = "https://overfreunde.abfx.de/";
    url += folder;
    url += "?mitgliedNr=" + getMembershipNo();
    return url;
  }

  private String join(List<String> list) {
    StringBuilder sb = new StringBuilder();
    for (String single : list) {
      sb.append(single).append("\n");
    }
    return sb.toString();
  }

  public int getAnzahlFahrtenDiesJahrAnVerschiedenenTagen() {
    Logbook logbook = Daten.project.getCurrentLogbook();
    if (logbook == null) {
      Logger.log(Logger.WARNING, Logger.MSG_ABF_WARNING,
          "getAnzahlFahrtenDiesJahrAnVerschiedenenTagen() Kein Fahrtenbuch offen " + logbook);
      return 0;
    }
    return logbook.countPersonUsage(getId(), false /* allowSameDay */);
  }

  public String checkUndAktualisiereHandyNr(String action, String newPhone) {
    if (!isErlaubtTelefon()) {
      return "noQuestion";
    }
    String telnumAusProfil = getFestnetz1();
    if (telnumAusProfil != null && newPhone.contentEquals(telnumAusProfil)) {
      return "noQuestion";
    }
    telnumAusProfil = getHandy2();
    if (telnumAusProfil != null && newPhone.contentEquals(telnumAusProfil)) {
      return "noQuestion"; // Nummer unverändert
    }
    if (telnumAusProfil == null) {
      telnumAusProfil = getFestnetz1();
    }

    // weder noch
    String frage = "Bevor es losgeht... eine Frage zu Deinen Benutzereinstellungen:\n";
    frage += "- Heutige Telefonnummer ist: " + newPhone + ",\n";
    frage += "- sonst übliche TelefonNr lautete: " + telnumAusProfil + ".\n";
    frage += "Wenn Du Dich nur vertippt hast, drücke bitte die Taste ESC auf der Tastatur oben links.\n";
    frage += "\n";
    frage += "Darf sich EFa die neue Nummer merken? ";
    frage += "Soll EFa in Zukunft die neue Nummer vorschlagen?\n";
    frage += "alte Nummer                                   Drücke ESC für zurück                        neue Nummer\n";
    int antwort = Dialog.auswahlDialog("Zukünftige Vorbelegung der Telefonnummer", frage,
        newPhone + " vorschlagen", // 0 ja neue Nummer übernehmen
        "nix mehr vorschlagen", // 1 Erlaubnis entziehen
        telnumAusProfil + " vorschlagen"); // 2 = alte bisherige Nummer
    switch (antwort) {
      case 0: // neue Nummer zukünftig merken (rechts, default, selektiert)
        setHandy2(newPhone);
        setFestnetz1(null);
        setErlaubnisTelefon(true);
        // TODO Dialog "PS: Kennt der Schriftwart Deine neue Nummer schon?"
        return "savedNew"; // muss noch gespeichert werden / persistiert
      case 1: // gar nix mehr vorschlagen
        setHandy2(null);
        setFestnetz1(null);
        setErlaubnisTelefon(false);
        return "savedEmpty"; // muss noch gespeichert werden / persistiert
      case 2: // alten Vorschlag beibehalten (links)
        setHandy2(telnumAusProfil);
        setFestnetz1(null);
        setErlaubnisTelefon(true);
        return "savedOld"; // muss noch gespeichert werden / persistiert

      case 3: // hier könnte ein Button "abbrechen" rein...
        return "abbrechen"; // = nix tun
      case -1: // abbrechen = cancel = ESC = x // zurück, nochmal die Nummer ändern
        return "abbrechen"; // = nix tun
      default: // unbekannt
        return "abbrechen"; // = nix tun
    }
  }

}
