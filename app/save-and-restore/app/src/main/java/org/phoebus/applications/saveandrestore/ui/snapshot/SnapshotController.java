/**
 * Copyright (C) 2024 European Spallation Source ERIC.
 */
package org.phoebus.applications.saveandrestore.ui.snapshot;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.converter.DoubleStringConverter;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
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
import org.phoebus.applications.saveandrestore.SafeMultiply;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.ConfigurationData;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.RestoreResult;
import org.phoebus.applications.saveandrestore.model.Snapshot;
import org.phoebus.applications.saveandrestore.model.SnapshotData;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.model.event.SaveAndRestoreEventReceiver;
import org.phoebus.applications.saveandrestore.model.websocket.MessageType;
import org.phoebus.applications.saveandrestore.model.websocket.SaveAndRestoreWebSocketMessage;
import org.phoebus.applications.saveandrestore.ui.ImageRepository;
import org.phoebus.applications.saveandrestore.ui.RestoreMode;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreBaseController;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.SnapshotMode;
import org.phoebus.applications.saveandrestore.ui.VTypePair;
import org.phoebus.applications.saveandrestore.ui.WebSocketMessageHandler;
import org.phoebus.core.types.TimeStampedProcessVariable;
import org.phoebus.core.vtypes.VDisconnectedData;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.saveandrestore.util.SnapshotUtil;
import org.phoebus.saveandrestore.util.Threshold;
import org.phoebus.saveandrestore.util.Utilities;
import org.phoebus.saveandrestore.util.VNoData;
import org.phoebus.ui.application.ContextMenuHelper;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.javafx.FocusUtil;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.time.DateTimePane;
import org.phoebus.util.time.TimestampFormats;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This controller is for the use case of loading a configuration {@link Node} to take a new snapshot.
 * Once the snapshot has been saved, this controller calls the {@link SnapshotTab} API to load
 * the view associated with restore actions.
 */
public class SnapshotController extends SaveAndRestoreBaseController implements WebSocketMessageHandler {


    @SuppressWarnings("unused")
    @FXML
    private BorderPane borderPane;
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

    @SuppressWarnings("unused")
    @FXML
    private TooltipTableColumn<String> pvNameColumn;
    @SuppressWarnings("unused")
    @FXML
    private TooltipTableColumn<Instant> timeColumn;
    @SuppressWarnings("unused")
    @FXML
    private TooltipTableColumn<VType> storedReadbackColumn;
    @SuppressWarnings("unused")
    @FXML
    private TooltipTableColumn<VType> liveReadbackColumn;
    @SuppressWarnings("unused")
    @FXML
    private TableColumn<TableEntry, ?> readbackColumn;
    @SuppressWarnings("unused")
    @FXML
    private TableColumn<TableEntry, ActionResult> actionResultColumn;

    @SuppressWarnings("unused")
    @FXML
    private TableColumn<TableEntry, ActionResult> actionResultReadbackColumn;

    @FXML
    protected TableView<TableEntry> snapshotTableView;

    @FXML
    protected TableColumn<TableEntry, Boolean> selectedColumn;

    @FXML
    protected TooltipTableColumn<Integer> idColumn;

    @FXML
    protected TooltipTableColumn<VType> storedValueColumn;

    @FXML
    protected TooltipTableColumn<VType> liveValueColumn;

    @FXML
    protected TableColumn<TableEntry, VTypePair> deltaColumn;

    @FXML
    protected TableColumn<TableEntry, VTypePair> deltaReadbackColumn;

    @FXML
    protected TableColumn<TableEntry, ?> statusColumn;

    @FXML
    protected TableColumn<TableEntry, ?> severityColumn;

    @FXML
    protected TableColumn<TableEntry, ?> valueColumn;

    @FXML
    protected TableColumn<TableEntry, ?> firstDividerColumn;

    @FXML
    protected TableColumn<TableEntry, ?> compareColumn;

    @FXML
    protected TableColumn<TableEntry, ?> baseSnapshotColumn;

    @FXML
    private TableColumn<TableEntry, AlarmSeverity> storedSeverityColumn;

    @FXML
    private TableColumn<TableEntry, AlarmSeverity> liveSeverityColumn;

    @FXML
    protected TooltipTableColumn<VType> baseSnapshotValueColumn;

    @FXML
    protected TableColumn<TableEntry, VTypePair> baseSnapshotDeltaColumn;

    @FXML
    protected VBox progressIndicator;

    protected final Map<String, SaveAndRestorePV> pvs = new HashMap<>();

    protected Node configurationNode;

    public static final Logger LOGGER = Logger.getLogger(SnapshotController.class.getName());

    protected ServiceLoader<SaveAndRestoreEventReceiver> eventReceivers;

    private final SimpleStringProperty tabTitleProperty = new SimpleStringProperty();
    private final SimpleStringProperty tabIdProperty = new SimpleStringProperty();

    private final SimpleObjectProperty<Image> tabGraphicImageProperty = new SimpleObjectProperty<>();
    private List<List<Pattern>> regexPatterns = new ArrayList<>();

    protected final SimpleStringProperty snapshotNameProperty = new SimpleStringProperty();
    private final SimpleStringProperty snapshotCommentProperty = new SimpleStringProperty();
    private final SimpleStringProperty createdByTextProperty = new SimpleStringProperty();
    private final SimpleStringProperty createdDateTextProperty = new SimpleStringProperty();
    private final SimpleStringProperty lastModifiedDateTextProperty = new SimpleStringProperty();
    private final SimpleBooleanProperty snapshotRestorableProperty = new SimpleBooleanProperty(false);
    protected final SimpleBooleanProperty showDeltaPercentageProperty = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty hideEqualItemsProperty = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty logActionProperty = new SimpleBooleanProperty(false);
    /**
     * Property used to indicate if there is new snapshot data to save, or if snapshot metadata
     * has changed (e.g. user wants to rename the snapshot or update the comment).
     */
    protected final SimpleBooleanProperty snapshotDataDirty = new SimpleBooleanProperty(false);
    private final SimpleObjectProperty<SnapshotMode> snapshotModeProperty = new SimpleObjectProperty<>(SnapshotMode.READ_PVS);
    private final SimpleObjectProperty<RestoreMode> restoreModeProperty = new SimpleObjectProperty<>(RestoreMode.CLIENT_RESTORE);

    /**
     * List of snapshots used managed in this controller. Index 0 is always the base snapshot,
     * all others are snapshots added in the compare use-case.
     */
    protected List<Snapshot> snapshots = new ArrayList<>();

    private final SimpleBooleanProperty selectionInverted = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty showReadbacks = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty showDeltaPercentage = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty compareViewEnabled = new SimpleBooleanProperty(false);
    /**
     * Used to control the disable state on the Take Snapshot button.
     */
    private final SimpleObjectProperty<NodeType> nodeTypeProperty = new SimpleObjectProperty<>(NodeType.SNAPSHOT);

    private SnapshotUtil snapshotUtil;
    /**
     * Used to disable portions of the UI when long-lasting operations are in progress, e.g.
     * take snapshot or save snapshot.
     */
    protected final SimpleBooleanProperty disabledUi = new SimpleBooleanProperty(false);

    /**
     * The final {@link Snapshot} object holding the data for this controller. When user performs operations (loading,
     * taking and saving snapshots), the fields of this object are updated accordingly.
     */
    private final Snapshot snapshot = new Snapshot();


    /**
     * {@link Map} of {@link TableEntry} items corresponding to the snapshot data, i.e.
     * one per PV as defined in the snapshot's configuration. This map is used to
     * populate the {@link TableView}, but other parameters (e.g. hideEqualItems) may
     * determine which elements in the {@link Map} to actually represent.
     */
    protected final Map<String, TableEntry> tableEntryItems = new LinkedHashMap<>();

    public SnapshotController(SnapshotTab snapshotTab) {
        snapshotTab.textProperty().bind(tabTitleProperty);
        snapshotTab.idProperty().bind(tabIdProperty);
        ImageView imageView = new ImageView();
        imageView.imageProperty().bind(tabGraphicImageProperty);
        snapshotTab.setGraphic(imageView);
    }


    @FXML
    public void initialize() {

        Node snapshotNode = Node.builder().nodeType(NodeType.SNAPSHOT).build();
        snapshot.setSnapshotNode(snapshotNode);

        // Locate registered SaveAndRestoreEventReceivers
        eventReceivers = ServiceLoader.load(SaveAndRestoreEventReceiver.class);
        progressIndicator.visibleProperty().bind(disabledUi);
        disabledUi.addListener((observable, oldValue, newValue) -> borderPane.setDisable(newValue));

        snapshotDataDirty.addListener((obs, o, n) -> {
            if (n && !tabTitleProperty.get().startsWith("* ")) {
                Platform.runLater(() -> tabTitleProperty.setValue("* " + tabTitleProperty.get()));
            } else if (!n && tabTitleProperty.get().startsWith("* ")) {
                Platform.runLater(() -> tabTitleProperty.setValue(tabTitleProperty.get().substring(2)));
            }
        });

        snapshotName.textProperty().bindBidirectional(snapshotNameProperty);
        snapshotName.disableProperty().bind(userIdentity.isNull());
        snapshotComment.textProperty().bindBidirectional(snapshotCommentProperty);
        snapshotComment.disableProperty().bind(userIdentity.isNull());
        createdBy.textProperty().bind(createdByTextProperty);
        createdDate.textProperty().bind(createdDateTextProperty);
        snapshotLastModifiedLabel.textProperty().bind(lastModifiedDateTextProperty);

        takeSnapshotButton.disableProperty().bind(Bindings.createBooleanBinding(() ->
                !nodeTypeProperty.get().equals(NodeType.SNAPSHOT) ||
                        userIdentity.isNull().get(), nodeTypeProperty, userIdentity));

        snapshotNameProperty.addListener(((observableValue, oldValue, newValue) ->
                snapshotDataDirty.set(newValue != null && !newValue.equals(snapshot.getSnapshotNode().getName()))));
        snapshotCommentProperty.addListener(((observableValue, oldValue, newValue) ->
                snapshotDataDirty.set(newValue != null && !newValue.equals(snapshot.getSnapshotNode().getDescription()))));

        saveSnapshotButton.disableProperty().bind(Bindings.createBooleanBinding(() ->
                        // TODO: support save (=update) a composite snapshot from the snapshot view. In the meanwhile, disable save button.
                        snapshotDataDirty.not().get() ||
                                snapshotNameProperty.isEmpty().get() ||
                                (!Preferences.allow_empty_descriptions && snapshotCommentProperty.isEmpty().get()) ||
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
        thresholdSpinner.getEditor().textProperty().addListener((a, o, n) -> parseAndUpdateThreshold(n));

        SpinnerValueFactory<Double> multiplierSpinnerValueFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 999.0, 1.0, 0.01);
        multiplierSpinnerValueFactory.setConverter(new DoubleStringConverter());
        multiplierSpinner.setValueFactory(multiplierSpinnerValueFactory);
        multiplierSpinner.getEditor().setAlignment(Pos.CENTER_RIGHT);
        multiplierSpinner.getEditor().textProperty()
                .addListener((a, o, n) -> {
                    multiplierSpinner.getEditor().getStyleClass().remove("input-error");
                    multiplierSpinner.setTooltip(null);
                    snapshotRestorableProperty.set(true);
                    try {
                        updateSnapshotValues(Double.parseDouble(n.trim()));
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

            applyFilter(filterText, preserveSelectionCheckBox.isSelected(), regexPatterns);
        });

        preserveSelectionCheckBox.selectedProperty()
                .addListener((observableValue, aBoolean, isSelected) -> applyPreserveSelection(isSelected));

        showLiveReadbackButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/show_live_readback_column.png"))));
        showLiveReadbackButton.selectedProperty()
                .addListener((a, o, n) -> {
                    this.showReadbacks.set(n);
                    actionResultReadbackColumn.visibleProperty().setValue(actionResultReadbackColumn.getGraphic() != null);
                });

        ImageView showHideDeltaPercentageButtonImageView = new ImageView(new Image(getClass().getResourceAsStream("/icons/show_hide_delta_percentage.png")));
        showHideDeltaPercentageButtonImageView.setFitWidth(16);
        showHideDeltaPercentageButtonImageView.setFitHeight(16);

        showDeltaPercentageButton.setGraphic(showHideDeltaPercentageButtonImageView);
        showDeltaPercentageProperty.bind(showDeltaPercentageButton.selectedProperty());
        showDeltaPercentageButton.selectedProperty()
                .addListener((a, o, n) ->
                        this.showDeltaPercentage.set(n));

        hideEqualItemsButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/hide_show_equal_items.png"))));
        hideEqualItemsProperty.bind(hideEqualItemsButton.selectedProperty());
        hideEqualItemsButton.selectedProperty()
                .addListener((a, o, n) ->
                        hideEqualItems());

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
                LOGGER.log(Level.WARNING, "Unknown snapshot mode \"" + snapshotModeString + "\", defaulting to " + SnapshotMode.READ_PVS);
                snapshotModeProperty.set(SnapshotMode.READ_PVS);
            }
        }

        ToggleGroup toggleGroup = new ToggleGroup();
        toggleGroup.getToggles().addAll(readPVs, readFromArchiver);
        toggleGroup.selectToggle(toggleGroup.getToggles().stream()
                .filter(t -> t.getUserData().equals(snapshotModeProperty.get())).findFirst().get());
        toggleGroup.selectedToggleProperty().addListener((obs, o, n) ->
                snapshotModeProperty.set((SnapshotMode) n.getUserData()));

        restoreFromClient.setUserData(RestoreMode.CLIENT_RESTORE);
        restoreFromService.setUserData(RestoreMode.SERVICE_RESTORE);

        String restoreModeString = Preferences.default_restore_mode;
        if (restoreModeString == null || restoreModeString.isEmpty()) {
            restoreModeProperty.set(RestoreMode.CLIENT_RESTORE);
        } else {
            try {
                restoreModeProperty.set(RestoreMode.valueOf(restoreModeString));
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.WARNING, "Unknown restore mode \"" + restoreModeString + "\", defaulting to " + RestoreMode.CLIENT_RESTORE);
                restoreModeProperty.set(RestoreMode.CLIENT_RESTORE);
            }
        }

        ToggleGroup restoreToggleGroup = new ToggleGroup();
        restoreToggleGroup.getToggles().addAll(restoreFromClient, restoreFromService);
        restoreToggleGroup.selectToggle(restoreToggleGroup.getToggles().stream()
                .filter(t -> t.getUserData().equals(restoreModeProperty.get())).findFirst().get());
        restoreToggleGroup.selectedToggleProperty().addListener((obs, o, n) -> restoreModeProperty.set((RestoreMode) n.getUserData()));

        snapshotTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        snapshotTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        snapshotTableView.getStylesheets().add(SnapshotController.class.getResource("/save-and-restore-style.css").toExternalForm());

        snapshotTableView.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() != KeyCode.SPACE) {
                return;
            }

            ObservableList<TableEntry> selections = snapshotTableView.getSelectionModel().getSelectedItems();

            if (selections == null) {
                return;
            }

            selections.stream().filter(item -> !item.readOnlyProperty().get()).forEach(item -> item.selectedProperty().setValue(!item.selectedProperty().get()));

            // Somehow JavaFX TableView handles SPACE pressed event as going into edit mode of the cell.
            // Consuming event prevents NullPointerException.
            event.consume();
        });

        snapshotTableView.setRowFactory(tableView -> new TableRow<>() {
            final ContextMenu contextMenu = new ContextMenu();

            @Override
            protected void updateItem(TableEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setOnContextMenuRequested(null);
                } else {
                    setOnContextMenuRequested(event -> {
                        List<TimeStampedProcessVariable> selectedPVList = snapshotTableView.getSelectionModel().getSelectedItems().stream()
                                .map(tableEntry -> {
                                    Instant time = Instant.now();
                                    if (tableEntry.timestampProperty().getValue() != null) {
                                        time = tableEntry.timestampProperty().getValue();
                                    }
                                    return new TimeStampedProcessVariable(tableEntry.pvNameProperty().get(), time);
                                })
                                .collect(Collectors.toList());

                        contextMenu.hide();
                        contextMenu.getItems().clear();
                        SelectionService.getInstance().setSelection(SaveAndRestoreApplication.NAME, selectedPVList);

                        ContextMenuHelper.addSupportedEntries(FocusUtil.setFocusOn(this), contextMenu);
                        contextMenu.getItems().add(new SeparatorMenuItem());
                        MenuItem toggle = new MenuItem();
                        toggle.setText(item.readOnlyProperty().get() ? Messages.makeRestorable : Messages.makeReadOnly);
                        CheckBox toggleIcon = new CheckBox();
                        toggleIcon.setFocusTraversable(false);
                        toggleIcon.setSelected(item.readOnlyProperty().get());
                        toggle.setGraphic(toggleIcon);
                        toggle.setOnAction(actionEvent -> {
                            item.readOnlyProperty().setValue(!item.readOnlyProperty().get());
                            item.selectedProperty().set(!item.readOnlyProperty().get());
                        });
                        contextMenu.getItems().add(toggle);
                        contextMenu.show(this, event.getScreenX(), event.getScreenY());
                        disableProperty().set(item.readOnlyProperty().get());
                    });
                }
            }
        });

        int width = measureStringWidth("000", Font.font(20));
        idColumn.setPrefWidth(width);
        idColumn.setMinWidth(width);
        idColumn.setCellValueFactory(cell -> {
            int idValue = cell.getValue().idProperty().get();
            idColumn.setPrefWidth(Math.max(idColumn.getWidth(), measureStringWidth(String.valueOf(idValue), Font.font(20))));
            return new ReadOnlyObjectWrapper<>(idValue);
        });

        storedValueColumn.setCellFactory(e -> new VTypeCellEditor<>());
        storedValueColumn.setOnEditCommit(e -> {
            VType updatedValue = e.getRowValue().readOnlyProperty().get() ? e.getOldValue() : e.getNewValue();
            ObjectProperty<VTypePair> value = e.getRowValue().valueProperty();
            value.setValue(new VTypePair(value.get().base, updatedValue, value.get().threshold));
            updateLoadedSnapshot(e.getRowValue(), updatedValue);
        });

        liveValueColumn.setCellFactory(e -> new VTypeCellEditor<>());

        selectedColumn.setCellFactory(column -> new SelectionCell());

        CheckBox selectAllCheckBox = new CheckBox();
        selectAllCheckBox.selectedProperty().set(true);
        selectAllCheckBox.setTooltip(new Tooltip(Messages.includeThisPV));
        selectAllCheckBox.selectedProperty().addListener((ob, o, n) ->
                snapshotTableView.getItems().stream().filter(te -> te.readOnlyProperty().not().get())
                        .forEach(te -> te.selectedProperty().set(n)));
        selectedColumn.setGraphic(selectAllCheckBox);

        selectionInverted.addListener((ob, o, n) -> snapshotTableView.getItems().stream().filter(te -> te.readOnlyProperty().not().get())
                .forEach(te -> te.selectedProperty().set(te.selectedProperty().not().get())));

        MenuItem inverseMI = new MenuItem(Messages.inverseSelection);
        inverseMI.setOnAction(e -> selectionInverted.set(selectionInverted.not().get()));
        final ContextMenu contextMenu = new ContextMenu(inverseMI);
        selectAllCheckBox.setContextMenu(contextMenu);

        timeColumn.setCellFactory(c -> new TimestampTableCell());

        deltaColumn.setCellValueFactory(e -> e.getValue().valueProperty());
        deltaColumn.setCellFactory(e -> new VDeltaCellEditor<>());
        deltaColumn.setComparator((pair1, pair2) -> {
            Utilities.VTypeComparison vtc1 = Utilities.valueToCompareString(pair1.value, pair1.base, pair1.threshold);
            Utilities.VTypeComparison vtc2 = Utilities.valueToCompareString(pair2.value, pair2.base, pair2.threshold);
            return Double.compare(vtc1.getAbsoluteDelta(), vtc2.getAbsoluteDelta());
        });

        deltaReadbackColumn.setCellValueFactory(e -> e.getValue().liveReadbackProperty());
        deltaReadbackColumn.setCellFactory(e -> new VDeltaCellEditor<>());
        deltaReadbackColumn.setComparator((pair1, pair2) -> {
            Utilities.VTypeComparison vtc1 = Utilities.valueToCompareString(pair1.value, pair1.base, pair1.threshold);
            Utilities.VTypeComparison vtc2 = Utilities.valueToCompareString(pair2.value, pair2.base, pair2.threshold);
            return Double.compare(vtc1.getAbsoluteDelta(), vtc2.getAbsoluteDelta());
        });

        showDeltaPercentage.addListener((ob, o, n) -> deltaColumn.setCellFactory(e -> {
            VDeltaCellEditor<VTypePair> vDeltaCellEditor = new VDeltaCellEditor<>();
            vDeltaCellEditor.setShowDeltaPercentage(n);
            return vDeltaCellEditor;
        }));

        actionResultColumn.setCellFactory(c -> new ActionResultTableCell());
        actionResultReadbackColumn.setCellFactory(c -> new ActionResultTableCell());

        liveReadbackColumn.setCellFactory(e -> new VTypeCellEditor<>());
        storedReadbackColumn.setCellFactory(e -> new VTypeCellEditor<>());
        readbackColumn.visibleProperty().bind(showReadbacks);

        liveSeverityColumn.setCellFactory(a -> new AlarmSeverityCell());
        storedSeverityColumn.setCellFactory(a -> new AlarmSeverityCell());

        timeColumn.visibleProperty().bind(compareViewEnabled.not());
        firstDividerColumn.visibleProperty().bind(compareViewEnabled);
        statusColumn.visibleProperty().bind(compareViewEnabled.not());
        severityColumn.visibleProperty().bind(compareViewEnabled.not());
        valueColumn.visibleProperty().bind(compareViewEnabled.not());

        compareViewEnabled.addListener((ob, o, n) -> snapshotTableView.layout());

        snapshotUtil = new SnapshotUtil();

        webSocketClientService.addWebSocketMessageHandler(this);
    }

    private void updateUi() {
        Platform.runLater(() -> {
            Node node = snapshot.getSnapshotNode();
            snapshotNameProperty.set(node.getName());
            snapshotCommentProperty.set(node.getDescription());
            createdDateTextProperty.set(node.getCreated() != null ? TimestampFormats.SECONDS_FORMAT.format(node.getCreated().toInstant()) : null);
            lastModifiedDateTextProperty.set(node.getLastModified() != null ? TimestampFormats.SECONDS_FORMAT.format(node.getLastModified().toInstant()) : null);
            createdByTextProperty.set(node.getUserName());
            filterToolbar.disableProperty().set(node.getName() == null);
        });
        showSnapshotInTable();
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

    /**
     * Loads data from a configuration {@link Node} in order to populate the
     * view with PV items and prepare it to take a snapshot.
     *
     * @param configurationNode A {@link Node} of type {@link org.phoebus.applications.saveandrestore.model.NodeType#CONFIGURATION}
     */
    public void initializeViewForNewSnapshot(Node configurationNode) {
        this.configurationNode = configurationNode;
        tabTitleProperty.setValue(Messages.unnamedSnapshot);
        tabIdProperty.setValue(null);
        JobManager.schedule("Get configuration", monitor -> {
            ConfigurationData configurationData;
            try {
                configurationData = SaveAndRestoreService.getInstance().getConfiguration(configurationNode.getUniqueId());
            } catch (Exception e) {
                ExceptionDetailsErrorDialog.openError(borderPane, Messages.errorGeneric, Messages.errorUnableToRetrieveData, e);
                LOGGER.log(Level.INFO, "Error loading configuration", e);
                return;
            }
            showLiveReadbackButton.setSelected(configurationHasReadbackPvs(configurationData));
            List<ConfigPv> configPvs = configurationData.getPvList();
            SnapshotData snapshotData = new SnapshotData();
            snapshotData.setSnapshotItems(configurationToSnapshotItems(configPvs));
            this.snapshot.setSnapshotData(snapshotData);
            updateUi();
            Platform.runLater(() -> actionResultReadbackColumn.visibleProperty().setValue(false));
            setTabImage(snapshot.getSnapshotNode());
        });
    }

    @FXML
    @SuppressWarnings("unused")
    public void takeSnapshot() {
        disabledUi.set(true);
        resetMetaData();
        takeSnapshot(snapshotModeProperty.get(), snapshot -> {
            disabledUi.set(false);
            if (snapshot.isPresent()) {
                snapshotDataDirty.set(true);
                snapshotRestorableProperty.set(false);
                Platform.runLater(() -> {
                    actionResultColumn.visibleProperty().set(true);
                    actionResultReadbackColumn.visibleProperty().set(true);
                });
                this.snapshot.setSnapshotNode(snapshot.get().getSnapshotNode());
                this.snapshot.setSnapshotData(snapshot.get().getSnapshotData());
                updateUi();
            }
        });
    }

    /**
     * Restores snapshot meta-data properties to indicate that the UI
     * is not showing persisted {@link Snapshot} data.
     */
    private void resetMetaData() {
        tabTitleProperty.setValue(Messages.unnamedSnapshot);
        snapshotNameProperty.setValue(null);
        snapshotCommentProperty.setValue(null);
        createdDateTextProperty.setValue(null);
        lastModifiedDateTextProperty.setValue(null);
        createdByTextProperty.setValue(null);
    }

    @SuppressWarnings("unused")
    public void saveSnapshot(ActionEvent actionEvent) {
        disabledUi.set(true);
        JobManager.schedule("Save Snapshot", monitor -> {
            List<SnapshotItem> snapshotItems = snapshot.getSnapshotData().getSnapshotItems();
            SnapshotData snapshotData = new SnapshotData();
            snapshotData.setSnapshotItems(snapshotItems);
            this.snapshot.setSnapshotData(snapshotData);
            Node snapshotNode =
                    Node.builder()
                            .nodeType(NodeType.SNAPSHOT)
                            .name(snapshotNameProperty.get())
                            .description(snapshotCommentProperty.get())
                            .uniqueId(snapshot.getSnapshotNode().getUniqueId())
                            .build();
            snapshot.setSnapshotNode(snapshotNode);

            try {
                Snapshot _snapshot = SaveAndRestoreService.getInstance().saveSnapshot(configurationNode, snapshot);
                Node _snapshotNode = _snapshot.getSnapshotNode();
                if (logActionProperty.get()) {
                    eventReceivers.forEach(r -> r.snapshotSaved(_snapshotNode, this::showLoggingError));
                }
                snapshotDataDirty.set(false);
                Platform.runLater(() -> loadSnapshot(_snapshotNode));
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

    private List<SnapshotItem> configurationToSnapshotItems(List<ConfigPv> configPvs) {
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


    private void updateLoadedSnapshot(TableEntry rowValue, VType newValue) {
        snapshot.getSnapshotData().getSnapshotItems().stream()
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

    /**
     * Handles clean-up when the associated {@link SnapshotTab} is closed.
     * A check is made if content is dirty, in which case user is prompted to cancel or close anyway.
     *
     * @return <code>true</code> if content is not dirty or user chooses to close anyway,
     * otherwise <code>false</code>.
     */
    @Override
    public boolean handleTabClosed() {
        if (snapshotDataDirty.get()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(Messages.closeTabPrompt);
            alert.setContentText(Messages.promptCloseSnapshotTabContent);
            DialogHelper.positionDialog(alert, borderPane, -150, -150);
            Optional<ButtonType> result = alert.showAndWait();
            return result.isPresent() && result.get().equals(ButtonType.OK);
        } else {
            webSocketClientService.removeWebSocketMessageHandler(this);
            dispose();
            return true;
        }
    }

    /**
     * Releases PV resources.
     */
    private void dispose() {
        pvs.values().forEach(SaveAndRestorePV::dispose);
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
     * Loads a persisted snapshot {@link Node} for restore.
     *
     * @param snapshotNode An existing {@link Node} of type {@link NodeType#SNAPSHOT} or
     *                     of type {@link NodeType#COMPOSITE_SNAPSHOT}.
     */
    public void loadSnapshot(Node snapshotNode) {
        disabledUi.set(true);
        JobManager.schedule("Load snapshot items", monitor -> {
            try {
                Snapshot snapshot = getSnapshotFromService(snapshotNode);
                this.snapshot.setSnapshotNode(snapshot.getSnapshotNode());
                this.snapshot.setSnapshotData(snapshot.getSnapshotData());
                boolean configurationHasReadbacks = configurationHasReadbackPvs(snapshot.getSnapshotData());
                Platform.runLater(() -> {
                    nodeTypeProperty.set(snapshot.getSnapshotNode().getNodeType());
                    showLiveReadbackButton.setSelected(configurationHasReadbacks);
                    actionResultColumn.visibleProperty().setValue(false);
                    actionResultReadbackColumn.visibleProperty().setValue(false);
                    snapshotRestorableProperty.set(true);
                    selectedColumn.visibleProperty().set(true);
                    tabTitleProperty.setValue(snapshotNode.getName());
                    tabIdProperty.setValue(snapshotNode.getUniqueId());
                    snapshotRestorableProperty.set(true);
                    setTabImage(snapshotNode);
                });
                updateUi();
            } finally {
                disabledUi.set(false);
            }
        });
    }

    @FXML
    public void restore() {
        disabledUi.setValue(true);
        restore(restoreModeProperty.get(), restoreResultList -> {
            disabledUi.setValue(false);
            if (logActionProperty.get()) {
                eventReceivers.forEach(r -> r.snapshotRestored(snapshot.getSnapshotNode(), restoreResultList, this::showLoggingError));
            }
        });
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
                Platform.runLater(() -> addSnapshot(snapshot));
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
        takeSnapshotFromArchiver(snapshot -> {
            if (snapshot.isEmpty()) {
                disabledUi.set(false);
                return;
            }
            Platform.runLater(() -> {
                try {
                    addSnapshot(snapshot.get());
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
            } else if (snapshotNode.getNodeType().equals(NodeType.COMPOSITE_SNAPSHOT)) {
                List<SnapshotItem> snapshotItems = SaveAndRestoreService.getInstance().getCompositeSnapshotItems(snapshotNode.getUniqueId());
                snapshotData = new SnapshotData();
                snapshotData.setSnapshotItems(snapshotItems);
            } else {
                throw new RuntimeException("Node type " + snapshotNode.getNodeType() + " not recognized as a valid snapshot type");
            }
        } catch (Exception e) {
            ExceptionDetailsErrorDialog.openError(borderPane, Messages.errorGeneric, Messages.errorUnableToRetrieveData, e);
            LOGGER.log(Level.INFO, "Error loading snapshot", e);
            throw e;
        }
        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotNode(snapshotNode);
        snapshot.setSnapshotData(snapshotData);
        return snapshot;
    }

    @Override
    public void handleWebSocketMessage(SaveAndRestoreWebSocketMessage<?> saveAndRestoreWebSocketMessage) {
        if (saveAndRestoreWebSocketMessage.messageType().equals(MessageType.NODE_UPDATED)) {
            Node node = (Node) saveAndRestoreWebSocketMessage.payload();
            if (tabIdProperty.get() != null && node.getUniqueId().equals(tabIdProperty.get())) {
                loadSnapshot(node);
            }
        }
    }

    /**
     * Set tab image based on node type, and optionally golden tag
     *
     * @param node A snapshot {@link Node}
     */
    private void setTabImage(Node node) {
        if (node.getNodeType().equals(NodeType.COMPOSITE_SNAPSHOT)) {
            tabGraphicImageProperty.set(ImageRepository.COMPOSITE_SNAPSHOT);
        } else {
            boolean golden = node.getTags() != null && node.getTags().stream().anyMatch(t -> t.getName().equals(Tag.GOLDEN));
            if (golden) {
                tabGraphicImageProperty.set(ImageRepository.GOLDEN_SNAPSHOT);
            } else {
                tabGraphicImageProperty.set(ImageRepository.SNAPSHOT);
            }
        }
    }

    /**
     * Takes a snapshot in either of the {@link SnapshotMode}s. Regardless of {@link SnapshotMode}, the
     * work is delegated to the {@link JobManager}, which then calls the <code>consumer</code> function upon completion.
     *
     * @param snapshotMode Specifies how to take the snapshot, see {@link SnapshotMode}
     * @param consumer     The callback method called when a result is available.
     */
    private void takeSnapshot(SnapshotMode snapshotMode, Consumer<Optional<Snapshot>> consumer) {
        switch (snapshotMode) {
            case READ_PVS -> takeSnapshotReadPVs(consumer);
            case FROM_ARCHIVER -> takeSnapshotFromArchiver(consumer);
            default -> throw new IllegalArgumentException("Snapshot mode " + snapshotMode + " not supported");
        }
    }

    private void takeSnapshotFromArchiver(Consumer<Optional<Snapshot>> consumer) {
        DateTimePane dateTimePane = new DateTimePane();
        Dialog<Instant> timePickerDialog = new Dialog<>();
        timePickerDialog.setTitle(Messages.dateTimePickerTitle);
        timePickerDialog.getDialogPane().setContent(dateTimePane);
        timePickerDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        timePickerDialog.setResultConverter(b -> {
            if (b.equals(ButtonType.OK)) {
                return dateTimePane.getInstant();
            }
            return null;
        });
        Optional<Instant> time = timePickerDialog.showAndWait();
        if (time.isEmpty()) { // User cancels date/time picker dialog
            consumer.accept(Optional.empty());
            return;
        }
        JobManager.schedule("Add snapshot from archiver", monitor -> {
            List<SnapshotItem> snapshotItems;
            try {
                snapshotItems = SaveAndRestoreService.getInstance().takeSnapshotFromArchiver(configurationNode.getUniqueId(), time.get());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to query archiver for data", e);
                return;
            }
            showTakeSnapshotResult(snapshotItems);
            Snapshot snapshot = new Snapshot();
            snapshot.setSnapshotNode(Node.builder().name(Messages.archiver).nodeType(NodeType.SNAPSHOT).created(new Date(time.get().toEpochMilli())).build());
            SnapshotData snapshotData = new SnapshotData();
            snapshotData.setUniqueId("anonymous");
            snapshotData.setSnapshotItems(snapshotItems);
            snapshot.setSnapshotData(snapshotData);
            consumer.accept(Optional.of(snapshot));
        });
    }

    private void takeSnapshotReadPVs(Consumer<Optional<Snapshot>> consumer) {
        JobManager.schedule("Take snapshot", monitor -> {
            // Clear snapshots array
            snapshots.clear();
            List<SnapshotItem> snapshotItems;
            try {
                snapshotItems = SaveAndRestoreService.getInstance().takeSnapshot(configurationNode.getUniqueId());
            } catch (Exception e) {
                Platform.runLater(() -> ExceptionDetailsErrorDialog.openError(snapshotTableView, Messages.errorGeneric, Messages.takeSnapshotFailed, e));
                consumer.accept(Optional.empty());
                return;
            }

            // Service can only return nulls for disconnected PVs, but UI expects VDisconnectedData or VNoData
            snapshotItems.forEach(si -> {
                if (si.getValue() == null) {
                    si.setValue(VDisconnectedData.INSTANCE);
                }
                if (si.getReadbackValue() == null) {
                    // If read-back PV name is not set, then VNoData.INSTANCE is the proper value
                    if (si.getConfigPv().getReadbackPvName() == null) {
                        si.setReadbackValue(VNoData.INSTANCE);
                    } else {
                        si.setReadbackValue(VDisconnectedData.INSTANCE);
                    }
                }
            });
            showTakeSnapshotResult(snapshotItems);
            Snapshot snapshot = new Snapshot();
            snapshot.setSnapshotNode(Node.builder().nodeType(NodeType.SNAPSHOT).build());
            SnapshotData snapshotData = new SnapshotData();
            snapshotData.setSnapshotItems(snapshotItems);
            snapshot.setSnapshotData(snapshotData);
            if (!Preferences.default_snapshot_name_date_format.isEmpty()) {
                String dateFormat = Preferences.default_snapshot_name_date_format;
                try {
                    //The format could be not correct
                    SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
                    snapshot.getSnapshotNode().setName(formatter.format(new Date()));
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, dateFormat + " is not a valid date format please check 'default_snapshot_name_date_format' preference ", e);
                }
            }
            consumer.accept(Optional.of(snapshot));
        });
    }

    /**
     * Updates the UI to indicate potential issues reading PVs.
     *
     * @param snapshotItems {@link List} of {@link SnapshotItem} as created in a read operation (by service or from archiver).
     */
    private void showTakeSnapshotResult(List<SnapshotItem> snapshotItems) {
        AtomicBoolean disconnectedPvEncountered = new AtomicBoolean(false);
        AtomicBoolean disconnectedReadbackPvEncountered = new AtomicBoolean(false);
        for (SnapshotItem snapshotItem : snapshotItems) {
            if (snapshotItem.getValue().equals(VDisconnectedData.INSTANCE)) {
                disconnectedPvEncountered.set(true);
                Platform.runLater(() -> {
                    actionResultColumn.setGraphic(new ImageView(ImageCache.getImage(SnapshotController.class, "/icons/error.png")));
                });
                break;
            }
        }
        for (SnapshotItem snapshotItem : snapshotItems) {
            if (snapshotItem.getConfigPv().getReadbackPvName() != null && snapshotItem.getReadbackValue() != null &&
                    snapshotItem.getReadbackValue().equals(VDisconnectedData.INSTANCE)) {
                disconnectedReadbackPvEncountered.set(true);
                Platform.runLater(() -> {
                    actionResultReadbackColumn.setGraphic(new ImageView(ImageCache.getImage(SnapshotController.class, "/icons/error.png")));
                });
                break;
            }
        }
        Platform.runLater(() -> {
            if (!disconnectedPvEncountered.get()) {
                actionResultColumn.setGraphic(new ImageView(ImageCache.getImage(SnapshotController.class, "/icons/ok.png")));
            }
            if (!disconnectedReadbackPvEncountered.get()) {
                actionResultReadbackColumn.setGraphic(new ImageView(ImageCache.getImage(SnapshotController.class, "/icons/ok.png")));
            }
        });
    }

    private void updateThreshold(double threshold) {
        snapshot.getSnapshotData().getSnapshotItems().forEach(item -> {
            VType vtype = item.getValue();
            VNumber diffVType;

            double ratio = threshold / 100;

            TableEntry tableEntry = tableEntryItems.get(item.getConfigPv().getPvName());
            if (tableEntry == null) {
                tableEntry = tableEntryItems.get(item.getConfigPv().getPvName());
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
        });
    }

    /**
     * Updates snapshot set-point values using user-defined multiplier.
     *
     * @param multiplier The (double) factor used to change the snapshot set-points used in restore operation.
     */
    private void updateSnapshotValues(double multiplier) {
        snapshot.getSnapshotData().getSnapshotItems()
                .forEach(item -> {
                    TableEntry tableEntry = tableEntryItems.get(item.getConfigPv().getPvName());
                    VType vtype = tableEntry.storedSnapshotValue().get();
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
                });
    }

    private void applyFilter(String filterText, boolean preserveSelection, List<List<Pattern>> regexPatterns) {
        if (filterText.isEmpty()) {
            List<TableEntry> arrayList = tableEntryItems.values().stream()
                    .peek(item -> {
                        if (!preserveSelection) {
                            if (!item.readOnlyProperty().get()) {
                                item.selectedProperty().set(true);
                            }
                        }
                    }).collect(Collectors.toList());

            Platform.runLater(() -> updateTable(arrayList));
            return;
        }

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

                    if (!preserveSelection) {
                        item.selectedProperty().setValue(matchEither);
                    } else {
                        matchEither |= item.selectedProperty().get();
                    }

                    return matchEither;
                }).collect(Collectors.toList());

        Platform.runLater(() -> updateTable(filteredEntries));
    }

    private void applyPreserveSelection(boolean preserve) {
        if (preserve) {
            boolean allSelected = tableEntryItems.values().stream().allMatch(item -> item.selectedProperty().get());
            if (allSelected) {
                tableEntryItems.values()
                        .forEach(item -> item.selectedProperty().set(false));
            }
        }
    }

    private void hideEqualItems() {
        ArrayList<TableEntry> arrayList = new ArrayList<>(tableEntryItems.values());
        Platform.runLater(() -> updateTable(arrayList));
    }

    /**
     * Restores a snapshot from client or service.
     *
     * @param restoreMode Specifies whether to restore from client or from service
     * @param completion  Callback to handle a potentially empty list of {@link RestoreResult}s.
     */
    private void restore(RestoreMode restoreMode, Consumer<List<RestoreResult>> completion) {
        actionResultColumn.setGraphic(null);
        actionResultReadbackColumn.setGraphic(null);
        JobManager.schedule("Restore snapshot " + snapshot.getSnapshotNode().getName(), monitor -> {
            List<RestoreResult> restoreResultList = null;
            try {
                switch (restoreMode) {
                    case CLIENT_RESTORE ->
                            restoreResultList = snapshotUtil.restore(getSnapshotItemsToRestore(snapshot));
                    case SERVICE_RESTORE ->
                            restoreResultList = SaveAndRestoreService.getInstance().restore(getSnapshotItemsToRestore(snapshot));
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(Messages.errorActionFailed);
                    alert.setContentText(e.getMessage());
                    alert.setHeaderText(Messages.restoreFailed);
                    DialogHelper.positionDialog(alert, snapshotTableView, -150, -150);
                    alert.showAndWait();
                });
                completion.accept(Collections.emptyList());
                return;
            }
            Platform.runLater(() -> actionResultColumn.visibleProperty().setValue(true));
            showRestoreResult(restoreResultList);
            completion.accept(restoreResultList);
        });
    }

    /**
     * Analyzes the result from a restore operation (by service or by client). If any PV is found to be
     * {@link VDisconnectedData#INSTANCE}, then the UI is updated to indicate this.
     *
     * @param restoreResultList Data created through a restore operation.
     */
    private void showRestoreResult(List<RestoreResult> restoreResultList) {
        List<TableEntry> tableEntries = snapshotTableView.getItems();
        AtomicBoolean disconnectedPvEncountered = new AtomicBoolean(false);
        for (TableEntry tableEntry : tableEntries) {
            Optional<RestoreResult> tableEntryOptional = restoreResultList.stream().filter(r -> r.getSnapshotItem().getConfigPv().getPvName().equals(tableEntry.getConfigPv().getPvName())).findFirst();
            if (tableEntryOptional.isPresent()) {
                disconnectedPvEncountered.set(true);
                tableEntry.setActionResult(ActionResult.FAILED);
            } else if (tableEntry.selectedProperty().not().get() || tableEntry.storedSnapshotValue().get().equals(VDisconnectedData.INSTANCE)) {
                tableEntry.setActionResult(ActionResult.PENDING);
            } else {
                tableEntry.setActionResult(ActionResult.OK);
            }
        }
        Platform.runLater(() -> {
            if (!disconnectedPvEncountered.get()) {
                actionResultColumn.setGraphic(new ImageView(ImageCache.getImage(SnapshotController.class, "/icons/ok.png")));
            } else {
                actionResultColumn.setGraphic(new ImageView(ImageCache.getImage(SnapshotController.class, "/icons/error.png")));
            }
        });
    }

    /**
     * Compiles a list of {@link SnapshotItem}s based on the snapshot's PVs (and potential read-only property setting)
     * as well as user's choice to exclude items in the UI.
     *
     * @param snapshot {@link Snapshot} contents.
     * @return A list of {@link SnapshotItem}s to be subject to a restore operation.
     */
    private List<SnapshotItem> getSnapshotItemsToRestore(Snapshot snapshot) {
        List<SnapshotItem> itemsToRestore = new ArrayList<>();

        for (SnapshotItem entry : snapshot.getSnapshotData().getSnapshotItems()) {
            TableEntry e = tableEntryItems.get(entry.getConfigPv().getPvName());

            boolean restorable = e.selectedProperty().get() &&
                    !e.readOnlyProperty().get() &&
                    entry.getValue() != null &&
                    !entry.getValue().equals(VNoData.INSTANCE);

            if (restorable) {
                itemsToRestore.add(entry);
            }
        }
        return itemsToRestore;
    }

    private void addSnapshot(Snapshot snapshot) {
        snapshots.add(snapshot);

        snapshotTableView.getColumns().clear();

        List<TableColumn<TableEntry, ?>> columns = new ArrayList<>();
        columns.add(selectedColumn);
        columns.add(idColumn);
        columns.add(pvNameColumn);
        columns.add(new DividerTableColumn());

        int minWidth = 130;

        if (compareViewEnabled.not().get()) {
            compareViewEnabled.set(true);
            compareColumn = new TableColumn<>(Messages.storedValues);
            compareColumn.getStyleClass().add("snapshot-table-centered");

            String baseSnapshotTimeStamp = snapshots.get(0).getSnapshotNode().getCreated() == null ?
                    "" :
                    " (" + TimestampFormats.SECONDS_FORMAT.format(snapshots.get(0).getSnapshotNode().getCreated().toInstant()) + ")";
            String snapshotName = snapshots.get(0).getSnapshotNode().getName() + baseSnapshotTimeStamp;

            baseSnapshotColumn = new TableColumn<>(snapshotName);
            baseSnapshotColumn.getStyleClass().add("snapshot-table-centered");

            baseSnapshotValueColumn = new TooltipTableColumn<>(Messages.baseSetpoint, Messages.toolTipTableColumnSetpointPVValue, minWidth);
            baseSnapshotValueColumn.setCellValueFactory(new PropertyValueFactory<>("snapshotVal"));
            baseSnapshotValueColumn.setCellFactory(e -> new VTypeCellEditor<>());
            baseSnapshotValueColumn.getStyleClass().add("snapshot-table-left-aligned");

            baseSnapshotValueColumn.setOnEditCommit(e -> {
                VType updatedValue = e.getRowValue().readOnlyProperty().get() ? e.getOldValue() : e.getNewValue();
                ObjectProperty<VTypePair> value = e.getRowValue().valueProperty();
                value.setValue(new VTypePair(value.get().base, updatedValue, value.get().threshold));
                updateLoadedSnapshot(e.getRowValue(), updatedValue);
                for (int i = 1; i < snapshots.size(); i++) {
                    ObjectProperty<VTypePair> compareValue = e.getRowValue().compareValueProperty(i);
                    compareValue.setValue(new VTypePair(updatedValue, compareValue.get().value, compareValue.get().threshold));
                }
            });

            baseSnapshotDeltaColumn = new TooltipTableColumn<>(Messages.tableColumnDeltaValue, "", minWidth);
            baseSnapshotDeltaColumn.setCellValueFactory(e -> e.getValue().valueProperty());
            baseSnapshotDeltaColumn.setCellFactory(e -> new VDeltaCellEditor<>());
            baseSnapshotDeltaColumn.getStyleClass().add("snapshot-table-left-aligned");

            baseSnapshotColumn.getColumns().addAll(baseSnapshotValueColumn, baseSnapshotDeltaColumn, new DividerTableColumn());
        } else {
            compareColumn.getColumns().clear();
        }

        compareColumn.getColumns().add(0, baseSnapshotColumn);

        for (int s = 1; s < snapshots.size(); s++) {
            Node snapshotNode = snapshots.get(s).getSnapshotNode();
            String snapshotName = snapshotNode.getName();

            List<SnapshotItem> entries = snapshot.getSnapshotData().getSnapshotItems();
            String nodeName;
            TableEntry tableEntry;
            // Base snapshot data
            List<TableEntry> baseSnapshotTableEntries = new ArrayList<>(tableEntryItems.values());
            SnapshotItem entry;
            for (int i = 0; i < entries.size(); i++) {
                entry = entries.get(i);
                nodeName = entry.getConfigPv().getPvName();
                tableEntry = tableEntryItems.get(nodeName);
                // tableEntry is null if the added snapshot has more items than the base snapshot.
                if (tableEntry == null) {
                    tableEntry = new TableEntry();
                    tableEntry.idProperty().setValue(tableEntryItems.size() + i + 1);
                    tableEntry.pvNameProperty().setValue(nodeName);
                    tableEntry.setConfigPv(entry.getConfigPv());
                    tableEntryItems.put(nodeName, tableEntry);
                    tableEntry.readbackNameProperty().set(entry.getConfigPv().getReadbackPvName());
                }
                tableEntry.setSnapshotValue(entry.getValue(), snapshots.size());
                tableEntry.setStoredReadbackValue(entry.getReadbackValue(), snapshots.size());
                tableEntry.readOnlyProperty().set(entry.getConfigPv().isReadOnly());
                baseSnapshotTableEntries.remove(tableEntry);
            }
            // If added snapshot has more items than base snapshot, the base snapshot's values for those
            // table rows need to be set to DISCONNECTED.
            for (TableEntry te : baseSnapshotTableEntries) {
                te.setSnapshotValue(VDisconnectedData.INSTANCE, snapshots.size());
            }

            TableColumn<TableEntry, ?> headerColumn = new TableColumn<>(snapshotName + " (" +
                    TimestampFormats.SECONDS_FORMAT.format(snapshotNode.getCreated().toInstant()) + ")");
            headerColumn.getStyleClass().add("snapshot-table-centered");

            TooltipTableColumn<VTypePair> setpointValueCol = new TooltipTableColumn<>(
                    Messages.setpoint,
                    Messages.toolTipTableColumnSetpointPVValue, minWidth);

            setpointValueCol.setCellValueFactory(e -> e.getValue().compareValueProperty(snapshots.size()));
            setpointValueCol.setCellFactory(e -> new VTypeCellEditor<>());
            setpointValueCol.setEditable(false);
            setpointValueCol.setSortable(false);
            setpointValueCol.getStyleClass().add("snapshot-table-left-aligned");

            TooltipTableColumn<VTypePair> deltaCol = new TooltipTableColumn<>(
                    Utilities.DELTA_CHAR + " " + Messages.baseSetpoint,
                    "", minWidth);
            deltaCol.setCellValueFactory(e -> e.getValue().compareValueProperty(snapshots.size()));
            deltaCol.setCellFactory(e -> {
                VDeltaCellEditor<VTypePair> vDeltaCellEditor = new VDeltaCellEditor<>();
                vDeltaCellEditor.setShowDeltaPercentage(showDeltaPercentage.get());
                return vDeltaCellEditor;
            });
            deltaCol.setEditable(false);
            deltaCol.setSortable(false);
            deltaCol.getStyleClass().add("snapshot-table-left-aligned");

            headerColumn.getColumns().addAll(setpointValueCol, deltaCol, new DividerTableColumn());

            compareColumn.getColumns().add(s, headerColumn);
        }

        columns.add(compareColumn);
        columns.add(liveValueColumn);
        columns.add(readbackColumn);

        snapshotTableView.getColumns().addAll(columns);

        connectPVs();
        updateTable(null);
    }

    private void showSnapshotInTable() {
        if (snapshots.isEmpty()) {
            snapshots.add(snapshot);
        } else {
            snapshots.set(0, snapshot);
        }
        AtomicInteger counter = new AtomicInteger(0);
        snapshot.getSnapshotData().getSnapshotItems().forEach(entry -> {
            TableEntry tableEntry = new TableEntry();
            String name = entry.getConfigPv().getPvName();
            tableEntry.idProperty().setValue(counter.incrementAndGet());
            tableEntry.pvNameProperty().setValue(name);
            tableEntry.setConfigPv(entry.getConfigPv());
            tableEntry.setSnapshotValue(entry.getValue(), 0);
            tableEntry.setStoredReadbackValue(entry.getReadbackValue(), 0);
            if (entry.getValue() == null || entry.getValue().equals(VDisconnectedData.INSTANCE)) {
                tableEntry.setActionResult(ActionResult.FAILED);
            } else {
                tableEntry.setActionResult(ActionResult.OK);
            }
            if (entry.getConfigPv().getReadbackPvName() != null) {
                if (entry.getReadbackValue() == null || entry.getReadbackValue().equals(VDisconnectedData.INSTANCE)) {
                    tableEntry.setActionResultReadback(ActionResult.FAILED);
                } else {
                    tableEntry.setActionResultReadback(ActionResult.OK);
                }
            }
            tableEntry.readbackNameProperty().set(entry.getConfigPv().getReadbackPvName());
            tableEntry.readOnlyProperty().set(entry.getConfigPv().isReadOnly());
            tableEntryItems.put(name, tableEntry);
        });

        updateTable(null);
        connectPVs();
    }

    /**
     * Sets new table entries for this table, but do not change the structure of the table.
     *
     * @param entries the entries to set
     */
    private void updateTable(List<TableEntry> entries) {
        final ObservableList<TableEntry> items = snapshotTableView.getItems();
        final boolean notHide = hideEqualItemsProperty.not().get();
        Platform.runLater(() -> {
            items.clear();
            tableEntryItems.forEach((key, value) -> {
                // there is no harm if this is executed more than once, because only one line is allowed for these
                // two properties (see SingleListenerBooleanProperty for more details)
                value.liveStoredEqualProperty().addListener((a, o, n) -> {
                    if (hideEqualItemsProperty.get()) {
                        if (n) {
                            snapshotTableView.getItems().remove(value);
                        } else {
                            snapshotTableView.getItems().add(value);
                        }
                    }
                });
                if (notHide || !value.liveStoredEqualProperty().get()) {
                    items.add(value);
                }
            });
        });
    }

    public Snapshot getSnapshot() {
        return snapshot;
    }

    public Node getConfigurationNode() {
        return configurationNode;
    }

    private int measureStringWidth(String text, Font font) {
        Text mText = new Text(text);
        if (font != null) {
            mText.setFont(font);
        }
        return (int) mText.getLayoutBounds().getWidth();
    }

    /**
     * Attempts to connect to all the PVs of the configuration/snapshot and binds the created {@link SaveAndRestorePV} objects
     * to the {@link TableEntry} objects matched on PV name.
     */
    private void connectPVs() {
        JobManager.schedule("Connect PVs", monitor -> tableEntryItems.values().forEach(e -> {
            SaveAndRestorePV pv = pvs.get(e.getConfigPv().getPvName());
            if (pv == null) {
                pvs.put(e.getConfigPv().getPvName(), new SaveAndRestorePV(e));
            } else {
                pv.setSnapshotTableEntry(e);
            }
        }));
    }

    /**
     * @param configurationData {@link ConfigurationData} obejct of a {@link org.phoebus.applications.saveandrestore.model.Configuration}
     * @return <code>true</code> if any if the {@link ConfigPv} items in {@link ConfigurationData#getPvList()} defines a non-null read-back
     * PV name, otherwise <code>false</code>.
     */
    private boolean configurationHasReadbackPvs(ConfigurationData configurationData) {
        return configurationData.getPvList().stream().anyMatch(cp -> cp.getReadbackPvName() != null);
    }

    /**
     * @param snapshotData {@link SnapshotData} obejct of a {@link org.phoebus.applications.saveandrestore.model.Snapshot}
     * @return <code>true</code> if any if the {@link ConfigPv} items in {@link SnapshotData#getSnapshotItems()} defines a non-null read-back
     * PV name, otherwise <code>false</code>.
     */
    private boolean configurationHasReadbackPvs(SnapshotData snapshotData) {
        return snapshotData.getSnapshotItems().stream().anyMatch(si -> si.getConfigPv().getReadbackPvName() != null);
    }


    private static class SelectionCell extends CheckBoxTableCell<TableEntry, Boolean> {

        @Override
        public void updateItem(final Boolean item, final boolean empty) {
            super.updateItem(item, empty);
            TableRow<TableEntry> tableRow = getTableRow();
            if (tableRow != null && tableRow.getItem() != null && tableRow.getItem().readOnlyProperty().get()) {
                setGraphic(null);
            }
        }
    }

    /**
     * <code>TimestampTableCell</code> is a table cell for rendering the {@link Instant} objects in the table.
     *
     * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
     */
    private static class TimestampTableCell extends TableCell<TableEntry, Instant> {
        @Override
        protected void updateItem(Instant item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
            } else if (item == null) {
                setText("---");
            } else {
                setText(TimestampFormats.SECONDS_FORMAT.format((item)));
            }
        }
    }

    /**
     * {@link TableCell} implementation for the action result columns.
     */
    private class ActionResultTableCell extends TableCell<TableEntry, ActionResult> {
        @Override
        public void updateItem(org.phoebus.applications.saveandrestore.ui.snapshot.ActionResult actionResult, boolean empty) {
            if (empty) {
                setGraphic(null);
            } else {
                switch (actionResult) {
                    case PENDING -> setGraphic(null);
                    case OK ->
                            setGraphic(new ImageView(ImageCache.getImage(SnapshotController.class, "/icons/ok.png")));
                    case FAILED ->
                            setGraphic(new ImageView(ImageCache.getImage(SnapshotController.class, "/icons/error.png")));
                }
            }
        }
    }

    /**
     * {@link TableCell} customized for the alarm severity column such that alarm information is
     * decorated in the same manner as in other applications.
     */
    private static class AlarmSeverityCell extends TableCell<TableEntry, AlarmSeverity> {

        @Override
        public void updateItem(AlarmSeverity alarmSeverity, boolean empty) {
            if (empty) {
                setText(null);
                setStyle(TableCellColors.REGULAR_CELL_STYLE);
            } else if (alarmSeverity == null) {
                setText("---");
                setStyle(TableCellColors.REGULAR_CELL_STYLE);
            } else {
                setText(alarmSeverity.toString());
                switch (alarmSeverity) {
                    case NONE -> setStyle(TableCellColors.ALARM_NONE_STYLE);
                    case UNDEFINED -> setStyle(TableCellColors.ALARM_UNDEFINED_STYLE);
                    case MINOR -> setStyle(TableCellColors.ALARM_MINOR_STYLE);
                    case MAJOR -> setStyle(TableCellColors.ALARM_MAJOR_STYLE);
                    case INVALID -> setStyle(TableCellColors.ALARM_INVALID_STYLE);
                }
            }

        }
    }
}