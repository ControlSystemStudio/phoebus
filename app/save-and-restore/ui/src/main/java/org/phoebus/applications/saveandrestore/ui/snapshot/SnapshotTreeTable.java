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
import org.phoebus.applications.saveandrestore.ApplicationContextProvider;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.Utilities;
import org.phoebus.applications.saveandrestore.ui.MultitypeTreeTableCell;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreController;
import org.phoebus.applications.saveandrestore.ui.model.VDisconnectedData;
import org.phoebus.applications.saveandrestore.ui.model.VNoData;
import org.phoebus.applications.saveandrestore.ui.model.VSnapshot;
import org.phoebus.applications.saveandrestore.ui.model.VTypePair;
import org.phoebus.applications.saveandrestore.ui.snapshot.hierarchyparser.IHierarchyParser;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.preferences.PreferencesReader;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.ui.application.ContextMenuHelper;
import org.phoebus.ui.javafx.ImageCache;

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

    private TreeTableEntry rootTTE = new TreeTableEntry("ROOT", null, null);
    private IHierarchyParser hierarchyParser = null;

    public static final Logger LOGGER = Logger.getLogger(SnapshotController.class.getName());

    public static final Image folderIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/save-and-restore/folder.png");

    private static boolean resizePolicyNotInitialized = true;
    private static PrivilegedAction<Object> resizePolicyAction = () -> {
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
     *
     */
    private static class TimestampTreeTableCell extends TreeTableCell<TreeTableEntry, TreeTableEntry> {
        @Override
        protected void updateItem(TreeTableEntry item, boolean empty) {
            super.updateItem(item, empty);

            if (item == null || empty) {
                setText("");
                setStyle("");
            } else {
                TreeTableEntry entry = getTreeTableRow().getItem();
                if (entry == null || entry.folder) {
                    setText(null);
                    setStyle("");
                } else {
                    Instant instant = item.tableEntry.timestampProperty().get();
                    if (instant == null) {
                        setText("---");
                    } else {
                        setText(Utilities.timestampToLittleEndianString(item.tableEntry.timestampProperty().get(), true));
                    }
                }
            }
        }
    }

    /**
     * <code>VTypeCellEditor</code> is an editor type for {@link VType} or {@link VTypePair}, which allows editing the
     * value as a string.
     *
     * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
     *
     * @param <T> {@link VType} or {@link VTypePair}
     */
    private static class VTypeTreeCellEditor<T> extends MultitypeTreeTableCell<TreeTableEntry, T> {
        private static final Image WARNING_IMAGE = new Image(
            SnapshotController.class.getResourceAsStream("/icons/hprio_tsk.png"));
        private static final Image DISCONNECTED_IMAGE = new Image(
                SnapshotController.class.getResourceAsStream("/icons/showerr_tsk.png"));
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
//                        FXMessageDialog.openError(controller.getSnapshotReceiver().getShell(), "Editing Error",
//                            e.getMessage());
                        return item;
                    }
                }
            });
            // FX does not provide any facilities to get the column index at mouse position, so use this hack, to know
            // where the mouse is located
            setOnMouseEntered(e -> ((SnapshotTreeTable) getTreeTableView()).setColumnAndRowAtMouse(getTableColumn(), getIndex()));
            setOnMouseExited(e -> ((SnapshotTreeTable) getTreeTableView()).setColumnAndRowAtMouse(null, -1));
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

            TreeTableEntry entry = getTreeTableRow().getItem();
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
                    tooltip.setText("No Value Available");
                    setTooltip(tooltip);
                    getStyleClass().add("diff-cell");
                } else if (item == VNoData.INSTANCE) {
                    setText(item.toString());
                    tooltip.setText("No Value Available");
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
     * @author Kunal Shroff
     *
     * @param <T>
     */
    private static class VDeltaTreeCellEditor<T> extends VTypeTreeCellEditor<T> {

        private static final Image WARNING_IMAGE = new Image(
                SnapshotController.class.getResourceAsStream("/icons/hprio_tsk.png"));
        private static final Image DISCONNECTED_IMAGE = new Image(
                SnapshotController.class.getResourceAsStream("/icons/showerr_tsk.png"));
        private final Tooltip tooltip = new Tooltip();

        private boolean showDeltaPercentage = false;
        private void setShowDeltaPercentage() { showDeltaPercentage = true; }

        VDeltaTreeCellEditor() {
            super();
        }

        @Override
        public void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            getStyleClass().remove("diff-cell");

            TreeTableEntry entry = getTreeTableRow().getItem();
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
                    tooltip.setText("No Value Available");
                    setTooltip(tooltip);
                    getStyleClass().add("diff-cell");
                } else if (item == VNoData.INSTANCE) {
                    setText(item.toString());
                    tooltip.setText("No Value Available");
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
     * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
     *
     * @param <T> the type of the values displayed by this column
     */
    private class TooltipTreeTableColumn<T> extends TreeTableColumn<TreeTableEntry, T> {
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
//            setOnEditStart(e -> controller.suspend());
//            setOnEditCancel(e -> controller.resume());
//            setOnEditCommit(e -> controller.resume());
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
     *
     */
    private class SelectionTreeTableColumn extends TooltipTreeTableColumn<TreeTableEntry> {
        SelectionTreeTableColumn() {
            super("", "Include this PV when restoring values", 30, 30, false);
            setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getValue()));
            //for those entries, which have a read-only property, disable the checkbox
            setCellFactory(cell -> new SelectionTreeTableColumnCell());
            setEditable(true);
            setSortable(false);
            selectAllCheckBox = new CheckBox();
            selectAllCheckBox.setSelected(false);
            selectAllCheckBox.setOnAction(action -> rootTTE.cbti.setSelected(selectAllCheckBox.isSelected()));
//            selectAllCheckBox.setOnAction(action -> treeTableEntryItems.values().stream().filter(item -> !item.folder)
//                        .filter(item -> !item.tableEntry.readOnlyProperty().get())
//                        .forEach(item -> item.tableEntry.selectedProperty().set(selectAllCheckBox.isSelected())));
            setGraphic(selectAllCheckBox);
            MenuItem inverseMI = new MenuItem("Inverse Selection");
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

    private class SelectionTreeTableColumnCell extends TreeTableCell<TreeTableEntry, TreeTableEntry> {
        private final CheckBox checkBox;
        private final CheckBox nullCheckBox;

        private ObservableValue<Boolean> booleanProperty;
        private BooleanProperty indeterminateProperty;

        public SelectionTreeTableColumnCell() {
            checkBox = new CheckBox();

            nullCheckBox = new CheckBox();
            nullCheckBox.setDisable(true);
            nullCheckBox.setSelected(false);

            setGraphic(null);
        }

        @Override
        protected void updateItem(TreeTableEntry item, boolean empty) {
            super.updateItem(item, empty);

            if (item == null || empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (!item.folder && item.tableEntry.readOnlyProperty().get()) {
                    setGraphic(nullCheckBox);
                    return;
                }

                setGraphic(checkBox);

                if (booleanProperty instanceof BooleanProperty) {
                    checkBox.selectedProperty().unbindBidirectional((BooleanProperty) booleanProperty);
                }

                if (indeterminateProperty != null) {
                    checkBox.indeterminateProperty().unbindBidirectional(indeterminateProperty);
                }

                booleanProperty = item.selected;
                checkBox.selectedProperty().bindBidirectional((BooleanProperty) booleanProperty);

                indeterminateProperty = item.indeterminate;
                checkBox.indeterminateProperty().bindBidirectional(indeterminateProperty);
            }
        }
    }

    private final List<VSnapshot> uiSnapshots = new ArrayList<>();
    private boolean showStoredReadbacks;
    private boolean showReadbacks;
    private boolean showDeltaPercentage;
    private final SnapshotController controller;
    private final Map<String, TreeTableEntry> treeTableEntryItems = new HashMap<>();
    private CheckBox selectAllCheckBox;

    private TreeTableColumn<TreeTableEntry, ?> columnAtMouse;
    private int rowAtMouse = -1;
    private int clickedColumn = -1;
    private int clickedRow = -1;

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

        setOnMouseClicked(e -> {
            if (getSelectionModel().getSelectedCells() != null && !getSelectionModel().getSelectedCells().isEmpty()) {
                if (columnAtMouse == null) {
                    clickedColumn = getSelectionModel().getSelectedCells().get(0).getColumn();
                } else {
                    int idx = getColumns().indexOf(columnAtMouse);
                    if (uiSnapshots.size() > 1) {
                        int i = showReadbacks ? 4 : 3;
                        if (idx < 0) {
                            // it is one of the grouped stored values columns
                            idx = getColumns().get(i).getColumns().indexOf(columnAtMouse);
                            if (idx >= 0) {
                                idx += i;
                            }
                        } else {
                            // it is either one of the first 3 columns (do nothing) or one of the live columns
                            if (idx > i) {
                                idx = getColumns().get(i).getColumns().size() + idx - 1;
                            }
                        }
                    }
                    if (idx < 0) {
                        clickedColumn = getSelectionModel().getSelectedCells().get(0).getColumn();
                    } else {
                        clickedColumn = idx;
                    }
                }
                clickedRow = rowAtMouse == -1 ? getSelectionModel().getSelectedCells().get(0).getRow() : rowAtMouse;
            }
        });

        PreferencesReader preferencesReader = (PreferencesReader) ApplicationContextProvider.getApplicationContext().getBean("preferencesReader");
        String parserClassName = preferencesReader.get("treeTableView.hierarchyParser");
        try {
            hierarchyParser = (IHierarchyParser) Class.forName(getClass().getPackageName() + ".hierarchyparser." + parserClassName).getConstructor().newInstance();
        } catch (Exception e) {
            e.printStackTrace();

            LOGGER.severe("Unable to find " + getClass().getPackageName() + ".hierarchyparser." + parserClassName + "!");
        }

        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() != KeyCode.SPACE) {
                return;
            }

            ObservableList<TreeItem<TreeTableEntry>> selections = getSelectionModel().getSelectedItems();

            if (selections == null) {
                return;
            }

            selections.stream().forEach(item -> item.getValue().selected.setValue(!item.getValue().selected.get()));

            // Somehow JavaFX TableView handles SPACE pressed event as going into edit mode of the cell.
            // Consuming event prevents NullPointerException.
            event.consume();
        });

        setRowFactory(tableView -> new TreeTableRow<>() {
            final ContextMenu contextMenu= new ContextMenu();

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
                        contextMenu.show(this, event.getScreenX(), event.getScreenY());
                    });
                }
            }
        });
    }

    private void recursiveSortByName(ObservableList<TreeItem<TreeTableEntry>> list) {
        FXCollections.sort(list, Comparator.comparing((TreeItem<TreeTableEntry> tte) -> !tte.getValue().folder)
                        .thenComparing((TreeItem<TreeTableEntry> tte) -> tte.getValue().name));

        list.stream().forEach(item -> recursiveSortByName(item.getChildren()));
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
                    treeTableEntryItems.put(pvName, signal);
                }
            }
        }
    }

    /**
     * Set the column and row number at current mouse position.
     *
     * @param column the column at mouse cursor (null if none)
     * @param row the row index at mouse cursor
     */
    private void setColumnAndRowAtMouse(TreeTableColumn<TreeTableEntry, ?> column, int row) {
        this.columnAtMouse = column;
        this.rowAtMouse = row;
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

        TreeTableColumn<TreeTableEntry, TreeTableEntry> pvNameColumn = new TooltipTreeTableColumn<>("PV Name",
                Messages.toolTipTableColumnPVName, 100);

        pvNameColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getValue()));
        pvNameColumn.setCellFactory(cell -> new PVNameTreeTableCell());
        pvNameColumn.setComparator(Comparator.comparing((TreeTableEntry tte) -> !tte.folder).thenComparing(tte -> tte.name));

        snapshotTreeTableColumns.add(pvNameColumn);
        setTreeColumn(pvNameColumn);

        if (showLiveReadback) {
            TreeTableColumn<TreeTableEntry, String> readbackPVName = new TooltipTreeTableColumn<>("Readback\nPV Name",
                    Messages.toolTipTableColumnReadbackPVName, 100);
            readbackPVName.setCellValueFactory(new TreeItemPropertyValueFactory<>("readbackName"));
            snapshotTreeTableColumns.add(readbackPVName);
        }

        width = measureStringWidth("MM:MM:MM.MMM MMM MM M", null);
        TreeTableColumn<TreeTableEntry, TreeTableEntry> timestampColumn = new TooltipTreeTableColumn<>("Timestamp",
            Messages.toolTipTableColumnTimestamp, width, width, true);
        timestampColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getValue()));
        timestampColumn.setCellFactory(c -> new TimestampTreeTableCell());
        timestampColumn.setPrefWidth(width);
        snapshotTreeTableColumns.add(timestampColumn);

        TreeTableColumn<TreeTableEntry, String> statusColumn = new TooltipTreeTableColumn<>("Status",
            Messages.toolTipTableColumnAlarmStatus, 100, 100, true);
        statusColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("status"));
        statusColumn.setCellFactory(cell -> new StringTreeTableCell());
        snapshotTreeTableColumns.add(statusColumn);

        TreeTableColumn<TreeTableEntry, String> severityColumn = new TooltipTreeTableColumn<>("Severity",
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
            if (treeTableEntry.folder) {
                return;
            }

            ObjectProperty<VTypePair> value = treeTableEntry.tableEntry.valueProperty();
            value.setValue(new VTypePair(value.get().base, e.getNewValue(), value.get().threshold));
            controller.updateSnapshot(0, e.getRowValue().getValue().tableEntry, e.getNewValue());
        });

        storedValueBaseColumn.getColumns().add(storedValueColumn);
        // show deltas in separate column
        TreeTableColumn<TreeTableEntry, VTypePair> delta = new TooltipTreeTableColumn<>(
                Utilities.DELTA_CHAR + " Live Setpoint",
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

            if (!vtc1.isWithinThreshold() && vtc2.isWithinThreshold()) {
                return -1;
            } else if (vtc1.isWithinThreshold() && !vtc2.isWithinThreshold()) {
                return 1;
            } else {
                return 0;
            }
        });
        storedValueBaseColumn.getColumns().add(delta);

        snapshotTreeTableColumns.add(storedValueBaseColumn);

        if (showStoredReadback) {
            TreeTableColumn<TreeTableEntry, VType> storedReadbackColumn = new TooltipTreeTableColumn<>(
                    "Stored Readback\n(" + Utilities.DELTA_CHAR + " Stored Setpoint)", "Stored Readback Value", 100);
            storedReadbackColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("storedReadback"));
            storedReadbackColumn.setCellFactory(e -> new VTypeTreeCellEditor<>());
            storedReadbackColumn.setEditable(false);
            snapshotTreeTableColumns.add(storedReadbackColumn);
        }

        TreeTableColumn<TreeTableEntry, VType> liveValueColumn = new TooltipTreeTableColumn<>("Live Setpoint", "Current PV Value",
            100);
        liveValueColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("liveValue"));
        liveValueColumn.setCellFactory(e -> new VTypeTreeCellEditor<>());
        liveValueColumn.setEditable(false);
        snapshotTreeTableColumns.add(liveValueColumn);


        if (showLiveReadback) {
            TreeTableColumn<TreeTableEntry, VType> readbackColumn = new TooltipTreeTableColumn<>(
                    "Live Readback\n(" + Utilities.DELTA_CHAR + " Live Setpoint)", "Current Readback Value", 100);
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

        TreeTableColumn<TreeTableEntry, TreeTableEntry> setpointPVName = new TooltipTreeTableColumn<>("PV Name",
            Messages.toolTipUnionOfSetpointPVNames, 100);
        setpointPVName.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getValue()));
        setpointPVName.setCellFactory(cell -> new PVNameTreeTableCell());
        setpointPVName.setComparator(Comparator.comparing((TreeTableEntry tte) -> !tte.folder).thenComparing(tte -> tte.name));

        list.add(setpointPVName);
        setTreeColumn(setpointPVName);

        list.add(new DividerTreeTableColumn());

        TreeTableColumn<TreeTableEntry, ?> storedValueColumn = new TooltipTreeTableColumn<>("Stored Values",
            Messages.toolTipTableColumnPVValues, -1);
        storedValueColumn.getStyleClass().add("toplevel");

        String snapshotName = snapshots.get(0).getSnapshot().get().getName() + " (" +
                String.valueOf(snapshots.get(0)) + ")";


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
            if (treeTableEntry.folder) {
                return;
            }

            ObjectProperty<VTypePair> value = treeTableEntry.tableEntry.valueProperty();
            value.setValue(new VTypePair(value.get().base, e.getNewValue(), value.get().threshold));
            controller.updateSnapshot(0, e.getRowValue().getValue().tableEntry, e.getNewValue());

            for (int i = 1; i < snapshots.size(); i++) {
                ObjectProperty<VTypePair> compareValue = e.getRowValue().getValue().tableEntry.compareValueProperty(i);
                compareValue.setValue(new VTypePair(e.getNewValue(), compareValue.get().value, compareValue.get().threshold));
            }
        });

        baseCol.getColumns().add(storedBaseSetpointValueColumn);

        // show deltas in separate column
        TreeTableColumn<TreeTableEntry, VTypePair> delta = new TooltipTreeTableColumn<>(
                Utilities.DELTA_CHAR + " Live Setpoint",
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
                    String.valueOf(snapshots.get(snapshotIndex)) + ")";
//            final ContextMenu menu = createContextMenu(snapshotIndex);

            TooltipTreeTableColumn<VTypePair> baseSnapshotCol = new TooltipTreeTableColumn<>(snapshotName,
                    "Setpoint PV value when the " + snapshotName + " snapshot was taken", 100);
//            baseSnapshotCol.label.setContextMenu(menu);
            baseSnapshotCol.getStyleClass().add("second-level");

            TooltipTreeTableColumn<VTypePair> setpointValueCol = new TooltipTreeTableColumn<>(
                    "Setpoint",
                    "Setpoint PV value when the " + snapshotName + " snapshot was taken", 66);

//            setpointValueCol.label.setContextMenu(menu);
            setpointValueCol.setCellValueFactory(e -> {
                TreeTableEntry treeTableEntry = e.getValue().getValue();
                if (treeTableEntry.folder) {
                    return new ReadOnlyObjectWrapper(null);
                }

                return treeTableEntry.tableEntry.compareValueProperty(snapshotIndex);
            });
            setpointValueCol.setCellFactory(e -> new VTypeTreeCellEditor<>());
            setpointValueCol.setEditable(false);
//            setpointValueCol.label.setOnMouseReleased(e -> {
//                if (e.getButton() == MouseButton.SECONDARY) {
//                    menu.show(setpointValueCol.label, e.getScreenX(), e.getScreenY());
//                }
//            });
            baseSnapshotCol.getColumns().add(setpointValueCol);

            TooltipTreeTableColumn<VTypePair> deltaCol = new TooltipTreeTableColumn<>(
                 Utilities.DELTA_CHAR + " Base Setpoint",
                "Setpoint PVV value when the " + snapshotName + " snapshot was taken", 50);
//            deltaCol.label.setContextMenu(menu);
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
//            deltaCol.label.setOnMouseReleased(e -> {
//                if (e.getButton() == MouseButton.SECONDARY) {
//                    menu.show(deltaCol.label, e.getScreenX(), e.getScreenY());
//                }
//            });
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

        TreeTableColumn<TreeTableEntry, VType> liveValueColumn = new TooltipTreeTableColumn<>("Live Setpoint",
            "Current Setpoint value", 100);

        liveValueColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("liveValue"));
        liveValueColumn.setCellFactory(e -> new VTypeTreeCellEditor<>());
        liveValueColumn.setEditable(false);
        list.add(liveValueColumn);

        getColumns().addAll(list);
    }

    private ContextMenu createContextMenu(final int snapshotIndex) {
        MenuItem removeItem = new MenuItem("Remove");
//        removeItem.setOnAction(ev -> SaveAndRestoreService.getInstance().execute("Remove Snapshot",
//            () -> update(controller.removeSnapshot(snapshotIndex))));
        MenuItem setAsBaseItem = new MenuItem("Set As Base");
//        setAsBaseItem.setOnAction(ev -> SaveAndRestoreService.getInstance().execute("Set new base Snapshot",
//            () -> update(controller.setAsBase(snapshotIndex))));
        MenuItem moveToNewEditor = new MenuItem("Move To New Editor");
//        moveToNewEditor.setOnAction(ev -> SaveAndRestoreService.getInstance().execute("Open Snapshot",
//            () -> update(controller.moveSnapshotToNewEditor(snapshotIndex))));
        return new ContextMenu(removeItem, setAsBaseItem, new SeparatorMenuItem(), moveToNewEditor);
    }

//    private void update(final List<TableEntry> entries) {
//        final List<Snapshot> snaps = controller.getAllSnapshots();
//        // the readback properties are changed on the UI thread, however they are just flags, which do not have any
//        // effect on the data model, so they can be read by anyone at anytime
//        Platform.runLater(
//            () -> updateTable(entries, snaps, controller.isShowReadbacks(), controller.isShowStoredReadbacks()));
//    }

    /**
     * Updates the table by setting new content, including the structure. The table is always recreated, even if the new
     * structure is identical to the old one. This is slightly more expensive; however, this method is only invoked per
     * user request (button click).
     *
     * @param entries the table entries (rows) to set on the table
     * @param snapshots the snapshots which are currently displayed
     * @param showLiveReadback true if readback column should be visible or false otherwise
     * @param showStoredReadback true if the stored readback value columns should be visible or false otherwise
     * @param showDeltaPercentage true if delta percentage should be be visible or false otherwise
     */
    public void updateTable(List<TableEntry> entries, List<VSnapshot> snapshots, boolean showLiveReadback, boolean showStoredReadback, boolean showDeltaPercentage) {
        getColumns().clear();
        uiSnapshots.clear();
        // we should always know if we are showing the stored readback or not, to properly extract the selection
        this.showStoredReadbacks = showStoredReadback;
        this.showReadbacks = showLiveReadback;
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
            TreeTableEntry treeTableEntry = treeTableEntryItems.get(tableEntry.pvNameProperty().get());
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
            ((TooltipTreeTableColumn<?>) getColumns().get(6)).setSaved(true); //uiSnapshots.get(0).isSaved());
        } else {
            TreeTableColumn<TreeTableEntry, ?> column = getColumns().get(4);
            for (int i = 0; i < uiSnapshots.size(); i++) {
                TreeTableColumn tableColumn = column.getColumns().get(i);
                if(tableColumn instanceof DividerTreeTableColumn){
                    continue;
                }
                ((TooltipTreeTableColumn<?>) tableColumn).setSaved(true); //uiSnapshots.get(i).isSaved());
            }
        }
    }

    /**
     * SnapshotTable cell renderer styled to fit the {@link DividerTreeTableColumn}
     */
    private class DividerCell extends TreeTableCell
    {
        @Override
        protected void updateItem(final Object object, final boolean empty)
        {
            super.updateItem(object, empty);
            getStyleClass().add("divider");
        }
    }

    /**
     * A table column styled to act as a divider between other columns.
     */
    private class DividerTreeTableColumn extends TreeTableColumn{

        public DividerTreeTableColumn(){
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


    private class PVNameTreeTableCell extends TreeTableCell<TreeTableEntry, TreeTableEntry> {

        private final HBox box;
        private final CheckBox checkBox;
        private final CheckBox nullCheckBox;
        private final ImageView folderIconImageView = new ImageView(folderIcon);

        private ObservableValue<Boolean> booleanProperty;

        private BooleanProperty indeterminateProperty;

        public PVNameTreeTableCell() {
            getStyleClass().add("check-box-tree-table-cell");
            box = new HBox();
            box.setSpacing(5);

            checkBox = new CheckBox();
            nullCheckBox = new CheckBox();
            nullCheckBox.setDisable(true);
            nullCheckBox.setSelected(false);

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
                if (!item.folder && item.tableEntry.readOnlyProperty().get()) {
                    box.getChildren().add(nullCheckBox);
                } else {
                    box.getChildren().add(checkBox);
                }
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

                booleanProperty = item.selected;
                checkBox.selectedProperty().bindBidirectional((BooleanProperty) booleanProperty);

                indeterminateProperty = item.indeterminate;
                checkBox.indeterminateProperty().bindBidirectional(indeterminateProperty);
            }
        }
    }

    private class StringTreeTableCell extends TreeTableCell<TreeTableEntry, String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            TreeTableEntry entry = getTreeTableRow().getItem();
            if (item == null || empty || entry.folder) {
                setText("");
            } else {
                setText(item);
            }
        }
    }
}
