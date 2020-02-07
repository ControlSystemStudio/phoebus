package org.phoebus.applications.alarm.logging.ui;

import static org.phoebus.applications.alarm.logging.ui.AlarmLogTableApp.logger;

import org.phoebus.util.time.TimeParser;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.event.EventHandler;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.elasticsearch.client.RestHighLevelClient;
import org.phoebus.applications.alarm.logging.ui.AlarmLogTableType;
import org.phoebus.applications.alarm.logging.ui.AlarmLogTableQueryUtil.Keys;
import org.phoebus.framework.jobs.Job;
import org.phoebus.util.time.TimestampFormats;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import javafx.util.Callback;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class AlarmLogTableController {

    @FXML
    TableView<AlarmLogTableType> tableView;

    @FXML
    TableColumn<AlarmLogTableType, String> configCol;
    @FXML
    TableColumn<AlarmLogTableType, String> pvCol;
    @FXML
    TableColumn<AlarmLogTableType, String> severityCol;
    @FXML
    TableColumn<AlarmLogTableType, String> messageCol;
    @FXML
    TableColumn<AlarmLogTableType, String> valueCol;
    @FXML
    TableColumn<AlarmLogTableType, String> timeCol;
    @FXML
    TableColumn<AlarmLogTableType, String> msgTimeCol;
    @FXML
    TableColumn<AlarmLogTableType, String> currentSeverityCol;
    @FXML
    TableColumn<AlarmLogTableType, String> currentMessageCol;
    @FXML
    TableColumn<AlarmLogTableType, String> mode;
    @FXML
    TableColumn<AlarmLogTableType, String> commandCol;
    @FXML
    TableColumn<AlarmLogTableType, String> userCol;
    @FXML
    TableColumn<AlarmLogTableType, String> hostCol;
    @FXML
    Button search;
    @FXML
    TextField query;
    @FXML
    AnchorPane ViewSearchPane;
    @FXML
    TextField searchText;
    @FXML
    TextField startTime;
    @FXML
    TextField endTime;
    TableColumn<AlarmLogTableType, String> sortTableCol = null;
    SortType sortColType = null;
    // Search parameters
    ObservableMap<Keys, String> searchParameters = FXCollections.<Keys, String>observableHashMap();
    // The search string
    // TODO need to standardize the search string so that it can be easily parsed
    private String searchString = "*";
    private Boolean isNodeTable = true;
    // Result
    private List<AlarmLogTableType> alarmMessages;

    private Job alarmLogSearchJob;
    private RestHighLevelClient searchClient;

    @FXML
    public void initialize() {
        tableView.getColumns().clear();
        configCol = new TableColumn<>("Config");
        configCol.setCellValueFactory(
                new Callback<CellDataFeatures<AlarmLogTableType, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(CellDataFeatures<AlarmLogTableType, String> alarmMessage) {
                        return new SimpleStringProperty(alarmMessage.getValue().getConfig());
                    }
                });
        tableView.getColumns().add(configCol);

        pvCol = new TableColumn<>("PV");
        pvCol.setCellValueFactory(new Callback<CellDataFeatures<AlarmLogTableType, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<AlarmLogTableType, String> alarmMessage) {
                return new SimpleStringProperty(alarmMessage.getValue().getPv());
            }
        });
        tableView.getColumns().add(pvCol);

        severityCol = new TableColumn<>("Severity");
        severityCol.setCellValueFactory(
                new Callback<CellDataFeatures<AlarmLogTableType, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(CellDataFeatures<AlarmLogTableType, String> alarmMessage) {
                        return new SimpleStringProperty(alarmMessage.getValue().getSeverity());
                    }
                });
        tableView.getColumns().add(severityCol);

        messageCol = new TableColumn<>("Message");
        messageCol.setCellValueFactory(
                new Callback<CellDataFeatures<AlarmLogTableType, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(CellDataFeatures<AlarmLogTableType, String> alarmMessage) {
                        return new SimpleStringProperty(alarmMessage.getValue().getMessage());
                    }
                });
        tableView.getColumns().add(messageCol);
        
      timeCol = new TableColumn<>("Time");
      timeCol.setCellValueFactory(
              new Callback<CellDataFeatures<AlarmLogTableType, String>, ObservableValue<String>>() {
                  @Override
                  public ObservableValue<String> call(CellDataFeatures<AlarmLogTableType, String> alarmMessage) {
                	  if (alarmMessage.getValue().getTime() != null) {
                		  String time = TimestampFormats.MILLI_FORMAT.format(alarmMessage.getValue().getInstant());
                		  return new SimpleStringProperty(time);
                	  }
                	  return null;
                  }
              });
      tableView.getColumns().add(timeCol);
        
        msgTimeCol = new TableColumn<>("Message Time");
        msgTimeCol.setCellValueFactory(
                new Callback<CellDataFeatures<AlarmLogTableType, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(CellDataFeatures<AlarmLogTableType, String> alarmMessage) {
                        String time = TimestampFormats.MILLI_FORMAT.format(alarmMessage.getValue().getMessage_time());
                        return new SimpleStringProperty(time);
                    }
                });
        tableView.getColumns().add(msgTimeCol);

        currentSeverityCol = new TableColumn<>("Current Severity");
        currentSeverityCol.setCellValueFactory(
                new Callback<CellDataFeatures<AlarmLogTableType, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(CellDataFeatures<AlarmLogTableType, String> alarmMessage) {
                        return new SimpleStringProperty(alarmMessage.getValue().getCurrent_severity());
                    }
                });
        tableView.getColumns().add(currentSeverityCol);

        currentMessageCol = new TableColumn<>("Current Message");
        currentMessageCol.setCellValueFactory(
                new Callback<CellDataFeatures<AlarmLogTableType, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(CellDataFeatures<AlarmLogTableType, String> alarmMessage) {
                        return new SimpleStringProperty(alarmMessage.getValue().getCurrent_message());
                    }
                });
        tableView.getColumns().add(currentMessageCol);
        
        commandCol = new TableColumn<>("Command");
        commandCol.setCellValueFactory(
                new Callback<CellDataFeatures<AlarmLogTableType, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(CellDataFeatures<AlarmLogTableType, String> alarmMessage) {
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
                new Callback<CellDataFeatures<AlarmLogTableType, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(CellDataFeatures<AlarmLogTableType, String> alarmMessage) {
                        return new SimpleStringProperty(alarmMessage.getValue().getUser());
                    }
                });
        tableView.getColumns().add(userCol);
        
        hostCol = new TableColumn<>("Host");
        hostCol.setCellValueFactory(
                new Callback<CellDataFeatures<AlarmLogTableType, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(CellDataFeatures<AlarmLogTableType, String> alarmMessage) {
                        return new SimpleStringProperty(alarmMessage.getValue().getHost());
                    }
                });
        tableView.getColumns().add(hostCol);

        searchParameters.put(Keys.PV, this.searchString);
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

	query.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode() == KeyCode.ENTER)  {
                    search();
                }
            }
        });

        peroidicSearch();
    }

    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> runningTask;

    private void peroidicSearch() {
        logger.info("Starting a peroidic search for alarm messages : " + searchString);
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
                    result -> Platform.runLater(() -> setAlarmMessages(result)), (url, ex) -> {
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

    public List<AlarmLogTableType> getAlarmMessages() {
        return alarmMessages;
    }

    public void setAlarmMessages(List<AlarmLogTableType> alarmMessages) {
        this.alarmMessages = alarmMessages;
        tableView.setItems(FXCollections.observableArrayList(this.alarmMessages));
	if (sortTableCol != null) {
            tableView.getSortOrder().add(sortTableCol);
            sortTableCol.setSortType(sortColType);
            sortTableCol.setSortable(true);
        }
    }

    public void setClient(RestHighLevelClient client) {
        this.searchClient = client;
    }

    void updateQuery() {
        Arrays.asList(query.getText().split("&")).forEach(s -> {
            String key = s.split("=")[0];
            for (Map.Entry<Keys, String> entry : searchParameters.entrySet()) {
                if (entry.getKey().getName().equals(key)) {
                    searchParameters.put(entry.getKey(), s.split("=")[1]);
                }
            }
        });
    }

    @FXML
    public void search() {
	tableView.getSortOrder().clear();
        updateQuery();
    }
	
    public void shutdown() {
        if (runningTask != null) {
            runningTask.cancel(true);
        }
    }

}
