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
import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.workbench.Locations;
import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.LogbookException;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.Tag;
import org.phoebus.olog.es.api.model.OlogAttachment;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.web.HyperLinkRedirectListener;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
    @SuppressWarnings("unused")
    private Node updatedIndicator;

    private WebEngine webEngine;

    @FXML
    public TitledPane attachmentsPane;
    @FXML
    public AttachmentsViewController attachmentsViewController;

    @FXML
    public VBox properties;
    @FXML
    public LogPropertiesController propertiesController;
    @FXML
    @SuppressWarnings("unused")
    private ImageView logbookIcon;
    @FXML
    @SuppressWarnings("unused")
    private Label logbooks;
    @FXML
    @SuppressWarnings("unused")
    private ImageView tagIcon;
    @FXML
    @SuppressWarnings("unused")
    private Label tags;
    @FXML
    @SuppressWarnings("unused")
    private Label logEntryId;
    @FXML
    @SuppressWarnings("unused")
    private Label level;

    @FXML
    @SuppressWarnings("unused")
    private Button copyURLButton;

    private LogEntry logEntry;
    private final LogClient logClient;

    private final SimpleBooleanProperty logEntryUpdated = new SimpleBooleanProperty();

    private Optional<Consumer<Long>> selectLogEntryInUI = Optional.empty();

    private Job fetchAttachmentsJob;

    private final Logger logger = Logger.getLogger(SingleLogEntryDisplayController.class.getName());

    public SingleLogEntryDisplayController(LogClient logClient) {
        super(logClient.getServiceUrl());
        this.logClient = logClient;
    }

    public void setSelectLogEntryInUI(Consumer<Long> selectLogEntryInUI) {
        this.selectLogEntryInUI = Optional.of(selectLogEntryInUI);
    }

    @FXML
    public void initialize() {

        logbookIcon.setImage(logbook);
        tagIcon.setImage(tag);

        copyURLButton.visibleProperty().setValue(LogbookUIPreferences.web_client_root_URL != null
                && !LogbookUIPreferences.web_client_root_URL.isEmpty());

        {
            Optional<String> webClientRoot = LogbookUIPreferences.web_client_root_URL == null || LogbookUIPreferences.web_client_root_URL.isEmpty() ? Optional.empty() : Optional.of(LogbookUIPreferences.web_client_root_URL);
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

        fetchAndSetAttachments();

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
        logbookList.addAll(entry.getLogbooks().stream().map(Logbook::getName).toList());

        ObservableList<String> tagList = FXCollections.observableArrayList();
        tagList.addAll(entry.getTags().stream().map(Tag::getName).toList());


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
    @SuppressWarnings("unused")
    public void copyURL() {
        final ClipboardContent content = new ClipboardContent();
        content.putString(LogbookUIPreferences.web_client_root_URL + "/" + logEntry.getId());
        Clipboard.getSystemClipboard().setContent(content);
    }

    /**
     * Retrieves attachments from the remote service and copies them to temporary files. Attachments
     * should be retrieved when user selects a log entry from search result list. Note that this
     * method also updates the attachments view once files have been downloaded.
     */
    private void fetchAndSetAttachments() {
        // Cancel ongoing job, if any.
        if (fetchAttachmentsJob != null) {
            fetchAttachmentsJob.cancel();
        }
        // No attachments...
        if (logEntry.getAttachments().isEmpty()) {
            return;
        }
        File attachmentsDirectoryOptional;
        try {
            attachmentsDirectoryOptional = checkAttachmentsDirectory();
        } catch (LogbookException e) {
            ExceptionDetailsErrorDialog.openError(Messages.AttachmentsNoStorage, e);
            return;
        }
        final File attachmentsDirectory = attachmentsDirectoryOptional;

        fetchAttachmentsJob = JobManager.schedule("Fetch attachment data", monitor -> {
            // Order attachments such that the list view looks the same in the list view if user returns to the log entry
            List<Attachment> sorted = logEntry.getAttachments().stream().sorted(Comparator.comparing(Attachment::getName)).toList();
            List<Attachment> attachmentList = new ArrayList<>();
            for (Attachment attachment : sorted) {
                if (monitor.isCanceled()) {
                    break;
                } else if (attachment.getName() == null || attachment.getName().isEmpty()) {
                    continue;
                }
                OlogAttachment fileAttachment = new OlogAttachment();
                fileAttachment.setContentType(attachment.getContentType());
                fileAttachment.setThumbnail(false);
                fileAttachment.setFileName(attachment.getName());
                // Determine file extension, needed to support transition to Image Viewer app for image attachments
                String fileExtension = "";
                int indexOfLastDot = attachment.getName().lastIndexOf('.');
                if (indexOfLastDot > -1) {
                    fileExtension = attachment.getName().substring(indexOfLastDot);
                }

                // Attachment file may already exist on disk, e.g. user has already viewed the log entry.

                File attachmentFile = new File(attachmentsDirectory, attachment.getId() + fileExtension);
                if (!attachmentFile.exists()) {
                    InputStream stream = logClient.getAttachment(logEntry.getId(), attachment.getName());
                    if(stream != null) {
                        Files.copy(stream, attachmentFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        attachmentFile.deleteOnExit();
                    }
                }
                fileAttachment.setFile(attachmentFile);
                attachmentList.add(fileAttachment);
            }
            // If job is cancelled, skip setting the list of attachments
            if (!monitor.isCanceled()) {
                attachmentsViewController.addAttachments(attachmentList);
            }
        });
    }

    private String getFullHtml(String commonmarkString) {
        return "<html><body><div class='olog'>" +
                toHtml(commonmarkString) +
                "</div></body></html>";
    }

    private void handle(MouseEvent me) {
        new ArchivedLogEntriesManager(logClient).handle(webView, logEntry);
    }

    /**
     * Checks if attachments directory exists. If not, an attempt is made to create it. Corner case:
     * a file named log-attachments exists.
     * <p>
     * User is presented with error dialog if the attachments directory cannot be created or determined.
     * </p>
     *
     * @return A non-empty {@link Optional} if the directory exists or if created successfully.
     * @throws LogbookException if directory could not be created, if it exists but is not a directory,
     *                     or if it exists but is not writable.
     */
    private File checkAttachmentsDirectory() throws LogbookException {
        File attachmentsDirectory = new File(Locations.user(), "log-attachments");
        if (!attachmentsDirectory.exists()) {
            logger.log(Level.INFO, "Attachments directory \"" + attachmentsDirectory.getAbsolutePath() + "\" does not exist, creating it");
            if (!attachmentsDirectory.mkdir()) {
                throw new LogbookException(MessageFormat.format(Messages.AttachmentsDirectoryFailedCreate, attachmentsDirectory.getAbsolutePath()));
            }
        } else if (!attachmentsDirectory.isDirectory()) {
            throw new LogbookException(MessageFormat.format(Messages.AttachmentsFileNotDirectory, attachmentsDirectory.getAbsolutePath()));
        } else if (!attachmentsDirectory.canWrite()) {
            throw new LogbookException(MessageFormat.format(Messages.AttachmentsDirectoryNotWritable, attachmentsDirectory.getAbsolutePath()));
        }
        return attachmentsDirectory;
    }

}
