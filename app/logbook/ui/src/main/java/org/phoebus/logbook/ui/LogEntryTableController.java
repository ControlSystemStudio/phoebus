package org.phoebus.logbook.ui;

import java.util.List;
import java.util.stream.Collectors;

import org.phoebus.logbook.LogEntry;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class LogEntryTableController extends LogbookSearchController {

    List<LogEntry> logEntries;

    @FXML
    TextField query;
    @FXML
    Button search;

    @FXML
    ListView<LogEntry> logs;

    @FXML
    public void initialize() {

        logs.setCellFactory(listView -> new ListCell<LogEntry>() {

            @Override
            public void updateItem(LogEntry logEntry, boolean empty) {
                super.updateItem(logEntry, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(null);
                    HBox hbox = new HBox();
                    LogEntryControl logbookEntryControl = new LogEntryControl();
                    logbookEntryControl.setLog(logEntry);
                    hbox.getChildren().add(logbookEntryControl);
                    setGraphic(hbox);
                }
            }
        });
    }

    public void refresh() {
        if (logEntries != null && !logEntries.isEmpty()) {
            ObservableList<LogEntry> logsList = FXCollections.observableArrayList();
            logsList.addAll(logEntries.stream().collect(Collectors.toList()));
            logs.setItems(logsList);
        }
    }

    public void setQuery(String string) {
        query.setText(string);
        search();
    }

    @FXML
    public void search() {
        super.search(query.getText());
    }

    @Override
    public void setLogs(List<LogEntry> logs) {
        this.logEntries = logs;
        refresh();
    }

}
