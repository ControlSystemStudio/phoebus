package org.phoebus.logbook.olog.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.image.attributes.ImageAttributesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.Tag;
import org.phoebus.ui.javafx.ImageCache;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.phoebus.util.time.TimestampFormats.MILLI_FORMAT;

public class LogEntryCellController {

    static final Image tag = ImageCache.getImage(LogEntryDisplayController.class, "/icons/add_tag.png");
    static final Image logbook = ImageCache.getImage(LogEntryDisplayController.class, "/icons/logbook-16.png");
    static final Image attachment = ImageCache.getImage(LogEntryDisplayController.class, "/icons/attachment-16.png");

    private TextContentRenderer textRenderer;
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
    Label description;

    public LogEntryCellController() {

        List<Extension> extensions = Arrays.asList(TablesExtension.create(), ImageAttributesExtension.create());
        parser = Parser.builder().extensions(extensions).build();
        textRenderer = TextContentRenderer.builder().extensions(extensions).build();
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

        description.setWrapText(true);

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
            if(logEntry.getSource() != null){
                description.setText(toText(logEntry.getSource()));
            }
            else if(logEntry.getDescription() != null){
                description.setText(toText(logEntry.getDescription()));
            }
        }
    }

    public void setLogEntry(LogEntry logEntry) {
        this.logEntry = logEntry;
        refresh();
    }

    /**
     * Converts Commonmark content to Text.
     * @param commonmarkString Raw Commonmark string
     * @return The Text output of the Commonmark processor.
     */
    private String toText(String commonmarkString){
        org.commonmark.node.Node document = parser.parse(commonmarkString);
        return textRenderer.render(document);
    }
}
