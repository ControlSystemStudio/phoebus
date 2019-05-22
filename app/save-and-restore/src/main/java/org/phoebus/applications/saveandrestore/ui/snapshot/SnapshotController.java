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
import javafx.beans.property.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import org.epics.gpclient.*;
import org.epics.pvdata.pv.PVScalarType;
import org.epics.vtype.*;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.GUIUpdateThrottle;
import org.phoebus.applications.saveandrestore.ui.Utilities;
import org.phoebus.applications.saveandrestore.ui.model.SnapshotEntry;
import org.phoebus.applications.saveandrestore.ui.model.VDisconnectedData;
import org.phoebus.applications.saveandrestore.ui.model.VNoData;
import org.phoebus.applications.saveandrestore.ui.model.VSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import se.esss.ics.masar.model.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class SnapshotController {

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

    @FXML
    private javafx.scene.Node tabContent;

    private SnapshotTable snapshotTable;

    @Autowired
    private SaveAndRestoreService saveAndRestoreService;
    private SimpleStringProperty createdByTextProperty = new SimpleStringProperty();
    private SimpleStringProperty createdDateTextProperty = new SimpleStringProperty();
    private SimpleStringProperty snapshotNameProperty = new SimpleStringProperty();
    private SimpleStringProperty snapshotCommentProperty = new SimpleStringProperty();

    private List<VSnapshot> snapshots = new ArrayList<>(10);
    private final Map<String, SnapshotTableEntryPvProxy> pvs = new HashMap<>();
    private final Map<String, SnapshotTableEntryPvProxy> pvsForDisposal = new HashMap<>();
    private final Map<String, String> readbacks = new HashMap<>();
    private final Map<String, SnapshotTableEntry> items = new LinkedHashMap<>();
    private final BooleanProperty snapshotRestorableProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty snapshotSaveableProperty = new SimpleBooleanProperty(false);
    private final ObjectProperty<VSnapshot> baseSnapshotProperty = new SimpleObjectProperty<>(null);

    private SimpleStringProperty tabTitleProperty = new SimpleStringProperty();

    private final AtomicInteger suspend = new AtomicInteger(0);
    private boolean showLiveReadbacks = false;
    private boolean showStoredReadbacks = false;
    private boolean hideEqualItems;

    private Node config;

    private static Executor UI_EXECUTOR = Platform::runLater;

    private TabTitleChangedListener tabTitleChangedListener;

    private boolean snapshotDataDirty = false;

    public static final Logger LOGGER = Logger.getLogger(SnapshotController.class.getName());

    /**
     * The rate at which the snapshotTable is updated
     */
    public static final long TABLE_UPDATE_RATE = 500;

    private final GUIUpdateThrottle throttle = new GUIUpdateThrottle(20, TABLE_UPDATE_RATE) {
        @Override
        protected void fire() {
            UI_EXECUTOR.execute(() -> {
                if (suspend.get() > 0) {
                    return;
                }
                pvs.forEach((k, v) -> {
                    items.get(k).setLiveValue(v.pvValue);
                    items.get(k).setReadbackValue(v.readbackValue);

                });
            });
        }
    };


    public void setTabTitleChangedListener(TabTitleChangedListener tabTitleChangedListener){
        this.tabTitleChangedListener = tabTitleChangedListener;
    }

    @FXML
    public void initialize() {

        snapshotComment.textProperty().bindBidirectional(snapshotCommentProperty);
        createdBy.textProperty().bind(createdByTextProperty);
        createdDate.textProperty().bind(createdDateTextProperty);
        snapshotName.textProperty().bindBidirectional(snapshotNameProperty);


        snapshotTable = new SnapshotTable(this);

        borderPane.setCenter(snapshotTable);

        saveSnapshotButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            return snapshotSaveableProperty.not().get() || snapshotNameProperty.isEmpty().get() || snapshotCommentProperty.isEmpty().get();
        }, snapshotSaveableProperty, snapshotNameProperty, snapshotCommentProperty));

        showLiveReadbackButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/show_live_readback_column.png"))));

        showLiveReadbackButton.selectedProperty().addListener((a, o, n) -> {
            showLiveReadbacks = n;
            snapshotTable.updateTable(new ArrayList(items.values()), snapshots, showLiveReadbacks, showStoredReadbacks);
        });


        showStoredReadbackButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/show_stored_readback_column.png"))));

        showStoredReadbackButton.selectedProperty().addListener((a, o, n) -> {
            showStoredReadbacks = n;
            snapshotTable.updateTable(new ArrayList(items.values()), snapshots, showLiveReadbacks, showStoredReadbacks);
        });

        hideShowEqualItemsButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/hide_show_equal_items.png"))));
        hideShowEqualItemsButton.selectedProperty()
                .addListener((a, o, n) -> {
                    hideEqualItems = n;
                    UI_EXECUTOR.execute(() -> snapshotTable.updateTable(new ArrayList(items.values())));
                });

        // Disable Restore button if there are no snapshot values
//        restoreButton.disableProperty().bind(Bindings.createBooleanBinding(() ->
//                        snapshotTable.getItems() == null ||
//                        snapshotTable.getItems().size() == 0 ||
//                        snapshotTable.getItems().get(0).valueProperty().getValue().value.getClass().isAssignableFrom(VNoData.class),
//                snapshotTable.getItems()));
        restoreButton.disableProperty().bind(snapshotRestorableProperty.not());

        throttle.start();
    }

    public void loadSnapshot(Node treeNode) {
        try {
            this.config = saveAndRestoreService.getParentNode(treeNode.getUniqueId());
        } catch (Exception e) {
            e.printStackTrace();
        }
        loadSnapshotInternal(treeNode);
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
                    new VSnapshot(snapshot, snapshotItemsToSnapshotEntries(snapshotItems), Instant.ofEpochMilli(snapshot.getCreated().getTime()));
            List<SnapshotTableEntry> tableEntries = addSnapshot(vSnapshot);
            snapshotTable.updateTable(tableEntries, snapshots, false, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadSaveSet(String saveSetUniqueId){
        this.config = saveAndRestoreService.getNode(saveSetUniqueId);

        try {
            List<ConfigPv> configPvs = saveAndRestoreService.getConfigPvs(config.getUniqueId());
            VSnapshot vSnapshot =
                    new VSnapshot(saveSetToSnapshotEntries(configPvs));
            List<SnapshotTableEntry> tableEntries = setSnapshotInternal(vSnapshot);
            UI_EXECUTOR.execute(() -> {
                snapshotTable.updateTable(tableEntries, snapshots, false, false);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadSnapshotInternal(Node snapshot) {

        try {

            List<SnapshotItem> snapshotItems = saveAndRestoreService.getSnapshotItems(snapshot.getUniqueId());

            snapshotCommentProperty.set(snapshot.getProperty("comment"));
            createdDateTextProperty.set(snapshot.getCreated().toString());
            createdByTextProperty.set(snapshot.getUserName());
            snapshotNameProperty.set(snapshot.getName());

            VSnapshot vSnapshot =
                    new VSnapshot(snapshot, snapshotItemsToSnapshotEntries(snapshotItems), Instant.ofEpochMilli(snapshot.getCreated().getTime()));
            List<SnapshotTableEntry> tableEntries = loadSnapshotInternal(vSnapshot);
            UI_EXECUTOR.execute(() -> {
                snapshotTable.updateTable(tableEntries, snapshots, false, false);
                if(tabTitleChangedListener != null){
                    tabTitleChangedListener.tabTitleChanged(snapshot.getName());
                }
                snapshotRestorableProperty.setValue(true);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML
    public void restore(ActionEvent event) {
        VSnapshot s = snapshots.get(0);
        Set<SnapshotTableEntryPvProxy> restorablePVs = new HashSet<>();
        try {
            suspend();
            List<SnapshotEntry> entries = s.getEntries();
            //final Set<SnapshotTableEntryPvProxy> restoredPVs = new HashSet<>();
            for (SnapshotEntry entry : entries) {
                final SnapshotTableEntry e = items.get(entry.getPVName());
                // only restore the value if the entry is in the filtered list as well
                if (e.selectedProperty().get() && !e.readOnlyProperty().get()) {
                    final SnapshotTableEntryPvProxy snapshotTableEntryPvProxy = pvs.get(e);

                    //restoredPVs.add(snapshotTableEntryPvProxy);
                    restorablePVs.add(snapshotTableEntryPvProxy);
                    //snapshotTableEntryPvProxy.pv.addPVWriterListener(l);
                    Object val = Utilities.toRawValue(entry.getValue());
                    if (val != null) {
                        snapshotTableEntryPvProxy.pv.write(val);
                    }

                }
            }
            try {
                long time = System.currentTimeMillis();
                while (System.currentTimeMillis() - time < 30000
                        /*&& !SaveRestoreService.getInstance().isCurrentJobCancelled()*/) {
                    synchronized (restorablePVs) {
                        boolean done = true;
                        for(SnapshotTableEntryPvProxy proxy : restorablePVs){
                            if(proxy.writeStatus == PVEvent.Type.WRITE_FAILED){
                                restorablePVs.wait(100);
                                done = false;
                                break;
                            }
                        }
                        if(done){
                           break;
                        }
                    }
                }
            } catch (InterruptedException e) {
                // ignore
            }

            List<String> messages = new ArrayList<>();
            for (SnapshotTableEntryPvProxy proxy : restorablePVs) {
                if (proxy.getWriteStatus().equals(PVEvent.Type.WRITE_FAILED)) {
                    StringBuilder sb = new StringBuilder(200);
                    sb.append(proxy.pvName).append(':').append(" error writing PV");
                    messages.add(sb.toString());
                }
            }

//            if (restoredPVs.size() != restorablePVCount) {
//                // not all PVs responded in time
//                for (SnapshotTableEntryPvProxy snapshotTableEntryPvProxy : restorablePVs.keySet()) {
//                    if (restoredPVs.containsKey(snapshotTableEntryPvProxy)) {
//                        messages.add(snapshotTableEntryPvProxy.pvName + ": Timeout");
//                    }
//                }
//            }
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
                alert.setTitle("Restore Error");
                alert.setContentText(sb.toString());
                alert.setHeaderText("Not all PVs were restored.");
                Optional<ButtonType> result = alert.showAndWait();
            }
        } finally {
            for (SnapshotTableEntryPvProxy proxy : restorablePVs) {
                proxy.restoreDone();
            }
            resume();
        }
    }

    @FXML
    public void takeSnapshot(ActionEvent event) {
        suspend();
        UI_EXECUTOR.execute(() -> {
            snapshotNameProperty.set(null);
            snapshotCommentProperty.set(null);
            createdByTextProperty.set(null);
            createdDateTextProperty.set(null);
        });
        try {
            List<SnapshotEntry> entries = new ArrayList<>(items.size());
            SnapshotTableEntryPvProxy snapshotTableEntryPvProxy;
            String name, delta = null;
            String readback = null;
            VType value = null;
            VType readbackValue = null;
            for (SnapshotTableEntry t : items.values()) {
                name = t.pvNameProperty().get();
                snapshotTableEntryPvProxy = pvs.get(name);
                // there is no issues with non atomic access to snapshotTableEntryPvProxy.value or snapshotTableEntryPvProxy.readbackValue because the SnapshotTableEntryPvProxy is
                // suspended and the value could not change while suspended
                //value = snapshotTableEntryPvProxy == null || snapshotTableEntryPvProxy.pvValue == null ? VDisconnectedData.INSTANCE : snapshotTableEntryPvProxy.pvValue;
                value = snapshotTableEntryPvProxy == null || snapshotTableEntryPvProxy.pvValue == null ? VDisconnectedData.INSTANCE : snapshotTableEntryPvProxy.pvValue;
                readback = readbacks.get(name);
                readbackValue = snapshotTableEntryPvProxy == null || snapshotTableEntryPvProxy.readbackValue == null ? VDisconnectedData.INSTANCE
                        : snapshotTableEntryPvProxy.readbackValue;
                for (VSnapshot s : getAllSnapshots()) {
                    delta = s.getDelta(name);
                    if (delta != null) {
                        break;
                    }
                }
                entries.add(new SnapshotEntry(t.getConfigPv(), value, t.selectedProperty().get(), readback, readbackValue,
                        delta, t.readOnlyProperty().get()));
            }

            Node snapshot = Node.builder().name("<unnamed snapshot>").nodeType(NodeType.SNAPSHOT).build();
            VSnapshot taken = new VSnapshot(snapshot, entries, Instant.now());
            snapshotDataDirty = true;
            snapshots.clear();
            List<SnapshotTableEntry> tableEntries = loadSnapshotInternal(taken);
            UI_EXECUTOR.execute(() -> {
                snapshotTable.updateTable(tableEntries, snapshots, false, false);
                if(tabTitleChangedListener != null){
                    tabTitleChangedListener.tabTitleChanged("* " + snapshot.getName());
                }
                snapshotDataDirty = true;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally{
            resume();
        }
    }

    @FXML
    public void saveSnapshot(ActionEvent event) {
        VSnapshot snapshot = snapshots.get(0);
        List<SnapshotEntry> snapshotEntries = snapshot.getEntries();
        List<SnapshotItem> snapshotItems = snapshotEntries
                .stream()
                .map(snapshotEntry -> SnapshotItem.builder().value(snapshotEntry.getValue()).configPv(snapshotEntry.getConfigPv()).readbackValue(snapshotEntry.getReadbackValue()).build())
                .collect(Collectors.toList());
        try {
            Node savedSnapshot = saveAndRestoreService.saveSnapshot(config.getUniqueId(), snapshotItems, snapshotNameProperty.get(), snapshotCommentProperty.get());
            snapshotDataDirty = false;
            loadSnapshot(savedSnapshot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<VSnapshot> getAllSnapshots() {
        synchronized (snapshots) {
            return new ArrayList<>(snapshots);
        }
    }

    private List<SnapshotTableEntry> loadSnapshotInternal(VSnapshot snapshotData){
        dispose(false);
        List<SnapshotTableEntry> ret = setSnapshotInternal(snapshotData);
        pvsForDisposal.values().forEach(p -> p.dispose());
        pvsForDisposal.clear();
        return ret;
    }

    private List<SnapshotTableEntry> setSnapshotInternal(VSnapshot snapshotData) {

        List<SnapshotEntry> entries = snapshotData.getEntries();
        synchronized (snapshots) {
            snapshots.add(snapshotData);
        }
        UI_EXECUTOR.execute(() -> snapshotRestorableProperty.set(snapshotData.getSnapshot().isPresent()));
        //snapshotRestorableProperty.set(snapshotData.getSnapshot().isPresent());
        String name;
        SnapshotTableEntry e;
        SnapshotEntry entry;
        for (int i = 0; i < entries.size(); i++) {
            entry = entries.get(i);
            e = new SnapshotTableEntry();
            name = entry.getPVName();
            e.idProperty().setValue(i + 1);
            e.pvNameProperty().setValue(name);
            e.setConfigPv(entry.getConfigPv());
            e.selectedProperty().setValue(entry.isSelected());
            e.setSnapshotValue(entry.getValue(), 0);
            e.setStoredReadbackValue(entry.getReadbackValue(), 0);
            items.put(name, e);
            readbacks.put(name, entry.getReadbackName());
            e.readbackNameProperty().set(entry.getReadbackName());
            e.readOnlyProperty().set(entry.isReadOnly());
        }
        connectPVs();
        UI_EXECUTOR.execute(() -> snapshotSaveableProperty.set(snapshotData.isSaveable()));
        //snapshotSaveableProperty.set(snapshotData.isSaveable());
        //updateThresholds();
        UI_EXECUTOR.execute(() -> baseSnapshotProperty.set(snapshotData));
        //baseSnapshotProperty.set(snapshotData);
        //List<SnapshotTableEntry> ret = filter(items.values(), filter);
        pvsForDisposal.values().forEach(p -> p.dispose());
        pvsForDisposal.clear();
        return new ArrayList<>(items.values());
    }

    private List<SnapshotTableEntry> addSnapshot(VSnapshot data) {
        int numberOfSnapshots = getNumberOfSnapshots();
        if (numberOfSnapshots == 0) {
            return setSnapshotInternal(data); // do not dispose of anything
        } else if (numberOfSnapshots == 1 && !getSnapshot(0).isSaveable() && !getSnapshot(0).isSaved()) {
            return setSnapshotInternal(data);
        } else {
            List<SnapshotEntry> entries = data.getEntries();
            String n;
            SnapshotTableEntry e;
            List<SnapshotTableEntry> withoutValue = new ArrayList<>(items.values());
            SnapshotEntry entry;
            for (int i = 0; i < entries.size(); i++) {
                entry = entries.get(i);
                n = entry.getPVName();
                e = items.get(n);
                if (e == null) {
                    e = new SnapshotTableEntry();
                    e.idProperty().setValue(items.size() + i + 1);
                    e.pvNameProperty().setValue(n);
                    e.setConfigPv(entry.getConfigPv());
                    items.put(n, e);
                    readbacks.put(n, entry.getReadbackName());
                    e.readbackNameProperty().set(entry.getReadbackName());
                }
                e.setSnapshotValue(entry.getValue(), numberOfSnapshots);
                e.setStoredReadbackValue(entry.getReadbackValue(), numberOfSnapshots);
                e.readOnlyProperty().set(entry.isReadOnly());
                withoutValue.remove(e);
            }
            for (SnapshotTableEntry te : withoutValue) {
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

            return new ArrayList<>(items.values());
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

    /**
     * Resume live updates from pvs
     */
    public void resume() {
        if (suspend.decrementAndGet() == 0) {
            pvs.values().forEach(e -> e.resume());
            this.throttle.trigger();
        }
    }

    /**
     * Suspend all live updates from the PVs
     */
    public void suspend() {
        suspend.incrementAndGet();
    }

    private void connectPVs() {
        suspend();
        try {
            items.values().forEach(snapshotTableEntry -> {
                SnapshotTableEntryPvProxy snapshotTableEntryPvProxy = pvs.get(snapshotTableEntry);
                if (snapshotTableEntryPvProxy == null) {
                    String pvName = snapshotTableEntry.pvNameProperty().get();
                    snapshotTableEntryPvProxy = pvsForDisposal.remove(pvName);
                    if (snapshotTableEntryPvProxy != null) {
                        pvs.put(snapshotTableEntry.pvNameProperty().get(), snapshotTableEntryPvProxy);
                    }
                    else{
                        pvs.put(pvName, new SnapshotTableEntryPvProxy(snapshotTableEntry));
                    }
                }
            });
        } finally {
            resume();
        }
    }

    private class SnapshotTableEntryPvProxy {
        final String pvName;
        final String readbackPvName;
        final String provider;
        PV<VType, Object> pv;
        PVReader<VType> reader;
        PVReader<VType> readbackReader;
        PVEvent.Type writeStatus = PVEvent.Type.WRITE_FAILED;
        //PVWriter<Object> writer;
        volatile VType pvValue = VDisconnectedData.INSTANCE;
        volatile VType readbackValue = VDisconnectedData.INSTANCE;
        //PVWriterListener writeListener;
        PVListener<VType, Object> listener;
        PVConfiguration pvConfiguration;

        SnapshotTableEntryPvProxy(SnapshotTableEntry snapshotTableEntry) {
            this.pvName = snapshotTableEntry.pvNameProperty().get();
            this.readbackPvName = snapshotTableEntry.readbackNameProperty().get();
            this.provider = snapshotTableEntry.getConfigPv().getProvider().name();

            this.listener = (event, p) -> {
                if(event.getType().contains(PVEvent.Type.VALUE)){
                    synchronized (SnapshotController.this) {
                        if (suspend.get() > 0) {
                            return;
                        }
                    }
                    this.pvValue = p.getValue(); //p.isConnected() ? p.getValue() : VDisconnectedData.INSTANCE;
                    throttle.trigger();
                }
                else if(event.getType().contains(PVEvent.Type.WRITE_SUCCEEDED)){
                    writeStatus = PVEvent.Type.WRITE_SUCCEEDED;
                }
//                else if(event.getType().contains(PVEvent.Type.WRITE_FAILED)){
//                    writeStatus = PVEvent.Type.WRITE_FAILED;
//                }
            };



            this.pvConfiguration = GPClient.readAndWrite(GPClient.channel(provider + "://" + pvName));

            this.pv = pvConfiguration
                    .addListener(listener)
                    .maxRate(Duration.ofMillis(500))
                    .start();

            this.pvValue = pv.getValue();

            //this.reader = GPClient.read(provider + "://" + this.pvName)
//            this.reader = GPClient.read("sim://noise(-5,5,0.01)")
//                    .addReadListener((event, p) -> {
//                        synchronized (SnapshotController.this) {
//                            if (suspend.get() > 0) {
//                                return;
//                            }
//                        }
//                        this.pvValue = p.isConnected() ? p.getValue() : VDisconnectedData.INSTANCE;
//                        throttle.trigger();
//                    })
//                    .maxRate(Duration.ofMillis(500))
//                    .start();
//            this.pvValue = reader.getValue();

            if (readbackPvName != null && !readbackPvName.isEmpty()) {
                this.readbackReader = GPClient.read(provider + "://" + this.readbackPvName)
                        .addReadListener((event, p) -> {
                            synchronized (SnapshotController.this) {
                                if (suspend.get() > 0) {
                                    return;
                                }
                            }
                            this.readbackValue = p.isConnected() ? p.getValue() : VDisconnectedData.INSTANCE;
                            if (showLiveReadbacks) {
                                throttle.trigger();
                            }
                        }).start();
                this.readbackValue = readbackReader.getValue();
            }
        }

        public PVEvent.Type getWriteStatus(){
            return writeStatus;
        }

        public void restoreDone(){
            pvConfiguration.addListener(listener);
            writeStatus = PVEvent.Type.WRITE_FAILED;
        }

        void resume() {
            if (reader != null) {
                this.pvValue = reader.getValue();
            }
//            if(pv != null){
//                this.pvValue = pv.getValue();
//            }
            if (readbackReader != null) {
                readbackValue = readbackReader.getValue();
            }
        }

        void dispose() {
            if (reader != null && !reader.isClosed()) {
                reader.close();
            }
//            if(pv != null && !pv.isClosed()){
//                pv.close();
//            }
            if (readbackReader != null && !readbackReader.isClosed()) {
                readbackReader.close();
            }
        }

        @Override
        public String toString(){
            return "PV";
        }
    }

    public boolean handleSnapshotTabClosed(){
        if(snapshotDataDirty){
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Close tab?");
            alert.setContentText("Snapshot data is not saved. Do you wish to continue?");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get().equals(ButtonType.OK)) {
                return true;
            }
            else{
                cleanupResources();
                return false;
            }
        }
        return false;
    }

    private void cleanupResources(){
        dispose(true);
    }

    /**
     * Dispose of all allocated resources, except PVs. If <code>closePVs</code> is true the pvs are disposed of,
     * otherwise they are only marked for disposal. It is expected that the caller to this method later checks the PVs
     * and disposes of those that have not been unmarked.
     *
     * @param closePVs true if the PVs should be closed and map cleared or false if they should only be marked for
     *            disposal
     */
    private void dispose(boolean closePVs) {
        synchronized (snapshots) {
            // synchronise, because this method can be called from the UI thread by Eclipse, when the editor is closing
            if (closePVs) {
                pvsForDisposal.values().forEach(e -> e.dispose());
                pvsForDisposal.clear();
                pvs.values().forEach(e -> e.dispose());
                pvs.clear();
            } else {
                pvs.forEach((e, p) -> pvsForDisposal.put(e, p));
                pvs.clear();
            }
            items.clear();
            snapshots.clear();
        }
    }

    /**
     * Returns true if items with equal live and snapshot values are currently hidden or false is shown.
     *
     * @return true if equal items are hidd en or false otherwise.
     */
    public boolean isHideEqualItems() {
        return hideEqualItems;
    }
}