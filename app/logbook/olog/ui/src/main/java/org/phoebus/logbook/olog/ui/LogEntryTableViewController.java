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
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.util.Pair;
import javafx.util.StringConverter;
import org.csstudio.trends.databrowser3.archive.ArchiveFetchJob;
import org.csstudio.trends.databrowser3.archive.ArchiveFetchJobListener;
import org.csstudio.trends.databrowser3.model.ArchiveDataSource;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.csstudio.trends.databrowser3.model.PVSamples;
import org.csstudio.trends.databrowser3.model.RequestType;
import org.epics.vtype.VEnum;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.logbook.*;
import org.phoebus.logbook.olog.ui.query.OlogQuery;
import org.phoebus.logbook.olog.ui.query.OlogQueryManager;
import org.phoebus.logbook.olog.ui.write.LogEntryEditorStage;
import org.phoebus.logbook.olog.ui.write.LogEntryUpdateStage;
import org.phoebus.olog.es.api.model.LogGroupProperty;
import org.phoebus.olog.es.api.model.OlogLog;
import org.phoebus.security.store.SecureStore;
import org.phoebus.security.tokens.AuthenticationScope;
import org.phoebus.security.tokens.ScopedAuthenticationToken;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A controller for a log entry table with a collapsible advance search section.
 *
 * @author Kunal Shroff
 */
public class LogEntryTableViewController extends LogbookSearchController {

    @FXML
    private ComboBox<OlogQuery> query;

    // elements associated with the various search
    @FXML
    private GridPane ViewSearchPane;

    // elements related to the table view of the log entries
    @FXML
    private TableView<TableViewListItem> tableView;
    @FXML
    private TableColumn<TableViewListItem, TableViewListItem> descriptionCol;
    @FXML
    @SuppressWarnings({"UnusedDeclaration"})
    private LogEntryDisplayController logEntryDisplayController;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    @SuppressWarnings({"UnusedDeclaration"})
    private AdvancedSearchViewController advancedSearchViewController;

    @FXML
    private Pagination pagination;
    @FXML
    private Node searchResultView;

    @FXML
    private TextField pageSizeTextField;

    @FXML
    private Label openAdvancedSearchLabel;
    // Model
    private SearchResult searchResult;
    /**
     * List of selected log entries
     */
    private final ObservableList<LogEntry> selectedLogEntries = FXCollections.observableArrayList();
    private final Logger logger = Logger.getLogger(LogEntryTableViewController.class.getName());

    private final SimpleBooleanProperty showDetails = new SimpleBooleanProperty();

    private final SimpleBooleanProperty advancedSearchVisibile = new SimpleBooleanProperty(false);

    sealed interface DecorationDataLoadingStatus permits LoadingSuccessful,
                                                         LoadingInProgress,
                                                         ChannelNotFound,
                                                         FetchFailed,
                                                         PVIsNotOfEnumType {}

    public record LoadingSuccessful (TreeMap<Instant, VEnum> instantToVEnum) implements DecorationDataLoadingStatus { }
    public record LoadingInProgress () implements DecorationDataLoadingStatus, DecorationDataToDisplay { }
    public record ChannelNotFound () implements DecorationDataLoadingStatus, DecorationDataToDisplay { }
    public record FetchFailed () implements DecorationDataLoadingStatus, DecorationDataToDisplay { }
    public record PVIsNotOfEnumType () implements DecorationDataLoadingStatus, DecorationDataToDisplay { }

    sealed interface DecorationDataToDisplay permits DataToToDisplay,
                                                     LoadingInProgress,
                                                     ChannelNotFound,
                                                     FetchFailed,
                                                     PVIsNotOfEnumType {}

    public record DataToToDisplay(List<VEnum> instantToVEnum) implements DecorationDataToDisplay { }

    private final ConcurrentMap<Integer, Pair<String, DecorationDataLoadingStatus>> decorationIndexToPVNameAndStatus = new ConcurrentHashMap<>();

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

        searchParameters.addListener((observable, oldValue, newValue) -> query.getEditor().setText(newValue));

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
        menuItemNewLogEntry.setOnAction(ae -> new LogEntryEditorStage(new OlogLog(), null, null).show());

        MenuItem menuItemUpdateLogEntry = new MenuItem(Messages.UpdateLogEntry);
        menuItemUpdateLogEntry.visibleProperty().bind(Bindings.createBooleanBinding(() -> selectedLogEntries.size() == 1, selectedLogEntries));
        menuItemUpdateLogEntry.acceleratorProperty().setValue(new KeyCodeCombination(KeyCode.U, KeyCombination.CONTROL_DOWN));
        menuItemUpdateLogEntry.setOnAction(ae -> new LogEntryUpdateStage(selectedLogEntries.get(0), null).show());

        contextMenu.getItems().addAll(groupSelectedEntries, menuItemShowHideAll, menuItemNewLogEntry);
        if (LogbookUIPreferences.log_entry_update_support) {
            contextMenu.getItems().add(menuItemUpdateLogEntry);
        }
        contextMenu.setOnShowing(e -> {
            try {
                SecureStore store = new SecureStore();
                ScopedAuthenticationToken scopedAuthenticationToken = store.getScopedAuthenticationToken(AuthenticationScope.LOGBOOK);
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

        descriptionCol = new TableColumn<>();
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
                if ("".equals(newValue)) {
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
                .bind(Bindings.createStringBinding(() -> advancedSearchVisibile.get() ?
                                Messages.AdvancedSearchHide : Messages.AdvancedSearchOpen,
                        advancedSearchVisibile));

        search();
    }

    // Keeps track of when the animation is active. Multiple clicks will be ignored
    // until a give resize action is completed
    private final AtomicBoolean moving = new AtomicBoolean(false);

    @FXML
    public void resize() {
        if (!moving.compareAndExchangeAcquire(false, true)) {
            Duration cycleDuration = Duration.millis(400);
            Timeline timeline;
            if (advancedSearchVisibile.get()) {
                query.disableProperty().set(false);
                KeyValue kv = new KeyValue(advancedSearchViewController.getPane().minWidthProperty(), 0);
                KeyValue kv2 = new KeyValue(advancedSearchViewController.getPane().maxWidthProperty(), 0);
                timeline = new Timeline(new KeyFrame(cycleDuration, kv, kv2));
                timeline.setOnFinished(event -> {
                    advancedSearchVisibile.set(false);
                    moving.set(false);
                    search();
                });
            } else {
                searchParameters.setQuery(query.getEditor().getText());
                double width = ViewSearchPane.getWidth() / 2.5;
                KeyValue kv = new KeyValue(advancedSearchViewController.getPane().minWidthProperty(), width);
                KeyValue kv2 = new KeyValue(advancedSearchViewController.getPane().prefWidthProperty(), width);
                timeline = new Timeline(new KeyFrame(cycleDuration, kv, kv2));
                timeline.setOnFinished(event -> {
                    advancedSearchVisibile.set(true);
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
        params.put("sort", advancedSearchViewController.getSortAscending().get() ? "up" : "down");
        params.put("from", Integer.toString(pagination.getCurrentPageIndex() * pageSizeProperty.get()));
        params.put("size", Integer.toString(pageSizeProperty.get()));

        searchInProgress.set(true);
        logger.log(Level.INFO, "Single search: " + queryString);
        search(params,
                searchResult1 -> {
                    searchInProgress.set(false);
                    setSearchResult(searchResult1);
                    logger.log(Level.INFO, "Starting periodic search: " + queryString);
                    periodicSearch(params, this::setSearchResult);
                    List<OlogQuery> queries = ologQueryManager.getQueries();
                    Platform.runLater(() -> {
                        ologQueries.setAll(queries);
                        query.getSelectionModel().select(ologQueries.get(0));
                    });
                },
                (msg, ex) -> {
                    searchInProgress.set(false);
                    ExceptionDetailsErrorDialog.openError(Messages.LogbooksSearchFailTitle, ex.getMessage(), null);
                });
    }

    @Override
    public void setLogs(List<LogEntry> logs) {
        throw new RuntimeException(new UnsupportedOperationException());
    }

    private void setSearchResult(SearchResult searchResult) {
        this.searchResult = searchResult;
        Platform.runLater(() -> {
            hitCountProperty.set(searchResult.getHitCount());
            pageCountProperty.set(1 + (hitCountProperty.get() / pageSizeProperty.get()));
            refresh();
        });

        decorate(searchResult);
    }

    private Optional<String>[] pvNamesForDecoration = new Optional[0];

    protected void setPVNameForDecoration(int i, String pvName) {
        if (i >= pvNamesForDecoration.length) {
            Optional<String>[] newPvNamesForDecoration = new Optional[i + 1];
            for (int j = 0; j < newPvNamesForDecoration.length; j++) {
                newPvNamesForDecoration[j] = j < pvNamesForDecoration.length ? pvNamesForDecoration[j] : Optional.empty();
            }
            pvNamesForDecoration = newPvNamesForDecoration;
        }

        if (pvName.isEmpty()) {
            pvNamesForDecoration[i] = Optional.empty();
        } else {
            pvNamesForDecoration[i] = Optional.of(pvName);
        }
        decorate(i, searchResult);
    }

    private void decorate(SearchResult searchResult) {
        for (int i = 0; i < pvNamesForDecoration.length; i++) {
            decorate(i, searchResult);
        }
    }

    private void decorate(int i, SearchResult searchResult) {
        if (pvNamesForDecoration[i].isPresent()) {
            List<LogEntry> logEntries = searchResult.getLogs();
            List<Instant> createdDates = logEntries.stream().map(logEntry -> logEntry.getCreatedDate()).collect(Collectors.toUnmodifiableList());
            Instant start = Collections.min(createdDates);
            Instant end = Collections.max(createdDates);

            retrievePVValues(i, pvNamesForDecoration[i].get(), start, end);
        } else {
            decorationIndexToPVNameAndStatus.remove(i);
            refresh();
        }
    }

    private void retrievePVValues(int decorationIndex,
                                  String pvName,
                                  Instant start,
                                  Instant end) {
        PVItem pvItem = new PVItem(pvName, Double.MAX_VALUE);
        pvItem.setRequestType(RequestType.RAW);

        pvItem.useDefaultArchiveDataSources();

        ArchiveFetchJobListener archiveFetchJobListener = new ArchiveFetchJobListener() {

            private boolean channelFoundAtLeastOnce = true;

            @Override
            public void fetchCompleted(ArchiveFetchJob archiveFetchJob) {
                if (channelFoundAtLeastOnce) {
                    PVSamples samples = pvItem.getSamples();
                    Lock lock = samples.getLock();
                    lock.lock();

                    try {
                        TreeMap<Instant, VEnum> newInstantToValue = new TreeMap<>();
                        Optional<VEnum> mostRecentDataPointBeforeStart = Optional.empty(); // When merging data from multiple sources, there may be moe than one data point before the start of the time period.

                        boolean isEnumPV = true;
                        for (int i = 0; i < samples.size(); i++) {
                            if (!(samples.get(i).getVType() instanceof VEnum)) {
                                isEnumPV = false;
                            }
                        }

                        if (isEnumPV) {
                            for (int i = 0; i < samples.size(); i++) {
                                if (samples.get(i).getVType() instanceof VEnum vEnum) {
                                    if (vEnum.getTime().getTimestamp().equals(start) || vEnum.getTime().getTimestamp().isAfter(start)) {
                                        newInstantToValue.put(vEnum.getTime().getTimestamp(), vEnum);
                                    } else if (vEnum.getTime().getTimestamp().isBefore(start)) {
                                        if (mostRecentDataPointBeforeStart.isEmpty()) {
                                            mostRecentDataPointBeforeStart = Optional.of(vEnum);
                                        } else if (vEnum.getTime().getTimestamp().isAfter(mostRecentDataPointBeforeStart.get().getTime().getTimestamp())) {
                                            mostRecentDataPointBeforeStart = Optional.of(vEnum);
                                        }
                                    }
                                }
                            }
                            if (mostRecentDataPointBeforeStart.isPresent()) {
                                newInstantToValue.put(mostRecentDataPointBeforeStart.get().getTime().getTimestamp(), mostRecentDataPointBeforeStart.get());
                            }
                            decorationIndexToPVNameAndStatus.put(decorationIndex, new Pair(pvName, new LoadingSuccessful(newInstantToValue)));
                        }
                        else {
                            decorationIndexToPVNameAndStatus.put(decorationIndex, new Pair(pvName, new PVIsNotOfEnumType()));
                        }
                    } finally {
                        lock.unlock();
                    }
                }
                else {
                    decorationIndexToPVNameAndStatus.put(decorationIndex, new Pair(pvName, new ChannelNotFound()));
                }
                refresh();
            }

            @Override
            public void archiveFetchFailed(ArchiveFetchJob archiveFetchJob, ArchiveDataSource archiveDataSource, Exception e) {
                decorationIndexToPVNameAndStatus.put(decorationIndex, new Pair(pvName, new FetchFailed()));
                refresh();
            }

            @Override
            public void channelNotFound(ArchiveFetchJob archiveFetchJob,
                                        boolean channelFoundAtLeastOnce,
                                        List<ArchiveDataSource> list) {
                this.channelFoundAtLeastOnce = channelFoundAtLeastOnce;
            }
        };

        decorationIndexToPVNameAndStatus.put(decorationIndex, new Pair(pvName, new LoadingInProgress()));
        refresh();
        ArchiveFetchJob archiveFetchJob = new ArchiveFetchJob(pvItem,
                                                              start,
                                                              end,
                                                              archiveFetchJobListener); // Note: The archive fetch job is automatically scheduled by the constructor!

        return;
    }

    public void setQuery(String parsedQuery) {
        searchParameters.setQuery(parsedQuery);
        search();
    }

    public String getQuery() {
        return query.getValue().getQuery();
    }

    private synchronized void refresh() {
        if (this.searchResult != null) {
            List<TableViewListItem> selectedLogEntries = new ArrayList<>(tableView.getSelectionModel().getSelectedItems());

            List<LogEntry> logEntries = searchResult.getLogs();
            logEntries.sort((o1, o2) -> -(o1.getCreatedDate().compareTo(o2.getCreatedDate())));

            Map<LogEntry, SortedMap<Integer, Pair<String, DecorationDataToDisplay>>> logEntryToPVNameAndVEnumDecorationDataToDisplay = new TreeMap<>((o1, o2) -> -(o1.getCreatedDate().compareTo(o2.getCreatedDate())));
            logEntries.forEach(logEntry -> logEntryToPVNameAndVEnumDecorationDataToDisplay.put(logEntry, new TreeMap<>())); // Initialize logEntryToPVNameAndVEnumDecorationDataToDisplay
            {
                for (int i = 0; i < pvNamesForDecoration.length; i++) {
                    if (decorationIndexToPVNameAndStatus.containsKey(i)) {
                        Pair<String, DecorationDataLoadingStatus> pvNameAndStatus = decorationIndexToPVNameAndStatus.get(i);
                        String pvName = pvNameAndStatus.getKey();
                        DecorationDataLoadingStatus status = pvNameAndStatus.getValue();

                        if (status instanceof LoadingInProgress) {
                            for (int j = logEntries.size() - 1; j >= 0; j--) {
                                LogEntry currentLogEntry = logEntries.get(j);
                                logEntryToPVNameAndVEnumDecorationDataToDisplay.get(currentLogEntry).put(i, new Pair(pvName, new LoadingInProgress()));
                            }
                        }
                        else if (status instanceof FetchFailed) {
                            for (int j = logEntries.size() - 1; j >= 0; j--) {
                                LogEntry currentLogEntry = logEntries.get(j);
                                logEntryToPVNameAndVEnumDecorationDataToDisplay.get(currentLogEntry).put(i, new Pair(pvName, new FetchFailed()));
                            }
                        }
                        else if (status instanceof ChannelNotFound) {
                            for (int j = logEntries.size() - 1; j >= 0; j--) {
                                LogEntry currentLogEntry = logEntries.get(j);
                                logEntryToPVNameAndVEnumDecorationDataToDisplay.get(currentLogEntry).put(i, new Pair(pvName, new ChannelNotFound()));
                            }
                        }
                        else if (status instanceof PVIsNotOfEnumType) {
                            for (int j = logEntries.size() - 1; j >= 0; j--) {
                                LogEntry currentLogEntry = logEntries.get(j);
                                logEntryToPVNameAndVEnumDecorationDataToDisplay.get(currentLogEntry).put(i, new Pair(pvName, new PVIsNotOfEnumType()));
                            }
                        }
                        else if (status instanceof LoadingSuccessful loadingSuccessful) {
                            TreeMap<Instant, VEnum> instantToVEnum = loadingSuccessful.instantToVEnum;
                            Instant previousLogEntryCreatedDate = Instant.ofEpochSecond(0);
                            Optional<VEnum> previousLogEntryVEnum = Optional.empty();
                            for (int j = logEntries.size() - 1; j >= 0; j--) {
                                LogEntry currentLogEntry = logEntries.get(j);
                                Instant currentLogEntryCreatedDate = currentLogEntry.getCreatedDate();
                                List<VEnum> vEnumsFromPreviousLogEntryToCurrentLogEntry = new LinkedList<>(instantToVEnum.subMap(previousLogEntryCreatedDate, false,
                                                currentLogEntryCreatedDate, true)
                                        .values());
                                if (previousLogEntryVEnum.isPresent()) {
                                    // Append currentLogEntryVEnum to vEnumsFromPreviousLogEntryToCurrentLogEntry
                                    // so that the current status is known.
                                    vEnumsFromPreviousLogEntryToCurrentLogEntry.add(0, previousLogEntryVEnum.get());
                                }

                                Optional<VEnum> currentLogEntryVEnum;
                                if (vEnumsFromPreviousLogEntryToCurrentLogEntry.isEmpty()) {
                                    // No changes have occurred since the last log entry.
                                    // Therefore, the VEnum of the last log entry is still
                                    // the current one.

                                    currentLogEntryVEnum = previousLogEntryVEnum;
                                } else {
                                    currentLogEntryVEnum = Optional.of(vEnumsFromPreviousLogEntryToCurrentLogEntry.get(vEnumsFromPreviousLogEntryToCurrentLogEntry.size() - 1));
                                }
                                logEntryToPVNameAndVEnumDecorationDataToDisplay.get(currentLogEntry).put(i, new Pair(pvName, new DataToToDisplay(vEnumsFromPreviousLogEntryToCurrentLogEntry)));
                                previousLogEntryCreatedDate = currentLogEntryCreatedDate;
                                previousLogEntryVEnum = currentLogEntryVEnum;
                            }
                        }
                    } else {
                        for (int j = logEntries.size() - 1; j >= 0; j--) {
                            LogEntry currentLogEntry = logEntries.get(j);
                            logEntryToPVNameAndVEnumDecorationDataToDisplay.get(currentLogEntry).remove(i);
                        }
                    }
                }
            }

            boolean showDetailsBoolean = showDetails.get();
            var logs = logEntries.stream().map(le -> new TableViewListItem(le,
                                                                           showDetailsBoolean,
                                                                           logEntryToPVNameAndVEnumDecorationDataToDisplay.getOrDefault(le, new TreeMap<>()))).toList();

            ObservableList<TableViewListItem> logsList = FXCollections.observableArrayList(logs);
            tableView.setItems(logsList);

            // This will ensure that selected entries stay selected after the list has been
            // updated from the search result.
            for (TableViewListItem selectedItem : selectedLogEntries) {
                for (TableViewListItem item : tableView.getItems()) {
                    if (item.getLogEntry().getId().equals(selectedItem.getLogEntry().getId())) {
                        Platform.runLater(() -> {
                            if (goBackAndGoForwardActions.isPresent()) {
                                goBackAndGoForwardActions.get().setIsRecordingHistoryDisabled(true); // Do not create a "Back" action for the automatic reload.
                                tableView.getSelectionModel().select(item);
                                goBackAndGoForwardActions.get().setIsRecordingHistoryDisabled(false);
                            } else {
                                tableView.getSelectionModel().select(item);
                            }
                        });
                    }
                }
            }
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
        private SortedMap<Integer, Pair<String, DecorationDataToDisplay>> decorationIndexToPVNameAndVEnumValuesFromPreviousLogEntryToLogEntry = new TreeMap<>();

        public TableViewListItem(LogEntry logEntry,
                                 boolean showDetails,
                                 SortedMap<Integer, Pair<String, DecorationDataToDisplay>> decorationIndexToPVNameAndDecorationDataToDisplay) {
            this(logEntry, showDetails);

            Objects.requireNonNull(decorationIndexToPVNameAndDecorationDataToDisplay);
            this.decorationIndexToPVNameAndVEnumValuesFromPreviousLogEntryToLogEntry = decorationIndexToPVNameAndDecorationDataToDisplay;
        }

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

        protected SortedMap<Integer, Pair<String, DecorationDataToDisplay>> getDecorationIndexToPVNameAndVEnumValuesFromPreviousLogEntryToLogEntry() {
            return decorationIndexToPVNameAndVEnumValuesFromPreviousLogEntryToLogEntry;
        }
    }

    public void setShowDetails(boolean show) {
        showDetails.set(show);
    }

    public boolean getShowDetails() {
        return showDetails.get();
    }

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
        logEntryDisplayController.updateLogEntry(logEntry);
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
