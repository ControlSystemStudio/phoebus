package org.phoebus.logbook.ui;

import java.io.IOException;
import java.util.List;

import org.phoebus.logbook.LogEntry;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;

public class LogEntryTableControl extends VBox {

    private final LogbookSearchController controller;

    public LogEntryTableControl() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("LogEntryTable.fxml"));
        try {
            fxmlLoader.setRoot(this);
            fxmlLoader.load();
            this.controller = fxmlLoader.getController();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void setLogs(List<LogEntry> logs) {
        controller.setLogs(logs);
    }

}
