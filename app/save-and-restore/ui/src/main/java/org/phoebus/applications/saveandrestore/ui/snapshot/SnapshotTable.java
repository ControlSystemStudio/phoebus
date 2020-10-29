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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
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
import org.epics.vtype.VStringArray;
import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.Utilities;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.ui.MultitypeTableCell;
import org.phoebus.applications.saveandrestore.ui.model.VDisconnectedData;
import org.phoebus.applications.saveandrestore.ui.model.VNoData;
import org.phoebus.applications.saveandrestore.ui.model.VSnapshot;
import org.phoebus.applications.saveandrestore.ui.model.VTypePair;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.ui.application.ContextMenuHelper;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.stream.Collectors;


class SnapshotTable extends TableView<TableEntry> {

    private TableColumn<TableEntry, ConfigPv> pvNameColumn;

    private static boolean resizePolicyNotInitialized = true;
    private static PrivilegedAction<Object> resizePolicyAction = () -> {
        try {
            // Java FX bugfix: the table columns are not properly resized for the first table
            Field f = TableView.CONSTRAINED_RESIZE_POLICY.getClass().getDeclaredField("isFirstRun");
            f.setAccessible(true);
            f.set(TableView.CONSTRAINED_RESIZE_POLICY, Boolean.FALSE);
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
    private static class TimestampTableCell extends TableCell<TableEntry, Instant> {
        @Override
        protected void updateItem(Instant item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setStyle("");
            } else if (item == null) {
                setText("---");
                setStyle("");
            } else {
                setText(Utilities.timestampToLittleEndianString(item, true));
            }
        }
    }

    /**
     * <code>VTypeCellEditor</code> is an editor type for {@link org.epics.vtype.VType} or {@link org.phoebus.applications.saveandrestore.ui.model.VTypePair}, which allows editing the
     * value as a string.
     *
     * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
     *
     * @param <T> {@link org.epics.vtype.VType} or {@link org.phoebus.applications.saveandrestore.ui.model.VTypePair}
     */
    private static class VTypeCellEditor<T> extends MultitypeTableCell<TableEntry, T> {
        private static final Image DISCONNECTED_IMAGE = new Image(
                SnapshotController.class.getResourceAsStream("/icons/showerr_tsk.png"));
        private final Tooltip tooltip = new Tooltip();

        VTypeCellEditor() {

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
                    }
                    else if (item instanceof VTypePair) {
                        VType value = ((VTypePair) item).value;
                        if (value instanceof VNumber) {
                            return ((VNumber) value).getValue().toString();
                        } else if (value instanceof VNumberArray) {
                            return ((VNumberArray) value).getData().toString();
                        } else if (value instanceof VEnum) {
                            return ((VEnum) value).getValue();
                        }
                        else {
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
            // FX does not provide any facilities to get the column index at mouse position, so use this hack, to know
            // where the mouse is located
            setOnMouseEntered(e -> ((SnapshotTable) getTableView()).setColumnAndRowAtMouse(getTableColumn(), getIndex()));
            setOnMouseExited(e -> ((SnapshotTable) getTableView()).setColumnAndRowAtMouse(null, -1));
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
            if (item == null || empty) {
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
    private static class VDeltaCellEditor<T> extends VTypeCellEditor<T> {

        private static final Image WARNING_IMAGE = new Image(
                SnapshotController.class.getResourceAsStream("/icons/hprio_tsk.png"));
        private static final Image DISCONNECTED_IMAGE = new Image(
                SnapshotController.class.getResourceAsStream("/icons/showerr_tsk.png"));
        private final Tooltip tooltip = new Tooltip();

        private boolean showDeltaPercentage = false;
        private void setShowDeltaPercentage() { showDeltaPercentage = true; }

        VDeltaCellEditor() {
            super();
        }

        @Override
        public void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            getStyleClass().remove("diff-cell");
            if (item == null || empty) {
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
     * <code>TooltipTableColumn</code> is the common table column implementation, which can also provide the tooltip.
     *
     * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
     *
     * @param <T> the type of the values displayed by this column
     */
    private class TooltipTableColumn<T> extends TableColumn<TableEntry, T> {
        private String text;
        private Label label;

        TooltipTableColumn(String text, String tooltip, int minWidth) {
            setup(text, tooltip, minWidth, -1, true);
        }

        TooltipTableColumn(String text, String tooltip, int minWidth, int prefWidth, boolean resizable) {
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
     * <code>SelectionTableColumn</code> is the table column for the first column in the table, which displays
     * a checkbox, whether the PV should be selected or not.
     *
     * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
     *
     */
    private class SelectionTableColumn extends TooltipTableColumn<Boolean> {
        SelectionTableColumn() {
            super("", "Include this PV when restoring values", 30, 30, false);
            setCellValueFactory(new PropertyValueFactory<>("selected"));
            //for those entries, which have a read-only property, disable the checkbox
            setCellFactory(column -> {
                TableCell<TableEntry, Boolean> cell = new CheckBoxTableCell<>(null,null);
                cell.itemProperty().addListener((a, o, n) -> {
                    cell.getStyleClass().remove("check-box-table-cell-disabled");
                    TableRow<?> row = cell.getTableRow();
                    if (row != null) {
                        TableEntry item = (TableEntry) row.getItem();
                        if (item != null) {
                            cell.setEditable(!item.readOnlyProperty().get());
                            if (item.readOnlyProperty().get()) {
                                cell.getStyleClass().add("check-box-table-cell-disabled");
                            }
                        }
                    }
                });
                return cell;
            });
            setEditable(true);
            setSortable(false);
            selectAllCheckBox = new CheckBox();
            selectAllCheckBox.setSelected(false);
            selectAllCheckBox.setOnAction(e -> getItems().stream().filter(te -> !te.readOnlyProperty().get())
                    .forEach(te -> te.selectedProperty().setValue(selectAllCheckBox.isSelected())));
            setGraphic(selectAllCheckBox);
            MenuItem inverseMI = new MenuItem("Inverse Selection");
            inverseMI.setOnAction(e -> getItems().stream().filter(te -> !te.readOnlyProperty().get())
                    .forEach(te -> te.selectedProperty().setValue(!te.selectedProperty().get())));
            final ContextMenu contextMenu = new ContextMenu(inverseMI);
            selectAllCheckBox.setOnMouseReleased(e -> {
                if (e.getButton() == MouseButton.SECONDARY) {
                    contextMenu.show(selectAllCheckBox, e.getScreenX(), e.getScreenY());
                }
            });
        }
    }

    private final List<VSnapshot> uiSnapshots = new ArrayList<>();
    private boolean showStoredReadbacks;
    private boolean showReadbacks;
    private boolean showDeltaPercentage;
    private final SnapshotController controller;
    private CheckBox selectAllCheckBox;

    private TableColumn<TableEntry, ?> columnAtMouse;
    private int rowAtMouse = -1;
    private int clickedColumn = -1;
    private int clickedRow = -1;

    /**
     * Constructs a new table.
     *
     * @param controller the controller
     */
    SnapshotTable(SnapshotController controller) {
        if (resizePolicyNotInitialized) {
            AccessController.doPrivileged(resizePolicyAction);
        }
        this.controller = controller;
        setEditable(true);
        getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        setMaxWidth(Double.MAX_VALUE);
        setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(this, Priority.ALWAYS);
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        getStylesheets().add(SnapshotTable.class.getResource("/style.css").toExternalForm());

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

        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() != KeyCode.SPACE) {
                return;
            }

            ObservableList<TableEntry> selections = getSelectionModel().getSelectedItems();

            if (selections == null) {
                return;
            }

            selections.stream().forEach(item -> item.selectedProperty().setValue(!item.selectedProperty().get()));

            // Somehow JavaFX TableView handles SPACE pressed event as going into edit mode of the cell.
            // Consuming event prevents NullPointerException.
            event.consume();
        });

        setRowFactory(tableView -> new TableRow<>() {
            final ContextMenu contextMenu = new ContextMenu();

            @Override
            protected void updateItem(TableEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setOnContextMenuRequested(null);
                } else {
                    setOnContextMenuRequested(event -> {
                        List<ProcessVariable> selectedPVList = getSelectionModel().getSelectedItems().stream()
                                .map(tableEntry -> new ProcessVariable(tableEntry.pvNameProperty().get()))
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

    /**
     * Set the column and row number at current mouse position.
     *
     * @param column the column at mouse cursor (null if none)
     * @param row the row index at mouse cursor
     */
    private void setColumnAndRowAtMouse(TableColumn<TableEntry, ?> column, int row) {
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
        List<TableColumn<TableEntry, ?>> snapshotTableEntries = new ArrayList<>(12);

        TableColumn<TableEntry, Boolean> selectedColumn = new SelectionTableColumn();
        snapshotTableEntries.add(selectedColumn);

        int width = measureStringWidth("000", Font.font(20));
        TableColumn<TableEntry, Integer> idColumn = new TooltipTableColumn<>("#",
            Messages.toolTipTableColumIndex, width, width, false);
        idColumn.setCellValueFactory(cell -> {
            int idValue = cell.getValue().idProperty().get();
            idColumn.setPrefWidth(Math.max(idColumn.getWidth(), measureStringWidth(String.valueOf(idValue), Font.font(20))));

            return new ReadOnlyObjectWrapper(idValue);
        });
        snapshotTableEntries.add(idColumn);

        pvNameColumn = new TooltipTableColumn<>("PV Name",
                Messages.toolTipTableColumnPVName, 100);

        pvNameColumn.setCellValueFactory(new PropertyValueFactory<>("pvName"));
        snapshotTableEntries.add(pvNameColumn);

        if (showLiveReadback) {
            TableColumn<TableEntry, ConfigPv> readbackPVName = new TooltipTableColumn<>("Readback\nPV Name",
                    Messages.toolTipTableColumnReadbackPVName, 100);
            readbackPVName.setCellValueFactory(new PropertyValueFactory<>("readbackName"));
            snapshotTableEntries.add(readbackPVName);
        }

        width = measureStringWidth("MM:MM:MM.MMM MMM MM M", null);
        TableColumn<TableEntry, Instant> timestampColumn = new TooltipTableColumn<>("Timestamp",
            Messages.toolTipTableColumnTimestamp, width, width, true);
        timestampColumn.setCellValueFactory(new PropertyValueFactory<TableEntry, Instant>("timestamp"));
        timestampColumn.setCellFactory(c -> new TimestampTableCell());
        timestampColumn.setPrefWidth(width);
        snapshotTableEntries.add(timestampColumn);

        TableColumn<TableEntry, String> statusColumn = new TooltipTableColumn<>("Status",
            Messages.toolTipTableColumnAlarmStatus, 100, 100, true);
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        snapshotTableEntries.add(statusColumn);

        TableColumn<TableEntry, String> severityColumn = new TooltipTableColumn<>("Severity",
                Messages.toolTipTableColumnAlarmSeverity, 100, 100, true);
        severityColumn.setCellValueFactory(new PropertyValueFactory<>("severity"));
        snapshotTableEntries.add(severityColumn);

        TableColumn<TableEntry, ?> storedValueBaseColumn = new TooltipTableColumn<>(
                "Stored Setpoint", "", -1);

        TableColumn<TableEntry, VType> storedValueColumn = new TooltipTableColumn<>(
            "Stored Setpoint",
            Messages.toolTipTableColumnSetpointPVValue, 100);
        storedValueColumn.setCellValueFactory(new PropertyValueFactory<>("snapshotVal"));
        storedValueColumn.setCellFactory(e -> new VTypeCellEditor<>());
        storedValueColumn.setEditable(true);
        storedValueColumn.setOnEditCommit(e -> {
            ObjectProperty<VTypePair> value = e.getRowValue().valueProperty();
            value.setValue(new VTypePair(value.get().base, e.getNewValue(), value.get().threshold));
            controller.updateSnapshot(0, e.getRowValue(), e.getNewValue());
        });

        storedValueBaseColumn.getColumns().add(storedValueColumn);
        // show deltas in separate column
        TableColumn<TableEntry, VTypePair> delta = new TooltipTableColumn<>(
                Utilities.DELTA_CHAR + " Live Setpoint",
                "", 100);
        delta.setCellValueFactory(e -> e.getValue().valueProperty());
        delta.setCellFactory(e -> {
            VDeltaCellEditor vDeltaCellEditor = new VDeltaCellEditor<>();
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
        storedValueBaseColumn.getColumns().add(delta);

        snapshotTableEntries.add(storedValueBaseColumn);

        if (showStoredReadback) {
            TableColumn<TableEntry, VType> storedReadbackColumn = new TooltipTableColumn<>(
                    "Stored Readback\n(" + Utilities.DELTA_CHAR + " Stored Setpoint)", "Stored Readback Value", 100);
            storedReadbackColumn.setCellValueFactory(new PropertyValueFactory<>("storedReadback"));
            storedReadbackColumn.setCellFactory(e -> new VTypeCellEditor<>());
            storedReadbackColumn.setEditable(false);
            snapshotTableEntries.add(storedReadbackColumn);
        }

        TableColumn<TableEntry, VType> liveValueColumn = new TooltipTableColumn<>("Live Setpoint", "Current PV Value",
            100);
        liveValueColumn.setCellValueFactory(new PropertyValueFactory<>("liveValue"));
        liveValueColumn.setCellFactory(e -> new VTypeCellEditor<>());
        liveValueColumn.setEditable(false);
        snapshotTableEntries.add(liveValueColumn);


        if (showLiveReadback) {
            TableColumn<TableEntry, VType> readbackColumn = new TooltipTableColumn<>(
                    "Live Readback\n(" + Utilities.DELTA_CHAR + " Live Setpoint)", "Current Readback Value", 100);
            readbackColumn.setCellValueFactory(new PropertyValueFactory<>("liveReadback"));
            readbackColumn.setCellFactory(e -> new VTypeCellEditor<>());
            readbackColumn.setEditable(false);
            snapshotTableEntries.add(readbackColumn);
        }

        getColumns().addAll(snapshotTableEntries);
    }

    private void createTableForMultipleSnapshots(List<VSnapshot> snapshots) {
        List<TableColumn<TableEntry, ?>> list = new ArrayList<>(7);
        TableColumn<TableEntry, Boolean> selectedColumn = new SelectionTableColumn();
        list.add(selectedColumn);

        int width = measureStringWidth("000", Font.font(20));
        TableColumn<TableEntry, Integer> idColumn = new TooltipTableColumn<>("#",
            Messages.toolTipTableColumIndex, width, width, false);
        idColumn.setCellValueFactory(cell -> {
            int idValue = cell.getValue().idProperty().get();
            idColumn.setPrefWidth(Math.max(idColumn.getWidth(), measureStringWidth(String.valueOf(idValue), Font.font(20))));

            return new ReadOnlyObjectWrapper(idValue);
        });
        list.add(idColumn);

        TableColumn<TableEntry, String> setpointPVName = new TooltipTableColumn<>("PV Name",
            Messages.toolTipUnionOfSetpointPVNames, 100);
        setpointPVName.setCellValueFactory(new PropertyValueFactory<>("pvName"));
        list.add(setpointPVName);

        list.add(new DividerTableColumn());

        TableColumn<TableEntry, ?> storedValueColumn = new TooltipTableColumn<>("Stored Values",
            Messages.toolTipTableColumnPVValues, -1);
        storedValueColumn.getStyleClass().add("toplevel");

        String snapshotName = snapshots.get(0).getSnapshot().get().getName() + " (" +
                String.valueOf(snapshots.get(0)) + ")";


        TableColumn<TableEntry, ?> baseCol = new TooltipTableColumn<>(
            snapshotName,
            Messages.toolTipTableColumnSetpointPVValue, 33);
        baseCol.getStyleClass().add("second-level");

        TableColumn<TableEntry, VType> storedBaseSetpointValueColumn = new TooltipTableColumn<>(
            "Base Setpoint",
            Messages.toolTipTableColumnBaseSetpointValue, 100);

        storedBaseSetpointValueColumn.setCellValueFactory(new PropertyValueFactory<>("snapshotVal"));
        storedBaseSetpointValueColumn.setCellFactory(e -> new VTypeCellEditor<>());
        storedBaseSetpointValueColumn.setEditable(true);
        storedBaseSetpointValueColumn.setOnEditCommit(e -> {
            ObjectProperty<VTypePair> value = e.getRowValue().valueProperty();
            value.setValue(new VTypePair(value.get().base, e.getNewValue(), value.get().threshold));
            controller.updateSnapshot(0, e.getRowValue(), e.getNewValue());

            for (int i = 1; i < snapshots.size(); i++) {
                ObjectProperty<VTypePair> compareValue = e.getRowValue().compareValueProperty(i);
                compareValue.setValue(new VTypePair(e.getNewValue(), compareValue.get().value, compareValue.get().threshold));
            }
        });

        baseCol.getColumns().add(storedBaseSetpointValueColumn);

        // show deltas in separate column
        TableColumn<TableEntry, VTypePair> delta = new TooltipTableColumn<>(
                Utilities.DELTA_CHAR + " Live Setpoint",
                "", 100);

        delta.setCellValueFactory(e -> e.getValue().valueProperty());
        delta.setCellFactory(e -> {
            VDeltaCellEditor vDeltaCellEditor = new VDeltaCellEditor<>();
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

        storedValueColumn.getColumns().addAll(baseCol, new DividerTableColumn());

        for (int i = 1; i < snapshots.size(); i++) {
            final int snapshotIndex = i;

            snapshotName = snapshots.get(snapshotIndex).getSnapshot().get().getName() + " (" +
                    String.valueOf(snapshots.get(snapshotIndex)) + ")";


            TooltipTableColumn<VTypePair> baseSnapshotCol = new TooltipTableColumn<>(snapshotName,
                    "Setpoint PV value when the " + snapshotName + " snapshot was taken", 100);
            baseSnapshotCol.getStyleClass().add("second-level");

            TooltipTableColumn<VTypePair> setpointValueCol = new TooltipTableColumn<>(
                    "Setpoint",
                    "Setpoint PV value when the " + snapshotName + " snapshot was taken", 66);


            setpointValueCol.setCellValueFactory(e -> e.getValue().compareValueProperty(snapshotIndex));
            setpointValueCol.setCellFactory(e -> new VTypeCellEditor<>());
            setpointValueCol.setEditable(false);

            baseSnapshotCol.getColumns().add(setpointValueCol);

            TooltipTableColumn<VTypePair> deltaCol = new TooltipTableColumn<>(
                 Utilities.DELTA_CHAR + " Base Setpoint",
                "Setpoint PVV value when the " + snapshotName + " snapshot was taken", 50);
            deltaCol.setCellValueFactory(e -> e.getValue().compareValueProperty(snapshotIndex));
            deltaCol.setCellFactory(e -> {
                VDeltaCellEditor vDeltaCellEditor = new VDeltaCellEditor<>();
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
            storedValueColumn.getColumns().addAll(baseSnapshotCol, new DividerTableColumn());
        }
        list.add(storedValueColumn);

        TableColumn<TableEntry, VType> liveValueColumn = new TooltipTableColumn<>("Live Setpoint",
            "Current Setpoint value", 100);

        liveValueColumn.setCellValueFactory(new PropertyValueFactory<>("liveValue"));
        liveValueColumn.setCellFactory(e -> new VTypeCellEditor<>());
        liveValueColumn.setEditable(false);
        list.add(liveValueColumn);

        getColumns().addAll(list);
    }

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
        final ObservableList<TableEntry> items = getItems();
        final boolean notHide = !controller.isHideEqualItems();
        items.clear();
        entries.forEach(e -> {
            // there is no harm if this is executed more than once, because only one line is allowed for these
            // two properties (see SingleListenerBooleanProperty for more details)
            e.selectedProperty()
                .addListener((a, o, n) -> selectAllCheckBox.setSelected(n ? selectAllCheckBox.isSelected() : false));
            e.liveStoredEqualProperty().addListener((a, o, n) -> {
                if (controller.isHideEqualItems()) {
                    if (n) {
                        getItems().remove(e);
                    } else {
                        getItems().add(e);
                    }
                }
            });
            if (notHide || !e.liveStoredEqualProperty().get()) {
                items.add(e);
            }
        });
    }

    /**
     * Update the table column titles, by putting an asterisk to non saved snapshots or remove asterisk from saved
     * snapshots.
     */
    private void updateTableColumnTitles() {
        // add the * to the title of the column if the snapshot is not saved
        if (uiSnapshots.size() == 1) {
            ((TooltipTableColumn<?>) getColumns().get(6)).setSaved(true); //uiSnapshots.get(0).isSaved());
        } else {
            TableColumn<TableEntry, ?> column = getColumns().get(4);
            for (int i = 0; i < uiSnapshots.size(); i++) {
                TableColumn tableColumn = column.getColumns().get(i);
                if(tableColumn instanceof DividerTableColumn){
                    continue;
                }
                ((TooltipTableColumn<?>) tableColumn).setSaved(true); //uiSnapshots.get(i).isSaved());
            }
        }
    }

    /**
     * SnapshotTable cell renderer styled to fit the {@link DividerTableColumn}
     */
    private class DividerCell extends TableCell
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
    private class DividerTableColumn extends TableColumn{

        public DividerTableColumn(){
            setPrefWidth(10);
            setMinWidth(10);
            setMaxWidth(50);
            setCellFactory(c -> new DividerCell());
        }
    }
}
