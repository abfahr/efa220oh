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
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.nmichael.efa.Daten;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.gui.SimpleOptionInputDialog;
import de.nmichael.efa.gui.util.AutoCompleteList;
import de.nmichael.efa.gui.util.AutoCompletePopupWindow;
import de.nmichael.efa.gui.util.AutoCompletePopupWindowCallback;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;

public class ItemTypeStringAutoComplete extends ItemTypeString implements
    AutoCompletePopupWindowCallback {

  private enum Mode {
    none, normal, up, delete, enter, escape
  }

  protected boolean showButton;
  protected boolean popupComplete;
  protected JButton button;
  protected Color originalButtonColor;
  protected AutoCompleteList autoCompleteList;
  protected Object rememberedId;
  protected boolean withPopup = true;
  protected boolean valueIsKnown = false;
  protected boolean isCheckSpelling = false;
  protected boolean isCheckPermutations = false;
  protected boolean isVisibleSticky = true;
  protected String ignoreEverythingAfter = null;
  protected String alternateFieldNameForPlainText = null;
  protected boolean alwaysReturnPlainText = false;
  protected ItemTypeDate validAtDateItem;
  protected ItemTypeTime validAtTimeItem;
  protected boolean alwaysTitleCase = false;
  protected boolean datenschutzModus = false;

  public ItemTypeStringAutoComplete(String name, String value, int type,
      String category, String description, boolean showButton) {
    super(name, value, type, category, description);
    this.showButton = showButton;
    this.popupComplete = Daten.efaConfig == null || Daten.efaConfig.getValuePopupComplete();
    if (isPersonListHidden(name)) {
      this.showButton = false;
      this.popupComplete = false;
      this.alwaysTitleCase = true;
      this.datenschutzModus = true;
    }
  }

  private boolean isPersonListHidden(String fieldname) {
    if (fieldname == null) {
      return false;
    }
    if (Daten.isAdminMode()) {
      return false; // kein filter
    }
    if (fieldname == "PersonId"
        || fieldname == "CoxName"
        || fieldname.matches("Crew.Name")
        || fieldname.matches("Crew..Name")
        || fieldname.matches("PersonList_._PersonId")
        || fieldname.matches("PersonList_.._PersonId")
        || fieldname == "ReportedByPersonId"
        || fieldname == "FixedByPersonId"
        || fieldname == "From" /* Emails */) {
      return true;
    } else if (fieldname == "BOAT"
        || fieldname == "BoatName"
        || fieldname == "DestinationName"
        || fieldname == "GUIITEM_ADDITIONALWATERS"
        || fieldname == "SessionGroupId") {
      return false;
    }
    return false; // fuer Breakpoint
  }

  public ItemTypeStringAutoComplete(String name, String value, int type,
      String category, String description, boolean showButton,
      AutoCompleteList autoCompleteList) {
    super(name, value, type, category, description);
    this.showButton = showButton;
    this.popupComplete = Daten.efaConfig == null || Daten.efaConfig.getValuePopupComplete();
    setAutoCompleteData(autoCompleteList);
  }

  public void setValidAt(ItemTypeDate validAtDate, ItemTypeTime validAtTime) {
    this.validAtDateItem = validAtDate;
    this.validAtTimeItem = validAtTime;
  }

  @Override
  public void iniDisplay() {
    if (showButton) {
      button = new JButton();
      originalButtonColor = button.getBackground();
      Dialog.setPreferredSize(button, fieldHeight - 4, fieldHeight - 8);
      button.addActionListener(new java.awt.event.ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          buttonPressed(e);
        }
      });
      button.addFocusListener(new java.awt.event.FocusAdapter() {
        @Override
        public void focusLost(FocusEvent e) {
          field_focusLost(e);
        }
      });
    }
    super.iniDisplay();
    ((JTextField) field).addKeyListener(new java.awt.event.KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        autoComplete(e);
      }
    });
  }

  @Override
  public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
    int plusy = super.displayOnGui(dlg, panel, x, y);
    if (button != null) {
      panel.add(button, new GridBagConstraints(x + labelGridWidth + fieldGridWidth, y, 1,
          fieldGridHeight, 0.0, 0.0,
          GridBagConstraints.WEST, GridBagConstraints.NONE,
          new Insets(padYbefore, 0, padYafter, 0), 0, 0));
    }
    return plusy;
  }

  public void setAutoCompleteData(AutoCompleteList autoCompleteList) {
    this.autoCompleteList = autoCompleteList;
  }

  public AutoCompleteList getAutoCompleteData() {
    return this.autoCompleteList;
  }

  public void setChecks(boolean checkSpelling, boolean checkPermutations) {
    this.isCheckSpelling = checkSpelling;
    this.isCheckPermutations = checkPermutations;
  }

  public void setIgnoreEverythingAfter(String s) {
    ignoreEverythingAfter = s;
  }

  @Override
  public void setVisible(boolean visible) {
    if (visible == true && isVisibleSticky() == false) {
      return;
    }
    super.setVisible(visible);
    if (button != null) {
      button.setVisible(visible);
    }
  }

  // used to hide input fields in EfaBoathouseFrame that remain invisible, even if the
  // setCrewRangeSelection(i) would want to make them visible again
  public void setVisibleSticky(boolean visible) {
    isVisibleSticky = visible;
    setVisible(visible);
  }

  public boolean isVisibleSticky() {
    return isVisibleSticky;
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    if (button != null) {
      button.setEnabled(enabled);
    }
  }

  @Override
  public void showValue() {
    super.showValue();
    autoComplete(null);
  }

  public void setId(Object id) {
    if (autoCompleteList != null && id != null) {
      value = autoCompleteList.getValueForId(id.toString());
    }
  }

  public Object getId(String qname) {
    return (autoCompleteList != null ? autoCompleteList.getId(qname) : null);
  }

  // the following methods are for bypassing autoCompleteList and misusing
  // ItemTypeStringAutoComplete as an item to store an id for the text it is
  // displaying, without using any auto complete functionality
  public void setRememberedId(Object id) {
    rememberedId = id;
  }

  public Object getRememberedId() {
    return rememberedId;
  }

  @Override
  protected void field_focusLost(FocusEvent e) {
    if (e != null && e.isTemporary()) {
      // avoid that the popup window disappears when showing on a JDialog: with JDialog, we receive
      // temporary focusLost events all the time...
      return;
    }
    if (popupComplete) {
      AutoCompletePopupWindow.hideWindow();
    }
    if (isCheckSpelling && Daten.efaConfig != null
        && Daten.efaConfig.getValueCorrectMisspelledNames()) {
      checkSpelling();
    }
    super.field_focusLost(e);
  }

  private String convertToTitleCase(String lowercaseName) {
    // these cause the character following // to be capitalized
    final String ACTIONABLE_DELIMITERS = " '-/";

    StringBuilder sb = new StringBuilder();
    boolean capNext = true;

    for (char c : lowercaseName.toCharArray()) {
      c = (capNext)
          ? Character.toUpperCase(c)
          : Character.toLowerCase(c);
      sb.append(c);
      capNext = (ACTIONABLE_DELIMITERS.indexOf(c) >= 0); // explicit cast not needed
    }
    return sb.toString();
  }

  public void showOrRemoveAutoCompletePopupWindow() {
    if (popupComplete) {
      JTextField f = (JTextField) field;
      if (f.isEnabled() && f.isEditable()) {
        if (!AutoCompletePopupWindow.isShowingAt(f)) {
          AutoCompletePopupWindow.hideWindow();
          try {
            Thread.sleep(50);
          } catch (InterruptedException eignore) {}
          AutoCompletePopupWindow.showAndSelect(f, autoCompleteList, f.getText(), null);
        } else {
          autoComplete(null);
          AutoCompletePopupWindow.hideWindow();
        }
      }
    }
  }

  private void buttonPressed(ActionEvent e) {
    showOrRemoveAutoCompletePopupWindow();
    actionEvent(e);
  }

  private void autoComplete(KeyEvent e) {
    if (field == null) {
      return;
    }

    JTextField field = (JTextField) this.field;

    AutoCompleteList list = getAutoCompleteList();
    if (list == null) {
      setButtonColor(null);
      return;
    } else {
      list.update();
    }

    if (e != null && e.getKeyCode() == -23) {
      return; // dieses Key-Event wurde von AutoCompletePopupWindow generiert
    }

    if (field.getText().trim().length() == 0) {
      setButtonColor(null);
    }

    String complete = null;
    String prefix = null;
    String base = null;

    Mode mode = Mode.none; // 0
    if (e == null
        || (EfaUtil.isRealChar(e) && e.getKeyCode() != KeyEvent.VK_ENTER)
        || e.getKeyCode() == KeyEvent.VK_DOWN) {
      mode = Mode.normal; // 1
    } else if (e.getKeyCode() == KeyEvent.VK_UP) {
      mode = Mode.up; // 2
    } else if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
      mode = Mode.delete; // 3
    } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
      mode = Mode.enter; // 4
    } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
      mode = Mode.escape; // 5
    }

    // System.out.println("autoComplete("+e+") on "+getName()+" with text '"+field.getText()+"' in
    // mode "+mode);
    if (e == null || mode == Mode.enter || mode == Mode.escape) {
      field.setText(field.getText().trim());
    }

    boolean matching = false;

    if (mode == Mode.normal
        || ((mode == Mode.enter || mode == Mode.escape || mode == Mode.none)
            && field.getText().length() > 0)) {

      // remove leading spaces
      String spc = field.getText();
      if (spc.startsWith(" ")) {
        int i = 0;
        do {
          i++;
        } while (i < spc.length() && spc.charAt(i) == ' ');
        if (i >= spc.length()) {
          field.setText("");
        } else {
          field.setText(spc.substring(i));
        }
      }

      if (field.getSelectedText() != null) {
        prefix = field.getText().toLowerCase().substring(0, field.getSelectionStart());
      } else {
        prefix = field.getText().toLowerCase();
      }

      if (e != null && e.getKeyCode() == KeyEvent.VK_DOWN && !datenschutzModus) {
        if (withPopup && popupComplete && AutoCompletePopupWindow.isShowingAt(field)) {
          complete = list.getNext();
        } else {
          complete = list.getNext(prefix);
        }
        if (complete == null) {
          complete = list.getFirst(prefix);
        }
      } else {

        if (e != null) {
          if (!datenschutzModus) { // abf dies muss verboten werden
            complete = list.getFirst(prefix); // Taste gedrückt --> OK, Wortanfang genügt
          }
        } else {
          complete = list.getExact(field.getText().toLowerCase()); // keine Taste gedrückt --> nur
          // richtig, wenn gesamtes Feld exakt vorhanden!
        }
        String aliasCandidate = list.getAlias(prefix);
        if (aliasCandidate != null) {
          Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_AUTOCOMPLETE,
              "Formular: Kürzel " + prefix + " wird zu " + aliasCandidate);
          complete = aliasCandidate;
          prefix = aliasCandidate;
        }

      }
      if (e == null && complete != null) {
        complete = list.getExact(complete);
      }
      if (complete != null) {
        if (e != null && mode != Mode.none) { // nur bei wirklichen Eingaben
          field.setText(complete);
          field.select(prefix.length(), complete.length());
        }
        matching = true;
      } else {
        if (alwaysTitleCase) {
          field.setText(convertToTitleCase(prefix));
        }
      }
      if (withPopup && popupComplete && e != null && mode != Mode.none) {
        AutoCompletePopupWindow
            .showAndSelect(field, list, (complete != null ? complete : ""), null);
      }
    }

    if (mode == Mode.up && !datenschutzModus) {
      if (field.getSelectedText() != null) {
        prefix = field.getText().toLowerCase().substring(0, field.getSelectionStart());
      } else {
        prefix = field.getText().toLowerCase();
      }

      if (withPopup && popupComplete && AutoCompletePopupWindow.isShowingAt(field)) {
        complete = list.getPrev();
      } else {
        complete = list.getPrev(prefix);
      }
      if (complete == null) {
        complete = list.getLast(prefix); // liste.getFirst(anf);
      }
      if (complete != null) {
        field.setText(complete);
        field.select(prefix.length(), complete.length());
        matching = true;
      }
      if (withPopup && popupComplete) {
        AutoCompletePopupWindow
            .showAndSelect(field, list, (complete != null ? complete : ""), null);
      }
    }

    if (mode == Mode.delete) {
      if ((complete = list.getFirst(field.getText().toLowerCase().trim())) != null
          && (complete.equals(field.getText()))) {
        matching = true;
      }
    }

    // make sure to accept value as known which have a known base part (everything before
    // ignoreEverythingAfter)
    String ignoredString = null;
    int ignorePos = -1;
    for (int i = 0; ignoreEverythingAfter != null && i < ignoreEverythingAfter.length(); i++) {
      ignorePos = (prefix != null ? prefix.indexOf(ignoreEverythingAfter.charAt(i))
          : field.getText().indexOf(ignoreEverythingAfter.charAt(i)));
      if (ignorePos >= 0) {
        break;
      }
    }
    if (ignorePos >= 0) {
      String s = (prefix != null ? prefix : field.getText());
      base = s.substring(0, ignorePos).trim();
      ignoredString = s.substring(ignorePos + 1).trim();
      if (ignoredString.length() == 0) {
        ignoredString = null;
      }
    }
    if (base != null && !matching) {
      String firstInList = list.getFirst(base.trim());
      if (firstInList != null && field.getText().startsWith(firstInList)) {
        matching = true;
      }
    }

    // make sure to accept any values with trailing spaces as well
    if (prefix != null && !matching && prefix.endsWith(" ")) {
      String firstInList = list.getFirst(prefix.trim());
      if (firstInList != null && field.getText().startsWith(firstInList)) {
        matching = true;
      }
    }

    // in case of versionized data, make sure it also valid
    boolean valid = false;
    if (matching && validAtDateItem != null) {
      long t = LogbookRecord.getValidAtTimestamp(validAtDateItem.getDate(),
          (validAtTimeItem != null ? validAtTimeItem.getTime() : null));
      if (ignoredString == null && complete != null) {
        valid = autoCompleteList.isValidAt(complete, t);
      }
      if (ignoredString != null && base != null) {
        valid = autoCompleteList.isValidAt(base, t);
      }
      if (!valid) {
        matching = false;
      }
    } else {
      valid = true;
    }

    if (matching) {
      setButtonColor((ignoredString == null ? Color.green : Color.yellow));
    } else {
      setButtonColor((valid ? Color.red : Color.orange));
    }
    if (Logger.isTraceOn(Logger.TT_GUI, 5)) {
      Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_AUTOCOMPLETE,
          "field=" + field.getText()
              + ", complete=" + complete
              + ", matching=" + matching
              + ", valid=" + valid
              + ", validAtDateItem=" + validAtDateItem
              + ", ignoredString=" + ignoredString);
    }
    // System.out.println("autoComplete("+e+") on "+getName()+" with text '"+field.getText()+"' ->
    // matching="+matching);

    if (mode == Mode.enter) {
      field.select(-1, -1);
      field.setCaretPosition(field.getText().length());
      if (withPopup && popupComplete) {
        AutoCompletePopupWindow.hideWindow();
      }
    }

    if (mode == Mode.escape) {
      if (withPopup && popupComplete) {
        AutoCompletePopupWindow.hideWindow();
      }
    }

    if (field.getText().length() == 0) {
      setButtonColor(null);
    }
  }

  private void checkSpelling() {
    String name = getValueFromField().trim();
    if (name.length() == 0) {
      return;
    }

    int ignorePos = -1;
    for (int i = 0; ignoreEverythingAfter != null && i < ignoreEverythingAfter.length(); i++) {
      ignorePos = name.indexOf(ignoreEverythingAfter.charAt(i));
      if (ignorePos >= 0) {
        break;
      }
    }
    if (ignorePos >= 0) {
      name = name.substring(0, ignorePos).trim();
    }

    AutoCompleteList list = getAutoCompleteList();
    if (list == null) {
      return;
    }

    Vector<String> neighbours = null;
    if (list.getExact(name.toLowerCase()) == null) {
      int radius = (name.length() < 6 ? name.length() / 2 : 3);
      neighbours = list.getNeighbours(name, radius, (isCheckPermutations ? 6 : 0));
    }
    if (neighbours != null && neighbours.size() > 0) {
      ItemTypeList item = new ItemTypeList("NAME", IItemType.TYPE_PUBLIC, "",
          LogString.itemIsUnknown(name, International.getString("Name")) + "\n" +
              International.getString("Meintest Du ...?"));
      for (int i = 0; i < neighbours.size(); i++) {
        item.addItem(neighbours.get(i), neighbours.get(i), false, '\0');
      }
      item.setFieldSize(300, 200);

      if (field == null || !field.isValid()) {
        // field is invalid if the entire input dialog has already been closed.
        // This can happen in case of SimpleInputDialog, where this check, triggered
        // by a focusLost event, is actually called when the user hits ENTER at the
        // end of the input. In this case, also the window is closed and the control
        // already returns to the calling thread. This is too late for us to check spelling
        // then.
        return;
      }
      if (SimpleOptionInputDialog.showOptionInputDialog(dlg,
          International.getString("Tippfehler?"), item,
          new String[] { International.getString("Ersetzen"),
              International.getString("Abbruch") },
          new int[] { SimpleOptionInputDialog.OPTION_OK,
              SimpleOptionInputDialog.OPTION_CANCEL },
          null)) {
        String suggestedName = item.getSelectedText();
        if (suggestedName != null && suggestedName.length() > 0) {
          this.parseAndShowValue(suggestedName);
        }
      }
    }
  }

  @Override
  public void acpwCallback(JTextField field) {
    autoComplete(null);
  }

  private AutoCompleteList getAutoCompleteList() {
    return autoCompleteList;
  }

  private void setButtonColor(Color color) {
    valueIsKnown = (color == Color.green || color == Color.yellow);
    if (button != null) {
      if (color != null) {
        if (!Daten.lookAndFeel.endsWith("MetalLookAndFeel")) {
          button.setContentAreaFilled(true);
        }
        button.setBackground(color);
      } else {
        button.setBackground(originalButtonColor);
      }
    }
  }

  public boolean isKnown() {
    return valueIsKnown;
  }

  public void setAlternateFieldNameForPlainText(String fieldName) {
    alternateFieldNameForPlainText = fieldName;
  }

  public String getAlternateFieldNameForPlainText() {
    return alternateFieldNameForPlainText;
  }

  public void setAlwaysReturnPlainText(boolean alwaysReturnPlainText) {
    this.alwaysReturnPlainText = alwaysReturnPlainText;
  }

  public boolean getAlwaysReturnPlainText() {
    return alwaysReturnPlainText;
  }

  public void requestButtonFocus() {
    if (button != null) {
      button.requestFocus();
    }
  }

  public JButton getButton() {
    return button;
  }

  @Override
  public boolean isValidInput() {
    if (alternateFieldNameForPlainText == null && value != null && value.length() > 0) {
      // make sure the entered value is a valid ID
      if (autoCompleteList.getId(value) == null) {
        lastInvalidErrorText = International.getString("Unbekannter Name nicht erlaubt");
        return false;
      }
    }
    return super.isValidInput();
  }

}
