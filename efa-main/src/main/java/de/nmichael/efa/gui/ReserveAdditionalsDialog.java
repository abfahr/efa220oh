package de.nmichael.efa.gui;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.data.BoatRecord;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.gui.util.EfaMouseListener;
import de.nmichael.efa.util.International;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class ReserveAdditionalsDialog extends BaseDialog{
//Todo: Rückgabe der selektierten Boote an die Reservierungslogik
//Todo: Sortierung der Liste
//Todo: Fehlermeldung wenn bereits Boote reserviert sind
//Todo: Boot von welchem die ursprüngliche Reservierung ausgeht, sollte möglichst in rechter Liste sein - darf zumindest aber nicht in linker Liste auftauchen
//Todo: Internationalisierung
//Todo: Feature Toggle um zwischen alter und neuer Logik umzuschalten
//Todo: für normale Nutzer soll die Gruppe der eigentlichen Reservierung vorselektiert und der Wechsel der GroupDropDown nicht möglich sein

//Klasse um bei Bootsreservierung durch Admin direkt weitere Boote oder ganze Bootsgruppen auszuwählen

    public static final String BOAT_SELECT_BTN = "boat_select_btn";
    public static final String BOAT_ALL_SELECT_BTN = "boat_select_all_btn";
    public static final String BOAT_REMOVE_BTN = "boat_remove_btn";

    private String KEYACTION_ENTER;

    private String[] boatSeatsValuesArray = EfaTypes
            .makeBoatSeatsArray(EfaTypes.ARRAY_STRINGLIST_VALUES); //Bootskategorien/-gruppen : keys (1,2,3,...)
    private String[] boatSeatsDisplayArray = EfaTypes
            .makeBoatSeatsArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY);//Bootskategorien/-gruppen : Werte (Canadier, SUPs,..)

    private ItemTypeStringList kategorienDropDown;

    private ItemTypeLabel label_caution;

    private ItemTypeList<IItemType> itemsOfGroupList;

    private ItemTypeList<IItemType> selectedItemsList;

    private ItemTypeButton selectButton;

    private ItemTypeButton selectAllButton;

    private ItemTypeButton removeButton;

    private ItemTypeStringList groupDropDown;

    private HashMap<String, FilteringModel> groupsData;

    public ReserveAdditionalsDialog(Window parent, String title) {
        super(parent, title, International.getStringWithMnemonic("OK"));
    }

    @Override
    public void keyAction(ActionEvent evt) {

    }

    @Override
    protected void iniDialog() throws Exception {
       KEYACTION_ENTER = addKeyAction("ENTER");

       mainPanel.setLayout(new GridBagLayout());

       groupsData = new HashMap<>();

       label_caution = new ItemTypeLabel("L0", IItemType.TYPE_INTERNAL, "",
                "Soll die Reservierung erweitert werden?");
       label_caution.displayOnGui(this, mainPanel, 0,0);

       groupDropDown = new ItemTypeStringList("Groups","1",  boatSeatsValuesArray, boatSeatsDisplayArray,0,"",null);
       groupDropDown.setFieldSize(300, 30);
       groupDropDown.displayOnGui(this, mainPanel, 1, 0);

        //linke Liste: Zeigt Boote/Items der per dropDown ausgewählten Gruppe
       itemsOfGroupList = new ItemTypeList("Stuff",IItemType.TYPE_PUBLIC, "", "Boote/Material der Gruppe");
       itemsOfGroupList.setPadding(0, 10, 0, 10);
       itemsOfGroupList.setFieldSize(300, 200);
       itemsOfGroupList.displayOnGui(this, mainPanel, 0, 1);

       //rechte Liste: zeigt die Boote/Items, welche zur Reservierung ausgewählt wurden.
       selectedItemsList = new ItemTypeList("SelectedStuff",IItemType.TYPE_PUBLIC, "", "Ausgewählte(s) Boote/Material");
       selectedItemsList.setPadding(0, 0, 0, 10);
       selectedItemsList.setFieldSize(300, 200);
       selectedItemsList.displayOnGui(this, mainPanel, 1, 1);

       selectButton = new ItemTypeButton(BOAT_SELECT_BTN,0, "","Auswählen");
       //selectButton.setFieldSize(20, 30);
       selectButton.setPadding(0, 10, 0, 0);
       selectButton.displayOnGui(this, mainPanel, 0, 2);

       selectAllButton = new ItemTypeButton(BOAT_ALL_SELECT_BTN,0, "","Gesamte Gruppe auswählen");
       //selectAllButton.setFieldSize(20, 30);
       selectAllButton.setPadding(0, 10, 5, 0);
       selectAllButton.displayOnGui(this, mainPanel,0,3);

       removeButton = new ItemTypeButton(BOAT_REMOVE_BTN,0, "","Entfernen");
       //removeButton.setFieldSize(20, 30);
       removeButton.setPadding(0, 0, 0, 0);
       removeButton.displayOnGui(this, mainPanel,1,2);

        if (closeButton != null) {
            closeButton.setIcon(getIcon("button_accept.png"));
        }

        selectButton.registerItemListener(new AddRemoveListener(itemsOfGroupList, selectedItemsList) {
            @Override
            protected List<IItemType> getSelectedItems(IItemType itemType, AWTEvent event) {

                List<IItemType> result = new ArrayList<>();
                if (itemType.getName().equals(BOAT_SELECT_BTN) && event instanceof ActionEvent) {
                    IItemType selectedItem = itemsOfGroupList.getSelectedValue();
                        result.add(selectedItem);
                }
                return result; //Liste mit selektierten Items
            }
        });

        selectAllButton.registerItemListener(new AddRemoveListener(itemsOfGroupList, selectedItemsList) {

            List<IItemType> result = new ArrayList<>();

            @Override
            protected List<IItemType> getSelectedItems(IItemType itemType, AWTEvent event) {
                if (itemType.getName().equals(BOAT_ALL_SELECT_BTN) && event instanceof ActionEvent) {
                    for (int i = 0; i <= itemsOfGroupList.size() -1; i++) {
                        Object item = itemsOfGroupList.getItemObject(i);
                        if (item instanceof IItemType) {
                         result.add((ItemType) item);
                        }
                    }
                }
                return result; //Liste mit allen Items der Gruppe
            }
        });

        removeButton.registerItemListener(new AddRemoveListener(selectedItemsList, itemsOfGroupList) {
            @Override
            protected List<IItemType> getSelectedItems(IItemType itemType, AWTEvent event) {
                //TODO: Zurücklegen soll nur möglich sein, wenn auch die entsprechende Kategorie aktiviert ist
                List<IItemType> result = new ArrayList<>();
                if (itemType.getName().equals(BOAT_REMOVE_BTN) && event instanceof ActionEvent) {
                    IItemType selectedItem = selectedItemsList.getSelectedValue();
                    result.add(selectedItem);
                }
                return result;
            }
        });

        //wählt Items mittels Doppelklick statt mit selectButton
        itemsOfGroupList.registerItemListener(new AddRemoveListener(itemsOfGroupList, selectedItemsList) {
            @Override
            protected List<IItemType> getSelectedItems(IItemType itemType, AWTEvent event) {

                List<IItemType> result = new ArrayList<>();
                if(event instanceof ActionEvent )
                {
                    String actionCommand = ((ActionEvent) event).getActionCommand();
                    if (actionCommand.equals(EfaMouseListener.EVENT_MOUSECLICKED_2x)) {
                        IItemType selectedItem = itemsOfGroupList.getSelectedValue();
                        result.add(selectedItem);
                    }
                }
                return result;
            }
        });


        IItemListener groupDropDownListener = new IItemListener() {

            @Override
            /**
             * Listener um in Liste boatList/ die Boote der Kategorie zu zeigen, welche in der Dropdownleiste gewählt wurde
             */
            public void itemListenerAction(IItemType itemType, AWTEvent event) {
                if (event instanceof ItemEvent
                        && event.getID() == ItemEvent.ITEM_STATE_CHANGED && ((ItemEvent) event).getStateChange() == ItemEvent.SELECTED) {

                //    new Thread(new Runnable() {
                     //   public void run() {
                            FilteringModel currentModel = null;
                            try {
                                String selectedCategory = null;
                                Object selectedItem = ((ItemEvent) event).getItem();
                                if (selectedItem instanceof ItemTypeStringList.ItemLabelValue) {
                                    selectedCategory = ((ItemTypeStringList.ItemLabelValue) selectedItem).getValue();
                                }
                                currentModel = ReserveAdditionalsDialog.this.groupsData.get(selectedCategory);
                                //Aufrufe an die Datenbank reduzieren

                                if (currentModel == null) {
                                    ArrayList<IItemType> itemsByTypeSeats = ReserveAdditionalsDialog.this.createListOfItemsByTypeSeats(selectedCategory);
                                    currentModel = new FilteringModel(itemsByTypeSeats);
                                    ReserveAdditionalsDialog.this.groupsData.put(selectedCategory, currentModel);
                                }

                            } catch (EfaException e) {
                                throw new RuntimeException(e);
                            }

                            FilteringModel finalCurrentModel = currentModel;
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    //Nur mit gefilterten Items befüllen, damit nicht neu bestehendes Material/Boote ausgeliehen werden können
                                    itemsOfGroupList.removeAllItems();
                                    List<IItemType> selectedItems = selectedItemsList.getItemObjects();
                                    List<IItemType> filteredList = finalCurrentModel.filter(selectedItems);
                                    for (IItemType boat :filteredList) {
                                        itemsOfGroupList.addItem(boat.getName(), boat, true, '\0');
                                    }
                                    itemsOfGroupList.requestFocus();
                                }
                            });
                     //   }
                   // }).start();
                }
            }
        };

        groupDropDown.registerItemListener(groupDropDownListener);

    }

    private ArrayList<IItemType> createListOfItemsByTypeSeats(String typeSeats)
            throws  EfaException{
        IDataAccess allBoats = Daten.project.getBoats(false).data();
        ArrayList<IItemType> liste = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (DataKey<?, ?, ?> dataKey : allBoats.getAllKeys()) {
            BoatRecord boatRecord = (BoatRecord) allBoats.get(dataKey);
            if (!boatRecord.isValidAt(now)) {
                // Boot nicht mehr gültig, abgelaufen
                continue;
            }
            if (!typeSeats.equals(boatRecord.getTypeSeats(0))) {
                // aber nicht fremde Bootstypen - hier als Sitzplätze
                // für andere Bootstypen, bitte neue Reservierung dort aufmachen.
                continue;
            }
            ItemTypeBoolean item = new ItemTypeBoolean(boatRecord.getName(), false,
                    IItemType.TYPE_INTERNAL, "", boatRecord.getQualifiedName());
            item.setDataKey(boatRecord.getKey());
            liste.add(item);
        }
        return liste;
    }

    public static boolean showInputDialog(Window parent) {
        ReserveAdditionalsDialog dlg = new ReserveAdditionalsDialog(parent, "Übertragen auf weitere Gruppen");
        dlg.showDialog();
        return dlg.resultSuccess;
    }

    private abstract class AddRemoveListener implements IItemListener{

        private ItemTypeList source;
        private ItemTypeList target;

        public AddRemoveListener(ItemTypeList source, ItemTypeList target) {
            this.source = source;
            this.target = target;
        }
        //Verschiebt Items von einer Liste zur anderen Liste
        @Override
        public void itemListenerAction(IItemType itemType, AWTEvent event) {
            List<IItemType> selectedItems = getSelectedItems(itemType, event);
            for (IItemType selectedItem : selectedItems){
                if (target != null){
                    String selectedCategory = groupDropDown.getValue();
                    //Nur hinzufügen wenn auch die korrekte Kategorie aktuell ausgewählt wurde
                    FilteringModel currentModel = groupsData.get(selectedCategory);
                    if (currentModel != null & currentModel.filter(null).contains(selectedItem)){
                        target.addItem(selectedItem.getName(), selectedItem, true,'\0' );
                    }
                }
                if (source != null & selectedItems.size() == 1){
                    source.removeItem(source.getSelectedIndex());
                } else {
                    source.removeAllItems();
                }
            }

            if (target == null){
                target.requestFocus();
            }
            if (source == null){
                source.requestFocus();
            }
            selectedItems.clear();
        }

        protected abstract List<IItemType> getSelectedItems(IItemType itemType, AWTEvent event);
    }

    /**
     * Data model for group specific items. Items may be filtered.
     */
    private static class FilteringModel {
        List<IItemType> originList;
        List<IItemType> filteredList;

        public FilteringModel() {
            this(new ArrayList<>());
        }

        public FilteringModel(ArrayList<IItemType> originList){
            this.originList = originList;
            filteredList = new ArrayList<>();
        }

        public void addElement(IItemType element) {
            originList.add(element);
        }

        public List<IItemType> filter(Collection<IItemType> itemTypes) {
            if (itemTypes == null){
                return originList;
            }
            return originList.stream().filter(item -> !itemTypes.contains(item)).collect(Collectors.toList());
        }
    }

}
