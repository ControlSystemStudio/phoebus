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
import javafx.scene.Node;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.image.attributes.ImageAttributesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.olog.es.api.model.LogGroupProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.phoebus.util.time.TimestampFormats.SECONDS_FORMAT;

public class MergedLogEntryDisplayController {

    @FXML
    private Node attachmentsPane;
    @FXML
    WebView logDescription;
    private LogClient logClient;

    private HtmlRenderer htmlRenderer;
    private Parser parser;

    private ObservableList<LogEntry> logEntries = FXCollections.observableArrayList();

    private Logger logger = Logger.getLogger(MergedLogEntryDisplayController.class.getName());

    public MergedLogEntryDisplayController(LogClient logClient) {
        this.logClient = logClient;
    }

    @FXML
    public void initialize(){
        List<Extension> extensions = Arrays.asList(TablesExtension.create(), ImageAttributesExtension.create());
        parser = Parser.builder().extensions(extensions).build();
        htmlRenderer = HtmlRenderer.builder()
                .attributeProviderFactory(context -> new OlogAttributeProvider(logClient.getServiceUrl()))
                .extensions(extensions).build();

        // Content is defined by the source (default) or description field. If both are null
        // or empty, do no load any content to the WebView.
        WebEngine webEngine = logDescription.getEngine();
        webEngine.setUserStyleSheetLocation(getClass()
                .getResource("/detail-log-webview.css").toExternalForm());
    }

    @FXML
    public void downloadSelectedAttachments() {

    }

    /**
     * Set the selected entry containing the log group property to be used for finding
     * other entries in the group.
     * @param logEntry The log entry selected by user in the table/list view.
     */
    public void setLogEntry(LogEntry logEntry){
        getLogEntries(logEntry);
    }

    private void mergeAndRender(LogEntry selectedLogEntry) {
        List<Attachment> attachments = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();
        logEntries.stream().forEach(l -> {
            stringBuilder.append(createSeparator(l));
            if(l.getId().equals(selectedLogEntry.getId())){
                stringBuilder.append("<div class='selected-log-entry'>");
            }
            stringBuilder.append(toHtml(l.getSource()));
            if(l.getId().equals(selectedLogEntry.getId().longValue())){
                stringBuilder.append("</div>");
            }
            attachments.addAll(l.getAttachments());
        });
        WebEngine webEngine = logDescription.getEngine();
        webEngine.setUserStyleSheetLocation(getClass()
                .getResource("/detail-log-webview.css").toExternalForm());
        webEngine.loadContent(stringBuilder.toString());

        if(attachments.isEmpty()){
            attachmentsPane.setManaged(false);
            attachmentsPane.setVisible(false);
        }
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
        stringBuilder.append("<div class='separator'>");
        stringBuilder.append(SECONDS_FORMAT.format(logEntry.getCreatedDate())).append(", ");
        stringBuilder.append(logEntry.getOwner()).append(", ");
        stringBuilder.append(logEntry.getTitle());
        //stringBuilder.append(logEntry.getId());
        stringBuilder.append("</div>");
        return stringBuilder.toString();
    }

    /**
     * Converts Commonmark content to HTML.
     *
     * @param commonmarkString Raw Commonmark string
     * @return The HTML output of the Commonmark processor.
     */
    private String toHtml(String commonmarkString) {
        org.commonmark.node.Node document = parser.parse(commonmarkString);
        String html = htmlRenderer.render(document);
        // Wrap the content in a named div so that a suitable height may be determined.
        return "<div id='olog'>\n" + html + "</div>";
    }

    private void getLogEntries(LogEntry logEntry) {

        String id =
                logEntry.getProperties().stream()
                        .filter(p -> p.getName().equals(LogGroupProperty.NAME)).findFirst().get().getAttributes().get("id");
        logger.log(Level.INFO, "Fetching log entries for group " + id);
        try {
            Map<String, String> mMap = new HashMap<>();
            mMap.put("properties", LogGroupProperty.NAME + ".id." + id);
            logEntries.setAll(logClient.findLogs(mMap));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unable to locate log entry items using log entry group id " + id, e);
        }

        mergeAndRender(logEntry);
    }
}
