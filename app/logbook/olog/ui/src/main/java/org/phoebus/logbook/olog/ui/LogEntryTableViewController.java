package org.phoebus.logbook.olog.ui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.image.attributes.ImageAttributesExtension;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.olog.ui.LogbookQueryUtil.Keys;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.util.time.TimeParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * A controller for a log entry table with a collapsible advance search section.
 * @author Kunal Shroff
 *
 */
public class LogEntryTableViewController extends LogbookSearchController {

    static final Image tag = ImageCache.getImage(LogEntryDisplayController.class, "/icons/add_tag.png");
    static final Image logbook = ImageCache.getImage(LogEntryDisplayController.class, "/icons/logbook-16.png");

    @FXML
    private Button resize;
    @FXML
    private Button search;
    @FXML
    private TextField query;

    // elements associated with the various search
    @FXML
    private GridPane ViewSearchPane;

    // elements related to the table view of the log entires
    @FXML
    private TableView<LogEntry> tableView;
    @FXML
    private TableColumn<LogEntry, LogEntry> descriptionCol;

    // detail logview
    @FXML
    public SplitPane logEntryDisplay;
    @FXML
    private LogEntryDisplayController logEntryDisplayController;

    @FXML
    private Node topLevelNode;
    @FXML
    private AdvancedSearchViewController advancedSearchViewController;

    // Model
    List<LogEntry> logEntries;

    // Search parameters
    ObservableMap<Keys, String> searchParameters;

    /**
     * Constructor.
     * @param logClient Log client implementation
     */
    public LogEntryTableViewController(LogClient logClient){
        setClient(logClient);
        List<Extension> extensions = Arrays.asList(TablesExtension.create(), ImageAttributesExtension.create());
    }


    @FXML
    public void initialize() {

        resize.setText(">");

        searchParameters = FXCollections.observableHashMap();
        searchParameters.put(Keys.SEARCH, "*");
        searchParameters.put(Keys.STARTTIME, TimeParser.format(java.time.Duration.ofHours(8)));
        searchParameters.put(Keys.ENDTIME, TimeParser.format(java.time.Duration.ZERO));
        advancedSearchViewController.setSearchParameters(searchParameters);

        query.setText(searchParameters.entrySet().stream()
                .sorted(Entry.comparingByKey())
                .map((e) -> e.getKey().getName().trim() + "=" + e.getValue().trim())
                .collect(Collectors.joining("&")));

        searchParameters.addListener((MapChangeListener<Keys, String>) change -> query.setText(searchParameters.entrySet().stream()
                .sorted(Entry.comparingByKey())
                .map((e) -> e.getKey().getName().trim() + "=" + e.getValue().trim())
                .collect(Collectors.joining("&"))));

        // The display table.
        tableView.getColumns().clear();
        tableView.setEditable(false);
        tableView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<LogEntry>() {
            @Override
            public void changed(ObservableValue<? extends LogEntry> observable, LogEntry oldValue, LogEntry newValue) {
                logEntryDisplayController.setLogEntry(newValue);
            }
        });

        descriptionCol = new TableColumn<>("Log");
        descriptionCol.setMaxWidth(1f * Integer.MAX_VALUE * 100);
        descriptionCol.setCellValueFactory(col -> new SimpleObjectProperty(col.getValue()));
        descriptionCol.setCellFactory(col -> {

            return new TableCell<>() {
                private Node graphic ;
                private LogEntryCellController controller ;

                {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("LogEntryCell.fxml"));
                        graphic = loader.load();
                        controller = loader.getController();
                    } catch (IOException exc) {
                        throw new RuntimeException(exc);
                    }
                }

                @Override
                public void updateItem(LogEntry logEntry, boolean empty) {
                    super.updateItem(logEntry, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        controller.setLogEntry(logEntry);
                        setGraphic(graphic);
                    }
                }
            };
        });

        tableView.getColumns().add(descriptionCol);

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
            if (resize.getText().equals("<")) {
                Duration cycleDuration = Duration.millis(400);
                KeyValue kv = new KeyValue(advancedSearchViewController.getPane().minWidthProperty(), 0);
                KeyValue kv2 = new KeyValue(advancedSearchViewController.getPane().maxWidthProperty(), 0);
                Timeline timeline = new Timeline(new KeyFrame(cycleDuration, kv, kv2));
                timeline.play();
                timeline.setOnFinished(event -> {
                    resize.setText(">");
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
                    resize.setText("<");
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

    @FXML
    public void setSelection() {

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
