package org.phoebus.logbook.olog.ui;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.image.attributes.ImageAttributesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.Tag;
import org.phoebus.logbook.olog.ui.LogEntryTableViewController.TableViewListItem;
import org.phoebus.ui.javafx.ImageCache;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.phoebus.util.time.TimestampFormats.SECONDS_FORMAT;

public class LogEntryCellController {

    static final Image tag = ImageCache.getImage(SingleLogEntryDisplayController.class, "/icons/add_tag.png");
    static final Image logbook = ImageCache.getImage(SingleLogEntryDisplayController.class, "/icons/logbook-16.png");
    static final Image attachment = ImageCache.getImage(SingleLogEntryDisplayController.class, "/icons/attachment-16.png");
    static final Image conversation = ImageCache.getImage(SingleLogEntryDisplayController.class, "/icons/conversation.png");

    private TextContentRenderer textRenderer;
    private Parser parser;

    // Model
    TableViewListItem logEntry;

    @FXML
    VBox root;

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
    Label logEntryId;
    @FXML
    Label level;

    @FXML
    ImageView attachmentIcon;

    @FXML
    Label description;

    @FXML
    ImageView conversationIcon;

    @FXML
    private Pane detailsPane;

    private SimpleBooleanProperty expanded = new SimpleBooleanProperty(true);

    public LogEntryCellController() {

        List<Extension> extensions = Arrays.asList(TablesExtension.create(), ImageAttributesExtension.create());
        parser = Parser.builder().extensions(extensions).build();
        textRenderer = TextContentRenderer.builder().extensions(extensions).build();
    }

    @FXML
    public void initialize() {
        logbookIcon.setImage(logbook);
        tagIcon.setImage(tag);
        attachmentIcon.setImage(null);
        // hide/show using CSS pseudo-selector "grouped"
        // that is defined in LogEntryTableViewController
        conversationIcon.setImage(conversation);
    }

    @FXML
    public void refresh() {
        if (logEntry != null) {

            time.setText(SECONDS_FORMAT.format(logEntry.getLogEntry().getCreatedDate()));
            owner.setText(logEntry.getLogEntry().getOwner());
            title.setText(logEntry.getLogEntry().getTitle());
            title.getStyleClass().add("title");

            if (!logEntry.getLogEntry().getLogbooks().isEmpty()) {
                logbooks.setWrapText(false);
                logbooks.setText(logEntry.getLogEntry().getLogbooks().stream().map(Logbook::getName).collect(Collectors.joining(",")));
            }
            if (!logEntry.getLogEntry().getTags().isEmpty()) {
                tags.setText(logEntry.getLogEntry().getTags().stream().map(Tag::getName).collect(Collectors.joining(",")));
            } else {
                tags.setText(null);
            }
            if (!logEntry.getLogEntry().getAttachments().isEmpty()) {
                attachmentIcon.setImage(attachment);
            } else {
                attachmentIcon.setImage(null);
            }
            description.setWrapText(false);
            if (logEntry.getLogEntry().getSource() != null) {
                description.setText(toText(logEntry.getLogEntry().getSource()));
            } else if (logEntry.getLogEntry().getDescription() != null) {
                description.setText(toText(logEntry.getLogEntry().getDescription()));
            } else {
                description.setText(null);
            }

            logEntryId.setText(logEntry.getLogEntry().getId() != null ? logEntry.getLogEntry().getId().toString() : "");
            level.setText(logEntry.getLogEntry().getLevel());
        }
    }

    public void setLogEntry(TableViewListItem logEntry) {
        this.logEntry = logEntry;
        detailsPane.managedProperty().bind(logEntry.isShowDetails());
        detailsPane.visibleProperty().bind(logEntry.isShowDetails());
        refresh();
    }

    /**
     * Converts Commonmark content to Text. At most one line of text is returned, the newline character is
     * used to detect line break.
     *
     * @param commonmarkString Raw Commonmark string
     * @return The Text output of the Commonmark processor.
     */
    private String toText(String commonmarkString) {
        org.commonmark.node.Node document = parser.parse(commonmarkString);
        String text = textRenderer.render(document);
        String[] lines = text.split("\r\n|\n|\r");
        return lines[0];
    }
}
