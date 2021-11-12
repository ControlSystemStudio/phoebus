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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.olog.es.api.model.LogGroupProperty;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.phoebus.util.time.TimestampFormats.SECONDS_FORMAT;

/**
 * Controller for the merged log entry view. The idea is to create a single view of all the entries
 * in a log entry group such that the entire content of the group (aka thread) can be read without the
 * need to select the individual log entries from a list.
 * <p>
 * The attachments and properties of the individual log entries are intentionally left out as the
 * purpose is to read a sequence of text, and not to show all details.
 * <p>
 * Embedded attachments (i.e. images) are maintained in the merged view, as are - of course - other
 * Markdown elements like lists or tables.
 */
public class MergedLogEntryDisplayController extends HtmlAwareController {

    @FXML
    WebView logDescription;

    private final LogClient logClient;

    private final ObservableList<LogEntry> logEntries = FXCollections.observableArrayList();

    private final Logger logger = Logger.getLogger(MergedLogEntryDisplayController.class.getName());

    private WebEngine webEngine;

    /** for communication to the Javascript engine. */
    private JSObject javascriptConnector;

    /** for communication from the Javascript engine. */
    private JavaConnector javaConnector = new JavaConnector();

    private Function<LogEntry, Void> logSelectionHandler;

    public MergedLogEntryDisplayController(LogClient logClient) {
        super(logClient.getServiceUrl());
        this.logClient = logClient;
    }

    @FXML
    public void initialize() {
        // Content is defined by the source (default) or description field. If both are null
        // or empty, do no load any content to the WebView.
        webEngine = logDescription.getEngine();
        webEngine.setUserStyleSheetLocation(getClass()
                .getResource("/detail_log_webview.css").toExternalForm());
        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (Worker.State.SUCCEEDED == newValue) {
                // set an interface object named 'javaConnector' in the web engine's page
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaConnector", javaConnector);
            }
        });
    }

    public void setLogSelectionHandler(Function<LogEntry, Void> handler){
        this.logSelectionHandler = handler;
    }

    /**
     * Set the selected entry containing the log group property to be used for finding
     * other entries in the group. Note that this will query the service
     * such that all log records in the group are retrieved, irrespective of how
     * the specified log record was obtained.
     *
     * @param logEntry The log entry selected by user in the table/list view.
     */
    public void setLogEntry(LogEntry logEntry) {
        getLogEntries(logEntry);
    }

    private void mergeAndRender() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<html><body>");
        logEntries.forEach(l -> {
            stringBuilder.append(createSeparator(l));
            stringBuilder.append("<div class='olog-merged'>");
            stringBuilder.append(toHtml(l.getSource()));
            stringBuilder.append("</div>");
        });
        stringBuilder.append("</body><html>");
        webEngine.loadContent(stringBuilder.toString());
    }

    /**
     * Creates the log entry separator item inserted as a header for each log entry
     * when showing the log group view.
     *
     * @param logEntry
     * @return
     */
    private String createSeparator(LogEntry logEntry) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<div class='separator' onClick='window.javaConnector.toLowerCase(" + logEntry.getId() + ")'>");
        stringBuilder.append(SECONDS_FORMAT.format(logEntry.getCreatedDate())).append(", ");
        stringBuilder.append(logEntry.getOwner()).append(", ");
        stringBuilder.append(logEntry.getTitle());
        stringBuilder.append("<div style='float: right;'>").append(logEntry.getId()).append("</div>");
        stringBuilder.append("</div>");
        return stringBuilder.toString();
    }

    private void getLogEntries(LogEntry logEntry) {

        String id =
                logEntry.getProperties().stream()
                        .filter(p -> p.getName().equals(LogGroupProperty.NAME)).findFirst().get().getAttributes().get(LogGroupProperty.ATTRIBUTE_ID);
        logger.log(Level.INFO, "Fetching log entries for group " + id);
        try {
            Map<String, String> mMap = new HashMap<>();
            mMap.put("properties", LogGroupProperty.NAME + ".id." + id);
            logEntries.setAll(logClient.findLogs(mMap));
            logEntries.sort(Comparator.comparing(LogEntry::getCreatedDate));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unable to locate log entry items using log entry group id " + id, e);
        }

        mergeAndRender();
    }

    public class JavaConnector {
        /**
         * called when the JS side wants a String to be converted.
         *
         * @param value
         *         the String to convert
         */
        public void toLowerCase(String value) {
            LogEntry logEntry = logEntries.stream().filter(l -> Long.toString(l.getId()).equals(value)).findFirst().get();
            logSelectionHandler.apply(logEntry);
        }
    }
}
