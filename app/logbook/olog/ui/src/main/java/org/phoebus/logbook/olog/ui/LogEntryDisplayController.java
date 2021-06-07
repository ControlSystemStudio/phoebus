package org.phoebus.logbook.olog.ui;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.image.attributes.ImageAttributesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.HtmlRenderer;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.LogbookException;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.Tag;
import org.phoebus.logbook.olog.ui.write.LogEntryCompletionHandler;
import org.phoebus.logbook.olog.ui.write.LogEntryEditorStage;
import org.phoebus.logbook.olog.ui.write.LogEntryModel;
import org.phoebus.olog.es.api.model.LogGroupProperty;
import org.phoebus.olog.es.api.model.OlogLog;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.javafx.ImageCache;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.phoebus.util.time.TimestampFormats.SECONDS_FORMAT;

public class LogEntryDisplayController {

    static final Image tag = ImageCache.getImage(LogEntryDisplayController.class, "/icons/add_tag.png");
    static final Image logbook = ImageCache.getImage(LogEntryDisplayController.class, "/icons/logbook-16.png");
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
    private ToolBar toolBar;

    private LogEntry logEntry;

    public LogEntryDisplayController() {
        this.logClient = null;
    }

    public LogEntryDisplayController(LogClient logClient) {
        this.logClient = logClient;
    }

    private Logger logger = Logger.getLogger(LogEntryCellController.class.getName());

    /**
     * List of attachments selected in the preview's {@link ListView}.
     */

    private ObservableList<Attachment> selectedAttachments = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        List<Extension> extensions = Arrays.asList(TablesExtension.create(), ImageAttributesExtension.create());
        parser = Parser.builder().extensions(extensions).build();
        htmlRenderer = HtmlRenderer.builder()
                .attributeProviderFactory(context -> new OlogAttributeProvider())
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

        clearLogView();
    }

    private void clearLogView() {
        logbookIcon.setImage(null);
        tagIcon.setImage(null);
        logOwner.setText(null);
        logTime.setText(null);
        logTitle.setText(null);
        logbooks.setText(null);
        tags.setText(null);
        logEntryId.setText(null);
        level.setText(null);
        toolBar.visibleProperty().setValue(false);
    }

    public void refresh() {
        if (logEntry != null) {

            toolBar.visibleProperty().setValue(true);

            logbookIcon.setImage(logbook);
            tagIcon.setImage(tag);

            attachmentsPane.setExpanded(logEntry.getAttachments() != null && !logEntry.getAttachments().isEmpty());
            attachmentsPane.setVisible(logEntry.getAttachments() != null && !logEntry.getAttachments().isEmpty());

            propertiesPane.setExpanded(logEntry.getProperties() != null && !logEntry.getProperties().isEmpty());
            propertiesPane.setVisible(logEntry.getProperties() != null && !logEntry.getProperties().isEmpty());

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

            attachmentsPreviewController
                    .setAttachments(FXCollections.observableArrayList(logEntry.getAttachments()));

            if (!logEntry.getProperties().isEmpty()) {
                propertiesController.setProperties(logEntry.getProperties());
            }

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
    }

    public LogEntry getLogEntry() {
        return logEntry;
    }

    public void setLogEntry(LogEntry logEntry) {
        this.logEntry = logEntry;
        refresh();
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
     * An {@link AttributeProvider} used to style elements of a log entry. Other types of
     * attribute processing may be added.
     */
    class OlogAttributeProvider implements AttributeProvider {

        /**
         * Processes image nodes to prepend the service root URL, where needed. For table nodes the olog-table
         * class is added in order to give it some styling.
         *
         * @param node The {@link org.commonmark.node.Node} being processed.
         * @param s    The HTML tag, e.g. p, img, strong etc.
         * @param map  Map of attributes for the node.
         */
        @Override
        public void setAttributes(org.commonmark.node.Node node, String s, Map<String, String> map) {
            if (node instanceof TableBlock) {
                map.put("class", "olog-table");
            }
            // Image paths may be relative (when added through dialog), or absolute URLs (e.g. when added "manually" in editor).
            // Relative paths must be prepended with service root URL, while absolute URLs must not be changed.
            if (node instanceof org.commonmark.node.Image) {
                String src = map.get("src");
                if (!src.toLowerCase().startsWith("http")) {
                    String serviceUrl = logClient.getServiceUrl();
                    if (serviceUrl.endsWith("/")) {
                        serviceUrl = serviceUrl.substring(0, serviceUrl.length() - 1);
                    }
                    src = serviceUrl + "/" + src;
                }
                map.put("src", src);
            }
        }
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


    @FXML
    public void reply(){

        Property logGroupProperty = logEntry.getProperty(LogGroupProperty.NAME);
        if(logGroupProperty == null){
            logGroupProperty = LogGroupProperty.create();
            logEntry.getProperties().add(logGroupProperty);
        }
        OlogLog ologLog = new OlogLog();
        ologLog.setTitle(logEntry.getTitle());
        ologLog.setTags(logEntry.getTags());
        ologLog.setLogbooks(logEntry.getLogbooks());
        ologLog.setProperties(logEntry.getProperties());
        ologLog.setLevel(logEntry.getLevel());

        new LogEntryEditorStage(DockPane.getActiveDockPane(), new LogEntryModel(ologLog), l -> {
            try {
                updateLogEntry(logEntry);
            } catch (LogbookException e) {
                logger.log(Level.SEVERE, "Failed to update log entry id=" + logEntry.getId(), e);
                return;
            }
        }).show();
    }

    private void updateLogEntry(LogEntry logEntry) throws LogbookException {
        logClient.updateLogEntry(logEntry);
    }
}
