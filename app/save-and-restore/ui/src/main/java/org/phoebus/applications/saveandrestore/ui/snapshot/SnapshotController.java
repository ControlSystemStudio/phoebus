/**
 * Copyright (C) 2019 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.phoebus.applications.saveandrestore.ui.snapshot;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import org.epics.gpclient.*;
import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.data.NodeChangedListener;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.model.SnapshotEntry;
import org.phoebus.applications.saveandrestore.ui.model.VDisconnectedData;
import org.phoebus.applications.saveandrestore.ui.model.VNoData;
import org.phoebus.applications.saveandrestore.ui.model.VSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SnapshotController implements NodeChangedListener {

    @FXML
    private TextArea snapshotComment;

    @FXML
    private TextField createdBy;

    @FXML
    private TextField createdDate;

    @FXML
    private BorderPane borderPane;

    @FXML
    private TextField snapshotName;

    @FXML
    private Button restoreButton;

    @FXML
    private Button saveSnapshotButton;

    @FXML
    private ToggleButton showLiveReadbackButton;

    @FXML
    private ToggleButton showStoredReadbackButton;

    @FXML
    private ToggleButton hideShowEqualItemsButton;

    private SnapshotTable snapshotTable;

    /**
     * The {@link SnapshotTab} controlled by this controller.
     */
    private SnapshotTab snapshotTab;

    @Autowired
    private SaveAndRestoreService saveAndRestoreService;

    @Autowired
    private String defaultEpicsProtocol;

    private SimpleStringProperty createdByTextProperty = new SimpleStringProperty();
    private SimpleStringProperty createdDateTextProperty = new SimpleStringProperty();
    private SimpleStringProperty snapshotNameProperty = new SimpleStringProperty();
    private SimpleStringProperty snapshotCommentProperty = new SimpleStringProperty();
    private SimpleStringProperty snapshotUniqueIdProperty = new SimpleStringProperty();

    private List<VSnapshot> snapshots = new ArrayList<>(10);
    private final Map<String, PV> pvs = new HashMap<>();
    private final Map<String, String> readbacks = new HashMap<>();
    private final Map<String, TableEntry> tableEntryItems = new LinkedHashMap<>();
    private final BooleanProperty snapshotRestorableProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty snapshotSaveableProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty showLiveReadbackProperty = new SimpleBooleanProperty(false);

    private boolean showStoredReadbacks = false;
    private boolean hideEqualItems;

    private Node config;

    private static Executor UI_EXECUTOR = Platform::runLater;

    //private SimpleBooleanProperty snapshotNodePropertiesDirty = new SimpleBooleanProperty(false);

    public static final Logger LOGGER = Logger.getLogger(SnapshotController.class.getName());

    /**
     * The time between updates of dynamic data in the table, in ms
     */
    public static final long TABLE_UPDATE_INTERVAL = 500;

    @FXML
    public void initialize() {

        snapshotComment.textProperty().bindBidirectional(snapshotCommentProperty);
        createdBy.textProperty().bind(createdByTextProperty);
        createdDate.textProperty().bind(createdDateTextProperty);
        snapshotName.textProperty().bindBidirectional(snapshotNameProperty);

        snapshotTable = new SnapshotTable(this);
        borderPane.setCenter(snapshotTable);

        saveSnapshotButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
           boolean canSave = snapshotSaveableProperty.get() && (!snapshotNameProperty.isEmpty().get() && !snapshotCommentProperty.isEmpty().get());
            return !canSave;
        }, snapshotSaveableProperty, snapshotNameProperty, snapshotCommentProperty));

        showLiveReadbackButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/show_live_readback_column.png"))));
        showLiveReadbackButton.setTooltip(new Tooltip(Messages.toolTipShowLiveReadback));
        showLiveReadbackProperty.bind(showLiveReadbackButton.selectedProperty());
        showLiveReadbackButton.selectedProperty().addListener((a, o, n) -> {
            UI_EXECUTOR.execute(() -> snapshotTable.updateTable(new ArrayList(tableEntryItems.values()), snapshots, showLiveReadbackProperty.get(), showStoredReadbacks));
        });

        showStoredReadbackButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/show_stored_readback_column.png"))));
        showStoredReadbackButton.setTooltip(new Tooltip(Messages.toolTipShowStoredReadback));
        showStoredReadbackButton.selectedProperty().addListener((a, o, n) -> {
            UI_EXECUTOR.execute(() -> snapshotTable.updateTable(new ArrayList(tableEntryItems.values()), snapshots, showLiveReadbackProperty.get(), showStoredReadbacks));
        });

        hideShowEqualItemsButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/hide_show_equal_items.png"))));
        hideShowEqualItemsButton.setTooltip(new Tooltip(Messages.toolTipShowHideEqualToggleButton));
        hideShowEqualItemsButton.selectedProperty()
                .addListener((a, o, n) -> {
                    hideEqualItems = n;
                    UI_EXECUTOR.execute(() -> snapshotTable.updateTable(new ArrayList(tableEntryItems.values())));
                });

        restoreButton.disableProperty().bind(snapshotRestorableProperty.not());

        saveAndRestoreService.addNodeChangeListener(this);
    }

    public void setSnapshotTab(SnapshotTab snapshotTab){
        this.snapshotTab = snapshotTab;
    }

    public void loadSnapshot(Node snapshot) {
        try {
            this.config = saveAndRestoreService.getParentNode(snapshot.getUniqueId());
            snapshotNameProperty.set(snapshot.getName());
            snapshotUniqueIdProperty.set(snapshot.getUniqueId());
            snapshotTab.updateTabTitile(snapshot.getName(), Boolean.parseBoolean(snapshot.getProperty("golden")));
        } catch (Exception e) {
            e.printStackTrace();
        }
        loadSnapshotInternal(snapshot);
    }

    public void addSnapshot(Node treeNode) {
        if (!treeNode.getNodeType().equals(NodeType.SNAPSHOT)) {
            return;
        }

        for (VSnapshot vSnapshot : snapshots) {
            if (treeNode.getUniqueId().equals(vSnapshot.getId())) {
                return;
            }
        }

        try {
            Node snapshot = saveAndRestoreService.getNode(treeNode.getUniqueId());
            List<SnapshotItem> snapshotItems = saveAndRestoreService.getSnapshotItems(snapshot.getUniqueId());
            VSnapshot vSnapshot =
                    new VSnapshot(snapshot, snapshotItemsToSnapshotEntries(snapshotItems));
            List<TableEntry> tableEntries = addSnapshot(vSnapshot);
            snapshotTable.updateTable(tableEntries, snapshots, false, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadSaveSet(Node node){

        SnapshotController.this.config = saveAndRestoreService.getNode(node.getUniqueId());
        try {
            List<ConfigPv> configPvs = saveAndRestoreService.getConfigPvs(config.getUniqueId());
            Node snapshot = Node.builder().name(Messages.unnamedSnapshot).nodeType(NodeType.SNAPSHOT).build();
            VSnapshot vSnapshot =
                    new VSnapshot(snapshot, saveSetToSnapshotEntries(configPvs));
            List<TableEntry> tableEntries = setSnapshotInternal(vSnapshot);
            UI_EXECUTOR.execute(() -> {
                snapshotTable.updateTable(tableEntries, snapshots, false, false);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadSnapshotInternal(Node snapshot) {

        UI_EXECUTOR.execute(() -> {
            try {
            List<SnapshotItem> snapshotItems = saveAndRestoreService.getSnapshotItems(snapshot.getUniqueId());

            snapshotCommentProperty.set(snapshot.getProperty("comment"));
            createdDateTextProperty.set(snapshot.getCreated().toString());
            createdByTextProperty.set(snapshot.getUserName());
            snapshotNameProperty.set(snapshot.getName());

            VSnapshot vSnapshot =
                    new VSnapshot(snapshot, snapshotItemsToSnapshotEntries(snapshotItems));
            List<TableEntry> tableEntries = loadSnapshotInternal(vSnapshot);

            snapshotTable.updateTable(tableEntries, snapshots, false, false);
            snapshotRestorableProperty.set(true);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    public void restore(ActionEvent event) {
        new Thread(() -> {
            VSnapshot s = snapshots.get(0);
            CountDownLatch countDownLatch = new CountDownLatch(s.getEntries().size());
            s.getEntries().stream().forEach(e -> {
                pvs.get(e.getPVName()).setCountDownLatch(countDownLatch);
                pvs.get(e.getPVName()).writeStatus = PVEvent.Type.WRITE_FAILED;
            });
            try {
                List<SnapshotEntry> entries = s.getEntries();
                for (SnapshotEntry entry : entries) {
                    final TableEntry e = tableEntryItems.get(entry.getPVName());
                    if (e.selectedProperty().get() && !e.readOnlyProperty().get()) {
                        final PV pv = pvs.get(e.getConfigPv().getPvName());
                        if (entry.getValue() != null) {
                            pv.pv.write(entry.getValue());
                        }
                    }
                }

                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                List<String> messages = new ArrayList<>();
                for (SnapshotEntry entry : entries) {
                    PV pv = pvs.get(entry.getPVName());
                    if (pv.getWriteStatus().equals(PVEvent.Type.WRITE_FAILED)) {
                        StringBuilder sb = new StringBuilder(200);
                        sb.append(pv.pvName).append(':').append(" error writing PV");
                        messages.add(sb.toString());
                    }
                }

                if (messages.isEmpty()) {
                    LOGGER.log(Level.FINE, "Restored snapshot {0}", s.getSnapshot().get().getName());
                } else {
                    Collections.sort(messages);
                    StringBuilder sb = new StringBuilder(messages.size() * 200);
                    messages.forEach(e -> sb.append(e).append('\n'));
                    LOGGER.log(Level.WARNING,
                            "Not all PVs could be restored for {0}: {1}. The following errors occured:\n{2}",
                            new Object[] { s.getSnapshot().get().getName(), s.getSnapshot().get(), sb.toString() });
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(Messages.restoreErrorTitle);
                    alert.setContentText(sb.toString());
                    alert.setHeaderText(Messages.restoreErrorContent);
                    alert.showAndWait();
                }
            } finally {
            }
        }).start();

    }

    @FXML
    public void takeSnapshot(ActionEvent event) {

        UI_EXECUTOR.execute(() -> {
            snapshotNameProperty.set(null);
            snapshotCommentProperty.set(null);
            createdByTextProperty.set(null);
            createdDateTextProperty.set(null);
            snapshotSaveableProperty.setValue(false);
        });
        try {
            List<SnapshotEntry> entries = new ArrayList<>(tableEntryItems.size());
            PV pv;
            String name, delta = null;
            String readbackName = null;
            VType value = null;
            VType readbackValue = null;
            for (TableEntry t : tableEntryItems.values()) {
                name = t.pvNameProperty().get();
                pv = pvs.get(t.pvNameProperty().get());

                // there is no issues with non atomic access to snapshotTableEntryPvProxy.value or snapshotTableEntryPvProxy.readbackValue because the PV is
                // suspended and the value could not change while suspended
                value = pv == null || pv.pvValue == null ? VDisconnectedData.INSTANCE : pv.pvValue;
                readbackName = readbacks.get(name);
                readbackValue = pv == null || pv.readbackValue == null ? VDisconnectedData.INSTANCE : pv.readbackValue;
                for (VSnapshot s : getAllSnapshots()) {
                    delta = s.getDelta(name);
                    if (delta != null) {
                        break;
                    }
                }

                entries.add(new SnapshotEntry(t.getConfigPv(), value, t.selectedProperty().get(), readbackName, readbackValue,
                        delta, t.readOnlyProperty().get()));
            }

            Node snapshot = Node.builder().name(Messages.unnamedSnapshot).nodeType(NodeType.SNAPSHOT).build();

            VSnapshot taken = new VSnapshot(snapshot, entries);
            snapshots.clear();
            snapshots.add(taken);
            List<TableEntry> tableEntries = loadSnapshotInternal(taken);
            UI_EXECUTOR.execute(() -> {
                snapshotTable.updateTable(tableEntries, snapshots, showLiveReadbackProperty.get(), false);
                snapshotSaveableProperty.setValue(true);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally{

        }
    }

    @FXML
    public void saveSnapshot(ActionEvent event) {
        if(snapshotSaveableProperty.get()){ // There is a new snapshot to save
            VSnapshot snapshot = snapshots.get(0);
            List<SnapshotEntry> snapshotEntries = snapshot.getEntries();
            List<SnapshotItem> snapshotItems = snapshotEntries
                    .stream()
                    .map(snapshotEntry -> SnapshotItem.builder().value(snapshotEntry.getValue()).configPv(snapshotEntry.getConfigPv()).readbackValue(snapshotEntry.getReadbackValue()).build())
                    .collect(Collectors.toList());
            try {
                Node savedSnapshot = saveAndRestoreService.saveSnapshot(config, snapshotItems, snapshotNameProperty.get(), snapshotCommentProperty.get());
                loadSnapshot(savedSnapshot);
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(Messages.errorActionFailed);
                alert.setContentText(e.getMessage());
                alert.setHeaderText(Messages.saveSnapshotErrorContent);
                alert.showAndWait();
            }
        }
        else{ // Only snapshot name and/or comment have changed
            try {
                Node snapshotNode = snapshots.get(0).getSnapshot().get();
                Map<String, String> properties = snapshotNode.getProperties();
                properties.put("comment", snapshotCommentProperty.get());
                snapshotNode.setProperties(properties);
                snapshotNode.setName(snapshotNameProperty.get());
                snapshotNode = saveAndRestoreService.updateNode(snapshotNode);
                loadSnapshot(snapshotNode);
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(Messages.errorActionFailed);
                alert.setContentText(e.getMessage());
                alert.setHeaderText(Messages.saveSnapshotErrorContent);
                alert.showAndWait();
            }
        }

    }

    public List<VSnapshot> getAllSnapshots() {
        synchronized (snapshots) {
            return new ArrayList<>(snapshots);
        }
    }

    private List<TableEntry> loadSnapshotInternal(VSnapshot snapshotData){
        dispose();
        return setSnapshotInternal(snapshotData);
    }

    private List<TableEntry> setSnapshotInternal(VSnapshot snapshotData) {
         List<SnapshotEntry> entries = snapshotData.getEntries();
        synchronized (snapshots) {
            snapshots.add(snapshotData);
        }
        UI_EXECUTOR.execute(() -> snapshotRestorableProperty.set(snapshotData.getSnapshot().isPresent()));
        String name;
        TableEntry e;
        SnapshotEntry entry;
        for (int i = 0; i < entries.size(); i++) {
            entry = entries.get(i);
            e = new TableEntry();
            name = entry.getPVName();
            e.idProperty().setValue(i + 1);
            e.pvNameProperty().setValue(name);
            e.setConfigPv(entry.getConfigPv());
            e.selectedProperty().setValue(entry.isSelected());
            e.setSnapshotValue(entry.getValue(), 0);
            e.setStoredReadbackValue(entry.getReadbackValue(), 0);
            tableEntryItems.put(name, e);
            readbacks.put(name, entry.getReadbackName());
            e.readbackNameProperty().set(entry.getReadbackName());
            e.readOnlyProperty().set(entry.isReadOnly());
            PV pv = pvs.get(name);
            if(pv != null){
                pv.setSnapshotTableEntry(e);
            }
        }
        connectPVs();
        UI_EXECUTOR.execute(() -> snapshotSaveableProperty.set(snapshotData.isSaveable()));
        return new ArrayList<>(tableEntryItems.values());
    }

    private List<TableEntry> addSnapshot(VSnapshot data) {
        int numberOfSnapshots = getNumberOfSnapshots();
        if (numberOfSnapshots == 0) {
            return setSnapshotInternal(data); // do not dispose of anything
        } else if (numberOfSnapshots == 1 && !getSnapshot(0).isSaveable() && !getSnapshot(0).isSaved()) {
            return setSnapshotInternal(data);
        } else {
            List<SnapshotEntry> entries = data.getEntries();
            String n;
            TableEntry e;
            List<TableEntry> withoutValue = new ArrayList<>(tableEntryItems.values());
            SnapshotEntry entry;
            for (int i = 0; i < entries.size(); i++) {
                entry = entries.get(i);
                n = entry.getPVName();
                e = tableEntryItems.get(n);
                if (e == null) {
                    e = new TableEntry();
                    e.idProperty().setValue(tableEntryItems.size() + i + 1);
                    e.pvNameProperty().setValue(n);
                    e.setConfigPv(entry.getConfigPv());
                    tableEntryItems.put(n, e);
                    readbacks.put(n, entry.getReadbackName());
                    e.readbackNameProperty().set(entry.getReadbackName());
                }
                e.setSnapshotValue(entry.getValue(), numberOfSnapshots);
                e.setStoredReadbackValue(entry.getReadbackValue(), numberOfSnapshots);
                e.readOnlyProperty().set(entry.isReadOnly());
                withoutValue.remove(e);
            }
            for (TableEntry te : withoutValue) {
                te.setSnapshotValue(VDisconnectedData.INSTANCE, numberOfSnapshots);
            }
            synchronized (snapshots) {
                snapshots.add(data);
            }
            connectPVs();
            UI_EXECUTOR.execute(() -> {
                if (!snapshotSaveableProperty.get()) {
                    snapshotSaveableProperty.set(data.isSaveable());
                }
                snapshotRestorableProperty.set(true);
            });

            return new ArrayList<>(tableEntryItems.values());
        }
    }

    private List<SnapshotEntry> snapshotItemsToSnapshotEntries(List<SnapshotItem> snapshotItems) {
        List<SnapshotEntry> snapshotEntries = new ArrayList<>();
        for (SnapshotItem snapshotItem : snapshotItems) {
            SnapshotEntry snapshotEntry =
                    new SnapshotEntry(snapshotItem, true);
            snapshotEntries.add(snapshotEntry);
        }

        return snapshotEntries;
    }

    private List<SnapshotEntry> saveSetToSnapshotEntries(List<ConfigPv> configPvs){
        List<SnapshotEntry> snapshotEntries = new ArrayList<>();
        for (ConfigPv configPv : configPvs) {
            SnapshotEntry snapshotEntry =
                    new SnapshotEntry(configPv, VNoData.INSTANCE, true, configPv.getReadbackPvName(), VNoData.INSTANCE, null, configPv.isReadOnly());
            snapshotEntries.add(snapshotEntry);
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
        synchronized (snapshots) {
            return snapshots.isEmpty() ? null
                    : index >= snapshots.size() ? snapshots.get(snapshots.size() - 1)
                    : index < 0 ? snapshots.get(0) : snapshots.get(index);
        }
    }

    /**
     * Returns the number of all snapshots currently visible in the viewer (including the base snapshot).
     *
     * @return the number of all snapshots
     */
    public int getNumberOfSnapshots() {
        synchronized (snapshots) {
            return snapshots.size();
        }
    }

    private void connectPVs() {
        try {
            tableEntryItems.values().forEach(e -> {
                PV pv = pvs.get(e.getConfigPv().getPvName());
                if (pv == null) {
                    pvs.put(e.getConfigPv().getPvName(), new PV(e));
                }
            });
        } finally {
        }
    }

    private class PV {
        final String pvName;
        final String readbackPvName;
        CountDownLatch countDownLatch;
        org.epics.gpclient.PV<VType, VType> pv;
        PVReader<VType> pvReader;
        PVReader<VType> readbackReader;
        PVEvent.Type writeStatus = PVEvent.Type.WRITE_FAILED;
        volatile VType pvValue = VDisconnectedData.INSTANCE;
        volatile VType readbackValue = VDisconnectedData.INSTANCE;
        TableEntry snapshotTableEntry;
        boolean readOnly;

        PV(TableEntry snapshotTableEntry) {
            this.snapshotTableEntry = snapshotTableEntry;
            this.pvName = patchPvName(snapshotTableEntry.pvNameProperty().get());
            this.readbackPvName = patchPvName(snapshotTableEntry.readbackNameProperty().get());
            this.readOnly = snapshotTableEntry.readOnlyProperty().get();

            if(this.readOnly){
                pvReader = GPClient.read(pvName)
                        .addReadListener((event, p) -> {
                            this.pvValue = p.isConnected() ? p.getValue() : VDisconnectedData.INSTANCE;
                            this.snapshotTableEntry.setLiveValue(this.pvValue);
                        })
                        .connectionTimeout(Duration.ofMillis(3*TABLE_UPDATE_INTERVAL))
                        .maxRate(Duration.ofMillis(TABLE_UPDATE_INTERVAL))
                        .start();
            }
            else{
                PVConfiguration pvConfiguration = GPClient.readAndWrite(GPClient.channel(pvName));
                pv = pvConfiguration.addListener((event, p) -> {
                    if(event.getType().contains(PVEvent.Type.VALUE)){
                        this.pvValue = p.isConnected() ? (VType)p.getValue() : VDisconnectedData.INSTANCE;
                        this.snapshotTableEntry.setLiveValue(this.pvValue);
                    }
                    else if(event.getType().contains(PVEvent.Type.WRITE_SUCCEEDED)){
                        if(countDownLatch != null){
                            LOGGER.info(countDownLatch + " Write OK, signalling latch");
                            countDownLatch.countDown();
                        }
                        writeStatus = PVEvent.Type.WRITE_SUCCEEDED;
                    }
                    else if(event.getType().contains(PVEvent.Type.WRITE_FAILED)){
                        if(countDownLatch != null){
                            LOGGER.info(countDownLatch + "Write FAILED, signalling latch");
                            countDownLatch.countDown();
                        }
                        writeStatus = PVEvent.Type.WRITE_FAILED;
                    }
                }).maxRate(Duration.ofMillis(TABLE_UPDATE_INTERVAL))
                        .start();
            }

            if (readbackPvName != null && !readbackPvName.isEmpty()) {
                this.readbackReader = GPClient.read(this.readbackPvName)
                        .addReadListener((event, p) -> {
                            if (showLiveReadbackProperty.get()) {
                                this.readbackValue = p.isConnected() ? p.getValue() : VDisconnectedData.INSTANCE;
                                snapshotTableEntry.setReadbackValue(this.readbackValue);
                            }
                        }).maxRate(Duration.ofMillis(TABLE_UPDATE_INTERVAL)).start();
            }
        }

        private String patchPvName(String pvName){
            if(pvName == null || pvName.isEmpty()){
                return null;
            }
            else if(pvName.startsWith("ca://") || pvName.startsWith("pva://")){
                return pvName;
            }
            else{
                return defaultEpicsProtocol + "://" + pvName;
            }
        }

        public void setCountDownLatch(CountDownLatch countDownLatch){
            LOGGER.info(countDownLatch + " New CountDownLatch set");
            this.countDownLatch = countDownLatch;
        }

        public void setSnapshotTableEntry(TableEntry snapshotTableEntry){
            this.snapshotTableEntry = snapshotTableEntry;
        }

        public PVEvent.Type getWriteStatus(){
            return writeStatus;
        }

        void dispose() {
            if (pv != null && !pv.isClosed()) {
                pv.close();
            }
            if(pvReader != null && !pvReader.isClosed()){
                pvReader.close();
            }
            if (readbackReader != null && !readbackReader.isClosed()) {
                readbackReader.close();
            }
        }
    }

    public boolean handleSnapshotTabClosed(){
        if(snapshotSaveableProperty.get()){
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(Messages.promptCloseSnapshotTabTitle);
            alert.setContentText(Messages.promptCloseSnapshotTabContent);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get().equals(ButtonType.CANCEL)) {
                return false;
            }
        }
        dispose();
        saveAndRestoreService.removeNodeChangeListener(this);
        return true;
    }

    /**
     * Dispose of all allocated resources, except PVs. If <code>closePVs</code> is true the pvs are disposed of,
     * otherwise they are only marked for disposal. It is expected that the caller to this method later checks the PVs
     * and disposes of those that have not been unmarked.
     *
     */
    private void dispose() {
        synchronized (snapshots) {
            pvs.values().forEach(e -> e.dispose());
            pvs.clear();
            tableEntryItems.clear();
            snapshots.clear();
        }
    }

    /**
     * Returns true if items with equal live and snapshot values are currently hidden or false is shown.
     *
     * @return true if equal items are hidden or false otherwise.
     */
    public boolean isHideEqualItems() {
        return hideEqualItems;
    }

    @Override
    public void nodeChanged(Node node) {
        if (node.getUniqueId().equals(snapshotUniqueIdProperty.get())) {
            snapshotNameProperty.set(node.getName());
            snapshotSaveableProperty.setValue(false);
            snapshotTab.updateTabTitile(node.getName(), Boolean.parseBoolean(node.getProperty("golden")));
        }
    }
}