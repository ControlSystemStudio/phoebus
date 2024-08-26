/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
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
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.SnapshotMode;
import org.phoebus.applications.saveandrestore.ui.Threshold;
import org.phoebus.applications.saveandrestore.ui.Utilities;
import org.phoebus.applications.saveandrestore.ui.VNoData;
import org.phoebus.applications.saveandrestore.ui.VTypePair;
import org.phoebus.core.vtypes.VDisconnectedData;
import org.phoebus.core.vtypes.VTypeHelper;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.time.DateTimePane;
import org.phoebus.util.time.TimestampFormats;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class SnapshotTableViewController extends BaseSnapshotTableViewController {

    @FXML
    private TooltipTableColumn<String> pvNameColumn;
    @FXML
    private TooltipTableColumn<Instant> timeColumn;
    @FXML
    private TooltipTableColumn<VType> storedReadbackColumn;
    @FXML
    private TooltipTableColumn<VType> liveReadbackColumn;
    @FXML
    private TableColumn<TableEntry, ?> readbackColumn;

    private final SimpleBooleanProperty selectionInverted = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty showReadbacks = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty showDeltaPercentage = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty hideEqualItems = new SimpleBooleanProperty(false);

    private final SimpleBooleanProperty compareViewEnabled = new SimpleBooleanProperty(false);


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

    public void takeSnapshot(SnapshotMode snapshotMode, Consumer<Snapshot> consumer){
        switch (snapshotMode){
            case READ_PVS -> takeSnapshot(consumer);
            case FROM_ARCHIVER -> takeSnapshotFromArchiver(consumer);
            default -> throw new IllegalArgumentException("Snapshot mode " + snapshotMode + " not supported");
        }
    }

    private void takeSnapshotFromArchiver(Consumer<Snapshot> consumer){
        DateTimePane dateTimePane = new DateTimePane();
        Dialog<Instant> timePickerDialog = new Dialog<>();
        timePickerDialog.setTitle(Messages.dateTimePickerTitle);
        timePickerDialog.getDialogPane().setContent(dateTimePane);
        timePickerDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        timePickerDialog.setResultConverter(b -> {
            if(b.equals(ButtonType.OK)){
                return dateTimePane.getInstant();
            }
            return null;
        });
        Instant time = timePickerDialog.showAndWait().get();
        if(time == null){ // User cancels date/time picker dialog
            return;
        }
        JobManager.schedule("Add snapshot from archiver", monitor -> {
            List<SnapshotItem> snapshotItems;
            try {
                snapshotItems = SaveAndRestoreService.getInstance().takeSnapshotFromArchiver(snapshotController.configurationNode.getUniqueId(), time);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to query archiver for data", e);
                return;
            }
            Snapshot snapshot = new Snapshot();
            snapshot.setSnapshotNode(Node.builder().nodeType(NodeType.SNAPSHOT).name(Messages.archiver).created(new Date(time.toEpochMilli())).build());
            SnapshotData snapshotData = new SnapshotData();
            snapshotData.setUniqueId("anonymous");
            snapshotData.setSnapshotItems(snapshotItems);
            snapshot.setSnapshotData(snapshotData);
            consumer.accept(snapshot);
        });
    }

    private void takeSnapshot(Consumer<Snapshot> consumer) {
        JobManager.schedule("Take snapshot", monitor -> {
            // Clear snapshots array
            snapshots.clear();
            List<SnapshotItem> snapshotItems;
            try {
                snapshotItems = SaveAndRestoreService.getInstance().takeSnapshot(snapshotController.getConfigurationNode().getUniqueId());
            } catch (Exception e) {
                ExceptionDetailsErrorDialog.openError(snapshotTableView, Messages.errorGeneric, Messages.takeSnapshotFailed, e);
                consumer.accept(null);
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
                SimpleDateFormat formatter = new SimpleDateFormat(Preferences.default_snapshot_name_date_format);
                snapshot.getSnapshotNode().setName(formatter.format(new Date()));
            }
            consumer.accept(snapshot);
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
     * Restores a snapshot through a call to the remote service.
     *
     * @param snapshot   The {@link Snapshot} object subject to restore
     * @param completion A handler for the outcome of the restore operation
     */
    public void restore(Snapshot snapshot, Consumer<List<RestoreResult>> completion) {
        JobManager.schedule("Restore snapshot " + snapshot.getSnapshotNode().getName(), monitor -> {
            List<SnapshotItem> itemsToRestore = new ArrayList<>();
            List<RestoreResult> restoreResultList = null;
            // Before invoking restore, compile a list of items where non-restorable PVs are excluded.
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

            try {
                restoreResultList = SaveAndRestoreService.getInstance().restore(itemsToRestore);
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(Messages.errorActionFailed);
                    alert.setContentText(e.getMessage());
                    alert.setHeaderText(Messages.restoreFailed);
                    DialogHelper.positionDialog(alert, snapshotTableView, -150, -150);
                    alert.showAndWait();
                });
            }
            completion.accept(restoreResultList);
        });
    }

    /**
     * Restores a snapshot from client.
     *
     * @param snapshot   The {@link Snapshot} object subject to restore
     * @param completion A handler for the outcome of the restore operation
     */
    @SuppressWarnings("unused")
    public void restoreFromClient(Snapshot snapshot, Consumer<List<String>> completion) {
        new Thread(() -> {
            List<String> restoreFailedPVNames = new ArrayList<>();
            CountDownLatch countDownLatch = new CountDownLatch(snapshot.getSnapshotData().getSnapshotItems().size());
            snapshot.getSnapshotData().getSnapshotItems()
                    .forEach(e -> pvs.get(getPVKey(e.getConfigPv().getPvName(), e.getConfigPv().isReadOnly())).setCountDownLatch(countDownLatch));

            for (SnapshotItem entry : snapshot.getSnapshotData().getSnapshotItems()) {
                TableEntry e = tableEntryItems.get(getPVKey(entry.getConfigPv().getPvName(), entry.getConfigPv().isReadOnly()));

                boolean restorable = e.selectedProperty().get() &&
                        !e.readOnlyProperty().get() &&
                        entry.getValue() != null &&
                        !entry.getValue().equals(VNoData.INSTANCE);

                if (restorable) {
                    final SaveAndRestorePV pv = pvs.get(getPVKey(e.pvNameProperty().get(), e.readOnlyProperty().get()));
                    if (entry.getValue() != null) {
                        try {
                            pv.getPv().write(VTypeHelper.toObject(entry.getValue()));
                        } catch (Exception writeException) {
                            restoreFailedPVNames.add(entry.getConfigPv().getPvName());
                        } finally {
                            pv.countDown();
                        }
                    }
                } else {
                    countDownLatch.countDown();
                }
            }

            try {
                countDownLatch.await(10, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                LOGGER.log(Level.INFO, "Encountered InterruptedException", e);
            }

            if (restoreFailedPVNames.isEmpty()) {
                LOGGER.log(Level.FINE, "Restored snapshot {0}", snapshot.getSnapshotNode().getName());
            } else {
                Collections.sort(restoreFailedPVNames);
                StringBuilder sb = new StringBuilder(restoreFailedPVNames.size() * 200);
                restoreFailedPVNames.forEach(e -> sb.append(e).append('\n'));
                LOGGER.log(Level.WARNING,
                        "Not all PVs could be restored for {0}: {1}. The following errors occurred:\n{2}",
                        new Object[]{snapshot.getSnapshotNode().getName(), snapshot.getSnapshotNode(), sb.toString()});
            }
            completion.accept(restoreFailedPVNames);
        }).start();
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
