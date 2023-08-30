package org.phoebus.logbook.ui;

import static org.phoebus.util.time.TimestampFormats.SECONDS_FORMAT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.Tag;
import org.phoebus.logbook.ui.LogbookQueryUtil.Keys;
import org.phoebus.logbook.ui.write.AttachmentsViewController;
import org.phoebus.logbook.ui.write.LogEntryModel;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.util.time.TimeParser;


import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
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

    @FXML
    Button resize;
    @FXML
    Button search;
    @FXML
    TextField query;

    // elements associated with the various search
    @FXML
    GridPane ViewSearchPane;

    // elements related to the table view of the log entires
    @FXML
    TableView<LogEntry> tableView;

    @FXML
    TableColumn<LogEntry, LogEntry> timeOwnerCol;
    @FXML
    TableColumn<LogEntry, LogEntry> descriptionCol;
    @FXML
    TableColumn<LogEntry, LogEntry> metaCol;

    @FXML
    private Node topLevelNode;

    @FXML
    private AdvancedSearchViewController advancedSearchViewController;

    // Model
    List<LogEntry> logEntries;

    // Search parameters
    ObservableMap<Keys, String> searchParameters;

    public LogEntryTableViewController(LogClient logClient){
        setClient(logClient);
    }

    @FXML
    public void initialize() {

        resize.setText("<");

        searchParameters = FXCollections.observableHashMap();
        searchParameters.put(Keys.SEARCH, "*");
        searchParameters.put(Keys.STARTTIME, TimeParser.format(java.time.Duration.ofHours(8)));
        searchParameters.put(Keys.ENDTIME, TimeParser.format(java.time.Duration.ZERO));
        advancedSearchViewController.setSearchParameters(searchParameters);

        query.setText(searchParameters.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map((e) -> e.getKey().getName().trim() + "=" + e.getValue().trim())
                .collect(Collectors.joining("&")));

        searchParameters.addListener((MapChangeListener<Keys, String>) change -> query.setText(searchParameters.entrySet().stream()
                .sorted(Entry.comparingByKey())
                .map((e) -> e.getKey().getName().trim() + "=" + e.getValue().trim())
                .collect(Collectors.joining("&"))));

        // The display table.
        tableView.getColumns().clear();
        tableView.setEditable(false);

        timeOwnerCol = new TableColumn<>("Time");
        descriptionCol = new TableColumn<>("Log");
        metaCol = new TableColumn<>("Logbook/Tags");

        timeOwnerCol.setMaxWidth(1f * Integer.MAX_VALUE * 25);
        timeOwnerCol.setCellValueFactory(col -> new SimpleObjectProperty(col.getValue()));
        timeOwnerCol.setCellFactory(col -> {
            final GridPane pane = new GridPane();
            final Label timeText = new Label();
            timeText.setStyle("-fx-font-weight: bold");
            final Label ownerText = new Label();
            pane.addColumn(0, timeText, ownerText);

            return new TableCell<>() {
                @Override
                public void updateItem(LogEntry logEntry, boolean empty) {
                    super.updateItem(logEntry, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        if (logEntry.getCreatedDate() != null) {
                            timeText.setText(SECONDS_FORMAT.format(logEntry.getCreatedDate()));
                        }
                        ownerText.setText(logEntry.getOwner());
                        setGraphic(pane);
                    }
                }
            };
        });

        descriptionCol.setMaxWidth(1f * Integer.MAX_VALUE * 50);
        descriptionCol.setCellValueFactory(col -> new SimpleObjectProperty(col.getValue()));
        descriptionCol.setCellFactory(col -> {
            final GridPane pane = new GridPane();
            final Label titleText = new Label();
            titleText.setStyle("-fx-font-weight: bold");
            final Text descriptionText = new Text();
            descriptionText.wrappingWidthProperty().bind(descriptionCol.widthProperty());

            Node parent = topLevelNode.getScene().getRoot();
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("write/AttachmentsView.fxml"));
            fxmlLoader.setControllerFactory(clazz -> {
                try {
                    if(clazz.isAssignableFrom(AttachmentsViewController.class)){
                        AttachmentsViewController attachmentsViewController =
                                (AttachmentsViewController)clazz.getConstructor(Node.class, Boolean.class)
                                        .newInstance(parent, false);
                        return attachmentsViewController;
                    }
                } catch (Exception e) {
                    Logger.getLogger(LogEntryTableViewController.class.getName()).log(Level.SEVERE, "Failed to construct controller for attachments view", e);
                }
                return null;
            });
            try {
                Node node = fxmlLoader.load();

                pane.addColumn(0, titleText, descriptionText, node);
            } catch (IOException e) {
                Logger.getLogger(LogEntryTableViewController.class.getName()).log(Level.WARNING, "Unable to load fxml for attachments view", e);
            }

            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            pane.getColumnConstraints().add(cc);

            return new TableCell<>() {
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

                        AttachmentsViewController controller = fxmlLoader.getController();
                        LogEntryModel model = new LogEntryModel(logEntry);

                        controller.setImages(model.getImages());
                        controller.setFiles(model.getFiles());
                        setGraphic(pane);
                    }
                }
            };

        });

        metaCol.setMaxWidth(1f * Integer.MAX_VALUE * 25);
        metaCol.setCellValueFactory(col -> new SimpleObjectProperty(col.getValue()));
        metaCol.setCellFactory(col -> {
            final GridPane pane = new GridPane();
            final Label logbooks = new Label();
            final Separator seperator = new Separator();
            final Label tags = new Label();
            pane.addColumn(0, logbooks, seperator, tags);

            return new TableCell<>() {
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

        // Bind ENTER key press to search
        query.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                search();
            }
        });
    }

    // Keeps track of when the animation is active. Multiple clicks will be ignored
    // until a give resize action is completed
    private AtomicBoolean moving = new AtomicBoolean(false);

    @FXML
    public void resize() {
        if (!moving.compareAndExchangeAcquire(false, true)) {
            if (resize.getText().equals(">")) {
                Duration cycleDuration = Duration.millis(400);
                KeyValue kv = new KeyValue(advancedSearchViewController.getPane().minWidthProperty(), 0);
                KeyValue kv2 = new KeyValue(advancedSearchViewController.getPane().maxWidthProperty(), 0);
                Timeline timeline = new Timeline(new KeyFrame(cycleDuration, kv, kv2));
                timeline.play();
                timeline.setOnFinished(event -> {
                    resize.setText("<");
                    moving.set(false);
                });
            } else {
                Duration cycleDuration = Duration.millis(400);
                double width = ViewSearchPane.getWidth() / 3;
                KeyValue kv = new KeyValue(advancedSearchViewController.getPane().minWidthProperty(), width);
                KeyValue kv2 = new KeyValue(advancedSearchViewController.getPane().prefWidthProperty(), width);
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
        Arrays.asList(query.getText().split("&")).forEach(s -> {
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

    @Override
    public void setLogs(List<LogEntry> logs) {
        List<LogEntry> copy = logs.stream()
                .sorted((one, two) -> two.getCreatedDate().compareTo(one.getCreatedDate()))
                .collect(Collectors.toList());

        this.logEntries = copy;
        refresh();
    }

    public void setQuery(String parsedQuery) {
        query.setText(parsedQuery);
        updateQuery();
        search();
    }

    public String getQuery() {
        return query.getText();
    }

    private void refresh() {
        if (logEntries != null) {
            ObservableList<LogEntry> logsList = FXCollections.observableArrayList();
            logsList.addAll(new ArrayList<>(logEntries));
            tableView.setItems(logsList);
        }
    }
}
