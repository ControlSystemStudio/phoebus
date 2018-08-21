package org.phoebus.applications.alarm.logging.ui;

import static org.phoebus.applications.alarm.logging.ui.AlarmLogTableApp.logger;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.elasticsearch.client.RestHighLevelClient;
import org.phoebus.applications.alarm.messages.AlarmStateMessage;
import org.phoebus.framework.jobs.Job;
import org.phoebus.util.time.TimestampFormats;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.util.Callback;

public class AlarmLogTableController {

    @FXML
    TableView<AlarmStateMessage> tableView;

    @FXML
    TableColumn<AlarmStateMessage, String> configCol;
    @FXML
    TableColumn<AlarmStateMessage, String> pvCol;
    @FXML
    TableColumn<AlarmStateMessage, String> severityCol;
    @FXML
    TableColumn<AlarmStateMessage, String> messageCol;
    @FXML
    TableColumn<AlarmStateMessage, String> valueCol;
    @FXML
    TableColumn<AlarmStateMessage, String> timeCol;
    @FXML
    TableColumn<AlarmStateMessage, String> msgTimeCol;
    @FXML
    TableColumn<AlarmStateMessage, String> currentSeverityCol;
    @FXML
    TableColumn<AlarmStateMessage, String> currentMessageCol;
    @FXML
    TableColumn<AlarmStateMessage, String> mode;

    // The search string
    // TODO need to standardize the search string so that it can be easily parsed
    private String searchString = "*";
    // Result
    private List<AlarmStateMessage> alarmStateMessages;

    private Job alarmLogSearchJob;
    private RestHighLevelClient searchClient;

    @FXML
    public void initialize() {
        tableView.getColumns().clear();
        configCol = new TableColumn<>("Config");
        configCol.setCellValueFactory(
                new Callback<CellDataFeatures<AlarmStateMessage, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(CellDataFeatures<AlarmStateMessage, String> alarmStateMessage) {
                        return new SimpleStringProperty(alarmStateMessage.getValue().getConfig());
                    }
                });
        tableView.getColumns().add(configCol);

        pvCol = new TableColumn<>("PV");
        pvCol.setCellValueFactory(new Callback<CellDataFeatures<AlarmStateMessage, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<AlarmStateMessage, String> alarmStateMessage) {
                return new SimpleStringProperty(alarmStateMessage.getValue().getPv());
            }
        });
        tableView.getColumns().add(pvCol);

        severityCol = new TableColumn<>("Severity");
        severityCol.setCellValueFactory(
                new Callback<CellDataFeatures<AlarmStateMessage, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(CellDataFeatures<AlarmStateMessage, String> alarmStateMessage) {
                        return new SimpleStringProperty(alarmStateMessage.getValue().getSeverity());
                    }
                });
        tableView.getColumns().add(severityCol);

        messageCol = new TableColumn<>("Message");
        messageCol.setCellValueFactory(
                new Callback<CellDataFeatures<AlarmStateMessage, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(CellDataFeatures<AlarmStateMessage, String> alarmStateMessage) {
                        return new SimpleStringProperty(alarmStateMessage.getValue().getMessage());
                    }
                });
        tableView.getColumns().add(messageCol);

        timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(
                new Callback<CellDataFeatures<AlarmStateMessage, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(CellDataFeatures<AlarmStateMessage, String> alarmStateMessage) {
                        String time = TimestampFormats.MILLI_FORMAT.format(alarmStateMessage.getValue().getInstant());
                        return new SimpleStringProperty(time);
                    }
                });
        tableView.getColumns().add(timeCol);

        msgTimeCol = new TableColumn<>("Message Time");
        msgTimeCol.setCellValueFactory(
                new Callback<CellDataFeatures<AlarmStateMessage, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(CellDataFeatures<AlarmStateMessage, String> alarmStateMessage) {
                        String time = TimestampFormats.MILLI_FORMAT.format(alarmStateMessage.getValue().getMessage_time());
                        return new SimpleStringProperty(time);
                    }
                });
        tableView.getColumns().add(msgTimeCol);

        currentSeverityCol = new TableColumn<>("Current Severity");
        currentSeverityCol.setCellValueFactory(
                new Callback<CellDataFeatures<AlarmStateMessage, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(CellDataFeatures<AlarmStateMessage, String> alarmStateMessage) {
                        return new SimpleStringProperty(alarmStateMessage.getValue().getCurrent_severity());
                    }
                });
        tableView.getColumns().add(currentSeverityCol);

        currentMessageCol = new TableColumn<>("Current Message");
        currentMessageCol.setCellValueFactory(
                new Callback<CellDataFeatures<AlarmStateMessage, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(CellDataFeatures<AlarmStateMessage, String> alarmStateMessage) {
                        return new SimpleStringProperty(alarmStateMessage.getValue().getCurrent_message());
                    }
                });
        tableView.getColumns().add(currentMessageCol);
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
            alarmLogSearchJob = AlarmLogSearchJob.submit(searchClient, searchString,
                    result -> Platform.runLater(() -> setAlarmStateMessages(result)), (url, ex) -> {
                        logger.log(Level.WARNING, "Shutting down alarm log message scheduler.", ex);
                        runningTask.cancel(true);
                    });
        }, 0, 10, TimeUnit.SECONDS);
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
        peroidicSearch();
    }

    public List<AlarmStateMessage> getAlarmStateMessages() {
        return alarmStateMessages;
    }

    public void setAlarmStateMessages(List<AlarmStateMessage> alarmStateMessages) {
        this.alarmStateMessages = alarmStateMessages;
        tableView.setItems(FXCollections.observableArrayList(this.alarmStateMessages));
    }

    public void setClient(RestHighLevelClient client) {
        this.searchClient = client;
    }

    public void shutdown() {
        if (runningTask != null) {
            runningTask.cancel(true);
        }
    }

}
