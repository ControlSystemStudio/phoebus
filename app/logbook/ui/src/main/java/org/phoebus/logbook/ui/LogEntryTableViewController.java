package org.phoebus.logbook.ui;

import static org.phoebus.ui.time.TemporalAmountPane.Type.TEMPORAL_AMOUNTS_AND_NOW;
import static org.phoebus.util.time.TimestampFormats.SECONDS_FORMAT;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.Tag;
import org.phoebus.logbook.ui.LogbookQueryUtil.Keys;
import org.phoebus.logbook.ui.write.PropertiesTab;
import org.phoebus.ui.dialog.PopOver;
import org.phoebus.ui.javafx.FilesTab;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.ImagesTab;
import org.phoebus.ui.time.TimeRelativeIntervalPane;
import org.phoebus.util.time.TimeParser;
import org.phoebus.util.time.TimeRelativeInterval;
import org.phoebus.util.time.TimestampFormats;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * A controller for a log entry table with a collapsible advance search section.
 * @author Kunal Shroff
 *
 */
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

    @FXML
    GridPane timePane;
    @FXML
    TextField startTime;
    @FXML
    TextField endTime;
    PopOver timeSearchpopover;

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
    List<String> logbookNames;
    List<String> tagNames;

    private ListSelectionController tagController;
    private ListSelectionController logbookController;

    // Search parameters
    ObservableMap<Keys, String> searchParameters;

    @FXML
    public void initialize() {
        // initialize the list of searchable parameters like logbooks, tags, etc...

        // initially set the search pane collapsed
        AdavanceSearchPane.minWidthProperty().set(0);
        AdavanceSearchPane.maxWidthProperty().set(0);
        resize.setText("<");

        searchParameters = FXCollections.<Keys, String>observableHashMap();
        searchParameters.put(Keys.SEARCH, "*");
        searchParameters.put(Keys.STARTTIME, TimeParser.format(java.time.Duration.ofHours(8)));
        searchParameters.put(Keys.ENDTIME, TimeParser.format(java.time.Duration.ZERO));

        // XXX ideally using binding like the example underneath would be ideal.
        //
        // query.textProperty().bind(Bindings.createStringBinding(() -> {
        // return searchParameters.entrySet().stream().map((e) -> {
        // return e.getKey().trim() + ":" + e.getValue().trim();
        // }).collect(Collectors.joining(","));
        // }, searchParameters));

        searchParameters.addListener(new MapChangeListener<Keys, String>() {
            @Override
            public void onChanged(Change<? extends Keys, ? extends String> change) {
                Platform.runLater(() -> {
                    query.setText(searchParameters.entrySet().stream().sorted(Map.Entry.comparingByKey()).map((e) -> {
                        return e.getKey().getName().trim() + "=" + e.getValue().trim();
                    }).collect(Collectors.joining(",")));
                    searchText.setText(searchParameters.get(Keys.SEARCH));
                    searchLogbooks.setText(searchParameters.get(Keys.LOGBOOKS));
                    searchTags.setText(searchParameters.get(Keys.TAGS));
                });
            }
        });

        startTime.textProperty().bind(Bindings.valueAt(searchParameters, Keys.STARTTIME));
        endTime.textProperty().bind(Bindings.valueAt(searchParameters, Keys.ENDTIME));

        searchText.setText(searchParameters.get(Keys.SEARCH));
        query.setText(searchParameters.entrySet().stream().sorted(Map.Entry.comparingByKey()).map((e) -> {
            return e.getKey().getName().trim() + ":" + e.getValue().trim();
        }).collect(Collectors.joining(",")));

        FXMLLoader logbookSelectionLoader = new FXMLLoader();
        logbookSelectionLoader.setLocation(this.getClass().getResource("ListSelection.fxml"));
        try {
            logbookSelectionLoader.load();
            logbookController = logbookSelectionLoader.getController();
            logbookController.setOnApply((List<String> t) -> {
                Platform.runLater(() -> {
                    searchParameters.put(Keys.LOGBOOKS, t.stream().collect(Collectors.joining(",")));
                    //searchLogbooks.setText(t.stream().collect(Collectors.joining(",")));
                    if (logbookSearchpopover.isShowing())
                        logbookSearchpopover.hide();
                });
                return true;
            });
            logbookController.setOnCancel((List<String> t) -> {
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
            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue,
                    Boolean newPropertyValue) {
                if (newPropertyValue) {
                    if(logbookNames == null) {
                        logbookNames = getClient().listLogbooks().stream().map(Logbook::getName).sorted().collect(Collectors.toList());
                    }
                    logbookController.setAvailable(logbookNames);
                    logbookSearchpopover.show(searchLogbooks);
                } else if (logbookSearchpopover.isShowing()) {
                    logbookSearchpopover.hide();
                }
            }
        });

        FXMLLoader tagSelectionLoader = new FXMLLoader();
        tagSelectionLoader.setLocation(this.getClass().getResource("ListSelection.fxml"));
        try {
            tagSelectionLoader.load();
            tagController = tagSelectionLoader.getController();
            tagController.setOnApply((List<String> t) -> {
                Platform.runLater(() -> {
                    searchParameters.put(Keys.TAGS, t.stream().collect(Collectors.joining(",")));
                    //searchTags.setText(t.stream().collect(Collectors.joining(",")));
                    if (tagSearchpopover.isShowing())
                        tagSearchpopover.hide();
                });
                return true;
            });
            tagController.setOnCancel((List<String> t) -> {
                if (tagSearchpopover.isShowing())
                    tagSearchpopover.hide();
                return true;
            });
            tagSearchpopover = new PopOver(tagSelectionLoader.getRoot());
        } catch (IOException e) {
            e.printStackTrace();
        }
        searchTags.focusedProperty().addListener(
                (ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
                    if (newPropertyValue) {
                        if(tagNames == null) {
                            tagNames = getClient().listTags().stream().map(Tag::getName).sorted().collect(Collectors.toList());
                        }
                        tagController.setAvailable(tagNames);
                        tagSearchpopover.show(searchTags);
                    } else if (tagSearchpopover.isShowing()) {
                        tagSearchpopover.hide();
                    }
                });

        VBox timeBox = new VBox();

        TimeRelativeIntervalPane timeSelectionPane = new TimeRelativeIntervalPane(TEMPORAL_AMOUNTS_AND_NOW);

        // TODO needs to be initialized from the values in the search parameters
        TimeRelativeInterval initial = TimeRelativeInterval.of(java.time.Duration.ofHours(8), java.time.Duration.ZERO);
        timeSelectionPane.setInterval(initial);

        HBox hbox = new HBox();
        hbox.setSpacing(5);
        hbox.setAlignment(Pos.CENTER_RIGHT);
        Button apply = new Button();
        apply.setText("Apply");
        apply.setPrefWidth(80);
        apply.setOnAction((event) -> {
            Platform.runLater(() -> {
                TimeRelativeInterval interval = timeSelectionPane.getInterval();
                if (interval.isStartAbsolute()) {
                    searchParameters.put(Keys.STARTTIME,
                            TimestampFormats.MILLI_FORMAT.format(interval.getAbsoluteStart().get()));
                } else {
                    searchParameters.put(Keys.STARTTIME, TimeParser.format(interval.getRelativeStart().get()));
                }
                if (interval.isEndAbsolute()) {
                    searchParameters.put(Keys.ENDTIME,
                            TimestampFormats.MILLI_FORMAT.format(interval.getAbsoluteEnd().get()));
                } else {
                    searchParameters.put(Keys.ENDTIME, TimeParser.format(interval.getRelativeEnd().get()));
                }
                if (timeSearchpopover.isShowing())
                    timeSearchpopover.hide();
            });
        });
        Button cancel = new Button();
        cancel.setText("Cancel");
        cancel.setPrefWidth(80);
        cancel.setOnAction((event) -> {
            if (timeSearchpopover.isShowing())
                timeSearchpopover.hide();
        });
        hbox.getChildren().addAll(apply, cancel);
        timeBox.getChildren().addAll(timeSelectionPane, hbox);
        timeSearchpopover = new PopOver(timeBox);
        startTime.focusedProperty().addListener(
                (ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
                    if (newPropertyValue) {
                        timeSearchpopover.show(timePane);
                    } else if (timeSearchpopover.isShowing()) {
                        timeSearchpopover.hide();
                    }
                });

        endTime.focusedProperty().addListener(
                (ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
                    if (newPropertyValue) {
                        timeSearchpopover.show(timePane);
                    } else if (timeSearchpopover.isShowing()) {
                        timeSearchpopover.hide();
                    }
                });

        // The display table.
        tableView.getColumns().clear();
        tableView.setEditable(false);

        timeOwnerCol = new TableColumn<LogEntry, LogEntry>("Time");

        descriptionCol = new TableColumn<LogEntry, LogEntry>("Log");

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
                        if(!files.isEmpty() || !images.isEmpty()) {
                            tPane.setExpanded(true);
                        } else {
                            tPane.setExpanded(false);
                        }
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
                double width = ViewSearchPane.getWidth() / 3;
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
    void updateQuery() {
        Arrays.asList(query.getText().split(",")).forEach(s -> {
            String key = s.split("=")[0];
            for (Keys k : Keys.values()) {
                if (k.getName().equals(key)) {
                    searchParameters.put(k, s.split("=")[1]);
                }
            }
        });
    }

    @FXML
    public void search() {
        // parse the various time representations to Instant
        super.search(LogbookQueryUtil.parseQueryString(query.getText()));
    }

    @FXML
    void setSearchText() {
        searchParameters.put(Keys.SEARCH, searchText.getText());
    }

    @FXML
    void setSelectedLogbooks() {

    }

    @FXML
    void setSelectedTags() {

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
        if (tagSearchpopover.isShowing())
            tagSearchpopover.hide();
        else
            tagSearchpopover.show(searchTags);
    }

    @FXML
    public void showTimeSelection() {
        if (timeSearchpopover.isShowing())
            timeSearchpopover.hide();
        else
            timeSearchpopover.show(timePane);
    }

    @Override
    public void setLogs(List<LogEntry> logs) {
        this.logEntries = logs;
        refresh();
    }

    public void setQuery(String parsedQuery) {
        query.setText(parsedQuery);
        updateQuery();
        search();
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
            return new Image(new FileInputStream(imageFile), 0, 0, true, true);
        } catch (FileNotFoundException e) {
            LogEntryTable.log.log(Level.WARNING, "failed to create image from attachement", e);
            return null;
        }
    }
}
