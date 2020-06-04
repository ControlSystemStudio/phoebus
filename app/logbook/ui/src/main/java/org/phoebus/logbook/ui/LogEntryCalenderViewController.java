package org.phoebus.logbook.ui;

import static org.phoebus.ui.time.TemporalAmountPane.Type.TEMPORAL_AMOUNTS_AND_NOW;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.Tag;
import org.phoebus.logbook.ui.LogbookQueryUtil.Keys;
import org.phoebus.ui.dialog.PopOver;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.time.TimeRelativeIntervalPane;
import org.phoebus.util.time.TimeParser;
import org.phoebus.util.time.TimeRelativeInterval;
import org.phoebus.util.time.TimestampFormats;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
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

/**
 * A controller for a log entry table with a collapsible advance search section.
 * @author Kunal Shroff
 *
 */
public class LogEntryCalenderViewController extends LogbookSearchController {

    private static final Logger logger = Logger.getLogger(LogEntryCalenderViewController.class.getName());

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
    GridPane ViewSearchPane;
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

    // Model
    List<LogEntry> logEntries;
    List<String> logbookNames;
    List<String> tagNames;

    private ListSelectionController tagController;
    private ListSelectionController logbookController;

    // Search parameters
    ObservableMap<Keys, String> searchParameters;


    @FXML
    private AnchorPane agendaPane;
    @FXML
    private Agenda agenda;
    private LogEntryControl logEntryControl;

    // Model
    private Map<Appointment, LogEntry> map;
    private Map<String, Agenda.AppointmentGroup> appointmentGroupMap = new TreeMap<String, Agenda.AppointmentGroup>();

    
    @FXML
    public void initialize() {

        // initialize the list of searchable parameters like logbooks, tags, etc...

        // initially set the search pane collapsed
        AdavanceSearchPane.minWidthProperty().set(0);
        AdavanceSearchPane.maxWidthProperty().set(0);
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
                    logEntryControl = new LogEntryControl();
                    logEntryControl.setLog(map.get(appointment));
                    Scene dialogScene = new Scene(logEntryControl, 300, 200);
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
            String styleSheetResource = LogbookUiPreferences.calendarViewItemStylesheet;
            agenda.getStylesheets().add(this.getClass().getResource(styleSheetResource).toString());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to set css style", e);
        }

        AnchorPane.setTopAnchor(agenda, 6.0);
        AnchorPane.setBottomAnchor(agenda, 6.0);
        AnchorPane.setLeftAnchor(agenda, 6.0);
        AnchorPane.setRightAnchor(agenda, 6.0);
        agendaPane.getChildren().add(agenda);

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
                    }).collect(Collectors.joining("&")));
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
            return e.getKey().getName().trim() + "=" + e.getValue().trim();
        }).collect(Collectors.joining("&")));

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
            logger.log(Level.WARNING, "Failed to open logbook search.", e);
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
            logger.log(Level.WARNING, "Failed to open tag search.", e);
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

}
