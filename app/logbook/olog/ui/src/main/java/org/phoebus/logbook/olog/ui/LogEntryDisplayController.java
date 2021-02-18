package org.phoebus.logbook.olog.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.phoebus.util.time.TimestampFormats.MILLI_FORMAT;

public class LogEntryDisplayController {

    private static final Logger logger = Logger.getLogger(LogEntryDisplayController.class.getName());


    static final Image tag = ImageCache.getImage(LogEntryDisplayController.class, "/icons/add_tag.png");
    static final Image logbook = ImageCache.getImage(LogEntryDisplayController.class, "/icons/logbook-16.png");

    String styles = "-fx-background-color: #0000ff;" + "-fx-border-color: #ff0000;";

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
    ListView<String> logTags;

    @FXML
    ListView<String> LogLogbooks;

    @FXML
    TitledPane LogAttachments;
    @FXML
    TilePane imageGallery;

    @FXML
    public TitledPane propertiesPane;
    @FXML
    public VBox properties;
    @FXML
    public LogPropertiesController propertiesController;


    private LogEntry logEntry;

    @FXML
    public void initialize() {
        logTime.setStyle("-fx-font-weight: bold");
        logTitle.setStyle("-fx-font-weight: bold");

        logTags.setCellFactory(listView -> new ListCell<String>() {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setGraphic(new ImageView(tag));
                    setText(item);
                }
            }
        });

        LogLogbooks.setCellFactory(listView -> new ListCell<String>() {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setGraphic(new ImageView(logbook));
                    setText(item);
                }
            }
        });

        List<Extension> extensions = Arrays.asList(TablesExtension.create(), ImageAttributesExtension.create());
        parser = Parser.builder().extensions(extensions).build();
        htmlRenderer = HtmlRenderer.builder()
                .attributeProviderFactory(context -> new LogEntryCellController.OlogAttributeProvider())
                .extensions(extensions).build();
    }

    public void refresh() {
        if (logEntry != null) {

            // System.out.println("expand att: "+!logEntry.getAttachments().isEmpty()+ "
            // tags: " + !logEntry.getTags().isEmpty() + "
            // logbooks:"+!logEntry.getLogbooks().isEmpty());

            LogAttachments.setExpanded(logEntry.getAttachments() != null && !logEntry.getAttachments().isEmpty());
            LogAttachments.setVisible(logEntry.getAttachments() != null && !logEntry.getAttachments().isEmpty());

            propertiesPane.setExpanded(logEntry.getProperties() != null && !logEntry.getProperties().isEmpty());
            propertiesPane.setVisible(logEntry.getProperties() != null && !logEntry.getProperties().isEmpty());

            logTime.setText(MILLI_FORMAT.format(logEntry.getCreatedDate()));

            logOwner.setText(logEntry.getOwner());

            logTitle.setWrapText(true);
            logTitle.setText(logEntry.getTitle());

            logDescription.setDisable(true);
            // Content is defined by the source (default) or description field. If both are null
            // or empty, do no load any content to the WebView.
            WebEngine webEngine = logDescription.getEngine();
            webEngine.setUserStyleSheetLocation(getClass()
                    .getResource("/detail-log-webview.css").toExternalForm());
            if(logEntry.getSource() != null && !logEntry.getSource().isEmpty()){
                webEngine.loadContent(toHtml(logEntry.getSource()));
            }
            else if(logEntry.getDescription() != null && !logEntry.getDescription().isEmpty()){
                webEngine.loadContent(toHtml(logEntry.getDescription()));
            }
            ObservableList<String> logbookList = FXCollections.observableArrayList();
            logbookList.addAll(logEntry.getLogbooks().stream().map(Logbook::getName).collect(Collectors.toList()));
            LogLogbooks.setItems(logbookList);

            ObservableList<String> tagList = FXCollections.observableArrayList();
            tagList.addAll(logEntry.getTags().stream().map(Tag::getName).collect(Collectors.toList()));
            logTags.setItems(tagList);

            imageGallery.getChildren().clear();
            logEntry.getAttachments().forEach(attachment -> {
                ImageView imageView;
                imageView = createImageView(attachment.getFile());
                imageGallery.getChildren().addAll(imageView);
            });

            propertiesController.setProperties(logEntry.getProperties());
        }
    }

    public LogEntry getLogEntry() {
        return logEntry;
    }

    public void setLogEntry(LogEntry logEntry) {
        this.logEntry = logEntry;
        refresh();
    }

    private ImageView createImageView(final File imageFile) {

        ImageView imageView = null;
        try {
            final Image image = new Image(new FileInputStream(imageFile), 150, 0, true, true);
            imageView = new ImageView(image);
            imageView.setFitWidth(150);
            imageView.setOnMouseClicked(new EventHandler<MouseEvent>() {

                @Override
                public void handle(MouseEvent mouseEvent) {

                    if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {

                        if (mouseEvent.getClickCount() == 2) {
                            try {
                                BorderPane borderPane = new BorderPane();
                                ImageView imageView = new ImageView();
                                Image image = new Image(new FileInputStream(imageFile));
                                imageView.setImage(image);
                                imageView.setStyle("-fx-background-color: BLACK");
                                imageView.setFitHeight(image.getHeight() - 10);
                                imageView.setPreserveRatio(true);
                                imageView.setSmooth(true);
                                imageView.setCache(true);
                                borderPane.setCenter(imageView);
                                borderPane.setStyle("-fx-background-color: BLACK");
                                Stage newStage = new Stage();
                                newStage.setWidth(image.getWidth());
                                newStage.setHeight(image.getHeight());
                                newStage.setTitle(imageFile.getName());
                                Scene scene = new Scene(borderPane, Color.BLACK);
                                newStage.setScene(scene);
                                newStage.show();
                            } catch (FileNotFoundException e) {
                                logger.log(Level.WARNING, "failed to open Image File.", e);
                            }

                        }
                    }
                }
            });
        } catch (FileNotFoundException ex) {
            logger.log(Level.WARNING, "failed to open Image File.", ex);
        }
        return imageView;
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
