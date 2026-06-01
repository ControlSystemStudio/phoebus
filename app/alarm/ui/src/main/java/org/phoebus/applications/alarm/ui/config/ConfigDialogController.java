/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.alarm.ui.config;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;
import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.ui.Messages;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.util.time.TimeParser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAmount;

public abstract class ConfigDialogController {

    @SuppressWarnings("unused")
    @FXML
    private ScrollPane scroll;
    @SuppressWarnings("unused")
    @FXML
    private GridPane layout;


    @SuppressWarnings("unused")
    @FXML
    private TextField path;

    @SuppressWarnings("unused")
    @FXML
    protected CheckBox enabled;

    @SuppressWarnings("unused")
    @FXML
    protected ComboBox<String> relativeDate;


    @SuppressWarnings("unused")
    @FXML
    protected DateTimePicker enabledDatePicker;

    // Shared table placeholders
    @SuppressWarnings("unused")
    @FXML
    private StackPane guidancePlaceholder;
    @SuppressWarnings("unused")
    @FXML
    private StackPane displaysPlaceholder;
    @SuppressWarnings("unused")
    @FXML
    private StackPane commandsPlaceholder;
    @SuppressWarnings("unused")
    @FXML
    private StackPane actionsPlaceholder;

    @FXML
    protected OptionsTablesController optionsTablesViewController;

    protected final AlarmClient alarmClient;
    protected final AlarmTreeItem<?> alarmTreeItem;

    protected final SimpleBooleanProperty itemEnabledProperty = new SimpleBooleanProperty();
    protected final SimpleStringProperty relativeDateProperty = new SimpleStringProperty(null);
    protected final SimpleObjectProperty<LocalDateTime> enableDateProperty =
            new SimpleObjectProperty<>(null);

    public ConfigDialogController(AlarmClient alarmClient, AlarmTreeItem<?> alarmTreeItem) {
        this.alarmClient = alarmClient;
        this.alarmTreeItem = alarmTreeItem;
    }

    @FXML
    public void initialize() {

        path.setText(alarmTreeItem.getPathName());

        relativeDate.valueProperty().bindBidirectional(relativeDateProperty);
        enabledDatePicker.dateTimeValueProperty().bindBidirectional(enableDateProperty);

        enabled.setOnAction(e -> {
            itemEnabledProperty.setValue(enabled.isSelected());
            relativeDateProperty.set(null);
            enableDateProperty.set(null);
        });

        enableDateProperty.addListener((observable, oldValue, newValue) -> {
            enabled.setSelected(newValue == null && relativeDateProperty.isNull().get());
            if (newValue != null) {
                relativeDateProperty.setValue(null);
            }
        });

        relativeDateProperty.addListener((observable, oldValue, newValue) -> {
            enabled.setSelected(newValue == null && enableDateProperty.isNull().get());
            if (newValue != null) {
                enableDateProperty.setValue(null);
            }
        });

        // Day-cell factory – disable past dates
        enabledDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });

        // ENTER key handler on the date picker's editor
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

        // Make sure first element in shelving options is null
        // so user can "deselect" a relative date.
        String[] shelvingOptions = new String[AlarmSystem.shelving_options.length + 1];
        System.arraycopy(AlarmSystem.shelving_options, 0, shelvingOptions, 1, AlarmSystem.shelving_options.length);
        relativeDate.getItems().addAll(shelvingOptions);

        // ── Scroll-pane width listener ────────────────────────────────────────
        scroll.widthProperty().addListener((p, old, width) ->
                layout.setPrefWidth(Math.max(width.doubleValue() - 40, 450)));
    }

    /**
     * Attempts to determine a {@link LocalDateTime} based on the user input.
     *
     * @return A non-null {@link LocalDateTime} if user has specified a valid date/time, or <code>null</code> if
     * there is no user input from which to determine a date/time.
     * @throws IllegalArgumentException if user has entered an invalid date/time.
     */
    protected LocalDateTime determineEnableDate() {

        if (enableDateProperty.isNotNull().get()) {
            if (isEnableDateValid(enableDateProperty.get())) {
                return enableDateProperty.get();
            } else {
                showInvalidEnableDateDialog();
                throw new IllegalArgumentException("Enable date invalid");
            }
        } else if (relativeDateProperty.isNotNull().get()) {
            final TemporalAmount amount =
                    TimeParser.parseTemporalAmount(relativeDateProperty.get());
            final LocalDateTime updateDate = LocalDateTime.now().plus(amount);
            if (isEnableDateValid(updateDate)) {
                return updateDate;
            } else {
                showInvalidEnableDateDialog();
                throw new IllegalArgumentException("Enable date invalid");
            }
        }
        return null;
    }

    /**
     * @param enableDate A non-null {@link LocalDateTime}
     * @return <code>true</code> if the specified date/time is considered valid, e.g. in the future.
     */
    private boolean isEnableDateValid(LocalDateTime enableDate) {
        return !enableDate.isBefore(LocalDateTime.now()) && !enableDate.isEqual(LocalDateTime.now());
    }

    /**
     * Shows a dialog indicate that the user-specified date is invalid, e.g. a date/time not in the future.
     */
    private void showInvalidEnableDateDialog() {
        Alert prompt = new Alert(Alert.AlertType.INFORMATION);
        prompt.setTitle(Messages.promptTitle);
        prompt.setHeaderText(Messages.promptTitle);
        prompt.setContentText(Messages.promptContent);
        DialogHelper.positionDialog(prompt, enabledDatePicker, 0, 0);
        prompt.showAndWait();
    }

    public abstract void validateAndStore();
}
