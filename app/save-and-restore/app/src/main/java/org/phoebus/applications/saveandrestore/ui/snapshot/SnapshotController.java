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
 *
 * <a target="_blank" href="https://icons8.com/icons/set/percentage">Percentage icon</a> icon by <a target="_blank" href="https://icons8.com">Icons8</a>
 * <a target="_blank" href="https://icons8.com/icons/set/price-tag">Price Tag icon</a> icon by <a target="_blank" href="https://icons8.com">Icons8</a>
 * <a target="_blank" href="https://icons8.com/icons/set/add-tag">Add Tag icon</a> icon by <a target="_blank" href="https://icons8.com">Icons8</a>
 * <a target="_blank" href="https://icons8.com/icons/set/remove-tag">Remove Tag icon</a> icon by <a target="_blank" href="https://icons8.com">Icons8</a>
 */
package org.phoebus.applications.saveandrestore.ui.snapshot;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VEnum;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.Preferences;
import org.phoebus.applications.saveandrestore.common.VDisconnectedData;
import org.phoebus.applications.saveandrestore.common.VNoData;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.ConfigurationData;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Snapshot;
import org.phoebus.applications.saveandrestore.model.SnapshotData;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.model.event.SaveAndRestoreEventReceiver;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.pv.PVPool;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This controller is for the use case of loading a configuration {@link Node} to take a new snapshot.
 * Once the snapshot has been saved, this controller calls the {@link SnapshotTab} API to load
 * the view associated with restore actions.
 */
public class SnapshotController {

    @FXML
    protected TextArea snapshotComment;
    @FXML
    private BorderPane borderPane;

    @FXML
    protected TextField snapshotName;

    @FXML
    protected Button saveSnapshotButton;

    @FXML
    protected ToggleButton showLiveReadbackButton;

    @FXML
    private ToggleButton showHideDeltaPercentageButton;

    @FXML
    protected ToggleButton hideShowEqualItemsButton;

    @FXML
    private Button saveSnapshotAndCreateLogEntryButton;

    protected SnapshotTable snapshotTable;

    private final SimpleStringProperty snapshotNameProperty = new SimpleStringProperty();
    private final SimpleStringProperty snapshotCommentProperty = new SimpleStringProperty();

    protected final Map<String, PV> pvs = new HashMap<>();
    protected final Map<String, TableEntry> tableEntryItems = new LinkedHashMap<>();
    protected final BooleanProperty showLiveReadbackProperty = new SimpleBooleanProperty(false);

    private final BooleanProperty showStoredReadbackProperty = new SimpleBooleanProperty(false);

    private boolean showDeltaPercentage = false;
    protected boolean hideEqualItems;

    /**
     * Property used to indicate if there is new snapshot data to save.
     */
    protected final SimpleBooleanProperty snapshotDataDirty = new SimpleBooleanProperty(false);

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
     * List of added snapshots. In the case of taking a new snapshot, this should contain but one element.
     */
    protected final List<Snapshot> snapshots = new ArrayList<>(10);

    @FXML
    public void initialize() {

        snapshotTable = new SnapshotTable(this);

        borderPane.setCenter(snapshotTable);

        saveSnapshotButton.disableProperty().bind(Bindings.createBooleanBinding(() ->
                        snapshotDataDirty.not().get() ||
                                snapshotNameProperty.isEmpty().get() ||
                                snapshotCommentProperty.isEmpty().get(),
                snapshotDataDirty, snapshotNameProperty, snapshotCommentProperty));

        saveSnapshotAndCreateLogEntryButton.disableProperty().bind(Bindings.createBooleanBinding(() -> (
                        snapshotDataDirty.not().get()) ||
                        snapshotNameProperty.isEmpty().get() ||
                        snapshotCommentProperty.isEmpty().get(),
                snapshotDataDirty, snapshotNameProperty, snapshotCommentProperty));

        // Locate registered SaveAndRestoreEventReceivers
        eventReceivers = ServiceLoader.load(SaveAndRestoreEventReceiver.class);
        // Do not show the create log entry button if no event receivers have been registered
        saveSnapshotAndCreateLogEntryButton.visibleProperty().set(eventReceivers.iterator().hasNext());

        initializeCommonComponents();
    }

    /**
     * Initializes components common between this class and the {@link RestoreSnapshotController} class.
     */
    protected void initializeCommonComponents() {

        snapshotName.textProperty().bindBidirectional(snapshotNameProperty);
        snapshotComment.textProperty().bindBidirectional(snapshotCommentProperty);

        showLiveReadbackButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/show_live_readback_column.png"))));
        showLiveReadbackProperty.bind(showLiveReadbackButton.selectedProperty());
        showLiveReadbackButton.selectedProperty().addListener((a, o, n) -> Platform.runLater(() -> {
            ArrayList<TableEntry> arrayList = new ArrayList<>(tableEntryItems.values());
            snapshotTable.updateTable(arrayList, snapshots, showLiveReadbackProperty.get(), showStoredReadbackProperty.get(), showDeltaPercentage);
        }));

        ImageView showHideDeltaPercentageButtonImageView = new ImageView(new Image(getClass().getResourceAsStream("/icons/show_hide_delta_percentage.png")));
        showHideDeltaPercentageButtonImageView.setFitWidth(16);
        showHideDeltaPercentageButtonImageView.setFitHeight(16);

        showHideDeltaPercentageButton.setGraphic(showHideDeltaPercentageButtonImageView);
        showHideDeltaPercentageButton.selectedProperty()
                .addListener((a, o, n) -> {
                    showDeltaPercentage = n;
                    Platform.runLater(() -> {
                        ArrayList<TableEntry> arrayList = new ArrayList<>(tableEntryItems.values());
                        snapshotTable.updateTable(arrayList, snapshots, showLiveReadbackProperty.get(), showStoredReadbackProperty.get(), showDeltaPercentage);
                    });
                });

        hideShowEqualItemsButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/hide_show_equal_items.png"))));
        hideShowEqualItemsButton.selectedProperty()
                .addListener((a, o, n) -> {
                    hideEqualItems = n;
                    ArrayList<TableEntry> arrayList = new ArrayList<>(tableEntryItems.values());
                    Platform.runLater(() -> snapshotTable.updateTable(arrayList));
                });


        progressIndicator.visibleProperty().bind(disabledUi);
        disabledUi.addListener((observable, oldValue, newValue) -> borderPane.setDisable(newValue));
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
                ExceptionDetailsErrorDialog.openError(showLiveReadbackButton, Messages.errorGeneric, Messages.errorUnableToRetrieveData, e);
                LOGGER.log(Level.INFO, "Error loading configuration", e);
                return;
            }
            List<ConfigPv> configPvs = configuration.getPvList();
            Snapshot snapshot = new Snapshot();
            snapshot.setSnapshotNode(Node.builder().name(Messages.unnamedSnapshot).nodeType(NodeType.SNAPSHOT).build());
            SnapshotData snapshotData = new SnapshotData();
            snapshotData.setSnasphotItems(configurationToSnapshotItems(configPvs));
            snapshot.setSnapshotData(snapshotData);
            snapshots.add(0, snapshot);
            List<TableEntry> tableEntries = createTableEntries(snapshots.get(0));
            Platform.runLater(() -> {
                snapshotTable.updateTable(tableEntries, snapshots, false, false, false);
            });
        });
    }

    @FXML
    @SuppressWarnings("unused")
    private void takeSnapshot() {
        snapshotDataDirty.set(true);
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
                    snapshotData.setSnasphotItems(entries);
                    snapshot.setSnapshotData(snapshotData);
                    snapshots.set(0, snapshot);
                    List<TableEntry> tableEntries = createTableEntries(snapshots.get(0));
                    snapshotTable.updateTable(tableEntries, snapshots, showLiveReadbackProperty.get(), false, showDeltaPercentage);
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
            snapshotData.setSnasphotItems(snapshotItems);
            Snapshot snapshot = new Snapshot();
            snapshot.setSnapshotData(snapshotData);
            snapshot.setSnapshotNode(Node.builder().nodeType(NodeType.SNAPSHOT).name(snapshotNameProperty.get()).description(snapshotCommentProperty.get()).build());
            try {
                snapshot = SaveAndRestoreService.getInstance().saveSnapshot(configurationNode, snapshot);
                Node _snapshotNode = snapshot.getSnapshotNode();
                javafx.scene.Node jfxNode = (javafx.scene.Node) actionEvent.getSource();
                String userData = (String) jfxNode.getUserData();
                if (userData.equalsIgnoreCase("true")) {
                    eventReceivers.forEach(r -> r.snapshotSaved(_snapshotNode, this::showLoggingError));
                }
                // Snapshot successfully saved, clean up and request tab to switch to restore view.
                dispose();
                Platform.runLater(() -> snapshotTab.loadSnapshot(_snapshotNode));
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
            tableEntry.setStoredReadbackValue(entry.getReadbackValue(), 0);
            String key = getPVKey(name, entry.getConfigPv().isReadOnly());
            tableEntry.readbackNameProperty().set(entry.getConfigPv().getReadbackPvName());
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

    /**
     * Returns the number of all snapshots currently visible in the viewer (including the base snapshot).
     *
     * @return the number of all snapshots
     */
    public int getNumberOfSnapshots() {
        return 1;
    }

    private void connectPVs() {
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
                    rowValue.snapshotValProperty().set(newVType);
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
            this.readbackPvName = patchPvName(snapshotTableEntry.readbackNameProperty().get());
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
        if (snapshotDataDirty.get()) {
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
        return hideEqualItems;
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
                        LOGGER.log(Level.WARNING, "Failed to read PV " + pv.pvName);
                    }
                    VType readBackValue = VNoData.INSTANCE;
                    if (pv.readbackPv != null && !pv.readbackValue.equals(VDisconnectedData.INSTANCE)) {
                        try {
                            readBackValue = pv.readbackPv.asyncRead().get(Preferences.readTimeout, TimeUnit.MILLISECONDS);
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Failed to read read-back PV " + pv.readbackPvName);
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

    public void addSnapshot(Node snapshotNode) {

        // Alert and return if user has not yet taken a snapshot.
        if (snapshotDataDirty.not().get()) {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle(Messages.cannotCompareTitle);
            alert.setHeaderText(Messages.cannotCompareHeader);
            DialogHelper.positionDialog(alert, snapshotTab.getTabPane(), -200, -200);
            alert.show();
            return;
        }

        getSnapshotDataAndAdd(snapshotNode);
    }

    /**
     * Updates table data such that the added snapshot can be rendered for the sake of comparison.
     * Since the added snapshot may have a different number of valued, some care is taken to
     * render sensible values (e.g. DISCONNECTED) for such table rows.
     *
     * @param snapshotNode A {@link Node} of type {@link NodeType#SNAPSHOT}
     * @return List of updated {@link TableEntry}s.
     */
    protected List<TableEntry> getSnapshotDataAndAdd(Node snapshotNode) {
        SnapshotData snapshotData = SaveAndRestoreService.getInstance().getSnapshot(snapshotNode.getUniqueId());
        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotNode(snapshotNode);
        snapshot.setSnapshotData(snapshotData);
        int numberOfSnapshots = getNumberOfSnapshots();
        if (numberOfSnapshots == 0) {
            return createTableEntries(snapshot); // do not dispose of anything
        } else {
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
                tableEntry.setSnapshotValue(entry.getValue(), numberOfSnapshots);
                tableEntry.setStoredReadbackValue(entry.getReadbackValue(), numberOfSnapshots);
                tableEntry.readOnlyProperty().set(entry.getConfigPv().isReadOnly());
                baseSnapshotTableEntries.remove(tableEntry);
            }
            // If added snapshot has more items than base snapshot, the base snapshot's values for those
            // table rows need to be set to DISCONNECTED.
            for (TableEntry te : baseSnapshotTableEntries) {
                te.setSnapshotValue(VDisconnectedData.INSTANCE, numberOfSnapshots);
            }
            snapshots.add(snapshot);
            connectPVs();
            snapshotTable.updateTable(new ArrayList<>(tableEntryItems.values()), snapshots, false, false, false);
            return new ArrayList<>(tableEntryItems.values());
        }
    }

    public void setSnapshotNameProperty(String name){
        snapshotNameProperty.set(name);
        // Externally saved so not really dirty.
        snapshotDataDirty.set(false);
    }
}