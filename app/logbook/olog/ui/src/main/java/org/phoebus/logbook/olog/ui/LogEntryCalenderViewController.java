package org.phoebus.logbook.olog.ui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.util.StringConverter;
import jfxtras.scene.control.agenda.Agenda;
import jfxtras.scene.control.agenda.Agenda.Appointment;
import jfxtras.scene.control.agenda.Agenda.AppointmentGroup;
import jfxtras.scene.control.agenda.Agenda.AppointmentImplLocal;
import org.phoebus.framework.nls.NLS;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.SearchResult;
import org.phoebus.logbook.olog.ui.query.OlogQuery;
import org.phoebus.logbook.olog.ui.query.OlogQueryManager;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A controller for a log entry table with a collapsible advance search section.
 *
 * @author Kunal Shroff
 */
public class LogEntryCalenderViewController extends LogbookSearchController {

    private static final Logger logger = Logger.getLogger(LogEntryCalenderViewController.class.getName());

    @FXML
    Button resize;
    @FXML
    Button search;
    @FXML
    ComboBox<OlogQuery> query;

    // elements associated with the various search
    @FXML
    GridPane ViewSearchPane;

    // Model
    List<LogEntry> logEntries;

    @SuppressWarnings("unused")
    @FXML
    private AnchorPane agendaPane;
    @FXML
    private Agenda agenda;

    // Model
    private Map<Appointment, LogEntry> map;
    private Map<String, Agenda.AppointmentGroup> appointmentGroupMap = new TreeMap<>();

    @SuppressWarnings("unused")
    @FXML
    private AdvancedSearchViewController advancedSearchViewController;

    private final OlogQueryManager ologQueryManager;
    private final ObservableList<OlogQuery> ologQueries = FXCollections.observableArrayList();
    private final SearchParameters searchParameters;

    public LogEntryCalenderViewController(LogClient logClient, OlogQueryManager ologQueryManager, SearchParameters searchParameters) {
        setClient(logClient);
        this.ologQueryManager = ologQueryManager;
        this.searchParameters = searchParameters;
    }

    @FXML
    public void initialize() {

        advancedSearchViewController.setSearchCallback(this::search);
        configureComboBox();
        // Set the search parameters in the advanced search controller so that it operates on the same object.
        ologQueries.setAll(ologQueryManager.getQueries());

        searchParameters.addListener((observable, oldValue, newValue) -> query.getEditor().setText(newValue));

        agenda = new Agenda();
        agenda.setEditAppointmentCallback(new Callback<Agenda.Appointment, Void>() {

            @Override
            public Void call(Appointment appointment) {
                return null;
            }
        });

        agenda.setActionCallback((appointment) -> {
            // show detailed view
            try {
                if (map != null) {
                    final Stage dialog = new Stage();
                    dialog.initModality(Modality.NONE);
                    ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
                    FXMLLoader loader = new FXMLLoader();
                    loader.setResources(resourceBundle);
                    loader.setLocation(this.getClass().getResource("LogEntryDisplay.fxml"));
                    loader.setControllerFactory(clazz -> {
                        try {
                            if (clazz.isAssignableFrom(SingleLogEntryDisplayController.class)) {
                                return clazz.getConstructor(LogClient.class).newInstance(client);
                            } else if (clazz.isAssignableFrom(AttachmentsViewController.class)) {
                                return clazz.getConstructor().newInstance();
                            } else if (clazz.isAssignableFrom(LogEntryDisplayController.class)) {
                                return clazz.getConstructor().newInstance();
                            } else if (clazz.isAssignableFrom(LogPropertiesController.class)) {
                                return clazz.getConstructor().newInstance();
                            }
                        } catch (Exception e) {
                            logger.log(Level.SEVERE, "Failed to construct controller for log entry display", e);
                        }
                        return null;
                    });
                    loader.load();
                    LogEntryDisplayController controller = loader.getController();
                    controller.setLogEntry(map.get(appointment));
                    Scene dialogScene = new Scene(loader.getRoot(), 800, 600);
                    dialog.setScene(dialogScene);
                    dialog.show();
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to show details for : " + appointment.getSummary(), e);
            }
            return null;
        });

        agenda.allowDraggingProperty().set(false);
        agenda.allowResizeProperty().set(false);

        appointmentGroupMap = agenda.appointmentGroups().stream()
                .collect(Collectors.toMap(AppointmentGroup::getDescription, Function.identity()));
        // find the css file

        try {
            String styleSheetResource = LogbookUIPreferences.calendar_view_item_stylesheet;
            URL url = this.getClass().getResource(styleSheetResource);
            // url may be null...
            if (url != null) {
                agenda.getStylesheets().add(this.getClass().getResource(styleSheetResource).toString());
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to set css style", e);
        }

        AnchorPane.setTopAnchor(agenda, 6.0);
        AnchorPane.setBottomAnchor(agenda, 6.0);
        AnchorPane.setLeftAnchor(agenda, 6.0);
        AnchorPane.setRightAnchor(agenda, 6.0);
        agendaPane.getChildren().add(agenda);

        query.itemsProperty().bind(new SimpleObjectProperty<>(ologQueries));

        query.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                // Query set -> search is triggered!
                query.setValue(new OlogQuery(query.getEditor().getText()));
            }
        });
        query.getEditor().setText(ologQueries.get(0).getQuery());
        // Query set -> search is triggered!
        query.getSelectionModel().select(ologQueries.get(0));

        resize.setText(">");

        search.disableProperty().bind(searchInProgress);

        determineConnectivity(connectivityMode -> {
            connectivityModeObjectProperty.set(connectivityMode);
            connectivityCheckerCountDownLatch.countDown();
            switch (connectivityModeObjectProperty.get()){
                case HTTP_ONLY -> search();
                case WEB_SOCKETS_SUPPORTED -> connectWebSocket();
            }
        });
    }

    // Keeps track of when the animation is active. Multiple clicks will be ignored
    // until a give resize action is completed
    private final AtomicBoolean moving = new AtomicBoolean(false);

    @SuppressWarnings("unused")
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
                    query.disableProperty().set(false);
                    //advancedSearchViewController.updateSearchParametersFromInput();
                    search();
                });
            } else {
                searchParameters.setQuery(query.getEditor().getText());
                Duration cycleDuration = Duration.millis(400);
                double width = ViewSearchPane.getWidth() / 4;
                KeyValue kv = new KeyValue(advancedSearchViewController.getPane().minWidthProperty(), width);
                KeyValue kv2 = new KeyValue(advancedSearchViewController.getPane().prefWidthProperty(), width);
                Timeline timeline = new Timeline(new KeyFrame(cycleDuration, kv, kv2));
                timeline.play();
                timeline.setOnFinished(event -> {
                    resize.setText("<");
                    moving.set(false);
                    query.disableProperty().set(true);
                });
            }
        }
    }

    @FXML
    @Override
    public void search() {
        String queryString = query.getEditor().getText();
        Map<String, String> params =
                LogbookQueryUtil.parseHumanReadableQueryString(ologQueryManager.getOrAddQuery(queryString).getQuery());

        logger.log(Level.INFO, "Single search: " + queryString);
        search(params,
                searchResult1 -> {
                    searchInProgress.set(false);
                    setSearchResult(searchResult1);
                    List<OlogQuery> queries = ologQueryManager.getQueries();
                    Platform.runLater(() -> {
                        ologQueries.setAll(queries);
                        query.getSelectionModel().select(ologQueries.get(0));
                    });
                },
                (msg, ex) -> {
                    searchInProgress.set(false);
                    ExceptionDetailsErrorDialog.openError(agenda, Messages.SearchFailed, "", ex);
        });
    }

    public String getQuery() {
        return query.getValue().getQuery();
    }

    @Override
    public void setLogs(List<LogEntry> logs) {
        this.logEntries = logs;
        refresh();
    }

    private void refresh() {
        map = new HashMap<Appointment, LogEntry>();
        map = this.logEntries.stream().collect(Collectors.toMap(new Function<LogEntry, Appointment>() {

            @Override
            public Appointment apply(LogEntry logentry) {
                AppointmentImplLocal appointment = new Agenda.AppointmentImplLocal();
                appointment.withSummary(logentry.getDescription());
                appointment.withDescription(logentry.getDescription());
                appointment.withStartLocalDateTime(
                        LocalDateTime.ofInstant(logentry.getCreatedDate(), ZoneId.systemDefault()));
                appointment.withEndLocalDateTime(
                        LocalDateTime.ofInstant(logentry.getCreatedDate().plusSeconds(2400), ZoneId.systemDefault()));
                List<String> logbookNames = getLogbookNames();
                if (logbookNames != null && !logbookNames.isEmpty()) {
                    try {
                        int index = logbookNames.indexOf(logentry.getLogbooks().iterator().next().getName());
                        if (index >= 0 && index <= 22) {
                            appointment.setAppointmentGroup(appointmentGroupMap.get(String.format("group%02d", (index + 1))));
                        } else {
                            appointment.setAppointmentGroup(appointmentGroupMap.get(String.format("group%02d", 23)));
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                return appointment;
            }
        }, new Function<LogEntry, LogEntry>() {

            @Override
            public LogEntry apply(LogEntry logentry) {
                return logentry;
            }

        }));
        Platform.runLater(() -> {
            agenda.appointments().clear();
            agenda.appointments().setAll(map.keySet());
        });
    }

    private List<String> getLogbookNames() {
        try {
            return client.listLogbooks().stream().map(l -> l.getName()).collect(Collectors.toList());
        } catch (Exception e) {
            logger.log(Level.INFO, "Unable to retireve logbook names", e);
            return null;
        }
    }

    private void setSearchResult(SearchResult searchResult) {
        setLogs(searchResult.getLogs());
        List<OlogQuery> queries = ologQueryManager.getQueries();
        Platform.runLater(() -> {
            ologQueries.setAll(queries);
            // Top-most query is the one used in the search.
            query.getSelectionModel().select(ologQueries.get(0));
            // Add the listener
        });
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
                        final ListCell<OlogQuery> cell = new ListCell<>() {
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
                        return cell;
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
}
