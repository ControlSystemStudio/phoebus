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
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
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
import org.phoebus.applications.saveandrestore.Preferences;
import org.phoebus.applications.saveandrestore.SafeMultiply;
import org.phoebus.applications.saveandrestore.common.Threshold;
import org.phoebus.applications.saveandrestore.common.Utilities;
import org.phoebus.applications.saveandrestore.common.VNoData;
import org.phoebus.applications.saveandrestore.common.VTypePair;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Snapshot;
import org.phoebus.applications.saveandrestore.model.SnapshotData;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.model.event.SaveAndRestoreEventReceiver;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.util.time.TimestampFormats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RestoreSnapshotController extends SnapshotController {

    @FXML
    private Label createdBy;

    @FXML
    private Label createdDate;

    @FXML
    private BorderPane borderPane;

    @FXML
    private Label snapshotLastModifiedLabel;

    @FXML
    private Button takeSnapshotButton;

    @FXML
    private Button restoreButton;

    @FXML
    private ToggleButton showStoredReadbackButton;

    @FXML
    private ToggleButton showTreeTableButton;

    @FXML
    private Spinner<Double> thresholdSpinner;

    @FXML
    private Spinner<Double> multiplierSpinner;

    @FXML
    private TextField filterTextField;

    @FXML
    private CheckBox preserveSelectionCheckBox;

    private SnapshotTreeTable snapshotTreeTable;

    private final SimpleStringProperty createdByTextProperty = new SimpleStringProperty();
    private final SimpleStringProperty createdDateTextProperty = new SimpleStringProperty();

    private final SimpleStringProperty lastModifiedDateTextProperty = new SimpleStringProperty();
    private final SimpleStringProperty snapshotNameProperty = new SimpleStringProperty();
    private final SimpleStringProperty snapshotCommentProperty = new SimpleStringProperty();
    private final SimpleStringProperty snapshotUniqueIdProperty = new SimpleStringProperty();

    private final BooleanProperty snapshotRestorableProperty = new SimpleBooleanProperty(false);

    private final BooleanProperty showStoredReadbackProperty = new SimpleBooleanProperty(false);

    private final boolean showDeltaPercentage = false;

    private final SimpleBooleanProperty showTreeTable = new SimpleBooleanProperty(false);

    /**
     * Property used to indicate if snapshot node has changed with respect to name or comment, or both.
     */
    private final SimpleBooleanProperty nodeDataDirty = new SimpleBooleanProperty(false);

    private List<List<Pattern>> regexPatterns = new ArrayList<>();

    /**
     * A {@link Node} of type {@link NodeType#SNAPSHOT} or {@link NodeType#COMPOSITE_SNAPSHOT}.
     */
    private Node snapshotNode;

    private final List<String> restoreFailedPVNames = new ArrayList<>();

    private final SimpleBooleanProperty nodeMetaDataDirty = new SimpleBooleanProperty(false);

    public RestoreSnapshotController(SnapshotTab snapshotTab) {
        super(snapshotTab);
    }

    @FXML
    public void initialize() {

        snapshotTable = new SnapshotTable(this);

        initializeCommonComponents();

        snapshotName.textProperty().bindBidirectional(snapshotNameProperty);
        snapshotNameProperty.addListener(((observableValue, oldValue, newValue) -> nodeDataDirty.set(newValue != null && !newValue.equals(snapshotNode.getName()))));

        snapshotComment.textProperty().bindBidirectional(snapshotCommentProperty);
        snapshotCommentProperty.addListener(((observableValue, oldValue, newValue) -> nodeDataDirty.set(newValue != null && !newValue.equals(snapshotNode.getDescription()))));

        createdBy.textProperty().bind(createdByTextProperty);
        createdDate.textProperty().bind(createdDateTextProperty);
        snapshotLastModifiedLabel.textProperty().bind(lastModifiedDateTextProperty);

        borderPane.setCenter(snapshotTable);

        if (Preferences.tree_tableview_enable) {
            snapshotTreeTable = new SnapshotTreeTable(this);

            showTreeTable.addListener((observableValue, aBoolean, on) -> {
                if (on) {
                    borderPane.getChildren().remove(snapshotTable);
                    borderPane.setCenter(snapshotTreeTable);
                } else {
                    borderPane.getChildren().remove(snapshotTreeTable);
                    borderPane.setCenter(snapshotTable);
                }
            });
        }

        saveSnapshotButton.disableProperty().bind(Bindings.createBooleanBinding(() ->
                        nodeMetaDataDirty.not().get() ||
                                snapshotNameProperty.isEmpty().get() ||
                                snapshotCommentProperty.isEmpty().get(),
                nodeMetaDataDirty, snapshotNameProperty, snapshotCommentProperty));

        showStoredReadbackButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/show_stored_readback_column.png"))));
        showStoredReadbackButton.selectedProperty().addListener((a, o, n) -> Platform.runLater(() -> {
            ArrayList<TableEntry> arrayList = new ArrayList<>(tableEntryItems.values());
            snapshotTable.updateTable(arrayList, snapshots, showLiveReadbackProperty.get(), showStoredReadbackProperty.get(), showDeltaPercentage);
            if (Preferences.tree_tableview_enable) {
                snapshotTreeTable.updateTable(arrayList, snapshots, showLiveReadbackProperty.get(), showStoredReadbackProperty.get(), showDeltaPercentage);
            }
        }));

        if (Preferences.tree_tableview_enable) {
            showTreeTableButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/show_tree_table_view.png"))));
            showTreeTableButton.selectedProperty().bindBidirectional(showTreeTable);
        } else {
            showTreeTableButton.setVisible(false);
        }

        SpinnerValueFactory<Double> thresholdSpinnerValueFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 999.0, 0.0, 0.01);
        thresholdSpinnerValueFactory.setConverter(new DoubleStringConverter());
        thresholdSpinner.setValueFactory(thresholdSpinnerValueFactory);
        thresholdSpinner.getEditor().setAlignment(Pos.CENTER_RIGHT);
        thresholdSpinner.getEditor().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        thresholdSpinner.getEditor().textProperty().addListener((a, o, n) -> parseAndUpdateThreshold(n));

        SpinnerValueFactory<Double> multiplierSpinnerValueFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 999.0, 1.0, 0.01);
        multiplierSpinnerValueFactory.setConverter(new DoubleStringConverter());
        multiplierSpinner.setValueFactory(multiplierSpinnerValueFactory);
        multiplierSpinner.getEditor().setAlignment(Pos.CENTER_RIGHT);
        multiplierSpinner.getEditor().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        multiplierSpinner.getEditor().textProperty()
                .addListener((a, o, n) -> {
                    multiplierSpinner.getEditor().getStyleClass().remove("input-error");
                    multiplierSpinner.setTooltip(null);
                    snapshotRestorableProperty.set(true);

                    double parsedNumber;
                    try {
                        parsedNumber = Double.parseDouble(n.trim());
                        updateSnapshotValues(parsedNumber);
                    } catch (NumberFormatException e) {
                        multiplierSpinner.getEditor().getStyleClass().add("input-error");
                        multiplierSpinner.setTooltip(new Tooltip(Messages.toolTipMultiplierSpinner));
                        snapshotRestorableProperty.set(false);
                    }
                });

        ImageView showHideDeltaPercentageButtonImageView = new ImageView(new Image(getClass().getResourceAsStream("/icons/show_hide_delta_percentage.png")));
        showHideDeltaPercentageButtonImageView.setFitWidth(16);
        showHideDeltaPercentageButtonImageView.setFitHeight(16);

        restoreButton.disableProperty().bind(snapshotRestorableProperty.not());

        DockPane.getActiveDockPane().addEventFilter(KeyEvent.ANY, event -> {
            if (event.isShortcutDown() && event.getCode() == KeyCode.F) {
                if (!filterTextField.isFocused()) {
                    filterTextField.requestFocus();
                }
            }
        });

        preserveSelectionCheckBox.selectedProperty().addListener((observableValue, aBoolean, isSelected) -> {
            if (isSelected) {
                boolean allSelected = tableEntryItems.values().stream().allMatch(item -> item.selectedProperty().get());

                if (allSelected) {
                    tableEntryItems.values()
                            .forEach(item -> item.selectedProperty().set(false));
                }
            }
        });

        String filterShortcutName = (new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN)).getDisplayText();
        filterTextField.setPromptText("* for all matching and , as or separator, & as and separator. Start with / for regex. All if empty. (" + filterShortcutName + ")");

        filterTextField.addEventHandler(KeyEvent.ANY, event -> {
            String filterText = filterTextField.getText().trim();

            if (filterText.isEmpty()) {
                List<TableEntry> arrayList = tableEntryItems.values().stream()
                        .peek(item -> {
                            if (!preserveSelectionCheckBox.isSelected()) {
                                if (!item.readOnlyProperty().get()) {
                                    item.selectedProperty().set(true);
                                }
                            }
                        }).collect(Collectors.toList());

                Platform.runLater(() -> {
                    snapshotTable.updateTable(arrayList);
                    if (Preferences.tree_tableview_enable) {
                        snapshotTreeTable.updateTable(arrayList);
                    }
                });

                return;
            }

            List<String> filters = Arrays.asList(filterText.split(","));
            regexPatterns = filters.stream()
                    .map(item -> {
                        if (item.startsWith("/")) {
                            return List.of(Pattern.compile(item.substring(1, item.length() - 1).trim()));
                        } else {
                            return Arrays.stream(item.split("&"))
                                    .map(andItem -> andItem.replaceAll("\\*", ".*"))
                                    .map(andItem -> Pattern.compile(andItem.trim()))
                                    .collect(Collectors.toList());
                        }
                    }).collect(Collectors.toList());

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

                        if (!preserveSelectionCheckBox.isSelected()) {
                            item.selectedProperty().setValue(matchEither);
                        } else {
                            matchEither |= item.selectedProperty().get();
                        }

                        return matchEither;
                    }).collect(Collectors.toList());

            Platform.runLater(() -> {
                snapshotTable.updateTable(filteredEntries);
                if (Preferences.tree_tableview_enable) {
                    snapshotTreeTable.updateTable(filteredEntries);
                }
            });
        });

        // Locate registered SaveAndRestoreEventReceivers
        eventReceivers = ServiceLoader.load(SaveAndRestoreEventReceiver.class);

        progressIndicator.visibleProperty().bind(disabledUi);
        disabledUi.addListener((observable, oldValue, newValue) -> borderPane.setDisable(newValue));

        snapshotNameProperty.addListener((observable, oldValue, newValue) -> {
            if (oldValue != null && newValue != null && !oldValue.equals(newValue)) {
                nodeMetaDataDirty.set(true);
            }
        });

        snapshotCommentProperty.addListener((observable, oldValue, newValue) -> {
            if (oldValue != null && newValue != null && !oldValue.equals(newValue) && !newValue.equals(snapshotNode.getDescription())) {
                nodeMetaDataDirty.set(true);
            } else {
                nodeMetaDataDirty.set(false);
            }
        });
    }

    /**
     * Loads a snapshot {@link Node} for restore.
     *
     * @param snapshotNode An existing {@link Node} of type {@link NodeType#SNAPSHOT}
     */
    public void loadSnapshot(Node snapshotNode) {
        this.snapshotNode = snapshotNode;
        snapshotNameProperty.set(snapshotNode.getName());
        snapshotUniqueIdProperty.set(snapshotNode.getUniqueId());
        snapshotCommentProperty.set(snapshotNode.getDescription());
        createdDateTextProperty.set(TimestampFormats.SECONDS_FORMAT.format(snapshotNode.getCreated().toInstant()));
        lastModifiedDateTextProperty.set(TimestampFormats.SECONDS_FORMAT.format(snapshotNode.getLastModified().toInstant()));
        createdByTextProperty.set(snapshotNode.getUserName());
        snapshotTab.updateTabTitle(snapshotNode.getName());
        snapshotTab.setId(snapshotNode.getUniqueId());

        if (this.snapshotNode.getNodeType().equals(NodeType.SNAPSHOT)) {
            if (snapshotNode.getTags() != null && snapshotNode.getTags().stream().anyMatch(t -> t.getName().equals(Tag.GOLDEN))) {
                snapshotTab.setGoldenImage();
            }
            loadSnapshotInternal();
        } else {
            takeSnapshotButton.setDisable(true);
            snapshotName.setEditable(false);
            snapshotComment.setEditable(false);
            snapshotTab.setCompositeSnapshotImage();
            loadCompositeSnapshotInternal(snapshot -> Platform.runLater(() -> {
                List<TableEntry> tableEntries = createTableEntries(snapshot);
                snapshotTable.updateTable(tableEntries, snapshots, false, false, false);
                if (Preferences.tree_tableview_enable) {
                    snapshotTreeTable.updateTable(tableEntries, snapshots, false, false, false);
                }
                snapshotRestorableProperty.set(true);
            }));
        }
    }

    @Override
    public void addSnapshot(Node snapshotNode) {
        // Comparison to same snapshot is pointless...
        if (snapshots.stream().anyMatch(snapshot -> snapshot.getSnapshotNode().getUniqueId().equals(snapshotNode.getUniqueId()))) {
            return;
        }

        super.getSnapshotDataAndAdd(snapshotNode);
    }

    private void loadSnapshotInternal() {
        disabledUi.set(true);
        JobManager.schedule("Load snapshot items", monitor -> {
            SnapshotData snapshotData;
            try {
                this.configurationNode = SaveAndRestoreService.getInstance().getParentNode(snapshotNode.getUniqueId());
                snapshotData = SaveAndRestoreService.getInstance().getSnapshot(snapshotNode.getUniqueId());
            } catch (Exception e) {
                ExceptionDetailsErrorDialog.openError(snapshotTreeTable, Messages.errorGeneric, Messages.errorUnableToRetrieveData, e);
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

                snapshotTable.updateTable(tableEntries, snapshots, false, false, false);
                if (Preferences.tree_tableview_enable) {
                    snapshotTreeTable.updateTable(tableEntries, snapshots, false, false, false);
                }
                snapshotRestorableProperty.set(true);
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
                ExceptionDetailsErrorDialog.openError(snapshotTreeTable, Messages.errorGeneric, Messages.errorUnableToRetrieveData, e);
                return;
            } finally {
                disabledUi.set(false);
            }
            Snapshot snapshot = new Snapshot();
            snapshot.setSnapshotNode(snapshotNode);
            SnapshotData snapshotData = new SnapshotData();
            snapshotData.setSnasphotItems(snapshotItems);
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

    /**
     * Updates table data such that the added snapshot can be rendered for the sake of comparison.
     * Since the added snapshot may have a different number of valued, some care is taken to
     * render sensible values (e.g. DISCONNECTED) for such table rows.
     *
     * @param snapshotNode An existing {@link Node} of type {@link NodeType#SNAPSHOT}
     * @return List of updated {@link TableEntry}s.
     */
    @Override
    protected List<TableEntry> getSnapshotDataAndAdd(Node snapshotNode) {
        List<TableEntry> tableEntries = super.getSnapshotDataAndAdd(snapshotNode);
        snapshotRestorableProperty.set(true);
        return tableEntries;
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

    private void updateThreshold(double threshold) {
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

    private void updateSnapshotValues(double multiplier) {
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

                    tableEntry.snapshotValProperty().set(newVType);

                    ObjectProperty<VTypePair> value = tableEntry.valueProperty();
                    value.setValue(new VTypePair(value.get().base, newVType, value.get().threshold));
                }));

        parseAndUpdateThreshold(thresholdSpinner.getEditor().getText().trim());
    }

    @Override
    public void updateLoadedSnapshot(int snapshotIndex, TableEntry rowValue, VType newValue) {
        super.updateLoadedSnapshot(snapshotIndex, rowValue, newValue);
        parseAndUpdateThreshold(thresholdSpinner.getEditor().getText().trim());
    }

    private void parseAndUpdateThreshold(String value) {
        thresholdSpinner.getEditor().getStyleClass().remove("input-error");
        thresholdSpinner.setTooltip(null);

        double parsedNumber;
        try {
            parsedNumber = Double.parseDouble(value.trim());
            updateThreshold(parsedNumber);
        } catch (Exception e) {
            thresholdSpinner.getEditor().getStyleClass().add("input-error");
            thresholdSpinner.setTooltip(new Tooltip(Messages.toolTipMultiplierSpinner));
        }
    }

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
        pvs.values().forEach(PV::dispose);
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
    private void takeSnapshot() {
        dispose();
        snapshotTab.newSnapshot(configurationNode);
    }

    @FXML
    @SuppressWarnings("unused")
    @Override
    public void saveSnapshot(ActionEvent actionEvent) {
        disabledUi.set(true);
        JobManager.schedule("Save Snapshot", monitor -> {
            snapshotNode.setName(snapshotNameProperty.get());
            snapshotNode.setDescription(snapshotCommentProperty.get());
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
}