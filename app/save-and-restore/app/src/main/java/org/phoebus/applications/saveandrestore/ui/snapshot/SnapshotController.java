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
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.epics.vtype.*;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.*;
import org.phoebus.applications.saveandrestore.model.event.SaveAndRestoreEventReceiver;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreBaseController;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.VNoData;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.security.tokens.ScopedAuthenticationToken;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * This controller is for the use case of loading a configuration {@link Node} to take a new snapshot.
 * Once the snapshot has been saved, this controller calls the {@link SnapshotTab} API to load
 * the view associated with restore actions.
 */
public class SnapshotController extends SaveAndRestoreBaseController {


    @FXML
    private BorderPane borderPane;

    protected final Map<String, SaveAndRestorePV> pvs = new HashMap<>();

    protected Node configurationNode;

    public static final Logger LOGGER = Logger.getLogger(SnapshotController.class.getName());

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

    @FXML
    protected SnapshotTableViewController snapshotTableViewController;

    @FXML
    protected SnapshotControlsViewController snapshotControlsViewController;

    private final SimpleObjectProperty<Snapshot> snapshotProperty = new SimpleObjectProperty<>();

    @FXML
    public void initialize() {

        // Locate registered SaveAndRestoreEventReceivers
        eventReceivers = ServiceLoader.load(SaveAndRestoreEventReceiver.class);
        progressIndicator.visibleProperty().bind(disabledUi);
        disabledUi.addListener((observable, oldValue, newValue) -> borderPane.setDisable(newValue));
        snapshotControlsViewController.setSnapshotController(this);
        snapshotControlsViewController.setFilterToolbarDisabled(true);
        snapshotTableViewController.setSnapshotController(this);

        snapshotProperty.addListener((ob, o, n) -> {
            if (n != null) {
                snapshotControlsViewController.setSnapshotNode(n.getSnapshotNode());
            }
        });
    }

    /**
     * Loads data from a configuration {@link Node} in order to populate the
     * view with PV items and prepare it to take a snapshot.
     *
     * @param configurationNode A {@link Node} of type {@link org.phoebus.applications.saveandrestore.model.NodeType#CONFIGURATION}
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
            snapshot.setSnapshotNode(Node.builder().nodeType(NodeType.SNAPSHOT).build());
            SnapshotData snapshotData = new SnapshotData();
            snapshotData.setSnapshotItems(configurationToSnapshotItems(configPvs));
            snapshot.setSnapshotData(snapshotData);
            snapshotProperty.set(snapshot);
            Platform.runLater(() -> snapshotTableViewController.showSnapshotInTable(snapshot));
        });
    }

    @FXML
    @SuppressWarnings("unused")
    public void takeSnapshot() {
        disabledUi.set(true);
        snapshotTab.setText(Messages.unnamedSnapshot);
        snapshotTableViewController.takeSnapshot(snapshot -> {
            disabledUi.set(false);
            snapshotProperty.set(snapshot);
        });
    }

    @SuppressWarnings("unused")
    public void saveSnapshot(ActionEvent actionEvent) {

        disabledUi.set(true);
        JobManager.schedule("Save Snapshot", monitor -> {
            List<SnapshotItem> snapshotItems = snapshotProperty.get().getSnapshotData().getSnapshotItems();
            SnapshotData snapshotData = new SnapshotData();
            snapshotData.setSnapshotItems(snapshotItems);
            Snapshot snapshot = snapshotProperty.get();
            // Creating new or updating existing (e.g. name change)?
            if (snapshot == null) {
                snapshot = new Snapshot();
                snapshot.setSnapshotNode(Node.builder().nodeType(NodeType.SNAPSHOT)
                        .name(snapshotControlsViewController.getSnapshotNameProperty().get())
                        .description(snapshotControlsViewController.getSnapshotCommentProperty().get()).build());
            } else {
                snapshot.getSnapshotNode().setName(snapshotControlsViewController.getSnapshotNameProperty().get());
                snapshot.getSnapshotNode().setDescription(snapshotControlsViewController.getSnapshotCommentProperty().get());
            }
            snapshot.setSnapshotData(snapshotData);

            try {
                snapshot = SaveAndRestoreService.getInstance().saveSnapshot(configurationNode, snapshot);
                snapshotProperty.set(snapshot);
                Node _snapshotNode = snapshot.getSnapshotNode();
                javafx.scene.Node jfxNode = (javafx.scene.Node) actionEvent.getSource();
                String userData = (String) jfxNode.getUserData();
                if (userData.equalsIgnoreCase("true")) {
                    eventReceivers.forEach(r -> r.snapshotSaved(_snapshotNode, this::showLoggingError));
                }
                snapshotControlsViewController.snapshotDataDirty.set(false);
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

    public Snapshot getSnapshot() {
        return snapshotProperty.get();
    }

    public void updateThreshold(double threshold) {
        snapshotTableViewController.updateThreshold(snapshotProperty.get(), threshold);
    }


    public void updateLoadedSnapshot(TableEntry rowValue, VType newValue) {
        snapshotProperty.get().getSnapshotData().getSnapshotItems().stream()
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

    public boolean handleSnapshotTabClosed() {
        if (snapshotControlsViewController.snapshotDataDirty.get()) {
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
        pvs.values().forEach(SaveAndRestorePV::dispose);
    }

    /**
     * Returns true if items with equal live and snapshot values are currently hidden or false is shown.
     *
     * @return true if equal items are hidden or false otherwise.
     */
    public boolean isHideEqualItems() {
        return snapshotControlsViewController.getHideEqualItemsProperty().get();
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

    public Node getConfigurationNode() {
        return configurationNode;
    }

    public void setSnapshotNameProperty(String name) {
        snapshotControlsViewController.getSnapshotNameProperty().set(name);
    }

    /**
     * Updates snapshot set-point values with user defined multiplier. Note that the stored snapshot
     * is not affected, only the values shown in the snapshot view. The updated value is used when
     * user requests a restore operation.
     *
     * @param multiplier The (double) factor used to change the snapshot set-points used in restore operation.
     */
    public void updateSnapshotValues(double multiplier) {
        snapshotTableViewController.updateSnapshotValues(snapshotProperty.get(), multiplier);
    }

    public void applyFilter(String filterText, boolean preserveSelection, List<List<Pattern>> regexPatterns) {
        snapshotTableViewController.applyFilter(filterText, preserveSelection, regexPatterns);
    }

    public void applyPreserveSelection(boolean preserve) {
        snapshotTableViewController.applyPreserveSelection(preserve);
    }

    public void showReadback(boolean showLiveReadback) {
        snapshotTableViewController.showReadback(showLiveReadback);
    }

    public void showDeltaPercentage(boolean showDeltaPercentage) {
        snapshotTableViewController.setShowDeltaPercentage(showDeltaPercentage);
    }

    public void applyHideEqualItems() {
        snapshotTableViewController.hideEqualItems();
    }


    private void loadSnapshotInternal(Node snapshotNode) {
        Platform.runLater(() -> disabledUi.set(true));
        JobManager.schedule("Load snapshot items", monitor -> {
            try {
                Snapshot snapshot = getSnapshotFromService(snapshotNode);
                snapshotProperty.set(snapshot);
                Platform.runLater(() -> {
                    snapshotTableViewController.showSnapshotInTable(snapshot);
                    snapshotControlsViewController.getSnapshotRestorableProperty().set(true);
                });
            } finally {
                Platform.runLater(() -> disabledUi.set(false));
            }
        });
    }

    /**
     * Loads a snapshot {@link Node} for restore.
     *
     * @param snapshotNode An existing {@link Node} of type {@link NodeType#SNAPSHOT}
     */
    public void loadSnapshot(Node snapshotNode) {
        snapshotControlsViewController.setSnapshotNode(snapshotNode);
        snapshotControlsViewController.setSnapshotRestorableProperty(true);
        snapshotTableViewController.setSelectionColumnVisible(true);

        loadSnapshotInternal(snapshotNode);
    }

    public void restore(ActionEvent actionEvent) {
        snapshotTableViewController.restore(snapshotProperty.get(), restoreFailedPVNames -> {
            javafx.scene.Node jfxNode = (javafx.scene.Node) actionEvent.getSource();
            String userData = (String) jfxNode.getUserData();
            if (userData.equalsIgnoreCase("true")) {
                eventReceivers.forEach(r -> r.snapshotRestored(snapshotProperty.get().getSnapshotNode(), restoreFailedPVNames, this::showLoggingError));
            }
        });
    }

    public void addSnapshot(Node snapshotNode) {
        disabledUi.set(true);
        try {
            Snapshot snapshot = getSnapshotFromService(snapshotNode);
            snapshotTableViewController.addSnapshot(snapshot);
        } catch (Exception e) {
            Logger.getLogger(SnapshotController.class.getName()).log(Level.WARNING, "Failed to add snapshot", e);
        } finally {
            disabledUi.set(false);
        }
    }

    private Snapshot getSnapshotFromService(Node snapshotNode) throws Exception {
        SnapshotData snapshotData;
        try {
            if (snapshotNode.getNodeType().equals(NodeType.SNAPSHOT)) {
                this.configurationNode = SaveAndRestoreService.getInstance().getParentNode(snapshotNode.getUniqueId());
                snapshotData = SaveAndRestoreService.getInstance().getSnapshot(snapshotNode.getUniqueId());
            } else {
                List<SnapshotItem> snapshotItems = SaveAndRestoreService.getInstance().getCompositeSnapshotItems(snapshotNode.getUniqueId());
                snapshotData = new SnapshotData();
                snapshotData.setSnapshotItems(snapshotItems);
            }
        } catch (Exception e) {
            ExceptionDetailsErrorDialog.openError(snapshotTab.getContent(), Messages.errorGeneric, Messages.errorUnableToRetrieveData, e);
            LOGGER.log(Level.INFO, "Error loading snapshot", e);
            throw e;
        }
        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotNode(snapshotNode);
        snapshot.setSnapshotData(snapshotData);
        return snapshot;
    }

    @Override
    public void secureStoreChanged(List<ScopedAuthenticationToken> validTokens) {
        snapshotControlsViewController.secureStoreChanged(validTokens);
    }
}