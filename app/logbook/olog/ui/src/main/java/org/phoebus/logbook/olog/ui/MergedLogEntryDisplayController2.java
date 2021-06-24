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
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.image.attributes.ImageAttributesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.phoebus.framework.nls.NLS;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.olog.es.api.model.LogGroupProperty;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MergedLogEntryDisplayController2 {

    @FXML
    private ListView<LogEntry> listView;

    private LogClient logClient;

    private ObservableList<LogEntry> logEntries = FXCollections.observableArrayList();

    private Logger logger = Logger.getLogger(MergedLogEntryDisplayController2.class.getName());

    public MergedLogEntryDisplayController2(LogClient logClient) {
        this.logClient = logClient;
    }

    @FXML
    public void initialize() {
        listView.setCellFactory(c -> new LogEntryCell());
        //listView.setId("log-entry-list");
        //listView.getStylesheets().add(MergedLogEntryDisplayController2.class.getResource("/log_entry_group_list.css").toExternalForm());

    }

    /**
     * Set the selected entry containing the log group property to be used for finding
     * other entries in the group.
     *
     * @param logEntry The log entry selected by user in the table/list view.
     */
    public void setLogEntry(LogEntry logEntry) {
        String id =
                logEntry.getProperties().stream()
                        .filter(p -> p.getName().equals(LogGroupProperty.NAME)).findFirst().get().getAttributes().get("id");

        logger.log(Level.INFO, "Fetching log entries for group " + id);
        try {
            Map<String, String> mMap = new HashMap<>();
            mMap.put("properties", LogGroupProperty.NAME + ".id." + id);
            logEntries.setAll(logClient.findLogs(mMap));
            List<LogEntry> d = listView.itemsProperty().get();
            listView.itemsProperty().set(logEntries);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unable to locate log entry items using log entry group id " + id, e);
        }

    }

    private class LogEntryCell extends ListCell<LogEntry> {

        private Node graphic;
        private LogEntryGroupCellController controller;

        public LogEntryCell() {
            super();

            try {
                ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
                FXMLLoader loader = new FXMLLoader();
                loader.setResources(resourceBundle);
                loader.setLocation(this.getClass().getResource("LogEntryGroupCell.fxml"));
                loader.setControllerFactory(clazz -> {
                    try {
                        if(clazz.isAssignableFrom(LogEntryGroupCellController.class)) {
                            return clazz.getConstructor(String.class).newInstance(logClient.getServiceUrl());
                        }
                        else if(clazz.isAssignableFrom(AttachmentsPreviewController.class)) {
                            return clazz.getConstructor().newInstance();
                        }
                        else if(clazz.isAssignableFrom(LogPropertiesController.class)) {
                            return clazz.getConstructor().newInstance();
                        }
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Unable to load fxml", e);
                    }
                    return null;
                });
                graphic = loader.load();
                controller = loader.getController();
            } catch (IOException exc) {
                throw new RuntimeException(exc);
            }
        }

        @Override
        protected void updateItem(LogEntry logEntry, boolean empty) {
            super.updateItem(logEntry, empty);
            if (!empty) {
                controller.setLogEntry(logEntry);
                setGraphic(graphic);
            }
            else{
                setGraphic(null);
            }
        }
    }
}
