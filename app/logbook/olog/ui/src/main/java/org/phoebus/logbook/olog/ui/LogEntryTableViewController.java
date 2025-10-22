package org.phoebus.logbook.olog.ui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Pagination;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.phoebus.applications.logbook.authentication.OlogAuthenticationScope;
import org.phoebus.core.websocket.WebSocketMessageHandler;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogService;
import org.phoebus.logbook.LogbookException;
import org.phoebus.logbook.LogbookPreferences;
import org.phoebus.logbook.SearchResult;
import org.phoebus.logbook.olog.ui.query.OlogQuery;
import org.phoebus.logbook.olog.ui.query.OlogQueryManager;
import org.phoebus.logbook.olog.ui.spi.Decoration;
import org.phoebus.logbook.olog.ui.write.EditMode;
import org.phoebus.logbook.olog.ui.write.LogEntryEditorStage;
import org.phoebus.olog.es.api.model.LogGroupProperty;
import org.phoebus.olog.es.api.model.OlogLog;
import org.phoebus.security.store.SecureStore;
import org.phoebus.security.tokens.ScopedAuthenticationToken;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A controller for a log entry table with a collapsible advance search section.
 *
 * @author Kunal Shroff
 */
public class LogEntryTableViewController extends LogbookSearchController implements WebSocketMessageHandler {

    @FXML
    @SuppressWarnings("unused")
    private ComboBox<OlogQuery> query;

    // elements associated with the various search
    @FXML
    @SuppressWarnings("unused")
    private GridPane viewSearchPane;

    @SuppressWarnings("unused")
    @FXML
    private SplitPane splitPane;

    // elements related to the table view of the log entries
    @FXML
    @SuppressWarnings("unused")
    private TableView<TableViewListItem> tableView;
    @FXML
    @SuppressWarnings({"UnusedDeclaration"})
    private LogEntryDisplayController logEntryDisplayController;
    @FXML
    @SuppressWarnings("unused")
    private ProgressIndicator progressIndicator;
    @FXML
    @SuppressWarnings({"UnusedDeclaration"})
    private AdvancedSearchViewController advancedSearchViewController;

    @FXML
    @SuppressWarnings("unused")
    private Pagination pagination;
    @FXML
    @SuppressWarnings("unused")
    private Node searchResultView;

    @FXML
    @SuppressWarnings("unused")
    private TextField pageSizeTextField;

    @SuppressWarnings("unused")
    @FXML
    private Pane logDetailView;

    @SuppressWarnings("unused")
    @FXML
    private VBox errorPane;

    @FXML
    @SuppressWarnings("unused")
    private Label openAdvancedSearchLabel;
    // Model
    private SearchResult searchResult;

    /**
     * List of selected log entries
     */
    private final ObservableList<LogEntry> selectedLogEntries = FXCollections.observableArrayList();
    private static final Logger logger = Logger.getLogger(LogEntryTableViewController.class.getName());

    private final SimpleBooleanProperty showDetails = new SimpleBooleanProperty();
    private final SimpleBooleanProperty advancedSearchVisible = new SimpleBooleanProperty(false);


    /**
     * Constructor.
     *
     * @param logClient Log client implementation
     */
    public LogEntryTableViewController(LogClient logClient,
                                       OlogQueryManager ologQueryManager,
                                       SearchParameters searchParameters) {
        setClient(logClient);
        this.ologQueryManager = ologQueryManager;
        this.searchParameters = searchParameters;
    }

    protected void setGoBackAndGoForwardActions(LogEntryTable.GoBackAndGoForwardActions goBackAndGoForwardActions) {
        this.goBackAndGoForwardActions = Optional.of(goBackAndGoForwardActions);
    }

    private final SimpleIntegerProperty hitCountProperty = new SimpleIntegerProperty(0);
    private final SimpleIntegerProperty pageSizeProperty =
            new SimpleIntegerProperty(LogbookUIPreferences.search_result_page_size);
    private final SimpleIntegerProperty pageCountProperty = new SimpleIntegerProperty(0);
    private final OlogQueryManager ologQueryManager;
    private final ObservableList<OlogQuery> ologQueries = FXCollections.observableArrayList();
    private final SimpleBooleanProperty userHasSignedIn = new SimpleBooleanProperty(false);

    private final SearchParameters searchParameters;

    protected Optional<LogEntryTable.GoBackAndGoForwardActions> goBackAndGoForwardActions = Optional.empty();

    @FXML
    public void initialize() {

        logEntryDisplayController.setLogEntryTableViewController(this);

        advancedSearchViewController.setSearchCallback(this::search);

        configureComboBox();
        ologQueries.setAll(ologQueryManager.getQueries());

        searchParameters.addListener((observable, oldValue, newValue) -> {
            query.getEditor().setText(newValue);
        });

        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        MenuItem groupSelectedEntries = new MenuItem(Messages.GroupSelectedEntries);
        groupSelectedEntries.setOnAction(e -> createLogEntryGroup());

        groupSelectedEntries.disableProperty()
                .bind(Bindings.createBooleanBinding(() ->
                        selectedLogEntries.size() < 2 || userHasSignedIn.not().get(), selectedLogEntries, userHasSignedIn));
        ContextMenu contextMenu = new ContextMenu();

        MenuItem menuItemShowHideAll = new MenuItem(Messages.ShowHideDetails);
        menuItemShowHideAll.acceleratorProperty().setValue(new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        menuItemShowHideAll.setOnAction(ae -> {
            showDetails.set(!showDetails.get());
            tableView.getItems().forEach(item -> item.setShowDetails(!item.isShowDetails().get()));
        });

        MenuItem menuItemNewLogEntry = new MenuItem(Messages.NewLogEntry);
        menuItemNewLogEntry.acceleratorProperty().setValue(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
        menuItemNewLogEntry.setOnAction(ae -> new LogEntryEditorStage(new OlogLog(), null, EditMode.NEW_LOG_ENTRY).showAndWait());

        MenuItem menuItemUpdateLogEntry = new MenuItem(Messages.UpdateLogEntry);
        menuItemUpdateLogEntry.visibleProperty().bind(Bindings.createBooleanBinding(() -> selectedLogEntries.size() == 1, selectedLogEntries));
        menuItemUpdateLogEntry.acceleratorProperty().setValue(new KeyCodeCombination(KeyCode.U, KeyCombination.CONTROL_DOWN));
        menuItemUpdateLogEntry.setOnAction(ae -> new LogEntryEditorStage(selectedLogEntries.get(0), null, EditMode.UPDATE_LOG_ENTRY).show());

        contextMenu.getItems().addAll(groupSelectedEntries, menuItemShowHideAll, menuItemNewLogEntry);
        if (LogbookUIPreferences.log_entry_update_support) {
            contextMenu.getItems().add(menuItemUpdateLogEntry);
        }
        contextMenu.setOnShowing(e -> {
            try {
                SecureStore store = new SecureStore();
                ScopedAuthenticationToken scopedAuthenticationToken = store.getScopedAuthenticationToken(new OlogAuthenticationScope());
                userHasSignedIn.set(scopedAuthenticationToken != null);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Secure Store file not found.", ex);
            }
        });

        tableView.setContextMenu(contextMenu);

        // The display table.
        tableView.getColumns().clear();
        tableView.setEditable(false);
        tableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            // Update detailed view, but only if selection contains a single item.
            if (newValue != null && tableView.getSelectionModel().getSelectedItems().size() == 1) {
                if (goBackAndGoForwardActions.isPresent() && !goBackAndGoForwardActions.get().getIsRecordingHistoryDisabled()) {
                    goBackAndGoForwardActions.get().addGoBackAction();
                    goBackAndGoForwardActions.get().goForwardActions.clear();
                }
                logEntryDisplayController.setLogEntry(newValue.getLogEntry());
            }
            List<LogEntry> logEntries = tableView.getSelectionModel().getSelectedItems()
                    .stream().map(TableViewListItem::getLogEntry).collect(Collectors.toList());
            selectedLogEntries.setAll(logEntries);
        });

        tableView.getStylesheets().add(this.getClass().getResource("/search_result_view.css").toExternalForm());
        pagination.getStylesheets().add(this.getClass().getResource("/pagination.css").toExternalForm());

        TableColumn<TableViewListItem, TableViewListItem> descriptionCol = new TableColumn<>();
        descriptionCol.setMaxWidth(1f * Integer.MAX_VALUE * 100);
        descriptionCol.setCellValueFactory(col -> new SimpleObjectProperty<>(col.getValue()));
        descriptionCol.setCellFactory(col -> new TableCell<>() {
            {
                setStyle("-fx-padding: -1px");
            }

            private final Node graphic;
            private final PseudoClass childlessTopLevel =
                    PseudoClass.getPseudoClass("grouped");
            private final LogEntryCellController controller;

            {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("LogEntryCell.fxml"));
                    graphic = loader.load();
                    controller = loader.getController();
                    controller.setDecorations(decorations);
                } catch (IOException exc) {
                    throw new RuntimeException(exc);
                }
            }

            @Override
            public void updateItem(TableViewListItem logEntry, boolean empty) {
                super.updateItem(logEntry, empty);
                if (empty) {
                    setGraphic(null);
                    pseudoClassStateChanged(childlessTopLevel, false);
                } else {
                    controller.setLogEntry(logEntry);
                    setGraphic(graphic);
                    boolean b = LogGroupProperty.getLogGroupProperty(logEntry.getLogEntry()).isPresent();
                    pseudoClassStateChanged(childlessTopLevel, b);
                }
            }
        });

        tableView.getColumns().add(descriptionCol);
        tableView.setPlaceholder(new Label(Messages.NoSearchResults));

        progressIndicator.visibleProperty().bind(searchInProgress);

        searchResultView.disableProperty().bind(searchInProgress);

        pagination.currentPageIndexProperty().addListener((a, b, c) -> search());

        pageSizeTextField.setText(Integer.toString(pageSizeProperty.get()));

        Pattern DIGIT_PATTERN = Pattern.compile("\\d*");
        // This is to accept numerical input only, and at most 3 digits (maximizing search to 999 hits).
        pageSizeTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (DIGIT_PATTERN.matcher(newValue).matches()) {
                if (newValue.isEmpty()) {
                    pageSizeProperty.set(LogbookUIPreferences.search_result_page_size);
                } else if (newValue.length() > 3) {
                    pageSizeTextField.setText(oldValue);
                } else {
                    pageSizeProperty.set(Integer.parseInt(newValue));
                }
            } else {
                pageSizeTextField.setText(oldValue);
            }
        });

        // Hide the pagination widget if hit count == 0 or page count < 2
        pagination.visibleProperty().bind(Bindings.createBooleanBinding(() -> hitCountProperty.get() > 0 && pagination.pageCountProperty().get() > 1,
                hitCountProperty, pagination.pageCountProperty()));
        pagination.pageCountProperty().bind(pageCountProperty);
        pagination.maxPageIndicatorCountProperty().bind(pageCountProperty);

        query.itemsProperty().bind(new SimpleObjectProperty<>(ologQueries));

        query.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                search();
            }
        });
        query.getEditor().setText(ologQueries.get(0).getQuery());
        query.getSelectionModel().select(ologQueries.get(0));
        searchParameters.setQuery(ologQueries.get(0).getQuery());

        openAdvancedSearchLabel.setOnMouseClicked(e -> resize());

        openAdvancedSearchLabel.textProperty()
                .bind(Bindings.createStringBinding(() -> advancedSearchVisible.get() ?
                                Messages.AdvancedSearchHide : Messages.AdvancedSearchOpen,
                        advancedSearchVisible));

        determineConnectivity(connectivityMode -> {
            connectivityModeObjectProperty.set(connectivityMode);
            connectivityCheckerCountDownLatch.countDown();
            switch (connectivityMode) {
                case HTTP_ONLY -> search();
                case WEB_SOCKETS_SUPPORTED -> connectWebSocket();
            }
        });

    }

    // Keeps track of when the animation is active. Multiple clicks will be ignored
    // until a give resize action is completed
    private final AtomicBoolean moving = new AtomicBoolean(false);

    @FXML
    public void resize() {
        if (!moving.compareAndExchangeAcquire(false, true)) {
            Duration cycleDuration = Duration.millis(400);
            Timeline timeline;
            if (advancedSearchVisible.get()) {
                query.disableProperty().set(false);
                KeyValue kv = new KeyValue(advancedSearchViewController.getPane().minWidthProperty(), 0);
                KeyValue kv2 = new KeyValue(advancedSearchViewController.getPane().maxWidthProperty(), 0);
                timeline = new Timeline(new KeyFrame(cycleDuration, kv, kv2));
                timeline.setOnFinished(event -> {
                    advancedSearchVisible.set(false);
                    moving.set(false);
                    search();
                });
            } else {
                searchParameters.setQuery(query.getEditor().getText());
                double width = viewSearchPane.getWidth() / 2.5;
                KeyValue kv = new KeyValue(advancedSearchViewController.getPane().minWidthProperty(), width);
                KeyValue kv2 = new KeyValue(advancedSearchViewController.getPane().prefWidthProperty(), width);
                timeline = new Timeline(new KeyFrame(cycleDuration, kv, kv2));
                timeline.setOnFinished(event -> {
                    advancedSearchVisible.set(true);
                    moving.set(false);
                    query.disableProperty().set(true);
                });
            }
            timeline.play();
        }
    }

    /**
     * Performs a single search based on the current query. If the search completes successfully,
     * the UI is updated and a periodic search is launched using the same query. If on the other hand
     * the search fails (service off-line or invalid query), a periodic search is NOT launched.
     */
    @Override
    public void search() {
        // In case the page size text field is empty, or the value is zero, set the page size to the default
        if ("".equals(pageSizeTextField.getText()) || Integer.parseInt(pageSizeTextField.getText()) == 0) {
            pageSizeTextField.setText(Integer.toString(LogbookUIPreferences.search_result_page_size));
        }

        searchInProgress.set(true);

        String queryString = query.getEditor().getText();
        // Construct the query parameters from the search field string. Note that some keys
        // are treated as "hidden" and removed in the returned map.

        Map<String, String> params =
                LogbookQueryUtil.parseHumanReadableQueryString(ologQueryManager.getOrAddQuery(queryString).getQuery());
        params.put("sort", advancedSearchViewController.getSortAscending() ? "up" : "down");
        params.put("from", Integer.toString(pagination.getCurrentPageIndex() * pageSizeProperty.get()));
        params.put("size", Integer.toString(pageSizeProperty.get()));

        searchInProgress.set(true);
        logger.log(Level.INFO, "Single search: " + queryString);
        search(params,
                searchResult1 -> {
                    searchInProgress.set(false);
                    setSearchResult(searchResult1);
                    List<OlogQuery> queries = ologQueryManager.getQueries();
                    if (connectivityModeObjectProperty.get().equals(ConnectivityMode.HTTP_ONLY)) {
                        logger.log(Level.INFO, "Starting periodic search: " + queryString);
                        periodicSearch(params, this::setSearchResult);
                    }
                    Platform.runLater(() -> {
                        ologQueries.setAll(queries);
                        query.getSelectionModel().select(ologQueries.get(0));
                    });
                },
                (msg, ex) -> {
                    searchInProgress.set(false);
                    ExceptionDetailsErrorDialog.openError(splitPane, Messages.SearchFailed, "", ex);
                });
    }

    @Override
    public void setLogs(List<LogEntry> logs) {
        throw new RuntimeException(new UnsupportedOperationException());
    }

    private List<Decoration> decorations;

    protected void setDecorations(List<Decoration> decorations) {
        this.decorations = decorations;
        for (Decoration decoration : decorations) {
            decoration.setRefreshLogEntryTableView(() -> refresh());
        }
    }

    private void setSearchResult(SearchResult searchResult) {
        this.searchResult = searchResult;

        List<LogEntry> logEntries = searchResult.getLogs();
        decorations.forEach(decoration -> decoration.setLogEntries(logEntries));

        Platform.runLater(() -> {
            hitCountProperty.set(searchResult.getHitCount());
            pageCountProperty.set(1 + (hitCountProperty.get() / pageSizeProperty.get()));
            refresh();
        });
    }

    public void setQuery(String parsedQuery) {
        searchParameters.setQuery(parsedQuery);
        search();
    }

    public String getQuery() {
        return query.getValue().getQuery();
    }

    private void refresh() {
        Runnable refreshRunnable = () -> {
            if (this.searchResult != null) {
                List<TableViewListItem> selectedLogEntries = new ArrayList<>(tableView.getSelectionModel().getSelectedItems());

                List<LogEntry> logEntries = searchResult.getLogs();
                logEntries.sort((o1, o2) -> advancedSearchViewController.getSortAscending() ? o1.getCreatedDate().compareTo(o2.getCreatedDate()) :
                        -(o1.getCreatedDate().compareTo(o2.getCreatedDate())));

                boolean showDetailsBoolean = showDetails.get();
                var logs = logEntries.stream().map(le -> new TableViewListItem(le, showDetailsBoolean)).toList();

                ObservableList<TableViewListItem> logsList = FXCollections.observableArrayList(logs);
                tableView.setItems(logsList);

                // This will ensure that selected entries stay selected after the list has been
                // updated from the search result.
                for (TableViewListItem selectedItem : selectedLogEntries) {
                    for (TableViewListItem item : tableView.getItems()) {
                        if (item.getLogEntry().getId().equals(selectedItem.getLogEntry().getId())) {
                            if (goBackAndGoForwardActions.isPresent()) {
                                goBackAndGoForwardActions.get().setIsRecordingHistoryDisabled(true); // Do not create a "Back" action for the automatic reload.
                                tableView.getSelectionModel().select(item);
                                goBackAndGoForwardActions.get().setIsRecordingHistoryDisabled(false);
                            } else {
                                tableView.getSelectionModel().select(item);
                            }
                        }
                    }
                }
            }
        };

        if (Platform.isFxApplicationThread()) {
            refreshRunnable.run();
        } else {
            Platform.runLater(refreshRunnable);
        }
    }

    private void createLogEntryGroup() {
        List<Long> logEntryIds =
                selectedLogEntries.stream().map(LogEntry::getId).collect(Collectors.toList());
        JobManager.schedule("Group log entries", monitor -> {
            try {
                LogClient logClient = LogService.getInstance().getLogFactories().get(LogbookPreferences.logbook_factory).getLogClient();
                logClient.groupLogEntries(logEntryIds);
                search();
            } catch (LogbookException e) {
                logger.log(Level.INFO, "Unable to create log entry group from selection");
                Platform.runLater(() -> {
                    final Alert dialog = new Alert(AlertType.ERROR);
                    dialog.setHeaderText(Messages.GroupingFailed);
                    DialogHelper.positionDialog(dialog, tableView, 0, 0);
                    dialog.showAndWait();
                });
            }
        });
    }

    @FXML
    @SuppressWarnings("unused")
    public void goToFirstPage() {
        pagination.setCurrentPageIndex(0);
    }

    @FXML
    @SuppressWarnings("unused")
    public void goToLastPage() {
        pagination.setCurrentPageIndex(pagination.pageCountProperty().get() - 1);
    }

    private void configureComboBox() {
        Font defaultQueryFont = Font.font("Liberation Sans", FontWeight.BOLD, 12);
        Font defaultQueryFontRegular = Font.font("Liberation Sans", FontWeight.NORMAL, 12);
        query.setVisibleRowCount(OlogQueryManager.getInstance().getQueryListSize());
        // Needed to customize item rendering, e.g. default query rendered in bold.
        query.setCellFactory(
                new Callback<>() {
                    @Override
                    public ListCell<OlogQuery> call(ListView<OlogQuery> param) {
                        return new ListCell<>() {
                            @Override
                            public void updateItem(OlogQuery item,
                                                   boolean empty) {
                                super.updateItem(item, empty);
                                if (item != null) {
                                    setText(item.getQuery().isEmpty() ? "<empty>" : item.getQuery());
                                    if (item.isDefaultQuery()) {
                                        setFont(defaultQueryFont);
                                    } else {
                                        setFont(defaultQueryFontRegular);
                                    }
                                } else {
                                    setText(null);
                                }
                            }
                        };
                    }
                });

        // This is needed for the "editor" part of the ComboBox
        query.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(OlogQuery query) {
                        if (query == null) {
                            return "";
                        } else {
                            return query.getQuery();
                        }
                    }

                    @Override
                    public OlogQuery fromString(String s) {
                        return new OlogQuery(s);
                    }
                });

    }

    /**
     * Wrapper class for a {@link LogEntry} and a flag indicating whether details of the
     * log entry meta-data should be rendered in the list view.
     */
    public static class TableViewListItem {
        private final SimpleBooleanProperty showDetails = new SimpleBooleanProperty(true);
        private final LogEntry logEntry;

        public TableViewListItem(LogEntry logEntry, boolean showDetails) {
            this.logEntry = logEntry;
            this.showDetails.set(showDetails);
        }

        public SimpleBooleanProperty isShowDetails() {
            return showDetails;
        }

        public LogEntry getLogEntry() {
            return logEntry;
        }

        public void setShowDetails(boolean show) {
            this.showDetails.set(show);
        }
    }

    public void setShowDetails(boolean show) {
        showDetails.set(show);
    }

    public boolean getShowDetails() {
        return showDetails.get();
    }

    @SuppressWarnings("unused")
    public void showHelp() {
        new HelpViewer(LogbookUIPreferences.search_help).show();
    }

    /**
     * Handler for a {@link LogEntry} change, new or updated.
     * A search is triggered to make sure the result list reflects the change, and
     * the detail view controller is called to refresh, if applicable.
     *
     * @param logEntry A {@link LogEntry}
     */
    public void logEntryChanged(LogEntry logEntry) {
        search();
        setLogEntry(logEntry);
    }

    protected LogEntry getLogEntry() {
        return logEntryDisplayController.getLogEntry();
    }

    protected void setLogEntry(LogEntry logEntry) {
        logEntryDisplayController.setLogEntry(logEntry);
    }

    /**
     * Selects a log entry as a result of an action outside the {@link TreeView}, but selection happens on the
     * {@link TreeView} item, if it exists (match on log entry id). If it does not exist, selection is cleared
     * anyway to indicate that user selected log entry is not visible in {@link TreeView}.
     *
     * @param logEntry User selected log entry.
     * @return <code>true</code> if user selected log entry is present in {@link TreeView}, otherwise
     * <code>false</code>.
     */
    public boolean selectLogEntry(LogEntry logEntry) {
        tableView.getSelectionModel().clearSelection();
        Optional<TableViewListItem> optional = tableView.getItems().stream().filter(i -> i.getLogEntry().getId().equals(logEntry.getId())).findFirst();
        if (optional.isPresent()) {
            tableView.getSelectionModel().select(optional.get());
            return true;
        }
        return false;
    }
}
