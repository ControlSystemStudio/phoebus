/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.logbook.olog.ui;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogbookException;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.olog.ui.write.LogEntryEditorStage;
import org.phoebus.olog.es.api.model.LogGroupProperty;
import org.phoebus.olog.es.api.model.OlogLog;
import org.phoebus.ui.docking.DockPane;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LogEntryDisplayController {

    @FXML
    private SingleLogEntryDisplayController singleLogEntryDisplayController;
    @FXML
    private MergedLogEntryDisplayController mergedLogEntryDisplayController;
    @FXML
    private ToggleButton showHideLogEntryGroupButton;
    @FXML
    private ToolBar toolBar;
    @FXML
    private Button replyButton;
    @FXML
    private BorderPane emptyPane;
    @FXML
    private Node singleLogEntryDisplay;
    @FXML
    private Node mergedLogEntryDisplay;

    private LogClient logClient;

    private SimpleBooleanProperty showLogGroup = new SimpleBooleanProperty(false);
    private SimpleObjectProperty<LogEntry> logEntryProperty =
            new SimpleObjectProperty<>();

    private Logger logger = Logger.getLogger(LogEntryDisplayController.class.getName());

    public LogEntryDisplayController(LogClient logClient){
        this.logClient = logClient;
    }

    @FXML
    public void initialize() {
        emptyPane.visibleProperty()
                .bind(Bindings.createBooleanBinding(() -> logEntryProperty.get() == null, logEntryProperty));
        replyButton.disableProperty()
                .bind(Bindings.createBooleanBinding(() -> logEntryProperty.get() == null, logEntryProperty));
        singleLogEntryDisplay.visibleProperty().bind(Bindings
                .createBooleanBinding(() -> !showLogGroup.get() && logEntryProperty.get() != null,
                        showLogGroup, logEntryProperty));
        mergedLogEntryDisplay.visibleProperty().bind(Bindings
                .createBooleanBinding(() -> showLogGroup.get() && logEntryProperty.get() != null,
                        showLogGroup, logEntryProperty));
        showHideLogEntryGroupButton.selectedProperty().bindBidirectional(showLogGroup);
        toolBar.setVisible(LogbookUIPreferences.log_entry_groups_support);
        toolBar.setManaged(LogbookUIPreferences.log_entry_groups_support);
    }

    @FXML
    public void reply() {
        LogEntry logEntry = logEntryProperty.get();
        Property logGroupProperty = logEntry.getProperty(LogGroupProperty.NAME);
        if (logGroupProperty == null) {
            logGroupProperty = LogGroupProperty.create();
            logEntry.getProperties().add(logGroupProperty);
        }
        OlogLog ologLog = new OlogLog();
        ologLog.setTitle(logEntry.getTitle());
        ologLog.setTags(logEntry.getTags());
        ologLog.setLogbooks(logEntry.getLogbooks());
        ologLog.setProperties(logEntry.getProperties());
        ologLog.setLevel(logEntry.getLevel());

        // Show a new editor dialog. When user selects to save the reply entry, update the original log entry
        // to ensure that it contains the log group property.
        new LogEntryEditorStage(DockPane.getActiveDockPane(), ologLog, l -> {
            try {
                // NOTE: source is set to null, while description is set to the log entry source. Reason is that
                // the description field is the contents of the editor text area in the original log entry. This
                // is processed on the server such that the description is set as source, while the description is
                // generated as a plain text variant of the source markup content.
                ((OlogLog)logEntry).setDescription(logEntry.getSource());
                ((OlogLog)logEntry).setSource(null);
                logClient.updateLogEntry(logEntry);
            } catch (LogbookException e) {
                logger.log(Level.SEVERE, "Failed to update log entry id=" + logEntry.getId(), e);
                return;
            }
        }).show();
    }

    public void setLogEntry(LogEntry logEntry) {
        logEntryProperty.set(logEntry);
        singleLogEntryDisplayController.setLogEntry(logEntry);
        if(LogbookUIPreferences.log_entry_groups_support){
            boolean hasLinkedEntries = logEntry.getProperties()
                    .stream().anyMatch(p -> p.getName().equals(LogGroupProperty.NAME));
            showHideLogEntryGroupButton.setDisable(!hasLinkedEntries);
            if(hasLinkedEntries){
                mergedLogEntryDisplayController.setLogEntry(logEntry);
            }
            // When a new log entry is selected, show the single record view
            showLogGroup.set(false);
        }
    }

    public LogEntry getLogEntry() {
        return logEntryProperty.get();
    }
}
