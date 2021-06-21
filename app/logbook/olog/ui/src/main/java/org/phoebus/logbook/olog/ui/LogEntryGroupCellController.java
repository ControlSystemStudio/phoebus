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

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSException;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.image.attributes.ImageAttributesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.Tag;
import org.phoebus.ui.javafx.ImageCache;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.phoebus.util.time.TimestampFormats.SECONDS_FORMAT;

public class LogEntryGroupCellController {

    static final Image tag = ImageCache.getImage(SingleLogEntryDisplayController.class, "/icons/add_tag.png");
    static final Image logbook = ImageCache.getImage(SingleLogEntryDisplayController.class, "/icons/logbook-16.png");

    private HtmlRenderer htmlRenderer;
    private Parser parser;

    @FXML
    private VBox root;
    @FXML
    private AttachmentsPreviewController attachmentsPreviewController;
    @FXML
    public LogPropertiesController propertiesController;
    @FXML
    private ImageView logbookIcon;
    @FXML
    private Label logbooks;
    @FXML
    private ImageView tagIcon;
    @FXML
    private Label tags;
    @FXML
    private Label logEntryId;
    @FXML
    private Label level;
    @FXML
    public TitledPane propertiesPane;
    @FXML
    public TitledPane attachmentsPane;
    @FXML
    public TitledPane headerPane;
    @FXML
    private Label logTime;
    @FXML
    private Label logOwner;
    @FXML
    private Label logTitle;
    @FXML
    private WebView webView;
    @FXML
    private Button copyURLButton;
    @FXML
    private LogEntryHeaderController logEntryHeaderController;

    private LogClient logClient;
    private LogEntry logEntry;
    private WebEngine webEngine;

    private Logger logger = Logger.getLogger(LogEntryGroupCellController.class.getName());

    public LogEntryGroupCellController(LogClient logClient){
        this.logClient = logClient;
    }

    @FXML
    public void initialize(){
        logbookIcon.setImage(logbook);
        tagIcon.setImage(tag);
        copyURLButton.visibleProperty().setValue(LogbookUIPreferences.web_client_root_URL != null
                && !LogbookUIPreferences.web_client_root_URL.isEmpty());
        /*
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(this.getClass().getResource("LogEntryHeader.fxml"));
            Node header = loader.load();
            logEntryHeaderController = loader.getController();
            headerPane.setGraphic(header);
            String stylesheet =
                    LogEntryGroupCellController.class.getResource("/titled_pane_customization.css").toExternalForm();
            headerPane.getStylesheets().add(stylesheet);
            headerPane.getStyleClass().add("log-entry-header");
            attachmentsPane.getStylesheets().add(stylesheet);
            attachmentsPane.getStyleClass().add("attachment-properties-header");
            propertiesPane.getStylesheets().add(stylesheet);
            propertiesPane.getStyleClass().add("attachment-properties-header");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load log entry header fxml", e);
        }

         */
        List<Extension> extensions = Arrays.asList(TablesExtension.create(), ImageAttributesExtension.create());
        parser = Parser.builder().extensions(extensions).build();
        htmlRenderer = HtmlRenderer.builder()
                .attributeProviderFactory(context -> new OlogAttributeProvider(logClient.getServiceUrl()))
                .extensions(extensions).build();

        // Content is defined by the source (default) or description field. If both are null
        // or empty, do no load any content to the WebView.
        webEngine = webView.getEngine();
        webEngine.setUserStyleSheetLocation(getClass()
                .getResource("/detail-log-webview.css").toExternalForm());
        webEngine.loadContent("MUSIGNY");

    }

    /**
     * Copies the URL of the log entry. The URL can be used to direct non-Phoebus clients to
     * the HTML representation as served by the web client, see
     * https://github.com/Olog/phoebus-olog-web-client
     */
    @FXML
    public void copyURL() {
        final ClipboardContent content = new ClipboardContent();
        content.putString(LogbookUIPreferences.web_client_root_URL + "/" + logEntry.getId());
        Clipboard.getSystemClipboard().setContent(content);
    }

    public void setLogEntry(LogEntry logEntry) {
        this.logEntry = logEntry;

        if(logEntry.getAttachments() == null || logEntry.getAttachments().isEmpty()){
            attachmentsPane.setVisible(false);
            attachmentsPane.setManaged(false);
        }
        else{
            attachmentsPane.setExpanded(logEntry.getAttachments() != null && !logEntry.getAttachments().isEmpty());
            attachmentsPane.setVisible(logEntry.getAttachments() != null && !logEntry.getAttachments().isEmpty());
            if(!logEntry.getAttachments().isEmpty()){
                attachmentsPreviewController
                        .setAttachments(FXCollections.observableArrayList(logEntry.getAttachments()));
            }
            attachmentsPane.setManaged(true);
        }


        List<String> hiddenPropertiesNames = Arrays.asList(LogbookUIPreferences.hidden_properties);
        // Remove the hidden properties
        List<Property> propertiesToShow =
                logEntry.getProperties().stream().filter(property -> !hiddenPropertiesNames.contains(property.getName())).collect(Collectors.toList());
        if(propertiesToShow.isEmpty()){
            propertiesPane.setVisible(false);
            propertiesPane.setManaged(false);
        }
        else{
            propertiesPane.setExpanded(!propertiesToShow.isEmpty());
            propertiesPane.setVisible(!propertiesToShow.isEmpty());
            if (!propertiesToShow.isEmpty()) {
                propertiesController.setProperties(propertiesToShow);
            }
            propertiesPane.setManaged(true);
        }


        logTime.setText(SECONDS_FORMAT.format(logEntry.getCreatedDate()));
        logOwner.setText(logEntry.getOwner());

        logTitle.setWrapText(true);
        logTitle.setText(logEntry.getTitle());

        /*

        if (logEntry.getSource() != null) {
            webEngine.loadContent(toHtml(logEntry.getSource()));
        } else if (logEntry.getDescription() != null) {
            webEngine.loadContent(toHtml(logEntry.getDescription()));
        }


         */


        ObservableList<String> logbookList = FXCollections.observableArrayList();
        logbookList.addAll(logEntry.getLogbooks().stream().map(Logbook::getName).collect(Collectors.toList()));

        ObservableList<String> tagList = FXCollections.observableArrayList();
        tagList.addAll(logEntry.getTags().stream().map(Tag::getName).collect(Collectors.toList()));

        if (!logEntry.getLogbooks().isEmpty()) {
            logbooks.setWrapText(false);
            logbooks.setText(logEntry.getLogbooks().stream().map(Logbook::getName).collect(Collectors.joining(",")));
        }

        if (!logEntry.getTags().isEmpty()) {
            tags.setText(logEntry.getTags().stream().map(Tag::getName).collect(Collectors.joining(",")));
        } else {
            tags.setText(null);
        }

        logEntryId.setText(Long.toString(logEntry.getId()));
        level.setText(logEntry.getLevel());

        logEntryHeaderController.setLogEntry(logEntry);
    }

    public void expandHeader(boolean expand){
        headerPane.setExpanded(expand);
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

    private void adjustHeight() {
        Platform.runLater(new Runnable(){
            @Override
            public void run() {
                try {
                    Object result = webEngine.executeScript(
                            "document.getElementById('root').offsetHeight");
                    if(result instanceof Integer) {
                        Integer i = (Integer) result;
                        double height = new Double(i);
                        height = height + 20;
                        webView.setPrefHeight(height);
                    }
                } catch (JSException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
