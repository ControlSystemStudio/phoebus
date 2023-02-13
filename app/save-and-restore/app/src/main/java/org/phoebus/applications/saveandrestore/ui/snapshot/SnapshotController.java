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
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.converter.DoubleStringConverter;
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
import org.phoebus.applications.saveandrestore.ui.model.SnapshotEntry;
import org.phoebus.applications.saveandrestore.ui.model.VSnapshot;
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
    private Button saveSnapshotButton;

    @FXML
    private ToggleButton showLiveReadbackButton;

    @FXML
    private ToggleButton showHideDeltaPercentageButton;

    @FXML
    private ToggleButton hideShowEqualItemsButton;

    @FXML
    private Button saveSnapshotAndCreateLogEntryButton;

    private SnapshotTable snapshotTable;

    private SaveAndRestoreService saveAndRestoreService;

    private final SimpleStringProperty snapshotNameProperty = new SimpleStringProperty();
    private final SimpleStringProperty snapshotCommentProperty = new SimpleStringProperty();

    private VSnapshot snapshot;
    private final Map<String, PV> pvs = new HashMap<>();
    private final Map<String, TableEntry> tableEntryItems = new LinkedHashMap<>();
    private final BooleanProperty snapshotRestorableProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty showLiveReadbackProperty = new SimpleBooleanProperty(false);

    private final BooleanProperty showStoredReadbackProperty = new SimpleBooleanProperty(false);

    private boolean showDeltaPercentage = false;
    protected boolean hideEqualItems;

    /**
     * Property used to indicate if there is new snapshot data to save.
     */
    private final SimpleBooleanProperty snapshotDataDirty = new SimpleBooleanProperty(false);

    private Node configurationNode;

    public static final Logger LOGGER = Logger.getLogger(SnapshotController.class.getName());

    /**
     * The time between updates of dynamic data in the table, in ms
     */
    public static final long TABLE_UPDATE_INTERVAL = 500;


    protected ServiceLoader<SaveAndRestoreEventReceiver> eventReceivers;

    /**
     * A {@link Node} of type {@link NodeType#SNAPSHOT} or {@link NodeType#COMPOSITE_SNAPSHOT}.
     */
    private Node snapshotNode;

    @FXML
    protected VBox progressIndicator;

    /**
     * Used to disable portions of the UI when long-lasting operations are in progress, e.g.
     * take snapshot or save snapshot.
     */
    protected final SimpleBooleanProperty disabledUi = new SimpleBooleanProperty(false);

    @FXML
    public void initialize() {

        saveAndRestoreService = SaveAndRestoreService.getInstance();

        snapshotName.textProperty().bindBidirectional(snapshotNameProperty);
        snapshotComment.textProperty().bindBidirectional(snapshotCommentProperty);

        snapshotTable = new SnapshotTable(this);

        borderPane.setCenter(snapshotTable);

        saveSnapshotButton.disableProperty().bind(Bindings.createBooleanBinding(() -> (
                        snapshotDataDirty.not().get()) ||
                        snapshotNameProperty.isEmpty().get() ||
                        snapshotCommentProperty.isEmpty().get(),
                snapshotDataDirty, snapshotNameProperty, snapshotCommentProperty));

        showLiveReadbackButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/show_live_readback_column.png"))));
        showLiveReadbackButton.setTooltip(new Tooltip(Messages.toolTipShowLiveReadback));
        showLiveReadbackProperty.bind(showLiveReadbackButton.selectedProperty());
        showLiveReadbackButton.selectedProperty().addListener((a, o, n) -> Platform.runLater(() -> {
            ArrayList<TableEntry> arrayList = new ArrayList<>(tableEntryItems.values());
            snapshotTable.updateTable(arrayList, List.of(snapshot), showLiveReadbackProperty.get(), showStoredReadbackProperty.get(), showDeltaPercentage);
        }));

        SpinnerValueFactory<Double> multiplierSpinnerValueFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 999.0, 1.0, 0.01);
        multiplierSpinnerValueFactory.setConverter(new DoubleStringConverter());

        ImageView showHideDeltaPercentageButtonImageView = new ImageView(new Image(getClass().getResourceAsStream("/icons/show_hide_delta_percentage.png")));
        showHideDeltaPercentageButtonImageView.setFitWidth(16);
        showHideDeltaPercentageButtonImageView.setFitHeight(16);

        showHideDeltaPercentageButton.setGraphic(showHideDeltaPercentageButtonImageView);
        showHideDeltaPercentageButton.setTooltip(new Tooltip(Messages.toolTipShowHideDeltaPercentageToggleButton));
        showHideDeltaPercentageButton.selectedProperty()
                .addListener((a, o, n) -> {
                    showDeltaPercentage = n;
                    Platform.runLater(() -> {
                        ArrayList<TableEntry> arrayList = new ArrayList<>(tableEntryItems.values());
                        snapshotTable.updateTable(arrayList, List.of(snapshot), showLiveReadbackProperty.get(), showStoredReadbackProperty.get(), showDeltaPercentage);
                    });
                });

        hideShowEqualItemsButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/hide_show_equal_items.png"))));
        hideShowEqualItemsButton.setTooltip(new Tooltip(Messages.toolTipShowHideEqualToggleButton));
        hideShowEqualItemsButton.selectedProperty()
                .addListener((a, o, n) -> {
                    hideEqualItems = n;
                    ArrayList<TableEntry> arrayList = new ArrayList<>(tableEntryItems.values());
                    Platform.runLater(() -> snapshotTable.updateTable(arrayList));
                });

        // Locate registered SaveAndRestoreEventReceivers
        eventReceivers = ServiceLoader.load(SaveAndRestoreEventReceiver.class);

        progressIndicator.visibleProperty().bind(disabledUi);
        disabledUi.addListener((observable, oldValue, newValue) -> borderPane.setDisable(newValue));

        // Do not show the create log entry button if no event receivers have been registered
        saveSnapshotAndCreateLogEntryButton.visibleProperty().set(eventReceivers.iterator().hasNext());
    }

    /**
     * Loads data from a configuration {@link Node} in order to populate the
     * view with PV items and prepare it to take a snapshot.
     *
     * @param configurationNode A {@link Node} of type {@link NodeType#CONFIGURATION}
     */
    public void newSnapshot(Node configurationNode) {
        this.configurationNode = configurationNode;
        JobManager.schedule("Get configuration", monitor -> {
            ConfigurationData configuration;
            try {
                configuration = saveAndRestoreService.getConfiguration(configurationNode.getUniqueId());
            } catch (Exception e) {
                ExceptionDetailsErrorDialog.openError(showLiveReadbackButton, Messages.errorGeneric, Messages.errorUnableToRetrieveData, e);
                LOGGER.log(Level.INFO, "Error loading configuration", e);
                return;
            }
            List<ConfigPv> configPvs = configuration.getPvList();
            snapshotNode = Node.builder().name(Messages.unnamedSnapshot).nodeType(NodeType.SNAPSHOT).build();
            snapshot = new VSnapshot(snapshotNode, configurationToSnapshotItems(configPvs));
            List<TableEntry> tableEntries = setSnapshotInternal();
            Platform.runLater(() -> {
                snapshotTable.updateTable(tableEntries, List.of(snapshot), false, false, false);
            });
        });
    }

    private void loadCompositeSnapshotInternal(Consumer<VSnapshot> completion) {

        JobManager.schedule("Load composite snapshot items", items -> {
            disabledUi.set(true);
            List<SnapshotItem> snapshotItems;
            try {
                snapshotItems = saveAndRestoreService.getCompositeSnapshotItems(snapshotNode.getUniqueId());
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Error loading composite snapshot for restore", e);
                ExceptionDetailsErrorDialog.openError(showLiveReadbackButton, Messages.errorGeneric, Messages.errorUnableToRetrieveData, e);
                return;
            } finally {
                disabledUi.set(false);
            }
            VSnapshot vSnapshot =
                    //new VSnapshot(snapshotNode, snapshotItemsToSnapshotItems(snapshotItems));
                    new VSnapshot(snapshotNode, snapshotItems);
            disabledUi.set(false);
            completion.accept(vSnapshot);
        });
    }

    @FXML
    public void takeSnapshot() {
        // User may click Take Snapshot button when the view is showing an existing snapshot.
        // In this case we need to "invalidate" the <code>sapshotNode</code> field and set it to a new, unsaved one.
        if (snapshotNode.getUniqueId() != null) {
            snapshotNode = Node.builder().name(Messages.unnamedSnapshot).nodeType(NodeType.SNAPSHOT).build();
        }
        snapshotDataDirty.set(true);
        disabledUi.set(true);

        List<SnapshotItem> entries = new ArrayList<>();
        readAll(list ->
                Platform.runLater(() -> {
                    dispose();
                    disabledUi.set(false);
                    entries.addAll(list);
                    Node snapshotNode = Node.builder().name(Messages.unnamedSnapshot).nodeType(NodeType.SNAPSHOT).build();
                    snapshot = new VSnapshot(snapshotNode, entries);
                    List<TableEntry> tableEntries = setSnapshotInternal();
                    snapshotTable.updateTable(tableEntries, List.of(snapshot), showLiveReadbackProperty.get(), false, showDeltaPercentage);
                })
        );
    }

    @FXML
    public void saveSnapshot() {

        disabledUi.set(true);
        JobManager.schedule("Save Snapshot", monitor -> {
            //List<SnapshotEntry> snapshotEntries = snapshot.getEntries();
            List<SnapshotItem> snapshotItems = snapshot.getEntries();
            //.stream()
            //.map(snapshotEntry -> SnapshotItem.builder().value(snapshotEntry.getValue()).configPv(snapshotEntry.getConfigPv()).readbackValue(snapshotEntry.getReadbackValue()).build())
            //      .collect(Collectors.toList());

            SnapshotData snapshotData = new SnapshotData();
            snapshotData.setSnasphotItems(snapshotItems);
            Snapshot snapshot = new Snapshot();
            snapshot.setSnapshotData(snapshotData);
            snapshot.setSnapshotNode(Node.builder().nodeType(NodeType.SNAPSHOT).name(snapshotNameProperty.get()).description(snapshotCommentProperty.get()).build());
            try {
                snapshot = saveAndRestoreService.saveSnapshot(configurationNode, snapshot);
                snapshotDataDirty.set(false);
                snapshotNode = snapshot.getSnapshotNode();
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

    protected List<TableEntry> setSnapshotInternal() {
        snapshotRestorableProperty.set(snapshot.getSnapshot().isPresent());
        String name;
        TableEntry e;
        SnapshotItem entry;
        for (int i = 0; i < snapshot.getEntries().size(); i++) {
            entry = snapshot.getEntries().get(i);
            e = new TableEntry();
            name = entry.getConfigPv().getPvName();
            e.idProperty().setValue(i + 1);
            e.pvNameProperty().setValue(name);
            e.setConfigPv(entry.getConfigPv());
            //e.selectedProperty().setValue(entry.isSelected());
            e.setSnapshotValue(entry.getValue(), 0);
            e.setStoredReadbackValue(entry.getReadbackValue(), 0);
            String key = getPVKey(name, entry.getConfigPv().isReadOnly());
            e.readbackNameProperty().set(entry.getConfigPv().getReadbackPvName());
            e.readOnlyProperty().set(entry.getConfigPv().isReadOnly());
            tableEntryItems.put(key, e);
            PV pv = pvs.get(key);
            if (pv != null) {
                pv.setSnapshotTableEntry(e);
            }
        }
        connectPVs();
        return new ArrayList<>(tableEntryItems.values());
    }

    /*
    protected List<SnapshotEntry> snapshotItemsToSnapshotItems(List<SnapshotItem> snapshotItems) {
        List<SnapshotEntry> snapshotEntries = new ArrayList<>();
        for (SnapshotItem snapshotItem : snapshotItems) {
            SnapshotEntry snapshotEntry =
                    new SnapshotEntry(snapshotItem, true);
            snapshotEntries.add(snapshotEntry);
        }

        return snapshotEntries;
    }

     */

    protected List<SnapshotItem> configurationToSnapshotItems(List<ConfigPv> configPvs) {
        List<SnapshotItem> snapshotEntries = new ArrayList<>();
        for (ConfigPv configPv : configPvs) {
            SnapshotItem snapshotItem = new SnapshotItem();
            snapshotItem.setConfigPv(configPv);
            snapshotItem.setValue(VNoData.INSTANCE);
            snapshotItem.setReadbackValue(VNoData.INSTANCE);

            //SnapshotEntry snapshotEntry =
            //        new SnapshotEntry(snapshotItem, true);
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
    public VSnapshot getSnapshot(int index) {
        return snapshot;
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
            }
        });
    }

    public void updateLoadedSnapshot(int snapshotIndex, TableEntry rowValue, VType newValue) {
        snapshot.getEntries().stream()
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

    private static class PV {
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
     * Dispose of all allocated resources, except PVs. If <code>closePVs</code> is true the pvs are disposed of,
     * otherwise they are only marked for disposal. It is expected that the caller to this method later checks the PVs
     * and disposes of those that have not been unmarked.
     */
    private void dispose() {
        pvs.values().forEach(PV::dispose);
        pvs.clear();
        tableEntryItems.clear();
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

    private void logNewSnapshotSaved() {
        JobManager.schedule("Log new snapshot saved", monitor -> eventReceivers
                .forEach(r -> r.snapshotSaved(snapshotNode, this::showLoggingError)));
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
     * @param completion Callback receiving a list of {@link SnapshotEntry}s where values for PVs that could
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
                    String name = t.pvNameProperty().get();
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
                    //String delta = snapshot.getDelta(name);
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
}