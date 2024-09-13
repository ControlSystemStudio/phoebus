package org.phoebus.logbook.olog.ui;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.logbook.*;
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
import java.util.Optional;
import java.util.function.Consumer;
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

    @FXML
    private Node updatedIndicator;

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

    private final SimpleBooleanProperty logEntryUpdated = new SimpleBooleanProperty();

    private Optional<Consumer<Long>> selectLogEntryInUI = Optional.empty();

    public SingleLogEntryDisplayController(LogClient logClient) {
        super(logClient.getServiceUrl());
        this.logClient = logClient;
    }

    public void setSelectLogEntryInUI(Consumer<Long> selectLogEntryInUI) {
        this.selectLogEntryInUI = Optional.of(id -> selectLogEntryInUI.accept(id));
    };

    @FXML
    public void initialize() {

        logbookIcon.setImage(logbook);
        tagIcon.setImage(tag);

        copyURLButton.visibleProperty().setValue(LogbookUIPreferences.web_client_root_URL != null
                && !LogbookUIPreferences.web_client_root_URL.isEmpty());

        {
            Optional<String> webClientRoot = LogbookUIPreferences.web_client_root_URL == null || LogbookUIPreferences.web_client_root_URL.equals("") ? Optional.empty() : Optional.of(LogbookUIPreferences.web_client_root_URL);
            webEngine = webView.getEngine();
            // This will make links clicked in the WebView to open in default browser.
            webEngine.getLoadWorker().stateProperty().addListener(new HyperLinkRedirectListener(webView, webClientRoot, selectLogEntryInUI));
        }

        updatedIndicator.visibleProperty().bind(logEntryUpdated);
        updatedIndicator.setOnMouseEntered(me -> updatedIndicator.setCursor(Cursor.HAND));
        updatedIndicator.setOnMouseClicked(this::handle);

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

        logEntryUpdated.set(logEntry.getModifiedDate() != null &&
                !logEntry.getModifiedDate().equals(logEntry.getCreatedDate()));
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
                        OlogAttachment fileAttachment = new OlogAttachment() {
                            @Override
                            protected void finalize() {
                                if (getFile() != null && getFile().exists()) {
                                    getFile().delete();
                                }
                            }
                        };
                        fileAttachment.setContentType(attachment.getContentType());
                        fileAttachment.setThumbnail(false);
                        fileAttachment.setFileName(attachment.getName());
                        // A bit of a hack here. The idea is to create a temporary file with a known name,
                        // i.e. without the random file name part.
                        // Files.createdTempFile does not support it, so a bit of workaround is needed.
                        try {
                            // This creates a temp file with a random part
                            Path random = Files.createTempFile(attachment.getId(),  attachment.getName());
                            // This does NOT create a file
                            Path nonRandom = random.resolveSibling(attachment.getId());
                            if(!Files.exists(nonRandom.toAbsolutePath())){
                                // Moves the temp file with random part to file with non-random part.
                                nonRandom = Files.move(random, nonRandom);
                                Files.copy(logClient.getAttachment(logEntry.getId(), attachment.getName()), nonRandom, StandardCopyOption.REPLACE_EXISTING);
                                nonRandom.toFile().deleteOnExit();
                            }
                            fileAttachment.setFile(nonRandom.toFile());
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

    private void handle(MouseEvent me) {
        new ArchivedLogEntriesManager(logClient).handle(webView, logEntry);
    }
}
