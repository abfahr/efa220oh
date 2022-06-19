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
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
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

  private static final String NEIN_DANKE = "neinDanke";
  // =========================================================================
  // Field Names
  // =========================================================================
  public static final String ID = "Id";
  public static final String FIRSTNAME = "FirstName";
  public static final String LASTNAME = "LastName";
  public static final String FIRSTLASTNAME = "FirstLastName";
  public static final String STATUSID = "StatusId";
  public static final String EMAIL = "Email";
  public static final String ISALLOWEDEMAIL = "erlaubtEmail";
  public static final String ISALLOWEDPHONE = "erlaubtTelefon";
  public static final String ISALLOWEDSHORT = "erlaubtKürzel";
  public static final String HASCHANGEDSPELLNAME = "hatSchreibweiseGeändert";
  public static final String MEMBERSHIPNO = "MembershipNo";
  public static final String EXCLUDEFROMSTATISTIC = "ExcludeFromStatistics";
  public static final String INPUTSHORTCUT = "InputShortcut";
  public static final String FESTNETZ1 = "FreeUse1";
  public static final String HANDY2 = "FreeUse2";
  public static final String[] IDX_NAME_NAME = new String[] { FIRSTLASTNAME };
  private static final String GUIITEM_GROUPS = "GUIITEM_GROUPS";
  private static final String CAT_BASEDATA = "%01%" + International.getString("Basisdaten");
  private static final String CAT_MOREDATA = "%02%" + International.getString("Weitere Daten");
  private static final String CAT_GROUPS = "%03%" + International.getString("Gruppen");

  public static void initialize() {
    Vector<String> f = new Vector<>();
    Vector<Integer> t = new Vector<>();

    f.add(ID);
    t.add(IDataAccess.DATA_UUID);
    f.add(FIRSTNAME);
    t.add(IDataAccess.DATA_STRING);
    f.add(LASTNAME);
    t.add(IDataAccess.DATA_STRING);
    f.add(FIRSTLASTNAME);
    t.add(IDataAccess.DATA_VIRTUAL);
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
    f.add(HASCHANGEDSPELLNAME);
    t.add(IDataAccess.DATA_BOOLEAN);
    f.add(MEMBERSHIPNO);
    t.add(IDataAccess.DATA_STRING);
    f.add(EXCLUDEFROMSTATISTIC);
    t.add(IDataAccess.DATA_BOOLEAN);
    f.add(INPUTSHORTCUT);
    t.add(IDataAccess.DATA_STRING);
    f.add(FESTNETZ1);
    t.add(IDataAccess.DATA_STRING);
    f.add(HANDY2);
    t.add(IDataAccess.DATA_STRING);
    MetaData metaData = constructMetaData(Persons.DATATYPE, f, t, true);
    metaData.setKey(new String[] { ID }); // plus VALID_FROM
    metaData.addIndex(IDX_NAME_NAME);
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
    return getFullName(getString(FIRSTNAME), getString(LASTNAME),
        (alwaysFirstFirst ? true : Daten.efaConfig.getValueNameFormatIsFirstNameFirst()));
  }

  public DataTypeDate getBirthday() {
    return null;
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

  public boolean hatSchreibweiseNameGeaendert() {
    return getBool(HASCHANGEDSPELLNAME);
  }

  public void setSchreibweiseGeaendert(boolean schreibweiseGeaendert) {
    setBool(HASCHANGEDSPELLNAME, schreibweiseGeaendert);
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

  public static String getFullName(String first, String last, boolean firstFirst) {
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
    return s;
  }

  private static String getFirstLastName(String name) {
    if (name == null || name.length() == 0) {
      return "";
    }
    int pos = name.indexOf(", ");
    if (pos < 0) {
      return name;
    }
    return (name.substring(pos + 2) + " " + name.substring(0, pos)).trim();
  }

  @Override
  public String getQualifiedName() {
    return getFullName(getFirstName(), getLastName(),
        Daten.efaConfig.getValueNameFormatIsFirstNameFirst());
  }

  @Override
  public String[] getQualifiedNameFields() {
    return IDX_NAME_NAME;
  }

  @Override
  public String[] getQualifiedNameFieldsTranslateVirtualToReal() {
    return new String[] { FIRSTNAME, LASTNAME };
  }

  @Override
  public String[] getQualifiedNameValues(String qname) {
    return new String[] { getFirstLastName(qname.trim()) };
  }

  public static String[] tryGetFirstLastName(String s) {
    String name = s.trim();

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
    return new String[] { firstName, lastName };
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
    if (fieldName.equals(STATUSID)) {
      return getStatusName();
    }
    return super.getAsText(fieldName);
  }

  @Override
  public boolean setFromText(String fieldName, String value) {
    switch (fieldName) {
      case EMAIL:
        // mit jeder Email auch Erlaubnis setzen
        if (value != null && !value.trim().isBlank()) {
          setErlaubnisEmail(true);
        }
        return super.setFromText(fieldName, value);
      case STATUSID:
        switch (value) {
          case "Mitglieder ausgetreten":
          case "Mitglieder verstorben":
          case "Mitglieder ausgeschlossen":
          case "Passives Mitglied":
          case "Externe Adressen":
          case "andere":
          case "Gast":
            setInvalidFrom(System.currentTimeMillis());
            setDeleted(true);
          default:
        }
        if ("Mitglieder gek?ndigt".equals(value)) {
          value = "Mitglieder gekündigt"; // Umlaute
        }
        Status status = getPersistence().getProject().getStatus(false);
        StatusRecord statusRecord = status.findStatusByName(value);
        if (statusRecord != null) {
          set(fieldName, statusRecord.getId());
        }

        break;
      default:
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
    Vector<IItemType> v = new Vector<>();
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
      v.add(item = new ItemTypeBoolean(PersonRecord.HASCHANGEDSPELLNAME,
          hatSchreibweiseNameGeaendert(),
          IItemType.TYPE_PUBLIC, CAT_MOREDATA,
          International.getString("hat eigene Schreibweise Name")));
      v.add(item = new ItemTypeBoolean(PersonRecord.ISALLOWEDEMAIL, isErlaubtEmail(),
          IItemType.TYPE_PUBLIC, CAT_MOREDATA, International.getString("ist Erlaubt Email")));
      v.add(item = new ItemTypeBoolean(PersonRecord.ISALLOWEDPHONE, isErlaubtTelefon(),
          IItemType.TYPE_PUBLIC, CAT_MOREDATA, International.getString("ist Erlaubt Telefon")));
      v.add(item = new ItemTypeBoolean(PersonRecord.ISALLOWEDSHORT, isErlaubtKuerzel(),
          IItemType.TYPE_PUBLIC, CAT_MOREDATA, International.getString("ist Erlaubt Kürzel")));
      item.setFieldSize(300, -1);
      v.add(item = new ItemTypeBoolean(PersonRecord.EXCLUDEFROMSTATISTIC,
          getExcludeFromPublicStatistics(),
          IItemType.TYPE_PUBLIC, CAT_MOREDATA, International
          .getString("von allgemein verfügbaren Statistiken ausnehmen")));

    } // admin visible

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
        Hashtable<UUID, String> groupIds = new Hashtable<>();
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
    // ist Person ein Mitglied? mit korrektem Status?
    if (!isDyingMember()) {
      return retVal;
    }

    // ist Person ewig gülig, d.h. ohne GültigBisDatum
    if (getInvalidFrom() == Long.MAX_VALUE) {
      return retVal; // ewig gültig
    }

    long now = System.currentTimeMillis();

    // Person erst kürzlich ungültig (dies Jahr)
    Calendar c = Calendar.getInstance();
    c.setTimeInMillis(now);
    int yearActual = c.get(Calendar.YEAR);
    c.setTimeInMillis(getInvalidFrom());
    int yearInvalid = c.get(Calendar.YEAR);
    if (yearActual == yearInvalid) {
      return retVal; // nicht <uy diesem Jahr
    }

    // Person erst kürzlich ungültig (diese Woche)
    long invalidMillisAgo = now - getInvalidFrom();
    long oneWeek = 7 * 24 * 60 * 60 * 1000;
    if (invalidMillisAgo < oneWeek) {
      return retVal; // nicht alt genug
    }

    // jetzt wirklich löschen (zur Löschung markieren)
    setDeleted(true); // doof - Datensatz fehlt dann
    retVal = true;
    return retVal;
  }

  public boolean isDyingMember() {
    if (!isStatusMember()) {
      return true; // Person ist kein Mitglied
    }

    switch (getStatusName()) {
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
    boolean kombinierteEmailErlaubnis;
    kombinierteEmailErlaubnis = isErlaubtEmail();
    if (!isValidEmail(emailToAdresse)) {
      emailToAdresse = "efa+no.invalidEmailMitglied" + Daten.EMAILDEBUG_DOMAIN;
      emailSubject = "invalidEmail " + getFirstLastName() + " ";
      kombinierteEmailErlaubnis = false;
    }
    if (getLastModified() == IDataAccess.UNDEFINED_LONG) {
      emailToAdresse = "efa.error.LastModified";
      emailSubject = "Error LastModified ";
      kombinierteEmailErlaubnis = false;
    }
    emailSubject += "OH Änderung " + aktion;
    if (!kombinierteEmailErlaubnis) {
      emailToAdresse = emailToAdresse.replaceAll("@", ".").trim();
      emailToAdresse = "efa+no." + emailToAdresse + Daten.EMAILDEBUG_DOMAIN;
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
    List<String> msg = new ArrayList<>();
    msg.add("Hallo " + anrede + "!");
    msg.add("");
    if (errorText != null && !errorText.isBlank()) {
      msg.add("Das Ergebnis Deiner Änderung lautet: " + aktion);
      msg.add("-->  \"" + errorText + "\"  <--");
      msg.add("");
    }
    if (aktion.contains("CONFIRM")) {
      msg.add("Hier ein Auszug Deiner persönlichen Daten bei EFA am Isekai. ");
      msg.add(" Vor- und Nachname: " + getFirstLastName());
      msg.add(" OH-MitgliedNr: " + getMembershipNo());
      msg.add(" Telefon: " + getHandy2() + " " + suppressNull(getFestnetz1()));
      msg.add(" Telefon als Eingabehilfe in EFA "
          + (isErlaubtTelefon() ? "" : "nicht ") + "freigegeben.");
      msg.add(" Email: " + suppressNull(getEmail()));
      msg.add(" Emails versenden ist EFA " + (isErlaubtEmail() ? "" : "nicht ") + "erlaubt.");
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
    msg.add("EFA-Touchscreen im Bootshaus");
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
    String url = International.getString("Web Domain EFA Bootshaus");
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

  public String checkUndAktualisiereHandyNr(String newPhone, boolean alleFragen) {
    // true = nur zugesagte Leute werden korrigiert.
    // false = alle Leute werden gefragt, Ausnahme zugesagte Nummer stimmt noch
    String telnumAusProfil = International.getString("keine Nummer bzw nix"); // keine bzw. nix
    if (!alleFragen || isErlaubtTelefon()) {
      if (!isErlaubtTelefon()) {
        return "noQuestion";
      }
      telnumAusProfil = getFestnetz1();
      if (telnumAusProfil != null && newPhone.contentEquals(telnumAusProfil)) {
        if (showHinweisShortCutKürzel()) {
          return "keinHinweisAufKürzelErwünscht";
        }
        return "noQuestion"; // Nummer unverändert
      }
      telnumAusProfil = getHandy2();
      if (telnumAusProfil != null && newPhone.contentEquals(telnumAusProfil)) {
        if (showHinweisShortCutKürzel()) {
          return "keinHinweisAufKürzelErwünscht";
        }
        return "noQuestion"; // Nummer unverändert
      }
      if (telnumAusProfil == null) {
        telnumAusProfil = getFestnetz1();
      }
    }
    if (!isErlaubtTelefon() && "040-040".equals(getHandy2())) {
      return "noQuestion";
    }

    // weder noch
    String frage = International.getMessage(
        "Vorbelegung neu {newPhone} anstelle von {telnumAusProfil}", newPhone, telnumAusProfil);
    int antwort = Dialog.auswahlDialog(International.getString("Vorbelegung der Telefonnummer"),
        frage, newPhone + " vorschlagen", // 0 ja neue Nummer übernehmen
        "nix mehr vorschlagen", // 1 Erlaubnis entziehen
        telnumAusProfil + " vorschlagen"); // 2 = alte bisherige Nummer
    switch (antwort) {
      case 0: // neue Nummer zukünftig merken (rechts, default, selektiert)
        setHandy2(newPhone);
        setFestnetz1(null);
        setErlaubnisTelefon(true);
        return "savedNew"; // muss noch gespeichert werden / persistiert
      case 1: // gar nix mehr vorschlagen
        setHandy2("040-040");
        setFestnetz1(null);
        setErlaubnisTelefon(false);
        return "savedEmpty"; // muss noch gespeichert werden / persistiert
      case 2: // alten Vorschlag beibehalten (links)
        if (isErlaubtTelefon()) {
          setHandy2(telnumAusProfil);
          setFestnetz1(null);
          setErlaubnisTelefon(true);
          return "savedOld"; // muss noch gespeichert werden / persistiert
        } else {
          setHandy2("040-040");
          setFestnetz1(null);
          setErlaubnisTelefon(false);
          return "savedEmpty"; // muss noch gespeichert werden / persistiert
        }
      case 3: // hier könnte ein Button "abbrechen" rein...
        return "abbrechen"; // = nix tun
      case -1: // abbrechen = cancel = ESC = x // zurück, nochmal die Nummer ändern
        return "abbrechen"; // = nix tun
      default: // unbekannt
        return "abbrechen"; // = nix tun
    }
  }

  private boolean showHinweisShortCutKürzel() {
    if (isErlaubtKuerzel()) {
      return false;
    }
    if (getInputShortcut() != null &&
        getInputShortcut().equalsIgnoreCase(NEIN_DANKE)) {
      return false;
    }
    if (getAnzahlFahrtenDiesJahrAnVerschiedenenTagen() < 3) {
      return false;
    }
    String hinweis = "Wenn Du es Leid bist, jedes Mal Deinen vollen Namen einzugeben, dann kannst Du \n";
    hinweis += "Dir ein Kürzel ausdenken und praktischerweise mit Deinem Namen verknüpfen. " + "\n";
    hinweis += "Bitte leg dazu eine Reservierung für ein Boot an und benutze " + "\n";
    hinweis += "den Link zu Deinem Profil aus der Bestätigungs-Email...";
    int antwort = Dialog.yesNoCancelDialog("Hinweis zu Namenskürzeln", hinweis);
    if (antwort == Dialog.NO) {
      setInputShortcut(NEIN_DANKE);
      setErlaubnisKuerzel(false);
      return true;
    }
    return false;
  }

}
