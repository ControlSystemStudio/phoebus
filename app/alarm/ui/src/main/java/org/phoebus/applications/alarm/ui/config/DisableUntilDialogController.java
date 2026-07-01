package org.phoebus.applications.alarm.ui.config;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;
import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.util.time.TimeParser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAmount;

public class DisableUntilDialogController {

    @FXML
    private DateTimePicker enabledDatePicker;

    @FXML
    private ComboBox<String> relativeDate;

    @FXML
    private Label invalidDate;


    protected final SimpleStringProperty relativeDateProperty = new SimpleStringProperty(null);
    protected final SimpleObjectProperty<LocalDateTime> enableDateProperty = new SimpleObjectProperty<>();
    protected final BooleanProperty invalidDateProperty = new SimpleBooleanProperty();

    public void initialize(){
        invalidDate.setTextFill(Color.RED);
        relativeDate.valueProperty().bindBidirectional(relativeDateProperty);
        enabledDatePicker.dateTimeValueProperty().bindBidirectional(enableDateProperty);
        invalidDate.visibleProperty().bind(invalidDateProperty);
        enableDateProperty.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                relativeDateProperty.setValue(null);
            }
        });


        enabledDatePicker.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue && invalidDateProperty.get()){
                invalidDateProperty.set(false);
            }
        });

        relativeDate.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue && invalidDateProperty.get()){
                invalidDateProperty.set(false);
            }
        });

        relativeDateProperty.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                enableDateProperty.setValue(null);
            }
        });

        enabledDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });

        enabledDatePicker.addEventHandler(KeyEvent.KEY_PRESSED, keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                try {
                    TextFormatter<?> tf = enabledDatePicker.getEditor().getTextFormatter();
                    @SuppressWarnings("unchecked")
                    StringConverter<LocalDateTime> conv =
                            (StringConverter<LocalDateTime>) tf.getValueConverter();
                    conv.fromString(enabledDatePicker.getEditor().getText());
                    enableDateProperty.set(conv.fromString(enabledDatePicker.getEditor().getText()));
                    enabledDatePicker.getEditor().commitValue();
                } catch (DateTimeParseException ex) {
                    keyEvent.consume();
                }
            }
        });


        String[] shelvingOptions = new String[AlarmSystem.shelving_options.length + 1];
        System.arraycopy(AlarmSystem.shelving_options, 0, shelvingOptions, 1, AlarmSystem.shelving_options.length);
        relativeDate.getItems().addAll(shelvingOptions);


    }

    public void setDefaultDate(LocalDateTime date){
        enableDateProperty.set(date);
    }


    /**
     * @param enableDate A non-null {@link LocalDateTime}
     * @return <code>true</code> if the specified date/time is considered valid, e.g. in the future.
     */
    private boolean isEnableDateValid(LocalDateTime enableDate) {
        return !enableDate.isBefore(LocalDateTime.now()) && !enableDate.isEqual(LocalDateTime.now());
    }


    /**
     * Attempts to determine a {@link LocalDateTime} based on the user input.
     *
     * @return A non-null {@link LocalDateTime} if user has specified a valid date/time, or <code>null</code> if
     * there is no user input from which to determine a date/time.
     * @throws IllegalArgumentException if user has entered an invalid date/time.
     */
    public LocalDateTime determineEnableDate() {

        if (enableDateProperty.isNotNull().get()) {
            if (isEnableDateValid(enableDateProperty.get())) {
                return enableDateProperty.get();
            } else {
                invalidDateProperty.set(true);
                throw new IllegalArgumentException("Enable date invalid");
            }
        } else if (relativeDateProperty.isNotNull().get()) {
            final TemporalAmount amount =
                    TimeParser.parseTemporalAmount(relativeDateProperty.get());
            final LocalDateTime updateDate = LocalDateTime.now().plus(amount);
            if (isEnableDateValid(updateDate)) {
                return updateDate;
            } else {
                invalidDateProperty.set(true);
                throw new IllegalArgumentException("Enable date invalid");
            }
        }
        return null;
    }
}
