/*
 * Copyright (C) 2023 European Spallation Source ERIC.
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
 *
 */

package org.phoebus.logbook.olog.ui;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.SearchResult;
import org.phoebus.ui.application.ApplicationLauncherService;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class downloads archived log entries and then prompt user for a file in which the
 * list of log entries is written as pretty printed JSON.
 *
 * If an external application has been configured for extension json, it will be launched once the
 * file has been saved successfully.
 */
public class ArchivedLogEntriesManager {
    private LogClient logClient;

    public ArchivedLogEntriesManager(LogClient logClient) {
        this.logClient = logClient;
    }

    /**
     * Downloads archived log entries as a {@link org.phoebus.framework.jobs.Job} and then
     * launches a {@link FileChooser} such that user can specify target file name for the retrieved data.
     * <p>
     * The current {@link LogEntry} is written as first JSON object in the output file.
     *
     * @param ownerNode {@link Node} used by {@link FileChooser}.
     * @param logEntry  A (valid) log entry id shared among all archived entries with same id.
     */
    public void handle(Node ownerNode, LogEntry logEntry) {
        JobManager.schedule("Get Archived Log Entries", monitor -> {
            long logEntryId = logEntry.getId();
            SearchResult searchResult;
            try {
                searchResult = logClient.getArchivedEntries(logEntryId);
            } catch (Exception e) {
                Platform.runLater(() -> {
                    ExceptionDetailsErrorDialog.openError("Error", Messages.ArchivedDownloadFailed, e);
                });
                return;
            }
            if (searchResult.getHitCount() == 0) { // Should not happen!
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setHeaderText(MessageFormat.format(Messages.ArchivedNoEntriesFound, logEntryId));
                    alert.show();
                });
                return;
            }
            String fileName = "archived_log_entries_" + logEntryId + ".json";

            FileChooser fileChooser = new FileChooser();
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Json files (.json)", "*.json");
            fileChooser.getExtensionFilters().add(extFilter);
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            fileChooser.setInitialFileName(fileName);

            Platform.runLater(() -> {
                File destinationFile = fileChooser.showSaveDialog(ownerNode.getScene().getWindow());
                if (destinationFile != null) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                            .withZone(ZoneId.systemDefault());
                    JavaTimeModule javaTimeModule = new JavaTimeModule();
                    // Since this write to file that someone will read, format time accordingly...
                    javaTimeModule.addSerializer(Instant.class, new JsonSerializer<>() {
                        @Override
                        public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                            gen.writeString(formatter.format(value));
                        }
                    });

                    ObjectMapper objectMapper = new ObjectMapper().registerModule(javaTimeModule);
                    BufferedOutputStream writer = null;
                    try {
                        writer = new BufferedOutputStream(new FileOutputStream(destinationFile));
                        List<LogEntry> allLogEntries = new ArrayList<>();
                        allLogEntries.add(logEntry); // Current log entry is first element in array...
                        allLogEntries.addAll(searchResult.getLogs()); // ...add all archived log entries
                        writer.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(allLogEntries).getBytes());
                    } catch (Exception e) {
                        Logger.getLogger(ArchivedLogEntriesManager.class.getName())
                                .log(Level.WARNING, "Unable to save archived log entries to file", e);
                        Platform.runLater(() -> {
                            ExceptionDetailsErrorDialog.openError("Error", Messages.ArchivedSaveFailed, e);
                        });
                    } finally {
                        if (writer != null) {
                            // Launch viewer, if one has been configured for extension json
                            ApplicationLauncherService.openFile(destinationFile, false, null);
                            try {
                                writer.flush();
                                writer.close();
                            } catch (IOException e) {
                                // Ignore
                            }
                        }
                    }
                }
            });
        });
    }
}
