/**
 * Copyright (C) 2024 European Spallation Source ERIC.
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
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.ConfigurationData;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.RestoreResult;
import org.phoebus.applications.saveandrestore.model.Snapshot;
import org.phoebus.applications.saveandrestore.model.SnapshotData;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.model.event.SaveAndRestoreEventReceiver;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreBaseController;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.SnapshotMode;
import org.phoebus.saveandrestore.util.VNoData;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.security.tokens.ScopedAuthenticationToken;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This controller is for the use case of loading a configuration {@link Node} to take a new snapshot.
 * Once the snapshot has been saved, this controller calls the {@link SnapshotTab} API to load
 * the view associated with restore actions.
 */
public class SnapshotController extends SaveAndRestoreBaseController {


    @SuppressWarnings("unused")
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
                snapshotTableViewController.showSnapshotInTable(n);
            }
        });
    }

    /**
     * Loads data from a configuration {@link Node} in order to populate the
     * view with PV items and prepare it to take a snapshot.
     *
     * @param configurationNode A {@link Node} of type {@link org.phoebus.applications.saveandrestore.model.NodeType#CONFIGURATION}
     */
    public void initializeViewForNewSnapshot(Node configurationNode) {
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
        snapshotTableViewController.takeSnapshot(snapshotControlsViewController.getDefaultSnapshotMode(), snapshot -> {
            disabledUi.set(false);
            if (snapshot.isPresent()) {
                snapshotProperty.set(snapshot.get());
                snapshotTableViewController.showSnapshotInTable(snapshot.get());
            }
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
                if (snapshotControlsViewController.logAction()) {
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
                    //snapshotTableViewController.showSnapshotInTable(snapshot);
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
        snapshotTableViewController.restore(snapshotControlsViewController.getRestoreMode(), snapshotProperty.get(), restoreResultList -> {
            if (snapshotControlsViewController.logAction()) {
                eventReceivers.forEach(r -> r.snapshotRestored(snapshotProperty.get().getSnapshotNode(), restoreResultList, this::showLoggingError));
            }
            if (restoreResultList != null && !restoreResultList.isEmpty()) {
                showAndLogFailedRestoreResult(snapshotProperty.get(), restoreResultList);
            }
            else{
                LOGGER.log(Level.INFO, "Successfully restored snapshot \"" + snapshotProperty.get().getSnapshotNode().getName() + "\"");
            }
        });
    }

    private void showAndLogFailedRestoreResult(Snapshot snapshot, List<RestoreResult> restoreResultList) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(restoreResultList.stream()
                .map(r -> r.getSnapshotItem().getConfigPv().getPvName()).collect(Collectors.joining(System.lineSeparator())));
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(Messages.restoreFailedPVs);
            alert.setContentText(stringBuilder.toString());
            alert.show();
        });
        LOGGER.log(Level.WARNING,
                "Not all PVs could be restored for {0}: {1}. The following errors occurred:\n{2}",
                new Object[]{snapshot.getSnapshotNode().getName(), snapshot.getSnapshotNode(), stringBuilder.toString()});
    }

    /**
     * Adds a snapshot for the sake of comparison with the one currently in view.
     *
     * @param snapshotNode A snapshot {@link Node} selected by user in the {@link javafx.scene.control.TreeView},
     *                     i.e. a snapshot previously persisten in the service.
     */
    public void addSnapshot(Node snapshotNode) {
        disabledUi.set(true);
        JobManager.schedule("Add snapshot", monitor -> {
            try {
                Snapshot snapshot = getSnapshotFromService(snapshotNode);
                Platform.runLater(() -> snapshotTableViewController.addSnapshot(snapshot));
            } catch (Exception e) {
                Logger.getLogger(SnapshotController.class.getName()).log(Level.WARNING, "Failed to add snapshot", e);
            } finally {
                disabledUi.set(false);
            }
        });
    }

    /**
     * Launches a date/time picker and then reads from archiver to construct an in-memory {@link Snapshot} used for comparison.
     */
    public void addSnapshotFromArchiver() {
        disabledUi.set(true);
        snapshotTableViewController.takeSnapshot(SnapshotMode.FROM_ARCHIVER, snapshot -> {
            if (snapshot.isEmpty()) {
                disabledUi.set(false);
                return;
            }
            Platform.runLater(() -> {
                try {
                    snapshotTableViewController.addSnapshot(snapshot.get());
                } finally {
                    disabledUi.set(false);
                }
            });
        });
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