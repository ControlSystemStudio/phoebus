package org.phoebus.applications.alarm.logging.ui;

import com.sun.jersey.api.client.WebResource;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.phoebus.applications.alarm.logging.ui.AlarmLogTableQueryUtil.Keys;
import org.phoebus.ui.dialog.PopOver;
import org.phoebus.ui.time.TimeRelativeIntervalPane;
import org.phoebus.util.time.TimeParser;
import org.phoebus.util.time.TimeRelativeInterval;
import org.phoebus.util.time.TimestampFormats;

import java.util.logging.Logger;

import static org.phoebus.ui.time.TemporalAmountPane.Type.TEMPORAL_AMOUNTS_AND_NOW;

/**
 * Controller for the advanced search UI in the alarm log application.
 */
public class AdvancedSearchViewController {

    static final Logger logger = Logger.getLogger(AdvancedSearchViewController.class.getName());

    @FXML
    GridPane timePane;
    @FXML
    TextField startTime;
    @FXML
    TextField endTime;
    @FXML
    TextField searchPV;
    @FXML
    TextField searchSeverity;
    @FXML
    TextField searchMessage;
    @FXML
    TextField searchCurrentSeverity;
    @FXML
    TextField searchCurrentMessage;
    @FXML
    TextField searchUser;
    @FXML
    TextField searchHost;
    @FXML
    TextField searchCommand;

    PopOver timeSearchPopover;

    private WebResource searchClient;

    // Search parameters
    ObservableMap<Keys, String> searchParameters;

    @FXML
    private AnchorPane advancedSearchPane;

    public AdvancedSearchViewController(WebResource client){
        this.searchClient = client;
    }

    @FXML
    public void initialize() {

        advancedSearchPane.minWidthProperty().set(0);
        advancedSearchPane.maxWidthProperty().set(0);

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
                if (timeSearchPopover.isShowing())
                    timeSearchPopover.hide();
            });
        });
        Button cancel = new Button();
        cancel.setText("Cancel");
        cancel.setPrefWidth(80);
        cancel.setOnAction((event) -> {
            if (timeSearchPopover.isShowing())
                timeSearchPopover.hide();
        });
        hbox.getChildren().addAll(apply, cancel);
        timeBox.getChildren().addAll(timeSelectionPane, hbox);
        timeSearchPopover = new PopOver(timeBox);
        startTime.focusedProperty().addListener(
                (ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
                    if (newPropertyValue) {
                        timeSearchPopover.show(timePane);
                    } else if (timeSearchPopover.isShowing()) {
                        timeSearchPopover.hide();
                    }
                });

        endTime.focusedProperty().addListener(
                (ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
                    if (newPropertyValue) {
                        timeSearchPopover.show(timePane);
                    } else if (timeSearchPopover.isShowing()) {
                        timeSearchPopover.hide();
                    }
                });

        searchPV.textProperty().addListener((observable, oldValue, newValue) -> {
            searchParameters.put(Keys.PV, newValue);
        });

        searchSeverity.textProperty().addListener((observable, oldValue, newValue) -> {
            searchParameters.put(Keys.SEVERITY, newValue);
        });

        searchMessage.textProperty().addListener((observable, oldValue, newValue) -> {
            searchParameters.put(Keys.MESSAGE, newValue);
        });

        searchCurrentSeverity.textProperty().addListener((observable, oldValue, newValue) -> {
            searchParameters.put(Keys.CURRENTSEVERITY, newValue);
        });

        searchCurrentMessage.textProperty().addListener((observable, oldValue, newValue) -> {
            searchParameters.put(Keys.CURRENTMESSAGE, newValue);
        });

        searchUser.textProperty().addListener((observable, oldValue, newValue) -> {
            searchParameters.put(Keys.USER, newValue);
        });

        searchHost.textProperty().addListener((observable, oldValue, newValue) -> {
            searchParameters.put(Keys.HOST, newValue);
        });

        searchCommand.textProperty().addListener((observable, oldValue, newValue) -> {
            searchParameters.put(Keys.COMMAND, newValue);
        });
    }

    public void setSearchParameters(ObservableMap<Keys, String> params){
        searchParameters = params;
        searchParameters.addListener((MapChangeListener<Keys, String>) change -> {
            searchPV.setText(searchParameters.get(Keys.PV));
            searchSeverity.setText(searchParameters.get(Keys.SEVERITY));
            searchMessage.setText(searchParameters.get(Keys.MESSAGE));
            searchCurrentSeverity.setText(searchParameters.get(Keys.CURRENTSEVERITY));
            searchCurrentMessage.setText(searchParameters.get(Keys.CURRENTMESSAGE));
            searchUser.setText(searchParameters.get(Keys.USER));
            searchHost.setText(searchParameters.get(Keys.HOST));
            searchCommand.setText(searchParameters.get(Keys.COMMAND));
        });

        startTime.textProperty().bind(Bindings.valueAt(searchParameters, Keys.STARTTIME));
        endTime.textProperty().bind(Bindings.valueAt(searchParameters, Keys.ENDTIME));
        searchPV.setText(searchParameters.get(Keys.PV));
        searchSeverity.setText(searchParameters.get(Keys.SEVERITY));
        searchMessage.setText(searchParameters.get(Keys.MESSAGE));
        searchCurrentSeverity.setText(searchParameters.get(Keys.CURRENTSEVERITY));
        searchCurrentMessage.setText(searchParameters.get(Keys.CURRENTMESSAGE));
        searchUser.setText(searchParameters.get(Keys.USER));
        searchHost.setText(searchParameters.get(Keys.HOST));
        searchCommand.setText(searchParameters.get(Keys.COMMAND));
    }

    public AnchorPane getPane(){
        return advancedSearchPane;
    }
}
