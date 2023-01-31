/*
 * This software is Copyright by the Board of Trustees of Michigan
 * State University (c) Copyright 2016.
 *
 * Contact Information:
 *   Facility for Rare Isotope Beam
 *   Michigan State University
 *   East Lansing, MI 48824-1321
 *   http://frib.msu.edu
 */
package org.phoebus.applications.saveandrestore.ui.snapshot;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.StringConverter;
import org.epics.vtype.Alarm;
import org.epics.vtype.EnumDisplay;
import org.epics.vtype.Time;
import org.epics.vtype.VEnum;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.common.Utilities;
import org.phoebus.applications.saveandrestore.common.VDisconnectedData;
import org.phoebus.applications.saveandrestore.common.VNoData;
import org.phoebus.applications.saveandrestore.common.VTypePair;
import org.phoebus.applications.saveandrestore.ui.ImageRepository;
import org.phoebus.applications.saveandrestore.ui.MultitypeTreeTableCell;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreController;
import org.phoebus.applications.saveandrestore.ui.model.VSnapshot;
import org.phoebus.applications.saveandrestore.ui.snapshot.hierarchyparser.IHierarchyParser;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.preferences.PreferencesReader;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.ui.application.ContextMenuHelper;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.util.time.TimestampFormats;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

class SnapshotTreeTable extends TreeTableView<TreeTableEntry> {

    private final TreeTableEntry rootTTE = new TreeTableEntry("ROOT", null, null);
    private IHierarchyParser hierarchyParser = null;

    public static final Logger LOGGER = Logger.getLogger(SnapshotController.class.getName());

    private static boolean resizePolicyNotInitialized = true;
    private static final PrivilegedAction<Object> resizePolicyAction = () -> {
        try {
            // Java FX bugfix: the table columns are not properly resized for the first table
            Field f = TreeTableView.CONSTRAINED_RESIZE_POLICY.getClass().getDeclaredField("isFirstRun");
            f.setAccessible(true);
            f.set(TreeTableView.CONSTRAINED_RESIZE_POLICY, Boolean.FALSE);
        } catch (NoSuchFieldException | IllegalAccessException | RuntimeException e) {
            // ignore
        }
        // Even if failed to set the policy, pretend that it was set. In such case the UI will be slightly dorked the
        // first time, but will be OK in all other cases.
        resizePolicyNotInitialized = false;
        return null;
    };

    /**
     * <code>TimestampTableCell</code> is a table cell for rendering the {@link Instant} objects in the table.
     *
     * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
     */
    private static class TimestampTreeTableCell extends TreeTableCell<TreeTableEntry, TreeTableEntry> {
        @Override
        protected void updateItem(TreeTableEntry item, boolean empty) {
            super.updateItem(item, empty);

            if (item == null || empty) {
                setText("");
                setStyle("");
            } else {
                TreeTableEntry entry = getTableRow().getItem();
                if (entry == null || entry.folder) {
                    setText(null);
                    setStyle("");
                } else {
                    Instant instant = item.tableEntry.timestampProperty().get();
                    if (instant == null) {
                        setText("---");
                    } else {
                        setText(TimestampFormats.SECONDS_FORMAT.format(item.tableEntry.timestampProperty().get()));
                    }
                }
            }
        }
    }

    /**
     * <code>VTypeCellEditor</code> is an editor type for {@link VType} or {@link VTypePair}, which allows editing the
     * value as a string.
     *
     * @param <T> {@link VType} or {@link VTypePair}
     * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
     */
    private static class VTypeTreeCellEditor<T> extends MultitypeTreeTableCell<TreeTableEntry, T> {

        private static final Image DISCONNECTED_IMAGE =
                ImageCache.getImage(SaveAndRestoreController.class, "/icons/showerr_tsk.png");
        private final Tooltip tooltip = new Tooltip();

        VTypeTreeCellEditor() {

            setConverter(new StringConverter<>() {
                @Override
                public String toString(T item) {
                    if (item == null) {
                        return "";
                    } else if (item instanceof VNumber) {
                        return ((VNumber) item).getValue().toString();
                    } else if (item instanceof VNumberArray) {
                        return ((VNumberArray) item).getData().toString();
                    } else if (item instanceof VEnum) {
                        return ((VEnum) item).getValue();
                    } else if (item instanceof VTypePair) {
                        VType value = ((VTypePair) item).value;

                        if (value instanceof VNumber) {
                            return ((VNumber) value).getValue().toString();
                        } else if (value instanceof VNumberArray) {
                            return ((VNumberArray) value).getData().toString();
                        } else if (value instanceof VEnum) {
                            return ((VEnum) value).getValue();
                        } else {
                            return value.toString();
                        }
                    } else {
                        return item.toString();
                    }
                }

                @SuppressWarnings("unchecked")
                @Override
                public T fromString(String string) {
                    T item = getItem();
                    try {
                        if (string == null) {
                            return item;
                        } else if (item instanceof VType) {
                            return (T) Utilities.valueFromString(string, (VType) item);
                        } else if (item instanceof VTypePair) {
                            VTypePair t = (VTypePair) item;
                            if (t.value instanceof VDisconnectedData) {
                                return (T) new VTypePair(t.base, Utilities.valueFromString(string, t.base),
                                        t.threshold);
                            } else {
                                return (T) new VTypePair(t.base, Utilities.valueFromString(string, t.value),
                                        t.threshold);
                            }
                        } else {
                            return item;
                        }
                    } catch (IllegalArgumentException e) {
                        return item;
                    }
                }
            });
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean isTextFieldType() {
            T item = getItem();
            if (item instanceof VEnum) {
                getItems().clear();

                VEnum value = (VEnum) item;
                List<String> labels = value.getDisplay().getChoices();
                List<T> values = new ArrayList<>(labels.size());
                for (int i = 0; i < labels.size(); i++) {
                    values.add((T) VEnum.of(i, EnumDisplay.of(labels), Alarm.none(), Time.now()));
                }
                setItems(values);

                return false;
            } else if (item instanceof VTypePair) {
                VTypePair v = ((VTypePair) item);
                VType type = v.value;
                if (type instanceof VEnum) {
                    getItems().clear();

                    VEnum value = (VEnum) type;
                    List<String> labels = value.getDisplay().getChoices();
                    List<T> values = new ArrayList<>(labels.size());
                    for (int i = 0; i < labels.size(); i++) {
                        values.add(
                                (T) new VTypePair(v.base, VEnum.of(i, EnumDisplay.of(labels), Alarm.none(), Time.now()), v.threshold));
                    }
                    setItems(values);

                    return false;
                }
            }
            return true;
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            updateItem(getItem(), isEmpty());
        }

        @Override
        public void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            getStyleClass().remove("diff-cell");

            TreeTableEntry entry = getTableRow().getItem();
            if (item == null || empty) {
                setText("");
                setTooltip(null);
                setGraphic(null);
            } else if (entry.folder) {
                setText("");
                setStyle("");
                setGraphic(null);
            } else {
                if (item == VDisconnectedData.INSTANCE) {
                    setText(VDisconnectedData.DISCONNECTED);
                    setGraphic(new ImageView(DISCONNECTED_IMAGE));
                    tooltip.setText(Messages.noValueAvailable);
                    setTooltip(tooltip);
                    getStyleClass().add("diff-cell");
                } else if (item == VNoData.INSTANCE) {
                    setText(item.toString());
                    tooltip.setText(Messages.noValueAvailable);
                    setTooltip(tooltip);
                } else if (item instanceof VType) {
                    setText(Utilities.valueToString((VType) item));
                    setGraphic(null);
                    tooltip.setText(item.toString());
                    setTooltip(tooltip);
                } else if (item instanceof VTypePair) {
                    VTypePair pair = (VTypePair) item;
                    if (pair.value == VDisconnectedData.INSTANCE) {
                        setText(VDisconnectedData.DISCONNECTED);
                        if (pair.base != VDisconnectedData.INSTANCE) {
                            getStyleClass().add("diff-cell");
                        }
                        setGraphic(new ImageView(DISCONNECTED_IMAGE));
                    } else if (pair.value == VNoData.INSTANCE) {
                        setText(pair.value.toString());
                    } else {
                        setText(Utilities.valueToString(pair.value));
                    }

                    tooltip.setText(item.toString());
                    setTooltip(tooltip);
                }
            }
        }
    }

    /**
     * A dedicated CellEditor for displaying delta only.
     * TODO can be simplified further
     *
     * @param <T>
     * @author Kunal Shroff
     */
    private static class VDeltaTreeCellEditor<T> extends VTypeTreeCellEditor<T> {

        private static final Image WARNING_IMAGE = ImageCache.getImage(SnapshotController.class, "/icons/hprio_tsk.png");
        private static final Image DISCONNECTED_IMAGE = ImageCache.getImage(SnapshotTreeTable.class, "/icons/showerr_tsk.png");
        private final Tooltip tooltip = new Tooltip();

        private boolean showDeltaPercentage = false;

        private void setShowDeltaPercentage() {
            showDeltaPercentage = true;
        }

        VDeltaTreeCellEditor() {
            super();
        }

        @Override
        public void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            getStyleClass().remove("diff-cell");

            TreeTableEntry entry = getTableRow().getItem();
            if (item == null || empty) {
                setText("");
                setTooltip(null);
                setGraphic(null);
            } else if (entry.folder) {
                setText("");
                setTooltip(null);
                setGraphic(null);
            } else {
                if (item == VDisconnectedData.INSTANCE) {
                    setText(VDisconnectedData.DISCONNECTED);
                    setGraphic(new ImageView(DISCONNECTED_IMAGE));
                    tooltip.setText(Messages.noValueAvailable);
                    setTooltip(tooltip);
                    getStyleClass().add("diff-cell");
                } else if (item == VNoData.INSTANCE) {
                    setText(item.toString());
                    tooltip.setText(Messages.noValueAvailable);
                    setTooltip(tooltip);
                } else if (item instanceof VTypePair) {
                    VTypePair pair = (VTypePair) item;
                    if (pair.value == VDisconnectedData.INSTANCE) {
                        setText(VDisconnectedData.DISCONNECTED);
                        if (pair.base != VDisconnectedData.INSTANCE) {
                            getStyleClass().add("diff-cell");
                        }
                        setGraphic(new ImageView(DISCONNECTED_IMAGE));
                    } else if (pair.value == VNoData.INSTANCE) {
                        setText(pair.value.toString());
                    } else {
                        Utilities.VTypeComparison vtc = Utilities.deltaValueToString(pair.value, pair.base, pair.threshold);
                        String percentage = Utilities.deltaValueToPercentage(pair.value, pair.base);
                        if (!percentage.isEmpty() && showDeltaPercentage) {
                            Formatter formatter = new Formatter();
                            setText(formatter.format("%g", Double.parseDouble(vtc.getString())) + " (" + percentage + ")");
                        } else {
                            setText(vtc.getString());
                        }
                        if (!vtc.isWithinThreshold()) {
                            getStyleClass().add("diff-cell");
                            setGraphic(new ImageView(WARNING_IMAGE));
                        }
                    }

                    tooltip.setText(item.toString());
                    setTooltip(tooltip);
                }
            }
        }
    }

    /**
     * <code>TooltipTreeTableColumn</code> is the common table column implementation, which can also provide the tooltip.
     *
     * @param <T> the type of the values displayed by this column
     * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
     */
    private static class TooltipTreeTableColumn<T> extends TreeTableColumn<TreeTableEntry, T> {
        private String text;
        private Label label;

        TooltipTreeTableColumn(String text, String tooltip, int minWidth) {
            setup(text, tooltip, minWidth, -1, true);
        }

        TooltipTreeTableColumn(String text, String tooltip, int minWidth, int prefWidth, boolean resizable) {
            setup(text, tooltip, minWidth, prefWidth, resizable);
        }

        private void setup(String text, String tooltip, int minWidth, int prefWidth, boolean resizable) {
            label = new Label(text);
            label.setTooltip(new Tooltip(tooltip));
            label.setTextAlignment(TextAlignment.CENTER);
            setGraphic(label);

            if (minWidth != -1) {
                setMinWidth(minWidth);
            }
            if (prefWidth != -1) {
                setPrefWidth(prefWidth);
            }
            setResizable(resizable);
            this.text = text;
        }

        void setSaved(boolean saved) {
            if (saved) {
                label.setText(text);
            } else {
                String t = this.text;
                if (text.indexOf('\n') > 0) {
                    t = "*" + t.replaceFirst("\n", "*\n");
                } else {
                    t = "*" + t + "*";
                }
                label.setText(t);
            }
        }
    }

    /**
     * <code>SelectionTreeTableColumn</code> is the table column for the first column in the table, which displays
     * a checkbox, whether the PV should be selected or not.
     *
     * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
     */
    private class SelectionTreeTableColumn extends TooltipTreeTableColumn<TreeTableEntry> {
        SelectionTreeTableColumn() {
            super("", Messages.includeThisPV, 30, 30, false);
            setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getValue()));
            //for those entries, which have a read-only property, disable the checkbox
            setCellFactory(cell -> new SelectionTreeTableColumnCell());
            setEditable(true);
            setSortable(false);
            selectAllCheckBox = new CheckBox();
            selectAllCheckBox.setSelected(false);
            selectAllCheckBox.setOnAction(action -> rootTTE.cbti.setSelected(selectAllCheckBox.isSelected()));
            setGraphic(selectAllCheckBox);
            MenuItem inverseMI = new MenuItem(Messages.inverseSelection);
            inverseMI.setOnAction(action -> treeTableEntryItems.values().stream().filter(item -> !item.folder)
                    .filter(item -> !item.tableEntry.readOnlyProperty().get())
                    .forEach(item -> item.tableEntry.selectedProperty().set(!item.tableEntry.selectedProperty().get())));
            final ContextMenu contextMenu = new ContextMenu(inverseMI);
            selectAllCheckBox.setOnMouseReleased(e -> {
                if (e.getButton() == MouseButton.SECONDARY) {
                    contextMenu.show(selectAllCheckBox, e.getScreenX(), e.getScreenY());
                }
            });
        }
    }

    private static class SelectionTreeTableColumnCell extends TreeTableCell<TreeTableEntry, TreeTableEntry> {
        private final CheckBox checkBox;

        private ObservableValue<Boolean> booleanProperty;
        private BooleanProperty indeterminateProperty;
        private BooleanProperty disabledProperty;

        public SelectionTreeTableColumnCell() {
            checkBox = new CheckBox();

            setGraphic(null);
        }

        @Override
        protected void updateItem(TreeTableEntry item, boolean empty) {
            super.updateItem(item, empty);

            if (item == null || empty) {
                setText(null);
                setGraphic(null);
            } else {
                setGraphic(checkBox);

                if (booleanProperty instanceof BooleanProperty) {
                    checkBox.selectedProperty().unbindBidirectional((BooleanProperty) booleanProperty);
                }

                if (indeterminateProperty != null) {
                    checkBox.indeterminateProperty().unbindBidirectional(indeterminateProperty);
                }

                if (disabledProperty != null) {
                    checkBox.disableProperty().unbindBidirectional(disabledProperty);
                }

                booleanProperty = item.selected;
                checkBox.selectedProperty().bindBidirectional((BooleanProperty) booleanProperty);

                indeterminateProperty = item.indeterminate;
                checkBox.indeterminateProperty().bindBidirectional(indeterminateProperty);

                disabledProperty = item.disabled;
                checkBox.disableProperty().bindBidirectional(disabledProperty);
            }
        }
    }

    private final List<VSnapshot> uiSnapshots = new ArrayList<>();
    private boolean showDeltaPercentage;
    private final SnapshotController controller;
    private final Map<String, TreeTableEntry> treeTableEntryItems = new HashMap<>();
    private CheckBox selectAllCheckBox;

    /**
     * Constructs a new table.
     *
     * @param controller the controller
     */
    SnapshotTreeTable(SnapshotController controller) {
        if (resizePolicyNotInitialized) {
            AccessController.doPrivileged(resizePolicyAction);
        }

        this.controller = controller;

        setRoot(rootTTE.cbti);
        setShowRoot(false);

        setEditable(true);
        getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        setMaxWidth(Double.MAX_VALUE);
        setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(this, Priority.ALWAYS);
        setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        getStylesheets().add(SnapshotTreeTable.class.getResource("/style.css").toExternalForm());

        PreferencesReader preferencesReader = new PreferencesReader(SaveAndRestoreApplication.class, "/save_and_restore_preferences.properties");
        String parserClassName = preferencesReader.get("treeTableView.hierarchyParser");
        try {
            hierarchyParser = (IHierarchyParser) Class.forName(getClass().getPackageName() + ".hierarchyparser." + parserClassName).getConstructor().newInstance();
        } catch (Exception e) {
            e.printStackTrace();

            LOGGER.severe("Unable to find " + getClass().getPackageName() + ".hierarchyparser." + parserClassName + "! Setting default RegexHierarchyParser!");
            try {
                parserClassName = "RegexHierarchyParser";
                hierarchyParser = (IHierarchyParser) Class.forName(getClass().getPackageName() + ".hierarchyparser." + parserClassName).getConstructor().newInstance();

            } catch (Exception ee) {
                e.printStackTrace();

                LOGGER.severe("Unable to find " + parserClassName + "! This is a serious issue!");
            }
        }

        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() != KeyCode.SPACE) {
                return;
            }

            ObservableList<TreeItem<TreeTableEntry>> selections = getSelectionModel().getSelectedItems();

            if (selections == null) {
                return;
            }

            selections.stream()
                    .filter(item -> item.getValue().folder || !item.getValue().tableEntry.readOnlyProperty().get())
                    .forEach(item -> item.getValue().selected.setValue(!item.getValue().selected.get()));

            // Somehow JavaFX TableView handles SPACE pressed event as going into edit mode of the cell.
            // Consuming event prevents NullPointerException.
            event.consume();
        });

        setRowFactory(tableView -> new TreeTableRow<>() {
            final ContextMenu contextMenu = new ContextMenu();

            @Override
            protected void updateItem(TreeTableEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || item.folder || empty) {
                    setOnContextMenuRequested(null);
                } else {
                    setOnContextMenuRequested(event -> {
                        List<ProcessVariable> selectedPVList = getSelectionModel().getSelectedItems().stream()
                                .map(treeTableEntry -> new ProcessVariable(treeTableEntry.getValue().pvNameProperty().get()))
                                .collect(Collectors.toList());

                        contextMenu.hide();
                        contextMenu.getItems().clear();
                        SelectionService.getInstance().setSelection(SaveAndRestoreApplication.NAME, selectedPVList);
                        ContextMenuHelper.addSupportedEntries(this, contextMenu);
                        if (!item.folder) {
                            MenuItem toggle = new MenuItem();
                            toggle.setText(item.tableEntry.readOnlyProperty().get() ? Messages.makeRestorable : Messages.makeReadOnly);
                            CheckBox toggleIcon = new CheckBox();
                            toggleIcon.setFocusTraversable(false);
                            toggleIcon.setSelected(item.tableEntry.readOnlyProperty().get());
                            toggle.setGraphic(toggleIcon);
                            toggle.setOnAction(actionEvent -> {
                                item.tableEntry.readOnlyProperty().set(!item.tableEntry.readOnlyProperty().get());
                                item.tableEntry.selectedProperty().set(!item.tableEntry.readOnlyProperty().get());
                            });

                            contextMenu.getItems().add(new SeparatorMenuItem());
                            contextMenu.getItems().add(toggle);
                        }
                        contextMenu.show(this, event.getScreenX(), event.getScreenY());
                    });
                }
            }
        });
    }

    private void recursiveSortByName(ObservableList<TreeItem<TreeTableEntry>> list) {
        FXCollections.sort(list, Comparator.comparing((TreeItem<TreeTableEntry> tte) -> !tte.getValue().folder)
                .thenComparing((TreeItem<TreeTableEntry> tte) -> tte.getValue().name));

        list.forEach(item -> recursiveSortByName(item.getChildren()));
    }

    /**
     * Split PV names by their seperators _ and : and build a tree structure
     *
     * @param tableEntryItems source for creating treeTableEntryItems;
     */

    private void createTreeTableEntryItems(List<TableEntry> tableEntryItems) {

        if (!rootTTE.children.isEmpty()) {
            return;
        }

        for (TableEntry entry : tableEntryItems) {
            String pvName = entry.pvNameProperty().get();

            List<String> parsedPV = hierarchyParser.parse(pvName);

            TreeTableEntry parent = rootTTE;
            for (int index = 0; index < parsedPV.size(); index++) {
                if (parent.children.containsKey(parsedPV.get(index)) && parent.children.get(parsedPV.get(index)).folder) {
                    parent = parent.children.get(parsedPV.get(index));
                } else if (index < parsedPV.size() - 1) {
                    parent = new TreeTableEntry(parsedPV.get(index), null, parent);
                }

                if (index == parsedPV.size() - 1) {
                    TreeTableEntry signal = new TreeTableEntry(parsedPV.get(index), entry, parent);
                    signal.initializeEqualPropertyChangeListener(controller);
                    signal.initializeChangeListenerForColumnHeaderCheckBox(selectAllCheckBox);
                    signal.initializeReadonlyChangeListenerForToggle();
                    treeTableEntryItems.put(getPVKey(pvName, entry.readOnlyProperty().get()), signal);
                }
            }
        }
    }

    private String getPVKey(String pvName, boolean isReadonly) {
        return pvName + "_" + isReadonly;
    }

    private int measureStringWidth(String text, Font font) {
        Text mText = new Text(text);
        if (font != null) {
            mText.setFont(font);
        }
        return (int) mText.getLayoutBounds().getWidth();
    }


    private void createTableForSingleSnapshot(boolean showLiveReadback, boolean showStoredReadback) {
        List<TreeTableColumn<TreeTableEntry, ?>> snapshotTreeTableColumns = new ArrayList<>(12);

        TreeTableColumn<TreeTableEntry, TreeTableEntry> selectedColumn = new SelectionTreeTableColumn();
        snapshotTreeTableColumns.add(selectedColumn);

        int width = measureStringWidth("000", Font.font(20));
        TreeTableColumn<TreeTableEntry, Integer> idColumn = new TooltipTreeTableColumn<>("#",
                Messages.toolTipTableColumIndex, width, width, false);
        idColumn.setCellValueFactory(cell -> {
            TreeTableEntry treeTableEntry = cell.getValue().getValue();
            if (treeTableEntry.folder) {
                return new ReadOnlyObjectWrapper(null);
            }

            int idValue = treeTableEntry.tableEntry.idProperty().get();
            idColumn.setPrefWidth(Math.max(idColumn.getWidth(), measureStringWidth(String.valueOf(idValue), Font.font(20))));

            return new ReadOnlyObjectWrapper(idValue);
        });
        idColumn.setCellFactory(cell -> new IdTreeTableCell());
        snapshotTreeTableColumns.add(idColumn);

        TreeTableColumn<TreeTableEntry, TreeTableEntry> pvNameColumn = new TooltipTreeTableColumn<>(Messages.pvName,
                Messages.toolTipTableColumnPVName, 100);

        pvNameColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getValue()));
        pvNameColumn.setCellFactory(cell -> new PVNameTreeTableCell());
        pvNameColumn.setComparator(Comparator.comparing((TreeTableEntry tte) -> !tte.folder).thenComparing(tte -> tte.name));

        snapshotTreeTableColumns.add(pvNameColumn);
        setTreeColumn(pvNameColumn);

        if (showLiveReadback) {
            TreeTableColumn<TreeTableEntry, String> readbackPVName = new TooltipTreeTableColumn<>(Messages.readbackPVName,
                    Messages.toolTipTableColumnReadbackPVName, 100);
            readbackPVName.setCellValueFactory(new TreeItemPropertyValueFactory<>("readbackName"));
            snapshotTreeTableColumns.add(readbackPVName);
        }

        width = measureStringWidth("MM:MM:MM.MMM MMM MM M", null);
        TreeTableColumn<TreeTableEntry, TreeTableEntry> timestampColumn = new TooltipTreeTableColumn<>(Messages.timestamp,
                Messages.toolTipTableColumnTimestamp, width, width, true);
        timestampColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getValue()));
        timestampColumn.setCellFactory(c -> new TimestampTreeTableCell());
        timestampColumn.getStyleClass().add("timestamp-column");
        timestampColumn.setPrefWidth(width);
        snapshotTreeTableColumns.add(timestampColumn);

        TreeTableColumn<TreeTableEntry, String> statusColumn = new TooltipTreeTableColumn<>(Messages.status,
                Messages.toolTipTableColumnAlarmStatus, 100, 100, true);
        statusColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("status"));
        statusColumn.setCellFactory(cell -> new StringTreeTableCell());
        snapshotTreeTableColumns.add(statusColumn);

        TreeTableColumn<TreeTableEntry, String> severityColumn = new TooltipTreeTableColumn<>(Messages.severity,
                Messages.toolTipTableColumnAlarmSeverity, 100, 100, true);
        severityColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("severity"));
        severityColumn.setCellFactory(cell -> new StringTreeTableCell());
        snapshotTreeTableColumns.add(severityColumn);

        TreeTableColumn<TreeTableEntry, ?> storedValueBaseColumn = new TooltipTreeTableColumn<>(
                "Stored Setpoint", "", -1);

        TreeTableColumn<TreeTableEntry, VType> storedValueColumn = new TooltipTreeTableColumn<>(
                "Stored Setpoint",
                Messages.toolTipTableColumnSetpointPVValue, 100);
        storedValueColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("snapshotVal"));
        storedValueColumn.setCellFactory(e -> new VTypeTreeCellEditor<>());
        storedValueColumn.setEditable(true);
        storedValueColumn.setOnEditCommit(e -> {
            TreeTableEntry treeTableEntry = e.getRowValue().getValue();
            VType updatedValue = treeTableEntry.tableEntry.readOnlyProperty().get() ? e.getOldValue() : e.getNewValue();

            ObjectProperty<VTypePair> value = treeTableEntry.tableEntry.valueProperty();
            value.setValue(new VTypePair(value.get().base, updatedValue, value.get().threshold));
            controller.updateLoadedSnapshot(0, e.getRowValue().getValue().tableEntry, updatedValue);
        });

        storedValueBaseColumn.getColumns().add(storedValueColumn);
        // show deltas in separate column
        TreeTableColumn<TreeTableEntry, VTypePair> delta = new TooltipTreeTableColumn<>(
                Utilities.DELTA_CHAR + " " + Messages.liveSetpoint,
                "", 100);
        delta.setCellValueFactory(e -> {
            TreeTableEntry treeTableEntry = e.getValue().getValue();
            if (treeTableEntry.folder) {
                return null;
            }
            // TODO:
            TableEntry tableEntry = treeTableEntry.tableEntry;
            if (tableEntry == null) {
                return new ReadOnlyObjectWrapper(10);
            }
            return treeTableEntry.tableEntry.valueProperty();
        });
        delta.setCellFactory(e -> {
            VDeltaTreeCellEditor vDeltaTreeCellEditor = new VDeltaTreeCellEditor<>();
            if (showDeltaPercentage) {
                vDeltaTreeCellEditor.setShowDeltaPercentage();
            }

            return vDeltaTreeCellEditor;
        });
        delta.setEditable(false);

        delta.setComparator((pair1, pair2) -> {
            Utilities.VTypeComparison vtc1 = Utilities.valueToCompareString(pair1.value, pair1.base, pair1.threshold);
            Utilities.VTypeComparison vtc2 = Utilities.valueToCompareString(pair2.value, pair2.base, pair2.threshold);

            return Double.compare(vtc1.getAbsoluteDelta(), vtc2.getAbsoluteDelta());
        });

        storedValueBaseColumn.getColumns().add(delta);

        snapshotTreeTableColumns.add(storedValueBaseColumn);

        if (showStoredReadback) {
            TreeTableColumn<TreeTableEntry, VType> storedReadbackColumn = new TooltipTreeTableColumn<>(
                    "Stored Readback\n(" + Utilities.DELTA_CHAR + " Stored Setpoint)", Messages.storedReadbackValue, 100);
            storedReadbackColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("storedReadback"));
            storedReadbackColumn.setCellFactory(e -> new VTypeTreeCellEditor<>());
            storedReadbackColumn.setEditable(false);
            snapshotTreeTableColumns.add(storedReadbackColumn);
        }

        TreeTableColumn<TreeTableEntry, VType> liveValueColumn = new TooltipTreeTableColumn<>(Messages.liveSetpoint, Messages.currentPVValue,
                100);
        liveValueColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("liveValue"));
        liveValueColumn.setCellFactory(e -> new VTypeTreeCellEditor<>());
        liveValueColumn.setEditable(false);
        snapshotTreeTableColumns.add(liveValueColumn);


        if (showLiveReadback) {
            TreeTableColumn<TreeTableEntry, VType> readbackColumn = new TooltipTreeTableColumn<>(
                    Messages.liveReadbackVsSetpoint, Messages.currentReadbackValue, 100);
            readbackColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("liveReadback"));
            readbackColumn.setCellFactory(e -> new VTypeTreeCellEditor<>());
            readbackColumn.setEditable(false);
            snapshotTreeTableColumns.add(readbackColumn);
        }

        getColumns().addAll(snapshotTreeTableColumns);
    }

    private void createTableForMultipleSnapshots(List<VSnapshot> snapshots) {
        List<TreeTableColumn<TreeTableEntry, ?>> list = new ArrayList<>(7);
        TreeTableColumn<TreeTableEntry, TreeTableEntry> selectedColumn = new SelectionTreeTableColumn();
        list.add(selectedColumn);

        int width = measureStringWidth("000", Font.font(20));
        TreeTableColumn<TreeTableEntry, Integer> idColumn = new TooltipTreeTableColumn<>("#",
                Messages.toolTipTableColumIndex, width, width, false);
        idColumn.setCellValueFactory(cell -> {
            TreeTableEntry treeTableEntry = cell.getValue().getValue();
            if (treeTableEntry.folder) {
                return null;
            }

            int idValue = treeTableEntry.tableEntry.idProperty().get();
            idColumn.setPrefWidth(Math.max(idColumn.getWidth(), measureStringWidth(String.valueOf(idValue), Font.font(20))));

            return new ReadOnlyObjectWrapper(idValue);
        });
        idColumn.setCellFactory(cell -> new IdTreeTableCell());
        list.add(idColumn);

        TreeTableColumn<TreeTableEntry, TreeTableEntry> setpointPVName = new TooltipTreeTableColumn<>(Messages.pvName,
                Messages.toolTipUnionOfSetpointPVNames, 100);
        setpointPVName.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getValue()));
        setpointPVName.setCellFactory(cell -> new PVNameTreeTableCell());
        setpointPVName.setComparator(Comparator.comparing((TreeTableEntry tte) -> !tte.folder).thenComparing(tte -> tte.name));

        list.add(setpointPVName);
        setTreeColumn(setpointPVName);

        list.add(new DividerTreeTableColumn());

        TreeTableColumn<TreeTableEntry, ?> storedValueColumn = new TooltipTreeTableColumn<>(Messages.storedValues,
                Messages.toolTipTableColumnPVValues, -1);
        storedValueColumn.getStyleClass().add("toplevel");

        String snapshotName = snapshots.get(0).getSnapshot().get().getName() + " (" +
                snapshots.get(0) + ")";


        TreeTableColumn<TreeTableEntry, ?> baseCol = new TooltipTreeTableColumn<>(
                snapshotName,
                Messages.toolTipTableColumnSetpointPVValue, 33);
        baseCol.getStyleClass().add("second-level");

        TreeTableColumn<TreeTableEntry, VType> storedBaseSetpointValueColumn = new TooltipTreeTableColumn<>(
                "Base Setpoint",
                Messages.toolTipTableColumnBaseSetpointValue, 100);

        storedBaseSetpointValueColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("snapshotVal"));
        storedBaseSetpointValueColumn.setCellFactory(e -> new VTypeTreeCellEditor<>());
        storedBaseSetpointValueColumn.setEditable(true);
        storedBaseSetpointValueColumn.setOnEditCommit(e -> {
            TreeTableEntry treeTableEntry = e.getRowValue().getValue();
            VType updatedValue = treeTableEntry.tableEntry.readOnlyProperty().get() ? e.getOldValue() : e.getNewValue();

            ObjectProperty<VTypePair> value = treeTableEntry.tableEntry.valueProperty();
            value.setValue(new VTypePair(value.get().base, updatedValue, value.get().threshold));
            controller.updateLoadedSnapshot(0, e.getRowValue().getValue().tableEntry, updatedValue);

            for (int i = 1; i < snapshots.size(); i++) {
                ObjectProperty<VTypePair> compareValue = e.getRowValue().getValue().tableEntry.compareValueProperty(i);
                compareValue.setValue(new VTypePair(e.getNewValue(), compareValue.get().value, compareValue.get().threshold));
            }
        });

        baseCol.getColumns().add(storedBaseSetpointValueColumn);

        // show deltas in separate column
        TreeTableColumn<TreeTableEntry, VTypePair> delta = new TooltipTreeTableColumn<>(
                Utilities.DELTA_CHAR + " " + Messages.liveSetpoint,
                "", 100);

        delta.setCellValueFactory(e -> {
            TreeTableEntry treeTableEntry = e.getValue().getValue();
            if (treeTableEntry.folder) {
                return new ReadOnlyObjectWrapper(null);
            }

            return treeTableEntry.tableEntry.valueProperty();
        });
        delta.setCellFactory(e -> {
            VDeltaTreeCellEditor vDeltaCellEditor = new VDeltaTreeCellEditor<>();
            if (showDeltaPercentage) {
                vDeltaCellEditor.setShowDeltaPercentage();
            }

            return vDeltaCellEditor;
        });
        delta.setEditable(false);
        delta.setComparator((pair1, pair2) -> {
            Utilities.VTypeComparison vtc1 = Utilities.valueToCompareString(pair1.value, pair1.base, pair1.threshold);
            Utilities.VTypeComparison vtc2 = Utilities.valueToCompareString(pair2.value, pair2.base, pair2.threshold);

            if (!vtc1.isWithinThreshold() && vtc2.isWithinThreshold()) {
                return -1;
            } else if (vtc1.isWithinThreshold() && !vtc2.isWithinThreshold()) {
                return 1;
            } else {
                return 0;
            }
        });
        baseCol.getColumns().add(delta);

        storedValueColumn.getColumns().addAll(baseCol, new DividerTreeTableColumn());

        for (int i = 1; i < snapshots.size(); i++) {
            final int snapshotIndex = i;

            snapshotName = snapshots.get(snapshotIndex).getSnapshot().get().getName() + " (" +
                    snapshots.get(snapshotIndex) + ")";

            TooltipTreeTableColumn<VTypePair> baseSnapshotCol = new TooltipTreeTableColumn<>(snapshotName,
                    String.format(Messages.setpointPVWhen, snapshotName), 100);
            baseSnapshotCol.getStyleClass().add("second-level");

            TooltipTreeTableColumn<VTypePair> setpointValueCol = new TooltipTreeTableColumn<>(
                    Messages.setpoint,
                    String.format(Messages.setpointPVWhen, snapshotName), 66);

            setpointValueCol.setCellValueFactory(e -> {
                TreeTableEntry treeTableEntry = e.getValue().getValue();
                if (treeTableEntry.folder) {
                    return new ReadOnlyObjectWrapper(null);
                }

                return treeTableEntry.tableEntry.compareValueProperty(snapshotIndex);
            });
            setpointValueCol.setCellFactory(e -> new VTypeTreeCellEditor<>());
            setpointValueCol.setEditable(false);

            baseSnapshotCol.getColumns().add(setpointValueCol);

            TooltipTreeTableColumn<VTypePair> deltaCol = new TooltipTreeTableColumn<>(
                    Utilities.DELTA_CHAR + " " + Messages.baseSetpoint,
                    String.format(Messages.setpointPVWhen, snapshotName), 50);
            deltaCol.setCellValueFactory(e -> {
                TreeTableEntry treeTableEntry = e.getValue().getValue();
                if (treeTableEntry.folder) {
                    return new ReadOnlyObjectWrapper(null);
                }

                return e.getValue().getValue().tableEntry.compareValueProperty(snapshotIndex);
            });
            deltaCol.setCellFactory(e -> {
                VDeltaTreeCellEditor vDeltaCellEditor = new VDeltaTreeCellEditor<>();
                if (showDeltaPercentage) {
                    vDeltaCellEditor.setShowDeltaPercentage();
                }

                return vDeltaCellEditor;
            });
            deltaCol.setEditable(false);
            deltaCol.setComparator((pair1, pair2) -> {
                Utilities.VTypeComparison vtc1 = Utilities.valueToCompareString(pair1.value, pair1.base, pair1.threshold);
                Utilities.VTypeComparison vtc2 = Utilities.valueToCompareString(pair2.value, pair2.base, pair2.threshold);

                if (!vtc1.isWithinThreshold() && vtc2.isWithinThreshold()) {
                    return -1;
                } else if (vtc1.isWithinThreshold() && !vtc2.isWithinThreshold()) {
                    return 1;
                } else {
                    return 0;
                }
            });
            baseSnapshotCol.getColumns().addAll(deltaCol);
            storedValueColumn.getColumns().addAll(baseSnapshotCol, new DividerTreeTableColumn());
        }
        list.add(storedValueColumn);

        TreeTableColumn<TreeTableEntry, VType> liveValueColumn = new TooltipTreeTableColumn<>(Messages.liveSetpoint,
                Messages.currentSetpointValue, 100);

        liveValueColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("liveValue"));
        liveValueColumn.setCellFactory(e -> new VTypeTreeCellEditor<>());
        liveValueColumn.setEditable(false);
        list.add(liveValueColumn);

        getColumns().addAll(list);
    }

    /**
     * Updates the table by setting new content, including the structure. The table is always recreated, even if the new
     * structure is identical to the old one. This is slightly more expensive; however, this method is only invoked per
     * user request (button click).
     *
     * @param entries             the table entries (rows) to set on the table
     * @param snapshots           the snapshots which are currently displayed
     * @param showLiveReadback    true if readback column should be visible or false otherwise
     * @param showStoredReadback  true if the stored readback value columns should be visible or false otherwise
     * @param showDeltaPercentage true if delta percentage should be be visible or false otherwise
     */
    public void updateTable(List<TableEntry> entries, List<VSnapshot> snapshots, boolean showLiveReadback, boolean showStoredReadback, boolean showDeltaPercentage) {
        getColumns().clear();
        uiSnapshots.clear();
        // we should always know if we are showing the stored readback or not, to properly extract the selection
        this.showDeltaPercentage = showDeltaPercentage;
        uiSnapshots.addAll(snapshots);
        if (uiSnapshots.size() == 1) {
            createTableForSingleSnapshot(showLiveReadback, showStoredReadback);
        } else {
            createTableForMultipleSnapshots(snapshots);
        }
        updateTableColumnTitles();
        updateTable(entries);
    }

    /**
     * Sets new table entries for this table, but do not change the structure of the table.
     *
     * @param entries the entries to set
     */
    public void updateTable(List<TableEntry> entries) {
        rootTTE.clear();

        if (treeTableEntryItems.isEmpty()) {
            createTreeTableEntryItems(entries);
        }

        final boolean notHide = !controller.isHideEqualItems();

        entries.forEach(tableEntry -> {
            TreeTableEntry treeTableEntry = treeTableEntryItems.get(getPVKey(tableEntry.pvNameProperty().get(), tableEntry.readOnlyProperty().get()));
            if (treeTableEntry != null) {
                treeTableEntry.update(tableEntry);
                if (notHide || !tableEntry.liveStoredEqualProperty().get()) {
                    treeTableEntry.add();
                }

                // To display correct checkbox marker (indeterminate, checked, or none)
                if (!tableEntry.readOnlyProperty().get()) {
                    tableEntry.selectedProperty().set(!tableEntry.selectedProperty().get());
                    tableEntry.selectedProperty().set(!tableEntry.selectedProperty().get());
                }
            }
        });

        recursiveSortByName(rootTTE.cbti.getChildren());
    }

    /**
     * Update the table column titles, by putting an asterisk to non saved snapshots or remove asterisk from saved
     * snapshots.
     */
    private void updateTableColumnTitles() {
        // add the * to the title of the column if the snapshot is not saved
        if (uiSnapshots.size() == 1) {
            ((TooltipTreeTableColumn<?>) getColumns().get(6)).setSaved(true);
        } else {
            TreeTableColumn<TreeTableEntry, ?> column = getColumns().get(4);
            for (int i = 0; i < uiSnapshots.size(); i++) {
                TreeTableColumn tableColumn = column.getColumns().get(i);
                if (tableColumn instanceof DividerTreeTableColumn) {
                    continue;
                }
                ((TooltipTreeTableColumn<?>) tableColumn).setSaved(true);
            }
        }
    }

    /**
     * SnapshotTable cell renderer styled to fit the {@link DividerTreeTableColumn}
     */
    private class DividerCell extends TreeTableCell {
        @Override
        protected void updateItem(final Object object, final boolean empty) {
            super.updateItem(object, empty);
            getStyleClass().add("divider");
        }
    }

    /**
     * A table column styled to act as a divider between other columns.
     */
    private class DividerTreeTableColumn extends TreeTableColumn {

        public DividerTreeTableColumn() {
            setPrefWidth(10);
            setMinWidth(10);
            setMaxWidth(50);
            setCellFactory(c -> new DividerCell());
        }
    }

    private class IdTreeTableCell extends TreeTableCell<TreeTableEntry, Integer> {
        @Override
        protected void updateItem(Integer integer, boolean empty) {
            super.updateItem(integer, empty);

            if (integer == null || empty || integer < 0) {
                setText(null);
            } else {
                setText(integer.toString());
            }
        }
    }

    private class ReadbackPVNameTreeTableCell extends TreeTableCell<TreeTableEntry, TreeTableEntry> {
        @Override
        protected void updateItem(TreeTableEntry item, boolean empty) {
            super.updateItem(item, empty);

            if (item == null || empty || item.folder) {
                setText(null);
            } else {
                setText(item.tableEntry.readbackNameProperty().get());
            }

            setGraphic(null);
        }
    }


    private static class PVNameTreeTableCell extends TreeTableCell<TreeTableEntry, TreeTableEntry> {

        private final HBox box;
        private final CheckBox checkBox;
        private final ImageView folderIconImageView = new ImageView(ImageRepository.FOLDER);

        private ObservableValue<Boolean> booleanProperty;

        private BooleanProperty indeterminateProperty;
        private BooleanProperty disabledProperty;

        public PVNameTreeTableCell() {
            getStyleClass().add("check-box-tree-table-cell");
            box = new HBox();
            box.setSpacing(5);

            checkBox = new CheckBox();

            setGraphic(null);
        }

        @Override
        protected void updateItem(TreeTableEntry item, boolean empty) {
            super.updateItem(item, empty);

            if (item == null || empty) {
                setText(null);
                setGraphic(null);
            } else {
                setText(item.name);

                box.getChildren().clear();
                box.getChildren().add(checkBox);
                if (item.folder) {
                    box.getChildren().add(folderIconImageView);
                }

                setGraphic(box);

                if (booleanProperty instanceof BooleanProperty) {
                    checkBox.selectedProperty().unbindBidirectional((BooleanProperty) booleanProperty);
                }

                if (indeterminateProperty != null) {
                    checkBox.indeterminateProperty().unbindBidirectional(indeterminateProperty);
                }

                if (disabledProperty != null) {
                    checkBox.disableProperty().unbindBidirectional(disabledProperty);
                }

                booleanProperty = item.selected;
                checkBox.selectedProperty().bindBidirectional((BooleanProperty) booleanProperty);

                indeterminateProperty = item.indeterminate;
                checkBox.indeterminateProperty().bindBidirectional(indeterminateProperty);

                disabledProperty = item.disabled;
                checkBox.disableProperty().bindBidirectional(disabledProperty);
            }
        }
    }

    private static class StringTreeTableCell extends TreeTableCell<TreeTableEntry, String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            TreeTableEntry entry = getTableRow().getItem();
            if (item == null || empty || entry.folder) {
                setText("");
            } else {
                setText(item);
            }
        }
    }
}
