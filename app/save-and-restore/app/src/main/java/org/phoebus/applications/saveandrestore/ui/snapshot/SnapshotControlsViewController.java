/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 */

package org.phoebus.applications.saveandrestore.ui.snapshot;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.util.converter.DoubleStringConverter;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.Preferences;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.event.SaveAndRestoreEventReceiver;
import org.phoebus.applications.saveandrestore.ui.RestoreMode;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreBaseController;
import org.phoebus.applications.saveandrestore.ui.SnapshotMode;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.util.time.TimestampFormats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class SnapshotControlsViewController extends SaveAndRestoreBaseController {

    private static final Logger logger = Logger.getLogger(SnapshotControlsViewController.class.getName());
    private SnapshotController snapshotController;

    @FXML
    protected TextField snapshotName;

    @FXML
    protected TextArea snapshotComment;

    @FXML
    protected Button saveSnapshotButton;

    @SuppressWarnings("unused")
    @FXML
    private Label createdBy;

    @SuppressWarnings("unused")
    @FXML
    private Label createdDate;

    @SuppressWarnings("unused")
    @FXML
    private Label snapshotLastModifiedLabel;

    @SuppressWarnings("unused")
    @FXML
    private Button takeSnapshotButton;

    @SuppressWarnings("unused")
    @FXML
    private Button restoreButton;

    @SuppressWarnings("unused")
    @FXML
    private Spinner<Double> thresholdSpinner;

    @SuppressWarnings("unused")
    @FXML
    private Spinner<Double> multiplierSpinner;

    @SuppressWarnings("unused")
    @FXML
    private TextField filterTextField;

    @SuppressWarnings("unused")
    @FXML
    private CheckBox preserveSelectionCheckBox;

    @FXML
    protected ToggleButton showLiveReadbackButton;

    @SuppressWarnings("unused")
    @FXML
    private ToggleButton showDeltaPercentageButton;

    @FXML
    protected ToggleButton hideEqualItemsButton;

    @SuppressWarnings("unused")
    @FXML
    private ToolBar filterToolbar;

    @SuppressWarnings("unused")
    @FXML
    private CheckBox logAction;

    @SuppressWarnings("unused")
    @FXML
    private RadioButton readPVs;

    @SuppressWarnings("unused")
    @FXML
    private RadioButton readFromArchiver;

    @SuppressWarnings("unused")
    @FXML
    private RadioButton restoreFromClient;

    @SuppressWarnings("unused")
    @FXML
    private RadioButton restoreFromService;

    private List<List<Pattern>> regexPatterns = new ArrayList<>();

    protected final SimpleStringProperty snapshotNameProperty = new SimpleStringProperty();
    private final SimpleStringProperty snapshotCommentProperty = new SimpleStringProperty();
    private final SimpleStringProperty createdByTextProperty = new SimpleStringProperty();
    private final SimpleStringProperty createdDateTextProperty = new SimpleStringProperty();
    private final SimpleStringProperty lastModifiedDateTextProperty = new SimpleStringProperty();
    private final SimpleBooleanProperty snapshotRestorableProperty = new SimpleBooleanProperty(false);

    protected final SimpleBooleanProperty showLiveReadbackProperty = new SimpleBooleanProperty(false);

    protected final SimpleBooleanProperty showDeltaPercentageProperty = new SimpleBooleanProperty(false);

    private final SimpleBooleanProperty hideEqualItemsProperty = new SimpleBooleanProperty(false);

    private final SimpleBooleanProperty logActionProperty = new SimpleBooleanProperty(false);

    /**
     * Property used to indicate if there is new snapshot data to save, or if snapshot metadata
     * has changed (e.g. user wants to rename the snapshot or update the comment).
     */
    protected final SimpleBooleanProperty snapshotDataDirty = new SimpleBooleanProperty(false);

    private final SimpleObjectProperty<Node> snapshotNodeProperty = new SimpleObjectProperty<>();

    private final SimpleObjectProperty<SnapshotMode> snapshotModeProperty = new SimpleObjectProperty<>(SnapshotMode.READ_PVS);

    private final SimpleObjectProperty<RestoreMode> restoreModeProperty = new SimpleObjectProperty<>(RestoreMode.CLIENT_RESTORE);

    public void setSnapshotController(SnapshotController snapshotController) {
        this.snapshotController = snapshotController;
    }

    @FXML
    public void initialize() {

        snapshotName.textProperty().bindBidirectional(snapshotNameProperty);
        snapshotName.disableProperty().bind(userIdentity.isNull());
        snapshotComment.textProperty().bindBidirectional(snapshotCommentProperty);
        snapshotComment.disableProperty().bind(userIdentity.isNull());
        createdBy.textProperty().bind(createdByTextProperty);
        createdDate.textProperty().bind(createdDateTextProperty);
        snapshotLastModifiedLabel.textProperty().bind(lastModifiedDateTextProperty);

        takeSnapshotButton.disableProperty().bind(Bindings.createBooleanBinding(() ->
                (snapshotNodeProperty.isNotNull().get() && !snapshotNodeProperty.get().getNodeType().equals(NodeType.SNAPSHOT)) ||
                        userIdentity.isNull().get(), snapshotNodeProperty, userIdentity));

        snapshotNameProperty.addListener(((observableValue, oldValue, newValue) ->
                snapshotDataDirty.set(newValue != null && (snapshotNodeProperty.isNull().get() || snapshotNodeProperty.isNotNull().get() && !newValue.equals(snapshotNodeProperty.get().getName())))));
        snapshotCommentProperty.addListener(((observableValue, oldValue, newValue) ->
                snapshotDataDirty.set(newValue != null && (snapshotNodeProperty.isNull().get() || snapshotNodeProperty.isNotNull().get() && !newValue.equals(snapshotNodeProperty.get().getDescription())))));

        saveSnapshotButton.disableProperty().bind(Bindings.createBooleanBinding(() ->
                        // TODO: support save (=update) a composite snapshot from the snapshot view. In the meanwhile, disable save button.
                        snapshotNodeProperty.isNull().get() ||
                        snapshotNodeProperty.get().getNodeType().equals(NodeType.COMPOSITE_SNAPSHOT) ||
                        snapshotDataDirty.not().get() ||
                                snapshotNameProperty.isEmpty().get() ||
                                snapshotCommentProperty.isEmpty().get() ||
                                userIdentity.isNull().get(),
                snapshotDataDirty, snapshotNameProperty, snapshotCommentProperty, userIdentity));

        // Do not show the create log checkbox if no event receivers have been registered
        logAction.visibleProperty().set(ServiceLoader.load(SaveAndRestoreEventReceiver.class).iterator().hasNext());

        restoreButton.disableProperty().bind(Bindings.createBooleanBinding(() ->
                snapshotRestorableProperty.not().get() ||
                        userIdentity.isNull().get(), snapshotRestorableProperty, userIdentity));

        SpinnerValueFactory<Double> thresholdSpinnerValueFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 999.0, 0.0, 0.01);
        thresholdSpinnerValueFactory.setConverter(new DoubleStringConverter());
        thresholdSpinner.setValueFactory(thresholdSpinnerValueFactory);
        thresholdSpinner.getEditor().setAlignment(Pos.CENTER_RIGHT);
        thresholdSpinner.getEditor().getStylesheets().add(getClass().getResource("/save-and-restore-style.css").toExternalForm());
        thresholdSpinner.getEditor().textProperty().addListener((a, o, n) -> parseAndUpdateThreshold(n));

        SpinnerValueFactory<Double> multiplierSpinnerValueFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 999.0, 1.0, 0.01);
        multiplierSpinnerValueFactory.setConverter(new DoubleStringConverter());
        multiplierSpinner.setValueFactory(multiplierSpinnerValueFactory);
        multiplierSpinner.getEditor().setAlignment(Pos.CENTER_RIGHT);
        multiplierSpinner.getEditor().getStylesheets().add(getClass().getResource("/save-and-restore-style.css").toExternalForm());
        multiplierSpinner.getEditor().textProperty()
                .addListener((a, o, n) -> {
                    multiplierSpinner.getEditor().getStyleClass().remove("input-error");
                    multiplierSpinner.setTooltip(null);
                    snapshotRestorableProperty.set(true);
                    double parsedNumber;
                    try {
                        parsedNumber = Double.parseDouble(n.trim());
                        snapshotController.updateSnapshotValues(parsedNumber);
                        //parseAndUpdateThreshold(thresholdSpinner.getEditor().getText().trim());
                    } catch (NumberFormatException e) {
                        multiplierSpinner.getEditor().getStyleClass().add("input-error");
                        multiplierSpinner.setTooltip(new Tooltip(Messages.toolTipMultiplierSpinner));
                        snapshotRestorableProperty.set(false);
                    }
                });

        DockPane.getActiveDockPane().addEventFilter(KeyEvent.ANY, event -> {
            if (event.isShortcutDown() && event.getCode() == KeyCode.F) {
                if (!filterTextField.isFocused()) {
                    filterTextField.requestFocus();
                }
            }
        });

        String filterShortcutName = (new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN)).getDisplayText();
        filterTextField.setPromptText("* for all matching and , as or separator, & as and separator. Start with / for regex. All if empty. (" + filterShortcutName + ")");

        filterTextField.addEventHandler(KeyEvent.ANY, event -> {
            String filterText = filterTextField.getText().trim();

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

            snapshotController.applyFilter(filterText, preserveSelectionCheckBox.isSelected(), regexPatterns);
        });

        preserveSelectionCheckBox.selectedProperty().addListener((observableValue, aBoolean, isSelected) -> snapshotController.applyPreserveSelection(isSelected));

        showLiveReadbackButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/show_live_readback_column.png"))));
        showLiveReadbackProperty.bind(showLiveReadbackButton.selectedProperty());
        showLiveReadbackButton.selectedProperty()
                .addListener((a, o, n) ->
                        snapshotController.showReadback(showLiveReadbackProperty.get()));

        ImageView showHideDeltaPercentageButtonImageView = new ImageView(new Image(getClass().getResourceAsStream("/icons/show_hide_delta_percentage.png")));
        showHideDeltaPercentageButtonImageView.setFitWidth(16);
        showHideDeltaPercentageButtonImageView.setFitHeight(16);

        showDeltaPercentageButton.setGraphic(showHideDeltaPercentageButtonImageView);
        showDeltaPercentageProperty.bind(showDeltaPercentageButton.selectedProperty());
        showDeltaPercentageButton.selectedProperty()
                .addListener((a, o, n) ->
                        snapshotController.showDeltaPercentage(n));

        hideEqualItemsButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/hide_show_equal_items.png"))));
        hideEqualItemsProperty.bind(hideEqualItemsButton.selectedProperty());
        hideEqualItemsButton.selectedProperty()
                .addListener((a, o, n) ->
                        snapshotController.applyHideEqualItems());

        snapshotNodeProperty.addListener((ob, old, node) -> {
            if (node != null) {
                updateUi(node);
            }
        });

        logAction.selectedProperty().bindBidirectional(logActionProperty);

        readPVs.setUserData(SnapshotMode.READ_PVS);
        readFromArchiver.setUserData(SnapshotMode.FROM_ARCHIVER);

        String snapshotModeString = Preferences.default_snapshot_mode;
        if (snapshotModeString == null || snapshotModeString.isEmpty()) {
            snapshotModeProperty.set(SnapshotMode.READ_PVS);
        } else {
            try {
                snapshotModeProperty.set(SnapshotMode.valueOf(snapshotModeString));
            } catch (IllegalArgumentException e) {
                logger.log(Level.WARNING, "Unknown snapshot mode \"" + snapshotModeString + "\", defaulting to " + SnapshotMode.READ_PVS);
                snapshotModeProperty.set(SnapshotMode.READ_PVS);
            }
        }

        ToggleGroup toggleGroup = new ToggleGroup();
        toggleGroup.getToggles().addAll(readPVs, readFromArchiver);
        toggleGroup.selectToggle(toggleGroup.getToggles().stream()
                .filter(t -> t.getUserData().equals(snapshotModeProperty.get())).findFirst().get());
        toggleGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            snapshotModeProperty.set((SnapshotMode) n.getUserData());
        });

        restoreFromClient.setUserData(RestoreMode.CLIENT_RESTORE);
        restoreFromService.setUserData(RestoreMode.SERVICE_RESTORE);

        String restoreModeString = Preferences.default_restore_mode;
        if (restoreModeString == null || restoreModeString.isEmpty()) {
            restoreModeProperty.set(RestoreMode.CLIENT_RESTORE);
        } else {
            try {
                restoreModeProperty.set(RestoreMode.valueOf(restoreModeString));
            } catch (IllegalArgumentException e) {
                logger.log(Level.WARNING, "Unknown restore mode \"" + restoreModeString + "\", defaulting to " + RestoreMode.CLIENT_RESTORE);
                restoreModeProperty.set(RestoreMode.CLIENT_RESTORE);
            }
        }

        ToggleGroup restoreToggleGroup = new ToggleGroup();
        restoreToggleGroup.getToggles().addAll(restoreFromClient, restoreFromService);
        restoreToggleGroup.selectToggle(restoreToggleGroup.getToggles().stream()
                .filter(t -> t.getUserData().equals(restoreModeProperty.get())).findFirst().get());
        restoreToggleGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            restoreModeProperty.set((RestoreMode) n.getUserData());
        });
    }

    public SimpleStringProperty getSnapshotNameProperty() {
        return snapshotNameProperty;
    }

    public SimpleStringProperty getSnapshotCommentProperty() {
        return snapshotCommentProperty;
    }

    @SuppressWarnings("unused")
    @FXML
    public void takeSnapshot() {
        snapshotDataDirty.set(true);
        snapshotRestorableProperty.set(false);
        snapshotController.takeSnapshot();
    }

    @SuppressWarnings("unused")
    @FXML
    public void saveSnapshot(ActionEvent event) {
        snapshotController.saveSnapshot(event);
    }

    @FXML
    public void restore(ActionEvent event) {
        snapshotController.restore(event);
    }

    public SimpleBooleanProperty getSnapshotRestorableProperty() {
        return snapshotRestorableProperty;
    }

    public void setSnapshotNode(Node node) {
        snapshotNodeProperty.set(node);
        updateUi(node);
    }

    private void updateUi(Node node){
        Platform.runLater(() -> {
            snapshotNameProperty.set(node.getName());
            snapshotCommentProperty.set(node.getDescription());
            createdDateTextProperty.set(node.getCreated() != null ? TimestampFormats.SECONDS_FORMAT.format(node.getCreated().toInstant()) : null);
            lastModifiedDateTextProperty.set(node.getLastModified() != null ? TimestampFormats.SECONDS_FORMAT.format(node.getLastModified().toInstant()) : null);
            createdByTextProperty.set(node.getUserName());
            filterToolbar.disableProperty().set(node.getName() == null);
        });
    }

    private void parseAndUpdateThreshold(String value) {
        thresholdSpinner.getEditor().getStyleClass().remove("input-error");
        thresholdSpinner.setTooltip(null);

        double parsedNumber;
        try {
            parsedNumber = Double.parseDouble(value.trim());
            snapshotController.updateThreshold(parsedNumber);
        } catch (Exception e) {
            thresholdSpinner.getEditor().getStyleClass().add("input-error");
            thresholdSpinner.setTooltip(new Tooltip(Messages.toolTipMultiplierSpinner));
        }
    }

    public SimpleBooleanProperty getHideEqualItemsProperty() {
        return hideEqualItemsProperty;
    }

    public void setFilterToolbarDisabled(boolean disabled) {
        filterToolbar.disableProperty().set(disabled);
    }

    public void setSnapshotRestorableProperty(boolean restorable) {
        snapshotRestorableProperty.set(restorable);
    }

    public SnapshotMode getDefaultSnapshotMode() {
        return snapshotModeProperty.get();
    }
    public RestoreMode getRestoreMode() {
        return restoreModeProperty.get();
    }

    public boolean logAction() {
        return logActionProperty.get();
    }
}
