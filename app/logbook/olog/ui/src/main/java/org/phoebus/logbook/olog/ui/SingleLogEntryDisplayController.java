package org.phoebus.logbook.olog.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.Tag;
import org.phoebus.ui.javafx.ImageCache;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.phoebus.util.time.TimestampFormats.SECONDS_FORMAT;

public class SingleLogEntryDisplayController extends HtmlAwareController {

    static final Image tag = ImageCache.getImage(SingleLogEntryDisplayController.class, "/icons/add_tag.png");
    static final Image logbook = ImageCache.getImage(SingleLogEntryDisplayController.class, "/icons/logbook-16.png");

    @FXML
    Label logTime;
    @FXML
    Label logOwner;
    @FXML
    Label logTitle;
    @FXML
    WebView logDescription;

    @FXML
    public TitledPane attachmentsPane;
    @FXML
    public AttachmentsPreviewController attachmentsPreviewController;

    @FXML
    public TitledPane propertiesPane;
    @FXML
    public VBox properties;
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
    private Button copyURLButton;

    private LogEntry logEntry;

    public SingleLogEntryDisplayController(String serviceUrl) {
        super(serviceUrl);
    }


    @FXML
    public void initialize() {

        logbookIcon.setImage(logbook);
        tagIcon.setImage(tag);

        copyURLButton.visibleProperty().setValue(LogbookUIPreferences.web_client_root_URL != null
                && !LogbookUIPreferences.web_client_root_URL.isEmpty());
    }

    public void setLogEntry(LogEntry logEntry) {
        this.logEntry = logEntry;

        // Always expand properties pane.
        attachmentsPane.setExpanded(true);
        attachmentsPreviewController
                .setAttachments(FXCollections.observableArrayList(logEntry.getAttachments()));

        List<String> hiddenPropertiesNames = Arrays.asList(LogbookUIPreferences.hidden_properties);
        // Remove the hidden properties
        List<Property> propertiesToShow =
                logEntry.getProperties().stream().filter(property -> !hiddenPropertiesNames.contains(property.getName())).collect(Collectors.toList());
        propertiesController.setProperties(propertiesToShow);

        logTime.setText(SECONDS_FORMAT.format(logEntry.getCreatedDate()));
        logOwner.setText(logEntry.getOwner());

        logTitle.setWrapText(true);
        logTitle.setText(logEntry.getTitle());

        // Content is defined by the source (default) or description field. If both are null
        // or empty, do no load any content to the WebView.
        WebEngine webEngine = logDescription.getEngine();
        webEngine.setUserStyleSheetLocation(getClass()
                .getResource("/detail_log_webview.css").toExternalForm());


        if (logEntry.getSource() != null) {
            webEngine.loadContent(toHtml(logEntry.getSource()));
        } else if (logEntry.getDescription() != null) {
            webEngine.loadContent(toHtml(logEntry.getDescription()));
        }
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
}
