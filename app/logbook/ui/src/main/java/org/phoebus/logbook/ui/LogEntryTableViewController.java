package org.phoebus.logbook.ui;

import static org.phoebus.util.time.TimestampFormats.SECONDS_FORMAT;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.Tag;
import org.phoebus.logbook.ui.write.PropertiesTab;
import org.phoebus.ui.dialog.PopOver;
import org.phoebus.ui.javafx.FilesTab;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.ImagesTab;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class LogEntryTableViewController extends LogbookSearchController {

    static final Image tag = ImageCache.getImage(LogEntryController.class, "/icons/add_tag.png");
    static final Image logbook = ImageCache.getImage(LogEntryController.class, "/icons/logbook-16.png");
    String styles = "-fx-background-color: #0000ff;" + "-fx-border-color: #ff0000;";

    @FXML
    Button resize;
    @FXML
    Button search;
    @FXML
    TextField query;
    @FXML
    AnchorPane AdavanceSearchPane;

    // elements associated with the various search
    @FXML
    AnchorPane ViewSearchPane;
    @FXML
    TextField searchText;
    @FXML
    TextField searchLogbooks;
    PopOver logbookSearchpopover;

    @FXML
    TextField searchTags;
    PopOver tagSearchpopover;

    
    // elements related to the table view of the log entires
    @FXML
    TableView<LogEntry> tableView;

    @FXML
    TableColumn<LogEntry, LogEntry> timeOwnerCol;
    @FXML
    TableColumn<LogEntry, LogEntry> descriptionCol;
    @FXML
    TableColumn<LogEntry, LogEntry> metaCol;

    // Model
    List<LogEntry> logEntries;
    List<String> logbookNames = Collections.emptyList();
    List<String> tagNames = Collections.emptyList();

    @FXML
    public void initialize() {
        // initialize the list of searchable parameters like logbooks, tags, etc...

        // initially set the serach pane collapsed
        AdavanceSearchPane.minWidthProperty().set(0);
        AdavanceSearchPane.maxWidthProperty().set(0);
        resize.setText("<");

        if (getClient() != null) {
            logbookNames = getClient().listLogbooks().stream().map(Logbook::getName).sorted().collect(Collectors.toList());
            tagNames = getClient().listTags().stream().map(Tag::getName).sorted().collect(Collectors.toList());
        }
        FXMLLoader logbookSelectionLoader = new FXMLLoader();
        logbookSelectionLoader.setLocation(this.getClass().getResource("ListSelection.fxml"));
        try {
            logbookSelectionLoader.load();
            ListSelectionController controller = logbookSelectionLoader.getController();
            controller.setAvailable(logbookNames);
            controller.setOnApply((List<String> t) -> {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        searchLogbooks.setText(t.stream().collect(Collectors.joining(",")));
                        if (logbookSearchpopover.isShowing())
                            logbookSearchpopover.hide();
                    }
                });
                return true;
            });
            controller.setOnCancel((List<String> t) -> {
                if (logbookSearchpopover.isShowing())
                    logbookSearchpopover.hide();
                return true;
            });
            logbookSearchpopover = new PopOver(logbookSelectionLoader.getRoot());
        } catch (IOException e) {
            e.printStackTrace();
        }
        searchLogbooks.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) {
                if (newPropertyValue) {
                    logbookSearchpopover.show(searchLogbooks);
                } else {
                    if (logbookSearchpopover.isShowing())
                        logbookSearchpopover.hide();
                }
            }
        });


        FXMLLoader tagSelectionLoader = new FXMLLoader();
        tagSelectionLoader.setLocation(this.getClass().getResource("ListSelection.fxml"));
        try {
            tagSelectionLoader.load();
            ListSelectionController controller = tagSelectionLoader.getController();
            controller.setAvailable(tagNames);
            controller.setOnApply((List<String> t) -> {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        searchTags.setText(t.stream().collect(Collectors.joining(",")));
                        if (tagSearchpopover.isShowing())
                            tagSearchpopover.hide();
                    }
                });
                return true;
            });
            controller.setOnCancel((List<String> t) -> {
                if (tagSearchpopover.isShowing())
                    tagSearchpopover.hide();
                return true;
            });
            tagSearchpopover = new PopOver(tagSelectionLoader.getRoot());
        } catch (IOException e) {
            e.printStackTrace();
        }
        searchTags.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue,
                    Boolean newPropertyValue) {
                if (newPropertyValue) {
                    tagSearchpopover.show(searchTags);
                } else {
                    if (tagSearchpopover.isShowing())
                        tagSearchpopover.hide();
                }
            }
        });

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
            final Text descriptionText = new Text();
            descriptionText.wrappingWidthProperty().bind(descriptionCol.widthProperty());

            TabPane tabPane = new TabPane();
            ImagesTab imagesTab = new ImagesTab();
            FilesTab filesTab = new FilesTab();
            PropertiesTab propertiesTab = new PropertiesTab();
            tabPane.getTabs().addAll(imagesTab, filesTab, propertiesTab);
            TitledPane tPane = new TitledPane(Messages.Attachments, tabPane);
            Accordion imageGallery = new Accordion();
            imageGallery.getPanes().add(tPane);

            pane.addColumn(0, titleText, descriptionText, imageGallery);
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            pane.getColumnConstraints().add(cc);

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

                        final List<Image> images = new ArrayList<>();
                        final List<File> files = new ArrayList<>();
                        logEntry.getAttachments().stream().forEach(attachment -> {
                            if (attachment.getContentType().startsWith(Attachment.CONTENT_IMAGE)) {
                                images.add(createImage(attachment.getFile()));
                            } else {
                                files.add(attachment.getFile());
                            }
                        });
                        filesTab.setFiles(files);
                        imagesTab.setImages(images);
                        descriptionText.setText(logEntry.getDescription());
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

    // Keeps track of when the animation is active. Multiple clicks will be ignored
    // until a give resize action is completed
    private AtomicBoolean moving = new AtomicBoolean(false);

    @FXML
    public void resize() {
        if (!moving.compareAndExchangeAcquire(false, true)) {
            if (resize.getText().equals(">")) {
                Duration cycleDuration = Duration.millis(400);
                KeyValue kv = new KeyValue(AdavanceSearchPane.minWidthProperty(), 0);
                KeyValue kv2 = new KeyValue(AdavanceSearchPane.maxWidthProperty(), 0);
                Timeline timeline = new Timeline(new KeyFrame(cycleDuration, kv, kv2));
                timeline.play();
                timeline.setOnFinished(event -> {
                    resize.setText("<");
                    moving.set(false);
                });
            } else {
                Duration cycleDuration = Duration.millis(400);
                double width = ViewSearchPane.getWidth()/3;
                KeyValue kv = new KeyValue(AdavanceSearchPane.minWidthProperty(), width);
                KeyValue kv2 = new KeyValue(AdavanceSearchPane.prefWidthProperty(), width);
                Timeline timeline = new Timeline(new KeyFrame(cycleDuration, kv, kv2));
                timeline.play();
                timeline.setOnFinished(event -> {
                    resize.setText(">");
                    moving.set(false);
                });
            }
        }
    }

    @FXML
    public void search() {
        super.search(query.getText());
    }

    @FXML
    public void showLogbookSelection() {
        if (logbookSearchpopover.isShowing())
            logbookSearchpopover.hide();
        else
            logbookSearchpopover.show(searchLogbooks);
    }
    
    @FXML
    public void showTagSelection() {
        System.out.println("gggg");
        if (tagSearchpopover.isShowing())
            tagSearchpopover.hide();
        else
            tagSearchpopover.show(searchTags);
    }
    
    public void setQuery(String parsedQuery) {
        query.setText(parsedQuery);
        search();
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

    private Image createImage(final File imageFile) {
        try {
            return new Image(new FileInputStream(imageFile), 150, 0, true, true);
        } catch (FileNotFoundException e) {
            LogEntryTable.log.log(Level.WARNING, "failed to create image from attachement", e);
            return null;
        }
    }
}
