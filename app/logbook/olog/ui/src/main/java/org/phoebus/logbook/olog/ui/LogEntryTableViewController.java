package org.phoebus.logbook.olog.ui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogEntryImpl.LogEntryBuilder;
import org.phoebus.logbook.LogbookException;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.olog.ui.LogbookQueryUtil.Keys;
import org.phoebus.olog.es.api.model.LogGroupProperty;
import org.phoebus.ui.dialog.DialogHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A controller for a log entry table with a collapsible advance search section.
 *
 * @author Kunal Shroff
 */
public class LogEntryTableViewController extends LogbookSearchController {

    @FXML
    private Button resize;
    @FXML
    private TextField query;

    // elements associated with the various search
    @FXML
    private GridPane ViewSearchPane;

    // elements related to the table view of the log entires
    @FXML
    private TreeView<LogEntry> treeView;

    @FXML
    private LogEntryDisplayController logEntryDisplayController;

    @FXML
    private Node topLevelNode;
    @FXML
    private AdvancedSearchViewController advancedSearchViewController;

    // Model
    List<LogEntry> logEntries;

    // TreeTable root item
    TreeItem<LogEntry> rootItem =
            new TreeItem<>(LogEntryBuilder.log().id(-1L).description("Dummy root item").build());

    // Search parameters
    ObservableMap<Keys, String> searchParameters;

    /**
     * List of selected log entries
     */
    private ObservableList<LogEntry> selectedLogEntries = FXCollections.observableArrayList();

    private Logger logger = Logger.getLogger(LogEntryTableViewController.class.getName());

    /**
     * Constructor.
     *
     * @param logClient Log client implementation
     */
    public LogEntryTableViewController(LogClient logClient) {
        setClient(logClient);
    }


    @FXML
    public void initialize() {

        searchParameters = FXCollections.observableHashMap();

        LogbookQueryUtil.parseQueryString(LogbookUIPreferences.default_logbook_query).entrySet().stream().forEach(entry -> {
            searchParameters.put(Keys.findKey(entry.getKey()), entry.getValue());
        });
        advancedSearchViewController.setSearchParameters(searchParameters);

        query.setText(searchParameters.entrySet().stream()
                .sorted(Entry.comparingByKey())
                .map((e) -> e.getKey().getName().trim() + "=" + e.getValue().trim())
                .collect(Collectors.joining("&")));

        searchParameters.addListener((MapChangeListener<Keys, String>) change -> query.setText(searchParameters.entrySet().stream()
                .sorted(Entry.comparingByKey())
                .map((e) -> e.getKey().getName().trim() + "=" + e.getValue().trim())
                .collect(Collectors.joining("&"))));

        // The log entry tree.
        treeView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<LogEntry>>() {
            @Override
            public void changed(ObservableValue<? extends TreeItem<LogEntry>> observable, TreeItem<LogEntry> oldValue, TreeItem<LogEntry> newValue) {
                // newValue may be null after search and refresh of the tree
                if (newValue != null) {
                    logEntryDisplayController.setLogEntry(newValue.getValue());
                }
                selectedLogEntries
                        .setAll(treeView.getSelectionModel().getSelectedItems().stream().map(TreeItem::getValue).collect(Collectors.toList()));
            }
        });

        treeView.setCellFactory(tv -> new LogEntryTreeCell());
        treeView.getStylesheets().add(this.getClass().getResource("/tree_view.css").toExternalForm());

        // Bind ENTER key press to search
        query.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                search();
            }
        });

        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        treeView.setRoot(rootItem);
        treeView.setShowRoot(false);

        MenuItem groupSelectedEntries = new MenuItem(Messages.GroupSelectedEntries);
        groupSelectedEntries.setOnAction(e -> {
            createLogEntryGroup();
        });
        groupSelectedEntries.disableProperty()
                .bind(Bindings.createBooleanBinding(() ->
                        selectedLogEntries.size() < 2, selectedLogEntries));
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().add(groupSelectedEntries);
        treeView.setContextMenu(contextMenu);
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
        String queryText = query.getText();
        searchParameters.clear();
        LogbookQueryUtil.parseHumanReadableQueryString(queryText).entrySet().stream().forEach(entry -> {
            searchParameters.put(Keys.findKey(entry.getKey()), entry.getValue());
        });
    }

    @FXML
    public void search() {
        // parse the various time representations to Instant
        treeView.getSelectionModel().clearSelection();
        super.search(LogbookQueryUtil.parseQueryString(query.getText()));
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

    public String getQuery() {
        return query.getText();
    }

    private void refresh() {
        if (logEntries != null) {
            ObservableList<TreeItem<LogEntry>> tree =
                    LogEntryTreeHelper.createTree(logEntries);
            rootItem.getChildren().setAll(tree);
            treeView.getSelectionModel().selectFirst();
        }
    }


    private class LogEntryTreeCell extends TreeCell<LogEntry> {

        private Node graphic;
        private LogEntryCellController controller;
        private final PseudoClass childlessTopLevel =
                PseudoClass.getPseudoClass("childless-top-level");
        private final PseudoClass child =
                PseudoClass.getPseudoClass("child");

        public LogEntryTreeCell() {
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
                return;
            } else {
                controller.setLogEntry(logEntry);
                setGraphic(graphic);
            }
            boolean b1 = getTreeItem().getParent().getParent() == null;
            boolean b2 = getTreeItem().getChildren().size() == 0;
            pseudoClassStateChanged(childlessTopLevel, b1 && b2);
            pseudoClassStateChanged(child, !b1);
        }
    }

    private void createLogEntryGroup() {
        try {
            Property logGroupProperty = getLogEntryGroupProperty(selectedLogEntries);
            // Update all log entries asynchronously
            JobManager.schedule("Update log entries", monitor -> {
                selectedLogEntries.forEach(l -> {
                    // Update only if log entry does not contains the log group property
                    if (LogGroupProperty.getLogGroupProperty(l).isEmpty()) {
                        l.getProperties().add(logGroupProperty);
                        try {
                            getClient().updateLogEntry(l);
                        } catch (LogbookException e) {
                            logger.log(Level.SEVERE, "Failed to update log entry " + l.getId(), e);
                        }
                    }
                });
                // When all log entries are updated, run the search to trigger an update of the UI
                search();
            });

        } catch (LogbookException e) {
            final Alert dialog = new Alert(AlertType.INFORMATION);
            dialog.setHeaderText("Cannot create log entry group. Selected list of log entries references more than one existing group.");
            DialogHelper.positionDialog(dialog, treeView, 0, 0);
            dialog.showAndWait();
        }
    }

    protected Property getLogEntryGroupProperty(List<LogEntry> logEntries) throws LogbookException {
        List<Property> logGroupProperties = getLogEntryGroupProperties(logEntries);
        if (logGroupProperties.size() > 1) {
            throw new LogbookException("Selected log entries contain more than one log group property id");
        }
        if (logGroupProperties.isEmpty()) {
            return LogGroupProperty.create();
        } else {
            return logGroupProperties.get(0);
        }
    }

    private List<Property> getLogEntryGroupProperties(List<LogEntry> logEntries) {
        List<Property> logEntryGroupProperties = new ArrayList<>();
        logEntries.forEach(l -> {
            Optional<Property> logGroupProperty =
                    LogGroupProperty.getLogGroupProperty(l);
            if (logGroupProperty.isPresent()) {
                logEntryGroupProperties.add(logGroupProperty.get());
            }
        });
        return logEntryGroupProperties;
    }
}
