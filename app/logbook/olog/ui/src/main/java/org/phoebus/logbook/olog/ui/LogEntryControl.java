package org.phoebus.logbook.olog.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import org.phoebus.logbook.LogEntry;

import java.io.IOException;

/**
 * A control to display a single log entry, this control can be embedded in trees, tables, or other views.
 */
public class LogEntryControl extends VBox {

    private final LogEntryController controller;

    public LogEntryControl() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("LogEntryDisplay.fxml"));
        try {
            fxmlLoader.load();
            controller = fxmlLoader.getController();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void setLog(LogEntry logEntry) {
        controller.setLogEntry(logEntry);
    }

}
