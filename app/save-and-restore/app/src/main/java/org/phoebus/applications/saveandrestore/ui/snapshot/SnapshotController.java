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
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
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
import javafx.scene.control.TextArea;
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
import org.phoebus.applications.saveandrestore.SafeMultiply;
import org.phoebus.applications.saveandrestore.common.Threshold;
import org.phoebus.applications.saveandrestore.common.Utilities;
import org.phoebus.applications.saveandrestore.common.VDisconnectedData;
import org.phoebus.applications.saveandrestore.common.VNoData;
import org.phoebus.applications.saveandrestore.ui.NodeChangedListener;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.model.SnapshotEntry;
import org.phoebus.applications.saveandrestore.ui.model.VSnapshot;
import org.phoebus.applications.saveandrestore.common.VTypePair;
import org.phoebus.framework.preferences.PreferencesReader;
import org.phoebus.pv.PVFactory;
import org.phoebus.pv.PVPool;
import org.phoebus.ui.docking.DockPane;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SnapshotController implements NodeChangedListener {

    @FXML
    private Label snapshotCommentLabel;

    @FXML
    private TextArea snapshotComment;

    @FXML
    private TextField createdBy;

    @FXML
    private TextField createdDate;

    @FXML
    private BorderPane borderPane;

    @FXML
    private Label snapshotNameLabel;

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
    private ToggleButton showTreeTableButton;

    @FXML
    private Label thresholdLabel;

    @FXML
    private Spinner<Double> thresholdSpinner;

    @FXML
    private Label multiplierLabel;

    @FXML
    private Spinner<Double> multiplierSpinner;

    @FXML
    private ToggleButton showHideDeltaPercentageButton;

    @FXML
    private ToggleButton hideShowEqualItemsButton;

    @FXML
    private TextField filterTextField;

    @FXML
    private CheckBox preserveSelectionCheckBox;

    private SnapshotTable snapshotTable;

    private SnapshotTreeTable snapshotTreeTable;

    /**
     * The {@link SnapshotTab} controlled by this controller.
     */
    private SnapshotTab snapshotTab;

    private SaveAndRestoreService saveAndRestoreService;

    private String defaultEpicsProtocol;

    private final SimpleStringProperty createdByTextProperty = new SimpleStringProperty();
    private final SimpleStringProperty createdDateTextProperty = new SimpleStringProperty();
    private final SimpleStringProperty snapshotNameProperty = new SimpleStringProperty();
    private final SimpleStringProperty snapshotCommentProperty = new SimpleStringProperty();
    private final SimpleStringProperty snapshotUniqueIdProperty = new SimpleStringProperty();

    private final List<VSnapshot> snapshots = new ArrayList<>(10);
    private final Map<String, PV> pvs = new HashMap<>();
    private final Map<String, String> readbacks = new HashMap<>();
    private final Map<String, TableEntry> tableEntryItems = new LinkedHashMap<>();
    private final BooleanProperty snapshotRestorableProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty snapshotSaveableProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty showLiveReadbackProperty = new SimpleBooleanProperty(false);

    private final ObservableSet<Integer> dirtySnapshotEntries = FXCollections.observableSet();
    private String persistentSnapshotName = null;
    private boolean persistentGoldenState = false;

    private final boolean showStoredReadbacks = false;

    private boolean showDeltaPercentage = false;
    private boolean hideEqualItems;

    private final SimpleBooleanProperty showTreeTable = new SimpleBooleanProperty(false);
    private boolean isTreeTableViewEnabled;


    private Node config;

    private static final Executor UI_EXECUTOR = Platform::runLater;

    public static final Logger LOGGER = Logger.getLogger(SnapshotController.class.getName());

    /**
     * The time between updates of dynamic data in the table, in ms
     */
    public static final long TABLE_UPDATE_INTERVAL = 500;

    private List<List<Pattern>> regexPatterns = new ArrayList<>();

    @FXML
    public void initialize() {

        saveAndRestoreService = SaveAndRestoreService.getInstance();

        defaultEpicsProtocol =
                new PreferencesReader(PVFactory.class, "/pv_preferences.properties").get("default");
        isTreeTableViewEnabled = new PreferencesReader(getClass(), "/save_and_restore_preferences.properties").getBoolean("treeTableView.enable");

        snapshotNameLabel.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        snapshotNameLabel.getStyleClass().add("stand-out-mandatory");
        snapshotName.textProperty().bindBidirectional(snapshotNameProperty);
        snapshotName.textProperty().addListener(((observableValue, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                snapshotNameLabel.getStyleClass().add("stand-out-mandatory");
            } else {
                snapshotNameLabel.getStyleClass().remove("stand-out-mandatory");
            }
        }));

        snapshotCommentLabel.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        snapshotCommentLabel.getStyleClass().add("stand-out-mandatory");
        snapshotComment.textProperty().bindBidirectional(snapshotCommentProperty);
        snapshotComment.textProperty().addListener(((observableValue, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                snapshotCommentLabel.getStyleClass().add("stand-out-mandatory");
            } else {
                snapshotCommentLabel.getStyleClass().remove("stand-out-mandatory");
            }
        }));

        createdBy.textProperty().bind(createdByTextProperty);
        createdDate.textProperty().bind(createdDateTextProperty);

        snapshotTable = new SnapshotTable(this);

        borderPane.setCenter(snapshotTable);

        if (isTreeTableViewEnabled) {
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

        saveSnapshotButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
           boolean canSave = snapshotSaveableProperty.get() && (!snapshotNameProperty.isEmpty().get() && !snapshotCommentProperty.isEmpty().get());
            return !canSave;
        }, snapshotSaveableProperty, snapshotNameProperty, snapshotCommentProperty));

        showLiveReadbackButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/show_live_readback_column.png"))));
        showLiveReadbackButton.setTooltip(new Tooltip(Messages.toolTipShowLiveReadback));
        showLiveReadbackProperty.bind(showLiveReadbackButton.selectedProperty());
        showLiveReadbackButton.selectedProperty().addListener((a, o, n) -> {
            UI_EXECUTOR.execute(() -> {
                ArrayList<TableEntry> arrayList = new ArrayList<>(tableEntryItems.values());
                snapshotTable.updateTable(arrayList, snapshots, showLiveReadbackProperty.get(), showStoredReadbacks, showDeltaPercentage);
                if (isTreeTableViewEnabled) {
                    snapshotTreeTable.updateTable(arrayList, snapshots, showLiveReadbackProperty.get(), showStoredReadbacks, showDeltaPercentage);
                }
            });
        });

        showStoredReadbackButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/show_stored_readback_column.png"))));
        showStoredReadbackButton.setTooltip(new Tooltip(Messages.toolTipShowStoredReadback));
        showStoredReadbackButton.selectedProperty().addListener((a, o, n) -> {
            UI_EXECUTOR.execute(() -> {
                ArrayList<TableEntry> arrayList = new ArrayList<>(tableEntryItems.values());
                snapshotTable.updateTable(arrayList, snapshots, showLiveReadbackProperty.get(), showStoredReadbacks, showDeltaPercentage);
                if (isTreeTableViewEnabled) {
                    snapshotTreeTable.updateTable(arrayList, snapshots, showLiveReadbackProperty.get(), showStoredReadbacks, showDeltaPercentage);
                }
            });
        });

        if (isTreeTableViewEnabled) {
            showTreeTableButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/show_tree_table_view.png"))));
            showTreeTableButton.setTooltip(new Tooltip(Messages.toolTipShowTreeTable));
            showTreeTableButton.selectedProperty().bindBidirectional(showTreeTable);
        } else {
            showTreeTableButton.setVisible(false);
        }

        thresholdLabel.setText(Messages.labelThreshold);

        SpinnerValueFactory<Double> thresholdSpinnerValueFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 999.0, 0.0, 0.01);
        thresholdSpinnerValueFactory.setConverter(new DoubleStringConverter());
        thresholdSpinner.setValueFactory(thresholdSpinnerValueFactory);
        thresholdSpinner.getEditor().setAlignment(Pos.CENTER_RIGHT);
        thresholdSpinner.getEditor().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        thresholdSpinner.getEditor().textProperty().addListener((a, o, n) -> parseAndUpdateThreshold(n));

        multiplierLabel.setText(Messages.labelMultiplier);

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

                    Double parsedNumber;
                    try {
                        parsedNumber = Double.parseDouble(n.trim());
                        updateSnapshot(parsedNumber);
                    } catch (NumberFormatException e) {
                        multiplierSpinner.getEditor().getStyleClass().add("input-error");
                        multiplierSpinner.setTooltip(new Tooltip(Messages.toolTipMultiplierSpinner));
                        snapshotRestorableProperty.set(false);
                    }
                });

        ImageView showHideDeltaPercentageButtonImageView = new ImageView(new Image(getClass().getResourceAsStream("/icons/show_hide_delta_percentage.png")));
        showHideDeltaPercentageButtonImageView.setFitWidth(16);
        showHideDeltaPercentageButtonImageView.setFitHeight(16);

        showHideDeltaPercentageButton.setGraphic(showHideDeltaPercentageButtonImageView);
        showHideDeltaPercentageButton.setTooltip(new Tooltip(Messages.toolTipShowHideDeltaPercentageToggleButton));
        showHideDeltaPercentageButton.selectedProperty()
                .addListener((a, o, n) -> {
                    showDeltaPercentage = n;

                    UI_EXECUTOR.execute(() -> {
                        ArrayList<TableEntry> arrayList = new ArrayList<>(tableEntryItems.values());
                        snapshotTable.updateTable(arrayList, snapshots, showLiveReadbackProperty.get(), showStoredReadbacks, showDeltaPercentage);
                        if (isTreeTableViewEnabled) {
                            snapshotTreeTable.updateTable(arrayList, snapshots, showLiveReadbackProperty.get(), showStoredReadbacks, showDeltaPercentage);
                        }
                    });
                });

        hideShowEqualItemsButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/hide_show_equal_items.png"))));
        hideShowEqualItemsButton.setTooltip(new Tooltip(Messages.toolTipShowHideEqualToggleButton));
        hideShowEqualItemsButton.selectedProperty()
                .addListener((a, o, n) -> {
                    hideEqualItems = n;

                    ArrayList<TableEntry> arrayList = new ArrayList<>(tableEntryItems.values());
                    UI_EXECUTOR.execute(() -> snapshotTable.updateTable(arrayList));
                    if (isTreeTableViewEnabled) {
                        UI_EXECUTOR.execute(() -> snapshotTreeTable.updateTable(arrayList));
                    }
                });

        restoreButton.disableProperty().bind(snapshotRestorableProperty.not());

        saveAndRestoreService.addNodeChangeListener(this);

        DockPane.getActiveDockPane().addEventFilter(KeyEvent.ANY, event -> {
            if (event.isShortcutDown() && event.getCode() == KeyCode.F) {
                if (!filterTextField.isFocused()) {
                    filterTextField.requestFocus();
                }
            }
        });

        preserveSelectionCheckBox.selectedProperty().addListener((observableValue, aBoolean, isSelected) -> {
            if (isSelected) {
                boolean allSelected = tableEntryItems.values().stream()
                        .filter(item -> !item.selectedProperty().get())
                        .collect(Collectors.toList()).isEmpty();

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
                        .map(item -> {
                            if (!preserveSelectionCheckBox.isSelected()) {
                                if (!item.readOnlyProperty().get()) {
                                    item.selectedProperty().set(true);
                                }
                            }
                            return item;
                        }).collect(Collectors.toList());

                UI_EXECUTOR.execute(() -> {
                    snapshotTable.updateTable(arrayList);
                    if (isTreeTableViewEnabled) {
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

            UI_EXECUTOR.execute(() -> {
                snapshotTable.updateTable(filteredEntries);
                if (isTreeTableViewEnabled) {
                    snapshotTreeTable.updateTable(filteredEntries);
                }
            });
        });

        dirtySnapshotEntries.addListener((SetChangeListener<Integer>) change -> {
            if (dirtySnapshotEntries.size() == 0) {
                snapshotSaveableProperty.set(false);

                snapshotNameProperty.set(persistentSnapshotName);
                snapshotTab.updateTabTitile(persistentSnapshotName, persistentGoldenState);
            } else {
                snapshotSaveableProperty.set(true);

                snapshotNameProperty.set(persistentSnapshotName + " " + Messages.snapshotModifiedText);
                snapshotTab.updateTabTitile(persistentSnapshotName + " " + Messages.snapshotModifiedText, false);
            }
        });
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
            snapshotTab.setId(snapshot.getUniqueId());

            persistentSnapshotName = snapshot.getName();
            persistentGoldenState = Boolean.parseBoolean(snapshot.getProperty("golden"));
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Error loading snapshot", e);
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
            snapshotTable.updateTable(tableEntries, snapshots, false, false, false);
            if (isTreeTableViewEnabled) {
                snapshotTreeTable.updateTable(tableEntries, snapshots, false, false, false);
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Error adding snapshot", e);
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
                snapshotTable.updateTable(tableEntries, snapshots, false, false, false);
                if (isTreeTableViewEnabled) {
                    snapshotTreeTable.updateTable(tableEntries, snapshots, false, false, false);
                }
            });
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Error loading save set", e);
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

                snapshotTable.updateTable(tableEntries, snapshots, false, false, false);
                if (isTreeTableViewEnabled) {
                    snapshotTreeTable.updateTable(tableEntries, snapshots, false, false, false);
                }
                snapshotRestorableProperty.set(true);

                dirtySnapshotEntries.clear();
                vSnapshot.getEntries().forEach(item -> {
                    item.getValueProperty().addListener((observableValue, vType, newVType) -> {
                        if (!Utilities.areVTypesIdentical(newVType, item.getStoredValue(), false)) {
                            dirtySnapshotEntries.add(item.getConfigPv().getId());
                        } else {
                            dirtySnapshotEntries.remove(item.getConfigPv().getId());
                        }
                    });
                });
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Error loading snapshot", e);
            }
        });
    }

    @FXML
    public void restore(ActionEvent event) {
        new Thread(() -> {
            VSnapshot s = snapshots.get(0);
            CountDownLatch countDownLatch = new CountDownLatch(s.getEntries().size());
            s.getEntries().forEach(e -> pvs.get(getPVKey(e.getPVName(), e.isReadOnly())).setCountDownLatch(countDownLatch));
            try {
                List<String> restoreFailed = new ArrayList<>();
                List<SnapshotEntry> entries = s.getEntries();
                for (SnapshotEntry entry : entries) {
                    TableEntry e = tableEntryItems.get(getPVKey(entry.getPVName(), entry.isReadOnly()));

                    boolean restorable = e.selectedProperty().get() && !e.readOnlyProperty().get();

                    if (restorable) {
                        final PV pv = pvs.get(getPVKey(e.pvNameProperty().get(), e.readOnlyProperty().get()^e.readonlyOverrideProperty().get()));
                        if (entry.getValue() != null) {
                            try {
                                pv.pv.write(Utilities.toRawValue(entry.getValue()));
                            } catch (Exception writeException) {
                                restoreFailed.add(entry.getPVName());
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

                if (restoreFailed.isEmpty()) {
                    LOGGER.log(Level.FINE, "Restored snapshot {0}", s.getSnapshot().get().getName());
                } else {
                    Collections.sort(restoreFailed);
                    StringBuilder sb = new StringBuilder(restoreFailed.size() * 200);
                    restoreFailed.forEach(e -> sb.append(e).append('\n'));
                    LOGGER.log(Level.WARNING,
                            "Not all PVs could be restored for {0}: {1}. The following errors occured:\n{2}",
                            new Object[] { s.getSnapshot().get().getName(), s.getSnapshot().get(), sb.toString() });
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle(Messages.restoreErrorTitle);
                        alert.setContentText(sb.toString());
                        alert.setHeaderText(Messages.restoreErrorContent);
                        alert.showAndWait();
                    });
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

            snapshotTab.setId(null);
            snapshotTab.updateTabTitile(Messages.unnamedSnapshot, false);
            dirtySnapshotEntries.clear();
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
                pv = pvs.get(getPVKey(t.pvNameProperty().get(), t.readOnlyProperty().get()^t.readonlyOverrideProperty().get()));

                // there is no issues with non atomic access to snapshotTreeTableEntryPvProxy.value or snapshotTreeTableEntryPvProxy.readbackValue because the PV is
                // suspended and the value could not change while suspended
                value = pv == null || pv.pvValue == null ? VDisconnectedData.INSTANCE : pv.pvValue;
                String key = getPVKey(name, t.readOnlyProperty().get()^t.readonlyOverrideProperty().get());
                readbackName = readbacks.get(key);
                readbackValue = pv == null || pv.readbackValue == null ? VDisconnectedData.INSTANCE : pv.readbackValue;
                for (VSnapshot s : getAllSnapshots()) {
                    delta = s.getDelta(name);
                    if (delta != null) {
                        break;
                    }
                }

                entries.add(new SnapshotEntry(t.getConfigPv(), value, t.selectedProperty().get(), readbackName, readbackValue,
                        delta, t.readOnlyProperty().get()^t.readonlyOverrideProperty().get()));
            }

            Node snapshot = Node.builder().name(Messages.unnamedSnapshot).nodeType(NodeType.SNAPSHOT).build();

            multiplierSpinner.getEditor().setText("1.0");
            VSnapshot taken = new VSnapshot(snapshot, entries);
            snapshots.clear();
            snapshots.add(taken);
            List<TableEntry> tableEntries = loadSnapshotInternal(taken);
            UI_EXECUTOR.execute(() -> {
                snapshotTable.updateTable(tableEntries, snapshots, showLiveReadbackProperty.get(), false, showDeltaPercentage);
                if (isTreeTableViewEnabled) {
                    snapshotTreeTable.updateTable(tableEntries, snapshots, showLiveReadbackProperty.get(), false, showDeltaPercentage);
                }
                snapshotSaveableProperty.setValue(true);
            });
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Error taking snapshot", e);
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
            String key = getPVKey(name, entry.isReadOnly());
            tableEntryItems.put(key, e);
            readbacks.put(key, entry.getReadbackName());
            e.readbackNameProperty().set(entry.getReadbackName());
            e.readOnlyProperty().set(entry.isReadOnly());
            PV pv = pvs.get(key);
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
                String key = getPVKey(n, entry.isReadOnly());
                e = tableEntryItems.get(key);
                if (e == null) {
                    e = new TableEntry();
                    e.idProperty().setValue(tableEntryItems.size() + i + 1);
                    e.pvNameProperty().setValue(n);
                    e.setConfigPv(entry.getConfigPv());
                    tableEntryItems.put(key, e);
                    readbacks.put(key, entry.getReadbackName());
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
                PV pv = pvs.get(getPVKey(e.getConfigPv().getPvName(), e.getConfigPv().isReadOnly()));
                if (pv == null) {
                    pvs.put(getPVKey(e.getConfigPv().getPvName(), e.getConfigPv().isReadOnly()), new PV(e));
                }
            });
        } finally {
        }
    }

    private void updateThreshold(double threshold) {
        snapshots.forEach(snapshot -> {
            snapshot.getEntries().forEach(item -> {
                VType vtype = item.getValue();
                VType diffVType = null;

                double ratio = threshold/100;

                TableEntry tableEntry = tableEntryItems.get(getPVKey(item.getPVName(), item.isReadOnly()));
                if (tableEntry == null) {
                    tableEntry = tableEntryItems.get(getPVKey(item.getPVName(), !item.isReadOnly()));
                }

                if (!item.getConfigPv().equals(tableEntry.getConfigPv())) {
                    return;
                }

                if (vtype instanceof VNumber) {
                    diffVType = SafeMultiply.multiply((VNumber) vtype, ratio);
                    VNumber vNumber = (VNumber) diffVType;
                    boolean isNegative = vNumber.getValue().doubleValue() < 0;

                    tableEntry.setThreshold(Optional.of(new Threshold<>(isNegative ? SafeMultiply.multiply(vNumber.getValue(), -1.0) : vNumber.getValue())));
                }
            });
        });
    }

    private void updateSnapshot(double multiplier) {
        snapshots.forEach(snapshot -> {
            snapshot.getEntries()
                    .forEach(item -> {
                        TableEntry tableEntry = tableEntryItems.get(getPVKey(item.getPVName(), item.isReadOnly()));

                        if (item.isReadOnly() == !tableEntry.readonlyOverrideProperty().get()) {
                            return;
                        }

                        VType vtype = item.getStoredValue();
                        VType newVType;

                        if (vtype instanceof VNumber) {
                            newVType = SafeMultiply.multiply((VNumber) vtype, multiplier);
                        } else if (vtype instanceof VNumberArray) {
                            newVType = SafeMultiply.multiply((VNumberArray) vtype, multiplier);
                        } else {
                            return;
                        }

                        item.set(newVType, item.isSelected());

                        tableEntry.snapshotValProperty().set(newVType);

                        ObjectProperty<VTypePair> value = tableEntry.valueProperty();
                        value.setValue(new VTypePair(value.get().base, newVType, value.get().threshold));
                    });
        });

        parseAndUpdateThreshold(thresholdSpinner.getEditor().getText().trim());
    }

    public void updateSnapshot(int snapshotIndex, TableEntry rowValue, VType newValue) {
        VSnapshot snapshot = snapshots.get(snapshotIndex);
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
                    item.set(newVType, rowValue.selectedProperty().get());
                    rowValue.snapshotValProperty().set(newVType);
                });

        parseAndUpdateThreshold(thresholdSpinner.getEditor().getText().trim());
    }

    private void parseAndUpdateThreshold(String value) {
        thresholdSpinner.getEditor().getStyleClass().remove("input-error");
        thresholdSpinner.setTooltip(null);

        Double parsedNumber;
        try {
            parsedNumber = Double.parseDouble(value.trim());
            updateThreshold(parsedNumber);
        } catch (Exception e) {
            thresholdSpinner.getEditor().getStyleClass().add("input-error");
            thresholdSpinner.setTooltip(new Tooltip(Messages.toolTipMultiplierSpinner));
        }
    }

    private class PV {
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

        public void countDown() {
            this.countDownLatch.countDown();
        }

        public void setSnapshotTableEntry(TableEntry snapshotTableEntry){
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

            persistentSnapshotName = node.getName();
            persistentGoldenState = Boolean.parseBoolean(node.getProperty("golden"));
        }
    }

    private String getPVKey(String pvName, boolean isReadonly) {
        return pvName + "_" + isReadonly;
    }
}