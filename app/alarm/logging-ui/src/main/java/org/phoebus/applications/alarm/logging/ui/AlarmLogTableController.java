package org.phoebus.applications.alarm.logging.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.jersey.api.client.WebResource;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.util.Duration;
import org.phoebus.applications.alarm.logging.ui.AlarmLogTableQueryUtil.Keys;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.ui.AlarmUI;
import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.ui.application.ContextMenuHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.util.time.TimeParser;
import org.phoebus.util.time.TimestampFormats;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
    TextField searchText;
    @FXML
    TextField startTime;
    @FXML
    TextField endTime;
    TableColumn<AlarmLogTableItem, String> sortTableCol = null;
    SortType sortColType = null;
    // Search parameters
    ObservableMap<Keys, String> searchParameters = FXCollections.<Keys, String>observableHashMap();
    // The search string
    // TODO need to standardize the search string so that it can be easily parsed
    private String searchString = "*";
    private Boolean isNodeTable = true;
    // Result
    private List<AlarmLogTableItem> alarmMessages;

    private Job alarmLogSearchJob;
    private WebResource searchClient;
    
    @FXML
    private ProgressIndicator progressIndicator;
    private SimpleBooleanProperty searchInProgress = new SimpleBooleanProperty(false);

    public AlarmLogTableController(WebResource client) {
        setClient(client);
    }

    @FXML
    public void initialize() {
        resize.setText("<");
        tableView.getColumns().clear();
        configCol = new TableColumn<>("Config");
        configCol.setCellValueFactory(
                new Callback<CellDataFeatures<AlarmLogTableItem, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(CellDataFeatures<AlarmLogTableItem, String> alarmMessage) {
                        return new SimpleStringProperty(alarmMessage.getValue().getConfig());
                    }
                });
        tableView.getColumns().add(configCol);

        pvCol = new TableColumn<>("PV");
        pvCol.setCellValueFactory(new Callback<CellDataFeatures<AlarmLogTableItem, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<AlarmLogTableItem, String> alarmMessage) {
                return new SimpleStringProperty(alarmMessage.getValue().getPv());
            }
        });
        tableView.getColumns().add(pvCol);

        severityCol = new TableColumn<>("Severity");
        severityCol.setCellValueFactory(
                new Callback<CellDataFeatures<AlarmLogTableItem, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(CellDataFeatures<AlarmLogTableItem, String> alarmMessage) {
                        return new SimpleStringProperty(alarmMessage.getValue().getSeverity());
                    }
                });
        severityCol.setCellFactory(alarmLogTableTypeStringTableColumn -> new TableCell<AlarmLogTableItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty  ||  item == null)
                {
                    setText("");
                    setTextFill(Color.BLACK);
                }
                else
                {
                    setText(item);
                    setTextFill(AlarmUI.getColor(parseSeverityLevel(item)));
                }
            }
        });
        tableView.getColumns().add(severityCol);

        messageCol = new TableColumn<>("Message");
        messageCol.setCellValueFactory(
                new Callback<CellDataFeatures<AlarmLogTableItem, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(CellDataFeatures<AlarmLogTableItem, String> alarmMessage) {
                        return new SimpleStringProperty(alarmMessage.getValue().getMessage());
                    }
                });
        tableView.getColumns().add(messageCol);

        timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(
                new Callback<CellDataFeatures<AlarmLogTableItem, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(CellDataFeatures<AlarmLogTableItem, String> alarmMessage) {
                        if (alarmMessage.getValue().getTime() != null) {
                            String time = TimestampFormats.MILLI_FORMAT.format(alarmMessage.getValue().getTime());
                            return new SimpleStringProperty(time);
                        }
                        return null;
                    }
                });
        tableView.getColumns().add(timeCol);

        msgTimeCol = new TableColumn<>("Message Time");
        msgTimeCol.setCellValueFactory(
                new Callback<CellDataFeatures<AlarmLogTableItem, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(CellDataFeatures<AlarmLogTableItem, String> alarmMessage) {
                        String time = TimestampFormats.MILLI_FORMAT.format(alarmMessage.getValue().getMessage_time());
                        return new SimpleStringProperty(time);
                    }
                });
        tableView.getColumns().add(msgTimeCol);

        deltaTimeCol = new TableColumn<>("Time Delta");
        deltaTimeCol.setCellValueFactory(
                alarmMessage -> {
                    java.time.Duration delta = java.time.Duration.between(alarmMessage.getValue().getMessage_time(), Instant.now());
                    return new SimpleStringProperty(delta.toHours() + ":" + delta.toMinutesPart() + ":" + delta.toSecondsPart()
                            + "." + delta.toMillisPart());
                });
        deltaTimeCol.setVisible(false);
        tableView.getColumns().add(deltaTimeCol);

        currentSeverityCol = new TableColumn<>("Current Severity");
        currentSeverityCol.setCellValueFactory(
                new Callback<CellDataFeatures<AlarmLogTableItem, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(CellDataFeatures<AlarmLogTableItem, String> alarmMessage) {
                        return new SimpleStringProperty(alarmMessage.getValue().getCurrent_severity());
                    }
                });
        currentSeverityCol.setCellFactory(alarmLogTableTypeStringTableColumn -> new TableCell<AlarmLogTableItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty  ||  item == null)
                {
                    setText("");
                    setTextFill(Color.BLACK);
                }
                else
                {
                    setText(item);
                    setTextFill(AlarmUI.getColor(parseSeverityLevel(item)));
                }
            }
        });
        tableView.getColumns().add(currentSeverityCol);

        currentMessageCol = new TableColumn<>("Current Message");
        currentMessageCol.setCellValueFactory(
                new Callback<CellDataFeatures<AlarmLogTableItem, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(CellDataFeatures<AlarmLogTableItem, String> alarmMessage) {
                        return new SimpleStringProperty(alarmMessage.getValue().getCurrent_message());
                    }
                });
        tableView.getColumns().add(currentMessageCol);

        commandCol = new TableColumn<>("Command");
        commandCol.setCellValueFactory(
                new Callback<CellDataFeatures<AlarmLogTableItem, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(CellDataFeatures<AlarmLogTableItem, String> alarmMessage) {
                        String action = alarmMessage.getValue().getCommand();
                        if (action != null) {
                            return new SimpleStringProperty(action);
                        }
                        boolean en = alarmMessage.getValue().isEnabled();
                        if (alarmMessage.getValue().getUser() != null && alarmMessage.getValue().getHost() != null) {
                            if (en == false) {
                                return new SimpleStringProperty("Disabled");
                            } else if (en == true) {
                                return new SimpleStringProperty("Enabled");
                            }
                        }
                        return null;
                    }
                });
        tableView.getColumns().add(commandCol);

        userCol = new TableColumn<>("User");
        userCol.setCellValueFactory(
                new Callback<CellDataFeatures<AlarmLogTableItem, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(CellDataFeatures<AlarmLogTableItem, String> alarmMessage) {
                        return new SimpleStringProperty(alarmMessage.getValue().getUser());
                    }
                });
        tableView.getColumns().add(userCol);

        hostCol = new TableColumn<>("Host");
        hostCol.setCellValueFactory(
                new Callback<CellDataFeatures<AlarmLogTableItem, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(CellDataFeatures<AlarmLogTableItem, String> alarmMessage) {
                        return new SimpleStringProperty(alarmMessage.getValue().getHost());
                    }
                });
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
                        .sorted(Map.Entry.comparingByKey())
                        .map((e) -> e.getKey().getName().trim() + "=" + e.getValue().trim())
                        .collect(Collectors.joining("&")));

        searchParameters.addListener(
                (MapChangeListener<Keys, String>) change -> query.setText(searchParameters.entrySet().stream()
                        .sorted(Entry.comparingByKey())
                        .map((e) -> e.getKey().getName().trim() + "=" + e.getValue().trim())
                        .collect(Collectors.joining("&"))));

        query.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                search();
            }
        });

	    progressIndicator.visibleProperty().bind(searchInProgress);
        searchInProgress.addListener((observable, oldValue, newValue) -> {
            tableView.setDisable(newValue.booleanValue());
        });

        search.disableProperty().bind(searchInProgress);

        periodicSearch();
    }

    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
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
                sortTableCol = (TableColumn) tableView.getSortOrder().get(0);
                sortColType = sortTableCol.getSortType();
            }
            alarmLogSearchJob = AlarmLogSearchJob.submit(searchClient, searchString, isNodeTable, searchParameters,
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
        if (isNodeTable == false) {
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

        query.setText(searchParameters.entrySet().stream().sorted(Map.Entry.comparingByKey()).map((e) -> {
            return e.getKey().getName().trim() + "=" + e.getValue().trim();
        }).collect(Collectors.joining("&")));
    }

    public List<AlarmLogTableItem> getAlarmMessages() {
        return alarmMessages;
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

    public void setClient(WebResource client) {
        this.searchClient = client;
    }

    /**
     * A Helper method which returns the appropriate {@link SeverityLevel} matching the
     * string level
     * @param level
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
    private AtomicBoolean moving = new AtomicBoolean(false);

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

    Map<String, Keys> lookup = Arrays.stream(Keys.values()).collect(Collectors.toMap(Keys::getName, k -> {
        return k;
    }));

    @FXML
    void updateQuery() {
        List<String> searchTerms = Arrays.asList(query.getText().split("&"));
        searchTerms.stream().forEach(s -> {
            String key = s.split("=")[0];
            String value = s.split("=")[1];
            if(lookup.containsKey(key)) {
                searchParameters.put(lookup.get(key), value);
            }
        });

    }

    @FXML
    public void search() {
        searchInProgress.set(true);
        tableView.getSortOrder().clear();
        updateQuery();
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @FXML
    public void createContextMenu() {
        final ContextMenu contextMenu = new ContextMenu();
        MenuItem configurationInfo = new MenuItem(Messages.ConfigurationInfo);
        configurationInfo.setOnAction( actionEvent -> {
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
            AlarmLogConfigSearchJob.submit(searchClient,
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
                                sb.append("message_time: " + TimestampFormats.MILLI_FORMAT.format(result.getMessage_time()) + newLine);
                                sb.append("config: " + result.getConfig() + newLine);
                                sb.append("user: " + result.getUser() + newLine);
                                sb.append("host: " + result.getHost() + newLine);
                                sb.append("enabled: " + result.isEnabled() + newLine);
                                Object jsonObject = objectMapper.readValue(result.getConfig_msg(), Object.class);
                                sb.append("config_msg: " + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject) + newLine);
                                alarmInfo.setContentText(sb.toString());
                            } catch (JsonProcessingException e) {
                                alarmInfo.setContentText(Messages.ConfigurationInfoNotFound);
                            }
                        }
                        alarmInfo.show();
                    }),
                    (url, ex) -> ExceptionDetailsErrorDialog.openError("Alarm Log Info Error", ex.getMessage(), ex)
            );

        });
        contextMenu.getItems().add(configurationInfo);

        contextMenu.getItems().add(new SeparatorMenuItem());

       // search for other context menu actions registered for AlarmLogTableType
        SelectionService.getInstance().setSelection("AlarmLogTable", tableView.getSelectionModel().getSelectedItems());
        ContextMenuHelper.addSupportedEntries(tableView, contextMenu);

        tableView.setContextMenu(contextMenu);

    }

    public void shutdown() {
        if (runningTask != null) {
            runningTask.cancel(true);
        }
    }

}
