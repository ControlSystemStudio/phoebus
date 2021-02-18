package org.phoebus.logbook.olog.ui;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.TouchEvent;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.image.attributes.ImageAttributesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.HtmlRenderer;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.Tag;
import org.phoebus.ui.javafx.ImageCache;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.phoebus.util.time.TimestampFormats.MILLI_FORMAT;

public class LogEntryCellController {

    static final Image tag = ImageCache.getImage(LogEntryDisplayController.class, "/icons/add_tag.png");
    static final Image logbook = ImageCache.getImage(LogEntryDisplayController.class, "/icons/logbook-16.png");
    static final Image attachment = ImageCache.getImage(LogEntryDisplayController.class, "/icons/attachment-16.png");

    private HtmlRenderer htmlRenderer;
    private Parser parser;

    // Model
    LogEntry logEntry;

    @FXML
    Label time;
    @FXML
    Label owner;
    @FXML
    Label title;
    @FXML
    Label logbooks;
    @FXML
    ImageView logbookIcon;
    @FXML
    Label tags;
    @FXML
    ImageView tagIcon;
    @FXML
    Label attachments;
    @FXML
    ImageView attachmentIcon;

    @FXML
    WebView description;

    public LogEntryCellController() {

        List<Extension> extensions = Arrays.asList(TablesExtension.create(), ImageAttributesExtension.create());
        parser = Parser.builder().extensions(extensions).build();
        htmlRenderer = HtmlRenderer.builder()
                .attributeProviderFactory(context -> new OlogAttributeProvider())
                .extensions(extensions).build();
    }

    @FXML
    public void initialize() {
        time.setStyle("-fx-font-weight: bold");
        title.setStyle("-fx-font-weight: bold");

        logbooks.setText("");
        logbookIcon.setImage(null);
        tags.setText("");
        tagIcon.setImage(null);
        attachments.setText("");
        attachmentIcon.setImage(null);

        title.setText("");
    }

    @FXML
    public void refresh() {
        if (logEntry != null) {

            time.setText(MILLI_FORMAT.format(logEntry.getCreatedDate()));
            owner.setText(logEntry.getOwner());
            title.setText(logEntry.getTitle());

            if ( !logEntry.getLogbooks().isEmpty() ) {
                logbookIcon.setImage(logbook);
                logbooks.setWrapText(true);
                logbooks.setText(logEntry.getLogbooks().stream().map(Logbook::getName).collect(Collectors.joining(",")));
            }
            if ( !logEntry.getTags().isEmpty() ) {
                tagIcon.setImage(tag);
                tags.setWrapText(true);
                tags.setText(logEntry.getTags().stream().map(Tag::getName).collect(Collectors.joining(",")));
            }
            if( !logEntry.getAttachments().isEmpty()) {
                attachmentIcon.setImage(attachment);
                attachments.setText(String.valueOf(logEntry.getAttachments().size()));
            }
            description.setDisable(true);
            // Content is defined by the source (default) or description field. If both are null
            // or empty, do no load any content to the WebView.
            WebEngine webEngine = description.getEngine();
            webEngine.setUserStyleSheetLocation(getClass()
                    .getResource("/webview.css").toExternalForm());
            if(logEntry.getSource() != null && !logEntry.getSource().isEmpty()){
                webEngine.loadContent(toHtml(logEntry.getSource()));
            }
            else if(logEntry.getDescription() != null && !logEntry.getDescription().isEmpty()){
                webEngine.loadContent(toHtml(logEntry.getDescription()));
            }
        }
    }

    public void setLogEntry(LogEntry logEntry) {
        this.logEntry = logEntry;
        refresh();
    }

    /**
     * Converts Commonmark content to HTML.
     * @param commonmarkString Raw Commonmark string
     * @return The HTML output of the Commonmark processor.
     */
    private String toHtml(String commonmarkString){
        org.commonmark.node.Node document = parser.parse(commonmarkString);
        return htmlRenderer.render(document);
    }

    /**
     * An {@link AttributeProvider} used to style elements of a log entry. Other types of
     * attribute processing is of course possible.
     */
    static class OlogAttributeProvider implements AttributeProvider {
        @Override
        public void setAttributes(org.commonmark.node.Node node, String s, Map<String, String> map) {
            if (node instanceof TableBlock) {
                map.put("class", "olog-table");
            }
        }
    }
}
