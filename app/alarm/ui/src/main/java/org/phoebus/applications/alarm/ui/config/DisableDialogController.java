package org.phoebus.applications.alarm.ui.config;

import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.DisableDurationPreset;
import org.phoebus.applications.alarm.model.EnabledState;
import org.phoebus.applications.alarm.ui.Messages;
import org.phoebus.applications.alarm.ui.tree.AlarmTreeHelper;
import org.phoebus.util.time.TimestampFormats;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;


public class DisableDialogController {

    @FXML
    private GridPane disableDurationsBox;
    @FXML
    private DateTimePicker enabledDatePicker;

    /** The new enabled state which the user is requesting.
     *
     * Set to `new EnabledState(true)` to indicate an invalid selection.
     */
    private final SimpleObjectProperty<EnabledState> enabledStateProperty = new SimpleObjectProperty<>();
    private final Node okButton;

    public DisableDialogController(final EnabledState initialDateTime, final Node okButton)
    {
        enabledStateProperty.set(initialDateTime);
        this.okButton = okButton;
    }

    @FXML
    public void initialize()
    {
        // Disable OK button when in invalid state
        okButton.disableProperty().bind(enabledStateProperty.map(enabledState -> enabledState.enabled));

        // Create radio buttons from presets
        final ToggleGroup group = new ToggleGroup();
        int row = 0;
        for (final Map.Entry<String, DisableDurationPreset> preset : AlarmSystem.disable_duration_presets.entrySet()) {
            final LocalDateTime enableDateTime = preset.getValue().getNextDisableDateTime(LocalDateTime.now());

            final RadioButton radioButton = new RadioButton(preset.getKey());
            disableDurationsBox.add(radioButton, 0, row);
            radioButton.setToggleGroup(group);
            radioButton.selectedProperty().addListener(_ -> {
                if (radioButton.isSelected()) {
                    enabledStateProperty.set(new EnabledState(enableDateTime));
                }
            });

            final Label timeLabel = new Label(AlarmTreeHelper.formatEnableDateTime(enableDateTime));
            disableDurationsBox.add(timeLabel, 1, row);
            timeLabel.setStyle("-fx-text-fill: #888888");

            row++;
        }

        // 'indefinitely' radio button
        final RadioButton indefinitelyRadioButton = new RadioButton(Messages.indefinitely);
        disableDurationsBox.add(indefinitelyRadioButton, 0, row);
        indefinitelyRadioButton.setToggleGroup(group);
        indefinitelyRadioButton.selectedProperty().addListener(_ -> {
            if (indefinitelyRadioButton.isSelected()) {
                enabledStateProperty.set(new EnabledState(false));
            }
        });
        row++;

        // 'custom' radio button
        final RadioButton customRadioButton = new RadioButton(Messages.custom);
        disableDurationsBox.add(customRadioButton, 0, row);
        customRadioButton.setToggleGroup(group);
        customRadioButton.selectedProperty().addListener(_ -> {
            // If the 'custom' option has just been selected and the previous state was indefinite,
            // invalidate the selection because the user needs to select a date
            if (customRadioButton.isSelected() && enabledStateProperty.get().enabled_date == null) {
                enabledStateProperty.set(new EnabledState(true));
            }
        });

        // Custom enabled date picker
        enabledDatePicker.disableProperty().bind(customRadioButton.selectedProperty().not());
        enabledDatePicker.dateTimeValueProperty().addListener(_ -> {
            // Only update enabled state from enabled date picker if 'custom' is selected
            if (customRadioButton.isSelected()) {
                enabledStateProperty.set(new EnabledState(enabledDatePicker.getDateTimeValue()));
            }
        });
        enabledStateProperty.addListener(_ -> {
            enabledDatePicker.setDateTimeValue(enabledStateProperty.get().enabled_date);
        });

        // Select initial datetime
        final EnabledState enabledState = enabledStateProperty.get();
        if (enabledState.enabled_date != null) {
            customRadioButton.setSelected(true);
            enabledDatePicker.setDateTimeValue(enabledState.enabled_date);
        } else if (!enabledState.enabled) {
            indefinitelyRadioButton.setSelected(true);
        }
    }

    public EnabledState getEnabledState() {
        return enabledStateProperty.get();
    }
}
