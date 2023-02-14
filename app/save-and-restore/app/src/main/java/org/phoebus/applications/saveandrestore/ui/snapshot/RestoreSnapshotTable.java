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
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.common.Utilities;
import org.phoebus.applications.saveandrestore.common.VNoData;
import org.phoebus.applications.saveandrestore.common.VTypePair;
import org.phoebus.applications.saveandrestore.model.Snapshot;
import org.phoebus.core.types.TimeStampedProcessVariable;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.ui.application.ContextMenuHelper;

import java.security.AccessController;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


class RestoreSnapshotTable extends SnapshotTable {

    /**
     * <code>SelectionTableColumn</code> is the table column for the first column in the table, which displays
     * a checkbox, whether the PV should be selected or not.
     *
     * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
     */
    private class SelectionTableColumn extends TooltipTableColumn<Boolean> {
        SelectionTableColumn() {
            super("", "Include this PV when restoring values", 30, 30, false);
            setCellValueFactory(new PropertyValueFactory<>("selected"));
            //for those entries, which have a read-only property, disable the checkbox
            setCellFactory(column -> {
                TableCell<TableEntry, Boolean> cell = new CheckBoxTableCell<>(null, null);
                // initialize the checkbox
                UpdateCheckboxState(cell);
                cell.itemProperty().addListener((a, o, n) -> {
                    UpdateCheckboxState(cell);
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

        private void UpdateCheckboxState(TableCell<TableEntry, Boolean> cell) {
            cell.getStyleClass().remove("check-box-table-cell-disabled");

            TableRow<?> row = cell.getTableRow();
            if (row != null) {
                TableEntry item = (TableEntry) row.getItem();
                if (item != null) {
                    cell.setEditable(!item.readOnlyProperty().get());
                    if (item.readOnlyProperty().get()) {
                        cell.getStyleClass().add("check-box-table-cell-disabled");
                    } else if (item.valueProperty().get().value.equals(VNoData.INSTANCE)) {
                        item.selectedProperty().set(false);
                    }
                }
            }
        }
    }

    private final List<Snapshot> uiSnapshots = new ArrayList<>();
    private boolean showStoredReadbacks;
    private boolean showReadbacks;
    private boolean showDeltaPercentage;
    private final RestoreSnapshotController controller;
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
    RestoreSnapshotTable(RestoreSnapshotController controller) {
        super(controller);
        if (resizePolicyNotInitialized) {
            AccessController.doPrivileged(resizePolicyAction);
        }
        this.controller = controller;
        setEditable(true);
        getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        setMaxWidth(Double.MAX_VALUE);
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        getStylesheets().add(RestoreSnapshotTable.class.getResource("/style.css").toExternalForm());

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

            selections.stream().filter(item -> !item.readOnlyProperty().get()).forEach(item -> item.selectedProperty().setValue(!item.selectedProperty().get()));

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
                        List<TimeStampedProcessVariable> selectedPVList = getSelectionModel().getSelectedItems().stream()
                                .map(tableEntry -> {
                                    Instant time = Instant.now();
                                    if (tableEntry.timestampProperty().getValue() != null) {
                                        time = tableEntry.timestampProperty().getValue();
                                    }
                                    return new TimeStampedProcessVariable(tableEntry.pvNameProperty().get(), time);
                                })
                                .collect(Collectors.toList());

                        contextMenu.hide();
                        contextMenu.getItems().clear();
                        SelectionService.getInstance().setSelection(SaveAndRestoreApplication.NAME, selectedPVList);
                        ContextMenuHelper.addSupportedEntries(this, contextMenu);
                        contextMenu.getItems().add(new SeparatorMenuItem());
                        MenuItem toggle = new MenuItem();
                        toggle.setText(item.readOnlyProperty().get() ? "Make restorable" : "Make readonly");
                        CheckBox toggleIcon = new CheckBox();
                        toggleIcon.setFocusTraversable(false);
                        toggleIcon.setSelected(item.readOnlyProperty().get());
                        toggle.setGraphic(toggleIcon);
                        toggle.setOnAction(actionEvent -> {
                            item.readOnlyProperty().setValue(!item.readOnlyProperty().get());
                            item.selectedProperty().set(!item.readOnlyProperty().get());
                        });
                        contextMenu.getItems().add(toggle);
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
     * @param row    the row index at mouse cursor
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

    private void createTableForMultipleSnapshots(List<Snapshot> snapshots) {
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

        String snapshotName = snapshots.get(0).getSnapshotNode().getName() + " (" +
                snapshots.get(0) + ")";

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
            VType updatedValue = e.getRowValue().readOnlyProperty().get() ? e.getOldValue() : e.getNewValue();

            ObjectProperty<VTypePair> value = e.getRowValue().valueProperty();
            value.setValue(new VTypePair(value.get().base, updatedValue, value.get().threshold));
            controller.updateLoadedSnapshot(0, e.getRowValue(), updatedValue);

            for (int i = 1; i < snapshots.size(); i++) {
                ObjectProperty<VTypePair> compareValue = e.getRowValue().compareValueProperty(i);
                compareValue.setValue(new VTypePair(updatedValue, compareValue.get().value, compareValue.get().threshold));
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

            snapshotName = snapshots.get(snapshotIndex).getSnapshotNode().getName() + " (" +
                    snapshots.get(snapshotIndex) + ")";


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
}
