/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 *
 */

package org.phoebus.applications.saveandrestore.ui.snapshot;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.Preferences;
import org.phoebus.applications.saveandrestore.SafeMultiply;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.RestoreResult;
import org.phoebus.applications.saveandrestore.model.Snapshot;
import org.phoebus.applications.saveandrestore.model.SnapshotData;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.ui.RestoreMode;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.SnapshotMode;
import org.phoebus.applications.saveandrestore.ui.VTypePair;
import org.phoebus.core.vtypes.VDisconnectedData;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.saveandrestore.util.SnapshotUtil;
import org.phoebus.saveandrestore.util.Threshold;
import org.phoebus.saveandrestore.util.Utilities;
import org.phoebus.saveandrestore.util.VNoData;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.time.DateTimePane;
import org.phoebus.util.time.TimestampFormats;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class SnapshotTableViewController extends BaseSnapshotTableViewController {

    @SuppressWarnings("unused")
    @FXML
    private TooltipTableColumn<String> pvNameColumn;
    @SuppressWarnings("unused")
    @FXML
    private TooltipTableColumn<Instant> timeColumn;
    @SuppressWarnings("unused")
    @FXML
    private TooltipTableColumn<VType> storedReadbackColumn;
    @SuppressWarnings("unused")
    @FXML
    private TooltipTableColumn<VType> liveReadbackColumn;
    @SuppressWarnings("unused")
    @FXML
    private TableColumn<TableEntry, ?> readbackColumn;

    private final SimpleBooleanProperty selectionInverted = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty showReadbacks = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty showDeltaPercentage = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty hideEqualItems = new SimpleBooleanProperty(false);

    private final SimpleBooleanProperty compareViewEnabled = new SimpleBooleanProperty(false);

    private SnapshotUtil snapshotUtil;

    @FXML
    public void initialize() {

        super.initialize();

        selectedColumn.setCellFactory(column -> new SelectionCell());

        CheckBox selectAllCheckBox = new CheckBox();
        selectAllCheckBox.selectedProperty().set(true);
        selectAllCheckBox.setTooltip(new Tooltip(Messages.includeThisPV));
        selectAllCheckBox.selectedProperty().addListener((ob, o, n) ->
                snapshotTableView.getItems().stream().filter(te -> te.readOnlyProperty().not().get())
                        .forEach(te -> te.selectedProperty().set(n)));
        selectedColumn.setGraphic(selectAllCheckBox);

        selectionInverted.addListener((ob, o, n) -> snapshotTableView.getItems().stream().filter(te -> te.readOnlyProperty().not().get())
                .forEach(te -> te.selectedProperty().set(te.selectedProperty().not().get())));

        MenuItem inverseMI = new MenuItem(Messages.inverseSelection);
        inverseMI.setOnAction(e -> selectionInverted.set(selectionInverted.not().get()));
        final ContextMenu contextMenu = new ContextMenu(inverseMI);
        selectAllCheckBox.setContextMenu(contextMenu);

        timeColumn.setCellFactory(c -> new SnapshotTableViewController.TimestampTableCell());

        deltaColumn.setCellValueFactory(e -> e.getValue().valueProperty());
        deltaColumn.setCellFactory(e -> new VDeltaCellEditor<>());
        deltaColumn.setComparator((pair1, pair2) -> {
            Utilities.VTypeComparison vtc1 = Utilities.valueToCompareString(pair1.value, pair1.base, pair1.threshold);
            Utilities.VTypeComparison vtc2 = Utilities.valueToCompareString(pair2.value, pair2.base, pair2.threshold);
            return Double.compare(vtc1.getAbsoluteDelta(), vtc2.getAbsoluteDelta());
        });

        deltaReadbackColumn.setCellValueFactory(e -> e.getValue().liveReadbackProperty());
        deltaReadbackColumn.setCellFactory(e -> new VDeltaCellEditor<>());
        deltaReadbackColumn.setComparator((pair1, pair2) -> {
            Utilities.VTypeComparison vtc1 = Utilities.valueToCompareString(pair1.value, pair1.base, pair1.threshold);
            Utilities.VTypeComparison vtc2 = Utilities.valueToCompareString(pair2.value, pair2.base, pair2.threshold);
            return Double.compare(vtc1.getAbsoluteDelta(), vtc2.getAbsoluteDelta());
        });

        showDeltaPercentage.addListener((ob, o, n) -> deltaColumn.setCellFactory(e -> {
            VDeltaCellEditor<VTypePair> vDeltaCellEditor = new VDeltaCellEditor<>();
            vDeltaCellEditor.setShowDeltaPercentage(n);
            return vDeltaCellEditor;
        }));


        hideEqualItems.addListener((ob, o, n) -> {

        });

        liveReadbackColumn.setCellFactory(e -> new VTypeCellEditor<>());
        storedReadbackColumn.setCellFactory(e -> new VTypeCellEditor<>());
        readbackColumn.visibleProperty().bind(showReadbacks);

        timeColumn.visibleProperty().bind(compareViewEnabled.not());
        firstDividerColumn.visibleProperty().bind(compareViewEnabled);
        statusColumn.visibleProperty().bind(compareViewEnabled.not());
        severityColumn.visibleProperty().bind(compareViewEnabled.not());
        valueColumn.visibleProperty().bind(compareViewEnabled.not());

        compareViewEnabled.addListener((ob, o, n) -> snapshotTableView.layout());

        snapshotUtil = new SnapshotUtil();
    }

    public void setSnapshotController(SnapshotController snapshotController) {
        this.snapshotController = snapshotController;
    }


    /**
     * {@link CheckBoxTableCell} handling the selection checkboxes for each row in the table.
     * Some logic is needed in case a PV is defined as read-only in the configuration.
     * For such PVs the checkbox is not rendered. It would be confusing since a read-only
     * PV must not be part of a restore operation.
     */
    private static class SelectionCell extends CheckBoxTableCell<TableEntry, Boolean> {

        @Override
        public void updateItem(final Boolean item, final boolean empty) {
            super.updateItem(item, empty);
            TableRow<TableEntry> tableRow = getTableRow();
            if (tableRow != null && tableRow.getItem() != null && tableRow.getItem().readOnlyProperty().get()) {
                setGraphic(null);
            }
        }
    }

    public void takeSnapshot(SnapshotMode snapshotMode, Consumer<Optional<Snapshot>> consumer) {
        switch (snapshotMode) {
            case READ_PVS -> takeSnapshot(consumer);
            case FROM_ARCHIVER -> takeSnapshotFromArchiver(consumer);
            default -> throw new IllegalArgumentException("Snapshot mode " + snapshotMode + " not supported");
        }
    }

    private void takeSnapshotFromArchiver(Consumer<Optional<Snapshot>> consumer) {
        DateTimePane dateTimePane = new DateTimePane();
        Dialog<Instant> timePickerDialog = new Dialog<>();
        timePickerDialog.setTitle(Messages.dateTimePickerTitle);
        timePickerDialog.getDialogPane().setContent(dateTimePane);
        timePickerDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        timePickerDialog.setResultConverter(b -> {
            if (b.equals(ButtonType.OK)) {
                return dateTimePane.getInstant();
            }
            return null;
        });
        Optional<Instant> time = timePickerDialog.showAndWait();
        if (time.isEmpty()) { // User cancels date/time picker dialog
            consumer.accept(Optional.empty());
            return;
        }
        JobManager.schedule("Add snapshot from archiver", monitor -> {
            List<SnapshotItem> snapshotItems;
            try {
                snapshotItems = SaveAndRestoreService.getInstance().takeSnapshotFromArchiver(snapshotController.configurationNode.getUniqueId(), time.get());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to query archiver for data", e);
                return;
            }
            Snapshot snapshot = new Snapshot();
            snapshot.setSnapshotNode(Node.builder().nodeType(NodeType.SNAPSHOT).name(Messages.archiver).created(new Date(time.get().toEpochMilli())).build());
            SnapshotData snapshotData = new SnapshotData();
            snapshotData.setUniqueId("anonymous");
            snapshotData.setSnapshotItems(snapshotItems);
            snapshot.setSnapshotData(snapshotData);
            consumer.accept(Optional.of(snapshot));
        });
    }

    private void takeSnapshot(Consumer<Optional<Snapshot>> consumer) {
        JobManager.schedule("Take snapshot", monitor -> {
            // Clear snapshots array
            snapshots.clear();
            List<SnapshotItem> snapshotItems;
            try {
                snapshotItems = SaveAndRestoreService.getInstance().takeSnapshot(snapshotController.getConfigurationNode().getUniqueId());
            } catch (Exception e) {
                ExceptionDetailsErrorDialog.openError(snapshotTableView, Messages.errorGeneric, Messages.takeSnapshotFailed, e);
                consumer.accept(Optional.empty());
                return;
            }
            // Service can only return nulls for disconnected PVs, but UI expects VDisonnectedData
            snapshotItems.forEach(si -> {
                if (si.getValue() == null) {
                    si.setValue(VDisconnectedData.INSTANCE);
                }
                if (si.getReadbackValue() == null) {
                    si.setReadbackValue(VDisconnectedData.INSTANCE);
                }
            });
            Snapshot snapshot = new Snapshot();
            snapshot.setSnapshotNode(Node.builder().nodeType(NodeType.SNAPSHOT).build());
            SnapshotData snapshotData = new SnapshotData();
            snapshotData.setSnapshotItems(snapshotItems);
            snapshot.setSnapshotData(snapshotData);
            showSnapshotInTable(snapshot);
            if (!Preferences.default_snapshot_name_date_format.isEmpty()) {
                String dateFormat = Preferences.default_snapshot_name_date_format;
                try {
                    //The format could be not correct
                    SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
                    snapshot.getSnapshotNode().setName(formatter.format(new Date()));
                }
                catch (Exception e) {
                    LOGGER.log(Level.WARNING, dateFormat + " is not a valid date format please check 'default_snapshot_name_date_format' preference ", e);
                }
            }
            consumer.accept(Optional.of(snapshot));
        });

    }

    public void updateThreshold(Snapshot snapshot, double threshold) {
        snapshot.getSnapshotData().getSnapshotItems().forEach(item -> {
            VType vtype = item.getValue();
            VNumber diffVType;

            double ratio = threshold / 100;

            TableEntry tableEntry = tableEntryItems.get(getPVKey(item.getConfigPv().getPvName(), item.getConfigPv().isReadOnly()));
            if (tableEntry == null) {
                tableEntry = tableEntryItems.get(getPVKey(item.getConfigPv().getPvName(), !item.getConfigPv().isReadOnly()));
            }

            if (!item.getConfigPv().equals(tableEntry.getConfigPv())) {
                return;
            }

            if (vtype instanceof VNumber) {
                diffVType = SafeMultiply.multiply((VNumber) vtype, ratio);
                VNumber vNumber = diffVType;
                boolean isNegative = vNumber.getValue().doubleValue() < 0;

                tableEntry.setThreshold(Optional.of(new Threshold<>(isNegative ? SafeMultiply.multiply(vNumber.getValue(), -1.0) : vNumber.getValue())));
            }
        });
    }

    /**
     * Updates snapshot set-point values using user-defined multiplier.
     *
     * @param snapshot   Current snapshot loaded into table view.
     * @param multiplier The (double) factor used to change the snapshot set-points used in restore operation.
     */
    public void updateSnapshotValues(Snapshot snapshot, double multiplier) {
        snapshot.getSnapshotData().getSnapshotItems()
                .forEach(item -> {
                    TableEntry tableEntry = tableEntryItems.get(getPVKey(item.getConfigPv().getPvName(), item.getConfigPv().isReadOnly()));
                    VType vtype = tableEntry.storedSnapshotValue().get();
                    VType newVType;

                    if (vtype instanceof VNumber) {
                        newVType = SafeMultiply.multiply((VNumber) vtype, multiplier);
                    } else if (vtype instanceof VNumberArray) {
                        newVType = SafeMultiply.multiply((VNumberArray) vtype, multiplier);
                    } else {
                        return;
                    }

                    item.setValue(newVType);

                    tableEntry.snapshotValProperty().set(newVType);

                    ObjectProperty<VTypePair> value = tableEntry.valueProperty();
                    value.setValue(new VTypePair(value.get().base, newVType, value.get().threshold));
                });
    }

    public void applyFilter(String filterText, boolean preserveSelection, List<List<Pattern>> regexPatterns) {
        if (filterText.isEmpty()) {
            List<TableEntry> arrayList = tableEntryItems.values().stream()
                    .peek(item -> {
                        if (!preserveSelection) {
                            if (!item.readOnlyProperty().get()) {
                                item.selectedProperty().set(true);
                            }
                        }
                    }).collect(Collectors.toList());

            Platform.runLater(() -> updateTable(arrayList));
            return;
        }

        List<TableEntry> filteredEntries = tableEntryItems.values().stream()
                .filter(item -> {
                    boolean matchEither = false;
                    for (List<Pattern> andPatternList : regexPatterns) {
                        boolean matchAnd = true;
                        for (Pattern pattern : andPatternList) {
                            matchAnd &= pattern.matcher(item.pvNameProperty().get()).find();
                        }

                        matchEither |= matchAnd;
                    }

                    if (!preserveSelection) {
                        item.selectedProperty().setValue(matchEither);
                    } else {
                        matchEither |= item.selectedProperty().get();
                    }

                    return matchEither;
                }).collect(Collectors.toList());

        Platform.runLater(() -> updateTable(filteredEntries));
    }

    public void applyPreserveSelection(boolean preserve) {
        if (preserve) {
            boolean allSelected = tableEntryItems.values().stream().allMatch(item -> item.selectedProperty().get());
            if (allSelected) {
                tableEntryItems.values()
                        .forEach(item -> item.selectedProperty().set(false));
            }
        }
    }

    public void showReadback(boolean showLiveReadback) {
        this.showReadbacks.set(showLiveReadback);
    }

    public void hideEqualItems() {
        ArrayList<TableEntry> arrayList = new ArrayList<>(tableEntryItems.values());
        Platform.runLater(() -> updateTable(arrayList));
    }

    /**
     * Restores a snapshot from client or service.
     *
     * @param restoreMode Specifies whether to restore from client or from service
     * @param snapshot    {@link Snapshot} content
     * @param completion  Callback to handle a potentially empty list of {@link RestoreResult}s.
     */
    public void restore(RestoreMode restoreMode, Snapshot snapshot, Consumer<List<RestoreResult>> completion) {
        JobManager.schedule("Restore snapshot " + snapshot.getSnapshotNode().getName(), monitor -> {
            List<RestoreResult> restoreResultList = null;
            try {
                switch (restoreMode) {
                    case CLIENT_RESTORE ->
                            restoreResultList = snapshotUtil.restore(getSnapshotItemsToRestore(snapshot));
                    case SERVICE_RESTORE ->
                            restoreResultList = SaveAndRestoreService.getInstance().restore(getSnapshotItemsToRestore(snapshot));
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(Messages.errorActionFailed);
                    alert.setContentText(e.getMessage());
                    alert.setHeaderText(Messages.restoreFailed);
                    DialogHelper.positionDialog(alert, snapshotTableView, -150, -150);
                    alert.showAndWait();
                });
                return;
            }
            completion.accept(restoreResultList);
        });
    }

    /**
     * Compiles a list of {@link SnapshotItem}s based on the snapshot's PVs (and potential read-only property setting)
     * as well as user's choice to exclude items in the UI.
     *
     * @param snapshot {@link Snapshot} contents.
     * @return A list of {@link SnapshotItem}s to be subject to a restore operation.
     */
    private List<SnapshotItem> getSnapshotItemsToRestore(Snapshot snapshot) {
        List<SnapshotItem> itemsToRestore = new ArrayList<>();

        for (SnapshotItem entry : snapshot.getSnapshotData().getSnapshotItems()) {
            TableEntry e = tableEntryItems.get(getPVKey(entry.getConfigPv().getPvName(), entry.getConfigPv().isReadOnly()));

            boolean restorable = e.selectedProperty().get() &&
                    !e.readOnlyProperty().get() &&
                    entry.getValue() != null &&
                    !entry.getValue().equals(VNoData.INSTANCE);

            if (restorable) {
                itemsToRestore.add(entry);
            }
        }
        return itemsToRestore;
    }


    public void setShowDeltaPercentage(boolean showDeltaPercentage) {
        this.showDeltaPercentage.set(showDeltaPercentage);
    }

    public void addSnapshot(Snapshot snapshot) {
        snapshots.add(snapshot);

        snapshotTableView.getColumns().clear();

        List<TableColumn<TableEntry, ?>> columns = new ArrayList<>();
        columns.add(selectedColumn);
        columns.add(idColumn);
        columns.add(pvNameColumn);
        columns.add(new DividerTableColumn());

        int minWidth = 130;

        if (compareViewEnabled.not().get()) {
            compareViewEnabled.set(true);
            compareColumn = new TableColumn<>(Messages.storedValues);
            compareColumn.getStyleClass().add("snapshot-table-centered");

            String baseSnapshotTimeStamp = snapshots.get(0).getSnapshotNode().getCreated() == null ?
                    "" :
                    " (" + TimestampFormats.SECONDS_FORMAT.format(snapshots.get(0).getSnapshotNode().getCreated().toInstant()) + ")";
            String snapshotName = snapshots.get(0).getSnapshotNode().getName() + baseSnapshotTimeStamp;

            baseSnapshotColumn = new TableColumn<>(snapshotName);
            baseSnapshotColumn.getStyleClass().add("snapshot-table-centered");

            baseSnapshotValueColumn = new TooltipTableColumn<>(Messages.baseSetpoint, Messages.toolTipTableColumnSetpointPVValue, minWidth);
            baseSnapshotValueColumn.setCellValueFactory(new PropertyValueFactory<>("snapshotVal"));
            baseSnapshotValueColumn.setCellFactory(e -> new VTypeCellEditor<>());
            baseSnapshotValueColumn.getStyleClass().add("snapshot-table-left-aligned");

            baseSnapshotValueColumn.setOnEditCommit(e -> {
                VType updatedValue = e.getRowValue().readOnlyProperty().get() ? e.getOldValue() : e.getNewValue();
                ObjectProperty<VTypePair> value = e.getRowValue().valueProperty();
                value.setValue(new VTypePair(value.get().base, updatedValue, value.get().threshold));
                snapshotController.updateLoadedSnapshot(e.getRowValue(), updatedValue);
                for (int i = 1; i < snapshots.size(); i++) {
                    ObjectProperty<VTypePair> compareValue = e.getRowValue().compareValueProperty(i);
                    compareValue.setValue(new VTypePair(updatedValue, compareValue.get().value, compareValue.get().threshold));
                }
            });

            baseSnapshotDeltaColumn = new TooltipTableColumn<>(Messages.tableColumnDeltaValue, "", minWidth);
            baseSnapshotDeltaColumn.setCellValueFactory(e -> e.getValue().valueProperty());
            baseSnapshotDeltaColumn.setCellFactory(e -> new VDeltaCellEditor<>());
            baseSnapshotDeltaColumn.getStyleClass().add("snapshot-table-left-aligned");

            baseSnapshotColumn.getColumns().addAll(baseSnapshotValueColumn, baseSnapshotDeltaColumn, new DividerTableColumn());
        } else {
            compareColumn.getColumns().clear();
        }

        compareColumn.getColumns().add(0, baseSnapshotColumn);

        for (int s = 1; s < snapshots.size(); s++) {
            Node snapshotNode = snapshots.get(s).getSnapshotNode();
            String snapshotName = snapshotNode.getName();

            List<SnapshotItem> entries = snapshot.getSnapshotData().getSnapshotItems();
            String nodeName;
            TableEntry tableEntry;
            // Base snapshot data
            List<TableEntry> baseSnapshotTableEntries = new ArrayList<>(tableEntryItems.values());
            SnapshotItem entry;
            for (int i = 0; i < entries.size(); i++) {
                entry = entries.get(i);
                nodeName = entry.getConfigPv().getPvName();
                String key = getPVKey(nodeName, entry.getConfigPv().isReadOnly());
                tableEntry = tableEntryItems.get(key);
                // tableEntry is null if the added snapshot has more items than the base snapshot.
                if (tableEntry == null) {
                    tableEntry = new TableEntry();
                    tableEntry.idProperty().setValue(tableEntryItems.size() + i + 1);
                    tableEntry.pvNameProperty().setValue(nodeName);
                    tableEntry.setConfigPv(entry.getConfigPv());
                    tableEntryItems.put(key, tableEntry);
                    tableEntry.readbackNameProperty().set(entry.getConfigPv().getReadbackPvName());
                }
                tableEntry.setSnapshotValue(entry.getValue(), snapshots.size());
                tableEntry.setStoredReadbackValue(entry.getReadbackValue(), snapshots.size());
                tableEntry.readOnlyProperty().set(entry.getConfigPv().isReadOnly());
                baseSnapshotTableEntries.remove(tableEntry);
            }
            // If added snapshot has more items than base snapshot, the base snapshot's values for those
            // table rows need to be set to DISCONNECTED.
            for (TableEntry te : baseSnapshotTableEntries) {
                te.setSnapshotValue(VDisconnectedData.INSTANCE, snapshots.size());
            }

            TableColumn<TableEntry, ?> headerColumn = new TableColumn<>(snapshotName + " (" +
                    TimestampFormats.SECONDS_FORMAT.format(snapshotNode.getCreated().toInstant()) + ")");
            headerColumn.getStyleClass().add("snapshot-table-centered");

            TooltipTableColumn<VTypePair> setpointValueCol = new TooltipTableColumn<>(
                    Messages.setpoint,
                    Messages.toolTipTableColumnSetpointPVValue, minWidth);

            setpointValueCol.setCellValueFactory(e -> e.getValue().compareValueProperty(snapshots.size()));
            setpointValueCol.setCellFactory(e -> new VTypeCellEditor<>());
            setpointValueCol.setEditable(false);
            setpointValueCol.setSortable(false);
            setpointValueCol.getStyleClass().add("snapshot-table-left-aligned");

            TooltipTableColumn<VTypePair> deltaCol = new TooltipTableColumn<>(
                    Utilities.DELTA_CHAR + " " + Messages.baseSetpoint,
                    "", minWidth);
            deltaCol.setCellValueFactory(e -> e.getValue().compareValueProperty(snapshots.size()));
            deltaCol.setCellFactory(e -> {
                VDeltaCellEditor vDeltaCellEditor = new VDeltaCellEditor<>();
                vDeltaCellEditor.setShowDeltaPercentage(showDeltaPercentage.get());
                return vDeltaCellEditor;
            });
            deltaCol.setEditable(false);
            deltaCol.setSortable(false);
            deltaCol.getStyleClass().add("snapshot-table-left-aligned");

            headerColumn.getColumns().addAll(setpointValueCol, deltaCol, new DividerTableColumn());

            compareColumn.getColumns().add(s, headerColumn);
        }

        columns.add(compareColumn);
        columns.add(liveValueColumn);
        columns.add(readbackColumn);

        snapshotTableView.getColumns().addAll(columns);

        connectPVs();
        updateTable(null);
    }
}
