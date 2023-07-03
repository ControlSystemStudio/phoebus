/**
 * Copyright (C) 2019 European Spallation Source ERIC.
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.phoebus.applications.saveandrestore.ui.snapshot;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.epics.vtype.*;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.Preferences;
import org.phoebus.applications.saveandrestore.SafeMultiply;
import org.phoebus.applications.saveandrestore.common.*;
import org.phoebus.applications.saveandrestore.model.*;
import org.phoebus.applications.saveandrestore.model.event.SaveAndRestoreEventReceiver;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.pv.PVPool;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This controller is for the use case of loading a configuration {@link Node} to take a new snapshot.
 * Once the snapshot has been saved, this controller calls the {@link SnapshotTab} API to load
 * the view associated with restore actions.
 */
public class SnapshotController {


    @FXML
    private BorderPane borderPane;


    protected final Map<String, PV> pvs = new HashMap<>();
    protected final Map<String, TableEntry> tableEntryItems = new LinkedHashMap<>();


    protected Node configurationNode;

    public static final Logger LOGGER = Logger.getLogger(SnapshotController.class.getName());

    /**
     * The time between updates of dynamic data in the table, in ms
     */
    public static final long TABLE_UPDATE_INTERVAL = 500;


    protected ServiceLoader<SaveAndRestoreEventReceiver> eventReceivers;

    @FXML
    protected VBox progressIndicator;

    protected SnapshotTab snapshotTab;

    public SnapshotController(SnapshotTab snapshotTab) {
        this.snapshotTab = snapshotTab;
    }

    /**
     * Used to disable portions of the UI when long-lasting operations are in progress, e.g.
     * take snapshot or save snapshot.
     */
    protected final SimpleBooleanProperty disabledUi = new SimpleBooleanProperty(false);

    /**
     * List of added snapshots. In the case of taking a new snapshot, or restoring a snapshot, this should contain but one element.
     */
    protected final List<Snapshot> snapshots = new ArrayList<>(10);

    @FXML
    protected SnapshotTableViewController snapshotTableViewController;

    @FXML
    protected SnapshotControlsViewController snapshotControlsViewController;

    private Node snapshotNode;

    @FXML
    public void initialize() {
        // Locate registered SaveAndRestoreEventReceivers
        eventReceivers = ServiceLoader.load(SaveAndRestoreEventReceiver.class);
        progressIndicator.visibleProperty().bind(disabledUi);
        disabledUi.addListener((observable, oldValue, newValue) -> borderPane.setDisable(newValue));
        snapshotControlsViewController.setSnapshotController(this);
        snapshotControlsViewController.setFilterToolbarDisabled(true);
        snapshotTableViewController.setSnapshotController(this);
    }

    /**
     * Loads data from a configuration {@link Node} in order to populate the
     * view with PV items and prepare it to take a snapshot.
     *
     * @param configurationNode A {@link Node} of type {@link NodeType#CONFIGURATION}
     */
    public void newSnapshot(Node configurationNode) {
        this.configurationNode = configurationNode;
        snapshotTab.updateTabTitle(Messages.unnamedSnapshot);
        JobManager.schedule("Get configuration", monitor -> {
            ConfigurationData configuration;
            try {
                configuration = SaveAndRestoreService.getInstance().getConfiguration(configurationNode.getUniqueId());
            } catch (Exception e) {
                ExceptionDetailsErrorDialog.openError(borderPane, Messages.errorGeneric, Messages.errorUnableToRetrieveData, e);
                LOGGER.log(Level.INFO, "Error loading configuration", e);
                return;
            }
            List<ConfigPv> configPvs = configuration.getPvList();
            Snapshot snapshot = new Snapshot();
            snapshot.setSnapshotNode(Node.builder().name(Messages.unnamedSnapshot).nodeType(NodeType.SNAPSHOT).build());
            SnapshotData snapshotData = new SnapshotData();
            snapshotData.setSnapshotItems(configurationToSnapshotItems(configPvs));
            snapshot.setSnapshotData(snapshotData);
            snapshots.add(0, snapshot);
            List<TableEntry> tableEntries = createTableEntries(snapshots.get(0));
            Platform.runLater(() -> snapshotTableViewController.updateTable(tableEntries, snapshots, false, false));
        });
    }

    @FXML
    @SuppressWarnings("unused")
    public void takeSnapshot() {
        snapshotControlsViewController.getSnapshotDataDirty().set(true);
        disabledUi.set(true);
        List<SnapshotItem> entries = new ArrayList<>();
        readAll(list ->
                Platform.runLater(() -> {
                    tableEntryItems.clear();
                    disabledUi.set(false);
                    entries.addAll(list);
                    Snapshot snapshot = new Snapshot();
                    snapshot.setSnapshotNode(Node.builder().name(Messages.unnamedSnapshot).nodeType(NodeType.SNAPSHOT).build());
                    SnapshotData snapshotData = new SnapshotData();
                    snapshotData.setSnapshotItems(entries);
                    snapshot.setSnapshotData(snapshotData);
                    snapshots.set(0, snapshot);
                    List<TableEntry> tableEntries = createTableEntries(snapshots.get(0));
                    snapshotTableViewController.updateTable(tableEntries, snapshots,
                            snapshotControlsViewController.showLiveReadbackProperty.get(),
                            snapshotControlsViewController.showDeltaPercentageProperty.get());

                    if (!Preferences.default_snapshot_name_date_format.equals("")) {
                        SimpleDateFormat formater = new SimpleDateFormat(Preferences.default_snapshot_name_date_format);
                        snapshotControlsViewController.getSnapshotNameProperty().set(formater.format(new Date()));
                    }
                })
        );
    }

    @FXML
    @SuppressWarnings("unused")
    public void saveSnapshot(ActionEvent actionEvent) {

        disabledUi.set(true);
        JobManager.schedule("Save Snapshot", monitor -> {
            List<SnapshotItem> snapshotItems = snapshots.get(0).getSnapshotData().getSnapshotItems();
            SnapshotData snapshotData = new SnapshotData();
            snapshotData.setSnapshotItems(snapshotItems);
            Snapshot snapshot = new Snapshot();
            snapshot.setSnapshotData(snapshotData);
            snapshot.setSnapshotNode(Node.builder().nodeType(NodeType.SNAPSHOT)
                    .name(snapshotControlsViewController.getSnapshotNameProperty().get())
                    .description(snapshotControlsViewController.getSnapshotCommentProperty().get()).build());
            try {
                snapshot = SaveAndRestoreService.getInstance().saveSnapshot(configurationNode, snapshot);
                Node _snapshotNode = snapshot.getSnapshotNode();
                javafx.scene.Node jfxNode = (javafx.scene.Node) actionEvent.getSource();
                String userData = (String) jfxNode.getUserData();
                if (userData.equalsIgnoreCase("true")) {
                    eventReceivers.forEach(r -> r.snapshotSaved(_snapshotNode, this::showLoggingError));
                }
                Platform.runLater(() -> {
                    // Load snapshot via the tab as that will also update the tab title and id.
                    snapshotTab.loadSnapshot(_snapshotNode);
                });
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to save snapshot", e);
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(Messages.errorActionFailed);
                    alert.setContentText(e.getMessage());
                    alert.setHeaderText(Messages.saveSnapshotErrorContent);
                    DialogHelper.positionDialog(alert, borderPane, -150, -150);
                    alert.showAndWait();
                });
            } finally {
                disabledUi.set(false);
            }
        });
    }

    protected List<TableEntry> createTableEntries(Snapshot snapshot) {
        AtomicInteger counter = new AtomicInteger(0);
        snapshot.getSnapshotData().getSnapshotItems().forEach(entry -> {
            TableEntry tableEntry = new TableEntry();
            String name = entry.getConfigPv().getPvName();
            tableEntry.idProperty().setValue(counter.incrementAndGet());
            tableEntry.pvNameProperty().setValue(name);
            tableEntry.setConfigPv(entry.getConfigPv());
            tableEntry.setSnapshotValue(entry.getValue(), 0);
            tableEntry.setStoredReadbackValue(entry.getReadbackValue());
            String key = getPVKey(name, entry.getConfigPv().isReadOnly());
            tableEntry.readbackPvNameProperty().set(entry.getConfigPv().getReadbackPvName());
            tableEntry.readOnlyProperty().set(entry.getConfigPv().isReadOnly());
            tableEntryItems.put(key, tableEntry);
        });
        // Table entries created, associate connected PVs with these entries
        connectPVs();
        return new ArrayList<>(tableEntryItems.values());
    }

    protected List<SnapshotItem> configurationToSnapshotItems(List<ConfigPv> configPvs) {
        List<SnapshotItem> snapshotEntries = new ArrayList<>();
        for (ConfigPv configPv : configPvs) {
            SnapshotItem snapshotItem = new SnapshotItem();
            snapshotItem.setConfigPv(configPv);
            snapshotItem.setValue(VNoData.INSTANCE);
            snapshotItem.setReadbackValue(VNoData.INSTANCE);
            snapshotEntries.add(snapshotItem);
        }
        return snapshotEntries;
    }


    /**
     * Returns the snapshot stored under the given index.
     *
     * @param index the index of the snapshot to return
     * @return the snapshot under the given index (0 for the base snapshot and 1 or more for the compared ones)
     */
    public Snapshot getSnapshot(int index) {
        return snapshots.get(index);
    }

    public void updateThreshold(double threshold) {
        snapshots.forEach(snapshot -> snapshot.getSnapshotData().getSnapshotItems().forEach(item -> {
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
        }));
    }

    /**
     * Returns the number of all snapshots currently visible in the viewer (including the base snapshot).
     *
     * @return the number of all snapshots
     */
    public int getNumberOfSnapshots() {
        return 1;
    }

    protected void connectPVs() {
        tableEntryItems.values().forEach(e -> {
            PV pv = pvs.get(getPVKey(e.getConfigPv().getPvName(), e.getConfigPv().isReadOnly()));
            if (pv == null) {
                pvs.put(getPVKey(e.getConfigPv().getPvName(), e.getConfigPv().isReadOnly()), new PV(e));
            } else {
                pv.setSnapshotTableEntry(e);
            }
        });
    }

    public void updateLoadedSnapshot(int snapshotIndex, TableEntry rowValue, VType newValue) {
        snapshots.get(0).getSnapshotData().getSnapshotItems().stream()
                .filter(item -> item.getConfigPv().equals(rowValue.getConfigPv()))
                .findFirst()
                .ifPresent(item -> {
                    VType vtype = item.getValue();
                    VType newVType = null;
                    if (newValue instanceof VNumber) {
                        newVType = VNumber.of(((VNumber) newValue).getValue(), Alarm.alarmOf(vtype), Time.timeOf(vtype), Display.displayOf(vtype));
                    } else if (newValue instanceof VNumberArray) {
                        newVType = VNumberArray.of(((VNumberArray) newValue).getData(), Alarm.alarmOf(vtype), Time.timeOf(vtype), Display.displayOf(vtype));
                    } else if (newValue instanceof VString) {
                        newVType = VString.of(((VString) newValue).getValue(), Alarm.alarmOf(vtype), Time.timeOf(vtype));
                    } else if (newValue instanceof VStringArray) {
                        newVType = VStringArray.of(((VStringArray) newValue).getData(), Alarm.alarmOf(vtype), Time.timeOf(vtype));
                    } else if (newValue instanceof VEnum) {
                        newVType = newValue;
                    }
                    item.setValue(newVType);
                    rowValue.storedValueProperty().set(newVType);
                });
    }

    protected static class PV {
        final String pvName;
        final String readbackPvName;
        CountDownLatch countDownLatch;
        org.phoebus.pv.PV pv;
        org.phoebus.pv.PV readbackPv;
        volatile VType pvValue = VDisconnectedData.INSTANCE;
        volatile VType readbackValue = VDisconnectedData.INSTANCE;
        TableEntry snapshotTableEntry;
        boolean readOnly;

        PV(TableEntry snapshotTableEntry) {
            this.snapshotTableEntry = snapshotTableEntry;
            this.pvName = patchPvName(snapshotTableEntry.pvNameProperty().get());
            this.readbackPvName = patchPvName(snapshotTableEntry.readbackPvNameProperty().get());
            this.readOnly = snapshotTableEntry.readOnlyProperty().get();

            try {
                pv = PVPool.getPV(pvName);
                pv.onValueEvent().throttleLatest(TABLE_UPDATE_INTERVAL, TimeUnit.MILLISECONDS).subscribe(value -> {
                    pvValue = org.phoebus.pv.PV.isDisconnected(value) ? VDisconnectedData.INSTANCE : value;
                    this.snapshotTableEntry.setLiveValue(pvValue);
                });

                if (readbackPvName != null && !readbackPvName.isEmpty()) {
                    readbackPv = PVPool.getPV(this.readbackPvName);
                    readbackPv.onValueEvent()
                            .throttleLatest(TABLE_UPDATE_INTERVAL, TimeUnit.MILLISECONDS)
                            .subscribe(value -> {
                                this.readbackValue = org.phoebus.pv.PV.isDisconnected(value) ? VDisconnectedData.INSTANCE : value;
                                this.snapshotTableEntry.setReadbackValue(this.readbackValue);
                            });
                } else {
                    // If configuration does not define readback PV, then UI should show "no data" rather than "disconnected"
                    this.snapshotTableEntry.setReadbackValue(VNoData.INSTANCE);
                }
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Error connecting to PV", e);
            }
        }

        private String patchPvName(String pvName) {
            if (pvName == null || pvName.isEmpty()) {
                return null;
            } else if (pvName.startsWith("ca://") || pvName.startsWith("pva://")) {
                return pvName.substring(pvName.lastIndexOf('/') + 1);
            } else {
                return pvName;
            }
        }

        public void setCountDownLatch(CountDownLatch countDownLatch) {
            LOGGER.info(countDownLatch + " New CountDownLatch set");
            this.countDownLatch = countDownLatch;
        }

        public void countDown() {
            this.countDownLatch.countDown();
        }

        public void setSnapshotTableEntry(TableEntry snapshotTableEntry) {
            this.snapshotTableEntry = snapshotTableEntry;
            this.snapshotTableEntry.setLiveValue(pv.read());
        }

        void dispose() {
            if (pv != null) {
                PVPool.releasePV(pv);
            }

            if (readbackPv != null) {
                PVPool.releasePV(readbackPv);
            }
        }
    }

    public boolean handleSnapshotTabClosed() {
        if (snapshotControlsViewController.getSnapshotDataDirty().get()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(Messages.closeTabPrompt);
            alert.setContentText(Messages.promptCloseSnapshotTabContent);
            DialogHelper.positionDialog(alert, borderPane, -150, -150);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get().equals(ButtonType.CANCEL)) {
                return false;
            }
        }
        dispose();
        return true;
    }

    /**
     * Releases PV resources.
     */
    private void dispose() {
        pvs.values().forEach(PV::dispose);
    }

    /**
     * Returns true if items with equal live and snapshot values are currently hidden or false is shown.
     *
     * @return true if equal items are hidden or false otherwise.
     */
    public boolean isHideEqualItems() {
        return snapshotControlsViewController.getHideEqualItemsProperty().get();
    }

    protected String getPVKey(String pvName, boolean isReadonly) {
        return pvName + "_" + isReadonly;
    }

    private void showLoggingError(String cause) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(Messages.loggingFailedTitle);
            alert.setHeaderText(Messages.loggingFailed);
            alert.setContentText(cause != null ? cause : Messages.loggingFailedCauseUnknown);
            DialogHelper.positionDialog(alert, borderPane, -150, -150);
            alert.showAndWait();
        });
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
                    PV pv = pvs.get(getPVKey(t.pvNameProperty().get(), t.readOnlyProperty().get()));
                    VType value = VNoData.INSTANCE;
                    try {
                        value = pv.pv.asyncRead().get(Preferences.readTimeout, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to read PV " + pv.pvName, e);
                    }
                    VType readBackValue = VNoData.INSTANCE;
                    if (pv.readbackPv != null && !pv.readbackValue.equals(VDisconnectedData.INSTANCE)) {
                        try {
                            readBackValue = pv.readbackPv.asyncRead().get(Preferences.readTimeout, TimeUnit.MILLISECONDS);
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Failed to read read-back PV " + pv.readbackPvName, e);
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

    public Node getConfigurationNode() {
        return configurationNode;
    }

    public void setSnapshotNameProperty(String name) {
        snapshotControlsViewController.getSnapshotNameProperty().set(name);
    }

    public void updateSnapshotValues(double multiplier) {
        snapshots.forEach(snapshot -> snapshot.getSnapshotData().getSnapshotItems()
                .forEach(item -> {
                    TableEntry tableEntry = tableEntryItems.get(getPVKey(item.getConfigPv().getPvName(), item.getConfigPv().isReadOnly()));
                    VType vtype = tableEntry.getStoredSnapshotValue().get();
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
                }));
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

            Platform.runLater(() -> snapshotTableViewController.updateTable(arrayList));
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

        Platform.runLater(() -> snapshotTableViewController.updateTable(filteredEntries));
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

    public void applyShowReadback(boolean showLiveReadback, boolean showDeltaPercentage) {
        Platform.runLater(() -> {
            ArrayList<TableEntry> arrayList = new ArrayList<>(tableEntryItems.values());
            snapshotTableViewController.updateTable(arrayList, snapshots, showLiveReadback, showDeltaPercentage);
        });
    }

    public void applyHideEqualItems(boolean hide) {
        ArrayList<TableEntry> arrayList = new ArrayList<>(tableEntryItems.values());
        Platform.runLater(() -> snapshotTableViewController.updateTable(arrayList));
    }

    private void loadCompositeSnapshotInternal(Consumer<Snapshot> completion) {

        JobManager.schedule("Load composite snapshot items", items -> {
            disabledUi.set(true);
            List<SnapshotItem> snapshotItems;
            try {
                snapshotItems = SaveAndRestoreService.getInstance().getCompositeSnapshotItems(snapshotNode.getUniqueId());
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Error loading composite snapshot for restore", e);
                //ExceptionDetailsErrorDialog.openError(snapshotTable, Messages.errorGeneric, Messages.errorUnableToRetrieveData, e);
                return;
            } finally {
                disabledUi.set(false);
            }
            Snapshot snapshot = new Snapshot();
            snapshot.setSnapshotNode(snapshotNode);
            SnapshotData snapshotData = new SnapshotData();
            snapshotData.setSnapshotItems(snapshotItems);
            snapshot.setSnapshotData(snapshotData);
            snapshots.add(0, snapshot);
            disabledUi.set(false);
            completion.accept(snapshot);
        });
    }

    private void loadSnapshotInternal() {
        disabledUi.set(true);
        JobManager.schedule("Load snapshot items", monitor -> {
            SnapshotData snapshotData;
            try {
                this.configurationNode = SaveAndRestoreService.getInstance().getParentNode(snapshotNode.getUniqueId());
                snapshotData = SaveAndRestoreService.getInstance().getSnapshot(snapshotNode.getUniqueId());
            } catch (Exception e) {
                ExceptionDetailsErrorDialog.openError(snapshotTab.getContent(), Messages.errorGeneric, Messages.errorUnableToRetrieveData, e);
                LOGGER.log(Level.INFO, "Error loading snapshot", e);
                return;
            } finally {
                disabledUi.set(false);
            }
            Snapshot snapshot = new Snapshot();
            snapshot.setSnapshotNode(snapshotNode);
            snapshot.setSnapshotData(snapshotData);
            snapshots.add(0, snapshot);
            Platform.runLater(() -> {
                List<TableEntry> tableEntries = createTableEntries(snapshot);
                snapshotTableViewController.updateTable(tableEntries, snapshots, false, false);
                snapshotControlsViewController.getSnapshotRestorableProperty().set(true);
                disabledUi.set(false);
            });
        });
    }

    /**
     * Loads a snapshot {@link Node} for restore.
     *
     * @param snapshotNode An existing {@link Node} of type {@link NodeType#SNAPSHOT}
     */
    public void loadSnapshot(Node snapshotNode) {
        this.snapshotNode = snapshotNode;
        snapshotControlsViewController.setSnapshotNode(snapshotNode);
        snapshotControlsViewController.setRestoreMode(true);
        //snapshotUniqueIdProperty.set(snapshotNode.getUniqueId());
        snapshotTableViewController.setSelectionColumnVisible(true);

        if (this.snapshotNode.getNodeType().equals(NodeType.SNAPSHOT)) {
            loadSnapshotInternal();
        } else {
            snapshotControlsViewController.setNameAndCommentDisabled(true);
            loadCompositeSnapshotInternal(snapshot -> Platform.runLater(() -> {
                List<TableEntry> tableEntries = createTableEntries(snapshot);
                snapshotTableViewController.updateTable(tableEntries, snapshots, false, false);
                snapshotControlsViewController.getSnapshotRestorableProperty().set(true);
            }));
        }
    }

    @FXML
    public void restore(ActionEvent actionEvent) {
        new Thread(() -> {
            List<String> restoreFailedPVNames = new ArrayList<>();
            Snapshot snapshot = snapshots.get(0);
            CountDownLatch countDownLatch = new CountDownLatch(snapshot.getSnapshotData().getSnapshotItems().size());
            snapshot.getSnapshotData().getSnapshotItems()
                    .forEach(e -> pvs.get(getPVKey(e.getConfigPv().getPvName(), e.getConfigPv().isReadOnly())).setCountDownLatch(countDownLatch));

            for (SnapshotItem entry : snapshot.getSnapshotData().getSnapshotItems()) {
                TableEntry e = tableEntryItems.get(getPVKey(entry.getConfigPv().getPvName(), entry.getConfigPv().isReadOnly()));

                boolean restorable = e.selectedProperty().get() && !e.readOnlyProperty().get() &&
                        !entry.getValue().equals(VNoData.INSTANCE);

                if (restorable) {
                    final PV pv = pvs.get(getPVKey(e.pvNameProperty().get(), e.readOnlyProperty().get()));
                    if (entry.getValue() != null) {
                        try {
                            pv.pv.write(Utilities.toRawValue(entry.getValue()));
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
            javafx.scene.Node jfxNode = (javafx.scene.Node) actionEvent.getSource();
            String userData = (String) jfxNode.getUserData();
            if (userData.equalsIgnoreCase("true")) {
                eventReceivers.forEach(r -> r.snapshotRestored(snapshot.getSnapshotNode(), restoreFailedPVNames, this::showLoggingError));
            }
        }).start();
    }
}