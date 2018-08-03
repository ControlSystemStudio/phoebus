package org.phoebus.logbook.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.Tag;
import org.phoebus.ui.javafx.ImageCache;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.TilePane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import static org.phoebus.util.time.TimestampFormats.*;

public class LogEntryTableController extends LogbookSearchController {
    static final Image tag = ImageCache.getImage(LogEntryController.class, "/icons/add_tag.png");
    static final Image logbook = ImageCache.getImage(LogEntryController.class, "/icons/logbook-16.png");

    @FXML
    TextField query;
    @FXML
    Button search;

    @FXML
    TableView<LogEntry> tableView;

    @FXML
    TableColumn<LogEntry, LogEntry> timeOwnerCol;
    @FXML
    TableColumn<LogEntry, LogEntry> descriptionCol;
    @FXML
    TableColumn<LogEntry, LogEntry> metaCol;

    List<LogEntry> logEntries;

    @FXML
    public void initialize() {

        tableView.getColumns().clear();
        tableView.setEditable(false);

        // Create column EmpNo (Data type of String).
        timeOwnerCol = new TableColumn<LogEntry, LogEntry>("Time");

        // Create column FullName (Data type of String).
        descriptionCol = new TableColumn<LogEntry, LogEntry>("Log");

        // Create 2 sub column for FullName.
        metaCol = new TableColumn<LogEntry, LogEntry>("Logbook/Tags");

        timeOwnerCol.setMaxWidth(1f * Integer.MAX_VALUE * 25);
        timeOwnerCol.setCellValueFactory(col -> {
            return new SimpleObjectProperty(col.getValue());
        });
        timeOwnerCol.setCellFactory(col -> {
            final GridPane pane = new GridPane();
            final Label timeText = new Label();
            timeText.setStyle("-fx-font-weight: bold");
            final Label ownerText = new Label();
            pane.addColumn(0, timeText, ownerText);

            return new TableCell<LogEntry, LogEntry>() {
                @Override
                public void updateItem(LogEntry logEntry, boolean empty) {
                    super.updateItem(logEntry, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        timeText.setText(SECONDS_FORMAT.format(logEntry.getCreatedDate()));
                        ownerText.setText(logEntry.getOwner());
                        setGraphic(pane);
                    }
                }
            };
        });

        descriptionCol.setMaxWidth(1f * Integer.MAX_VALUE * 50);
        descriptionCol.setCellValueFactory(col -> {
            return new SimpleObjectProperty(col.getValue());
        });
        descriptionCol.setCellFactory(col -> {
            final GridPane pane = new GridPane();
            final Label titleText = new Label();
            titleText.setStyle("-fx-font-weight: bold");
            final Label descriptionText = new Label();
            TilePane imageGallery = new TilePane();
            pane.addColumn(0, titleText, descriptionText, imageGallery);

            return new TableCell<LogEntry, LogEntry>() {
                @Override
                public void updateItem(LogEntry logEntry, boolean empty) {
                    super.updateItem(logEntry, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        if (logEntry.getTitle() == null || logEntry.getTitle().isEmpty()) {
                            titleText.setVisible(false);
                        } else {
                            titleText.setVisible(true);
                            titleText.setText(logEntry.getTitle());
                        }
                        descriptionText.setText(logEntry.getDescription());
                        // imageGallery.getChildren().clear();
                        // logEntry.getAttachments().forEach(attachment -> {
                        // ImageView imageView;
                        // imageView = createImageView(attachment.getFile());
                        // imageGallery.getChildren().addAll(imageView);
                        // });
                        // imageGallery.requestLayout();
                        setGraphic(pane);
                    }
                }
            };

        });

        metaCol.setMaxWidth(1f * Integer.MAX_VALUE * 25);
        metaCol.setCellValueFactory(col -> {
            return new SimpleObjectProperty(col.getValue());
        });
        metaCol.setCellFactory(col -> {
            final GridPane pane = new GridPane();
            final Label logbooks = new Label();
            final Separator seperator = new Separator();
            final Label tags = new Label();
            pane.addColumn(0, logbooks, seperator, tags);

            return new TableCell<LogEntry, LogEntry>() {
                @Override
                public void updateItem(LogEntry logEntry, boolean empty) {
                    super.updateItem(logEntry, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        logbooks.setText(logEntry.getLogbooks().stream().map(Logbook::getName)
                                .collect(Collectors.joining(System.lineSeparator())));
                        logbooks.setGraphic(new ImageView(logbook));
                        tags.setText(logEntry.getTags().stream().map(Tag::getName)
                                .collect(Collectors.joining(System.lineSeparator())));
                        tags.setGraphic(new ImageView(tag));
                        setGraphic(pane);
                    }
                }
            };
        });

        tableView.getColumns().add(timeOwnerCol);
        tableView.getColumns().add(descriptionCol);
        tableView.getColumns().add(metaCol);

    }

    @Override
    public void setLogs(List<LogEntry> logs) {
        this.logEntries = logs;
        refresh();
    }

    private void refresh() {
        if (logEntries != null && !logEntries.isEmpty()) {
            ObservableList<LogEntry> logsList = FXCollections.observableArrayList();
            logsList.addAll(logEntries.stream().collect(Collectors.toList()));
            tableView.setItems(logsList);
        }
    }

    @FXML
    public void search() {
        super.search(query.getText());
    }

    public void setQuery(String parsedQuery) {
        query.setText(parsedQuery);
        search();

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
                                e.printStackTrace();
                            }

                        }
                    }
                }
            });
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        return imageView;
    }

}
