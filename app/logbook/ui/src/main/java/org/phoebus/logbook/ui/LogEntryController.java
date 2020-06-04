package org.phoebus.logbook.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.Tag;
import org.phoebus.ui.javafx.ImageCache;

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
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class LogEntryController {

    private static final Logger logger = Logger.getLogger(LogEntryController.class.getName());

    static final Image tag = ImageCache.getImage(LogEntryController.class, "/icons/add_tag.png");
    static final Image logbook = ImageCache.getImage(LogEntryController.class, "/icons/logbook-16.png");

    String styles = "-fx-background-color: #0000ff;" + "-fx-border-color: #ff0000;";

    @FXML
    Label logTime;
    @FXML
    TextArea logDescription;

    @FXML
    TitledPane logTagsPane;
    @FXML
    ListView<String> logTags;

    @FXML
    TitledPane LogLogbooksPane;
    @FXML
    ListView<String> LogLogbooks;

    @FXML
    TitledPane LogAttchments;
    @FXML
    TilePane imageGallery;

    private LogEntry logEntry;

    @FXML
    public void initialize() {

        logDescription.setBackground(Background.EMPTY);

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
    }

    public void refresh() {
        if (logEntry != null) {

            // System.out.println("expand att: "+!logEntry.getAttachments().isEmpty()+ "
            // tags: " + !logEntry.getTags().isEmpty() + "
            // logbooks:"+!logEntry.getLogbooks().isEmpty());

            LogAttchments.setExpanded(logEntry.getAttachments() != null && !logEntry.getAttachments().isEmpty());
            LogAttchments.setVisible(logEntry.getAttachments() != null && !logEntry.getAttachments().isEmpty());
            LogLogbooksPane.setExpanded(!logEntry.getLogbooks().isEmpty());
            logTagsPane.setExpanded(!logEntry.getTags().isEmpty());

            logDescription.setWrapText(true);
            logDescription.setText(logEntry.getDescription());

            StringBuilder text = new StringBuilder();
            if (logEntry.getCreatedDate() != null) {
                text.append(logEntry.getCreatedDate().toString()).append(System.lineSeparator());
            }
            if (logEntry.getTitle() != null) {
                text.append(logEntry.getTitle());
            }
            logTime.setText(text.toString());

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
}
