/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui.dataedit;

import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.ex.EfaModifyException;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.gui.dataedit.DataEditDialog;
import de.nmichael.efa.gui.dataedit.VersionizedDataDeleteDialog;
import de.nmichael.efa.gui.util.EfaMouseListener;
import de.nmichael.efa.gui.util.TableCellRenderer;
import de.nmichael.efa.gui.util.TableItem;
import de.nmichael.efa.gui.util.TableItemHeader;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

// @i18n complete

public class ClubworkItemTypeDataRecordTable extends ItemTypeDataRecordTable {

    public ClubworkItemTypeDataRecordTable(String name,
                                           TableItemHeader[] tableHeader,
                                           StorageObject persistence,
                                           long validAt,
                                           AdminRecord admin,
                                           String filterFieldName, String filterFieldValue,
                                           String[] actions, int[] actionTypes, String[] actionIcons,
                                           IItemListenerDataRecordTable itemListenerActionTable,
                                           int type, String category, String description) {
        super("TABLE",
                persistence.createNewRecord().getGuiTableHeader(),
                persistence, validAt, admin,
                filterFieldName, filterFieldValue, // defaults are null
                actions, actionTypes, actionIcons, // default actions: new, edit, delete
                itemListenerActionTable,
                type, category, description);
    }

    protected void updateFilter() {
        searchField.getValueFromGui();
        filterBySearch.getValueFromGui();
        boolean aggre = filterBySearch.getValue() && searchField.getValue().trim().length() >= 3;
        if(aggre) {
            filterFieldName = null;
        }
        else {
            filterFieldName = "Flag";
        }
        if (filterBySearch.isChanged() || (filterBySearch.getValue() && searchField.isChanged())) {
            updateData();
            updateAggregations(aggre);
            showValue();

        }
        filterBySearch.setUnchanged();
        searchField.setUnchanged();
    }
}
