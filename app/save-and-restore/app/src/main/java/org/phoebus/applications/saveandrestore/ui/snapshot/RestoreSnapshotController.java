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
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.util.converter.DoubleStringConverter;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.SafeMultiply;
import org.phoebus.applications.saveandrestore.common.*;
import org.phoebus.applications.saveandrestore.model.*;
import org.phoebus.applications.saveandrestore.model.event.SaveAndRestoreEventReceiver;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.nls.NLS;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.util.time.TimestampFormats;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RestoreSnapshotController extends SnapshotController {

    @FXML
    private BorderPane borderPane;

    private final SimpleStringProperty snapshotUniqueIdProperty = new SimpleStringProperty();

    /**
     * A {@link Node} of type {@link NodeType#SNAPSHOT} or {@link NodeType#COMPOSITE_SNAPSHOT}.
     */
    private Node snapshotNode;

    private final List<String> restoreFailedPVNames = new ArrayList<>();

    private final SimpleBooleanProperty nodeMetaDataDirty = new SimpleBooleanProperty(false);

    private boolean compareViewInitialized = false;

    private CompareSnapshotsTableViewController compareSnapshotsTableViewController;

    public RestoreSnapshotController(SnapshotTab snapshotTab) {
        super(snapshotTab);
    }

    @FXML
    public void initialize() {

        ImageView showHideDeltaPercentageButtonImageView = new ImageView(new Image(getClass().getResourceAsStream("/icons/show_hide_delta_percentage.png")));
        showHideDeltaPercentageButtonImageView.setFitWidth(16);
        showHideDeltaPercentageButtonImageView.setFitHeight(16);

        // Locate registered SaveAndRestoreEventReceivers
        eventReceivers = ServiceLoader.load(SaveAndRestoreEventReceiver.class);

        progressIndicator.visibleProperty().bind(disabledUi);
        disabledUi.addListener((observable, oldValue, newValue) -> borderPane.setDisable(newValue));

    }

    /**
     * Loads a snapshot {@link Node} for restore.
     *
     * @param snapshotNode An existing {@link Node} of type {@link NodeType#SNAPSHOT}
     */
    public void loadSnapshot(Node snapshotNode) {
        this.snapshotNode = snapshotNode;
        snapshotControlsViewController.setSnapshotNode(snapshotNode);
        snapshotUniqueIdProperty.set(snapshotNode.getUniqueId());
        snapshotTab.updateTabTitle(snapshotNode.getName());
        snapshotTab.setId(snapshotNode.getUniqueId());
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

    public void addSnapshot(Node snapshotNode) {
        if(!compareViewInitialized){
            initializeCompareView();
        }

        getSnapshotDataAndAdd(snapshotNode);
    }

    private void initializeCompareView(){
        ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
        FXMLLoader loader = new FXMLLoader();
        loader.setResources(resourceBundle);
        loader.setLocation(SnapshotTab.class.getResource("CompareSnapshotsTableView.fxml"));

        try {
            javafx.scene.Node tableView = loader.load();
            compareSnapshotsTableViewController = loader.getController();
            compareSnapshotsTableViewController.setSnapshotController(this);
            compareSnapshotsTableViewController.setSelectionColumnVisible(true);
            borderPane.setCenter(tableView);
            compareViewInitialized = true;
        } catch (IOException e) {
            ExceptionDetailsErrorDialog.openError("Error",
                    "Failed to load compare snapshots view", e);
        }
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

    @FXML
    public void restore(ActionEvent actionEvent) {
        new Thread(() -> {
            restoreFailedPVNames.clear();
            Snapshot snapshot = snapshots.get(0);
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
            javafx.scene.Node jfxNode = (javafx.scene.Node) actionEvent.getSource();
            String userData = (String) jfxNode.getUserData();
            if (userData.equalsIgnoreCase("true")) {
                eventReceivers.forEach(r -> r.snapshotRestored(snapshot.getSnapshotNode(), restoreFailedPVNames, this::showLoggingError));
            }
        }).start();
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
                    tableEntry.readbackPvNameProperty().set(entry.getConfigPv().getReadbackPvName());
                }
                tableEntry.setSnapshotValue(entry.getValue(), numberOfSnapshots);
                tableEntry.setStoredReadbackValue(entry.getReadbackValue());
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
            compareSnapshotsTableViewController.updateTable(new ArrayList<>(tableEntryItems.values()), snapshots, false, false);
            return new ArrayList<>(tableEntryItems.values());
        }
    }


    /**
     * Returns the snapshot stored under the given index.
     *
     * @param index the index of the snapshot to return
     * @return the snapshot under the given index (0 for the base snapshot and 1 or more for the compared ones)
     */
    public Snapshot getSnapshot(int index) {
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

    //@Override
    //public void updateLoadedSnapshot(int snapshotIndex, TableEntry rowValue, VType newValue) {
    //    super.updateLoadedSnapshot(snapshotIndex, rowValue, newValue);
    //    parseAndUpdateThreshold(thresholdSpinner.getEditor().getText().trim());
    //}



    public boolean handleSnapshotTabClosed() {
        if (nodeMetaDataDirty.get()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(Messages.closeTabPrompt);
            alert.setContentText(Messages.promptCloseSnapshotTabContent);
            DialogHelper.positionDialog(alert, snapshotTab.getTabPane(), -150, -150);
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
        pvs.values().forEach(SaveAndRestorePV::dispose);
        pvs.clear();
        tableEntryItems.clear();
        snapshots.clear();
    }

    private void showLoggingError(String cause) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(Messages.loggingFailedTitle);
            alert.setHeaderText(Messages.loggingFailed);
            alert.setContentText(cause != null ? cause : Messages.loggingFailedCauseUnknown);
            DialogHelper.positionDialog(alert, snapshotTab.getTabPane(), -150, -150);
            alert.showAndWait();
        });
    }

    @FXML
    @SuppressWarnings("unused")
    public void takeSnapshot() {
        dispose();
        snapshotTab.newSnapshot(configurationNode);
    }

    @FXML
    @SuppressWarnings("unused")
    @Override
    public void saveSnapshot(ActionEvent actionEvent) {
        disabledUi.set(true);
        JobManager.schedule("Save Snapshot", monitor -> {
            snapshotControlsViewController.setSnapshotNode(snapshotNode);
            try {
                snapshotNode = SaveAndRestoreService.getInstance().updateNode(snapshotNode);
                // Snapshot successfully saved, clean up and request tab to switch to restore view.
                nodeMetaDataDirty.set(false);
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

    public Node getSnapshotNode(){
        return snapshotNode;
    }
}