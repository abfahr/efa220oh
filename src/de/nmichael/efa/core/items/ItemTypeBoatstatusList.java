/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.core.items;

import java.awt.Color;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.UUID;
import java.util.Vector;

import javax.swing.JPanel;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.data.BoatDamageRecord;
import de.nmichael.efa.data.BoatDamages;
import de.nmichael.efa.data.BoatRecord;
import de.nmichael.efa.data.BoatStatusRecord;
import de.nmichael.efa.data.Boats;
import de.nmichael.efa.data.GroupRecord;
import de.nmichael.efa.data.Groups;
import de.nmichael.efa.data.Logbook;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.data.PersonRecord;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataKeyIterator;
import de.nmichael.efa.data.types.DataTypeIntString;
import de.nmichael.efa.data.types.DataTypeList;
import de.nmichael.efa.gui.EfaBoathouseFrame;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

public class ItemTypeBoatstatusList extends ItemTypeList {

  public static final int SEATS_OTHER = 99;

  public enum SortingBy {
    EfaSorting, // default EFA-Sorting
    DescriptionOrt, // Tor 1
    BoatType, // Wildwasser
    BoatNameAffix, // (PE, Rot)
    Owner, // Eigentümer Overfreunde
    PaddelArt, // Riggering
    Steuermann // Coxing
  }

  EfaBoathouseFrame efaBoathouseFrame;
  SortingBy sortmode;

  public ItemTypeBoatstatusList(String name,
      int type, String category, String description,
      EfaBoathouseFrame efaBoathouseFrame) {
    super(name, type, category, description);
    this.efaBoathouseFrame = efaBoathouseFrame;
  }

  public int displayOnGui(Window dlg, JPanel panel,
      String borderLayoutOrientation, SortingBy sortmode) {
    this.sortmode = sortmode;
    return displayOnGui(dlg, panel, borderLayoutOrientation);
  }

  public void setBoatStatusData(Vector<BoatStatusRecord> v, Logbook logbook, String other) {
    Vector<ItemTypeListData> vdata = sortBootsList(v, logbook);
    if (other != null) {
      BoatListItem item = new BoatListItem();
      item.text = other;
      vdata.add(0, new ItemTypeListData(other, item, false, -1));
    }
    clearIncrementalSearch();
    list.setSelectedIndex(-1);
    setItems(vdata);
    showValue();
  }

  Vector<ItemTypeListData> sortBootsList(Vector<BoatStatusRecord> vectorBoatStatusRecord,
      Logbook logbook) {
    if (vectorBoatStatusRecord == null
        || vectorBoatStatusRecord.size() == 0
        || logbook == null) {
      return new Vector<ItemTypeListData>();
    }

    long now = System.currentTimeMillis();
    Boats allBoats = Daten.project.getBoats(false);

    Groups groups = Daten.project.getGroups(false);
    Hashtable<UUID, Color> groupColors = new Hashtable<UUID, Color>();
    try {
      DataKeyIterator it = groups.data().getStaticIterator();
      for (DataKey<?, ?, ?> k = it.getFirst(); k != null; k = it.getNext()) {
        GroupRecord gr = (GroupRecord) groups.data().get(k);
        if (gr != null && gr.isValidAt(now)) {
          String cs = gr.getColor();
          if (cs != null && cs.length() > 0) {
            groupColors.put(gr.getId(), EfaUtil.getColorOrGray(cs));
          }
        }
      }
      iconWidth = 0;
      if (groupColors.size() > 0 ||
          Daten.efaConfig.isEfaBoathouseShowSchadenkreisInAllLists()) {
        iconWidth = Daten.efaConfig.getValueEfaDirekt_fontSize();
      }
      iconHeight = iconWidth;
    } catch (Exception e) {
      Logger.logdebug(e);
    }

    BoatDamages boatDamages = Daten.project.getBoatDamages(false);
    Vector<BoatString> vectorBoatString = new Vector<BoatString>();
    for (int i = 0; i < vectorBoatStatusRecord.size(); i++) {
      BoatStatusRecord myBoatStatusRecord = vectorBoatStatusRecord.get(i);

      BoatRecord myBoatRecord = allBoats.getBoat(myBoatStatusRecord.getBoatId(), now);
      Hashtable<Integer, Integer> allSeats = new Hashtable<Integer, Integer>(); // seats -> variant
      // find all seat variants to be shown...
      if (myBoatRecord != null) {
        if (myBoatRecord.getNumberOfVariants() == 1) {
          allSeats.put(myBoatRecord.getNumberOfSeats(0, SEATS_OTHER),
              myBoatRecord.getTypeVariant(0));
        } else {
          if (myBoatStatusRecord.getCurrentStatus().equals(BoatStatusRecord.STATUS_AVAILABLE)) {
            for (int j = 0; j < myBoatRecord.getNumberOfVariants(); j++) {
              // if the boat is available, show the boat in all seat variants
              allSeats.put(myBoatRecord.getNumberOfSeats(j, SEATS_OTHER),
                  myBoatRecord.getTypeVariant(j));
            }
          } else {
            if (myBoatStatusRecord.getCurrentStatus().equals(BoatStatusRecord.STATUS_ONTHEWATER)) {
              // if the boat is on the water, show the boat
              // in the variant that it is currently being used in
              DataTypeIntString entry = myBoatStatusRecord.getEntryNo();
              if (entry != null && entry.length() > 0) {
                LogbookRecord lr = logbook.getLogbookRecord(myBoatStatusRecord.getEntryNo());
                if (lr != null && lr.getBoatVariant() > 0
                    && lr.getBoatVariant() <= myBoatRecord.getNumberOfVariants()) {
                  allSeats.put(
                      myBoatRecord.getNumberOfSeats(
                          myBoatRecord.getVariantIndex(lr.getBoatVariant()), SEATS_OTHER),
                      lr.getBoatVariant());
                }
              }
            }
          }
        }
        if (allSeats.size() == 0) {
          // just show the boat in any variant
          int vd = myBoatRecord.getDefaultVariant();
          if (vd < 1) {
            vd = myBoatRecord.getTypeVariant(0);
          }
          allSeats.put(myBoatRecord.getNumberOfSeats(0, SEATS_OTHER), vd);
        }
      } else {
        if (myBoatStatusRecord.getUnknownBoat()) {
          // unknown boat
          allSeats.put(SEATS_OTHER, -1);
        } else {
          // BoatRecord not found;
          // may be a boat which has a status,
          // but is invalid at timestamp "now"
          // don't add seats for this boat;
          // it should *not* appear in the list
        }
      }
      BoatDamageRecord[] damages = boatDamages.getBoatDamages(myBoatStatusRecord.getBoatId(), true,
          true);

      Integer[] seats = allSeats.keySet().toArray(new Integer[0]);
      for (Integer seat2 : seats) {
        int variant = allSeats.get(seat2);

        if (myBoatRecord != null
            && seats.length < myBoatRecord.getNumberOfVariants()) {
          // we have multiple variants, but all with the same number of seats
          if (myBoatRecord.getDefaultVariant() > 0) {
            variant = myBoatRecord.getDefaultVariant();
          }
        }

        BoatString myBoatString = new BoatString();

        // Seats
        int seat = seat2;
        if (seat == 0) {
          seat = SEATS_OTHER;
        }
        if (seat < 0) {
          seat = 0;
        }
        if (seat > SEATS_OTHER) {
          seat = SEATS_OTHER;
        }
        myBoatString.seats = seat;
        myBoatString.variant = variant;

        myBoatString.name = "";
        if (myBoatRecord != null) {
          myBoatString.name = myBoatRecord.getQualifiedName();
          if (Daten.efaConfig.isEfaBoathouseShowOrtDescriptionInAvailableList()) {
            if (sortmode == SortingBy.EfaSorting || sortmode == SortingBy.BoatType) {
              String suffix = myBoatRecord.getTypeDescription(0);
              if (suffix != null &&
                  !suffix.isBlank() &&
                  !suffix.equals("null")) {
                myBoatString.name += " \"" + suffix + "\"";
              }
            }
          }
        }
        // for BoatsOnTheWater, don't use the "real" boat name,
        // but rather what's stored in the boat status as "BoatText"
        if (myBoatStatusRecord.getCurrentStatus().equals(BoatStatusRecord.STATUS_ONTHEWATER)) {
          myBoatString.name = myBoatStatusRecord.getBoatText();
        }
        // myBoatString.name = "" + myBoatString.name + ""; // Prefix + Postfix
        myBoatString.sortBySeats = (Daten.efaConfig.getValueEfaDirekt_sortByAnzahl());
        myBoatString.sortKategorie = getSortingItem(myBoatRecord, myBoatStatusRecord);

        // Colors for Groups
        ArrayList<Color> aColors = new ArrayList<Color>();
        if (damages != null) {
          int farbe = 236;
          int count = 0;
          for (BoatDamageRecord damage : damages) {
            if (!damage.getFixed()) {
              count++;
              switch (count % 6) {
                case 1:
                  aColors.add(Color.RED);
                  break;
                case 2:
                  aColors.add(Color.BLUE);
                  break;
                case 3:
                  aColors.add(Color.ORANGE);
                  break;
                case 4:
                  aColors.add(Color.GREEN);
                  break;
                case 5:
                  aColors.add(Color.MAGENTA);
                  break;
                case 0:
                  aColors.add(Color.YELLOW);
                  break;
                default:
                  aColors.add(Color.BLACK);
                  break;
              }
            }
          }
        }
        if (myBoatRecord != null) {
          DataTypeList<UUID> grps = myBoatRecord.getAllowedGroupIdList();
          if (grps != null && grps.length() > 0) {
            for (int g = 0; g < grps.length(); g++) {
              UUID id = grps.get(g);
              Color c = groupColors.get(id);
              if (c != null) {
                aColors.add(c);
              }
            }
          }
        }
        Color[] colors = (aColors.size() > 0 ? aColors.toArray(new Color[0]) : null);

        BoatListItem myBoatListItem = new BoatListItem();
        myBoatListItem.list = this;
        myBoatListItem.text = myBoatString.name;
        myBoatListItem.boatStatus = myBoatStatusRecord;
        myBoatListItem.boatVariant = myBoatString.variant;
        myBoatString.colors = colors;
        myBoatString.record = myBoatListItem;

        if (sortmode == SortingBy.BoatType &&
            Daten.efaConfig.isEfaBoathouseShowBoatUsageStatisticsInAllLists() &&
            efaBoathouseFrame.isToggleF12LangtextF12()) {
          int frequency = logbook.countBoatUsage(myBoatStatusRecord.getBoatId());
          myBoatString.name = "(" + String.format("%1$3s", frequency) + "x) " + myBoatString.name;
        }

        if (Daten.efaConfig.getValueEfaDirekt_showZielnameFuerBooteUnterwegs() &&
            BoatStatusRecord.STATUS_ONTHEWATER.equals(myBoatStatusRecord.getCurrentStatus()) &&
            myBoatStatusRecord.getEntryNo() != null
            && myBoatStatusRecord.getEntryNo().length() > 0) {
          LogbookRecord lr = logbook.getLogbookRecord(myBoatStatusRecord.getEntryNo());
          if (lr != null) {
            String suffix = lr.getDestinationAndVariantName();
            if (suffix != null &&
                suffix.length() > 0 &&
                !suffix.isBlank() &&
                !suffix.contains(International.getString("Fehlermeldung PrivatMitVertrag")) &&
                !suffix.contains(International.getString("Fehlermeldung bei langerAusleihe")) &&
                !suffix.equals("null")) {
              myBoatString.name += "     -> \"" + suffix + "\"";
            }
          }
        }

        vectorBoatString.add(myBoatString);
        if (!myBoatString.sortBySeats) {
          break;
        }
      }
    }

    BoatString[] arrayBoatStrings = new BoatString[vectorBoatString.size()];
    for (int i = 0; i < arrayBoatStrings.length; i++) {
      arrayBoatStrings[i] = vectorBoatString.get(i);
    }
    Arrays.sort(arrayBoatStrings);

    Vector<ItemTypeListData> retValList = new Vector<ItemTypeListData>();
    int anz = -1;
    String lastSep = null;
    for (BoatString myBoatStringElement : arrayBoatStrings) {
      if (sortmode != null) {
        String s = myBoatStringElement.sortKategorie;
        String newSep = "--------- " + s + " -------------";
        if (!newSep.equals(lastSep)) {
          retValList.add(new ItemTypeListData(newSep, null, true, anz));
        }
        lastSep = newSep;
      } else if (myBoatStringElement.seats != anz) {
        String s = null;
        switch (myBoatStringElement.seats) {
          case 1:
            s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_1);
            break;
          case 2:
            s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_2);
            break;
          case 3:
            s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_3);
            break;
          case 4:
            s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_4);
            break;
          case 5:
            s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_5);
            break;
          case 6:
            s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_6);
            break;
          case 8:
            s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_8);
            break;
          default:
            s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, "" + myBoatStringElement.seats);
            break;
        }
        if (s == null || s.equals(EfaTypes.getStringUnknown())) {
          /*
           * @todo (P5) Doppeleinträge currently not supported in efa2 DatenFelder d =
           * Daten.fahrtenbuch
           * .getDaten().boote.getExactComplete(removeDoppeleintragFromBootsname(a[i].name)); if (d
           * != null) { s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS,
           * d.get(Boote.ANZAHL)); } else {
           */
          s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_OTHER);
          // }
        }
        if (s != null) {
          s = s.trim();
        }
        anz = myBoatStringElement.seats;
        String newSep = "--------- " + s + " -------------";
        if (!newSep.equals(lastSep)) {
          retValList.add(new ItemTypeListData(newSep, null, true, anz));
        }
        lastSep = newSep;
      }
      retValList.add(new ItemTypeListData(
          myBoatStringElement.name,
          myBoatStringElement.record,
          false, -1, null,
          myBoatStringElement.colors));
    }
    return retValList;
  }

  private String getSortingItem(BoatRecord aBoatRecord, BoatStatusRecord aBoatStatusRecord) {
    String sortString = null;
    if (sortmode == null || aBoatRecord == null) {
      return sortString;
    }
    String currentStatus = aBoatStatusRecord.getCurrentStatus();
    switch (sortmode) {
      case EfaSorting:
        sortString = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS,
            aBoatRecord.getTypeSeats(0));
        if (currentStatus == null) {
          break;
        }
        switch (currentStatus) {
          case BoatStatusRecord.STATUS_AVAILABLE:
            break;
          case BoatStatusRecord.STATUS_ONTHEWATER:
            DataTypeIntString entryNoLogbook = aBoatStatusRecord.getEntryNo();
            LogbookRecord lr = efaBoathouseFrame.getLogbook().getLogbookRecord(entryNoLogbook);
            if (lr != null) {
              sortString = lr.getDestinationAndVariantName();
            } else {
              Logger.log(Logger.WARNING, Logger.MSG_WARN_CANTEXECCOMMAND,
                  "Problem kein Fahrtenbucheintrag für " + entryNoLogbook, false);
            }
            break;
          case BoatStatusRecord.STATUS_NOTAVAILABLE:
            break;
          default:
            break;
        }
        break;
      case DescriptionOrt:
        if (currentStatus == null) {
          break;
        }
        switch (currentStatus) {
          case BoatStatusRecord.STATUS_AVAILABLE:
            sortString = aBoatRecord.getTypeDescription(0);
            break;
          case BoatStatusRecord.STATUS_ONTHEWATER:
            sortString = aBoatRecord.getTypeDescription(0);
            break;
          case BoatStatusRecord.STATUS_NOTAVAILABLE:
            sortString = aBoatStatusRecord.getComment();
            sortString = aBoatRecord.getTypeDescription(0);
            break;
          default:
            sortString = aBoatStatusRecord.getComment();
            sortString = aBoatRecord.getTypeDescription(0);
            break;
        }
        break;
      case BoatType:
        sortString = Daten.efaTypes.getValue(EfaTypes.CATEGORY_BOAT, aBoatRecord.getTypeType(0));
        break;
      case BoatNameAffix:
        sortString = aBoatRecord.getNameAffix();
        break;
      case Owner:
        sortString = aBoatRecord.getOwner();
        break;
      case PaddelArt:
        sortString = Daten.efaTypes.getValue(EfaTypes.CATEGORY_RIGGING,
            aBoatRecord.getTypeRigging(0));
        break;
      case Steuermann:
        sortString = Daten.efaTypes.getValue(EfaTypes.CATEGORY_COXING,
            aBoatRecord.getTypeCoxing(0));
        break;
      default:
        break;
    }
    if (sortString != null && sortString.isBlank()) {
      sortString = null;
    }
    return sortString;
  }

  public void setPersonStatusData(Vector<PersonRecord> v, String other) {
    Vector<ItemTypeListData> vdata = sortMemberList(v);
    if (other != null) {
      vdata.add(0, new ItemTypeListData(other, null, false, -1));
    }
    clearIncrementalSearch();
    list.setSelectedIndex(-1);
    setItems(vdata);
    showValue();
  }

  Vector sortMemberList(Vector<PersonRecord> v) {
    if (v == null || v.size() == 0) {
      return v;
    }
    BoatString[] arrayBoatStrings = new BoatString[v.size()];
    for (int i = 0; i < v.size(); i++) {
      PersonRecord pr = v.get(i);
      arrayBoatStrings[i] = new BoatString();
      arrayBoatStrings[i].seats = SEATS_OTHER;
      arrayBoatStrings[i].name = pr.getQualifiedName();
      arrayBoatStrings[i].sortBySeats = false;
      BoatListItem item = new BoatListItem();
      item.list = this;
      item.text = arrayBoatStrings[i].name;
      item.person = pr;
      arrayBoatStrings[i].record = item;
    }
    Arrays.sort(arrayBoatStrings);

    Vector<ItemTypeListData> vv = new Vector<ItemTypeListData>();
    char lastChar = ' ';
    for (BoatString element : arrayBoatStrings) {
      String name = element.name;
      if (name.length() > 0) {
        if (name.toUpperCase().charAt(0) != lastChar) {
          lastChar = name.toUpperCase().charAt(0);
          vv.add(new ItemTypeListData("---------- " + lastChar + " ----------", null, true,
              SEATS_OTHER));
        }
        vv.add(new ItemTypeListData(name, element.record, false, SEATS_OTHER));
      }
    }
    return vv;
  }

  public BoatListItem getSelectedBoatListItem() {
    if (list == null || list.isSelectionEmpty()) {
      return null;
    } else {
      Object o = getSelectedValue();
      if (o != null) {
        return (BoatListItem) o;
      }
      return null;
    }
  }

  public static BoatListItem createBoatListItem(int mode) {
    BoatListItem b = new BoatListItem();
    b.mode = mode;
    return b;
  }

  public static class BoatListItem {
    public int mode;
    public ItemTypeBoatstatusList list;
    public String text;
    public BoatRecord boat;
    public BoatStatusRecord boatStatus;
    public int boatVariant = 0;
    public PersonRecord person;
  }

  class BoatString implements Comparable<BoatString> {

    public String name;
    public String sortKategorie;
    public int seats;
    public boolean sortBySeats;
    public Object record;
    public int variant;
    public Color[] colors;

    private String normalizeString(String s) {
      if (s == null) {
        return "";
      }
      s = s.toLowerCase();
      if (s.indexOf("ä") >= 0) {
        s = EfaUtil.replace(s, "ä", "a", true);
      }
      if (s.indexOf("Ä") >= 0) {
        s = EfaUtil.replace(s, "Ä", "a", true);
      }
      if (s.indexOf("à") >= 0) {
        s = EfaUtil.replace(s, "à", "a", true);
      }
      if (s.indexOf("á") >= 0) {
        s = EfaUtil.replace(s, "á", "a", true);
      }
      if (s.indexOf("â") >= 0) {
        s = EfaUtil.replace(s, "â", "a", true);
      }
      if (s.indexOf("ã") >= 0) {
        s = EfaUtil.replace(s, "ã", "a", true);
      }
      if (s.indexOf("æ") >= 0) {
        s = EfaUtil.replace(s, "æ", "ae", true);
      }
      if (s.indexOf("ç") >= 0) {
        s = EfaUtil.replace(s, "ç", "c", true);
      }
      if (s.indexOf("è") >= 0) {
        s = EfaUtil.replace(s, "è", "e", true);
      }
      if (s.indexOf("é") >= 0) {
        s = EfaUtil.replace(s, "é", "e", true);
      }
      if (s.indexOf("è") >= 0) {
        s = EfaUtil.replace(s, "è", "e", true);
      }
      if (s.indexOf("é") >= 0) {
        s = EfaUtil.replace(s, "é", "e", true);
      }
      if (s.indexOf("ê") >= 0) {
        s = EfaUtil.replace(s, "ê", "e", true);
      }
      if (s.indexOf("ì") >= 0) {
        s = EfaUtil.replace(s, "ì", "i", true);
      }
      if (s.indexOf("í") >= 0) {
        s = EfaUtil.replace(s, "í", "i", true);
      }
      if (s.indexOf("î") >= 0) {
        s = EfaUtil.replace(s, "î", "i", true);
      }
      if (s.indexOf("ñ") >= 0) {
        s = EfaUtil.replace(s, "ñ", "n", true);
      }
      if (s.indexOf("ö") >= 0) {
        s = EfaUtil.replace(s, "ö", "o", true);
      }
      if (s.indexOf("Ö") >= 0) {
        s = EfaUtil.replace(s, "Ö", "o", true);
      }
      if (s.indexOf("ò") >= 0) {
        s = EfaUtil.replace(s, "ò", "o", true);
      }
      if (s.indexOf("ó") >= 0) {
        s = EfaUtil.replace(s, "ó", "o", true);
      }
      if (s.indexOf("ô") >= 0) {
        s = EfaUtil.replace(s, "ô", "o", true);
      }
      if (s.indexOf("õ") >= 0) {
        s = EfaUtil.replace(s, "õ", "o", true);
      }
      if (s.indexOf("ø") >= 0) {
        s = EfaUtil.replace(s, "ø", "o", true);
      }
      if (s.indexOf("ü") >= 0) {
        s = EfaUtil.replace(s, "ü", "u", true);
      }
      if (s.indexOf("Ü") >= 0) {
        s = EfaUtil.replace(s, "Ü", "u", true);
      }
      if (s.indexOf("ù") >= 0) {
        s = EfaUtil.replace(s, "ù", "u", true);
      }
      if (s.indexOf("ú") >= 0) {
        s = EfaUtil.replace(s, "ú", "u", true);
      }
      if (s.indexOf("û") >= 0) {
        s = EfaUtil.replace(s, "û", "u", true);
      }
      if (s.indexOf("ß") >= 0) {
        s = EfaUtil.replace(s, "ß", "ss", true);
      }
      return s;
    }

    @Override
    public int compareTo(BoatString other) {
      String sThis = normalizeString(sortKategorie) + normalizeString(name);
      String sOther = normalizeString(other.sortKategorie) + normalizeString(other.name);
      return sThis.compareTo(sOther);
    }
  }

}
