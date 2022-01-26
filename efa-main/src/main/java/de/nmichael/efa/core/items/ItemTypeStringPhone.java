
package de.nmichael.efa.core.items;

import de.nmichael.efa.Daten;
import de.nmichael.efa.util.International;

public class ItemTypeStringPhone extends ItemTypeString {

  protected String regexp;
  protected boolean sehrStreng = false;

  public ItemTypeStringPhone(String name, String value, int type, String category,
      String description) {
    super(name, value, type, category, description);
    setNotNull(true);
    setMinCharacters(3);
    setRegexp(Daten.efaConfig.getRegexForHandynummer());
  }

  public String getRegexp() {
    return regexp;
  }

  public void setRegexp(String regexp) {
    this.regexp = regexp;
  }

  public boolean isSehrStreng() {
    return sehrStreng;
  }

  public void setSehrStreng(boolean sehrStreng) {
    this.sehrStreng = sehrStreng;
  }

  @Override
  public boolean isValidInput() {
    if (!super.isValidInput()) {
      return false;
    }
    if (value.length() < 5) {
      lastInvalidErrorText = "Das ist keine Telefonnummmer, oder?";
      return false;
    }
    if (isSehrStreng() && !getValue().matches(getRegexp())) {
      lastInvalidErrorText = International.getString("Fehlermeldung bei BadHandynummer");
      return false;
    }
    return true;
  }

}
