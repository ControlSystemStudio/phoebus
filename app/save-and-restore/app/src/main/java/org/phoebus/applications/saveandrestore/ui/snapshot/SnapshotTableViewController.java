/*
 * Copyright (C) 2020 European Spallation Source ERIC.
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
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.Preferences;
import org.phoebus.applications.saveandrestore.SafeMultiply;
import org.phoebus.applications.saveandrestore.common.*;
import org.phoebus.applications.saveandrestore.model.*;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.util.time.TimestampFormats;

import java.security.AccessController;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

        selectionInverted.addListener((ob, o, n) -> {
            snapshotTableView.getItems().stream().filter(te -> te.readOnlyProperty().not().get())
                    .forEach(te -> {
                        te.selectedProperty().set(te.selectedProperty().not().get());
                    });
        });

        MenuItem inverseMI = new MenuItem(Messages.inverseSelection);
        inverseMI.setOnAction(e -> {
            selectionInverted.set(selectionInverted.not().get());
        });
        final ContextMenu contextMenu = new ContextMenu(inverseMI);
        selectAllCheckBox.setContextMenu(contextMenu);

        timeColumn.setCellFactory(c -> new SnapshotTableViewController.TimestampTableCell());

        deltaColumn.setCellValueFactory(e -> e.getValue().valueProperty());
        deltaColumn.setCellFactory(e -> new VDeltaCellEditor<>());

        showDeltaPercentage.addListener((ob, o, n) -> deltaColumn.setCellFactory(e -> {
            VDeltaCellEditor<VTypePair> vDeltaCellEditor = new VDeltaCellEditor<>();
            vDeltaCellEditor.setShowDeltaPercentage(n);
            return vDeltaCellEditor;
        }));

        deltaColumn.setComparator((pair1, pair2) -> {
            Utilities.VTypeComparison vtc1 = Utilities.valueToCompareString(pair1.value, pair1.base, pair1.threshold);
            Utilities.VTypeComparison vtc2 = Utilities.valueToCompareString(pair2.value, pair2.base, pair2.threshold);
            return Double.compare(vtc1.getAbsoluteDelta(), vtc2.getAbsoluteDelta());
        });

        hideEqualItems.addListener((ob, o, n) -> {

        });

        liveReadbackColumn.setCellFactory(e -> new VTypeCellEditor<>());
        storedReadbackColumn.setCellFactory(e -> new VTypeCellEditor<>());
        readbackColumn.visibleProperty().bind(showReadbacks);

        timeColumn.visibleProperty().bind(compareViewEnabled.not());
        firstDividerColumn.visibleProperty().bind(compareViewEnabled);
        //compareColumn.visibleProperty().bind(compareViewEnabled);
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
    private class SelectionCell extends CheckBoxTableCell<TableEntry, Boolean> {

        @Override
        public void updateItem(final Boolean item, final boolean empty) {
            super.updateItem(item, empty);
            TableRow<TableEntry> tableRow = getTableRow();
            if (tableRow != null && tableRow.getItem() != null && tableRow.getItem().readOnlyProperty().get()) {
                setGraphic(null);
            }
        }
    }

    public void takeSnapshot(Consumer<Snapshot> consumer) {
        List<SnapshotItem> entries = new ArrayList<>();
        readAll(list ->
                Platform.runLater(() -> {
                    tableEntryItems.clear();
                    entries.addAll(list);
                    Snapshot snapshot = new Snapshot();
                    snapshot.setSnapshotNode(Node.builder().nodeType(NodeType.SNAPSHOT).build());
                    SnapshotData snapshotData = new SnapshotData();
                    snapshotData.setSnapshotItems(entries);
                    snapshot.setSnapshotData(snapshotData);
                    //List<TableEntry> tableEntries = createTableEntries(snapshot);
                    //updateTable(tableEntries);
                    showSnapshotInTable(snapshot);
                    if (!Preferences.default_snapshot_name_date_format.equals("")) {
                        SimpleDateFormat formatter = new SimpleDateFormat(Preferences.default_snapshot_name_date_format);
                        snapshot.getSnapshotNode().setName(formatter.format(new Date()));
                    }
                    consumer.accept(snapshot);
                })
        );
    }

    /**
     * Reads all PVs using a thread pool. All reads are asynchronous, waiting at most the amount of time
     * configured through a preference setting.
     *
     * @param completion Callback receiving a list of {@link SnapshotItem}s where values for PVs that could
     *                   not be read are set to {@link VDisconnectedData#INSTANCE}.
     */
    private void readAll(Consumer<List<SnapshotItem>> completion) {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        SnapshotItem[] snapshotEntries = new SnapshotItem[tableEntryItems.values().size()];
        JobManager.schedule("Take snapshot", monitor -> {
            final CountDownLatch countDownLatch = new CountDownLatch(tableEntryItems.values().size());
            for (TableEntry t : tableEntryItems.values()) {
                // Submit read request only if job has not been cancelled
                executorService.submit(() -> {
                    SaveAndRestorePV pv = pvs.get(getPVKey(t.pvNameProperty().get(), t.readOnlyProperty().get()));
                    VType value = VDisconnectedData.INSTANCE;
                    try {
                        value = pv.getPv().asyncRead().get(Preferences.readTimeout, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to read PV " + pv.getPvName(), e);
                    }
                    VType readBackValue = VDisconnectedData.INSTANCE;
                    if (pv.getReadbackPv() != null && !pv.getReadbackValue().equals(VDisconnectedData.INSTANCE)) {
                        try {
                            readBackValue = pv.getReadbackPv().asyncRead().get(Preferences.readTimeout, TimeUnit.MILLISECONDS);
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Failed to read read-back PV " + pv.getReadbackPvName(), e);
                        }
                    }
                    SnapshotItem snapshotItem = new SnapshotItem();
                    snapshotItem.setConfigPv(t.getConfigPv());
                    snapshotItem.setValue(value);
                    snapshotItem.setReadbackValue(readBackValue);

                    snapshotEntries[t.idProperty().get() - 1] = snapshotItem;
                    countDownLatch.countDown();
                });
            }
            countDownLatch.await();
            completion.accept(Arrays.asList(snapshotEntries));
            executorService.shutdown();
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
                    VType vtype = tableEntry.getStoredValue().get();
                    VType newVType;

                    if (vtype instanceof VNumber) {
                        newVType = SafeMultiply.multiply((VNumber) vtype, multiplier);
                    } else if (vtype instanceof VNumberArray) {
                        newVType = SafeMultiply.multiply((VNumberArray) vtype, multiplier);
                    } else {
                        return;
                    }

                    item.setValue(newVType);

                    tableEntry.storedValueProperty().set(newVType);

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

    public void hideEqualItems(boolean hide) {
        ArrayList<TableEntry> arrayList = new ArrayList<>(tableEntryItems.values());
        Platform.runLater(() -> updateTable(arrayList));
    }

    public void restore(Snapshot snapshot, Consumer<List<String>> completion) {
        new Thread(() -> {
            List<String> restoreFailedPVNames = new ArrayList<>();
            CountDownLatch countDownLatch = new CountDownLatch(snapshot.getSnapshotData().getSnapshotItems().size());
            snapshot.getSnapshotData().getSnapshotItems()
                    .forEach(e -> pvs.get(getPVKey(e.getConfigPv().getPvName(), e.getConfigPv().isReadOnly())).setCountDownLatch(countDownLatch));

            for (SnapshotItem entry : snapshot.getSnapshotData().getSnapshotItems()) {
                TableEntry e = tableEntryItems.get(getPVKey(entry.getConfigPv().getPvName(), entry.getConfigPv().isReadOnly()));

                boolean restorable = e.selectedProperty().get() && !e.readOnlyProperty().get() &&
                        !entry.getValue().equals(VNoData.INSTANCE);

                if (restorable) {
                    final SaveAndRestorePV pv = pvs.get(getPVKey(e.pvNameProperty().get(), e.readOnlyProperty().get()));
                    if (entry.getValue() != null) {
                        try {
                            pv.getPv().write(Utilities.toRawValue(entry.getValue()));
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
                countDownLatch.await();
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
        if (compareViewEnabled.not().get()) {
            //snapshotTableView.getColumns().clear();
            compareViewEnabled.set(true);
            // Initialize compare view
            Node baseSnapshotNode = snapshotController.getSnapshot().getSnapshotNode();
            /*
            baseSnapshotColumn.textProperty()
                    .set(snapshotNode.getName() +
                            " (" +
                            TimestampFormats.SECONDS_FORMAT.format(snapshotNode.getCreated().toInstant()) +
                            ")");

            baseSnapshotDeltaColumn.setCellValueFactory(e -> e.getValue().valueProperty());
            baseSnapshotDeltaColumn.setCellFactory(e -> new VDeltaCellEditor<>());

             */

            TableColumn<TableEntry, ?> storedValueColumn = new TableColumn<>(Messages.storedValues);
            storedValueColumn.setMinWidth(100);
            //storedValueColumn.setPrefWidth(300);
            //storedValueColumn.getStyleClass().add("toplevel");

            String baseSnapshotTimeStamp = snapshots.get(0).getSnapshotNode().getCreated() == null ?
                    "" :
                    " (" + TimestampFormats.SECONDS_FORMAT.format(snapshots.get(0).getSnapshotNode().getCreated().toInstant()) + ")";
            String snapshotName = snapshots.get(0).getSnapshotNode().getName() + baseSnapshotTimeStamp;

            TableColumn<TableEntry, ?> baseCol = new TooltipTableColumn<>(
                    snapshotName,
                    Messages.toolTipTableColumnSetpointPVValue, 33);
            baseCol.getStyleClass().add("second-level");

            TableColumn<TableEntry, VType> storedBaseSetpointValueColumn = new TooltipTableColumn<>(
                    Messages.baseSetpoint,
                    Messages.toolTipTableColumnBaseSetpointValue, 100);

            storedBaseSetpointValueColumn.setCellValueFactory(new PropertyValueFactory<>("storedValue"));
            storedBaseSetpointValueColumn.setCellFactory(e -> new VTypeCellEditor<>());
            storedBaseSetpointValueColumn.setEditable(true);
            storedBaseSetpointValueColumn.setOnEditCommit(e -> {
                VType updatedValue = e.getRowValue().readOnlyProperty().get() ? e.getOldValue() : e.getNewValue();

                ObjectProperty<VTypePair> value = e.getRowValue().valueProperty();
                value.setValue(new VTypePair(value.get().base, updatedValue, value.get().threshold));
                //controller.updateLoadedSnapshot(0, e.getRowValue(), updatedValue);

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
                vDeltaCellEditor.setShowDeltaPercentage(showDeltaPercentage.get());
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
            /*
            snapshotTableView.getColumns().add(0, selectedColumn);
            snapshotTableView.getColumns().add(1, idColumn);
            snapshotTableView.getColumns().add(2, new DividerTableColumn());
            snapshotTableView.getColumns().add(1, pvNameColumn);
            snapshotTableView.getColumns().addAll(3, Arrays.asList(storedValueColumn));
            snapshotTableView.getColumns().add(4, new DividerTableColumn());

             */
            snapshotTableView.getColumns().add(5, storedValueColumn);
        }


        Node snapshotNode = snapshot.getSnapshotNode();
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
                tableEntry.readbackPvNameProperty().set(entry.getConfigPv().getReadbackPvName());
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
        headerColumn.setPrefWidth(200);
        headerColumn.setResizable(true);

        TooltipTableColumn<VTypePair> setpointValueCol = new TooltipTableColumn<>(
                Messages.setpoint,
                "Setpoint PV value when the " + snapshotName + " snapshot was taken", 66);
        setpointValueCol.setCellValueFactory(e -> e.getValue().compareValueProperty(snapshots.size() - 1));
        setpointValueCol.setCellFactory(e -> new VTypeCellEditor<>());
        setpointValueCol.setEditable(false);
        setpointValueCol.setPreferredWidth(150);
        setpointValueCol.setMinWidth(100);

        TooltipTableColumn<VTypePair> deltaCol = new TooltipTableColumn<>(
                Utilities.DELTA_CHAR + Messages.baseSetpoint,
                "Setpoint PVV value when the " + snapshotName + " snapshot was taken", 50);
        deltaCol.setCellValueFactory(e -> e.getValue().compareValueProperty(snapshots.size() - 1));
        deltaCol.setCellFactory(e -> {
            VDeltaCellEditor vDeltaCellEditor = new VDeltaCellEditor<>();
            vDeltaCellEditor.setShowDeltaPercentage(showDeltaPercentage.get());
            return vDeltaCellEditor;
        });
        deltaCol.setPreferredWidth(150);
        deltaCol.setEditable(false);
        deltaCol.setMinWidth(100);

        headerColumn.getColumns().addAll(setpointValueCol, deltaCol, new DividerTableColumn());
        //compareColumn.getColumns().add(headerColumn);

        snapshots.add(snapshot);
        //Platform.runLater(() -> snapshotTableView.layout());


        connectPVs();
        updateTable(null);
    }
}
