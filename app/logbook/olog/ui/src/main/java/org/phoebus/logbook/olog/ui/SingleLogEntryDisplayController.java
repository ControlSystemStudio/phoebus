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
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.LogbookException;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.Tag;
import org.phoebus.olog.es.api.model.OlogAttachment;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.web.HyperLinkRedirectListener;

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
    WebView webView;

    private WebEngine webEngine;

    @FXML
    public TitledPane attachmentsPane;
    @FXML
    public AttachmentsViewController attachmentsViewController;

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
    private final LogClient logClient;

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

        webEngine = webView.getEngine();
        // This will make links clicked in the WebView to open in default browser.
        webEngine.getLoadWorker().stateProperty().addListener(new HyperLinkRedirectListener(webView));
    }

    public void setLogEntry(LogEntry entry) {
        logEntry = entry;

        // Set the log entry for the attachments view.
        attachmentsViewController.invalidateAttachmentList(logEntry);
        // Download attachments from service
        fetchAttachments();
        // Always expand properties pane.
        attachmentsPane.setExpanded(true);

        List<String> hiddenPropertiesNames = Arrays.asList(LogbookUIPreferences.hidden_properties);
        // Remove the hidden properties
        List<Property> propertiesToShow =
                entry.getProperties().stream().filter(property -> !hiddenPropertiesNames.contains(property.getName())).collect(Collectors.toList());
        propertiesController.setProperties(propertiesToShow);

        logTime.setText(SECONDS_FORMAT.format(entry.getCreatedDate()));
        logOwner.setText(logEntry.getOwner());

        logTitle.setWrapText(true);
        logTitle.setText(entry.getTitle());

        webEngine.setUserStyleSheetLocation(getClass()
                .getResource("/detail_log_webview.css").toExternalForm());


        if (entry.getSource() != null) {
            webEngine.loadContent(getFullHtml(entry.getSource()));
        } else if (entry.getDescription() != null) {
            webEngine.loadContent(getFullHtml(entry.getDescription()));
        }
        ObservableList<String> logbookList = FXCollections.observableArrayList();
        logbookList.addAll(entry.getLogbooks().stream().map(Logbook::getName).collect(Collectors.toList()));

        ObservableList<String> tagList = FXCollections.observableArrayList();
        tagList.addAll(entry.getTags().stream().map(Tag::getName).collect(Collectors.toList()));


        if (!entry.getLogbooks().isEmpty()) {
            logbooks.setWrapText(false);
            logbooks.setText(entry.getLogbooks().stream().map(Logbook::getName).collect(Collectors.joining(",")));
        }

        if (!entry.getTags().isEmpty()) {
            tags.setText(entry.getTags().stream().map(Tag::getName).collect(Collectors.joining(",")));
        } else {
            tags.setText(null);
        }

        logEntryId.setText(Long.toString(entry.getId()));
        level.setText(entry.getLevel());
    }

    /**
     * Copies the URL of the log entry. The URL can be used to direct non-Phoebus clients to
     * the HTML representation as served by the web client, see
     * <a href="https://github.com/Olog/phoebus-olog-web-client">Phoebus Olog on Github</a>
     */
    @FXML
    public void copyURL() {
        final ClipboardContent content = new ClipboardContent();
        content.putString(LogbookUIPreferences.web_client_root_URL + "/" + logEntry.getId());
        Clipboard.getSystemClipboard().setContent(content);
    }

    /**
     * Retrieves the actual attachments from the remote service and copies them to temporary files. Attachments
     * should be retrieved when user requests to see the details, not in connection to a log entry search.
     */
    private void fetchAttachments() {
        JobManager.schedule("Fetch attachment data", monitor -> {
            Collection<Attachment> attachments = logEntry.getAttachments().stream()
                    .filter((attachment) -> attachment.getName() != null && !attachment.getName().isEmpty())
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
                                    .log(Level.WARNING, "Failed to retrieve attachment " + fileAttachment.getFileName(), e);
                        }
                        return fileAttachment;
                    }).collect(Collectors.toList());
            // Update UI
            attachmentsViewController.setAttachments(FXCollections.observableArrayList(attachments));
        });
    }

    private String getFullHtml(String commonmarkString) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("<html><body><div class='olog'>");
        stringBuffer.append(toHtml(commonmarkString));
        stringBuffer.append("</div></body></html>");

        return stringBuffer.toString();
    }
}
