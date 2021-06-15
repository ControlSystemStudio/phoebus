package org.phoebus.logbook.olog.ui;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.image.attributes.ImageAttributesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.Tag;
import org.phoebus.olog.es.api.model.LogGroupProperty;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.phoebus.util.time.TimestampFormats.SECONDS_FORMAT;

public class SingleLogEntryDisplayController {

    static final Image tag = ImageCache.getImage(SingleLogEntryDisplayController.class, "/icons/add_tag.png");
    static final Image logbook = ImageCache.getImage(SingleLogEntryDisplayController.class, "/icons/logbook-16.png");
    private final LogClient logClient;

    private HtmlRenderer htmlRenderer;
    private Parser parser;

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
    private Button downloadButton;
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
    private Node attachmentsAndPropertiesPane;

    @FXML
    private Button copyURLButton;

    @FXML
    private GridPane headerPane;

    private Logger logger = Logger.getLogger(SingleLogEntryDisplayController.class.getName());

    /**
     * List of log entries identified by the log group property, see {@link LogGroupProperty}.
     * May initially be empty.
     */
    private List<LogEntry> logEntries;

    private LogEntry logEntry;

    /**
     * List of attachments selected in the preview's {@link ListView}.
     */

    private ObservableList<Attachment> selectedAttachments = FXCollections.observableArrayList();

    public SingleLogEntryDisplayController() {
        this.logClient = null;
    }

    public SingleLogEntryDisplayController(LogClient logClient) {
        this.logClient = logClient;
    }


    @FXML
    public void initialize() {

        logbookIcon.setImage(logbook);
        tagIcon.setImage(tag);

        List<Extension> extensions = Arrays.asList(TablesExtension.create(), ImageAttributesExtension.create());
        parser = Parser.builder().extensions(extensions).build();
        htmlRenderer = HtmlRenderer.builder()
                .attributeProviderFactory(context -> new OlogAttributeProvider(logClient.getServiceUrl()))
                .extensions(extensions).build();

        downloadButton.disableProperty().bind(Bindings.isEmpty(selectedAttachments));
        attachmentsPreviewController.addListSelectionChangeListener(change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    selectedAttachments.addAll(change.getAddedSubList());
                }
                if (change.wasRemoved()) {
                    selectedAttachments.removeAll(change.getRemoved());
                }
            }
        });

        copyURLButton.visibleProperty().setValue(LogbookUIPreferences.web_client_root_URL != null
                && !LogbookUIPreferences.web_client_root_URL.isEmpty());
    }

    public void setLogEntry(LogEntry logEntry) {
        this.logEntry = logEntry;

        attachmentsPane.setExpanded(logEntry.getAttachments() != null && !logEntry.getAttachments().isEmpty());
        attachmentsPane.setVisible(logEntry.getAttachments() != null && !logEntry.getAttachments().isEmpty());
        if(!logEntry.getAttachments().isEmpty()){
            attachmentsPreviewController
                    .setAttachments(FXCollections.observableArrayList(logEntry.getAttachments()));
        }

        List<String> hiddenPropertiesNames = Arrays.asList(LogbookUIPreferences.hidden_properties);
        // Remove the hidden properties
        List<Property> propertiesToShow =
                logEntry.getProperties().stream().filter(property -> !hiddenPropertiesNames.contains(property.getName())).collect(Collectors.toList());
        propertiesPane.setExpanded(!propertiesToShow.isEmpty());
        propertiesPane.setVisible(!propertiesToShow.isEmpty());
        if (!propertiesToShow.isEmpty()) {
            propertiesController.setProperties(propertiesToShow);
        }

        logTime.setText(SECONDS_FORMAT.format(logEntry.getCreatedDate()));
        logOwner.setText(logEntry.getOwner());

        logTitle.setWrapText(true);
        logTitle.setText(logEntry.getTitle());

        // Content is defined by the source (default) or description field. If both are null
        // or empty, do no load any content to the WebView.
        WebEngine webEngine = logDescription.getEngine();
        webEngine.setUserStyleSheetLocation(getClass()
                .getResource("/detail-log-webview.css").toExternalForm());


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

    /**
     * Downloads all selected attachments to folder selected by user.
     */
    @FXML
    public void downloadSelectedAttachments() {
        final DirectoryChooser dialog = new DirectoryChooser();
        dialog.setTitle(Messages.SelectFolder);
        dialog.setInitialDirectory(new File(System.getProperty("user.home")));
        File targetFolder = dialog.showDialog(attachmentsPane.getScene().getWindow());
        JobManager.schedule("Save attachments job", (monitor) ->
        {
            selectedAttachments.stream().forEach(a -> downloadAttachment(targetFolder, a));
        });
    }

    private void downloadAttachment(File targetFolder, Attachment attachment) {
        try {
            File targetFile = new File(targetFolder, attachment.getName());
            if (targetFile.exists()) {
                throw new Exception("Target file " + targetFile.getAbsolutePath() + " exists");
            }
            Files.copy(attachment.getFile().toPath(), targetFile.toPath());
        } catch (Exception e) {
            ExceptionDetailsErrorDialog.openError(attachmentsPane.getParent(), Messages.FileSave, Messages.FileSaveFailed, e);
        }
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


    private void mergeLogEntries() {
        StringBuilder stringBuilder = new StringBuilder();
        logEntries.stream().forEach(l -> {
            stringBuilder.append(createSeparator(l));
            stringBuilder.append(toHtml(l.getSource()));
        });
        WebEngine webEngine = logDescription.getEngine();
        webEngine.setUserStyleSheetLocation(getClass()
                .getResource("/detail-log-webview.css").toExternalForm());
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
        stringBuilder.append("<div class='separator'>");
        stringBuilder.append(SECONDS_FORMAT.format(logEntry.getCreatedDate())).append(". ");
        stringBuilder.append(logEntry.getOwner()).append(", ").append(logEntry.getTitle()).append(", ");
        stringBuilder.append(logEntry.getId());
        stringBuilder.append("</div>");
        return stringBuilder.toString();
    }
}
