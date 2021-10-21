package org.phoebus.logbook.olog.ui;

import javafx.application.Platform;
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
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.LogbookException;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.Tag;
import org.phoebus.olog.es.api.model.OlogAttachment;
import org.phoebus.olog.es.api.model.OlogLog;
import org.phoebus.ui.javafx.ImageCache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private LogClient logClient;

    public SingleLogEntryDisplayController(LogClient logClient) {
        super(logClient.getServiceUrl());
        this.logClient = logClient;
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
        // Get the attachments from service
        fetchAttachments();
        //attachmentsPreviewController
        //        .setAttachments(FXCollections.observableArrayList(logEntry.getAttachments()));

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

    /**
     * Retrieves the actual attachments from the remote service and copies them to temporary files. The idea is that attachments
     * should be retrieved when user requests to see the details, not in connection to a log entry search.
     * @return A {@link Collection} of {@link Attachment}s holding the attachment content.
     */
    private void fetchAttachments(){
        JobManager.schedule("Fetch attachment data", monitor -> {
            Collection<Attachment> attachments = logEntry.getAttachments().stream()
                    .filter( (attachment) -> {
                        return attachment.getName() != null && !attachment.getName().isEmpty();
                    })
                    .map((attachment) -> {
                        OlogAttachment fileAttachment = new OlogAttachment();
                        fileAttachment.setContentType(attachment.getContentType());
                        fileAttachment.setThumbnail(false);
                        fileAttachment.setFileName(attachment.getName());
                        try {
                            Path temp = Files.createTempFile("phoebus", attachment.getName());
                            Files.copy(logClient.getAttachment(logEntry.getId(), attachment.getName()), temp, StandardCopyOption.REPLACE_EXISTING);
                            fileAttachment.setFile(temp.toFile());
                            temp.toFile().deleteOnExit();
                        } catch (LogbookException | IOException e) {
                            Logger.getLogger(SingleLogEntryDisplayController.class.getName())
                                    .log(Level.WARNING, "Failed to retrieve attachment " + fileAttachment.getFileName() ,e);
                        }
                        return fileAttachment;
                    }).collect(Collectors.toList());
            // Update the log entry attachments object
            ((OlogLog)logEntry).setAttachments(attachments);
            // Update UI
            Platform.runLater(() -> attachmentsPreviewController.setAttachments(FXCollections.observableArrayList(logEntry.getAttachments())));
        });
    }
}
