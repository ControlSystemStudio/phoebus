package org.phoebus.logbook.ui;

import java.util.stream.Collectors;

import org.phoebus.logging.LogEntry;
import org.phoebus.logging.Logbook;
import org.phoebus.logging.Tag;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;

public class LogEntryController {

    @FXML
    TitledPane LogBody;
    @FXML
    Label logTime;
    @FXML
    TextArea logDescription;
    @FXML
    ListView<String> logTags;
    @FXML
    ListView<String> LogLogbooks;

    @FXML
    TitledPane LogAttchments;

    private LogEntry logEntry;

    @FXML
    public void initialize() {

        if (logEntry != null) {
            LogBody.setExpanded(true);
            LogAttchments.setExpanded(!logEntry.getAttachments().isEmpty());

            logDescription.setWrapText(true);
            logDescription.setText(logEntry.getDescription());

            logTime.setText(logEntry.getCreatedDate().toString());
            ObservableList<String> logbookList = FXCollections.observableArrayList();
            logbookList.addAll(logEntry.getLogbooks().stream().map(Logbook::getName).collect(Collectors.toList()));
            LogLogbooks.setItems(logbookList);
            ObservableList<String> tagList = FXCollections.observableArrayList();
            tagList.addAll(logEntry.getTags().stream().map(Tag::getName).collect(Collectors.toList()));
            logTags.setItems(tagList);
        }
    }

    public LogEntry getLogEntry() {
        return logEntry;
    }

    public void setLogEntry(LogEntry logEntry) {
        this.logEntry = logEntry;
        initialize();
    }

}
