/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui.widgets;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JPanel;

import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeBoolean;
import de.nmichael.efa.core.items.ItemTypeInteger;
import de.nmichael.efa.core.items.ItemTypeStringList;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

public abstract class Widget implements IWidget {

  public static final String PARAM_ENABLED = "Enabled";
  public static final String PARAM_POSITION = "Position";
  public static final String PARAM_UPDATEINTERVAL = "UpdateInterval";

  String name;
  String description;
  boolean ongui;
  Vector<IItemType> parameters = new Vector<IItemType>();
  JPanel myPanel;

  public Widget(String name, String description, boolean ongui) {
    this.name = name;
    this.description = description;
    this.ongui = ongui;

    addParameterInternal(new ItemTypeBoolean(PARAM_ENABLED, false,
        IItemType.TYPE_PUBLIC, "",
        (ongui ?
            International.getMessage("{item} anzeigen", name) :
              International.getMessage("{item} aktivieren", description))));

    if (ongui) {
      addParameterInternal(new ItemTypeStringList(PARAM_POSITION, POSITION_BOTTOM,
          new String[] { POSITION_TOP, POSITION_BOTTOM, POSITION_LEFT, POSITION_RIGHT,
          POSITION_CENTER },
          new String[] { International.getString("oben"),
          International.getString("unten"),
          International.getString("links"),
          International.getString("rechts"),
          International.getString("mitte")
      },
      IItemType.TYPE_PUBLIC, "",
      International.getString("Position")));

      addParameterInternal(new ItemTypeInteger(PARAM_UPDATEINTERVAL, 3600, 1, Integer.MAX_VALUE,
          false,
          IItemType.TYPE_PUBLIC, "",
          International.getString("Aktualisierungsintervall")
          + " (s)"));
    }
  }

  @Override
  public String getParameterName(String internalName) {
    return "Widget" + this.name + internalName;
  }

  void addParameterInternal(IItemType p) {
    p.setName(getParameterName(p.getName()));
    parameters.add(p);
  }

  IItemType getParameterInternal(String internalName) {
    String name = getParameterName(internalName);
    for (int i = 0; i < parameters.size(); i++) {
      if (parameters.get(i).getName().equals(name)) {
        return parameters.get(i);
      }
    }
    return null;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public IItemType[] getParameters() {
    IItemType[] a = new IItemType[parameters.size()];
    for (int i = 0; i < parameters.size(); i++) {
      a[i] = parameters.get(i);
    }
    return a;
  }

  @Override
  public void setParameter(IItemType param) {
    for (int i = 0; i < parameters.size(); i++) {
      IItemType p = parameters.get(i);
      if (p.getName().equals(param.getName())) {
        p.parseValue(param.toString());
      }
    }
  }

  @Override
  public void setEnabled(boolean enabled) {
    ((ItemTypeBoolean) getParameterInternal(PARAM_ENABLED)).setValue(enabled);
  }

  @Override
  public boolean isEnabled() {
    return ((ItemTypeBoolean) getParameterInternal(PARAM_ENABLED)).getValue();
  }

  @Override
  public void setPosition(String p) {
    try {
      getParameterInternal(PARAM_POSITION).parseValue(p);
    } catch (NullPointerException eignoremissingparameter) {}
  }

  @Override
  public String getPosition() {
    try {
      return getParameterInternal(PARAM_POSITION).toString();
    } catch (NullPointerException eignoremissingparameter) {
      return null;
    }
  }

  @Override
  public void setUpdateInterval(int seconds) {
    ((ItemTypeInteger) getParameterInternal(PARAM_UPDATEINTERVAL)).setValue(seconds);
  }

  @Override
  public int getUpdateInterval() {
    return ((ItemTypeInteger) getParameterInternal(PARAM_UPDATEINTERVAL)).getValue();
  }

  abstract void construct();

  @Override
  public abstract JComponent getComponent();

  @Override
  public void show(JPanel panel, int x, int y) {
    if (!ongui) {
      return;
    }
    myPanel = panel;
    construct();
    JComponent comp = getComponent();
    if (comp != null) {
      panel.add(comp, new GridBagConstraints(x, y, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    }
  }

  @Override
  public void show(JPanel panel, String orientation) {
    if (!ongui) {
      return;
    }
    myPanel = panel;
    construct();
    JComponent comp = getComponent();
    if (comp != null) {
      panel.add(comp, orientation);
    }
  }

  public static String[] getAllWidgetClassNames() {
    return new String[] {
        HTMLWidget.class.getCanonicalName(),
        MeteoAstroWidget.class.getCanonicalName(),
        AlertWidget.class.getCanonicalName()
    };
  }

  public static Vector<IWidget> getAllWidgets() {
    String[] classNames = getAllWidgetClassNames();
    Vector<IWidget> widgets = new Vector<IWidget>();
    for (String className : classNames) {
      try {
        IWidget w = (IWidget) Class.forName(className).newInstance();
        if (w != null) {
          widgets.add(w);
        }
      } catch (Exception e) {
        Logger.logdebug(e);
      }
    }
    return widgets;
  }
}
