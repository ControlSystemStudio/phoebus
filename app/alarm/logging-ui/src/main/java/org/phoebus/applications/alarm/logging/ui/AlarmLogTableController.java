package org.phoebus.applications.alarm.logging.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;

import javafx.util.Duration;
import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.logging.ui.AlarmLogTableQueryUtil.Keys;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.model.json.JsonModelReader;
import org.phoebus.applications.alarm.ui.AlarmUI;
import org.phoebus.applications.alarm.ui.table.AlarmInfoRow;
import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.ui.application.ContextMenuHelper;
import org.phoebus.ui.application.TableHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.FocusUtil;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.JFXUtil;
import org.phoebus.util.time.TimeParser;
import org.phoebus.util.time.TimestampFormats;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static org.phoebus.applications.alarm.logging.ui.AlarmLogTableApp.logger;

public class AlarmLogTableController {

    @FXML
    TableView<AlarmLogTableItem> tableView;
    @FXML
    @SuppressWarnings("unused")
    private AdvancedSearchViewController advancedSearchViewController;
    @FXML
    TableColumn<AlarmLogTableItem, String> configCol;
    @FXML
    TableColumn<AlarmLogTableItem, String> pvCol;
    @FXML
    TableColumn<AlarmLogTableItem, String> severityCol;
    @FXML
    TableColumn<AlarmLogTableItem, String> messageCol;
    @FXML
    TableColumn<AlarmLogTableItem, String> valueCol;
    @FXML
    TableColumn<AlarmLogTableItem, String> timeCol;
    @FXML
    TableColumn<AlarmLogTableItem, String> msgTimeCol;
    @FXML
    TableColumn<AlarmLogTableItem, String> deltaTimeCol;
    @FXML
    TableColumn<AlarmLogTableItem, String> currentSeverityCol;
    @FXML
    TableColumn<AlarmLogTableItem, String> currentMessageCol;
    @FXML
    TableColumn<AlarmLogTableItem, String> mode;
    @FXML
    TableColumn<AlarmLogTableItem, String> commandCol;
    @FXML
    TableColumn<AlarmLogTableItem, String> userCol;
    @FXML
    TableColumn<AlarmLogTableItem, String> hostCol;
    @FXML
    Button resize;
    @FXML
    Button search;
    @FXML
    TextField query;
    @FXML
    GridPane ViewSearchPane;
    @FXML
    private TextField configSelection;
    @FXML
    private ToggleButton configDropdownButton;

    @FXML
    TableColumn sortTableCol = null;
    SortType sortColType = null;
    // Search parameters
    ObservableMap<Keys, String> searchParameters = FXCollections.observableHashMap();
    // The search string
    // TODO need to standardize the search string so that it can be easily parsed
    private String searchString = "*";
    private Boolean isNodeTable = true;
    // Result
    private List<AlarmLogTableItem> alarmMessages;

    private Job alarmLogSearchJob;
    private HttpClient httpClient;

    @FXML
    private ProgressIndicator progressIndicator;
    private final SimpleBooleanProperty searchInProgress = new SimpleBooleanProperty(false);
    private final ObservableList<AlarmConfiguration> alarmConfigs = FXCollections.observableArrayList();
    private final SimpleStringProperty selectedConfigsString = new SimpleStringProperty();


    private final ContextMenu configsContextMenu = new ContextMenu();

    public AlarmLogTableController(HttpClient client) {
        setClient(client);
    }

	private final String replaceKey(final String key) {
		String repKey = key;
		if(key == Keys.SEVERITY.getName())
			repKey = "alarm_severity";
		else if(key == Keys.MESSAGE.getName())
			repKey = "alarm_message";
		else if(key == Keys.CURRENTSEVERITY.getName())
			repKey = "pv_severity";
		else if(key == Keys.CURRENTMESSAGE.getName())
			repKey = "pv_message";

		return repKey;
	}

	private String recoverKey(String key) {
		if(key.contains("alarm_severity"))
			key = "severity";
		else if(key.contains("alarm_message"))
			key = "message";
		else if(key.contains("pv_severity"))
			key = "current_severity";
		else if(key.contains("pv_message"))
			key = "current_message";

		return key;
	}

    @FXML
    public void initialize() {
        resize.setText("<");
        tableView.getColumns().clear();

        configCol.setCellValueFactory(
                alarmMessage -> new SimpleStringProperty(alarmMessage.getValue().getConfig()));
        tableView.getColumns().add(configCol);

        pvCol = new TableColumn<>("PV");
        pvCol.setCellValueFactory(alarmMessage -> new SimpleStringProperty(alarmMessage.getValue().getPv()));
        tableView.getColumns().add(pvCol);

        severityCol.setCellValueFactory(
                alarmMessage -> new SimpleStringProperty(alarmMessage.getValue().getSeverity()));
        severityCol.setCellFactory(alarmLogTableTypeStringTableColumn -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setStyle("-fx-text-fill: black;  -fx-background-color: transparent");
                    setText("");
                } else {
                    setText(item);
                    SeverityLevel severityLevel = parseSeverityLevel(item);
                    setStyle("-fx-alignment: center; -fx-border-color: transparent; -fx-border-width: 2 0 2 0; -fx-background-insets: 2 0 2 0; -fx-text-fill: " + JFXUtil.webRGB(AlarmUI.getColor(severityLevel)) + ";  -fx-background-color: " + JFXUtil.webRGB(AlarmUI.getBackgroundColor(severityLevel)));
                }
            }
        });
        tableView.getColumns().add(severityCol);

        messageCol.setCellValueFactory(
                alarmMessage -> new SimpleStringProperty(alarmMessage.getValue().getMessage()));
        tableView.getColumns().add(messageCol);

        valueCol.setCellValueFactory(
                alarmMessage -> {
                    String value = alarmMessage.getValue().getValue(); 
                    return new SimpleStringProperty(value);
                });
        tableView.getColumns().add(valueCol);

        timeCol.setCellValueFactory(
                alarmMessage -> {
                    if (alarmMessage.getValue().getTime() != null) {
                        String time = TimestampFormats.MILLI_FORMAT.format(alarmMessage.getValue().getTime());
                        return new SimpleStringProperty(time);
                    }
                    return null;
                });
        tableView.getColumns().add(timeCol);

        msgTimeCol.setCellValueFactory(
                alarmMessage -> {
                    String time = TimestampFormats.MILLI_FORMAT.format(alarmMessage.getValue().getMessage_time());
                    return new SimpleStringProperty(time);
                });
        tableView.getColumns().add(msgTimeCol);

        deltaTimeCol.setCellValueFactory(
                alarmMessage -> {
                    java.time.Duration delta = java.time.Duration.between(alarmMessage.getValue().getMessage_time(), Instant.now());
                    return new SimpleStringProperty(delta.toHours() + ":" + delta.toMinutesPart() + ":" + delta.toSecondsPart()
                            + "." + delta.toMillisPart());
                });
        tableView.getColumns().add(deltaTimeCol);

        currentSeverityCol.setCellValueFactory(
                alarmMessage -> new SimpleStringProperty(alarmMessage.getValue().getCurrent_severity()));
        currentSeverityCol.setCellFactory(alarmLogTableTypeStringTableColumn -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setStyle("-fx-text-fill: black;  -fx-background-color: transparent");
                    setText("");
                } else {

                    setText(item);
                    SeverityLevel severityLevel = parseSeverityLevel(item);
                    setStyle("-fx-alignment: center; -fx-border-color: transparent; -fx-border-width: 2 0 2 0; -fx-background-insets: 2 0 2 0; -fx-text-fill: " + JFXUtil.webRGB(AlarmUI.getColor(severityLevel)) + ";  -fx-background-color: " + JFXUtil.webRGB(AlarmUI.getBackgroundColor(severityLevel)));
                }
            }
        });
        tableView.getColumns().add(currentSeverityCol);

        currentMessageCol.setCellValueFactory(
                alarmMessage -> new SimpleStringProperty(alarmMessage.getValue().getCurrent_message()));
        tableView.getColumns().add(currentMessageCol);

        commandCol.setCellValueFactory(
                alarmMessage -> {
                    String action = alarmMessage.getValue().getCommand();
                    if (action != null) {
                        return new SimpleStringProperty(action);
                    }
                    boolean en = alarmMessage.getValue().isEnabled();
                    if (alarmMessage.getValue().getUser() != null && alarmMessage.getValue().getHost() != null) {
                        if (!en) {
                            return new SimpleStringProperty("Disabled");
                        } else {
                            try {
                                final JsonNode jsonNode = (JsonNode) JsonModelReader.parseJsonText(alarmMessage.getValue().getConfig_msg());
                                if (jsonNode == null) {
                                    logger.log(Level.WARNING, "There is no JasonNode");
                                    return null;
                                }
                                final boolean latching = jsonNode.get("latching").asBoolean();
                                return new SimpleStringProperty(latching ? "Enabled:Latched" : "Enabled:Unlatch");
                            } catch (Exception e) {
                                logger.log(Level.SEVERE, "Unexpected error in alarmMessage" + e);
                            }
                        }
                    }
                    return null;
                });
        tableView.getColumns().add(commandCol);

        userCol.setCellValueFactory(
                alarmMessage -> new SimpleStringProperty(alarmMessage.getValue().getUser()));
        tableView.getColumns().add(userCol);

        hostCol.setCellValueFactory(
                alarmMessage -> new SimpleStringProperty(alarmMessage.getValue().getHost()));
        tableView.getColumns().add(hostCol);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        searchParameters.put(Keys.PV, this.searchString);
        searchParameters.put(Keys.ROOT, "*");
        searchParameters.put(Keys.MESSAGE, "*");
        searchParameters.put(Keys.SEVERITY, "*");
        searchParameters.put(Keys.CURRENTSEVERITY, "*");
        searchParameters.put(Keys.CURRENTMESSAGE, "*");
        searchParameters.put(Keys.COMMAND, "*");
        searchParameters.put(Keys.USER, "*");
        searchParameters.put(Keys.HOST, "*");
        searchParameters.put(Keys.STARTTIME, TimeParser.format(java.time.Duration.ofDays(7)));
        searchParameters.put(Keys.ENDTIME, TimeParser.format(java.time.Duration.ZERO));
        advancedSearchViewController.setSearchParameters(searchParameters);

        query.setText(
                searchParameters.entrySet().stream()
                        .filter(e -> !e.getKey().getName().equals(Keys.ROOT.getName())) // Exclude alarm config (root) as selection is managed in drop-down
                        .sorted(Map.Entry.comparingByKey())
                        .map((e) -> replaceKey(e.getKey().getName().trim()) + "=" + e.getValue().trim())
                        .collect(Collectors.joining("&")));

        searchParameters.addListener(
                (MapChangeListener<Keys, String>) change ->
                        query.setText(searchParameters.entrySet().stream()
                                .sorted(Entry.comparingByKey())
                                .filter(e -> !e.getKey().getName().equals(Keys.ROOT.getName())) // Exclude alarm config (root) as selection is managed in drop-down
                                .filter(e -> !e.getValue().equals(""))
                                .map((e) -> replaceKey(e.getKey().getName().trim()) + "=" + e.getValue().trim())
                                .collect(Collectors.joining("&"))));

        query.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                search();
            }
        });

        progressIndicator.visibleProperty().bind(searchInProgress);
        searchInProgress.addListener((observable, oldValue, newValue) -> tableView.setDisable(newValue));

        search.disableProperty().bind(searchInProgress);

        String[] configNames = AlarmSystem.config_names;
        for (String configName : configNames) {
            AlarmConfiguration alarmConfiguration = new AlarmConfiguration(configName, false);
            alarmConfigs.add(alarmConfiguration);
            CheckBox checkBox = new CheckBox(configName);
            CustomMenuItem configItem = new CustomMenuItem(checkBox);
            configItem.setHideOnClick(false);
            checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                alarmConfiguration.setSelected(newValue);
                setSelectedConfigsString();
                search();
            });
            configsContextMenu.getItems().add(configItem);
        }

        Image downIcon = ImageCache.getImage(AlarmLogTableController.class, "/icons/down_triangle.png");
        configDropdownButton.setGraphic(new ImageView(downIcon));
        configDropdownButton.focusedProperty().addListener((changeListener, oldVal, newVal) ->
        {
            if (!newVal && !configsContextMenu.isShowing() && !configsContextMenu.isShowing())
                configDropdownButton.setSelected(false);
        });

        configSelection.textProperty().bind(selectedConfigsString);

        setSelectedConfigsString();
        periodicSearch();
    }

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> runningTask;

    private void periodicSearch() {
        logger.info("Starting a periodic search for alarm messages : " + searchString);
        if (runningTask != null) {
            runningTask.cancel(true);
        }
        runningTask = executor.scheduleAtFixedRate(() -> {
            if (alarmLogSearchJob != null) {
                alarmLogSearchJob.cancel();
            }
            sortTableCol = null;
            sortColType = null;
            if (!tableView.getSortOrder().isEmpty()) {
                sortTableCol = tableView.getSortOrder().get(0);
                sortColType = sortTableCol.getSortType();
            }
            alarmLogSearchJob = AlarmLogSearchJob.submit(httpClient, searchString, isNodeTable, searchParameters,
                    result -> Platform.runLater(() -> {
                        setAlarmMessages(result);
                        searchInProgress.set(false);
                    }), (url, ex) -> {
                        searchInProgress.set(false);
                        logger.log(Level.WARNING, "Shutting down alarm log message scheduler.", ex);
                        runningTask.cancel(true);
                    });
        }, 0, 10, TimeUnit.SECONDS);
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public void setIsNodeTable(Boolean isNodeTable) {
        this.isNodeTable = isNodeTable;
        if (!isNodeTable) {
            searchParameters.put(Keys.PV, this.searchString);
        } else {
            searchParameters.put(Keys.PV, "*");
        }
        searchParameters.put(Keys.ROOT, "*");
        searchParameters.put(Keys.MESSAGE, "*");
        searchParameters.put(Keys.SEVERITY, "*");
        searchParameters.put(Keys.CURRENTSEVERITY, "*");
        searchParameters.put(Keys.CURRENTMESSAGE, "*");
        searchParameters.put(Keys.COMMAND, "*");
        searchParameters.put(Keys.USER, "*");
        searchParameters.put(Keys.HOST, "*");
        searchParameters.put(Keys.STARTTIME, TimeParser.format(java.time.Duration.ofDays(7)));
        searchParameters.put(Keys.ENDTIME, TimeParser.format(java.time.Duration.ZERO));

        query.setText(searchParameters.entrySet().stream().sorted(Map.Entry.comparingByKey()).map((e) -> replaceKey(e.getKey().getName().trim()) + "=" + e.getValue().trim()).collect(Collectors.joining("&")));
    }

    public void setAlarmMessages(List<AlarmLogTableItem> alarmMessages) {
        this.alarmMessages = alarmMessages;
        tableView.setItems(FXCollections.observableArrayList(this.alarmMessages));
        if (sortTableCol != null) {
            tableView.getSortOrder().add(sortTableCol);
            sortTableCol.setSortType(sortColType);
            sortTableCol.setSortable(true);
        }
    }

    public void setClient(HttpClient client) {
        this.httpClient = client;
    }

    /**
     * A Helper method which returns the appropriate {@link SeverityLevel} matching the
     * string level
     *
     * @param level Severity level
     */
    private static SeverityLevel parseSeverityLevel(String level) {
        switch (level.toUpperCase()) {
            case "MINOR_ACK":
                return SeverityLevel.MINOR_ACK;
            case "MAJOR_ACK":
                return SeverityLevel.MAJOR_ACK;
            case "INVALID_ACK":
                return SeverityLevel.INVALID_ACK;
            case "UNDEFINED_ACK":
                return SeverityLevel.UNDEFINED_ACK;
            case "MINOR":
                return SeverityLevel.MINOR;
            case "MAJOR":
                return SeverityLevel.MAJOR;
            case "INVALID":
                return SeverityLevel.INVALID;
            case "UNDEFINED":
                return SeverityLevel.UNDEFINED;

            default:
                return SeverityLevel.OK;
        }

    }

    // Keeps track of when the animation is active. Multiple clicks will be ignored
    // until a give resize action is completed
    private final AtomicBoolean moving = new AtomicBoolean(false);

    @FXML
    public void resize() {
        if (!moving.compareAndExchangeAcquire(false, true)) {
            if (resize.getText().equals(">")) {
                Duration cycleDuration = Duration.millis(400);
                KeyValue kv = new KeyValue(advancedSearchViewController.getPane().minWidthProperty(), 0);
                KeyValue kv2 = new KeyValue(advancedSearchViewController.getPane().maxWidthProperty(), 0);
                Timeline timeline = new Timeline(new KeyFrame(cycleDuration, kv, kv2));
                timeline.play();
                timeline.setOnFinished(event -> {
                    resize.setText("<");
                    moving.set(false);
                });
            } else {
                Duration cycleDuration = Duration.millis(400);
                double width = ViewSearchPane.getWidth() / 3;
                KeyValue kv = new KeyValue(advancedSearchViewController.getPane().minWidthProperty(), width);
                KeyValue kv2 = new KeyValue(advancedSearchViewController.getPane().prefWidthProperty(), width);
                Timeline timeline = new Timeline(new KeyFrame(cycleDuration, kv, kv2));
                timeline.play();
                timeline.setOnFinished(event -> {
                    resize.setText(">");
                    moving.set(false);
                });
            }
        }
    }

    Map<String, Keys> lookup =
            Arrays.stream(Keys.values()).filter(k -> !k.getName().equals(Keys.ROOT.getName())).collect(Collectors.toMap(Keys::getName, k -> k));

    @FXML
    void updateQuery() {
        List<String> searchTerms = Arrays.asList(query.getText().split("&"));
        Set<String> searchKeywords = new TreeSet<>();
        searchTerms.forEach(s -> {
            String[] splitString = s.split("=");
            if (splitString.length > 1) {
                String key = recoverKey(splitString[0]);
                searchKeywords.add(key);
                String value = splitString[1];
                if (lookup.containsKey(key)) {
                    searchParameters.put(lookup.get(key), value);
                }
            }
        });

        for (Keys key : searchParameters.keySet()) {
            if (!searchKeywords.contains(key.toString())) {
                searchParameters.put(key, "");
            }
        }

        // Add root (alarm config) separately as it is selected differently by user,
        // i.e. from drop-down rather than typing into the text field.
        searchParameters.put(Keys.ROOT, configSelection.getText());

    }

    @FXML
    public void search() {
        searchInProgress.set(true);
        tableView.getSortOrder().clear();
        updateQuery();
        alarmLogSearchJob.cancel();
        periodicSearch();
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @FXML
    public void createContextMenu() {
        final ContextMenu contextMenu = new ContextMenu();
        MenuItem configurationInfo = new MenuItem(Messages.ConfigurationInfo);
        configurationInfo.setOnAction(actionEvent -> {
            List<String> configs = tableView.getSelectionModel().getSelectedItems()
                    .stream().map(e -> {
                        try {
                            URI uri = new URI(e.getConfig().replace(" ", "%20"));
                            return uri.getSchemeSpecificPart();
                        } catch (URISyntaxException ex) {
                            ex.printStackTrace();
                        }
                        return null;
                    })
                    .collect(Collectors.toList());
            // TODO place holder method for showing additional alarm info
            AlarmLogConfigSearchJob.submit(httpClient,
                    configs.get(0),
                    result -> Platform.runLater(() -> {
                        Alert alarmInfo = new Alert(Alert.AlertType.INFORMATION);
                        alarmInfo.setTitle(Messages.AlarmInformation);
                        alarmInfo.setHeaderText(null);
                        alarmInfo.setResizable(true);
                        // Corner case: search query may return zero results (or null), so dialog message should show that.
                        if (result == null) {
                            alarmInfo.setContentText(Messages.ConfigurationInfoNotFound);
                        } else {
                            try {
                                String newLine = System.lineSeparator();
                                StringBuilder sb = new StringBuilder();
                                sb.append("message_time: ").append(TimestampFormats.MILLI_FORMAT.format(result.getMessage_time())).append(newLine);
                                sb.append("config: ").append(result.getConfig()).append(newLine);
                                sb.append("user: ").append(result.getUser()).append(newLine);
                                sb.append("host: ").append(result.getHost()).append(newLine);
                                sb.append("enabled: ").append(result.isEnabled()).append(newLine);
                                Object jsonObject = objectMapper.readValue(result.getConfig_msg(), Object.class);
                                sb.append("config_msg: ").append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject)).append(newLine);
                                alarmInfo.setContentText(sb.toString());
                            } catch (JsonProcessingException e) {
                                alarmInfo.setContentText(Messages.ConfigurationInfoNotFound);
                            }
                        }
                        alarmInfo.show();
                    }),
                    (url, ex) -> ExceptionDetailsErrorDialog.openError("Alarm Log Info Error", ex)
            );

        });
        contextMenu.getItems().add(configurationInfo);

        contextMenu.getItems().add(new SeparatorMenuItem());

        // search for other context menu actions registered for AlarmLogTableType
        SelectionService.getInstance().setSelection("AlarmLogTable", tableView.getSelectionModel().getSelectedItems());

        if (TableHelper.addContextMenuColumnVisibilityEntries(tableView, contextMenu))
            contextMenu.getItems().add(new SeparatorMenuItem());

        ContextMenuHelper.addSupportedEntries(FocusUtil.setFocusOn(tableView), contextMenu);

        tableView.setContextMenu(contextMenu);

    }

    public void shutdown() {
        if (runningTask != null) {
            runningTask.cancel(true);
        }
    }

    private static class AlarmConfiguration {
        private final String configurationName;
        private boolean selected;

        public AlarmConfiguration(String configurationName, boolean selected) {
            this.configurationName = configurationName;
            this.selected = selected;
        }

        public String getConfigurationName() {
            return configurationName;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }
    }

    private void setSelectedConfigsString() {
        List<AlarmConfiguration> selectedConfigs =
                alarmConfigs.stream().filter(AlarmConfiguration::isSelected).collect(Collectors.toList());
        if (selectedConfigs.size() == alarmConfigs.size() || selectedConfigs.size() == 0) {
            selectedConfigsString.set("*");
        } else {
            selectedConfigsString.set(alarmConfigs.stream().filter(AlarmConfiguration::isSelected)
                    .map(AlarmConfiguration::getConfigurationName).collect(Collectors.joining(",")));
        }
    }

    @FXML
    public void selectConfigs() {
        if (configDropdownButton.isSelected()) {
            configsContextMenu.show(configSelection, Side.BOTTOM, 0, 0);
        } else {
            configsContextMenu.hide();
        }
    }

    void save(final Memento memento) {
        TableHelper.saveColumnVisibilities(tableView, memento, (col, idx) -> "COL" + idx + "vis");
    }

    void restore(final Memento memento) {
        TableHelper.restoreColumnVisibilities(tableView, memento, (col, idx) -> "COL" + idx + "vis");
    }
}
