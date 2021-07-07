package org.phoebus.logbook.olog.ui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import jfxtras.scene.control.agenda.Agenda;
import jfxtras.scene.control.agenda.Agenda.Appointment;
import jfxtras.scene.control.agenda.Agenda.AppointmentGroup;
import jfxtras.scene.control.agenda.Agenda.AppointmentImplLocal;
import org.phoebus.framework.nls.NLS;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.ui.time.TimeRelativeIntervalPane;
import org.phoebus.util.time.TimeParser;
import org.phoebus.util.time.TimeRelativeInterval;
import org.phoebus.util.time.TimestampFormats;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
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

import static org.phoebus.logbook.olog.ui.LogbookQueryUtil.*;
import static org.phoebus.ui.time.TemporalAmountPane.Type.TEMPORAL_AMOUNTS_AND_NOW;

/**
 * A controller for a log entry table with a collapsible advance search section.
 * @author Kunal Shroff
 *
 */
public class LogEntryCalenderViewController extends LogbookSearchController {

    private static final Logger logger = Logger.getLogger(LogEntryCalenderViewController.class.getName());

    @FXML
    Button resize;
    @FXML
    Button search;
    @FXML
    TextField query;

    // elements associated with the various search
    @FXML
    GridPane ViewSearchPane;

    // Model
    List<LogEntry> logEntries;

    // Search parameters
    ObservableMap<Keys, String> searchParameters;


    @FXML
    private AnchorPane agendaPane;
    @FXML
    private Agenda agenda;

    // Model
    private Map<Appointment, LogEntry> map;
    private Map<String, Agenda.AppointmentGroup> appointmentGroupMap = new TreeMap<String, Agenda.AppointmentGroup>();

    @FXML
    private AdvancedSearchViewController advancedSearchViewController;

    public LogEntryCalenderViewController(LogClient logClient){
        setClient(logClient);
    }
    
    @FXML
    public void initialize() {

        resize.setText("<");

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
                                return clazz.getConstructor(String.class).newInstance(getClient().getServiceUrl());
                            }
                            else if(clazz.isAssignableFrom(AttachmentsPreviewController.class)){
                                return clazz.getConstructor().newInstance();
                            }
                            else if(clazz.isAssignableFrom(LogEntryDisplayController.class)){
                                return clazz.getConstructor(LogClient.class).newInstance(getClient());
                            }
                            else if(clazz.isAssignableFrom(LogPropertiesController.class)){
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
            if(url != null){
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

        searchParameters = FXCollections.observableHashMap();

        LogbookQueryUtil.parseQueryString(LogbookUIPreferences.default_logbook_query).entrySet().stream().forEach(entry -> {
            searchParameters.put(Keys.findKey(entry.getKey()), entry.getValue());
        });
        advancedSearchViewController.setSearchParameters(searchParameters);


        searchParameters.addListener(new MapChangeListener<Keys, String>() {
            @Override
            public void onChanged(Change<? extends Keys, ? extends String> change) {
                Platform.runLater(() -> {
                    query.setText(searchParameters.entrySet().stream().sorted(Map.Entry.comparingByKey()).map((e) -> {
                        return e.getKey().getName().trim() + "=" + e.getValue().trim();
                    }).collect(Collectors.joining("&")));
                });
            }
        });

        query.setText(searchParameters.entrySet().stream().sorted(Map.Entry.comparingByKey()).map((e) -> {
            return e.getKey().getName().trim() + "=" + e.getValue().trim();
        }).collect(Collectors.joining("&")));

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
            });
        });
        Button cancel = new Button();
        cancel.setText("Cancel");
        cancel.setPrefWidth(80);
        hbox.getChildren().addAll(apply, cancel);
        timeBox.getChildren().addAll(timeSelectionPane, hbox);

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

    public void setQuery(String parsedQuery) {
        query.setText(parsedQuery);
        updateQuery();
        search();
    }

    public String getQuery() {
        return query.getText();
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
                if(logbookNames !=null && !logbookNames.isEmpty()){
                    int index = logbookNames.indexOf(logentry.getLogbooks().iterator().next().getName());
                    if(index >= 0 && index <= 22){
                        appointment.setAppointmentGroup(appointmentGroupMap.get(String.format("group%02d",(index+1))));
                    } else {
                        appointment.setAppointmentGroup(appointmentGroupMap.get(String.format("group%02d", 23)));
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
        agenda.appointments().clear();
        agenda.appointments().setAll(map.keySet());
    }

    private List<String> getLogbookNames(){
        try {
            return getClient().listLogbooks().stream().map(l -> l.getName()).collect(Collectors.toList());
        } catch (Exception e) {
            logger.log(Level.INFO, "Unable to retireve logbook names", e);
            return null;
        }
    }
}
